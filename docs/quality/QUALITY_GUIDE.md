# Quality guide

## Current comparison and annotation continuation

The [comparison cycle](../engineering/SWIPE_OPERATION_OWNERSHIP.md) introduces
`:app:qualityMutationSwipe`, verified locally against typed comparison ownership,
shared progress scheduling and ordinary Kotlin camera reconciliation. The existing
`:app:qualityMutationAnalysis` now also selects the shared scheduler. Their scopes
overlap; do not add their mutant totals. Both join the existing domain task without
running a second dispatcher or changing SWIPE equations. New named eligible code
is measured below target; technical constructor/Compose reviews remain explicit.
The [annotation cycle](../engineering/PLOT_NOTICE_OWNERSHIP.md) has real compact/
150% bounds and passive-input regressions. Broad final CI is pending publication.

## Analysis and rendering milestone

The [current source-scoped milestone](../engineering/ANALYSIS_RENDERING_REBUILD.md) documents typed recording-analysis
ownership, the ready-frame plot pipeline, actual native RED/GREEN, full scientific
text at 150%, comparable complexity and focused mutation evidence. Whole rebuild
remains IN PROGRESS; [actual current source verification](../engineering/ANALYSIS_RENDERING_VERIFICATION_8b73c21.md)
records all three passing remote jobs, metrics, screenshots and precise residual work.

Focused verified tasks: `:app:qualityMutationAnalysis` and `:app:qualityMutationPlots`.
Both scopes also join `:app:qualityMutationDomain`; framework Compose correspondence
remains explicitly unsupported, never assigned fabricated source CRAP-L.


Source ec19b18 now has observed successful fast, actual API 35 emulator and
expanded mutation CI. The [source-scoped verification](../engineering/WORKSPACE_VERIFICATION_ec19b18.md)
records numerical results, report scopes, visual limits and exact next work.
The whole rebuild remains IN PROGRESS; PR #1 stays draft.

## Methodology and ownership

The [simulation owner](../engineering/SIMULATION_OWNERSHIP.md) removes the lab's
dedicated executor and rejects results after disposal through a typed operation
controller. Focused real mutations: `:app:qualityMutationSimulation`; its scope
also joins the expanded domain run. The new default evaluator and coroutine
adapter have deterministic JVM tests plus real Android ViewModel factory tests.

Canonical quality compilation explicitly requires `-Pkotlin.incremental=false`.
The bytecode inventory rejects other modes; normal development builds may remain
incremental. This prevents mode-dependent generated Compose branch denominators
from being reported as a source coverage regression. `bytecodeInventory` reads
exact Kotlin metadata names/descriptors, including Result/value-class mangling,
without loading app classes. Class fingerprints, PSI ownership/locations and
ambiguity rejection remain enforced. Missing/empty/stale metadata fails the gate.
The native workflow produces its own `android-function-mapping.json`; those
CRAP-L scores use Android-only method-line coverage and never merge with JVM.

The [workspace milestone](../engineering/WORKSPACE_REBUILD.md) adds deterministic
operation/scheduler/resource tests and typed time-only versus spectral requests.
Focused commands: `:app:qualityMutationWorkspace` and `:app:qualityMutationPlots`.
The same scopes join `qualityMutationDomain`, including coroutine implementation
classes explicitly. Scores remain scope-specific/informational. Every mutation
task now rejects PIT RUN_ERROR/MEMORY_ERROR/unfinished outcomes using
`tools/mutation_report.py`; PIT exit zero alone is insufficient. Timeouts and
nonviable operators remain distinct reported outcomes, never called killed tests.
The emulator script also executes real EN/ES compact 150% session configurations
and a landscape session case, retaining `session-screenshots/` and
`workspace-configuration-*.txt` separately from full-suite coverage. Inspect the
actual collected PNGs and run results before making a visual/Android pass claim.

The [2026-11-08 forensic audit](FORENSIC_COMPLIANCE_AUDIT.md) reconciles every changed
callable and all unscored declarations. Installation/invocation, architectural
practice and executable enforcement are distinct claims. Historical runtime
invocation cannot independently be proved from repository configuration.

