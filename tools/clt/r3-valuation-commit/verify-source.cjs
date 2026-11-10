'use strict';
const fs=require('fs');const path=require('path');
const root=path.resolve(process.argv[2]||process.cwd());
const read=p=>fs.readFileSync(path.join(root,p),'utf8');
const base='backend/src/main/java/com/behsazan/corebanking/collateral/workflow/';
const web='frontend/src/app/features/collateral/';
const service=read(base+'CollateralValuationCommitService.java');
const policy=read(base+'CollateralValuationCommitPolicy.java');
const ctrl=read(base+'CollateralWorkflowController.java');
const frontend=read(web+'collateral-workflow.service.ts');
const component=read(web+'collateral-draft-worklist.component.ts');
const html=read(web+'collateral-draft-worklist.component.html');
const sql=read('database/oracle/clt/r3valuationpost/01_VALUATION_POSTING_SCHEMA_CANDIDATE_NOT_APPROVED.sql');
let n=0;
const has=(test,label)=>{if(!test)throw new Error('CLT_R3_VALUATION_SOURCE_FAIL:'+label);n++;};
const checks=[
 [service.includes('r3-posting.valuation-enabled:false'),'default off'],
 [!read('config/clt-unified-local.yml').includes('r3-posting.valuation-enabled:true'),'no config override'],
 [service.includes('DataSource sharedDataSource')&&!service.includes('DriverManager'),'same datasource'],
 [service.includes('TransactionTemplate') && service.includes('DataSourceTransactionManager'),'single transaction'],
 [service.includes('SELECT * FROM CLT.COLLATERAL_VALUATION_REQUEST') && service.includes('FOR UPDATE'),'Stage lock'],
 [service.includes('FOR UPDATE OF c.RECORD_VERSION'),'parent lock'],
 [service.includes('VALUATION_STATUS_CODE=\'SUPERSEDED\''),'previous record superseded'],
 [service.includes("'FINAL'"),'new FINAL record'],
 [service.includes('CURRENT_VALUATION_HISTORY_ID=?'),'pointer CAS'],
 [service.includes('RECORD_VERSION=RECORD_VERSION+1'),'record version bump'],
 [service.includes('UQ_COL_VAL_HIST_SEQUENCE'),'physical sequence uniqueness'],
 [service.includes('UQ_COL_VAL_HIST_SUPERSEDES'),'physical predecessor uniqueness'],
 [service.includes('FK_COLL_CURRENT_VALUATION'),'physical FK integrity'],
 [service.includes('ALL_TAB_IDENTITY_COLS'),'Oracle identity verification'],
 [service.includes('RETURNING COLLATERAL_VALUATION_HISTORY_ID INTO ?'),'generated ID'],
 [service.includes('R3_VAL_POSTING_RELEASE'),'approved release'],
 [service.includes('R3_VAL_POSTING_TYPE_RULE'),'approved appraisability and interval'],
 [service.includes('R3_VAL_POSTING_VALUER_RULE'),'valuer authority'],
 [service.includes('R3_VAL_POSTING_LEDGER'),'receipt ledger'],
 [service.includes('UQ_CLT_R3_VAL_POST_REQ') && service.includes('UQ_CLT_R3_VAL_POST_IDEMP'),'idempotency constraints'],
 [service.includes('CREATED_AT') && service.includes('POSTED_AT'),'audit timestamps'],
 [service.includes('PREPARED') && service.includes('COMMITTED'),'ledger in transaction'],
 [service.includes('preflight.inspect("valuation"'),'server stage evidence'],
 [service.includes('workflow.validateForStageDecision'),'live revalidation'],
 [service.includes('CollateralStageFingerprint.digest'),'payload integrity'],
 [service.includes('requireSupersession') && service.includes('predecessor('),'predecessor guard'],
 [service.includes('requirePeriod(') && service.includes('requireValuer('),'independent rules'],
 [service.includes('requireAmount('),'currency and value guard'],
 [service.includes('SCOPE_clt.collateral.read') && service.includes('SCOPE_clt.collateral.review'),'existing scope model'],
 [service.includes('actor.branch()') && service.includes('actor.subject()'),'branch/actor from principal'],
 [service.includes('requireSupersession') && service.includes('UQ_COL_VAL_HIST_SUPERSEDES'),'race protection'],
 [service.includes('checkReplay('),'replay guard'],
 [service.includes('receipt(String requestId') && service.includes('currentAtRead'),'post-revaluation receipt semantics'],
 [service.includes('RECORDED_EFFECTIVE_TO'),'recorded deadline'],
 [service.includes('oracleTehranDate()'),'explicit date source'],
 [policy.includes('EXCLUSIVE_NEXT_START'),'period boundary explicit'],
 [policy.includes('VALUATION_DATE') && policy.includes('EFFECTIVE_FROM'),'anchor explicit'],
 [policy.includes('YEAR') && policy.includes('MONTH') && policy.includes('DAY'),'interval units'],
 [policy.includes('eligibleValue()'),'assessed value upper bound'],
 [policy.includes('G-18') && policy.includes('G-10'),'open gaps'],
 [!service.includes('DELETE FROM CLT.') && !service.includes('UPDATE CLT.COLLATERAL_ALLOCATION'),'no allocation mutation'],
 [ctrl.includes('@GetMapping("/valuation/posting-activation")'),'activation endpoint'],
 [ctrl.includes('@GetMapping("/valuation/{requestId}/receipt")'),'receipt endpoint'],
 [ctrl.includes('@PostMapping("/valuation/{requestId}/commit")'),'commit endpoint'],
 [frontend.includes('valuationReceipt(') && frontend.includes('commitValuation('),'typed TS calls'],
 [component.includes('valuationCommitKeys') && component.includes('this.workflow.commitValuation'),'same idem on retry'],
 [component.includes('valuationCanCommit()') && component.includes('globalThis.confirm'),'client explicit action'],
 [html.includes('!valuationCanCommit()') && html.includes('ثبت نهایی/تجدید ارزیابی'),'UI button conditional'],
 [html.includes('ارزیابی جاری') && html.includes('رکورد تاریخی'),'receipt current semantics'],
 [html.includes('کفایت ارزش') && html.includes('تخصیص'),'not allocation eligibility'],
 [sql.includes('WHENEVER SQLERROR EXIT') && sql.includes('RAISE_APPLICATION_ERROR'),'migration blocked'],
 [sql.includes('/*') && sql.includes('*/'),'candidate DDL nonexecutable'],
 [sql.includes('UQ_CLT_R3_VAL_POST_REQ') && sql.includes('UQ_CLT_R3_VAL_POST_IDEMP'),'candidate unique receipt'],
 [sql.includes('CK_CLT_R3_VAL_TYPE_PERIOD'),'candidate period guard'],
 [fs.existsSync(path.join(root,'docs/collateral/CLT-R3-VALUATION-TRANSACTION-CONTRACT-FA.md')),'contract exists'],
 [read('docs/collateral/CLT-GAP-REGISTER-R1.0-FA.md').includes('D-06'),'gap tracker updated'],
 [read('docs/collateral/CLT-ROADMAP-R1.0-FA.md').includes('CLT-R3-VALUATION-ATOMIC-COMMIT'),'roadmap checkpoint'],
 [fs.existsSync(path.join(root,'backend/src/test/java/com/behsazan/corebanking/collateral/workflow/CollateralValuationCommitPolicyTest.java')),'JUnit'],
 [read(base+'CollateralInsuranceCommitService.java').includes('r3-posting.insurance-enabled:false'),'existing insurance unaffected'],
 [read(base+'CollateralSponsorCommitService.java').includes('sponsor-enabled:false'),'existing sponsor unaffected'],
];
for (const [x,label] of checks)has(x,label);
let ts;
try {ts=require(path.join(root,'frontend','node_modules','typescript'));}
catch {try{ts=require('typescript');}catch{}}
if(!ts){const {execFileSync}=require('child_process');const globalDir=execFileSync(process.platform==='win32'?'npm.cmd':'npm',['root','-g'],{encoding:'utf8',timeout:5000}).trim();ts=require(path.join(globalDir,'typescript'));}
for(const fn of ['collateral-workflow.service.ts','collateral-draft-worklist.component.ts']){
 const src=ts.createSourceFile(fn,read(web+fn),ts.ScriptTarget.Latest,true,ts.ScriptKind.TS);
 has(src.parseDiagnostics.length===0,'typescript parse '+fn);
}
console.log('CLT_R3_VALUATION_SOURCE_PASS='+n+'/'+n);
