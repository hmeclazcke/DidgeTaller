---
name: ub-mode
description: Use for Didge Taller code implementation, debugging, bug fixes, refactoring, and code review using Uncle Bob-inspired engineering principles, behavior-focused tests, and small verifiable changes.
---

# Uncle Bob-Inspired Engineering Mode

**First Make it Work → Then Make it Right → Practice.**

Apply this workflow to the current task, without expanding its scope. Read the
repository's [AGENTS.md](../../../AGENTS.md) and only the project context relevant
to the task. Scale the depth of investigation and validation to the risk described
there; the phases below are a way to reason, not a fixed list of tool calls.
Existing repository safety, data protection, testing, and Git requirements take
precedence over this skill. Invoking `$ub-mode` does not authorize tool installation,
device operations, publication, or a push. Write new instructions, technical
documentation, and commit messages in English; preserve existing language and history.

## First Make it Work

### Laziness Protocol

- One decision lives in one place: keep each business rule with its responsible component.
- Ship the smallest edit that solves the problem. Avoid speculative frameworks.

### Foundational Thinking

- Get the data shape right before writing logic. Identify inputs, outputs, invariants,
  ownership, and lifetimes.
- Prioritize decisions that simplify later phases and avoid unnecessary shared mutable state.

### TDD

- For behavior changes with a meaningful automatable contract, prefer a focused failing
  test before the fix. Confirm that it fails for the expected behavioral reason, rather
  than a broken environment. This is especially valuable for regressions and risky logic;
  do not require a red test for a text edit or another low-risk change it cannot protect.
- Make the smallest change that passes the relevant check, then refactor where it
  improves clarity while preserving the verified behavior.
- Do not write dishonest, tautological, or meaningless tests when meaningful automation
  is impractical. Record the limitation and use a reproducible manual check or concrete
  evidence instead. For instruction-only or configuration work, validate its real format
  and discovery rather than inventing unrelated application tests.

### Redesign From First Principles

- Consider the design that would exist if the new requirement had been present from
  the beginning. Improve toward it incrementally, without unnecessary rewrites.

### Attack the Premise

- If successive fixes based on the same assumption fail, stop repeating that assumption.
  Revisit the evidence and identify which component actually owns the problem before
  trying another fix.

### Subtract Before You Add

- Remove unnecessary complexity before adding abstractions, state, filters, or heuristics.
  Preserve domain responsibilities and scientifically meaningful information.

### Outcome-Oriented Execution

- Define observable, verifiable outcomes and the evidence needed to demonstrate them.
- Work through small, reversible steps; distinguish implementation from verified results.

### Build the Lever

- For repeated or nontrivial work, prefer a focused tool, replay, or reproducible test
  over repeated manual actions. Keep helpers scoped and protect private inputs.

### Architecture and Domain Modeling

- Model the domain explicitly and maintain a consistent glossary with the existing code
  and product documentation. Distinguish recordings, measurements, estimates, confidence,
  and rendered traces rather than treating them as interchangeable.
- Make invalid states difficult or impossible to represent; use Kotlin types, nullability,
  and sealed alternatives where appropriate instead of suppressing compiler warnings.
- Validate data at system boundaries and prefer pure, testable domain logic.
- Make operations idempotent where appropriate; minimize shared mutable state and give
  unavoidable state an explicit owner and lifecycle. Define concurrency ownership,
  cancellation, and thread boundaries for asynchronous audio or Android work.

## Then Make it Right

### Fix Root Causes

- Reproduce bugs before fixing them. Trace inputs and decisions to the responsible layer;
  fix underlying causes instead of concealing symptoms.

### Sequence Verifiable Units

- Prefer small, independently verifiable units when the task has multiple changes.
  Keep failed attempts distinguishable from validated results; avoid a full validation
  cycle after each trivial edit.

### Test Behavior, Not Implementation

- Assert observable outcomes and relevant boundary cases. Reject tests that still pass
  when the feature is broken, or that merely duplicate the implementation.

### Complexity Analysis

- When complexity and test effectiveness matter to the task, apply the intent behind
  Uncle Bob's CRAP metric: prioritize complex code whose behavior lacks meaningful
  coverage. Combine measured complexity with coverage only when compatible data exists.
