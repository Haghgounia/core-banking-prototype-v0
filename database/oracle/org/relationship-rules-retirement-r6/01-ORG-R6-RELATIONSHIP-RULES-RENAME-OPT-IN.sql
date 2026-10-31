-- ORG R6: Non-destructive archive of the obsolete relationship-rule registry.
-- SQL Developer: execute with F5. Preview by default. No DROP, DELETE or COMMIT.
-- Oracle DDL auto-commits: take backup, stop app and verify external consumers first.
-- Only ORG.ORGANIZATION_UNIT_RELATIONSHIP_RULES is affected.
-- Active ORG.ORGANIZATION_UNIT_RELATIONSHIPS and _TYPES must remain unchanged.
SET SERVEROUTPUT ON SIZE UNLIMITED
SET VERIFY OFF
SET DEFINE ON
DEFINE APPLY_CHANGES = NO
DEFINE CONFIRM_APP_UAT = NO
DEFINE CONFIRM_APP_STOPPED = NO
DEFINE CONFIRM_DBA_BACKUP = NO
DEFINE CONFIRM_EXTERNAL_CONSUMERS = NO
PROMPT =====================================================================
PROMPT ORG R6 RELATIONSHIP RULES DEPRECATION - PREVIEW FIRST
PROMPT Requires explicit DBA, application UAT and external consumer approvals.
PROMPT =====================================================================
DECLARE
  c_old CONSTANT VARCHAR2(128) := 'ORGANIZATION_UNIT_RELATIONSHIP_RULES';
  c_arch CONSTANT VARCHAR2(128) := 'DEPRECATED_ORGANIZATION_UNIT_RELATIONSHIP_RULES';
  v_apply VARCHAR2(3) := UPPER(TRIM('&&APPLY_CHANGES'));
  v_uat VARCHAR2(3) := UPPER(TRIM('&&CONFIRM_APP_UAT'));
  v_stop VARCHAR2(3) := UPPER(TRIM('&&CONFIRM_APP_STOPPED'));
  v_dba VARCHAR2(3) := UPPER(TRIM('&&CONFIRM_DBA_BACKUP'));
  v_external VARCHAR2(3) := UPPER(TRIM('&&CONFIRM_EXTERNAL_CONSUMERS'));
  v_old NUMBER; v_arch NUMBER; v_table VARCHAR2(128);
  v_rows_before NUMBER; v_rows_after NUMBER;
  v_object_before NUMBER; v_object_after NUMBER;
  v_columns NUMBER; v_pk NUMBER; v_uk NUMBER;
  v_out_fk NUMBER; v_in_fk NUMBER; v_stored NUMBER;
  v_synonyms NUMBER; v_triggers NUMBER; v_source_refs NUMBER; v_jobs NUMBER;
  v_sequence NUMBER; v_blockers NUMBER := 0;
  FUNCTION table_count(p_table VARCHAR2) RETURN NUMBER IS v_n NUMBER;
  BEGIN
    SELECT COUNT(*) INTO v_n FROM ALL_TABLES
     WHERE OWNER='ORG' AND TABLE_NAME=p_table;
    RETURN v_n;
  END;
