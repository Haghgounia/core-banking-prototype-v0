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
const controller = read('backend/src/main/java/com/behsazan/corebanking/deposit/opening/web/DepositOpeningController.java');
const service = read('backend/src/main/java/com/behsazan/corebanking/deposit/opening/application/DepositOpeningAggregateService.java');
const repository = read('backend/src/main/java/com/behsazan/corebanking/deposit/opening/oracle/DepositOpeningAggregateRepository.java');
const auditRepository = read('backend/src/main/java/com/behsazan/corebanking/deposit/opening/audit/oracle/DepositOpeningAuditRepository.java');
const models = read('backend/src/main/java/com/behsazan/corebanking/deposit/opening/domain/DepositOpeningModels.java');
const handler = read('backend/src/main/java/com/behsazan/corebanking/shared/error/GlobalExceptionHandler.java');
const frontendService = read('frontend/src/app/features/four-deposits/deposit-opening.service.ts');
const wizardTs = read('frontend/src/app/features/four-deposits/deposit-opening-wizard.component.ts');
const wizardHtml = read('frontend/src/app/features/four-deposits/deposit-opening-wizard.component.html');

const persistedTables = [
  'DEPOSIT_OPENING_REQUEST',
  'DEPOSIT_OPENING_PARTY',
  'DEPOSIT_OPENING_SIGNATURE_RULE',
  'DEPOSIT_OPENING_TERM',
  'DEPOSIT_OPENING_MATURITY_INSTRUCTION',
  'DEPOSIT_OPENING_PROFIT_INSTRUCTION',
  'DEPOSIT_OPENING_WITHDRAWAL_MEDIA',
  'DEPOSIT_OPENING_SERVICE_SELECTION',
  'DEPOSIT_OPENING_FUNDING',
  'DEPOSIT_OPENING_CHECK',
  'DEPOSIT_OPENING_TERMS_ACCEPTANCE',
  'DEPOSIT_OPENING_DECISION',
  'DEPOSIT_OPENING_STATUS_HISTORY'
];
const sourceCheckCodes = versionAtLeast(version,'0.10.0') ? [
  'CUSTOMER_IDENTITY','MOBILE_OWNERSHIP','LEGAL_CAPACITY','KYC_CDD','PEP_SANCTIONS',
  'CUSTOMER_RISK','EXPECTED_ACTIVITY','ACCOUNT_COUNT_STATUS','PRODUCT_ELIGIBILITY','DOCUMENTS',
  'INQUIRIES','SIGNATORY_AUTHORITY','TERMS_ACCEPTANCE','SHARIA_CONTRACT','TAX_PROFILE','DUPLICATE_REQUEST'
] : [
  'PRODUCT_ELIGIBILITY','KYC_CDD','SANCTIONS','DOCUMENTS','INQUIRIES',
  'OPENING_RULES','SIGNATORY_AUTHORITY','TERMS_ACCEPTANCE','DUPLICATE_REQUEST'
];

const checks = [
  [versionAtLeast(version,'0.3.98'), `VERSION must preserve Phase 2 or later source, got ${version}`],
  [pom.includes(`<version>${version}-SNAPSHOT</version>`), `backend Maven source version is not ${version}-SNAPSHOT`],
  [packageJson.version === version, `frontend source version must match VERSION ${version}, got ${packageJson.version}`],
  [models.includes('@JsonProperty("DEPOSIT_OPENING_REQUEST")') && models.includes('PersistedAggregateResponse'), 'typed aggregate JSON contract is incomplete'],
  [controller.includes('@RequestMapping("/api/v1/deposit-opening")') && controller.includes('@PostMapping("/requests")'), 'deposit-opening create endpoint missing'],
  [service.includes('@Transactional') && service.includes('findByIdempotencyKey') && service.includes('true, Map.of()'), 'transaction/idempotency service contract missing'],
  [service.includes('جمع درصد مالکیت') && (service.includes('همه کنترل‌ها باید PASS') || service.includes('همه کنترل‌های الزامی Account Creation')) && service.includes('تصمیم نهایی درخواست تأییدشده باید APPROVE'), 'business validation guards are incomplete'],
  [persistedTables.every(table => repository.includes(`INSERT INTO %s.${table}`)), 'repository does not cover the complete Phase 2 aggregate table set'],
  [repository.includes('SEQ_DEPOSIT_OPENING_REQUEST') && repository.includes('SEQ_DEPOSIT_OPENING_TERM'), 'explicit parent/term sequence allocation missing'],
  [(repository.includes('Initial aggregate persistence') && service.includes('insertInitialStatusHistory')) || (service.includes('recordOpeningCreated') && auditRepository.includes('insertStatusHistory')), 'system-managed initial status history is missing'],
  [handler.includes('DEPOSIT_OPENING_VALIDATION_FAILED'), 'global ProblemDetail mapping for deposit opening validation is missing'],
  [frontendService.includes("'/api/v1/deposit-opening'") && frontendService.includes('createAggregate'), 'Angular deposit opening API client is missing'],
  [wizardTs.includes('persistAggregate()') && (wizardHtml.includes('ثبت اتمیک Aggregate') || wizardHtml.includes('ثبت Opening')) && wizardHtml.includes('OPENING_REQUEST_ID='), 'wizard is not connected to atomic persistence'],
  [sourceCheckCodes.every(code => wizardTs.includes(`code:'${code}'`)), 'wizard check codes are not aligned with the active opening prototype'],
  [wizardTs.includes("fundingMethod:new FormControl('TRANSFER'") && wizardTs.includes("ACCEPTANCE_SOURCE_CODE:v.channel==='BRANCH'?'BRANCH':v.channel==='API'?'API':'UI'"), 'funding/terms reference codes are not aligned with source contract'],
  [exists('docs/DPS2-0.3.98-FOUR-DEPOSITS-PHASE2-PERSISTENCE-QA.md') && exists('docs/install/INSTALL-0.3.98-FA.txt') && exists('docs/patches/PATCH-0.3.98-README-FA.txt'), '0.3.98 QA/install/patch documentation is incomplete']
];

const failed = checks.filter(([ok]) => !ok).map(([, message]) => message);
if (failed.length) {
  console.error('DPS2 Deposit Opening persistence verification FAILED:');
  for (const message of failed) console.error(`- ${message}`);
  process.exit(1);
}
console.log(`DPS2 Deposit Opening persistence verification OK: ${persistedTables.length} transactional table contracts, ${sourceCheckCodes.length} source-aligned checks, idempotent create endpoint.`);
