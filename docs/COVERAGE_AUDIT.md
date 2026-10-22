# Initial JVM coverage audit — 2026-10-22

Measurement only, starting from clean `main` at `1b6a539`. Application version
3.2.6-beta1/12 and all production/test sources are unchanged. No new tests,
refactoring, instrumented/device tests, private WAV replay, CPD or mutation tooling.

## Compatibility and reproduction

Installed **JaCoCo 0.8.15**, the latest stable release in the
[official change history](https://www.jacoco.org/jacoco/trunk/doc/changes.html).
It supports the existing Java 17 bytecode target and local JBR 21.0.11, includes
recent Kotlin SMAP fixes, and inherits Kotlin/Compose filters introduced in 0.8.13
and 0.8.14. Kotlin/Compose plugin 2.2.21, AGP 8.13.2, Gradle 8.13, local JDK 21
and CI Temurin 17 are retained. The Gradle built-in `jacoco` plugin attaches the
agent to existing JVM `Test` tasks; Android build-type coverage is not enabled,
avoiding a second instrumentation path. No application runtime dependency is added.

From the repository root:

```powershell
.\gradlew.bat :app:jacocoDebugUnitTestReport :app:assembleDebug --console=plain
.\gradlew.bat detekt detektComplexity --console=plain
python tools/detekt_summary.py
python tools/coverage_summary.py
```

Use `./gradlew` and `python3` on Linux/macOS. The report task depends on
`:app:testDebugUnitTest`; Gradle reruns tests when their inputs/agent change and
clears that test task's execution file before execution. To deliberately force a
fresh run later, add `--rerun-tasks` to the first command. No coverage threshold or
verification gate is installed; low coverage does not fail CI. Existing test
failures, configuration errors and missing/invalid reports remain failures.

Outputs (ignored build artifacts):

- `app/build/jacoco/testDebugUnitTest.exec`: JVM execution data.
- `app/build/reports/jacoco/debug/html/index.html`: browsable source coverage.
- `app/build/reports/jacoco/debug/coverage.xml`: JaCoCo counters and method identities.
- `app/build/reports/jacoco/debug/summary.md` and `summary.json`: reproducible
  component counters and the eight reviewed method correspondences.
- GitHub Actions artifact: `DidgeTaller-JVM-Coverage` (HTML, XML and summaries).

## Denominator and compiler filtering

The report reads production debug classes from the existing Kotlin output
`app/build/tmp/kotlin-classes/debug` and Java output
`app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes`.
These paths were verified against this toolchain; review them when upgrading AGP/KGP.
Only `ar/didgetaller/**/*.class` is included. Android R/BuildConfig/Manifest classes
and Kotlin WhenMappings are excluded. Test classes, dependencies and generated
Android outputs are not report inputs. The task rejects absent production classes
or execution data instead of presenting an empty report as success.

JaCoCo's report-time Kotlin/Compose bytecode filters remove recognized compiler
scaffolding. There is deliberately no `$*`, lambda, ComposableSingletons or whole-UI
exclusion: generated containers can hold handwritten drawing/callback logic.
Remaining accessors, default dispatchers, state machines and Compose transformations
can still affect counters, particularly method coverage. Counters describe this
compiled JVM artifact, not an exact count of handwritten Kotlin functions.

The XML also contains four empty SMAP references (Comparisons.kt in three packages
and ui/Effects.kt). They carry no counters or executable dependency code. The
summarizer separates them from the 42 actual project sources and rejects any
non-project source with nonzero counters.

The overall denominator retains Android and UI code. A zero there means **not
executed by this JVM suite**, not proven untestable or never tested elsewhere.
AudioRecord/Looper/MediaPlayer/SharedPreferences/Android JSON and Compose rendering
need Android execution or suitable test doubles; this suite uses the mock Android
JAR (`unitTests.isReturnDefaultValues = true`) rather than an emulator/runtime.
Existing historical instrumented results are not merged into this measurement.

## Coverage by component

Each named component includes its entire Kotlin source file and nested classes.
Line counters use JaCoCo source-file unions, not summed class lines, avoiding
double-counting lines shared by nested classes. Rows overlap and must not be added.

| Component | Lines | Instructions | Branches | Methods |
| --- | ---: | ---: | ---: | ---: |
| **Overall production** | **27.59%** | **18.80%** | **27.65%** | **21.63%** |
| Pure core package | 98.24% | 92.53% | 76.54% | 85.71% |
| PitchDetector.kt | 100.00% | 93.31% | 83.33% | 80.95% |
| PitchTrace.kt | 100.00% | 97.86% | 86.82% | 91.30% |
| PitchStream.kt | 95.00% | 82.61% | 68.75% | 80.00% |
| LivePitch.kt | 100.00% | 98.07% | 88.46% | 100.00% |
| Pcm16WavReader.kt | 100.00% | 90.09% | 69.77% | 100.00% |
| ProgressiveWavWriter.kt | 96.55% | 83.83% | 65.00% | 83.33% |
| WavCodec.kt | 100.00% | 92.77% | 64.29% | 100.00% |
| Audio Android adapters | 0.00% | 0.00% | 0.00% | 0.00% |
| Session persistence, data package | 0.00% | 0.00% | 0.00% | 0.00% |
| SessionAnalysis.kt | 96.30% | 93.27% | 71.05% | 93.75% |
| WavAnalysis.kt | 100.00% | 94.06% | 73.39% | 86.67% |
| PlotViewport.kt (scales/projection/transforms) | 100.00% | 96.44% | 80.95% | 82.22% |
| PitchAxis.kt | 100.00% | 98.23% | 95.00% | 100.00% |
| ExplorerState.kt | 84.62% | 60.38% | 48.33% | 45.00% |
| PlotSurface.kt | 11.11% | 6.09% | 6.74% | 14.63% |
| AcousticCharts.kt | 0.00% | 0.00% | 0.00% | 0.00% |
| PlotGestures.kt | 0.00% | 0.00% | 0.00% | 0.00% |

Overall raw counters: lines 899/3,258; instructions 12,623/67,156; branches
1,114/4,029; methods 315/1,456. Source files match the 42-file production scope.
The summary cross-checks that source-file counter sums equal report totals.

The detector's main diagnostic method alone is 100% line / **82.41% branch**;
PitchTrace.build is 100% / **94.05%**. Whole-file percentages above include the
other methods and model classes. A covered line needs only one executed instruction;
dense Kotlin lines can contain several unexecuted conditions. JaCoCo branch counts
do not include exception handlers or establish all path combinations, timing,
thread ordering, acoustic accuracy, or assertion effectiveness. See the official
[counter definitions](https://www.jacoco.org/jacoco/trunk/doc/counters.html).

## Five highest-priority uncovered behaviors

1. **Capture termination and failure handoff:** MicrophoneEngine is entirely
   uncovered here, including permission/init failures, blocking-read shutdown,
   recording-limit completion, stale callbacks and close-failure handoff. The
   tested PitchStream/LivePitch logic does not test the Android thread/handler path.
2. **Durable session transitions:** RecordingStore, LocalStore, TrashStore,
   SessionToolsStore and AnalysisCache are 0% in this JVM run. Failed rename/index
   commit, partial recovery, orphan reconciliation, trash/undo and stored instrument
   compatibility need Android-aware characterization. Pure SessionAnalysis coverage
   does not prove storage integrity.
3. **WAV exceptional boundaries:** normal streaming, 15-minute synthetic output,
   low-space preservation and partial-header repair already have JVM tests. Missing
   constructor-cleanup execution (ProgressiveWavWriter.kt:17), incomplete validation
   branches and unexercised write/sync/close faults remain distinct gaps. Reader
   lines are all reached but 26/86 branches are missed. Do not label low disk space
   or normal repair as wholly untested.
4. **Actual chart drawing and pointer interaction:** PlotSurface's composable
   body and its drawing callbacks are unexecuted. The file's 11.11% line coverage
   comes from helpers such as axisLabel/placeAxisLabels, not rendered plots.
   AcousticCharts and PlotGestures remain zero; clipping, gap/color rendering,
   real Paint widths, pointer centroid/pan/pinch and viewer lifecycle need the
   appropriate emulator/visual checks. ExplorerState also misses spectrum defaults,
   spectrum domain and logarithmic-domain choices, despite well-covered pure math.
5. **Streaming/diagnostic edge paths:** PitchStream's full-window/oversized-chunk
   replacement at line 20 is unexecuted; zero/invalid-count and range guards have
   missed branches. Detector diagnostics and trace decisions retain 20/120 and
   34/258 missed branches respectively. Some are defensive or compiler-adjacent:
   examine actual source/bytecode and existing assertions before proposing tests.
   The opt-in private replay is skipped, so this run adds no real-audio evidence.

## Detekt correspondence and CRAP limitations

A universal name/line join is unsafe: Kotlin overloads/default dispatchers,
inlining, local functions and Compose callbacks do not preserve a one-function to
one-method relation. Concrete inspection with `javap -p` and XML shows that
PitchTrace.alternativePeriods includes a local function emitted separately as
`alternativePeriods$alternative`; MicrophoneEngine.readLoop emits multiple
`readLoop$lambda...` methods; PlotSurface spans its composable body and many
lambda methods/classes. Those cases are **not assigned CRAP scores**.

Eight pure functions were reviewed as one source body / one JVM method. The
explicit manifest `config/jacoco/method-matches.json` records source file, declaration
line, LF-normalized source SHA-256, owner, method name, full JVM descriptor and
bytecode start line. A score requires an exact unique XML/Detekt match and unchanged
source hash. Stale/ambiguous scores are omitted with a reason, without blocking
coverage/CI or substituting invented values. Re-review this manifest
after source or compiler changes; do not auto-regenerate it as evidence of review.
No generic fuzzy matching or class-wide coverage substitution is used.

For these eight only, calculated `C² × (1 − v)³ + C`, with Detekt cyclomatic
complexity C and method-level coverage fraction v. **CRAP-L** uses JaCoCo LINE;
**CRAP-I** uses INSTRUCTION as a sensitivity comparison. Both are reproducible
approximations: the [original CRAP proposal](https://www.artima.com/weblogs/viewpost.jsp?thread=215899)
uses basis-path coverage, which JaCoCo does not provide. Detekt's source complexity
also differs from JaCoCo bytecode complexity (scope functions, guards and exception
handling differ). These are not canonical basis-path CRAP scores or a quality gate.

| Reviewed method | Detekt C | CRAP-L | CRAP-I |
| --- | ---: | ---: | ---: |
| PitchDetector.analyzeWithDiagnostics | 43 | 43.000 | 44.051 |
| PitchDetector.spectrum | 4 | 4.000 | 4.000 |
| PitchTrace.build | 34 | 34.000 | 34.000 |
| PitchTrace.isolated | 15 | 15.000 | 15.002 |
| PitchTrace.tone | 9 | 9.000 | 9.001 |
| PitchStream.append | 6 | 6.027 | 6.399 |
| LivePitch.next | 8 | 8.000 | 8.001 |
| AxisDomain.transform | 4 | 4.000 | 4.000 |

The near equality to C is a limitation of high line/instruction coverage, not proof
that remaining branches are safe. No project-wide CRAP percentage, threshold,
Compose score or score for generated methods is reported. Pair the estimates with
branch gaps and risk context; test execution alone does not verify assertions.

Local validation: 127 JVM tests passed, one opt-in private replay skipped, no
failures/errors; report generation and APK build succeeded. CI evidence and exact
commands/results are maintained in [VALIDATION.md](VALIDATION.md) and decisions
in [WORK_LOG.md](WORK_LOG.md). Pre-integration recovery point: `1b6a539`; preserve
subsequent work before recovery.
