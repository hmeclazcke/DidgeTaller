# Workspace, rendering and simulation verification

Status: this milestone verified; whole-application reconstruction IN PROGRESS.
Production/test/tooling source: `ec19b18923db546a23052f2e4d5f9722f20ac453`.
Original main remains `3a160043f9ebb3a420b1d89a9cf4ddfe7d1b5d67`.
Later documentation-only descendants may reuse this evidence only while executable
inputs remain identical. PR #1 remains draft; no merge or physical-device operation.

## Actual remote executions

| Workflow | Result and scope | Evidence |
| --- | --- | --- |
| Android Kotlin CI | Passed: both JVM variants, parser tests, Lint, debug/test APKs, localization, differential, JaCoCo, Detekt, CPD, source/metric ratchets and Python failure-semantics tests | [42508379814](https://github.com/hmeclazcke/DidgeTaller/actions/runs/42508379814) |
| Full emulator | Passed: clean API 35 x86_64 Pixel 2 emulator; 153 discovered, 143 passed, ten protected/configuration skips, zero failures; five further EN/ES/150%/landscape cases passed | [42508375240](https://github.com/hmeclazcke/DidgeTaller/actions/runs/42508375240) |
| Manual mutations | Passed execution/report validity and original focused threshold; expanded domain score remains informational | [42508377125](https://github.com/hmeclazcke/DidgeTaller/actions/runs/42508377125) |

The redundant push fast run 42508375248 was cancelled by existing PR concurrency;
it is not a passing execution. The mutation-only dispatch intentionally skips the
emulator job; the separate push run actually executes it. No required failure is
hidden through continue-on-error. Artifacts expire after seven days; commands in
the [quality guide](../quality/QUALITY_GUIDE.md) regenerate them.

## Comparable recent evidence

| Metric | Geometry source 6f84faf | Current ec19b18 |
| --- | --- | --- |
| App JVM debug + release | 748 passed / 16 protected skips | 780 passed / 16 protected skips; 796 discovered |
| Android full suite | 139 passed / ten skipped | 143 passed / ten skipped; 153 discovered |
| Separate configuration cases | Five passed | Five passed |
| JVM line / branch, Linux CI | 39.13% / 36.17% | 39.71% / 36.43% |
| Pure core line / branch, Linux CI | 98.90% / 82.53% | 98.98% / 82.70% |
| Android-only line / branch | 91.56% / 68.53% | 91.63% / 68.66% |
| Reliable function CRAP-L | 430 | 436 |
| Independent Detekt-unscored declarations | 94 | 96 |
| JVM above target | 76 | 75 |
| Android above target | No retained comparable report | 29 |
| CPD groups >=100 tokens | Zero | Zero |
| Local comparable Detekt observations | 1,967 informational | 1,970 informational |
| Lint errors / warnings / hints | 0 / 52 / 8 | 0 / 52 / 8 |

Code denominators expand with the added implementation; these percentages are not
a constant-source experiment. Original-main JVM coverage was 28.19%/25.69%, but
its eight-function CRAP allowlist is not comparable to the expanded inventory.
Final Windows JVM coverage is 39.69%/36.33%, core 98.92%/82.45%. Its denominators
match Linux exactly; the covered-counter difference is isolated to RecordingCapture
(one line/six branches). Do not merge environments or claim identical execution.
The earlier noncanonical Windows branch percentage is not used as a clean baseline.

All 576 PSI named declarations remain visible: 436 measured, 40 nested within
parent metrics, 28 abstract, 13 anonymous and 59 Compose. The independent unscored
count is 59 Compose + 28 abstract + nine nonlocal anonymous; four additional local
anonymous declarations are outside Detekt's independent count. The two new
unscored entries are real bodyless loader/task contracts, not untested eligible
logic. Detekt's object-literal visitor limitation and Compose correspondence
limitations remain explicit. No MCC/coverage value is fabricated.

The historical metrics.json key measured_functions is the Detekt inventory count
(532 here), not the number of reliable CRAP-L scores. Use methods length (436),
unmapped_functions (96), and function-mapping.json's complete PSI classifications.
This schema-label limitation does not change gate arithmetic or its rejection of
unmeasured eligible changed functions.

## Ownership and defect-detection evidence

The [simulation design](SIMULATION_OWNERSHIP.md) includes actual native RED before
the disposal correction, nine controller/seven coroutine JVM contracts and four
native contracts. Android source-specific scores confirm calculate/onSimulationState/
onCleared at 1/2/1 with full method-line execution; simulation request is 5.0145
at 91.67% Android method lines, finish/close 2/2 and loader submit 4. JVM directly
executes controller methods at 100% method lines. Framework coverage is never
borrowed to inflate JVM results.

Expanded PIT executed 1,883 mutants in 51 bytecode classes: 1,462 killed, 405
survived, 14 uncovered, two timed out, zero execution errors; PIT duration 137 s.
Detection is 1,464/1,883 = 77.75% including timeouts; killed-only is 77.64%.
The prior f4b5954 report had 1,801 mutants in 43 classes, so scope differs. New
WaveGeometry/RasterPlacement factories kill all 46 mutants. Simulation is 36:
31 killed/five survived, matching its focused local selection. The original
PlotTouch threshold separately remains 11/11, not an application-wide score.

The five simulation survivors concern clearing a finished private handle, ignored
Unit return values and generated coroutine resume guards. They are not removed or
declared formally equivalent. Earlier seven capture/playback survivors retain
their individual analysis in [workspace evidence](WORKSPACE_REBUILD.md). Remaining
405 broad survivors and 14 uncovered mutants are unfinished domain-specific work.
Tests killed the newly exposed reentrant-disposal and default-evaluator weaknesses;
production behavior was not changed just to force a perfect score.

The existing 741-case synthetic differential probe remains byte-identical against
original main. It protects selected acoustic/persistence/geometry contracts, not
the entire UI or the new controller. Late publication after disposal is an
intentional bug correction independently tested against owner-lifetime rules.

## Native and visual limits

Local fresh API 37 had a full-run launcher ANR: 122 passed/21 failed/nine skipped.
One launcher-only recovery and bounded retry passed all 21 affected cases; four
final simulation cases passed separately. That failed run is preserved, never
relabeled a clean pass. The remote API 35 full pass is separate evidence and enables
Android coverage; local API 37 coverage was not enabled.

The remote artifact contains 36 authentic synthetic PNGs; seven were visually
inspected for this source: EN/ES wide Dron at 150%, landscape wide Dron, landscape
selected spectrogram, EN rendered spectrogram, ordinary selected cursor, and ES
recovery confirmation. Selected-tab assertions now prove Dron captures actually
show Dron. The rendered spectrogram contains real synthetic harmonic data; the
earlier selected-state capture can precede tile readiness and is not promotional
proof of a complete raster. Gap views correctly avoid inventing frequency.

Known unfinished UI issue: scientific description truncates at 150% in narrow
fullscreen portrait. Recovery-dialog body scrollability and large-font explanatory
text still need focused accessibility review. No universal clipping-free, physical
acoustic-accuracy or measured performance improvement is claimed.

Next production task: characterize and replace recording-analysis version/executor/
progress orchestration with explicit operation ownership. Association/comparison,
remaining PlotSurface axes/gesture boundaries (MCC 127), DSP hotspots and other
high-risk functions remain IN PROGRESS. Main, official skill/license, Samsung and
private WAVs are untouched; all native data is synthetic.