`tools/quality_gate.py` now also validates complete schema-2 PSI source scope,
same-source hashes, paired normalized/raw method metrics and changed authored
callables. Both `main/java` and `main/kotlin` are scanned. Eligible missing or
ambiguous scores fail; nonpositive source MCC cannot become a score. New/modified
measurable functions must stay below CRAP-L target or have an exact content-bound
risk review. Unsupported transformations/auxiliary declarations need narrow
technical reviews; abstract contracts and nested bodies are accounted without
inventing standalone execution metrics. See `config/quality/source-baseline.json`
(historical `9cd18f6` fingerprints, separate from numerical baseline) and
`source-reviews.json` (explicit reasons, evidence and exact score ceilings).
Reviews expire after body/signature edits and are never renewed automatically.
`build/reports/quality/changed-callables.json` exposes accepted/rejected changes.
Do not update a baseline or exception merely to pass a failure. These checks do
not prove every assertion, architectural decision or hardware failure path.

The project-local SKILL.md is the original MIT-licensed Robert C. Martin ub-mode, pinned in UPSTREAM.md. Do not edit it. Verify the official repository and fetch SKILL.md/LICENSE from one exact revision when updating; compare UTF-8 Git blobs and SHA-256. AGENTS.md owns Android/data/Git safety and risk-based selection. The previous adaptation remains recoverable in Git at 989bfb2.

Crapper, Dryer and Mutator currently document other languages, not this Kotlin/Compose/AGP combination. They were reviewed, not installed. Kotlin equivalents here are Detekt/JaCoCo reviewed CRAP-L, PMD CPD and standalone free PIT. No paid Arcmutate dependency. The Android PIT plugin was not adopted: standalone CLI avoids unverified AGP/plugin coupling. Gradle resolves pinned Maven Central dependencies (CPD 7.19.0, BSD-style PMD license; PIT 1.20.0, Apache-2.0); no upstream scripts execute. The CLI's generic JVM operators were checked against source-level Kotlin branches, not assumed valid for all Kotlin constructs.

## Verified local components

Run from repository root with an installed JDK 21, Gradle wrapper 9.6.0, AGP 9.4.1, SDK Platform 35, Build Tools 36.0.0, NDK 27.0.12077973 and CMake 3.22.1. The daemon criteria accept any JDK 21 vendor and do not download a vendor runtime. Application Java/Kotlin bytecode still targets 17; Kotlin remains 2.2.21. Windows uses gradlew.bat; Linux uses sh ./gradlew. Python 3 uses only the standard library.

```powershell
.\gradlew.bat '-Pkotlin.incremental=false' :app:test
.\gradlew.bat '-Pkotlin.incremental=false' :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest
.\gradlew.bat '-Pkotlin.incremental=false' :app:jacocoDebugUnitTestReport detekt detektComplexity :engineering-tools:test :engineering-tools:sourceInventory :engineering-tools:bytecodeInventory
python tools/detekt_summary.py
python tools/coverage_summary.py
.\gradlew.bat :app:qualityDuplication
python tools/quality_gate.py
python -m unittest discover -s tools -p 'test_*.py'
.\gradlew.bat :app:qualityMutation
```

The rebuild additionally executes real expanded mutations with `:app:qualityMutationDomain` (currently informational score, required successful execution/nonempty report). Its explicit `domainTargets`/persistence selection in app/build.gradle.kts includes period search, trace decisions, music, geometry, playback, worker-owned recording state, atomic derived-cache publication, WAV I/O, transactional metadata, JSON validation and library projection; it does not select generated data/state classes or protected Real* tests. `:app:qualityMutationCapture` and `:app:qualityMutationCache` are focused development selections with reports in app/build/reports/quality/pit-capture and pit-cache; CI's domain run already includes them without repeating those selections separately. Existing full JVM protected cases still skip without authorization. `:app:qualityDifferential '-PdifferentialBaseline=checkpoints/baseline-production.jar'` compares 741 synthetic observable outputs against the pinned original production JAR; `:app:qualityPerformanceComparison` with the same property records controlled host samples without a timing threshold. These commands were executed locally. CI recreates its oracle from baseline 3a16004 in a separate temporary detached worktree; no original checkout or user data is rewritten. The [case study](../engineering/REBUILD_CASE_STUDY.md) documents preparation, scope and current limitations.

