-- Core Banking Prototype 0.3.7
-- Recovery installer based on verification result:
-- Missing phases: 2, 3, 4, 6
-- Existing phases: 1, 5
--
-- Place/copy this file under:
-- database/oracle/cif/reference-data/
-- and run with F5 / Run Script in SQL Developer.

SET DEFINE OFF
SET SERVEROUTPUT ON
SET FEEDBACK ON
SET ECHO ON

WHENEVER SQLERROR EXIT SQL.SQLCODE

PROMPT ============================================================
PROMPT CIF REFERENCE DATA RECOVERY - 0.3.7
PROMPT Missing phases: 2, 3, 4, 6
PROMPT ============================================================

PROMPT
PROMPT [1/4] Phase 2 - Compliance / Risk / KYC
@@compliance-risk/install.sql

PROMPT
PROMPT [2/4] Phase 3 - Contact / Geography
@@contact-geography/install.sql

PROMPT
PROMPT [3/4] Phase 4 - Organization / Product
@@organization-product/install.sql

PROMPT
PROMPT [4/4] Phase 6 - Analytics / Recommendation
@@analytics-recommendation/install.sql

PROMPT
PROMPT ============================================================
PROMPT RECOVERY INSTALL COMPLETED
PROMPT Re-run verify-cif-reference-data-0.3.7-v2.sql
PROMPT Expected final result: PASS
PROMPT ============================================================
