# DPS2 0.7.0 — Phase 7 E2E Integration & Runtime Hardening QA

## Scope
Phase 7 hardens the existing Deposit Account Opening bounded context defined by the supplied HTML. It does not add Servicing/Account Operations business functions.

## Runtime contracts
- Single Opening validates active DPS2 reference codes before write.
- Every Party referenced by the aggregate must exist in `CIF.PARTY`.
- `PRODUCT_VERSION_ID` must resolve through `PDL.PRODUCT_VERSION` + `PDL.PRODUCT`, belong to Product Class `DEPOSIT`, and remain inside the four supported families.
- Product validity/origination/record status and currency are checked when available.
- Term/Pricing/Profit-payment references are checked against the selected Product Version when IDs are supplied.
- Batch processing validates its fixed normalized Opening codes and selected opening channel against the same DPS2 references.

## Idempotency/concurrency
Migration `database/oracle/dps2/migrations/0.7.0-phase7-opening-e2e-hardening.sql` adds database-level uniqueness guards for:
- `DEPOSIT_OPENING_REQUEST.IDEMPOTENCY_KEY`
- `DEPOSIT_OPENING_BATCH.IDEMPOTENCY_KEY`
- `(DEPOSIT_OPENING_BATCH_ITEM.OPENING_BATCH_ID, EXTERNAL_ROW_KEY)`

The migration aborts with explicit `-20701/-20702/-20703` errors if legacy duplicates exist. Single and Batch create paths handle concurrent duplicate-key races as idempotent replay.

## Readiness API
- `GET /api/v1/deposit-opening/readiness`
- `POST /api/v1/deposit-opening/readiness/rollback-probe`
- `POST /api/v1/deposit-opening/requests/validate`

The rollback probe inserts a temporary Batch row inside an isolated transaction, verifies it is visible, marks the transaction rollback-only, then verifies that no row remains.

## UI
- New `/four-deposits/runtime-readiness` diagnostics page.
- Step 5 of the seven-step Opening wizard includes explicit `Oracle / CIF / PDL` runtime preflight.
- Account Service and Tax Profile/Withholding remain external HTML contracts and are shown as WARN rather than simulated as integrated services.

## Gate
Run `node tools/verify-dps2-deposit-opening-phase7.mjs` and the complete production build. Runtime qualification additionally requires executing the 0.7.0 migration on the target Oracle and running Readiness/Rollback Probe there.
