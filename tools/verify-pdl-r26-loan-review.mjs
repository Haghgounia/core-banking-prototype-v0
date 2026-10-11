/** R26: UX and authoritative-domain regression for Loan Product Builder.
 *  Static/source-based checks only; Oracle UAT and full Angular/Maven build remain separate. */
import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const read = p => fs.readFileSync(path.join(root,p), 'utf8');
const workspaceHtml = read('frontend/src/app/features/product-builder/product-workspace.component.html');
const workspaceCss = read('frontend/src/app/features/product-builder/product-workspace.component.scss');
const formHtml = read('frontend/src/app/features/product-builder/pdl-table.component.html');
const formTs = read('frontend/src/app/features/product-builder/pdl-table.component.ts');
const formCss = read('frontend/src/app/features/product-builder/pdl-table.component.scss');
const validator = read('backend/src/main/java/com/behsazan/corebanking/productbuilder/application/ProductBuilderBusinessValidator.java');
const service = read('backend/src/main/java/com/behsazan/corebanking/productbuilder/application/ProductBuilderService.java');
const repo = read('backend/src/main/java/com/behsazan/corebanking/productbuilder/oracle/PdlProductBuilderRepository.java');
const options = read('backend/src/main/java/com/behsazan/corebanking/productbuilder/application/PdlReferenceOptionService.java');
const policy = read('frontend/src/app/features/product-builder/rule-policy-lite.component.html');
let passed=0, failed=0;
function check(label,ok){console.log(`${ok?'PASS':'FAIL'} | ${label}`);if(ok)passed++;else failed++;}

check('01 identity balance label rendered in Persian',workspaceHtml.includes('دارایی بانک (تسهیلات)') && workspaceHtml.includes('بدهی بانک (سپرده)'));
check('01 identity balance persistence retains ASSET/LIABILITY code',workspaceHtml.includes('type="hidden" formControlName="BALANCE_NATURE_CODE"'));
check('01 identity fields do not vertically stretch with loan-family help',workspaceCss.includes('.form-grid{align-items:start}') && workspaceCss.includes('.form-grid>mat-form-field{align-self:start'));
check('02 three approval flags hidden in generic editor, no three simultaneous switches',formTs.includes("tableName() === 'LOAN_PRODUCT_PROCESS_RULE' && LOAN_APPROVAL_FLAGS.some") && formHtml.includes('loanApprovalMode'));
check('02 single selection clearly states no automatic disbursement',formHtml.includes('به معنی اعطای خودکار تسهیلات نیست'));
check('02 each UI approval choice maps exclusively to one persisted flag',formTs.includes('this.form.controls[flag]?.setValue(mode === flag ? 1 : 0'));
check('02 historic multi-flag records require explicit correction',formTs.includes("this.loanApprovalMode.setValue(selected.length > 1 ? 'INVALID'"));
check('02 backend rejects multiple concurrent approval methods',validator.includes('selectedApprovalModes > 1') && validator.includes('COMMITTEE_APPROVAL_REQUIRED'));
check('03 operation options use PDL business vocabulary not surrogate DPS2 lookup',options.includes('CHANNEL_OPERATION_CHOICES') && options.includes('"PRODUCT_CHANNEL_OPERATION".equals(normalize(table))'));
check('03 loan channel excludes cash withdrawal and transfer',formTs.includes('LOAN_CHANNEL_OPERATION_CODES = new Set') && !formTs.match(/LOAN_CHANNEL_OPERATION_CODES = new Set\([^\n]*(CASH_WITHDRAWAL|TRANSFER_OUT)/));
check('03 operation types filtered after product classification from real product version',formTs.includes("this.service.row('PRODUCT_VERSION'") && formTs.includes("this.service.row('PRODUCT'") && formTs.includes('base.filter(opt => allowed.has'));
check('03 backend domain control handles new and changed-parent operations',service.includes('validateChannelOperationDomain') && service.includes('number(old.get("CHANNEL_RULE_ID")) == number(values.get("CHANNEL_RULE_ID"))'));
check('03 prior domain data remains visible without implicit migration',formTs.includes("مقدار قدیمی؛ عنوان فارسی مرجع") && service.includes('Historical code in its original channel rule'));
check('04 reference dropdown supports legacy PDL lacking physical FK',repo.includes('boolean loanCollateral = "LOAN_PRODUCT_COLLATERAL_RULE"') && formTs.includes('isLoanCollateralReference'));
check('04 collateral references are retrieved from actual active PDL master',repo.includes('descriptor(loanCollateral ? "LOAN_PRODUCT_COLLATERAL"') && repo.includes('T.RECORD_STATUS_CODE = \'ACTIVE\''));
check('04 collateral label uses authoritative PRODUCT_COLLATERAL_NAME, not DESCRIPTION',repo.includes('column(parent, "PRODUCT_COLLATERAL_NAME")') && repo.includes('column(parent, "PRODUCT_COLLATERAL_CODE")'));
check('04 no mocked collateral reference options in Java',repo.includes('new SelectOption(rs.getObject("V"), rs.getString("C"), rs.getString("L"))'));
check('05 coverage percent UI converts to Oracle ratio and back',formTs.includes('isCollateralCoverage(column)') && formTs.includes('Number((percent / 100).toFixed(4))') && formTs.includes('Number((ratio * 100).toFixed(2))'));
check('05 Min and Max hints shown under inputs',formTs.includes('Min — کمترین ارزش پوشش') && formTs.includes('Max — بیشترین ارزش پوشش') && formHtml.includes('businessFieldHint(column)'));
check('05 backend still validates coverage max >= min',validator.includes('durationRange(values, "MIN_COVERAGE_PERCENT", "MAX_COVERAGE_PERCENT"'));
check('06 priority options assigned to each product version and auto next rank',formTs.includes('const count = siblings.length + 1;') && formTs.includes('collateralPriorityChoices.set(Array.from({length: count}') && formTs.includes(': count;'));
check('06 priority serialized by locking parent version and transaction',repo.includes('FOR UPDATE') && service.includes('repository.lockCollateralProductVersion(versionId)') && service.includes('@Transactional'));
check('06 create/update/delete reindex collateral rule, preventing duplicates',service.includes('reorderLoanCollateral(prepared, null') && service.includes('reorderLoanCollateral(merged, id') && service.includes('normalizeLoanCollateralPriorities(requiredLoanId(current'));
check('06 loan collateral grid sorted by ascending priority',repo.includes('T.PRIORITY_NO ASC'));
check('07 required collateral yes mandatory no optional, not forbidden',formHtml.includes('به معنی اختیاری بودن این نوع وثیقه است') && formTs.includes("'الزامی' : 'اختیاری'"));
check('08 Lite scope clarifies policy governance not runtime approval',policy.includes('موتور تصمیم اعتباری یا گردش کار اعطای تسهیلات نیست'));
check('09 R26 styles use shared theme tokens',formCss.includes('.loan-approval-picker') && formCss.includes('var(--app-surface-muted)'));
check('10 original batch-opening scope preserved',fs.existsSync(path.join(root,'tools/verify-dps2-batch-cif-pdl-runtime.mjs')));
console.log(`PDL_R26_LOAN_REVIEW_STATIC_PASS=${passed} FAIL=${failed}`);
if(failed)process.exit(1);
