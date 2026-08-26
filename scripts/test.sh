#!/usr/bin/env sh
set -eu

cd "$(dirname "$0")/.."

# Keep tests behind a stable wrapper for humans, agents, and CI.
bun run test
