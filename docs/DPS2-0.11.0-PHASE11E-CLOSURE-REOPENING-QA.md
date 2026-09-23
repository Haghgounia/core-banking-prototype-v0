# DPS2 0.11.0 — Phase 11E Controlled Closure + Reopening

Scope is strictly Four Deposits / DPS2.

## Contract
- Direct generic `/close` endpoint is removed.
- Closure is `REQUESTED -> APPROVED -> EXECUTED` with maker/checker approval.
- Pre-closure checks persist account status, active Hold, active Reservation, ledger validity and settlement readiness.
- Active Hold or active Reservation blocks execution even if state changed after request.
- Positive ledger balance is settled only through the Phase 11D subledger posting primitive.
- Final `CLOSED` transition requires ledger, available, blocked and pending-debit balances to be zero.
- Reopening is `REQUESTED -> APPROVED -> EXECUTED` and only `CLOSED -> ACTIVE`.
- CLOSE and REOPEN write lifecycle event and status history.
- All mutating workflow APIs are idempotent.

## Gates
- `PHASE11E_STATIC_BASELINE_PASS`
- `PHASE11E_SCHEMA_RUNTIME_READINESS_PASS`
- `PHASE11E_DB_BASELINE_PASS`
- `PHASE11E_IMPLEMENTATION_PASS`
- `PHASE11E_RUNTIME_E2E_PASS`
