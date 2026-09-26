import fs from 'node:fs';
const files={
 ver:'VERSION',
 ctrl:'backend/src/main/java/com/behsazan/corebanking/deposit/account/operations/web/DepositAccountOperationsController.java',
 svc:'backend/src/main/java/com/behsazan/corebanking/deposit/account/closure/application/DepositClosureService.java',
 repo:'backend/src/main/java/com/behsazan/corebanking/deposit/account/closure/oracle/DepositClosureRepository.java',
 model:'backend/src/main/java/com/behsazan/corebanking/deposit/account/closure/domain/DepositClosureModels.java',
 bal:'backend/src/main/java/com/behsazan/corebanking/deposit/account/balance/application/DepositBalanceService.java',
 ngs:'frontend/src/app/features/four-deposits/deposit-account-operations.service.ts',
 ngc:'frontend/src/app/features/four-deposits/deposit-account-operations.component.ts',
 ngh:'frontend/src/app/features/four-deposits/deposit-account-operations.component.html',
 mig:'database/oracle/dps2/migrations/0.11.0-phase11e-controlled-closure-reopening.sql',
 dbv:'database/oracle/dps2/verification/0.11.0-phase11e-controlled-closure-reopening-verifier.sql',
 run:'tools/runtime-dps2-phase11e-e2e.mjs',
 cmd:'tools/apply-dps2-phase11e.cmd',
 qa:'docs/DPS2-0.11.0-PHASE11E-CLOSURE-REOPENING-QA.md'
};
const text={};for(const [k,p] of Object.entries(files)){try{text[k]=fs.readFileSync(p,'utf8')}catch{text[k]=''}}
let pass=0,fail=0;function check(label,ok){if(ok){pass++;console.log(`PASS | ${label}`)}else{fail++;console.log(`FAIL | ${label}`)}}
check('VERSION remains 0.11.0',text.ver.trim()==='0.11.0');
check('11E closure domain models exist',text.model.includes('record ClosureCase')&&text.model.includes('record ReopeningCase'));
check('closure service exists',text.svc.includes('class DepositClosureService'));
check('closure repository exists',text.repo.includes('class DepositClosureRepository'));
check('generic direct close endpoint removed',!text.ctrl.includes('@PostMapping("/{accountId}/close")'));
check('controlled closure request API exists',text.ctrl.includes('@PostMapping("/{accountId}/closures")'));
check('closure approval API exists',text.ctrl.includes('/closures/{closureId}/approve'));
check('closure execute API exists',text.ctrl.includes('/closures/{closureId}/execute'));
check('reopening request API exists',text.ctrl.includes('@PostMapping("/{accountId}/reopenings")'));
check('reopening approval API exists',text.ctrl.includes('/reopenings/{reopeningId}/approve'));
check('reopening execute API exists',text.ctrl.includes('/reopenings/{reopeningId}/execute'));
check('11E mutation APIs require idempotency key',(text.ctrl.match(/X-Idempotency-Key/g)||[]).length>=9);
check('closure request requires ACTIVE',text.svc.includes('درخواست بستن فقط برای حساب ACTIVE'));
check('reopening request requires CLOSED',text.svc.includes('Reopening فقط برای حساب CLOSED'));
check('active Hold blocks closure execute',text.svc.includes('Hold فعال مانع بستن حساب است'));
check('active Reservation blocks closure execute',text.svc.includes('Reservation فعال مانع بستن حساب است'));
check('closure checks persist Hold',text.svc.includes('NO_ACTIVE_HOLD'));
check('closure checks persist Reservation',text.svc.includes('NO_ACTIVE_RESERVATION'));
check('closure checks settlement readiness',text.svc.includes('SETTLEMENT_READY'));
check('positive ledger creates settlement item',text.svc.includes('insertSettlement(closureId,ledger'));
check('positive ledger settles through 11D or evolved Step05 posting primitive',text.svc.includes('balanceService.post(accountId')||text.svc.includes('ACCOUNT_CLOSURE_SETTLEMENT'));
check('closure financial trace is legacy ACCOUNT_CLOSURE or evolved Step05 closure settlement',text.svc.includes('"ACCOUNT_CLOSURE",closureId')||text.svc.includes('"ACCOUNT_CLOSURE_SETTLEMENT"'));
check('closure final balance gate requires zero balances',text.svc.includes('مانده‌های حساب پس از تسویه Closure صفر نیستند'));
check('closure status transition is ACTIVE to CLOSED',text.svc.includes('changeStatus(accountId,account.recordVersion(),"ACTIVE","CLOSED"'));
check('reopening status transition is CLOSED to ACTIVE',text.svc.includes('changeStatus(accountId,account.recordVersion(),"CLOSED","ACTIVE"'));
check('CLOSE lifecycle event emitted',text.svc.includes('"CLOSE","ACTIVE","CLOSED"'));
check('REOPEN lifecycle event emitted',text.svc.includes('"REOPEN","CLOSED","ACTIVE"'));
check('closure status history emitted with canonical approval trace',text.svc.includes('"CLOSURE:"+closureId')&&text.svc.includes('c.approvalId(),actor'));
check('reopening status history emitted with canonical approval trace',text.svc.includes('"REOPENING:"+reopeningId')&&text.svc.includes('r.approvalId(),actor'));
check('approval maker/checker separation enforced',text.svc.includes('درخواست‌کننده و تأییدکننده Closure باید متفاوت باشند')&&text.svc.includes('درخواست‌کننده و تأییدکننده Reopening باید متفاوت باشند'));
check('approval actor must match designated approver',text.repo.includes("APPROVER_USER_ID=:actor"));
check('closure/reopening idempotency persisted',text.repo.includes('DEPOSIT_OPERATION_IDEMPOTENCY')&&text.svc.includes('ACCOUNT_CLOSURE_EXECUTE')&&text.svc.includes('ACCOUNT_REOPEN_EXECUTE'));
check('closure workflow reads checks and settlement items',text.repo.includes('DEPOSIT_ACCOUNT_CLOSURE_CHECK')&&text.repo.includes('DEPOSIT_ACCOUNT_CLOSURE_SETTLEMENT_ITEM'));
check('11E UI service methods exist',text.ngs.includes('requestClosure(')&&text.ngs.includes('executeReopening('));
check('11E UI removes direct close client',!text.ngs.includes('`${this.base}/${accountId}/close`'));
check('11E UI controlled workflow card exists',text.ngh.includes('Controlled Closure / Reopening'));
check('11E UI shows closure checks',text.ngh.includes('ch.checkCode')&&text.ngh.includes('ch.resultStatusCode'));
check('11E migration is DPS2 scoped',text.mig.includes('DPS2 Phase 11E')&&!text.mig.includes('CIF.'));
check('11E migration has no business insert/update/delete',!/^\s*(INSERT|UPDATE|DELETE)\s+/mi.test(text.mig));
check('closure account/status index declared',text.mig.includes('IX_DEP_CLOSURE_ACCOUNT_STATUS'));
check('reopening account/status index declared',text.mig.includes('IX_DEP_REOPEN_ACCOUNT_STATUS'));
check('approval account/status index declared',text.mig.includes('IX_DEP_APPROVAL_ACCOUNT_STATUS'));
check('11E DB verifier checks Package 16 tables',text.dbv.includes("assert_table('DEPOSIT_ACCOUNT_CLOSURE')")&&text.dbv.includes("assert_table('DEPOSIT_ACCOUNT_REOPENING')"));
check('11E DB verifier checks CLOSE/REOPEN lifecycle contract',text.dbv.includes('lifecycle event contract supports CLOSE/REOPEN'));
check('11E DB verifier checks executed approvals',text.dbv.includes('executed closures have approved approval request')&&text.dbv.includes('executed reopenings have approved approval request'));
check('11E apply helper exists',text.cmd.includes('PHASE11E_IMPLEMENTATION_PASS'));
check('11E runtime E2E exists',text.run.includes('PHASE11E_RUNTIME_E2E_PASS'));
check('runtime E2E closes then reopens account',text.run.includes("accountStatusCode!=='CLOSED'")&&text.run.includes("accountStatusCode!=='ACTIVE'"));
check('runtime E2E verifies evolved Step05 closure and approval traces',text.run.includes('CLOSURE_STEP05_SETTLEMENT_TRANSACTION_MISSING')&&text.run.includes('CLOSURE_STATUS_HISTORY_APPROVAL_TRACE_MISSING')&&text.run.includes('REOPEN_STATUS_HISTORY_APPROVAL_TRACE_MISSING'));
check('11E QA document exists',text.qa.includes('Phase 11E'));
console.log('------------------------------------------------------------');console.log(`PHASE11E_STATIC_VERIFIER_PASS=${pass}`);console.log(`PHASE11E_STATIC_VERIFIER_FAIL=${fail}`);if(fail)process.exit(1);console.log('PHASE11E_STATIC_BASELINE_PASS');
