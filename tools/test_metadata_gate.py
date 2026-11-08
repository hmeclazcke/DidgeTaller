"""A required metadata report cannot disappear behind an existing coverage summary."""
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest


class MetadataGateTest(unittest.TestCase):
    def test_missing_metadata_fails_the_actual_gate_process(self):
        repository = Path(__file__).resolve().parent.parent
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            config = root / 'config/quality'
            config.mkdir(parents=True)
            (config / 'policy.properties').write_bytes((repository / 'config/quality/policy.properties').read_bytes())
            (config / 'retired-methods.json').write_text('{}')
            for relative, value in [('app/build/reports/jacoco/debug/summary.json', {}),
                                    ('build/reports/detekt/summary.json', {'functions_by_cyclomatic': []})]:
                path = root / relative
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_text(json.dumps(value))
            result = subprocess.run([sys.executable, str(repository / 'tools/quality_gate.py')], cwd=root,
                                    capture_output=True, text=True)
            self.assertNotEqual(0, result.returncode)
            self.assertIn('jvm-method-inventory.xml', result.stderr)
            self.assertFalse((root / 'build/reports/quality/metrics.json').exists())


if __name__ == '__main__':
    unittest.main()
