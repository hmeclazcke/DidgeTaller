# Optional SWIPE′ comparison

## Use it on Android

Open **Library → Sessions → a didgeridoo session → Fundamental** and enable
**Compare pitch estimators**. The original YIN curve remains visible. A dashed
purple SWIPE′ curve appears after local WAV analysis; its legend explicitly says
**experimental**. The same curves are available in the full-screen viewer, where
**Tools** also contains the switch. In Spanish, use **Biblioteca → Sesiones →
Fundamental → Comparar estimadores de tono**.

Playback, inspection, zoom and pan use the chart's existing time/frequency
projection. Inspection reports the nearest SWIPE′ sample within half a hop,
without interpolating a frequency. Disable comparison to restore the original
camera and chart. The existing **Signal evidence at selection** inspector remains
independent and available.

The feature needs no network, Python, desktop software or model downloads.
It supports 44,100-Hz didgeridoo WAVs, including existing and new app recordings.
Flute sessions keep their original YIN analysis; SWIPE′ is intentionally unavailable
there for the upstream safety reason below. Unsupported WAVs fail safely rather
than being resampled or assigned another instrument's range.

## Implementation and provenance

The JNI library compiles the same legacy SPTK SWIPE′ C implementation used in the
[engineering evaluation](PITCH_RESOLUTION.md), from the
[pysptk 1.0.1 source distribution](https://pypi.org/project/pysptk/1.0.1/).
This is the spectral/prime-harmonic SWIPE′ implementation, not a new harmonic-peak
detector. No estimator has been substituted for production YIN.

The [production-consolidation review](PITCH_RESOLUTION.md#production-consolidation-review)
retains that decision: the didgeridoo-only held-out references do not justify
replacing YIN. A documented upstream boundary-selection defect has been corrected;
it is not an octave filter.

Seven upstream files are byte-identical. `swipe.c` has one documented correction:
an upper-boundary strength maximum now returns that boundary candidate instead
of the minimum frequency. Current hashes and the original patched-file hash are in
[PROVENANCE.json](../app/src/main/cpp/vendor/sptk/PROVENANCE.json).
The SWIPE/vector MIT and SPTK FFT/allocation BSD-style notices are retained in
the source and in the APK's [third-party notice](../app/src/main/assets/third_party/SWIPE.txt).
The current [SPTK repository](https://github.com/sp-nitech/SPTK) has evolved;
do not silently replace this legacy implementation with current main or assume
the entire project has the same license as this component.

Only the build needs NDK 27.0.12077973 and CMake 3.22.1. The stripped native library
is approximately 24–39 kB per ABI, with no Python or C++ runtime dependency.
The minimal SPTK header declares the functions used here. Compiler allocation
wrappers serialize the upstream FFT globals, reclaim all per-call memory and
enforce a 32-MiB native allocation budget, including allocation headers. Native
entry points reject unsupported ranges/rates before executing upstream code.

### Deliberate restriction to didgeridoo

The original C implementation excludes the Nyquist bin from the arrays used
for cubic interpolation. At 44.1 kHz and 200–1600 Hz, its last ERB query is about
22,028 Hz, beyond the last available frequency in its smallest FFT (21,878 Hz).
`bilookv` can return the array length and `splinv` then accesses outside the array.
Repeated Android calls on identical flute input produced approximately 426.9 or
439.4 Hz. Those results cannot serve as a numerical reference.

This integration does not silently patch that algorithm or expose the unsafe
configuration. The fixed 35–140-Hz didgeridoo configuration has a last ERB query
near 21,821 Hz and a last smallest-FFT bin near 22,028 Hz. Flute comparison and other
sample rates remain unsupported. The flute production detector is unchanged.

## Analysis contract and resource bounds

- Search: 35–140 Hz, original strength threshold 0.3. No octave correction,
  frequency averaging, held-note substitution or learned model.
- Output hop: 8,192 samples (185.76 ms). The first center is source frame 12,288
  (278.64 ms); the reference input starts at frame 4,096. Only complete nominal
  16,384-sample windows are returned.
- SWIPE′ uses centered multiresolution windows, with a largest FFT of 8,192
  samples. The nominal window is used for a digital RMS ≥0.005 presence gate,
  matching the offline evaluation. This is not a calibrated acoustic threshold.
- Native calls return eight core estimates with surrounding real context. Block
  starts align to the reference's hop/FFT grid; overlaps preserve the neighboring
  spectral frames. The implementation is offline and uses lookahead, not a live
  or zero-latency estimator.
- Missing estimates remain missing. Only adjacent accepted samples can connect;
  changes of at least 600 cents break the connector but retain both estimates.
  There is no bridge across missing samples. Pauses shorter than the analysis
  support cannot be resolved as instantaneous silence.
- A single background worker checks cancellation between bounded native blocks.
  Disabling the switch hides the overlay immediately; the current native block
  finishes before cancellation releases its buffers. At most 262,144 PCM samples
  enter one native call, with at most 20,000 retained output points. App recordings
  up to 30 minutes fit this budget; oversized input is rejected before processing.
- Only the current session's completed curve is cached in memory. Leaving the
  session, deleting it or backgrounding the app cancels and clears the comparison.
  Detail/full-screen transitions and configuration recreation retain it.
- No SWIPE′ result is written to the session index, WAV, historical timeline or
  analysis cache. Original YIN timestamps/decisions remain untouched. Captured YIN
  timestamps represent delivery times, whereas SWIPE′ timestamps are WAV window
  centers; equal chart coordinates do not imply identical acquisition latency.

The comparison uses the initial didgeridoo search range. Historical sessions do
not persist whether capture had been extended to 220 Hz; this feature does not
infer that setting or claim to compare identical detector bandwidths in that case.

## Numerical and Android validation

On the protected 19:17 WAV, Android's 121 aligned outputs had the same acceptance
decisions and a maximum observed frequency difference of **0 Hz** from the prior
independently compiled Windows SPTK library (test tolerance: 0.01 Hz).
The common RMS presence gate is applied to both references.

| Fully contained nominal windows | Production YIN accepted | Android SWIPE′ accepted | SWIPE′ accepted range |
| --- | ---: | ---: | --- |
| Confirmed normal drone, 3–8 s | 21/25 | 25/25 | 63.84–68.56 Hz |
| Confirmed “I”, 9–17 s | 14/42 | 41/42 | 62.53–66.49 Hz |

These are coverage and hypothesis measurements, not physical pitch accuracy.
The real recording has no independent moment-by-moment lip-oscillator reference.
SWIPE′ still makes known synthetic errors: pure 35 Hz can yield about 37.08 Hz,
and a missing-first-harmonic 35-Hz example can yield about 82 Hz. The original
pure-140-Hz → 35-Hz boundary bug now yields 138.99 Hz. The golden tests retain
the remaining failures and explicitly test the corrected boundary.
The prior held-out comparison favored production YIN overall; this overlay does
not resolve the physical drone-frequency problem.

See [VALIDATION.md](VALIDATION.md) for measured emulator timings, exact commands,
test outcomes, screenshots and limitations. No Samsung operations were performed.

## Reproduce

Run the normal JVM/build tasks in [Development](DEVELOPMENT.md). On an explicitly
selected emulator, run `ar.didgetaller.core.SwipeNativeTest` and
`ar.didgetaller.ui.SwipeComparisonUiTest` with AndroidJUnitRunner. The private-fixture
test skips unless `swipeFixture` and `swipeReference` instrumentation arguments
point to a protected emulator copy and its local numerical TSV. Never bundle
private WAVs or reference output into either APK or Git.

The public [synthetic golden TSV](../app/src/androidTest/assets/swipe-reference.tsv)
was generated with the independent legacy library using:

```powershell
python tools/generate_swipe_reference.py <local-sptk-library> <output.tsv>
```

[The generator](../tools/generate_swipe_reference.py) specifies the deterministic
PCM, ranges and alignment. It uses only Python's standard library and loads the
reference library locally. No downloads or app runtime Python are involved.
Private results and prior reference scripts remain in the ignored experiment
areas `checkpoints/experiments/swipe-android/` and
`checkpoints/experiments/pitch-resolution/`.
