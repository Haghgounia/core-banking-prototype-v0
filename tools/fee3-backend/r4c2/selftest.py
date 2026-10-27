#!/usr/bin/env python3
"""Mocked transport regression only (not proof of actual Oracle or Maven)."""
from __future__ import annotations
import contextlib,copy,importlib.util,io,sys
from pathlib import Path
HERE=Path(__file__).resolve().parent
spec=importlib.util.spec_from_file_location('fee3_r4c2_runtime',HERE/'runtime-readonly.py')
rt=importlib.util.module_from_spec(spec);spec.loader.exec_module(rt)
PASSED=FAILED=0
def test(name,condition):
    global PASSED,FAILED
    if condition:PASSED+=1;print('PASS | '+name)
    else:FAILED+=1;print('FAIL | '+name)
def reply(profile,day,blocked=False,code="MOCK_INVALID_INPUT"):
    if blocked:return {'status':'BLOCKED','approved':False,'published':False,'persisted':False,
        'financialPosting':False,'calculation':None,'snapshotSha256':None,
        'diagnostics':[code]}
    estimate=profile.endswith('ESTIMATE')
    breakdown=([{'days':10,'yearDenominator':365,'calendarDatasetEvidence':'CAL2:PERSIAN:123:456'}]
        if estimate else [{'eventDate':day}])
    totals={'PARTICIPATION_REALIZED':'36000000','DEBT_PURCHASE_SPREAD':'8000000',
            'SALAM_SPREAD':'15000000'}
    if estimate:
        from decimal import Decimal,ROUND_HALF_UP
        amount=(Decimal('100000000')*Decimal('18')/Decimal('100')*
            Decimal('10')/Decimal('365')).quantize(Decimal(1),rounding=ROUND_HALF_UP)
        total=str(amount)
    else:total=totals[profile]
    return {'status':'SIMULATED','approved':False,'published':False,'persisted':False,
        'financialPosting':False,'versionReference':{'status':'UNSAVED'},
        'simulationMode':'UNVERIFIED_UAT_DRAFT','resultKind':rt.NATURE[profile],
        'snapshotSha256':'b'*64,'diagnostics':['CLIENT_CONTRACT_EVIDENCE_UNVERIFIED'],
        'calculation':{'status':'SIMULATED','modelProfile':profile,'resultKind':rt.NATURE[profile],
            'rawIrr':total,'roundedIrr':total,'breakdown':breakdown}}
def mock(allow_writes=False,allow_workflow=False,missing_formula=False):
    traffic=[];day='2026-10-27'
    approved={p:rt.request(day,p) for p in rt.SPEC}
    approved[('PARTICIPATION_REALIZED','MUDARABA')]=rt.request(day,'PARTICIPATION_REALIZED','MUDARABA')
    def transport(base,method,path,body=None):
        traffic.append((method,path,body))
        if method=='GET':
            if path=='/readiness':return {'schema':'FEE3','readsEnabled':True,'missingTables':[],
                'draftWritesEnabled':allow_writes,'calcItemCount':297,'tariffVersionCount':300,
                'oracleBusinessDate':day}
            if path=='/workflow/readiness':return {k:allow_workflow for k in
                ('workflowEnabled','checkerDecisionEnabled','approvalEnabled','postingEnabled',
                 'authenticated','makerRole','checkerRole')}
            if path=='/references/operations':return [{'module':'LOAN','code':'LOAN_PROFIT',
                'active':True,'allowedAmountKinds':['FINANCING_PROFIT']}]
            if path=='/references/formulas':return ([{'code':f,'active':True,'method':'FORMULA'}
                for f in {x[1] for x in rt.SPEC.values()}] if not missing_formula else [])
            raise AssertionError('unexpected GET')
        assert method=='POST' and path=='/r4/loans/models/preview'
        p=body['draft']['version']['uiContext']['loanModelProfile']
        if body in approved.values():return reply(p,day)
        x=body['inputs'];v=body['draft']['version'];r=body['draft']['rules'][0]
        if body.get('effectiveOn') is not None:
            code='HISTORICAL_EFFECTIVE_UNSUPPORTED'
        elif v.get('taxMode')=='TAXABLE' or v.get('financialPolicy'):
            code='TAX_AND_FINANCIAL_POLICY_OUT_OF_SCOPE'
        elif v['uiContext'].get('loanCalculationEvent')=='PERIOD_PROFIT':
            code='LOAN_MODEL_EVENT_MISMATCH_OR_PENDING'
        elif v['uiContext'].get('loanContractTypes')==['DEBT_PURCHASE'] and p=='PARTICIPATION_ESTIMATE':
            code='CONTRACT_TYPE_MODEL_MISMATCH'
        elif r.get('rate') is not None:code='UNSUPPORTED_MODEL_RULE'
        elif r.get('formulaCode')=='LOAN_LATE_HOLIDAY':code='MODEL_FORMULA_MISMATCH'
        elif '_fceEditableExpressionV1' in r.get('parameters',{}):code='CLIENT_FORMULA_OVERRIDE_FORBIDDEN'
        elif body['draft'].get('calcItemId') is not None:code='PARTIAL_ORACLE_REFERENCE'
        elif p=='PARTICIPATION_REALIZED':
            if 'contractRate' in x:code='UNREVIEWED_INPUT_FIELDS'
            elif x.get('eventDate')!=day:code='EVENT_NOT_AT_BANK_BUSINESS_DATE'
            elif x.get('realizationStatementReference')=='':code='REALIZATION_EVIDENCE_REQUIRED'
            else:code='REALIZED_PROFIT_OR_SHARE_INVALID'
        elif p=='DEBT_PURCHASE_SPREAD':
            code=('DEBT_PURCHASE_EVIDENCE_REQUIRED' if x.get('purchasePriceEvidenceReference')==''
                  else 'NEGATIVE_SPREAD_NEEDS_LOSS_POLICY')
        elif p=='SALAM_SPREAD':code='NEGATIVE_SPREAD_NEEDS_LOSS_POLICY'
        elif p.endswith('ESTIMATE'):
            if 'yearFraction' in x:code='UNREVIEWED_INPUT_FIELDS'
            elif x.get('estimateBasisReference')=='':code='ESTIMATE_CONTRACT_EVIDENCE_REQUIRED'
            else:code='ESTIMATE_RATE_NUMBER_18_8'
        else:code='MOCK_INVALID_INPUT'
        return reply(p,day,True,code)

    original=rt.http;rt.http=transport
    try:
        with contextlib.redirect_stdout(io.StringIO()):out=rt.run('http://127.0.0.1:8091')
    finally:rt.http=original
    return out,traffic

