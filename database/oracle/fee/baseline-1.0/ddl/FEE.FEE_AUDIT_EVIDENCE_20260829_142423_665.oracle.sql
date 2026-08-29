PROMPT ==============================================================
PROMPT SchemaForge Validation Findings
PROMPT ==============================================================
PROMPT [WARNING] SCHEMA_NOT_FOUND [tables.FEE_AUDIT_EVIDENCE]: Schema FEE does not exist in database metadata.
PROMPT [WARNING] TABLE_NAME_NOT_PLURAL [tables.FEE_AUDIT_EVIDENCE]: Table name FEE_AUDIT_EVIDENCE appears to be singular. Table names should be plural.
PROMPT [WARNING] METADATA_DATATYPE_MISMATCH [tables.FEE_AUDIT_EVIDENCE.columns.ENTITY_ID]: Document type VARCHAR2(200 CHAR) differs from database metadata for ENTITY_ID. Metadata frequencies: VARCHAR2(100) [1]. Total occurrences: 1.
PROMPT [WARNING] METADATA_DATATYPE_MISMATCH [tables.FEE_AUDIT_EVIDENCE.columns.ENTITY_TYPE_CODE]: Document type VARCHAR2(50 CHAR) differs from database metadata for ENTITY_TYPE_CODE. Metadata frequencies: VARCHAR2(128) [1]. Total occurrences: 1.
PROMPT [WARNING] METADATA_DATATYPE_MISMATCH [tables.FEE_AUDIT_EVIDENCE.columns.EVIDENCE_REF]: Document type VARCHAR2(500 CHAR) differs from database metadata for EVIDENCE_REF. Metadata frequencies: VARCHAR2(200) [2]. Total occurrences: 2.
PROMPT [WARNING] FK_TABLE_NOT_FOUND [tables.FEE_AUDIT_EVIDENCE.foreignKeys.FK_FEE_AUDIT_OVERRIDE]: Referenced table FEE_OVERRIDE_REQUEST does not exist in database metadata.
PROMPT [WARNING] TABLE_NAME_NOT_PLURAL [tables.FEE_AUDIT_EVIDENCE.foreignKeys.FK_FEE_AUDIT_OVERRIDE]: Referenced table FEE_OVERRIDE_REQUEST appears to be singular. Table names should be plural.
PROMPT [WARNING] FK_TABLE_NOT_FOUND [tables.FEE_AUDIT_EVIDENCE.foreignKeys.FK_FEE_AUDIT_REV]: Referenced table FEE_REVERSAL does not exist in database metadata.
PROMPT [WARNING] TABLE_NAME_NOT_PLURAL [tables.FEE_AUDIT_EVIDENCE.foreignKeys.FK_FEE_AUDIT_REV]: Referenced table FEE_REVERSAL appears to be singular. Table names should be plural.
PROMPT [WARNING] FK_TABLE_NOT_FOUND [tables.FEE_AUDIT_EVIDENCE.foreignKeys.FK_FEE_AUDIT_TX]: Referenced table FEE_TRANSACTION does not exist in database metadata.
PROMPT [WARNING] TABLE_NAME_NOT_PLURAL [tables.FEE_AUDIT_EVIDENCE.foreignKeys.FK_FEE_AUDIT_TX]: Referenced table FEE_TRANSACTION appears to be singular. Table names should be plural.
PROMPT ==============================================================

PROMPT ==============================================================
PROMPT SchemaForge Offline Oracle DDL
PROMPT Source File : FEE-Target-DataModel-Baseline-1.0-EA-Oracle.xml
PROMPT Schema      : FEE
PROMPT ==============================================================
SET DEFINE OFF;
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK;

PROMPT [INFRASTRUCTURE TEMPLATE][ORACLE] DBA review required; values are placeholders.
-- Optional permanent tablespace for application data:
-- CREATE TABLESPACE TS_FEE
--   DATAFILE '<DATAFILE_PATH>/ts_fee_01.dbf'
--   SIZE <INITIAL_SIZE> AUTOEXTEND ON NEXT <NEXT_SIZE> MAXSIZE <MAX_SIZE>
--   EXTENT MANAGEMENT LOCAL SEGMENT SPACE MANAGEMENT AUTO;
-- Optional dedicated index tablespace:
-- CREATE TABLESPACE ITS_FEE
--   DATAFILE '<DATAFILE_PATH>/its_fee_01.dbf'
--   SIZE <INITIAL_SIZE> AUTOEXTEND ON NEXT <NEXT_SIZE> MAXSIZE <MAX_SIZE>
--   EXTENT MANAGEMENT LOCAL SEGMENT SPACE MANAGEMENT AUTO;

