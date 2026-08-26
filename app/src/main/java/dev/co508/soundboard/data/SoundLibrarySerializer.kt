package dev.co508.soundboard.data

import androidx.datastore.core.Serializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

/**
 * JSON codec for the persisted [SoundLibrary].
 *
 * A malformed file yields an empty library rather than a [CorruptionException]:
 * the board is a convenience list the user can rebuild in seconds, so silently
 * starting fresh beats refusing to launch. `ignoreUnknownKeys` lets an older
 * build open a library written by a newer one.
 */
object SoundLibrarySerializer : Serializer<SoundLibrary> {
    private val json = Json { ignoreUnknownKeys = true }

    override val defaultValue = SoundLibrary()

    override suspend fun readFrom(input: InputStream): SoundLibrary =
        try {
            json.decodeFromString(SoundLibrary.serializer(), input.readBytes().decodeToString())
        } catch (_: SerializationException) {
            defaultValue
        }

    override suspend fun writeTo(
        t: SoundLibrary,
        output: OutputStream,
    ) {
        output.write(json.encodeToString(SoundLibrary.serializer(), t).encodeToByteArray())
    }
}
