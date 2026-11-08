# Forensic engineering compliance verification

Status: audit findings verified; whole-application reconstruction **IN PROGRESS**.
Audit date: 2026-11-08. Source interval: `9cd18f69d6aaecbdd1e1f8c644347d4d2103836c`
to `672c9b2120a7c869cd4331699ba85017945a0db4`. Original main remains
`3a160043f9ebb3a420b1d89a9cf4ddfe7d1b5d67`; PR #1 remains draft.

## What can be proved about ub-mode

The current session explicitly invoked and loaded the project-local methodology.
Its SKILL.md and MIT LICENSE were downloaded from the official pinned revision
`3da62bf3fcfd2034de41f30d920d2739e566171f` and compared byte-for-byte:
SKILL SHA-256 `4120873db2e3681ea9b7de4d7105fc9d3406657c9ada91ce77b9b3c4962f3202`;
LICENSE `cf496364b10d049ba2b04ac4c04f29ab1a34b54be96209cb32666fac9eba6bf3`.
UTF-8, `name: ub-mode`, local discovery path and project references are valid.
Project safety/Kotlin adaptations remain in AGENTS.md and the quality guide.
Explicit feature-branch/privacy instructions take precedence over generic upstream
publication advice. The official file was not edited.

Installation, discoverability, invocation, application and enforcement are different
claims. **Historical invocation cannot be independently verified from the available
evidence.** This audit did not search unrelated private Codex session records.
Existing task prompts/checkpoints record invocation and test-first intentions;
aggregated commits cannot establish the chronological order of every test/code edit.
Source, regression tests and real CI demonstrate capture ownership, pure state
boundaries, immutable publication snapshots, differential checks and mutation
execution. They do not establish universal architectural compliance. The original
gate also had a demonstrable measurement-completeness gap, corrected below.

## Git provenance

| Commit | Production change | Associated evidence |
| --- | --- | --- |
| `12dc104` | CaptureHandoff, RecordingCoordinator, owned terminal delivery and ViewModel adapters | Deterministic handoff/coordinator contracts; synthetic recording instrumentation; resource translations |
| `de8df92` | SpectrogramRaster extraction and workspace delegation | Known-power/pixel/cancellation tests; selected mutation scope expanded |
| `a7d2514` | Disposal/error ownership in coordinator and ViewModel | Regression contracts for errors/permission results after disposal |
| `7aa7fcb` | Playback intent through asynchronous seek acknowledgement | Four reproduced deterministic failures, stronger playback assertions and real emulator rerun |
| `672c9b2` | None | Documentation evidence only |

No inventory, complexity configuration or quality-tool rule changed in this
interval. Production Kotlin changed in eight files. The audit itself changes no
application production/test/resources, acoustic parameters or saved formats.

## Exact reconciliation

| Independent Detekt inventory | Previous | Added | Removed/reclassified | Latest |
| --- | ---: | ---: | ---: | ---: |
| Reliably scored | 361 | 36 | 0 | 397 |
| Unscored | 76 | 14 | 0 | 90 |
| Total | 437 | 50 | 0 | 487 |

All **50 added named declarations are actual source additions**, not old functions
newly found by a tool. There are also **16 modified existing named functions**.
No old named declaration was removed, moved or renamed; none changed scoring
classification. New declaration does not mean wholly new behavior: callbacks,
recording policy, finalization, playback starts and raster conversion extract
previously inline/lambda responsibilities. Those historical lambdas are separately
accounted in the linked PSI inventory rather than called new application features.

The additional 14 unscored declarations are exactly:

* Seven abstract `RecordingCoordinator.Observer` contracts at
  [RecordingCoordinator.kt](../../app/src/main/java/ar/didgetaller/core/RecordingCoordinator.kt), lines 18–24.
  They have no executable body/line denominator; inventing coverage or CRAP is wrong.
