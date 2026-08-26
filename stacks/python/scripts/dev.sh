#!/usr/bin/env sh
set -eu

cd "$(dirname "$0")/.."

eval "$(./scripts/worktree-ports.py export)"
export API_HOST="${API_HOST:-127.0.0.1}"
export PYTHONPATH="${PYTHONPATH:-apps/api/src:packages/shared/src}"

# This script is intentionally stack-local. Copy it to root scripts only after
# selecting the Python stack for a target repository.
echo "508 Devkit Python stack"
echo "  API: http://${API_HOST}:${API_PORT}"
echo "  Postgres: 127.0.0.1:${POSTGRES_HOST_PORT}"
echo "  Redis: 127.0.0.1:${REDIS_HOST_PORT}"
echo

uv run --package example-api uvicorn example_api.main:create_app \
  --factory \
  --host "$API_HOST" \
  --port "$API_PORT" \
  --reload \
  --reload-dir apps/api/src \
  --reload-dir packages/shared/src
