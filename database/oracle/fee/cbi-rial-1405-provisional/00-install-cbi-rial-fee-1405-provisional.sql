SET ECHO ON
SET SERVEROUTPUT ON SIZE UNLIMITED
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK;
PROMPT === Importing CBI Rial Fee 1405 PROVISIONAL ===
@01-import-cbi-rial-fee-1405-provisional.sql
PROMPT === Structural verification ===
@02-verify-cbi-rial-fee-1405-provisional.sql
PROMPT === Source-to-Oracle business-value reconciliation ===
@04-reconcile-cbi-rial-fee-1405-provisional-enforced.sql
COMMIT;
PROMPT === CBI Rial Fee 1405 PROVISIONAL committed successfully ===
