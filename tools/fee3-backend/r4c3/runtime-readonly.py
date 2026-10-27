#!/usr/bin/env python3
"""Only localhost nonmutating Qard preview: refuses all workflow/write/collection endpoints."""
from __future__ import annotations
import argparse,copy,json,sys
from datetime import date,datetime,timedelta,timezone
from decimal import Decimal,ROUND_HALF_UP,localcontext
from pathlib import Path
from urllib.parse import urlsplit
from urllib.request import ProxyHandler,HTTPRedirectHandler,build_opener,Request
from urllib.error import HTTPError,URLError

ROOT=Path(__file__).resolve().parents[3]
REPORT=ROOT/'docs/install/FEE3-R4C3-RUNTIME-RESULT.json'
API='/api/v1/fee3'
GET={'/readiness','/workflow/readiness','/references/operations','/references/formulas'}
POST={'/r4/qard/preview'}
SPEC={
 'ON_ANNIVERSARY':('ON_ANNIVERSARY','UNDECIDED'),
 'FIRST_INSTALLMENT_OF_PERIOD':('FIRST_INSTALLMENT_OF_PERIOD','UNDECIDED'),
 'JALALI_YEAR_START':('START_OF_YEAR','JALALI_CALENDAR_YEAR'),
 'CONTRACT_YEAR_START':('START_OF_YEAR','LOAN_CONTRACT_YEAR')
}
class NoRedirect(HTTPRedirectHandler):
    def redirect_request(self,*args,**kwargs):raise RuntimeError('REDIRECT_FORBIDDEN')
CLIENT=build_opener(ProxyHandler({}),NoRedirect())
class Checks:
    def __init__(self):self.rows=[]
    def add(self,name,ok,detail=''):
        okay=bool(ok);self.rows.append({'name':name,'result':'PASS' if okay else 'FAIL',
            'detail':str(detail)[:230]})
        print(('PASS' if okay else 'FAIL')+' | '+name+(' | '+str(detail)[:110] if detail else ''),flush=True)
    def report(self):
        passed=sum(r['result']=='PASS' for r in self.rows)
        return {'protocol':'FEE3_R4C3_QARD_READONLY_V1','atUtc':datetime.now(timezone.utc).isoformat(),
            'noMutationAPIs':True,'passed':passed,'failed':len(self.rows)-passed,'checks':self.rows}

def http(base,method,path,body=None):
    parsed=urlsplit(base)
    if parsed.scheme!='http' or parsed.hostname!='127.0.0.1' or parsed.port!=8091 or \
       parsed.path or parsed.query or parsed.fragment or parsed.username or parsed.password:
        raise RuntimeError('LOOPBACK_BASE_ONLY')
    if method=='GET':
        if path not in GET or body is not None:raise RuntimeError('GET_NOT_ALLOWLISTED')
    elif method=='POST':
        if path not in POST or body is None:raise RuntimeError('POST_NOT_ALLOWLISTED')
    else:raise RuntimeError('METHOD_NOT_ALLOWED')
    payload=None if body is None else json.dumps(body,separators=(',',':'),ensure_ascii=True).encode('utf8')
    req=Request(base+API+path,method=method,data=payload,
                headers={'Accept':'application/json','Content-Type':'application/json'})
    print('HTTP_STEP_START '+method+' '+path,flush=True)
    try:
        with CLIENT.open(req,timeout=20) as response:
            if response.status!=200:raise RuntimeError('HTTP_NOT_200_'+str(response.status))
            if 'json' not in response.headers.get('Content-Type','').lower():
                raise RuntimeError('NON_JSON_RESPONSE')
            raw=response.read(2_000_001)
            if len(raw)>2_000_000:raise RuntimeError('OVERSIZED_RESPONSE')
            result=json.loads(raw.decode('utf8'),parse_float=Decimal)
            print('HTTP_STEP_PASS '+method+' '+path+' HTTP=200',flush=True)
            return result
    except (HTTPError,URLError,RuntimeError,OSError,ValueError,TimeoutError) as ex:
        message=type(ex).__name__+' '+str(getattr(ex,'reason',ex))[:150]
        print('HTTP_STEP_FAIL '+method+' '+path+' | '+message,flush=True)
        raise RuntimeError(message) from ex

