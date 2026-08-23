-- Read-only verification for CIF Party onboarding reference FKs.
-- No DDL/DML is performed.
SET PAGESIZE 200
SET LINESIZE 220

PROMPT === Relevant enabled foreign keys ===
SELECT c.constraint_name,
       cc.table_name AS child_table,
       cc.column_name AS child_column,
       pc.table_name AS parent_table,
       pcc.column_name AS parent_column,
       c.status
  FROM all_constraints c
  JOIN all_cons_columns cc
    ON cc.owner = c.owner AND cc.constraint_name = c.constraint_name
  JOIN all_constraints pc
    ON pc.owner = c.r_owner AND pc.constraint_name = c.r_constraint_name
  JOIN all_cons_columns pcc
    ON pcc.owner = pc.owner AND pcc.constraint_name = pc.constraint_name
   AND pcc.position = cc.position
 WHERE c.owner = 'CIF'
   AND c.constraint_type = 'R'
   AND cc.table_name IN ('PARTY','PARTY_NAME','ORGANIZATION','PARTY_IDENTIFIER')
 ORDER BY cc.table_name, cc.position, c.constraint_name;

PROMPT === Script codes expected by PARTY_NAME.SCRIPT_CODE ===
SELECT script_code, name_fa, name_en, is_active
  FROM cif.ref_script
 ORDER BY sort_order, script_code;

PROMPT === Data source codes expected by PARTY.CREATION_SOURCE_CODE and PARTY_NAME.SOURCE_CODE ===
SELECT data_source_code, name_fa, name_en, is_active
  FROM cif.ref_data_source
 ORDER BY sort_order, data_source_code;

PROMPT === Source-system codes (different domain; should not drive PARTY.CREATION_SOURCE_CODE) ===
SELECT source_system_code, name_fa, name_en, is_active
  FROM cif.ref_source_system
 ORDER BY sort_order, source_system_code;

PROMPT === Required Party creation defaults ===
SELECT 'REF_PARTY_TYPE' ref_table, party_type_code code, name_fa, is_active
  FROM cif.ref_party_type
 WHERE party_type_code IN ('PERSON','ORGANIZATION')
UNION ALL
SELECT 'REF_PARTY_LIFECYCLE_STATUS', lifecycle_status_code, name_fa, is_active
  FROM cif.ref_party_lifecycle_status
 WHERE lifecycle_status_code = 'ACTIVE'
UNION ALL
SELECT 'REF_PARTY_STATUS_REASON', reason_code, name_fa, is_active
  FROM cif.ref_party_status_reason
 WHERE reason_code = 'NEW_REGISTRATION'
UNION ALL
SELECT 'REF_VERIFICATION_STATUS', verification_status_code, name_fa, is_active
  FROM cif.ref_verification_status
 WHERE verification_status_code = 'UNVERIFIED'
UNION ALL
SELECT 'REF_DATA_QUALITY_STATUS', data_quality_status_code, name_fa, is_active
  FROM cif.ref_data_quality_status
 WHERE data_quality_status_code = 'INCOMPLETE';
