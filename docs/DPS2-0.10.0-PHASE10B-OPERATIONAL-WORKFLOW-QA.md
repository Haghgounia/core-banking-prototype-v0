# DPS2 0.10.0 — Phase 10B/10C Operational Opening v5 Backend & Workflow QA

## Scope
Alignment of Deposit Opening runtime with `Deposit_Account_Opening_Operational_Final_2026-09-17.html` after the Phase 10A schema foundation.

## Implemented contract
- Opening aggregate now persists `DEPOSIT_OPENING_FUNDING` as `0..N` rows.
- Added persistence for `DEPOSIT_OPENING_OBLIGATION` and `DEPOSIT_OPENING_FUND_ALLOC`.
- Opening checks persist phase, blocking scope, required/recheck flags, validity and source-evaluation metadata.
- Approved Opening no longer requires Funding to be `SUCCESS` before Account creation; it requires a complete Funding Plan and financial obligations.
- Create Account rechecks the Account-Creation gate and financial plan immediately before creating the account.
- Account is created only as `PENDING_ACTIVATION`; Opening activation status becomes `PENDING_READINESS`.
- Settlement is a separate operation and records confirmed settlement evidence; it allocates Funding rows to Obligation rows and updates opening balances.
- Activation Readiness is a separate operation. Mandatory `ACCOUNT_ACTIVATION` checks must resolve to `PASS`, `WAIVED` or `NOT_APPLICABLE` before status becomes `READY`.
- Expired PASS evidence is rejected as `FAIL` during readiness evaluation.
- Activate rejects any Opening whose `ACTIVATION_STATUS_CODE` is not `READY`.
- Debit-capability blockers are independent of Account lifecycle and can produce `BLOCKED_BY_RESTRICTION` on an ACTIVE account.
- Direct batch processing / activation is disabled for Operational v5 until each row owns an independent Opening and its own CDD/Funding/Readiness gates.

## Runtime endpoints
- `POST /api/v1/deposit-opening/requests/{id}/account`
- `POST /api/v1/deposit-opening/requests/{id}/account/settlement`
- `POST /api/v1/deposit-opening/requests/{id}/account/readiness`
- `GET  /api/v1/deposit-opening/requests/{id}/account/readiness`
- `POST /api/v1/deposit-opening/requests/{id}/account/activate`
- `GET  /api/v1/deposit-opening/requests/{id}/account`

The legacy `/account/settle` alias is temporarily accepted by the controller; `/account/settlement` is the canonical contract.

## Schema runtime delta
`0.10.0-phase10b-opening-operational-runtime-alignment.sql` changes only `DEPOSIT_OPENING_FUNDING.ATTEMPT_AT` to nullable. A funding row is a plan before the first real settlement attempt, therefore an attempt timestamp must not be fabricated during aggregate creation.

## Static QA
- Phase 10A schema verifier: 34/34 PASS.
- Phase 10B backend/workflow verifier: 31/31 PASS.
- TypeScript parse check: no syntax/type errors were observed beyond unavailable Angular/RxJS packages in the isolated packaging environment.
- Maven compile cannot run in the packaging environment because Maven Wrapper 3.9.16 cannot be downloaded; Windows build remains the compile/runtime gate.

## Oracle qualification
- Phase 10A foundation: PASS on target Oracle.
- Phase 10A semantic constraint repair: PASS; all 10 expected FKs are ENABLED.
- Phase 10B runtime alignment: pending target execution.

## No fabricated integrations
The backend does not claim to call SIAH, Tax, Fee, Compliance or external Ledger services. Readiness accepts explicit integration evidence/reference values and persists their status. Settlement requires a caller-provided settlement reference; production adapters remain a later integration task.