Together these are the fast/build/static quality sequence; mutation is a separate deep step, not appropriate for README-only work. Both debug/release JVM suites are selected by :app:test. Synthetic acoustics are part of those suites. Public verification must use a clean emulator containing only synthetic data. Protected Android fixture entry points require both current user authorization and the explicit allowPrivateFixtures=true argument; existing files alone never opt in. Public CI never supplies that flag. Private JVM fixture variables must remain unset. Their assumption skips are intentional, not replaced with fake recordings. Preserve older emulator/private fixtures without opening or deleting them.

AGP 9 defaults to creating only debug unit tests. The explicit `android.onlyEnableUnitTestForTheTestedBuildType=false` preserves both suites, and CI names both tasks so disappearance cannot silently pass. Legacy Kotlin/DSL opt-outs preserve the existing compiler integration. This empirically validated combination is outside Kotlin 2.2.21's published fully tested Gradle/AGP range; it is not a claim of complete vendor support. Remove the temporary opt-outs through a separately validated migration before AGP 10. Gradle 10 deprecation warnings are known debt.

Execute each native command separately in Windows PowerShell and check `$LASTEXITCODE`; a later successful command must not hide an earlier failure. GitHub's required Bash steps use fail-fast exit propagation. The full command sequence includes `python tools/check_localization.py` before publishing. See the [verification audit](QUALITY_AUDIT_VERIFICATION.md) for actual measurements and deliberately failing gate probes.

Full instrumentation uses only a verified emulator:

```powershell
python tools/run_instrumentation.py --adb "$env:LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe" --serial emulator-5554
```

On CI, adb is on PATH and --adb can be omitted. The wrapper installs only on emulator-* with ranchu/goldfish hardware, executes the entire AndroidJUnitRunner suite, saves output and fails on missing success or failures even if adb returns zero. The baseline suite was actually launched with the equivalent explicit adb install-r and am instrument commands; failures are recorded in QUALITY_BASELINE. Full verification means the component sequence above PLUS this entire instrumentation run, not merely an app-test APK build. Use the latest executed results in QUALITY_BASELINE; an initial failing run remains recorded there.

## Reports and enforcement

The isolated `refactor/uncle-bob-rebuild` effort is **IN PROGRESS**; see [baseline](../engineering/REBUILD_BASELINE.md) and [checkpoint](../engineering/REBUILD_PROGRESS.md). Compiler PSI inventories every named declaration, lambda and explicit accessor. The source/owner/parameter-count/body-location/descriptor mapper replaces the eight-function allowlist for actual gate inputs; legacy manual pins remain provenance checks. Baseline expansion records previously invisible original debt, not an improvement. Named functions without their own Detekt metric (locals), Compose transformations, declarations without bodies and anonymous owners remain explicitly classified. JVM and Android scores must be generated separately with the corresponding JaCoCo XML. Inline/suspend/ambiguous candidates are rejected rather than guessed. Reports and the expanded ratchet do not certify architecture or prove branch-path coverage.

- app/build/reports/jacoco/debug/: HTML/XML and summary.json/summary.md, debug JVM coverage only. All production Kotlin including handwritten lambdas/Compose remains in the denominator. Generated R/BuildConfig/WhenMappings are filtered; JaCoCo Kotlin filters handle bytecode scaffolding. Android adapters/persistence remain zero. Instrumentation coverage is reported separately, never merged into this JVM percentage.
- build/reports/detekt/: static findings and per-function MCC/cognitive observations. Findings remain informational; reliable unique file/function MCC regressions are checked separately.
- app/build/reports/quality/cpd.xml: real Kotlin lexer scan, exact token clones, not Dryer's structural similarity metric. Current threshold is in config/quality/policy.properties. Parser errors/empty scans fail; CPD findings feed the ratchet rather than being suppressed.
- build/reports/quality/metrics.json: package coverage, reliable reviewed method CRAP-L, unmatched specs, function complexity and CPD findings. Config/quality/baseline.json records measured floors/debt; policy.properties is the sole policy setting source. Use python tools/quality_gate.py --record only after reviewing intended baseline changes, never in CI.
- app/build/reports/quality/pit/: real XML/HTML mutations for PlotTouchKt.plotTouch and PlotTouchTest. The 11 generic bytecode mutations map to the source branches/arithmetic; one diagonal-boundary survivor exposed a missing test and is now killed. This scope excludes Compose, Android, generated enum methods and acoustic detectors. A selected-function score is not whole-app strength. PIT's warning about its optional commercial Kotlin plugin remains visible. The free operators work for this verified scope only. mutationThreshold is enforced by PIT; missing/stale mutation XML fails too.
- build/reports/quality/instrumentation.txt/json: complete nonprivate synthetic emulator output. Private WAVs and screenshots are not CI artifacts.

