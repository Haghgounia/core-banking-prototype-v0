#!/usr/bin/env python3
"""Local HTTP mock validates harness sequencing, safety, no-proxy and failure gates.
The mock is NOT a substitute for a compiled Spring/Oracle runtime acceptance test.
"""
from __future__ import annotations
import contextlib,hashlib,importlib.util,io,json,os,sys,tempfile,threading
from decimal import Decimal,ROUND_HALF_UP,ROUND_HALF_EVEN,ROUND_FLOOR,ROUND_CEILING
from http.server import ThreadingHTTPServer,BaseHTTPRequestHandler
from pathlib import Path

HERE=Path(__file__).resolve().parent
spec=importlib.util.spec_from_file_location('fee3r4b1_tested_runtime',HERE/'runtime-readonly.py')
app=importlib.util.module_from_spec(spec);spec.loader.exec_module(app)
DAY='2026-10-27'

def d(x):return Decimal(str(x))
def blocked(reason):return {'status':'BLOCKED','diagnostics':[reason]}
def preview(req):
    v=req['draft']['version'];rule=req['draft']['rules'][0];cfg=rule['parameters']['originalConfig']
    amount=req['inputs'].get('amount');day=req.get('effectiveOn')
    if v['method'] not in ('BRACKET','PROGRESSIVE'):return blocked('SERVICE_METHOD_NOT_IN_R4B1')
    if day and day<DAY:return blocked('HISTORICAL_SIMULATION_NOT_ALLOWED')
    if req['draft']['calcItemId'] is not None:return blocked('PARTIAL_ORACLE_REFERENCE')
    if set(v['financialPolicy'])-{'taxPercent'}:return blocked('FINANCIAL_ADJUSTMENTS_NOT_CERTIFIED')
    if amount is None or d(amount)<=0 or -d(amount).as_tuple().exponent>2:
        return blocked('EXPLICIT_POSITIVE_IRR_AMOUNT_REQUIRED')
    if v['method']=='PROGRESSIVE':
        rows=cfg['tiers'];last=d(0);raw=d(0);trace=[]
        for n,row in enumerate(rows):
            upper=d(row['upTo']) if row['upTo'] is not None else None
            rate=d(row['ratePercent']);fixed=d(row['fixedAmount'])
            if -rate.as_tuple().exponent>8:return blocked('TIER_RATE_OR_FIXED_INVALID')
            if upper is not None and upper<=last:return blocked('TIER_UPPER_BOUND_INVALID')
            part=min(d(amount),upper if upper is not None else d(amount))-last
            if part>0:
                raw+=part*rate/d(100)+fixed
                trace.append({'chargedBasisIrr':str(part)})
            if upper is None or d(amount)<=upper:break
            last=upper
    else:
        rows=cfg['brackets'];last=d(0);trace=[]
        for row in rows:
            if d(row['from'])!=last:return blocked('BRACKET_GAP_OR_OVERLAP')
            if row['to'] is not None:last=d(row['to'])
        matched=[r for r in rows if d(amount)>=d(r['from']) and (r['to'] is None or d(amount)<d(r['to']))]
        if len(matched)!=1:return blocked('BRACKET_GAP_OR_OVERLAP')
        r=matched[0];raw=d(amount)*d(r['ratePercent'])/d(100)+d(r['fixedAmount']);trace=[{}]
    if rule.get('max') is not None:raw=min(raw,d(rule['max']))
    if rule.get('min') is not None:raw=max(raw,d(rule['min']))
    rounding={'HALF_UP':ROUND_HALF_UP,'HALF_EVEN':ROUND_HALF_EVEN,
              'FLOOR':ROUND_FLOOR,'DOWN':ROUND_FLOOR,'UP':ROUND_CEILING,'CEIL':ROUND_CEILING}[v['rounding']]
    fee=raw.quantize(d(1),rounding=rounding)
    tax=(fee*d(v['financialPolicy']['taxPercent'])/d(100)).quantize(d(1),rounding=rounding) if v['taxMode']=='TAXABLE' else d(0)
    content=json.dumps(req,sort_keys=True,ensure_ascii=True)
    return {'contractVersion':'FEE3_SERVICE_FEE_R4B1_V1','status':'SIMULATED','persisted':False,
        'financialPosting':False,'approved':False,'published':False,'simulationMode':'UAT_DRAFT_SIMULATION',
        'versionReference':{'provenance':'UNPERSISTED_CLIENT_DRAFT'},
        'oracleBusinessDate':DAY,'effectiveOn':day or DAY,'roundingScale':0,
        'snapshotSha256':hashlib.sha256(content.encode()).hexdigest(),
        'calculation':{'status':'SIMULATED','persisted':False,'financialPosting':False,
            'feeIrr':str(fee),'taxIrr':str(tax),'totalIrr':str(fee+tax),'breakdown':trace},
        'diagnostics':[]}

