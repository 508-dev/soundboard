#!/usr/bin/env sh
set -eu

cd "$(dirname "$0")/.."
export UV_LOCKED=1

uv run ruff format apps packages tests
