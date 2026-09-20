set serveroutput on
set define off
prompt ============================================================
prompt DPS2 Phase 10F - Created account reference reconciliation
prompt Contract: CREATED_ACCOUNT_ID is the persisted integration reference.
prompt           CREATED_ACCOUNT_NO remains resolvable from DEPOSIT_ACCOUNT.
prompt ============================================================

DECLARE
  PROCEDURE drop_constraint_if_exists(p_constraint_name VARCHAR2) IS
    l_count NUMBER;
  BEGIN
    SELECT COUNT(*) INTO l_count
      FROM all_constraints
     WHERE owner = 'DPS2'
       AND table_name = 'DEPOSIT_OPENING_REQUEST'
       AND constraint_name = UPPER(p_constraint_name);

    IF l_count > 0 THEN
      EXECUTE IMMEDIATE 'ALTER TABLE DPS2.DEPOSIT_OPENING_REQUEST DROP CONSTRAINT ' || DBMS_ASSERT.SIMPLE_SQL_NAME(UPPER(p_constraint_name));
      DBMS_OUTPUT.PUT_LINE('DROP: DPS2.DEPOSIT_OPENING_REQUEST.' || UPPER(p_constraint_name));
    ELSE
      DBMS_OUTPUT.PUT_LINE('SKIP: constraint not present: ' || UPPER(p_constraint_name));
    END IF;
  END;

  PROCEDURE add_completed_constraint IS
    l_count NUMBER;
  BEGIN
    SELECT COUNT(*) INTO l_count
      FROM all_constraints
     WHERE owner = 'DPS2'
       AND table_name = 'DEPOSIT_OPENING_REQUEST'
       AND constraint_name = 'CHK_DEPOSIT_OPENING_REQUEST_REQUEST_STATUS_CODE_CREATED_ACCOUNT_ID';

    IF l_count = 0 THEN
      EXECUTE IMMEDIATE q'[
        ALTER TABLE DPS2.DEPOSIT_OPENING_REQUEST
        ADD CONSTRAINT CHK_DEPOSIT_OPENING_REQUEST_REQUEST_STATUS_CODE_CREATED_ACCOUNT_ID
        CHECK (REQUEST_STATUS_CODE <> 'COMPLETED' OR CREATED_ACCOUNT_ID IS NOT NULL)
        ENABLE
      ]';
      DBMS_OUTPUT.PUT_LINE('ADD : CHK_DEPOSIT_OPENING_REQUEST_REQUEST_STATUS_CODE_CREATED_ACCOUNT_ID');
    ELSE
      DBMS_OUTPUT.PUT_LINE('OK  : CHK_DEPOSIT_OPENING_REQUEST_REQUEST_STATUS_CODE_CREATED_ACCOUNT_ID already exists');
    END IF;
  END;
BEGIN
  -- Obsolete pair rule: Phase 10 does not persist CREATED_ACCOUNT_NO.
  drop_constraint_if_exists('CHK_DEPOSIT_OPENING_REQUEST_CREATED_ACCOUNT_ID_CREATED_ACCOUNT_NO');

  -- Obsolete completion rule required both ID and number.
  drop_constraint_if_exists('CHK_DEPOSIT_OPENING_REQUEST_REQUEST_STATUS_CODE_CREATED_ACCOUNT_ID_CREATED_ACCOUNT_NO');

  -- Final contract: completed opening must have an account integration reference.
  add_completed_constraint;
END;
/

prompt
prompt === Verification ===
column constraint_name format a95
column status format a10
select constraint_name, status
  from all_constraints
 where owner = 'DPS2'
   and table_name = 'DEPOSIT_OPENING_REQUEST'
   and constraint_name in (
     'CHK_DEPOSIT_OPENING_REQUEST_CREATED_ACCOUNT_ID_CREATED_ACCOUNT_NO',
     'CHK_DEPOSIT_OPENING_REQUEST_REQUEST_STATUS_CODE_CREATED_ACCOUNT_ID_CREATED_ACCOUNT_NO',
     'CHK_DEPOSIT_OPENING_REQUEST_REQUEST_STATUS_CODE_CREATED_ACCOUNT_ID'
   )
 order by constraint_name;

prompt
prompt === Existing rows violating final completion contract ===
select count(*) as INVALID_COMPLETED_ROWS
  from DPS2.DEPOSIT_OPENING_REQUEST
 where REQUEST_STATUS_CODE = 'COMPLETED'
   and CREATED_ACCOUNT_ID is null;

prompt ============================================================
prompt SUCCESS: Phase 10F created-account reference contract reconciled.
prompt NOTE: CREATED_ACCOUNT_NO remains nullable and is not populated by this migration.
prompt NOTE: No business row is inserted, updated or deleted.
prompt ============================================================
