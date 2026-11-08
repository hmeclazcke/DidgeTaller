# Session workspace ownership and display preparation

Status: implementation and local JVM/static validation complete; native/CI evidence
must be recorded separately before accepting this milestone. Whole rebuild remains
IN PROGRESS. Original main and persisted formats are unchanged.

## Responsibilities and invariants

The old SessionWorkspace mixed coroutine ownership, WAV/level reads, cache keys,
spectrogram/pixel/Bitmap creation and Compose publication in one renderer (MCC 13).
Its generation checks protected many stale results, but the Android/data coupling
prevented deterministic scheduling tests. This is an architectural reconstruction;
the new fake-loader tests are not evidence of a reproduced old main-thread race.

* WorkspaceController is the sole transition writer. Each open/render has an
  identity-bearing operation; opening/rendering are distinct types. Completion
  claims that operation once before notifying an observer. Invalidation precedes
  cancellation, so synchronous cancellation callbacks cannot regain authority.
* WorkspaceLoader is the asynchronous boundary; CoroutineWorkspaceLoader implements
  it with supplied dispatchers and task-owned progress children. All results return
  to the UI owner; callback failures are not swallowed as analysis failures.
* WorkspaceData owns one Closeable PCM reader per operation, validates the loaded
  rate/frame contract and prepares existing waveform/STFT/palette data without
  Android. Its structured use closes readers on errors and cancellation. Frame
  conversion clamps milliseconds before multiplication and retains two frames.
* SessionWorkspace only wires real files/cache/Bitmap construction and publishes
  the controller's immutable snapshot into Compose. That read-only projection has
  no independent transition logic. ViewModel disposal permanently closes the owner.
* A completed tile remains in its original coordinates during replacement. Typed
  cache keys preserve the existing disk key format and separate power reuse from
  palette/reference rendering. Identical pending/completed requests schedule once.
* Visualization has no playback port and cannot modify cursor, pause intent or
  audio selection. Existing PlaybackController/MediaPlayer remains authoritative;
  native integration tests start playback while visualization is still opening.

Empty, opening, rendering, ready, cancelled and failed are explicit statuses.
Failed operations with null/blank messages remain visible and retryable. New
diagnostic messages resolve through existing ES/EN resources. No new detector,
interpolation, threshold, WAV format or session-index migration is introduced.
Readers detect structural source replacement; immutable published WAVs remain the
cache contract. This is not a promise to detect arbitrary external same-size WAV
edits on every render, or filesystem durability across power loss.

Stale images are never published. Unreferenced Bitmaps are reclaimed by Android;
published images are not forcibly recycled while Compose may still draw them.
No claim of immediate native memory reclamation or measured frame-rate improvement.

## Characterization and regression evidence

Controllable fake tasks cover old success/failure/cancellation after replacement,
duplicate completion/progress, repeated IDs/requests, cancellation/retry, preserved
tile coordinates, wrong response kind, missing messages and disposal. Actual
coroutines run on two deterministic queues to exercise delayed owner delivery,
scope cancellation, exception propagation and reader closure without sleeps.
During development, two new-controller contracts failed (post-disposal clear and
wrong-kind response); both were corrected. Another red test removed an unnecessary
intermediate empty publication when opening. These are explicitly tests of the
new protocol, not invented historical device failures.

WorkspaceData tests compare waveform arrays, spectral power and palette pixels
exactly with the existing algorithms, including a logarithmic zoom/reference.
Known source-frame endpoints and extreme timestamp clamping are independent oracles.
PlotReduction extracts the actual RMS/PSD display reductions into pure functions.
Known cell maxima and zoomed-bin coordinates preserve display behavior; the old
empty-density reduction threw, and the red/green regression now returns no curve.
PlotSurface's spectrum memoization keys now include actual plot width, including
axis margins/font geometry. Canonical MusicalTrace, gesture/time conversion and
the existing 80 ms real-player pause bound are unchanged.

