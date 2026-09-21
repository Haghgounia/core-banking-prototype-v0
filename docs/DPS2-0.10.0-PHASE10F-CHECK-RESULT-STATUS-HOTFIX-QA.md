# DPS2 0.10.0 Phase 10F - Check Result Status Reconciliation

## Runtime symptom
Activation Readiness failed with:

`ORA-02290: check constraint (DPS2.CHK_DEPOSIT_OPENING_CHECK_RESULT_STATUS_CODE) violated`

## Root cause
The Phase 10 operational readiness service and the reference-data seed use the canonical result-status vocabulary:

- `PASS`
- `FAIL`
- `PENDING`
- `WAIVED`
- `NOT_APPLICABLE`

The Oracle `DEPOSIT_OPENING_CHECK` table retained an older check constraint that did not match that contract.

## Repair
`0.10.0-phase10f-check-result-status-reconciliation.sql`:

1. preflights existing rows and refuses to change the constraint if non-canonical business values exist;
2. drops the obsolete named check constraint when present;
3. recreates it with the canonical five-value contract;
4. verifies the constraint, reference catalog and business rows.

No business row is inserted, updated or deleted.
