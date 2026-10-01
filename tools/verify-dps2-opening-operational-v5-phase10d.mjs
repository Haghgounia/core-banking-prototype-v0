import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const htmlPath=path.join(root,'frontend/src/app/features/four-deposits/deposit-opening-wizard.component.html');
const tsPath=path.join(root,'frontend/src/app/features/four-deposits/deposit-opening-wizard.component.ts');
const batchHtmlPath=path.join(root,'frontend/src/app/features/four-deposits/deposit-opening-batch.component.html');
const batchServicePath=path.join(root,'backend/src/main/java/com/behsazan/corebanking/deposit/opening/batch/application/DepositOpeningBatchService.java');
const aggregateServicePath=path.join(root,'backend/src/main/java/com/behsazan/corebanking/deposit/opening/application/DepositOpeningAggregateService.java');
const operationalServicePath=path.join(root,'backend/src/main/java/com/behsazan/corebanking/deposit/opening/operational/application/DepositOpeningOperationalService.java');
const operationalRepositoryPath=path.join(root,'backend/src/main/java/com/behsazan/corebanking/deposit/opening/operational/oracle/DepositOpeningOperationalRepository.java');
const prototypeActivationAdapterPath=path.join(root,'backend/src/main/java/com/behsazan/corebanking/deposit/opening/operational/integration/PrototypeActivationEvidenceAdapter.java');
const applicationYmlPath=path.join(root,'backend/src/main/resources/application.yml');

const read=(p)=>fs.existsSync(p)?fs.readFileSync(p,'utf8'):'';
const html=read(htmlPath), ts=read(tsPath), batchHtml=read(batchHtmlPath), batchService=read(batchServicePath), aggregateService=read(aggregateServicePath), operationalService=read(operationalServicePath), operationalRepository=read(operationalRepositoryPath), prototypeActivationAdapter=read(prototypeActivationAdapterPath), applicationYml=read(applicationYmlPath);
const checks=[];
const add=(name,pass)=>checks.push({name,pass:!!pass});
const has=(s,...tokens)=>tokens.every(t=>s.includes(t));
const ordered=(s,...tokens)=>{let pos=-1;for(const t of tokens){const next=s.indexOf(t,pos+1);if(next<0)return false;pos=next;}return true};

