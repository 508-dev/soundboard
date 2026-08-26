package dev.co508.soundboard.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.co508.soundboard.audio.PlaybackStatus
import dev.co508.soundboard.audio.SoundboardEngine
import dev.co508.soundboard.data.Sound
import dev.co508.soundboard.data.SoundRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** One row on the board: the persisted sound plus its live playback status. */
data class SoundRowState(
    val sound: Sound,
    val status: PlaybackStatus,
) {
    val isPlaying: Boolean get() = status == PlaybackStatus.PLAYING
    val isUnavailable: Boolean get() = status == PlaybackStatus.UNAVAILABLE
}

class SoundboardViewModel(
    private val repository: SoundRepository,
    private val engine: SoundboardEngine,
) : ViewModel() {
    val rows: StateFlow<List<SoundRowState>> =
        combine(repository.sounds, engine.statuses) { sounds, statuses ->
            sounds.map { SoundRowState(it, statuses[it.id] ?: PlaybackStatus.IDLE) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    /** True while anything is audible; the screen uses it to offer "Stop all". */
    val anyPlaying: StateFlow<Boolean> =
        engine.statuses
            .map { statuses -> statuses.any { it.value == PlaybackStatus.PLAYING } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), false)

    fun toggle(sound: Sound) = engine.toggle(sound)

    fun pauseAll() = engine.pauseAll()

    fun add(uri: Uri) {
        viewModelScope.launch { repository.add(uri) }
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

    /** Removes a sound from the board. The underlying file is never deleted. */
    fun remove(id: String) {
        engine.release(id)
        viewModelScope.launch { repository.remove(id) }
    }

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
