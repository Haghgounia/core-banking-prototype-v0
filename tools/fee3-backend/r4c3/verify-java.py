#!/usr/bin/env python3
"""Framework stub signature check; NOT a real Maven run."""
from pathlib import Path
import runpy,subprocess,tempfile,sys
ROOT=Path(__file__).resolve().parents[3]
SRC=ROOT/'backend/src/main/java/com/behsazan/corebanking/fee3'
STUB=runpy.run_path(str(ROOT/'tools/fee3-backend/r4c1/verify-java.py'))['STUB']
def main():
    with tempfile.TemporaryDirectory(prefix='fee3-r4c3-signature-') as folder:
        root=Path(folder);sources=[]
        for name,text in STUB.items():
            path=root/'stubs'/name;path.parent.mkdir(parents=True,exist_ok=True)
            path.write_text(text,encoding='utf8');sources.append(path)
        sources += [SRC/'domain/Fee3Models.java',SRC/'r3/Fee3PreviewEngine.java',
            SRC/'r3/Fee3SubmissionPolicy.java',SRC/'r4/Fee3R4CalculationContract.java',
            SRC/'application/Fee3DraftPolicy.java']
        sources += sorted((SRC/'r4c1').glob('*.java'))
        sources += sorted((SRC/'r4c3').glob('*.java'))
        sources += [ROOT/'backend/src/test/java/com/behsazan/corebanking/fee3/r4c3/Fee3R4C3QardContractTest.java']
        try:
            run=subprocess.run(['javac','--release','21','-encoding','UTF-8','-d',str(root/'classes'),
                *map(str,sources)],capture_output=True,text=True,timeout=90)
        except (OSError,subprocess.TimeoutExpired) as e:
            print('FEE3_R4C3_JAVA_SIGNATURE_UNAVAILABLE',str(e));return 2
        if run.returncode:
            print('FEE3_R4C3_JAVA_SIGNATURE_FAIL\n'+run.stderr[-6000:]);return 2
        print('FEE3_R4C3_JAVA_SIGNATURE_PASS=5 new production classes + JUnit signature (framework stubs)')
        return 0
if __name__=='__main__':sys.exit(main())
