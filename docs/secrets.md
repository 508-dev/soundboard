# Secrets

The app itself has no API keys, no backend credentials, and no runtime
configuration — it makes no network calls, so there is no `.env` contract here
at all. Every secret in this project exists to publish releases, and all of it
lives in GitHub Actions secrets. See `docs/deployment.md` for how each one is
obtained.

## GitHub Actions Secrets

| Secret | Used for | Without it |
| --- | --- | --- |
| `RELEASE_KEYSTORE_BASE64` | the app signing key, `base64 -w0` of the keystore | nothing is published; the release is still tagged |
| `RELEASE_KEYSTORE_PASSWORD` | keystore password | as above |
| `RELEASE_KEY_ALIAS` | key alias within the keystore | as above |
| `RELEASE_KEY_PASSWORD` | same value as `RELEASE_KEYSTORE_PASSWORD` — PKCS12 keystores have no separate key password | as above |
| `PLAY_SERVICE_ACCOUNT_JSON` | Play Developer API service account, whole JSON file | Play is skipped; GitHub Releases and F-Droid still publish |
| `RELEASE_PLEASE_TOKEN` | fine-grained PAT so the release PR triggers CI | falls back to `GITHUB_TOKEN`; the release PR arrives unchecked |

The signing key is reused to sign the self-hosted F-Droid repository index. One
key means one set of secrets to protect and rotate, at the cost of coupling two
things that could have failed independently: rotating it invalidates the
repository fingerprint users pinned *and* breaks app updates. Since both
failures already require every user to re-add or reinstall, coupling them costs
little. Split them if that ever stops being true.

**The signing key cannot be rotated in practice.** Android refuses to update an
installed app with a differently-signed APK. Losing the keystore or its password
means every existing user has to uninstall and reinstall, losing their board.
Back it up offline and off this machine.

## Release Signing Locally

1. Generate a keystore (see the command in `keystore.properties.example`).
2. Copy `keystore.properties.example` to `keystore.properties` and fill in
   the real values.
3. Never commit `keystore.properties` or the keystore file itself — both are
   gitignored. If either is ever accidentally committed, rotate the signing
   key (generate a new keystore); scrubbing git history is not sufficient,
   same as any other leaked credential.

Debug builds (`./gradlew assembleDebug`/`installDebug`) don't need any of
this — they use Android's auto-generated debug keystore.

CI never writes a `keystore.properties`. It passes the same four values as
`SOUNDBOARD_KEYSTORE_FILE`, `SOUNDBOARD_KEYSTORE_PASSWORD`,
`SOUNDBOARD_KEY_ALIAS`, and `SOUNDBOARD_KEY_PASSWORD` environment variables,
which `app/build.gradle.kts` reads when no properties file is present. With
neither source configured, release builds are unsigned — which is what
f-droid.org's build server wants, since it signs its own binary.

## User Data Is Not Ours To Leak

The app holds URIs pointing at the user's own audio files, plus the read
grants for them. Treat a user's `sound_library.json` from a bug report as
personal data: it lists file paths and names from their device. Don't paste
one into an issue without the user scrubbing it first.

## Agent Notes

- Never print the contents of `keystore.properties` or a keystore file, and
  never echo a `SOUNDBOARD_KEYSTORE_*` or `FDROID_*` value in a workflow step.
- If a real `keystore.properties` or `.jks`/`.keystore` file shows up staged
  for commit, stop and flag it rather than committing it.
- `scripts/fdroid-publish.sh` stages an `fdroid/config.yml` that resolves the
  signing passwords on disk. It commits explicit paths, never `git add -A`, and
  writes a `.gitignore` next to it. Keep both properties if you touch it.
- Report a security concern to caleb@508.dev (see `SECURITY.md`), not a
  public issue.
