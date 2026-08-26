# Development

Local development follows the pattern used across existing projects:

- Docker Compose owns infrastructure.
- App services run on the host.
- Ports are deterministic per worktree.

## Commands

```bash
./scripts/worktree-ports.sh env
./scripts/docker-compose.sh up -d postgres redis
./scripts/dev.sh
./scripts/check-all.sh
```

`worktree-ports.sh env` is the print-only URL/port mode. It prints `WEB_URL`
first, then `WEB_PORT`, then the remaining assigned ports and derived
connection strings, and exits without starting services. Keep that order when
adapting the helper so coding workspace tools discover the web surface before
API or infrastructure URLs.

## Worktree Port Reservations

`scripts/worktree-ports.sh` normally hashes the absolute git worktree path and
derives stable local ports from that hash. Some agent/worktree orchestrators
reserve ports for each workspace. Keep product-specific environment variable
names out of the helper and map them to these generic names in the run command
or wrapper script:

- `WORKTREE_PORT_BLOCK_START`: first port in a reserved block.
- `WORKTREE_PORT_BLOCK_SIZE`: size of the reserved block, default `10`.
- `WORKTREE_PRIMARY_PORT`: one reserved public port.
- `WORKTREE_PRIMARY_PORT_TARGET`: `WEB_PORT` or `API_PORT`, default `WEB_PORT`.

When a block is present, the helper uses compact offsets inside it for web, API,
worker health, database, cache, and OTEL example ports. When only one public port
is present, the helper assigns it to the selected primary target and keeps other
ports on the normal deterministic worktree allocation.

## Optional Port Reclaim

Dev scripts may offer opt-in port reclaim so a developer can rerun the same
worktree script after a stale dev process is left behind:

```bash
./scripts/dev.sh --reclaim-ports
DEVKIT_RECLAIM_PORTS=1 ./scripts/dev.sh
```

Reclaiming must stay conservative. Before killing a process, inspect the port
owner with `lsof`, verify the process cwd or parent process cwd is under the
current worktree, and verify the command looks like the same service type the
script is about to start. Refuse to kill unrelated listeners and print the pid,
cwd, and command so the developer can decide manually.

The root `scripts/dev.sh` includes a small shell example for single host-run
web-dev processes. It walks a short parent chain from the process listening on
`WEB_PORT` and only sends `SIGTERM` when it finds a same-worktree JS dev command
such as Next, Vite, Astro, webpack, rsbuild, Bun, or pnpm. The shell helper is
generic enough for adapted repos to add other host-run app processes:

```sh
# Example only: add a service-specific signature before enabling this.
reclaim_service_port api "$API_PORT"
reclaim_service_port worker-health "$WORKER_HEALTH_PORT"
```

Each extra service needs its own command matcher, such as a Uvicorn app import
for an API, a queue-worker binary name, or a bot executable. Do not treat all
same-worktree processes as reclaimable; a repo can run unrelated listeners from
the same checkout.

Avoid reclaiming Docker-owned infrastructure ports from the host dev script.
For Postgres, Redis, MinIO, and similar Compose services, use stable
`COMPOSE_PROJECT_NAME` with `./scripts/docker-compose.sh up` / `down` so
same-worktree runs are idempotent. If an infrastructure port is held by
something else, report the owner and ask the developer to stop it or change the
configured port.

For multi-service repos, prefer a small service-aware mux helper instead of
copying generic shell globs. A good pattern is:

```text
scripts/dev.sh web
  -> scripts/dev_mux.py --ensure-port web
  -> lsof owner pids for the web URL port
  -> walk parent/child process tree
  -> require same worktree path scope
  -> require service signature, such as the uvicorn app import or discord-bot
  -> stop the related service process tree
  -> verify the port is free before starting
```

This avoids killing a different service that happens to run from the same repo,
and avoids prefix-path mistakes such as treating `/tmp/app/foo-bar` as being
inside `/tmp/app/foo`.

## Worktree Includes

Use `.worktreeinclude` to allowlist ignored local files that should be copied into new sibling worktrees. Treat entries as gitignore-style path patterns, not shell globs passed directly to `cp`.

Example:

```text
.env
.env.local
.env.development.local
.sops.yaml
```

Do not include generated state such as `.venv`, `node_modules`, caches, local databases, screenshots, or raw logs. Those should be recreated per worktree.

## Workspace Context

Do not commit `.context/`. Conductor creates it as workspace-local scratch for
agents. Durable runbooks and decisions belong in tracked docs such as this file,
`docs/tooling.md`, and `docs/pattern-report.md`.

## Docker Build Contexts

Keep `.dockerignore` in every repo that has Dockerfiles or Compose services. Exclude local secrets, dependency directories, caches, agent scratch state, and build outputs so Docker does not upload large or sensitive files into the build context.

Example:

```text
.git
.context
.env
.env.*
!.env.example
.venv
node_modules
**/node_modules
__pycache__
.pytest_cache
.mypy_cache
.ruff_cache
dist
build
```

## Why Host-Run Services

Host-run app services are faster for reload loops, easier for agents to inspect, and avoid rebuilding containers for normal code changes.

Use full-container Compose only when validating deployment parity.

## Agent Notes

- Keep root scripts as stable entrypoints. Change package-manager internals
  behind them when adapting a target repo.
- Use `./scripts/worktree-ports.sh env` before debugging port conflicts.
- Copy ignored local config through `.worktreeinclude`; do not commit copied
  `.env` files or generated workspace state.