- Never claim a CRAP score, enforce a CRAP threshold, or equate coverage with effective
  tests unless the required measurements were actually calculated and interpreted.

### Duplication Analysis

- Apply DRY to duplicated knowledge and business rules. Similar text or code may serve
  different responsibilities; do not merge it mechanically or create false coupling.

### Mutation Testing

- When suitable tools are available and justified, use a focused mutation run to assess
  test effectiveness. Investigate surviving mutations and distinguish equivalent mutations
  from untested behavior. Do not start expensive project-wide mutation testing by default.

### Refactoring

- Characterize existing behavior, improve structure, and verify that observable behavior
  remains unchanged. Separate intentional behavior changes from structural cleanup.

## Kotlin and Android Tool Compatibility

This skill requires no installed analysis or mutation tools. Do not assume the
Java-oriented `crapper`, `dryer`, or `mutator` utilities support Kotlin source code.

| Purpose | Optional Kotlin-compatible approach | Check before using |
| --- | --- | --- |
| Static analysis and complexity | Detekt | Availability and compatibility with the project's Kotlin and Gradle versions |
| Coverage | JaCoCo or another compatible coverage solution | Kotlin/Android support, report configuration, and interpretation of generated code |
| Duplication | PMD CPD with Kotlin support | The installed version's Kotlin language support and the intended source scope |
| Mutation testing | A verified Kotlin-compatible solution | Actual support for this Kotlin/Android toolchain, bytecode behavior, cost, and task scope |

These optional tools are not installed as part of this skill. Before using one, verify
availability and suitability. If unavailable, report that the measurement was not run
and use existing tests, focused code inspection, and reproducible checks. Do not fabricate
tool output, metric values, or coverage. Do not install tools or change Gradle dependencies
without authorization for that separate work.

## Didge Taller Engineering Rules

### Audio and DSP

- Preserve raw WAV recordings and original measurements, including uncertainty and gaps.
- Validate audio changes with reproducible signals and, when available, private real
  recordings in read-only replays. Report missing recordings or reference measurements;
  synthetic success is not physical acoustic validation.
- Distinguish detector behavior, confidence classification, and graphical presentation.
  Find the owning layer before changing thresholds or rendering rules.
- Never invent frequency estimates to hide uncertainty. Preserve support for both
  Didgeridoo and Native American Flute, including profile-specific behavior.

### Visualization

- Test observable chart behavior and inspect actual screenshots when visual chart
  behavior changes.
  Preserve scientifically meaningful data, original readings, and access to uncertainty.
- Avoid misleading interpolation, filtering, or scaling. A successful build alone does
  not verify a chart; record visual evidence and any remaining validation limits.

### Android

- Preserve application data and follow the repository's lifecycle and migration rules.
- Run instrumented tests only on an explicitly selected emulator, never the user's Samsung.
  Never run an unqualified connected-test task when it could target that phone.
- Do not perform destructive migrations or uninstall operations. Never clear app data
  or automatically restore over the Samsung. Follow the stricter existing backup and
  update requirements.

### Git and CI

- Follow the repository's existing Git, identity, validation, staging, and GitHub rules.
  Keep changes small, validated, and recoverable; review explicit staged paths and diffs.
- Push to `origin/main` only with authorization under repository policy and after checking
  the proposed changes for private or sensitive files. Skill use itself is not authorization.
- Never force-push or rewrite history without explicit approval. Never commit private WAV
  recordings, secrets, signing keys, backups, or local fixtures.
- Preserve CI tasks and project versions unless the authorized task requires a concrete
  change. Check triggered CI results and report actual failures without claiming success.

## Practice

- Record material decisions, validation, and limits in the repository's existing
  WORK_LOG and VALIDATION documents when the scope warrants it. A small edit does not
  require a log entry for every action. Explain why the change was made and what was
  actually verified in the final report.
- Protect the available context window: retain constraints, decisions, and evidence
  locations; read focused references instead of copying entire logs or this skill repeatedly.
- Keep changes small and recoverable. Avoid destructive cleanup and preserve previous work.
- Create small, descriptive Git commits after relevant checks. Follow repository GitHub
  and CI policies; never push without applicable user authorization. State the final
  commit, synchronization status, and unresolved limits.
