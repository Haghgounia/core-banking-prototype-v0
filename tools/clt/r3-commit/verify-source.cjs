'use strict';
const fs=require('fs');const path=require('path');
const root=path.resolve(process.argv[2]||process.cwd());
const get=p=>fs.readFileSync(path.join(root,p),'utf8');
const base='backend/src/main/java/com/behsazan/corebanking/collateral/workflow/';
const ui='frontend/src/app/features/collateral/';
const service=get(base+'CollateralSponsorCommitService.java');
const policy=get(base+'CollateralSponsorCommitPolicy.java');
const controller=get(base+'CollateralWorkflowController.java');
const workflow=get(base+'CollateralWorkflowService.java');
const read=get(base+'CollateralPostingReadinessService.java');
const client=get(ui+'collateral-workflow.service.ts');
const component=get(ui+'collateral-draft-worklist.component.ts');
const html=get(ui+'collateral-draft-worklist.component.html');
const candidate=get('database/oracle/clt/r3posting/01_SPONSOR_POSTING_LEDGER_RELEASE_CANDIDATE_NOT_APPROVED.sql');
let checks=0;
function check(ok,message){if(!ok)throw Error('CLT_R3_COMMIT_CONTRACT_FAIL:'+message);checks++;}
check(service.includes('sponsor-enabled:false'), 'feature disabled default');
check(!get('config/clt-unified-local.yml').includes('r3-posting:'), 'no auto flag in dev overlay');
check(service.includes('sharedApplicationDataSource') && !service.includes('DriverManager') && !service.includes('new Hikari'), 'shared app datasource');
check(service.includes('DataSourceTransactionManager') && service.includes('TransactionTemplate'), 'atomic transaction');
check(service.includes('FOR UPDATE') && service.includes('lockParent('), 'stage and parent row locks');
check(service.includes('UQ_CLT_SPONSOR_COLL_PARTY') && service.includes('VALIDATED'), 'physical sponsor uniqueness gate');
check(service.includes('UQ_CLT_R3_POST_REQ') && service.includes('UQ_CLT_R3_POST_IDEMP'), 'ledger uniqueness gate');
check(service.includes('SEQ_CLT_R3_POSTING_LEDGER.NEXTVAL'), 'ledger sequence');
check(service.includes("'PREPARED'") && service.includes("'COMMITTED'"), 'transactional receipt lifecycle');
check(service.includes('RETURNING COLLATERAL_SPONSOR_ID INTO ?'), 'oracle returned generated key');
check(service.includes('preflight.inspect("sponsor"') && service.includes('workflow.validateForStageDecision'), 'in-transaction revalidation');
check(service.includes('CollateralStageFingerprint.digest'), 'digest recomputed from locked Oracle Stage');
check(service.includes('SCOPE_clt.collateral.review') && service.includes('SCOPE_clt.collateral.read'), 'scoped final write');
check(service.includes('who.branch()') && service.includes('who.subject()'), 'principal branch and actor');
check(service.includes('RELEASE_STATUS=\'APPROVED\''), 'approved release');
check(service.includes('EFFECTIVE_FROM <= SYSTIMESTAMP') && service.includes('EFFECTIVE_TO > SYSTIMESTAMP'), 'release time window');
check(policy.includes('makerSubject()') && policy.includes('postingSubject()'), 'independent actor');
check(policy.includes('checkReplay') && service.includes('IDEMPOTENCY_ALREADY_USED'), 'idem conflicts');
check(service.includes('JUSTIFICATION') && service.includes('POSTED_BY') && service.includes('POLICY_REVISION'), 'ledger audit metadata');
check(service.includes('command.reason().trim()') && service.includes('EXPECTED_VERSION'), 'replay reason and version guard');
check(controller.includes('@PostMapping("/sponsor/{requestId}/commit")'), 'sponsor endpoint');
check(controller.includes('@GetMapping("/sponsor/posting-activation")'), 'activation endpoint');
check(controller.includes('@GetMapping("/sponsor/{requestId}/receipt")'), 'read receipt endpoint');
check(service.includes('receipt(String requestId') && service.includes("COMMIT_STATUS='COMMITTED'"), 'read-only persisted receipt');
check(client.includes('sponsorReceipt(') && component.includes('sponsorPosted'), 'UI receipt evidence');
check(client.includes('commitSponsor(') && client.includes('sponsorPostingActivation()'), 'typed angular client');
check(component.includes('sponsorCanCommit()') && component.includes('globalThis.confirm'), 'explicit user consent in UI');
check(component.includes('sponsorCommitKeys') && component.includes('this.workflow.commitSponsor'), 'client retry key');
check(html.includes('ثبت عملیاتی ضامن') && html.includes('!sponsorCanCommit()'), 'disabled UI gate');
check(!read.includes('INSERT INTO CLT.') && !read.includes('jdbc.update('), 'read-only preflight preserved');
check(candidate.includes('WHENEVER SQLERROR EXIT') && candidate.includes('RAISE_APPLICATION_ERROR'), 'fail-closed candidate');
check(candidate.includes('UNIQUE (COLLATERAL_ID, PARTY_ID)'), 'candidate physical unique');
check(candidate.includes('UNIQUE (REQUEST_KIND,REQUEST_ID)') && candidate.includes('UNIQUE (BRANCH_CODE,IDEMPOTENCY_KEY)'), 'candidate receipt unique');
check(!get('config/clt-unified-local.yml').includes('CLT_R3_SPONSOR_POSTING_ENABLED:true'), 'not auto-enabled');
let ts;
try { ts=require('typescript'); } catch {}
if (!ts) {
 try {
  const {execFileSync}=require('child_process');
  const globalRoot=execFileSync(process.platform==='win32'?'npm.cmd':'npm', ['root','-g'],{encoding:'utf8',timeout:5000}).trim();
  ts=require(path.join(globalRoot,'typescript'));
 } catch {}
}
if (ts) {
 for(const file of ['collateral-workflow.service.ts','collateral-draft-worklist.component.ts']) {
  const source=ts.createSourceFile(file,get(ui+file),ts.ScriptTarget.Latest,true,ts.ScriptKind.TS);
  check(source.parseDiagnostics.length===0,'typescript syntax '+file);
 }
} else console.log('CLT_R3_TYPESCRIPT_SYNTAX_NOT_RUN=typescript_module_missing');
check(fs.existsSync(path.join(root,'docs/collateral/CLT-R3-SPONSOR-ATOMIC-COMMIT-CHECKPOINT-2026-11-09.md')), 'checkpoint');
check(fs.existsSync(path.join(root,'docs/collateral/CLT-R3-SPONSOR-COMMIT-TRANSACTION-CONTRACT-FA.md')), 'contract');
console.log('CLT_R3_SPONSOR_COMMIT_SOURCE_PASS='+checks+'/'+checks);
