-- Core Banking Prototype 0.4.0
-- Phase 4 - Deposit Account Creation & Activation
-- Target schema: DPS2
--
-- Architecture boundary:
--   DEPOSIT_OPENING_REQUEST belongs to Deposit Opening / Origination.
--   DEPOSIT_ACCOUNT and DEPOSIT_ACCOUNT_LIFECYCLE_EVENT represent the prototype
--   Account Operations contract. CREATED_ACCOUNT_ID is an integration reference.
--   CREATED_ACCOUNT_ID is intentionally no physical cross-domain FK; no FK is created from DEPOSIT_OPENING_REQUEST to DEPOSIT_ACCOUNT.
--
-- ACCOUNT_NO values beginning with DPA are TECHNICAL PROTOTYPE NUMBERS only.
-- They are not Bank Mellat production account numbers and must not be used as a
-- production numbering algorithm.

SET DEFINE OFF;

DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_count
      FROM ALL_TAB_COLUMNS
     WHERE OWNER = 'DPS2'
       AND TABLE_NAME = 'DEPOSIT_OPENING_REQUEST'
       AND COLUMN_NAME = 'CREATED_ACCOUNT_ID';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE DPS2.DEPOSIT_OPENING_REQUEST ADD (CREATED_ACCOUNT_ID NUMBER(19))';
    END IF;
END;
/

DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_count
      FROM ALL_SEQUENCES
     WHERE SEQUENCE_OWNER = 'DPS2'
       AND SEQUENCE_NAME = 'SEQ_DEPOSIT_ACCOUNT';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'CREATE SEQUENCE DPS2.SEQ_DEPOSIT_ACCOUNT START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE';
    END IF;
END;
/

DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_count
      FROM ALL_SEQUENCES
     WHERE SEQUENCE_OWNER = 'DPS2'
       AND SEQUENCE_NAME = 'SEQ_DEP_ACCOUNT_LIFECYCLE_EVT';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'CREATE SEQUENCE DPS2.SEQ_DEP_ACCOUNT_LIFECYCLE_EVT START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE';
    END IF;
END;
/

DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_count
      FROM ALL_TABLES
     WHERE OWNER = 'DPS2'
       AND TABLE_NAME = 'DEPOSIT_ACCOUNT';
    IF v_count = 0 THEN
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
                CONSTRAINT CK_DEP_ACCOUNT_STATUS CHECK (ACCOUNT_STATUS_CODE IN ('PENDING_ACTIVATION','ACTIVE','CLOSED'))
            )
        ~';
    END IF;
END;
/

DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_count
      FROM ALL_TABLES
     WHERE OWNER = 'DPS2'
       AND TABLE_NAME = 'DEPOSIT_ACCOUNT_LIFECYCLE_EVENT';
    IF v_count = 0 THEN
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
                CONSTRAINT PK_DEP_ACCOUNT_LIFECYCLE_EVT PRIMARY KEY (LIFECYCLE_EVENT_ID),
                CONSTRAINT FK_DEP_ACCT_EVT_ACCOUNT FOREIGN KEY (ACCOUNT_ID)
                    REFERENCES DPS2.DEPOSIT_ACCOUNT (ACCOUNT_ID),
                CONSTRAINT CK_DEP_ACCT_EVT_TYPE CHECK (EVENT_TYPE_CODE IN ('CREATE','ACTIVATE'))
            )
        ~';
    END IF;
END;
/

DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_count
      FROM ALL_INDEXES
     WHERE OWNER = 'DPS2'
       AND INDEX_NAME = 'IX_DEP_ACCT_EVT_ACCOUNT';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'CREATE INDEX DPS2.IX_DEP_ACCT_EVT_ACCOUNT ON DPS2.DEPOSIT_ACCOUNT_LIFECYCLE_EVENT (ACCOUNT_ID, EVENT_AT)';
    END IF;
END;
/

DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_count
      FROM ALL_INDEXES
     WHERE OWNER = 'DPS2'
       AND INDEX_NAME = 'IX_DEP_ACCT_EVT_OPEN_REQ';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'CREATE INDEX DPS2.IX_DEP_ACCT_EVT_OPEN_REQ ON DPS2.DEPOSIT_ACCOUNT_LIFECYCLE_EVENT (OPENING_REQUEST_ID, EVENT_AT)';
    END IF;
END;
/

COMMENT ON TABLE DPS2.DEPOSIT_ACCOUNT IS
'Phase 4 prototype account contract. Account Operations bounded context; DPA account number is technical prototype only';
COMMENT ON COLUMN DPS2.DEPOSIT_ACCOUNT.OPENING_REQUEST_ID IS
'Integration key to Deposit Opening; intentionally no physical cross-domain FK';
COMMENT ON COLUMN DPS2.DEPOSIT_OPENING_REQUEST.CREATED_ACCOUNT_ID IS
'Integration reference to account created after approved opening; intentionally no physical cross-domain FK';
COMMENT ON TABLE DPS2.DEPOSIT_ACCOUNT_LIFECYCLE_EVENT IS
'Immutable prototype lifecycle events for CREATE and ACTIVATE operations';

COMMIT;
