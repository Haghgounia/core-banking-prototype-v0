#!/usr/bin/env python3
"""R4-C1 loan PROFIT proof: direct loopback, GET and no-write preview POST only.
No approval, posting, mutation, or arbitrary formula endpoints are allowed.
"""
from __future__ import annotations
import argparse,copy,json,re,sys
from datetime import date,datetime,timezone,timedelta
from decimal import Decimal,ROUND_HALF_UP,localcontext
from pathlib import Path
from urllib.parse import urlsplit
from urllib.request import ProxyHandler,HTTPRedirectHandler,build_opener,Request
from urllib.error import HTTPError,URLError

ROOT=Path(__file__).resolve().parents[3]
REPORT=ROOT/'docs/install/FEE3-R4C1-RUNTIME-RESULT.json'
API='/api/v1/fee3'
GET={'/readiness','/workflow/readiness','/references/operations','/references/formulas'}
POST={'/r4/loans/profit/preview'}
class NoRedirect(HTTPRedirectHandler):
    def redirect_request(self,*args,**kwargs):raise RuntimeError('REDIRECT_FORBIDDEN')
CLIENT=build_opener(ProxyHandler({}),NoRedirect())
class Checks:
    def __init__(self):self.rows=[]
    def add(self,name,ok,details=''):
        ok=bool(ok);self.rows.append({'name':name,'result':'PASS' if ok else 'FAIL','details':str(details)[:180]})
        print(('PASS' if ok else 'FAIL')+' | '+name+((' | '+str(details)[:100]) if details else ''),flush=True)
        return ok
    def outcome(self):
        pass_count=sum(r['result']=='PASS' for r in self.rows)
        return {'protocol':'FEE3_R4C1_LOAN_PROFIT_READONLY_V1','atUtc':datetime.now(timezone.utc).isoformat(),
            'noWrites':True,'posting':False,'passed':pass_count,'failed':len(self.rows)-pass_count,
            'checks':self.rows}
def http(base,method,path,body=None):
    if method=='GET' and path not in GET or method=='POST' and path not in POST or method not in ('GET','POST'):
        raise RuntimeError('ROUTE_NOT_ALLOWLISTED')
    if method=='GET' and body is not None:raise RuntimeError('GET_BODY_FORBIDDEN')
    if method=='POST' and body is None:raise RuntimeError('POST_REQUIRES_BODY')
    payload=json.dumps(body,ensure_ascii=True,separators=(',',':')).encode() if body is not None else None
    request=Request(base+API+path,method=method,data=payload,
        headers={'Content-Type':'application/json','Accept':'application/json'})
    print('HTTP_STEP_START '+method+' '+path,flush=True)
    try:
        with CLIENT.open(request,timeout=18) as response:
            if response.status!=200:raise RuntimeError('UNEXPECTED_HTTP_'+str(response.status))
            if 'json' not in response.headers.get('Content-Type','').lower():raise RuntimeError('NON_JSON_RESPONSE')
            content=response.read(2_000_001)
            if len(content)>2_000_000:raise RuntimeError('OVERSIZED_RESPONSE')
            obj=json.loads(content.decode())
            print('HTTP_STEP_PASS '+method+' '+path+' HTTP=200',flush=True)
            return obj
    except (HTTPError,URLError,RuntimeError,OSError,ValueError,TimeoutError) as err:
        msg=type(err).__name__+' '+str(getattr(err,'reason',err))[:180]
        if isinstance(err,HTTPError):
            try:
                j=json.loads(err.read(4000))
                msg+=' HTTP_'+str(err.code)+' '+str(j.get('errorCode',''))[:60]
            except (OSError,ValueError,TypeError):msg+=' HTTP_'+str(err.code)
        print('HTTP_STEP_FAIL '+method+' '+path+' | '+msg,flush=True)
        raise RuntimeError(method+' '+path+' | '+msg) from err