add('CIF party search integration',has(ts,'cifService.searchParties'));
add('party search only active',has(ts,"status:'ACTIVE'"));
add('party search UX',has(html,'<h3 class="block-title">جستجوی Party</h3>','formControlName="partySearchText"','(click)="searchParties()"','افزودن به درخواست','انتخاب از نتایج جستجو','(click)="editParty(p.partyId)"'));
add('money comma formatting',has(ts,"Intl.NumberFormat('en-US'","onMoneyInput(control:'openingAmount'|'fundingAmount'|'signPresetAmount'")&&has(html,"onMoneyInput('openingAmount',$event)","onMoneyInput('fundingAmount',$event)","onMoneyInput('signPresetAmount',$event)"));
add('funding account operations integration',has(ts,'accountOperationsService.search'));
add('funding ownership verification',has(ts,'details.parties.some'));
add('funding ownership source reference',has(ts,'ACCOUNT_OPERATIONS:'));
add('funding active account selector',has(html,'حساب مبدأ مجاز','formControlName="fundingSourceAccountId"')&&has(ts,"status:'ACTIVE'","a.accountStatusCode==='ACTIVE'"));
add('org-unit digital routing',has(ts,"channel==='INTERNET'||channel==='MOBILE'"));
add('org-unit virtual branch code',has(ts,"setValue('0205'"));
add('product-driven withdrawal media',has(html,'allowedWithdrawalMedia()')||has(html,'currentFamily().media'));
add('cheque quantity is owned by CHEQUEBOOK payment instrument',!ts.includes('chequeCount')&&has(ts,"const chequebook=this.paymentInstruments().find(x=>x.type==='CHEQUEBOOK')","Number(chequebook.chequebookLeafCount)>0")&&has(ts,"DEPOSIT_OPENING_WITHDRAWAL_MEDIA:this.media().map(code=>({WITHDRAWAL_MEDIA_CODE:code,REQUESTED_QUANTITY:1")&&has(ts,"INSTRUMENT_TYPE_CODE:x.type","CHEQUEBOOK_LEAF_COUNT:x.type==='CHEQUEBOOK'?x.chequebookLeafCount:null"));
add('withdrawal/payment instrument separation',has(html,'تفکیک مفهومی:','<b>Withdrawal Media</b>','<b>Payment Instrument</b>','درخواست صدور یا تخصیص ابزار مشخص به یک Party'));
add('payment instrument follow-up scroll navigation',has(html,'تکمیل درخواست کارت','id="paymentInstrumentCard"')&&has(ts,"document.getElementById('paymentInstrumentCard')","classList.add('flash-section')","scrollIntoView({behavior:'smooth',block:'center'})"));
add('opening obligation summary',has(ts,'openingObligations'));
const obligationStart=ts.indexOf('openingObligations():OpeningObligationRow[]');
const obligationEnd=obligationStart>=0?ts.indexOf('\n }',obligationStart):-1;
const obligationExpr=obligationStart>=0?ts.slice(obligationStart,obligationEnd>=0?obligationEnd+3:obligationStart+1200):'';
add('reviewed simulated fee/tax obligation projection',has(obligationExpr,"type:'INITIAL_BALANCE'","source:'PRODUCT_BUILDER'","type:'OPENING_FEE'","source:'FEE_SERVICE'","type:'TAX'","source:'TAX_SERVICE'","type:'CARD_FEE'","type:'CHEQUEBOOK_FEE'")&&has(html,'مبالغ این Prototype خروجی نمونه شبیه‌سازی سرویس هستند'));
add('coverage uses total obligations',has(ts,'fundingCoverageGap'));
add('fund allocation lifecycle uses canonical PENDING to POSTED statuses',has(ts,"ALLOCATION_STATUS_CODE:'PENDING'")&&!ts.includes("ALLOCATION_STATUS_CODE:'PLANNED'")&&has(aggregateService,'PENDING/POSTED/REVERSED/REFUNDED/FAILED')&&has(operationalService,'countPendingAllocations(openingRequestId)','postPendingAllocations(openingRequestId, settlementReference, actor)')&&has(operationalRepository,"a.ALLOCATION_STATUS_CODE = 'PENDING'","ALLOCATION_STATUS_CODE = 'POSTED'"));
add('pending allocation settlement validates coverage and overage',has(operationalService,'countPendingAllocationCoverageGaps(openingRequestId)','countPendingAllocationFundingOverages(openingRequestId)')&&has(operationalRepository,'countPendingAllocationCoverageGaps','countPendingAllocationFundingOverages'));
add('create gate evidence controls',has(html,'<summary>شواهد فنی کنترل‌ها</summary>','formControlName="identityReference"','formControlName="kycReference"','formControlName="sanctionsReference"','formControlName="inquiriesReference"'));
add('reviewed create-gate semantics use explicit business failures instead of blanket missing-reference failure',has(ts,"else if(c.code==='INQUIRIES'||c.code==='TAX_PROFILE')fail=!this.taxQueried()")&&!ts.includes('else if(c.required)fail=!this.checkEvidenceReference(c.code).trim()'));
add('product eligibility runtime proof',has(ts,'PDL-RUNTIME:'));
add('P10F Qard product contract is explicit',has(ts,"'P10F_QARD_SAVINGS':{minOpeningAmount:10000000","channels:['BRANCH','MOBILE','INTERNET']","media:['CASH','CARD','TRANSFER']"));
add('product eligibility does not overload runtime preflight',has(ts,"else if(c.code==='PRODUCT_ELIGIBILITY')fail=this.productEligibilityErrors().length>0")&&!ts.includes("PRODUCT_ELIGIBILITY')fail=this.productPolicyErrors(true).length>0||!v.orgUnit||this.runtimeValidation()?.valid!==true"));
add('tax read-only/no-fabrication contract',has(html,'وضعیت مالیاتی افتتاح — DEPOSIT_OPENING_TAX_STATUS','formControlName="taxResidency" readonly','formControlName="taxExemptionCode" [disabled]="true"','formControlName="taxStatusSource" readonly','formControlName="taxVerificationReference" readonly')&&has(ts,"this.form.controls.taxStatusSource.setValue('CIF_FINANCIAL_PROFILE'","this.form.controls.taxExemptionCode.setValue('',","else if(c.code==='INQUIRIES'||c.code==='TAX_PROFILE')fail=!this.taxQueried()"));
add('prototype activation adapter supplies three external blocking evidences',has(prototypeActivationAdapter,
  'CBI_SIAH_REGISTRATION','PROTO-SIAH-OPENING-',
  'FINAL_COMPLIANCE_RECHECK','PROTO-COMPLIANCE-OPENING-',
  'RESTRICTIONS_READY','PROTO-RESTRICTIONS-OPENING-',
  'PROTOTYPE_ADAPTER:'));
add('prototype final compliance evidence has bounded validity',has(prototypeActivationAdapter,'evaluatedAt.plusHours(finalComplianceValidityHours)'));
add('prototype evidence only fills missing external evidence',has(operationalService,'prototypeActivationEvidenceAdapter.evidence(openingRequestId, now)','forEach(evidence::putIfAbsent)'));
add('prototype adapter can be disabled for real integrations',has(applicationYml,'DEPOSIT_OPENING_PROTOTYPE_ACTIVATION_EVIDENCE_ENABLED','prototype-activation-evidence:','enabled:'));
add('activation integration fields are system-managed in prototype UI',!has(ts,'siahReference:new FormControl','finalComplianceReference:new FormControl','restrictionsReference:new FormControl')&&has(html,'value="Prototype Adapter" readonly','Source=PROTOTYPE_ADAPTER'));
add('account opened sms evidence forwarded',has(ts,"add('ACCOUNT_OPENED_SMS',v.accountOpenedSmsReference)")&&has(html,'formControlName="accountOpenedSmsReference"'));
add('mandatory create-settle-readiness-activate order',ordered(html,'(click)="createOpeningAndAccount()"','(click)="settleDepositOpening()"','(click)="runActivationReadiness()"','(click)="activateDepositAccount()"')&&has(ts,'createAggregate(this.payload())','createAccount(opening.openingRequestId)',"account.accountStatusCode!=='PENDING_ACTIVATION'","readiness?.activationStatusCode!=='READY'"));
add('batch direct processing disabled UI',has(batchHtml,'پردازش و فعال‌سازی مستقیم گروهی در Backend فعلاً مسدود است'));
add('batch direct activation disabled backend',has(batchService,'فعال‌سازی مستقیم گروهی در Operational v5 مجاز نیست.'));

let ok=0;
for(const c of checks){
  console.log(`${c.pass?'OK ':'ERR'} ${c.name}`);
  if(c.pass)ok++;
}
if(ok!==checks.length) throw new Error(`Phase 10D v5 verifier failed: ${ok}/${checks.length}`);
console.log(`DPS2 Operational Opening v5 Phase 10D verification OK: ${ok}/${checks.length} UI/integration checks passed.`);
