# Comparison ownership and accessible plot verification

Status: whole application **IN PROGRESS**. This report covers executable source
11fa244f27160bf0401312d64ea2742917137e27, following52bb4f5 andc9b3ab3. Original main
3a160043f9ebb3a420b1d89a9cf4ddfe7d1b5d67 remains unchanged. PR1 remains draft.
Documentation-only descendants may reuse these exact unchanged executable inputs.

## Observed remote execution

| Workflow | Source | Actual result |
| --- | --- | --- |
| [Fast42650147321](https://github.com/hmeclazcke/DidgeTaller/actions/runs/42650147321) |11fa244| Passed |
| [Full emulator42650133214](https://github.com/hmeclazcke/DidgeTaller/actions/runs/42650133214) |11fa244| Passed; full suite and five extra configurations |
| [Real mutation42614248589](https://github.com/hmeclazcke/DidgeTaller/actions/runs/42614248589) |c9b3ab3| Passed; unchanged selected scope reused |

The earlier [native42614242780](https://github.com/hmeclazcke/DidgeTaller/actions/runs/42614242780)
is FAILED: main suite152 passed/ten skips, followed by real ES150% notice overflow.
It is not silently reclassified as successful. Its extra EN case passes; ES fails;
landscape is not reached. Native reproduction identifies vertical padding, then
an absent landscape frame. The follow-up reserves a readable minimum graph,
grows into spare space and permits intentional scrolling of enlarged secondary
text. Both full paragraphs and axis bounds remain asserted; no font shrink or
assertion/quality-policy relaxation is used. Duplicate automatic fast push
cancellation is not a pass. No extra manual mutation run was dispatched.

## Production and test evidence

- [Comparison](SWIPE_OPERATION_OWNERSHIP.md): native ViewModelStore.clear/enable
  RED exposes a running state in a cancelled owner scope. Typed session requests,
  terminal ownership, permanent disposal and shared serial scheduling replace
  mixed ViewModel coroutine/Handler publication. Cached curves belong to their
  session; stale/duplicate outcomes cannot publish. Serial SWIPE equations and
  reader use remain unchanged.
- Camera reconciliation is pure Kotlin with four behavior tests/12 killed
  mutants. Progress preserves pan, enabling adjusts the range once, disposal
  restores originals and missing camera models are not recreated.
- A cancellation survivor exposes an assertion trapped in cancelled work; the
  tests now observe results on the caller thread. Focused analysis67 mutants:
  57 killed/ten survived; comparison81:72/nine, zero uncovered/tool errors. These
  selections overlap shared scheduling and are never summed.
- [Annotation](PLOT_NOTICE_OWNERSHIP.md): the viewer retains original message
  policy while native rendering owns measured insets. The passive notice does
  not steal seek/scrub. Native text-layout assertions require complete text,
  last-character visibility, a drawable frame and no axis or parent clipping.
- Ten final API37 focused cases pass: three EN/ES/landscape150% evidence cases,
  three readiness/notice interaction cases, actual fullscreen paused seeking,
  new portrait/landscape pinch/pan/scrub contracts at80ms, and existing complete
  pinch/pan/rotation/reset at normal scale. The old65% normal-font area assertion
  is unchanged; its extension to150% rejects a disproportionate area after gesture
  assertions pass. This is not represented as a passing large-font area guarantee.
- Preserve rejected local evidence: startup System UI/Pixel Launcher ANRs and
  incorrect positional pinch arguments in the new driver. Named finger arguments
  fix that test; no production gesture algorithm is tuned to accommodate it.
  Original and final PNGs are authentic owned-emulator synthetic captures.

## Reproducible measured scopes

| Measured category | Previous source8b73c21 | Source11fa244 |
| --- | ---: | ---: |
| JVM discovered / passed / skipped |896 /880 /16|936 /920 /16|
| Android discovered / passed / skipped |158 /148 /10|163 /153 /10|
| Extra real system-configuration cases |5 passed|5 passed|
| JVM line / branch coverage |42.58% /38.83%|43.39% /39.70%|
| Pure core JVM line / branch |99.04% /83.52%|99.07% /83.78%|
| Separate Android line / branch |91.94% /68.47%|92.02% /68.62%|
| CRAP-L scored / independent unscored |470 /111|484 /114|
| Above-target JVM / Android |73 /29|72 /28|
| Actual broad PIT classes / mutants |64 /2250|70 /2318 (c9b3ab3 scope reused)|
| Detekt informational findings |1989|1987|
| Lint errors / warnings / hints |0 /52 /8|0 /53 /8 (Linux)|
| CPD groups >=100 tokens |0|0|
| Original-main synthetic differential |741 identical|741 byte-identical|

Linux JVM468 discovered/460 passed/eight protected skips per variant, no failures.
Complete fresh API35 x86_64 emulator suite163 discovered,153 passed,ten justified
skips,zero failures. Its real runner summary is OK(163 tests). Two library and
three workspace platform-configuration cases each pass independently: EN/ES,
720x1280 at150% and ES1280x720. Raw logs and six current-source CI PNGs are checked;
large-font scientific text is complete after deliberate scrolling in landscape.
No Android compilation is misrepresented as execution.

Android coverage belongs to the complete suite only, before the five extra cases:
5272/5729 lines,4430/6456 branches. Do not merge it with JVM. Package lines/branches:
core95.33%/69.54%, persistence94.92%/65.24%, UI90.66%/69.87%, Android audio91.67%/64.63%.
The JVM denominator keeps framework/UI code; zero JVM execution is not silently
excluded. Coverage fractions change with source/generated code and platform;
the UI-only correction slightly lowers JVM lines relative toc9b3ab3. The before/
after table does not attribute all changes to this final padding fix.

Local correction debug JVM460 passed/eight skips; lines43.38%,branches39.61%,
core99.02%/83.55%. Local Lint has52 warnings/eight hints; Linux has one additional
AndroidGradlePluginVersion metadata advisory saying a newer Gradle is available.
The issue is wrapper update metadata, not a new application-source warning; it
is preserved, not suppressed, and no unrelated toolchain upgrade is attempted.
Static/build/localization, all45 Python gate tests and source quality checks pass.
No new duplicate, numerical gate-baseline relaxation or missing ordinary score.

Artifacts on the linked runs: Quality-Metrics-and-Test-Results,
DidgeTaller-JVM-Coverage, DidgeTaller-Detekt, Complete-Emulator-Results and
Kotlin-Mutations. Seven-day retention; no WAV/private session content is uploaded.
Complete function-level mappings, XML test results, reports and synthetic PNGs
are evidence; [quality guide](../quality/QUALITY_GUIDE.md) gives reproducible commands.
Fast job duration7m08s; native job10m34s, distinct from test or mutation engine time.

484 scored/114 independent unscored account for638 named declarations:
484 measured,36 parent-accounted local functions,32 abstract,13 anonymous-owner,
73 Compose. Four local anonymous implementations are not double-counted.
Relative to source8b73c21,17 new named declarations =14 measured,two abstract,
one Compose. No new eligible ordinary callable is silently omitted or waived.
Anonymous Detekt MCC=0 and transformed Compose correspondence remain explicit
technical limitations; zero executed coverage is never called unavailable.

MCC setSwipeComparison15 ->2; owner maximum5, scheduler4, camera4, publication1.
SessionPlot74,viewer68,PlotSurface8,canvas7,notice2 are unchanged by the layout
correction. High viewer complexity remains reconstruction work; UI accessibility
does not claim to solve it. Gates reject missing reports, fresh unreviewed source
fingerprints and ordinary scoring gaps; two existing exact Compose reviews are
renewed for the tested source. No numeric baseline or ordinary risk waiver changes.

PIT on70 actual bytecode classes generates2318 mutants:1807 killed,494 survived,
15 uncovered,two timed out,zero errors. Killed1807/2318=77.96%; detected1809/2318
=78.04% including timeouts. Engine98s is not provisioning/job duration. Focused
mandatory gate11/11; broad score informational, nonempty/error-free execution
mandatory. Selected source/JVM tests/build scopes are unchanged by11fa244's two
Compose file changes, verified against the actual mutation XML class list.
This is reused selected-scope evidence fromc9b3ab3, not a fictitious11fa244 run.
Broader scope/retargeted coroutine bytecode prevent naive score comparisons.

## Limits and continuation

Both READMEs are updated equivalently and their public blobs/actual GitHub HTML
rendering verified. Existing galleries, Spanish resources and scientific claims
are preserved. No CPU/memory/frame-rate benchmark improvement is claimed.
Synthetic differential outputs remain741 identical against pinned main; this
is DSP evidence, not proof of pixel equality or physical acoustic accuracy.

Next production cycle: saved-YIN/component/evidence admission after disposal
and session replacement, deterministic RED before rewriting the actual owners.
Remaining requestMusicalTrace/DSP/UI hotspots, above-target functions and meaningful
survivors are not complete. [Checkpoint](REBUILD_PROGRESS.md),
[case study](REBUILD_CASE_STUDY.md). Main not changed/merged, official skill/MIT
unchanged, no Samsung/private WAV access, no destructive migration or metronome.

Official upstream pin3da62bf3fcfd2034de41f30d920d2739e566171f; local skill SHA256
4120873db2e3681ea9b7de4d7105fc9d3406657c9ada91ce77b9b3c4962f3202, unchanged.
MIT notice preserved. This establishes current loading/integrity, not an invented
history of every previous Codex skill invocation.
