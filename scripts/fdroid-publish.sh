#!/usr/bin/env bash
# Publish a signed release APK into the self-hosted F-Droid repository that
# lives on this repo's `gh-pages` branch and is served by GitHub Pages.
#
#   gh-pages/
#     index.html                  landing page: repo URL + fingerprint
#     fdroid/repo/*.apk           every release ever published
#     fdroid/repo/index-v*.{jar,json}   the signed index clients fetch
#     fdroid/metadata/            app metadata + fastlane store copy
#
# The branch is the store: an F-Droid repo is cumulative, so old APKs have to
# survive each run. That's why this clones the branch and adds to it rather
# than deploying a freshly built directory.
#
# Usage: scripts/fdroid-publish.sh <path-to-release.apk>
#
# Requires `fdroid` (fdroidserver) on PATH, a JDK for jarsigner, ANDROID_HOME
# for apksigner, and:
#   GH_TOKEN, GITHUB_REPOSITORY          push access to gh-pages
#   FDROID_KEYSTORE                      path to the index-signing keystore
#   FDROID_KEY_ALIAS
#   FDROID_KEYSTORE_PASS, FDROID_KEY_PASS
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."
repo_root=$PWD

apk=${1:?usage: scripts/fdroid-publish.sh <path-to-release.apk>}
[[ -f $apk ]] || { echo "no such APK: $apk" >&2; exit 1; }
apk=$(realpath "$apk")

: "${GH_TOKEN:?}" "${GITHUB_REPOSITORY:?}" "${FDROID_KEYSTORE:?}"
: "${FDROID_KEY_ALIAS:?}" "${FDROID_KEYSTORE_PASS:?}" "${FDROID_KEY_PASS:?}"

package_name=dev.co508.soundboard
# Single source for the version numbers — never recompute the versionCode here.
eval "$(scripts/sync-version.sh --print)"
pages_url="https://$(cut -d/ -f1 <<<"$GITHUB_REPOSITORY").github.io/$(cut -d/ -f2 <<<"$GITHUB_REPOSITORY")"
remote="https://x-access-token:${GH_TOKEN}@github.com/${GITHUB_REPOSITORY}.git"

workdir=$(mktemp -d)
trap 'rm -rf "$workdir"' EXIT
pages=$workdir/pages

# --- get the branch, or start it ---------------------------------------------
if git clone --quiet --depth 1 --branch gh-pages "$remote" "$pages" 2>/dev/null; then
    echo "==> cloned existing gh-pages"
else
    echo "==> gh-pages does not exist yet, starting it"
    git init --quiet --initial-branch=gh-pages "$pages"
    git -C "$pages" remote add origin "$remote"
fi
git -C "$pages" config user.name "github-actions[bot]"
git -C "$pages" config user.email "41898282+github-actions[bot]@users.noreply.github.com"

mkdir -p "$pages/fdroid/repo" "$pages/fdroid/metadata/$package_name"

# --- stage config, metadata, and the new APK ---------------------------------
# config.yml carries the signing material (indirected through {env:}), so it is
# staged but never committed — see the .gitignore written below.
cp "$repo_root/fdroid/config.yml" "$pages/fdroid/config.yml"
cp "$repo_root/fdroid/metadata/$package_name.yml" "$pages/fdroid/metadata/"
# fdroidserver attaches metadata/<pkg>/<locale>/changelogs/<code>.txt to a
# release only by matching <code> against the app's current version, and
# index-v2 additionally requires the newest Builds: entry to carry that same
# versionCode. Neither is inferred from the APK for a binary repo like this
# one. Pin both here rather than in the tracked metadata file, which would
# otherwise need editing on every release. Nothing builds from this Builds:
# entry — `fdroid update` only reads it.
# shellcheck disable=SC2154  # version/version_code come from the eval above
cat >> "$pages/fdroid/metadata/$package_name.yml" <<EOF

# Appended at publish time by scripts/fdroid-publish.sh.
CurrentVersion: $version
CurrentVersionCode: $version_code
Builds:
  - versionName: $version
    versionCode: $version_code
    commit: v$version
EOF
# Store copy lives in fastlane/ so Play and F-Droid read the same source.
# fdroidserver wants it under metadata/<packageName>/<locale>/.
cp -r "$repo_root/fastlane/metadata/android/." "$pages/fdroid/metadata/$package_name/"
cp "$apk" "$pages/fdroid/repo/"

