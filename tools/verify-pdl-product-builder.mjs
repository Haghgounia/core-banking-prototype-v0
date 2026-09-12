import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const read = rel => fs.readFileSync(path.join(root, rel), 'utf8');
const exists = rel => fs.existsSync(path.join(root, rel));
const must = (condition, message) => { if (!condition) throw new Error(message); };

const routes = read('frontend/src/app/app.routes.ts');
const shell = read('frontend/src/app/layout/app-shell.component.html');
const home = read('frontend/src/app/features/product-builder/product-builder-home.component.html');
const homeTs = read('frontend/src/app/features/product-builder/product-builder-home.component.ts');
const tableTs = read('frontend/src/app/features/product-builder/pdl-table.component.ts');
const tableHtml = read('frontend/src/app/features/product-builder/pdl-table.component.html');
const workspace = read('frontend/src/app/features/product-builder/product-workspace.component.ts');
const workspaceHtml = read('frontend/src/app/features/product-builder/product-workspace.component.html');
const controller = read('backend/src/main/java/com/behsazan/corebanking/productbuilder/web/ProductBuilderController.java');
const repository = read('backend/src/main/java/com/behsazan/corebanking/productbuilder/oracle/PdlProductBuilderRepository.java');
const catalog = read('backend/src/main/java/com/behsazan/corebanking/productbuilder/application/PdlCatalog.java');
const service = read('backend/src/main/java/com/behsazan/corebanking/productbuilder/application/ProductBuilderService.java');
const validator = read('backend/src/main/java/com/behsazan/corebanking/productbuilder/application/ProductBuilderBusinessValidator.java');
const governance = read('backend/src/main/java/com/behsazan/corebanking/productbuilder/application/ProductGovernanceService.java');
const governanceWriteGuard = read('backend/src/main/java/com/behsazan/corebanking/productbuilder/application/ProductGovernanceWriteGuard.java');
const models = read('backend/src/main/java/com/behsazan/corebanking/productbuilder/domain/ProductBuilderModels.java');
const frontendModels = read('frontend/src/app/features/product-builder/product-builder.models.ts');
const yml = read('backend/src/main/resources/application.yml');
const pdlMigration = read('database/oracle/pdl/migrations/0.3.88-fix96-unified-product-builder-baseline.sql');
const dpsSeed = read('database/oracle/dps/migrations/0.3.88-fix96-unified-product-builder-reference-seed.sql');

