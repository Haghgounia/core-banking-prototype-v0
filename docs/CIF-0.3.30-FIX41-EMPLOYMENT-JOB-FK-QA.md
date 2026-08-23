# CIF 0.3.30 — FIX41: Employment Job FK Alignment

## Problem
The prior FIX40 migration used non-existent columns `ACTIVE_FLAG` and `DISPLAY_ORDER` on `CIF.REF_OCCUPATION`. The current Oracle metadata defines `IS_ACTIVE` and `SORT_ORDER`. More importantly, adding `HOMEMAKER` / `UNEMPLOYED` to `REF_OCCUPATION` was not the correct domain fix because those values are employment-status values, not detailed occupation codes.

## Verified Current Oracle Contract
From `CIF-tables-2026-08-23-1624.xlsx`:
- `CIF.PARTY_EMPLOYMENT.OCCUPATION_CODE` is NOT NULL.
- Active FK: `FK_PARTY_EMPLOYMENT_REF_OCCUPATION` -> `CIF.REF_OCCUPATION(OCCUPATION_CODE)`.
- `OCCUPATION_GROUP_CODE` currently has no FK.
- `CIF.REF_OCCUPATION` columns are `OCCUPATION_CODE, NAME_FA, NAME_EN, DESCRIPTION_FA, SORT_ORDER, IS_ACTIVE, PARENT_CODE, VALID_FROM, VALID_TO, RECORD_VERSION`.

## Actual UI Contract
The employment UI deliberately uses the shared general employment catalog:
- `GEO.JOB_GROUPS` for the Persian job-group selector.
- `GEO.JOBS` for the Persian detailed job selector.
- `SearchableComboComponent` persists the selected option `code`, therefore the persisted value is `JOB_CODE`.

The previous FK therefore rejected valid detailed GEO job codes because it expected a code from the small CIF `REF_OCCUPATION` domain.

## Fix
1. Preserve detailed GEO group/job selection in UI.
2. Drop obsolete `FK_PARTY_EMPLOYMENT_REF_OCCUPATION`.
3. Add `FK_EMP_GEO_JOB` from `OCCUPATION_CODE` to `GEO.JOBS(JOB_CODE)`.
4. Add `FK_EMP_GEO_JOB_GROUP` from `OCCUPATION_GROUP_CODE` to `GEO.JOB_GROUPS(JOB_GROUP_CODE)`.
5. Create both FKs as `ENABLE NOVALIDATE` so historic rows are retained while new/changed rows are enforced.
6. Add backend pre-validation for active job, active group, and job/group consistency.

## Migration
`database/oracle/cif/migrations/0.3.30-fix41-employment-job-fk-alignment.sql`

If the migration reports `ORA-01031`, grant direct REFERENCES privilege from GEO to CIF as documented at the top of the migration.

## Regression Scenarios
- Select a job group such as «آهنگران».
- Select a detailed job such as «آهنگر در و پنجره».
- Save the PERSON employment record.
- Confirm no `ORA-02291` occurs for occupation.
- Confirm `PARTY_EMPLOYMENT.OCCUPATION_CODE = GEO.JOBS.JOB_CODE`.
- Confirm `PARTY_EMPLOYMENT.OCCUPATION_GROUP_CODE = GEO.JOB_GROUPS.JOB_GROUP_CODE`.
