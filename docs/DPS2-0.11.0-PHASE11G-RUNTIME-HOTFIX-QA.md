# DPS2 0.11.0 - Phase 11G Runtime Hotfix QA

Date: 2026-09-23

## Observed runtime failures

1. `GET /api/v1/deposit-accounts/{id}/profit` returned HTTP 500 for existing active term accounts.
2. The Phase 11G bootstrap opening request failed validation because `DEPOSIT_OPENING_MATURITY_INSTRUCTION.INSTRUCTION_SOURCE_CODE=PRODUCT` is not an active governed reference value.

## Root cause and corrections

- `REF_DEP_OPEN_INSTRUCTION_SOURCE` seeds `CUSTOMER` and `PRODUCT_DEFAULT`; the runtime bootstrap now uses `PRODUCT_DEFAULT` for a maturity instruction derived from the governed product term rule.
- A 500 response from the `/profit` read endpoint is treated as a profit-storage/schema readiness failure instead of an ordinary account-selection miss. The runtime E2E now fails fast with `PHASE11G_PROFIT_STORAGE_NOT_READY_OR_SCHEMA_MISMATCH` and instructs the operator to run `tools/apply-dps2-phase11g.cmd` and require `PHASE11G_DB_BASELINE_PASS`.
- The Phase 11G Oracle migration is rerunnable: table/sequence creation tolerates existing objects, FK/index creation is guarded, and backfill excludes accounts that already have an active profit contract.

## Static qualification

`node tools/verify-dps2-profit-processing-11g.mjs`

Expected result after this hotfix:

- `PHASE11G_STATIC_VERIFIER_PASS=74`
- `PHASE11G_STATIC_VERIFIER_FAIL=0`
- `PHASE11G_STATIC_BASELINE_PASS`

## Required runtime qualification order

1. Run `tools/apply-dps2-phase11g.cmd` with the configured Oracle connection.
2. Require `PHASE11G_DB_BASELINE_PASS` and `PHASE11G_IMPLEMENTATION_PASS`.
3. Run `node tools/runtime-dps2-phase11g-e2e.mjs`.
4. Require `PHASE11G_RUNTIME_E2E_PASS`.
