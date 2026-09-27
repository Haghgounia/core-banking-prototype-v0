import fs from 'node:fs';
const checks = [
 ['wavec models', 'backend/src/main/java/com/behsazan/corebanking/deposit/account/wavec/domain/DepositAccountWaveCModels.java', ['ReservePositionRequest','RegulatoryReportRequest','FeeRuleRequest','FeeAssessmentRequest','ProfitabilityRequest','TaxLiabilityRequest','TaxPaymentRequest','TaxReconciliationRequest']],
 ['wavec service', 'backend/src/main/java/com/behsazan/corebanking/deposit/account/wavec/application/DepositAccountWaveCService.java', ['calculateReservePosition','generateRegulatoryReport','createFeeRule','assessFee','calculateProfitability','executeTaxAdjustment','createTaxLiability','payTaxLiability','reconcileTax']],
 ['wavec repository', 'backend/src/main/java/com/behsazan/corebanking/deposit/account/wavec/oracle/DepositAccountWaveCRepository.java', ['insertReservePosition','insertRegulatoryReport','insertRegulatoryReportItem','insertFeeRule','insertFeeTier','insertFeeAssessment','insertProfitability','insertTaxLiability','insertTaxPayment','insertTaxReconciliation','postTaxAdjustment']],
 ['controller', 'backend/src/main/java/com/behsazan/corebanking/deposit/account/operations/web/DepositAccountOperationsController.java', ['/reserve-positions','/regulatory-reports','/fee-rules','/fee-assessments','/profitability-snapshots','/tax-liabilities','/tax-payments','/tax-reconciliations','/reward-programs']],
 ['waved models', 'backend/src/main/java/com/behsazan/corebanking/deposit/account/waved/domain/DepositAccountWaveDModels.java', ['RewardProgramRequest']],
 ['waved service', 'backend/src/main/java/com/behsazan/corebanking/deposit/account/waved/application/DepositAccountWaveDService.java', ['createRewardProgram']],
 ['waved repository', 'backend/src/main/java/com/behsazan/corebanking/deposit/account/waved/oracle/DepositAccountWaveDRepository.java', ['insertRewardProgram','insertRewardEligibilityRule']],
 ['angular service', 'frontend/src/app/features/four-deposits/deposit-account-operations.service.ts', ['sendNotificationEvent','addDelegation','calculateReservePosition','generateRegulatoryReport','createFeeRule','assessFee','calculateProfitability','executeTaxAdjustment','createTaxLiability','payTaxLiability','reconcileTax','createRewardProgram']],
 ['angular component', 'frontend/src/app/features/four-deposits/deposit-account-operations.component.ts', ['sendNotificationEvent','addDelegation','calculateReservePosition','generateRegulatoryReport','createFeeRule','assessFee','calculateProfitability','executeTaxAdjustment','createTaxLiability','payTaxLiability','reconcileTax','createRewardProgram']],
 ['angular html', 'frontend/src/app/features/four-deposits/deposit-account-operations.component.html', ['ارسال رویداد اطلاع‌رسانی','ثبت وکالت/نمایندگی','محاسبه و ثبت ذخیره قانونی','تولید گزارش نظارتی','ثبت Fee Rule','محاسبه Fee Assessment','محاسبه Profitability','ایجاد بدهی دوره','پرداخت بدهی','تطبیق مالیات','تعریف برنامه','بررسی اهلیت و عضویت']]
];
let pass=0, fail=0;
for (const [name,file,needles] of checks){
  const body=fs.readFileSync(file,'utf8');
  for(const n of needles){ const ok=body.includes(n); console.log(`${ok?'PASS':'FAIL'} | ${name} | ${n}`); ok?pass++:fail++; }
}
console.log(`DESIGNER_BUSINESS_GAP_CLOSURE_A_PASS=${pass}`);
console.log(`DESIGNER_BUSINESS_GAP_CLOSURE_A_FAIL=${fail}`);
if(fail) process.exit(1);
console.log('DPS2_DESIGNER_BUSINESS_GAP_CLOSURE_A_STATIC_PASS');
