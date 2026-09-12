SET ECHO ON
SET SERVEROUTPUT ON
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK;
PROMPT === Importing CBI Rial Fee 1405 PROVISIONAL ===
@01-import-cbi-rial-fee-1405-provisional.sql
PROMPT === Verifying ===
@02-verify-cbi-rial-fee-1405-provisional.sql
COMMIT;
PROMPT === CBI Rial Fee 1405 PROVISIONAL committed successfully ===
