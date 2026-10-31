-- ORG R5-C3: archive ONLY the obsolete CONTACT_POINTS master.
-- Preview by default. Requires source cutover, UI/DBA/external signoff.
-- No DML, no DROP; Oracle ALTER TABLE RENAME auto-commits.
-- ORG.CONTACT_POINTS_V2 is not modified.
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
PROMPT ORG R5-C3 CONTACT_POINTS ARCHIVAL - PREVIEW FIRST
PROMPT TARGET OUCP STAYS ACTIVE. CONTACT_POINTS_V2 IS UNCHANGED.
PROMPT ================================================================
DECLARE
  c_old CONSTANT VARCHAR2(128) := 'CONTACT_POINTS';
  c_arch CONSTANT VARCHAR2(128) := 'DEPRECATED_CONTACT_POINTS';
  v_apply VARCHAR2(3) := UPPER(TRIM('&&APPLY_CHANGES'));
  v_source VARCHAR2(3) := UPPER(TRIM('&&CONFIRM_SOURCE_CUTOVER'));
  v_uat VARCHAR2(3) := UPPER(TRIM('&&CONFIRM_APP_UAT'));
  v_stop VARCHAR2(3) := UPPER(TRIM('&&CONFIRM_APP_STOPPED'));
  v_backup VARCHAR2(3) := UPPER(TRIM('&&CONFIRM_DBA_BACKUP'));
  v_external VARCHAR2(3) := UPPER(TRIM('&&CONFIRM_EXTERNAL_CONSUMERS'));
  v_old NUMBER; v_arch NUMBER; v_master VARCHAR2(128);
  v_rows_before NUMBER; v_rows_after NUMBER; v_obj_before NUMBER; v_obj_after NUMBER;
  v_in_fk NUMBER; v_stored NUMBER; v_synonyms NUMBER; v_triggers NUMBER;
  v_src NUMBER; v_jobs NUMBER; v_v2 NUMBER; v_oucp NUMBER; v_snapshot NUMBER;
  v_missing NUMBER; v_blockers NUMBER:=0;
  FUNCTION tcount(p_name VARCHAR2) RETURN NUMBER IS n NUMBER;
  BEGIN SELECT COUNT(*) INTO n FROM ALL_TABLES WHERE OWNER='ORG' AND TABLE_NAME=p_name; RETURN n; END;
