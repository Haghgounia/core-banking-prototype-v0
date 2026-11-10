#!/usr/bin/env node
'use strict';
/* BUC09/10/11/12/15 lifecycle source contract. Static checks, not live Oracle evidence. */
const fs=require('fs'),path=require('path');
const root=path.resolve(__dirname,'../../..');
const base='backend/src/main/java/com/behsazan/corebanking/collateral/';
function read(rel){const file=path.join(root,rel);if(!fs.existsSync(file))throw Error('LIFECYCLE_FILE_MISSING '+rel);return fs.readFileSync(file,'utf8');}
const repo=read(base+'posting/CollateralPostingRepository.java');
const svc=read(base+'posting/CollateralPostingService.java');
const release=read(base+'release/CollateralReleasePostingService.java');
const intro=read(base+'reintroduction/CollateralReintroductionService.java');
const cu=read(base+'crossunit/CollateralCrossUnitService.java');
const props=read('backend/src/main/resources/clt/collateral-posting.properties');
const cand=read('database/oracle/clt/posting/02_BUC09_10_15_STAGE_CONSUMPTION_CANDIDATE_NOT_APPROVED.sql');
const ctrl=read(base+'release/CollateralReleasePostingController.java');
const security=read(base+'registry/CollateralRegistrySecurityConfiguration.java');
const localSecurity=read(base+'localuat/CltLocalUatSecurityConfiguration.java');
const ui=read('frontend/src/app/features/collateral/collateral-final-release-panel.component.html');
const view=read('frontend/src/app/features/collateral/collateral-final-release-panel.component.ts');
const api=read('frontend/src/app/features/collateral/collateral-final-release.service.ts');
let checks=0;
const ok=(condition,label)=>{if(!condition)throw Error('CLT_LIFECYCLE_CONTRACT_FAIL '+label);checks++};
const has=(str,tokens,label)=>{for(const term of tokens)ok(str.includes(term),(label||'')+' '+term)};
has(repo,['public String ownerBranch(JdbcTemplate','SELECT BRANCH_CODE FROM CLT.COLLATERAL',
 'FOR UPDATE WAIT 2','matchesCrossUnitIntent','FOR UPDATE OF g.GRANT_STATUS,i.INTENT_STATUS',
 "g.GRANT_STATUS='APPROVED'",'g.SOURCE_RECORD_VERSION=c.RECORD_VERSION',
 'g.VALID_UNTIL_UTC>SYS_EXTRACT_UTC(SYSTIMESTAMP)',"t.USE_IN_OTHER_BRANCHES='Y'",
 'i.REGISTRATION_NUMBER=c.COLLATERAL_NUMBER','i.CLIENT_ID=?','i.PAYLOAD_DIGEST=?',
 'g.CONSUMER_BRANCH=i.CONSUMER_BRANCH','p.consumerBranch()',
 'consumeReviewedStage(JdbcTemplate',"SET REQUEST_STATUS='POSTED'", "SET INTENT_STATUS='POSTED'",
 'POSTED_TRANSACTION_ID','POSTED_ALLOCATION_ID','POSTED_AT_UTC','RECORD_VERSION=RECORD_VERSION+1',
 'POSTING_SOURCE_STAGE_ALREADY_CONSUMED_OR_CHANGED',"REQUEST_STATUS='RECORDED_NOT_UNALLOCATED'",
 'OR TRANSACTION_STATUS_CODE NOT IN (?,','AND EXECUTING_BRANCH_CODE=? AND CURRENCY_CODE=?'],'repository');
ok(svc.indexOf('repository.ownerBranch(jdbc,id)')<svc.indexOf('repository.lockParent(jdbc,id,owner)'), 'authoritative owner before root lock');
ok(svc.indexOf('repository.lockParent(jdbc,id,owner)')<svc.indexOf('repository.lockPermit(jdbc,permitId,id,owner)'), 'parent lock before permit');
ok(svc.indexOf('repository.insertTransaction(jdbc,transactionId')<svc.indexOf('repository.consumeReviewedStage(jdbc,p,transactionId,allocationId)'), 'stage consumed after financial txn insert');
ok(svc.indexOf('repository.consumeReviewedStage(jdbc,p,transactionId,allocationId)')<svc.indexOf('repository.bumpParent(jdbc,parent,who.subject())'), 'stage CAS before parent CAS');
ok(svc.indexOf('repository.bumpParent(jdbc,parent,who.subject())')<svc.indexOf('repository.consumePermit(jdbc,p,key'), 'permit CAS last before commit');
has(svc,['sourceRequestId','UQ_CLT_POST_SOURCE', 'Integer.valueOf(10).equals(constraints)',
 'stagePostingReady("CLT_ALLOCATION_REQUEST"','stagePostingReady("CLT_UNALLOC_REQ"',
 'stagePostingReady("CLT_CROSS_UNIT_INTENT"','SEARCH_CONDITION_VC',
 'POSTING_CROSS_UNIT_BANK_CONTRACT_DISABLED','POSTING_CONSUMER_BRANCH_NOT_ATTESTED',
 'crossUnitPostingReady()','who.branch().equals(p.consumerBranch())',
 'core-banking.collateral.posting.cross-unit-allocation-enabled',
 'POSTING_LOCAL_PROTOTYPE_FINANCIAL_WRITE_DENIED'],'posting service');
