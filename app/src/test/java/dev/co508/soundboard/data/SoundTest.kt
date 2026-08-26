package dev.co508.soundboard.data

import org.junit.Assert.assertEquals
import org.junit.Test

class SoundTest {
    private fun sound(percent: Int) = Sound(id = "a", uri = "content://rain", name = "Rain", volumePercent = percent)

    @Test
    fun `gain maps percent onto a 0 to 1 multiplier`() {
        assertEquals(1f, sound(100).gain, TOLERANCE)
        assertEquals(0.5f, sound(50).gain, TOLERANCE)
        assertEquals(0f, sound(0).gain, TOLERANCE)
    }

    @Test
    fun `gain coerces a corrupt persisted percent instead of throwing`() {
        // A hand-edited or partially written library must still be playable.
        assertEquals(1f, sound(9_000).gain, TOLERANCE)
        assertEquals(0f, sound(-12).gain, TOLERANCE)
    }

    private companion object {
        const val TOLERANCE = 0.0001f
    }
}
