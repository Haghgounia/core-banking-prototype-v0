#!/usr/bin/env python3
"""R4-A runtime acceptance, loopback-only, no proxy, no redirects, no Oracle writes."""
from __future__ import annotations
from datetime import date,timedelta,datetime,timezone
from pathlib import Path
import argparse,hashlib,json,re,sys
from urllib.request import Request,build_opener,ProxyHandler,HTTPRedirectHandler
from urllib.error import HTTPError,URLError
from urllib.parse import urlsplit

ROOT=Path(__file__).resolve().parents[3]
RESULT=ROOT/'docs/install/FEE3-R4A-RUNTIME-RESULT.json'
API='/api/v1/fee3'
GET={'/readiness','/references/operations','/workflow/readiness'}
POST={'/r4/preview'}

class NoRedirect(HTTPRedirectHandler):
    def redirect_request(self,req,fp,code,msg,headers,url):
        raise RuntimeError('REDIRECT_FORBIDDEN')
CLIENT=build_opener(ProxyHandler({}),NoRedirect())

class Gate:
    def __init__(self):self.checks=[]
    def check(self,name,ok,detail=''):
        self.checks.append({'name':name,'result':'PASS' if ok else 'FAIL','details':str(detail)[:200]})
        print(('PASS' if ok else 'FAIL')+' | '+name+((' | '+str(detail)[:200]) if detail else ''),flush=True)
        return ok
    def result(self):
        passed=sum(c['result']=='PASS' for c in self.checks)
        failed=len(self.checks)-passed
        return {'protocol':'FEE3_R4A_READONLY_V1','atUtc':datetime.now(timezone.utc).isoformat(),
                'noWrites':True,'posting':False,'passed':passed,'failed':failed,'checks':self.checks}

def http(base,method,path,payload=None):
    if method=='GET' and path not in GET or method=='POST' and path not in POST or method not in ('GET','POST'):
        raise RuntimeError('REQUEST_NOT_ALLOWLISTED')
    data=json.dumps(payload,ensure_ascii=True,separators=(',',':')).encode('utf-8') if payload is not None else None
    req=Request(base+API+path,data=data,method=method,
        headers={'Content-Type':'application/json','Accept':'application/json'})
    print('HTTP_STEP_START '+method+' '+path,flush=True)
    try:
        with CLIENT.open(req,timeout=12) as response:
            if response.status!=200:raise RuntimeError('HTTP_STATUS_'+str(response.status))
            if 'json' not in response.headers.get('Content-Type','').lower():raise RuntimeError('NON_JSON_CONTENT')
            raw=response.read(2000001)
            if len(raw)>2000000:raise RuntimeError('BODY_TOO_LARGE')
            value=json.loads(raw.decode('utf-8'))
            print('HTTP_STEP_PASS '+method+' '+path+' HTTP=200',flush=True)
            return value
    except (HTTPError,URLError,OSError,TimeoutError,ValueError,RuntimeError) as err:
        label=type(err).__name__
        if isinstance(err,HTTPError):label+=' '+str(err.code)
        elif isinstance(err,URLError):label+=' '+str(err.reason)[:130]
        elif isinstance(err,RuntimeError):label+=' '+str(err)[:80]
        raise RuntimeError(method+' '+path+' | '+label) from err

def fixture(op,day):
    return {
        'calcItemId':None,'tariffVersionId':None,'expectedItemRecordVersion':None,
        'expectedVersionRecordVersion':None,
        'item':{'title':'R4-A read only test','module':op['module'],'operation':op['code'],
                'amountKind':'SERVICE_FEE','variant':None,'ownerDomain':'TARIFF','description':None},
        'version':{'validFrom':day,'validTo':None,'origin':'BANK_INTERNAL','sourceId':None,
                   'method':'FIXED','taxMode':'EXEMPT','rounding':'HALF_UP','changeReason':'Read only test',
                   'financialPolicy':{},'serviceCurrency':'IRR','feeCurrency':'IRR','uiContext':{}},
        'scope':{'channels':['ALL'],'customers':['ALL'],'groups':['ALL'],
                 'products':['ALL'],'contracts':[],'currency':'IRR'},
        'rules':[{'order':1,'method':'FIXED','base':'REQUEST_AMOUNT','rateSource':'RULE','rate':None,
                  'fixed':12500,'collateral':None,'coverage':None,'min':None,'max':None,
                  'formulaCode':None,'parameters':{}}],
        'tests':[{'title':'R4-A evidence','request':{'amount':1000000},'expected':12500}]
    }

