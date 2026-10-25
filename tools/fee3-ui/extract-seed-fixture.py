#!/usr/bin/env python3
"""Offline FEE3 UI fixture derivation from the reviewed V13.8.84 seed scripts.
This script DOES NOT touch Oracle. Usage:
 python tools/fee3-ui/extract-seed-fixture.py /path/to/full_seed.sql /path/to/reference_seed.sql
"""
import json, re, sys
from pathlib import Path

if len(sys.argv) != 3:
    raise SystemExit('Usage: extract-seed-fixture.py FULL_SEED_SQL REFERENCE_SEED_SQL')
full = Path(sys.argv[1]).read_text(encoding='utf-8')
reference = Path(sys.argv[2]).read_text(encoding='utf-8')


def split_csv(s):
    tokens = []; start = 0; depth = 0; quoted = False; i = 0
    while i < len(s):
        c = s[i]
        if c == "'":
            if quoted and i+1 < len(s) and s[i+1] == "'": i += 2; continue
            quoted = not quoted
        elif not quoted:
            if c == '(': depth += 1
            elif c == ')': depth -= 1
            elif c == ',' and depth == 0:
                tokens.append(s[start:i].strip()); start = i+1
        i += 1
    tokens.append(s[start:].strip())
    return tokens


def paired(s, start):
    assert s[start] == '('
    depth = 0; quoted = False; i = start
    while i < len(s):
        c = s[i]
        if c == "'":
            if quoted and i + 1 < len(s) and s[i + 1] == "'": i += 2; continue
            quoted = not quoted
        elif not quoted:
            if c == '(': depth += 1
            elif c == ')':
                depth -= 1
                if depth == 0: return s[start + 1:i], i + 1
        i += 1
    raise ValueError('Unclosed parenthesis')


def concat_parts(s):
    pieces=[];last=0;depth=0;quoted=False;i=0
    while i<len(s):
        if s[i]=="'":
            if quoted and i+1<len(s) and s[i+1]=="'":i+=2;continue
            quoted=not quoted
        elif not quoted:
            if s[i]=='(':depth+=1
            elif s[i]==')':depth-=1
            elif depth==0 and s[i:i+2]=='||':
                pieces.append(s[last:i]);i+=2;last=i;continue
        i+=1
    pieces.append(s[last:]);return pieces


def literal(text):
    value = text.strip()
    chunks=concat_parts(value)
    if len(chunks)>1: return ''.join(literal(part) for part in chunks)
    if value == 'NULL': return None
    if value in ('SYSTIMESTAMP', 'SYSDATE'): return None
    if value.upper().startswith('TO_CLOB('): return literal(paired(value,value.index('('))[0])
    if value.upper().startswith('TO_DATE('): return literal(split_csv(paired(value,value.index('('))[0])[0])
    if value.startswith("N'") and value.endswith("'"): return value[2:-1].replace("''", "'")
    if value.startswith("'") and value.endswith("'"): return value[1:-1].replace("''", "'")
    if re.fullmatch(r'-?\d+(?:\.\d+)?', value): return float(value) if '.' in value else int(value)
    # Scalar subselects are replaced by natural business keys in the UI fixture.
    if value.startswith('(') or value.startswith('s.'): return None
    raise ValueError('Unrecognized SQL expression: ' + value[:120])


def insert_payload(line):
    m = re.search(r'\bINSERT(?:\s+INTO\s+FEE3\.\w+)?\s*\(',line)
    if not m: raise ValueError('missing insert: ' + line[:60])
    columns, end = paired(line,m.end()-1)
    # MERGE: VALUES(...);  INSERT INTO SELECT: SELECT ... FROM dual WHERE ...
    v = re.search(r'\bVALUES\s*\(',line[end:])
    if v:
        start = end+v.end()-1; vals,_ = paired(line,start)
    else:
        pos = line.find(' SELECT ',end)
        if pos < 0: raise ValueError('missing SELECT: ' + line[:60])
        expression = line[pos+8:]
        # only the SQL field list, stopping at the top-level FROM dual
        depth=0; quoted=False; stop=None;i=0
        while i < len(expression):
            c=expression[i]
            if c=="'":
                if quoted and i+1<len(expression) and expression[i+1]=="'": i+=2;continue
                quoted=not quoted
            elif not quoted:
                if c=='(': depth+=1
                elif c==')': depth-=1
                elif depth==0 and expression[i:i+10].upper()==' FROM DUAL': stop=i;break
            i+=1
        if stop is None: raise ValueError('missing FROM dual: '+line[:80])
        vals=expression[:stop]
    keys=[x.strip().upper() for x in split_csv(columns)]
    raw=split_csv(vals)
    if len(keys)!=len(raw):raise ValueError(f'Columns/values mismatch {len(keys)} {len(raw)} {line[:90]}')
    return {k:literal(v) for k,v in zip(keys,raw)}


