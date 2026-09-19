import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const migration=path.join(root,'database/oracle/dps2/migrations/0.10.0-phase10-opening-operational-v5-foundation.sql');
if(!fs.existsSync(migration)) throw new Error('Phase 10 migration is missing: '+migration);
const s=fs.readFileSync(migration,'utf8');
const checks=[
 ['request risk level','CUSTOMER_RISK_LEVEL_CODE'],
 ['request risk ref','RISK_ASSESSMENT_REFERENCE'],
 ['request expected activity','EXPECTED_ACTIVITY_REFERENCE'],
 ['joint basis','JOINT_ACCOUNT_BASIS_CODE'],
 ['activation status','ACTIVATION_STATUS_CODE'],
 ['activation deadline','ACTIVATION_DEADLINE_AT'],
 ['funding source party','SOURCE_PARTY_ID'],
 ['funding source account','SOURCE_ACCOUNT_ID'],
 ['funding purpose','FUNDING_PURPOSE_CODE'],
 ['funding ownership','SOURCE_OWNERSHIP_VERIFIED_FLAG'],
 ['check phase','CHECK_PHASE_CODE'],
 ['blocking scope','BLOCKING_SCOPE_CODE'],
 ['check required flag','REQUIRED_FLAG'],
 ['check recheck flag','RECHECK_REQUIRED_FLAG'],
 ['check validity','VALID_UNTIL'],
 ['batch basis','BULK_OPENING_BASIS_CODE'],
 ['batch legal basis','LEGAL_BASIS_REFERENCE'],
 ['obligation table','DEPOSIT_OPENING_OBLIGATION'],
 ['fund allocation table','DEPOSIT_OPENING_FUND_ALLOC'],
 ['account opened product','OPENED_PRODUCT_VERSION_ID'],
 ['account current product','CURRENT_PRODUCT_VERSION_ID'],
 ['account ownership','OWNERSHIP_TYPE_CODE'],
 ['account balances','LEDGER_BALANCE'],
 ['account debit capability','DEBIT_CAPABILITY_CODE'],
 ['PEP sanctions check','PEP_SANCTIONS'],
 ['SIAH readiness check','CBI_SIAH_REGISTRATION'],
 ['financial settlement check','FINANCIAL_SETTLEMENT'],
 ['final compliance recheck','FINAL_COMPLIANCE_RECHECK'],
 ['new funding purpose opening total','OPENING_TOTAL'],
 ['new funding purpose initial balance','INITIAL_BALANCE'],
 ['new funding purpose charges','CHARGES'],
 ['legacy check deactivation',"CHECK_CODE IN ('SANCTIONS','OPENING_RULES')"],
 ['no created account number persistence','CREATED_ACCOUNT_NO is intentionally not persisted'],
 ['success marker','SUCCESS: Phase 10 Operational Opening v5 schema foundation reconciled.']
];
let ok=0;
for(const [name,token] of checks){
 const pass=s.includes(token);
 console.log(`${pass?'OK ':'ERR'} ${name}`);
 if(pass) ok++;
}
if(ok!==checks.length) throw new Error(`Phase 10 v5 verifier failed: ${ok}/${checks.length}`);
console.log(`DPS2 Operational Opening v5 Phase 10 verification OK: ${ok}/${checks.length} schema-foundation checks passed.`);
