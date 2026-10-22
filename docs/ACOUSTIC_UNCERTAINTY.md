# Acoustic uncertainty diagnostics

## Data flow

`PitchDetector` keeps its existing YIN algorithm, ranges, thresholds and accepted Hz.
`PitchStream` attaches the existing observer's `Diagnostic` to the returned `Reading`.
This optional metadata is separate from `Reading.hz`; rejected candidates never replace
zero Hz. Direct detector callers remain compatible with a null diagnostic by default.
`MicrophoneEngine` forwards the same reading through its existing guarded callback.

`LivePitch` remains the sole source of live/stored accepted Hz, including the stricter
flute gate. The ViewModel records a compact diagnostic sample after that decision:

| State | Evidence | Interpretation |
|---|---|---|
| No relevant signal | Detector reports `low_amplitude` | Below the existing RMS gate; not proof of absolute silence |
| Accepted pitch | LivePitch returns stored Hz | Accepted by the current profile; not a calibrated probability or independent acoustic validation |
| Uncertain pitch | Other observations without stored Hz | Signal/analysis does not justify a measured fundamental |

Each sample contains callback observation time, instrument, RMS, YIN periodicity,
the selected raw candidate, detector reason and state. A detector-accepted candidate
rejected by LivePitch is identified as `profile_gate`. Short/insufficient input stays
uncertain, rather than being declared silence. Missing diagnostic metadata clears the
latest diagnostic display; it is not synthesized or inferred from a historical null Hz.

## Retention and compatibility

- Only the latest 128 diagnostic samples are retained in memory, roughly 24 seconds
  for nominal didgeridoo callbacks or 12 seconds for flute. The bound is by count;
  timestamps describe callback observation, not exact PCM analysis positions.
- The history stores scalar metadata, not PCM, Reading waveforms, spectra or full
  minima lists. The current Reading still has its bounded single-window diagnostic.
- A new microphone start or instrument selection clears history. Stopping keeps
  the bounded evidence available until another start, selection or ViewModel disposal.
- No diagnostic sidecar, database field, JSON key, WAV change or automatic session
  rewrite is introduced. Previously saved sessions have no such evidence and cannot
  be classified as silence versus uncertainty from missing Hz alone.
- SessionAnalysis points, summaries, LivePitch and PitchTrace are unchanged. Diagnostic
  candidates do not enter charts, musical note labels, saved measurements or links.

## Interface

Measure reuses its existing three-way status, with a discreet amber uncertainty label
and explanatory text that preserves the chart gap. Normal view offers a collapsed
Signal diagnostics button. Expanded details show the last observed state, RMS,
periodicity, a candidate explicitly marked diagnostic-only, a localized rejection
reason and the retained frame count/limit. These are ordinary localized Text/Button
semantics in English and Spanish. The last-frame label remains explicit after stopping;
this is not a saved measurement or a historical Session chart annotation.

## Verification and limits

Synthetic regressions cover silence, a known periodic drone, energetic noise, unchanged
detector outputs, flute rejection, fixed retention/eviction and independent snapshots.
The opt-in `RealAcousticDiagnosticsTest` checks six windows of one manifest-verified
private WAV against pre-change golden outputs, retaining rejected evidence while
preserving accepted lower-register values. Enable only that test with
`DIDGE_TALLER_FIXTURES` pointing to the existing private fixture directory. No private
audio is committed or included in the APK; the test verifies its hash before and after.

This change does not resolve the physical vocalization I. RMS measures recorded energy,
and YIN periodicity is not certainty about which acoustic source or period is the drone.
There is no second estimator, frequency correction, threshold relaxation or gap filling.
Focused emulator tests additionally verify all three states through the real reading
callback, localized candidate/reason labels, no promotion to notes or PitchTrace, a
byte-identical existing synthetic session index/WAV, retention after stop and reset on
profile change. Native English/Spanish screenshots were inspected. This is controlled
PCM injection, not microphone hardware or audible TalkBack validation. No full layout
matrix, physical capture/durability or sustained battery/performance test is claimed.
See VALIDATION.md and WORK_LOG.md for actual commands and results.
