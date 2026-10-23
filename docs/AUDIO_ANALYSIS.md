# Audio analysis

This document defines the recorded-audio pipeline and its numerical interpretation. For interaction, use the [Session explorer guide](SESSION_EXPLORER.md); for rejected live candidates, see [Acoustic uncertainty](ACOUSTIC_UNCERTAINTY.md).

## Pipeline and ownership

```text
WAV → Pcm16WavReader → WavAnalysis
                         ├─ SpectralAnalysis → Radix2Fft → averaged power spectrum
                         ├─ PitchDetector → SessionAnalysis → pitch statistics
                         └─ HarmonicAnalysis → measured peaks and candidates
```

[`WavAnalysis`](../app/src/main/java/ar/didgetaller/core/WavAnalysis.kt) accepts a `MonoPcmSource`, a half-open frame interval `[startFrame, endFrame)`, settings, cancellation, and progress callbacks. Result times refer to window centers on the original WAV timeline. A window estimate is not an instantaneous measurement.

Analysis runs outside the UI thread. Cancellation is cooperative between windows; the caller discards obsolete results. Progress is reported as integer percentages. Reanalysis and caches do not overwrite the WAV, captured series, or historical summary.

The live spectrum is a separate normalized visualization. It must not be interpreted as the recorded-analysis PSD or as calibrated dB.

## WAV reading and bounded work

[`Pcm16WavReader`](../app/src/main/java/ar/didgetaller/core/Pcm16WavReader.kt) reads RIFF/WAVE little-endian PCM16, mono or stereo, at the file's actual sample rate from 8–192 kHz. Stereo is averaged to mono; opposite-phase channels can cancel. There is no independent left/right analysis or general audio-format importer implied by this reader.

The reader locates `fmt ` and `data`, respects unknown chunks and odd-byte padding, and validates lengths, alignment, bits, channels, and byte rate. Unsupported encodings, empty/truncated data, and invalid structures are rejected. The reader is read-only; it does not repair an original WAV.

Block access avoids loading the whole recording. Analysis retains a mean PSD and compact per-window readings, not a full spectrogram matrix. The default spectral budget is 10000 windows; exceeding it reports an error instead of silently truncating. The flute pitch pass has a separate bound of four times that budget.

Recovery of the app's own interrupted captures is a separate write path. Its behavior and failure limits are documented in [WAV capture reliability](WAV_CAPTURE_RELIABILITY.md).

## Spectrum, resolution, and units

Defaults are a **32768-point FFT**, periodic Hann window, **16384-sample hop** (50% overlap), and mean removal per window. At 44.1 kHz this gives approximately 743 ms per window, 372 ms per hop, and **1.3458 Hz between bins**. Different WAV sample rates change those quantities. Bin spacing is not guaranteed frequency accuracy or the minimum separation of resolvable tones.

For PCM normalized to digital full scale:

```text
PSD[k] = side[k] × |FFT(Hann × (x − mean))|² / (fs × sum(Hann²))
```

`side[k]` is 2 for interior bins and 1 for DC/Nyquist. Units are **digital full-scale²/Hz**. Summing PSD and multiplying by `fs/N` estimates mean digital signal power after DC removal. Linear periodograms are averaged before conversion to dB.

One final partial window covers the tail. It uses a Hann window over available samples, zero-padding to N, and weight `availableSamples/N`. Its effective resolution is lower. This explicit weighted-tail variant is not a claim of exact equivalence with another library's Welch implementation.

Displayed spectrum levels use `10 log10(PSD[k] / maxPSD)`: the maximum is 0 dB **relative**. They are not dB SPL, absolute microphone calibration, or an automatic basis for comparing recording loudness. Peak/centroid extraction treats a maximum PSD of at most `1e-16` as having no significant energy; silence/DC therefore does not create a meaningful 0 dB peak.

Derived metrics include:

- **Power-weighted spectral centroid** in Hz. It differs from a magnitude-weighted centroid.
- Power fractions in `[0,100)`, `[100,250)`, `[250,500)`, `[500,1000)`, `[1000,2000)`, `[2000,5000)`, and the remainder up to Nyquist, clipped to the available frequency domain.
- Measured peaks, candidate harmonic orders, and candidate-band energy fraction, defined in [Harmonic candidates](HARMONICS.md).

