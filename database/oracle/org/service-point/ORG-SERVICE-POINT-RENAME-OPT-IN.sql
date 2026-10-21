-- ORG / Service Point table naming migration
-- Run with SQL Developer F5 (Run Script), ideally during a stopped-application window.
-- No records are modified. Foreign keys continue pointing to the same table objects.
-- IMPORTANT: Oracle DDL implicitly commits and CANNOT be rolled back by ROLLBACK.
-- It can invalidate views, stored PL/SQL, synonyms, and external integrations referring
-- to old names. Inspect the dependency query below before opting in.
-- The existing column, constraint and sequence names are intentionally unchanged.
-- This is a table-name migration ONLY.
SET SERVEROUTPUT ON SIZE UNLIMITED
SET VERIFY OFF
DEFINE APPLY_CHANGES = NO
PROMPT === ORG SERVICE POINT: intended table-name changes ===
PROMPT Default is PREVIEW. To apply, change APPLY_CHANGES = NO to YES near the top.

SELECT owner, table_name, num_rows
  FROM all_tables
 WHERE owner = 'ORG'
   AND table_name IN (
    'POINT_OF_SERVICE_TYPES','POINTS_OF_SERVICE',
    'ORGANIZATION_UNIT_POINT_OF_SERVICE_ASSIGNMENTS',
    'POINT_OF_SERVICE_CONTACT_POINTS',
    'POINT_OF_SERVICE_OPERATING_SCHEDULES',
    'POINT_OF_SERVICE_OPERATING_EXCEPTIONS',
    'POINT_OF_SERVICE_OPERATING_EXCEPTION_INTERVALS',
    'SERVICE_POINT_TYPES','SERVICE_POINTS',
    'ORGANIZATION_UNIT_SERVICE_POINT_ASSIGNMENTS',
    'SERVICE_POINT_CONTACT_POINTS',
    'SERVICE_POINT_OPERATING_SCHEDULES',
    'SERVICE_POINT_OPERATING_EXCEPTIONS',
    'SERVICE_POINT_OPERATING_EXCEPTION_INTERVALS')
 ORDER BY table_name;

PROMPT === Dependent views and program units: review before APPLY ===
SELECT owner, name, type, referenced_name
  FROM all_dependencies
 WHERE referenced_owner='ORG'
   AND referenced_name IN (
    'POINT_OF_SERVICE_TYPES','POINTS_OF_SERVICE',
    'ORGANIZATION_UNIT_POINT_OF_SERVICE_ASSIGNMENTS',
    'POINT_OF_SERVICE_CONTACT_POINTS',
    'POINT_OF_SERVICE_OPERATING_SCHEDULES',
    'POINT_OF_SERVICE_OPERATING_EXCEPTIONS',
    'POINT_OF_SERVICE_OPERATING_EXCEPTION_INTERVALS')
 ORDER BY owner, name;

DECLARE
  TYPE t_names IS TABLE OF VARCHAR2(128) INDEX BY PLS_INTEGER;
  old_names t_names;
  new_names t_names;
  v_old NUMBER;
  v_new NUMBER;
  v_mode VARCHAR2(8) := UPPER(TRIM('&&APPLY_CHANGES'));
  v_to_rename NUMBER:=0;
BEGIN
  old_names(1):='POINT_OF_SERVICE_TYPES'; new_names(1):='SERVICE_POINT_TYPES';
  old_names(2):='POINTS_OF_SERVICE'; new_names(2):='SERVICE_POINTS';
  old_names(3):='ORGANIZATION_UNIT_POINT_OF_SERVICE_ASSIGNMENTS'; new_names(3):='ORGANIZATION_UNIT_SERVICE_POINT_ASSIGNMENTS';
  old_names(4):='POINT_OF_SERVICE_CONTACT_POINTS'; new_names(4):='SERVICE_POINT_CONTACT_POINTS';
  old_names(5):='POINT_OF_SERVICE_OPERATING_SCHEDULES'; new_names(5):='SERVICE_POINT_OPERATING_SCHEDULES';
  old_names(6):='POINT_OF_SERVICE_OPERATING_EXCEPTIONS'; new_names(6):='SERVICE_POINT_OPERATING_EXCEPTIONS';
  old_names(7):='POINT_OF_SERVICE_OPERATING_EXCEPTION_INTERVALS'; new_names(7):='SERVICE_POINT_OPERATING_EXCEPTION_INTERVALS';
  IF v_mode NOT IN ('YES','NO') THEN
    RAISE_APPLICATION_ERROR(-20101,'APPLY_CHANGES must be YES or NO');
  END IF;
  -- Preflight ALL seven pairs before any DDL; allows a safe resume after partial rename.
  FOR i IN 1..7 LOOP
    SELECT COUNT(*) INTO v_old FROM all_tables
     WHERE owner='ORG' AND table_name=old_names(i);
    SELECT COUNT(*) INTO v_new FROM all_objects
     WHERE owner='ORG' AND object_name=new_names(i)
       AND object_type IN ('TABLE','VIEW','SYNONYM','MATERIALIZED VIEW');
    IF v_old=1 AND v_new=0 THEN
      v_to_rename:=v_to_rename+1;
      DBMS_OUTPUT.PUT_LINE('PENDING: '||old_names(i)||' -> '||new_names(i));
    ELSIF v_old=0 AND v_new=1 THEN
      SELECT COUNT(*) INTO v_new FROM all_tables
       WHERE owner='ORG' AND table_name=new_names(i);
      IF v_new<>1 THEN
        RAISE_APPLICATION_ERROR(-20102,'Target exists but is not a TABLE: '||new_names(i));
      END IF;
      DBMS_OUTPUT.PUT_LINE('ALREADY RENAMED: '||new_names(i));
    ELSE
      RAISE_APPLICATION_ERROR(-20103,'Unsafe old/new state for '||old_names(i)||' / '||new_names(i));
    END IF;
  END LOOP;
  IF v_mode='NO' THEN
    DBMS_OUTPUT.PUT_LINE('PREVIEW COMPLETE. No writes; pending renames='||v_to_rename);
    RETURN;
  END IF;
  -- This migration is intended for a maintenance window with the application stopped.
  -- Rename preserves the existing data/PK/FK/indexes and leaves sequences/columns as-is.
  FOR i IN 1..7 LOOP
    SELECT COUNT(*) INTO v_old FROM all_tables
     WHERE owner='ORG' AND table_name=old_names(i);
    IF v_old=1 THEN
      EXECUTE IMMEDIATE 'ALTER TABLE ORG.'||old_names(i)||' RENAME TO '||new_names(i);
      DBMS_OUTPUT.PUT_LINE('RENAMED: '||old_names(i)||' -> '||new_names(i));
    ELSE
      DBMS_OUTPUT.PUT_LINE('SKIPPED ALREADY RENAMED: '||new_names(i));
    END IF;
  END LOOP;
  DBMS_OUTPUT.PUT_LINE('SERVICE_POINT_RENAME_APPLY_COMPLETE. Verify 7 new table names before starting patched app.');
END;
/

PROMPT === Final target-name verification ===
SELECT table_name
  FROM all_tables
 WHERE owner='ORG' AND table_name IN (
    'SERVICE_POINT_TYPES','SERVICE_POINTS',
    'ORGANIZATION_UNIT_SERVICE_POINT_ASSIGNMENTS',
    'SERVICE_POINT_CONTACT_POINTS','SERVICE_POINT_OPERATING_SCHEDULES',
    'SERVICE_POINT_OPERATING_EXCEPTIONS','SERVICE_POINT_OPERATING_EXCEPTION_INTERVALS')
 ORDER BY table_name;
