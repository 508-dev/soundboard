#!/usr/bin/env sh
set -eu

cd "$(dirname "$0")/.."

# Delegate to package scripts so target repos can swap tools without changing
# every shell entrypoint.
bun run lint
