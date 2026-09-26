import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const read=p=>fs.readFileSync(path.join(root,p),'utf8');
let pass=0,fail=0;
const check=(label,ok)=>{if(ok){pass++;console.log(`PASS | ${label}`)}else{fail++;console.log(`FAIL | ${label}`)}};
const files={
 road:'docs/DPS2-CANONICAL-ROADMAP-FA.md',
 trace:'docs/dps2/reference/Deposit_Account_Operations_Traceability_Guide_FA_v2_2026-09-22.html',
 txSvc:'backend/src/main/java/com/behsazan/corebanking/deposit/account/transaction/application/DepositTransactionService.java',
 closureSvc:'backend/src/main/java/com/behsazan/corebanking/deposit/account/closure/application/DepositClosureService.java',
 closureRepo:'backend/src/main/java/com/behsazan/corebanking/deposit/account/closure/oracle/DepositClosureRepository.java',
 opsModels:'backend/src/main/java/com/behsazan/corebanking/deposit/account/operations/domain/DepositAccountOperationsModels.java',
 opsRepo:'backend/src/main/java/com/behsazan/corebanking/deposit/account/operations/oracle/DepositAccountOperationsRepository.java',
 waveB:'backend/src/main/java/com/behsazan/corebanking/deposit/account/waveb/application/DepositAccountWaveBService.java',
 waveC:'backend/src/main/java/com/behsazan/corebanking/deposit/account/wavec/application/DepositAccountWaveCService.java',
 ctl:'backend/src/main/java/com/behsazan/corebanking/deposit/account/operations/web/DepositAccountOperationsController.java',
 uiSvc:'frontend/src/app/features/four-deposits/deposit-account-operations.service.ts',
 uiHtml:'frontend/src/app/features/four-deposits/deposit-account-operations.component.html',
 v11i:'tools/verify-dps2-step05-transaction-processing-11i.mjs',
 v11e:'tools/verify-dps2-closure-reopening-11e.mjs',
 v11k:'tools/verify-dps2-wave-b-statements-limits-maturity-11k.mjs',
 r11i:'tools/runtime-dps2-phase11i-e2e.mjs',
 r11e:'tools/runtime-dps2-phase11e-e2e.mjs',
 r11k:'tools/runtime-dps2-phase11k-wave-b-e2e.mjs',
 r0910:'tools/runtime-dps2-phase11nd-steps09-10-e2e.mjs',
 mig:'database/oracle/dps2/migrations/0.11.0-phase11nd-steps05-10-canonical-audit.sql',
 db:'database/oracle/dps2/verification/0.11.0-phase11nd-steps05-10-canonical-audit-verifier.sql',
 apply:'tools/apply-dps2-phase11nd.cmd',
 qualify:'tools/qualify-dps2-phase11nd.cmd',
 runnerCmd:'tools/run-oracle-sql.cmd',
 runnerJava:'tools/java/OracleJdbcSqlRunner.java',
 qa:'docs/DPS2-0.11.0-PHASE11ND-STEPS05-10-CANONICAL-AUDIT-QA.md',
 build:'build-production.cmd'
};
for(const [k,p] of Object.entries(files))check(`11N-D file exists: ${k}`,fs.existsSync(path.join(root,p)));
const t=Object.fromEntries(Object.entries(files).filter(([,p])=>fs.existsSync(path.join(root,p))).map(([k,p])=>[k,read(p)]));

check('roadmap registers 11N-D as current Steps05-10 audit',/Phase 11N-D — Steps 05–10 Canonical Regression \/ Audit/.test(t.road||'')&&/Phase 11N-D[\s\S]{0,240}Status:[^\n]+IN_PROGRESS/.test(t.road||''));
check('roadmap preserves Steps05-07 and 09-10 DONE during audit',/[|] 05 [|][^\n]+[|] DONE [|]/.test(t.road||'')&&/[|] 06 [|][^\n]+[|] DONE [|]/.test(t.road||'')&&/[|] 07 [|][^\n]+[|] DONE [|]/.test(t.road||'')&&/[|] 09 [|][^\n]+[|] DONE [|]/.test(t.road||'')&&/[|] 10 [|][^\n]+[|] DONE [|]/.test(t.road||''));
check('roadmap keeps Step08 partial until 11N-D qualification',/[|] 08 [|][^\n]+[|] PARTIAL [|]/.test(t.road||'')&&/11N-D audit\/regression/.test(t.road||''));
check('roadmap keeps Steps11-13 outside 11N-D',/[|] 11 [|][^\n]+[|] PARTIAL [|]/.test(t.road||'')&&/[|] 12 [|][^\n]+[|] PARTIAL [|]/.test(t.road||'')&&/[|] 13 [|][^\n]+[|] PARTIAL [|]/.test(t.road||''));

