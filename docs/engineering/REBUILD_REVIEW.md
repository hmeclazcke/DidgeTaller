# Production responsibility review — IN PROGRESS

## Current acceptance supersedes historical review questions

The tables below preserve the original source-reading decisions at their historical
milestone. Their phrases such as PlotSurface139, unowned ViewModel diagnostics and
pending persistence/workspace are not current open defects. The
[finite acceptance report](REBUILD_ACCEPTANCE.md) reconciles each major subsystem
with implemented boundaries and tests. The [source hash inventory](REBUILD_SOURCE_INVENTORY.csv)
accounts for all116 current production Kotlin files, including class-only files;
698 named declarations remain measured or explicitly classified. Current metrics
and limitations are in [final verification](REBUILD_FINAL_VERIFICATION.md). Optional
style/performance work does not reopen a previously closed reconstruction cycle.


Follow-up review: SimulationController/SimulationLoader and
lab/CoroutineSimulationLoader replace the laboratory executor/version lifecycle;
TallerViewModel retains one derived projection. VisualGeometry, TraceDrawing and
WaveDrawing separate portable preparation from native resources. Boundaries,
tests and remaining exceptions are in [simulation ownership](SIMULATION_OWNERSHIP.md)
and [workspace evidence](WORKSPACE_REBUILD.md).

The initial responsibility review contained 63 project-owned Kotlin files: 32 core, 20 UI, 6 data, 3 audio and 2 entry/coordination files. The current compiler-PSI inventory records the expanded exact source set/hashes; the baseline inventory preserves original main. Every file below has been read during this rebuild. Reading a file is not a claim that every defect is fixed or that its tests are complete. Decisions are deliberately different from final acceptance.

The [workspace continuation](WORKSPACE_REBUILD.md) reviews/rebuilds SessionWorkspace,
CoroutineWorkspaceLoader, WorkspaceController, WorkspaceData, WorkspaceVisuals and
PlotReduction. Their ownership, data/resource boundaries and tests are documented
there. PlotSurface display reduction is extracted; remaining drawing/gesture/axis
coupling and other ViewModel analysis/navigation responsibilities are not complete.

## Core

| File | Responsibility and current decision |
| --- | --- |
| AcousticDiagnostics.kt | Bounded rejection/evidence history; retain separate diagnostic provenance, extend boundary tests where uncovered. |
| AcousticModel.kt | Approximate bore impedance model; retain accepted physics semantics; peak decision complexity still needs a protected redesign cycle. |
| AudioLevel.kt | Source-aligned RMS evidence; retain its independence from pitch validity. |
| AudioVisuals.kt | Bounded viewport waveform/STFT; existing long-session contracts protect memory bounds; spectrogram orchestration remains complex. |
| BoreOptimizer.kt | Cancellable deterministic search over the bore model; search responsibilities and callback/error handling need further decomposition. |
| DroneComponentAnalysis.kt | Supported low-frequency component evidence and rejection; keep scientific thresholds; classification/peak responsibilities remain high-risk work. |
| HarmonicAnalysis.kt | Spectrum peaks and tentative integer associations; retain uncertainty; analyze remains complex and needs differential coverage before restructuring. |
| InstrumentProfile.kt | Central instrument acceptance/range configuration; retain shared rules and stored identifiers. |
| LivePitch.kt | Live acceptance and display smoothing; fixed nonfinite display input with a red/green regression; finite policy unchanged. |
| Music.kt | Equal-temperament transformations and canonical historical text; retain persisted Spanish note names/US numeric formatting; strengthen locale-independent text tests. |
| MusicalTrace.kt | Canonical supported-component/estimated-pitch distinction and 100 ms nearby readout; preserve gaps; didgeridoo assembly complexity remains. |
| Pcm16WavReader.kt | Validated block PCM reading; retain coherent I/O boundary; mutation survivors still require review. |
| PitchAxis.kt | Frequency-correct musical tick placement; retain pure geometry and boundary tests. |
| PitchDetector.kt | Frame preparation, diagnostics and acceptance orchestration; YIN period work extracted and differential equality verified. |
| PitchStream.kt | Streaming frame cadence and retained samples; retain bounded frame ownership; characterization protects append behavior. |
| PitchTrace.kt | Historical evidence, omissions and links; rebuilt with a single-owner builder and separate neighborhood decisions. |
| PlaybackController.kt | Rebuilt stopped/preparing/ready ownership and latest-only asynchronous seeks behind Android ports; deterministic behavior tests. |
| PlotTouch.kt | Pure tap/drag classification; retain focused mutation gate; do not replace pan/zoom with competing state. |
| PlotViewport.kt | Pure camera/projection/domain transforms; retain shared coordinates; tick-generation complexity still above target. |
| ProgressiveWavWriter.kt | Progressive PCM/header checkpoints/repair; keep formats and recoverable close errors; state ownership moved above this I/O primitive. |
| Radix2Fft.kt | Mathematical FFT primitive; retain numerical behavior; no speculative performance rewrite. |
| RecordingCapture.kt | New per-worker monitoring/recording/finished owner; typed terminal outcome, bounded samples, discard claim and exactly-once finalization. |
| SavedPitchEvidence.kt | Bounded rereading of diagnostic pitch evidence; retain separation from canonical trace; inspect orchestration complexity further. |
| SavedYinReplay.kt | Explicit historical estimator replay; retain diagnostic-only semantics and cancellation boundary. |
| SessionAnalysis.kt | Historical accepted-point summaries; retain canonical format and profile acceptance. |
| SpectralAnalysis.kt | Windowed spectrum calculations; retain pure mathematical boundary and deterministic tests. |
| SpectralPalette.kt | Stable energy/color mapping; retain independence from detector acceptance. |
| SwipeAnalysis.kt | Cancellable SWIPE evidence on recorded blocks; retain separate estimator meaning; orchestration remains above target. |
| SwipeNative.kt | Validated JNI adapter; retain boundary validation and bounded native calls. |
| WavAnalysis.kt | Offline block analysis, spectra, pitch/statistics and progress; broad responsibility/complexity remains a priority. |
| WavCodec.kt | Canonical PCM16 byte encoding; preserve exact bytes; boundary assertions strengthened without changing encoding. |
| YinPeriodSearch.kt | New per-frame period comparison/search/refinement owner; original math/order/thresholds preserved in 741-output differential contracts. |

