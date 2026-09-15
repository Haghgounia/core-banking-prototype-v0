# QA Report — 0.6.0

Phase 6 implements the supplied `Deposit_Account_Opening.html` Batch / Bulk Deposit Opening flow on top of the build-qualified 0.5.2 baseline.

## Static regression gate
- 37/37 `tools/verify-*.mjs` scripts PASS.
- Phase 1–5 DPS2 verifiers remain PASS.
- Phase 6 verifier PASS: supplied-HTML Batch Header/Item/Error flow, isolated item processing, Opening/Account linkage, activation and dedicated UI/API.
- Node verifier confirms 36 other verifier scripts are cwd-independent and Windows-safe.

## Phase 6 checks
- Existing DPS2 Batch tables are reused; no operational table is recreated.
- Source-defined Batch/Item/Source/Error-Stage reference codes are seeded idempotently.
- Each valid row creates one independent `DEPOSIT_OPENING_REQUEST` with `REQUEST_TYPE_CODE=BULK` and `BATCH_ITEM_ID` traceability.
- Each valid row is processed in a `REQUIRES_NEW` transaction, so one failed row does not rollback successful rows.
- Existing `DepositAccountLifecycleService` is reused for account creation and activation.
- Account creation yields `PENDING_ACTIVATION`; activation is a separate operation.
- Partial Batch revalidation keeps successful rows untouched and allows transient failed rows to return to `VALID` before a retry.

## Build status in packaging environment
The Maven wrapper could not reach Maven Central to download Maven 3.9.16, so a Java/Angular production compile could not be completed in this environment. No compiler error was reached. The target Windows environment previously built 0.5.2 successfully and should run `build-production.cmd` as the release gate for 0.6.0.

## Oracle runtime gate
Execute `database/oracle/dps2/migrations/0.6.0-phase6-batch-opening.sql` before running the Phase 6 workflow. The migration is rerunnable and contains reference-data DML only.
