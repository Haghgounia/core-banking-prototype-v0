import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const read=relative=>fs.readFileSync(path.join(root,relative),'utf8');
const exists=relative=>fs.existsSync(path.join(root,relative));
const version=read('VERSION').trim();
const pom=read('backend/pom.xml');
const pkg=JSON.parse(read('frontend/package.json'));
const repo=read('backend/src/main/java/com/behsazan/corebanking/deposit/account/operations/oracle/DepositAccountOperationsRepository.java');
const service=read('backend/src/main/java/com/behsazan/corebanking/deposit/account/operations/application/DepositAccountOperationsService.java');
const controller=read('backend/src/main/java/com/behsazan/corebanking/deposit/account/operations/web/DepositAccountOperationsController.java');
const models=read('backend/src/main/java/com/behsazan/corebanking/deposit/account/operations/domain/DepositAccountOperationsModels.java');
const ts=read('frontend/src/app/features/four-deposits/deposit-account-operations.component.ts');
const html=read('frontend/src/app/features/four-deposits/deposit-account-operations.component.html');
const client=read('frontend/src/app/features/four-deposits/deposit-account-operations.service.ts');
const routes=read('frontend/src/app/app.routes.ts');
const shell=read('frontend/src/app/layout/app-shell.component.html');
const home=read('frontend/src/app/features/four-deposits/four-deposits-home.component.html');
const breadcrumb=read('frontend/src/app/shared/ui/app-breadcrumb.component.ts');
const config=read('backend/src/main/resources/application.yml');
const buildCmd=read('build-production.cmd');
const buildSh=read('build-production.sh');

const semverAtLeast=(actual,minimum)=>{const a=actual.split('.').map(Number),b=minimum.split('.').map(Number);for(let i=0;i<3;i++){if((a[i]||0)>(b[i]||0))return true;if((a[i]||0)<(b[i]||0))return false;}return true;};
const mutatingSql=/\b(INSERT\s+INTO|UPDATE\s+[^\s]|DELETE\s+FROM|MERGE\s+INTO)\b/i;
const checks=[
 [semverAtLeast(version,'0.8.0'),`VERSION must be >= 0.8.0, got ${version}`],
 [pom.includes(`<version>${version}-SNAPSHOT</version>`)&&pkg.version===version,'backend/frontend version sync is incomplete'],
 [exists('backend/src/main/java/com/behsazan/corebanking/deposit/account/operations/domain/DepositAccountOperationsModels.java'),'Account Operations domain models are missing'],
 [models.includes('AccountSummary')&&models.includes('AccountDetails')&&models.includes('LifecycleEvent')&&models.includes('OwnerParty'),'Account 360 read models are incomplete'],
 [repo.includes('DEPOSIT_ACCOUNT A')&&repo.includes('DEPOSIT_OPENING_REQUEST R')&&repo.includes('DEPOSIT_OPENING_PARTY'),'Account/Opening/Party read model linkage is incomplete'],
 [repo.includes('PRODUCT_VERSION PV')&&repo.includes('PRODUCT_FAMILY_CODE'),'PDL product-family enrichment is missing'],
 [repo.includes('DEPOSIT_ACCOUNT_LIFECYCLE_EVENT')&&repo.includes('ORDER BY EVENT_AT'),'lifecycle timeline query is missing'],
 [!mutatingSql.test(repo),'Phase 8 Foundation repository must remain read-only'],
 [service.includes('PENDING_ACTIVATION')&&service.includes('ACTIVE')&&service.includes('CLOSED'),'existing account status contract is not preserved'],
 [['QARD_SAVINGS','CURRENT_ACCOUNT','SHORT_TERM_DEPOSIT','LONG_TERM_DEPOSIT'].every(x=>service.includes(`"${x}"`)),'four-family search guard is incomplete'],
 [controller.includes('@RequestMapping("/api/v1/deposit-accounts")')&&controller.includes('@GetMapping')&&(semverAtLeast(version,'0.9.0')||(!controller.includes('@PostMapping')&&!controller.includes('@PutMapping')&&!controller.includes('@DeleteMapping'))),'Phase 8 GET contract is missing or pre-0.9 API is unexpectedly mutating'],
 [client.includes("private readonly base='/api/v1/deposit-accounts'")&&client.includes('search(')&&client.includes('get(accountId:number)'),'Angular Account Operations API client is incomplete'],
 [html.includes('Account 360')&&html.includes('Timeline چرخه عمر')&&(semverAtLeast(version,'0.9.0')||html.includes('Read-only Foundation')),'Account Operations workspace contract is incomplete'],
 [ts.includes('QARD_SAVINGS')&&ts.includes('CURRENT_ACCOUNT')&&ts.includes('SHORT_TERM_DEPOSIT')&&ts.includes('LONG_TERM_DEPOSIT'),'four families are not exposed in Account Operations UI'],
 [routes.includes("path: 'four-deposits/account-operations'")&&shell.includes('/four-deposits/account-operations')&&home.includes('/four-deposits/account-operations')&&breadcrumb.includes('/four-deposits/account-operations'),'route/menu/home/breadcrumb wiring is incomplete'],
 [config.includes('deposit-account: DPS2'),'deposit-account schema configuration is missing'],
 [buildCmd.includes('verify-dps2-deposit-account-operations-phase8.mjs')&&buildSh.includes('verify-dps2-deposit-account-operations-phase8.mjs'),'production build does not invoke Phase 8 verifier'],
 [exists('docs/DPS2-0.8.0-FOUR-DEPOSITS-ACCOUNT-OPERATIONS-FOUNDATION-QA.md'),'Phase 8 QA document is missing']
];
const failed=checks.filter(([ok])=>!ok).map(([,msg])=>msg);
if(failed.length){console.error('DPS2 Deposit Account Operations Phase 8 verification FAILED:');for(const msg of failed)console.error(`- ${msg}`);process.exit(1)}
console.log(`DPS2 Deposit Account Operations Phase 8 verification OK: ${checks.length}/${checks.length} read-only Account Operations Foundation checks passed.`);
