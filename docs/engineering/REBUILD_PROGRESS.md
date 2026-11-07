# Uncle Bob rebuild — IN PROGRESS

- Branch: refactor/uncle-bob-rebuild; draft PR: https://github.com/hmeclazcke/DidgeTaller/pull/1. Do not merge.
- Worktree: C:/projects/DidgeTaller-uncle-bob. Original main checkout retained separately and clean.
- Original main: 3a160043f9ebb3a420b1d89a9cf4ddfe7d1b5d67. Never commit/push to main or rewrite history.
- Validated implementation/test/configuration HEAD: d90926c81a9590ad7f67a8a235586f1aa0524e18. Later evidence-only commits may advance this checkpoint with unchanged runtime/test inputs; use git rev-parse HEAD for its current revision.
- Official ub-mode SKILL.md/LICENSE unchanged, byte-verified against upstream 3da62bf3fcfd2034de41f30d920d2739e566171f.
- No Samsung access, private recording access, format migration or metronome implementation. Never enable protected fixture arguments/environment variables without current authorization.

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

1. Reproduce RecordingStore's malformed-index read/write risk with owned synthetic historical metadata. Separate tolerant read recovery from strict mutation; never overwrite corrupt evidence or change existing keys/serialization. Also characterize interrupted multi-store transactions in TrashStore/SessionToolsStore.
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