Local final validation: 734 JVM discovered across debug/release, 718 passed, zero
failures, 16 protected skips. Initial implementation counts were 728/712/16, before
wave-amplitude and reducer boundary regressions. Latest canonical local coverage
is 38.64% JVM lines / 35.55% branches, core 98.81% / 81.84%.
Denominators changed; do not call these equal-scope percentage
improvements. All 34 measured workspace/reduction functions stay below target;
pure/scheduler/reduction logic has 100% method lines and CRAP-L 1–9, while the
Android facade has real JVM zeros retained. SessionWorkspace.render becomes MCC 1 /
JVM CRAP-L 2; controller
render is MCC/CRAP-L 9, loader submit 3, data preparation <=3, reductions 5 and 6.
PlotSurface MCC falls 139 -> 130; its remaining drawing/gesture/axis responsibilities
are still a material next reconstruction cycle, not declared complete.

Current source mapping: 426 measured / 93 independent Detekt-unscored. The additional
three are abstract loader/task contracts, not missing ordinary measurements.
Existing 58 Compose and nine invalid-MCC anonymous declarations remain explicit.
76 measured JVM methods remain above target in the wider application. Numerical
baselines are not reset. Signature relocation of VisualRequest and consolidation
of stateSnapshot into the controller have explicit retired-method correspondences.
New constructors, accessors and wiring initializers receive exact-content technical
reviews with real test references; no eligible ordinary function is waived.

## Remaining measurement limitations (bounded investigation)

Detekt 1.23.8's [pinned metrics source](https://repo.maven.apache.org/maven2/io/gitlab/arturbosch/detekt/detekt-metrics/1.23.8/detekt-metrics-1.23.8-sources.jar)
explicitly skips visitNamedFunction for functions with an object-literal ancestor.
That produces the observed MCC=0/no-body traversal on concrete anonymous adapters;
this is an upstream visitor limitation, not zero test coverage or permission to
invent MCC=1. A validated standalone visitor or named adapter redesign remains work.

One same-name Compose method is not the entire callable body: current PlotSurface
has 31 related JVM methods, primary method 71 lines, drawing lambda 87 lines.
Source MCC includes these callbacks. Pairing it only with the primary method's
line counter would assign setup coverage to different drawing work. Source/SMAP
aggregation needs independent validation; no speculative score is enabled here.
Details/probe JSON remain under ignored checkpoints/workspace. Official ub-mode
and license remain unchanged; the Kotlin tools are not original Crapper/Mutator.

## Mutation review

The first workspace scope generated 187 mutants (155 killed, 25 survived, seven
uncovered). Review added real tests for cancelled/replaced work, two-frame/empty
boundaries, fractional milliseconds, blank failures, nondefault log/reference,
last-read interruption and owner disposal before dispatch. Coroutine state-machine
classes are intentionally included in the expanded selected scope; generated Unit
bridges/continuation guards remain distinct from meaningful domain mutants.
Final scope totals must be taken from the final XML/CI, not this initial run.

A later Windows run produced two RUN_ERROR outcomes with a reset PIT minion socket;
those are not killed mutants or valid completed measurements. The interruption
contract now uses its own joined worker instead of interrupting JUnit/PIT's shared
thread. The bounded rerun completes with 204 mutants: 186 killed, 17 survived, one
uncovered, zero tool errors/timeouts (91.18% detection of this selected scope).
This includes coroutine implementation classes; it is not comparable with the
smaller initial denominator. Generated Unit bridges, redundant input/continuation
guards and an unused generated getter remain explicit. PlotReduction's first
selected scope was 56 mutants: 49 killed / seven surviving. Future-audio RMS and
exact-bin budget/edge tests kill four meaningful geometry/loop weaknesses: final
53 killed / three surviving (94.64%). Remaining guard/clamp mutations overlap the
confirmed reduced-bin range, plus an equivalent empty-list return. These are
contract arguments, not formal equivalence proofs. No score is called whole-app.

