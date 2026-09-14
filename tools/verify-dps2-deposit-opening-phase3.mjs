import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const read = relative => fs.readFileSync(path.join(root, relative), 'utf8');
const exists = relative => fs.existsSync(path.join(root, relative));

const version = read('VERSION').trim();
const versionAtLeast=(actual,minimum)=>{const a=actual.split('.').map(Number),m=minimum.split('.').map(Number);for(let i=0;i<3;i++){if(a[i]>m[i])return true;if(a[i]<m[i])return false}return true};
const pom = read('backend/pom.xml');
const packageJson = JSON.parse(read('frontend/package.json'));
const models = read('backend/src/main/java/com/behsazan/corebanking/deposit/opening/domain/DepositOpeningModels.java');
const service = read('backend/src/main/java/com/behsazan/corebanking/deposit/opening/application/DepositOpeningAggregateService.java');
const repository = read('backend/src/main/java/com/behsazan/corebanking/deposit/opening/oracle/DepositOpeningAggregateRepository.java');
const wizardTs = read('frontend/src/app/features/four-deposits/deposit-opening-wizard.component.ts');
const wizardHtml = read('frontend/src/app/features/four-deposits/deposit-opening-wizard.component.html');

const phase3Tables = [
  'DEPOSIT_OPENING_SIGNATORY',
  'DEPOSIT_OPENING_SIGNATORY_AUTHORITY',
  'DEPOSIT_OPENING_AUTHORIZED_USER',
  'DEPOSIT_OPENING_DELEGATION',
  'DEPOSIT_OPENING_BENEFICIARY',
  'DEPOSIT_OPENING_DOCUMENT',
  'DEPOSIT_OPENING_PAYMENT_INSTRUMENT',
  'DEPOSIT_OPENING_PRICING_OVERRIDE_REQUEST',
  'DEPOSIT_OPENING_TAX_STATUS',
  'DEPOSIT_OPENING_REWARD_ENROLLMENT'
];
const modelTypes = [
  'record Signatory(', 'record SignatoryAuthority(', 'record AuthorizedUser(', 'record Delegation(',
  'record Beneficiary(', 'record OpeningDocument(', 'record PaymentInstrument(',
  'record PricingOverrideRequest(', 'record TaxStatus(', 'record RewardEnrollment('
];
const uiHooks = [
  'addSignatory()', 'addAuthority()', 'addAuthorizedUser()', 'addDelegation()', 'addBeneficiary()',
  'addPaymentInstrument()', 'addPricingRequest()', 'addRewardEnrollment()', 'addDocument()'
];

const checks = [
  [versionAtLeast(version,'0.3.99'), `VERSION must preserve Phase 3 or later source, got ${version}`],
  [pom.includes(`<version>${version}-SNAPSHOT</version>`), `backend Maven source version is not ${version}-SNAPSHOT`],
  [packageJson.version === version, `frontend source version must match ${version}, got ${packageJson.version}`],
  [modelTypes.every(type => models.includes(type)), 'Phase 3 typed aggregate models are incomplete'],
  [phase3Tables.every(table => models.includes(`@JsonProperty("${table}")`)), 'AggregateRequest does not expose all Phase 3 tables'],
  [phase3Tables.every(table => repository.includes(`INSERT INTO %s.${table}`)), 'repository does not cover all Phase 3 tables'],
  [repository.includes('SEQ_DEPOSIT_OPENING_SIGNATORY') && service.includes('persistedSignatoryIds'), 'signatory client-reference to Oracle-ID mapping is missing'],
  [service.includes('Authority باید به شناسه موقت') && service.includes('Party صاحب امضا باید در DEPOSIT_OPENING_PARTY'), 'signatory/authority integrity validation missing'],
  [service.includes('مرجع سند وکالت/نمایندگی الزامی است') && service.includes('درصد سهم ذی‌نفع باید بین صفر و ۱۰۰'), 'relationship validation guards missing'],
  [service.includes('برای Override تأییدشده مرجع تأیید الزامی است') && service.includes('مرجع رضایت برای عضویت برنامه جایزه الزامی است'), 'pricing/reward validation guards missing'],
  [service.includes('مدرک MISSING/REJECTED مجاز نیست') && repository.includes('VERIFIED_AT'), 'document lifecycle persistence/validation missing'],
  [uiHooks.every(hook => wizardTs.includes(hook)), 'Angular wizard Phase 3 add/remove workflows are incomplete'],
  [phase3Tables.every(table => wizardTs.includes(table) || wizardHtml.includes(table)), 'wizard payload/UI does not reference all Phase 3 tables'],
  [wizardHtml.includes('صاحبان امضا') && wizardHtml.includes('وکالت/نمایندگی') && wizardHtml.includes('وضعیت مالیاتی') && wizardHtml.includes('عضویت اولیه برنامه جایزه'), 'Phase 3 operational UI sections are incomplete'],
  [exists('docs/DPS2-0.3.99-FOUR-DEPOSITS-PHASE3-AGGREGATE-QA.md') && exists('docs/install/INSTALL-0.3.99-FA.txt') && exists('docs/patches/PATCH-0.3.99-README-FA.txt'), '0.3.99 QA/install/patch documentation is incomplete']
];

const failed = checks.filter(([ok]) => !ok).map(([, message]) => message);
if (failed.length) {
  console.error('DPS2 Deposit Opening Phase 3 verification FAILED:');
  for (const message of failed) console.error(`- ${message}`);
  process.exit(1);
}
console.log(`DPS2 Deposit Opening Phase 3 verification OK: ${phase3Tables.length} additional aggregate tables, signatory-ID mapping, relationship/document/tax/pricing/reward persistence.`);
