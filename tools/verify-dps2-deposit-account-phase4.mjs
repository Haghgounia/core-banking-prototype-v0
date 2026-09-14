import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const read=relative=>fs.readFileSync(path.join(root,relative),'utf8');
const exists=relative=>fs.existsSync(path.join(root,relative));
const version=read('VERSION').trim();
const versionAtLeast=(actual,minimum)=>{const a=actual.split('.').map(Number),m=minimum.split('.').map(Number);for(let i=0;i<3;i++){if(a[i]>m[i])return true;if(a[i]<m[i])return false}return true};
const pom=read('backend/pom.xml');
const packageJson=JSON.parse(read('frontend/package.json'));
const controller=read('backend/src/main/java/com/behsazan/corebanking/deposit/opening/web/DepositOpeningController.java');
const service=read('backend/src/main/java/com/behsazan/corebanking/deposit/account/application/DepositAccountLifecycleService.java');
const repository=read('backend/src/main/java/com/behsazan/corebanking/deposit/account/oracle/DepositAccountRepository.java');
const models=read('backend/src/main/java/com/behsazan/corebanking/deposit/account/domain/DepositAccountModels.java');
const migration=read('database/oracle/dps2/migrations/0.4.0-phase4-deposit-account-lifecycle.sql');
const frontendService=read('frontend/src/app/features/four-deposits/deposit-opening.service.ts');
const wizardTs=read('frontend/src/app/features/four-deposits/deposit-opening-wizard.component.ts');
const wizardHtml=read('frontend/src/app/features/four-deposits/deposit-opening-wizard.component.html');

const checks=[
 [versionAtLeast(version,'0.4.0'),`VERSION must preserve Phase 4 or later source, got ${version}`],
 [pom.includes(`<version>${version}-SNAPSHOT</version>`),`backend Maven source version is not ${version}-SNAPSHOT`],
 [packageJson.version===version,`frontend source version must match ${version}, got ${packageJson.version}`],
 [controller.includes('@PostMapping("/requests/{id}/account")')&&controller.includes('@PostMapping("/requests/{id}/account/activate")')&&controller.includes('@GetMapping("/requests/{id}/account")'),'Phase 4 account lifecycle endpoints are incomplete'],
 [service.includes('repository.lockOpening(openingRequestId)')&&repository.includes('FOR UPDATE'),'Opening-level SELECT FOR UPDATE concurrency guard is missing'],
 [service.includes('opening.createdAccountId() != null')&&service.includes('response(existing, true)'),'Account creation is not idempotent'],
 [service.includes('"PENDING_ACTIVATION".equals')&&repository.includes("ACCOUNT_STATUS_CODE = 'PENDING_ACTIVATION'")&&repository.includes("ACCOUNT_STATUS_CODE = 'ACTIVE'"),'PENDING_ACTIVATION -> ACTIVE transition contract missing'],
 [repository.includes('CREATED_ACCOUNT_ID = :accountId'),'CREATED_ACCOUNT_ID integration reference is not persisted'],
 [repository.includes("REQUEST_STATUS_CODE = 'COMPLETED'")&&repository.includes("'APPROVED', 'COMPLETED'")&&repository.includes('DEPOSIT_OPENING_STATUS_HISTORY'),'Opening completion/history transition missing'],
 [repository.includes('"CREATE"')||service.includes('"CREATE"'),'CREATE lifecycle event missing'],
 [service.includes('"ACTIVATE"')&&repository.includes('DEPOSIT_ACCOUNT_LIFECYCLE_EVENT'),'ACTIVATE lifecycle event missing'],
 [models.includes('record AccountLifecycleResponse('),'typed Phase 4 lifecycle response missing'],
 [migration.includes('CREATE TABLE DPS2.DEPOSIT_ACCOUNT')&&migration.includes('CREATE TABLE DPS2.DEPOSIT_ACCOUNT_LIFECYCLE_EVENT'),'Phase 4 Oracle tables missing'],
 [migration.includes('intentionally no physical cross-domain FK')||migration.includes('intentionally no physical cross-domain FK'.toUpperCase()),'cross-domain FK boundary is not documented'],
 [migration.includes('TECHNICAL PROTOTYPE NUMBERS'),'DPA prototype number warning is missing'],
 [frontendService.includes('createAccount(')&&frontendService.includes('activateAccount(')&&frontendService.includes('getAccount('),'Angular account lifecycle API client is incomplete'],
 [wizardTs.includes('createDepositAccount()')&&wizardTs.includes('activateDepositAccount()')&&wizardHtml.includes('ثبت Opening')&&wizardHtml.includes('ایجاد حساب')&&wizardHtml.includes('فعال‌سازی حساب'),'Step 7 three-stage lifecycle UI is incomplete'],
 [exists('docs/DPS2-0.4.0-FOUR-DEPOSITS-PHASE4-ACCOUNT-LIFECYCLE-QA.md')&&exists('docs/install/INSTALL-0.4.0-FA.txt')&&exists('docs/patches/PATCH-0.4.0-README-FA.txt'),'0.4.0 release documentation is incomplete']
];
const failed=checks.filter(([ok])=>!ok).map(([,message])=>message);
if(failed.length){console.error('DPS2 Deposit Account Phase 4 verification FAILED:');for(const message of failed)console.error(`- ${message}`);process.exit(1)}
console.log('DPS2 Deposit Account Phase 4 verification OK: lifecycle APIs, idempotent locked creation, activation, opening completion, events, migration and Step 7 UI.');
