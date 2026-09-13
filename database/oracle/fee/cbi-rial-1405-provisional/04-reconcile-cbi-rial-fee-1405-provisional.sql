-- Standalone read-only reconciliation wrapper.
-- IMPORTANT: on mismatch this wrapper reports ORA-20260 but does NOT COMMIT or ROLLBACK
-- the caller transaction. Run it in a clean/fresh session for independent validation.
SET DEFINE OFF;
SET SERVEROUTPUT ON SIZE UNLIMITED;
WHENEVER SQLERROR CONTINUE NONE;
ALTER SESSION SET CURRENT_SCHEMA = FEE;
PROMPT === Standalone CBI Rial Fee 1405 source-to-Oracle reconciliation ===
@04-reconcile-cbi-rial-fee-1405-provisional-core.sql
