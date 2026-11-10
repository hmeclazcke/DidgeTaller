# Practice metronome

The dedicated Metronome tab sits between Measure and Laboratory. Large tempo
controls, a tap-to-edit numeric dialog, visible meter and beat indicators keep
transport and sound within reach. Rhythm, accent and gain options expand only on
request. Narrow portrait uses one scrollable column; short landscape uses two. Tempo glyphs
fit their actual slot without disabling font scaling. Navigation keeps complete
accessible titles and chooses short captions from the actual width/text-scale
budget. Landscape practice omits the redundant toolbar; short viewports use a
compact navigation height. Start/stop precedes the secondary sound/pause row.

Tempo is 30-240 BPM; meters are 2/4, 3/4 and 4/4. Each quarter-note beat has one,
two, three or four pulses. Only the first subdivision of the first beat receives
the configurable accent. Sound defaults off; its visible switch distinguishes
requested sound from capture blocking, zero click gain, muted system media and
output failure. Hardware volume keys address media on this tab; the Android volume
panel is available when muted. The app never forces a system volume or audio route.

Start/resume, pause and stop have distinct behavior. Pause retains the next pulse;
stop resets the bar. Runtime state never autostarts after recreation or process
restart. Settings keep the same separate preference file and keys.

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
also closes the queue. Navigation out of the metronome and Activity backgrounding pause
the transport; returning does not restart it. Audio-focus loss pauses it as well.
These are drift controls, not a hard-real-time/sample-accurate audio guarantee:
Android dispatch, cold audio startup and speaker latency still apply.

## Microphone isolation

`TallerViewModel.microphoneStart` admits a mute operation synchronously before
opening `AudioRecord`. The same engine monitor serializes tick effects and this
admission, so a concurrent tick cannot reopen an output after the mute returns.
Recording/monitor state keeps that interlock active through capture/finalization.
The sound control is disabled while blocked and settings cannot bypass it.
Visual pulses continue. Changing sections pauses the metronome before saved-session
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

The selected PIT scope is the engine, grid, settings, focus owner, PCM generation,
sound-status policy and foreground screen policy. The original domain components are included in the
existing manual domain mutation job. It does not measure Android audio hardware or
all application code. Existing source/coverage/CRAP-L gates remain unchanged.
Compose transformations and constructors have explicit source-bound technical
reviews rather than invented source-level coverage. Android framework adapters
are verified through native tests; zero JVM execution coverage is kept visible
and is not replaced with Android coverage in the JVM report.

No physical-device test or installation is part of this feature delivery. Existing
acoustic equations, WAV/session formats and private recordings remain unchanged.

Transport actions clear tempo-editor focus, and the numeric keyboard offers Done.
This keeps the keyboard from covering the practice settings after stopping. The
native transport test asserts that focus was released before its screenshot; the
component-test host also respects system drawing insets.

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

## Audio evidence and limits of this correction

The previous Samsung verification left the intentional `audible=false` preference
in place. The old collapsed panel obscured this control, and media mute/zero gain
were not exposed. This establishes a silent configuration, not a proven Samsung
AudioTrack defect. The redesign retains the valid non-silent PCM waveform, extracts
it for RMS/peak tests and now checks gain acceptance and track playback state.
Native emulator tests additionally require all 576 frames of both accent and
normal clicks to be consumed, report the output device types, exercise media mute
and verify track release. They do not prove physical speaker audibility or latency.
A Samsung listening check still needs separate authorization; this task does not
access the phone, private WAVs, or session metadata.

UI regressions cover all four destinations and Back, tempo buttons/editor, meter,
subdivisions, sound interlock, pause/resume/stop and keyboard dismissal. The clean
emulator workflow captures the real app in English and Spanish at 150% font,
including short landscape, without opening private sessions.

For fast native diagnosis, dispatch the existing deep workflow with scope
`metronome`. It runs 11 selected native scenarios and three real font/locale
configurations, preserves failure screenshots and reports a focused result. It
never represents the complete suite or produces an invented Android coverage
measurement. Full acceptance still uses scope `instrumentation`.
