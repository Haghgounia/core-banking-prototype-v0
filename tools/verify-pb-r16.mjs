import {readFileSync} from 'node:fs';
import {dirname, resolve} from 'node:path';
import {fileURLToPath} from 'node:url';
const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const get = path => readFileSync(resolve(root, path), 'utf8');
const dir = 'frontend/src/app/features/product-builder/';
const list = get(dir+'product-list.component.html');
const listTs = get(dir+'product-list.component.ts');
const workspace = get(dir+'product-workspace.component.html');
const workspaceTs = get(dir+'product-workspace.component.ts');
const table = get(dir+'pdl-table.component.html');
const tableTs = get(dir+'pdl-table.component.ts');
const labels = get(dir+'product-business-labels.ts');
const routes = get('frontend/src/app/app.routes.ts');
const build = get('build-production.cmd');
const unixBuild = get('build-production.sh');
const checks = [
 ['product grid family values use Persian label adapter', list.includes("familyLabel(row['PRODUCT_FAMILY_CODE'])") && listTs.includes('familyLabel(value: unknown)')],
 ['product grid status values use Persian label adapter', list.includes("statusLabel(row['PRODUCT_STATUS_CODE'])") && listTs.includes('statusLabel(value: unknown)')],
 ['product grid currency values use Persian label adapter', list.includes("currencyLabel(row['DEFAULT_CURRENCY_CODE'])") && listTs.includes('currencyLabel(value: unknown)')],
 ['product technical identifier remains visible', list.includes("row['PRODUCT_CODE']")],
 ['family mapping preserves source codes while displaying names', labels.includes('QARD_SAVINGS:') && labels.includes('قرض‌الحسنه پس‌انداز') && labels.includes('CURRENT_ACCOUNT:')],
 ['retired and draft are distinct localized product states', labels.includes("RETIRED: 'بازنشسته'") && labels.includes("DRAFT: 'پیش‌نویس'")],
 ['unknown business codes are not echoed as Latin', labels.includes("'عنوان فارسی مرجع موجود نیست'")],
 ['product class Reactive Form changes invalidate deposit/loan capability signals', workspaceTs.includes('this.productClassSignal.set(value)') && workspaceTs.includes('this.productClassSignal()')],
 ['product family Reactive Form changes invalidate specialized rules', workspaceTs.includes('this.productFamilySignal.set(value)') && workspaceTs.includes('this.productFamilySignal()')],
 ['Step 6 explicitly displays Product status independently', workspace.includes('وضعیت محصول</dt>') && workspace.includes('productStatusLabel(')],
 ['Step 6 explicitly displays Version lifecycle status independently', workspace.includes('وضعیت مستقل نسخه') && workspace.includes('versionStatusLabel(')],
 ['Product status save does not mutate Version', workspaceTs.includes("this.service.update('PRODUCT', id, values)") && !workspaceTs.includes("this.service.update('PRODUCT_VERSION', id, values)")],
 ['Step 6 explains status separation and offers actionable navigation', workspace.includes('وضعیت محصول با وضعیت نسخه یکسان نیست') && workspace.includes('(click)="goToStep(2)"')],
 ['retired products are not labelled ready to release', workspaceTs.includes('!this.productRetired()')],
 ['saving Product status shows context feedback', workspaceTs.includes('this.productNotice.set(') && workspace.includes('productNotice()')],
 ['version selection survives product reload', workspaceTs.includes('previousSelection = this.selectedVersionId()') && workspaceTs.includes('PRODUCT_VERSION_ID')],
 ['version validity is presented in Persian calendar', workspaceTs.includes("fa-IR-u-ca-persian") && workspace.includes('persianValidityDate(')],
 ['pricing children have visible labelled navigation', table.includes('class="child-action"') && table.includes('{{ child.label }}') && tableTs.includes('PRODUCT_PRICING_COMPONENT')],
 ['closing prechecks, settlement and approvals retain distinct child tables', ['DEPOSIT_PRODUCT_CLOSURE_PRECHECK','DEPOSIT_PRODUCT_CLOSURE_SETTLEMENT_RULE','DEPOSIT_PRODUCT_CLOSURE_APPROVAL_RULE'].every(x=>tableTs.includes(x))],
 ['child action navigates programmatically and handles failure', table.includes('openChild(row, child)') && tableTs.includes('await this.router.navigate') && tableTs.includes('اگر نه')===false && tableTs.includes('باز کردن جزئیات انجام نشد')],
 ['actual child route exists in Angular router', routes.includes('product-builder/tables/:table')],
 ['child page can return to parent rule', table.includes('returnToParentRule()') && tableTs.includes('parentFilterColumn')],
 ['pricing/closing child purpose explained in business language', tableTs.includes('childSectionHelp()') && tableTs.includes('پیش‌شرط‌های بستن') && tableTs.includes('تشکیل‌دهنده این قاعده')],
 ['deposit eligibility does not expose loan-only child', tableTs.includes('if (this.isEligibilityTable() && !this.loanProductContext()) return []') && tableTs.includes("product['PRODUCT_CLASS_CODE'] === 'LOAN'")],
 ['eligibility child domain lookup fails closed on missing reference', tableTs.includes('private async resolveEligibilityProductDomain()') && tableTs.includes('this.loanProductContext.set(false)')],
 ['common eligibility itself remains accessible', tableTs.includes("return this.tableName() === 'PRODUCT_ELIGIBILITY_RULE'")],
 ['PDL grid controlled options and unknown labels remain localized', tableTs.includes('if (column.referenceControlled) return') && tableTs.includes('مقدار قدیمی؛ عنوان فارسی مرجع')],
 ['PDL grid boolean fields are localized', tableTs.includes("return this.truthy(value) ? 'بله' : 'خیر'")],
 ['PDL context filter no longer exposes raw Oracle column code', table.includes('contextFilterLabel()') && !table.includes('{{ filterColumn() }} =')],
 ['all previous R15 functionality remains checked in build', build.includes('verify-pb-r15.mjs') && unixBuild.includes('verify-pb-r15.mjs')],
 ['R16 verifier runs in Windows and Unix production builds', build.includes('verify-pb-r16.mjs') && unixBuild.includes('verify-pb-r16.mjs')],
];
let failures=0;
for (const [name, ok] of checks) {
  console.log(`${ok ? 'PASS' : 'FAIL'} | ${name}`);
  if (!ok) failures++;
}
console.log(`PB_R16_STATIC_PASS=${checks.length-failures} FAIL=${failures}`);
if (failures) process.exitCode=1;
