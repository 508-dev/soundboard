# Contributing

This repository is a reference scaffold. Changes should improve conventions that apply across many projects without turning the repo into a product-specific app.

## Principles

- Prefer small, composable defaults over large generated frameworks.
- Keep root files broadly useful.
- Put language/runtime conventions in `stacks/` and team-specific, platform-specific, or workflow-heavy choices in `extras/`.
- Preserve supply-chain cooldowns and committed lockfiles.
- Update agent-facing guidance when conventions change.

## Local Checks

Run the narrowest relevant checks while iterating:

```bash
./scripts/lint.sh
./scripts/typecheck.sh
./scripts/test.sh
```

Before opening or updating a PR, run:

```bash
./scripts/check-all.sh
```

## Pull Requests

Use the PR template. Include what changed, why it belongs in the devkit, and how it was validated.

Avoid committing local state such as `.venv`, `node_modules`, caches, raw logs, screenshots, and `.context/artifacts/`.

## Agent Notes

- Keep convention changes paired with docs and skill updates.
- Do not turn stack examples into root defaults without explaining why the
  convention applies across most projects.
- Validate both the root template and any stack touched by the change.
