-- Core Banking Prototype 0.11.0
-- DPS2 Phase 11A - Account Operations Schema Reconciliation
--
-- Purpose
--   Reconcile legacy Phase 4/9 constraints/defaults with the already-deployed
--   Account Operations physical model before implementing Phase 11 business APIs.
--
-- Scope (no business data mutation)
--   1) DEPOSIT_ACCOUNT status contract: allow PENDING_ACTIVATION / ACTIVE /
--      SUSPENDED / DORMANT / CLOSED and remove the legacy 3-state guard.
--   2) DEPOSIT_ACCOUNT_LIFECYCLE_EVENT event contract: allow CREATE / ACTIVATE /
--      SUSPEND / REACTIVATE / MARK_DORMANT / CLOSE / REOPEN.
--   3) DEPOSIT_ACTIVATION_RUN.RUN_STATUS_CODE default: STARTED (not numeric 0).
--   4) DEPOSIT_ACCOUNT_EXT_REGISTRY.REGISTRATION_STATUS_CODE default: NOT_SENT
--      (not numeric 0).
--
-- Safety
--   * Idempotent: safe to rerun.
--   * DDL only; no INSERT/UPDATE/DELETE/MERGE.
--   * Existing data is validated by the canonical constraints.
--   * Stops immediately when a required table/column is missing.
--
-- Recommended execution: SQL Developer -> Run Script (F5)

SET DEFINE OFF;
SET SERVEROUTPUT ON;
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK;

PROMPT ============================================================
PROMPT DPS2 Phase 11A - Account Operations Schema Reconciliation
PROMPT Core Banking Prototype 0.11.0
PROMPT ============================================================

SELECT SYS_CONTEXT('USERENV','SESSION_USER') AS SESSION_USER,
       SYS_CONTEXT('USERENV','CURRENT_SCHEMA') AS CURRENT_SCHEMA
  FROM DUAL;

DECLARE
    FUNCTION table_exists(p_table VARCHAR2) RETURN BOOLEAN IS
        v_count NUMBER;
    BEGIN
        SELECT COUNT(*) INTO v_count
          FROM ALL_TABLES
         WHERE OWNER = 'DPS2'
           AND TABLE_NAME = UPPER(p_table);
        RETURN v_count > 0;
    END;

    FUNCTION column_exists(p_table VARCHAR2, p_column VARCHAR2) RETURN BOOLEAN IS
        v_count NUMBER;
    BEGIN
        SELECT COUNT(*) INTO v_count
          FROM ALL_TAB_COLUMNS
         WHERE OWNER = 'DPS2'
           AND TABLE_NAME = UPPER(p_table)
           AND COLUMN_NAME = UPPER(p_column);
        RETURN v_count > 0;
    END;

    FUNCTION constraint_exists(p_table VARCHAR2, p_constraint VARCHAR2) RETURN BOOLEAN IS
        v_count NUMBER;
    BEGIN
        SELECT COUNT(*) INTO v_count
          FROM ALL_CONSTRAINTS
         WHERE OWNER = 'DPS2'
           AND TABLE_NAME = UPPER(p_table)
           AND CONSTRAINT_NAME = UPPER(p_constraint);
        RETURN v_count > 0;
    END;

    PROCEDURE require_table(p_table VARCHAR2) IS
    BEGIN
        IF NOT table_exists(p_table) THEN
            RAISE_APPLICATION_ERROR(
                -21100,
                'Required table DPS2.' || UPPER(p_table) || ' is missing. ' ||
                'Install/reconcile the Account Operations physical model before Phase 11A.'
            );
        END IF;
    END;

    PROCEDURE require_column(p_table VARCHAR2, p_column VARCHAR2) IS
    BEGIN
        IF NOT column_exists(p_table, p_column) THEN
            RAISE_APPLICATION_ERROR(
                -21101,
                'Required column DPS2.' || UPPER(p_table) || '.' || UPPER(p_column) ||
                ' is missing. Stop and reconcile the physical model.'
            );
        END IF;
    END;

    PROCEDURE drop_constraint_if_exists(p_table VARCHAR2, p_constraint VARCHAR2) IS
    BEGIN
        IF constraint_exists(p_table, p_constraint) THEN
            EXECUTE IMMEDIATE
                'ALTER TABLE DPS2.' || DBMS_ASSERT.SIMPLE_SQL_NAME(UPPER(p_table)) ||
                ' DROP CONSTRAINT ' || DBMS_ASSERT.SIMPLE_SQL_NAME(UPPER(p_constraint));
            DBMS_OUTPUT.PUT_LINE('DROP: DPS2.' || UPPER(p_table) || '.' || UPPER(p_constraint));
        END IF;
    END;

