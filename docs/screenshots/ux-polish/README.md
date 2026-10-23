# Product polish: emulator evidence

Date: 2026-10-23. Android API 37, x86_64, emulator-5554. All recordings, timelines
and names shown are synthetic audit fixtures or explicitly owned emulator tests.
These are actual rendered screenshots, not mockups or physical acoustic validation.
The app intentionally uses its dark theme under both Android light and dark modes.

| Evidence | Configuration | What to inspect |
| --- | --- | --- |
| [Before session](before-session.png) / [after session](after-session.png) | 1080×2400,420 dpi, about 411×914 dp,font scale 1.0,English | Full-screen action moves to card heading; shared player gains an accessible seek control; comparison is no longer prominent by default |
| [Before dialog](before-dialog.png) / [after dialog](after-dialog.png) | Same typical-phone configuration | Container uses the app palette rather than the inherited purple surface |
| [Compact Spanish](after-compact-es.png) | 1080×2400,480 dpi,360×800 dp,font scale 1.5,Android light | Large text, wrapped controls, measured Hz/note/time axes and playback controls; partial adjacent tab is scrollable |
| [Large English](after-large-en.png) | 1080×2400,360 dpi,480×1067 dp,font scale 1.3,Android dark | Hierarchy and chart/transport on a larger phone with enlarged text |
| [Enabled comparison](after-comparison.png) | Typical phone,English | Explicit YIN / experimental dashed SWIPE′ legend remains when diagnostics collapse |
| [Advanced viewer](after-advanced-viewer.png) | Typical phone,English,native window capture | Tools sheet's advanced section retains the explicit comparison switch |
| [Landscape spectrogram](after-landscape-chart.png) | Compact phone,font scale 1.5,Spanish | Chart, time labels and shared transport fit landscape without covering data |
| [Before wide measurement](before-landscape-measure.png) / [after wide measurement](after-landscape-measure.png) | Typical phone,landscape,font scale 1.0,English,native window captures | Previously the large vertical note card obscured Hz/status; the short-height layout keeps the primary readout above Stop and save |
| [Compact wide measurement](after-compact-wide.png) | Compact phone,landscape,font scale 1.5,Spanish,native window capture | Complete note label, frequency, signal state and stop control remain visible with enlarged text |

Before/after session captures use the same deterministic fixture; the new seek
example is paused at 3 seconds rather than the initial 0 seconds. Dialog screenshots
capture the dialog surface only. Most other screenshots capture the Compose root;
native window captures include Android system bars. Scroll positions differ by
the control being inspected; partially offscreen preceding cards are ordinary scroll.

The full local inventory and logs remain ignored under `checkpoints/ux-polish/`.
See [audit](../../UI_AUDIT.md) and [validation](../../VALIDATION.md) for scope,
failed/repeated checks, real navigation/recording tests and remaining limitations.
No full TalkBack traversal, audible/sample-accurate synchronization or physical
Samsung validation is inferred from these pictures.
