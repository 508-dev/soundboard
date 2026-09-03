# Contributing

## Local Checks

Run the narrowest relevant checks while iterating:

```bash
./gradlew ktlintCheck
./gradlew testDebugUnitTest
```

Before opening or updating a PR, run:

```bash
./gradlew check
```

`./scripts/lint.sh`, `./scripts/test.sh`, and `./scripts/check-all.sh` are
thin wrappers around the same `./gradlew` tasks, kept for consistency with
CI. Use whichever is convenient.

## Checking Audio Changes

`./gradlew check` cannot tell you whether the app makes the right sound.
Anything touching `audio/` — playback, looping, volume, audio focus, the
foreground service — needs a manual pass on a real device. `docs/development.md`
→ "Testing Audio Behavior" lists the scenarios; say in the PR which ones you
ran.

## Pull Requests

Use the PR template. Include what changed, why, and how it was validated.

**Title your PR as a Conventional Commit.** PRs are squash-merged, so the title
becomes the commit subject on `main`, and release-please reads those subjects to
decide the next version and write the changelog. A title it cannot parse costs a
changelog entry, so CI checks the shape:

```
feat: fade sounds in when playback starts        -> minor version bump
fix(audio): keep focus when a call interrupts    -> patch version bump
feat!: drop support for Android 8                -> breaking change
chore(deps): bump media3 to 1.11.0               -> no release on its own
```

`feat`, `fix`, `perf`, `revert`, `refactor`, `docs`, `test`, `build`, `ci`,
`chore`, and `style` are accepted. `docs/deployment.md` explains what each one
does to the version.

## Versions

Never edit `versionCode` or `versionName` in `app/build.gradle.kts`, and don't
edit `CHANGELOG.md`. Both are generated — `version.txt` is the source of truth
and release-please owns it. If you do change `version.txt` deliberately, run
`./scripts/sync-version.sh` and commit what it regenerates; CI fails on drift.

Avoid committing local state such as build outputs, `local.properties`,
`keystore.properties`, `.idea/`, and `.context/`.

## Agent Notes

- Keep convention changes paired with docs updates (`docs/`, `README.md`).
- Validate with `./gradlew check` before treating a change as complete, and
  be explicit about what a green build does *not* prove for audio changes.
