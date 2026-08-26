# 508 Devkit

Last reviewed: 2026-07-05

Opinionated sane defaults and conventions for software projects.

This is not a scaffolding CLI. It is a reference repo and GitHub template that
gives agents and humans a shared baseline for how new projects should be shaped:
repository layout, local development, dependency safety, CI, agent instructions,
operational memory, and documentation. It comes from 508.dev practice, but it
is meant to be useful outside 508.dev too.

Point an agent at this repo when starting or normalizing a project. The agent should inspect the target repo, ask clarifying questions when the product or stack is ambiguous, and then copy or adapt only the conventions that fit.

## What It Captures

- Agent-native instructions for Codex, Claude Code, Cursor, and future agents.
- Gitignored workspace-local `.context/` conventions for Conductor and agents.
- Bun and pnpm as first-class JavaScript package manager examples, with Bun
  shown first.
- Optional `uv` Python workspace conventions.
- Optional Ruby/Rails/Rack conventions.
- Dependency cooldowns for Bun, pnpm, uv, and Bundler.
- Host-run development services.
- Docker Compose examples for local infrastructure such as databases and
  caches.
- Deterministic worktree ports.
- Framework-neutral frontend conventions.
- `.worktreeinclude` for copying local-only env files into sibling worktrees.
- `.dockerignore` for small, secret-safe Docker build contexts.
- GitHub PR, issue, and CI hygiene.
- Security scanning and dependency update policy.
- Pydantic settings/schema and Alembic migration examples for Python services.
- Drizzle ORM examples for TypeScript services.
- Ruff, MyPy, Pytest, Biome, and Vitest.
- Optional SOPS documentation without forcing SOPS into every repo.

## Quickstart

### Create A New Repository

Use GitHub's `Use this template` button to create a new repository from
`508-dev/508-devkit`.

GitHub templates copy the default-branch file tree. Treat the generated
repository as a bootstrap workspace: the first PR should select the root
hygiene files, stacks, extras, docs, and workflows that fit the product, then
delete the rest.

Recommended first prompt in the generated repo:

```text
This repository was generated from 508 Devkit. Do a template selection pass:
use MANIFEST.md to produce a selection report before editing, covering every
top-level path in the devkit and this repo with adopt/adapt/skip/delete/defer
and a one-line reason. Then keep only the root hygiene, stacks, extras, docs,
and workflows that fit this project, delete the rest, rename all devkit/example
identifiers, and run the narrowest relevant checks.
```

See `docs/github-template.md` for the cleanup checklist.

### Normalize An Existing Repository

Use the repo directly as a reference:

```text
Use https://github.com/508-dev/508-devkit or a local checkout of it as the
project bootstrap reference.
Inspect my target repo, ask any necessary questions, then apply the relevant conventions.
```

Or install/use the bundled agent skill from this repo:

```text
Install the skill from https://raw.githubusercontent.com/508-dev/508-devkit/main/skills/508-devkit/SKILL.md
or from skills/508-devkit/SKILL.md in a local checkout.
Run it as /508-devkit, /bootstrap-project, or whatever command name your agent client assigns.
```

Expected agent behavior:

- Inspect the target repo before editing.
- Use `MANIFEST.md` to produce a selection report before editing. Cover every
  top-level path in the devkit and the target repo with an adopt, adapt, skip,
  delete, or defer decision and a one-line reason.
- Ask about product shape, deployment target, data stores, and language/runtime choices when those are unclear.
- Automatically pick up existing conventions when the repo already has them.
- Prefer the devkit defaults for new projects unless there is a clear reason to choose a stack or extra.
- Run the narrowest relevant checks before calling the bootstrap complete.

For this repo itself:

```bash
bun install --frozen-lockfile
./scripts/check-all.sh
./scripts/dev.sh
```

## Layout

```text
AGENTS.md      Canonical agent operating instructions
MANIFEST.md    File inventory and template-selection checklist
DECISIONS.md   Decision authority for devkit topology and policy
docs           Durable project documentation
extras         Optional workflow, deployment, and support add-ons
scripts        Stable human/agent entrypoints
skills         Optional project-local agent skills
stacks         Language/runtime convention packs
```

## Read Next

1. Read `DECISIONS.md`.
2. Read `MANIFEST.md`.
3. Read `docs/github-template.md` when starting from GitHub's template button.
4. Read `docs/tooling.md`.
5. Read `docs/frontend.md`.
6. Read `docs/pattern-report.md` and `docs/template-proposal.md` when you need
   devkit design history.
