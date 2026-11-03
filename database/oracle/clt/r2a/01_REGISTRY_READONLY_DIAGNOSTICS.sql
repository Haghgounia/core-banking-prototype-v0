-- Optional DBA/SQLcl diagnostics. Read only. Do not run as the application account.
SET DEFINE OFF
SELECT table_name FROM all_tables WHERE owner = 'CLT'
AND table_name IN ('COLLATERAL','COLLATERAL_TYPE','COLLATERAL_GROUP',
                   'COLLATERAL_TYPE_ATTRIBUTE','COLLATERAL_ATTRIBUTE_DEFINITION')
ORDER BY table_name;
SELECT column_name, data_type, data_precision, data_scale, char_used
FROM all_tab_columns WHERE owner = 'CLT' AND table_name = 'COLLATERAL'
ORDER BY column_id;
SELECT owner, table_name, privilege, grantee FROM all_tab_privs
WHERE owner = 'CLT' AND table_name IN ('COLLATERAL','COLLATERAL_TYPE','COLLATERAL_GROUP',
 'COLLATERAL_TYPE_ATTRIBUTE','COLLATERAL_ATTRIBUTE_DEFINITION')
ORDER BY grantee, table_name, privilege;
