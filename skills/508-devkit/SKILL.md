# 508 Devkit

Use this skill when creating or normalizing a software project with 508 Devkit
conventions.

Last reviewed: 2026-07-05

Preserve the devkit's topology and policy, but verify current versions, action
SHAs, and API documentation before applying them to a target repo.

Canonical repo: https://github.com/508-dev/508-devkit

## Current Shape

- Root files provide broadly useful hygiene: agent instructions, shell
  entrypoints, worktree-safe ports, Docker Compose examples, GitHub templates,
  dependency cooldowns, and docs.
- `stacks/` contains language/runtime convention packs. TypeScript, Python, and
  Ruby are examples, not universal defaults.
- `extras/` contains opt-in add-ons such as Dockerfiles, dev containers,
  object storage, CODEOWNERS, Gitleaks, Dependency Review, and TODO-to-issue.
- `.context/` is workspace-local agent scratch and must not be committed.

When the target repo was generated from GitHub's `Use this template` button,
treat the first change as a template selection pass. GitHub copies the
default-branch file tree; the generated repo should not keep every stack, extra,
example app, or design-history document.

## Workflow

1. Inspect the devkit decision authority and inventory:
   - `DECISIONS.md`.
   - `MANIFEST.md`.
   - `docs/supply-chain.md` when dependency policy is relevant.
   - `docs/github-template.md` when the target repo was generated from the
     GitHub template.
2. Inspect the target repo before editing:
   - `DECISIONS.md` when present.
   - `AGENTS.md`, `CLAUDE.md`, Cursor rules.
   - `pyproject.toml`, `uv.lock` when Python is present.
   - `Gemfile`, `Gemfile.lock`, `.ruby-version`, `gems.rb`, and `gems.locked`
     when Ruby is present.
   - `package.json`, `bun.lock`, `pnpm-lock.yaml`, `bunfig.toml`, `pnpm-workspace.yaml`.
   - Compose files.
   - `.github/workflows`.
   - `.env.example`.
   - `scripts/`.
3. Produce a selection report before editing. It must include every top-level
   path in the devkit and every top-level path in the target repo, with columns
   for path, source, decision, and reason. Use decisions such as `adopt`,
   `adapt`, `skip`, `delete`, and `defer`.
4. Decide which devkit pieces apply. Do not infer a language, framework,
   database, ORM, migration tool, object store, or package manager from the
   devkit alone.
   - Start with broadly useful repo hygiene: agent instructions, stable scripts,
     `.dockerignore` when Docker/build contexts exist, GitHub PR/issue
     templates when the repo uses GitHub, and docs that match the target repo.
   - Use TypeScript stack examples only when the target repo is JavaScript or
     TypeScript. Bun is shown first, but pnpm is first-class when the repo or
     team prefers it.
   - Use the Python stack only when the target repo is Python or the user asks
     for Python conventions. Treat Pydantic settings and Alembic migrations as
     examples, not requirements.
   - Use the Ruby stack only when the target repo is Ruby or the user asks for
     Ruby conventions. Treat Rails and Rack paths as adaptive examples, not
     requirements.
   - Use Drizzle examples only when TypeScript-side database access is wanted
     and Drizzle fits the repo. Keep an existing data-access layer when one is
     already established.
   - Use framework-neutral frontend conventions only to avoid choosing a
     frontend framework prematurely.
   - Use Docker Compose examples only when local infrastructure is needed.
     Replace or remove Postgres and Redis when the target repo uses different
     infrastructure.
   - Use worktree ports when the repo benefits from parallel local worktrees or
     agent workspaces.
   - Use `.worktreeinclude` only for ignored local config that should follow
     sibling worktrees.
   - Use `extras/object-storage/` only when the target repo explicitly needs
     local S3-compatible storage.
   - Use Gitleaks only when the repo owner wants CI secret scanning and is ready
     to handle baseline findings.
   - Use Dependency Review only when the repo owner wants GitHub dependency
     graph-based vulnerability, license, or dependency-change reporting and has
     enabled the dependency graph.
   - Use SOPS only when encrypted tracked files are needed.
