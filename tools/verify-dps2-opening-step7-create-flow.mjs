import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const read=p=>fs.readFileSync(path.join(root,p),'utf8');
const ts=read('frontend/src/app/features/four-deposits/deposit-opening-wizard.component.ts');
const html=read('frontend/src/app/features/four-deposits/deposit-opening-wizard.component.html');
const scss=read('frontend/src/app/features/four-deposits/deposit-opening-wizard.component.scss');
const phase10Foundation=read('database/oracle/dps2/migrations/0.10.0-phase10-opening-operational-v5-foundation.sql');
const aggregateRepository=read('backend/src/main/java/com/behsazan/corebanking/deposit/opening/oracle/DepositOpeningAggregateRepository.java');
const aggregateService=read('backend/src/main/java/com/behsazan/corebanking/deposit/opening/application/DepositOpeningAggregateService.java');
const accountRepository=read('backend/src/main/java/com/behsazan/corebanking/deposit/account/oracle/DepositAccountRepository.java');
const accountLifecycle=read('backend/src/main/java/com/behsazan/corebanking/deposit/account/application/DepositAccountLifecycleService.java');
const operationalService=read('backend/src/main/java/com/behsazan/corebanking/deposit/opening/operational/application/DepositOpeningOperationalService.java');
const prototypeActivationAdapter=read('backend/src/main/java/com/behsazan/corebanking/deposit/opening/operational/integration/PrototypeActivationEvidenceAdapter.java');
const checks=[];
const add=(name,ok)=>checks.push({name,ok:!!ok});
const has=(body,...parts)=>parts.every(x=>body.includes(x));
const ordered=(body,...parts)=>{let pos=-1;for(const part of parts){const next=body.indexOf(part,pos+1);if(next<0)return false;pos=next}return true};

add('risk is reviewed 3-option combo',has(html,
  'formControlName="customerRiskLevel"',
  '<option value="STANDARD">عادی / Standard</option>',
  '<option value="LOW">کم‌ریسک</option>',
  '<option value="HIGH">پرریسک / EDD</option>'));
add('risk evidence prefers CIF and has explicit opening-form fallback',has(ts,
  'CIF:RISK_ASSESSMENT:',
  'OPENING_FORM:RISK:',
  'syncRiskEvidenceFromSelection()'));
add('expected activity prefers CIF and has explicit opening-form fallback',has(ts,
  'CIF:FINANCIAL_PROFILE:',
  'OPENING_FORM:EXPECTED_ACTIVITY:',
  'syncExpectedActivityReference()'));
add('expected activity is a required Account Creation check and carries evidence',has(ts,
  "{code:'EXPECTED_ACTIVITY',title:'سطح فعالیت مورد انتظار',type:'COMPLIANCE',phase:'PRE_APPROVAL',scope:'ACCOUNT_CREATION',required:true",
  'EXPECTED_ACTIVITY:v.expectedActivityReference',
  "else if(c.code==='EXPECTED_ACTIVITY')fail=!v.expectedActivityReference.trim()"));