def main():
    parser=argparse.ArgumentParser()
    parser.add_argument('--base-url',default='http://127.0.0.1:8091')
    args=parser.parse_args()
    gate=Gate()
    try:
        parsed=urlsplit(args.base_url)
        if(parsed.scheme!='http' or parsed.hostname!='127.0.0.1' or parsed.port is None or
           parsed.path not in ('','/') or parsed.query or parsed.fragment or parsed.username or parsed.password):
            raise RuntimeError('ONLY_EXPLICIT_HTTP_127.0.0.1_PORT_ALLOWED')
        base='http://127.0.0.1:'+str(parsed.port)
        first=http(base,'GET','/readiness')
        if not gate.check('FEE3 Oracle reachable and all tables present',isinstance(first,dict) and
            first.get('readsEnabled') is True and first.get('missingTables')==[]):return 2
        if not gate.check('draft writes explicitly disabled',first.get('draftWritesEnabled') is False):return 2
        before=(first.get('calcItemCount'),first.get('tariffVersionCount'))
        if not gate.check('Oracle counts present',all(isinstance(x,int) and x>=0 for x in before)):return 2
        day=first.get('oracleBusinessDate');date.fromisoformat(day)
        workflow=http(base,'GET','/workflow/readiness')
        if not gate.check('all workflow switches denied',isinstance(workflow,dict) and all(
            workflow.get(k) is False for k in ('workflowEnabled','checkerDecisionEnabled',
            'approvalEnabled','postingEnabled','authenticated','makerRole','checkerRole'))):return 2
        operations=http(base,'GET','/references/operations')
        op=next((x for x in operations if isinstance(x,dict) and x.get('active') is True and
            'SERVICE_FEE' in x.get('allowedAmountKinds',[])),None)
        if not gate.check('authoritative active fee service operation available',op is not None):return 2
        draft=fixture(op,day)
        request={'draft':draft,'inputs':{'amount':1000000},'effectiveOn':None}
        simulated=http(base,'POST','/r4/preview',request)
        if not gate.check('R4-A response contract and 12500 IRR simulation',
            simulated.get('status')=='SIMULATED' and simulated.get('contractVersion')==
            'FEE3_CALCULATION_CONTRACT_R4A_V1' and str(simulated.get('calculation',{}).get('totalIrr')) in
            ('12500','12500.0','12500.00')):return 2
        gate.check('unapproved unpersisted version is explicitly identified',
            simulated.get('versionReference',{}).get('provenance')=='UNPERSISTED_CLIENT_DRAFT' and
            simulated.get('simulationMode')=='UAT_DRAFT_SIMULATION' and
            all(simulated.get(k) is False for k in ('financialPosting','persisted','published','approved')))
        gate.check('Oracle clock, precision and SHA256 evidence',
            simulated.get('oracleBusinessDate')==day and simulated.get('effectiveOn')==day and
            simulated.get('roundingScale')==0 and simulated.get('roundingCode')=='HALF_UP' and
            re.fullmatch('[0-9a-f]{64}',str(simulated.get('snapshotSha256',''))) is not None)
        second=http(base,'POST','/r4/preview',request)
        gate.check('snapshot hash repeats exactly',second.get('snapshotSha256')==simulated.get('snapshotSha256'))
        bad=json.loads(json.dumps(request));bad['draft']['rules'][0]['fixed']=12500.001
        v=http(base,'POST','/r4/preview',bad)
        gate.check('Oracle 22,2 precision rejects 3 fractional digits',v.get('status')=='BLOCKED' and
            'IRR_MONEY_EXCEEDS_NUMBER_22_2' in v.get('diagnostics',[]))
        bad=json.loads(json.dumps(request));bad['effectiveOn']=(date.fromisoformat(day)-timedelta(days=1)).isoformat()
        v=http(base,'POST','/r4/preview',bad)
        gate.check('historical effective date rejected',v.get('status')=='BLOCKED' and
            'HISTORICAL_SIMULATION_NOT_IN_R4A' in v.get('diagnostics',[]))
        bad=json.loads(json.dumps(request));bad['draft']['version']['method']='FORMULA';bad['draft']['rules'][0]['method']='FORMULA'
        v=http(base,'POST','/r4/preview',bad)
        gate.check('uncertified formula rejected',v.get('status')=='BLOCKED' and
            'METHOD_NOT_CERTIFIED_FOR_R4A' in v.get('diagnostics',[]))
        bad=json.loads(json.dumps(request));bad['draft']['calcItemId']=99999999
        v=http(base,'POST','/r4/preview',bad)
        gate.check('partial persisted ID cannot spoof published tariff',v.get('status')=='BLOCKED' and
            'PARTIAL_VERSION_REFERENCE_FORBIDDEN' in v.get('diagnostics',[]))
        last=http(base,'GET','/readiness')
        gate.check('item/version counts unchanged',before==(last.get('calcItemCount'),last.get('tariffVersionCount')))
    except (ValueError,TypeError,KeyError,RuntimeError) as exc:
        gate.check('R4A runtime failure',False,str(exc)[:180])
    finally:
        report=gate.result();RESULT.parent.mkdir(parents=True,exist_ok=True)
        RESULT.write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
        print('FEE3_R4A_RUNTIME_PASS='+str(report['passed'])+' FAIL='+str(report['failed']),flush=True)
        print('REPORT='+str(RESULT.relative_to(ROOT)),flush=True)
    return 0 if gate.result()['failed']==0 else 2

if __name__=='__main__':sys.exit(main())