def records(text,table):
    for line in text.splitlines():
        if line.startswith(f'MERGE INTO FEE3.{table} ') or line.startswith(f'INSERT INTO FEE3.{table} '):
            yield line, insert_payload(line)


def js(v):return json.dumps(v,ensure_ascii=False,separators=(',',':'))

def from_json(v,fallback):
    if not v:return fallback
    return json.loads(v)

items={}
for line,r in records(full,'FCE_CALC_ITEM'):
    row={'code':r['CALC_CODE'],'title':r['TITLE_FA'],'module':r['MODULE_CODE'],'operation':r['OPERATION_CODE'],
        'amountKind':r['AMOUNT_KIND_CODE'],'variant':r['SERVICE_VARIANT_CODE'] or '', 'ownerDomain':r['OWNER_DOMAIN_CODE'],
        'enabled':bool(r['IS_ENABLED']),'description':r['DESCRIPTION_FA'] or '', 'versions':[]}
    assert row['code'] not in items
    items[row['code']] = row
versions={}
for line,r in records(full,'FCE_TARIFF_VERSION'):
    code = re.search(r"CALC_CODE='(FCE-\d+)'",line).group(1)
    policy=from_json(r['FINANCIAL_POLICY_JSON'],{})
    entry={'no':r['VERSION_NO'],'from':r['VALID_FROM'],'to':r['VALID_TO'] or '',
        'status':r['STATUS_CODE'],'active':bool(r['IS_ACTIVE']),'origin':r['TARIFF_ORIGIN_CODE'],
        'method':r['BASE_METHOD_CODE'],'taxMode':r['TAX_MODE_CODE'],'rounding':r['ROUNDING_CODE'],
        'sourceCode':re.search(r"DOCUMENT_NO='([^']+)'",line).group(1) if 'DOCUMENT_NO=' in line else '',
        'reason':r['CHANGE_REASON'] or '', 'financialPolicy':policy,'rules':[],'scopes':[],'tests':[]}
    versions[(code,entry['no'])]=entry
    items[code]['versions'].append(entry)
for line,r in records(full,'FCE_CALC_RULE'):
    code=re.search(r"CALC_CODE='(FCE-\d+)'",line).group(1)
    no=int(re.search(r'AND tv.VERSION_NO=(\d+)',line).group(1))
    params=from_json(r['PARAMETERS_JSON'],{})
    versions[(code,no)]['rules'].append({'order':r['RULE_ORDER'],'method':r['METHOD_CODE'],
        'base':r['AMOUNT_BASE_CODE'] or '', 'rateSource':r['RATE_SOURCE_CODE'],'rate':r['RATE_PERCENT'],
        'fixed':r['FIXED_AMOUNT_IRR'],'collateral':r['COLLATERAL_TYPE_CODE'] or '',
        'coverage':r['COVERAGE_PERCENT'],'min':r['MIN_FEE_IRR'],'max':r['MAX_FEE_IRR'],
        'formula':r['FORMULA_CODE'] or '', 'parameters':params})
for line,r in records(full,'FCE_VERSION_SCOPE'):
    code=re.search(r"CALC_CODE='(FCE-\d+)'",line).group(1)
    no=int(re.search(r'AND tv.VERSION_NO=(\d+)',line).group(1))
    versions[(code,no)]['scopes'].append({'channels':from_json(r['CHANNEL_CODES_JSON'],[]),
        'customers':from_json(r['CUSTOMER_TYPES_JSON'],[]),'groups':from_json(r['CUSTOMER_GROUPS_JSON'],[]),
        'products':from_json(r['PRODUCT_CODES_JSON'],[]),'contracts':from_json(r['CONTRACT_TYPES_JSON'],[]),
        'currency':r['CURRENCY_CODE'],'priority':r['PRIORITY_NO']})
