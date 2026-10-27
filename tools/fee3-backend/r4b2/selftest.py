#!/usr/bin/env python3
"""Independent decimal mock of R4B2 harness & network safety.
This checks *the qualification harness only*. Actual Spring/Oracle acceptance is separate.
"""
from __future__ import annotations
import contextlib,hashlib,importlib.util,io,json,os,sys,tempfile,threading
from datetime import date
from decimal import Decimal,ROUND_HALF_UP
from http.server import BaseHTTPRequestHandler,ThreadingHTTPServer
from pathlib import Path

HERE=Path(__file__).resolve().parent
spec=importlib.util.spec_from_file_location('fee3r4b2_readonly',HERE/'runtime-readonly.py')
app=importlib.util.module_from_spec(spec);spec.loader.exec_module(app)
DAY='2026-10-27'
def D(x):return Decimal(str(x))
def reject(code):return {'status':'BLOCKED','diagnostics':[code]}

def simulation(req):
    d=req['draft'];v=d['version'];i=d['item'];r=d['rules'][0]
    p=v['uiContext']['guaranteePolicy'];inp=req['inputs']
    if i['operation']!='GUARANTEE_ISSUE':return reject('GUARANTEE_ISSUE_ONLY')
    if v['method'] not in ('DAILY','PERCENT'):return reject('GUARANTEE_METHOD_NOT_CERTIFIED')
    if d['calcItemId'] is not None and d['tariffVersionId'] is None:return reject('PARTIAL_ORACLE_REFERENCE')
    if set(v['financialPolicy'])-{'taxPercent'}:return reject('FINANCIAL_MODIFIERS_UNREVIEWED')
    if D(inp['amount']).as_tuple().exponent < -2:return reject('POSITIVE_GUARANTEE_AMOUNT_REQUIRED')
    rules=p['portfolioRules']; bytype={t['collateralType']:t for t in rules}
    for t in rules:
        if t['collateralType']=='CASH_100' and D(t['coveragePercent'])<100:return reject('CASH_100_COVERAGE_BELOW_100')
        if t['rateSource']=='CONFIG' and t['ratePercent'] is None:return reject('TARIFF_RATE_INVALID')
    normal={'amount','portfolio'}
    if v['method']=='DAILY':normal|={'periodStart','periodEndExclusive'}
    if any(t['rateSource']=='REQUEST' for t in rules):normal.add('contractRate')
    if set(inp)!=normal:return reject('PORTFOLIO_INPUT_FIELDS_UNREVIEWED')
    if sum(D(r['sharePercent']) for r in inp['portfolio'])!=100:return reject('PORTFOLIO_SHARES_MUST_TOTAL_100')
    amount=D(inp['amount']);raw=Decimal(0);trace=[]
    for part in inp['portfolio']:
        t=bytype[part['collateralType']];share=D(part['sharePercent']);coverage=D(t['coveragePercent'])
        allocated=amount*share/100;face=allocated*coverage/100
        basis=allocated if p['feeBasis']=='GUARANTEE_PORTFOLIO_ALLOCATED' else face
        rate=Decimal(0) if t['rateSource']=='EXEMPT' else D(
            t['ratePercent'] if t['rateSource']=='CONFIG' else inp['contractRate'])
        # Tests only send a full Gregorian future calendar year in DAILY.
        factor=Decimal(1)
        contribution=basis*rate/100*factor;raw+=contribution
        trace.append({'collateralType':t['collateralType'],'rawFeeIrr':str(contribution)})
    if r.get('min') is not None:raw=max(raw,D(r['min']))
    if r.get('max') is not None:raw=min(raw,D(r['max']))
    fee=raw.quantize(Decimal('1'),rounding=ROUND_HALF_UP)
    tax=(fee*D(v['financialPolicy']['taxPercent'])/100).quantize(Decimal('1'),rounding=ROUND_HALF_UP) if v['taxMode']=='TAXABLE' else Decimal(0)
    serial=json.dumps(req,sort_keys=True,ensure_ascii=True)
    return {'contractVersion':'FEE3_GUARANTEE_R4B2_V1','status':'SIMULATED','persisted':False,
        'financialPosting':False,'published':False,'approved':False,'roundingScale':0,
        'oracleBusinessDate':DAY,'snapshotSha256':hashlib.sha256(serial.encode()).hexdigest(),
        'versionReference':{'provenance':'UNPERSISTED_CLIENT_DRAFT'},
        'calculation':{'status':'SIMULATED','persisted':False,'financialPosting':False,
            'feeIrr':str(fee),'taxIrr':str(tax),'totalIrr':str(fee+tax),'breakdown':trace},
        'diagnostics':[]}