check('trace Step05 requires transaction validation authorization legs and reversal',/step-05/.test(t.trace||'')&&/DEPOSIT_TRANSACTION_VALIDATION/.test(t.trace||'')&&/DEPOSIT_TRANSACTION_AUTHORIZATION/.test(t.trace||'')&&/DEPOSIT_TRANSACTION_LEG/.test(t.trace||'')&&/DEPOSIT_TRANSACTION_REVERSAL/.test(t.trace||''));
check('trace Steps09-10 are account services and party access only',/id="step-09"[\s\S]*DEPOSIT_ACCOUNT_API_ACCESS/.test(t.trace||'')&&/id="step-10"[\s\S]*DEPOSIT_ACCOUNT_PAYMENT_INSTRUMENT/.test(t.trace||''));
check('Step05 regression remains covered by 11I verifier/runtime',/PHASE11I_STATIC_BASELINE_PASS/.test(t.v11i||'')&&/PHASE11I_RUNTIME_E2E_PASS/.test(t.r11i||''));
check('Step06-08 regression remains covered by 11K verifier/runtime',/PHASE11K_STATIC_BASELINE_PASS/.test(t.v11k||'')&&/PHASE11K_RUNTIME_E2E_PASS/.test(t.r11k||''));

check('Step09 service actions exist',/inquiry/.test(t.ctl||'')&&/confirmation/.test(t.ctl||'')&&/notificationPreference/.test(t.ctl||'')&&/notificationEvent/.test(t.ctl||'')&&/grantApiAccess/.test(t.ctl||'')&&/revokeApiAccess/.test(t.ctl||''));
check('Step10 party-access actions exist',/signatureRule/.test(t.ctl||'')&&/delegation/.test(t.ctl||'')&&/authorizedUser/.test(t.ctl||'')&&/beneficiary/.test(t.ctl||'')&&/paymentInstrument/.test(t.ctl||''));
check('scoped Steps09-10 runtime has its own PASS marker',/PHASE11ND_RUNTIME_STEPS09_10_PASS/.test(t.r0910||''));
check('scoped Steps09-10 runtime does not execute Steps11-13 APIs',!/regulatory-restrictions|pricing-overrides|tax-exemptions|tax-certificates|compliance-evaluations/.test(t.r0910||''));

