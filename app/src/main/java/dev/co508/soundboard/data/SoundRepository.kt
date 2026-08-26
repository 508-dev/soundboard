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
import kotlinx.coroutines.flow.map
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
    val sounds: Flow<List<Sound>> = store.data.map { it.sounds }

    /**
     * Adds a picked document to the board, persisting read access to it.
     *
     * The caller must have used `ACTION_OPEN_DOCUMENT` (not `ACTION_GET_CONTENT`);
     * only the former grants a permission that survives a reboot.
     */
    suspend fun add(uri: Uri) {
        contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val name = displayNameOf(uri) ?: uri.lastPathSegment ?: "Sound"
        store.updateData { it.withSound(id = UUID.randomUUID().toString(), uri = uri.toString(), name = name) }
    }

    /**
     * Drops a sound from the board and releases its URI permission.
     *
     * The file itself is never deleted — that promise is in the confirmation
     * dialog the UI shows before calling this.
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

        fun create(context: Context): SoundRepository {
            val store =
                DataStoreFactory.create(serializer = SoundLibrarySerializer) {
                    context.dataStoreFile(FILE_NAME)
                }
            return SoundRepository(store, context.contentResolver)
        }
    }
}
