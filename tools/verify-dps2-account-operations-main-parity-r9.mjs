import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const read=p=>fs.readFileSync(path.join(root,p),'utf8');
const html=read('frontend/src/app/features/four-deposits/deposit-account-operations.component.html');
const ts=read('frontend/src/app/features/four-deposits/deposit-account-operations.component.ts');
let pass=0,fail=0;const check=(n,o)=>{if(o){pass++;console.log(`PASS | ${n}`)}else{fail++;console.log(`FAIL | ${n}`)}};
const ops=[
['01','نگهداری حساب'],['02','چرخه عمر و انسداد'],['03','عملیات سپرده مدت‌دار'],['04','مدیریت سود'],['05','تراکنش‌های سپرده'],['06','صورتحساب'],['07','سقف و محدودیت'],['08','بستن و سررسید'],['09','خدمات حساب'],['10','طرف، دسترسی و ابزار'],['11','مقررات و انطباق'],['12','قیمت‌گذاری و کارمزد'],['13','مالیات و کسورات'],['14','تطبیق و مغایرت'],['15','استثنا و اصلاح'],['16','نوسترو / وسترو'],['17','جوایز و قرعه‌کشی']
];
check('dashboard menu matches MAIN',html.includes("scrollToSection('ops-dashboard')")&&html.includes('<span>نمای عملیاتی</span>'));
for(const [n,label] of ops){
  check(`MAIN menu ${n} ${label}`,html.includes(`scrollToSection('ops-step${n}')`)&&html.includes(`<span>${label}</span>`));
  check(`section target ${n}`,html.includes(`id="ops-step${n}"`)||html.includes(`[attr.id]="'ops-step${n}'"`));
}
check('exact 17 operational nav targets',ops.every(([n])=>html.includes(`scrollToSection('ops-step${n}')`)));
check('term step remains reachable without term contract',html.includes('@if(!termOperations())')&&html.includes('فرم Step 03 برای این حساب قابل اجرا نیست'));
check('profit step remains reachable without profit profile',html.includes('@if(!profit())')&&html.includes('فرم Step 04 حذف نشده'));
check('dashboard has local degraded-mode warning',html.includes('dashboardWarning()')&&ts.includes("readonly dashboardWarning=signal('')"));
const loadDash=ts.slice(ts.indexOf('async loadDashboard()'),ts.indexOf('async search()',ts.indexOf('async loadDashboard()')));
check('dashboard failure does not call global fail',!loadDash.includes('this.fail('));
check('dashboard fallback uses operational search',ts.includes("this.service.search({offset:0,limit:100})"));
check('dashboard fallback preserves account status counts',ts.includes("count('ACTIVE')")&&ts.includes("count('PENDING_ACTIVATION')")&&ts.includes("count('SUSPENDED')"));
check('all selected-account operation sections remain present',ops.every(([n])=>html.includes(`ops-step${n}`)));
check('MAIN headings retained',[
'عملیات سپرده مدت‌دار','مدیریت سود سپرده','پردازش تراکنش‌های سپرده','مدیریت صورتحساب سپرده','مدیریت سقف‌ها و محدودیت‌های حساب','بستن حساب و پردازش سررسید','مدیریت خدمات حساب سپرده','مدیریت ارتباط طرف با حساب','مدیریت مقررات و انطباق سپرده','مدیریت قیمت‌گذاری و کارمزد سپرده','مدیریت مالیات و کسورات سود سپرده','تطبیق و کنترل مغایرت سپرده','مدیریت استثناها و اصلاحات سپرده','حساب‌های نوسترو / وسترو','مدیریت جوایز و قرعه‌کشی سپرده‌ها'].every(x=>html.includes(x)));
console.log('------------------------------------------------------------');
console.log(`DPS2_ACCOUNT_OPERATIONS_MAIN_PARITY_R9_PASS=${pass}`);
console.log(`DPS2_ACCOUNT_OPERATIONS_MAIN_PARITY_R9_FAIL=${fail}`);
if(!fail)console.log('DPS2_ACCOUNT_OPERATIONS_MAIN_PARITY_R9_STATIC_PASS');else process.exitCode=1;
