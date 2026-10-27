#!/usr/bin/env python3
"""Compiles the real R4-C1 calculation class and runs independent arithmetic and negative cases."""
from pathlib import Path
import subprocess,tempfile,sys
ROOT=Path(__file__).resolve().parents[3]
SRC=ROOT/'backend/src/main/java/com/behsazan/corebanking/fee3'
FILES=[SRC/'domain/Fee3Models.java',SRC/'r3/Fee3PreviewEngine.java',
       SRC/'r4/Fee3R4CalculationContract.java',
       SRC/'r4c1/Fee3R4C1LoanProfitContract.java',
       ROOT/'tools/fee3-backend/r4c1/Fee3R4C1Probe.java']
with tempfile.TemporaryDirectory(prefix='fee3-r4c1-java-') as tmp:
    try:
        compile=subprocess.run(['javac','-encoding','UTF-8','-d',tmp,*map(str,FILES)],
            capture_output=True,text=True,timeout=80)
        if compile.returncode:
            print('FEE3_R4C1_JAVA_COMPILE_FAIL\n'+compile.stderr[-4500:]);sys.exit(2)
        print('FEE3_R4C1_JAVA_COMPILE_PASS',flush=True)
        run=subprocess.run(['java','-cp',tmp,'Fee3R4C1Probe'],capture_output=True,
            text=True,timeout=35)
        print(run.stdout,end='')
        if run.returncode:print(run.stderr[-1500:]);sys.exit(2)
    except (OSError,subprocess.TimeoutExpired) as ex:
        print('FEE3_R4C1_JAVA_EXECUTION_FAIL',ex);sys.exit(3)
