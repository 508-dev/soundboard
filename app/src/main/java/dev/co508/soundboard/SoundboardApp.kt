package dev.co508.soundboard

import android.app.Application
import dev.co508.soundboard.audio.SoundboardEngine
import dev.co508.soundboard.data.SoundRepository

/**
 * Process-wide holder for the engine and repository.
 *
 * The engine lives here rather than in the playback service so that binding is
 * never on the path between a tap and a sound: the service exists purely to
 * keep this process alive and show the notification, and the UI talks to the
 * engine directly. See `DECISIONS.md` → "Engine In Application, Service For
 * Lifetime Only". No DI framework, matching the sibling app.
 */
class SoundboardApp : Application() {
    /** Built on the main thread, as ExoPlayer requires. */
    val engine: SoundboardEngine by lazy { SoundboardEngine(this) }

    val repository: SoundRepository by lazy { SoundRepository.create(this) }
}
