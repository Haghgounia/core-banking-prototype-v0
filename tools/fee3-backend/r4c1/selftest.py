#!/usr/bin/env python3
"""Offline protocol tests; no Oracle/HTTP required. Test script is NOT a real runtime qualification."""
from __future__ import annotations
import contextlib,copy,importlib.util,io,sys
from decimal import Decimal,ROUND_HALF_UP
from pathlib import Path
HERE=Path(__file__).resolve().parent
spec=importlib.util.spec_from_file_location('fee3_r4c1_runtime',HERE/'runtime-readonly.py')
rt=importlib.util.module_from_spec(spec);spec.loader.exec_module(rt)
PASS=0;FAIL=0
def test(name,good):
    global PASS,FAIL
    if good:PASS+=1;print('PASS | '+name)
    else:FAIL+=1;print('FAIL | '+name)
def answer(status='SIMULATED',fee='18000000',daily=False):
    if status=='BLOCKED':
        return {'status':'BLOCKED','persisted':False,'financialPosting':False,'approved':False,
                'published':False,'calculation':None,'snapshotSha256':None,
                'diagnostics':['BAD_INPUT']}
    breakdown=([{'days':10,'yearDenominator':365,'calendarDatasetEvidence':'CAL2:PERSIAN:1:1'}]
               if daily else [{'contractMonths':12}])
    return {'status':'SIMULATED','persisted':False,'financialPosting':False,'approved':False,
            'published':False,'simulationMode':'UNVERIFIED_UAT_DRAFT',
            'versionReference':{'status':'UNSAVED'},'snapshotSha256':'a'*64,
            'diagnostics':['CLIENT_CONTRACT_RATE_UNVERIFIED'],
            'calculation':{'status':'SIMULATED','persisted':False,'financialPosting':False,
                           'feeIrr':fee,'taxIrr':'0','breakdown':breakdown}}

def mock_runtime(draftWrites=False,workflow=False,formulas=True,daily=True):
    traffic=[]
    def fake(base,method,path,body=None):
        traffic.append((method,path,body))
        if method=='GET':
            if path=='/readiness':return {'schema':'FEE3','readsEnabled':True,
                'draftWritesEnabled':draftWrites,'missingTables':[],
                'calcItemCount':297,'tariffVersionCount':300,'oracleBusinessDate':'2026-10-27'}
            if path=='/workflow/readiness':return {k:workflow for k in
                ('workflowEnabled','checkerDecisionEnabled','approvalEnabled','postingEnabled',
                 'authenticated','makerRole','checkerRole')}
            if path=='/references/operations':return [{'code':'LOAN_PROFIT','module':'LOAN',
                'active':True,'allowedAmountKinds':['FINANCING_PROFIT']}]
            if path=='/references/formulas':return ([{'code':x,'active':True,'method':'FORMULA'}
                for x in ('TPL_E624746F262F6E','TPL_7F2FE580EDB351')] if formulas else [])
            raise AssertionError('Unexpected GET '+path)
        assert method=='POST' and path=='/r4/loans/profit/preview'
        ctx=body['draft']['version']['uiContext']
        basis=ctx['loanPolicy']['dayCountBasis']
        indata=body['inputs']
        r=body['draft']['rules'][0]
        if basis=='JALALI_ACT_ACT' and not daily:return answer('BLOCKED')
        if (set(indata)-({'amount','contractRate','contractReference','rateEvidenceReference'}|
            ({'periodStart','periodEndExclusive'} if basis=='JALALI_ACT_ACT' else {'contractMonths'})) or
            'contractReference' not in indata or indata.get('contractRate')=='1.123456789' or
            indata.get('contractMonths')=='1.5' or
            '_fceEditableExpressionV1' in r['parameters'] or
            r['formulaCode']=='LOAN_LATE_HOLIDAY' or
            ctx['loanModelProfile']=='PARTICIPATION_REALIZED' or
            body['draft']['version']['taxMode']=='UNREVIEWED' or
            body['draft']['calcItemId'] is not None or r['rate'] is not None):
            return answer('BLOCKED')
        if basis=='CONTRACT_MONTHS':return answer()
        fee=(Decimal('18000000')*Decimal('10')/Decimal('365')).quantize(
            Decimal(1),rounding=ROUND_HALF_UP)
        return answer(fee=str(fee),daily=True)
    old=rt.http;rt.http=fake
    try:
        with contextlib.redirect_stdout(io.StringIO()):out=rt.run('http://127.0.0.1:8091')
    finally:rt.http=old
    return out,traffic

monthly=rt.request('2026-10-27','CONTRACT_MONTHS')
daily_request=rt.request('2026-10-27','JALALI_ACT_ACT')
test('contract month sample only contract months',monthly['inputs']['contractMonths']==12 and
     'yearFraction' not in monthly['inputs'])
test('Jalali sample precise non-deduced year fraction',daily_request['inputs']['periodStart']=='2026-10-27'
     and daily_request['inputs']['periodEndExclusive']=='2026-11-06' and 'yearFraction' not in daily_request['inputs'])
test('only unpersisted client-supplied UAT contract',monthly['draft']['calcItemId'] is None
     and monthly['draft']['tariffVersionId'] is None and monthly['draft']['version']['sourceId'] is None)
test('client evidence is explicitly marked unverified',monthly['inputs']['rateEvidenceReference'].endswith('UNVERIFIED'))
result,traffic=mock_runtime()
test('all mock HTTP controls pass',result['passed']>15 and result['failed']==0)
test('strict preview GET/POST route allowlist',all(method=='GET' and path in rt.GET or
    method=='POST' and path in rt.POST for method,path,_ in traffic))
test('no GET with body',all(payload is None for method,path,payload in traffic if method=='GET'))
test('no write/approval/posting URLs',not any('posting' in path or 'submit' in path or
    'approve' in path or '/items' in path for _,path,_ in traffic))
test('positive monthly and Jalali cases present',sum(method=='POST' for method,_,_ in traffic)>=13)
result,traffic=mock_runtime(draftWrites=True)
test('draft writes enabled blocks all preview posts',result['failed']>0 and
    all(method!='POST' for method,_,_ in traffic))
result,traffic=mock_runtime(workflow=True)
test('workflow enabled blocks all preview posts',result['failed']>0 and
    all(method!='POST' for method,_,_ in traffic))
result,traffic=mock_runtime(formulas=False)
test('missing formula reference blocks all preview posts',result['failed']>0 and
    all(method!='POST' for method,_,_ in traffic))
result,traffic=mock_runtime(daily=False)
test('missing CAL2 coverage must fail qualification',result['failed']>0)
test('blocked response never exposes result fee',rt.blocked(answer('BLOCKED')))
test('positive response cannot have posting true',not rt.safe(dict(answer(),financialPosting=True)))
test('wrong numeric fee never accepted',not rt.simulated(answer(),18000001))
try:rt.http('http://127.0.0.1:8091','POST','/workflow/approve',{})
except Exception as ex:blocked=True
else:blocked=False
test('POST approve route rejected',blocked)
print(f'FEE3_R4C1_SELFTEST_PASS={PASS} FAIL={FAIL}')
sys.exit(0 if FAIL==0 else 2)
