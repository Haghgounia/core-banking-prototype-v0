import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const fail = message => { console.error(`FEE2_STUDIO_VERIFY_FAIL: ${message}`); process.exit(1); };
const pass = message => console.log(`PASS ${message}`);
const read = relative => {
  const file = path.join(root, relative);
  if (!fs.existsSync(file)) fail(`missing file: ${relative}`);
  return fs.readFileSync(file, 'utf8');
};

const routes = read('frontend/src/app/app.routes.ts');
for (const route of ['fee2/studio','dashboard','catalog','editor','simulation','runtime','compare','regulations','reference','history','audit']) {
  if (!routes.includes(`'${route}'`) && !routes.includes(`path: '${route}'`)) fail(`studio route missing: ${route}`);
}
pass('studio route family is registered while legacy FEE2 routes remain');
if (!routes.includes("path: 'fee2'" ) || !routes.includes("path: 'fee2/versions'") || !routes.includes("path: 'fee2/tables/:table'")) fail('existing FEE2 routes were removed');

const shell = read('frontend/src/app/layout/app-shell.component.html');
if (!shell.includes('routerLink="/fee2"') || !shell.includes('routerLink="/fee2/studio"')) fail('existing and studio FEE2 menu entries must coexist');
pass('existing FEE2 menu is preserved and studio menu is additive');

const controller = read('backend/src/main/java/com/behsazan/corebanking/fee2/web/Fee2Controller.java');
const service = read('backend/src/main/java/com/behsazan/corebanking/fee2/application/Fee2Service.java');
const repo = read('backend/src/main/java/com/behsazan/corebanking/fee2/oracle/Fee2Repository.java');
const models = read('backend/src/main/java/com/behsazan/corebanking/fee2/domain/Fee2Models.java');
for (const token of ['/studio/summary','/studio/catalog','/studio/fees']) if (!controller.includes(token)) fail(`studio API missing: ${token}`);
for (const token of ['StudioCreateRequest','StudioCreateResponse','StudioSummary','StudioCatalogItem']) if (!models.includes(token)) fail(`studio contract missing: ${token}`);
for (const token of ['@Transactional','createStudioFee','FEE_DEFINITION','FEE_VERSION','FEE_BINDING','FEE_TAX','updateCalculation']) if (!service.includes(token)) fail(`transactional studio creation contract missing: ${token}`);
if (!repo.includes('studioSummary(') || !repo.includes('studioCatalog(')) fail('studio read models missing');
pass('studio backend uses FEE2 transactional create and dedicated read models');

const studioDir = path.join(root, 'frontend/src/app/features/fee2/studio');
const requiredFiles = [
  'fee2-studio-shell.component.ts','fee2-studio-dashboard.component.ts','fee2-studio-catalog.component.ts',
  'fee2-studio-editor.component.ts','fee2-studio-create-dialog.component.ts','fee2-studio-simulation.component.ts',
  'fee2-studio-runtime.component.ts','fee2-studio-compare.component.ts','fee2-studio-list-page.component.ts',
  'fee2-studio-state.service.ts'
];
for (const f of requiredFiles) if (!fs.existsSync(path.join(studioDir,f))) fail(`studio UI file missing: ${f}`);
pass('business studio pages are implemented as Angular components');

const wizardTs = read('frontend/src/app/features/fee2/studio/fee2-studio-create-dialog.component.ts');
const wizardHtml = read('frontend/src/app/features/fee2/studio/fee2-studio-create-dialog.component.html');
for (const type of ['FIXED','PERCENTAGE','FIXED_PLUS_PERCENTAGE','PROGRESSIVE','BRACKET','MATRIX','USAGE','DAILY','FORMULA']) if (!wizardTs.includes(`calculationType:'${type}'`)) fail(`wizard template missing: ${type}`);
for (const label of ['الگو','مشخصات','اعمال و مالیات','بازبینی','ایجاد و باز کردن استودیو']) if (!wizardHtml.includes(label)) fail(`wizard UX missing: ${label}`);
pass('four-step studio wizard covers all nine FEE2 calculation methods');

const editorTs = read('frontend/src/app/features/fee2/studio/fee2-studio-editor.component.ts');
const editorHtml = read('frontend/src/app/features/fee2/studio/fee2-studio-editor.component.html');
const editorSource = `${editorTs}\n${editorHtml}`;
for (const label of ['عمومی','اعمال','شرایط','محاسبه','تعدیلات و سقف','مالیات','سهم‌ها','بازبینی']) if (!editorSource.includes(label)) fail(`studio editor tab missing: ${label}`);
if (!editorHtml.includes('app-fee2-calculation-editor')) fail('existing FEE2 calculation editor is not reused by studio');
pass('studio editor preserves the existing calculation editor and technical forms');

