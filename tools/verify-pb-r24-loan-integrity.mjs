import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const read = p => fs.readFileSync(path.join(root,p),'utf8');
const app = 'frontend/src/app/features/product-builder/';
const home = read(app+'product-builder-home.component.html');
const work = read(app+'product-workspace.component.ts');
const workHtml = read(app+'product-workspace.component.html');
const pdl = read(app+'pdl-table.component.ts');
const pdlHtml = read(app+'pdl-table.component.html');
const menu = read('frontend/src/app/features/reference-menu/reference-menu.component.ts');
const routes = read('frontend/src/app/app.routes.ts');
const labels = read(app+'product-business-labels.ts');
const backend = read('backend/src/main/java/com/behsazan/corebanking/productbuilder/application/ProductBuilderService.java');
const repo = read('backend/src/main/java/com/behsazan/corebanking/productbuilder/oracle/PdlProductBuilderRepository.java');
const model = read('backend/src/main/java/com/behsazan/corebanking/productbuilder/application/LoanTypeClassification.java');
const validator = read('backend/src/main/java/com/behsazan/corebanking/productbuilder/application/ProductBuilderBusinessValidator.java');
const options = read('backend/src/main/java/com/behsazan/corebanking/productbuilder/application/PdlReferenceOptionService.java');
const tests = read('backend/src/test/java/com/behsazan/corebanking/productbuilder/application/LoanProductContractR24Test.java');
let pass=0,fail=0;
function ck(label,ok){ if(ok){pass++; console.log('PASS | '+label);}else{fail++;console.error('FAIL | '+label);} }
const pairs = [
  ['QARD_HASAN', 'QARD_HASAN_LOAN', 1, 'تسهیلات قرض‌الحسنه'],
  ['EXCHANGE', 'EXCHANGE_LOAN', 2, 'تسهیلات مبادله‌ای'],
  ['PARTICIPATORY', 'PARTICIPATORY_LOAN', 3, 'تسهیلات مشارکتی'],
  ['COMMITMENT', 'COMMITMENT_LOAN', 4, 'تعهدات اعتباری']
];
ck('Product Builder home has exact deposit reference card', home.includes('routerLink="/deposit/reference-data"') && home.includes('<strong>اطلاعات پایه محصول سپرده</strong>'));
ck('existing loan reference card retained and correctly named', home.includes('routerLink="/loan/reference-data"') && home.includes('<strong>اطلاعات پایه محصول تسهیلات</strong>'));
ck('deposit and loan reference routes exist', routes.includes("path: 'deposit/reference-data'") && routes.includes("path: 'loan/reference-data'"));
ck('existing product and code-set cards unchanged', home.includes('routerLink="/product-builder/products"') && home.includes("'CODE_SET'"));
ck('Product Builder uses existing quick-card theme', home.includes('class="quick-grid"') && home.includes('class="quick-card card"'));
ck('new LOAN families removed from stale static TARGET list', !/const TARGET_PRODUCT_FAMILIES[\s\S]*?const LOAN_FAMILY_FROM_GROUP/.exec(work)?.[0].includes("domain: 'LOAN'},"));
ck('live LOAN_TYPE reference accessed by Product Builder', work.includes('this.service.loanProductFamilies()') && backend.includes('repository.activeLoanGroupCodes()'));
ck('only ACTIVE loan types contribute to family list', repo.includes("WHERE RECORD_STATUS_CODE='ACTIVE' AND LOAN_GROUP BETWEEN 1 AND 4"));
ck('family list uses unpaged server DISTINCT query', repo.includes('SELECT DISTINCT LOAN_GROUP FROM') && backend.includes('loanProductFamilies()') && work.includes('this.loanFamilyLoadError.set(this.message(e))'));
ck('new family selection blocked before lookup completes', work.includes('this.loanFamilyLoading()') && work.includes('this.productId() === null && !this.loanFamilyOptions().some'));
ck('old families retained for existing products only', work.includes('this.productId() !== null && current') && work.includes('PRODUCT_FAMILY_LABELS[current]'));
ck('family selector describes Mudaraba correctly', workHtml.includes('مضاربه') && workHtml.includes('تسهیلات مشارکتی'));
ck('missing reference links to loan type input', workHtml.includes('routerLink="/loan/reference-data/LOAN_TYPE"'));
ck('family selector provides loading and error states', workHtml.includes('loanFamilyLoading()') && workHtml.includes('loanFamilyLoadError()'));
for(const [symbol,family,group,label] of pairs){
  ck('dynamic family '+family, work.includes(`${group}: {code: '${family}', label: '${label}'`));
  ck('server group '+symbol, model.includes(`case ${symbol} -> "${family}"`));
  ck('family translation '+family, labels.includes(family+": '"+label+"'"));
}
ck('server checks loan group membership in real PDL', repo.includes('activeLoanGroupExists(int groupCode)') && repo.includes("LOAN_GROUP = :groupCode AND RECORD_STATUS_CODE = 'ACTIVE'"));
ck('server validates family on create and update', backend.includes('validateLoanProductFamily(table, prepared, null)') && backend.includes('validateLoanProductFamily(table, merged, existing)'));
ck('loan product cannot become deposit when profile exists', backend.includes('دامنه محصول دارای پروفایل تسهیلات قابل تغییر نیست'));
ck('existing loan currency and family protected by profile links', backend.includes('repository.loanProductHasProfiles(id)') && backend.includes('DEFAULT_CURRENCY_CODE'));
ck('loan type group locked if linked profile exists', backend.includes('repository.loanTypeHasProfiles(id)'));
ck('family equality does not bypass DEPOSIT to LOAN transition', backend.includes('"LOAN".equals(text(previous.get("PRODUCT_CLASS_CODE")))'));
ck('type/group/family compatibility enforced at backend profile save', backend.includes('group != number(type.get("LOAN_GROUP"))'));
ck('legacy RETAIL_LOAN remains readable only', model.includes('isLegacyRetailFamily') && !work.includes("{code: 'RETAIL_LOAN', label:"));
ck('loan type selection is limited to product business group', pdl.includes('loanAllowedTypeIds') && pdl.includes('filterColumn:\'LOAN_GROUP\',filterValue:group'));
ck('type catalog uses ACTIVE records and all pages', pdl.includes("String(row['RECORD_STATUS_CODE']).trim().toUpperCase() === 'ACTIVE'") && pdl.includes("received >= result.totalElements"));
ck('loan type list is filtered in selector', pdl.includes("column.name === 'LOAN_TYPE_ID'") && pdl.includes('base.filter(item => allowed.includes(Number(item.value)))'));
ck('disallowed type is blocked before profile save', pdl.includes('!this.loanAllowedTypeIds()?.includes(selectedType)'));
ck('type plan mapping check retained', backend.includes('loanTypePlanAllowed(type, plan)') && pdl.includes('this.loanPlanAllowedIds()?.includes(plan)'));
ck('classification reference form is in LOAN reference menu', menu.includes("table: 'FACILITY_OPERATION_PARAMETER', title: 'الگوی طبقه‌بندی و انتقال مطالبات'"));
ck('classification reference has create/update generic route', routes.includes("path: 'loan/reference-data/:table'") && routes.includes('PdlTableComponent'));
ck('classification editor groups durations, transfer, and status', pdl.includes('FACILITY_OPERATION_PARAMETER: {') && pdl.includes('آستانه‌های طبقه‌بندی مطالبات') && pdl.includes('روش انتقال مطالبات به طبقات'));
ck('four transfer dimensions are controlled switches', ['TRANSFER_PRINCIPLE','TRANSFER_INTEREST','TRANSFER_PENALTY','TRANSFER_COMMISSION'].every(x=>pdl.includes("'"+x+"'")) && pdl.includes("return this.booleanFieldChecked(column) ? 'انتقال می‌یابد'") && pdlHtml.includes('loanSwitchLabel(column)'));
ck('classification status and method not fabricated', pdl.includes('معنای کدهای ۰ تا ۹ در DDL تعریف نشده') && pdl.includes('PARAMETER_STATUS_CODE'));
ck('classifier has 1..99 Oracle NUMBER2 range check', validator.includes('days < 1 || days > 99'));
ck('classifier has NUMBER1 transfer method bounds', validator.includes('method < 0 || method > 9'));
ck('classifier enforces transfer 0/1', validator.includes('flag != 0 && flag != 1'));
ck('classifier avoids schema migration/seeds', !/\b(CREATE TABLE|ALTER TABLE|INSERT INTO PDL)\b/i.test(read('backend/src/test/java/com/behsazan/corebanking/productbuilder/application/LoanProductContractR24Test.java')));
ck('loan profile links directly to classifier form', pdlHtml.includes('routerLink="/loan/reference-data/FACILITY_OPERATION_PARAMETER"'));
ck('loan applicant checkbox is real Material input', pdlHtml.includes('<mat-checkbox [checked]="loanApplicantAll.value"') && pdl.includes('MatCheckboxModule'));
ck('checked applicant wildcard uses ALL', pdl.includes("nature.setValue('ALL'") && pdlHtml.includes('همه موارد (بدون محدودیت ماهیت متقاضی)'));
ck('unchecked applicant requires explicit NATURAL / LEGAL', pdl.includes('nature.setValue(null') && pdlHtml.includes("option.code !== 'ALL'"));
ck('backend reference options contain applicant ALL', options.includes('"LOAN_PRODUCT_PROFILE.PARTY_NATURE_CODE"') && options.includes('"ALL", "همه موارد", "NATURAL", "حقیقی", "LEGAL", "حقوقی"'));
ck('backend rejects ungoverned applicant nature', validator.includes('Set.of("ALL", "NATURAL", "LEGAL")'));
ck('no duplicate editable loan currency', pdl.includes("(this.tableName() === 'LOAN_PRODUCT_PROFILE' && column.name === 'CURRENCY_TYPE_CODE')"));
ck('loan currency is visibly inherited from product', pdlHtml.includes('ارز از هویت محصول خوانده می‌شود') && pdl.includes('loadLoanProductCurrency()'));
ck('UI fetches canonical product-version currency', pdl.includes("this.service.row('PRODUCT_VERSION', version)") && pdl.includes("this.service.row('PRODUCT', Number(versionRow['PRODUCT_ID']))"));
ck('UI verifies ISO3 currency and rejects mismatched legacy row', pdl.includes('/^[A-Z]{3}$/.test(currency)') && pdl.includes('ارز پروفایل موجود با ارز اصلی محصول مغایرت دارد'));
ck('backend derives NOT NULL profile currency on create', backend.includes('prepared.put("CURRENCY_TYPE_CODE", currency)'));
ck('backend blocks profile currency override', backend.includes('prepared.remove("CURRENCY_TYPE_CODE")') && backend.includes('ارز پروفایل مستقلاً قابل تغییر نیست'));
ck('backend blocks editing inconsistent historic profile', backend.includes('ارز پروفایل موجود با هویت محصول مغایرت دارد'));
ck('backend disallows changing existing profile version', backend.includes('نسخه پروفایل موجود ثابت است'));
ck('form preserves raw required currency in payload', pdl.includes('this.form.getRawValue()') && pdl.includes("this.form.controls['CURRENCY_TYPE_CODE']?.setValue(currency"));
ck('repayment mandatory PARTIAL_PREPAYMENT_ALLOWED resets to 0 not NULL', pdl.includes("name === 'PARTIAL_PREPAYMENT_ALLOWED' ? 0 : null"));
ck('new JUnit covers all scopes', ['businessGroupsMapToFamilies','applicantNatureAllIsValid','patternRejectsZeroAndOutOfRangeDurations','transferFlagsMustBeBinary','transferMethodMustFitOracleNumberOneWithoutInventedSemantics'].every(x=>tests.includes(x)));
console.log(`PB_R24_LOAN_STATIC_PASS=${pass} FAIL=${fail}`);
if(fail)process.exitCode=1;
