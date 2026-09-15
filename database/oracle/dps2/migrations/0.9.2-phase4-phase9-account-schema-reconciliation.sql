-- Core Banking Prototype 0.9.2
-- DPS2 prerequisite reconciliation for Phase 4 + Phase 9
-- Purpose:
--   Repair an environment where Deposit Opening exists but the Account Operations
--   Phase 4 objects were never installed (DEPOSIT_ACCOUNT / lifecycle history).
--
-- Safe / idempotent behavior:
--   * Requires DPS2.DEPOSIT_OPENING_REQUEST to exist before doing any account DDL.
--   * Creates missing Phase 4 account objects in DPS2 only.
--   * Reuses partially-created sequences/tables from prior failed attempts.
--   * Enables lifecycle event types CREATE / ACTIVATE / CLOSE.
--   * Adds append-only protection to lifecycle history.
--   * Does NOT create synthetic historical events.
--   * Does NOT touch FEE or any other schema.
--
-- Recommended execution: SQL Developer -> Run Script (F5)

SET DEFINE OFF;
SET SERVEROUTPUT ON;
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK;

PROMPT ============================================================
PROMPT DPS2 Phase 4 + Phase 9 Account Schema Reconciliation 0.9.2
PROMPT ============================================================

SELECT SYS_CONTEXT('USERENV','SESSION_USER') AS SESSION_USER,
       SYS_CONTEXT('USERENV','CURRENT_SCHEMA') AS CURRENT_SCHEMA
  FROM DUAL;

DECLARE
    v_count NUMBER;

    FUNCTION table_exists(p_owner VARCHAR2, p_table VARCHAR2) RETURN BOOLEAN IS
        v NUMBER;
    BEGIN
        SELECT COUNT(*) INTO v
          FROM ALL_TABLES
         WHERE OWNER = UPPER(p_owner)
           AND TABLE_NAME = UPPER(p_table);
        RETURN v > 0;
    END;

    FUNCTION column_exists(p_owner VARCHAR2, p_table VARCHAR2, p_column VARCHAR2) RETURN BOOLEAN IS
        v NUMBER;
    BEGIN
        SELECT COUNT(*) INTO v
          FROM ALL_TAB_COLUMNS
         WHERE OWNER = UPPER(p_owner)
           AND TABLE_NAME = UPPER(p_table)
           AND COLUMN_NAME = UPPER(p_column);
        RETURN v > 0;
    END;

    FUNCTION sequence_exists(p_owner VARCHAR2, p_sequence VARCHAR2) RETURN BOOLEAN IS
        v NUMBER;
    BEGIN
        SELECT COUNT(*) INTO v
          FROM ALL_SEQUENCES
         WHERE SEQUENCE_OWNER = UPPER(p_owner)
           AND SEQUENCE_NAME = UPPER(p_sequence);
        RETURN v > 0;
    END;

    FUNCTION constraint_exists(p_owner VARCHAR2, p_table VARCHAR2, p_constraint VARCHAR2) RETURN BOOLEAN IS
        v NUMBER;
    BEGIN
        SELECT COUNT(*) INTO v
          FROM ALL_CONSTRAINTS
         WHERE OWNER = UPPER(p_owner)
           AND TABLE_NAME = UPPER(p_table)
           AND CONSTRAINT_NAME = UPPER(p_constraint);
        RETURN v > 0;
    END;

    FUNCTION index_exists(p_owner VARCHAR2, p_index VARCHAR2) RETURN BOOLEAN IS
        v NUMBER;
    BEGIN
        SELECT COUNT(*) INTO v
          FROM ALL_INDEXES
         WHERE OWNER = UPPER(p_owner)
           AND INDEX_NAME = UPPER(p_index);
        RETURN v > 0;
    END;

    PROCEDURE require_column(p_table VARCHAR2, p_column VARCHAR2) IS
    BEGIN
        IF NOT column_exists('DPS2', p_table, p_column) THEN
            RAISE_APPLICATION_ERROR(
                -20922,
                'Existing DPS2.' || p_table || ' is incompatible: missing column ' || p_column ||
                '. Stop and reconcile the physical model before continuing.'
            );
        END IF;
    END;

