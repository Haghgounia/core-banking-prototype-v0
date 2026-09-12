SET DEFINE OFF;
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK;
PROMPT Delegating to CBI Rial Fee 1405 PROVISIONAL versioned importer...
@@cbi-rial-1405-provisional/00-install-cbi-rial-fee-1405-provisional.sql
