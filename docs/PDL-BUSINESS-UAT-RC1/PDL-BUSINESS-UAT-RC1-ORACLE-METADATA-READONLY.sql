-- R30/HF8: Oracle PDL 54-table metadata parity (READ ONLY)
-- Query tool: SQL*Plus / SQLcl; run under account with ALL_* view access.
-- No INSERT/UPDATE/DELETE/MERGE/DDL/COMMIT/EXECUTE statements in this file.
-- PDL is the product-builder schema, not FEE/FEE2.
SET PAGESIZE 400
SET LINESIZE 240
SET FEEDBACK ON
PROMPT [PDL HF8] Enumerate 54 catalog tables and physical Oracle presence
WITH expected AS (
 SELECT column_value AS table_name
 FROM TABLE(sys.odcivarchar2list(
  'PRODUCT'
  ,'PRODUCT_VERSION'
  ,'PRODUCT_VERSION_MODULE'
  ,'PRODUCT_LEGACY_MAPPING'
  ,'PRODUCT_RELATIONSHIP'
  ,'PRODUCT_ORG_SCOPE'
  ,'PRODUCT_CHANNEL_OPERATION'
  ,'PRODUCT_CHANNEL_RULE'
  ,'PRODUCT_RATE_TIER'
  ,'PRODUCT_ELIGIBILITY_RULE'
  ,'PRODUCT_ELIGIBILITY_CRITERION'
  ,'PRODUCT_REQUIRED_INQUIRY'
  ,'PRODUCT_PRICING_COMPONENT'
  ,'PRODUCT_PRICING_RULE'
  ,'PRODUCT_REQUIRED_DOCUMENT'
  ,'DEPOSIT_PRODUCT_JOINT_RULE'
  ,'DEPOSIT_PRODUCT_HOLD_RULE'
  ,'DEPOSIT_PRODUCT_ALLOWED_TERM'
  ,'DEPOSIT_PRODUCT_CLOSURE_SETTLEMENT_RULE'
  ,'DEPOSIT_PRODUCT_PROFILE'
  ,'DEPOSIT_PRODUCT_WITHDRAWAL_MEDIA'
  ,'DEPOSIT_PRODUCT_CLOSURE_APPROVAL_RULE'
  ,'DEPOSIT_PRODUCT_CLOSURE_PRECHECK'
  ,'DEPOSIT_PRODUCT_TRANSACTION_RULE'
  ,'DEPOSIT_PRODUCT_OPENING_RULE'
  ,'DEPOSIT_PRODUCT_CLOSURE_RULE'
  ,'DEPOSIT_PRODUCT_TERM_RULE'
  ,'DEPOSIT_PRODUCT_DORMANCY_RULE'
  ,'DEPOSIT_PROFIT_PAYMENT_RULE'
  ,'LOAN_PRODUCT_PROCESS_RULE'
  ,'LOAN_PRODUCT_PROFILE'
  ,'LOAN_FINANCIAL_EXTENSION'
  ,'LOAN_ELIGIBILITY_EXTENSION'
  ,'LOAN_PRODUCT_REPAYMENT_RULE'
  ,'LOAN_PRODUCT_COLLATERAL_RULE'
  ,'FACILITY_OPERATION_PARAMETER'
  ,'PLAN_TYPE'
  ,'LOAN_TYPE_PLAN_TYPE'
  ,'LOAN_TYPE'
  ,'PATTERN_OPERATION_LOAN_TYPE'
  ,'PATTERN_OPERATION_DETAIL'
  ,'ECONOMIC_SECTION'
  ,'LOAN_USAGE'
  ,'ECONOMIC_SUB_SECTION'
  ,'LOAN_PRODUCT_COLLATERAL'
  ,'DOCUMENT_TYPE'
  ,'PATTERN_OPERATION'
  ,'OPERATION'
  ,'SUB_OPERATION'
  ,'CORRESPONDENT_ACCOUNT_PRODUCT_PROFILE'
  ,'CORRESPONDENT_ACCOUNT_SETTLEMENT_RULE'
  ,'CODE_SET'
  ,'CODE_VALUE'
  ,'CODE_VALUE_TRANSITION'
 ))
)
SELECT e.table_name, CASE WHEN t.table_name IS NULL THEN 'MISSING_IN_ORACLE' ELSE 'PRESENT' END AS oracle_status
FROM expected e
LEFT JOIN all_tables t ON t.owner='PDL' AND t.table_name=e.table_name
ORDER BY e.table_name;

PROMPT [PDL HF8] PDL column metadata (physical snapshot; compare externally against source catalog)
SELECT c.table_name, c.column_id, c.column_name, c.data_type,
       c.data_length, c.data_precision, c.data_scale, c.nullable
FROM all_tab_columns c
WHERE c.owner='PDL'
ORDER BY c.table_name, c.column_id;

PROMPT [PDL HF8] PDL primary/unique/foreign/check constraints and status
SELECT c.table_name, c.constraint_name, c.constraint_type, c.status,
       c.validated, c.r_owner, c.r_constraint_name
FROM all_constraints c
WHERE c.owner='PDL' AND c.constraint_type IN ('P','U','R','C')
ORDER BY c.table_name, c.constraint_type, c.constraint_name;

PROMPT [PDL HF8] PDL constraint-column mapping
SELECT c.table_name, c.constraint_name, c.column_name, c.position
FROM all_cons_columns c
WHERE c.owner='PDL'
ORDER BY c.table_name, c.constraint_name, c.position;

PROMPT [PDL HF8] Review only; any mismatches require explicit analysis and DBA-approved migration.
