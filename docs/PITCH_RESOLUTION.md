# Pitch estimation: engineering decision

Android follow-up, 2026-10-23: the validated legacy SPTK implementation is now an
optional, on-device comparison curve for 44.1-kHz didgeridoo sessions. It does
not replace YIN or change the conclusions below. See [SWIPE′ comparison](SWIPE_COMPARISON.md)
for configuration, numerical parity, the unsafe upstream flute configuration,
resource limits and Android validation.

Evaluation date: 2026-10-23. **Decision: retain production YIN and add optional, read-only saved-WAV evidence inspection (outcome B).** None of the evaluated alternatives demonstrated a defensible improvement as the application's drone-pitch source. The physical drone frequency during the reported “I” vocalization remains unresolved.

## Measurement contract

Three quantities must remain separate:

- **Musical drone component:** a source-related pitch a musician wants to follow. A spectral family can support a hypothesis, but does not independently identify lip oscillation.
- **Whole-signal periodicity:** a repeating structure in the recorded mixture. A strong recurrence near 34 Hz can coexist with a spectral family near 67 Hz.
- **Missing or uncertain pitch:** no justified accepted estimate. Energy above a digital RMS threshold distinguishes this from a low-energy window; neither state is a calibrated sound-pressure measurement.

There is no rule that 70 Hz is always right or 35 Hz always wrong. Algorithms are not independent witnesses, and their agreement is not physical validation. No frequency averaging, automatic octave doubling, held-note substitution, or interpolation is used to resolve conflicting hypotheses.

## Previous evidence consolidated

The existing private investigations were reused rather than repeated:

| Approach | Established result | Decision |
| --- | --- | --- |
| Extend YIN's lower range | Additional strong lower periodicities, without identifying the drone source | Diagnostic evidence only |
| Normal-drone spectral/temporal analysis | A strong approximately 70-Hz family and additional components can favor a longer waveform recurrence | Preserve competing interpretations |
| Add a 182.221-Hz component to a known 69.567-Hz family | Production YIN can favor approximately 36 Hz | Component target and mixture periodicity require separate scoring |
| Low-pass before YIN | More real-audio coverage accompanied by low-note/octave and flute regressions | Reject a general prefilter |
| Frame-wise YIN-like + NSDF + harmonic comb | False consensus, boundary failures, and more known-F0 octave errors | Reject as a replacement |
| Harmonic-family tracking | Useful high-family observations, but competing periods and unsupported frames remain | No automatic drone assignment |
| Live acoustic diagnostics | Separates low energy from rejected pitch without changing accepted values | Preserve; do not infer it from historical null points |

The dated records remain intact in [VALIDATION.md](VALIDATION.md) and the ignored local experiment area. This report is the current decision record, not a revision of historical results.

## Authentic implementations executed

