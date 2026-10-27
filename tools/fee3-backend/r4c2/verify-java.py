#!/usr/bin/env python3
"""Signature compilation with framework stubs only; NOT full Maven/Oracle runtime."""
import runpy,subprocess,tempfile,sys
from pathlib import Path
ROOT=Path(__file__).resolve().parents[3]
MAIN=ROOT/'backend/src/main/java/com/behsazan/corebanking/fee3'
parent=runpy.run_path(str(ROOT/'tools/fee3-backend/r4c1/verify-java.py'))
STUB=parent['STUB']
def main():
    with tempfile.TemporaryDirectory(prefix='fee3-r4c2-signature-') as name:
        tmp=Path(name);files=[]
        for relative,src in STUB.items():
            p=tmp/'stubs'/relative;p.parent.mkdir(parents=True,exist_ok=True)
            p.write_text(src,encoding='utf8');files.append(p)
        files += [MAIN/'domain/Fee3Models.java',MAIN/'r3/Fee3PreviewEngine.java',
                  MAIN/'r3/Fee3SubmissionPolicy.java',MAIN/'r4/Fee3R4CalculationContract.java',
                  MAIN/'application/Fee3DraftPolicy.java']
        files += sorted((MAIN/'r4c1').glob('*.java'))
        files += sorted((MAIN/'r4c2').glob('*.java'))
        files += [ROOT/'backend/src/test/java/com/behsazan/corebanking/fee3/r4c2/Fee3R4C2LoanModelContractTest.java']
        try:
            p=subprocess.run(['javac','--release','21','-encoding','UTF-8','-d',str(tmp/'classes'),
                *map(str,files)],capture_output=True,text=True,encoding='utf8',errors='replace',timeout=110)
        except (OSError,subprocess.TimeoutExpired) as e:
            print('FEE3_R4C2_JAVA_SIGNATURE_UNAVAILABLE',e);return 2
        if p.returncode:
            print('FEE3_R4C2_JAVA_SIGNATURE_FAIL\n'+p.stderr[-5000:]);return 2
        print('FEE3_R4C2_JAVA_SIGNATURE_PASS=4 new production classes + JUnit signature (framework stubs)')
        return 0
if __name__=='__main__':sys.exit(main())
