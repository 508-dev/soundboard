# Deployment

## Target: F-Droid

The goal is a listing on F-Droid (or a comparable FOSS store). F-Droid builds
the app from source on its own infrastructure using metadata you submit to the
`fdroiddata` repo — it does not run the APK you build locally. That means:

- The build must be reproducible from a clean checkout of this repo with
  `./gradlew assembleRelease` (no manual steps, no network calls during
  build beyond fetching declared Gradle dependencies).
- Every dependency must itself be free software (see `docs/tooling.md` →
  "Free-Software Constraint").
- F-Droid signs the APK it builds and distributes; the app's own release
  signing config (`keystore.properties`) is not used for that build.

Submission process (once the MVP is ready): follow F-Droid's
["Submit metadata to Fdroiddata"](https://f-droid.org/docs/Submitting_to_F-Droid_Quick_Start_Guide/)
guide — check current requirements there rather than trusting this doc to
stay current on F-Droid's own process.

## Secondary Path: GitHub Releases APK

Until an F-Droid submission is accepted, a signed release APK attached to a
GitHub Release is a reasonable way to let people side-load the app:

1. Generate a keystore and `keystore.properties` locally (see
   `keystore.properties.example`) — do not commit either.
2. `./gradlew assembleRelease` produces a signed APK at
   `app/build/outputs/apk/release/`.
3. Attach it to a GitHub Release. Communicate the SHA-256 of the APK in the
   release notes so people can verify what they installed.

Release builds run R8 (`isMinifyEnabled`/`isShrinkResources`). Smoke-test a
release build on a device before publishing — shrinking is the one build
difference that can break playback without failing the build.

## What's Explicitly Out Of Scope

No backend, no server deploy, no environment-specific config, no rollback
strategy beyond "publish a new version." The app has no network calls at all
and requests no `INTERNET` permission.

## Agent Notes

- Do not add a deploy CI workflow (auto-publishing to F-Droid or cutting
  GitHub Releases) without the maintainer explicitly asking — release
  signing material and F-Droid submission are deliberate, infrequent, human
  actions here, not something to automate by default.
