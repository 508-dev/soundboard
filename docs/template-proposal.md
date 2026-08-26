# AI-First Repository Template Proposal

## Design Goal

This template captures recurring conventions from existing repositories so
coding agents can create project-specific files on demand without needing a
scaffolding CLI.

When used as a GitHub template repository, the first generated-repo change
should be a selection pass. GitHub copies the default-branch file tree, so users
and agents must prune unselected stacks, extras, example apps, and design-history
docs before feature work starts.

It is optimized for:

- Codex: `AGENTS.md`, `.context/`, explicit scripts, surgical edit guidance.
- Conductor: gitignored `.context/` for workspace-local agent scratch.
- Claude Code: `CLAUDE.md` as a short pointer to canonical rules.
- Cursor: `.cursor/rules/repo-conventions.mdc`.
- Future agents: documented boundaries, deterministic commands, and machine-readable structure.

## Default Shape

```text
.
├── AGENTS.md
├── CLAUDE.md
├── README.md
├── .cursor/rules/
├── .github/workflows/
├── stacks/
│   ├── python/
│   ├── ruby/
│   └── typescript/
├── extras/
├── docs/
└── scripts/
```

## Defaults

- Repository tooling: Bun, Biome, TypeScript, Vitest.
- Optional Python stack: `uv`, Ruff, MyPy, Pytest.
- Infra: Docker Compose examples for local databases, caches, or similar
  services.
- Local dev: host-run app services with Docker-managed infra.
- Ports: stable worktree-derived allocations.
- CI: frozen installs, area-aware checks, lint/type/test parity.
- Env: `.env.example` as the runtime contract.

## Alternative Paths

- For Python APIs, workers, or shared packages, copy `stacks/python/`.
- For Go, Rust, or other runtimes, add matching `stacks/<runtime>/` directories instead of changing the root base.
- For very large JS monorepos, pnpm is a first-class option.
- For static sites, drop database and cache services.
- For product repos with browser UI, add Playwright after the first interactive flow exists.
- For LLM features, add deterministic eval fixtures before live model evals.

## GitHub Template Mode

Marking this repo as a GitHub template improves discoverability, but it does
not change the pick-and-choose model. Generated repos should keep the broad root
hygiene, explicitly select one or more stacks, and remove irrelevant examples.

See `docs/github-template.md` for the generated-repo cleanup checklist.

## Non-Goals

- No CLI generator.
- No expectation that generated repos keep every file from this repo.
- No mandatory deployment platform.
- No full app implementation.
- No secret management opinion beyond environment-variable contracts and CI secret boundaries.
