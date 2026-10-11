/**
 * Product Builder 2026-10-11 UX/business-regression contract.
 * No Oracle fixture or network access is needed. Use runtime UAT separately.
 */
import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const read = file => fs.readFileSync(path.join(root, file), 'utf8');
const front = 'frontend/src/app/features/product-builder/';
const java = 'backend/src/main/java/com/behsazan/corebanking/productbuilder/';
const ui = read(front + 'pdl-table.component.ts');
const html = read(front + 'pdl-table.component.html');
const serviceUi = read(front + 'product-builder.service.ts');
const product = read(front + 'product-workspace.component.ts');
const backend = read(java + 'application/ProductBuilderService.java');
const validator = read(java + 'application/ProductBuilderBusinessValidator.java');
const options = read(java + 'application/PdlReferenceOptionService.java');
const controller = read(java + 'web/ProductBuilderController.java');
let okCount = 0, failCount = 0;
function check(id, success) {
  if (success) {okCount++; console.log('PASS | ' + id);}
  else {failCount++; console.error('FAIL | ' + id);}
}
check('01 direct full-product management', html.includes('مدیریت کامل محصول') && ui.includes("['/product-builder/products', id]"));
check('02 eligibility extension remains child action', ui.includes("{table: 'LOAN_ELIGIBILITY_EXTENSION'") && html.includes('[routerLink]="[\'/product-builder/tables\', child.table]"'));
check('02 eligibility parent PK linked on creation', ui.includes("this.form.addControl('ELIGIBILITY_RULE_ID'") && ui.includes("table === 'LOAN_ELIGIBILITY_EXTENSION'"));
check('03 allowed channel operations retained with useful help', ui.includes("{table: 'PRODUCT_CHANNEL_OPERATION'") && ui.includes('ماتریس مجوز عملیات هر کانال'));
check('04 org units use real multiple checkboxes', html.includes('<mat-checkbox [checked]="orgUnitSelected(option.value)"') && html.includes('toggleOrgUnit(option.value, $event.checked)'));
check('04 org units persisted through dedicated contract', serviceUi.includes('saveOrgScopes(') && controller.includes('@PostMapping("/org-scopes/bulk")'));
check('04 org scope bulk updates atomic normalized rows', backend.includes('@Transactional\n    public List<Map<String, Object>> saveOrgScopes') && backend.includes('record.put("ORG_UNIT_ID", unitId)') && backend.includes('occupied::contains'));
check('04 product allowlist default on and hidden', ui.includes("column.name === 'IS_ALLOWED') value = 1") && ui.includes('this.isOrgScopePermission(column)'));
check('05 required inquiries default required and hidden', ui.includes("column.name === 'IS_MANDATORY') value = 1") && ui.includes('this.isRequiredInquiryFlag(column)'));
check('05 required inquiries backend rejects optional new row', backend.includes('validateInquiryRequiredForCreate(table, prepared)'));
check('06 value-unit ordering generic', ui.includes('private unitFirst(') && ui.includes('this.unitFirst(result)'));
check('06 eligibility tenure units precede amounts', html.indexOf('<mat-label>واحد</mat-label><mat-select [formControl]="eligibilityCustomerTenureUnit"') < html.indexOf('<mat-label>مقدار</mat-label><input matInput type="number" min="0" [formControl]="eligibilityCustomerTenureValue"'));
check('06 inquiry validity unit before value', html.includes('<mat-label>واحد</mat-label><mat-select [formControl]="inquiryAgeUnit"') && html.indexOf('[formControl]="inquiryAgeUnit"') < html.indexOf('[formControl]="inquiryAgeValue"'));
const specialized = product.match(/readonly loanRules:[\s\S]*?= \[([\s\S]*?)\];/)?.[1] ?? '';
check('07 eligibility only in shared rules', !specialized.includes("table: 'PRODUCT_ELIGIBILITY_RULE'") && product.includes("readonly commonRules:") && product.includes("table: 'PRODUCT_ELIGIBILITY_RULE'"));
check('08 finance discount switch reactive', ui.includes("check('INTEREST_DISCOUNT_ALLOWED', ['INTEREST_DISCOUNT_DAY'])") && ui.includes("valueChanges.subscribe(() => this.syncLoanConditionalFields())"));
check('08 finance backend guards discounted days', validator.includes('!bool(values, "INTEREST_DISCOUNT_ALLOWED") && values.get("INTEREST_DISCOUNT_DAY") != null'));
check('09 installment/participation modes from actual loan group', ui.includes("[1,2].includes(group) ? 'INSTALLMENT'") && ui.includes("group === 3 ? 'PARTICIPATORY'"));
check('09 backend verifies installment month, participation day', backend.includes('matchesDurationUnit(text(values.get("INSTALLMENT_INTERVAL_UNIT_CODE")), "MONTH")') && backend.includes('matchesDurationUnit(text(values.get("TERM_UNIT_CODE")), "DAY")'));
check('09 loan repayment rule excludes unrelated inputs', ui.includes('repaymentFieldHidden(') && ui.includes('MIN_INSTALLMENT_COUNT') && ui.includes('MIN_TERM_DURATION'));
check('10 governed duration code not guessed fixed set', backend.includes('referenceOptionService.matchesDurationUnit(unit, "DAY")') && options.includes('referenceOptions("dps-term-units")'));
console.log('PDL_2026_10_11_UX_PASS=' + okCount + ' FAIL=' + failCount);
if (failCount) process.exit(1);
