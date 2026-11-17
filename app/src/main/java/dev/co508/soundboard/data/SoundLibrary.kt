package dev.co508.soundboard.data

import kotlinx.serialization.Serializable

/**
 * The persisted board: an ordered list of [Sound]s, plus the [Category]s that
 * slice it into named subsets.
 *
 * [sounds] is the only place a sound is stored; a category just holds ids. So
 * the "All sounds" view is `sounds` itself, a category view is
 * [soundsIn], and a sound can belong to many categories without being
 * duplicated.
 *
 * All mutations are pure functions returning a new library, which keeps them
 * unit-testable without a DataStore or an Android runtime. [SoundRepository]
 * is the only thing that persists the results.
 *
 * [categories] defaults to empty so a library written before categories
 * existed deserializes unchanged — there is no migration step.
 */
@Serializable
data class SoundLibrary(
    val sounds: List<Sound> = emptyList(),
    val categories: List<Category> = emptyList(),
) {
    /**
     * Appends a sound, ignoring the request when [uri] is already on the board,
     * and files it under [categoryId] when one is given.
     *
     * De-duplicating by URI means re-picking a file the user already added is a
     * no-op rather than a confusing double row. When the file *is* already
     * there, a non-null [categoryId] still applies — to the existing sound, not
     * a second copy of it — so picking a known file while viewing a category
     * does what the user plainly meant rather than nothing at all.
     */
    fun withSound(
        id: String,
        uri: String,
        name: String,
        categoryId: String? = null,
    ): SoundLibrary {
        val existing = sounds.firstOrNull { it.uri == uri }
        val added = if (existing != null) this else copy(sounds = sounds + Sound(id = id, uri = uri, name = name))
        if (categoryId == null) return added
        return added.withSoundInCategory(categoryId, existing?.id ?: id)
    }

    /**
     * Deletes a sound outright: off the master list and out of every category.
     *
     * The underlying file is never touched. Pruning the id everywhere is what
     * keeps categories from accumulating dangling references — [soundsIn] also
     * tolerates them, but two defences cost nothing and only one of them keeps
     * the document from growing.
     */
    fun withoutSound(id: String): SoundLibrary =
        copy(
            sounds = sounds.filterNot { it.id == id },
            categories = categories.map { it.copy(soundIds = it.soundIds.filterNot { soundId -> soundId == id }) },
        )

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

    // ---- Categories ----------------------------------------------------

    fun findCategory(id: String): Category? = categories.firstOrNull { it.id == id }

    /**
     * The sounds to show for [categoryId], or every sound when it is null.
     *
     * A category's ids are resolved with [mapNotNull], so an id left behind by
     * a sound that vanished is skipped rather than crashing the board — the
     * same "degrade rather than refuse to launch" stance the serializer takes.
     * An unknown [categoryId] yields no sounds instead of silently falling back
     * to all of them, which would hide the bug.
     */
    fun soundsIn(categoryId: String?): List<Sound> {
        if (categoryId == null) return sounds
        val category = findCategory(categoryId) ?: return emptyList()
        val byId = sounds.associateBy { it.id }
        return category.soundIds.mapNotNull { byId[it] }
    }

    /** Every category [soundId] currently belongs to. Backs the per-sound checklist. */
    fun categoryIdsOf(soundId: String): Set<String> = categories.filter { soundId in it.soundIds }.map { it.id }.toSet()

    /** Sounds on the board that are not yet in [categoryId] — the "add from library" candidates. */
    fun soundsNotIn(categoryId: String): List<Sound> {
        val category = findCategory(categoryId) ?: return emptyList()
        return sounds.filterNot { it.id in category.soundIds }
    }

    /**
     * Appends a new, empty category. A blank name is a no-op.
     *
     * Duplicate names are allowed: identity is [Category.id], and refusing the
     * user's second "Ambience" would be this app deciding it knows better.
     */
    fun withCategory(
        id: String,
        name: String,
    ): SoundLibrary {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return this
        return copy(categories = categories + Category(id = id, name = trimmed))
    }

    /** Deletes a category. Its sounds are untouched and stay on the main board. */
    fun withoutCategory(id: String): SoundLibrary = copy(categories = categories.filterNot { it.id == id })

    /** Renames a category, trimming whitespace; a blank result is a no-op, as in [renamed]. */
    fun categoryRenamed(
        id: String,
        name: String,
    ): SoundLibrary {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return this
        return copy(categories = categories.map { if (it.id == id) it.copy(name = trimmed) else it })
    }

    /** Files one sound under one category, appending at the end. Already-present is a no-op. */
    fun withSoundInCategory(
        categoryId: String,
        soundId: String,
    ): SoundLibrary = withSoundsInCategory(categoryId, listOf(soundId))

    /**
     * Files several sounds under one category in a single change.
     *
     * Ids that aren't on the board are dropped rather than stored, so a stale
     * selection (a sound deleted while the picker was open) can't write a
     * dangling reference.
     */
    fun withSoundsInCategory(
        categoryId: String,
        soundIds: List<String>,
    ): SoundLibrary {
        val category = findCategory(categoryId) ?: return this
        val known = sounds.map { it.id }.toSet()
        val toAdd = soundIds.filter { it in known && it !in category.soundIds }.distinct()
        if (toAdd.isEmpty()) return this
        return replacing(category.copy(soundIds = category.soundIds + toAdd))
    }

    /**
     * Takes one sound out of one category, leaving it on the board.
     *
     * This is the cheap, reversible counterpart to [withoutSound]: the sound
     * keeps its URI permission and any player it has, because it may still be
     * audible and is certainly still on the main board.
     */
    fun withoutSoundInCategory(
        categoryId: String,
        soundId: String,
    ): SoundLibrary {
        val category = findCategory(categoryId) ?: return this
        if (soundId !in category.soundIds) return this
        return replacing(category.copy(soundIds = category.soundIds.filterNot { it == soundId }))
    }

    /**
     * Sets exactly which categories [soundId] belongs to, in one write.
     *
     * Backs the per-sound checklist, where the user may tick and untick several
     * boxes before confirming. Categories gaining the sound append it at the
     * end, so an existing category keeps the order it already had.
     */
    fun withSoundCategories(
        soundId: String,
        categoryIds: Set<String>,
    ): SoundLibrary {
        if (sounds.none { it.id == soundId }) return this
        return copy(
            categories =
                categories.map { category ->
                    val shouldContain = category.id in categoryIds
                    val doesContain = soundId in category.soundIds
                    when {
                        shouldContain == doesContain -> category
                        shouldContain -> category.copy(soundIds = category.soundIds + soundId)
                        else -> category.copy(soundIds = category.soundIds.filterNot { it == soundId })
                    }
                },
        )
    }

    /**
     * Reorders one category's sounds — the per-category equivalent of [reordered].
     *
     * Degrades the same way: unknown ids in [order] are ignored and ids missing
     * from it keep their relative position at the end, so a stale order from a
     * drag that raced a membership change rearranges rather than drops rows.
     */
    fun categoryReordered(
        categoryId: String,
        order: List<String>,
    ): SoundLibrary {
        val category = findCategory(categoryId) ?: return this
        val present = category.soundIds.toSet()
        val known = order.filter { it in present }.distinct()
        val remaining = category.soundIds.filterNot { it in known }
        return replacing(category.copy(soundIds = known + remaining))
    }

    private fun replacing(category: Category): SoundLibrary =
        copy(categories = categories.map { if (it.id == category.id) category else it })
}
