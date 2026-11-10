'use strict';
const fs=require('fs');const path=require('path');
const root=path.resolve(process.argv[2]||process.cwd());
const read=p=>fs.readFileSync(path.join(root,p),'utf8');
const java='backend/src/main/java/com/behsazan/corebanking/collateral/workflow/';
const web='frontend/src/app/features/collateral/';
const service=read(java+'CollateralInsuranceCommitService.java');
const policy=read(java+'CollateralInsuranceCommitPolicy.java');
const controller=read(java+'CollateralWorkflowController.java');
const client=read(web+'collateral-workflow.service.ts');
const component=read(web+'collateral-draft-worklist.component.ts');
const html=read(web+'collateral-draft-worklist.component.html');
const candidate=read('database/oracle/clt/r3insurancepost/01_INSURANCE_POSTING_SCHEMA_CANDIDATE_NOT_APPROVED.sql');
const sponsor=read(java+'CollateralSponsorCommitService.java');
const preflight=read(java+'CollateralPostingReadinessService.java');
let n=0;
function has(ok,code){if(!ok)throw Error('CLT_R3_INSURANCE_SOURCE_FAIL:'+code);n++;}
const tests=[
  [service.includes('r3-posting.insurance-enabled:false'),'flag default off'],
  [!read('config/clt-unified-local.yml').includes('r3-posting.insurance-enabled:true'),'no automatic config'],
  [service.includes('DataSource sharedDataSource') && !service.includes('DriverManager'),'shared data source'],
  [service.includes('TransactionTemplate') && service.includes('DataSourceTransactionManager'),'atomic transaction'],
  [service.includes('lockStage(') && service.includes('lockCollateral('),'stage/parent locks'],
  [service.includes('FOR UPDATE'),'row locks SQL'],
  [service.includes('R3_INS_POSTING_RELEASE'),'separate release'],
  [service.includes('R3_INS_POSTING_RULE'),'per type coverage rule'],
  [service.includes('R3_INS_POSTING_LEDGER'),'immutable receipt ledger'],
  [service.includes('UQ_CLT_INS_RENEW_ONCE'),'physically unique renewal'],
  [service.includes('UQ_CI_POLICY'),'physical insurer number unique'],
  [service.includes('UQ_CLT_R3_INS_POST_REQ') && service.includes('UQ_CLT_R3_INS_POST_IDEMP'),'unique receipt and idem'],
  [service.includes("c.CONSTRAINT_TYPE='U'") && service.includes("c.VALIDATED='VALIDATED'"),'validated unique check'],
  [service.includes('SEQ_CLT_R3_INS_POST_LEDGER.NEXTVAL'),'ledger sequence'],
  [service.includes('PREPARED') && service.includes('COMMITTED'),'two-stage ledger within transaction'],
  [service.includes('RETURNING COLLATERAL_INSURANCE_ID INTO ?'),'Oracle generated id'],
  [service.includes('preflight.inspect("insurance"'), 'server preflight'],
  [service.includes('workflow.validateForStageDecision'),'server live insurance revalidation'],
  [service.includes('CollateralStageFingerprint.digest'),'hash from Stage'],
  [service.includes('requireCoverage('),'coverage guard'],
  [service.includes('checkPredecessor(') && service.includes('requireRenewal('),'renewal guard'],
  [service.includes('PREVIOUS_INSURANCE_ID'),'renewal lineage persisted'],
  [service.includes('SCOPE_clt.collateral.read') && service.includes('SCOPE_clt.collateral.review'),'shared scopes'],
  [service.includes('who.branch()') && service.includes('who.subject()'),'real principal branch'],
  [service.includes("RELEASE_STATUS='APPROVED'"),'approved release status'],
  [service.includes('EFFECTIVE_FROM<=SYSTIMESTAMP') && service.includes('EFFECTIVE_TO>SYSTIMESTAMP'),'effective window'],
  [service.includes('POSTED_BY') && service.includes('POLICY_REVISION'),'audit policy refs'],
  [service.includes('RECORD_VERSION') && service.includes('PAYLOAD_HASH'),'stage version'],
  [service.includes('checkReplay(') && service.includes('IDEMPOTENCY_REUSED'),'replay key protection'],
  [service.includes('receipt(String requestId') && service.includes("COMMIT_STATUS='COMMITTED'"),'receipt read after disable'],
  [service.includes('COVERAGE_RULE_ID'),'rule evidence captured in ledger'],
  [policy.includes('CANCELLATION_IMPACT_DECISION_REF') || policy.includes('cancellationImpactDecision'),'explicit cancellation impact approval'],
  [policy.includes('unresolvedAfterRegistration'),'still open gaps'],
  [!service.includes('DELETE FROM CLT.COLLATERAL_INSURANCE') && !service.includes('UPDATE CLT.COLLATERAL_INSURANCE'),'no implicit cancellation'],
  [controller.includes('@GetMapping("/insurance/posting-activation")'),'activation API'],
  [controller.includes('@GetMapping("/insurance/{requestId}/receipt")'),'receipt API'],
  [controller.includes('@PostMapping("/insurance/{requestId}/commit")'),'commit API'],
  [client.includes('insuranceReceipt(') && client.includes('commitInsurance('),'typed API'],
  [component.includes('insuranceCommitKeys') && component.includes('this.workflow.commitInsurance'),'UI retry idempotency'],
  [component.includes('insuranceCanCommit()') && component.includes('globalThis.confirm'),'UI consent'],
  [html.includes('!insuranceCanCommit()') && html.includes('ثبت / تمدید عملیاتی بیمه'),'conditional UI'],
  [html.includes('لغو بیمه‌نامه در این نسخه انجام نمی‌شود'),'cancellation clearly blocked'],
  [!preflight.includes('jdbc.update(') && !preflight.includes('DELETE FROM'),'read-only preflight preserved'],
  [sponsor.includes('SPONSOR_ATOMIC_COMMIT') || sponsor.includes('sponsor-enabled:false'),'sponsor logic remains'],
  [candidate.includes('WHENEVER SQLERROR EXIT') && candidate.includes('RAISE_APPLICATION_ERROR'),'SQL migration cannot execute as-is'],
  [candidate.includes('UNIQUE (PREVIOUS_INSURANCE_ID)'),'DB renewal uniqueness'],
  [candidate.includes('UNIQUE (REQUEST_ID)') && candidate.includes('UNIQUE (BRANCH_CODE,IDEMPOTENCY_KEY)'),'ledger uniqueness DDL'],
  [!read('config/clt-unified-local.yml').includes('R3_INSURANCE_POSTING_FLAG=true'),'no dev override'],
  [read('docs/collateral/CLT-GAP-REGISTER-R1.0-FA.md').includes('D-05'),'canonical gap register updated'],
  [read('docs/collateral/CLT-ROADMAP-R1.0-FA.md').includes('CLT-R3-INSURANCE-ATOMIC-COMMIT'),'roadmap checkpoint'],
  [fs.existsSync(path.join(root,'docs/collateral/CLT-R3-INSURANCE-TRANSACTION-CONTRACT-FA.md')),'contract'],
  [fs.existsSync(path.join(root,'backend/src/test/java/com/behsazan/corebanking/collateral/workflow/CollateralInsuranceCommitPolicyTest.java')),'JUnit'],
];
for(const [ok,code] of tests)has(ok,code);
let ts;
try { ts=require(path.join(root,'frontend','node_modules','typescript')); }
catch { try { ts=require('typescript'); } catch {} }
if (!ts) {
 try {
  const {execFileSync}=require('child_process');
  const globalDir=execFileSync(process.platform==='win32' ? 'npm.cmd' : 'npm',
   ['root','-g'],{encoding:'utf8',timeout:5000}).trim();
  ts=require(path.join(globalDir,'typescript'));
 } catch { throw Error('CLT_R3_INSURANCE_TYPESCRIPT_MODULE_REQUIRED_FOR_QA'); }
}
for (const f of ['collateral-workflow.service.ts','collateral-draft-worklist.component.ts']) {
 const src=ts.createSourceFile(f,read(web+f),ts.ScriptTarget.Latest,true,ts.ScriptKind.TS);
 has(src.parseDiagnostics.length===0,'TypeScript parse '+f);
}
// Angular template constructs are not compiled here (full Angular dependency graph unavailable).
console.log('CLT_R3_INSURANCE_SOURCE_PASS='+n+'/'+n);
