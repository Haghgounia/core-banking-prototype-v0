PROMPT =====================================================================
PROMPT Core Banking Prototype 0.3.31-fix42 - required cross-schema grants
PROMPT Run this file as GEO owner or DBA. Do NOT run it as application user.
PROMPT =====================================================================

WHENEVER SQLERROR EXIT SQL.SQLCODE

GRANT REFERENCES ON GEO.JOBS TO CIF;
GRANT REFERENCES ON GEO.JOB_GROUPS TO CIF;

PROMPT Direct REFERENCES grants required by CIF are now in place.
PROMPT Next run: 0.3.31-fix42-employment-job-fk-repair.sql
