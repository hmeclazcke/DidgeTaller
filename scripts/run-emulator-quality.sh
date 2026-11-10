#!/usr/bin/env bash
set -euo pipefail

# The Python runner verifies the explicit emulator identity before installing or executing anything.
emulator_serial="${1:?An explicit emulator serial is required}"
[[ "$emulator_serial" == emulator-* ]] || exit 2
hardware="$(adb -s "$emulator_serial" shell getprop ro.hardware | tr -d '\r')"
[[ "$hardware" == ranchu || "$hardware" == goldfish ]] || exit 2

# Change settings only after the runner has verified this fresh emulator. Preserve failure status.
collect_and_restore() {
  result=$?
  trap - EXIT
  # Keep partial synthetic evidence when a configuration assertion fails, without hiding that failure.
  if ! adb -s "$emulator_serial" pull \
    /sdcard/Android/data/ar.didgetaller/files/rebuild-library-evidence \
    build/reports/quality/library-screenshots; then result=1; fi
  if ! adb -s "$emulator_serial" pull \
    /sdcard/Android/data/ar.didgetaller/files/session-evidence \
    build/reports/quality/session-screenshots; then result=1; fi
  if ! adb -s "$emulator_serial" pull \
    /sdcard/Android/data/ar.didgetaller/files/metronome-evidence \
    build/reports/quality/metronome-screenshots; then result=1; fi
  if ! adb -s "$emulator_serial" shell wm size reset; then result=1; fi
  if ! adb -s "$emulator_serial" shell settings put system font_scale 1.0; then result=1; fi
  if ! adb -s "$emulator_serial" shell cmd locale set-app-locales ar.didgetaller --user 0 --locales en; then result=1; fi
  exit "$result"
}
trap collect_and_restore EXIT
python3 tools/run_instrumentation.py --serial "$emulator_serial" --coverage

# Compose-local overrides do not reproduce a separate Android dialog window's configuration.
# Verify actual compact screen, application locales and system font at fresh process boundaries.
adb -s "$emulator_serial" shell wm size 720x1280
adb -s "$emulator_serial" shell settings put system font_scale 1.5
for language in en es; do
  adb -s "$emulator_serial" shell cmd locale set-app-locales ar.didgetaller --user 0 --locales "$language"
  adb -s "$emulator_serial" shell am force-stop ar.didgetaller
  timeout 180s adb -s "$emulator_serial" shell am instrument -w -r \
    -e class 'ar.didgetaller.ui.LibraryRecoveryUiTest#libraryRecoveryRequiresConfirmationAndPreservesOriginalIndexAndWav' \
    -e libraryExpectedLanguage "$language" -e libraryEvidenceLabel "$language-platform150" \
    ar.didgetaller.test/androidx.test.runner.AndroidJUnitRunner \
    > "build/reports/quality/library-configuration-$language.txt"
  python3 tools/run_instrumentation.py --verify-log "build/reports/quality/library-configuration-$language.txt"
  timeout 180s adb -s "$emulator_serial" shell am instrument -w -r \
    -e class 'ar.didgetaller.ui.SessionEvidenceUiTest#captureAndPreserveOriginals' \
    -e evidenceLabel "$language-workspace150" \
    -e evidenceExpectedLanguage "$language" \
    ar.didgetaller.test/androidx.test.runner.AndroidJUnitRunner \
    > "build/reports/quality/workspace-configuration-$language.txt"
  python3 tools/run_instrumentation.py --verify-log "build/reports/quality/workspace-configuration-$language.txt"
  timeout 180s adb -s "$emulator_serial" shell am instrument -w -r \
    -e class 'ar.didgetaller.ui.MetronomeUiTest#transportSettingsAndVisualCaptureInterlock' \
    -e evidenceLabel "$language-metronome150" \
    ar.didgetaller.test/androidx.test.runner.AndroidJUnitRunner \
    > "build/reports/quality/metronome-configuration-$language.txt"
  python3 tools/run_instrumentation.py --verify-log "build/reports/quality/metronome-configuration-$language.txt"
done
adb -s "$emulator_serial" shell wm size 1280x720
timeout 180s adb -s "$emulator_serial" shell am instrument -w -r \
  -e class 'ar.didgetaller.ui.SessionEvidenceUiTest#captureAndPreserveOriginals' \
  -e evidenceLabel 'landscape-workspace150' \
  -e evidenceExpectedLanguage 'es' \
  ar.didgetaller.test/androidx.test.runner.AndroidJUnitRunner \
  > build/reports/quality/workspace-configuration-landscape.txt
python3 tools/run_instrumentation.py --verify-log build/reports/quality/workspace-configuration-landscape.txt
timeout 180s adb -s "$emulator_serial" shell am instrument -w -r \
  -e class 'ar.didgetaller.ui.MetronomeUiTest#transportSettingsAndVisualCaptureInterlock' \
  -e evidenceLabel 'landscape-metronome150' \
  ar.didgetaller.test/androidx.test.runner.AndroidJUnitRunner \
  > build/reports/quality/metronome-configuration-landscape.txt
python3 tools/run_instrumentation.py --verify-log build/reports/quality/metronome-configuration-landscape.txt
