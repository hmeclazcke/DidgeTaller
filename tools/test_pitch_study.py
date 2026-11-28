import math
import unittest

from pitch_study import metrics


class PitchStudyScoringTest(unittest.TestCase):
    def test_miss_octave_and_false_positive_have_separate_denominators(self):
        rows = [{"target_hz": ref, "estimated_hz": hz} for ref, hz in
                [(60, 60), (60, 120), (60, 0), (0, 60), (0, 0), (math.nan, 60)]]
        result = metrics(rows)
        self.assertEqual((3, 2, 1, 1, 1, 1, 1, 2, 1), tuple(result[k] for k in
                         ("voiced", "detected", "miss", "within10", "within25", "within50",
                          "octave", "negative", "false_positive")))
        self.assertEqual(1200, result["p95_cents"])
        self.assertEqual(1, result["unlabelled"])

    def test_no_estimates_yield_no_conditional_error(self):
        self.assertIsNone(metrics([{"target_hz": 75, "estimated_hz": 0}])["p95_hz"])
