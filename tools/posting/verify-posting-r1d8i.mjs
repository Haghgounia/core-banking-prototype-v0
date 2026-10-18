#!/usr/bin/env node
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import {spawnSync} from 'node:child_process';
const root=process.cwd();
const read = rel => fs.readFileSync(path.join(root,rel),'utf8');
const dir='backend/src/main/java/com/behsazan/corebanking/';
const service=read(dir+'posting/cash/CashPostingService.java');
const dps=read(dir+'deposit/account/transaction/application/DepositTransactionService.java');
const bridge=read(dir+'posting/cash/DepositCashPostingBridge.java');
const guard=read(dir+'posting/cash/DevOnlyCashPostingGuard.java');
const controller=read(dir+'posting/cash/CashPostingJournalReadController.java');
const checks=[
 ['default mode remains PUBLISHED_ONLY',service.includes('cash.mode:PUBLISHED_ONLY')],
 ['cash posting still explicitly off-by-default',service.includes('havingValue="true"')],
 ['PUBLISHED path still present',service.includes("PUBLICATION_STATE='PUBLISHED'")&&service.includes('maker-checker approval for template')],
 ['DEV_ONLY selects ONLY DRAFT',service.includes("PUBLICATION_STATE='DRAFT'")],
 ['DEV_ONLY skips approval SELECT only when isolated',service.includes('if (!devOnly()) {')],
 ['DPS2 guards BEFORE financial changes',dps.indexOf('cashPostingBridge.preflight(cashInstruction)')>0&&dps.indexOf('cashPostingBridge.preflight(cashInstruction)')<dps.indexOf('balanceService.post(leg.accountId()')],
 ['DPS2 records GL BEFORE POSTED',dps.indexOf('cashPostingBridge.record(cashInstruction)')>0&&dps.indexOf('cashPostingBridge.record(cashInstruction)')<dps.indexOf('repository.transition(txId,t.recordVersion(),"AUTHORIZED","POSTED"')],
 ['mandatory existing Oracle transaction',service.includes('Propagation.MANDATORY')],
 ['write trail tagged DEV_ONLY',service.includes('postingMode')&&service.includes('ORACLE_POSITION_LEDGER_DEV_ONLY')&&service.includes('"DEV_ONLY" : "APPROVED"')],
 ['DEV_ONLY no fake approval INSERT',!service.includes('INSERT INTO PST.POSTING_APPROVALS')&&!service.includes('INSERT INTO ACC.ACCOUNTING_APPROVALS')],
 ['strict single account guard',guard.includes('s.pilotAccountId() != 1L')],
 ['strict dev-only max amount',guard.includes('100000')],
 ['read endpoint scoped to pilot in DEV_ONLY',controller.includes('devPilotAccountId != 1L')&&controller.includes('ACCOUNT_ID=:account')],
 ['synthetic ownership acknowledgement',guard.includes('syntheticAccountConfirmed')],
 ['Spring posting-dev profile required',service.includes('Profiles.of("posting-dev")')],
 ['prod profiles prohibited',service.includes('Profiles.of("prod", "production")')],
 ['runtime Oracle DB and PDB identity queried',service.includes("'USERENV','DB_NAME'")&&service.includes("'USERENV','CON_NAME'")]
];
let errors=0;
for (const [name,ok] of checks) { console.log((ok?'PASS':'FAIL')+' '+name);if(!ok)errors++; }
const tmp=fs.mkdtempSync(path.join(os.tmpdir(),'r1d8i-policy-'));
try {
 const classes=['CashPostingPolicy.java','DevOnlyCashPostingGuard.java'].map(f=>path.join(root,dir+'posting/cash/'+f));
 const test=path.join(root,'backend/src/test/java/com/behsazan/corebanking/posting/cash/DevOnlyCashPostingGuardSelfTest.java');
 const c=spawnSync('javac',['-encoding','UTF-8','-d',tmp,...classes,test],{encoding:'utf8'});
 if (c.status!==0) {console.error(c.stderr||c.error);errors++;} else {
  const r=spawnSync('java',['-cp',tmp,'com.behsazan.corebanking.posting.cash.DevOnlyCashPostingGuardSelfTest'],{encoding:'utf8'});
  console.log(r.stdout.trim());if(r.status!==0){errors++;console.error(r.stderr);}
 }
} finally { fs.rmSync(tmp,{recursive:true,force:true}); }
console.log('POSTING_R1D8I_STATIC_STATUS='+(errors?'FAIL':'PASS'));
process.exitCode=errors?1:0;
