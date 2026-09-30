import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const htmlPath=path.join(root,'frontend/src/app/features/four-deposits/deposit-opening-wizard.component.html');
const tsPath=path.join(root,'frontend/src/app/features/four-deposits/deposit-opening-wizard.component.ts');
const batchHtmlPath=path.join(root,'frontend/src/app/features/four-deposits/deposit-opening-batch.component.html');
const batchServicePath=path.join(root,'backend/src/main/java/com/behsazan/corebanking/deposit/opening/batch/application/DepositOpeningBatchService.java');

const read=(p)=>fs.existsSync(p)?fs.readFileSync(p,'utf8'):'';
const html=read(htmlPath), ts=read(tsPath), batchHtml=read(batchHtmlPath), batchService=read(batchServicePath);
const checks=[];
const add=(name,pass)=>checks.push({name,pass:!!pass});
const has=(s,...tokens)=>tokens.every(t=>s.includes(t));
const ordered=(s,...tokens)=>{let pos=-1;for(const t of tokens){const next=s.indexOf(t,pos+1);if(next<0)return false;pos=next;}return true};

add('CIF party search integration',has(ts,'cifService.searchParties'));
add('party search only active',has(ts,"status:'ACTIVE'"));
add('party search UX',has(html,'<h3 class="block-title">جستجوی Party</h3>','formControlName="partySearchText"','(click)="searchParties()"','افزودن به درخواست'));
add('funding account operations integration',has(ts,'accountOperationsService.search'));
add('funding ownership verification',has(ts,'details.parties.some'));
add('funding ownership source reference',has(ts,'ACCOUNT_OPERATIONS:'));
add('funding active account selector',has(html,'حساب مبدأ مجاز','formControlName="fundingSourceAccountId"')&&has(ts,"status:'ACTIVE'","a.accountStatusCode==='ACTIVE'"));
add('org-unit digital routing',has(ts,"channel==='INTERNET'||channel==='MOBILE'"));
add('org-unit virtual branch code',has(ts,"setValue('0205'"));
add('product-driven withdrawal media',has(html,'currentFamily().media'));
add('cheque quantity',has(ts,'chequeCount'));
add('withdrawal/payment instrument separation',has(html,'تفکیک مفهومی:','<b>Withdrawal Media</b>','<b>Payment Instrument</b>','درخواست صدور یا تخصیص ابزار مشخص به یک Party'));
add('opening obligation summary',has(ts,'openingObligations'));
const obligationStart=ts.indexOf('readonly openingObligations=');
const obligationEnd=obligationStart>=0?ts.indexOf('\n',obligationStart):-1;
const obligationExpr=obligationStart>=0?ts.slice(obligationStart,obligationEnd>=0?obligationEnd:obligationStart+1200):'';
add('no fabricated fee tax obligation',has(obligationExpr,"type:'INITIAL_BALANCE'","source:'PRODUCT_BUILDER'")&&!obligationExpr.includes("type:'OPENING_FEE'")&&!obligationExpr.includes("type:'TAX'")&&!obligationExpr.includes("type:'SERVICE_CHARGE'"));
add('coverage uses total obligations',has(ts,'fundingCoverageGap'));
add('create gate evidence controls',has(html,'<summary>شواهد فنی کنترل‌ها</summary>','formControlName="identityReference"','formControlName="kycReference"','formControlName="sanctionsReference"','formControlName="inquiriesReference"'));
add('required external evidence not auto pass',has(ts,'else if(c.required)fail=!this.checkEvidenceReference(c.code).trim()'));
add('product eligibility runtime proof',has(ts,'PDL-RUNTIME:'));
add('tax no-fabrication notice',has(html,'وضعیت مالیاتی افتتاح — DEPOSIT_OPENING_TAX_STATUS','formControlName="taxResidency" readonly','formControlName="taxExemptionCode" readonly','formControlName="taxStatusSource" readonly','formControlName="taxVerificationReference" readonly')&&has(ts,"else if(c.code==='TAX_PROFILE')fail=!v.taxResidency||!v.taxStatusSource||!v.taxVerificationReference"));
add('final compliance validity evidence',has(ts,'finalComplianceValidUntil'));
add('account opened sms evidence forwarded',has(ts,"add('ACCOUNT_OPENED_SMS',v.accountOpenedSmsReference)"));
add('mandatory create-settle-readiness-activate order',ordered(html,'(click)="persistAggregate()"','(click)="createDepositAccount()"','(click)="settleDepositOpening()"','(click)="runActivationReadiness()"','(click)="activateDepositAccount()"')&&has(ts,"account.accountStatusCode!=='PENDING_ACTIVATION'","activationReadiness()?.activationStatusCode!=='READY'"));
add('batch direct processing disabled UI',has(batchHtml,'پردازش و فعال‌سازی مستقیم گروهی در Backend فعلاً مسدود است'));
add('batch direct activation disabled backend',has(batchService,'فعال‌سازی مستقیم گروهی در Operational v5 مجاز نیست.'));

let ok=0;
for(const c of checks){
  console.log(`${c.pass?'OK ':'ERR'} ${c.name}`);
  if(c.pass)ok++;
}
if(ok!==checks.length) throw new Error(`Phase 10D v5 verifier failed: ${ok}/${checks.length}`);
console.log(`DPS2 Operational Opening v5 Phase 10D verification OK: ${ok}/${checks.length} UI/integration checks passed.`);
