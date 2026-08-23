PROMPT =====================================================================
PROMPT DEPRECATED: 0.3.30-fix41-employment-job-fk-alignment.sql
PROMPT =====================================================================
PROMPT Do NOT use this migration. FIX41 could drop the old occupation FK
PROMPT before Oracle verified that CIF had a direct REFERENCES grant on GEO.
PROMPT Use the FIX42 scripts instead, in this order:
PROMPT   1) 0.3.31-fix42-employment-job-fk-diagnostic.sql
PROMPT   2) 0.3.31-fix42-employment-job-fk-grants.sql   (as GEO owner / DBA)
PROMPT   3) 0.3.31-fix42-employment-job-fk-repair.sql
PROMPT =====================================================================
