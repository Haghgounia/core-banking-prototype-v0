# CAL 0.3.39 FIX50 - Raw transactional calendar import

## Requested behavior

The calendar importer performs no dataset/content validation and no row-count verification.

## Flow

1. Receive the two multipart files.
2. Skip the first/header line without validating it.
3. Stream `calendar_day.csv` and batch INSERT into `CAL.CALENDAR_DAY`.
4. Stream `calendar_date.csv` and batch INSERT into `CAL.CALENDAR_DATE`.
5. Return normally; the enclosing Spring `@Transactional` boundary commits the transaction.

If an INSERT/parser/JDBC exception occurs, normal transaction semantics roll the transaction back. No explicit pre/post dataset verification query is executed.

## Removed controls

- Seed readiness check
- Empty destination check
- calendar_day/calendar_date row-count comparison
- 3:1 representation count check
- Database row-count verification
- Canonical date continuity check
- DAY_ID continuity check
- JDN check
- ISO weekday check
- Calendar-system whitelist/count check
- SHA-256 official-dataset comparison
- Header equality validation

## Constraints

The application does not enable, disable, or modify Oracle constraints. Their state is controlled externally by the DBA, as requested.

## Technical parsing only

The importer performs only the minimum CSV field splitting and Java numeric/date conversion required to bind values to JDBC parameters. It does not validate ranges, calendar-system vocabulary, leap-year flags, date continuity, business rules, or expected counts.
