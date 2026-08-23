# CIF 0.3.31 — FIX42: Cross-schema Employment FK Repair

## Observed execution state
The FIX41 migration validated GEO metadata and then successfully dropped `FK_PARTY_EMPLOYMENT_REF_OCCUPATION`, but Oracle raised `ORA-00942` while executing the cross-schema FK DDL `REFERENCES GEO.JOBS (JOB_CODE)`. Therefore the database is currently in a partial migration state: the old occupation FK is absent and the replacement GEO FK was not created.

## Root cause
`GEO.JOBS` and `GEO.JOB_GROUPS` are defined in the project and FIX41's metadata pre-check reached the DDL stage, which means the parent key metadata was visible. Oracle cross-schema foreign keys additionally require the owner of the child constraint (`CIF`) to have a **direct REFERENCES privilege** on the parent table/column. A role-based or SELECT-only grant does not satisfy this DDL requirement and Oracle may surface `ORA-00942` during `ALTER TABLE ... REFERENCES GEO...`.

## Safe repair order
1. Run `0.3.31-fix42-employment-job-fk-diagnostic.sql` (read-only).
2. As GEO owner or DBA, run `0.3.31-fix42-employment-job-fk-grants.sql`.
3. Run `0.3.31-fix42-employment-job-fk-repair.sql`.
4. Verification must return exactly two rows: `FK_EMP_GEO_JOB` and `FK_EMP_GEO_JOB_GROUP`.

## Required direct privileges
```sql
GRANT REFERENCES ON GEO.JOBS TO CIF;
GRANT REFERENCES ON GEO.JOB_GROUPS TO CIF;
```

## Expected final mappings
- `CIF.PARTY_EMPLOYMENT.OCCUPATION_CODE -> GEO.JOBS.JOB_CODE`
- `CIF.PARTY_EMPLOYMENT.OCCUPATION_GROUP_CODE -> GEO.JOB_GROUPS.JOB_GROUP_CODE`

The new FKs use `ENABLE NOVALIDATE` so existing legacy rows are not blocked, while new/updated rows are enforced.
