import fs from 'node:fs';
import path from 'node:path';

const root = process.cwd();
const read = rel => fs.readFileSync(path.join(root, rel), 'utf8');
const must = (condition, message) => { if (!condition) throw new Error(message); };

const routes = read('frontend/src/app/app.routes.ts');
const shell = read('frontend/src/app/layout/app-shell.component.html');
const home = read('frontend/src/app/features/product-builder/product-builder-home.component.html');
const table = read('frontend/src/app/features/product-builder/pdl-table.component.ts');
const workspace = read('frontend/src/app/features/product-builder/product-workspace.component.ts');
const controller = read('backend/src/main/java/com/behsazan/corebanking/productbuilder/web/ProductBuilderController.java');
const repository = read('backend/src/main/java/com/behsazan/corebanking/productbuilder/oracle/PdlProductBuilderRepository.java');
const catalog = read('backend/src/main/java/com/behsazan/corebanking/productbuilder/application/PdlCatalog.java');
const yml = read('backend/src/main/resources/application.yml');

must(routes.includes("path: 'product-builder'"), 'Product builder home route missing');
must(routes.includes("path: 'product-builder/products/:productId'"), 'Product workspace route missing');
must(routes.includes("path: 'product-builder/tables/:table'"), 'PDL generic table route missing');
must(shell.includes('routerLink="/product-builder"'), 'Separate product builder menu missing');
must(home.includes("['PDL.PRODUCT', 'PDL.PRODUCT_VERSION']"), 'Core PDL table disclosure missing');
must(table.includes('loadLookups()'), 'FK lookup loading missing from generic PDL form');
must(workspace.includes("PRODUCT_CLASS_CODE"), 'Product class form missing');
must(workspace.includes("DEPOSIT_PRODUCT_PROFILE"), 'Deposit rule navigation missing');
must(workspace.includes("LOAN_PRODUCT_PROFILE"), 'Loan rule navigation missing');
must(controller.includes('/tables/{table}/rows'), 'Generic PDL CRUD API missing');
must(controller.includes('/products/{productId}/workspace'), 'Product workspace API missing');
must(repository.includes("ALL_TAB_COLUMNS"), 'Oracle column metadata discovery missing');
must(repository.includes("ALL_CONSTRAINTS"), 'Oracle constraint metadata discovery missing');
must(repository.includes("ALL_CONS_COLUMNS"), 'Oracle FK/PK metadata discovery missing');
must(repository.includes("LOCK TABLE"), 'Prototype numeric PK allocator guard missing');
must(repository.includes("IS_DELETED = 1"), 'Logical delete support missing');
must(yml.includes('product-definition: PDL'), 'PDL schema configuration missing');

const entries = [...catalog.matchAll(/register\("[A-Z0-9_$#]+"/g)].length;
must(entries === 50, `Expected 50 PDL catalog tables, found ${entries}`);

console.log('PDL product builder static verification: OK');
console.log(`Catalog tables: ${entries}`);
