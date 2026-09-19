SET DEFINE OFF
SET SERVEROUTPUT ON
SET FEEDBACK ON
SET VERIFY OFF
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

PROMPT ============================================================
PROMPT DPS2 Phase 10A - Constraint Reconciliation Repair 0.10.0-R2
PROMPT Purpose: complete constraints/indexes after ORA-02275 on v5 foundation
PROMPT ============================================================

DECLARE
  FUNCTION constraint_name_exists(p_name VARCHAR2) RETURN BOOLEAN IS
    v_count NUMBER;
  BEGIN
    SELECT COUNT(*) INTO v_count
      FROM ALL_CONSTRAINTS
     WHERE OWNER='DPS2' AND CONSTRAINT_NAME=UPPER(p_name);
    RETURN v_count > 0;
  END;

  FUNCTION find_fk(
      p_table VARCHAR2,
      p_column VARCHAR2,
      p_ref_table VARCHAR2,
      p_ref_column VARCHAR2
  ) RETURN VARCHAR2 IS
    v_name VARCHAR2(128);
  BEGIN
    SELECT MIN(c.CONSTRAINT_NAME)
      INTO v_name
      FROM ALL_CONSTRAINTS c
      JOIN ALL_CONS_COLUMNS cc
        ON cc.OWNER=c.OWNER
       AND cc.CONSTRAINT_NAME=c.CONSTRAINT_NAME
      JOIN ALL_CONSTRAINTS p
        ON p.OWNER=c.R_OWNER
       AND p.CONSTRAINT_NAME=c.R_CONSTRAINT_NAME
      JOIN ALL_CONS_COLUMNS pc
        ON pc.OWNER=p.OWNER
       AND pc.CONSTRAINT_NAME=p.CONSTRAINT_NAME
       AND pc.POSITION=cc.POSITION
     WHERE c.OWNER='DPS2'
       AND c.TABLE_NAME=UPPER(p_table)
       AND c.CONSTRAINT_TYPE='R'
       AND cc.COLUMN_NAME=UPPER(p_column)
       AND p.OWNER='DPS2'
       AND p.TABLE_NAME=UPPER(p_ref_table)
       AND pc.COLUMN_NAME=UPPER(p_ref_column)
       AND (SELECT COUNT(*)
              FROM ALL_CONS_COLUMNS z
             WHERE z.OWNER=c.OWNER
               AND z.CONSTRAINT_NAME=c.CONSTRAINT_NAME)=1;
    RETURN v_name;
  END;

  FUNCTION find_check(
      p_table VARCHAR2,
      p_condition VARCHAR2
  ) RETURN VARCHAR2 IS
    v_name VARCHAR2(128);
  BEGIN
    SELECT MIN(CONSTRAINT_NAME)
      INTO v_name
      FROM ALL_CONSTRAINTS
     WHERE OWNER='DPS2'
       AND TABLE_NAME=UPPER(p_table)
       AND CONSTRAINT_TYPE='C'
       AND REGEXP_REPLACE(UPPER(SEARCH_CONDITION_VC),'[[:space:]\"]','') =
           REGEXP_REPLACE(UPPER(p_condition),'[[:space:]\"]','');
    RETURN v_name;
  EXCEPTION
    WHEN OTHERS THEN
      -- SEARCH_CONDITION_VC comparison is only a duplicate-avoidance optimization.
      RETURN NULL;
  END;

  FUNCTION find_leading_index(
      p_table VARCHAR2,
      p_column VARCHAR2
  ) RETURN VARCHAR2 IS
    v_name VARCHAR2(128);
  BEGIN
    SELECT MIN(INDEX_NAME)
      INTO v_name
      FROM ALL_IND_COLUMNS
     WHERE INDEX_OWNER='DPS2'
       AND TABLE_OWNER='DPS2'
       AND TABLE_NAME=UPPER(p_table)
       AND COLUMN_POSITION=1
       AND COLUMN_NAME=UPPER(p_column);
    RETURN v_name;
  END;

  PROCEDURE add_fk(
      p_name VARCHAR2,
      p_table VARCHAR2,
      p_column VARCHAR2,
      p_ref_table VARCHAR2,
      p_ref_column VARCHAR2
  ) IS
    v_existing VARCHAR2(128);
    v_orphans NUMBER;
    v_sql VARCHAR2(4000);
  BEGIN
    v_existing := find_fk(p_table,p_column,p_ref_table,p_ref_column);
    IF v_existing IS NOT NULL THEN
      DBMS_OUTPUT.PUT_LINE('OK  : '||p_table||'.'||p_column||' -> '||p_ref_table||'.'||p_ref_column||' already enforced as '||v_existing);
      RETURN;
    END IF;

    IF constraint_name_exists(p_name) THEN
      RAISE_APPLICATION_ERROR(-21031,'Constraint name DPS2.'||p_name||' exists but does not enforce expected FK semantics.');
    END IF;

    v_sql := 'SELECT COUNT(*) FROM DPS2.'||p_table||' c WHERE c.'||p_column||' IS NOT NULL '
          || 'AND NOT EXISTS (SELECT 1 FROM DPS2.'||p_ref_table||' p WHERE p.'||p_ref_column||'=c.'||p_column||')';
    EXECUTE IMMEDIATE v_sql INTO v_orphans;
    IF v_orphans > 0 THEN
      RAISE_APPLICATION_ERROR(-21032,'Cannot create '||p_name||': '||v_orphans||' orphan row(s) in '||p_table||'.'||p_column);
    END IF;

    EXECUTE IMMEDIATE 'ALTER TABLE DPS2.'||p_table||' ADD CONSTRAINT '||p_name||
      ' FOREIGN KEY ('||p_column||') REFERENCES DPS2.'||p_ref_table||' ('||p_ref_column||')';
    DBMS_OUTPUT.PUT_LINE('ADD : '||p_name||'  '||p_table||'.'||p_column||' -> '||p_ref_table||'.'||p_ref_column);
  END;

  PROCEDURE add_check(
      p_name VARCHAR2,
      p_table VARCHAR2,
      p_condition VARCHAR2
  ) IS
    v_existing VARCHAR2(128);
  BEGIN
    v_existing := find_check(p_table,p_condition);
    IF v_existing IS NOT NULL THEN
      DBMS_OUTPUT.PUT_LINE('OK  : check '||p_table||' '||p_condition||' already enforced as '||v_existing);
      RETURN;
    END IF;

    IF constraint_name_exists(p_name) THEN
      DBMS_OUTPUT.PUT_LINE('OK  : check constraint name '||p_name||' already exists.');
      RETURN;
    END IF;

    EXECUTE IMMEDIATE 'ALTER TABLE DPS2.'||p_table||' ADD CONSTRAINT '||p_name||' CHECK ('||p_condition||')';
    DBMS_OUTPUT.PUT_LINE('ADD : check '||p_name||' on '||p_table);
  END;

  PROCEDURE add_index(
      p_name VARCHAR2,
      p_table VARCHAR2,
      p_column VARCHAR2
  ) IS
    v_existing VARCHAR2(128);
    v_count NUMBER;
  BEGIN
    v_existing := find_leading_index(p_table,p_column);
    IF v_existing IS NOT NULL THEN
      DBMS_OUTPUT.PUT_LINE('OK  : index coverage '||p_table||'.'||p_column||' via '||v_existing);
      RETURN;
    END IF;

    SELECT COUNT(*) INTO v_count
      FROM ALL_INDEXES
     WHERE OWNER='DPS2' AND INDEX_NAME=UPPER(p_name);
    IF v_count > 0 THEN
      RAISE_APPLICATION_ERROR(-21033,'Index name DPS2.'||p_name||' already exists but does not cover expected leading column.');
    END IF;

    EXECUTE IMMEDIATE 'CREATE INDEX DPS2.'||p_name||' ON DPS2.'||p_table||' ('||p_column||')';
    DBMS_OUTPUT.PUT_LINE('ADD : index '||p_name||' on '||p_table||'('||p_column||')');
  END;
