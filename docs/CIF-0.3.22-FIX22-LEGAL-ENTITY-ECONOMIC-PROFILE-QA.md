# CIF 0.3.22 Fix22 — Legal Entity Economic Profile QA

## Scope

Fix22 is intentionally limited to the `ORGANIZATION` / legal-entity path. The baseline is `0.3.22-prototype-fix21` plus the latest supplied CIF metadata `CIF-tables-2026-08-22-1200.xlsx`.

The metadata confirms these Organization attributes: `ISIC_CODE`, `ACTIVITY_STATUS_CODE`, `MAIN_ACTIVITY_DESCRIPTION`, `EMPLOYEE_COUNT`, `ENTERPRISE_SIZE_CODE`, and `OWNERSHIP_TYPE_CODE`.

## Implemented behavior

- Economic activity is selected by Persian business title from `CIF.REF_ISIC_ACTIVITY`; `ISIC_CODE` remains the persisted business code.
- Organization activity status is selected from dedicated `CIF.REF_ORGANIZATION_ACTIVITY_STATUS`.
- Employee count is a numeric value, not Reference Data; it is captured once in Legal Entity Identity.
- Enterprise size is selected from dedicated `CIF.REF_ENTERPRISE_SIZE`; selected code persists in `ORGANIZATION.ENTERPRISE_SIZE_CODE`.
- Ownership type is selected from dedicated `CIF.REF_OWNERSHIP_TYPE`; selected code persists in `ORGANIZATION.OWNERSHIP_TYPE_CODE`.
- Main-activity description uses a full-width multi-line field.
- Section 3.1 no longer repeats employee count; updating 3.1 preserves the existing `ORGANIZATION.EMPLOYEE_COUNT`.
- Existing Fix19 multi-record grids remain unchanged and retain business-readable context.

## Reference Data introduced

| Table | Code column | Seed count |
|---|---|---:|
| `REF_ORGANIZATION_ACTIVITY_STATUS` | `ACTIVITY_STATUS_CODE` | 4 |
| `REF_ENTERPRISE_SIZE` | `ENTERPRISE_SIZE_CODE` | 4 |
| `REF_OWNERSHIP_TYPE` | `OWNERSHIP_TYPE_CODE` | 6 |

All three tables are added to the governed Party Reference metadata catalog. Catalog count changes from 99 to 102 tables.

## Persistence and server validation

`CifService.upsertOrganization` validates active values against the dedicated Reference Data tables before writing to `ORGANIZATION`. The migration adds `ENABLE NOVALIDATE` foreign keys where the Organization columns exist, so future DML is governed without rejecting legacy rows during migration.

## Person regression boundary

- `personForm` model mapping is unchanged from Fix21.
- The Person identity template block in `party-create.component.html` is byte-equivalent to Fix21 for the compared block.
- The Person edit block in `party-360.component.html` is unchanged for the compared block.
- No Person table, Person request, or Person persistence SQL was changed.
- The generic Party classification form was restored unchanged; organization-specific attributes do not leak into Party Classification domains.

## Static QA performed

- TypeScript parser: no syntax diagnostics in all modified CIF TypeScript files.
- JSON validation: `party-reference-model.json` is valid JSON; 102 active table definitions; all three new resources resolve to a 10-column descriptor.
- Form-control check: all Organization controls referenced by modified templates exist in corresponding FormGroups.
- Section 3.1 duplicate check: zero `employeeCount` controls.
- Raw-code UX check: no raw `<input>` remains for `ACTIVITY_STATUS_CODE`, `ENTERPRISE_SIZE_CODE`, `OWNERSHIP_TYPE_CODE`, or Organization `ISIC_CODE` in the three modified legal-entity forms.
- Latest CIF metadata readback confirms all six Organization fields targeted by Fix22 exist.
- Java parse-stage check found no syntax-class diagnostics in modified service code; dependency resolution is unavailable in the execution container.
- `node tools/sync-system-specification.mjs` completed successfully for version `0.3.22-prototype-fix22`.

## Build limitation in this execution environment

A full Angular/Maven dependency build could not be completed because the container cannot download npm/Maven dependencies. `npm ci` and Maven Wrapper dependency retrieval are network-blocked. The package therefore includes static/parser QA and must receive the normal `build-production.cmd` verification on the project Windows environment before deployment.

## Required migration

Run:

`database/oracle/cif/migrations/0.3.22-fix22-organization-economic-profile.sql`

before functional testing of the Organization forms.