# Without this fdroidserver generates a placeholder repo icon every run.
if [[ -f $repo_root/fastlane/metadata/android/en-US/images/icon.png ]]; then
    mkdir -p "$pages/fdroid/repo/icons"
    cp "$repo_root/fastlane/metadata/android/en-US/images/icon.png" \
        "$pages/fdroid/repo/icons/icon.png"
fi

# repo/status/ is fdroidserver's own run bookkeeping: it changes every run and
# records absolute runner paths. Nothing serves it to clients.
printf 'config.yml\n*.keystore\n*.jks\nrepo/status/\n' > "$pages/fdroid/.gitignore"
# Staged config.yml resolves the signing passwords; fdroidserver refuses to be
# quiet about group/world-readable permissions on it, and it is right to.
chmod 600 "$pages/fdroid/config.yml"

# --- build and sign the index -------------------------------------------------
# --use-date-from-apk keeps "Last updated" tied to the build rather than to
# whenever this script happened to run, so re-running it is a no-op.
echo "==> fdroid update"
(cd "$pages/fdroid" && fdroid update --create-metadata --pretty --use-date-from-apk)

# --- landing page --------------------------------------------------------------
# Clients pin the repo by fingerprint, so publish it next to the URL: adding the
# repo without one is trust-on-first-use.
# The fingerprint is the SHA-256 of the certificate the index is signed with.
# It is not written into the index itself, so read it back off the signed jar.
fingerprint=$(keytool -printcert -jarfile "$pages/fdroid/repo/index.jar" 2>/dev/null |
    sed -nE 's/^[[:space:]]*SHA256:[[:space:]]*([0-9A-Fa-f:]+)$/\1/p' |
    head -1 | tr -d ':' | tr '[:upper:]' '[:lower:]')
[[ -n $fingerprint ]] || echo "warning: could not read the index signing fingerprint" >&2

repo_url="$pages_url/fdroid/repo"
add_url=$repo_url${fingerprint:+?fingerprint=$fingerprint}

python3 - "$pages/index.html" "$repo_url" "$add_url" "$fingerprint" <<'PY'
import html, sys
out, repo_url, add_url, fingerprint = sys.argv[1:5]
fp = html.escape(fingerprint) or "(unavailable)"
with open(out, "w") as f:
    f.write(f"""<!doctype html>
<html lang="en">
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>508.dev Soundboard &mdash; F-Droid repository</title>
<style>
  :root {{ color-scheme: light dark; }}
  body {{ font: 16px/1.6 system-ui, sans-serif; max-width: 42rem; margin: 3rem auto; padding: 0 1.25rem; }}
  code {{ overflow-wrap: anywhere; }}
  pre {{ padding: .75rem; border: 1px solid currentColor; border-radius: .4rem; overflow-x: auto; }}
</style>
<h1>508.dev Soundboard</h1>
<p>An F-Droid repository with signed release builds of
<a href="https://github.com/508-dev/soundboard">508.dev Soundboard</a>.</p>
<h2>Add this repository</h2>
<p>In the F-Droid app: <strong>Settings &rarr; Repositories &rarr; +</strong>, then paste:</p>
<pre><code>{html.escape(add_url)}</code></pre>
<p>Repository URL: <code>{html.escape(repo_url)}</code><br>
Signing fingerprint (SHA-256): <code>{fp}</code></p>
<p>These builds are signed with the same key as the APKs attached to the
project's GitHub Releases, so you can switch between the two without
uninstalling. Builds from <em>f-droid.org</em> are signed by F-Droid instead and
are not interchangeable with these.</p>
<p><a href="https://github.com/508-dev/soundboard">Source code</a> &middot; GPL-3.0</p>
</html>
""")
PY

# --- commit ------------------------------------------------------------------
# Explicit paths, never `git add -A` and never --force: config.yml holds
# resolved secrets on disk at this point, and the .gitignore written above is
# the second line of defence that keeps it and any stray keystore out.
git -C "$pages" add \
    index.html fdroid/.gitignore fdroid/repo fdroid/metadata
if git -C "$pages" diff --cached --quiet; then
    echo "==> nothing changed, not pushing"
    exit 0
fi
git -C "$pages" commit --quiet -m "Publish $(basename "$apk")"
git -C "$pages" push --quiet origin gh-pages
echo "==> published to $repo_url"
echo "==> fingerprint ${fingerprint:-unknown}"
