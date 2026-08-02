-- ============================================================================
-- Reference Data Prototype
-- Oracle DDL: FOREIGN_CITIES
-- Source dataset: GEO.FOREIGN_CITIES_data_20220502_14010212_1154.sql
--
-- Source profile:
--   Rows             : 38,379
--   FOREIGN_CITY_ID  : 1 .. 38,379
--   Max code length  : 10 characters
--   Max name length  : 49 characters
--
-- All active reference-data tables use the GEO owner.
--
-- Run as Script (F5) in Oracle SQL Developer, SQLcl, or SQL*Plus.
-- ============================================================================

SET DEFINE ON
SET VERIFY OFF
SET SERVEROUTPUT ON

DEFINE TARGET_SCHEMA = GEO

PROMPT Creating &&TARGET_SCHEMA..FOREIGN_CITIES ...

CREATE TABLE &&TARGET_SCHEMA..FOREIGN_CITIES
(
    FOREIGN_CITY_ID            NUMBER(10)          NOT NULL,
    FOREIGN_CITY_CODE          VARCHAR2(20 CHAR)   NOT NULL,
    FOREIGN_CITY_NAME          VARCHAR2(200 CHAR)  NOT NULL,
    FOREIGN_CITY_ENGLISH_NAME  VARCHAR2(200 CHAR),
    COUNTRY_ID                 NUMBER(10)          NOT NULL,
    IS_CAPITAL                 NUMBER(1)           DEFAULT 0 NOT NULL,
    IS_METROPOLIS              NUMBER(1)           DEFAULT 0 NOT NULL,
    IS_ACTIVE                  NUMBER(1)           DEFAULT 1 NOT NULL,
    SORT_ORDER                 NUMBER(10),
    CREATED_BY                 NUMBER(19)          DEFAULT 1 NOT NULL,
    CREATED_DATE               TIMESTAMP(6)        DEFAULT CURRENT_TIMESTAMP NOT NULL,
    LAST_MODIFIED_BY           NUMBER(19),
    LAST_MODIFIED_DATE         TIMESTAMP(6),

    CONSTRAINT PK_FOREIGN_CITIES
        PRIMARY KEY (FOREIGN_CITY_ID),

    CONSTRAINT UK_FOREIGN_CITIES_CODE
        UNIQUE (FOREIGN_CITY_CODE),

    CONSTRAINT CK_FC_CAPITAL
        CHECK (IS_CAPITAL IN (0, 1)),

    CONSTRAINT CK_FC_METROPOLIS
        CHECK (IS_METROPOLIS IN (0, 1)),

    CONSTRAINT CK_FC_ACTIVE
        CHECK (IS_ACTIVE IN (0, 1)),

    CONSTRAINT FK_FC_COUNTRY
        FOREIGN KEY (COUNTRY_ID)
        REFERENCES &&TARGET_SCHEMA..COUNTRIES (COUNTRY_ID)
);

-- Supports the hierarchical country filter used by the reference-data forms.
CREATE INDEX &&TARGET_SCHEMA..IX_FC_COUNTRY_ACTIVE
    ON &&TARGET_SCHEMA..FOREIGN_CITIES (COUNTRY_ID, IS_ACTIVE);

-- Supports ordering and prefix searches within the selected country.
CREATE INDEX &&TARGET_SCHEMA..IX_FC_COUNTRY_NAME
    ON &&TARGET_SCHEMA..FOREIGN_CITIES (COUNTRY_ID, FOREIGN_CITY_NAME);

CREATE SEQUENCE &&TARGET_SCHEMA..SEQ_FOREIGN_CITIES
    START WITH 38380
    INCREMENT BY 1
    MINVALUE 1
    NOMAXVALUE
    CACHE 100
    NOCYCLE;

COMMENT ON TABLE &&TARGET_SCHEMA..FOREIGN_CITIES IS
    'Reference data for cities outside Iran, organized by country';

COMMENT ON COLUMN &&TARGET_SCHEMA..FOREIGN_CITIES.FOREIGN_CITY_ID IS
    'Surrogate primary key';

COMMENT ON COLUMN &&TARGET_SCHEMA..FOREIGN_CITIES.FOREIGN_CITY_CODE IS
    'Unique business/source code of the foreign city';

COMMENT ON COLUMN &&TARGET_SCHEMA..FOREIGN_CITIES.FOREIGN_CITY_NAME IS
    'Localized display name';

COMMENT ON COLUMN &&TARGET_SCHEMA..FOREIGN_CITIES.FOREIGN_CITY_ENGLISH_NAME IS
    'English display name';

COMMENT ON COLUMN &&TARGET_SCHEMA..FOREIGN_CITIES.COUNTRY_ID IS
    'Parent country identifier';

COMMENT ON COLUMN &&TARGET_SCHEMA..FOREIGN_CITIES.IS_CAPITAL IS
    'Capital indicator: 0 = No, 1 = Yes';

COMMENT ON COLUMN &&TARGET_SCHEMA..FOREIGN_CITIES.IS_METROPOLIS IS
    'Metropolis indicator: 0 = No, 1 = Yes';

COMMENT ON COLUMN &&TARGET_SCHEMA..FOREIGN_CITIES.IS_ACTIVE IS
    'Lifecycle status: 0 = Inactive, 1 = Active';

PROMPT &&TARGET_SCHEMA..FOREIGN_CITIES created successfully.
PROMPT Sequence starts at 38380, immediately after the source maximum ID.
