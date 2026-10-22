-- CAL2 PERSONAL R1C: DATA-PRESERVING RENAME from the already-created R1B numeric table.
-- SQL Developer F5. Run while CONNECTED AS CAL2 (Oracle RENAME requires object ownership).
-- STOP APP, BACK UP CAL2 SCHEMA and save previous app source before APPLY.
-- Oracle DDL has implicit commits. A failed APPLY may require manual, step-specific recovery.
-- No DROP, TRUNCATE, DELETE or recreation of IDs; old numeric IDs and all rows stay in place.
-- Default PREVIEW. Change only the DEFINE below to YES after a successful preview.
SET SERVEROUTPUT ON SIZE UNLIMITED
SET DEFINE ON
DEFINE APPLY_CHANGES = NO
PROMPT === CAL2 PERSONAL R1C: rename ITEMS to ENTRIES (data preserving) ===
DECLARE
  v_apply        VARCHAR2(3) := UPPER(TRIM('&&APPLY_CHANGES'));
  v_old_table    NUMBER;
  v_new_table    NUMBER;
  v_old_seq      NUMBER;
  v_new_seq      NUMBER;
  v_count        NUMBER;
  v_constraint   NUMBER;
  v_index        NUMBER;
  v_dependencies NUMBER;
  v_references   NUMBER;
  v_conflicts    NUMBER;
  v_rows_before  NUMBER;
  v_rows_after   NUMBER;
  v_started      BOOLEAN := FALSE;

  PROCEDURE assert_column(p_table VARCHAR2, p_column VARCHAR2, p_expected NUMBER) IS
    n NUMBER;
  BEGIN
    SELECT COUNT(*) INTO n FROM ALL_TAB_COLUMNS
     WHERE OWNER='CAL2' AND TABLE_NAME=p_table AND COLUMN_NAME=p_column;
    IF n <> p_expected THEN
      RAISE_APPLICATION_ERROR(-20741, 'Unexpected column: '||p_table||'.'||p_column);
    END IF;
  END;
  PROCEDURE assert_constraint(p_name VARCHAR2, p_expected NUMBER) IS
    n NUMBER;
  BEGIN
    SELECT COUNT(*) INTO n FROM ALL_CONSTRAINTS
     WHERE OWNER='CAL2' AND CONSTRAINT_NAME=p_name AND STATUS='ENABLED' AND VALIDATED='VALIDATED';
    IF n <> p_expected THEN RAISE_APPLICATION_ERROR(-20742, 'Missing/invalid constraint or name collision: '||p_name); END IF;
  END;
  PROCEDURE assert_index(p_name VARCHAR2, p_expected NUMBER) IS
    n NUMBER;
  BEGIN
    SELECT COUNT(*) INTO n FROM ALL_INDEXES
     WHERE OWNER='CAL2' AND INDEX_NAME=p_name AND STATUS='VALID';
    IF n <> p_expected THEN RAISE_APPLICATION_ERROR(-20743, 'Missing/invalid index or name collision: '||p_name); END IF;
  END;
  PROCEDURE rename_constraint(p_old VARCHAR2, p_new VARCHAR2) IS
  BEGIN
    EXECUTE IMMEDIATE 'ALTER TABLE CAL2.USER_CALENDAR_ENTRIES RENAME CONSTRAINT '||p_old||' TO '||p_new;
    DBMS_OUTPUT.PUT_LINE('RENAMED CONSTRAINT: '||p_old||' -> '||p_new);
  END;
  PROCEDURE rename_index(p_old VARCHAR2, p_new VARCHAR2) IS
  BEGIN
    EXECUTE IMMEDIATE 'ALTER INDEX CAL2.'||p_old||' RENAME TO '||p_new;
    DBMS_OUTPUT.PUT_LINE('RENAMED INDEX: '||p_old||' -> '||p_new);
  END;
