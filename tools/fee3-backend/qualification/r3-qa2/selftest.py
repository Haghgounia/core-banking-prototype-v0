#!/usr/bin/env python3
"""QA2 regression: only a local stub; no external service, Oracle, or application writes."""
from __future__ import annotations
import json
import os
from http.server import ThreadingHTTPServer, BaseHTTPRequestHandler
from pathlib import Path
import socket
import subprocess
import sys
from threading import Thread

CHECKER = Path(__file__).with_name('qualify.py')
COUNTS = {'GET': 0, 'POST': 0, 'POST_PATHS': [], 'PROXY': 0, 'READINESS': 0}
STATE = {'draftWritesEnabled': False, 'workflowEnabled': False, 'redirect': False,
         'disconnect': False, 'http_error': False, 'drift': False, 'missing_counts': False,
         'fail_preview': False}

class Handler(BaseHTTPRequestHandler):
    def log_message(self, *args):
        pass

    def send(self, status, value):
        raw = json.dumps(value, ensure_ascii=False).encode('utf-8')
        self.send_response(status)
        self.send_header('Content-Type', 'application/json; charset=UTF-8')
        self.send_header('Content-Length', str(len(raw)))
        self.end_headers()
        self.wfile.write(raw)

    def do_GET(self):
        COUNTS['GET'] += 1
        path = self.path.removeprefix('/api/v1/fee3')
        if path == '/readiness':
            COUNTS['READINESS'] += 1
            if STATE['disconnect']:
                self.close_connection = True
                try: self.connection.shutdown(socket.SHUT_RDWR)
                except OSError: pass
                self.connection.close()
                return
            if STATE['redirect']:
                self.send_response(302)
                self.send_header('Location', 'https://example.test/credentials')
                self.send_header('Content-Length', '0')
                self.end_headers()
                return
            if STATE['http_error']:
                return self.send(503, {'errorCode':'FEE3_NOT_READY', 'detail':'mock server unavailable'})
            response = {'readsEnabled': True, 'missingTables': [],
                        'draftWritesEnabled': STATE['draftWritesEnabled'],
                        'oracleBusinessDate': '2026-10-27',
                        'calcItemCount': 297 + (1 if STATE['drift'] and COUNTS['READINESS'] > 1 else 0),
                        'tariffVersionCount': 300}
            if STATE['missing_counts']:
                response.pop('calcItemCount')
            return self.send(200, response)
        if path.startswith('/items?'):
            return self.send(200, {'rows':[], 'total':297, 'page':0, 'size':5})
        if path == '/references/operations':
            return self.send(200, [{'active':True,'allowedAmountKinds':['SERVICE_FEE'],
                                    'module':'GENERAL','code':'CHECKBOOK_ISSUE'}])
        if path in ('/references/formulas', '/references/sources'):
            return self.send(200, [])
        if path == '/workflow/readiness':
            return self.send(200, {'workflowEnabled': STATE['workflowEnabled'],
                        'checkerDecisionEnabled': False, 'approvalEnabled': False,
                        'postingEnabled': False, 'authenticated': False,
                        'makerRole':False, 'checkerRole':False})
        return self.send(404, {'errorCode':'NOT_FOUND'})

    def do_POST(self):
        COUNTS['POST'] += 1
        path = self.path.removeprefix('/api/v1/fee3')
        COUNTS['POST_PATHS'].append(path)
        payload = json.loads(self.rfile.read(int(self.headers.get('Content-Length','0'))))
        if path == '/validation':
            ok = (payload['version']['validFrom'] >= '2026-10-27' and
                  payload['version']['feeCurrency'] == 'IRR')
            return self.send(200, {'submissionReady':ok,'approvalReady':False,
                                   'issues':[] if ok else [{'code':'NOT_READY'}]})
        if path == '/preview':
            if STATE['fail_preview']:
                return self.send(422, {'errorCode':'INVALID_PREVIEW','detail':'mock fault'})
            formula = payload['draft']['version']['method'] == 'FORMULA'
            return self.send(200, {'status':'BLOCKED' if formula else 'SIMULATED',
                                   'totalIrr':None if formula else 12500,
                                   'persisted':False,'financialPosting':False})
        return self.send(403, {'errorCode':'NON_READONLY_REQUEST_FORBIDDEN'})

class Proxy(BaseHTTPRequestHandler):
    def log_message(self,*args): pass
    def do_GET(self):
        COUNTS['PROXY'] += 1
        self.send_response(502)
        self.end_headers()
    def do_POST(self):
        COUNTS['PROXY'] += 1
        self.send_response(502)
        self.end_headers()

