# DPS2 0.11.0 Phase 11B — Account Servicing Core + Account Party

## Scope
Phase 11B operationalizes the already-deployed account servicing model without changing Deposit Opening 10F.

Implemented:
- Persistent `DEPOSIT_ACCOUNT_PARTY` as the post-opening Party source.
- Idempotent backfill from `DEPOSIT_OPENING_PARTY` using `SOURCE_OPENING_PARTY_ID`.
- Account basic information servicing (`ACCOUNT_NAME`, `OPENING_ORG_UNIT_CODE`) with optimistic record-version control.
- Account Party add/deactivate operations.
- Account Contact add/end operations.
- Append-style `DEPOSIT_ACCOUNT_SERVICING_HISTORY` entries for mutations.
- Account 360 reads Account Party, Contact and Servicing History.
- Angular Account Operations workspace exposes the Phase 11B actions.

Not in Phase 11B:
- Suspend / Dormancy / Reactivation and Hold (Phase 11C).
- Balance/Subledger (11D).
- Controlled closure replacement (11E).
- Term, Profit and Transaction engines (later phases).

## API contract
- `GET /api/v1/deposit-accounts`
- `GET /api/v1/deposit-accounts/{accountId}`
- `PUT /api/v1/deposit-accounts/{accountId}/basic-info`
- `POST /api/v1/deposit-accounts/{accountId}/parties`
- `DELETE /api/v1/deposit-accounts/{accountId}/parties/{accountPartyId}`
- `POST /api/v1/deposit-accounts/{accountId}/contacts`
- `DELETE /api/v1/deposit-accounts/{accountId}/contacts/{contactId}`

## Static qualification
`PHASE11B_STATIC_VERIFIER_PASS=22`, `PHASE11B_STATIC_VERIFIER_FAIL=0`.

Legacy regression qualification remained green for Phase 8, Phase 9, 0.9.2, 0.9.3, Phase 10, 10B, 10D and 10E/10F.

## Runtime closure marker
Phase 11B is CLOSED only after the target Oracle instance reports:
- `PHASE11B_SCHEMA_DATA_RECONCILIATION_PASS`
- `PHASE11B_DB_BASELINE_PASS`
- `PHASE11B_IMPLEMENTATION_PASS`
