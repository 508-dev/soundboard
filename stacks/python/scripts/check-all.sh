#!/usr/bin/env sh
set -eu

cd "$(dirname "$0")/.."
export UV_LOCKED=1

./scripts/lint.sh
uv run ruff format --check apps packages tests
./scripts/typecheck.sh
./scripts/test.sh
