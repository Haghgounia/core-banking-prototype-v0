#!/usr/bin/env python3
"""FEE3 R4C2: fail-closed direct-loopback Oracle qualification; read-only GET + simulation-only POST.
Never exercises save, approval, publishing, posting, or accounting APIs.
"""
from __future__ import annotations
import argparse,copy,json,re,sys
from datetime import date,datetime,timedelta,timezone
from decimal import Decimal,ROUND_HALF_UP,localcontext
from pathlib import Path
from urllib.parse import urlsplit
from urllib.request import ProxyHandler,HTTPRedirectHandler,build_opener,Request
from urllib.error import HTTPError,URLError

ROOT=Path(__file__).resolve().parents[3]
REPORT=ROOT/'docs/install/FEE3-R4C2-RUNTIME-RESULT.json'
API='/api/v1/fee3'
GET={'/readiness','/workflow/readiness','/references/operations','/references/formulas'}
POST={'/r4/loans/models/preview'}
class NoRedirect(HTTPRedirectHandler):
    def redirect_request(self,*args,**kwargs):raise RuntimeError('REDIRECT_FORBIDDEN')
CLIENT=build_opener(ProxyHandler({}),NoRedirect())
class Checks:
    def __init__(self):self.rows=[]
    def add(self,name,okay,detail=''):
        okay=bool(okay)
        self.rows.append({'name':name,'result':'PASS' if okay else 'FAIL','detail':str(detail)[:200]})
        print(('PASS' if okay else 'FAIL')+' | '+name+((' | '+str(detail)[:120]) if detail else ''),flush=True)
    def report(self):
        passed=sum(v['result']=='PASS' for v in self.rows)
        return {'protocol':'FEE3_R4C2_LOAN_MODELS_READONLY_V1',
            'atUtc':datetime.now(timezone.utc).isoformat(),'noMutationAPIs':True,
            'passed':passed,'failed':len(self.rows)-passed,'checks':self.rows}

def http(base,method,path,body=None):
    if method=='GET':
        if path not in GET or body is not None:raise RuntimeError('GET_NOT_ALLOWLISTED')
    elif method=='POST':
        if path not in POST or body is None:raise RuntimeError('POST_NOT_ALLOWLISTED')
    else:raise RuntimeError('HTTP_METHOD_FORBIDDEN')
    payload=None if body is None else json.dumps(body,ensure_ascii=True,separators=(',',':')).encode()
    req=Request(base+API+path,method=method,data=payload,
                headers={'Content-Type':'application/json','Accept':'application/json'})
    print('HTTP_STEP_START '+method+' '+path,flush=True)
    try:
        with CLIENT.open(req,timeout=20) as response:
            if response.status!=200:raise RuntimeError('HTTP_NOT_200_'+str(response.status))
            if 'json' not in response.headers.get('Content-Type','').lower():
                raise RuntimeError('NON_JSON_RESPONSE')
            data=response.read(2_000_001)
            if len(data)>2_000_000:raise RuntimeError('OVERSIZED_RESPONSE')
            result=json.loads(data.decode('utf8'))
            print('HTTP_STEP_PASS '+method+' '+path+' HTTP=200',flush=True)
            return result
    except (HTTPError,URLError,RuntimeError,OSError,ValueError,TimeoutError) as ex:
        msg=type(ex).__name__+' '+str(getattr(ex,'reason',ex))[:150]
        print('HTTP_STEP_FAIL '+method+' '+path+' | '+msg,flush=True)
        raise RuntimeError(msg) from ex