## Android adapters and persistence

| File | Responsibility and current decision |
| --- | --- |
| audio/MicrophoneEngine.kt | Android device/thread/Handler adapter; fixed cross-worker writer ownership and stale error delivery; device construction/release and remaining loop complexity need further review. |
| audio/SessionPlayer.kt | Reduced to MediaPlayer/scheduler adapters behind the pure controller; actual emulator playback remains required alongside JVM tests. |
| audio/SessionWorkspace.kt | Cancellable viewport work; retain bounded cache/tile coordinates; further lifecycle/error tests are needed before final acceptance. |
| data/AnalysisCache.kt | Regenerable keyed analysis/visual/component serialization; concurrent temporary-file corruption reproduced and fixed; parser/orchestration complexity remains. |
| data/AtomicCacheFile.kt | New unique temporary ownership and atomic publication primitive; failure/cancellation/concurrency contracts verified on JVM and through Android cache integration. |
| data/LocalStore.kt | Historical measurements/design formats retained; mutations now share the guarded document transaction. Pure legacy measurement validation rejects nonfinite values. |
| data/RecordingStore.kt | Rebuilt WAV/index ordering with shared writer ownership, strict mutation preflight, preserved orphans, explicit backed-up/quarantined recovery and duplicate-ID display policy. No corrupt-as-empty mutation fallback remains. |
| data/SessionToolsStore.kt | Shared read/modify/publish transaction; invalid individual marker rows stay preserved, unsupported mutation containers fail closed. Format/domain validation lives in SessionToolsMetadata. |
| data/TrashStore.kt | Tombstones use the shared document owner/validated read-only backup. Permanent deletion still requires an acknowledged index publication; cross-store empty-trash remains sequential and retryable, not one database transaction. |
| MainActivity.kt | Permission/activity lifecycle and SAF export boundary; retain public entry/package/signing behavior; error/cancellation cases still need broader evidence. |
| TallerViewModel.kt | Library state now has one coherent immutable snapshot and recoverable metadata-command errors. Major remaining decomposition includes terminal capture events, acoustic requests and asynchronous analysis ownership. |

## Presentation