has(release,['TRANSACTION_STATUS_CODE IS NULL','crossUnitPermissions(id)',
 'RELEASE_CROSS_UNIT_GRANTS_NOT_CLOSED','GRANT_STATUS IN (\'PENDING\',\'APPROVED\')',
 'RELEASE_CROSS_UNIT_SCHEMA_INCOMPLETE','/lifecycle-interlocks'.replace('/lifecycle-interlocks','public LifecycleInterlocks lifecycle')],'release');
has(intro,['crossUnitCleared(old)','Integer.valueOf(1).equals(posted) && crossUnitCleared(old)',
 'GRANT_STATUS IN (\'PENDING\',\'APPROVED\')','FINAL_RELEASE_NOT_PROVEN'],'reintroduction');
has(cu,['Parent p=ownerCollateral(id,a.branch(),true);',
 'Parent p=ownerCollateral(collateral,a.branch(),true);',
 'ownerCollateral(collateral,a.branch(),true); // parent BEFORE grant, always'],'crossunit lock order');
has(svc,['repository.lockAllocation(jdbc,p.allocationId(),id,owner)'],'BUC10 owner lock');
has(cu,['boolean posted="POSTED".equals(r.status())','replay,posted,r.created()',
 'BUC09_ATOMIC_POSTING_COMPLETED_USE_FINANCIAL_LEDGER_AS_AUTHORITY'],'crossunit');
has(props,['cross-unit-allocation-enabled=${CLT_CROSS_UNIT_POSTING_ENABLED:false}',
 'allocation-enabled=${CLT_ALLOCATION_POSTING_ENABLED:false}',
 'unallocation-enabled=${CLT_UNALLOCATION_POSTING_ENABLED:false}'],'flags');
ok(!props.includes('CLT_CROSS_UNIT_POSTING_ENABLED:true'),'BUC15 financial posting default off');
has(security,['"/api/v1/collaterals/*/final-release/posting-readiness"',
 '"/api/v1/collaterals/*/final-release/approved-permits"'],'secured BUC11 existing reads');
has(localSecurity,['"/api/v1/collaterals/*/final-release/posting-readiness"',
 '"/api/v1/collaterals/*/final-release/approved-permits"'],'local BUC11 existing reads');
has(security,['"/api/v1/collaterals/*/final-release/lifecycle-interlocks"'],'secured BUC11 lifecycle read');
has(localSecurity,['"/api/v1/collaterals/*/final-release/lifecycle-interlocks"'],'local analyst lifecycle read');
has(ctrl,['@GetMapping("/lifecycle-interlocks")','service.lifecycle(collateralId,auth)'],'release GET');
has(api,['interface ReleaseLifecycleInterlocks','lifecycleInterlocks(id:string)'],'analyst API');
has(view,['interlocks=signal<ReleaseLifecycleInterlocks|null>','this.api.lifecycleInterlocks(id)'],'analyst state');
has(ui,['lk.activeAllocations','lk.openCrossUnitGrants','lk.unresolvedTransactions',
 'lk.capacityInterlocksClear','ثبت نهایی فقط'],'analyst view');
has(cand,['WHENEVER SQLERROR EXIT','RAISE_APPLICATION_ERROR(-20957','/*',
 "REQUEST_STATUS='POSTED'","INTENT_STATUS='POSTED'",'UQ_CLT_POST_SOURCE',
 'DROP CONSTRAINT CK_CLT_ALLOC_REQ_STATUS','DROP CONSTRAINT CK_CLT_UA_REQ_STATUS',
 'DROP CONSTRAINT CK_CLT_CU_INTENT_STATUS'],'candidate');
ok(cand.indexOf('RAISE_APPLICATION_ERROR(-20957')<cand.indexOf('ALTER TABLE CLT.'),'fail closed before DDL proposal');
ok(cand.trimEnd().endsWith('*/'),'DDL remains commented out');
console.log('CLT_BUC09_10_11_12_15_LIFECYCLE_CONTRACT_PASS='+checks+'/'+checks+'; ORACLE_DDL=NONE; POSTING_FLAGS=OFF');
