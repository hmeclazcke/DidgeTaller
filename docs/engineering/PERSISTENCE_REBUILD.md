# Persistence reconstruction

Status: **IN PROGRESS** within the application rebuild. Original main remains
`3a160043f9ebb3a420b1d89a9cf4ddfe7d1b5d67`; implementation started from feature commit `5f93e23`.

## Demonstrated defects and design

Three new emulator tests failed on the preceding implementation: a damaged nonempty
index authorized deletion, deleting another session dropped an unknown JSON row, and
duplicate identifiers produced duplicate library entries. The failed evidence is retained
locally in `checkpoints/persistence-red.txt`. All fixtures use unique test-owned namespaces.

`RecordingStore` now owns WAV/index ordering rather than parsing metadata and silently
inventing an empty database. `RecordingMetadata`, `MeasurementMetadata` and
`SessionToolsMetadata` own wire-format validation; mathematical detectors are unchanged.
`StoredDocument` owns a complete locked read/modify/publish transaction behind a small
storage port. `JsonPreferenceDocument` adapts Android preferences and Android's JSON codec.
All store instances use the same cached preference object's process-local monitor.
This is a single-process guarantee, not a cross-process filesystem lock.

The existing array/object formats, keys, IDs, names, profile fallback and historical
timestamps remain readable. There is no schema migration. An unsupported root/version
envelope is damaged/unsupported, never an empty authoritative array. Android org.json's
existing permissive grammar remains; root type and trailing content are checked explicitly.

## Invariants and recovery

- Missing initial document and valid empty array are legitimate; empty text, malformed JSON,
  unsupported roots and a missing previously backed-up document are distinct damaged states.
- Reads never rewrite original metadata. A validated last-good copy can supply display data;
  writes remain blocked until explicit recovery. A WAV without readable metadata remains recoverable.
- Recovery requires a valid backup and archives the exact damaged text under a SHA-256 keyed
  quarantine entry before restoring the index in the same preference transaction. No backup
  means no automatic empty replacement. Repeated recovery is harmless.
- Every complete publication includes an additive last-good JSON key. This protects an
  individual JSON document, **not** loss/corruption of the entire preference XML or storage device.
- Invalid individual records do not destroy other records. Unknown JSON rows/fields survive
  unrelated mutations. The first readable occurrence of a duplicate session ID is displayed;
  source duplicates remain preserved. Invalid frequency/confidence at a valid time becomes
  an explicit gap; no interpolation or detector substitution occurs.
- WAV contents are prepared before index publication. Invalid new metadata is rejected before
  WAV rename. If index publication fails afterward, the complete WAV remains an orphan that
  can be recovered. No recovery deletes orphan WAVs.
- Permanent deletion validates the ID, successfully publishes metadata, then deletes that WAV.
  A failure or interruption before deletion leaves the WAV intact. A crash between publication
  and deletion may leave an orphan; preservation takes priority over automatic cleanup.
- Failed commits do not authorize deletion. Android updates preference memory before disk I/O;
  a best-effort rollback restores prior in-process values, while the caller receives a failure
  explicitly stating unknown durability. A retry re-reads current state under the shared lock.
- Saving byte arrays syncs the temporary WAV before rename. Neither rename nor SharedPreferences
  is claimed to guarantee directory-entry durability across every filesystem/power loss.
  Existing progressive capture finalization semantics are preserved.

`SessionToolsStore`, `TrashStore` and legacy `LocalStore` use this same document boundary.
They no longer publish stale independent read/modify/write snapshots. This does not make a
multi-store trash-emptying operation one cross-file transaction; successful earlier operations
remain durable and later failures are retried without destructive recovery.

## Verification

Before the subsequent state/UI cycle: both JVM variants executed 271 tests each, 263 passed,
8 protected skips each, zero failures. The focused Android persistence run executed **25/25**
on the owned API 37 emulator; it includes existing capture and historical-format regressions.
Coverage/CRAP and mutation reports are generated artifacts, not committed personal data.
The first two-class PIT iteration killed 90/109 mutants; added cancellation/hash/boundary
assertions killed 107/121 on the expanded implementation of those two classes. Later runs
must identify their larger scope separately. Compiler-generated checks remain reported.

The unchanged quality ratchet passed with 357 mapped functions, 75 explicitly unscored and
79 above-target JVM CRAP-L scores. The denominator increased from the preceding 330/71;
these totals alone are not an architectural score. No baseline was reset. Four removed
method identities have explicit reviewed lineage to the new publication/codec owners.
Actual Android coverage is separate and must be regenerated for this source revision.

Commands are in the [quality guide](../quality/QUALITY_GUIDE.md). The new focused command is
`./gradlew :app:qualityMutationPersistence`; its score is informational, while successful
execution and a nonempty report are required. Real Android JSON/preferences/filesystem tests
remain essential: the JVM-only org.json test dependency is not Android's implementation.

Remaining work includes library recovery UX/state ownership, wider lifecycle/UI reconstruction,
process-death fault injection, whole-preference/storage-device disaster recovery and the
application-wide architecture acceptance criteria. No physical device or private audio was used.
