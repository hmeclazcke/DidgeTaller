# Sequential validation checkpoint

Old Gradle clients finished. One --no-daemon execution completed successfully in
2m47s: debug JVM 550 discovered/542 passed/8 protected skips/0 failures, debug APK,
androidTest APK, Lint, coverage, Detekt, PSI/JVM inventories and CPD. Historical
NoClassDefFoundError output is invalid overlap evidence, not a production regression.
Four changed Compose declarations received source-bound technical reviews; no numeric
gate changes. Next: run focused native CI on this commit, inspect screenshots and
collapsed-zero-scroll test; stop if it fails. Samsung update remains conditional on
that result and fresh package/signature/read-only recording metadata checks.

# Icon-only navigation and collapsed metronome viewport - IN PROGRESS

Baseline bfc70c5, feature/metronome. Remove visual navigation captions; the icon
supplies the complete localized description to Material NavigationBarItem's Tab
selection/click semantics. Existing destinations and Back are unchanged. Adapt
collapsed metronome density to available height, preserving scroll for expanded
settings and constrained accessibility viewports. Add native role/click/selection
checks and a 360x520dp, 150% font collapsed-zero-scroll/expanded-scroll regression.
Navigation tests now address full accessible titles instead of removed captions.

Validation is not complete. Overlapping local Gradle clients invalidated one JVM run with widespread NoClassDefFoundError; this is not accepted as regression evidence. Wait for every client to finish, then rebuild and run checks sequentially before using reports. Current ignored logs are checkpoints/metronome-icons/
(build.log, test-build.log, final-build.log). Wait for running Gradle sessions to
finish before a single canonical validation. Refresh only source-bound Compose
reviews after inspecting current inventory/mapping; do not relax numeric gates.
Then run the existing manual metronome emulator scope and inspect real EN/ES and
landscape screenshots. Commit/push only validated changes to feature/metronome,
update PR2, and install on R5CT21KTQ8W only after fresh signature/package and
read-only data checks. Samsung installation is explicitly authorized for this
iteration but has NOT been performed. Reuse metadata-only helpers under
checkpoints/samsung-metronome-ux-4e30174 in a fresh evidence directory; no WAV
contents, instrumentation, data clearing or uninstall on the phone. Main intact.

# Full native suite and caption-independent configuration checks - 2026-11-10

Full run 42837217775 on production source 4e30174 executed 176 cases: 166 passed,
10 protected/configuration skips, zero failures. Its later Spanish 150% library
configuration case still selected the old visible Biblioteca caption, now Biblio
at that extreme width. Navigation tests select the unchanged full accessible title
instead of the abbreviated rendering. No assertions, seek tolerances, application
behavior or production inputs change. Keep the existing complete-suite acceptance
and rerun the final workflow to observe all configuration cases together.

The owner separately authorized installing this correction on Samsung. Use only
the specified serial, package/signature checks and read-only current metadata;
never read private audio or perform destructive application operations.

# Measured viewport follow-up - 2026-11-10

Focused 42833806307 confirms the portrait structure and actual tempo display are
restored. Its remaining diagnostics exposed a constrained-pane test using global
configuration width, a pause click below the viewport, and redundant centered text
alignment in a glyph-bound assertion. Pass measured navigation width, scroll to
the secondary action before tapping, and keep a single tempo alignment owner.
Remove the outer unused BoxWithConstraints flagged by Lint; retain the actually
needed width scopes. Local native APK compilation/Lint pass before publication.
No pure timing/audio/PIT inputs changed; next validate the short native scope,
then run complete instrumentation once on the accepted implementation.

# Captured portrait/landscape correction - 2026-11-10

Focused run 42830157254 retained four real failure images. They revealed that
remaining body width/height misclassified a short portrait as landscape. Use
Android orientation, a width/font budget for short captions and the normal single
portrait column. The header owns per-destination suppression and frees landscape
practice space; the primary transport action precedes sound/pause. The old image
also demonstrated subpixel width rounding: preserve the project's existing
one-pixel glyph allowance, explicit no-ellipsis/height checks and nonoverlap checks.

The selected paused-scrubbing scenario passed in isolation. Read its pause assertion
on main after the callback finishes publishing id and pause intent, avoiding an
instrumentation-thread observation between the two assignments. The 80ms seek
contract and pause assertion are unchanged; no playback production code changed.

Local compilation, resources and gate pass: 592 measured/135 explicitly unscored,
64 historical above-target functions, no new numerical exceptions. TallerApp MCC
40, window toolbar 6 and navigation renderer 3 (original host 48). Existing timing,
PCM and 177-mutant evidence is unchanged. Revalidate the captured UI configurations
before the final complete native run. PR2 remains draft; no physical device/data
operation is part of this correction.

# Focused native diagnostics - 2026-11-10

The full run on 2a9d806 still found label overflow and a paused-scrubbing failure.
Do not repeat the whole suite to diagnose each layout. Add an explicit manual
metronome-only emulator scope with native audio/navigation cases, the four
localized label regressions and the paused-scrubbing scenario, followed by real
EN/ES 150% and landscape captures. It is labeled focused, has no invented coverage,
and preserves every failure in its exit code. Full acceptance stays unchanged.
Capture images on failure as well as success; the full script now installs its
collection trap before running the suite, after explicit emulator verification.

# Responsive metronome follow-up - 2026-11-10

Real API35 acceptance exposed a four-tab Spanish label constraint and a test's
incorrect initial-tempo assumption. Correct line height/full item label width,
shorten only the very narrow laboratory label, and keep full accessible titles.
NavigationSections owns this presentation; TallerApp stays within its existing
complexity ceiling. Bound tempo glyph size to the available slot and choose the
landscape layout from actual constraints. Strengthen four-label/digit overflow
checks; allow deliberate scrolling on extremely short 150% viewports. Retain
failure/top/transport captures. Revalidate affected UI and gates, reuse unchanged
pure-engine/PIT results. Latest remote outcome and screenshot evidence live on PR2.

# Metronome UX/audio correction - 2026-11-10

Replace the large Measure panel with a dedicated second section. Four destination
icons, a central explicit tempo editor, beat-derived animation and compact advanced
options preserve the existing transport and capture interlock. Media keys target
the media stream on that tab; requested sound and muted/zero-gain/failed output are
visibly distinguished. Do not infer a Samsung hardware cause from the earlier
persisted off preference. Extract unchanged PCM generation for energy tests and
validate native gain/play state; require frame-consumption evidence in the clean
emulator. Native tests additionally cover four sections, Back, all meter indicator
counts, invalid tempo and keyboard dismissal at real compact EN/ES 150% settings.
VersionCode stays 13 because this is a correction on the same unmerged beta branch;
no phone installation or new release is performed. Local evidence is in VALIDATION;
remote/native acceptance remains pending until the actual runs complete. Keep PR2
draft, preserve main and private data.

# Metronome feature — 2026-11-09

Branch `feature/metronome` starts at merged main `bc2df23`. The rhythm model and
absolute rational grid live outside Android. A single engine monitor owns click
effects, capture admission, cancellation and terminal disposal. Measure hosts one
expandable panel; navigation/backgrounding pause without automatic restart.
Separate preferences preserve settings without changing session/WAV formats.
See [design and reproducible checks](METRONOME.md). Physical-device access and
installation are outside this task; only a new synthetic emulator data directory
is used. Existing rebuild tools/gates are retained.

Actual API35 captures from source `5f706ba` prompted an IME usability correction:
transport/Done end tempo editing, the native case asserts focus/keyboard release,
and screenshots use safe insets and a stable scrolled view. This changes UI inputs;
its CI is revalidated while unchanged domain/PIT evidence is reused. PR2 remains
draft and main remains unchanged.

# 2026-11-08 workspace continuation handoff

## Verified comparison/rendering delivery - 2026-11-09

[Executable source11fa244 verification](engineering/SWIPE_VERIFICATION_11fa244.md) records passed fast42650147321,
full API35 emulator42650133214, five actual EN/ES/large-font configuration cases,
and valid unchanged-scope mutation42614248589. JVM936/920/16; Android163/153/10,
zero failures. JVM43.39/39.70%, core99.07/83.78%; separate Android92.02/68.62%.
CRAP-L484/114 (72 JVM/28 Android high), Detekt1987, Linux Lint0/53/8 (one
version-metadata advisory beyond local52), CPD0. The earlier failed ES150 run
is preserved along with the actual layout correction; no gate/test weakening.
Whole application remains IN PROGRESS; exact next owner-admission regression
and remaining work are in the continuation checkpoint. Reuse executable source
for documentation-only descendants, not as proof of a newly executed suite.

## 2026-11-09 CI layout correction

Preserve the failed c9b3ab3 extra-config result; do not retry it unchanged or
weaken overflow assertions. Native reproduction identifies vertical padding,
then a permanently absent landscape frame. The safe-inset owner now supplies
an intrinsic minimum with intentional scrolling,180dp minimum graph and full
scaled text. Ten final focused native cases pass; ordinary source quality policy
and the existing65% normal-scale graph assertion are unchanged. Publish the
correction on the feature branch and observe its real CI before final evidence.
Reuse passing mutation42614248589 because this delta changes none of its targets.

## 2026-11-09 comparison operation integration

[Source and evidence](engineering/SWIPE_OPERATION_OWNERSHIP.md) record native
post-disposal RED/GREEN, cached/camera ownership, the shared coroutine scheduler,
stronger cancellation observations, selected PIT, final local JVM936/920/16 and
separately retained launcher/test-driver failures. Source/CRAP-L gate passes with
484 scored/114 independent unscored and no ordinary risk waiver. Final remote
emulator coverage and broad mutations await the single combined publication.

## 2026-11-09 bounded plot annotation

The [annotation ownership cycle](engineering/PLOT_NOTICE_OWNERSHIP.md) records the actual native bounds RED,
final four-case GREEN plus ES/landscape150 validation, passive seek/scrub proof,
focused current-source metrics and the next independent comparison lifecycle RED.
No DSP, persistence, measurement threshold or public/private data changes.

## 2026-11-08 analysis and rendering decisions

The [current source-scoped milestone](engineering/ANALYSIS_RENDERING_REBUILD.md) documents typed recording-analysis
ownership, the ready-frame plot pipeline, actual native RED/GREEN, full scientific
text at 150%, comparable complexity and focused mutation evidence. Whole rebuild
remains IN PROGRESS; [actual current source verification](engineering/ANALYSIS_RENDERING_VERIFICATION_8b73c21.md)
records all three passing remote jobs, metrics, screenshots and precise residual work.


Source ec19b18 now has observed successful fast, actual API 35 emulator and
expanded mutation CI. The [source-scoped verification](engineering/WORKSPACE_VERIFICATION_ec19b18.md)
records numerical results, report scopes, visual limits and exact next work.
The whole rebuild remains IN PROGRESS; PR #1 stays draft.

Replace laboratory version/executor orchestration after a deterministic native
RED showed a disposed ViewModel accepting a late failure. Use a typed operation
owner, scoped serial evaluation and one derived Compose snapshot. Preserve the
public Android factory constructor. Resolve Result mangling through exact compiler
metadata instead of guessing or waiving scoring. Require canonical quality builds
after observed generated-denominator variance. Local final evidence and exact next
analysis-orchestration task are in docs/engineering/REBUILD_PROGRESS.md. Publish
only the feature branch and inspect actual CI; preserve the failed local launcher
run separately from its bounded successful retry. Whole rebuild stays IN PROGRESS.

Separate waveform/raster geometry from drawing. Pure factories retain source-time
sample positions, min/max bin centers, amplitude, log coordinates and completed
tile bounds. A native WaveDrawing adapter owns cached Path construction; Canvas
retains clipping/ink. Eight contracts and selected real PIT defend the boundary.
The first inline caching attempt raised PlotSurface MCC to 134; extracting native
resource ownership restored 127 and lowered cognitive complexity to 279. This is
not a claimed global-complexity or measured performance win. New measured functions
score 4/6; the actual Compose adapter is explicitly classified unscored. Observe
final native evidence before proceeding to ViewModel async operation ownership.

The strengthened ES/150% assertion actually failed in run 42498662573; default
139 native cases passed with ten protected skips. Failure node bounds y=10..161
show the target outside the vertical content viewport. performScrollTo on the tab
addresses its nearest horizontal scroll owner, not the vertical parent. Reveal the
row first via the sibling expand control, then scroll/tap Dron and retain both
selection/fullscreen assertions. This corrects evidence setup without bypassing
real touch, weakening assertions or changing production navigation.

Continue into rendering: prepare ordered trace instructions in pure TraceDrawing,
retaining original link/style/uncertainty order and source coordinates. Keep actual
projection and ink in PlotSurface; retain prepared marks across cursor updates.
Seven behavioral tests and selected PIT protect the seam. A surviving addition
mutant exposed zero-origin-only test data; add timestamp translation invariance,
not production special cases. New factories score 7/8; overall renderer remains
MCC 127 and unfinished. Preserve exact source-bound Compose/constructor reviews;
no new measured-function waiver or baseline reset. Final native CI must be checked.

Observed source 9a7229f native CI: full 149-case suite passed with ten protected
skips, but the separate EN/150% screenshot rejected its active window. Preserve
that failed run. Synchronize the screenshot helper's lazy accessibility connection
and await the required app window rather than sleeping 250 ms; on failure retain
the rejected PNG/package for diagnosis. Assert real configuration locale/font.
Compile the test APK, then verify the actual follow-up CI without weakening the
window check. Fast and expanded mutation runs already succeeded for unchanged
production code and need not be repeated manually.

Continue existing feature branch from 4d6dbea, protected main 3a16004 and draft
PR #1. Rebuilt SessionWorkspace around one typed operation owner, a controllable
coroutine loader and resource-scoped PCM preparation; the Compose facade publishes
the same immutable snapshot and owns no competing transitions. Added actual
coroutine cancellation/reader tests, native lifecycle/playback/data tests and
typed time-only versus spectral requests. Extracted display RMS/PSD reduction and
fixed empty-spectrum failure plus negative-amplitude request incompatibility.

The final source passes local JVM/build/static/coverage/ratchet/selected-PIT gates;
new-source Android and broad mutation CI must be inspected after publication.
No original numerical baseline is reset; constructor/accessor/Compose limitations
are exact-content reviews with executable test references. Source retirement is
explicit for the request relocation and consolidated tile publication. Run-error
reports now fail mutation tasks; shared-runner interruption was isolated.
WORKSPACE_REBUILD.md and the top of REBUILD_PROGRESS.md contain exact evidence and
next work. Whole reconstruction remains IN PROGRESS; do not merge or touch Samsung.

# 2026-11-08 forensic audit handoff

Continue feature branch refactor/uncle-bob-rebuild, protected main 3a16004 and draft
PR #1. Confirmed 50 source-added named declarations and 16 modified declarations
between 9cd18f6 and 672c9b2. The 14 extra unscored entries are seven abstract
observer methods plus seven anonymous adapters with invalid standalone MCC=0;
they were not old code newly discovered by an inventory change. Do not conflate
the Detekt denominator (487) with all PSI named declarations (531).

Implemented focused source-fingerprint/complete-input gate correction, including
explicit historical-source technical/risk reviews rather than hidden exclusions.
No application source/test/resource or acoustic changes. Full forensic inventory,
method mutation evidence, limitations and continuation actions are linked from
docs/quality/FORENSIC_COMPLIANCE_AUDIT.md. Audit validation is tool-only; reuse
unchanged runtime evidence. Whole-application rebuild remains IN PROGRESS.

## Application-wide UX/UI audit — 2026-10-24

Historical entries retain their original language and version-specific scope. See [Language policy](LANGUAGE_POLICY.md); current English entries appear with their delivery dates.

- Started from clean `main`/`origin/main` at `514f3c4`; retained all existing
  acoustic behavior and historical data. Followed the screen inventory and
  existing UI test infrastructure. Used only `emulator-5554`, never the Samsung.
- Corrected large-text Measure priority, bottom navigation labels, redundant
  top-bar subtitle, saved pitch legend order and accessible control names.
  Replaced concatenated translated prose with complete EN/ES resources and
  normalized Spanish “dron” terminology. Regression tests were red before
  relevant corrections and green afterward.
- The broad instrumented suite still has nine failures in legacy Fundamental
  tests that assume pre-replay pitch series/scales. Their expectations need a
  separate, explicit decision on current replay versus preserved capture data;
  no acoustic or historical measurements were changed to mask them. The
  detailed matrix and exact validation results are in [UI_AUDIT](UI_AUDIT.md)
  and [VALIDATION](VALIDATION.md). No push should be claimed as validated while
  this gate remains red.

## Independent saved-session pitch evidence — 2026-10-24

- Began from clean `00ade24`. The private four-bout WAV and Samsung screenshot
  were available; original audio hash was checked before and after. Reused the
  existing protected tracker experiments rather than reopening F0 research.
- Isolated the 30.73 s false negative to an F0-style 3.5× ambiguity veto, not
  a missing ~65 Hz spectral line, chart timestamp defect or cache omission.
  Retained the directly supported partial as `COMPETING_PERIODICITY`, with the
  alternative spectral evidence inspectable. A stateful sinusoidal tracker did
  not add evidence for the affected windows and was not introduced.
- Made the YIN fundamental and spectral component independent visual layers,
  with Fundamental / Component / Both controls and explicit source labels.
  Replayed current YIN from the saved WAV in memory to avoid showing stale
  historical acceptance; the old readings remain in diagnostics and storage.
  A captured pitch beyond the normal 140 Hz tolerance selects the historical
  expanded 220 Hz replay range, protecting supported toots without guessing
  an unrecorded session setting.
  The low-energy ~51 Hz tail point is rejected by the unchanged current
  support gate. The acoustically ambiguous ~35 Hz candidate remains visible.
- Fixed the immersive audio-level label collision after inspecting the real
  15:27 fixture in an API 37 emulator. Portrait, forced landscape, Spanish
  and English captures are private under the ignored experiment directory.
  No Samsung operation or original data modification occurred.
