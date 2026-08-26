#!/usr/bin/env sh
set -eu

cd "$(dirname "$0")/.."

# Prefer developer-provided .env values, but keep .env.example as the baseline
# so Compose validation works before a local .env exists.
ENV_FILE=".env"
if [ ! -f "$ENV_FILE" ]; then
  ENV_FILE=".env.example"
fi

load_port_reservations() {
  file="$1"
  if [ ! -f "$file" ]; then
    return 0
  fi

  # Read only the reservation inputs consumed by worktree-ports.sh. Avoid
  # sourcing the whole .env file because local env files are configuration, not
  # shell scripts.
  while IFS= read -r line || [ -n "$line" ]; do
    case "$line" in
      WORKTREE_PORT_BLOCK_START=*|WORKTREE_PORT_BLOCK_SIZE=*|WORKTREE_PRIMARY_PORT=*|WORKTREE_PRIMARY_PORT_TARGET=*)
        key="${line%%=*}"
        value="${line#*=}"
        case "$value" in
          \"*\") value="${value#\"}"; value="${value%\"}" ;;
          \'*\') value="${value#\'}"; value="${value%\'}" ;;
        esac
        export "$key=$value"
        ;;
    esac
  done < "$file"
}

load_port_reservations "$ENV_FILE"

PORT_ENV_FILE="$(mktemp)"
trap 'rm -f "$PORT_ENV_FILE"' EXIT HUP INT TERM
./scripts/worktree-ports.sh env > "$PORT_ENV_FILE"

# Env-file order is significant: examples provide defaults, generated ports
# make sibling worktrees safe, and .env has final local override authority.
if [ "$ENV_FILE" = ".env" ]; then
  exec docker compose -f compose.yml --env-file .env.example --env-file "$PORT_ENV_FILE" --env-file .env "$@"
fi

exec docker compose -f compose.yml --env-file .env.example --env-file "$PORT_ENV_FILE" "$@"
