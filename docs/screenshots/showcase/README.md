# README showcase captures

These five PNGs are unaltered screenshots of Didge Taller running on the
`emulator-5554` Android emulator with the `81e33cb` debug APK. The emulator was
set to 1080 x 2400 pixels, density 420, font scale 1.0. Android reported the
app locale as `en`; the English labels were checked in the rendered images.
No Samsung data or private recordings were used.

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

`live-measurement.png` comes from
`RecoveryUiTest.controlledPcmReachesLiveMeterAndProfilesResetWithoutGraphFiltering`.
Deterministic 70 Hz PCM passes through `PitchStream` and the production live
reading callback. The test deliberately uses no physical microphone or WAV
writer. The image demonstrates the live display, not a physical measurement or
a completed recording.

The SVG mark is a direct transcription of the current Android adaptive icon's
foreground paths and launcher background color. It is artwork, not a screenshot.
Historical screenshots remain in their existing directories for dated evidence.
