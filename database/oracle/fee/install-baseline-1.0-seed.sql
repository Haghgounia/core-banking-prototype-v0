-- FEE Baseline 1.0 / Seed installer
SET DEFINE OFF;
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK;
@@baseline-1.0/seed/fee_seed_all.sql
@@baseline-1.0/seed/99_fee_seed_verify.sql
