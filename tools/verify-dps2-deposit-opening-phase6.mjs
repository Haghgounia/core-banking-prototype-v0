import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const read=relative=>fs.readFileSync(path.join(root,relative),'utf8');
const exists=relative=>fs.existsSync(path.join(root,relative));
const version=read('VERSION').trim();
function semverAtLeast(actual,minimum){const a=actual.split('.').map(Number),b=minimum.split('.').map(Number);for(let i=0;i<3;i++){if((a[i]||0)>(b[i]||0))return true;if((a[i]||0)<(b[i]||0))return false;}return true;}
const pom=read('backend/pom.xml');
const pkg=JSON.parse(read('frontend/package.json'));
const models=read('backend/src/main/java/com/behsazan/corebanking/deposit/opening/batch/domain/DepositOpeningBatchModels.java');
const repo=read('backend/src/main/java/com/behsazan/corebanking/deposit/opening/batch/oracle/DepositOpeningBatchRepository.java');
const service=read('backend/src/main/java/com/behsazan/corebanking/deposit/opening/batch/application/DepositOpeningBatchService.java');
const aggregateRepo=read('backend/src/main/java/com/behsazan/corebanking/deposit/opening/oracle/DepositOpeningAggregateRepository.java');
const controller=read('backend/src/main/java/com/behsazan/corebanking/deposit/opening/web/DepositOpeningController.java');
const accountService=read('backend/src/main/java/com/behsazan/corebanking/deposit/account/application/DepositAccountLifecycleService.java');
const migration=read('database/oracle/dps2/migrations/0.6.0-phase6-batch-opening.sql');
const frontendService=read('frontend/src/app/features/four-deposits/deposit-opening.service.ts');
const uiTs=read('frontend/src/app/features/four-deposits/deposit-opening-batch.component.ts');
const uiHtml=read('frontend/src/app/features/four-deposits/deposit-opening-batch.component.html');
const routes=read('frontend/src/app/app.routes.ts');
const home=read('frontend/src/app/features/four-deposits/four-deposits-home.component.html');
const shell=read('frontend/src/app/layout/app-shell.component.html');
const breadcrumb=read('frontend/src/app/shared/ui/app-breadcrumb.component.ts');
const buildCmd=read('build-production.cmd');
const buildSh=read('build-production.sh');

const batchStatuses=['DRAFT','VALIDATING','READY','PROCESSING','COMPLETED','PARTIAL','FAILED'];
const itemStatuses=['PENDING','VALID','INVALID','PROCESSING','SUCCESS','FAILED'];
const sourceTypes=['FILE','API','MANUAL'];
const endpoints=['@PostMapping("/batches")','@GetMapping("/batches/{batchId}")','@PostMapping("/batches/{batchId}/validate")','@PostMapping("/batches/{batchId}/process")','@PostMapping("/batches/{batchId}/activate")'];

