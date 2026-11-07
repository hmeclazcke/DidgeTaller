# Capture events and recording workflow

This continuation starts at rebuild `9cd18f6`, with original main `3a160043f9ebb3a420b1d89a9cf4ddfe7d1b5d67` retained as the reference. The whole-application rebuild remains **IN PROGRESS**.

## Reproduced defect

The previous worker-owned writer prevented capture A from closing B's file. Its callbacks still had no capture identity. `onRecordingDiscarded` and `onRecordingFailed` unconditionally changed the ViewModel's global listening/phase/error fields. A delayed discard therefore stopped the *displayed* monitor B, even though B owned a different worker. Ready callbacks also obtained profile, timestamps and pending points from mutable ViewModel fields at delivery time.

`RecordingHandoffTest.delayedDiscardCannotStopAReplacementMonitor` failed on the owned API 37 emulator before the correction: **Discard A must not stop monitor B**, one executed test/one failure. It uses a real writer and Handler, controlled worker finalization and isolated synthetic storage; it never opens physical microphone hardware.

## Responsibilities and invariants

- [RecordingCapture](../../app/src/main/java/ar/didgetaller/core/RecordingCapture.kt) owns one writer and one opaque `CaptureId`. `complete` claims a terminal envelope under its lock, then finalizes only its own writer outside that lock. A monitoring worker produces one envelope with no recording outcome; a duplicate produces **no envelope**. This avoids treating a duplicate as an empty terminal arriving ahead of the real close result. The existing `finish` API delegates to this claim and retains its return contract.
- [MicrophoneEngine](../../app/src/main/java/ar/didgetaller/audio/MicrophoneEngine.kt) performs device/file work. It posts one typed terminal envelope on the main Handler instead of separate untagged stopped/ready/error callbacks. Previously fixed per-worker reading, error and resource ownership remain in force.
- [CaptureHandoff](../../app/src/main/java/ar/didgetaller/core/CaptureHandoff.kt) distinguishes current transport ownership, recording snapshot ownership and claimed publication. A stopped recording's snapshot survives a replacement monitor. Terminal delivery and publication acknowledgements are idempotent. A replacement retains notice ownership even after stopping. Starting a new attempt retires older notices even when the device fails to open; a separate JVM red/green regression protects that error from a late saved notice. Engine construction attempts likewise retire queued stream errors before touching the device.
- [RecordingCoordinator](../../app/src/main/java/ar/didgetaller/core/RecordingCoordinator.kt) owns recording phases, listening state, chronology updates and terminal/publication policy. It is pure Kotlin; injected publication and observer ports provide effects. Its immutable `CapturedSession` freezes the original profile, wall timestamp and measured points. Disposal waits for pending publication/cleanup, suppresses obsolete notices and prevents a delayed permission result from reopening hardware.
- [TallerViewModel](../../app/src/main/java/ar/didgetaller/TallerViewModel.kt) adapts the coordinator to Compose, localized messages, the microphone and the existing recording store/executor. One observer writes the recording UI snapshot. Disk publication still runs on the existing serial executor; acknowledgements return to main. A successful older save refreshes the library while a replacement monitor keeps its listening state and notice.

This is a main-thread workflow boundary, not a thread-safe general event bus. Real worker finalization remains synchronized by `RecordingCapture`. The publication port must acknowledge on the same UI owner thread. The UI continues to disallow a second recording while a save/discard is pending; passive monitoring can restart. No queue of additional recordings or background recording policy was introduced.

## Behavioral verification

The new JVM contracts exercise late success/failure/discard/cancellation, replacement monitoring, duplicate terminals/acknowledgements, immutable publication data, missing registrations, observer delivery, duration-driven completion, disposal and delayed permission callbacks. Existing WAV byte and persistence assertions remain intact. Android handoff cases add delayed failure and success alongside the original discard reproduction; success must import once with original profile/points/bytes and preserve B's status. Existing controlled-PCM UI fixtures now register their worker identity through the production workflow instead of setting a phase without an owner.

PIT selects the actual `CaptureHandoff` and `RecordingCoordinator` bytecode through `qualityMutationHandoff`, also included in the expanded domain selection. An initial run exposed missing observer-delivery assertions, an unregistered-ready boundary and a disposal assertion that could fail for the wrong reason; the tests were strengthened. Equivalent/dependent mutations remain explicitly distinguishable from killed mutants in the checkpoint. This selected scope is not whole-application mutation coverage.

## Compatibility and limits

Persisted session schemas, identifiers, WAV encoding, acoustic thresholds and canonical MusicalTrace semantics are unchanged. Failed finalization retains the partial for the existing recovery path; a claimed discard never becomes permission to import it. Tests use synthetic files and assert original bytes/index preservation. Main, Samsung and private recordings are untouched.

The old callbacks are deliberately corrected, so equality with the old race is not a goal. Existing deterministic differential DSP/WAV/geometry contracts remain the numerical oracle. Android tests establish framework behavior only on their reported emulator configuration; no physical timeout, microphone accuracy or universal device compatibility is claimed. The broader ViewModel, SessionWorkspace and PlotSurface reconstruction is unfinished.


The first API 35 CI execution at de8df92 exercised 147 cases and failed one disposed-notice expectation (136 passed/10 skipped). The corrected contract keeps all data-integrity checks, requires nonblank failure feedback for live owners, and requires no feedback after clearing, including an explicitly injected queued device error. Microphone errors now enter RecordingCoordinator's lifecycle boundary; a failed start clears stale listening independently of error-callback side effects. These corrected inputs require a new full emulator run; the failed run is not claimed as a pass.
