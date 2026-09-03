# Deployment

Releases are cut and published by GitHub Actions. Nothing is built or uploaded
from a workstation, and no signing material leaves GitHub secrets.

- `.github/workflows/release.yml` — the pipeline.
- `.github/workflows/pr-title.yml` — guards the commit subjects it reads.
- `scripts/sync-version.sh` — the one place the version numbers are derived.
- `scripts/fdroid-publish.sh` — the self-hosted F-Droid repository.

One-time account setup is in [Setting It Up](#setting-it-up) at the bottom.

## The Pipeline

```
merge a PR into main
  |
  '-> release-please grooms a "chore(main): release X.Y.Z" PR
      |  version.txt + CHANGELOG.md, from Conventional Commit subjects
      '-> CI tops it up with the files derived from those
          (versionCode, versionName, store release notes)
  |
  '-> you merge the release PR, when you want to ship
      |
      '-> tag vX.Y.Z + GitHub Release
          |
          +-> signed APK + SHA-256 attached to the Release
          +-> signed AAB -> Google Play, internal track
          '-> signed APK -> self-hosted F-Droid repo (gh-pages)
```

Merging to `main` never ships anything on its own. Merging the *release PR* is
the deliberate act, and it is the only one.

Both jobs live in one workflow run because a tag or PR created with
`GITHUB_TOKEN` does not start a new workflow run — a separate tag-triggered
publish workflow would look correct and never fire.

## Versioning

`version.txt` is the source of truth. release-please owns it, deriving the next
version from the Conventional Commit subjects merged since the last release:

| Commit subject | Effect |
| --- | --- |
| `feat: ...` | minor bump (`0.1.0` → `0.2.0`) |
| `fix: ...`, `perf: ...` | patch bump (`0.1.0` → `0.1.1`) |
| `feat!: ...`, or a `BREAKING CHANGE:` footer | minor bump while below 1.0.0, major after |
| `docs:`, `chore:`, `ci:`, `refactor:`, `test:`, `build:`, `style:` | no release on their own |

Because the repo is pre-1.0, `bump-minor-pre-major` keeps breaking changes at
`0.x` rather than jumping to `1.0.0`. Take that setting out of
`.release-please-config.json` when the app is ready to promise stability.

Everything else is derived by `scripts/sync-version.sh`:

```
version.txt
  -> app/build.gradle.kts   versionName, and versionCode
  -> fastlane/metadata/android/<locale>/changelogs/<versionCode>.txt
```

`versionCode` is computed, never chosen: `major * 1000000 + minor * 1000 +
patch`. It rises with any semver bump and stays far below Play's
2 147 483 647 ceiling. Play permanently refuses a `versionCode` it has already
seen, in that app, forever — including one from a deleted release — so this is
the one number in the project that must never be fudged.

Both stay plain integer/string literals on their own lines in
`app/build.gradle.kts` because F-Droid's update bot regex-parses them straight
out of that file to notice a new version. Replacing them with an expression,
however tidy, silently ends automatic F-Droid releases.

Run `./scripts/sync-version.sh` after changing `version.txt` by hand.
`./scripts/sync-version.sh --check` runs in CI and fails on drift.

## Signing

There are three signatures in play, and they are not interchangeable.

| Channel | Signed by | Interchangeable with |
| --- | --- | --- |
| GitHub Release APK | our release key | the self-hosted F-Droid repo, and Play if the same key was uploaded to Play App Signing |
| Self-hosted F-Droid repo | our release key | as above |
| Google Play | Play App Signing | only itself, unless we supplied the key |
| f-droid.org | F-Droid's own key | nothing else |

Android refuses to update an installed app with a differently-signed APK, so a
user moving between two channels with different keys has to uninstall first and
loses their board. Two consequences worth taking seriously:

- **Upload our own key to Play App Signing** at app-creation time. Google
  otherwise generates the app signing key itself, and Play builds stop being
  interchangeable with the ones we publish. This choice is permanent.
- **f-droid.org builds can never match.** F-Droid builds from source and signs
  with its own key, by design. The way out is F-Droid's
  [reproducible builds](https://f-droid.org/docs/Reproducible_Builds/) process,
  where F-Droid verifies its build matches ours bit-for-bit and ships our
  signature instead. Worth pursuing later; it is not a prerequisite.

CI gets the key from `RELEASE_KEYSTORE_BASE64` and friends (see
`docs/secrets.md`) and passes them to Gradle as `SOUNDBOARD_KEYSTORE_*`
environment variables. Locally, `keystore.properties` does the same job — see
`keystore.properties.example`. With neither, a release build is simply
unsigned, which is exactly what f-droid.org's build server wants.

## Google Play

CI uploads the AAB to the **internal** track and stops there. Promotion to
production is a manual step in the Play Console, on purpose: a bad build never
reaches users automatically, and Google's review queue is not hit on every
merge.

Release notes come from the same `fastlane/` files F-Droid reads, renamed at
upload time into the `whatsnew-<locale>` layout Play expects. The R8 mapping
file is uploaded too, without which every Play crash report is an unreadable
obfuscated stack trace.

Play needs store graphics — icon, feature graphic, screenshots — that this
pipeline does not upload. They are set once in the Play Console UI. The text is
in `fastlane/metadata/android/en-US/`; paste it in from there so the listings
stay in step.

## F-Droid

Two independent paths, deliberately. They are not alternatives — running both
means the app is discoverable in F-Droid's own index *and* users get new
versions the moment CI finishes.

### Self-hosted repository (automated)

`scripts/fdroid-publish.sh` maintains a real F-Droid repository on the
`gh-pages` branch, served by GitHub Pages:

```
https://508-dev.github.io/soundboard/fdroid/repo
```

An F-Droid repository is cumulative — every version ever published stays in the
index — which is why this clones the branch and adds to it rather than deploying
a freshly built directory. `gh-pages` *is* the store.

The index is signed with the app's release key, and the resulting fingerprint is
what clients pin. `index.html` on that branch publishes the URL and fingerprint
together, because adding a repository without one is trust-on-first-use.
Rotating the release key therefore forces every user to re-add the repository —
the same blast radius as rotating it for the app itself.

`fdroid/config.yml` and `fdroid/metadata/dev.co508.soundboard.yml` are the
inputs. Store copy is *not* duplicated there: it is copied out of `fastlane/`,
so Play and both F-Droid paths render from the same source.

### f-droid.org (one-time submission, then automatic)

f-droid.org builds and signs on its own infrastructure. Nothing can be pushed
to it from GitHub Actions, ever. The interaction is a single merge request:

1. Fork <https://gitlab.com/fdroid/fdroiddata>.
2. Copy `fdroid/fdroiddata/dev.co508.soundboard.yml` to
   `metadata/dev.co508.soundboard.yml` in the fork. Don't add a header comment
   to it — see `fdroid/fdroiddata/README.md` for why.
3. Run `fdroid rewritemeta` against the fork and confirm it produces no diff
   before opening the MR — their CI fails the job otherwise. The command is in
   `fdroid/fdroiddata/README.md`.
4. Open the merge request, following the current
   [Quick Start guide](https://f-droid.org/docs/Submitting_to_F-Droid_Quick_Start_Guide/)
   — check their docs rather than trusting this file to stay current.

Once merged, `UpdateCheckMode: Tags` makes their bot watch this repo for new
`v*` tags and `AutoUpdateMode: Version` makes it read the version literals out
of `app/build.gradle.kts` at that tag and add a build entry itself. From then
on, tagging a release is the whole interaction. Expect days of lag: their build
cycle is not ours.

**Two things to verify before submitting**, because both would fail on their
builder rather than ours:

- `gradle/gradle-daemon-jvm.properties` pins the Gradle daemon to Java 25 and
  supplies Foojay download URLs. If F-Droid's builder does not have that JDK and
  cannot fetch one, the build fails there while passing everywhere else. The fix
  is to drop the pin.
- `./gradlew assembleRelease` must work from a clean checkout with no
  `keystore.properties` and no network beyond declared Gradle dependencies.

## Cutting A Release By Hand

If the automation is broken and something has to ship:

```bash
# 1. Set the version and regenerate everything derived from it.
printf '0.2.0\n' > version.txt
./scripts/sync-version.sh
# 2. Commit, tag, push. The tag is what F-Droid watches.
git commit -am "chore(main): release 0.2.0" && git tag v0.2.0 && git push --follow-tags
# 3. Build signed artifacts (needs a local keystore.properties).
./gradlew assembleRelease bundleRelease
```

Then attach `app/build/outputs/apk/release/app-release.apk` to a GitHub Release
with its SHA-256, and upload the AAB in the Play Console. Update
`.release-please-manifest.json` to match, or the next automated release will
try to reuse the version.

Release builds run R8 (`isMinifyEnabled`, `isShrinkResources`). **Smoke-test a
release build on a device before promoting it in Play** — shrinking is the one
build difference that can break playback without failing the build. `./gradlew
check` proves nothing about sound.

## Out Of Scope

No backend, no server deploy, no environment-specific config, no rollback
beyond publishing a new version. The app makes no network calls and requests no
`INTERNET` permission.

---

# Setting It Up

Everything below is done once. Until the secrets exist the pipeline degrades
quietly rather than failing: with no signing key it tags and writes release
notes but publishes nothing, and with no Play credentials it still ships to
GitHub Releases and F-Droid.

## 1. Repository settings

- **Settings → General → Pull Requests**: enable *Allow squash merging*, and set
  the squash commit message to **"Pull request title"**. release-please reads
  those subjects; `pr-title.yml` enforces their shape.
- **Settings → Actions → General → Workflow permissions**: allow GitHub Actions
  to create and approve pull requests.
- **Settings → Pages**: set the source to the `gh-pages` branch, root. The
  branch does not exist yet — the first release creates it. Come back after.

## 2. The release signing key

This key is the app's identity. Losing it means never being able to update the
app again for anyone who installed it from a channel it signed.

```bash
keytool -genkeypair -v -keystore release.keystore -alias soundboard \
  -keyalg RSA -keysize 2048 -validity 10000
base64 -w0 release.keystore   # the value for RELEASE_KEYSTORE_BASE64
```

Back up `release.keystore` and its passwords somewhere durable and offline —
a password manager, not this repo. Then add, under **Settings → Secrets and
variables → Actions**:

| Secret | Value |
| --- | --- |
| `RELEASE_KEYSTORE_BASE64` | the `base64 -w0` output above |
| `RELEASE_KEYSTORE_PASSWORD` | the password you set |
| `RELEASE_KEY_ALIAS` | `soundboard` |
| `RELEASE_KEY_PASSWORD` | **the same password again** |

The last two passwords are the same value. Keystores default to PKCS12 format,
which has no per-entry passwords — `keytool` warns "Different store and key
passwords not supported for PKCS12 KeyStores" and discards a separate
`-keypass`, so the store password is what unlocks the key. The two settings
survive from the older JKS format, which did support them separately, and
Gradle still requires both to be supplied.

## 3. A token for release-please (recommended)

A pull request opened with the default `GITHUB_TOKEN` does not trigger CI, so
the release PR would arrive with no checks on it. Create a fine-grained personal
access token scoped to this repository with **Contents: read and write** and
**Pull requests: read and write**, and add it as `RELEASE_PLEASE_TOKEN`. The
workflow falls back to `GITHUB_TOKEN` without it.

## 4. Google Play

1. Create a **Play Console** account (one-time US$25). Use an *organization*
   account for 508.dev: personal accounts opened recently must run a closed test
   with 12 testers for 14 continuous days before they can apply for production
   access. Verify the current policy — Google changes it.
2. Create the app: package name `dev.co508.soundboard`, and fill in the store
   listing from `fastlane/metadata/android/en-US/`.
3. **Choose Play App Signing with your own key** and upload the keystore from
   step 2 — see [Signing](#signing). This is permanent.
4. Upload one AAB by hand. The Play Developer API cannot create an app or
   publish to a track that has never received a manual upload, so the first
   release must be manual regardless.
5. Create a Google Cloud service account, give it a JSON key, and grant it
   access in **Play Console → Users and permissions** with *Release apps to
   testing tracks*.
6. Add the whole JSON file contents as the `PLAY_SERVICE_ACCOUNT_JSON` secret.

## 5. Baseline the version

release-please finds the last release by tag. Without one it walks back through
all history and writes a first changelog covering everything. Tag the current
state once, after the setup commit lands on `main`:

```bash
git tag v0.1.0 && git push origin v0.1.0
```

## 6. F-Droid

The self-hosted repository needs nothing beyond step 1 — the first release
creates `gh-pages`, and the run log prints the repository URL and fingerprint to
publish. Submitting to f-droid.org is the separate, later step described in
[F-Droid](#f-droid) above.

## Still To Do

- Store graphics: a 512×512 PNG icon, a 1024×500 feature graphic, and at least
  two phone screenshots. Play requires them; F-Droid reads them from
  `fastlane/metadata/android/en-US/images/` (`icon.png`,
  `phoneScreenshots/*.png`) if they are committed there.
- Confirm the licence is GPL-3.0-**only** and not `-or-later`. Both F-Droid
  metadata files claim `GPL-3.0-only`, chosen as the conservative reading
  because nothing in the repo says "or later". If that is wrong, fix both files
  and say so in `README.md`.

## Agent Notes

- Never hand-edit `versionCode`, `versionName`, or `version.txt`. Change
  `version.txt` only when deliberately overriding a release, and always re-run
  `./scripts/sync-version.sh`.
- Do not turn the version literals in `app/build.gradle.kts` into an expression,
  and do not move them onto a shared line. F-Droid parses them textually.
- Do not add publishing to a new store, or promote Play uploads past the
  internal track, without the maintainer asking. Release signing material and
  store submissions are deliberate human decisions here.
