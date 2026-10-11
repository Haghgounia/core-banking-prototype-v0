/** R27 regression contracts: product identities, policy Lite, channel operations. */
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {createRequire} from 'node:module';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const read = p=>fs.readFileSync(path.join(root,p),'utf8');
const fe='frontend/src/app/features/product-builder/';
const java='backend/src/main/java/com/behsazan/corebanking/productbuilder/';
const files={
  work:read(fe+'product-workspace.component.ts'),
  wh:read(fe+'product-workspace.component.html'),
  ws:read(fe+'product-workspace.component.scss'),
  api:read(fe+'product-builder.service.ts'),
  table:read(fe+'pdl-table.component.ts'),
  th:read(fe+'pdl-table.component.html'),
  pol:read(fe+'rule-policy-lite.component.ts'),
  ph:read(fe+'rule-policy-lite.component.html'),
  ps:read(fe+'rule-policy-lite.component.scss'),
  repo:read(java+'oracle/PdlProductBuilderRepository.java'),
  svc:read(java+'application/ProductBuilderService.java'),
  ctl:read(java+'web/ProductBuilderController.java'),
  psrv:read(java+'application/RuleGovernanceLiteService.java'),
  pctl:read(java+'web/RuleGovernanceLiteController.java'),
  choices:read(java+'application/PdlReferenceOptionService.java'),
  taxonomy:read(java+'application/LoanTypeClassification.java')
};
let passed=0,failed=0;
function ck(label,condition){try{assert.ok(condition,label);passed++;console.log('PASS | '+label);}catch(e){failed++;console.error('FAIL | '+label);}}
// Product identity: one consistent editor; never return to generic PRODUCT row form.
ck('create product routes to unified workspace',files.table.includes("this.router.navigate(['/product-builder/products/new'])"));
ck('edit product routes to identity step',files.table.includes("this.router.navigate(['/product-builder/products', productId])"));
ck('edit avoids generic record form for PRODUCT',/async edit\(row:[\s\S]*?if \(this\.tableName\(\) === 'PRODUCT'\)\s*\{[\s\S]*?return;[\s\S]*?const descriptor/.test(files.table));
ck('PRODUCT table explains consistent create/edit path',files.th.includes('دو فرم موازی با رفتار متفاوت وجود ندارد'));
ck('read-only balance nature has no editable mat-select',files.wh.includes('readonly aria-readonly="true"') && !files.wh.includes('<mat-select formControlName="BALANCE_NATURE_CODE"'));
ck('balance nature derived from actual domain',files.work.includes("value === 'DEPOSIT' ? 'LIABILITY' : 'ASSET'") && files.work.includes("this.productForm.controls.BALANCE_NATURE_CODE.setValue("));
ck('currency and balance inputs do not stretch with reference notes',files.ws.includes('.form-grid{align-items:start}') && files.ws.includes('.form-grid>mat-form-field{align-self:start'));
// Source of truth: group of at least one ACTIVE loan contract, not static seed or truncated UI pages.
ck('loan families requested through dedicated endpoint',files.work.includes('this.service.loanProductFamilies()') && files.api.includes('/loan-product-families'));
ck('loan family endpoint implemented',files.ctl.includes('@GetMapping("/loan-product-families")') && files.svc.includes('repository.activeLoanGroupCodes()'));
ck('loan family groups query is unpaged, DISTINCT, ACTIVE-only',files.repo.includes('SELECT DISTINCT LOAN_GROUP FROM') && files.repo.includes("WHERE RECORD_STATUS_CODE='ACTIVE' AND LOAN_GROUP BETWEEN 1 AND 4 ORDER BY LOAN_GROUP"));
ck('known four loan groups map to distinct product families',['QARD_HASAN_LOAN','EXCHANGE_LOAN','PARTICIPATORY_LOAN','COMMITMENT_LOAN'].every(c=>files.work.includes(`code: '${c}'`)));
ck('civil participation is a contract beneath participatory family',files.wh.includes('«مشارکت مدنی»') && files.wh.includes('تسهیلات مشارکتی'));
ck('empty family catalog has a diagnostic and retry',files.wh.includes('loanFamilyOptions().length') && files.wh.includes('loadLoanFamilyOptions()') && files.wh.includes('اطلاعات پایه انواع عقود'));
ck('family API failures shown, no synthetic options',files.work.includes('this.loanFamilyLoadError.set(this.message(e))') && files.wh.includes('هیچ گزینه حدسی جایگزین نشده است'));
ck('new product family must be in authoritative options',files.work.includes('this.productId() === null && !this.loanFamilyOptions().some'));
ck('historical product family remains readable',files.work.includes('this.productId() !== null && current') && files.work.includes('خانواده قدیمی'));
// Governance lite policy lifecycle and guide.
ck('policy options from PDL, not hardcoded mock',files.psrv.includes('FROM " + table("RULE_POLICY_VERSION")') && files.pctl.includes('@GetMapping("/rule-policies")'));
ck('policy dropdown disabled while unavailable',files.ph.includes('[disabled]="!policies().length || loadingPolicies() || busy()"'));
ck('policy empty state distinct from API error state',files.ph.includes('!loadingPolicies() && !error() && !policies().length') && files.pol.includes("this.error.set('دریافت نسخه‌های سیاست از PDL موفق نبود:"));
ck('empty policy offers creating first draft',files.ph.includes('ساخت نخستین سیاست') && files.ph.includes('[open]="!policies().length"'));
ck('policy form provides business-code example',files.ph.includes('LOAN_PRODUCT_POLICY') && files.ph.includes('کد سیاست'));
ck('maker and checker identifiers explained as UAT, not authenticated users',files.ph.includes('uat_maker01') && files.ph.includes('uat_checker02') && files.ph.includes('هویت احرازشده سرور'));
ck('policy guide explains create / review / approve / bind',files.ph.includes('ایجاد پیش‌نویس سیاست') && files.ph.includes('تصویب با کاربر مستقل') && files.ph.includes('اتصال سیاست مصوب'));
ck('backend Maker Checker separation still enforced',files.psrv.includes('checker.equalsIgnoreCase(current.maker())'));
ck('backend policy binding requires approved policy and draft product version',files.psrv.includes('فقط سیاست مصوب قابل اتصال است') && files.psrv.includes('اتصال سیاست فقط برای نسخه پیش‌نویس مجاز است'));
ck('runtime enforcement still honestly described as incomplete',files.ph.includes('اجرای قواعد در تراکنش‌های بانکی را فعال نمی‌کند'));
// Channel operation read is independent of EDIT-option scope; errors show raw code for diagnostics.
ck('channel grid places business operation first',files.table.includes("['OPERATION_CODE', 'IS_ALLOWED', 'REQUIRES_ADDITIONAL_AUTH']"));
ck('persisted operations use fixed read-only Persian lookup',files.table.includes('CHANNEL_OPERATION_PERSIAN[code]'));
ck('historical missing labels retain original code',files.table.includes('کد تاریخی تعریف‌نشده: ${code}'));
ck('domain-specific operation filters still kept for new edits',files.table.includes('LOAN_CHANNEL_OPERATION_CODES') && files.table.includes('DEPOSIT_CHANNEL_OPERATION_CODES'));
const regex=files.table.match(/CHANNEL_OPERATION_PERSIAN:\s*Readonly<Record<string, string>>\s*=\s*\{([\s\S]*?)\n\};/);
const pairs=regex ? [...regex[1].matchAll(/\b([A-Z_]+):\s*'([^']+)'/g)] : [];
const dict=Object.fromEntries(pairs.map(m=>[m[1],m[2]]));
ck('all 11 declared operations have display labels',Object.keys(dict).length===11 && ['APPLICATION','REPAYMENT','CASH_WITHDRAWAL','TRANSFER_OUT','OPENING'].every(c=>dict[c]?.length>4));
ck('loan and deposit operation labels reflect distinct businesses',dict.APPLICATION?.includes('تسهیلات') && dict.CASH_WITHDRAWAL?.includes('سپرده'));
ck('reference options match local display vocabulary',Object.keys(dict).every(code=>files.choices.includes(`"${code}"`)));
// The source guard runs before npm ci in Windows builds. TypeScript AST is
// optional here; Angular compilation later remains the authoritative check.
let typescript;
for (const entry of [path.join(root,'frontend','package.json'),path.join(root,'package.json'),import.meta.url]) {
 try { typescript=createRequire(entry)('typescript'); break; } catch { /* Next resolution scope. */ }
}
if(typescript){
 for(const file of ['product-workspace.component.ts','product-builder.service.ts','pdl-table.component.ts','rule-policy-lite.component.ts']) {
  const source=read(fe+file);
  const sf=typescript.createSourceFile(file,source,typescript.ScriptTarget.Latest,true,typescript.ScriptKind.TS);
  ck('TypeScript syntax '+file,sf.parseDiagnostics.length===0);
 }
}else console.log('SKIP | TypeScript AST: frontend dependencies unavailable before npm install; Angular build checks later.');
console.log(`PDL_R27_PRODUCT_POLICY_CHANNEL_STATIC_PASS=${passed} FAIL=${failed}`);
if(failed)process.exitCode=1;
