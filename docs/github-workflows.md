# GitHub Workflows

Common GitHub hygiene, adopted from the 508 Devkit template this repo was
generated from and kept because it's stack-agnostic.

## Default Files

Root `.github/` files are meant to be safe defaults for most repositories:

- `.github/PULL_REQUEST_TEMPLATE.md`: prompts for summary, validation, risk, and screenshots.
- `.github/ISSUE_TEMPLATE/bug_report.yml`: captures reproducible defects.
- `.github/ISSUE_TEMPLATE/feature_request.yml`: captures product or workflow requests.
- `.github/ISSUE_TEMPLATE/docs_request.yml`: captures documentation gaps.
- `.github/ISSUE_TEMPLATE/config.yml`: keeps blank issues allowed and documents where to add discussion links.
- `.github/workflows/ci.yml`: version-file check, ktlint, unit tests, Android
  lint, and a debug APK build via `./gradlew`. See `docs/tooling.md` for the
  pinned toolchain.

Keep these templates short. They should improve issue and PR quality without making lightweight collaboration feel bureaucratic.

Workflows pin third-party actions to commit SHAs and use `harden-runner` in audit mode. When applying this devkit, update pinned SHAs intentionally rather than floating back to moving tags.

Plain checkout-and-run validation jobs should usually keep `contents: read` only.
Do not add `pull-requests: read` unless the job explicitly calls PR APIs or
posts PR comments.

If path-filtered jobs are used with branch protection, require a final aggregate
job such as `ci-passed` instead of requiring every skipped job directly.
When using `dorny/paths-filter` without `pull-requests: read`, set `token: ""`
and check out enough history so the action uses git-based detection.

## Release Workflows

Not devkit defaults — these are specific to shipping this app, and
`docs/deployment.md` is the reference for them.

- `.github/workflows/release.yml`: release-please grooms a release PR on every
  merge to `main`; merging that PR tags a version and publishes to GitHub
  Releases, Google Play, and the self-hosted F-Droid repository. Both jobs are
  in one workflow because a tag or PR created with `GITHUB_TOKEN` does not
  trigger a new workflow run.
- `.github/workflows/pr-title.yml`: checks the PR title is a Conventional
  Commit, since squash-merging makes it the commit subject release-please reads.
  Deliberately a plain regex rather than a third-party action — a release-gating
  check is a poor place to add a supply-chain dependency.

`release.yml` is the only workflow here that needs write permissions or
secrets, and it scopes them per job.

## Security Extras

Secret scanning and Dependency Review are extras, not default workflows.

Use `extras/github/gitleaks.yml.example` only when maintainers want CI history
scanning for secrets and are ready to triage baseline findings or false
positives.

Use `extras/github/dependency-review.yml.example` only when maintainers want
GitHub dependency graph-based vulnerability, license, or dependency-change
reporting.

## Dependency Review

Dependency Review is not part of the default security workflow. It depends on
GitHub's dependency graph and is best treated as vulnerability, license, and
dependency-change reporting, not as the primary active supply-chain attack
detector.

Use `extras/github/dependency-review.yml.example` only when the repo owner wants
GitHub dependency graph-based reporting. To enable it in a target repo:

1. Enable GitHub's dependency graph for the repository.
2. Copy `extras/github/dependency-review.yml.example` to `.github/workflows/dependency-review.yml`.
3. Keep the workflow opt-in; do not gate it on `repository.private == false`.

## CODEOWNERS

Do not enable CODEOWNERS with placeholders. Copy `extras/github/CODEOWNERS.example` to `.github/CODEOWNERS` only after replacing owners with real GitHub users or teams.

Start broad, then make ownership more specific as code ownership becomes real. CODEOWNERS can affect required reviews and branch protection, so stale entries create workflow friction.

## Discussions

Use `extras/github/community/DISCUSSION_TEMPLATE/questions.yml` only when the repository uses GitHub Discussions for support or product feedback.

Discussion templates should be lighter than issue templates. Ask for the question, context, and a minimal example; avoid long pledges or community rules unless the project has an explicit support policy.
