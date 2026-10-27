#!/usr/bin/env python3
"""Static policy gates for additive-only R4C1 release. Not a substitute for Maven/Oracle."""
from pathlib import Path
import hashlib,sys
ROOT=Path(__file__).resolve().parents[3]
BASE=ROOT/'backend/src/main/java/com/behsazan/corebanking/fee3/r4c1'
FILES=['Fee3R4C1Controller.java','Fee3R4C1LoanProfitContract.java',
       'Fee3R4C1JalaliDayRepository.java','Fee3R4C1Service.java',
       'Fee3R4C1ProblemAdvice.java']
checks=[]
def check(name,yes):
    checks.append(bool(yes));print(('PASS' if yes else 'FAIL')+' | '+name)
for f in FILES:check(f,(BASE/f).is_file())
contract=(BASE/'Fee3R4C1LoanProfitContract.java').read_text(encoding='utf-8')
service=(BASE/'Fee3R4C1Service.java').read_text(encoding='utf-8')
repo=(BASE/'Fee3R4C1JalaliDayRepository.java').read_text(encoding='utf-8')
controller=(BASE/'Fee3R4C1Controller.java').read_text(encoding='utf-8')
check('preview-only endpoint',controller.count('@PostMapping')==1 and '/loans/profit/preview' in controller)
check('service explicitly readOnly', '@Transactional(readOnly=true)' in service)
check('active Oracle operation', 'activeOperation(' in service)
check('R3 business guard preserved','Fee3DraftPolicy.check' in service and 'Fee3SubmissionPolicy.check' in service)
check('full R4-A persisted snapshot comparison','references.resolve(draft)' in service)
check('only static formula templates', 'TPL_E624746F262F6E' in contract and 'TPL_7F2FE580EDB351' in contract)
check('no client yearFraction', 'UNREVIEWED_INPUT_FIELDS' in contract and 'yearFactor(' in contract)
check('CAL2 sourced from canonical day', '.CANONICAL_DAY' in repo and '.CALENDAR_DATE' in repo)
check('not Gregorian day denominators','IS_LEAP_YEAR' in repo and '365' in contract and '366' in contract)
check('no tax determination', 'TAX_TREATMENT_NOT_REVIEWED_FOR_PROFIT' in contract)
check('no posting flags','false,false,false,false' in contract)
for name,text in [('service',service),('calendar',repo)]:
    check('no write SQL in '+name,not any(verb in text.upper() for verb in
        ['INSERT INTO ','UPDATE FEE3.','DELETE FROM ','MERGE INTO ','COMMIT ','NEXTVAL']))
check('nonzero package sha256',len(hashlib.sha256(contract.encode()).hexdigest())==64)
print(f'FEE3_R4C1_SOURCE_PASS={sum(checks)} FAIL={len(checks)-sum(checks)}')
sys.exit(0 if all(checks) else 2)
