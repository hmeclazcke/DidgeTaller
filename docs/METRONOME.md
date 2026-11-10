# Practice metronome

The expandable panel in Measure supplies a visual rhythm without opening another
destination. Tempo is 30–240 BPM; meters are 2/4, 3/4 and 4/4. Each quarter-note
beat has one, two, three or four pulses. Only the first subdivision of the first
beat receives the configurable accent. Sound defaults off; click gain is separate
from the system media volume. Start/resume, pause and stop have distinct behavior.
Pause retains the next pulse; stop resets the bar. Runtime state never autostarts
after recreation or process restart. Settings use a separate preference file.

## Timing and ownership

`core/Metronome.kt` owns an integer rational grid, transport state and one pending
callback. Deadlines are calculated from a monotonic origin, not from the delivery
time of the preceding callback. Late delivery skips missed subdivisions rather
than producing a burst. Tempo changes invalidate old callbacks and place the next
pulse one new interval ahead, preserving the pulse ordinal; changing meter or
subdivision restarts the bar on that next interval. Pause, stop and close revoke
callback ownership. Close is permanent and idempotent.

The Android adapter uses uptime and an audio-priority `HandlerThread`, created
only when needed. It sleeps until each absolute deadline; there is no polling.
Visual snapshots are posted to the main thread and stale snapshots are rejected.
Two reusable 12 ms static PCM clicks use `AudioTrack`. PCM is generated once per
owner, not per beat. Pause/stop release tracks and audio focus. ViewModel disposal
also closes the queue. Navigation out of Measure and Activity backgrounding pause
the transport; returning does not restart it. Audio-focus loss pauses it as well.
These are drift controls, not a hard-real-time/sample-accurate audio guarantee:
Android dispatch, cold audio startup and speaker latency still apply.

## Microphone isolation

`TallerViewModel.microphoneStart` admits a mute operation synchronously before
opening `AudioRecord`. The same engine monitor serializes tick effects and this
admission, so a concurrent tick cannot reopen an output after the mute returns.
Recording/monitor state keeps that interlock active through capture/finalization.
The sound control is disabled while blocked and settings cannot bypass it.
Visual pulses continue. Leaving Measure pauses the metronome before saved-session
playback/analysis. The metronome never writes audio, session indexes or WAV files.

## Verification and measurement limits

`MetronomeTest` characterizes rational deadlines, late/early delivery, 10,000
delayed callbacks, meters/subdivisions, accents, transport, duplicate/stale events,
microphone admission, failed outputs and invalid inputs. `MetronomeUiTest` uses a
clean emulator, isolated preferences/files, real Android click/microphone admission,
lifecycle/navigation and persistence tests. The normal Compose click/scroll tests
exercise Stop while the rhythm is running. The existing emulator workflow also
runs the metronome interaction in English and Spanish on a compact viewport at
150% system font, retains screenshots and restores emulator configuration.

Reproducible commands (canonical quality compilation):

```powershell
.\gradlew.bat -Pkotlin.incremental=false :app:testDebugUnitTest --tests ar.didgetaller.core.MetronomeTest
.\gradlew.bat -Pkotlin.incremental=false :app:qualityMutationMetronome
```

The selected PIT scope is the engine, grid, settings, focus owner and foreground
screen policy, also included in the
existing manual domain mutation job. It does not measure Android audio hardware or
all application code. Existing source/coverage/CRAP-L gates remain unchanged.
Compose transformations and constructors have explicit source-bound technical
reviews rather than invented source-level coverage. Android framework adapters
are verified through native tests; zero JVM execution coverage is kept visible
and is not replaced with Android coverage in the JVM report.

No physical-device test or installation is part of this feature delivery. Existing
acoustic equations, WAV/session formats and private recordings remain unchanged.

## Source-bound review decisions

The strict JVM ratchet originally rejected branching audio-focus policy embedded in
the Android adapter. That policy is now `FocusedMetronomeClick`, tested with granted,
denied, lost and stale focus leases, and included in PIT. Preference normalization
and foreground screen ownership are also pure tested policies. New Android methods
retain actual zero JVM coverage; none needs a new above-target numerical exception.

The remaining new unscored declarations are constructors, initializers, accessors
and Compose transformations. Their exact semantic hashes are reviewed in the
existing source-review file. They are not silently omitted or assigned fake scores.
The recording observer is an existing anonymous-owner mapping limitation; its
added interlock is covered by the native microphone admission test.

Three existing host methods need renewed source-bound reviews because their bodies
changed: `MainActivity.onCreate` (MCC 6, JVM CRAP-L 42),
`TallerViewModel.onDestinationChanged` (MCC 6, JVM CRAP-L 42) and
`onAppStopped` (MCC 4, JVM CRAP-L 20). The first reduces its previous complexity;
the other two add an unconditional transport pause without new branches. Their
existing numeric debt ceilings are not increased. Native navigation, background
and ViewModel disposal regressions protect those Android boundaries. All new
ordinary functions still have to pass the unchanged numerical/source gates.
