import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const read = name => fs.readFileSync(path.join(root, name), 'utf8');
const legacy = read('tools/verify-pdl-product-builder.mjs');
const workspace = read('frontend/src/app/features/product-builder/product-workspace.component.ts');
const classification = read('backend/src/main/java/com/behsazan/corebanking/productbuilder/application/LoanTypeClassification.java');
const repository = read('backend/src/main/java/com/behsazan/corebanking/productbuilder/oracle/PdlProductBuilderRepository.java');
const service = read('backend/src/main/java/com/behsazan/corebanking/productbuilder/application/ProductBuilderService.java');
let passed=0;
function verify(ok, why){if(!ok)throw new Error(why);console.log('PASS | '+why);passed++;}
verify(!legacy.includes("'VOSTRO_ACCOUNT','RETAIL_LOAN','QARD_HASAN_LOAN'"), 'Historical static loan expectation removed');
verify(legacy.includes('staticFamilyBlock'), 'Historical verifier now checks static deposit catalog only');
verify(legacy.includes('Loan group family mapping missing'), 'Historical verifier checks dynamic loan group mappings');
verify(legacy.includes('Dynamic loan family source missing'), 'Historical verifier checks PDL source');
verify(legacy.includes('Inactive loan types must not create loan families'), 'Historical verifier checks ACTIVE filter');
verify(legacy.includes('Legacy RETAIL_LOAN must not be offered on create'), 'Historical verifier rejects legacy loan on create');
verify(workspace.includes('this.service.loanProductFamilies()') && service.includes('repository.activeLoanGroupCodes()') && repository.includes('SELECT DISTINCT LOAN_GROUP FROM'), 'Product workspace uses the unpaged Oracle loan type family endpoint');
verify(!/const TARGET_PRODUCT_FAMILIES:[\s\S]*?const LOAN_FAMILY_FROM_GROUP:/.exec(workspace)?.[0].includes("domain: 'LOAN'},"), 'Static list contains no loan families');
verify(classification.includes('isLegacyRetailFamily'), 'Historical retail loans remain readable');
for(const family of ['QARD_HASAN_LOAN','EXCHANGE_LOAN','PARTICIPATORY_LOAN','COMMITMENT_LOAN'])
  verify(workspace.includes(`code: '${family}'`), 'Live group maps to '+family);
console.log('PB_R24_HF1_STATIC_PASS='+passed+' FAIL=0');
