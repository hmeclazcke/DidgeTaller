import unittest

from phase_three_scores import matched_correct, zero_noise_threshold, subharmonic


class SourceEvidenceTest(unittest.TestCase):
    def test_fractional_ties_keep_matched_coverage_unbiased(self):
        rows = [dict(estimated_hz=x, strength=.9, target_hz=68) for x in (68, 68, 120)]
        self.assertAlmostEqual(4/3, matched_correct(rows, 2))

    def test_noise_boundary_excludes_equal_rank(self):
        rows = [dict(estimated_hz=68, strength=.9, target_hz=0)]
        self.assertGreater(zero_noise_threshold(rows), .9)

    def test_source_pitch_is_not_known_just_because_a_peak_is_present(self):
        self.assertTrue(subharmonic(dict(lowest_line=340, estimated_hz=68)))
        self.assertFalse(subharmonic(dict(lowest_line=340, estimated_hz=339)))
        self.assertFalse(subharmonic(dict(lowest_line=340, estimated_hz=0)))
