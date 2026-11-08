# Laboratory simulation operation ownership

Status: implemented and locally verified; observe the final branch CI before
accepting its remote validation. Whole-application reconstruction remains IN PROGRESS.
Main, persisted formats and AcousticModel.evaluate are unchanged.

## Reproduced defect

TallerViewModel.calculate previously submitted work to a dedicated executor and
accepted callbacks when their integer version still matched. onCleared shut down
the executor but did not invalidate that version. Interrupting arithmetic does
not guarantee it stops, and a posted completion can already be queued.

A small loader seam retained that acceptance rule. An actual Android test created
the ViewModel, cleared its ViewModelStore, then delivered controlled success and
failure callbacks on Main. The late failure changed the disposed owner's message
to a simulation error. API 37 red run: one passed, one failed for that assertion.
This is a deterministic ViewModel reproduction, not a claim of a historical
Samsung failure. The test now also checks success immediately before delivering
failure so the latter cannot conceal a transient publication.

## Design

* SimulationController owns Empty, Working, Ready, Failed and Disposed snapshots.
  Working can retain the previous completed model; failure clears that model,
  preserving existing visible behavior. Illegal Ready-with-null states are not
  expressible through its constructor.
* Each operation owns its task. Replacement invalidates the old operation before
  requesting cancellation; completion claims ownership once. Late and duplicate
  outcomes cannot publish. Disposed is permanent and close is idempotent.
* SimulationLoader explicitly distinguishes scheduling rejection from terminal
  results. A callback observer error is not silently turned into a model error.
* CoroutineSimulationLoader uses the ViewModel's scope and an explicitly serial
  view of Dispatchers.Default. Noncooperative arithmetic can finish, but its result
  cannot regain publication authority. Cancelling queued requests prevents their
  evaluation. The former dedicated simulation executor is removed.
* TallerViewModel projects that same immutable snapshot into Compose. simulation
  and simBusy are derived from it. The public Application constructor remains
  available to AndroidViewModelFactory; a separate internal constructor supplies
  controlled test dependencies.

The remaining two generic exception catches are at scheduling/evaluation result
boundaries; cancellation is rethrown, and observer code is outside the evaluation
catch. The three early returns defend ownership around reentrant cancellation and
publication. These Detekt observations remain visible, without suppressions.
No physical acoustic calibration, CPU/FPS improvement or full ViewModel rebuild
is claimed. Association, comparison and saved-analysis orchestration still need
their next reconstruction cycles.

## Tests and mutation evidence

Nine controller and seven scheduler JVM contracts cover replacement, disposal,
duplicate results, inline callbacks, submission rejection, observer failures,
real coroutine turns, queued cancellation and actual default model wiring. The
default evaluator test uses a cylindrical model and an approximate quarter-wave
expectation; it does not validate a physical didgeridoo.

Four native tests pass on the new, empty API 37 emulator: late callbacks after
disposal, latest-shape behavior, current failure/retry and the real AndroidViewModelFactory/default
scheduler path. The original Application constructor and prior-result display
are protected. No WAV is required by these tests.

Selected free PIT: 36 generated, 31 killed, five survived, zero uncovered/errors/
timeouts (86.11%, denominator 36). Additional tests killed a reentrant-disposal
guard mutant and the previously uncovered default evaluator reference. Remaining
survivors concern an unreachable old task handle, ignored Unit returns and
generated coroutine resume guards. These are retained and reported, not declared
formally equivalent or removed merely to force 100%.

The complete local API 37 suite first reported 122 passed, 21 failed and nine
protected/configuration skips. A captured system window explicitly showed Pixel
Launcher ANR stealing focus. After restarting only that fresh emulator launcher
and disabling animations, all 21 affected cases passed in one targeted retry.
The original failed run is preserved; this is not advertised as one clean full
pass. Mandatory API 35 CI remains a separate final gate.

## Reliable Kotlin correspondence and compilation

Returning Result<SimulationTask> produces submit-gIAlu-s on the JVM. A simple
name/descriptor lookup cannot safely guess that correspondence. The engineering
tool now uses the already pinned Kotlin compiler's metadata reader plus ASM to
obtain exact declared source names, JVM names and descriptors. It never loads or
executes application classes. Class SHA-256 fingerprints reject stale reports;
owner, source-body location and argument-count ambiguity checks remain mandatory.
An actual compiler fixture covers Result overloads, value classes and @JvmName.
Compose, suspend, inline, local and anonymous limitations are not silently erased.

The gate also requires this report and cross-checks its names. Synthetic CLI tests
show missing/stale/mismatched reports fail. The explicit bytecode inventory command
rejects noncanonical compilation rather than overwriting valid evidence.

On this AGP/Kotlin combination, incremental and non-incremental compilation emitted
different generated branch denominators for unchanged Compose source. For example,
ImmersiveViewer had 702 versus 546 missed branches; canonical compilation restored
the fresh-CI denominator. Overall candidate JVM branch coverage changed from
34.53% to 36.33% without a behavior change. The precise compiler emission cause is
not claimed resolved. Quality commands/workflows now explicitly use
-Pkotlin.incremental=false; ordinary development builds remain incremental.
No difficult production class is excluded to improve a percentage.

## Reproduce

```powershell
.\gradlew.bat '-Pkotlin.incremental=false' :app:test
.\gradlew.bat '-Pkotlin.incremental=false' :app:qualityMutationSimulation
.\gradlew.bat '-Pkotlin.incremental=false' :app:jacocoDebugUnitTestReport detekt detektComplexity :engineering-tools:test :engineering-tools:sourceInventory :engineering-tools:bytecodeInventory
python tools/detekt_summary.py
python tools/coverage_summary.py
python tools/quality_gate.py
```

Native tests must use an explicitly selected clean emulator. Full commands and
report scopes are in the [quality guide](../quality/QUALITY_GUIDE.md). Local logs,
the red/green native outputs and ANR evidence remain ignored under checkpoints/workspace.
