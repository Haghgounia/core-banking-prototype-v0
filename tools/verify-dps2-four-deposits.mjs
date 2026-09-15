import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const read = relative => fs.readFileSync(path.join(root, relative), 'utf8');
const exists = relative => fs.existsSync(path.join(root, relative));

const refProvider = read('backend/src/main/java/com/behsazan/corebanking/deposit/opening/reference/DepositOpeningReferenceDescriptorProvider.java');
const opProvider = read('backend/src/main/java/com/behsazan/corebanking/deposit/opening/operational/DepositOpeningOperationalDescriptorProvider.java');
const repository = read('backend/src/main/java/com/behsazan/corebanking/referencedata/management/oracle/OracleReferenceRepository.java');
const routes = read('frontend/src/app/app.routes.ts');
const shell = read('frontend/src/app/layout/app-shell.component.html');
const menuTs = read('frontend/src/app/features/reference-menu/reference-menu.component.ts');
const menuHtml = read('frontend/src/app/features/reference-menu/reference-menu.component.html');
const home = read('frontend/src/app/features/four-deposits/four-deposits-home.component.html');
const wizardTs = read('frontend/src/app/features/four-deposits/deposit-opening-wizard.component.ts');
const wizardHtml = read('frontend/src/app/features/four-deposits/deposit-opening-wizard.component.html');
const opsTs = read('frontend/src/app/features/four-deposits/deposit-opening-operations.component.ts');
const pageHtml = read('frontend/src/app/features/reference-data/presentation/reference-page.component.html');

const refCount = (refProvider.match(/^\s{12}descriptor\(/gm) ?? []).length;
const opCount = (opProvider.match(/^\s{12}descriptor\(/gm) ?? []).length;
const refResources = [...refProvider.matchAll(/"(dps2-[a-z0-9-]+)", CATEGORY/g)].map(m => m[1]);
const opResources = [...opProvider.matchAll(/"(dps2-opening-[a-z0-9-]+)", CATEGORY/g)].map(m => m[1]);
const excludedAuditTables = [
  'DEPOSIT_OPENING_AUDIT_EVENT',
  'DEPOSIT_OPENING_AUDIT_FIELD_CHANGE',
  'DEPOSIT_OPENING_CHANGE_SET',
  'DEPOSIT_OPENING_SNAPSHOT',
  'DEPOSIT_OPENING_STATUS_HISTORY'
];
const familyKeys = ['QARD_SAVINGS','CURRENT_ACCOUNT','SHORT_TERM_DEPOSIT','LONG_TERM_DEPOSIT'];
const configFiles = [
  'config/application.yml',
  'backend/src/main/resources/application.yml'
];

const checks = [
  [refCount === 59 && refResources.length === 59 && new Set(refResources).size === 59, `DPS2 reference descriptors must be 59 unique forms, got ${refCount}/${refResources.length}`],
  [opCount === 25 && opResources.length === 25 && new Set(opResources).size === 25, `DPS2 operational descriptors must be 25 unique forms, got ${opCount}/${opResources.length}`],
  [refProvider.includes('CATEGORY = "DEPOSIT_OPENING_REFERENCE"') && refProvider.includes('schemas.deposit-opening:DPS2'), 'DPS2 reference category/schema contract missing'],
  [opProvider.includes('CATEGORY = "DEPOSIT_OPENING_OPERATIONAL"') && opProvider.includes('schemas.deposit-opening:DPS2'), 'DPS2 operational category/schema contract missing'],
  [excludedAuditTables.every(t => !opProvider.includes(`schemaName, "${t}"`)), 'append-only/change-controlled tables must not be generic operational CRUD descriptors'],
  [configFiles.every(f => read(f).includes('deposit-opening: DPS2')), 'deposit-opening DPS2 schema property must exist in canonical application configs'],
  [routes.includes("path: 'four-deposits'") && routes.includes("path: 'four-deposits/opening'") && routes.includes("path: 'four-deposits/operations/:resource'") && routes.includes("path: 'four-deposits/reference-data/:resource'"), 'four-deposits routes are incomplete'],
  [shell.includes('>چهار سپرده</span>') && shell.includes('routerLink="/four-deposits/opening"') && shell.includes('routerLink="/four-deposits/operations"') && shell.includes('routerLink="/four-deposits/reference-data"'), 'sidebar Four Deposits menu is incomplete'],
  [menuTs.includes("'FOUR_DEPOSIT'") && menuTs.includes("DEPOSIT_OPENING_REFERENCE") && menuHtml.includes('اطلاعات پایه افتتاح چهار سپرده'), 'DPS2 reference menu scope is missing'],
  [familyKeys.every(k => wizardTs.includes(`${k}:`)), 'wizard must contain exactly the four supported deposit family contracts'],
  [(wizardTs.match(/readonly steps=\[/g) ?? []).length === 1 && (wizardTs.includes("'Payload و ثبت'") || wizardTs.includes("'ثبت و فعال‌سازی حساب'")) && wizardHtml.includes('step()===7'), 'seven-step opening wizard contract is incomplete'],
  [wizardTs.includes('DEPOSIT_OPENING_REQUEST') && wizardTs.includes('DEPOSIT_OPENING_PARTY') && wizardTs.includes('DEPOSIT_OPENING_FUNDING') && wizardTs.includes('DEPOSIT_OPENING_CHECK') && wizardTs.includes('DEPOSIT_OPENING_DECISION'), 'wizard payload must cover core DPS2 opening entities'],
  [home.includes('قرض‌الحسنه پس‌انداز') && home.includes('حساب جاری') && home.includes('سپرده کوتاه‌مدت') && home.includes('سپرده بلندمدت') && home.includes('گواهی سپرده'), 'Four Deposits landing scope text is incomplete'],
  [opsTs.includes('dps2-opening-request') && opsTs.includes('dps2-opening-decision') && opsTs.includes('dps2-opening-batch'), 'operational forms grouping is incomplete'],
  [pageHtml.includes("@case ('TIMESTAMP')") && pageHtml.includes('datetime-local'), 'generic reference UI must support TIMESTAMP editing'],
  [repository.includes('optionalField("updatedBy")') && repository.includes('optionalField("updatedAt")'), 'generic repository must maintain UPDATED_BY / UPDATED_AT when descriptors expose them'],
  [exists('docs/DPS2-0.3.97-FOUR-DEPOSITS-PHASE1-QA.md'), 'Phase 1 QA documentation is missing']
];

const failed = checks.filter(([ok]) => !ok).map(([, message]) => message);
if (failed.length) {
  console.error('DPS2 Four Deposits verification FAILED:');
  for (const message of failed) console.error(`- ${message}`);
  process.exit(1);
}
console.log(`DPS2 Four Deposits verification OK: ${refCount} reference forms, ${opCount} operational forms, 4 families, 7-step wizard.`);