* Seven anonymous implementations at
  [TallerViewModel.kt](../../app/src/main/java/ar/didgetaller/TallerViewModel.kt),
  lines 349, 352, 353, 354, 358, 362 and 363: `changed`, `saved`, `saveFailed`,
  `discarded`, `finalizationFailed`, `libraryChanged`, `idleAfterDisposal`.
  Each has a unique same-name/source-range JaCoCo method in `$recording$1`.
  Each has real **0% JVM method lines**; the mapper nevertheless cannot score it
  because Detekt emits **MCC 0**, an invalid complexity input. Coverage availability
  and invalid complexity must not be conflated. Android execution is separate.

## Every unscored declaration is accounted for

| Classification | In the 90 Detekt denominator | Additional PSI declarations | Actual reason |
| --- | ---: | ---: | --- |
| Abstract contracts | 23 | 0 | `hasBody=false`, no executable method-line denominator |
| Compose functions | 58 | 0 | Valid source MCC and one JVM candidate each; descriptor transformation deliberately unsupported by current correlator |
| Anonymous implementations | 9 | 4 | Nine MCC=0 rows (seven new ViewModel, two existing JSON adapters); four existing local playback event overrides have no standalone Detekt MCC |
| Nested functions | 0 | 40 | No independent complexity/bytecode pair; complexity attributed to enclosing source method |
| Total | 90 | 44 | 134 unscored PSI declarations, not 134 independent Detekt functions |

Thus PSI named totals are **481 → 531**, distinct from **437 → 487** Detekt
observations. The four existing playback anonymous methods are counted only once
in the extra 44, not again in the 90. All 13 anonymous overrides have one diagnostic
JVM candidate. Compose is an investigated mapping gap, not an assertion that Kotlin
is inherently unmeasurable: validating transformed descriptors/generated-body
boundaries is the next measurement improvement. Zero-covered measurable methods
already receive real CRAP-L scores. They are never reclassified as unavailable.

The [complete machine-readable inventory](FORENSIC_FUNCTION_INVENTORY.json) contains
every changed authored callable, first relevant commit, lexical identity/hash,
source location, available MCC/method counters/CRAP-L, owner test evidence, executed
method mutations, historical gate status and action. Its `unscored` array includes
all 134 PSI declarations with `in_detekt_denominator` identifying exactly the 90,
individual diagnostic candidates and specific explanations/recommendations.
Constructors, accessors, initializers and lambdas are separately inventoried; they
are not presented as compiler-generated methods or independent CRAP measurements.
Two existing constructors (MicrophoneEngine and PlaybackController.State.Ready)
changed signature; unique source-scope correspondence classifies them as modified,
not as two new constructors plus two removals. The seven added constructor
identities therefore represent five new constructors and those two evolutions.
Token relocation matches require unique correspondence in both directions;
ambiguous similar lambdas remain additions/removals. Token hashes retain line
breaks conservatively, ignore horizontal formatting/comments, and do not prove
semantic equivalence or infer test-first chronology.

## Quality of changed/new code

Of 66 new/modified named declarations, **52 have reliable scores**: 38 have 100%
JVM method-line coverage and 14 have 0%. Seven are abstract and seven have invalid
standalone Detekt MCC. Every one has owner-level behavioral evidence recorded;
that is not a claim that every assertion or hardware failure branch is covered.
All 36 newly added measured functions are below CRAP-L 10. Four **existing modified**
methods exceed the JVM target:

| Function | MCC before → after | JVM lines | JVM CRAP-L before → after | Separate Android CRAP-L | Finding |
| --- | ---: | ---: | ---: | ---: | --- |
| MicrophoneEngine.start | 8 → 8 | 0% | 72 → 72 | 8.152 (86.67% lines) | Device initialization/error boundary; hardware fault coverage incomplete |
| TallerViewModel.startSession | 5 → 5 | 0% | 30 → 30 | 5.049 (87.5%) | Existing recording orchestration; retain lifecycle tests |
| TallerViewModel.discardSession | 3 → 3 | 0% | 12 → 12 | 3.141 (75%) | Existing too-late-discard fallback; preserve audio/index assertions |
| SessionWorkspace.render | 17 → 13 | 0% | 306 → 182 | 13 (100%) | Improved raster boundary, but orchestration remains materially complex |

