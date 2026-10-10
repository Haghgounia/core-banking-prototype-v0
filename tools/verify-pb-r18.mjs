import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const read=p=>fs.readFileSync(path.join(root,p),'utf8');
const ts=read('frontend/src/app/features/product-builder/pdl-table.component.ts');
const html=read('frontend/src/app/features/product-builder/pdl-table.component.html');
const ws=read('frontend/src/app/features/product-builder/product-workspace.component.ts');
const wh=read('frontend/src/app/features/product-builder/product-workspace.component.html');
const java=read('backend/src/main/java/com/behsazan/corebanking/productbuilder/application/ProductBuilderService.java');
const refs=read('backend/src/main/java/com/behsazan/corebanking/productbuilder/application/PdlReferenceOptionService.java');
const tx=read('backend/src/main/java/com/behsazan/corebanking/deposit/account/transaction/application/DepositTransactionService.java');
const junit=read('backend/src/test/java/com/behsazan/corebanking/productbuilder/application/PdlOrgScopeR18Test.java');
const cmd=read('build-production.cmd'); const sh=read('build-production.sh');
const tests=[
 ['organization code is readonly in editor',html.includes('isSystemOrgUnitCode(column)') && html.includes('از واحد سازمانی انتخاب‌شده توسط سامانه')],
 ['org code control is disabled rather than an editable textbox',ts.includes('this.isContextLocked(c) || this.isSystemOrgUnitCode(c)')],
 ['org code never sent from form as trusted payload',ts.includes("delete values['ORG_UNIT_CODE']")],
 ['org unit is selected through governed DPS reference ID',refs.includes('"PRODUCT_ORG_SCOPE".equals(normalize(table)) && "ORG_UNIT_ID".equals(name)') && refs.includes('new SelectOption(o.value(), o.code(), o.label())')],
 ['missing reference cannot be treated as arbitrary free text',refs.includes('"DPS:dps-org-units", List.of()')],
 ['backend derives organization code on INSERT',java.includes('if ("PRODUCT_ORG_SCOPE".equals(normalizeTable(table))) {\n            resolveOrgUnitCode(prepared);'.replaceAll('\\n','\n'))],
 ['backend derives organization code on UPDATE',java.includes('resolveOrgUnitCode(scope);')],
 ['backend ignores client-supplied unit code',java.includes('values.put("ORG_UNIT_CODE", orgUnitId > 0 ? referenceOptionService.governedOrgUnitCode(orgUnitId) : null)')],
 ['backend checks org reference active/current',refs.includes('isCurrent') && refs.includes('isActive') && refs.includes('findById("dps-org-units", orgUnitId)')],
 ['org lookup and inactive rejection covered by JUnit',junit.includes('derivesCanonicalCodeFromTheSelectedReferenceRecord') && junit.includes('rejectsInactiveOrUnknownOrgUnit')],
 ['module-table mapping includes PRICING',ws.includes("PRODUCT_PRICING_RULE: 'PRICING'")],
 ['module-table mapping includes TRANSACTION and withdrawal instruments',ws.includes("DEPOSIT_PRODUCT_WITHDRAWAL_MEDIA: 'TRANSACTION'")],
 ['disabled capabilities cannot navigate from Common Rules',wh.includes('@if (ruleEnabled(rule.table))') && wh.includes('قابلیت غیرفعال است؛ تنظیم این بخش الزامی نیست')],
 ['disabled capabilities cannot navigate from Deposit Rules',wh.includes('این قابلیت غیرفعال است و نیازی به تکمیل این فرم نیست')],
 ['review count excludes disabled modules',ws.includes('this.enabledRules().filter(rule => this.count(rule.table) > 0)')],
 ['review shows inactive label instead of asking configuration',wh.includes("ruleEnabled(rule.table) ? count(rule.table) : 'غیرفعال'")],
 ['module disable preserves saved rules',!ws.includes("delete('PRODUCT_PRICING_RULE'")],
 ['create opens form before remote profile duplicate check',ts.indexOf('this.editorOpen.set(true);\n    try {'.replaceAll('\\n','\n')) < ts.indexOf("this.service.rows(table, {\n          page: 0, size: 1".replaceAll('\\n','\n'))],
 ['create can report remote startup error while editor remains visible',ts.includes('آماده‌سازی فرم کامل نشد:')],
 ['create button shows pending state',html.includes('editorOpening() ?')],
 ['profile uniqueness remains protected',java.includes('validateVersionScopedCreate(table, prepared)')],
 ['profile immutable identity remains protected',java.includes('گروه و نوع سپرده پس از ثبت قابل تغییر نیستند')],
 ['transaction rules warning explains non-automatic runtime gating',html.includes('نبود یک قاعده در نسخه فعلی به‌تنهایی برداشت نقدی را مسدود نمی‌کند')],
 ['operational transaction validations are still status/holds/balance/restrictions',tx.includes('ACCOUNT_STATUS') && tx.includes('ACTIVE_HOLD') && tx.includes('TRANSACTION_RESTRICTION')],
 ['R18 wired into Windows production build',cmd.includes(String.raw`tools\verify-pb-r18.mjs`)],
 ['R18 wired into Unix production build',sh.includes('tools/verify-pb-r18.mjs')]
];
let fails=0;
for(const [name,ok] of tests){console.log(`${ok?'PASS':'FAIL'} | ${name}`);if(!ok)fails++;}
console.log(`PB_R18_STATIC_PASS=${tests.length-fails} PB_R18_STATIC_FAIL=${fails}`);
if(fails)process.exitCode=1;
