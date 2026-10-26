---
name: ub-mode
description: Use for Didge Taller code implementation, debugging, bug fixes, refactoring, and code review with small, verifiable changes.
---

# Uncle Bob-Inspired Engineering Mode

Apply only to the task at hand. Repository `AGENTS.md` and current user instructions control scope, safety, Git, and validation. This skill requires no extra tools or dependencies; using it does not authorize installation or publication.

## Work → verify → improve

1. Identify the observable problem, responsible layer, inputs, outputs, and important invariants. Reuse existing evidence before investigating again. For a bug, reproduce the smallest meaningful failure and fix its cause.
2. Choose the smallest coherent change. Keep one business decision in one owner; use clear names, explicit data shapes, and testable boundaries. Apply DRY to duplicated knowledge, not merely similar syntax. Avoid speculative frameworks, filters, and shared mutable state.
3. For behavior changes, write a focused failing test first when it will meaningfully protect the contract; confirm the failure is behavioral. If automation would be tautological or impractical, use a reproducible manual check and state that limit. Instruction-only work needs format/discovery checks, not application tests.
4. Make the change, run risk-appropriate checks from `AGENTS.md`, then refactor only where it improves clarity. Verify observable behavior rather than mirroring implementation details. Reuse unchanged successful results; diagnose failures before bounded retries.
5. Report what changed, the evidence, and remaining limits. Record material decisions and validation in existing docs when useful; avoid routine log edits for tiny changes.

## Domain safeguards

- For audio, preserve raw recordings and measurements. Keep pitch candidates, confidence decisions, musical interpretation, and chart rendering separate. Synthetic references do not establish physical instrument accuracy. Do not invent a frequency to fill an uncertain interval; preserve didgeridoo and flute behavior.
- For chart changes, inspect actual changed views when visual behavior matters, preserve gaps and original readings, and avoid misleading interpolation or scaling. A build alone does not prove visual correctness.
- For Android, preserve data and lifecycle behavior. Instrument only a selected emulator; never run instrumentation, uninstall, clear, or restore over the user's Samsung.
- For Git, follow the repository's identity, explicit staging, privacy review, commit, and push rules. Never publish private fixtures, secrets, or recordings.

## Optional analysis

Use Detekt, JaCoCo, duplication, complexity, or mutation tools only when available and informative for the risk or a required gate. Coverage is not proof of effective tests. Do not invent metrics, add dependencies, or perform project-wide mutation runs by default. If repeated fixes fail, revisit the premise and the responsible component before adding another rule.