JVM and Android scores are never merged or substituted into the JVM gate. These
four were admitted by the old **non-regression** policy: three were unchanged risk,
one improved. Green CI never meant all changed methods were below target. The
current audit explicitly reviews them at exact source hashes/CC/score ceilings;
`render` remains a concrete architectural priority, not a completed reconstruction.

### Mutation evidence, not assumed strength

Verified domain execution at `7aa7fcb`: 29 selected bytecode classes, 1,519 mutants,
1,119 killed, 382 survived, 16 uncovered, 2 timeouts. The 73.80% PIT detection result
includes timeouts; it is not a whole-app killed-mutant score. Unchanged inputs at
`672c9b2` permit evidence reuse. In the **changed named methods specifically**, 35
methods contain 190 direct mutants: **183 killed / 7 survived**, no other outcomes.
Compiler-generated lambda mutations outside those exact named methods are not
silently added to that denominator.

The seven survivors are in PlaybackController `poll` (118), `start` (103), `toggle`
(132); CaptureHandoff `terminal` (44); RecordingCoordinator `append` (66), `saving`
(73), `discarding` (79). Operators remove equality guards or `handoff.stop()`.
Several interact with other state invariants and may be redundant, but equivalence
is **not proved**. They remain specific test-strength/contract-review work. The
Android adapters TallerViewModel/MicrophoneEngine/SessionWorkspace are not selected
by free pure-JVM PIT; their extracted domain collaborators are. No full mutation
or emulator rerun was needed for this tooling-only audit.

## Executable controls: before and corrected

The previous gate rejected global coverage drops, higher unique-name complexity,
new/increased reliably mapped high CRAP-L, lost reviewed JVM mappings and new CPD
clones. It did **not** join source changes to inventory completeness. A new eligible
unscored function, ambiguous mapping or empty complexity list could pass. Missing
summary files/malformed JSON already failed, but the raw coverage XML and the
complete PSI mapping were not required by the gate itself. Overloaded-function
complexity aggregation omitted ambiguous names. Legacy high-risk unchanged scores
could pass without a new source-bound review after a body edit.

Isolated CLI tests reproduced those failures against synthetic temporary sources
and metrics. Corrected `quality_gate.py` now additionally:

1. Requires nonempty schema-2 PSI inventory, normalized coverage, raw JaCoCo XML,
   complexity summary and complete function mapping. Validates all production
   `.kt` paths in **both main/java and main/kotlin**, current file hashes, every
   named source identity, actual raw method counters and paired source complexity.
2. Uses historical callable fingerprints pinned to `9cd18f6`, separate from the
   unchanged numerical quality baseline. No old debt is erased or denominator
   shrunk. New/modified callables are emitted in `changed-callables.json`.
3. Requires changed measurable functions below target or an explicit source-bound
   above-target risk review. Missing/ambiguous/invalid ordinary measurements fail;
   a generic exception cannot waive them. Overloads use full source identity.
4. Allows abstract contracts without fabricated execution scores. Nested/auxiliary
   bodies inside named functions inherit source-body accountability. Constructors,
   property initializers and accessors outside such bodies require explicit review.
5. Requires narrow, content-bound technical reviews for unsupported anonymous,
   Compose, inline or suspend transformations. They remain visibly **unscored**.
   Renames/moves change identity; body/signature edits expire the review. Reviewed
   high-risk ceilings cannot rise silently. Review configuration changes are
   visible in Git; CI never automatically regenerates these acknowledgements.

There are **35 explicit historical-source acknowledgements**: four high-risk
methods, seven anonymous adapters and 24 auxiliary declarations. Their reasons,
exact hashes, test paths and remaining risks are in
[source-reviews.json](../../config/quality/source-reviews.json). They are an audit
review, not owner approval, a Crapper certification or a completed architecture.
Future eligible unscored code fails; reviewed framework gaps can still pass
**explicitly**, not silently. A gate cannot judge architectural cohesion, assert
that tests are meaningful from coverage alone, or prevent an authorized maintainer
from editing its policy. PR review remains necessary; branch protections were not
changed.

