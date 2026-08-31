SET DEFINE OFF;
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK;
PROMPT ============================================================
PROMPT CBI Fee Tariff 1404 - Transactional Clean Import
PROMPT Import -> Verify -> Commit. Any SQL/verification error rolls back.
PROMPT ============================================================
@@01-import-cbi-fee-1404.sql
@@02-verify-cbi-fee-1404.sql
PROMPT [CBI1404] Verification passed. Committing transaction ...
COMMIT;
PROMPT [CBI1404] Import committed successfully.
