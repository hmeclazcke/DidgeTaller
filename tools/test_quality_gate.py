"""Failure-semantics and known-formula checks for the quality ratchets."""
import copy
import unittest
from quality_gate import RETIRED, crap, violations


class QualityGateTest(unittest.TestCase):
    def setUp(self):
        self.base = dict(coverage={'core': {'LINE': .8}}, complexity={'f': 11},
                         methods=[dict(identity='f()', crap_line=11)], duplicates=[])

    def test_known_crap_examples(self):
        self.assertEqual(30, crap(5, 0))
        self.assertEqual(5, crap(5, 1))
        self.assertEqual(8.125, crap(5, .5))

    def test_baseline_passes_and_coverage_loss_fails(self):
        self.assertEqual([], violations(self.base, self.base))
        changed = copy.deepcopy(self.base)
        changed['coverage']['core']['LINE'] = .7
        self.assertTrue(violations(changed, self.base))

    def test_complexity_and_mapping_regressions_fail(self):
        changed = copy.deepcopy(self.base)
        changed['complexity']['new'] = 10
        self.assertTrue(violations(changed, self.base))
        changed = copy.deepcopy(self.base)
        changed['methods'] = []
        self.assertTrue(violations(changed, self.base))

    def test_new_duplication_and_crap_regressions_fail(self):
        changed = copy.deepcopy(self.base)
        changed['duplicates'] = [dict(hash='new')]
        self.assertTrue(violations(changed, self.base))
        changed = copy.deepcopy(self.base)
        changed['methods'][0]['crap_line'] = 12
        self.assertTrue(violations(changed, self.base))

    def test_retirement_requires_the_explicit_replacement_to_be_measured(self):
        identity='ar/didgetaller/audio/SessionPlayer.requestSeek(J)V'
        replacement=RETIRED[identity]['replacement']
        baseline=copy.deepcopy(self.base);baseline['methods']=[dict(identity=identity,crap_line=5)]
        current=copy.deepcopy(baseline);current['methods']=[]
        self.assertTrue(violations(current,baseline))
        current['methods']=[dict(identity=replacement,crap_line=3)]
        self.assertEqual([],violations(current,baseline))


if __name__ == '__main__':
    unittest.main()