BEGIN
  DBMS_OUTPUT.PUT_LINE('=== Foreign-key reconciliation by semantics (name-independent) ===');

  add_fk('FK_DOR_JOINT_BASIS','DEPOSIT_OPENING_REQUEST','JOINT_ACCOUNT_BASIS_CODE','REF_DEP_OPEN_JOINT_BASIS','JOINT_ACCOUNT_BASIS_CODE');
  add_fk('FK_DOR_ACT_STATUS','DEPOSIT_OPENING_REQUEST','ACTIVATION_STATUS_CODE','REF_DEP_OPEN_ACTIVATION_STATUS','ACTIVATION_STATUS_CODE');
  add_fk('FK_DOF_FUND_PURPOSE','DEPOSIT_OPENING_FUNDING','FUNDING_PURPOSE_CODE','REF_DEP_OPEN_FUND_PURPOSE','FUNDING_PURPOSE_CODE');

  add_fk('FK_DOCHECK_PHASE','DEPOSIT_OPENING_CHECK','CHECK_PHASE_CODE','REF_DEP_OPEN_CHECK_PHASE','CHECK_PHASE_CODE');
  add_fk('FK_DOCHECK_SCOPE','DEPOSIT_OPENING_CHECK','BLOCKING_SCOPE_CODE','REF_DEP_OPEN_BLOCKING_SCOPE','BLOCKING_SCOPE_CODE');

  add_fk('FK_DOO_REQ','DEPOSIT_OPENING_OBLIGATION','OPENING_REQUEST_ID','DEPOSIT_OPENING_REQUEST','OPENING_REQUEST_ID');
  add_fk('FK_DOO_TYPE','DEPOSIT_OPENING_OBLIGATION','OBLIGATION_TYPE_CODE','REF_DEP_OPEN_OBLIGATION_TYPE','OBLIGATION_TYPE_CODE');
  add_fk('FK_DOO_SETTLE','DEPOSIT_OPENING_OBLIGATION','SETTLEMENT_STATUS_CODE','REF_DEP_OPEN_SETTLEMENT_STATUS','SETTLEMENT_STATUS_CODE');

  add_fk('FK_DOFA_FUNDING','DEPOSIT_OPENING_FUND_ALLOC','OPENING_FUNDING_ID','DEPOSIT_OPENING_FUNDING','OPENING_FUNDING_ID');
  add_fk('FK_DOFA_OBLIGATION','DEPOSIT_OPENING_FUND_ALLOC','OPENING_OBLIGATION_ID','DEPOSIT_OPENING_OBLIGATION','OPENING_OBLIGATION_ID');

  DBMS_OUTPUT.PUT_LINE('=== Check-constraint reconciliation ===');
  add_check('CK_DOF_OWN_VER','DEPOSIT_OPENING_FUNDING','SOURCE_OWNERSHIP_VERIFIED_FLAG IN (0,1)');
  add_check('CK_DOC_REQ_FLAG','DEPOSIT_OPENING_CHECK','REQUIRED_FLAG IN (0,1)');
  add_check('CK_DOC_RECHECK_FLAG','DEPOSIT_OPENING_CHECK','RECHECK_REQUIRED_FLAG IN (0,1)');
  add_check('CK_DOO_MANDATORY','DEPOSIT_OPENING_OBLIGATION','MANDATORY_FOR_ACTIVATION_FLAG IN (0,1)');

  DBMS_OUTPUT.PUT_LINE('=== Supporting-index reconciliation ===');
  add_index('IX_DOO_REQUEST','DEPOSIT_OPENING_OBLIGATION','OPENING_REQUEST_ID');
  add_index('IX_DOFA_FUNDING','DEPOSIT_OPENING_FUND_ALLOC','OPENING_FUNDING_ID');
  add_index('IX_DOFA_OBLIGATION','DEPOSIT_OPENING_FUND_ALLOC','OPENING_OBLIGATION_ID');
