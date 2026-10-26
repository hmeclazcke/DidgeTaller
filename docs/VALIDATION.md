## Musical Experience 4.0 — 2026-10-26

Baseline: clean `main`/`origin/main` at `005b296`. The old Library miniature
used historical saved YIN points, while session detail could display current
WAV replay plus a separate spectral component. Their incompatible data sources
and scales produced different shapes for the same recording. The musician-facing
chart now uses one `MusicalTrace` per instrument: supported spectral partials
for didgeridoo, accepted pitch for Native American flute. Library, detail and
fullscreen consume the same source series and whole-session bounds. A gap is
retained when the chosen measurement has no support. Historical YIN and SWIPE'
remain in Advanced diagnostics; coherent higher-register historical readings
are separate candidate markers and never joined into the main line.

- `:app:testDebugUnitTest :app:jacocoDebugUnitTestReport detekt
  detektComplexity :app:lintDebug :app:assembleDebug
  :app:assembleDebugAndroidTest` passed: 183 JVM tests, 0 failures, 8 skipped.
  JaCoCo HTML/XML, Detekt and lint reports were generated. Detekt is
  informational and lint reported 51 warnings plus 10 hints; this is not a
  claim of zero static-analysis findings.
- `python tools/check_localization.py` passed: 548 strings, 5 plurals and 2
  arrays with matching English/Spanish resources and positional formats.
- On an API 37 emulator, a focused private-fixture test confirmed identical
  `PlotWindow` bounds in Library, detail and fullscreen, a supported measured
  component near 30.73 s, and a playback cursor aligned within 500 ms of the
  requested position. Original WAV bytes and stored historical readings were
  unchanged. First card availability for a 39.96 s recording was 3,148 ms
  including cold analysis and UI delivery (214 component windows); this is one
  emulator observation, not a general performance benchmark.
- Inspected before/after and current screenshots are in ignored
  `checkpoints/experiments/musical-experience-4/`. A complete 105-case
  instrumentation run finished 104/105; the one failure was a test that read
  the chart cursor before asynchronous `MediaPlayer` preparation. The test now
  waits for the player and rendered cursor, and passed independently. A later
  full run stopped in the rotation case after 103 passing cases; that case
  passed independently. The user explicitly waived completion of another full
  instrumentation run for this delivery. **No final 105/105 result is claimed.**
  The physical Samsung has not been touched in this iteration.

## Application-wide UX/UI audit — 2026-10-24

Baseline: clean `514f3c4`, with `origin/main` aligned. No physical Samsung,
private recording, session index or user preference was accessed. All Android
instrumentation used `adb -s emulator-5554` on API 37 and test-owned synthetic
WAVs. The exact logs and visual matrix are in ignored
`checkpoints/experiments/ui-audit-2026-10-24/`.

- `:app:testDebugUnitTest :app:jacocoDebugUnitTestReport detekt :app:assembleDebug
  :app:assembleDebugAndroidTest` completed successfully after the responsive
  layout and navigation-label adjustments:
  179 JVM cases reported, 0 failures and 8 skips. JaCoCo XML/HTML and Detekt
  reports were generated. Detekt is configured as informational (`ignoreFailures`).
- The full emulator instrumentation run reported **100 tests, 9 failures**.
  Failures: three in `FundamentalPrecisionUiTest`, two in
  `GraphIntelligenceUiTest`, one each in `PrecisionUiTest` and `RecoveryUiTest`,
  and two in `SessionNavigationTest`. They assert legacy captured-series identity,
  old pitch viewport values or colors from synthetic timelines whose silent WAV
  now yields a different current replay. The acoustic replay code was untouched
  in this UI iteration. This run is not a passing gate and is not counted as one.
- Focused red/green emulator tests reproduced and verified the subtitle,
  chart legend, accessible chart controls, short-landscape reading, compact-phone
  navigation ellipsis and portrait status clipping. The short-landscape test was
  strengthened after a screenshot revealed a partly clipped status; its full
  text now fits above the record action. Spanish and English compiled
  resources retain matching keys and Android format tests passed.
- After an AVD restart, a further grouped emulator run completed nine
  `UIAuditTest` cases and fourteen `LocalizationUiTest` cases, then made no
  progress in the rotation class and was stopped. Those first 23 results are
  recorded, but the grouped run is **incomplete** and has no passing-suite
  result. The short-landscape 2× test was rerun independently and passed with
  its stronger full-height assertion and inspected screenshot.
- Screen inventory traversals passed in EN portrait 1.0, ES portrait 1.3,
  ES short landscape 2.0, EN compact portrait 1.5 and ES wide landscape 1.3.
  Actual screenshots were opened and reviewed. The ordinary Measure screen is
  readable in compact portrait 1.5, normal portrait 2.0 and short landscape 2.0.
  The wide simulated display still uses a phone-first single-column layout.

Only presentation, strings and UI regression tests changed. YIN, SWIPE′,
component tracking, PitchTrace, persistence and playback timing were not edited.
The emulator tests do not prove audible sample-accurate cursor alignment or a
complete TalkBack reading order. No APK was installed on the Samsung.

## Saved-session component correction — 2026-10-24

Baseline: clean `main` at `00ade24`, aligned with `origin/main`. The protected
39.96 s WAV matched its private SHA-256 manifest before and after analysis.
No Samsung command, audio write or session migration
was performed. Exact private commands, CSV, spectra and plots are in ignored
`checkpoints/experiments/drone-component/REPORT_V3.md`.

- Replayed the original WAV through current Kotlin YIN and component analysis.
  The old component gate rejected a measured ~65.026 Hz partial at 30.810 s
  because a separate ~224.75 Hz peak was near 3.5 times its frequency. The
  revised decision records `COMPETING_PERIODICITY` while retaining the direct
  partial and its 2nd/3rd harmonic support. This is a supported spectral
  component, **not** a verified physical F0. A separate sinusoidal association
  engine was unnecessary for this observed failure.
- The old UI masked component evidence near accepted YIN readings. Both
  independent layers are now visible in Both mode; Fundamental and Component
  modes are also available. No values are averaged, doubled or connected
  across measurement types. The WAV-derived YIN series is recomputed on demand
  using the current detector, source-window timing and bounded PCM reads.
  Original saved timeline bytes remain inspectable in Advanced diagnostics.
  The 140/220 Hz capture setting was not persisted. Replay uses 220 Hz only
  when an original accepted reading above the normal 140 Hz tolerance proves
  the expanded range; a deterministic 180 Hz toot replay test passed. Without
  such a reading, historical range choice remains unknowable and replay uses
  the default 140 Hz setting.
- Aligned 15:27 coverage, old versus revised YIN-or-component: normal 1
  27/28 → 28/28, I 1 27/29 → 28/29, normal 2 40/44 → 44/44, I 2 23/47 →
  46/47. Revised independent component counts are 27/28, 28/29, 39/44 and
  46/47. The longest unsupported energetic run in I 2 fell from ~2.60 s to
  one ~186 ms step. These are audible-window evidence counts, not pitch
  accuracy or proof of physical lip frequency.
- At ~38.174 s, an old ~51.44 Hz reading had full-window RMS ~0.00660 but
  YIN's actual comparison support RMS ~0.000502. The unchanged current
  0.005 support gate rejects it; original history remains untouched. A ~35 Hz
  periodicity near the second I and a ~59 Hz release estimate remain visible
  because their physical interpretation is unresolved. The ~32.7 Hz number
  in the supplied screenshot is an axis label.
- Previous fixed-seed references: 384/960 accepted known-generator spectral
  components localized within 50 cents; 0/192 silence/noise false positives.
  New reserved references: 104/104 accepted components localized, 0/39
  negative false positives, including genuine 35 Hz with strong 70 Hz second
  harmonic, 140 Hz toot-like families and competing upper peaks. Thirteen
  missing-35 windows abstained. Component localization and generating F0 are
  scored separately. Five older protected WAVs gained 7, 4, 34, 0 and 5
  supported windows; none of the new accepts had RMS below 0.005.
- Kotlin/Python 15:27 cross-check: 140 jointly accepted components, zero
  Kotlin-only, two Python-only; maximum common difference 0.0001273 cents.
  The original WAV hash remained unchanged. The real-WAV plot and actual
  emulator captures are private, excluded from Git and the APK.
- The API 37 emulator exercised the real 15:27 fixture in portrait and forced
  2400×1080 landscape, plus synthetic Spanish/English screens. Screenshots
  were inspected: both curves remain separate; pauses remain gaps; the audio
  level label no longer overlaps the dark plot. Zoom, pan, playback cursor,
  layer choice, component-only frequency-axis range, diagnostic provenance
  and session/WAV preservation were checked. The fixture was copied only
  into emulator-private storage. The final focused emulator run passed
  **10/10** cases: five component UI, two performance and three existing
  SWIPE′ comparison cases.
- A pre-existing SWIPE′ pixel comparison initially failed: the baseline
  capture occurred before new on-demand analyses finished. After waiting for
  them, a one-channel-level GPU rounding difference remained in a grid line;
  an exact bitmap comparison was replaced with a per-channel tolerance of 1.
  Camera, source level and chart restoration assertions remain. All three
  SWIPE′ UI cases then passed.
- Emulator performance: 40 s source component analysis 963 ms for 214
  frames; current YIN replay 534 ms for 214 frames. Sampled managed heap
  high-water was +4,067,328 bytes and sampled native high-water +0 bytes for
  the component test; 180 s took 4,397 ms for 968 frames. These are emulator
  measurements, not a low-end-phone or battery benchmark. Analysis is
  read-only, background and cancellable. YIN replay is bounded to 30 minutes;
  component analysis is bounded to 12,000 frames.
- Final debug JVM/Detekt/JaCoCo/debug app and test APK command passed:
  `.\gradlew.bat :app:testDebugUnitTest detekt
  :app:jacocoDebugUnitTestReport :app:assembleDebug
  :app:assembleDebugAndroidTest --offline --rerun-tasks --quiet`, with all
  protected fixture environment variables set. JUnit XML had **179 passed,
  zero failures, zero skips**. The 10 focused emulator cases passed after
  the viewport correction; the final extra core edit only selects a proven
  captured 220 Hz range and passed its JVM regression. No final full emulator
  rerun was needed for that core-only condition.

## Saved-session drone component — 2026-10-24

Baseline: clean `main` at `332d921`, matching `origin/main`; no previous
uncommitted work. The original four-bout WAV matched its supplied SHA-256
before and after. All private analysis files remain under ignored
`checkpoints/experiments/drone-component/`. No Samsung access occurred.

- Supplied SciPy spectral study reproduced on three protected October 23 WAVs:
  at most 0.0005 Hz difference from its rounded CSV. Praat tracks were read
  but not rerun because Parselmouth is absent.
- Initial component hypothesis rejected on prior held-out references:
  48 wrong accepted oscillator components and 5/148 noise false positives.
  Conservative revision froze its SHA-256 before an independent fixed-seed
  set: 384/960 known-generator frames accepted and localized within 50 cents;
  576/960 abstained; 0 wrong accepted; 0/192 silence/noise false positives.
  This evaluates spectral-component localization, not physical F0.
  A separate fixed transition/brief-toot sequence localized 18/19 eligible
  pitched windows and produced 0/1 false positives in the fully silent window;
  it abstained on the 185-Hz segment instead of carrying an old 70-Hz value.
- Kotlin/Python 15:27 aligned comparison: 114 both accepted, zero Kotlin-only,
  two Python-only frames, 0.00013 cents maximum difference among common
  frequencies. Protected JVM tests also checked October 21 19:17 and October
  23 10:17/10:18 medians. Private fixture paths are supplied only via
  `DIDGE_COMPONENT_WAV`, `DIDGE_OCT21_1917_WAV`,
  `DIDGE_OCT23_1017_WAV`, `DIDGE_OCT23_1018_WAV` and
  `DIDGE_COMPONENT_REFERENCE`.
- Existing aligned YIN chart on the four-bout WAV: 12/29 first-“I” windows,
  32/44 second-drone windows and 9/47 second-“I” windows accepted. A
  secondary component is visible in 15, 8 and 14 additional aligned windows,
  respectively, without changing accepted F0. Exact masked output, plot and
  command sequence are in the ignored private report.
- Targeted JVM tests passed, including genuine 35/70/140 Hz harmonic families,
  weak/missing low fundamentals, silence/noise, masking near accepted toots,
  flute isolation and protected real WAV comparisons.
- API 37 emulator: two bilingual UI cases passed with synthetic session WAVs;
  chart, legend, advanced decision text, zoom, pan, playback cursor, original
  WAV bytes and session index were checked. Four screenshots were captured
  and inspected locally. A separate 40-s synthetic Android performance test
  passed: 214 frames in 1.245 s. Native-heap delta was -1,028,624 bytes;
  this is not peak managed/native memory or phone battery use.
- The new processing is background, read-only, bounded to 12,000 frames and
  cancelled on session exit. YIN, SWIPE′, flute, live capture and persisted
  formats are unchanged. The real 15:27 graph is an offline reconstruction
  from the actual Kotlin results, not a physical Samsung screenshot.
- Final stabilized run: `.\gradlew.bat :app:testDebugUnitTest detekt
  :app:jacocoDebugUnitTestReport :app:assembleDebug --offline` passed.
  JUnit XML: **175 tests, zero failures, zero skips** with protected fixture
  variables set. Detekt generated its informational report; JaCoCo generated
  the debug JVM coverage report; the debug APK assembled successfully.
  The first attempted command used nonexistent app-scoped `detekt` and
  `jacocoTestReport` names and failed at task selection before executing
  validation. The task list confirmed root `detekt` and
  `app:jacocoDebugUnitTestReport`, used in the passing run.
- Final relevant emulator run passed **6/6**: two bilingual component UI
  cases, one 40-s performance case and three pre-existing SWIPE′ comparison
  regressions. It used only `emulator-5554`; no physical device was attached.

## Multi-estimator reliability follow-up

At clean `1610028`, the existing ignored Python environment executed genuine
`librosa.pyin` on the protected four-bout WAV. It reused the 450-window frozen
synthetic corpus and prior production YIN/SPTK reference outputs; the synthetic
production results were previously shown unchanged by `1610028`. The conservative
selection policy changed **zero** decisions on development or held-out references.
Both baseline and policy scored 170/190 and 250/260 correct within 50 cents,
respectively; false positives remained zero. On the real first/second “I”, YIN
accepted 12/28 and 9/46 windows, while the policy accepted the same counts.
The third method did not justify Android integration, so no app source or UI was
changed and no new Android tests were claimed. Fresh validation on unchanged app
source passed: 165/165 JVM tests, Detekt, JaCoCo report generation and debug APK
assembly (`BUILD SUCCESSFUL`, 33 seconds). No instrumented test was run because
the app UI was unchanged. The separate follow-up command and private numerical
output are in `checkpoints/experiments/multi-estimator-reliability/REPORT.md`.

## Four-bout acoustic failure: validated source and presentation correction

Baseline `f428628`, clean and four commits ahead of fetched origin/main; source
checkpoint `checkpoints/before-reproduction-1527-f428628.zip`. The protected
39.96-s reproduction WAV and two physical screenshots were available and inspected.
The WAV matched its supplied SHA-256 before and after all processing. No Samsung
command, install, instrumentation, data modification or push was performed.

### Reproduction and before/after evidence

