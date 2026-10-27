#!/usr/bin/env python3
"""Compiles REAL project pure Java contract files and runs numeric/negative probe (no Oracle)."""
from pathlib import Path
import subprocess,tempfile,sys
ROOT=Path(__file__).resolve().parents[3]
MAIN=ROOT/'backend/src/main/java/com/behsazan/corebanking/fee3'
PROBE=ROOT/'tools/fee3-backend/r4c2/Fee3R4C2Probe.java'
FILES=[
 MAIN/'domain/Fee3Models.java',
 MAIN/'r3/Fee3PreviewEngine.java',
 MAIN/'r4/Fee3R4CalculationContract.java',
 MAIN/'r4c1/Fee3R4C1LoanProfitContract.java',
 MAIN/'r4c2/Fee3R4C2LoanModelContract.java', PROBE
]
def main():
    with tempfile.TemporaryDirectory(prefix='fee3-r4c2-java-') as name:
        command=['javac','--release','21','-encoding','UTF-8','-d',name,*map(str,FILES)]
        try:
            result=subprocess.run(command,capture_output=True,text=True,encoding='utf8',errors='replace',timeout=90)
            if result.returncode:
                print('FEE3_R4C2_PURE_COMPILE_FAIL\n'+result.stderr[-4000:]);return 2
            run=subprocess.run(['java','-cp',name,'Fee3R4C2Probe'],capture_output=True,
                text=True,encoding='utf8',errors='replace',timeout=90)
            print(run.stdout)
            if run.returncode:
                print('FEE3_R4C2_PURE_TEST_FAIL\n'+run.stderr[-4000:]);return 2
            if 'FEE3_R4C2_PURE_JAVA_PASS=' not in run.stdout:return 2
            return 0
        except (OSError,subprocess.TimeoutExpired) as ex:
            print('FEE3_R4C2_PURE_JAVA_UNAVAILABLE',type(ex).__name__,str(ex)[:200]);return 2
if __name__=='__main__':sys.exit(main())
