-- PB-R15 / read-only Oracle diagnostics. No DML, DDL, COMMIT or rollback.
-- Run as a user who can read ALL_CONSTRAINTS and PDL product tables.
-- Set target version here (example from the reported Product Builder screen):
DEFINE PRODUCT_VERSION_ID = 26

PROMPT Primary/unique constraints of profile and opening rules
SELECT c.table_name, c.constraint_name, c.constraint_type,
       LISTAGG(cc.column_name, ', ') WITHIN GROUP (ORDER BY cc.position) AS constrained_columns
  FROM ALL_CONSTRAINTS c
  JOIN ALL_CONS_COLUMNS cc ON cc.owner = c.owner AND cc.constraint_name = c.constraint_name
 WHERE c.owner = 'PDL'
   AND c.table_name IN ('DEPOSIT_PRODUCT_PROFILE', 'DEPOSIT_PRODUCT_OPENING_RULE')
   AND c.constraint_type IN ('P','U')
 GROUP BY c.table_name, c.constraint_name, c.constraint_type
 ORDER BY c.table_name, c.constraint_type, c.constraint_name;

PROMPT Unique indexes that may enforce further uniqueness
SELECT i.table_name, i.index_name, i.uniqueness,
       LISTAGG(ic.column_name, ', ') WITHIN GROUP (ORDER BY ic.column_position) AS indexed_columns
  FROM ALL_INDEXES i
  JOIN ALL_IND_COLUMNS ic ON ic.index_owner = i.owner AND ic.index_name = i.index_name
 WHERE i.table_owner = 'PDL'
   AND i.table_name IN ('DEPOSIT_PRODUCT_PROFILE', 'DEPOSIT_PRODUCT_OPENING_RULE')
   AND i.uniqueness = 'UNIQUE'
 GROUP BY i.table_name, i.index_name, i.uniqueness
 ORDER BY i.table_name, i.index_name;

PROMPT Existing data for the selected product version (read-only)
SELECT 'DEPOSIT_PRODUCT_PROFILE' AS table_name, COUNT(*) AS record_count
  FROM PDL.DEPOSIT_PRODUCT_PROFILE WHERE PRODUCT_VERSION_ID = &PRODUCT_VERSION_ID
UNION ALL
SELECT 'DEPOSIT_PRODUCT_OPENING_RULE', COUNT(*)
  FROM PDL.DEPOSIT_PRODUCT_OPENING_RULE WHERE PRODUCT_VERSION_ID = &PRODUCT_VERSION_ID;
