# Rebuild baseline

Source: `3a160043f9ebb3a420b1d89a9cf4ddfe7d1b5d67` (main unchanged).

JVM tests and reports reproduced in the isolated worktree on Gradle 9.6.0 / AGP 9.4.1 / JDK 21 / Kotlin 2.2.21; production source is identical to main. Both variants: 195 discovered, 187 passed, 8 protected skips each. JVM lines 28.19%, branches 25.69%; core 98.47% / 77.25%.

Prior actual clean CI API 35 Android evidence: [42361585985](https://github.com/hmeclazcke/DidgeTaller/actions/runs/42361585985), code 90d7167 with identical production inputs: 127 discovered / 117 passed / 10 justified skips, 91.09% lines / 68.25% branches. Seven private fixture guards and three configuration-dependent skips; none authorized for private access here.

Prior tools: 1,978 Detekt findings; Lint 0 errors / 60 warnings / 10 hints; CPD 59 files and zero 100-token groups; PIT 11/11 killed in PlotTouchKt only. No performance baseline has been measured yet.

## Measurement visibility (not a production improvement)

Original reviewed CRAP-L: 8 mapped, 329/337 Detekt functions unmapped, 3 high scores (43, 33, 15). Compiler PSI reveals 378 named declarations: 337 top-level/member functions and 41 nested functions whose complexity belongs to their parent. New source/owner/descriptor/parameter-count/body-location correlation maps 276, with 57 Compose transformations, 41 nested metrics, 3 abstract declarations and one anonymous owner classified explicitly. Lambdas/accessors are inventoried separately, not silently counted as independent Detekt measurements.

Expanded JVM measurement exposes 97 scores >=10, including Android-dependent functions with zero JVM coverage. This is previously invisible debt, not a regression or a justification for leaving it. Android-based scores must be reported separately. No coverage sets are merged.

## Architecture and ownership risks

- TallerViewModel (565 lines) owns capture, library, analysis jobs, playback, diagnostics and design simulation; UI depends on it directly.
- TallerApp (1,007 lines), PlotSurface and ImmersiveViewer combine presentation, state decisions and graph rendering.
- Audio adapters combine platform effects and seek/capture state; stale callback and capture-finalization ownership require characterization.
- Stores combine JSON parsing, file operations, recovery and UI-oriented fallback messages; caches use shared deterministic temporary file names.
- DSP is already framework-independent, but PitchDetector combines preprocessing, visualization, period search, refinement and acceptance; PitchTrace combines validation, neighborhood evidence and linking.
- PCM/WAV processing is bounded; spectrogram tiles are bounded. Measurements are required before claiming throughput/allocation improvements.

## Programmatic source inventory

Generated from Kotlin compiler PSI by `:engineering-tools:sourceInventory` and `python tools/function_mapping.py`. This inventory does not claim that every source has completed architectural review.

| Source (under app/src/main/java/ar/didgetaller) | Lines | Named functions |
| --- | ---: | ---: |
| audio/MicrophoneEngine.kt | 147 | 7 |
| audio/SessionPlayer.kt | 77 | 11 |
| audio/SessionWorkspace.kt | 70 | 5 |
| core/AcousticDiagnostics.kt | 42 | 2 |
| core/AcousticModel.kt | 163 | 12 |
| core/AudioLevel.kt | 25 | 1 |
| core/AudioVisuals.kt | 76 | 5 |
| core/BoreOptimizer.kt | 110 | 6 |
| core/DroneComponentAnalysis.kt | 161 | 6 |
| core/HarmonicAnalysis.kt | 42 | 1 |
| core/InstrumentProfile.kt | 16 | 2 |
| core/LivePitch.kt | 21 | 1 |
| core/Music.kt | 40 | 6 |
| core/MusicalTrace.kt | 111 | 7 |
| core/Pcm16WavReader.kt | 86 | 6 |
| core/PitchAxis.kt | 20 | 1 |
| core/PitchDetector.kt | 137 | 3 |
| core/PitchStream.kt | 37 | 1 |
| core/PitchTrace.kt | 129 | 9 |
| core/PlotTouch.kt | 15 | 1 |
| core/PlotViewport.kt | 97 | 12 |
| core/ProgressiveWavWriter.kt | 55 | 6 |
| core/Radix2Fft.kt | 40 | 1 |
| core/SavedPitchEvidence.kt | 59 | 2 |
| core/SavedYinReplay.kt | 63 | 2 |
| core/SessionAnalysis.kt | 48 | 3 |
| core/SpectralAnalysis.kt | 55 | 3 |
| core/SpectralPalette.kt | 15 | 2 |
| core/SwipeAnalysis.kt | 72 | 4 |
| core/SwipeNative.kt | 9 | 2 |
| core/WavAnalysis.kt | 102 | 3 |
| core/WavCodec.kt | 19 | 4 |
| data/AnalysisCache.kt | 112 | 10 |
| data/LocalStore.kt | 77 | 10 |
| data/RecordingStore.kt | 148 | 11 |
| data/SessionToolsStore.kt | 30 | 13 |
| data/TrashStore.kt | 13 | 3 |
| MainActivity.kt | 84 | 3 |
| TallerViewModel.kt | 566 | 73 |
| ui/AcousticCharts.kt | 172 | 10 |
| ui/AcousticDiagnosticsPanel.kt | 57 | 1 |
| ui/ComparisonScreen.kt | 107 | 7 |
| ui/DiagnosticText.kt | 103 | 1 |
| ui/ExplorerState.kt | 95 | 8 |
| ui/ImmersiveViewer.kt | 431 | 11 |
| ui/InspectionReading.kt | 5 | 1 |
| ui/LibraryTools.kt | 105 | 3 |
| ui/LocalizedLabels.kt | 68 | 10 |
| ui/PlotGestures.kt | 72 | 1 |
| ui/PlotSurface.kt | 301 | 10 |
| ui/RecordingAnalysisPanel.kt | 81 | 1 |
| ui/SavedPitchEvidencePanel.kt | 74 | 3 |
| ui/ScreenAwake.kt | 21 | 1 |
| ui/SessionExplorer.kt | 203 | 5 |
| ui/SessionTransport.kt | 33 | 2 |
| ui/SwipeComparisonPanel.kt | 127 | 5 |
| ui/TallerApp.kt | 1008 | 34 |
| ui/TallerNavigation.kt | 31 | 4 |
| ui/ViewerIcons.kt | 34 | 0 |
