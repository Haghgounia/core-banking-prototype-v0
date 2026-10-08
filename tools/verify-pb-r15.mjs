import {readFileSync} from 'node:fs';
import {dirname, resolve} from 'node:path';
import {fileURLToPath} from 'node:url';
const root=resolve(dirname(fileURLToPath(import.meta.url)),'..');
const load=p=>readFileSync(resolve(root,p),'utf8');
const ui=load('frontend/src/app/features/product-builder/pdl-table.component.ts');
const html=load('frontend/src/app/features/product-builder/pdl-table.component.html');
const ref=load('backend/src/main/java/com/behsazan/corebanking/productbuilder/application/PdlReferenceOptionService.java');
const service=load('backend/src/main/java/com/behsazan/corebanking/productbuilder/application/ProductBuilderService.java');
const validation=load('backend/src/main/java/com/behsazan/corebanking/productbuilder/application/ProductBuilderBusinessValidator.java');
const test=load('backend/src/test/java/com/behsazan/corebanking/productbuilder/application/PdlPricingRelationshipR15Test.java');
const spec=load('docs/reference/Unified_Product_Builder_Interactive_Forms_FA.html');
const m=spec.match(/const MODEL=(\[.*?\]);/s);
if(!m) throw new Error('Reference HTML model unavailable');
const model=JSON.parse(m[1]);
const fields=(table,cols)=>cols.every(name=>model.find(t=>t.name===table)?.columns.some(c=>c.name===name));
const cases=[
 ['pricing fields exist in approved HTML',fields('PRODUCT_PRICING_RULE',['PRICING_PURPOSE_CODE','PRICING_METHOD_CODE','SETTLEMENT_FREQUENCY_CODE','DESTINATION_RULE_CODE','EARLY_TERMINATION_RATE'])],
 ['all four pricing purposes are bank-user selectable', ['DEPOSIT_PROFIT','LOAN_INTEREST','COMMISSION','PENALTY'].every(x=>ref.includes(`"${x}"`)) && ref.includes('PRODUCT_PRICING_RULE.PRICING_PURPOSE_CODE')],
 ['fixed/floating/tiered/formula pricing methods are controlled',ref.includes('PRODUCT_PRICING_RULE.PRICING_METHOD_CODE') && ['FIXED','FLOATING','TIERED','FORMULA'].every(x=>ref.includes(`"${x}"`))],
 ['accrual and settlement have separate selectors',ref.includes('PRODUCT_PRICING_RULE.ACCRUAL_FREQUENCY_CODE') && ref.includes('PRODUCT_PRICING_RULE.SETTLEMENT_FREQUENCY_CODE')],
 ['payment destination is a controlled select',ref.includes('PRODUCT_PRICING_RULE.DESTINATION_RULE_CODE') && html.includes('isSelect(column)')],
 ['all form choices are constrained by live Oracle CHECK when present',ref.includes('businessFormOptions(String table, String field, List<SelectOption> databaseChoices)') && ref.includes('allowed.contains(normalize(option.code()))')],
 ['controlled form field writes reject mismatched values',ref.includes('return copy(column, true, "PDL_FORM_CONTRACT", choices);') && ref.includes('validateChangedValues')],
 ['early rate conditional enablement exists',ui.includes('private syncPricingFields()') && ui.includes("this.form.controls['EARLY_TERMINATION_ALLOWED']?.valueChanges.subscribe") && ui.includes('rate.setValue(null')],
 ['early rate is explained as fraction of 1',ui.includes('۰٫۱۸') && ui.includes('۱۸ درصد') && ui.includes('نرخ تسویه در خاتمه پیش از موعد')],
 ['server validates pricing rates and payment destinations',validation.includes('private void validatePricingRule') && validation.includes('early.compareTo(BigDecimal.ONE)') && validation.includes('MAX_DESTINATION_COUNT')],
 ['relationship type reflects reviewed product-specific choices',ref.includes('PRODUCT_RELATIONSHIP.RELATIONSHIP_TYPE_CODE') && ['REQUIRES','BUNDLE','RATE_DEPENDENCY','COLLATERAL_ACCOUNT'].every(x=>ref.includes(`"${x}"`))],
 ['product destination has dedicated business selector',html.includes('isRelationshipTargetProduct(column)') && ui.includes('relationshipProducts.set(')],
 ['target versions are filtered by target product',html.includes('isRelationshipTargetVersion(column)') && ui.includes('return this.relationshipVersions().filter(v => v.productId === target)')],
 ['version selection resets on target product change',ui.includes("this.form.controls['TARGET_PRODUCT_VERSION_ID']?.setValue(null)")],
 ['current product cannot be its own target, including on server',service.includes('"محصول مقصد نباید همان محصول مبدأ باشد."')],
 ['target version ownership checked on server',service.includes('number(target.get("PRODUCT_ID")) != targetProduct')],
 ['relationship priority and amount bounds validated',validation.includes('private void validateRelationship') && validation.includes('priority < 1') && validation.includes('max.compareTo(min) < 0')],
 ['mandatory relationship is an on/off control',html.includes('isPricingOrRelationshipSwitch(column)') && ui.includes("column.name === 'IS_MANDATORY'")],
 ['profile and opening create lookup existing version before INSERT',ui.includes('existing.totalElements > 0') && ui.includes('await this.edit(existing.items[0])')],
 ['profile and opening clone require another version',ui.includes('versionCloneCandidates.set(candidates)') && ui.includes('this.cloneDestinationVersion.value') && html.includes('نسخه مقصد برای کپی رکورد')],
 ['profile and opening backend give explicit duplicate guidance',service.includes('validateVersionScopedCreate(table, prepared)') && service.includes('قاعده افتتاح برای این نسخه محصول قبلاً ثبت شده است')],
 ['version-scoped clones set destination PRODUCT_VERSION_ID',ui.includes("this.form.controls['PRODUCT_VERSION_ID']?.setValue(id)")],
 ['original rows remain unchanged by Clone',ui.includes('this.editingId.set(null)') && ui.includes('this.cloning.set(true)')],
 ['delete continues to require confirmation',ui.includes('window.confirm(')],
 ['JUnit covers pricing/relationship contract',test.includes('reviewedPricingSelectorsMatchBankingContract') && test.includes('earlyTerminationRateIsConditionalAndBounded') && test.includes('relationshipPriorityAndAmountBoundsAreValidated')],
 ['Product Builder unchanged catalog contract',load('backend/src/main/java/com/behsazan/corebanking/productbuilder/application/PdlCatalog.java').includes('register("PRODUCT_PRICING_RULE"')],
];
let failed=0;
cases.forEach(([name,passed])=>{console.log(`${passed?'PASS':'FAIL'} | ${name}`);if(!passed)failed++;});
console.log(`PB_R15_STATIC_PASS=${cases.length-failed} FAIL=${failed}`);
if(failed)process.exitCode=1;
