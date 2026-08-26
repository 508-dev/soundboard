# Secrets

There is exactly one class of secret in this repo: release-signing key
material, used only when producing a signed release APK (see
`docs/deployment.md`). The app itself has no API keys, no backend
credentials, and no runtime configuration — it makes no network calls, so
there is no `.env` contract here at all.

## Release Signing

1. Generate a keystore (see the command in `keystore.properties.example`).
2. Copy `keystore.properties.example` to `keystore.properties` and fill in
   the real values.
3. Never commit `keystore.properties` or the keystore file itself — both are
   gitignored. If either is ever accidentally committed, rotate the signing
   key (generate a new keystore); scrubbing git history is not sufficient,
   same as any other leaked credential.

Debug builds (`./gradlew assembleDebug`/`installDebug`) don't need any of
this — they use Android's auto-generated debug keystore.

## User Data Is Not Ours To Leak

The app holds URIs pointing at the user's own audio files, plus the read
grants for them. Treat a user's `sound_library.json` from a bug report as
personal data: it lists file paths and names from their device. Don't paste
one into an issue without the user scrubbing it first.

## Agent Notes

- Never print the contents of `keystore.properties` or a keystore file.
- If a real `keystore.properties` or `.jks`/`.keystore` file shows up staged
  for commit, stop and flag it rather than committing it.
- Report a security concern to caleb@508.dev (see `SECURITY.md`), not a
  public issue.
