# Current README showcase - en

## 2026-11-10 UI update

`live-measurement.png` and `metronome.png` are unaltered, visually inspected
captures from implementation `89cd201`, version `3.3.0-beta1` / code `13`.
[Passing API 35 Pixel 2 x86_64 run](https://github.com/hmeclazcke/DidgeTaller/actions/runs/42865033307),
artifact `Focused-Metronome-Results`, normal font scale, English locale.
The live reading uses controlled 70 Hz PCM through the production callback,
without a hardware microphone or WAV writer. Metronome is stopped after testing
its settings. Neither image contains private audio or comes from the Samsung.
The four saved-session images retain the historical provenance below.

| Current file | Dimensions | SHA-256 |
| --- | --- | --- |
| live-measurement.png | 1080 x 1920 | `d440046655e156519cdd2f5d64bccd0b89b26a0cee983f6f156a1fab8bbe22ff` |
| metronome.png | 1080 x 1920 | `07a1d7ee2b726dd0552db9f920f1f98decbd385398cf9254381cd1a9c1c5a20e` |

## Historical 2026-11-06 capture record

This record covers the unchanged session images and the previous live-reading
revision, which remains available in Git history.

Captured on 2026-11-06 from the real Android app on emulator-5554 (ranchu), based on production revision 0ed87d6. Production executable code and Android resources are unchanged; this task translates two comments and technical test messages and updates screenshot-only fixture preparation.

Captured debug APK: package `ar.didgetaller`, version `3.2.6-beta1` / code `12`, SHA-256 `8287b1f3357f4f03a88d2a1039509495c06c0c5a66193a8a032fd58208453fa9`.

Resolution: 1080 x 2400 px; density 420; font scale 1.0; app locale `en`. All five original PNGs were visually inspected independently. No retouching, overlays, generated UI, private recordings or Samsung access. Earlier originals remain available in Git history at 0ed87d6 and the dated evidence directories.

`library.png`, `session-analysis.png`, `spectrogram.png`, and
`harmonic-analysis.png` use an
isolated 18-second mono PCM16 WAV generated locally from five sustained tones
(68, 71.2, 74, 69.4, and 72 Hz), harmonics, and a one-second silence. The
synthetic WAV's SHA-256 is
`e06d0cef9500cd80208de983913f5a7ae64a5dbd9dc5cceecad26e16bed85d09`.
`SessionEvidenceUiTest.readmeShowcaseFromSyntheticWav` saves the WAV through
`RecordingStore`, derives its saved pitch readings with `PitchStream`, and lets
the real app calculate the drone trace, STFT spectrogram, and harmonic analysis.
The WAV remains in ignored local evidence and is not committed.

The selected session timestamp is 6.48 s; the measured supported component is 71.2 Hz. The test taps the actual plot, waits for the paused real player and checks cursor synchronization before capture. Demo names are localized fixture data, not translated user sessions. Spectrogram and harmonic peaks are calculated by the production pipeline from the same WAV.

`live-measurement.png` comes from
`RecoveryUiTest.controlledPcmReachesLiveMeterAndProfilesResetWithoutGraphFiltering`.
Deterministic 70 Hz PCM passes through `PitchStream` and the production live
reading callback. The test deliberately uses no physical microphone or WAV
writer. The image demonstrates the live display, not a physical measurement or
a completed recording.


## Original PNG integrity

| File | Dimensions | SHA-256 |
| --- | --- | --- |
| harmonic-analysis.png | 1080 x 2400 | `1bea5adb95f63a19b537d273ea653ae9158cfecb90ccd37e8c5591b1ff0769da` |
| library.png | 1080 x 2400 | `c1d598e47e8512396c188fb6232914b090002ed04fc537c2635a42e39b8d62b6` |
| live-measurement.png (previous revision) | 1080 x 2400 | `e1689cd0a4c8d7da9b9314cf144697165a12b8f17ad3671fdc364d6ab12e1de6` |
| session-analysis.png | 1080 x 2400 | `a93be741d5e59545c7d9393b12705a559e40e2f9ac6e72bebdacfe6d4517eb57` |
| spectrogram.png | 1080 x 2400 | `b84da7f1b9a6d620d96fb9b63177d605443b7aec9025d17933dc9370df425002` |

The SVG mark transcribes the current adaptive icon artwork; it is not a screenshot.
