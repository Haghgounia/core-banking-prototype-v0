PROMPT ==============================================================
PROMPT SchemaForge Validation Findings
PROMPT ==============================================================
PROMPT [WARNING] SCHEMA_NOT_FOUND [tables.FEE_DEMO_PARTY]: Schema FEE does not exist in database metadata.
PROMPT [WARNING] TABLE_NAME_NOT_PLURAL [tables.FEE_DEMO_PARTY]: Table name FEE_DEMO_PARTY appears to be singular. Table names should be plural.
PROMPT [WARNING] METADATA_DATATYPE_MISMATCH [tables.FEE_DEMO_PARTY.columns.CUSTOMER_SEGMENT_CODE]: Document type VARCHAR2(50 CHAR) differs from database metadata for CUSTOMER_SEGMENT_CODE. Metadata frequencies: VARCHAR2(30) [2]. Total occurrences: 2.
PROMPT [WARNING] METADATA_DATATYPE_MISMATCH [tables.FEE_DEMO_PARTY.columns.NAME_FA]: Document type VARCHAR2(250 CHAR) differs from database metadata for NAME_FA. Metadata frequencies: VARCHAR2(200) [155], VARCHAR2(500) [3], VARCHAR2(100) [2], VARCHAR2(120) [1], VARCHAR2(300) [1], VARCHAR2(50) [1]. Total occurrences: 163.
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

CREATE SEQUENCE FEE.SEQ_FEE_DEMO_PARTY START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE NOORDER;