END;
/

COMMIT;

PROMPT ============================================================
PROMPT Verification - semantic FKs
PROMPT ============================================================
SELECT c.TABLE_NAME,
       c.CONSTRAINT_NAME,
       cc.COLUMN_NAME,
       p.TABLE_NAME AS REFERENCED_TABLE,
       pc.COLUMN_NAME AS REFERENCED_COLUMN,
       c.STATUS
  FROM ALL_CONSTRAINTS c
  JOIN ALL_CONS_COLUMNS cc
    ON cc.OWNER=c.OWNER AND cc.CONSTRAINT_NAME=c.CONSTRAINT_NAME
  JOIN ALL_CONSTRAINTS p
    ON p.OWNER=c.R_OWNER AND p.CONSTRAINT_NAME=c.R_CONSTRAINT_NAME
  JOIN ALL_CONS_COLUMNS pc
    ON pc.OWNER=p.OWNER AND pc.CONSTRAINT_NAME=p.CONSTRAINT_NAME AND pc.POSITION=cc.POSITION
 WHERE c.OWNER='DPS2'
   AND c.CONSTRAINT_TYPE='R'
   AND (
        (c.TABLE_NAME='DEPOSIT_OPENING_REQUEST' AND cc.COLUMN_NAME IN ('JOINT_ACCOUNT_BASIS_CODE','ACTIVATION_STATUS_CODE'))
     OR (c.TABLE_NAME='DEPOSIT_OPENING_FUNDING' AND cc.COLUMN_NAME='FUNDING_PURPOSE_CODE')
     OR (c.TABLE_NAME='DEPOSIT_OPENING_CHECK' AND cc.COLUMN_NAME IN ('CHECK_PHASE_CODE','BLOCKING_SCOPE_CODE'))
     OR (c.TABLE_NAME='DEPOSIT_OPENING_OBLIGATION' AND cc.COLUMN_NAME IN ('OPENING_REQUEST_ID','OBLIGATION_TYPE_CODE','SETTLEMENT_STATUS_CODE'))
     OR (c.TABLE_NAME='DEPOSIT_OPENING_FUND_ALLOC' AND cc.COLUMN_NAME IN ('OPENING_FUNDING_ID','OPENING_OBLIGATION_ID'))
   )
 ORDER BY c.TABLE_NAME,cc.COLUMN_NAME;