must(routes.includes("path: 'product-builder'"), 'Product builder home route missing');
must(routes.includes("path: 'product-builder/products/:productId'"), 'Product workspace route missing');
must(routes.includes("path: 'product-builder/tables/:table'"), 'PDL generic table route missing');
must(shell.includes('routerLink="/product-builder"'), 'Separate product builder menu missing');
must(home.includes("['PDL.PRODUCT', 'PDL.PRODUCT_VERSION']"), 'Core PDL table disclosure missing');
must(home.includes('businessTableCount()') && home.includes('tableCount()'), 'Business/physical table counts missing from PDL home');
must(homeTs.includes("'11': 'حساب‌های نوسترو / وسترو'"), 'Correspondent package title missing');
must(homeTs.includes("'90': 'زیرساخت مدیریت کدها'"), 'Code infrastructure package title missing');
must(tableTs.includes('loadLookups()'), 'FK lookup loading missing from generic PDL form');
must(tableTs.includes('PersianDateInputComponent'), 'Persian DATE control missing from generic PDL form');
must(tableTs.includes('TimeInputComponent'), 'Clock TIME control missing from generic PDL form');
must(tableHtml.includes('<app-persian-date-input'), 'Persian date input not rendered by generic PDL form');
must(tableHtml.includes('<app-time-input'), 'Clock time input not rendered by generic PDL form');
must(tableTs.includes('PDL_CHILD_NAVIGATION'), 'Hierarchical PDL child-navigation map missing');
for (const token of [
  'PRODUCT_CHANNEL_OPERATION', 'CHANNEL_RULE_ID',
  'PRODUCT_PRICING_COMPONENT', 'PRICING_RULE_ID',
  'PRODUCT_RATE_TIER', 'PRICING_COMPONENT_ID',
  'LOAN_ELIGIBILITY_EXTENSION', 'ELIGIBILITY_RULE_ID',
  'DEPOSIT_PRODUCT_ALLOWED_TERM', 'TERM_RULE_ID',
  'DEPOSIT_PRODUCT_CLOSURE_PRECHECK', 'DEPOSIT_PRODUCT_CLOSURE_SETTLEMENT_RULE', 'DEPOSIT_PRODUCT_CLOSURE_APPROVAL_RULE', 'CLOSURE_RULE_ID',
  'CORRESPONDENT_ACCOUNT_SETTLEMENT_RULE', 'CORRESPONDENT_PRODUCT_PROFILE_ID'
]) must(tableTs.includes(token), `Hierarchical child-navigation token missing: ${token}`);
must(tableHtml.includes('childLinks(row)'), 'Child navigation actions are not rendered in the PDL grid');
must(tableHtml.includes('child.childFilterColumn') && tableHtml.includes('child.filterValue'), 'Child navigation must propagate locked parent context');
must(tableTs.includes("tableName() !== 'DEPOSIT_PROFIT_PAYMENT_RULE'"), 'Profit-payment UI synchronization missing');
must(workspace.includes('TARGET_PRODUCT_FAMILIES'), 'Target product-family catalog missing');
for (const family of ['CURRENT_ACCOUNT','QARD_SAVINGS','SHORT_TERM_DEPOSIT','LONG_TERM_DEPOSIT','CERTIFICATE_OF_DEPOSIT','NOSTRO_ACCOUNT','VOSTRO_ACCOUNT','RETAIL_LOAN','QARD_HASAN_LOAN']) {
  must(workspace.includes(family), `Target family missing: ${family}`);
}
must(workspace.includes('DEPOSIT_PROFIT_PAYMENT_RULE'), 'Profit payment navigation missing');
must(workspace.includes('CORRESPONDENT_ACCOUNT_PRODUCT_PROFILE'), 'Correspondent profile navigation missing');
must(workspace.includes('PRODUCT_RELATIONSHIP'), 'Product relationship navigation missing');
must(workspaceHtml.includes('<app-persian-date-input formControlName="VALID_FROM"'), 'Persian product-version dates missing');

// FIX97: the Product Workspace itself is the six-stage wizard; no parallel wizard state/table is allowed.
for (const token of ['currentStep = signal(1)', 'setStep(step: number)', 'nextStep()', 'previousStep()']) {
  must(workspace.includes(token), `Six-step wizard state/navigation missing: ${token}`);
}
for (const token of [
  'مرحله ۱', 'مرحله ۲', 'مرحله ۳', 'مرحله ۴', 'مرحله ۵', 'مرحله ۶',
  'هویت و خانواده محصول', 'نسخه، اعتبار و وضعیت‌های عملیاتی', 'مدیریت مستقیم ماژول‌های نسخه',
  'قواعد مشترک', 'قواعد تخصصی', 'Product 360، Validation و Governed Lifecycle'
]) must(workspaceHtml.includes(token), `FIX97 wizard UI token missing: ${token}`);

// Module management must persist through generic CRUD on the real PRODUCT_VERSION_MODULE table.
for (const token of [
  "this.service.rows('PRODUCT_VERSION_MODULE'",
  "this.service.create('PRODUCT_VERSION_MODULE'",
  "this.service.update('PRODUCT_VERSION_MODULE'",
  'IS_ENABLED', 'CONFIGURATION_STATUS_CODE', 'VALIDATION_STATUS_CODE'
]) must(workspace.includes(token), `Direct PRODUCT_VERSION_MODULE persistence missing: ${token}`);
must(workspaceHtml.includes("['/product-builder/tables', 'PRODUCT_VERSION_MODULE']"), 'Direct PRODUCT_VERSION_MODULE audit/table link missing');

// Family-aware applicability and readiness are part of FIX97, not a future placeholder.
for (const token of ['TERM', 'PROFIT_PAYMENT', 'CORRESPONDENT', 'NOSTRO_ACCOUNT', 'VOSTRO_ACCOUNT']) {
  must(workspace.includes(token), `Family applicability token missing: ${token}`);
}
for (const token of ['readinessChecks()', 'readinessScore()', 'enabledModuleCount()', 'isConfigured(', 'isValidated(']) {
  must(workspace.includes(token), `Readiness computation missing: ${token}`);
}
must(workspaceHtml.includes('PRODUCT_STATUS_CODE') && workspaceHtml.includes('VERSION_STATUS_CODE')
  && workspaceHtml.includes('ORIGINATION_STATUS_CODE') && workspaceHtml.includes('SERVICING_STATUS_CODE'),
  'Review status summary is incomplete');
