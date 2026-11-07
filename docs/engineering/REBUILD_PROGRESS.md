# Uncle Bob rebuild — IN PROGRESS

- Branch: refactor/uncle-bob-rebuild; draft PR: https://github.com/hmeclazcke/DidgeTaller/pull/1. Do not merge.
- Worktree: C:/projects/DidgeTaller-uncle-bob. Original main checkout retained separately and clean.
- Original main: 3a160043f9ebb3a420b1d89a9cf4ddfe7d1b5d67. Never commit/push to main or rewrite history.
- Previous persistence implementation/test/configuration HEAD: 6c6f00d (resolve the full SHA with git rev-parse 6c6f00d). Later evidence-only commits may advance this checkpoint with unchanged runtime/test inputs; use git rev-parse HEAD for its current revision.
- Official ub-mode SKILL.md/LICENSE unchanged, byte-verified against upstream 3da62bf3fcfd2034de41f30d920d2739e566171f.
- No Samsung access, private recording access, format migration or metronome implementation. Never enable protected fixture arguments/environment variables without current authorization.

## Capture/workflow/raster continuation - IN PROGRESS

Resumed at 9cd18f6; main remains 3a160043f9ebb3a420b1d89a9cf4ddfe7d1b5d67. [Capture event design](CAPTURE_EVENT_OWNERSHIP.md) records the reproduced late-discard failure and ownership contracts. RecordingCapture now emits a single claimed terminal envelope, MicrophoneEngine delivers its identity on main, and pure CaptureHandoff/RecordingCoordinator own snapshots, phases, notification ownership, disposal and publication acknowledgement. TallerViewModel publishes one immutable Compose recording snapshot. Delayed permission callbacks cannot reopen disposed hardware. Existing formats and writer/cache/index protections remain unchanged.

SessionWorkspace delegates power-to-ARGB conversion to pure, cancellable SpectrogramRaster. Known-power, reference, orientation, invalid-matrix and cancellation tests preserve pixels and measured data. Render MCC decreases 17 to 13 under the same Detekt rules; the coordinator's completion policy is MCC 8 and raster conversion MCC 7. PlotSurface MCC 139 and broader workspace/ViewModel responsibilities remain unfinished.

Final local combined inputs after lifecycle correction: 622 JVM discovered / 606 passed / 16 protected skips / zero failures; 35.53% JVM lines and 32.73% branches; core 98.66%/80.13%. Ratchets pass: 395 measured / 90 unscored of 485 Detekt functions, 78 above-target JVM CRAP-L, CPD zero >=100-token groups. All 529 PSI declarations are accounted for: 395 measured, 40 nested, 23 bodyless, 13 anonymous-owner and 58 Compose-transformed. The fourteen additional unscored declarations are seven observer interface declarations and seven anonymous Android adapters, not lost mappings or hidden exclusions. Detekt 1,969 informational findings; Lint zero errors/52 warnings/8 hints. New scopes and host differences prevent treating percentage changes as equal-denominator coverage improvement.

Focused Windows PIT: handoff/coordinator 99 mutants, 94 killed/5 surviving/zero uncovered; raster 31/31 killed after strengthening boundary tests. Five workflow survivors were reviewed: an unused Kotlin Unit bridge return and ownership/phase cleanup guards dependent on the remaining invariants; they remain reported, not counted as killed. Observer-delivery, unregistered-import and disposed-begin survivors were killed by meaningful test improvements. Capture remains host-specific: 54 mutants, 44 killed/9 surviving/1 uncovered on Windows; Linux evidence must be reported separately. The initial de8df92 broad CI run passed: 1,497 mutants, 1,093 killed/386 survived/16 uncovered/2 timeouts, 29 classes, 78 seconds engine time. The lifecycle correction changes a selected class, so its new broad CI result remains pending.

Local full build, both JVM variants, coverage, Detekt, CPD, Lint, Android APK compilation, 741 differential outputs, 21 Python tests, compiler inventory test and localization pass. The original API 37 red test actually executed (1 failure); the later verification process crashed before discovering tests after a slow boot/low-memory run. It is not an Android pass. No screenshots from that aborted run are accepted. Initial clean API 35 CI at de8df92 executed all 147 tests: 136 passed/1 failed/10 skipped. Its genuine failure was a legacy assertion equating a nullable error with an empty message after disposal. The corrected test requires a nonblank error for live owners and no notice after disposal, retains every WAV/index assertion and explicitly injects a late device error after clearing. Device errors now pass through the same pure lifecycle boundary; failed starts also end stale listening state without relying on an error callback. Two additional JVM error contracts pass. New full API 35 CI is required; no framework pass is claimed yet.

