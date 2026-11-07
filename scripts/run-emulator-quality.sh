#!/usr/bin/env bash
set -euo pipefail

# The Python runner verifies the explicit emulator identity before installing or executing anything.
emulator_serial="${1:?An explicit emulator serial is required}"
python3 tools/run_instrumentation.py --serial "$emulator_serial" --coverage

# The CI AVD is fresh. Only this synthetic, isolated test's screenshots are collected, never audio.
adb -s "$emulator_serial" pull \
  /sdcard/Android/data/ar.didgetaller/files/rebuild-library-evidence \
  build/reports/quality/library-screenshots
