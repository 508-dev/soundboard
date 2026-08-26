# Development

No host services, no Docker Compose infra, no ports to coordinate — this is
a single Android app with local storage. The whole "dev loop" is
Gradle/Android Studio.

## First-Time Setup

1. Install **Android Studio** (current stable channel). It bundles a
   matching JDK, the Android SDK, platform-tools, and an emulator. Run it
   once so its first-run wizard installs the SDK.
2. Open this repository's root directory in Android Studio — it finds
   `settings.gradle.kts` and syncs automatically.
3. If you'd rather not install Android Studio: install the Android
   command-line tools + SDK platform 37 + build-tools 36 yourself, set
   `ANDROID_HOME`, then `./gradlew` works standalone. This is the harder
   path for a first Android project; Android Studio is recommended instead.
4. To sign local release builds, copy `keystore.properties.example` to
   `keystore.properties` and generate a keystore — see that file's comments.
   Debug builds don't need this.

The Gradle daemon runs on Java 25 (`gradle/gradle-daemon-jvm.properties`).
If your system JDK is a different major version, point `JAVA_HOME` at
Android Studio's bundled JBR for CLI builds:

```bash
export JAVA_HOME=/opt/android-studio/jbr   # path varies by install
```

## Commands

```bash
./gradlew tasks                 # sanity-check the build loads
./gradlew ktlintCheck           # lint/format check
./gradlew ktlintFormat          # apply formatting fixes
./gradlew testDebugUnitTest     # JVM unit tests, no emulator needed
./gradlew lintDebug             # Android lint
./gradlew assembleDebug         # build the debug APK
./gradlew installDebug          # build + install to a connected device/emulator
./gradlew check assembleDebug   # everything CI runs
```

`scripts/lint.sh`, `scripts/test.sh`, and `scripts/check-all.sh` wrap the
same tasks for CI/local consistency.

## Running The App

From Android Studio: pick a device/emulator in the toolbar and hit Run. From
the CLI, with a device connected or emulator running:

```bash
./gradlew installDebug
adb shell am start -n dev.co508.soundboard.debug/dev.co508.soundboard.MainActivity
```

(Debug builds get a `.debug` application-id suffix — see `app/build.gradle.kts`
— so debug and a real release build can be installed side by side.)

## Testing Audio Behavior

The unit tests cover pure logic only. The parts that matter most to this app
need a real device or emulator with audio, and are checked by hand:

- **Simultaneous playback** — start three sounds; all three should be audible
  and loop without a gap at the loop point.
- **Independent volume** — change one sound's percentage while others play;
  only that sound's level should move. Then change the device's media volume;
  all sounds should move together, keeping their relative balance.
- **Background playback** — start a sound, press Home, lock the screen. Audio
  continues and the notification shows the count. "Stop all" silences
  everything and the notification disappears.
- **Audio focus** — start a sound, then play something in another app. Ours
  should duck or pause, and recover when the other app stops.
- **Missing file** — add a sound from removable storage or a synced folder,
  then delete/move the file and press play. The row should show "File
  unavailable" rather than failing silently. Restoring the file and pressing
  play again should recover.

`adb logcat` is the tool of choice for the service lifecycle; filter with
`adb logcat | grep -i soundboard`.

## Storage

The board is a single JSON document written through DataStore to
`filesDir/datastore/sound_library.json`. There is no database and no schema
migration step; `SoundLibrarySerializer` tolerates unknown keys and degrades
a malformed file to an empty board. Inspect it on a debug build with:

```bash
adb shell run-as dev.co508.soundboard.debug cat files/datastore/sound_library.json
```

Note the board is deliberately excluded from Android backup — the URIs it
stores are only valid on the install that took them. See `DECISIONS.md` →
"Reference Picked Files By URI, Never Copy".

## Workspace Scratch

Do not commit `.context/` — it's gitignored workspace-local scratch for
agents. Durable knowledge belongs in `docs/`, `README.md`, or `DECISIONS.md`.

## Agent Notes

- Prefer `./gradlew <task>` over calling `kotlinc`/`aapt`/etc. directly.
- `testDebugUnitTest` and `ktlintCheck` don't need an emulator;
  `installDebug` and instrumented tests (`connectedDebugAndroidTest`, once
  `androidTest/` exists — see `DECISIONS.md` → "Deferred: Instrumented
  Coverage") do.
- If `ANDROID_HOME`/`ANDROID_SDK_ROOT` isn't set and Android Studio isn't
  installed, say so rather than guessing at a build result.
- You cannot verify audio by building. Don't claim playback, looping, mixing,
  or ducking works from a green `./gradlew check` — say what was and wasn't
  checked.
