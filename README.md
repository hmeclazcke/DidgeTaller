# Didge Taller

[English](README.md) | [Español](README.es.md)

A native Android app for studying didgeridoo and Native American flute sound. Measure pitch, record practice sessions, and explore how frequency and spectral energy change over time. An experimental laboratory also lets you investigate didgeridoo bore geometry and simulated resonances.

Didge Taller works offline, without accounts, advertising, or network access. Recordings and analysis stay on your device unless you export them. Developed as a personal, noncommercial project, it is currently in beta; acoustic estimates and design predictions have the limitations described below.

## Features

### Measure and record

- Real-time frequency, musical note, and cents deviation using YIN-based pitch estimation, with signal and uncertainty feedback.
- Instrument profiles for **Didgeridoo** (35–140 Hz initially, extendable to 220 Hz) and **Native American flute** (200–1600 Hz).
- One action starts measurement and mono PCM16 WAV recording together. Stopping saves the session, including its captured pitch readings.
- Recording limits of 1, 5, 15, or 30 minutes; 15 minutes is the default. Recording stops and saves when you leave Measure or put the app in the background.
- A wide measurement view for reading the display while playing.

### Listen and explore

- A **Session Library** with playback, rename, WAV export, and recoverable deletion through Trash.
- **Fundamental**, **Spectrogram**, **Waveform**, and **Harmonics** views, with full-screen exploration, zoom, pan, cursor inspection, and reset controls.
- Playback, seeking, excerpt selection, and looping, with a cursor tied to the audio player's position.
- WAV analysis for the whole recording or a selected interval: power spectrum, harmonic candidates, pitch summaries, and spectral energy bands.
- A/B comparison of sessions or excerpts, with overlaid spectra and optional normalization.

### Experiment with instrument design

The didgeridoo **Laboratory** provides bore-profile editing, presets, approximate input-impedance simulation, and evolutionary search against three target resonances. Saved designs can be associated with didgeridoo sessions to examine predictions alongside recordings. These are exploratory tools, not validated construction specifications or flute-design tools. See the [Laboratory guide](README-LAB.md).

The interface is available in **English and Spanish**, selected through Android's language configuration. Stored names and annotations keep their original text.

## Screenshots

Real application captures from the emulator using synthetic signals and test sessions. The live measurement example is in Spanish; the other three show the English interface. They illustrate the UI, not physical measurement accuracy.

| Live measurement | Session Library | Fundamental | Spectrogram |
| --- | --- | --- | --- |
| <img src="docs/screenshots/v326-final/measure-drone.png" alt="Live didgeridoo measurement showing a synthetic 70 Hz tone" width="210"> | <img src="docs/screenshots/localization/en-library.png" alt="Session Library with saved synthetic recordings" width="210"> | <img src="docs/screenshots/localization/en-fundamental.png" alt="Full-screen Fundamental chart with a gap between readings" width="210"> | <img src="docs/screenshots/localization/en-spectrogram.png" alt="Full-screen spectrogram showing spectral energy over time" width="210"> |

More [English and Spanish screenshots](docs/screenshots/localization/INDEX.md), including waveform and design views, are available with their capture conditions. The [live measurement evidence](docs/screenshots/v326-final/README.md) documents its synthetic input. Some captures predate the latest uncertainty feedback and placement of playback controls.

## Technology stack

| Area | Implementation |
| --- | --- |
| Language and UI | Kotlin 2.2.21, Jetpack Compose BOM 2025.04.01, Material 3, Navigation Compose |
| Build | Gradle 8.13 wrapper, Android Gradle Plugin 8.13.2, Java target 17 |
| Android | Minimum SDK 26 (Android 8.0), compile/target SDK 35; application ID `ar.didgetaller` |
| Audio and DSP | Android `AudioRecord` and `MediaPlayer`; Kotlin implementations of YIN-based pitch estimation, radix-2 FFT, STFT, WAV processing, and acoustic modeling |
| Quality | JUnit 4, AndroidX/Compose instrumentation tests, GitHub Actions, Detekt 1.23.8, JaCoCo 0.8.15 |

Versions are defined in the [root](build.gradle.kts) and [app](app/build.gradle.kts) Gradle files. The [manifest](app/src/main/AndroidManifest.xml) requests microphone access and has no Internet permission.

## Getting started

You need Git, Android Studio with a Gradle JDK compatible with this project (JDK 17 is the documented baseline), and Android SDK Platform 35. A separate Gradle installation is unnecessary. The first sync downloads the wrapper distribution and dependencies.

1. Clone the repository with an account that has access:

   ```sh
   git clone https://github.com/hmeclazcke/DidgeTaller.git
   cd DidgeTaller
   ```

2. In Android Studio, choose **Open** and select the repository root containing `settings.gradle.kts`. Configure the Gradle JDK and install SDK Platform 35 through SDK Manager, then sync.
3. Build the debug application and run the JVM tests from the repository root:

   ```powershell
   # Windows PowerShell
   .\gradlew.bat :app:assembleDebug
   .\gradlew.bat :app:testDebugUnitTest
   ```

   ```sh
   # macOS / Linux
   sh ./gradlew :app:assembleDebug
   sh ./gradlew :app:testDebugUnitTest
   ```