These bands are not detected formants or notes.

## Instrument-specific pitch analysis

[`PitchDetector`](../app/src/main/java/ar/didgetaller/core/PitchDetector.kt) uses a YIN-based difference function, cumulative mean normalized difference, lag selection, and local interpolation. The input RMS gate is 0.005 in normalized digital amplitude. A candidate must meet the detector's periodicity/range criteria; WAV analysis additionally requires periodicity confidence of at least 0.80.

| Profile | App analysis range | Pitch windows |
| --- | --- | --- |
| Didgeridoo | 35–220 Hz | Uses the spectral-window pass; the detector averages groups of four input samples for its low-register path |
| Native American flute | 200–1600 Hz | Separate 8192-sample pitch pass, half-window hop, full-rate detector path |

The flute pass preserves more melodic timing detail without changing the spectrum's FFT resolution. Its profile includes a small endpoint guard for interpolation. The low-register averaging path is not a dedicated anti-alias FIR filter.

`WavAnalysis.Settings` defaults to didgeridoo. The app's `AnalysisCache` supplies the saved session instrument and its range. Generic settings must also satisfy `maxPitchHz < sampleRate/4`; arbitrary settings or successful synthetic tones do not establish validated support for another instrument.

Live capture has its own profile window/cadence and acceptance decisions. Its saved point times are callback-delivery times; reanalysis uses WAV-window centers. These timelines are not interchangeable sample-exact observations.

## Summaries and missing estimates

`SessionAnalysis.summarize` requires at least three usable readings grouped in the same rounded semitone, with a strict majority of usable readings in that group. It reports the group's median, indexed 10th/90th percentile range, and percentage within ±25 cents of that median.

WAV analysis also reports the median and arithmetic mean of all valid windows, valid-window fraction, and median periodicity confidence. These answer different questions from the dominant-note summary. A short or mixed excerpt may contain real spectral peaks without supporting a representative note.

Fundamental uses captured readings when available, even after reanalysis; recordings without a captured series may use analyzed readings. `PitchTrace` classifies display evidence separately from the estimator. Missing estimates remain missing, uncertain originals remain inspectable, and visual links do not generate intermediate samples.

## Spectrogram and derived caches

`AudioVisuals` computes the viewport STFT with the same default FFT size and hop. Windows are anchored to the original WAV and contribute power according to temporal support. Display cells do not represent additional measurements, and finer pixels do not resolve content beyond the analysis window.

The spectrogram holds a relative color reference during an exploration. It is not calibrated SPL or a guaranteed global maximum for the whole recording. Waveform rendering preserves min/max extrema, showing original samples at close zoom.

The derived cache uses SHA-256, interval/parameters, and versions; instrument is part of recorded-analysis identity. Algorithm data and visual tiles have separate versions. The 64 MiB cache is regenerable, and a cache write failure does not invalidate a successful analysis.

## Evidence and limits

The implementation is Kotlin code using standard numerical methods, with no bundled DidgeLab, CADSD, or SciPy engine. Known-signal tests, an independent test DFT, and recorded-audio replays are documented in [VALIDATION.md](VALIDATION.md). Those checks establish their specific digital-processing behavior, not scientific physical accuracy or universal phone performance.

Microphone filtering, room acoustics, playing technique, and simultaneous voice/drone can change observable periodicity. The “I” vocalization remains unresolved; neither an expected note nor a spectral band justifies forcing a fundamental. No vowel/toot classifier, calibrated sound level, or validated physical bore prediction is implied.

For numerical reference, the historical implementation study cites the [YIN paper](https://pubmed.ncbi.nlm.nih.gov/12002874/), [Welch documentation](https://docs.scipy.org/doc/scipy/reference/generated/scipy.signal.welch.html), and [WAVE format reference](https://www.mmsp.ece.mcgill.ca/Documents/AudioFormats/WAVE/WAVE.html). These are background references, not claims that external tools were run in this documentation update.
