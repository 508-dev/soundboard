#!/usr/bin/env sh
set -eu

cd "$(dirname "$0")/.."
WORKTREE_ROOT="$(pwd -P)"

usage() {
  cat >&2 <<'EOF'
usage: dev.sh [--reclaim-ports]

Options:
  --reclaim-ports   Before starting, stop this worktree's own JS dev process
                    if it is still listening on WEB_PORT. Adapted repos can
                    call reclaim_service_port for other host-run app services.
  --no-reclaim-ports
                    Disable reclaiming even when DEVKIT_RECLAIM_PORTS=1.
  --help            Show this help.

Environment:
  DEVKIT_RECLAIM_PORTS=1  Same as --reclaim-ports.
EOF
}

RECLAIM_PORTS="${DEVKIT_RECLAIM_PORTS:-0}"
while [ "$#" -gt 0 ]; do
  case "$1" in
    --reclaim-ports)
      RECLAIM_PORTS=1
      ;;
    --no-reclaim-ports)
      RECLAIM_PORTS=0
      ;;
    --help|-h)
      usage
      exit 0
      ;;
    *)
      usage
      exit 2
      ;;
  esac
  shift
done

eval "$(./scripts/worktree-ports.sh export)"
export WEB_HOST="${WEB_HOST:-127.0.0.1}"

# Root dev runs only language-neutral infra plus the TypeScript convention
# watcher. Runtime-specific services, such as the Python API, live in stacks and
# should be started from their stack scripts when selected for a target repo.
echo "508 Devkit local stack"
echo "Assigned worktree ports:"
./scripts/worktree-ports.sh env | sed 's/^/  /'
echo
echo "Starting services"
echo "  Web: ${WEB_URL} (framework-neutral TypeScript watcher)"
echo "  Postgres: 127.0.0.1:${POSTGRES_HOST_PORT}"
echo "  Redis: 127.0.0.1:${REDIS_HOST_PORT}"
echo

./scripts/docker-compose.sh up -d postgres redis

detect_js_runner() {
  if [ -n "${DEVKIT_JS_RUNNER:-}" ]; then
    printf '%s\n' "$DEVKIT_JS_RUNNER"
    return
  fi

  # Keep the root script usable when a repository chooses the pnpm stack
  # variant. The packageManager field is the strongest signal; lockfiles are a
  # fallback for copied templates where package.json has been edited.
  if grep -Eq '"packageManager"[[:space:]]*:[[:space:]]*"pnpm@' package.json 2>/dev/null; then
    printf '%s\n' pnpm
    return
  fi

  if grep -Eq '"packageManager"[[:space:]]*:[[:space:]]*"bun@' package.json 2>/dev/null; then
    printf '%s\n' bun
    return
  fi

  if [ -f pnpm-lock.yaml ]; then
    printf '%s\n' pnpm
    return
  fi

  printf '%s\n' bun
}

JS_RUNNER="$(detect_js_runner)"

port_listener_pids() {
  port="$1"
  if ! command -v lsof >/dev/null 2>&1; then
    echo "dev.sh --reclaim-ports requires lsof to inspect port owners." >&2
    return 1
  fi
  lsof -nP -tiTCP:"$port" -sTCP:LISTEN 2>/dev/null | sort -u
}

process_cwd() {
  pid="$1"
  lsof -a -p "$pid" -d cwd -Fn 2>/dev/null | sed -n 's/^n//p' | sed -n '1p'
}

process_command() {
  pid="$1"
  ps -p "$pid" -o command= 2>/dev/null || true
}

process_parent_pid() {
  pid="$1"
  ps -p "$pid" -o ppid= 2>/dev/null | tr -d ' ' || true
}

