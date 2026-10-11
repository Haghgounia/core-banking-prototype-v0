import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
function read(p){return fs.readFileSync(path.join(root,p),'utf8');}
const ts=read('frontend/src/app/features/product-builder/pdl-table.component.ts');
const html=read('frontend/src/app/features/product-builder/pdl-table.component.html');
const css=read('frontend/src/app/features/product-builder/pdl-table.component.scss');
const matrixHtml=read('frontend/src/app/features/product-builder/product-workspace.component.html');
const matrixCss=read('frontend/src/app/features/product-builder/product-workspace.component.scss');
const ref=read('backend/src/main/java/com/behsazan/corebanking/productbuilder/application/PdlReferenceOptionService.java');
const repo=read('backend/src/main/java/com/behsazan/corebanking/productbuilder/oracle/PdlProductBuilderRepository.java');
const svc=read('backend/src/main/java/com/behsazan/corebanking/productbuilder/application/ProductBuilderService.java');
const validator=read('backend/src/main/java/com/behsazan/corebanking/productbuilder/application/ProductBuilderBusinessValidator.java');
const model=read('frontend/src/app/features/product-builder/product-builder.models.ts');
let pass=0,fail=0;
function check(name,ok){if(ok){pass++;console.log('PASS | '+name)}else{fail++;console.error('FAIL | '+name)}}
for(const table of ['LOAN_PRODUCT_PROFILE','LOAN_PRODUCT_REPAYMENT_RULE','LOAN_PRODUCT_COLLATERAL_RULE','LOAN_PRODUCT_PROCESS_RULE','LOAN_FINANCIAL_EXTENSION','LOAN_ELIGIBILITY_EXTENSION']){
 check('specialty loan covered: '+table,ts.includes(table)&&ref.includes(table)&&(validator.includes(table)||svc.includes('"'+table+'"')));
}
check('governance summary and fixed width columns',matrixHtml.includes('governance-overview')&&matrixHtml.includes('<colgroup>')&&matrixCss.includes('min-width:1460px'));
check('governance matrix keeps 6 readable columns', (matrixHtml.match(/<th scope="col">/g)||[]).length===6);
check('governance matrix retains reasons and true runtime state',matrixHtml.includes('rule.reason')&&matrixHtml.includes('governanceRuntimeFa(rule.runtimeIntegration)'));
check('presence policy types aligned with Angular template',model.includes("'REQUIRED' | 'WHEN_ENABLED' | 'OPTIONAL'"));
check('loan reference foreign keys stay actual PDL lookups',ref.includes('column.foreignKey()) return column')&&repo.includes('RECORD_STATUS_CODE = \'ACTIVE\''));
for(const t of ['LOAN_TYPE','LOAN_USAGE','PLAN_TYPE','ECONOMIC_SUB_SECTION','FACILITY_OPERATION_PARAMETER','LOAN_PRODUCT_COLLATERAL']){
 check('loan parent reference '+t,svc.includes('"'+t+'"'));
}
check('type-to-plan PDL mapping enforced server-side',svc.includes('loanTypePlanAllowed')&&repo.includes('LOAN_TYPE_PLAN_TYPE'));
check('subsection to plan PDL mapping enforced',svc.includes('loanEconomicSubSectionAllowed')&&repo.includes('ECONOMIC_SECTION'));
check('type-to-plan PDL mapping loaded in Angular',ts.includes("this.service.rows('LOAN_TYPE_PLAN_TYPE'")&&ts.includes('loadLoanPlans(true)'));
check('reference error visible on bank form',html.includes('loanLookupError(column)')&&html.includes('reference-error'));
check('absent references do not pretend to be live data',ref.includes('PDL_FORM_CONTRACT')&&html.includes('loanBusinessHelp()'));
check('repayment method from approved contract as selector',ref.includes('LOAN_PRODUCT_REPAYMENT_RULE.REPAYMENT_METHOD_CODE')&&html.includes('isSelect(column)'));
for (const f of ['CURRENCY_TYPE_CODE','INSTALLMENT_FREQUENCY_CODE','TERM_UNIT_CODE','INSTALLMENT_INTERVAL_UNIT_CODE','GRACE_UNIT_CODE','OVERRIDE_APPROVAL_LEVEL_CODE']) {
 check('reference code from DPS '+f,ref.includes('"'+f+'"'));
}
check('loan switches use controlled toggle',html.includes('isLoanSwitch(column)')&&html.includes('toggleLoanSwitch(column,$event.checked)'));
check('loan form section headings and responsive CSS',html.includes('loanSectionHeading(column)')&&css.includes('.loan-fieldset-title'));
check('grace and prepayment conditionally validated',validator.includes('validateLoanRepayment')&&validator.includes('PARTIAL_PREPAYMENT_ALLOWED'));
check('loan processes validate overrides',validator.includes('validateLoanProcess')&&validator.includes('OVERRIDE_APPROVAL_LEVEL_CODE'));
check('collateral coverage range validated',validator.includes('validateLoanCollateral')&&validator.includes('MIN_COVERAGE_PERCENT'));
check('financial contribution ratio validated',validator.includes('validateLoanFinancial')&&validator.includes('MAX_CUSTOMER_CONTRIBUTION'));
check('loan eligibility validated',validator.includes('validateLoanEligibility')&&validator.includes('REQUIRED_GUARANTOR_COUNT'));
check('loan clone target other product version',html.includes("tableName() === 'LOAN_PRODUCT_PROFILE'")&&html.includes("tableName() === 'LOAN_FINANCIAL_EXTENSION'"));
check('loan diagnostic SQL strictly read-only',!(/\b(INSERT|UPDATE|DELETE|MERGE|DROP|CREATE|ALTER)\b/i).test(read('docs/install/PDL-LOAN-REFERENCES-R21-READONLY.sql')));
console.log(`PB_R21_LOAN_STATIC_PASS=${pass} PB_R21_LOAN_STATIC_FAIL=${fail}`);
if(fail)process.exitCode=1;
