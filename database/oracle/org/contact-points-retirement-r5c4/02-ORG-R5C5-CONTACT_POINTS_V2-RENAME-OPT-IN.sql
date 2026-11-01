-- ORG R5-C5: archive the historical V2 pilot; OUCP remains the SOLE operational store.
-- Preview only by default. No DML, DROP, TRUNCATE or auto-commit command.
-- Execute F5 after R5-C4B build/runtime UAT and DBA/external-consumer signoff.
-- Oracle DDL RENAME implicitly commits. Do not set gates YES without actual approvals.
SET SERVEROUTPUT ON SIZE UNLIMITED
SET VERIFY OFF
SET DEFINE ON
DEFINE APPLY_CHANGES = NO
DEFINE CONFIRM_SOURCE_CUTOVER = NO
DEFINE CONFIRM_APP_UAT = NO
DEFINE CONFIRM_APP_STOPPED = NO
DEFINE CONFIRM_DBA_BACKUP = NO
DEFINE CONFIRM_EXTERNAL_CONSUMERS = NO
PROMPT ================================================================
PROMPT ORG R5-C5 CONTACT_POINTS_V2 HISTORICAL RENAME - PREVIEW FIRST
PROMPT OUCP REMAINS ACTIVE. NEVER DROP OR REWRITE V2 DATA.
PROMPT ================================================================
DECLARE
  c_old  CONSTANT VARCHAR2(128) := 'CONTACT_POINTS_V2';
  c_arch CONSTANT VARCHAR2(128) := 'DEPRECATED_CONTACT_POINTS_V2';
  v_apply  VARCHAR2(3) := UPPER(TRIM('&&APPLY_CHANGES'));
  v_source VARCHAR2(3) := UPPER(TRIM('&&CONFIRM_SOURCE_CUTOVER'));
  v_uat    VARCHAR2(3) := UPPER(TRIM('&&CONFIRM_APP_UAT'));
  v_stop   VARCHAR2(3) := UPPER(TRIM('&&CONFIRM_APP_STOPPED'));
  v_backup VARCHAR2(3) := UPPER(TRIM('&&CONFIRM_DBA_BACKUP'));
  v_ext    VARCHAR2(3) := UPPER(TRIM('&&CONFIRM_EXTERNAL_CONSUMERS'));
  v_old NUMBER; v_arch NUMBER; v_oucp NUMBER; v_old_master NUMBER; v_old_archive NUMBER;
  v_selected VARCHAR2(128);
  v_obj_before NUMBER := -1; v_obj_after NUMBER := -1;
  v_rows_before NUMBER := -1; v_rows_after NUMBER := -1; v_oucp_rows NUMBER := -1;
  v_historic NUMBER := -1; v_native NUMBER := -1; v_orphans NUMBER := -1;
  v_snapshot_missing NUMBER := -1; v_oucp_required NUMBER := -1;
  v_fk NUMBER; v_stored NUMBER; v_synonyms NUMBER; v_triggers NUMBER;
  v_source_refs NUMBER; v_jobs NUMBER; v_programs NUMBER; v_grants NUMBER;
  v_blockers NUMBER := 0;
  FUNCTION tab_count(p_tab VARCHAR2) RETURN NUMBER IS n NUMBER;
  BEGIN
    SELECT COUNT(*) INTO n FROM ALL_TABLES WHERE OWNER='ORG' AND TABLE_NAME=p_tab;
    RETURN n;
  END;
