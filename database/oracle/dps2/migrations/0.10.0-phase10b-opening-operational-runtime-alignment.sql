SET DEFINE OFF
SET SERVEROUTPUT ON SIZE UNLIMITED
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

PROMPT ============================================================
PROMPT DPS2 Phase 10B - Operational Opening v5 Runtime Alignment 0.10.0
PROMPT Purpose: allow funding-plan persistence before a real settlement attempt
PROMPT ============================================================

DECLARE
  v_cnt      PLS_INTEGER;
  v_nullable VARCHAR2(1);
BEGIN
  SELECT COUNT(*)
    INTO v_cnt
    FROM ALL_TABLES
   WHERE OWNER='DPS2'
     AND TABLE_NAME='DEPOSIT_OPENING_FUNDING';

  IF v_cnt <> 1 THEN
    RAISE_APPLICATION_ERROR(-21050,
      'DPS2.DEPOSIT_OPENING_FUNDING is missing. Apply Phase 10A first.');
  END IF;

  SELECT COUNT(*)
    INTO v_cnt
    FROM ALL_TAB_COLUMNS
   WHERE OWNER='DPS2'
     AND TABLE_NAME='DEPOSIT_OPENING_FUNDING'
     AND COLUMN_NAME='ATTEMPT_AT';

  IF v_cnt <> 1 THEN
    RAISE_APPLICATION_ERROR(-21051,
      'DPS2.DEPOSIT_OPENING_FUNDING.ATTEMPT_AT is missing. Apply Phase 10A first.');
  END IF;

  SELECT NULLABLE
    INTO v_nullable
    FROM ALL_TAB_COLUMNS
   WHERE OWNER='DPS2'
     AND TABLE_NAME='DEPOSIT_OPENING_FUNDING'
     AND COLUMN_NAME='ATTEMPT_AT';

  IF v_nullable = 'N' THEN
    EXECUTE IMMEDIATE
      'ALTER TABLE DPS2.DEPOSIT_OPENING_FUNDING MODIFY (ATTEMPT_AT NULL)';
    DBMS_OUTPUT.PUT_LINE('ALTER: DEPOSIT_OPENING_FUNDING.ATTEMPT_AT is now nullable.');
  ELSE
    DBMS_OUTPUT.PUT_LINE('OK   : DEPOSIT_OPENING_FUNDING.ATTEMPT_AT is already nullable.');
  END IF;
END;
/

PROMPT === Verification ===
SELECT OWNER, TABLE_NAME, COLUMN_NAME, DATA_TYPE, NULLABLE
  FROM ALL_TAB_COLUMNS
 WHERE OWNER='DPS2'
   AND TABLE_NAME='DEPOSIT_OPENING_FUNDING'
   AND COLUMN_NAME='ATTEMPT_AT';

DECLARE
  v_nullable VARCHAR2(1);
BEGIN
  SELECT NULLABLE
    INTO v_nullable
    FROM ALL_TAB_COLUMNS
   WHERE OWNER='DPS2'
     AND TABLE_NAME='DEPOSIT_OPENING_FUNDING'
     AND COLUMN_NAME='ATTEMPT_AT';

  IF v_nullable <> 'Y' THEN
    RAISE_APPLICATION_ERROR(-21052,
      'ATTEMPT_AT must be nullable for pre-settlement funding plans.');
  END IF;

  DBMS_OUTPUT.PUT_LINE('SUCCESS: Phase 10B runtime schema alignment completed.');
  DBMS_OUTPUT.PUT_LINE('NOTE: ATTEMPT_AT remains NULL while funding is only planned; Settlement sets it to SYSTIMESTAMP on the first real attempt.');
END;
/

COMMIT;
