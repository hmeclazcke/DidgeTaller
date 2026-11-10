# Inverse design and workshop construction

Branch: `feature/inverse-design-construction`; draft [PR #5](https://github.com/hmeclazcke/DidgeTaller/pull/5).
Main reference: `b411bb8978ab5c610ba5ffba882a98cb1e2b6bef`.
Current status: **IN VERIFICATION**. Observe Android execution, screenshots and
exact-source CI before declaring the complete workflow ready.

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
  a 100 mm ruler. Print at 100% and measure; automatic calibration is not claimed.

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

Local executable checkpoint: 580 debug JVM cases, 572 passed, eight protected skips,
no failures; app and instrumented APKs compiled. JVM line/branch 47.02%/45.10%, core
99.23%/81.84%. Source gate: 667 measured, 149 unscored by the numerical summary,
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

Next: execute Android scope and inspect images/PDF; fix actual failures; observe
final fast CI; replace pending status with exact evidence. Keep PR5 draft,
main unchanged. No Samsung or private recording access.
