# Two-sheet workshop documents

The report and the full-size pattern are separate deliverables. The default has
exactly two A4 sheets. Split wood uses a landscape instrument/stock overview and
a landscape carving sheet; laminate/PVC keep portrait specifications and a
landscape construction drawing. Tiled templates contain only the overlapping 1:1 profile.

`WorkshopDocument`, `WorkshopTables`, `WorkshopDiagram` and `WorkshopTemplates`
prepare immutable dimensions and drawing commands without Android dependencies.
`ConstructionPdf` only owns native page/Canvas resources. `WorkshopPainter` makes
closed contours, shading and strokes testable on the JVM; SVG uses the same marks.
No resonance equations, original design serialization or recording data change.

Wood cavities are semicircles: diameter 30 means depth 15 from the joining plane,
not 30. For a maximum 83 mm bore, wall 8 and radial reserve 1, each prepared half
requires width101 and thickness50.5 mm. Those are separate axes: the joined
rectangular envelope is101×101, not automatically a round101 mm exterior. The log
case assumes a concentric circular minimum envelope. An editable9 mm wall is an
example constraint, not a universal structural recommendation.

`WoodStock` distinguishes natural logs, two boards and a block to split.
`WoodFit` checks the union of bore and measured-stock breakpoints; piecewise-linear
bounds cannot hide an intermediate neck. For a log, usable radius is the measured
outer radius minus bark/exterior/uncertainty; the joining-plane offset is half the
saw kerf plus face loss. Available half depth is their difference. For boards,
width loses a face allowance on each side and each half loses two prepared faces;
for a block, half thickness starts from(total thickness−kerf)/2. End trim is per
end. Board dimensions refer to the smaller of both pieces. Bounds, straightness,
centering and sound material require explicit confirmation; missing ranges are not
extrapolated. Unconfirmed or negative clearance is stated, never silently resized.
No geometric test certifies species/grain strength or actual workmanship.
Laminated layers use the established enclosing-opening approximation. PVC cuts
include their insertion lengths; the sample assembled length is 800+650+90-30-25
=1485 mm, with the connector starting at 770 and the second tube at 835 mm.

Displayed dimensions use 0.1 mm. Minimum material dimensions round upward;
calculations retain full precision. SVG point coordinates use six decimals and
physical point units. Station labels use two rows when close together; all station
positions remain in the measurement CSV. Wood sheets show selected meaningful
sections and retain full station detail in an additional six-decimal numeric CSV
table; unavailable stock stays blank. Dense wood stations do not add PDF pages.
Component tables for laminate/PVC retain their36-row legibility boundary and reject
oversized cells instead of shrinking text. SVG preserves the precise drawing marks.

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
Use `WoodStockTest` and `WoodWorkshopTest` for actual-stock constraints and
independent83/41.5/50.5/101 mm identities;`:app:qualityMutationWood` targets the pure
fit/form logic. On a clean emulator,`WorkshopDocumentsUiTest` generates ten bilingual PDFs,
render every page through Android PdfRenderer and verify the 100mm raster ruler.
`ConstructionUiTest` executes the real Compose export callbacks and isolated storage.
The construction CI scope includes both classes.

Samples and previews: [synthetic export gallery](../examples/construction/README.md).
Physical printer calibration, material certification and played pitch are not
claimed. Remaining layout/text mutations are disclosed separately from numerical
geometry regressions; screenshots complement, rather than replace, geometry tests.


## Earlier generator execution (historical, before the stock revision)

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


PVC drawing correction: dashed socket outlines use the measured socket diameters
and insertion lengths at their true assembled positions. They do not claim an
unknown connector outside diameter or wall thickness. Solid lines remain the
actual internal bore. The parts table retains the measured body/socket dimensions.


Final verification: [fast CI](https://github.com/hmeclazcke/DidgeTaller/actions/runs/43037920159)
and [11 native executions](https://github.com/hmeclazcke/DidgeTaller/actions/runs/43037955490)
passed on764bc63. Selected PIT487 generated/245 killed/231 survived/11 uncovered;
50.31% of generated mutants killed,19s execution. Do not infer whole-app strength.
The synthetic gallery records the native API35 artifact and exact file hashes.


## Current stock-aware evidence

See [construction progress](CONSTRUCTION_PROGRESS.md) and the [actual sample gallery](../examples/construction/README.md)
for commit-scoped native runs, current stock examples, limitations and source-bound
review counts. The owner-supplied reference image guides style only. No raster art
is used as a source of manufacturing coordinates. Pure vector geometry is shared
by PDF/SVG; the optional1:1 template retains a true100 mm calibration reference.


## Focused workshop usability correction — 2026-11-11

The current split-wood layout supersedes the former pair of long A/B projections.
Page 1 contains the complete bore, actual distance/diameter callouts and available
stock. Rectangular outside edges stay parallel; logs use their stated minimum or
measured bounds. Page 2 contains three complete cross sections and a selected
station table, with full-diameter, half-depth and remaining-wall arrows.

`CarvingMeasurements.Row` distinguishes bore diameter, half-groove depth, wood
below the groove and minimum wall (including board sides). For 50.5 mm prepared
boards the 0/1020/1500 mm rows are30/43/83 mm bores,15/21.5/41.5 mm depths and
35.5/29/9 mm remaining bases. A 70 mm board has28.5 mm below the bell groove;
a 101 mm width still has9 mm side walls. Logs use the prepared circular-cap radius
and joining-plane offset. Unconfirmed or absent local measurements produce no
invented remaining thickness or limiting location. All selected print stations
also appear in the full-precision CSV. SVG follows the new dimensioned second
sheet; exhaustive numeric station data remains in CSV. True-size tiles are
unchanged and separate.

Spanish print dimensions use decimal commas; target notes use compact accidentals
such as Do♯2. Required dimensions/feasibility remain independent of wood strength
and played pitch. Three native cases passed on a fresh API 37 emulator,
including actual Compose export callbacks. 20 default pages and 10 tile pages were
rendered; the requested Spanish PDFs were independently rasterized with the Windows
PDF renderer before and after editing. Local logs stay ignored; source/artifact
hashes and actual examples are linked from the sample gallery. No acoustic engine,
saved design/session format, microphone operation or physical-device state changed.
