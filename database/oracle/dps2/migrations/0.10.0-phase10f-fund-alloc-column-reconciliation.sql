SET DEFINE OFF
SET SERVEROUTPUT ON
SET FEEDBACK ON
SET VERIFY OFF
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

PROMPT ============================================================
PROMPT DPS2 Phase 10F - FUND_ALLOC column reconciliation
PROMPT Contract: DEPOSIT_OPENING_FUND_ALLOC must match Phase 10 v5
PROMPT ============================================================

DECLARE
  v_table_count NUMBER;
  v_row_count NUMBER;

  FUNCTION column_exists(p_column VARCHAR2) RETURN BOOLEAN IS
    v_count NUMBER;
  BEGIN
    SELECT COUNT(*) INTO v_count
      FROM ALL_TAB_COLUMNS
     WHERE OWNER='DPS2'
       AND TABLE_NAME='DEPOSIT_OPENING_FUND_ALLOC'
       AND COLUMN_NAME=UPPER(p_column);
    RETURN v_count > 0;
  END;

  PROCEDURE add_required(p_column VARCHAR2, p_ddl VARCHAR2) IS
  BEGIN
    IF column_exists(p_column) THEN
      DBMS_OUTPUT.PUT_LINE('OK  : DPS2.DEPOSIT_OPENING_FUND_ALLOC.'||p_column);
    ELSE
      IF v_row_count > 0 THEN
        RAISE_APPLICATION_ERROR(
          -21061,
          'Cannot safely add required column '||p_column||
          ' because DPS2.DEPOSIT_OPENING_FUND_ALLOC contains '||v_row_count||
          ' existing row(s). Reconcile legacy data explicitly before rerunning.'
        );
      END IF;
      EXECUTE IMMEDIATE
        'ALTER TABLE DPS2.DEPOSIT_OPENING_FUND_ALLOC ADD ('||p_column||' '||p_ddl||' NOT NULL)';
      DBMS_OUTPUT.PUT_LINE('ADD : DPS2.DEPOSIT_OPENING_FUND_ALLOC.'||p_column||' '||p_ddl||' NOT NULL');
    END IF;
  END;

  PROCEDURE add_required_default(p_column VARCHAR2, p_ddl VARCHAR2, p_default VARCHAR2) IS
  BEGIN
    IF column_exists(p_column) THEN
      DBMS_OUTPUT.PUT_LINE('OK  : DPS2.DEPOSIT_OPENING_FUND_ALLOC.'||p_column);
    ELSE
      EXECUTE IMMEDIATE
        'ALTER TABLE DPS2.DEPOSIT_OPENING_FUND_ALLOC ADD ('||p_column||' '||p_ddl||
        ' DEFAULT '||p_default||' NOT NULL)';
      DBMS_OUTPUT.PUT_LINE('ADD : DPS2.DEPOSIT_OPENING_FUND_ALLOC.'||p_column||
                           ' DEFAULT '||p_default||' NOT NULL');
    END IF;
  END;

  PROCEDURE add_optional(p_column VARCHAR2, p_ddl VARCHAR2) IS
  BEGIN
    IF column_exists(p_column) THEN
      DBMS_OUTPUT.PUT_LINE('OK  : DPS2.DEPOSIT_OPENING_FUND_ALLOC.'||p_column);
    ELSE
      EXECUTE IMMEDIATE
        'ALTER TABLE DPS2.DEPOSIT_OPENING_FUND_ALLOC ADD ('||p_column||' '||p_ddl||')';
      DBMS_OUTPUT.PUT_LINE('ADD : DPS2.DEPOSIT_OPENING_FUND_ALLOC.'||p_column||' '||p_ddl);
    END IF;
  END;
