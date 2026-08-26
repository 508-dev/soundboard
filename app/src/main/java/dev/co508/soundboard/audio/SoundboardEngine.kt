package dev.co508.soundboard.audio

import android.content.Context
import android.media.AudioManager
import androidx.annotation.MainThread
import androidx.core.net.toUri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import dev.co508.soundboard.data.Sound
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Mixes any number of looping sounds, each with its own gain.
 *
 * One [ExoPlayer] per sound (see `DECISIONS.md` → "Media3 ExoPlayer, One Player
 * Per Sound"). Players are created on first play and retained while paused so
 * resume is instant; they are released when the sound leaves the board or the
 * whole engine shuts down.
 *
 * **Threading:** ExoPlayer instances must be touched only from the thread that
 * built them. Every method here is main-thread-only, and the engine is built on
 * the main thread by [dev.co508.soundboard.SoundboardApp].
 *
 * **Audio focus** is requested once for the engine as a whole rather than per
 * player: N players each requesting focus would be N competing requests from
 * the same app, and the last one would win against its own siblings.
 */
@MainThread
class SoundboardEngine(
    private val context: Context,
) {
    private val players = mutableMapOf<String, ExoPlayer>()

    /** Gains last requested by the UI, kept so ducking can be applied on top. */
    private val gains = mutableMapOf<String, Float>()

    private val _statuses = MutableStateFlow<Map<String, PlaybackStatus>>(emptyMap())

    /** Per-sound status, keyed by [Sound.id]. Absent means [PlaybackStatus.IDLE]. */
    val statuses: StateFlow<Map<String, PlaybackStatus>> = _statuses.asStateFlow()

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val focus = AudioFocusHolder(audioManager, ::onFocusChanged)

    /** Multiplier applied to every player while another app has ducked us. */
    private var duckFactor = 1f

    /** Sounds paused by focus loss, to be resumed if focus comes back. */
    private val pausedByFocusLoss = mutableSetOf<String>()

    /** True while any sound is audible — drives the foreground service lifetime. */
    val isAnythingPlaying: Boolean
        get() = _statuses.value.any { it.value == PlaybackStatus.PLAYING }

    fun play(sound: Sound) {
        // A player that hit an error stays in that state and ignores play(),
        // so retrying a restored file needs a fresh one.
        if (_statuses.value[sound.id] == PlaybackStatus.UNAVAILABLE) {
            players.remove(sound.id)?.release()
        }
        val player = players.getOrPut(sound.id) { buildPlayer(sound) }
        gains[sound.id] = sound.gain
        player.volume = sound.gain * duckFactor
        if (!focus.request()) return
        pausedByFocusLoss.remove(sound.id)
        player.play()
        setStatus(sound.id, PlaybackStatus.PLAYING)
    }

    fun pause(id: String) {
        val player = players[id] ?: return
        player.pause()
        pausedByFocusLoss.remove(id)
        // An unavailable sound stays unavailable; pausing it must not present
        // it as a healthy, paused row.
        if (_statuses.value[id] != PlaybackStatus.UNAVAILABLE) {
            setStatus(id, PlaybackStatus.PAUSED)
        }
        abandonFocusIfSilent()
    }

    fun toggle(sound: Sound) {
        if (_statuses.value[sound.id] == PlaybackStatus.PLAYING) pause(sound.id) else play(sound)
    }

    /** Applies a new relative volume live, without interrupting playback. */
    fun setGain(
        id: String,
        gain: Float,
    ) {
        gains[id] = gain
        players[id]?.volume = gain * duckFactor
    }

    /** Pauses every sound. Backs the notification's "Stop all" action. */
    fun pauseAll() {
        _statuses.value
            .filterValues { it == PlaybackStatus.PLAYING }
            .keys
            .toList()
            .forEach(::pause)
    }

    /** Releases one sound's player — call when it is removed from the board. */
    fun release(id: String) {
        players.remove(id)?.release()
        gains.remove(id)
        pausedByFocusLoss.remove(id)
        _statuses.value = _statuses.value - id
        abandonFocusIfSilent()
    }

    /** Releases everything. The engine is unusable afterwards. */
    fun releaseAll() {
        players.values.forEach(ExoPlayer::release)
        players.clear()
        gains.clear()
        pausedByFocusLoss.clear()
        _statuses.value = emptyMap()
        focus.abandon()
    }

    private fun buildPlayer(sound: Sound): ExoPlayer =
        ExoPlayer
            .Builder(context)
            .build()
            .apply {
                setAudioAttributes(
                    AudioAttributes
                        .Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                        .build(),
                    // handleAudioFocus: false, because AudioFocusHolder owns
                    // one request for the whole engine. See AudioFocusHolder.
                    false,
                )
                repeatMode = Player.REPEAT_MODE_ONE
                setMediaItem(MediaItem.fromUri(sound.uri.toUri()))
                addListener(
                    object : Player.Listener {
                        override fun onPlayerError(error: PlaybackException) {
                            setStatus(sound.id, PlaybackStatus.UNAVAILABLE)
                            abandonFocusIfSilent()
                        }
                    },
                )
                prepare()
            }

    private fun setStatus(
        id: String,
        status: PlaybackStatus,
    ) {
        _statuses.value = _statuses.value + (id to status)
    }

    private fun abandonFocusIfSilent() {
        if (!isAnythingPlaying) focus.abandon()
    }

    private fun onFocusChanged(change: Int) {
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                pausedByFocusLoss.clear()
                pauseAll()
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                // Remember what was playing so AUDIOFOCUS_GAIN can restore exactly that.
                _statuses.value
                    .filterValues { it == PlaybackStatus.PLAYING }
                    .keys
                    .forEach { id ->
                        pausedByFocusLoss += id
                        players[id]?.pause()
                        setStatus(id, PlaybackStatus.PAUSED)
                    }
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> applyDuck(DUCK_FACTOR)

            AudioManager.AUDIOFOCUS_GAIN -> {
                applyDuck(1f)
                pausedByFocusLoss.toList().forEach { id ->
                    players[id]?.play()
                    setStatus(id, PlaybackStatus.PLAYING)
                }
                pausedByFocusLoss.clear()
            }
        }
    }

    private fun applyDuck(factor: Float) {
        duckFactor = factor
        players.forEach { (id, player) ->
            player.volume = (gains[id] ?: 1f) * factor
        }
    }

    private companion object {
        /** Conventional ducking level while another app speaks over us. */
        const val DUCK_FACTOR = 0.2f
    }
}
