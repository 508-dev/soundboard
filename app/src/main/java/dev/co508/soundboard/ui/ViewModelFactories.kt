package dev.co508.soundboard.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import dev.co508.soundboard.SoundboardApp

/** Fetches the app-scoped [SoundboardApp] inside a `viewModelFactory { initializer { ... } }` block. */
fun CreationExtras.app(): SoundboardApp =
    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as SoundboardApp
