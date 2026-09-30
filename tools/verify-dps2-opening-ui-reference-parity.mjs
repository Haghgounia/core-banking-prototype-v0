import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const read=relative=>fs.readFileSync(path.join(root,relative),'utf8');
const html=read('frontend/src/app/features/four-deposits/deposit-opening-wizard.component.html');
const scss=read('frontend/src/app/features/four-deposits/deposit-opening-wizard.component.scss');
const ts=read('frontend/src/app/features/four-deposits/deposit-opening-wizard.component.ts');

const requiredHtml=[
  'class="app" [class.sidebar-docked]',
  'class="sidebar" id="appSidebar"',
  'class="sidebar-launcher"',
  'class="topbar opening-topbar"',
  'Services &amp; Relationships · Tested',
  'Final Candidate Review',
  'class="wizard-shell"',
  'class="stepper"',
  'class="domain-grid"',
  'class="domain-option deposit"',
  'class="media-experience"',
  'class="media-card-grid"',
  'class="media-impact-panel"',
  'class="profit-policy-card"',
  'class="subsection-card service-auto-card"',
  'class="service-auto-actions"',
  'class="service-owner-summary"',
  '<th>خدمت</th><th>کانال پیشنهادی</th><th>مقصد قابل نمایش</th><th>منبع تشخیص</th><th>وضعیت</th><th>نوع تنظیم</th><th>عملیات</th>',
  'class="service-override-shell"',
  'class="service-override-grid"',
  'class="subsection-card payment-instrument-focus"',
  'class="payment-instrument-grid"',
  'CHEQUEBOOK_LEAF_COUNT',
  'class="pricing-dependent-grid"',
  'class="mini-grid"',
  'class="reward-dependent-grid"',
  'تعهدات مالی مورد انتظار — DEPOSIT_OPENING_OBLIGATION',
  'مدارک افتتاح — DEPOSIT_OPENING_DOCUMENT',
  'پذیرش شروط — DEPOSIT_OPENING_TERMS_ACCEPTANCE',
  '۷. ایجاد حساب، تسویه و فعال‌سازی کنترل‌شده'
];

const requiredCss=[
  '--nav:#0b1220','--bg:#f3f6fa','--primary:#155eef','--deposit:#6d36c9','--radius:16px',
  '.app.sidebar-floating .sidebar','.app.sidebar-docked','.sidebar-launcher','.topbar{position:sticky',
  '.view{display:block;padding:28px','.wizard-shell{display:grid;grid-template-columns:230px',
  '.stepper{background:white;border:1px solid var(--line);border-radius:16px',
  '.panel-card{background:white;border:1px solid var(--line);border-radius:18px;padding:22px',
  '.domain-grid{display:grid;grid-template-columns:1fr 1fr;gap:14px',
  '.form-grid{display:grid;grid-template-columns:repeat(12',
  '.control{width:100%;border:1px solid #cfd8e4;background:#fff;border-radius:9px',
  '.repeatable-grid{width:100%;min-width:760px;border-collapse:collapse;font-size:9px',
  '.media-experience{margin-top:8px;border:1px solid #d8e1ee;border-radius:16px',
  '.media-card-grid{display:grid;grid-template-columns:repeat(2',
  '.service-auto-card{border-color:#cddcf3',
  '.service-override-grid{display:grid;grid-template-columns:',
  '.payment-instrument-grid{display:grid;grid-template-columns:',
  '.signatory-simple-card{padding:16px',
  '.pricing-dependent-grid{display:grid;grid-template-columns:',
  '.reward-dependent-grid{display:grid;grid-template-columns:',
  '.profit-policy-card{background:#fff!important;border:1px solid #d8e4f4!important'
];

const forbiddenHtml=['<mat-form-field','matInput','<mat-select','<mat-option'];
const requiredTs=['sidebarDocked=signal(false)','serviceOverrideOpen=signal(false)','mediaDescription(code:string)','serviceOwnerSummary()','INSTRUMENT_HOLDER_ROLE_CODE','CHEQUEBOOK_LEAF_COUNT','SETTLEMENT_ACCOUNT_ID','DESTINATION_ACCOUNT_ID'];

let pass=0,fail=0;
const report=(name,ok)=>{console.log(`${ok?'PASS':'FAIL'} | ${name}`);ok?pass++:fail++;};
for(const token of requiredHtml) report(`HTML visual contract | ${token}`,html.includes(token));
for(const token of requiredCss) report(`CSS visual contract | ${token}`,scss.includes(token));
for(const token of forbiddenHtml) report(`Material visual drift absent | ${token}`,!html.includes(token));
for(const token of requiredTs) report(`Runtime bridge | ${token}`,ts.includes(token));
report('service table is 7-column final contract',(html.match(/<th>/g)??[]).length>20 && html.includes('کانال پیشنهادی')&&html.includes('نوع تنظیم'));
report('reference shell is not the old panel-only shell',!html.includes('<main class="panel panel-card">'));

console.log('------------------------------------------------------------');
console.log(`DPS2_OPENING_UI_REFERENCE_PARITY_PASS=${pass}`);
console.log(`DPS2_OPENING_UI_REFERENCE_PARITY_FAIL=${fail}`);
if(fail)process.exit(1);
console.log('DPS2_OPENING_UI_REFERENCE_PARITY_STATIC_PASS');