BEGIN
  SELECT COUNT(*) INTO v_table_count
    FROM ALL_TABLES
   WHERE OWNER='DPS2'
     AND TABLE_NAME='DEPOSIT_OPENING_FUND_ALLOC';

  IF v_table_count = 0 THEN
    RAISE_APPLICATION_ERROR(-21060,
      'Required table DPS2.DEPOSIT_OPENING_FUND_ALLOC is missing. Apply Phase 10 foundation first.');
  END IF;

  SELECT COUNT(*) INTO v_row_count FROM DPS2.DEPOSIT_OPENING_FUND_ALLOC;
  DBMS_OUTPUT.PUT_LINE('Existing row count: '||v_row_count);

  -- Phase 10 v5 canonical column contract.
  add_required('OPENING_FUND_ALLOC_ID', 'NUMBER(19)');
  add_required('OPENING_FUNDING_ID', 'NUMBER(19)');
  add_required('OPENING_OBLIGATION_ID', 'NUMBER(19)');
  add_required('ALLOCATED_AMOUNT', 'NUMBER(19,4)');
  add_required('ALLOCATION_STATUS_CODE', 'VARCHAR2(30)');
  add_optional('SETTLEMENT_REFERENCE', 'VARCHAR2(120)');
  add_required_default('CREATED_AT', 'TIMESTAMP(6)', 'SYSTIMESTAMP');
  add_required('CREATED_BY', 'VARCHAR2(100)');
  add_optional('UPDATED_AT', 'TIMESTAMP(6)');
  add_optional('UPDATED_BY', 'VARCHAR2(100)');
  add_required_default('RECORD_VERSION', 'NUMBER(19)', '1');
END;
/

PROMPT
PROMPT === Verification: Phase 10 v5 FUND_ALLOC columns ===
COLUMN COLUMN_NAME FORMAT A32
COLUMN DATA_TYPE FORMAT A20
COLUMN NULLABLE FORMAT A8
COLUMN DATA_DEFAULT FORMAT A24
SELECT COLUMN_ID,
       COLUMN_NAME,
       DATA_TYPE,
       DATA_PRECISION,
       DATA_SCALE,
       CHAR_LENGTH,
       NULLABLE,
       DATA_DEFAULT
  FROM ALL_TAB_COLUMNS
 WHERE OWNER='DPS2'
   AND TABLE_NAME='DEPOSIT_OPENING_FUND_ALLOC'
 ORDER BY COLUMN_ID;

DECLARE
  v_missing NUMBER;
BEGIN
  SELECT COUNT(*) INTO v_missing
    FROM (
      SELECT 'OPENING_FUND_ALLOC_ID' c FROM dual UNION ALL
      SELECT 'OPENING_FUNDING_ID' FROM dual UNION ALL
      SELECT 'OPENING_OBLIGATION_ID' FROM dual UNION ALL
      SELECT 'ALLOCATED_AMOUNT' FROM dual UNION ALL
      SELECT 'ALLOCATION_STATUS_CODE' FROM dual UNION ALL
      SELECT 'SETTLEMENT_REFERENCE' FROM dual UNION ALL
      SELECT 'CREATED_AT' FROM dual UNION ALL
      SELECT 'CREATED_BY' FROM dual UNION ALL
      SELECT 'UPDATED_AT' FROM dual UNION ALL
      SELECT 'UPDATED_BY' FROM dual UNION ALL
      SELECT 'RECORD_VERSION' FROM dual
    ) expected
   WHERE NOT EXISTS (
     SELECT 1
       FROM ALL_TAB_COLUMNS c
      WHERE c.OWNER='DPS2'
        AND c.TABLE_NAME='DEPOSIT_OPENING_FUND_ALLOC'
        AND c.COLUMN_NAME=expected.c
   );

  IF v_missing <> 0 THEN
    RAISE_APPLICATION_ERROR(-21062,
      'FUND_ALLOC reconciliation incomplete; missing expected column count='||v_missing);
  END IF;
END;
/

PROMPT ============================================================
PROMPT SUCCESS: Phase 10F FUND_ALLOC column contract reconciled.
PROMPT NOTE: No existing business row was inserted, updated or deleted.
PROMPT ============================================================