Next exact production task after the current CI gate: separate SessionWorkspace request loading/cache/raster publication behind a controlled loader, characterize session switching/open-render-cancel and late progress/completion with deterministic tests, and retain original tile coordinates while a replacement loads. Then separate PlotSurface's rendering decisions and TallerViewModel's remaining analysis/navigation jobs. Do not restart persistence or writer-generation investigations. Broader mutation survivors, acoustic hotspots, lifecycle/error/performance evidence and full application acceptance remain unresolved.

The persistence section below is historical evidence for the prior milestone.

## Current persistence/library continuation ? IN PROGRESS

Resumed existing branch at 5f93e23. Persistence milestone c132642 replaces corrupt-as-empty mutation, per-instance writer locks and destructive unknown-row filtering. See [design/invariants](PERSISTENCE_REBUILD.md). Current library work replaces six separately published collections/counters with LibrarySnapshot/LibraryCatalog, adds explicit ES/EN recovery confirmation and catches metadata-command failures at the UI boundary. The new UI failure test first reproduced an uncaught StoredDocument.Unavailable on the main thread; the source correction passed the clean API 35 suite; the local failure chronology is retained below.

Local final-source JVM checks: 548 discovered / 532 passed / 16 protected skips / zero failures across debug/release. JVM lines/branches 33.69%/31.23%; pure core 98.54%/78.93%; data package 38.85%/33.46%. Different source denominators and host from the previous Linux result. Ratchets pass: 361 measured / 76 unscored Detekt functions; 79 above-target JVM CRAP-L; all 481 PSI declarations explicitly classified (361 measured, 40 nested, 16 bodyless, 6 anonymous-owner, 58 Compose-transformed). CPD zero >=100-token groups. Detekt 1,966; Lint zero errors / 52 warnings / 9 hints. Lint UseKtx observations decrease 12 to 4 through consolidation; dependency-advisory counts are time-dependent. Parser/Python/localization and 741 differential outputs pass.

The expanded local Windows PIT execution completed: 26 bytecode classes, 1,369 mutants, 960 killed / 390 survived / 17 uncovered / 2 timeouts; detected score 962/1369 = 70.27%, not whole-app strength. Scoped production/test inputs are unchanged by subsequent ViewModel-only error handling. The existing PlotTouch gate remains 11/11. Windows capture file-lock limitations remain separate from Linux evidence. Do not claim all survivors are equivalent or killed.

Android: final persistence focus 25/25 passed on the owned synthetic API 37 emulator. The initial three library cases passed semantics, but seven screenshots were rejected after visual inspection exposed a System UI ANR overlay. Screenshot capture now requires the active accessibility window to belong to the test activity. The later four-case run aborted before discovery with `Process crashed`; the crash buffer records the API 37 launcher, not a proven Didge Java failure. Heavy build/emulator overlap exhausted host memory. Failed evidence is retained under ignored checkpoints. Do not count this as a final Android pass or valid screenshots. Clean API 35 CI subsequently resolved this framework gate; see the final evidence below. CI now collects only LibraryRecoveryUiTest's synthetic screenshots after a successful full run, with fail-fast shell propagation.

Next exact production cycle: characterize MicrophoneEngine/TallerViewModel terminal capture events, freeze the capture's persistence metadata before asynchronous publication, and enforce callback/cancellation ownership. Preserve the already-correct per-worker RecordingCapture writer model. Then separate SessionWorkspace/renderers/PlotSurface responsibilities and canonical Loading versus Unavailable. Do not repeat the persistence reconstruction, initial all-file review or successful unchanged deep checks. Whole-preference/storage-device recovery and broader process-death fault injection remain explicit persistence limitations. Keep PR #1 draft.

### 22655bb remote checkpoint and visual follow-up

Fast PR run 42412056518, full emulator/coverage run 42412053137 and manual mutation run 42412071596 all passed. Downloaded evidence confirms Linux JVM 548 discovered / 532 passed / 16 protected skips; 33.71% lines / 31.33% branches, pure core 98.61% / 79.23%. Full API 35: 144 discovered / 134 passed / 10 explicit skips, separate coverage 91.44% / 68.75%, data package 94.92% / 65.24%. Linux PIT: 26 classes, 1369 mutants, 970 killed / 381 survived / 16 uncovered / 2 timeouts; detected 71.00%, engine 78 seconds. All selected mutation sources/tests remain unchanged by the later dialog-only fix.

