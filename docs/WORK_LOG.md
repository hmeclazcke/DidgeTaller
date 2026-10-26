## Application-wide UX/UI audit — 2026-10-24

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
