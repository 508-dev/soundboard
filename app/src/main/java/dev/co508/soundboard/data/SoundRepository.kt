package dev.co508.soundboard.data

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteException
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.net.toUri
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.UUID

/**
 * Persists the board and takes the long-lived read permission on picked files.
 *
 * Storage is a single JSON document via DataStore rather than a database: the
 * board is one short ordered list with no queries and no relations. See
 * `DECISIONS.md` → "DataStore Over Room".
 */
class SoundRepository(
    private val store: DataStore<SoundLibrary>,
    private val contentResolver: ContentResolver,
) {
    /**
     * The whole document, not just the sound list: the ViewModel needs sounds
     * and categories together to resolve the selected category's rows, and
     * splitting them into two flows would only let the two disagree for a
     * frame.
     */
    val library: Flow<SoundLibrary> = store.data

    /**
     * Adds a picked document to the board, persisting read access to it.
     *
     * The caller must have used `ACTION_OPEN_DOCUMENT` (not `ACTION_GET_CONTENT`);
     * only the former grants a permission that survives a reboot.
     */
    suspend fun add(uri: Uri) = add(listOf(uri))

    /**
     * Adds every picked document in a single write.
     *
     * Names are resolved before touching the store so one slow content provider
     * doesn't hold a DataStore transaction open. A file whose permission can't
     * be persisted is skipped rather than failing the whole batch — with a
     * multi-select of thirty files, losing one shouldn't lose the other
     * twenty-nine.
     */
    suspend fun add(
        uris: List<Uri>,
        categoryId: String? = null,
    ) {
        if (uris.isEmpty()) return

        val picked =
            uris.mapNotNull { uri ->
                try {
                    contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    uri to (displayNameOf(uri) ?: uri.lastPathSegment ?: DEFAULT_NAME)
                } catch (_: SecurityException) {
                    null
                }
            }
        if (picked.isEmpty()) return

        store.updateData { library ->
            picked.fold(library) { acc, (uri, name) ->
                acc.withSound(
                    id = UUID.randomUUID().toString(),
                    uri = uri.toString(),
                    name = name,
                    categoryId = categoryId,
                )
            }
        }
    }

    /**
     * Drops a sound from the board entirely and releases its URI permission.
     *
     * The file itself is never deleted — that promise is in the confirmation
     * dialog the UI shows before calling this. Every category referencing the
     * sound loses it too; see [SoundLibrary.withoutSound]. Use
     * [removeFromCategory] for the reversible, category-scoped removal.
     */
    suspend fun remove(id: String) {
        val removed = store.data.first().find(id) ?: return
        store.updateData { it.withoutSound(id) }
        // Only release once the row is gone, so a crash between the two leaves
        // a usable sound rather than a row pointing at an unreadable URI.
        releaseUriPermission(removed.uri.toUri())
    }

    suspend fun setVolume(
        id: String,
        percent: Int,
    ) {
        store.updateData { it.withVolume(id, percent) }
    }

    /** Renames a sound — see [SoundLibrary.renamed]. */
    suspend fun rename(
        id: String,
        name: String,
    ) {
        store.updateData { it.renamed(id, name) }
    }

    /** Persists a new order for the main board — see [SoundLibrary.reordered]. */
    suspend fun reorder(order: List<String>) {
        store.updateData { it.reordered(order) }
    }

    /** Persists a new order within one category — see [SoundLibrary.categoryReordered]. */
    suspend fun reorderCategory(
        categoryId: String,
        order: List<String>,
    ) {
        store.updateData { it.categoryReordered(categoryId, order) }
    }

    /** Creates an empty category and returns its new id, or null if [name] was blank. */
    suspend fun createCategory(name: String): String? {
        if (name.isBlank()) return null
        val id = UUID.randomUUID().toString()
        store.updateData { it.withCategory(id, name) }
        return id
    }

    /**
     * Deletes a category. Its sounds keep their files, URI grants, and places
     * on the main board — only the grouping goes away.
     */
    suspend fun deleteCategory(id: String) {
        store.updateData { it.withoutCategory(id) }
    }

    suspend fun renameCategory(
        id: String,
        name: String,
    ) {
        store.updateData { it.categoryRenamed(id, name) }
    }

    /** Files existing sounds under a category — see [SoundLibrary.withSoundsInCategory]. */
    suspend fun addToCategory(
        categoryId: String,
        soundIds: List<String>,
    ) {
        store.updateData { it.withSoundsInCategory(categoryId, soundIds) }
    }

    /**
     * Takes a sound out of one category without deleting it.
     *
     * Deliberately does **not** release the URI permission or touch the engine:
     * the sound is still on the main board and may still be playing. That
     * asymmetry with [remove] is the whole point of having both.
     */
    suspend fun removeFromCategory(
        categoryId: String,
        soundId: String,
    ) {
        store.updateData { it.withoutSoundInCategory(categoryId, soundId) }
    }

    /** Sets a sound's full category membership in one write — see [SoundLibrary.withSoundCategories]. */
    suspend fun setSoundCategories(
        soundId: String,
        categoryIds: Set<String>,
    ) {
        store.updateData { it.withSoundCategories(soundId, categoryIds) }
    }

    private fun releaseUriPermission(uri: Uri) {
        try {
            contentResolver.releasePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: SecurityException) {
            // Already released, or never held — nothing to clean up.
        }
    }

    private fun displayNameOf(uri: Uri): String? =
        try {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                val column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (column >= 0 && cursor.moveToFirst()) {
                    cursor.getString(column)?.substringBeforeLast('.')
                } else {
                    null
                }
            }
        } catch (_: SecurityException) {
            null
        } catch (_: SQLiteException) {
            null
        }

    companion object {
        private const val FILE_NAME = "sound_library.json"
        private const val DEFAULT_NAME = "Sound"

        fun create(context: Context): SoundRepository {
            val store =
                DataStoreFactory.create(serializer = SoundLibrarySerializer) {
                    context.dataStoreFile(FILE_NAME)
                }
            return SoundRepository(store, context.contentResolver)
        }
    }
}
