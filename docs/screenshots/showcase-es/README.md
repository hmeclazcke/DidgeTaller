# Spanish README showcase captures

These five PNGs are unaltered screenshots of Didge Taller running on the
`emulator-5554` Android emulator with the `81e33cb` debug APK. The emulator
was set to 1080 x 2400 pixels, density 420, font scale 1.0. Android reported
the app locale as `es-AR`; the Spanish labels were checked in all five
rendered images. The previous emulator app locale (`en`) was restored after
capture. No Samsung data or private recordings were used.

`library.png`, `session-analysis.png`, `spectrogram.png`, and
`harmonic-analysis.png` use the same isolated 18-second mono PCM16 WAV as the
[English showcase](../showcase/README.md): five sustained tones (68, 71.2,
74, 69.4, and 72 Hz), harmonics, and a one-second silence. Its SHA-256 is
`e06d0cef9500cd80208de983913f5a7ae64a5dbd9dc5cceecad26e16bed85d09`.
`SessionEvidenceUiTest.readmeShowcaseFromSyntheticWav` saves the WAV through
`RecordingStore`, derives saved pitch readings with `PitchStream`, and lets
the real app calculate the drone trace, STFT spectrogram, and harmonic
analysis. The WAV remains in ignored local evidence and is not committed.

`live-measurement.png` comes from
`RecoveryUiTest.controlledPcmReachesLiveMeterAndProfilesResetWithoutGraphFiltering`.
Deterministic 70 Hz PCM passes through `PitchStream` and the production live
reading callback. This capture uses no physical microphone or WAV writer.
It demonstrates the live display, not a physical measurement or completed
recording.

The README reuses the existing SVG mark from the English showcase; it adds no
new application artwork. Historical screenshots remain in their existing
directories for dated evidence.
