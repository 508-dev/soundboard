# Dependency Supply-Chain Policy

## Renovate

`renovate.json` sets `minimumReleaseAge: "7 days"` — Renovate holds a new
dependency release for 7 days before opening a PR for it, giving the
ecosystem time to catch and report problems. Renovate understands Gradle
version catalogs natively, so it opens PRs directly against
`gradle/libs.versions.toml` without extra configuration.

Unlike Bun/uv/pnpm/Bundler (the devkit's other package managers), Gradle has
no first-party cooldown flag of its own — Renovate's `minimumReleaseAge` is
the enforcement point for this project, not a per-tool setting.

## Locked, Verified Builds

- The Gradle wrapper (`gradle/wrapper/gradle-wrapper.properties`) pins an
  exact Gradle version with `distributionSha256Sum`, so CI and every
  contributor build with the same, integrity-checked Gradle — never a
  floating "latest."
- `gradle/libs.versions.toml` pins every dependency to an exact version
  (Gradle version catalogs don't support ranges the way some ecosystems do
  by convention, and this repo doesn't opt into ranges).
- CI builds from the committed wrapper and version catalog only — no
  network-resolved "whatever's current" dependency graph.

## Bumping A Pinned Version

Prefer letting Renovate open the PR (it respects the 7-day cooldown
automatically). If bumping by hand — e.g. an urgent fix — is unavoidable:

1. Check the new version's release notes for breaking changes.
2. Update the version in `gradle/libs.versions.toml`.
3. Run `./gradlew check assembleDebug`.
4. Update `docs/tooling.md`'s pinned-version table and its "set" date.
5. Note the exception (why it skipped the cooldown) in the PR.

## Security Workflows

Opt-in, not default:

- `extras/github/gitleaks.yml.example` — CI secret scanning. Add only when
  ready to triage baseline/historic findings.
- `extras/github/dependency-review.yml.example` — GitHub dependency-graph
  based vulnerability/license/change reporting on PRs. Requires the repo's
  dependency graph to be enabled.

## Free-Software Constraint

See `docs/tooling.md` → "Free-Software Constraint (F-Droid)" and
`DECISIONS.md` → "GPL-3, Targeting F-Droid." A dependency that isn't itself
free software is a supply-chain problem specific to this project, not just a
license-hygiene nit — it can block F-Droid distribution entirely.