PIT can exit zero while reporting RUN_ERROR. Every Gradle mutation task now runs
tools/mutation_report.py: missing/empty/unexecuted reports, unknown/unfinished or
infrastructure-error outcomes fail. Four isolated validator tests defend those
semantics; 41 Python tooling tests pass. Normal survivors remain informational,
and the existing PlotTouch score threshold remains enforced.

The request model initially treated a negative waveform-amplitude viewport as a
spectral band. A failing regression reproduces that integration incompatibility;
VisualRequest.forView now distinguishes time-only waveform work from spectral Hz/
palette work. UI readiness checks are removed in favor of the owner boundary, and
VisualKind replaces the ambiguous Boolean render parameter. The real long-WAV
test also exercises a negative amplitude viewport without weakening its byte or
sample-resolution assertions.

The seven prior survivors were examined individually:

| Method | Remaining mutant meaning / contract reasoning |
| --- | --- |
| PlaybackController.poll | Removing backend-null guard is masked by ready=false in Stopped/Preparing; legal Ready always retains its backend. |
| PlaybackController.start | Internal callers require Ready before entering; the fallback guard cannot be reached through valid callbacks. |
| PlaybackController.toggle | The null-backend disjunct is redundant with !ready under the single-owner state invariant. |
| CaptureHandoff.terminal | Clearing the private recording identity after a non-Ready outcome releases an opaque reference; claimed snapshots/publication refusal already prevent reuse. |
| RecordingCoordinator.append | Phase rejection overlaps missing current pending snapshot after save/discard and absent recording during idle monitoring. |
| RecordingCoordinator.saving | Early handoff.stop detaches the old monitor; phase rejection and terminal handling protect visible state if omitted. |
| RecordingCoordinator.discarding | Same lifecycle overlap as saving; retaining explicit detachment documents ownership and shortens reference lifetime. |

These are contract/invariant equivalence arguments, not a formal proof over arbitrary
invalid reflective states. Cleanup and defensive checks are retained; no meaningless
private-state assertion or production deletion is used to force 100%. Focused reruns
currently reproduce 94 killed/5 survived of 99 handoff mutants and 125/22 of 147
playback mutants. New workspace survivors are reviewed separately in final evidence.

## Reproduction and remaining acceptance

### Ordered trace preparation follow-up

TraceDrawing now owns canonical/legacy source-coordinate drawing decisions;
PlotSurface retains projection, clipping, ink and Canvas operations. Its ordered
Mark list preserves the original interleaving of segments, isolated dots and
uncertainty rings. Separate lists would reorder overlapping marks, so that design
was rejected. MusicalTrace links and bridge flags remain authoritative; no new
measurement is generated or used for the 100 ms readout. Historical input must
come through PitchTrace's non-null rendered-sample boundary.

Seven independent contracts pin empty/isolated/gapped/bridged traces, tone,
uncertainty visibility, malformed boundary input and the 1,200 ms link boundary.
PIT exposed a timestamp-subtraction survivor because the initial cases used a
zero-origin predecessor. Translation-invariance checks now include nonzero starts;
the subtraction-to-addition mutant is killed without changing correct logic.
Prepared marks are retained while only the cursor moves; bridge path effects are
also retained. This is structural work, not a measured CPU/FPS improvement.

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests 'ar.didgetaller.core.Workspace*Test' --tests ar.didgetaller.core.PlotReductionTest
.\gradlew.bat :app:qualityMutationWorkspace :app:qualityMutationPlots
```

Full commands/report interpretation remain in the quality guide. Native tests use
synthetic isolated namespaces, compare WAV/index bytes, verify real Bitmap creation,
missing input retry and disposal while work/playback is pending. The clean-emulator
workflow additionally collects actual session views in EN/ES compact 150% font and
landscape. Compiled androidTest is not executed instrumentation; CI and screenshot
results must be observed. Main, Samsung and private WAVs remain untouched.