- Actual `PitchStream → PitchDetector → LivePitch → PitchTrace`: 214 readings,
  84 accepted, 79 periodicity rejections, 51 low-amplitude windows. All 84 accepted
  points survive the trace. Detailed synchronized results and second-drone findings
  are in [PITCH_RESOLUTION](PITCH_RESOLUTION.md#four-bout-failure-reproduction-and-correction).
- The leading YIN comparison can be quiet while the trailing unused PCM is loud.
  A deterministic regression failed before the additional support-energy check;
  it now passes without changing lag selection or periodicity thresholds. One late
  51.436-Hz acceptance changes to explicit `low_comparison_amplitude`. Its raw
  candidate remains inspectable. Energetic 35.7-Hz alternatives and the 59.3-Hz release
  remain; no indiscriminate low-note or isolated-point filter was added.
- Replay of the frozen 1305-window corpus preserves **all synthetic outputs**.
  Four acceptances change on the three older real recordings; five more change on
  the two subsequent held-out recordings. Each has measured comparison RMS < 0.005.
  Known waveform-F0 held-out didgeridoo: 125 correct/135 eligible, 125 accepted,
  zero accepted errors, 10 abstentions, zero false positives/31 negative windows.
  Held-out flute: 125/125 correct, zero false positives/4 negatives. Development
  didgeridoo retains 10 octave errors; no general octave-accuracy improvement is claimed.
- Independent SciPy periodic-Hann PSD/raster comparison: maximum relative cell
  discrepancy 5.94e−8 above−70dB; all 108 time columns populated. STFT 32768 / hop 16384,
  power/dB normalization, source-time mapping and four acoustic blocks are consistent.
  The spectrogram implementation is unchanged.
- AnalysisCache.VERSION advances2→3 so explicitly requested WAV analysis cannot
  reuse results from the previous pitch policy. A seeded valid version 2 cache test
  failed before invalidation and passes afterward, preserving original WAV bytes,
  session index, stored measurements and the old cache file. No eager reanalysis,
  session migration or VISUAL_VERSION change occurs.

### Tests and actual development failures

Final full JVM run with protected fixtures enabled: **165 tests, 165 passed**, no
failures/errors/skips. Debug/app-test assembly, Detekt and JaCoCo report generation
succeeded. Detekt remains informational; this is not a zero-findings or physical
accuracy claim. The final cache-only change received a fresh debug/test build and
all 9 local-persistence emulator tests; unchanged acoustic tests were not repeated.

Across executed emulator runs, **65 distinct instrumented cases passed**:
43 successful cases from the initial broad regression, the corrected SWIPE′ cases
and recording-handoff cases, 3 new session-evidence cases, 4 native SWIPE′ cases and
9 persistence cases, with overlaps deduplicated in the local validation inventory.
This is not a single 65/65 run. The initial broad command had two actual exact-pixel
restoration failures plus an incorrectly qualified test class; neither is counted
as a pass. The restore defect came from missing top/bottom-margin keys in the
pitch-axis tick cache after asynchronous level loading. Correcting those keys
restores pixel-identical YIN charts after disabling comparison in both languages.
The correctly qualified `ar.didgetaller.audio.RecordingHandoffTest` passes.

The new audio-level tests first failed against an empty implementation, then passed
with source-time RMS, real pauses/brief sound, DC removal, partial tails, cancellation
and input preservation. No new F0 samples are derived from that strip. An initial
landscape UI attempt failed because the test scrolled only the inner horizontal
tab container and clicked offscreen. Scrolling the vertical parent first reproduces
the actual accessible interaction; both final matrix cases pass in each configuration.

### Android visual and performance checks

Only explicitly selected `emulator-5554`, API 37/x86_64:
typical1080×2400/420dpi/font1.0; compact 360 dp / Spanish / font 1.5 portrait;
landscape2400×1080/360dpi/English/font1.3. Native screenshots of the exact private
WAV replay were inspected for pitch, source-level strip, STFT, seek, full-screen
and axes. Actual scrolling reveals controls cropped at intermediate offsets;
no permanent navigation-bar obstruction was reproduced. Large text still wraps
transport and neighboring tabs remain horizontally scrollable. Fullscreen is the
recommended view when simultaneous chart/transport space is limited. Density 420,
font 1.0, portrait and default app locale were restored after the matrix.

The original replay measurements are used for historical-session screenshots;
they are not rewritten to make a before/after image look better. Original bytes
and session index equality are asserted. Existing tests exercise playback, seek,
cursor, zoom/pan, comparison cancellation/restore, instrument independence,
orientation/Back and recording finalization. Audible/sample-accurate synchronization,
full TalkBack traversal, physical gesture animation and battery are not certified.

Measured emulator costs on the 39.96-s WAV:

| Operation | Measured result |
| --- | --- |
| New RMS overview, three measured runs after warmup | 97.92–114.66 ms; about 0.0025–0.0029× audio duration |
| RMS/work arrays | 10,420 bytes; reader/object overhead excluded |
| Actual production stream including live visuals | Mean 2.772 ms, p95 4.260 ms, max 8.361 ms per analyzed callback; cadence 185.76 ms |
| Android SWIPE′, all 213 aligned estimates | Matches existing native reference within 0.01 Hz; 1428.42 ms |
| SWIPE′ native peak / largest minute-test PCM block | 2,209,916 bytes / 180,224 bytes; bounded cancellation test passed |

The RMS scan runs on the session-opening worker, once per open, with cancellation;
100-ms cells and 36,000-cell budget. It never runs another pitch detector or writes
measurements. Pixel reduction is retained while playback advances. No new native
resources or audio-capture threading changes; no Samsung-performance claim.

### Reproduction commands, artifacts and limits

```powershell
$env:DIDGE_TALLER_FIXTURES = '<protected local fixture directory>'
.\gradlew.bat :app:testDebugUnitTest :app:jacocoDebugUnitTestReport detekt :app:assembleDebug --console=plain
.\gradlew.bat :app:assembleDebugAndroidTest --console=plain
$adbPath = Join-Path $env:LOCALAPPDATA 'Android/Sdk/platform-tools/adb.exe'
# After checking ro.kernel.qemu == 1, install and instrument only emulator-5554.
& $adbPath -s emulator-5554 shell am instrument -w -r -e class ar.didgetaller.ui.SessionEvidenceUiTest ar.didgetaller.test/androidx.test.runner.AndroidJUnitRunner
```

The session-evidence test defaults to synthetic PCM; external private input is
explicitly opt-in and never an APK asset. Exact private replay commands, frozen
selection, CSVs, full results, failed/successful logs and reviewed images remain in
`checkpoints/experiments/reproduction-1527/REPORT.md`, `pipeline.csv`,
`before-after.png`, `ui-final/session-evidence/`, `ui-matrix.ps1` and
`delivery-validation.json`. No private screenshot is committed.

Unresolved: the musical oscillator is not independently measured; vocalizations
and the second drone retain ambiguity. No estimator fusion is justified. The
supplied package lacks the original saved timeline, so exact callback-delivery
offsets and equality with the phone's persisted samples cannot be certified.
No unsupported continuity or physical ground truth is claimed.

## Product UX polish and visual closure — 2026-10-23

Scope and priorities are in [UI_AUDIT](UI_AUDIT.md); reviewed synthetic-only
before/after captures are indexed in [screenshots](screenshots/ux-polish/README.md).
The starting point was clean `67b541d`, two documentation commits ahead of fetched
`origin/main/170bcf9`. The lifecycle correction was committed separately as
`4c5c5c2`; remaining work concerns UI, localization, focused tests and documentation.

### Executed checks and actual failures

- Baseline debug/app-test assembly succeeded. Six existing UIAuditTest cases
  passed, producing 29 baseline captures across measurement, Laboratory, Library,
  session/analysis, four immersive charts, empty states and rename dialog.
- New comparison-discovery and accessible-seek regressions failed before the UI
  change. An additional comparison touch-target assertion then failed; explicit
  minimum 48-dp sizing and a localized accessible name were added. Do not count
  those development failures as passes.
- Broad stabilized replay: 49 cases across PrecisionUiTest, SessionNavigationTest,
  ViewerRotationTest, UIAuditTest, SwipeComparisonUiTest, RecordingHandoffTest
  and LocalizationUiTest. 48 passed; one new audit case failed only when capturing
  a modal sheet with two Compose roots. The screenshot helper now captures the
  native window for that sheet; rerunning that complete case passed. The 48 other
  cases were not repeated merely to claim a single 49/49 run.
- The first visual matrix passed 4 compact and 5 large-phone cases, including
  the real flute-profile recording/session test. Actual native landscape capture
  inspection nevertheless exposed offscreen frequency/status in wide measurement.
  An added visibility assertion reproduced the defect. The short-height layout
  now places note and readout side by side and combines the instrument/capture
  heading. Enlarged-text review also prompted a tighter note size and an assertion
  on the complete, unclipped secondary note-label bounds.
- After that layout correction, all 6 PrecisionUiTest cases passed. The final
  strengthened rotation/visibility/edge-Back/background-save regression additionally
  passed at the typical, compact Spanish 150% and large English 130% configurations.
  Earlier chart/transport/comparison matrix evidence remains applicable: those
  components did not change in the final wide-layout correction.
- Across broad replay, corrected case, matrices and targeted repetitions,
  **51 distinct UI cases have passed**. This is a combined inventory, not a claim
  that all ran in one successful invocation or under every configuration.
- Final JVM XML:160 tests,157 passed, three opt-in private-fixture cases skipped,
  zero failures/errors. Debug and app-test APKs built successfully. Detekt and
  JaCoCo report generation succeeded; Detekt remains informational with existing
  and Compose/style findings, not a zero-findings gate. No coverage percentage
  or mutation score is claimed.

### Observable behavior

- The existing chart card now has header full-screen access, Play/Restart,
  larger elapsed/total text and an accessible seek slider. Detail reuses the
  existing 80-ms MediaPlayer position helper also used by the immersive viewer.
  A paused 5-s seek was checked against actual player position and chart cursor
  with 80-ms tolerance. Active playback survives chart changes; camera state
  survives returning to Fundamental. No independent timer/player was introduced.
- Advanced diagnostics contains the saved-session comparison and selected-window
  inspector. Opening it leaves SWIPE off; explicit activation is required.
  Collapsing keeps the active legend; switching off cancels/restores. Existing
  English/Spanish SWIPE tests still verify identical chart pixels after disabling,
  missing estimates, playback, zoom/pan and independent session instrument profiles.
- Actual MainActivity rotation/Back tests preserve the capture start/timeline/
  progressive WAV, return wide→normal rather than exiting, and save exactly once
  on actual background lifecycle. Session viewer Back and recreation preserve
  playback/camera until leaving the session, when playback stops. Completed Android
  edge gestures were exercised on API 37 gesture navigation; physical predictive
  animation and full cancelled-gesture behavior were not separately certified.
- New control labels are localized; comparison has a measured minimum 48-dp target.
  The existing dark palette now supplies surface-container tokens for dialogs/
  sheets. Full TalkBack traversal, audible alignment, external SAF destination
  handling and some secondary/legacy dialogs remain explicit validation limits.

### Environment, commands and safety

Only `emulator-5554` (API 37,x86_64) was used:1080×2400 at 420 dpi (~411×914 dp),
font scale 1.0/English;480 dpi (360×800 dp),font scale 1.5/Spanish;360 dpi (480×1067 dp),
font scale 1.3/English. Portrait and actual landscape were captured. Android light/dark
modes were exercised; the application itself has one intentional dark theme.
The emulator's density 420,font scale 1.0,default app locale and Android light mode were
restored after the matrix. No Samsung command, package/signature/version change,
new dependency or private acoustic replay was performed.

Commands used `gradlew.bat :app:testDebugUnitTest :app:assembleDebug
:app:assembleDebugAndroidTest`, then `gradlew.bat detekt
:app:jacocoDebugUnitTestReport`. Test-only screenshot changes used the app-test
assembly task; final enlarged-text build and quality logs are `large-text-build.log`
and `ready-quality.log`. Installation was exclusively `adb -s emulator-5554 install
-r` for the debug/test APKs, followed by explicit `shell am instrument -w -r -e class`
selection. No unqualified connected-device task was used.

Ignored `checkpoints/ux-polish/` contains exact commands in `run-matrix.ps1`
(including `-WideOnly`), individual logs, before/after inventories and
`validate_result.py` / `summary-validation.json`. Native/C/core, audio/data,
PitchTrace, PlotSurface, ExplorerState and navigation routes match the baseline;
no original recording, index, association or historical measurement was modified.
New UI tests compare owned WAV bytes, and existing session tests verify recovery,
index preservation, Undo and finalization. APK inspection finds no audio fixtures.
Only synthetic/emulator captures are versioned. No push or new CI run was requested.

## UX audit: recording lifecycle regression — 2026-10-23

- Baseline main/67b541d, clean; fetched origin/main still170bcf9. Devices were
  enumerated; every device command selects emulator-5554. Samsung untouched.
- Added PrecisionUiTest.rotationAndEdgeBackPreserveCaptureButBackgroundStillSavesOnce.
  Red run on the current pre-fix APK: expected CAPTURING, actual SAVING after
  real landscape recreation. Log: checkpoints/ux-polish/rotation-red-ui.log.
- Corrected onAppStopped's configuration guard ordering only. Stable working-tree
  command: gradlew.bat :app:testDebugUnitTest :app:assembleDebug
  :app:assembleDebugAndroidTest, BUILD SUCCESSFUL; JVM XML:160 tests,157 passed,
  three opt-in private-fixture tests skipped, zero failures/errors.
- The new emulator regression passes after the fix: same retained ViewModel,
  recording start, timeline prefix and progressive WAV survive actual rotation
  and an Android edge Back gesture. Returning to portrait preserves capture;
  Activity moving to CREATED saves once and resume does not restart recording.
  Broader regression log: checkpoints/ux-polish/regression-ui.log. Its remaining
  cases were still in progress when this focused lifecycle phase completed.
- No physical-device operation, private audio replay, estimator modification,
  signing/package change or original-data migration. UI polish and visual matrix
  validation are reported separately; this is not physical acoustic validation.

## Additional October 23 recordings: analytical extension — 2026-10-23

- Started at clean `main/32b97fb`, one documentation commit ahead of cached
  `origin/main/170bcf9`. Production sources match the prior source lock. The other
  conflicted worktree is untouched. No production, Gradle, fixture or device changes.
- Verified both newly supplied private originals: mono PCM16/44100 Hz, 523908 and
  1920996 frames, 11.88 and 43.56 s. All declared frames readable. Both SHA-256
  values match after processing. Paths/hashes/audio/plots stay in ignored storage.
- Reused frozen RMS/STFT selection and the existing compiled Kotlin and native C
  replay. Selected cores before estimator execution; selection SHA-256
  `8c893c4c436d0e63c15b1725db8b71b790c710507014e8b0179da716800ab11f`.
  Common nominal N=16384, hop8192, first center12288, 35–140 Hz, unchanged gates;
  internal time/spectral supports remain different. No new phonetic listening
  validation or independently known real-WAV physical F0 is claimed.
- New replay: 62+232=294 complete windows, 37 bounded C calls, 403 frequency
  outputs bit-identical between observer/control libraries, maximum input 90112
  samples. No historical estimator rerun. Actual saved-WAV adapter used through
  the established local C process bridge; no new JNI/device validation is claimed.
- Single-drone core: 37/37 agreement within both 25/50 cents, medians 70.251/70.254 Hz.
  Longer clip normal 1: 18/18, 69.598/69.591 Hz. Normal 2: 14 eligible, one pair
  disagreeing at 35/65.952 Hz, 13 SWIPE-only. Combined new normal: 55/56 pairs agree,
  69 eligible. Five-recording totals reuse cached old rows: 124 eligible, 108 pairs,
  16 SWIPE-only, 96/108 within 25 cents, 97/108 within 50 cents. No YIN-only/neither.
- I cores: 35/35 and 22/22 both accepted; 14/35 and 10/22 agree within 25/50 cents.
  Actual YIN reproduces 85/114–116-Hz choices while SWIPE stays near 68 Hz. Deeper
  later YIN minima do not supersede its first accepted dip. Whole-bout rejection
  categories and narrower user annotations are separately retained in the report.
- Focused Java `SupportProbe` calls actual production diagnostics on fixed PCM
  centers. Its accepted Hz/periodicity match replay rows. An onset 50.450-Hz result
  passes full-window RMS despite sub-gate leading comparison support; a middle
  35-Hz result instead has high-energy comparison support. These are distinct
  mechanisms; no correction or blanket low-note suppression was implemented.
- Local commands, exact classpath and results live in the ignored
  `checkpoints/experiments/stable-drone-comparison/oct23/REPORT.md` and
  `run-command.json`. Ran `run_extension.py`, `analyze.py`, `evidence.py`,
  `run_support_probe.py`, `plots.py`, `validate.py` with the existing pitch-resolution
  Python environment. Source/audio/selection locks, timing grid, thresholds,
  differences, denominators and diagnostic parity checks pass. Reviewed three
  comparison PNGs plus the estimator-blind selection plots.
- No packages installed, uploads, app tests/builds, emulator/Samsung operations,
  performance claims, CI run or automatic push. Production behavior is unchanged.

## Stable normal-drone agreement investigation — 2026-10-23

- Started clean at main/170bcf9. Re-read AGENTS.md, ub-mode, required context,
  PITCH_RESOLUTION and native/Kotlin processing conditions. No production changes,
  Android builds, new algorithm benchmarks, packages, devices or cloud audio calls.
- Inventoried 16 local read-only backup TARs, protected fixtures and loose diagnostic
  WAVs. SHA-256 deduplication found 18 original contents: 13 stored Didgeridoo and
  five stored flute. Six loose v325-private WAVs are duplicates. Synthetic experiment
  inputs were excluded. The 10:17/10:18 October 23 files were not found. Stored
  instrument labels are not independently verified physical-source labels.
- Selected intervals before estimator execution using annotations and PCM-only
  RMS/STFT descriptors. FROZEN_PROTOCOL.json records thresholds/supports. Final
  selection SHA-256: 1d0c68664d8f9299389bc0f37e34226876569559deb39dfb0dfa932d1618943b.
  Two first normal bodies use musician sequence plus inspected amplitude/spectrogram;
  two cores lie inside the confirmed 19:17 normal label. Unannotated stationary
  candidates remain separate. No pitch output or agreement enters selection.
- Compiled current PitchStream/PitchDetector/LivePitch and SwipeAnalysis classes
  using the existing Kotlin compiler caches, without Gradle/Android builds. The
  actual saved-WAV adapter invokes current SPTK C through a local binary process
  pipe. An observer-only C copy reads the actual winning strength matrix value
  before native pitch selection; each call also executes the control library and
  asserts bit-exact frequency equality. No JNI execution is claimed for this task;
  native Android parity was established in the preceding implementation tests.
- The first offline harness attempt failed its guard because an evidence map let
  later overlap-context outputs overwrite earlier core scores. Corrected the
  ignored harness to retain only each call's selected core; the production adapter
  was unchanged. The successful run contains 1571 complete matched windows over
  13 recordings, 202 bounded C calls and 2162 bit-identical native control outputs.
  Maximum kernel input: 90112 PCM samples. All 652 previous YIN rows match exactly,
  and maximum SWIPE difference from the established whole-signal reference is 0 Hz.
- Comparison uses 35–140 Hz, source 44100-Hz PCM16, nominal N=16384/hop=8192,
  first center=12288. YIN mean-removal/downsampling/leading difference support and
  SWIPE centered spectral supports differ and are explicitly documented. Common
  centers are not identical instantaneous observations. Acceptance thresholds and
  separate RMS gates stay unchanged. Both metrics/raw candidates/rejections and
  signed Hz/cents differences are retained; confidence values are not equated.
- Primary cores: 19:04 2.96788–8.66788; 19:17 3–4.46788 and 5.01788–6.71788;
  19:26 2.21788–5.01788. Total 55 eligible, 52 both accepted, three SWIPE-only,
  zero YIN-only/neither. Agreement: 41/52 within 25 cents; 42/52 within 50 cents.
  Crucially, all ten accepted pairs in the confirmed 19:17 cores disagree, while
  all 42 pairs in the other two sequence-supported normal bodies agree <=50 cents.
  Overlapping windows are descriptive observations, not independent trials.
- Full confirmed 19:17 3–8-s annotation, without the stationary mask: 0/21 pairs
  agree <=50 cents, from 25 eligible windows. Eighteen consecutive disagreements
  occur at centers 4.55111–7.70902 s; seven survive inside the stricter middle core.
  Near-octave counts across primary pairs are two within +/-100 cents of 1200,
  three within +/-150; do not call every 35–37 versus 64–69 pair an octave error.
- Corrected interpretation of the requested timestamp: 5.294149660 s is the window
  END, center 5.108390023 s, input 4.922630385–5.294149660. Actual YIN 36.496871 Hz,
  periodicity .715757; SWIPE 65.833369 Hz, strength .391067; difference 1021.26 cents.
  Neighbors persist in different registers. Reused the independently documented
  spectral family near 69.57/139.20/208.06 Hz plus strong 182.22-Hz component and
  prior difference-function/ACF evidence; no third estimator was executed.
- Confirmed I 9–17 s: 42 eligible, 14 both accepted, 27 SWIPE-only, one neither,
  zero YIN-only; all windows exceed the digital energy gates. Agreement 1/14 <=25c,
  7/14 <=50c; accepted medians 61.86/64.95 Hz. The seven larger differences stay in
  the same register, not near-octave. Coverage is not evidence of physical accuracy.
- Exploratory stationarity inventory: 32 intervals in nine recordings, 513 eligible,
  418 accepted pairs, 398/418 <=50c. Unknown technique and confirmed vocalization
  must not be silently pooled as normal drone. Four Didgeridoo-profile recordings
  have no qualifying stationary interval but their full timelines were analyzed.
- Validated denominator identities, timestamp mapping, score/gate acceptance,
  known source-window alignment and all prior 652 reference rows. Rehashed all
  18 inventoried source contents/copies and 190 audio members across the 16 archives;
  source bytes and archive size/mtime are unchanged. Source-lock and environment
  files record the exact local classes, C source, reference DLL and library versions.
- Reviewed estimator-blind selection plots and aligned comparison figures. Plots
  retain accepted dots, rejected gutters, RMS/STFT and separate quality panels;
  no line bridges missing estimates. Frozen labels distinguish confirmed/sequence
  normal cores, confirmed I, low-energy input and unclassified candidates. Exact
  unannotated attacks/releases or vowels were not invented from pitch estimates.
- Reproduce locally from repository root: inventory.py, select_audio.py (existing
  pitch-resolution venv), build_observer.py, run.py, summarize.py (same venv),
  validate.py, make_report.py, all in checkpoints/experiments/stable-drone-comparison.
  Exact compiler/run commands, source hashes, REPORT.md, aligned/classified CSV,
  summary.json, validation.json and private PNGs remain ignored there. Only concise
  aggregate documentation is committed; no audio, private plots, fixtures, personal
  paths or experimental dependency is published. The investigation does not justify
  automatic fusion or identify a uniquely correct physical drone source.

## SWIPE production review and register reliability — 2026-10-23

- Baseline was clean `main` at published `dbed749` (user's manual commit), equal
  to origin/main after fetch. No assumption was made that Codex had committed the
  previous work. AGENTS.md, ub-mode, required product/roadmap/validation context,
  current native/JNI/Kotlin processing, licensing and existing experiments reviewed.
  Other worktrees/history were preserved. No Samsung commands were issued.
- Production decision: retain YIN as the normal didgeridoo detector. Current
  SWIPE′ is optional saved-WAV analysis only; there was no continuous dual-detector
  normal path to remove. Microphone uses PitchStream/PitchDetector/LivePitch, saved
  reanalysis WavAnalysis uses YIN, and original captured samples remain canonical
  for Fundamental. Flute stays on its existing full-rate YIN/range. No new fallback,
  estimator substitution, confidence conversion, schema change or data migration.
- TDD: new native known-140-Hz test failed with “140 Hz became 35.0 Hz”; chart test
  failed because a strong measured 35→70 transition had a connector. Corrected
  upstream boundary assignment pc[0]→pc[maxi], and cut didgeridoo connectors across
  >=600 cents regardless of periodicity. Both endpoints/events remain; no filter
  or low-frequency cutoff was introduced. The first full JVM run had two existing
  assertions requiring the old strong-register connectors; updated only those
  connector expectations while retaining raw-point/toot/continuity assertions.
  Failed development logs remain ignored, not counted as passed validation.
- Reused the frozen deterministic dataset and previous YIN CSV. Re-executed only
  SPTK with the one-line correction, independently built using existing TinyCC;
  no downloads/installations. Development known waveform F0: 84/124 correct versus
  74/124 before (same 114 accepted); ten pure-140-Hz two-octave errors disappear,
  now 138.993414 Hz. Held-out didgeridoo: YIN 125/135 correct, 125 accepted, zero
  >50-cent errors; SWIPE′ before/after 96/135 correct, 125 accepted, 29 errors.
  Accuracy among accepted: 100% versus 76.8%; both coverage 92.6%, ten abstentions,
  zero integer-octave errors and zero false positives /31 negative windows.
  No superiority or physical-frequency claim is supported. Separate held-out
  69.567+182.221-Hz component target remains 20/60 for both methods. A bounded local
  cubic-interpolation arithmetic check achieved only 105/135 and was not integrated.
- All 652 complete aligned SWIPE′ real-WAV windows are unchanged by the boundary
  fix. Actual production-stream tests replayed the three manifest-verified private
  WAVs read-only. Against existing `real-audio/final` CSVs, all first 16 raw fields
  match across 654 decisions (256/122/276 including two partial tails), graph
  classification reasons match, and zero additional real-recording connectors
  are removed. New register-cut behavior is demonstrated on known synthetic point
  transitions, not falsely attributed to those recordings.
- Pre-chart real outputs contain three YIN and four SWIPE′ accepted 45–55-Hz
  hypotheses. They exist before projection; RMS ranges from substantial to near
  the presence gate. Their physical correctness is unresolved, so no sample was
  deleted or promoted. Native strength >0.3 is already enforced; RMS is only a
  presence gate, not proof of pitch. Source-grid timestamps are monotonic and
  aligned; old Android callback timestamps remain unchanged and are not treated
  as independently calibrated oscillator timing. The October 23 10:17/10:18 WAVs
  were not found locally; those specific points and 115-Hz events were not tested.
- Native/metadata change: seven vendor files remain byte-identical. PROVENANCE.json
  records the modified file's original and current SHA-256 and exact correction;
  all MIT/BSD-style notices retained, APK notice updated. Independent desktop
  golden changes only nine public pure-140-Hz rows (35→138.993414); the explicit
  known-F0 regression additionally checks interior native output. No range or
  threshold tuning. Native 44.1-kHz/35–140-Hz guard, four ABIs, bounded allocation
  cleanup and 32-MiB cap remain unchanged. Unsafe flute configuration still rejected.
- Full stabilized JVM/debug/test-APK, JaCoCo and informational Detekt/complexity
  tasks passed (`final-validation.log`): 160 discovered, 157 passed, three optional
  fixture tests skipped. All three opt-in private classes also passed separately
  with DIDGE_TALLER_FIXTURES and DIDGE_DIAGNOSTIC_LABEL=swipe-consolidation
  (`private-pipeline.log`). No test total is presented as a permanent project metric.
  Standard command: `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:jacocoDebugUnitTestReport detekt detektComplexity --console=plain`.
- Explicit emulator-5554/ranchu/API37/x86_64 instrumentation: nine distinct tests
  passed in 80.825 s: four SwipeNativeTest cases (golden/noise, native boundary,
  protected WAV parity/hash, bounded minute/cancellation), RegisterPresentationUiTest,
  three SwipeComparisonUiTest cases (en/es and actual navigation), and existing
  FundamentalPrecisionUiTest#shortTootAndFluteRemainVisible. No full instrumented
  suite or physical-device test. The initial red native case is separate evidence.
- Emulator offline analysis: 22.92-s private WAV/121 points in 1769.15 ms; synthetic
  minute/321 points in 5126.86 ms. Tracked maximum native work: 2,209,916 B including
  headers; maximum PCM read 180,224 B. Extra reader/result/VM memory is not included;
  these are not process-wide peaks or proof of real-time capture/ARM/battery safety.
  The 30-minute cancellation case remains bounded to one block. Production YIN
  capture cost was not altered. Native numerical results match the independent
  corrected reference within 0.01 Hz; original private outputs have zero delta.
- Pixel assertions and reviewed [register capture](screenshots/swipe-comparison/register-separation.png)
  verify both groups without a fictitious sweep. Brief valid low events, attacks,
  releases, genuine register changes, flute policy and breathing gaps have JVM
  coverage. Existing en/es comparison tests preserve viewport and exact chart
  pixels on disable, WAV bytes/index/timeline and the independent signal inspector.
  Localization parity passes (519 strings/five plurals/two arrays). No new UI text.
- Detailed reproducible CSV/JSON, compiler command, failed/passed test logs and
  screenshots remain ignored in `checkpoints/experiments/swipe-consolidation/`.
  Public reproducibility uses focused native/JVM/UI regressions, the golden TSV,
  source provenance and tools/generate_swipe_reference.py. Original recordings,
  session names/design associations/settings and package/signing config unchanged.
  Default promotion and the specific October 23 acoustic interpretation remain
  unresolved; the current evidence does not justify silently deploying a new main
  pitch estimator merely because its comparison curve is more continuous.

## Optional on-device SWIPE′ comparison — 2026-10-23

- Clean baseline `600d70e`, original main synchronized with origin/main. AGENTS.md,
  ub-mode, product/roadmap/validation context, PITCH_RESOLUTION.md and ignored prior
  SPTK experiments were reviewed. Other worktrees, including the conflicted old
  documentation worktree, were untouched. Configured author/committer identities
  matched before committing. No baseline or empty commit was created.
- Reused byte-identical legacy SPTK C from the independently tested pysptk 1.0.1
  distribution. Eight upstream SHA-256 hashes verified; MIT/BSD-style notices are
  in source and APK assets. Added only a minimal header, bounded allocation/JNI
  adapters and CMake/NDK build integration. No Python/model/runtime network access.
  The first native build obtained pinned NDK 27.0.12077973/CMake 3.22.1 using
  accepted SDK licenses and compiled all four ABIs. CI installs these explicitly.
- TDD: SwipeAnalysisTest initially ran four tests with three expected behavioral
  failures before the analyzer was implemented. A subsequent camera-restoration
  test failed because comparison Reset changed the original extended-range flag;
  retaining/restoring the original presentation flags fixed it. Both red logs
  remain ignored. These were development failures, not successful validation.
- The first native synthetic check found nondeterministic flute output on identical
  PCM (approximately 426.9 versus 439.4 Hz). Source inspection found the final ERB
  interpolation query outside the smallest FFT arrays for 200–1600 Hz. This task
  deliberately restricts SWIPE′ to 44.1-kHz didgeridoo sessions, with native guards
  before upstream execution; the production flute path is unchanged. No unsafe
  flute results are presented as validated estimates. See SWIPE_COMPARISON.md.
- Stabilized command:
  `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:jacocoDebugUnitTestReport detekt detektComplexity --console=plain`
  **BUILD SUCCESSFUL**. JVM XML: 157 discovered, 154 passed, three optional private
  fixture tests skipped, zero failures/errors. Seven new JVM tests cover alignment,
  bounded reads, presence gating, gaps/register jumps, short/unsupported/oversized
  input, cancellation and exact camera/range restoration. JaCoCo and informational
  Detekt/complexity reports generated; existing audit warnings remain, not a clean
  static-analysis claim. Localization checker: 519 strings, five plurals, two arrays
  with en/es positional-format parity.
- Final guarded instrumentation used only `adb -s emulator-5554`, hardware ranchu,
  API 37 x86_64, 1080×2400/density 420. Install/update of both APKs succeeded.
  AndroidJUnitRunner class selection:
  `ar.didgetaller.ui.SwipeComparisonUiTest,ar.didgetaller.core.SwipeNativeTest,ar.didgetaller.ui.SavedPitchEvidenceUiTest,ar.didgetaller.ui.SessionNavigationTest#immersiveViewportPinchInspectionResetAndBackPreserveSession`.
  **Nine tests passed in 73.23 s**: three comparison UI cases (en/es and real
  TallerApp navigation), three native numerical/resource cases, two existing
  signal-evidence UI cases and one existing immersive navigation/camera regression.
  A prior five-case focused run also passed; it is a repeat, not five extra distinct
  cases. No full instrumented suite or physical-device operation was performed.
- Native golden tests use deterministic public PCM and the independent Windows
  legacy C library, not self-generated Android expectations. Pure 35/70/140 Hz,
  weak/absent first harmonic, silence and broadband noise were checked, including
  known SWIPE′ inaccuracies rather than only attractive outputs. Unsafe flute
  configuration is rejected. Private numerical test used an emulator-only copy
  outside the session library, passed as `swipeFixture`/`swipeReference` arguments.
  Original private WAVs were only read, never bundled or committed.
- All 121 aligned windows of the protected 19:17 recording match the prior whole-
  signal SPTK output, including missing estimates: observed maximum delta 0 Hz,
  asserted tolerance 0.01 Hz after the common RMS gate. Fully contained normal
  3–8 s: SWIPE′ 25/25 accepted (63.84–68.56 Hz), baseline YIN 21/25. Confirmed
  “I” 9–17 s: SWIPE′ 41/42 (62.53–66.49 Hz), baseline YIN 14/42. No physical
  oscillator accuracy can be inferred from these coverage figures. Prior held-out
  synthetic superiority of YIN remains unchanged; no new correction rule was tuned.
- Final measured emulator analysis: protected 22.92-s WAV/121 points, **1709.94 ms**;
  generated 60-s WAV-equivalent source/321 points, **4620.98 ms**. Maximum tracked
  native allocations across blocks: **2,209,916 bytes**, including allocation
  headers; largest PCM read: **180,224 bytes**. Reader byte buffer and Kotlin result
  objects are additional; these are not whole-process peak-memory measurements.
  A virtual 30-minute source was cancelled after one native block; a full 30-minute
  wall-clock/battery run was not performed. Native calls cap input at 262,144 samples
  and allocations at 32 MiB; retained curves cap at 20,000 points. No low-end-phone,
  ARM runtime, thermal or battery claim is made. ARM ABIs compiled successfully.
- Stripped native library sizes: arm64-v8a 35,192 B; armeabi-v7a 24,356 B; x86 38,460 B;
  x86_64 35,176 B. No C++ runtime was introduced. Native ELF segment alignment and
  packaged notices were inspected. Android estimates are offline/centered, not a
  real-time detector. Historical YIN delivery timestamps remain unchanged.
- Additional focused checks after the stabilized run added a weak-first-harmonic
  golden case and a low-amplitude RMS assertion. The seven analyzer JVM tests and
  the native synthetic parity test passed again; no production numerical rule
  changed. ELF LOAD alignment was 0x4000 on all four ABIs; linked libraries were
  only libm/libdl/libc. The APK notice matched its reviewed source bytes.
  A final cancellation-error guard was compiled and the seven analyzer JVM tests
  plus the English comparison UI case passed again (14.01 s for that UI repeat).
  All 104 relative file/image references in the reviewed documentation exist;
  the three original protected WAV hashes still match their fixture manifest.
  Git attributes preserve the upstream files' exact bytes and whitespace instead
  of normalizing the CRLF header or reformatting retained license notices.
- UI checks cover explicit opt-in, experimental/dashed legend, shared projection,
  advancing playback cursor, inspection, pinch/pan, independent preview/fullscreen
  cameras, cancellation/exit and session instrument ownership. Disabling restores
  both the original viewport and identical chart bitmap in en/es cases. Original
  WAV bytes, session JSON and stored timelines compare exactly before/after. An
  upstream native call already running finishes its bounded block on cancellation.
- Reviewed real captures: [screenshots and capture conditions](screenshots/swipe-comparison/README.md).
  An initial light-theme legend lacked contrast; the text now uses a darker purple
  on light surfaces. Default app-theme and Spanish harness captures were inspected.
  Signal evidence at selection remains present and its two existing tests pass.
- Reproduction: public golden generator `tools/generate_swipe_reference.py`; native
  tests and synthetic TSV in androidTest. Detailed private TSV, hashes, build/test
  logs, verification.json and timings remain ignored under
  `checkpoints/experiments/swipe-android/`; prior source/reference evidence remains
  under `checkpoints/experiments/pitch-resolution/`. No Samsung, WAV mutation,
  capture/YIN/PitchTrace logic change, schema migration or historical rewrite.

## Pitch engineering resolution and optional saved-WAV evidence — 2026-10-23

- Clean baseline `4e6a06a`, main synchronized with origin/main; existing conflicted documentation worktree preserved untouched. Read AGENTS.md, ub-mode, required context, prior extended-range/filter/interference/normal-drone/harmonic-tracker/prototype/diagnostic evidence, production paths and tests. No baseline commit needed. Configured author/committer identities matched before the first commit. No microphone/Samsung/private-data mutation.
- One integrated benchmark under ignored `checkpoints/experiments/pitch-resolution/`: frozen deterministic protocol, 51 development/held-out synthetic clips, actual current PitchStream→PitchDetector→LivePitch baseline and 652 complete windows from three manifest-verified private WAVs. Ran genuine librosa 0.11.0 pYIN, unmodified bundled SPTK SWIPE′ C from verified pysptk 1.0.1 source, and the pinned author causal SWIPE reference. Isolated Python environment and a 489,586-byte official portable C compiler; no Android dependency. No neural runtime/model installed. Original source/license inspection and upstream hashes retained locally.
- Full commands/scripts: `generate.py`, `run_production.py`, `compare.py`, `compare_sptk.py`, `finalize_metrics.py`, `run_integrated_audit.py`; `reproduce.py` reruns them offline against the existing inputs/toolchain, without installation or Android/Git operations. Protocol, frozen dependencies, source hashes, compiler commands, CSVs, timing data and three PNG/SVG figures remain ignored. Source archive verified against PyPI SHA-256. Initial official compiler download mirror timed out; the official mirror endpoint succeeded. Initial C compile exposed a duplicate C99 round fallback; platform macro `-Dround=round` resolved it without editing algorithm sources.
- Results and measurement definitions are consolidated in PITCH_RESOLUTION.md. Held-out known-waveform F0: YIN 250/260 correct, zero accepted errors and 10 abstentions; pYIN 238/260, 12 octave errors and 10 negative false positives; bundled SWIPE′ 219/260, 29 >50-cent errors (nine >20% relative errors), 12 abstentions; author causal SWIPE ties YIN but fails a development genuine-low missing-harmonic condition. More real coverage did not establish physical correctness. Outcome B selected; no estimator replacement, octave correction, or blended pitch policy.
- New core characterization red: `:app:testDebugUnitTest --tests '*SavedPitchEvidenceTest'` reports seven failing tests against an empty inspection skeleton (`core-red.log`). First implementation leaves one failure because a fixed 0.15-Hz assertion was too strict at 880 Hz (actual unchanged detector 880.171); corrected the test to a five-cent tolerance while retaining exact equality to the actual baseline. Final full JVM results: **150 discovered, 147 passed, three optional private cases skipped, zero failures/errors**.
- Stabilized broader check: `.\gradlew.bat :app:testDebugUnitTest :app:jacocoDebugUnitTestReport :app:assembleDebug :app:assembleDebugAndroidTest detekt detektComplexity --console=plain` -> BUILD SUCCESSFUL, 25 s (`final-validation.log`). Initial integrated JVM/debug/test-APK build also passed in 27 s. JaCoCo/Detekt and their summary scripts passed with informational findings; no quality threshold or Android coverage claim. Existing reviewed-method scores with stale source correspondences remain omitted, rather than updating the audit manifest without review.
- Protected opt-in, process-scoped `DIDGE_TALLER_FIXTURES`: `:app:testDebugUnitTest --tests '*RealSavedPitchEvidenceTest'` -> BUILD SUCCESSFUL, one real-WAV case passed, manifest SHA-256 intact (`private-evidence-test.log`). Environment removed afterward; ordinary broad validation keeps private fixtures disabled. Normal-drone inspection retains the lower YIN value alongside a higher spectral candidate; I inspection exposes strong lower periodicity without promoting it; breathing inspection returns low-energy with no candidates.
- Additional direct audit of the actual integrated core on **1305 windows**: accepted values and periodicity exactly equal the unchanged production decision on every exact inspected window. Median 2.010 ms, p95 3.871 ms and median allocated 445,712 bytes per desktop request, excluding the first 20 warm-up requests. Bounded on-demand computation, not a low-end Android real-time benchmark. Initial Java source-mode harness compile rejected a PowerShell UTF-8 BOM; normalizing only that ignored helper fixed it.
- UI checks target only emulator-5554 after hardware=ranchu and boot confirmation. Started the existing API 37 AVD read-only, without wiping/saving its snapshot. Explicit install-r of app/test APKs succeeded. Two new bilingual cases plus two existing playback/navigation cases pass together: **four distinct cases**, 14.202 s (`emulator-tests.log`). New cases also pass separately after native screenshot/container refinement (two repeats), then finally with line-layout assertions (two further successful repeats, 12.598 s, `ui-layout-final.log`). No full UI suite or font/size matrix is claimed.
- A strict added didOverflowWidth assertion failed in two cases and one diagnostic repeat: Text reported intrinsic width 794 px while retaining a 912-px paragraph constraint. Replaced that assertion with actual line extents, no ellipsis, one-pixel integer-rounding allowance and height checks, following the existing localization-test approach. Both languages pass; production was unchanged for this test correction. Earlier failed/interrupted checks are not counted as successful cases.
- Reviewed native English/Spanish component captures using synthetic data; actual line bounds fit. Native screenshots supersede the initial Compose capture attempt. UI tests verify collapsed/on-demand behavior, no rerun when the cursor changes, explicit pause result without retained Hz, cancelling a pending request on exit, and byte-identical original synthetic WAV/session JSON. Spoken TalkBack, full physical layout matrix and acoustic hardware accuracy remain unverified.
- `python tools/check_localization.py` passes: 509 strings, five plurals, two arrays with en/es and positional-format parity. No modification to original PitchDetector, PitchStream, LivePitch, instrument profiles, PitchTrace, chart adapters, WAV lifecycle, storage formats, caches, manifest, Gradle or app version. Three private WAV hashes remain intact. No personal recordings, diagnostic CSVs, downloaded sources, binaries, models or environments are staged.
- Review explicit staged paths/statistics/diff/whitespace before the focused commit. Publication is authorized only as a normal fast-forward; final commit/CI status is reported at delivery. Recoverable starting reference `4e6a06a`; preserve subsequent work before recovery. Physical I/source-pitch ambiguity remains unresolved; the implemented benefit is optional transparent evidence, not improved absolute F0 accuracy.

# Documentation overhaul — 2026-10-23

- Scope: documentation only, based on the clean working tree at `8d4cab3`. Initial `git status --short --branch` and `git log origin/main..HEAD --oneline` confirmed the two existing local commits `bb92981` and `8d4cab3`. No baseline commit was needed. Read AGENTS.md, the local ub-mode skill, required project context, relevant guides, Gradle/workflow files, localization resources, and focused source paths. No application/configuration/test changes.
- Rebuilt README.md in English and added its complete Spanish counterpart. Reworked README-LAB.md, ARCHITECTURE.md, SESSION_EXPLORER.md, AUDIO_ANALYSIS.md, and HARMONICS.md; added DEVELOPMENT.md as the single developer-check reference. Removed stale front-page version/test totals, obsolete navigation, duplicated onboarding, agent prompts, and device-specific incident narration from the presentation. Historical audits, roadmap, research notes, device reports, and earlier validation/work-log entries remain intact.
- Cross-checked toolchain/dependency versions, Android SDKs and manifest, routes/control labels, profile ranges, WAV/analysis pipeline, harmonic rules, cache behavior, CI tasks, and the existing MIT LICENSE against repository files. Didgeridoo-only design tools remain explicitly experimental; physical vocalization I, microphone accuracy, playback synchronization, sustained capture/battery, and TalkBack limits remain explicit.
- Lightweight Python standard-library check, supplied through a PowerShell here-string to `python -`: **8 documents, 110 relative links/images, four distinct tracked PNGs**, all local targets/anchors valid. Checked UTF-8, balanced fences, language navigation, matching en/es heading structure, screenshot paths, versions, and referenced wrapper/tool files. The first helper invocation failed its Unicode language-label assertion because PowerShell piped the script using its default encoding; rerunning with explicit UTF-8 OutputEncoding passed. No application failure was involved.
- Visually inspected the four selected source images and their evidence indexes. All are existing 1080×2400 emulator captures using documented synthetic signals/sessions; no private audio/device content was added. PNG signatures/dimensions and tracked status pass. Both READMEs use the same assets and disclose older UI placement. Existing external research URLs were retained; no external availability audit or hosted GitHub rendering check was performed.
- `git diff --check` passed. Reviewed the explicit staged filenames, statistics, complete staged diff, and `git diff --cached --check` before committing. Only Markdown is staged; no new binaries, fixtures, screenshots, recordings, dependencies, resources, or build files. Existing Git author/committer identities matched the user-configured identity.
- No Gradle task, APK build, JVM/Android test suite, emulator/physical-device command, private WAV access, tool installation, network publication, or CI run was performed for this documentation-only task. Commands shown in the guides were verified against checked-in configuration and existing evidence, not re-executed. The documentation commit remains local to avoid pushing the two unrelated local feature commits. Recovery baseline: `8d4cab3`; preserve later work before any recovery.

## Acoustic uncertainty interface and final validation - 2026-10-22

- Core phase committed as `bb92981` (`feat(audio): retain bounded acoustic uncertainty diagnostics`). UI red: the new controlled-PCM emulator case fails on the absent `acoustic-diagnostics-toggle` before implementation (`checkpoints/acoustic-ui-red-emulator.log`). Its initial test-only Session/File argument typo was corrected before running this red case; that compile failure is not counted as behavioral evidence.
- Added a collapsed diagnostic panel on normal Measure view and amber uncertainty feedback. Details use 16 new English/Spanish positional resources, with selected candidates explicitly diagnostic-only, normalized RMS, YIN periodicity, localized rejection reason, last-frame provenance and bounded retention count. Existing detection status strings remain; no Session chart overlay, historical inference or saved data change. Ordinary localized Text/Button semantics expose these details without a custom accessibility tree.
- Final production command: `.\gradlew.bat :app:testDebugUnitTest :app:jacocoDebugUnitTestReport :app:assembleDebug :app:assembleRelease :app:assembleDebugAndroidTest detekt detektComplexity --console=plain` -> BUILD SUCCESSFUL, 1m33s, 123 tasks (`acoustic-uncertainty-final-build.log`). JVM XML: 142 discovered, 140 passed, two optional private tests skipped, zero failures/errors. The separate explicit six-window private test already passed in the core phase; no claim that both private tests ran in the full suite. Debug and unsigned release APKs, release vital lint, coverage XML/HTML and informational Detekt/complexity reports generated successfully.
- English app locale `en`: new `RecoveryUiTest#acousticUncertaintyDetailsDoNotBecomeNotesOrPersistedMeasurements` plus existing `controlledPcmReachesLiveMeterAndProfilesResetWithoutGraphFiltering` -> 2/2, 35.601s (`acoustic-ui-en.log`). Spanish `es-AR`: new case -> 1/1, 21.873s (`acoustic-ui-es.log`). Two distinct cases / three successful executions, not three distinct cases. All commands explicitly target emulator-5554, guarded ranchu/API 37, with isolated synthetic files/preferences. Original empty app-locale override `[]` restored after each run. No AudioRecord/recording is used by these injection tests.
- New Android case verifies noise/accepted tone/low signal, localized raw candidate and reason, missing measured Hz and empty PitchTrace on noise, unchanged accepted 70 Hz, retained evidence after stop, clearing on profile selection, and byte-identical original synthetic session JSON/WAV. Existing regression also covers flute and profile reset. Reviewed native screenshots `checkpoints/acoustic-ui-en.png` / `acoustic-ui-es.png` (ignored, synthetic): status, explanatory text and diagnostic candidate fit; remaining details use normal vertical scrolling. No full multilingual/layout matrix, physical audio, TalkBack traversal or battery/latency benchmark was run.
- `python tools/check_localization.py` -> 496 strings, five plurals, two arrays, en/es parity and positional formats valid. Exact source comparison confirms all detector methods, LivePitch and PitchTrace are unchanged from `33792ba`; only Reading's optional metadata was added to PitchDetector.kt. No second estimator, threshold/range/Hz change, WAV lifecycle, serialization, dependency/toolchain/version change or Samsung access. History is transient; old sessions remain unclassified. Public architecture and limits: ACOUSTIC_UNCERTAINTY.md.
- Two local commits are the requested delivery; no push or CI is triggered in this iteration. Starting recovery reference remains `33792ba`; preserve later work before recovery.

## Bounded acoustic diagnostic transport - 2026-10-22

- Red: normal production PitchStream output drops an energetic rejected candidate; the new regression fails with `Production stream must carry rejection evidence` (ignored `checkpoints/acoustic-diagnostics-red.log`). Added only an optional Reading metadata field before red; no estimator logic was changed. Green transports the existing diagnostic and retains up to 128 compact entries in the ViewModel, with three evidence states and the existing LivePitch gate.
- `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --console=plain` passed (26s, `acoustic-diagnostics-core-build.log`). A subsequent nullable-observation guard was compiled/checked by the private-window test, then the full JVM/debug tasks passed again in 13s (`acoustic-diagnostics-core-final.log`): 142 discovered, 140 passed, two optional private tests skipped, zero failures/errors. No private fixture environment is enabled for the full suite.
- Explicit opt-in: set `DIDGE_TALLER_FIXTURES=C:/projects/DidgeTaller_local_fixtures`, run `:app:testDebugUnitTest --tests '*RealAcousticDiagnosticsTest'`, then remove that process environment variable -> BUILD SUCCESSFUL, 4s (`acoustic-diagnostics-private-windows.log`). One test checks six windows of the 19:17 WAV against pre-change golden Hz/periodicity and verifies the original hash before/after. No other private WAV was replayed. Synthetic tests cover all three states, unchanged detector outputs, diagnostic-only rejected candidates, profile rejection, bounded eviction and clear/snapshot behavior.
- YIN implementation and thresholds, LivePitch, PitchTrace, chart adapters, session serialization and WAV lifecycle remain unchanged. No Samsung access, dependency/toolchain/version change, second estimator or historical data migration. Details and transient retention limits: ACOUSTIC_UNCERTAINTY.md. Starting recovery reference is `33792ba`; preserve later work before any recovery. UI follow-up is a separate small phase.

## Session chart playback controls - 2026-10-22

- Moved the existing Play/Pause and Restart controls into the chart's LabCard through a content slot. The same SessionDetail cursor feeds both chart and elapsed/total label, now visible before playback (`00:00 / duration`) and while paused. No player, polling loop, seek callback, tab state, filtering or saved data was added or changed. Export/rename/delete remain in their separate Audio card, below the chart. English/Spanish share a positional time-format resource.
- Red: new `SessionNavigationTest#chartTransportPreservesTimeAndPlaybackAcrossTabs` fails on the original UI because `00:00 / 00:20` is absent (ignored `checkpoints/session-player-red-emulator.log`). Green: real MediaPlayer with isolated synthetic 20-second WAVs verifies playing tab changes without restarting, pause, seek to 2.3 seconds, unchanged paused position after switching back, elapsed/total text, controls below the chart and Restart. The first green attempt hit the test's default one-second startup timeout; after allowing five seconds for asynchronous player operations, the same assertions pass in 29.386s (`session-player-new-test-emulator.log`). Production code was unchanged for this retry.
- Two existing cases also pass: `systemBackPopsDetailPreservesScrollAndStopsPlayback` and `explorerRendersRealGraphsSeeksPausesLoopsAndCompares` (`session-player-green-emulator.log`, mixed run containing the startup timeout above). Three distinct cases passed across these runs; this is not a single 3/3 run. Explicit `adb -s emulator-5554`, guarded ranchu hardware, existing isolated test application, no physical device commands.
- One debug-build invocation: `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest --console=plain` -> BUILD SUCCESSFUL in 29s (`session-player-build.log`). JVM: 135 passed, one optional private replay skipped, zero failures/errors. Test APK only was built before red and rebuilt after the timeout correction (14s/3s); no second debug APK build. `python tools/check_localization.py` passes: 480 strings, five plurals and two arrays in each locale.
- Inspected actual emulator screenshots for Fundamental and Spectrogram at the same paused position: compact controls and `00:02 / 00:20` are inside the chart card; the vertical cursor is at 2.3 seconds in both chart coordinate systems. Local synthetic evidence remains ignored at `checkpoints/session-player-fundamental.png` and `session-player-spectrogram.png`. No multilingual/layout matrix, physical audio synchronization assessment, unrelated analysis checks or Samsung access. Spanish resource parity is checked; the new arrangement was visually inspected in English only.
- Starting/recovery reference: `283b343`; preserve later work before recovery. Current delivery's commit/push and asynchronous CI status are reported after publication.

## App version, native splash and authorized Samsung update - 2026-10-22

- Added a discreet version label below the existing recording settings on Measure. PackageManager supplies the installed Gradle versionName and versionCode through PackageInfoCompat: currently `3.2.6-beta1` / `12`. English and Spanish use positional string resources. Versions were deliberately not bumped; rebuilds with identical version values share this label. The installed APK SHA-256 distinguishes this exact artifact.
- Android 12+ uses the framework splash attributes with a static 288 dp vector, artwork within the central 192 dp safe circle, a timber didgeridoo, mint acoustic waves and the existing dark background. The launcher icon, activity/lifecycle, startup timing and dependencies are unchanged. Reference: [Android splash screen specifications](https://developer.android.com/develop/ui/views/launch/splash-screen). Older Android versions receive the matching dark window background.
- One Gradle invocation: `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --console=plain` -> BUILD SUCCESSFUL in 28s; 136 JVM cases discovered, 135 passed, one optional private replay skipped, zero failures/errors. `python tools/check_localization.py` passes with 479 strings, five plurals and two arrays per locale. Local log: ignored `checkpoints/version-splash-build.log`. No release build, broad instrumentation, Detekt/JaCoCo rerun or multilingual matrix was launched for this small UI change.
- Inspected a raster preview rendered directly from the vector paths and the actual English version label on emulator-5554/API 37; native hierarchy confirms `Version 3.2.6-beta1 · build 12`, fully visible in expanded settings. Local evidence: ignored `checkpoints/version-splash-preview.png` and `version-settings-en.png/xml`. A cold-start screenshot missed the transient splash, so Android's final splash mask/transition and the new Spanish label were not visually verified. The preview is artwork evidence, not a captured Android splash. No automated UI test was added for this low-impact presentation change.
- Authorized Samsung SM-G990E update, explicit serial R5CT21KTQ8W: inactive microphone; fresh read-only backup verified 69 files/18 WAVs, no partial recording. Package `ar.didgetaller`, identical signing SHA-256 `fbf54fbe12161bdbf5adbfb9d906961511e9b49a715be8eac8f886e01292b7b2`, compatible SDK/version and unchanged declared permissions checked before `adb -s R5CT21KTQ8W install -r` -> Success. MainActivity launch -> Status: ok; live process and empty crash buffer. Installed version `3.2.6-beta1`/12 and APK SHA-256 `dfde6fb77c6a3c44781542453048a58cbdcf5d4a6db2744db55b7cae9a9fc011` match the new local build exactly.
- Post-update: runtime permission grants/flags unchanged; 68/69 original files byte-identical, all 18 indexed sessions and 18 readable WAVs preserved. The only change is valid Android runtime `shared_prefs/android.app.ActivityThread.IDS.xml`; no new derived files. Private backup/evidence remain ignored under backups/ and checkpoints/version-splash-samsung-update.json. No uninstall, clear, permission reset, restoration, microphone activation or Samsung instrumentation occurred.
- Previous localization delivery `728d13f`: [Actions run #10](https://github.com/hmeclazcke/DidgeTaller/actions/runs/39816031274) is now completed/success, including the job and all 20 steps. This closes the earlier asynchronous CI observation; current UI delivery CI is checked after push and reported separately.

## Localization completion: diagnostics, plurals and bilingual audit - 2026-10-22

- Completed the second stage after `886834f` (`feat(i18n): localize application interface in English and Spanish`). Final inventory: 478 ordinary strings, five plural resources and two 12-entry note arrays in each language; 483 message/plural keys, not 483 call sites. Resource parity, positional formats, supported locales and Unicode checks pass. CI now also runs `python3 tools/check_localization.py`. No custom selector, framework or dependency/toolchain/app-version change; README.md remains unchanged.
- `DiagnosticText.kt` adapts 71 existing string-only diagnostic patterns at display boundaries, including nested capture/analysis errors, export events, microphone permission and model comparison. No protected engine contract was rewritten. Unknown native exception details remain verbatim. In-memory `Session.hasStoredName` distinguishes absent-name labels from identical stored names; no new serialized field, rewrite, rename, UUID, timestamp, association or WAV change. The original JSON writer and recovery/import methods are untouched.
- Red/green evidence: English nested close-error snackbar failed before the adapter (i18n-diagnostic-red.log), then passed. English flute selector clipped at 320 dp/font 1.3 before removing its one-line cap (i18n-layout-red.log); both languages now fit. Changing locale retained an English inspection before cache invalidation (i18n-inspection-red.log); it now clears only that text and retains selection/cameras. Unindexed audio displayed the Spanish fallback before the provenance flag (i18n-generated-name-red.log); the final test verifies live en/es switching and preserves a deliberately stored identical Spanish name. All fixtures are synthetic and test-owned.
- Full production validation: `.\gradlew.bat :app:testDebugUnitTest :app:jacocoDebugUnitTestReport :app:assembleDebug :app:assembleRelease :app:assembleDebugAndroidTest detekt detektComplexity --console=plain` -> BUILD SUCCESSFUL, 1m 53s, 123 tasks (i18n-complete-validation.log). JVM XML: 136 discovered, 135 passed, one optional private replay skipped, zero failures/errors. Debug and unsigned release APKs and release vital lint succeed. Subsequent localization-only test-assertion edits compile successfully with `:app:assembleDebugAndroidTest` (last i18n-all-localized-test-queries-build.log, 4s); production sources/APKs are unchanged after the full build.
- Final emulator evidence comprises **49 distinct passing cases / 55 successful executions**, not a single 55/55 run. Every command used guarded `adb -s emulator-5554`, hardware ranchu, boot complete, API 37. Standard per-app locales were en/es-AR/de-DE; original empty override `[]` restored. Context configuration tests additionally cover in-place language changes, notes, decimals, compiled positional formats/plural boundaries, byte-identical index/extras/PCM and unchanged session association. Native chart semantics and axis bounds are asserted. Exact successful groups follow.

| Log under ignored checkpoints/ | Configuration and classes | Result |
| --- | --- | --- |
| i18n-complete-en-emulator.log | en; LocalizationUiTest (9), UIAuditTest matrix/rename (2), AxisReliabilityUiTest (1), RecordingHandoffTest/CapturePersistenceTest/LocalPersistenceTest (16) | 28/28, 123.257s |
| i18n-complete-es-emulator.log | es-AR; UIAuditTest matrix/rename (2), AxisReliabilityUiTest (1), SessionNavigationTest (15) | 17 passed; one test-helper formatting failure |
| i18n-es-progress-retry.log | es-AR; SessionNavigationTest#progressAndCancelButtonWorkOnLongReadOnlyWav after helper correction | 1/1, 25.479s |
| i18n-en-existing-graphs.log | en; FundamentalPrecisionUiTest (3), GraphIntelligenceUiTest (3), SessionNavigationTest#designAssociationAndManualMarkersSurviveReopening | 6 passed; one remaining Spanish-only legend assertion |
| i18n-en-legend-retry.log | en; FundamentalPrecisionUiTest#reportedScenarioCardDetailAndFullscreen after text assertion correction | 1/1, 22.295s |
| i18n-de-fallback.log | actual de-DE app override; UIAuditTest rename dialog / AxisReliabilityUiTest | 2/2, 53.04s; visible English fallback |

- Reproduction: set the guarded emulator app locale with `adb -s emulator-5554 shell cmd locale set-app-locales ar.didgetaller --user 0 --locales en` (or es-AR/de-DE), install both test/debug APKs using explicit `install -r`, then `adb -s emulator-5554 shell am instrument -w -r -e class <comma-separated fully qualified classes/methods above> ar.didgetaller.test/androidx.test.runner.AndroidJUnitRunner`. Class packages are `ar.didgetaller.ui`, `ar.didgetaller.audio` and `ar.didgetaller.data` as indicated by their names. Restore an empty override with the same locale command omitting `--locales`. No connected-device aggregate task, Samsung operation, uninstall, clear, wipe or reset was used.
- Failed attempts remain distinct from successful evidence. Initial stage-2 locale tests found a duplicate Library-title selector and subpixel text-size rounding; assertions now select the two real title/tab nodes and check glyph/height/ellipsis bounds with one-pixel rounding tolerance. The old inventory tried selecting a viewer plot without opening its current dropdown; the test interaction now uses that menu. Rename cancellation initially compared a stale pre-rename fixture object rather than the actual stored name; corrected to the indexed name. The first Spanish extended run had one fixture assertion and five focus/back timeouts while an actual System UI ANR dialog was present (owned i18n-system.png). Selecting Wait restored emulator focus without changing app code/data; the repeated focus/back cases passed. A zero-argument test helper initially invoked Android's formatting overload on an unfilled progress template; corrected to the raw-resource overload. The last English legend assertion now resolves the full localized legend. No assertion was removed or bypassed to make a product failure pass.
- `python tools/detekt_summary.py` and `python tools/coverage_summary.py` succeed. Detekt: 1,615 informational findings; no baseline/suppression/threshold change. JVM coverage: overall 26.33% lines / 26.52% branches, pure core unchanged 98.37% / 77.15%; 44 production source files and all eight reviewed core method matches retained. Android-only translation/persistence tests do not raise JVM Android capture coverage (still 0% in that report). No new project-wide CRAP score or coverage claim.
- Visual review: standard portrait 1080x2400/density 420/font 1.0, plus measurement at 320 dp/font 1.3, in both languages. Reviewed measurement/settings, Laboratory/design/search/guide/live spectrum, Library/designs/detail, analysis, excerpt controls, comparison, rename and all four viewers. Selected 25 unedited synthetic captures and limitations are in screenshots/localization/INDEX.md. Long English instrument text now wraps. Ordinary scrolling, horizontal tabs and existing ellipsis of user names remain; these are not new clipping defects. Full TalkBack speech, physical-device/landscape layouts, every font/window configuration and every error/dialog state remain unverified. Legacy MainActivity-based test classes were text-updated and compiled, but not run against existing application data in this task.
- Git scope audit: all 20 core files and three audio files, MainActivity and TallerViewModel have unchanged Git content relative to cb153d5. WAV recovery `71e7d33` and handoff `a34583a` remain ancestors with no algorithm change. RecordingStore changes are limited to in-memory name provenance; serialization and recovery are unchanged. Wrapper/Kotlin/AGP/JDK versions, schemas, saved content and README remain unchanged. `git diff --check`, resource validation and actionlint pass. User-configured author/committer rechecked; origin/main fetched with no divergent changes. Push/CI results will be recorded after the authorized delivery; local check success alone is not a completed Actions workflow.

## Localization stage 1: resources and interface — 2026-10-22

- Started on clean main at `cb153d5`, identity/configured author/committer verified unchanged. Reviewed AGENTS.md, ub-mode, product/roadmap/validation/data-safety/viewer context and ignore rules. No baseline commit needed. User authorized complete en/es localization, staged commits and push, excluding README changes and protected engines/data. Consulted official Android localization, Compose resources and app-language documentation; links/architecture in LOCALIZATION.md.
- Read-only inventory found 980 Kotlin literals, with 640 UI occurrences / 551 distinct raw UI literals before classification. Includes routes/tags/format patterns/internal identifiers, not all translatable text. Reviewed templates and persisted names/markers/preset categories separately. Stage 1 creates 410 paired string keys and two 12-element note arrays, English default/Spanish equivalent, manual standard locale config and resource app label. No custom selector or dependency/version changes.
- Red: new isolated `LocalizationUiTest.englishMeasurementAndLibrary` expected Start measurement under an en configuration. First instrument process failed to start amid emulator boot/service ANRs; no assertions or product failure counted. Retry after startup settled executed and failed at the English-label display assertion before production changes (`checkpoints/i18n-red-emulator-retry.log`). Initial install/startup evidence remains in i18n-red-build.log/i18n-red-emulator.log. Every operation explicitly targeted guarded emulator-5554; no Samsung operation, clear/wipe or private data access.
- UI migration uses stringResource/Context.getString, localized enum/marker/preset/note presentation and positional arguments. Core, audio, persistence, ViewModel logic, routes, schema, marker types and generated stored names remain unchanged. Corrected a transient migration encoding issue in new resource labels before validation. Ordinary intermediate compile/link failures were resolved; they are not counted as successful builds. Readable default-name comparison remains tied to the original stored value to avoid treating generated legacy names as custom solely after a locale change.
- An existing JVM axis-label assertion assumed Spanish comma unconditionally. Initial full run: 135 discovered, one failure/one skip. Retained all geometry/collision assertions and added explicit en/es/de decimal/trailing-zero/negative-zero assertions, restoring Locale in finally. Final `.\gradlew.bat :app:testDebugUnitTest :app:jacocoDebugUnitTestReport :app:assembleDebug :app:assembleRelease :app:assembleDebugAndroidTest detekt detektComplexity --console=plain`: **BUILD SUCCESSFUL in 22s**, 123 tasks; fresh JUnit XML **136 discovered / 135 passed / one optional private replay skipped / zero failures/errors**. Log checkpoints/i18n-stage1-validation-final.log.
- Guarded install-r of both debug APKs, then `adb -s emulator-5554 shell am instrument -w -r -e class ar.didgetaller.ui.LocalizationUiTest,ar.didgetaller.audio.RecordingHandoffTest,ar.didgetaller.data.CapturePersistenceTest,ar.didgetaller.data.LocalPersistenceTest ar.didgetaller.test/androidx.test.runner.AndroidJUnitRunner`: **OK (19 tests)** in 32.773s. Three locale tests plus all sixteen existing handoff/persistence cases, no failures/skips. en/es/de configuration contexts verify visible measurement/Library labels, English fallback, C2 vs Do · C2, 73.4 vs 73,4 Hz and decimal-axis labels. No microphone started. Log checkpoints/i18n-stage1-emulator.log.
- `python tools/check_localization.py`, Detekt/coverage summaries succeed. Stage 1 audit: 1,587 informational findings; JVM overall 27.08% lines / 25.09% branches, pure core unchanged 98.37% / 77.15%. Android locale tests are not part of JVM coverage; no Android coverage claim. Debug/unsigned-release/instrumented APKs and release vital lint build. Protected core/audio/data and previous WAV fixes remain byte-identical; README unchanged.
- Stage 1 is a coherent, validated interface/resource migration, not a claim that backend diagnostics, plurals, all long text/layouts or language-change lifecycle testing are complete. Those remain active work in the same localization task. No push/CI result claimed yet for this local stage. Generated scripts, inventory, logs, synthetic files and APKs stay ignored; only reviewed implementation/tests/resources/validator/docs will be committed.

## WAV close-failure handoff fix — 2026-10-22

- Started on clean `main` at `946320d`, synchronized with origin/main. Reviewed AGENTS.md, ub-mode, required context/roadmap, data safety, viewer lifecycle notes, existing recovery characterization and actual engine/ViewModel/store paths. Configured/effective Git identities match the user's existing identity. No empty starting commit, identity/toolchain/dependency/version/workflow changes or internationalization. User explicitly authorized this fix and push.
- Root cause: `MicrophoneEngine.finishRecording` caught writer close failure, posted an error, then unconditionally invoked ready. `TallerViewModel` could import/index that file and replace the error with a saved-session message. Header-write/sync/handle-close failures all reach the same gate; readable bytes alone do not justify close success. A simple removal of ready would leave SAVING unresolved, so a distinct terminal failure callback is required.
- Added four emulator-only `RecordingHandoffTest` regressions before modifying production. Uses a UUID-owned Application/files/preferences namespace, real Android Handler/engine/ViewModel/writer/store and synthetic PCM. Test-only reflection attaches the writer and invokes the private completion method; closing only its owned RandomAccessFile causes a real finalization IOException. No AudioRecord initialization, microphone permission, fake Android objects or production fault-injection API. Debug-name reflection is an explicit test maintenance limit.
- Red build `.\gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest --console=plain` passed (33 seconds). Guarded explicit `adb -s emulator-5554 shell am instrument -w -r -e class ar.didgetaller.audio.RecordingHandoffTest ar.didgetaller.test/androidx.test.runner.AndroidJUnitRunner`: **four tests / three expected failures / one pass**, before any production edits. Both stale and readable-checkpoint files incorrectly received one ready callback (expected zero); the ViewModel test lost its .part through erroneous import. Normal finalization/repeated finish already passed. Logs: ignored `checkpoints/handoff-red-build.log`, `handoff-red-emulator.log`.
- Minimal fix in two production files: ready only after successful close; failed close posts stopped then dedicated main-thread failure. ViewModel returns CAPTURING/SAVING to IDLE without submitting persistence, preserves the diagnostic and shuts its persistence executor after clearance. Writer detachment still occurs once under its existing lock. No deletion, repair, new indexing or retry in the failure path. `git diff 71e7d33 -- app/src/main/java/ar/didgetaller/core/ProgressiveWavWriter.kt app/src/main/java/ar/didgetaller/data/RecordingStore.kt` is empty: the previous recovery fix is preserved byte-for-byte.
- Initial green emulator batch: four new plus twelve existing persistence cases, **OK (16 tests)**. Expanded the existing ViewModel test to cover clearance before failure delivery/executor shutdown and a pre-existing named flute session with timestamp/audio/index preservation. Recompiled only the changed test APK (`checkpoints/handoff-final-test-build.log`, BUILD SUCCESSFUL in 32 seconds), then reran all relevant suites: `adb -s emulator-5554 shell am instrument -w -r -e class ar.didgetaller.audio.RecordingHandoffTest,ar.didgetaller.data.CapturePersistenceTest,ar.didgetaller.data.LocalPersistenceTest ar.didgetaller.test/androidx.test.runner.AndroidJUnitRunner`: **OK (16 tests)** in **39.234 seconds**, zero failures/skips. Log: ignored `checkpoints/handoff-final-emulator.log`.
- Full local validation: `.\gradlew.bat :app:testDebugUnitTest :app:jacocoDebugUnitTestReport :app:assembleDebug :app:assembleRelease :app:assembleDebugAndroidTest detekt detektComplexity --console=plain`: **BUILD SUCCESSFUL in 2m49s**, 123 tasks (48 executed / seven from cache / 68 up-to-date). Fresh JVM XML: **135 discovered / 134 passed / one optional private replay skipped / zero failures/errors**, 19 suites. No private fixture environment was set/read. Debug APK, unsigned release APK, instrumented debug APK and release vital lint all succeed. Existing ArrowForward deprecation remains outside scope. Log: ignored `checkpoints/handoff-validation.log`.
- JaCoCo 0.8.15 debug JVM report and both summary scripts succeed. Overall 27.65% lines / 28.07% branches; writer remains 100% lines / 82.81% branches. New engine/ViewModel tests execute on Android, not under the JVM agent, so no Android coverage increase is claimed. Detekt 1.23.8 succeeds on 42 production files with 1,508 informational warnings and 269 paired functions; no suppressions/baseline or cleanup. Existing eight reviewed pure-method matches remain valid. Reports stay ignored in `app/build/reports/jacoco/debug/` and `build/reports/detekt/`. Existing actionlint and diff whitespace checks pass.
- Device guard: started only the existing Medium_Phone_API_37.0 AVD, no snapshot load/save or wipe; ranchu and boot-complete verified before explicit emulator-5554 install-r/instrumentation. No Samsung commands, private recording/user-session access, unqualified connected-test task or unrelated UI/private-fixture suite. Test cleanup deletes only UUID-owned fixtures; existing persistence suite retains its established isolated context.
- Limits: controlled header-operation IOException is reproduced, not isolated sync-only/handle-close faults, raw partial OS writes, power loss, real microphone interruption or concurrent callback races. Failed captures keep their UUID/bytes and can undergo existing exactly-once pending recovery; pre-existing session metadata/extras remain unchanged. Unsaved in-memory timeline/profile metadata is not journaled and cannot be recreated from WAV by startup recovery. Read/write and persistence-error behavior remain unchanged. Detailed outcome distinctions and risks are in [WAV_CAPTURE_RELIABILITY.md](WAV_CAPTURE_RELIABILITY.md).
- Reviewed all six explicit staged text paths, the complete new test file, full diff, names/stat and whitespace before commit `a34583a`, `fix(audio): prevent persistence handoff after WAV close failure`; pushed normally to origin/main. No tracked WAV/part, generated output or private data. The test-owned emulator was stopped without clearing userdata. Also verified the prior recovery run #8 (`39796325450`, exact `71e7d33` SHA) has now completed/success, updated at 14:42:45 UTC; this supplements, rather than rewrites, its earlier timestamped pending-status record below.
- [Actions run #9, 39801132988](https://github.com/hmeclazcke/DidgeTaller/actions/runs/39801132988), exact SHA `a34583a374280dd2e76562dc1d51bdced0a85bda`: **workflow completed/success**, updated at **15:08:32 UTC**, confirmed again at 15:10:45 UTC. Downloaded the completed workflow's log archive: Temurin 17.0.20, JVM BUILD SUCCESSFUL in 2m16s, debug APK in 1m11s, instrumented-APK compilation in 49s, JaCoCo in 13s and Detekt in 21s. CI compiles but does not run emulator tests; release APK validation above is local. The run-level final conclusion and build logs are verified independently of individual job metadata.
- API consistency limit: the job/list/attempt-job endpoints still returned queued/no steps for job `119901800811` after the workflow completed and published its logs/artifacts. The initial strict step-metadata evidence helper therefore rejected that stale response; this was not a test/build failure. Used the completed run endpoint, exact SHA, real workflow logs and published artifacts for final verification, without claiming those stale per-job fields were successful. Raw metadata/log archive remain ignored under checkpoints/github-ci. No duplicate workflow or CI configuration change was made.
- Downloaded coverage artifact `12436041851`, 212 files; HTML/XML and summary are present. CI summary.json **structurally equals local coverage**, including all 42 source files, component counters and eight reviewed matches, with no unmatched specs. Both sides cover 905/3,273 lines and 1,141/4,065 branches. APK and Detekt artifacts were also published. Evidence is ignored under `checkpoints/github-ci/coverage-39801132988`; no private recordings or secrets published.
- Closure commit changes only validation/work records with `[skip ci]`; application/tests remain exactly the validated `a34583a` tree. Recoverable handoff fix: `a34583a`; pre-handoff state: `946320d`; earlier complete-frame recovery remains `71e7d33`. Preserve subsequent work before recovery. Normal push only, no history rewriting or phone operations.

## Complete-frame WAV recovery fix — 2026-10-22

- Started on clean `main` at `7b1e182`; reviewed AGENTS.md, ub-mode, context, roadmap, data safety, prior characterization, diffs and ignore rules. Configured/effective Git author and committer match the user's existing identity; no settings changed. No checkpoint commit needed. User explicitly authorized this fix and push. Toolchain/dependencies, app version and general historical WAV reader remain unchanged.
- Red before production edits: converted `staleCheckpointImportExposesOnlyHeaderFramesDespitePreservedTail` into desired-behavior regression `staleCheckpointImportRecoversEveryCompleteFrameAndPreservesMetadata`. Built APK/app-test and ran that method via guarded `adb -s emulator-5554 shell am instrument -w -r -e class ar.didgetaller.data.CapturePersistenceTest#staleCheckpointImportRecoversEveryCompleteFrameAndPreservesMetadata ar.didgetaller.test/androidx.test.runner.AndroidJUnitRunner`. **One failure, expected 1500 ms but got 1000 ms**, confirming the actual import-boundary defect. Log: `checkpoints/wav-fix-red-emulator.log`. No production code changed before this failure.
- Added three JVM boundary tests and ran `.\gradlew.bat :app:testDebugUnitTest --tests ar.didgetaller.core.WavFinalizationTest --console=plain` before production edits: **seven tests, two failures** (unsupported immutable header accepted; 45-byte partial incorrectly reported repaired), five passed. Log: `checkpoints/wav-fix-red-jvm.log`. Torn/inconsistent length-field recovery already passed and remains a contract.
- Production change is limited to `ProgressiveWavWriter.repair` validation and `RecordingStore` import/recovery ordering. Validate the fixed PCM16 mono header before mutation, ignore only potentially torn length fields, retain all complete physical frames, reject RIFF overflow and preserve no-frame partials. Repair before duration/rename/index; check destination collision before any repair. No changes to append/close, engine handoff, DSP, PitchTrace, charts, UI, session schema or private WAVs. Details and remaining limits in [WAV_CAPTURE_RELIABILITY.md](WAV_CAPTURE_RELIABILITY.md).
- Focused green JVM run and APK/app-test builds succeeded (`checkpoints/wav-fix-green-focused.log`). Initial emulator batch had 11 passes and one assertion mismatch: the duplicate-import test expected IllegalArgumentException, but the earlier destination check intentionally raises IllegalStateException. Corrected that test expectation only, rebuilt the instrumented APK (`checkpoints/wav-fix-test-rebuild.log`); no production workaround or suppressed test.
- Final full JVM/build/analysis command: `.\gradlew.bat :app:jacocoDebugUnitTestReport :app:assembleDebug :app:assembleDebugAndroidTest detekt detektComplexity --console=plain`: **BUILD SUCCESSFUL**, 32 seconds, 74 tasks (four executed). Fresh JUnit XML: 19 suites, **135 discovered / 134 passed / one skipped / zero failures or errors**. Only the opt-in RealAudioDiagnosticsTest private replay is skipped; DIDGE_TALLER_FIXTURES is unset. Log: `checkpoints/wav-fix-validation.log`.
- Final emulator run: existing Medium_Phone_API_37.0 started without snapshot load/save or wipe; boot complete and ranchu hardware verified. App/test installed only with explicit `adb -s emulator-5554 install -r`. Ran `adb -s emulator-5554 shell am instrument -w -r -e class ar.didgetaller.data.CapturePersistenceTest,ar.didgetaller.data.LocalPersistenceTest ar.didgetaller.test/androidx.test.runner.AndroidJUnitRunner`: **OK (12 tests)**, 7.642 seconds. Four capture tests plus all eight existing persistence tests, no failures/skips; log `checkpoints/wav-fix-final-emulator.log`. PCM/header equality, 1,500 ms / 12,000 frames, metadata/UUID/association/marker preservation, repeated recovery and collision/malformed input preservation all pass. No unqualified connected-test task, physical Samsung operation, private audio replay or unrelated UI/private-fixture instrumentation.
- `python tools/detekt_summary.py` and `python tools/coverage_summary.py` succeed. Detekt: 1,508 informational warnings, 269 paired functions, no parser failure; nine additional MagicNumber findings from explicit WAV header/range validation, no baseline/suppression. JVM writer coverage: **100% lines / 82.81% branches / 90.84% instructions**; overall **27.72% lines / 28.11% branches**. Android audio/data remain 0% in JVM coverage; emulator coverage was not collected. Reports remain ignored at `app/build/reports/jacoco/debug/` and `build/reports/detekt/`.
- All generated logs, synthetic audio and emulator output remain ignored under build/checkpoints or test-owned storage. Only reviewed source/tests and English documentation will be staged. No valid complete PCM is removed for supported partials; one incomplete trailing byte may be discarded. Raw I/O/power-loss failures and multi-gigabyte overflow fixtures were not exercised. Already-imported historical WAVs are not migrated. Separate close-failure handoff remains open.
- Reviewed the full staged diff, explicit seven text paths, names/stat and whitespace checks before commit `71e7d33`, `fix(audio): recover complete PCM before importing captures`; pushed normally to origin/main. Existing actionlint also validates the unchanged workflow. No generated/private audio is tracked. Test-owned emulator stopped after validation without clearing/wiping userdata.
- [Actions run #8, 39796325450](https://github.com/hmeclazcke/DidgeTaller/actions/runs/39796325450) was created by the push for exact SHA `71e7d33534a8d4edb9a8f8c6e678838c762b1f48`. Repeated read-only API checks, including cache-busted run/jobs endpoints, still report **queued** at **14:36:51 UTC**: job `119884297128`, label ubuntu-latest, no runner assigned, no steps started, no conclusion. This is pending external execution, not a successful or failed CI result. No CI artifact/log comparison is possible yet. The workflow runs JVM/build/coverage/Detekt and compiles instrumented tests; the 12 emulator test results above are local only. Raw status metadata remains ignored under checkpoints/github-ci. No duplicate run or workflow change was introduced to bypass the queue.
- Queue-state record committed as `04a4624`, `docs(audio): record WAV recovery validation and CI queue [skip ci]`, and pushed normally. Subsequent API checks show all **19 CI steps completed/success**, including JVM tests, app/instrumented APK compilation, coverage, Detekt, summaries and uploads. At **14:41:21 UTC**, job and workflow still report **in_progress / no conclusion**; direct job, check-run and attempt endpoints agree. No aggregate completed/success is claimed.
- Downloaded coverage artifact `12433048913` from run #8 (212 files). Its summary.json **structurally equals the local report**, including all component/source counters and eight reviewed method matches, with zero unmatched specs. CI/local report: 905/3,265 lines and 1,141/4,059 branches covered. Downloaded HTML/XML/summaries and status evidence remain ignored under `checkpoints/github-ci/coverage-39796325450`. Job-level logs were not collected while the job status remains in_progress. All step outcomes and the artifact are verified; only the final GitHub job/workflow conclusion remains pending.
- Final status record is documentation-only with `[skip ci]`, preserving validated source/tests and the original CI run. Recoverable fix: `71e7d33`; pre-fix state: `7b1e182`. Preserve later work before any recovery. Local validation and push are complete; the pending aggregate status is reported explicitly.

## WAV finalization and persistence characterization — 2026-10-22

- Started on clean `main` at `486aa5e`. Reviewed AGENTS.md, installed ub-mode, product context/roadmap, data-safety and validation/work records, actual writer/engine/ViewModel/store lifecycle and existing tests. Configured/effective Git identities match the user; no configuration changed or empty starting commit. No changes to production Kotlin, build dependencies, toolchain, data formats, DSP/PitchTrace/UI/navigation or private recordings.
- Added WavFinalizationTest with four JVM tests for repeat close/post-close append rejection, initialization input preservation/reuse, low-space append rejection followed by successful retry with exact PCM, and repair/idempotence of a real pre-close disk snapshot with a torn trailing sample. `.\gradlew.bat :app:testDebugUnitTest --tests ar.didgetaller.core.WavFinalizationTest --console=plain`: **BUILD SUCCESSFUL in 11s**, four passed on first run. Characterization against unchanged production; no artificial TDD failure or production fix claimed. Log: ignored checkpoints/wav-focused.log.
- `.\gradlew.bat :app:jacocoDebugUnitTestReport :app:assembleDebug :app:assembleDebugAndroidTest --console=plain`: **BUILD SUCCESSFUL in 2m45s**, 72 tasks (24 executed / 7 from cache / 41 up-to-date). Full JVM suite actually executed: **131 passed, one skipped, zero failures/errors**, 132 discovered / 19 suites. The skipped test is the opt-in private WAV replay; DIDGE_TALLER_FIXTURES was unset. After adding the third Android characterization, `.\gradlew.bat :app:assembleDebugAndroidTest --console=plain` succeeded in 9s (four executed / 44 up-to-date). Logs: ignored checkpoints/wav-full.log and wav-instrumented-build.log.
- ADB was absent from PATH; used the existing SDK binary explicitly. Device listing showed the Samsung and no running emulator. Started the existing Medium_Phone_API_37.0 AVD on port 5554, hidden with audio disabled and no snapshot load/save; no downloads, wipe-data or emulator reset. Every subsequent device command explicitly selected emulator-5554, with ranchu and boot-complete checks before install/instrumentation. Installed both debug APKs with install -r; never addressed or installed on the Samsung, cleared app data or copied private fixtures. The new test uses UUID cache/preference namespaces and cleans only those fixtures; existing LocalPersistenceTest uses its established isolated namespace.
- Emulator Android API 37: CapturePersistenceTest (three new cases) plus **all eight existing LocalPersistenceTest cases**, **OK (11 tests)** in 18.226s. Real Android storage/preferences, synthetic PCM, no mocks or reflection. Cases verify collision preservation/repeated recovery, rejected incomplete import followed by repair/single import, and a confirmed stale-checkpoint import defect. Full unrelated UI/private-fixture instrumentation was intentionally not run; no full-UI-suite claim. Logs/guarded runner: ignored checkpoints/wav-persistence.log and run_wav_persistence.py. No Android instrumented coverage was collected. After validation, shut down only the emulator started for this task using guarded adb -s emulator-5554 emu kill; no userdata wipe or phone operation.
- Reproduced defect: a copied pre-close writer state at 8kHz has 12,000 complete frames / 24,044 bytes but a checkpoint header declaring 8,000. importCapture succeeds, returned/reloaded duration is 1,000ms instead of the available 1,500ms, reader exposes 8,000 frames, all file bytes remain unchanged, and recoverPending returns zero after rename to .wav. The test explicitly characterizes a known limitation, not the desired future fix. Source inspection confirms the engine hands off a file even after close failure, but actual write/sync/close I/O failure and physical capture loss were not induced. Reproduction, separate minimal-fix proposal and remaining callback/concurrent-stop gaps are in [WAV_CAPTURE_RELIABILITY.md](WAV_CAPTURE_RELIABILITY.md).
- `python tools/coverage_summary.py`: successful real-report summary. ProgressiveWavWriter.kt **100% line / 82.50% branch / 90.66% instruction**, versus 96.55% / 65% / 83.83% before. Overall JVM **27.62% line / 27.82% branch**. Android capture and data-package coverage remain **0% in this JVM report**; emulator execution does not justify a numerical coverage claim. Historical coverage audit retained intact.
- Existing actionlint and git diff --check pass. Diff against `486aa5e` confirms production sources, Gradle dependencies/toolchain and analysis configuration/helpers unchanged. Workflow only adds compilation of the instrumented test APK; no CI emulator/device operations. Generated audio, reports, logs, emulator state and private inputs stay outside Git.
- Test commit `5935337`, `test(audio): characterize WAV finalization and persistence`, pushed normally to origin/main after reviewing all six explicitly staged text files and full diffs. [GitHub Actions run #7, 39792488131](https://github.com/hmeclazcke/DidgeTaller/actions/runs/39792488131), exact SHA `593533727a62b06c41aab5a4bc62dbd74352110b`: job test-and-apk **completed/success**, with every step successful. Downloaded job logs confirm Temurin 17.0.20, JVM tests BUILD SUCCESSFUL in 3m9s, main APK in 1m38s, instrumented APK compilation in 1m, JaCoCo report in 18s and Detekt in 27s. CI compiles Android tests but does not execute them; the 11-case instrumented result belongs exclusively to the local guarded emulator run. Final API check at 2026-10-22 14:17:46 UTC: the sole job/check-run completed successfully at 14:11:07 UTC, while the workflow aggregate and check-suite still report in_progress with null conclusion. This delayed aggregate status is recorded honestly; overall workflow success is not claimed.
- Downloaded CI coverage artifact `12433175232`, 485,493 bytes / 212 files, archive digest `sha256:252c49af770700e1406748ce0983d18530b9bd78dcce40c7d7436597a3290fdd`. Its complete summary structurally matches the local report, including updated writer coverage, 900/3,258 covered lines, 1,121/4,029 covered branches, 12,653/67,156 instructions and existing eight reviewed method matches. No Android coverage was merged. Main APK and Detekt artifacts also uploaded. Evidence remains ignored in checkpoints/github-ci/coverage-39792488131 and the local wav-*.log files.
- Closure changes only these validation/work records with `[skip ci]`, preserving the tested source/tests/workflow. Recoverable test integration: `5935337`; pre-test state: `486aa5e`. Preserve subsequent work before recovery. The reproduced stale-header defect remains intentionally unfixed, with the separate proposal documented above.

## Initial JaCoCo JVM coverage audit — 2026-10-22

- Started on clean `main` at `1b6a539`, with the user's configured/effective Git author and committer matching. Reviewed current repository/ub-mode instructions, product context, roadmap, validation/work records, .gitignore and Gradle/CI files. No starting checkpoint commit required. Kotlin/Compose plugin 2.2.21, AGP 8.13.2, Gradle 8.13, local JBR 21.0.11 and JVM bytecode target 17 remain unchanged; CI retains Temurin 17.
- Consulted official JaCoCo release notes/counter documentation, Gradle plugin documentation and the original CRAP proposal. Installed stable JaCoCo 0.8.15 through Gradle's built-in plugin, limited agent instrumentation to project classes and registered `:app:jacocoDebugUnitTestReport`. No Android build-type/offline coverage instrumentation, app dependency or coverage verification threshold. Report input is production debug bytecode only, with generated Android/WhenMappings exclusions and JaCoCo's built-in Kotlin/Compose filters. Kept lambda/ComposableSingletons containers because they can contain handwritten code; residual compiler effects and Android-dependent zeros are explicit limitations.
- `.\gradlew.bat :app:jacocoDebugUnitTestReport :app:assembleDebug --console=plain`: **BUILD SUCCESSFUL in 34s**, 44 tasks, two executed and 42 up-to-date. The existing JVM test task actually executed with the agent, and the report task executed; app compilation/APK were valid and up-to-date. Fresh XML: 18 JUnit suites, 128 discovered tests, **127 passed, 1 skipped, 0 failures/errors**. Skipped RealAudioDiagnosticsTest is opt-in and DIDGE_TALLER_FIXTURES was unset. Existing tests create temporary synthetic WAVs; no personal recordings were accessed. Full log: ignored `checkpoints/jacoco-initial.log`. HTML/XML reports and nonempty execution data generated at documented build paths, without mismatch/instrumentation errors.
- Overall JVM coverage: **899/3,258 lines (27.59%), 12,623/67,156 instructions (18.80%), 1,114/4,029 branches (27.65%), 315/1,456 methods (21.63%)**. Detailed per-component/source-file measurements and five risk-prioritized gaps are in [COVERAGE_AUDIT.md](COVERAGE_AUDIT.md). Detector and trace files reach 100% lines but only 83.33% and 86.82% branches. Android audio and data packages are unexecuted here; no historical instrumented results were merged. XML lists 42 actual source files plus four empty compiler SMAP references (Comparisons.kt in three packages and ui/Effects.kt), without counters. The summarizer distinguishes these metadata-only entries, rejects non-project sources with nonzero counters, and cross-checks the 42-source-file sums against report totals. HTML footer identifies JaCoCo 0.8.15.202606040825.
- `.\gradlew.bat detekt detektComplexity --console=plain`: BUILD SUCCESSFUL, existing unchanged-source tasks up-to-date. `python tools/detekt_summary.py` and `python tools/coverage_summary.py` succeed on actual reports. No tests added or changed. `javap -p`/XML/source inspection verified eight explicit pure-function correspondences (source hash, declaration, owner, name, descriptor and bytecode line). Rejected generic matching for split local/lambda/Compose functions. Calculated explicitly labeled CRAP-L/CRAP-I approximations for those eight using method line/instruction counters; neither is canonical basis-path CRAP, no aggregate/threshold claimed. Correspondences are reviewed fixtures under config/jacoco, not inferred by fuzzy joins. A stale/ambiguous future match omits that score with a reason rather than blocking coverage/CI.
- Existing actionlint validates the workflow; `git diff --check` passes. Source/wrapper/settings/root Gradle/properties comparison against `1b6a539` is unchanged. App build changes only add JVM tooling; no DSP/UI/chart/navigation/data-version/source changes, new tests, CPD or mutation tooling. No device operations or physical acoustic validation. CI keeps existing tests/APK/Detekt checks and adds coverage reports, summary and artifact, without a ratio gate.
- Integration commit `8d5d576`, `chore(quality): add informational JVM coverage audit`, pushed normally to origin/main after reviewing all seven explicitly staged text files and full diffs. [GitHub Actions run #6, 39785451002](https://github.com/hmeclazcke/DidgeTaller/actions/runs/39785451002), exact SHA `8d5d57626775821c08aff36c2ff10bd333934db0`: **completed/success**. Every job step succeeded. Downloaded job logs confirm Temurin 17.0.20; JVM tests BUILD SUCCESSFUL in 2m56s, APK in 1m54s, JaCoCo report in 20s and Detekt in 28s. The workflow/check-suite API briefly remained in_progress after the job completed; waited for the aggregate completed/success result before closing the record. Existing SDK manager/jvmTarget/ArrowForward and Ubuntu runner notices remain outside scope.
- Downloaded coverage artifact `12429341302`, `DidgeTaller-JVM-Coverage`, 485,498 bytes / 212 files: HTML report tree, XML and summaries. **CI summary.json structurally equals the local summary**, including every component/source counter, all eight reviewed correspondences and both CRAP approximations; no unmatched specs. Parsed the downloaded XML with the same summarizer successfully. Archive digest `sha256:36f5eadb5c17f2c24f3303adc8f35e5887af08b8d1b7fc8c036476079ec96993` identifies the artifact archive, not the XML alone. APK and Detekt artifacts were also published. Raw metadata, job logs and downloaded reports remain ignored under checkpoints/github-ci/coverage-39785451002; no secrets or private recordings published.
- Closure commit updates only these validation/work records with `[skip ci]`; tooling, workflow and application remain identical to the successful run. Recoverable coverage integration: `8d5d576`; pre-integration tooling: `1b6a539`. Preserve subsequent work before recovery. No history rewriting, force push or device operations.

## Initial Detekt integration and quality audit — 2026-10-22

- Started on clean `main` at `3dc3ddb`, synchronized with origin/main. Reviewed AGENTS.md, ub-mode, PRODUCT_CONTEXT, ROADMAP, existing validation/work records, .gitignore and current CI before editing. Configured and effective Git author/committer matched the user's existing identity; no identity settings changed. No starting checkpoint commit was needed.
- Toolchain inspection: app Kotlin/Compose plugin 2.2.21, AGP 8.13.2, Gradle distribution 8.13, JBR 21.0.11 locally, JVM target 17; existing Actions uses Temurin 17. Consulted the official Detekt compatibility table, Gradle/runtime documentation, type-resolution and complexity-rule documentation. Selected stable plugin/engine 1.23.8 with root-level syntax-only analysis and its own compiler 2.0.21. No application classpath/type resolution or compiler override. Versions in app/build.gradle.kts, wrapper, settings and gradle.properties are unchanged. The compatibility rationale and analysis limits are in [DETEKT_AUDIT.md](DETEKT_AUDIT.md).
- `.\gradlew.bat detekt detektComplexity --console=plain`: successful initial execution, 42 production Kotlin files. Final configuration rerun with `.\gradlew.bat detekt detektComplexity dependencyInsight --dependency kotlin-compiler-embeddable --configuration detekt --console=plain`: **BUILD SUCCESSFUL**, three tasks executed, 52 seconds. Dependency insight confirms isolated kotlin-compiler-embeddable 2.0.21. Full logs are ignored in `checkpoints/detekt-initial.log` and `checkpoints/detekt-final.log`. Disabled the default Markdown output after observing that both tasks initially shared its path; HTML/XML/SARIF/TXT outputs have separate destinations. Final runs have no reported parser/configuration failures.
- Main audit: **1,499 findings, all warning in SARIF**, Checkstyle count independently equal; 19 rules. 817 MagicNumber and 438 MaxLineLength findings dominate volume. Additional metric task: 538 observations / 269 paired named-function measurements, excluded from the finding total. `python tools/detekt_summary.py` succeeds, rejects malformed/unpaired metrics by construction, and writes summary.md/summary.json under `build/reports/detekt/`. Its actual report schema, metric pairing and XML/SARIF count agreement were verified on these outputs; no unrelated application tests were invented for configuration work.
- `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --console=plain`: **BUILD SUCCESSFUL**, 43 tasks executed, 2m48s. Fresh JUnit XML: 18 suites, 128 discovered tests, **127 passed, 1 skipped, 0 failures, 0 errors**. The skipped test is the explicitly opt-in private WAV replay, RealAudioDiagnosticsTest.replayPrivateWavsThroughProductionStreamAndLiveDecision; DIDGE_TALLER_FIXTURES was not set. No private WAV files were read or replayed. Debug APK built at the existing path/version. Existing jvmTarget/ArrowForward deprecations and native-library stripping warning remain outside scope.
- Existing actionlint 1.7.12: `.github/workflows/android.yml` valid. `git diff --check` passes. Comparison against `3dc3ddb` shows no changes under app/, gradle/, gradle.properties or settings.gradle.kts. The 42 tracked production Kotlin files match the analysis count. No baseline or source suppressions added; no formatting, refactoring, CPD, coverage or mutation tooling. No device operations, UI changes or physical/acoustic validation. Generated reports remain ignored under build/; only reviewed source/configuration/documentation paths will be committed.
- CI preserves the two existing app checks and adds explicit informational analysis, standard-library Python summary generation, and a DidgeTaller-Detekt artifact. Existing findings are non-blocking through Detekt ignoreFailures, while execution/configuration/summary errors remain visible failures.
- Integration commit `5e30e56`, `chore(quality): integrate informational Detekt audit`, pushed normally to origin/main after reviewing all eight explicitly staged text files and the full staged diff. [GitHub Actions run #5, 39782333774](https://github.com/hmeclazcke/DidgeTaller/actions/runs/39782333774), exact SHA `5e30e566219b5260801f233f0c2692027cae2972`: **completed/success**, every step successful. Logs confirm Temurin 17.0.20, JVM checks BUILD SUCCESSFUL in 2m56s, APK BUILD SUCCESSFUL in 1m54s, and both Detekt tasks executed with BUILD SUCCESSFUL in 29s. Summary generation and both artifact uploads succeeded. Existing SDK manager/jvmTarget/ArrowForward deprecations remain non-blocking; no execution or parser failure was observed.
- Detekt artifact `12427229015`, `DidgeTaller-Detekt`, 389,163 bytes, ten files (four formats for each task plus two summaries), downloaded and inspected. **CI summary.json is structurally identical to the local summary**, including all 1,499 warnings by rule and all 269 paired function metrics; CI SARIF contains 1,499 audit findings and 538 separate metric observations. Artifact archive digest: `sha256:a0322fe1c3ecf250fe0d4e84bfdd208b751ab6f7bdb1a373325ab0a6f4a77404` (not a digest of an individual report). APK artifact `12427039335` also published. Read-only API initially returned HTTP 401 while following the artifact redirect; removing Authorization on cross-host redirects allowed download, without changing the workflow or requesting new permissions. Raw CI logs/API responses/downloaded reports stay ignored under checkpoints/github-ci.
- Closure updates only validation/work records and uses `[skip ci]`; the application, tooling and workflow remain identical to the successful run's commit. Recoverable integration: `5e30e56`; pre-audit tooling: `3dc3ddb`. Preserve subsequent work before any recovery. No force push, history rewriting, private material publication or device operation.

## Repository-local ub-mode skill installation — 2026-10-22

- Scope: agent configuration only, starting from clean `main` at `dc61960`. Created `.agents/skills/ub-mode/SKILL.md` with YAML `name: ub-mode` and a description covering implementation, debugging, bug fixes, refactoring, and code review. Reviewed the full requested Work/Right/Practice methodology, Kotlin tool compatibility, audio/visualization safeguards, Android data protection, and Git/CI authorization requirements. The skill is instruction-only and has no required tool dependencies.
- Added a short English link and explicit `$ub-mode` invocation to AGENTS.md, with existing safety/testing/Git requirements taking precedence. Added the user's prospective English policy for new instructions, technical documentation, and commit messages; preserved all previous project text and historical commits.
- The bundled skill-creator `quick_validate.py` could not run because PyYAML is not installed. No dependency was installed. Instead, `codex-cli 0.163.0` generated its local protocol schema and a focused probe used only `initialize` and `skills/list` with `forceReload: true`: the native Codex loader parsed the YAML, returned the expected name/description/path, `scope: repo`, `enabled: true`, and no ub-mode loading errors from both the repository root and `app/src/main`. No model turn was started. Probe/schema/evidence remain ignored under checkpoints; this verifies actual discovery, not guaranteed implicit selection on every prompt.
- Manual review checks the complete methodology and unavailable-tool fallback: Detekt, coverage, CPD, and mutation tools are optional future choices only. No Detekt, CPD, coverage, mutation-testing, Java-oriented utilities, Gradle plugins, or dependencies were installed or executed. Configuration checks and diff review replace unrelated application tests for this instruction-only change. `git diff --check` and comparisons against `dc61960` verify no changes to Kotlin, Gradle, Android resources/configuration, DSP, wrapper, or GitHub Actions. No Samsung operations or private data changes.
- Installation commit `bd7d42d`, `chore(agent): install repository-local ub-mode skill`, was pushed normally to origin/main after identity and staged-content review: exactly the four intended text files, with no private/sensitive material detected. GitHub remains private; local and remote main matched before this final record.
- [GitHub Actions run #4,39779474236](https://github.com/hmeclazcke/DidgeTaller/actions/runs/39779474236), for full SHA `bd7d42d785bb7a8a923c3c544966b9fc74f6e162`, completed successfully: Tests JVM, Compilar APK, artifact upload, and every other job step succeeded. No workflow change was required. This is CI build/test evidence, not physical audio or device validation.
- The closure commit updates only these validation/work records and uses `[skip ci]` to avoid another identical build. Skill, AGENTS.md, application, and workflow remain identical to the successful installation commit. Recovery point for the installed skill: `bd7d42d`; preserve later work before recovery.

To verify discovery in a new Codex CLI session:

```powershell
Set-Location 'C:\projects\DidgeTaller'
codex
```

Inside Codex, run `/skills` and find `ub-mode`, or type `$` and select it. Invoke it explicitly with a prompt such as `$ub-mode Review PitchTrace without changing files.` Codex can select it implicitly when the task matches its name/description; discovery does not guarantee activation for every prompt. If it is missing from the list, restart Codex and check for skill-loading errors. See the [official OpenAI skill documentation](https://learn.chatgpt.com/docs/build-skills) for repository discovery and explicit/implicit invocation.

## Corrección de CI Android — 22/10/2026

- Base limpia `ab5ca26`, rama `main` sincronizada. Runs GitHub #1 (`39766933483`) y #2 (`39767278472`) fallaron antes de Gradle: `setup-android@v3` ejecutó `sdkmanager tools`, paquete inexistente; tests y APK quedaron omitidos. Logs y anotaciones de #2 confirman también Node20 obsoleto y deprecación de setup-java v4.
- Workflow: `android-actions/setup-android@v4` con `packages: 'platform-tools'`; `actions/checkout@v7`, `actions/setup-java@v6` y `actions/upload-artifact@v7`. Releases oficiales estables comprobadas por API:4.0.4/7.0.1/6.1.0/7.0.2 respectivamente; los cuatro action.yml declaran Node24. Se conserva Temurin17, SDK35/build-tools35.0.0 y ambos comandos originales `./gradlew --no-daemon :app:testDebugUnitTest` / `./gradlew --no-daemon :app:assembleDebug`. `chmod +x ./gradlew` solo en el runner: el índice local confirma modo100644, insuficiente para ejecutar directamente en Ubuntu.
- Validación previa al push: actionlint1.7.12 oficial, archivo descargado en checkpoints con SHA-256 contrastado contra su manifiesto (`6e7241b51e6817ea6a047693d8e6fed13b31819c9a0dd6c5a726e1592d22f6e9`); `actionlint .github/workflows/android.yml` sin diagnósticos. `git diff --check` aprobado y diff contra `ab5ca26` sin cambios en app, configuración Gradle ni wrapper. Identidad Git configurada y efectiva del usuario comprobada.
- Commit de CI `fd0a558`, `fix(ci): replace obsolete Android tools and Node 20 actions`, subido normalmente a origin/main. [Run #3,39776045256](https://github.com/hmeclazcke/DidgeTaller/actions/runs/39776045256), SHA `fd0a5581fd9f7360975bcea4af1293a16a101666`: **completed/success**. Preparación de Android y SDK, chmod, Tests JVM, Compilar APK y publicación del artefacto: todos success. Logs confirman `:app:testDebugUnitTest` ejecutado y BUILD SUCCESSFUL en2m46s; `:app:assembleDebug` y BUILD SUCCESSFUL en1m39s. No hubo un segundo error de CI.
- Artefacto `DidgeTaller-Kotlin-APK`, ID12424756817,10.724.604 bytes de archivo publicado, no vencido; digest del artefacto `sha256:56ca9e7ee68dc4b2dddbd063d799042dfbaefacc9b116370d11ec523a72c5b3e` (no atribuirlo al APK descomprimido). Logs confirman `sdkmanager platform-tools`, sin solicitud a tools. Sin advertencias Node20/setup-java v4; subsisten deprecaciones informativas del CLI sdkmanager y las conocidas jvmTarget/ArrowForward, más el aviso de migración de ubuntu-latest, sin fallos. No se alteran versiones del proyecto para eliminar esas advertencias.
- Comprobación adicional compara ambos comandos Gradle exactos contra la base y verifica JDK17/paquete explícito: aprobada. El primer patrón auxiliar incluyó también chmod; corregido para comparar solo invocaciones de Gradle, sin cambios al workflow. La lectura local de logs encontró un BOM incompatible con la salida cp1252 y se completó desde el ZIP guardado; no fue un error de Actions. Scripts, metadatos y logs bajo checkpoints, ignorados por Git. No se aportaron audios privados al runner ni se ejecutaron pruebas de dispositivos/Samsung.
- Cierre exclusivamente documental con `[skip ci]`: registra el run exitoso sin repetir una compilación de workflow/app idénticos a `fd0a558`. No cambia los disparadores habituales push/pull_request/workflow_dispatch. Fuente y workflow recuperables: `fd0a558`; preservar trabajo posterior antes de recuperar.

## Vinculación con GitHub privado — 22/10/2026

Alcance: publicar el historial existente en `https://github.com/hmeclazcke/DidgeTaller.git`, por pedido explícito del usuario. Código y versión de la app conservados.

- `git status --short --branch`, `git log --all --format=fuller`, `git branch -vv`, `git remote -v` y revisión de `.gitignore`: árbol inicial limpio, rama `main`, 12 commits desde `4f90893` hasta `1fbfb127adc1319f8e136c08d6d8c68fa2d4ea09`, sin remotos ni seguimiento. No correspondía un commit de punto de partida.
- Auditoría de cada árbol mediante `git rev-list --all` / `git ls-tree -rz` y de cada contenido único mediante `git cat-file blob`: 412 rutas históricas, 464 blobs (200 de texto y264 binarios), 34.205.062 bytes. Incluye archivos eliminados o reemplazados, sin depender de las exclusiones actuales. Búsqueda de rutas privadas/generadas, firmas de audio, claves privadas, tokens conocidos, JWT, credenciales en URL y asignaciones de secretos: sin material sensible detectado. Las236 coincidencias de asignaciones son exclusivamente `password="false"` en jerarquías Android y su explicación documental; revisadas como falsos positivos.
- Binarios:263 PNG únicos (capturas documentales de UI e ícono) revisados en seis hojas de contacto; sin credenciales visibles detectadas ni metadatos textuales PNG. Único comprimido: `gradle/wrapper/gradle-wrapper.jar`,43.583 bytes, SHA-256 `2db75c40782f5e8ba1fc278a5574bab070adccb2d21ca5a6e5ed840888448046`; entradas de clases/licencia del wrapper, sin audio ni respaldo. No se encontraron WAV, PCM, APK, almacenes de claves ni respaldos privados en los árboles históricos. Metadatos de los12 commits revisados.
- `python checkpoints/audit_github_history.py github`: credencial obtenida en memoria mediante Git Credential Manager, sin imprimir ni guardar tokens. API autenticada como `hmeclazcke`, con permiso `push`; repositorio correcto con `private=true` y `visibility=private`. No fue necesario iniciar sesión. GitHub informó tamaño cero y respondió409 a la consulta inicial de referencias de un repositorio vacío; `git ls-remote origin` confirmó que no existían referencias.
- `git remote add origin https://github.com/hmeclazcke/DidgeTaller.git`: origen agregado sin reinicialización ni cambios al historial. Lectura de credenciales, red y escritura en `.git/config` requirieron ejecución autorizada fuera del sandbox; reintentos aprobados correctos. Los scripts, informe y hojas de contacto permanecen en `checkpoints/`, ignorados por Git. Generación visual corregida para usar System.Drawing local ante ausencia de Pillow y evitar la limitación de ConvertFrom-Json con una clave vacía; sin instalar dependencias.
- `git push --set-upstream origin main`: correcto, nueva rama remota y seguimiento `origin/main` configurado. La transferencia demoró varios minutos sin salida intermedia y finalmente terminó con código0; no se canceló ni forzó. `git ls-remote --heads origin main`, `git rev-parse HEAD` y `git rev-parse origin/main` coinciden exactamente en `1fbfb127adc1319f8e136c08d6d8c68fa2d4ea09`. `git rev-list --left-right --count main...origin/main`:0/0; `git status --short --branch`: `main...origin/main`, árbol limpio antes del registro documental.
- `git diff --exit-code 1fbfb12 -- app`: sin cambios de aplicación. `git fsck --full`: código0, con un blob no referenciado informado; no se borró ni se envió ese objeto como parte de una referencia. Identidad configurada y efectiva del usuario comprobada antes del commit documental, sin modificar configuración global ni autores existentes.

Límites: revisión de rutas, formatos, patrones y evidencia visual; no demuestra ausencia matemática de secretos arbitrariamente codificados. No se ejecutaron compilación ni pruebas Android porque no se modifica la app. Sin operaciones sobre dispositivos, borrados, reescritura de commits ni force push. Recuperación del código de esta entrega: `4330e71` (3.2.6-beta1/12), también contenido en `1fbfb12` y el cierre documental posterior; preservar cualquier trabajo posterior antes de recuperar.

## Cierre 3.2.6-beta1/12 — 21/10/2026

Código recuperable `4330e71`, base `309135e`. APK11142161bytes, SHA-256 `50be9eeb17e90ce0054fa26256628037995964f9243a0761fdd771f670609a72`; `assembleDebug`, `assembleDebugAndroidTest` y128 JVM aprobados. **44 casos UI distintos aprobados exclusivamente en emulador**, más8 repeticiones de matriz/datos;21 [capturas nativas revisadas](screenshots/v326-final/README.md). Anchos reales360/≈420dp y fuentes1,3/1,5, con resolución y densidad controladas. Mismo APK en todas las comprobaciones finales.

Regresión inicial:40/42 UI pasaron; fallaron expectativas literales de «0,00 Hz» y de la leyenda anterior. Actualizadas al formato compacto, ambos casos pasaron por separado; más2 de rotación completan44 distintos. No contar las dos ejecuciones fallidas como aprobadas ni afirmar una corrida única42/42. La primera suite JVM detectó que un test exigía enlazar el borde débil de un toot: ahora exige el corte intencional del borde, conserva todos sus puntos, su conexión interna y su escala;128/128 posteriores aprobados. Logs y agregación por caso en `checkpoints/v326-final-ui.json`, `v326-release-regression.log`, `v326-release-focused.log` y `v326-final-build.log`.

Contraste adicional en solo lectura con el Samsung: **las652 lecturas guardadas coinciden exactamente en Hz/confianza con el replay** (256/121/275); hashes de sus WAV idénticos a los fixtures. El replay añade dos ventanas finales sin Hz que no llegaron al índice al cerrar la captura. Se conservan los tiempos originales de entrega; no se reemplazan por posiciones PCM. La menor lectura guardada de19:17 es35,247Hz. En las series guardadas, dibujados en tendencia159/46/127, inciertos4/2/2, enlaces145/40/115→145/38/115 y mismas escalas. Sin aumento de aceptaciones ni modificación de valores; la exactitud física de los candidatos ambiguos sigue abierta.

En19:26, tramos aproximados inferidos por orden/envolvente: drone2,6–5,5s, primera «I»7,2–12,8s, drone16–21,2s, «A»24–30s, drone33–38s y segunda «I»41–47,2s. Primera «I»:29/30 ventanas admitidas, RMS mediano0,188, periodicidad0,914; segunda:7/33, RMS0,120, periodicidad0,577. «A»:3/32 con RMS0,153. Es evidencia de señal presente y mayor ambigüedad para YIN, sin evaluar la calidad interpretativa ni fingir límites fonéticos exactos. Reporte privado `checkpoints/v326-segments.json`.

Samsung `R5CT21KTQ8W`: respaldo fresco `backups/samsung-before-3.2.6-20261021-201724`, firma/paquete/permisos y compatibilidad comprobados; **install-r/Success**, arranque `Status: ok`, APK instalado con el mismo SHA. **69/69 archivos con hashes intactos,18 sesiones y18 WAV indexados legibles**, sin diferencias runtime ni archivos derivados nuevos en la comprobación; buffer crash del proceso vacío. Informe `checkpoints/v326-samsung-update.json`. Nunca se instrumentó, limpió, desinstaló ni restauró el teléfono.

No se declara resuelta la «I» física. YIN/rangos/umbrales conservados al no demostrar una mejora fiable; los huecos acústicos y algunos candidatos discordantes siguen existiendo. Flauta y toot sintético con rango ampliado aprobados; el rango inicial Didgeridoo35–140Hz mantiene su limitación para toots superiores. Sin prueba física nueva de afinador, sincronía audible,15min/batería ni TalkBack completo.

## Diagnóstico y presentación 3.2.6 — etapas A–D

Etapa D: ejes medidos con el Paint real y margen4+4dp tanto en Fundamental como en STFT; STFT deja de reservar56dp fijos. Números sin ceros decimales redundantes; notas compactas conservadas. Se elimina el Hz separado, identificado por la leyenda de detalle o el rango del visor. Los márgenes siguen congelados durante gestos. En el mismo fixture1080px/densidad420dpi/fuente1,0, STFT pasa de147 a69px de margen: datos817,5→895,5px (+9,54%). Fundamental ya medía sus etiquetas:75px antes/después, sin fingir una ganancia horizontal. No se tocaron cámaras/insets/gestos.

El paso redondeado de50s podía dejar solo0s en47,9s; ahora hay al menos dos marcas candidatas, con supresión de etiquetas que colisionan según el texto medido. AxisLayout2 JVM aprobadas; pruebas preliminares AxisReliability UI en densidades420dpi/1,0,360dpi/1,3 y420dpi/1,5. Esas densidades no son anchos dp; la matriz final ajusta también la resolución para anchos360/420dp. Comparación real con APK3.2.5 extraído en lectura del Samsung y capturas antes/después del renderer. Recovery UI de colores/heading aprobado: «Explorar el audio» se ve completo al desplazarlo a la vista; el recorte previo corresponde al scroll de sesión, sin defecto de insets demostrado. Se conserva navegación y scroll.

Se conserva el DSP tras comparar cinco alternativas sobre los tres WAV: desplazar el soporte hacia el final (12288/8192 muestras), ampliar a32768, pasar bajos400Hz y extender el rango inferior a25Hz. Informes privados en `checkpoints/v326-experiments*`. Las ventanas completas comparables son256/121/275; las finales parciales siguen en el reporte principal.

| Variante | Admitidos 19:04 / 19:17 / 19:26 | Observación |
|---|---|---|
| Actual | 163 / 48 / 129 | Referencia, sin cambios |
| Soporte tardío12288 | 162 / 48 / 127 | Gana10/5/8 y pierde11/5/10 lecturas |
| Soporte tardío8192 | 163 / 49 / 127 | Gana17/8/13 y pierde17/7/15 |
| Ventana32768 | 162 / 49 / 135 | No comparable en las dos primeras ventanas; prolonga9/4/10 lecturas sobre ventanas originalmente de RMS bajo |
| Pasabajos400 | 165 / 62 / 146 | Añade candidatos77–124Hz y períodos largos ambiguos; no demuestra recuperación correcta de F0 |
| Límite25Hz | 163 / 63 / 172 | Añade períodos31–34Hz, no corrige automáticamente a66Hz |

En 19:17, el replay nominal **no entrega33Hz**: entrega35–36,6Hz en3,07–7,89s y pierde muchas lecturas energéticas en9–16s. Al permitir25Hz solo en el experimento aparecen33,2–34,1Hz en13,65–15,70s, con periodicidad de hasta0,968. El espectro contiene familias competidoras: cerca de67/134/201Hz y181Hz en el primer tramo; cerca de66/132/196Hz y307Hz durante el tramo posterior. La banda grave no basta para declarar F0=66Hz; componentes fuera de su serie armónica favorecen períodos largos o ambigüedad. No se puede atribuir únicamente al balance armónico ni afirmar un error de octava demostrado. No hay etiquetas exactas de cada vocal. En19:26,7–12s conserva muchas lecturas alrededor68Hz,24–30s es ambiguo y40–47s conserva pocas; son tramos compatibles con el orden aportado, no una segmentación fonética validada.

Corrección de presentación: un cambio Didgeridoo≥600c con periodicidad mínima de sus extremos<0,90 separa los grupos aunque ambos tengan apoyo local. No elimina muestras, no duplica Hz, no altera la escala ni se aplica a flauta. Las otras reglas3.2.5 siguen iguales. En19:17 se eliminan exactamente dos conexiones engañosas (3,065s y8,081s); muestras admitidas/incertidumbres permanecen48/2. Otros dos WAV: mismos puntos y conexiones. Colores sin cambios. Medir distingue «Señal presente · frecuencia incierta» de «sin señal» y mantieneHz vacío; nunca retiene la última nota como actual.

Pruebas focalizadas de presentación aprobadas: RegisterTransition2, PitchRecovery10, FundamentalRepair7 y replay privado1; compile/assembleDebug/assembleDebugAndroidTest correctos. RecoveryUiTest4 casos aprobados solo en emulador: tres series reales, históricos, colores y callback real de Medir con perfiles/silencio/incertidumbre. Capturas privadas inspeccionadas: curva19:17 sin barridos verticales y estado incierto. Los timelines reales usan WAV silencioso de soporte en emulador; no son reproducción acústica ni prueba física.

Base limpia `309135e`, identidad Git configurada comprobada; fuente y hashes preservados en `checkpoints/before-3.2.6`. Los tres WAV externos coinciden con su README y se abren solo en lectura. Reportes privados CSV: `app/build/reports/real-audio/baseline`; no se versionan ni empaquetan audios.

Reproducción con `Pcm16WavReader → PitchStream → PitchDetector → LivePitch → PitchTrace`. `LivePitch` extrae literalmente la aceptación y suavizado existentes del ViewModel, sin modificar parámetros. Observador opcional registra candidato, periodicidad, mínimos CMNDF, soporte temporal del candidato, rechazo, Hz UI/guardados y presentación. La autocorrelación normalizada sobre la ventana completa es una comparación independiente, no una etiqueta correcta. El CSV identifica perfil y cuándo intervino el suavizado exclusivo de pantalla.

| WAV (hora) | Ventanas | Hz admitidos | RMS <0,005 | Con energía, sin Hz |
|---|---:|---:|---:|---:|
| 19:04 | 256 | 163 | 62 | 31 |
| 19:17 | 122 | 48 | 24 | 50 |
| 19:26 | 276 | 129 | 62 | 85 |

Las pérdidas energéticas se producen antes de la UI: **161 candidatos con periodicidad<0,65 y5 ventanas sin mínimo YIN utilizable** (1/1/3 por archivo). Las148 de RMS bajo no se etiquetan automáticamente como silencio real. No hay descartes adicionales en LivePitch ni pérdidas atribuibles a la UI en este replay. En 19:17, 3,07–7,89 s hay estimaciones de35–36,6Hz; varios mínimos largos también aparecen en autocorrelación, mientras60–69Hz compiten con menor periodicidad. A14,025s, el experimento permite33,725Hz/0,968, frente al mínimo66,416Hz/0,308; no es evidencia para duplicar automáticamente el valor. No está demostrado que sean simplemente octavas falsas. Los límites de vocales no están etiquetados con exactitud.

Validación: compilación y 9 pruebas focalizadas aprobadas (AcousticReliability, LivePitch, PitchDiagnostic), más la prueba opcional sobre los tres WAV. Comparación contra las clases preservadas de `309135e`: igualdad exacta de Hz/confianza/RMS en 652 ventanas completas. Los reportes del stream incluyen dos ventanas finales parciales adicionales. `DIDGE_TALLER_FIXTURES` habilita la prueba privada; sin esa variable se omite. `DIDGE_DIAGNOSTIC_LABEL` separa reportes antes/después. No atribuir a esta instrumentación una corrección de vocalización.

Reproducción local: configurar `DIDGE_TALLER_FIXTURES` con la carpeta privada y ejecutar `gradlew.bat :app:testDebugUnitTest --tests "*RealAudioDiagnosticsTest" --rerun --no-build-cache`. `--rerun` vuelve a leer los archivos aunque solo cambie el audio o la etiqueta del reporte; la disponibilidad del archivo privado no se impone a otros desarrolladores. No publicar los CSV ni agregar audios a recursos de pruebas.

# Registro de validación

## Recovery & Acoustic Reliability 3.2.5 — 21/10/2026

APK **3.2.5-beta1/11**,11.543.314 bytes, SHA-256 `0a9bbfb7a780900432b3872e693031d38b3af924743ea0dc68bfc112ea1a149a`. JBR21.0.11/bytecode17/SDK35; paquete/firma/dependencias/permisos conservados. La recuperación de presentación está verificada; **la pérdida física durante «I» sigue abierta**.

### Recuperación y causas comprobadas

Git inicial limpio en `56c2e81`, con3 commits reales, no solamente el histórico `4f90893`. Copia adicional previa:73 archivos de fuente/configuración, ZIP211.071 bytes y hashes comprobados en `checkpoints/before-3.2.5/`. ZIP3.2 contiene53 Kotlin y configuración;3.2.2-final contiene59 archivos de fuente y su manifiesto verifica101 archivos; before3.2.4 contiene61 de fuente; after3.2.4 contiene63 y verifica142 hashes. after3.2.3 contiene solo9 archivos de fuente: es parcial, no un respaldo completo. No se restauraron directorios ni se volvió al commit histórico.

Comparación real de fuente3.2/3.2.1/3.2.2/3.2.3/3.2.4: YIN no cambió desde3.2, ni el rango/ventana/cadencia didgeridoo, gate RMS0,005 o aceptación DSP0,65. El cambio anterior3.1→3.2 agregó el camino de flauta a tasa completa y ajustes exclusivos de ese registro, conservando el camino grave/4. ViewModel3.2.2 y3.2.4 idénticos: didgeridoo muestra Hz>0 del detector; flauta exige rango y0,80. Hz/confianza originales se guardan, no la frecuencia suavizada del medidor. `PitchTrace` no interviene en el callback del micrófono. Conversión musical, persistencia, reanálisis y cachés no cambiaron en3.2.5.

La regresión de color sí es demostrable:3.2/3.2.2 calculaban color por cents/confianza;3.2.3/3.2.4 reemplazaron ese cálculo por verde constante en ambos renderers. La fragmentación combina ausencias originales, el umbral visual0,85 de3.2.3, y reglas acumuladas de bordes/modo dominante. Se recuperó selectivamente la semántica de color y se reemplazaron esas decisiones de presentación, sin copiar módulos históricos enteros.

### Conducta de presentación

Admisión de estimaciones desde0,65, igual que DSP; valores inválidos/fuera de perfil/bajo umbral no generan una tendencia. Resumen histórico sigue0,70. Períodos breves bajos:≤4 puntos, retorno medido por ambos lados, separación≤400ms, total≤1s y relación1,5/2/3/4±80c; no se usa un modo global. Retornos únicos débiles exigen extremos coherentes≤80c, vecinos≤400ms y retorno≤600ms; múltiplos2/3/4 con periodicidad<0,92, o excursión didgeridoo≥100c con periodicidad<0,90. Saltos didgeridoo≥600c frente a dos vecinos coherentes, periodicidad<0,90 y sin apoyo propio en±4 lecturas/1s quedan inciertos accesibles. No son errores físicos confirmados.

Además, un salto≥300c con extremo débil<0,90 sin ese apoyo queda como punto sin barrido; no se elimina. Ataques moderados, cambios respaldados y flauta siguen conectados. Tras≤4 omisiones, extremos dentro de80c y≤1s pueden enlazarse con trazo discontinuo; no se crean frecuencias intermedias. Ausencia clara, tiempos invertidos y pausas largas cortan. General usa puntos corroborados temporalmente o≥0,90 si representan al menos la mitad y son≥3; de lo contrario usa toda la tendencia. Extiende a todos los eventos respaldados, incluidos toots breves; originales y rango completo permanecen disponibles.

Color compartido por tarjeta/detalle/fullscreen: verde≤25c y periodicidad≥0,90; amarillo hasta60c, periodicidad0,80–0,90 o falta de vecino; naranja>60c o periodicidad<0,80. Se toma la periodicidad más débil del enlace. El cursor expone periodicidad/variación y los originales con razón. Son medidas diferentes, no probabilidad calibrada ni confirmación acústica. No se mezclan con la paleta STFT.

### Comparación de sesiones originales

Fuente: respaldo Samsung en solo lectura. Métricas ejecutadas contra clases compiladas del código real3.2.2,3.2.4 y3.2.5, no reconstrucciones desde README. La comparación visual usa esos APK y los mismos timelines con WAV silencioso en almacenamiento aislado del emulador; no reanaliza ni sustituye originales del teléfono.

| Serie | Frecuencias guardadas | Principales/enlaces3.2.2 |3.2.4|3.2.5|General3.2.5|
|---|---:|---:|---:|---:|---|
|Re·D2,17,32s|49/91|43/35|40/33|47/38|60,61–85,71Hz|
|Mib·Eb2,7,72s|30/40|26/23|20/19|25/23|44,61–90,15Hz|
|Reciente,51,5s|125/275|121/104|117/102|122/103|58,69–83,00Hz|

No se optimizó solo el conteo de enlaces. Retención de frecuencias3.2.5:95,9%/83,3%/97,6%. D2 pasa de7 a9 componentes porque se recuperan originales y un ataque aislado queda sin barrido; tramo mayor14 puntos/2.399ms frente a13/2.217ms. Eb2 conserva el inicio respaldado alrededor de50Hz en naranja, en lugar de borrarlo por modo global: tramo mayor24 puntos/5.384ms frente a20/4.267ms. Reciente:19 componentes frente a15, con originales aislados recuperados; los tramos principales de34/33 puntos y6.131/5.938ms se conservan, sin reconstruir150 ausencias. Colores verde/amarillo/naranja: D2=11/7/29, Eb2=0/14/11, reciente=73/27/22. La ausencia de verde en Eb2 refleja su periodicidad guardada, no un fallo del renderer.

El eje reciente hasta≈50s es correcto: WAV51.500ms y viewport temporal0–51.500. Las regiones vacías no se eliminan con zoom ni se rellenan para hacer parecer mejor la medición.

### Vocal «I»: resultado y límite real

El usuario ubicó la vocal en los últimos15s; se inspeccionó36,5–51,5s del WAV respaldado. Con YIN actual,81 ventanas:18 estimadas(mediana64,53Hz),25 con RMS<0,005 y38 energéticas no aceptadas. En40–48,5s solo2/46 se aceptan. Autocorrelación normalizada independiente sobre PCM detecta un transitorio fuerte alrededor de37,15s rechazado por YIN; entre las38 ventanas energéticas rechazadas solo1 supera0,80 de autocorrelación máxima en35–220Hz(mediana0,462). Los tiempos de captura son callbacks, no ventanas PCM alineadas exactamente.

Hay pérdida de amplitud/periodicidad en la señal guardada y también ambigüedad del estimador; no puede atribuirse todo a silencio, al instrumento o al teléfono. Un experimento aislado de ventana más reciente aumentó18→20/81 estimaciones, pero podía admitir≈97Hz ante un período fuerte≈65Hz: se rechazó, YIN del APK sigue intacto. Las seis pruebas PCM nuevas sí mantienen70Hz ante cambios de balance armónico/amplitud y reanudación; no prueban una vocalización física. Hace falta una prueba dirigida con referencia independiente para resolver este punto, no otra bajada ciega de umbrales.

### Validación ejecutada

`gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest --console=plain`: **BUILD SUCCESSFUL**, **120 JVM**, cero fallos/errores/omitidos.16 casos nuevos:6 de streaming PCM y10 de presentación. `PitchStream` es la ventana/cadencia usada por AudioRecord, no un pipeline paralelo solo para tests; parámetros congelados por captura y callback condicionado a su AudioRecord original. Toot sintético205Hz probado con rango ampliado220; el inicial140 no promete ese registro. Flauta200–1600Hz, armónicos dominantes/melodía/silencios, persistencia y regreso al perfil didgeridoo aprobados. No se identifica automáticamente señal fuera de rango desde ausencia de estimación.

**42 casos instrumentados distintos del APK final**:40 regresión y2 recreación/rotación real de MainActivity, exclusivamente `emulator-5554`. Incluyen reproducción/seek/selección/bucle, Vista amplia con captura, Atrás, cámaras/zoom/pan/Reset, swipe/Deshacer, datos/asociaciones y flauta. Los tests nuevos verifican colores en píxeles del Canvas, nota/Hz visibles en Medir y originales/duración. Medir usa PCM controlado en el callback real y estado de captura aislado sin escritor/AudioRecord: sus imágenes no son una grabación física. Logs y hash del APK: `checkpoints/v325-release-*.log/.json`.

Fallos investigados y corregidos: expectativas de descarte indiscriminado, guarda que omitía retorno del toot, import de test, acceso al estado delegado y fixture1s insuficiente para cinco lecturas. Una variante de escala y el experimento DSP se descartaron. Revisión visual adicional retiró el barrido del ataque débil y protegió los ataques moderados con regresiones específicas. No se cuentan ejecuciones fallidas/interrumpidas como aprobadas. El mensaje del commit17a8420 dice14 en el foco inicial; el conteo correcto fue12(6+6), aclarado sin reescribirlo.

Matriz final:6 repeticiones aprobadas(3 casos en360dp/fuente1,3 y3 en420dp/fuente1,5), excluidas del conteo42 distintos. Todos los informes `v325-release-regression/rotation/360/420.json` corresponden al mismo SHA del APK final. Comparaciones históricas adicionales:1 caso contra3.2.4 y1 contra3.2.2, excluidos del conteo del APK final. La primera regresión118 JVM/42 UI se repitió después de la revisión visual del ataque aislado; el cierre usa120 JVM y el APK posterior. [22 capturas nativas revisadas](screenshots/v325-final/README.md), sin composiciones ni imágenes generadas. No se volvió a atribuir a Samsung una medida física del área del espectrograma tomada en otra entrega.

### Samsung e integridad

Samsung SM-G990E `R5CT21KTQ8W`: micrófono inactivo y respaldo fresco `backups/samsung-before-3.2.5-20261021-184836`,64 archivos/15 WAV, hashes contrastados antes de instalar. Paquete `ar.didgetaller`, versión10→11 y certificado común `fbf54fbe12161bdbf5adbfb9d906961511e9b49a715be8eac8f886e01292b7b2` verificados. `adb -s R5CT21KTQ8W install -r …` → **Success**; `am start -W` → **Status: ok**, proceso23680 y buffer de crash sin fatal. APK instalado igual al compilado.

Revisión a18:49:09: **64/64 hashes intactos**, sin archivos nuevos ni diferencias runtime; índice/ajustes/relaciones conservados, **15 sesiones y15 WAV indexados/legibles**. Informe `checkpoints/v325-samsung-update.json`; respaldo vigente en `backups/latest.txt`. Sin instrumentación, clear, uninstall, restauración, grabación ni reproducción de sus sesiones. El usuario aclaró que el borrado histórico fue autorizado; esta iteración no lo atribuye a un accidente.

Se preservan33 de los40 Kotlin main anteriores byte a byte; siete modificados y uno nuevo(`PitchStream`). YIN, WavAnalysis, datos, reproducción, STFT, cámara/gestos, navegación y simulación intactos; no migración ni cambio de versiones de caché. Código recuperable `42924ec`, base anterior `56c2e81`; cierre fuente+manifiesto en `checkpoints/after-3.2.5`, indicado por `checkpoints/latest.txt`. Git usa la identidad configurada; sin push ni reescritura histórica.

Pendiente: vocalización física con referencia independiente, gesto físico, sincronía audible,15min/batería y TalkBack completo. Instalar/arrancar no valida esos puntos. La nueva versión no promete una línea continua durante los huecos del WAV reciente.

## Adopción de Git y punto de partida — 21/10/2026

Solicitud: conservar el estado acumulado y establecer commits verificables por fase. Sin cambios al código de la app ni a su versión `3.2.4-beta1/10`.

- Identidad configurada y efectiva de autor/committer comprobada antes del primer commit: Hernán A. Meclazcke, mediante `git config --get user.name`, `git config --get user.email` y `git var GIT_AUTHOR_IDENT` / `GIT_COMMITTER_IDENT`. No se modificó la configuración global ni se sustituyó el autor.
- Estado inicial: un único commit `4f90893` (`Primer commit`), 28 archivos modificados y 115 nuevos del proyecto. Revisión de rutas, diffs, archivos nuevos, índices de capturas y jerarquías XML de UI; sin audio, credenciales ni archivos generados en la selección. Los atributos `password="false"` de las jerarquías Android no son contraseñas guardadas. Las capturas conservadas son evidencia de UI del proyecto; sus resultados históricos no se vuelven a contar como pruebas actuales.
- Entorno comprobado: Windows, JBR **21.0.11**, Platform 35 y Build Tools 35.0.0 disponibles; bytecode objetivo 17. `gradlew.bat :app:testDebugUnitTest --console=plain`: **BUILD SUCCESSFUL in 51s**, 104 pruebas en 11 suites, cero fallos, errores u omitidos. `gradlew.bat :app:assembleDebug --console=plain`: **BUILD SUCCESSFUL in 4s**. Los XML permanecen en `app/build/test-results/testDebugUnitTest/`, excluidos de Git.
- Commit de partida **`3f0510f`**, `chore(project): checkpoint existing 3.2.4-beta1 state`: 143 archivos preparados por rutas revisadas; `git diff --cached --check` aprobado, sin rutas privadas/generadas. Preserva los cambios acumulados, sin reconstruir commits funcionales retrospectivos ni modificar `4f90893`.
- Segunda fase: reglas permanentes en `AGENTS.md` y exclusiones adicionales de audio privado, credenciales, fixtures locales y cachés Kotlin/nativas en `.gitignore`. La compilación creó transitoriamente `.kotlin/sessions/*.salive`: la selección de rutas lo detectó y excluyó antes de preparar archivos. No se borró ese caché ni se incluyó en el commit de partida.
- Verificación de exclusiones con `git check-ignore`: 34 rutas representativas privadas/generadas correctamente ignoradas y seis controles de fuente, tests, documentación, capturas y wrapper no ignorados. También se comprobó `.WAV` en mayúsculas en este repositorio Windows (`core.ignorecase=true`). Sin crear archivos de prueba. `git diff --check` aprobado; ninguna ruta privada/generada rastreada según la revisión de nombres. `git diff --exit-code 3f0510f -- app` confirma que la segunda fase no modifica la app; no requiere repetir las pruebas JVM ni la compilación.
- Alcance: esta iteración verifica JVM y compilación; no ejecuta instrumentación, instalaciones ni operaciones en Samsung. Las advertencias existentes de `jvmTarget` y `ArrowForward` obsoletos no impidieron las tareas. Validación física/acústica pendiente según los registros siguientes.

Recuperación del código verificado: `3f0510f` (`3.2.4-beta1/10`); el commit posterior de política Git conserva el mismo código e incorpora las reglas permanentes. Consultar `git log --oneline` para identificar el cierre. Los respaldos y audios privados siguen fuera del repositorio.

## Reparación selectiva de Fundamental 3.2.4 — 21/10/2026

APK **3.2.4-beta1/10**,11277304 bytes, SHA-256 `6fe6bcdcfb976acd5124e1f91b43fabddefb41a534677fd6f6ec8c8611c1c1a6`. Mismo paquete/firma; JDK17.0.20, bytecode17/SDK35, sin dependencias/permisos nuevos. Checkpoint previo `checkpoints/before-3.2.4`; cierre `checkpoints/latest.txt`.

### Causa y reversión parcial

La regresión3.2.3 se reproduce con las dos series reales reportadas. El umbral visual global0,85 descartaba lecturas útiles de periodicidad moderada y cada incertidumbre borraba el enlace anterior. No era decimación: F0 no se reduce en tarjetas. Comparación contra fuente/APK3.2.2 y3.2.3, preservados; no rollback global. Se recupera aceptación0,70 y se reemplaza la segmentación dañina en `PitchTrace` común a tarjeta/detalle/fullscreen. Datos originales intactos.

La curva muestra tendencia, no certificación de nota tocada. Ante hasta4 omisiones y≤1000ms, enlaza **dos extremos medidos** si coinciden dentro de80cents; no inserta muestras ni suaviza. Una frecuencia nula solo admite ese enlace si su confianza guardada está en[0,50;0,70); null con confianza0/alta/desconocida, valores inválidos y pausas largas cortan. Sin omisiones, separación máxima1200ms. El dato incierto conserva Hz/tiempo/confianza/razón y acceso en herramientas/cursor.

Solo en didgeridoo: una excursión única≥100cents entre vecinos aceptados a≤400ms, coherentes≤80cents, se omite de la curva principal. Se conserva criterio previo de múltiplos2/3/4. Para breves períodos alternativos bajos: modo dominante por semitono, banda±160cents con≥6 lecturas y≥55% de aceptadas; candidato<mediana/1,4, confianza[0,65;0,90), razón1,5/2/3/4±80cents; grupo≤4 lecturas/≤800ms con separación≤400ms. Bordes junto a ausencia: confianza<0,90, dos vecinos aceptados coherentes, diferencia≥100cents y caída de confianza≥0,04 frente al más fuerte. Son heurísticas de presentación, no correcciones del detector ni prueba de error. Cambios respaldados por varias lecturas y flauta conservados; un evento real de una única lectura puede quedar entre originales.

General conserva algoritmo previo, incluyendo todos los puntos principales y contexto mínimo±3semitonos: no nuevo recorte por percentiles. Inciertos aislados no inflan rango. Ajustada, Mostrar todos/rango exacto, Reset, límites de zoom y cámaras independientes intactos. «N fuera» ya no aparece en título/lectura/leyenda principal; sigue consultable en herramientas.

### Casos reales y comparación visual

Fixtures privados extraídos **en solo lectura del respaldo existente**: Re·D2,20oct17:06,17320ms,72,077Hz,91 lecturas; Mib·Eb2,20oct21:20,7720ms,75,880Hz,40 lecturas. Se conservaron exactamente serie/fechas/duración/resumen, con IDs nuevos y WAV silenciosos de igual duración **solo en almacenamiento de prueba del emulador**. No se copiaron audios reales al repositorio ni se reanalizaron/sobrescribieron WAV del Samsung. Es validación del renderizado de sus lecturas guardadas, no reproducción acústica real.

Mismo emulador API37,1080×2400,densidad411≈420dp,fuente1,0; ejecuciones con APK3.2.2/8,3.2.3/9 y3.2.4/10. Los APK históricos se instalaron exclusivamente en emulador. Conteos de puntos principales/enlaces:

| Sesión |3.2.2 previa|3.2.3 regresiva|3.2.4 reparada|General reparada|
|---|---:|---:|---:|---|
|Re·D2|43/35|17/15|40/33|60,58–85,68Hz|
|Mib·Eb2|26/23|12/5|20/19|63,77–90,18Hz|

Re·D2 recupera la tendencia y conserva huecos reales; no se afirma línea ininterrumpida durante toda la grabación. Mib·Eb2 recupera un tramo principal continuo cerca de75Hz: antes su General llegaba a44,56Hz y quedaban trazos mínimos;3.2.2 incluía períodos bajos hasta≈35Hz. Inicio sin tendencia respaldada permanece vacío. Originales no borrados. Métricas comparativas `checkpoints/v324-real-comparison.json`; fixture privado ignorado `v324-real-fixtures.json`.

Eje medido con Paint: números sin sufijo repetido, unidad Hz única y notas secundarias; margen4+4dp y congelación durante gesto conservados. Detalle916px: reserva115,55→74,55px(−35,48%), ancho de datos769,625→810,625px(+5,33%). Leyenda «Línea: tendencia» y ayuda en herramientas; se quita el párrafo permanente de gestos bajo Fundamental. Otros gráficos conservan su presentación.

### Validación y límites

Gate `gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest`: **BUILD SUCCESSFUL,104 JVM**, cero fallos/errores/omitidos; logs `v324-focus-build.log`/`v324-final-build.log`. Siete casos nuevos: drones moderados, outliers bajos/altos, puentes limitados, silencio real, toot respaldado, flauta, períodos alternativos breves frente a sostenidos, ruido prolongado y1000 puntos sin decimación. Expectativas antiguas de segmentación0,85 actualizadas porque expresaban la conducta rechazada. Primera ejecución detectó además un borde que descartaba el retorno legítimo del toot: corregido exigiendo degradación real de confianza; no se cuenta esa ejecución fallida como aprobada.

**39 casos instrumentados distintos**, exclusivamente `emulator-5554`:1 comparación real nueva(`v324-after`),36 regresión y2 recreación/rotación real de MainActivity. Además6 repeticiones:3 casos en360dp/fuente1,3 y3 en420dp/fuente1,5. Todos sin fallos/omitidos, mismo SHA del APK final en cada `v324-*-run.json`. Regresión incluye Back/Vista amplia con captura, cámaras independientes, zoom/pan/ancla/reset, originales, cero sin nota, reproducción/selección, flauta/perfiles, WAV/índice/relaciones/persistencia. No se cuenta dos veces un caso repetido por configuración.

Comparaciones baseline:1 caso contra3.2.3 y1 contra3.2.2, aprobados separadamente, excluidos del conteo39 del APK final. Arranque prematuro del baseline antes de terminar de compilar el APK de prueba falló por clase no disponible; repetición limpia aprobada, fallo conservado aparte. El curador auxiliar rechazó inicialmente el texto singular «1 test»; corregido el parser, sin repetir suites aprobadas ni modificar app/APK. Solo cuatro archivos main cambian frente al checkpoint3.2.3 (`PitchTrace`, `PlotSurface`, `SessionExplorer`, `ImmersiveViewer`),39 main idénticos; informe `v324-source-evidence.json`. Sin cambios DSP/PCM/STFT/YIN/reproducción/captura/persistencia/cachés/cámaras/gestos/navegación/permisos/dependencias.

[20 capturas revisadas](screenshots/v324-final/README.md): comparación de ambas tarjetas/detalles en tres versiones, fullscreen reparado, fuentes grandes, leyenda, toot/flauta, Reset,0Hz y espectrograma horizontal. Matriz sin superposición de ejes ni mutilación de las miniaturas. Re·D2 conserva pausas y algunos cambios respaldados: no se maquillaron como tono perfectamente plano. STFT conserva contenedor compacto; no se vuelve a atribuir una medida física71,6% a esta entrega sin medirla allí.

### Samsung: actualización no destructiva

Serial **R5CT21KTQ8W**, SM-G990E. Micrófono inactivo verificado antes de respaldar y justo antes de instalar. Respaldo fresco `backups/samsung-before-3.2.4-20261021-152023`: **61 archivos/13 WAV**, hashes contrastados antes de actualizar. Certificado del APK nuevo e instalado previo coincide: `fbf54fbe12161bdbf5adbfb9d906961511e9b49a715be8eac8f886e01292b7b2`. Paquete `ar.didgetaller`, código9→10.

`adb -s R5CT21KTQ8W install -r …` → **Success**. `am start -W` → **Status: ok**, proceso18346, sin fatal en su buffer de crash. Bytes del APK instalado iguales al compilado. Verificación a15:20:53: **61/61 hashes intactos**, sin nuevos archivos derivados/runtime; índice completo idéntico,13 sesiones y13 WAV indexados/legibles. Incluye ajustes/asociaciones/mediciones; no migración, reanálisis masivo ni escritura de datos originales. Informe/script/log privados `checkpoints/v324-samsung-update.json`, `update_samsung324.py`, `v324-samsung-update.log`. No instrumentación, clear, uninstall, restauración, captura ni reproducción de audio real. Captura actual del teléfono negra: no permite inspección visual física; la revisión de gráficos reales se realizó con sus lecturas en emulador.

Límites: confianza no calibrada, clasificación contextual no infalible; eventos brevísimos pueden quedar inciertos y siguen inspeccionables. Amplitud por lectura no almacenada; tiempos históricos corresponden a callback, no ventana PCM exacta. No se atribuye cada punto omitido a error físico confirmado. Gestos con dedos físicos, afinador independiente, sincronía audible,15min/batería y TalkBack completo pendientes.

## Fundamental Precision 3.2.3 — 21/10/2026

APK **3.2.3-beta1 / 9**, mismo `ar.didgetaller`, 11275867 bytes, SHA-256 `319f1223326e60ea34f0c357a39639c1725b2304f1260627933aa1df05ba5787`. JDK21.0.11/bytecode17/SDK35; dependencias/permisos intactos. Línea base86 JVM+APK aprobada antes de modificar. Gate `gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest`: **BUILD SUCCESSFUL**, **97 JVM**, cero fallos/errores/omitidos (`checkpoints/v323-final-build.log`). Once casos nuevos; expectativa anterior actualizada para incluir eventos confiables raros en General.

### Causas y política común

Los valores problemáticos ya están en el índice. No existe decimación de F0 en las tarjetas: tarjeta y visor consumen `PitchTrace` íntegro y sus enlaces `previous`. 3.2.2 aceptaba confianza≥0,70 y solo reconocía retornos a períodos múltiples; conectaba estimaciones débiles no armónicas y bordes junto a ausencias. Pintarlas naranja no evitaba dibujar las caídas.

Ahora la curva exige confianza YIN≥0,85 y validez del perfil, sin promover lecturas por repetirse. Se mantiene el criterio de periodicidad aislada de3.2.2. Retornos no armónicos: una sola lectura con confianza<0,92, dos vecinos por lado a≤400ms entre muestras, vecinos≥0,90 y dispersión≤80cents, separación central≥100cents y caída de confianza≥0,04 respecto de todos ellos. Bordes: lectura<0,92 junto a ausencia/contexto sin apoyo, dos vecinos del otro lado a≤400ms entre muestras, ambos≥0,85 y alguno≥0,90, coherentes≤80cents, separación≥100cents y caída≥0,04 frente al más confiable. Son reglas de incertidumbre de presentación; confianza no es probabilidad calibrada. No confirman error ni nota tocada.

La incertidumbre corta antes/después. Normal usa verde; originales agrega círculos pequeños sin unirlos. Cursor: Hz/confianza/razón sin nota confirmada. Ausencias no generan0Hz. No se interpolan ni suavizan mediciones. Transiciones respaldadas permanecen confiables, sin duplicar modelos. Amplitud y curva CMNDF no se persistían por lectura: no se inventaron metadatos.

General usa percentiles2,5–97,5% desde40 confiables y margen previo, conserva contexto mínimo±3semitonos y extiende a **todos** los confiables con margen `max(0,6Hz;8% de amplitud total)`. Un toot de dos lecturas no desaparece por peso estadístico. Ajustada conserva percentiles/precisión fina; zoom admite0,2Hz. Mostrar todos incluye inciertas; rango exacto habilita dominio amplio. Reset vuelve a tiempo completo+General y desactiva dominio extendido. Sin auto-fit durante gesto/pan. Aviso discreto de lecturas fuera del viewport actual, incluidas inciertas; originales y ayuda en herramientas.

### Diagnóstico real, solo lectura

Respaldo diagnóstico fresco: **61 archivos/13 WAV**, hashes contrastados sin escribir al Samsung. Informes privados ignorados `v323-real-context.json`, `v323-real-isolated.json`, `v323-real-pitch-diagnostic.json`; script `diagnose323.py`. Seis eventos inspeccionados; ejemplos:

| Tiempo / Hz / confianza guardados | Contexto original | PCM cercano: RMS / periodicidad |
|---|---|---|
|4,880s /66,846 /0,787|71,661→66,846→ausencia|0,07951 /0,560: energía suficiente, periodicidad débil|
|6,376s /77,733 /0,797|72,360→77,733→ausencia|0,01872 /0,490; mínimo≈45,37Hz: ambiguo|
|8,042s /35,235 /0,785|ausencia→35,235→65,557|0,10347 /0,713; mínimo≈66,02Hz: no confirma caída real|
|9,717s /65,419 /0,905|71,973→65,419→ausencia|0,00285, menor que umbral RMS0,005: no aceptar mínimo como detección|

Los tiempos capturados son de entrega de callback. Ventanas16384 cercanas, promedio/4 y CMNDF local no reconstruyen exactamente la ventana original. El script informa lag entero sin interpolación ni aceptación RMS. No atribuir cada evento al detector o instrumento. Ambigüedades43/130Hz documentadas debajo. JVM reproduce también ataque91,115Hz/conf0,861 y final65,419Hz/conf0,905 como inciertos junto a ausencias.

### Eje e interfaz

Mismo fixture, emulador1080×2400, densidad411 (≈420dp), fuente1,0; `PlotDataArea` gobierna dibujo/gesto/inspección. Antes: etiqueta ficticia `000,0000 Hz` y mínimo56dp. Después: cada etiqueta real Hz/nota medida con el mismo Paint del Canvas, nota85%, margen exterior4dp+separación4dp. Ancho retenido durante gesto, recalculado al soltar. Se elimina Hz superior redundante. Otros gráficos conservan márgenes.

| Superficie / ancho total | Reserva anterior→actual | Datos anterior→actual | Ganancia de datos |
|---|---:|---:|---:|
|Detalle /916px|189,825→115,550px|695,350→769,625px|74,275px /10,68%|
|Fullscreen /1080px|189,825→115,550px|859,350→933,625px|74,275px /8,64%|

Reserva−39,13%; detalle recupera8,11 puntos porcentuales del ancho total. Medidas de este escenario, no constantes para todas las frecuencias/fuentes. Leyenda «Verde: confiable · huecos: sin lectura segura»; aviso «N fuera · ver Escalas y rangos»; explicación técnica en panel existente.

### Pruebas y cierre

- JVM nuevos: drone69–72 con siete tipos de pico; retorno no armónico; toot corto/sostenido incluso2/202 lecturas; salida real de YIN sobre tonos sintéticos205/207Hz con RMS/confianza; melodía/glissando; incertidumbre prolongada; silencios/tiempos discontinuos; ejemplos reales; serie larga de tarjeta; Ajustada/Reset/vacíos/extremos/perfiles y notas/ticks.
- Nuevos UI **3/3** (`v323-fundamental-instrumentation.log`): tarjeta/detalle/fullscreen, Ajustada/pinch/Reset/originales/cursor, toot breve y flauta. Primer recorrido falló por buscar un instante fuera del zoom temporal: corregido con Reset antes de inspeccionar. Dos expectativas iniciales JVM erróneas: índice tras ausencia omitida y nombre corto frente a bilingüe. Ejecuciones fallidas preservadas y excluidas del conteo.
- Regresión final **33/33** (`v323-regression-instrumentation.log`) + rotación real **2/2** (`v323-rotation-instrumentation.log`): **38 casos instrumentados distintos** contando los3 nuevos. Matriz360dp/fuente1,3 y420dp/fuente1,5: **4/4 cada una**, repeticiones de los3 nuevos y visor horizontal; no sumarlas como casos nuevos. Cada JSON `v323-*-run.json` registra el SHA del mismo APK final. Toda instrumentación dirigida explícitamente a `emulator-5554`, con guardas de hardware.
- Una corrida agrupando las35 regresiones quedó esperando durante ViewerRotation después de las otras clases; se interrumpió solo en emulador y se descartó completa. Se ejecutaron limpias33+2 por separado, como en validaciones anteriores. No se presenta la corrida parcial como aprobada.
- [13 capturas auténticas revisadas](screenshots/v323-final/README.md): antes/después, tarjeta/detalle, General/Ajustada/Reset, originales/cursor, toot/flauta, fuentes1,3/1,5 y horizontal. Sin V espurias en fixture normal; originales sin conectar; toot/melodía visibles; eje y notas sin recorte/superposición. Leyenda ocupa una línea a fuente1,0 y dos en los focos grandes. Sin prometer una única línea con cualquier fuente.
- **21 módulos** DSP/PCM/WAV/STFT/reproducción/persistencia/cachés/gestos/cámara comparados byte a byte con `checkpoints/before-3.2.3`: idénticos. Hashes en `v323-final-evidence.json`. Sin migración ni cambios de versiones de caché.

Samsung **R5CT21KTQ8W / SM-G990E**: respaldo fresco `samsung-before-3.2.3-20261021-144220`, **61 archivos/13 WAV**, tar15417344bytes. Certificado compatible `fbf54fbe12161bdbf5adbfb9d906961511e9b49a715be8eac8f886e01292b7b2`, versión8→9, mismo paquete. Guardas de micrófono inactivo/ausencia de `.part` y hashes estables antes de instalar. `adb -s R5CT21KTQ8W install -r …` → **Success**; arranque **Status:ok**, PID7123, buffer crash vacío. SHA del APK instalado coincide con el local. Comprobación a **2026-10-21T14:42:50**: **61/61 hashes idénticos**, ningún nuevo derivado ni diferencia runtime en ese momento; índice completo igual, **13 sesiones/13 WAV indexados legibles**. No extrapolar estos hashes a cambios posteriores del usuario. Informe `checkpoints/v323-samsung-update.json`; respaldo señalado por `backups/latest.txt`. Sin instrumentación, clear, uninstall, restauración, captura o reproducción automática en el teléfono. Accesibilidad de archivos/índice comprobada de forma no destructiva; no se atribuye recorrido visual o prueba musical física.

Checkpoint final recuperable `checkpoints/after-3.2.3`, indicado por `checkpoints/latest.txt`: fuente/documentos de esta iteración, APK final y manifiesto. Cambios anteriores preservados; no se creó un commit que los mezclara. Capturas auxiliares/ejecuciones descartadas archivadas en checkpoints, únicamente13 imágenes útiles en docs.

Fixtures visuales equivalentes al escenario reportado, no grabaciones del Samsung ni ejecuciones acústicas reales. PCM real analizada separadamente en privado. Límites: amplitud no persistida, tiempo de callback, posible incertidumbre en notas brevísimas imperfectas, precisión física/calibración y pruebas manuales de gesto/afinador/sincronía/15min/TalkBack pendientes.

## Graph Intelligence & UX Stability 3.2.2 — 21/10/2026

**APK3.2.2-beta1 / 8**, `ar.didgetaller`, 11257318 bytes, SHA-256 `caa33120c435943b2f441e652322393fb6f904421371c9d04c837663b720916d`. `testDebugUnitTest assembleDebug assembleDebugAndroidTest`: **BUILD SUCCESSFUL** (`checkpoints/v322-final-build.log`), **86 JVM**, cero fallos/errores/omitidos. Se verificaron los76 resultados existentes antes de modificar; se añadieron10 casos matemáticos/modelo. Stack/permisos intactos.

| Ejecución en emulator-5554 API37 | Resultado | Log checkpoints/ |
|---|---:|---|
| GraphIntelligenceUiTest | 3/3 | v322-intelligence-instrumentation.log |
| PrecisionUiTest | 5/5 | v322-precision-instrumentation.log |
| StudioViewer + SessionNavigation + LocalPersistence | 25/25 | v322-regression-instrumentation.log |
| ViewerRotation | 2/2 | v322-rotation-instrumentation.log |
| 360dp / fuente1,3: Intelligence + visor horizontal | 4/4 | v322-360-large-instrumentation.log |
| 420dp / fuente1,5: Intelligence + visor horizontal | 4/4 | v322-420-large-instrumentation.log |

**35 casos distintos**=3+5+25+2. Los ocho recorridos de matriz son repeticiones, no casos nuevos. JSON por ejecución contiene SHA del mismo APK final. Instrumentación solo emulador, con guardas de hardware; los tres nuevos casos crean/limpian exclusivamente preferencias `intelligence-tests-*` y directorio `intelligence-tests`. Ninguna prueba instrumental en Samsung.

### Causas y cambios comprobados

1. **Autoescala/zoom-out:** el dominio de Fundamental era literalmente `0..max(maxHz,500,full.high)`. General incluía todos los puntos aceptados por confianza/rango, incluso retornos aislados a períodos múltiples. Ahora General usa lecturas clasificadas confiables, percentiles2,5–97,5 para≥40 puntos, margen `max(0,6Hz;1,2% mediana;18% dispersión)` y contexto mínimo de tres semitonos por lado. El dominio interactivo es ese rango, no el rango físico global. Para70±0,15Hz:≈58,9–83,3Hz; Ajustada≈69,01–70,99Hz. Mediana/rango se memoizan por serie, no se recalculan por gesto. El pan X no altera Y. Reset restaura ambos ejes y desactiva rango extendido; no cambia selección/audio/mediciones. Valores fuera de la escala robusta se cuentan y siguen accesibles en Mostrar todos o rango exacto.
2. **Propiedad del estado:** `vm.explorer(id)` persistía en el mapa del ViewModel al regresar a Biblioteca y lo compartían detalle/fullscreen. La tarjeta ya calculaba su rango sin ese mapa; el defecto comprobado estaba en el preview integrado y la nueva visita. Ahora preview y fullscreen tienen objetos/cámaras separados por UUID/tipo de gráfico; comparten solo selección, fragmento y bucle. La salida real elimina ambos; la rotación no los elimina. UI comprueba igualdad de viewport de tarjeta antes/después, preview intacto al salir del fullscreen, nuevo estado al reabrir A y perfil/rango propio de B(flauta), además de cambio a STFT. Se evita también clamping de Y de miniaturas que pegaba lecturas recortadas al borde: se recorta el Canvas en coordenadas reales.
3. **Picos:** el índice ya contiene las caídas, antes de cualquier renderizado; no hay decimación de Fundamental que invente puntos. `PitchTrace` antes solo filtraba rango/confianza≥0,70, insuficiente para resolver ambigüedad de periodicidad. Se conserva esa política básica y se añade una clasificación contextual conservadora: un único punto con confianza<0,92, retorno a vecinos plausibles(conf≥0,65), ≤400ms por lado/≤600ms total, vecinos separados≤80cents y razón2/3/4 dentro de80cents → estimación incierta, nunca Hz corregidos ni error acústico declarado. Se interrumpe antes/después; el original queda como círculo opcional y cursor con razón, Hz y confianza YIN, sin nota/cents confirmados. Notas sostenidas, transiciones70→75→82→90, melodía392→440→523→440 y octavas limpias de alta confianza permanecen sin suavizado. Una articulación rapidísima de octava con periodicidad imperfecta puede quedar incierta: el dato no desaparece ni se modifica.
4. **0Hz/C#-14:** `PitchAxis` empezaba en `low.coerceAtLeast(.001)` y generaba MIDI negativos extremos; el texto redondeaba esas frecuencias positivas a0Hz. `Music.pitch` rechazaba0 exacto pero aceptaba cualquier positivo. Ahora Music admite solo MIDI0–127 con sus bordes de medio semitono (aprox7,94–12911Hz), y PitchAxis enumera únicamente0..127. Numeric ticks fuera de ese rango no llevan nota ni«≈». Pruebas de0/negativos/NaN/infinito/subnormales/0,001/1Hz y notas C2,C#2,C3,C4,A4,A5. UI solicita rango exacto0–1 y verifica0Hz sin nombre musical; luego Reset vuelve al auto-fit. También se corrigió semántica accesible que anunciaba«—Hz aproximadamente» para0.

### Audio real en modo solo lectura

Se leyeron los WAV del respaldo fresco previo a3.2.2, generado mientras estaba instalada3.2.1, sin copiarlos al repo público ni escribir al teléfono. Script local `checkpoints/diagnose322.py`, informe privado ignorado `v322-real-pitch-diagnostic.json`: cuatro eventos de didgeridoo, ventanas PCM16384 cercanas con el mismo promedio por4, win2200, diferencia/CMNDF y umbral0,14 del detector. Se informa frecuencia del lag entero, sin pretender interpolación ni alineación exacta con la captura.

| Tiempo original / Hz / confianza | Vecinos | PCM cercano, final de ventana | Resultado |
|---|---|---|---|
| 2,653s /42,903 /0,8762 | 2,475s126,857 →2,831s128,718 | 2,553s: RMS0,0765,≈43,07Hz/conf0,897; 2,653s: RMS0,0793,≈129,71Hz/conf0,896 | Mínimos a períodos relacionados y dependencia de la ventana; incierta por retorno aislado |
| 6,166s /44,776 /0,7447 |136,246 →138,035(conf0,698) | 5,966s≈44,82Hz; 6,166s≈137,81Hz | Vecino marginal y periodicidad ambigua; incierta |
| 6,927s /43,336 /0,7686 |132,604 →128,888 |6,827s≈43,41Hz;6,927s≈129,71Hz, RMS0,0446 | Retorno aislado; incierta |
| 7,870s /36,732 /0,7961(otra sesión) |141,346 →74,343 |7,770s≈36,87Hz;7,870s≈74,49Hz | Vecinos no coinciden: no se fuerza la clasificación de retorno |

Los timestamps guardados son del callback/tiempo transcurrido, no posiciones de muestra ni timestamp de inicio de ventana. La PCM cercana reproduce el comportamiento del estimador pero no prueba qué nota tocó la persona. No se modificó YIN por una presunción de ground truth. Silencio, incertidumbre de confianza, periodicidades ambiguas y cambios confiables conservan tratamientos distintos. Originales y cachés previas no se reescriben.

### UI, integridad y límites

[Capturas reales revisadas](screenshots/v322-final/README.md): General/Ajustada/Reset, tarjeta antes/después, preview y reapertura, flauta, incierta y cursor,0Hz, horizontal y fuentes grandes. Datos STFT sin ejes: **66,4% de altura útil en420dp/fuente1,0;71,2% en360dp/1,3;73,8% en420dp/1,5**, según logs `v322-*-layout-metrics.log`. Se conservan los48dp/insets/STFT de3.2; no se atribuye una nueva medición física del71,6%. Los gestos matemáticos y multitáctiles inyectados no equivalen a una prueba con dedos en Samsung.

Samsung `R5CT21KTQ8W`: certificado `fbf54fbe12161bdbf5adbfb9d906961511e9b49a715be8eac8f886e01292b7b2` compatible, install-r **Success**, arranque **Status:ok** y buffer crash sin fatal. Respaldo fresco `samsung-before-3.2.2-20261021-140322`: **60 archivos/12 WAV**, **59 archivos idénticos**. Diferencias runtime comprobadas: `{"shared_prefs/android.app.ActivityThread.IDS.xml": {"valid_xml": true, "entries": 1}}`; nuevos derivados: `[]`. WAV, índice, ajustes, relaciones y perfiles intactos en la comprobación de 2026-10-21T14:03:41. No clear/uninstall/instrumentación/reproducción/captura automática. Informe `v322-samsung-update.json`. Después, un segundo intento de comprobar volvió a detectar RECORD_AUDIO activo y se abortó antes de leer hashes o instalar; no se interfirió con esa captura. No extender los hashes de la primera comprobación a cambios posteriores hechos por el usuario. El teléfono estaba bloqueado durante la preparación: UI física de3.2.2 no comprobada.

14 módulos comparados byte a byte con antes de3.2.2: YIN, audio PCM/WAV, STFT, reproducción, simulación, persistencia, cachés y gestos anteriores idénticos; lista `v322-final-evidence.json`. No cambio de formatos ni versiones de caché, porque cambia exclusivamente presentación/estado/notación.

Fallos de desarrollo no contados: primera corrida nueva detectó selección de nodo fusionado en tarjeta (test pasó a árbol no fusionado) y semántica de0Hz; se corrigieron y repitieron los tres casos limpios. No se presenta una corrida parcial como regresión aprobada. Persisten límites previos: alineación de captura por callback, precisión/calibración física del micrófono, clasificación contextual no infalible, STFT743ms, actualización de regiones STFT al soltar, MediaPlayer no sample-accurate. Pendientes gesto físico, afinador, sincronía audible,15min/batería y TalkBack completo.

## Stability & Precision 3.2.1 — 21/10/2026

**APK:** 3.2.1-beta1/7, `ar.didgetaller`, 11518152 bytes, SHA-256 `c7b56895053f889019f51bedc338fa8e3bef2dbb1840bf1bd282de4a86b9ef33`. JDK17/SDK35, mismas dependencias/permisos. Gate `testDebugUnitTest assembleDebug assembleDebugAndroidTest`: **BUILD SUCCESSFUL**, **76 JVM, cero fallos/errores/omitidos** (`checkpoints/v321-final-build.log`, XML de app/build/test-results).

| Emulador emulator-5554 | Resultado limpio | Log checkpoints/ |
|---|---:|---|
| PrecisionUiTest | 5/5 | v321-precision-instrumentation.log |
| StudioViewer + SessionNavigation + LocalPersistence | 25/25 | v321-regression-instrumentation.log |
| ViewerRotation | 2/2 | v321-rotation-instrumentation.log |
| 360 dp, fuente 1,3 | 3/3 | v321-360-large-instrumentation.log |
| 420 dp, fuente 1,5 | 3/3 | v321-420-large-instrumentation.log |

**32 casos distintos** (5+25+2), no una suite única de 32. Focos de tamaño/fuente adicionales, no duplicar su conteo. JSON de cada ejecución registra el SHA del mismo APK final. Toda instrumentación se dirigió explícitamente al emulador y tiene guarda ranchu/goldfish. Ninguna en Samsung.

### Causas y comprobaciones

- Atrás: Vista amplia solo cambiaba `vm.fullScreen`; el back stack seguía en Medir raíz y Android salía. Se agregó `measure/wide` y propiedad común de captura; no BackHandler. Probados tres ingresos/retornos, Back real por tecla y gesto de borde, escucha sola, silencio y grabación activa. Mismo `sessionStartedAtMs`, prefijo de muestras y archivo .part que continúa creciendo; ninguna sesión hasta detener y exactamente una al guardar.
- Zoom: razones independientes de separación X/Y, umbral por eje y factor limitado por evento eran inestables al alinear/cruzar dedos. Se reemplazaron por razón radial de centroides de Compose. La fórmula anterior combinaba centro actual con pan escalado por el span nuevo, desplazando el ancla al hacer ambas cosas. Ahora `inicio = anterior.low + anterior.span*(centro-pan) - nuevo.span*centro`, en espacio lineal/log correspondiente, con clamp final. Márgenes Y de Fundamental estables; no cuantización de viewport ni presets durante gestos.
- Raster: STFT reiniciaba ventanas en el inicio del viewport y cortaba al final visible, por lo que un pan cambiaba las observaciones. Ahora origen/hop fijos al WAV y contribución ponderada por soporte temporal. Sin copiar celdas vacías de un vecino. Se conserva Hann32768/hop16384 y la referencia relativa fija; cache visual v3 separada de análisis v2. Se eliminó redondeo entero del rectángulo dibujado y la espera de180ms tras soltar/resetear. No FFT nueva por cada movimiento de dedo.
- Pruebas geométricas: zoom2/inverso,120 incrementos equivalentes a uno, ancla con pan simultáneo X/Y, dato→píxel→dato en tres tamaños, límites/zoom extremos lineales/log y entradas inválidas. Prueba STFT de pan de200 muestras conserva exactamente las ventanas y potencias de apoyo; sin columnas vacías sobre señal, silencio real cero. UI inyecta pinch horizontal/vertical, verifica proporción y ancla, pan en ambos ejes, rotación, continuación/inverso/reset y referencia fija; incluye onda y armónicos. Toggle log en el bin lineal inferior dejó de producir intervalo degenerado.
- Fundamental: miniatura usaba min/max±4 Hz; visor percentiles/margen estrecho. Además, Analizar podía sustituir la serie por reanálisis de otra ventana/cadencia, mientras Biblioteca conservaba la original. Ahora misma serie capturada, mismos tiempos/validez, `PitchTrace` y vista general común. Si no hay serie histórica, se identifica la fuente de reanálisis. Ajustada sigue disponible para <1Hz, con advertencia de puntos fuera; General incluye todos los válidos y contexto de ±3 semitonos como mínimo. No suavizado añadido.
- Datos reales leídos del respaldo (sin recalcular ni escribir): sesión de17,32s,91 puntos/43 válidos,35,24–91,12Hz; de7,72s,40/26,37,92–76,67Hz. Otra incluye126,86→42,90→128,72Hz con confianza0,876/0,912. Esos saltos existen en el índice; no son picos inventados por Canvas. La confianza no permite afirmar si son sonido real o errores de periodicidad/octava; se conservan. Flauta real:115 puntos/65 válidos,481,63–1518,83Hz, sin inferir su nota tocada. Diagnóstico privado ignorado por Git: `v321-real-timeline.json`.
- Baja confianza no participa de la curva ni escala; se representa con círculo aislado cuando entra en rango, se informa cantidad en herramientas. Una anomalía válida por rango/confianza se mantiene aunque difiera. Nulos y gaps interrumpen segmentos. Tests de70±0,1, silencio,35Hz con baja/alta confianza y melodía392→440→523 sin suavizar. La misma serie se conserva al terminar análisis WAV. Resumen de Biblioteca sigue describiendo la nota predominante, no todos los sonidos.
- Lecturas: el título del visor ya no se reemplaza por números truncados; badge i/acción Lectura completa lleva al panel con texto que ocupa el ancho disponible y se adapta a líneas. Se verifican texto y ausencia de overflow, fuentes1,3/1,5. STFT únicamente Hz y dB relativos. Layout de48dp/márgenes de STFT idéntico a3.2; pruebas exigen ≥65% de altura útil para datos sin ejes en las configuraciones recorridas. [Capturas revisadas](screenshots/v321-final/README.md).

### Integridad y límites

Samsung **SM-G990E / R5CT21KTQ8W**: firma `fbf54fbe12161bdbf5adbfb9d906961511e9b49a715be8eac8f886e01292b7b2` compatible; `install -r` **Success**, arranque correcto, sin fatal en buffer crash del proceso. Respaldo fresco `samsung-before-3.2.1-20261021-112011`: **48 archivos / 10 WAV**. **47 archivos idénticos**; única diferencia: `files/profileInstalled`, marcador ProfileVerifier de 24 bytes con esquema 1 y fecha exactamente igual a `lastUpdateTime` del paquete. Índice, ajustes, relaciones, perfiles y WAV intactos. El teléfono estaba bloqueado; no se atribuye inspección visual física de 3.2.1. Al revalidar más tarde no había proceso activo, sin fatal en el buffer del PID de arranque; no se volvió a iniciar. Informe `checkpoints/v321-samsung-update.json`; no clear/uninstall/restauración/instrumentación. La cifra actual supera los 8 WAV de 3.2 porque el usuario grabó después.

11 módulos DSP/captura/reproducción/simulación/persistencia comparados byte a byte con el checkpoint inicial: idénticos; listado en `v321-final-evidence.json`. Solo se cambió el muestreo visual STFT, sin cambiar sus parámetros acústicos ni el reanálisis numérico existente.

Fallos investigados, no aprobados: captura de panel con dos raíces; lectura con ancho intrínseco/overflow (ahora ancho completo); screenshot anterior al render retardado de reset (demora eliminada); aserciones antes de completar Back gestual (esperar destino). El recorrido de fragmento inyectaba Back con la actividad RESUMED pero sin foco tras cerrar el menú; se comprobó en PrecisionBack y se espera devolución del foco, sin cambiar navegación de producción. Una comparación exacta de100Hz tras log se sustituyó por tolerancia1e-8Hz. Intento ampliado interrumpido al límite240s (`v321-instrumentation-timeout.log`); ejecución final separada con360s. SystemUI del emulador murió a11:12:43 (DeadSystemException), interrumpiendo wm density antes de comenzar pruebas; se hizo arranque frío sin snapshots ni wipe SOLO del AVD, y se evitan reconfiguraciones redundantes. Log `v321-systemui-recovery.log`. No se cuentan ejecuciones fallidas/incompletas.

Pendiente: pinch con dedos físicos/Samsung y percepción de fluidez/latencia, precisión de flauta/PVC contra afinador, sincronía audible,15min/batería y TalkBack completo. No se afirma que ruido/fundamental débil estén calibrados. Durante un gesto se conserva la tesela completada en sus coordenadas; si se sale de su cobertura, la nueva región llega al soltar/cargar, sin inventar energía. STFT≈743ms difumina transitorios y ampliar no agrega información; MediaPlayer no es sample-accurate/gapless. APIs consultadas: [gestos Compose](https://developer.android.com/develop/ui/compose/touch-input/pointer-input/understand-gestures), [back stack](https://developer.android.com/guide/navigation/backstack).

## Cierre Acoustic Studio 3.2 — 21/10/2026

### APK y Samsung

- **3.2.0-beta1 / 6**, `ar.didgetaller`, JDK 17 / SDK 35; sin cambios de dependencias ni permisos. APK **11,624,005 bytes**, SHA-256 `08fbd64c52622ce2e601cc43ac6544a72211d27e06c36c3bd261a58e9201a0b5`. Certificado `fbf54fbe12161bdbf5adbfb9d906961511e9b49a715be8eac8f886e01292b7b2`, idéntico al APK instalado respaldado.
- Samsung **SM-G990E / R5CT21KTQ8W**: `adb -s R5CT21KTQ8W install -r` devolvió **Success**, arranque COLD correcto (1532 ms), PID inicial 28440, buffer crash vacío. No instrumentación, uninstall, pm clear, restauración, captura ni reproducción de audio en el teléfono.
- Respaldo fresco `backups/samsung-before-3.2-20261021-092022/`: tar 10.447.360 bytes, **27 archivos / 8 WAV** (el usuario tenía una sesión adicional frente a los siete WAV de 3.1). Índice, ajustes, relaciones y los ocho WAV conservan hashes. Verificación posterior completa: **25/27 archivos idénticos**; diferencias limitadas a los metadatos runtime documentados en `checkpoints/v32-samsung-update.json`, XML válido / marcador ProfileVerifier de 24 bytes con fecha compatible. Archivos nuevos exclusivamente derivados de análisis. No se ignoraron diferencias del usuario.
- Navegación física no destructiva por Biblioteca, sesión y visor; capturas reales antes/después de la MISMA sesión de **7,72 s**, inspeccionadas visualmente. Datos coloreados sin ejes: **306 / 1080 px (28.3%) → 773 / 1080 px (71.6%)**, porcentaje sobre altura TOTAL. No se extrapola esta sesión a todos los teléfonos. [Imágenes y matriz](screenshots/v32-final/README.md).

### Pruebas aprobadas

| Comprobación | Resultado | Evidencia |
|---|---|---|
| `:app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest --console=plain` | **67 JVM**, cero fallos/errores/omitidos; APK y test APK compilados | XML `app/build/test-results/testDebugUnitTest/`, `checkpoints/v32-jvm-summary.json` |
| StudioViewer + SessionNavigation + LocalPersistence | **25/25**, 112,687 s, emulador | `checkpoints/v32-regression-final-instrumentation.log` |
| ViewerRotation | **2/2**, 22,43 s, emulador | `checkpoints/v32-rotation-close-instrumentation.log` |
| Fuente 1,5, 420 dp; visor/Medir/perfil persistido | **2/2**, 16,785 s | `checkpoints/v32-420-f150-final-instrumentation.log` |
| 360 dp, fuentes 1,0 y 1,3 | **2/2 por configuración** | `checkpoints/v32-360-f100-instrumentation.log`, `v32-360-f130-instrumentation.log` |

Son **27 casos funcionales distintos**, aprobados en ejecuciones limpias complementarias (25 + 2), no una suite única de 27. Los JSON `v32-*-run.json` registran el mismo SHA del APK final. Después del gate solo se ajustaron las pruebas de rotación y del selector de gráficos; sus APK de instrumentación se recompilaron. El foco final del selector real aprobó 2/2 (`checkpoints/v32-evidence-close-instrumentation.log`), sin modificar el APK de la app. Verifica que una lectura puntual no se arrastre a otro tipo de gráfico; las capturas finales proceden de ese recorrido por menú. Todas las pruebas instrumentadas apuntaron explícitamente a `emulator-5554`, con guarda de hardware/almacenamiento aislado. No se ejecutó connectedDebugAndroidTest sobre dispositivos conectados.

JVM: equivalencias 69,2957/C♯2, 261,6256/C4, 392/G4, 440/A4 y 880/A5; cents decimales, octavas, entradas inválidas, ticks exactos/adaptativos y escala por instrumento. YIN: flauta 200/261,63/329,63/392/440/659,26/880/1600 Hz (tolerancia 0,3%, confianza >0,90); H2 más intenso que H1 más ruido uniforme (0,5%, >0,80); silencio/ruido sin nota; regresión didge 35,5/50/65,4/69,3/70/98/125/210 Hz (0,4 Hz). PCM melódico de siete segmentos verifica saltos y pausa sin resumen de nota predominante ni órdenes armónicos inventados. No son grabaciones de flauta física.

Persistencia en emulador: JSON antiguo sin campo conserva bytes al leer, WAV intacto; nueva flauta guarda/renombra/reabre con su instrumento, caché y rango correctos aun seleccionando Didgeridoo para la próxima captura. Se probó el flujo real de micrófono/grabación/guardado en emulador (silencio); no se presenta como validación acústica. La ficha de flauta no propone asociar geometría de didgeridoo.

Visor: inspección temporal de coordenada calculada desde `PlotDataArea` (2000 ms ±20 ms), pan/pinch, restablecer, selección, seguimiento/reproducción, Atrás, recreación/rotación y cuatro gráficos. La prueba de rotación comprueba que el reproductor sigue activo al volver al detalle antes de pausarlo para una aserción de UI estable. Medidas de área de datos sin ejes en horizontal: **74,6% / 71,2% / 66,4% / 73,8%** para 360-1,0 / 360-1,3 / 420-1,0 / 420-1,5. [Matriz visual](screenshots/v32-final/README.md).

### Fallos corregidos y límites

- Capturas iniciales confirmaron que la causa principal era la suma de TopAppBar, acciones, leyenda y lecturas en filas. No había doble inset del Scaffold exterior; se consolidó el propietario de áreas seguras y la barra de estado del visor. Se reemplazaron filas por barras de 48 dp y panel contextual. Un primer ajuste aún quedaba bajo la proporción deseada: se redujeron los márgenes reservados medidos, conservando etiquetas/ejes.
- En 360 dp el criterio inicial de horizontal corto era insuficiente y un snackbar tapaba Pantalla completa: corregidos. Con fuente 1,5 el contador temporal se truncaba: mayor ancho y prioridad frente a botones de zoom alternativos; prueba sin elipsis.
- Selectores instrumentados ambiguos y dos reemplazos incorrectos de etiquetas se corrigieron. Una ejecución de rotación se interrumpió por ComposeNotIdle al continuar el cursor; otra falló por la misma espera al volver al detalle. La prueba final verifica preservación del player antes de pausarlo. Logs de fallo/interrupción conservados en checkpoints; **no se cuentan como aprobados**.
- YIN de flauta conserva periodicidad, usa tasa completa, ventana de entrada 8192 y comparación ~50 ms, actualización ~93 ms; no máxima FFT ni continuidad que impida saltos. La guarda de ±0,5% solo cubre extremos/interpolación. Los ataques/mezclas ambiguas pueden no tener lectura; la confianza no es probabilidad calibrada.
- STFT de 32768 muestras (~743 ms) conserva resolución espectral de 3.1: transitorios rápidos pueden verse difusos. La pista de fundamental usa ventanas más cortas. Melodías sin nota predominante no reciben una F0 global ficticia para numerar armónicos.
- Pendientes manuales: flauta/PVC reales contra afinador, soplido y fundamental extremadamente débil, sincronía audible, 15 minutos/batería, accesibilidad completa. MediaPlayer sigue sin promesa sample-accurate/gapless; cámara retenida en recreación, no tras muerte del proceso. No se modificó el simulador de didgeridoo.

## 20/10/2026 — Cierre de Didge Taller 3.1 beta1

### APK e instalación

- **3.1.0-beta1 / código 5**, paquete `ar.didgetaller`, mismo certificado debug SHA-256 `fbf54fbe12161bdbf5adbfb9d906961511e9b49a715be8eac8f886e01292b7b2`, comprobado contra el APK instalado respaldado.
- APK: `app/build/outputs/apk/debug/app-debug.apk`, **11.136.831 bytes**, SHA-256 `a53dacb774288740967e2244214b2684db49997ba6e8d55f9399afd1516d259e`.
- Samsung **SM-G990E / R5CT21KTQ8W** actualizado mediante `adb -s R5CT21KTQ8W install -r ...`: **Success**. `am start -W` correcto, proceso **22054**, sin FATAL EXCEPTION en su buffer crash. No se iniciaron grabaciones ni reproducciones reales.
- Respaldo fresco: `backups/samsung-before-3.1-20261020-203046/`, tar de **9.607.168 bytes**, **24 archivos / 7 WAV** verificados antes de instalar. Después: **22 archivos idénticos**, incluidos todos los WAV, índice, ajustes, relaciones y cachés anteriores. Cambiaron solo `shared_prefs/android.app.ActivityThread.IDS.xml` y `files/profileInstalled`, metadatos automáticos del runtime. Se decodificó el marcador de 24 bytes: esquema 1, fecha **21:03:39**, coincidente con la actualización del paquete. [AndroidX ProfileVerifier](https://raw.githubusercontent.com/androidx/androidx/androidx-main/profileinstaller/profileinstaller/src/main/java/androidx/profileinstaller/ProfileVerifier.java) documenta ese archivo. No se restauró, borró ni recreó dato del usuario.

El último retoque de borde/formatos tuvo una segunda actualización no destructiva (5→5), después de un nuevo respaldo en `backups/samsung-before-3.1-20261020-210152/`. También devolvió Success; el APK/PID anteriores corresponden al cierre final. Frente al respaldo original de 3.0 siguen intactos 22/24 archivos y los 7 WAV; frente al respaldo inmediatamente previo, 23/24 (solo cambió el marcador ProfileVerifier).

### Pruebas ejecutadas

| Objetivo | Resultado | Evidencia local |
|---|---|---|
| Línea base | 44 JVM y assembleDebug aprobados | Checkpoint anterior y registro de inicio |
| Gate final `:app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest --console=plain` | **57 JVM**, 0 errores/fallos; APK y APK de pruebas compilados | `checkpoints/v31-visual-close-build.log`, XML en `app/build/test-results/testDebugUnitTest/` |
| Suite SessionNavigation + LocalPersistence + ViewerRotation | **22/22**, 268,462 s, emulador | `v31-regression-final.log` |
| Pan/pinch, selección/bucle, recreación/horizontal | **3/3** foco | `v31-viewer-focus.log` |
| Rango 100 Hz/log, referencia fija, inspección H1/dB y audio del espectro | Aprobada tras esperar estado de referencia inicializado | `v31-final-state-focus.log` |
| Permiso concedido al regresar/recrear, sin arranque oculto | Aprobada | Caso `returningWithPermissionGrantedUpdatesFeedbackWithoutStartingCapture` en `v31-swipe-permission-focus.log`; esa ejecución tenía un fallo distinto de swipe, corregido después |
| Swipe corto lento/rápido, reversión, mantener/suelta y Deshacer/WAV; Medir/foco/formularios | **2/2** después de correcciones | `v31-safety-fix-focus.log` |
| Último retoque visual de diseños/resonancias y regresión swipe | **2/2**, cero píxeles rojos en tarjeta en reposo | `v31-visual-close.log`, 40,686 s |
| Atrás con modo de tres botones | **1/1**, además del gesto real desde el borde aprobado en suite | `v31-threebutton.log` |
| Matriz 360 dp / fuentes 1,0 y 1,3; 420 dp / 1,0 y 1,5 | Cuatro recorridos aprobados; capturas reales inspeccionadas | `v31-matrix-*.log`, `v31-last-foci.log`, `docs/screenshots/v31-final/` |
| TalkBack | Smoke parcial: servicio ligado, exploración táctil activa, 14 nodos etiquetados y acción principal identificable; ajustes restaurados | `v31-talkback-dumpsys.txt`, `v31-accessibility-smoke.json`; no lectura audible ni recorrido completo |

**24 casos funcionales instrumentados distintos** aprobados entre suite y focos; los recorridos de capturas son adicionales. No se afirma una suite única final de 24 ni cobertura completa. Todas las invocaciones instrumentadas usaron `adb -s emulator-5554 shell am instrument ...`, nunca `connectedDebugAndroidTest` ni el Samsung. No hubo desinstalaciones, pm clear o borrado privado sobre el teléfono.

Las 13 JVM nuevas prueban proyección píxel/dato, inversión vertical, anclaje de zoom, pan, extremos, límites lineales/logarítmicos, ticks y escala robusta con valores atípicos/silencio. Tolerancias: **1e-9** para coordenadas/fracciones y **1e-6 Hz** en transformaciones logarítmicas. Se compararon **26 archivos anteriores** de core/data/captura/reproducción contra el ZIP inicial: idénticos byte a byte.

WAV largo sintético: **900 s / 39.690.000 muestras / 79.380.044 bytes**. Se abrió el visor, inició/canceló STFT y generó onda con UI operativa en **10.513 ms para el recorrido completo**; zoom a 10 ms recuperó 441 muestras. Se conservó el original. JVM también limita la STFT a 65.536 celdas y onda a 1024 puntos. No es un benchmark aislado de FFT/Samsung y no se midieron FPS, batería o memoria total con perfilador.

### Fallos investigados y corregidos

- Redondeo de tick logarítmico apenas fuera del borde: clamp de presentación y prueba. Un fixture de outlier estaba fuera del rango válido del detector; se corrigió el fixture.
- Pinch inicial de prueba tenía argumentos en orden incorrecto; corregido según API oficial, sin adaptar la matemática a una prueba errónea.
- Cierre al rotar por altura transitoria casi nula del gráfico y límites inválidos para etiquetas; se evita dibujar hasta tener espacio útil. Recreación y rotación real posteriores aprobadas.
- Herramientas fuera del ancho y selección comprimida: barra compacta/FlowRow. El espectro ocultaba pausa: reproductor disponible en los cuatro gráficos.
- La prueba de referencia espectral leía antes del efecto de inicialización; espera explícita del estado completo. Conservación al cambiar rango/log aprobada.
- Gesto corto rápido de 80 ms descartaba por velocidad antes del umbral: reproducido y corregido exigiendo distancia al soltar. Foco posterior con WAV/Deshacer aprobado.
- Snackbar de permiso seguía visible durante captura válida: se retira cuando el permiso está disponible, comprobado por aserción y nueva captura.
- El primer script local de verificación tenía un alias de encoding inválido y falló antes de operar sobre Samsung. Tras instalar, el control estricto de hashes detectó el marcador ProfileVerifier; se investigó en modo de lectura y se comprobó su estructura/fecha. No se reinstaló como remedio ni se ignoraron diferencias sin identificar.

### Límites y prueba manual

Pendientes: didgeridoo PVC, lectura desde 1–2 m con nota activa, micrófono/AGC, sincronía audible y latencia de seek/bucle, gesto predictivo Samsung, TalkBack completo, fuentes personalizadas del teléfono y captura física sostenida de 15 minutos/almacenamiento/temperatura. Los sintéticos no demuestran precisión profesional. Cámara/selección sobreviven a recreación de Activity; no se promete restaurarlas tras muerte del proceso.

Prueba manual: Biblioteca → sesión de ~69,6 Hz → Pantalla completa → alternar gráficos → arrastrar/pellizcar/mantener → Girar → reproducir/pausar/buscar → Seguir y explorar manualmente → Atrás a sesión y Biblioteca. En Herramientas, marcar inicio/fin, analizar fragmento y probar bucle. Para borrar, usar una sesión prescindible y Deshacer; conservar la papelera si se desea recuperar el audio.

Fecha inicial: 19-10-2026.

## 20/10/2026 — Entrega integral 3.0 beta1 (estado vigente)

Se implementaron herramientas B–F reutilizando A. Se inspeccionaron AGENTS, README, documentación/arquitectura/código/pruebas, dispositivos y estado Git antes de modificar. Git sigue ausente; se creó `checkpoints/before-3.0-20261020-160736.zip`. Comparación binaria contra ese checkpoint confirmó **11 archivos de fase A/motores sin un solo byte cambiado**: PitchDetector, FFT, lector, PSD, armónicos, WavAnalysis, SessionAnalysis, Music, WavCodec, AcousticModel y BoreOptimizer. No se añadieron permisos/dependencias ni red.

### Resultados ejecutados

| Verificación | Resultado real |
| --- | --- |
| Línea base `gradlew.bat :app:testDebugUnitTest :app:assembleDebug --console=plain` | Éxito antes de modificar, suite previa de 33 JVM/código A |
| Gate final `gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest --console=plain` | **BUILD SUCCESSFUL in 16s**, 44 JVM sin fallas: Core 10, AudioAnalysis 20, WavReader 3, VisualAnalysis 11. Logs `app/build/gradle-v3-final.log`, XML JUnit en `app/build/test-results/testDebugUnitTest/`. |
| Primera suite instrumentada completa, solo `emulator-5554` API 37 | 16 casos en 118,176 s: 15 pasaron y falló el selector de error duplicado. No se presenta esta ejecución como suite limpia. |
| Focos posteriores de UI | Se corrigió el contexto del error y el scroll de la aserción; `damagedAudioReportsErrorWithoutChangingMetadata` pasó en la corrida focalizada de 2 casos (43,018 s), cuyo otro caso detectó un timeout de bucle. La corrida previa de 3 casos (68,108 s) ya había aprobado captura y explorador/zoom, pero todavía fallaba visibilidad del error. |
| Regresión de seeks/bucle | Se reprodujo timeout al encadenar seek previo al inicio y cerca del final. Se coordinó un solo seek pendiente con OnSeekCompleteListener. `explorerRendersRealGraphsSeeksPausesLoopsAndCompares` pasó (45,396 s); tras ajustar onStop para no marcar cancelada una vista terminada, volvió a pasar **37,631 s** con esa comprobación añadida. |
| Asociación/anotación manual | `designAssociationAndManualMarkersSurviveReopening`: **OK (1 test), 27,686 s**, selector real de diseño, simulación vs audio separadas y relación/marcador conservados al reabrir. Su primer intento tocaba detrás del snackbar de guardado; la prueba espera ahora su cierre antes de tocar, y verifica que exista el diseño. |
| Total de cobertura instrumentada de esta entrega | **18 casos distintos aprobados**, 7 de persistencia y 11 UI, mediante suite y repeticiones focalizadas; **no una única corrida final de 18**. Todos los comandos y guardas apuntaron al emulador; nunca Samsung. Logs en `app/build/instrumentation-v3*.log`. |
| Integración Medir con AudioRecord real del emulador | Un botón inicia captura, el mismo detiene/guarda WAV legible; segunda captura finaliza/guarda con onAppStopped sin seguir escuchando. Se verificaron duración/tamaño, reapertura e incremento del listado. La entrada del emulador no representa al PVC. |
| Explorador del emulador | WAV sintético → STFT/bitmap con energía real, onda, zoom/desplazamiento, inicio/fin 5–10 s, análisis de 5 s y F0 66,1±0,3 Hz, seek fuera/dentro del bucle, retorno al inicio, pausa/cursor estable (≤80 ms de tolerancia en la comprobación), consulta H1, comparación A/B y Atrás a Biblioteca. No se evaluó audio audible. |
| Papelera/compatibilidad | Swipe corto cancela; mantener dedo más allá del umbral no elimina; soltar elimina del listado y Deshacer restaura bytes. Prueba de stores reabiertos conserva WAV, índice original, caché, marcador y relación de diseño. También pasaron legado Java/diseños, WAV anterior/recuperado, renombrado y recuperación de captura parcial propia. |
| Cancelación/errores/integridad | Cancelación STFT/onda/core; fase A/progreso/Cancelar sobre fuente de 900 s en emulador; callbacks descartados al salir; silencio, WAV dañado/incompleto/no compatible. Caché reutiliza intervalos y cambia al modificar WAV sintético; originales/índice sin cambios por analizar. Espacio insuficiente simulado conserva audio previo válido. |
| Captura/lectura de 15 minutos en JVM | WAV escrito en bloques y releído: **79.380.044 bytes / 39.690.000 muestras / 900.000 ms**, memoria PCM acotada. No es AudioRecord físico durante 15 minutos. |
| STFT de 900 s en JVM | Fuente virtual completa, 256×256 floats = **262.144 bytes de celdas**, ~3,8 s en esta PC (varía por ejecución); no se conserva la matriz de todas las ventanas. No mide consumo/batería del Samsung ni todo el pipeline YIN de 15 minutos. |
| Captura de pantalla | `docs/screenshots/v3-spectrogram-synthetic-66hz.png`, captura real Compose/Canvas de la prueba sobre seno de 66,1 Hz/20 s. No es maqueta ni grabación real del PVC. |

Comandos de instrumentación: instalar app/test con `adb -s emulator-5554 install -r <APK>` y ejecutar `adb -s emulator-5554 shell am instrument -w [-e class ar.didgetaller.ui.SessionNavigationTest#metodo] ar.didgetaller.test/androidx.test.runner.AndroidJUnitRunner`. Se usó una lista separada por comas para los focos de varios métodos. No se ejecutó `connectedDebugAndroidTest`, para no permitir selección automática del teléfono.

### Tolerancias y algoritmos

Fase A conserva las tolerancias ya descritas abajo: tonos 65,41/69,30/73,42/70 Hz, fundamental débil/ausente, armónicos ajenos/ruido/silencio y niveles analíticos. Las nuevas pruebas STFT admiten **±3 Hz** en el pico rasterizado (bins ~1,35–1,46 Hz y filas de ~1,95 Hz, Hann finita), **±3 dB** para relación entre filas de 140/210 Hz reducidas; no sustituyen el ensayo más estricto de PSD de fase A (±0,4 dB sintéticos). Los extremos/muestras de onda se preservan exactamente a 1/32768 de amplitud digital; cabecera, frames y duración se comprueban con enteros exactos. Las tolerancias de UI/reproductor son funcionales, no certifican sincronía audible fina ni un bucle entre muestras sin separación.

La STFT reutiliza FFT/Hann de fase A (N32768, hop16384), promedia potencia de ventanas reales por celda de viewport y expresa dB respecto al máximo visible. Onda min/max/muestras, espectro con máximos por columna y candidatos medidos, caché SHA-256/versionada/64 MiB. MediaPlayer aporta currentPosition, seeks confirmados y bucle con polling sobre posición real; no integra un cronómetro ficticio. Papelera aditiva persiste tombstones sin borrar WAV antes del vaciado explícito. Especificación completa: [SESSION_EXPLORER.md](SESSION_EXPLORER.md).

### APK y Samsung físico

- APK final: `app/build/outputs/apk/debug/app-debug.apk`, **11,287,565 bytes**, `3.0.0-beta1` / versionCode **4**, paquete `ar.didgetaller`.
- SHA-256 del APK final e instalado: **0EE9820862E51DEF416859576DC2339E592C409A410E96A69F6A765044650C87**.
- `apksigner verify --print-certs` comparó APK anterior y nuevo: certificado SHA-256 **FBF54FBE12161BDBF5ADBFB9D906961511E9B49A715BE8EAC8F886E01292B7B2**, idéntico. Se verificó paquete/versionCode y que no hubiera RECORD_AUDIO en ejecución.
- Samsung **SM-G990E / R5CT21KTQ8W**, estado ADB device. Actualización autorizada `adb -s R5CT21KTQ8W install -r app/build/outputs/apk/debug/app-debug.apk` → **Success**. Se aplicó primero la versión probada y después la corrección final del estado de vista al volver de segundo plano, ambas con la misma actualización no destructiva.
- Último inicio: `am start -W -n ar.didgetaller/.MainActivity` → **Status: ok**, COLD, 1808 ms; PID **24724**. Consulta `logcat -d --pid=24724 -t 300 AndroidRuntime:E '*:S'`: sin entradas. `dumpsys package` confirma 3.0.0-beta1/4; hash del base.apk instalado idéntico al archivo local. Esto verifica arranque/proceso, no una inspección visual/acústica completa en el teléfono.
- Respaldo anterior verificado: [DATA_SAFETY.md](DATA_SAFETY.md). **5 WAV, local_data.xml y tuner_settings.xml conservan sus SHA-256** antes/después de las actualizaciones y arranques. Una comparación global inicial difirió por la preferencia interna `android.app.ActivityThread.IDS.xml`: se identificó el único campo `IDSCount`, actualizado por Android al iniciar; no contiene el índice de sesiones. No se promete igualdad de ese contador del sistema ni se restauró sobre él. La comparación de datos de usuario se hizo por hashes normalizados, sin depender de saltos CRLF de los logs de Windows.
- No se desinstaló, limpió, restauró ni instrumentó el teléfono. No se borraron/modificaron sesiones/WAV reales, no se inició captura/análisis/reproducción de ellos, no se cambió firma/ID ni se usaron install -d/-t. El reporte local está en `app/build/samsung-v3-validation.json` y el respaldo privado en `backups/latest.txt`.

### Pendientes y prueba manual recomendada

1. Samsung → Biblioteca → sesión anterior de aproximadamente **69,6 Hz / Do♯2** → desplazarse a **Análisis del WAV → Analizar grabación**. Leer F0/mediana/media, incertidumbre, candidatos Hn y dB. El resultado puede diferir del resumen anterior; no se fuerza una nota esperada.
2. Volver al explorador, alternar Fundamental / Espectrograma / Forma de onda / Armónicos. Probar 0–500 Hz, escala log/lineal y entrada exacta de rango (por ejemplo 50–100 Hz). Tocar para buscar, ampliar y comprobar cursor al reproducir/pausar/reiniciar. Confirmar reproducción audible del WAV original.
3. Marcar inicio y fin, Analizar fragmento y Repetir fragmento. Comparar dos sesiones o dos intervalos y alternar normalización. Probar marcador manual y asociación con un diseño de dimensiones realmente ingresadas; simulación y audio deben seguir distinguidos.
4. Biblioteca: swipe corto/vuelta cancela; largo al soltar mueve a papelera; Deshacer restaura. También se puede restaurar después de reabrir la app. **No vaciar la papelera si querés conservar esa sesión.**
5. Medir: grabación corta del PVC, detener/reabrir/reproducir. Luego probar 15 minutos sostenidos con espacio libre suficiente, detención manual/al salir y contador. Esta captura física no se ejecutó por el agente.

Siguen pendientes precisión acústica frente a referencia independiente, sincronización fina/audible y deriva durante 15 minutos reales, consumo térmico/batería/memoria del teléfono, TalkBack/fuentes grandes/rotación/tamaños físicos y falta real de espacio/interrupción de energía. No se validaron modelos contra CADSD/DidgeLab ni un clasificador de vocales/toots. El bucle no es sample-accurate/gapless. El detector de fundamental de reanálisis mantiene 35–220 Hz, aunque los gráficos llegan a Nyquist. Lo pendiente está marcado en ROADMAP; no se atribuye precisión profesional por pasar señales sintéticas.

## Registro de entregas anteriores (los límites/cantidades siguientes son históricos)

## 20/10/2026 — Validación e instalación de fase A en Samsung

Alcance exclusivo: revisar fase A, completar pruebas, compilar y actualizar el teléfono. No se avanzó B ni se modificaron algoritmos/código de producción, identificador, firma, versión o esquema. Se revisaron AGENTS, AUDIO_ANALYSIS, VALIDATION, ROADMAP, clases de DSP/lector y panel/integración al detalle. Git sigue ausente (`not a git repository`). El panel está después de Audio y antes del gráfico temporal; no depende de que exista resumen/serie histórica.

| Verificación | Resultado |
| --- | --- |
| `gradlew.bat :app:testDebugUnitTest --console=plain` | `BUILD SUCCESSFUL in 3s`; **33/33** JVM: CoreTest 10, AudioAnalysisTest 20, WavReaderTest 3, cero fallas |
| `gradlew.bat :app:assembleDebug --console=plain` | `BUILD SUCCESSFUL in 1s`, APK de fase A actualizado respecto de lo instalado antes en Samsung; tareas `UP-TO-DATE` porque producción no cambió |
| `gradlew.bat :app:assembleDebugAndroidTest --console=plain` | `BUILD SUCCESSFUL in 3s`; solo se modificaron pruebas |
| Instrumentación focalizada `adb -s emulator-5554 shell am instrument -w -e class <lista de tres métodos> ar.didgetaller.test/androidx.test.runner.AndroidJUnitRunner` | **OK (3 tests)**, 44,121 s. Métodos: `recoveredAudioCanBeAnalyzedWithoutChangingWavOrIndex`, `damagedAudioReportsErrorWithoutChangingMetadata`, `progressAndCancelButtonWorkOnLongReadOnlyWav` de SessionNavigationTest. No se repitió toda la suite previa de navegación/persistencia. |
| WAV recuperado en UI del emulador | Abrir Biblioteca/detalle, Analizar grabación, resultados H1, bytes/índice idénticos; después Reproducir/Pausar sigue funcionando con MediaPlayer. Sin evaluación audible. |
| Progreso/Cancelar en UI del emulador | WAV sintético de prueba extendido a 900 s en almacenamiento aislado; progreso visible, botón Cancelar, mensaje Análisis cancelado, sin resultado ni cambio de índice/tamaño. No es grabación de 15 minutos ni benchmark completo. |
| Error en UI del emulador | WAV dañado de prueba muestra `No es un WAV RIFF válido.` sin cambiar metadatos. |
| Identificación física | `adb devices -l`: Samsung **SM-G990E**, serial **R5CT21KTQ8W**, estado `device`; también estaba emulator-5554. Todos los comandos de teléfono usaron su serial explícito. |
| Actualización física autorizada | `adb -s R5CT21KTQ8W install -r app/build/outputs/apk/debug/app-debug.apk` → **Success**. Ninguna otra opción de instalación. No hubo incompatibilidad de firma o versión. |
| Arranque y errores | `am start -W -n ar.didgetaller/.MainActivity` → `Status: ok`, `LaunchState: COLD`, 961 ms; a los 2 s PID 9812 activo. `logcat -d --pid=9812 -t 300 AndroidRuntime:E '*:S'` sin entradas. El teléfono estaba `Dozing`/`NotificationShade`: no se validó visualmente el detalle desbloqueado ni se reprodujo/analizó su audio. |
| Conservación de datos físicos | SHA-256 de `shared_prefs/local_data.xml` y de los **4 WAV privados**, mismos nombres, idénticos antes de instalar y después del arranque. SHA del `base.apk` instalado coincide con el APK local indicado abajo. Solo lectura para las comprobaciones. |

Pruebas numéricas añadidas: seno de **70 Hz**, F0 ±0,25 Hz, pico ±0,12 Hz; mezcla **70/140/210 Hz** con amplitudes 1000/9000/6000 PCM (H1 débil), F0 ±0,25 Hz, picos ±0,20 Hz, orden H1/H2/H3, H2 más intensa. Niveles relativos contra `20 log10(A/A_H2)` ±0,4 dB por ventanas finitas/cola y cuantización. Centroide de potencia analítico ±0,25 Hz; energía 0–100 Hz contra 1/118 ±0,002 y suma de fracciones 1 ±1e−12. Las pruebas existentes siguen cubriendo silencio/DC, fundamental ausente, ruido, picos ajenos, PCM16, WAV truncados y cancelación. Estas tolerancias sintéticas no demuestran precisión acústica sobre didgeridoos reales.

APK instalado: `app/build/outputs/apk/debug/app-debug.apk`, **13.742.147 bytes**, SHA-256 **B8B711A0B6ABB7C5B82CE62B1985409B3C468F3455E4BFE3AD5B39E513FCD857**, `2.1.0-beta1`/3. Es el mismo código de producción de fase A; las adiciones de esta tarea están en tests y documentación.

No se ejecutó en Samsung instrumentación, desinstalación, `pm clear`, borrado de archivos, recreación de índice ni migraciones. No se cambió firma/ID ni se usaron `install -d`/`install -t`. No se inició una grabación, análisis o reproducción de sus sesiones. Queda al usuario la prueba acústica manual y la comprobación visual tras desbloquear.

**Prueba manual exacta:** desbloquear → abrir Didge Taller → Biblioteca → Sesiones → abrir su WAV anterior de aproximadamente **69,6 Hz / Do♯2** → desplazarse debajo de Audio hasta **Análisis del WAV** → **Analizar grabación** → esperar el resultado/Volver a analizar → leer frecuencia predominante, mediana/media, picos Hn candidatos, Hz y dB relativos → volver al bloque Audio y pulsar **Reproducir**, luego **Pausar**. No hace falta guardar de nuevo. La F0 puede resultar incierta, y no todos los múltiplos tienen necesariamente un pico real. Precisión física, reproducción audible y sincronía fina siguen pendientes.

## 20/10/2026 — Didge Taller 3.0, fase A: fundamentos de análisis

Entorno Windows 11, JBR 21.0.11, SDK/compileSdk 35; mismas dependencias. Se revisaron AGENTS, README, contexto, arquitectura, roadmap, validaciones, registro de trabajo, estudio DidgeLab y módulos de audio/core/datos/UI/navegación/pruebas. `git status --short` devolvió `fatal: not a git repository`; no se dispone de diff/commits. La navegación corregida y las 7 pruebas instrumentadas previas estaban presentes en código, no solo 4 como decía el pedido.

| Verificación | Resultado | Alcance real |
| --- | --- | --- |
| Línea base `gradlew.bat :app:testDebugUnitTest :app:assembleDebug --console=plain` | `BUILD SUCCESSFUL in 1s`, tareas `UP-TO-DATE` | Suite previa de 10 JVM y APK anterior |
| Primer núcleo `:app:testDebugUnitTest --console=plain` | `BUILD SUCCESSFUL in 5s`; 29 pruebas sin fallas | 10 existentes + 16 DSP + 3 WAV; antes de añadir 2 casos de rango/ambigüedad |
| Compilación final `gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest --console=plain` | `BUILD SUCCESSFUL in 7s`; **31/31 JVM**, cero fallas | `CoreTest` 10, `AudioAnalysisTest` 18, `WavReaderTest` 3; APK app/test generados |
| `adb -s emulator-5554 shell am instrument -w ar.didgetaller.test/androidx.test.runner.AndroidJUnitRunner` | **OK (10 tests)**, 17,245 s | 4 persistencia/audio, 3 navegación y 3 nuevas: WAV recuperado de solo lectura, error WAV dañado y cancelación al salir |
| Tras precalcular centroide/bandas en worker: instrumentación focalizada con `-e class ar.didgetaller.ui.SessionNavigationTest#recoveredAudioCanBeAnalyzedWithoutChangingWavOrIndex` | **OK (1 test)**, 20,061 s | Reanálisis completo de WAV recuperado, resultado y panel, bytes e índice idénticos antes/después; captura real de la UI |

Todos los APK se instalaron explícitamente en `emulator-5554` con `install -r`; ADB también listaba el Samsung, pero **ningún comando se dirigió al teléfono físico en esta fase**. No hubo instrumentación, instalación, borrado, migración ni reproducción en Samsung. Las pruebas usan directorios/preferencias propios y no cambian la Biblioteca existente. No se corrió `connectedDebugAndroidTest`.

### Señales y tolerancias verificadas

- FFT frente a una DFT compleja independiente de 32 puntos: error absoluto ≤1e−10 por componente.
- PSD digital: seno de amplitud 0,5 y frecuencia exactamente en bin → potencia integrada 0,125 ±0,00002; duplicar amplitud → +6,0206 ±0,002 dB. Centroide de mezcla conocida ponderada por potencia ±0,04 Hz; bandas suman 1 ±1e−12.
- Senoides 65,41 / 69,30 / 73,42 Hz: F0 ±0,25 Hz y nota equivalente correcta; picos aislados Hann/interpolados ±0,12 Hz. Son tolerancias sintéticas, no una especificación de precisión del micrófono.
- H1 débil/H1 ausente con H2+H3 dominantes: F0 común recuperada y H2 más intensa; no se fabrica pico H1. Solo componentes pares: se acepta la periodicidad observable 138,6 Hz, no se impone 69,3 Hz. Desvío de H2 a 141,4 Hz y pico ajeno a 333 Hz se distinguen.
- Ruido moderado reproducible y modulación de amplitud: F0 ±0,35 Hz; ruido solo, DC y silencio no producen resumen ni órdenes armónicos confiables. Barrido suave refleja cambio; selección de intervalos antes/después de un cambio de nota no mezcla el otro registro. 440 Hz a 48 kHz funciona con rango configurado hasta 1000 Hz.
- Reanálisis idéntico produce serie/resumen/PSD idénticos; omitir visualizaciones no cambia Hz/confianza del detector en vivo. WAV PCM16 del encoder anterior, estéreo con bloque JUNK impar, muestreo real, lecturas arbitrarias, EOF, formato inválido y truncado cubiertos.
- Fuente virtual de **15 minutos**: se solicitan solo buffers de 32768 muestras y la cancelación se respeta tras tres lecturas. No es grabación real de 15 minutos ni benchmark completo de análisis largo.

Captura real: [`screenshots/analisis-wav-sintetico-emulador.png`](screenshots/analisis-wav-sintetico-emulador.png), obtenida por la prueba sobre un **seno sintético de 66,1 Hz / 20 s** como WAV recuperado. Se observan 53 ventanas, F0/centroide y H1 calculados. No representa al PVC ni un espectrograma implementado.

APK fase A: `app/build/outputs/apk/debug/app-debug.apk`, 13.742.147 bytes, SHA-256 `B8B711A0B6ABB7C5B82CE62B1985409B3C468F3455E4BFE3AD5B39E513FCD857`; continúa `2.1.0-beta1`/3. Documentación científica/limitaciones en [AUDIO_ANALYSIS.md](AUDIO_ANALYSIS.md), [HARMONICS.md](HARMONICS.md) y [ACOUSTIC_VALIDATION.md](ACOUSTIC_VALIDATION.md).

Pendientes de esta fase: contraste con software externo y audio físico etiquetado, precisión/ruido/procesamiento automático del Samsung, fuentes grandes/TalkBack del panel nuevo y rendimiento completo de WAV de 15 minutos. SciPy no está instalado (`ModuleNotFoundError`) y no se usó como oráculo ejecutado. Sí se verificaron fórmulas por DFT/potencia analítica. Fases pendientes: visualizaciones interactivas/tiles STFT (B), selección/seek/bucle/caché persistente (C), comparador (D), asociación a diseños/validación física del modelo (E), swipe/Deshacer persistente/tarjetas y WAV progresivo para 15 minutos (F). Captura real sigue limitada a 60 s y conserva su implementación actual.

## 20/10/2026 — Corrección del back stack del detalle de sesión

Causa: `LibraryScreen` guardaba un `selectedId` local y reemplazaba la lista por el detalle sin crear un destino. Android no tenía una entrada anterior para Atrás. Se reemplazó esa selección por `NavHost`/`NavController` de Navigation Compose 2.9.0: `measure`, `lab`, `library`, `session/{id}`. La lista conserva su entrada y su `rememberScrollState`. La flecha Material de la barra superior hace `popBackStack()`; Atrás lo resuelve el propio NavHost. Un listener de cambios de destino detiene captura y reproducción y libera MediaPlayer. No se agregó un BackHandler, ni se modificaron motores o formatos de persistencia.

| Verificación | Resultado y alcance |
| --- | --- |
| Línea base `gradlew.bat :app:testDebugUnitTest :app:assembleDebug --console=plain` | `BUILD SUCCESSFUL in 1s`; tareas anteriores al cambio actualizadas |
| `gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest --console=plain` tras la corrección final | `BUILD SUCCESSFUL in 10s`; 10 pruebas JVM, cero fallas/errores; APK app y test compilados |
| Primer intento instrumentado en API 37 | Las 4 pruebas existentes pasaron; las 3 nuevas fallaron antes de interactuar con UI por `NoSuchMethodException: InputManager.getInstance` en Espresso transitivo 3.5.0. Se fijó `espresso-core:3.7.0`, que elimina esa reflexión según las [notas oficiales](https://developer.android.com/jetpack/androidx/releases/test). Sin cambios de Compose, SDK ni algoritmos. |
| `gradlew.bat :app:assembleDebugAndroidTest --console=plain` tras ajustar Espresso | `BUILD SUCCESSFUL in 6s` |
| `adb -s emulator-5554 shell am instrument -w ar.didgetaller.test/androidx.test.runner.AndroidJUnitRunner` | Ejecución final `OK (7 tests)` en 32,903 s. 4 pruebas de persistencia/reproducción y 3 de navegación, incluido el borde inferior tras unificar barras; **solo emulador API 37**. |
| `SessionNavigationTest`: tecla Atrás de Android y flecha superior | Lista desplazada hasta la décima tarjeta para Atrás y hasta la última para la flecha; audio WAV sintético reproduciéndose. Ambos caminos vuelven a Biblioteca → Sesiones con el mismo offset (tolerancia 1 px), actividad `RESUMED`, ID de reproducción nulo y posición cero. Abrir/cerrar de nuevo no duplica entradas. |
| Comprobación del borde inferior | Al ampliar la prueba a la última tarjeta se detectó un salto de 6 px (`4501 → 4495`) debido a alturas diferentes de barra superior durante la transición. Se sustituyó la barra artesanal del listado por la misma `TopAppBar` Material del detalle. La prueba conserva el caso de la última tarjeta. |
| Cambio de sección durante reproducción | Libera reproducción; volver a Biblioteca restaura la lista y el offset sin reabrir el detalle abandonado. |
| Recorrido adicional en MainActivity del emulador | Biblioteca → sesión previa → Atrás mediante `input keyevent 4`; Sesiones visible y MainActivity todavía en primer plano. Captura real de la flecha en `screenshots/sesion-navegacion-emulador.png`; la sesión mostrada contiene silencio. |

Las pruebas nuevas usan `ComponentActivity`, un Application con directorio `navigation-tests` y preferencias `navigation-tests-local_data`, y limpian solo esos datos propios. No alteran la Biblioteca existente. La clase nueva exige hardware de emulador y los comandos siempre especificaron `-s emulator-5554`; no se usó `connectedDebugAndroidTest`.

APK de esta corrección: `app/build/outputs/apk/debug/app-debug.apk`, 13.742.147 bytes, SHA-256 `6C4125780B91E1A9EF6C534D2F34629038DE674809047FA183A80F04E83B0DA2`. Sigue identificado como `2.1.0-beta1`/3. Pendiente: validar manualmente el gesto predictivo Atrás en el Samsung y la reproducción audible; las pruebas cubren la tecla Atrás Android y el estado real de MediaPlayer en emulador.

Instalación en Samsung con la autorización de actualización `install -r` ya otorgada por el usuario: primero se comprobó que Didge Taller no estuviera en primer plano. `adb -s R5CT21KTQ8W install -r app/build/outputs/apk/debug/app-debug.apk` respondió `Success`. El SHA-256 del `base.apk` instalado coincidió con el indicado arriba y el checksum de `shared_prefs/local_data.xml` fue idéntico antes y después (`LOCAL_DATA_CHECKSUM_UNCHANGED=True`). No se lanzó instrumentación ni se borraron datos en el teléfono; la instalación no constituye validación manual del gesto ni del audio.

## 20/10/2026 — Flujo Medir y sesiones (iteración actual)

Entorno: Windows 11, JBR 21.0.11 (`JAVA_HOME`), Android SDK Platform 35 y Build Tools 35.0.0. ADB mostró **Samsung R5CT21KTQ8W** y **emulator-5554**; la validación instrumentada y los recorridos anteriores se hicieron explícitamente en `emulator-5554`. La instalación autorizada posterior en Samsung se registra abajo.

| Verificación | Evidencia | Alcance |
| --- | --- | --- |
| Línea base antes de modificar: `gradlew.bat :app:testDebugUnitTest --console=plain` | `BUILD SUCCESSFUL in 30s`; 7 pruebas JVM previas | Núcleo anterior |
| Línea base antes de modificar: `gradlew.bat :app:assembleDebug --console=plain` | `BUILD SUCCESSFUL in 8s` | APK anterior |
| APK final: `gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest --console=plain` | `BUILD SUCCESSFUL in 49s`; 10/10 pruebas JVM, APK de app y test, versión `2.1.0-beta1` / código 3 | Compilación y pruebas JVM; no valida sonido real |
| Instrumentación con `adb -s emulator-5554 shell am instrument -w ar.didgetaller.test/androidx.test.runner.AndroidJUnitRunner` | `OK (4 tests)` en 1,348 s | Persistencia aislada de JSON antiguo, WAV/sesión nueva, WAV antiguo, WAV huérfano recuperado y reproducción/pausa/reinicio de WAV sintético; **solo emulador** |
| Recorrido manual en emulador: Medir → iniciar → silencio 14 s → detener → Biblioteca → detalle | Sesión privada `0a231c90-614a-484b-93a6-821fcf266856.wav`, 1.269.804 bytes, duración 14.396 ms; índice JSON con `timeline` de silencios y sin `summary`; UI indicó nota no determinada | Captura y guardado de una sesión sin señal; no hay nota física ni audio audible |
| Detalle: reproducir/pausar | El control cambió a Pausar y volvió a Reproducir; se vio contador/cursor por estado | No se verificó sonido audible ni sincronía exacta |
| Cierre automático a 60 s | La UI volvió a Iniciar; WAV privado de **5.292.044 bytes**, tamaño exacto de 60 s a 44,1 kHz/PCM16 + 44 bytes | Silencio del emulador; no instrumento |
| Cierre al salir | Al tocar Inicio durante una sesión, `onStop` generó WAV privado de **1.081.388 bytes**; la app volvió a reposo al reabrir | Salida normal; no cubre cierre forzado del proceso |
| Persistencia y UX final | Tras `am force-stop` y reapertura, la sesión previa seguía en Biblioteca; Vista amplia mantuvo el botón fijo; a 900×1800 y fuente 1,3× Iniciar/Detener siguió visible | Comprobación visual del emulador; no TalkBack |
| Renombrar, exportar y borrar sesión | Se añadió `_PVC` al nombre; SAF guardó copia de **1.081.388 bytes** en Descargas. Tras confirmar Borrar, desapareció el WAV privado y la copia exportada quedó presente | Recorrido de UI solo en emulador |

Capturas reales del emulador en [`screenshots/`](screenshots/): `medir-reposo-emulador.png`, `medir-vista-amplia-emulador.png`, `medir-pantalla-chica-emulador.png`, `medir-activa-emulador.png`, `sesiones-emulador.png` y `sesion-detalle-emulador.png`. Son capturas de interfaz sobre silencio; ninguna representa una nota medida. El resumen de sesión se verificó con señales sintéticas en JVM y persistencia Android, pero no con un didgeridoo real.

En la primera ejecución de las pruebas instrumentadas tras añadir recuperación de WAV, una aserción falló porque asumía que el registro legado aparecería primero en la lista; la lista se ordena por fecha y había archivos de prueba anteriores. Se corrigió la aserción para buscar por ID y se aislaron/limpiaron solo los archivos y preferencias propios de las pruebas. La compilación final y la segunda ejecución instrumentada pasaron. La recuperación no rellena datos de nota/frecuencia ausentes.

Tras esa compilación se abrió `ar.didgetaller/.MainActivity` con `adb -s emulator-5554 shell am start`; el proceso siguió activo (`pidof ar.didgetaller`). La búsqueda de los rótulos antiguos «Afinar», «Guardar medición», «Grabar audio» y «Activar micrófono» en el código visible no devolvió coincidencias.

Pendiente: accesibilidad/TalkBack, rotación, audio audible, nota física contra afinador independiente, estabilidad con señal real y sincronía exacta del cursor durante reproducción. El cursor está implementado usando la posición de MediaPlayer y los tiempos de lectura, pero solo se inspeccionó el estado del control sobre silencio. No correr `connectedDebugAndroidTest`, `pm clear`, desinstalación, reinstalación ni pruebas instrumentadas en el Samsung sin autorización explícita. Usar el equipo físico solo para pruebas manuales no destructivas.

APK final: `app/build/outputs/apk/debug/app-debug.apk`, 10.553.108 bytes, SHA-256 `73E1B02F43751F6C09AB5F7BB3AEE60BB6998F5D5469135DFFB61C3A08468D0D`. El emulador se restauró a 1080×2400 y escala de fuente 1,0 al terminar.

### Instalación autorizada en Samsung — 20/10/2026

El usuario proporcionó expresamente el comando `adb install -r` para el APK debug. Había dos dispositivos conectados, por lo que se añadió `-s R5CT21KTQ8W` para seleccionar el Samsung. Antes tenía `versionCode=2`, `versionName=2.0.0-beta1`. `adb -s R5CT21KTQ8W install -r .\app\build\outputs\apk\debug\app-debug.apk` respondió `Success`; `dumpsys package ar.didgetaller` confirmó `versionCode=3`, `versionName=2.1.0-beta1`. El primer `am start` arrancó un proceso que dejó de figurar poco después; no se observó excepción fatal en el registro inspeccionado, por lo que la causa no se puede atribuir. Se repitió `am start -W`: `Status: ok`, `LaunchState: COLD`, `TotalTime: 631 ms`; a los 2 s la actividad estaba en primer plano y el proceso activo. No se usó `pm clear`, desinstalación ni instrumentación en el Samsung. La instalación y el arranque del proceso no verifican audio real, datos previos ni reproducción audible.

## 20/10/2026 — Rediseño y validación parcial

Entorno Windows 11: Temurin JDK 17.0.20.1; Android SDK Platform 35 y Build Tools 35.0.0 presentes. AGP 8.13.2, Gradle 8.13, Kotlin 2.2.21; no se cambiaron algoritmos `PitchDetector`, `AcousticModel`, `Music` ni `BoreOptimizer`.

| Acción | Resultado | Alcance |
| --- | --- | --- |
| Línea base `:app:testDebugUnitTest --console=plain` | `BUILD SUCCESSFUL in 43s`, siete tests existentes, todos `UP-TO-DATE` | Suite JVM previa, antes de editar |
| Línea base `:app:assembleDebug --console=plain` | `BUILD SUCCESSFUL in 3s` | APK previo |
| Rediseño `:app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest --console=plain` | `BUILD SUCCESSFUL in 27s`, siete tests JVM aprobados, APK app y test generados | Compilación y pruebas locales, no interacción |
| Primer `:app:connectedDebugAndroidTest --console=plain` | Dos fallas: `FileNotFoundException` en directorio privado de `ar.didgetaller.test` y `SharedPreferences.commit()` fallido; el proceso instrumentado no tenía permiso de escritura allí | Error del aislamiento de pruebas, corregido usando contexto de la app con nombres de prueba separados |
| Segundo `:app:connectedDebugAndroidTest --console=plain` | `BUILD SUCCESSFUL in 13s`; 2/2 pruebas en Samsung SM-G990E, Android 16 | Verificó lectura de JSON antiguo de mediciones/diseños, guardado/borrado y persistencia/renombrado/borrado de WAV aislados; **no probó audio capturado ni la UI** |
| `:app:installDebug --console=plain` | `BUILD SUCCESSFUL in 5s`; APK instalado en SM-G990E | Instalación, no prueba de uso |

**Incidencia grave de conservación de datos.** Después de `connectedDebugAndroidTest`, `adb shell pm list packages ar.didgetaller` no devolvió la app y `adb shell am start -n ar.didgetaller/.MainActivity` devolvió `Error type 3`. Tras instalarla otra vez, `run-as ar.didgetaller ls shared_prefs` devolvió “No such file or directory”; `run-as` mostró solo `cache` y `code_cache`. El usuario confirmó que en este mismo dispositivo estaban guardadas las lecturas 66,9 y 66,1 Hz. Una búsqueda de archivos cuyo nombre contiene “didge” en `Download` y `Documents` no encontró exportaciones. Por lo observado, la tarea instrumentada quitó la instalación y se perdieron los datos privados anteriores. **No repetir `connectedDebugAndroidTest` en el teléfono del usuario.** El usuario eligió **no reconstruir** las lecturas. No se escribieron registros sustitutos ni se inventaron fechas.

El Samsung estaba bloqueado al intentar abrir la app; `uiautomator dump` mostró `com.android.systemui`, por lo que no hay capturas ni validación de navegación del rediseño en ese equipo. Sus permisos reales, micrófono físico, estabilidad, WAV acústico, reproducción audible, exportación, rotación, TalkBack y precisión siguen pendientes. El usuario informó antes que había instalado una versión anterior, probado afinador y guardado mediciones C2 66,9 y 66,1 Hz; esto es evidencia comunicada por el usuario, no una prueba realizada aquí.

### Emulador Android aislado — 20/10/2026

AVD `Medium_Phone_API_37.0`, 1080×2400, densidad 420; también 900×1800 con escala de fuente 1,3. Se instaló el APK con `adb -s emulator-5554 install -r`; no se volvió a usar `connectedDebugAndroidTest` en el Samsung. Capturas reales, sin maquetas, en [`screenshots/`](screenshots/).

| Escenario en el emulador | Evidencia observada |
| --- | --- |
| Navegación principal | Afinar, Laboratorio y Biblioteca visibles y accesibles; pestañas internas Diseñar, Espectro, Buscar, Guía y categorías de Biblioteca observadas mediante `uiautomator dump`. |
| Permiso denegado | Diálogo Android visible; al denegar, Afinar mostró “Permiso denegado” y mantuvo Guardar medición desactivado. |
| Permiso concedido y silencio | Al permitir, Afinar mostró “Escuchando · sin señal”, sin frecuencia ni nota inventada. Tras `am force-stop` y reapertura, la escucha optativa se reanudó. |
| Grabación | Se pulsó Grabar y se observó contador; al detener, apareció “Grabación guardada en Biblioteca”. Archivo privado `files/recordings/<UUID>.wav` de 1.425.452 bytes; en Biblioteca figura 16 s y 1,4 MB. El audio era silencio/entrada del emulador, sin evaluación musical. |
| Reproducción y pausa | Al pulsar Reproducir, el control cambió a Pausar; al pulsar Pausar volvió a Reproducir. No se evaluó sonido audible porque el emulador se inició sin audio. |
| Renombrar y persistir | Se añadió `_PVC` al nombre; tras `am force-stop` y reapertura, el WAV y el nombre siguieron presentes. |
| Exportar WAV | El selector `ACTION_CREATE_DOCUMENT` guardó el WAV en Descargas del emulador; archivo de 1.425.452 bytes y mensaje “Archivo exportado”. Android sustituyó `/` y `:` del nombre propuesto por `_`. |
| Eliminar WAV | Apareció diálogo de confirmación; al confirmar, Biblioteca quedó vacía y `files/recordings` sin el archivo privado. La copia exportada se conserva. |
| Optimización UI | Buscar mostró “Generación 6 / 16”, terminó con candidato y error ponderado 92,4 cents (simulación), y ofreció aplicar el perfil al diseñador. No es una medición física. |
| Pantalla pequeña / letra grande | A 900×1800 y fuente 1,3× se observó que el control de escucha quedaba inicialmente fuera de vista; se movieron Activar/Detener escucha y Guardar medición a una franja fija y se compactó el estado sin señal. La captura posterior muestra ambos controles visibles. |

**Límites de estas pruebas:** no se tocó un didgeridoo, no se comprobó frecuencia real ni calibración, ni sonido de reproducción, TalkBack, dispositivos de otras marcas o pérdida de recursos durante uso prolongado. Las pruebas de persistencia instrumentadas corrieron antes del último ajuste visual; el ajuste visual pasó las siete pruebas JVM y compilación final. Las capturas de emulador no sustituyen una captura del Samsung desbloqueado.

Tras fijar controles y reiniciar el desplazamiento, `:app:testDebugUnitTest :app:assembleDebug --console=plain` terminó `BUILD SUCCESSFUL in 4s`; ese APK se recorrió en emulador. Luego se quitó un límite heredado que descartaba mediciones al superar 200 y diseños al superar 100 entradas. La compilación y siete pruebas JVM finales terminaron `BUILD SUCCESSFUL in 2s`. **APK actual:** `app/build/outputs/apk/debug/app-debug.apk`, 10.485.510 bytes, SHA-256 `DFF0B2B2871392844580B2C54BCC060D2B7762F84D27DA72FB02BF043F5C2742`. El último cambio de retención no se repitió como prueba instrumentada.

El APK actual se instaló en el Samsung con `adb -s R5CT21KTQ8W install -r ...` (`Success`). La versión inmediatamente anterior se recorrió en emulador y generó las capturas (su único cambio posterior fue quitar el límite de retención). El Samsung seguía bloqueado; la instalación no prueba la interfaz ni el audio real. El usuario pidió dejar de solicitar permiso en cada comando ADB; no se hicieron más inspecciones ADB después de instalar el APK actual.

## Comprobado en el entorno de construcción inicial (registro histórico)

- `kotlinc app/src/main/java/ar/didgetaller/core/*.kt docs/PURE_JVM_CHECK.kt -include-runtime -d /tmp/didge-core.jar` — OK, Kotlin 1.9.0 + OpenJDK 21.
- `java -jar /tmp/didge-core.jar` — OK: 50 Hz → 50.00 Hz; 65.4 Hz → 65.40 Hz; 73.416 Hz → 73.42 Hz; 82.407 Hz → 82.41 Hz; 98 Hz → 98.00 Hz; 125 Hz → 125.01 Hz; confianza ≈1 en sonidos sintéticos limpios.
- Simulador, preset cilindro 1500×40 mm — pico inicial 57 Hz y series de resonancias estimadas en frecuencias superiores. No calibrado en un instrumento real.
- Encoder WAV RIFF: cabecera/longitud verificadas sobre muestras sintéticas.
- `./gradlew ...` — **NO ejecutado**: falta Android SDK/entorno Gradle completo, no se dispone de acceso a descargar dependencias en el contenedor.
- Android Studio Sync/APK/UI/emulador/micrófono: **NO verificados**.

## Verificación Windows del 19/10/2026

Entorno: Windows 11; `JAVA_HOME` temporal apuntando a Temurin JDK 17.0.20.1. Android SDK en `C:\Users\Raclima\AppData\Local\Android\Sdk`, con Platform 35 (`android.jar`), Build Tools 35.0.0 (`aapt2.exe`) y Platform Tools (`adb.exe`). El JBR de Android Studio y el Java predeterminado son 25; se eligió JDK 17 para ejecutar Gradle. El proyecto compilado usa AGP 8.13.2, Kotlin 2.2.21, Compose BOM 2025.04.01 y distribución Gradle 8.13. Esto difiere de los números 8.9.2/8.11.1 indicados en `AGENTS.md`; no se cambiaron versiones porque la compilación funcionó. El script de arranque descargó el JAR del wrapper de Gradle 8.11.1 y verificó su SHA-256 `2db75c40782f5e8ba1fc278a5574bab070adccb2d21ca5a6e5ed840888448046`; ese JAR arrancó la distribución 8.13 declarada en `gradle-wrapper.properties`.

| Fecha | Entorno/dispositivo | Comando o escenario | Resultado exacto | Evidencia/error |
| --- | --- | --- | --- | --- |
| 19/10/2026 | Windows 11, JDK 17, SDK 35 | `gradlew.bat :app:testDebugUnitTest --console=plain` | `BUILD SUCCESSFUL in 1m 30s`; 7 pruebas, 0 fallas, 0 errores, 0 omitidas | `app/build/test-results/testDebugUnitTest/TEST-ar.didgetaller.core.CoreTest.xml` |
| 19/10/2026 | Windows 11, JDK 17, SDK 35 | `gradlew.bat :app:assembleDebug --console=plain` | `BUILD SUCCESSFUL in 39s`; APK de 10.418.846 bytes | `app/build/outputs/apk/debug/app-debug.apk`; SHA-256 `E253B1AA42527112F4A1DF42B3E9EEB94FC761E3BAC053E2A104686D774D9C42` |
| 19/10/2026 | Windows 11, JDK 17, SDK 35 | `gradlew.bat :app:test --console=plain` | `BUILD SUCCESSFUL in 21s`; 7 pruebas debug y 7 release, todas aprobadas | Informes XML en `app/build/test-results/testDebugUnitTest/` y `testReleaseUnitTest/` |
| 19/10/2026 | APK generado | `aapt.exe dump badging app-debug.apk` | Paquete `ar.didgetaller`, `compileSdk 35`, `minSdk 26`, `targetSdk 35`, actividad `ar.didgetaller.MainActivity` | Lectura del APK empaquetado |
| 19/10/2026 | APK generado | `aapt.exe dump permissions app-debug.apk` | `RECORD_AUDIO` declarado; no se declaró `INTERNET` | Lectura de permisos del APK empaquetado |
| 19/10/2026 | ADB | `adb.exe devices` | Lista vacía: ningún Android conectado | No se instaló ni ejecutó el APK |
| pendiente | Android físico o emulador | Permisos, micrófono, WAV, diseñador, CSV y navegación | No probado | Requiere dispositivo; pedir autorización antes de instalar en uno físico |

Incidencias observadas: el primer intento de `testDebugUnitTest` no pudo descargar el wrapper (`Invoke-WebRequest: Unable to connect to the remote server`); con acceso de red autorizado descargó y verificó el JAR. El primer intento de `assembleDebug` dentro del sandbox no pudo crear `C:\.gradle\wrapper\dists\...\gradle-8.13-bin.zip.lck`; fuera del sandbox terminó correctamente. `adb devices` dentro del sandbox no pudo crear `\.android`; fuera de él mostró la lista vacía. Gradle avisó que `kotlinOptions.jvmTarget` está obsoleto y que `libandroidx.graphics.path.so` se empaquetó sin quitar símbolos; ninguna advertencia impidió la compilación.

Alcance: se verificaron compilación, pruebas unitarias locales y metadatos del APK. El repositorio solo contiene `CoreTest.kt` como suite de pruebas; no hay pruebas instrumentadas en `androidTest`. No se verificaron Android Studio Sync por separado, interfaz en dispositivo, permisos reales, captura de audio, exportación de archivos ni precisión acústica física.

No informar “listo, compila” hasta que ambas tareas Gradle hayan terminado correctamente. No inventar APK si no existe.

## 19/10/2026 — Funciones inspiradas en DidgeLab (sin código copiado)
- Kotlin JVM: `kotlinc core/Music.kt core/AcousticModel.kt core/BoreOptimizer.kt docs/LAB_JVM_CHECK.kt -include-runtime -d /tmp/check.jar` -> OK.
- `java -jar /tmp/check.jar` -> OK (simulación PVC 1500 x 40 mm: base ~56.8 Hz coherente con cuarto de onda; cono 1500 mm ~81.4 Hz; perfiles por puntos; cancelación; búsqueda 8 generaciones x 10 individuos redujo score de 265.7 a 40.9 cents).
- Pruebas Android/Gradle/Compose y micrófono físico: NO ejecutadas aquí por falta de SDK.
- Motor 1D: solo aproximación, sin validación cruzada todavía con DidgeLab ni instrumentos reales.

## Instrumented-suite remediation — 2026-10-25

Starting point: clean local `278944d`, one commit ahead of `origin/main`. The
previous application-wide audit and its visual matrix were retained; this pass
addressed the nine failures from its 100-test emulator run. No production code,
pitch estimator, private WAV, saved session, or Samsung data was changed.

The failures were test-contract and fixture defects introduced by the current
saved-session policy: the chart may display fresh YIN decisions from the WAV,
while the original captured readings remain preserved for diagnostics. Several
older tests calculated expected viewports from the historical series after the
fresh replay had loaded. Their WAVs also omitted events asserted by the tests.
The review of each failing case was:

| Test | Original failure | Verified correction |
| --- | --- | --- |
| `FundamentalPrecisionUiTest.shortTootAndFluteRemainVisible` | The stored series included a ~206 Hz toot and flute melody, but the WAVs contained constant tones. | Deterministic PCM now contains the toot and flute transitions; both remain visible. |
| `FundamentalPrecisionUiTest.reportedScenarioCardDetailAndFullscreen` | Equated a library card based on stored readings with a detail chart using current WAV replay. | Assert the original low reading remains in the card and storage, and current accepted replay points reflect the audible drone. |
| `FundamentalPrecisionUiTest.originalInspectionFittedPinchAndReset` | Computed expected camera from stored readings while the plotted replay and component layer were loading. | Wait for both analyses and compare reset/fitted behavior against the active scale sources; assert the historical 60 Hz reading is still stored. |
| `GraphIntelligenceUiTest.uncertainRawPeriodRemainsInspectableAndZeroAxisHasNoMusicalName` | Expected a stored 42.9 Hz candidate to appear in a chart replaying a 129 Hz WAV. | Assert it is absent from the current chart but preserved in storage and visible in Advanced diagnostics; retain zero-axis note-label checks. |
| `GraphIntelligenceUiTest.generalFittedExtremeZoomResetAndGraphKindsStayCoherent` | Used the historical series for an exact viewport equality after replay/component completion. | Compare against the active replay-plus-component scale; retain fitted, zoom, reset and spectrogram checks. |
| `PrecisionUiTest.canonicalPitchAndBothScalesAndCompleteInspectionRemainAvailable` | Required object identity between the historical timeline and current replay. | Assert the chart uses current replay, the original timeline is unchanged, the audible drone is accepted and silent PCM yields rejected frames. |
| `RecoveryUiTest.allSurfacesRenderVariationAndPeriodicityWithOriginalDataIntact` | Required stable/moderate/review chart colors from a silent WAV with only fabricated stored pitch changes. | A deterministic WAV now contains measured steady, varying, review and silent sections; the pixel-color and original-data assertions remain. |
| `SessionNavigationTest.immersiveViewportPinchInspectionResetAndBackPreserveSession` | Calculated the initial viewport from the historical timeline before the current replay settled. | Wait for replay and calculate from the active plotted points; retain pinch, inspection, reset, follow and Back checks. |
| `SessionNavigationTest.accessibleSeekUsesTheSamePlayerAndChartClockAcrossTabs` | Compared the returned chart camera to a viewport calculated from stored readings. | Wait for replay and compare against the active source; retain the player, seek, clock, chart-switching and WAV-integrity checks. |

The test-only WAV generator emits phase-continuous, known-frequency PCM. These
fixtures test UI source selection, chart scaling and presentation; they do not
establish physical pitch accuracy for real instruments. The original
`checkpoints/experiments/ui-audit-2026-10-24/final-emulator-suite.log` remains
available locally as the failing baseline. No private fixture was staged.

Validation on API 37 `emulator-5554`:

- The focused nine-test rerun had four residual assertions tied to scale-source
  timing and the edge of a silent analysis window. After waiting for both
  on-demand analyses and using the actual PCM window timing, a focused five-test
  rerun passed.
- The **complete** instrumentation command was
  `adb -s emulator-5554 shell am instrument -w
  ar.didgetaller.test/androidx.test.runner.AndroidJUnitRunner`:
  **103 tests, 0 failures**, 425.083 seconds in the first repaired-suite run. Log:
  `checkpoints/experiments/ui-audit-2026-10-24/full-emulator-suite-recheck.log`
  (ignored local evidence).
- After adding explicit Advanced-diagnostics provenance coverage, a full rerun
  reached 102/103: `SessionNavigationTest` assumed the transient Cancel button
  must be present after opening the tools sheet. The UI only shows this button
  while rendering, so the assertion was sensitive to completion timing. The
  test now checks cancellation if loading continues and otherwise requires the
  completed spectrogram with no error. The focused test passed. The failed
  rerun remains in ignored
  `checkpoints/experiments/ui-audit-2026-10-24/full-emulator-suite-final.log`.
- The next full run reached 102/103: the externally injected Android edge
  gesture in `PrecisionUiTest.rotationAndEdgeBackPreserveCaptureButBackgroundStillSavesOnce`
  was not observed within five seconds. The same case passed in the earlier
  full run and in a focused rerun. The test now waits for Activity focus after
  rotation and permits one further edge gesture while still requiring the
  original recording state and correct Back destination. The complete rerun
  after an emulator-only reboot is recorded in the ignored
  `checkpoints/experiments/ui-audit-2026-10-24/full-emulator-suite-after-reboot.log`.
- That post-reboot run aborted during `PrecisionUiTest` with Android's
  `FinalizerWatchdogDaemon`: `HardwareRenderer$DestroyContextRunnable` timed
  out. A focused rerun of the edge-Back test passed. A subsequent complete run
  with `PrecisionUiTest` first timed out waiting for Activity focus after a
  chart-picker action (`PrecisionUiTest.pick`, line 71); it was stopped after
  this failure. Its ignored log is `full-emulator-suite-rotation-first.log` in
  the same directory. These are unresolved emulator/UI-integration failures;
  the exact cause of the focus timeout has not been established. **There is no
  passing complete run of the final test APK at this checkpoint.** No push was made.
- `./gradlew.bat :app:testDebugUnitTest :app:jacocoDebugUnitTestReport detekt
  :app:assembleDebug --console=plain` succeeded. The Gradle tasks were mostly
  up to date after the earlier audit; JaCoCo and Detekt reports remain generated.
  Detekt is configured as informational (`ignoreFailures`), so this result is
  task completion rather than a claim of zero findings. Log:
  `checkpoints/experiments/ui-audit-2026-10-24/gradle-gates-recheck.log`.

The full visual/accessibility matrix belongs to the earlier audit above; it
was not repeated for this fixture-only correction. No physical Samsung command
was issued. Audible playback precision and real-instrument pitch accuracy remain
outside what these synthetic emulator tests demonstrate.

## Final UI test synchronization and adaptive icon — 2026-10-25

This continuation retained the earlier audit and fixture repairs. The Android
test helpers now wait for the selected chart and the closed selector menu
instead of treating transient Activity focus as chart readiness. The immersive
Back test uses Espresso for deterministic system Back; a separate test still
covers the injected edge gesture. The recording-wide toggle uses its labelled
accessibility bounds, and the session test waits for window focus before
injecting Back. Cancellation remains asserted while analysis is active; if the
analysis has completed, the test requires the completed result instead of
trying to press a button that has correctly disappeared. No production audio
algorithm or persisted measurement changed.

The old launcher drawable was an opaque square bitmap inside the launcher
mask. The manifest now references an adaptive icon with a teal background,
separate didgeridoo foreground and monochrome layer. `ic_splash_didge.xml` and
the Android 12 splash theme remain unchanged. The old bitmap was copied into
ignored local evidence before removal. Emulator screenshots in
`checkpoints/experiments/ui-audit-2026-10-24/` show the icon before and after
in the launcher and the preserved splash: `icon-before-launcher.png`,
`icon-after-launcher.png`, and `splash-after-350.png`. These are Pixel Launcher
screenshots, not Samsung One UI verification.

`./gradlew.bat :app:testDebugUnitTest :app:jacocoDebugUnitTestReport detekt
:app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest --console=plain`
completed successfully after correcting four integer format specifiers
(`%s` to `%d` in both locales) and two misplaced indentation lines. The JVM
XML reports contain 179 tests: 171 passed, 8 skipped, 0 failed. Lint reports
0 errors, 42 warnings and 10 hints. Detekt is informational because its Gradle
configuration ignores findings; JaCoCo XML and HTML were generated. Build log:
`checkpoints/experiments/ui-audit-2026-10-24/final-gradle-gates-after-lint-fix.log`.

One complete API 37 host-GPU emulator run passed 103/103 after the icon and
test-helper edits (`full-host-final.log`), before the string-format and final
selector/focus corrections. Subsequent full runs exposed chart-selector and
window-focus timing in tests, which were corrected and confirmed in focused
runs. A later complete run progressed through SessionNavigation and UIAudit,
then Android `system_server` Watchdog killed the WindowManager after a 67-second
lock; `ActivityScenario.close` could no longer reach DESTROYED. The crash log is
`final-system-watchdog.txt`. An earlier emulator native crash occurred in
`libEGL_emulation.so` on RenderThread. These logs do not establish an app
rendering defect. A cold-booted full final-state run with host GPU completed
**103/103, 0 failures**, 374.056 seconds; see `full-host-after-cold-boot.log`.
The emulator was explicitly selected as `emulator-5554` for every
instrumentation command. The physical Samsung was not accessed during this
local validation.

The CI-equivalent `./gradlew.bat --no-daemon detektComplexity --console=plain`
completed successfully, analyzing 55 Kotlin files; log:
`checkpoints/experiments/ui-audit-2026-10-24/detekt-complexity-final.log`.
`aapt2 dump badging` confirmed the debug APK declares package
`ar.didgetaller`, version `3.2.6-beta1` / code 12, and the adaptive
`res/mipmap-anydpi-v26/ic_launcher.xml` icon. Final emulator screenshots were
pulled into ignored `final-evidence/` and visually inspected: measurement,
active wide capture in landscape, session chart, spectrogram, and legend.
The synthetic chart shows independent fundamental/component labels and a real
gap; no source values were altered by this UI continuation. Manual TalkBack
speech/focus order and Samsung One UI launcher masking remain unverified.

## Codex workflow instructions — 2026-10-26

This instruction-only change replaced the 227-line root `AGENTS.md` with a
39-line active policy and reduced the repository-local `ub-mode` skill from
185 to 27 lines. Historical release notes remain in Git history and the
existing validation/work logs. The current instructions select checks by task
risk, reuse valid evidence, and reserve full emulator suites for relevant broad
or release-critical changes. `.github/workflows/android.yml` compiles the
instrumentation APK but does not run instrumentation tests.

Validation: reviewed the two instruction diffs and staged paths; checked
Markdown links, skill front matter, required safety/Git rules, and
`git diff --check`. No application code, Android resources, or CI workflow
changed. No JVM, emulator, or APK build was run for this instruction-only edit.
Instruction size was measured in lines; token savings and future tool-call
reductions have not been measured in real Codex sessions.

## Saved-session UX polish — 2026-10-26

The session now opens with playback and the musician-facing drone chart. Four
visible chart choices wrap on narrow screens; choosing Harmonics brings its
plot into view. Scales/ranges, selection/excerpts, markers, and advanced
diagnostics live in a scrollable Tools sheet. The diagnostic chart has its own
YIN/SWIPE' presentation and state; enabling comparison no longer changes the
ordinary preview curve or camera. Advanced comparison still opens in the
existing full-screen viewer, with playback position retained on return. The
drone axis names the supported spectral component in Hz without implying a
confirmed fundamental. Harmonics shows
frequency (Hz) and relative level (dB); the YIN heading also states Hz.

Validation on the final UI source: `:app:testDebugUnitTest` (183 tests, 8
skipped, 0 failures), `:app:assembleDebug`, and
`:app:assembleDebugAndroidTest` passed. `python tools/check_localization.py`
passed with 552 strings in English and Spanish. `detekt` and
`detektComplexity` passed on 56 Kotlin files. Logs are under ignored
`checkpoints/session-ux-*.log`. Focused emulator instrumentation passed:
6 navigation/rotation tests and 9 chart/diagnostic/evidence tests on the
changed workflows, plus the screenshot scenario in default portrait, compact
360 dp with 1.5x font, landscape, Spanish, and Spanish compact with 1.5x
font. The historical trace/recovery scenario passed after adapting its heading
lookup to the shorter Audio label. An earlier evidence capture failed because
a modal sheet creates a second Compose root; native-window captures now pass.
An earlier compact chart selection failed because the horizontally scrolling
tab hid Harmonics; wrapped chart choices resolved it.
The focused full-screen comparison and playback-return test passed after the
final viewer adjustment (`checkpoints/session-ux-diagnostic-viewer.log`).

Rendered emulator screenshots were opened and inspected for the session,
Harmonics, Tools, and Advanced Diagnostics in portrait, landscape, compact,
English, and Spanish. Private synthetic screenshots are in ignored
`checkpoints/session-ux-*.png`; none are included in Git. The spectrum axes,
playback controls, and diagnostic labels remained legible in the inspected
images. The ordinary chart and separate YIN chart retained different meanings.
No full instrumentation suite or physical acoustic validation was run for this
focused UI change. CI compiles Android instrumentation tests but does not run
them. Samsung installation and post-install integrity are separate pending
steps after the push and CI result.

## Compact saved-session chart tabs — 2026-10-26

Restored the Material 3 scrollable tab row in place of wrapping filter chips.
The tab row stays on one line and scrolls to the selected chart.
`:app:testDebugUnitTest`, `:app:assembleDebug`, and
`:app:assembleDebugAndroidTest` passed. Emulator-only focused navigation and
playback tests passed, including a 360 dp Spanish layout with 1.5x font.
Rendered English portrait, Spanish compact, and verified landscape screenshots
were inspected under ignored `checkpoints/compact-tabs-evidence/`. No Samsung
test or installation was performed for this change.
