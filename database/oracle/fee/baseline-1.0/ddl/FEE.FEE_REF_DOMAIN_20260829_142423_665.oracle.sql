PROMPT ==============================================================
PROMPT SchemaForge Validation Findings
PROMPT ==============================================================
PROMPT [WARNING] SCHEMA_NOT_FOUND [tables.FEE_REF_DOMAIN]: Schema FEE does not exist in database metadata.
PROMPT [WARNING] TABLE_NAME_NOT_PLURAL [tables.FEE_REF_DOMAIN]: Table name FEE_REF_DOMAIN appears to be singular. Table names should be plural.
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

CREATE SEQUENCE FEE.SEQ_FEE_REF_DOMAIN START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE NOORDER;

-- Persian table name: دامنه اطلاعات پایه کارمزد
-- تعریف دامنه‌های کد و مقادیر کنترلی مورد استفاده در Prototype مستقل کارمزد.
CREATE TABLE FEE.FEE_REF_DOMAIN -- W:SCHEMA|TABLE-PLURAL
(
  /*   0*/  DOMAIN_ID NUMBER(19,0) DEFAULT FEE.SEQ_FEE_REF_DOMAIN.NEXTVAL NOT NULL,
  /*   0*/  DOMAIN_CODE VARCHAR2(50 CHAR) NOT NULL,
  /* 163*/  NAME_FA VARCHAR2(200 CHAR) NOT NULL,
  /* 163*/  NAME_EN VARCHAR2(200 CHAR),
  /* 238*/  DESCRIPTION VARCHAR2(1000 CHAR),
  /*   0*/  VALUE_TYPE_CODE VARCHAR2(30 CHAR) DEFAULT 'TEXT' NOT NULL,
  /*   5*/  DISPLAY_ORDER NUMBER(10,0),
  /* 181*/  IS_ACTIVE CHAR(1 CHAR) DEFAULT 'Y' NOT NULL,
  /* 129*/  CREATED_AT TIMESTAMP(6) DEFAULT SYSTIMESTAMP NOT NULL,
  /* 2858*/  CREATED_BY VARCHAR2(100 CHAR) DEFAULT 'SYSTEM' NOT NULL,
  /* 122*/  UPDATED_AT TIMESTAMP(6),
  /* 118*/  UPDATED_BY VARCHAR2(100 CHAR),
  /* 273*/  RECORD_VERSION NUMBER(10,0) DEFAULT 1 NOT NULL,
CONSTRAINT PK_FEE_REF_DOMAIN PRIMARY KEY (DOMAIN_ID)
USING INDEX (CREATE UNIQUE INDEX FEE.PK_FEE_REF_DOMAIN_DOMAIN_ID ON FEE.FEE_REF_DOMAIN(DOMAIN_ID)
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

ALTER TABLE FEE.FEE_REF_DOMAIN ADD CONSTRAINT CK_FEE_REF_DOMAIN_ACTIVE CHECK(IS_ACTIVE IN ('Y','N')) ENABLE;

ALTER TABLE FEE.FEE_REF_DOMAIN ADD CONSTRAINT UK_FEE_REF_DOMAIN_CODE UNIQUE(DOMAIN_CODE)
 USING INDEX (CREATE UNIQUE INDEX FEE.UK_FEE_REF_DOMAIN_CODE ON FEE.FEE_REF_DOMAIN(DOMAIN_CODE)
/*
-- ORACLE INDEX PHYSICAL OPTIONS
PCTFREE 10
INITRANS 2
NOCOMPRESS
-- LOGGING/NOLOGGING intentionally unspecified: Oracle index logging is independent of the base table.
NOPARALLEL
*/ TABLESPACE ITS_FEE);

COMMENT ON TABLE FEE.FEE_REF_DOMAIN IS 'دامنه اطلاعات پایه کارمزد';

COMMENT ON COLUMN FEE.FEE_REF_DOMAIN.DOMAIN_ID IS 'شناسه دامنه - ستون فیزیکی Oracle DOMAIN_ID.';

COMMENT ON COLUMN FEE.FEE_REF_DOMAIN.DOMAIN_CODE IS 'کد دامنه - ستون فیزیکی Oracle DOMAIN_CODE.';

COMMENT ON COLUMN FEE.FEE_REF_DOMAIN.NAME_FA IS 'نام فارسی دامنه - ستون فیزیکی Oracle NAME_FA.';

COMMENT ON COLUMN FEE.FEE_REF_DOMAIN.NAME_EN IS 'نام انگلیسی دامنه - ستون فیزیکی Oracle NAME_EN.';

COMMENT ON COLUMN FEE.FEE_REF_DOMAIN.DESCRIPTION IS 'شرح دامنه - ستون فیزیکی Oracle DESCRIPTION.';

COMMENT ON COLUMN FEE.FEE_REF_DOMAIN.VALUE_TYPE_CODE IS 'نوع مقدار دامنه - ستون فیزیکی Oracle VALUE_TYPE_CODE.';

COMMENT ON COLUMN FEE.FEE_REF_DOMAIN.DISPLAY_ORDER IS 'ترتیب نمایش - ستون فیزیکی Oracle DISPLAY_ORDER.';

COMMENT ON COLUMN FEE.FEE_REF_DOMAIN.IS_ACTIVE IS 'فعال است - ستون فیزیکی Oracle IS_ACTIVE.';

COMMENT ON COLUMN FEE.FEE_REF_DOMAIN.CREATED_AT IS 'زمان ایجاد - ستون فیزیکی Oracle CREATED_AT.';

COMMENT ON COLUMN FEE.FEE_REF_DOMAIN.CREATED_BY IS 'ایجادکننده - ستون فیزیکی Oracle CREATED_BY.';

COMMENT ON COLUMN FEE.FEE_REF_DOMAIN.UPDATED_AT IS 'زمان آخرین تغییر - ستون فیزیکی Oracle UPDATED_AT.';

COMMENT ON COLUMN FEE.FEE_REF_DOMAIN.UPDATED_BY IS 'آخرین تغییر دهنده - ستون فیزیکی Oracle UPDATED_BY.';

COMMENT ON COLUMN FEE.FEE_REF_DOMAIN.RECORD_VERSION IS 'نسخه رکورد - ستون فیزیکی Oracle RECORD_VERSION.';

/*
SchemaForge Object Summary
Schemas      : 1
Sequences    : 1
Tables       : 1
Columns      : 13
Primary Keys : 1
Unique Keys  : 1
Checks       : 1
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