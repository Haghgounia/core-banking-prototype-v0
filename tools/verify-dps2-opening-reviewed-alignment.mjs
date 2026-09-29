import fs from 'node:fs';

const tsPath='frontend/src/app/features/four-deposits/deposit-opening-wizard.component.ts';
const htmlPath='frontend/src/app/features/four-deposits/deposit-opening-wizard.component.html';
const scssPath='frontend/src/app/features/four-deposits/deposit-opening-wizard.component.scss';
const modelPath='backend/src/main/java/com/behsazan/corebanking/deposit/opening/domain/DepositOpeningModels.java';
const repoPath='backend/src/main/java/com/behsazan/corebanking/deposit/opening/oracle/DepositOpeningAggregateRepository.java';
const servicePath='backend/src/main/java/com/behsazan/corebanking/deposit/opening/application/DepositOpeningAggregateService.java';
const bodies={ts:fs.readFileSync(tsPath,'utf8'),html:fs.readFileSync(htmlPath,'utf8'),scss:fs.readFileSync(scssPath,'utf8'),model:fs.readFileSync(modelPath,'utf8'),repo:fs.readFileSync(repoPath,'utf8'),service:fs.readFileSync(servicePath,'utf8')};

const checks=[
 ['7-step reviewed labels',bodies.ts,["'محصول و درخواست'","'Party و امضا'","'شرایط افتتاح'","'تأمین وجه'","'کنترل و شروط'","'بازبینی و تصمیم'","'ایجاد و فعال‌سازی'"]],
 ['Step 2 auto signatory UX',bodies.html,['صاحبان امضا و نحوه امضای حساب','ورود ردیف‌به‌ردیف فقط برای استثناء','ویرایش استثناء صاحب امضا / اختیار']],
 ['Step 3 product-derived contract UX',bodies.html,['تنظیمات قراردادی در زمان افتتاح','روش‌های برداشت و دسترسی درخواستی','شرایط سود مؤثر — DEPOSIT_OPENING_PROFIT_INSTRUCTION','تعیین‌شده توسط محصول']],
 ['Maturity destination account',bodies.ts,['settlementAccountReference','loadDestinationAccounts','maturitySettlementNeedsSelection','SETTLEMENT_ACCOUNT_REFERENCE']],
 ['Profit destination account',bodies.ts,['profitDestinationAccountReference','profitDestinationNeedsSelection','DESTINATION_ACCOUNT_REFERENCE']],
 ['Step 4 obligation allocation UX',bodies.html,['تعهدات مالی مورد انتظار — DEPOSIT_OPENING_OBLIGATION','هدف تخصیص','DEPOSIT_OPENING_FUNDING','پاک‌کردن برنامه منابع']],
 ['Canonical fund allocation',bodies.ts,['fundAllocations()','ALLOCATED_AMOUNT','DEPOSIT_OPENING_FUND_ALLOC:this.fundAllocations()']],
 ['Reviewed product term/pricing identifiers',bodies.ts,['id:710301','tierId:710331','pricingRuleId:71010','paymentRuleId:71012','id:720301','tierId:720331','pricingRuleId:72010','paymentRuleId:72012']],
 ['Reviewed request decision status payload',bodies.ts,['REQUEST_STATUS_CODE:v.decisionStatus','DEPOSIT_OPENING_DECISION:v.decisionCode?']],
 ['Step 5 reviewed checks table',bodies.html,['کنترل‌های افتتاح — DEPOSIT_OPENING_CHECK','دامنه Blocking','شبیه‌سازی خطا','checkStatusFa(c.status)']],
 ['Step 5 reviewed document checklist',bodies.html,['مدارک افتتاح — DEPOSIT_OPENING_DOCUMENT','<th>مدرک</th><th>الزام</th><th>وضعیت</th><th>اقدام</th>','toggleDocumentChecklist(d.code)']],
 ['Opening document reference optional per reviewed UX',bodies.service,['Reviewed opening UX treats DOCUMENT_REFERENCE as an optional external DMS reference']],
 ['Terms acceptance reviewed UX',bodies.html,['پذیرش شروط — DEPOSIT_OPENING_TERMS_ACCEPTANCE','پذیرنده','منبع پذیرش','وضعیت مدرک پذیرش','شروط نسخه فوق توسط پذیرنده انتخاب‌شده مطالعه و پذیرفته شد.']],
 ['Terms technical evidence payload',bodies.ts,['acceptedByPartyId','acceptanceSource','evidenceReference','EVIDENCE_REFERENCE']],
 ['Step 6 reviewed decision lifecycle',bodies.html,['ارجاع برای بررسی','بازگشت برای اصلاح','decisionStatusLabel()']],
 ['Step 7 controlled activation sequence',bodies.html,['ثبت Opening','ایجاد حساب','Funding / Settlement','Activation Readiness Gate','فعال‌سازی حساب']],
 ['Reviewed alignment styles',bodies.scss,['Opening Reviewed Alignment 2026-09-29','.repeatable-grid-wrap','.signatory-auto-card','.terms-grid','.doc-editor']],
 ['Backend maturity reference support',bodies.model,['SETTLEMENT_ACCOUNT_REFERENCE']],
 ['Backend profit destination support',bodies.model,['DESTINATION_ACCOUNT_REFERENCE','DESTINATION_SELECTED_BY_CUSTOMER']],
 ['Repository maturity persistence support',bodies.repo,['SETTLEMENT_ACCOUNT_REFERENCE','settlementAccountReference']],
 ['Repository profit destination persistence support',bodies.repo,['DESTINATION_ACCOUNT_REFERENCE','destinationAccountReference']]
];
let pass=0,fail=0;
for(const [label,body,needles] of checks){
 for(const needle of needles){const ok=body.includes(needle);console.log(`${ok?'PASS':'FAIL'} | ${label} | ${needle}`);ok?pass++:fail++;}
}
const forbidden=[
 ['old Step5 free document form', '<mat-label>نوع مدرک</mat-label><mat-select formControlName="documentType"'],
 ['old editable decision status select', '<mat-select formControlName="decisionStatus">'],
 ['equal split allocation algorithm', 'const perTarget=targets.length?x.amount/targets.length:0']
];
for(const [label,needle] of forbidden){const ok=!bodies.html.includes(needle)&&!bodies.ts.includes(needle);console.log(`${ok?'PASS':'FAIL'} | forbidden absent | ${label}`);ok?pass++:fail++;}
console.log('------------------------------------------------------------');
console.log(`DPS2_OPENING_REVIEWED_ALIGNMENT_PASS=${pass}`);
console.log(`DPS2_OPENING_REVIEWED_ALIGNMENT_FAIL=${fail}`);
if(fail)process.exit(1);
console.log('DPS2_OPENING_REVIEWED_ALIGNMENT_STATIC_PASS');
