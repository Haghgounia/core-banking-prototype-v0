#!/usr/bin/env python3
"""R4-B2 real UAT HTTP qualification: direct loopback, GET and preview-only POST, no writes.
Production engine validation can only be proven by running this against the real Spring/Oracle runtime.
"""
from __future__ import annotations
import argparse,copy,json,re,sys
from datetime import date,datetime,timezone
from decimal import Decimal,InvalidOperation
from pathlib import Path
from urllib.error import HTTPError,URLError
from urllib.parse import urlsplit
from urllib.request import Request,build_opener,ProxyHandler,HTTPRedirectHandler

ROOT=Path(__file__).resolve().parents[3]
REPORT=ROOT/'docs/install/FEE3-R4B2-RUNTIME-RESULT.json'
API='/api/v1/fee3'
GET={'/readiness','/workflow/readiness','/references/operations'}
POST={'/r4/guarantees/preview'}

class NoRedirect(HTTPRedirectHandler):
    def redirect_request(self,*args,**kwargs):raise RuntimeError('REDIRECT_FORBIDDEN')
CLIENT=build_opener(ProxyHandler({}),NoRedirect())

class Checks:
    def __init__(self):self.items=[]
    def check(self,name,result,details=''):
        result=bool(result);self.items.append({'name':name,'result':'PASS' if result else 'FAIL','details':str(details)[:180]})
        print(('PASS' if result else 'FAIL')+' | '+name+((' | '+str(details)[:100]) if details else ''),flush=True)
        return result
    def outcome(self):
        passed=sum(i['result']=='PASS' for i in self.items)
        return {'protocol':'FEE3_R4B2_GUARANTEE_READONLY_V1','atUtc':datetime.now(timezone.utc).isoformat(),
            'noWrites':True,'financialPosting':False,'passed':passed,'failed':len(self.items)-passed,
            'checks':self.items}

def http(base,method,path,body=None):
    if method=='GET' and path not in GET or method=='POST' and path not in POST or method not in ('GET','POST'):
        raise RuntimeError('HTTP_METHOD_OR_ROUTE_NOT_ALLOWLISTED')
    data=json.dumps(body,ensure_ascii=True,separators=(',',':')).encode('utf8') if body is not None else None
    req=Request(base+API+path,method=method,data=data,
        headers={'Content-Type':'application/json','Accept':'application/json'})
    print('HTTP_STEP_START '+method+' '+path,flush=True)
    try:
        with CLIENT.open(req,timeout=15) as res:
            if res.status!=200:raise RuntimeError('NON_200_HTTP_STATUS_'+str(res.status))
            if 'json' not in res.headers.get('Content-Type','').lower():raise RuntimeError('NON_JSON_CONTENT')
            payload=res.read(2_000_001)
            if len(payload)>2_000_000:raise RuntimeError('OVERSIZED_RESPONSE')
            value=json.loads(payload.decode('utf8'))
            print('HTTP_STEP_PASS '+method+' '+path+' HTTP=200',flush=True)
            return value
    except (HTTPError,URLError,ValueError,OSError,RuntimeError,TimeoutError) as e:
        if isinstance(e,HTTPError):
            reason='HTTP_'+str(e.code)
            try:
                err=json.loads(e.read(5000))
                if isinstance(err,dict):reason+=' '+str(err.get('errorCode',''))[:80]
            except (ValueError,OSError):pass
        else:reason=type(e).__name__+' '+str(getattr(e,'reason',e))[:130]
        print('HTTP_STEP_FAIL '+method+' '+path+' | '+reason,flush=True)
        raise RuntimeError(method+' '+path+' | '+reason) from e

def tariff_rule(t,src,rate,coverage):
    return {'collateralType':t,'rateSource':src,'ratePercent':rate,'coveragePercent':coverage}

def request(op,day,basis='GUARANTEE_PORTFOLIO_ALLOCATED',method='PERCENT'):
    portfolio={'collateralType':'MIXED_PORTFOLIO','feeBasis':basis,'portfolioRules':[
        tariff_rule('CASH_100','EXEMPT',None,100),
        tariff_rule('PROMISSORY_NOTE','CONFIG',2.25,120),
        tariff_rule('MOVABLE_PROPERTY','CONFIG',1,100)]}
    data={'amount':100000000,'portfolio':[
        {'collateralType':'CASH_100','sharePercent':'15'},
        {'collateralType':'PROMISSORY_NOTE','sharePercent':'50'},
        {'collateralType':'MOVABLE_PROPERTY','sharePercent':'35'}]}
    params={}
    if method=='DAILY':
        begin=date(date.fromisoformat(day).year+1,1,1)
        end=date(begin.year+1,1,1)
        data.update({'periodStart':begin.isoformat(),'periodEndExclusive':end.isoformat()})
        params={'dayBasis':'ACT_ACT'}
    draft={
        'calcItemId':None,'tariffVersionId':None,'expectedItemRecordVersion':None,'expectedVersionRecordVersion':None,
        'item':{'title':'R4B2 guarantee no-write UAT','module':op['module'],'operation':op['code'],
            'amountKind':'GUARANTEE_FEE','variant':None,'ownerDomain':'GUARANTEE','description':None},
        'version':{'validFrom':day,'validTo':None,'origin':'BANK_INTERNAL','sourceId':None,
            'method':method,'taxMode':'EXEMPT','rounding':'HALF_UP','changeReason':'R4B2 safe no-write UAT',
            'financialPolicy':{},'serviceCurrency':'IRR','feeCurrency':'IRR',
            'uiContext':{'guaranteePolicy':portfolio}},
        'scope':{'channels':['ALL'],'customers':['ALL'],'groups':['ALL'],'products':['ALL'],
            'contracts':[],'currency':'IRR'},
        'rules':[{'order':1,'method':method,'base':basis,'rateSource':'RULE',
            'rate':None,'fixed':None,'collateral':None,'coverage':None,'min':None,'max':None,
            'formulaCode':None,'parameters':{'originalConfig':params}}],
        'tests':[{'title':'Numerical source comparison','request':{'amount':100000000},'expected':None}]
    }
    return {'draft':draft,'inputs':data,'effectiveOn':None}

