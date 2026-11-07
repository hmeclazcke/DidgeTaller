# Uncle Bob rebuild — IN PROGRESS

- Branch: `refactor/uncle-bob-rebuild`.
- Worktree: `C:/projects/DidgeTaller-uncle-bob`; original main checkout retained separately.
- Pinned baseline: `3a160043f9ebb3a420b1d89a9cf4ddfe7d1b5d67`.
- Main must never receive commits/pushes/merges from this effort. No physical-device access or private recordings.
- Official ub-mode SKILL.md/LICENSE byte-verified against `3da62bf3fcfd2034de41f30d920d2739e566171f`; unchanged.

## Verified foundation

- Baseline reproduced: both JVM variants, JaCoCo, Detekt/MCC and CPD passed; full logs ignored in checkpoints.
- Kotlin compiler PSI inventory: 59 files, 378 named functions, 1,455 lambdas, 73 explicit accessors.
- Conservative mapper now correlates 276 functions (rather than 8), and classifies every other named declaration: 57 Compose, 41 local functions included in parent complexity, 3 abstract declarations, 1 anonymous owner.
- The expanded JVM perspective exposes 97 high scores; this is newly visible existing debt, not an improvement. Android method coverage is measured separately.
- Parser ownership/overload/local-function test passes; 16 Python tests pass including eight new ambiguity/boundary correspondence cases.
- Original production bytecode frozen locally in ignored checkpoints/baseline-production.jar; 741 synthetic observable-output lines match after the DSP/trace reconstruction.

## Implemented production milestones

- `ab69dfa`: period comparison/search/refinement separated into YinPeriodSearch; trace builder owns omission/connection state. Original 43/33/15 hotspots become 9/entry-point 1/7; all extracted trace/search work stays below 10. Preserve exact finite acoustics, gaps, diagnostics and WAV bytes.
- `cf26f28`: framework-independent PlaybackController with stopped/preparing/ready and coalesced-seek state; SessionPlayer is an Android adapter. Twenty-two deterministic playback tests cover errors, stale callbacks, start selection, pause, loop, completion and disposal.
- LivePitch rejects nonfinite display input after a demonstrated red/green defect test. Finite acceptance policy is unchanged.
- Local integration: both JVM variants each 223 discovered / 215 passed / 0 failed / 8 protected skips; debug and Android-test APKs built. Lint 0 errors / 59 warnings / 10 hints after correcting the worktree-local SDK property and invalidating its stale lint cache. No lint rule was suppressed.
- JaCoCo: total JVM 30.66% lines / 27.84% branches; core 98.50% / 78.36%, with changed code denominators disclosed. CPD zero groups. Current mapping 324/395 Detekt functions; all 439 PSI named declarations classified. Ratchets pass with original coverage floors and exactly two explicit measured method replacements.
- Expanded PIT: 17 selected classes; first 1,143 mutants exposed missing assertions. Strengthened run: 1,146 total, 767 killed / 354 survived / 23 uncovered / 2 timeouts, 769/1,146 = 67.10% PIT-detected score; survivors remain unfinished. Focused 11/11 gate still enforced. Mutation scope does not cover the entire app.
- Controlled JVM DSP microbenchmark executed for both profiles; samples/allocation tradeoffs are recorded in REBUILD_CASE_STUDY. No Android CPU/battery/rendering claim.
- Capture ownership milestone: reproduced the delayed-finalizer writer-close defect on the clean emulator, then introduced one RecordingCapture per worker. Fifteen new JVM cases and all twelve RecordingHandoffTest cases pass, preserving saved bytes/index, discard failures, recovery and disposal contracts. No persisted format changed.
- Current local full JVM suites: 238 discovered/230 passed/8 protected skips each; 21 Python tests pass. All 444 PSI declarations accounted (329 mapped/71 unscored of 400 Detekt functions), 84 high scores. JVM 31.38% lines/28.62% branches; core 98.47%/78.58%. Static counts unchanged from previous milestone. Differential 741 outputs still exact.
- Initial 18-class PIT: 1,197 total, 806 killed/365 survived/24 uncovered/2 timed out. Reviewing capture survivors added duration/empty-chunk assertions; focused capture strengthened from 38 to 40 killed out of 51 (10 survived/1 uncovered), not a whole-suite score. Windows cannot exercise successful open-writer rename paths; inspect Linux CI mutations separately.
- Published 07c756c passed actual fast/differential run 42368989332, full emulator run 42368900284 (127 discovered/117 passed/10 skipped/0 failed), and manual mutation run 42368987707 (17-class 1,146 distribution). Report artifacts downloaded and inspected. Its Android line/branch coverage is separately 91.08%/68.12%. Subsequent capture changes need new CI evidence.
- Fresh local Didge_Rebuild_API37 AVD uses synthetic data only. Eight focused UI methods passed across ES/EN, portrait/landscape and compact 150% font. Screenshots are ignored local evidence, not new public promotional assets. Main/physical device/private fixtures untouched.

## Review and execution order

Reviewed directly: recording/playback/workspace adapters, persistence/cache stores, all original core Kotlin files, MainActivity, full ViewModel, ExplorerState/gestures/transport, measurement/diagnostics/analysis UI and most of TallerApp. Full UI/native-wrapper review is NOT yet complete.

1. Finish the remaining UI/native source review and record per-subsystem decisions. Do not claim file inventory equals review.
2. Capture-generation writer ownership is fixed and characterized. Continue reviewing microphone device creation/release error paths and ViewModel terminal-event ownership; the complete capture subsystem is not declared finished. Preserve all handoff tests' save/discard/index/byte assertions.
3. Address cache temporary-file writer collisions and index error recovery using synthetic historical fixtures; do not change persisted keys or rewrite original WAVs.
4. Separate remaining ViewModel/UI responsibilities, especially PlotSurface MCC 139 and MeasureScreen nested decisions; inspect actual compact/landscape/ES/EN/150%-font output.
5. Review all 354 mutation survivors and 23 uncovered outcomes, distinguishing real assertion gaps from Kotlin artifacts without marking unreviewed cases equivalent. Add recording-state mutation scope after its pure boundary exists.
6. Inspect actual feature-branch fast/differential/full-emulator/manual-PIT results and reports; record exact SHAs. Reproduce only evidenced failures.
7. Maintain the draft PR, rebuild case study and bilingual README. All major subsystems and final compatible-scope scorecard remain required before readiness.

## Exact local commands

Run in this worktree with protected fixture environment variables unset:

```powershell
.\gradlew.bat '-Pkotlin.incremental=false' :app:test :engineering-tools:test :app:jacocoDebugUnitTestReport detekt detektComplexity :engineering-tools:sourceInventory :app:qualityDuplication :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest
python tools/detekt_summary.py
python tools/coverage_summary.py
python tools/quality_gate.py
python -m unittest discover -s tools -p 'test_*.py'
.\gradlew.bat :app:qualityDifferential :app:qualityPerformanceComparison '-PdifferentialBaseline=checkpoints/baseline-production.jar'
.\gradlew.bat :app:qualityMutation :app:qualityMutationDomain
.\gradlew.bat :app:qualityMutationCapture
```

Detailed logs/reports are under ignored checkpoints and build reports. The original oracle JAR is local and code-only; fresh CI rebuilds it from the pinned commit. Do not overwrite it with rebuilt classes. Never execute protected fixture cases or operate the physical phone.

## Acceptance state

IN PROGRESS. Comprehensive architecture changes, high-risk reduction, broad mutation, complete UI review, final emulator/differential/persistence/performance validation and final scorecard remain unmet. Do not advertise a completed reconstruction or reuse baseline metrics as final results.
