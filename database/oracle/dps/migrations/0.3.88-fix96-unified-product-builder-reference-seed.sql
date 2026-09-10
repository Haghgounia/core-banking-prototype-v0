-- ============================================================================
-- Core Banking Prototype 0.3.88 / FIX96
-- DPS reference seed for Unified Product Builder product/deposit semantics.
-- Existing legacy codes are deliberately preserved for backward compatibility.
-- ============================================================================
SET DEFINE OFF;

-- Product families expected by the target Unified Product Builder.
INSERT INTO DPS.REF_PRODUCT_FAMILY_CODE
  (PRODUCT_FAMILY_ID, CODE, NAME_FA, NAME_EN, DESCRIPTION, IS_ACTIVE, VERSION_NO, IS_CURRENT, RECORD_VERSION, CREATED_BY)
SELECT DPS.SEQ_REF_PRODUCT_FAMILY_CODE.NEXTVAL, x.CODE, x.NAME_FA, x.NAME_EN, x.DESCRIPTION, 1, 1, 1, 1, 'FIX96'
FROM (
  SELECT 'CURRENT_ACCOUNT' CODE, 'خانواده حساب جاری' NAME_FA, 'Current Account' NAME_EN, 'خانواده حساب جاری' DESCRIPTION FROM DUAL UNION ALL
  SELECT 'QARD_SAVINGS', 'خانواده قرض‌الحسنه پس‌انداز', 'Qard Savings', 'خانواده سپرده قرض‌الحسنه پس‌انداز' FROM DUAL UNION ALL
  SELECT 'SHORT_TERM_DEPOSIT', 'خانواده سپرده کوتاه‌مدت', 'Short-Term Deposit', 'خانواده سپرده کوتاه‌مدت' FROM DUAL UNION ALL
  SELECT 'LONG_TERM_DEPOSIT', 'خانواده سپرده بلندمدت', 'Long-Term Deposit', 'خانواده سپرده بلندمدت' FROM DUAL UNION ALL
  SELECT 'CERTIFICATE_OF_DEPOSIT', 'خانواده گواهی سپرده', 'Certificate of Deposit', 'خانواده گواهی سپرده' FROM DUAL UNION ALL
  SELECT 'NOSTRO_ACCOUNT', 'خانواده حساب نوسترو', 'Nostro Account', 'حساب ما نزد بانک مقابل' FROM DUAL UNION ALL
  SELECT 'VOSTRO_ACCOUNT', 'خانواده حساب وسترو', 'Vostro Account', 'حساب بانک مقابل نزد ما' FROM DUAL UNION ALL
  SELECT 'RETAIL_LOAN', 'خانواده تسهیلات خرد', 'Retail Loan', 'خانواده تسهیلات خرد' FROM DUAL UNION ALL
  SELECT 'QARD_HASAN_LOAN', 'خانواده تسهیلات قرض‌الحسنه', 'Qard Hasan Loan', 'خانواده تسهیلات قرض‌الحسنه' FROM DUAL
) x
WHERE NOT EXISTS (
  SELECT 1 FROM DPS.REF_PRODUCT_FAMILY_CODE r
  WHERE r.CODE = x.CODE AND r.IS_CURRENT = 1
);

-- Deposit group codes expected by DEPOSIT_PRODUCT_PROFILE.
INSERT INTO DPS.REF_DEPOSIT_GROUP_CODE
  (DEPOSIT_GROUP_ID, CODE, NAME_FA, NAME_EN, DESCRIPTION, IS_ACTIVE, VERSION_NO, IS_CURRENT, RECORD_VERSION, CREATED_BY)
SELECT DPS.SEQ_REF_DEPOSIT_GROUP_CODE.NEXTVAL, x.CODE, x.NAME_FA, x.NAME_EN, x.NAME_FA, 1, 1, 1, 1, 'FIX96'
FROM (
  SELECT 'QARD_HASAN' CODE, 'قرض‌الحسنه' NAME_FA, 'Qard Hasan' NAME_EN FROM DUAL UNION ALL
  SELECT 'CURRENT', 'جاری', 'Current' FROM DUAL UNION ALL
  SELECT 'SHORT_TERM', 'کوتاه‌مدت', 'Short Term' FROM DUAL UNION ALL
  SELECT 'LONG_TERM', 'بلندمدت', 'Long Term' FROM DUAL UNION ALL
  SELECT 'CERTIFICATE', 'گواهی سپرده', 'Certificate of Deposit' FROM DUAL
) x
WHERE NOT EXISTS (
  SELECT 1 FROM DPS.REF_DEPOSIT_GROUP_CODE r
  WHERE r.CODE = x.CODE AND r.IS_CURRENT = 1
);

-- Deposit type codes expected by DEPOSIT_PRODUCT_PROFILE.
INSERT INTO DPS.REF_DEPOSIT_TYPE_CODE
  (DEPOSIT_TYPE_ID, CODE, NAME_FA, NAME_EN, DESCRIPTION, IS_ACTIVE, VERSION_NO, IS_CURRENT, RECORD_VERSION, CREATED_BY)
SELECT DPS.SEQ_REF_DEPOSIT_TYPE_CODE.NEXTVAL, x.CODE, x.NAME_FA, x.NAME_EN, x.NAME_FA, 1, 1, 1, 1, 'FIX96'
FROM (
  SELECT 'SAVINGS' CODE, 'پس‌انداز قرض‌الحسنه' NAME_FA, 'Qard Savings' NAME_EN FROM DUAL UNION ALL
  SELECT 'CURRENT_NO_INTEREST', 'جاری بدون سود', 'Current No Interest' FROM DUAL UNION ALL
  SELECT 'SHORT_TERM_DEPOSIT', 'سپرده کوتاه‌مدت', 'Short-Term Deposit' FROM DUAL UNION ALL
  SELECT 'LONG_TERM_DEPOSIT', 'سپرده بلندمدت', 'Long-Term Deposit' FROM DUAL UNION ALL
  SELECT 'CERTIFICATE_OF_DEPOSIT', 'گواهی سپرده', 'Certificate of Deposit' FROM DUAL
) x
WHERE NOT EXISTS (
  SELECT 1 FROM DPS.REF_DEPOSIT_TYPE_CODE r
  WHERE r.CODE = x.CODE AND r.IS_CURRENT = 1
);

COMMIT;

PROMPT [DPS FIX96] Semantic family/deposit reference seed completed. Legacy rows were preserved.