check('generic positive closure uses Step05 derived transaction',/ACCOUNT_CLOSURE_SETTLEMENT/.test(t.closureSvc||'')&&/transactionService\.postDerived/.test(t.closureSvc||''));
check('Step05 accepts internal closure settlement type',/DERIVED_TYPES[\s\S]*ACCOUNT_CLOSURE_SETTLEMENT/.test(t.txSvc||''));
check('closure derived transaction carries balanced source and settlement legs',/new DerivedTransactionLeg\(accountId,null,"DEBIT",ledger\)/.test(t.closureSvc||'')&&/new DerivedTransactionLeg\(null,settlementRef,"CREDIT",ledger\)/.test(t.closureSvc||''));
check('closure child idempotency key respects Step05 80-character contract',/candidate\.length\(\)<=80/.test(t.closureSvc||'')||/candidate\.length<=80/.test(t.closureSvc||''));
check('closure settlement item persists owning transaction id',/TRANSACTION_ID=COALESCE\(TRANSACTION_ID,:tx\)/.test(t.closureRepo||'')&&/settleItems\(closureId,transactionId,actor\)/.test(t.closureSvc||''));
check('pre-settled maturity/early closure projects transaction id to settlement item',/transactionIdFromReference\(settlementRef\)/.test(t.closureSvc||''));
check('closure final financial reference is TX id',/postingRef="TX-"\+transactionId/.test(t.closureSvc||''));
check('generic closure no longer posts principal directly through 11D',!/balanceService\.post\(accountId/.test(t.closureSvc||''));
check('closure status history persists approval request id',/insertStatusHistory\(accountId,"ACTIVE","CLOSED"[^\n]+c\.approvalId\(\)/.test(t.closureSvc||'')&&/APPROVAL_REQUEST_ID/.test(t.closureRepo||''));
check('reopening status history persists approval request id',/insertStatusHistory\(accountId,"CLOSED","ACTIVE"[^\n]+r\.approvalId\(\)/.test(t.closureSvc||''));
check('Account 360 status history exposes approval trace',/Long approvalRequestId/.test(t.opsModels||'')&&/APPROVAL_REQUEST_ID/.test(t.opsRepo||'')&&/approvalRequestId:number\|null/.test(t.uiSvc||'')&&/Approval \{\{h\.approvalRequestId/.test(t.uiHtml||''));
check('historical 11E verifier accepts evolved Step05 closure',/evolved Step05 posting primitive/.test(t.v11e||'')&&/evolved Step05 closure settlement/.test(t.v11e||''));
check('11E runtime verifies Step05 closure and approval trace',/CLOSURE_STEP05_SETTLEMENT_TRANSACTION_MISSING/.test(t.r11e||'')&&/ACCOUNT_CLOSURE_SETTLEMENT/.test(t.r11e||'')&&/CLOSURE_STATUS_HISTORY_APPROVAL_TRACE_MISSING/.test(t.r11e||'')&&/REOPEN_STATUS_HISTORY_APPROVAL_TRACE_MISSING/.test(t.r11e||''));

check('11N-D migration contains no business DML',/no business DML/i.test(t.mig||'')&&!/^\s*(INSERT|UPDATE|DELETE|MERGE)\s+/mi.test(t.mig||''));
check('11N-D migration reconciles only Step09-10 Wave C sequences',/SEQ_DEPOSIT_ACCOUNT_INQUIRY/.test(t.mig||'')&&/SEQ_DEPOSIT_ACCOUNT_PAYMENT_INSTRUMENT/.test(t.mig||'')&&!/DEPOSIT_ACCOUNT_REGULATORY_RESTRICTION|DEPOSIT_ACCOUNT_PRICING_OVERRIDE|DEPOSIT_TAX_EXEMPTION/.test(t.mig||''));
check('11N-D DB verifier covers evolved Step05 plus verified 11N-C pre-settled and legacy direct closure trace',/verified 11N-C pre-settled/.test(t.db||'')&&/legacy direct Package17 trace/.test(t.db||'')&&/ACCOUNT_CLOSURE_SETTLEMENT/.test(t.db||''));
check('11N-D DB verifier covers status-history approval trace',/status-history approval/.test(t.db||''));
check('11N-D DB verifier is scoped away from Steps11-13',!/DEPOSIT_ACCOUNT_REGULATORY_RESTRICTION|DEPOSIT_ACCOUNT_PRICING_OVERRIDE|DEPOSIT_TAX_EXEMPTION|DEPOSIT_TAX_CERTIFICATE/.test(t.db||''));

check('Oracle runner supports local SQLPlus or remote JDBC without Docker',/sqlplus/.test(t.runnerCmd||'')&&/OracleJdbcSqlRunner/.test(t.runnerCmd||'')&&!/docker/.test(t.runnerCmd||'')&&/jdbc:oracle:thin:@\//.test(t.runnerJava||''));
check('Oracle JDBC runner drains DBMS_OUTPUT before rethrowing verifier errors',/drainDbmsOutput\(con\)/.test(t.runnerJava||'')&&/addSuppressed/.test(t.runnerJava||''));
check('11N-D apply uses remote-capable Oracle runner',/run-oracle-sql\.cmd/.test(t.apply||'')&&!/docker/.test(t.apply||''));
check('qualifier does not invoke full 11L apply or runtime',!/apply-dps2-phase11l|runtime-dps2-phase11l-wave-c/.test(t.qualify||''));
check('qualifier runs only Steps05-10 Oracle/build/runtime gates',/verify-dps2-step05-transaction-processing-11i/.test(t.qualify||'')&&/verify-dps2-wave-b-statements-limits-maturity-11k/.test(t.qualify||'')&&/apply-dps2-phase11nd/.test(t.qualify||'')&&/runtime-dps2-phase11i-e2e/.test(t.qualify||'')&&/runtime-dps2-phase11k-wave-b-e2e/.test(t.qualify||'')&&/runtime-dps2-phase11nd-steps09-10-e2e/.test(t.qualify||'')&&/runtime-dps2-phase11e-e2e/.test(t.qualify||''));
check('qualifier emits final 11N-D marker',/DPS2_PHASE11ND_STEPS05_10_QUALIFICATION_PASS/.test(t.qualify||''));
check('production build includes Phase 11N-D gate',/verify-dps2-phase11nd-steps05-10-canonical-audit/.test(t.build||''));

console.log('------------------------------------------------------------');
console.log(`PHASE11ND_STATIC_VERIFIER_PASS=${pass}`);
console.log(`PHASE11ND_STATIC_VERIFIER_FAIL=${fail}`);
if(fail)process.exit(1);
console.log('PHASE11ND_STATIC_BASELINE_PASS');
