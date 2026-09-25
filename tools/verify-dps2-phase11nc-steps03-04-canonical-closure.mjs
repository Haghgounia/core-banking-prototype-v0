import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const read=p=>fs.readFileSync(path.join(root,p),'utf8');
let pass=0,fail=0;
function check(label,ok){if(ok){pass++;console.log(`PASS | ${label}`)}else{fail++;console.log(`FAIL | ${label}`)}}
const files={
 road:'docs/DPS2-CANONICAL-ROADMAP-FA.md',
 termSvc:'backend/src/main/java/com/behsazan/corebanking/deposit/account/term/application/DepositTermService.java',
 termRepo:'backend/src/main/java/com/behsazan/corebanking/deposit/account/term/oracle/DepositTermRepository.java',
 termModels:'backend/src/main/java/com/behsazan/corebanking/deposit/account/term/domain/DepositTermModels.java',
 profitSvc:'backend/src/main/java/com/behsazan/corebanking/deposit/account/profit/application/DepositProfitService.java',
 profitRepo:'backend/src/main/java/com/behsazan/corebanking/deposit/account/profit/oracle/DepositProfitRepository.java',
 txSvc:'backend/src/main/java/com/behsazan/corebanking/deposit/account/transaction/application/DepositTransactionService.java',
 txModels:'backend/src/main/java/com/behsazan/corebanking/deposit/account/transaction/domain/DepositTransactionModels.java',
 txRepo:'backend/src/main/java/com/behsazan/corebanking/deposit/account/transaction/oracle/DepositTransactionRepository.java',
 closureSvc:'backend/src/main/java/com/behsazan/corebanking/deposit/account/closure/application/DepositClosureService.java',
 ctl:'backend/src/main/java/com/behsazan/corebanking/deposit/account/operations/web/DepositAccountOperationsController.java',
 mig:'database/oracle/dps2/migrations/0.11.0-phase11nc-steps03-04-canonical-closure.sql',
 db:'database/oracle/dps2/verification/0.11.0-phase11nc-steps03-04-canonical-closure-verifier.sql',
 runtime:'tools/runtime-dps2-phase11nc-steps03-04-e2e.mjs',
 apply:'tools/apply-dps2-phase11nc.cmd',
 qualify:'tools/qualify-dps2-phase11nc.cmd',
 qa:'docs/DPS2-0.11.0-PHASE11NC-STEPS03-04-CANONICAL-CLOSURE-QA.md',
 build:'build-production.cmd'
};
for(const [k,p] of Object.entries(files))check(`11N-C file exists: ${k}`,fs.existsSync(path.join(root,p)));
const t=Object.fromEntries(Object.entries(files).filter(([k,p])=>fs.existsSync(path.join(root,p))).map(([k,p])=>[k,read(p)]));
const balancedParens=s=>{const scrub=s.replace(/'(?:''|[^'])*'/g,"''");let depth=0;for(const ch of scrub){if(ch==='(')depth++;else if(ch===')'){depth--;if(depth<0)return false}}return depth===0};
const legacyPwVerifierBlock=(t.db||'').match(/-- 11N-C cutover rule:[\s\S]*?chk\('executed partial withdrawals retain Step05 or verified legacy 11D trace',v\);/)?.[0]||'';
check('roadmap records 11N-C final closure',/11N-B \| Steps 01–02 Canonical Closure \| CLOSED/.test(t.road||'')&&/11N-C \| Steps 03–04 Canonical Closure \| CLOSED/.test(t.road||''));
check('roadmap marks Step03/04 done after qualification',/\| 03 \|[^\n]+\| DONE \|/.test(t.road||'')&&/\| 04 \|[^\n]+\| DONE \|/.test(t.road||'')&&/DPS2_PHASE11NC_STEPS03_04_QUALIFICATION_PASS/.test(t.road||''));
check('AD-01 canonical profit ownership is documented',/(canonical profile\/period\/payment ownership|Profile.*Period.*Payment)/is.test(t.road||''));
check('migration is reconciliation-only and requires canonical tables',/no business DML/i.test(t.mig||'')&&/req_table\('DEPOSIT_TERM_SETTLEMENT'\)/.test(t.mig||'')&&/req_table\('DEPOSIT_PROFIT_PAYMENT'\)/.test(t.mig||''));
check('partial withdrawal executes through Step05 derived transaction',/transactionService\.postDerived\(new DerivedTransactionRequest\(accountId,"TERM_PARTIAL_WITHDRAWAL"/.test(t.termSvc||''));
check('partial withdrawal persists transaction id',/executePartial\([^\n]+txId/.test(t.termSvc||'')||/executePartial\([\s\S]*?txId/.test(t.termSvc||''));
check('partial withdrawal settlement is POSTED and transaction-linked',/insertPostedSettlement\([^\n]+"PARTIAL_WITHDRAWAL"/.test(t.termSvc||''));
check('maturity execution endpoint exists',/term-operations\/maturity\/execute/.test(t.ctl||''));
check('maturity execution uses Step05 MATURITY_SETTLEMENT',/transactionService\.postDerived\(new DerivedTransactionRequest\(accountId,"MATURITY_SETTLEMENT"/.test(t.termSvc||''));
check('maturity event stores settlement transaction id',/upsertMaturityEvent\([^\n]+txId/.test(t.termSvc||''));
check('maturity settlement persists same transaction',/insertPostedSettlement\([^\n]+"MATURITY"[^\n]+txId/.test(t.termSvc||''));
check('maturity financial branch closes via controlled Package16',/requestClosure\(accountId,new ClosureRequest\("MATURITY"/.test(t.termSvc||'')&&/executePreSettledClosure/.test(t.termSvc||''));
check('maturity renewal branch remains non-settlement renewal',/"RENEW_PRINCIPAL"/.test(t.termSvc||'')&&/executeAutomaticRenewal/.test(t.termSvc||''));
check('maturity WAIT_INSTRUCTION remains pending and non-financial',/"WAIT_INSTRUCTION"/.test(t.termSvc||'')&&/"PENDING"/.test(t.termSvc||''));
check('early termination posts through Step05',/transactionService\.postDerived\(new DerivedTransactionRequest\(accountId,"TERM_EARLY_TERMINATION"/.test(t.termSvc||''));
check('early termination settlement links transaction id',/markCalculatedSettlementPosted\([^\n]+txId/.test(t.termSvc||''));
check('early termination uses controlled pre-settled closure',/executePreSettledClosure\(accountId,Objects\.requireNonNull\(e\.closureId\(\)\),"TX-"\+txId/.test(t.termSvc||''));
check('early termination status stays inside canonical XML vocabulary',!/HANDED_OFF_TO_CLOSURE/.test((t.termSvc||'')+(t.termRepo||'')+read('frontend/src/app/features/four-deposits/deposit-account-operations.component.html'))&&/\"REQUESTED\"\.equals\(upper\(e\.statusCode\(\)\)\)/.test(t.termSvc||'')&&/STATUS_CODE='REQUESTED'/.test(t.termRepo||''));
check('early termination request preserves canonical REQUESTED until approved closure executes',!/linkEarlyTerminationClosure/.test(t.termSvc||'')&&/e.statusCode==='REQUESTED' && e.closureStatusCode==='APPROVED'/.test(read('frontend/src/app/features/four-deposits/deposit-account-operations.component.html')));
check('early termination view resolves controlled closure by shared approval evidence',/C\.APPROVAL_REQUEST_ID=E\.APPROVAL_REQUEST_ID/.test(t.termRepo||''));
check('early termination view no longer uses timestamp-order closure heuristic',!/C\.REQUESTED_AT>=E\.CREATED_AT/.test(t.termRepo||''));
check('migration reconciles status-history reason for canonical early termination',/DEPOSIT_ACCOUNT_STATUS_HISTORY/.test(t.mig||'')&&/TERM_EARLY_TERMINATION/.test(t.mig||'')&&/DROP CONSTRAINT CHK_DEPOSIT_ACCOUNT_STATUS_HISTORY_REASON_CODE/.test(t.mig||'')&&/ADD CONSTRAINT CHK_DEPOSIT_ACCOUNT_STATUS_HISTORY_REASON_CODE/.test(t.mig||''));
check('transaction domain exposes derived transaction request and legs',/record DerivedTransactionRequest/.test(t.txModels||'')&&/record DerivedTransactionLeg/.test(t.txModels||''));
check('derived transaction service creates authorization evidence',/DERIVED_AUTHORIZATION_APPROVED/.test(t.txSvc||'')&&/postDerived\(/.test(t.txSvc||''));
check('derived transaction repository persists supplied authorization type while preserving legacy maker-checker overload',/authorization\(long id,long txId,String requestedBy,String decidedBy,String reason,String actor\)\{authorization\(id,txId,\"MAKER_CHECKER\",requestedBy,decidedBy,reason,actor\);\}/.test(t.txRepo||'')&&/authorization\(long id,long txId,String authorizationType,String requestedBy,String decidedBy,String reason,String actor\)/.test(t.txRepo||'')&&/:authType/.test(t.txRepo||'')&&/addValue\(\"authType\",authorizationType\)/.test(t.txRepo||''));
check('derived transaction service posts deposit legs through 11D',/balanceService\.post\(/.test(t.txSvc||'')&&/DEPOSIT_TRANSACTION/.test(t.txSvc||''));
check('profit service exposes outstanding payable for term workflows',/outstandingPayable\(long accountId\)/.test(t.profitSvc||''));
check('profit maturity helper posts through Step05',/postOutstandingViaStep05/.test(t.profitSvc||'')&&/transactionService\.postDerived/.test(t.profitSvc||''));
check('profit external destinations use Step05',/LINKED_ACCOUNT/.test(t.profitSvc||'')&&/CUSTOMER_SELECTED_ACCOUNT/.test(t.profitSvc||'')&&/"PROFIT_PAYMENT"/.test(t.profitSvc||''));
check('same-deposit profit remains direct canonical posting',/"SAME_DEPOSIT"/.test(t.profitSvc||'')&&/P11H-PROFIT-/.test(t.profitSvc||''));
check('profit payment records Step05 transaction reference',/postingReference="TX-"\+txId/.test(t.profitSvc||'')||/"TX-"\+transactionId/.test(t.profitSvc||''));
check('profit payment remains owned by Profit Period',/insertPayment\(period\.profitPeriodId\(\)/.test(t.profitSvc||'')&&/applyPeriodPayment/.test(t.profitSvc||''));
check('profit accrual remains owned by Profit Period',/PROFIT_PERIOD_ID/.test(t.profitRepo||'')&&/accrual/i.test(t.profitRepo||''));
check('DB verifier checks partial withdrawal Step05 trace with verified legacy 11D compatibility',/executed partial withdrawals retain Step05 or verified legacy 11D trace/.test(t.db||'')&&/POSTING_REFERENCE='TERM-PW-'/.test(t.db||'')&&/SOURCE_ENTITY_TYPE='TERM_PARTIAL_WITHDRAWAL'/.test(t.db||''));
check('legacy partial-withdrawal verifier predicate has balanced parentheses',Boolean(legacyPwVerifierBlock)&&balancedParens(legacyPwVerifierBlock));
check('Oracle fail-fast codes stay inside raise_application_error range',/-20401/.test(t.mig||'')&&/-20402/.test(t.mig||'')&&/-20420/.test(t.db||'')&&!/-21\d{3}/.test((t.mig||'')+(t.db||'')));
check('11N-C migration still performs no legacy business backfill',!/\b(INSERT|UPDATE|DELETE|MERGE)\s+INTO?\s+DPS2\.DEPOSIT_TERM_PARTIAL_WITHDRAWAL/i.test(t.mig||''));
check('DB verifier checks maturity transaction and settlement coherence',/financial maturity event and term settlement share transaction id/.test(t.db||''));
check('DB verifier checks early termination transaction and closure coherence',/executed early termination has POSTED Step05 transaction/.test(t.db||'')&&/controlled Package16 trace/.test(t.db||'')&&/shared approval evidence/.test(t.db||''));
check('DB verifier checks early termination status-history reason contract',/status-history reason constraint accepts documented early termination reason/.test(t.db||'')&&/SEARCH_CONDITION_VC/.test(t.db||''));
check('DB verifier checks derived authorization and subledger trace',/derived Step05 transactions retain independent authorization evidence/.test(t.db||'')&&/reaches subledger/.test(t.db||''));
check('DB verifier checks external profit Step05 trace',/canonical profit payments use Step05 transaction reference/.test(t.db||''));
check('DB verifier enforces canonical profit period ownership',/canonical accruals remain owned by Profit Period/.test(t.db||'')&&/canonical payments remain owned by Profit Period/.test(t.db||''));
check('runtime covers positive partial withdrawal trace',/PHASE11NC_RUNTIME_PARTIAL_WITHDRAWAL=/.test(t.runtime||''));
check('runtime covers positive maturity execution trace',/PHASE11NC_RUNTIME_MATURITY=/.test(t.runtime||''));
check('runtime covers external profit payment through Step05',/PHASE11NC_RUNTIME_EXTERNAL_PROFIT=/.test(t.runtime||''));
check('runtime covers early termination through Step05 and closure',/PHASE11NC_RUNTIME_EARLY_TERMINATION=/.test(t.runtime||''));
check('runtime has final PASS marker',/PHASE11NC_RUNTIME_E2E_PASS/.test(t.runtime||''));
check('historical 11H verifier accepts actual Step05 external profit execution',read('tools/verify-dps2-step04-canonical-profit-11h.mjs').includes('const step05ExternalPayment=')&&read('tools/verify-dps2-step04-canonical-profit-11h.mjs').includes('transactionService.postDerived')&&read('tools/verify-dps2-step04-canonical-profit-11h.mjs').includes('PROFIT_PAYMENT'));
check('historical 11H UI verifier accepts evolved Step05 execution wording',read('tools/verify-dps2-step04-canonical-profit-11h.mjs').includes('const step05ExternalPaymentUi=')&&read('tools/verify-dps2-step04-canonical-profit-11h.mjs').includes("t.uiHtml.includes('Step 05')&&t.uiHtml.includes('Transaction')"));
check('historical 11K verifier accepts evolved maturity upsert semantics',read('tools/verify-dps2-wave-b-statements-limits-maturity-11k.mjs').includes('const evolvedMaturityProgress=')&&read('tools/verify-dps2-wave-b-statements-limits-maturity-11k.mjs').includes('processedSql=processed?\"SYSTIMESTAMP\":\"NULL\"')&&read('tools/verify-dps2-wave-b-statements-limits-maturity-11k.mjs').includes('legacyMaturityProgress||evolvedMaturityProgress'));
check('apply helper wires static migration DB verifier',/verify-dps2-phase11nc-steps03-04-canonical-closure\.mjs/.test(t.apply||'')&&/phase11nc-steps03-04-canonical-closure\.sql/.test(t.apply||''));
check('qualifier uses Oracle -> build -> runtime order',/apply-dps2-phase11nc\.cmd/.test(t.qualify||'')&&/build-production\.cmd/.test(t.qualify||'')&&/runtime-dps2-phase11nc-steps03-04-e2e\.mjs/.test(t.qualify||''));
check('production build includes Phase 11N-C gate',/verify-dps2-phase11nc-steps03-04-canonical-closure\.mjs/.test(t.build||''));
console.log('------------------------------------------------------------');
console.log(`PHASE11NC_STATIC_VERIFIER_PASS=${pass}`);console.log(`PHASE11NC_STATIC_VERIFIER_FAIL=${fail}`);
if(fail){process.exit(1)}console.log('PHASE11NC_STATIC_BASELINE_PASS');
