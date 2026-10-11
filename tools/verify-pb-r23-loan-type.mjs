import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const read = p => fs.readFileSync(path.join(root, p), 'utf8');
const dir = 'backend/src/main/java/com/behsazan/corebanking/productbuilder/application/';
const model = read(dir + 'LoanTypeClassification.java');
const service = read(dir + 'ProductBuilderService.java');
const provider = read(dir + 'PdlReferenceOptionService.java');
const test = read('backend/src/test/java/com/behsazan/corebanking/productbuilder/application/LoanTypeClassificationR23Test.java');
const editor = read('frontend/src/app/features/product-builder/pdl-table.component.ts');
const html = read('frontend/src/app/features/product-builder/pdl-table.component.html');
const uiFiles = [
  'frontend/src/app/features/product-builder/pdl-table.component.html',
  'frontend/src/app/features/product-builder/loan-type-plan-reference.component.html',
  'frontend/src/app/features/product-builder/product-builder-home.component.html',
  'frontend/src/app/features/reference-menu/reference-menu.component.html',
  'frontend/src/app/features/reference-menu/reference-menu.component.ts',
  'frontend/src/app/features/reference-hub/reference-hub.component.html',
  'frontend/src/app/shared/ui/app-breadcrumb.component.ts'
];
let passed=0, failed=0;
function assert(label, value) { if(value) {++passed; console.log('PASS | '+label);} else {++failed; console.error('FAIL | '+label);} }
for (const [symbol,code,title] of [
  ['QARD_HASAN',1,'قرض‌الحسنه'], ['EXCHANGE',2,'مبادله‌ای'], ['PARTICIPATORY',3,'مشارکتی'], ['COMMITMENT',4,'تعهدات']
]) assert('loan group enum '+symbol, model.includes(`${symbol}(${code}, "${title}")`));
for (const [symbol,code,title] of [
  ['NOT_APPLICABLE',0,'فاقد کاربرد'], ['LETTER_OF_CREDIT',1,'اعتبارات اسنادی'], ['BANK_GUARANTEE',2,'ضمانت‌نامه']
]) assert('commitment enum '+symbol, model.includes(`${symbol}(${code}, "${title}`));
assert('loan code stays numeric and schema compatible', model.includes('new SelectOption(value.code()') && model.includes('new BigDecimal(raw.toString().trim()).intValueExact()'));
assert('taxonomies not tied to central-bank code', model.includes('not an Oracle reference-data surrogate or a CBI code'));
assert('backend validates mapping and both obligation types', model.includes('group == LoanGroup.COMMITMENT.code()') && model.includes('ObligationType.LETTER_OF_CREDIT.code()') && model.includes('ObligationType.BANK_GUARANTEE.code()'));
assert('backend rejects obligations on other groups', model.includes('else if (obligation != ObligationType.NOT_APPLICABLE.code())'));
assert('backend interest method explicitly disabled', model.includes('INTEREST_NOT_CONFIGURED = 0') && model.includes('روش محاسبه سود فعلاً غیرفعال است'));
assert('backend fills not-null interest method on create', model.includes('values.put("INTEREST_METHOD", INTEREST_NOT_CONFIGURED)'));
assert('backend does not rewrite legacy method on updates', model.includes('!sameInteger(values.get("INTEREST_METHOD"), previous.get("INTEREST_METHOD"))'));
assert('backend business gates run at both CRUD boundaries', service.includes('LoanTypeClassification.validateForSave(prepared, null)') && service.includes('LoanTypeClassification.validateForSave(merged, existing)'));
assert('descriptor serves numeric enums as controlled selectors', provider.includes('"PDL_FIXED_ENUM", LoanTypeClassification.loanGroupOptions()') && provider.includes('"PDL_FIXED_ENUM", LoanTypeClassification.obligationOptions()'));
assert('descriptor serves paused method as readonly contract', provider.includes('"PDL_DISABLED_FEATURE", LoanTypeClassification.disabledInterestOptions()'));
assert('group input changes obligation control', editor.includes("this.form.controls['LOAN_GROUP']?.valueChanges.subscribe(() => this.syncLoanTypeGroup())"));
assert('commitment form requires choice, no zero option', editor.includes('this.loanTypeHasCommitment() ? base.filter(opt => Number(opt.value) === 1 || Number(opt.value) === 2)'));
assert('other group auto assigns none and locks field', editor.includes('obligation.setValue(0, {emitEvent: false})') && editor.includes('obligation.disable({emitEvent: false})'));
assert('paused method is disabled in reactive form', editor.includes("this.form.controls['INTEREST_METHOD']?.disable({emitEvent: false})"));
assert('paused method displays as non-editable field', html.includes('@else if (isLoanTypeInterestPaused(column))') && html.includes('pausedInterestDescription()'));
assert('existing method values are retained by raw-form persistence', editor.includes('const values = this.form.getRawValue()'));
assert('frontend also validates group and type', editor.includes('گروه تسهیلات یا نوع تعهد نامعتبر است'));
assert('user understands temporary feature pause', html.includes('روش محاسبه سود فعلاً غیرفعال و سیستمی است'));
assert('reference title is unchanged', html.includes('ثبت نوع عقد مطابق جدول PDL'));
assert('reference page does not require new data or DDL', !model.includes('CREATE TABLE') && !model.includes('INSERT INTO PDL'));
assert('R23 Java regression tests cover seven cases', (test.match(/@Test\b/g) ?? []).length >= 7);
for (const ui of uiFiles) assert('consistent UI naming '+path.basename(ui), read(ui).includes('اطلاعات پایه محصول تسهیلات') && !read(ui).includes('فرم‌های مرجع محصول تسهیلات'));
assert('deposit name also follows approved naming', read('frontend/src/app/features/reference-menu/reference-menu.component.html').includes('اطلاعات پایه محصول سپرده'));
assert('R22 historical static verifier tracks current UI naming', read('tools/verify-pb-r22-loan-reference.mjs').includes("hub.includes('اطلاعات پایه محصول تسهیلات')"));
assert('Windows release build includes R23', read('build-production.cmd').includes('verify-pb-r23-loan-type.mjs'));
assert('Unix release build includes R23', read('build-production.sh').includes('verify-pb-r23-loan-type.mjs'));
console.log(`PB_R23_LOAN_TYPE_STATIC_PASS=${passed} FAIL=${failed}`);
if(failed) process.exitCode=1;
