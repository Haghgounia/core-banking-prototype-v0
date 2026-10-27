#!/usr/bin/env python3
"""Static control of exclusive R4C2 feature, no migration and no bank mutation path."""
import re,sys
from pathlib import Path
ROOT=Path(__file__).resolve().parents[3]
MAIN=ROOT/'backend/src/main/java/com/behsazan/corebanking/fee3/r4c2'
TEXT=(MAIN/'Fee3R4C2LoanModelContract.java').read_text('utf8')
SVC=(MAIN/'Fee3R4C2Service.java').read_text('utf8')
CTRL=(MAIN/'Fee3R4C2Controller.java').read_text('utf8')
RUNTIME=(ROOT/'tools/fee3-backend/r4c2/runtime-readonly.py').read_text('utf8')
checks={
 'Five separate bank-model profiles':all(x in TEXT for x in
   ('PARTICIPATION_REALIZED','PARTICIPATION_ESTIMATE','DEBT_PURCHASE_SPREAD',
    'SALAM_SPREAD','SALAM_ESTIMATE')),
 'Contract profile has exact event+formula+day-count mapping':'new Spec(' in TEXT and 'MODEL_FORMULA_MISMATCH' in TEXT,
 'Client yearFraction and formula overrides blocked':'CLIENT_FORMULA_OVERRIDE_FORBIDDEN' in TEXT and 'UNREVIEWED_INPUT_FIELDS' in TEXT,
 'No tax/discount/waiver model enabled':'TAX_AND_FINANCIAL_POLICY_OUT_OF_SCOPE' in TEXT,
 'Contract money and rate scales checked':'scale(b)<=2' in TEXT and 'scale(b)<=8' in TEXT,
 'Negative spreads must not silently become profit':'NEGATIVE_SPREAD_NEEDS_LOSS_POLICY' in TEXT,
 'Realized share is not an annual rate':'realizedProfit' in TEXT and 'sharePercent' in TEXT and 'contractRate' in TEXT,
 'Estimates derive year fraction from CAL2-controlled R4C1':'Fee3R4C1LoanProfitContract.yearFactor' in TEXT,
 'Event business date cannot be future':'EVENT_NOT_AT_BANK_BUSINESS_DATE' in TEXT,
 'Client claim is unverified':'CLIENT_CONTRACT_EVIDENCE_UNVERIFIED' in TEXT,
 'Separate gross/estimate result nature':'resultKind' in TEXT and 'NOT_RECOGNIZED_PROFIT' in TEXT,
 'SHA256 stable snapshot evidence':'SHA-256' in TEXT and 'fingerprint' in TEXT,
 'No financial authorization flags':'false,false,false,false' in TEXT and 'NO_ACCOUNTING_POSTING' in TEXT,
 'Spring service is explicitly readOnly':'@Transactional(readOnly=true)' in SVC,
 'No SQL write code in new production package':not re.search(r'(?i)\b(INSERT|UPDATE|DELETE|MERGE|DROP|ALTER|TRUNCATE)\s+(INTO|FROM|TABLE|FEE3\.)',TEXT+SVC+CTRL),
 'No repository mutations through new service':not re.search(r'(?i)\b(save|persist|postToLedger|executeCharge|applyChange)\s*\(',SVC),
 'Oracle operation is checked before preview calculation':'activeOperation' in SVC,
 'Oracle formula reference read before arithmetic':'oracle.formulas()' in SVC,
 'Exact physical Oracle ID proof reused':'references.resolve(draft)' in SVC,
 'R3 submission check remains':'Fee3SubmissionPolicy.check' in SVC,
 'Current R4-C1 CAL2 repository reused':'calendar.yearSlices' in SVC,
 'No arbitrary runtime formula parser':'EXPRESSION_TEXT' not in TEXT+SVC and 'ScriptEngine' not in TEXT+SVC,
 'Only one specialized preview endpoint':'@PostMapping("/loans/models/preview")' in CTRL and CTRL.count('@PostMapping')==1,
 'Loopback-only HTTP test client':'127.0.0.1' in RUNTIME and 'ProxyHandler({})' in RUNTIME,
 'Runtime denies workflow/writes before POST':'FAIL_CLOSED_BEFORE_PREVIEW_POST' in RUNTIME,
 'Runtime allowlist includes no posting endpoint':"POST={'/r4/loans/models/preview'}" in RUNTIME,
 'Runtime reports source and version counts':'calcItemCount' in RUNTIME and 'tariffVersionCount' in RUNTIME,
 'No SQL/DDL migration supplied':not list((ROOT/'tools/fee3-backend/releases/r4c2/overlay').rglob('*.sql'))
}
failed=0
for title,yes in checks.items():
 print(('PASS' if yes else 'FAIL')+' | '+title)
 if not yes:failed+=1
print(f'FEE3_R4C2_SOURCE_PASS={len(checks)-failed} FAIL={failed}')
sys.exit(0 if not failed else 2)
