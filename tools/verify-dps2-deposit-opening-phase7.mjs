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
const aggregate=read('backend/src/main/java/com/behsazan/corebanking/deposit/opening/application/DepositOpeningAggregateService.java');
const validator=read('backend/src/main/java/com/behsazan/corebanking/deposit/opening/readiness/application/DepositOpeningRuntimeValidator.java');
const runtimeRepo=read('backend/src/main/java/com/behsazan/corebanking/deposit/opening/readiness/oracle/DepositOpeningRuntimeRepository.java');
const readiness=read('backend/src/main/java/com/behsazan/corebanking/deposit/opening/readiness/application/DepositOpeningReadinessService.java');
const batch=read('backend/src/main/java/com/behsazan/corebanking/deposit/opening/batch/application/DepositOpeningBatchService.java');
const controller=read('backend/src/main/java/com/behsazan/corebanking/deposit/opening/web/DepositOpeningController.java');
const migration=read('database/oracle/dps2/migrations/0.7.0-phase7-opening-e2e-hardening.sql');
const serviceTs=read('frontend/src/app/features/four-deposits/deposit-opening.service.ts');
const wizardTs=read('frontend/src/app/features/four-deposits/deposit-opening-wizard.component.ts');
const wizardHtml=read('frontend/src/app/features/four-deposits/deposit-opening-wizard.component.html');
const readinessTs=read('frontend/src/app/features/four-deposits/deposit-opening-runtime-readiness.component.ts');
const readinessHtml=read('frontend/src/app/features/four-deposits/deposit-opening-runtime-readiness.component.html');
const routes=read('frontend/src/app/app.routes.ts');
const home=read('frontend/src/app/features/four-deposits/four-deposits-home.component.html');
const shell=read('frontend/src/app/layout/app-shell.component.html');
const breadcrumb=read('frontend/src/app/shared/ui/app-breadcrumb.component.ts');
const buildCmd=read('build-production.cmd');
const buildSh=read('build-production.sh');

