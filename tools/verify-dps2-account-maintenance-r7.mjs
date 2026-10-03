import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const read=(p)=>fs.readFileSync(path.join(root,p),'utf8');
const html=read('frontend/src/app/features/four-deposits/deposit-account-operations.component.html');
const scss=read('frontend/src/app/features/four-deposits/deposit-account-operations.component.scss');
const ts=read('frontend/src/app/features/four-deposits/deposit-account-operations.component.ts');
const servicingSvc=read('backend/src/main/java/com/behsazan/corebanking/deposit/account/servicing/application/DepositAccountServicingService.java');
const waveModels=read('backend/src/main/java/com/behsazan/corebanking/deposit/account/servicing/domain/DepositAccountWaveAModels.java');
const waveRepo=read('backend/src/main/java/com/behsazan/corebanking/deposit/account/servicing/oracle/DepositAccountWaveARepository.java');
const uiSvc=read('frontend/src/app/features/four-deposits/deposit-account-operations.service.ts');
let pass=0,fail=0;
const check=(name,ok)=>{if(ok){pass++;console.log(`PASS | ${name}`)}else{fail++;console.log(`FAIL | ${name}`)}};

check('owner customer search field exists',html.includes('<mat-label>جستجوی مشتری</mat-label>')&&html.includes('(click)="searchMaintenanceOwners()"'));
check('owner customer search uses CIF party search',ts.includes('async searchMaintenanceOwners()')&&ts.includes('this.cifService.searchParties({text,status:\'ACTIVE\''));
check('search supports Persian and Arabic digit normalization',ts.includes('normalizeCustomerSearchText')&&ts.includes("'۰۱۲۳۴۵۶۷۸۹'")&&ts.includes("'٠١٢٣٤٥٦٧٨٩'"));
check('search result selection is wired',html.includes('(click)="selectMaintenanceOwner(p)"')&&ts.includes('selectMaintenanceOwner(p:PartySummary)'));
check('inactive customers cannot be selected',html.includes("(p.lifecycleStatusCode??'').toUpperCase()!=='ACTIVE'")&&ts.includes("(p.lifecycleStatusCode??'').toUpperCase()!=='ACTIVE'"));
check('already linked owner is protected',html.includes('قبلاً مالک حساب است')&&ts.includes('ownerPartyAlreadyLinked('));

check('ownership percent field is required',html.includes('<mat-label>درصد مالکیت</mat-label>')&&html.includes('required [(ngModel)]="newPartyOwnership"'));
check('save button blocks missing/invalid ownership',html.includes('!ownerOwnershipValid()')&&ts.includes('ownerOwnershipValid(){return this.newPartyOwnership!==null'));
check('save method has server-independent ownership guard',ts.includes("this.error.set('درصد مالکیت الزامی است و باید عددی بین ۰ تا ۱۰۰ باشد.')"));
check('backend requires ownership for OWNER/JOINT_OWNER create',servicingSvc.includes('Set.of("OWNER","JOINT_OWNER").contains(role)&&r.ownershipPercent()==null'));
check('backend requires ownership for owner update',servicingSvc.includes('if(r.ownershipPercent()==null)throw new IllegalArgumentException("درصد مالکیت برای مالک و مالک مشترک الزامی است.")'));
check('backend owner create history contains ownership percent',servicingSvc.includes('",ownership="+Objects.toString(r.ownershipPercent(),"")'));

check('Persian calendar formatter exists',ts.includes("new Intl.DateTimeFormat('fa-IR-u-ca-persian'"));
check('formatter includes seconds',ts.includes("second:'2-digit'"));
check('base maintenance grid uses Persian date-time',html.includes('{{persianDateTime(h.effectiveAt)}}')&&html.includes('{{persianDateTime(a.lastActionAt)}}'));
check('product/condition grid uses Persian date-time',html.includes('{{persianDateTime(c.lastActionAt)}}')&&html.includes('{{persianDateTime(ph.lastActionAt)}}'));
check('owner audit uses Persian date-time and validity stays date-only',html.includes('{{persianDateTime(h.effectiveAt)}}')&&html.includes('{{persianDate(owner.validFrom)}}'));
check('Wave A exposes real last-action timestamps',waveModels.includes('OffsetDateTime lastActionAt')&&uiSvc.includes('lastActionAt:string|null'));
check('Wave A reads physical audit timestamps instead of inventing midnight',waveRepo.includes('COALESCE(UPDATED_AT,CREATED_AT)')&&waveRepo.includes('COALESCE(C.UPDATED_AT,A.DECIDED_AT,A.REQUESTED_AT,C.CREATED_AT)')&&waveRepo.includes('COALESCE(H.UPDATED_AT,A.DECIDED_AT,A.REQUESTED_AT,H.CREATED_AT)'));
check('maintenance date header is Persian business wording',html.includes('<th>تاریخ انجام</th>'));

const step1=html.slice(html.indexOf('id="ops-step01"'),html.indexOf('id="ops-step02"'));
check('maintenance owner UI has no Party label',!step1.includes('شناسه مشتری / Party')&&!step1.includes('<small>Party '));
check('maintenance owner audit description has no Servicing History label',!step1.includes('Servicing History'));
check('customer result grid headers are Persian',step1.includes('<th>نام / عنوان مشتری</th><th>شناسه مشتری</th><th>شناسه اصلی</th><th>نوع مشتری</th><th>وضعیت</th><th>عملیات</th>'));
check('maintenance source entity descriptions are localized',ts.includes("DEPOSIT_ACCOUNT_PARTY:'مالک / صاحب حساب'")&&ts.includes("DEPOSIT_ACCOUNT_CONTACT:'اطلاعات تماس حساب'"));
check('maintenance business codes are localized',ts.includes("SMS_NOTIFY:'اعلان پیامکی'")&&ts.includes("MIN_BALANCE:'حداقل مانده حساب'")&&ts.includes("DATA_CORRECTION:'اصلاح اطلاعات'"));
check('owner history structured values are localized',ts.includes("'شناسه مشتری: '")&&ts.includes("'، درصد مالکیت: '")&&ts.includes("'، صاحب اصلی: بله'"));
check('reusable search styling exists',scss.includes('.maintenance-owner-search')&&scss.includes('.maintenance-customer-results')&&scss.includes('.owner-required-error'));

console.log('------------------------------------------------------------');
console.log(`DPS2_ACCOUNT_MAINTENANCE_R7_PASS=${pass}`);
console.log(`DPS2_ACCOUNT_MAINTENANCE_R7_FAIL=${fail}`);
if(!fail)console.log('DPS2_ACCOUNT_MAINTENANCE_R7_STATIC_PASS');else process.exitCode=1;
