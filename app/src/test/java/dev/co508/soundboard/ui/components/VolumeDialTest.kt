package dev.co508.soundboard.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

/**
 * The dial's arc starts at 210° clockwise from 12 o'clock and sweeps 300°,
 * leaving a 60° dead zone centred on the bottom.
 */
class VolumeDialTest {
    private val size = IntSize(200, 200)
    private val radius = 80f

    /** A point on the dial at [degreesFromTop], measured clockwise from 12 o'clock. */
    private fun pointAt(degreesFromTop: Double): Offset {
        val radians = Math.toRadians(degreesFromTop)
        return Offset(
            x = size.width / 2f + (radius * sin(radians)).toFloat(),
            y = size.height / 2f - (radius * cos(radians)).toFloat(),
        )
    }

    @Test
    fun `arc start reads zero`() {
        assertEquals(0, percentAt(pointAt(210.0), size))
    }

    @Test
    fun `arc end reads one hundred`() {
        assertEquals(100, percentAt(pointAt(150.0), size))
    }

    @Test
    fun `straight up is the midpoint of the sweep`() {
        assertEquals(50, percentAt(pointAt(0.0), size))
    }

    @Test
    fun `quarter and three-quarter positions`() {
        assertEquals(25, percentAt(pointAt(285.0), size))
        assertEquals(75, percentAt(pointAt(75.0), size))
    }

    @Test
    fun `dead zone snaps to the nearer end instead of wrapping`() {
        // Just past 100%: stay at 100 rather than jumping to 0.
        assertEquals(100, percentAt(pointAt(160.0), size))
        // Just before 0%: stay at 0 rather than jumping to 100.
        assertEquals(0, percentAt(pointAt(200.0), size))
    }

    @Test
    fun `distance from centre does not affect the reading`() {
        // Only the angle matters, so a sloppy drag outside the ring still works.
        val near = Offset(size.width / 2f, size.height / 2f - 5f)
        val far = Offset(size.width / 2f, size.height / 2f - 500f)

        assertEquals(percentAt(near, size), percentAt(far, size))
    }
}
