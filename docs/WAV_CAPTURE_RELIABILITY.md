# WAV capture reliability characterization — 2026-10-22

Scope: seven focused tests, production unchanged from `486aa5e`. Synthetic PCM
and test-owned temporary files only. No new libraries or changes to DSP, UI,
formats, persistence implementation or private recordings. Tests characterize
current behavior before a separately authorized fix/refactoring.

## Lifecycle investigation

| Scenario | Confirmed implementation | Evidence / remaining gap |
| --- | --- | --- |
| Normal stop | ViewModel freezes timeline and enters SAVING; MicrophoneEngine.stop clears active/audio and unblocks the worker. Worker finally detaches the writer, closes it, posts stopped and hands off the file. RecordingStore validates, renames, then indexes; main callback returns to IDLE. | Writer close and store import are tested separately. No new end-to-end microphone test. Stop is asynchronous, without joining the worker. |
| Append failure | A free-space rejection precedes writing and sample-count updates. A worker exception enters finally, which attempts finalization. | Rejection, unchanged bytes/count, and successful retry are tested. Actual partial OS write failure is not injected. |
| Initialization failure | Writer rejects existing content, invalid rate and insufficient space, closes its handle in catch, and rethrows. Engine reports initialization failure; ViewModel stops capture when startRecording returns false. | Writer input preservation/reuse tested; actual AudioRecord permission/device-init failure remains untested. File reuse alone does not prove absence of leaked handles. |
| Close failure | Writer marks itself closed before header/sync and closes the handle in finally; a repeated close does not retry finalization. Engine reports the close error but still invokes recording-ready. | Confirmed by inspection. Physical sync/close failure not forced; stale-header import defect reproduced below. |
| Repeated stop | ViewModel ignores finishSession outside CAPTURING; engine clears its recording flag under a lock before closing. Repeated writer close is a no-op. | Exact WAV bytes and rejection of append after close are tested. Repeated concurrent engine stop is not exercised. |
| Late callback | Queued pitch delivery checks both active and AudioRecord identity at delivery. Other completion/error callbacks do not all have that delivery-time guard. | Source inspection only. No claim that an old callback has corrupted another session. |
| Interruption | Activity.onStop requests stop/save. Process death cannot run callbacks; next ViewModel creation calls recoverPending on owned UUID .part files using actual complete-frame length. | A real pre-close disk snapshot, with a torn final sample, is repaired and read exactly. Existing Android recovery test also passes. This does not simulate power loss or OS durability. |

## Added tests

`WavFinalizationTest` (four JVM tests):

1. Normal finalization, repeated close, append rejection after close, unchanged
   exact WAV bytes and decoded signed PCM values.
2. Existing audio is not overwritten on initialization failure; invalid-rate and
   no-space failures leave empty files usable by a fresh writer.
3. Low-space rejection leaves accepted bytes and sample count unchanged; a retry
   after space returns adds only the accepted new PCM, with correct final header.
4. Snapshot taken before close/checkpoint, plus one torn trailing byte: reader
   rejects it initially, repair preserves complete PCM frames, repeated repair
   changes no bytes, and the original normally finalized file remains intact.

`CapturePersistenceTest` (three emulator tests with real Android files/preferences):

1. Destination collision and repeated recovery preserve both captures, original
   index, timeline and instrument. No existing WAV is overwritten.
2. A zero-data stale header is rejected without changing the partial/index; after
   explicit repair, import succeeds once and a duplicate attempt leaves one
   unchanged session with no invented analysis.
3. Characterizes the confirmed stale-checkpoint import limitation below. Its
   assertions describe the defect, not a desired contract for a future fix.

The new Android tests check ranchu/goldfish before setup and use per-test UUID
cache directories and preference names. Cleanup removes only those owned fixtures.
The existing LocalPersistenceTest suite uses its established isolated namespace.
No reflection, fake Android objects, timing sleeps or AudioRecord input are used.

## Confirmed defect: stale checkpoint accepted as finalized capture

