#!/usr/bin/env bash
set -euo pipefail
serial="${1:?An explicit clean emulator serial is required}"
[[ "$serial" == emulator-* ]] || exit 2
hardware="$(adb -s "$serial" shell getprop ro.hardware | tr -d '\r')"
[[ "$hardware" == ranchu || "$hardware" == goldfish ]] || exit 2
mkdir -p build/reports/quality
collect() {
  status=$?
  trap - EXIT
  if ! adb -s "$serial" pull /sdcard/Android/data/ar.didgetaller/files/construction-evidence build/reports/quality/construction-screenshots; then status=1; fi
  adb -s "$serial" shell wm size reset || status=1
  adb -s "$serial" shell settings put system font_scale 1.0 || status=1
  exit "$status"
}
trap collect EXIT
adb -s "$serial" install -r app/build/outputs/apk/debug/app-debug.apk
adb -s "$serial" install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
run_case() {
  log="build/reports/quality/construction-$1.txt"
  timeout 600s adb -s "$serial" shell am instrument -w -r -e class "$2" -e evidenceLabel "$1" ar.didgetaller.test/androidx.test.runner.AndroidJUnitRunner > "$log"
  python3 tools/run_instrumentation.py --verify-log "$log"
}
run_case default ar.didgetaller.ui.ConstructionUiTest
for language in es en; do
  adb -s "$serial" shell cmd locale set-app-locales ar.didgetaller --user 0 --locales "$language"
  adb -s "$serial" shell am force-stop ar.didgetaller
  run_case "$language-normal" 'ar.didgetaller.ui.ConstructionUiTest#completeWoodWorkflowSearchSaveToleranceAndAllExports'
done
adb -s "$serial" shell wm size 720x1280
adb -s "$serial" shell settings put system font_scale 1.5
for language in en es; do
  adb -s "$serial" shell cmd locale set-app-locales ar.didgetaller --user 0 --locales "$language"
  adb -s "$serial" shell am force-stop ar.didgetaller
  run_case "$language-150" 'ar.didgetaller.ui.ConstructionUiTest#measuredPvcCatalogPersistsAndProducesCorrectAssemblyWithoutChangingLegacyDesigns'
done
adb -s "$serial" shell wm size 1280x720
run_case landscape-150 'ar.didgetaller.ui.ConstructionUiTest#completeWoodWorkflowSearchSaveToleranceAndAllExports'
