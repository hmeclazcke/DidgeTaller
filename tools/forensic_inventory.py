"""Compare PSI snapshots and archived metrics, never infer source provenance from totals.

Snapshot inputs contain Git-tracked Kotlin source only, extracted with git archive.
Generate history-inventory.xml using sourceInventory with inventorySource pointing
at checkpoints/compliance/snapshots. No application or private fixtures execute.
"""
import argparse
from collections import Counter
import io
import json
from pathlib import Path
import subprocess
import tarfile
import xml.etree.ElementTree as ET
import zipfile

PREVIOUS = '9cd18f69d6aaecbdd1e1f8c644347d4d2103836c'
LATEST = '672c9b2120a7c869cd4331699ba85017945a0db4'


def snapshots(path):
    revisions = {}
    for source in ET.parse(path).getroot():
        suffix = source.get('file').split('/snapshots/', 1)[1]
        revision, file = suffix.split('/', 1)
        rows = revisions.setdefault(revision, {})
        for entry in source:
            row = dict(entry.attrib, file=file, kind=entry.tag)
            key = file + '::' + row['identity']
            if key in rows:
                raise ValueError('Ambiguous authored identity: ' + key)
            rows[key] = row
    return revisions


def metric_key(row):
    return row['file'], row['owner'], row['signature']


def change_inventory(before, after):
    changes = []
    added, removed = set(after) - set(before), set(before) - set(after)
    # Exact token-body and unique signature pairs identify relocation without
    # guessing from names alone. Ambiguous similarities remain added/removed.
    for key in sorted(added.copy()):
        row = after[key]
        candidates = [old for old in removed if row['kind'] == before[old]['kind']
                      and row['semanticHash'] == before[old]['semanticHash']]
        inverse = [new for new in added if before[candidates[0]]['kind'] == after[new]['kind']
                   and before[candidates[0]]['semanticHash'] == after[new]['semanticHash']] if len(candidates) == 1 else []
        if len(candidates) == 1 and len(inverse) == 1:
            old = candidates[0]
            changes.append(dict(row, change='moved', previous_identity=old))
            added.remove(key)
            removed.remove(old)
    for key in sorted(set(before) & set(after)):
        if before[key]['semanticHash'] != after[key]['semanticHash']:
            changes.append(dict(after[key], change='modified'))
    changes += [dict(after[key], change='added') for key in sorted(added)]
    changes += [dict(before[key], change='removed') for key in sorted(removed)]
    return changes