const checksStart=ts.indexOf('readonly checks=signal<CheckRow[]>([');
const checksEnd=checksStart>=0?ts.indexOf(']);',checksStart):-1;
const checkCatalog=checksStart>=0&&checksEnd>checksStart?ts.slice(checksStart,checksEnd):'';
const dbRequiredCreateChecks=[];
for(const part of phase10Foundation.split('MERGE INTO DPS2.REF_DEP_OPEN_CHECK t').slice(1)){
  const code=part.match(/USING \(SELECT '([A-Z0-9_]+)' CHECK_CODE/);
  const active=/IS_ACTIVE=1/.test(part);
  const required=/DEFAULT_REQUIRED_FLAG=1/.test(part);
  const scope=/DEFAULT_BLOCKING_SCOPE_CODE='ACCOUNT_CREATION'/.test(part);
  if(code&&active&&required&&scope)dbRequiredCreateChecks.push(code[1]);
}
add('frontend covers every active required Account Creation check from DB catalog',dbRequiredCreateChecks.length>0&&dbRequiredCreateChecks.every(code=>checkCatalog.includes(`code:'${code}'`)));
add('approved aggregate rejects missing required catalog checks before persistence',has(aggregateRepository,
  'requiredAccountCreationCheckCodes()',
  "DEFAULT_BLOCKING_SCOPE_CODE = 'ACCOUNT_CREATION'")&&has(aggregateService,
  'missingRequiredChecks = repository.requiredAccountCreationCheckCodes()',
  'کنترل‌های الزامی Account Creation در Payload وجود ندارند'));
add('aggregate validation is instance-scoped when repository catalog is queried',
  aggregateService.includes('private void validate(AggregateRequest aggregate)')&&
  !aggregateService.includes('private static void validate(AggregateRequest aggregate)'));
add('Create Account error reports exact unresolved check codes/titles',has(accountRepository,
  'unresolvedRequiredAccountCreationCheckDetails',
  "r.CHECK_CODE || ' — ' || NVL(r.TITLE_FA, r.TITLE_EN)")&&has(accountLifecycle,
  'repository.unresolvedRequiredAccountCreationCheckDetails(openingRequestId)',
  'کنترل‌های الزامی حل‌نشده Account Creation'));
add('preflight uses DRAFT payload to avoid approved/check circular gate',has(ts,
  'private preflightPayload()',
  "REQUEST_STATUS_CODE:'DRAFT'",
  'DEPOSIT_OPENING_DECISION:null',
  'validateAggregate(this.preflightPayload())'));
add('preflight no longer validates final approved payload directly',!ts.includes('validateAggregate(this.payload())'));
add('funding purpose uses active Phase10 canonical codes',has(ts,
  "fundingPurpose:new FormControl('OPENING_TOTAL'",
  "v.fundingTarget==='ALL'?'OPENING_TOTAL'",
  "?'INITIAL_BALANCE':'CHARGES'")&&!ts.includes("FUNDING_PURPOSE_CODE:x.purpose||'COMBINED'"));
add('runtime preflight refreshes Party/CIF adapters before validation',ordered(ts,
  'async runRuntimePreflight()',
  'await this.refreshPrimaryPartyAdapters(false)',
  'validateAggregate(this.preflightPayload())'));
add('system suggested owner signatories follow reviewed VERIFIED contract',has(ts,
  "role:'OWNER_SIGNATORY'",
  "verification:'VERIFIED'"));
add('reviewed Step5 gate does not blanket fail missing technical references',has(ts,
  "else if(c.code==='INQUIRIES'||c.code==='TAX_PROFILE')fail=!this.taxQueried()")&&!ts.includes('else if(c.required)fail=!this.checkEvidenceReference(c.code).trim()'));
add('P10F Qard Savings uses the correct opening policy',has(ts,
  "'P10F_QARD_SAVINGS':{minOpeningAmount:10000000",
  "channels:['BRANCH','MOBILE','INTERNET']",
  "media:['CASH','CARD','TRANSFER']"));
add('product eligibility is independent from runtime preflight',has(ts,
  "else if(c.code==='PRODUCT_ELIGIBILITY')fail=this.productEligibilityErrors().length>0")&&
  !ts.includes("else if(c.code==='PRODUCT_ELIGIBILITY')fail=this.productPolicyErrors(true).length>0||!v.orgUnit||this.runtimeValidation()?.valid!==true"));
add('product eligibility emits exact diagnostic reasons',has(ts,
  'productEligibilityErrors()',
  'روش برداشت نامعتبر برای',
  'ابزار پرداخت نامعتبر برای',
  'کانال «${this.openingChannelLabel(v.channel)}» برای محصول'));
add('local blockers render once without duplicating accountError',has(ts,
  "if(blockers.length){this.accountError.set('');return;}"));

const createStart=ts.indexOf('async createOpeningAndAccount()');
const createEnd=ts.indexOf('async createDepositAccount()',createStart);
const createBody=createStart>=0&&createEnd>createStart?ts.slice(createStart,createEnd):'';
add('create flow order is preflight -> checks -> blockers -> persist -> account',ordered(createBody,
  'await this.runRuntimePreflight()',
  'this.runChecks()',
  'const blockers=this.creationBlockers()',
  'createAggregate(this.payload())',
  'createAccount(opening.openingRequestId)'));
add('step7 blocker reports unresolved check names/statuses and eligibility reason',has(ts,'const unresolved=createChecks.filter',"c.code==='PRODUCT_ELIGIBILITY'?",'productEligibilityErrors().join'));
add('all-obligation target synchronizes source amount',has(html,'formControlName="fundingTarget" (change)="syncFundingAmountFromTarget()"','با انتخاب «همه تعهدات فعلی»، مانده کل مبلغ قابل وصول به‌صورت خودکار درج می‌شود.'));
add('funding amount derives from remaining current obligations',has(ts,'fundingTargetRemaining(','this.fundAllocations()','if(target===\'ALL\')return obligations.reduce','syncFundingAmountFromTarget()'));
add('obligation changes resync funding amount',has(ts,'addPaymentInstrument()','removePaymentInstrument(i:number)','this.syncFundingAmountFromTarget()'));
add('step7 has exactly four lifecycle columns',scss.includes('.lifecycle-toolbar{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));align-items:stretch}'));
add('step7 user actions remain create-settle-readiness-activate',has(html,'۱. ایجاد حساب','۲. اجرای تأمین وجه و تسویه','۳. اجرای Gate آمادگی فعال‌سازی','۴. فعال‌سازی حساب'));
add('prototype activation blockers are supplied by backend adapter',has(prototypeActivationAdapter,
  'CBI_SIAH_REGISTRATION','FINAL_COMPLIANCE_RECHECK','RESTRICTIONS_READY','PROTOTYPE_ADAPTER:')&&has(operationalService,'forEach(evidence::putIfAbsent)'));
add('prototype UI no longer asks operator for three external blocking references',
  !ts.includes('siahReference:new FormControl')&&!ts.includes('finalComplianceReference:new FormControl')&&!ts.includes('restrictionsReference:new FormControl')&&
  has(html,'SIAH / CBI Tracking','Final Compliance Recheck','Restrictions Ready','value="Prototype Adapter" readonly'));
add('activation button is enabled only for READY pending account',has(html,
  "account()?.accountStatusCode!=='PENDING_ACTIVATION'",
  "activationReadiness()?.activationStatusCode!=='READY'"));
add('activation handler never silently ignores invalid lifecycle state',has(ts,
  "حساب ${account.accountNo} قبلاً فعال شده است.",
  "فعال‌سازی فقط برای حساب PENDING_ACTIVATION مجاز است. وضعیت جاری:",
  "Activation Readiness Gate هنوز READY نیست. وضعیت جاری:"));
add('activation success is visible to operator',has(ts,
  "this.accountNotice.set(`حساب ${activated.accountNo} با موفقیت فعال شد.`)")&&has(html,'@if(accountNotice())','notice ok'));


for(const c of checks)console.log(`${c.ok?'PASS':'FAIL'} | ${c.name}`);
const failed=checks.filter(c=>!c.ok);
console.log('------------------------------------------------------------');
console.log(`DPS2_OPENING_STEP7_FLOW_PASS=${checks.length-failed.length}`);
console.log(`DPS2_OPENING_STEP7_FLOW_FAIL=${failed.length}`);
if(failed.length)process.exit(1);
console.log('DPS2_OPENING_STEP7_FLOW_STATIC_PASS');
