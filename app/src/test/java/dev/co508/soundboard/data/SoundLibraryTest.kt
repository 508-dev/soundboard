package dev.co508.soundboard.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SoundLibraryTest {
    @Test
    fun `withSound appends in order`() {
        val library =
            SoundLibrary()
                .withSound("a", "content://rain", "Rain")
                .withSound("b", "content://fan", "Fan")

        assertEquals(listOf("Rain", "Fan"), library.sounds.map { it.name })
    }

    @Test
    fun `withSound ignores a uri already on the board`() {
        val library =
            SoundLibrary()
                .withSound("a", "content://rain", "Rain")
                .withSound("b", "content://rain", "Rain (copy)")

        assertEquals(1, library.sounds.size)
        assertEquals("a", library.sounds.single().id)
    }

    @Test
    fun `new sounds start at full relative volume`() {
        val library = SoundLibrary().withSound("a", "content://rain", "Rain")

        assertEquals(Sound.DEFAULT_VOLUME_PERCENT, library.sounds.single().volumePercent)
    }

    @Test
    fun `withoutSound removes only the named sound`() {
        val library =
            SoundLibrary()
                .withSound("a", "content://rain", "Rain")
                .withSound("b", "content://fan", "Fan")
                .withoutSound("a")

        assertEquals(listOf("Fan"), library.sounds.map { it.name })
        assertNull(library.find("a"))
    }

    @Test
    fun `withoutSound on an unknown id is a no-op`() {
        val library = SoundLibrary().withSound("a", "content://rain", "Rain")

        assertEquals(library, library.withoutSound("nope"))
    }

    @Test
    fun `withVolume sets one sound and leaves the others alone`() {
        val library =
            SoundLibrary()
                .withSound("a", "content://rain", "Rain")
                .withSound("b", "content://fan", "Fan")
                .withVolume("a", 40)

        assertEquals(40, library.find("a")?.volumePercent)
        assertEquals(100, library.find("b")?.volumePercent)
    }

    @Test
    fun `withVolume clamps out-of-range input`() {
        val library = SoundLibrary().withSound("a", "content://rain", "Rain")

        assertEquals(100, library.withVolume("a", 250).find("a")?.volumePercent)
        assertEquals(0, library.withVolume("a", -40).find("a")?.volumePercent)
    }
}
