-- Installer-enforced reconciliation wrapper.
-- Any mismatch must rollback the pending import transaction before SQL*Plus exits.
SET DEFINE OFF;
SET SERVEROUTPUT ON SIZE UNLIMITED;
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK;
ALTER SESSION SET CURRENT_SCHEMA = FEE;
@04-reconcile-cbi-rial-fee-1405-provisional-core.sql