The same Python entry point runs locally and in required fast CI. Shell failures
propagate; no continue-on-error is used. The existing workflow already executes
PSI tests/inventory, summaries and gate, so no parallel competing workflow was
added. Full instrumentation and broad PIT retain their existing, different cadence.
Current PMD CPD still scans `main/java`; all current production Kotlin is there.
Its root must also be extended if production is introduced under `main/kotlin`;
the new source completeness guard is not a claim that every existing analyzer
already shares that source root. No flavors or other production source sets are
currently included in this policy. Field/type declarations without authored
callable bodies are not independently CRAP-scored. JaCoCo XML does not embed a
source Git SHA: raw-counter consistency cannot establish freshness by itself;
fresh CI execution or independently verified artifact provenance remains required.
`--record` is an explicit numerical-baseline operation, never a CI repair command;
it does not renew the separate source policy/reviews.

## Verified evidence and reproduction

| Evidence | Actual successful remote execution |
| --- | --- |
| Previous metrics | [42415475188](https://github.com/hmeclazcke/DidgeTaller/actions/runs/42415475188), head `9cd18f6` |
| Latest source JVM/static/coverage | [42469282408](https://github.com/hmeclazcke/DidgeTaller/actions/runs/42469282408), head `7aa7fcb` |
| Real Android suite/coverage | [42469279440](https://github.com/hmeclazcke/DidgeTaller/actions/runs/42469279440), head `7aa7fcb` |
| Real domain mutations | [42469281768](https://github.com/hmeclazcke/DidgeTaller/actions/runs/42469281768), head `7aa7fcb` |
| Documentation milestone fast gates | [42471345043](https://github.com/hmeclazcke/DidgeTaller/actions/runs/42471345043), head `672c9b2` |

Run IDs, GitHub head SHAs, completion and conclusions were independently checked
through the authenticated API, not inferred from YAML. Archived source hashes and
Git diffs establish the applicability of the runtime evidence. Artifact retention
is seven days; committed inventory/audit preserve selected nonprivate findings,
not all ephemeral report data. See REBUILD_VERIFICATION_7aa7fcb.md for historical
656 JVM discovered/640 passed/16 protected skips; Android 147/137/10, and distinct
coverage denominators. They are reused evidence, not newly executed audit tests.

Focused audit commands (repository root; Gradle JVM/source tools only):

```powershell
.\gradlew.bat :engineering-tools:test :engineering-tools:sourceInventory
python tools/coverage_summary.py
python tools/quality_gate.py
python -m unittest discover -s tools -p 'test_*.py'
```

The coverage refresh/gate require proven same-source JaCoCo/Detekt/CPD reports;
they do not generate test execution. Audit results: **37 Python tests and 2
engineering-tool JUnit tests passed**, zero failures/skips. Synthetic CLI probes
cover below/above target, eligible unscored, ambiguity, explicit technical review,
expired review, abstract contract, absent/raw/empty/malformed reports, incomplete
mapping, omitted Kotlin source root, stale sources and unchanged historical risk.
Formula tests include known scores and genuine zero coverage. Logs remain ignored
under checkpoints/compliance; no private fixtures were used.

For source provenance reconstruction:

```powershell
python tools/forensic_inventory.py --prepare-snapshots
.\gradlew.bat :engineering-tools:sourceInventory '-PinventorySource=checkpoints/compliance/snapshots' '-PinventoryOutput=checkpoints/compliance/history-inventory.xml'
python tools/forensic_inventory.py --mutation-artifact checkpoints/github-ci/42469281768-13613225849.zip
```

The final command also needs the verified previous/latest function-mapping JSONs
and latest JVM coverage XML exported from the cited artifacts as
`checkpoints/compliance/{previous,latest}-function-mapping.json` and
`latest-coverage.xml`, plus the same-source Detekt summary. The optional mutation
ZIP records actual method outcomes; omitting it does not imply mutations executed.
The preparer extracts only tracked `.kt` files, never recordings/emulator data.

Before continuing reconstruction: retain this enforcement, fix/validate standalone
anonymous MCC and Compose descriptor mapping, and target workspace orchestration
plus the seven identified survivor contracts. This audit does not reopen unrelated
DSP/persistence implementation. Main, official skill, Samsung and private WAVs
remain unchanged; no merge or broad reconstruction is performed here.
