# Architecture

Didge Taller is a native Android application with one Gradle module, `app`. Kotlin/JVM code handles signal processing and models; Android adapters handle audio, local files, and presentation. There is no backend or Internet permission.

## Responsibility map

Paths below are relative to [app/src/main/java/ar/didgetaller](../app/src/main/java/ar/didgetaller).

| Area | Main components | Responsibility |
| --- | --- | --- |
| Entry and coordination | `MainActivity`, `TallerViewModel` | Permissions, lifecycle, document export, capture/session state, background work |
| Signal processing | `core/PitchDetector`, `PitchStream`, `LivePitch`, `SessionAnalysis` | YIN-based estimates, window cadence, live acceptance/display decisions, summaries |
| Recorded audio | `core/Pcm16WavReader`, `WavAnalysis`, `SpectralAnalysis`, `HarmonicAnalysis`, `Radix2Fft` | Block reading, interval analysis, power spectra, harmonic candidates |
| Chart data and mathematics | `core/AudioVisuals`, `PitchTrace`, `PlotViewport`, `PitchAxis`, `SpectralPalette` | STFT, waveform envelopes, pitch classification/links, coordinates, scales and colors |
| Design models | `core/AcousticModel`, `BoreOptimizer` | Approximate input impedance and evolutionary bore search |
| Android audio | `audio/MicrophoneEngine`, `SessionPlayer`, `SessionWorkspace` | AudioRecord, MediaPlayer, cancellable viewport work |
| Storage | `data/LocalStore`, `RecordingStore`, `SessionToolsStore`, `TrashStore`, `AnalysisCache` | Local JSON/preferences, WAV index, associations/markers, recoverable deletion, derived caches |
| Presentation | `ui/TallerApp`, `TallerNavigation`, `SessionExplorer`, `ImmersiveViewer`, `AcousticCharts`, `ComparisonScreen` | Compose screens, navigation, interactive plots, comparison |

The ViewModel coordinates these components; the directory boundaries describe responsibilities rather than independently packaged modules.

## Capture and pitch decisions

```text
AudioRecord → MicrophoneEngine
                ├─ ProgressiveWavWriter → owned .part → finalized WAV
                └─ PitchStream → PitchDetector → LivePitch
                                                   ├─ live display
                                                   └─ captured points → session summary/index

Captured points → PitchTrace → Library preview / detail / full-screen Fundamental
```

Capture starts explicitly. PCM16 mono at 44.1 kHz is written progressively; the app does not retain the whole recording in RAM. Stopping, reaching the configured limit, leaving Measure, or backgrounding the app finalizes the capture. Recovery examines only the app's own interrupted partial files. It does not repair historical WAVs or reconstruct missing pitch readings.

`InstrumentProfile` defines didgeridoo and Native American flute ranges/windows. The detector estimates periodicity; `LivePitch` applies the live decision and display smoothing while stored points retain accepted detector frequencies. `SessionAnalysis` derives summaries. `PitchTrace` classifies original points and display links without rewriting measurements or filling gaps.

Optional diagnostic metadata travels beside the accepted frequency. A bounded in-memory history explains rejected observations on Measure; raw candidates never become notes, chart points, or saved measurements. See [Acoustic uncertainty](ACOUSTIC_UNCERTAINTY.md).

## Analysis and rendering

`Pcm16WavReader` reads recorded audio by blocks. `WavAnalysis` produces interval spectra, pitch readings, and statistics with cancellation/progress. `AnalysisCache` keys derived results by WAV fingerprint, version, interval, and instrument. Reanalysis does not overwrite the recording or historical summary.

`SessionWorkspace` generates the visible waveform/STFT on background dispatchers. The UI retains a bounded viewport rather than a full-recording spectrogram. Completed tiles remain in their data coordinates during gestures; new rendering follows the gesture. Color references stay fixed during an exploration. Playback cursor updates do not require recomputing the FFT.

When captured readings exist, Fundamental uses that series even after reanalysis. Sessions without a captured series can use analyzed readings. Cards, detail, and full-screen plots share the pitch classification.

Technical definitions and limits: [Audio analysis](AUDIO_ANALYSIS.md), [Harmonic candidates](HARMONICS.md), and [Session explorer](SESSION_EXPLORER.md).

## Navigation, state, and lifecycle

Navigation Compose owns the back stack:

| Destination | Purpose |
| --- | --- |
| `measure`, `measure/wide` | Two presentations of the same active capture |
| `lab`, `lab/tool/{tool}` | Laboratory home and tools |
| `library` | Sessions and designs |
| `session/{id}`, `viewer/{id}/{plot}` | Session detail and its full-screen viewer |
| `compare` | A/B comparison |

System Back and app navigation pop destinations; there is no substitute back stack implemented through `BackHandler`.

The ViewModel retains independent detail and full-screen chart cameras for the active session. Selection, excerpt, and loop are shared by that session. Detail/viewer transitions and Activity recreation preserve session ownership and playback; actually leaving the session releases resources and transient exploration state. Backgrounding stops playback and capture. Switching normal/wide Measure alone does not restart or save a capture.

Camera and selection retention covers Activity recreation, not guaranteed restoration after process death. Read the [viewer engineering notes](IMMERSIVE_VIEWER.md) before changing gestures, safe areas, orientation, or lifecycle; their dated updates supersede older sections.

## Persistence and privacy

Recordings use private app files and a JSON index in SharedPreferences. Historical `measurements`, `designs`, and `recordings` keys remain compatible. Session tools and Trash add separate state rather than rewriting originals. Trash retains audio and relationships until an explicit permanent-delete confirmation.

Derived caches are disposable and versioned, with a 64 MiB disk limit. Audio export uses Android's Storage Access Framework and copies by blocks. Stored user text is not translated or renamed when the app language changes.

See [Data safety](DATA_SAFETY.md) for the existing backup/update rules and [WAV capture reliability](WAV_CAPTURE_RELIABILITY.md) for failure handling and evidence. These documents retain historical engineering records.

## Testing boundaries

Pure-core tests run on the JVM; Android storage, navigation, player, and UI tests use isolated emulator fixtures. Local synthetic success is distinct from physical microphone accuracy, audible synchronization, or validated acoustic predictions. [Development](DEVELOPMENT.md) lists checks; [VALIDATION.md](VALIDATION.md) records what actually ran.
