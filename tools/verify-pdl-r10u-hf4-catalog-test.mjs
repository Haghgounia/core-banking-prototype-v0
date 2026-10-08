import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const testPath = path.join(root, 'backend/src/test/java/com/behsazan/corebanking/productbuilder/application/PdlCatalogTest.java');
const catalogPath = path.join(root, 'backend/src/main/java/com/behsazan/corebanking/productbuilder/application/PdlCatalog.java');
const buildCmdPath = path.join(root, 'build-production.cmd');
const buildShPath = path.join(root, 'build-production.sh');

const test = fs.readFileSync(testPath, 'utf8');
const catalog = fs.readFileSync(catalogPath, 'utf8');
const buildCmd = fs.readFileSync(buildCmdPath, 'utf8');
const buildSh = fs.readFileSync(buildShPath, 'utf8');

let pass = 0;
let fail = 0;
const check = (condition, message) => {
  if (condition) {
    pass += 1;
    console.log(`PASS | ${message}`);
  } else {
    fail += 1;
    console.error(`FAIL | ${message}`);
  }
};

check(catalog.includes('register("PRODUCT_ELIGIBILITY_CRITERION"'), 'R10U criterion table remains registered in PDL catalog');
check(test.includes('hasSize(54)'), 'catalog test expects 54 physical entries after R10U');
check(test.includes('"02", 10L'), 'catalog test expects 10 Common Rules entries after criterion table addition');
check(test.includes('isEqualTo(51L)'), 'catalog test expects 51 business tables after R10U');
check(test.includes('PdlCatalog.contains("PRODUCT_ELIGIBILITY_CRITERION")'), 'catalog test explicitly protects eligibility criterion registration');
check(!test.includes('hasSize(53)'), 'obsolete pre-R10U physical table count is absent');
check(!test.includes('"02", 9L'), 'obsolete pre-R10U Common Rules count is absent');
check(!test.includes('isEqualTo(50L)'), 'obsolete pre-R10U business table count is absent');
check(buildCmd.includes('verify-pdl-r10u-hf4-catalog-test.mjs'), 'Windows production build runs HF4 catalog regression guard');
check(buildSh.includes('verify-pdl-r10u-hf4-catalog-test.mjs'), 'Unix production build runs HF4 catalog regression guard');

console.log('------------------------------------------------------------');
console.log(`PDL_R10U_HF4_CATALOG_TEST_PASS=${pass}`);
console.log(`PDL_R10U_HF4_CATALOG_TEST_FAIL=${fail}`);
if (fail) process.exit(1);
console.log('PDL_R10U_HF4_CATALOG_TEST_STATIC_PASS');
