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
must(tableTs.includes("tableName() !== 'DEPOSIT_PROFIT_PAYMENT_RULE'"), 'Profit-payment UI synchronization missing');
must(workspace.includes('TARGET_PRODUCT_FAMILIES'), 'Target product-family catalog missing');
for (const family of ['CURRENT_ACCOUNT','QARD_SAVINGS','SHORT_TERM_DEPOSIT','LONG_TERM_DEPOSIT','CERTIFICATE_OF_DEPOSIT','NOSTRO_ACCOUNT','VOSTRO_ACCOUNT','RETAIL_LOAN','QARD_HASAN_LOAN']) {
  must(workspace.includes(family), `Target family missing: ${family}`);
}
must(workspace.includes('DEPOSIT_PROFIT_PAYMENT_RULE'), 'Profit payment navigation missing');
must(workspace.includes('CORRESPONDENT_ACCOUNT_PRODUCT_PROFILE'), 'Correspondent profile navigation missing');
must(workspace.includes('PRODUCT_RELATIONSHIP'), 'Product relationship navigation missing');
must(workspaceHtml.includes('<app-persian-date-input formControlName="VALID_FROM"'), 'Persian product-version dates missing');
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
