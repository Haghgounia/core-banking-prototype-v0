-- CLT reference governance: read-only Oracle 19c+ diagnostics; no DDL/DML/COMMIT.
-- Execute under a suitably authorized SQLcl/SQL Developer session before requesting any migration approval.
SELECT table_name FROM all_tables WHERE owner = 'CLT'
  AND table_name IN ('COLLATERAL_GROUP','COLLATERAL_TYPE','COLLATERAL_ATTRIBUTE_DEFINITION',
                     'COLLATERAL_TYPE_ATTRIBUTE','COLLATERAL_INSURANCE_TYPE','INSURANCE_COMPANY',
                     'CLT_REFERENCE_CHANGE_REQUEST','CLT_REFERENCE_CHANGE_AUDIT')
 ORDER BY table_name;
SELECT sequence_name FROM all_sequences WHERE sequence_owner = 'CLT'
  AND sequence_name IN ('SEQ_COLLATERAL_GROUP','SEQ_COLLATERAL_TYPE',
                        'SEQ_COLLATERAL_ATTRIBUTE_DEFINITION','SEQ_COLLATERAL_TYPE_ATTRIBUTE',
                        'SEQ_COLLATERAL_INSURANCE_TYPE','SEQ_INSURANCE_COMPANY',
                        'SEQ_CLT_REFERENCE_CHANGE_REQUEST','SEQ_CLT_REFERENCE_CHANGE_AUDIT')
 ORDER BY sequence_name;
SELECT table_name, column_name, data_type, data_length, char_used, data_precision, data_scale,
       nullable, data_default FROM all_tab_columns
 WHERE owner='CLT' AND table_name IN ('COLLATERAL_GROUP','COLLATERAL_TYPE',
       'COLLATERAL_ATTRIBUTE_DEFINITION','COLLATERAL_TYPE_ATTRIBUTE',
       'COLLATERAL_INSURANCE_TYPE','INSURANCE_COMPANY')
 ORDER BY table_name,column_id;
