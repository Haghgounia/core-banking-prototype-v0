#!/usr/bin/env python3
"""R4-B1 safe UAT. Loopback direct HTTP, GET and preview-only POST, no DB writes."""
from __future__ import annotations
import argparse,copy,json,re,sys
from datetime import date,datetime,timedelta,timezone
from pathlib import Path
from urllib.parse import urlsplit
from urllib.error import HTTPError,URLError
from urllib.request import Request,build_opener,ProxyHandler,HTTPRedirectHandler

ROOT=Path(__file__).resolve().parents[3]
REPORT=ROOT/'docs/install/FEE3-R4B1-RUNTIME-RESULT.json'
API='/api/v1/fee3'
GET={'/readiness','/workflow/readiness','/references/operations'}
POST={'/r4/services/preview'}

class NoRedirect(HTTPRedirectHandler):
    def redirect_request(self,*args,**kwargs):raise RuntimeError('REDIRECT_FORBIDDEN')
CLIENT=build_opener(ProxyHandler({}),NoRedirect())

class Checks:
    def __init__(self):self.items=[]
    def test(self,title,condition,details=''):
        if not isinstance(condition,bool):condition=bool(condition)
        self.items.append({'name':title,'result':'PASS' if condition else 'FAIL','details':str(details)[:220]})
        print(('PASS' if condition else 'FAIL')+' | '+title+((' | '+str(details)[:160]) if details else ''),flush=True)
        return condition
    def outcome(self):
        p=sum(x['result']=='PASS' for x in self.items)
        return {'protocol':'FEE3_R4B1_SERVICES_READONLY_V1','atUtc':datetime.now(timezone.utc).isoformat(),
            'noWrites':True,'financialPosting':False,'passed':p,'failed':len(self.items)-p,'checks':self.items}

def http(base,method,path,data=None):
    if method=='GET':
        if path not in GET:raise RuntimeError('GET_ROUTE_NOT_ALLOWLISTED')
    elif method=='POST':
        if path not in POST:raise RuntimeError('POST_ROUTE_NOT_ALLOWLISTED')
    else:raise RuntimeError('HTTP_METHOD_NOT_ALLOWLISTED')
    payload=json.dumps(data,ensure_ascii=True,separators=(',',':')).encode('utf8') if data is not None else None
    req=Request(base+API+path,method=method,data=payload,
        headers={'Content-Type':'application/json','Accept':'application/json'})
    print('HTTP_STEP_START '+method+' '+path,flush=True)
    try:
        with CLIENT.open(req,timeout=15) as res:
            if res.status!=200:raise RuntimeError('NON_200_STATUS_'+str(res.status))
            if 'json' not in res.headers.get('Content-Type','').lower():raise RuntimeError('NON_JSON_CONTENT')
            raw=res.read(2000001)
            if len(raw)>2000000:raise RuntimeError('JSON_RESPONSE_TOO_BIG')
            value=json.loads(raw.decode('utf8'))
            print('HTTP_STEP_PASS '+method+' '+path+' HTTP=200',flush=True)
            return value
    except (HTTPError,URLError,ValueError,OSError,TimeoutError,RuntimeError) as exc:
        reason=type(exc).__name__
        if isinstance(exc,HTTPError):reason+=' HTTP='+str(exc.code)
        elif isinstance(exc,URLError):reason+=' '+str(exc.reason)[:110]
        else:reason+=' '+str(exc)[:110]
        print('HTTP_STEP_FAIL '+method+' '+path+' | '+reason,flush=True)
        raise RuntimeError(method+' '+path+' | '+reason) from exc

def root_request(op,day,method,amount):
    if method=='PROGRESSIVE':
        rows=[{'upTo':10000000,'ratePercent':0.1,'fixedAmount':0},
              {'upTo':None,'ratePercent':0.05,'fixedAmount':0}]
        config={'basisCode':'REQUEST_AMOUNT','basisExplicit':True,'rateSource':'CONFIG',
                'tiers':rows,'brackets':[]}
    else:
        rows=[{'from':0,'to':10000000,'ratePercent':0,'fixedAmount':20000},
              {'from':10000000,'to':None,'ratePercent':0.08,'fixedAmount':0}]
        config={'basisCode':'REQUEST_AMOUNT','basisExplicit':True,'rateSource':'CONFIG',
                'brackets':rows,'tiers':[]}
    draft={
        'calcItemId':None,'tariffVersionId':None,'expectedItemRecordVersion':None,'expectedVersionRecordVersion':None,
        'item':{'title':'R4-B1 no-write services golden','module':op['module'],'operation':op['code'],
                'amountKind':'SERVICE_FEE','variant':None,'ownerDomain':'TARIFF','description':None},
        'version':{'validFrom':day,'validTo':None,'origin':'BANK_INTERNAL','sourceId':None,
            'method':method,'taxMode':'EXEMPT','rounding':'HALF_UP',
            'changeReason':'R4B1 no-write service UAT','financialPolicy':{},
            'serviceCurrency':'IRR','feeCurrency':'IRR','uiContext':{}},
        'scope':{'channels':['ALL'],'customers':['ALL'],'groups':['ALL'],'products':['ALL'],
            'contracts':[],'currency':'IRR'},
        'rules':[{'order':1,'method':method,'base':'REQUEST_AMOUNT','rateSource':'RULE',
            'rate':None,'fixed':None,'collateral':None,'coverage':None,'min':None,'max':None,
            'formulaCode':None,'parameters':{'originalConfig':config}}],
        'tests':[{'title':'Numeric reference','request':{'amount':amount},'expected':None}]}
    return {'draft':draft,'inputs':{'amount':amount},'effectiveOn':None}