Coverage ratchets permit only the explicit fractional tolerance in policy.properties. New/increased MCC at or above the target fails. Reviewed mappings disappearing fail. Increasing high CRAP-L or adding a high mapped function fails. An explicit one-to-one predecessor/replacement in retired-methods.json retains the original risk ceiling across a signature change: the predecessor must disappear, the replacement must be measured, and its score cannot increase. Missing, ambiguous or unreviewed lineage never grants an allowance. This retains debt rather than resetting it. New CPD hashes/counts fail. Python tests exercise deliberately failing regressions and signature evolution. CRAP-L = MCC squared times (1 - method line coverage) cubed plus MCC. Method-level line coverage is explicit; it is not basis-path coverage or an official Crapper score. Compiler-PSI source hashes, owners, parameter counts, body bounds and unique bytecode descriptors establish correspondence; ambiguous and transformed declarations stay visibly unscored.

## CI cadence

android.yml runs both JVM variants, lint, compilation, localization, JaCoCo, informational Detekt, CPD and enforced quality ratchets for relevant source/tool/config/workflow pushes and PRs. Documentation-only paths avoid these costs. Artifacts retain seven days and contain reports, not WAVs or user data.

The rebuild branch also receives the complete emulator workflow on relevant pushes. Fast CI runs the engineering-tools parser test/inventory and mandatory differential comparison. Manual mutation runs both enforced PlotTouch and informational expanded domain scope; artifact Kotlin-Mutations contains both report directories. No blanket continue-on-error or score claim hides survivors. Historical main evidence above retains its original scope. The branch must remain draft until the broader acceptance criteria are satisfied.

quality-deep.yml executes the complete emulator suite on relevant main pushes and manual dispatch. Its scope input selects all, instrumentation, or mutation to reuse unchanged evidence. Focused mutation runs only on manual dispatch; a skipped mutation job on a push does not mean mutations ran. There is no recurring schedule, paid tool, or continue-on-error. Main emulator failures fail the workflow; repository branch-protection settings are not changed. Actual CI outcomes are linked in QUALITY_BASELINE. Documentation-only pushes avoid both workflows; manual dispatch can verify an exact documentation revision.

Required workflow steps fail their jobs; main currently has no branch protection/rulesets, so they are not merge-blocking repository rules. Timeouts are 30 minutes (fast), 45 (emulator), and 25 (mutation). Fast concurrency groups automatic push/PR runs by source repository and head branch, avoiding duplicate work for the same update; fork branches and explicit manual evidence runs remain separate. Concurrency cancels superseded automatic runs, while separate workflows and manual scopes remain independent. Only downloaded Maven dependencies and wrapper distributions are cached; project compilation/coverage outputs are never restored. Artifacts retain seven days. No automatic schedule or redundant device matrix is configured.

## Separate emulator coverage

AGP instruments only the optional coverage-enabled debug APK. Default developer/debug builds remain unchanged. Both reports use JaCoCo 0.8.15 and the same production-class filters. Use the nonincremental compiler flag shown below for a canonical measurement. Reused incremental Compose outputs produced extra uncovered branches locally; a nonincremental rebuild reproduced every CI source-file counter exactly (global JVM branch coverage 25.69%, rather than the earlier cached-output 23.39%; line/core measurements unchanged). This observation does not establish a general Kotlin compiler bug. CI starts from a fresh checkout with no reused project compilation outputs. Build, execute, then report without modifying production sources between those steps:

