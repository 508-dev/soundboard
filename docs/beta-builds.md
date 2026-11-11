# Beta Builds

Every push to a branch other than `main`, `gh-pages`, or a
`release-please--...` branch builds **Soundboard Beta**. The workflow can also
be started manually with **Run workflow**. It publishes a GitHub pre-release
containing one APK, then removes older beta releases for that branch. Deleting
a branch removes its beta releases once `beta.yml` is on the default branch.

Betas use the application ID `dev.co508.soundboard.beta`, an orange launcher
background, and a separate signing key, so they install beside the production
app. Each build uses the workflow run number as its `versionCode`; its
`versionName` is `<version.txt>-beta.<run>+<short sha>`. A new tag per build
lets update clients distinguish successive betas.

## Install

Download the latest APK for your branch from
<https://github.com/508-dev/soundboard/releases> and open it on your phone.
Beta releases are marked **Pre-release** and their titles begin with `Beta`.

### Obtainium

1. Add the app from `https://github.com/508-dev/soundboard`.
2. Turn on **Include prereleases**.
3. Set **Filter APKs by regular expression** to `soundboard-beta`.
4. Set **Filter release titles by regular expression** to `^Beta`.

The APK filter prevents Obtainium from selecting the production APK attached
to a stable release. The title filter keeps it on beta pre-releases. Obtainium
will notify you when a new beta is published and can install it over the
previous beta while the signing key remains the same.

## Signing And Secrets

The repository secrets `BETA_KEYSTORE_BASE64`, `BETA_KEYSTORE_PASSWORD`,
`BETA_KEY_ALIAS`, and `BETA_KEY_PASSWORD` already exist. They belong only to
the beta app; the production signing key and its `Prod` environment are not
used by this workflow. If the beta key is lost, beta users must uninstall and
reinstall Soundboard Beta rather than update it in place.

On a fork, or if the keystore secret is unavailable, the workflow still runs
`./gradlew assembleBeta` as a compile check and skips publishing. Local beta
builds without signing environment variables use the debug key.
