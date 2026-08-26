#!/usr/bin/env sh
set -eu

cd "$(dirname "$0")/.."

# Keep the root command stable while the TypeScript stack owns compiler details.
bun run typecheck