def request(day,basis):
    months=basis=='CONTRACT_MONTHS'
    contract='INSTALLMENT_SALE' if months else 'MURABAHA'
    formula='TPL_7F2FE580EDB351' if months else 'TPL_E624746F262F6E'
    data={'amount':'100000000.00','contractRate':'18.00000000',
        'contractReference':'UAT-LOAN-CLIENT-UNVERIFIED',
        'rateEvidenceReference':'UAT-CONTRACT-RATE-UNVERIFIED'}
    if months:data['contractMonths']=12
    else:
        d=date.fromisoformat(day);data.update({'periodStart':day,'periodEndExclusive':(d+timedelta(days=10)).isoformat()})
    draft={
        'calcItemId':None,'tariffVersionId':None,'expectedItemRecordVersion':None,
        'expectedVersionRecordVersion':None,
        'item':{'title':'R4-C1 UAT EXCHANGE LOAN PROFIT','module':'LOAN','operation':'LOAN_PROFIT',
            'amountKind':'FINANCING_PROFIT','variant':None,'ownerDomain':'LOAN','description':None},
        'version':{'validFrom':day,'validTo':None,'origin':'CONTRACT_RATE_REGULATED',
            'sourceId':None,'method':'FORMULA','taxMode':'OUT_OF_SCOPE','rounding':'HALF_UP',
            'changeReason':'no-write standalone UAT only','financialPolicy':{},
            'serviceCurrency':'IRR','feeCurrency':'IRR',
            'uiContext':{'loanModelProfile':'EXCHANGE_ACCRUAL','loanCalculationEvent':'PERIOD_PROFIT',
                'loanContractTypes':[contract],'loanPolicy':{'dayCountBasis':basis,
                    'holidayPolicy':'CHARGE_ALL_DAYS','contractEvidenceRequired':True}}},
        'scope':{'channels':['ALL'],'customers':['ALL'],'groups':['ALL'],'products':['ALL'],
            'contracts':[contract],'currency':'IRR'},
        'rules':[{'order':1,'method':'FORMULA','base':'REQUEST_AMOUNT','rateSource':'REQUEST',
            'rate':None,'fixed':None,'collateral':None,'coverage':None,'min':None,'max':None,
            'formulaCode':formula,'parameters':{}}],
        'tests':[{'title':'UAT independent arithmetic reference','request':{'amount':'100000000'},'expected':None}]
    }
    return {'draft':draft,'inputs':data,'effectiveOn':None}

def safe(obj):
    if not isinstance(obj,dict):return False
    if any(obj.get(key) is not False for key in ('persisted','financialPosting','approved','published')):return False
    calc=obj.get('calculation')
    return calc is None or isinstance(calc,dict) and all(calc.get(k) is False for k in ('persisted','financialPosting'))

def simulated(obj,fee):
    if not safe(obj) or obj.get('status')!='SIMULATED':return False
    calc=obj.get('calculation')
    if not isinstance(calc,dict) or calc.get('status')!='SIMULATED':return False
    try:
        return Decimal(str(calc.get('feeIrr')))==Decimal(str(fee)) and Decimal(str(calc.get('taxIrr')))==0
    except (ValueError,TypeError):return False

def blocked(obj):
    return safe(obj) and obj.get('status')=='BLOCKED' and obj.get('calculation') is None and \
        obj.get('snapshotSha256') is None and bool(obj.get('diagnostics'))

