PROMPT =====================================================================
PROMPT Core Banking Prototype 0.3.22-fix14 - financial/employment UI data
PROMPT =====================================================================

-- Existing REF_ISIC_ACTIVITY seed rows used the code itself as NAME_FA/NAME_EN,
-- which caused operational ComboBoxes to display opaque codes to users.
MERGE INTO CIF.REF_ISIC_ACTIVITY T
USING (
  SELECT '6419' ISIC_CODE, 'سایر واسطه‌گری‌های پولی' NAME_FA, 'Other monetary intermediation' NAME_EN FROM DUAL UNION ALL
  SELECT '6201', 'فعالیت‌های برنامه‌نویسی رایانه‌ای', 'Computer programming activities' FROM DUAL UNION ALL
  SELECT '4690', 'عمده‌فروشی غیرتخصصی', 'Non-specialized wholesale trade' FROM DUAL UNION ALL
  SELECT '0111', 'کشت غلات (به‌جز برنج)، حبوبات و دانه‌های روغنی', 'Growing of cereals (except rice), leguminous crops and oil seeds' FROM DUAL
) S
ON (T.ISIC_CODE = S.ISIC_CODE)
WHEN MATCHED THEN UPDATE SET
  T.NAME_FA = S.NAME_FA,
  T.NAME_EN = S.NAME_EN,
  T.IS_ACTIVE = 1
WHEN NOT MATCHED THEN INSERT (
  ISIC_CODE, NAME_FA, NAME_EN, DESCRIPTION_FA, SORT_ORDER,
  IS_ACTIVE, PARENT_CODE, VALID_FROM, VALID_TO, RECORD_VERSION
) VALUES (
  S.ISIC_CODE, S.NAME_FA, S.NAME_EN, NULL,
  CASE S.ISIC_CODE WHEN '6419' THEN 10 WHEN '6201' THEN 20 WHEN '4690' THEN 30 ELSE 40 END,
  1, NULL, NULL, NULL, 1
);

COMMIT;

DECLARE
  V_BAD_COUNT NUMBER;
BEGIN
  SELECT COUNT(*) INTO V_BAD_COUNT
    FROM CIF.REF_ISIC_ACTIVITY
   WHERE ISIC_CODE IN ('6419','6201','4690','0111')
     AND (NAME_FA IS NULL OR NAME_FA = ISIC_CODE);

  IF V_BAD_COUNT <> 0 THEN
    RAISE_APPLICATION_ERROR(-20114, 'REF_ISIC_ACTIVITY Persian-title alignment failed.');
  END IF;
END;
/

PROMPT 0.3.22-fix14 financial/employment UI data completed.
