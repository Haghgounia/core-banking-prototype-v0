set serveroutput on
set define off
prompt ============================================================
prompt DPS2 Phase 10F - Opening check RESULT_STATUS_CODE reconciliation
prompt Contract: PASS / FAIL / PENDING / WAIVED / NOT_APPLICABLE
prompt ============================================================

DECLARE
  l_invalid_rows NUMBER;
  l_count        NUMBER;
BEGIN
  SELECT COUNT(*)
    INTO l_invalid_rows
    FROM DPS2.DEPOSIT_OPENING_CHECK
   WHERE RESULT_STATUS_CODE IS NOT NULL
     AND RESULT_STATUS_CODE NOT IN ('PASS','FAIL','PENDING','WAIVED','NOT_APPLICABLE');

  DBMS_OUTPUT.PUT_LINE('Existing non-canonical row count: ' || l_invalid_rows);

  IF l_invalid_rows > 0 THEN
    RAISE_APPLICATION_ERROR(
      -20051,
      'DEPOSIT_OPENING_CHECK contains non-canonical RESULT_STATUS_CODE values. Reconcile business data before changing the constraint.'
    );
  END IF;

  SELECT COUNT(*)
    INTO l_count
    FROM all_constraints
   WHERE owner = 'DPS2'
     AND table_name = 'DEPOSIT_OPENING_CHECK'
     AND constraint_name = 'CHK_DEPOSIT_OPENING_CHECK_RESULT_STATUS_CODE';

  IF l_count > 0 THEN
    EXECUTE IMMEDIATE q'[
      ALTER TABLE DPS2.DEPOSIT_OPENING_CHECK
      DROP CONSTRAINT CHK_DEPOSIT_OPENING_CHECK_RESULT_STATUS_CODE
    ]';
    DBMS_OUTPUT.PUT_LINE('DROP: DPS2.DEPOSIT_OPENING_CHECK.CHK_DEPOSIT_OPENING_CHECK_RESULT_STATUS_CODE');
  ELSE
    DBMS_OUTPUT.PUT_LINE('SKIP: legacy/result-status constraint name is not present.');
  END IF;

  EXECUTE IMMEDIATE q'[
    ALTER TABLE DPS2.DEPOSIT_OPENING_CHECK
    ADD CONSTRAINT CHK_DEPOSIT_OPENING_CHECK_RESULT_STATUS_CODE
    CHECK (RESULT_STATUS_CODE IN ('PASS','FAIL','PENDING','WAIVED','NOT_APPLICABLE'))
    ENABLE
  ]';
  DBMS_OUTPUT.PUT_LINE('ADD : DPS2.DEPOSIT_OPENING_CHECK.CHK_DEPOSIT_OPENING_CHECK_RESULT_STATUS_CODE');
END;
/

prompt
prompt === Verification: canonical constraint ===
column constraint_name format a60
column status format a10
column search_condition_vc format a120
select constraint_name, status, search_condition_vc
  from all_constraints
 where owner = 'DPS2'
   and table_name = 'DEPOSIT_OPENING_CHECK'
   and constraint_name = 'CHK_DEPOSIT_OPENING_CHECK_RESULT_STATUS_CODE';

prompt
prompt === Verification: reference catalog ===
column result_status_code format a24
select result_status_code, is_active
  from DPS2.REF_DEP_OPEN_CHECK_RESULT
 where result_status_code in ('PASS','FAIL','PENDING','WAIVED','NOT_APPLICABLE')
 order by result_status_code;

prompt
prompt === Verification: non-canonical business rows ===
select count(*) as NON_CANONICAL_ROWS
  from DPS2.DEPOSIT_OPENING_CHECK
 where RESULT_STATUS_CODE IS NOT NULL
   and RESULT_STATUS_CODE NOT IN ('PASS','FAIL','PENDING','WAIVED','NOT_APPLICABLE');

prompt ============================================================
prompt SUCCESS: Phase 10F opening-check result status contract reconciled.
prompt Canonical statuses: PASS, FAIL, PENDING, WAIVED, NOT_APPLICABLE.
prompt NOTE: No existing business row was inserted, updated or deleted.
prompt ============================================================