def run(base):
    c=Checks()
    try:
        ready=http(base,'GET','/readiness')
        if not isinstance(ready,dict):raise RuntimeError('READINESS_NOT_OBJECT')
        c.add('FEE3 Oracle read and 11 tables',ready.get('schema')=='FEE3' and
            ready.get('readsEnabled') is True and ready.get('missingTables')==[])
        c.add('draft writes remain disabled',ready.get('draftWritesEnabled') is False)
        counts={x:ready.get(x) for x in ('calcItemCount','tariffVersionCount')}
        c.add('Oracle baseline present',counts=={'calcItemCount':297,'tariffVersionCount':300},counts)
        day=ready.get('oracleBusinessDate')
        try:date.fromisoformat(day)
        except (TypeError,ValueError):raise RuntimeError('ORACLE_BANK_DATE_MISSING')
        wf=http(base,'GET','/workflow/readiness')
        switches=('workflowEnabled','checkerDecisionEnabled','approvalEnabled','postingEnabled',
                  'authenticated','makerRole','checkerRole')
        all_denied=isinstance(wf,dict) and all(wf.get(k) is False for k in switches)
        c.add('workflow, authentication, and posting denied',all_denied)
        if ready.get('draftWritesEnabled') is not False or not all_denied or \
           not isinstance(counts['calcItemCount'],int) or counts['calcItemCount']<=0 or \
           not isinstance(counts['tariffVersionCount'],int) or counts['tariffVersionCount']<=0 or \
           ready.get('readsEnabled') is not True or ready.get('missingTables')!=[]:
            raise RuntimeError('FAIL_CLOSED_BEFORE_ANY_PREVIEW_POST')
        operations=http(base,'GET','/references/operations')
        op=next((x for x in operations if isinstance(x,dict) and x.get('module')=='LOAN'
                 and x.get('code')=='LOAN_PROFIT' and x.get('active') is True and
                 'FINANCING_PROFIT' in x.get('allowedAmountKinds',[])),None)
        c.add('authoritative active loan profit operation',op is not None)
        formulas=http(base,'GET','/references/formulas')
        formula_codes={'TPL_E624746F262F6E','TPL_7F2FE580EDB351'}
        found={x['code'] for x in formulas if isinstance(x,dict) and
            x.get('code') in formula_codes and x.get('active') is True and x.get('method')=='FORMULA'}
        c.add('two exact loan profit formula references active',found==formula_codes)
        if op is None or found!=formula_codes:raise RuntimeError('REFERENCE_PROOF_REQUIRED_BEFORE_POST')
        monthly=request(day,'CONTRACT_MONTHS');daily=request(day,'JALALI_ACT_ACT')
        good=http(base,'POST','/r4/loans/profit/preview',monthly)
        c.add('contract-months preview 100M 18pct 12mo = 18M',simulated(good,'18000000'),
              good.get('diagnostics',[]) if isinstance(good,dict) else good)
        c.add('snapshot and unverified-client evidence flags',safe(good) and
            good.get('simulationMode')=='UNVERIFIED_UAT_DRAFT' and
            good.get('versionReference',{}).get('status')=='UNSAVED' and
            'CLIENT_CONTRACT_RATE_UNVERIFIED' in good.get('diagnostics',[]) and
            isinstance(good.get('snapshotSha256'),str) and
            bool(re.fullmatch('[0-9a-f]{64}',good['snapshotSha256'])))
        again=http(base,'POST','/r4/loans/profit/preview',monthly)
        c.add('deterministic replay exact SHA256',good.get('snapshotSha256')==again.get('snapshotSha256'))
        daily_result=http(base,'POST','/r4/loans/profit/preview',daily)
        if daily_result.get('status')=='BLOCKED':
            c.add('CAL2 Jalali coverage must be proven (not yet)',False,daily_result.get('diagnostics',[]))
        else:
            calc=daily_result.get('calculation',{});breakdown=calc.get('breakdown',[])
            with localcontext() as ctx:
                ctx.prec=34
                fraction=sum((Decimal(str(row['days']))/Decimal(str(row['yearDenominator']))
                    for row in breakdown),Decimal(0))
                expected=(Decimal('100000000')*Decimal('18')/Decimal('100')*fraction).quantize(
                    Decimal('1'),rounding=ROUND_HALF_UP)
            c.add('CAL2-backed 10 Jalali days exact total',simulated(daily_result,expected) and
                  sum(int(row['days']) for row in breakdown)==10 and all(
                  row['yearDenominator'] in (365,366) and str(row['calendarDatasetEvidence']).startswith('CAL2:PERSIAN:')
                  for row in breakdown),daily_result.get('diagnostics',[]))
        def invalid(name,source,edit):
            data=copy.deepcopy(source);edit(data)
            response=http(base,'POST','/r4/loans/profit/preview',data)
            c.add(name,blocked(response),response.get('diagnostics',[]) if isinstance(response,dict) else response)
        invalid('client provided yearFraction denied',daily,
            lambda x:x['inputs'].update({'yearFraction':1}))
        invalid('missing client contract evidence denied',monthly,
            lambda x:x['inputs'].pop('contractReference'))
        invalid('unapproved profit rate 9 fractional digits denied',monthly,
            lambda x:x['inputs'].update({'contractRate':'1.123456789'}))
        invalid('loan contract months cannot be fractional',monthly,
            lambda x:x['inputs'].update({'contractMonths':'1.5'}))
        invalid('editable expression denied',monthly,
            lambda x:x['draft']['rules'][0]['parameters'].update({'_fceEditableExpressionV1':'amount * 10'}))
        invalid('late damage formula not repurposed',monthly,
            lambda x:x['draft']['rules'][0].update({'formulaCode':'LOAN_LATE_HOLIDAY'}))
        invalid('participation event not exchange accrual',monthly,
            lambda x:x['draft']['version']['uiContext'].update({'loanModelProfile':'PARTICIPATION_REALIZED'}))
        invalid('tax unreviewed cannot be charged',monthly,
            lambda x:x['draft']['version'].update({'taxMode':'UNREVIEWED'}))
        invalid('partial Oracle reference cannot spoof persisted draft',monthly,
            lambda x:x['draft'].update({'calcItemId':824}))
        invalid('caller provided fixed tariff rate denied',monthly,
            lambda x:x['draft']['rules'][0].update({'rate':18}))
        after=http(base,'GET','/readiness')
        after_counts={x:after.get(x) for x in counts}
        c.add('Oracle item/version counts unchanged',counts==after_counts,after_counts)
    except Exception as ex:
        c.add('qualification runtime',False,type(ex).__name__+' '+str(ex)[:180])
    return c.outcome()

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--base-url',default='http://127.0.0.1:8091')
    parser.add_argument('--report',default=str(REPORT));args=parser.parse_args()
    obj=urlsplit(args.base_url)
    if obj.scheme!='http' or obj.hostname not in ('127.0.0.1','localhost') or obj.username or \
       obj.password or obj.query or obj.fragment or obj.path not in ('','/') or obj.port!=8091:
        print('FEE3_R4C1_UNSAFE_DESTINATION_REJECTED');return 3
    result=run('http://127.0.0.1:8091');out=Path(args.report)
    out.parent.mkdir(parents=True,exist_ok=True)
    out.write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print(f'FEE3_R4C1_RUNTIME_PASS={result["passed"]} FAIL={result["failed"]}')
    print('REPORT='+str(out))
    return 0 if result['failed']==0 else 2
if __name__=='__main__':sys.exit(main())
