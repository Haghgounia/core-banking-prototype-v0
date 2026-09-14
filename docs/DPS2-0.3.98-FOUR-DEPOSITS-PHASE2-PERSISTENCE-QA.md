# DPS2 0.3.98 — Four Deposits Phase 2 / Transactional Persistence QA

## Scope

Phase 2 converts the seven-step Deposit Account Opening wizard from a payload-only prototype to an Oracle-backed aggregate write endpoint.

### API

`POST /api/v1/deposit-opening/requests`

Headers:

- `X-User-Id`: actor written to `CREATED_BY` / decision metadata.
- `X-Correlation-Id`: correlation identifier used by system-managed status history.

### Transaction boundary

The following objects are written in one Spring `@Transactional` boundary when present in the request:

1. `DPS2.DEPOSIT_OPENING_REQUEST`
2. `DPS2.DEPOSIT_OPENING_PARTY`
3. `DPS2.DEPOSIT_OPENING_SIGNATURE_RULE`
4. `DPS2.DEPOSIT_OPENING_TERM`
5. `DPS2.DEPOSIT_OPENING_MATURITY_INSTRUCTION`
6. `DPS2.DEPOSIT_OPENING_PROFIT_INSTRUCTION`
7. `DPS2.DEPOSIT_OPENING_WITHDRAWAL_MEDIA`
8. `DPS2.DEPOSIT_OPENING_SERVICE_SELECTION`
9. `DPS2.DEPOSIT_OPENING_FUNDING`
10. `DPS2.DEPOSIT_OPENING_CHECK`
11. `DPS2.DEPOSIT_OPENING_TERMS_ACCEPTANCE`
12. `DPS2.DEPOSIT_OPENING_DECISION`
13. system-managed `DPS2.DEPOSIT_OPENING_STATUS_HISTORY`

No `DEPOSIT_ACCOUNT` is created in this transaction. Account instantiation/activation remains the next lifecycle phase after Origination approval.

## Guards implemented

- `IDEMPOTENCY_KEY` replay returns the already-created request and does not duplicate children.
- Reuse of one idempotency key with a different `REQUEST_NO` is rejected.
- Exactly one primary owner is required.
- Owner shares must total 100 percent (tolerance 0.01).
- Joint ownership requires at least two owners.
- Approved requests require opening date, successful funding, all opening checks `PASS`, accepted terms and decision `APPROVE`.
- Maturity instruction cannot be persisted without a term row.
- Maturity date is derived from term value/unit when it is not supplied.
- Terms acceptance defaults the accepting Party to the primary owner when the UI does not explicitly send it.

## Source-contract alignment

The Angular wizard now uses the opening-prototype codes for:

- Funding methods: `TRANSFER`, `INTERNAL`, `CASH`, `CARD`.
- Opening checks: `PRODUCT_ELIGIBILITY`, `KYC_CDD`, `SANCTIONS`, `DOCUMENTS`, `INQUIRIES`, `OPENING_RULES`, `SIGNATORY_AUTHORITY`, `TERMS_ACCEPTANCE`, `DUPLICATE_REQUEST` with their matching check types.
- Terms acceptance source: `BRANCH`, `UI`, or `API` according to the opening channel.

## Verification status

Static verifier: `node tools/verify-dps2-deposit-opening-persistence.mjs`.

A real Oracle integration run is still required in the target environment to prove reference-code/FK compatibility and transaction rollback behavior against the installed DPS2 schema.
