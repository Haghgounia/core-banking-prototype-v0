-- ============================================================================
-- Core Banking Prototype 0.10.0
-- DPS2 Phase 10F - Deposit Opening CREATED_AT default reconciliation
--
-- Purpose
--   Dedicated Deposit Opening repositories intentionally omit CREATED_AT and
--   rely on the Oracle audit-column contract DEFAULT SYSTIMESTAMP.
--   Some installed baseline tables can have CREATED_AT NOT NULL without a
--   default, which causes ORA-01400 during aggregate persistence.
--
-- Scope
--   DPS2.DEPOSIT_OPENING_% tables that physically contain CREATED_AT.
--
-- Safety
--   * Idempotent: reapplying DEFAULT SYSTIMESTAMP is harmless.
--   * No existing row values are updated.
--   * Nullability, datatype, PK/FK/check constraints and data are preserved.
-- ============================================================================

SET SERVEROUTPUT ON SIZE UNLIMITED
SET VERIFY OFF
SET FEEDBACK ON
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

PROMPT ============================================================
PROMPT DPS2 Phase 10F - CREATED_AT default reconciliation
PROMPT ============================================================

DECLARE
  v_count PLS_INTEGER := 0;
BEGIN
  FOR r IN (
    SELECT table_name
      FROM all_tab_columns
     WHERE owner = 'DPS2'
       AND table_name LIKE 'DEPOSIT_OPENING\_%' ESCAPE '\'
       AND column_name = 'CREATED_AT'
     ORDER BY table_name
  ) LOOP
    EXECUTE IMMEDIATE
      'ALTER TABLE DPS2.' || DBMS_ASSERT.SIMPLE_SQL_NAME(r.table_name) ||
      ' MODIFY (CREATED_AT DEFAULT SYSTIMESTAMP)';
    v_count := v_count + 1;
    DBMS_OUTPUT.PUT_LINE('OK  : DPS2.' || r.table_name || '.CREATED_AT DEFAULT SYSTIMESTAMP');
  END LOOP;

  IF v_count = 0 THEN
    RAISE_APPLICATION_ERROR(-20910,
      'No DPS2.DEPOSIT_OPENING_% table with CREATED_AT was found.');
  END IF;

  DBMS_OUTPUT.PUT_LINE('Reconciled table count: ' || v_count);
END;
/

PROMPT === Verification: CREATED_AT defaults ===
SET LONG 200
SET LONGCHUNKSIZE 200
COLUMN TABLE_NAME FORMAT A45
COLUMN NULLABLE FORMAT A8
COLUMN DATA_DEFAULT FORMAT A40

SELECT table_name,
       nullable,
       data_default
  FROM all_tab_columns
 WHERE owner = 'DPS2'
   AND table_name LIKE 'DEPOSIT_OPENING\_%' ESCAPE '\'
   AND column_name = 'CREATED_AT'
 ORDER BY table_name;

PROMPT ============================================================
PROMPT SUCCESS: Phase 10F CREATED_AT default contract reconciled.
PROMPT No existing business data was modified.
PROMPT ============================================================
