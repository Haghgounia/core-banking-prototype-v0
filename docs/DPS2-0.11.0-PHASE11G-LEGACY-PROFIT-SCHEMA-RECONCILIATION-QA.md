# DPS2 0.11.0 - Phase 11G Legacy Profit Schema Reconciliation QA

## Issue

The Oracle environment already contained an earlier `DPS2.DEPOSIT_PROFIT_ACCRUAL` physical model. The idempotent Phase 11G `CREATE TABLE` correctly ignored ORA-00955, but subsequent DDL failed first on `PROFIT_POSTING_ID` and then on `ACCRUAL_TO_DATE` because the existing table did not match the current Repository contract.

## Resolution

The Phase 11G migration now performs repository-contract schema reconciliation before indexes, foreign keys, backfill, or runtime query usage.

It checks all columns currently consumed by:

- `DEPOSIT_PROFIT_CONTRACT`
- `DEPOSIT_PROFIT_ACCRUAL`
- `DEPOSIT_PROFIT_POSTING`

Missing optional trace/reference columns are added idempotently. Missing required financial columns are added automatically only when the legacy table is empty. If a legacy table contains rows and a required financial column is absent, migration fails with an explicit manual-mapping diagnostic rather than fabricating financial history.

The database verifier now also checks evolved repository columns such as `ACCRUAL_TO_DATE`, `LAST_PAYMENT_DATE`, and `SUBLEDGER_ENTRY_ID` before data-integrity queries.

## Expected rerun

`tools\\apply-dps2-phase11g.cmd` may be rerun safely. Existing columns are reported as `already exists`; missing columns on empty prototype tables are reported as `added`.

## Static qualification

- `PHASE11G_STATIC_VERIFIER_PASS=77`
- `PHASE11G_STATIC_VERIFIER_FAIL=0`
- `PHASE11G_STATIC_BASELINE_PASS`