def ok_fee(r,amount):
    return isinstance(r,dict) and r.get('status')=='SIMULATED' and r.get('calculation',{}).get('status')=='SIMULATED' and (
        str(r.get('calculation',{}).get('feeIrr')) in (str(amount),str(amount)+'.0',str(amount)+'.00'))

def readonly_flags(r):
    return isinstance(r,dict) and all(r.get(k) is False for k in ('financialPosting','persisted','published','approved')) and (
        r.get('calculation',{}).get('financialPosting') is False and r.get('calculation',{}).get('persisted') is False)

def diagnostics_has(r,code):
    return isinstance(r,dict) and r.get('status')=='BLOCKED' and code in r.get('diagnostics',[])

def main():
    parser=argparse.ArgumentParser(description='Fail-closed R4B1 UAT preview test. No write API allowlisted.')
    parser.add_argument('--base-url',default='http://127.0.0.1:8091')
    args=parser.parse_args()
    checks=Checks()
    try:
        u=urlsplit(args.base_url)
        if u.scheme!='http' or u.hostname!='127.0.0.1' or u.port is None or u.path not in ('','/') or (
            u.query or u.fragment or u.username or u.password):raise RuntimeError('ONLY_DIRECT_LOOPBACK_127_0_0_1_ALLOWED')
        base='http://127.0.0.1:'+str(u.port)
        ready=http(base,'GET','/readiness')
        if not checks.test('FEE3 Oracle readiness and 11 required tables',
            isinstance(ready,dict) and ready.get('schema')=='FEE3' and ready.get('readsEnabled') is True and
            ready.get('missingTables')==[]):return 2
        if not checks.test('draft writes disabled',ready.get('draftWritesEnabled') is False):return 2
        before=(ready.get('calcItemCount'),ready.get('tariffVersionCount'))
        if not checks.test('baseline Oracle count fields',all(isinstance(n,int) and n>=0 for n in before),before):return 2
        day=ready.get('oracleBusinessDate');date.fromisoformat(day)
        workflow=http(base,'GET','/workflow/readiness')
        if not checks.test('workflow, authentication and posting denied',isinstance(workflow,dict) and all(
            workflow.get(k) is False for k in ('workflowEnabled','checkerDecisionEnabled','approvalEnabled',
            'postingEnabled','authenticated','makerRole','checkerRole'))):return 2
        ops=http(base,'GET','/references/operations')
        op=next((x for x in ops if isinstance(x,dict) and x.get('active') is True and
            'SERVICE_FEE' in x.get('allowedAmountKinds',[])),None)
        if not checks.test('active source operation from Oracle',op is not None):return 2
        progressive=root_request(op,day,'PROGRESSIVE',30000000)
        p=http(base,'POST','/r4/services/preview',progressive)
        if not checks.test('progressive source golden 30M = 20K',ok_fee(p,20000),p.get('diagnostics','')):return 2
        checks.test('R4B1 contract and 2-band trace',p.get('contractVersion')=='FEE3_SERVICE_FEE_R4B1_V1' and
            len(p.get('calculation',{}).get('breakdown',[]))==2)
        checks.test('correct unsaved status and no financial side effects',readonly_flags(p) and
            p.get('versionReference',{}).get('provenance')=='UNPERSISTED_CLIENT_DRAFT')
        checks.test('business date/rounding/hash provenance',p.get('oracleBusinessDate')==day and
            p.get('effectiveOn')==day and p.get('roundingScale')==0 and
            re.fullmatch('[0-9a-f]{64}',str(p.get('snapshotSha256',''))) is not None)
        p2=http(base,'POST','/r4/services/preview',progressive)
        checks.test('SHA-256 deterministic replay',p.get('snapshotSha256')==p2.get('snapshotSha256'))
        br=root_request(op,day,'BRACKET',10000000)
        b=http(base,'POST','/r4/services/preview',br)
        checks.test('bracket exact boundary 10M = 8K',ok_fee(b,8000))
        br['inputs']['amount']=9999999
        b=http(base,'POST','/r4/services/preview',br)
        checks.test('bracket below boundary in first fixed bracket = 20K',ok_fee(b,20000))
        first=root_request(op,day,'PROGRESSIVE',10000000)
        b=http(base,'POST','/r4/services/preview',first)
        checks.test('progressive limit enters first band only = 10K',ok_fee(b,10000))
        taxed=copy.deepcopy(first);taxed['draft']['version']['taxMode']='TAXABLE'
        taxed['draft']['version']['financialPolicy']={'taxPercent':10}
        b=http(base,'POST','/r4/services/preview',taxed)
        checks.test('explicit tax on rounded 10K fee = 1K; total 11K',ok_fee(b,10000) and
            str(b.get('calculation',{}).get('taxIrr')) in ('1000','1000.0') and
            str(b.get('calculation',{}).get('totalIrr')) in ('11000','11000.0'))
        capped=copy.deepcopy(progressive);capped['draft']['rules'][0]['max']=15000
        b=http(base,'POST','/r4/services/preview',capped)
        checks.test('max cap applied to full progressive sum = 15K',ok_fee(b,15000))
        bad=copy.deepcopy(progressive);bad['inputs']['amount']=100.001
        b=http(base,'POST','/r4/services/preview',bad)
        checks.test('decimal scale >2 rejected',diagnostics_has(b,'EXPLICIT_POSITIVE_IRR_AMOUNT_REQUIRED'))
        bad=copy.deepcopy(progressive);bad['draft']['rules'][0]['parameters']['originalConfig']['tiers']=[
            {'upTo':None,'ratePercent':0.123456789,'fixedAmount':0}]
        b=http(base,'POST','/r4/services/preview',bad)
        checks.test('tier rate beyond 8 decimals rejected',diagnostics_has(b,'TIER_RATE_OR_FIXED_INVALID'))
        bad=copy.deepcopy(br);bad['draft']['rules'][0]['parameters']['originalConfig']['brackets']=[
            {'from':0,'to':10,'ratePercent':1,'fixedAmount':0},
            {'from':11,'to':None,'ratePercent':1,'fixedAmount':0}]
        b=http(base,'POST','/r4/services/preview',bad)
        checks.test('gap between brackets rejected, not treated as zero',diagnostics_has(b,'BRACKET_GAP_OR_OVERLAP'))
        bad=copy.deepcopy(progressive);bad['draft']['version']['method']='FORMULA'
        b=http(base,'POST','/r4/services/preview',bad)
        checks.test('formula method cannot run through service tier endpoint',diagnostics_has(b,'SERVICE_METHOD_NOT_IN_R4B1'))
        bad=copy.deepcopy(progressive);bad['draft']['version']['financialPolicy']={'waiverPercent':100}
        b=http(base,'POST','/r4/services/preview',bad)
        checks.test('unapproved waiver forbidden',diagnostics_has(b,'FINANCIAL_ADJUSTMENTS_NOT_CERTIFIED'))
        bad=copy.deepcopy(progressive);bad['draft']['calcItemId']=99999999
        b=http(base,'POST','/r4/services/preview',bad)
        checks.test('partial Oracle reference forbidden',diagnostics_has(b,'PARTIAL_ORACLE_REFERENCE'))
        bad=copy.deepcopy(progressive);bad['effectiveOn']=(date.fromisoformat(day)-timedelta(days=1)).isoformat()
        b=http(base,'POST','/r4/services/preview',bad)
        checks.test('historical effective date rejected',diagnostics_has(b,'HISTORICAL_SIMULATION_NOT_ALLOWED'))
        end=http(base,'GET','/readiness')
        checks.test('Oracle catalog counts unchanged',before==(
            end.get('calcItemCount'),end.get('tariffVersionCount')))
    except (ValueError,KeyError,TypeError,RuntimeError) as err:
        checks.test('R4B1 runtime failure',False,str(err)[:180])
    finally:
        report=checks.outcome();REPORT.parent.mkdir(parents=True,exist_ok=True)
        REPORT.write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf8')
        print('FEE3_R4B1_RUNTIME_PASS='+str(report['passed'])+' FAIL='+str(report['failed']),flush=True)
        print('REPORT='+str(REPORT.relative_to(ROOT)),flush=True)
    return 0 if checks.outcome()['failed']==0 else 2

if __name__=='__main__':sys.exit(main())
