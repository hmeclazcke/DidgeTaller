"""Reject plausible-looking but ambiguous source/bytecode correspondences."""
import unittest
import xml.etree.ElementTree as ET
from function_mapping import correlate


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

    def test_descriptor_handles_arrays_and_objects_as_one_argument(self):
        f = self.function(); f['parameters'] = '3'
        self.assertEqual(correlate(f, [self.method(desc='([[D[SLjava/util/List;)V')], 3)['status'], 'measured')

    def test_anonymous_members_are_not_assumed_to_contribute_to_enclosing_metric(self):
        f=self.function();f['owner']='test/Example$<anonymous>';f['local']='true'
        self.assertEqual(correlate(f, [self.method()], None)['status'], 'anonymous-owner')


if __name__ == '__main__':
    unittest.main()
