"""Isolated CLI probes: no app source, real metrics, baselines or user data changed."""
import hashlib
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
import xml.etree.ElementTree as ET
from source_quality import identity

TOOLS = Path(__file__).resolve().parent


class SourceQualityCliTest(unittest.TestCase):
    def probe(self, status='measured', cc=1, review=False, unchanged=False, missing=None, malformed=False, empty=False, defect=None):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)

            def write(path, value):
                target = root / path
                target.parent.mkdir(parents=True, exist_ok=True)
                target.write_text(value if isinstance(value, str) else json.dumps(value), encoding='utf-8')

            file = 'app/src/main/java/test/Example.kt'
            write('synthetic-test', 'Nonprivate fixture evidence for review-path validation.')
            source = 'package test\nfun old() = 1\nfun added() = 2\n'
            write(file, source)
            inventory = ET.Element('kotlin-inventory', schema='2')
            unit = ET.SubElement(inventory, 'source', file=file, sha256=hashlib.sha256(source.encode()).hexdigest())
            functions, matches = [], []
            for index, name in enumerate(('old', 'added')):
                row = dict(file=file, name=name, owner='test/ExampleKt', signature=name + '()',
                    line=str(index + 2), start=str(index + 2), end=str(index + 2), kind='function',
                    identity=name, semanticHash=hashlib.sha256(name.encode()).hexdigest(),
                    offset=str(index * 20), endOffset=str(index * 20 + 10), body='true', local='false')
                ET.SubElement(unit, 'function', {k: v for k, v in row.items() if k != 'file'})
                m = dict(row, status='measured' if index == 0 else status, cc=1 if index == 0 else cc,
                         line_coverage=1, crap_line=1 if index == 0 else cc,
                         method=dict(owner=row['owner'], name=name, descriptor='()I', coverage={'LINE': {'covered': 1, 'missed': 0}}))
                functions.append(m)
                if index == 1 and status == 'declaration-without-body':
                    row['body'] = m['body'] = 'false'
                    unit[-1].set('body', 'false')
                if m['status'] == 'measured':
                    matches.append(dict(source=file, owner=m['owner'], name=name, descriptor='()I', cyclomatic=m['cc'], coverage=m['method']['coverage']))
            write('build/reports/quality/source-inventory.xml', ET.tostring(inventory, encoding='unicode'))
            write('build/reports/quality/function-mapping.json', dict(total_functions=2, functions=functions))
            counter = {'covered': 8, 'missed': 2}
            components = {group: {'LINE': counter, 'BRANCH': counter} for group in ('Overall production', 'Pure core')}
            write('app/build/reports/jacoco/debug/summary.json', dict(components=components,
                source_method_matches=matches, reviewed_method_matches=[], unmatched_method_specs=[]))
            raw = ET.Element('report')
            owner = ET.SubElement(ET.SubElement(raw, 'package', name='test'), 'class', name='test/ExampleKt')
            for f in functions:
                method = ET.SubElement(owner, 'method', name=f['name'], desc='()I')
                ET.SubElement(method, 'counter', type='LINE', covered='1', missed='0')
            write('app/build/reports/jacoco/debug/coverage.xml', ET.tostring(raw, encoding='unicode'))
            class_root = 'app/build/tmp/kotlin-classes/debug'
            class_file = class_root + '/test/ExampleKt.class'
            write(class_file, 'Synthetic class bytes; actual compiler decoding is tested in engineering-tools.')
            fingerprint = hashlib.sha256((root / class_file).read_bytes()).hexdigest()
            metadata = ET.Element('jvmMethods', schema='1', classesScanned='1', classesRoot=class_root, compilation='non-incremental')
            for function in functions:
                ET.SubElement(metadata, 'function', owner='test/ExampleKt', name=function['name'],
                    descriptor='()I', sourceName=function['name'], classSha256=fingerprint)
            write('build/reports/quality/jvm-method-inventory.xml', ET.tostring(metadata, encoding='unicode'))
            write('build/reports/detekt/summary.json', dict(functions_by_cyclomatic=[] if empty else
                [dict(file=file, line=int(f['line']), function=f['name'], cyclomatic=f['cc']) for f in functions]))
            write('app/build/reports/quality/cpd.xml', '<pmd-cpd><file path="Example.kt"/></pmd-cpd>')
            coverage = {group: {'LINE': .8, 'BRANCH': .8} for group in components}
            write('config/quality/baseline.json', dict(coverage=coverage, complexity={}, methods=[], duplicates=[]))
            baseline = {identity(functions[0]): functions[0]['semanticHash']}
            if unchanged:
                baseline[identity(functions[1])] = functions[1]['semanticHash']
                write('config/quality/baseline.json', dict(coverage=coverage, complexity={file + ':added': cc},
                    methods=[dict(identity='test/ExampleKt.added()I', crap_line=cc)], duplicates=[]))
            write('config/quality/source-baseline.json', dict(schema=1, revision='synthetic-old', callables=baseline))
            reviews = {}
            if review:
                reviews[identity(functions[1])] = dict(file=file, semanticHash=functions[1]['semanticHash'],
                    reason='Synthetic mapping limitation with separate behavioral evidence', evidence=['synthetic-test'], metric_status=status)
            write('config/quality/source-reviews.json', dict(reviews=reviews))
            write('config/quality/policy.properties', (TOOLS.parent / 'config/quality/policy.properties').read_text())
            write('config/quality/retired-methods.json', {})
            if missing:
                (root / missing).unlink()
            if malformed:
                write('build/reports/detekt/summary.json', '{')
            if defect == 'expired-review':
                reviews[identity(functions[1])]['semanticHash'] = '0' * 64
                write('config/quality/source-reviews.json', dict(reviews=reviews))
            elif defect == 'omitted-source-root':
                write('app/src/main/kotlin/test/Other.kt', 'package test\nfun other() = 3')
            elif defect == 'stale-source':
                write(file, source + 'fun silentlyChanged() = 4\n')
            elif defect == 'empty-inventory':
                write('build/reports/quality/source-inventory.xml', '<kotlin-inventory schema="2"/>')
            elif defect == 'incomplete-mapping':
                write('build/reports/quality/function-mapping.json', dict(total_functions=2, functions=functions[:1]))
            elif defect == 'stale-mapping':
                functions[1]['semanticHash'] = '0' * 64
                write('build/reports/quality/function-mapping.json', dict(total_functions=2, functions=functions))
            elif defect == 'stale-metadata-class':
                write(class_file, 'Different synthetic class bytes')
            elif defect == 'metadata-name-mismatch':
                metadata[0].set('sourceName', 'notTheAuthoredName')
                write('build/reports/quality/jvm-method-inventory.xml', ET.tostring(metadata, encoding='unicode'))
            return subprocess.run([sys.executable, str(TOOLS / 'quality_gate.py')], cwd=root,
                                  capture_output=True, text=True).returncode

    def test_new_measured_below_target_passes(self):
        self.assertEqual(0, self.probe())

    def test_new_above_target_fails(self):
        self.assertNotEqual(0, self.probe(cc=12))

    def test_new_eligible_unscored_and_ambiguous_functions_fail(self):
        for status in ('missing-complexity', 'no-bytecode-match', 'ambiguous-bytecode'):
            with self.subTest(status=status):
                self.assertNotEqual(0, self.probe(status=status))

    def test_explicit_source_bound_technical_review_is_visible_and_allowed(self):
        self.assertEqual(0, self.probe(status='anonymous-owner', review=True))
        self.assertNotEqual(0, self.probe(status='missing-complexity', review=True))

    def test_missing_required_reports_fail(self):
        for path in ('app/build/reports/jacoco/debug/summary.json', 'build/reports/detekt/summary.json',
                     'app/build/reports/jacoco/debug/coverage.xml', 'build/reports/quality/function-mapping.json',
                     'build/reports/quality/source-inventory.xml', 'build/reports/quality/jvm-method-inventory.xml'):
            with self.subTest(path=path):
                self.assertNotEqual(0, self.probe(missing=path))

    def test_empty_or_malformed_complexity_fails(self):
        self.assertNotEqual(0, self.probe(empty=True))
        self.assertNotEqual(0, self.probe(malformed=True))

    def test_unchanged_historical_risk_does_not_require_a_fake_refactor(self):
        self.assertEqual(0, self.probe(cc=12, unchanged=True))

    def test_abstract_contract_does_not_require_fabricated_executable_coverage(self):
        self.assertEqual(0, self.probe(status='declaration-without-body'))

    def test_source_bound_review_expires(self):
        self.assertNotEqual(0, self.probe(status='anonymous-owner', review=True, defect='expired-review'))

    def test_empty_stale_incomplete_or_scope_omitting_reports_fail(self):
        for defect in ('omitted-source-root', 'stale-source', 'empty-inventory', 'incomplete-mapping', 'stale-mapping',
                       'stale-metadata-class', 'metadata-name-mismatch'):
            with self.subTest(defect=defect):
                self.assertNotEqual(0, self.probe(defect=defect))


if __name__ == '__main__':
    unittest.main()
