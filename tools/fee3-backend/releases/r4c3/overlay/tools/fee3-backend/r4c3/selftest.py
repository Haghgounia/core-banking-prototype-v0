#!/usr/bin/env python3
"""No-network HTTP harness regression; mock != Oracle evidence."""
from __future__ import annotations
import copy,importlib.util,io,contextlib,sys
from decimal import Decimal,ROUND_HALF_UP,localcontext
from pathlib import Path
HERE=Path(__file__).resolve().parent
spec=importlib.util.spec_from_file_location('fee3_r4c3_transport',HERE/'runtime-readonly.py')
rt=importlib.util.module_from_spec(spec);spec.loader.exec_module(rt)
YES=NO=0
def check(label,condition):
    global YES,NO
    if condition:YES+=1;print('PASS | '+label)
    else:NO+=1;print('FAIL | '+label)
def reply(model,day,blocked=False,code='MOCK_UNEXPECTED'):
    if blocked:return {'status':'BLOCKED','approved':False,'published':False,'persisted':False,
        'financialPosting':False,'calculation':None,'snapshotSha256':None,'diagnostics':[code]}
    with localcontext() as ctx:
        ctx.prec=34
        factor=Decimal(10)/Decimal(365)
        raw=Decimal('100000000')*Decimal(4)/Decimal(100)*factor
        rounded=raw.quantize(Decimal('1'),rounding=ROUND_HALF_UP)
    return {'status':'SIMULATED','approved':False,'published':False,'persisted':False,
        'financialPosting':False,'simulationMode':'UNVERIFIED_UAT_DRAFT','timingModel':model,
        'versionReference':{'status':'UNSAVED'},
        'snapshotSha256':'b'*64,
        'diagnostics':['PREVIOUS_CHARGE_HISTORY_NOT_CHECKED','NO_DUPLICATE_CERTIFICATION',
            'NOT_A_CHARGE_AUTHORIZATION','NO_ORACLE_WRITE','NO_ACCOUNTING_POSTING',
            'LOAN_SCHEDULE_UNVERIFIED'],
        'calculation':{'status':'SIMULATED','timingModel':model,
            'resultKind':'QARD_SERVICE_FEE_NOT_DEMANDED',
            'duplicateCheckStatus':'NOT_VERIFIED_NO_AUTHORITATIVE_CHARGE_HISTORY',
            'candidateAssessmentKey':('d'*64 if model=='CONTRACT_YEAR_START' else 'c'*64),
            'eventJalaliYear':1405,'eventJalaliMonth':8,'eventJalaliDay':5,
            'eventJalaliOrdinal':221,'eventCalendarDatasetEvidence':'CAL2:PERSIAN:101:201',
            'breakdown':[{'days':10,'yearDenominator':365,
                'calendarDatasetEvidence':'CAL2:PERSIAN:101:201'}],
            'yearFraction':str(factor),
            'rawIrr':str(raw),'roundedIrr':str(rounded)}}

def simulate(writes=False,workflow=False,noformula=False):
    day='2026-10-27';traffic=[]
    base={m:rt.request(day,m) for m in rt.SPEC}
    def fake(baseurl,method,path,body=None):
        traffic.append((method,path,body))
        if method=='GET':
            if path=='/readiness':return {'schema':'FEE3','readsEnabled':True,'missingTables':[],
                'draftWritesEnabled':writes,'oracleBusinessDate':day,
                'calcItemCount':297,'tariffVersionCount':300}
            if path=='/workflow/readiness':return {k:workflow for k in
                ('workflowEnabled','checkerDecisionEnabled','approvalEnabled','postingEnabled',
                 'authenticated','makerRole','checkerRole')}
            if path=='/references/operations':return [{'module':'LOAN','code':'QARD_FEE','active':True,
                'allowedAmountKinds':['QARD_FEE']}]
            if path=='/references/formulas':return [] if noformula else [{
                'code':'TPL_E624746F262F6E','method':'FORMULA','active':True}]
            raise ValueError('Unexpected GET')
        assert method=='POST' and path=='/r4/qard/preview'
        m=body['draft']['version']['uiContext']['loanModelProfile']
        v=body['draft']['version'];inputs=body['inputs'];r=body['draft']['rules'][0]
        if m=='CONTRACT_YEAR_START' and body==base[m]:return reply(m,day)
        if m=='FIRST_INSTALLMENT_OF_PERIOD' and body==base[m]:return reply(m,day)
        if m=='FIRST_INSTALLMENT_OF_PERIOD' and inputs.get('amount')=='200000000.00':
            original=reply(m,day);original['snapshotSha256']='a'*64
            with localcontext() as ctx:
                ctx.prec=34
                factor=Decimal(10)/Decimal(365)
                raw=Decimal('200000000')*Decimal(4)/Decimal(100)*factor
                original['calculation']['rawIrr']=str(raw)
                original['calculation']['roundedIrr']=str(raw.quantize(Decimal('1'),rounding=ROUND_HALF_UP))
                original['calculation']['yearFraction']=str(factor)
            return original
        if m=='ON_ANNIVERSARY':code='QARD_ANNIVERSARY_NOT_CAL2_VERIFIED'
        elif m=='JALALI_YEAR_START' and body==base[m]:code='QARD_NOT_JALALI_NEW_YEAR'
        elif body['draft'].get('calcItemId') is not None:code='PARTIAL_ORACLE_REFERENCE'
        elif m not in rt.SPEC:code='QARD_TIMING_MODEL_UNSUPPORTED'
        elif v['uiContext']['loanPolicy']['holidayPolicy']!='CHARGE_ALL_DAYS' or \
             v['uiContext']['loanPolicy']['yearStartBasis']!=rt.SPEC[m][1]:
            code='QARD_TIMING_POLICY_MISMATCH'
        elif v['taxMode']!='OUT_OF_SCOPE' or v['financialPolicy']:
            code='QARD_TAX_OR_FINANCIAL_POLICY_UNREVIEWED'
        elif r['rate'] is not None:code='QARD_RULE_UNSUPPORTED'
        elif r['formulaCode']!='TPL_E624746F262F6E':code='QARD_FORMULA_NOT_CERTIFIED'
        elif r['parameters']:code='CLIENT_FORMULA_PARAMETERS_FORBIDDEN'
        elif body['draft']['item']['operation']!='QARD_FEE':code='NOT_QARD_FEE'
        elif 'yearFraction' in inputs or 'alreadyCharged' in inputs or 'annualInterest' in inputs:
            code='QARD_UNREVIEWED_INPUT_FIELDS'
        elif inputs.get('contractEvidenceReference')=='' or \
             inputs.get('scheduleEvidenceReference')=='':
            code='QARD_CONTRACT_SCHEDULE_EVIDENCE_REQUIRED'
        elif inputs.get('firstInstallmentDate')!=day:code='FIRST_INSTALLMENT_SCHEDULE_MISMATCH'
        elif inputs.get('loanContractStartDate')!=day:code='QARD_PERIOD_OR_CONTRACT_DATES_INVALID'
        elif inputs.get('eventDate')!=day:code='QARD_EVENT_NOT_AT_BANK_BUSINESS_DATE'
        elif inputs.get('amount')=='10.001':code='QARD_AMOUNT_NUMBER22_2'
        elif inputs.get('contractRate')!='4.00000000':code='QARD_RATE_NUMBER18_8'
        else:code='MOCK_NOT_MATCHED'
        return reply(m,day,True,code)
    old=rt.http;rt.http=fake
    try:
        with contextlib.redirect_stdout(io.StringIO()):output=rt.run('http://127.0.0.1:8091')
    finally:rt.http=old
    return output,traffic