4. Select an Android 8.0+ emulator or a dedicated test device in Android Studio and run the `app` configuration. Grant microphone permission when starting measurement. The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

For an existing installation with recordings, preserve its data and verify package/signing compatibility before updating. Do not uninstall or clear app data to resolve an installation conflict. Instrumented tests belong on an explicitly selected emulator.

See [Development](docs/DEVELOPMENT.md) for quality checks, wrapper troubleshooting, and device-safety rules.

## How to use

1. Open **Measure / Medir** and choose **Instrument / Instrumento** before starting. For didgeridoo, extend the pitch range to 220 Hz if needed. The recording limit is under **settings / ajustes**.
2. Tap **Start measurement / Iniciar medición**, allow microphone access, and play. Read the frequency, note, cents, and signal status together. An uncertain result may leave the frequency blank even when sound is present.
3. Tap **Stop and save / Detener y guardar**. Find the result under **Library → Sessions / Biblioteca → Sesiones**. A recording with no reliable note still retains its audio.
4. Open a session and explore **Fundamental** for pitch over time or **Spectrogram / Espectrograma** for spectral energy. Waveform and Harmonics offer sample-amplitude and spectrum views. Open a chart full-screen for zoom, pan, and inspection; use **Tools / Herramientas** for ranges and excerpt controls.
5. Use **Play / Reproducir**, **Pause / Pausar**, and **Restart / Reiniciar** to listen. Seek to a position or select an excerpt to loop. Android Back returns from the viewer to the session; leaving the session stops playback.

The [Session explorer guide](docs/SESSION_EXPLORER.md) explains chart colors, missing readings, comparison, and Trash. The [Laboratory guide](README-LAB.md) covers the separate design workflow.

## Pitch detection and limitations

YIN estimates periodicity by comparing a short signal window with delayed versions of itself. A repeating pattern provides a candidate period, which is converted to frequency. Didge Taller uses a YIN-based implementation with instrument-specific windows and ranges; it does not simply label the largest FFT peak as the fundamental.

- Phone microphones, low-frequency filtering, noise, attacks, strong harmonics, and complex vocalizations can produce weak or competing periodicities.
- The fundamental of the complete recorded mixture may differ from the drone component a musician wants to follow. The app does not isolate the drone from a simultaneous voice.
- Some windows are rejected rather than assigned a misleading frequency. Fundamental preserves missing readings and access to uncertain originals; display links do not create intermediate measurements.
- YIN confidence describes periodicity, not a calibrated probability of a correct note. Spectral levels are digital and relative, not calibrated sound-pressure levels.
- The reported **“I” vocalization issue remains unresolved**. Diagnostic candidates are explanatory evidence, not corrected or saved pitch estimates.
- Playback cursors and loops are not sample-accurate or guaranteed gapless. Spectrogram zoom does not increase the underlying time/frequency resolution.

Synthetic tests, recorded-audio replay, and emulator checks provide engineering evidence. They do not establish scientific measurement accuracy, universal device compatibility, or validated physical design predictions. Independent tuner comparison, sustained physical recording/battery testing, audible synchronization, and a full TalkBack walkthrough remain open validation areas. See [Audio analysis](docs/AUDIO_ANALYSIS.md), [Acoustic uncertainty](docs/ACOUSTIC_UNCERTAINTY.md), and the dated [validation records](docs/VALIDATION.md).

## Architecture

The single `app` module separates Android-independent DSP and models (`core/`), audio capture/playback (`audio/`), local storage and derived caches (`data/`), and Compose presentation (`ui/`). `TallerViewModel` coordinates work and session state; Navigation Compose owns the back stack.

Start with the [architecture overview](docs/ARCHITECTURE.md). [Localization](docs/LOCALIZATION.md) explains resource boundaries. Historical engineering reports remain in `docs/`; their dated results describe specific revisions rather than permanent project guarantees.

## Testing and quality

JVM tests exercise DSP, WAV handling, summaries, and chart mathematics. Android tests cover persistence, navigation, playback, and Compose behavior with isolated emulator fixtures. GitHub Actions runs JVM tests, builds the debug app and test APK, validates localization, and produces Detekt and JaCoCo reports; it does **not** run the emulator test suite.

Detekt findings and JaCoCo JVM coverage are informational, with no enforced quality threshold. Coverage does not measure physical acoustic accuracy or replace Android tests. Commands and report locations are in [Development](docs/DEVELOPMENT.md); dated findings are retained in the [Detekt](docs/DETEKT_AUDIT.md) and [coverage](docs/COVERAGE_AUDIT.md) audits.

## Contributing and license

No formal contribution process is documented yet. Before preparing changes, read [AGENTS.md](AGENTS.md) and the [development guide](docs/DEVELOPMENT.md), keep changes focused, and exclude private audio, local diagnostics, credentials, and build output.

The repository includes an [MIT license](LICENSE). DidgeLab and CADSD are research references, not bundled engines or dependencies; their licenses do not replace this repository's license. See the [Laboratory guide](README-LAB.md) for attribution context.
