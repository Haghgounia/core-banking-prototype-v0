SET SERVEROUTPUT ON
SET DEFINE OFF

DECLARE
  v_table_count NUMBER;
  v_scope_count NUMBER;
BEGIN
  SELECT COUNT(*)
    INTO v_table_count
    FROM ALL_TABLES
   WHERE OWNER = 'FEE2'
     AND TABLE_NAME = 'FEE_SCOPE';

  IF v_table_count <> 1 THEN
    RAISE_APPLICATION_ERROR(-20001, 'FEE2.FEE_SCOPE table is missing. Apply the FEE2 schema before this reconciliation.');
  END IF;

  SELECT COUNT(*)
    INTO v_scope_count
    FROM FEE2.FEE_SCOPE
   WHERE SCOPE_CODE = 'BANK';

  IF v_scope_count = 0 THEN
    INSERT INTO FEE2.FEE_SCOPE(
      ID, SCOPE_CODE, NAME_FA, NAME_EN, STATUS, TIME_ZONE,
      CREATED_AT, CREATED_BY, ROW_VERSION
    ) VALUES (
      '5f9ec6d1-2a81-4d7c-b442-3e8fd3028a10',
      'BANK', 'بانک', 'Bank', 'ACTIVE', 'Asia/Tehran',
      SYSTIMESTAMP, 'fee2-scope-baseline', 0
    );
    DBMS_OUTPUT.PUT_LINE('Inserted FEE2 scope: BANK - بانک');
  ELSE
    DBMS_OUTPUT.PUT_LINE('FEE2 scope BANK already exists; no insert performed.');
  END IF;

  SELECT COUNT(*)
    INTO v_scope_count
    FROM FEE2.FEE_SCOPE
   WHERE SCOPE_CODE = 'BANK'
     AND STATUS = 'ACTIVE';

  IF v_scope_count <> 1 THEN
    RAISE_APPLICATION_ERROR(-20002, 'FEE2 scope BANK exists but is not ACTIVE or is duplicated. Review FEE2.FEE_SCOPE.');
  END IF;

  COMMIT;
  DBMS_OUTPUT.PUT_LINE('FEE2_STUDIO_SCOPE_BASELINE_PASS');
EXCEPTION
  WHEN OTHERS THEN
    ROLLBACK;
    RAISE;
END;
/
