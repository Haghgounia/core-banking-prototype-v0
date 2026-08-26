# CAL 0.3.41 - FIX52 - Angular Import Result Contract QA

## Problem
Angular compilation failed with TS2339 because `calendar-dataset-import.component.html` referenced `minimumCanonicalDate` and `maximumCanonicalDate`, while `CalendarDatasetImportResult` exposes only:
- schemaName
- calendarDayFileName
- calendarDateFileName
- calendarDayRows
- calendarDateRows
- elapsedMillis

## Fix
Removed the two non-existent template bindings and replaced the Canonical range tile with the two actual uploaded file names.

## Import behavior retained
No dataset validation or row-count validation was added back. The FIX50 raw transactional JDBC import behavior remains unchanged: CALENDAR_DAY is inserted first, CALENDAR_DATE second, and Spring commits after both streams return successfully. Insert/parse/database errors still roll the transaction back.

## Static verification
Passed:
- verify-cif-persisted-grids.mjs
- verify-ea-oracle-comparison.mjs
- verify-calendar-reference.mjs
- verify-calendar-dataset-import.mjs (19 checks)
- grep check confirms no `minimumCanonicalDate` / `maximumCanonicalDate` references remain in frontend source.

## Build note
A full Angular/Maven build was not executed in the packaging environment because frontend node_modules are not present. The user's Windows build is the final integration build.
