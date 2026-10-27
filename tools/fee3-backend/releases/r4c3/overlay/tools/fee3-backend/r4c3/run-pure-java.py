#!/usr/bin/env python3
"""Real Java pure contract compile and numeric/negative probe. No Oracle/write access."""
from pathlib import Path
import subprocess,sys,tempfile
ROOT=Path(__file__).resolve().parents[3]
SRC=ROOT/'backend/src/main/java/com/behsazan/corebanking/fee3'
FILES=[SRC/'domain/Fee3Models.java',SRC/'r3/Fee3PreviewEngine.java',
       SRC/'r4/Fee3R4CalculationContract.java',
       SRC/'r4c1/Fee3R4C1LoanProfitContract.java',
       SRC/'r4c3/Fee3R4C3QardContract.java',
       ROOT/'tools/fee3-backend/r4c3/Fee3R4C3Probe.java']
def main():
    with tempfile.TemporaryDirectory(prefix='fee3-r4c3-pure-') as root:
        try:
            compile=subprocess.run(['javac','--release','21','-encoding','UTF-8','-d',root,
                *map(str,FILES)],capture_output=True,text=True,timeout=90)
            if compile.returncode:
                print('FEE3_R4C3_COMPILE_FAIL\n'+compile.stderr[-4500:]);return 2
            run=subprocess.run(['java','-cp',root,'Fee3R4C3Probe'],capture_output=True,
                               text=True,timeout=90)
            print(run.stdout)
            if run.returncode or ' FAIL=0' not in run.stdout:
                print('FEE3_R4C3_PURE_JAVA_FAIL\n'+run.stderr[-4500:]);return 2
            return 0
        except (OSError,subprocess.TimeoutExpired) as ex:
            print('FEE3_R4C3_JAVA_UNAVAILABLE',ex);return 2
if __name__=='__main__':sys.exit(main())
