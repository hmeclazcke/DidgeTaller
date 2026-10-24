# Pitch estimation: engineering decision

## Four-bout failure reproduction and correction

A subsequent 39.96-s private recording was reproduced from `f428628` through the
actual production stream and optional Android SWIPE′ path. It contains the
musician-confirmed normal / I / normal / I sequence. The missing Fundamental
segments are primarily detector abstentions: 84 of 214 stream readings are accepted,
79 fail periodicity, and 51 fail the full-window amplitude gate. PitchTrace preserves
all 84 accepted samples; no additional sample exclusion explains the long gaps.

| Acoustic bout | Eligible complete windows | YIN accepted | SWIPE′ accepted | Median YIN / SWIPE′ Hz | Agreement ≤50 cents, both accepted |
| --- | ---: | ---: | ---: | --- | --- |
| Normal 1 | 28 | 27 | 28 | 71.130 / 71.211 | 26/27 |
| I 1 | 28 | 12 | 28 | 72.283 / 68.439 | 1/12 |
| Normal 2 | 42 | 30 | 42 | 62.876 / 63.758 | 26/30 |
| I 2 | 46 | 9 | 46 | 71.812 / 66.311 | 2/9 |

These are coverage and agreement, not physical accuracy. Estimator-blind stationary
cores retain the same conclusion. The second normal body differs acoustically:
the prominent grave component decreases from approximately 71.3 to 65.3 Hz, its
second family from 142.7 to 131.2 Hz, and a component around 182.4 Hz becomes
dominant. This supports a change in both frequency content and harmonic balance;
it does not independently identify the lip oscillator or prove the cause of every
YIN decision. Both vocalizations retain competing evidence. No fusion, automatic
octave correction, held pitch or new estimator is introduced.

One reproducible acceptance defect **is corrected**. The low-register YIN
difference function compares only a leading portion of the PCM window. Previously,
energy arriving in its unexamined tail could pass the full-window RMS gate and
validate a candidate calculated on nearly silent input. The same existing 0.005
gate now also checks the union of samples used by the selected integer lag.
Raw candidate, minima and periodicity are preserved; diagnostics add comparison
RMS and `low_comparison_amplitude`. Full-rate flute decisions remain unchanged.

In this reproduction, the correction rejects one late 51.436-Hz candidate whose
comparison RMS is 0.000502 despite full RMS 0.006598. Across the five older WAVs it
rejects nine similarly unsupported acceptances. It does not remove two energetic
35.7-Hz alternatives during I2 or a 59.3-Hz release candidate. All previously frozen
synthetic outputs remain identical, including existing errors and abstentions;
this is not a claim that octave accuracy has generally improved.

Independent SciPy periodograms reproduce the Android STFT raster to Float
precision (maximum relative discrepancy below 6e−8 above the −70-dB floor).
The four blocks and changing spectral bands are present in the PCM. STFT,
normalization, axes and original waveform bytes are unchanged.

Saved Fundamental now shows a separate source-time digital audio-level strip,
so continuous sound can remain visible when no pitch is accepted. It is not
confidence, a pitch curve or an acoustic source classifier. It is read on a worker,
cancelled with session ownership, retained only in memory and never persisted as
a measurement. Historical estimates are not silently corrected or replaced.
The optional SWIPE′ comparison and selected-window evidence remain available.

Detailed private traces, exact boundaries, per-window minima, before/after plots
and reproducible commands are retained locally under
`checkpoints/experiments/reproduction-1527/REPORT.md`. The supplied package lacks
the phone's saved timeline, so exact callback-to-PCM timing cannot be certified
from the screenshots. See [validation](VALIDATION.md) for executed tests,
Android performance, visual review and remaining limits.

Android follow-up, 2026-10-23: the validated legacy SPTK implementation is now an
optional, on-device comparison curve for 44.1-kHz didgeridoo sessions. It does
not replace YIN or change the conclusions below. See [SWIPE′ comparison](SWIPE_COMPARISON.md)
for configuration, numerical parity, the unsafe upstream flute configuration,
resource limits and Android validation.

Evaluation date: 2026-10-23. **Decision: retain production YIN and add optional, read-only saved-WAV evidence inspection (outcome B).** None of the evaluated alternatives demonstrated a defensible improvement as the application's drone-pitch source. The physical drone frequency during the reported “I” vocalization remains unresolved.

## Stable normal-drone agreement study

An estimator-blind, aligned real-audio comparison at source baseline `170bcf9`
answers a narrower question than the accuracy benchmarks below: **the methods
agree closely in two sequence-supported initial normal-drone bodies, but disagree
persistently inside the confirmed normal body of 19:17.** They are not consistently
equivalent during steady normal playing.