def request(day,model,origin=None):
    timing,year=SPEC[model]
    origin=origin or day
    data={'contractReference':'UAT-LOAN-CONTRACT-UNVERIFIED',
          'contractEvidenceReference':'UAT-CONTRACT-UNVERIFIED',
          'rateEvidenceReference':'UAT-RATE-UNVERIFIED',
          'scheduleEvidenceReference':'UAT-SCHEDULE-UNVERIFIED',
          'loanContractStartDate':origin,
          'periodStart':day,'periodEndExclusive':(date.fromisoformat(day)+timedelta(days=10)).isoformat(),
          'eventDate':day,'amount':'100000000.00','contractRate':'4.00000000'}
    if model=='FIRST_INSTALLMENT_OF_PERIOD':data['firstInstallmentDate']=day
    draft={'calcItemId':None,'tariffVersionId':None,
      'expectedItemRecordVersion':None,'expectedVersionRecordVersion':None,
      'item':{'title':'R4-C3 UAT QARD FEE '+model,'module':'LOAN','operation':'QARD_FEE',
              'amountKind':'QARD_FEE','variant':None,'ownerDomain':'CREDIT','description':None},
      'version':{'validFrom':day,'validTo':None,'origin':'BANK_INTERNAL','sourceId':None,
                 'method':'FORMULA','taxMode':'OUT_OF_SCOPE','rounding':'HALF_UP',
                 'changeReason':'unverified Qard draft only','financialPolicy':{},
                 'serviceCurrency':'IRR','feeCurrency':'IRR',
                 'uiContext':{'loanModelProfile':model,'loanContractTypes':['QARD_HASANA'],
                   'loanPolicy':{'contractEvidenceRequired':True,'contractInputsRequired':True,
                     'dayCountBasis':'JALALI_ACT_ACT','holidayPolicy':'CHARGE_ALL_DAYS',
                     'qardChargeTiming':timing,'yearStartBasis':year}}},
      'scope':{'channels':['ALL'],'customers':['ALL'],'groups':['ALL'],'products':['ALL'],
               'contracts':['QARD_HASANA'],'currency':'IRR'},
      'rules':[{'order':1,'method':'FORMULA','base':'REQUEST_AMOUNT','rateSource':'REQUEST',
        'rate':None,'fixed':None,'collateral':None,'coverage':None,'min':None,'max':None,
        'formulaCode':'TPL_E624746F262F6E','parameters':{}}],
      'tests':[{'title':'Unverified UAT only','request':{'amount':'100000000'},'expected':None}]}
    return {'draft':draft,'inputs':data,'effectiveOn':None}

def safe(reply):
    return isinstance(reply,dict) and all(reply.get(k) is False for k in
        ('approved','published','persisted','financialPosting'))

def simulated(reply,model,amount='100000000',rate='4'):
    if not safe(reply) or reply.get('status')!='SIMULATED' or \
       reply.get('timingModel')!=model or reply.get('simulationMode')!='UNVERIFIED_UAT_DRAFT':return False
    calc=reply.get('calculation')
    if not isinstance(calc,dict) or calc.get('status')!='SIMULATED' or \
       calc.get('timingModel')!=model or calc.get('resultKind')!='QARD_SERVICE_FEE_NOT_DEMANDED' or \
       calc.get('duplicateCheckStatus')!='NOT_VERIFIED_NO_AUTHORITATIVE_CHARGE_HISTORY' or \
       not isinstance(calc.get('breakdown'),list) or not calc.get('breakdown') or \
       not isinstance(calc.get('candidateAssessmentKey'),str) or \
       len(calc['candidateAssessmentKey'])!=64 or \
       not isinstance(reply.get('snapshotSha256'),str) or len(reply['snapshotSha256'])!=64:return False
    if not isinstance(reply.get('versionReference'),dict) or \
       reply['versionReference'].get('status')!='UNSAVED':return False
    if not {'PREVIOUS_CHARGE_HISTORY_NOT_CHECKED','NO_DUPLICATE_CERTIFICATION',
            'NOT_A_CHARGE_AUTHORIZATION','NO_ORACLE_WRITE','NO_ACCOUNTING_POSTING'} \
            .issubset(set(reply.get('diagnostics') or [])):return False
    if not str(calc.get('eventCalendarDatasetEvidence','')).startswith('CAL2:PERSIAN:') or \
       not 1200<=calc.get('eventJalaliYear',0)<=1700 or \
       not 1<=calc.get('eventJalaliMonth',0)<=12 or \
       not 1<=calc.get('eventJalaliDay',0)<=31:return False
    try:
        rows=calc['breakdown']
        if sum(int(x['days']) for x in rows)!=10 or any(
            x['yearDenominator'] not in (365,366) or
            not str(x['calendarDatasetEvidence']).startswith('CAL2:PERSIAN:') for x in rows):
            return False
        with localcontext() as context:
            context.prec=34
            frac=sum((Decimal(str(x['days']))/Decimal(str(x['yearDenominator']))
                      for x in rows),Decimal(0))
            expected=(Decimal(amount)*Decimal(rate)/Decimal(100)*frac).quantize(
                Decimal(1),rounding=ROUND_HALF_UP)
            return Decimal(str(calc['roundedIrr']))==expected and \
                Decimal(str(calc['rawIrr']))>=0 and \
                Decimal(str(calc['yearFraction']))==frac
    except (TypeError,ValueError,KeyError,ArithmeticError):return False

