# Playback intent, asynchronous seeks and completion ownership

The API 35 run [42421703832](https://github.com/hmeclazcke/DidgeTaller/actions/runs/42421703832), at `a7d2514`, executed 147 Android tests: 136 passed, 10 skipped and one failed. The remaining failure was a position change after pausing a loop in `SessionNavigationTest.explorerRendersRealGraphsSeeksPausesLoopsAndCompares`. The capture finalization/disposal cases passed. The failed run is not acceptance evidence.

## Reproduced controller defects

Investigation produced four deterministic JVM failures before changing production:

- A completion queued before Pause restarted a loop despite the paused UI state.
- A transient `playing=false` during seeking made Toggle resume instead of pause.
- Completion during a seek replaced the newest selected 17-second target with the 10-second loop start.
- Completion during a nonloop seek incorrectly changed the requested playing state to paused.

These independently reproduce real controller defects consistent with the integration failure. They do not prove which callback ordering occurred in the failed emulator run, which did not record the two positions. The integration assertion retains its 80 ms tolerance and now records positions, region and pause state on failure.

[Android documents](https://developer.android.com/reference/android/media/MediaPlayer.html) that the engine's playing-state observation can lag its commands, seeks complete asynchronously, and `pause()` is not legal in the Prepared state. A UI's requested pause therefore cannot be inferred from an instantaneous `isPlaying()` query.

## Existing controller, stronger responsibilities

[PlaybackController](../../app/src/main/java/ar/didgetaller/core/PlaybackController.kt) continues to own one backend and one authoritative timeline. No replacement player, library or parallel time store was introduced.

| Event | Policy |
| --- | --- |
| Toggle | Invert the owned playback intent. Pause a backend only after its first start; an initial seek can still be in Prepared state. |
| Preparation with a selected nonzero position | Send the seek and wait for its acknowledgement before starting audio. Preparation at zero can start directly. |
| Completion while paused, seeking, or already playing again | Ignore the obsolete completion; it cannot change a newer command. |
| Genuine completion with a loop | Seek to the loop start. Resume after acknowledgement only if the current intent still requests playback. |
| Seek acknowledgement | Send a newer queued target first. Once the latest target settles, preserve pause or resume playback. Reaching the end settles completion rather than restarting at zero. |
| Loop polling | Require both active playback intent and backend playback. A delayed platform pause observation cannot initiate another loop seek. |

`Ready.hasStarted` expresses platform command legality, independently of desired pause. All four combinations are meaningful: prepared/paused, prepared/awaiting seek with play requested, started/playing, and started/paused or completed. State is still written by the UI-thread controller. Effects, seek ownership and released-backend guards remain centralized.

An existing restart-at-end tolerance is preserved. A pending user seek takes priority over an old backend end position. The 39 controller contracts cover initial selection, completion ordering, delayed pause observations, session replacement, preparation, loop boundaries, disposal and errors. Two earlier tests were updated to require playback *after* acknowledgement, replacing their assumption of immediate start before an asynchronous seek completed.

## Mutation evidence

The previous same-class selection had 130 mutants, 104 killed and 26 surviving. The revised selection has 147 mutants; focused Windows execution killed 125, with 22 survivors and no uncovered mutants. These are different bytecode denominators, not an identical-population score comparison. They are not whole-application mutation results.

PIT exposed a scheduler fake that retained already-executed callbacks. It could conceal removal of recurring loop monitoring. `tick()` now consumes the callback before execution, and assertions require the next poll. Additional contracts protect valid zero-start loops, a selected loop when resuming at the end, changing sessions while preparing, and resource ownership after duplicate preparation. Eight initial surviving mutants were killed by these stronger assertions. Remaining Kotlin bridge/invariant-guard survivors stay visible; no blanket equivalent-mutant exclusion was added.

Reproduce with `./gradlew :app:qualityMutationPlayback`. Reports are in `app/build/reports/quality/pit-playback/`. The existing manual domain workflow already includes these two bytecode classes, so CI does not run a duplicate playback job. The score is informational; successful execution and a nonempty report are required.

## Local verification and remaining gate

Both JVM variants: 656 discovered, 640 passed, 16 protected skips, zero failures. JVM line/branch coverage is 35.67%/33.04%; core is 98.67%/80.49%. Coverage denominators expanded. Ratchets pass: 397 mapped functions, 90 unscored, 78 JVM high scores and no CPD clones of at least 100 tokens. Relevant controller MCC/CRAP-L pairs are preparation 5/5, completion 6/6, polling 9/9, toggle 7/7 and seek 7/7. Necessary state guards increase some complexities while keeping the controller functions below 10; they are not removed to improve metrics.

Debug/androidTest compilation, Lint (0 errors/52 warnings/8 hints), Detekt (1,970 informational findings), localization parity, 21 Python tests and 741 unchanged differential outputs pass locally. Those numerical contracts do not compare asynchronous Android effects. The full clean API 35 suite must validate the corrected real-player scenario before this framework gate is closed. Source hashes, final CI results and scope limitations belong in [the checkpoint](REBUILD_PROGRESS.md).

Session formats, original WAV bytes and acoustic algorithms are unchanged. No Samsung or private fixture was accessed. Whole-application reconstruction remains **IN PROGRESS**; SessionWorkspace, PlotSurface and remaining ViewModel/DSP risks still require their own cycles.
