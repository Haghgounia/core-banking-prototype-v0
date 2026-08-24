# CIF 0.3.34 — FIX45: Oracle → EA virtual-column regression test correction

## Observed failure
The FIX44 production build reached Maven tests and failed only in `OracleEaMetadataInspectorDictionarySqlTest.virtualColumnMustComeFromAllTabColsNotAllTabColumns`.

The runtime SQL itself was already corrected in FIX44:
- visible column metadata source: `ALL_TAB_COLUMNS C`
- virtual-column metadata source: `ALL_TAB_COLS TC`
- selected expression: `COALESCE(TC.VIRTUAL_COLUMN, 'NO') AS VIRTUAL_COLUMN`

## Root cause
The test used:

```java
assertFalse(main.contains("C.VIRTUAL_COLUMN"));
```

This is a false-positive assertion because the string `TC.VIRTUAL_COLUMN` contains `C.VIRTUAL_COLUMN` as a substring. Therefore a correct FIX44 query caused the test to fail.

## Fix
The assertion now uses an alias-aware regular expression:

```java
(?<![A-Z0-9_])C\.VIRTUAL_COLUMN\b
```

This detects only a standalone `C.VIRTUAL_COLUMN` reference and does not match `TC.VIRTUAL_COLUMN`.

Regression examples were added for both forms:
- `SELECT C.VIRTUAL_COLUMN ...` -> must be detected as invalid
- `SELECT TC.VIRTUAL_COLUMN ...` -> must be accepted

## Database impact
None. No SQL migration is required.

## Deployment note
A clean source tree should report the same release number at all build stages. For FIX45 the first line of `build-production.cmd` must show `0.3.34-prototype-fee-p1`, Angular package must build as `0.3.34`, and Maven must build `0.3.34-SNAPSHOT`.
