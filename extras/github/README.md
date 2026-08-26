# GitHub Extras

These files are useful in many repositories but should be copied intentionally.

## Agent Notes

- Do not copy every file in this directory by default.
- Ask what governance or security workflow the target repo actually wants.
- Prefer explaining an extra as available over silently adding a workflow that
  can create noisy checks or require repository settings.

## CODEOWNERS

`CODEOWNERS.example` is a starting point for review ownership. Do not commit it as active `.github/CODEOWNERS` until the owners are real GitHub users or teams.

Active CODEOWNERS affects reviewer routing and may interact with branch protection, so keep ownership broad at first and tighten it as the team stabilizes.

## Community Templates

Use `community/DISCUSSION_TEMPLATE/questions.yml` only for repositories that use GitHub Discussions for public or internal support. Keep the language project-specific and avoid making the template longer than the support workflow can justify.

## Secret Scanning

Use `gitleaks.yml.example` when maintainers want CI history scanning for
secrets. It downloads a pinned gitleaks release, verifies the checksum, and runs
`gitleaks git`.

Do not add it automatically to every repository. Secret scanners can surface
historic findings or false positives that need a cleanup process.

## Dependency Review

Use `dependency-review.yml.example` when maintainers want GitHub dependency
graph-based vulnerability, license, or dependency-change reporting.

Do not add it automatically. It requires GitHub's dependency graph to be enabled
and is not the primary active supply-chain attack detector.
