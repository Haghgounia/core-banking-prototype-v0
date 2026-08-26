# CAL 0.3.38 FIX49 - Canonical Date Midnight Import

## Root cause
`CAL.CALENDAR_DAY.CANONICAL_DATE` is an Oracle `DATE` protected by `CK_CAL_DAY_MIDNIGHT (CANONICAL_DATE = TRUNC(CANONICAL_DATE))`. FIX48 bound Java `java.sql.Date` through `PreparedStatement.setDate`. JDBC date binding can be timezone/session sensitive and may reach Oracle with a non-zero time component.

## Fix
The insert now uses `TO_DATE(?, 'YYYY-MM-DD')` and binds the parsed `LocalDate` as its ISO string. Oracle therefore constructs the value at 00:00:00 independent of JVM/driver/session timezone.

## Scope
No DDL or data model change. CSV format is unchanged. Transaction and validation logic are unchanged.
