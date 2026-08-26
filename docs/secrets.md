# Secrets

Most repos should use environment variables and CI secrets.

SOPS is optional. Add it only when the repository needs encrypted files checked into Git, such as shared non-production config or deploy manifests.

GitHub secret scanning and push protection should be the first choice when they
are available for the repository. Use the Gitleaks extra only when maintainers
also want repo-local CI scanning and are ready to triage historical findings.

If SOPS is adopted:

1. Copy `.sops.yaml.example` to `.sops.yaml`.
2. Replace the example Age recipient.
3. Store encrypted files under `secrets/`.
4. Document decrypt/edit commands in this file.

Do not commit plaintext secrets.

## Agent Notes

- Replace generic reporting language with the target repo's real private
  vulnerability channel before shipping public docs.
- Do not add `.sops.yaml` unless encrypted tracked files are actually needed.
- Do not add the Gitleaks workflow silently. Secret scanning can be noisy until
  a baseline and ignore process exist.
- When a real secret is found, tell maintainers to rotate it. Removing it from
  git history is not enough.
