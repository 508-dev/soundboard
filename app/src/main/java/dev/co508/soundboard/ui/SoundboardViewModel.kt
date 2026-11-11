package dev.co508.soundboard.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.co508.soundboard.audio.PlaybackStatus
import dev.co508.soundboard.audio.SoundboardEngine
import dev.co508.soundboard.data.Category
import dev.co508.soundboard.data.Sound
import dev.co508.soundboard.data.SoundLibrary
import dev.co508.soundboard.data.SoundRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** One row on the board: the persisted sound plus its live playback status. */
data class SoundRowState(
    val sound: Sound,
    val status: PlaybackStatus,
) {
    val isPlaying: Boolean get() = status.isPlaying
    val isLooping: Boolean get() = status == PlaybackStatus.LOOPING
    val isUnavailable: Boolean get() = status == PlaybackStatus.UNAVAILABLE
}

/** A category plus how many sounds are in it, for the switcher's subtitle. */
data class CategoryState(
    val category: Category,
    val soundCount: Int,
)

class SoundboardViewModel(
    private val repository: SoundRepository,
    private val engine: SoundboardEngine,
) : ViewModel() {
    /**
     * Shared eagerly because the snapshot readers below ([categoryIdsOf],
     * [soundsNotIn]) answer a tap synchronously and would otherwise see the
     * default empty library whenever nothing happened to be collecting.
     */
    private val library: StateFlow<SoundLibrary> =
        repository.library.stateIn(viewModelScope, SharingStarted.Eagerly, SoundLibrary())

    private val _selectedCategoryId = MutableStateFlow<String?>(null)

    /**
     * Which category the board is showing, or null for "All sounds".
     *
     * Deliberately not persisted: it resets to All sounds on a cold start
     * rather than reopening into a filtered view the user may not remember
     * choosing. It survives rotation, because the ViewModel does.
     */
    val selectedCategoryId: StateFlow<String?> = _selectedCategoryId.asStateFlow()

    val categories: StateFlow<List<CategoryState>> =
        library
            .map { lib -> lib.categories.map { CategoryState(it, lib.soundsIn(it.id).size) } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    /** How many sounds exist in total — the "All sounds" row's subtitle. */
    val allSoundsCount: StateFlow<Int> =
        library
            .map { it.sounds.size }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), 0)

    /** The selected category itself, or null while showing All sounds. */
    val selectedCategory: StateFlow<Category?> =
        combine(library, _selectedCategoryId) { lib, id -> id?.let(lib::findCategory) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    val rows: StateFlow<List<SoundRowState>> =
        combine(library, engine.statuses, _selectedCategoryId) { lib, statuses, categoryId ->
            // Fall back to All sounds if the selected category is gone, so a
            // delete from another surface can't leave the board permanently
            // empty with no way back.
            val effective = categoryId?.takeIf { lib.findCategory(it) != null }
            lib.soundsIn(effective).map { SoundRowState(it, statuses[it.id] ?: PlaybackStatus.IDLE) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    /**
     * True while anything is audible; the screen uses it to offer "Stop all".
     *
     * Intentionally global rather than scoped to the visible category: the
     * notification's Stop All is global too, and a sound playing in a category
     * you are not looking at is exactly the one you most want to be able to
     * stop.
     */
    val anyPlaying: StateFlow<Boolean> =
        engine.statuses
            .map { statuses -> statuses.any { it.value.isPlaying } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), false)

    fun selectCategory(id: String?) {
        _selectedCategoryId.value = id
    }

    fun toggle(sound: Sound) = engine.toggle(sound)

    fun pauseAll() = engine.pauseAll()

    /**
     * Adds picked files to the board, filing them under the visible category
     * when one is selected — picking files while viewing "Space" should put
     * them in Space, not just somewhere on the main board.
     */
    fun add(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val categoryId = _selectedCategoryId.value
        viewModelScope.launch { repository.add(uris, categoryId) }
    }

    /**
     * Applies a volume change to the running mix without persisting it.
     *
     * Called continuously while the dial is being dragged, so it must stay off
     * disk — [commitVolume] does the write once the gesture ends.
     */
    fun previewVolume(
        id: String,
        percent: Int,
    ) = engine.setGain(id, percent.coerceIn(Sound.MIN_VOLUME_PERCENT, Sound.MAX_VOLUME_PERCENT) / 100f)

    fun commitVolume(
        id: String,
        percent: Int,
    ) {
        previewVolume(id, percent)
        viewModelScope.launch { repository.setVolume(id, percent) }
    }

    /**
     * Deletes a sound from the whole app: every category, the main board, its
     * player, and its URI grant. The underlying file is never deleted.
     */
    fun remove(id: String) {
        engine.release(id)
        viewModelScope.launch { repository.remove(id) }
    }

    /**
     * Takes a sound out of one category, leaving it on the main board.
     *
     * Does not release the player — the sound may still be playing, and it is
     * still reachable under All sounds. That is the difference from [remove].
     */
    fun removeFromCategory(
        categoryId: String,
        soundId: String,
    ) {
        viewModelScope.launch { repository.removeFromCategory(categoryId, soundId) }
    }

    fun rename(
        id: String,
        name: String,
    ) {
        viewModelScope.launch { repository.rename(id, name) }
    }

    /**
     * Persists a new sound order.
     *
     * Called once a drag ends (with the visible rows' ids in their new order)
     * and by rearrange mode's sort shortcuts — both are one-shot writes, not a
     * continuously maintained sort, so a later manual drag is free to move
     * things again. A category's order is its own; rearranging "Space" leaves
     * the main board untouched.
     */
    fun commitOrder(order: List<String>) {
        val categoryId = _selectedCategoryId.value
        viewModelScope.launch {
            if (categoryId == null) repository.reorder(order) else repository.reorderCategory(categoryId, order)
        }
    }

    /**
     * Creates a category.
     *
     * With no [addSoundId] this switches to the new category, because creating
     * one from the switcher means going there to fill it. With one, the sound
     * is filed under the new category and the board stays where it is — that
     * path starts from a sound's checklist, where jumping to an empty category
     * would abandon the thing the user was doing.
     */
    fun createCategory(
        name: String,
        addSoundId: String? = null,
    ) {
        viewModelScope.launch {
            val id = repository.createCategory(name) ?: return@launch
            if (addSoundId == null) {
                _selectedCategoryId.value = id
            } else {
                repository.addToCategory(id, listOf(addSoundId))
            }
        }
    }

    fun renameCategory(
        id: String,
        name: String,
    ) {
        viewModelScope.launch { repository.renameCategory(id, name) }
    }

    /** Deletes a category, returning the board to All sounds if it was the visible one. */
    fun deleteCategory(id: String) {
        if (_selectedCategoryId.value == id) _selectedCategoryId.value = null
        viewModelScope.launch { repository.deleteCategory(id) }
    }

    fun addToCategory(
        categoryId: String,
        soundIds: List<String>,
    ) {
        if (soundIds.isEmpty()) return
        viewModelScope.launch { repository.addToCategory(categoryId, soundIds) }
    }

    fun setSoundCategories(
        soundId: String,
        categoryIds: Set<String>,
    ) {
        viewModelScope.launch { repository.setSoundCategories(soundId, categoryIds) }
    }

    /**
     * Snapshot of which categories a sound is in, read when its checklist
     * opens. The dialog then owns the ticks locally and commits once, matching
     * the volume dial's preview/commit split rather than writing per tap.
     */
    fun categoryIdsOf(soundId: String): Set<String> = library.value.categoryIdsOf(soundId)

    /** Snapshot of the sounds not yet in [categoryId], read when the picker opens. */
    fun soundsNotIn(categoryId: String): List<Sound> = library.value.soundsNotIn(categoryId)

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L

        val Factory =
            viewModelFactory {
                initializer {
                    val app = app()
                    SoundboardViewModel(app.repository, app.engine)
                }
            }
    }
}