Seven API 35 screenshots were actually inspected. Normal recovery is readable and its audio/index assertions pass, but the caller-locale Spanish fixture exposed an English dialog and an ineffective platform-font simulation. Follow-up resolves localized content outside the platform dialog context, asserts translated strings, makes its explanation scrollable, and adds two real Android locale/font/compact runs. Compilation/Lint/Detekt/YAML/shell validation passed; the new CI configuration runs must be observed. Do not present the first green suite as proof that this visual gap was absent. No valid physical/Samsung test is claimed.

## Final verified continuation at 6c6f00d

The continuation remains **IN PROGRESS** at application level. This milestone is published and validated, not permission to merge. c132642 rebuilds persistence, 22655bb publishes coherent library state/recoverable metadata commands, and 6c6f00d fixes caller-locale dialog ownership and verifies actual platform font/locale configurations. Later documentation-only commits do not invalidate unchanged source/test evidence.

| Check | Previous d90926c | Current 6c6f00d evidence |
| --- | --- | --- |
| JVM debug + release | 492 discovered / 476 passed / 16 protected skips | 548 / 532 / 16, zero failures; 274/266/8 per variant |
| Complete API 35 suite | 129 / 119 / 10 skips | 144 / 134 / 10, zero failures |
| Additional real configuration runs | Historical separate configurations | Two targeted recovery cases, both passed; Android en/es, font_scale 1.5, 720x1280, three-button navigation |
| JVM Linux lines / branches | 31.51% / 28.84% | 33.69% / 31.33%, changed source denominator |
| Pure core JVM lines / branches | 98.61% / 79.18% | 98.61% / 79.23%; no acoustic policy change |
| Separate Android lines / branches | 91.15% / 68.10% | 91.41% / 68.72%; excludes the two later configuration runs |
| Android data package | See prior report | 94.92% lines / 65.24% branches |
| Function correspondence | 330 measured / 71 unscored | 361 / 76; all 481 PSI declarations classified |
| High CRAP-L | JVM 83; separate Android 32 | JVM 79; separate Android 30 (local mapper on exact CI XML) |
| RecordingStore.list MCC | 16 | 4; codec responsibilities now independently testable |
| PMD CPD | Zero >=100-token groups | Zero, unchanged threshold |
| Detekt | 1970 informational findings | 1965 under the same configuration |
| Android Lint | 0 errors / 59 warnings / 10 hints | 0 / 53 / 9 remotely; dependency-advisory counts vary over time |
| Differential outputs | 741 identical | 741 identical; no broader persistence-differential claim |
| Python tooling | 21 passed | 21 passed in CI; six additional local shell failure probes passed separately |
| Linux PIT | 19 classes; 1198 mutants | 26 classes; 1369 mutants, 970 killed / 381 survived / 16 uncovered / 2 timeouts; detected 972/1369 = 71.00% |

Actual successful runs:

