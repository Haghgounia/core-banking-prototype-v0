#!/usr/bin/env python3
"""Fail-closed R4B1 source/artifact integrity checks: no Oracle operations are performed."""
from pathlib import Path
import hashlib,json,re,sys
ROOT=Path(__file__).resolve().parents[3]
MANIFEST=ROOT/'tools/fee3-backend/releases/r4b1/manifest.json'
checks=[]

def test(label,okay):
    checks.append(bool(okay));print(('PASS' if okay else 'FAIL')+' | '+label,flush=True)

def sha(file):return hashlib.sha256(file.read_bytes()).hexdigest()

def main():
    if not MANIFEST.is_file():
        print('FEE3_R4B1_SOURCE_FAIL | manifest missing');return 2
    m=json.loads(MANIFEST.read_text(encoding='utf8'))
    test('manifest contract and readOnly',m.get('patchId')=='FEE3-BE-R4B1-20261027' and
         m.get('schema')==1 and m.get('readOnlySimulation') is True and m.get('databaseChanges') is False)
    for name,expected in m.get('anchors',{}).items():
        file=ROOT/name;test('baseline SHA256 '+name,file.is_file() and sha(file)==expected)
    for name,expected in m.get('files',{}).items():
        file=ROOT/name;test('release SHA256 '+name,file.is_file() and sha(file)==expected)
    base=ROOT/'backend/src/main/java/com/behsazan/corebanking/fee3/r4b1'
    classes=list(base.glob('*.java'))
    test('only four new production Java units',len(classes)==4)
    controller=(base/'Fee3R4B1Controller.java').read_text(encoding='utf8')
    service=(base/'Fee3R4B1Service.java').read_text(encoding='utf8')
    contract=(base/'Fee3R4B1ServiceContract.java').read_text(encoding='utf8')
    harness=(ROOT/'tools/fee3-backend/r4b1/runtime-readonly.py').read_text(encoding='utf8')
    test('only isolated R4B1 preview HTTP endpoint',controller.count('@PostMapping')==1 and
         '"/services/preview"' in controller and not any(x in controller.lower() for x in
         ('@putmapping','@deletemapping','@patchmapping')))
    test('read-only transaction and R3 preflight', '@Transactional(readOnly=true)' in service and
         'validation.preflight(request.draft())' in service and 'reference.resolve(request.draft())' in service)
    test('no write primitives in new Java units',not any(re.search(p,'\n'.join(x.read_text(encoding='utf8') for x in classes),re.I)
         for p in (r'\bINSERT\s+INTO\b',r'\bUPDATE\s+FCE_',r'\bDELETE\s+FROM\b',
                   r'\bMERGE\s+INTO\b',r'\bCOMMIT\b',r'@Transactional\s*\(\s*\)')))
    test('BigDecimal arithmetic and distinct bracket/progressive branches',
         'BigDecimal' in contract and '"BRACKET".equals(method)' in contract and
         '"PROGRESSIVE"' in contract and 'RoundingMode' in contract)
    test('source-verified reference reuse', 'Fee3R4CalculationContract.fingerprint' in contract and
         'Fee3R4ReferenceRepository' in service)
    test('all approval and posting flags denied',all(x in contract for x in (
         '"UNPUBLISHED_DRAFT_ONLY"','"NO_ACCOUNTING_POSTING"','"NO_ORACLE_WRITE"')))
    test('HTTP only local socket with proxy disabled', 'ProxyHandler({})' in harness and
         "u.hostname!='127.0.0.1'" in harness)
    test('allowlisted read-only HTTP methods',"POST={'/r4/services/preview'}" in harness and
         '"drafts"' not in harness)
    test('untested financial adjustments not used', 'FINANCIAL_ADJUSTMENTS_NOT_CERTIFIED' in contract and
         'SERVICE_FEE_ONLY' in contract)
    print(f'FEE3_R4B1_SOURCE_PASS={sum(checks)} FAIL={len(checks)-sum(checks)}')
    return 0 if all(checks) else 2

if __name__=='__main__':sys.exit(main())
