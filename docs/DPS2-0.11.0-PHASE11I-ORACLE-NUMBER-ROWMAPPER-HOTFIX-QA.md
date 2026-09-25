# DPS2 0.11.0 — Phase 11I Oracle NUMBER RowMapper Hotfix QA

Date: 2026-09-24
Status: SOURCE_FIXED / RUNTIME_REQUALIFICATION_REQUIRED

## Symptom

`POST /api/v1/deposit-accounts/{id}/transactions` returned HTTP 500 even after Step 05 DB Gate passed 27/27 and all eight Step 05 sequence high-water checks passed.

## Root cause

`DepositTransactionRepository` directly cast nullable Oracle `NUMBER` values using `(Long) ResultSet.getObject(...)`. Oracle JDBC may return `BigDecimal` for `NUMBER`; the first cash-deposit initiate inserted transaction/history/leg/cash-detail data and then called `view()`. Mapping `DEPOSIT_TRANSACTION_LEG.ACCOUNT_ID` could throw `ClassCastException`, causing the enclosing Spring transaction to roll back.

## Fix

Use JDBC typed extraction `ResultSet.getObject(index, Long.class)` consistently for nullable numeric columns in Step 05 repository mappings. Covered mappings include idempotency `ACCOUNT_ID`, original transaction IDs, account-limit `COUNT_LIMIT`, leg `ACCOUNT_ID`, linked subledger IDs, and reversal approval IDs.

## Qualification

```bat
node tools\verify-dps2-step05-transaction-processing-11i.mjs
bin\stop.cmd
build-production.cmd
bin\start.cmd
set CORE_BANKING_BASE_URL=http://127.0.0.1:8091
node tools\runtime-dps2-phase11i-e2e.mjs
```

Expected static gate:

```text
PHASE11I_STATIC_VERIFIER_PASS=60
PHASE11I_STATIC_VERIFIER_FAIL=0
PHASE11I_STATIC_BASELINE_PASS
```

No Oracle migration is required for this source-only hotfix; DB Gate 27/27 remains valid. Phase 11I closes only after production build and `PHASE11I_RUNTIME_E2E_PASS`.
