# Harmonic candidates

Harmonic analysis uses the WAV's averaged power spectrum and a representative fundamental estimated from periodicity. When no reliable representative fundamental is available, peaks remain **unassigned**. The implementation neither infers F0 from the strongest FFT peak nor creates peaks at theoretical multiples.

## Implemented criteria

[`HarmonicAnalysis`](../app/src/main/java/ar/didgetaller/core/HarmonicAnalysis.kt) applies these rules:

1. Select interior local maxima above −50 dB relative to the maximum, with local prominence of at least a factor of four in power (approximately 6 dB) against valleys within six bins on either side.
2. Refine peak position with a parabola over three log-power bins. Sub-bin estimation is useful for isolated stable peaks; it cannot separate components unresolved by the window.
3. Find the nearest integer order to `peakHz/F0`. Accept it only when the frequency error is within:

   ```text
   min(0.20 × F0, max(0.75 × binHz, expectedHz × (2^(35/1200) − 1)))
   ```

   This caps the tolerance before it approaches a neighboring harmonic. It is not a guarantee of 35-cent measurement accuracy.
4. Retain up to 64 peaks by power, then order them by frequency. The initial analysis panel selects the eight strongest for display. Each peak provides measured Hz, relative dB, candidate order when available, and deviation `1200 × log2(measuredHz/expectedHz)` in cents.
5. Calculate **candidate-band energy fraction** as the power in the union of ±2-bin bands around assigned peaks divided by total spectral power. This includes noise inside those bands and excludes energy outside them. It is not THD or a validated HNR/tonality measure.

## Interpretation

A weak first harmonic can coexist with strong second/third harmonics. Their common periodicity may support an estimate even without a first-harmonic peak. Conversely, two peaks at 138.6 and 277.2 Hz do not establish a physical fundamental of 69.3 Hz: observable periodicity also supports 138.6 Hz. An existing regression prevents forcing that missing subharmonic.

Unrelated peaks remain unassigned. Mixtures, noise, attacks, sweeps, and vocalizations can make both periodicity and harmonic assignment ambiguous. **Hn?** labels are candidates; faint guides indicate theoretical multiples, not observations.

Radiated-sound harmonics, bore input-impedance resonances, and played registers/toots are different concepts. The app does not automatically identify voices, vowels, or toots. An averaged spectrum mixes moments; use the spectrogram, excerpts, and A/B comparison to inspect changes. There is no automatic per-partial stability tracker or musical-quality score.

[Audio analysis](AUDIO_ANALYSIS.md) defines normalization and windows. [Session explorer](SESSION_EXPLORER.md) explains interaction. The historical [acoustic validation protocol](ACOUSTIC_VALIDATION.md) keeps recorded-spectrum comparisons separate from bore-model validation.
