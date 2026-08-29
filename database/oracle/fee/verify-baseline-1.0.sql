-- FEE Baseline 1.0 / Verification
SET SERVEROUTPUT ON;
SET DEFINE OFF;
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK;
@@baseline-1.0/seed/99_fee_seed_verify.sql