- Section-level counts, synthetic controls, older-WAV checks, runtime and
  limitations are in [PITCH_RESOLUTION.md](PITCH_RESOLUTION.md) and
  [VALIDATION.md](VALIDATION.md). Private CSV, plots and exact commands are
  in ignored `checkpoints/experiments/drone-component/REPORT_V3.md`.

## Saved-session drone component decision — 2026-10-24

- Started clean at `332d921` with `origin/main` aligned. Reused the October
  24 private study and prior YIN/SWIPE′/pYIN/fusion evidence; did not restart
  estimator research or change the accepted pitch detector.
- Reproduced the study's low spectral ridge, rejected a permissive prototype
  after held-out false positives, then froze and validated a conservative
  component-only hypothesis on independent synthetic cases. No spectral
  observation is presented as verified physical F0.
- Added read-only, bounded saved-WAV analysis for didgeridoo sessions. YIN
  Fundamental remains canonical; a dashed secondary component appears only
  in supported gaps. The advanced panel retains the raw candidate and reason.
  No live processing, persistence, cache migration, flute behavior or source
  WAV handling changed.
- Protected four-bout replay gains 15/29 first-“I”, 8/44 second-drone and
  14/47 second-“I” distinct secondary-evidence windows beyond accepted YIN;
  unsupported intervals remain gaps. A true 35 Hz harmonic family is retained,
  while weak/missing lower lines abstain rather than becoming a forced 70 Hz F0.
- Targeted JVM and emulator cases passed; synthetic screenshots were inspected.
  On an API 37 emulator, 40 s of synthetic audio took 1.245 s to analyze.
  Final validation passed 175 JVM tests, 6 relevant emulator cases, root
  Detekt, JaCoCo report generation and debug APK assembly.
  Exact private reports and images are ignored under
  `checkpoints/experiments/drone-component/`. No Samsung operation or private
  audio publication occurred. Physical drone pitch, long-session peak memory
  and battery behavior remain unresolved.

## Product UX polish and audit closure — 2026-10-23

- Reviewed all implemented screen families, routes and secondary surfaces in source;
  exercised the main flows with isolated synthetic emulator recordings. UI_AUDIT
  distinguishes rendered/interactive evidence from source-only and physical limits.
- Kept the existing player/chart integration and strengthened it with shared clock
  polling, an accessible seek slider and clearer elapsed/total text. Full-screen
  access moves into the chart heading. Tabs, cameras, paused seeks, playback,
  original WAVs and uncertainty gaps retain their existing owners/contracts.
- Moved optional SWIPE comparison and signal-window inspection into Advanced
  diagnostics. Opening it performs no analysis; explicit comparison still has
  a visible experimental legend and switch-controlled cancellation. YIN/SWIPE
  remain independent and unchanged, including flute behavior and all thresholds.
- Added accessible names to key sliders/switch and explicit48-dp comparison target;
  completed dark surface tokens so dialogs match the existing palette.
- Actual screenshots exposed a second P1: short landscape wide measurement hid
  Hz/status. Added a conditional two-column card and shared instrument/capture
  heading, preserving vertical presentation. Strengthened visibility bounds under
  large fonts instead of accepting a passing test with a clipped secondary label.
- Broad 49-case replay had one modal-screenshot harness failure; corrected and
  reran that case. Matrices and focused final checks pass; 51 distinct UI cases
  have passed across runs. Final JVM:157 passed, three opt-in fixture skips;
  debug APK and informational quality reports generated. Exact counts/limits are
  in VALIDATION, with reviewed synthetic before/after screenshots.
- Deferred full TalkBack traversal, audible/sample-accurate playback validation,
  physical predictive animation and denser advanced/legacy controls. No P0 was
  reproduced in the exercised inventory. No acoustic correction, persistence
  change, private audio use, Samsung operation or push. Existing local commits and
  the unrelated worktree remain preserved.

## UX audit: recording lifecycle correction — 2026-10-23

- Started clean at main/67b541d with two unpushed documentation commits; fetch
  confirms origin/main remains170bcf9. Preserved the other worktree and all history.
- Reproduced a P1 workflow defect on the API37 emulator: rotating during active
  wide measurement entered SAVING because onAppStopped stopped the microphone
  before checking isChangingConfigurations. This was historical behavior, but
  violates the current requirement to preserve an in-progress recording on rotation.
- Moved only the configuration-change guard ahead of microphoneStop. The retained
  ViewModel keeps the existing capture; actual backgrounding still stops/saves.
  No DSP, capture engine, WAV writer, session format or detector selection changes.
- Added an actual Activity/AudioRecord regression: preserve start time/timeline/
  progressive file through landscape, edge Back to normal measurement and portrait;
  actual background lifecycle saves exactly once. It failed before the fix and
  passed afterward. No synthetic detection accuracy or Samsung test is implied.
- Debug/app-test builds and JVM suite pass in the working tree, which also contains
  a separate UI-polish change. Broader UI validation is recorded separately.

## October 23 private recording follow-up — 2026-10-23

- Extended the existing aligned comparison at clean `32b97fb` after both missing
  private originals became available. Preserved the source lock, thresholds,
  selection rules and cached historical observations; processed only 294 new
  complete windows. Original hashes unchanged, no application/device changes.
- Normal-drone agreement is conditional: 37/37 and 18/18 matched estimates agree
  within 25/50 cents, but the second normal body has 13 YIN rejections and one
  accepted 35-Hz result against SWIPE near 66 Hz. Thus central problems are not
  unique to the previous clip; its continuous lower YIN register was not replicated.
- Both I cores reproduce accepted conflicts near 85/115 versus 68 Hz. Actual YIN
  diagnostics identify its first-dip selection despite a deeper later 68-Hz minimum.
  A separate onset low reading exposes differing energy-gate/comparison support.
  These are concrete observations, not proof of physical source F0 or a validated fix.
- Keep estimators separately inspectable. Neither numeric averaging nor a simple
  rejection-only fallback resolves accepted conflicts. Prior controlled SWIPE
  regressions remain relevant; no fusion or production policy change is justified.
- Private reproducible report/CSV/plots/probes remain in the ignored experiment
  extension. PITCH_RESOLUTION adds shareable derived results, VALIDATION records
  checks and limits. No automatic push; the existing unpushed documentation commit
  and unrelated worktree are preserved.

## Stable normal-drone YIN/SWIPE comparison — 2026-10-23

- Investigated the concrete same-time agreement question at clean source baseline
  170bcf9, reusing actual current production classes and SPTK C. No app behavior,
  detector selection, filtering, confidence policy, persistence or chart changed.
- Discovered all protected archive audio, deduplicated to 18 contents; replayed
  the 13 Didgeridoo-profile recordings. October 23 inputs absent. Locked audio-only
  stationarity selection before estimator execution; kept unannotated technique
  distinct from confirmed/sequence-supported normal and confirmed I.
- Result is recording-dependent: two initial normal bodies agree closely, but the
  confirmed normal middle of 19:17 has persistent 35–37 versus 64–69-Hz selections,
  not merely attacks/releases or isolated outliers. Primary aggregate 42/52 paired
  estimates within 50 cents out of 55 eligible; confirmed-only cores 0/10 out of 13.
  Full 3–8-s confirmed normal remains 0/21 out of 25 without stationarity selection.
- The known 5.294-s value is a window end, not its center; actual aligned estimates
  are YIN 36.496871 and SWIPE 65.833369 Hz. Difference is 1021.26 cents, not an exact
  octave. Greater I coverage is measured separately and does not establish accuracy.
- Local ignored report/plots/CSV and frozen reproduction scripts retain full
  evidence. VALIDATION records observer parity, denominator/hash checks and limits;
  PITCH_RESOLUTION summarizes only shareable aggregates. Original archives, audio,
  names/settings/associations and unrelated worktrees preserved. No devices,
  uploads, new dependencies, production changes or automatic estimator fusion.

## SWIPE production consolidation review — 2026-10-23

- Started at clean published dbed749, the user's manual commit; original main and
  unrelated/conflicted worktree preserved. Checked actual execution paths rather
  than assuming the optional comparison had already become the live detector.
- Fixed a demonstrable SPTK boundary-selection defect: an upper-range maximum
  no longer substitutes the minimum frequency. Known 140-Hz reference now gives
  138.99 Hz rather than 35 Hz. Upstream notices and original/current hashes retained.
- Fixed didgeridoo register connectors: >=600-cent changes separate the lines even
  with high periodicity. All originals, short valid events and note groups remain
  visible/inspectable; no added smoothing, octave correction or duration filter.
  Flute/capture/storage behavior remains unchanged.
- Did not promote SWIPE′ to the normal detector: held-out didgeridoo known-F0
  accuracy remains 96/135 versus production YIN 125/135. Greater real-WAV coverage
  does not override those controlled regressions. No unnecessary dual-detector
  normal path exists; YIN remains normal, SWIPE′ optional and YIN diagnostics intact.
- Replayed actual private pipelines; 654 decisions/raw fields and classifications
  match their historical baseline. Boundary fix changes no real-WAV SWIPE′ output.
  Low-frequency hypotheses exist before drawing and cannot all be called false.
  October 23 files were absent locally, so no claim is made about their attacks,
  pauses, vocalizations or reported 115-Hz excursions. Physical drone F0 unresolved.
- Full JVM/build/quality, opt-in private replays and nine focused emulator tests
  passed. Exact before/after counts, red failures, runtime/memory measurements and
  limits are in VALIDATION.md; current engineering decision is in PITCH_RESOLUTION.md.
  No Samsung access, private publication, recording mutation or history rewrite.

## Optional SWIPE′ pitch comparison on Android — 2026-10-23

- Implemented the requested comparison inside saved-session Fundamental, rather
  than replacing the baseline detector. Actual legacy SPTK C is built through a
  small JNI library; numerical source files retain exact upstream hashes/notices.
  Enabling the switch starts bounded local WAV analysis. Results are transient,
  cancellable and share chart coordinates/playback/gestures with YIN. Disabling
  restores the original camera and chart. The existing signal inspector is intact.
- Restricted comparison to 44.1-kHz didgeridoo recordings after reproducing an
  upstream out-of-bounds interpolation defect in the flute configuration. Native
  guards prevent unsafe entry. Flute/live detection and persisted formats remain
  untouched. This is a deliberate supported-scope limitation, not an unvalidated
  repair of the upstream algorithm. No frequency was clamped or octave-corrected.
- Android matches all 121 protected 19:17 reference windows and retains known
  synthetic SWIPE′ errors. More accepted frames during normal drone and “I” are
  useful comparative evidence, not proof of lip-driven frequency. The earlier
  engineering decision to retain production YIN stands.
- Relevant JVM/build/native/UI checks passed. English/Spanish UI tests verify
  restored chart pixels, cancellation, playback, gestures and unchanged WAV/index;
  the actual app navigation/theme was exercised. Performance/resource measurements,
  failed development checks, provenance and limits are recorded in VALIDATION.md
  and SWIPE_COMPARISON.md. New native build tools are pinned in app/CI configuration.
- Technical follow-ups remain: independently validate physical source frequency;
  investigate the unsafe upstream flute configuration separately; measure ARM/low-
  end-phone battery/thermal behavior before broad performance claims. No Samsung
  operations or automatic branch/worktree reconciliation were performed. Commit
  and publish only this reviewed implementation if a normal fast-forward is safe.

## 2026-10-23 — Integrated pitch decision and saved signal evidence

- Completed the requested end-to-end comparison rather than repeating rejected prefilters, blanket octave rules or the earlier approximate ensemble. Genuine pYIN and two independently sourced SWIPE implementations were executed alongside the actual production pipeline on one frozen development/held-out protocol and all three private WAVs. Source/model/license/runtime inspection kept external tools entirely outside the APK. PESTO was inspected but not installed because of its absent PyTorch/torchaudio runtime and unvalidated deployment, not because its small weights were assumed large.
- Decision B: no replacement meets the evidence bar. Added a compact optional saved-WAV inspector with original-profile acceptance, distinct lower-search periodicity, observed three-peak families and sound-energy state. The fields are deliberately separate, without fused frequencies, inferred notes, connectors, historical rewrites or changes to Fundamental. Spectral alignment can occur in noise; UI and report state this. Both languages are supported.
- Added deterministic regression tests and a protected private-WAV case; targeted emulator tests verify explicit requests, cancellation, text layout and byte-identical data. One stabilized full JVM/debug/quality run and focused UI regressions passed. Precise results, failed test/harness attempts, performance boundaries and commands are in VALIDATION.md. Existing core decisions match exactly on all 1305 integrated audit windows.
- PITCH_RESOLUTION.md consolidates the measurement contract, all prior rejected approaches, authentic-method provenance, aligned-window/latency limitations, common-denominator metrics, real coverage, integration choice and unresolved physical ambiguity. Both READMEs and the session guide link to the result. Raw private experiments/plots remain ignored; no acoustic data or temporary tools are published.
- Main starts at published `4e6a06a`; unrelated worktrees/history are preserved. Commit only the reviewed successful optional-inspection phase and report the actual fast-forward/CI status. No Samsung commands, automatic reconciliation, destructive operations or version/toolchain changes. The physical lip-driven drone is still not independently identified during ambiguous mixtures; this delivery does not promise a corrected continuous Fundamental curve.

# 2026-10-23 — GitHub documentation overhaul

- Replaced the dated Spanish landing page with an English project introduction, features organized by musician workflow, a four-image gallery, verified stack, source setup, usage, acoustic limits, architecture links, quality overview, and contribution/license context. Added a complete README.es.md with equivalent structure, commands, links, and assets.
- Reused README-LAB.md as the experimental didgeridoo-design guide. Rewrote ARCHITECTURE.md and SESSION_EXPLORER.md around current responsibilities and user workflows; translated/revised AUDIO_ANALYSIS.md and HARMONICS.md, including the implemented flute path. Added only one new supporting guide, DEVELOPMENT.md, to consolidate build/test/report commands and safe development rules.
- Removed duplicated setup, agent-specific prompts, superseded navigation, obsolete version/test totals, transient presentation details, and personal-device troubleshooting from the README entry points. Preserved historical reports, research, roadmap, existing screenshots, and all earlier log content. MIT LICENSE already exists; no licensing decision or permission was invented.
- No application behavior, UI/resources, Gradle configuration, test infrastructure, audio, or stored data changed. Validation uses source/configuration comparison, four visual image reviews, 110 relative-link/image checks, bilingual parity checks, and Git diff review; see VALIDATION.md. No builds/tests or device access were needed.
- Publication decision: preserve `bb92981` and `8d4cab3` and leave the documentation commit local above them. A direct main push would include those feature commits. Before publication, review and authorize that full outgoing history, or adapt/cherry-pick the documentation onto an isolated branch while accounting for features absent from its base. Do not reset or rewrite existing history.
- Remaining decisions/evidence: establish a formal contribution process and publication/release policy if desired; retain the explicit unresolved acoustic, sustained-capture, synchronization, and accessibility limits. Starting recovery reference: `8d4cab3`, with subsequent work preserved first.

## 2026-10-22 - Acoustic uncertainty UI and delivery

- Core commit `bb92981` preserves bounded diagnostic evidence without modifying accepted frequencies. Added optional localized details on Measure plus discreet uncertainty feedback, keeping candidate Hz separate from notes and chart data. No Session screen changes or historical-data assumptions. English/Spanish use standard resources; candidate/reason Text semantics are tested.
- UI regression failed before implementation, then passed in en/es with real pipeline PCM injection and isolated persistence. Existing didgeridoo/flute meter regression also passed. Native screenshots were reviewed; no clipped status/candidate text observed at the tested configuration. Normal scrolling exposes lower details. Physical audio, full font/layout matrix and TalkBack traversal remain unverified.
- Final JVM/debug/release/app-test/JaCoCo/Detekt validation passed: 140 JVM cases, two optional skips; the separate private test checks six verified real windows and passed. Two distinct emulator cases / three successful executions, preserving the synthetic session JSON/WAV exactly. No Samsung access, private audio mutation, second estimator or correction of vocalization I. Exact commands/results and limits are in VALIDATION.md and ACOUSTIC_UNCERTAINTY.md.
- Deliver as a second small local commit; no push/CI in this iteration. Recoverable pre-change reference is `33792ba`, with later work preserved before recovery. The remaining acoustic question is which competing period, if any, represents a reliable drone fundamental during I; this change records and communicates uncertainty rather than resolving it.

## 2026-10-22 - Bounded acoustic uncertainty evidence

- Inspection confirmed that the existing optional YIN observer is disconnected from normal microphone results, while LivePitch and SessionAnalysis correctly preserve missing Hz. Chose an optional Reading diagnostic, transported by PitchStream through the existing callback, plus a 128-entry in-memory scalar history in the ViewModel. No MicrophoneEngine lifecycle or persistence changes; existing accepted Hz remain authoritative.
- Three states distinguish detector low amplitude, profile-accepted pitch and uncertain pitch. A stricter flute rejection stays uncertain. Evidence resets on a new successful microphone start or profile selection, remains bounded after stop and is never stored in session JSON/WAVs. Historical null points remain unknown. See ACOUSTIC_UNCERTAINTY.md for architecture and limitations.
- Regression failed before transport, then synthetic checks and one explicit private six-window test passed without changing accepted Hz/periodicity or private audio hashes. Core JVM/debug validation is logged in VALIDATION.md. Next phase: discreet localized uncertainty feedback and optional diagnostic details, with focused emulator checks. No fix for physical I is claimed, and no Samsung operation is authorized or performed in this task.

## 2026-10-22 - Session chart playback controls

