# Verified capture and playback continuation - IN PROGRESS

Runtime/test/configuration revision: `7aa7fcb82a64e316f1dc21a6e84c7b4c42532d7d`. Original main remains `3a160043f9ebb3a420b1d89a9cf4ddfe7d1b5d67`. Later documentation-only commits reuse this evidence only while those inputs are unchanged. PR #1 remains draft; no merge or physical-device installation occurred.

## Implemented production work

- `12dc104`: per-worker terminal envelopes, immutable capture publication and a pure recording workflow with one Compose snapshot.
- `de8df92`: cancellable spectral power-to-pixel preparation and expanded existing PIT selections.
- `a7d2514`: device-error lifecycle ownership, disposed-notice assertions and failed-start state correction.
- `7aa7fcb`: playback intent independent of asynchronous backend observations, valid first-start/pause commands, acknowledged initial/loop seeks and obsolete-completion rejection.

See [capture ownership](CAPTURE_EVENT_OWNERSHIP.md) and [playback intent](PLAYBACK_INTENT_OWNERSHIP.md) for defects, red tests, decisions and source links. These are production changes, not a claim that the whole application has been rebuilt.

## Actual remote executions

| Run | Revision | Execution and result |
| --- | --- | --- |
| [Fast quality 42469282408](https://github.com/hmeclazcke/DidgeTaller/actions/runs/42469282408) | 7aa7fcb | Passed: both JVM variants, builds, Lint, localization, differential contracts, JaCoCo, Detekt, inventory, CPD, regression gates and Python tooling tests. |
| [Full emulator 42469279440](https://github.com/hmeclazcke/DidgeTaller/actions/runs/42469279440) | 7aa7fcb | Passed: real API 35 x86_64 Pixel 2 instrumentation, separate coverage and extra EN/ES compact 150%-font executions. |
| [Expanded mutations 42469281768](https://github.com/hmeclazcke/DidgeTaller/actions/runs/42469281768) | 7aa7fcb | Passed: original 11/11 gesture gate and expanded 29-class PIT selection. Engine time 75 seconds; broad score informational. |

The duplicate automatic fast push was cancelled by concurrency, not counted as a pass. The emulator push intentionally skips its manual mutation job; the mutation dispatch intentionally skips instrumentation. Required script failures propagate to failed jobs. No required check uses `continue-on-error`.

Earlier failures remain evidence, not passes: [42419882993](https://github.com/hmeclazcke/DidgeTaller/actions/runs/42419882993) exposed the obsolete post-disposal equality; [42421703832](https://github.com/hmeclazcke/DidgeTaller/actions/runs/42421703832) failed real loop pause stability. The live/disposed assertions were strengthened without dropping WAV/index checks. The pause-position assertion still uses 80 ms; no timeout/tolerance was relaxed.

## Before/after scorecard

The prior checkpoint is the recorded persistence/library milestone (6c6f00d; its selected PIT inputs were unchanged from 22655bb). It was not rerun unnecessarily. Both percentage columns below use Linux CI, but production/bytecode denominators changed after extraction and new logic. JVM and Android coverage are separate measurements and are never merged.

| Check | Prior checkpoint | 7aa7fcb | Interpretation |
| --- | --- | --- | --- |
| JVM debug + release | 548 discovered / 532 pass / 16 skip | 656 / 640 / 16; 0 failures | 328 discovered, 320 pass, 8 protected skips per variant. |
| Android complete suite | 144 discovered / 134 pass / 10 skip | 147 / 137 / 10; 0 failures | Actual execution, not compilation. Seven private guards and three configuration-dependent skips remain. |
| Additional configurations | EN/ES focused evidence | 1 pass each, 0 failures | The same recovery case repeats at 720x1280 and actual platform fontScale 1.5; not two additional unique scenarios. |
| JVM debug line / branch | 33.69% / 31.33% | 35.69% / 33.31% | Expanded source; does not prove equal-denominator improvement. |
| Core JVM line / branch | 98.61% / 79.23% | 98.73% / 80.77% | JVM effects/logic view only. |
| Android line / branch | 91.41% / 68.72% | 91.56% / 68.46% | Separate emulator view; branch percentage decreased with changed denominator. |
| CRAP-L mapped / unscored | 361 / 76 | 397 / 90 | Fourteen added unscored entries are seven observer declarations and seven anonymous Android adapters. No reviewed mapping was lost. |
| JVM / Android scores >=10 | 79 / 30 | 78 / 30 | Broad debt remains. Android scores are separately mapped, informational. |
| PIT selected scope | 26 bytecode classes | 29 | Not whole-application mutation testing. |
| PIT raw outcomes | 1,369: 970 killed, 381 survived, 16 uncovered, 2 timeouts | 1,519: 1,119 killed, 382 survived, 16 uncovered, 2 timeouts | All outcomes retained; broad selection changed. |
| PIT detected score | 972/1,369 = 71.00% | 1,121/1,519 = 73.80% | PIT includes timeouts as detected; they are not relabeled killed. |
| Same playback class selection | 104 killed / 26 survived of 130 | 125 / 22 of 147 | More behavior and stronger assertions, not an identical mutant population. |
| CPD | 0 groups >=100 tokens | 0 | No new clone regression; not proof of perfect semantic DRY. |
| Detekt | 1,965 informational | 1,970 | Same rules; no global warning cleanup claimed. |
| Lint | 0 errors / 53 warnings / 9 hints | 0 / 52 / 8 | Dependency advisories can change independently of production edits. |
| Differential contracts | 741 identical outputs | 741 identical outputs | Synthetic DSP/WAV/geometry; asynchronous effect corrections have independent contracts. |

The compiler-PSI inventory accounts for all 531 named declarations: 397 measured, 40 nested with parent metrics, 23 bodyless, 13 anonymous-owner and 58 Compose-transformed. The Detekt function-observation count is 487; it is not the same denominator as all PSI declarations. CRAP-L uses matched function-specific method-line coverage. No class coverage substitution, merged JVM/Android score or official Crapper claim is made.

Relevant completed work: SessionWorkspace.render MCC 17 to 13; MicrophoneEngine.finishRecording now MCC 2; recording completion MCC/CRAP-L 8; raster 7; playback completion 6, poll 9, toggle 7. Higher necessary state checks were retained instead of gaming complexity. Global JVM hotspots still include microphone readLoop 18/342, requestMusicalTrace 16/272 and workspace render 13/182 (MCC/CRAP-L); their Android scores differ. PlotSurface MCC 139 remains unscored by the Compose correspondence and requires architectural work.

## Evidence, visuals and limits

Downloaded artifacts are ignored under `checkpoints/github-ci/`; GitHub artifact retention is seven days (current artifacts expire 2026-11-15). Available artifacts include Quality-Metrics-and-Test-Results, JVM coverage, Detekt reports, Complete-Emulator-Results and Kotlin-Mutations. Reproduce with the commands in [the quality guide](../quality/QUALITY_GUIDE.md); generated reports are not committed as source.

All 13 actual API 35 library/recovery PNGs were inspected, including caller-locale EN/ES dialogs and real platform EN/ES 150%-font configurations. Original PNGs are retained unmodified; contact sheets were only an ignored review aid. Dialog actions remain above three-button navigation; long explanations have vertical scrolling. No System UI obstruction was accepted. These are library/recovery images, not new playback graph screenshots. Native playback/graph behavior is supported by executed instrumentation, not fabricated visual evidence.

The original API 37 regression actually failed before correction. Later local API 37 verification aborted before discovering tests and is not counted as a pass; clean API 35 CI provides the final framework evidence. No physical screen timeout, microphone accuracy, device universality or new performance benchmark is claimed.

Both README languages render on the feature branch and match their Git blob SHAs. Spanish resources remain Spanish; localization parity passes (574 strings, 5 plurals, 2 arrays). The official ub-mode skill remains byte-identical to pinned upstream 3da62bf3fcfd2034de41f30d920d2739e566171f, with its MIT license intact. No main, Samsung, private WAV, persistence-format or acoustic-threshold changes were made. No metronome was implemented.

## Still IN PROGRESS

Next production cycle: controlled SessionWorkspace loading/cache/publication with deterministic open/render/cancel/session-switch and late-progress tests; then separate PlotSurface decisions and remaining ViewModel analysis/navigation ownership. Selected preparing-state playback guards also need targeted survivor review; surviving guards are not automatically declared equivalent. Other acoustic hotspots, remaining broad mutants, wider UI/device/error/lifecycle and measured performance evidence still prevent whole-application acceptance. Keep PR #1 in draft and do not merge.
