# f-droid.org Submission

`dev.co508.soundboard.yml` in this directory is the payload for a one-time
merge request to [gitlab.com/fdroid/fdroiddata](https://gitlab.com/fdroid/fdroiddata),
copied there as `metadata/dev.co508.soundboard.yml`. See `docs/deployment.md`
→ "F-Droid" for the submission steps.

**The metadata file itself carries no comments on purpose.** fdroiddata's CI
runs `fdroid rewritemeta` on any changed file and fails the job if that
produces a diff — and `rewritemeta` unconditionally strips every leading
comment. A header explaining the file lasts exactly one rewrite pass before
their own lint deletes it and breaks CI. Keep explanatory context here
instead.

## Why it's shaped this way

- **Nothing in this repo pushes to f-droid.org.** It builds and signs on its
  own infrastructure, on its own schedule, with its own key. This file is
  read once at merge time and from then on only by their bot.
- **`UpdateCheckMode: Tags`** makes that bot watch this repo for new `v*` tags.
- **`AutoUpdateMode: Version`** (no tag pattern after it) makes it read
  `versionName`/`versionCode` out of `app/build.gradle.kts` at that tag and
  append a new `Builds:` entry itself — the reason those two values have to
  stay plain literals in that file, not an expression. An older syntax,
  `Version v%v`, supplied a commit-tag pattern; it's not valid on current
  fdroiddata schemas, and it was always redundant under `UpdateCheckMode:
  Tags` anyway — `checkupdates` already knows the tag and assigns
  `commit: <tag>` directly, ignoring the pattern.
- **No `Summary`/`Description` fields.** F-Droid reads
  `fastlane/metadata/android/` from this repo instead, so the store copy has
  one source rather than three.

## Before submitting

Run against a real fdroiddata checkout, not just schema validation — CI runs
`fdroid rewritemeta` and fails on any diff it produces, which schema
validation alone won't catch:

```bash
python3 -m venv /tmp/fdroid-venv && /tmp/fdroid-venv/bin/pip install fdroidserver
mkdir -p /tmp/fdroiddata-check/metadata
cp fdroid/fdroiddata/dev.co508.soundboard.yml /tmp/fdroiddata-check/metadata/
printf 'repo_url: https://example.org/fdroid/repo\nrepo_name: check\n' > /tmp/fdroiddata-check/config.yml
chmod 600 /tmp/fdroiddata-check/config.yml
cd /tmp/fdroiddata-check && /tmp/fdroid-venv/bin/fdroid rewritemeta dev.co508.soundboard
diff -u /path/to/soundboard/fdroid/fdroiddata/dev.co508.soundboard.yml metadata/dev.co508.soundboard.yml
```

A clean diff means the file is already in canonical form.
