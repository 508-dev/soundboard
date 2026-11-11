package dev.co508.soundboard.data

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class SoundLibrarySerializerTest {
    @Test
    fun `round-trips a library`() =
        runTest {
            val original =
                SoundLibrary()
                    .withSound("a", "content://rain", "Rain")
                    .withSound("b", "content://fan", "Fan")
                    .withVolume("b", 35)

            val bytes = ByteArrayOutputStream().also { SoundLibrarySerializer.writeTo(original, it) }.toByteArray()
            val restored = SoundLibrarySerializer.readFrom(ByteArrayInputStream(bytes))

            assertEquals(original, restored)
        }

    @Test
    fun `malformed json degrades to an empty board rather than failing to launch`() =
        runTest {
            val restored = SoundLibrarySerializer.readFrom(ByteArrayInputStream("{ not json".toByteArray()))

            assertEquals(SoundLibrary(), restored)
        }

    @Test
    fun `unknown keys from a newer build are ignored`() =
        runTest {
            val json = """{"sounds":[{"id":"a","uri":"content://rain","name":"Rain","fadeMs":250}]}"""

            val restored = SoundLibrarySerializer.readFrom(ByteArrayInputStream(json.toByteArray()))

            assertEquals(listOf("Rain"), restored.sounds.map { it.name })
        }

    @Test
    fun `an empty file yields an empty board`() =
        runTest {
            assertEquals(SoundLibrary(), SoundLibrarySerializer.readFrom(ByteArrayInputStream(ByteArray(0))))
        }

    @Test
    fun `a library written before categories existed loads unchanged`() =
        runTest {
            val json = """{"sounds":[{"id":"a","uri":"content://rain","name":"Rain","volumePercent":40}]}"""

            val restored = SoundLibrarySerializer.readFrom(ByteArrayInputStream(json.toByteArray()))

            assertEquals(listOf("Rain"), restored.sounds.map { it.name })
            assertEquals(40, restored.sounds.single().volumePercent)
            assertEquals(emptyList<Category>(), restored.categories)
        }

    @Test
    fun `round-trips categories and their membership`() =
        runTest {
            val original =
                SoundLibrary()
                    .withSound("a", "content://rain", "Rain")
                    .withSound("b", "content://fan", "Fan")
                    .withCategory("space", "Background (Space)")
                    .withSoundsInCategory("space", listOf("b", "a"))

            val bytes = ByteArrayOutputStream().also { SoundLibrarySerializer.writeTo(original, it) }.toByteArray()
            val restored = SoundLibrarySerializer.readFrom(ByteArrayInputStream(bytes))

            assertEquals(original, restored)
            assertEquals(listOf("Fan", "Rain"), restored.soundsIn("space").map { it.name })
        }
}
