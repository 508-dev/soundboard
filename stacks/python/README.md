# Python Stack

Use this stack when the target project needs a Python API, worker, shared
Python package, or Python-first service tooling.

## Contains

- `pyproject.toml` and `uv.lock` for a `uv` workspace.
- `apps/api`: minimal FastAPI wiring with Pydantic settings and Alembic
  examples.
- `packages/shared`: shared settings, schemas, and observability helpers.
- `scripts/worktree-ports.py`: Python implementation of the worktree port helper.
- `scripts/{lint,format,typecheck,test,check-all}.sh`: validation wrappers for
  this stack.
- Type checker config examples for MyPy, Pyright, Pyrefly, and ty in
  `pyproject.toml`.

The root devkit uses `scripts/worktree-ports.sh` by default so projects are not
forced to include Python just for port allocation.

## Apply

Copy the relevant files into the target repo root:

```bash
cp -R stacks/python/apps stacks/python/packages .
cp stacks/python/pyproject.toml stacks/python/uv.lock .
cp stacks/python/scripts/*.sh stacks/python/scripts/worktree-ports.py scripts/
```

Then validate the stack from this directory or from the copied target:

```bash
./scripts/check-all.sh
```

Before copying this stack into a downstream repo, run:

```bash
uv --no-config --version
```

The committed stack config intentionally omits relative `exclude-newer`
cooldowns so older uv clients can parse it. If the target machine has uv
`0.9.17` or newer, you may add `exclude-newer = "P7D"` to `[tool.uv]` and
`[tool.uv.pip]`, then regenerate `uv.lock` so it records
`exclude-newer-span = "P7D"`. If uv is older, ask whether to upgrade uv before
adding relative cooldowns. Do not write `P7D` or `7 days` into persistent uv
config for older clients; they parse those values as dates and fail during
settings discovery.

## Agent Notes

- Do not copy this stack just because a repo has scripts. Select it only when
  Python is part of the target runtime or tooling.
- Copy `apps/`, `packages/`, `pyproject.toml`, and `uv.lock` together so the
  workspace and lockfile stay coherent.
- Keep one type checker as the CI gate. The default stack still runs MyPy
  because it is mature, Python-native, and has the widest plugin ecosystem.
  Prefer Pyrefly for new projects that want a fast modern checker and language
  server, Pyright when the team standardizes on Pylance or the Pyright CLI, and
  ty as an advisory experiment until it exits beta.
- Keep root port helpers shell-based unless the target repo intentionally wants
  Python helper scripts.
- If copying this stack's `scripts/dev.sh`, copy
  `scripts/worktree-ports.py` with it or adapt `dev.sh` to use the root shell
  port helper.
- Verify uv cooldown support before adding optional `exclude-newer = "P7D"`
  lines to a target repo.
- Update `.env.example` whenever settings fields change.
- Run `./scripts/check-all.sh` from the copied Python stack before handing off.
