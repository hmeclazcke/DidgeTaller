"""Fail closed on changed authored callables, with source-bound technical reviews.

This adds source-change accountability to metric ratchets. A review acknowledges a
specific limitation/risk; it does not turn unmeasured code into measured code.
"""
import hashlib
import json
from pathlib import Path
import xml.etree.ElementTree as ET
from function_mapping import metadata_aliases


def identity(row):
    return hashlib.sha256((row['file'] + '::' + row['identity']).encode()).hexdigest()


def declarations(inventory):
    if inventory.get('schema') != '2':
        raise ValueError('Source inventory schema 2 required')
    rows, files = [], set()
    for source in inventory:
        file = source.get('file')
        if file in files:
            raise ValueError('Duplicate inventory file: ' + file)
        files.add(file)
        text = Path(file).read_text(encoding='utf-8')
        if hashlib.sha256(text.encode()).hexdigest() != source.get('sha256'):
            raise ValueError('Stale source inventory: ' + file)
        for entry in source:
            row = dict(entry.attrib, file=file, kind=entry.tag)
            if not row.get('identity') or len(row.get('semanticHash', '')) != 64:
                raise ValueError('Missing authored callable fingerprint: ' + file)
            rows.append(row)
    expected = {p.as_posix() for root in ('java', 'kotlin')
                for p in Path('app/src/main', root).rglob('*.kt')}
    if not expected or files != expected or not rows:
        raise ValueError('Incomplete production source inventory: ' + str(sorted(expected ^ files)))
    if len({identity(r) for r in rows}) != len(rows):
        raise ValueError('Duplicate authored callable identity')
    return rows


def function_key(row):
    return row['file'], row['owner'], row['signature'], int(row['line'])


def check_changes(rows, mapping, baseline, reviews, target):
    if baseline.get('schema') != 1 or not baseline.get('revision') or not baseline.get('callables'):
        raise ValueError('Missing source baseline')
    functions = {function_key(r): r for r in mapping['functions']}
    named = [r for r in rows if r['kind'] == 'function']
    if len(functions) != len(mapping['functions']) or set(functions) != {function_key(r) for r in named}:
        raise ValueError('Function mapping does not account for every authored named declaration')
    for row in named:
        mapped = functions[function_key(row)]
        if any(mapped.get(field) != row[field] for field in ('identity', 'semanticHash', 'body', 'local')):
            raise ValueError('Stale source-to-metric mapping: ' + row['file'])
    changes, errors = [], []
    for row in rows:
        key = identity(row)
        if baseline['callables'].get(key) == row['semanticHash']:
            continue
        metric = functions.get(function_key(row)) if row['kind'] == 'function' else None
        reason = None
        if metric:
            if metric['status'] == 'declaration-without-body' and row['body'] == 'false':
                reason = 'non-executable abstract contract'
            elif metric['status'] == 'measured':
                if metric['cc'] < 1 or not 0 <= metric['line_coverage'] <= 1:
                    raise ValueError('Invalid measured function inputs')
                score = metric['cc'] ** 2 * (1 - metric['line_coverage']) ** 3 + metric['cc']
                if abs(metric['crap_line'] - score) > 1e-8:
                    raise ValueError('Inconsistent CRAP-L calculation')
                if score < target:
                    reason = 'reliably measured below target'
            elif metric['status'] == 'nested-metric-in-parent' and row['local'] == 'true':
                reason = 'nested declaration accounted in enclosing source body'
        elif any(p['file'] == row['file'] and int(p['offset']) <= int(row['offset'])
                 and int(row['endOffset']) <= int(p['endOffset']) for p in named):
            reason = 'auxiliary construct accounted in enclosing named declaration'
        if reason is None:
            review = reviews.get(key, {})
            valid = (review.get('semanticHash') == row['semanticHash']
                     and review.get('file') == row['file']
                     and review.get('reason') and review.get('evidence')
                     and all(Path(path).is_file() for path in review['evidence']))
            if valid and metric and metric['status'] != 'measured':
                valid = (metric['status'] in {'anonymous-owner', 'compose-transformation',
                                             'inline-source-mapping', 'suspend-state-machine'}
                         and review.get('metric_status') == metric['status'])
            if valid and metric and metric['status'] == 'measured':
                valid = (review.get('max_crap_line', -1) >= metric['crap_line']
                         and review.get('max_cc', -1) >= metric['cc'])
            if valid:
                reason = 'explicit source-bound risk/technical review'
            else:
                errors.append(f"Unreviewed changed callable: {row['file']}:{row['line']} {row.get('name', row['kind'])} ({metric['status'] if metric else row['kind']})")
        changes.append(dict(id=key, file=row['file'], line=int(row['line']), kind=row['kind'],
                            name=row.get('name'), status=reason or 'rejected',
                            metric_status=metric['status'] if metric else 'auxiliary-not-independently-scored'))
    return dict(changes=changes, violations=errors, total_callables=len(rows), named_functions=len(named))


def measure_source_changes(coverage, detekt, target):
    aliases = metadata_aliases(Path('build/reports/quality/jvm-method-inventory.xml'), Path('app/build/tmp/kotlin-classes/debug'))
    raw = ET.parse('app/build/reports/jacoco/debug/coverage.xml').getroot()
    raw_methods = {(owner.get('name'), method.get('name'), method.get('desc')):
                   {c.get('type'): {k: int(c.get(k)) for k in ('covered', 'missed')}
                    for c in method.findall('counter')}
                   for package in raw.findall('package') for owner in package.findall('class')
                   for method in owner.findall('method')}
    inventory = ET.parse('build/reports/quality/source-inventory.xml').getroot()
    rows = declarations(inventory)
    mapping = json.loads(Path('build/reports/quality/function-mapping.json').read_text())
    if not detekt.get('functions_by_cyclomatic') or not mapping.get('functions'):
        raise ValueError('Empty complexity/function report')
    if mapping['total_functions'] != sum(r['kind'] == 'function' for r in rows):
        raise ValueError('Function inventory count mismatch')
    measured = [r for r in mapping['functions'] if r['status'] == 'measured']
    expected = {(r['method']['owner'], r['method']['name'], r['method']['descriptor']):
                (r['cc'], r['method']['coverage']) for r in measured}
    actual = {(r['owner'], r['name'], r['descriptor']): (r['cyclomatic'], r['coverage'])
              for r in coverage['source_method_matches']}
    if not measured or actual != expected or len(actual) != len(coverage['source_method_matches']):
        raise ValueError('Coverage summary and function mappings disagree or are empty')
    complexity = {(r['file'], r['line'], r['function']): r['cyclomatic'] for r in detekt['functions_by_cyclomatic']}
    for row in measured:
        if complexity.get((row['file'], int(row['line']), row['name'])) != row['cc'] or row['cc'] < 1:
            raise ValueError('Missing/invalid matched complexity')
        method = row['method']
        key = method['owner'], method['name'], method['descriptor']
        if aliases.get(key, method['name']) != row['name']:
            raise ValueError('Kotlin metadata disagrees with source callable')
        counters = raw_methods.get((method['owner'], method['name'], method['descriptor']), {})
        if any(counters.get(kind) != value for kind, value in method['coverage'].items()):
            raise ValueError('Raw JaCoCo method counters disagree with mapping')
    baseline = json.loads(Path('config/quality/source-baseline.json').read_text())
    reviews = json.loads(Path('config/quality/source-reviews.json').read_text())['reviews']
    return check_changes(rows, mapping, baseline, reviews, target)
