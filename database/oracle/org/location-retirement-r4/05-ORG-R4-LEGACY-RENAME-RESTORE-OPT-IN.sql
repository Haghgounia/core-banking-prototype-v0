-- ORG R4-B recovery option: reverse ONLY the two table renames after DBA approval.
-- Does not restore legacy APIs/code; coordinate application deployment separately.
-- Oracle DDL commits implicitly. Not a transactional rollback.
SET SERVEROUTPUT ON SIZE UNLIMITED
SET VERIFY OFF
DEFINE APPLY_RESTORE = NO
PROMPT =============================================================
PROMPT ORG R4-B DBA-ONLY ARCHIVAL RENAME REVERSAL - PREVIEW BY DEFAULT
PROMPT =============================================================
DECLARE
 v_apply VARCHAR2(3):=UPPER(TRIM('&&APPLY_RESTORE'));
 v_old NUMBER:=0; v_new NUMBER:=0;
 v_old_p NUMBER:=0; v_new_p NUMBER:=0;
 FUNCTION n(p_table VARCHAR2) RETURN NUMBER IS c NUMBER; BEGIN
   SELECT COUNT(*) INTO c FROM ALL_TABLES WHERE OWNER='ORG' AND TABLE_NAME=p_table;
   RETURN c;
 END;
BEGIN
 IF v_apply NOT IN ('YES','NO') THEN RAISE_APPLICATION_ERROR(-20951,'APPLY_RESTORE must be YES or NO'); END IF;
 v_old:=n('LOCATIONS'); v_new:=n('DEPRECATED_LOCATIONS');
 v_old_p:=n('POSTAL_ADDRESSES'); v_new_p:=n('DEPRECATED_POSTAL_ADDRESSES');
 DBMS_OUTPUT.PUT_LINE('APPLY_RESTORE='||v_apply||' | ORIGINAL_LOCATIONS='||v_old||
   ' | ARCHIVED_LOCATIONS='||v_new||' | ORIGINAL_POSTAL='||v_old_p||' | ARCHIVED_POSTAL='||v_new_p);
 IF v_apply='NO' THEN
   DBMS_OUTPUT.PUT_LINE('ORG_R4B_RESTORE_PREVIEW_COMPLETE | NO DDL | NO DML');
   RETURN;
 END IF;
 IF v_old+v_new<>1 OR v_old_p+v_new_p<>1 THEN
   RAISE_APPLICATION_ERROR(-20952,'Unexpected table name state; DBA intervention required.');
 END IF;
 IF v_old=0 AND v_new=1 THEN
   EXECUTE IMMEDIATE 'ALTER TABLE ORG.DEPRECATED_LOCATIONS RENAME TO LOCATIONS';
   DBMS_OUTPUT.PUT_LINE('RESTORED | LOCATIONS');
 END IF;
 IF v_old_p=0 AND v_new_p=1 THEN
   EXECUTE IMMEDIATE 'ALTER TABLE ORG.DEPRECATED_POSTAL_ADDRESSES RENAME TO POSTAL_ADDRESSES';
   DBMS_OUTPUT.PUT_LINE('RESTORED | POSTAL_ADDRESSES');
 END IF;
 IF n('LOCATIONS')<>1 OR n('POSTAL_ADDRESSES')<>1 THEN
   RAISE_APPLICATION_ERROR(-20953,'Post restore name check failed; DBA review required.');
 END IF;
 DBMS_OUTPUT.PUT_LINE('ORG_R4B_RESTORE_PASS | ORIGINALS=2 | NO DML');
END;
/
