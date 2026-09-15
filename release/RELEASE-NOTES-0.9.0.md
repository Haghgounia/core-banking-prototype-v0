# Release Notes — 0.9.0

## Four Deposits Phase 9 — Deposit Account Servicing

This release moves Account Operations from read-only Foundation to the first controlled mutating operation supported by the existing DPS2 physical contract.

### Added
- `POST /api/v1/deposit-accounts/{accountId}/close`
- `DepositAccountServicingService`
- `DepositAccountServicingRepository`
- optimistic concurrency with `RECORD_VERSION`
- row lock with `FOR UPDATE`
- `CLOSE` lifecycle event
- lifecycle append-only trigger
- Account Operations Close action
- Phase 9 static verifier and QA documentation

### Model boundary
The existing account model supports `CLOSED` but does not model Hold/Blocked/Dormant/Reactivated states. Those capabilities remain out of scope until the corresponding canonical/physical model is supplied or approved.

### Database
Run:
`database/oracle/dps2/migrations/0.9.0-phase9-account-closure-servicing.sql`