5. Copy or adapt files from the `508-devkit` repository.
6. Update names, package scopes, ports, and docs to fit the target project.
7. In GitHub-template-generated repos, delete unselected stacks, extras,
   workflows, example names, and docs before starting product feature work.
8. Run the narrowest relevant checks.

## Worktree And Docker Files

Keep `.worktreeinclude` as a short allowlist. Good examples are `.env`, `.env.local`, `.env.development.local`, and `.sops.yaml`. Do not include generated state such as `.venv`, `node_modules`, caches, local databases, screenshots, or logs.

Keep `.dockerignore` broad enough to exclude `.git`, `.context`, local secrets, dependency directories, caches, logs, and build outputs. Preserve explicit exceptions for committed templates such as `.env.example`.

Keep worktree port helpers generic. If the target workspace orchestrator exposes
a reserved port block, map it to `WORKTREE_PORT_BLOCK_START` and optionally
`WORKTREE_PORT_BLOCK_SIZE` before running scripts. If it exposes one public
port, map it to `WORKTREE_PRIMARY_PORT` and set `WORKTREE_PRIMARY_PORT_TARGET`
to `WEB_PORT` or `API_PORT`.

Root Compose services are examples for local infrastructure such as databases
and caches. Replace or remove Postgres and Redis when the target repo uses
something else. Do not add object storage from root defaults.

When selecting the Python stack and copying its `scripts/dev.sh`, also copy
`stacks/python/scripts/worktree-ports.py` or adapt the script to the root shell
port helper.

When selecting the Ruby stack and copying its `scripts/dev.sh`, also copy the
root `scripts/worktree-ports.sh` helper or adapt the script to the target repo's
existing port strategy. Keep the root shell helper as the canonical port helper
unless the target repo has a specific reason to standardize scripts in Ruby. If
an ADE exposes a product-specific port variable, map it to `PORT`,
`WORKTREE_PRIMARY_PORT`, or `WORKTREE_PORT_BLOCK_START` in that ADE's wrapper
instead of adding the product-specific name to reusable stack scripts.

## Frontend Frameworks

Do not infer a frontend framework from this devkit. `stacks/typescript` is a TypeScript convention workspace, not a finished Vite, Next.js, or TanStack app.

When applying the devkit, inspect the target repo and ask when needed. Choose a framework only when the product shape and deployment target make it clear. Map neutral env names such as `WEB_API_BASE_URL` into framework-specific public env names after that choice.

## GitHub Files

Use the root `.github/` templates as defaults for most repositories. Keep PR and issue templates short enough that they improve collaboration without adding process overhead.

Keep CODEOWNERS, discussion templates, and TODO-to-issue automation opt-in. Copy them from `extras/` only after replacing placeholder owners, confirming the support workflow, or accepting the workflow permissions.

Do not copy security extras by default. Use `extras/github/gitleaks.yml.example`
only after confirming maintainers want CI secret scanning. Use
`extras/github/dependency-review.yml.example` only after confirming GitHub's
dependency graph is enabled and the repo owner wants that reporting. Dependency
Review is not the primary supply-chain attack detector.

Use `extras/object-storage/compose.object-storage.yml.example` only when the
target repo needs local S3-compatible storage. The example intentionally splits
the MinIO server and client images and uses an init service for idempotent
bucket creation.

## Source Of Truth

The canonical files live in the `508-devkit` repository. Do not recreate large
snippets from memory when the repo is available locally or online.

## Python uv Cooldowns

Before writing uv dependency cooldowns, run `uv --no-config --version`.
Relative `exclude-newer` values such as `P7D` and `7 days` require uv `0.9.17`
or newer. If the target machine has an older uv, ask the user whether they want
to upgrade uv. Do not write relative cooldowns into `pyproject.toml`, `uv.toml`,
or `~/.config/uv/uv.toml` for old uv clients because every uv command can fail
during settings discovery.

## Ruby Bundler Cooldowns

Before writing Bundler dependency cooldowns, run `bundle --version`.
`source "https://rubygems.org", cooldown: 7` requires Bundler `4.0.13` or
newer. If the target machine has an older Bundler, ask before upgrading it. Once
cooldown is added, pin the compatible Bundler version in `Gemfile.lock` with
`bundle lock --bundler=4.0.13` or newer.