SPEC={
 'PARTICIPATION_REALIZED':('REALIZED_PROFIT','PARTICIPATION_REALIZED','CONTRACT_MONTHS'),
 'PARTICIPATION_ESTIMATE':('ESTIMATED_PROFIT','TPL_E624746F262F6E','JALALI_ACT_ACT'),
 'DEBT_PURCHASE_SPREAD':('PURCHASE_SPREAD','DEBT_PURCHASE_SPREAD','CONTRACT_MONTHS'),
 'SALAM_SPREAD':('SALAM_SETTLEMENT','SALAM_PRICE_SPREAD','CONTRACT_MONTHS'),
 'SALAM_ESTIMATE':('ESTIMATED_PROFIT','TPL_E624746F262F6E','JALALI_ACT_ACT')
}
NATURE={
 'PARTICIPATION_REALIZED':'BANK_SHARE_OF_REPORTED_REALIZED_PROFIT_UNVERIFIED',
 'PARTICIPATION_ESTIMATE':'PARTICIPATION_PERIOD_ESTIMATE_NOT_REALIZED',
 'DEBT_PURCHASE_SPREAD':'GROSS_DEBT_PURCHASE_SPREAD_NOT_RECOGNIZED_PROFIT',
 'SALAM_SPREAD':'GROSS_SALAM_SPREAD_NOT_RECOGNIZED_PROFIT',
 'SALAM_ESTIMATE':'SALAM_PERIOD_ESTIMATE_NOT_REALIZED'
}

def request(day,profile,contract_override=None):
    event,formula,basis=SPEC[profile]
    contract=contract_override or ({'DEBT_PURCHASE_SPREAD':'DEBT_PURCHASE',
        'SALAM_SPREAD':'SALAM','SALAM_ESTIMATE':'SALAM'}.get(profile,'MUSHARAKA_MADANI'))
    data={'contractReference':'UAT-LOAN-CONTRACT-UNVERIFIED',
          'calculationEvidenceReference':'UAT-EVENT-PROOF-UNVERIFIED'}
    if basis=='JALALI_ACT_ACT':
        data.update({'amount':'100000000.00','contractRate':'18.00000000',
            'rateEvidenceReference':'UAT-RATE-UNVERIFIED','periodStart':day,
            'periodEndExclusive':(date.fromisoformat(day)+timedelta(days=10)).isoformat(),
            'estimateBasisReference':'UAT-ESTIMATE-BASE-UNVERIFIED'})
    else:
        data['eventDate']=day
        if profile=='PARTICIPATION_REALIZED':data.update({
            'realizedProfit':'90000000.00','sharePercent':'40.00000000',
            'realizationStatementReference':'UAT-REALIZATION-UNVERIFIED',
            'shareAgreementReference':'UAT-SHARE-UNVERIFIED'})
        if profile=='DEBT_PURCHASE_SPREAD':data.update({
            'faceAmount':'100000000.00','purchasePrice':'92000000.00',
            'faceAmountEvidenceReference':'UAT-FACE-UNVERIFIED',
            'purchasePriceEvidenceReference':'UAT-PURCHASE-UNVERIFIED'})
        if profile=='SALAM_SPREAD':data.update({
            'saleAmount':'105000000.00','purchaseAmount':'90000000.00',
            'saleEvidenceReference':'UAT-SALE-UNVERIFIED',
            'purchaseEvidenceReference':'UAT-PURCHASE-UNVERIFIED'})
    draft={
        'calcItemId':None,'tariffVersionId':None,'expectedItemRecordVersion':None,
        'expectedVersionRecordVersion':None,
        'item':{'title':'R4-C2 UAT LOAN MODEL '+profile,'module':'LOAN',
            'operation':'LOAN_PROFIT','amountKind':'FINANCING_PROFIT',
            'variant':None,'ownerDomain':'LOAN','description':None},
        'version':{'validFrom':day,'validTo':None,'origin':'CONTRACT_RATE_REGULATED',
            'sourceId':None,'method':'FORMULA','taxMode':'OUT_OF_SCOPE',
            'rounding':'HALF_UP','changeReason':'nonfinancial unverified UAT',
            'financialPolicy':{},'serviceCurrency':'IRR','feeCurrency':'IRR',
            'uiContext':{'loanModelProfile':profile,'loanCalculationEvent':event,
                'loanContractTypes':[contract],
                'loanPolicy':{'contractEvidenceRequired':True,'dayCountBasis':basis,
                              'holidayPolicy':'CHARGE_ALL_DAYS'}}},
        'scope':{'channels':['ALL'],'customers':['ALL'],'groups':['ALL'],
            'products':['ALL'],'contracts':[contract],'currency':'IRR'},
        'rules':[{'order':1,'method':'FORMULA','base':'REQUEST_AMOUNT','rateSource':'REQUEST',
            'rate':None,'fixed':None,'collateral':None,'coverage':None,
            'min':None,'max':None,'formulaCode':formula,'parameters':{}}],
        'tests':[{'title':'UAT reference only','request':{'amount':'100000000'},'expected':None}]
    }
    return {'draft':draft,'inputs':data,'effectiveOn':None}

