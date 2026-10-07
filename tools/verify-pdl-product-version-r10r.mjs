import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const read = rel => fs.readFileSync(path.join(root, rel), 'utf8');
let pass = 0;
let fail = 0;
const check = (condition, message) => {
  if (condition) { console.log(`PASS | ${message}`); pass++; }
  else { console.log(`FAIL | ${message}`); fail++; }
};

const html = read('frontend/src/app/features/product-builder/product-workspace.component.html');
const ts = read('frontend/src/app/features/product-builder/product-workspace.component.ts');
const frontendService = read('frontend/src/app/features/product-builder/product-builder.service.ts');
const frontendModels = read('frontend/src/app/features/product-builder/product-builder.models.ts');
const controller = read('backend/src/main/java/com/behsazan/corebanking/productbuilder/web/ProductBuilderController.java');
const service = read('backend/src/main/java/com/behsazan/corebanking/productbuilder/application/ProductBuilderService.java');
const repository = read('backend/src/main/java/com/behsazan/corebanking/productbuilder/oracle/PdlProductBuilderRepository.java');
const models = read('backend/src/main/java/com/behsazan/corebanking/productbuilder/domain/ProductBuilderModels.java');
const build = read('build-production.cmd');

check(html.includes('class="field-caption"><strong>نسخه مبدأ</strong>'), 'source version has explicit label/caption');
check(html.includes('نسخه‌ای از همین محصول که نسخه جدید از آن مشتق می‌شود'), 'source version explains business meaning');
check(html.includes('class="system-counter"') && !html.includes('formControlName="VERSION_NO"'), 'version number is display-only rather than editable form field');
check(html.includes('توسط سیستم از آخرین شماره نسخه + ۱ محاسبه می‌شود'), 'version number system assignment is explained to user');
check(ts.includes('await this.service.productVersionDefaults(productId)'), 'new-version form loads defaults from backend system source');
check(ts.includes('VALID_FROM: defaults.systemDate'), 'valid-from defaults to system date');
check(html.includes('تاریخ روز سامانه پیش‌فرض است و قابل ویرایش می‌باشد'), 'valid-from remains editable with system-date default');
check(!html.includes('مجاز (OPEN)') && !html.includes('متوقف (SUSPENDED)') && !html.includes('بسته (CLOSED)'), 'origination combo renders Persian titles only');
check(!html.includes('فعال (ACTIVE)') && !html.includes('خاتمه‌یافته (CLOSED)'), 'servicing combo renders Persian titles only');
check(html.includes('[value]="approvalTimeDisplay()" readonly'), 'approval time is read-only system display');
check(html.includes('[value]="approvalActorDisplay()" readonly'), 'approval actor is read-only system display');
check(ts.includes("if (value === 'prototype-ui') return 'کاربر پیش‌فرض محصول‌ساز';"), 'prototype fallback actor has Persian business display');
check(frontendModels.includes('export interface PdlProductVersionDefaults'), 'frontend defaults contract exists');
check(frontendService.includes('/versions/new-defaults'), 'frontend calls product-version defaults endpoint');
check(controller.includes('/products/{productId}/versions/new-defaults'), 'backend exposes product-version defaults endpoint');
check(models.includes('record ProductVersionDefaults'), 'backend defaults contract exists');
check(repository.includes('SELECT TRUNC(SYSDATE) AS SYSTEM_DATE FROM DUAL') && repository.includes('SYSTIMESTAMP'), 'system date/time are sourced from database clock');
check(repository.includes('lockAndNextProductVersionNo') && repository.includes('LOCK TABLE') && repository.includes('MAX(VERSION_NO)'), 'backend allocates next version number under database lock');
check(service.includes('prepared.put("VERSION_NO", repository.lockAndNextProductVersionNo(productId))'), 'backend overrides client version number on create');
check(service.includes('prepared.remove("VERSION_NO")'), 'backend prevents manual version-number updates');
check(service.includes('prepared.remove("APPROVED_AT")') && service.includes('prepared.remove("APPROVED_BY")'), 'client cannot directly set approval audit fields');
check(service.includes('values.put("APPROVED_AT", repository.currentDatabaseDateTime())') && service.includes('values.put("APPROVED_BY", actor)'), 'approval audit is system-populated from database time and request actor');
check(build.includes('verify-pdl-product-version-r10r.mjs'), 'R10R verifier is wired into production build');

console.log('------------------------------------------------------------');
console.log(`PDL_PRODUCT_VERSION_R10R_PASS=${pass}`);
console.log(`PDL_PRODUCT_VERSION_R10R_FAIL=${fail}`);
if (fail) process.exit(1);
console.log('PDL_PRODUCT_VERSION_R10R_STATIC_PASS');
