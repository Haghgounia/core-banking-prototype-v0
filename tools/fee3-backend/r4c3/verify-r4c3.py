#!/usr/bin/env python3
"""Static production safety contract checks; must not claim Maven/Oracle acceptance."""
from pathlib import Path
import json,re,sys
ROOT=Path(__file__).resolve().parents[3]
JAVA=ROOT/'backend/src/main/java/com/behsazan/corebanking/fee3/r4c3'
CODE=(JAVA/'Fee3R4C3QardContract.java').read_text(encoding='utf8')
SERVICE=(JAVA/'Fee3R4C3Service.java').read_text(encoding='utf8')
CAL=(JAVA/'Fee3R4C3JalaliEventRepository.java').read_text(encoding='utf8')
CTRL=(JAVA/'Fee3R4C3Controller.java').read_text(encoding='utf8')
RUN=(ROOT/'tools/fee3-backend/r4c3/runtime-readonly.py').read_text(encoding='utf8')
SHELL=(ROOT/'tools/fee3-backend/r4c3/run-runtime-readonly.cmd').read_text(encoding='ascii')
SQL=(ROOT/'docs/install/FEE3-R4C3-ORACLE-READONLY-VERIFY.sql').read_text(encoding='utf8')
CHECK=[]
def check(name,value):
    CHECK.append((name,bool(value)));print(('PASS' if value else 'FAIL')+' | '+name)
check('Four model timings whitelisted',all(s in CODE for s in (
    '"ON_ANNIVERSARY"','"FIRST_INSTALLMENT_OF_PERIOD"',
    '"JALALI_YEAR_START"','"CONTRACT_YEAR_START"')))
check('Year-start policies distinct',all(s in CODE for s in (
    '"JALALI_CALENDAR_YEAR"','"LOAN_CONTRACT_YEAR"','"START_OF_YEAR"')))
check('Exact Qard operation and amount kind',all(s in CODE for s in (
    '"QARD_FEE".equals(i.operation())','"QARD_FEE".equals(i.amountKind())')))
check('No loan profit formula in Qard engine','TPL_E624746F262F6E' in CODE and
    'PARTICIPATION_REALIZED' not in CODE)
check('R4C1 CAL2 JALALI_ACT_ACT year fraction reused',
    'Fee3R4C1LoanProfitContract.yearFactor(from,to,slices)' in CODE)
check('One unique timing assessment key is candidate only',
    'candidateAssessmentKey' in CODE and 'NO_DUPLICATE_CERTIFICATION' in CODE)
check('No authoritative history claim', 'NOT_VERIFIED_NO_AUTHORITATIVE_CHARGE_HISTORY' in CODE and
    'PREVIOUS_CHARGE_HISTORY_NOT_CHECKED' in CODE)
check('No financial permission switches',all(x in CODE for x in (
    'NOT_A_CHARGE_AUTHORIZATION','NO_ACCOUNTING_POSTING','NO_ORACLE_WRITE')))
check('Reject client formula and yearFraction',
    'CLIENT_FORMULA_PARAMETERS_FORBIDDEN' in CODE and 'QARD_UNREVIEWED_INPUT_FIELDS' in CODE)
check('Separate rate and money scale',
    'QARD_RATE_NUMBER18_8' in CODE and 'QARD_AMOUNT_NUMBER22_2' in CODE)
check('No pre-approved bank tax or waiver',
    'QARD_TAX_OR_FINANCIAL_POLICY_UNREVIEWED' in CODE)
check('No payment or charge schedule registration',
    '"SIMULATED"' in CODE and '"BLOCKED"' in CODE and '"APPROVED"' not in CODE)
check('Only read-only transaction service','@Transactional(readOnly=true)' in SERVICE)
check('No database mutation code new Java',not re.search(
    r'\b(?:INSERT\s+INTO|UPDATE\s+\w+|DELETE\s+FROM|MERGE\s+INTO|TRUNCATE|CREATE\s+TABLE|DROP\s+TABLE)\b',
    '\n'.join(p.read_text(encoding='utf8') for p in JAVA.glob('*.java')),re.I))
check('Oracle operation checked before calculation',SERVICE.index('oracle.activeOperation')<
    SERVICE.index('QardContract.calculate'))
check('Oracle active formula checked','oracle.formulas()' in SERVICE and
    '"FORMULA".equals(f.method())' in SERVICE)
check('R3 submission policy reused','Fee3SubmissionPolicy.check(draft)' in SERVICE)
check('Oracle draft linkage verified','references.resolve(draft)' in SERVICE)
check('CAL2 canonical event SQL only SELECT',
    'SELECT D.ISO_DATE_TEXT' in CAL and 'C.MONTH_NO,C.DAY_NO' in CAL and
    not re.search(r'(?im)^\s*(?:INSERT|UPDATE|DELETE|MERGE|ALTER|DROP|CREATE)\b',CAL))
check('CAL2 year gap and day evidence checked',
    'QARD_EVENT_DAY_NOT_COVERED_BY_CAL2_PERIOD' in CODE and
    'QARD_CAL2_EVENT_MAPPING_UNTRUSTED' in CODE)
check('Single nonpersistent specialized API','@PostMapping("/preview")' in CTRL and
    '/api/v1/fee3/r4/qard' in CTRL)
check('Runtime only loopback and no redirect',
    "parsed.hostname!='127.0.0.1'" in RUN and 'NoRedirect' in RUN and 'ProxyHandler({})' in RUN)
check('Runtime strict GET/POST allowlist',
    "POST={'/r4/qard/preview'}" in RUN and
    "GET={'/readiness','/workflow/readiness','/references/operations','/references/formulas'}" in RUN)
check('Runtime fails closed before POST','FAIL_CLOSED_PREVIEW_NOT_SENT' in RUN)
check('Runtime verifies source counts','after.get(k)==ready.get(k)' in RUN)
check('Runtime CMD has no secret or write operations',
    'runtime-readonly.py' in SHELL and 'sqlplus' not in SHELL and 'curl' not in SHELL)
# SQL may contain vocabulary in literal strings; reject whole writable SQL statements.
check('SQL SELECT only and read-only metadata',
    'FEE3.FCE_CALC_ITEM' in SQL and 'CAL2.CALENDAR_DATE' in SQL and
    not re.search(r'(?im)^\s*(?:INSERT|UPDATE|DELETE|MERGE|ALTER|DROP|TRUNCATE|CREATE|COMMIT|ROLLBACK|EXEC|BEGIN|DECLARE|GRANT|REVOKE)\b',SQL))
check('No patch migration DDL supplied',not (ROOT/'tools/fee3-backend/r4c3/migrate.sql').exists())
check('All new production classes exclusively r4c3',
    len(list(JAVA.glob('*.java')))==5)
passed=sum(bool(x) for _,x in CHECK)
summary={'protocol':'FEE3_R4C3_SOURCE_V1','passed':passed,'failed':len(CHECK)-passed,
         'checks':[{'name':x,'result':'PASS' if okay else 'FAIL'} for x,okay in CHECK]}
out=ROOT/'docs/install/FEE3-R4C3-SOURCE-RESULT.json';out.parent.mkdir(parents=True,exist_ok=True)
out.write_text(json.dumps(summary,indent=2)+'\n',encoding='utf8')
print(f'FEE3_R4C3_SOURCE_PASS={passed} FAIL={len(CHECK)-passed}')
sys.exit(0 if len(CHECK)==passed else 2)
