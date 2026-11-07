# Quality audit verification — 2026-11-07

## Revision and provenance

The audit started with cleanly synchronized main/origin/main at `6841a38d9b174b6dc22c9c451b8258e135c28c4f`, plus an uncommitted Android Studio toolchain migration. The user explicitly authorized including and validating that migration. Commit `ec3c134` preserves its original, unvalidated state; it is not itself validation evidence. No production source, acoustic acceptance rule, persisted format, or resource translation changed in this audit.

GitHub's contents API confirmed that both public READMEs matched their local Git blobs. Their last content commit was `0e807dc`, while main was `6841a38`: subsequent code/evidence commits had not modified those files. This was not a lost push or stale README cache. The new README edits describe the updated toolchain.

The original official ub-mode remains byte-identical to `3da62bf3fcfd2034de41f30d920d2739e566171f`, including its MIT license. Skill SHA-256: `4120873db2e3681ea9b7de4d7105fc9d3406657c9ada91ce77b9b3c4962f3202`. Discovery path/frontmatter remain `.agents/skills/ub-mode/SKILL.md`, `name: ub-mode`, UTF-8. No nested agent session was needed.

## Toolchain correction and local execution

Validated locally: Gradle 9.6.0, AGP 9.4.1, installed JDK 21; Kotlin 2.2.21 and application bytecode target 17 retained. The wrapper distribution checksum matches Gradle's official checksum. CI now provides Temurin 21 and Build Tools 36.0.0. Vendor-specific daemon download URLs/Foojay resolution were removed; installed JDK 21 criteria accept either local JetBrains or CI Temurin.

AGP 9's new debug-only unit-test default initially omitted release tests. The explicit compatibility property restores both variants; CI explicitly requests both task names. This prevents counting stale release XML as a new execution. Both suites were executed after the correction. Legacy Kotlin/DSL opt-outs preserve the existing integration; this combination is empirically tested, outside Kotlin 2.2.21's published fully tested range. AGP/Gradle 10 compatibility is deferred, not claimed.

| Current local measurement | Discovered | Executed, excluding skips | Passed | Failed | Skipped |
| --- | ---: | ---: | ---: | ---: | ---: |
| Debug JVM | 195 | 187 | 187 | 0 | 8 |
| Release JVM | 195 | 187 | 187 | 0 | 8 |
| Total case-variant executions | 390 | 374 | 374 | 0 | 16 |
| Python quality-tool tests | 8 | 8 | 8 | 0 | 0 |

The 195 cases are the same source tests executed in two variants, not 390 distinct behaviors. The eight per-variant skips require protected audio; all private fixture environment variables were unset. Existing deterministic YIN/SWIPE/STFT/MusicalTrace/flute/WAV and state/coordinate regressions passed within these suites. Native SWIPE's device execution is covered by the emulator suite, not inferred from a JVM pass.

Local integrated build/static/mutation sequence completed in 2m19s; the restored release suite then completed in 25s. Debug and Android-test APK compilation passed. Detekt 1.23.8 reported 1,978 informational findings, 337 functions; maximum MCC 139 in PlotSurface, cognitive complexity 309. Android Lint: 0 errors, 60 warnings, 10 hints. Localization parity: 564 strings, 5 plurals, 2 arrays. No resource changes were necessary.

## Coverage, complexity and CRAP-L

JaCoCo 0.8.15 debug JVM production coverage is **28.19% lines / 25.69% branches** (1,307/4,637 lines; 1,455/5,663 branches). Pure core is **98.47% / 77.25%**. Android audio adapters and persistence remain 0% in this JVM report; they are retained in its denominator and measured separately on Android. Nonincremental Kotlin compilation reproduced the fresh-CI counters. No probe sets or incompatible reports are merged. Java/Kotlin/Compose generated scaffolding follows JaCoCo filters; handwritten production UI/lambdas are retained. C++/JNI coverage is not measured.

