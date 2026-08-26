package dev.co508.soundboard.audio

import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager

/**
 * Owns the app's single audio-focus request.
 *
 * Idempotent: [request] is a no-op while focus is already held, so the engine
 * can call it on every play without stacking requests. AudioFocusRequest is
 * API 26+, which is our minSdk, so there is no legacy branch here.
 */
internal class AudioFocusHolder(
    private val audioManager: AudioManager,
    private val onChange: (Int) -> Unit,
) {
    private var held = false

    private val request =
        AudioFocusRequest
            .Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                AudioAttributes
                    .Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            ).setWillPauseWhenDucked(false)
            .setOnAudioFocusChangeListener { onChange(it) }
            .build()

    /** Returns true when focus is held and playback may proceed. */
    fun request(): Boolean {
        if (held) return true
        held = audioManager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        return held
    }

    fun abandon() {
        if (!held) return
        audioManager.abandonAudioFocusRequest(request)
        held = false
    }
}
