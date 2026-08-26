#!/usr/bin/env bash
# JVM unit tests only. No emulator required. For instrumented tests
# (androidTest/), use `./gradlew connectedDebugAndroidTest` against a
# connected device or running emulator.
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."
./gradlew testDebugUnitTest
