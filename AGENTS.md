# AI Agent Development Guide

## Environment

- Only `python3` is guaranteed. Do not assume `python` exists.
- Prefer package scripts and repo-provided entrypoints over raw commands. When a
  repo uses Python, prefer `uv run`; when it uses Bun, prefer `bun run`.
- Treat install, dev, and test commands as executable code. Inspect manifests, package scripts, lockfiles, Docker files, and setup scripts before running them in unfamiliar repos.

## Dependency Supply-Chain Safety

- Bun: keep `bunfig.toml` with `minimumReleaseAge = 604800`.
- uv: add optional `exclude-newer = "P7D"` only after confirming the local
  `uv` version supports relative `exclude-newer` durations.
- pnpm: keep `minimumReleaseAge: 10080` in `pnpm-workspace.yaml`.
- Bundler: use `source "https://rubygems.org", cooldown: 7` only with Bundler
  `4.0.13` or newer, then pin that Bundler version in `Gemfile.lock`.
- CI should use locked installs:
  - `bun install --frozen-lockfile`
  - `uv sync --locked` when a Python workspace is present
  - `pnpm install --frozen-lockfile` when pnpm is used.
  - `bundle install` with deployment/frozen settings when Ruby is used.
- Commit lockfiles.

## Repository Shape

- `AGENTS.md`: canonical agent operating instructions.
- `MANIFEST.md`: file inventory and template-selection checklist.
- `DECISIONS.md`: decision authority for devkit topology and policy.
- `docs`: contributor-facing documentation.
- `extras`: optional workflows, deployment examples, and support add-ons.
- `scripts`: stable project entrypoints.
- `skills`: optional project-local agent skills.
- `stacks/typescript`: framework-neutral Bun/TypeScript conventions.
- `stacks/python`: optional Python API/shared-package workspace.
- `stacks/ruby`: optional Ruby/Rails/Rack workspace conventions.
- `.context`: gitignored workspace-local scratch for Conductor and agents.

## Development Workflow

- Run infrastructure with Docker Compose.
- Run app services on the host for reload speed and debuggability.
- Use `./scripts/worktree-ports.sh env` to inspect local ports.
- Use `./scripts/docker-compose.sh` instead of raw `docker compose` for local worktree-safe infra.
- Use `./scripts/dev.sh` for host-run app services.
- Treat `apps/*` as disposable wiring examples, not framework code to cargo-cult into every project.
- Do not assume a frontend framework from this devkit. Choose Next.js, Vite, TanStack Start, Astro, Expo, or no frontend based on the target project.
- Keep `.worktreeinclude` as a short allowlist of ignored local config to copy into sibling worktrees, such as `.env`, `.env.local`, and `.sops.yaml`.
- Keep `.dockerignore` in sync with the repo shape so Docker build contexts exclude secrets, local dependencies, caches, `.context/`, and generated outputs.

## Editing Rules

- Read target files, callers, exports, tests, and obvious shared utilities before editing.
- When applying this devkit or cleaning up a GitHub-template-generated repo,
  read `MANIFEST.md` and produce a selection report before editing. Cover every
  top-level path in this devkit and the target repo with an adopt, adapt, skip,
  delete, or defer decision and a one-line reason.
- Keep edits surgical.
- Do not reformat unrelated files.
- Add or update tests when behavior changes.
- Update `.env.example` when adding configuration.
- Update docs when changing developer workflows.
- When selecting the Python stack, the included examples use Pydantic for
  settings/boundary schemas and Alembic for database migrations. Keep them when
  they fit; replace them when the target repo has better existing choices.
- Before adding uv cooldown config, run `uv --no-config --version`. Relative
  `exclude-newer` values such as `P7D` require uv `0.9.17` or newer. If the
  target machine is older, ask before upgrading uv; do not write `P7D` or
  `7 days` into `pyproject.toml` or `uv.toml` because older uv clients fail
  during settings discovery.
- Before adding Bundler cooldown config, run `bundle --version`. The
  `cooldown:` source option requires Bundler `4.0.13` or newer. If Bundler is
  older, ask before upgrading it; do not add cooldown syntax that the target
  repo's Bundler cannot parse.
- The TypeScript stack includes Drizzle examples for database access. Keep
  Drizzle when it fits; replace it when the target repo already uses another
  data-access layer.
- Keep secrets in environment variables or SOPS-managed files, never in code.

## `.context/`

Use `.context/` for workspace-local agent scratch only. Do not commit it.

Durable project knowledge belongs in tracked docs:

- Architecture and layout: `README.md`, `docs/template-proposal.md`, `docs/pattern-report.md`.
- Tooling decisions: `docs/tooling.md`, `docs/supply-chain.md`.
- Local development runbooks: `docs/development.md`.
- Repeated failure patterns: concise tracked docs, not raw logs or transcripts.

## Validation

Before calling work complete, run the narrowest relevant checks:

```bash
./scripts/lint.sh
./scripts/typecheck.sh
./scripts/test.sh
```

For broader changes, run:

```bash
./scripts/check-all.sh
```
