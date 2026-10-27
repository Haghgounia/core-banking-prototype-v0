#!/usr/bin/env python3
"""Run real pure Java source contract tests without Spring, Maven or Oracle."""
from pathlib import Path
import subprocess,sys,tempfile
ROOT=Path(__file__).resolve().parents[3]
P=ROOT/'backend/src/main/java/com/behsazan/corebanking/fee3'
J=ROOT/'tools/fee3-backend/r4b1/Fee3R4B1Probe.java'
with tempfile.TemporaryDirectory(prefix='fee3-r4b1-pure-') as d:
    cmd=['javac','-encoding','UTF-8','-d',d,
         str(P/'domain/Fee3Models.java'),str(P/'r3/Fee3PreviewEngine.java'),
         str(P/'r4/Fee3R4CalculationContract.java'),
         str(P/'r4b1/Fee3R4B1ServiceContract.java'),str(J)]
    try:
        compiled=subprocess.run(cmd,capture_output=True,text=True,encoding='utf8',errors='replace',timeout=60)
        if compiled.returncode:
            print('FEE3_R4B1_PURE_JAVA_COMPILE_FAIL\n'+compiled.stderr[-4200:]);sys.exit(2)
        print('FEE3_R4B1_PURE_JAVA_COMPILE_PASS',flush=True)
        tested=subprocess.run(['java','-cp',d,'Fee3R4B1Probe'],capture_output=True,text=True,
                              encoding='utf8',errors='replace',timeout=50)
        print(tested.stdout,end='',flush=True)
        if tested.returncode:print(tested.stderr[-2000:]);sys.exit(2)
    except (OSError,subprocess.TimeoutExpired) as e:
        print('FEE3_R4B1_JAVA_TOOL_ERROR',str(e));sys.exit(3)