PROMPT [SCHEMA BOOTSTRAP] Oracle schema FEE is created by CREATE USER and must be provisioned by a DBA.
-- Secure provisioning template; intentionally not executed by SchemaForge:
-- CREATE USER FEE IDENTIFIED BY "<SECURE_PASSWORD>"
--   DEFAULT TABLESPACE TS_FEE TEMPORARY TABLESPACE TEMP
--   QUOTA UNLIMITED ON TS_FEE
--   QUOTA UNLIMITED ON ITS_FEE;
-- GRANT CREATE SESSION, CREATE TABLE, CREATE SEQUENCE, CREATE VIEW,
--       CREATE PROCEDURE, CREATE TRIGGER TO FEE;

CREATE SEQUENCE FEE.SEQ_FEE_AUDIT_EVIDENCE START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE NOORDER;

-- Persian table name: شاهد و مدرک ممیزی کارمزد
-- نگهداری ارجاع/هش/پیامک Evidence برای ورودی، Rule، Approval، Posting، Override یا Reversal.
CREATE TABLE FEE.FEE_AUDIT_EVIDENCE -- W:SCHEMA|TABLE-PLURAL
(
  /*   0*/  AUDIT_EVIDENCE_ID NUMBER(19,0) DEFAULT FEE.SEQ_FEE_AUDIT_EVIDENCE.NEXTVAL NOT NULL,
  /*   0*/  FEE_TRANSACTION_ID NUMBER(19,0),
  /*   0*/  OVERRIDE_REQUEST_ID NUMBER(19,0),
  /*   0*/  FEE_REVERSAL_ID NUMBER(19,0),
  /*   0*/  EVIDENCE_TYPE_CODE VARCHAR2(50 CHAR) NOT NULL,
  /*   1*/  ENTITY_TYPE_CODE VARCHAR2(50 CHAR), -- W:TYPE
  /*   1*/  ENTITY_ID VARCHAR2(200 CHAR), -- W:TYPE
  /*   2*/  EVIDENCE_REF VARCHAR2(500 CHAR), -- W:TYPE
  /*   0*/  EVIDENCE_HASH VARCHAR2(128 CHAR),
  /*   0*/  EVIDENCE_PAYLOAD CLOB,
  /*   0*/  CAPTURED_AT TIMESTAMP(6) NOT NULL,
  /*   1*/  CAPTURED_BY VARCHAR2(100 CHAR) NOT NULL,
CONSTRAINT PK_FEE_AUDIT_EVIDENCE PRIMARY KEY (AUDIT_EVIDENCE_ID)
USING INDEX (CREATE UNIQUE INDEX FEE.PK_FEE_AUDIT_EVIDENCE_AUDIT_EVIDENCE_ID ON FEE.FEE_AUDIT_EVIDENCE(AUDIT_EVIDENCE_ID)
/*
-- ORACLE INDEX PHYSICAL OPTIONS
PCTFREE 10
INITRANS 2
NOCOMPRESS
-- LOGGING/NOLOGGING intentionally unspecified: Oracle index logging is independent of the base table.
NOPARALLEL
*/ TABLESPACE ITS_FEE)
)
/*
-- ORACLE TABLE PHYSICAL OPTIONS
PCTFREE 10
INITRANS 1
-- PCTUSED intentionally omitted: with ASSM it is ignored; review only for MSSM tablespaces.
NOCOMPRESS
-- LOGGING/NOLOGGING intentionally unspecified: redo/recovery policy must come from source/profile.
NOPARALLEL
-- [SOURCE PHYSICAL REVIEW][ORACLE] SEGMENT CREATION is left unspecified so the database/session DEFERRED_SEGMENT_CREATION policy is not overridden.
SEGMENT CREATION <DEFERRED_OR_IMMEDIATE>
*/ TABLESPACE TS_FEE;

CREATE INDEX FEE.IX_FEE_AUDIT_ENTITY ON FEE.FEE_AUDIT_EVIDENCE(ENTITY_TYPE_CODE,ENTITY_ID)
/*
-- ORACLE INDEX PHYSICAL OPTIONS
PCTFREE 10
INITRANS 2
NOCOMPRESS
-- LOGGING/NOLOGGING intentionally unspecified: Oracle index logging is independent of the base table.
NOPARALLEL
*/ TABLESPACE ITS_FEE;

CREATE INDEX FEE.IX_FEE_AUDIT_EVIDENCE_FEE_TRANSACTION_ID ON FEE.FEE_AUDIT_EVIDENCE(FEE_TRANSACTION_ID)
/*
-- ORACLE INDEX PHYSICAL OPTIONS
PCTFREE 10
INITRANS 2
NOCOMPRESS
-- LOGGING/NOLOGGING intentionally unspecified: Oracle index logging is independent of the base table.
NOPARALLEL
*/ TABLESPACE ITS_FEE;

CREATE INDEX FEE.IX_FEE_AUDIT_EVIDENCE_OVERRIDE_REQUEST_ID ON FEE.FEE_AUDIT_EVIDENCE(OVERRIDE_REQUEST_ID)
/*
-- ORACLE INDEX PHYSICAL OPTIONS
PCTFREE 10
INITRANS 2
NOCOMPRESS
-- LOGGING/NOLOGGING intentionally unspecified: Oracle index logging is independent of the base table.
NOPARALLEL
*/ TABLESPACE ITS_FEE;

