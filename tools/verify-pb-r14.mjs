import {readFileSync} from 'node:fs';
import {resolve, dirname} from 'node:path';
import {fileURLToPath} from 'node:url';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const file = path => readFileSync(resolve(root,path), 'utf8');
const backend = file('backend/src/main/java/com/behsazan/corebanking/productbuilder/application/PdlReferenceOptionService.java');
const rules = file('backend/src/main/java/com/behsazan/corebanking/productbuilder/application/ProductBuilderBusinessValidator.java');
const errors = file('backend/src/main/java/com/behsazan/corebanking/shared/error/GlobalExceptionHandler.java');
const ui = file('frontend/src/app/features/product-builder/pdl-table.component.ts');
const html = file('frontend/src/app/features/product-builder/pdl-table.component.html');
const ref = file('docs/reference/Unified_Product_Builder_Interactive_Forms_FA.html');
const dps = file('backend/src/main/java/com/behsazan/corebanking/deposit/productfactory/reference/DepositProductReferenceDescriptorProvider.java');
const diag = file('database/oracle/pdl/diagnostics/pb-r14-joint-hold-closure-readiness.sql');
const model = JSON.parse(ref.slice(ref.indexOf('const MODEL=')+'const MODEL='.length,ref.indexOf(';',ref.indexOf('const MODEL='))));
const options = (table,column) => model.find(m=>m.name===table)?.columns.find(c=>c.name===column)?.options ?? [];
const check = (desc,ok) => {if(ok)passed++; console.log(`${ok?'PASS':'FAIL'} | ${desc}`);return ok?0:1; };
let failed=0;
let passed=0;
for (const [column, resource] of Object.entries({
 HOLD_TYPE_CODE:'dps-hold-types', CLOSURE_TYPE_CODE:'dps-closure-types',
 BALANCE_DESTINATION_CODE:'dps-balance-destinations', SETTLEMENT_METHOD_CODE:'dps-settlement-methods',
 DESTINATION_CODE:'dps-destinations'
})) failed += check(`${column} uses actual DPS descriptor ${resource}`,
 backend.includes(`Map.entry("${column}", "${resource}")`) && dps.includes(`spec("${resource}",`));
failed += check('Closure STATUS_CODE is scoped to rule statuses and does not affect other tables',
 backend.includes('"DEPOSIT_PRODUCT_CLOSURE_RULE".equals(normalize(table)) && "STATUS_CODE".equals(name)') && backend.includes('resource = "dps-rule-statuses"'));
failed += check('Canonical CHECK-backed options explicitly cover hold, closure, settlement fields',
 ['DEPOSIT_PRODUCT_HOLD_RULE.HOLD_TYPE_CODE','DEPOSIT_PRODUCT_CLOSURE_RULE.CLOSURE_TYPE_CODE',
 'DEPOSIT_PRODUCT_CLOSURE_RULE.BALANCE_DESTINATION_CODE','DEPOSIT_PRODUCT_CLOSURE_RULE.STATUS_CODE',
 'DEPOSIT_PRODUCT_CLOSURE_SETTLEMENT_RULE.SETTLEMENT_METHOD_CODE','DEPOSIT_PRODUCT_CLOSURE_SETTLEMENT_RULE.DESTINATION_CODE']
 .every(v=>backend.includes(`"${v}"`)) && backend.includes('checkedDepositOptions(column.options())'));
failed += check('Fallback is bound to Oracle CHECK and never an unverified hardcoded option set',
 backend.includes('if (CHECK_BACKED_FIELDS.contains(fieldKey) && !column.options().isEmpty())') &&
 backend.includes('PDL_CHECK_CONSTRAINT') && backend.includes('if (label != null)'));
for (const [column,mapping] of Object.entries({
 OWNERSHIP_TYPE_CODE:['"1", "SINGLE"','"2", "JOINT"'],
 SIGNING_RULE_CODE:['"1", "ANY_TO_SIGN"','"2", "BOTH_TO_SIGN"'],
 PROFIT_DISTRIBUTION_CODE:['"1", "EQUAL"','"2", "OWNERSHIP_SHARE"','"3", "CUSTOM_PERCENT"']
})) failed += check(`Joint ${column}: historic numeric DPS codes translate before PDL persistence`,
 backend.includes(`"${column}", Map.of(`) && mapping.every(v=>backend.includes(v)) && backend.includes('mapJointOptions(name, column.options(), governed)'));
failed += check('Reference PDL table options agree with translated codes',
 ['SINGLE','JOINT'].every(v=>options('DEPOSIT_PRODUCT_JOINT_RULE','OWNERSHIP_TYPE_CODE').some(o=>o[0]===v)) &&
 ['ANY_TO_SIGN','BOTH_TO_SIGN'].every(v=>options('DEPOSIT_PRODUCT_JOINT_RULE','SIGNING_RULE_CODE').some(o=>o[0]===v)) &&
 ['OWNERSHIP_SHARE','EQUAL','CUSTOM_PERCENT'].every(v=>options('DEPOSIT_PRODUCT_JOINT_RULE','PROFIT_DISTRIBUTION_CODE').some(o=>o[0]===v)));
failed += check('Joint minimum/maximum owner/signature counts validated at API boundary',
 rules.includes('case "DEPOSIT_PRODUCT_JOINT_RULE" -> validateJointOwnerCounts(values)') && rules.includes('requiredSigners > maximumOwners'));
failed += check('UI renders governed selectors; result values are PDL semantic codes',
 html.includes('<mat-select [formControl]="control(column)"') &&
 backend.includes('new SelectOption(persistedCode, persistedCode, reference.label())'));
failed += check('PDL CHECK fallback is presented with truthful source hint',
 html.includes("column.referenceSource === 'PDL_CHECK_CONSTRAINT'"));
failed += check('HTML reference has real hold, closure, destination, status choices',
 ['HOLD_TYPE_CODE','CLOSURE_TYPE_CODE','BALANCE_DESTINATION_CODE','STATUS_CODE'].every((col,i)=>
 options(['DEPOSIT_PRODUCT_HOLD_RULE','DEPOSIT_PRODUCT_CLOSURE_RULE','DEPOSIT_PRODUCT_CLOSURE_RULE','DEPOSIT_PRODUCT_CLOSURE_RULE'][i],col).length > 0));
failed += check('Oracle ORA-02290 diagnostic maps to safe Persian business message',
 errors.includes('ORA-02290') && errors.includes('DATABASE_CHECK_CONSTRAINT'));
failed += check('UI surfaces Spring ProblemDetail and avoids generic unexpected error',
 ui.includes('instanceof HttpErrorResponse') && ui.includes('problem.detail') && ui.includes('HTTP ${error.status}'));
failed += check('Existing conditional FK lookup cannot override mapped/check-backed source',
 ui.includes('DPS_MAPPED:|PDL_CHECK_CONSTRAINT$'));
failed += check('Read-only Oracle diagnostic contains no business writes',
 ['INSERT INTO','UPDATE PDL.','DELETE FROM','MERGE INTO','CREATE TABLE','ALTER TABLE'].every(s=>!diag.includes(s)) && diag.includes('SEARCH_CONDITION_VC'));
failed += check('PB-R12 clone/delete confirmations remain in the grid',
 html.includes('(click)="clone(row)"') && ui.includes('window.confirm('));
console.log(`PB_R14_STATIC_PASS=${passed} FAIL=${failed}`);
if(failed) process.exitCode=1;