test('exact 5 model fixtures',len(rt.SPEC)==5 and len(rt.NATURE)==5)
test('realization input uses share not annual rate','sharePercent' in rt.request('2026-10-27',
    'PARTICIPATION_REALIZED')['inputs'] and 'contractRate' not in rt.request('2026-10-27',
    'PARTICIPATION_REALIZED')['inputs'])
test('estimate has no client year fraction','yearFraction' not in rt.request('2026-10-27',
    'SALAM_ESTIMATE')['inputs'])
test('spread has no customer supplied annual rate','contractRate' not in rt.request('2026-10-27',
    'DEBT_PURCHASE_SPREAD')['inputs'])
test('no persisted Oracle draft IDs',all(rt.request('2026-10-27',p)['draft']['calcItemId'] is None
    for p in rt.SPEC))
out,traffic=mock()
test('all simulated transport assertions pass',out['failed']==0 and out['passed']>25)
test('GET is pure and POST simulation only',all((m=='GET' and p in rt.GET and b is None)
    or (m=='POST' and p in rt.POST and b is not None) for m,p,b in traffic))
test('positive and negative tests actually sent',sum(m=='POST' for m,_,_ in traffic)>20)
test('no save, workflow or posting URLs',not any('/posting' in p or '/approve' in p or
    '/submit' in p or '/write' in p or '/items' in p for _,p,_ in traffic))
out,traffic=mock(allow_writes=True)
test('draft writes enabled blocks BEFORE POST',out['failed']>0 and all(m!='POST' for m,_,_ in traffic))
out,traffic=mock(allow_workflow=True)
test('workflow enabled blocks BEFORE POST',out['failed']>0 and all(m!='POST' for m,_,_ in traffic))
out,traffic=mock(missing_formula=True)
test('missing active formula blocks BEFORE POST',out['failed']>0 and all(m!='POST' for m,_,_ in traffic))
test('missing evidence response must not be counted as simulated',not rt.simulation(
    reply('PARTICIPATION_REALIZED','2026-10-27',True),'PARTICIPATION_REALIZED','36000000'))
test('approved true response forbidden',not rt.safe(dict(reply('SALAM_SPREAD','2026-10-27'),approved=True)))
test('rounded money mismatch forbidden',not rt.simulation(reply('SALAM_SPREAD','2026-10-27'),
    'SALAM_SPREAD','15000001'))
test('blocked response has no financial result',rt.blocked(reply('SALAM_SPREAD','2026-10-27',True)))
try:rt.http('http://127.0.0.1:8091','POST','/workflow/approve',{})
except RuntimeError:guarded=True
else:guarded=False
test('approval endpoint is absent from allowlist',guarded)
print(f'FEE3_R4C2_SELFTEST_PASS={PASSED} FAIL={FAILED}')
sys.exit(0 if FAILED==0 else 2)
