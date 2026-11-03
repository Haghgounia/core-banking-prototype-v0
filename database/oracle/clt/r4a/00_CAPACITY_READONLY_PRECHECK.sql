-- CLT-R4A diagnostic only. Run manually as read-only DBA metadata query.
-- NO DDL/DML, no transaction commit, no changes to CLT.
SELECT owner, table_name, column_name, data_type, data_precision, data_scale
  FROM all_tab_columns
 WHERE owner = 'CLT'
   AND ((table_name = 'COLLATERAL' AND column_name IN
          ('COLLATERAL_ID','BRANCH_CODE','CURRENCY_CODE','CURRENT_VALUATION_HISTORY_ID','COLLATERAL_STATUS_CODE','VALID_FROM','VALID_TO'))
     OR (table_name = 'COLLATERAL_ALLOCATION' AND column_name IN
          ('COLLATERAL_ID','MODULE_ID','CURRENCY_CODE','ALLOCATE_AMOUNT','USED'))
     OR (table_name = 'COLLATERAL_VALUATION_HISTORY' AND column_name IN
          ('COLLATERAL_ID','COLLATERAL_VALUATION_HISTORY_ID','VALUATION_STATUS_CODE','ELIGIBLE_VALUE','EFFECTIVE_FROM','EFFECTIVE_TO'))
     OR (table_name = 'COLLATERAL_TYPE' AND column_name IN
          ('STATUS','RESERVE_RATIO','USAGE_PERCENT_LOAN','USAGE_PERCENT_GUARANTEE','USAGE_PERCENT_LETTERCREDIT','USAGE_PERCENT_GREENLICENSE'))
     OR (table_name = 'REF_COLLATERAL_STATUS' AND column_name IN
          ('COLLATERAL_STATUS_CODE','IS_ACTIVE_STATUS_FLAG')))
 ORDER BY table_name, column_name;

SELECT table_name, constraint_name, constraint_type, status
  FROM all_constraints
 WHERE owner='CLT' AND table_name IN
 ('COLLATERAL','COLLATERAL_ALLOCATION','COLLATERAL_VALUATION_HISTORY','COLLATERAL_TYPE','REF_COLLATERAL_STATUS')
 ORDER BY table_name, constraint_type, constraint_name;
