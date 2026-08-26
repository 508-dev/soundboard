# Triage CI Failure

Use when a GitHub Actions check fails on a devkit-style repo.

## Workflow

1. Inspect the failing check, job, and log section.
2. Reproduce the narrow command locally when possible.
3. Classify the failure: code, test, dependency, environment, flake, secret, permission, or workflow configuration.
4. Fix the root cause, not just the symptom.
5. Rerun the smallest relevant local check.
6. If CI-only, document the reason and push the focused workflow fix.

Do not rerun CI repeatedly without changing the hypothesis.