const checks=[
  [semverAtLeast(version,'0.6.0'),`VERSION must be >= 0.6.0, got ${version}`],
  [pom.includes(`<version>${version}-SNAPSHOT</version>`),'backend version must match VERSION'],
  [pkg.version===version,'frontend version must match VERSION'],
  [models.includes('record BatchCreateRequest')&&models.includes('record BatchItemView')&&models.includes('record BatchErrorView'),'Batch domain contracts are incomplete'],
  [repo.includes('DEPOSIT_OPENING_BATCH')&&repo.includes('DEPOSIT_OPENING_BATCH_ITEM')&&repo.includes('DEPOSIT_OPENING_BATCH_ERROR'),'Batch repository does not cover Header/Item/Error tables'],
  [repo.includes('${core-banking.schemas.cif:CIF}')&&repo.includes('${core-banking.schemas.product-definition:PDL}')&&repo.includes('.PARTY')&&repo.includes('.PRODUCT_VERSION'),'Batch validation does not use Party/Product integration contracts'],
  [semverAtLeast(version,'0.10.0') ? service.includes('پردازش مستقیم Batch در Operational v5') : (service.includes('PROPAGATION_REQUIRES_NEW')&&service.includes('requiresNew.executeWithoutResult')),'per-item transaction isolation / v5 direct-processing guard is missing'],
  [semverAtLeast(version,'0.10.0') ? service.includes('هر Batch Item باید ابتدا Opening مستقل') : (service.includes('new OpeningRequest(')&&service.includes('"BULK"')&&service.includes('"INDIVIDUAL"')&&service.includes('"APPROVED"')),'normalized Opening creation / v5 independent-opening guard is missing'],
  [aggregateRepo.includes('insertBatchRequest')&&aggregateRepo.includes('BATCH_ITEM_ID'),'Batch item -> Opening Request linkage is missing'],
  [semverAtLeast(version,'0.10.0') ? service.includes('QARD_SAVINGS') : (service.includes('new OpeningParty')&&service.includes('"OWNER"')&&service.includes('new BigDecimal("100")')),'HTML quick-grid normalization / v5 QARD-only guard is missing'],
  [semverAtLeast(version,'0.10.0') ? service.includes('فعال‌سازی مستقیم گروهی در Operational v5 مجاز نیست') : (service.includes('accountLifecycleService.createAccount')&&service.includes('accountLifecycleService.activateAccount')),'existing Account Lifecycle reuse / v5 direct-activation guard is missing'],
  [accountService.includes('PENDING_ACTIVATION')&&accountService.includes('ACTIVE'),'account create/activation lifecycle contract is missing'],
  [endpoints.every(x=>controller.includes(x)),'Batch API endpoints are incomplete'],
  [batchStatuses.every(x=>migration.includes(`'${x}'`)),'EA Batch status seed is incomplete'],
  [itemStatuses.every(x=>migration.includes(`'${x}'`)),'EA Batch item status seed is incomplete'],
  [sourceTypes.every(x=>migration.includes(`'${x}'`)),'EA Batch source type seed is incomplete'],
  [['VALIDATION','PROCESSING','POSTING'].every(x=>migration.includes(`'${x}'`)),'EA Batch error stages are incomplete'],
  [migration.includes("'ROW_INVALID'")&&migration.includes('Prototype provisional'),'HTML-aligned provisional ROW_INVALID governance note is missing'],
  [migration.includes("'PROCESSING_FAILED'")&&migration.includes("'ACTIVATION_FAILED'")&&migration.includes('Data-Governance'),'technical provisional error codes are not explicitly governed'],
  [frontendService.includes('createBatch(')&&frontendService.includes('validateBatch(')&&frontendService.includes('processBatch(')&&frontendService.includes('activateBatch('),'Angular Batch API service is incomplete'],
  [semverAtLeast(version,'0.10.0')
    ? (uiHtml.includes('افتتاح گروهی حساب سپرده')
      &&uiHtml.includes('اعتبارسنجی Batch')
      &&uiHtml.includes('به‌روزرسانی وضعیت ردیف‌ها')
      &&uiHtml.includes('تکمیل Opening فردی')
      &&uiHtml.includes('پردازش و فعال‌سازی مستقیم گروهی در Backend فعلاً مسدود است')
      &&!uiHtml.includes('پردازش و ایجاد حساب')
      &&!uiHtml.includes('فعال‌سازی حساب‌های موفق'))
    : (uiHtml.includes('افتتاح گروهی حساب سپرده')
      &&uiHtml.includes('اعتبارسنجی Batch')
      &&(uiHtml.includes('پردازش و ایجاد حساب')||uiHtml.includes('پردازش پس از CDD فردی'))
      &&(uiHtml.includes('فعال‌سازی حساب‌های موفق')||uiHtml.includes('فعال‌سازی فقط فردی'))),
    'supplied HTML / Operational v5 Batch actions are not represented'],
  [uiHtml.includes('Party ID')&&uiHtml.includes('Product Version')&&uiHtml.includes('Request No')&&uiHtml.includes('Account No'),'supplied HTML Bulk grid columns are incomplete'],
  [uiTs.includes('QARD_SAVINGS')&&uiTs.includes('CURRENT_ACCOUNT')&&uiTs.includes('SHORT_TERM_DEPOSIT')&&uiTs.includes('LONG_TERM_DEPOSIT'),'four deposit families are incomplete in Batch UI'],
  [routes.includes("path: 'four-deposits/batch-opening'")&&home.includes('/four-deposits/batch-opening')&&shell.includes('/four-deposits/batch-opening')&&breadcrumb.includes('/four-deposits/batch-opening'),'Batch route/navigation is incomplete'],
  [!service.includes('CERTIFICATE')&&!uiHtml.includes('گواهی سپرده'),'Certificate of Deposit leaked into Opening Batch scope'],
  [buildCmd.includes('verify-dps2-deposit-opening-phase6.mjs')&&buildSh.includes('verify-dps2-deposit-opening-phase6.mjs'),'production build does not execute Phase 6 verifier'],
  [exists('docs/DPS2-0.6.0-FOUR-DEPOSITS-PHASE6-BATCH-OPENING-QA.md')&&exists('docs/install/INSTALL-0.6.0-FA.txt')&&exists('docs/patches/PATCH-0.6.0-README-FA.txt'),'Phase 6 QA/install/patch docs are incomplete']
];
const failed=checks.filter(([ok])=>!ok).map(([,msg])=>msg);
if(failed.length){console.error('DPS2 Deposit Opening Phase 6 verification FAILED:');for(const msg of failed)console.error(`- ${msg}`);process.exit(1)}
console.log(semverAtLeast(version,'0.10.0') ? 'DPS2 Deposit Opening Phase 6 compatibility verification OK: Header/Item/Error contracts preserved; Operational v5 blocks direct Batch processing/activation pending per-item Opening gates.' : 'DPS2 Deposit Opening Phase 6 verification OK: supplied-HTML Batch Header/Item/Error flow, isolated item processing, Opening/Account linkage, activation and dedicated UI/API verified.');
