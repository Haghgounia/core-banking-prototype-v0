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
 ['Step 7 controlled activation sequence',bodies.html,['(click)="createOpeningAndAccount()"','۱. ایجاد حساب','۲. اجرای تأمین وجه و تسویه','۳. اجرای Gate آمادگی فعال‌سازی','۴. فعال‌سازی حساب']],
 ['Step 7 aggregate persistence is internal',bodies.ts,['createOpeningAndAccount()','createAggregate(this.payload())','createAccount(opening.openingRequestId)','creationBlockers()']],
 ['Party owner selector/editing',bodies.html,['formControlName="partyId"','انتخاب از نتایج جستجو','(click)="editParty(p.partyId)"']],
 ['Comma-delimited money inputs',bodies.html,["onMoneyInput('openingAmount',$event)","onMoneyInput('fundingAmount',$event)","onMoneyInput('signPresetAmount',$event)",'moneyFormat(totalObligationAmount())']],
 ['Opening obligations reviewed projection',bodies.ts,["type:'INITIAL_BALANCE'",'openingObligations():OpeningObligationRow[]']],
 ['Reviewed alignment styles',bodies.scss,['Opening Reviewed Alignment 2026-09-29','.repeatable-grid-wrap','.signatory-auto-card','.terms-grid','.doc-editor']],
 ['Backend maturity reference support',bodies.model,['SETTLEMENT_ACCOUNT_REFERENCE']],
 ['Reviewed selectable product contract',bodies.html,['formControlName="productVersionId"','(change)="selectProductVersion($any($event.target).value)"','مشاهده مشخصات محصول','productPolicySummary()']],
 ['Live Product Builder product/version source',bodies.ts,['ProductBuilderService',"loadAllPdlRows('PRODUCT')","loadAllPdlRows('PRODUCT_VERSION')",'ORIGINATION_STATUS_CODE','RECORD_STATUS_CODE']],
 ['Reviewed alternate digital product policy',bodies.ts,['DEP-QH-DIG-001','minOpeningAmount:5000000',"channels:['MOBILE','INTERNET']","customerTypes:['PERSON']","paymentInstruments:['CARD']",'rewardPrograms:[19001]','productPolicyErrors(includeOwners=true)']],
 ['P10F Qard Savings policy contract',bodies.ts,['P10F_QARD_SAVINGS','minOpeningAmount:10000000',"channels:['BRANCH','MOBILE','INTERNET']","media:['CASH','CARD','TRANSFER']",'productEligibilityErrors()']],
 ['Product eligibility separated from runtime preflight',bodies.ts,["else if(c.code==='PRODUCT_ELIGIBILITY')fail=this.productEligibilityErrors().length>0",'Runtime Preflight:','productEligibilityErrors().join']],
 ['Product-specific withdrawal media',bodies.html,['@for(code of allowedWithdrawalMedia();track code)']],
 ['Service adapter no manual target',bodies.html,['خدمات پیشنهادی سیستم — DEPOSIT_OPENING_SERVICE_SELECTION','type="hidden" formControlName="serviceTarget"','serviceOverrideTargetLabel()','serviceRecommendationStatus()']],
 ['Service adapter real Party/CIF source',bodies.ts,['cifService.getParty',"verifiedContact(profile,'MOBILE')","verifiedContact(profile,'EMAIL')",'channelPreference(profile,channel)','buildRecommendedServices()']],
 ['System suggestions protected outside override',bodies.html,['@if(serviceOverrideOpen()){<button class="btn small danger" type="button" (click)="removeService($index)">حذف</button>}@else{<span class="service-source">خودکار</span>}']],
 ['Risk/evidence system controlled',bodies.html,['formControlName="riskAssessmentReference" readonly','formControlName="expectedActivityReference" readonly','formControlName="identityReference" readonly','formControlName="kycReference" readonly']],
 ['Tax system controlled/no fabricated exemption',bodies.html,['formControlName="taxResidency" readonly','formControlName="withholdingApplicable" [disabled]="true"','formControlName="taxExemptionCode" [disabled]="true"','formControlName="taxStatusSource" readonly','formControlName="taxVerificationReference" readonly']],
 ['Tax source truthfulness',bodies.ts,['CIF_FINANCIAL_PROFILE','taxExemptionStatusLabel()']],
 ['Pricing workflow controlled',bodies.html,['سطح تأیید موردنیاز','[value]="form.controls.pricingAuthority.value" readonly',"[value]=\"form.controls.pricingApproval.value||'-'\" readonly"]],
 ['Reward policy/consent controlled',bodies.html,['@for(r of rewardPrograms();track r.id)','[disabled]="!form.controls.rewardProgram.value||!hasPrimaryOwner()"','formControlName="rewardConsent" readonly']],
 ['Single-opening request type excludes manual bulk',bodies.html,['افتتاح گروهی — ارجاع از پردازش گروهی']],
 ['Owner table keeps reviewed party type',bodies.html,['<th>Party ID</th><th>نام / عنوان</th><th>نوع</th><th>نقش</th>','partyTypeLabel(p.partyId)']],
 ['Backend profit destination support',bodies.model,['DESTINATION_ACCOUNT_REFERENCE','DESTINATION_SELECTED_BY_CUSTOMER']],
 ['Repository maturity persistence support',bodies.repo,['SETTLEMENT_ACCOUNT_REFERENCE','settlementAccountReference']],
 ['Repository profit destination persistence support',bodies.repo,['DESTINATION_ACCOUNT_REFERENCE','destinationAccountReference']],
 ['Step 4 reviewed obligation service rows',bodies.ts,["type:'OPENING_FEE'","gross:100000","source:'FEE_SERVICE'","type:'TAX'","gross:10000","source:'TAX_SERVICE'","type:'CARD_FEE'","gross:150000*cardCount","type:'CHEQUEBOOK_FEE'","gross:80000"]],
 ['Step 4 reviewed obligation simulation disclosure',bodies.html,['مبالغ این Prototype خروجی نمونه شبیه‌سازی سرویس هستند','FEE_SERVICE','TAX_SERVICE']],
 ['Payment instrument follow-up navigation',bodies.ts,["document.getElementById('paymentInstrumentCard')","classList.add('flash-section')","scrollIntoView({behavior:'smooth',block:'center'})"]],
 ['Current-account cheque leaf ownership',bodies.ts,["const chequebook=this.paymentInstruments().find(x=>x.type==='CHEQUEBOOK')","DEPOSIT_OPENING_WITHDRAWAL_MEDIA:this.media().map(code=>({WITHDRAWAL_MEDIA_CODE:code,REQUESTED_QUANTITY:1","CHEQUEBOOK_LEAF_COUNT:x.type==='CHEQUEBOOK'?x.chequebookLeafCount:null"]]
];
let pass=0,fail=0;
for(const [label,body,needles] of checks){
 for(const needle of needles){const ok=body.includes(needle);console.log(`${ok?'PASS':'FAIL'} | ${label} | ${needle}`);ok?pass++:fail++;}
}
const forbidden=[
 ['old Step5 free document form', '<mat-label>نوع مدرک</mat-label><mat-select formControlName="documentType"'],
 ['old editable decision status select', '<mat-select formControlName="decisionStatus">'],
 ['equal split allocation algorithm', 'const perTarget=targets.length?x.amount/targets.length:0'],
 ['service adapter pending placeholder', 'پیشنهاد Service واقعی هنوز از Adapter محصول/Party دریافت نشده است.'],
 ['editable product-version input', 'formControlName="productVersionId" readonly'],
 ['synthetic tax service marker', "taxStatusSource.setValue('TAX_SERVICE'"],
 ['manual Step7 persist button','>۱. ثبت Opening</button>'],
 ['legacy chequeCount field','chequeCount']
];
for(const [label,needle] of forbidden){const ok=!bodies.html.includes(needle)&&!bodies.ts.includes(needle);console.log(`${ok?'PASS':'FAIL'} | forbidden absent | ${label}`);ok?pass++:fail++;}
console.log('------------------------------------------------------------');
console.log(`DPS2_OPENING_REVIEWED_ALIGNMENT_PASS=${pass}`);
console.log(`DPS2_OPENING_REVIEWED_ALIGNMENT_FAIL=${fail}`);
if(fail)process.exit(1);
console.log('DPS2_OPENING_REVIEWED_ALIGNMENT_STATIC_PASS');
