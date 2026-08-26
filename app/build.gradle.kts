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
        versionCode = 1
        versionName = "0.1.0"

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
            // Release signing is opt-in and local: see keystore.properties.example.
            // Unsigned release builds are still buildable for CI/verification.
            val keystorePropertiesFile = rootProject.file("keystore.properties")
            if (keystorePropertiesFile.exists()) {
                val keystoreProperties =
                    Properties().apply {
                        load(keystorePropertiesFile.inputStream())
                    }
                signingConfigs.create("release") {
                    storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                    storePassword = keystoreProperties.getProperty("storePassword")
                    keyAlias = keystoreProperties.getProperty("keyAlias")
                    keyPassword = keystoreProperties.getProperty("keyPassword")
                }
                signingConfig = signingConfigs.getByName("release")
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