class Handler(BaseHTTPRequestHandler):
    def log_message(self,*args):pass
    def reply(self,data,status=200):
        payload=json.dumps(data,ensure_ascii=True).encode()
        self.send_response(status);self.send_header('Content-Type','application/json')
        self.send_header('Content-Length',str(len(payload)));self.end_headers();self.wfile.write(payload)
    def do_GET(self):
        self.server.gets.append(self.path)
        if self.server.redirect:
            self.send_response(302);self.send_header('Location','https://example.org');self.end_headers();return
        if self.path.endswith('/readiness') and not self.path.endswith('/workflow/readiness'):
            self.reply({'schema':'FEE3','readsEnabled':True,'draftWritesEnabled':self.server.writes,
                'missingTables':[],'calcItemCount':297,'tariffVersionCount':300,'oracleBusinessDate':DAY})
        elif self.path.endswith('/workflow/readiness'):
            state=dict.fromkeys(('workflowEnabled','checkerDecisionEnabled','approvalEnabled',
                'postingEnabled','authenticated','makerRole','checkerRole'),False)
            state['workflowEnabled']=self.server.workflow;self.reply(state)
        elif self.path.endswith('/references/operations'):
            self.reply([{'module':'GUARANTEE','code':'GUARANTEE_ISSUE',
                'active':True,'allowedAmountKinds':['GUARANTEE_FEE']}])
        else:self.reply({'error':'DENIED'},403)
    def do_POST(self):
        self.server.posts.append(self.path)
        data=json.loads(self.rfile.read(int(self.headers.get('Content-Length','0'))))
        if self.path.endswith('/r4/guarantees/preview'):self.reply(simulation(data))
        else:self.reply({'error':'DENIED'},403)

class Stub(ThreadingHTTPServer):
    def __init__(self):
        super().__init__(('127.0.0.1',0),Handler)
        self.gets=[];self.posts=[];self.writes=False;self.redirect=False;self.workflow=False

def exercise(server,overrides=None):
    with tempfile.TemporaryDirectory(prefix='fee3-r4b2-http-') as work:
        root=app.ROOT;report=app.REPORT;old=sys.argv
        app.ROOT=Path(work);app.REPORT=Path(work)/'runtime.json'
        sys.argv=['runtime-readonly.py','--base-url',overrides or f'http://127.0.0.1:{server.server_port}']
        try:
            with contextlib.redirect_stdout(io.StringIO()) as captured:ret=app.main()
            result=json.loads(app.REPORT.read_text(encoding='utf8'))
        finally:
            app.ROOT=root;app.REPORT=report;sys.argv=old
        return ret,result,captured.getvalue()

def main():
    checks=[]
    def check(name,pass_):
        checks.append(bool(pass_));print(('PASS' if pass_ else 'FAIL')+' | '+name,flush=True)
    srv=Stub();thread=threading.Thread(target=srv.serve_forever,daemon=True);thread.start()
    try:
        os.environ['HTTP_PROXY']='http://127.0.0.1:1'
        ret,report,log=exercise(srv)
        check('independent mock happy path passes harness',ret==0 and report['failed']==0)
        check('harness only POSTs preview URL',bool(srv.posts) and set(srv.posts)=={'/api/v1/fee3/r4/guarantees/preview'})
        check('GET requests all explicitly allowed',set(srv.gets)<=set('/api/v1/fee3'+x for x in app.GET))
        check('proxy bypass and deterministic golden answers', 'portfolio covered basis 15/50/35 is 1700000' in log)
        check('negative cases exercised','missing tariff rate denied' in log and 'invalid share total denied' in log)
        check('no posting or write implied in result',report['financialPosting'] is False and report['noWrites'] is True)
        count=len(srv.posts)
        try:app.http('http://127.0.0.1:'+str(srv.server_port),'POST','/drafts',{})
        except RuntimeError:check('draft write endpoint forbidden by allowlist',len(srv.posts)==count)
        else:check('draft write endpoint forbidden by allowlist',False)
        srv.posts.clear();srv.writes=True
        ret,_,_=exercise(srv);check('Oracle write-enable flag prevents any POST',ret!=0 and srv.posts==[])
        srv.writes=False;srv.workflow=True;srv.posts.clear()
        ret,_,_=exercise(srv);check('workflow flag prevents POST',ret!=0 and srv.posts==[])
        srv.workflow=False;srv.redirect=True;srv.posts.clear()
        ret,_,_=exercise(srv);check('redirect to remote blocked',ret!=0 and srv.posts==[])
        srv.redirect=False;srv.posts.clear()
        ret,_,_=exercise(srv,'http://example.org:8091')
        check('remote destination rejected',ret!=0 and srv.posts==[])
    finally:
        srv.shutdown();srv.server_close();thread.join(timeout=2)
        os.environ.pop('HTTP_PROXY',None)
    print(f'FEE3_R4B2_HARNESS_SELFTEST_PASS={sum(checks)} FAIL={len(checks)-sum(checks)}')
    return 0 if all(checks) else 2

if __name__=='__main__':sys.exit(main())
