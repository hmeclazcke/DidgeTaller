# Quality guide

## Methodology and ownership

The project-local SKILL.md is the original MIT-licensed Robert C. Martin ub-mode, pinned in UPSTREAM.md. Do not edit it. Verify the official repository and fetch SKILL.md/LICENSE from one exact revision when updating; compare UTF-8 Git blobs and SHA-256. AGENTS.md owns Android/data/Git safety and risk-based selection. The previous adaptation remains recoverable in Git at 989bfb2.

Crapper, Dryer and Mutator currently document other languages, not this Kotlin/Compose/AGP combination. They were reviewed, not installed. Kotlin equivalents here are Detekt/JaCoCo reviewed CRAP-L, PMD CPD and standalone free PIT. No paid Arcmutate dependency. The Android PIT plugin was not adopted: standalone CLI avoids unverified AGP/plugin coupling. Gradle resolves pinned Maven Central dependencies (CPD 7.19.0, BSD-style PMD license; PIT 1.20.0, Apache-2.0); no upstream scripts execute. The CLI's generic JVM operators were checked against source-level Kotlin branches, not assumed valid for all Kotlin constructs.

## Verified local components

Run from repository root with an installed JDK 21, Gradle wrapper 9.6.0, AGP 9.4.1, SDK Platform 35, Build Tools 36.0.0, NDK 27.0.12077973 and CMake 3.22.1. The daemon criteria accept any JDK 21 vendor and do not download a vendor runtime. Application Java/Kotlin bytecode still targets 17; Kotlin remains 2.2.21. Windows uses gradlew.bat; Linux uses sh ./gradlew. Python 3 uses only the standard library.

```powershell
.\gradlew.bat :app:test
.\gradlew.bat :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest
.\gradlew.bat '-Pkotlin.incremental=false' :app:jacocoDebugUnitTestReport detekt detektComplexity :engineering-tools:test :engineering-tools:sourceInventory
python tools/detekt_summary.py
python tools/coverage_summary.py
.\gradlew.bat :app:qualityDuplication
python tools/quality_gate.py
python -m unittest discover -s tools -p 'test_*.py'
.\gradlew.bat :app:qualityMutation
```

The rebuild additionally executes real expanded mutations with `:app:qualityMutationDomain` (currently informational score, required successful execution/nonempty report). It selects 17 concrete bytecode classes, including period search, trace decisions, music, geometry, playback and WAV I/O; it does not select generated data/state classes or protected Real* tests. Existing full JVM protected cases still skip without authorization. `:app:qualityDifferential '-PdifferentialBaseline=checkpoints/baseline-production.jar'` compares 741 synthetic observable outputs against the pinned original production JAR; `:app:qualityPerformanceComparison` with the same property records controlled host samples without a timing threshold. These commands were executed locally. CI recreates its oracle from baseline 3a16004 in a separate temporary detached worktree; no original checkout or user data is rewritten. The [case study](../engineering/REBUILD_CASE_STUDY.md) documents preparation, scope and current limitations.

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

Coverage ratchets permit only the explicit fractional tolerance in policy.properties. New/increased MCC at or above the target fails. Reviewed mappings disappearing fail. Increasing high CRAP-L or adding a high mapped function fails. New CPD hashes/counts fail. Unit checks exercise known CRAP examples and deliberately failing regressions. Existing debt is not silently cleared. CRAP-L = MCC squared times (1 - method line coverage) cubed plus MCC. Method-level line coverage is explicit; it is not basis-path coverage or an official Crapper score. Only source-hash/descriptor/line reviewed matches are scored. Overloads, lambdas, inline/suspend/Compose/name-mangled methods are unmapped unless explicitly reviewed. Current mapping is deliberately incomplete; missing measurements are visible.

## CI cadence

android.yml runs both JVM variants, lint, compilation, localization, JaCoCo, informational Detekt, CPD and enforced quality ratchets for relevant source/tool/config/workflow pushes and PRs. Documentation-only paths avoid these costs. Artifacts retain seven days and contain reports, not WAVs or user data.

