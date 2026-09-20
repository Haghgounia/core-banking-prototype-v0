# DPS2 0.10.0 Phase 10F - FUND_ALLOC Column Reconciliation

## Runtime finding
Settlement reached `DepositOpeningOperationalRepository.insertAllocation()` and Oracle returned:

`ORA-00904: "ALLOCATED_AMOUNT": invalid identifier`

The Phase 10 v5 source contract defines `DPS2.DEPOSIT_OPENING_FUND_ALLOC.ALLOCATED_AMOUNT NUMBER(19,4) NOT NULL`. The Phase 10 foundation only created the table when absent, so a legacy pre-existing table could survive without the newer column set.

## Fix
`0.10.0-phase10f-fund-alloc-column-reconciliation.sql` reconciles all 11 canonical columns of `DEPOSIT_OPENING_FUND_ALLOC`, not only `ALLOCATED_AMOUNT`.

For missing required columns without a safe default, the migration only adds them when the table is empty. If legacy rows exist, it aborts instead of inventing business values.

No business rows are inserted, updated, or deleted.
