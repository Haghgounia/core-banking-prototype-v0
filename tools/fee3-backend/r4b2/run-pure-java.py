#!/usr/bin/env python3
"""Compile and run actual R4B2 calculation source. No framework/Oracle mocks needed."""
import subprocess,sys,tempfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[3]
SRC=ROOT/'backend/src/main/java/com/behsazan/corebanking/fee3'
JAVA=ROOT/'tools/fee3-backend/r4b2/Fee3R4B2Probe.java'
with tempfile.TemporaryDirectory(prefix='fee3-r4b2-') as path:
    cmd=['javac','-encoding','UTF-8','-d',path,*map(str,[SRC/'domain/Fee3Models.java',
        SRC/'r3/Fee3PreviewEngine.java',SRC/'r4/Fee3R4CalculationContract.java',
        SRC/'r4b2/Fee3R4B2GuaranteeContract.java',JAVA])]
    try:
        compiled=subprocess.run(cmd,capture_output=True,text=True,encoding='utf8',errors='replace',timeout=55)
        if compiled.returncode:
            print('FEE3_R4B2_COMPILE_FAIL',compiled.stderr[-3500:]);sys.exit(2)
        print('FEE3_R4B2_PURE_COMPILE_PASS',flush=True)
        res=subprocess.run(['java','-cp',path,'Fee3R4B2Probe'],capture_output=True,
            text=True,encoding='utf8',errors='replace',timeout=45)
        print(res.stdout,end='')
        if res.returncode:print(res.stderr[-3500:]);sys.exit(2)
    except (OSError,subprocess.TimeoutExpired) as error:
        print('FEE3_R4B2_JAVA_TOOL_ERROR',str(error));sys.exit(3)