7. Copy `.env.example` to `.env`.
8. Run `./scripts/worktree-ports.sh env`.
9. Run `./scripts/docker-compose.sh up -d postgres redis`.
10. Run `./scripts/dev.sh`.

The port helper's `env` command is the print-only URL/port mode. It prints
`WEB_URL` first, followed by `WEB_PORT` and the rest of the assigned worktree
ports, so coding orchestrators that scan startup output for a URL open the web
surface first.

If a stale same-worktree dev process is still holding the web port, rerun with:

```bash
./scripts/dev.sh --reclaim-ports
```

Port reclaim is opt-in and intentionally narrow: the script checks the listener
with `lsof` and refuses to kill it unless the process chain looks like this
worktree's own JS dev server.

## Worktree And Docker Hygiene

Keep `.worktreeinclude` as a short allowlist of ignored local files that should follow a developer into new sibling worktrees. Typical entries are `.env`, `.env.local`, and `.sops.yaml`; never include generated directories or large state.

Keep `.dockerignore` broad enough to exclude VCS metadata, agent scratch state, dependencies, caches, logs, and secrets from Docker build contexts. Make exceptions only for committed templates such as `.env.example`.

Do not commit `.context/`. Conductor creates `.context/` as workspace-local
agent scratch. Durable project knowledge belongs in normal docs such as
`docs/tooling.md`, `docs/development.md`, `docs/pattern-report.md`, or
`README.md`.

## Package Manager Policy

Bun is shown first because it is the author's preference and keeps small
projects simple, but it is not a universal requirement.

pnpm is first-class for teams or workspaces that already prefer it, need its
monorepo behavior, or want the broader pnpm ecosystem. If using pnpm, add
`pnpm-workspace.yaml`, set `minimumReleaseAge: 10080`, and change CI install
commands to `pnpm install --frozen-lockfile`.

## Pick-And-Choose Stacks And Extras

This repository intentionally includes files that conflict with each other. It
is a template source, not an installable preset or final generated app.

- `stacks/typescript/`: framework-neutral TypeScript conventions, Drizzle
  examples, Biome, Vitest.
- `stacks/python/`: optional Python API/shared-package workspace.
- `stacks/ruby/`: optional Ruby/Rails/Rack conventions with Bundler cooldown
  guidance.
- `stacks/typescript/pnpm/`: pnpm root files and CI fragment for larger TypeScript workspaces.
- `extras/dev-scripts/`: JS-first script variants and JS implementations of helper scripts.
- `extras/dockerfiles/`: opt-in Dockerfile examples for deployment parity.
- `extras/devcontainer/`: opt-in dev container example.
- `extras/object-storage/`: very opt-in MinIO Compose example for projects
  that need local S3-compatible storage.
- `extras/github/`: CODEOWNERS and discussion templates that need project-specific owners or support policy.
- `extras/todo-to-issue/`: opt-in workflow for turning TODO comments into GitHub issues.
- `.sops.yaml.example`: optional SOPS starter only for repos that need encrypted files.

Keep root defaults for most new projects: shell wrappers, shell worktree ports,
and example Compose-managed infra. Select language/runtime stacks such as
`stacks/typescript/`, `stacks/python/`, or `stacks/ruby/` based on the target
project. Treat stack files as conventions to adapt, not product code to copy
blindly. If this repo was used through GitHub's template button, do the same
selection and pruning before feature work.

## Agent Notes

- Never copy the whole repository into a target project.
- A GitHub-generated repo starts with the whole file tree by design; make the
  first project PR a template selection pass that removes irrelevant stacks,
  extras, workflows, and docs.
- Start with root hygiene files, then select only the stacks and extras that
  match the target repo.
- Treat `stacks/` as peer language/runtime convention packs. TypeScript and
  Python are examples, not universal defaults. Ruby is available as an
  opt-in convention pack when the target repo actually uses Ruby.
- Treat `extras/` as opt-in workflows or deployment helpers that may require
  repo settings, real owners, or team process.
- Keep `.context/` gitignored. Promote durable learnings into tracked docs
  instead of committing agent scratch.
- Keep worktree port helpers generic. If a workspace orchestrator exposes a
  reserved port or port block, map it to `WORKTREE_PRIMARY_PORT` or
  `WORKTREE_PORT_BLOCK_START` outside the helper.

## Skill Interface

The repository is the source of truth. The downloadable skill in `skills/508-devkit/SKILL.md` is the agent-facing interface that explains how to apply these files to a target repo.

Task-shaped skills in `skills/*/SKILL.md` capture repeatable workflows such as migration creation, service creation, context promotion, and CI triage.