def safe(reply):
    return isinstance(reply,dict) and all(reply.get(k) is False for k in
        ('approved','published','persisted','financialPosting'))
def simulation(reply,profile,total):
    if not safe(reply) or reply.get('status')!='SIMULATED':return False
    c=reply.get('calculation')
    if not isinstance(c,dict) or c.get('status')!='SIMULATED' or \
       c.get('modelProfile')!=profile or c.get('resultKind')!=NATURE[profile] or \
       reply.get('resultKind')!=NATURE[profile]:return False
    try:return Decimal(str(c.get('roundedIrr')))==Decimal(str(total)) and \
        Decimal(str(c.get('rawIrr')))>=0
    except (ValueError,TypeError):return False

def blocked(reply,expected=None):
    return safe(reply) and reply.get('status')=='BLOCKED' and \
        reply.get('calculation') is None and reply.get('snapshotSha256') is None and \
        bool(reply.get('diagnostics')) and \
        (expected is None or expected in reply['diagnostics'])

def expected_estimate(reply):
    try:
        breakdown=reply['calculation']['breakdown']
        if not isinstance(breakdown,list) or not breakdown:return None
        if sum(int(s['days']) for s in breakdown)!=10:return None
        if any(s['yearDenominator'] not in (365,366) or
               not str(s['calendarDatasetEvidence']).startswith('CAL2:PERSIAN:')
               for s in breakdown):return None
        with localcontext() as ctx:
            ctx.prec=34
            frac=sum((Decimal(str(s['days']))/Decimal(str(s['yearDenominator']))
                     for s in breakdown),Decimal(0))
            return (Decimal('100000000')*Decimal('18')/Decimal('100')*frac).quantize(
                Decimal('1'),rounding=ROUND_HALF_UP)
    except (ValueError,TypeError,KeyError,ArithmeticError):return None

