import math
import unittest

from phase_two_scores import describe, curve


class PhaseTwoScoresTest(unittest.TestCase):
    @staticmethod
    def rows(pairs):
        return [dict(target_hz=t, estimated_hz=f, strength=s) for t, f, s in pairs]

    def test_ratio_errors_distinguish_both_directions_and_unlabelled_sources(self):
        rows = self.rows([(68, 340, .99), (204, 68, .95), (68, 136, .95),
                          (68, 0, .1), (math.nan, 68, .99), (0, 68, .8)])
        d = describe(rows)
        self.assertEqual((4, 3, 1, 1, 1, 1, 1),
                         tuple(d[k] for k in ("voiced", "detected", "octave", "ratio3", "ratio5",
                                               "unlabelled_emitted", "false_positive")))

    def test_selective_accuracy_keeps_original_denominator_and_all_ties(self):
        rows = self.rows([(68, 68, .9), (68, 136, .9), (68, 70, .1), (68, 0, 1)])
        point = curve(rows, .9)
        self.assertEqual((.5, .5), (point["coverage"], point["accuracy"]))
