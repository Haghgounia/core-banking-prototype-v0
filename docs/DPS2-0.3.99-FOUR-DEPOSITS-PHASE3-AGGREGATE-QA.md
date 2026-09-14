# DPS2 0.3.99 — Four Deposits Phase 3 / Full Opening Aggregate QA

## Scope

Phase 3 extends the atomic Deposit Account Opening aggregate introduced in 0.3.98 so the operational wizard can persist the remaining opening-time relationships and service selections defined by the supplied DPS2 model and prototype.

The API remains:

`POST /api/v1/deposit-opening/requests`

No new DDL is introduced. The target Oracle `DPS2` schema must already contain the supplied Deposit Account Opening tables/sequences and their reference data.

## Aggregate extension

The existing Phase 2 transaction is extended with these additional table contracts when present in the payload:

1. `DPS2.DEPOSIT_OPENING_SIGNATORY`
2. `DPS2.DEPOSIT_OPENING_SIGNATORY_AUTHORITY`
3. `DPS2.DEPOSIT_OPENING_AUTHORIZED_USER`
4. `DPS2.DEPOSIT_OPENING_DELEGATION`
5. `DPS2.DEPOSIT_OPENING_BENEFICIARY`
6. `DPS2.DEPOSIT_OPENING_PAYMENT_INSTRUMENT`
7. `DPS2.DEPOSIT_OPENING_PRICING_OVERRIDE_REQUEST`
8. `DPS2.DEPOSIT_OPENING_TAX_STATUS`
9. `DPS2.DEPOSIT_OPENING_REWARD_ENROLLMENT`
10. `DPS2.DEPOSIT_OPENING_DOCUMENT`

Together with the Phase 2 contracts, the create endpoint now covers 23 opening aggregate table contracts including system-managed initial `DEPOSIT_OPENING_STATUS_HISTORY`.

## Signatory identifier handling

The prototype payload uses `DEPOSIT_OPENING_SIGNATORY.OPENING_SIGNATORY_ID` to connect authority rows to a Signatory before Oracle persistence. In Phase 3 this value is treated as a request-local client correlation key only. Backend allocates the persisted identifier from `DPS2.SEQ_DEPOSIT_OPENING_SIGNATORY`, maps each request-local key to the new Oracle ID, and writes `DEPOSIT_OPENING_SIGNATORY_AUTHORITY.OPENING_SIGNATORY_ID` with the persisted ID.

The client key is never trusted as the Oracle primary key.

## Validation guards

Phase 3 adds source-aligned guards for:

- Signatory Party must belong to the request Party set.
- Signatory client references must be unique when authority rows are supplied.
- Every Signatory Authority must reference a Signatory from the same aggregate.
- Authority amount cannot be negative and Boolean flags accept only 0/1.
- Delegation requires distinct grantor/delegate Parties plus delegation type, authority scope and document reference.
- Beneficiary share must be between 0 and 100 when supplied.
- Payment instrument quantity must be positive.
- Approved pricing override requires an approval reference.
- Tax status, when supplied, requires tax residency and source code.
- Reward enrollment requires a valid program ID and consent reference.
- Received/verified documents require a document reference.
- Approved opening requests reject document rows with `MISSING` or `REJECTED` state.

Existing Phase 2 guards for idempotency, ownership, funding, checks, terms and decision remain in force.

## UI coverage

The seven-step Angular wizard remains intact. Phase 3 adds operational editors for:

- Signatory and Signatory Authority
- Authorized User
- Delegation / legal representation
- Beneficiary
- Payment Instrument request
- Pricing Override request
- Tax Status snapshot
- Reward Enrollment
- Opening Document

The final payload includes all Phase 3 table contracts and is still submitted as one atomic create request.

## Verification

Static guards:

- `node tools/verify-dps2-four-deposits.mjs`
- `node tools/verify-dps2-deposit-opening-persistence.mjs`
- `node tools/verify-dps2-deposit-opening-phase3.mjs`

A real Oracle integration test is still required in the target environment to prove installed FK/reference-code compatibility, sequence permissions, rollback behavior and full end-to-end UI/API execution.

## Lifecycle boundary

`DEPOSIT_ACCOUNT` creation/activation is intentionally not included in Phase 3. The opening aggregate ends at an approved Origination request. Account instantiation and transition to `PENDING_ACTIVATION`/`ACTIVE` remain the next phase.
