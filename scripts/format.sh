#!/usr/bin/env sh
set -eu

cd "$(dirname "$0")/.."

# Formatting is explicit and separate from format:check/pre-commit.
bun run format