```powershell
.\gradlew.bat '-Pkotlin.incremental=false' -PandroidCoverage=true :app:assembleDebug :app:assembleDebugAndroidTest
python tools/run_instrumentation.py --adb "$env:LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe" --serial emulator-5554 --coverage
.\gradlew.bat '-Pkotlin.incremental=false' -PandroidCoverage=true :app:jacocoEmulatorReport
python tools/emulator_coverage_summary.py
```

The runner removes only its named emulator coverage probe file before execution, retrieves the new binary execution data, and requires complete per-test results plus a matching successful runner summary. Missing execution data and reports with no covered production lines fail. Reports are app/build/reports/jacoco/emulator/ (HTML/XML/summary.json); raw probe data is build/reports/quality/instrumentation.ec. These contain code coverage, not audio. They measure the executed synthetic scenarios, not all physical-device behavior or JNI/C++ coverage. No percentages or probe sets are merged across JVM/Android. This optional build uses AGP's [documented instrumentation coverage](https://developer.android.com/studio/test/coverage-report), not a second coverage framework.

The existing function mapper also accepts that separate Android XML after generating a fresh compiler inventory and Detekt summary for the **same production revision**:

```powershell
python tools/function_mapping.py --coverage app/build/reports/jacoco/emulator/coverage.xml --output build/reports/quality/android-function-mapping.json
```

This invocation was exercised with the exact downloaded d90926c CI XML (stored locally under checkpoints/ci-android-coverage.xml), matching the unchanged local production source hashes. It mapped the same 330 functions, with 71 visibly unscored and 32 above CRAP-L target, separately from JVM's 83. It is informational local analysis, not an added automatic CI gate or combined coverage. Never feed an older report after source edits or substitute class coverage.

For hand-created AVDs, verify hardware main keys are disabled before claiming gesture navigation works: a gestural resource mode alone does not prove System UI attached its edge handler. The rebuild's fresh API 37 AVD required hw.mainKeys=no and recovery from a cold-boot System UI ANR under host memory pressure. The unchanged native edge-Back test and native MainActivity probe then passed. Prefer an existing valid phone profile; do not change app navigation, suppress a test, or use KEYCODE_BACK to hide an invalid emulator configuration. All emulator actions must keep the explicit serial and protected-fixture rules above.

Complete quality verification consists of the fast/build/static sequence, focused mutation, and this coverage-enabled full emulator sequence. Run expensive components only for explicitly requested deep validation or relevant high-risk changes. Keep the emulator serial explicit even when Samsung is attached.

## Persistence and library validation

The [persistence reconstruction](../engineering/PERSISTENCE_REBUILD.md) adds a shared document owner, validated last-good reads, explicit recovery/quarantine and a single library-state snapshot. Existing formats remain unchanged. This protects individual JSON documents; it is not a disaster-recovery guarantee for an entire lost preferences XML or storage device.

`./gradlew :app:qualityMutationPersistence` runs the seven selected persistence/JSON/library bytecode classes and their deterministic JVM tests. Its report is `app/build/reports/quality/pit-persistence/`; its score is informational, execution/nonempty-report checks are mandatory. The same classes are included in `qualityMutationDomain` and the existing manual CI mutation scope, without another competing framework. The test-only org.json 20250517 dependency supplies a real JVM codec; it is not packaged in the application and does not substitute for Android's implementation. `IndexIntegrityTest`, `CapturePersistenceTest`, `LocalPersistenceTest` and `LibraryRecoveryUiTest` exercise real Android preferences, JSON, files and Compose on synthetic isolated namespaces.

On this Windows host, simultaneous Gradle/PIT and the API 37 Google Play emulator exhausted free memory and left a System UI ANR overlay. UI semantics alone did not detect the obstruction. Library screenshot capture now requires the active accessibility window to belong to the test activity. Serialize heavy builds/mutations and emulator verification, stop idle Gradle daemons, and inspect actual screenshots. Never hide a system failure by changing app navigation or suppressing an assertion.

