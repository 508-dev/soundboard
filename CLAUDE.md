# Claude Code Instructions

The canonical agent instructions are in `AGENTS.md`.

Follow the same repo rules as Codex:

- Read before editing.
- Prefer repo-provided scripts. Use `bun run` for root tooling, `uv run` when a
  Python workspace is present, and `bundle exec` when a Ruby workspace is
  present.
- Keep changes scoped.
- Update `.env.example`, tests, and docs when contracts change.
- Use gitignored `.context/` for concise workspace-local operational memory.
  Promote durable knowledge into tracked docs instead of committing `.context/`.
