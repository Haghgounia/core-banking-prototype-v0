#!/usr/bin/env node
// Read-only source contract + dependency-free Java policy test.
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { spawnSync } from 'node:child_process';
const root=process.cwd();
function read(file) { return fs.readFileSync(path.join(root,file),'utf8'); }
const source='backend/src/main/java/com/behsazan/corebanking/';
const service=read(source+'posting/cash/CashPostingService.java');
const dps=read(source+'deposit/account/transaction/application/DepositTransactionService.java');
const policy=read(source+'posting/cash/CashPostingPolicy.java');
const bridge=read(source+'posting/cash/DepositCashPostingBridge.java');
const controller=read(source+'posting/cash/CashPostingJournalReadController.java');
const checks=[
 ['explicit off-by-default',service.includes('havingValue="true"')&&service.includes('core-banking.posting.cash')],
 ['existing Oracle transaction mandatory',service.includes('Propagation.MANDATORY')],
 ['cash posting invoked before DPS2 status POSTED',dps.indexOf('cashPostingBridge.record(')>0 && dps.indexOf('cashPostingBridge.record(')<dps.indexOf('repository.transition(txId,t.recordVersion(),"AUTHORIZED","POSTED"')],
 ['no DPS2 regression when disabled',dps.includes('cashPostingBridge.isEnabled()')],
 ['banking reversal protection',dps.includes('forbids reversals until a complete GL reversal path')],
 ['Oracle PST persisted',service.includes('INSERT INTO PST.POSTING_INSTRUCTIONS')&&service.includes('INSERT INTO PST.POSTING_EXECUTIONS')],
 ['Formance-style LGR persisted',service.includes('INSERT INTO LGR.LEDGER_MOVEMENTS')&&service.includes('POSITION_BALANCES')],
 ['GL journal persisted',service.includes('INSERT INTO ACC.ACCOUNTING_TRANSACTIONS')&&service.includes('INSERT INTO ACC.ACCOUNTING_ENTRIES')],
 ['GL fiscal period guarded',service.includes("PERIOD_STATE='OPEN'")],
 ['published mapping and role scope',service.includes('ACC.ACCOUNT_ROLE_ASSIGNMENTS')&&service.includes("PUBLICATION_STATE='PUBLISHED'")],
 ['journal read access token',controller.includes('MessageDigest.isEqual')&&controller.includes('requiredToken.length<16')],
 ['no accounting through Kafka consumer',!service.includes('KafkaListener')&&!bridge.includes('KafkaListener')],
 ['balanced policy',policy.includes('Unbalanced journal')&&policy.includes('CASH_DEPOSIT')&&policy.includes('CASH_WITHDRAWAL')]
];
let failed=false;
for (const [name,ok] of checks) {console.log((ok?'PASS':'FAIL')+' '+name);if(!ok)failed=true;}
const tmp=fs.mkdtempSync(path.join(os.tmpdir(),'posting-r1d5-'));
try {
 const files=[source+'posting/cash/CashPostingPolicy.java','backend/src/test/java/com/behsazan/corebanking/posting/cash/CashPostingPolicySelfTest.java'].map(f=>path.join(root,f));
 const compile=spawnSync('javac',['-encoding','UTF-8','-d',tmp,...files],{encoding:'utf8'});
 if(compile.status!==0){console.error('FAIL javac policy test',compile.stderr||compile.error);failed=true;}
 else {
  const result=spawnSync('java',['-cp',tmp,'com.behsazan.corebanking.posting.cash.CashPostingPolicySelfTest'],{encoding:'utf8'});
  console.log(result.stdout.trim()); if(result.status!==0){console.error(result.stderr);failed=true;}
 }
} finally {fs.rmSync(tmp,{recursive:true,force:true});}
console.log(failed?'POSTING_R1D5_STATIC_STATUS=FAIL':'POSTING_R1D5_STATIC_STATUS=PASS');
process.exitCode=failed?1:0;
