"""Provenance comparisons must not convert ambiguous text similarity into a move."""
import unittest
from forensic_inventory import change_inventory


class ForensicInventoryTest(unittest.TestCase):
    def test_unambiguous_constructor_evolution_is_not_new_plus_removed(self):
        old = dict(kind='constructor', file='Example.kt', scope='test/Example', semanticHash='before')
        new = dict(old, semanticHash='after')
        result = change_inventory({'old-signature': old}, {'new-signature': new})
        self.assertEqual(['modified-signature'], [r['change'] for r in result])
        self.assertEqual('old-signature', result[0]['previous_identity'])

    def test_unchanged_declarations_are_not_newly_written(self):
        row = dict(kind='function', semanticHash='same')
        self.assertEqual([], change_inventory({'id': row}, {'id': row}))

    def test_body_change_is_modified_not_added(self):
        result = change_inventory({'id': dict(kind='function', semanticHash='old')},
                                  {'id': dict(kind='function', semanticHash='new')})
        self.assertEqual(['modified'], [r['change'] for r in result])

    def test_unique_relocation_preserves_provenance(self):
        row = dict(kind='lambda', semanticHash='same')
        result = change_inventory({'old': row}, {'new': row})
        self.assertEqual('moved', result[0]['change'])
        self.assertEqual('old', result[0]['previous_identity'])

    def test_ambiguous_identical_fragments_are_not_assigned_arbitrary_origins(self):
        row = dict(kind='lambda', semanticHash='same')
        result = change_inventory({'old': row}, {'new': row, 'other': row})
        self.assertEqual(['added', 'added', 'removed'], [r['change'] for r in result])


if __name__ == '__main__':
    unittest.main()
