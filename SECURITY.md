# Security Policy

## Reporting Vulnerabilities

Do not open public issues for vulnerabilities or leaked secrets. Report
security concerns to caleb@508.dev.

## Data Handling

Soundboard stores a single local JSON document listing the sounds on the
board: for each one, the `content://` URI the user picked, a display name,
and a volume percentage. It never copies, moves, or modifies the audio files
themselves.

The app requests no `INTERNET` permission and makes no network calls, so
there is no server-side attack surface, no telemetry, and nothing transmitted
off-device.

Read access to picked files comes from the Storage Access Framework: the app
holds a persistable read grant per file, taken when the sound is added and
released when it is removed. It never requests broad storage permissions
(`READ_EXTERNAL_STORAGE`/`READ_MEDIA_AUDIO`), so its access is limited to the
specific files the user chose.

The board is excluded from Android cloud backup and device transfer, because
those URI grants are only valid on the install that took them.

## Secret Handling

- The only local secret is release-signing key material. Keep it in
  `keystore.properties` (gitignored; see `keystore.properties.example`) and
  a keystore file, never committed.
- Never commit a real `keystore.properties`, `.jks`/`.keystore` file, or
  signing passwords.

## Dependency Policy

- Renovate opens dependency PRs on a 7-day `minimumReleaseAge` cooldown (see
  `renovate.json`), covering Gradle dependencies via
  `gradle/libs.versions.toml`.
- CI builds and tests against the committed Gradle wrapper
  (`gradle/wrapper/gradle-wrapper.properties`), pinned with a `distributionSha256Sum`.
- Keep dependencies free-software (AndroidX/Kotlin-stdlib class) per
  `DECISIONS.md` → "GPL-3, Targeting F-Droid" — this is a licensing
  requirement for F-Droid distribution, not just a preference.

## GitHub Actions

Workflows use least-privilege permissions, pinned action SHAs, and
`persist-credentials: false` where practical. `extras/github/gitleaks.yml.example`
and `extras/github/dependency-review.yml.example` are opt-in if the project
wants CI secret scanning or dependency graph reporting.
