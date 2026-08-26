package dev.co508.soundboard.data

import kotlinx.serialization.Serializable

/**
 * One sound in the board.
 *
 * [uri] is the SAF document URI the user picked, held as a string. The file is
 * never copied into app storage — see `DECISIONS.md` → "Reference Picked Files
 * By URI, Never Copy".
 *
 * [volumePercent] is a 0..100 gain applied *relative to* whatever the Android
 * media stream volume currently is, so the two compose naturally: the system
 * volume moves every sound together, this moves one sound against the others.
 */
@Serializable
data class Sound(
    val id: String,
    val uri: String,
    val name: String,
    val volumePercent: Int = DEFAULT_VOLUME_PERCENT,
) {
    /**
     * Linear amplitude multiplier for ExoPlayer's `volume` property.
     *
     * Coerced rather than validated in `init` so a hand-edited or partially
     * corrupt persisted library degrades to a playable value instead of
     * crashing the app on startup.
     */
    val gain: Float
        get() = volumePercent.coerceIn(MIN_VOLUME_PERCENT, MAX_VOLUME_PERCENT) / MAX_VOLUME_PERCENT.toFloat()

    companion object {
        const val MIN_VOLUME_PERCENT = 0
        const val MAX_VOLUME_PERCENT = 100
        const val DEFAULT_VOLUME_PERCENT = 100
    }
}