PROMPT ============================================================
PROMPT Verification - check constraints and supporting indexes
PROMPT ============================================================
SELECT TABLE_NAME,CONSTRAINT_NAME,CONSTRAINT_TYPE,STATUS
  FROM ALL_CONSTRAINTS
 WHERE OWNER='DPS2'
   AND TABLE_NAME IN ('DEPOSIT_OPENING_FUNDING','DEPOSIT_OPENING_CHECK','DEPOSIT_OPENING_OBLIGATION')
   AND CONSTRAINT_TYPE='C'
   AND (
        SEARCH_CONDITION_VC LIKE '%SOURCE_OWNERSHIP_VERIFIED_FLAG%'
     OR SEARCH_CONDITION_VC LIKE '%REQUIRED_FLAG%'
     OR SEARCH_CONDITION_VC LIKE '%RECHECK_REQUIRED_FLAG%'
     OR SEARCH_CONDITION_VC LIKE '%MANDATORY_FOR_ACTIVATION_FLAG%'
   )
 ORDER BY TABLE_NAME,CONSTRAINT_NAME;

SELECT INDEX_NAME,TABLE_NAME,COLUMN_NAME,COLUMN_POSITION
  FROM ALL_IND_COLUMNS
 WHERE INDEX_OWNER='DPS2'
   AND TABLE_OWNER='DPS2'
   AND (
        (TABLE_NAME='DEPOSIT_OPENING_OBLIGATION' AND COLUMN_NAME='OPENING_REQUEST_ID')
     OR (TABLE_NAME='DEPOSIT_OPENING_FUND_ALLOC' AND COLUMN_NAME IN ('OPENING_FUNDING_ID','OPENING_OBLIGATION_ID'))
   )
 ORDER BY TABLE_NAME,INDEX_NAME,COLUMN_POSITION;

PROMPT ============================================================
PROMPT SUCCESS: Phase 10A constraint/index reconciliation completed without duplicate-FK creation.
PROMPT NOTE: Existing equivalent FKs are preserved even if their constraint names differ from the source naming contract.
PROMPT ============================================================
