import json
from http.server import ThreadingHTTPServer, BaseHTTPRequestHandler
import subprocess
from threading import Thread
from pathlib import Path

CHECKER = Path(__file__).with_name('qualify.py')
COUNTS={'GET':0,'POST':0,'POST_PATHS':[]}
STATE={'draftWritesEnabled': False, 'workflowEnabled': False}

class Handler(BaseHTTPRequestHandler):
    def log_message(self, *args):
        pass
    def send(self, code, obj):
        data=json.dumps(obj).encode('utf-8')
        self.send_response(code)
        self.send_header('Content-Type','application/json')
        self.send_header('Content-Length',str(len(data)))
        self.end_headers()
        self.wfile.write(data)
    def do_GET(self):
        COUNTS['GET']+=1
        path=self.path.split('/api/v1/fee3',1)[-1]
        if path=='/readiness':
            return self.send(200,{'readsEnabled':True,'missingTables':[], 'draftWritesEnabled':STATE['draftWritesEnabled'],
                                  'oracleBusinessDate':'2026-10-27', 'itemCount':300, 'versionCount':300})
        if path.startswith('/items'):return self.send(200,{'rows':[], 'total':300,'page':0,'size':5})
        if path=='/references/operations':return self.send(200,[{'active':True,'allowedAmountKinds':['SERVICE_FEE'],'module':'DEPOSIT','code':'ACCOUNT_OPENING'}])
        if path in ('/references/formulas','/references/sources'):return self.send(200,[])
        if path=='/workflow/readiness':return self.send(200,{'workflowEnabled':STATE['workflowEnabled'],
            'checkerDecisionEnabled':False,'approvalEnabled':False, 'postingEnabled':False,
            'authenticated':False,'makerRole':False,'checkerRole':False})
        self.send(404,{})
    def do_POST(self):
        COUNTS['POST']+=1
        path=self.path.split('/api/v1/fee3',1)[-1]
        COUNTS['POST_PATHS'].append(path)
        payload=json.loads(self.rfile.read(int(self.headers.get('Content-Length','0'))))
        if path=='/validation':
            draft=payload
            ok = draft['version']['validFrom'] >= '2026-10-27' and draft['version']['feeCurrency']=='IRR'
            return self.send(200,{'submissionReady':ok,'approvalReady':False,'issues':[] if ok else [{'code':'NOT_READY'}]})
        if path=='/preview':
            formula=payload['draft']['version']['method']=='FORMULA'
            return self.send(200,{'status':'BLOCKED' if formula else 'SIMULATED',
                                   'totalIrr': None if formula else 12500,
                                   'persisted':False,'financialPosting':False})
        self.send(400,{'notAllowed':path})

server=ThreadingHTTPServer(('127.0.0.1',0), Handler)
worker=Thread(target=server.serve_forever,daemon=True)
worker.start()
base=f'http://127.0.0.1:{server.server_port}'
try:
    good=subprocess.run(['python',str(CHECKER),'--mode','runtime','--base-url',base],capture_output=True,text=True,timeout=30)
    print(good.stdout)
    assert good.returncode==0,(good.stdout,good.stderr)
    assert COUNTS['POST']==5,COUNTS
    assert set(COUNTS['POST_PATHS'])=={'/validation','/preview'},COUNTS
    print('QUALIFICATION_MOCK_HAPPY_PASS=1')
    COUNTS['POST']=0;COUNTS['POST_PATHS']=[]
    STATE['draftWritesEnabled']=True
    blocked=subprocess.run(['python',str(CHECKER),'--mode','runtime','--base-url',base],capture_output=True,text=True,timeout=30)
    assert blocked.returncode!=0, blocked.stdout
    assert COUNTS['POST']==0,COUNTS
    print('QUALIFICATION_MOCK_WRITEFLAG_FAIL_CLOSED_PASS=1')
    STATE['draftWritesEnabled']=False
    STATE['workflowEnabled']=True
    blocked=subprocess.run(['python',str(CHECKER),'--mode','runtime','--base-url',base],capture_output=True,text=True,timeout=30)
    assert blocked.returncode!=0, blocked.stdout
    assert COUNTS['POST']==0,COUNTS
    print('QUALIFICATION_MOCK_WORKFLOW_FAIL_CLOSED_PASS=1')
    bad=subprocess.run(['python',str(CHECKER),'--mode','runtime','--base-url','https://example.com:443'],capture_output=True,text=True,timeout=30)
    assert bad.returncode!=0, bad.stdout
    print('QUALIFICATION_MOCK_REMOTE_NETWORK_BLOCKED_PASS=1')
finally:
    server.shutdown()
