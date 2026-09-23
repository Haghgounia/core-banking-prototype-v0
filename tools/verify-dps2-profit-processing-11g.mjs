import fs from 'node:fs';
const read=p=>fs.readFileSync(p,'utf8');
const exists=p=>fs.existsSync(p);
let pass=0,fail=0;const check=(label,ok)=>{console.log(`${ok?'PASS':'FAIL'} | ${label}`);ok?pass++:fail++};
const pkg=JSON.parse(read('frontend/package.json'));
const files={
 models:'backend/src/main/java/com/behsazan/corebanking/deposit/account/profit/domain/DepositProfitModels.java',
 svc:'backend/src/main/java/com/behsazan/corebanking/deposit/account/profit/application/DepositProfitService.java',
 provision:'backend/src/main/java/com/behsazan/corebanking/deposit/account/profit/application/DepositProfitContractProvisioningService.java',
 repo:'backend/src/main/java/com/behsazan/corebanking/deposit/account/profit/oracle/DepositProfitRepository.java',
 lifecycle:'backend/src/main/java/com/behsazan/corebanking/deposit/account/application/DepositAccountLifecycleService.java',
 controller:'backend/src/main/java/com/behsazan/corebanking/deposit/account/operations/web/DepositAccountOperationsController.java',
 migration:'database/oracle/dps2/migrations/0.11.0-phase11g-profit-interest-processing.sql',
 dbver:'database/oracle/dps2/verification/0.11.0-phase11g-profit-interest-processing-verifier.sql',
 runtime:'tools/runtime-dps2-phase11g-e2e.mjs',
 pdlRepair:'tools/reconcile-pdl-profit-config-phase11g.mjs',
 apply:'tools/apply-dps2-phase11g.cmd',
 uiService:'frontend/src/app/features/four-deposits/deposit-account-operations.service.ts',
 uiTs:'frontend/src/app/features/four-deposits/deposit-account-operations.component.ts',
 uiHtml:'frontend/src/app/features/four-deposits/deposit-account-operations.component.html',
 regression:'tools/runtime-dps2-phase11f-e2e.mjs',
 qa:'docs/DPS2-0.11.0-PHASE11G-PROFIT-INTEREST-PROCESSING-QA.md',
 install:'docs/DPS2-0.11.0-PHASE11G-INSTALL-FA.md',
 pricingHotfix:'docs/DPS2-0.11.0-PHASE11G-OPENING-PRICING-TRACE-HOTFIX-QA.md',
 dualWriteHotfix:'docs/DPS2-0.11.0-PHASE11G-LEGACY-ACCRUAL-DUAL-WRITE-QA.md',
 build:'build-production.cmd'
};
for(const [k,p] of Object.entries(files))check(`11G file exists: ${k}`,exists(p));
const t={};for(const [k,p] of Object.entries(files))t[k]=exists(p)?read(p):'';
check('VERSION remains 0.11.0',pkg.version==='0.11.0');
check('profit contract domain exists',t.models.includes('record ProfitContract('));
check('profit accrual domain exists',t.models.includes('record ProfitAccrual('));
check('profit posting domain exists',t.models.includes('record ProfitPosting('));
check('profit view aggregates contract/accrual/posting',t.models.includes('record ProfitView(ProfitContract contract,List<ProfitAccrual> accruals,List<ProfitPosting> postings)'));
check('activation provisions operational profit contract',t.lifecycle.includes('profitContractProvisioningService.ensureForActivation'));
check('term families require profit snapshot at activation',t.provision.includes('SHORT_TERM_DEPOSIT')&&t.provision.includes('LONG_TERM_DEPOSIT')&&t.provision.includes('Activation حساب مدت‌دار بدون Profit Instruction'));
check('profit contract is provisioned from Opening snapshot',t.repo.includes('DEPOSIT_OPENING_PROFIT_INSTRUCTION')&&t.repo.includes('SOURCE_OPENING_PROFIT_INSTRUCTION_ID'));
check('profit provisioning preserves product trace identifiers',t.repo.includes('PRICING_RULE_ID')&&t.repo.includes('PRICING_COMPONENT_ID')&&t.repo.includes('RATE_TIER_ID')&&t.repo.includes('PROFIT_PAYMENT_RULE_ID'));
check('term contract rate is synchronized from profit snapshot',t.repo.includes('syncTermRate')&&t.repo.includes('DEPOSIT_TERM_CONTRACT'));
check('11G accrual uses current 11D ledger balance basis',t.svc.includes('repository.ledgerBalance(accountId)'));
check('11G supports percentage method',t.svc.includes('PERCENTAGE'));
check('11G supports ACT_365 day count',t.svc.includes('ACT_365')&&t.svc.includes('365'));
check('11G supports ACT_360 day count',t.svc.includes('ACT_360')&&t.svc.includes('360'));
check('accrual is bounded by contract effective-to date',t.svc.includes('c.effectiveTo()!=null&&through.isAfter(c.effectiveTo())'));
check('accrual stores calculation evidence',t.repo.includes('ACCRUAL_FROM_DATE')&&t.repo.includes('BASIS_AMOUNT')&&t.repo.includes('ANNUAL_RATE'));
check('same-deposit profit posting uses 11D posting primitive',t.svc.includes('balanceService.post(')&&t.svc.includes('DEPOSIT_PROFIT_POSTING'));
check('external profit destination is deferred to 11H',t.svc.includes('Phase 11H')&&t.svc.includes('SAME_DEPOSIT'));
check('11G does not create DEPOSIT_TRANSACTION rows',!t.svc.includes('DEPOSIT_TRANSACTION')&&!t.repo.includes('INSERT INTO DPS2.DEPOSIT_TRANSACTION')&&!t.migration.includes('INSERT INTO DPS2.DEPOSIT_TRANSACTION'));
check('accrual mutation requires idempotency',t.controller.includes('/profit/accruals')&&t.controller.includes('X-Idempotency-Key'));
check('posting mutation requires idempotency',t.controller.includes('/profit/postings')&&t.controller.includes('X-Idempotency-Key'));
check('11G idempotency is persisted in shared operation table',t.repo.includes('DEPOSIT_OPERATION_IDEMPOTENCY')&&t.repo.includes("'PROFIT_ENGINE'"));
check('posting idempotency hash is request-stable',t.svc.includes('hash("POST_PROFIT|"+accountId+"|"+date)')&&!t.svc.includes('POST_PROFIT|"+accountId+"|"+date+"|"+c.accruedAmount'));
check('profit GET API exists',t.controller.includes('@GetMapping("/{accountId}/profit")'));
check('profit accrual API exists',t.controller.includes('@PostMapping("/{accountId}/profit/accruals")'));
check('profit posting API exists',t.controller.includes('@PostMapping("/{accountId}/profit/postings")'));
check('migration creates profit contract table',t.migration.includes('CREATE TABLE DPS2.DEPOSIT_PROFIT_CONTRACT'));
check('migration creates accrual table',t.migration.includes('CREATE TABLE DPS2.DEPOSIT_PROFIT_ACCRUAL'));
check('migration creates posting table',t.migration.includes('CREATE TABLE DPS2.DEPOSIT_PROFIT_POSTING'));
check('migration creates active-contract uniqueness',t.migration.includes('UX_DEP_PROFIT_ACTIVE_ACCOUNT'));
check('migration links accrual rows to profit postings by FK',t.migration.includes('FK_DEP_PROFIT_ACCR_POST')&&t.migration.includes('FOREIGN KEY(PROFIT_POSTING_ID) REFERENCES DPS2.DEPOSIT_PROFIT_POSTING'));
check('migration reconciles legacy profit table schemas before indexes and FKs',t.migration.includes('full Repository contract')&&t.migration.includes("ensure_col('DEPOSIT_PROFIT_ACCRUAL','ACCRUAL_TO_DATE'")&&t.migration.includes("ensure_col('DEPOSIT_PROFIT_ACCRUAL','PROFIT_POSTING_ID'")&&t.migration.includes("ensure_col('DEPOSIT_PROFIT_CONTRACT','LAST_PAYMENT_DATE'")&&t.migration.includes("ensure_col('DEPOSIT_PROFIT_POSTING','SUBLEDGER_ENTRY_ID'"));
check('legacy profit reconciliation refuses to fabricate required financial data',t.migration.includes('Manual legacy-data mapping is required; no financial value was fabricated.'));
check('legacy accrual PROFIT_PERIOD_ID is reconciled without fabricating period identity',t.migration.includes('PROFIT_PERIOD_ID legacy NOT NULL relaxed')&&t.migration.includes('Manual legacy-period mapping is required; no period identifier was fabricated.')&&t.dbver.includes('legacy PROFIT_PERIOD_ID does not block canonical accrual inserts'));
check('legacy accrual compatibility is dual-written from canonical 11G values',t.repo.includes('hasLegacyAccrualProjection')&&t.repo.includes('ACCRUAL_DATE,BALANCE_BASIS_AMOUNT,RATE_VALUE,DAY_FRACTION,ACCRUAL_AMOUNT,ACCRUAL_STATUS_CODE,CREATED_BY')&&t.repo.includes("'CALCULATED'")&&t.repo.includes('dayFraction'));
check('posting synchronizes canonical and legacy accrual statuses',t.repo.includes("ACCRUAL_STATUS_CODE='POSTED'")&&t.repo.includes('POSTING_REFERENCE=:ref')&&t.repo.includes('P11G-PROFIT-'));
check('DB verifier validates legacy/canonical accrual projection coherence',t.dbver.includes('legacy accrual projection is dual-written from canonical 11G values')&&t.dbver.includes('legacy accrual projection diverges from canonical 11G values'));
check('legacy accrual schema detection rejects partial compatibility projection',t.repo.includes('expected 0 or 7 compatibility columns')&&t.repo.includes('Incomplete legacy DEPOSIT_PROFIT_ACCRUAL projection'));
check('migration execute-immediate helper uses VARCHAR2',t.migration.includes('PROCEDURE ddl(p_sql VARCHAR2)'));
check('migration backfills only explicit Opening profit snapshots',t.migration.includes('JOIN DPS2.DEPOSIT_OPENING_PROFIT_INSTRUCTION')&&t.migration.includes('P.RATE_VALUE IS NOT NULL'));
check('migration does not fabricate product rate',t.migration.includes('No product rate is fabricated here'));
check('DB verifier checks profit tables',t.dbver.includes('DEPOSIT_PROFIT_CONTRACT')&&t.dbver.includes('DEPOSIT_PROFIT_ACCRUAL')&&t.dbver.includes('DEPOSIT_PROFIT_POSTING'));
check('DB verifier checks evolved 11G repository columns',t.dbver.includes("col('DEPOSIT_PROFIT_ACCRUAL','ACCRUAL_TO_DATE')")&&t.dbver.includes("col('DEPOSIT_PROFIT_CONTRACT','LAST_PAYMENT_DATE')")&&t.dbver.includes("col('DEPOSIT_PROFIT_POSTING','SUBLEDGER_ENTRY_ID')"));
check('DB verifier checks 11D subledger traceability',t.dbver.includes('same-deposit profit postings trace to 11D subledger'));
check('DB verifier checks profit FK and active-contract uniqueness',t.dbver.includes('accrual-to-posting FK enabled')&&t.dbver.includes('one-active-profit-contract unique index exists'));
check('Angular profit contracts exist',t.uiService.includes('DepositProfitContract')&&t.uiService.includes('DepositProfitView'));
check('Angular profit API methods exist',t.uiService.includes('accrueProfit(')&&t.uiService.includes('postProfit('));
check('11G UI card exists',t.uiHtml.includes('Profit / Interest Processing')&&t.uiHtml.includes('Phase 11G'));
check('UI blocks external-destination posting',t.uiHtml.includes("paymentDestinationCode!=='SAME_DEPOSIT'"));
check('UI communicates 11D/11H boundaries',t.uiHtml.includes('Subledger primitive فاز 11D')&&t.uiHtml.includes('Phase 11H'));
check('UI defaults accrual-through to current date rather than last accrual date',t.uiTs.includes("const today=new Date().toISOString().slice(0,10)")&&t.uiTs.includes('this.profitAccrualThrough=p.contract.effectiveTo'));
check('runtime bootstrap preserves governed maturity action',t.runtime.includes('maturityActionCode'));
check('runtime bootstrap uses governed opening instruction source code',t.runtime.includes("INSTRUCTION_SOURCE_CODE:'PRODUCT_DEFAULT'")&&!t.runtime.includes("INSTRUCTION_SOURCE_CODE:'PRODUCT'"));
check('runtime bootstrap resolves governed pricing and profit-payment identifiers',t.runtime.includes('resolveProfitConfiguration')&&t.runtime.includes("pdlDescriptor('PRODUCT_PRICING_RULE')")&&t.runtime.includes("pdlDescriptor('DEPOSIT_PROFIT_PAYMENT_RULE')")&&t.runtime.includes('PRICING_RULE_ID:profitConfig.pricingRuleId')&&t.runtime.includes('PROFIT_PAYMENT_RULE_ID:profitConfig.profitPaymentRuleId'));
check('runtime bootstrap does not fabricate nullable pricing identifiers',!t.runtime.includes('PRICING_RULE_ID:null')&&!t.runtime.includes('PROFIT_PAYMENT_RULE_ID:null'));
check('11G PDL profit reconciliation tool exists',t.pdlRepair.includes('PHASE11G_PDL_PROFIT_REPAIR_PASS')&&t.pdlRepair.includes('PRODUCT_PRICING_RULE')&&t.pdlRepair.includes('PRODUCT_PRICING_COMPONENT')&&t.pdlRepair.includes('DEPOSIT_PROFIT_PAYMENT_RULE'));
check('PDL profit reconciliation is metadata-driven and dry-run by default',t.pdlRepair.includes('/descriptor')&&t.pdlRepair.includes('requiredColumns')&&t.pdlRepair.includes("process.argv.includes('--apply')")&&t.pdlRepair.includes('DRY_RUN'));
check('PDL profit reconciliation preserves governed rate trace',t.pdlRepair.includes('PRODUCT_RATE_TIER')&&t.pdlRepair.includes('resolvedRate')&&t.pdlRepair.includes('pricingRuleId')&&t.pdlRepair.includes('profitPaymentRuleId'));
check('runtime fails fast on profit storage/schema 500',t.runtime.includes('PHASE11G_PROFIT_STORAGE_NOT_READY_OR_SCHEMA_MISMATCH')&&t.runtime.includes('e.status===500'));
check('canonical production build includes Phase 11D-G gates',t.build.includes('verify-dps2-balance-subledger-11d.mjs')&&t.build.includes('verify-dps2-closure-reopening-11e.mjs')&&t.build.includes('verify-dps2-term-operations-11f.mjs')&&t.build.includes('verify-dps2-profit-processing-11g.mjs'));
check('11F bootstrap regression now includes governed profit instruction',t.regression.includes('DEPOSIT_OPENING_PROFIT_INSTRUCTION')&&t.regression.includes('resolveProfitConfiguration')&&t.regression.includes('PRICING_RULE_ID:profitConfig.pricingRuleId')&&!t.regression.includes('PRICING_RULE_ID:null'));
check('11G runtime E2E exists',t.runtime.includes('PHASE11G_RUNTIME_E2E_PASS'));
check('runtime E2E self-provisions profit-bearing term account',t.runtime.includes('bootstrapProfitAccount')&&t.runtime.includes('DEPOSIT_OPENING_PROFIT_INSTRUCTION'));
check('runtime E2E verifies accrual idempotency',t.runtime.includes('ACCRUAL_IDEMPOTENCY_REPLAY_MISSING'));
check('runtime E2E verifies posting idempotency',t.runtime.includes('POSTING_IDEMPOTENCY_REPLAY_MISSING'));
check('runtime E2E verifies profit affects ledger',t.runtime.includes('PROFIT_LEDGER_DELTA_MISMATCH'));
check('runtime E2E verifies posting-to-subledger trace',t.runtime.includes('PROFIT_SUBLEDGER_TRACE_MISMATCH'));
check('Windows 11G apply helper exists',t.apply.includes('Phase 11G')&&t.apply.includes('verify-dps2-profit-processing-11g.mjs'));
check('11G QA document exists',t.qa.includes('Phase 11G')&&t.qa.includes('Profit / Interest'));
check('11G install runbook includes governed pricing reconciliation',t.install.includes('reconcile-pdl-profit-config-phase11g.mjs')&&t.install.includes('runtime-dps2-phase11g-e2e.mjs'));
check('11G opening pricing trace hotfix QA exists',t.pricingHotfix.includes('PRICING_RULE_ID')&&t.pricingHotfix.includes('PRODUCT_PRICING_RULE')&&t.pricingHotfix.includes('DEPOSIT_PROFIT_PAYMENT_RULE'));
check('11G legacy accrual dual-write QA exists',t.dualWriteHotfix.includes('Dual-Write Compatibility')&&t.dualWriteHotfix.includes('DAY_FRACTION')&&t.dualWriteHotfix.includes('PROFIT_PERIOD_ID'));
console.log('------------------------------------------------------------');console.log(`PHASE11G_STATIC_VERIFIER_PASS=${pass}`);console.log(`PHASE11G_STATIC_VERIFIER_FAIL=${fail}`);if(fail){console.log('PHASE11G_STATIC_BASELINE_FAIL');process.exit(1)}console.log('PHASE11G_STATIC_BASELINE_PASS');