- Placed the existing playback buttons and always-visible elapsed/total time directly below the chart inside its existing card. A content slot keeps SessionDetail's existing player callbacks and cursor as the single source for both tabs. Audio file actions remain separate below the chart; no engine, classification, persistence, navigation or chart-rendering changes.
- Added one focused real-MediaPlayer regression using existing isolated synthetic fixtures. It fails before the UI change, then passes with paused/playing tab continuity, seek, restart, time display and control placement. Two existing playback/navigation tests also passed. A one-second asynchronous startup timeout required a test-only five-second wait; assertions stayed intact, and only that case was repeated.
- One debug build, 135 passing JVM tests/one optional skip and bilingual resource validation succeeded. Reviewed native English screenshots of both tabs with the same 2.3-second cursor/time. Spanish layout and physical synchronization remain unverified in this task; no broad matrix or Samsung access. Details and exact local logs are in VALIDATION.md.
- Focused phase starts at `283b343`; commit and push the validated changes normally, report actual CI status and preserve later work before any recovery.

## 2026-10-22 - App version and native acoustic splash

- Added the installed package version to the existing expandable recording settings, with English/Spanish resources and no new screen or version/toolchain changes. Find it through Measure/Medir -> Limit/Límite settings/ajustes; current values are `3.2.6-beta1`, build 12. Identical-version rebuilds require APK hashes to distinguish exactly.
- Added a static didgeridoo/wave vector for Android 12+ framework splash, preserving launcher artwork and startup behavior. The base window uses the same dark background. No protected engine, WAV lifecycle, persistence, chart, UI navigation, localization architecture or README changes.
- One debug build and existing JVM suite passed (135 passed, one optional skip); resource parity passed. Inspected vector preview and emulator English settings. Android splash transition and Spanish placement remain visual checks; no broad matrix or unrelated expensive checks run locally.
- Explicitly authorized Samsung update completed after fresh verified backup/signature/package/version/permission checks: install-r Success, launch ok, installed bytes/version match. All 18 sessions/WAVs and runtime permission grants/flags preserved; only Android's ActivityThread runtime identifier XML changed. Evidence and private backup stay ignored. See VALIDATION.md for hashes, counts and limitations.
- Prior localization commit `728d13f` now has fully completed/successful Actions #10. This UI change is committed/pushed as one focused phase; report its actual asynchronous CI state separately. Recoverable starting point is `728d13f`; preserve subsequent work before any recovery.

## 2026-10-22 - Localization completion and bilingual verification

- Completed the English-default/Spanish migration after first-stage commit `886834f`: 478 strings, five plural keys and 24 note-array entries per language. Resources cover all known application-authored UI text; technical notation, internal keys, historical names and unknown native error details remain intentional exceptions. `DiagnosticText.kt` localizes existing string-only diagnostics without changing protected engines. In-memory name provenance translates missing/orphan labels while preserving identical saved names; no schema or recovery modification.
- Behavior-focused regressions first reproduced nested-error language, long English instrument clipping, stale-language inspection text and an orphan-label/name ambiguity. Minimal presentation corrections pass. Configuration tests preserve exact session JSON, tool associations and PCM; 20 core/three audio files and ViewModel/MainActivity remain unchanged. Root README, toolchain, DSP, WAV lifecycle/recovery, private audio and Samsung data are untouched.
- Local validation: 135 JVM passed / one private replay skipped; debug/release/test APKs, vital lint, JaCoCo, Detekt, resource validator and actionlint pass. There are 49 distinct passing emulator cases across 55 successful executions in en/es/de fallback; mixed failed attempts and targeted retries are recorded honestly in VALIDATION.md. Existing gesture/data assertions remain; only UI text expectations/harness details changed. Selected 25 synthetic images reviewed; TalkBack speech, physical/landscape and exhaustive layout/state coverage remain manual work.
- The initial Spanish extended run encountered a System UI ANR, recovered with Wait, not an app/data reset. Remaining test-only fixture/format/legend assumptions were corrected and their cases rerun. No functional cleanup/refactoring or typed-diagnostic redesign was added. Future diagnostics need resource/adapter entries until separately scoped contract work. Unknown native error details remain verbatim. CI/commit closure follows the authorized push; do not infer aggregate Actions success from individual steps.

## 2026-10-22 — Localization stage 1: interface resources

- English default and Spanish UI resources replace 410 distinct labels/templates, with two note arrays and standard en/es locale config. Compose reads resources directly; native drawing/semantics/event callbacks use captured Context. Presentation adapters keep enum/route/preset/marker identities and historical content unchanged. No custom selector, framework, schema/toolchain upgrade, DSP/WAV/gesture changes or README rewrite.
- Added an English-label regression before production edits; after an emulator startup failure, its retry failed for the expected untranslated label. Three locale tests now pass for en/es/de fallback, including notes and decimals. Updated the prior Spanish-only JVM formatting assumption with explicit en/es/de assertions while retaining geometry checks. Final: 135 JVM pass / one private replay skipped; 19 emulator locale/handoff/persistence tests pass; debug/release/app-test APKs, release lint, JaCoCo, Detekt and resource parity/format validator pass. Evidence/limits in VALIDATION.md.
- This is the first coherent stage of the active full migration. Backend event/diagnostic presentation, plural wording, expanded bilingual visual/accessibility checks and final GitHub verification remain pending; no complete-migration claim yet. Technical architecture/inventory in LOCALIZATION.md. All test data is synthetic/isolated; Samsung and private recordings remain untouched.

## 2026-10-22 — Fix WAV close-failure handoff