- [Fast PR gates](https://github.com/hmeclazcke/DidgeTaller/actions/runs/42413957308), head 6c6f00d: both JVM variants, APK/test compilation, Lint, localization, JaCoCo, Detekt, compiler inventory, differential, CPD, Python and enforced ratchets.
- [Full emulator and real configurations](https://github.com/hmeclazcke/DidgeTaller/actions/runs/42413952372), head 6c6f00d: complete instrumentation plus separate en/es compact 150%-font runs, coverage and synthetic screenshot collection.
- [Manual mutations](https://github.com/hmeclazcke/DidgeTaller/actions/runs/42412071596), head 22655bb: enforced PlotTouch 11/11 plus informational 26-class domain score. Selected production/tests/build/config inputs are byte-unchanged at 6c6f00d; no unnecessary rerun was made for dialog-only changes. Engine time 78 seconds.

All 71 production Kotlin source hashes from the final CI inventory match the local source. Seven newly selected persistence/JSON/library classes contribute 171 mutants: 145 killed / 26 survived / zero uncovered. This does not eliminate existing-scope survivors or establish whole-application mutation strength. The 16 JVM and seven Android private guards remain protected; three other Android configuration cases retain their previously recorded separate execution evidence.

Thirteen final authentic PNGs were downloaded and visually inspected: seven default/caller-locale views and six real compact en/es platform-font views. Their local location is checkpoints/library-final-ci-screenshots; reports/ZIPs/logs are under checkpoints/github-ci. The compact explanation is intentionally scrollable, while confirm/cancel stay outside system navigation. Earlier API 37 ANR images and the first CI caller-locale mismatch remain rejected historical evidence, not final screenshots. Screenshots are synthetic test evidence, not README showcase assets. No new rendering-performance claim is made.

Six local fault probes stubbed every device command and verified preflight failure, actual JUnit-failure parsing despite adb zero, timeout, missing image collection, failed settings restoration and successful cleanup. They are local probes (checkpoints/probe_emulator_script.py), not six additional Android or active CI Python tests.

Main remains 3a160043f9ebb3a420b1d89a9cf4ddfe7d1b5d67 and clean; no merge, Samsung/private-audio access or format migration occurred. The official SKILL.md and MIT license still match the pinned upstream hashes. The PR must remain draft until remaining whole-application architecture criteria are met.

## Implemented and verified

1. Compiler-PSI source inventory and conservative Detekt/JaCoCo owner/signature/body correspondence replace the eight-function allowlist. Every one of 445 named declarations is classified. Constructors/accessors/lambdas are not silently claimed as independent source-function scores.
2. DSP period evidence/search/refinement and trace assembly have cohesive owners; original 43/33/15 hotspots become 9/entry point 1/7, with actual extracted work below 10. LivePitch nonfinite display input was fixed after a red/green regression. Finite acoustic policy is unchanged.
3. Pure PlaybackController owns stopped/preparing/ready state and latest-only seeks behind MediaPlayer/scheduler ports; 22 deterministic contracts cover stale callbacks, seeking, loops, failures and disposal.
4. Per-worker RecordingCapture prevents an old microphone worker closing its replacement's writer. Sixteen JVM contracts plus all twelve Android handoff cases preserve duration, discard, close failure, bytes/index integrity, recovery and disposal.
5. Independent AtomicCacheFile temporary ownership fixes a reproduced same-key writer corruption race. Six JVM contracts and actual Android cache integration preserve formats and original WAVs. Canonical frequency text has explicit locale-independent boundary tests.
6. PIT actually mutates 19 bytecode classes across domain responsibilities. Reviewing capture survivors fixed a permissive test oracle and missing zero-append/full-disk assertion; capture now kills 51/51 on Linux. Broad survivors remain visible.
7. Initial direct responsibility review covers all 63 runtime Kotlin files and owned JNI boundary: REBUILD_REVIEW.md. Reading is not completed architectural acceptance.

## Actual evidence at d90926c

| Check | Result |
| --- | --- |
| JVM debug + release | 492 discovered; 476 passed; 0 failed; 16 protected skips (246/238/0/8 each) |
| Full clean API 35 emulator | 129 discovered; 119 passed; 0 failed; 10 skips (7 private guards, 3 configuration cases) |
| JVM Linux line / branch | 31.51% / 28.84%; pure core 98.61% / 79.18% |
| Separate Android line / branch | 91.15% / 68.10%; never merged with JVM |
| JVM CRAP-L | 330 measured / 71 unscored of 401 Detekt functions; 83 above target |
| Separate Android CRAP-L | Same 330 mapped / 71 unscored; 32 above target; local informational mapping of exact CI XML |
| Detekt / Lint / CPD | 1,970 unchanged-rule findings; Lint 0 errors/59 warnings/10 hints; CPD zero groups >=100 tokens |
| PIT domain | 1,198 generated: 825 killed, 355 survived, 16 uncovered, 2 timed out; PIT detected 827/1,198 = 69.03%; 109 seconds engine time |
| PIT focused / capture | PlotTouch 11/11 enforced; capture 51/51 Linux, selected scope only |
| Tool tests | 21 Python checks and compiler-PSI JUnit test pass |
| Differential contracts | 741 synthetic output lines byte-identical against compiled original main |

Windows open-file rename paths differ: its current focused capture result is 41 killed/9 survived/1 uncovered. Do not merge or substitute host results. JVM and Android report denominators differ from original main after restructuring; consult the case study before claiming percentage improvement. Android-dependent JVM zeros are not absence of all tests. Original measured coverage floors and explicit signature-lineage ceilings remain enforced; broad mutation and Android coverage are informational.

- Fast/differential CI: https://github.com/hmeclazcke/DidgeTaller/actions/runs/42386035464
- Actual full Android/coverage: https://github.com/hmeclazcke/DidgeTaller/actions/runs/42386030241
- Actual 19-class mutations: https://github.com/hmeclazcke/DidgeTaller/actions/runs/42386032298
- Duplicate automatic fast push cancelled by concurrency, not counted as a pass. Artifacts downloaded under ignored checkpoints/github-ci; retention is seven days.

## Emulator and visual evidence

Fresh synthetic-only Didge_Rebuild_API37, explicit serial emulator-5558, 1080x2400/density 420; no historical emulator fixtures used. Twenty-six initial EN/ES screenshots inspected across session, diagnostics, spectrum, spectrogram, waveform and compact 150%-font states. Their initial hardware-key configuration does not prove software-navigation insets.

The hand-created AVD inherited hw.mainKeys=true, so System UI's gesture handler was unattached despite gestural resource mode. Set hw.mainKeys=no only in this AVD, cold-booted without wiping data, verified the handler attached/enabled, and recovered a cold-boot System UI ANR under host memory pressure by stopping idle build daemons. The exact unchanged edge-Back test then passed 1/1, retaining playback through fullscreen and releasing it on exit. A native MainActivity normal/wide/Back probe also passed without recording or key-Back fallback. Two corrected soft-key compact Spanish portrait/landscape 150% tests passed; their images and three native screenshots were inspected (31 selected images total). Failed diagnostic logs are retained as invalid-environment evidence; no navigation test patch/timeout increase remains. All three configuration-dependent cases skipped by default CI have executed locally; private cases remain guarded.

The AVD remains reusable; after evidence capture restore default font/size before stopping only this emulator. Never wipe another AVD, use private audio, or select a physical device. The original-main UI oracle is an additional detached ignored worktree at checkpoints/original-ui, not a replacement for main.

## Next exact engineering cycles

1. Complete the current persistence/library CI and screenshot evidence. Corrupt-index reproduction, preserved recovery, shared publication, unknown-row retention and library snapshot ownership are implemented; do not restart them. Further process-death fault injection and whole-preference/storage disaster recovery remain distinct work.
2. Continue microphone device construction/release/error ownership and ViewModel terminal-event coordination. Writer generation isolation is fixed; do not restart that investigation or weaken handoff byte/index assertions.
3. Separate ViewModel responsibilities and large UI/renderer decisions, especially PlotSurface MCC 139, MeasureScreen, SessionPlot and gesture transitions. Protect real native behavior and visuals first. Model canonical trace Loading separately from final Unavailable; early screenshots precede asynchronous completion.
4. Review latest 355 mutation survivors/16 uncovered and other core high-risk functions (WavAnalysis 29, BoreOptimizer/DroneComponent classification 23, HarmonicAnalysis 20). Use characterization/differential protection; do not alter thresholds or split meaningless wrappers to game scores. Expand meaningful pure scope when justified.
5. Keep case study, equivalent README languages and draft PR accurate. Final comprehensive architecture acceptance, compatible-scope scorecard and broader performance/lifecycle/error evidence remain unmet. Do not declare the full rebuild complete.

## Reproducible commands

See docs/quality/QUALITY_GUIDE.md for prerequisites, report scopes and safe emulator selection. Run with protected fixture environment variables unset.

```powershell
.\gradlew.bat '-Pkotlin.incremental=false' :app:test :engineering-tools:test :app:jacocoDebugUnitTestReport detekt detektComplexity :engineering-tools:sourceInventory :app:qualityDuplication :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest
python tools/detekt_summary.py
python tools/coverage_summary.py
python tools/quality_gate.py
python -m unittest discover -s tools -p 'test_*.py'
.\gradlew.bat :app:qualityDifferential :app:qualityPerformanceComparison '-PdifferentialBaseline=checkpoints/baseline-production.jar'
.\gradlew.bat :app:qualityMutation :app:qualityMutationDomain
.\gradlew.bat :app:qualityMutationCapture :app:qualityMutationCache
```

The local original production JAR was compiled before implementation edits. Do not overwrite it with rebuilt classes. CI rebuilds its oracle from pinned main. Reuse passing evidence for unchanged inputs; do not run broad suites or mutation merely because the tools exist.
