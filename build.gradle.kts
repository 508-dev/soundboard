// Root build file. Plugins are declared here with `apply false` and applied
// per-module in app/build.gradle.kts, per standard Android Gradle plugin
// convention. Do not add dependencies or android {} blocks at this level.
//
// No `org.jetbrains.kotlin.android` here: AGP 9+ has built-in Kotlin support
// and applying that plugin alongside it is now a build error. See
// DECISIONS.md / docs/tooling.md for why AGP 9.2.0 is pinned, and
// https://developer.android.com/build/migrate-to-built-in-kotlin.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ktlint) apply false
}
