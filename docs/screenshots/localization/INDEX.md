# English and Spanish UI evidence

These are reviewed Compose captures from isolated synthetic emulator fixtures on
2026-10-22. No private recordings, historical sessions or Samsung data appear.
Spanish fixture names are intentional user content and remain unchanged in English.

The standard inventory uses Android's actual per-app locale (`en` / `es-AR`) on
API 37, 1080 x 2400 pixels, density 420, font scale 1.0 (about 411 dp wide).
The two narrow measurement captures use a 320 dp Compose configuration context
with font scale 1.3 and the longer flute label; they are not whole-device resizes.
Images are copied without editing. Scroll boundaries and horizontal tab overflow
indicate ordinary scrolling; long stored names retain the existing ellipsis policy.

| Screen | English | Spanish |
| --- | --- | --- |
| Measurement | [en](en-measure.png) | [es](es-measure.png) |
| Measurement, 320 dp / font 1.3 | [en](en-measure-320-font130.png) | [es](es-measure-320-font130.png) |
| Bore design | [en](en-design.png) | [es](es-design.png) |
| Guide | [en](en-guide.png) | [es](es-guide.png) |
| Session Library | [en](en-library.png) | [es](es-library.png) |
| WAV analysis | [en](en-analysis.png) | [es](es-analysis.png) |
| Comparison form | [en](en-comparison.png) | [es](es-comparison.png) |
| Rename dialog | [en](en-rename-dialog.png) | [es](es-rename-dialog.png) |
| Fundamental viewer | [en](en-fundamental.png) | [es](es-fundamental.png) |
| Spectrogram viewer | [en](en-spectrogram.png) | [es](es-spectrogram.png) |
| Waveform viewer | [en](en-waveform.png) | [es](es-waveform.png) |
| Harmonic spectrum viewer | [en](en-harmonics.png) | [es](es-harmonics.png) |

[Unsupported de-DE app locale](de-fallback-rename-dialog.png) displays the English
dialog and preserves its Spanish fixture name. The original empty app-locale
override was restored after the audit. This set contains 25 selected images.

Additional generated inventory captures (capture settings, lab home, search,
design Library, session detail, excerpt controls and empty Library) were inspected
locally. They are not all duplicated in this selected evidence set. Assertions
cover native chart semantics, decimal separators, measured axis bounds and
complete instrument text without ellipsis/height clipping. Float glyph bounds
allow one pixel for integer TextLayoutResult size rounding.

This is a representative portrait audit, not verification of every dialog state,
screen size, landscape layout, font size or spoken TalkBack output. See
[localization architecture](../../LOCALIZATION.md) and
[validation history](../../VALIDATION.md) for commands and exact run results.
