"""Measured ratchets, never substitute missing reports with successful empty data."""
import argparse
from collections import Counter
import hashlib
import json
from pathlib import Path
import xml.etree.ElementTree as ET

BASELINE = Path('config/quality/baseline.json')
OUT = Path('build/reports/quality')
POLICY = dict(line.split('=', 1) for line in Path('config/quality/policy.properties').read_text().splitlines() if line and not line.startswith('#'))
RETIRED = json.loads(Path('config/quality/retired-methods.json').read_text())


def ratio(counter):
    total = counter['covered'] + counter['missed']
    if not total:
        raise ValueError('Missing coverage denominator')
    return counter['covered'] / total


def crap(cc, coverage):
    if cc < 1 or not 0 <= coverage <= 1:
        raise ValueError('Invalid CRAP inputs')
    return cc ** 2 * (1 - coverage) ** 3 + cc


def measure():
    cov = json.loads(Path('app/build/reports/jacoco/debug/summary.json').read_text())
    detekt = json.loads(Path('build/reports/detekt/summary.json').read_text())
    functions = detekt['functions_by_cyclomatic']
    counts = Counter((r['file'], r['function']) for r in functions)
    complexity = {r['file'] + ':' + r['function']: r['cyclomatic'] for r in functions
                  if counts[r['file'], r['function']] == 1}
    methods = []
    for r in cov.get('source_method_matches', cov['reviewed_method_matches']):
        coverage = ratio(r['coverage']['LINE'])
        methods.append(dict(identity=r['owner'] + '.' + r['name'] + r['descriptor'],
                            file=r['source'], cc=r['cyclomatic'], line_coverage=coverage,
                            crap_line=crap(r['cyclomatic'], coverage),
                            risk='above-target' if crap(r['cyclomatic'], coverage) >= float(POLICY['crapTarget']) else 'within-target'))
    cpd = ET.parse('app/build/reports/quality/cpd.xml').getroot()
    if not cpd.findall('{*}file'):
        raise ValueError('CPD did not scan production files')
    if cpd.findall('{*}error'):
        raise ValueError('CPD parser errors')
    duplicates = []
    for d in cpd.findall('{*}duplication'):
        fragment = d.find('{*}codefragment').text
        duplicates.append(dict(hash=hashlib.sha256(fragment.encode()).hexdigest(),
                               tokens=int(d.attrib['tokens']), lines=int(d.attrib['lines']),
                               occurrences=[dict(file=Path(f.attrib['path']).name,
                                                 line=int(f.attrib['line'])) for f in d.findall('{*}file')]))
    return dict(coverage={name: {k: ratio(value[k]) for k in ('LINE', 'BRANCH')}
                          for name, value in cov['components'].items()
                          if name in ('Overall production', 'Pure core')},
                complexity=complexity, methods=methods, duplicates=duplicates,
                measured_functions=len(functions),
                unmapped_functions=len(functions)-len(methods),
                unmatched_specs=cov['unmatched_method_specs'])


def violations(current, baseline):
    errors = []
    for group, counters in baseline['coverage'].items():
        for kind, floor in counters.items():
            if current['coverage'][group][kind] + float(POLICY['coverageTolerance']) < floor:
                errors.append(f'Coverage regression: {group}/{kind}')
    for key, cc in current['complexity'].items():
        if cc >= int(POLICY['complexityTarget']) and cc > baseline['complexity'].get(key, int(POLICY['complexityTarget'])-1):
            errors.append(f'Complexity regression: {key} = {cc}')
    previous = {r['identity']: r for r in baseline['methods']}
    now = {r['identity']: r for r in current['methods']}
    for key, row in previous.items():
        if key not in now:
            replacement = RETIRED.get(key, {}).get('replacement')
            if replacement not in now:
                errors.append(f'Reviewed mapping lost: {key}')
        elif now[key]['crap_line'] >= float(POLICY['crapTarget']) and now[key]['crap_line'] > row['crap_line'] + 1e-9:
            errors.append(f'CRAP-L regression: {key}')
    for key, row in now.items():
        if key not in previous and row['crap_line'] >= float(POLICY['crapTarget']):
            # An explicitly reviewed signature evolution keeps its measured predecessor's
            # ceiling. It must not reset debt, allow ambiguous lineage, or hide an increase.
            predecessors = [old for identity, old in previous.items()
                            if identity not in now and RETIRED.get(identity, {}).get('replacement') == key]
            if len(predecessors) != 1:
                errors.append(f'New high CRAP-L: {key}')
            elif row['crap_line'] > predecessors[0]['crap_line'] + 1e-9:
                errors.append(f'CRAP-L replacement regression: {key}')
    allowed = Counter(r['hash'] for r in baseline['duplicates'])
    for key, count in Counter(r['hash'] for r in current['duplicates']).items():
        if count > allowed[key]:
            errors.append('New CPD group: ' + key)
    return errors


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--record', action='store_true', help='Explicit reviewed baseline update; never use in CI')
    args = parser.parse_args()
    current = measure()
    OUT.mkdir(parents=True, exist_ok=True)
    (OUT/'metrics.json').write_text(json.dumps(current, indent=2)+'\n')
    if args.record:
        BASELINE.parent.mkdir(parents=True, exist_ok=True)
        BASELINE.write_text(json.dumps(current, indent=2)+'\n')
    errors = violations(current, json.loads(BASELINE.read_text()))
    print(json.dumps(dict(mapped=len(current['methods']), unmapped=current['unmapped_functions'],
                          high_risk=sum(r['risk']=='above-target' for r in current['methods']),
                          cpd_groups=len(current['duplicates']), violations=errors)))
    if errors:
        raise SystemExit(1)


if __name__ == '__main__':
    main()
