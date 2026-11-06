# Quality baseline - 2026-11-06

Starting revision 989bfb2, clean main and origin/main matched. Gradle 8.13, AGP 8.13.2, Kotlin 2.2.21, JDK target/runtime 17, Android compile/target 35, min 26; NDK 27.0.12077973 and CMake 3.22.1. JUnit 4.13.2, JaCoCo 0.8.15, Detekt 1.23.8.

One workflow android.yml runs debug JVM tests, debug/application-test builds, localization and informational JaCoCo/Detekt reports. It does not execute instrumentation, lint, duplication or mutation. Detekt ignoreFailures is true; no coverage or CRAP regression gate. Reviewed method correspondences exist with source hashes, JVM descriptors and exact lines; their scores are explicitly line/instruction approximations, not basis-path coverage.

Debug and release JVM tasks were discovered via :app:tasks --all. Full :app:test and instrumentation execution results are recorded below after execution. At baseline there are 56 test source files; source-file counts are not executed test counts. Optional private-fixture tests must skip without supplied recordings. Private audio/checkpoints/backups are ignored; no physical-device operation is part of this task. Available AVD: Medium_Phone_API_37.0.

Coverage, complexity, duplication and mutation values are unavailable until measured in this iteration. JVM coverage does not represent Android UI coverage. Full integration and CI results must not be inferred from compilation.

## Executed results and current limits

Both JVM variants: initially 190 discovered / 182 passed / 8 skipped / 0 failed each. After the mutation-driven diagonal boundary test: 191 discovered / 183 passed / 8 skipped / 0 failed each (382 case-variant executions total). Synthetic detector, flute, STFT, MusicalTrace, WAV and coordinate regressions passed. Eight opt-in protected recording cases per variant were skipped without providing private audio.

Complete Android run: API 37 Medium_Phone_API_37.0, ranchu, emulator-5554, 1080x2400/density420/font1.0, no private arguments. 125 discovered: 106 passed, 13 failed, 6 assumption skips (protected fixtures and environment-dependent cases). The full suite was executed, not merely compiled. This is NOT a green application baseline. Local evidence: ignored checkpoints/quality-full-instrumentation.log and quality-android-summary.json. Failing classes: DroneComponentUiTest (2), FundamentalPrecisionUiTest, GraphIntelligenceUiTest, PrecisionUiTest (2), SessionNavigationTest (3), StudioViewerTest, UIAuditTest (2), ViewerRotationTest. Repair requires focused reproduction; gesture/readout contract mismatches are present but not every failure is yet diagnosed. No classes were excluded or assertions weakened.

JaCoCo debug JVM: production line 28.15%, branch 23.30%; pure core line 97.90%, branch 76.92%. Audio Android adapters and data persistence JVM coverage are 0%; retained in the denominator. HTML/XML reports exist. Instrumentation coverage not configured or merged.

Detekt: 335 measured functions; PlotSurface MCC 139, cognitive 309. 1,977 informational findings. Two current reviewed method mappings, 333 functions not scored. LivePitch.next CRAP-L 8.0, AxisDomain.transform 4.0, zero mapped scores at/above 10. Six older mapping specifications rejected for changed source hashes; no automatic identity guessing. CRAP-L uses method line coverage, not basis-path coverage. Broad CRAP automation is incomplete.

PMD CPD 7.19.0: Kotlin production scan, zero exact clone groups at 100-token minimum, parser completed with real per-file token counts. Zero clones at this threshold does not prove absence of semantic duplication. No production refactor was justified by CPD.

PIT 1.20.0: PlotTouchKt.plotTouch only, DEFAULTS generic JVM operators, 11 generated, initially 10 killed / 1 survivor / 0 uncovered; the surviving diagonal boundary mutation was killed by a new behavioral regression. Final 11 killed / 0 survived / 0 uncovered, 100% selected-scope score, approximately four seconds for the focused task after compilation. No paid plugin; Kotlin-specific commercial enhancement warning remains visible. Do not generalize this scope to the app.

Lint debug: zero errors, 57 warnings, 10 hints. Debug/app-test builds and Detekt completed; localization parity 564 strings / 5 plurals / 2 arrays. Official skill blob, MIT notice and discovery metadata verified. Four ratchet failure-semantics tests passed. Local new fast quality components execute successfully. Full Android repair and successful complete CI execution remain unfinished; manual deep workflow is diagnostic, not a proven mandatory main-branch gate.
