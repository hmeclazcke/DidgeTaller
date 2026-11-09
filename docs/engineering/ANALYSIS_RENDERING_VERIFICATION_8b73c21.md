# Analysis/rendering source verification - 8b73c21

Source: `8b73c2110c72d20bc1aab696fed3139877542edc`; implementation commits
`a4dd88a` and `8b73c21`. Original main remains
`3a160043f9ebb3a420b1d89a9cf4ddfe7d1b5d67`. PR #1 remains draft; whole rebuild
IN PROGRESS. Documentation-only descendants may reuse unchanged executable inputs;
the source SHA above identifies exactly what was executed.

## Actual remote runs

| Workflow | Run | Scope | Result |
| --- | --- | --- | --- |
| Android Kotlin CI | [42536349325](https://github.com/hmeclazcke/DidgeTaller/actions/runs/42536349325) | JVM variants, static/build, differential, localization, coverage, CRAP-L/CPD gates | Passed |
| Full emulator | [42536344767](https://github.com/hmeclazcke/DidgeTaller/actions/runs/42536344767) | Fresh API 35 emulator-5554, complete suite, separate coverage, five configuration cases | Passed |
| Manual mutation | [42536346213](https://github.com/hmeclazcke/DidgeTaller/actions/runs/42536346213) | Enforced 11-mutant focused scope plus expanded domain execution | Passed |

Automatic duplicate fast push runs are cancelled by concurrency in favor of the PR
run, never counted as passes. Superseded native source a4dd88a was cancelled after
publication of the actual axis correction; its full instrumentation is not claimed
as passing. That source's fast and mutation runs passed independently, but final
scope totals must come from the runs above.

## Source-scoped remote metrics

Final Linux JVM: 896 discovered, 880 passed, 16 protected skips, zero failures
(448/440/eight per variant). JaCoCo 42.58% lines / 38.83% branches; core 99.04% /
83.52%. Source inventory: 470 reliable CRAP-L scores, 111 independent unscored,
73 JVM scores above target. The 621 PSI functions are 470 measured, 36 parent-
accounted locals, 30 abstract, 13 anonymous-owner and 72 Compose transformations.
The independent unscored count excludes four local anonymous implementations;
there is no ordinary eligible missing-score waiver. Changed-code gate and its
failure-semantics tests passed; no baseline/threshold reset.

Final selected broad PIT: 64 mutated bytecode classes, 2,250 generated, 1,739
killed, 494 survived, 15 uncovered, two timed out, zero tool errors. Killed fraction
1,739/2,250 = 77.29%; PIT detection including timeouts 1,741/2,250 = 77.38%.
Engine execution 154 seconds, separate from provisioning/build time. The enforced
focused selection kills 11/11. Broad score is informational; successful nonempty,
error-free execution is required. Its scope grows from 51 classes/1,883 mutants,
so 77.38% versus previous 77.75% is not a same-scope regression comparison or a
whole-application score. Survivors and uncovered paths remain visible.

Reports are available as seven-day run artifacts: Quality-Metrics-and-Test-Results,
DidgeTaller-JVM-Coverage, DidgeTaller-Detekt and Kotlin-Mutations. They contain no
private fixtures/WAVs. Local copies are ignored checkpoints/github-ci/<run>-*.zip.

## Android-only evidence

Fresh GitHub API 35 x86_64 emulator, explicit serial emulator-5554, JaCoCo enabled:
158 discovered, 148 passed, zero failed, ten protected/configuration skips. The
five additional EN/ES library, EN/ES session and ES landscape configurations pass
independently. Full-suite instrumentation took place, not just APK compilation;
raw runner logs contain OK (158 tests), which includes the ten reported skips.
Android-only coverage: lines 5,188 covered/455 missed = 91.94%; branches 4,396
covered/2,024 missed = 68.47%. Never merged with JVM coverage. Native CRAP-L maps
470 functions, 29 above target; unsupported classifications remain explicit.

Complete-Emulator-Results contains instrumentation.json/txt, separate coverage,
function correspondence and 36 authentic synthetic PNGs. Six current remote PNGs
were visually inspected: normal session, spectrogram, harmonics, EN/ES fullscreen
150% and ES landscape 150%. Scientific text and navigation controls remain visible.
A remaining pre-existing UI issue is explicit: the centered empty-trace message
can overlap the Y-label area at 150%. Its owner is outside PlotSurface, in
ImmersiveViewer's overlay; bounding that annotation to the actual plot rectangle
is the next renderer cycle. This is not a claim of complete UI accessibility.

| Comparable report category | ec19b18 source | 8b73c21 source |
| --- | ---: | ---: |
| JVM discovered / passed / skipped | 796 / 780 / 16 | 896 / 880 / 16 |
| Android discovered / passed / skipped | 153 / 143 / 10 | 158 / 148 / 10 |
| JVM line / branch | 39.71% / 36.43% | 42.58% / 38.83% |
| Core JVM line / branch | 98.98% / 82.70% | 99.04% / 83.52% |
| Android line / branch | 91.63% / 68.66% | 91.94% / 68.47% |
| Reliable CRAP-L / independent unscored | 436 / 96 | 470 / 111 |
| Above-target JVM / Android | 75 / 29 | 73 / 29 |
| Broad PIT classes / mutants | 51 / 1,883 | 64 / 2,250 |
| Detekt informational | 1,970 | 1,989 |
| Lint errors / warnings / hints | 0 / 52 / 8 | 0 / 52 / 8 |
| CPD groups >=100 tokens | 0 | 0 |
| Original-main synthetic differential | 741 identical | 741 identical |

Coverage and mutation denominators expand with production/generated code. Android
branch coverage slightly decreases; broad PIT scope changes and Detekt information
increases, so these are not advertised as universal metric improvements. Named
renderer decisions 163 -> 167 while maximum MCC 127 -> nine and facade MCC eight
are the explicit structural tradeoff. The quality gate remains passing without
numerical baseline updates; high-risk historical functions are still unfinished.

## Local evidence

Canonical Windows JVM: 896 discovered, 880 passed, 16 protected assumption skips,
zero failures. Five engineering-tool and 45 Python tests pass; localization parity
576 strings/five plurals/two arrays. Lint 0 errors/52 warnings/eight hints, Detekt
1,989 informational, CPD zero groups >=100 tokens. JaCoCo JVM 42.57% line/38.74%
branch; core 98.99%/83.28%. No local Android coverage enabled. Local full API 37
before the final fixes retains 146 passed/one genuine failure/nine skips; later
focused final native ownership, readiness and EN/ES/150%/landscape checks pass.
No clean final local full-suite pass is invented.

Selected local PIT: analysis 65/52 killed/13 survived/zero uncovered; plots
426/345 killed/79 survived/two uncovered, zero timeouts/errors. All 26 AxisDrawing
mutants killed. Broader scope and survivor interpretation remain separate.

Local unaltered PNGs were inspected for actual scientific text, selected cursor,
compact session, spectrogram, readiness and 150% EN/ES/landscape. Final axis
landscape no longer collides; graphs remain backed by actual synthetic audio.
No CPU/FPS improvement or exhaustive pixel equivalence is claimed.

## Remaining work

Next production cycle: bound the empty-evidence annotation to the measured plot
rectangle with a real EN/ES 150% overlap regression; then characterize `TallerViewModel.setSwipeComparison` late
progress/result/failure and disposal/replacement, then replace its independently
owned executor/version publication. Association/optimization, remaining DSP/UI
hotspots and mutation survivors still require substantive work. No original skill,
acoustic equations, saved format or physical device changed in this milestone.
