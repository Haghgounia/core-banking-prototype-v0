-- ============================================================================
-- fee_seed_all.sql - master seed loader
-- Fee Prototype Target Model Baseline 1.0 / Oracle
-- UTF-8
-- ============================================================================
SET DEFINE OFF;
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK;
ALTER SESSION SET CURRENT_SCHEMA = FEE;

PROMPT Loading Fee Baseline 1.0 seed data...
@@01_fee_reference_data.sql
@@02_fee_demo_master_data.sql
@@03_fee_policy_regulatory_data.sql
@@04_fee_configuration_data.sql
@@05_fee_arrangement_data.sql
@@06_fee_runtime_sample_data.sql
PROMPT Fee seed data loaded successfully.
