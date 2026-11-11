package dev.co508.soundboard.data

import kotlinx.serialization.Serializable

/**
 * A user-named subset of the board — "Background (Space)", "Combat", and so on.
 *
 * A category holds *references*, not sounds: [soundIds] points into
 * [SoundLibrary.sounds], which stays the single copy of every sound. One sound
 * can therefore appear in any number of categories at no storage cost beyond a
 * repeated id, and removing it from one category cannot affect another. See
 * `DECISIONS.md` → "Categories Are Ordered Id Lists, Not Copies".
 *
 * [soundIds] is ordered, which is what gives each category its own arrangement:
 * a scene-ordered "Space" and an alphabetical main board are the same sounds in
 * two orders. Ids that no longer resolve are ignored when rendering rather than
 * treated as an error — see [SoundLibrary.soundsIn].
 */
@Serializable
data class Category(
    val id: String,
    val name: String,
    val soundIds: List<String> = emptyList(),
)
