-- ============================================================================
-- Core Banking Prototype 0.11.0 / PDL R10U-HF5
-- Governed default-currency business-code reconciliation.
--
-- Why:
--   DPS.REF_DEFAULT_CURRENCY_CODE.CODE is the business code consumed by PDL and
--   deposit runtime contracts.  Numeric placeholder codes 1/2/3/4 conflict with
--   the canonical ISO-style business values IRR/USD/EUR/AED.
--
-- Scope:
--   * No DDL.
--   * Reconciles the four known current currency rows without changing surrogate IDs.
--   * Reconciles legacy numeric PDL.PRODUCT.DEFAULT_CURRENCY_CODE values if present.
--   * Idempotent: rerunning after successful reconciliation is a no-op.
-- ============================================================================
SET DEFINE OFF;
SET SERVEROUTPUT ON;

PROMPT [PDL R10U-HF5] Validating default currency reference rows ...
DECLARE
  l_count NUMBER;
  l_bad   NUMBER;
  l_rows  NUMBER := 0;

  PROCEDURE require_currency(p_old_code VARCHAR2, p_new_code VARCHAR2, p_name_fa VARCHAR2) IS
    l_ok NUMBER;
  BEGIN
    SELECT COUNT(*) INTO l_ok
      FROM DPS.REF_DEFAULT_CURRENCY_CODE
     WHERE IS_CURRENT = 1
       AND IS_ACTIVE = 1
       AND NAME_FA = p_name_fa
       AND UPPER(TRIM(CODE)) IN (UPPER(p_old_code), UPPER(p_new_code));
    IF l_ok = 0 THEN
      RAISE_APPLICATION_ERROR(-20851,
        'PDL R10U-HF5: expected active/current currency row not found for ' || p_name_fa ||
        ' (' || p_old_code || ' -> ' || p_new_code || ')');
    END IF;
  END;
BEGIN
  SELECT COUNT(*) INTO l_count
    FROM ALL_TABLES
   WHERE OWNER = 'DPS' AND TABLE_NAME = 'REF_DEFAULT_CURRENCY_CODE';
  IF l_count = 0 THEN
    RAISE_APPLICATION_ERROR(-20850, 'PDL R10U-HF5: DPS.REF_DEFAULT_CURRENCY_CODE does not exist');
  END IF;

  require_currency('1', 'IRR', 'ریال ایران');
  require_currency('2', 'USD', 'دلار آمریکا');
  require_currency('3', 'EUR', 'یورو');
  require_currency('4', 'AED', 'درهم امارات متحده عربی');

  SELECT COUNT(*) INTO l_bad
    FROM DPS.REF_DEFAULT_CURRENCY_CODE numeric_row
   WHERE UPPER(TRIM(numeric_row.CODE)) IN ('1','2','3','4')
     AND EXISTS (
       SELECT 1
         FROM DPS.REF_DEFAULT_CURRENCY_CODE iso_row
        WHERE iso_row.VERSION_NO = numeric_row.VERSION_NO
          AND UPPER(TRIM(iso_row.CODE)) = CASE UPPER(TRIM(numeric_row.CODE))
            WHEN '1' THEN 'IRR' WHEN '2' THEN 'USD' WHEN '3' THEN 'EUR' WHEN '4' THEN 'AED' END
          AND iso_row.DEFAULT_CURRENCY_ID <> numeric_row.DEFAULT_CURRENCY_ID
     );
  IF l_bad > 0 THEN
    RAISE_APPLICATION_ERROR(-20852,
      'PDL R10U-HF5: conflicting ISO currency code rows already exist for a numeric reference version');
  END IF;

  UPDATE DPS.REF_DEFAULT_CURRENCY_CODE
     SET CODE = CASE UPPER(TRIM(CODE))
       WHEN '1' THEN 'IRR'
       WHEN '2' THEN 'USD'
       WHEN '3' THEN 'EUR'
       WHEN '4' THEN 'AED'
       ELSE CODE
     END
   WHERE (UPPER(TRIM(CODE)) = '1' AND NAME_FA = 'ریال ایران')
      OR (UPPER(TRIM(CODE)) = '2' AND NAME_FA = 'دلار آمریکا')
      OR (UPPER(TRIM(CODE)) = '3' AND NAME_FA = 'یورو')
      OR (UPPER(TRIM(CODE)) = '4' AND NAME_FA = 'درهم امارات متحده عربی');
  l_rows := SQL%ROWCOUNT;

  SELECT COUNT(*) INTO l_count
    FROM ALL_TAB_COLUMNS
   WHERE OWNER='PDL' AND TABLE_NAME='PRODUCT' AND COLUMN_NAME='DEFAULT_CURRENCY_CODE';
  IF l_count = 1 THEN
    EXECUTE IMMEDIATE q'~
      UPDATE PDL.PRODUCT
         SET DEFAULT_CURRENCY_CODE = CASE UPPER(TRIM(DEFAULT_CURRENCY_CODE))
           WHEN '1' THEN 'IRR' WHEN '2' THEN 'USD' WHEN '3' THEN 'EUR' WHEN '4' THEN 'AED'
           ELSE DEFAULT_CURRENCY_CODE END
       WHERE UPPER(TRIM(DEFAULT_CURRENCY_CODE)) IN ('1','2','3','4')~';
    DBMS_OUTPUT.PUT_LINE('[PDL R10U-HF5] PDL.PRODUCT legacy currency rows reconciled: ' || SQL%ROWCOUNT);
  END IF;

  SELECT COUNT(*) INTO l_count
    FROM DPS.REF_DEFAULT_CURRENCY_CODE
   WHERE IS_CURRENT = 1
     AND IS_ACTIVE = 1
     AND UPPER(TRIM(CODE)) IN ('IRR','USD','EUR','AED');
  IF l_count < 4 THEN
    RAISE_APPLICATION_ERROR(-20853,
      'PDL R10U-HF5: active/current ISO currency set is incomplete after reconciliation; count=' || l_count);
  END IF;

  DBMS_OUTPUT.PUT_LINE('[PDL R10U-HF5] DPS reference rows reconciled: ' || l_rows);
  DBMS_OUTPUT.PUT_LINE('PDL_R10U_HF5_CURRENCY_REFERENCE_PASS');
END;
/

COMMIT;

PROMPT [PDL R10U-HF5] Current governed currency codes:
SELECT CODE, NAME_FA, IS_ACTIVE, IS_CURRENT, VERSION_NO
  FROM DPS.REF_DEFAULT_CURRENCY_CODE
 WHERE IS_CURRENT = 1
 ORDER BY CODE;

PROMPT [PDL R10U-HF5] Done.
