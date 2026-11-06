"""Summarize separate Android coverage, retaining untested production packages."""
import json
from pathlib import Path
import xml.etree.ElementTree as ET


def counters(element):
    result={}
    for row in element.findall('counter'):
        if row.attrib['type'] in ('LINE','BRANCH'):
            covered=int(row.attrib['covered']);missed=int(row.attrib['missed'])
            result[row.attrib['type']]=dict(covered=covered,missed=missed,
                fraction=covered/(covered+missed) if covered+missed else None)
    return result


def main():
    root=Path('app/build/reports/jacoco/emulator')
    report=ET.parse(root/'coverage.xml').getroot()
    overall=counters(report)
    if not overall.get('LINE',{}).get('covered',0):
        raise SystemExit('No production lines covered by emulator execution')
    result=dict(scope='Android instrumentation only; not merged with JVM',
        test_run=json.loads(Path('build/reports/quality/instrumentation.json').read_text()),overall=overall,
        packages={p.attrib['name']:counters(p) for p in report.findall('package')})
    (root/'summary.json').write_text(json.dumps(result,indent=2)+'\n',encoding='utf-8')
    print(json.dumps(result,indent=2))


if __name__=='__main__':main()
