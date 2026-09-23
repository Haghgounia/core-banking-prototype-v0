import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const read=r=>fs.readFileSync(path.join(root,r),'utf8');
const exists=r=>fs.existsSync(path.join(root,r));
const models=read('backend/src/main/java/com/behsazan/corebanking/deposit/account/balance/domain/DepositBalanceModels.java');
const repo=read('backend/src/main/java/com/behsazan/corebanking/deposit/account/balance/oracle/DepositBalanceRepository.java');
const service=read('backend/src/main/java/com/behsazan/corebanking/deposit/account/balance/application/DepositBalanceService.java');
const accountLifecycle=read('backend/src/main/java/com/behsazan/corebanking/deposit/account/application/DepositAccountLifecycleService.java');
const openingService=read('backend/src/main/java/com/behsazan/corebanking/deposit/opening/operational/application/DepositOpeningOperationalService.java');
const servicing=read('backend/src/main/java/com/behsazan/corebanking/deposit/account/servicing/application/DepositAccountServicingService.java');
const opsModels=read('backend/src/main/java/com/behsazan/corebanking/deposit/account/operations/domain/DepositAccountOperationsModels.java');
const opsService=read('backend/src/main/java/com/behsazan/corebanking/deposit/account/operations/application/DepositAccountOperationsService.java');
const controller=read('backend/src/main/java/com/behsazan/corebanking/deposit/account/operations/web/DepositAccountOperationsController.java');
const client=read('frontend/src/app/features/four-deposits/deposit-account-operations.service.ts');
const html=read('frontend/src/app/features/four-deposits/deposit-account-operations.component.html');
const mig=read('database/oracle/dps2/migrations/0.11.0-phase11d-balance-subledger.sql');
const dbv=read('database/oracle/dps2/verification/0.11.0-phase11d-balance-subledger-verifier.sql');
const runtime=read('tools/runtime-dps2-phase11d-e2e.mjs');
const checks=[
 [read('VERSION').trim()==='0.11.0','VERSION remains 0.11.0'],
 [models.includes('record BalanceSnapshot')&&models.includes('record SubledgerEntry')&&models.includes('record BalanceReservation'),'11D balance domain models exist'],
 [models.includes('record PostEntryRequest')&&models.includes('record CreateReservationRequest')&&models.includes('record ReleaseReservationRequest'),'11D command contracts exist'],
 [repo.includes('DEPOSIT_ACCOUNT_BALANCE')&&repo.includes('DEPOSIT_SUBLEDGER_ENTRY')&&repo.includes('DEPOSIT_BALANCE_RESERVATION'),'Package 17 repository uses canonical tables'],
 [repo.includes('FOR UPDATE')&&repo.includes('findBalance(long accountId, boolean lock)'),'account/balance locking exists'],
 [repo.includes('UK_DEPOSIT_SUBLEDGER_ENTRY_POSTING_REFERENCE')===false&&repo.includes('POSTING_REFERENCE'),'posting persistence uses posting reference'],
 [repo.includes('activePartialHoldTotal')&&repo.includes("HOLD_TYPE_CODE='PARTIAL'"),'partial hold aggregation exists'],
 [repo.includes('activeDebitBlockHoldCount')&&repo.includes("IN ('FULL','DEBIT_ONLY')"),'debit-blocking hold aggregation exists'],
 [repo.includes('activeCreditBlockHoldCount')&&repo.includes("IN ('FULL','CREDIT_ONLY')"),'credit-blocking hold aggregation exists'],
 [repo.includes('activeReservationTotal')&&repo.includes("RESERVATION_STATUS_CODE='ACTIVE'"),'active reservation aggregation exists'],
 [repo.includes('lastEntryId == null ? \"\" : \"LAST_SUBLEDGER_ENTRY_ID=:lastEntryId,\"')&&repo.includes('if (lastEntryId != null) params.addValue(\"lastEntryId\", lastEntryId)')&&!repo.includes('LAST_SUBLEDGER_ENTRY_ID=COALESCE(:lastEntryId,LAST_SUBLEDGER_ENTRY_ID)'),'derived-balance refresh does not bind nullable last subledger id'],
 [service.includes('@Transactional\n    public PostEntryResponse post')||service.includes('@Transactional\r\n    public PostEntryResponse post'),'subledger posting is transactional'],
 [service.includes('repository.insertSubledger')&&service.includes('refreshLocked(accountId, balance, newLedger'),'subledger entry and ledger update share posting transaction'],
 [service.includes('"CREDIT".equals(dc) ? balance.ledgerBalance().add(amount) : balance.ledgerBalance().subtract(amount)'),'ledger changes only by debit/credit posting direction'],
 [service.includes('before.available().compareTo(amount) < 0'),'debit posting checks available balance'],
 [service.includes('activeDebitBlockHoldCount')&&service.includes('Debit به علت Hold فعال مجاز نیست'),'debit posting enforces hold'],
 [service.includes('activeCreditBlockHoldCount')&&service.includes('Credit به علت Hold فعال مجاز نیست'),'credit posting enforces hold'],
 [service.includes('replayOrClaim')&&service.includes('SUBLEDGER_POST'),'posting idempotency exists'],
 [service.includes('BALANCE_RESERVE')&&service.includes('BALANCE_RELEASE'),'reservation idempotency exists'],
 [service.includes('validateReservationOwner')&&service.includes('Reservation غیرتراکنشی'),'reservation owner/transaction exclusivity is enforced'],
 [service.includes('refreshBalance(long accountId')&&servicing.includes('balanceService.refreshBalance(accountId,actor)'),'Hold changes recalculate Package 17 balance'],
 [service.includes('debitBlocked ? BigDecimal.ZERO : ledger.subtract(partial).subtract(reservations).max(BigDecimal.ZERO)'),'available balance formula subtracts partial holds/reservations with zero floor'],
 [service.includes('debitBlocked ? ledger.max(BigDecimal.ZERO) : partial'),'blocked amount reflects FULL/DEBIT_ONLY vs PARTIAL hold'],
 [openingService.includes('balanceService.initializeOpeningBalance')&&!openingService.includes('accountRepository.updateBalances(account.accountId()'),'Opening settlement writes Package 17 instead of legacy account balance'],
 [openingService.includes('balanceService.currentBalance(account.accountId())'),'Opening readiness reads Package 17 balance'],
 [accountLifecycle.includes('balanceService.initializeAccount')&&accountLifecycle.includes('balanceService.currentBalance(row.accountId())'),'new account/lifecycle response use Package 17 balance'],
 [opsModels.includes('BalanceSnapshot balance')&&opsModels.includes('List<SubledgerEntry> subledgerEntries')&&opsModels.includes('List<BalanceReservation> reservations'),'Account 360 exposes Package 17 state'],
 [opsService.includes('balanceService.get(accountId)'),'Account Operations reads balance service'],
 [controller.includes('/{accountId}/balance')&&controller.includes('/balance/postings')&&controller.includes('/balance/reservations'),'11D APIs exist'],
 [controller.includes('@RequestHeader(name="X-Idempotency-Key")'),'11D mutation APIs require idempotency key'],
 [client.includes('DepositAccountBalance')&&client.includes('DepositSubledgerEntry')&&client.includes('DepositBalanceReservation'),'Angular Account 360 balance types exist'],
 [html.includes('Balance / Package 17')&&html.includes('Subledger')&&html.includes('Balance Reservations'),'Angular Account 360 shows Package 17'],
 [html.includes('Hold و Reservation فقط')&&html.includes('LEDGER_BALANCE'),'UI communicates ledger/availability boundary'],
 [mig.includes("widen_number('DEPOSIT_ACCOUNT_BALANCE','LEDGER_BALANCE')")&&mig.includes("widen_number('DEPOSIT_SUBLEDGER_ENTRY','AMOUNT')")&&mig.includes("widen_number('DEPOSIT_BALANCE_RESERVATION','AMOUNT')"),'11D migration reconciles 4-decimal precision'],
 [mig.indexOf('v_entries NUMBER:=0;')>mig.indexOf('DECLARE')&&mig.indexOf('v_entries NUMBER:=0;')<mig.indexOf('FUNCTION table_exists'),'11D migration declares scalar state before local PL/SQL subprograms'],
 [mig.includes("'BALANCE_MIGRATION'")&&mig.includes("'PH11D-MIG-'||A.ACCOUNT_ID"),'legacy monetary balance gets traceable migration subledger baseline'],
 [mig.includes('v_null_entry_base + ROW_NUMBER() OVER (ORDER BY A.ACCOUNT_ID)')&&mig.includes('v_null_entry_base + v_migration_entry_need > 999999'),'migration baseline allocates unique non-transactional entry sequence numbers'],
 [mig.includes('reconcile_tx_sequence_uniqueness')&&mig.includes('DROP CONSTRAINT UK_DEPOSIT_SUBLEDGER_ENTRY_TRANSACTION_ID_ENTRY_SEQUENCE_NO')&&mig.includes('UX_DEP_SUBLEDGER_TX_SEQ_NN'),'11D migration reconciles transaction-scoped entry sequence uniqueness'],
 [mig.includes('CASE WHEN TRANSACTION_ID IS NOT NULL THEN TRANSACTION_ID ELSE NULL END')&&mig.includes('CASE WHEN TRANSACTION_ID IS NOT NULL THEN ENTRY_SEQUENCE_NO ELSE NULL END'),'non-transactional postings are excluded from transaction sequence uniqueness'],
 [dbv.includes('legacy null-sensitive transaction/sequence constraint removed')&&dbv.includes('transaction-scoped unique index excludes NULL transaction postings')&&dbv.includes('non-null transaction entry sequence remains unique'),'DB verifier covers transaction-scoped sequence uniqueness'],
 [dbv.includes("DBMS_METADATA.GET_DDL('INDEX','UX_DEP_SUBLEDGER_TX_SEQ_NN','DPS2')")&&!dbv.includes("COLUMN_EXPRESSION LIKE '%TRANSACTION_ID%'"),'DB verifier avoids LONG comparison for function-based index expressions'],
 [mig.includes('INSERT INTO DPS2.DEPOSIT_ACCOUNT_BALANCE')&&mig.includes('NOT EXISTS (SELECT 1 FROM DPS2.DEPOSIT_ACCOUNT_BALANCE'),'balance backfill is idempotent'],
 [mig.includes('IX_DEP_SUBLEDGER_ACCOUNT_POSTED')&&mig.includes('IX_DEP_RESERV_ACCOUNT_STATUS'),'11D operational indexes are declared'],
 [mig.includes('FUNCTION index_columns_exist')&&mig.includes("LISTAGG(IC.COLUMN_NAME,',')")&&mig.includes("create_index_if_missing('DEPOSIT_SUBLEDGER_ENTRY','ACCOUNT_ID,POSTED_AT'")&&mig.includes("create_index_if_missing('DEPOSIT_BALANCE_RESERVATION','ACCOUNT_ID,RESERVATION_STATUS_CODE,EXPIRES_AT'"),'11D migration reuses equivalent existing index column lists'],
 [dbv.includes('PROCEDURE index_columns_ok')&&dbv.includes("index_columns_ok('DEPOSIT_SUBLEDGER_ENTRY','ACCOUNT_ID,POSTED_AT'")&&dbv.includes("index_columns_ok('DEPOSIT_BALANCE_RESERVATION','ACCOUNT_ID,RESERVATION_STATUS_CODE,EXPIRES_AT'"),'DB verifier accepts semantic index coverage instead of fixed index names'],
 [mig.includes('PHASE11D_BALANCE_SUBLEDGER_RECONCILIATION_PASS'),'11D migration emits reconciliation marker'],
 [dbv.includes('every deposit account has Package 17 balance row'),'DB verifier checks complete balance ownership migration'],
 [mig.includes('GREATEST(X.LEDGER_BALANCE-X.PARTIAL_HOLD-X.ACTIVE_RESERVATION,0)')&&dbv.includes('GREATEST(B.LEDGER_BALANCE'),'migration/verifier enforce non-negative available balance'],
 [dbv.includes('AVAILABLE_BALANCE formula is consistent')&&dbv.includes('PENDING_DEBIT_AMOUNT reflects active reservations'),'DB verifier checks balance mathematics'],
 [dbv.includes('legacy non-zero balances without prior Package 17 trace have migration subledger baseline')&&dbv.includes('migration subledger baseline agrees with cutover ledger balance'),'DB verifier checks migration traceability and cutover agreement'],
 [dbv.includes('PHASE11D_DB_BASELINE_PASS'),'DB verifier emits baseline marker'],
 [exists('tools/apply-dps2-phase11d.cmd'),'Windows 11D apply helper exists'],
 [exists('tools/runtime-dps2-phase11d-e2e.mjs')&&runtime.includes('PHASE11D_RUNTIME_E2E_PASS'),'11D runtime E2E harness exists'],
 [runtime.includes('idempotentReplay===true')&&runtime.includes('Reservation changed ledger balance'),'runtime E2E verifies idempotency and reservation/ledger separation'],
 [runtime.includes('PARTIAL')&&runtime.includes('Partial hold changed ledger balance')&&runtime.includes('Partial hold did not reduce available balance'),'runtime E2E verifies partial Hold changes availability but not ledger'],
 [exists('docs/DPS2-0.11.0-PHASE11D-BALANCE-SUBLEDGER-QA.md'),'11D DPS2 QA document exists']
];
let pass=0,fail=0;for(const [ok,msg] of checks){if(ok){pass++;console.log(`PASS | ${msg}`)}else{fail++;console.error(`FAIL | ${msg}`)}}
console.log('------------------------------------------------------------');console.log(`PHASE11D_STATIC_VERIFIER_PASS=${pass}`);console.log(`PHASE11D_STATIC_VERIFIER_FAIL=${fail}`);if(fail)process.exit(1);console.log('PHASE11D_STATIC_BASELINE_PASS');
