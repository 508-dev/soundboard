#!/usr/bin/env bash
# Propagate version.txt into the files that must carry a literal version.
#
#   version.txt                                  <- release-please owns this
#     -> app/build.gradle.kts                       versionName + versionCode
#     -> fastlane/metadata/android/en-US/changelogs/<versionCode>.txt
#
# versionCode is derived, never chosen: major * 1000000 + minor * 1000 + patch.
# That keeps it strictly increasing across any semver bump (Play rejects a
# versionCode that does not increase, permanently), while staying far below
# Play's 2100000000 ceiling. Both values stay plain literals on their own lines
# because F-Droid's update bot regex-parses them out of build.gradle.kts to
# decide there is a new version to build — an expression it cannot parse means
# no automatic F-Droid releases. See docs/deployment.md.
#
# Usage:
#   scripts/sync-version.sh            rewrite the derived files
#   scripts/sync-version.sh --check    fail if they are out of date (CI)
#   scripts/sync-version.sh --print    emit `version=`/`version_code=` for eval
#
# --print exists so nothing else has to reimplement the formula above. Callers
# do: eval "$(scripts/sync-version.sh --print)".
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."

mode=${1:-write}
case $mode in
    write | --check | --print) ;;
    *)
        echo "usage: ${BASH_SOURCE[0]##*/} [--check|--print]" >&2
        exit 2
        ;;
esac

version=$(tr -d '[:space:]' < version.txt)
[[ $version =~ ^([0-9]+)\.([0-9]+)\.([0-9]+)$ ]] || {
    echo "version.txt must hold a bare MAJOR.MINOR.PATCH version, got: '$version'" >&2
    exit 1
}
major=${BASH_REMATCH[1]} minor=${BASH_REMATCH[2]} patch=${BASH_REMATCH[3]}

if ((minor > 999 || patch > 999)); then
    echo "minor/patch above 999 would collide in the versionCode formula" >&2
    exit 1
fi
version_code=$((major * 1000000 + minor * 1000 + patch))

if [[ $mode == --print ]]; then
    printf 'version=%s\nversion_code=%s\n' "$version" "$version_code"
    exit 0
fi

gradle_file=app/build.gradle.kts
changelog_file="fastlane/metadata/android/en-US/changelogs/${version_code}.txt"

# --- app/build.gradle.kts ----------------------------------------------------
gradle_updated=$(sed -E \
    -e "s/^([[:space:]]*)versionCode = [0-9]+$/\1versionCode = ${version_code}/" \
    -e "s/^([[:space:]]*)versionName = \".*\"$/\1versionName = \"${version}\"/" \
    "$gradle_file")

# A typo'd or reformatted literal would silently no-op the sed above, so prove
# the result actually says what we intended rather than trusting the rewrite.
if ! grep -qE "^[[:space:]]*versionCode = ${version_code}$" <<<"$gradle_updated" ||
    ! grep -qE "^[[:space:]]*versionName = \"${version}\"$" <<<"$gradle_updated"; then
    echo "could not set the version literals in $gradle_file." >&2
    echo "They must each be a plain literal on its own line." >&2
    exit 1
fi

# --- release notes -----------------------------------------------------------
# Play and F-Droid both read a plain-text, <=500-char changelog keyed by
# versionCode. Render it from the top section of the CHANGELOG release-please
# generates, stripping the Markdown they would show verbatim.
render_changelog() {
    if [[ ! -f CHANGELOG.md ]]; then
        echo "Version ${version}."
        return
    fi
    awk '
        /^## / { if (seen++) exit; next }   # start at section 1, stop at section 2
        seen                                 # print the body in between
    ' CHANGELOG.md |
        sed -E \
            -e 's/^#+ +(.*)$/\1:/' \
            -e 's/^\* /- /' \
            -e 's/ ?\(\[[^]]*\]\([^)]*\)\)//g' \
            -e 's/\[([^]]*)\]\([^)]*\)/\1/g' \
            -e 's/[[:space:]]+$//' |
        cat -s |
        sed -e '/./,$!d' |
        head -c 500
}
changelog_body=$(render_changelog)
[[ -n ${changelog_body//[[:space:]]/} ]] || changelog_body="Version ${version}."

# --- write or verify ---------------------------------------------------------
if [[ $mode == --check ]]; then
    status=0
    diff -u "$gradle_file" - <<<"$gradle_updated" || status=1
    if [[ ! -f $changelog_file ]]; then
        echo "missing release notes: $changelog_file" >&2
        status=1
    elif ! diff -u "$changelog_file" - <<<"$changelog_body"; then
        status=1
    fi
    if ((status != 0)); then
        echo >&2
        echo "Derived version files are stale. Run: scripts/sync-version.sh" >&2
    fi
    exit $status
fi

printf '%s\n' "$gradle_updated" > "$gradle_file"
mkdir -p "$(dirname "$changelog_file")"
printf '%s\n' "$changelog_body" > "$changelog_file"
echo "version ${version} -> versionCode ${version_code}, ${changelog_file}"