Sixteen protected local backup archives and loose fixtures contain 18 unique
recording contents after hash deduplication. Thirteen have the Didgeridoo profile
and were analyzed; five have the flute profile and were excluded. The October 23
10:17 and 10:18 recordings were not found in those locations. Unannotated recordings
were not silently labeled normal or vocalized from their pitch outputs.

Selection used only musician annotations, RMS and spectral stationarity, before
executing the estimators: 8192-sample Hann STFT, 50-ms steps, 20–1200-Hz descriptors,
local ±7-frame neighborhoods with RMS ≥0.005, level span ≤6 dB, spectral cosine
similarity ≥0.94 and flatness <0.12; candidate runs ≥1.2 s. Entire nominal
16384-sample comparison windows must fit inside the selected cores. The first
normal body in 19:04/19:26 has sequence-annotation support; only 19:17 has the
explicit 3–8-s normal label. No new phonetic listening validation is claimed.

| Recording, October 21 | Selected normal core, seconds | Eligible | Both accepted | YIN median Hz | SWIPE′ median Hz | Agreement ≤50 cents |
| --- | --- | ---: | ---: | ---: | ---: | ---: |
| 19:04 | 2.96788–8.66788 | 29 | 29 | 70.87 | 70.89 | 29/29 |
| 19:17 | 3.00000–4.46788 | 6 | 3 | 35.68 | 65.92 | 0/3 |
| 19:17 | 5.01788–6.71788 | 7 | 7 | 36.04 | 66.01 | 0/7 |
| 19:26 | 2.21788–5.01788 | 13 | 13 | 67.72 | 67.88 | 13/13 |

The four normal cores contain **55 eligible windows**: 52 both accepted, three
SWIPE′-only, no YIN-only and no both-rejected. Agreement among the 52 pairs is
**41/52 (78.85%) within 25 cents**, **42/52 (80.77%) within 50 cents**. The pooled
percentage must not hide the recording-level difference. Only two pairs fall
within ±100 cents of an octave; three within ±150 cents. Many lower/upper-register
disagreements are not exact octave relationships and are not proven octave errors.

For the entire confirmed 19:17 normal annotation, without the stationary mask,
agreement is **0/21 accepted pairs out of 25 eligible windows**. Eighteen consecutive
pairs disagree at centers 4.55111–7.70902 s; the stricter middle core retains seven
consecutive disagreements at centers 5.29415–6.40871 s. This is not solely an
attack/release or isolated-point effect.

The previously reported **5.294149660 s is a window end**. Its center is
**5.108390023 s**, over source PCM 4.922630385–5.294149660 s. At that aligned
position, YIN returns **36.496871 Hz / periodicity 0.715757** and SWIPE′
**65.833369 Hz / strength 0.391067**, a difference of **1021.26 cents**. Their
quality measures are different quantities. The previously audited 69.57/139.20/
208.06-Hz family and strong 182.22-Hz component support competing acoustic
interpretations; neither the higher spectral hypothesis nor the lower waveform
recurrence establishes the physical lip-driven drone frequency.

Confirmed “I”, 9–17 s, remains separate: 42 eligible windows, YIN accepted 14,
SWIPE′ 41, both accepted 14, SWIPE′-only 27, both rejected one. All nominal windows
exceed the digital presence gates. Agreement is 1/14 within 25 cents and 7/14
within 50 cents; accepted medians are 61.86 and 64.95 Hz. There are no near-octave
disagreements there, but seven same-register differences exceed 50 cents. More
accepted frames do not establish greater physical accuracy.

Across the complete inventory, 32 acoustically stationary candidates in nine
recordings contain 513 eligible windows and 418 accepted pairs, with 398/418
(95.22%) within 50 cents. Most lack technique labels and some include vocalization;
this exploratory percentage is **not** the primary normal-drone result.

The replay uses actual current Kotlin production classes and the current SPTK C
kernel through the saved-WAV adapter, at unchanged ranges/thresholds. Internal
supports differ despite common nominal centers; no instantaneous or physical
equivalence is assumed. An offline read-only strength observer leaves 2162 native
outputs bit-identical to the control library. All 1571 complete windows, including
rejections, are retained locally. No production code, detector selection, fusion,
filter, session format or original audio was changed. These disagreements alone
do not justify automatic estimator fusion or averaging conflicting frequencies.

The private report, exact interval inventory, CSV, plots, frozen protocol and
reproduction scripts remain under `checkpoints/experiments/stable-drone-comparison/`.
See its `REPORT.md`, `classified.csv`, `summary.json`, `validation.json` and
`*-comparison.png`; only aggregate findings are published here.

