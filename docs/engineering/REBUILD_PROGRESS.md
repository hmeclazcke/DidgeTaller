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
- Cache milestone: a controlled emulator test reproduced two same-key writers mixing bytes through a shared temporary file. AtomicCacheFile now owns unique same-directory temporary files and atomic whole-value publication. Six JVM contracts and all 27 selected Android persistence/capture tests pass; formats/keys and original WAVs unchanged. Focused cache PIT kills its one DEFAULTS mutant (narrow scope, not comprehensive I/O mutation coverage). Full current JVM/static/build/differential and ratchets pass.
- Native edge-Back on the fresh API 37 AVD fails while default API 35 CI skips this configuration-dependent test. Lifecycle/longer-swipe and enabled-animation probes did not fix it; those test edits were removed. The exact original main test also FAILS on that same clean AVD (checkpoints/edge-original-main.log, source 3a16004), so this is not a newly introduced behavior. The rebuilt APKs were restored on the emulator afterward. Do not call this configuration passed or silently substitute KEYCODE_BACK. Next diagnosis must distinguish the Compose test-clock/native predictive-Back boundary from normal MainActivity behavior; do not keep retrying unchanged swipes. Other compact ES/EN cases pass.
- Initial reading is complete for all 63 runtime Kotlin files and the owned JNI boundary; REBUILD_REVIEW.md records each responsibility and retained/rebuilt/unresolved decision. This is not a claim that every architectural issue is fixed. New pure canonical-frequency text assertions protect export formatting under a comma-decimal UI locale.
- Latest integrated local evidence after the cache and canonical-text assertions: each JVM variant 245 discovered/237 passed/8 protected skips/0 failures, total 490/474/16. JVM coverage 31.49% lines/28.73% branches, core 98.54%/78.88%; 330 measured/71 unscored, 83 high. Detekt 1,970; Lint 0 errors/59 warnings/10 hints; CPD no groups. The 19-class broad mutation selection is configured and must execute in remote manual CI; do not relabel the previous 18-class local result as current.
- Code 3ec5fd2 now has actual passing fast/differential CI 42384700836, full API 35 emulator CI 42384695004 (129 discovered/119 passed/10 skipped/0 failed), and manual 19-class mutation CI 42384739383. Artifacts confirm separate Android 91.15% lines/68.24% branches; Linux JVM 31.51%/28.84%, core 98.61%/79.18% (open-file rename success adds coverage versus Windows). PIT: 1,198 total, 821 killed/359 survived/16 uncovered/2 timed out, detected 823/1,198 = 68.70%, 115 seconds engine time. RecordingCapture Linux scope is 47 killed/4 survived of 51. Duplicate automatic fast push cancelled by concurrency; no false passing status.
- Survivor review then exposed overly permissive portable discard expectations. Tests now probe OS rename capability independently using RandomAccessFile, rather than accepting the production result as their oracle. Added a full-disk-after-duration-limit contract kills the zero-append boundary mutant. Production code is unchanged. Both variants now 246 discovered/238 passed/8 protected skips/0 failed each; Windows focused capture PIT 41 killed/9 survived/1 uncovered of 51. This final test strengthening still requires its own remote evidence; do not label the earlier Linux result as a rerun of these assertions.

## Review and execution order

Initial direct source reading and file inventory are complete; see REBUILD_REVIEW.md. Material redesign and verification remain unfinished across several reviewed responsibilities.

1. Continue the per-subsystem redesign cycles in REBUILD_REVIEW.md, using the existing characterization and exact source evidence. Do not repeat unchanged source-reading audits or confuse the completed reading checklist with implementation acceptance.
2. Capture-generation writer ownership is fixed and characterized. Continue reviewing microphone device creation/release error paths and ViewModel terminal-event ownership; the complete capture subsystem is not declared finished. Preserve all handoff tests' save/discard/index/byte assertions.
3. Cache temporary-file collision is fixed. Continue index corruption and interrupted transaction recovery with synthetic historical fixtures; preserve raw corrupt index evidence, keys and original WAVs.
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
.\gradlew.bat :app:qualityMutationCache
```

Detailed logs/reports are under ignored checkpoints and build reports. The original oracle JAR is local and code-only; fresh CI rebuilds it from the pinned commit. Do not overwrite it with rebuilt classes. Never execute protected fixture cases or operate the physical phone.

## Acceptance state

IN PROGRESS. Comprehensive architecture changes, high-risk reduction, broad mutation, complete UI review, final emulator/differential/persistence/performance validation and final scorecard remain unmet. Do not advertise a completed reconstruction or reuse baseline metrics as final results.