def coverage_candidates(row, coverage):
    if row.get('kind', 'function') != 'function':
        return []
    package_source = row['file'].split('/main/', 1)[-1].split('/', 1)[-1]
    result = []
    for package in coverage.findall('package'):
        for owner in package.findall('class'):
            if package.get('name') + '/' + owner.get('sourcefilename', '') != package_source:
                continue
            for method in owner.findall('method'):
                if method.get('name') != row['name'] or not method.get('line'):
                    continue
                if not int(row['start']) <= int(method.get('line')) <= int(row['end']):
                    continue
                result.append(dict(owner=owner.get('name'), descriptor=method.get('desc'),
                    counters={c.get('type'): {k: int(c.get(k)) for k in ('covered', 'missed')}
                              for c in method.findall('counter')}))
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--evidence', type=Path, default=Path('checkpoints/compliance'))
    parser.add_argument('--output', type=Path, default=Path('docs/quality/FORENSIC_FUNCTION_INVENTORY.json'))
    parser.add_argument('--mutation-artifact', type=Path, help='Verified 42469281768 Kotlin-Mutations ZIP (optional)')
    parser.add_argument('--prepare-snapshots', action='store_true', help='Extract tracked Kotlin source only; no private/application data')
    args = parser.parse_args()
    if args.prepare_snapshots:
        history = [PREVIOUS] + subprocess.check_output(['git', 'rev-list', '--reverse', PREVIOUS + '..' + LATEST], text=True).split()
        for revision in history:
            with tarfile.open(fileobj=io.BytesIO(subprocess.check_output(['git', 'archive', revision, 'app/src/main']))) as archive:
                for member in archive:
                    if member.isfile() and member.name.endswith('.kt') and '..' not in Path(member.name).parts:
                        path = args.evidence / 'snapshots' / revision / member.name
                        path.parent.mkdir(parents=True, exist_ok=True)
                        path.write_bytes(archive.extractfile(member).read())
        print(f'Prepared {len(history)} tracked source snapshots under {args.evidence / "snapshots"}')
        return
    revisions = snapshots(args.evidence / 'history-inventory.xml')
    before, after = revisions[PREVIOUS], revisions[LATEST]
    history = [PREVIOUS] + subprocess.check_output(['git', 'rev-list', '--reverse', PREVIOUS + '..' + LATEST], text=True).split()
    first = {}
    for older, newer in zip(history, history[1:]):
        for row in change_inventory(revisions[older], revisions[newer]):
            first.setdefault((row['file'], row['identity']), newer)
    old_metrics = json.loads((args.evidence / 'previous-function-mapping.json').read_text())
    new_metrics = json.loads((args.evidence / 'latest-function-mapping.json').read_text())
    old_map = {metric_key(r): r for r in old_metrics['functions']}
    new_map = {metric_key(r): r for r in new_metrics['functions']}
    coverage = ET.parse(args.evidence / 'latest-coverage.xml').getroot()
    mutations = []
    if args.mutation_artifact:
        with zipfile.ZipFile(args.mutation_artifact) as archive:
            reports = [n for n in archive.namelist() if n.endswith('mutations.xml') and 'pit-domain/' in n]
            if len(reports) != 1:
                raise ValueError('Expected one domain mutation report')
            mutations = list(ET.fromstring(archive.read(reports[0])))
    complexity = json.loads(Path('build/reports/detekt/summary.json').read_text())
    cc = {(r['file'], r['line'], r['function']): r['cyclomatic'] for r in complexity['functions_by_cyclomatic']}
    changes = change_inventory(before, after)
    for row in changes:
        row['first_relevant_commit'] = first.get((row['file'], row['identity']))
        if row['kind'] != 'function':
            row['measurement_status'] = 'authored auxiliary construct; not independently scored by existing pipeline'
            continue
        metric = new_map.get(metric_key(row)) if row['change'] != 'removed' else old_map.get(metric_key(row))
        row['measurement_status'] = metric['status'] if metric else 'not-in-historical-mapping'
        row['cyclomatic'] = cc.get((row['file'], int(row['line']), row['name']))
        row['crap_line'] = metric.get('crap_line') if metric else None
        row['line_coverage'] = metric.get('line_coverage') if metric else None
        row['coverage_candidates'] = coverage_candidates(row, coverage) if row['change'] != 'removed' else []
        unit = Path(row['file']).stem
        unit_tests = dict(TallerViewModel=['RecordingCoordinator', 'CaptureHandoff'],
            MicrophoneEngine=['RecordingCapture', 'RecordingCoordinator'],
            SessionWorkspace=['SpectrogramRaster']).get(unit, [unit])
        row['behavioral_test_evidence'] = [f'app/src/test/java/ar/didgetaller/core/{test}Test.kt'
            for test in unit_tests if Path(f'app/src/test/java/ar/didgetaller/core/{test}Test.kt').is_file()]
        row['android_evidence'] = ('app/src/androidTest/java/ar/didgetaller/ui/SessionEvidenceUiTest.kt'
            if unit == 'SessionWorkspace' else 'app/src/androidTest/java/ar/didgetaller/audio/RecordingHandoffTest.kt'
            if unit in ('TallerViewModel', 'MicrophoneEngine', 'RecordingCapture', 'RecordingCoordinator', 'CaptureHandoff')
            else 'app/src/androidTest/java/ar/didgetaller/ui/SessionNavigationTest.kt'
            if unit == 'PlaybackController' else None)
        row['test_evidence_limit'] = 'Owner-level behavioral contracts; coverage counters do not identify which assertion protects this exact function.'
        selected = [m for m in mutations if m.findtext('mutatedClass') == row['owner'].replace('/', '.')
                    and m.findtext('mutatedMethod') == row['name']]
        row['executed_mutations'] = dict(Counter(m.get('status') for m in selected))
        row['mutation_scope_status'] = ('executed method mutants' if selected else
            'abstract contract; no executable method' if row['body'] == 'false' else
            'Android adapter excluded from selected pure JVM mutation scope' if unit in ('TallerViewModel', 'MicrophoneEngine', 'SessionWorkspace') else
            'no direct method mutants in selected report; not proof of mutation protection')
        row['historical_gate'] = 'Measured global coverage/complexity/CRAP regression ratchets; no source-change completeness requirement.'
        row['audit_action'] = ('Review existing above-target Android orchestration; source-bound acknowledgement and explicit remaining debt.'
            if row.get('crap_line', 0) is not None and row.get('crap_line', 0) >= 10 else
            'Retain real method coverage, reject invalid MCC=0; explicit technical review of thin anonymous adapter.'
            if row['measurement_status'] == 'anonymous-owner' else
            'No standalone executable body to score.' if row['body'] == 'false' else
            'Retain reliable measurements and owner behavioral tests; strengthen meaningful surviving mutations separately.')
    # All unscored named declarations, including the nested denominator that is
    # absent from Detekt's independent metric denominator, remain visible.
    unscored = []
    psi = {(r['file'], r.get('owner'), r.get('signature')): r for r in after.values() if r['kind'] == 'function'}
    for key, metric in sorted(new_map.items()):
        if metric['status'] == 'measured':
            continue
        row = dict(psi[key], measurement_status=metric['status'])
        row['cyclomatic'] = cc.get((row['file'], int(row['line']), row['name']))
        row['in_detekt_denominator'] = (row['file'], int(row['line']), row['name']) in cc
        row['change'] = next((r['change'] for r in changes if r['file'] == row['file'] and r['identity'] == row['identity']), 'unchanged')
        row['coverage_candidates'] = coverage_candidates(row, coverage)
        row['classification'] = {'declaration-without-body': 'F: abstract contract; no executable body',
            'nested-metric-in-parent': 'E: nested complexity belongs to enclosing metric',
            'compose-transformation': 'E: transformed Compose signature; current correlator does not map',
            'anonymous-owner': 'C: missing/invalid standalone Detekt MCC despite identifiable JVM candidate'}.get(metric['status'], 'A: eligible measurement gap')
        if metric['status'] == 'anonymous-owner':
            row['technical_explanation'] = ('Detekt emits MCC=0 at this exact anonymous declaration; invalid source complexity, not zero coverage.'
                if row['cyclomatic'] == 0 else 'Detekt emits no independent MCC row for this local anonymous event implementation.')
        elif metric['status'] == 'compose-transformation':
            row['technical_explanation'] = 'Valid source MCC and one same-name/source-range JVM candidate exist; descriptor has Compose-added arguments. Current correlator deliberately skips transformed signatures. This is an investigated tool coverage gap, not inherent unmeasurability or missing test execution.'
        elif metric['status'] == 'declaration-without-body':
            row['technical_explanation'] = 'PSI hasBody=false; no method LINE denominator. Detekt MCC=1 on a declaration is not executable complexity/coverage.'
        else:
            row['technical_explanation'] = 'Local declaration is included in source inventory but has no standalone Detekt MCC/JVM source-range pair; enclosing method includes its work. Outside the 487 independent Detekt denominator.'
        row['safety_critical'] = row['file'].endswith(('TallerViewModel.kt', 'RecordingCoordinator.kt', 'PlaybackController.kt', 'JsonPreferenceDocument.kt'))
        row['recommended_action'] = {'declaration-without-body': 'Keep contract; do not fabricate score.',
            'nested-metric-in-parent': 'Review/test enclosing function; no double-counted standalone score.',
            'compose-transformation': 'Validate Compose descriptor/source correlation with known signatures and generated-body boundaries before enabling scoring; retain actual UI tests.',
            'anonymous-owner': 'Investigate Detekt anonymous-method MCC; retain real coverage separately; never replace MCC with guessed 1.'}.get(metric['status'], 'Resolve measurement before accepting new eligible logic.')
        unscored.append(row)
    added = {key: row for key, row in new_map.items() if key not in old_map}
    removed = {key: row for key, row in old_map.items() if key not in new_map}
    reclassified = [key for key in old_map.keys() & new_map.keys() if old_map[key]['status'] != new_map[key]['status']]
    result = dict(schema=1, previous=PREVIOUS, latest=LATEST,
        evidence_runs=dict(previous=42415475188, latest_source=42469282408),
        counts=dict(previous=old_metrics['counts'], latest=new_metrics['counts'],
            added_by_measurement=dict(Counter(r['status'] for r in added.values())), removed=len(removed),
            reclassified=len(reclassified), authored_changes=dict(Counter(r['kind'] + ':' + r['change'] for r in changes)),
            current_unscored_psi=len(unscored), current_unscored_detekt=sum(r['in_detekt_denominator'] for r in unscored)),
        changes=changes, unscored=unscored,
        limits=['Lexical fingerprints ignore horizontal whitespace/comments but conservatively retain line breaks; not a proof of semantic equivalence.',
                'Auxiliary constructs are newly inventoried by this audit, not newly introduced merely because first seen here.',
                'JaCoCo method line/branch counters prove execution, not exact test identity or test-first chronology.',
                'Unique source-range candidates for unscored functions are diagnostic, not automatically accepted CRAP mappings.'])
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(result, indent=2) + '\n', encoding='utf-8')
    print(json.dumps(result['counts'], indent=2))


if __name__ == '__main__':
    main()
