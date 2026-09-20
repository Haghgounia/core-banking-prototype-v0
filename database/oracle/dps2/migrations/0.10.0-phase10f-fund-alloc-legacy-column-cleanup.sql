SET SERVEROUTPUT ON
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

PROMPT ============================================================
PROMPT DPS2 Phase 10F - FUND_ALLOC legacy column cleanup
PROMPT Contract: ALLOCATED_AMOUNT is the canonical Phase 10 v5 column
PROMPT ============================================================

DECLARE
  v_legacy_count NUMBER := 0;
  v_new_count    NUMBER := 0;
  v_row_count    NUMBER := 0;
BEGIN
  SELECT COUNT(*) INTO v_legacy_count
    FROM ALL_TAB_COLUMNS
   WHERE OWNER = 'DPS2'
     AND TABLE_NAME = 'DEPOSIT_OPENING_FUND_ALLOC'
     AND COLUMN_NAME = 'ALLOCATION_AMOUNT';

  SELECT COUNT(*) INTO v_new_count
    FROM ALL_TAB_COLUMNS
   WHERE OWNER = 'DPS2'
     AND TABLE_NAME = 'DEPOSIT_OPENING_FUND_ALLOC'
     AND COLUMN_NAME = 'ALLOCATED_AMOUNT';

  EXECUTE IMMEDIATE 'SELECT COUNT(*) FROM DPS2.DEPOSIT_OPENING_FUND_ALLOC' INTO v_row_count;
  DBMS_OUTPUT.PUT_LINE('Existing row count: ' || v_row_count);

  IF v_new_count = 0 THEN
    RAISE_APPLICATION_ERROR(-20001, 'Canonical column ALLOCATED_AMOUNT is missing; run FUND_ALLOC column reconciliation first.');
  END IF;

  IF v_legacy_count = 0 THEN
    DBMS_OUTPUT.PUT_LINE('OK  : legacy column ALLOCATION_AMOUNT is already absent.');
  ELSIF v_row_count > 0 THEN
    RAISE_APPLICATION_ERROR(-20002, 'Legacy column ALLOCATION_AMOUNT exists and table contains business rows. Cleanup stopped to avoid data loss.');
  ELSE
    EXECUTE IMMEDIATE 'ALTER TABLE DPS2.DEPOSIT_OPENING_FUND_ALLOC DROP COLUMN ALLOCATION_AMOUNT CASCADE CONSTRAINTS';
    DBMS_OUTPUT.PUT_LINE('DROP: DPS2.DEPOSIT_OPENING_FUND_ALLOC.ALLOCATION_AMOUNT');
  END IF;
END;
/

PROMPT === Verification ===
SELECT COLUMN_ID, COLUMN_NAME, DATA_TYPE, DATA_PRECISION, DATA_SCALE, NULLABLE, DATA_DEFAULT
  FROM ALL_TAB_COLUMNS
 WHERE OWNER = 'DPS2'
   AND TABLE_NAME = 'DEPOSIT_OPENING_FUND_ALLOC'
 ORDER BY COLUMN_ID;

DECLARE
  v_legacy_count NUMBER;
  v_new_count    NUMBER;
BEGIN
  SELECT COUNT(*) INTO v_legacy_count
    FROM ALL_TAB_COLUMNS
   WHERE OWNER = 'DPS2'
     AND TABLE_NAME = 'DEPOSIT_OPENING_FUND_ALLOC'
     AND COLUMN_NAME = 'ALLOCATION_AMOUNT';

  SELECT COUNT(*) INTO v_new_count
    FROM ALL_TAB_COLUMNS
   WHERE OWNER = 'DPS2'
     AND TABLE_NAME = 'DEPOSIT_OPENING_FUND_ALLOC'
     AND COLUMN_NAME = 'ALLOCATED_AMOUNT';

  IF v_legacy_count <> 0 OR v_new_count <> 1 THEN
    RAISE_APPLICATION_ERROR(-20003, 'FUND_ALLOC amount-column contract verification failed.');
  END IF;
END;
/

PROMPT ============================================================
PROMPT SUCCESS: Phase 10F FUND_ALLOC legacy column cleaned up.
PROMPT Canonical column: ALLOCATED_AMOUNT NUMBER(19,4)
PROMPT No existing business row was modified.
PROMPT ============================================================