Eight exact source-hash, JVM-descriptor and line-reviewed functions are mapped; **329 of 337 remain unmapped**. CRAP-L uses method-specific line coverage, not class coverage, instruction coverage or official Crapper basis-path coverage:

`CRAP-L = CC² × (1 − method_line_coverage)³ + CC`

Only eight reliable scores exist; a highest-ten table would invent two mappings. Sorted complete inventory:

| File / function | CC | Method line coverage | CRAP-L | Risk |
| --- | ---: | ---: | ---: | --- |
| core/PitchDetector.kt: analyzeWithDiagnostics | 43 | 100% | 43.000 | Historical above-target |
| core/PitchTrace.kt: build | 33 | 100% | 33.000 | Historical above-target |
| core/PitchTrace.kt: isolated | 15 | 100% | 15.000 | Historical above-target |
| core/PitchTrace.kt: tone | 9 | 100% | 9.000 | Within target |
| core/LivePitch.kt: next | 8 | 100% | 8.000 | Within target |
| core/PitchStream.kt: append | 6 | 92.3077% | 6.016 | Within target |
| core/PitchDetector.kt: spectrum | 4 | 100% | 4.000 | Within target |
| core/PlotViewport.kt: AxisDomain.transform | 4 | 100% | 4.000 | Within target |

The target remains below 10; three historical violations are explicit debt. Overloads, suspend/inline functions, lambdas, extensions, mangled/Compose/synthetic methods are not automatically guessed. New/increased high mapped risk, disappearing reviewed mappings, and reliable unique-function complexity increases fail the ratchet. The original coverage floors and tolerance remain unchanged; this audit did not reset debt to manufacture success.

## Duplication and mutation

PMD CPD 7.19.0's Kotlin lexer scanned **59 production files**, completed without parser errors, and found **0 clone groups at 100 tokens**. Baseline 0; new violations 0. No significant duplicate location or detector-driven refactor exists at this threshold. This does not establish absence of semantic duplication and is not Dryer's 0.8 similarity metric. CPD creates the report; the subsequent quality gate rejects new groups or an empty/missing report.

Free PIT 1.20.0 actually mutated `ar.didgetaller.core.PlotTouchKt` against six `PlotTouchTest` cases. DEFAULTS generic JVM operators, one worker, 4,000 ms timeout constant: **11 generated, 11 killed, 0 survived, 0 uncovered, 0 other outcomes; 11/11 = 100% selected-scope score**. PIT reported 2s total, 1s mutation phase, excluding compilation/downloads. Its optional commercial Kotlin plugin is not installed. These reviewed arithmetic/branch mutants validate this scope only, not all Kotlin-generated bytecode or whole-app test strength. Existing diagonal-boundary coverage already kills the earlier survivor; no new production correction was justified.

## Real remote evidence available at audit start

These results were downloaded through the GitHub API and inspected from actual job metadata/artifacts. They predate the newly authorized toolchain update; final publication must inspect the new commit's runs rather than relabeling them.

