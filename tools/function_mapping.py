"""Conservative Kotlin PSI / Detekt / JaCoCo function-level correspondence.

An inventory row is never silently dropped. Unsupported transformations and ambiguous
locations are classified, not assigned class coverage or guessed by function name.
"""
import argparse
from collections import Counter
import hashlib
import json
from pathlib import Path
import xml.etree.ElementTree as ET


def argument_count(descriptor):
    index, count = 1, 0
    while descriptor[index] != ')':
        while descriptor[index] == '[':
            index += 1
        index = descriptor.index(';', index) + 1 if descriptor[index] == 'L' else index + 1
        count += 1
    return count


def correlate(function, methods, complexity):
    if '<anonymous>' in function['owner'] or not function['name']:
        return dict(status='anonymous-owner')
    for flag, reason in [('local', 'nested-metric-in-parent'), ('compose', 'compose-transformation'),
                         ('inline', 'inline-source-mapping'), ('suspend', 'suspend-state-machine')]:
        if function[flag] == 'true':
            return dict(status=reason)
    if function['body'] != 'true':
        return dict(status='declaration-without-body')
    if complexity is None:
        return dict(status='missing-complexity')
    if complexity < 1:
        return dict(status='invalid-complexity')
    parameters = int(function['parameters']) + (function['receiver'] == 'true')
    candidates = [m for m in methods if m['owner'] == function['owner'] and m['name'] == function['name']
                  and int(function['start']) <= m['line'] <= int(function['end'])
                  and argument_count(m['descriptor']) == parameters]
    if len(candidates) != 1:
        return dict(status='ambiguous-bytecode' if candidates else 'no-bytecode-match')
    counter = candidates[0]['coverage'].get('LINE', {})
    total = counter.get('covered', 0) + counter.get('missed', 0)
    if not total:
        return dict(status='no-method-line-denominator')
    coverage = counter['covered'] / total
    return dict(status='measured', method=candidates[0], cc=complexity, line_coverage=coverage,
                crap_line=complexity ** 2 * (1 - coverage) ** 3 + complexity)


def inventory_report(inventory_path, complexity_path, coverage_path):
    metrics = json.loads(complexity_path.read_text(encoding='utf-8'))['functions_by_cyclomatic']
    by_location = {}
    for metric in metrics:
        key = (metric['file'], metric['line'], metric['function'])
        if key in by_location:
            raise ValueError(f'Ambiguous Detekt location: {key}')
        by_location[key] = metric['cyclomatic']
    methods_by_file = {}
    for package in ET.parse(coverage_path).getroot().findall('package'):
        for owner in package.findall('class'):
            source = owner.get('sourcefilename')
            for method in owner.findall('method'):
                if method.get('line') is None:
                    continue
                row = dict(owner=owner.get('name'), name=method.get('name'), descriptor=method.get('desc'),
                           line=int(method.get('line')), coverage={c.get('type'): {k: int(c.get(k)) for k in ['missed', 'covered']}
                                                                 for c in method.findall('counter')})
                methods_by_file.setdefault(package.get('name') + '/' + source, []).append(row)
    functions, sources = [], []
    for source in ET.parse(inventory_path).getroot():
        path = source.get('file')
        text = Path(path).read_text(encoding='utf-8')
        if hashlib.sha256(text.encode('utf-8')).hexdigest() != source.get('sha256'):
            raise ValueError(f'Stale PSI inventory: {path}')
        sources.append(dict(source.attrib, functions=len(source.findall('function')),
                            lambdas=len(source.findall('lambda')), accessors=len(source.findall('accessor'))))
        candidates = methods_by_file.get(path.split('/main/', 1)[-1].split('/', 1)[-1], [])
        for function in source.findall('function'):
            row = dict(function.attrib, file=path)
            key = (path, int(function.get('line')), function.get('name'))
            row.update(correlate(row, candidates, by_location.get(key)))
            functions.append(row)
    if not sources or not functions:
        raise ValueError('Empty production inventory')
    # Even nested/abstract/framework declarations remain in this denominator.
    return dict(parser='Kotlin PSI 2.2.21', coverage_metric='method-specific JaCoCo LINE fraction',
                sources=sources, functions=functions,
                counts=dict(Counter(f['status'] for f in functions)), total_functions=len(functions))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--inventory', type=Path, default=Path('build/reports/quality/source-inventory.xml'))
    parser.add_argument('--complexity', type=Path, default=Path('build/reports/detekt/summary.json'))
    parser.add_argument('--coverage', type=Path, default=Path('app/build/reports/jacoco/debug/coverage.xml'))
    parser.add_argument('--output', type=Path, default=Path('build/reports/quality/function-mapping.json'))
    args = parser.parse_args()
    report = inventory_report(args.inventory, args.complexity, args.coverage)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
    print(json.dumps(dict(total=report['total_functions'], counts=report['counts'])))


if __name__ == '__main__':
    main()
