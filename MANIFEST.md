# 508 Devkit Manifest

**This is a record of the 508.dev devkit this repo was generated from, not an
inventory of this repo.** Soundboard is a native Android app; the devkit's
Bun/TypeScript, Python, Ruby, and Docker Compose pieces listed below were
deleted during setup (see `DECISIONS.md` → "Native Android, Not A Web/Service
Stack"). It is kept for one purpose: if this project ever gains a component the
devkit covers — a backend, a companion web app — this table is the checklist for
pulling that convention pack in.

This manifest is the agent-facing inventory for template selection. Use it to
walk the repo before copying or deleting files.

Disposition tags:

- `keep-by-default`: broad repo hygiene that usually survives, with project
  edits.
- `select-per-stack`: keep only when the target language/runtime or package
  manager calls for it.
- `opt-in`: keep only after confirming the workflow, owner, permission,
  infrastructure, or deployment need.
- `devkit-only-delete-in-generated-repos`: design history or devkit interface
  content that should not remain in a generated product repo unless explicitly
  repurposed.

## Top-Level Inventory

| Path | Disposition | Purpose |
| --- | --- | --- |
| `.cursor/rules/` | `keep-by-default` | Cursor agent rule pointing to canonical repo instructions. |
| `.dockerignore` | `keep-by-default` | Keeps Docker build contexts small and secret-safe. |
| `.editorconfig` | `keep-by-default` | Cross-editor formatting baseline. |
| `.env.example` | `keep-by-default` | Environment contract template; rewrite for the target app. |
| `.github/` | `keep-by-default` | Small issue, PR, and CI defaults when the repo uses GitHub. |
| `.gitignore` | `keep-by-default` | Ignore rules for local state, dependencies, caches, and `.context/`. |
| `.pre-commit-config.yaml` | `opt-in` | Optional local hook runner for repos that want pre-commit. |
| `.sops.yaml.example` | `opt-in` | Optional SOPS starter for repos with encrypted tracked files. |
| `.worktreeinclude` | `keep-by-default` | Allowlist of ignored local config copied into sibling worktrees. |
| `AGENTS.md` | `keep-by-default` | Canonical agent operating instructions. |
| `CLAUDE.md` | `keep-by-default` | Claude Code pointer to canonical instructions. |
| `CONTRIBUTING.md` | `keep-by-default` | Contributor workflow baseline. |
| `DECISIONS.md` | `keep-by-default` | Devkit constitution; rewrite into project decisions after generation. |
| `LICENSE` | `keep-by-default` | Repository license; replace if the target project uses another license. |
| `MANIFEST.md` | `keep-by-default` | This inventory and template-selection checklist. |
| `README.md` | `keep-by-default` | Project overview and quickstart; rewrite for the target project. |
| `SECURITY.md` | `keep-by-default` | Vulnerability reporting and security expectations. |
| `biome.json` | `select-per-stack` | Biome formatter/linter config for JavaScript or TypeScript projects. |
| `bun.lock` | `select-per-stack` | Bun lockfile for the root TypeScript workspace example. |
| `bunfig.toml` | `select-per-stack` | Bun install policy, including dependency cooldowns. |
| `compose.yml` | `opt-in` | Local infrastructure examples such as Postgres and Redis. |
| `docker-compose.yml` | `opt-in` | Compatibility wrapper for `compose.yml`. |
| `docs/` | mixed | Durable documentation; see the docs inventory below. |
| `extras/` | `opt-in` | Optional workflows, Dockerfiles, dev containers, storage, and GitHub add-ons. |
| `llms.txt` | `keep-by-default` | Short index for agents and LLM-based tooling. |
| `package.json` | `select-per-stack` | Root Bun/TypeScript scripts and dependency metadata. |
| `pnpm-workspace.example.yaml` | `select-per-stack` | pnpm alternative root workspace policy. |
| `renovate.json` | `keep-by-default` | Dependency update policy with cooldown-aware scheduling. |
| `scripts/` | `keep-by-default` | Stable human/agent entrypoints for dev, test, lint, ports, and Compose. |
| `skills/` | `opt-in` | Project-local agent skills; usually remove from product repos unless maintained. |
| `stacks/` | `select-per-stack` | Language/runtime convention packs. |

## Docs Inventory

| Path | Disposition | Purpose |
| --- | --- | --- |
| `docs/agent-walkthrough.md` | `devkit-only-delete-in-generated-repos` | Expected agent behavior when applying this devkit. |
| `docs/deployment.md` | `keep-by-default` | Deployment documentation placeholder and decision prompts. |
| `docs/development.md` | `keep-by-default` | Local development runbook and script conventions. |
| `docs/frontend.md` | `keep-by-default` | Framework-neutral frontend policy. |
| `docs/github-template.md` | `devkit-only-delete-in-generated-repos` | Cleanup checklist for GitHub-template-generated repos. |
| `docs/github-workflows.md` | `keep-by-default` | GitHub workflow and template guidance. |
| `docs/interfaces.md` | `keep-by-default` | Runtime and boundary contract guidance. |
| `docs/observability.md` | `keep-by-default` | Logging, metrics, traces, and incident context guidance. |
| `docs/pattern-report.md` | `devkit-only-delete-in-generated-repos` | Design-history synthesis from source repos. |
| `docs/secrets.md` | `keep-by-default` | Secret handling and environment boundary guidance. |
| `docs/supply-chain.md` | `keep-by-default` | Canonical dependency cooldown and locked-install policy. |
| `docs/template-proposal.md` | `devkit-only-delete-in-generated-repos` | Design rationale for this template source. |
| `docs/tooling.md` | `keep-by-default` | Tooling policy and command conventions. |

## Stack Inventory

| Path | Disposition | Purpose |
| --- | --- | --- |
| `stacks/typescript/` | `select-per-stack` | Framework-neutral Bun/TypeScript conventions, Biome, Vitest, and Drizzle examples. |
| `stacks/typescript/pnpm/` | `select-per-stack` | pnpm root files and CI fragment for pnpm workspaces. |
| `stacks/python/` | `select-per-stack` | Optional uv Python API/shared-package workspace with Ruff, MyPy, Pytest, Pydantic, and Alembic examples. |
| `stacks/ruby/` | `select-per-stack` | Optional Ruby/Rails/Rack conventions with Bundler cooldown guidance. |

## Extras Inventory

| Path | Disposition | Purpose |
| --- | --- | --- |
| `extras/dev-scripts/` | `opt-in` | JS-first variants of root dev and worktree-port scripts. |
| `extras/devcontainer/` | `opt-in` | Dev container starter for teams that want containerized development. |
| `extras/dockerfiles/` | `opt-in` | Example deployment Dockerfiles for API, worker, and web services. |
| `extras/github/` | `opt-in` | CODEOWNERS, discussion template, Gitleaks, and Dependency Review examples. |
| `extras/object-storage/` | `opt-in` | MinIO Compose example for local S3-compatible storage. |
| `extras/todo-to-issue/` | `opt-in` | GitHub workflow for turning TODO comments into issues. |

## Selection Report

Before editing a generated repo or normalizing a target repo, produce a
selection report with one row for every top-level path in this devkit and every
top-level path in the target repo:

| Path | Source | Decision | Reason |
| --- | --- | --- | --- |
| `example/path` | devkit or target | adopt, adapt, skip, delete, or defer | One concrete reason. |

The report is intentionally mechanical. It makes skipped files explicit and
keeps the first PR focused on selection before product feature work.