BEGIN
  IF v_apply NOT IN ('YES', 'NO') THEN
    RAISE_APPLICATION_ERROR(-20740, 'APPLY_CHANGES must be YES or NO');
  END IF;
  IF SYS_CONTEXT('USERENV','SESSION_USER') <> 'CAL2' THEN
    RAISE_APPLICATION_ERROR(-20744, 'Connect to Oracle as CAL2; RENAME of TABLE/SEQUENCE requires ownership. NO CHANGES.');
  END IF;
  SELECT COUNT(*) INTO v_old_table FROM ALL_TABLES WHERE OWNER='CAL2' AND TABLE_NAME='USER_CALENDAR_ITEMS';
  SELECT COUNT(*) INTO v_new_table FROM ALL_TABLES WHERE OWNER='CAL2' AND TABLE_NAME='USER_CALENDAR_ENTRIES';
  SELECT COUNT(*) INTO v_old_seq FROM ALL_SEQUENCES WHERE SEQUENCE_OWNER='CAL2' AND SEQUENCE_NAME='SEQ_CAL2_USER_CALENDAR_ITEMS';
  SELECT COUNT(*) INTO v_new_seq FROM ALL_SEQUENCES WHERE SEQUENCE_OWNER='CAL2' AND SEQUENCE_NAME='SEQ_CAL2_USER_CALENDAR_ENTRIES';

  IF v_old_table=0 AND v_new_table=1 AND v_old_seq=0 AND v_new_seq=1 THEN
    assert_column('USER_CALENDAR_ENTRIES','USER_CALENDAR_ENTRY_ID',1);
    assert_column('USER_CALENDAR_ENTRIES','ENTRY_TYPE',1);
    assert_column('USER_CALENDAR_ENTRIES','USER_CALENDAR_ITEM_ID',0);
    assert_column('USER_CALENDAR_ENTRIES','ITEM_TYPE',0);
    assert_constraint('PK_CAL2_UCE',1);
    assert_constraint('FK_CAL2_UCE_DAY',1);
    assert_constraint('CK_CAL2_UCE_TYPE',1);
    assert_constraint('CK_CAL2_UCE_STATUS',1);
    assert_constraint('CK_CAL2_UCE_ADV',1);
    assert_constraint('CK_CAL2_UCE_REPEAT',1);
    assert_constraint('CK_CAL2_UCE_ACK',1);
    assert_constraint('CK_CAL2_UCE_REM_TIME',1);
    assert_index('PK_CAL2_UCE',1);
    assert_index('IX_CAL2_UCE_ACTOR_DAY',1);
    assert_index('IX_CAL2_UCE_ALERT',1);
    DBMS_OUTPUT.PUT_LINE('ALREADY_RENAMED: CAL2 USER_CALENDAR_ENTRIES; no changes. Run the R1C read-only verifier.');
    RETURN;
  END IF;
  IF v_old_table<>1 OR v_new_table<>0 OR v_old_seq<>1 OR v_new_seq<>0 THEN
    RAISE_APPLICATION_ERROR(-20745, 'Not a complete R1B starting layout (possibly partial rename). NO CHANGES; inspect manually.');
  END IF;
  assert_column('USER_CALENDAR_ITEMS','USER_CALENDAR_ITEM_ID',1);
  assert_column('USER_CALENDAR_ITEMS','ITEM_TYPE',1);
  assert_column('USER_CALENDAR_ITEMS','USER_CALENDAR_ENTRY_ID',0);
  assert_column('USER_CALENDAR_ITEMS','ENTRY_TYPE',0);
  SELECT COUNT(*) INTO v_count FROM ALL_TAB_COLUMNS
   WHERE OWNER='CAL2' AND TABLE_NAME='USER_CALENDAR_ITEMS'
     AND COLUMN_NAME='USER_CALENDAR_ITEM_ID' AND DATA_TYPE='NUMBER'
     AND DATA_PRECISION=12 AND DATA_SCALE=0 AND NULLABLE='N';
  IF v_count<>1 THEN RAISE_APPLICATION_ERROR(-20746, 'R1B numeric ID type mismatch; no changes.'); END IF;
  FOR c IN (SELECT COLUMN_VALUE old_name FROM TABLE(SYS.ODCIVARCHAR2LIST(
      'PK_CAL2_UCI','FK_CAL2_UCI_DAY','CK_CAL2_UCI_TYPE','CK_CAL2_UCI_STATUS',
      'CK_CAL2_UCI_ADV','CK_CAL2_UCI_REPEAT','CK_CAL2_UCI_ACK','CK_CAL2_UCI_REM_TIME'))) LOOP
    assert_constraint(c.old_name,1);
  END LOOP;
  FOR i IN (SELECT COLUMN_VALUE old_name FROM TABLE(SYS.ODCIVARCHAR2LIST(
      'PK_CAL2_UCI','IX_CAL2_UCI_ACTOR_DAY','IX_CAL2_UCI_ALERT'))) LOOP
    assert_index(i.old_name,1);
  END LOOP;
  SELECT COUNT(*) INTO v_conflicts FROM ALL_OBJECTS
   WHERE OWNER='CAL2' AND OBJECT_NAME IN (
     'USER_CALENDAR_ENTRIES','SEQ_CAL2_USER_CALENDAR_ENTRIES',
     'PK_CAL2_UCE','IX_CAL2_UCE_ACTOR_DAY','IX_CAL2_UCE_ALERT');
  SELECT COUNT(*) INTO v_constraint FROM ALL_CONSTRAINTS
   WHERE OWNER='CAL2' AND CONSTRAINT_NAME IN (
     'PK_CAL2_UCE','FK_CAL2_UCE_DAY','CK_CAL2_UCE_TYPE','CK_CAL2_UCE_STATUS',
     'CK_CAL2_UCE_ADV','CK_CAL2_UCE_REPEAT','CK_CAL2_UCE_ACK','CK_CAL2_UCE_REM_TIME');
  IF v_conflicts>0 OR v_constraint>0 THEN
    RAISE_APPLICATION_ERROR(-20747, 'One or more target object/constraint names are already occupied. NO CHANGES.');
  END IF;
  -- Fail closed: external database objects and incoming FKs require a coordinated migration.
  SELECT COUNT(*) INTO v_dependencies FROM ALL_DEPENDENCIES
   WHERE REFERENCED_OWNER='CAL2' AND REFERENCED_NAME IN
     ('USER_CALENDAR_ITEMS','SEQ_CAL2_USER_CALENDAR_ITEMS')
     AND TYPE NOT IN ('INDEX')
     -- The renamed table itself may depend on its default SEQUENCE; that is expected.
     AND NOT (OWNER='CAL2' AND NAME='USER_CALENDAR_ITEMS' AND TYPE='TABLE');
  SELECT COUNT(*) INTO v_references FROM ALL_CONSTRAINTS
   WHERE R_OWNER='CAL2' AND R_CONSTRAINT_NAME='PK_CAL2_UCI' AND CONSTRAINT_TYPE='R';
  DBMS_OUTPUT.PUT_LINE('DB_OBJECT_DEPENDENCIES='||v_dependencies||'; INCOMING_FKS='||v_references);
  IF v_dependencies>0 OR v_references>0 THEN
    RAISE_APPLICATION_ERROR(-20748, 'External database dependencies/FKs detected. Review impact manually; NO CHANGES.');
  END IF;
  EXECUTE IMMEDIATE 'SELECT COUNT(*) FROM CAL2.USER_CALENDAR_ITEMS' INTO v_rows_before;
  DBMS_OUTPUT.PUT_LINE('R1B_ROWS_BEFORE='||v_rows_before);
  DBMS_OUTPUT.PUT_LINE('PENDING: rename table, ID column, ENTRY_TYPE, sequence, 8 constraints, 3 indexes; rebind ID DEFAULT');
  IF v_apply='NO' THEN
    DBMS_OUTPUT.PUT_LINE('CAL2_PERSONAL_R1C_PREVIEW_PASS. No writes.');
    RETURN;
  END IF;

  v_started := TRUE;
  EXECUTE IMMEDIATE 'RENAME USER_CALENDAR_ITEMS TO USER_CALENDAR_ENTRIES';
  DBMS_OUTPUT.PUT_LINE('RENAMED TABLE: USER_CALENDAR_ENTRIES');
  EXECUTE IMMEDIATE 'ALTER TABLE CAL2.USER_CALENDAR_ENTRIES RENAME COLUMN USER_CALENDAR_ITEM_ID TO USER_CALENDAR_ENTRY_ID';
  EXECUTE IMMEDIATE 'ALTER TABLE CAL2.USER_CALENDAR_ENTRIES RENAME COLUMN ITEM_TYPE TO ENTRY_TYPE';
  DBMS_OUTPUT.PUT_LINE('RENAMED COLUMNS: USER_CALENDAR_ENTRY_ID, ENTRY_TYPE');
  EXECUTE IMMEDIATE 'RENAME SEQ_CAL2_USER_CALENDAR_ITEMS TO SEQ_CAL2_USER_CALENDAR_ENTRIES';
  DBMS_OUTPUT.PUT_LINE('RENAMED SEQUENCE: SEQ_CAL2_USER_CALENDAR_ENTRIES');
  EXECUTE IMMEDIATE 'ALTER TABLE CAL2.USER_CALENDAR_ENTRIES MODIFY (USER_CALENDAR_ENTRY_ID DEFAULT CAL2.SEQ_CAL2_USER_CALENDAR_ENTRIES.NEXTVAL)';
  DBMS_OUTPUT.PUT_LINE('REBOUND DEFAULT TO NEW SEQUENCE');
  rename_constraint('PK_CAL2_UCI','PK_CAL2_UCE');
  rename_constraint('FK_CAL2_UCI_DAY','FK_CAL2_UCE_DAY');
  rename_constraint('CK_CAL2_UCI_TYPE','CK_CAL2_UCE_TYPE');
  rename_constraint('CK_CAL2_UCI_STATUS','CK_CAL2_UCE_STATUS');
  rename_constraint('CK_CAL2_UCI_ADV','CK_CAL2_UCE_ADV');
  rename_constraint('CK_CAL2_UCI_REPEAT','CK_CAL2_UCE_REPEAT');
  rename_constraint('CK_CAL2_UCI_ACK','CK_CAL2_UCE_ACK');
  rename_constraint('CK_CAL2_UCI_REM_TIME','CK_CAL2_UCE_REM_TIME');
  rename_index('PK_CAL2_UCI','PK_CAL2_UCE');
  rename_index('IX_CAL2_UCI_ACTOR_DAY','IX_CAL2_UCE_ACTOR_DAY');
  rename_index('IX_CAL2_UCI_ALERT','IX_CAL2_UCE_ALERT');

  EXECUTE IMMEDIATE 'SELECT COUNT(*) FROM CAL2.USER_CALENDAR_ENTRIES' INTO v_rows_after;
  DBMS_OUTPUT.PUT_LINE('R1C_ROWS_AFTER='||v_rows_after);
  IF v_rows_after <> v_rows_before THEN
    RAISE_APPLICATION_ERROR(-20749, 'Row count mismatch after DDL; STOP and inspect manually.');
  END IF;
  DBMS_OUTPUT.PUT_LINE('CAL2_PERSONAL_R1C_RENAME_COMPLETE. Row counts preserved. Run read-only verifier.');
EXCEPTION
  WHEN OTHERS THEN
    IF v_started THEN
      DBMS_OUTPUT.PUT_LINE('R1C_DDL_PARTIAL_FAILURE_POSSIBLE: DDL autocommits. DO NOT RE-RUN BLINDLY; inspect schema and restore using DBA runbook.');
    ELSE
      DBMS_OUTPUT.PUT_LINE('R1C_PREFLIGHT_STOPPED: no DDL performed by this script.');
    END IF;
    RAISE;
END;
/