The emulator quality script also runs the recovery scenario in actual Android en/es application locales, 150% system font and a compact 720x1280 viewport. It records those two targeted runs separately (`library-configuration-en.txt`, `library-configuration-es.txt`) from the full suite and coverage. Fail-fast exit handling is preserved through evidence collection and settings restoration. Caller-locale Compose fixtures are not advertised as configuring a platform dialog window's font scale. Inspect the real PNGs; tags and passing assertions alone missed an English dialog inside the original Spanish fixture.

When analyzing downloaded coverage, verify the artifact run's head SHA against the source revision. The mapper rejects a stale PSI inventory; JaCoCo XML itself does not embed a source-tree Git SHA, so successful parsing alone does not establish report provenance. Never substitute an older external XML after a production edit.

## Prioritized debt

1. Complete remaining function measurement limitations after the compiler-PSI expansion. The original eight-function allowlist is superseded; mapped and explicitly unscored declarations are recorded in function-mapping.json. The original 43/33/15 acoustic hotspots were restructured under differential protection. Broad high-risk UI/Android functions remain unfinished; JVM zeros must not be confused with separate emulator coverage.
2. Improve critical uncovered Android error/background paths using the separate instrumentation report. Android coverage is informational until a stable cross-environment baseline is established; the existing JVM ratchets remain enforced. Never substitute class coverage into CRAP.
3. Review the remaining selected-domain survivors, including the 26 new persistence/JSON/library survivors. The seven-class persistence expansion is implemented and executed; do not repeat its integration. Keep generic Kotlin bytecode operators and paid enhancements distinct.
4. Reduce measured high complexity in UI presentation only with behavioral characterization. PlotSurface MCC 139 is a prioritization signal, not proof of an acoustic fault. Do not split stable logic solely to improve a number.

When a gate fails, inspect its saved report, reproduce one failure, fix the cause, and preserve the failed evidence. Do not reset baselines or add exclusions to manufacture green CI. Samsung installation is outside this task and all private recordings remain untouched.

## Capture ownership and raster selections

`./gradlew :app:qualityMutationHandoff` executes real PIT for `CaptureHandoff` and `RecordingCoordinator`; `./gradlew :app:qualityMutationRaster` executes `SpectrogramRaster`. Reports are under `app/build/reports/quality/pit-handoff/` and `pit-raster/`. These are focused development selections; the same production classes/tests participate in the existing manual `qualityMutationDomain` CI run without duplicate jobs. Scores are informational; execution and a nonempty mutation report remain required. Generated model/accessor methods are not claimed as mutation-tested domain logic.

The [capture design](../engineering/CAPTURE_EVENT_OWNERSHIP.md) describes the pure main-thread workflow, immutable per-recording publication and disposal boundaries. JVM tests exercise controlled terminal/acknowledgement ordering. Emulator handoff tests additionally exercise actual WAV finalization, Handler delivery, ViewModel disposal and original file/index preservation. Neither proof substitutes for physical microphone accuracy or Android power-loss durability.


## Playback intent selection

`./gradlew :app:qualityMutationPlayback` executes the controller and its event adapter with the deterministic PlaybackControllerTest contracts. It generates `app/build/reports/quality/pit-playback/` and was executed locally. The same two bytecode classes/tests are already selected in `qualityMutationDomain`, including manual CI. No separate duplicate CI job is added. The score is informational; execution/nonempty reports are mandatory. [Playback intent](../engineering/PLAYBACK_INTENT_OWNERSHIP.md) explains preparation, pause, seek acknowledgement, loop completion and remaining mutation limitations. Validate actual MediaPlayer behavior using the complete clean-emulator workflow; mocked effects alone are insufficient.


Accepted source-scoped results and before/after limits are recorded in [REBUILD_VERIFICATION_7aa7fcb.md](../engineering/REBUILD_VERIFICATION_7aa7fcb.md). Documentation-only descendants reuse this evidence while production, tests, build and quality inputs are unchanged. Do not combine JVM and instrumentation coverage XMLs or substitute the informational broad mutation score for the enforced original gesture threshold.
