# Collateral R2 G-03 / G-12 - implementation checkpoint (2026-11-09)

**Baseline:** `CLT-ROADMAP-R1.0-FA.md`, the 9 attached Collateral documents including V1.42 UI, XMI 26-table model, and successful installation of R2 G-01 patch.

## Architectural decisions and scope

- `HAS_MULTIPLICITY` is **not** `IS_COUNTABLE_FLAG`. They are independent flags. The latter is an explicit nullable extension requiring approved configuration for each active Collateral Type. NULL means an unresolved policy; no guess from legacy seed values.
- Countable serial range input: decimal strings up to 100 digits, inclusive 1..20 items per row, no overlap even with leading zeroes, max 80 rows per immutable draft, amounts exact IRR integers. Validation remains on backend.
- Drafts remain immutable and own their rows; insert parent draft, audit and normalized serial children in one Oracle transaction, then compare child rows to the JSON snapshot. A mismatch blocks reviewer submission via existing integrity check.
- New endpoint `GET /api/v1/collaterals/introduction-drafts/types/{typeId}/countability` reads the two independent Oracle flags. `GET /api/v1/collaterals/introduction-drafts/{draftId}/item-ranges` checks persisted children for the current maker and branch.
- Existing complete 17-reference-table baseline remains unchanged until a reviewed DB extension is approved. This code does not create new finance writers or a standalone CLT process.
- Future operational registered-asset child table is designed in a separate, NOT-YET-ACTIVE migration file. **No final CLT.COLLATERAL writes or registration numbers are enabled.** G-14 and G-17 remain unresolved.

## DB governance

- `database/clt/r2-g03-g12/00_READONLY_PRECHECK.sql`: readonly metadata report.
- `01_G12_FLAG_AND_G03_DRAFT_RANGES_DBA_OPT_IN.sql`: one-time G12 column and independent draft range child table; review/apply only through DBA/change control. No default value for IS_COUNTABLE_FLAG.
- `02_G03_REGISTERED_RANGES_DBA_OPT_IN.sql`: proposed final table for the later registration stage; do not apply before approval of G14/G17 and mapping.
- `03_BACKFILL_EXISTING_DRAFT_RANGES_DBA_OPT_IN.sql`: optional migration for earlier JSON-only draft snapshots, review row validity and hashes before execution. Without it old countable drafts fail readback/review instead of appearing valid.

**No source installer or service startup runs any Oracle DDL/DML.** Client cannot choose an alternative Countability state through the draft API. Business approval is required to set `IS_COUNTABLE_FLAG` to Y or N per type; no type is silently reclassified.

## Delivery/acceptance boundary

- Implemented in source: G12 independent flag read + UI, G03 normalized child insert/readback, integrity review gate, SQL schema proposals, migration guard and backfill proposal.
- Pending: DBA migration review/application, approved values for countable flag, browser/Oracle UAT at end of development, native Windows full build, registered-asset operations/G14/G17.

This checkpoint does not alter R0-R7, does not certify production readiness, and does not close G-03 or G-12 at governance level until the migration and live schema are accepted.
