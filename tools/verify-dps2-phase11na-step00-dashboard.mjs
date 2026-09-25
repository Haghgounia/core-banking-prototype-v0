import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const read=p=>fs.readFileSync(path.join(root,p),'utf8');
const exists=p=>fs.existsSync(path.join(root,p));
const files={
 roadmap:'docs/DPS2-CANONICAL-ROADMAP-FA.md',
 models:'backend/src/main/java/com/behsazan/corebanking/deposit/account/dashboard/domain/DepositAccountDashboardModels.java',
 repo:'backend/src/main/java/com/behsazan/corebanking/deposit/account/dashboard/oracle/DepositAccountDashboardRepository.java',
 svc:'backend/src/main/java/com/behsazan/corebanking/deposit/account/dashboard/application/DepositAccountDashboardService.java',
 ctl:'backend/src/main/java/com/behsazan/corebanking/deposit/account/operations/web/DepositAccountOperationsController.java',
 uiService:'frontend/src/app/features/four-deposits/deposit-account-operations.service.ts',
 uiTs:'frontend/src/app/features/four-deposits/deposit-account-operations.component.ts',
 uiHtml:'frontend/src/app/features/four-deposits/deposit-account-operations.component.html',
 db:'database/oracle/dps2/verification/0.11.0-phase11na-step00-dashboard-verifier.sql',
 runtime:'tools/runtime-dps2-phase11na-step00-dashboard-e2e.mjs',
 apply:'tools/apply-dps2-phase11na.cmd',
 qualify:'tools/qualify-dps2-phase11na.cmd',
 qa:'docs/DPS2-0.11.0-PHASE11NA-STEP00-DASHBOARD-QA.md',
 build:'build-production.cmd'
};
let pass=0,fail=0;
function chk(ok,msg){if(ok){pass++;console.log(`PASS | ${msg}`)}else{fail++;console.error(`FAIL | ${msg}`)}}
for(const [k,v] of Object.entries(files))chk(exists(v),`11N-A file exists: ${k}`);
const roadmap=read(files.roadmap),models=read(files.models),repo=read(files.repo),svc=read(files.svc),ctl=read(files.ctl),uiService=read(files.uiService),uiTs=read(files.uiTs),uiHtml=read(files.uiHtml),db=read(files.db),runtime=read(files.runtime),build=read(files.build);
chk(/11N-A/.test(roadmap)&&/Step 00/.test(roadmap),'roadmap registers Phase 11N-A Step 00 before closure');
chk(/DashboardView/.test(models)&&/AccountStatusSummary/.test(models)&&/HoldSummary/.test(models)&&/TransactionSummary/.test(models)&&/CurrencyBalanceSummary/.test(models),'dashboard domain exposes all Step 00 summaries');
for(const t of ['DEPOSIT_ACCOUNT','DEPOSIT_ACCOUNT_HOLD','DEPOSIT_TRANSACTION','DEPOSIT_ACCOUNT_BALANCE'])chk(repo.includes(t),`dashboard repository reads ${t}`);
chk(!/\b(INSERT|UPDATE|DELETE|MERGE)\b/i.test(repo),'dashboard repository contains no business DML');
chk(/@Transactional\(readOnly\s*=\s*true\)/.test(svc),'dashboard service is transactionally read-only');
chk(/@GetMapping\("\/dashboard"\)/.test(ctl)&&/dashboardService\.get\(\)/.test(ctl),'REST exposes GET-only dashboard endpoint');
chk(!/@(?:Post|Put|Patch|Delete)Mapping\("\/dashboard/.test(ctl),'REST exposes no dashboard mutation endpoint');
chk(/http\.get<DepositAccountDashboard>\(`\$\{this\.base\}\/dashboard`\)/.test(uiService),'Angular service reads dashboard through GET');
chk(/signal<DepositAccountDashboard\|null>/.test(uiTs)&&/loadDashboard\(\)/.test(uiTs),'Angular component loads dashboard runtime projection');
chk(/Step 00 · نمای عملیاتی سپرده/.test(uiHtml),'Angular UI contains canonical Step 00 dashboard');
chk(/هیچ Business Write ندارد/.test(uiHtml),'UI states Step 00 no-business-write boundary');
chk(!/\b(POST|PUT|DELETE|PATCH)\b/.test(runtime),'runtime Step 00 E2E uses no business mutation verb');
for(const t of ['DEPOSIT_ACCOUNT','DEPOSIT_ACCOUNT_HOLD','DEPOSIT_TRANSACTION','DEPOSIT_ACCOUNT_BALANCE'])chk(db.includes(t),`DB verifier checks ${t}`);
chk(/PHASE11NA_DB_BASELINE_PASS/.test(db),'DB verifier has final PASS marker');
chk(/PHASE11NA_RUNTIME_E2E_PASS/.test(runtime),'runtime has final PASS marker');
chk(/verify-dps2-phase11na-step00-dashboard\.mjs/.test(build),'production build includes Phase 11N-A static gate');
console.log('------------------------------------------------------------');
console.log(`PHASE11NA_STATIC_VERIFIER_PASS=${pass}`);console.log(`PHASE11NA_STATIC_VERIFIER_FAIL=${fail}`);
if(fail)process.exit(1);console.log('PHASE11NA_STATIC_BASELINE_PASS');
