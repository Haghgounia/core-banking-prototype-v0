-- Core Banking Prototype 0.11.0
-- DPS2 Phase 11A DB verifier
-- Read-only verification; does not mutate business data.

SET DEFINE OFF;
SET SERVEROUTPUT ON;
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK;

PROMPT ============================================================
PROMPT DPS2 Phase 11A - Account Operations DB Verifier
PROMPT ============================================================

DECLARE
    v_pass NUMBER := 0;
    v_fail NUMBER := 0;
    v_count NUMBER;
    v_search VARCHAR2(4000);
    v_ddl CLOB;

    PROCEDURE check_true(p_name VARCHAR2, p_ok BOOLEAN, p_detail VARCHAR2 DEFAULT NULL) IS
    BEGIN
        IF p_ok THEN
            v_pass := v_pass + 1;
            DBMS_OUTPUT.PUT_LINE('PASS | ' || p_name || CASE WHEN p_detail IS NOT NULL THEN ' | ' || p_detail ELSE '' END);
        ELSE
            v_fail := v_fail + 1;
            DBMS_OUTPUT.PUT_LINE('FAIL | ' || p_name || CASE WHEN p_detail IS NOT NULL THEN ' | ' || p_detail ELSE '' END);
        END IF;
    END;

    FUNCTION table_exists(p_table VARCHAR2) RETURN BOOLEAN IS
    BEGIN
        SELECT COUNT(*) INTO v_count
          FROM ALL_TABLES
         WHERE OWNER='DPS2' AND TABLE_NAME=UPPER(p_table);
        RETURN v_count=1;
    END;

    FUNCTION column_exists(p_table VARCHAR2, p_column VARCHAR2) RETURN BOOLEAN IS
    BEGIN
        SELECT COUNT(*) INTO v_count
          FROM ALL_TAB_COLUMNS
         WHERE OWNER='DPS2' AND TABLE_NAME=UPPER(p_table) AND COLUMN_NAME=UPPER(p_column);
        RETURN v_count=1;
    END;

    FUNCTION constraint_condition(p_table VARCHAR2, p_constraint VARCHAR2) RETURN VARCHAR2 IS
        v VARCHAR2(4000);
    BEGIN
        SELECT SEARCH_CONDITION_VC INTO v
          FROM ALL_CONSTRAINTS
         WHERE OWNER='DPS2'
           AND TABLE_NAME=UPPER(p_table)
           AND CONSTRAINT_NAME=UPPER(p_constraint)
           AND STATUS='ENABLED'
           AND VALIDATED='VALIDATED';
        RETURN UPPER(REPLACE(REPLACE(v, CHR(10), ' '), CHR(13), ' '));
    EXCEPTION
        WHEN NO_DATA_FOUND THEN RETURN NULL;
    END;

BEGIN
    check_true('DEPOSIT_ACCOUNT exists', table_exists('DEPOSIT_ACCOUNT'));
    check_true('DEPOSIT_ACCOUNT.ACCOUNT_STATUS_CODE exists', column_exists('DEPOSIT_ACCOUNT','ACCOUNT_STATUS_CODE'));
    check_true('DEPOSIT_ACCOUNT_LIFECYCLE_EVENT exists', table_exists('DEPOSIT_ACCOUNT_LIFECYCLE_EVENT'));
    check_true('DEPOSIT_ACTIVATION_RUN.RUN_STATUS_CODE exists', column_exists('DEPOSIT_ACTIVATION_RUN','RUN_STATUS_CODE'));
    check_true('DEPOSIT_ACCOUNT_EXT_REGISTRY.REGISTRATION_STATUS_CODE exists', column_exists('DEPOSIT_ACCOUNT_EXT_REGISTRY','REGISTRATION_STATUS_CODE'));

    SELECT COUNT(*) INTO v_count
      FROM ALL_CONSTRAINTS
     WHERE OWNER='DPS2'
       AND TABLE_NAME='DEPOSIT_ACCOUNT'
       AND CONSTRAINT_NAME='CK_DEP_ACCOUNT_STATUS';
    check_true('legacy CK_DEP_ACCOUNT_STATUS removed', v_count=0);

    v_search := constraint_condition('DEPOSIT_ACCOUNT','CHK_DEPOSIT_ACCOUNT_ACCOUNT_STATUS_CODE');
    check_true('canonical account-status constraint enabled/validated', v_search IS NOT NULL);
    check_true('account status supports SUSPENDED', INSTR(v_search, '''SUSPENDED''')>0);
    check_true('account status supports DORMANT', INSTR(v_search, '''DORMANT''')>0);
    check_true('account status supports CLOSED', INSTR(v_search, '''CLOSED''')>0);

    SELECT COUNT(*) INTO v_count
      FROM ALL_CONSTRAINTS
     WHERE OWNER='DPS2'
       AND TABLE_NAME='DEPOSIT_ACCOUNT_LIFECYCLE_EVENT'
       AND CONSTRAINT_NAME='CK_DEP_ACCT_EVT_TYPE';
    check_true('legacy CK_DEP_ACCT_EVT_TYPE removed', v_count=0);

    v_search := constraint_condition(
        'DEPOSIT_ACCOUNT_LIFECYCLE_EVENT',
        'CHK_DEPOSIT_ACCOUNT_LIFECYCLE_EVENT_EVENT_TYPE_CODE'
    );
    check_true('canonical lifecycle event constraint enabled/validated', v_search IS NOT NULL);
    check_true('lifecycle supports SUSPEND', INSTR(v_search, '''SUSPEND''')>0);
    check_true('lifecycle supports REACTIVATE', INSTR(v_search, '''REACTIVATE''')>0);
    check_true('lifecycle supports MARK_DORMANT', INSTR(v_search, '''MARK_DORMANT''')>0);
    check_true('lifecycle supports REOPEN', INSTR(v_search, '''REOPEN''')>0);

    v_ddl := DBMS_METADATA.GET_DDL('TABLE','DEPOSIT_ACTIVATION_RUN','DPS2');
    check_true(
        'activation-run default is STARTED',
        DBMS_LOB.INSTR(UPPER(v_ddl), 'RUN_STATUS_CODE')>0
        AND DBMS_LOB.INSTR(UPPER(v_ddl), 'DEFAULT ''STARTED''')>0
    );

    v_ddl := DBMS_METADATA.GET_DDL('TABLE','DEPOSIT_ACCOUNT_EXT_REGISTRY','DPS2');
    check_true(
        'external-registry default is NOT_SENT',
        DBMS_LOB.INSTR(UPPER(v_ddl), 'REGISTRATION_STATUS_CODE')>0
        AND DBMS_LOB.INSTR(UPPER(v_ddl), 'DEFAULT ''NOT_SENT''')>0
    );

    DBMS_OUTPUT.PUT_LINE('------------------------------------------------------------');
    DBMS_OUTPUT.PUT_LINE('PHASE11A_DB_VERIFIER_PASS=' || v_pass);
    DBMS_OUTPUT.PUT_LINE('PHASE11A_DB_VERIFIER_FAIL=' || v_fail);

    IF v_fail > 0 THEN
        RAISE_APPLICATION_ERROR(-21109, 'Phase 11A DB verifier failed; failed checks=' || v_fail);
    END IF;

    DBMS_OUTPUT.PUT_LINE('PHASE11A_DB_BASELINE_PASS');
END;
/
