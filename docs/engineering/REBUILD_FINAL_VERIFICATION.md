# Rebuild implementation verification - 2026-11-09

Protected original main:3a160043f9ebb3a420b1d89a9cf4ddfe7d1b5d67.
Feature branch:refactor/uncle-bob-rebuild; PR1 remains draft, never merged.
The containing implementation tree and [source hash inventory](REBUILD_SOURCE_INVENTORY.csv)
identify these local inputs. [Machine-readable values](REBUILD_FINAL_METRICS.json)
preserve counters/denominators. Actual remote source SHA, workflow URLs, conclusions
and downloadable artifacts are maintained in the [PR evidence table](https://github.com/hmeclazcke/DidgeTaller/pull/1).
Required remote checks are a separate integration gate, not inferred from local success.

## Comparable evidence and scope

The original [audit](../quality/QUALITY_AUDIT_VERIFICATION.md) and
[baseline](REBUILD_BASELINE.md) preserve the original measurement environment;
its last verified runtime90d7167 is followed by documentation-only main3a160043.
The previous6867 documentation checkpoint reuses executable11fa244 evidence.
None of these historical checks is presented as a new final execution.

| Measure | Original main evidence | Previous executable milestone | Final local implementation |
| --- | --- | --- | --- |
| JVM case-variants |390 discovered;374 pass;16 skips |936/920/16 |1054 discovered;1038 pass;16 protected skips;0 failures |
| Android suite |127/117/10 |163/153/10 |170 discovered;160 pass;10 skips;0 failures |
| Additional configurations |Existing emulator evidence |Five pass |Five pass: EN/ES compact actual font150 and ES landscape |
| JVM line/branch |28.19%/25.69% |43.39%/39.70% Linux |46.63%/43.96% local |
| Core line/branch |98.47%/77.25% |99.07%/83.78% Linux |99.05%/84.87% local |
| Android line/branch |91.09%/68.25% |92.02%/68.62% |92.07%/68.83%, separate scope |
| Independently scored/unscored |8/329 under old allowlist |484/114 |545/117 under PSI/JVM mapping |
| CRAP-L above10 |Three of8 measured |72 JVM/28 Android |64 JVM/30 Android; separate reports |
| PlotSurface MCC |139 |8 |8; native canvas7 |
| SessionPlot MCC |Not scored by old correlator |74 |44; pure inspection owner methods remain below10 in JVM |
| WavAnalysis.analyze MCC |29 |29 |2; window pass6, flute pass8 |
| CPD groups >=100 tokens |0 across59 files |0 |0; no lowered clone threshold |
| Detekt informational findings |1978 |1987 |2000; no blanket suppression/config reduction |
| Local Android Lint |0 errors/60 warnings/10 hints |0/52/8 local |0/52/7; online version metadata can differ in Linux CI |
| Mutation selection |11 gesture mutants |2318 across70 classes |New focused scope612; broad exact-source scope executed separately in CI |
| Synthetic differential |Not yet configured |741 identical |757 identical, including16 complete offline analyses |
| Python tooling tests |8 |45 |46 pass |

Counts are case/variant executions, not unique user behaviors. JVM skips are eight
protected-audio cases per variant; Android skips are protected fixtures and explicit
configuration assumptions. Five configuration runs are separate, not additional
full-suite coverage merged into the170. Final current-APK owner/presentation smoke
also passes14/14; it overlaps the full suite and is not added to its total.
The emulator is the owned synthetic API37 ranchu/x86_64, explicitly emulator-5580.
CI uses its own clean API35 environment. Neither proves every physical device.

Coverage denominators grow with real source changes and differ between JVM and
Android. Generated scaffolding follows JaCoCo filters; handwritten UI remains in
scope. New pure interval/inspection methods can have zero Android executions while
being fully protected on JVM; this explains some new Android above-target scores.
An increase from28 to30 Android scores is not hidden by changing scope or merging
JVM probes. Core coverage changes by a small fraction as its denominator changes.
Detekt's total informational count grows; structural improvement is demonstrated
by actual responsibility changes and comparable function complexities, not a claim
that every count fell. No acoustic thresholds were tuned to improve these values.

## All source functions remain accounted for

116 production Kotlin files contain698 named declarations:545 measured,
32 nested declarations accounted in enclosing bodies,35 abstract declarations,
73 Compose transformations and13 anonymous implementations. Four anonymous
implementations are local, leaving117 independently unscored. The increase from
114 is three abstract contracts, not three silently unmeasured eligible functions.
Auxiliary constructors/initializers/accessors/wiring remain in the authored callable
inventory and receive narrow source-bound reviews without invented function MCC.
Required gate reports0 violations; existing numeric baselines are unchanged.

## Mutation strength and limitations

PIT DEFAULTS executes a production-only focused scope:612 mutants,471 killed,
121 survived,19 uncovered,one timed out. Killed fraction471/612=76.96%; detected
(including timeout)472/612=77.12%. The same612-mutant production scope before the
last assertion improvements has439 killed/148 survived/23 uncovered/two timeouts.
These scores do not describe the whole application. A rejected experimental1095
report included test classes matched by prefix globs and is not production evidence.
CLI exclusions and a failing-then-passing validator fixture close that scope defect.
No test is removed from execution and no operator/threshold is weakened.

Meaningful additions assert actual localized readout text, every shifted-origin
spectrogram cell, waveform boundaries, accepted-point mean/median/confidence/fraction,
flute half-percent bounds, exact frame budgets, equal-progress previews and observer
publication after cache removal. These target real surviving behavior mutations.
Remaining owner survivors include Unit-return/null artifacts, redundant checks after
an already revoked owner and private terminal bookkeeping. These are equivalent
candidates, not formally proven equivalents. Boundary/error and numerical survivors
remain visible in reports; no recognized data-loss/lifecycle gap is excused merely
by a high score. Generated value-carrier/Intrinsics mutations are not advertised
as scientifically meaningful acoustic defects. Broader CI includes this scope plus
the existing capture/persistence/playback/workspace/DSP selections.

## Regressions, visual evidence and reproducibility

Real native RED includes four saved-diagnostic admissions after ViewModel clear,
optimizer executor rejection and late playback error publication. Final typed owners
and permanent player disposal pass controlled callbacks and actual Android tests.
Original WAV boundary RED has two failures: overflowed intervals accepted before I/O,
and a negative timestamp at a valid large absolute frame. Independent BigInteger and
range assertions protect the fixes; valid synthetic offline results remain identical.
Empty wave/tile inspection originally throws; it now reports missing evidence.

Full JVM/static/build/differential validation passes once on final source, with focused
checks during development. A temporary Windows text-encoding/CRCRLF write is rejected
by strict PSI before publication, then corrected with explicit UTF-8/LF and verified;
failed local logs remain preserved. The final native coverage report has no class-ID
mismatch warning against the complete170-case execution. No incompatible probes merge.
The initial custom library evidence label is rejected by its filename allowlist;
the corrected authorized EN/ES labels then pass. No product assertion is weakened.

Six actual native PNGs inspected include ordinary fullscreen, EN/ES compact150,
ES landscape, spectrogram and recovery dialog. Enlarged content intentionally scrolls;
not all information must occupy one frame. Scientific descriptions lay out every
character and remain reachable; normal geometry and80ms seeking contracts remain.
Detailed screenshots/logs stay ignored and are not public repository assets.

Verified commands and report locations are in [QUALITY_GUIDE](../quality/QUALITY_GUIDE.md).
No new global tool, paid dependency, private recording, physical-device operation,
format migration or metronome is involved. Official ub-mode and its MIT notice remain
unchanged. Main is a reference, not a publication target; the owner decides any merge.

## Final source precision check after the first remote integration

All three f72136c runs actually pass: [fast42702489875](https://github.com/hmeclazcke/DidgeTaller/actions/runs/42702489875),
[native42702483758](https://github.com/hmeclazcke/DidgeTaller/actions/runs/42702483758) and
[mutation42702487499](https://github.com/hmeclazcke/DidgeTaller/actions/runs/42702487499).
The broader report contains101 production classes/2937 mutants:2284 killed,
615 survived,34 uncovered and4 timeouts, engine127s. Those are earlier source-scoped
results, not an assertion that a changed function was already tested remotely.

Independent coordinate review then finds an introduced readout regression which
those green jobs miss. At125ms/44.1kHz, frame5512.5 starts column three in a four-column
11025-frame tile; truncating first to5512 reads column two. A real failing assertion
(-6.0206 rather than-2.4988dB) precedes restoring the original fractional selection.
Integer bounds checking remains, while only column choice retains sub-sample time.
No detector, drawn raster, gesture, WAV or persistence algorithm changes.

The containing final local implementation passes527 JVM cases per variant and the
complete170-case native run plus all five configurations again. This document's local
metrics and source hash inventory include the correction. The focused612-mutant
outcomes remain unchanged: DEFAULTS does not generate this numeric-type regression,
so that unchanged score does not invalidate the independent boundary test.
Removing a now-redundant identity conversion leaves all455 production Kotlin class
files byte-identical; exact hashes justify reusing executed binary evidence for that
nonfunctional cleanup. Current source-scoped CI must be observed in PR1 before
integration readiness; earlier f721 jobs remain historical evidence. No optional
style/performance work is promoted into another open-ended reconstruction.