class Handler(BaseHTTPRequestHandler):
    def log_message(self,*args):pass
    def out(self,data,code=200,extra=None):
        payload=json.dumps(data).encode();self.send_response(code)
        self.send_header('Content-Type','application/json');self.send_header('Content-Length',str(len(payload)))
        if extra:self.send_header(*extra)
        self.end_headers();self.wfile.write(payload)
    def do_GET(self):
        self.server.gets.append(self.path)
        if self.server.redirect and self.path.endswith('/readiness'):
            self.send_response(302);self.send_header('Location','https://example.org/');self.end_headers();return
        if self.path.endswith('/readiness') and not self.path.endswith('/workflow/readiness'):
            self.out({'schema':'FEE3','readsEnabled':True,'draftWritesEnabled':self.server.writes,
                'missingTables':[],'calcItemCount':297,'tariffVersionCount':300,'oracleBusinessDate':DAY})
        elif self.path.endswith('/workflow/readiness'):
            self.out(dict.fromkeys(['workflowEnabled','checkerDecisionEnabled','approvalEnabled',
                 'postingEnabled','authenticated','makerRole','checkerRole'],False))
        elif self.path.endswith('/references/operations'):
            self.out([{'code':'TRANSFER','module':'PAYMENT','active':True,'allowedAmountKinds':['SERVICE_FEE']}])
        else:self.out({'error':'NOT_FOUND'},404)
    def do_POST(self):
        self.server.posts.append(self.path)
        length=int(self.headers.get('Content-Length','0'))
        request=json.loads(self.rfile.read(length).decode('utf8'))
        if self.path.endswith('/r4/services/preview'):self.out(preview(request))
        else:self.out({'error':'WRITE_NOT_ALLOWED'},403)

class Stub(ThreadingHTTPServer):
    def __init__(self):
        super().__init__(('127.0.0.1',0),Handler)
        self.posts=[];self.gets=[];self.writes=False;self.redirect=False

def exercise(server):
    with tempfile.TemporaryDirectory(prefix='fee3-r4b1-test-') as work:
        original_root=app.ROOT;app.ROOT=Path(work)
        app.REPORT=Path(work)/'test.json'
        prior=sys.argv;sys.argv=['runtime-readonly.py','--base-url',f'http://127.0.0.1:{server.server_port}']
        try:
            with contextlib.redirect_stdout(io.StringIO()) as captured:result=app.main()
        finally:sys.argv=prior;app.ROOT=original_root
        return result,app.REPORT.read_text(),captured.getvalue()

def main():
    checks=[]
    def ok(name,condition):
        checks.append(bool(condition))
        print(('PASS' if condition else 'FAIL')+' | '+name)
    server=Stub();thread=threading.Thread(target=server.serve_forever,daemon=True)
    thread.start()
    try:
        os.environ['http_proxy']='http://127.0.0.1:1'
        os.environ['HTTP_PROXY']='http://127.0.0.1:1'
        ret,report,output=exercise(server)
        data=json.loads(report)
        ok('all harness cases pass with direct socket, proxy ignored',ret==0 and data['failed']==0)
        ok('harness no financial posting asserted',data['financialPosting'] is False and data['noWrites'] is True)
        ok('only preview endpoint called with POST',bool(server.posts) and set(server.posts)=={'/api/v1/fee3/r4/services/preview'})
        ok('GET allowlist restricts all calls',set(server.gets)<=set('/api/v1/fee3'+x for x in app.GET))
        ok('harness executes golden progressive', 'progressive source golden 30M = 20K' in output)
        ok('harness executes golden bracket', 'bracket exact boundary 10M = 8K' in output)
        ok('harness tests false waiver', 'unapproved waiver forbidden' in output)
        ok('partial Oracle version blocked', 'partial Oracle reference forbidden' in output)
        ok('historic date blocked', 'historical effective date rejected' in output)
        ok('catalog counts compared before/after', 'Oracle catalog counts unchanged' in output)
        ok('unapproved POST /drafts refused before network',guard_http_forbidden(server))
        server.posts.clear();server.writes=True
        ret,report,_=exercise(server)
        ok('write flag prevents ALL preview POSTs',ret!=0 and server.posts==[])
        server.writes=False;server.redirect=True;server.posts.clear()
        ret,report,_=exercise(server)
        ok('redirect forbidden before POST',ret!=0 and server.posts==[])
        server.redirect=False;server.posts.clear()
        with tempfile.TemporaryDirectory() as d:
            original_root=app.ROOT;app.ROOT=Path(d)
            app.REPORT=Path(d)/'blocked.json'
            old=sys.argv;sys.argv=['runtime-readonly.py','--base-url','http://example.org:80']
            try:
                with contextlib.redirect_stdout(io.StringIO()):ret=app.main()
            finally:sys.argv=old;app.ROOT=original_root
        ok('remote host forbidden before POST',ret!=0 and server.posts==[])
    finally:
        server.shutdown();server.server_close();thread.join(timeout=2)
        os.environ.pop('http_proxy',None);os.environ.pop('HTTP_PROXY',None)
    print(f'FEE3_R4B1_HARNESS_SELFTEST_PASS={sum(checks)} FAIL={len(checks)-sum(checks)}')
    return 0 if all(checks) else 2

def guard_http_forbidden(s):
    size=len(s.posts)
    try:app.http('http://127.0.0.1:'+str(s.server_port),'POST','/drafts',{})
    except RuntimeError:return len(s.posts)==size
    return False

if __name__=='__main__':sys.exit(main())
