# SWIPE′ comparison UI evidence

Captured on 2026-10-23, API 37 x86_64 emulator, 1080×2400, density 420.
No private audio, physical device or microphone input is shown.

- [Android fullscreen](android-fullscreen.png): actual TallerApp navigation and
  theme, five-second synthetic 70-Hz WAV, original synthetic YIN readings and the
  on-device SWIPE′ curve. Playback is active; the common blue cursor is visible.
- [Spanish controls](spanish-controls.png): isolated SessionExplorer UI harness
  using MaterialTheme's light palette, six-second synthetic 70-Hz/harmonic WAV
  with a deliberately silent interval. Both curves retain the pause. This capture
  verifies Spanish copy and controls; it is not the application's default theme.

These illustrate comparative estimates and interaction, not physical pitch
accuracy. English and Spanish tests also verified exact chart-pixel restoration
after disabling comparison. Numerical and timing evidence is recorded in
[VALIDATION.md](../../VALIDATION.md) and [SWIPE_COMPARISON.md](../../SWIPE_COMPARISON.md).
