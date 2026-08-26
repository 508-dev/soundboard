#!/usr/bin/env sh
set -eu

cd "$(dirname "$0")/.."

export WEB_HOST="${WEB_HOST:-127.0.0.1}"
export RACK_ENV="${RACK_ENV:-development}"
export RAILS_ENV="${RAILS_ENV:-development}"

if [ -x ./scripts/worktree-ports.sh ]; then
  eval "$(./scripts/worktree-ports.sh export)"
  export PORT="${PORT:-$WEB_PORT}"
else
  export PORT="${PORT:-${WORKTREE_PRIMARY_PORT:-3000}}"
fi

echo "508 Devkit Ruby stack"
echo "  Web: http://${WEB_HOST}:${PORT}"
if [ -n "${POSTGRES_HOST_PORT:-}" ]; then
  echo "  Postgres: 127.0.0.1:${POSTGRES_HOST_PORT}"
fi
if [ -n "${REDIS_HOST_PORT:-}" ]; then
  echo "  Redis: 127.0.0.1:${REDIS_HOST_PORT}"
fi
echo

if [ -x bin/dev ]; then
  exec bin/dev
fi

if [ -x bin/rails ]; then
  exec bundle exec rails server --binding "$WEB_HOST" --port "$PORT"
fi

if [ -f config.ru ]; then
  exec bundle exec rackup --host "$WEB_HOST" --port "$PORT"
fi

echo "No Ruby dev entrypoint found. Add bin/dev, bin/rails, or config.ru." >&2
exit 1
