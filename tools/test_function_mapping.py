"""Reject plausible-looking but ambiguous source/bytecode correspondences."""
import unittest
import tempfile
from pathlib import Path
import xml.etree.ElementTree as ET
from function_mapping import correlate, metadata_aliases


class FunctionMappingTest(unittest.TestCase):
    def function(self, **extra):
        return dict(name='read', owner='test/Example', line='3', start='3', end='5', parameters='1',
                    receiver='false', inline='false', suspend='false', compose='false', local='false', body='true', **extra)

    def method(self, owner='test/Example', name='read', line='4', desc='(I)I'):
        return dict(owner=owner, name=name, line=int(line), descriptor=desc,
                    coverage={'LINE': {'covered': 1, 'missed': 1}})

    def test_matches_owner_parameter_count_and_body_location(self):
        result = correlate(self.function(), [self.method(), self.method(owner='test/Other')], 3)
        self.assertEqual(result['status'], 'measured')
        self.assertEqual(result['method']['descriptor'], '(I)I')

    def test_rejects_same_name_outside_declaration_body(self):
        self.assertEqual(correlate(self.function(), [self.method(line='7')], 3)['status'], 'no-bytecode-match')

    def test_rejects_ambiguous_same_line_overloads(self):
        self.assertEqual(correlate(self.function(), [self.method(), self.method(desc='(Ljava/lang/String;)I')], 3)['status'], 'ambiguous-bytecode')

    def test_rejects_argument_count_mismatch(self):
        self.assertEqual(correlate(self.function(), [self.method(desc='(II)I')], 3)['status'], 'no-bytecode-match')

    def test_extension_receiver_is_an_actual_bytecode_argument(self):
        f = self.function(); f['receiver'] = 'true'
        self.assertEqual(correlate(f, [self.method(desc='(Ljava/lang/String;I)I')], 3)['status'], 'measured')

    def test_does_not_score_transformed_or_nested_functions(self):
        for flag in ['compose', 'inline', 'suspend', 'local']:
            f = self.function(); f[flag] = 'true'
            self.assertNotEqual(correlate(f, [self.method()], 3)['status'], 'measured')

    def test_missing_metric_or_zero_denominator_is_unmeasured(self):
        self.assertEqual(correlate(self.function(), [self.method()], None)['status'], 'missing-complexity')
        m = self.method(); m['coverage']['LINE'] = {'covered': 0, 'missed': 0}
        self.assertEqual(correlate(self.function(), [m], 3)['status'], 'no-method-line-denominator')

    def test_zero_coverage_is_measured_but_zero_complexity_is_invalid(self):
        m = self.method(); m['coverage']['LINE'] = {'covered': 0, 'missed': 2}
        measured = correlate(self.function(), [m], 3)
        self.assertEqual('measured', measured['status'])
        self.assertEqual(12, measured['crap_line'])
        self.assertEqual('invalid-complexity', correlate(self.function(), [m], 0)['status'])

    def test_descriptor_handles_arrays_and_objects_as_one_argument(self):
        f = self.function(); f['parameters'] = '3'
        self.assertEqual(correlate(f, [self.method(desc='([[D[SLjava/util/List;)V')], 3)['status'], 'measured')

    def test_anonymous_members_are_not_assumed_to_contribute_to_enclosing_metric(self):
        f=self.function();f['owner']='test/Example$<anonymous>';f['local']='true'
        self.assertEqual(correlate(f, [self.method()], None)['status'], 'anonymous-owner')

    def test_mangled_names_require_exact_metadata_owner_name_and_descriptor(self):
        method = self.method(name='read-gIAlu-s', desc='(I)Ljava/lang/Object;')
        key = method['owner'], method['name'], method['descriptor']
        self.assertEqual('no-bytecode-match', correlate(self.function(), [method], 3)['status'])
        measured = correlate(self.function(), [method], 3, {key: 'read'})
        self.assertEqual('measured', measured['status'])
        self.assertEqual(3.0 ** 2 * .5 ** 3 + 3, measured['crap_line'])
        for bad in [('other/Owner', key[1], key[2]), (key[0], key[1], '(J)Ljava/lang/Object;')]:
            self.assertEqual('no-bytecode-match', correlate(self.function(), [method], 3, {bad: 'read'})['status'])
        self.assertEqual('no-bytecode-match', correlate(self.function(), [method], 3, {key: 'other'})['status'])

    def test_metadata_never_overrides_source_location_or_resolves_ambiguous_overloads_by_guessing(self):
        methods = [self.method(name='read-x', desc='(I)I'), self.method(name='read-y', desc='(J)I')]
        aliases = {(m['owner'], m['name'], m['descriptor']): 'read' for m in methods}
        self.assertEqual('ambiguous-bytecode', correlate(self.function(), methods, 3, aliases)['status'])
        methods[0]['line'] = 100
        self.assertEqual('measured', correlate(self.function(), methods, 3, aliases)['status'])
        self.assertEqual('(J)I', correlate(self.function(), methods, 3, aliases)['method']['descriptor'])

    def test_metadata_input_rejects_missing_empty_duplicate_and_stale_class_evidence(self):
        with tempfile.TemporaryDirectory() as directory:
            report = Path(directory) / 'methods.xml'
            with self.assertRaises(FileNotFoundError):
                metadata_aliases(report)
            report.write_text('<jvmMethods schema="1" classesScanned="1" compilation="non-incremental"/>')
            with self.assertRaises(ValueError):
                metadata_aliases(report)
            row = '<function owner="Example" name="read-x" sourceName="read" descriptor="(I)I" classSha256="' + 'a' * 64 + '"/>'
            report.write_text('<jvmMethods schema="1" classesScanned="1" compilation="non-incremental">' + row * 2 + '</jvmMethods>')
            with self.assertRaises(ValueError):
                metadata_aliases(report)
            report.write_text('<jvmMethods schema="1" classesScanned="1" compilation="non-incremental" classesRoot="' + directory.replace('\\', '/') + '">' + row + '</jvmMethods>')
            Path(directory, 'Example.class').write_bytes(b'not-the-reported-class')
            with self.assertRaisesRegex(ValueError, 'Stale Kotlin metadata'):
                metadata_aliases(report, Path(directory))


if __name__ == '__main__':
    unittest.main()