## Production consolidation review

Reviewed on 2026-10-23 from clean, published `dbed749`, the user's manual commit.
**Decision: do not promote the current SWIPE′ implementation to the normal
didgeridoo detector.** Physical-phone feedback is useful user evidence, but the
available controlled references still fail the previously defined acceptance bar.
This decision does not claim that YIN identifies the physical drone correctly.

The actual microphone path remains `AudioRecord → PitchStream → PitchDetector
(YIN) → LivePitch → stored points → PitchTrace`. Saved-WAV reanalysis also uses
YIN; Fundamental prefers original captured readings when available. SWIPE′ runs
only after explicitly enabling its saved-WAV comparison. There is no simultaneous
normal-path SWIPE′/YIN pair to eliminate and no silent fallback to another method.
The optional signal inspector and regression tests retain direct YIN diagnostics.
The Native American flute's detector/range are unchanged. No live SWIPE′ latency
or capture-thread safety is inferred from an offline comparison benchmark.

Two reproducible defects were corrected:

1. **Native boundary candidate:** upstream `pitch()` returned `pc[0]` for a maximum
   at either search boundary. It now returns `pc[maxi]`. The known 140-Hz reference
   changes from 35 Hz to 138.99 Hz, removing ten two-octave errors in the existing
   development reference windows. This retains the selected hypothesis; it does
   not double low estimates. Seven vendor files remain byte-identical, with the
   eighth file's original/current hashes and exact correction recorded in provenance.
2. **Chart connector:** didgeridoo changes of at least 600 cents now break the
   connector regardless of YIN periodicity. Both measured endpoints/groups remain
   visible and inspectable. Confidence is not evidence of a continuous sweep
   through the intervening frequencies. No new sample filter, low-note cutoff,
   interpolation, held pitch or event-duration rule was added. Flute connectors
   retain their existing policy.

Only the existing SPTK candidate was rerun, using its original frozen configuration
and dataset splits. The independent desktop build and Android JNI agree on the
corrected synthetic boundary. On **didgeridoo-only held-out known waveform F0**:

| Method | Correct / all | Accepted / all | Correct / accepted | Errors >50 cents | Integer-octave errors |
| --- | ---: | ---: | ---: | ---: | ---: |
| Production YIN | 125/135 | 125/135 | 100% | 0 | 0 |
| SWIPE′ before boundary fix | 96/135 | 125/135 | 76.8% | 29 | 0 |
| SWIPE′ after boundary fix | 96/135 | 125/135 | 76.8% | 29 | 0 |

Thus, the boundary correction fixes a real defect but does not establish general
superiority. Errors remain on pure 40/73.4-Hz controls and weak/missing-harmonic
conditions. No thresholds were selected from these held-out cases. The unsafe
flute SWIPE′ configuration is excluded from this decision.
Both methods retain ten abstentions and zero false positives in the 31 held-out
didgeridoo noise/pause windows. Both recover the known 69.567-Hz component in
20/60 held-out interference windows; this separate component target is not the
fundamental of the complete mixture. An additional local interpolation-arithmetic
check reached only 105/135 correct reference windows and was not integrated.

All 652 complete aligned SWIPE′ windows across the three October 21 WAVs are
unchanged. The actual production-stream replay, including two partial-tail
decisions, preserves all 654 original candidate/acceptance/display/stored fields
and classifications. It removes no additional links on those recordings.
Normal 3–8 s and “I” 9–17 s keep the coverage reported below. Increased coverage
still does not prove the intended physical oscillator frequency.

The October 21 replay contains three accepted YIN and four accepted SWIPE′
hypotheses in 45–55 Hz before chart projection. Some have substantial RMS, others
are close to the presence gate. They are estimator outputs, not invented chart
coordinates. SWIPE′ already applies its native strength threshold; that score is
not calibrated physical-pitch confidence and is not interchangeable with YIN
periodicity. Their physical validity cannot be decided from level or brevity.
The October 23 10:17/10:18 files were not found in the existing local fixtures or
experiment inventory; those specific points, attacks/releases and 115-Hz events
were **not tested**. No causal claim about dividing a 345-Hz component is made.

Commands, before/after results, Android performance, native/UI checks and private
artifact locations are recorded in [VALIDATION.md](VALIDATION.md). Original WAVs,
session formats, names, associations and settings are unchanged.

## Additional private recordings: aligned comparison follow-up

