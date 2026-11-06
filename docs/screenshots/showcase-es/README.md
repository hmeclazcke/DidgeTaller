# Current README showcase - es-AR

Captured on 2026-11-06 from the real Android app on emulator-5554 (ranchu), based on production revision 0ed87d6. Production executable code and Android resources are unchanged; this task translates two comments and technical test messages and updates screenshot-only fixture preparation.

Captured debug APK: package `ar.didgetaller`, version `3.2.6-beta1` / code `12`, SHA-256 `8287b1f3357f4f03a88d2a1039509495c06c0c5a66193a8a032fd58208453fa9`.

Resolution: 1080 x 2400 px; density 420; font scale 1.0; app locale `es-AR`. All five original PNGs were visually inspected independently. No retouching, overlays, generated UI, private recordings or Samsung access. Earlier originals remain available in Git history at 0ed87d6 and the dated evidence directories.

`library.png`, `session-analysis.png`, `spectrogram.png`, and
`harmonic-analysis.png` use the same isolated 18-second mono PCM16 WAV as the
[English showcase](../showcase/README.md): five sustained tones (68, 71.2,
74, 69.4, and 72 Hz), harmonics, and a one-second silence. Its SHA-256 is
`e06d0cef9500cd80208de983913f5a7ae64a5dbd9dc5cceecad26e16bed85d09`.
`SessionEvidenceUiTest.readmeShowcaseFromSyntheticWav` saves the WAV through
`RecordingStore`, derives saved pitch readings with `PitchStream`, and lets
the real app calculate the drone trace, STFT spectrogram, and harmonic
analysis. The WAV remains in ignored local evidence and is not committed.

The selected session timestamp is 6.48 s; the measured supported component is 71.2 Hz. The test taps the actual plot, waits for the paused real player and checks cursor synchronization before capture. Demo names are localized fixture data, not translated user sessions. Spectrogram and harmonic peaks are calculated by the production pipeline from the same WAV.

`live-measurement.png` comes from
`RecoveryUiTest.controlledPcmReachesLiveMeterAndProfilesResetWithoutGraphFiltering`.
Deterministic 70 Hz PCM passes through `PitchStream` and the production live
reading callback. This capture uses no physical microphone or WAV writer.
It demonstrates the live display, not a physical measurement or completed
recording.

The README reuses the existing SVG mark from the English showcase; it adds no
new application artwork. Historical screenshots remain in their existing
directories for dated evidence.

## Original PNG integrity

| File | Dimensions | SHA-256 |
| --- | --- | --- |
| harmonic-analysis.png | 1080 x 2400 | `7c82406aaf72b9a1fa052141a536e40bc7cde34f0eb59ac47a3b056150882dc3` |
| library.png | 1080 x 2400 | `966b76221b910cd7493216da1236683ed57077a2149afd6b363a914bcfe7f0b1` |
| live-measurement.png | 1080 x 2400 | `c9d70ee8fa0fb90ca53032f51f68b2117f3762606c65854598d04c8a2b346d57` |
| session-analysis.png | 1080 x 2400 | `028afa62e07d9b1ec6e5b83a33ef4cdec2ade2301cf4766b2dc028c270404f9c` |
| spectrogram.png | 1080 x 2400 | `522d0f862847698644c278b39b26b8ddecea0655dec3b4da46b8f3318597e9f0` |