BEGIN
    -- 0) Hard prerequisite: the Opening bounded context must already exist.
    IF NOT table_exists('DPS2', 'DEPOSIT_OPENING_REQUEST') THEN
        RAISE_APPLICATION_ERROR(
            -20920,
            'DPS2.DEPOSIT_OPENING_REQUEST is missing. The base DPS2 Deposit Opening DDL must be installed before Phase 4/9.'
        );
    END IF;

    DBMS_OUTPUT.PUT_LINE('OK  : DPS2.DEPOSIT_OPENING_REQUEST exists.');

    -- 1) Opening -> Account integration reference.
    IF NOT column_exists('DPS2', 'DEPOSIT_OPENING_REQUEST', 'CREATED_ACCOUNT_ID') THEN
        EXECUTE IMMEDIATE
            'ALTER TABLE DPS2.DEPOSIT_OPENING_REQUEST ADD (CREATED_ACCOUNT_ID NUMBER(19))';
        DBMS_OUTPUT.PUT_LINE('ADD : DPS2.DEPOSIT_OPENING_REQUEST.CREATED_ACCOUNT_ID');
    ELSE
        DBMS_OUTPUT.PUT_LINE('OK  : CREATED_ACCOUNT_ID already exists.');
    END IF;

    -- 2) Sequences.
    IF NOT sequence_exists('DPS2', 'SEQ_DEPOSIT_ACCOUNT') THEN
        EXECUTE IMMEDIATE
            'CREATE SEQUENCE DPS2.SEQ_DEPOSIT_ACCOUNT START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE';
        DBMS_OUTPUT.PUT_LINE('ADD : DPS2.SEQ_DEPOSIT_ACCOUNT');
    ELSE
        DBMS_OUTPUT.PUT_LINE('OK  : DPS2.SEQ_DEPOSIT_ACCOUNT already exists.');
    END IF;

    IF NOT sequence_exists('DPS2', 'SEQ_DEP_ACCOUNT_LIFECYCLE_EVT') THEN
        EXECUTE IMMEDIATE
            'CREATE SEQUENCE DPS2.SEQ_DEP_ACCOUNT_LIFECYCLE_EVT START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE';
        DBMS_OUTPUT.PUT_LINE('ADD : DPS2.SEQ_DEP_ACCOUNT_LIFECYCLE_EVT');
    ELSE
        DBMS_OUTPUT.PUT_LINE('OK  : DPS2.SEQ_DEP_ACCOUNT_LIFECYCLE_EVT already exists.');
    END IF;

    -- 3) Phase 4 account table. Create only when wholly absent.
    IF NOT table_exists('DPS2', 'DEPOSIT_ACCOUNT') THEN
        EXECUTE IMMEDIATE q'~
            CREATE TABLE DPS2.DEPOSIT_ACCOUNT (
                ACCOUNT_ID           NUMBER(19)      NOT NULL,
                ACCOUNT_NO           VARCHAR2(32)    NOT NULL,
                OPENING_REQUEST_ID   NUMBER(19)      NOT NULL,
                PRODUCT_VERSION_ID   NUMBER(19),
                CURRENCY_CODE        VARCHAR2(3)     NOT NULL,
                OPENING_AMOUNT       NUMBER(22,4),
                ACCOUNT_STATUS_CODE  VARCHAR2(30)    DEFAULT 'PENDING_ACTIVATION' NOT NULL,
                ACTIVATED_AT         TIMESTAMP(6),
                CREATED_AT           TIMESTAMP(6)    DEFAULT SYSTIMESTAMP NOT NULL,
                CREATED_BY           VARCHAR2(100)   NOT NULL,
                UPDATED_AT           TIMESTAMP(6),
                UPDATED_BY           VARCHAR2(100),
                RECORD_VERSION       NUMBER(19)      DEFAULT 1 NOT NULL,
                CONSTRAINT PK_DEPOSIT_ACCOUNT PRIMARY KEY (ACCOUNT_ID),
                CONSTRAINT UK_DEPOSIT_ACCOUNT_NO UNIQUE (ACCOUNT_NO),
                CONSTRAINT UK_DEPOSIT_ACCOUNT_OPEN_REQ UNIQUE (OPENING_REQUEST_ID),
                CONSTRAINT CK_DEP_ACCOUNT_STATUS
                    CHECK (ACCOUNT_STATUS_CODE IN ('PENDING_ACTIVATION','ACTIVE','CLOSED'))
            )
        ~';
        DBMS_OUTPUT.PUT_LINE('ADD : DPS2.DEPOSIT_ACCOUNT');
    ELSE
        DBMS_OUTPUT.PUT_LINE('OK  : DPS2.DEPOSIT_ACCOUNT already exists; validating required columns.');
        require_column('DEPOSIT_ACCOUNT','ACCOUNT_ID');
        require_column('DEPOSIT_ACCOUNT','ACCOUNT_NO');
        require_column('DEPOSIT_ACCOUNT','OPENING_REQUEST_ID');
        require_column('DEPOSIT_ACCOUNT','PRODUCT_VERSION_ID');
        require_column('DEPOSIT_ACCOUNT','CURRENCY_CODE');
        require_column('DEPOSIT_ACCOUNT','OPENING_AMOUNT');
        require_column('DEPOSIT_ACCOUNT','ACCOUNT_STATUS_CODE');
        require_column('DEPOSIT_ACCOUNT','ACTIVATED_AT');
        require_column('DEPOSIT_ACCOUNT','CREATED_AT');
        require_column('DEPOSIT_ACCOUNT','CREATED_BY');
        require_column('DEPOSIT_ACCOUNT','UPDATED_AT');
        require_column('DEPOSIT_ACCOUNT','UPDATED_BY');
        require_column('DEPOSIT_ACCOUNT','RECORD_VERSION');
    END IF;

    -- Ensure Phase 9 status set even if the table came from a partial/older Phase 4 install.
    IF constraint_exists('DPS2','DEPOSIT_ACCOUNT','CK_DEP_ACCOUNT_STATUS') THEN
        EXECUTE IMMEDIATE
            'ALTER TABLE DPS2.DEPOSIT_ACCOUNT DROP CONSTRAINT CK_DEP_ACCOUNT_STATUS';
    END IF;
    EXECUTE IMMEDIATE q'~
        ALTER TABLE DPS2.DEPOSIT_ACCOUNT
        ADD CONSTRAINT CK_DEP_ACCOUNT_STATUS
        CHECK (ACCOUNT_STATUS_CODE IN ('PENDING_ACTIVATION','ACTIVE','CLOSED'))
    ~';
    DBMS_OUTPUT.PUT_LINE('OK  : CK_DEP_ACCOUNT_STATUS supports PENDING_ACTIVATION / ACTIVE / CLOSED.');

    -- 4) Lifecycle table. Intentionally create without FK first, then add FK explicitly.
    --    This avoids current-schema name-resolution surprises in mixed-schema worksheets.
    IF NOT table_exists('DPS2', 'DEPOSIT_ACCOUNT_LIFECYCLE_EVENT') THEN
        EXECUTE IMMEDIATE q'~
            CREATE TABLE DPS2.DEPOSIT_ACCOUNT_LIFECYCLE_EVENT (
                LIFECYCLE_EVENT_ID  NUMBER(19)      NOT NULL,
                ACCOUNT_ID          NUMBER(19)      NOT NULL,
                OPENING_REQUEST_ID  NUMBER(19)      NOT NULL,
                EVENT_TYPE_CODE     VARCHAR2(30)    NOT NULL,
                FROM_STATUS_CODE    VARCHAR2(30),
                TO_STATUS_CODE      VARCHAR2(30)    NOT NULL,
                CORRELATION_ID      VARCHAR2(100),
                EVENT_AT            TIMESTAMP(6)    DEFAULT SYSTIMESTAMP NOT NULL,
                EVENT_BY            VARCHAR2(100)   NOT NULL,
                CREATED_AT          TIMESTAMP(6)    DEFAULT SYSTIMESTAMP NOT NULL,
                CREATED_BY          VARCHAR2(100)   NOT NULL,
                CONSTRAINT PK_DEP_ACCOUNT_LIFECYCLE_EVT PRIMARY KEY (LIFECYCLE_EVENT_ID)
            )
        ~';
        DBMS_OUTPUT.PUT_LINE('ADD : DPS2.DEPOSIT_ACCOUNT_LIFECYCLE_EVENT');
    ELSE
        DBMS_OUTPUT.PUT_LINE('OK  : DPS2.DEPOSIT_ACCOUNT_LIFECYCLE_EVENT already exists; validating required columns.');
        require_column('DEPOSIT_ACCOUNT_LIFECYCLE_EVENT','LIFECYCLE_EVENT_ID');
        require_column('DEPOSIT_ACCOUNT_LIFECYCLE_EVENT','ACCOUNT_ID');
        require_column('DEPOSIT_ACCOUNT_LIFECYCLE_EVENT','OPENING_REQUEST_ID');
        require_column('DEPOSIT_ACCOUNT_LIFECYCLE_EVENT','EVENT_TYPE_CODE');
        require_column('DEPOSIT_ACCOUNT_LIFECYCLE_EVENT','FROM_STATUS_CODE');
        require_column('DEPOSIT_ACCOUNT_LIFECYCLE_EVENT','TO_STATUS_CODE');
        require_column('DEPOSIT_ACCOUNT_LIFECYCLE_EVENT','CORRELATION_ID');
        require_column('DEPOSIT_ACCOUNT_LIFECYCLE_EVENT','EVENT_AT');
        require_column('DEPOSIT_ACCOUNT_LIFECYCLE_EVENT','EVENT_BY');
        require_column('DEPOSIT_ACCOUNT_LIFECYCLE_EVENT','CREATED_AT');
        require_column('DEPOSIT_ACCOUNT_LIFECYCLE_EVENT','CREATED_BY');
    END IF;

    IF NOT constraint_exists('DPS2','DEPOSIT_ACCOUNT_LIFECYCLE_EVENT','FK_DEP_ACCT_EVT_ACCOUNT') THEN
        EXECUTE IMMEDIATE q'~
            ALTER TABLE DPS2.DEPOSIT_ACCOUNT_LIFECYCLE_EVENT
            ADD CONSTRAINT FK_DEP_ACCT_EVT_ACCOUNT
            FOREIGN KEY (ACCOUNT_ID)
            REFERENCES DPS2.DEPOSIT_ACCOUNT (ACCOUNT_ID)
        ~';
        DBMS_OUTPUT.PUT_LINE('ADD : FK_DEP_ACCT_EVT_ACCOUNT');
    ELSE
        DBMS_OUTPUT.PUT_LINE('OK  : FK_DEP_ACCT_EVT_ACCOUNT already exists.');
    END IF;

    IF constraint_exists('DPS2','DEPOSIT_ACCOUNT_LIFECYCLE_EVENT','CK_DEP_ACCT_EVT_TYPE') THEN
        EXECUTE IMMEDIATE
            'ALTER TABLE DPS2.DEPOSIT_ACCOUNT_LIFECYCLE_EVENT DROP CONSTRAINT CK_DEP_ACCT_EVT_TYPE';
    END IF;
    EXECUTE IMMEDIATE q'~
        ALTER TABLE DPS2.DEPOSIT_ACCOUNT_LIFECYCLE_EVENT
        ADD CONSTRAINT CK_DEP_ACCT_EVT_TYPE
        CHECK (EVENT_TYPE_CODE IN ('CREATE','ACTIVATE','CLOSE'))
    ~';
    DBMS_OUTPUT.PUT_LINE('OK  : CK_DEP_ACCT_EVT_TYPE supports CREATE / ACTIVATE / CLOSE.');

    -- 5) Supporting indexes.
    IF NOT index_exists('DPS2','IX_DEP_ACCT_EVT_ACCOUNT') THEN
        EXECUTE IMMEDIATE
            'CREATE INDEX DPS2.IX_DEP_ACCT_EVT_ACCOUNT ON DPS2.DEPOSIT_ACCOUNT_LIFECYCLE_EVENT (ACCOUNT_ID, EVENT_AT)';
        DBMS_OUTPUT.PUT_LINE('ADD : IX_DEP_ACCT_EVT_ACCOUNT');
    END IF;

    IF NOT index_exists('DPS2','IX_DEP_ACCT_EVT_OPEN_REQ') THEN
        EXECUTE IMMEDIATE
            'CREATE INDEX DPS2.IX_DEP_ACCT_EVT_OPEN_REQ ON DPS2.DEPOSIT_ACCOUNT_LIFECYCLE_EVENT (OPENING_REQUEST_ID, EVENT_AT)';
        DBMS_OUTPUT.PUT_LINE('ADD : IX_DEP_ACCT_EVT_OPEN_REQ');
    END IF;

    -- 6) Append-only history protection.
    EXECUTE IMMEDIATE q'~
        CREATE OR REPLACE TRIGGER DPS2.TRG_DEP_ACCT_EVT_APPEND_ONLY
        BEFORE UPDATE OR DELETE ON DPS2.DEPOSIT_ACCOUNT_LIFECYCLE_EVENT
        BEGIN
            RAISE_APPLICATION_ERROR(-20091, 'DEPOSIT_ACCOUNT_LIFECYCLE_EVENT is append-only');
        END;
    ~';
    DBMS_OUTPUT.PUT_LINE('OK  : TRG_DEP_ACCT_EVT_APPEND_ONLY enabled/compiled.');

    -- 7) Documentation comments.
    EXECUTE IMMEDIATE q'~
        COMMENT ON TABLE DPS2.DEPOSIT_ACCOUNT IS
        'Phase 4/9 prototype Account Operations contract for Deposit Account lifecycle'
    ~';
    EXECUTE IMMEDIATE q'~
        COMMENT ON COLUMN DPS2.DEPOSIT_ACCOUNT.OPENING_REQUEST_ID IS
        'Integration key to Deposit Opening; intentionally no physical cross-domain FK'
    ~';
    EXECUTE IMMEDIATE q'~
        COMMENT ON COLUMN DPS2.DEPOSIT_OPENING_REQUEST.CREATED_ACCOUNT_ID IS
        'Integration reference to Account Operations; intentionally no physical cross-domain FK'
    ~';
    EXECUTE IMMEDIATE q'~
        COMMENT ON TABLE DPS2.DEPOSIT_ACCOUNT_LIFECYCLE_EVENT IS
        'Append-only Account Operations lifecycle history for CREATE, ACTIVATE and CLOSE'
    ~';

    DBMS_OUTPUT.PUT_LINE('DONE: DPS2 account lifecycle prerequisites reconciled successfully.');
