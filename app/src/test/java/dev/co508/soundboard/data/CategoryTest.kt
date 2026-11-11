package dev.co508.soundboard.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Category behaviour, with the two removals' asymmetry as the centrepiece:
 * `withoutSoundInCategory` is a reversible ungrouping, `withoutSound` is a
 * deletion that has to leave no references behind.
 */
class CategoryTest {
    private fun board() =
        SoundLibrary()
            .withSound("a", "content://rain", "Rain")
            .withSound("b", "content://fan", "Fan")
            .withSound("c", "content://cafe", "Cafe")
            .withCategory("space", "Background (Space)")
            .withCategory("fantasy", "Background (Fantasy)")

    @Test
    fun `a new category is empty and named`() {
        val library = SoundLibrary().withCategory("space", "Space")

        assertEquals("Space", library.findCategory("space")?.name)
        assertEquals(emptyList<String>(), library.findCategory("space")?.soundIds)
    }

    @Test
    fun `withCategory trims and rejects a blank name`() {
        assertEquals("Space", SoundLibrary().withCategory("s", "  Space  ").findCategory("s")?.name)
        assertEquals(emptyList<Category>(), SoundLibrary().withCategory("s", "   ").categories)
    }

    @Test
    fun `duplicate category names are allowed because identity is the id`() {
        val library = SoundLibrary().withCategory("one", "Ambience").withCategory("two", "Ambience")

        assertEquals(2, library.categories.size)
    }

    @Test
    fun `a null category id means every sound, in board order`() {
        assertEquals(listOf("Rain", "Fan", "Cafe"), board().soundsIn(null).map { it.name })
    }

    @Test
    fun `an unknown category id yields nothing rather than silently falling back to all sounds`() {
        assertEquals(emptyList<Sound>(), board().soundsIn("nope"))
    }

    @Test
    fun `a sound can be in several categories at once`() {
        val library =
            board()
                .withSoundInCategory("space", "a")
                .withSoundInCategory("fantasy", "a")

        assertEquals(listOf("Rain"), library.soundsIn("space").map { it.name })
        assertEquals(listOf("Rain"), library.soundsIn("fantasy").map { it.name })
        assertEquals(setOf("space", "fantasy"), library.categoryIdsOf("a"))
    }

    @Test
    fun `filing a sound twice in one category does not duplicate it`() {
        val library = board().withSoundInCategory("space", "a").withSoundInCategory("space", "a")

        assertEquals(listOf("a"), library.findCategory("space")?.soundIds)
    }

    @Test
    fun `a sound that is not on the board cannot be filed`() {
        val library = board().withSoundInCategory("space", "ghost")

        assertEquals(emptyList<String>(), library.findCategory("space")?.soundIds)
    }

    @Test
    fun `withSoundsInCategory skips unknown ids rather than storing dangling references`() {
        val library = board().withSoundsInCategory("space", listOf("a", "ghost", "b"))

        assertEquals(listOf("a", "b"), library.findCategory("space")?.soundIds)
    }

    @Test
    fun `removing a sound from one category leaves it in the others and on the board`() {
        val library =
            board()
                .withSoundInCategory("space", "a")
                .withSoundInCategory("fantasy", "a")
                .withoutSoundInCategory("space", "a")

        assertEquals(emptyList<Sound>(), library.soundsIn("space"))
        assertEquals(listOf("Rain"), library.soundsIn("fantasy").map { it.name })
        assertEquals("Rain", library.find("a")?.name)
    }

    @Test
    fun `deleting a sound prunes it from every category`() {
        val library =
            board()
                .withSoundInCategory("space", "a")
                .withSoundInCategory("fantasy", "a")
                .withoutSound("a")

        assertNull(library.find("a"))
        assertTrue(library.categories.all { "a" !in it.soundIds })
    }

    @Test
    fun `soundsIn ignores an id whose sound has vanished`() {
        // Reach past the mutators to build the dangling state a crash-between-writes
        // could leave behind, and prove the board still renders.
        val library = board().copy(categories = listOf(Category("space", "Space", listOf("a", "ghost"))))

        assertEquals(listOf("Rain"), library.soundsIn("space").map { it.name })
    }

    @Test
    fun `deleting a category keeps its sounds on the board`() {
        val library = board().withSoundInCategory("space", "a").withoutCategory("space")

        assertNull(library.findCategory("space"))
        assertEquals(listOf("Rain", "Fan", "Cafe"), library.soundsIn(null).map { it.name })
    }

    @Test
    fun `categoryRenamed trims and rejects a blank name`() {
        assertEquals("Deep space", board().categoryRenamed("space", " Deep space ").findCategory("space")?.name)
        assertEquals(
            "Background (Space)",
            board().categoryRenamed("space", "  ").findCategory("space")?.name,
        )
    }

    @Test
    fun `withSoundCategories sets membership exactly, adding and removing in one write`() {
        val library =
            board()
                .withSoundInCategory("space", "a")
                .withSoundCategories("a", setOf("fantasy"))

        assertEquals(setOf("fantasy"), library.categoryIdsOf("a"))
    }

    @Test
    fun `withSoundCategories with an empty set ungroups a sound entirely`() {
        val library =
            board()
                .withSoundInCategory("space", "a")
                .withSoundInCategory("fantasy", "a")
                .withSoundCategories("a", emptySet())

        assertEquals(emptySet<String>(), library.categoryIdsOf("a"))
        assertEquals("Rain", library.find("a")?.name)
    }

    @Test
    fun `withSoundCategories leaves an existing category's order alone`() {
        val library =
            board()
                .withSoundsInCategory("space", listOf("a", "b"))
                .withSoundCategories("c", setOf("space"))

        assertEquals(listOf("a", "b", "c"), library.findCategory("space")?.soundIds)
    }

    @Test
    fun `a category keeps its own order, independent of the board`() {
        val library =
            board()
                .withSoundsInCategory("space", listOf("a", "b", "c"))
                .categoryReordered("space", listOf("c", "a", "b"))

        assertEquals(listOf("Cafe", "Rain", "Fan"), library.soundsIn("space").map { it.name })
        assertEquals(listOf("Rain", "Fan", "Cafe"), library.soundsIn(null).map { it.name })
    }

    @Test
    fun `a stale category order rearranges what it can and keeps the rest`() {
        val library =
            board()
                .withSoundsInCategory("space", listOf("a", "b", "c"))
                .categoryReordered("space", listOf("c", "ghost"))

        assertEquals(listOf("c", "a", "b"), library.findCategory("space")?.soundIds)
    }

    @Test
    fun `soundsNotIn lists the add-from-library candidates`() {
        val library = board().withSoundInCategory("space", "a")

        assertEquals(listOf("Fan", "Cafe"), library.soundsNotIn("space").map { it.name })
    }

    @Test
    fun `picking a file while a category is open files it there`() {
        val library = board().withSound("d", "content://wind", "Wind", categoryId = "space")

        assertEquals(listOf("Wind"), library.soundsIn("space").map { it.name })
    }

    @Test
    fun `re-picking a known file files the existing sound rather than adding a copy`() {
        val library = board().withSound("dupe", "content://rain", "Rain again", categoryId = "space")

        assertEquals(3, library.sounds.size)
        assertEquals(listOf("a"), library.findCategory("space")?.soundIds)
    }
}
