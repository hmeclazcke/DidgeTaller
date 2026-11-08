# Didge Taller engineering rebuild — IN PROGRESS

This is an isolated reconstruction on `refactor/uncle-bob-rebuild`, starting at main `3a160043f9ebb3a420b1d89a9cf4ddfe7d1b5d67`. It is not a completed whole-application rebuild, a certification, or a replacement for main. The original worktree and user data remain untouched. The original MIT-licensed ub-mode is unchanged; project-specific Kotlin execution remains separate.

## Why the first boundaries changed

The original 59 Kotlin files contain framework-independent DSP, but the largest detector mixes PCM preprocessing, visual summaries, lag-window policy, CMNDF search, subharmonic refinement, support-energy validation and acceptance. PitchTrace mixes neighborhood evidence, omission handling and connection state. SessionPlayer combines Android effects with preparation, playback intent, asynchronous seeks, error ownership and looping. The much larger ViewModel and Compose screens still need their own reconstruction cycles.

The original eight-function CRAP allowlist hid most of that picture. Before modifying production, a compiler-PSI inventory and conservative bytecode correspondence measured 276 original functions. That exposed 97 JVM-based high scores rather than the three visible through the allowlist. Many Android adapters have zero JVM coverage. Expanded visibility is not claimed as a production improvement or confused with Android coverage.

## Decisions implemented in the first production milestone

### Separate period evidence from display processing

