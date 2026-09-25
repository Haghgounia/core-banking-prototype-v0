import fs from 'node:fs';
const read=p=>fs.readFileSync(p,'utf8'),exists=p=>fs.existsSync(p);let pass=0,fail=0;const check=(label,ok)=>{console.log(`${ok?'PASS':'FAIL'} | ${label}`);ok?pass++:fail++};
const files={
 roadmap:'docs/DPS2-CANONICAL-ROADMAP-FA.md',models:'backend/src/main/java/com/behsazan/corebanking/deposit/account/profit/domain/DepositProfitModels.java',
 svc:'backend/src/main/java/com/behsazan/corebanking/deposit/account/profit/application/DepositProfitService.java',provision:'backend/src/main/java/com/behsazan/corebanking/deposit/account/profit/application/DepositProfitContractProvisioningService.java',
 repo:'backend/src/main/java/com/behsazan/corebanking/deposit/account/profit/oracle/DepositProfitRepository.java',controller:'backend/src/main/java/com/behsazan/corebanking/deposit/account/operations/web/DepositAccountOperationsController.java',
 migration:'database/oracle/dps2/migrations/0.11.0-phase11h-step04-canonical-profit-completion.sql',dbver:'database/oracle/dps2/verification/0.11.0-phase11h-step04-canonical-profit-completion-verifier.sql',
 runtime:'tools/runtime-dps2-phase11h-e2e.mjs',apply:'tools/apply-dps2-phase11h.cmd',uiService:'frontend/src/app/features/four-deposits/deposit-account-operations.service.ts',
 uiTs:'frontend/src/app/features/four-deposits/deposit-account-operations.component.ts',uiHtml:'frontend/src/app/features/four-deposits/deposit-account-operations.component.html',qa:'docs/DPS2-0.11.0-PHASE11H-STEP04-CANONICAL-PROFIT-QA.md',build:'build-production.cmd'};
for(const [k,p] of Object.entries(files))check(`11H file exists: ${k}`,exists(p));const t={};for(const [k,p] of Object.entries(files))t[k]=exists(p)?read(p):'';
check('canonical roadmap registers Phase 11H before implementation',t.roadmap.includes('Phase 11H — Step 04 Canonical Profit Completion')&&t.roadmap.includes('Operational Step 04'));
check('roadmap keeps Step 04 PARTIAL until external destinations use Step 05',t.roadmap.includes('Step 04 تا زمانی')&&t.roadmap.includes('Step 05'));
check('profit profile domain exists',t.models.includes('record ProfitProfile('));
check('profit period domain exists',t.models.includes('record ProfitPeriod('));
check('profit accrual detail domain exists',t.models.includes('record ProfitAccrualDetail('));
check('profit adjustment domain exists',t.models.includes('record ProfitAdjustment('));
check('profit payment domain exists',t.models.includes('record ProfitPayment('));
check('profit view exposes canonical Step 04 aggregates',t.models.includes('ProfitProfile profile')&&t.models.includes('List<ProfitPeriod> periods')&&t.models.includes('List<ProfitAccrualDetail> accrualDetails')&&t.models.includes('List<ProfitAdjustment> adjustments')&&t.models.includes('List<ProfitPayment> payments'));
check('activation provisions canonical account profit profile',t.provision.includes('provisionProfileFromOpening')&&t.repo.includes('DEPOSIT_ACCOUNT_PROFIT_PROFILE'));
check('profile is sourced from opening profit snapshot',t.repo.includes('SOURCE_OPENING_PROFIT_INST_ID')&&t.repo.includes('DEPOSIT_OPENING_PROFIT_INSTRUCTION'));
check('accrual resolves or creates profit period',t.svc.includes('ensurePeriod(')&&t.repo.includes('periodForDate(')&&t.repo.includes('createPeriod('));
check('accrual writes PROFIT_PERIOD_ID',t.repo.includes('PROFIT_ACCRUAL_ID,PROFIT_PERIOD_ID')&&t.svc.includes('period.profitPeriodId()'));
check('accrual creates BASE_PROFIT detail evidence',t.repo.includes('insertAccrualDetail')&&t.repo.includes("'BASE_PROFIT'"));
check('profit period totals are updated atomically with accrual',t.repo.includes('applyPeriodAccrual')&&t.svc.includes('applyPeriodAccrual'));
check('adjustment request requires source accrual/payment',t.svc.includes('ORIGINAL_ACCRUAL_ID')&&t.svc.includes('ORIGINAL_PAYMENT_ID')&&t.svc.includes('دقیقاً یکی'));
check('adjustment supports INCREASE DECREASE REVERSAL',t.svc.includes('INCREASE')&&t.svc.includes('DECREASE')&&t.svc.includes('REVERSAL'));
check('adjustment uses maker-checker approval request',t.repo.includes('DEPOSIT_OPERATION_APPROVAL_REQUEST')&&t.repo.includes("'PROFIT_ADJUSTMENT'")&&t.svc.includes('approveAdjustment'));
check('adjustment maker and approver must differ',t.svc.includes('Maker و Approver تعدیل سود باید متفاوت باشند')&&t.dbver.includes('A.REQUESTED_BY_USER_ID=A.APPROVER_USER_ID'));
check('adjustment posting is non-destructive',t.svc.includes('applyPeriodAdjustment')&&t.svc.includes('applyContractAdjustment')&&!t.svc.includes('DELETE FROM'));
check('same-deposit payment creates canonical DEPOSIT_PROFIT_PAYMENT',t.repo.includes('insertPayment')&&t.repo.includes('DEPOSIT_PROFIT_PAYMENT')&&t.svc.includes('repository.insertPayment'));
check('same-deposit payment still uses 11D posting primitive',t.svc.includes('balanceService.post(')&&t.svc.includes('DEPOSIT_PROFIT_POSTING'));
check('payment trace updates canonical period and contract',t.svc.includes('applyPeriodPayment')&&t.svc.includes('applyPosting'));
check('paid period reopens when new accrual becomes payable',t.repo.includes("PERIOD_STATUS_CODE='CLOSED' THEN 'CLOSED'")&&t.repo.includes("THEN 'CALCULATED'")&&t.repo.includes("P2.PERIOD_STATUS_CODE<>'CLOSED'"));
check('posted adjustment reopens paid period for payment',t.repo.includes("THEN 'APPROVED'")&&t.repo.includes("NVL(PAYABLE_AMOUNT,0)+:delta>NVL(PAID_AMOUNT,0)"));

