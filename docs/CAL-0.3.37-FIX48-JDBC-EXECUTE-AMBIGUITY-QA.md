# CAL 0.3.37 / FIX48 — JDBC execute overload ambiguity

## Root cause
`JdbcTemplate.execute(connection -> ...)` is ambiguous in Spring JDBC because both `ConnectionCallback<T>` and `StatementCallback<T>` are valid functional-interface overloads. Java 21 therefore rejects the two import methods at compile time.

## Fix
Both batch-import calls now explicitly cast the lambda to `ConnectionCallback<Long>`. This preserves the intended semantics: obtain the transactional JDBC `Connection`, create one `PreparedStatement`, stream the CSV, and execute 1000-row batches.

## Affected methods
- `loadCalendarDays`
- `loadCalendarDates`

## Data/model impact
No DDL, DML migration, CSV format, transaction behavior, or CAL schema change.

## Expected build identifiers
- Product: `0.3.37-prototype-fee-p1`
- Backend: `0.3.37-SNAPSHOT`
- Frontend: `0.3.37`
