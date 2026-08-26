#!/usr/bin/env bash
# Everything CI runs: ktlint, Android lint, unit tests, debug assembly.
# No emulator required.
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."
./gradlew check assembleDebug
