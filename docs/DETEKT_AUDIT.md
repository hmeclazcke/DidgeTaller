# Initial Detekt audit — 2026-10-22

This is an informational measurement of production Kotlin at application source
commit `3dc3ddb` (3.2.6-beta1/12), not a functional change or proof of defects.
The integration changes only build tooling, CI, a report summarizer, and documentation.

## Compatibility and scope

| Component | Existing version |
| --- | --- |
| Kotlin Android / Compose compiler plugin | 2.2.21 |
| Gradle wrapper distribution | 8.13 |
| Android Gradle Plugin | 8.13.2 |
| Local Gradle JVM | JetBrains JBR 21.0.11 |
| GitHub Actions JVM | Temurin 17 |
| Application JVM bytecode target | 17 |
| Detekt plugin and analysis engine | **1.23.8**, stable |

The [official compatibility table](https://detekt.dev/docs/introduction/compatibility/)
lists Detekt 1.23.8 as built with Gradle 8.12.1, Kotlin 2.0.21 and AGP 8.8.1,
with Java 8 bytecode and tests up to JDK 21. These are upstream build/test versions,
not a guarantee for every newer application toolchain. Newer listed Detekt releases
are prereleases. We prefer the stable release and validate this repository directly.

The plugin is applied at the root, with explicit production source directories
`app/src/main/java` and `app/src/main/kotlin`, including only `**/*.kt` and excluding
`**/build/**` and `**/generated/**`. All 42 current production Kotlin files are
analyzed; test sources, resources, private fixtures, dependencies and generated
outputs are outside this scope. No Android variant integration is applied.

Both tasks use **syntax-only analysis**, without an application classpath. The
isolated Detekt compiler remains 2.0.21; no Kotlin dependency alignment is forced.
See the official [runtime dependency guidance](https://detekt.dev/docs/1.23.8/gettingstarted/gradle/#gradle-runtime-dependencies)
and [type-resolution limitations](https://detekt.dev/docs/1.23.8/gettingstarted/type-resolution/).
Successful analysis verifies the current source syntax, not support for arbitrary
future Kotlin 2.2 language features or type-aware analysis of Kotlin 2.2 libraries.
Gradle's embedded Kotlin version shown by `--version` is not the app plugin version.

## Reproduce locally

From the repository root, using the existing JDK and Android SDK setup:

```powershell
.\gradlew.bat --version
.\gradlew.bat detekt detektComplexity --console=plain
python tools/detekt_summary.py
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --console=plain
```

On Linux/macOS use `./gradlew` and `python3`. Python is needed only for the summary;
the summarizer uses the standard library and installs nothing.

Reports are ignored build artifacts under `build/reports/detekt/`:

- `detekt.html`, `.xml` (Checkstyle), `.sarif`, `.txt`: real threshold-based findings.
- `complexity.html`, `.xml`, `.sarif`, `.txt`: measurement observations only.
- `summary.md`, `summary.json`: counts by severity/rule and ranked function metrics.

`detekt` builds upon the default rules and adds `CognitiveComplexMethod` at 15.
Cyclomatic complexity remains 15, long methods 60 logical lines, large classes 600,
and function-count thresholds 11. These rules report at or above their thresholds.
`detektComplexity` enables only the two complexity rules at threshold zero to obtain
paired measurements for 269 named functions reached by those rules, including
functions below the normal audit thresholds. Its **538 metric observations must
not be added to the finding count**. Nested local functions contribute to enclosing
metrics rather than receiving separate entries. Counts and ranking come from SARIF;
the summarizer cross-checks the audit total against Checkstyle XML and rejects
missing or duplicate paired metrics. Sorting uses descending cyclomatic complexity,
then cognitive complexity, then source path and line.

Both tasks use `ignoreFailures = true`: existing findings remain visible and do not
fail GitHub Actions. Configuration validation is enabled with warnings treated as
errors. CI retains the existing JVM/APK checks, explicitly runs both audit tasks,
generates the summary, and uploads `DidgeTaller-Detekt`. Tool execution failures
and invalid reports remain failures; the workflow does not use `continue-on-error`.
No baseline, new source suppressions, formatting plugin or automatic rewriting is used.

## Findings

**1,499 findings: 1,499 SARIF warnings, 0 errors, 0 notes.** Severity here is the
emitted SARIF level, not a calibrated estimate of product risk. Checkstyle agrees
on the total. Default style rules remain visible rather than being suppressed.

| Rule | Warnings |
| --- | ---: |
| MagicNumber | 817 |
| MaxLineLength | 438 |
| WildcardImport | 60 |
| FunctionNaming | 40 |
| CognitiveComplexMethod | 33 |
| CyclomaticComplexMethod | 27 |
| LongParameterList | 15 |
| EmptyElseBlock | 14 |
| ReturnCount | 10 |
| TooGenericExceptionCaught | 10 |
| LongMethod | 9 |
| LoopWithTooManyJumpStatements | 9 |
| ComplexCondition | 8 |
| TooManyFunctions | 4 |
| MatchingDeclarationName | 1 |
| NestedBlockDepth | 1 |
| SwallowedException | 1 |
| UnusedParameter | 1 |
| UnusedPrivateProperty | 1 |

## Top ten functions

Paths below are relative to `app/src/main/java/ar/didgetaller/`.

| Function | Source | Cyclomatic | Cognitive |
| --- | --- | ---: | ---: |
| PlotSurface | ui/PlotSurface.kt:58 | 88 | 224 |
| ImmersiveViewer | ui/ImmersiveViewer.kt:130 | 50 | 127 |
| SessionPlot | ui/ImmersiveViewer.kt:62 | 45 | 64 |
| TimeChart | ui/AcousticCharts.kt:64 | 44 | 93 |
| PitchDetector.analyzeWithDiagnostics | core/PitchDetector.kt:26 | 43 | 58 |
| SpectrumChart | ui/AcousticCharts.kt:113 | 42 | 99 |
| TallerApp | ui/TallerApp.kt:75 | 41 | 141 |
| MeasureScreen | ui/TallerApp.kt:226 | 38 | 79 |
| PlotTools | ui/ImmersiveViewer.kt:253 | 36 | 71 |
| PitchTrace.build | core/PitchTrace.kt:90 | 34 | 25 |

## Risk-prioritized follow-up (not implemented)

1. **Capture completion and error handling.** `MicrophoneEngine.readLoop` is 18/27
   and contains a generic catch at line 109; generic catches also occur at 48, 50
   and 68. The permission catch at 47 produces `SwallowedException` because the
   original exception is replaced by user-facing text. A future review should
   characterize read/append/close failures and callback ordering: `finishRecording`
   reports close failure but still hands off the file. This is a source-inspection
   concern associated with the findings, not a demonstrated loss of recordings.
   Preserve recoverable partial files and existing lifecycle/data safeguards.
2. **Detector and trace decision complexity.** `analyzeWithDiagnostics` (43/58,
   73 logical lines) combines amplitude checks, difference-function loops, candidate
   selection, interpolation and optional diagnostics. `PitchTrace.build` (34/25)
   combines confidence/profile classification, omissions, support and link decisions;
   `alternativePeriods` is 22/19, with seven clauses in one flagged condition.
   Review against the existing boundary/regression tests before any later extraction.
   Complexity does not establish an octave error, justify changing YIN thresholds,
   or solve the physical vowel-I uncertainty. `PitchStream.append` is 6/6;
   `WavAnalysis.analyze` is 29/39 and 64 logical lines.
3. **Chart projection and viewer state.** `PlotSurface` is the largest measured
   function (88/224, 123 logical lines); drawing branches, labels, clipping, cursor
   and gesture adapters share a function. `ImmersiveViewer`, `SessionPlot`,
   `TimeChart` and `SpectrumChart` are also near the top. This increases regression
   risk around gaps/colors, measured margins and camera ownership; it does not
   prove incorrect rendering. A future change needs existing chart/viewport tests
   and visual evidence. Review the single `UnusedParameter` (`initialPlot`,
   ImmersiveViewer.kt:130) as a possible stale contract before deleting anything.

LongMethod findings (logical lengths, not physical file lines): TallerApp 127,
PlotSurface 123, ImmersiveViewer 107, MeasureScreen 90, DesignerScreen 88,
analyzeWithDiagnostics 73, PlotTools 67, WavAnalysis.analyze 64 and SessionPlot 64.
**No LargeClass finding at the default 600 threshold.** There is still a breadth
problem: TooManyFunctions reports TallerViewModel 59, SessionToolsStore 13,
RecordingStore 11 and the top-level TallerApp.kt file 25. A large file of Compose
functions is not a large Kotlin class. RecordingStore.list (16/26) merits later
persistence/recovery review; function counts alone do not prove data corruption.

## Context-dependent findings and limits

- The 40 FunctionNaming findings include capitalized `@Composable` functions,
  an expected Compose convention. The untouched defaults do not recognize that
  convention; review these as candidates for a precise future Compose configuration.
- Many MagicNumber findings are DSP constants, numeric tolerances, grid positions,
  colors or dimensions. They are maintainability prompts, not evidence that their
  values are wrong. Do not mechanically replace or alter scientific constants.
- Broad catches in Pcm16WavReader.kt:60 and ProgressiveWavWriter.kt:17 close the
  resource and rethrow. SessionWorkspace explicitly rethrows CancellationException
  before its generic catch. These are likely contextual false positives or lower
  priority than a catch that loses an error; do not remove defensive cleanup.
- EmptyElseBlock can be an intentional exhaustive no-op in drawing/UI decisions;
  ReturnCount and LoopWithTooManyJumpStatements can flag useful guard clauses.
  Compose nesting inflates cognitive complexity; its measured branching is real,
  but the number is not equivalent to defect probability or runtime cost.
- `FRAME` in MicrophoneEngine is reported unused. No cleanup is performed.
- Type-dependent checks (including UnsafeCallOnNullableType, coroutine type checks
  and some unused-code checks) are unavailable or incomplete in syntax-only mode.
  The absence of those findings is **not** a null-safety or concurrency guarantee.
  Manual inspection sees `!!` in guarded PitchTrace paths and the recording writer;
  Detekt did not verify those invariants. Constructors, initializers and nested
  functions are not fully described by the named-function top ten.
- No test coverage was measured. **No CRAP score**, coverage percentage, mutation
  score, duplicate-code score, acoustic accuracy or physical validation is claimed.
  No CPD, JaCoCo, mutation tooling, private WAV replay or device operation was run.

Validation commands, exact results and CI evidence are recorded in
[VALIDATION.md](VALIDATION.md); decisions and remaining work are in
[WORK_LOG.md](WORK_LOG.md). Preserve later work before recovering the pre-audit
tooling state at `3dc3ddb`; the application source remains unchanged.
