import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const scriptFile=fileURLToPath(import.meta.url);
const root=path.resolve(path.dirname(scriptFile),'..');
const files={
  html:path.join(root,'frontend/src/app/features/four-deposits/deposit-account-operations.component.html'),
  scss:path.join(root,'frontend/src/app/features/four-deposits/deposit-account-operations.component.scss'),
  ts:path.join(root,'frontend/src/app/features/four-deposits/deposit-account-operations.component.ts'),
  svc:path.join(root,'frontend/src/app/features/four-deposits/deposit-account-operations.service.ts')
};
let pass=0,fail=0;
function check(name,ok,detail=''){if(ok){pass++;console.log(`PASS | ${name}`)}else{fail++;console.log(`FAIL | ${name}${detail?` | ${detail}`:''}`)}}
for(const [k,f] of Object.entries(files))check(`file exists: ${k}`,fs.existsSync(f),f);
if(fail){process.exitCode=1;process.exit()}
const html=fs.readFileSync(files.html,'utf8');
const scss=fs.readFileSync(files.scss,'utf8');
const ts=fs.readFileSync(files.ts,'utf8');
const svc=fs.readFileSync(files.svc,'utf8');

for(const marker of [
  'class="operations-reference-app"','class="app" [class.sidebar-docked]','class="sidebar" id="accountOpsSidebar"',
  'class="topbar operations-topbar"','class="page-head"','class="scope-note"','class="operations-layout"',
  'class="operations-stepper"','class="account-context','id="ops-dashboard"','id="ops-search"','id="ops-results"','id="ops-account"',
  'id="ops-step01"','id="ops-step02"','id="ops-step03"','id="ops-step04"','id="ops-step05"','id="ops-step06"',
  'id="ops-step07"','id="ops-step08"','id="ops-step09"','id="ops-step10"','id="ops-step11"','id="ops-step12"',
  'id="ops-step13"','id="ops-step14"','id="ops-step15"','id="ops-step16"','id="ops-step17"','id="ops-closure"','id="ops-hold"'
]) check(`UI marker: ${marker}`,html.includes(marker));

for(const token of ['--nav:#0b1220','--bg:#f3f6fa','--primary:#155eef','--deposit:#6d36c9','--radius:16px','sidebar-floating','sidebar-docked','operations-stepper','account-context','operation-step-card','mat-mdc-form-field','@media(max-width:760px)']) check(`Opening visual token: ${token}`,scss.includes(token));

for(const action of [
  'loadDashboard()','search()','clear()','select(row)','suspendSelected()','markDormantSelected()','reactivateSelected()',
  'runActivation()','executeActivationRun(','runBulkAction()','saveMaintenance()','requestCondition()','requestProductVersionChange()','addSignatoryAuthority()',
  'saveTermInstruction()','executeTermMaturity()','requestTermPartial()','requestTermRenewal()','requestTermConversion()','requestTermEarlyTermination()',
  'accrueProfit()','postProfit()','requestProfitAdjustment()','initiateTransaction()','reverseTransaction(','requestClosure()','requestReopening()',
  'createHold()','releaseHold(','scrollToSection('
]) check(`critical UI action retained: ${action}`,html.includes(action));

for(const method of ['sidebarDocked=signal(false)','sidebarOpen=signal(false)','openSidebar()','closeSidebar()','toggleSidebarDock()','scrollToSection(id:string)']) check(`component navigation: ${method}`,ts.includes(method));

// Every click handler must resolve to a component method.
const clickMethods=[...html.matchAll(/\(click\)="([A-Za-z_$][\w$]*)\s*\(/g)].map(m=>m[1]);
const uniqueClicks=[...new Set(clickMethods)];
const missingClicks=uniqueClicks.filter(m=>!new RegExp(`\\b(?:async\\s+)?${m.replace(/[$]/g,'\\$&')}\\s*\\(`).test(ts));
check('all template click handlers resolve to component methods',missingClicks.length===0,missingClicks.join(','));

// Every service call from the component must resolve to a service method.
const serviceCalls=[...ts.matchAll(/this\.service\.([A-Za-z_$][\w$]*)\s*\(/g)].map(m=>m[1]);
const uniqueServiceCalls=[...new Set(serviceCalls)];
const missingService=uniqueServiceCalls.filter(m=>!new RegExp(`\\b${m.replace(/[$]/g,'\\$&')}\\s*\\(`).test(svc));
check('all component service calls resolve to service methods',missingService.length===0,missingService.join(','));
check('service wiring breadth remains substantial',uniqueServiceCalls.length>=60,`count=${uniqueServiceCalls.length}`);

for(const marker of [
  'dashboard()','get(','activation','bulk','term','profit','transactions','closure','hold','waveB','waveC','waveD'
]) check(`service/runtime contract marker: ${marker}`,svc.toLowerCase().includes(marker.toLowerCase()));

check('no old hero-only shell remains',!html.includes('class="hero card"'));
check('selected account status badge retained',html.includes('[attr.data-status]="detail.account.accountStatusCode"'));
check('account context shows available balance',html.includes('detail.balance.availableBalance'));
check('canonical boundary text retained',html.includes('مرزبندی Canonical'));

console.log('------------------------------------------------------------');
console.log(`DPS2_ACCOUNT_OPERATIONS_UI_ALIGNMENT_PASS=${pass}`);
console.log(`DPS2_ACCOUNT_OPERATIONS_UI_ALIGNMENT_FAIL=${fail}`);
if(fail===0)console.log('DPS2_ACCOUNT_OPERATIONS_UI_ALIGNMENT_STATIC_PASS');
else process.exitCode=1;
