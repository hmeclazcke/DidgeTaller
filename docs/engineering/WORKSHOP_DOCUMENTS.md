# Two-sheet workshop documents

The report and the full-size pattern are separate deliverables. The default has
exactly two A 4 sheets: portrait specifications/cutting rows and a landscape
construction drawing. Tiled templates contain only the overlapping 1:1 profile.

`WorkshopDocument`, `WorkshopTables`, `WorkshopDiagram` and `WorkshopTemplates`
prepare immutable dimensions and drawing commands without Android dependencies.
`ConstructionPdf` only owns native page/Canvas resources. `WorkshopPainter` makes
closed contours, shading and strokes testable on the JVM; SVG uses the same marks.
No resonance equations, original design serialization or recording data change.

Wood cavities are semicircles: diameter 30 means depth 15 from the joining plane,
not 30. For a maximum 83 mm bore, wall 8 and radial reserve 1, each prepared half
requires width 101 and thickness 50.5 mm. Saw kerf, facing, end trimming and actual
material strength require workshop measurements; they are not invented here.
Laminated layers use the established enclosing-opening approximation. PVC cuts
include their insertion lengths; the sample assembled length is 800+650+90-30-25
=1485 mm, with the connector starting at 770 and the second tube at 835 mm.

Displayed dimensions use 0.1 mm. Minimum material dimensions round upward;
calculations retain full precision. SVG point coordinates use six decimals and
physical point units. Station labels use two rows when close together; all station
positions remain in the measurement table/CSV. The readable report supports up to
36 method-specific rows and rejects cells that cannot fit; it never truncates data
or silently shrinks text. CSV and SVG remain available for dense reports.

Every export vocabulary term has an English/Spanish pair. Sensitivity descriptions
use that same vocabulary, including bell diameter. Human names replace UUID titles;
file suggestions add a short random suffix. CSV escapes formula-like text.

## Source-bound technical reviews

The source gate has no new numerical baseline/threshold exception. Named eligible
pure functions are measured normally. New data constructors/enum vocabulary and a
page-local mark-list initializer lack independent Detekt function metrics; their
behavior is checked through document/contour/CSV tests. The Canvas adapter constructor
only captures a page canvas and is exercised by native PDF rendering. Compose source
functions retain their explicit compiler-transformation classification: worker
ownership is unchanged, locale follows LocalConfiguration, export naming is tested,
and native UI cases exercise callbacks, navigation and preserved storage. These
reviews do not claim JVM execution of Android Canvas or invent Compose CRAP scores.

## Reproduction and limits

Run `:app:testDebugUnitTest --tests ar.didgetaller.core.WorkshopDocumentTest` for
independent dimensions, groove circles, material minima, catalog overlaps, template
coordinates, calibration, invalid/dense inputs, localization and filenames.
`:app:qualityMutationDocuments` challenges this selected pure document scope.
On a clean emulator, run `WorkshopDocumentsUiTest` to generate six bilingual PDFs,
render every page through Android PdfRenderer and verify the 100mm raster ruler.
`ConstructionUiTest` executes the real Compose export callbacks and isolated storage.
The construction CI scope includes both classes.

Samples and previews: [synthetic export gallery](../examples/construction/README.md).
Physical printer calibration, material certification and played pitch are not
claimed. Remaining layout/text mutations are disclosed separately from numerical
geometry regressions; screenshots complement, rather than replace, geometry tests.


## Verified local execution

Debug/release JVM suites pass; exact totals are recorded in the construction
checkpoint. Six native tests pass on the clean API 37 emulator (document rendering,
actual Compose export callbacks, three methods, isolated persistence and navigation).
A separate Spanish narrow 150% workflow passes. Every generated default PDF page was
rendered at 2x and inspected; tile previews were reviewed as a complete contact sheet.
Numerical tests protect cavities, stock minima, variable PVC reducers, assembly
positions, layered openings, non-monotone bores, tile contours and calibration.

The selected mutation scope deliberately includes layout as well as dimensional
logic. Independent workshop assertions improved detection compared with the first
run; remaining survivors are disclosed, not declared equivalent or killed. This
is not whole-application mutation coverage. No threshold, mutator or baseline was
weakened. Source-bound reviews concern only constructors/Compose correspondence.