The two previously unavailable October 23 recordings were subsequently supplied
and analyzed at `32b97fb`, with unchanged production sources from `170bcf9`.
The earlier absence statements describe the earlier investigation. This extension
reuses its frozen selection thresholds, actual Kotlin/C implementations, cadence,
acceptance policies and metric functions; it does not repeat the historical dataset.
Private filenames, input hashes, source PCM and plots remain local. Here **A** is
the single-drone clip (11.88 s), **B** the two-vocalization clip (43.56 s); both are
mono PCM16 at 44100 Hz.

The musician's playing-condition sequence is combined with estimator-blind
RMS/STFT selection. These are audio-defined stationary cores, not independently
verified physical-F0 labels. Entire nominal windows must fit inside each core.

| Clip / condition | Core, seconds | Eligible | Both accepted | SWIPE′ only | YIN / SWIPE′ median Hz | Agreement ≤25 / ≤50 cents |
| --- | --- | ---: | ---: | ---: | --- | --- |
| A normal | 2.31788–9.51788 | 37 | 37 | 0 | 70.251 / 70.254 | 37/37 / 37/37 |
| B normal 1 | 2.96788–6.71788 | 18 | 18 | 0 | 69.598 / 69.591 | 18/18 / 18/18 |
| B normal 2 | 24.01788–27.01788 | 14 | 1 | 13 | 35.000* / 65.952 | 0/1 / 0/1 |
| B I 1 | 10.36788–16.26788; 16.31788–17.71788 | 35 | 35 | 0 | 85.179 / 68.008 | 14/35 / 14/35 |
| B I 2 | 33.26788–37.61788 | 22 | 22 | 0 | 114.997 / 68.346 | 10/22 / 10/22 |

There are no YIN-only or both-rejected observations in these cores. *Only one
YIN value is accepted in B normal 2; its median does not describe a persistent
35-Hz curve.* Twelve of its raw candidates are near 66 Hz and two at 35 Hz.
Thirteen fail periodicity acceptance despite substantial signal energy. The one
accepted 35-Hz observation is centered at 24.79891 s. Thus problematic middle-drone
behavior is not unique to the earlier recording, although its sustained accepted
35–37-Hz regime is not reproduced continuously here.

The new normal cores agree in **55/56 matched pairs (98.21%)**, with **69 eligible
windows** and 13 SWIPE′-only observations. Across all five annotated/sequence-supported
recordings, the normal-core totals become **124 eligible, 108 both accepted,
16 SWIPE′-only**, no YIN-only/neither: **96/108 (88.89%) within 25 cents** and
**97/108 (89.81%) within 50 cents**. Per-recording results above remain essential:
the high matched-pair percentage does not erase YIN's missing second normal body.

Both I cores have complete acceptance but substantial disagreement. First I:
YIN has 14 observations near 68 Hz, four near 85 Hz and 17 near 114 Hz, so its
85.179-Hz median is a multimodal summary, not a sustained note. Second I: ten
near 68 Hz and twelve near 115–116 Hz. SWIPE′ stays near 68 Hz. These accepted
conflicts persist for seconds inside the selected cores, not only at boundaries.
Actual YIN diagnostics reproduce its first-CMNDF-dip choice even when a later
68-Hz minimum is deeper. Spectra retain low components near 66–67 Hz while
341–345-Hz peaks dominate. Neither this nor division of an upper peak proves
the physical source frequency or a causal explanation for 115 Hz.

Coverage remains conditional: over the complete energetic I bouts, YIN accepts
44/44 and 34/35, versus SWIPE′ 41/44 and 32/35. These observations do not establish
general SWIPE′ superiority or override the earlier known-F0 regressions.

A separate onset diagnostic in A finds YIN 50.450 Hz at center 1.95048 s. Its
full-window RMS passes at 0.05951, but the leading comparison-support union has
RMS 0.000257; the later attack supplies the energy. This identifies a testable
support-consistency concern, distinct from energetic middle-drone ambiguity.
The attack's descending estimates and release discrepancies remain visible.
Both estimators reject all 82 low-energy non-boundary windows across A/B; weak
unclassified events and boundary overlaps are retained separately.

**Decision: preserve separate hypotheses; do not fuse automatically.** A rejection-only
fallback would miss accepted 85/115-Hz conflicts, and averaging creates unsupported
pitches. Source-support consistency is a concrete regression target, but this
analytical task changes no estimator, gate, chart, detector selection or stored data.
The physical drone/source distinction remains unresolved without an independent
reference. The reproducible private extension is under
`checkpoints/experiments/stable-drone-comparison/oct23/` (`REPORT.md`, aligned and
classified CSVs, summary, three comparison plots and validation record).

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
