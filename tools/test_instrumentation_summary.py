import unittest
from run_instrumentation import summarize

class InstrumentationSummaryTest(unittest.TestCase):
    def test_failure_and_skip_are_not_passes(self):
        text="INSTRUMENTATION_STATUS: numtests=3\nINSTRUMENTATION_STATUS_CODE: 0\nINSTRUMENTATION_STATUS: class=Example\nINSTRUMENTATION_STATUS: test=broken\nINSTRUMENTATION_STATUS_CODE: -2\nINSTRUMENTATION_STATUS_CODE: -4"
        r=summarize(text)
        self.assertEqual((1,1,1,3),(r['passed'],r['failed'],r['skipped'],r['discovered']))
        self.assertEqual('broken',r['failures'][0]['test'])
    def test_start_events_are_not_executions(self):
        r=summarize("INSTRUMENTATION_STATUS: numtests=2\nINSTRUMENTATION_STATUS_CODE: 1")
        self.assertEqual(0,r['passed']+r['failed']+r['skipped'])

if __name__=='__main__':unittest.main()
