import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const fail = (message) => { console.error(`FEE2_FORMS_VERIFY_FAIL: ${message}`); process.exit(1); };
const ok = (message) => console.log(`PASS ${message}`);
const read = (relative) => {
  const file = path.join(root, relative);
  if (!fs.existsSync(file)) fail(`missing file: ${relative}`);
  return fs.readFileSync(file, 'utf8');
};

const expected = [
  'FEE_APPROVAL','FEE_AUDIT','FEE_BINDING','FEE_CALC_ITEM','FEE_CALC_LOG','FEE_CONDITION','FEE_CONFIG_REVISION',
  'FEE_DEFINITION','FEE_IDEMPOTENCY','FEE_MODIFIER','FEE_REFERENCE_DATA','FEE_REGULATION','FEE_SCOPE','FEE_SHARE',
  'FEE_SIMULATION_CASE','FEE_SIMULATION_RESULT','FEE_SIMULATION_RUN','FEE_TAX','FEE_VERSION'
].sort();

const catalog = read('backend/src/main/java/com/behsazan/corebanking/fee2/application/Fee2Catalog.java');
const actual = [...catalog.matchAll(/register\("([A-Z0-9_]+)"/g)].map(match => match[1]).sort();
if (JSON.stringify(actual) !== JSON.stringify(expected)) {
  const missing = expected.filter(x => !actual.includes(x));
  const extra = actual.filter(x => !expected.includes(x));
  fail(`FEE2 catalog mismatch missing=${missing.join(',') || '-'} extra=${extra.join(',') || '-'}`);
}
ok('19 FEE2 tables are registered exactly once');

const yaml = read('backend/src/main/resources/application.yml');
const configYaml = read('config/application.yml');
if (!/^\s*fee2:\s*FEE2\s*$/m.test(yaml) || !/^\s*fee2:\s*FEE2\s*$/m.test(configYaml)) fail('fee2: FEE2 schema configuration missing');
ok('FEE2 schema configuration');

const controller = read('backend/src/main/java/com/behsazan/corebanking/fee2/web/Fee2Controller.java');
for (const token of ['/api/v1/fee2','/catalog','/tables/{table}/descriptor','/tables/{table}/rows','/versions/{id}/transition']) {
  if (!controller.includes(token)) fail(`FEE2 API token missing: ${token}`);
}
ok('FEE2 dedicated REST API');

const repo = read('backend/src/main/java/com/behsazan/corebanking/fee2/oracle/Fee2Repository.java');
for (const token of ['UUID.randomUUID().toString()','core-banking.schemas.fee2:FEE2','ROW_VERSION=ROW_VERSION+1','VERSION_NO','FEE_APPROVAL']) {
  if (!repo.includes(token)) fail(`FEE2 repository contract missing: ${token}`);
}
if (repo.includes('Long id') || repo.includes('@PathVariable long id')) fail('FEE2 must not use numeric IDs for UUID primary keys');
const dataDefaultRead = repo.indexOf('rs.getString("DATA_DEFAULT")');
const commentsRead = repo.indexOf('rs.getString("COMMENTS")');
if (dataDefaultRead < 0 || commentsRead < 0 || dataDefaultRead > commentsRead) {
  fail('Oracle ALL_TAB_COLUMNS.DATA_DEFAULT (LONG) must be read before later COMMENTS column to avoid ORA-17027');
}
if (!repo.includes('Oracle exposes ALL_TAB_COLUMNS.DATA_DEFAULT as LONG')) {
  fail('FEE2 descriptor LONG-column JDBC guard comment missing');
}
ok('UUID persistence, optimistic locking and Oracle LONG metadata read order');

const service = read('backend/src/main/java/com/behsazan/corebanking/fee2/application/Fee2Service.java');
for (const token of ['VERSION_CHILDREN','DRAFT', 'READY', 'APPROVED', 'ACTIVE', 'RETIRED', 'requireDraftVersion']) {
  if (!service.includes(token)) fail(`FEE2 lifecycle/child guard missing: ${token}`);
}
ok('version lifecycle and draft-only child editing');

const routes = read('frontend/src/app/app.routes.ts');
for (const token of ["path: 'fee2'", "path: 'fee2/versions'", "path: 'fee2/tables/:table'"]) {
  if (!routes.includes(token)) fail(`FEE2 route missing: ${token}`);
}
ok('FEE2 Angular routes');

const shell = read('frontend/src/app/layout/app-shell.component.html');
if (!shell.includes('routerLink="/fee2"') || !shell.includes('موتور کارمزد جدید')) fail('independent FEE2 sidebar entry missing');
if (!shell.includes('routerLink="/fee"') || !shell.includes('مدیریت کارمزد')) fail('existing Fee menu must remain unchanged');
ok('separate FEE2 menu while legacy Fee menu remains');

const home = read('frontend/src/app/features/fee2/fee2-home.component.html');
for (const token of ['دامنه‌های کارمزد','کاتالوگ کارمزدها','طراحی و نسخه‌بندی','شبیه‌سازی','تاریخچه محاسبات']) {
  if (!home.includes(token)) fail(`FEE2 home section missing: ${token}`);
}
ok('FEE2 business-oriented home');

const tableTs = read('frontend/src/app/features/fee2/fee2-table.component.ts');
const tableHtml = read('frontend/src/app/features/fee2/fee2-table.component.html');
for (const token of ['isMoneyColumn','toLocaleString','PersianDateInputComponent','loadLookups']) {
  if (!tableTs.includes(token)) fail(`FEE2 form behavior missing: ${token}`);
}
for (const token of ['app-persian-date-input','فقط مشاهده','ثبت / ویرایش کنترل‌شده']) {
  if (!tableHtml.includes(token)) fail(`FEE2 form UI missing: ${token}`);
}
ok('FEE2 governed form UX');

const workspaceTs = read('frontend/src/app/features/fee2/fee2-version-workspace.component.ts');
const workspaceHtml = read('frontend/src/app/features/fee2/fee2-version-workspace.component.html');
for (const table of ['FEE_BINDING','FEE_CONDITION','FEE_MODIFIER','FEE_TAX','FEE_SHARE']) {
  if (!workspaceTs.includes(table)) fail(`version workspace child missing: ${table}`);
}
for (const token of ['دامنه کارمزد','تعریف کارمزد','Workspace نسخه‌محور','ارسال برای بررسی','تأیید نسخه','فعال‌سازی']) {
  if (!workspaceHtml.includes(token) && !workspaceTs.includes(token)) fail(`version workspace lifecycle/UX missing: ${token}`);
}
ok('version-centric workspace and lifecycle actions');

const allNewSource = [catalog, controller, repo, service, routes, shell, home, tableTs, tableHtml, workspaceTs, workspaceHtml].join('\n');
for (const forbidden of ['ir.bank.feeengine','simple-fee-engine-r5.2','fee-studio/components']) {
  if (allNewSource.includes(forbidden)) fail(`reference-project source marker leaked into implementation: ${forbidden}`);
}
ok('reference project is not copied into current implementation');


const calculationValidator = read('backend/src/main/java/com/behsazan/corebanking/fee2/application/Fee2CalculationConfigValidator.java');
const models = read('backend/src/main/java/com/behsazan/corebanking/fee2/domain/Fee2Models.java');
if (!models.includes('CalculationConfigRequest')) fail('FEE2 calculation request contract missing');
if (!controller.includes('/versions/{id}/calculation') || !service.includes('updateCalculation(') || !repo.includes('updateCalculation(')) {
  fail('dedicated FEE2 calculation persistence API missing');
}
for (const token of ['PROGRESSIVE','BRACKET','MATRIX','USAGE','DAILY','FORMULA','validateRanges','validateMatrix','validateFormula']) {
  if (!calculationValidator.includes(token)) fail(`calculation validator contract missing: ${token}`);
}
if (!repo.includes('CALC_CONFIG_JSON=:configJson') || !repo.includes("STATUS='DRAFT'") || !repo.includes('ROW_VERSION=:rowVersion')) {
  fail('calculation persistence must be draft-only and optimistic-lock protected');
}
if (!repo.includes('"CALC_CONFIG_SCHEMA_VERSION", "CALC_CONFIG_JSON"')) fail('raw calculation JSON must be system-managed in generic FEE_VERSION form');
ok('dedicated calculation API, validation and draft-only optimistic locking');

const calculationTs = read('frontend/src/app/features/fee2/fee2-calculation-editor.component.ts');
const calculationHtml = read('frontend/src/app/features/fee2/fee2-calculation-editor.component.html');
const calculationScss = read('frontend/src/app/features/fee2/fee2-calculation-editor.component.scss');
for (const token of ['FIXED','PERCENTAGE','FIXED_PLUS_PERCENTAGE','PROGRESSIVE','BRACKET','MATRIX','USAGE','DAILY','FORMULA']) {
  if (!calculationTs.includes(`'${token}'`)) fail(`calculation editor missing method: ${token}`);
}
for (const token of ['پله‌های تدریجی','بازه‌های محاسبه','ماتریس تعرفه','مصرف و سهمیه','فرمول کنترل‌شده','ذخیره منطق محاسبه']) {
  if (!calculationHtml.includes(token)) fail(`calculation editor UI missing: ${token}`);
}
for (const token of ['addRange','addDimension','addMatrixCell','formulaIssues','validationErrors','updateCalculation']) {
  if (!calculationTs.includes(token)) fail(`calculation editor behavior missing: ${token}`);
}
if (!calculationHtml.includes('کاربر بانکی نیازی به ویرایش مستقیم JSON ندارد')) fail('business-user JSON abstraction message missing');
if (!calculationScss.includes('var(--app-primary)') || !calculationScss.includes('var(--app-border)')) fail('calculation editor must use application theme tokens');
if (!workspaceHtml.includes('app-fee2-calculation-editor') || !workspaceTs.includes('Fee2CalculationEditorComponent')) fail('calculation editor is not integrated into version workspace');
ok('nine-method business calculation editor with specialized progressive/bracket/matrix/usage/formula UX');

const phase2Source = [calculationValidator, models, controller, service, repo, calculationTs, calculationHtml, calculationScss, workspaceTs, workspaceHtml].join('\n');
for (const forbidden of ['ir.bank.feeengine','simple-fee-engine-r5.2','fee-studio/components','progressive-editor.js','formula-editor.js']) {
  if (phase2Source.includes(forbidden)) fail(`reference-project source marker leaked into phase 2 implementation: ${forbidden}`);
}
ok('phase 2 is an independent Core Banking implementation, not copied reference source');

console.log('FEE2_FORMS_VERIFY_PASS');
