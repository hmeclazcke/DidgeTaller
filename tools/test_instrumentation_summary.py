import unittest
from run_instrumentation import summarize, successful

class InstrumentationSummaryTest(unittest.TestCase):
    def test_failure_and_skip_are_not_passes(self):
        text="INSTRUMENTATION_STATUS: numtests=3\nINSTRUMENTATION_STATUS_CODE: 0\nINSTRUMENTATION_STATUS: class=Example\nINSTRUMENTATION_STATUS: test=broken\nINSTRUMENTATION_STATUS_CODE: -2\nINSTRUMENTATION_STATUS_CODE: -4"
        r=summarize(text)
        self.assertEqual((1,1,1,3),(r['passed'],r['failed'],r['skipped'],r['discovered']))
        self.assertEqual('broken',r['failures'][0]['test'])
    def test_start_events_are_not_executions(self):
        r=summarize("INSTRUMENTATION_STATUS: numtests=2\nINSTRUMENTATION_STATUS_CODE: 1")
        self.assertEqual(0,r['passed']+r['failed']+r['skipped'])

    def test_success_requires_complete_results_and_an_end_summary(self):
        text="INSTRUMENTATION_STATUS: numtests=2\nINSTRUMENTATION_STATUS_CODE: 0\nINSTRUMENTATION_STATUS_CODE: -4\nOK (2 tests)"
        self.assertTrue(successful(text))
        self.assertFalse(successful(text,1))
        self.assertFalse(successful(text.replace('OK (2 tests)','')))
        self.assertFalse(successful(text.replace('numtests=2','numtests=3')))
        self.assertFalse(successful(text.replace('CODE: -4','CODE: -2')))

    def test_empty_or_truncated_runs_cannot_claim_success(self):
        self.assertFalse(successful('OK (0 tests)'))
        self.assertFalse(successful('INSTRUMENTATION_STATUS: numtests=2\nOK (2 tests)'))
        self.assertFalse(successful('INSTRUMENTATION_STATUS: numtests=1\nINSTRUMENTATION_STATUS_CODE: -4\nOK (1 test)'))

if __name__=='__main__':unittest.main()