END;
/

PROMPT ============================================================
PROMPT Verification
PROMPT ============================================================

SELECT OWNER, TABLE_NAME
  FROM ALL_TABLES
 WHERE OWNER = 'DPS2'
   AND TABLE_NAME IN (
       'DEPOSIT_OPENING_REQUEST',
       'DEPOSIT_ACCOUNT',
       'DEPOSIT_ACCOUNT_LIFECYCLE_EVENT'
   )
 ORDER BY TABLE_NAME;

SELECT SEQUENCE_OWNER, SEQUENCE_NAME
  FROM ALL_SEQUENCES
 WHERE SEQUENCE_OWNER = 'DPS2'
   AND SEQUENCE_NAME IN ('SEQ_DEPOSIT_ACCOUNT','SEQ_DEP_ACCOUNT_LIFECYCLE_EVT')
 ORDER BY SEQUENCE_NAME;

SELECT OWNER, TABLE_NAME, CONSTRAINT_NAME, CONSTRAINT_TYPE, STATUS
  FROM ALL_CONSTRAINTS
 WHERE OWNER = 'DPS2'
   AND CONSTRAINT_NAME IN (
       'PK_DEPOSIT_ACCOUNT',
       'UK_DEPOSIT_ACCOUNT_NO',
       'UK_DEPOSIT_ACCOUNT_OPEN_REQ',
       'CK_DEP_ACCOUNT_STATUS',
       'PK_DEP_ACCOUNT_LIFECYCLE_EVT',
       'FK_DEP_ACCT_EVT_ACCOUNT',
       'CK_DEP_ACCT_EVT_TYPE'
   )
 ORDER BY TABLE_NAME, CONSTRAINT_NAME;

SELECT OWNER, TRIGGER_NAME, STATUS
  FROM ALL_TRIGGERS
 WHERE OWNER = 'DPS2'
   AND TRIGGER_NAME = 'TRG_DEP_ACCT_EVT_APPEND_ONLY';

SELECT OWNER, TABLE_NAME, COLUMN_NAME, DATA_TYPE, NULLABLE
  FROM ALL_TAB_COLUMNS
 WHERE OWNER = 'DPS2'
   AND TABLE_NAME IN ('DEPOSIT_ACCOUNT','DEPOSIT_ACCOUNT_LIFECYCLE_EVENT')
 ORDER BY TABLE_NAME, COLUMN_ID;

PROMPT ============================================================
PROMPT SUCCESS: Phase 4 account prerequisites + Phase 9 closure support reconciled.
PROMPT ============================================================
