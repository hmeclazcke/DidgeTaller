"""Require executed mutations; PIT infrastructure errors are never successful detections."""
import argparse
from collections import Counter
import json
from pathlib import Path
import xml.etree.ElementTree as ET


def summarize(path):
    root = ET.parse(path).getroot()
    mutations = root.findall('mutation')
    if root.tag != 'mutations' or not mutations:
        raise ValueError('Missing/nonempty PIT mutations report required')
    counts = Counter(m.get('status') for m in mutations)
    valid = {'KILLED', 'SURVIVED', 'NO_COVERAGE', 'TIMED_OUT', 'NON_VIABLE'}
    unexpected = {status: count for status, count in counts.items() if status not in valid}
    if unexpected:
        raise ValueError('PIT execution incomplete: ' + json.dumps(unexpected))
    for mutation in mutations:
        owner=mutation.findtext('mutatedClass','').split('$',1)[0]
        source=mutation.findtext('sourceFile','')
        if owner.endswith('Test') or source.endswith(('Test.kt','Test.java')):
            raise ValueError('Test bytecode is not production mutation scope: ' + owner)
    executed = sum(int(m.get('numberOfTestsRun', '0')) for m in mutations
                   if m.get('status') in {'KILLED', 'SURVIVED'})
    if executed <= 0:
        raise ValueError('No completed mutant test execution')
    denominator = len(mutations) - counts['NON_VIABLE']
    return dict(total=len(mutations), outcomes=dict(counts), detection_denominator=denominator,
                detected_fraction=(counts['KILLED'] + counts['TIMED_OUT']) / denominator)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('report', type=Path)
    args = parser.parse_args()
    print(json.dumps(summarize(args.report)))


if __name__ == '__main__':
    main()
