# Security Policy

## Reporting Vulnerabilities

Do not open public issues for vulnerabilities or leaked secrets.

Report security concerns to the project maintainers through the private channel configured for the target repository. When applying this devkit to a new repository, replace this paragraph with the real reporting address or process.

## Secret Handling

- Keep secrets in environment variables or encrypted files.
- Never commit real `.env` files, tokens, private keys, credentials, or production data.
- Use `.env.example` for documented configuration only.
- Use `.sops.yaml.example` as a starting point when a repository needs encrypted files.

## Dependency Policy

This devkit uses dependency cooldowns and locked installs:

- Bun: `minimumReleaseAge = 604800`.
- uv: `exclude-newer = "P7D"` when a Python workspace is present and uv is
  `0.9.17` or newer.
- Renovate: `minimumReleaseAge = "7 days"`.
- CI should use frozen or locked installs.

## GitHub Actions

Workflows should use least-privilege permissions, pinned action SHAs, `persist-credentials: false` where practical, and `harden-runner` in audit or block mode.

## Agent Notes

- Do not leave the placeholder reporting channel in a real repository.
- Prefer GitHub native secret scanning and push protection where available.
- Add `extras/github/gitleaks.yml.example` only when maintainers want CI secret
  scanning and can triage findings.
- Add `extras/github/dependency-review.yml.example` only when dependency graph
  reporting is desired and enabled.
- Check `uv --no-config --version` before adding relative uv cooldowns to a
  downstream repo. Ask before upgrading uv; do not leave old uv clients with
  unparseable `P7D` or `7 days` config.
