package dev.co508.soundboard.audio

/** What one sound is doing right now. Mirrored into the UI per row. */
enum class PlaybackStatus {
    /** No player allocated, or explicitly stopped. */
    IDLE,

    /** Looping. */
    PLAYING,

    /** Player retained at its current position; tapping play resumes instantly. */
    PAUSED,

    /**
     * The URI could not be read — typically the file was moved, renamed, or
     * deleted, or the SAF grant was revoked. The row stays on the board so the
     * user can see what broke and delete it deliberately.
     */
    UNAVAILABLE,
}
