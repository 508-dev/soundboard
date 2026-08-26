#!/usr/bin/env sh
set -eu

cd "$(dirname "$0")/.."

# Root validation covers the language-neutral wrapper scripts plus the default
# TypeScript stack. Run stack-local check-all scripts after selecting extras.
./scripts/lint.sh
./scripts/typecheck.sh
./scripts/test.sh
bun run build