CREATE INDEX FEE.IX_FEE_AUDIT_EVIDENCE_FEE_REVERSAL_ID ON FEE.FEE_AUDIT_EVIDENCE(FEE_REVERSAL_ID)
/*
-- ORACLE INDEX PHYSICAL OPTIONS
PCTFREE 10
INITRANS 2
NOCOMPRESS
-- LOGGING/NOLOGGING intentionally unspecified: Oracle index logging is independent of the base table.
NOPARALLEL
*/ TABLESPACE ITS_FEE;

ALTER TABLE FEE.FEE_AUDIT_EVIDENCE ADD CONSTRAINT FK_FEE_AUDIT_TX FOREIGN KEY (FEE_TRANSACTION_ID) REFERENCES FEE.FEE_TRANSACTION(FEE_TRANSACTION_ID) ENABLE;

ALTER TABLE FEE.FEE_AUDIT_EVIDENCE ADD CONSTRAINT FK_FEE_AUDIT_OVERRIDE FOREIGN KEY (OVERRIDE_REQUEST_ID) REFERENCES FEE.FEE_OVERRIDE_REQUEST(OVERRIDE_REQUEST_ID) ENABLE;

ALTER TABLE FEE.FEE_AUDIT_EVIDENCE ADD CONSTRAINT FK_FEE_AUDIT_REV FOREIGN KEY (FEE_REVERSAL_ID) REFERENCES FEE.FEE_REVERSAL(FEE_REVERSAL_ID) ENABLE;

COMMENT ON TABLE FEE.FEE_AUDIT_EVIDENCE IS 'شاهد و مدرک ممیزی کارمزد';

COMMENT ON COLUMN FEE.FEE_AUDIT_EVIDENCE.AUDIT_EVIDENCE_ID IS 'شناسه شاهد ممیزی - ستون فیزیکی Oracle AUDIT_EVIDENCE_ID.';

COMMENT ON COLUMN FEE.FEE_AUDIT_EVIDENCE.FEE_TRANSACTION_ID IS 'شناسه تراکنش کارمزد - ستون فیزیکی Oracle FEE_TRANSACTION_ID.';

COMMENT ON COLUMN FEE.FEE_AUDIT_EVIDENCE.OVERRIDE_REQUEST_ID IS 'شناسه درخواست Override - ستون فیزیکی Oracle OVERRIDE_REQUEST_ID.';

COMMENT ON COLUMN FEE.FEE_AUDIT_EVIDENCE.FEE_REVERSAL_ID IS 'شناسه برگشت کارمزد - ستون فیزیکی Oracle FEE_REVERSAL_ID.';

COMMENT ON COLUMN FEE.FEE_AUDIT_EVIDENCE.EVIDENCE_TYPE_CODE IS 'نوع شاهد یا مدرک - ستون فیزیکی Oracle EVIDENCE_TYPE_CODE.';

COMMENT ON COLUMN FEE.FEE_AUDIT_EVIDENCE.ENTITY_TYPE_CODE IS 'نوع موجودیت مرتبط - ستون فیزیکی Oracle ENTITY_TYPE_CODE.';

COMMENT ON COLUMN FEE.FEE_AUDIT_EVIDENCE.ENTITY_ID IS 'شناسه موجودیت مرتبط - ستون فیزیکی Oracle ENTITY_ID.';

COMMENT ON COLUMN FEE.FEE_AUDIT_EVIDENCE.EVIDENCE_REF IS 'ارجاع مدرک یا فایل - ستون فیزیکی Oracle EVIDENCE_REF.';

COMMENT ON COLUMN FEE.FEE_AUDIT_EVIDENCE.EVIDENCE_HASH IS 'هش مدرک - ستون فیزیکی Oracle EVIDENCE_HASH.';

COMMENT ON COLUMN FEE.FEE_AUDIT_EVIDENCE.EVIDENCE_PAYLOAD IS 'محتوای مدرک یا Payload - ستون فیزیکی Oracle EVIDENCE_PAYLOAD.';

COMMENT ON COLUMN FEE.FEE_AUDIT_EVIDENCE.CAPTURED_AT IS 'زمان ثبت مدرک - ستون فیزیکی Oracle CAPTURED_AT.';

COMMENT ON COLUMN FEE.FEE_AUDIT_EVIDENCE.CAPTURED_BY IS 'ثبت‌کننده مدرک - ستون فیزیکی Oracle CAPTURED_BY.';

/*
SchemaForge Object Summary
Schemas      : 1
Sequences    : 1
Tables       : 1
Columns      : 12
Primary Keys : 1
Unique Keys  : 0
Checks       : 0
Foreign Keys : 3
Physical FKs : 3
Logical FKs  : 0
Indexes      : 4
*/

/*
Generated By : SchemaForge
Generated On : 2026-08-29 14:25:13 +03:30
Source File  : FEE-Target-DataModel-Baseline-1.0-EA-Oracle.xml
Dialect      : Oracle
*/