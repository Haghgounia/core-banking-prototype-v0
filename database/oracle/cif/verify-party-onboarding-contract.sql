-- Read-only verification for Party onboarding physical contract.
-- Run as a user that can query ALL_TAB_COLUMNS / ALL_CONSTRAINTS / ALL_CONS_COLUMNS
-- and select the CIF/GEO reference tables.

PROMPT === Required columns used by atomic Party onboarding ===
SELECT owner, table_name, column_id, column_name, data_type, nullable
FROM all_tab_columns
WHERE owner = 'CIF'
  AND table_name IN ('PARTY','PARTY_NAME','PERSON','ORGANIZATION','PARTY_IDENTIFIER','PARTY_STATUS_HISTORY')
  AND nullable = 'N'
ORDER BY table_name, column_id;

PROMPT === ORGANIZATION registration country contract ===
SELECT owner, table_name, column_name, data_type, data_length, nullable
FROM all_tab_columns
WHERE owner = 'CIF'
  AND table_name = 'ORGANIZATION'
  AND column_name = 'REGISTRATION_COUNTRY_CODE';

PROMPT === Enabled FKs used by Party onboarding ===
SELECT c.constraint_name,
       c.table_name child_table,
       cc.column_name child_column,
       p.table_name parent_table,
       pc.column_name parent_column,
       c.status
FROM all_constraints c
JOIN all_cons_columns cc
  ON cc.owner = c.owner AND cc.constraint_name = c.constraint_name
JOIN all_constraints p
  ON p.owner = c.r_owner AND p.constraint_name = c.r_constraint_name
JOIN all_cons_columns pc
  ON pc.owner = p.owner AND pc.constraint_name = p.constraint_name AND pc.position = cc.position
WHERE c.owner = 'CIF'
  AND c.constraint_type = 'R'
  AND c.table_name IN ('PARTY','PARTY_NAME','ORGANIZATION','PARTY_IDENTIFIER','PARTY_STATUS_HISTORY')
ORDER BY child_table, c.constraint_name, cc.position;

PROMPT === Party base reference values expected by onboarding ===
SELECT 'REF_PARTY_TYPE' ref_table, party_type_code code, name_fa, is_active FROM cif.ref_party_type WHERE party_type_code IN ('PERSON','ORGANIZATION')
UNION ALL
SELECT 'REF_PARTY_LIFECYCLE_STATUS', lifecycle_status_code, name_fa, is_active FROM cif.ref_party_lifecycle_status WHERE lifecycle_status_code = 'ACTIVE'
UNION ALL
SELECT 'REF_PARTY_STATUS_REASON', reason_code, name_fa, is_active FROM cif.ref_party_status_reason WHERE reason_code = 'NEW_REGISTRATION'
UNION ALL
SELECT 'REF_VERIFICATION_STATUS', verification_status_code, name_fa, is_active FROM cif.ref_verification_status WHERE verification_status_code = 'UNVERIFIED'
UNION ALL
SELECT 'REF_DATA_QUALITY_STATUS', data_quality_status_code, name_fa, is_active FROM cif.ref_data_quality_status WHERE data_quality_status_code = 'INCOMPLETE'
UNION ALL
SELECT 'REF_DATA_SOURCE', data_source_code, name_fa, is_active FROM cif.ref_data_source WHERE data_source_code = 'CORE'
UNION ALL
SELECT 'REF_SCRIPT', script_code, name_fa, is_active FROM cif.ref_script WHERE script_code = 'ARAB'
ORDER BY ref_table, code;

PROMPT === Registration country IRN ===
SELECT country_id, country_iso_code, country_iso_code2, country_name, country_english_name, is_active
FROM geo.countries
WHERE country_iso_code = 'IRN';