BEGIN
    -- Hard prerequisites from the current Account Operations physical model.
    require_table('DEPOSIT_ACCOUNT');
    require_column('DEPOSIT_ACCOUNT', 'ACCOUNT_STATUS_CODE');

    require_table('DEPOSIT_ACCOUNT_LIFECYCLE_EVENT');
    require_column('DEPOSIT_ACCOUNT_LIFECYCLE_EVENT', 'EVENT_TYPE_CODE');

    require_table('DEPOSIT_ACTIVATION_RUN');
    require_column('DEPOSIT_ACTIVATION_RUN', 'RUN_STATUS_CODE');

    require_table('DEPOSIT_ACCOUNT_EXT_REGISTRY');
    require_column('DEPOSIT_ACCOUNT_EXT_REGISTRY', 'REGISTRATION_STATUS_CODE');

    DBMS_OUTPUT.PUT_LINE('OK  : Phase 11A prerequisite tables/columns exist.');

    -- 1) Account status contract.
    -- Remove the legacy Phase 4/9 3-state constraint and recreate the canonical one.
    drop_constraint_if_exists('DEPOSIT_ACCOUNT', 'CK_DEP_ACCOUNT_STATUS');
    drop_constraint_if_exists('DEPOSIT_ACCOUNT', 'CHK_DEPOSIT_ACCOUNT_ACCOUNT_STATUS_CODE');

    EXECUTE IMMEDIATE q'~
        ALTER TABLE DPS2.DEPOSIT_ACCOUNT
        ADD CONSTRAINT CHK_DEPOSIT_ACCOUNT_ACCOUNT_STATUS_CODE
        CHECK (ACCOUNT_STATUS_CODE IN (
            'PENDING_ACTIVATION','ACTIVE','SUSPENDED','DORMANT','CLOSED'
        )) ENABLE VALIDATE
    ~';
    DBMS_OUTPUT.PUT_LINE('ADD : canonical DEPOSIT_ACCOUNT account-status contract.');

    -- 2) Lifecycle event contract.
    drop_constraint_if_exists('DEPOSIT_ACCOUNT_LIFECYCLE_EVENT', 'CK_DEP_ACCT_EVT_TYPE');
    drop_constraint_if_exists(
        'DEPOSIT_ACCOUNT_LIFECYCLE_EVENT',
        'CHK_DEPOSIT_ACCOUNT_LIFECYCLE_EVENT_EVENT_TYPE_CODE'
    );

    EXECUTE IMMEDIATE q'~
        ALTER TABLE DPS2.DEPOSIT_ACCOUNT_LIFECYCLE_EVENT
        ADD CONSTRAINT CHK_DEPOSIT_ACCOUNT_LIFECYCLE_EVENT_EVENT_TYPE_CODE
        CHECK (EVENT_TYPE_CODE IN (
            'CREATE','ACTIVATE','SUSPEND','REACTIVATE','MARK_DORMANT','CLOSE','REOPEN'
        )) ENABLE VALIDATE
    ~';
    DBMS_OUTPUT.PUT_LINE('ADD : canonical DEPOSIT_ACCOUNT_LIFECYCLE_EVENT event contract.');

    -- 3) Activation-run default must belong to its own status domain.
    EXECUTE IMMEDIATE q'~
        ALTER TABLE DPS2.DEPOSIT_ACTIVATION_RUN
        MODIFY (RUN_STATUS_CODE DEFAULT 'STARTED')
    ~';
    DBMS_OUTPUT.PUT_LINE('MOD : DEPOSIT_ACTIVATION_RUN.RUN_STATUS_CODE DEFAULT STARTED.');

    -- 4) External-registry default must belong to its own status domain.
    EXECUTE IMMEDIATE q'~
        ALTER TABLE DPS2.DEPOSIT_ACCOUNT_EXT_REGISTRY
        MODIFY (REGISTRATION_STATUS_CODE DEFAULT 'NOT_SENT')
    ~';
    DBMS_OUTPUT.PUT_LINE('MOD : DEPOSIT_ACCOUNT_EXT_REGISTRY.REGISTRATION_STATUS_CODE DEFAULT NOT_SENT.');

    DBMS_OUTPUT.PUT_LINE('------------------------------------------------------------');
    DBMS_OUTPUT.PUT_LINE('SUCCESS: Phase 11A Account Operations schema reconciliation completed.');
    DBMS_OUTPUT.PUT_LINE('NOTE: No business row was inserted, updated, deleted or merged.');
END;
/

PROMPT ------------------------------------------------------------
PROMPT Phase 11A post-migration summary
PROMPT ------------------------------------------------------------

SELECT TABLE_NAME,
       CONSTRAINT_NAME,
       STATUS,
       VALIDATED,
       SEARCH_CONDITION_VC
  FROM ALL_CONSTRAINTS
 WHERE OWNER = 'DPS2'
   AND (
       (TABLE_NAME = 'DEPOSIT_ACCOUNT'
        AND CONSTRAINT_NAME = 'CHK_DEPOSIT_ACCOUNT_ACCOUNT_STATUS_CODE')
       OR
       (TABLE_NAME = 'DEPOSIT_ACCOUNT_LIFECYCLE_EVENT'
        AND CONSTRAINT_NAME = 'CHK_DEPOSIT_ACCOUNT_LIFECYCLE_EVENT_EVENT_TYPE_CODE')
   )
 ORDER BY TABLE_NAME, CONSTRAINT_NAME;

SELECT TABLE_NAME,
       COLUMN_NAME,
       NULLABLE,
       DEFAULT_LENGTH
  FROM ALL_TAB_COLUMNS
 WHERE OWNER = 'DPS2'
   AND (
       (TABLE_NAME = 'DEPOSIT_ACTIVATION_RUN' AND COLUMN_NAME = 'RUN_STATUS_CODE')
       OR
       (TABLE_NAME = 'DEPOSIT_ACCOUNT_EXT_REGISTRY' AND COLUMN_NAME = 'REGISTRATION_STATUS_CODE')
   )
 ORDER BY TABLE_NAME, COLUMN_NAME;

PROMPT ============================================================
PROMPT PHASE11A_SCHEMA_RECONCILIATION_PASS
PROMPT ============================================================