Reproduction: `CapturePersistenceTest.staleCheckpointImportExposesOnlyHeaderFramesDespitePreservedTail`.

1. Write 8,000 mono PCM16 frames at 8 kHz; this checkpoints a one-second header.
2. Append 4,000 more frames, below the next checkpoint; copy the actual on-disk
   bytes before close into a test-owned UUID .part. The copy has 12,000 frames
   (1.5 seconds, 24,044 bytes), but its header still declares 8,000 frames.
3. Call RecordingStore.importCapture on the copy. It succeeds and renames it .wav.
4. Both the returned/reloaded session and Pcm16WavReader expose only 1,000 ms /
   8,000 frames. All original bytes survive, including the extra 0.5 seconds.
5. recoverPending returns zero because that capture is no longer a .part.

This is a reproduced import-boundary defect: a known writer snapshot can be
accepted with a shorter accessible duration. It is **not** evidence of deleted
PCM or a reproduced physical-device close failure. The inspected engine handoff
after close failure could expose this state, but that causal hardware path remains
untested. Normal startup recovery repairs .part files before promoting them and
is a separate path.

Proposed separate minimal fix: validate the complete-frame length against the
declared length for **owned fixed-format capture partials before rename/index**;
reject an inconsistent partial through the existing save-error path and retain it
for recovery. Add a desired-behavior regression then update this characterization.
Do not tighten the general WAV reader's rules for historical audio, discard tail
bytes, or silently repair arbitrary WAV files. No fix is implemented here.

## Validation and coverage

- Focused JVM run: four tests passed on the first run, against unchanged production.
  These are characterization tests; no artificial failing test or production fix.
- Full JVM suite with JaCoCo: **131 passed, one opt-in private replay skipped**
  (132 discovered / 19 suites), zero failures/errors.
- Emulator API 37 / hardware ranchu, explicit serial emulator-5554:
  **11 passed** = three new tests plus all eight existing LocalPersistenceTest cases.
- Main APK and instrumented APK built. CI now compiles the instrumented APK;
  it does not run an emulator. Existing CI JVM/build/coverage/Detekt steps retained.
- Full unrelated UI instrumentation/private-fixture suites were not executed.
  No private fixture inputs were copied/read, Samsung operations performed,
  unqualified connected-test task used, data cleared or emulator userdata wiped.

| JVM metric | Before | After |
| --- | ---: | ---: |
| ProgressiveWavWriter.kt line | 96.55% | 100.00% |
| ProgressiveWavWriter.kt branch | 65.00% | 82.50% |
| ProgressiveWavWriter.kt instruction | 83.83% | 90.66% |
| Overall line / branch | 27.59% / 27.65% | 27.62% / 27.82% |

Android capture and persistence remain **0% in the JVM report**. The emulator
tests demonstrate persistence behavior but were not instrumented for coverage;
no numerical Android coverage increase is claimed. Raw I/O faults, concurrent
stop and callback ordering still need an appropriate test seam or Android scenario
in a separate task. Existing tests do not establish physical/acoustic correctness.

Reproduce JVM/build checks from repository root:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests ar.didgetaller.core.WavFinalizationTest
.\gradlew.bat :app:jacocoDebugUnitTestReport :app:assembleDebug :app:assembleDebugAndroidTest
python tools/coverage_summary.py
```

For emulator persistence, first verify `adb -s emulator-5554 shell getprop ro.hardware`
is ranchu/goldfish and boot is complete. Install both existing build outputs with
explicit `adb -s emulator-5554 install -r`, then run:

```text
adb -s emulator-5554 shell am instrument -w -r -e class ar.didgetaller.data.CapturePersistenceTest,ar.didgetaller.data.LocalPersistenceTest ar.didgetaller.test/androidx.test.runner.AndroidJUnitRunner
```

Exact logs, build results and CI evidence are recorded in VALIDATION.md; decisions
and the unresolved defect are recorded in WORK_LOG.md. Preserve subsequent work
before recovering the pre-test state at `486aa5e`.
