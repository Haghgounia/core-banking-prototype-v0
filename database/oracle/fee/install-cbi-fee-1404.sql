SET DEFINE OFF;
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK;
PROMPT Delegating to clean CBI Fee Tariff 1404 importer...
@@cbi-1404/00-install-cbi-fee-1404.sql
