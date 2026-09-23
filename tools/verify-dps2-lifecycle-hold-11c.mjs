import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const read=r=>fs.readFileSync(path.join(root,r),'utf8');
const exists=r=>fs.existsSync(path.join(root,r));
const models=read('backend/src/main/java/com/behsazan/corebanking/deposit/account/operations/domain/DepositAccountOperationsModels.java');
const servicingModels=read('backend/src/main/java/com/behsazan/corebanking/deposit/account/servicing/domain/DepositAccountServicingModels.java');
const opRepo=read('backend/src/main/java/com/behsazan/corebanking/deposit/account/operations/oracle/DepositAccountOperationsRepository.java');
const repo=read('backend/src/main/java/com/behsazan/corebanking/deposit/account/servicing/oracle/DepositAccountServicingRepository.java');
const service=read('backend/src/main/java/com/behsazan/corebanking/deposit/account/servicing/application/DepositAccountServicingService.java');
const controller=read('backend/src/main/java/com/behsazan/corebanking/deposit/account/operations/web/DepositAccountOperationsController.java');
const client=read('frontend/src/app/features/four-deposits/deposit-account-operations.service.ts');
const ts=read('frontend/src/app/features/four-deposits/deposit-account-operations.component.ts');
const html=read('frontend/src/app/features/four-deposits/deposit-account-operations.component.html');
const wizard=read('frontend/src/app/features/four-deposits/deposit-opening-wizard.component.ts');
const mig=read('database/oracle/dps2/migrations/0.11.0-phase11c-lifecycle-hold-runtime-readiness.sql');
const dbv=read('database/oracle/dps2/verification/0.11.0-phase11c-lifecycle-hold-verifier.sql');
const cmd=read('build-production.cmd'), sh=read('build-production.sh');
const checks=[
 [read('VERSION').trim()==='0.11.0','VERSION is 0.11.0'],
 [models.includes('AccountStatusHistory')&&models.includes('AccountHold')&&models.includes('HoldHistory'),'11C Account 360 models exist'],
 [models.includes('List<AccountStatusHistory> statusHistory')&&models.includes('List<AccountHold> holds')&&models.includes('List<HoldHistory> holdHistory'),'AccountDetails exposes status/hold history'],
 [servicingModels.includes('LifecycleActionRequest')&&servicingModels.includes('CreateHoldRequest')&&servicingModels.includes('ReleaseHoldRequest'),'11C command models exist'],
 [opRepo.includes('DEPOSIT_ACCOUNT_STATUS_HISTORY')&&opRepo.includes('DEPOSIT_ACCOUNT_HOLD_HISTORY'),'Account 360 reads lifecycle/hold history'],
 [opRepo.includes('A.DORMANCY_DATE'),'Account 360 exposes dormancy date'],
 [repo.includes("ACCOUNT_STATUS_CODE=:toStatus")&&repo.includes('DORMANCY_DATE='),'guarded lifecycle status update exists'],
 [repo.includes('DEPOSIT_ACCOUNT_STATUS_HISTORY')&&repo.includes('insertStatusHistory'),'status history persistence exists'],
 [repo.includes('DEPOSIT_ACCOUNT_HOLD(')&&repo.includes('insertHoldHistory'),'hold + hold history persistence exists'],
 [repo.includes('DEPOSIT_OPERATION_IDEMPOTENCY')&&repo.includes('completeIdempotency'),'idempotency persistence exists'],
 [service.includes('suspend(')&&service.includes('"SUSPEND","ACTIVE","SUSPENDED"'),'ACTIVE -> SUSPENDED implemented'],
 [service.includes('markDormant(')&&service.includes('"MARK_DORMANT","ACTIVE","DORMANT"'),'ACTIVE -> DORMANT implemented'],
 [service.includes('reactivate(')&&service.includes('Set.of("SUSPENDED","DORMANT")'),'SUSPENDED/DORMANT -> ACTIVE implemented'],
 [service.includes('expectedRecordVersion')&&service.includes('conflict('),'optimistic concurrency retained'],
 [service.includes('replayOrClaim')&&service.includes('X-Idempotency-Key الزامی است'),'idempotent lifecycle/hold contract exists'],
 [service.includes('COLLATERAL_PLEDGE')&&service.includes('ORIGIN_ONLY')&&service.includes('Hold وثیقه‌ای'),'collateral origin-only guard exists'],
 [service.includes('"PARTIAL".equals(type)')&&service.includes('ارز Hold باید با ارز حساب یکسان باشد'),'partial hold amount/currency guard exists'],
 [service.includes('enforceReleasePolicy')&&service.includes('AUTO_EXPIRY'),'hold release-policy enforcement exists'],
 [service.includes('HOLDABLE_ACCOUNT_STATUSES')&&!service.includes('createHold(accountId,r,actor,correlation,idempotencyKey);repository.changeStatus'),'hold creation does not transition account status'],
 [controller.includes('/lifecycle/suspend')&&controller.includes('/lifecycle/reactivate')&&controller.includes('/lifecycle/mark-dormant'),'lifecycle APIs exist'],
 [controller.includes('/holds/{holdId}/release')&&controller.includes('@RequestHeader(name="X-Idempotency-Key")'),'hold APIs require idempotency key'],
 [client.includes('suspend(id:number')&&client.includes('reactivate(id:number')&&client.includes('markDormant(id:number'),'Angular lifecycle client exists'],
 [client.includes('createHold(id:number')&&client.includes('releaseHold(id:number'),'Angular hold client exists'],
 [ts.includes('suspendSelected()')&&ts.includes('markDormantSelected()')&&ts.includes('reactivateSelected()'),'Lifecycle UI actions are wired'],
 [ts.includes('createHold()')&&ts.includes('releaseHold(holdId:number)'),'Hold UI actions are wired'],
 [html.includes('Hold / Block')&&html.includes('Status History')&&html.includes('مرزبندی Phase 11C'),'11C Account Operations UI contract exists'],
 [mig.includes('IX_DEP_HOLD_ACCOUNT_STATUS')&&mig.includes('IX_DEP_STATUS_HIST_ACCOUNT_AT')&&mig.includes('IX_DEP_IDEMP_ACCOUNT_OP'),'11C operational indexes are declared'],
 [mig.includes("'STATUS','HOLD'")&&mig.includes('DEPOSIT_ACCOUNT_SERVICING_HISTORY'),'servicing-history constraint supports STATUS/HOLD'],
 [!/(INSERT\s+INTO|UPDATE\s+DPS2\.|DELETE\s+FROM|MERGE\s+INTO)/i.test(mig),'11C migration contains no business DML'],
 [dbv.includes('PHASE11C_DB_BASELINE_PASS')&&dbv.includes('idempotency unique guard enabled'),'11C DB verifier covers runtime contract'],
 [dbv.includes('servicing history supports STATUS/HOLD'),'11C DB verifier covers servicing-history STATUS/HOLD contract'],
 [repo.includes('RECORD_VERSION=:version')&&!repo.includes('expectedRecordVersionsion'),'11B basic-info SQL placeholder regression fixed'],
 [wizard.includes('details.parties.some')&&!wizard.includes('details.owners.some')&&wizard.includes("o.statusCode==='ACTIVE'"),'opening funding ownership uses active Account Party contract'],
 [cmd.includes('verify-dps2-lifecycle-hold-11c.mjs')&&sh.includes('verify-dps2-lifecycle-hold-11c.mjs'),'production builds invoke 11C verifier'],
 [cmd.includes('Existing runtime JAR is locked or cannot be removed')&&cmd.includes('if errorlevel 1'),'Windows production build fails fast on locked runtime JAR'],
 [exists('tools/apply-dps2-phase11c.cmd'),'Windows 11C apply helper exists'],
 [exists('docs/DPS2-0.11.0-PHASE11C-LIFECYCLE-HOLD-QA.md'),'11C QA document exists'],
 [exists('release/QA-REPORT-0.11.0-PHASE11C.md'),'11C release QA report exists'],
 [exists('tools/runtime-dps2-phase11c-e2e.mjs'),'11C runtime E2E harness exists'],
 [read('tools/runtime-dps2-phase11c-e2e.mjs').includes('PHASE11C_RUNTIME_E2E_PASS'),'11C runtime E2E pass marker exists']
];
let pass=0,fail=0;for(const [ok,msg] of checks){if(ok){pass++;console.log(`PASS | ${msg}`)}else{fail++;console.error(`FAIL | ${msg}`)}}
console.log('------------------------------------------------------------');console.log(`PHASE11C_STATIC_VERIFIER_PASS=${pass}`);console.log(`PHASE11C_STATIC_VERIFIER_FAIL=${fail}`);if(fail)process.exit(1);console.log('PHASE11C_STATIC_BASELINE_PASS');
