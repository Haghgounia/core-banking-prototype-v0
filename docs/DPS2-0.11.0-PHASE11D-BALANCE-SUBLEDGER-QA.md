# DPS2 0.11.0 — Phase 11D Balance + Subledger QA

## Scope
Only Four Deposits / DPS2.

Phase 11D makes Package 17 the operational monetary source of truth:

- `DEPOSIT_ACCOUNT_BALANCE`
- `DEPOSIT_SUBLEDGER_ENTRY`
- `DEPOSIT_BALANCE_RESERVATION`
- integration with `DEPOSIT_ACCOUNT_HOLD`
- idempotency through `DEPOSIT_OPERATION_IDEMPOTENCY`

No CIF, CAL/CAL2, Fee, PDL, Reference Data, or shared application feature is changed by this phase.

## Contracts

1. `LEDGER_BALANCE` changes only with an atomic Subledger posting.
2. `PARTIAL` Hold contributes to `BLOCKED_AMOUNT` and reduces `AVAILABLE_BALANCE`; available balance has a zero floor and never becomes negative.
3. `FULL` and `DEBIT_ONLY` Hold force debit availability to zero without changing Ledger.
4. Active Balance Reservation contributes to `PENDING_DEBIT_AMOUNT` and reduces Available without changing Ledger.
5. `CREDIT_ONLY` / `FULL` hold blocks credit posting; `DEBIT_ONLY` / `FULL` blocks debit posting.
6. Every mutation is idempotent.
7. New Deposit Opening settlement creates the opening credit in Package 17; legacy `DEPOSIT_ACCOUNT.LEDGER_BALANCE/AVAILABLE_BALANCE` remain compatibility fields only.
8. Existing legacy balances are reconciled into Package 17 with `BALANCE_MIGRATION` Subledger trace rows where non-zero.
9. Package 17 monetary columns are widened to 4 decimal places to align with four-deposit transaction/opening precision.

## Qualification

Run:

```bat
set CORE_BANKING_ORACLE_CONNECT=SYSTEM/Oracle123@//10.1.60.51:1522/FREEPDB1
tools\apply-dps2-phase11d.cmd
```

Expected DB markers:

```text
PHASE11D_STATIC_BASELINE_PASS
PHASE11D_BALANCE_SUBLEDGER_RECONCILIATION_PASS
PHASE11D_DB_VERIFIER_FAIL=0
PHASE11D_DB_BASELINE_PASS
PHASE11D_IMPLEMENTATION_PASS
```

After building and starting the application:

```bat
set CORE_BANKING_BASE_URL=http://127.0.0.1:8091
node tools\runtime-dps2-phase11d-e2e.mjs
```

Expected runtime marker:

```text
PHASE11D_RUNTIME_E2E_PASS
```

The runtime probe performs a net-zero credit/debit pair and verifies both PARTIAL Hold create/release and reservation create/release. Hold/Reservation must change availability without changing ledger; final ledger and available balance return to their initial values while traceable Subledger/Hold/Reservation rows remain.

## Source qualification status

- Phase 11D static verifier: `47/47 PASS`.
- All existing `tools/verify-dps2*.mjs` regression guards pass on the Phase 11D source.
- Phase 9 and Phase 11C verifier expectations were updated only to recognize the evolved Four Deposits UI wording; their business controls were not weakened.
- The 11D migration also reconciles pre-existing Package 17 balance rows that have no prior Subledger trace, and recomputes derived Available/Blocked/Pending values at cutover.
- Full Maven/Angular compilation is intentionally left to the target Windows environment because this workspace has neither Maven nor frontend `node_modules` installed.

Status: **SOURCE QUALIFIED / TARGET DB + RUNTIME VERIFICATION REQUIRED**. Phase 11D is not CLOSED until the target markers above pass.
