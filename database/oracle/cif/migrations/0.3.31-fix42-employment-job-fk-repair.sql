PROMPT =====================================================================
PROMPT Core Banking Prototype 0.3.31-fix42 - employment job FK safe repair
PROMPT =====================================================================
PROMPT This script repairs both states:
PROMPT   A) FIX41 already dropped the old FK but failed before creating new FKs.
PROMPT   B) Clean database where the old CIF.REF_OCCUPATION FK still exists.
PROMPT
PROMPT Prerequisite (run as GEO owner or DBA):
PROMPT   GRANT REFERENCES ON GEO.JOBS TO CIF;
PROMPT   GRANT REFERENCES ON GEO.JOB_GROUPS TO CIF;
PROMPT =====================================================================

WHENEVER SQLERROR EXIT SQL.SQLCODE

SET SERVEROUTPUT ON;

DECLARE
  v_count NUMBER;
BEGIN
  -- Parent tables and keys must physically exist before the child FK is touched.
  SELECT COUNT(*) INTO v_count
    FROM ALL_TABLES
   WHERE OWNER='GEO' AND TABLE_NAME='JOBS';
  IF v_count = 0 THEN
    RAISE_APPLICATION_ERROR(-20301, 'GEO.JOBS is not visible. Verify that GEO reference-data DDL is installed in this database.');
  END IF;

  SELECT COUNT(*) INTO v_count
    FROM ALL_TABLES
   WHERE OWNER='GEO' AND TABLE_NAME='JOB_GROUPS';
  IF v_count = 0 THEN
    RAISE_APPLICATION_ERROR(-20302, 'GEO.JOB_GROUPS is not visible. Verify that GEO reference-data DDL is installed in this database.');
  END IF;

  SELECT COUNT(*) INTO v_count
    FROM ALL_CONSTRAINTS c
    JOIN ALL_CONS_COLUMNS cc
      ON cc.OWNER=c.OWNER AND cc.CONSTRAINT_NAME=c.CONSTRAINT_NAME
   WHERE c.OWNER='GEO'
     AND c.TABLE_NAME='JOBS'
     AND c.CONSTRAINT_TYPE IN ('P','U')
     AND c.STATUS='ENABLED'
     AND cc.COLUMN_NAME='JOB_CODE';
  IF v_count = 0 THEN
    RAISE_APPLICATION_ERROR(-20303, 'GEO.JOBS.JOB_CODE must have an enabled PK/UK.');
  END IF;

  SELECT COUNT(*) INTO v_count
    FROM ALL_CONSTRAINTS c
    JOIN ALL_CONS_COLUMNS cc
      ON cc.OWNER=c.OWNER AND cc.CONSTRAINT_NAME=c.CONSTRAINT_NAME
   WHERE c.OWNER='GEO'
     AND c.TABLE_NAME='JOB_GROUPS'
     AND c.CONSTRAINT_TYPE IN ('P','U')
     AND c.STATUS='ENABLED'
     AND cc.COLUMN_NAME='JOB_GROUP_CODE';
  IF v_count = 0 THEN
    RAISE_APPLICATION_ERROR(-20304, 'GEO.JOB_GROUPS.JOB_GROUP_CODE must have an enabled PK/UK.');
  END IF;

  DBMS_OUTPUT.PUT_LINE('Parent tables and unique business keys are available.');
END;
/

PROMPT Existing employment rows that do not match GEO.JOBS (informational only):
SELECT COUNT(*) AS LEGACY_JOB_CODE_MISMATCHES
  FROM CIF.PARTY_EMPLOYMENT e
 WHERE e.OCCUPATION_CODE IS NOT NULL
   AND NOT EXISTS (
       SELECT 1 FROM GEO.JOBS j WHERE j.JOB_CODE=e.OCCUPATION_CODE
   );

PROMPT Existing employment rows that do not match GEO.JOB_GROUPS (informational only):
SELECT COUNT(*) AS LEGACY_JOB_GROUP_MISMATCHES
  FROM CIF.PARTY_EMPLOYMENT e
 WHERE e.OCCUPATION_GROUP_CODE IS NOT NULL
   AND NOT EXISTS (
       SELECT 1 FROM GEO.JOB_GROUPS g WHERE g.JOB_GROUP_CODE=e.OCCUPATION_GROUP_CODE
   );

DECLARE
  v_count NUMBER;
