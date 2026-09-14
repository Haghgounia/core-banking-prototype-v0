# Core Banking Prototype 0.4.0 — Phase 4 Release Notes

## Deposit Account Creation & Activation

Phase 4 adds the operational account lifecycle after an approved Deposit Opening request while preserving the bounded-context boundary between Origination and Account Operations.

### APIs

- `POST /api/v1/deposit-opening/requests/{id}/account`
- `POST /api/v1/deposit-opening/requests/{id}/account/activate`
- `GET /api/v1/deposit-opening/requests/{id}/account`

### Lifecycle

- Opening must be `APPROVED` before account creation.
- Account creation is idempotent and protected by `SELECT ... FOR UPDATE` on the Opening root.
- New account state is `PENDING_ACTIVATION`.
- Activation is accepted only from `PENDING_ACTIVATION` and changes the account to `ACTIVE`.
- Successful activation changes Opening from `APPROVED` to `COMPLETED` and records status history.
- `CREATE` and `ACTIVATE` lifecycle events are recorded.

### Architecture

`DEPOSIT_OPENING_REQUEST.CREATED_ACCOUNT_ID` is an Integration Reference only. No physical cross-domain FK is created from Deposit Opening to `DEPOSIT_ACCOUNT`.

`DPA...` values are Technical Prototype Account Numbers only and are not a production Bank Mellat account-numbering algorithm.

### Database

Migration:

`database/oracle/dps2/migrations/0.4.0-phase4-deposit-account-lifecycle.sql`

### Release qualification status

Static regression and patch-overlay verification are PASS. Production Maven/Angular build is not verified in the current isolated environment because Maven Central and NPM Registry are unreachable. Therefore the generated ZIPs are QA Candidate source packages, not a production-build-qualified final release.
