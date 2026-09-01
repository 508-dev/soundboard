# AI Agent Development Guide

## Environment

- This is a single-module native Android app (Kotlin + Jetpack Compose,
  Gradle Kotlin DSL). There is no Node/Python/Ruby/Docker stack — don't
  reach for `bun`, `uv`, `bundle`, or `docker compose` here. The repo was
  generated from the 508.dev devkit, which is web-oriented; the stack packs
  were deleted (see `DECISIONS.md` → "Native Android, Not A Web/Service
  Stack").
- Building, testing, and running the app requires the Android SDK. Prefer
  `./gradlew <task>` over invoking tools directly. `./gradlew tasks` and
  `./gradlew testDebugUnitTest` don't need an emulator; `installDebug` and
  instrumented tests do.
- The Gradle daemon is pinned to Java 25 (`gradle/gradle-daemon-jvm.properties`).
  If the system JDK differs, use Android Studio's bundled JBR:
  `JAVA_HOME=/opt/android-studio/jbr ./gradlew <task>`.
- If `ANDROID_HOME`/`ANDROID_SDK_ROOT` isn't set and Android Studio isn't
  installed, say so rather than guessing at a build result — see
  `docs/development.md` for setup.
- Treat `keystore.properties` (gitignored, see `keystore.properties.example`)
  as a secret. Never print its contents or commit a real one.

## Repository Shape

- `AGENTS.md`: this file — canonical agent operating instructions.
- `MANIFEST.md`: devkit file inventory this repo was generated from; useful
  if pulling in another devkit convention pack later (e.g. a backend).
- `DECISIONS.md`: this project's architecture decisions and why.
- `SPEC.md`: the MVP product spec. Delete it once the MVP ships — after that,
  work moves to tickets.
- `app/`: the single Gradle module. All app code lives here.
- `docs/`: contributor-facing documentation.
- `extras/github/`: optional GitHub hygiene (CODEOWNERS, gitleaks, dependency review).
- `scripts/`: thin wrappers around `./gradlew` for CI and local use.
- `.context/`: gitignored workspace-local scratch for agents.

## Code Map

Four layers, each with one job:

- `data/` — `Sound`, `SoundLibrary` (pure, testable list operations),
  `SoundLibrarySerializer`, `SoundRepository` (DataStore + SAF permissions).
- `audio/` — `SoundboardEngine` (one ExoPlayer per sound, the mixer),
  `AudioFocusHolder` (one focus request for the whole app), `PlaybackService`
  (foreground lifetime + notification), `PlaybackStatus`.
- `ui/` — `SoundboardViewModel`, `SoundboardScreen`, `components/SoundRow`,
  `components/ReorderableSoundList` (rearrange mode's drag-to-reorder list —
  a separate row from `SoundRow`, not a shared one; see `DECISIONS.md`),
  `components/VolumeDial`, `components/RenameDialog`, `components/AppScaffold` (drawer),
  `components/DrawerScaffold` (shared top bar), `navigation/`, `about/`,
  `theme/`.
- `SoundboardApp` — the composition root; owns the engine and repository.

## Audio Rules

These are the constraints most likely to be violated by a plausible-looking
change. Read `DECISIONS.md` before working in `audio/`.

- **`SoundboardEngine` is main-thread-only.** ExoPlayer instances may only be
  touched from the thread that built them. Don't move engine calls onto a
  background dispatcher.
- **Audio focus is held centrally by `AudioFocusHolder`**, and every player is
  built with `handleAudioFocus = false`. Don't let individual players manage
  focus — N players requesting focus fight each other.
- **The engine lives in `SoundboardApp`, not in `PlaybackService`.** The
  service mirrors engine state into a notification and nothing more. Don't
  move player ownership into it or add a binder.
- **Don't reach for `MediaSessionService`.** A `MediaSession` is 1:1 with a
  single `Player`; this app mixes N.
- **Never copy a picked file into app storage.** The app references user files
  by persisted SAF URI on purpose.

## Editing Rules

- Read target files, callers, and existing tests before editing.
- Keep edits surgical. Do not reformat unrelated files.
- Add or update tests (`app/src/test/`) when behavior changes.
- Update `docs/` when changing developer workflows or dependency pins.
- Update `gradle/libs.versions.toml` version comments/context when bumping a
  dependency deliberately; don't bump opportunistically outside of Renovate.
  Android lint's "newer version available" warnings are expected.
- Keep dependencies AndroidX/Kotlin-stdlib class — see `DECISIONS.md` →
  "GPL-3, Targeting F-Droid" before adding anything that isn't itself free
  software (no Google Play Services, no Firebase, no closed-source SDKs).
- No dependency injection framework is in use by design (see `DECISIONS.md`).
  Don't introduce Hilt/Koin without discussing it first.
- **Adding or removing a dependency means editing `DEPENDENCIES` in
  `ui/about/LicensesScreen.kt`.** That list is hand-maintained because the
  Play-services OSS-licences plugin isn't free software. A stale list is an
  F-Droid compliance problem, not a cosmetic one.
- About-page copy lives in `res/values/strings.xml` under `about_*`, not in
  `AboutScreen.kt`. Several values are still marked `TODO`.
- Prefer matching the sibling app `emotion-tracker` on shared conventions;
  where this repo diverges, `DECISIONS.md` says why.

## Validation

Before calling work complete, run the narrowest relevant checks:

```bash
./gradlew ktlintCheck
./gradlew testDebugUnitTest
```

For broader changes, or before opening a PR:

```bash
./gradlew check
```

`./gradlew check` runs lint, unit tests, and ktlint together. It does not
require an emulator. `./gradlew assembleDebug` additionally confirms the app
packages; `installDebug` and instrumented tests need a connected device or
emulator.

**A green build proves nothing about sound.** Unit tests cover pure logic
only — library mutations, persistence, and the dial's angle→percent maths.
Playback, looping, mixing, per-sound volume, audio focus, and the foreground
service are only verifiable on a device. When you change `audio/`, either run
the manual checks in `docs/development.md` → "Testing Audio Behavior" or say
plainly that you didn't.