BEGIN
  IF v_apply NOT IN ('YES','NO') OR v_uat NOT IN ('YES','NO')
     OR v_stop NOT IN ('YES','NO')
     OR v_dba NOT IN ('YES','NO') OR v_external NOT IN ('YES','NO') THEN
    RAISE_APPLICATION_ERROR(-20801,'All flags must be exactly YES or NO.');
  END IF;
  v_old := table_count(c_old);
  v_arch := table_count(c_arch);
  IF v_old + v_arch <> 1 THEN
    RAISE_APPLICATION_ERROR(-20802,
      'Exactly one original or deprecated relationship-rule table must exist.');
  END IF;
  v_table := CASE WHEN v_old=1 THEN c_old ELSE c_arch END;
  EXECUTE IMMEDIATE 'SELECT COUNT(*) FROM ORG.' || v_table INTO v_rows_before;
  SELECT OBJECT_ID INTO v_object_before FROM ALL_OBJECTS
   WHERE OWNER='ORG' AND OBJECT_NAME=v_table AND OBJECT_TYPE='TABLE';
  SELECT COUNT(DISTINCT COLUMN_NAME) INTO v_columns FROM ALL_TAB_COLUMNS
   WHERE OWNER='ORG' AND TABLE_NAME=v_table
     AND COLUMN_NAME IN ('ORGANIZATION_UNIT_RELATIONSHIP_RULE_ID',
       'ORGANIZATION_UNIT_RELATIONSHIP_TYPE_ID', 'SOURCE_ORGANIZATION_UNIT_TYPE_ID',
       'TARGET_ORGANIZATION_UNIT_TYPE_ID', 'OPERATION_TYPE_CODE', 'SUBSYSTEM_CODE');
  SELECT COUNT(*) INTO v_pk FROM ALL_CONSTRAINTS
   WHERE OWNER='ORG' AND TABLE_NAME=v_table
     AND CONSTRAINT_TYPE='P' AND STATUS='ENABLED' AND VALIDATED='VALIDATED';
  SELECT COUNT(*) INTO v_uk FROM ALL_CONSTRAINTS
   WHERE OWNER='ORG' AND TABLE_NAME=v_table
     AND CONSTRAINT_TYPE='U' AND STATUS='ENABLED' AND VALIDATED='VALIDATED';
  SELECT COUNT(*) INTO v_out_fk FROM ALL_CONSTRAINTS fk
    JOIN ALL_CONSTRAINTS pk ON pk.OWNER=fk.R_OWNER
       AND pk.CONSTRAINT_NAME=fk.R_CONSTRAINT_NAME
   WHERE fk.OWNER='ORG' AND fk.TABLE_NAME=v_table AND fk.CONSTRAINT_TYPE='R'
     AND fk.STATUS='ENABLED' AND fk.VALIDATED='VALIDATED'
     AND pk.OWNER='ORG' AND pk.TABLE_NAME IN
       ('ORGANIZATION_UNIT_TYPES','ORGANIZATION_UNIT_RELATIONSHIP_TYPES');
  -- Any inbound FK makes a rename unsafe until consumers are reviewed explicitly.
  SELECT COUNT(*) INTO v_in_fk FROM ALL_CONSTRAINTS fk
    JOIN ALL_CONSTRAINTS pk ON pk.OWNER=fk.R_OWNER
       AND pk.CONSTRAINT_NAME=fk.R_CONSTRAINT_NAME
   WHERE fk.CONSTRAINT_TYPE='R'
     AND pk.OWNER='ORG' AND pk.TABLE_NAME=v_table;
  SELECT COUNT(*) INTO v_stored FROM ALL_DEPENDENCIES
   WHERE REFERENCED_OWNER='ORG' AND REFERENCED_NAME=v_table
     AND TYPE <> 'SYNONYM';
  SELECT COUNT(*) INTO v_synonyms FROM ALL_SYNONYMS
   WHERE TABLE_OWNER='ORG' AND TABLE_NAME=v_table;
  SELECT COUNT(*) INTO v_triggers FROM ALL_TRIGGERS
   WHERE TABLE_OWNER='ORG' AND TABLE_NAME=v_table;
  -- This is an additional string-based screening; it cannot discover all dynamic SQL.
  SELECT COUNT(*) INTO v_source_refs FROM ALL_SOURCE
   WHERE UPPER(TEXT) LIKE '%' || c_old || '%'
      OR UPPER(TEXT) LIKE '%' || c_arch || '%';
  SELECT COUNT(*) INTO v_jobs FROM ALL_SCHEDULER_JOBS
   WHERE UPPER(JOB_ACTION) LIKE '%' || c_old || '%'
      OR UPPER(JOB_ACTION) LIKE '%' || c_arch || '%';
  SELECT COUNT(*) INTO v_sequence FROM ALL_SEQUENCES
   WHERE SEQUENCE_OWNER='ORG' AND SEQUENCE_NAME='SEQ_ORGANIZATION_UNIT_RELATIONSHIP_RULES';
  IF v_columns<>6 OR v_pk<>1 OR v_uk<>1 OR v_out_fk<>3 OR v_sequence<>1 THEN
    v_blockers := v_blockers+1;
  END IF;
  IF v_in_fk<>0 OR v_stored<>0 OR v_synonyms<>0 OR v_triggers<>0
     OR v_source_refs<>0 OR v_jobs<>0 THEN
    v_blockers := v_blockers+1;
  END IF;
  DBMS_OUTPUT.PUT_LINE('FLAGS | APPLY_CHANGES='||v_apply||' | CONFIRM_APP_UAT='||v_uat||
      ' | CONFIRM_APP_STOPPED='||v_stop||' | CONFIRM_DBA_BACKUP='||v_dba||
      ' | CONFIRM_EXTERNAL_CONSUMERS='||v_external);
  DBMS_OUTPUT.PUT_LINE('SOURCE | TABLE='||v_table||' | ROWS='||v_rows_before||
      ' | OBJECT_ID='||v_object_before||' | OLD='||v_old||' | DEPRECATED='||v_arch);
  DBMS_OUTPUT.PUT_LINE('STRUCTURE | COLUMNS='||v_columns||'/6 | PK='||v_pk||
      ' | UK='||v_uk||' | OUTBOUND_FK='||v_out_fk||'/3 | SEQUENCE='||v_sequence);
  DBMS_OUTPUT.PUT_LINE('DEPENDENCIES | INBOUND_FK='||v_in_fk||' | STORED='||v_stored||
      ' | SYNONYMS='||v_synonyms||' | TRIGGERS='||v_triggers||
      ' | SOURCE_TEXT='||v_source_refs||' | SCHEDULER_JOBS='||v_jobs);
  DBMS_OUTPUT.PUT_LINE('PRECHECK | BLOCKERS='||v_blockers);
  IF v_apply='NO' THEN
    DBMS_OUTPUT.PUT_LINE('ORG_R6_RULES_PREVIEW_COMPLETE | NO DDL | NO DML');
    RETURN;
  END IF;
  IF v_uat<>'YES' OR v_stop<>'YES' OR v_dba<>'YES' OR v_external<>'YES' THEN
    RAISE_APPLICATION_ERROR(-20803,'Explicit UAT, application stop, DBA and external approvals are required.');
  END IF;
  IF v_blockers<>0 THEN
    RAISE_APPLICATION_ERROR(-20804,'Dependency or structure blockers. No R6 rename performed.');
  END IF;
  IF v_arch=1 THEN
    DBMS_OUTPUT.PUT_LINE('ORG_R6_RULES_ALREADY_DEPRECATED | NO DDL');
    RETURN;
  END IF;
  -- Oracle commits this DDL automatically, even if a subsequent validation fails.
  EXECUTE IMMEDIATE 'ALTER TABLE ORG.'||c_old||' RENAME TO '||c_arch;
  IF table_count(c_old)<>0 OR table_count(c_arch)<>1 THEN
    RAISE_APPLICATION_ERROR(-20805,'Rename executed but table-name verification failed.');
  END IF;
  EXECUTE IMMEDIATE 'SELECT COUNT(*) FROM ORG.' || c_arch INTO v_rows_after;
  SELECT OBJECT_ID INTO v_object_after FROM ALL_OBJECTS
   WHERE OWNER='ORG' AND OBJECT_NAME=c_arch AND OBJECT_TYPE='TABLE';
  IF v_rows_after<>v_rows_before OR v_object_after<>v_object_before THEN
    RAISE_APPLICATION_ERROR(-20806,'Post-rename object or row preservation mismatch.');
  END IF;
  DBMS_OUTPUT.PUT_LINE('RENAMED | ORG.'||c_old||' -> ORG.'||c_arch);
  DBMS_OUTPUT.PUT_LINE('ORG_R6_RULES_DEPRECATION_PASS | RENAMED_NOW=1 | ROWS='||
     v_rows_after||' | OBJECT_ID_PRESERVED=1 | NO DML');
END;
/
PROMPT === R6 TABLE NAME VERIFICATION - READ ONLY ===
SELECT TABLE_NAME FROM ALL_TABLES WHERE OWNER='ORG'
  AND TABLE_NAME IN ('ORGANIZATION_UNIT_RELATIONSHIP_RULES',
    'DEPRECATED_ORGANIZATION_UNIT_RELATIONSHIP_RULES') ORDER BY TABLE_NAME;
PROMPT NOTE: Keep the historical rule sequence, PK, UK and outbound FK constraints intact.