const step05ExternalPayment=((t.svc.includes('Step 05 / Transaction Processing'))||(t.svc.includes('transactionService.postDerived')&&t.svc.includes('PROFIT_PAYMENT')))&&t.roadmap.includes('LINKED_ACCOUNT')&&t.roadmap.includes('CUSTOMER_SELECTED_ACCOUNT');
check('external destinations explicitly depend on or execute through Step 05',step05ExternalPayment);
check('adjustment APIs exist',t.controller.includes('/profit/adjustments')&&t.controller.includes('/approve')&&t.controller.includes('/post'));
check('migration creates or preserves five document Step 04 tables',t.migration.includes('DEPOSIT_ACCOUNT_PROFIT_PROFILE')&&t.migration.includes('DEPOSIT_PROFIT_PERIOD')&&t.migration.includes('DEPOSIT_PROFIT_ACCRUAL_DETAIL')&&t.migration.includes('DEPOSIT_PROFIT_ADJUSTMENT')&&t.migration.includes('DEPOSIT_PROFIT_PAYMENT'));
check('migration metadata checks are DPS2-owner aware under SYSTEM connection',t.migration.includes("FROM all_tables WHERE owner='DPS2'")&&t.migration.includes("FROM all_sequences WHERE sequence_owner='DPS2'")&&!t.migration.includes('FROM user_tables')&&!t.migration.includes('FROM user_sequences'));
check('migration creates missing sequences above existing table maxima',t.migration.includes('SELECT NVL(MAX(')&&t.migration.includes("START WITH '||TO_CHAR(v_start)"));
check('migration backfills profile from active contract/opening trace',t.migration.includes('Backfilling canonical Profit Profile')&&t.migration.includes('SOURCE_OPENING_PROFIT_INSTRUCTION_ID'));
check('migration maps existing accruals to period',t.migration.includes('Mapping canonical accruals to Profit Period')&&t.migration.includes('A.PROFIT_PERIOD_ID IS NULL'));
check('migration backfills accrual detail',t.migration.includes('Backfilling Accrual Detail evidence'));
check('migration backfills payment evidence for prior 11G postings',t.migration.includes('Backfilling Payment evidence for existing 11G postings'));
check('DB verifier checks canonical profile/contract coherence',t.dbver.includes('profile and operational contract snapshots are coherent'));
check('DB verifier checks accrual-period-detail trace',t.dbver.includes('canonical accruals trace to profit period')&&t.dbver.includes('BASE_PROFIT detail evidence'));
check('DB verifier checks adjustment approval trace',t.dbver.includes('approved/posted adjustments have approved maker-checker trace'));
check('DB verifier checks payment/posting trace',t.dbver.includes('same-deposit postings have canonical profit payment evidence'));
check('DB verifier rejects PAID period with outstanding balance',t.dbver.includes('PAID profit periods have no outstanding payable balance'));
check('migration reconciles reopened PAID period state',t.migration.includes('Reconciling reopened payable Profit Period status')&&t.migration.includes("PERIOD_STATUS_CODE='PAID'")&&t.migration.includes('PAYABLE_AMOUNT>PAID_AMOUNT'));

