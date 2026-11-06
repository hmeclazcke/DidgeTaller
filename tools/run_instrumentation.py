"""Execute the entire suite on one verified emulator; ADB exit zero alone is insufficient."""
import argparse
import os
import json
from pathlib import Path
import re
import subprocess


def summarize(text):
    counts={'passed':0,'failed':0,'skipped':0}
    failures=[]
    current={}
    for line in text.splitlines():
        field=re.match(r'INSTRUMENTATION_STATUS: (class|test)=(.*)',line)
        if field:current[field[1]]=field[2]
        code=re.match(r'INSTRUMENTATION_STATUS_CODE: (-?\d+)',line)
        if code and int(code[1])<=0:
            value=int(code[1]);key='passed' if value==0 else 'skipped' if value in (-3,-4) else 'failed'
            counts[key]+=1
            if key=='failed':failures.append(dict(current))
            current={}
    return dict(counts,discovered=max([int(v) for v in re.findall(r'numtests=(\d+)',text)] or [0]),failures=failures)


def successful(text, returncode=0):
    counts=summarize(text)
    end=re.search(r'OK \((\d+) tests?\)',text)
    return (returncode==0 and end is not None and 'FAILURES!!!' not in text
            and counts['discovered']>0 and counts['passed']>0 and counts['failed']==0
            and counts['passed']+counts['skipped']==counts['discovered']==int(end[1]))


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--serial')
    p.add_argument('--verify-log', type=Path)
    p.add_argument('--adb', default='adb')
    p.add_argument('--coverage',action='store_true',help='Requires APKs built with -PandroidCoverage=true')
    args = p.parse_args()
    if args.verify_log:
        text=args.verify_log.read_bytes().decode('utf-16' if args.verify_log.read_bytes().startswith(b'\xff\xfe') else 'utf-8')
        print(json.dumps(summarize(text),indent=2))
        if not successful(text):raise SystemExit(1)
        return
    if not args.serial or not args.serial.startswith('emulator-'):
        p.error('Only an explicit emulator serial is allowed')
    command = [args.adb, '-s', args.serial]
    hardware = subprocess.check_output(command+['shell', 'getprop', 'ro.hardware'], text=True).strip()
    if hardware not in ('ranchu', 'goldfish'):
        raise SystemExit('Not a supported emulator')
    for apk in ('app/build/outputs/apk/debug/app-debug.apk',
                'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk'):
        subprocess.run(command+['install', '-r', apk], check=True)
    coverage_args=[]
    if args.coverage:
        Path('build/reports/quality/instrumentation.ec').unlink(missing_ok=True)
        subprocess.run(command+['shell','run-as','ar.didgetaller','rm','-f','files/quality-coverage.ec'],check=True)
        coverage_args=['-e','coverage','true','-e','coverageFile','/data/user/0/ar.didgetaller/files/quality-coverage.ec']
    output = subprocess.run(command+['shell', 'am', 'instrument', '-w', '-r']+coverage_args+[
                            'ar.didgetaller.test/androidx.test.runner.AndroidJUnitRunner'],
                            stdout=subprocess.PIPE, stderr=subprocess.STDOUT, timeout=1800)
    path = Path('build/reports/quality/instrumentation.txt')
    path.parent.mkdir(parents=True, exist_ok=True)
    text = output.stdout.decode('utf-8', errors='replace')
    path.write_text(text, encoding='utf-8')
    path.with_suffix('.json').write_text(json.dumps(dict(summarize(text),scope='complete suite'),indent=2))
    if args.coverage:
        coverage=subprocess.check_output(command+['exec-out','run-as','ar.didgetaller','cat','files/quality-coverage.ec'])
        if not coverage:raise SystemExit('Missing emulator coverage data')
        path.with_suffix('.ec').write_bytes(coverage)
    print(text[-2000:])
    if not successful(text,output.returncode):
        raise SystemExit('Instrumentation failed; full report: '+str(path))


if __name__ == '__main__':
    main()