BEGIN
  IF v_apply NOT IN ('YES','NO') OR v_source NOT IN ('YES','NO')
     OR v_uat NOT IN ('YES','NO') OR v_stop NOT IN ('YES','NO')
     OR v_backup NOT IN ('YES','NO') OR v_ext NOT IN ('YES','NO') THEN
    RAISE_APPLICATION_ERROR(-20941, 'All six flags must be exactly YES or NO.');
  END IF;

  v_old := tab_count(c_old); v_arch := tab_count(c_arch);
  v_oucp := tab_count('ORGANIZATION_UNIT_CONTACT_POINTS');
  v_old_master := tab_count('CONTACT_POINTS');
  v_old_archive := tab_count('DEPRECATED_CONTACT_POINTS');
  IF v_old + v_arch <> 1 THEN
    RAISE_APPLICATION_ERROR(-20942, 'Exactly one V2 table name must exist.');
  END IF;
  v_selected := CASE WHEN v_old=1 THEN c_old ELSE c_arch END;
  EXECUTE IMMEDIATE 'SELECT COUNT(*) FROM ORG.'||v_selected INTO v_rows_before;
  SELECT OBJECT_ID INTO v_obj_before FROM ALL_OBJECTS
   WHERE OWNER='ORG' AND OBJECT_TYPE='TABLE' AND OBJECT_NAME=v_selected;

  IF v_oucp=1 THEN
    SELECT COUNT(*) INTO v_oucp_rows FROM ORG.ORGANIZATION_UNIT_CONTACT_POINTS;
    SELECT COUNT(*) INTO v_snapshot_missing
      FROM ORG.ORGANIZATION_UNIT_CONTACT_POINTS
     WHERE CONTACT_TYPE_CODE IS NULL OR CONTACT_VALUE IS NULL OR SOURCE_CODE IS NULL;
    SELECT COUNT(*) INTO v_oucp_required FROM ALL_TAB_COLUMNS
     WHERE OWNER='ORG' AND TABLE_NAME='ORGANIZATION_UNIT_CONTACT_POINTS'
       AND COLUMN_NAME IN ('ORGANIZATION_UNIT_CONTACT_POINT_ID',
                           'CONTACT_TYPE_CODE','CONTACT_VALUE','SOURCE_CODE');
  END IF;

  -- Historical V2 parity is NOT compared against newly added/edited OUCP records.
  EXECUTE IMMEDIATE
    'SELECT COUNT(LEGACY_ASSOCIATION_ID), COUNT(*)-COUNT(LEGACY_ASSOCIATION_ID) '
    ||'FROM ORG.'||v_selected INTO v_historic,v_native;
  IF v_oucp=1 THEN
    EXECUTE IMMEDIATE
      'SELECT COUNT(*) FROM ORG.'||v_selected||' v '
      ||'WHERE v.LEGACY_ASSOCIATION_ID IS NOT NULL '
      ||'AND NOT EXISTS (SELECT 1 FROM ORG.ORGANIZATION_UNIT_CONTACT_POINTS a '
      ||'WHERE a.ORGANIZATION_UNIT_CONTACT_POINT_ID=v.LEGACY_ASSOCIATION_ID)'
      INTO v_orphans;
  END IF;

  SELECT COUNT(*) INTO v_fk FROM ALL_CONSTRAINTS fk JOIN ALL_CONSTRAINTS pk
    ON pk.OWNER=fk.R_OWNER AND pk.CONSTRAINT_NAME=fk.R_CONSTRAINT_NAME
   WHERE fk.CONSTRAINT_TYPE='R' AND pk.OWNER='ORG' AND pk.TABLE_NAME=v_selected;
  SELECT COUNT(*) INTO v_stored FROM ALL_DEPENDENCIES
   WHERE REFERENCED_OWNER='ORG' AND REFERENCED_NAME=v_selected AND TYPE<>'SYNONYM';
  SELECT COUNT(*) INTO v_synonyms FROM ALL_SYNONYMS
   WHERE TABLE_OWNER='ORG' AND TABLE_NAME=v_selected;
  SELECT COUNT(*) INTO v_triggers FROM ALL_TRIGGERS
   WHERE TABLE_OWNER='ORG' AND TABLE_NAME=v_selected;
  SELECT COUNT(*) INTO v_source_refs FROM ALL_SOURCE
   WHERE REGEXP_LIKE(UPPER(TEXT),
    '(^|[^A-Z0-9_])CONTACT_POINTS_V2([^A-Z0-9_]|$)');
  SELECT COUNT(*) INTO v_jobs FROM ALL_SCHEDULER_JOBS
   WHERE REGEXP_LIKE(UPPER(JOB_ACTION),
    '(^|[^A-Z0-9_])CONTACT_POINTS_V2([^A-Z0-9_]|$)');
  SELECT COUNT(*) INTO v_programs FROM ALL_SCHEDULER_PROGRAMS
   WHERE REGEXP_LIKE(UPPER(PROGRAM_ACTION),
    '(^|[^A-Z0-9_])CONTACT_POINTS_V2([^A-Z0-9_]|$)');
  SELECT COUNT(*) INTO v_grants FROM ALL_TAB_PRIVS
   WHERE TABLE_SCHEMA='ORG' AND TABLE_NAME=v_selected AND GRANTEE<>'ORG';

  IF v_oucp<>1 OR v_old_master<>0 OR v_old_archive<>1
     OR v_oucp_rows<6930 OR v_snapshot_missing<>0 OR v_oucp_required<>4 THEN
     v_blockers := v_blockers+1;
  END IF;
  -- The R5-C4A signed-off baseline was exactly 6930 migration-only rows.
  IF v_rows_before<>6930 OR v_historic<>6930 OR v_native<>0 OR v_orphans<>0 THEN
     v_blockers := v_blockers+1;
  END IF;
  IF v_fk<>0 OR v_stored<>0 OR v_synonyms<>0 OR v_triggers<>0
     OR v_source_refs<>0 OR v_jobs<>0 OR v_programs<>0 OR v_grants<>0 THEN
     v_blockers := v_blockers+1;
  END IF;

  DBMS_OUTPUT.PUT_LINE('FLAGS | APPLY_CHANGES='||v_apply||' | SOURCE_CUTOVER='||v_source||
   ' | APP_UAT='||v_uat||' | STOPPED='||v_stop||' | BACKUP='||v_backup||' | EXTERNAL='||v_ext);
  DBMS_OUTPUT.PUT_LINE('TABLES | V2_ACTIVE='||v_old||' | V2_ARCHIVED='||v_arch||
   ' | OUCP_ACTIVE='||v_oucp||' | ORIGINAL_MASTER='||v_old_master||
   ' | ORIGINAL_ARCHIVED='||v_old_archive);
  DBMS_OUTPUT.PUT_LINE('DATA | V2_ROWS='||v_rows_before||' | V2_HISTORICAL='||v_historic||
   ' | V2_NATIVE='||v_native||' | HISTORICAL_ORPHANS='||v_orphans||
   ' | OUCP_ROWS='||v_oucp_rows||' | SNAPSHOT_MISSING='||v_snapshot_missing);
  DBMS_OUTPUT.PUT_LINE('DEPENDENCIES | INBOUND_FK='||v_fk||' | STORED='||v_stored||
   ' | SYNONYMS='||v_synonyms||' | TRIGGERS='||v_triggers||
   ' | SOURCE_LINES='||v_source_refs||' | JOBS='||v_jobs||
   ' | PROGRAMS='||v_programs||' | GRANTS='||v_grants);
  DBMS_OUTPUT.PUT_LINE('PRECHECK | BLOCKERS='||v_blockers||' | OBJECT_ID='||v_obj_before);
  IF v_apply='NO' THEN
    DBMS_OUTPUT.PUT_LINE('ORG_R5C5_V2_RENAME_PREVIEW_COMPLETE | NO DDL | NO DML');
    RETURN;
  END IF;
  IF v_source<>'YES' OR v_uat<>'YES' OR v_stop<>'YES'
      OR v_backup<>'YES' OR v_ext<>'YES' THEN
    RAISE_APPLICATION_ERROR(-20943, 'Actual source cutover/UAT/stopped writers/backup/external approvals required.');
  END IF;
  IF v_blockers<>0 THEN
    RAISE_APPLICATION_ERROR(-20944, 'V2 rename blocked by data or dependencies.');
  END IF;
  IF v_arch=1 THEN
    DBMS_OUTPUT.PUT_LINE('ORG_R5C5_V2_ALREADY_ARCHIVED | NO DDL');
    RETURN;
  END IF;
  EXECUTE IMMEDIATE 'ALTER TABLE ORG.CONTACT_POINTS_V2 RENAME TO DEPRECATED_CONTACT_POINTS_V2';
  IF tab_count(c_old)<>0 OR tab_count(c_arch)<>1 THEN
    RAISE_APPLICATION_ERROR(-20945, 'Post-rename name check failed. DDL already committed.');
  END IF;
  EXECUTE IMMEDIATE 'SELECT COUNT(*) FROM ORG.'||c_arch INTO v_rows_after;
  SELECT OBJECT_ID INTO v_obj_after FROM ALL_OBJECTS
   WHERE OWNER='ORG' AND OBJECT_TYPE='TABLE' AND OBJECT_NAME=c_arch;
  IF v_rows_after<>v_rows_before OR v_obj_after<>v_obj_before THEN
    RAISE_APPLICATION_ERROR(-20946, 'Post-rename object/data check failed. DDL already committed.');
  END IF;
  DBMS_OUTPUT.PUT_LINE('RENAMED | ORG.CONTACT_POINTS_V2 -> ORG.DEPRECATED_CONTACT_POINTS_V2');
  DBMS_OUTPUT.PUT_LINE('ORG_R5C5_V2_RENAME_PASS | ROWS='||v_rows_after||
   ' | OBJECT_ID_PRESERVED=1 | OUCP_ACTIVE=1 | NO DML');
EXCEPTION
  WHEN OTHERS THEN
    DBMS_OUTPUT.PUT_LINE('ORG_R5C5_V2_RENAME_FAILED | '||SQLCODE||' | '||SUBSTR(SQLERRM,1,200));
    RAISE;
END;
/
PROMPT === R5-C5 V2 TABLE NAME CHECK (READ-ONLY) ===
SELECT TABLE_NAME FROM ALL_TABLES WHERE OWNER='ORG' AND TABLE_NAME IN
 ('CONTACT_POINTS_V2','DEPRECATED_CONTACT_POINTS_V2',
  'CONTACT_POINTS','DEPRECATED_CONTACT_POINTS','ORGANIZATION_UNIT_CONTACT_POINTS')
 ORDER BY TABLE_NAME;
