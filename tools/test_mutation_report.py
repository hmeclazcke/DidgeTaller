"""Tool-error outcomes must fail the same report validator used by Gradle/CI."""
from pathlib import Path
import tempfile
import unittest
import xml.etree.ElementTree as ET
import subprocess
import sys
from mutation_report import summarize


class MutationReportTest(unittest.TestCase):
    def report(self, statuses, runs=1):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory, 'mutations.xml')
            path.write_text('<mutations>' + ''.join(f'<mutation status="{status}" numberOfTestsRun="{runs}"/>' for status in statuses) + '</mutations>')
            return summarize(path)

    def test_real_outcomes_have_an_explicit_denominator_and_timeouts_are_distinct(self):
        result = self.report(['KILLED', 'SURVIVED', 'NO_COVERAGE', 'TIMED_OUT', 'NON_VIABLE'])
        self.assertEqual(5, result['total']); self.assertEqual(4, result['detection_denominator'])
        self.assertEqual(.5, result['detected_fraction']); self.assertEqual(1, result['outcomes']['TIMED_OUT'])

    def test_infrastructure_and_unfinished_outcomes_fail(self):
        for status in ['RUN_ERROR', 'MEMORY_ERROR', 'STARTED', 'NOT_STARTED', 'UNKNOWN']:
            with self.subTest(status=status), self.assertRaises(ValueError):
                self.report(['KILLED', status])
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory, 'mutations.xml')
            path.write_text('<mutations><mutation status="RUN_ERROR" numberOfTestsRun="0"/></mutations>')
            process = subprocess.run([sys.executable, str(Path(__file__).with_name('mutation_report.py')), str(path)],
                                     capture_output=True, text=True)
            self.assertNotEqual(0, process.returncode)
            self.assertIn('PIT execution incomplete', process.stderr)

    def test_test_bytecode_is_not_a_production_mutation_result(self):
        for owner,source in [('ar.demo.OwnerTest','OwnerTest.kt'),('ar.demo.OwnerTest$Pending','OwnerTest.kt')]:
            with self.subTest(owner=owner),tempfile.TemporaryDirectory() as directory:
                path=Path(directory,'mutations.xml')
                path.write_text(f'<mutations><mutation status="KILLED" numberOfTestsRun="1"><mutatedClass>{owner}</mutatedClass><sourceFile>{source}</sourceFile></mutation></mutations>')
                with self.assertRaisesRegex(ValueError,'Test bytecode'):
                    summarize(path)

    def test_empty_or_unexecuted_reports_fail(self):
        for statuses in [[], ['NO_COVERAGE'], ['NON_VIABLE']]:
            with self.subTest(statuses=statuses), self.assertRaises(ValueError):
                self.report(statuses)
        with self.assertRaises(ValueError): self.report(['KILLED'], runs=0)

    def test_missing_and_malformed_reports_fail(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory, 'mutations.xml')
            with self.assertRaises(FileNotFoundError): summarize(path)
            path.write_text('<mutations>')
            with self.assertRaises(ET.ParseError): summarize(path)


if __name__ == '__main__':
    unittest.main()
