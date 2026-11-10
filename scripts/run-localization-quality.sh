#!/usr/bin/env bash
set -euo pipefail

# Run the two known regressions before the broader related suite, on synthetic data only.
serial="${1:?An explicit emulator serial is required}"
[[ "$serial" == emulator-* ]] || exit 2
hardware="$(adb -s "$serial" shell getprop ro.hardware | tr -d '\r')"
[[ "$hardware" == ranchu || "$hardware" == goldfish ]] || exit 2
mkdir -p build/reports/quality
adb -s "$serial" install -r app/build/outputs/apk/debug/app-debug.apk
adb -s "$serial" install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
run_case() {
  log="build/reports/quality/localization-$1.txt"
  timeout 300s adb -s "$serial" shell am instrument -w -r -e class "$2" ar.didgetaller.test/androidx.test.runner.AndroidJUnitRunner > "$log"
  python3 tools/run_instrumentation.py --verify-log "$log"
}
run_case regressions 'ar.didgetaller.ui.LocalizationUiTest#spanishMeasurementAndLibrary,ar.didgetaller.ui.LocalizationUiTest#configurationChangeRelocalizesWithoutRewritingSessionData'
run_case suite 'ar.didgetaller.ui.LocalizationUiTest,ar.didgetaller.ui.MetronomeUiTest#fourSectionsAndBackPreserveNavigationAndPauseMetronome'
