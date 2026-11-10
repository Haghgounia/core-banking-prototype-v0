#!/usr/bin/env node
'use strict';
const fs=require('fs'),path=require('path');
const root=path.resolve(__dirname,'../../..');
function file(rel){const p=path.join(root,rel);if(!fs.existsSync(p))throw Error('BUC12_MISSING_FILE '+rel);return fs.readFileSync(p,'utf8');}
let passed=0;
function check(ok,what){if(!ok)throw Error('BUC12_SOURCE_CONTRACT_FAIL '+what);passed++;}
function has(src,token,what){check(src.includes(token),what||token);}
const jbase='backend/src/main/java/com/behsazan/corebanking/collateral/reintroduction/';
const m=file(jbase+'CollateralReintroductionModels.java');
const pol=file(jbase+'CollateralReintroductionPolicy.java');
const svc=file(jbase+'CollateralReintroductionService.java');
const ctrl=file(jbase+'CollateralReintroductionController.java');
const sec=file(jbase+'CollateralReintroductionExceptionHandler.java');
const ts=file('frontend/src/app/features/collateral/collateral-reintroduction.service.ts');
const cmp=file('frontend/src/app/features/collateral/collateral-reintroduction-panel.component.ts');
const html=file('frontend/src/app/features/collateral/collateral-reintroduction-panel.component.html');
const reg=file('frontend/src/app/features/collateral/collateral-registry.component.ts');
const regHtml=file('frontend/src/app/features/collateral/collateral-registry.component.html');
const sql=file('database/oracle/clt/reintroduction/01_REINTRODUCTION_PERMIT_SCHEMA_CANDIDATE_NOT_APPROVED.sql');
const doc=file('docs/collateral/CLT-BUC12-ASSET-REINTRODUCTION-2026-11-10.md');
for(const x of ['/readiness','/evidence','/approved-permits','/lineage','/commit'])has(ctrl,x);
for(const x of ['@RequestHeader("If-Match")','@RequestHeader("Idempotency-Key")','Authentication actor'])has(ctrl,x);
for(const x of ['REINTRO_FINAL_RELEASE_NOT_PROVEN','REINTRO_PREVIOUS_ALLOCATION_NOT_CLEARED','REINTRO_ASSET_ALREADY_CURRENT_OR_LINK_INVALID','REINTRO_NEW_BANK_NUMBER_REQUIRED','REINTRO_THREE_PARTY_APPROVAL_REQUIRED','REINTRO_APPROVAL_EXPIRED','REINTRO_REVIEWED_DRAFT_NOT_CONSISTENT'])has(pol,x);
for(const x of ['CLT_FINAL_RELEASE_PERMIT','POSTED_RECORD_VERSION','BUC11_FINAL_RELEASE','CLT_ASSET_IDENTITY','UQ_CLT_ASSET_ONE_CURRENT','CLT_REINTRODUCTION_PERMIT','CLT_REGISTRATION_NUMBER_AUTHORITY','CLT_REGISTRATION_LEDGER','CLT_ASSET_COLLATERAL_LINK','CLT_COLLATERAL_SERIAL_RANGE','COLLATERAL_ATTRIBUTE_DEFINITION_ITEM','COLLATERAL_STATUS_HISTORY','BUC12_REINTRODUCTION'])has(svc,x);
for(const x of ['new TransactionTemplate','new DataSourceTransactionManager','FOR UPDATE','try{return Objects.requireNonNull(tx.execute','POSTED_COLLATERAL_ID','POSTED_COLLATERAL_NUMBER','IDEMPOTENCY_KEY','OWNER_REVERIFICATION_REFERENCE','BANK_APPROVAL_REFERENCE','APPROVAL_REFERENCE=?','MANUAL_ENTRY_ALLOWED_FLAG','validSerialRanges','verifiedAttributes','persistAttributes','persistSerialRows','SOURCE_REFERENCE','READONLY','posting-enabled']){
  const txt=x==='new TransactionTemplate'?'new TransactionTemplate(new DataSourceTransactionManager':x==='READONLY'?'NEVER changed':x;
  has(svc,txt,x);
}
check(!svc.includes('DELETE FROM CLT.COLLATERAL'),'no delete physical collateral');
check(!svc.includes('UPDATE CLT.COLLATERAL\n'),'no updating original collateral');
check(!svc.includes('UPDATE CLT.COLLATERAL_ALLOCATION'),'no cross-transaction allocation mutation');
check(!svc.includes('POST /api/v1/collaterals/r3'),'stable endpoint');
has(svc,'REINTRO_PROTOTYPE_FINAL_WRITE_BLOCKED');has(svc,'REINTRO_EXPLICIT_POSTING_SCOPE_REQUIRED');
check(!/core-banking\.collateral\.reintroduction\.posting-enabled.*true/.test(svc),'posting default not true');
for(const x of ['approvedRegistrationNumber','newCollateralId','previousRegistrationNumber','assetId'])has(m,x);
for(const x of ['@RestControllerAdvice','IllegalArgumentException'])has(sec,x);
for(const x of ['@Injectable','approvedRegistrationNumber','Idempotency-Key','If-Match'])has(ts,x);
for(const x of ['confirm','randomUUID','canCommit','selectPermit','replayKey'])has(cmp,x);
for(const x of ['شماره انتظامی جدید','{{p.approvedRegistrationNumber}}','{{r.newCollateralId}}','item.previousCollateralId'])has(html,x);
has(reg,'CollateralReintroductionPanelComponent');has(regHtml,'app-collateral-reintroduction-panel');
check(sql.includes('RAISE_APPLICATION_ERROR(-20942'),'unsafe SQL guard present');
check(sql.indexOf('/*')<sql.indexOf('CREATE TABLE CLT.CLT_REINTRODUCTION_PERMIT'),'candidate DDL commented');
check(sql.includes('UQ_CLT_REINTRO_ONE_POSTED'),'single previous posting');
check(sql.includes('FK_CLT_REINTRO_NUMBER'),'bank-issued number FK');
check(sql.trimEnd().endsWith('*/'),'all candidate DDL commented');
for(const x of ['BUC-12','BR-04','G-14','G-17','Oracle','فعال‌سازی'])has(doc,x);
console.log('CLT_BUC12_SOURCE_CONTRACT_PASS='+passed+'/'+passed);
