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

Avoid committing local state such as build outputs, `local.properties`,
`keystore.properties`, `.idea/`, and `.context/`.

## Agent Notes

- Keep convention changes paired with docs updates (`docs/`, `README.md`).
- Validate with `./gradlew check` before treating a change as complete, and
  be explicit about what a green build does *not* prove for audio changes.
