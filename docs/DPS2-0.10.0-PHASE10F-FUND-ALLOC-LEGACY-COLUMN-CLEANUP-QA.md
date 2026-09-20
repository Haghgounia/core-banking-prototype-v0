# DPS2 0.10.0 Phase 10F - FUND_ALLOC Legacy Column Cleanup

## Problem
A pre-Phase-10 `DPS2.DEPOSIT_OPENING_FUND_ALLOC` table retained legacy `ALLOCATION_AMOUNT NUMBER(19,2) NOT NULL`, while the Phase 10 v5 runtime contract uses `ALLOCATED_AMOUNT NUMBER(19,4) NOT NULL`.

## Source audit
DPS2 application/domain/repository code uses `ALLOCATED_AMOUNT`. `ALLOCATION_AMOUNT` occurrences are confined to the independent FEE module and are not part of DPS2 opening settlement.

## Migration behavior
- Requires canonical `ALLOCATED_AMOUNT` to exist.
- If legacy `ALLOCATION_AMOUNT` is absent: no-op.
- If legacy column exists and the table is empty: drops the legacy column.
- If business rows exist: aborts instead of risking data loss.

## Expected marker
`SUCCESS: Phase 10F FUND_ALLOC legacy column cleaned up.`
