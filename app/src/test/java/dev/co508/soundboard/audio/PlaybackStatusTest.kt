package dev.co508.soundboard.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackStatusTest {
    @Test
    fun `stopped and paused sounds play once next`() {
        listOf(PlaybackStatus.IDLE, PlaybackStatus.PAUSED, PlaybackStatus.UNAVAILABLE).forEach { status ->
            assertEquals(PlaybackAction.PLAY_ONCE, status.nextPlaybackAction())
        }
    }

    @Test
    fun `a one-shot sound loops next`() {
        assertEquals(PlaybackAction.LOOP, PlaybackStatus.PLAYING.nextPlaybackAction())
    }

    @Test
    fun `a looping sound pauses next`() {
        assertEquals(PlaybackAction.PAUSE, PlaybackStatus.LOOPING.nextPlaybackAction())
    }

    @Test
    fun `both playing stages count as audible`() {
        assertTrue(PlaybackStatus.PLAYING.isPlaying)
        assertTrue(PlaybackStatus.LOOPING.isPlaying)
        listOf(PlaybackStatus.IDLE, PlaybackStatus.PAUSED, PlaybackStatus.UNAVAILABLE).forEach { status ->
            assertFalse(status.isPlaying)
        }
    }
}