- Confirmed the engine caught close errors but always authorized ready/import. The ViewModel could turn that error into a saved-session message. Reproduced with actual writer/Handler/ViewModel completion paths before production edits: four Android tests, three expected failures, normal close/repeated finish passed. Test-only reflection and an owned closed file handle provide deterministic IOException without starting a microphone or adding a runtime seam/library.
- Changed only engine ready/failure routing and the ViewModel terminal failure handler: successful close alone permits ready; failure ends SAVING/CAPTURING, leaves the error visible and retains the same partial without import/delete/retry. Clearing the ViewModel also releases its persistence executor after failure delivery. Existing locked detachment prevents duplicate outcomes. Byte-for-byte preserved writer/store recovery from `71e7d33`, including complete-frame validation and collision/idempotence safeguards.
- Four new tests cover stale/readable checkpoints, normal/repeated finish, live and cleared ViewModels, unchanged existing named flute session/index/audio/associations and later exactly-once recovery. Final emulator run: all 16 relevant cases pass. JVM: 134 pass / one private replay skipped. Debug, unsigned release and instrumented APKs, release vital lint, JaCoCo, Detekt, summaries and actionlint pass. Android execution was not measured by JVM coverage. Exact commands/results in VALIDATION.md.
- No DSP, PitchTrace, charts, unrelated UI, internationalization, dependencies, data schema/version changes, private audio or Samsung operations. Remaining work: isolated sync/handle-close failures, real microphone lifecycle and callback races, OS durability, and any separately authorized journaling of unsaved analysis metadata. Startup recovery remains audio-only; no analysis was invented. Distinct failure outcomes and limits are documented in WAV_CAPTURE_RELIABILITY.md.
- Fix commit `a34583a`, `fix(audio): prevent persistence handoff after WAV close failure`, pushed normally to origin/main. [Actions #9, 39801132988](https://github.com/hmeclazcke/DidgeTaller/actions/runs/39801132988) completed/success for the exact fix SHA (updated 15:08:32 UTC). Downloaded workflow logs corroborate successful JVM/APK/app-test compilation/coverage/Detekt on Temurin 17; downloaded coverage summary matches local exactly. Per-job API metadata remained stale/queued, so no successful individual step status is inferred from that response; final workflow conclusion and actual logs are separate evidence, documented in VALIDATION.md. Previous recovery run #8 is also now confirmed completed/success. Closure is documentation-only with `[skip ci]`; recoverable fix `a34583a`, pre-fix `946320d`, prior recovery `71e7d33`. Preserve later work before recovery.

## 2026-10-22 — Fix complete-frame WAV capture recovery

- Reproduced the desired regression before production edits: owned partial with 1.5 seconds of PCM and a one-second checkpoint imports as 1,000 ms. The import path bypassed repair, then renamed the file so startup recovery could no longer visit it. Chose recovery before import over the earlier rejection-only proposal, as explicitly requested in this task.
- Minimal change in two production files: validate the writer's immutable fixed-format header, tolerate stale/torn size fields, recover all complete physical PCM frames, rewrite both lengths, and reject unsupported/oversized files before mutation. Empty/half-frame partials stay untouched. Import and pending recovery check collisions before repair; UUID, timestamps, timeline, instrument, markers and design associations are preserved. Historical WAV reader/files and separate engine close-failure handoff remain unchanged.
- Updated existing capture regressions and added one Android startup/malformed/idempotence case plus three JVM boundaries. Synthetic PCM and temporary files only. Full JVM: 134 passed / one private replay skipped; final emulator persistence run: 12 passed. Both APKs, coverage, Detekt and summaries succeed. Writer line/branch coverage 100%/82.81%; no Android coverage claim. Initial duplicate-import exception assertion corrected to match the intentional earlier collision check, then final suite passed. Evidence and commands in VALIDATION.md; current behavior and limitations precede the retained history in WAV_CAPTURE_RELIABILITY.md.
- No Samsung/private recording access, new dependencies, app version changes, DSP/UI/refactoring or unrelated fixes. Follow-up remains actual raw-I/O durability and callback/close-error handoff; no complete supported PCM frames are discarded, but an incomplete trailing byte cannot be reconstructed. Existing already-imported stale WAVs require separately authorized investigation rather than automatic migration.
- Fix commit `71e7d33`, `fix(audio): recover complete PCM before importing captures`, pushed to origin/main after successful local validation and staged review. [Actions run #8, 39796325450](https://github.com/hmeclazcke/DidgeTaller/actions/runs/39796325450) targets the exact fix SHA. Initial queued status was recorded/pushed in `04a4624`, `docs(audio): record WAV recovery validation and CI queue [skip ci]`. Subsequently all 19 CI steps completed/success, including JVM/APK/app-test compilation, coverage, Detekt and uploads. Downloaded the 212-file coverage artifact; CI/local summaries match exactly. At 14:41:21 UTC, job/workflow/check-run still report in_progress with no conclusion, so aggregate success remains unconfirmed. Final status documentation also uses `[skip ci]`; no new code/workflow or duplicate execution. Recoverable fix `71e7d33`, pre-fix `7b1e182`; preserve subsequent work first.

## 2026-10-22 — WAV finalization and persistence tests

- Kept production unchanged from `486aa5e` and added seven focused characterizations: four JVM writer/file tests and three Android persistence tests using real preferences/files in UUID-isolated emulator namespaces. No new dependencies, production refactoring, DSP/UI changes or private recording access. Existing AVD used without wipe/reset; every mutation targeted guarded emulator-5554, never Samsung.
- Confirmed/reproduced import-boundary defect: a stale one-second checkpoint header with 1.5 seconds of complete PCM is accepted and renamed .wav; duration/reader expose only one second although all 24,044 bytes survive. Normal pending-part recovery no longer visits it. Characterization is explicitly a known limitation and must change with an intentional fix; production failure behavior was not silently changed. Separate minimal-fix proposal: reject owned partials whose declared fixed-format length mismatches complete PCM length before rename/index, preserving them for recovery. Do not change the general historical WAV reader or discard tail bytes.
- Low-space rejection, initialization failures, repeat close, exact PCM/header preservation, retry after rejection and torn-frame snapshot repair pass. Android collision preservation and rejected incomplete import/repair/single import also pass. Close-failure handoff is confirmed by inspection; real raw write/sync/close faults, microphone stop races, late callbacks and process-kill durability are not reproduced. Details and lifecycle matrix: WAV_CAPTURE_RELIABILITY.md.
- Validation: full JVM suite 131 passed / one private replay skipped (132 discovered), APK/app-test builds pass; emulator three new + eight existing persistence tests = 11 passed. JVM writer line/branch coverage 100%/82.50%, Android capture/data JVM coverage still zero; no instrumented coverage collected. Full unrelated UI/private-fixture instrumentation not run. CI adds only compilation of the instrumented test APK and retains existing checks. Logs/results/limits in VALIDATION.md.
- Test commit `5935337`, `test(audio): characterize WAV finalization and persistence`, pushed normally to origin/main. [Actions run #7, 39792488131](https://github.com/hmeclazcke/DidgeTaller/actions/runs/39792488131), exact SHA: test-and-apk job completed/success, every step passed including new instrumented-APK compilation. Downloaded logs and the coverage artifact; CI/local summaries match. Instrumented tests were executed locally only, not on CI. At final check (14:17:46 UTC), workflow/check-suite aggregate still report in_progress despite the successful sole job; no aggregate success is claimed. See VALIDATION.md for exact evidence and status limits.
- Final record is documentation-only with `[skip ci]`. Recoverable test integration: `5935337`; pre-test state: `486aa5e`. Preserve later work before recovery. No production fix in this task; stale-header import and raw-I/O/callback gaps remain open as documented.

## 2026-10-22 — Initial JVM coverage audit

- Added stable JaCoCo 0.8.15 to existing JVM test tasks and a debug HTML/XML report task. Preserved Kotlin 2.2.21, AGP 8.13.2, Gradle 8.13, JVM target 17 and local/CI JDKs. No coverage threshold, runtime dependency or Android device instrumentation. Kept meaningful production/lambda code in the denominator, using narrow generated-class exclusions and upstream Kotlin/Compose bytecode filters.
- Existing tests ran with the agent: 127 passed and one opt-in private replay skipped, no failures/errors; report and APK build succeeded. Overall 27.59% lines / 27.65% branches, with core 98.24% / 76.54% and Android audio/storage unexecuted by this suite. Component instruction/method counters, chart gaps and prioritized follow-up are documented in COVERAGE_AUDIT.md, with exact evidence in VALIDATION.md.
- Added a standard-library report summarizer and eight manually reviewed pure-function correspondences to Detekt. Source hashes are LF-normalized for Windows/Linux reproducibility. CRAP-L/CRAP-I are limited, explicitly labeled line/instruction approximations; original basis-path coverage is unavailable. Local functions emitted separately, Android callbacks and Compose split bodies are not given scores. The manifest must be reviewed again after source/compiler changes, not automatically refreshed as a claim of validation.
- No new tests, app source changes, private recording access, DSP/UI/navigation edits, Samsung operations, CPD or mutation tooling. Existing synthetic WAV tests remain part of the unchanged JVM suite.
- Integration commit `8d5d576`, `chore(quality): add informational JVM coverage audit`, pushed normally to origin/main. [GitHub Actions run #6, 39785451002](https://github.com/hmeclazcke/DidgeTaller/actions/runs/39785451002) completed successfully on that exact SHA, including JVM tests, APK, coverage, Detekt, summaries and three uploads. Downloaded the 212-file coverage artifact and job logs; CI and local summaries match exactly, including all component counters and eight reviewed method correspondences. Coverage is reproducible on Windows/JBR 21 and Linux/Temurin 17. Exact evidence and remaining limits are in VALIDATION.md.
- Closure is documentation-only with `[skip ci]`; no changes to validated tooling/app. Recoverable integration: `8d5d576`; pre-integration tooling: `1b6a539`. Preserve later work before recovery. Future improvements require separate test/implementation scope and are not performed by this audit.

## 2026-10-22 — Initial Detekt integration and audit

- Integrated stable Detekt 1.23.8 at the Gradle root with explicit production Kotlin sources, default checks plus cognitive complexity, and informational failure policy. Kept the existing Kotlin 2.2.21/Gradle 8.13/AGP 8.13.2/JDK setup. Syntax-only analysis uses Detekt's isolated compiler 2.0.21; type-aware checks are an explicit limitation, not a promised full Kotlin 2.2 compiler integration. Current 42-file source analysis and dependency inspection passed.
- Added a separate two-rule metrics task and a Python standard-library summarizer, retaining normal finding thresholds and avoiding baseline/suppression/formatting changes. Summary validates XML/SARIF counts and paired function metrics. The first run exposed a shared default Markdown output path; disabled that unused output and reran the final configuration successfully, keeping the four intended report formats distinct.
- Audit: 1,499 SARIF warnings across 19 rules. Top ten and all rule counts are in DETEKT_AUDIT.md; 538 measurement observations are kept separate. Risk-first follow-up: capture/close/error handoff and recoverability; detector/PitchTrace decision boundaries; chart projection and viewer state. Documented Compose naming, DSP constants and defensive cleanup catches as context-dependent findings, without blanket suppression or cleanup. No coverage/CRAP calculation.
- Validation: 127 JVM tests passed, one opt-in private WAV test skipped (128 discovered, 18 suites), debug APK built; both analysis tasks, dependency inspection, summary generation, actionlint and diff checks passed. No app source, version, DSP/UI, stored data, private recordings or device changes. CI now runs the informational audit and uploads reports alongside the existing test/build checks. Exact commands/results/limits are in VALIDATION.md.
- Integration commit `5e30e56`, `chore(quality): integrate informational Detekt audit`, pushed normally to origin/main. [GitHub Actions run #5, 39782333774](https://github.com/hmeclazcke/DidgeTaller/actions/runs/39782333774) succeeded on that exact SHA, including tests, APK, both Detekt tasks, summary and both uploads. Downloaded the ten-file Detekt artifact; its complete summary matches the local result, including counts by rule and all 269 paired metrics, confirming reproducibility on Linux/Temurin 17 as well as Windows/JBR 21. Evidence and existing warnings are recorded in VALIDATION.md.
- Final commit is documentation-only with `[skip ci]`, recording that completed run without repeating unchanged tooling/app checks. Recoverable integration: `5e30e56`; pre-audit tooling: `3dc3ddb`. Preserve later work before recovery. Later engineering improvements require separate characterization and scope; none implemented here.

## 2026-10-22 — Install repository-local ub-mode

- Added the instruction-only `ub-mode` skill at `.agents/skills/ub-mode/SKILL.md`, implementing the user's Uncle Bob-inspired methodology and project-specific safety constraints. Kept optional Kotlin analysis/mutation tools optional and uninstalled. Added a short AGENTS.md reference, explicit `$ub-mode` invocation, and prospective English writing policy; retained existing rules and historical text.
- Native Codex CLI skill discovery passed from root and nested Kotlin source directory: enabled repository skill, expected YAML metadata, and no loading errors. The Python helper lacked PyYAML; no installation or false claim of execution. Instructions, manual discovery steps, validation limits, and evidence locations are recorded in VALIDATION.md.
- Installation commit `bd7d42d`, `chore(agent): install repository-local ub-mode skill`, pushed to origin/main. GitHub Actions run #4 `39779474236` completed successfully, including JVM tests, APK compilation, and artifact upload. No workflow changes were needed.
- Scope remains agent configuration and these existing records only: no application, Gradle, DSP, UI, Samsung, or private fixture changes. Final documentation-only result commit uses `[skip ci]` to avoid repeating the same validated build. Installed-skill recovery point: `bd7d42d`; prior baseline: `dc61960`. Preserve subsequent work before recovery.

## 22/10/2026 — Corrección exclusiva de GitHub Actions

- Causa confirmada en logs del run39767278472: setup-android v3 pide `tools`, retirado del SDK; Gradle no llegó a ejecutarse. Se actualiza a v4 con `platform-tools` explícito y se migran checkout/setup-java/upload-artifact a releases estables con Node24, verificadas en sus repositorios oficiales. JDK17 y versiones/tareas del proyecto conservados.
- Se habilita ejecución de gradlew solo en Ubuntu mediante un paso chmod: modo100644 comprobado, sin modificar el wrapper ni su modo local. actionlint1.7.12, comparación exacta de comandos Gradle y revisión de diff aprobados. Commit `fd0a558`, `fix(ci): replace obsolete Android tools and Node 20 actions`, subido a origin/main.
- Run #3 `39776045256` del mismo SHA: success en todos los pasos, incluidos tests JVM, assembleDebug y artefacto APK12424756817. Desaparecen el fallo por tools y las advertencias Node20; no aparece un siguiente error. Persisten avisos de deprecación de sdkmanager/jvmTarget/ArrowForward y migración de Ubuntu, fuera de esta corrección y sin fallo. Evidencia y límites en VALIDATION.
- Registro final solo documental con `[skip ci]` para evitar repetir el mismo workflow y código ya aprobados. Se mantiene la ejecución normal de tests y build en los siguientes pushes sin esa marca. Punto recuperable de CI: `fd0a558`; conservar primero cualquier trabajo posterior.
- Alcance: workflow y evidencia documental. Sin cambios de app, datos privados ni Samsung. Fuente recuperable anterior `ab5ca26`; logs de diagnóstico y herramientas locales ignorados en checkpoints.

## 22/10/2026 — Repositorio privado en GitHub

- Pedido explícito de vincular y subir el Git existente, conservando historial y código. Estado inicial limpio en `main`,12 commits, HEAD `1fbfb12`, sin remotos. Auditoría completa de rutas y contenidos históricos, incluidos eliminados:464 blobs, sin WAV privados, respaldos, claves Android ni credenciales detectados. Capturas documentales/ícono y wrapper revisados; coincidencias `password="false"` descartadas como atributos de UI, no secretos.
- Git Credential Manager y API verifican sesión `hmeclazcke`, permiso de escritura y repositorio `hmeclazcke/DidgeTaller` privado antes de configurar o subir. El sandbox inicialmente bloqueó credenciales, red y `.git/config`; ejecuciones autorizadas fuera de él completaron las verificaciones. No hizo falta login. `origin` agregado con URL HTTPS; remoto vacío confirmado antes del primer push.
- `git push --set-upstream origin main` terminó correctamente, sin force push. Los12 commits quedan preservados en GitHub; HEAD local, `origin/main` y referencia remota coinciden en `1fbfb127adc1319f8e136c08d6d8c68fa2d4ea09`, diferencia0/0 y árbol limpio antes de este registro. Se agrega únicamente un commit documental para dejar evidencia según AGENTS.md; se cotejará nuevamente el hash remoto tras publicarlo.
- Informe detallado en VALIDATION. Scripts/resultados de auditoría y hojas de contacto bajo `checkpoints/`, excluidos del repositorio; no se borraron privados ni objetos Git. App idéntica a la base, sin compilaciones, pruebas de dispositivos ni cambios funcionales en esta iteración. Código recuperable3.2.6: `4330e71`, conservado junto con los commits históricos; mantener primero trabajo posterior ante cualquier recuperación.

## 21/10/2026 — 3.2.6, diagnóstico reproducible

Cierre: fuente/APK3.2.6-beta1/12 en `4330e71`;128 JVM,44 UI distintos más8 repeticiones y21 capturas revisadas. Regresión UI40/42 inicial, dos expectativas textuales actualizadas y repetidas con éxito; no se repitieron los40 casos ya aprobados con el mismo APK. Prueba JVM de toot ajustada para exigir continuidad interna y corte del borde débil intencional, sin perder puntos. Lectura del índice Samsung confirma652 Hz/confianzas exactamente iguales al replay y mínimo35,247Hz en19:17. Series guardadas verificadas también en emulador, con WAV silencioso de soporte. Samsung install-r/Success, arranque correcto,69 hashes intactos/18 sesiones/18 WAV legibles después del respaldo fresco; informe v326-samsung-update. No hay corrección DSP demostrada para la «I» ni validación física nueva. Cierre recuperable en checkpoints/latest.txt; audios/diagnósticos privados fuera de Git.

Etapa D validada: margen STFT medido, unidad redundante retirada, números compactos y marcas X sin colisiones. Mismo1080px/420dpi: datosSTFT+9,54%; ancho Fundamental ya era correcto y se conserva. 2 JVM nuevas y UI de ejes a tres configuraciones aprobadas; título completo al traerlo al viewport, sin modificar scroll/insets. Comparación con APK instalado3.2.5 obtenida solo en lectura; capturas reales revisadas. Corregida la etiqueta inicial que confundía densidad dpi con ancho dp; la matriz final ajusta ambos explícitamente.

Etapas B–C: ninguna de cinco alternativas acústicas demostró una mejora suficientemente fiable; YIN y umbrales se conservan. El replay nominal no produce33Hz; la extensión experimental sí encuentra períodos33Hz fuertes, sin demostrar una octava incorrecta. Se separan dos enlaces débiles entre grupos respaldados de36/66Hz sin borrar puntos; Medir explica señal presente sin frecuencia confiable. Tests focalizados, compilación y4 UI en emulador aprobados; capturas de curva/estado incierto revisadas. La vocalización física sigue pendiente. Corregida codificación de acentos de esta documentación tras detectar la conversión del pipe de PowerShell; no se reescribe el commit previo.

Preservada base `309135e`. Diagnóstico opcional del estimador real y decisión LivePitch compartida con ViewModel. Tres WAV privados íntegros; pérdidas por baja periodicidad reproducidas, no por renderer. Candidatos largos de 19:17 requieren comparación acústica antes de cualquier corrección. 9 JVM focalizadas + reproducción privada y assembleDebug aprobados; 652 ventanas iguales a la base. Informes locales ignorados, sin cambios DSP aún.

# Registro de trabajo

## Cierre verificable 3.2.5 — 21/10/2026

- Código final `42924ec`:120 JVM,42 casos UI distintos y6 repeticiones de matriz sobre el mismo APK SHA `0a9bbfb7a780900432b3872e693031d38b3af924743ea0dc68bfc112ea1a149a`. Se repitió la regresión tras corregir el barrido detectado visualmente; fallos/experimentos previos no cuentan como aprobados.22 capturas nativas revisadas y comparación real con APK3.2.2/3.2.4; métricas y límites en VALIDATION.
- Samsung actualizado por install-r/Success y arranque correcto; respaldo fresco18:48:36 de64 archivos/15 WAV,64 hashes intactos tras actualizar,15 sesiones/WAV indexados/legibles, APK instalado idéntico. Sin operaciones destructivas ni instrumentación física. Cierre recuperable fuente+hashes en `checkpoints/after-3.2.5`; anterior `before-3.2.5` conservado.
- La «I» no se declara resuelta: señal real ambigua y un transitorio perdido, sin frecuencia física de referencia para decidir entre períodos alternativos. No se incorporó el experimento YIN. Próximo paso musical: prueba de vocales con referencia independiente y eventos delimitados; también siguen pendientes gesto físico/audio audible/15min/TalkBack.

## 3.2.5 — enlaces sin barridos espurios, 21/10/2026

- La inspección de las capturas detectó un barrido desde un ataque débil aislado91Hz hacia el tramo74Hz. Se conserva su punto original, pero un salto≥300c no se une si un extremo tiene periodicidad<0,90 y carece de apoyo temporal dentro de±4 lecturas/1s. No se elimina la lectura ni se aplica a flauta; toots/retornos respaldados y ataques moderados permanecen conectados. Dos regresiones nuevas verifican ambos lados del límite funcional, sin forzar notas.
- 120 JVM sin fallos/errores/omitidos y ambos APK compilados. Foco anterior de10 casos de gráficos aprobado; se ejecuta nueva regresión del APK final después de la revisión visual. Prueba UI de Medir reforzada: ahora exige nota/Hz visibles y timeline del callback, no solo estado del ViewModel; fixture sintético explícito, sin AudioRecord ni WAV real. Ajustada se captura con el panel cerrado.
- El usuario ubicó la «I» en los últimos15s de la sesión51,5s. Contraste de PCM existente en solo lectura:18/81 ventanas aceptadas,25 bajo RMS0,005 y38 con energía pero periodicidad insuficiente; mediana de las aceptadas64,53Hz. Autocorrelación detecta además un transitorio fuerte rechazado por YIN. Experimento aislado de ventana reciente recuperó alguna lectura pero podía admitir≈97Hz ante período fuerte≈65Hz; rechazado, sin incorporar al detector. No afirmar corrección física de la «I» ni atribuir todos los huecos a silencio/error.
- Corrección de conteo: el foco inicial AcousticReliability+FluteAnalysis tuvo12 pruebas(6+6), no14 como dice el mensaje del commit17a8420; ese commit se conserva sin reescribir. Los resultados XML completos son la fuente de verdad.

## 3.2.5 — recuperación de Fundamental y colores, 21/10/2026

- Recuperada selectivamente la semántica de color del código3.2 (ZIP completo), centralizada en `PitchTrace`: variación entre extremos medidos y periodicidad mínima de ambos. Verde≤25c/≥0,90; amarillo≤60c,0,80–0,90 o falta de vecino; naranja>60c o<0,80. Cursor permite consultar ambas medidas; enlace sobre omisiones es discontinuo. Sin cambios al espectrograma/gestos/cámaras/transporte.
- Retirados el modo dominante global y el descarte de bordes≥100c junto a ausencias. Presentación admite las estimaciones que YIN acepta desde0,65, sin cambiar resumen≥0,70 ni originales. Exclusión contextual limitada a retornos débiles aislados, períodos bajos breves con retorno medido a ambos lados y saltos≥600c sin apoyo en±4 lecturas/1s frente a dos vecinos coherentes; no afirma error físico. General usa corroboración temporal o periodicidad≥0,90 si cubren al menos la mitad de las lecturas; notas/toots respaldados y Mostrar todos preservados.
- 36 JVM focalizadas aprobadas y luego8 de color tras hacer amarillo el punto sin vecino; APK/app-test compilados. Tres casos UI nuevos aprobados por focos: píxeles de los tres colores/datos intactos, Medir con PCM controlado y perfiles, series reales D2/Eb2/reciente con WAV silencioso de soporte. Fallos previos conservados en checkpoints: expectativa antigua de filtrar una excursión sin caída de evidencia, dos guardas que ocultaban el toot/retorno corregidas, import del test, reflexión sobre estado delegado y fixture de1s insuficiente para cinco lecturas estables corregido a1,5s. No contar ejecuciones fallidas como aprobadas.
- Comparación JVM ejecutada sobre código real compilado de3.2.2/3.2.4, además del actual. Re·D2:43/40/47 puntos principales de49 frecuencias guardadas; Mib·Eb2:26/20/25 de30; reciente:121/117/122 de125. La reciente dura51,5s reales; no se rellenan150 ausencias. La variante inicial que amplió demasiado General fue rechazada; rango corregido reciente≈58,7–83Hz. La regresión completa, matriz y actualización segura se registran al cierre.

## 3.2.5 — fase de captura verificada, 21/10/2026

- Base limpia `56c2e81`; copia recuperable de 73 archivos con SHA-256 en `checkpoints/before-3.2.5/source.zip` y `manifest.json`. Código completo confirmado en ZIP3.2, checkpoint3.2.2 y before3.2.4; after3.2.3 es parcial, after3.2.4 sí contiene fuente. No rollback global.
- YIN y MicrophoneEngine idénticos entre3.2 y3.2.4; mismo umbral DSP0,65/RMS0,005 y configuración didgeridoo. Se conserva YIN intacto. Extraída la ventana/cadencia real del micrófono a `PitchStream` para probar ese mismo camino con PCM por chunks; parámetros congelados por captura y resultados pendientes de una captura anterior no llegan a otra sesión/perfil.
- 12 pruebas focalizadas AcousticReliability/FluteAnalysis y assembleDebug aprobados. Seis casos nuevos cubren balances armónicos/vocales sintéticas, amplitud/transiciones, silencio/ruido/reanudación, toot en rango ampliado, flauta y lecturas parciales/cadencia. No se atribuye validación física de la «I».
- Respaldo fresco Samsung solo lectura:64 archivos/15 WAV con hashes verificados. Sesión reciente51.500ms/275 lecturas,125 con frecuencia,117 principales/15 segmentos en3.2.4; su WAV confirma51,5s. En ventanas PCM cercanas hay amplitud baja y periodicidad insuficiente: no demuestra una regresión del detector ni identifica qué tramo correspondía a la vocal.

## Adopción de Git profesional — 21/10/2026

- Revisados estado, identidad configurada/efectiva y exclusiones antes de crear commits. Se conservó el commit histórico `4f90893` y toda la implementación existente, sin cambios al código de la app.
- Punto de partida `3f0510f`, `chore(project): checkpoint existing 3.2.4-beta1 state`: 143 archivos de fuente, tests y documentación/evidencias revisadas. Preparación por rutas explícitas; sin WAV privados, respaldos, builds, cachés ni credenciales.
- Validación nueva: 104 pruebas JVM/11 suites sin fallos, errores u omitidos; APK debug compilado. JBR21.0.11, SDK35. No se repitieron pruebas instrumentadas ni se operó sobre dispositivos.
- Reglas permanentes incorporadas al inicio de `AGENTS.md`; `.gitignore` ampliado para audio privado, fixtures locales, credenciales y cachés Kotlin/nativas. La guarda de selección rechazó inicialmente un caché `.salive` generado durante Gradle; se excluyó de la selección sin borrar archivos. No hubo fallos de compilación o tests.
- Verificados 34 casos de exclusión, seis controles de inclusión y WAV en mayúsculas mediante `git check-ignore`; revisión de whitespace y ausencia de privados/generados rastreados aprobada. Segunda fase limitada a cinco archivos de política/documentación, con `app/` idéntico al punto de partida; no se repiten pruebas de app por esos cambios.
- Próximas fases: commits pequeños después de validar cada funcionalidad, revisión del índice antes de cada commit e informe final de hashes/estado/recuperación. No push ni reescritura del historial. Las validaciones físicas pendientes de 3.2.4 siguen pendientes.

## Reparación selectiva de Fundamental 3.2.4 — 21/10/2026

- Baseline preservada en `checkpoints/before-3.2.4`, sin reset/stash/commit global. Comparación selectiva contra3.2.2: regresión por umbral0,85 y corte automático de enlaces. Revertidos; presentación0,70 con puentes limitados entre extremos medidos, sin reconstruir frecuencias ni alterar originales.
- Retornos aislados/períodos bajos breves quedan inspeccionables sin destruir tendencia/General; silencio claro/largo conserva huecos. Bordes exigen degradación de confianza; evita descartar retorno legítimo del toot, detectado en el primer foco fallido. Se actualizaron expectativas de tests que exigían la política rechazada.
- Casos reales desde respaldo local en solo lectura, WAV silencioso en sandbox de emulador: Re·D2 recupera40 puntos/33 enlaces frente a17/15; Mib·Eb2 recupera20/19 frente a12/5. Comparación adicional contra APK3.2.2/8. No afirmar que cada descarte sea error del detector ni que los huecos reales deban rellenarse.
- Eje numérico con unidad única Hz/nota secundaria; reserva115,55→74,55px y datos+5,33% en detalle comparable. Leyenda breve y sin «N fuera» en zona principal. Solo4 archivos main modificados,39 idénticos; sin cambio de motores/cámaras/persistencia.
- Gate104 JVM/build aprobado;39 UI distintos(1 comparación real+36 regresión+2 rotación),6 repeticiones de matriz360/420dp y fuentes1,3/1,5.20 capturas revisadas, índice `docs/screenshots/v324-final/README.md`. Primer baseline lanzado antes de completar APK de prueba falló por clase no disponible; se conserva el fallo y repetición limpia, sin contarlo aprobado. Validador auxiliar de logs corregido para aceptar el singular «1 test»; no fallo de la app.
- Samsung `R5CT21KTQ8W`: respaldo fresco `samsung-before-3.2.4-20261021-152023`,61 archivos/13 WAV; install-r/Success y arranque correcto, APK/firma compatibles.61 hashes intactos,13 sesiones y13 WAV indexados/legibles a15:20:53; informe `v324-samsung-update.json`. Captura física negra, sin afirmar inspección visual allí ni solicitar desbloqueo. Sin instrumentación/captura de audio/reproducción/clear/uninstall/restauración en el teléfono.
- Cierre recuperable en `checkpoints/latest.txt`; previo3.2.3 y modificaciones de Git conservados. Pendiente validación musical/física de eventos brevísimos, afinador independiente, pinch físico, sincronía audible,15min/batería y TalkBack completo. Los huecos reales no se rellenan para mejorar la estética.

## Fundamental Precision 3.2.3 — 21/10/2026

- Checkpoint previo `checkpoints/before-3.2.3`: fuente/docs/estado/diff preservados; Git con cambios anteriores. Sin reset/stash ni commit que los mezcle.
- Causas: aceptación visual0,70 conectaba periodicidad débil no armónica y colas junto a ausencias; percentiles recortaban toots confiables raros; eje medía etiqueta ficticia. Diagnóstico de13 WAV/índice real en solo lectura, sin asumir error en todos los saltos.
- `PitchTrace` común: confianza de presentación0,85, retorno no armónico/bordes inciertos, originales intactos y huecos sin interpolación. General incluye cada confiable; Ajustada robusta. Textos reales medidos/origen fijo durante gesto, curva verde en todas las superficies y leyendas breves.
- Cierre:97 JVM,38 instrumentados distintos (3 nuevos+33 regresión+2 rotación), matriz360/420dp y fuentes1,3/1,5;13 capturas inspeccionadas. Eje189,825→115,550px; datos detalle+10,68%. Recorrido del cursor corregido tras buscar fuera del zoom. Corrida agrupada interrumpida en rotación excluida;33+2 limpios por separado. Detalles y logs `v323-*` en VALIDATION.
- Samsung: install-r/Success y arranque; firma compatible y APK instalado idéntico. Respaldo fresco61 archivos/13 WAV;61 hashes sin cambios,13 sesiones y13 WAV indexados/legibles a14:42:50. Sin instrumentación, clear, uninstall, restauración, captura ni reproducción física. Checkpoint recuperable en `checkpoints/latest.txt`.
- Límites/próximo paso manual: confianza no calibrada, amplitud no guardada por lectura, clasificación de eventos brevísimos imperfectos no infalible. No modificar motores por presunta nota real. Validación física de afinador/gestos/audio/15min/TalkBack pendiente.

## Graph Intelligence & UX Stability 3.2.2 — 21/10/2026

- Estado previo y fuente completa preservados en `checkpoints/before-3.2.2`; se verificaron76 JVM previos y logs de32 instrumentados históricos. No reset/stash/commit de cambios ajenos.
- Causas corregidas: dominio mínimo hardcodeado500Hz, cámara de detalle/fullscreen compartida y retenida fuera de sesión, confianza de periodicidad insuficiente para interpretar retornos aislados, generación de notas desde0,001Hz que redondeaban a0.
- General robusta/domain por datos, estado por contexto con selección común, incertidumbre contextual sin corrección destructiva, etiquetas MIDI0–127. Lecturas originales intactas. Script de diagnóstico PCM en respaldo, sin tocar el teléfono; no cambio de detector por evidencia ambigua.
- Cierre:86 JVM,35 instrumentados distintos + matriz360/420 y fuentes grandes, capturas revisadas, APK3.2.2-beta1/8. Samsung install-r/Success; 12 WAV y datos de usuario con hashes intactos. Detalles/fallos reales en VALIDATION.
- Checkpoint final indicado en `checkpoints/latest.txt`, sin ZIP masivo ni audios personales. Pendientes validaciones físicas de gesto/afinador/audio/15min/TalkBack; clasificación contextual admite ambigüedades y mantiene originales inspeccionables.

## Stability & Precision 3.2.1 — 21/10/2026

- Git ahora disponible con cambios anteriores de3.2 sin commit; se preservaron antes de editar en `checkpoints/before-3.2.1` (fuente, estado y diff), sin reset/stash ni commits que mezclen trabajo previo.
- Corregidos back stack de Vista amplia y propiedad de captura, zoom radial/ancla+pan, límites y márgenes estables, origen fijo de STFT/raster flotante, reset sin demora, serie/escala/segmentos comunes de Fundamental y panel de lectura completa. Sin nuevos instrumentos/simuladores/dependencias.
- Diagnóstico privado del respaldo: saltos de frecuencia ya presentes, algunos con confianza alta; conservados sin suavizado/corrección destructiva. Reanálisis no sustituye lecturas capturadas. VISUAL_VERSION3 perezosa; análisis VERSION2 y motores conservados.
- Gate final:76 JVM,32 casos instrumentados diferentes (5+25+2), focos360/420 con fuente grande, capturas revisadas. Fallos y timeout inicial conservados sin contarlos como aprobados; detalles en VALIDATION.
- Samsung install-r/Success, firma compatible, respaldo fresco de48 archivos/10 WAV y hashes de datos del usuario intactos. No captura/reproducción ni pruebas instrumentadas físicas.
- Pendiente manual: pinch/pan físico, afinador independiente, audio/sincronía,15min/batería/TalkBack. Checkpoint de cierre pequeño indicado en `checkpoints/latest.txt`; no nuevo ZIP masivo.

## Cierre Acoustic Studio 3.2 — 21/10/2026

Implementación completa de los tres entregables, conservando arquitectura, navegación, WAV progresivo, laboratorio y cachés derivadas. Sin Git disponible; checkpoints ZIP antes de 3.2 y al cerrar fases A/C, más cierre final indicado en `checkpoints/latest.txt`.

- A: visor compartido de cuatro gráficos, barras compactas adaptativas, leyenda integrada, opciones contextuales e insets únicos con restauración de barra de estado/orientación. Iteraciones visuales reales hasta superar objetivo. Samsung misma sesión: **306 / 1080 px (28.3%) → 773 / 1080 px (71.6%)**, área de datos sin ejes. Sin datos inventados/ocultación de silencios.
- B: `Music.pitch`, `PitchAxis`, etiquetas medidas Hz/nota secundaria y equivalencias ≈, cursor/cents, autoescala y aviso de valores fuera del viewport; STFT solo Hz.
- C: `InstrumentProfile`, parámetros YIN/captura por perfil (flauta 200–1600 Hz a tasa completa, ventana8192/cadencia93 ms), protección localizada contra octava, confianza/pausas, pista offline más corta; perfil persistido y caché v2. Biblioteca/detalle/Medir actualizados, Laboratorio claramente didgeridoo. Lectura antigua sin campo → Didgeridoo sin reescritura.
- D: **67 JVM**, **27 casos instrumentados distintos** solo emulador, matriz 360/420 dp y fuentes ampliadas; selección temporal, gestos, reproductor, persistencia antigua/nueva y captura emulada. Corregidos snackbar, tiempo truncado y pruebas ambiguas; dos ejecuciones de rotación fallida/interrumpida conservadas, seguidas por cierre limpio 2/2. Detalles/logs en VALIDATION.
- APK 3.2.0-beta1/6 actualizado con install-r/Success en Samsung `R5CT21KTQ8W`; firma comprobada y respaldo fresco de 27 archivos/8 WAV. Todos los datos de usuario conservan hashes; solo runtime y nuevos derivados documentados. Capturas físicas antes/después y matriz revisadas: `docs/screenshots/v32-final/README.md`.

Pendiente de prueba manual, no de implementación: flauta/PVC reales y afinador, sincronía audible, soplido/ataques extremos, 15 minutos/batería y recorrido TalkBack completo. No se calibró precisión física ni se simuló una flauta. Capturas iterativas archivadas en checkpoints para no saturar docs.

## 2026-10-20 — Cierre de Didge Taller 3.1 beta1

- Implementados visor NavHost, matemáticas puras y gestos compartidos, cámaras por sesión, cuatro adaptadores, seguimiento de posición real y conservación al rotar. Laboratorio con portada/destinos y barras fijas; Biblioteca/detalle/acciones/formatos/permiso revisados. No se rehicieron algoritmos ni modelos persistidos: 26 módulos anteriores idénticos al ZIP inicial.
- Auditoría antes/después en emulador: corregidos Marcar fin vertical, herramientas ocultas, escala de F0, cabeceras excesivas, fechas repetidas, falta de pausa en espectro y permiso obsoleto. También se reprodujo un fling corto que eliminaba por velocidad: nuevo veto por distancia al soltar, reversión y Deshacer probados.
- Gates: 57 JVM y APK aprobados. Suite 22/22 más focos completan 24 casos funcionales distintos; matriz 360/420 dp normal/ampliada, horizontal, gesto de borde y tres botones. TalkBack solo smoke parcial. WAV sintético de 900 s con cancelación/zoom real. Precisión audible y rendimiento físico pendientes.
- APK **3.1.0-beta1 / 5**, SHA `a53dacb774288740967e2244214b2684db49997ba6e8d55f9399afd1516d259e`, instalado Samsung SM-G990E mediante install -r/Success, arranque sin fatal. Respaldo fresco en `backups/latest.txt`; 7 WAV/índice/ajustes/relaciones intactos. Dos metadatos automáticos identificados y comprobados. Sin borrado/restauración de datos del usuario ni instrumentación física.
- Archivos principales: ui/ImmersiveViewer, PlotSurface, PlotGestures, ExplorerState, SessionExplorer, TallerApp, TallerNavigation; core/PlotViewport; audio/SessionWorkspace; TallerViewModel/MainActivity; pruebas de viewport, navegación, rotación y auditoría. Documentación de arquitectura en IMMERSIVE_VIEWER, resultados en VALIDATION y UI_AUDIT.
- Recuperación: checkpoint validado indicado en `checkpoints/latest.txt`. Git ausente, sin commits inventados. Próximo paso exacto: prueba manual del usuario en Samsung descrita en VALIDATION; no rehacer fase A ni ejecutar instrumentación sobre el teléfono.

## 2026-10-20 — Checkpoint 3.1: auditoría e integración del visor

- Leídos AGENTS, contexto/documentación, fuentes y pruebas. Sin Git en esta carpeta; respaldo previo ZIP en checkpoints. Línea base: 44 JVM y assembleDebug aprobados.
- Inventario real a 360 dp/fuente 1,3: Medir, denegación, captura silenciosa, foco, diseñador/perfil/simulación, espectro vacío, búsqueda/progreso, guía, listas y detalle. Reproducidos el botón «Marcar fin» vertical y la dificultad de alcanzar pestañas dentro de formularios largos.
- Implementados viewport/proyección puros, gestos compartidos en dos ejes y adaptadores gráficos; visor como destino NavHost, cámara/selección retenidas, navegación de herramientas del Laboratorio, resumen compacto, acciones adaptables, referencia de color fija por exploración. Motores originales y almacenamiento de sesiones sin cambios de esquema.
- Primeras capturas posteriores: 4 gráficos a pantalla completa y «Marcar fin» horizontal. Auditoría visual llevó a compactar la barra para que las herramientas permanezcan visibles.
- Pruebas nuevas encontraron redondeo de ticks logarítmicos en borde (corregido), fixture de outlier fuera del rango válido (corregido), orden incorrecto de pinch en test (corregido) y cierre al dibujar un viewport transitoriamente colapsado durante rotación (corregido, pendiente foco).
- En curso: focos de visor y rotación real de MainActivity, luego matriz visual. No se instaló todavía esta versión en Samsung ni se ejecutaron allí pruebas. Resultados finales se registrarán por separado; no contar las ejecuciones fallidas como aprobadas.


## 2026-10-20 — Implementación integral 3.0 beta1 cerrada

- Se completaron herramientas funcionales de B–F reutilizando A: STFT/onda/espectro interactivos, selección/seek/bucle, comparación A/B, asociaciones diseño/sesión, marcadores manuales, papelera durable/Deshacer y captura progresiva con límite inicial 15 min. No hay Git; checkpoint previo en `checkpoints/`. Comparación binaria confirma 11 archivos de análisis/detector/motores sin cambios.
- Archivos principales nuevos: core AudioVisuals/ProgressiveWavWriter/SpectralPalette; audio SessionPlayer/SessionWorkspace; data AnalysisCache/TrashStore/SessionToolsStore; UI AcousticCharts/SessionExplorer/ComparisonScreen/LibraryTools. Se integraron ViewModel, navegación, tarjetas, persistencia compatible y exportación SAF por bloques en IO. No se añadieron dependencias ni permisos. Claves históricas y fórmulas acústicas/evolución conservadas.
- **44/44 JVM**, APK app/test compilados; **18 casos instrumentados distintos aprobados solo en emulador** mediante suite + focos, no una sola corrida final de 18. Casos de captura real AudioRecord y onStop, navegación/scroll/release, legado, papelera/Deshacer/recarga, cache/hashes, análisis/gráficos/zoom/fragmento/seek/pausa/bucle/comparación, asociación/simulación/marcadores al reabrir. Historial exacto de ejecuciones/fallos en VALIDATION y logs `app/build/`.
- Fallos corregidos: acceso al Application en helper de espacio y tipos de una aserción; error duplicado entre análisis y visualización, scroll de aserción en detalle ampliado; encadenamiento de seeks/bucle corregido coordinando OnSeekCompleteListener con un solo salto pendiente (regresión pasó); onStop no marca como cancelada una visualización terminada. La prueba de asociación espera el cierre del snackbar antes de tocar un control que quedaba detrás; verificó diseño y persistencia real.
- WAV 900 s: 79.380.044 bytes escritos/releídos sin PCM completo en RAM. STFT virtual 900 s: viewport de 262.144 bytes de celdas y ~3,8 s PC; no es medición de batería/AudioRecord sostenido en Samsung. Captura real del emulador guardada en `docs/screenshots/v3-spectrogram-synthetic-66hz.png`, señal sintética identificada.
- Samsung SM-G990E/R5CT21KTQ8W actualizado de forma no destructiva con `install -r`: **Success**; firma/ID compatibles, versión **3.0.0-beta1/4**, arranque Status ok, PID **24724**, sin AndroidRuntime fatal en la consulta. APK final **11287565 bytes**, SHA-256 **0EE9820862E51DEF416859576DC2339E592C409A410E96A69F6A765044650C87**; base.apk instalado coincide. Índice local_data, ajustes y **5 WAV** conservaron SHA-256. Solo cambió IDSCount de una preferencia interna de Android; no se confundió con datos del usuario. Respaldo local verificable en `backups/latest.txt` y DATA_SAFETY. No se instrumentó/desinstaló/limpió/restauró el Samsung ni se inició audio real.
- Documentación actualizada: AGENTS, README, ROADMAP, VALIDATION, arquitectura, AUDIO_ANALYSIS, HARMONICS, DIDGELAB_STUDY y capturas. Nuevas SESSION_EXPLORER/DATA_SAFETY documentan algoritmos, escalas, memoria, cache, reproducción/papelera, respaldo y límites.
- **Continuación exacta:** no hay un fallo de compilación/test pendiente ni una fase B–F esperando implementación básica. Priorizar la prueba manual del WAV físico ~69,6 Hz y PVC, sincronía audible/seek/bucle, grabación sostenida 15 min y accesibilidad; después contraste independiente de audio/simulador y estudio con grabaciones etiquetadas. El bucle no es gapless/sample-accurate; marcadores no son clasificador; reanálisis F0 35–220 Hz. No declarar esas validaciones físicas completadas ni reescribir los motores sin evidencia. Instrumentación siempre y solo emulador; preservar el respaldo y datos del teléfono.

## 2026-10-20 — Validación de A e instalación física autorizada

- Se revisó la integración existente de Pcm16WavReader, SpectralAnalysis, HarmonicAnalysis, WavAnalysis y RecordingAnalysisPanel. Sin errores comprobados en algoritmos: no se cambió producción ni se avanzó fase B. No hay Git para consultar diff.
- Se añadieron dos tests numéricos de 70 Hz y mezcla 70/140/210 con H1 débil, niveles relativos y métricas; se amplió la prueba de WAV recuperado con reproducción/pausa posterior y se añadió prueba UI de progreso/Cancelar sobre archivo sintético largo aislado. 33/33 JVM pasaron; APK debug y de pruebas compilaron. Tres pruebas focalizadas pasaron únicamente en emulator-5554; no se repitieron suites sin una hipótesis nueva.
- Samsung SM-G990E, R5CT21KTQ8W `device`: `install -r` → Success. Inicio Status ok, proceso activo, sin AndroidRuntime en el intervalo inspeccionado. SHA del APK instalado coincide con el local y SHA del índice + 4 WAV sin cambios antes/después. Sin instrumentación, limpieza, desinstalación, grabación ni cambios en sesiones físicas. El equipo estaba en reposo/NotificationShade; interacción visual y audio audible quedan para el usuario.
- Se documentaron tolerancias, comandos/hash y pasos para abrir su grabación de 69,6 Hz, encontrar Analizar grabación debajo de Audio y reproducir después. Véase VALIDATION. El APK sigue 2.1.0-beta1/3, ahora con fase A instalada en Samsung.
- Esta tarea termina en validación/instalación. B queda pendiente y requiere una tarea posterior; no iniciar nuevas visualizaciones durante esta entrega.

## 2026-10-20 — Didge Taller 3.0, fase A cerrada

- Se inspeccionó el proyecto existente, incluyendo la navegación ya corregida; línea base Gradle aprobada. La carpeta no tiene `.git`: no hubo diff ni commits. Se organizó A–F y sus dependencias al inicio de ROADMAP; se conserva el límite de captura hasta implementar WAV progresivo.
- Núcleo nuevo: `Radix2Fft` extraído de la FFT propia existente, `Pcm16WavReader` de solo lectura por bloques, `SpectralAnalysis`/`PowerSpectrum`, `HarmonicAnalysis` y `WavAnalysis`. Hann periódica, PSD digital de un lado, promedio ponderado con cola parcial, picos interpolados, candidatos con tolerancias y métricas explícitas. No se portó código externo ni se añadieron dependencias. `PitchDetector` conserva su método de periodicidad y puede omitir gráficos; `SessionAnalysis` admite rango explícito manteniendo defaults/formatos.
- Integración real: `TallerViewModel` ejecuta reanálisis cancelable y rechaza callbacks obsoletos; `RecordingAnalysisPanel` ofrece Analizar grabación, resultados/errores/progreso sobre el WAV completo, incluso recuperado. Centroide y bandas se precalculan en worker. Resultado solo en memoria; WAV, índice y resumen anterior se conservan. No hubo migraciones ni cambios en simulación/evolución.
- Pruebas: **31 JVM** y **10 instrumentadas solo en emulador**; después del ajuste de métricas en worker se repitió el caso recuperado (1/1). Compilación final APK app/test exitosa. Se capturó UI real sobre seno sintético de 66,1 Hz / 20 s, identificado como tal. Hash/comandos/tolerancias en VALIDATION. Esta fase no instaló ni ejecutó nada en Samsung.
- Documentación nueva: AUDIO_ANALYSIS (pipeline/unidades/rendimiento), HARMONICS (criterios/ambigüedad), ACOUSTIC_VALIDATION (protocolo y referencias pendientes). Actualizados arquitectura, README, AGENTS, roadmap, estudio del motor y capturas. SciPy no está instalado; no se atribuye comparación externa ejecutada.
- **Reanudación exacta:** empezar fase B por un espectro interactivo grande desde `recordingAnalysis.result.spectrum` y sus candidatos, sin recomputar en Compose; ejes Hz/dB relativos, selección de pico, zoom/pan y accesibilidad. Después envelope min/max de onda y STFT por tiles con caché limitada. Mantener el flujo de un botón, Navigation Compose y almacenamiento. Para 15 minutos, implementar F1 antes de tocar el límite; no aumentar solo MAX_RECORD_SECONDS. No correr instrumentación en el Samsung.

## 2026-10-20 — Atrás desde el detalle de sesión

- Causa corregida: `selectedId` reemplazaba visualmente Biblioteca sin crear back stack. Se retiró y se agregó Navigation Compose 2.9.0 con rutas de las tres secciones y `session/{id}`; NavController reemplaza `vm.tab` como fuente de navegación. Abrir un diseño sigue llevando al diseñador.
- Lista y detalle tienen scroll propio guardado por la entrada de navegación. La flecha de la barra superior y Atrás del sistema quitan el mismo detalle. Los cambios de destino detienen captura/reproducción; MediaPlayer libera recursos incluso ante un error de `stop()`. Cambiar de sección descarta el detalle abandonado y conserva el estado de la lista.
- La prueba ampliada a la última tarjeta detectó un salto de 6 px por las alturas distintas de las barras superiores. Se unificaron con `TopAppBar` Material y se mantuvo ese caso en la prueba de regresión.
- Se agregaron tres pruebas de UI con WAV sintético y datos aislados: Atrás Android, flecha, y cambio de sección con reproducción. Verifican offset de scroll, pila, actividad activa y reproductor detenido. Primer intento falló por Espresso 3.5.0 y API 37; se fijó Espresso 3.7.0 con el arreglo oficial. Resultado final: 10/10 JVM, 7/7 instrumentadas solo en emulador, APK debug compilado. Captura real nueva en `docs/screenshots/sesion-navegacion-emulador.png`.
- No se modificaron motores acústicos, modelos persistidos ni registros existentes. Pendiente validación manual del gesto predictivo y audio audible en Samsung. Detalles y hash del APK en `VALIDATION.md`.
- Se actualizó el Samsung con `install -r`, usando la autorización previa y verificando primero que la app no estuviera en primer plano. `Success`, hash del APK instalado idéntico al compilado y checksum de `local_data.xml` sin cambios. Ninguna prueba instrumentada se ejecutó en el teléfono.

## 2026-10-20 — Medir y sesiones integradas

- A pedido explícito del usuario se instaló el APK final en Samsung SM-G990E con `adb -s R5CT21KTQ8W install -r`: `Success`. La versión pasó de `2.0.0-beta1`/2 a `2.1.0-beta1`/3; la actividad inició y quedó un proceso activo. No hubo instrumentación ni limpieza de datos en el teléfono; la interacción musical sigue pendiente.
- Continuación tras la interrupción: se revisó el código y se añadió recuperación no destructiva de WAV privados con UUID que no tenían entrada en el índice. Biblioteca los muestra como «Audio recuperado», sin nota ni serie temporal inventadas; renombrarlos incorpora sus metadatos al índice. Se añadió cobertura instrumentada de recuperación y compatibilidad.
- La primera ejecución instrumentada detectó una expectativa errónea del test sobre el orden por fecha ante archivos de prueba anteriores. Se seleccionó el registro legado por ID y se aisló la limpieza a los datos propios de los tests. Resultado final: 10/10 pruebas JVM, 4/4 instrumentadas solo en emulador y APK debug compilado (`2.1.0-beta1`, SHA-256 en `VALIDATION.md`).
- Se tomó línea base Windows: siete pruebas JVM y `assembleDebug` aprobados con JBR 21/SDK 35. `git status` confirmó que esta carpeta no contiene `.git`, por lo que no se pueden hacer commits aquí.
- Se reemplazó Afinar por Medir: un único botón inicia AudioRecord, lectura y WAV; el mismo detiene y guarda automáticamente. También se cierra a los 60 s, al navegar fuera y en `onStop`. Se quitó la escucha automática. Se mantuvo Laboratorio.
- `SessionAnalysis.kt` calcula nota predominante y mediana de Hz desde lecturas válidas, rango percentil 10/90 y estabilidad. `RecordingStore` conserva WAV/índice anteriores y añade resumen/serie temporal a sesiones nuevas; las lecturas antiguas quedan separadas y visibles.
- Biblioteca ahora presenta Sesiones y Diseños; el detalle suma gráfico de Hz en el tiempo, cursor aproximado de reproducción, controles de audio y acciones de archivo. No se cambiaron fórmulas de `AcousticModel` ni `PitchDetector`.
- El APK `2.1.0-beta1` y 10 pruebas JVM pasaron; cuatro pruebas instrumentadas aisladas pasaron **solo en emulador**. Se recorrió una sesión de silencio de 14 s con WAV privado y detalle, cierre automático de 60 s, guardado al salir, persistencia tras reapertura, Vista amplia y 900×1800 con fuente 1,3×. Se verificó renombrar, exportar por SAF y borrar conservando la copia. El emulador no permitió validar nota ni audio físico; pendiente contraste en teléfono con didgeridoo, sonido audible, accesibilidad, rotación y sincronía precisa.
- Política: jamás instrumentación ni operaciones que puedan borrar datos sobre el Samsung real sin autorización explícita. Las capturas nuevas son reales del emulador y muestran silencio.

## 2026-10-20 — Navegación, afinador, grabaciones y biblioteca

- Se leyó la documentación, los módulos Kotlin y las pruebas; se estableció línea base con siete tests JVM y APK debug.
- `TallerApp.kt`: navegación Material 3 de tres secciones; afinador de gran lectura con estados y modo de máxima legibilidad; Biblioteca dividida por tipo; Laboratorio conserva Diseñar, Espectro, Buscar y Guía. Se preservó el motor acústico.
- `MicrophoneEngine.kt`: grabación manual hasta 60 s y entrega del WAV al detener; `RecordingStore.kt`: WAV privado e índice persistente. `TallerViewModel.kt`: estado de escucha, guardado contextual, reproducción, pausa y gestión de registros. `LocalStore.kt`: IDs/cents nuevos y lectura de JSON antiguo. `MainActivity.kt`: reanudación optativa, cese en `onStop` y exportación SAF tras rotación.
- El editor conserva puntos personalizados al mover longitud, boquilla, diámetro intermedio o campana. La lista de diseños no recalcula simulaciones.
- Se añadieron dos pruebas instrumentadas aisladas para persistencia; pasaron en Samsung SM-G990E. Las siete pruebas JVM y el APK debug también pasaron. El primer intento instrumentado falló por contexto de escritura incorrecto; se corrigió.
- Incidencia: `connectedDebugAndroidTest` dejó la app desinstalada; al reinstalarla, no había `local_data`. El usuario confirmó que las dos mediciones reales estaban en este teléfono. No repetir pruebas instrumentadas allí. Se buscaron exportaciones por nombre en Descargas/Documentos sin resultados; el usuario eligió no reconstruir los valores. Detalles en `VALIDATION.md`.
- Quedan pendientes la interacción visual con el teléfono desbloqueado, audio físico, exportación real, accesibilidad, rotación y contraste con afinador independiente. Ninguno se declara validado.
- En emulador Android se verificaron navegación, estados de permiso y silencio, recorrido WAV completo, persistencia tras cierre, exportación a Descargas y optimización visible. La pantalla chica con fuente 1,3× reveló que el control de escucha quedaba fuera de vista; ahora escucha y guardar medición quedan fijos sobre la barra de navegación. Se reinicia el scroll al cambiar de sección.
- El APK actual tiene 10.485.510 bytes y se instaló con `adb install -r` en el Samsung; la versión visualmente idéntica anterior se probó en el emulador. El Samsung seguía bloqueado, por lo que no se obtuvieron capturas suyas. Hash y evidencia en `VALIDATION.md`. Se limpiaron los XML temporales de inspección y se conservaron capturas PNG reales en `docs/screenshots/`.
- `LocalStore` ya no descarta automáticamente las mediciones antiguas después de 200 entradas ni los diseños después de 100; las siete pruebas JVM y `assembleDebug` pasaron tras el cambio. Las dos pruebas de persistencia instrumentadas pasaron antes de este ajuste; no se repitieron en el teléfono de uso.
- `git status` devolvió “not a git repository” en esta carpeta; no fue posible crear commits pequeños desde este entorno.

## 2026-10-19 — Migración inicial Kotlin+Compose

- Se creó nuevo proyecto limpio preservando applicationId `ar.didgetaller`.
- Se portó a Kotlin independiente el núcleo `Music`, `PitchDetector`, `AcousticModel` y `WavCodec`, y se probaron con JVM real.
- Se implementaron cinco pantallas Compose Material 3, ViewModel y captura AudioRecord en hilo dedicado.
- Se preservaron claves JSON y SharedPreferences del antiguo proyecto Java.
- Se agregó bootstrapping de `gradle-wrapper.jar` con verificación SHA oficial, tests JUnit, y workflow CI.
- Bloqueado: SDK/Gradle Android no instalados en este entorno. Primera acción en Windows: sincronizar y compilar.
- Siguiente paso: fase P0 del ROADMAP.

## 2026-10-19 — DidgeLab personal
- Corregido alcance: uso personal NO COMERCIAL.
- Motor acústico: perfil arbitrario, propagación 1D con pérdidas y radiación aproximadas, cálculo de picos.
- Motor de evolución para 3 picos; interfaz Buscar, editor por puntos y retrocompatibilidad JSON de diseños.
- Validado núcleo puro JVM y generación de candidatos. Falta Android Studio build, UI y pruebas físicas.
- Referencia: `docs/DIDGELAB_STUDY.md`, DidgeLab CC BY-NC-SA 4.0; código Kotlin de implementación independiente.

## 2026-10-19 — Primera compilación Android verificada en Windows

- Se comprobó JDK 17.0.20.1, Android SDK Platform 35 y Build Tools 35.0.0. Se usó JDK 17 porque el JBR de Android Studio disponible es 25.
- El wrapper descargó su JAR oficial, verificó el SHA-256 y ejecutó Gradle 8.13. No se cambió ninguna versión ni código fuente: AGP 8.13.2 y Gradle 8.13 compilaron correctamente pese a diferir de las versiones escritas en `AGENTS.md`.
- `:app:testDebugUnitTest` aprobó 7/7 pruebas; `:app:test` aprobó las mismas 7 en debug y release; `:app:assembleDebug` generó el APK. Los primeros intentos tropezaron con restricciones de red/caché del sandbox; el registro exacto está en `docs/VALIDATION.md`.
- Archivos modificados: documentación de validación, hoja de ruta y registro de trabajo. El JAR del wrapper y los artefactos de Gradle se generaron al compilar.
- Siguiente paso P0: conectar o iniciar un Android, pedir autorización antes de instalar en un dispositivo físico y verificar permisos, audio, WAV, CSV, navegación y diseños.

## Four-bout acoustic failure resolution

Started from clean `f428628`, preserving the four local commits ahead of fetched
`origin/main/170bcf9` and the other worktree. A Git source archive was created before
edits. The externally supplied WAV hash and both screenshots were verified; no
Samsung operation was performed. An interruption during validation was resumed
from the existing diffs/logs without restarting the research or duplicating changes.

The actual production replay, independent STFT and existing frozen regression
corpus isolate a quiet-comparison/full-buffer energy mismatch. Reuse the existing
amplitude threshold on the selected lag's real sample support; retain the rejected
candidate as diagnostic evidence. Keep the original YIN decisions otherwise and
leave full-rate flute and SWIPE′ untouched. Reject fusion/replacement: additional
coverage does not establish the physical drone. Make energetic uncertainty readable
with a separate bounded, source-time audio-level strip rather than invented Hz.

The second normal bout has changed grave-frequency content and much stronger
182-Hz energy. This is an observed acoustic difference, not proof of a particular
physical mechanism. The current recording's large interior gaps remain honest
abstentions. Historical readings are preserved; the source fix applies to new
processing, not a silent migration. Derived analysis cache version 3 prevents reuse
of the old acceptance policy on requested reanalysis; original sessions are unchanged.
Exact original callback timing is unavailable
without the phone's saved timeline, which was not supplied.

Validation, including real failed regressions and their corrections, is recorded
in VALIDATION. The authoritative private report and reproducible artifacts are
`checkpoints/experiments/reproduction-1527/REPORT.md`. No new dependencies, native
changes, session schema changes, private Git assets, push or physical-phone install.

## Multi-estimator pitch reliability decision

Reused the prior genuine pYIN, SPTK and production YIN benchmarks; executed
pYIN on the later four-bout private WAV with aligned input. The tested agreement
rule offered no held-out accuracy or real-vocalization coverage gain. The two
energetic 35.7-Hz points remain competing periodicity evidence, not verified
musical notes. The established quiet 51.4-Hz defect is already fixed by `1610028`.
Keep the current Android detector/UI and diagnostic alternatives; avoid an extra
runtime estimator and unsupported register correction. Exact private evidence is
retained under `checkpoints/experiments/multi-estimator-reliability/`. Current
documentation records the result without publishing private audio or plots.

## Instrumented-suite remediation — 2026-10-25

- Continued from `278944d` without repeating the completed application-wide
  audit. Triaged all nine failures in the previous 100-test emulator run. The
  affected tests mixed the original captured pitch series with the current
  source-aligned WAV replay or asserted tone changes absent from the WAV.
- Added deterministic PCM fixtures and updated camera/source assertions to
  follow the chart's actual replay and component timing. Kept original timeline,
  source-WAV integrity, color, reset, seek, flute, toot and navigation checks.
  No production estimator, chart rule or persisted data changed.
- The full API 37 emulator suite passed **103/103** after the fixes. JVM,
  JaCoCo, Detekt and debug APK tasks completed successfully. Exact commands,
  per-case causes and limits are in [VALIDATION](VALIDATION.md); logs remain in
  ignored local checkpoints. The Samsung was not accessed.
- The synthetic tests establish UI behavior, not physical didgeridoo pitch.
  Existing pending product limitations and historical reports remain unchanged.
- Publication remains blocked: after the 103/103 complete pass, two later
  complete runs exposed transient cancellation/edge-gesture assumptions and
  the tests were refined. The post-reboot final-APK run crashed in Android's
  `HardwareRenderer` finalizer; a reordered run then timed out waiting for
  Activity focus in the chart picker. See [VALIDATION](VALIDATION.md) for logs.
  At this checkpoint, `278944d` remains local; publication waits for a final
  passing suite.

## Final UX continuation — 2026-10-25

- Preserved the earlier audit and committed its deterministic fixture/test
  repairs as `a9b28b8`. Stabilized chart selection, Back focus, recording-wide
  navigation, and analysis completion assertions without changing acoustic
  algorithms or stored session behavior.
- Replaced the opaque square launcher bitmap with a layered Android adaptive
  icon. The existing splash drawable and theme remain unchanged. Before/after
  Pixel Launcher and splash screenshots were inspected in ignored local
  `checkpoints/experiments/ui-audit-2026-10-24/`.
- Corrected four localized integer format specifiers and indentation exposed by
  the final lint run. JVM (171 passed, 8 skipped), JaCoCo, Detekt, lint and
  debug APK tasks completed. Subsequent full runs exposed and fixed test timing
  and one API 37 emulator WindowManager Watchdog crash. A cold-booted final
  host-GPU run completed **103/103** with no test failure.
- `detektComplexity` analyzed 55 Kotlin files successfully. Local validation
  is complete; publication and physical installation depend on safe Git review,
  remote CI success, and device integrity checks. No production pitch estimator,
  private recording, Samsung data, or physical device was touched during this
  local phase.

## Musical Experience 4.0 — in progress, 2026-10-25

- Started from clean `main`/`origin/main` at `005b296`. Preserved all historical
  recordings, detector implementations, saved readings, and the new launcher.
- Established one musician-facing saved-session quantity per instrument:
  supported spectral drone component for didgeridoo, accepted YIN pitch for
  Native American flute. The Library card, detail and fullscreen use the same
  `MusicalTrace`; the three-way ordinary-session selector is removed. Existing
  YIN/SWIPE' comparisons remain in Advanced diagnostics.
- The private 15:27 four-section WAV was checked in an emulator. Card and
  detail use identical `PlotWindow` bounds; 30.73 s has supported component
  evidence. The historical low YIN candidates are not mixed into the main
  component curve. Screenshots and full logs are private under ignored
  `checkpoints/experiments/musical-experience-4/`.
- JVM suite: 183 tests, 0 failures, 8 skipped. Gradle coverage, Detekt,
  lint, debug APK and app-test APK completed. One complete emulator run finished
  104/105 with an asynchronous test cursor race; the corrected case passed
  independently. A further run stopped at the rotation case after 103 passes;
  that case also passed independently. The user waived another complete run
  for this delivery. Do not report 105/105.
- The focused implementation and test commit is `9b789ba`. Staged privacy
  review found no private audio, screenshots, secrets or user paths. The
  documentation commit, safe fast-forward push, CI and separately verified
  Samsung update remain pending. The phone has not yet been accessed in this
  iteration.

## Codex workflow policy — 2026-10-26

The root agent file previously combined current rules with obsolete release
instructions and required broad context reads/builds even for small changes.
The local engineering skill also made red-first TDD and optional analysis feel
unconditional. Both now use progressive disclosure and risk-based validation.
Privacy, Samsung safety, explicit Git staging/identity, honest test reporting,
and small root-cause fixes remain mandatory. No application behavior changed.

## Saved-session UX — 2026-10-26

The preview chart and transport now lead the session detail. Four wrapped
chart choices replace a horizontally hidden Harmonics tab; a Tools sheet
contains the musician utilities and a distinct advanced YIN/SWIPE' chart.
The ordinary spectral-drone preview keeps its own viewport and meaning even
when experimental comparison is enabled. The diagnostic chart can still open
the existing full-screen viewer with experimental layers. The bottom section
bar is omitted inside session detail to preserve room on compact screens; Back returns to
the saved-session library. Landscape puts transport beside the chart.

Focused JVM, static/resource, APK, and emulator checks passed as recorded in
`docs/VALIDATION.md`. Evidence is synthetic and remains in ignored local
directories. README screenshot replacement remains a separate follow-up.

## README showcase — 2026-10-26

Replaced the small mixed-language screenshot table with a two-column visual
showcase of the accepted session UX and related measurement views. Four fresh
English app captures are featured; a Library preview is linked. The existing
launcher identity was transcribed from its vector paths into a standalone SVG.
The README now distinguishes the ordinary supported spectral drone component
from live YIN readings and advanced YIN/SWIPE' diagnostics. Production app
code and historical screenshot evidence remain unchanged.

## Spanish README showcase — 2026-10-26

The Spanish README now follows the same hierarchy, two-column gallery, build
path, and scientific limits as the English README. The Spanish screenshots
use the same isolated synthetic signal and emulator capture paths. Existing
English assets and production app code remain unchanged.

## Live measurement and discard — 2026-10-27

The active Measure layout now prioritizes the current reading and timer, with
separate save and confirmed discard actions. The recorder lock resolves save,
timeout, and discard ownership once; a discarded UUID capture is renamed out
of recovery before cleanup. A failed cleanup is surfaced and never reported
as success. The existing save-on-navigation/background policy and fullscreen
back stack are retained. Validation and the remaining recovery and TalkBack
limits are recorded in `docs/VALIDATION.md`.

## Stable live measurement layout — 2026-10-27

Reserved measurement slots remove state-dependent height changes. Recording
controls now follow the card before secondary details; compact landscape and
large-text spacing keep controls above navigation. Emulator-only evidence and
validation limits are recorded in VALIDATION.md. Audio and recording lifecycle
were not changed.


## 2026-11-06 ? Session graph interaction and display lifecycle

Verified clean main and remote HEAD at 76ad193 before implementation. Kept acoustic estimators, trace construction, persisted formats, recording policy, package and signing untouched. Reused PlotProjection, shared ExplorerSelection and serialized SessionPlayer seeks. The measurement overlap came from the fullscreen bottom action losing the navigation padding supplied by the ordinary navigation bar. Added one lifecycle-owned View screen-on request at the Activity composition boundary. Validation and remaining physical-test limits are recorded in VALIDATION.md; device update evidence stays in ignored checkpoints.


## 2026-11-06 ? Direct graph timeline and compact session workspace

Replaced handle-only scrub eligibility with plot-wide horizontal scrubbing; two-finger translation/pinch owns viewport navigation. Kept pending preview separate from committed selection and fixed consumed cancellation releases committing a seek. Removed the primary transport slider and moved accessible SetProgress onto the graph. Compacted only the session workspace and fixed its empty Scaffold bottom bar suppressing navigation insets. No acoustic or persisted-data changes. Final checks and representative screenshot/configuration evidence are recorded in VALIDATION.md. Private delivery evidence stays under ignored checkpoints.


## 2026-11-06 ? International repository presentation

Classified tracked Spanish text before translation. Standardized technical comments/messages and documented explicit exceptions for localized UI, compatibility-sensitive mappings, fixtures and historical evidence. Added current English guidance without rewriting dated Spanish records. Updated the English/Spanish README pair with authentic refreshed emulator galleries and an attributed, technically honest explanation of the project-local ub-mode engineering instructions. Screenshot fixture changes are test-only; production executable code and all Android resources are unchanged. Validation and screenshot provenance are recorded in VALIDATION.md and the gallery indexes. Samsung was not accessed or modified.


## 2026-11-06 ? Measured quality milestone

Installed original licensed ub-mode and separated project safety. Added actual Kotlin CPD/PIT execution, explicit measured ratchets and manual deep CI. A real mutation survivor led to a diagonal boundary test. Complete instrumentation exposed 13 existing failures; no green full-suite claim or exclusions. See docs/quality/DECISIONS.md for decisions and the baseline for counts/debt. Samsung and private WAVs were untouched.

## Quality modernization follow-up - 2026-11-06

Resolved the 13 complete-suite Android failures through current gesture/visibility contracts and two presentation fixes: explicit inspection precedence and per-destination inset retention during navigation. The end-of-list scroll regression now preserves the exact offset. Default emulator suite: 125 discovered, 119 passed, 6 explicit skips, no failures; both conditional compact/150%-font scenarios subsequently passed. Final JVM variants: 195 discovered / 187 passed / 8 protected-fixture skips each. Selected coverage-enabled emulator smoke: 5 passed, real JaCoCo data/reports; full coverage execution belongs to the main/manual CI workflow.

Retained original quality floors, expanded only reviewed CRAP method debt to eight exact mappings (three historical high scores), and added meaningful projection/inspection tests. JVM coverage is 28.19% lines / 23.39% branches overall, 98.47% / 77.25% pure core. CPD found zero 100-token groups; PIT killed all 11 selected Kotlin mutants. Lint, Detekt, localization, Actionlint and eight Python gate/parser tests completed. See docs/quality/QUALITY_BASELINE.md and QUALITY_GUIDE.md for scopes, limitations, verified commands, local evidence and actual CI links. Official ub-mode/MIT remain byte-identical; both README languages and Spanish resources preserved. Samsung/private recordings untouched.


### CI/readiness and fixture-policy correction - 2026-11-06

CI 0e807dc fast checks and real PIT passed; the full emulator job failed one of 125 cases (114 passed, 10 skipped). The remaining assertion compared a provisional range with asynchronously completed canonical evidence; await that evidence before recording the reference, preserving exact comparisons. CI's skip inventory also exposed three auto-discovered private-fixture tests in the earlier local AVD. Those local cases are excluded from synthetic acceptance evidence. No private audio/screenshots were published and original bytes were asserted unchanged; Samsung untouched. Require explicit private-fixture authorization/argument, verify the policy, and use a newly created empty emulator for public validation. See QUALITY_BASELINE for the precise correction and final replacement evidence.

A nonincremental local compilation reproduces all fresh CI JVM coverage counters exactly: 28.19% lines / 25.69% branches overall, 98.47% / 77.25% core. Updated the coverage guide with the verified compiler flag rather than merging incompatible cached-output measurements.


### Final verification and toolchain ownership - 2026-11-07

Closed verification after observing all three successful jobs on 90d7167: fast 42361585974, complete clean emulator 42361585985 (117 passed, 10 explicit skips, zero failures) and PIT 42361596610 (11/11 killed). The retained-owner fixture repair passed with every original assertion. Recorded actual new Android percentages separately from prior snapshots and JVM; kept historic failures visible. Final delivery records evidence only and reuses byte-identical validated inputs, avoiding another full suite for Markdown. No production/acoustic change, Samsung operation or private audio inspection.

Actual new-toolchain CI exposed a deterministic ownership defect behind a timing-dependent test failure: ScreenAwakeTest discarded its LifecycleOwner while LifecycleRegistry held only a weak reference. Retained the owner through composition and transition callbacks; preserved every lifecycle/window assertion and all production behavior. Failed run 42360716441 and its raw stack/artifacts are retained, while fast run 42360716446 and mutation 42360726639 passed. Publish the narrow fixture repair and observe its complete fresh-emulator execution; no retry of identical failing inputs or private-device operation.

Included the user's existing AGP/Gradle/JDK migration only after explicit authorization; checkpoint ec3c134 preserves its original state. Removed vendor-pinned daemon downloads, retained installed JDK 21 criteria, checked the Gradle distribution checksum, aligned CI JDK/build tools, and restored release JVM tests omitted by AGP's new default. CI now names both variant tasks explicitly. Added download-only caches, bounded fast-job timeout and independent concurrency groups; no project build/coverage cache, threshold relaxation, new recurring schedule or branch-policy change.

The audit reconciles remote README blobs, actual successful/failed runs, method-level metrics and controlled failure probes in docs/quality/QUALITY_AUDIT_VERIFICATION.md. All prior passing reports remain labeled with their real revision; changed-toolchain CI must run again. No application/source behavior changed, no metronome work, no Samsung/local emulator operation, no protected audio inspection. Legacy Kotlin/DSL compatibility and absent merge protection remain explicit debt.

### Final passing CI evidence - 2026-11-06

Code 9e7b316: fast run https://github.com/hmeclazcke/DidgeTaller/actions/runs/42301032905 and full emulator run https://github.com/hmeclazcke/DidgeTaller/actions/runs/42301033022 passed. Full clean API 35 emulator: 127 discovered / 117 passed / 10 explicit skips / 0 failed, 450.824 s. Seven skips are protected-fixture opt-in guards; the three configuration-dependent cases passed on the new clean API 37 AVD. Two JVM variants each: 195 discovered / 187 passed / 8 protected skips / 0 failed. JVM coverage 28.19% lines / 25.69% branches; core 98.47% / 77.25%. Separate Android coverage 91.14% / 67.17% (audio lines 95.22%, persistence 94.37%). All exact test counts and coverage data were read from retained CI artifacts.

Real PIT run 42297582476 passed 11/11 killed; its relevant inputs are unchanged. CPD zero 100-token groups; reviewed CRAP-L eight mapped / 329 unmapped, three historical high scores retained. See QUALITY_BASELINE for full scope, actual links, privacy correction and remaining debt. The final evidence commit is documentation-only and reuses unchanged validated inputs; no redundant Android run or Samsung operation.

### Isolated Uncle Bob rebuild milestone — 2026-11-07

Branch refactor/uncle-bob-rebuild, separate C:/projects/DidgeTaller-uncle-bob worktree, baseline main 3a160043f9ebb3a420b1d89a9cf4ddfe7d1b5d67 unchanged. IN PROGRESS: this is not complete application acceptance. Original skill/license byte equality verified. Compiler PSI and source/descriptor/body correspondence expose 324 current measured functions; every one of 439 named declarations is classified. Original expanded baseline was 276 mapped/97 high, rather than eight allowlisted functions. Current high scores: 85; no coverage floor was relaxed.

DSP period search and trace assembly were reconstructed with 741 synthetic output lines byte-identical to original compiled bytecode. Complexity 43/33/15 becomes 9/entry-point 1/7, with actual extracted work <=9. Framework-independent PlaybackController owns session/seek state behind Android ports; 22 behavioral tests include stale callbacks, pause/loop/preparation and a red/green disposal scheduling defect. A separate red/green test prevents nonfinite live display state. Both JVM variants: 223 discovered/215 passed/8 protected skips/0 failures each. Debug and Android-test APKs build. JVM coverage 30.66% lines/27.84% branches, core 98.50%/78.36%, changed denominators disclosed. Lint 0 errors/59 warnings/10 hints; an invalid local SDK property and stale lint cache were corrected before this pass. Eighteen Python tests, parser test, localization and ratchets pass. Original static rules remain.

Expanded free PIT: 17 concrete classes, initial 1,143 mutants (753 killed, 364 survived, 24 uncovered, 2 timeouts); strengthened 1,146 (767 killed, 354 survived, 23 uncovered, 2 timeouts), 67.10% detected including timeouts. Domain score remains informational; focused 11/11 threshold still enforced. Controlled JVM timing/allocation samples and scope limitations are in docs/engineering/REBUILD_CASE_STUDY.md. Full branch emulator/CI evidence must be observed separately. No physical device/private audio operation, persisted-format change or metronome implementation. Next exact work is in REBUILD_PROGRESS.md; capture/persistence/ViewModel/UI reconstruction and remaining survivors are unfinished.

### Worker-owned capture and verified branch CI - 2026-11-07

IN PROGRESS on refactor/uncle-bob-rebuild. Source 07c756c has actual passing fast/differential CI 42368989332, full clean emulator CI 42368900284 (117 passed/10 skipped/0 failed of 127), and manual mutation CI 42368987707. Downloaded artifacts confirm 17-class PIT counts and separate Android coverage 91.08% lines/68.12% branches. The duplicate fast push was cancelled in favor of the identical PR run; not counted as passing.

A new controlled emulator regression demonstrated an old worker finalizing a replacement's writer. RecordingCapture now owns per-worker file/state, sample limits, discard claims and terminal outcomes. Twelve recording handoff integration tests pass, including the new regression, actual emulator timeout/discard, historical synthetic bytes/index/recovery and ViewModel disposal. Fifteen new JVM cases pass; full debug/release suites each 238 discovered/230 passed/8 protected skips/0 failures. Twenty-one Python tests, compiler parser, localization, builds, Detekt, Lint, CPD, JaCoCo and differential pass. Current mapping 329/400 with all 444 declarations accounted; 84 high scores remain visible. JVM 31.38% lines/28.62% branches, core 98.47%/78.58%, changed denominators. Original coverage floors retained; exact signature lineage keeps predecessor risk ceilings and fails regressions.

Expanded 18-class PIT executed 1,197 mutants: 806 killed, 365 survived, 24 uncovered, 2 timeouts. Focused survivor review added two-second limit and empty-chunk assertions; capture is now 40 killed/10 survived/1 uncovered of 51. The broad earlier run is not relabeled as a final run after those assertions. Windows open-file rename limitations remain explicit; Linux and Android evidence are separate. No optional paid plugin or operator exclusions.

A brand-new API 37 AVD used only synthetic data. Eight ES/EN UI audit methods passed, including compact portrait/landscape at 150% font; actual screenshots inspected, including unavailable trace and real spectrogram/harmonics. No physical Samsung/private WAV access, main changes, merge, or metronome. Remaining architecture, persistence/cache, mapping and survivor work is listed in REBUILD_PROGRESS.md. New capture CI results remain to be observed after publication.

### Independent cache publication and native Back differential - 2026-11-07

A controlled concurrent cache test failed on the old shared temporary path with a mixed integer value. Unique same-directory temporary ownership and atomic replacement now pass six JVM contracts plus all 27 selected emulator persistence/capture cases. No cache format/key or original recording changed. Free PIT's cache scope produced one cleanup mutant, killed; this is explicitly narrow. The domain selection now contains 19 classes for the next actual CI run.

Full local debug/release suites each 245 discovered/237 passed/8 protected skips/0 failed after adding the locale-independent canonical frequency text contract. JVM 31.49% lines/28.73% branches, core 98.54%/78.88%; 330 mapped/71 unscored of 401 Detekt functions, all 445 named declarations accounted, 83 high. Same-config Detekt 1,970; Lint 0 errors/59 warnings/10 hints; CPD zero >=100-token groups. Parser/Python/localization, debug/application-test APKs, quality ratchets and 741 differential outputs pass. Windows/Linux mutation paths are reported separately, never merged.

All 63 runtime Kotlin files and owned native wrapper received an initial direct responsibility review; REBUILD_REVIEW.md records retained/rebuilt/unresolved decisions. That reading milestone is not architectural completion. Eight focused ES/EN UI cases pass, with actual compact/landscape 150%-font screenshots inspected. API 37 native edge-Back fails. Readiness/gesture-distance/animation diagnostics did not resolve it; experimental test edits were removed. The exact original main APK/test at 3a16004 also fails on the same fresh AVD, establishing the limitation predates this rebuild. No skipped assertion or KEYCODE_BACK fallback was introduced. Original-main UI oracle lives in a detached ignored worktree; rebuilt emulator APKs were restored afterward. Native MainActivity versus Compose test-clock diagnosis remains unfinished.

Main and Samsung were untouched. Only the explicitly selected fresh emulator received synthetic fixtures and APKs. Continue from REBUILD_PROGRESS.md; full reconstruction remains IN PROGRESS and the PR must remain draft. New feature-branch CI must be inspected after publishing these source commits.

### Verified rebuild checkpoint d90926c - 2026-11-07

IN PROGRESS, draft PR https://github.com/hmeclazcke/DidgeTaller/pull/1. Source/test/config d90926c passed fast/differential CI 42386035464, full API 35 emulator 42386030241 and manual 19-class PIT 42386032298. Downloaded reports confirm JVM 492 discovered/476 passed/16 protected skips and Android 129 discovered/119 passed/10 explicit skips, zero failures. Linux JVM 31.51% lines/28.84% branches, core 98.61%/79.18%; separate Android 91.15%/68.10%. Final PIT: 1,198 total, 825 killed, 355 survived, 16 uncovered, 2 timed out; detected 827/1,198=69.03%, 109 seconds. Capture improves 47/51 to 51/51 on the same Linux scope after independent filesystem-oracle and zero-append/full-disk assertions; production logic unchanged. PlotTouch's enforced 11/11 gate passes. Thirty-two Android-method CRAP-L scores and 83 JVM scores exceed target across the same 330 reliable mappings; the Android mapping was generated locally from exact CI XML and is informational, never merged coverage.

The API 37 Back diagnosis is resolved: the hand-created AVD inherited hardware main keys, then suffered a cold-boot System UI ANR under host memory pressure. Set hw.mainKeys=no only in the new AVD, verified System UI's edge handler, stopped idle build daemons, and recovered the ANR. The unchanged original edge-Back test passed 1/1, preserving/releasing real playback correctly; native MainActivity wide/normal Back also passed without recording or key-Back fallback. Corrected software-navigation compact ES portrait/landscape 150% cases pass 2/2; their two images and three native screenshots were inspected, in addition to 26 initial screenshots. Earlier invalid-hardware images prove in-app layout rather than software-bar insets. No source navigation workaround, timeout increase, private fixture or physical device operation was introduced.

The case study, per-file review and concise REBUILD_PROGRESS checkpoint contain remaining implementation work and exact commands. Both README languages updated consistently; original ub-mode/license and Android resources untouched. Main remains 3a160043f9ebb3a420b1d89a9cf4ddfe7d1b5d67, clean; only the feature branch was pushed. Significant persistence/ViewModel/UI/high-risk/mutation work remains, so this is not completed whole-application acceptance. Evidence-only documentation changes reuse the unchanged d90926c implementation inputs.


### Persistence and library ownership continuation

Existing rebuild branch resumed from 5f93e23; c132642 commits the persistence redesign. Local final-source JVM: 548 discovered, 532 passed, 16 protected skips; 25 focused Android persistence cases pass. Original corruption/unknown-row/duplicate-ID cases failed 3/3 before the rewrite. Shared publication, preserved backups/quarantine, explicit recovery and one library snapshot replace the old owners. A further red test reproduced an uncaught corrupt-extras exception on the UI thread; the correction is compiled and under remote Android verification. Local API 37/System UI failures invalidate final screenshots and the aborted four-case run, rather than proving an app regression. CI collects synthetic library screenshots only from a fresh API 35 emulator after a successful complete run. Current metrics, pending gates and exact next actions are in docs/engineering/REBUILD_PROGRESS.md. Main, Samsung, private audio, serialized formats and the official skill remain untouched. Overall rebuild IN PROGRESS; PR #1 remains draft.


### Verified persistence/library and locale milestone ? 6c6f00d

Fast CI 42413957308 and full API 35 CI 42413952372 pass. Actual JVM totals: 548 discovered/532 passed/16 protected skips; Android: 144/134/10, plus two separate real compact en/es 150%-system-font recovery cases passed. Thirteen synthetic screenshots were inspected; the caller-locale mismatch found after the first green pipeline was fixed rather than ignored. JVM coverage 33.69% lines/31.33% branches; separate Android 91.41%/68.72%. Compiler/CI hashes match all 71 production Kotlin files. Ratchets pass with 361 mapped/76 unscored and 79 high JVM CRAP-L; separate Android mapping has 30 high. CPD zero; Detekt 1965; remote Lint 0/53/9. Python 21 CI cases and six separate local shell-failure probes pass. 741 differential outputs remain identical. Manual PIT 42412071596 at 22655bb remains valid for byte-unchanged selected inputs: 1369 mutants, 970 killed/381 survived/16 uncovered/2 timeouts; detected 71.00%, 26 classes, engine 78 seconds. The original focused gate remains 11/11. Overall rebuild IN PROGRESS: continue capture-device/terminal-event and ViewModel ownership, then renderers/high-risk logic; do not repeat completed persistence work. Main/Samsung/private WAVs and the original skill/license remain untouched. See REBUILD_PROGRESS.md for URLs, artifact paths and measurement limitations.


### Capture terminal ownership continuation

Resumed rebuild 9cd18f6 on the existing isolated branch. A real-Handler emulator test reproduced late discard A stopping replacement monitor B. Replaced untagged terminal callbacks with worker identities and one claimed envelope; extracted pure RecordingCoordinator and CaptureHandoff; the ViewModel now publishes one recording snapshot and immutable save requests. Extracted cancellable spectrogram raster preparation. See docs/engineering/CAPTURE_EVENT_OWNERSHIP.md and REBUILD_PROGRESS.md for decisions, selected mutation results, limitations and exact continuation. Main, Samsung, private recordings, acoustic algorithms and persisted formats remain untouched. Remote framework validation is pending publication.


Initial API 35 CI correctly rejected a legacy post-disposal error equality (136 pass/1 failure/10 skips of 147). Strengthen the contract instead of preserving callbacks into a destroyed UI: live owners require nonblank failure, disposed owners require silence, and original WAV/index assertions remain. Route raw device errors through the pure coordinator and independently reset failed-start listening; deterministic regressions and focused PIT pass. Remote rerun is required. The Spanish README paragraph also restores accents lost during shell text transport; original localized resources remain intact.


### Playback intent continuation after a7d2514

Verified actual branch/remote and main. Retrieved completed CI evidence: fast/PIT success, Android 136 pass/1 failure/10 skips (147), now failing pause stability in a loop. Four deterministic controller regressions reproduced stale completion and observation/intent confusion before correction. Preserve desired pause, defer first/loop starts until seek acknowledgement, and reject obsolete completion. PIT exposed an inaccurate scheduler fake; consuming callbacks and resource/boundary assertions now protect recurring monitoring. Local 656 JVM cases/640 pass/16 protected skips, ratchets and builds pass; new full remote framework evidence is pending. No tolerance increase, acoustic change, private fixture access or main modification.


Closed this source-scoped framework gate at 7aa7fcb: fast, full API 35 and expanded PIT all passed; the original pause assertion now passes unchanged. Extracted actual reports, separately mapped Android coverage, inspected all 13 available library/recovery PNGs and verified both rendered README Git blobs. Recorded versioned before/after evidence with changed-denominator and survivor limits. Main/original skill/private WAVs/Samsung untouched. Full rebuild stays IN PROGRESS; continue controlled SessionWorkspace publication and then PlotSurface/remaining ViewModel ownership, without restarting validated capture or persistence cycles.

### Final finite rebuild implementation - 2026-11-09

Closed saved-diagnostic/canonical/comparison/association/optimization ownership,
permanent playback disposal, pure interval/readout boundaries and cohesive offline
WAV passes. Native regressions prove post-clear publication/optimizer rejection;
original WAV tests prove interval/timestamp overflow.757 valid outputs still match
main. Strengthened returned-text, grid and statistics assertions from actual PIT
survivors; prevent test bytecode contaminating production mutation reports. All
BLOCKER/IMPORTANT production decisions are reconciled in REBUILD_ACCEPTANCE.md.
Final integration/remote gates and source-scoped evidence are in the verification
report and draft PR1. Preserve historical records without repeating their old tasks.

### Final precision correction and finite closure

Do not resume broad redesign: all finite production decisions are closed. A final
independent STFT boundary check catches early integer sample rounding despite green
f721 CI; a failing expected-energy test precedes the three-line type/precision repair.
Current source passes527 JVM cases per variant, complete synthetic native170 plus
five configurations and strict metrics. All455 production Kotlin class hashes match
after removing an identity cast, so no redundant binary suite is invented. Publish
one final source revision and observe its existing fast/native/manual mutation jobs.
PR1 remains draft and is the current remote evidence table. Owner review follows
successful required jobs; no automatic merge, Samsung access or metronome.


### Measure and metronome presentation redesign (from 4a28143)

Preserve approved icon-only navigation and all audio, capture, persistence and
metronome engines. Measure uses compact setup/readout spacing and opt-in details
instead of a second large live-explanation card. Reserved reading slots and
Save/Discard confirmation remain intact. Metronome groups tempo, beat feedback
and transport across the real available height; intrinsic overflow still scrolls.
Normal and 150%-font ES/EN portrait/landscape native contracts and synthetic PCM
screenshots are selected by the existing focused emulator workflow. This is a
focused run, not the complete instrumentation suite.

Local sequential Gradle build, 550 debug JVM cases (542 passed, 8 protected skips),
app/test APK compilation, Lint (0 errors), coverage/complexity/CPD and 46 Python
quality tests completed. Source-bound Compose reviews renewed without changing
numeric baselines. Remote native screenshots/CI and the authorized Samsung update
remain pending; do not describe them as passed before observing results. Next:
publish for one focused API 35 run, inspect screenshots, then verify package,
signature and metadata before Samsung install-r. No private audio access.


Screenshot review of 7062629: metronome balance and all its focused configurations
passed. Normal Measure recording/readout and protected confirmations passed.
The idle action was still visibly detached in the footer; place it with the reading.
Five 150%-font assertions incorrectly demanded simultaneous visibility in a genuinely
constrained viewport. Keep the normal-size simultaneous/no-scroll contract, and
explicitly scroll to verify reachability plus retained slot/control geometry at 150%.
Do not reduce font scaling or touch targets. Preserve the strict playback seek test.
The first native run is failure 42859788837, not final passing evidence.

Samsung metadata preflight, before installation/launch: 31 sessions /31 WAV files,
19 active /12 trashed; index membership is consistent, no partial capture. This differs
from the supplied previous 30/30 reference. Physical operations are paused pending
owner confirmation of the additional session. No WAV contents accessed.


Second native run 42862160962: 28/29 pass. The remaining assertion targets a blank
scientific-note placeholder; replace it with an explicit blank-text assertion and
assert independent C#2/A4 scientific labels for accepted synthetic tones. Actual
ES150 screenshots also expose the wide-view button squeezing the header to one
letter per line. Correct the production header with intrinsic-width wrapping;
recording actions likewise wrap by real available width and system font scale.
Keep normal simultaneous controls, geometry stability and min48dp assertions.
Owner confirmed the pre-existing 31-session/31-WAV Samsung baseline; signatures
and package/code13 match. Install remains gated on passing final native verification.

### Verified UI delivery at 89cd201

Fast CI42865043802 and focused API35 CI42865033307 pass: 29/29 native
executions, 542 passed/8 protected skips per JVM variant, no failures. Final
screenshots were visually inspected and the EN/ES live showcase refreshed;
metronome captures link from both README files. No full instrumentation or fresh
mutation claim is made for this focused presentation revision. VALIDATION.md
records exact scope and the acknowledged Compose mapping limitation.

The authorized Samsung install-r succeeded with the original compatible
package/signature and code13. Owner confirmed 31 sessions/31 WAV, 19 active/12
trashed. Protected metadata and app-owned preferences match after installation,
startup and visual checks. No private audio was read or played. No recording or
instrumentation ran on the phone. Inspected idle Measure, stopped metronome,
four icon-only destinations and scrolling advanced controls; collapsed metronome
positions remain unchanged after a vertical swipe. Returned to idle Measure.

No pending implementation or installation step remains for this UI task. PR2
stays draft, main remains unchanged. Documentation/screenshot-only follow-ups
reuse unchanged 89cd201 executable evidence; do not rebuild or reinstall for
those files. Physical imagery/metadata stays ignored and must never be published.


### Localization icon-navigation regression (from origin/main383e6ce)

Known RED: full emulator run42873034926 fails Spanish Library click and dynamic
locale node counts after icon-only navigation. Use localized content descriptions
within main-navigation, require click/selection semantics, one visible heading and
no visible Spanish tab caption. Preserve English/fallback locale checks and all
session-name/index/association/WAV-byte and explorer-state integrity assertions.
Production UI is unchanged. App/test APK compilation passes locally. The existing
manual deep workflow adds a clearly labeled localization scope: two regressions
must pass first, then the complete LocalizationUiTest plus the four-destination
Back/transport ownership case on a fresh API35 emulator. Logs are required artifacts;
failed or incomplete instrumentation fails the job. Actual remote outcomes are
linked from the fix PR, not inferred from APK compilation. No physical-device or
private-recording access; PR3 remains separate (already merged by the owner).


### Workshop domain checkpoint ? IN PROGRESS

See [construction checkpoint](engineering/CONSTRUCTION_PROGRESS.md) for implemented geometry, RED/GREEN,562 debug cases,218 selected mutants and explicit remaining scope. No manufacturing UI or PDF is claimed delivered.

### Complete workshop application workflow

Production992dadc adds bounded candidates, separate construction project/catalog
persistence, three manufacturing interpretations, tolerance, actual PDF/CSV/SVG
exports and bilingual Android navigation. Reused the acoustic solver and existing
revocable workers; original designs and recordings remain unchanged. A regression
exposed the laminated first-layer inlet constraint and was corrected. Mutation
survivors led to independent dimensional assertions and fixed-size mesh allocation.

Reviewed real emulator/PDF captures; corrected IME panning, dimension formatting,
word wrapping and tile registration. Eight native scenarios passed on992dadc;
test-only70e744d then passed the remaining landscape150 scenario after closing the
active keyboard before tab navigation and requiring selection semantics. No runtime
change was needed for that final test synchronization. Full provenance, selected
647-mutant results, reproducible commands and practical model limits are in the
[construction record](engineering/CONSTRUCTION_PROGRESS.md). PR5 stays draft;
main, physical devices, private audio and official ub-mode remain untouched.


# Workshop document generator revision — 2026-11-11

Continued feature/inverse-design-construction from2e997e1. Replaced paginated text
reports with a shared pure two-sheet document/drawing model and thin Android PDF
adapter. Full-size tiles are now a separate export. Corrected rounded presentation,
minimum-stock rounding, human filenames, bilingual export vocabulary and SVG/CSV.
Native-generated synthetic samples live in docs/examples/construction; no private
files or physical-device captures. Focused native checks passed, including Spanish
150%; source gate passes without numerical exceptions. Selected mutation findings
led to independent groove, scale, reducer, layered-cut and template assertions.
Next delivery step: commit/push, inspect fast CI and one construction-native run;
record actual conclusions rather than assuming a remote pass.

Publication: implementation fefa877 on the existing feature branch. A sample-only
follow-up preserves exact Android CSV line endings/hashes; no source/test change.
Remote run URLs and observed conclusions are maintained in PR #5 delivery evidence.


# Measured PVC socket drawing clarification

Final visual review found that a schematic connector reference box could be read
as an unmeasured outside contour. Replace it with two dashed measured socket
openings at their actual insertion positions, diameters and lengths. Keep the
solid actual bore and acoustic equations unchanged. Independent tests assert the
30/25 mm insertion widths and40.2/80.2 mm socket diameters. Both JVM variants pass;
the source gate now has713 measured/156 unscored,64 historical high-risk methods,
zero violations and CPD0. Native previews must be regenerated on the existing CI
emulator: local emulator restart was rejected by automatic execution policy.
No Samsung operations or private audio access. Next: push this bounded correction,
observe fast/native CI, download the actual generated PDFs/previews and refresh
only synthetic example evidence before final delivery.


# Workshop document delivery — COMPLETE for owner review

Implementation764bc63 passed fast CI43037920159 and native CI43037955490. The
native matrix executed11 cases successfully and produced the actual bilingual
PDF/SVG/CSV and12 page previews now refreshed in docs/examples/construction.
Measured sockets replace the ambiguous PVC reference box. Sample hashes preserve
actual CSV bytes; clean API35 provenance is recorded. Final PIT487/245 killed/
231 survived/11 uncovered is disclosed; quality gate713 measured/156 unscored and
0 new violations. Limits:36 readable report rows, builder-measured material/kerf,
physical printer calibration and passive-model uncertainty. No source work remains
for this generator request. Keep PR5 draft, main unchanged, no merge or Samsung use.
The evidence-only follow-up does not change source/test inputs; reuse runtime
checks for764bc63 and inspect any automatically triggered final CI conclusion.


# Stock-aware construction drawings — implementation checkpoint

Continued PR5 after preserving the supplied style reference. Explicit log/two-board/
block models now constrain inverse candidates with kerf/preparation/measurement
bounds and report limiting positions. Old projects stay unmeasured, not implicitly
feasible. New white/warm vector sheets distinguish raw stock, prepared mirrored
halves, bore diameter, groove depth and remaining wall. No acoustic equation or
saved design/recording schema was changed. Local JVM, source gate and selected
stock mutations passed as recorded in CONSTRUCTION_PROGRESS. Next: push validated
implementation, execute the existing clean-emulator construction workflow, inspect
actual Android PDFs and stock UI screenshots, then publish synthetic examples.
No physical device or private recording operations; main and PR5 merge untouched.


Stock-aware delivery:603 JVM cases per variant (595 passed/8 protected skips),
strict755/160 source gate with0 violations, selected stock PIT128/146 killed and
no uncovered mutants. Actual API35 native run43071118247 passed14 executions;
30 rendered PDF pages exactly match inspected prior output. Synthetic current
log/rectangular examples and hashes are in docs/examples/construction. Final
UI-only checkbox semantics/header density and keyboard-free evidence changes use
the focused EN/ES150% landscape scope. Final CI observation is the remaining
publication step; source/rendering acceptance and physical limitations are detailed
in engineering/CONSTRUCTION_PROGRESS.md. Keep PR5 draft and do not merge.


## Focused workshop usability correction — 2026-11-11

Replaced the misleading pair of longitudinal half-views with three full transverse
sections and a practical carving table. Page 1 retains the complete bore with real
station/diameter markers and constant rectangular outside edges. A pure station
model distinguishes groove depth, wood below the groove and minimum side/base wall;
logs use prepared circular-cap geometry, not board subtraction. Unknown dimensions
remain unknown. Full-precision CSV includes every selected print station; SVG
matches the second sheet. Presentation-only note names and ES/EN wording improved.

The original requested PDFs were rendered before editing. Final bilingual samples
were generated and rasterized by a fresh task-owned emulator; the Spanish known-stock
PDFs were independently rasterized on Windows and visually compared across pages.
See VALIDATION.md and the synthetic gallery manifest for exact evidence. The final
caption correction points full station measurements to CSV only. No acoustic,
persistence or physical-device changes. Keep PR #5 draft; final publication/CI
observation follows the validated implementation. No merge or private-file access.

## Bilingual workshop editorial correction — 2026-11-11

Reviewed all eight known-stock PDF pages. Localized only the synthetic generator's
sample title (Ejemplo de construcción / Construction example); real user names
remain unchanged, with a regression using Río claro. PDF feasibility now separates
positive allowance, the geometric minimum with no extra allowance, failure and
unconfirmed measurements, using the unchanged assessment and its tolerance.
Log section labels identify width at the joining plane and calculated plane-to-outside
depth, rather than board thickness. Board terminology and exterior geometry remain.
Both languages use clearer bore/depth/remaining-thickness terms and native decimals.

RED usability assertions failed before the correction. Final debug JVM: 616 cases,
608 passed, 8 protected skips. Two actual API 37 native document tests passed;
all four requested two-page A4 PDFs were regenerated, independently rasterized on
Windows and visually inspected. SVG/CSV samples refreshed from the same synthetic
payload. Source gate: 768 measured,160 unscored,64 historical above-target,zero
violations,CPD0. No new review/threshold exception or acoustic/storage change.
Prior unchanged geometry/selected mutation evidence is retained; no broad PIT or
physical-device operation was repeated. Final remote CI observation belongs to PR5.