const dashboard = read('frontend/src/app/features/fee2/studio/fee2-studio-dashboard.component.html');
const catalog = read('frontend/src/app/features/fee2/studio/fee2-studio-catalog.component.html');
for (const label of ['مرکز کنترل موتور کارمزد','صف کاری','پوشش موتور محاسبه','آخرین محاسبات ثبت‌شده']) if (!dashboard.includes(label)) fail(`dashboard layout section missing: ${label}`);
for (const label of ['کاتالوگ و نسخه‌های کارمزد','روش محاسبه','وضعیت نسخه']) if (!catalog.includes(label)) fail(`catalog layout section missing: ${label}`);
pass('studio dashboard and catalog expose the intended business layout');

const listTs = read('frontend/src/app/features/fee2/studio/fee2-studio-list-page.component.ts');
const listHtml = read('frontend/src/app/features/fee2/studio/fee2-studio-list-page.component.html');
if (editorTs.includes('private readonly state=inject(Fee2StudioStateService)')) fail('studio editor exposes private state to Angular template');
if (listTs.includes('private readonly state=inject(Fee2StudioStateService)')) fail('studio list page exposes private state to Angular template');
for (const token of ['@for (row of rows(); track row[\'ID\'] || $index) {','@for (c of config.columns; track c.key) {','@if (c.key === \'STATUS\' || c.key === \'RESULT_STATUS\') {']) {
  if (!listHtml.includes(token)) fail(`studio list template structural control-flow marker missing: ${token}`);
}
pass('studio Angular templates keep template-visible state public and table control flow well structured');

const stateService = read('frontend/src/app/features/fee2/studio/fee2-studio-state.service.ts');
const studioShellHtml = read('frontend/src/app/features/fee2/studio/fee2-studio-shell.component.html');
const scopeBaselineSql = read('database/oracle/fee2/FEE2_Studio_Bank_Scope_Baseline_2026-10-10.sql');
for (const token of ["if (\"FEE_SCOPE\".equals(table)) where.add(\"T.STATUS='ACTIVE'\")", "SCOPE_CODE||' · '||NVL"]) {
  if (!repo.includes(token)) fail(`FEE_SCOPE lookup governance missing: ${token}`);
}
for (const token of ['هیچ محدوده فعال FEE2 تعریف نشده است','reloadScopes()']) {
  if (!stateService.includes(token)) fail(`studio empty-scope recovery missing: ${token}`);
}
for (const token of ['مدیریت محدوده‌های FEE2','بارخوانی مجدد']) {
  if (!studioShellHtml.includes(token)) fail(`studio scope recovery UX missing: ${token}`);
}
for (const token of ["SCOPE_CODE = 'BANK'", "'BANK', 'بانک', 'Bank', 'ACTIVE'", 'FEE2_STUDIO_SCOPE_BASELINE_PASS']) {
  if (!scopeBaselineSql.includes(token)) fail(`FEE2 BANK baseline reconciliation missing: ${token}`);
}
pass('studio scope selector is backed by active FEE_SCOPE data with explicit BANK baseline and empty-state recovery');

const studioScssFiles = fs.readdirSync(studioDir).filter(f => f.endsWith('.scss'));
const studioStyles = studioScssFiles.map(f => read(`frontend/src/app/features/fee2/studio/${f}`)).join('\n');
for (const token of ['var(--app-bg)','var(--app-surface)','var(--app-border)','var(--app-text)','var(--app-muted)','var(--app-primary)','var(--app-primary-soft)']) {
  if (!studioStyles.includes(token)) fail(`studio must reuse application theme token: ${token}`);
}
if (/#[0-9a-fA-F]{3,8}\b|rgba?\s*\(/.test(studioStyles)) fail('studio SCSS must not introduce a separate hard-coded color palette');
if (studioStyles.includes('--studio-primary') || studioStyles.includes('--studio-bg') || studioStyles.includes('--studio-panel')) fail('studio-specific theme palette must not replace application theme tokens');
const studioShellScss = read('frontend/src/app/features/fee2/studio/fee2-studio-shell.component.scss');
for (const token of ['background: var(--app-surface)','background: var(--app-bg)','background: var(--app-primary-soft)','color: var(--app-text)']) {
  if (!studioShellScss.includes(token)) fail(`studio shell is not aligned with global application theme: ${token}`);
}
pass('studio keeps reference-project layout while using the global Core Banking theme and design tokens');

const sourceFiles = fs.readdirSync(studioDir).filter(f => /\.(ts|html|scss)$/.test(f)).map(f => read(`frontend/src/app/features/fee2/studio/${f}`));
const allStudioSource = [...sourceFiles, controller, service, repo, models].join('\n');
for (const forbidden of ['ir.bank.feeengine','simple-fee-engine-r5.2','fee-studio/components','openFeeWizard','feeTemplates','initialVersionFromTemplate']) {
  if (allStudioSource.includes(forbidden)) fail(`reference implementation marker found: ${forbidden}`);
}
if (fs.readdirSync(studioDir).some(f => f.endsWith('.js'))) fail('reference-style JavaScript files must not be introduced into Angular studio');
pass('studio implementation is independent Angular/Spring code, not copied reference source');

console.log('FEE2_STUDIO_VERIFY_PASS');