BEGIN
  IF v_apply NOT IN ('YES','NO') OR v_source NOT IN ('YES','NO')
     OR v_uat NOT IN ('YES','NO') OR v_stop NOT IN ('YES','NO')
     OR v_backup NOT IN ('YES','NO') OR v_external NOT IN ('YES','NO') THEN
    RAISE_APPLICATION_ERROR(-20831,'All flags must be exactly YES or NO.');
  END IF;
  v_old:=tcount(c_old); v_arch:=tcount(c_arch);
  IF v_old+v_arch<>1 THEN RAISE_APPLICATION_ERROR(-20832,'Exactly one old or archive master required.'); END IF;
  v_master:=CASE WHEN v_old=1 THEN c_old ELSE c_arch END;
  EXECUTE IMMEDIATE 'SELECT COUNT(*) FROM ORG.'||v_master INTO v_rows_before;
  SELECT OBJECT_ID INTO v_obj_before FROM ALL_OBJECTS
   WHERE OWNER='ORG' AND OBJECT_NAME=v_master AND OBJECT_TYPE='TABLE';
  v_v2:=tcount('CONTACT_POINTS_V2'); v_oucp:=tcount('ORGANIZATION_UNIT_CONTACT_POINTS');
  IF v_oucp=1 THEN
    SELECT COUNT(*) INTO v_snapshot FROM ORG.ORGANIZATION_UNIT_CONTACT_POINTS
     WHERE CONTACT_TYPE_CODE IS NULL OR CONTACT_VALUE IS NULL OR SOURCE_CODE IS NULL;
    EXECUTE IMMEDIATE 'SELECT COUNT(*) FROM ORG.ORGANIZATION_UNIT_CONTACT_POINTS a '
      ||'WHERE a.CONTACT_POINT_ID IS NOT NULL AND NOT EXISTS '
      ||'(SELECT 1 FROM ORG.'||v_master||' m WHERE m.CONTACT_POINT_ID=a.CONTACT_POINT_ID)'
      INTO v_missing;
  ELSE v_snapshot:=-1; v_missing:=-1; END IF;
  SELECT COUNT(*) INTO v_in_fk FROM ALL_CONSTRAINTS fk JOIN ALL_CONSTRAINTS pk
    ON pk.OWNER=fk.R_OWNER AND pk.CONSTRAINT_NAME=fk.R_CONSTRAINT_NAME
   WHERE fk.CONSTRAINT_TYPE='R' AND pk.OWNER='ORG' AND pk.TABLE_NAME=v_master;
  SELECT COUNT(*) INTO v_stored FROM ALL_DEPENDENCIES
   WHERE REFERENCED_OWNER='ORG' AND REFERENCED_NAME=v_master AND TYPE<>'SYNONYM';
  SELECT COUNT(*) INTO v_synonyms FROM ALL_SYNONYMS
   WHERE TABLE_OWNER='ORG' AND TABLE_NAME=v_master;
  SELECT COUNT(*) INTO v_triggers FROM ALL_TRIGGERS
   WHERE TABLE_OWNER='ORG' AND TABLE_NAME=v_master;
  -- Match the exact legacy identifier; do not confuse CONTACT_POINTS_V2
  -- or ORGANIZATION_UNIT_CONTACT_POINTS with the retired master.
  SELECT COUNT(*) INTO v_src FROM ALL_SOURCE
   WHERE REGEXP_LIKE(UPPER(TEXT),
     '(^|[^A-Z0-9_])(ORG[.])?CONTACT_POINTS([^A-Z0-9_]|$)')
      OR REGEXP_LIKE(UPPER(TEXT),
     '(^|[^A-Z0-9_])DEPRECATED_CONTACT_POINTS([^A-Z0-9_]|$)');
  SELECT COUNT(*) INTO v_jobs FROM ALL_SCHEDULER_JOBS
   WHERE REGEXP_LIKE(UPPER(JOB_ACTION),
     '(^|[^A-Z0-9_])(ORG[.])?CONTACT_POINTS([^A-Z0-9_]|$)')
      OR REGEXP_LIKE(UPPER(JOB_ACTION),
     '(^|[^A-Z0-9_])DEPRECATED_CONTACT_POINTS([^A-Z0-9_]|$)');
  IF v_in_fk<>0 OR v_stored<>0 OR v_synonyms<>0 OR v_triggers<>0
     OR v_src<>0 OR v_jobs<>0 THEN v_blockers:=v_blockers+1; END IF;
  IF v_v2<>1 OR v_oucp<>1 OR v_snapshot<>0 OR v_missing<>0 THEN v_blockers:=v_blockers+1; END IF;
  DBMS_OUTPUT.PUT_LINE('FLAGS | APPLY_CHANGES='||v_apply||' | SOURCE_CUTOVER='||v_source||
    ' | APP_UAT='||v_uat||' | STOPPED='||v_stop||' | BACKUP='||v_backup||' | EXTERNAL='||v_external);
  DBMS_OUTPUT.PUT_LINE('SOURCE | MASTER='||v_master||' | ROWS='||v_rows_before||
    ' | OLD='||v_old||' | ARCHIVED='||v_arch||' | OBJECT_ID='||v_obj_before);
  DBMS_OUTPUT.PUT_LINE('CANONICAL | OUCP_PRESENT='||v_oucp||' | REQUIRED_SNAPSHOT_MISSING='||v_snapshot||
    ' | HISTORICAL_ORPHANS='||v_missing||' | V2_PRESERVED='||v_v2);
  DBMS_OUTPUT.PUT_LINE('DEPENDENCIES | INBOUND_FK='||v_in_fk||' | STORED='||v_stored||
    ' | SYNONYMS='||v_synonyms||' | TRIGGERS='||v_triggers||
    ' | SOURCE_TEXT='||v_src||' | JOBS='||v_jobs);
  DBMS_OUTPUT.PUT_LINE('PRECHECK | BLOCKERS='||v_blockers);
  IF v_apply='NO' THEN
    DBMS_OUTPUT.PUT_LINE('ORG_R5C3_MASTER_RENAME_PREVIEW_COMPLETE | NO DDL | NO DML'); RETURN;
  END IF;
  IF v_source<>'YES' OR v_uat<>'YES' OR v_stop<>'YES' OR v_backup<>'YES'
      OR v_external<>'YES' THEN
    RAISE_APPLICATION_ERROR(-20833,'Source, UAT, stopped writers, DBA and external approvals required.');
  END IF;
  IF v_blockers<>0 THEN RAISE_APPLICATION_ERROR(-20834,'Rename blocked by dependencies.'); END IF;
  IF v_arch=1 THEN
    DBMS_OUTPUT.PUT_LINE('ORG_R5C3_MASTER_ALREADY_ARCHIVED | NO DDL'); RETURN;
  END IF;
  EXECUTE IMMEDIATE 'ALTER TABLE ORG.CONTACT_POINTS RENAME TO DEPRECATED_CONTACT_POINTS';
  IF tcount(c_old)<>0 OR tcount(c_arch)<>1 THEN
    RAISE_APPLICATION_ERROR(-20835,'Post-rename name check failed; DDL already committed.');
  END IF;
  EXECUTE IMMEDIATE 'SELECT COUNT(*) FROM ORG.'||c_arch INTO v_rows_after;
  SELECT OBJECT_ID INTO v_obj_after FROM ALL_OBJECTS
   WHERE OWNER='ORG' AND OBJECT_NAME=c_arch AND OBJECT_TYPE='TABLE';
  IF v_rows_after<>v_rows_before OR v_obj_after<>v_obj_before THEN
    RAISE_APPLICATION_ERROR(-20836,'Post-rename data/object check failed; DDL already committed.');
  END IF;
  DBMS_OUTPUT.PUT_LINE('RENAMED | ORG.CONTACT_POINTS -> ORG.DEPRECATED_CONTACT_POINTS');
  DBMS_OUTPUT.PUT_LINE('ORG_R5C3_CONTACT_POINTS_RENAME_PASS | ROWS='||v_rows_after||
    ' | OBJECT_ID_PRESERVED=1 | OUCP_STILL_ACTIVE=1 | V2_UNCHANGED=1 | NO DML');
END;
/
PROMPT === ORG R5-C3 NAME CHECK - READ ONLY ===
SELECT TABLE_NAME FROM ALL_TABLES WHERE OWNER='ORG'
 AND TABLE_NAME IN ('CONTACT_POINTS','DEPRECATED_CONTACT_POINTS','CONTACT_POINTS_V2',
   'ORGANIZATION_UNIT_CONTACT_POINTS') ORDER BY TABLE_NAME;
