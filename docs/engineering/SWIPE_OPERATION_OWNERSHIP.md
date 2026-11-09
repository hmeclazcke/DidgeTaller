# Experimental comparison operation ownership

Continuation after the bounded-annotation cycle. Whole application IN PROGRESS;
main remains3a160043 and PR1 stays draft. No acoustic algorithm, persisted format,
WAV contents, native kernel, detector threshold or private fixture changes.

## Reproduced lifecycle defect

The previous setSwipeComparison implementation used a ViewModel coroutine and
Handler progress, not an executor/version pair. It published enabled/running
before launching into a possibly cancelled scope. A real native regression calls
ViewModelStore.clear followed by enable: the cleared model became enabled/running
although no work could execute. The empty synthetic namespace never opens a
private WAV. The final test also exercises the public Application constructor.

SwipeComparisonController now owns a selected request and a publication grant.
Typed Idle/Disabled/Loading/Ready/Failed/Disposed snapshots preserve legal state
combinations. Retirement invalidates the grant before cancelling its producer;
terminal publication claims it once. Cached curves belong to exactly one request;
disabling retains that curve, another session cannot reuse it, and navigation
clear permits future work while disposal is permanent. Repeated in-flight enable
keeps the existing cancel/restart behavior rather than silently changing policy.

The ViewModel projects that one status into the existing read-only interface.
A separate pure UI-state binding changes comparison cameras only when the enabled
session changes; progress and terminal results cannot reset a user-panned camera. Disabled
and cleared states restore the original camera. One pure presentation-window
policy replaces the two existing calculations; its33.25..147Hz span is not a new
acceptance threshold or a physically confirmed fundamental.

## Scheduling and resource boundary

CoroutineSwipeComparisonLoader receives the SAME serial dispatcher already shared
with YIN/component processing. It reads an immutable session ID and the fixed
supported instrument, independent of the live instrument selector. The existing
Pcm16WavReader.use and SWIPE equations remain. Cancellation is cooperative between
kernel calls, not a claim of immediate native interruption.

The existing recording-analysis adapter and the new comparison adapter share one
small submitProgressWork function. It owns scheduling/progress delivery only;
domain controllers decide publication authority. This removes duplicated callback/
cancellation plumbing without a generic domain state framework. Exception observers
remain outside the input-error catch. A worker CancellationException both reaches
the domain terminal result and leaves the coroutine Job cancelled, not completed
normally. Existing recording-analysis scheduling tests defend this shared change.

## Tests and mutation findings

Thirteen pure controller contracts, two queued-dispatcher comparison contracts and
one shared-job contract cover cached toggles, replacement, stale progress/results,
inline and duplicate terminals, cancellation, scheduling failure/retry, reentrant
selection, permanent disposal, cleanup and presentation bounds. Native tests add
real ViewModel disposal, session replacement, original/panned camera preservation,
unsupported instrument handling and cached re-enable. The original disposal RED
now passes, alongside the previous analysis ownership cases.

Actual focused PIT initially reports67 analysis mutants/54 killed and69 comparison
mutants/56 killed. New behavioral assertions kill reentrant-cancel publication,
worker cancellation semantics and the cancellation predicate. A survivor exposed a
test weakness: an assertion inside an already cancelled worker need not fail the
JUnit caller. The tests now capture the observation and unexpected coroutine
errors and assert them from the test thread. No production equation is changed to
kill mutants. Final focused reports: analysis67=57 killed/10 surviving; comparison
81=72 killed/nine surviving (including12/12 camera-binding mutants killed); zero uncovered/timeouts/tool errors. The scopes SHARE
the scheduler classes and must not be added together as independent mutants.
Remaining generated coroutine/Unit returns and completed-handle cleanup mutations
remain reported; no blanket equivalent classification or100% claim.

## Testable camera boundary, without a gate exception

The gate rejects the first ViewModel bridge: measured MCC4 but JVM-line coverage
zero gives CRAP-L20. Instead of accepting a new high-score exception, camera
reconciliation moves into an ordinary Kotlin UI-state function that consumes real
ExplorerState objects. Four JVM behavior tests prove panning preservation,
session replacement, original-camera restoration and no creation of retired
views. All12 actual camera mutants are killed. The ViewModel retains snapshot
publication and delegates this UI responsibility. No new framework or metric
threshold is introduced; JVM and native coverage stay separate.

## Native evidence and integration

The affected13-case local API37 run records11 passed/two failed: Pixel Launcher
ANR stole focus (captured PNG and dumpsys evidence). One bounded launcher recovery
and only the failed-case retry follows; the original report remains failed.
The viewer capture passes. The showcase then reveals an independent test-driver
problem at compact150%: after Analyze scrolls down, its horizontal tab action does
not restore the vertical row, so the screen stays in Spectrogram. A diagnostic PNG
confirms the actual selected visualization. The test now scrolls the row vertically,
selects Harmonics, asserts selection and scrolls the graph into view; it passes
without loosening visibility assertions. Actual synthetic-WAV analysis succeeds.
No clean local full-suite or unexecuted final CI pass is claimed. Broad final
results and source-scoped coverage are recorded only after observed integration.

Source: [owner](../../app/src/main/java/ar/didgetaller/core/SwipeComparisonController.kt),
[shared scheduling](../../app/src/main/java/ar/didgetaller/audio/ProgressWork.kt),
[camera binding](../../app/src/main/java/ar/didgetaller/ui/ComparisonCameras.kt),
[ViewModel regressions](../../app/src/androidTest/java/ar/didgetaller/AnalysisOwnershipTest.kt),
[controller contracts](../../app/src/test/java/ar/didgetaller/core/SwipeComparisonControllerTest.kt),
[cancellation observation](../../app/src/test/java/ar/didgetaller/core/ProgressWorkTest.kt).

## Current local integration

Final native ownership rerun on explicit synthetic emulator-5580/API37 passes
all six tests after camera extraction, including the public factory/disposal
regression, previous analysis ownership and current comparison camera contracts.
The emulator is restored to normal size/font/EN and stopped. Previous launcher
failures and the corrected showcase driver remain separately recorded.

Both final JVM variants:936 discovered,920 passed,16 protected skips,zero failed.
Local JaCoCo43.40% lines/39.59% branches; pure core99.02%/83.55%. Source inventory:
638 named declarations,484 measured/36 parent-accounted locals/32 abstract/
13 anonymous/73 Compose. Independent Detekt-unscored114 excludes four local
anonymous implementations. Compared with source8b73c21,17 additional named
functions comprise14 measured, two abstract contracts and one Compose notice;
no new eligible ordinary function escapes measurement. JVM above-target count72.

The old setSwipeComparison MCC15 becomes a two-decision admission/delegation;
controller methods peak at5, shared scheduler4, camera policy4 and ViewModel
publication1. New named eligible logic stays below target without an ordinary
risk exception.26 narrow constructor/initializer/accessor/Compose reviews cover
only genuine independently-unscorable constructs. No threshold, baseline or
mapping rule is relaxed. Final gate has zero violations; CPD0 >=100 tokens,
Detekt1987 informational, Lint0 errors/52 warnings/eight hints. Final separate
Android coverage and broad PIT await actual CI rather than reusing source8b data.

Metric limit: CRAP-L uses the matched carrier JVM method's line counter.
Coroutine lambda bodies are separate compiler classes, not an automatically
merged source-body coverage score. Their behavior is checked through the explicit
state-machine PIT targets and queued/native outcomes above. A low carrier score
alone does not establish worker coverage or correctness; no class-level coverage
is substituted for an unmapped source callable.
