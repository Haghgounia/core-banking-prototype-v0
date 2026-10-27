#!/usr/bin/env python3
"""No-network reproduction: native JSON decimal from a Java BigDecimal must not lose precision."""
from __future__ import annotations
import importlib.util
import json
import sys
from decimal import Decimal, ROUND_HALF_UP, localcontext
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[3]
RUN = ROOT / 'tools/fee3-backend/r4c3/runtime-readonly.py'
S = importlib.util.spec_from_file_location('fee3_r4c3_hf1_runtime', RUN)
rt = importlib.util.module_from_spec(S)
S.loader.exec_module(rt)
passed = 0
failed = 0

def check(name, good):
    global passed, failed
    if good:
        passed += 1
    else:
        failed += 1
    print(('PASS' if good else 'FAIL') + ' | ' + name)

with localcontext() as ctx:
    ctx.prec = 34
    factor = Decimal(10) / Decimal(365)
    amount = Decimal('100000000')
    rate = Decimal('4')
    raw = (amount * rate / Decimal(100) * factor)
    rounded = raw.quantize(Decimal(1), rounding=ROUND_HALF_UP)

# The native API serializes BigDecimal as JSON numbers, unlike the original HTTP mock
# which passed strings and hid binary-float loss.
reply = {
    'status': 'SIMULATED', 'approved': False, 'published': False,
    'persisted': False, 'financialPosting': False,
    'simulationMode': 'UNVERIFIED_UAT_DRAFT', 'timingModel': 'FIRST_INSTALLMENT_OF_PERIOD',
    'versionReference': {'status': 'UNSAVED'}, 'snapshotSha256': 'a' * 64,
    'diagnostics': ['LOAN_SCHEDULE_UNVERIFIED', 'PREVIOUS_CHARGE_HISTORY_NOT_CHECKED',
        'NO_DUPLICATE_CERTIFICATION', 'NOT_A_CHARGE_AUTHORIZATION',
        'NO_ORACLE_WRITE', 'NO_ACCOUNTING_POSTING'],
    'calculation': {
        'status': 'SIMULATED', 'timingModel': 'FIRST_INSTALLMENT_OF_PERIOD',
        'resultKind': 'QARD_SERVICE_FEE_NOT_DEMANDED',
        'duplicateCheckStatus': 'NOT_VERIFIED_NO_AUTHORITATIVE_CHARGE_HISTORY',
        'candidateAssessmentKey': 'b'*64,
        'eventJalaliYear': 1405, 'eventJalaliMonth': 8, 'eventJalaliDay': 5,
        'eventJalaliOrdinal': 221,
        'eventCalendarDatasetEvidence': 'CAL2:PERSIAN:101:201',
        'breakdown': [{'days': 10, 'yearDenominator': 365,
                       'calendarDatasetEvidence': 'CAL2:PERSIAN:101:201'}],
        'yearFraction': 'REPLACE_WITH_NATIVE_DECIMAL',
        'rawIrr': 'REPLACE_WITH_NATIVE_DECIMAL',
        'roundedIrr': int(rounded)
    }
}
raw_json = json.dumps(reply, separators=(',', ':')).replace(
    '"REPLACE_WITH_NATIVE_DECIMAL"', str(factor), 1).replace(
    '"REPLACE_WITH_NATIVE_DECIMAL"', str(raw), 1)
check('native decimal serialized as JSON numeric tokens',
    ('"yearFraction":' + str(factor)) in raw_json and
    ('"rawIrr":' + str(raw)) in raw_json)
lossy = json.loads(raw_json)
check('reproduced old binary-float false negative',
    not rt.simulated(lossy, 'FIRST_INSTALLMENT_OF_PERIOD'))
precise = json.loads(raw_json, parse_float=Decimal)
check('Decimal parser preserves all fraction digits',
    precise['calculation']['yearFraction'] == factor)
check('preserved precision validates genuine 10-day amount',
    rt.simulated(precise, 'FIRST_INSTALLMENT_OF_PERIOD'))

class FakeReply:
    status = 200
    headers = {'Content-Type': 'application/json'}
    def __enter__(self): return self
    def __exit__(self, *unused): return False
    def read(self, size): return raw_json.encode('utf8')
class NoNetworkClient:
    def open(self, request, timeout):
        check('nonmutating preview endpoint used',
              request.get_method() == 'POST' and request.full_url.endswith('/api/v1/fee3/r4/qard/preview'))
        return FakeReply()
rt.CLIENT = NoNetworkClient()
through_http = rt.http('http://127.0.0.1:8091', 'POST', '/r4/qard/preview', {'draft': 'test'})
check('actual HTTP JSON reader retains BigDecimal',
    isinstance(through_http['calculation']['yearFraction'], Decimal))
check('actual HTTP JSON reader passes positive financial assertions',
    rt.simulated(through_http, 'FIRST_INSTALLMENT_OF_PERIOD'))
corrupt = json.loads(raw_json, parse_float=Decimal)
corrupt['calculation']['yearFraction'] += Decimal('0.000000000000001')
check('incorrect numeric result still fails closed',
    not rt.simulated(corrupt, 'FIRST_INSTALLMENT_OF_PERIOD'))
try:
    rt.http('http://127.0.0.1:8091', 'POST', '/r4/qard/collect', {})
except RuntimeError:
    check('payment collection POST still forbidden', True)
else:
    check('payment collection POST still forbidden', False)
try:
    rt.http('http://example.com:8091', 'GET', '/readiness')
except RuntimeError:
    check('remote HTTP destination still forbidden', True)
else:
    check('remote HTTP destination still forbidden', False)
print(f'FEE3_R4C3_HF1_SELFTEST_PASS={passed} FAIL={failed}')
sys.exit(0 if failed == 0 else 2)
