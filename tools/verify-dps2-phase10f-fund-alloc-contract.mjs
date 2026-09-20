import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const p=path.join(root,'database/oracle/dps2/migrations/0.10.0-phase10f-fund-alloc-column-reconciliation.sql');
if(!fs.existsSync(p)) throw new Error('Missing FUND_ALLOC reconciliation migration: '+p);
const s=fs.readFileSync(p,'utf8');
const tokens=[
 'DEPOSIT_OPENING_FUND_ALLOC',
 "add_required('OPENING_FUND_ALLOC_ID'",
 "add_required('OPENING_FUNDING_ID'",
 "add_required('OPENING_OBLIGATION_ID'",
 "add_required('ALLOCATED_AMOUNT'",
 "NUMBER(19,4)",
 "add_required('ALLOCATION_STATUS_CODE'",
 "add_optional('SETTLEMENT_REFERENCE'",
 "add_required_default('CREATED_AT'",
 "SYSTIMESTAMP",
 "add_required('CREATED_BY'",
 "add_optional('UPDATED_AT'",
 "add_optional('UPDATED_BY'",
 "add_required_default('RECORD_VERSION'",
 "Cannot safely add required column",
 "SUCCESS: Phase 10F FUND_ALLOC column contract reconciled."
];
let pass=0;
for(const t of tokens){const ok=s.includes(t); console.log(`${ok?'OK ':'ERR'} ${t}`); if(ok)pass++;}
if(pass!==tokens.length) throw new Error(`FUND_ALLOC verifier failed: ${pass}/${tokens.length}`);
console.log(`DPS2 Phase 10F FUND_ALLOC contract verification: ${pass}/${tokens.length} passed.`);
