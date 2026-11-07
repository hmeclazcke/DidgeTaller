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

The published code at `07c756c0055ff84c2600b6658b73111ab392fb4b` has successful [fast/differential CI](https://github.com/hmeclazcke/DidgeTaller/actions/runs/42368989332), [complete emulator instrumentation](https://github.com/hmeclazcke/DidgeTaller/actions/runs/42368900284), and [manual expanded mutations](https://github.com/hmeclazcke/DidgeTaller/actions/runs/42368987707). Downloaded report artifacts confirm both JVM variants 223 discovered/215 passed/8 skipped each, Android 127/117/10 with no failures, separate Android coverage 4,349/4,775 lines and 3,874/5,687 branches, and the exact 17-class 1,146-mutant distribution above. A duplicate fast push run was explicitly cancelled after the identical PR run started; it is not counted as a pass. Artifacts retain seven days; these links identify the source revision independently of later changes. Capture changes require their own subsequent CI result.

A newly created Didge_Rebuild_API37 emulator (1080x2400, density 420) uses only synthetic data. Local real screenshots were captured in English/Spanish, plus 1080x1920 portrait and landscape at 150% font. The corresponding eight UI audit methods passed. Inspected images show readable controls, intentional scrolling and accessible compact measurement actions; a canonical no-supported-reading state is shown honestly, without inventing a frequency. Screenshot capture does not prove every UI state or device configuration. No physical phone or historical emulator fixture was accessed.

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

**IN PROGRESS:** do not merge this draft as the completed rebuild. The [per-file responsibility review](REBUILD_REVIEW.md) covers all 63 runtime Kotlin files and the owned JNI boundary, but material redesign remains: capture device/error ownership, persistence-index corruption/recovery, ViewModel/UI separation, high-risk functions, mutation survivors and final compatible-scope scorecard. Per-worker writer ownership and concurrent-cache corruption are fixed; verified 07c756c CI is historical evidence for later changes. The native edge-Back test fails on the clean API 37 AVD for both rebuild and exact original 3a16004 APKs; readiness/longer-swipe/animation probes did not resolve it, their test edits were removed, and no key-event fallback hides the failure. Native MainActivity versus Compose test-clock behavior remains to be isolated. No Samsung access, private recording access, persisted-format change or metronome implementation occurred.