The rebuild branch also receives the complete emulator workflow on relevant pushes. Fast CI runs the engineering-tools parser test/inventory and mandatory differential comparison. Manual mutation runs both enforced PlotTouch and informational expanded domain scope; artifact Kotlin-Mutations contains both report directories. No blanket continue-on-error or score claim hides survivors. Historical main evidence above retains its original scope. The branch must remain draft until the broader acceptance criteria are satisfied.

quality-deep.yml executes the complete emulator suite on relevant main pushes and manual dispatch. Its scope input selects all, instrumentation, or mutation to reuse unchanged evidence. Focused mutation runs only on manual dispatch; a skipped mutation job on a push does not mean mutations ran. There is no recurring schedule, paid tool, or continue-on-error. Main emulator failures fail the workflow; repository branch-protection settings are not changed. Actual CI outcomes are linked in QUALITY_BASELINE. Documentation-only pushes avoid both workflows; manual dispatch can verify an exact documentation revision.

Required workflow steps fail their jobs; main currently has no branch protection/rulesets, so they are not merge-blocking repository rules. Timeouts are 30 minutes (fast), 45 (emulator), and 25 (mutation). Concurrency cancels superseded automatic runs, while separate workflows and manual scopes remain independent. Only downloaded Maven dependencies and wrapper distributions are cached; project compilation/coverage outputs are never restored. Artifacts retain seven days. No automatic schedule or redundant device matrix is configured.

## Separate emulator coverage

AGP instruments only the optional coverage-enabled debug APK. Default developer/debug builds remain unchanged. Both reports use JaCoCo 0.8.15 and the same production-class filters. Use the nonincremental compiler flag shown below for a canonical measurement. Reused incremental Compose outputs produced extra uncovered branches locally; a nonincremental rebuild reproduced every CI source-file counter exactly (global JVM branch coverage 25.69%, rather than the earlier cached-output 23.39%; line/core measurements unchanged). This observation does not establish a general Kotlin compiler bug. CI starts from a fresh checkout with no reused project compilation outputs. Build, execute, then report without modifying production sources between those steps:

```powershell
.\gradlew.bat '-Pkotlin.incremental=false' -PandroidCoverage=true :app:assembleDebug :app:assembleDebugAndroidTest
python tools/run_instrumentation.py --adb "$env:LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe" --serial emulator-5554 --coverage
.\gradlew.bat '-Pkotlin.incremental=false' -PandroidCoverage=true :app:jacocoEmulatorReport
python tools/emulator_coverage_summary.py
```

The runner removes only its named emulator coverage probe file before execution, retrieves the new binary execution data, and requires complete per-test results plus a matching successful runner summary. Missing execution data and reports with no covered production lines fail. Reports are app/build/reports/jacoco/emulator/ (HTML/XML/summary.json); raw probe data is build/reports/quality/instrumentation.ec. These contain code coverage, not audio. They measure the executed synthetic scenarios, not all physical-device behavior or JNI/C++ coverage. No percentages or probe sets are merged across JVM/Android. This optional build uses AGP's [documented instrumentation coverage](https://developer.android.com/studio/test/coverage-report), not a second coverage framework.

Complete quality verification consists of the fast/build/static sequence, focused mutation, and this coverage-enabled full emulator sequence. Run expensive components only for explicitly requested deep validation or relevant high-risk changes. Keep the emulator serial explicit even when Samsung is attached.

## Prioritized debt

1. Expand reviewed function mappings beyond the eight exact descriptor/declaration/entry matches. Three acoustic functions exceed CRAP-L 10 despite complete method line coverage: characterize their branches before any separately scoped refactor. Do not change accepted acoustics merely to improve a metric.
2. Improve critical uncovered Android error/background paths using the separate instrumentation report. Android coverage is informational until a stable cross-environment baseline is established; the existing JVM ratchets remain enforced. Never substitute class coverage into CRAP.
3. Expand mutation scope incrementally to pure persistence/recording decisions after reviewing generated Kotlin operators; keep generic operators and paid enhancements distinct.
4. Reduce measured high complexity in UI presentation only with behavioral characterization. PlotSurface MCC 139 is a prioritization signal, not proof of an acoustic fault. Do not split stable logic solely to improve a number.

When a gate fails, inspect its saved report, reproduce one failure, fix the cause, and preserve the failed evidence. Do not reset baselines or add exclusions to manufacture green CI. Samsung installation is outside this task and all private recordings remain untouched.