check('Angular Step 04 view exposes profile period adjustment payment',t.uiService.includes('DepositProfitProfile')&&t.uiService.includes('DepositProfitPeriod')&&t.uiService.includes('DepositProfitAdjustment')&&t.uiService.includes('DepositProfitPayment'));
check('Angular adjustment actions are wired',t.uiService.includes('requestProfitAdjustment')&&t.uiTs.includes('requestProfitAdjustment()')&&t.uiHtml.includes('ثبت درخواست تعدیل'));
check('Angular payment action uses canonical Step 04 wording',t.uiHtml.includes('پرداخت سود قابل پرداخت')&&t.uiHtml.includes('Payment History'));
const step05ExternalPaymentUi=t.uiHtml.includes('Step 05 / Transaction Processing')||(t.uiHtml.includes('Step 05')&&t.uiHtml.includes('Transaction'));
check('UI states Step 05 dependency/execution for external payment',step05ExternalPaymentUi);
check('runtime 11H verifies profile period detail payment',t.runtime.includes('PHASE11H_RUNTIME_E2E_PASS')&&t.runtime.includes('CANONICAL_PROFILE_MISSING')&&t.runtime.includes('PROFIT_PERIOD_TRACE_MISSING')&&t.runtime.includes('ACCRUAL_DETAIL_TRACE_MISSING')&&t.runtime.includes('PROFIT_PAYMENT_TRACE_MISSING'));
check('runtime 11H verifies adjustment maker-checker flow',t.runtime.includes('ADJUSTMENT_APPROVAL_MISSING')&&t.runtime.includes('ADJUSTMENT_NOT_POSTED'));
check('runtime 11H qualifies reopened legacy payable period before fresh-account flow',t.runtime.includes('qualifyOutstandingPayablePeriod')&&t.runtime.includes('PHASE11H_RUNTIME_REOPENED_PERIOD_PASS')&&t.runtime.includes('REOPENED_PERIOD_NOT_SETTLED'));

check('production build includes Phase 11H static gate',t.build.includes('verify-dps2-step04-canonical-profit-11h.mjs'));
check('11H apply helper wires static migration DB verifier',t.apply.includes('verify-dps2-step04-canonical-profit-11h.mjs')&&t.apply.includes('phase11h-step04-canonical-profit-completion.sql')&&t.apply.includes('phase11h-step04-canonical-profit-completion-verifier.sql'));
check('11H QA anchors scope to Step 04 document trace',t.qa.includes('Step 04')&&t.qa.includes('DEPOSIT_ACCOUNT_PROFIT_PROFILE')&&t.qa.includes('DEPOSIT_PROFIT_PAYMENT'));
console.log('------------------------------------------------------------');console.log(`PHASE11H_STATIC_VERIFIER_PASS=${pass}`);console.log(`PHASE11H_STATIC_VERIFIER_FAIL=${fail}`);if(fail){console.log('PHASE11H_STATIC_BASELINE_FAIL');process.exit(1)}console.log('PHASE11H_STATIC_BASELINE_PASS');
