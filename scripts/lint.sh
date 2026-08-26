#!/usr/bin/env bash
# ktlint + Android lint, no emulator required.
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."
./gradlew ktlintCheck lintDebug