// FIX98: Product 360, live rule validation and governed lifecycle.
for (const token of [
  '/products/{productId}/360',
  '/products/{productId}/versions/{versionId}/validate',
  '/products/{productId}/versions/{versionId}/approve',
  '/products/{productId}/versions/{versionId}/publish',
  '/products/{productId}/versions/{versionId}/draft'
]) must(controller.includes(token), `FIX98 governance endpoint missing: ${token}`);
for (const token of [
  'Product360', 'ProductModuleValidation', 'ProductReadinessItem'
]) must(models.includes(token), `FIX98 backend DTO missing: ${token}`);
for (const token of [
  'product360(', 'validateVersion(', 'approveVersion(', 'publishVersion(', 'returnToDraft(',
  'requiredModuleCodes(', 'countRequirement(', 'PRODUCT_CHANNEL_OPERATION', 'PRODUCT_PRICING_COMPONENT',
  'DEPOSIT_PRODUCT_ALLOWED_TERM', 'CORRESPONDENT_ACCOUNT_SETTLEMENT_RULE', 'LOAN_ELIGIBILITY_EXTENSION'
]) must(governance.includes(token), `FIX98 governance contract missing: ${token}`);
must(governance.includes('VERSION_STATUS_CODE", "APPROVED"'), 'Approve transition must persist APPROVED');
must(governance.includes('VERSION_STATUS_CODE", "ACTIVE"'), 'Publish transition must persist ACTIVE');
must(governance.includes('PRODUCT_STATUS_CODE", "ACTIVE"'), 'Publish transition must activate PRODUCT');
must(governance.includes('validateVersion(productId, productVersionId, actorName)'), 'Approve/Publish must re-run live validation');
for (const token of [
  'guardGovernedFieldsOnCreate', 'guardGovernedFieldsOnUpdate', 'PRODUCT_STATUS_CODE',
  'VERSION_STATUS_CODE', 'IS_CURRENT', 'APPROVED_AT', 'APPROVED_BY', 'VALIDATION_STATUS_CODE'
]) must(service.includes(token), `FIX98 governed-field CRUD guard missing: ${token}`);
for (const token of [
  'assertCreateAllowed', 'assertUpdateAllowed', 'assertDeleteAllowed', 'assertDraft',
  'PRODUCT_CHANNEL_OPERATION', 'PRODUCT_PRICING_COMPONENT', 'PRODUCT_RATE_TIER',
  'DEPOSIT_PRODUCT_ALLOWED_TERM', 'DEPOSIT_PRODUCT_CLOSURE_PRECHECK',
  'LOAN_ELIGIBILITY_EXTENSION', 'CORRESPONDENT_ACCOUNT_SETTLEMENT_RULE'
]) must(governanceWriteGuard.includes(token), `FIX98 immutable published-config guard missing: ${token}`);
must(service.includes('governanceWriteGuard.assertCreateAllowed'), 'Governance write guard not wired into create');
must(service.includes('governanceWriteGuard.assertUpdateAllowed'), 'Governance write guard not wired into update');
must(service.includes('governanceWriteGuard.assertDeleteAllowed'), 'Governance write guard not wired into delete');
for (const token of ['PdlProduct360','PdlProductModuleValidation','PdlProductReadinessItem']) {
  must(frontendModels.includes(token), `FIX98 frontend model missing: ${token}`);
}
for (const token of [
  'product360 = signal<PdlProduct360 | null>', 'validateGovernance()', 'approveGovernance()',
  'publishGovernance()', 'returnGovernanceToDraft()', 'ruleCountEntries()'
]) must(workspace.includes(token), `FIX98 workspace integration missing: ${token}`);
for (const token of [
  'Unified Product Builder · FIX98', 'Product 360 و Governance', 'اعتبارسنجی Ruleها',
  'تصویب نسخه', 'Publish نسخه', 'Validation ماژول‌ها از روی Rule Data واقعی', 'Product 360 — Rule Coverage'
]) must(workspaceHtml.includes(token), `FIX98 governance UI token missing: ${token}`);
must(!workspaceHtml.includes('(selectionChange)="setModuleValidationStatus'), 'Validation status must not be manually editable in FIX98');
must(workspace.includes("VALIDATION_STATUS_CODE: 'NOT_VALIDATED'"), 'Module changes must invalidate previous validation');
must(workspace.includes("delete values['PRODUCT_STATUS_CODE']"), 'Product lifecycle status must be stripped from normal update payload');
must(workspace.includes("delete payload['VERSION_STATUS_CODE']"), 'Version lifecycle status must be stripped from normal update payload');
must(workspace.includes('this.productForm.controls.PRODUCT_STATUS_CODE.disable'), 'Product lifecycle field must be explicitly system-managed');
must(workspace.includes('this.versionForm.controls.VERSION_STATUS_CODE.disable'), 'Version lifecycle field must be explicitly system-managed');
must(workspace.includes("moduleCode: 'LOAN_ELIGIBILITY'"), 'Loan eligibility must navigate through PRODUCT_ELIGIBILITY_RULE parent context');

