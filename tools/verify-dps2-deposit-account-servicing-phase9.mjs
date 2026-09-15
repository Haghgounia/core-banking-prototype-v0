import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const read=relative=>fs.readFileSync(path.join(root,relative),'utf8');
const exists=relative=>fs.existsSync(path.join(root,relative));
const version=read('VERSION').trim();
const pom=read('backend/pom.xml');
const pkg=JSON.parse(read('frontend/package.json'));
const models=read('backend/src/main/java/com/behsazan/corebanking/deposit/account/servicing/domain/DepositAccountServicingModels.java');
const repo=read('backend/src/main/java/com/behsazan/corebanking/deposit/account/servicing/oracle/DepositAccountServicingRepository.java');
const service=read('backend/src/main/java/com/behsazan/corebanking/deposit/account/servicing/application/DepositAccountServicingService.java');
const controller=read('backend/src/main/java/com/behsazan/corebanking/deposit/account/operations/web/DepositAccountOperationsController.java');
const client=read('frontend/src/app/features/four-deposits/deposit-account-operations.service.ts');
const ts=read('frontend/src/app/features/four-deposits/deposit-account-operations.component.ts');
const html=read('frontend/src/app/features/four-deposits/deposit-account-operations.component.html');
const migration=read('database/oracle/dps2/migrations/0.9.0-phase9-account-closure-servicing.sql');
const buildCmd=read('build-production.cmd');
const buildSh=read('build-production.sh');

const semverAtLeast=(actual,minimum)=>{const a=actual.split('.').map(Number),b=minimum.split('.').map(Number);for(let i=0;i<3;i++){if((a[i]||0)>(b[i]||0))return true;if((a[i]||0)<(b[i]||0))return false;}return true;};
const checks=[
 [semverAtLeast(version,'0.9.0'),`VERSION must be >= 0.9.0, got ${version}`],
 [pom.includes(`<version>${version}-SNAPSHOT</version>`)&&pkg.version===version,'backend/frontend version sync is incomplete'],
 [models.includes('CloseAccountRequest')&&models.includes('expectedRecordVersion')&&models.includes('CloseAccountResponse'),'Phase 9 servicing models are incomplete'],
 [repo.includes('FOR UPDATE')&&repo.includes('RECORD_VERSION'),'row lock / optimistic version contract is missing'],
 [repo.includes("ACCOUNT_STATUS_CODE='CLOSED'")&&repo.includes("ACCOUNT_STATUS_CODE='ACTIVE'")&&repo.includes('RECORD_VERSION=:expectedRecordVersion'),'ACTIVE -> CLOSED guarded update is missing'],
 [repo.includes("'CLOSE', 'ACTIVE', 'CLOSED'")&&repo.includes('DEPOSIT_ACCOUNT_LIFECYCLE_EVENT'),'CLOSE lifecycle event insert is missing'],
 [service.includes('"CLOSED".equals(status)')&&service.includes('new CloseAccountResponse(operationsService.get(accountId), true)'),'idempotent CLOSED replay guard is missing'],
 [service.includes('account.recordVersion() != request.expectedRecordVersion()'),'stale Record Version validation is missing'],
 [service.includes('!"ACTIVE".equals(status)'),'closure must be limited to ACTIVE accounts'],
 [controller.includes('@PostMapping("/{accountId}/close")')&&controller.includes('X-User-Id')&&controller.includes('X-Correlation-Id'),'closure API / audit headers are incomplete'],
 [client.includes('close(accountId:number,expectedRecordVersion:number)')&&client.includes('/close`'),'Angular close-account client is missing'],
 [ts.includes('closeSelected()')&&ts.includes('detail.account.recordVersion'),'UI does not send optimistic Record Version'],
 [html.includes('ACTIVE → CLOSED')&&html.includes('Gap مدل')&&html.includes('بستن حساب'),'controlled Servicing UX is incomplete'],
 [migration.includes("EVENT_TYPE_CODE IN ('CREATE','ACTIVATE','CLOSE')"),'Oracle lifecycle event constraint does not allow CLOSE'],
 [migration.includes('TRG_DEP_ACCT_EVT_APPEND_ONLY')&&migration.includes('BEFORE UPDATE OR DELETE'),'append-only lifecycle trigger is missing'],
 [!migration.match(/ALTER\s+TABLE\s+DPS2\.DEPOSIT_ACCOUNT\s+ADD\s*\(/i),'Phase 9 must not invent new DEPOSIT_ACCOUNT columns'],
 [buildCmd.includes('verify-dps2-deposit-account-servicing-phase9.mjs')&&buildSh.includes('verify-dps2-deposit-account-servicing-phase9.mjs'),'production build does not invoke Phase 9 verifier'],
 [exists('docs/DPS2-0.9.0-FOUR-DEPOSITS-ACCOUNT-SERVICING-QA.md'),'Phase 9 QA document is missing']
];
const failed=checks.filter(([ok])=>!ok).map(([,msg])=>msg);
if(failed.length){console.error('DPS2 Deposit Account Servicing Phase 9 verification FAILED:');for(const msg of failed)console.error(`- ${msg}`);process.exit(1)}
console.log(`DPS2 Deposit Account Servicing Phase 9 verification OK: ${checks.length}/${checks.length} controlled Account Closure checks passed.`);
