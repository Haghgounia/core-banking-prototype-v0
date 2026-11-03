-- ==========================================================================
-- CLT R2C CANDIDATE, NOT PART OF APPROVED BASE XMI (26 TABLES).
-- DBA must approve this migration separately; run on a reviewed CLT schema.
-- PRECONDITION: R2B stage table exists and these columns DO NOT exist yet.
-- No installation script executes this file. NOT a production deployment.
-- This updates ONLY draft storage; it does not create a final collateral.
-- ==========================================================================
SET DEFINE OFF
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

ALTER TABLE CLT.CLT_INTRODUCTION_DRAFT ADD (
  ASSET_REFERENCE VARCHAR2(200 CHAR),
  ITEM_RANGES_JSON CLOB
);
ALTER TABLE CLT.CLT_INTRODUCTION_DRAFT ADD CONSTRAINT CHK_CLT_INTRO_DRAFT_ITEM_RANGES_JSON
  CHECK (ITEM_RANGES_JSON IS JSON) ENABLE;

-- No operational key, registration number, uniqueness claim, or cross-domain
-- foreign key is inferred from ASSET_REFERENCE. The field is a DRAFT candidate.
-- Do not grant the stage writer DML on CLT.COLLATERAL or its live child tables.
PROMPT CLT_R2C_DRAFT_STAGE_EXTENSION_COMPLETED_PENDING_DBA_VALIDATION
