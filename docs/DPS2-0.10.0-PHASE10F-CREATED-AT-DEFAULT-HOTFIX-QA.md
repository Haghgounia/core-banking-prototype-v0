# DPS2 0.10.0 Phase 10F — CREATED_AT Default Reconciliation

## Runtime finding

The four-family Phase 10F fixture passed validation, but the first aggregate persistence failed with:

`ORA-01400: cannot insert NULL into ... CREATED_AT`

`DepositOpeningAggregateRepository` deliberately supplies `CREATED_BY` but not `CREATED_AT` for the opening aggregate. The repository contract therefore depends on Oracle audit columns having `DEFAULT SYSTIMESTAMP`.

## Root cause

At least one installed baseline table (`DPS2.DEPOSIT_OPENING_REQUEST`, the first table persisted by the aggregate service) has `CREATED_AT` as mandatory without an effective default in the live database.

## Fix

Migration `0.10.0-phase10f-created-at-default-reconciliation.sql` reconciles every physical `DPS2.DEPOSIT_OPENING_%` table containing `CREATED_AT` to:

`CREATED_AT DEFAULT SYSTIMESTAMP`

The migration is idempotent, does not rewrite existing rows, and does not change nullability or data types.

## Why database reconciliation instead of 25 Java INSERT edits

The dedicated opening repositories consistently rely on the database-managed audit timestamp contract. Re-establishing that contract at the schema boundary is smaller, safer and prevents the same failure from recurring on subsequent child-table inserts.

## Runtime qualification continuation

After the migration succeeds, no Java rebuild or application restart is required. Regenerate the fixture and rerun the Phase 10F E2E harness.
