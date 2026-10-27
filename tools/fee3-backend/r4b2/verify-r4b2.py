#!/usr/bin/env python3
"""Source SHA-256 and safety invariants, not a full Maven build or Oracle runtime test."""
from pathlib import Path
import hashlib,json,re,sys
ROOT=Path(__file__).resolve().parents[3]
MANIFEST=ROOT/'tools/fee3-backend/releases/r4b2/manifest.json'
checks=[]
def chk(title,result):
    checks.append(bool(result));print(('PASS' if result else 'FAIL')+' | '+title,flush=True)
def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()
def main():
    if not MANIFEST.is_file():
        print('FEE3_R4B2_SOURCE_FAIL | manifest missing');return 2
    m=json.loads(MANIFEST.read_text(encoding='utf8'))
    chk('release marker, no database change',m.get('patchId')=='FEE3-BE-R4B2-20261027' and
        m.get('schema')==1 and m.get('readOnlySimulation') is True and m.get('databaseChanges') is False)
    for name,expected in m.get('anchors',{}).items():
        path=ROOT/name;chk('source anchor '+name,path.is_file() and sha(path)==expected)
    for name,expected in m.get('files',{}).items():
        path=ROOT/name;chk('new file '+name,path.is_file() and sha(path)==expected)
    units=ROOT/'backend/src/main/java/com/behsazan/corebanking/fee3/r4b2'
    java=list(units.glob('*.java'));joined='\n'.join(p.read_text(encoding='utf8') for p in java)
    ctrl=(units/'Fee3R4B2Controller.java').read_text(encoding='utf8')
    service=(units/'Fee3R4B2Service.java').read_text(encoding='utf8')
    core=(units/'Fee3R4B2GuaranteeContract.java').read_text(encoding='utf8')
    harness=(ROOT/'tools/fee3-backend/r4b2/runtime-readonly.py').read_text(encoding='utf8')
    chk('four additive-only Spring Java classes',len(java)==4)
    chk('only one isolated preview endpoint','"/guarantees/preview"' in ctrl and
        ctrl.count('@PostMapping')==1 and not any(x in ctrl.lower() for x in
        ('@putmapping','@deletemapping','@patchmapping')))
    chk('read-only transactional service and Oracle proof', '@Transactional(readOnly=true)' in service and
        'reference.resolve(draft)' in service and 'oracle.activeOperation(' in service)
    chk('generic governance checks retained, narrow specialized PERCENT exception',
        'Fee3DraftPolicy.check(draft,day' in service and
        'Fee3SubmissionPolicy.check(draft)' in service and 'RULE_RATE_REQUIRED' in service and
        'portfolio&&"PERCENT"' in service)
    chk('no mutation or accounting primitives in new classes',not any(
        re.search(pattern,joined,re.I) for pattern in (r'\bINSERT\s+INTO\b',r'\bUPDATE\s+FCE_',
            r'\bDELETE\s+FROM\b',r'\bMERGE\s+INTO\b',r'@Transactional\s*\(\s*\)',
            r'\bCOMMIT\b')))
    chk('exact-decimal, separate allocated/covered basis and day count',all(
        tag in core for tag in ('BigDecimal','GUARANTEE_PORTFOLIO_ALLOCATED',
        'GUARANTEE_PORTFOLIO_COLLATERAL_VALUE','ACT_ACT','ACT_365','ACT_360',
        'CASH_100_COVERAGE_BELOW_100','PORTFOLIO_SHARES_MUST_TOTAL_100')))
    chk('scope explicitly issuance only','"GUARANTEE_ISSUE".equals(i.operation())' in core and
        '"GUARANTEE_FEE".equals(i.amountKind())' in core)
    chk('explicit no-approval, no posting and hash proof', all(
        t in core for t in ('"UNPUBLISHED_DRAFT_ONLY"','"NO_ACCOUNTING_POSTING"',
        '"NO_ORACLE_WRITE"','Fee3R4CalculationContract.fingerprint')))
    chk('direct HTTP, no proxy no redirect',all(t in harness for t in
        ('ProxyHandler({})','NoRedirect','u.hostname!=\'127.0.0.1\'')))
    chk('only preview POST method allowed',"POST={'/r4/guarantees/preview'}" in harness and
        "GET={'/readiness','/workflow/readiness','/references/operations'}" in harness)
    chk('release does not modify prior R3, R4A, R4B1 classes',all('r4b2/' in name or
        name.startswith(('docs/install/FEE3-BE-R4B2','tools/fee3-backend/r4b2/'))
        for name in m.get('files',{})))
    result=sum(checks)
    print(f'FEE3_R4B2_SOURCE_PASS={result} FAIL={len(checks)-result}')
    return 0 if all(checks) else 2
if __name__=='__main__':sys.exit(main())