must(controller.includes('/tables/{table}/rows'), 'Generic PDL CRUD API missing');
must(controller.includes('/products/{productId}/workspace'), 'Product workspace API missing');
must(repository.includes('ALL_TAB_COLUMNS'), 'Oracle column metadata discovery missing');
must(repository.includes('ALL_CONSTRAINTS'), 'Oracle constraint metadata discovery missing');
must(repository.includes('ALL_CONS_COLUMNS'), 'Oracle FK/PK metadata discovery missing');
must(repository.includes('LOCK TABLE'), 'Prototype numeric PK allocator guard missing');
must(repository.includes('IS_DELETED = 1'), 'Logical delete support missing');
must(service.includes('businessValidator.validate'), 'PDL business validator not wired into service');
must(validator.includes('DEPOSIT_PROFIT_PAYMENT_RULE'), 'Profit-payment validator missing');
must(validator.includes('CORRESPONDENT_ACCOUNT_SETTLEMENT_RULE'), 'Correspondent settlement validator missing');
must(yml.includes('product-definition: PDL'), 'PDL schema configuration missing');

for (const table of ['DEPOSIT_PROFIT_PAYMENT_RULE','CORRESPONDENT_ACCOUNT_PRODUCT_PROFILE','CORRESPONDENT_ACCOUNT_SETTLEMENT_RULE']) {
  must(catalog.includes(`register("${table}"`), `New PDL catalog table missing: ${table}`);
  must(pdlMigration.includes(`CREATE TABLE PDL.${table}`), `Migration DDL missing: ${table}`);
}
for (const family of ['SHORT_TERM_DEPOSIT','LONG_TERM_DEPOSIT','NOSTRO_ACCOUNT','VOSTRO_ACCOUNT']) {
  must(dpsSeed.includes(`'${family}'`), `DPS semantic seed missing: ${family}`);
}

const registrations = [...catalog.matchAll(/register\("([A-Z0-9_$#]+)"[^\n]*"([0-9]+)"/g)].map(m => ({table:m[1], pkg:m[2]}));
must(registrations.length === 53, `Expected 53 physical PDL catalog tables, found ${registrations.length}`);
const counts = registrations.reduce((m, item) => (m[item.pkg] = (m[item.pkg] ?? 0) + 1, m), {});
const expected = {'01':5,'02':9,'03':14,'04':6,'05':14,'11':2,'90':3};
for (const [pkg, count] of Object.entries(expected)) must(counts[pkg] === count, `Package ${pkg}: expected ${count}, found ${counts[pkg] ?? 0}`);
const businessCount = registrations.filter(item => item.pkg !== '90').length;
must(businessCount === 50, `Expected 50 business-model tables, found ${businessCount}`);
for (const table of ['CODE_SET','CODE_VALUE','CODE_VALUE_TRANSITION']) {
  const entry = registrations.find(item => item.table === table);
  must(entry?.pkg === '90', `${table} must be isolated in package 90`);
}

for (const rel of [
  'database/oracle/pdl/migrations/0.3.88-fix96-unified-product-builder-baseline.sql',
  'database/oracle/dps/migrations/0.3.88-fix96-unified-product-builder-reference-seed.sql'
]) must(exists(rel), `Missing migration: ${rel}`);

console.log('PDL unified product builder static verification: OK');
console.log(`Business model tables: ${businessCount}`);
console.log(`Physical PDL catalog tables: ${registrations.length}`);
console.log('Package distribution:', JSON.stringify(counts));
