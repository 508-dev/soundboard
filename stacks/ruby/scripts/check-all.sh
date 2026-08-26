#!/usr/bin/env sh
set -eu

cd "$(dirname "$0")/.."

bundle check
./scripts/lint.sh
./scripts/test.sh
