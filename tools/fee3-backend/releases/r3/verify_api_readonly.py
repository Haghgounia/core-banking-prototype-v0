"""Strictly non-mutating FEE3 R3 HTTP smoke: no draft insert, no workflow transition.
Runs against a running developer backend. POST requests target validation/preview only.
"""
import json
import sys
from urllib.request import Request, urlopen
from urllib.error import HTTPError, URLError

BASE = (sys.argv[1] if len(sys.argv) > 1 else 'http://127.0.0.1:8091').rstrip('/')

def call(path, payload=None):
    data = None if payload is None else json.dumps(payload, ensure_ascii=False).encode('utf-8')
    request = Request(BASE + path, data=data, headers={'Accept': 'application/json',
        'Content-Type': 'application/json'} if data is not None else {'Accept': 'application/json'})
    with urlopen(request, timeout=12) as response:
        return json.loads(response.read().decode('utf-8'))

def check(value, label):
    if not value:
        raise RuntimeError('FEE3_R3_HTTP_FAILED: ' + label)

try:
    readiness = call('/api/v1/fee3/readiness')
    check(readiness.get('readsEnabled') is True, 'Oracle reads disabled')
    check(readiness.get('missingTables') == [], 'Oracle tables missing')
    today = readiness.get('oracleBusinessDate')
    check(isinstance(today, str) and len(today) == 10, 'Oracle date missing')
    operations = call('/api/v1/fee3/references/operations')
    choice = next((o for o in operations if o.get('active') and
        'SERVICE_FEE' in o.get('allowedAmountKinds', [])), None)
    check(choice is not None, 'No compatible active operation in Oracle')
    draft = {
        'calcItemId': None, 'tariffVersionId': None,
        'expectedItemRecordVersion': None, 'expectedVersionRecordVersion': None,
        'item': {'title': 'HTTP UAT simulation', 'module': choice['module'],
                 'operation': choice['code'], 'amountKind': 'SERVICE_FEE',
                 'variant': None, 'ownerDomain': 'TARIFF', 'description': None},
        'version': {'validFrom': today, 'validTo': None, 'origin': 'BANK_INTERNAL',
                    'sourceId': None, 'method': 'FIXED', 'taxMode': 'EXEMPT',
                    'rounding': 'HALF_UP', 'changeReason': 'R3 no-write test',
                    'financialPolicy': {}, 'serviceCurrency': 'IRR', 'feeCurrency': 'IRR',
                    'uiContext': {}},
        'scope': {'channels': ['ALL'], 'customers': ['ALL'], 'groups': ['ALL'],
                  'products': ['ALL'], 'contracts': [], 'currency': 'IRR'},
        'rules': [{'order': 1, 'method': 'FIXED', 'base': 'REQUEST_AMOUNT',
                   'rateSource': 'RULE', 'rate': None, 'fixed': 12500,
                   'collateral': None, 'coverage': None, 'min': None, 'max': None,
                   'formulaCode': None, 'parameters': {}}],
        'tests': [{'title': 'Example', 'request': {'amount': 1000000}, 'expected': 12500}]
    }
    report = call('/api/v1/fee3/validation', draft)
    check(report.get('submissionReady') is True, 'Draft policy validation rejected fixture')
    check(report.get('approvalReady') is False, 'Approval improperly assumed')
    result = call('/api/v1/fee3/preview', {'draft': draft, 'inputs': {'amount': 1000000}})
    check(result.get('status') == 'SIMULATED', 'Simple preview not simulated')
    check(str(result.get('totalIrr')) in ('12500', '12500.0', '12500.00'), 'Wrong preview amount')
    check(result.get('persisted') is False and result.get('financialPosting') is False,
          'Preview produced financial side effect claim')
    workflow = call('/api/v1/fee3/workflow/readiness')
    check(workflow.get('postingEnabled') is False, 'Posting gate violated')
    check(workflow.get('authenticated') is False, 'Unauthenticated smoke should not be an actor')
    print('FEE3_R3_HTTP_READONLY_PASS: Oracle date=' + today + '; preview=12500 IRR; no writes')
except (HTTPError, URLError, ValueError, RuntimeError, KeyError) as error:
    print('FEE3_R3_HTTP_READONLY_FAIL:', error, file=sys.stderr)
    sys.exit(1)