check('four independently coded timing paths',len(rt.SPEC)==4)
check('all endpoint calls strict allowlist',rt.GET=={
    '/readiness','/workflow/readiness','/references/operations','/references/formulas'} and
    rt.POST=={'/r4/qard/preview'})
check('first installment has explicit due-date input',
    'firstInstallmentDate' in rt.request('2026-10-27','FIRST_INSTALLMENT_OF_PERIOD')['inputs'])
check('other models cannot claim installment schedule date',all(
    'firstInstallmentDate' not in rt.request('2026-10-27',m)['inputs'] for m in
    ('ON_ANNIVERSARY','JALALI_YEAR_START','CONTRACT_YEAR_START')))
result,traffic=simulate()
print(f"FEE3_R4C3_MOCK_RUNTIME_PASS={result['passed']} FAIL={result['failed']} (HTTP mock; NOT Oracle)")
check('mocked transport passes all positives and negatives',result['failed']==0 and result['passed']>=25)
check('mocked GET/POST only known endpoints',all((method=='GET' and path in rt.GET and b is None)
    or (method=='POST' and path in rt.POST and b is not None) for method,path,b in traffic))
check('HTTP tests actually send blocked and positive cases',sum(m=='POST' for m,_,_ in traffic)>=20)
check('no collection/write/workflow endpoint',not any(
    '/collect' in path or '/save' in path or '/submit' in path or '/posting' in path or
    '/approve' in path for _,path,_ in traffic))
for name,kw in [('writes enabled',{'writes':True}),('workflow enabled',{'workflow':True}),
                ('formula inactive',{'noformula':True})]:
    result,traffic=simulate(**kw)
    check(name+' fails before any preview POST',result['failed']>0 and
        all(method!='POST' for method,_,_ in traffic))
check('financial authorization flag always denied',not rt.safe(dict(reply(
    'FIRST_INSTALLMENT_OF_PERIOD','2026-10-27'),approved=True)))
check('blocked response has no amount or SHA',rt.blocked(reply(
    'FIRST_INSTALLMENT_OF_PERIOD','2026-10-27',True)))
check('missing duplicate check proof cannot simulate',not rt.simulated({**reply(
    'FIRST_INSTALLMENT_OF_PERIOD','2026-10-27'),
    'diagnostics':['NO_ORACLE_WRITE']},'FIRST_INSTALLMENT_OF_PERIOD'))
check('rounded amount mismatch rejected',not rt.simulated(reply(
    'FIRST_INSTALLMENT_OF_PERIOD','2026-10-27'),'FIRST_INSTALLMENT_OF_PERIOD','900000000'))
for name,method,path,body in [('POST approval','POST','/workflow/approve',{}),
    ('POST collection','POST','/charges/collect',{}),('GET secret','GET','/private-secrets',None)]:
    try:rt.http('http://127.0.0.1:8091',method,path,body)
    except RuntimeError:check(name+' denied by allowlist',True)
    else:check(name+' denied by allowlist',False)
try:rt.http('http://example.com:8091','GET','/readiness')
except RuntimeError:check('non-loopback destination denied',True)
else:check('non-loopback destination denied',False)
print(f'FEE3_R4C3_SELFTEST_PASS={YES} FAIL={NO}')
sys.exit(0 if NO==0 else 2)
