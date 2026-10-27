#!/usr/bin/env python3
"""Source-only R4-A certification of additive patch, strict Java compile, no Oracle access."""
from __future__ import annotations
import hashlib
import json
import os
import re
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT=Path(__file__).resolve().parents[3]
MANIFEST=ROOT/'tools/fee3-backend/releases/r4a/manifest.json'
R4=ROOT/'backend/src/main/java/com/behsazan/corebanking/fee3/r4'
PASS=FAIL=0

def check(label,good):
    global PASS,FAIL
    if good:PASS+=1
    else:FAIL+=1
    print(('PASS' if good else 'FAIL')+' | '+label,flush=True)
    return good

def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()

def launch(args,cwd=None):
    p=subprocess.run(args,cwd=cwd or ROOT,stdout=subprocess.PIPE,stderr=subprocess.PIPE,
                     text=True,encoding='utf-8',errors='replace',timeout=120)
    if p.returncode:
        print((p.stdout+'\n'+p.stderr)[-1700:],flush=True)
    return p

def main():
    try:
        manifest=json.loads(MANIFEST.read_text(encoding='utf-8'))
        check('release marker',manifest.get('patchId')=='FEE3-BE-R4A-20261027')
        for rel,expected in {**manifest['anchors'],**manifest['files']}.items():
            p=ROOT/rel
            check('sha256 '+rel,p.is_file() and sha(p)==expected)
        sources=list(R4.glob('*.java'))
        check('exact R4 Java source count',len(sources)==5)
        text='\n'.join(p.read_text(encoding='utf-8') for p in sources)
        check('no mutating SQL in new R4 source',not re.search(r'\b(?:INSERT\s+INTO|UPDATE\s+FCE_|DELETE\s+FROM|MERGE\s+INTO|CREATE\s+TABLE|DROP\s+TABLE)\b',text,re.I))
        check('R4 endpoint is independently namespaced','/api/v1/fee3/r4' in text)
        check('read only transaction on R4 service','@Transactional(readOnly=true)' in (R4/'Fee3R4Service.java').read_text(encoding='utf-8'))
        check('strict persisted draft reference check',all(x in text for x in
              ('ORACLE_SCOPE_DIFFERS_FROM_REQUEST','ORACLE_RULE_DIFFERS_FROM_REQUEST',
               'ORACLE_FINANCIAL_POLICY_DIFFERS_FROM_REQUEST','STALE_ORACLE_DRAFT_REFERENCE')))
        check('rounding and precision contract',all(x in text for x in
              ('IRR_MONEY_EXCEEDS_NUMBER_22_2','RATE_EXCEEDS_NUMBER_18_8','COVERAGE_EXCEEDS_NUMBER_12_4')))
        check('no posting/approval flags in response', 'boolean financialPosting, boolean published, boolean approved' in text)
        with tempfile.TemporaryDirectory(prefix='fee3-r4a-source-') as temp:
            base=ROOT/'backend/src/main/java/com/behsazan/corebanking/fee3'
            pure=[base/'domain/Fee3Models.java',base/'r3/Fee3PreviewEngine.java',
                  R4/'Fee3R4CalculationContract.java',
                  ROOT/'tools/fee3-backend/r4a/Fee3R4ContractProbe.java']
            cmd=['javac','-encoding','UTF-8','-d',temp]+[str(x) for x in pure]
            result=launch(cmd)
            if check('actual pure Java source compiles',result.returncode==0):
                result=launch(['java','-cp',temp,'Fee3R4ContractProbe'])
                check('51 deterministic arithmetic/governance tests',
                      result.returncode==0 and 'FEE3_R4A_CONTRACT_PROBE_PASS=51 FAIL=0' in result.stdout)
        print('FEE3_R4A_SOURCE_PASS='+str(PASS)+' FAIL='+str(FAIL),flush=True)
        return 0 if FAIL==0 else 2
    except (OSError,ValueError,subprocess.TimeoutExpired,KeyError) as error:
        print('FEE3_R4A_SOURCE_FAIL='+str(type(error).__name__)+': '+str(error),file=sys.stderr)
        return 3

if __name__=='__main__':sys.exit(main())
