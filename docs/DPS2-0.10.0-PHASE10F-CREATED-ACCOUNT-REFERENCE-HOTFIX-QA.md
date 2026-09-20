# DPS2 0.10.0 Phase 10F - Created Account Reference Reconciliation

## Root cause

The runtime account-creation flow persists only `DEPOSIT_OPENING_REQUEST.CREATED_ACCOUNT_ID` as the cross-boundary integration reference. `CREATED_ACCOUNT_NO` is intentionally not persisted because the account number is resolved from `DPS2.DEPOSIT_ACCOUNT` using the account id.

The live Oracle schema still contained two older checks that required `CREATED_ACCOUNT_ID` and `CREATED_ACCOUNT_NO` to be null/non-null together. Account creation therefore failed with `ORA-02290` immediately after the account row was created and the opening row attempted to store `CREATED_ACCOUNT_ID`.

## Reconciliation

The migration:

1. Drops `CHK_DEPOSIT_OPENING_REQUEST_CREATED_ACCOUNT_ID_CREATED_ACCOUNT_NO` if present.
2. Drops `CHK_DEPOSIT_OPENING_REQUEST_REQUEST_STATUS_CODE_CREATED_ACCOUNT_ID_CREATED_ACCOUNT_NO` if present.
3. Adds the final completion rule:
   `REQUEST_STATUS_CODE <> 'COMPLETED' OR CREATED_ACCOUNT_ID IS NOT NULL`.
4. Does not populate or modify `CREATED_ACCOUNT_NO`.
5. Does not modify business rows.

## Expected runtime result

After applying the migration, Phase 10F account creation may persist `CREATED_ACCOUNT_ID` without duplicating `ACCOUNT_NO` on the opening aggregate. Activation to `COMPLETED` remains protected by the presence of `CREATED_ACCOUNT_ID`.