const checks=[
 [semverAtLeast(version,'0.7.0'),`VERSION must be >= 0.7.0, got ${version}`],
 [pom.includes(`<version>${version}-SNAPSHOT</version>`)&&pkg.version===version,'backend/frontend version sync is incomplete'],
 [validator.includes('SUPPORTED_FAMILIES')&&['QARD_SAVINGS','CURRENT_ACCOUNT','SHORT_TERM_DEPOSIT','LONG_TERM_DEPOSIT'].every(x=>validator.includes(`"${x}"`)),'four-family runtime product guard is incomplete'],
 [validator.includes('REF_DEP_OPEN_REQUEST_TYPE')&&validator.includes('REF_DEP_OPEN_OWNERSHIP_TYPE')&&validator.includes('REF_DEP_OPEN_REQUEST_STATUS'),'DPS2 root reference validation is incomplete'],
 [validator.includes('repository.partyExists')&&runtimeRepo.includes('.PARTY WHERE PARTY_ID=:id'),'CIF.PARTY runtime validation is missing'],
 [runtimeRepo.includes('PRODUCT_VERSION PV JOIN')&&validator.includes('productClassCode')&&validator.includes('productFamilyCode'),'PDL Product/Product Version contract validation is missing'],
 [validator.includes('VERSION_STATUS_CODE')||validator.includes('versionStatusCode'),'product version lifecycle validation is missing'],
 [validator.includes('originationStatusCode')&&validator.includes('"OPEN"'),'origination OPEN guard is missing'],
 [aggregate.includes('runtimeValidator.validateAggregate(request)')&&aggregate.includes('validateRuntime('),'single Opening runtime validation/validate-only service is missing'],
 [aggregate.includes('DuplicateKeyException')&&aggregate.includes('findByIdempotencyKey(root.idempotencyKey())')&&aggregate.includes('true, Map.of()'),'single Opening concurrent idempotent replay guard is missing'],
 [batch.includes('runtimeValidator.validateBatchItem')&&batch.includes('runtimeValidator.validateBatchProcessContract'),'Batch runtime reference/product validation is missing'],
 [batch.includes('DuplicateKeyException')&&batch.includes('External Row Key داخل Batch باید یکتا باشد'),'Batch concurrent/idempotency hardening is missing'],
 [migration.includes('UX_DEP_OPEN_REQ_IDEMPOTENCY')&&migration.includes('UX_DEP_OPEN_BATCH_IDEMPOTENCY')&&migration.includes('UX_DEP_OPEN_BATCH_EXT_ROW_KEY')&&migration.includes('unique_guard_exists')&&migration.toUpperCase().includes('ALL_IND_COLUMNS'),'Phase 7 semantic unique guards are incomplete'],
 [migration.includes('-20701')&&migration.includes('-20702')&&migration.includes('-20703'),'legacy duplicate fail-fast guards are missing'],
 [readiness.includes('APPEND_ONLY')&&readiness.includes('IDEMPOTENCY')&&readiness.includes('uniqueGuardExists')&&runtimeRepo.includes('ALL_TRIGGERS')&&runtimeRepo.includes('ALL_INDEXES')&&runtimeRepo.includes('ALL_IND_COLUMNS'),'runtime readiness semantic metadata checks are incomplete'],
 [readiness.includes('PROPAGATION_REQUIRES_NEW')&&readiness.includes('setRollbackOnly')&&runtimeRepo.includes('PHASE7_ROLLBACK_PROBE')&&runtimeRepo.includes('LEGAL_BASIS_REFERENCE')&&runtimeRepo.includes('BULK_OPENING_BASIS_CODE')&&runtimeRepo.includes('CDD_APPROVAL_REFERENCE')&&runtimeRepo.includes('GOV_EMPLOYEE_SAVINGS_1376'),'isolated rollback probe is missing or is not Phase 10 batch-contract compatible'],
 [controller.includes('@PostMapping("/requests/validate")')&&controller.includes('@GetMapping("/readiness")')&&controller.includes('@PostMapping("/readiness/rollback-probe")'),'Phase 7 APIs are incomplete'],
 [serviceTs.includes('validateAggregate(')&&serviceTs.includes('getReadiness(')&&serviceTs.includes('runRollbackProbe('),'Angular Phase 7 API client is incomplete'],
 [wizardHtml.includes('کنترل Runtime — Oracle / CIF / PDL')&&wizardTs.includes('runRuntimePreflight'),'seven-step wizard runtime preflight is missing'],
 [readinessHtml.includes('آمادگی Runtime افتتاح حساب سپرده')&&readinessHtml.includes('Rollback Probe')&&readinessTs.includes('runProbe'),'dedicated runtime readiness UI is missing'],
 [routes.includes("path: 'four-deposits/runtime-readiness'")&&home.includes('/four-deposits/runtime-readiness')&&shell.includes('/four-deposits/runtime-readiness')&&breadcrumb.includes('/four-deposits/runtime-readiness'),'Phase 7 route/menu/home/breadcrumb wiring is incomplete'],
 [readiness.includes('ACCOUNT_SERVICE')&&readiness.includes('TAX_PROFILE_SERVICE')&&readiness.includes('"WARN"')&&exists('docs/DPS2-0.7.0-FOUR-DEPOSITS-PHASE7-E2E-HARDENING-QA.md'),'external HTML contracts must remain WARN and Phase 7 QA doc must exist']
];
const failed=checks.filter(([ok])=>!ok).map(([,msg])=>msg);
if(failed.length){console.error('DPS2 Deposit Opening Phase 7 verification FAILED:');for(const msg of failed)console.error(`- ${msg}`);process.exit(1)}
console.log(`DPS2 Deposit Opening Phase 7 verification OK: ${checks.length}/${checks.length} runtime validation, readiness, rollback and concurrency-hardening checks passed.`);