def blocked(reply,code=None):
    return safe(reply) and reply.get('status')=='BLOCKED' and \
        reply.get('calculation') is None and reply.get('snapshotSha256') is None and \
        isinstance(reply.get('diagnostics'),list) and bool(reply['diagnostics']) and \
        (code is None or code in reply['diagnostics'])

def run(base):
    checks=Checks()
    try:
        ready=http(base,'GET','/readiness')
        if not isinstance(ready,dict):raise RuntimeError('READINESS_INVALID')
        checks.add('Oracle FEE3 reads and 11 required tables',ready.get('schema')=='FEE3' and
            ready.get('readsEnabled') is True and ready.get('missingTables')==[])
        checks.add('draft writes remain disabled',ready.get('draftWritesEnabled') is False)
        counts={k:ready.get(k) for k in ('calcItemCount','tariffVersionCount')}
        checks.add('Oracle item/version baseline 297/300',counts=={
            'calcItemCount':297,'tariffVersionCount':300},counts)
        bankDay=ready.get('oracleBusinessDate');date.fromisoformat(bankDay)
        work=http(base,'GET','/workflow/readiness')
        switches=('workflowEnabled','checkerDecisionEnabled','approvalEnabled','postingEnabled',
                  'authenticated','makerRole','checkerRole')
        blocked_work=isinstance(work,dict) and all(work.get(k) is False for k in switches)
        checks.add('workflow, approval and posting disabled',blocked_work)
        if not all(x['result']=='PASS' for x in checks.rows):
            raise RuntimeError('FAIL_CLOSED_PREVIEW_NOT_SENT')
        ops=http(base,'GET','/references/operations')
        valid_op=isinstance(ops,list) and any(isinstance(o,dict) and o.get('module')=='LOAN' and
            o.get('code')=='QARD_FEE' and o.get('active') is True and
            'QARD_FEE' in (o.get('allowedAmountKinds') or []) for o in ops)
        checks.add('Oracle QARD_FEE operation active',valid_op)
        formulas=http(base,'GET','/references/formulas')
        valid_formula=isinstance(formulas,list) and any(isinstance(f,dict) and
            f.get('code')=='TPL_E624746F262F6E' and f.get('method')=='FORMULA' and
            f.get('active') is True for f in formulas)
        checks.add('Oracle exact calendar formula reference active',valid_formula)
        if not(valid_op and valid_formula):raise RuntimeError('QARD_ORACLE_VOCABULARY_NOT_ACTIVE')
        first=request(bankDay,'FIRST_INSTALLMENT_OF_PERIOD')
        actual=http(base,'POST','/r4/qard/preview',first)
        checks.add('first installment 10-day fee amount from CAL2',simulated(actual,
            'FIRST_INSTALLMENT_OF_PERIOD'))
        checks.add('client schedule, rate and duplicate history not certified',
            'LOAN_SCHEDULE_UNVERIFIED' in (actual.get('diagnostics') or []) and
            'PREVIOUS_CHARGE_HISTORY_NOT_CHECKED' in (actual.get('diagnostics') or []) and
            actual.get('approved') is False)
        replay=http(base,'POST','/r4/qard/preview',first)
        checks.add('deterministic SHA256 and candidate key replay',simulated(replay,
            'FIRST_INSTALLMENT_OF_PERIOD') and replay.get('snapshotSha256')==actual.get('snapshotSha256') and
            replay.get('calculation',{}).get('candidateAssessmentKey')==
            actual.get('calculation',{}).get('candidateAssessmentKey'))
        amended=copy.deepcopy(first);amended['inputs']['amount']='200000000.00'
        amendedResult=http(base,'POST','/r4/qard/preview',amended)
        checks.add('amended amount changes arithmetic but not cycle identity',
            simulated(amendedResult,'FIRST_INSTALLMENT_OF_PERIOD','200000000') and
            amendedResult.get('snapshotSha256')!=actual.get('snapshotSha256') and
            amendedResult.get('calculation',{}).get('candidateAssessmentKey')==
            actual.get('calculation',{}).get('candidateAssessmentKey'))
        contractYear=request(bankDay,'CONTRACT_YEAR_START')
        annual=http(base,'POST','/r4/qard/preview',contractYear)
        checks.add('contract-year start uses contract date, not solar new year',
            simulated(annual,'CONTRACT_YEAR_START') and
            annual.get('calculation',{}).get('candidateAssessmentKey')!=
            actual.get('calculation',{}).get('candidateAssessmentKey'))
        # Same-day origination is not an anniversary of a previous Qard contractual year.
        anniv=http(base,'POST','/r4/qard/preview',request(bankDay,'ON_ANNIVERSARY'))
        checks.add('contract origination must not be charged as anniversary',
            blocked(anniv,'QARD_ANNIVERSARY_NOT_CAL2_VERIFIED'))
        year=http(base,'POST','/r4/qard/preview',request(bankDay,'JALALI_YEAR_START'))
        if date.fromisoformat(bankDay).month not in (3,4):
            checks.add('ordinary business day is not Jalali year start',
                blocked(year,'QARD_NOT_JALALI_NEW_YEAR'))
        else:
            checks.add('Jalali new year event requires CAL2 day ordinal 1',
                (blocked(year,'QARD_NOT_JALALI_NEW_YEAR') or
                 simulated(year,'JALALI_YEAR_START') and
                 year.get('calculation',{}).get('eventJalaliOrdinal')==1))
        def negative(name,model,change,expected):
            body=request(bankDay,model);change(body)
            response=http(base,'POST','/r4/qard/preview',body)
            checks.add(name,blocked(response,expected),response.get('diagnostics'))
        negative('client yearFraction denied','FIRST_INSTALLMENT_OF_PERIOD',
            lambda x:x['inputs'].update(yearFraction='1.0'),'QARD_UNREVIEWED_INPUT_FIELDS')
        negative('no asserted duplicate certificate allowed','FIRST_INSTALLMENT_OF_PERIOD',
            lambda x:x['inputs'].update(alreadyCharged=False),'QARD_UNREVIEWED_INPUT_FIELDS')
        negative('precision beyond NUMBER22_2 rejected','FIRST_INSTALLMENT_OF_PERIOD',
            lambda x:x['inputs'].update(amount='10.001'),'QARD_AMOUNT_NUMBER22_2')
        negative('precision beyond NUMBER18_8 rejected','FIRST_INSTALLMENT_OF_PERIOD',
            lambda x:x['inputs'].update(contractRate='4.123456789'),'QARD_RATE_NUMBER18_8')
        negative('negative contract rate rejected','FIRST_INSTALLMENT_OF_PERIOD',
            lambda x:x['inputs'].update(contractRate='-4'),'QARD_RATE_NUMBER18_8')
        negative('missing contract evidence rejected','FIRST_INSTALLMENT_OF_PERIOD',
            lambda x:x['inputs'].update(contractEvidenceReference=''),
            'QARD_CONTRACT_SCHEDULE_EVIDENCE_REQUIRED')
        negative('missing schedule evidence rejected','FIRST_INSTALLMENT_OF_PERIOD',
            lambda x:x['inputs'].update(scheduleEvidenceReference=''),
            'QARD_CONTRACT_SCHEDULE_EVIDENCE_REQUIRED')
        negative('wrong first installment due date rejected','FIRST_INSTALLMENT_OF_PERIOD',
            lambda x:x['inputs'].update(firstInstallmentDate=(date.fromisoformat(bankDay)+
                timedelta(days=1)).isoformat()),'FIRST_INSTALLMENT_SCHEDULE_MISMATCH')
        negative('first installment cannot add annual interest','FIRST_INSTALLMENT_OF_PERIOD',
            lambda x:x['inputs'].update(annualInterest='18'),
            'QARD_UNREVIEWED_INPUT_FIELDS')
        negative('year start basis mismatch denied','JALALI_YEAR_START',
            lambda x:x['draft']['version']['uiContext']['loanPolicy'].update(
                yearStartBasis='LOAN_CONTRACT_YEAR'),'QARD_TIMING_POLICY_MISMATCH')
        negative('unsupported holiday rule denied','FIRST_INSTALLMENT_OF_PERIOD',
            lambda x:x['draft']['version']['uiContext']['loanPolicy'].update(
                holidayPolicy='ROLL_DUE_TO_WORKDAY'),'QARD_TIMING_POLICY_MISMATCH')
        negative('editable formula injection denied','FIRST_INSTALLMENT_OF_PERIOD',
            lambda x:x['draft']['rules'][0]['parameters'].update(
                _fceEditableExpressionV1='amount+100'),
            'CLIENT_FORMULA_PARAMETERS_FORBIDDEN')
        negative('loan profit formula cannot be substituted','FIRST_INSTALLMENT_OF_PERIOD',
            lambda x:x['draft']['rules'][0].update(formulaCode='PARTICIPATION_REALIZED'),
            'QARD_FORMULA_NOT_CERTIFIED')
        negative('tax/waiver financial override forbidden','FIRST_INSTALLMENT_OF_PERIOD',
            lambda x:x['draft']['version'].update(financialPolicy={'taxPercent':10}),
            'QARD_TAX_OR_FINANCIAL_POLICY_UNREVIEWED')
        negative('tax policy unreviewed rejected','FIRST_INSTALLMENT_OF_PERIOD',
            lambda x:x['draft']['version'].update(taxMode='UNREVIEWED'),
            'QARD_TAX_OR_FINANCIAL_POLICY_UNREVIEWED')
        negative('partial Oracle ID cannot impersonate approved tariff','FIRST_INSTALLMENT_OF_PERIOD',
            lambda x:x['draft'].update(calcItemId=235),
            'PARTIAL_ORACLE_REFERENCE')
        negative('fixed tariff rate cannot replace contract rate','FIRST_INSTALLMENT_OF_PERIOD',
            lambda x:x['draft']['rules'][0].update(rate=Decimal('4').to_eng_string()),
            'QARD_RULE_UNSUPPORTED')
        negative('not a loan profit operation','FIRST_INSTALLMENT_OF_PERIOD',
            lambda x:x['draft']['item'].update(operation='LOAN_PROFIT'),
            'NOT_QARD_FEE')
        negative('future bank event denied','FIRST_INSTALLMENT_OF_PERIOD',
            lambda x:x['inputs'].update(eventDate=(date.fromisoformat(bankDay)+
                timedelta(days=1)).isoformat()),'QARD_EVENT_NOT_AT_BANK_BUSINESS_DATE')
        negative('contract start cannot follow period','FIRST_INSTALLMENT_OF_PERIOD',
            lambda x:x['inputs'].update(loanContractStartDate=(date.fromisoformat(bankDay)+
                timedelta(days=1)).isoformat()),'QARD_PERIOD_OR_CONTRACT_DATES_INVALID')
        negative('wrong model code rejected','FIRST_INSTALLMENT_OF_PERIOD',
            lambda x:x['draft']['version']['uiContext'].update(
                loanModelProfile='PARTICIPATION_REALIZED'),'QARD_TIMING_MODEL_UNSUPPORTED')
        after=http(base,'GET','/readiness')
        checks.add('Oracle counts unchanged after preview',all(after.get(k)==ready.get(k)
            for k in ('calcItemCount','tariffVersionCount')) and
            after.get('draftWritesEnabled') is False)
    except Exception as ex:
        checks.add('fail-closed runtime diagnostic',False,
            type(ex).__name__+' '+str(ex)[:160])
    return checks.report()

def main():
    ap=argparse.ArgumentParser();ap.add_argument('--base',default='http://127.0.0.1:8091')
    args=ap.parse_args()
    result=run(args.base)
    REPORT.parent.mkdir(parents=True,exist_ok=True)
    REPORT.write_text(json.dumps(result,indent=2,ensure_ascii=False)+'\n',encoding='utf8')
    print(f"FEE3_R4C3_RUNTIME_PASS={result['passed']} FAIL={result['failed']}")
    print('REPORT='+str(REPORT));return 0 if result['failed']==0 else 2
if __name__=='__main__':sys.exit(main())