backend = ThreadingHTTPServer(('127.0.0.1',0), Handler)
proxy = ThreadingHTTPServer(('127.0.0.1',0), Proxy)
Thread(target=backend.serve_forever, daemon=True).start()
Thread(target=proxy.serve_forever, daemon=True).start()
base = f'http://127.0.0.1:{backend.server_port}'
assert base.startswith('http://127.0.0.1:')


def call(url=base, force_proxy=False):
    env = dict(os.environ)
    if force_proxy:
        addr = f'http://127.0.0.1:{proxy.server_port}'
        env.update(HTTP_PROXY=addr, http_proxy=addr, HTTPS_PROXY=addr, https_proxy=addr,
                   NO_PROXY='', no_proxy='')
    return subprocess.run([sys.executable, str(CHECKER), '--mode','runtime','--base-url',url],
                          capture_output=True, text=True, encoding='utf-8', errors='replace',
                          env=env, timeout=40)

def reset():
    for key in STATE:
        STATE[key] = False
    COUNTS['GET'] = COUNTS['POST'] = COUNTS['READINESS'] = 0
    COUNTS['POST_PATHS'] = []
    COUNTS['PROXY'] = 0

passed = 0
try:
    reset()
    good = call(force_proxy=True)
    assert good.returncode == 0, good.stdout + good.stderr
    assert COUNTS['POST'] == 5, COUNTS
    assert set(COUNTS['POST_PATHS']) == {'/validation','/preview'}, COUNTS
    assert COUNTS['PROXY'] == 0, COUNTS
    assert 'Real Oracle item/version counts unchanged' in good.stdout, good.stdout
    passed += 1; print('PASS | QA2 direct HTTP, proxy ignored, full no-write happy path')

    reset(); STATE['draftWritesEnabled'] = True
    bad = call()
    assert bad.returncode and COUNTS['POST'] == 0, (bad.stdout, COUNTS)
    passed += 1; print('PASS | QA2 writes enabled blocks POST')

    reset(); STATE['workflowEnabled'] = True
    bad = call()
    assert bad.returncode and COUNTS['POST'] == 0, (bad.stdout, COUNTS)
    passed += 1; print('PASS | QA2 workflow enabled blocks POST')

    reset(); bad = call('https://example.test:443')
    assert bad.returncode and COUNTS['GET'] == 0, bad.stdout
    passed += 1; print('PASS | QA2 remote destination rejected before network')

    reset(); STATE['redirect'] = True
    bad = call()
    assert bad.returncode and 'redirect blocked' in bad.stdout and COUNTS['POST'] == 0, bad.stdout
    passed += 1; print('PASS | QA2 redirect to remote destination blocked')

    reset(); STATE['disconnect'] = True
    bad = call()
    assert bad.returncode and 'GET /readiness' in bad.stdout and 'RemoteDisconnected' in bad.stdout, bad.stdout
    passed += 1; print('PASS | QA2 disconnected HTTP identified by exact route')

    reset(); STATE['http_error'] = True
    bad = call()
    assert bad.returncode and 'HTTP 503 errorCode=FEE3_NOT_READY' in bad.stdout, bad.stdout
    passed += 1; print('PASS | QA2 server-side HTTP error status and safe errorCode surfaced')

    reset(); STATE['drift'] = True
    bad = call()
    assert bad.returncode and 'Real Oracle item/version counts unchanged' in bad.stdout and 'FAIL' in bad.stdout, bad.stdout
    assert COUNTS['POST'] == 5, COUNTS
    passed += 1; print('PASS | QA2 real readiness count drift rejected')

    reset(); STATE['missing_counts'] = True
    bad = call()
    assert bad.returncode and COUNTS['POST'] == 0 and 'Initial Oracle counts present' in bad.stdout, bad.stdout
    passed += 1; print('PASS | QA2 missing Oracle count fields blocks POST')

    reset(); STATE['fail_preview'] = True
    bad = call()
    assert bad.returncode and 'HTTP_STEP_FAIL POST /preview' in bad.stdout and 'INVALID_PREVIEW' in bad.stdout, bad.stdout
    assert COUNTS['POST'] == 2, COUNTS
    passed += 1; print('PASS | QA2 preview error includes route/status/code; no extra POST')

    assert all(x in {'/preview', '/validation'} for x in COUNTS['POST_PATHS'])
    passed += 1; print('PASS | QA2 no write/approval/posting endpoints present in test traffic')
finally:
    backend.shutdown(); proxy.shutdown()
print('FEE3_R3_QA2_SELFTEST_PASS=' + str(passed) + ' FAIL=0')