-- Persian table name: طرف آزمایشی کارمزد
-- داده نمایشی Party/Customer/Organization فقط برای استقلال Prototype؛ جایگزین Party Master واقعی نیست.
CREATE TABLE FEE.FEE_DEMO_PARTY -- W:SCHEMA|TABLE-PLURAL
(
  /*   0*/  DEMO_PARTY_ID NUMBER(19,0) DEFAULT FEE.SEQ_FEE_DEMO_PARTY.NEXTVAL NOT NULL,
  /*   0*/  PARTY_NO VARCHAR2(50 CHAR) NOT NULL,
  /*   5*/  PARTY_TYPE_CODE VARCHAR2(30 CHAR) NOT NULL,
  /* 163*/  NAME_FA VARCHAR2(250 CHAR) NOT NULL, -- W:TYPE
  /* 163*/  NAME_EN VARCHAR2(250 CHAR),
  /*   2*/  CUSTOMER_SEGMENT_CODE VARCHAR2(50 CHAR), -- W:TYPE
  /*   0*/  CUSTOMER_GROUP_CODE VARCHAR2(50 CHAR),
  /*   0*/  KNOWLEDGE_BASED_FLAG CHAR(1 CHAR) DEFAULT 'N' NOT NULL,
  /*   0*/  WELFARE_SUPPORT_FLAG CHAR(1 CHAR) DEFAULT 'N' NOT NULL,
  /*   6*/  COUNTRY_CODE VARCHAR2(3 CHAR),
  /*  16*/  STATUS_CODE VARCHAR2(30 CHAR) DEFAULT 'ACTIVE' NOT NULL,
  /* 129*/  CREATED_AT TIMESTAMP(6) DEFAULT SYSTIMESTAMP NOT NULL,
  /* 2858*/  CREATED_BY VARCHAR2(100 CHAR) DEFAULT 'SYSTEM' NOT NULL,
  /* 122*/  UPDATED_AT TIMESTAMP(6),
  /* 118*/  UPDATED_BY VARCHAR2(100 CHAR),
  /* 273*/  RECORD_VERSION NUMBER(10,0) DEFAULT 1 NOT NULL,
CONSTRAINT PK_FEE_DEMO_PARTY PRIMARY KEY (DEMO_PARTY_ID)
USING INDEX (CREATE UNIQUE INDEX FEE.PK_FEE_DEMO_PARTY_DEMO_PARTY_ID ON FEE.FEE_DEMO_PARTY(DEMO_PARTY_ID)
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

ALTER TABLE FEE.FEE_DEMO_PARTY ADD CONSTRAINT CK_FEE_DEMO_PARTY_KB CHECK(KNOWLEDGE_BASED_FLAG IN ('Y','N')) ENABLE;

ALTER TABLE FEE.FEE_DEMO_PARTY ADD CONSTRAINT CK_FEE_DEMO_PARTY_WELF CHECK(WELFARE_SUPPORT_FLAG IN ('Y','N')) ENABLE;

ALTER TABLE FEE.FEE_DEMO_PARTY ADD CONSTRAINT UK_FEE_DEMO_PARTY_NO UNIQUE(PARTY_NO)
 USING INDEX (CREATE UNIQUE INDEX FEE.UK_FEE_DEMO_PARTY_NO ON FEE.FEE_DEMO_PARTY(PARTY_NO)
/*
-- ORACLE INDEX PHYSICAL OPTIONS
PCTFREE 10
INITRANS 2
NOCOMPRESS
-- LOGGING/NOLOGGING intentionally unspecified: Oracle index logging is independent of the base table.
NOPARALLEL
*/ TABLESPACE ITS_FEE);

COMMENT ON TABLE FEE.FEE_DEMO_PARTY IS 'طرف آزمایشی کارمزد';

COMMENT ON COLUMN FEE.FEE_DEMO_PARTY.DEMO_PARTY_ID IS 'شناسه طرف آزمایشی - ستون فیزیکی Oracle DEMO_PARTY_ID.';

COMMENT ON COLUMN FEE.FEE_DEMO_PARTY.PARTY_NO IS 'شماره طرف - ستون فیزیکی Oracle PARTY_NO.';

COMMENT ON COLUMN FEE.FEE_DEMO_PARTY.PARTY_TYPE_CODE IS 'نوع طرف - ستون فیزیکی Oracle PARTY_TYPE_CODE.';

COMMENT ON COLUMN FEE.FEE_DEMO_PARTY.NAME_FA IS 'نام فارسی طرف - ستون فیزیکی Oracle NAME_FA.';

COMMENT ON COLUMN FEE.FEE_DEMO_PARTY.NAME_EN IS 'نام انگلیسی طرف - ستون فیزیکی Oracle NAME_EN.';

COMMENT ON COLUMN FEE.FEE_DEMO_PARTY.CUSTOMER_SEGMENT_CODE IS 'کد بخش مشتری - ستون فیزیکی Oracle CUSTOMER_SEGMENT_CODE.';

COMMENT ON COLUMN FEE.FEE_DEMO_PARTY.CUSTOMER_GROUP_CODE IS 'کد گروه مشتری - ستون فیزیکی Oracle CUSTOMER_GROUP_CODE.';

COMMENT ON COLUMN FEE.FEE_DEMO_PARTY.KNOWLEDGE_BASED_FLAG IS 'شرکت دانش‌بنیان است - ستون فیزیکی Oracle KNOWLEDGE_BASED_FLAG.';

COMMENT ON COLUMN FEE.FEE_DEMO_PARTY.WELFARE_SUPPORT_FLAG IS 'مشمول گروه حمایتی است - ستون فیزیکی Oracle WELFARE_SUPPORT_FLAG.';

COMMENT ON COLUMN FEE.FEE_DEMO_PARTY.COUNTRY_CODE IS 'کد کشور - ستون فیزیکی Oracle COUNTRY_CODE.';

COMMENT ON COLUMN FEE.FEE_DEMO_PARTY.STATUS_CODE IS 'وضعیت طرف - ستون فیزیکی Oracle STATUS_CODE.';

COMMENT ON COLUMN FEE.FEE_DEMO_PARTY.CREATED_AT IS 'زمان ایجاد - ستون فیزیکی Oracle CREATED_AT.';

COMMENT ON COLUMN FEE.FEE_DEMO_PARTY.CREATED_BY IS 'ایجادکننده - ستون فیزیکی Oracle CREATED_BY.';

COMMENT ON COLUMN FEE.FEE_DEMO_PARTY.UPDATED_AT IS 'زمان آخرین تغییر - ستون فیزیکی Oracle UPDATED_AT.';

COMMENT ON COLUMN FEE.FEE_DEMO_PARTY.UPDATED_BY IS 'آخرین تغییر دهنده - ستون فیزیکی Oracle UPDATED_BY.';

COMMENT ON COLUMN FEE.FEE_DEMO_PARTY.RECORD_VERSION IS 'نسخه رکورد - ستون فیزیکی Oracle RECORD_VERSION.';

/*
SchemaForge Object Summary
Schemas      : 1
Sequences    : 1
Tables       : 1
Columns      : 16
Primary Keys : 1
Unique Keys  : 1
Checks       : 2
Foreign Keys : 0
Physical FKs : 0
Logical FKs  : 0
Indexes      : 0
*/

/*
Generated By : SchemaForge
Generated On : 2026-08-29 14:25:14 +03:30
Source File  : FEE-Target-DataModel-Baseline-1.0-EA-Oracle.xml
Dialect      : Oracle
*/