| Workflow / run | Commit | Trigger | Actual result and evidence |
| --- | --- | --- | --- |
| [Fast 42301032905](https://github.com/hmeclazcke/DidgeTaller/actions/runs/42301032905) | 9e7b316 | push | Success; both JVM variants, lint, build, localization, coverage, Detekt, CPD, ratchets, Python tests |
| [Full emulator 42301033022](https://github.com/hmeclazcke/DidgeTaller/actions/runs/42301033022) | 9e7b316 | push | Success; 127 discovered, 117 passed, 0 failed, 10 skipped; coverage enabled; 450.824s runner duration |
| [Focused PIT 42297582476](https://github.com/hmeclazcke/DidgeTaller/actions/runs/42297582476) | 0e807dc | manual mutation | Success; 11 actual mutants killed; emulator job intentionally skipped |
| [Failed emulator 42297556817](https://github.com/hmeclazcke/DidgeTaller/actions/runs/42297556817) | 0e807dc | push | Failure; 125 discovered, 114 passed, 1 failed, 10 skipped; later corrected canonical-analysis readiness |

The successful emulator was API 35, x86_64, Pixel 2, fresh runner userdata. Logs show emulator boot, explicit application/test APK installs, `am instrument`, per-test terminal outcomes, successful runner summary, probe collection, and separate JaCoCo report. APK compilation alone is never counted. Seven skips are protected fixture guards; three require other navigation/compact/font configurations and have separate earlier clean API 37 evidence, not a claim that this default CI configuration executes them.

| Separate Android coverage from the successful full CI run | Lines | Branches |
| --- | ---: | ---: |
| Overall production | 91.14% | 67.17% |
| Pure core | 95.25% | 69.19% |
| Audio adapters | 95.22% | 68.28% |
| Persistence | 94.37% | 58.60% |
| UI | 90.21% | 68.19% |

Android percentages are informational; JVM coverage ratchets are enforced. The old local five-test Android report is deliberately not substituted for this full CI evidence. New-toolchain Android execution is performed in GitHub's clean emulator, not on the historical local emulator or Samsung.

## Workflow inventory and reconciliation

Exactly two workflow files exist. No recurring schedule, paid runner or test matrix is configured.

| Workflow/job | Events and filters | Failure semantics and cost controls | Artifacts (7 days) |
| --- | --- | --- | --- |
| android.yml — Android Kotlin CI / test-and-apk | PRs and pushes, any branch, relevant app/tools/config/Gradle/scripts/workflow paths; manual dispatch | Required steps, 30m job timeout, automatic superseded-run cancellation; download-only dependency/wrapper cache | Quality-Metrics-and-Test-Results; DidgeTaller-JVM-Coverage; DidgeTaller-Detekt |
| quality-deep.yml — Full emulator and manual mutation quality / instrumentation | Relevant main pushes; manual all/instrumentation | Required when selected, 45m job timeout, runner command 1,800s timeout; fresh API 35 emulator; no restored build outputs | Complete-Emulator-Results, including runner statuses and separate HTML/XML/JSON/probes |
| Same workflow / mutation | Manual all/mutation only; intentionally skipped on pushes | Required when selected, 25m timeout; independent manual-scope concurrency; PIT threshold 100% | Focused-Kotlin-Mutations XML/HTML |

Documentation-only paths skip automatic workloads; changes to source, Gradle, tools, policy, or workflow files do not. No mandatory step uses `continue-on-error`. Upload/report steps using `always()` retain evidence without clearing earlier failures. Main has **no branch protection and no rulesets**: failing jobs are real failures, but not enforced merge rules. Repository policy was inspected, not changed.

| Check | Local execution | GitHub Actions | Enforced? | Evidence |
| --- | --- | --- | --- | --- |
| Both JVM variants / synthetic acoustics | New toolchain, 374 passed / 16 skips | Fast workflow actually executes both; explicit task names retained | Test failure fails job | JUnit XML per variant |
| Instrumentation | No local device operation in this audit | Entire suite, fresh emulator, actual runner output | Runner failures/missing success fail job | Complete-Emulator-Results |
| Debug/app-test compilation and Lint | Passed; 0 lint errors | Fast and deep builds, fast lint | Compilation/lint errors fail | Gradle steps, lint XML |
| JVM coverage | 28.19% / 25.69% | Real JaCoCo report then ratchet | Baseline floors and tolerance | coverage.xml, summary.json, metrics.json |
| Android coverage | Reused full clean-CI evidence only | Full-run probes plus report | Missing report/data fails; percentage informational | Emulator coverage XML/JSON |
| Detekt / complexity | 1,978 findings, 337 functions | Executes analysis and summaries | General findings informational; reliable MCC ratchet enforced | Detekt XML/SARIF/summary |
| CRAP-L | 8 mapped / 329 unmapped | Same exact mapping pipeline | High-risk regressions/mapping loss fail | metrics.json, baseline.json |
| Kotlin duplication | 59 files / 0 groups | CPD then ratchet | New groups/empty scan/missing report fail | cpd.xml, metrics.json |
| PIT mutation | 11/11 killed | Real manually selected job | PIT 100% threshold; missing XML fails | mutations.xml / HTML |
| Localization | 564 strings / 5 plurals / 2 arrays | Fast step | Parity/format failure fails | check_localization.py exit |
| Python tooling | 8 passed | Fast step | Unittest failure fails | Unittest output |

## Demonstrated failure behavior

Eight existing Python unit tests passed. Additional isolated CLI probes copied technical reports/policy only and deliberately changed inputs: healthy report exits 0; coverage regression, complexity increase, CRAP-L increase, mapping loss, new duplication, missing report, empty CPD scan, and zero coverage denominator each exit 1. These are **controlled report perturbations**, not newly discovered production defects. No real baselines were weakened. The actual historical failing emulator run above independently demonstrates that a test failure fails CI even when subsequent coverage/artifact collection succeeds. Runtime test configuration uses `ignoreFailures=false`; Lint uses `abortOnError=true`.

Ignored local evidence: `checkpoints/final-audit/remote-initial.json` (run/job/step timestamps and artifacts), `gate-probes/results.json`, `branch-protection.json`, toolchain logs, JVM XML and generated reports under `build/` and `app/build/`. Only technical, nonprivate CI reports are downloaded. Reports are regenerated by the [quality guide's verified commands](QUALITY_GUIDE.md); transient run metrics are not hardcoded into either README.

## New-toolchain remote execution and concrete test repair

Commit `24c149a9fab421f5c9fccff42b79b8e63d43ff56` passed [fast CI 42360716446](https://github.com/hmeclazcke/DidgeTaller/actions/runs/42360716446) (09:40:27–09:45:38 UTC) and [manual mutation 42360726639](https://github.com/hmeclazcke/DidgeTaller/actions/runs/42360726639) (09:40:37–09:42:55 UTC) on 2026-11-07. Actual downloaded artifacts exactly reproduce both 195-case JVM variants, JVM coverage, eight CRAP-L mappings, 59-file/zero-group CPD and 11 killed PIT mutants. The entire PIT job took 2m18s; its actual mutation engine reported under one second, excluding setup/compilation. Both public READMEs' exact-SHA GitHub-rendered HTML was also verified, including image markup.

The [full emulator run 42360716441](https://github.com/hmeclazcke/DidgeTaller/actions/runs/42360716441) (09:40:28–09:50:27 UTC) failed correctly: 127 discovered, 116 passed, 1 failed, 10 skipped. `ScreenAwakeTest` threw `LifecycleOwner of this LifecycleRegistry is already garbage collected` during a lifecycle transition. Its anonymous owner was passed directly into a weakly owning LifecycleRegistry and never retained. This was a test harness ownership bug, not an assertion about incorrect production screen-on behavior. The correction creates a retained owner which owns its registry and is captured by composition and transition callbacks. All existing acquisition/pause/resume/stop/disposal assertions remain. Production ScreenAwake is unchanged. The corrected Android-test APK compiled locally; complete clean-emulator CI must validate the fix. No blind rerun, exclusion, assertion weakening or local private emulator access was used.

Failed-run coverage (91.09% lines / 68.13% branches) is diagnostic only and must not be described as a passing full-suite result. This real new-toolchain failure further verifies required error propagation even when report/artifact steps succeed.

## Remaining debt and delivery gate

1. 329 unmapped functions and three historical CRAP-L violations; high UI complexity. Characterize branches before separately scoped refactoring.
2. Mutation scope is deliberately small; broaden only with reviewed Kotlin operators and meaningful deterministic tests.
3. Android error/background branches and navigation/font configurations are not all executed in the default CI device. Android percentage ratchet is not yet enabled.
4. Temporary AGP legacy flags, Kotlin vendor-tested range and Gradle 10 deprecations require a future compatible migration. Current actual execution, not an unsupported compatibility claim, is the acceptance evidence.
5. Repository merge protection is absent. Workflow failure semantics do not supply branch protection automatically.

Publication requires observing fast, full emulator, and one manually selected mutation run against the final published commit. Their live [fast](https://github.com/hmeclazcke/DidgeTaller/actions/workflows/android.yml) and [deep](https://github.com/hmeclazcke/DidgeTaller/actions/workflows/quality-deep.yml) histories expose exact SHAs, statuses, timestamps and expiring artifacts; pending is not passed. The final delivery report records those outcomes. This audit performs no local emulator install, Samsung operation, private recording read, data deletion, acoustic change, or metronome implementation.

## Final verified delivery

All three jobs passed against code revision **90d71674717f7143924ad73018b42dc4ca43dd27**. Actual logs, JUnit XML, mutation XML, full per-test Android terminal statuses and coverage summaries were downloaded and inspected, not inferred from green badges.

| Run | Event | Job start–end, 2026-11-07 UTC | Conclusion | Available artifacts |
| --- | --- | --- | --- | --- |
| [Fast 42361585974](https://github.com/hmeclazcke/DidgeTaller/actions/runs/42361585974) | push | 09:53:05–09:59:58 | Success | Quality-Metrics-and-Test-Results; DidgeTaller-JVM-Coverage; DidgeTaller-Detekt |
| [Full emulator 42361585985](https://github.com/hmeclazcke/DidgeTaller/actions/runs/42361585985) | push | 09:53:05–10:06:02 | Success | Complete-Emulator-Results |
| [PIT 42361596610](https://github.com/hmeclazcke/DidgeTaller/actions/runs/42361596610) | workflow_dispatch, mutation | 09:53:15–09:55:29 | Success | Focused-Kotlin-Mutations |

Final JVM: each variant 195 discovered / 187 executed and passed / 0 failed / 8 protected-fixture skips; total 390 / 374 / 0 / 16. The final fast artifact exactly reproduces local line/branch counters, eight reviewed CRAP-L scores, 337 measured functions, 329 unmapped functions, three historical high scores and zero CPD groups across 59 files. All eight Python tooling tests, localization, lint and build steps passed. PIT generated/killed 11/11, zero other outcomes; 100% denominator 11, one-second engine duration, 2m14s entire job.

Full Android: API 35 x86_64 Pixel 2, coverage enabled, clean synthetic emulator; **127 discovered / 117 executed and passed / 0 failed / 10 skipped**, runner duration **447.959s**. `ScreenAwakeTest` explicitly ends with status 0. All seven protected cases skip before fixture access; the remaining three configuration-dependent skips are documented above. This audit did not rerun those alternate configurations locally. Missing success, incomplete discovery, failures or absent probes would fail the runner/report steps.

| Final separate Android scope | Line coverage | Branch coverage |
| --- | ---: | ---: |
| Overall production | 91.09% (4,224/4,637) | 68.25% (3,865/5,663) |
| Pure core | 95.25% | 69.19% |
| Audio adapters | 95.22% | 68.56% |
| Persistence | 94.37% | 58.60% |
| UI | 90.21% | 70.59% |

These supersede earlier Android snapshots for the new toolchain. Android execution-path percentages remain informational and separate from stable JVM coverage. No acoustic score or baseline was adjusted. Final API metadata, exact-SHA README/workflow blob verification and immutable job logs are saved under ignored `checkpoints/final-audit/verified-code-90d7167.json`; run-ID-prefixed archives are under `checkpoints/github-ci/`.

The closing evidence commit changes documentation only. Production, tests, Gradle, tools, policy and workflow inputs remain byte-identical to the three successful runs above, verified by Git diff. Documentation-only pushes intentionally trigger no Android workflows; there is no newly pending required run and no claim that suites re-executed at the evidence-only SHA. This preserves valid exact-input evidence without another costly emulator run. Both public READMEs remain current, English/Spanish resources and galleries are preserved, and the official skill/license remain unchanged. No concrete failing gate blocks normal feature development; the explicitly listed measurement and compatibility debts remain.
