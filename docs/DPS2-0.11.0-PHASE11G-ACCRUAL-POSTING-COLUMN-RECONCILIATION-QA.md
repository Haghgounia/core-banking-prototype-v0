# DPS2 0.11.0 - Phase 11G Accrual Posting Column Reconciliation

## Problem
An earlier Phase 11G physical model can already contain `DPS2.DEPOSIT_PROFIT_ACCRUAL` without the later `PROFIT_POSTING_ID` traceability column. Because the base Phase 11G migration treats `ORA-00955` as idempotent table-exists success, the table is not recreated and the later FK creation fails with `ORA-00904: \"PROFIT_POSTING_ID\": invalid identifier`.

## Resolution
The Phase 11G migration now reconciles the existing table before creating `FK_DEP_PROFIT_ACCR_POST`:

- inspect `ALL_TAB_COLUMNS` for `DPS2.DEPOSIT_PROFIT_ACCRUAL.PROFIT_POSTING_ID`;
- add `PROFIT_POSTING_ID NUMBER(19)` only when missing;
- create the accrual-to-posting FK afterwards;
- preserve idempotency on repeated executions.

The Phase 11G DB verifier also checks that the traceability column exists.

## Static qualification

`PHASE11G_STATIC_VERIFIER_PASS=75`

`PHASE11G_STATIC_VERIFIER_FAIL=0`

`PHASE11G_STATIC_BASELINE_PASS`

## Runtime deployment impact
No Java or Angular source is changed by this hotfix. Rebuild and application restart are not required. Re-run `tools\\apply-dps2-phase11g.cmd`, then run the Phase 11G runtime E2E gate.
