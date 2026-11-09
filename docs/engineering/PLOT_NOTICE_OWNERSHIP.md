# Plot annotation ownership

Continuation from verified executable source 8b73c21 / documentation d82f3bf.
Whole rebuild IN PROGRESS; original main and the draft PR remain unchanged.

The fullscreen empty-trace message was centered over SessionPlot, including its
readout, axes and margins. A real API 37 compact/150% regression fails because
the message's left bound crosses the actual Y-label boundary. It compares native
semantic coordinates with PlotDataArea, not a guessed screen percentage.

PlotCanvas now owns a passive annotation inside the ready frame's measured
physical insets. Its Box and Canvas share the existing measurement, accessibility
and pointer modifier. Native text wraps at the available data width. The viewer
only supplies the localized message; it no longer owns its coordinates. No
frequency, grid, cursor, plot projection or gesture-engine calculation changes.
Absolute padding follows physical Canvas coordinates, independent of layout
direction. The annotation has no independent pointer handler or new loading state.

The unused onPreview/onExpand forwarding into PlotSurface/SessionPlot is removed;
it had no call sites in either previous implementation. Explicit fullscreen
buttons and onExpand in SessionExplorer remain active.

Native evidence: the exact original-message regression first fails, then passes.
Four focused cases pass including collapsed/reappearing frames and a tap/scrub
started over the annotation: the measured viewport, preview and committed time
agree. Tests reject incomplete text and annotation bounds outside the data area.
Six final focused native cases pass: the four-case interaction/readiness group,
ES compact portrait and ES landscape at 150%. Final EN/ES/landscape PNGs were
inspected. Current-source quality metrics are recorded at delivery;
no historical full-suite or mutation run is presented as a fresh execution.

No new pure domain calculation or mutation target is introduced. Existing DSP,
playback, projection and painter bytecode/tests are unchanged. Final integration
will run the applicable existing CI; there is no reason to rerun the unchanged
broad mutation scope solely for this Compose layout binding.

Source: [native binding](../../app/src/main/java/ar/didgetaller/ui/PlotCanvas.kt),
[real viewer regression](../../app/src/androidTest/java/ar/didgetaller/ui/SessionEvidenceUiTest.kt),
[input contract](../../app/src/androidTest/java/ar/didgetaller/ui/PlotReadinessUiTest.kt).

Focused final wiring rerun: four native cases pass after moving message selection
back to the viewer. The unchanged selection policy had initially been moved into
SessionPlot, increasing its MCC74 ->77; the real gate rejects that regression.
Retaining selection in the viewer and coordinates in the renderer restores
SessionPlot74, viewer68, PlotSurface8, PlotCanvas7 and PlotNotice2 without a
complexity exception. Debug JVM448 discovered/440 passed/eight protected skips;
CPD zero >=100 tokens. Source622 named functions:470 scored,36 nested,30 abstract,
13 anonymous and73 Compose. Independent unscored112, JVM high73, gate zero
violations after six exact Compose technical reviews and no ordinary score waiver.
Release/JVM, full native, broad mutation and final Lint belong to the next combined
integration, not a falsely repeated local full run for this UI-only boundary.

Next actual state defect is reproduced independently: after ViewModelStore.clear,
setSwipeComparison(true) publishes enabled/running state although the owner scope
is cancelled. This is a coroutine/Handler lifecycle problem in the current code,
not an executor/version implementation. Its failing native regression remains
uncommitted for the next implementation cycle; no private WAV is opened.