Original [PitchDetector](https://github.com/hmeclazcke/DidgeTaller/blob/3a160043f9ebb3a420b1d89a9cf4ddfe7d1b5d67/app/src/main/java/ar/didgetaller/core/PitchDetector.kt) had one 43-MCC analysis function. The rebuilt [detector](../../app/src/main/java/ar/didgetaller/core/PitchDetector.kt) delegates comparison-window preparation, the difference curve, period search/refinement and acceptance to [YinPeriodSearch](../../app/src/main/java/ar/didgetaller/core/YinPeriodSearch.kt):

```kotlin
val search = YinPeriodSearch.prepare(pcm, mean, sampleRate, minHz..maxHz)
val result = search.evaluate(pcm, rms, minHz..maxHz, observe != null)
```

The actual implementation retains the unavailable-result branches. The division follows signal-processing responsibilities rather than arbitrary line counts. Downsampling's boundary guard has one definition. Thresholds, arithmetic order, visual outputs, diagnostic codes and confidence semantics remain unchanged. The public function measures MCC 9; every extracted search function is below 10.

### Give trace connection state one owner

[PitchTrace](../../app/src/main/java/ar/didgetaller/core/PitchTrace.kt) now creates one builder for one input sequence. Only that builder writes previous-index and omitted-count state. Neighborhood evidence, alternative-period spans, omission bridging and unsupported register sweeps are distinct decisions. Original points and historical reason values remain intact.

The original build function's MCC 33 becomes a one-step entry point, but the real work is not hidden: the builder's append measures 7, connection policy 6, unsupported-sweep policy 7, and alternative-span scan 9. The isolated-event classifier decreases from 15 to 7; its measured-return evidence has MCC 8. All PitchTrace functions are below 10 under the unchanged Detekt configuration.

### Put playback decisions behind real platform ports

[PlaybackController](../../app/src/main/java/ar/didgetaller/core/PlaybackController.kt) owns stopped/preparing/ready states and a typed in-flight/latest seek pair. [SessionPlayer](../../app/src/main/java/ar/didgetaller/audio/SessionPlayer.kt) retains its UI-facing API and supplies MediaPlayer and Handler adapters. Typed failures leave the domain; existing localized message formatting belongs to the Android boundary.

This is a useful inversion, not a repository/interface layer for every function. Deterministic backends and schedulers exercise real user outcomes without mocking Android internals. The tests cover preparation, selected starting time, paused/playing seeks, coalescing, loops, end tolerance, errors, duplicate callbacks, disposal and old-player events. A failing disposal regression demonstrated a poll being scheduled after a prepared listener stopped playback; ownership is now checked after publishing that event.

Intentional deviations from demonstrated original defects:

- A released player's error callback cannot stop its replacement. Original seek/prepared callbacks checked player identity; its error callback did not.
- Opening another recording does not inherit the previous recording's pending position.
- Pending positions cannot overflow the public integer audio clock before preparation.
- Nonfinite detections cannot become a display state. A red LivePitch test reproduced positive infinity escaping into the original state; finite accepted readings and all confidence thresholds remain unchanged.

## Characterization and differential evidence

### Worker-owned recording milestone

The original MicrophoneEngine guarded reads with AudioRecord identity but finalized an engine-global writer. A retiring monitoring worker could therefore close a newly attached recording. A controlled emulator regression invoked the old worker's delayed finalization boundary after attaching the next writer; appending the next sample failed because that writer had been closed. This is a demonstrated ownership bug, not a proposed acoustic change. The boundary is controlled without opening a microphone; the separate timeout/discard integration test uses the emulator AudioRecord.

[RecordingCapture](../../app/src/main/java/ar/didgetaller/core/RecordingCapture.kt) now owns one worker's monitoring/recording/finished state, writer, sample limit, discard claim and typed terminal outcome. MicrophoneEngine passes that same owner through append and finally; it never looks up a replacement's writer. State claims are synchronized; file finalization happens after the exactly-once terminal claim. Retiring the device is synchronized with start/stop, and queued device errors check capture ownership before reaching the UI. Existing UUID.part/discard names, PCM format, finalization/recovery policy and callback contracts remain intact.

Fifteen JVM cases cover ownership, exact one- and two-second limits, empty chunks, invalid inputs, repeated completion, close/open errors and save/discard races. All twelve RecordingHandoffTest cases pass on the clean API 37 emulator, including saved index/WAV integrity, failed close and discard, recovery, ViewModel disposal, actual timeout and the new delayed-finalizer case. The tests now inject one capture boundary rather than mutating independent recording/writer flags. Windows can reject renaming an open RandomAccessFile; portable JVM assertions check the corresponding recoverable failure instead of pretending it succeeded. Android separately proves the successful rename/cleanup path.

The capture production methods have MCC/CRAP-L 3, 3, 5.019 and 6 with JVM method lines 100%, 100%, 90.91% and 100%. Android adapter readLoop still has MCC 18 (original 20) and JVM-only CRAP-L 342 (original 420); this remains visible unfinished adapter debt, not an under-10 claim. Explicit reviewed signature lineage preserves the original ceiling without allowing regressions or erasing that debt.

The complete local suites now discover 476 cases across debug/release: 460 pass, 16 protected skips, no failures. JVM lines/branches are 31.38%/28.62%; core 98.47%/78.58%, with changed source denominators. Mapping accounts for all 444 named declarations: 329 measured, 40 nested in parent metrics, 14 without bodies, 4 anonymous-owner and 57 Compose declarations. Detekt sees 400 named functions; 71 are unscored and 84 measured functions exceed target. Detekt remains 1,970 findings; Lint 0 errors/59 warnings/10 hints; CPD has no groups at 100 tokens. These numbers do not complete architecture acceptance.

An initial 18-class mutation run generated 1,197 mutants: 806 killed, 365 survived, 24 uncovered and 2 timed out (PIT detected 808/1,197, 67.50%; two minutes two seconds engine time). Capture contributed 51: 38 killed, 12 survived, 1 uncovered. Reviewing survivors exposed missing two-second multiplication and empty-chunk assertions. The strengthened focused capture run kills 40/51, with 10 survivors and 1 uncovered. Remaining file-claim/success-path mutations require Linux comparison: Windows's open-file rename rejection prevents those paths; one accepted-zero append boundary may be behavior-equivalent but has not been silently removed. Broad reruns and final survivor classification remain outstanding; do not combine independently timed reports into a fabricated whole-suite score.

### Concurrent derived-cache publication

The original AnalysisCache used the same `<key>.tmp` path for every writer. A deterministic emulator test held two DataOutputStreams open on that path, published the first and observed a mixed value: its first integer came from the other writer. This is a reproduced corruption race in disposable derived data, not corruption of original WAVs.

[AtomicCacheFile](../../app/src/main/java/ar/didgetaller/data/AtomicCacheFile.kt) gives each operation a unique same-directory temporary file, closes its stream before atomic replacement, and cleans only its own temporary path. No global serialization or growing lock registry is needed. Interrupted/failed writes leave the previous published value available. Cache formats, keys, versions, optional-failure semantics and size trimming remain unchanged. This is atomic visibility for regenerable data, not a durable recording transaction.

Six JVM tests prove complete replacement, old-value visibility during a write, failure/cancellation cleanup, failed publication and an explicitly interleaved two-writer contract. All 27 selected emulator persistence/capture tests pass, including the new concurrent-cache test. The free PIT DEFAULTS selection produces only one mutant for this adapter (cleanup removal), killed by the tests; that narrow result does not measure every file-system failure or platform method. The domain selection now includes 19 classes and still needs a current broad CI report. The helper has MCC/CRAP-L 1 with 100% JVM method lines. The final integrated local build/static/differential checks pass; whole JVM coverage is 31.47% lines/28.63% branches with changed denominators, and all 445 named declarations are classified (330 measured; 71 unscored of 401 Detekt functions).

An additional behavioral test protects Music.hzText's canonical US decimal format while the interface locale uses a comma, including invalid/nonfinite inputs. Production formatting remains unchanged. The final two JVM variants each discover 245 cases: 237 passed, 8 protected skips, no failures. With this additional assertion coverage, JVM lines/branches are 31.49%/28.73%, core 98.54%/78.88%; Music.hzText CRAP-L falls from 12 to 3 and the measured high count becomes 83. That specific reduction is added test coverage with unchanged production code, unlike structural denominator changes earlier in the rebuild.

### Verified remote and visual evidence

Code `3ec5fd2bdad58a87f4f616df3fac23d84bd391d0` subsequently passed [fast/differential CI](https://github.com/hmeclazcke/DidgeTaller/actions/runs/42384700836), [complete API 35 emulator CI](https://github.com/hmeclazcke/DidgeTaller/actions/runs/42384695004) and [19-class mutation CI](https://github.com/hmeclazcke/DidgeTaller/actions/runs/42384739383). Downloaded artifacts confirm 490 JVM cases (474 passed/16 protected skips), Android 129 discovered/119 passed/10 skips/0 failures and separate Android coverage 4,376/4,801 lines (91.15%), 3,900/5,715 branches (68.24%). Linux JVM coverage is 31.51%/28.84%, core 98.61%/79.18%; successful open-file rename covers extra capture paths relative to Windows. These platform measurements are not merged.

That mutation execution produced 1,198 mutants across 19 actual bytecode classes: 821 killed, 359 survived, 16 uncovered, 2 timed out. PIT detected 823/1,198 (68.70%); engine time 115 seconds. Capture contributes 47 killed/4 survived out of 51. Inspecting those survivors exposed a weakness in the portable tests: deriving expected rename capability from the production result could accept an always-failed discard. The strengthened tests independently probe the host filesystem with RandomAccessFile. A further limit/full-disk contract kills an otherwise surviving zero-sample append mutation without changing correct production logic. Full local suites now total 492 discovered/476 passed/16 protected skips; Windows focused capture is 41 killed/9 survived/1 uncovered. Remote results for these final test-only assertions must be recorded separately. No observed survivor was silently labeled equivalent or excluded.

Final implementation/test commit `d90926c81a9590ad7f67a8a235586f1aa0524e18` passed [fast/differential checks](https://github.com/hmeclazcke/DidgeTaller/actions/runs/42386035464), [complete API 35 instrumentation](https://github.com/hmeclazcke/DidgeTaller/actions/runs/42386030241) and [expanded mutations](https://github.com/hmeclazcke/DidgeTaller/actions/runs/42386032298). Downloaded artifacts confirm 492 JVM cases: 476 passed/16 protected skips; 129 Android cases: 119 passed/10 skips; no failures. Linux JVM lines/branches are 31.51%/28.84%, core 98.61%/79.18%; separate Android lines/branches 91.15%/68.10%. Instrumentation branch results vary slightly with executed asynchronous UI paths; they are informational and not merged with JVM coverage.

The final 19-class PIT run generates 1,198 mutants: 825 killed, 355 survived, 16 uncovered and 2 timed out; 827/1,198 = 69.03% PIT-detected, 109 seconds engine time. RecordingCapture improves from 47/51 to **51/51 killed on the same Linux platform, scope and operators** after those assertion changes, with unchanged production code. Windows remains 41/51 because open-file renaming fails there; that platform limit is visible. PlotTouch's existing 11/11 enforced gate also passes. A green broad mutation job means successful execution/reporting, not an enforced 69.03% whole-app threshold.

The same conservative mapper was executed against the exact downloaded d90926c Android JaCoCo XML: 330 functions measured, 71 unscored and 32 above CRAP-L target. This is a separate informational measurement, not merged coverage or an additional CI gate. For example, readLoop is JVM CRAP-L 342 (zero JVM method lines) versus Android 18.945 (85.71% method lines), while RecordingStore.list and requestMusicalTrace are each Android CRAP-L 16 with complete method lines. Those are genuine structural priorities despite integration coverage. Unsupported Compose methods remain unscored; their Detekt complexity is still visible.

The published code at `07c756c0055ff84c2600b6658b73111ab392fb4b` has successful [fast/differential CI](https://github.com/hmeclazcke/DidgeTaller/actions/runs/42368989332), [complete emulator instrumentation](https://github.com/hmeclazcke/DidgeTaller/actions/runs/42368900284), and [manual expanded mutations](https://github.com/hmeclazcke/DidgeTaller/actions/runs/42368987707). Downloaded report artifacts confirm both JVM variants 223 discovered/215 passed/8 skipped each, Android 127/117/10 with no failures, separate Android coverage 4,349/4,775 lines and 3,874/5,687 branches, and the exact 17-class 1,146-mutant distribution above. A duplicate fast push run was explicitly cancelled after the identical PR run started; it is not counted as a pass. Artifacts retain seven days; these links identify the source revision independently of later changes. Capture changes require their own subsequent CI result.

A newly created Didge_Rebuild_API37 emulator (1080x2400, density 420) uses only synthetic data. All 26 local screenshots were inspected: eleven English and eleven Spanish session/analysis/tools/fullscreen views plus four compact 1080x1920 portrait/landscape views at 150% font. The corresponding eight UI audit methods passed. Controls, horizontal tabs, scrolling and compact actions remain readable. Initial captures precede asynchronous canonical trace completion; later tools images show its supported near-70 Hz trace and real gap. The early unavailable overlay is not proof that the synthetic recording has no supported component; distinguishing loading from final absence remains a UI-state review item. Screenshot capture does not prove every UI state or device configuration. No physical phone or historical emulator fixture was accessed.

[DifferentialProbe](../../app/src/test/java/ar/didgetaller/core/DifferentialProbe.kt) runs unchanged against two isolated production classpaths: the pinned original JAR and rebuilt classes. The local oracle was compiled before production edits. CI regenerates it in a detached temporary worktree from the exact baseline commit.

The current contract compares **741 output lines byte-for-byte**: 128 synthetic PCM cases across frame sizes, low/flute frequencies, silence, harmonic interference and late attacks, including full readings/diagnostics and WAV hashes; 160 seeded trace/summary/range scenarios; five viewport coordinate cases. Equality passed after each DSP/trace restructuring. This protects those contracts, not all application behavior. JNI, persistence transactions and UI navigation require their separate integration evidence. No private audio is an input.

## Before/after scorecard — published 07c756c milestone, not final acceptance

| Category | Original main | Current local rebuild milestone | Interpretation |
| --- | --- | --- | --- |
| App JVM tests, both variants | 390 discovered / 374 passed / 16 skips | 446 discovered / 430 passed / 16 skips | 28 added cases per variant; private assumptions remain |
| Android suite | 127 discovered / 117 passed / 10 skips | 127 discovered / 117 passed / 10 skips | Complete real branch emulator run, not compilation |
| JVM production coverage | 28.19% lines / 25.69% branches | 30.66% / 27.84% | Code and denominator changed; generated-code exclusions unchanged |
| Pure core JVM coverage | 98.47% / 77.25% | 98.50% / 78.36% | Playback logic is now measurable on JVM; not whole-app coverage |
| Android coverage | 91.09% / 68.25% | 91.08% lines / 68.12% branches | Changed production denominators; always separate from JVM |
| Function measurement | 8 mapped / 329 unmapped of 337 | 324 mapped / 71 unscored of 395 Detekt functions | A different, broader measurement scope; 439 PSI named declarations remain accounted |
| Broader original CRAP-L baseline | 276 mapped, 97 high, measured before production edits | 324 mapped, 85 high | Existing hidden debt exposed; many high Android functions still need work |
| Known complex acoustic functions | 43 / 33 / 15 | 9 / entry-point 1 / 7; actual builder work <=9 | Responsibility separation plus differential protection |
| Highest UI complexity | PlotSurface MCC 139 | Still 139 | Unresolved; no completed architecture claim |
| Duplication | CPD: 0 groups at 100 tokens | 0 groups | Semantic duplication review remains necessary |
| Mutation scope | PlotTouchKt, 11/11 killed | 17 selected bytecode classes across DSP, music, trace, geometry, playback and WAV I/O | Scope expansion, not comparable whole-app scores |
| First expanded mutation execution | Not measured | 1,143 generated: 753 killed, 364 survived, 24 uncovered, 2 timed out | 755/1,143 PIT-detected score 66.05%; timeouts reported separately |
| Strengthened expanded execution | Not measured | 1,146 generated: 767 killed, 354 survived, 23 uncovered, 2 timed out | 769/1,146 = 67.10% detected; remaining survivors are unfinished work |
| Detekt, unchanged rules | 1,978 informational findings | 1,970 | Material complexity reduced; broad style/architecture debt remains |
| Android Lint | 0 errors / 60 warnings / 10 hints | 0 errors / 59 warnings / 10 hints | No rule category disabled; full accessibility review unfinished |
| Architecture | Shared platform/decision ownership | DSP stages, trace builder, pure playback controller | Capture, persistence, ViewModel and UI still need redesign cycles |
| CI | Fast + main emulator + manual narrow PIT | Branch-aware emulator, source inventory, differential checks and expanded manual PIT configured | Must inspect real branch runs before claiming passed |

The broad mutation analysis deliberately retains useful operators and reports Kotlin-generated artifacts. Unit-return/lambda/checkNotNull survivors may be equivalent, but are not automatically relabeled as killed. Tests were strengthened for loop restart call counts/actual stopped playback, region access, restart-from-stopped, WAV input rejection, flute endpoint/confidence acceptance and display smoothing versus stability. The expanded score is informational while survivors are reviewed; the original focused 100% threshold remains enforced. Both tasks require real nonempty reports.

## Controlled performance sample

`qualityPerformanceComparison` runs the same JVM harness in separate original/rebuilt processes, with 120 warmup frames and five batches of 50 frames per instrument. A Windows/JDK 21 sample, offline analysis without visuals:

| Profile | Original median time/frame | Rebuilt median time/frame | Original / rebuilt allocated bytes/frame |
| --- | ---: | ---: | ---: |
| Didgeridoo, 70 Hz | 0.591802 ms | 0.360060 ms | 35,536.48 / 35,744.48 |
| Flute, 440 Hz | 0.410282 ms | 0.254768 ms | 67,528.48 / 67,720.48 |

Checksums match. Refactoring adds a small allocation cost (208/192 bytes per frame) in this sample. These are controlled host observations, including identical reflection overhead, not Android real-time, battery, rendering or device-performance guarantees. No timing CI gate is inferred; wider performance/lifecycle evaluation remains open. The harness lives in engineering-tools because Android test compilation does not expose desktop JVM management APIs.

## Reproducibility and remaining work

See [quality commands](../quality/QUALITY_GUIDE.md), [commit-scoped baseline](REBUILD_BASELINE.md) and the [resumption checkpoint](REBUILD_PROGRESS.md). Raw reports/logs are ignored locally and nonprivate CI reports are uploaded with bounded retention. New high complexity, coverage regression and lost correspondences fail ratchets. The two removed private SessionPlayer methods have exact measured replacements; retirement is explicit and fails if the replacement is absent.

The API 37 native Back investigation is resolved as an environment failure. The hand-created AVD inherited hardware main keys, so its gesture handler was unattached even though the navigation-mode resource said gestural. After setting hw.mainKeys=no, cold booting without a data reset and recovering a host-memory-pressure System UI ANR, the unchanged original edge-Back scenario passed 1/1, retaining playback through fullscreen and releasing it on exit. A separate native MainActivity normal/wide/Back probe passed without starting recording. Five corrected software-key screenshots were inspected, including compact Spanish portrait/landscape at 150% font, whose two focused tests passed. No application navigation patch, longer timeout or key-Back substitution was retained. Earlier failed logs are preserved as invalid-environment evidence, not claimed application regressions.

**IN PROGRESS:** do not merge this draft as the completed rebuild. The [per-file responsibility review](REBUILD_REVIEW.md) covers all 63 runtime Kotlin files and the owned JNI boundary, but material redesign remains: capture device/error ownership, persistence-index corruption/recovery, ViewModel/UI separation, canonical loading/absence states, high-risk functions, 355 remaining mutation survivors/16 uncovered and final compatible-scope scorecard. The implemented milestones have passing d90926c remote evidence; this does not complete the whole application architecture. No Samsung access, private recording access, persisted-format change or metronome implementation occurred.

## Persistence and library reconstruction continuation

The [persistence design](PERSISTENCE_REBUILD.md) records three reproduced original defects and their new ownership boundaries. The work replaces production parsing/publication implementations rather than adding a JSON guard to the old destructive fallback. Unknown rows and fields survive unrelated edits; failed publication cannot authorize WAV deletion; explicit recovery preserves the damaged text. All instances serialize an entire read/modify/publish transaction through the preference file's process-local owner. This is not a cross-process or power-loss durability guarantee.

Library state now publishes once through a LibrarySnapshot rather than independently mutating sessions, designs, measurements, trash lists and their counter. LibraryCatalog owns the consistent Android read; the ViewModel no longer knows the preference-file key. The pure projection is behavior-tested and included in real mutations. Localized recovery presentation has its own composable, with state hoisted from the snapshot. A red Android case also reproduced metadata failure escaping onto the UI thread; the UI boundary now reports failure while preserving the damaged document and original audio. Broader ViewModel capture/analysis ownership remains unfinished.

Local expanded evidence: 548 JVM cases across both variants (532 pass, 16 protected skips), 361 reliable mappings / 76 unscored Detekt functions with all 481 PSI declarations classified, 79 high JVM CRAP-L scores, zero CPD groups, 1,966 Detekt findings and Lint 0/52/9. The seven new persistence/JSON/library mutation classes join the preceding 19: Windows generated 1,369 mutants (960 killed, 390 survived, 17 uncovered, 2 timeouts). Same-platform selected results and generated Kotlin checks must be reviewed individually; the broad detected score is informational. New domain coverage is not whole-application mutation coverage.

The 25 focused Android persistence cases pass. Later API 37 framework/System UI failures invalidate the local screenshot set and final library run, despite earlier passing Compose semantics. Screenshot capture now checks the actual active window, and fresh API 35 CI collects only synthetic library evidence. The exact final remote evidence belongs in the resumption checkpoint and draft PR; no unexecuted check is a pass. Production reconstruction remains IN PROGRESS, especially capture coordination, asynchronous ViewModel ownership, graph/rendering responsibilities and existing high-complexity domain decisions.

### A green UI suite did not prove localization

The first clean API 35 run at 22655bb passed all 144 cases (134 pass, 10 explicit skips), with separate Android coverage 91.44% lines / 68.75% branches. Human inspection of its seven authentic screenshots found the caller-locale fixture's Spanish warning paired with an English dialog. The existing assertions checked tags/clicks, not dialog text. A platform dialog installs its own Context; the fixture's Compose-local font override also did not configure that window.

Recovery content is now resolved in the caller's locale before creating the platform window, and the explanation can scroll on small screens. Assertions verify the actual translated title/action/explanation/cancel text. The fixture is explicitly named for caller-locale behavior, not physical system-font coverage. The emulator script additionally restarts the test process under actual Android application locales en/es, system font scale 1.5 and a 720x1280 display. It checks the activity/application configuration, retains separate test reports and restores settings even after failure. These two targeted runs do not overwrite or merge the full-suite coverage probes. Their outcome must be observed before claiming the new configuration gate passed. This is a concrete example of using visual verification to find a test blind spot rather than treating a green pipeline as completion.

### Verified outcome of the dialog follow-up

At 6c6f00d the fast gates and complete API 35 emulator workflow pass, including both additional real compact en/es 150%-system-font scenarios. Thirteen authentic PNGs were inspected. The dialog now follows the caller locale; in compact mode its explanation can scroll while the primary actions remain above Android navigation. All 71 runtime Kotlin source hashes match the final CI inventory. Final coverage is 33.69%/31.33% JVM and 91.41%/68.72% separate Android; 361 functions map, with 79 JVM and 30 Android above-target CRAP-L scores. RecordingStore.list MCC is 16 to 4, while the actual extracted codec methods remain visible and tested. Detekt is 1965; remote Lint 0 errors/53 warnings/9 hints. Dependency advisories vary with time, and changed denominators are not presented as direct acoustic improvement. The [checkpoint](REBUILD_PROGRESS.md) links successful runs and records exact remaining production work; the full rebuild remains IN PROGRESS.

## Capture terminal ownership and a single recording state

See [the capture-event design](CAPTURE_EVENT_OWNERSHIP.md) for source/test links. The prior writer boundary was correct but its untagged callbacks still crossed operation lifetimes. A deterministic real-Handler emulator regression first failed because discarding A stopped the displayed monitor B. The correction gives each worker an opaque identity and one terminal envelope; a duplicate finalizer cannot emit an empty outcome ahead of the valid one.

A pure RecordingCoordinator now owns phases, measured-point accumulation, immutable save snapshots, notice ownership, disposal and acknowledgement. The ViewModel supplies device/storage/localization effects and publishes one Compose snapshot. This replaces separate mutable pending timeline, capture profile and wall timestamp fields and independently written recording UI fields. It does not introduce a service locator, another audio engine or a new persistence format.

Mutation tests exposed an observer assertion that could pass while UI notifications were deleted, an unregistered-ready import boundary and a disposed-begin assertion that failed for the wrong reason. Tests now detect those defects. Focused Windows results are 89/94 handoff/coordinator mutants killed (five reviewed survivors, no uncovered); they are not a whole-application score. New raster boundary assertions improve its selected result from 25/31 to 31/31 without changing production behavior.

SpectrogramRaster separates Android bitmap allocation from power normalization, frequency orientation and palette conversion. Known powers verify actual ARGB anchors, explicit reference/range changes preserve measurement arrays, and cancellation stops obsolete column work. SessionWorkspace.render MCC decreases from 17 to 13 with unchanged rules. No timing or memory improvement is claimed; substantial workspace/rendering responsibilities remain.

Local combined evidence: 618 JVM discovered, 602 passed, 16 protected skips; JVM 35.46% line/32.67% branch, core 98.66%/80.09%; 393 measured functions/90 unscored, 78 JVM high scores, zero ratchet violations. Source denominators grew; these percentages are not an equal-scope comparison with the previous Linux milestone. Seven observer signatures and seven anonymous Android adapter methods explain the additional unscored declarations; every compiler-inventoried declaration remains classified. Detekt has 1,968 informational findings, Lint 0 errors/52 warnings/8 hints and CPD no >=100-token clones. This is not an overall static-debt cleanup claim.

The original 741 differential outputs still match. Existing Android WAV/index integrity assertions are retained and new delayed-ready tests require exactly one import with original profile, chronology and bytes. The post-correction local API 37 verification aborted before discovery; clean API 35 CI remains the framework gate pending publication. The rebuild remains IN PROGRESS, with the next executable task recorded in the progress checkpoint.


### Disposal and failed-start verification

The first actual API 35 run at de8df92 discovered/executed 147 cases: 136 passed, 10 skipped and one failed. The failing legacy assertion compared a null error with an empty message after disposal. The new workflow intentionally suppresses disposed-screen notices, so the test now separately requires visible nonblank failure for live owners and no notice after disposal. All WAV/index/marker assertions remain, and a queued raw device error is explicitly injected after clearing. CI correctly failed and stopped before extra font/locale checks; that run is not acceptance evidence.

The review also moved raw microphone errors through the coordinator's disposal/phase boundary, rather than leaving an unguarded ViewModel callback. A failed microphone start now resets stale listening state independently of error-callback side effects; a deterministic JVM regression first failed for this case. Corrected local inputs execute 622 JVM cases (606 pass, 16 protected skips), JVM coverage 35.53% line/32.73% branch, core 98.66%/80.13%; ratchets pass with 395 mapped/90 unscored. Focused ownership PIT kills 94/99, with the same five reviewed survivors; raster remains 31/31 with unchanged inputs. Clean full CI for these corrected inputs is required before the framework gate is closed.


## Playback commands own intent; observations do not

The next real API 35 failure concerned pause stability in a loop, not capture disposal. Four deterministic red tests exposed independent controller defects. The old decision `if (candidate.playing)` could interpret Pause as Resume during an asynchronous engine transition. A late completion could also replace a pending selected time with a loop start. See [the playback case](PLAYBACK_INTENT_OWNERSHIP.md) for exact contracts and Android API references.

The existing controller now changes its owned pause intent first and uses first-start state only to ensure legal platform commands. Initial selection and loop restart wait for the latest seek acknowledgement. Completion while paused/seeking/already resumed cannot restart audio. The same backend, graph cursor and player clock remain in use. The Android assertion retains its 80 ms bound and includes diagnostics; no timeout or tolerance was relaxed.

PIT found a fake scheduler retaining consumed tasks, which concealed a removed recurring poll. Correct task consumption and stronger session/preparation/loop assertions improve the selected two-class result from 104 killed/26 survivors of 130 to 125 killed/22 survivors of 147. Denominators differ because the controller now expresses more behavior; this is not a universal mutation score. All controller functions remain below MCC/CRAP-L 10, with method-line coverage explicitly distinct from branch coverage. Full framework CI remains required for the corrected real-player scenario.


### Accepted source-scoped verification

At 7aa7fcb, fast quality, real clean API 35 instrumentation and manual expanded PIT all passed. The suite executed 147 discovered cases (137 passed/10 skipped), and two additional EN/ES compact 150%-font executions passed. The original 80 ms loop-pause assertion passed without tolerance changes. All 13 supplied library/recovery PNGs were inspected; those images do not claim new graph visual coverage. See [the complete source-scoped scorecard](REBUILD_VERIFICATION_7aa7fcb.md) for exact JVM/Android coverage, mutation outcomes, unchanged official skill, artifact retention and remaining architectural work. Passing this milestone does not complete the application-wide reconstruction.
