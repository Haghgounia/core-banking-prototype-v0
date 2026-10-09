import {readFileSync} from 'node:fs';
import {resolve, dirname} from 'node:path';
import {fileURLToPath} from 'node:url';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const get = file => readFileSync(resolve(root, file), 'utf8');
const base = 'frontend/src/app/features/product-builder/';
const ts = get(base + 'pdl-table.component.ts');
const html = get(base + 'pdl-table.component.html');
const css = get(base + 'pdl-table.component.scss');
const list = get(base + 'product-list.component.html');
const labels = get(base + 'product-reference-field-labels.ts');
const ref = get('docs/reference/Unified_Product_Builder_Interactive_Forms_FA.html');
const options = get('backend/src/main/java/com/behsazan/corebanking/productbuilder/application/PdlReferenceOptionService.java');
const validator = get('backend/src/main/java/com/behsazan/corebanking/productbuilder/application/ProductBuilderBusinessValidator.java');
const repo = get('backend/src/main/java/com/behsazan/corebanking/productbuilder/oracle/PdlProductBuilderRepository.java');
const test = get('backend/src/test/java/com/behsazan/corebanking/productbuilder/application/ProductBuilderBusinessValidatorTest.java');
const testOptions = get('backend/src/test/java/com/behsazan/corebanking/productbuilder/application/PdlReferenceOptionContractTest.java');
const win = get('build-production.cmd');
const unix = get('build-production.sh');
const model = JSON.parse(ref.slice(ref.indexOf('const MODEL=') + 'const MODEL='.length, ref.indexOf(';', ref.indexOf('const MODEL='))));
const opening = model.find(table => table.name === 'DEPOSIT_PRODUCT_OPENING_RULE');
const finance = ['MIN_OPENING_AMOUNT','MAX_OPENING_AMOUNT','MIN_REQUIRED_BALANCE','MAX_ALLOWED_BALANCE'];
// Prevent TypeScript TS1117 in the business-label map (no npm dependencies required).
const fieldLabelMap = ts.match(/const labels:\s*Readonly<Record<string, string>>\s*=\s*\{([\s\S]*?)\n\s*\};/);
const labelKeys = [...(fieldLabelMap?.[1] ?? '').matchAll(/^\s*'([^']+)'\s*:/gm)].map(m => m[1]);
const uniqueFieldLabels = !!fieldLabelMap && labelKeys.length > 0 && new Set(labelKeys).size === labelKeys.length;
const checks = [
  ['field label map has no duplicate object keys (TS1117)', uniqueFieldLabels],
  ['closure details are not mixed with CRUD actions', html.includes('<th class="child-cell">جزئیات وابسته</th>') && html.includes('<th class="actions-cell">عملیات رکورد</th>')],
  ['closure details link invokes navigation, not delete', html.includes('$event.stopPropagation(); openChild(row, child)') && ts.includes('await this.router.navigate')],
  ['row delete can only be invoked by its distinct action', html.includes('$event.stopPropagation(); remove(row)') && html.includes('class="row-action delete-action"')],
  ['delete confirmation remains before actual service deletion', ts.indexOf('window.confirm(') !== -1 && ts.indexOf('window.confirm(') < ts.indexOf('await this.service.delete(')],
  ['clone cannot be mistaken for child navigation', html.includes('class="row-action"') && html.includes('کپی</span>') && html.includes('جزئیات وابسته')],
  ['editor controls close explicitly with type button', html.includes('(click)="closeEditor()"') && html.includes('type="button" (click)="closeEditor()"')],
  ['child pages can navigate back to parent rule', html.includes('returnToParentRule()') && ts.includes('parentFilterColumn')],
  ['table and filters update atomically upon child navigation', ts.includes('combineLatest([this.route.paramMap, this.route.queryParamMap]).pipe(auditTime(0))')],
  ['old async descriptor response is ignored after navigation', ts.includes('generation !== this.navigationGeneration') && ts.includes('this.descriptor.set(null)')],
  ['old async row results cannot overwrite new child table', ts.includes('generation !== this.navigationGeneration || table !== this.tableName()')],
  ['row primary key still powers row actions', html.includes('track row[descriptor()?.primaryKeyColumn') && ts.includes('row[descriptor.primaryKeyColumn]')],
  ['primary key no longer appears as a grid column', ts.includes('!c.primaryKey') && !ts.includes('return [descriptor.columns.find(c => c.primaryKey)!')],
  ['version technical id no longer appears as a grid column', ts.includes("c.name !== 'PRODUCT_VERSION_ID'")],
  ['technical foreign keys excluded from user grid', ts.includes('this.isTechnicalGridId(c)')],
  ['untranslated codes suppressed in bank user grid', ts.includes('this.isUntranslatedGridCode(c)')],
  ['product list no longer has surrogate numeric id cell', !list.includes('class="id-cell"') && list.includes("row['PRODUCT_CODE']")],
  ['bank user selected version is expressed by version number', ts.includes("version['VERSION_NO']") && ts.includes('contextFilterValueLabel()') && html.includes('{{ contextFilterValueLabel() }}')],
  ['non-version parent context is not shown as numeric surrogate', ts.includes("return 'قاعده مادر انتخاب‌شده'")],
  ['dormancy version label uses selected product', ts.includes("if (column.name === 'PRODUCT_VERSION_ID') return 'نسخه انتخاب‌شده محصول';")],
  ['grid number rendering includes comma separators', ts.includes("if (this.isMoneyColumn(column))") && ts.includes("toLocaleString('en-US'")],
  ['money detection includes principal and limit monetary amounts', ts.includes('PRINCIPAL|LIMIT|CEILING|FLOOR|THRESHOLD')],
  ['monetary detection excludes record identifiers and counts', ts.includes("column.name.endsWith('_ID')") && ts.includes('COUNT|RATE|PERCENT|RATIO')],
  ['money input continues to keep numeric API payload', ts.includes('onMoneyInput(column') && ts.includes('setValue(value)')],
  ['approved Persian labels govern existing forms', ts.includes('PRODUCT_REFERENCE_FIELD_LABELS') && labels.includes('DEPOSIT_PRODUCT_CLOSURE_RULE.DESCRIPTION')],
  ['label catalog includes dormancy and closing labels', labels.includes('DEPOSIT_PRODUCT_DORMANCY_RULE.INACTIVITY_PERIOD_UNIT_CODE') && labels.includes('DEPOSIT_PRODUCT_CLOSURE_PRECHECK.')],
  ['all model financial bounds are nullable, default null', finance.every(col => {const x=opening.columns.find(c => c.name===col);return x?.nullable && x.default === null;})],
  ['frontend explicitly explains null is not zero', html.includes('openingFinancialNote()') && ts.includes('NULL، نه صفر')],
  ['backend permits missing financial bounds', validator.includes('BigDecimal minOpening = decimal(values, "MIN_OPENING_AMOUNT")') && validator.includes('minOpening != null')],
  ['backend rejects negative and reversed financial bounds', validator.includes('minOpening.signum() < 0') && validator.includes('maxOpening.compareTo(minOpening) < 0') && validator.includes('maxBalance.compareTo(minBalance) < 0')],
  ['frontend does not fabricate default financial zeroes', !ts.includes("this.tableName() === 'DEPOSIT_PRODUCT_OPENING_RULE' && column.name === 'MIN_OPENING_AMOUNT'")],
  ['repository persists explicit NULL with no database default', repo.includes('if (isBlank(value) && hasDatabaseDefault(column.defaultValue())) continue;') && repo.includes('params.put(column.name(), databaseValue(column, value))')],
  ['joint fallback comes from reviewed HTML semantic contract', options.includes('JOINT_FORM_CONTRACT') && options.includes('mapJointOptions(name, column.options(), governed)')],
  ['Oracle CHECK restricts joint fallback if available', options.includes('if (allowed.contains(semantic))') && options.includes('allowed.contains(numeric)')],
  ['numeric DPS codes translate to semantic PDL codes', options.includes('translations.getOrDefault(incoming, incoming)')],
  ['backend opening rule JUnit regression added', test.includes('openingAmountsAllowNullButNotNegativeOrReversedBounds')],
  ['backend joint semantic fallback regression added', testOptions.includes('fallsBackToApprovedSemanticCodeWithoutAnOracleCheck')],
  ['release build invokes R17 regression on Windows', win.includes('verify-pb-r17.mjs')],
  ['release build invokes R17 regression on Unix', unix.includes('verify-pb-r17.mjs')],
  ['business closing explanation is Persian', html.includes('مراحل تکمیلی قاعده خاتمه') && ts.includes('پیش‌شرط‌های بستن')],
  ['grid child/CRUD actions are separately styled', css.includes('.db-grid .child-cell') && css.includes('.row-action')]
];
let failed=0;
for(const [name, good] of checks){console.log(`${good?'PASS':'FAIL'} | ${name}`);if(!good)failed++;}
console.log(`PB_R17_STATIC_PASS=${checks.length-failed} PB_R17_STATIC_FAIL=${failed}`);
if(failed) process.exitCode=1;