| File | Responsibility and current decision |
| --- | --- |
| AcousticCharts.kt | Legacy analysis/comparison chart rendering; excessive rendering/selection/geometry responsibilities need separation under UI characterization. |
| AcousticDiagnosticsPanel.kt | Diagnostic observations and uncertainty; retain explicit optional evidence; do not promote raw candidates to musical notes. |
| ComparisonScreen.kt | A/B selection, interval validation and metrics; move interval decisions to a pure validated boundary in a future cycle. |
| DiagnosticText.kt | Compatibility mapping of historical technical messages; retain existing data meanings and localized boundary. |
| ExplorerState.kt | Camera/selection state; retain shared session selection and independent view cameras; test gesture cancellation across navigation. |
| ImmersiveViewer.kt | Fullscreen orchestration, tools, interpretation and shared rendering; high complexity remains. Native Back/playback passed unchanged after correcting the local AVD and recovering System UI. |
| InspectionReading.kt | Explicit inspection precedence; cohesive pure helper retained. |
| LibraryTools.kt | Tools/library controls and labels; retain accepted workflows; accessibility/layout verification continues. |
| LocalizedLabels.kt | Context/localization adapters for stored domain labels; preserve both languages and persisted identifiers. |
| PlotGestures.kt | Single-owner scrub versus pinch/pan gesture policy; retain current user contract; high state-transition complexity requires further work. |
| PlotSurface.kt | Shared axes/clip/projection/cursor and four rendering paths; MCC 139 remains severe presentation debt; renderer responsibilities must be separated with visual/semantic protection. |
| RecordingAnalysisPanel.kt | Optional offline analysis presentation; retain component/fundamental distinction and no rewriting of original audio. |
| SavedPitchEvidencePanel.kt | Explicit diagnostic rereading presentation; retain independent loading/cancellation/error states. |
| ScreenAwake.kt | One lifecycle-aware effective foreground request; retain shared recording/playback ownership and disposal cleanup. |
| SessionExplorer.kt | Compact responsive session workspace plus optional tools; retain principal graph timeline, intentional scrolling and horizontal tabs; orchestration remains complex. |
| SessionTransport.kt | Compact controls and authoritative playback/selection projection; retain synchronization across detail/fullscreen. |
| SwipeComparisonPanel.kt | Opt-in secondary estimator diagnostics; retain its exclusion from canonical missing-measurement replacement. |
| TallerApp.kt | Root navigation plus many screen/render/business decisions; substantial decomposition remains; accepted visual identity is preserved. |
| TallerNavigation.kt | Section/back-stack policy; cohesive helper retained. Actual edge-Back passed after fixing the API 37 AVD's hardware-key configuration; no navigation patch was needed. |
| ViewerIcons.kt | Small presentation-only icon definitions; retain without an unnecessary abstraction layer. |

## Additional project-owned production boundaries

The native JNI wrapper, tracked allocation header and CMake boundary were read. The wrapper caps allocation, serializes the third-party FFT use and validates JNI input; retain those safety boundaries. Vendored SPTK/SWIPE implementations and licenses are not project-owned rewrite targets. Native numerical correctness and real-time performance are not proven by JVM PIT.

The JVM-only SourceInventory compiler-PSI utility and DspBenchmark harness were reviewed and executed. They never enter the APK. The mapper/gates retain explicit ambiguity and coverage semantics; not every lambda/accessor or Compose transformation receives an independent score. Review of tooling is not equivalent to universal Kotlin metric support.

This inventory closes the initial file-reading checklist, not the rebuild. Prioritized unresolved responsibilities are captured in [the progress checkpoint](REBUILD_PROGRESS.md); actual changes and evidence are in [the case study](REBUILD_CASE_STUDY.md). No baseline regeneration, blanket suppression or arbitrary layer count constitutes architectural acceptance.

## Persistence/library continuation

New production files reviewed during this continuation: StoredDocument (transaction and fault ownership), JsonPreferenceDocument (Android JSON/preferences adapter), RecordingMetadata, MeasurementMetadata and SessionToolsMetadata (wire boundaries), LibrarySnapshot (pure visible/trash projection), LibraryCatalog (consistent Android reload), and LibraryRecoveryNotice (explicit localized presentation). These additions are included in the compiler inventory and relevant JVM/Android checks. They do not replace the unfinished application-wide review decisions above.
