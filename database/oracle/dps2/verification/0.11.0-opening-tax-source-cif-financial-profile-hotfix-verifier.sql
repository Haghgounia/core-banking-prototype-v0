SET SERVEROUTPUT ON SIZE UNLIMITED
SET DEFINE OFF
SET VERIFY OFF
SET FEEDBACK ON
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

PROMPT ============================================================
PROMPT Core Banking Prototype 0.11.0
PROMPT DPS2 Opening Tax Source - CIF Financial Profile Verifier
PROMPT Read-only verifier
PROMPT ============================================================

DECLARE
  v_count NUMBER;
  v_title VARCHAR2(200);
BEGIN
  SELECT COUNT(*), MAX(TITLE_EN)
    INTO v_count, v_title
    FROM DPS2.REF_DEP_OPEN_TAX_STATUS_SOURCE
   WHERE TAX_STATUS_SOURCE_CODE='CIF_FINANCIAL_PROFILE'
     AND IS_ACTIVE=1
     AND (VALID_FROM IS NULL OR VALID_FROM<=TRUNC(SYSDATE))
     AND (VALID_TO IS NULL OR VALID_TO>=TRUNC(SYSDATE));

  IF v_count <> 1 THEN
    RAISE_APPLICATION_ERROR(-20482, 'Expected one active/valid CIF_FINANCIAL_PROFILE tax-status source, found ' || v_count);
  END IF;
  DBMS_OUTPUT.PUT_LINE('PASS | CIF_FINANCIAL_PROFILE active/valid | ' || v_title);
  DBMS_OUTPUT.PUT_LINE('DPS2_OPEN_TAX_CIF_SOURCE_VERIFIER_PASS');
END;
/
