import fs from 'node:fs';
const files={
 models:'backend/src/main/java/com/behsazan/corebanking/deposit/account/wavec/domain/DepositAccountWaveCModels.java',
 svc:'backend/src/main/java/com/behsazan/corebanking/deposit/account/wavec/application/DepositAccountWaveCService.java',
 repo:'backend/src/main/java/com/behsazan/corebanking/deposit/account/wavec/oracle/DepositAccountWaveCRepository.java',
 dmodels:'backend/src/main/java/com/behsazan/corebanking/deposit/account/waved/domain/DepositAccountWaveDModels.java',
 dsvc:'backend/src/main/java/com/behsazan/corebanking/deposit/account/waved/application/DepositAccountWaveDService.java',
 drepo:'backend/src/main/java/com/behsazan/corebanking/deposit/account/waved/oracle/DepositAccountWaveDRepository.java',
 ctl:'backend/src/main/java/com/behsazan/corebanking/deposit/account/operations/web/DepositAccountOperationsController.java',
 tx:'backend/src/main/java/com/behsazan/corebanking/deposit/account/transaction/application/DepositTransactionService.java',
 ui:'frontend/src/app/features/four-deposits/deposit-account-operations.service.ts',
 uiTs:'frontend/src/app/features/four-deposits/deposit-account-operations.component.ts',
 uiHtml:'frontend/src/app/features/four-deposits/deposit-account-operations.component.html',
 mig:'database/oracle/dps2/migrations/0.11.0-phase11n-steps11-17-rapid-completion.sql',
 db:'database/oracle/dps2/verification/0.11.0-phase11n-steps11-17-rapid-completion-verifier.sql',
 apply:'tools/apply-dps2-phase11n-rapid.cmd',
 qa:'docs/DPS2-0.11.0-PHASE11N-STEPS11-17-RAPID-COMPLETION-QA.md',
 build:'build-production.cmd',
 final:'tools/qualify-dps2-phase11nk-final.cmd',
 road:'docs/DPS2-CANONICAL-ROADMAP-FA.md'
};
let pass=0,fail=0; const text={};
for(const [k,p] of Object.entries(files)){if(fs.existsSync(p)){console.log(`PASS | rapid file exists: ${k}`);pass++;text[k]=fs.readFileSync(p,'utf8')}else{console.log(`FAIL | rapid file exists: ${k}`);fail++;text[k]=''}}
const checks=[
 ['Step11 reserve requirement projection',/ReserveRequirement/.test(text.models)&&/reserveRequirements\(accountId\)/.test(text.svc)],
 ['Step11 regulatory report item projection',/RegulatoryReportItem/.test(text.models)&&/regulatoryReportItems\(accountId\)/.test(text.svc)],
 ['Step12 fee override maker checker',/requestFeeOverride/.test(text.svc)&&/approveFeeOverride/.test(text.svc)&&/DEPOSIT_ACCOUNT_FEE_OVERRIDE/.test(text.repo)],
 ['Step12 pricing package enrollment',/enrollPricingPackage/.test(text.svc)&&/DEPOSIT_CUSTOMER_PACKAGE_ENROLLMENT/.test(text.repo)],
 ['Step12 governed fee rules tiers remain consumed not authored',/feeRules\(accountId\)/.test(text.svc)&&/feeTiers\(accountId\)/.test(text.svc)],
 ['Step13 tax adjustment approval trace',/requestTaxAdjustment/.test(text.svc)&&/approveTaxAdjustment/.test(text.svc)&&/DEPOSIT_TAX_ADJUSTMENT/.test(text.repo)],
 ['Step13 liability payment reconciliation projection',/taxLiabilities\(accountId\)/.test(text.svc)&&/taxPayments\(accountId\)/.test(text.svc)&&/taxReconciliations\(accountId\)/.test(text.svc)],
 ['Steps14-15 retained WaveD reconciliation exception correction',/startReconciliation/.test(text.dsvc)&&/executeCorrection/.test(text.dsvc)&&/rootCause/.test(text.dsvc)],
 ['Step16 correspondent reconciliation completion',/completeCorrespondentReconciliation/.test(text.dsvc)&&/markCorrespondentReconciled/.test(text.drepo)],
 ['Step17 draw entry winner workflow',/createLotteryDraw/.test(text.dsvc)&&/createLotteryEntry/.test(text.dsvc)&&/registerLotteryWinner/.test(text.dsvc)],
 ['Step17 prize payment goes through Step05',/REWARD_PRIZE_PAYMENT/.test(text.tx)&&/postDerived/.test(text.dsvc)&&/PAYMENT_TRANSACTION_ID/.test(text.drepo)],
 ['REST exposes rapid completion actions',/fee-overrides/.test(text.ctl)&&/tax-adjustments/.test(text.ctl)&&/lottery-winners/.test(text.ctl)&&/correspondent\/reconciliations/.test(text.ctl)],
 ['Angular client exposes rapid completion actions',/requestFeeOverride/.test(text.ui)&&/requestTaxAdjustment/.test(text.ui)&&/createLotteryDraw/.test(text.ui)&&/payLotteryWinner/.test(text.ui)],
 ['Angular operational UI wires rapid completion actions',/requestFeeOverride/.test(text.uiTs)&&/requestTaxAdjustment/.test(text.uiTs)&&/runLottery/.test(text.uiTs)&&/payLotteryWinner/.test(text.uiTs)&&/Step 05/.test(text.uiHtml)&&/ثبت پرداخت آخرین برنده/.test(text.uiHtml)],
 ['DB verifier checks approval and prize-payment trace',/fee override approval trace/.test(text.db)&&/reward prize transaction has account subledger trace/.test(text.db)],
 ['apply helper uses shared remote Oracle runner',/run-oracle-sql\.cmd/.test(text.apply)],
 ['production build includes rapid static gate',/verify-dps2-phase11n-steps11-17-rapid-completion/.test(text.build)],
 ['11N-K final qualifier exists with one production build and final marker',/One production build/.test(text.final)&&/DPS2_PHASE11NK_STEPS00_17_FINAL_QUALIFICATION_PASS/.test(text.final)],
 ['migration has no business insert update delete merge',!/(^|\n)\s*(INSERT|UPDATE|DELETE|MERGE)\s+/im.test(text.mig)],
 ['migration reconciles only missing operational sequences',/SEQ_DEPOSIT_ACCOUNT_FEE_OVERRIDE/.test(text.mig)&&/SEQ_DEPOSIT_LOTTERY_WINNER/.test(text.mig)],
 ['roadmap records qualification deferred to 11N-K',/QUALIFICATION_DEFERRED_TO_11N_K/.test(text.road)]
];
for(const [name,ok] of checks){console.log(`${ok?'PASS':'FAIL'} | ${name}`);ok?pass++:fail++}
console.log('------------------------------------------------------------');
console.log(`PHASE11N_RAPID_STATIC_VERIFIER_PASS=${pass}`);console.log(`PHASE11N_RAPID_STATIC_VERIFIER_FAIL=${fail}`);console.log(fail?'PHASE11N_RAPID_STATIC_BASELINE_FAIL':'PHASE11N_RAPID_STATIC_BASELINE_PASS');
process.exitCode=fail?1:0;
