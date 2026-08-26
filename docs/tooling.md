# Tooling

The root devkit is language-neutral. Language and runtime conventions live in
`stacks/`, and optional workflow or deployment add-ons live in `extras/`.

## TypeScript Stack

Show Bun first for TypeScript repository tooling. It keeps scripts fast and
simple, but pnpm is also first-class when a repo or team prefers it.

Required checks for `stacks/typescript`:

```bash
bun run lint
bun run typecheck
bun run test
bun run build
```

Use pnpm when a workspace or team wants its monorepo tooling, workspace
controls, or ecosystem compatibility. The pnpm convention files live in
`stacks/typescript/pnpm/`.

## Python Stack

Use `uv` for Python installs and execution when the target project selects
`stacks/python`.

Required checks for `stacks/python`:

```bash
uv sync --locked
UV_LOCKED=1 uv run ruff check apps packages tests
UV_LOCKED=1 uv run ruff format --check apps packages tests
UV_LOCKED=1 uv run mypy
UV_LOCKED=1 uv run pytest
```

Keep Python configuration in `pyproject.toml`. The Python stack shows Pydantic
settings/boundary schemas and Alembic migrations as examples; keep them when
they fit the target repo and replace them when an existing choice is better.

The Python stack includes example configuration for MyPy, Pyright, Pyrefly, and
ty, but projects should choose one type checker as the required CI gate.
Recommended defaults:

- Use Pyrefly for new Python projects that want fast CLI checks and a modern
  language server, provided the team is comfortable with its monthly release
  cadence and occasional new diagnostics on upgrade.
- Use Pyright when the team already standardizes on VS Code/Pylance, wants a
  mature standards-focused checker, or needs strong cross-editor language
  server behavior.
- Keep MyPy when the project relies on MyPy plugins, framework-specific typing
  behavior, or maximum ecosystem compatibility.
- Use ty only as an advisory or experimental checker until its beta/0.0.x
  version policy settles.

When switching the CI gate, update `stacks/python/scripts/typecheck.sh`, add the
chosen checker to the dev dependency group, and regenerate `uv.lock` with the
repo's supply-chain cooldown policy in mind.

## Ruby Stack

Use Bundler for Ruby installs and command execution when the target project
selects `stacks/ruby`.

The Ruby stack is intentionally a convention pack rather than a generated app.
Copy its `Gemfile.example` to `Gemfile`, generate a project-specific
`Gemfile.lock` with a compatible Bundler, and add Rails, Rack, Sidekiq, or other
runtime gems only when the target repo needs them.

Required checks for a typical Ruby project:

```bash
bundle check
bundle exec rubocop
bundle exec rspec
```

Use Rails or Rack conventions when the target repo already has them. The Ruby
stack intentionally does not make Rails a root default; it provides scripts that
adapt to `bin/dev`, Rails, Rack, RSpec, or Minitest-style test directories.

When adding Bundler cooldowns, require Bundler `4.0.13` or newer and pin that
Bundler version in `Gemfile.lock` before handing off the target repo.

## Dependency Safety

Use dependency cooldowns and frozen installs:

- Bun: `bunfig.toml` sets `minimumReleaseAge = 604800` seconds.
- uv: optional `exclude-newer = "P7D"` only with uv `0.9.17` or newer. Agents
  must check `uv --no-config --version` before writing relative cooldowns
  downstream and ask before upgrading old uv installations.
- pnpm: `pnpm-workspace.yaml` should set `minimumReleaseAge: 10080` minutes.
- Bundler: `Gemfile` should use
  `source "https://rubygems.org", cooldown: 7` when Bundler is `4.0.13` or
  newer.
- CI should use locked or frozen installs.

Regenerate lockfiles when cooldown settings change so CI validates committed
pins instead of resolving fresh dependency graphs.

## Workflow Permissions

Keep workflow permissions at `contents: read` unless a job explicitly calls PR
APIs or posts PR comments.

Dependency Review is opt-in through
`extras/github/dependency-review.yml.example`, not a default hidden behind a
repository variable. Do not make it run automatically for all public
repositories with `github.event.repository.private == false`; that reintroduces
the dependency graph footgun for public repos where the graph is disabled.

Gitleaks is also opt-in through `extras/github/gitleaks.yml.example`. Add it
only when maintainers want CI secret scanning and are prepared to triage
historic findings.