for line,r in records(full,'FCE_TEST_CASE'):
    code=re.search(r"CALC_CODE='(FCE-\d+)'",line).group(1)
    no=int(re.search(r'AND tv.VERSION_NO=(\d+)',line).group(1))
    versions[(code,no)]['tests'].append({'title':r['TEST_TITLE_FA'],'request':from_json(r['REQUEST_JSON'],{}),
        'expected':r['EXPECTED_IRR'],'result':r['LAST_RESULT_IRR'],'status':r['LAST_STATUS_CODE']})

operations=[]
for _,r in records(reference,'FCE_REF_OPERATION'):
    operations.append({'code':r['OPERATION_CODE'],'module':r['MODULE_CODE'],'title':r['OPERATION_NAME_FA'],
        'kinds':from_json(r['ALLOWED_AMOUNT_KINDS_JSON'],[]),'active':bool(r['IS_ACTIVE'])})
formulas=[]
for _,r in records(reference,'FCE_REF_FORMULA'):
    formulas.append({'code':r['FORMULA_CODE'],'title':r['FORMULA_NAME_FA'],'method':r['METHOD_CODE'],
        'expression':r['EXPRESSION_TEXT'],'inputs':from_json(r['INPUT_SCHEMA_JSON'],{}),
        'eligible':from_json(r['ELIGIBLE_CONTEXT_JSON'],{}),'active':bool(r['IS_ACTIVE'])})
sources=[]
for _,r in records(reference,'FCE_REF_SOURCE'):
    sources.append({'code':r['DOCUMENT_NO'] or r['SOURCE_TYPE_CODE'],'title':r['DOCUMENT_TITLE_FA'],
        'type':r['SOURCE_TYPE_CODE'],'date':r['DOCUMENT_DATE'] or '',
        'validation':r['VALIDATION_STATUS'],'notes':r['NOTES_FA'] or ''})
codes=[]
for _,r in records(reference,'FCE_REF_CODE'):
    codes.append({'group':r['CODE_GROUP'],'code':r['CODE_VALUE'],'label':r['LABEL_FA'],'active':bool(r['IS_ACTIVE'])})
assert (len(items),len(versions),sum(len(x['rules']) for x in versions.values()),sum(len(x['scopes']) for x in versions.values()),len(operations),len(formulas),len(sources),len(codes)) == (297,300,313,300,113,33,5,167), 'Seed count mismatch'

out=Path(__file__).resolve().parents[2]/'frontend/src/app/features/fee3/fee3-seed.data.ts'
out.write_text('''import type {Fee3Item,Fee3Operation,Fee3Formula,Fee3Source,Fee3ReferenceCode} from './fee3.models';
/* Generated strictly from FCE V13.8.84 supplied Oracle Seed SQL.\n * OFFLINE design fixture, NOT a live Oracle query or regulatory approval.\n * Regenerate using tools/fee3-ui/extract-seed-fixture.py.\n */\n'''+ '\n'.join(f'export const {key}: readonly {type_name}[] = {js(data)};' for key,type_name,data in [
    ('FEE3_SEED_ITEMS','Fee3Item',list(items.values())),('FEE3_SEED_OPERATIONS','Fee3Operation',operations),('FEE3_SEED_FORMULAS','Fee3Formula',formulas),('FEE3_SEED_SOURCES','Fee3Source',sources),('FEE3_SEED_CODES','Fee3ReferenceCode',codes)
])+'\n',encoding='utf-8')
print('Offline fixture generated:',out,'bytes',out.stat().st_size)
print('items',len(items),'versions',len(versions),'rules',sum(len(x['rules']) for x in versions.values()),'scopes',sum(len(x['scopes']) for x in versions.values()),'tests',sum(len(x['tests']) for x in versions.values()),'ops',len(operations),'formulas',len(formulas),'sources',len(sources))
