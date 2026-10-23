# Session explorer

The explorer connects a saved recording to its pitch timeline, spectrogram, waveform, and harmonic spectrum. All processing is local. Use [Getting started](../README.md#getting-started) for installation and [Audio analysis](AUDIO_ANALYSIS.md) for formulas and processing parameters.

## Open and listen

Open **Library → Sessions / Biblioteca → Sesiones** and choose a recording. Play/Pause, Restart, and elapsed/total time sit with the session chart. File actions such as export and rename are separate. A session without reliable pitch still preserves its WAV.

Choose a chart, then open it full-screen for detailed exploration. Playback continues when changing charts or moving between detail and the full-screen viewer. Android Back returns to the session; leaving that session or backgrounding the app stops playback.

The cursor uses the player's reported position. Seeking and excerpt loops use MediaPlayer; they are not sample-accurate or guaranteed gapless. A cursor position between analysis windows does not represent an instantaneous spectrum.

## Choose a view

| View | Axes | What it shows |
| --- | --- | --- |
| Fundamental | Time / Hz, with secondary musical-note labels | Captured pitch estimates and their display trend |
| Spectrogram / Espectrograma | Time / Hz, linear or logarithmic | STFT power distribution, with relative dB colors |
| Waveform / Forma de onda | Time / normalized PCM amplitude | Actual amplitude extrema; individual samples at close zoom |
| Harmonics / Armónicos | Hz / relative dB | Averaged power spectrum, measured peaks, and candidate harmonic orders |

A spectrogram band, a pitch estimate, and a simulated bore resonance describe different properties. None automatically identifies a played technique.

## Navigate and select

In the full-screen viewer, pinch to zoom, drag to pan, long-press to inspect, and double-tap to reset. The tools panel provides range controls and button alternatives. Time-domain charts support position and excerpt selection; spectrum inspection uses frequency.

**Reset view / Restablecer vista** returns to the overview. **Show all / Mostrar todos** provides access to the full pitch range; the fitted view is useful for small variations. Manual exploration disables optional playback following; enable Follow again when you want the camera to track the cursor.

Detail and full-screen views retain independent cameras. Selection, excerpt, and loop belong to the session. Rotation retains this state through Activity recreation; process-death restoration of the camera is not guaranteed.

## Read Fundamental carefully

When a session has captured readings, they remain the chart's source even after WAV analysis. Recordings without that series may use analyzed readings. Analysis does not overwrite the original measurements or summary.

- **Green** combines high periodicity with small local pitch variation.
- **Yellow** indicates moderate variation, intermediate periodicity, or insufficient neighboring context.
- **Orange** indicates greater variation or weaker periodicity.
- **Gaps and isolated points** preserve missing or uncertain evidence.

These colors describe display quality from periodicity and local context; they are not calibrated probabilities. Short coherent interruptions may have dashed links between measured endpoints. Links are visual aids, not newly estimated samples. Clear absences and long intervals interrupt the trace.

The tools and cursor expose original uncertain readings and classification details. An absent frequency in an old session does not establish whether the input was silent or acoustically ambiguous: transient live diagnostics are not stored in session history. See [Acoustic uncertainty](ACOUSTIC_UNCERTAINTY.md).

## Interpret the spectrogram and waveform

The spectrogram uses a 32768-point STFT with a 16384-sample hop. At 44.1 kHz, that is about 743 ms per window, 372 ms per hop, and 1.346 Hz between bins. A larger display does not improve those underlying resolutions; short transitions can share a window.

Colors use a relative reference held during the current exploration, not calibrated sound pressure. The palette progresses from purple through blue and green to yellow as energy increases. Spectrogram green has no relationship to Fundamental's pitch-quality green. Frequency ranges and linear/logarithmic scaling are display choices, not changes to microphone filtering.

The waveform preserves minima and maxima when many samples share a display column. Close zoom exposes actual PCM samples. Normalized amplitude is not a calibrated loudness measurement.

## Analyze an interval

Use **Analyze recording / Analizar grabación** for the complete WAV, or choose an excerpt through Tools. Analysis is cancellable and reports the interval it covers. It produces a spectrum, candidate peaks/harmonic orders, pitch statistics, and energy bands.

Didgeridoo analysis uses the 35–220 Hz pitch range; Native American flute sessions use 200–1600 Hz and shorter pitch windows. Displaying higher spectral frequencies does not extend the pitch profile or validate a toot classification.

A mixed or short excerpt can have clear spectral peaks without a reliable representative note. **Hn?** indicates a harmonic candidate relative to the estimated fundamental; faint multiple guides are theoretical positions, not detected components. See [Harmonic candidates](HARMONICS.md).

## Compare sessions or excerpts

**Compare two sessions / Comparar dos sesiones** supports whole recordings or start/end times in seconds, including two excerpts of the same recording. The result overlays spectra and reports pitch, stability/range, duration, spectral centroid, energy bands, and candidates.

A is solid green; B is dashed orange. Optional normalization references each spectrum to its own peak. Without it, both use a shared digital reference. Neither mode measures absolute sound pressure. Microphone distance, playing level, room acoustics, and phone processing can affect comparisons; a spectral difference does not prove a material-specific sound or identify a vowel.

## Designs, markers, and Trash

Didgeridoo sessions can reference a saved design. Simulated input-impedance resonances and recorded results stay separate; their difference is exploratory. Flute sessions cannot be associated with didgeridoo designs. [Laboratory](../README-LAB.md) explains the model.

Possible-toot and vocalization markers are manual annotations. The app does not automatically classify toots, vowels, or playing quality.

Swipe left or use the item action to move a session/design to Trash. Undo and Trash restoration preserve access after the immediate snackbar expires or the app restarts. WAVs and relationships remain until explicit permanent deletion. Emptying Trash requires confirmation; no timer permanently deletes recordings.

## Storage and validation limits

Recording defaults to 15 minutes, configurable to 1/5/15/30. PCM is written progressively; the app can recover its own interrupted partial captures where complete written samples remain. This cannot recover unwritten audio or missing pitch history. See [WAV capture reliability](WAV_CAPTURE_RELIABILITY.md).

Analysis and visual tiles are derived, versioned, and regenerable. The cache is limited to 64 MiB and does not replace the original audio. Corrupt cache entries can be recomputed; reanalysis preserves historical summaries.

[Validation records](VALIDATION.md) distinguish synthetic tests, recorded-audio replay, emulator interaction, and physical checks. The physical “I” vocalization issue, independent tuning accuracy, audible synchronization, sustained recording/battery behavior, and full TalkBack traversal remain open areas. Historical reports and [viewer engineering notes](IMMERSIVE_VIEWER.md) retain detailed decisions and superseded presentation rules.
