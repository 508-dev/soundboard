import java.util.Properties

// No `org.jetbrains.kotlin.android` here: AGP 9+ has built-in Kotlin support
// and applying that plugin alongside it is a build error. See
// https://developer.android.com/build/migrate-to-built-in-kotlin. The
// compose/serialization sub-plugins below are still applied separately —
// built-in Kotlin only subsumes the base kotlin-android plugin, not these.
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktlint)
}

// Release signing material, or null when none is configured. Two sources, both
// optional: a local `keystore.properties` (see `keystore.properties.example`)
// for signing side-load builds from a workstation, and `SOUNDBOARD_KEYSTORE_*`
// environment variables for CI, which holds the key in GitHub secrets and never
// materialises a properties file. `keystore.properties` wins when both are
// present. With neither, a release build comes out unsigned — which is exactly
// what F-Droid's build server wants, since it signs its own binary.
val releaseSigning: Map<String, String>? =
    run {
        val fromFile =
            rootProject.file("keystore.properties").takeIf { it.exists() }?.let { file ->
                val props = Properties().apply { file.inputStream().use(::load) }
                listOf("storeFile", "storePassword", "keyAlias", "keyPassword")
                    .associateWith { props.getProperty(it) }
            }
        // providers.environmentVariable, not System.getenv: the latter is opaque
        // to the configuration cache, which is on for this build.
        val fromEnv =
            mapOf(
                "storeFile" to "SOUNDBOARD_KEYSTORE_FILE",
                "storePassword" to "SOUNDBOARD_KEYSTORE_PASSWORD",
                "keyAlias" to "SOUNDBOARD_KEY_ALIAS",
                "keyPassword" to "SOUNDBOARD_KEY_PASSWORD",
            ).mapValues { (_, name) -> providers.environmentVariable(name).orNull }

        listOfNotNull(fromFile, fromEnv)
            .firstOrNull { material -> material.values.none { it.isNullOrBlank() } }
            ?.mapValues { (_, value) -> value!! }
    }

android {
    namespace = "dev.co508.soundboard"
    compileSdk = 37

    defaultConfig {
        applicationId = "dev.co508.soundboard"
        // API 26 (Android 8.0) gives us notification channels and the modern
        // foreground-service APIs natively, with no compat branches. Matches
        // the sibling app (emotion-tracker); revisit only with a concrete
        // reach requirement below that floor.
        minSdk = 26
        targetSdk = 36
        // Both literals are generated from `version.txt` by
        // `scripts/sync-version.sh` and re-checked by CI — don't hand-edit them.
        // release-please owns version.txt; versionCode is derived from it as
        // major * 1000000 + minor * 1000 + patch. They have to stay plain
        // literals on their own lines because F-Droid's update bot regex-parses
        // both straight out of this file (see docs/deployment.md).
        versionCode = 1001
        versionName = "0.1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            releaseSigning?.let { material ->
                signingConfig =
                    signingConfigs.create("release") {
                        storeFile = rootProject.file(material.getValue("storeFile"))
                        storePassword = material.getValue("storePassword")
                        keyAlias = material.getValue("keyAlias")
                        keyPassword = material.getValue("keyPassword")
                    }
            }
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    // Built-in Kotlin's jvmTarget defaults to targetCompatibility below, so
    // there's no separate `kotlin { compilerOptions { ... } }` block needed.
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        // BuildConfig.VERSION_NAME is shown on the About screen.
        buildConfig = true
    }

    // Google's dependency-info blob is an opaque, Google-signed binary stapled
    // into the artifact. F-Droid treats that as a non-free element in an
    // otherwise free build, and it defeats reproducibility. Off for both
    // outputs — nothing in this app reads it.
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

ktlint {
    // Uses the ktlint core version bundled with the Gradle plugin above.
    android.set(true)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.common)

    implementation(libs.androidx.datastore)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.serialization.json)
    testImplementation(libs.kotlinx.coroutines.test)
}