is_inside_worktree() {
  path="$1"
  case "$path" in
    "$WORKTREE_ROOT"|"$WORKTREE_ROOT"/*) return 0 ;;
    *) return 1 ;;
  esac
}

is_expected_service_command() {
  service_name="$1"
  command_line="$2"

  case "$service_name" in
    web)
      is_expected_web_dev_command "$command_line"
      ;;
    *)
      return 1
      ;;
  esac
}

is_expected_web_dev_command() {
  command_line="$1"
  case "$command_line" in
    next\ dev*|*" next dev"*|*next-server*|\
    vite\ *|*" vite "*|\
    astro\ dev*|*" astro dev"*|\
    remix\ vite:dev*|*" remix vite:dev"*|\
    webpack\ serve*|*" webpack serve"*|\
    rspack\ serve*|*" rspack serve"*|\
    rsbuild\ dev*|*" rsbuild dev"*|\
    parcel\ serve*|*" parcel serve"*|\
    tanstack\ start*|*" tanstack start"*|\
    tsc\ --noEmit\ --watch*|*" tsc --noEmit --watch"*|\
    bun\ run*|*" bun run"*|\
    pnpm\ *|*" pnpm "*)
      return 0
      ;;
    *)
      return 1
      ;;
  esac
}

is_expected_service_process() {
  service_name="$1"
  pid="$2"
  depth=0

  while [ -n "$pid" ] && [ "$pid" -gt 1 ] 2>/dev/null && [ "$depth" -lt 8 ]; do
    command_line="$(process_command "$pid")"
    cwd="$(process_cwd "$pid")"

    if [ -n "$cwd" ] && is_inside_worktree "$cwd" && is_expected_service_command "$service_name" "$command_line"; then
      return 0
    fi

    pid="$(process_parent_pid "$pid")"
    depth=$((depth + 1))
  done

  return 1
}

wait_for_port_release() {
  port="$1"
  attempts=0
  while [ "$attempts" -lt 20 ]; do
    if [ -z "$(port_listener_pids "$port")" ]; then
      return 0
    fi
    attempts=$((attempts + 1))
    sleep 0.1
  done
  return 1
}

reclaim_service_port() {
  service_name="$1"
  port="$2"
  pids="$(port_listener_pids "$port")"
  if [ -z "$pids" ]; then
    return 0
  fi

  reclaim_pids=""
  for pid in $pids; do
    if ! is_expected_service_process "$service_name" "$pid"; then
      command_line="$(process_command "$pid")"
      cwd="$(process_cwd "$pid")"
      echo "Refusing to reclaim ${service_name} port ${port}; pid ${pid} does not look like this worktree's ${service_name} process." >&2
      echo "  cwd: ${cwd:-unknown}" >&2
      echo "  cmd: ${command_line:-unknown}" >&2
      return 1
    fi

    reclaim_pids="${reclaim_pids}${pid} "
  done

  for pid in $reclaim_pids; do
    echo "Reclaiming ${service_name} port ${port} from pid ${pid}"
    kill "$pid" 2>/dev/null || true
  done

  if wait_for_port_release "$port"; then
    return 0
  fi

  echo "${service_name} port ${port} is still in use after SIGTERM; refusing to force-kill it." >&2
  return 1
}

if [ "$RECLAIM_PORTS" = "1" ]; then
  if ! reclaim_service_port web "$WEB_PORT"; then
    echo "Continuing because the root dev script does not bind WEB_PORT." >&2
  fi
fi

cleanup() {
  if [ -n "${WEB_PID:-}" ]; then kill "$WEB_PID" 2>/dev/null || true; fi
}
trap cleanup INT TERM EXIT

case "$JS_RUNNER" in
  bun)
    bun run --cwd stacks/typescript dev &
    ;;
  pnpm)
    pnpm -C stacks/typescript run dev &
    ;;
  *)
    echo "Unsupported DEVKIT_JS_RUNNER=${JS_RUNNER}; expected bun or pnpm." >&2
    exit 1
    ;;
esac
WEB_PID=$!

wait "$WEB_PID"