def run(base):
    checks=Checks()
    try:
        readiness=http(base,'GET','/readiness')
        if not isinstance(readiness,dict):raise RuntimeError('READINESS_NOT_OBJECT')
        checks.add('Oracle FEE3 reads and 11 tables',readiness.get('schema')=='FEE3' and
            readiness.get('readsEnabled') is True and readiness.get('missingTables')==[])
        checks.add('draft writes remain disabled',readiness.get('draftWritesEnabled') is False)
        counts={k:readiness.get(k) for k in ('calcItemCount','tariffVersionCount')}
        checks.add('Oracle item/version counts at accepted R4C1 baseline',
            counts=={'calcItemCount':297,'tariffVersionCount':300},counts)
        day=readiness.get('oracleBusinessDate');date.fromisoformat(day)
        workflow=http(base,'GET','/workflow/readiness')
        switches=('workflowEnabled','checkerDecisionEnabled','approvalEnabled','postingEnabled',
                  'authenticated','makerRole','checkerRole')
        no_workflow=isinstance(workflow,dict) and all(workflow.get(k) is False for k in switches)
        checks.add('workflow, maker/checker and posting denied',no_workflow)
        if readiness.get('readsEnabled') is not True or readiness.get('missingTables')!=[] or \
           readiness.get('draftWritesEnabled') is not False or not no_workflow or \
           counts!={'calcItemCount':297,'tariffVersionCount':300}:
            raise RuntimeError('FAIL_CLOSED_BEFORE_PREVIEW_POST')
        ops=http(base,'GET','/references/operations')
        valid_op=isinstance(ops,list) and any(isinstance(op,dict) and op.get('code')=='LOAN_PROFIT'
            and op.get('module')=='LOAN' and op.get('active') is True and
            'FINANCING_PROFIT' in op.get('allowedAmountKinds',[]) for op in ops)
        checks.add('authoritative active LOAN_PROFIT operation',valid_op)
        formulas=http(base,'GET','/references/formulas')
        required={x[1] for x in SPEC.values()}
        active={f.get('code') for f in formulas if isinstance(f,dict) and f.get('active') is True
            and f.get('method')=='FORMULA'} if isinstance(formulas,list) else set()
        checks.add('four authoritative exact loan formula codes active',required<=active,required-active)
        if not valid_op or not required<=active:raise RuntimeError('REFERENCE_PROOF_REQUIRED_BEFORE_POST')
        def post(req):return http(base,'POST','/r4/loans/models/preview',req)
        positive={
            'PARTICIPATION_REALIZED':'36000000',
            'DEBT_PURCHASE_SPREAD':'8000000',
            'SALAM_SPREAD':'15000000'
        }
        saved={}
        for p,value in positive.items():
            req=request(day,p);resp=post(req);saved[p]=(req,resp)
            checks.add('exact '+p+' gross amount',simulation(resp,p,value),
                resp.get('diagnostics',[]) if isinstance(resp,dict) else resp)
            checks.add('unpublished, unsaved, unverified '+p,
                safe(resp) and resp.get('versionReference',{}).get('status')=='UNSAVED' and
                resp.get('simulationMode')=='UNVERIFIED_UAT_DRAFT' and
                'CLIENT_CONTRACT_EVIDENCE_UNVERIFIED' in resp.get('diagnostics',[]) and
                bool(re.fullmatch('[a-f0-9]{64}',str(resp.get('snapshotSha256','')))))
        mud=request(day,'PARTICIPATION_REALIZED','MUDARABA')
        mud_resp=post(mud)
        checks.add('MUDARABA uses negotiated realized share not annual interest',
            simulation(mud_resp,'PARTICIPATION_REALIZED','36000000'))
        estimate_responses={}
        for p in ('PARTICIPATION_ESTIMATE','SALAM_ESTIMATE'):
            req=request(day,p);resp=post(req);estimate_responses[p]=(req,resp)
            expected=expected_estimate(resp) if isinstance(resp,dict) and resp.get('status')=='SIMULATED' else None
            checks.add('CAL2-backed 10 Jalali days '+p,
                expected is not None and simulation(resp,p,expected),
                resp.get('diagnostics',[]) if isinstance(resp,dict) else resp)
            checks.add('estimate explicitly NOT realized '+p,
                safe(resp) and 'NOT_REALIZED' in str(resp.get('resultKind','')) and
                resp.get('versionReference',{}).get('status')=='UNSAVED')
        replay=post(saved['PARTICIPATION_REALIZED'][0])
        checks.add('SHA256 deterministic replay',
            replay.get('snapshotSha256')==saved['PARTICIPATION_REALIZED'][1].get('snapshotSha256')
            and bool(re.fullmatch('[0-9a-f]{64}',str(replay.get('snapshotSha256','')))))
        def invalid(title,source,change,reason=None):
            body=copy.deepcopy(source);change(body);res=post(body)
            checks.add(title,blocked(res,reason),res.get('diagnostics',[]) if isinstance(res,dict) else res)
        real=saved['PARTICIPATION_REALIZED'][0]
        debt=saved['DEBT_PURCHASE_SPREAD'][0]
        salam=saved['SALAM_SPREAD'][0]
        est=estimate_responses['PARTICIPATION_ESTIMATE'][0]
        invalid('share greater than 100 rejected',real,
            lambda x:x['inputs'].update(sharePercent='100.00000001'),'REALIZED_PROFIT_OR_SHARE_INVALID')
        invalid('realization proof required',real,
            lambda x:x['inputs'].update(realizationStatementReference=''),'REALIZATION_EVIDENCE_REQUIRED')
        invalid('annual rate cannot replace realized share',real,
            lambda x:x['inputs'].update(contractRate='18'),'UNREVIEWED_INPUT_FIELDS')
        invalid('future event cannot be realized',real,
            lambda x:x['inputs'].update(eventDate=(date.fromisoformat(day)+timedelta(days=1)).isoformat()),
            'EVENT_NOT_AT_BANK_BUSINESS_DATE')
        invalid('debt negative spread not clipped to zero',debt,
            lambda x:x['inputs'].update(purchasePrice='101000000'),
            'NEGATIVE_SPREAD_NEEDS_LOSS_POLICY')
        invalid('debt purchase price evidence mandatory',debt,
            lambda x:x['inputs'].update(purchasePriceEvidenceReference=''),
            'DEBT_PURCHASE_EVIDENCE_REQUIRED')
        invalid('Salam negative spread not assumed zero',salam,
            lambda x:x['inputs'].update(saleAmount='89000000'),
            'NEGATIVE_SPREAD_NEEDS_LOSS_POLICY')
        invalid('Salam cannot self-certify tax',salam,
            lambda x:x['draft']['version'].update(taxMode='TAXABLE'),
            'TAX_AND_FINANCIAL_POLICY_OUT_OF_SCOPE')
        invalid('unverified yearFraction cannot be supplied',est,
            lambda x:x['inputs'].update(yearFraction='0.5'),'UNREVIEWED_INPUT_FIELDS')
        invalid('rate beyond NUMBER18_8 denied',est,
            lambda x:x['inputs'].update(contractRate='18.123456789'),'ESTIMATE_RATE_NUMBER_18_8')
        invalid('missing estimation basis evidence denied',est,
            lambda x:x['inputs'].update(estimateBasisReference=''),
            'ESTIMATE_CONTRACT_EVIDENCE_REQUIRED')
        invalid('customer cannot override formula text',est,
            lambda x:x['draft']['rules'][0]['parameters'].update(_fceEditableExpressionV1='amount*rate'),
            'CLIENT_FORMULA_OVERRIDE_FORBIDDEN')
        invalid('late damage formula cannot be repurposed',est,
            lambda x:x['draft']['rules'][0].update(formulaCode='LOAN_LATE_HOLIDAY'),
            'MODEL_FORMULA_MISMATCH')
        invalid('wrong contract/model forbidden',est,
            lambda x:x['draft']['version']['uiContext'].update(loanContractTypes=['DEBT_PURCHASE']),
            'CONTRACT_TYPE_MODEL_MISMATCH')
        invalid('partial Oracle reference cannot spoof approval',real,
            lambda x:x['draft'].update(calcItemId=824),'PARTIAL_ORACLE_REFERENCE')
        invalid('rule fixed tariff rate cannot override contract',est,
            lambda x:x['draft']['rules'][0].update(rate='18'),'UNSUPPORTED_MODEL_RULE')
        invalid('client waiver and tax blocked',est,
            lambda x:x['draft']['version']['financialPolicy'].update(taxPercent=0),
            'TAX_AND_FINANCIAL_POLICY_OUT_OF_SCOPE')
        invalid('event/model mismatch blocked',real,
            lambda x:x['draft']['version']['uiContext'].update(loanCalculationEvent='PERIOD_PROFIT'),
            'LOAN_MODEL_EVENT_MISMATCH_OR_PENDING')
        invalid('past effective date blocked',debt,
            lambda x:x.update(effectiveOn=(date.fromisoformat(day)-timedelta(days=1)).isoformat()),
            'HISTORICAL_EFFECTIVE_UNSUPPORTED')
        after=http(base,'GET','/readiness')
        after_counts={k:after.get(k) for k in counts}
        checks.add('Oracle catalog item/version counts unchanged',after_counts==counts,after_counts)
    except Exception as e:
        checks.add('qualification runtime',False,type(e).__name__+' '+str(e)[:200])
    return checks.report()

def main():
    parser=argparse.ArgumentParser()
    parser.add_argument('--base-url',default='http://127.0.0.1:8091')
    parser.add_argument('--report',default=str(REPORT))
    args=parser.parse_args()
    url=urlsplit(args.base_url)
    if url.scheme!='http' or url.hostname not in ('127.0.0.1','localhost') or \
       url.username or url.password or url.query or url.fragment or url.path not in ('','/') or \
       url.port!=8091:
        print('FEE3_R4C2_UNSAFE_DESTINATION_REJECTED');return 3
    result=run('http://127.0.0.1:8091')
    out=Path(args.report);out.parent.mkdir(parents=True,exist_ok=True)
    out.write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
    print(f'FEE3_R4C2_RUNTIME_PASS={result["passed"]} FAIL={result["failed"]}')
    print('REPORT='+str(out))
    return 0 if result['failed']==0 else 2
if __name__=='__main__':sys.exit(main())
