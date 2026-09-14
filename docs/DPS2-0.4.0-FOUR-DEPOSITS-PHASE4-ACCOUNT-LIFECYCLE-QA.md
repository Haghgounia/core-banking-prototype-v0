# DPS2 0.4.0 — Four Deposits Phase 4 / Account Creation & Activation QA

## Scope

Phase 4 closes the operational boundary after an approved Deposit Opening request by adding real account creation and activation operations. The approved Opening aggregate remains an Origination record; account lifecycle behavior is implemented in the separate `deposit.account` application/domain package.

## API contract

- `POST /api/v1/deposit-opening/requests/{id}/account`
- `POST /api/v1/deposit-opening/requests/{id}/account/activate`
- `GET /api/v1/deposit-opening/requests/{id}/account`

The create operation is idempotent. Repeating account creation for an Opening that already has `CREATED_ACCOUNT_ID` returns the existing account instead of inserting another one.

## Transaction and concurrency contract

Account creation locks the Opening root row with Oracle `SELECT ... FOR UPDATE` before checking `CREATED_ACCOUNT_ID`. This serializes competing account-creation requests for the same Opening and prevents duplicate account creation at the application transaction boundary. The account table also has a unique constraint on `OPENING_REQUEST_ID` as a database-level safeguard.

Activation locks the Opening and the linked account. It only accepts an account in `PENDING_ACTIVATION` state.

## Lifecycle

The cross-context operational sequence is:

1. Opening is already `APPROVED`.
2. Account creation inserts `DEPOSIT_ACCOUNT` with status `PENDING_ACTIVATION`.
3. `DEPOSIT_OPENING_REQUEST.CREATED_ACCOUNT_ID` is written as the integration reference.
4. `DEPOSIT_ACCOUNT_LIFECYCLE_EVENT` records `CREATE`.
5. Activation changes the account `PENDING_ACTIVATION -> ACTIVE`.
6. `DEPOSIT_ACCOUNT_LIFECYCLE_EVENT` records `ACTIVATE`.
7. Opening changes `APPROVED -> COMPLETED`.
8. `DEPOSIT_OPENING_STATUS_HISTORY` records the completion transition.

A second Activation attempt is rejected because Activation is valid only from `PENDING_ACTIVATION`.

## Bounded-context boundary

`DEPOSIT_OPENING_REQUEST.CREATED_ACCOUNT_ID` is an Integration Key only. The Phase 4 migration intentionally does **not** create a physical FK from Deposit Opening to `DEPOSIT_ACCOUNT`. This preserves the boundary between Origination and Account Operations. Within Account Operations, `DEPOSIT_ACCOUNT_LIFECYCLE_EVENT.ACCOUNT_ID` may use a physical FK to `DEPOSIT_ACCOUNT` because both are inside the same account lifecycle contract.

After Opening reaches `COMPLETED`, material account changes belong to Account Operations. The Opening aggregate remains immutable historical origination evidence except for explicitly governed audit/integration fields.

## Oracle migration

`database/oracle/dps2/migrations/0.4.0-phase4-deposit-account-lifecycle.sql`

The migration provides:

- `CREATED_ACCOUNT_ID` compatibility add when the supplied Opening DDL does not already contain the column.
- `SEQ_DEPOSIT_ACCOUNT`
- `SEQ_DEP_ACCOUNT_LIFECYCLE_EVT`
- `DEPOSIT_ACCOUNT`
- `DEPOSIT_ACCOUNT_LIFECYCLE_EVENT`
- account/event constraints and indexes
- explanatory comments for the cross-domain integration reference

`DPA...` account numbers are explicitly **Technical Prototype Numbers**. They are not a Bank Mellat production numbering algorithm.

## Angular Step 7

Step 7 is no longer payload-only. It is an explicit three-stage workflow:

1. ثبت Opening
2. ایجاد حساب
3. فعال‌سازی حساب

The UI exposes returned `OPENING_REQUEST_ID`, `ACCOUNT_ID`, account number and current account status. It also provides a GET refresh operation for the account state.

## Static verification

Run:

```text
node tools/verify-dps2-four-deposits.mjs
node tools/verify-dps2-deposit-opening-persistence.mjs
node tools/verify-dps2-deposit-opening-phase3.mjs
node tools/verify-dps2-deposit-account-phase4.mjs
```

## Production build verification

Release qualification additionally requires:

- backend Maven compile/package
- Angular production build
- complete `verify-*.mjs` regression suite
- patch overlay comparison against 0.3.99 baseline
- Full/Patch package content verification

If dependency repositories are unavailable in the build environment, Maven/Angular build status must remain explicitly `NOT VERIFIED`; static verification must not be represented as a production build PASS.