1. **Production YIN:** current `PitchStream → PitchDetector → LivePitch`, compiled directly from unchanged production sources. Didgeridoo uses its initial 35–140 Hz profile, 16384 input samples and 8192-sample cadence. Flute uses 200–1600 Hz and 8192 samples with 4096-sample cadence. Actual profile acceptance is retained, including flute's stricter gate.
2. **pYIN:** actual `librosa.pyin` from librosa 0.11.0, with probabilistic candidates and Viterbi decoding, not the earlier Kotlin YIN-like reference. Configuration: `center=False`, 0.1-semitone resolution, maximum transition rate 8 octaves/s, otherwise library defaults. Its probabilities are not interchangeable with production YIN confidence. [API and paper references](https://librosa.org/doc/0.11.0/generated/librosa.pyin.html).
3. **SWIPE′:** the unmodified C implementation bundled with the pysptk 1.0.1 source distribution and its SPTK FFT/vector support. It was called directly through its native API, with the documented 0.3 strength threshold. Source archive SHA-256 was checked against PyPI; the only compiler compatibility macro prevents a duplicate C99 `round` fallback. This is not the latest SPTK C++ implementation or an approximate harmonic comb. [API](https://pysptk.readthedocs.io/en/latest/generated/pysptk.sptk.swipe.html), [source distribution](https://pypi.org/project/pysptk/1.0.1/).
4. **Author causal SWIPE reference:** unmodified `groupmm/real_time_swipe`, commit `b5b0776674a2c8bb0068171cb9f0b15359972c0a`, as a distinct comparison. It uses prime-harmonic kernels and multiresolution spectra; it is not presented as identical to SPTK. [Repository and paper](https://github.com/groupmm/real_time_swipe).

The C implementation retains its MIT/SPTK notices; the author reference is MIT. All third-party sources, compiled tools and Python dependencies remain outside the Android build in ignored local storage. No GPL/AGPL source or neural runtime was incorporated into the application.

[PESTO](https://github.com/SonyCSLParis/pesto) was inspected but not executed: its repository is LGPL-3.0 and its implementation requires PyTorch/torchaudio, absent here. Its loader combines CQT preprocessing with a one-dimensional residual network. The inspected base configuration starts at A0 (27.5 Hz, with a bin-centering adjustment), so low pitch is not ruled out by range alone; model-specific range/export behavior remains unvalidated. The available model files are small; the substantial uninstalled runtime and unvalidated Android deployment, rather than model-file size, made installation unjustified for this task. No claim about its accuracy or device performance follows.

## Evaluation design and alignment

The frozen protocol contains 51 deterministic synthetic clips, split by whole conditions into development and held-out groups. It covers clean 35/70/140-Hz references, other low tones, weak/missing first harmonics, different envelopes, genuine lower fundamentals with strong upper components, alternating-cycle structures, varied interference levels/phases, flute references through 1600 Hz, pauses, abrupt changes, sub-gate audio, and seeded white/colored noise.

Known waveform-F0 windows are scored separately from mixtures containing a known target oscillator. The latter have no assigned single waveform-F0 reference in this comparison. Transitional windows spanning a change are excluded from steady-state accuracy and examined separately. Overlapping windows and related conditions are dependent observations, not independent statistical trials.

The author SWIPE threshold was selected from 0.1/0.2/0.3/0.4/0.5 using development data only, with a frozen objective penalizing false positives and octave errors; the chosen value was 0.4. Selection was written before evaluating held-out clips or private WAVs. SPTK uses its published default; pYIN uses the declared fixed configuration. No rules were tuned to private timestamps or after held-out scoring.

All methods receive the same original PCM16 source, normalized exactly where their API requires it, with the same original RMS gate of 0.005 applied to acceptance. Raw candidate outputs remain available separately. pYIN nominal windows match production input windows exactly. All results are aligned to the same nominal window centers, not confused with saved callback-delivery timestamps.

| Profile | Production / pYIN nominal input | SPTK maximum spectral window | Author causal SWIPE maximum window |
| --- | --- | --- | --- |
| Didgeridoo | 16384 samples, 371.5 ms | 8192 samples, 185.8 ms | 16384 samples, 371.5 ms |
| Flute | 8192 samples, 185.8 ms | 2048 samples, 46.4 ms | 2048 samples, 46.4 ms |

Internal comparison support differs even when nominal windows match. The selected centers coincide with the SPTK analysis grid; centered spectral support extends on either side of them. The author reference uses `delay=1` for a common center across its resolutions, with original past samples preloaded into its unchanged buffer operation. pYIN decodes each entire clip with future information: its CPU time or a stable output must not be advertised as causal real-time latency. Production decisions arrive at the original capture cadence; no microphone latency benchmark was performed.

Three original private WAVs were read with manifest-verified hashes: 652 complete production-cadence windows in total. Only one recording has confirmed conditions: normal drone at 3–8 s and “I” at 9–17 s. Statistics use windows wholly inside those intervals: 25 and 42 respectively. Other sections/recordings are held-out acoustic observations, not ground-truth F0 labels.

## Quantitative results

For held-out **known waveform F0**, every method has the same 260 reference windows. Correct means within 50 cents. An out-of-tolerance estimate exceeds 50 cents; gross relative error below uses a separate 20% criterion. Abstentions remain in the full denominator.

| Method | Correct / all | Accepted / all | Correct / accepted | Errors >50 cents | Gross relative error >20% | One-octave errors | Abstentions | Noise/pause false positives / 35 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Production YIN | 250/260 | 250/260 | 100% | 0 | 0 | 0 | 10 | 0/35 |
| pYIN | 238/260 | 250/260 | 95.2% | 12 | 12 | 12 | 10 | 10/35 |
| SPTK SWIPE′ | 219/260 | 248/260 | 88.3% | 29 | 9 | 0 | 12 | 0/35 |
| Author causal SWIPE | 250/260 | 250/260 | 100% | 0 | 0 | 0 | 10 | 0/35 |

Production and the causal reference tie at 96.15% correct over all held-out reference windows; this is not evidence of superiority. Ten sub-gate references deliberately abstain. On development data, production and the causal reference each get 170/190 correct, but with different failures: production misidentifies a weak-first-harmonic 70-Hz case; both spectral implementations fail the genuine 35-Hz H2/H5 condition. SPTK also has low-boundary bias and returns 35 Hz on a pure 140-Hz control: this is a **two-octave error**, not concealed by the one-octave column. Machine-readable results include all integer-octave errors.

On the separate interference target, production, SPTK and the author reference each recover the known 69.567-Hz component in only 20/60 held-out windows; pYIN gets 0/60. These are component-target results, not claims about the complete mixture's fundamental. No proposed combination was justified by them.

### Real audio: coverage, not correctness

| Method | Normal drone accepted / 25 | Accepted range | I accepted / 42 | Accepted range |
| --- | --- | --- | --- | --- |
| Production YIN | 21/25 | 35.25–62.42 Hz; 20 estimates near 35–37 | 14/42 | 61.45–64.86 Hz |
| pYIN | 22/25 | 35.41–36.66 Hz | 9/42 | 61.65–66.07 Hz |
| SPTK SWIPE′ | 25/25 | 63.84–68.56 Hz | 41/42 | 62.53–66.49 Hz |
| Author causal SWIPE | 4/25 | 65.18–65.77 Hz | 36/42 | 62.48–66.43 Hz |

SPTK's apparent continuity is attractive, but it does not overcome its controlled-reference failures or establish the physical drone pitch. pYIN stabilizes the existing lower regime rather than solving the drone/source distinction. On the other recordings, production/pYIN/SPTK accept 163/179/158 of 256 windows and 129/129/193 of 275 windows; no accuracy is assigned to these differences.

All methods reject the two fully contained low-energy breathing-control windows. The 42 fully contained I windows are energetic, so production rejections there are not silence. The new inspector reconstructs this distinction from WAV samples on demand, without inventing historical diagnostic fields.

### Changes and performance

Production follows genuine held-out note changes. In the held-out 440→880-Hz flute sequence, pYIN never obtains two consecutive correct 880-Hz estimates before the next pause; it retains the lower register. Production and both spectral implementations reach the new target at the first common center 93 ms after the boundary, with the production-sized window fully available at 186 ms. These are aligned sequence observations, not hardware latency measurements.

Desktop timing: production median 0.53 ms per decision (p95 1.14 ms); warm pYIN median 2.24 ms per frame amortized over a clip, with a separate 13.8-s initial warm-up; SPTK median 22.4 ms per frame amortized over short clips including initialization; author reference median 0.45 ms per call excluding kernel initialization. Those different cost definitions are retained in detailed timing files and do not establish Android performance.

## Application change: optional evidence inspection

Open a saved session, choose a time through its selection controls, then expand **Signal evidence at selection / Evidencia de señal en la selección** and request inspection. Cursor/playback movement does not schedule work. The result explicitly identifies the actual WAV window and remains there until requested again.

The bounded, read-only inspector reuses existing production code to show:

- The current YIN profile's freshly accepted periodicity, or a separately labeled rejected candidate.
- A distinct, accepted lower-search periodicity when the didgeridoo-only 25-Hz search supports one. This is diagnostic evidence, not octave correction; flute never runs this extra search.
- Up to three observed three-peak harmonic-family candidates, requiring measured lowest/H2/H3 peaks within existing spectral resolution/tolerance. No missing first harmonic is invented and no source is assigned to the drone.
- RMS, YIN periodicity, and low-energy/energy-present/insufficient-input state.

Markers occupy separate lanes with no connections or note labels. The inspector uses the profile's initial range, not an inferred historical capture setting. Its fresh PCM-window result must not be mistaken for a sample-exact reconstruction of a stored callback reading. Coincidental peak alignments occurred in colored-noise controls; the UI explicitly warns that noise can align peaks. The spectral display rule is descriptive, not a pitch policy validated on held-out data or another validated drone classifier.

No estimator replacement, live threshold/range change, saved-data migration, analysis-cache modification, Fundamental/PitchTrace change, WAV write, or new Android dependency was introduced. Background work is bounded to one window, closes its reader, and is cancelled on another request, closing the panel, leaving/deleting the session, or backgrounding. Existing playback and camera ownership remain intact.

The integrated inspector was replayed on all 1305 comparison windows. Its accepted values and confidence match the unchanged production decision on each exact inspected PCM window. Desktop median request cost was 2.01 ms (p95 3.87 ms), with approximately 446 KB allocated per request and only the compact result retained. This is suitable evidence for an optional bounded offline operation, not proof of low-end-device real-time suitability.

## Validation, reproducibility, and remaining uncertainty

Deterministic tests cover known 35/70/140-Hz references, flute, competing components, missing fundamentals, energetic noise, pauses, changed notes, bounds, cancellation, and an opt-in manifest-verified real WAV. Emulator cases cover explicit requests, both languages, text layout, stale-request cancellation, preserved original JSON/WAV, and playback/navigation regressions. Exact commands and actual counts belong in [VALIDATION.md](VALIDATION.md).

Private protocol, seeds, complete scripts, upstream hashes, frozen environment, raw CSVs, timing data and standalone PNG/SVG plots remain in the ignored local experiment directory. They are not public assets or APK fixtures. No private audio was uploaded or modified. The small synthetic set cannot establish broad accuracy, and no independently measured moment-by-moment lip/drone reference exists for the private recordings.

The decision is complete for this task: **retain the estimator and expose competing evidence on demand**. An independently referenced drone/source recording and a candidate that survives genuine low-note, transition, negative-control and flute validation would be required before replacing it. The new visualization does not resolve the physical I problem or promise a continuous correct Fundamental curve.
