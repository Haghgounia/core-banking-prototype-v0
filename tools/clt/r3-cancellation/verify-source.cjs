'use strict';
const fs=require('fs'),path=require('path');
const root=path.resolve(process.argv[2] || process.cwd());
const read=p=>fs.readFileSync(path.join(root,p),'utf8');
const src='backend/src/main/java/com/behsazan/corebanking/collateral/insurance/cancellation/';
const ui='frontend/src/app/features/collateral/';
const policy=read(src+'CollateralInsuranceCancellationPolicy.java');
const service=read(src+'CollateralInsuranceCancellationService.java');
const controller=read(src+'CollateralInsuranceCancellationController.java');
const security=read('backend/src/main/java/com/behsazan/corebanking/collateral/prototype/CltUnifiedPrototypeSecurityConfiguration.java');
const uiService=read(ui+'collateral-insurance-cancellation.service.ts');
const component=read(ui+'collateral-insurance-cancellation.component.ts');
const html=read(ui+'collateral-insurance-cancellation.component.html');
const registry=read(ui+'collateral-registry.component.html');
const registryTs=read(ui+'collateral-registry.component.ts');
const sql=read('database/oracle/clt/r3cancellation/01_CANCELLATION_EVENT_SCHEMA_CANDIDATE_NOT_APPROVED.sql');
let passed=0;
function assert(c,why){if(!c)throw new Error('CLT_R3_CANCEL_SOURCE_FAIL:'+why);passed++}
const checks=[
 [service.includes('r3-cancellation.stage-enabled:false') && service.includes('r3-cancellation.final-enabled:false'),'both flags default off'],
 [!read('config/clt-unified-local.yml').includes('r3-cancellation.final-enabled:true'),'no flag enable in config'],
 [service.includes('DataSource applicationDataSource') && !service.includes('DriverManager'),'single shared datasource'],
 [service.includes('new TransactionTemplate') && service.includes('DataSourceTransactionManager'),'single transaction'],
 [service.includes('SELECT') && service.includes('CLT.COLLATERAL_INSURANCE'),'real insurance reads'],
 [service.includes('CLT.COLLATERAL_ALLOCATION') && service.includes('COUNT(*)'),'allocation conservative count'],
 [!service.includes('USED=') && !service.includes("USED='NO'"),'no used status shortcut'],
 [service.includes('PREVIOUS_INSURANCE_ID') && service.includes('successors'),'renewal guard'],
 [service.includes('BRANCH_CODE=?') && service.includes('introduction.actor(authentication)'),'branch guard'],
 [service.includes('ALL_TRIGGERS') && service.includes('appendOnlyTriggersReady'),'append-only trigger evidence'],
 [service.includes('ALL_CONSTRAINTS') && service.includes('statusChecksReady'),'validated constraint gate'],
 [service.includes('R3_INS_CANCEL_REQUEST') && service.includes('R3_INS_CANCEL_AUDIT'),'stage+audit'],
 [service.includes('R3_INS_CANCEL_IMPACT') && service.includes('R3_INS_CANCEL_RELEASE'),'bank impact+release'],
 [service.includes('R3_INS_CANCEL_EVENT') && service.includes('UQ_R3_INS_CAN_EVENT_POLICY'),'event uniqueness'],
 [service.includes('FOR UPDATE') && service.includes('RECORD_VERSION=?'),'oracle lock+CAS'],
 [service.includes('IDEMPOTENCY_KEY') && service.includes('requireReplay'),'replay protected'],
 [service.includes('new Stage(') && service.includes('requirePosting('),'posting guard'],
 [service.includes('replacementValid(') && service.includes('REPLACEMENT_INSURANCE_ID'),'replacement verification'],
 [service.includes('SYSTIMESTAMP') && service.includes('Asia/Tehran'),'oracle business day'],
 [service.includes('APPROVED_AT<=SYSTIMESTAMP'),'impact approval not future'],
 [service.includes('EVENT_ID') || service.includes('CANCEL_EVENT_ID'),'immutable receipt ID'],
 [!service.includes('DELETE FROM CLT.') && !service.includes('DELETE CLT.'),'no deletes'],
 [!service.includes('UPDATE CLT.COLLATERAL_INSURANCE') && !service.includes('UPDATE CLT.COLLATERAL_ALLOCATION'),'no history mutation'],
 [!service.includes('UPDATE CLT.R3_INS_CANCEL_EVENT') && !service.includes('UPDATE CLT.R3_INS_CANCEL_AUDIT'),'append only DML'],
 [policy.includes('automaticCancellationAllowed') && policy.includes('new Assessment(candidate, false'),'never automatic allow'],
 [policy.includes('G-04') && policy.includes('G-06') && policy.includes('G-09'),'open governance gaps'],
 [policy.includes('NO_ALLOCATION_ROWS') && policy.includes('NO_RENEWAL_SUCCESSOR'),'blocked conditions'],
 [policy.includes('requireStageTransition') && policy.includes('MAKER_REQUIRED'),'maker reviewer separation'],
 [policy.includes('THREE_PARTY_SEPARATION_REQUIRED'),'three party guard'],
 [policy.includes('INDEPENDENT_IMPACT_DECISION_MISSING'),'independent impact'],
 [policy.includes('BANK_RELEASE_MISSING'),'release gate'],
 [policy.includes('REPLAY_CONFLICT'),'replay collision'],
 [controller.includes('cancellation-readiness') && controller.includes('cancellation-activation'),'GET routes'],
 [controller.includes('@PostMapping') && controller.includes('cancellation-requests'),'write routes guarded'],
 [controller.includes('receipt') && controller.includes('commit('),'receipt and commit route'],
 [security.includes('/cancellation-requests') && security.includes('anyRequest().denyAll()'),'local stage allowlist final deny'],
 [uiService.includes('CancellationReadiness') && uiService.includes('CancellationReceipt'),'typed frontend API'],
 [component.includes('this.cancellation.preview(') && component.includes('this.cancellation.create('),'UI live preview + staging'],
 [component.includes('this.cancellation.transition(') && component.includes('this.cancellation.commit('),'UI staged workflow'],
 [component.includes('globalThis.confirm') && component.includes('crypto.randomUUID'),'user confirmation stable idempotency'],
 [html.includes('مشاهده اثر لغو'),'UI assessment view'],
 [html.includes('لغو خودکار') && html.includes('ثبت رویداد لغو'),'UI boundary clear'],
 [registry.includes('app-collateral-insurance-cancellation') && registry.includes('app-collateral-insurance-panel'),'sibling no overwritten insurance form'],
 [registryTs.includes('CollateralInsuranceCancellationComponent'),'standalone component registered'],
 [sql.indexOf('WHENEVER SQLERROR EXIT')>=0 && sql.indexOf('RAISE_APPLICATION_ERROR')>=0,'SQL stop before DDL'],
 [sql.indexOf('RAISE_APPLICATION_ERROR') < sql.indexOf('CREATE SEQUENCE'),'candidate DDL gated first'],
 [sql.includes('TRG_R3_INS_CANCEL_EVENT_APPEND') && sql.includes('TRG_R3_INS_CANCEL_AUDIT_APPEND'),'candidate append-only triggers'],
 [sql.includes('UQ_R3_INS_CAN_EVENT_POLICY') && sql.includes('UQ_R3_INS_CAN_EVENT_IDEMP'),'candidate uniqueness'],
 [read('database/oracle/clt/r3cancellation/00_CANCELLATION_READONLY_DICTIONARY.sql').indexOf('INSERT INTO')<0,'read-only recon'],
 [read('docs/collateral/CLT-GAP-REGISTER-R1.0-FA.md').includes('D-07'),'gap updated'],
 [read('docs/collateral/CLT-ROADMAP-R1.0-FA.md').includes('2026-11-10 — R3 Insurance Cancellation'),'roadmap checkpoint'],
 [fs.existsSync(path.join(root,'docs/collateral/CLT-R3-CANCELLATION-LIFECYCLE-CONTRACT-FA.md')),'contract source'],
 [fs.existsSync(path.join(root,'docs/collateral/CLT-R3-CANCELLATION-CHECKPOINT-2026-11-10.md')),'checkpoint source'],
 [fs.existsSync(path.join(root,'tools/clt/r3-cancellation/R3CancellationSmoke.java')),'java smoke source'],
 [fs.existsSync(path.join(root,'backend/src/test/java/com/behsazan/corebanking/collateral/insurance/cancellation/CollateralInsuranceCancellationPolicyTest.java')),'junit source'],
 [read('backend/src/main/java/com/behsazan/corebanking/collateral/workflow/CollateralInsuranceCommitService.java').includes('r3-posting.insurance-enabled:false'),'previous insurance kept default off'],
 [read('backend/src/main/java/com/behsazan/corebanking/collateral/workflow/CollateralValuationCommitService.java').includes('r3-posting.valuation-enabled:false'),'previous valuation kept default off'],
];
for(const [truth,name] of checks)assert(truth,name);
let ts;try {ts=require(path.join(root,'frontend/node_modules/typescript'));} catch{
  try{ts=require('typescript');}catch{
    const cp=require('child_process');const np=process.platform==='win32'?'npm.cmd':'npm';
    ts=require(path.join(cp.execFileSync(np,['root','-g'],{encoding:'utf8',timeout:6000}).trim(),'typescript'));
  }
}
for(const f of ['collateral-insurance-cancellation.service.ts','collateral-insurance-cancellation.component.ts','collateral-registry.component.ts']) {
  const parsed=ts.createSourceFile(f,read(ui+f),ts.ScriptTarget.Latest,true,ts.ScriptKind.TS);
  assert(parsed.parseDiagnostics.length===0,'TypeScript parse '+f);
}
console.log('CLT_R3_CANCELLATION_SOURCE_PASS='+passed+'/'+passed);
