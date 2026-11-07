# Uncle Bob rebuild — IN PROGRESS

- Branch: `refactor/uncle-bob-rebuild`.
- Worktree: `C:/projects/DidgeTaller-uncle-bob`; original main checkout retained separately.
- Pinned baseline: `3a160043f9ebb3a420b1d89a9cf4ddfe7d1b5d67`.
- Main must never receive commits/pushes/merges from this effort. No physical-device access or private recordings.
- Official ub-mode SKILL.md/LICENSE byte-verified against `3da62bf3fcfd2034de41f30d920d2739e566171f`; unchanged.

## Verified foundation

- Baseline reproduced: both JVM variants, JaCoCo, Detekt/MCC and CPD passed; full logs ignored in checkpoints.
- Kotlin compiler PSI inventory: 59 files, 378 named functions, 1,455 lambdas, 73 explicit accessors.
- Conservative mapper now correlates 276 functions (rather than 8), and classifies every other named declaration: 57 Compose, 41 local functions included in parent complexity, 3 abstract declarations, 1 anonymous owner.
- The expanded JVM perspective exposes 97 high scores; this is newly visible existing debt, not an improvement. Android method coverage is measured separately.
- Parser ownership/overload/local-function test passes; 16 Python tests pass including eight new ambiguity/boundary correspondence cases.
- Original production bytecode frozen locally in ignored checkpoints/baseline-production.jar. A synthetic differential executable is being established before DSP changes.

## Review and execution order

Reviewed directly: recording/playback/workspace adapters; persistence/cache stores; PitchDetector/PitchTrace; WAV reader/writer/encoder; canonical MusicalTrace; music/geometry/profile/stream/acceptance core; first analysis ownership portion of ViewModel. Full 59-file review is NOT yet complete.

1. Finish differential probe and pin observable DSP/trace/geometry/WAV behavior.
2. Separate YIN window preparation, CMNDF search/refinement and acceptance from visualization; preserve arithmetic, thresholds and diagnostic contracts.
3. Establish explicit playback/capture ownership and domain state tests; characterize error paths before replacement.
4. Complete remaining core, ViewModel and UI source review; rebuild identified responsibilities in verified increments.
5. Integrate broad mapping into existing ratchets without disguising expanded measurement as improved code. Extend PIT across critical pure logic and investigate survivors.
6. Validate branch CI, fresh synthetic emulator suite, locale/compact/150%-font screenshots and differential/persistence/performance checks.
7. Publish branch only and maintain a draft PR; preserve main. Update bilingual README and case study with actual evidence.

## Acceptance state

IN PROGRESS. Comprehensive architecture changes, high-risk reduction, broad mutation, complete UI review, final emulator/differential/persistence/performance validation and final scorecard remain unmet. Do not advertise a completed reconstruction or reuse baseline metrics as final results.