def fees(r,fee,tax=None):
    if not isinstance(r,dict) or r.get('status')!='SIMULATED':return False
    c=r.get('calculation')
    if not isinstance(c,dict) or c.get('status')!='SIMULATED':return False
    def same(v,goal):
        try:
            x=Decimal(str(v));return x.is_finite() and x==Decimal(str(goal))
        except (TypeError,ValueError,InvalidOperation):return False
    return same(c.get('feeIrr'),fee) and (tax is None or same(c.get('taxIrr'),tax))

def safe(r):
    return (isinstance(r,dict) and all(r.get(k) is False for k in
        ('persisted','financialPosting','approved','published')) and isinstance(r.get('calculation'),dict) and
        r['calculation'].get('persisted') is False and r['calculation'].get('financialPosting') is False)

def blocked(r,code):return isinstance(r,dict) and r.get('status')=='BLOCKED' and code in r.get('diagnostics',[])

def main():
    parser=argparse.ArgumentParser(description='R4B2 restricted Oracle-backed preview qualification')
    parser.add_argument('--base-url',default='http://127.0.0.1:8091')
    args=parser.parse_args();c=Checks()
    try:
        u=urlsplit(args.base_url)
        if (u.scheme!='http' or u.hostname!='127.0.0.1' or u.port is None or
            u.path not in ('','/') or u.query or u.fragment or u.username or u.password):
            raise RuntimeError('ONLY_DIRECT_LOOPBACK_127_0_0_1_ALLOWED')
        base='http://127.0.0.1:'+str(u.port)
        r=http(base,'GET','/readiness')
        if not c.check('Oracle readiness and 11 tables',isinstance(r,dict) and r.get('schema')=='FEE3' and
            r.get('readsEnabled') is True and r.get('missingTables')==[]):return 2
        if not c.check('draft writes disabled',r.get('draftWritesEnabled') is False):return 2
        before=(r.get('calcItemCount'),r.get('tariffVersionCount'))
        if not c.check('real Oracle baseline present',all(isinstance(v,int) and v>=0 for v in before),before):return 2
        day=r.get('oracleBusinessDate');date.fromisoformat(day)
        state=http(base,'GET','/workflow/readiness')
        if not c.check('all workflow and posting blocked',isinstance(state,dict) and all(
            state.get(k) is False for k in ('workflowEnabled','checkerDecisionEnabled','approvalEnabled',
                'postingEnabled','authenticated','makerRole','checkerRole'))):return 2
        ops=http(base,'GET','/references/operations')
        op=next((o for o in ops if isinstance(o,dict) and o.get('module')=='GUARANTEE' and
            o.get('code')=='GUARANTEE_ISSUE' and o.get('active') is True and
            'GUARANTEE_FEE' in o.get('allowedAmountKinds',[])),None)
        if not c.check('active exact GUARANTEE_ISSUE operation from Oracle',op is not None):return 2
        q=request(op,day)
        result=http(base,'POST','/r4/guarantees/preview',q)
        if not c.check('portfolio allocated 15/50/35 is 1475000',fees(result,1475000),
                result.get('diagnostics','')):return 2
        c.check('contract version and three collateral trace rows',
            result.get('contractVersion')=='FEE3_GUARANTEE_R4B2_V1' and
            len(result.get('calculation',{}).get('breakdown',[]))==3)
        c.check('read-only draft has no banking side effects',safe(result) and
            result.get('versionReference',{}).get('provenance')=='UNPERSISTED_CLIENT_DRAFT')
        c.check('rounding, bank date, sha256 provenance',result.get('oracleBusinessDate')==day and
            result.get('roundingScale')==0 and re.fullmatch('[0-9a-f]{64}',str(result.get('snapshotSha256',''))) is not None)
        result2=http(base,'POST','/r4/guarantees/preview',q)
        c.check('deterministic replay SHA256',result.get('snapshotSha256')==result2.get('snapshotSha256'))
        q2=request(op,day,'GUARANTEE_PORTFOLIO_COLLATERAL_VALUE')
        x=http(base,'POST','/r4/guarantees/preview',q2)
        c.check('portfolio covered basis 15/50/35 is 1700000',fees(x,1700000))
        q2=request(op,day);q2['inputs']['portfolio']=[
            {'collateralType':'CASH_100','sharePercent':'40'},
            {'collateralType':'PROMISSORY_NOTE','sharePercent':'60'}]
        x=http(base,'POST','/r4/guarantees/preview',q2)
        c.check('two collateral allocated 40/60 is 1350000',fees(x,1350000))
        q2['draft']['version']['uiContext']['guaranteePolicy']['feeBasis']='GUARANTEE_PORTFOLIO_COLLATERAL_VALUE'
        q2['draft']['rules'][0]['base']='GUARANTEE_PORTFOLIO_COLLATERAL_VALUE'
        x=http(base,'POST','/r4/guarantees/preview',q2)
        c.check('two collateral covered 40/60 is 1620000',fees(x,1620000))
        daily=request(op,day,method='DAILY')
        x=http(base,'POST','/r4/guarantees/preview',daily)
        c.check('one full Gregorian year ACT_ACT is whole fee',fees(x,1475000))
        taxable=request(op,day);taxable['draft']['version']['taxMode']='TAXABLE'
        taxable['draft']['version']['financialPolicy']={'taxPercent':10}
        x=http(base,'POST','/r4/guarantees/preview',taxable)
        c.check('tax once on final fee 147500',fees(x,1475000,147500))
        capped=request(op,day);capped['draft']['rules'][0]['max']=1200000
        x=http(base,'POST','/r4/guarantees/preview',capped)
        c.check('aggregate maximum applied once 1200000',fees(x,1200000))
        bad=copy.deepcopy(q);bad['inputs']['portfolio'][2]['sharePercent']='30'
        x=http(base,'POST','/r4/guarantees/preview',bad)
        c.check('invalid share total denied',blocked(x,'PORTFOLIO_SHARES_MUST_TOTAL_100'))
        bad=copy.deepcopy(q);bad['draft']['version']['uiContext']['guaranteePolicy']['portfolioRules'][0]['coveragePercent']=99
        x=http(base,'POST','/r4/guarantees/preview',bad)
        c.check('cash collateral less than 100 denied',blocked(x,'CASH_100_COVERAGE_BELOW_100'))
        bad=copy.deepcopy(q);bad['draft']['version']['uiContext']['guaranteePolicy']['portfolioRules'][1]['ratePercent']=None
        x=http(base,'POST','/r4/guarantees/preview',bad)
        c.check('missing tariff rate denied',blocked(x,'TARIFF_RATE_INVALID'))
        bad=copy.deepcopy(q);bad['inputs']['yearFraction']=1
        x=http(base,'POST','/r4/guarantees/preview',bad)
        c.check('client-supplied year fraction denied',blocked(x,'PORTFOLIO_INPUT_FIELDS_UNREVIEWED'))
        bad=copy.deepcopy(q);bad['draft']['item']['operation']='GUARANTEE_RENEW'
        x=http(base,'POST','/r4/guarantees/preview',bad)
        c.check('renewal method cannot reuse issuance rule',blocked(x,'GUARANTEE_ISSUE_ONLY'))
        bad=copy.deepcopy(q);bad['draft']['version']['method']='FORMULA'
        x=http(base,'POST','/r4/guarantees/preview',bad)
        c.check('uncertified formula blocked',blocked(x,'GUARANTEE_METHOD_NOT_CERTIFIED'))
        bad=copy.deepcopy(q);bad['draft']['calcItemId']=1111
        x=http(base,'POST','/r4/guarantees/preview',bad)
        c.check('partial Oracle draft reference blocked',blocked(x,'PARTIAL_ORACLE_REFERENCE'))
        bad=copy.deepcopy(q);bad['inputs']['amount']='100000000.001'
        x=http(base,'POST','/r4/guarantees/preview',bad)
        c.check('precision beyond NUMBER22_2 denied',blocked(x,'POSITIVE_GUARANTEE_AMOUNT_REQUIRED'))
        bad=copy.deepcopy(q);bad['draft']['version']['financialPolicy']={'waiverPercent':100}
        x=http(base,'POST','/r4/guarantees/preview',bad)
        c.check('unapproved waiver not applied',blocked(x,'FINANCIAL_MODIFIERS_UNREVIEWED'))
        after=http(base,'GET','/readiness')
        c.check('Oracle item/version counts unchanged',before==(
            after.get('calcItemCount'),after.get('tariffVersionCount')))
    except (ValueError,KeyError,TypeError,RuntimeError) as error:
        c.check('R4B2 runtime transport/contract error',False,str(error)[:180])
    finally:
        report=c.outcome();REPORT.parent.mkdir(parents=True,exist_ok=True)
        REPORT.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
        print('FEE3_R4B2_RUNTIME_PASS='+str(report['passed'])+' FAIL='+str(report['failed']),flush=True)
        print('REPORT='+str(REPORT.relative_to(ROOT)),flush=True)
    return 0 if c.outcome()['failed']==0 else 2

if __name__=='__main__':sys.exit(main())
