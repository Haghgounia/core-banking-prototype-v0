# DPS2 0.10.0 Phase 10 Foundation - Index Idempotency Hotfix

This hotfix changes supporting-index reconciliation in
`0.10.0-phase10-opening-operational-v5-foundation.sql` from physical index-name checks
to semantic coverage checks (`table + leading column`).

It prevents ORA-01408 when an equivalent supporting index already exists under another name.

Covered contracts:
- `DEPOSIT_OPENING_OBLIGATION(OPENING_REQUEST_ID)`
- `DEPOSIT_OPENING_FUND_ALLOC(OPENING_FUNDING_ID)`
- `DEPOSIT_OPENING_FUND_ALLOC(OPENING_OBLIGATION_ID)`

The migration remains fail-fast if the expected physical index name exists but points to the wrong leading column.