BEGIN
  -- Remove the obsolete FK only after parent objects were validated above.
  SELECT COUNT(*) INTO v_count
    FROM ALL_CONSTRAINTS
   WHERE OWNER='CIF'
     AND TABLE_NAME='PARTY_EMPLOYMENT'
     AND CONSTRAINT_NAME='FK_PARTY_EMPLOYMENT_REF_OCCUPATION';
  IF v_count > 0 THEN
    EXECUTE IMMEDIATE 'ALTER TABLE CIF.PARTY_EMPLOYMENT DROP CONSTRAINT FK_PARTY_EMPLOYMENT_REF_OCCUPATION';
    DBMS_OUTPUT.PUT_LINE('Dropped obsolete FK_PARTY_EMPLOYMENT_REF_OCCUPATION');
  ELSE
    DBMS_OUTPUT.PUT_LINE('Obsolete occupation FK is already absent (expected after partial FIX41 run).');
  END IF;

  SELECT COUNT(*) INTO v_count
    FROM ALL_CONSTRAINTS
   WHERE OWNER='CIF'
     AND TABLE_NAME='PARTY_EMPLOYMENT'
     AND CONSTRAINT_NAME='FK_EMP_GEO_JOB';
  IF v_count = 0 THEN
    BEGIN
      EXECUTE IMMEDIATE q'[
        ALTER TABLE CIF.PARTY_EMPLOYMENT
        ADD CONSTRAINT FK_EMP_GEO_JOB
        FOREIGN KEY (OCCUPATION_CODE)
        REFERENCES GEO.JOBS (JOB_CODE)
        ENABLE NOVALIDATE
      ]';
      DBMS_OUTPUT.PUT_LINE('Created FK_EMP_GEO_JOB');
    EXCEPTION
      WHEN OTHERS THEN
        IF SQLCODE = -942 THEN
          RAISE_APPLICATION_ERROR(-20311,
            'Cannot reference GEO.JOBS from CIF. GEO.JOBS exists, but CIF most likely lacks a DIRECT REFERENCES grant. Run as GEO/DBA: GRANT REFERENCES ON GEO.JOBS TO CIF;');
        ELSE
          RAISE;
        END IF;
    END;
  ELSE
    DBMS_OUTPUT.PUT_LINE('FK_EMP_GEO_JOB already exists.');
  END IF;

  SELECT COUNT(*) INTO v_count
    FROM ALL_CONSTRAINTS
   WHERE OWNER='CIF'
     AND TABLE_NAME='PARTY_EMPLOYMENT'
     AND CONSTRAINT_NAME='FK_EMP_GEO_JOB_GROUP';
  IF v_count = 0 THEN
    BEGIN
      EXECUTE IMMEDIATE q'[
        ALTER TABLE CIF.PARTY_EMPLOYMENT
        ADD CONSTRAINT FK_EMP_GEO_JOB_GROUP
        FOREIGN KEY (OCCUPATION_GROUP_CODE)
        REFERENCES GEO.JOB_GROUPS (JOB_GROUP_CODE)
        ENABLE NOVALIDATE
      ]';
      DBMS_OUTPUT.PUT_LINE('Created FK_EMP_GEO_JOB_GROUP');
    EXCEPTION
      WHEN OTHERS THEN
        IF SQLCODE = -942 THEN
          RAISE_APPLICATION_ERROR(-20312,
            'Cannot reference GEO.JOB_GROUPS from CIF. GEO.JOB_GROUPS exists, but CIF most likely lacks a DIRECT REFERENCES grant. Run as GEO/DBA: GRANT REFERENCES ON GEO.JOB_GROUPS TO CIF;');
        ELSE
          RAISE;
        END IF;
    END;
  ELSE
    DBMS_OUTPUT.PUT_LINE('FK_EMP_GEO_JOB_GROUP already exists.');
  END IF;
END;
/

PROMPT Verification - exactly 2 rows are expected:
SELECT c.CONSTRAINT_NAME,
       cc.COLUMN_NAME AS CHILD_COLUMN,
       c.R_OWNER AS PARENT_OWNER,
       pc.TABLE_NAME AS PARENT_TABLE,
       pcc.COLUMN_NAME AS PARENT_COLUMN,
       c.STATUS,
       c.VALIDATED
  FROM ALL_CONSTRAINTS c
  JOIN ALL_CONS_COLUMNS cc
    ON cc.OWNER=c.OWNER AND cc.CONSTRAINT_NAME=c.CONSTRAINT_NAME
  JOIN ALL_CONSTRAINTS pc
    ON pc.OWNER=c.R_OWNER AND pc.CONSTRAINT_NAME=c.R_CONSTRAINT_NAME
  JOIN ALL_CONS_COLUMNS pcc
    ON pcc.OWNER=pc.OWNER AND pcc.CONSTRAINT_NAME=pc.CONSTRAINT_NAME
   AND pcc.POSITION=cc.POSITION
 WHERE c.OWNER='CIF'
   AND c.TABLE_NAME='PARTY_EMPLOYMENT'
   AND c.CONSTRAINT_NAME IN ('FK_EMP_GEO_JOB','FK_EMP_GEO_JOB_GROUP')
 ORDER BY c.CONSTRAINT_NAME;

PROMPT Expected mappings:
PROMPT   OCCUPATION_CODE       -> GEO.JOBS.JOB_CODE
PROMPT   OCCUPATION_GROUP_CODE -> GEO.JOB_GROUPS.JOB_GROUP_CODE
PROMPT =====================================================================
