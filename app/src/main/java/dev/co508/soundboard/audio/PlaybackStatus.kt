package dev.co508.soundboard.audio

/** What one sound is doing right now. Mirrored into the UI per row. */
enum class PlaybackStatus {
    /** No playback in progress; a player may still be allocated. */
    IDLE,

    /** Playing to the end of the file, without repeating. */
    PLAYING,

    /** Repeating the file until paused. */
    LOOPING,

    /** Player retained at its current position; tapping play resumes instantly. */
    PAUSED,

    /**
     * The URI could not be read — typically the file was moved, renamed, or
     * deleted, or the SAF grant was revoked. The row stays on the board so the
     * user can see what broke and delete it deliberately.
     */
    UNAVAILABLE,
    ;

    /** Whether this status counts as audible playback. */
    val isPlaying: Boolean get() = this == PLAYING || this == LOOPING
}

/** The action a row's next tap will perform. */
enum class PlaybackAction {
    PLAY_ONCE,
    LOOP,
    PAUSE,
}

/** The three-stage playback cycle: stopped/paused → once → looping → paused. */
fun PlaybackStatus.nextPlaybackAction(): PlaybackAction =
    when (this) {
        PlaybackStatus.PLAYING -> PlaybackAction.LOOP
        PlaybackStatus.LOOPING -> PlaybackAction.PAUSE
        PlaybackStatus.IDLE,
        PlaybackStatus.PAUSED,
        PlaybackStatus.UNAVAILABLE,
        -> PlaybackAction.PLAY_ONCE
    }
