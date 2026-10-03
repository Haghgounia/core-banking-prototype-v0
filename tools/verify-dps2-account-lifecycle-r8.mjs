import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const read=(p)=>fs.readFileSync(path.join(root,p),'utf8');
const html=read('frontend/src/app/features/four-deposits/deposit-account-operations.component.html');
const scss=read('frontend/src/app/features/four-deposits/deposit-account-operations.component.scss');
const ts=read('frontend/src/app/features/four-deposits/deposit-account-operations.component.ts');
const svc=read('frontend/src/app/features/four-deposits/deposit-account-operations.service.ts');
let pass=0,fail=0;
const check=(name,ok)=>{if(ok){pass++;console.log(`PASS | ${name}`)}else{fail++;console.log(`FAIL | ${name}`)}};

const a=html.indexOf('id="ops-step02"');
const b=html.indexOf('@if(termOperations();as term)',a);
const life=a>=0&&b>a?html.slice(a,b):'';

check('lifecycle section exists',a>=0&&b>a);
check('controlled activation is always visible',life.includes('فعال‌سازی کنترل‌شده حساب')&&!life.includes("@if(detail.account.accountStatusCode==='PENDING_ACTIVATION')"));
check('activation account selector exists',life.includes('حساب در انتظار فعال‌سازی')&&life.includes('[(ngModel)]="activationAccountId"'));
check('activation run status is readonly',life.includes('وضعیت Run')&&life.includes('[value]="activationRunStatus()"'));
check('activation two-step actions exist',life.includes('۱. اجرای کنترل‌های آمادگی')&&life.includes('۲. فعال‌سازی حساب'));
check('activation check grid exists',life.includes('<th>کنترل</th><th>نتیجه</th><th>شرح</th>'));
check('activation uses dedicated context',ts.includes('activationCandidates=signal<DepositAccountSummary[]>([])')&&ts.includes('activationControls=signal<DepositServicingControlsView|null>(null)'));
check('activation candidate search is PENDING_ACTIVATION',ts.includes("this.service.search({status:'PENDING_ACTIVATION',limit:100})"));

check('single lifecycle action form exists',life.includes('عملیات موردی چرخه عمر')&&life.includes('[(ngModel)]="lifecycleAction"')&&life.includes('(click)="executeLifecycleAction()"'));
check('lifecycle change grid exists',life.includes('تغییرات چرخه عمر')&&life.includes('detail.lifecycleEvents'));
check('lifecycle grid has Persian columns',life.includes('<th>رویداد</th><th>از وضعیت</th><th>به وضعیت</th><th>مرجع رویداد</th><th>انجام‌دهنده</th><th>تاریخ انجام</th>'));
const lifecycleApply=ts.slice(ts.indexOf('private applyLifecycleDetail'),ts.indexOf('private applyHoldDetail'));
check('lifecycle applies targeted state update',lifecycleApply.includes('lifecycleEvents:d.lifecycleEvents')&&lifecycleApply.includes('statusHistory:d.statusHistory')&&!lifecycleApply.includes('holds:d.holds'));
check('lifecycle mutation does not call applyDetail directly',!ts.match(/async suspendSelected\(\).*?this\.applyDetail\(r\.account\)/s));

check('hold balance summary exists',life.includes('وضعیت مسدودی و مانده قابل برداشت')&&life.includes('مانده دفتری')&&life.includes('مانده قابل برداشت'));
check('collateral system control exists',life.includes('کنترل سیستمی وثائق')&&life.includes('فقط سامانه مبدأ'));
check('current hold grid has canonical columns',life.includes('<th>شناسه</th><th>نوع</th><th>مبلغ</th><th>علت</th><th>وضعیت</th><th>سامانه مبدأ</th><th>ماژول مبدأ</th><th>مرجع درخواست</th><th>نحوه اجرا</th><th>سیاست رفع</th><th>ایجادکننده</th>'));

check('manual non-collateral hold form exists',life.includes('مسدودی دستی/کنترلی غیر وثیقه‌ای'));
check('manual hold does not offer collateral reason',!life.slice(life.indexOf('مسدودی دستی/کنترلی غیر وثیقه‌ای'),life.indexOf('عملیات گروهی حساب')).includes('COLLATERAL_PLEDGE'));
check('manual release targets explicit hold',life.includes('مسدودی موردنظر برای رفع')&&life.includes('[(ngModel)]="manualReleaseHoldId"'));
check('manual hold uses deposit origin contract',ts.includes("originSystemCode:'DEPOSIT_OPERATIONS'")&&ts.includes("originModuleCode:'ACCOUNT_HOLD'"));
check('manual hold targeted refresh exists',ts.includes('applyHoldDetail(r.account)'));

check('collateral service simulator exists',life.includes('شبیه‌ساز قرارداد سرویس سامانه وثائق'));
check('collateral create and release actions exist',life.includes('شبیه‌سازی سرویس ایجاد مسدودی')&&life.includes('شبیه‌سازی سرویس رفع مسدودی'));
check('collateral origin and policy are canonical',ts.includes("originSystemCode:'COLLATERAL'")&&ts.includes("originModuleCode:'COLLATERAL_MANAGEMENT'")&&ts.includes("releasePolicyCode:'ORIGIN_ONLY'"));
check('collateral service identity is explicit',ts.includes("'svc-collateral'")&&svc.includes("actor='deposit.operator'"));
check('collateral release supports partial amount',ts.includes('releaseAmount:amount')&&ts.includes("reasonCode:'COLLATERAL_RELEASED'"));
check('collateral target is explicit hold',life.includes('[(ngModel)]="collateralReleaseHoldId"')&&ts.includes('collateralActiveHolds()'));

check('status history grid exists',life.includes('تاریخچه وضعیت حساب')&&life.includes('detail.statusHistory'));
check('hold history grid exists',life.includes('تاریخچه مسدودی')&&life.includes('detail.holdHistory'));
check('history uses Persian date time',life.includes('persianDateTime(e.eventAt)')&&life.includes('persianDateTime(h.effectiveAt)')&&life.includes('persianDateTime(h.actionAt)'));
check('Persian date formatter includes seconds',ts.includes("second:'2-digit'")&&ts.includes("fa-IR-u-ca-persian"));
check('lifecycle visible grid headers are Persian',!life.includes('<th>Hold')&&!life.includes('<th>Status')&&!life.includes('<th>Action')&&!life.includes('<th>Time'));

check('lifecycle visual styles exist',scss.includes('.lifecycle-amount-summary')&&scss.includes('.collateral-system-control')&&scss.includes('.hold-wide-grid')&&scss.includes('.collateral-service-test'));

const servicingBlockStart=html.indexOf('@if(servicingControls();as wa){');
const termBlockStart=html.indexOf('@if(termOperations();as term)',servicingBlockStart);
const servicingPrefix=servicingBlockStart>=0&&termBlockStart>servicingBlockStart?html.slice(servicingBlockStart,termBlockStart):'';
check('servicing control-flow closes before term/later sections',servicingPrefix.trimEnd().endsWith('}'));

console.log('------------------------------------------------------------');
console.log(`DPS2_ACCOUNT_LIFECYCLE_R8_PASS=${pass}`);
console.log(`DPS2_ACCOUNT_LIFECYCLE_R8_FAIL=${fail}`);
if(!fail)console.log('DPS2_ACCOUNT_LIFECYCLE_R8_STATIC_PASS');else process.exitCode=1;
