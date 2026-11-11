# Inverse design and workshop construction

Branch: `feature/inverse-design-construction`; draft [PR #5](https://github.com/hmeclazcke/DidgeTaller/pull/5).
Main reference: `b411bb8978ab5c610ba5ffba882a98cb1e2b6bef`.
Current status: **READY FOR OWNER REVIEW**. The complete construction workflow is
implemented and executed on the clean emulator. PR5 remains draft and unmerged.

## Implemented application workflow

Laboratory > Search > Construction plan, or the same action on a saved Library
design, opens acoustic target, candidates, construction method and plan/export.
Original bore editing, three-target evolutionary search, four bottom destinations
and historical design serialization remain available. Construction schema-1 JSON
is separate (`construction_v1`), keyed by design ID; Laboratory has one draft.
A shared measured catalog supplies defaults; saved projects retain their snapshot.
Corrupt or unknown-schema documents block saving and preserve their originals.
No legacy design/session/recording metadata is rewritten.

Targets accept Hz or a note, two optional higher resonances, mouth ID, preferred/
maximum length and bore/bell bounds. Bounded deterministic search ranks feasible
candidates by cents error, optional resonances and piece count. Cards compare
resonance, cents, dimensions, components and ideal/manufactured profiles. Equivalent
manufactured candidates are deduplicated. No global optimum/playability is claimed.

## Manufacturing methods

- Split wood: mirrored semicircular hollows, regular and mandatory transition
  stations, two prepared halves A/B, minimum stock width/thickness, wall and radial
  overcut reserve. Add saw kerf separately to the unsplit blank.
- Transverse laminated layers: exact thickness list or nominal thickness with final
  remainder; constant openings enclose ideal extrema, report excess/stock dimensions.
  Search reserves a cylindrical first layer to preserve the requested mouth ID.
- PVC: user-measured tube ID/OD/stock length, cuts, socket dimensions, insertions,
  full fitting body and variable central passage. Cuts include inserted material;
  only the unoccupied fitting web adds assembled length. Stock, socket fit and
  overlap validation reject impossible combinations. No invented commercial catalog.

Measured CSV entry (mm):

```text
pipes:    id,ID,OD,stock_length
fittings: id,left_socket_ID,right_socket_ID,left_insertion,right_insertion,body_length,minimum_passage_ID,0:ID|web_length:ID
assembly: pipe_id,cut_length,next_fitting_id
```

Final assembly row ends in `-`. Add intermediate fitting points with `|`.
Web length equals body minus both insertions; endpoints and minimum passage must
agree. IDs use unique ASCII letters/digits/underscore/hyphen. Catalog starts empty.

## Acoustic and export contracts

The existing lossy transmission-line equations are reused by the original shape
entry point and a cylinder-chain entry point. Discontinuous junctions stay abrupt;
tapers use eight midpoint cylinders per interval. Uniform legacy/chain equivalence
and subdivision invariance are tested. Search uses a 3 Hz grid, final plans 1 Hz
with existing peak interpolation. These are passive model predictions, without
physical calibration or guaranteed played drone/toot.

Tolerance recalculates ID +/-1 mm, final-piece length +/-5 mm, bell ID +1 mm and
first PVC insertion 1 mm short (an exposed socket gap, not a compressed reducer).
Local sensitivity ranks up to 12 sections. Out-of-model cases are unavailable,
never zero. Circular sections do not represent asymmetric hollowing or structure.

- CSV: ideal/actual stations, both junction diameters, half depths, assembled length
  and parts. Locale-independent numbers; textual spreadsheet prefixes escaped.
- SVG: metric actual profile, discontinuities, stations, parts and 100 mm reference.
- PDF: A4 overview, reduced scale, wood half cross-sections, paginated measurements,
  parts/instructions and optional 1:1 inner-profile tiles. Tiles span 160 mm, start
  every 150 mm (10 mm overlap), using exactly 72/25.4 points/mm. Every page includes
  a 100 mm ruler. Match identical x marks on neighboring tiles. Overview and
  cross-section views are reduced; only the tiled inner profile is full size.
  Print at 100% and measure; automatic calibration is not claimed.

Exports use the existing Android document picker callback. Existing revocable
analysis ownership prevents stale publication after editing or closing. No audio
content is needed by this workflow. Numerical decimals are not physical precision.

## Validation and exact continuation

Historical `8cd0d14`: 562 debug cases, 554 passed, eight protected skips; 218 selected
PIT mutants, 172 killed, 43 survived, three uncovered. Not expanded-feature totals.

New tests cover manufactured geometry, measured reducers/overlaps, invalid catalogs,
layer constraints, bounded search, sensitivity, metric exports, tile coverage,
storage round trips/corruption and acoustic equivalence. A failing laminated-mouth
regression was reproduced and corrected. Numeric quality baselines are unchanged.

Verified production source: `992dadc`, unchanged by the later test-only `70e744d`.
CI debug and release each discover 580 JVM cases: 572 passed, eight protected skips,
zero failures. CI JVM line/branch 46.94%/45.18%, core 99.26%/82.01%.
Source gate: 670 measured, 150 unscored by the numerical summary,
64 historical above-target methods, zero new violations and zero CPD groups.
Unsupported constructors/Compose are reviewed by source hash; no numeric exception
or threshold change. EN/ES resource parity passes (668 strings).

Selected `:app:qualityMutationWorkshop`: 647 mutants, 432 killed, 200 survived,
15 uncovered, no timeout or runner errors (66.77% of generated mutants). Scope
now includes measured catalog, actual exports, target search and tolerance logic;
not comparable to the old 218-mutant scope. Earlier runs exposed weak dimensional
assertions and two incomplete outcomes (child connection reset, then unbounded
allocation under a loop-increment mutant). Independent station/depth/mesh/cut
checks and fixed-cardinality mesh allocation address those findings. Logs remain
ignored under `checkpoints/construction-complete/`.

`ConstructionUiTest` covers search/selection, three methods, catalog/project save,
tolerance, all exports, Android PDF rendering/ruler, legacy-key integrity, damaged
storage and real Laboratory entry/close. Manual deep scope `construction` runs on
clean API35 with EN/ES, normal/150%, portrait/landscape and screenshot artifacts.
Compilation is not an executed Android test.

First integration (`5247ca6`): [fast CI](https://github.com/hmeclazcke/DidgeTaller/actions/runs/42913461785)
and [focused emulator](https://github.com/hmeclazcke/DidgeTaller/actions/runs/42913473334)
passed; 9 native executions and 29 images. Visual review exposed IME panning at
150%, unformatted dimensions and PDF word breaks. `83b7997` uses explicit
[dialog window insets](https://developer.android.com/reference/kotlin/androidx/compose/ui/window/DialogProperties),
formats dimensions and adds natural word wrapping, a length dimension and tile
registration. Updated native tests check a visible editor heading, exact PDF page
count and registration position, as well as the 100 mm ruler. JVM, gate and selected
PIT passed locally with the updated construction code.

## Final source-scoped evidence

- [Fast CI for production source992dadc](https://github.com/hmeclazcke/DidgeTaller/actions/runs/42917012360):
  success; debug/release JVM, app and test APK, Lint, localization, coverage,
  Kotlin inventories, CRAP-L/source ratchet, CPD and Python/Java tooling tests.
  Differential synthetic contracts match the pinned original reference byte-for-byte.
  [Fast CI for test-only70e744d](https://github.com/hmeclazcke/DidgeTaller/actions/runs/42993559385)
  also passed with the final keyboard/selected-tab assertions compiled.
- [Eight passing native cases](https://github.com/hmeclazcke/DidgeTaller/actions/runs/42917003540):
  four base tests, EN/ES normal portrait and EN/ES narrow 150% portrait. The workflow
  itself failed only on landscape navigation after editing, and is retained as a
  failed run, not relabeled a full pass.
- [Targeted landscape150 verification](https://github.com/hmeclazcke/DidgeTaller/actions/runs/42993552622):
  success, one complete search/plan/save/tolerance/export scenario. Test-only70e744d
  closes the active IME through Espresso before navigating and requires selected-tab
  semantics. Production code is identical to992dadc. The default manual configuration
  still runs every construction case; the explicit landscape option reuses the
  eight unchanged successful production scenarios instead of repeating them.
- Remote Lint: zero errors,70 warnings,7 hints; three construction recommendations
  suggest Canvas KTX helpers. Broad Detekt:2453 informational findings. These are
  disclosed debt, not claims of zero static-analysis issues. No new numeric gate
  exception was introduced. Workspace orchestration MCC is9; domain calculations
  are independently measured and tested.
- All exports are triggered in Compose and their returned bytes verified. Android's
  real PdfRenderer checks A4 dimensions, exact extra tile count,100mm raster ruler
  and150mm registration. Actual system document-picker file destinations reuse the
  unchanged app export transport; no physical printer was tested.
- [Reviewed authentic screenshots](../screenshots/construction/README.md): target,
  candidates, wood and PVC plans, laminated steps,150% editor, PDF overview/tile,
  and landscape navigation. Original PNGs, synthetic data only.

## Practical limitations and reproducibility

Search targets constrain candidate search. Generating a plan for the current
geometry instead reports that actual bore without silently retuning it. PVC search
varies cuts within the user-entered assembly sequence; it does not enumerate every
possible catalog arrangement or perform stock nesting. Every listed cut can use
its own stock piece. Sensitivity is bounded to the listed global scenarios and
up to12 local pieces. Model resonance is not a measured played note, structural
certification or guaranteed physical performance. Construction requires measured
materials and printer-scale verification. These are explicit scope limits, not
unfinished export/UI integrations.

```powershell
.\gradlew.bat --max-workers=2 -Pkotlin.incremental=false :app:testDebugUnitTest :app:testReleaseUnitTest :app:assembleDebug :app:assembleDebugAndroidTest
.\gradlew.bat --max-workers=2 -Pkotlin.incremental=false :app:qualityMutationWorkshop
# Existing complete metric commands remain in docs/quality/QUALITY_GUIDE.md.
# GitHub manual deep workflow: scope=construction, configuration=all (or landscape).
```

No known functional or data-integrity blocker remains for owner review of this
feature. Keep PR5 draft; do not merge automatically. Main, Samsung, private WAVs,
legacy sessions/designs, official ub-mode and its license were not modified.
