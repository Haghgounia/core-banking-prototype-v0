PROMPT ==============================================================
PROMPT SchemaForge Validation Findings
PROMPT ==============================================================
PROMPT [WARNING] SCHEMA_NOT_FOUND [tables.FEE_DEMO_FX_RATE]: Schema FEE does not exist in database metadata.
PROMPT [WARNING] TABLE_NAME_NOT_PLURAL [tables.FEE_DEMO_FX_RATE]: Table name FEE_DEMO_FX_RATE appears to be singular. Table names should be plural.
PROMPT [WARNING] METADATA_DATATYPE_MISMATCH [tables.FEE_DEMO_FX_RATE.columns.RATE_VALUE]: Document type NUMBER(24,12) differs from database metadata for RATE_VALUE. Metadata frequencies: NUMBER(12,8) [3]. Total occurrences: 3.
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

CREATE SEQUENCE FEE.SEQ_FEE_DEMO_FX_RATE START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE NOORDER;

-- Persian table name: نرخ ارز آزمایشی کارمزد
-- نرخ تبدیل ارز نمایشی برای Simulator و Fee Currency Modality در Prototype مستقل.
CREATE TABLE FEE.FEE_DEMO_FX_RATE -- W:SCHEMA|TABLE-PLURAL
(
  /*   0*/  DEMO_FX_RATE_ID NUMBER(19,0) DEFAULT FEE.SEQ_FEE_DEMO_FX_RATE.NEXTVAL NOT NULL,
  /*   0*/  FROM_CURRENCY_CODE VARCHAR2(3 CHAR) NOT NULL,
  /*   0*/  TO_CURRENCY_CODE VARCHAR2(3 CHAR) NOT NULL,
  /*   0*/  RATE_TYPE_CODE VARCHAR2(50 CHAR) NOT NULL,
  /*   3*/  RATE_VALUE NUMBER(24,12) NOT NULL, -- W:TYPE
  /*   0*/  QUOTED_AT TIMESTAMP(6) NOT NULL,
  /*   6*/  EFFECTIVE_FROM TIMESTAMP(6),
  /*   6*/  EFFECTIVE_TO TIMESTAMP(6),
  /*   5*/  SOURCE_CODE VARCHAR2(50 CHAR),
  /* 181*/  IS_ACTIVE CHAR(1 CHAR) DEFAULT 'Y' NOT NULL,
  /* 129*/  CREATED_AT TIMESTAMP(6) DEFAULT SYSTIMESTAMP NOT NULL,
  /* 2858*/  CREATED_BY VARCHAR2(100 CHAR) DEFAULT 'SYSTEM' NOT NULL,
CONSTRAINT PK_FEE_DEMO_FX_RATE PRIMARY KEY (DEMO_FX_RATE_ID)
USING INDEX (CREATE UNIQUE INDEX FEE.PK_FEE_DEMO_FX_RATE_DEMO_FX_RATE_ID ON FEE.FEE_DEMO_FX_RATE(DEMO_FX_RATE_ID)
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

ALTER TABLE FEE.FEE_DEMO_FX_RATE ADD CONSTRAINT CK_FEE_DEMO_FX_RATE_POS CHECK(RATE_VALUE > 0) ENABLE;

ALTER TABLE FEE.FEE_DEMO_FX_RATE ADD CONSTRAINT CK_FEE_DEMO_FX_ACTIVE CHECK(IS_ACTIVE IN ('Y','N')) ENABLE;

ALTER TABLE FEE.FEE_DEMO_FX_RATE ADD CONSTRAINT CK_FEE_DEMO_FX_DATES CHECK(EFFECTIVE_TO IS NULL OR EFFECTIVE_FROM IS NULL OR EFFECTIVE_TO >= EFFECTIVE_FROM) ENABLE;

CREATE INDEX FEE.IX_FEE_DEMO_FX_LOOKUP ON FEE.FEE_DEMO_FX_RATE(FROM_CURRENCY_CODE,TO_CURRENCY_CODE,RATE_TYPE_CODE,QUOTED_AT)
/*
-- ORACLE INDEX PHYSICAL OPTIONS
PCTFREE 10
INITRANS 2
NOCOMPRESS
-- LOGGING/NOLOGGING intentionally unspecified: Oracle index logging is independent of the base table.
NOPARALLEL
*/ TABLESPACE ITS_FEE;

COMMENT ON TABLE FEE.FEE_DEMO_FX_RATE IS 'نرخ ارز آزمایشی کارمزد';

COMMENT ON COLUMN FEE.FEE_DEMO_FX_RATE.DEMO_FX_RATE_ID IS 'شناسه نرخ ارز - ستون فیزیکی Oracle DEMO_FX_RATE_ID.';

COMMENT ON COLUMN FEE.FEE_DEMO_FX_RATE.FROM_CURRENCY_CODE IS 'ارز مبدأ - ستون فیزیکی Oracle FROM_CURRENCY_CODE.';

COMMENT ON COLUMN FEE.FEE_DEMO_FX_RATE.TO_CURRENCY_CODE IS 'ارز مقصد - ستون فیزیکی Oracle TO_CURRENCY_CODE.';

COMMENT ON COLUMN FEE.FEE_DEMO_FX_RATE.RATE_TYPE_CODE IS 'نوع نرخ ارز - ستون فیزیکی Oracle RATE_TYPE_CODE.';

COMMENT ON COLUMN FEE.FEE_DEMO_FX_RATE.RATE_VALUE IS 'مقدار نرخ ارز - ستون فیزیکی Oracle RATE_VALUE.';

COMMENT ON COLUMN FEE.FEE_DEMO_FX_RATE.QUOTED_AT IS 'زمان اعلام نرخ - ستون فیزیکی Oracle QUOTED_AT.';

COMMENT ON COLUMN FEE.FEE_DEMO_FX_RATE.EFFECTIVE_FROM IS 'زمان شروع اعتبار نرخ - ستون فیزیکی Oracle EFFECTIVE_FROM.';

COMMENT ON COLUMN FEE.FEE_DEMO_FX_RATE.EFFECTIVE_TO IS 'زمان پایان اعتبار نرخ - ستون فیزیکی Oracle EFFECTIVE_TO.';

COMMENT ON COLUMN FEE.FEE_DEMO_FX_RATE.SOURCE_CODE IS 'کد منبع نرخ - ستون فیزیکی Oracle SOURCE_CODE.';

COMMENT ON COLUMN FEE.FEE_DEMO_FX_RATE.IS_ACTIVE IS 'فعال است - ستون فیزیکی Oracle IS_ACTIVE.';

COMMENT ON COLUMN FEE.FEE_DEMO_FX_RATE.CREATED_AT IS 'زمان ایجاد - ستون فیزیکی Oracle CREATED_AT.';

COMMENT ON COLUMN FEE.FEE_DEMO_FX_RATE.CREATED_BY IS 'ایجادکننده - ستون فیزیکی Oracle CREATED_BY.';

/*
SchemaForge Object Summary
Schemas      : 1
Sequences    : 1
Tables       : 1
Columns      : 12
Primary Keys : 1
Unique Keys  : 0
Checks       : 3
Foreign Keys : 0
Physical FKs : 0
Logical FKs  : 0
Indexes      : 1
*/

/*
Generated By : SchemaForge
Generated On : 2026-08-29 14:25:15 +03:30
Source File  : FEE-Target-DataModel-Baseline-1.0-EA-Oracle.xml
Dialect      : Oracle
*/