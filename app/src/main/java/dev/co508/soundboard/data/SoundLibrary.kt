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

    /**
     * Renames a sound, trimming surrounding whitespace.
     *
     * A blank result is a no-op rather than leaving the row with an empty label —
     * same "degrade rather than corrupt the board" stance as [withVolume]'s
     * clamping.
     */
    fun renamed(
        id: String,
        name: String,
    ): SoundLibrary {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return this
        return copy(sounds = sounds.map { if (it.id == id) it.copy(name = trimmed) else it })
    }

    /**
     * Moves the sound at [fromIndex] to [toIndex], shifting the sounds between them.
     *
     * Out-of-range indices are a no-op rather than a crash, since the caller derives
     * indices from a live drag gesture that can race a list change (e.g. a sound
     * removed elsewhere mid-drag).
     */
    fun moved(
        fromIndex: Int,
        toIndex: Int,
    ): SoundLibrary {
        if (fromIndex == toIndex || fromIndex !in sounds.indices || toIndex !in sounds.indices) return this
        val reordered = sounds.toMutableList()
        reordered.add(toIndex, reordered.removeAt(fromIndex))
        return copy(sounds = reordered)
    }

    /**
     * Reorders sounds to match [order], a list of every sound id in the desired order.
     *
     * Used both to commit a drag's final position and to apply the one-shot sort
     * shortcuts (name, volume, currently-playing) computed by the caller. An id in
     * [order] that isn't on the board is ignored; a sound missing from [order] keeps
     * its relative position, appended at the end — this degrades safely if the board
     * changed underneath a stale order (e.g. an add finished mid-drag) rather than
     * dropping rows.
     */
    fun reordered(order: List<String>): SoundLibrary {
        val byId = sounds.associateBy { it.id }
        val known = order.mapNotNull { byId[it] }
        val remaining = sounds.filterNot { it.id in order }
        return copy(sounds = known + remaining)
    }
}
