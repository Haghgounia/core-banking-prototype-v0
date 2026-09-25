-- Core Banking Prototype 0.11.0
-- DPS2 Phase 11N-B - Canonical Coverage Closure (Operational Steps 01-02)
-- Scope: current servicing organization unit + source-aligned reconciliation only.
SET DEFINE OFF;
SET SERVEROUTPUT ON;
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK;
PROMPT ============================================================
PROMPT DPS2 Phase 11N-B - Steps 01-02 Canonical Closure
PROMPT ORG_UNIT_CODE current servicing ownership reconciliation
PROMPT ============================================================
DECLARE
  v_missing NUMBER;
  v_reconciled NUMBER;
  v_nullable VARCHAR2(1);

  FUNCTION col_exists(p_table VARCHAR2,p_col VARCHAR2) RETURN BOOLEAN IS v NUMBER;
  BEGIN
    SELECT COUNT(*) INTO v FROM ALL_TAB_COLUMNS
     WHERE OWNER='DPS2' AND TABLE_NAME=UPPER(p_table) AND COLUMN_NAME=UPPER(p_col);
    RETURN v>0;
  END;
BEGIN
  IF NOT col_exists('DEPOSIT_ACCOUNT','ORG_UNIT_CODE') THEN
    EXECUTE IMMEDIATE 'ALTER TABLE DPS2.DEPOSIT_ACCOUNT ADD (ORG_UNIT_CODE VARCHAR2(30 CHAR))';
    DBMS_OUTPUT.PUT_LINE('ADD    | DPS2.DEPOSIT_ACCOUNT.ORG_UNIT_CODE VARCHAR2(30 CHAR)');
  ELSE
    DBMS_OUTPUT.PUT_LINE('KEEP   | DPS2.DEPOSIT_ACCOUNT.ORG_UNIT_CODE');
  END IF;

  -- Preserve the value that the pre-11N-B servicing UI treated as the mutable unit.
  -- This must happen before restoring OPENING_ORG_UNIT_CODE from the Opening source.
  EXECUTE IMMEDIATE q'[
    UPDATE DPS2.DEPOSIT_ACCOUNT A
       SET A.ORG_UNIT_CODE = COALESCE(
             A.ORG_UNIT_CODE,
             A.OPENING_ORG_UNIT_CODE,
             (SELECT R.ORG_UNIT_CODE FROM DPS2.DEPOSIT_OPENING_REQUEST R
               WHERE R.OPENING_REQUEST_ID=A.OPENING_REQUEST_ID)
           )
     WHERE A.ORG_UNIT_CODE IS NULL
  ]';
  v_reconciled := SQL%ROWCOUNT;
  DBMS_OUTPUT.PUT_LINE('RECON  | current ORG_UNIT_CODE backfilled='||v_reconciled);

  -- OPENING_ORG_UNIT_CODE is the immutable Opening snapshot in the canonical XML.
  UPDATE DPS2.DEPOSIT_ACCOUNT A
     SET A.OPENING_ORG_UNIT_CODE = (
           SELECT R.ORG_UNIT_CODE FROM DPS2.DEPOSIT_OPENING_REQUEST R
            WHERE R.OPENING_REQUEST_ID=A.OPENING_REQUEST_ID
         )
   WHERE EXISTS (
           SELECT 1 FROM DPS2.DEPOSIT_OPENING_REQUEST R
            WHERE R.OPENING_REQUEST_ID=A.OPENING_REQUEST_ID
              AND R.ORG_UNIT_CODE IS NOT NULL
              AND (A.OPENING_ORG_UNIT_CODE IS NULL OR A.OPENING_ORG_UNIT_CODE<>R.ORG_UNIT_CODE)
         );
  DBMS_OUTPUT.PUT_LINE('RECON  | immutable OPENING_ORG_UNIT_CODE restored='||SQL%ROWCOUNT);

  EXECUTE IMMEDIATE 'SELECT COUNT(*) FROM DPS2.DEPOSIT_ACCOUNT WHERE ORG_UNIT_CODE IS NULL' INTO v_missing;
  IF v_missing>0 THEN
    RAISE_APPLICATION_ERROR(-21320,'Cannot enforce canonical ORG_UNIT_CODE: '||v_missing||' account row(s) have no Opening/current org-unit source.');
  END IF;

  SELECT NULLABLE
    INTO v_nullable
    FROM ALL_TAB_COLUMNS
   WHERE OWNER='DPS2'
     AND TABLE_NAME='DEPOSIT_ACCOUNT'
     AND COLUMN_NAME='ORG_UNIT_CODE';

  IF v_nullable='Y' THEN
    EXECUTE IMMEDIATE 'ALTER TABLE DPS2.DEPOSIT_ACCOUNT MODIFY (ORG_UNIT_CODE VARCHAR2(30 CHAR) NOT NULL)';
    DBMS_OUTPUT.PUT_LINE('ENFORCE| DPS2.DEPOSIT_ACCOUNT.ORG_UNIT_CODE NOT NULL');
  ELSE
    DBMS_OUTPUT.PUT_LINE('KEEP   | DPS2.DEPOSIT_ACCOUNT.ORG_UNIT_CODE already NOT NULL');
  END IF;
END;
/
COMMIT;
PROMPT ============================================================
PROMPT PHASE11NB_SCHEMA_RECONCILIATION_PASS
PROMPT ============================================================
