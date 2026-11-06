# AGENTS.md — Didge Taller (Kotlin / Android)

Communicate with the user in clear Spanish. Write new source comments, agent instructions, technical documentation, and commit messages in English. Preserve existing text and history unless the task calls for a change.

Keep `README.es.md` and Spanish Android resources in Spanish. Apply the classification rules in [LANGUAGE_POLICY.md](docs/LANGUAGE_POLICY.md) before translating legacy strings, fixtures, or historical records.

## Start with the task

- Inspect `git status`, the current branch, relevant changes, and `.gitignore` before editing. Preserve prior work. If relevant project changes are already uncommitted, make a truthful, privacy-reviewed baseline commit before new source edits; never create an empty checkpoint.
- Read only the source and documentation needed for the task. `docs/PRODUCT_CONTEXT.md`, `docs/ROADMAP.md`, `docs/VALIDATION.md`, `docs/WORK_LOG.md`, and historical Git versions are references to consult when relevant, not mandatory full reads on every turn. Existing successful evidence may be reused when its inputs have not changed.
- Use the local [ub-mode skill](.agents/skills/ub-mode/SKILL.md) for code implementation, debugging, refactoring, and review. The file is the unmodified official methodology. Project-specific safety, Kotlin tool equivalents and risk-based execution are defined here and in [QUALITY_GUIDE.md](docs/quality/QUALITY_GUIDE.md).
- The code is the source of truth when older documentation conflicts with current behavior. Do not restart completed investigations or treat historical version notes as active instructions.

## Engineering and data safety

- Prefer small root-cause fixes, clear ownership, readable Kotlin, behavior-focused tests, and no speculative abstractions. Keep audio estimation, acceptance, persistence, and presentation distinct. Preserve uncertainty instead of inventing pitch or bridging silence.
- Preserve original WAVs, sessions, indexes, preferences, package ID `ar.didgetaller`, signing compatibility, and both instrument profiles. Do not commit private audio, fixtures, screenshots, backups, secrets, generated files, or signing material. Do not upload private recordings.
- Never run instrumentation on the physical Samsung. Select the emulator serial explicitly; do not use an unqualified connected-test task while the phone may be attached. Never uninstall, clear, restore over, or destructively modify Samsung data. Physical installation requires applicable user authorization, compatible package/signature, a read-only data baseline, `adb -s SERIAL install -r`, and post-install integrity checks.
- Public/full quality runs must use a clean emulator with synthetic data. Protected fixtures require current user authorization plus the explicit `allowPrivateFixtures=true` instrumentation argument; file presence alone never grants permission. Preserve existing private emulator fixtures without reading or deleting them.
- Do not install tools, add dependencies, change CI gates, publish, or push merely because a skill suggests them. Follow the current user authorization and report unverified claims honestly.

## Validation proportional to risk

- **Instructions, documentation, and localization text:** inspect the affected diff, links or resource formats, and the relevant localized strings. Run a focused resource check if resources changed. No unrelated Android build or instrumentation.
- **Small production change:** run directly related tests and the smallest build/check that catches integration errors. Add a regression test when behavior can be meaningfully automated.
- **UI, navigation, or charts:** run focused JVM/Compose tests and inspect the changed screen or interaction on an explicitly selected emulator when visual behavior matters. Test relevant locale, orientation, and font-size cases.
- **Audio, persistence, recording, or lifecycle:** use deterministic affected-area regressions, protected real fixtures only when relevant and available, and read-only data-integrity checks. Distinguish synthetic results from physical accuracy.
- **Broad or release-critical change:** run the full required gates once on the final state, including emulator instrumentation when relevant. Inspect `.github/workflows/` before relying on CI. Fast checks and manual deep checks have different scopes; only an actual emulator run counts as instrumentation. Never represent a partial or historical run as a final pass.
- Diagnose a failure before rerunning. Use bounded retries only for evidenced infrastructure faults, preserve logs, and report unresolved gates. Do not suppress tests or relax meaningful assertions for a green result.

## Efficient execution

- Search scoped paths and read relevant excerpts. Redirect verbose Gradle, emulator, and analysis logs to ignored local files; report short results and failure excerpts. Avoid repeated full-repository scans, unchanged successful suites, redundant experiments, and repeated progress summaries.
- Use existing test infrastructure and reusable evidence. Do not create a new framework or invoke optional tools such as mutation testing without a specific benefit. Do not spawn extra agents for routine work.
- Record material validation and limitations in `docs/VALIDATION.md`; record material decisions or handoff state in `docs/WORK_LOG.md`. A trivial edit needs no ceremonial log update. Keep reports proportional to the change.

## Git and delivery

- Before the first commit of an iteration, verify `git config --get user.name`, `git config --get user.email`, and the effective `git var GIT_AUTHOR_IDENT` / `git var GIT_COMMITTER_IDENT`. Use only the user's configured identity; do not invent or change it.
- Commit validated, coherent changes with English messages. Stage explicit paths (`git add -- <paths>`), then review staged names, diff, binary files, `git diff --cached --stat`, and `git diff --cached --check`. Do not use blanket staging, reset/clean to discard work, amend/rebase to rewrite history, or force-push.
- Push only when explicitly authorized for the current work and a normal safe fast-forward is possible. Check outgoing privacy and licensing, verify remote HEAD, and report CI as passed, failed, or pending based on the actual run. Never change repository visibility automatically.
- Finish with the commit hash, working-tree/remote status, tests actually run, and material limits. Historical release evidence remains in `docs/VALIDATION.md`, `docs/WORK_LOG.md`, and Git history; it is not an active command list.

## Executable quality policy

See [Quality guide](docs/quality/QUALITY_GUIDE.md) for verified commands, report limits and current failures. Documentation-only work uses links/syntax checks, not deep analysis. Small UI work needs focused JVM/UI and screenshots; recording/playback/persistence needs state/lifecycle/integration tests; acoustics needs deterministic affected-area regressions. Architecture/CI changes require broad applicable checks. Full quality work adds both JVM variants, emulator instrumentation, coverage, complexity, duplication and selected mutation analysis. Preserve uncertainty and never tune algorithms to improve metrics.

The official skill's Crapper/Dryer/Mutator objectives are pursued through the explicitly documented Kotlin tooling; do not mislabel numeric equivalents. Keep a concise decision/evidence log in docs/quality and ignored full logs, rather than shell transcripts in conversation. Do not install optional tools, clean worktrees or operate physical devices solely because the upstream skill recommends it. Invocation does not grant publication or installation authority.
