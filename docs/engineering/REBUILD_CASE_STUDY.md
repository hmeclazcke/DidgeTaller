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

[DifferentialProbe](../../app/src/test/java/ar/didgetaller/core/DifferentialProbe.kt) runs unchanged against two isolated production classpaths: the pinned original JAR and rebuilt classes. The local oracle was compiled before production edits. CI regenerates it in a detached temporary worktree from the exact baseline commit.

The current contract compares **741 output lines byte-for-byte**: 128 synthetic PCM cases across frame sizes, low/flute frequencies, silence, harmonic interference and late attacks, including full readings/diagnostics and WAV hashes; 160 seeded trace/summary/range scenarios; five viewport coordinate cases. Equality passed after each DSP/trace restructuring. This protects those contracts, not all application behavior. JNI, persistence transactions and UI navigation require their separate integration evidence. No private audio is an input.

## Before/after scorecard — current milestone, not final acceptance

| Category | Original main | Current local rebuild milestone | Interpretation |
| --- | --- | --- | --- |
| App JVM tests, both variants | 390 discovered / 374 passed / 16 skips | 446 discovered / 430 passed / 16 skips | 28 added cases per variant; private assumptions remain |
| Android suite | 127 discovered / 117 passed / 10 skips | Await branch CI evidence | Compilation alone is not acceptance |
| JVM production coverage | 28.19% lines / 25.69% branches | 30.66% / 27.84% | Code and denominator changed; generated-code exclusions unchanged |
| Pure core JVM coverage | 98.47% / 77.25% | 98.50% / 78.36% | Playback logic is now measurable on JVM; not whole-app coverage |
| Android coverage | 91.09% / 68.25% | Await actual branch emulator probes | Always separate from JVM |
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

**IN PROGRESS:** do not merge this draft as the completed rebuild. Remaining work includes complete source review, capture-generation ownership and finalization characterization, persistence/cache recovery and concurrent writes, ViewModel/UI responsibility separation, every high-risk function, full skip/configuration review, remaining mutation survivors, actual branch Android evidence/screenshots and the final compatible-scope scorecard. No Samsung access, private recording access, persisted-format change or metronome implementation occurred.
