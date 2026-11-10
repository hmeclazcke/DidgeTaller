#!/usr/bin/env bash
set -euo pipefail

# Manual focused evidence; this never represents the complete instrumentation suite.
serial="${1:?An explicit emulator serial is required}"
[[ "$serial" == emulator-* ]] || exit 2
hardware="$(adb -s "$serial" shell getprop ro.hardware | tr -d '\r')"
[[ "$hardware" == ranchu || "$hardware" == goldfish ]] || exit 2
mkdir -p build/reports/quality
result=0
collect() {
  status=$?
  trap - EXIT
  if ! adb -s "$serial" pull /sdcard/Android/data/ar.didgetaller/files/metronome-evidence build/reports/quality/metronome-screenshots; then status=1; fi
  if ! adb -s "$serial" pull /sdcard/Android/data/ar.didgetaller/files/measure-evidence build/reports/quality/measure-screenshots; then status=1; fi
  if ! adb -s "$serial" pull /sdcard/Android/data/ar.didgetaller/files/recovery build/reports/quality/measure-synthetic-screenshots; then status=1; fi
  if ! adb -s "$serial" shell wm size reset; then status=1; fi
  if ! adb -s "$serial" shell settings put system font_scale 1.0; then status=1; fi
  if ! adb -s "$serial" shell cmd locale set-app-locales ar.didgetaller --user 0 --locales en; then status=1; fi
  exit "$status"
}
trap collect EXIT
adb -s "$serial" install -r app/build/outputs/apk/debug/app-debug.apk
adb -s "$serial" install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
run_case() {
  label="$1"
  selection="$2"
  log="build/reports/quality/metronome-configuration-$label.txt"
  if ! timeout 240s adb -s "$serial" shell am instrument -w -r -e class "$selection" -e evidenceLabel "$label" -e studioLabel "$label" ar.didgetaller.test/androidx.test.runner.AndroidJUnitRunner > "$log"; then result=1; fi
  if ! python3 tools/run_instrumentation.py --verify-log "$log"; then result=1; fi
}
run_case default 'ar.didgetaller.ui.MetronomeUiTest,ar.didgetaller.ui.LocalizationUiTest#compactEnglishNavigationKeepsEveryLabelAtLargeFontScale,ar.didgetaller.ui.LocalizationUiTest#compactSpanishNavigationKeepsEveryLabelAtLargeFontScale,ar.didgetaller.ui.LocalizationUiTest#largeEnglishNavigationFitsAtDoubleFontScale,ar.didgetaller.ui.LocalizationUiTest#largeSpanishNavigationFitsAtDoubleFontScale,ar.didgetaller.ui.SessionEvidenceUiTest#scrollableFullscreenPreservesPinchPanAndPausedScrubbing'
run_case measure 'ar.didgetaller.ui.MeasureDiscardUiTest,ar.didgetaller.ui.RecoveryUiTest#controlledPcmReachesLiveMeterAndProfilesResetWithoutGraphFiltering,ar.didgetaller.ui.RecoveryUiTest#acousticUncertaintyDetailsDoNotBecomeNotesOrPersistedMeasurements'
adb -s "$serial" shell cmd locale set-app-locales ar.didgetaller --user 0 --locales es
adb -s "$serial" shell am force-stop ar.didgetaller
run_case es-normal 'ar.didgetaller.ui.MetronomeUiTest#transportSettingsAndVisualCaptureInterlock,ar.didgetaller.ui.MeasureDiscardUiTest#activeLayoutAndConfirmationAreAccessible,ar.didgetaller.ui.RecoveryUiTest#controlledPcmReachesLiveMeterAndProfilesResetWithoutGraphFiltering'
adb -s "$serial" shell wm size 720x1280
adb -s "$serial" shell settings put system font_scale 1.5
for language in en es; do
  adb -s "$serial" shell cmd locale set-app-locales ar.didgetaller --user 0 --locales "$language"
  adb -s "$serial" shell am force-stop ar.didgetaller
  run_case "$language-metronome150" 'ar.didgetaller.ui.MetronomeUiTest#transportSettingsAndVisualCaptureInterlock'
  run_case "$language-measure150" 'ar.didgetaller.ui.MeasureDiscardUiTest#activeLayoutAndConfirmationAreAccessible,ar.didgetaller.ui.RecoveryUiTest#controlledPcmReachesLiveMeterAndProfilesResetWithoutGraphFiltering'
done
adb -s "$serial" shell wm size 1280x720
run_case landscape-metronome150 'ar.didgetaller.ui.MetronomeUiTest#transportSettingsAndVisualCaptureInterlock'
run_case landscape-measure150 'ar.didgetaller.ui.MeasureDiscardUiTest#activeLayoutAndConfirmationAreAccessible,ar.didgetaller.ui.RecoveryUiTest#controlledPcmReachesLiveMeterAndProfilesResetWithoutGraphFiltering'
exit "$result"
