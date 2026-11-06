# Quality guide

## Methodology and ownership

The project-local SKILL.md is the original MIT-licensed Robert C. Martin ub-mode, pinned in UPSTREAM.md. Do not edit it. Verify the official repository and fetch SKILL.md/LICENSE from one exact revision when updating; compare UTF-8 Git blobs and SHA-256. AGENTS.md owns Android/data/Git safety and risk-based selection. The previous adaptation remains recoverable in Git at 989bfb2.

Crapper, Dryer and Mutator currently document other languages, not this Kotlin/Compose/AGP combination. They were reviewed, not installed. Kotlin equivalents here are Detekt/JaCoCo reviewed CRAP-L, PMD CPD and standalone free PIT. No paid Arcmutate dependency. The Android PIT plugin was not adopted: standalone CLI avoids unverified AGP/plugin coupling. Gradle resolves pinned Maven Central dependencies (CPD 7.19.0, BSD-style PMD license; PIT 1.20.0, Apache-2.0); no upstream scripts execute. The CLI's generic JVM operators were checked against source-level Kotlin branches, not assumed valid for all Kotlin constructs.

## Verified local components

Run from repository root with JDK 17 and the existing Android SDK/NDK/CMake toolchain. Windows uses gradlew.bat; Linux uses sh ./gradlew. Python 3 uses only the standard library.

```powershell
.\gradlew.bat :app:test
.\gradlew.bat :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest
.\gradlew.bat :app:jacocoDebugUnitTestReport detekt detektComplexity
python tools/detekt_summary.py
python tools/coverage_summary.py
.\gradlew.bat :app:qualityDuplication
python tools/quality_gate.py
python -m unittest discover -s tools -p test_quality_gate.py
.\gradlew.bat :app:qualityMutation
```

Together these are the fast/build/static quality sequence; mutation is a separate deep step, not appropriate for README-only work. Both debug/release JVM suites are selected by :app:test. Synthetic acoustics are part of those suites. Private fixture environment variables/Android arguments must remain unset in public verification; their assumption skips are intentional, not replaced with fake recordings.

Full instrumentation uses only a verified emulator:

```powershell
python tools/run_instrumentation.py --adb "$env:LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe" --serial emulator-5554
```

On CI, adb is on PATH and --adb can be omitted. The wrapper installs only on emulator-* with ranchu/goldfish hardware, executes the entire AndroidJUnitRunner suite, saves output and fails on missing success or failures even if adb returns zero. The baseline suite was actually launched with the equivalent explicit adb install-r and am instrument commands; failures are recorded in QUALITY_BASELINE. Full verification means the component sequence above PLUS this entire instrumentation run, not merely an app-test APK build. A green complete verification is currently unavailable until the Android failures are repaired.

## Reports and enforcement

- app/build/reports/jacoco/debug/: HTML/XML and summary.json/summary.md, debug JVM coverage only. All production Kotlin including handwritten lambdas/Compose remains in the denominator. Generated R/BuildConfig/WhenMappings are filtered; JaCoCo Kotlin filters handle bytecode scaffolding. Android adapters/persistence remain zero. No instrumentation coverage is merged. Device coverage remains unconfigured.
- build/reports/detekt/: static findings and per-function MCC/cognitive observations. Findings remain informational; reliable unique file/function MCC regressions are checked separately.
- app/build/reports/quality/cpd.xml: real Kotlin lexer scan, exact token clones, not Dryer's structural similarity metric. Current threshold is in config/quality/policy.properties. Parser errors/empty scans fail; CPD findings feed the ratchet rather than being suppressed.
- build/reports/quality/metrics.json: package coverage, reliable reviewed method CRAP-L, unmatched specs, function complexity and CPD findings. Config/quality/baseline.json records measured floors/debt; policy.properties is the sole policy setting source. Use python tools/quality_gate.py --record only after reviewing intended baseline changes, never in CI.
- app/build/reports/quality/pit/: real XML/HTML mutations for PlotTouchKt.plotTouch and PlotTouchTest. The 11 generic bytecode mutations map to the source branches/arithmetic; one diagonal-boundary survivor exposed a missing test and is now killed. This scope excludes Compose, Android, generated enum methods and acoustic detectors. A selected-function score is not whole-app strength. PIT's warning about its optional commercial Kotlin plugin remains visible. The free operators work for this verified scope only. mutationThreshold is enforced by PIT; missing/stale mutation XML fails too.
- build/reports/quality/instrumentation.txt/json: complete nonprivate synthetic emulator output. Private WAVs and screenshots are not CI artifacts.

Coverage ratchets permit only the explicit fractional tolerance in policy.properties. New/increased MCC at or above the target fails. Reviewed mappings disappearing fail. Increasing high CRAP-L or adding a high mapped function fails. New CPD hashes/counts fail. Four unit checks exercise known CRAP examples and deliberately failing regressions. Existing debt is not silently cleared. CRAP-L = MCC squared times (1 - method line coverage) cubed plus MCC. Method-level line coverage is explicit; it is not basis-path coverage or an official Crapper score. Only source-hash/descriptor/line reviewed matches are scored. Overloads, lambdas, inline/suspend/Compose/name-mangled methods are unmapped unless explicitly reviewed. Current mapping is deliberately incomplete; missing measurements are visible.

## CI cadence and incomplete integration

android.yml runs both JVM variants, lint, compilation, localization, JaCoCo, informational Detekt, CPD and enforced quality ratchets for relevant source/tool/config/workflow pushes and PRs. Documentation-only paths avoid these costs. Artifacts retain seven days and contain reports, not WAVs or user data.

quality-deep.yml is manual: independent focused mutation and complete emulator execution jobs. No recurring paid schedule. The full suite must be repaired and the actual workflow proven before claiming a mandatory main-branch complete-test gate. Manual diagnostic jobs fail honestly on existing test failures; there is no continue-on-error. CI verification results and remaining gaps belong in QUALITY_BASELINE, not a green badge implying unexecuted tests.

## Prioritized debt

1. Repair complete Android-suite failures, distinguishing stale gesture/readout assertions from production regressions. Do not weaken assertions or exclude failing classes. Re-run only affected scenarios before another complete run.
2. Review the six obsolete method correspondence specs and cover critical Android recording/persistence lifecycle paths with instrumentation coverage; never substitute class coverage into CRAP.
3. Expand mutation scope incrementally to pure persistence/recording decisions after reviewing generated Kotlin operators; keep generic operators and paid enhancements distinct.
4. Reduce measured high complexity in UI presentation only with behavioral characterization. PlotSurface MCC 139 is a prioritization signal, not proof of an acoustic fault. Do not split stable logic solely to improve a number.

When a gate fails, inspect its saved report, reproduce one failure, fix the cause, and preserve the failed evidence. Do not reset baselines or add exclusions to manufacture green CI. Samsung installation is outside this task and all private recordings remain untouched.
