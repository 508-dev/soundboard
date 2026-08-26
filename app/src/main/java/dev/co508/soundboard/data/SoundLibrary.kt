package dev.co508.soundboard.data

import kotlinx.serialization.Serializable

/**
 * The persisted board: an ordered list of [Sound]s.
 *
 * All mutations are pure functions returning a new library, which keeps them
 * unit-testable without a DataStore or an Android runtime. [SoundRepository]
 * is the only thing that persists the results.
 */
@Serializable
data class SoundLibrary(
    val sounds: List<Sound> = emptyList(),
) {
    /**
     * Appends a sound, ignoring the request when [uri] is already on the board.
     *
     * De-duplicating by URI means re-picking a file the user already added is a
     * no-op rather than a confusing double row.
     */
    fun withSound(
        id: String,
        uri: String,
        name: String,
    ): SoundLibrary =
        if (sounds.any { it.uri == uri }) {
            this
        } else {
            copy(sounds = sounds + Sound(id = id, uri = uri, name = name))
        }

    /** Removes a sound from the board. The underlying file is never touched. */
    fun withoutSound(id: String): SoundLibrary = copy(sounds = sounds.filterNot { it.id == id })

    /** Sets one sound's relative volume, clamping to the valid percent range. */
    fun withVolume(
        id: String,
        percent: Int,
    ): SoundLibrary =
        copy(
            sounds =
                sounds.map { sound ->
                    if (sound.id == id) {
                        sound.copy(
                            volumePercent =
                                percent.coerceIn(
                                    Sound.MIN_VOLUME_PERCENT,
                                    Sound.MAX_VOLUME_PERCENT,
                                ),
                        )
                    } else {
                        sound
                    }
                },
        )

    fun find(id: String): Sound? = sounds.firstOrNull { it.id == id }
}
