# Fix30 — EA / Oracle Model Comparison QA

Version: `0.3.22-prototype-fix30`  
Date: 2026-08-22

## Scope

A new management/system form was added at:

```text
/system/database-model-comparison
```

The screen uploads an Enterprise Architect XML/XMI export, selects one of the Oracle schemas already configured under `core-banking.schemas`, compares the EA table model with the live Oracle metadata, and reports exact row counts for the EA tables that exist in Oracle.

## Comparison rules

The structural comparison includes:

- table existence;
- EA raw table-definition count and unique table count;
- columns present in EA and Oracle;
- Oracle data type;
- character/raw length;
- NUMBER precision and scale;
- TIMESTAMP fractional-second precision;
- CHAR/BYTE length semantics where EA declares it;
- nullable / not-null;
- primary-key column list and order when the EA model defines a PK;
- columns missing from Oracle;
- columns present only in Oracle;
- exact `COUNT(*)` for each compared EA table when row counting is enabled;
- tables present in the selected Oracle schema but absent from the EA file.

Foreign keys, indexes, check constraints, unique constraints and comments are intentionally outside the Fix30 comparison status and are stated as such on the screen.

## Oracle metadata sources

The backend uses the same Spring DataSource as the running application and reads:

```text
ALL_USERS
ALL_TABLES
ALL_TAB_COLUMNS
ALL_CONSTRAINTS
ALL_CONS_COLUMNS
```

Row count is calculated with exact `SELECT COUNT(*)` against the selected owner/table. Schema selection is restricted to the resolved application configuration values:

```text
core-banking.schemas.cif
core-banking.schemas.reference-data
core-banking.schemas.deposit-product-factory
core-banking.schemas.party-reference
```

No database password is returned to the frontend.

## Uploaded sample validation

The supplied `Party-Operation_Froms-1.xml` was parsed directly with the Fix30 Java parser.

Observed parser result:

```text
53 raw table definitions
48 unique table names
816 unique-table columns
1 duplicate-definition warning
```

`PARTY` is defined six times in different EA packages with the same structural definition. Fix30 merges these duplicate definitions and reports one `PARTY` table instead of six.

## XML security

The parser is namespace-aware and disables:

- DOCTYPE declarations;
- external general entities;
- external parameter entities;
- external DTD loading;
- external DTD/schema access;
- XInclude and entity expansion.

A malicious DOCTYPE/XXE smoke input was rejected successfully by the JDK XML parser.

## Backend QA

Added tests:

```text
backend/src/test/java/com/behsazan/corebanking/system/modelcomparison/EaXmiModelParserTest.java
backend/src/test/java/com/behsazan/corebanking/system/modelcomparison/EaOracleComparisonServiceTest.java
```

Static Java compilation of the complete new `system/modelcomparison` package was executed with API stubs using Java 21 and completed with zero syntax/type-shape errors.

A comparison-engine smoke test verified an equivalent EA/Oracle table as:

```text
MATCH rows=12 match=2 diff=0 pk=MATCH
```

## Frontend QA

New frontend files:

```text
frontend/src/app/features/database-model-comparison/database-model-comparison.component.ts
frontend/src/app/features/database-model-comparison/database-model-comparison.component.html
frontend/src/app/features/database-model-comparison/database-model-comparison.component.scss
frontend/src/app/features/database-model-comparison/database-model-comparison.models.ts
frontend/src/app/features/database-model-comparison/database-model-comparison.service.ts
```

The TypeScript parser reported zero parse diagnostics for all changed/new TS files.

The form provides:

- EA XML/XMI file picker;
- configured Oracle schema selector;
- exact row-count option (enabled by default);
- connection product/user/JDBC target information without password;
- summary cards;
- table-level comparison grid;
- status/search filters;
- detailed per-column comparison grid;
- primary-key comparison;
- database-only table list;
- CSV export of the table-level report.

## Build regression guard

Added:

```text
tools/verify-ea-oracle-comparison.mjs
```

It is executed by `build-production.cmd` before npm/Maven build and verifies the route, menu, API wiring, secure EA parser, configured schema usage, Oracle metadata queries, row-count query and report grids.

Result:

```text
CIF persisted-grid verification OK: 67 CIF grids, no stream/card record renderers, dockable sidebar verified.
Fix30 EA/Oracle comparison verification OK: route, secure EA parser, configured-schema Oracle metadata, row counts and report grids verified.
```

## Full build limitation in this environment

A full Angular build could not be executed because the offline npm cache does not contain `zod-to-json-schema-3.25.2.tgz`.

A full Maven build could not be executed because the Maven Wrapper cannot download Maven 3.9.16 from `repo.maven.apache.org` in this environment.

Therefore the final compilation/build verification must run in the project Windows environment through:

```bat
build-production.cmd
```

The new Fix30 static verifier runs automatically before that build.

## Live Oracle limitation in this environment

The user's configured Oracle instance is not accessible from this sandbox. The actual live metadata comparison and row counts cannot be asserted here. In the user's runtime environment the endpoint uses the application's configured DataSource directly; no separate Oracle connection configuration was introduced.

## Database migration

Fix30 introduces no schema/table/column change and requires **no database migration**.
