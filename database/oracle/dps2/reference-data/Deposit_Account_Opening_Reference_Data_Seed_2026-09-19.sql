-- ============================================================================
-- Deposit Account Opening - Reference Data Seed Script
-- Source: latest attached Enterprise Architect XMI / Oracle physical model
-- Package: Deposit Account Opening - Reference Data
-- Revised: 2026-09-19 (DPS2-qualified + Phase 5/6 reconciliation)
-- Strategy: idempotent MERGE; safe to rerun.
--
-- IMPORTANT
-- 1) All target objects are explicitly qualified as DPS2; current schema may differ.
-- 2) The XMI models numeric PKs as AutoNum; this script intentionally omits PK IDs.
-- 3) Database character set should support Persian text (recommended AL32UTF8).
-- 4) Rows not assigned stable seed codes in XMI are NOT invented silently.
-- 5) Some values are added because the same XMI uses them in CHECK/default logic;
--    those rows are labeled as model-consistency values in DESCRIPTION.
-- 6) XMI review found three suspicious FK mappings in the operational model:
--    DEPOSIT_OPENING_REQUEST.REQUEST_TYPE_CODE -> REF_DEP_OPEN_REQUEST_STATUS.REQUEST_STATUS_CODE
--    DEPOSIT_OPENING_DECISION.DECISION_CODE -> REF_DEP_OPEN_DECISION_REASON.DECISION_REASON_CODE
--    DEPOSIT_OPENING_TERMS_ACCEPTANCE.ACCEPTANCE_SOURCE_CODE -> REF_DEP_OPEN_ACCEPTANCE_STATUS.ACCEPTANCE_STATUS_CODE
--    This seed script DOES NOT contaminate lookup data to satisfy those mappings.
--    Companion migration 0.9.3-reference-data-fk-reconciliation.sql reconciles the physical FK contract.
-- ============================================================================

SET DEFINE OFF
SET SERVEROUTPUT ON
SET FEEDBACK ON
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

PROMPT === Preflight: DPS2 reference tables ===
SELECT SYS_CONTEXT('USERENV','SESSION_USER') AS SESSION_USER,
       SYS_CONTEXT('USERENV','CURRENT_SCHEMA') AS CURRENT_SCHEMA
  FROM DUAL;
DECLARE
  l_missing NUMBER;
BEGIN
  SELECT COUNT(*)
    INTO l_missing
    FROM (
      SELECT 'REF_DEP_OPEN_OPERATION' table_name FROM dual
      UNION ALL SELECT 'REF_DEP_OPEN_SNAPSHOT_TYPE' table_name FROM dual
      UNION ALL SELECT 'REF_DEP_OPEN_BATCH_SOURCE_TYPE' table_name FROM dual
      UNION ALL SELECT 'REF_DEP_OPEN_FIELD_CHANGE_TYPE' table_name FROM dual
      UNION ALL SELECT 'REF_DEP_OPEN_CHECK' table_name FROM dual
      UNION ALL SELECT 'REF_DEP_OPEN_BATCH_STATUS' table_name FROM dual
      UNION ALL SELECT 'REF_DEP_OPEN_ACCEPTANCE_SOURCE' table_name FROM dual
      UNION ALL SELECT 'REF_DEP_OPEN_CHANNEL_SCOPE' table_name FROM dual
      UNION ALL SELECT 'REF_DEP_OPEN_FIRST_PAYMENT_RULE' table_name FROM dual
      UNION ALL SELECT 'REF_DEP_OPEN_VERIFICATION_STATUS' table_name FROM dual
      UNION ALL SELECT 'REF_DEP_OPEN_ENROLLMENT_STATUS' table_name FROM dual
      UNION ALL SELECT 'REF_DEP_OPEN_REQUEST_STATUS' table_name FROM dual
      UNION ALL SELECT 'REF_DEP_OPEN_TAX_STATUS_SOURCE' table_name FROM dual
      UNION ALL SELECT 'REF_DEP_OPEN_FREQUENCY' table_name FROM dual
      UNION ALL SELECT 'REF_DEP_OPEN_ACTION_STATUS' table_name FROM dual
      UNION ALL SELECT 'REF_DEP_OPEN_SIGNATURE_RULE' table_name FROM dual
      UNION ALL SELECT 'REF_DEP_OPEN_PAYMENT_INSTRUMENT' table_name FROM dual
      UNION ALL SELECT 'REF_DEP_OPEN_SERVICE' table_name FROM dual
      UNION ALL SELECT 'REF_DEP_OPEN_DOCUMENT_STATUS' table_name FROM dual
      UNION ALL SELECT 'REF_DEP_OPEN_FUNDING_STATUS' table_name FROM dual
      UNION ALL SELECT 'REF_DEP_OPEN_PAYMENT_DESTINATION' table_name FROM dual
      UNION ALL SELECT 'REF_DEP_OPEN_CHECK_TYPE' table_name FROM dual
      UNION ALL SELECT 'REF_DEP_OPEN_PRICING_OVERRIDE_TYPE' table_name FROM dual
      UNION ALL SELECT 'REF_DEP_OPEN_CHECK_RESULT' table_name FROM dual
      UNION ALL SELECT 'REF_DEP_OPEN_ACCESS_ROLE' table_name FROM dual
      UNION ALL SELECT 'REF_DEP_OPEN_AUTHORITY_SCOPE' table_name FROM dual
      UNION ALL SELECT 'REF_DEP_OPEN_AUDIT_ACTOR_TYPE' table_name FROM dual
      UNION ALL SELECT 'REF_DEP_OPEN_SIGNATORY_ROLE' table_name FROM dual
      UNION ALL SELECT 'REF_DEP_OPEN_SOURCE_OF_FUNDS' table_name FROM dual
      UNION ALL SELECT 'REF_DEP_OPEN_DECISION_REASON' table_name FROM dual
      UNION ALL SELECT 'REF_DEP_OPEN_BATCH_ITEM_STATUS' table_name FROM dual
      UNION ALL SELECT 'REF_DEP_OPEN_PAYMENT_DAY_RULE' table_name FROM dual
      UNION ALL SELECT 'REF_DEP_OPEN_PARTY_ROLE' table_name FROM dual
      UNION ALL SELECT 'REF_DEP_OPEN_MATURITY_ACTION' table_name FROM dual
      UNION ALL SELECT 'REF_DEP_OPEN_OWNERSHIP_TYPE' table_name FROM dual
      UNION ALL SELECT 'REF_DEP_OPEN_REQUEST_TYPE' table_name FROM dual
      UNION ALL SELECT 'REF_DEP_OPEN_AUTHORITY_LEVEL' table_name FROM dual
      UNION ALL SELECT 'REF_DEP_OPEN_DELEGATION_TYPE' table_name FROM dual
      UNION ALL SELECT 'REF_DEP_OPEN_DECISION' table_name FROM dual
      UNION ALL SELECT 'REF_DEP_OPEN_AUDIT_EVENT_TYPE' table_name FROM dual
      UNION ALL SELECT 'REF_DEP_OPEN_PROFIT_CALC_METHOD' table_name FROM dual
      UNION ALL SELECT 'REF_DEP_OPEN_BATCH_ERROR_STAGE' table_name FROM dual
      UNION ALL SELECT 'REF_DEP_OPEN_CHANNEL' table_name FROM dual
      UNION ALL SELECT 'REF_DEP_OPEN_ACCEPTANCE_STATUS' table_name FROM dual
      UNION ALL SELECT 'REF_DEP_OPEN_DOCUMENT_TYPE' table_name FROM dual
      UNION ALL SELECT 'REF_DEP_OPEN_HOLIDAY_ADJUSTMENT' table_name FROM dual
      UNION ALL SELECT 'REF_DEP_OPEN_BENEFICIARY_TYPE' table_name FROM dual
      UNION ALL SELECT 'REF_DEP_OPEN_PURPOSE' table_name FROM dual
      UNION ALL SELECT 'REF_DEP_OPEN_CHANNEL_ORG_MAP' table_name FROM dual
      UNION ALL SELECT 'REF_DEP_OPEN_INSTRUCTION_SOURCE' table_name FROM dual
      UNION ALL SELECT 'REF_DEP_OPEN_CHANGE_TYPE' table_name FROM dual
      UNION ALL SELECT 'REF_DEP_OPEN_TAX_RESIDENCY' table_name FROM dual
      UNION ALL SELECT 'REF_DEP_OPEN_CHANGE_STATUS' table_name FROM dual
      UNION ALL SELECT 'REF_DEP_OPEN_CHANGE_REASON' table_name FROM dual
      UNION ALL SELECT 'REF_DEP_OPEN_FUNDING_METHOD' table_name FROM dual
      UNION ALL SELECT 'REF_DEP_OPEN_WITHDRAWAL_MEDIA' table_name FROM dual
      UNION ALL SELECT 'REF_DEP_OPEN_DAY_COUNT_BASIS' table_name FROM dual
      UNION ALL SELECT 'REF_DEP_OPEN_BATCH_ERROR_CODE' table_name FROM dual
      UNION ALL SELECT 'REF_DEP_OPEN_TERM_UNIT' table_name FROM dual
      UNION ALL SELECT 'REF_DEP_OPEN_CHECK_PHASE' table_name FROM dual
      UNION ALL SELECT 'REF_DEP_OPEN_BLOCKING_SCOPE' table_name FROM dual
      UNION ALL SELECT 'REF_DEP_OPEN_OBLIGATION_TYPE' table_name FROM dual
      UNION ALL SELECT 'REF_DEP_OPEN_SETTLEMENT_STATUS' table_name FROM dual
      UNION ALL SELECT 'REF_DEP_OPEN_ACTIVATION_STATUS' table_name FROM dual
      UNION ALL SELECT 'REF_DEP_OPEN_JOINT_BASIS' table_name FROM dual
      UNION ALL SELECT 'REF_DEP_OPEN_FUND_PURPOSE' table_name FROM dual
    ) e
   WHERE NOT EXISTS (SELECT 1 FROM all_tables u WHERE u.owner = 'DPS2' AND u.table_name = e.table_name);
  IF l_missing > 0 THEN
    RAISE_APPLICATION_ERROR(-20001, 'One or more DPS2 Reference Data tables are missing.');
  END IF;
END;
/

PROMPT === Seeding XML-defined and XMI-consistency reference values ===

-- --------------------------------------------------------------------------
-- REF_DEP_OPEN_OPERATION
-- --------------------------------------------------------------------------
MERGE INTO DPS2.REF_DEP_OPEN_OPERATION t
USING (SELECT 'ALL_OPERATIONS' AS OPERATION_CODE FROM dual) s
ON (t.OPERATION_CODE = s.OPERATION_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'همه عملیات مجاز',
    t.TITLE_EN = 'All Operations',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 10,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'همه عملیات مجاز' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'All Operations' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 10 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (OPERATION_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('ALL_OPERATIONS', 'همه عملیات مجاز', 'All Operations', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 10, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_OPERATION t
USING (SELECT 'TRANSFER_OUT' AS OPERATION_CODE FROM dual) s
ON (t.OPERATION_CODE = s.OPERATION_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'انتقال وجه خروجی',
    t.TITLE_EN = 'Transfer Out',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 20,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'انتقال وجه خروجی' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Transfer Out' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 20 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (OPERATION_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('TRANSFER_OUT', 'انتقال وجه خروجی', 'Transfer Out', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 20, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_OPERATION t
USING (SELECT 'CASH_WITHDRAWAL' AS OPERATION_CODE FROM dual) s
ON (t.OPERATION_CODE = s.OPERATION_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'برداشت نقدی',
    t.TITLE_EN = 'Cash Withdrawal',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 30,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'برداشت نقدی' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Cash Withdrawal' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 30 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (OPERATION_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('CASH_WITHDRAWAL', 'برداشت نقدی', 'Cash Withdrawal', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 30, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_OPERATION t
USING (SELECT 'CHEQUE' AS OPERATION_CODE FROM dual) s
ON (t.OPERATION_CODE = s.OPERATION_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'عملیات چک',
    t.TITLE_EN = 'Cheque',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 40,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'عملیات چک' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Cheque' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 40 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (OPERATION_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('CHEQUE', 'عملیات چک', 'Cheque', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 40, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

-- --------------------------------------------------------------------------
-- REF_DEP_OPEN_SNAPSHOT_TYPE
-- Source authority: Phase 5 migration / immutable Opening audit contract.
-- --------------------------------------------------------------------------
MERGE INTO DPS2.REF_DEP_OPEN_SNAPSHOT_TYPE t
USING (
  SELECT 'SUBMITTED' code, 'تصویر زمان ارسال' fa, 'Submitted snapshot' en, 10 ord FROM DUAL UNION ALL
  SELECT 'APPROVED', 'تصویر زمان تأیید', 'Approved snapshot', 20 FROM DUAL UNION ALL
  SELECT 'COMPLETED', 'تصویر زمان تکمیل', 'Completed snapshot', 30 FROM DUAL UNION ALL
  SELECT 'PRE_CHANGE', 'تصویر قبل از تغییر', 'Pre-change snapshot', 40 FROM DUAL UNION ALL
  SELECT 'POST_CHANGE', 'تصویر بعد از تغییر', 'Post-change snapshot', 50 FROM DUAL
) s
ON (t.SNAPSHOT_TYPE_CODE = s.code)
WHEN MATCHED THEN UPDATE SET
  t.TITLE_FA=s.fa, t.TITLE_EN=s.en,
  t.DESCRIPTION='Phase 5 immutable canonical Opening snapshot type',
  t.DISPLAY_ORDER=s.ord, t.IS_ACTIVE=1,
  t.UPDATED_AT=SYSTIMESTAMP, t.UPDATED_BY='DEP_OPEN_REF_SEED',
  t.RECORD_VERSION=NVL(t.RECORD_VERSION,0)+1
WHERE NVL(t.TITLE_FA,CHR(0))<>s.fa
   OR NVL(t.TITLE_EN,CHR(0))<>s.en
   OR NVL(t.DESCRIPTION,CHR(0))<>'Phase 5 immutable canonical Opening snapshot type'
   OR NVL(t.DISPLAY_ORDER,-999999999)<>s.ord OR NVL(t.IS_ACTIVE,-1)<>1
WHEN NOT MATCHED THEN INSERT
  (SNAPSHOT_TYPE_CODE,TITLE_FA,TITLE_EN,DESCRIPTION,DISPLAY_ORDER,IS_ACTIVE,CREATED_AT,CREATED_BY,RECORD_VERSION)
VALUES
  (s.code,s.fa,s.en,'Phase 5 immutable canonical Opening snapshot type',s.ord,1,SYSTIMESTAMP,'DEP_OPEN_REF_SEED',1);

-- --------------------------------------------------------------------------
-- REF_DEP_OPEN_BATCH_SOURCE_TYPE
-- --------------------------------------------------------------------------
MERGE INTO DPS2.REF_DEP_OPEN_BATCH_SOURCE_TYPE t
USING (SELECT 'FILE' AS SOURCE_TYPE_CODE FROM dual) s
ON (t.SOURCE_TYPE_CODE = s.SOURCE_TYPE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'فایل',
    t.TITLE_EN = 'File',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 10,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'فایل' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'File' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 10 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (SOURCE_TYPE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('FILE', 'فایل', 'File', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 10, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_BATCH_SOURCE_TYPE t
USING (SELECT 'API' AS SOURCE_TYPE_CODE FROM dual) s
ON (t.SOURCE_TYPE_CODE = s.SOURCE_TYPE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'رابط برنامه‌نویسی API',
    t.TITLE_EN = 'API',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 20,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'رابط برنامه‌نویسی API' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'API' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 20 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (SOURCE_TYPE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('API', 'رابط برنامه‌نویسی API', 'API', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 20, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_BATCH_SOURCE_TYPE t
USING (SELECT 'MANUAL' AS SOURCE_TYPE_CODE FROM dual) s
ON (t.SOURCE_TYPE_CODE = s.SOURCE_TYPE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'ورود دستی',
    t.TITLE_EN = 'Manual',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 30,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'ورود دستی' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Manual' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 30 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (SOURCE_TYPE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('MANUAL', 'ورود دستی', 'Manual', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 30, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

-- --------------------------------------------------------------------------
-- REF_DEP_OPEN_FIELD_CHANGE_TYPE
-- Includes one or more values required/mentioned elsewhere in the same XMI.
-- --------------------------------------------------------------------------
MERGE INTO DPS2.REF_DEP_OPEN_FIELD_CHANGE_TYPE t
USING (SELECT 'SET' AS FIELD_CHANGE_TYPE_CODE FROM dual) s
ON (t.FIELD_CHANGE_TYPE_CODE = s.FIELD_CHANGE_TYPE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'مقداردهی اولیه',
    t.TITLE_EN = 'Set',
    t.DESCRIPTION = 'مقدار موردنیاز/ذکرشده در Default، Constraint یا شرح عملیاتی همین XMI برای حفظ سازگاری مدل.',
    t.DISPLAY_ORDER = 10,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'مقداردهی اولیه' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Set' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'مقدار موردنیاز/ذکرشده در Default، Constraint یا شرح عملیاتی همین XMI برای حفظ سازگاری مدل.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 10 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (FIELD_CHANGE_TYPE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('SET', 'مقداردهی اولیه', 'Set', 'مقدار موردنیاز/ذکرشده در Default، Constraint یا شرح عملیاتی همین XMI برای حفظ سازگاری مدل.', 10, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_FIELD_CHANGE_TYPE t
USING (SELECT 'UPDATE' AS FIELD_CHANGE_TYPE_CODE FROM dual) s
ON (t.FIELD_CHANGE_TYPE_CODE = s.FIELD_CHANGE_TYPE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'به‌روزرسانی',
    t.TITLE_EN = 'Update',
    t.DESCRIPTION = 'مقدار موردنیاز/ذکرشده در Default، Constraint یا شرح عملیاتی همین XMI برای حفظ سازگاری مدل.',
    t.DISPLAY_ORDER = 20,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'به‌روزرسانی' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Update' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'مقدار موردنیاز/ذکرشده در Default، Constraint یا شرح عملیاتی همین XMI برای حفظ سازگاری مدل.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 20 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (FIELD_CHANGE_TYPE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('UPDATE', 'به‌روزرسانی', 'Update', 'مقدار موردنیاز/ذکرشده در Default، Constraint یا شرح عملیاتی همین XMI برای حفظ سازگاری مدل.', 20, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_FIELD_CHANGE_TYPE t
USING (SELECT 'CLEAR' AS FIELD_CHANGE_TYPE_CODE FROM dual) s
ON (t.FIELD_CHANGE_TYPE_CODE = s.FIELD_CHANGE_TYPE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'پاک‌کردن مقدار',
    t.TITLE_EN = 'Clear',
    t.DESCRIPTION = 'مقدار موردنیاز/ذکرشده در Default، Constraint یا شرح عملیاتی همین XMI برای حفظ سازگاری مدل.',
    t.DISPLAY_ORDER = 30,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'پاک‌کردن مقدار' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Clear' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'مقدار موردنیاز/ذکرشده در Default، Constraint یا شرح عملیاتی همین XMI برای حفظ سازگاری مدل.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 30 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (FIELD_CHANGE_TYPE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('CLEAR', 'پاک‌کردن مقدار', 'Clear', 'مقدار موردنیاز/ذکرشده در Default، Constraint یا شرح عملیاتی همین XMI برای حفظ سازگاری مدل.', 30, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_FIELD_CHANGE_TYPE t
USING (SELECT 'ADD' AS FIELD_CHANGE_TYPE_CODE FROM dual) s
ON (t.FIELD_CHANGE_TYPE_CODE = s.FIELD_CHANGE_TYPE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'افزودن عضو',
    t.TITLE_EN = 'Add',
    t.DESCRIPTION = 'مقدار موردنیاز/ذکرشده در Default، Constraint یا شرح عملیاتی همین XMI برای حفظ سازگاری مدل.',
    t.DISPLAY_ORDER = 40,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'افزودن عضو' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Add' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'مقدار موردنیاز/ذکرشده در Default، Constraint یا شرح عملیاتی همین XMI برای حفظ سازگاری مدل.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 40 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (FIELD_CHANGE_TYPE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('ADD', 'افزودن عضو', 'Add', 'مقدار موردنیاز/ذکرشده در Default، Constraint یا شرح عملیاتی همین XMI برای حفظ سازگاری مدل.', 40, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_FIELD_CHANGE_TYPE t
USING (SELECT 'REMOVE' AS FIELD_CHANGE_TYPE_CODE FROM dual) s
ON (t.FIELD_CHANGE_TYPE_CODE = s.FIELD_CHANGE_TYPE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'حذف عضو',
    t.TITLE_EN = 'Remove',
    t.DESCRIPTION = 'مقدار موردنیاز/ذکرشده در Default، Constraint یا شرح عملیاتی همین XMI برای حفظ سازگاری مدل.',
    t.DISPLAY_ORDER = 50,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'حذف عضو' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Remove' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'مقدار موردنیاز/ذکرشده در Default، Constraint یا شرح عملیاتی همین XMI برای حفظ سازگاری مدل.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 50 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (FIELD_CHANGE_TYPE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('REMOVE', 'حذف عضو', 'Remove', 'مقدار موردنیاز/ذکرشده در Default، Constraint یا شرح عملیاتی همین XMI برای حفظ سازگاری مدل.', 50, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

-- --------------------------------------------------------------------------
-- REF_DEP_OPEN_BATCH_STATUS
-- --------------------------------------------------------------------------
MERGE INTO DPS2.REF_DEP_OPEN_BATCH_STATUS t
USING (SELECT 'DRAFT' AS BATCH_STATUS_CODE FROM dual) s
ON (t.BATCH_STATUS_CODE = s.BATCH_STATUS_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'پیش‌نویس',
    t.TITLE_EN = 'Draft',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 10,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'پیش‌نویس' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Draft' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 10 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (BATCH_STATUS_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('DRAFT', 'پیش‌نویس', 'Draft', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 10, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_BATCH_STATUS t
USING (SELECT 'VALIDATING' AS BATCH_STATUS_CODE FROM dual) s
ON (t.BATCH_STATUS_CODE = s.BATCH_STATUS_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'در حال اعتبارسنجی',
    t.TITLE_EN = 'Validating',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 20,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'در حال اعتبارسنجی' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Validating' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 20 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (BATCH_STATUS_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('VALIDATING', 'در حال اعتبارسنجی', 'Validating', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 20, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_BATCH_STATUS t
USING (SELECT 'READY' AS BATCH_STATUS_CODE FROM dual) s
ON (t.BATCH_STATUS_CODE = s.BATCH_STATUS_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'آماده',
    t.TITLE_EN = 'Ready',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 30,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'آماده' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Ready' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 30 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (BATCH_STATUS_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('READY', 'آماده', 'Ready', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 30, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_BATCH_STATUS t
USING (SELECT 'PROCESSING' AS BATCH_STATUS_CODE FROM dual) s
ON (t.BATCH_STATUS_CODE = s.BATCH_STATUS_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'در حال پردازش',
    t.TITLE_EN = 'Processing',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 40,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'در حال پردازش' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Processing' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 40 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (BATCH_STATUS_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('PROCESSING', 'در حال پردازش', 'Processing', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 40, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_BATCH_STATUS t
USING (SELECT 'COMPLETED' AS BATCH_STATUS_CODE FROM dual) s
ON (t.BATCH_STATUS_CODE = s.BATCH_STATUS_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'تکمیل‌شده',
    t.TITLE_EN = 'Completed',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 50,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'تکمیل‌شده' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Completed' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 50 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (BATCH_STATUS_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('COMPLETED', 'تکمیل‌شده', 'Completed', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 50, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_BATCH_STATUS t
USING (SELECT 'PARTIAL' AS BATCH_STATUS_CODE FROM dual) s
ON (t.BATCH_STATUS_CODE = s.BATCH_STATUS_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'جزئی',
    t.TITLE_EN = 'Partial',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 60,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'جزئی' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Partial' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 60 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (BATCH_STATUS_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('PARTIAL', 'جزئی', 'Partial', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 60, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_BATCH_STATUS t
USING (SELECT 'FAILED' AS BATCH_STATUS_CODE FROM dual) s
ON (t.BATCH_STATUS_CODE = s.BATCH_STATUS_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'ناموفق',
    t.TITLE_EN = 'Failed',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 70,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'ناموفق' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Failed' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 70 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (BATCH_STATUS_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('FAILED', 'ناموفق', 'Failed', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 70, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

-- --------------------------------------------------------------------------
-- REF_DEP_OPEN_ACCEPTANCE_SOURCE
-- --------------------------------------------------------------------------
MERGE INTO DPS2.REF_DEP_OPEN_ACCEPTANCE_SOURCE t
USING (SELECT 'UI' AS ACCEPTANCE_SOURCE_CODE FROM dual) s
ON (t.ACCEPTANCE_SOURCE_CODE = s.ACCEPTANCE_SOURCE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'رابط کاربری',
    t.TITLE_EN = 'UI',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 10,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'رابط کاربری' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'UI' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 10 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (ACCEPTANCE_SOURCE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('UI', 'رابط کاربری', 'UI', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 10, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_ACCEPTANCE_SOURCE t
USING (SELECT 'API' AS ACCEPTANCE_SOURCE_CODE FROM dual) s
ON (t.ACCEPTANCE_SOURCE_CODE = s.ACCEPTANCE_SOURCE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'رابط برنامه‌نویسی API',
    t.TITLE_EN = 'API',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 20,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'رابط برنامه‌نویسی API' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'API' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 20 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (ACCEPTANCE_SOURCE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('API', 'رابط برنامه‌نویسی API', 'API', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 20, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_ACCEPTANCE_SOURCE t
USING (SELECT 'BRANCH' AS ACCEPTANCE_SOURCE_CODE FROM dual) s
ON (t.ACCEPTANCE_SOURCE_CODE = s.ACCEPTANCE_SOURCE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'شعبه',
    t.TITLE_EN = 'Branch',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 30,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'شعبه' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Branch' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 30 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (ACCEPTANCE_SOURCE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('BRANCH', 'شعبه', 'Branch', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 30, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_ACCEPTANCE_SOURCE t
USING (SELECT 'BATCH' AS ACCEPTANCE_SOURCE_CODE FROM dual) s
ON (t.ACCEPTANCE_SOURCE_CODE = s.ACCEPTANCE_SOURCE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'پردازش گروهی',
    t.TITLE_EN = 'Batch',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 40,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'پردازش گروهی' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Batch' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 40 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (ACCEPTANCE_SOURCE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('BATCH', 'پردازش گروهی', 'Batch', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 40, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

-- --------------------------------------------------------------------------
-- REF_DEP_OPEN_CHANNEL_SCOPE
-- --------------------------------------------------------------------------
MERGE INTO DPS2.REF_DEP_OPEN_CHANNEL_SCOPE t
USING (SELECT 'BRANCH' AS CHANNEL_SCOPE_CODE FROM dual) s
ON (t.CHANNEL_SCOPE_CODE = s.CHANNEL_SCOPE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'شعبه',
    t.TITLE_EN = 'Branch',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 10,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'شعبه' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Branch' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 10 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (CHANNEL_SCOPE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('BRANCH', 'شعبه', 'Branch', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 10, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_CHANNEL_SCOPE t
USING (SELECT 'INTERNET' AS CHANNEL_SCOPE_CODE FROM dual) s
ON (t.CHANNEL_SCOPE_CODE = s.CHANNEL_SCOPE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'اینترنت‌بانک',
    t.TITLE_EN = 'Internet',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 20,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'اینترنت‌بانک' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Internet' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 20 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (CHANNEL_SCOPE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('INTERNET', 'اینترنت‌بانک', 'Internet', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 20, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_CHANNEL_SCOPE t
USING (SELECT 'MOBILE' AS CHANNEL_SCOPE_CODE FROM dual) s
ON (t.CHANNEL_SCOPE_CODE = s.CHANNEL_SCOPE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'موبایل‌بانک',
    t.TITLE_EN = 'Mobile',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 30,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'موبایل‌بانک' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Mobile' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 30 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (CHANNEL_SCOPE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('MOBILE', 'موبایل‌بانک', 'Mobile', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 30, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_CHANNEL_SCOPE t
USING (SELECT 'API' AS CHANNEL_SCOPE_CODE FROM dual) s
ON (t.CHANNEL_SCOPE_CODE = s.CHANNEL_SCOPE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'رابط برنامه‌نویسی API',
    t.TITLE_EN = 'API',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 40,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'رابط برنامه‌نویسی API' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'API' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 40 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (CHANNEL_SCOPE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('API', 'رابط برنامه‌نویسی API', 'API', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 40, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_CHANNEL_SCOPE t
USING (SELECT 'ALL' AS CHANNEL_SCOPE_CODE FROM dual) s
ON (t.CHANNEL_SCOPE_CODE = s.CHANNEL_SCOPE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'همه',
    t.TITLE_EN = 'All',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 50,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'همه' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'All' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 50 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (CHANNEL_SCOPE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('ALL', 'همه', 'All', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 50, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

-- --------------------------------------------------------------------------
-- REF_DEP_OPEN_FIRST_PAYMENT_RULE
-- --------------------------------------------------------------------------
MERGE INTO DPS2.REF_DEP_OPEN_FIRST_PAYMENT_RULE t
USING (SELECT 'PRORATE_FROM_OPENING' AS FIRST_PAYMENT_RULE_CODE FROM dual) s
ON (t.FIRST_PAYMENT_RULE_CODE = s.FIRST_PAYMENT_RULE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'محاسبه تناسبی از زمان افتتاح',
    t.TITLE_EN = 'Prorate From Opening',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 10,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'محاسبه تناسبی از زمان افتتاح' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Prorate From Opening' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 10 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (FIRST_PAYMENT_RULE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('PRORATE_FROM_OPENING', 'محاسبه تناسبی از زمان افتتاح', 'Prorate From Opening', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 10, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_FIRST_PAYMENT_RULE t
USING (SELECT 'MATURITY_ONLY' AS FIRST_PAYMENT_RULE_CODE FROM dual) s
ON (t.FIRST_PAYMENT_RULE_CODE = s.FIRST_PAYMENT_RULE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'فقط در سررسید',
    t.TITLE_EN = 'Maturity Only',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 20,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'فقط در سررسید' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Maturity Only' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 20 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (FIRST_PAYMENT_RULE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('MATURITY_ONLY', 'فقط در سررسید', 'Maturity Only', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 20, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

-- --------------------------------------------------------------------------
-- REF_DEP_OPEN_VERIFICATION_STATUS
-- --------------------------------------------------------------------------
MERGE INTO DPS2.REF_DEP_OPEN_VERIFICATION_STATUS t
USING (SELECT 'PENDING' AS VERIFICATION_STATUS_CODE FROM dual) s
ON (t.VERIFICATION_STATUS_CODE = s.VERIFICATION_STATUS_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'در انتظار',
    t.TITLE_EN = 'Pending',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 10,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'در انتظار' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Pending' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 10 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (VERIFICATION_STATUS_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('PENDING', 'در انتظار', 'Pending', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 10, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_VERIFICATION_STATUS t
USING (SELECT 'VERIFIED' AS VERIFICATION_STATUS_CODE FROM dual) s
ON (t.VERIFICATION_STATUS_CODE = s.VERIFICATION_STATUS_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'تأییدشده',
    t.TITLE_EN = 'Verified',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 20,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'تأییدشده' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Verified' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 20 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (VERIFICATION_STATUS_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('VERIFIED', 'تأییدشده', 'Verified', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 20, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_VERIFICATION_STATUS t
USING (SELECT 'REJECTED' AS VERIFICATION_STATUS_CODE FROM dual) s
ON (t.VERIFICATION_STATUS_CODE = s.VERIFICATION_STATUS_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'ردشده',
    t.TITLE_EN = 'Rejected',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 30,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'ردشده' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Rejected' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 30 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (VERIFICATION_STATUS_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('REJECTED', 'ردشده', 'Rejected', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 30, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

-- --------------------------------------------------------------------------
-- REF_DEP_OPEN_ENROLLMENT_STATUS
-- --------------------------------------------------------------------------
MERGE INTO DPS2.REF_DEP_OPEN_ENROLLMENT_STATUS t
USING (SELECT 'REQUESTED' AS ENROLLMENT_STATUS_CODE FROM dual) s
ON (t.ENROLLMENT_STATUS_CODE = s.ENROLLMENT_STATUS_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'درخواست‌شده',
    t.TITLE_EN = 'Requested',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 10,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'درخواست‌شده' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Requested' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 10 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (ENROLLMENT_STATUS_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('REQUESTED', 'درخواست‌شده', 'Requested', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 10, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_ENROLLMENT_STATUS t
USING (SELECT 'ELIGIBLE' AS ENROLLMENT_STATUS_CODE FROM dual) s
ON (t.ENROLLMENT_STATUS_CODE = s.ENROLLMENT_STATUS_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'واجد شرایط',
    t.TITLE_EN = 'Eligible',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 20,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'واجد شرایط' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Eligible' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 20 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (ENROLLMENT_STATUS_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('ELIGIBLE', 'واجد شرایط', 'Eligible', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 20, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_ENROLLMENT_STATUS t
USING (SELECT 'INELIGIBLE' AS ENROLLMENT_STATUS_CODE FROM dual) s
ON (t.ENROLLMENT_STATUS_CODE = s.ENROLLMENT_STATUS_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'فاقد شرایط',
    t.TITLE_EN = 'Ineligible',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 30,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'فاقد شرایط' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Ineligible' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 30 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (ENROLLMENT_STATUS_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('INELIGIBLE', 'فاقد شرایط', 'Ineligible', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 30, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_ENROLLMENT_STATUS t
USING (SELECT 'ENROLLED' AS ENROLLMENT_STATUS_CODE FROM dual) s
ON (t.ENROLLMENT_STATUS_CODE = s.ENROLLMENT_STATUS_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'عضوشده',
    t.TITLE_EN = 'Enrolled',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 40,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'عضوشده' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Enrolled' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 40 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (ENROLLMENT_STATUS_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('ENROLLED', 'عضوشده', 'Enrolled', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 40, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

-- --------------------------------------------------------------------------
-- REF_DEP_OPEN_REQUEST_STATUS
-- Includes one or more values required/mentioned elsewhere in the same XMI.
-- --------------------------------------------------------------------------
MERGE INTO DPS2.REF_DEP_OPEN_REQUEST_STATUS t
USING (SELECT 'DRAFT' AS REQUEST_STATUS_CODE FROM dual) s
ON (t.REQUEST_STATUS_CODE = s.REQUEST_STATUS_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'پیش‌نویس',
    t.TITLE_EN = 'Draft',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 10,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'پیش‌نویس' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Draft' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 10 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (REQUEST_STATUS_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('DRAFT', 'پیش‌نویس', 'Draft', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 10, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_REQUEST_STATUS t
USING (SELECT 'SUBMITTED' AS REQUEST_STATUS_CODE FROM dual) s
ON (t.REQUEST_STATUS_CODE = s.REQUEST_STATUS_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'ارسال‌شده',
    t.TITLE_EN = 'Submitted',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 20,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'ارسال‌شده' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Submitted' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 20 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (REQUEST_STATUS_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('SUBMITTED', 'ارسال‌شده', 'Submitted', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 20, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_REQUEST_STATUS t
USING (SELECT 'APPROVED' AS REQUEST_STATUS_CODE FROM dual) s
ON (t.REQUEST_STATUS_CODE = s.REQUEST_STATUS_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'تأییدشده',
    t.TITLE_EN = 'Approved',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 30,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'تأییدشده' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Approved' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 30 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (REQUEST_STATUS_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('APPROVED', 'تأییدشده', 'Approved', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 30, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_REQUEST_STATUS t
USING (SELECT 'REJECTED' AS REQUEST_STATUS_CODE FROM dual) s
ON (t.REQUEST_STATUS_CODE = s.REQUEST_STATUS_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'ردشده',
    t.TITLE_EN = 'Rejected',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 40,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'ردشده' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Rejected' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 40 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (REQUEST_STATUS_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('REJECTED', 'ردشده', 'Rejected', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 40, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_REQUEST_STATUS t
USING (SELECT 'COMPLETED' AS REQUEST_STATUS_CODE FROM dual) s
ON (t.REQUEST_STATUS_CODE = s.REQUEST_STATUS_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'تکمیل‌شده',
    t.TITLE_EN = 'Completed',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 50,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'تکمیل‌شده' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Completed' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 50 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (REQUEST_STATUS_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('COMPLETED', 'تکمیل‌شده', 'Completed', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 50, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_REQUEST_STATUS t
USING (SELECT 'IN_REVIEW' AS REQUEST_STATUS_CODE FROM dual) s
ON (t.REQUEST_STATUS_CODE = s.REQUEST_STATUS_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'در حال بررسی',
    t.TITLE_EN = 'In Review',
    t.DESCRIPTION = 'مقدار موردنیاز/ذکرشده در Default، Constraint یا شرح عملیاتی همین XMI برای حفظ سازگاری مدل.',
    t.DISPLAY_ORDER = 60,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'در حال بررسی' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'In Review' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'مقدار موردنیاز/ذکرشده در Default، Constraint یا شرح عملیاتی همین XMI برای حفظ سازگاری مدل.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 60 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (REQUEST_STATUS_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('IN_REVIEW', 'در حال بررسی', 'In Review', 'مقدار موردنیاز/ذکرشده در Default، Constraint یا شرح عملیاتی همین XMI برای حفظ سازگاری مدل.', 60, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_REQUEST_STATUS t
USING (SELECT 'RETURNED_FOR_CORRECTION' AS REQUEST_STATUS_CODE FROM dual) s
ON (t.REQUEST_STATUS_CODE = s.REQUEST_STATUS_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'برگشت برای اصلاح',
    t.TITLE_EN = 'Returned For Correction',
    t.DESCRIPTION = 'مقدار موردنیاز/ذکرشده در Default، Constraint یا شرح عملیاتی همین XMI برای حفظ سازگاری مدل.',
    t.DISPLAY_ORDER = 70,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'برگشت برای اصلاح' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Returned For Correction' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'مقدار موردنیاز/ذکرشده در Default، Constraint یا شرح عملیاتی همین XMI برای حفظ سازگاری مدل.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 70 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (REQUEST_STATUS_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('RETURNED_FOR_CORRECTION', 'برگشت برای اصلاح', 'Returned For Correction', 'مقدار موردنیاز/ذکرشده در Default، Constraint یا شرح عملیاتی همین XMI برای حفظ سازگاری مدل.', 70, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_REQUEST_STATUS t
USING (SELECT 'FAILED' AS REQUEST_STATUS_CODE FROM dual) s
ON (t.REQUEST_STATUS_CODE = s.REQUEST_STATUS_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'ناموفق',
    t.TITLE_EN = 'Failed',
    t.DESCRIPTION = 'مقدار موردنیاز/ذکرشده در Default، Constraint یا شرح عملیاتی همین XMI برای حفظ سازگاری مدل.',
    t.DISPLAY_ORDER = 80,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'ناموفق' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Failed' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'مقدار موردنیاز/ذکرشده در Default، Constraint یا شرح عملیاتی همین XMI برای حفظ سازگاری مدل.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 80 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (REQUEST_STATUS_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('FAILED', 'ناموفق', 'Failed', 'مقدار موردنیاز/ذکرشده در Default، Constraint یا شرح عملیاتی همین XMI برای حفظ سازگاری مدل.', 80, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_REQUEST_STATUS t
USING (SELECT 'CANCELLED' AS REQUEST_STATUS_CODE FROM dual) s
ON (t.REQUEST_STATUS_CODE = s.REQUEST_STATUS_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'لغوشده',
    t.TITLE_EN = 'Cancelled',
    t.DESCRIPTION = 'مقدار موردنیاز/ذکرشده در Default، Constraint یا شرح عملیاتی همین XMI برای حفظ سازگاری مدل.',
    t.DISPLAY_ORDER = 90,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'لغوشده' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Cancelled' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'مقدار موردنیاز/ذکرشده در Default، Constraint یا شرح عملیاتی همین XMI برای حفظ سازگاری مدل.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 90 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (REQUEST_STATUS_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('CANCELLED', 'لغوشده', 'Cancelled', 'مقدار موردنیاز/ذکرشده در Default، Constraint یا شرح عملیاتی همین XMI برای حفظ سازگاری مدل.', 90, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

-- --------------------------------------------------------------------------
-- REF_DEP_OPEN_TAX_STATUS_SOURCE
-- --------------------------------------------------------------------------
MERGE INTO DPS2.REF_DEP_OPEN_TAX_STATUS_SOURCE t
USING (SELECT 'TAX_SERVICE' AS TAX_STATUS_SOURCE_CODE FROM dual) s
ON (t.TAX_STATUS_SOURCE_CODE = s.TAX_STATUS_SOURCE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'سرویس مالیاتی',
    t.TITLE_EN = 'Tax Service',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 10,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'سرویس مالیاتی' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Tax Service' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 10 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (TAX_STATUS_SOURCE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('TAX_SERVICE', 'سرویس مالیاتی', 'Tax Service', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 10, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_TAX_STATUS_SOURCE t
USING (SELECT 'MANUAL_OVERRIDE' AS TAX_STATUS_SOURCE_CODE FROM dual) s
ON (t.TAX_STATUS_SOURCE_CODE = s.TAX_STATUS_SOURCE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'اصلاح دستی مجاز',
    t.TITLE_EN = 'Manual Override',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 20,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'اصلاح دستی مجاز' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Manual Override' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 20 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (TAX_STATUS_SOURCE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('MANUAL_OVERRIDE', 'اصلاح دستی مجاز', 'Manual Override', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 20, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

-- --------------------------------------------------------------------------
-- REF_DEP_OPEN_FREQUENCY
-- --------------------------------------------------------------------------
MERGE INTO DPS2.REF_DEP_OPEN_FREQUENCY t
USING (SELECT 'DAILY' AS FREQUENCY_CODE FROM dual) s
ON (t.FREQUENCY_CODE = s.FREQUENCY_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'روزانه',
    t.TITLE_EN = 'Daily',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 10,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'روزانه' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Daily' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 10 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (FREQUENCY_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('DAILY', 'روزانه', 'Daily', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 10, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_FREQUENCY t
USING (SELECT 'MONTHLY' AS FREQUENCY_CODE FROM dual) s
ON (t.FREQUENCY_CODE = s.FREQUENCY_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'ماهانه',
    t.TITLE_EN = 'Monthly',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 20,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'ماهانه' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Monthly' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 20 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (FREQUENCY_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('MONTHLY', 'ماهانه', 'Monthly', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 20, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_FREQUENCY t
USING (SELECT 'MATURITY' AS FREQUENCY_CODE FROM dual) s
ON (t.FREQUENCY_CODE = s.FREQUENCY_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'سررسید',
    t.TITLE_EN = 'Maturity',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 30,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'سررسید' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Maturity' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 30 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (FREQUENCY_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('MATURITY', 'سررسید', 'Maturity', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 30, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

-- --------------------------------------------------------------------------
-- REF_DEP_OPEN_ACTION_STATUS
-- --------------------------------------------------------------------------
MERGE INTO DPS2.REF_DEP_OPEN_ACTION_STATUS t
USING (SELECT 'PENDING' AS REQUEST_STATUS_CODE FROM dual) s
ON (t.REQUEST_STATUS_CODE = s.REQUEST_STATUS_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'در انتظار',
    t.TITLE_EN = 'Pending',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 10,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'در انتظار' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Pending' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 10 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (REQUEST_STATUS_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('PENDING', 'در انتظار', 'Pending', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 10, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_ACTION_STATUS t
USING (SELECT 'REQUESTED' AS REQUEST_STATUS_CODE FROM dual) s
ON (t.REQUEST_STATUS_CODE = s.REQUEST_STATUS_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'درخواست‌شده',
    t.TITLE_EN = 'Requested',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 20,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'درخواست‌شده' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Requested' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 20 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (REQUEST_STATUS_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('REQUESTED', 'درخواست‌شده', 'Requested', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 20, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_ACTION_STATUS t
USING (SELECT 'APPROVED' AS REQUEST_STATUS_CODE FROM dual) s
ON (t.REQUEST_STATUS_CODE = s.REQUEST_STATUS_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'تأییدشده',
    t.TITLE_EN = 'Approved',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 30,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'تأییدشده' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Approved' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 30 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (REQUEST_STATUS_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('APPROVED', 'تأییدشده', 'Approved', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 30, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_ACTION_STATUS t
USING (SELECT 'REJECTED' AS REQUEST_STATUS_CODE FROM dual) s
ON (t.REQUEST_STATUS_CODE = s.REQUEST_STATUS_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'ردشده',
    t.TITLE_EN = 'Rejected',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 40,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'ردشده' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Rejected' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 40 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (REQUEST_STATUS_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('REJECTED', 'ردشده', 'Rejected', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 40, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_ACTION_STATUS t
USING (SELECT 'ISSUED' AS REQUEST_STATUS_CODE FROM dual) s
ON (t.REQUEST_STATUS_CODE = s.REQUEST_STATUS_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'صادرشده',
    t.TITLE_EN = 'Issued',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 50,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'صادرشده' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Issued' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 50 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (REQUEST_STATUS_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('ISSUED', 'صادرشده', 'Issued', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 50, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

-- --------------------------------------------------------------------------
-- REF_DEP_OPEN_SIGNATURE_RULE
-- --------------------------------------------------------------------------
MERGE INTO DPS2.REF_DEP_OPEN_SIGNATURE_RULE t
USING (SELECT 'ALL' AS SIGNATURE_RULE_CODE FROM dual) s
ON (t.SIGNATURE_RULE_CODE = s.SIGNATURE_RULE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'همه',
    t.TITLE_EN = 'All',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 10,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'همه' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'All' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 10 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (SIGNATURE_RULE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('ALL', 'همه', 'All', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 10, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_SIGNATURE_RULE t
USING (SELECT 'ANY' AS SIGNATURE_RULE_CODE FROM dual) s
ON (t.SIGNATURE_RULE_CODE = s.SIGNATURE_RULE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'هر یک',
    t.TITLE_EN = 'Any',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 20,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'هر یک' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Any' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 20 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (SIGNATURE_RULE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('ANY', 'هر یک', 'Any', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 20, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_SIGNATURE_RULE t
USING (SELECT 'AT_LEAST_N' AS SIGNATURE_RULE_CODE FROM dual) s
ON (t.SIGNATURE_RULE_CODE = s.SIGNATURE_RULE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'حداقل N امضا',
    t.TITLE_EN = 'At Least N',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 30,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'حداقل N امضا' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'At Least N' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 30 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (SIGNATURE_RULE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('AT_LEAST_N', 'حداقل N امضا', 'At Least N', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 30, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

-- --------------------------------------------------------------------------
-- REF_DEP_OPEN_PAYMENT_INSTRUMENT
-- --------------------------------------------------------------------------
MERGE INTO DPS2.REF_DEP_OPEN_PAYMENT_INSTRUMENT t
USING (SELECT 'CARD' AS INSTRUMENT_TYPE_CODE FROM dual) s
ON (t.INSTRUMENT_TYPE_CODE = s.INSTRUMENT_TYPE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'کارت',
    t.TITLE_EN = 'Card',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 10,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'کارت' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Card' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 10 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (INSTRUMENT_TYPE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('CARD', 'کارت', 'Card', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 10, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_PAYMENT_INSTRUMENT t
USING (SELECT 'CHEQUEBOOK' AS INSTRUMENT_TYPE_CODE FROM dual) s
ON (t.INSTRUMENT_TYPE_CODE = s.INSTRUMENT_TYPE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'دسته‌چک',
    t.TITLE_EN = 'Chequebook',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 20,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'دسته‌چک' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Chequebook' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 20 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (INSTRUMENT_TYPE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('CHEQUEBOOK', 'دسته‌چک', 'Chequebook', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 20, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_PAYMENT_INSTRUMENT t
USING (SELECT 'WALLET' AS INSTRUMENT_TYPE_CODE FROM dual) s
ON (t.INSTRUMENT_TYPE_CODE = s.INSTRUMENT_TYPE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'کیف پول',
    t.TITLE_EN = 'Wallet',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 30,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'کیف پول' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Wallet' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 30 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (INSTRUMENT_TYPE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('WALLET', 'کیف پول', 'Wallet', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 30, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_PAYMENT_INSTRUMENT t
USING (SELECT 'TOKEN' AS INSTRUMENT_TYPE_CODE FROM dual) s
ON (t.INSTRUMENT_TYPE_CODE = s.INSTRUMENT_TYPE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'توکن',
    t.TITLE_EN = 'Token',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 40,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'توکن' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Token' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 40 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (INSTRUMENT_TYPE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('TOKEN', 'توکن', 'Token', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 40, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

-- --------------------------------------------------------------------------
-- REF_DEP_OPEN_SERVICE
-- --------------------------------------------------------------------------
MERGE INTO DPS2.REF_DEP_OPEN_SERVICE t
USING (SELECT 'ACCOUNT_INQUIRY' AS SERVICE_CODE FROM dual) s
ON (t.SERVICE_CODE = s.SERVICE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'استعلام اطلاعات حساب',
    t.TITLE_EN = 'Account Inquiry',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 10,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'استعلام اطلاعات حساب' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Account Inquiry' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 10 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (SERVICE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('ACCOUNT_INQUIRY', 'استعلام اطلاعات حساب', 'Account Inquiry', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 10, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_SERVICE t
USING (SELECT 'BALANCE_CONFIRMATION' AS SERVICE_CODE FROM dual) s
ON (t.SERVICE_CODE = s.SERVICE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'تأییدیه مانده و وضعیت حساب',
    t.TITLE_EN = 'Balance Confirmation',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 20,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'تأییدیه مانده و وضعیت حساب' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Balance Confirmation' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 20 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (SERVICE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('BALANCE_CONFIRMATION', 'تأییدیه مانده و وضعیت حساب', 'Balance Confirmation', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 20, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_SERVICE t
USING (SELECT 'NOTIFICATION' AS SERVICE_CODE FROM dual) s
ON (t.SERVICE_CODE = s.SERVICE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'اطلاع‌رسانی رویداد حساب',
    t.TITLE_EN = 'Notification',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 30,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'اطلاع‌رسانی رویداد حساب' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Notification' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 30 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (SERVICE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('NOTIFICATION', 'اطلاع‌رسانی رویداد حساب', 'Notification', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 30, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_SERVICE t
USING (SELECT 'API_ACCESS' AS SERVICE_CODE FROM dual) s
ON (t.SERVICE_CODE = s.SERVICE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'دسترسی API',
    t.TITLE_EN = 'API Access',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 40,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'دسترسی API' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'API Access' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 40 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (SERVICE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('API_ACCESS', 'دسترسی API', 'API Access', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 40, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_SERVICE t
USING (SELECT 'STATEMENT' AS SERVICE_CODE FROM dual) s
ON (t.SERVICE_CODE = s.SERVICE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'صورتحساب',
    t.TITLE_EN = 'Statement',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 50,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'صورتحساب' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Statement' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 50 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (SERVICE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('STATEMENT', 'صورتحساب', 'Statement', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 50, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

-- --------------------------------------------------------------------------
-- REF_DEP_OPEN_DOCUMENT_STATUS
-- --------------------------------------------------------------------------
MERGE INTO DPS2.REF_DEP_OPEN_DOCUMENT_STATUS t
USING (SELECT 'MISSING' AS DOCUMENT_STATUS_CODE FROM dual) s
ON (t.DOCUMENT_STATUS_CODE = s.DOCUMENT_STATUS_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'ناقص / دریافت‌نشده',
    t.TITLE_EN = 'Missing',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 10,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'ناقص / دریافت‌نشده' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Missing' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 10 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (DOCUMENT_STATUS_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('MISSING', 'ناقص / دریافت‌نشده', 'Missing', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 10, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_DOCUMENT_STATUS t
USING (SELECT 'RECEIVED' AS DOCUMENT_STATUS_CODE FROM dual) s
ON (t.DOCUMENT_STATUS_CODE = s.DOCUMENT_STATUS_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'دریافت‌شده',
    t.TITLE_EN = 'Received',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 20,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'دریافت‌شده' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Received' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 20 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (DOCUMENT_STATUS_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('RECEIVED', 'دریافت‌شده', 'Received', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 20, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_DOCUMENT_STATUS t
USING (SELECT 'VERIFIED' AS DOCUMENT_STATUS_CODE FROM dual) s
ON (t.DOCUMENT_STATUS_CODE = s.DOCUMENT_STATUS_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'تأییدشده',
    t.TITLE_EN = 'Verified',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 30,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'تأییدشده' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Verified' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 30 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (DOCUMENT_STATUS_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('VERIFIED', 'تأییدشده', 'Verified', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 30, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_DOCUMENT_STATUS t
USING (SELECT 'REJECTED' AS DOCUMENT_STATUS_CODE FROM dual) s
ON (t.DOCUMENT_STATUS_CODE = s.DOCUMENT_STATUS_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'ردشده',
    t.TITLE_EN = 'Rejected',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 40,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'ردشده' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Rejected' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 40 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (DOCUMENT_STATUS_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('REJECTED', 'ردشده', 'Rejected', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 40, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

-- --------------------------------------------------------------------------
-- REF_DEP_OPEN_FUNDING_STATUS
-- Includes one or more values required/mentioned elsewhere in the same XMI.
-- --------------------------------------------------------------------------
MERGE INTO DPS2.REF_DEP_OPEN_FUNDING_STATUS t
USING (SELECT 'PENDING' AS FUNDING_STATUS_CODE FROM dual) s
ON (t.FUNDING_STATUS_CODE = s.FUNDING_STATUS_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'در انتظار',
    t.TITLE_EN = 'Pending',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 10,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'در انتظار' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Pending' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 10 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (FUNDING_STATUS_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('PENDING', 'در انتظار', 'Pending', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 10, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_FUNDING_STATUS t
USING (SELECT 'SUCCESS' AS FUNDING_STATUS_CODE FROM dual) s
ON (t.FUNDING_STATUS_CODE = s.FUNDING_STATUS_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'موفق',
    t.TITLE_EN = 'Success',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 20,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'موفق' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Success' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 20 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (FUNDING_STATUS_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('SUCCESS', 'موفق', 'Success', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 20, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_FUNDING_STATUS t
USING (SELECT 'FAILED' AS FUNDING_STATUS_CODE FROM dual) s
ON (t.FUNDING_STATUS_CODE = s.FUNDING_STATUS_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'ناموفق',
    t.TITLE_EN = 'Failed',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 30,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'ناموفق' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Failed' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 30 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (FUNDING_STATUS_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('FAILED', 'ناموفق', 'Failed', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 30, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_FUNDING_STATUS t
USING (SELECT 'PARTIAL' AS FUNDING_STATUS_CODE FROM dual) s
ON (t.FUNDING_STATUS_CODE = s.FUNDING_STATUS_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'جزئی',
    t.TITLE_EN = 'Partial',
    t.DESCRIPTION = 'مقدار موردنیاز/ذکرشده در Default، Constraint یا شرح عملیاتی همین XMI برای حفظ سازگاری مدل.',
    t.DISPLAY_ORDER = 40,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'جزئی' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Partial' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'مقدار موردنیاز/ذکرشده در Default، Constraint یا شرح عملیاتی همین XMI برای حفظ سازگاری مدل.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 40 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (FUNDING_STATUS_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('PARTIAL', 'جزئی', 'Partial', 'مقدار موردنیاز/ذکرشده در Default، Constraint یا شرح عملیاتی همین XMI برای حفظ سازگاری مدل.', 40, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_FUNDING_STATUS t
USING (SELECT 'REVERSED' AS FUNDING_STATUS_CODE FROM dual) s
ON (t.FUNDING_STATUS_CODE = s.FUNDING_STATUS_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'برگشت‌شده',
    t.TITLE_EN = 'Reversed',
    t.DESCRIPTION = 'مقدار موردنیاز/ذکرشده در Default، Constraint یا شرح عملیاتی همین XMI برای حفظ سازگاری مدل.',
    t.DISPLAY_ORDER = 50,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'برگشت‌شده' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Reversed' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'مقدار موردنیاز/ذکرشده در Default، Constraint یا شرح عملیاتی همین XMI برای حفظ سازگاری مدل.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 50 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (FUNDING_STATUS_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('REVERSED', 'برگشت‌شده', 'Reversed', 'مقدار موردنیاز/ذکرشده در Default، Constraint یا شرح عملیاتی همین XMI برای حفظ سازگاری مدل.', 50, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_FUNDING_STATUS t
USING (SELECT 'REFUNDED' AS FUNDING_STATUS_CODE FROM dual) s
ON (t.FUNDING_STATUS_CODE = s.FUNDING_STATUS_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'مستردشده',
    t.TITLE_EN = 'Refunded',
    t.DESCRIPTION = 'مقدار موردنیاز/ذکرشده در Default، Constraint یا شرح عملیاتی همین XMI برای حفظ سازگاری مدل.',
    t.DISPLAY_ORDER = 60,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'مستردشده' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Refunded' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'مقدار موردنیاز/ذکرشده در Default، Constraint یا شرح عملیاتی همین XMI برای حفظ سازگاری مدل.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 60 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (FUNDING_STATUS_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('REFUNDED', 'مستردشده', 'Refunded', 'مقدار موردنیاز/ذکرشده در Default، Constraint یا شرح عملیاتی همین XMI برای حفظ سازگاری مدل.', 60, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

-- --------------------------------------------------------------------------
-- REF_DEP_OPEN_PAYMENT_DESTINATION
-- --------------------------------------------------------------------------
MERGE INTO DPS2.REF_DEP_OPEN_PAYMENT_DESTINATION t
USING (SELECT 'SAME_DEPOSIT' AS PAYMENT_DESTINATION_CODE FROM dual) s
ON (t.PAYMENT_DESTINATION_CODE = s.PAYMENT_DESTINATION_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'همان سپرده',
    t.TITLE_EN = 'Same Deposit',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 10,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'همان سپرده' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Same Deposit' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 10 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (PAYMENT_DESTINATION_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('SAME_DEPOSIT', 'همان سپرده', 'Same Deposit', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 10, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_PAYMENT_DESTINATION t
USING (SELECT 'CUSTOMER_SELECTED_ACCOUNT' AS PAYMENT_DESTINATION_CODE FROM dual) s
ON (t.PAYMENT_DESTINATION_CODE = s.PAYMENT_DESTINATION_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'حساب انتخابی مشتری',
    t.TITLE_EN = 'Customer Selected Account',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 20,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'حساب انتخابی مشتری' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Customer Selected Account' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 20 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (PAYMENT_DESTINATION_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('CUSTOMER_SELECTED_ACCOUNT', 'حساب انتخابی مشتری', 'Customer Selected Account', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 20, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_PAYMENT_DESTINATION t
USING (SELECT 'LINKED_ACCOUNT' AS PAYMENT_DESTINATION_CODE FROM dual) s
ON (t.PAYMENT_DESTINATION_CODE = s.PAYMENT_DESTINATION_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'حساب متصل',
    t.TITLE_EN = 'Linked Account',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 30,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'حساب متصل' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Linked Account' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 30 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (PAYMENT_DESTINATION_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('LINKED_ACCOUNT', 'حساب متصل', 'Linked Account', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 30, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

-- --------------------------------------------------------------------------
-- REF_DEP_OPEN_CHECK_TYPE
-- --------------------------------------------------------------------------
MERGE INTO DPS2.REF_DEP_OPEN_CHECK_TYPE t
USING (SELECT 'PRODUCT_RULE' AS CHECK_TYPE_CODE FROM dual) s
ON (t.CHECK_TYPE_CODE = s.CHECK_TYPE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'قاعده محصول',
    t.TITLE_EN = 'Product Rule',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 10,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'قاعده محصول' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Product Rule' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 10 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (CHECK_TYPE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('PRODUCT_RULE', 'قاعده محصول', 'Product Rule', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 10, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_CHECK_TYPE t
USING (SELECT 'COMPLIANCE' AS CHECK_TYPE_CODE FROM dual) s
ON (t.CHECK_TYPE_CODE = s.CHECK_TYPE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'تطبیق و الزامات نظارتی',
    t.TITLE_EN = 'Compliance',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 20,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'تطبیق و الزامات نظارتی' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Compliance' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 20 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (CHECK_TYPE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('COMPLIANCE', 'تطبیق و الزامات نظارتی', 'Compliance', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 20, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_CHECK_TYPE t
USING (SELECT 'DOCUMENT' AS CHECK_TYPE_CODE FROM dual) s
ON (t.CHECK_TYPE_CODE = s.CHECK_TYPE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'مدرک',
    t.TITLE_EN = 'Document',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 30,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'مدرک' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Document' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 30 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (CHECK_TYPE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('DOCUMENT', 'مدرک', 'Document', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 30, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_CHECK_TYPE t
USING (SELECT 'INQUIRY' AS CHECK_TYPE_CODE FROM dual) s
ON (t.CHECK_TYPE_CODE = s.CHECK_TYPE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'استعلام',
    t.TITLE_EN = 'Inquiry',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 40,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'استعلام' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Inquiry' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 40 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (CHECK_TYPE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('INQUIRY', 'استعلام', 'Inquiry', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 40, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_CHECK_TYPE t
USING (SELECT 'AUTHORITY' AS CHECK_TYPE_CODE FROM dual) s
ON (t.CHECK_TYPE_CODE = s.CHECK_TYPE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'اختیار',
    t.TITLE_EN = 'Authority',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 50,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'اختیار' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Authority' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 50 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (CHECK_TYPE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('AUTHORITY', 'اختیار', 'Authority', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 50, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_CHECK_TYPE t
USING (SELECT 'CONSENT' AS CHECK_TYPE_CODE FROM dual) s
ON (t.CHECK_TYPE_CODE = s.CHECK_TYPE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'رضایت / پذیرش',
    t.TITLE_EN = 'Consent',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 60,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'رضایت / پذیرش' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Consent' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 60 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (CHECK_TYPE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('CONSENT', 'رضایت / پذیرش', 'Consent', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 60, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_CHECK_TYPE t
USING (SELECT 'OPERATIONAL' AS CHECK_TYPE_CODE FROM dual) s
ON (t.CHECK_TYPE_CODE = s.CHECK_TYPE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'عملیاتی',
    t.TITLE_EN = 'Operational',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 70,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'عملیاتی' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Operational' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 70 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (CHECK_TYPE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('OPERATIONAL', 'عملیاتی', 'Operational', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 70, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

-- --------------------------------------------------------------------------
-- REF_DEP_OPEN_PRICING_OVERRIDE_TYPE
-- --------------------------------------------------------------------------
MERGE INTO DPS2.REF_DEP_OPEN_PRICING_OVERRIDE_TYPE t
USING (SELECT 'PREFERENTIAL_RATE' AS OVERRIDE_TYPE_CODE FROM dual) s
ON (t.OVERRIDE_TYPE_CODE = s.OVERRIDE_TYPE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'نرخ ترجیحی',
    t.TITLE_EN = 'Preferential Rate',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 10,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'نرخ ترجیحی' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Preferential Rate' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 10 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (OVERRIDE_TYPE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('PREFERENTIAL_RATE', 'نرخ ترجیحی', 'Preferential Rate', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 10, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_PRICING_OVERRIDE_TYPE t
USING (SELECT 'NEGOTIATED_RATE' AS OVERRIDE_TYPE_CODE FROM dual) s
ON (t.OVERRIDE_TYPE_CODE = s.OVERRIDE_TYPE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'نرخ توافقی',
    t.TITLE_EN = 'Negotiated Rate',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 20,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'نرخ توافقی' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Negotiated Rate' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 20 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (OVERRIDE_TYPE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('NEGOTIATED_RATE', 'نرخ توافقی', 'Negotiated Rate', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 20, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_PRICING_OVERRIDE_TYPE t
USING (SELECT 'FEE_DISCOUNT' AS OVERRIDE_TYPE_CODE FROM dual) s
ON (t.OVERRIDE_TYPE_CODE = s.OVERRIDE_TYPE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'تخفیف کارمزد',
    t.TITLE_EN = 'Fee Discount',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 30,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'تخفیف کارمزد' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Fee Discount' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 30 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (OVERRIDE_TYPE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('FEE_DISCOUNT', 'تخفیف کارمزد', 'Fee Discount', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 30, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_PRICING_OVERRIDE_TYPE t
USING (SELECT 'FEE_WAIVER' AS OVERRIDE_TYPE_CODE FROM dual) s
ON (t.OVERRIDE_TYPE_CODE = s.OVERRIDE_TYPE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'معافیت کارمزد',
    t.TITLE_EN = 'Fee Waiver',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 40,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'معافیت کارمزد' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Fee Waiver' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 40 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (OVERRIDE_TYPE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('FEE_WAIVER', 'معافیت کارمزد', 'Fee Waiver', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 40, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

-- --------------------------------------------------------------------------
-- REF_DEP_OPEN_CHECK_RESULT
-- Includes one or more values required/mentioned elsewhere in the same XMI.
-- --------------------------------------------------------------------------
MERGE INTO DPS2.REF_DEP_OPEN_CHECK_RESULT t
USING (SELECT 'PASS' AS RESULT_STATUS_CODE FROM dual) s
ON (t.RESULT_STATUS_CODE = s.RESULT_STATUS_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'موفق',
    t.TITLE_EN = 'Pass',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 10,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'موفق' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Pass' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 10 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (RESULT_STATUS_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('PASS', 'موفق', 'Pass', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 10, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_CHECK_RESULT t
USING (SELECT 'FAIL' AS RESULT_STATUS_CODE FROM dual) s
ON (t.RESULT_STATUS_CODE = s.RESULT_STATUS_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'ناموفق',
    t.TITLE_EN = 'Fail',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 20,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'ناموفق' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Fail' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 20 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (RESULT_STATUS_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('FAIL', 'ناموفق', 'Fail', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 20, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_CHECK_RESULT t
USING (SELECT 'PENDING' AS RESULT_STATUS_CODE FROM dual) s
ON (t.RESULT_STATUS_CODE = s.RESULT_STATUS_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'در انتظار',
    t.TITLE_EN = 'Pending',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 30,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'در انتظار' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Pending' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 30 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (RESULT_STATUS_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('PENDING', 'در انتظار', 'Pending', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 30, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_CHECK_RESULT t
USING (SELECT 'WAIVED' AS RESULT_STATUS_CODE FROM dual) s
ON (t.RESULT_STATUS_CODE = s.RESULT_STATUS_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'معاف‌شده',
    t.TITLE_EN = 'Waived',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 40,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'معاف‌شده' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Waived' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 40 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (RESULT_STATUS_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('WAIVED', 'معاف‌شده', 'Waived', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 40, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_CHECK_RESULT t
USING (SELECT 'NOT_APPLICABLE' AS RESULT_STATUS_CODE FROM dual) s
ON (t.RESULT_STATUS_CODE = s.RESULT_STATUS_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'نامرتبط / غیرقابل اعمال',
    t.TITLE_EN = 'Not Applicable',
    t.DESCRIPTION = 'مقدار موردنیاز/ذکرشده در Default، Constraint یا شرح عملیاتی همین XMI برای حفظ سازگاری مدل.',
    t.DISPLAY_ORDER = 50,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'نامرتبط / غیرقابل اعمال' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Not Applicable' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'مقدار موردنیاز/ذکرشده در Default، Constraint یا شرح عملیاتی همین XMI برای حفظ سازگاری مدل.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 50 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (RESULT_STATUS_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('NOT_APPLICABLE', 'نامرتبط / غیرقابل اعمال', 'Not Applicable', 'مقدار موردنیاز/ذکرشده در Default، Constraint یا شرح عملیاتی همین XMI برای حفظ سازگاری مدل.', 50, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

-- --------------------------------------------------------------------------
-- REF_DEP_OPEN_ACCESS_ROLE
-- --------------------------------------------------------------------------
MERGE INTO DPS2.REF_DEP_OPEN_ACCESS_ROLE t
USING (SELECT 'VIEWER' AS ACCESS_ROLE_CODE FROM dual) s
ON (t.ACCESS_ROLE_CODE = s.ACCESS_ROLE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'مشاهده‌گر',
    t.TITLE_EN = 'Viewer',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 10,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'مشاهده‌گر' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Viewer' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 10 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (ACCESS_ROLE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('VIEWER', 'مشاهده‌گر', 'Viewer', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 10, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_ACCESS_ROLE t
USING (SELECT 'OPERATOR' AS ACCESS_ROLE_CODE FROM dual) s
ON (t.ACCESS_ROLE_CODE = s.ACCESS_ROLE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'کاربر عملیاتی',
    t.TITLE_EN = 'Operator',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 20,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'کاربر عملیاتی' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Operator' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 20 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (ACCESS_ROLE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('OPERATOR', 'کاربر عملیاتی', 'Operator', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 20, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_ACCESS_ROLE t
USING (SELECT 'DELEGATE' AS ACCESS_ROLE_CODE FROM dual) s
ON (t.ACCESS_ROLE_CODE = s.ACCESS_ROLE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'نماینده',
    t.TITLE_EN = 'Delegate',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 30,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'نماینده' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Delegate' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 30 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (ACCESS_ROLE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('DELEGATE', 'نماینده', 'Delegate', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 30, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

-- --------------------------------------------------------------------------
-- REF_DEP_OPEN_AUTHORITY_SCOPE
-- --------------------------------------------------------------------------
MERGE INTO DPS2.REF_DEP_OPEN_AUTHORITY_SCOPE t
USING (SELECT 'ALL_OPERATIONS' AS AUTHORITY_SCOPE_CODE FROM dual) s
ON (t.AUTHORITY_SCOPE_CODE = s.AUTHORITY_SCOPE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'همه عملیات مجاز',
    t.TITLE_EN = 'All Operations',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 10,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'همه عملیات مجاز' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'All Operations' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 10 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (AUTHORITY_SCOPE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('ALL_OPERATIONS', 'همه عملیات مجاز', 'All Operations', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 10, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_AUTHORITY_SCOPE t
USING (SELECT 'INQUIRY_ONLY' AS AUTHORITY_SCOPE_CODE FROM dual) s
ON (t.AUTHORITY_SCOPE_CODE = s.AUTHORITY_SCOPE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'فقط استعلام',
    t.TITLE_EN = 'Inquiry Only',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 20,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'فقط استعلام' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Inquiry Only' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 20 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (AUTHORITY_SCOPE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('INQUIRY_ONLY', 'فقط استعلام', 'Inquiry Only', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 20, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_AUTHORITY_SCOPE t
USING (SELECT 'DEBIT_OPERATIONS' AS AUTHORITY_SCOPE_CODE FROM dual) s
ON (t.AUTHORITY_SCOPE_CODE = s.AUTHORITY_SCOPE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'عملیات بدهکار',
    t.TITLE_EN = 'Debit Operations',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 30,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'عملیات بدهکار' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Debit Operations' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 30 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (AUTHORITY_SCOPE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('DEBIT_OPERATIONS', 'عملیات بدهکار', 'Debit Operations', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 30, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

-- --------------------------------------------------------------------------
-- REF_DEP_OPEN_AUDIT_ACTOR_TYPE
-- Source authority: Phase 5 migration / DepositOpeningAuditService.
-- --------------------------------------------------------------------------
MERGE INTO DPS2.REF_DEP_OPEN_AUDIT_ACTOR_TYPE t
USING (
  SELECT 'USER' code, 'کاربر' fa, 'User' en, 'عامل انسانی ثبت‌کننده عملیات Opening' descr, 10 ord FROM DUAL UNION ALL
  SELECT 'SYSTEM', 'سامانه', 'System', 'عامل سیستمی برای عملیات خودکار و یکپارچه‌سازی', 20 FROM DUAL
) s
ON (t.ACTOR_TYPE_CODE=s.code)
WHEN MATCHED THEN UPDATE SET
  t.TITLE_FA=s.fa, t.TITLE_EN=s.en, t.DESCRIPTION=s.descr,
  t.DISPLAY_ORDER=s.ord, t.IS_ACTIVE=1,
  t.UPDATED_AT=SYSTIMESTAMP, t.UPDATED_BY='DEP_OPEN_REF_SEED',
  t.RECORD_VERSION=NVL(t.RECORD_VERSION,0)+1
WHERE NVL(t.TITLE_FA,CHR(0))<>s.fa OR NVL(t.TITLE_EN,CHR(0))<>s.en
   OR NVL(t.DESCRIPTION,CHR(0))<>s.descr OR NVL(t.DISPLAY_ORDER,-999999999)<>s.ord OR NVL(t.IS_ACTIVE,-1)<>1
WHEN NOT MATCHED THEN INSERT
  (ACTOR_TYPE_CODE,TITLE_FA,TITLE_EN,DESCRIPTION,DISPLAY_ORDER,IS_ACTIVE,CREATED_AT,CREATED_BY,RECORD_VERSION)
VALUES
  (s.code,s.fa,s.en,s.descr,s.ord,1,SYSTIMESTAMP,'DEP_OPEN_REF_SEED',1);

-- --------------------------------------------------------------------------
-- REF_DEP_OPEN_SIGNATORY_ROLE
-- --------------------------------------------------------------------------
MERGE INTO DPS2.REF_DEP_OPEN_SIGNATORY_ROLE t
USING (SELECT 'OWNER_SIGNATORY' AS SIGNATORY_ROLE_CODE FROM dual) s
ON (t.SIGNATORY_ROLE_CODE = s.SIGNATORY_ROLE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'مالک صاحب امضا',
    t.TITLE_EN = 'Owner Signatory',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 10,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'مالک صاحب امضا' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Owner Signatory' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 10 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (SIGNATORY_ROLE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('OWNER_SIGNATORY', 'مالک صاحب امضا', 'Owner Signatory', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 10, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_SIGNATORY_ROLE t
USING (SELECT 'AUTHORIZED_SIGNATORY' AS SIGNATORY_ROLE_CODE FROM dual) s
ON (t.SIGNATORY_ROLE_CODE = s.SIGNATORY_ROLE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'صاحب امضای مجاز',
    t.TITLE_EN = 'Authorized Signatory',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 20,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'صاحب امضای مجاز' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Authorized Signatory' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 20 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (SIGNATORY_ROLE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('AUTHORIZED_SIGNATORY', 'صاحب امضای مجاز', 'Authorized Signatory', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 20, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_SIGNATORY_ROLE t
USING (SELECT 'REPRESENTATIVE' AS SIGNATORY_ROLE_CODE FROM dual) s
ON (t.SIGNATORY_ROLE_CODE = s.SIGNATORY_ROLE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'نماینده',
    t.TITLE_EN = 'Representative',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 30,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'نماینده' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Representative' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 30 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (SIGNATORY_ROLE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('REPRESENTATIVE', 'نماینده', 'Representative', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 30, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_SIGNATORY_ROLE t
USING (SELECT 'GUARDIAN' AS SIGNATORY_ROLE_CODE FROM dual) s
ON (t.SIGNATORY_ROLE_CODE = s.SIGNATORY_ROLE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'قیم',
    t.TITLE_EN = 'Guardian',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 40,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'قیم' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Guardian' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 40 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (SIGNATORY_ROLE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('GUARDIAN', 'قیم', 'Guardian', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 40, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

-- --------------------------------------------------------------------------
-- REF_DEP_OPEN_SOURCE_OF_FUNDS
-- --------------------------------------------------------------------------
MERGE INTO DPS2.REF_DEP_OPEN_SOURCE_OF_FUNDS t
USING (SELECT 'SALARY' AS SOURCE_OF_FUNDS_CODE FROM dual) s
ON (t.SOURCE_OF_FUNDS_CODE = s.SOURCE_OF_FUNDS_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'حقوق و درآمد',
    t.TITLE_EN = 'Salary',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 10,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'حقوق و درآمد' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Salary' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 10 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (SOURCE_OF_FUNDS_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('SALARY', 'حقوق و درآمد', 'Salary', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 10, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_SOURCE_OF_FUNDS t
USING (SELECT 'SAVINGS' AS SOURCE_OF_FUNDS_CODE FROM dual) s
ON (t.SOURCE_OF_FUNDS_CODE = s.SOURCE_OF_FUNDS_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'پس‌انداز قبلی',
    t.TITLE_EN = 'Savings',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 20,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'پس‌انداز قبلی' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Savings' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 20 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (SOURCE_OF_FUNDS_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('SAVINGS', 'پس‌انداز قبلی', 'Savings', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 20, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_SOURCE_OF_FUNDS t
USING (SELECT 'BUSINESS' AS SOURCE_OF_FUNDS_CODE FROM dual) s
ON (t.SOURCE_OF_FUNDS_CODE = s.SOURCE_OF_FUNDS_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'فعالیت تجاری',
    t.TITLE_EN = 'Business',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 30,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'فعالیت تجاری' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Business' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 30 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (SOURCE_OF_FUNDS_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('BUSINESS', 'فعالیت تجاری', 'Business', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 30, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_SOURCE_OF_FUNDS t
USING (SELECT 'OTHER' AS SOURCE_OF_FUNDS_CODE FROM dual) s
ON (t.SOURCE_OF_FUNDS_CODE = s.SOURCE_OF_FUNDS_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'سایر',
    t.TITLE_EN = 'Other',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 40,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'سایر' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Other' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 40 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (SOURCE_OF_FUNDS_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('OTHER', 'سایر', 'Other', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 40, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

-- --------------------------------------------------------------------------
-- REF_DEP_OPEN_BATCH_ITEM_STATUS
-- --------------------------------------------------------------------------
MERGE INTO DPS2.REF_DEP_OPEN_BATCH_ITEM_STATUS t
USING (SELECT 'PENDING' AS ITEM_STATUS_CODE FROM dual) s
ON (t.ITEM_STATUS_CODE = s.ITEM_STATUS_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'در انتظار',
    t.TITLE_EN = 'Pending',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 10,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'در انتظار' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Pending' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 10 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (ITEM_STATUS_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('PENDING', 'در انتظار', 'Pending', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 10, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_BATCH_ITEM_STATUS t
USING (SELECT 'VALID' AS ITEM_STATUS_CODE FROM dual) s
ON (t.ITEM_STATUS_CODE = s.ITEM_STATUS_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'معتبر',
    t.TITLE_EN = 'Valid',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 20,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'معتبر' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Valid' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 20 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (ITEM_STATUS_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('VALID', 'معتبر', 'Valid', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 20, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_BATCH_ITEM_STATUS t
USING (SELECT 'INVALID' AS ITEM_STATUS_CODE FROM dual) s
ON (t.ITEM_STATUS_CODE = s.ITEM_STATUS_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'نامعتبر',
    t.TITLE_EN = 'Invalid',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 30,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'نامعتبر' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Invalid' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 30 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (ITEM_STATUS_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('INVALID', 'نامعتبر', 'Invalid', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 30, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_BATCH_ITEM_STATUS t
USING (SELECT 'PROCESSING' AS ITEM_STATUS_CODE FROM dual) s
ON (t.ITEM_STATUS_CODE = s.ITEM_STATUS_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'در حال پردازش',
    t.TITLE_EN = 'Processing',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 40,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'در حال پردازش' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Processing' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 40 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (ITEM_STATUS_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('PROCESSING', 'در حال پردازش', 'Processing', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 40, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_BATCH_ITEM_STATUS t
USING (SELECT 'SUCCESS' AS ITEM_STATUS_CODE FROM dual) s
ON (t.ITEM_STATUS_CODE = s.ITEM_STATUS_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'موفق',
    t.TITLE_EN = 'Success',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 50,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'موفق' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Success' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 50 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (ITEM_STATUS_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('SUCCESS', 'موفق', 'Success', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 50, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_BATCH_ITEM_STATUS t
USING (SELECT 'FAILED' AS ITEM_STATUS_CODE FROM dual) s
ON (t.ITEM_STATUS_CODE = s.ITEM_STATUS_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'ناموفق',
    t.TITLE_EN = 'Failed',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 60,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'ناموفق' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Failed' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 60 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (ITEM_STATUS_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('FAILED', 'ناموفق', 'Failed', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 60, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

-- --------------------------------------------------------------------------
-- REF_DEP_OPEN_PAYMENT_DAY_RULE
-- --------------------------------------------------------------------------
MERGE INTO DPS2.REF_DEP_OPEN_PAYMENT_DAY_RULE t
USING (SELECT 'OPENING_ANNIVERSARY' AS PAYMENT_DAY_RULE_CODE FROM dual) s
ON (t.PAYMENT_DAY_RULE_CODE = s.PAYMENT_DAY_RULE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'سالگرد / روز متناظر افتتاح',
    t.TITLE_EN = 'Opening Anniversary',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 10,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'سالگرد / روز متناظر افتتاح' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Opening Anniversary' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 10 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (PAYMENT_DAY_RULE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('OPENING_ANNIVERSARY', 'سالگرد / روز متناظر افتتاح', 'Opening Anniversary', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 10, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_PAYMENT_DAY_RULE t
USING (SELECT 'MATURITY_DATE' AS PAYMENT_DAY_RULE_CODE FROM dual) s
ON (t.PAYMENT_DAY_RULE_CODE = s.PAYMENT_DAY_RULE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'تاریخ سررسید',
    t.TITLE_EN = 'Maturity Date',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 20,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'تاریخ سررسید' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Maturity Date' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 20 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (PAYMENT_DAY_RULE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('MATURITY_DATE', 'تاریخ سررسید', 'Maturity Date', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 20, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

-- --------------------------------------------------------------------------
-- REF_DEP_OPEN_PARTY_ROLE
-- --------------------------------------------------------------------------
MERGE INTO DPS2.REF_DEP_OPEN_PARTY_ROLE t
USING (SELECT 'OWNER' AS ROLE_CODE FROM dual) s
ON (t.ROLE_CODE = s.ROLE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'مالک',
    t.TITLE_EN = 'Owner',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 10,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'مالک' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Owner' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 10 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (ROLE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('OWNER', 'مالک', 'Owner', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 10, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

-- --------------------------------------------------------------------------
-- REF_DEP_OPEN_MATURITY_ACTION
-- --------------------------------------------------------------------------
MERGE INTO DPS2.REF_DEP_OPEN_MATURITY_ACTION t
USING (SELECT 'RENEW_PRINCIPAL' AS MATURITY_ACTION_CODE FROM dual) s
ON (t.MATURITY_ACTION_CODE = s.MATURITY_ACTION_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'تمدید اصل سپرده',
    t.TITLE_EN = 'Renew Principal',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 10,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'تمدید اصل سپرده' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Renew Principal' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 10 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (MATURITY_ACTION_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('RENEW_PRINCIPAL', 'تمدید اصل سپرده', 'Renew Principal', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 10, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_MATURITY_ACTION t
USING (SELECT 'RENEW_PRINCIPAL_PAY_PROFIT' AS MATURITY_ACTION_CODE FROM dual) s
ON (t.MATURITY_ACTION_CODE = s.MATURITY_ACTION_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'تمدید اصل و پرداخت سود',
    t.TITLE_EN = 'Renew Principal Pay Profit',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 20,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'تمدید اصل و پرداخت سود' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Renew Principal Pay Profit' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 20 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (MATURITY_ACTION_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('RENEW_PRINCIPAL_PAY_PROFIT', 'تمدید اصل و پرداخت سود', 'Renew Principal Pay Profit', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 20, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_MATURITY_ACTION t
USING (SELECT 'PAY_TO_ACCOUNT' AS MATURITY_ACTION_CODE FROM dual) s
ON (t.MATURITY_ACTION_CODE = s.MATURITY_ACTION_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'واریز به حساب مقصد',
    t.TITLE_EN = 'Pay To Account',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 30,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'واریز به حساب مقصد' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Pay To Account' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 30 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (MATURITY_ACTION_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('PAY_TO_ACCOUNT', 'واریز به حساب مقصد', 'Pay To Account', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 30, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_MATURITY_ACTION t
USING (SELECT 'CLOSE_AND_SETTLE' AS MATURITY_ACTION_CODE FROM dual) s
ON (t.MATURITY_ACTION_CODE = s.MATURITY_ACTION_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'بستن و تسویه',
    t.TITLE_EN = 'Close And Settle',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 40,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'بستن و تسویه' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Close And Settle' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 40 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (MATURITY_ACTION_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('CLOSE_AND_SETTLE', 'بستن و تسویه', 'Close And Settle', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 40, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

-- --------------------------------------------------------------------------
-- REF_DEP_OPEN_OWNERSHIP_TYPE
-- --------------------------------------------------------------------------
MERGE INTO DPS2.REF_DEP_OPEN_OWNERSHIP_TYPE t
USING (SELECT 'INDIVIDUAL' AS OWNERSHIP_TYPE_CODE FROM dual) s
ON (t.OWNERSHIP_TYPE_CODE = s.OWNERSHIP_TYPE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'انفرادی',
    t.TITLE_EN = 'Individual',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 10,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'انفرادی' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Individual' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 10 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (OWNERSHIP_TYPE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('INDIVIDUAL', 'انفرادی', 'Individual', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 10, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_OWNERSHIP_TYPE t
USING (SELECT 'JOINT' AS OWNERSHIP_TYPE_CODE FROM dual) s
ON (t.OWNERSHIP_TYPE_CODE = s.OWNERSHIP_TYPE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'مشترک',
    t.TITLE_EN = 'Joint',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 20,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'مشترک' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Joint' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 20 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (OWNERSHIP_TYPE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('JOINT', 'مشترک', 'Joint', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 20, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

-- --------------------------------------------------------------------------
-- REF_DEP_OPEN_REQUEST_TYPE
-- --------------------------------------------------------------------------
MERGE INTO DPS2.REF_DEP_OPEN_REQUEST_TYPE t
USING (SELECT 'CUSTOMER_REQUEST' AS REQUEST_TYPE_CODE FROM dual) s
ON (t.REQUEST_TYPE_CODE = s.REQUEST_TYPE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'درخواست مشتری',
    t.TITLE_EN = 'Customer Request',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 10,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'درخواست مشتری' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Customer Request' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 10 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (REQUEST_TYPE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('CUSTOMER_REQUEST', 'درخواست مشتری', 'Customer Request', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 10, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_REQUEST_TYPE t
USING (SELECT 'STAFF_ASSISTED' AS REQUEST_TYPE_CODE FROM dual) s
ON (t.REQUEST_TYPE_CODE = s.REQUEST_TYPE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'ثبت با کمک کاربر شعبه',
    t.TITLE_EN = 'Staff Assisted',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 20,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'ثبت با کمک کاربر شعبه' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Staff Assisted' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 20 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (REQUEST_TYPE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('STAFF_ASSISTED', 'ثبت با کمک کاربر شعبه', 'Staff Assisted', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 20, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_REQUEST_TYPE t
USING (SELECT 'BULK' AS REQUEST_TYPE_CODE FROM dual) s
ON (t.REQUEST_TYPE_CODE = s.REQUEST_TYPE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'گروهی',
    t.TITLE_EN = 'Bulk',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 30,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'گروهی' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Bulk' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 30 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (REQUEST_TYPE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('BULK', 'گروهی', 'Bulk', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 30, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

-- --------------------------------------------------------------------------
-- REF_DEP_OPEN_AUTHORITY_LEVEL
-- --------------------------------------------------------------------------
MERGE INTO DPS2.REF_DEP_OPEN_AUTHORITY_LEVEL t
USING (SELECT 'BRANCH_MANAGER' AS AUTHORITY_LEVEL_CODE FROM dual) s
ON (t.AUTHORITY_LEVEL_CODE = s.AUTHORITY_LEVEL_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'مدیر شعبه',
    t.TITLE_EN = 'Branch Manager',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 10,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'مدیر شعبه' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Branch Manager' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 10 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (AUTHORITY_LEVEL_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('BRANCH_MANAGER', 'مدیر شعبه', 'Branch Manager', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 10, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_AUTHORITY_LEVEL t
USING (SELECT 'REGIONAL_MANAGER' AS AUTHORITY_LEVEL_CODE FROM dual) s
ON (t.AUTHORITY_LEVEL_CODE = s.AUTHORITY_LEVEL_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'مدیریت منطقه',
    t.TITLE_EN = 'Regional Manager',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 20,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'مدیریت منطقه' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Regional Manager' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 20 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (AUTHORITY_LEVEL_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('REGIONAL_MANAGER', 'مدیریت منطقه', 'Regional Manager', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 20, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_AUTHORITY_LEVEL t
USING (SELECT 'HEAD_OFFICE' AS AUTHORITY_LEVEL_CODE FROM dual) s
ON (t.AUTHORITY_LEVEL_CODE = s.AUTHORITY_LEVEL_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'اداره مرکزی',
    t.TITLE_EN = 'Head Office',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 30,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'اداره مرکزی' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Head Office' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 30 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (AUTHORITY_LEVEL_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('HEAD_OFFICE', 'اداره مرکزی', 'Head Office', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 30, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

-- --------------------------------------------------------------------------
-- REF_DEP_OPEN_DELEGATION_TYPE
-- --------------------------------------------------------------------------
MERGE INTO DPS2.REF_DEP_OPEN_DELEGATION_TYPE t
USING (SELECT 'POA' AS DELEGATION_TYPE_CODE FROM dual) s
ON (t.DELEGATION_TYPE_CODE = s.DELEGATION_TYPE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'وکالت',
    t.TITLE_EN = 'POA',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 10,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'وکالت' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'POA' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 10 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (DELEGATION_TYPE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('POA', 'وکالت', 'POA', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 10, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_DELEGATION_TYPE t
USING (SELECT 'GUARDIAN' AS DELEGATION_TYPE_CODE FROM dual) s
ON (t.DELEGATION_TYPE_CODE = s.DELEGATION_TYPE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'قیم',
    t.TITLE_EN = 'Guardian',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 20,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'قیم' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Guardian' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 20 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (DELEGATION_TYPE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('GUARDIAN', 'قیم', 'Guardian', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 20, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_DELEGATION_TYPE t
USING (SELECT 'LEGAL_REPRESENTATIVE' AS DELEGATION_TYPE_CODE FROM dual) s
ON (t.DELEGATION_TYPE_CODE = s.DELEGATION_TYPE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'نماینده قانونی',
    t.TITLE_EN = 'Legal Representative',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 30,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'نماینده قانونی' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Legal Representative' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 30 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (DELEGATION_TYPE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('LEGAL_REPRESENTATIVE', 'نماینده قانونی', 'Legal Representative', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 30, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

-- --------------------------------------------------------------------------
-- REF_DEP_OPEN_DECISION
-- --------------------------------------------------------------------------
MERGE INTO DPS2.REF_DEP_OPEN_DECISION t
USING (SELECT 'APPROVE' AS DECISION_CODE FROM dual) s
ON (t.DECISION_CODE = s.DECISION_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'تأیید',
    t.TITLE_EN = 'Approve',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 10,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'تأیید' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Approve' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 10 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (DECISION_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('APPROVE', 'تأیید', 'Approve', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 10, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_DECISION t
USING (SELECT 'REJECT' AS DECISION_CODE FROM dual) s
ON (t.DECISION_CODE = s.DECISION_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'رد',
    t.TITLE_EN = 'Reject',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 20,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'رد' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Reject' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 20 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (DECISION_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('REJECT', 'رد', 'Reject', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 20, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_DECISION t
USING (SELECT 'REFER' AS DECISION_CODE FROM dual) s
ON (t.DECISION_CODE = s.DECISION_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'ارجاع',
    t.TITLE_EN = 'Refer',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 30,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'ارجاع' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Refer' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 30 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (DECISION_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('REFER', 'ارجاع', 'Refer', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 30, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_DECISION t
USING (SELECT 'RETURN' AS DECISION_CODE FROM dual) s
ON (t.DECISION_CODE = s.DECISION_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'بازگشت برای اصلاح',
    t.TITLE_EN = 'Return',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 40,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'بازگشت برای اصلاح' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Return' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 40 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (DECISION_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('RETURN', 'بازگشت برای اصلاح', 'Return', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 40, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

-- --------------------------------------------------------------------------
-- REF_DEP_OPEN_AUDIT_EVENT_TYPE
-- Source authority: Phase 5 migration / DepositOpeningAuditService.
-- --------------------------------------------------------------------------
MERGE INTO DPS2.REF_DEP_OPEN_AUDIT_EVENT_TYPE t
USING (
  SELECT 'CREATE' code, 'ایجاد پرونده' fa, 'Create opening' en, 10 ord FROM DUAL UNION ALL
  SELECT 'CHANGE_REQUESTED', 'درخواست تغییر', 'Change requested', 20 FROM DUAL UNION ALL
  SELECT 'CHANGE_APPROVED', 'تأیید تغییر', 'Change approved', 30 FROM DUAL UNION ALL
  SELECT 'CHANGE_REJECTED', 'رد تغییر', 'Change rejected', 40 FROM DUAL UNION ALL
  SELECT 'CHANGE_APPLIED', 'اعمال تغییر', 'Change applied', 50 FROM DUAL UNION ALL
  SELECT 'ACCOUNT_LINKED', 'اتصال حساب ایجادشده', 'Created account linked', 60 FROM DUAL UNION ALL
  SELECT 'STATUS_CHANGE', 'تغییر وضعیت', 'Status changed', 70 FROM DUAL
) s
ON (t.EVENT_TYPE_CODE=s.code)
WHEN MATCHED THEN UPDATE SET
  t.TITLE_FA=s.fa, t.TITLE_EN=s.en,
  t.DESCRIPTION='Phase 5 controlled Opening audit event',
  t.DISPLAY_ORDER=s.ord, t.IS_ACTIVE=1,
  t.UPDATED_AT=SYSTIMESTAMP, t.UPDATED_BY='DEP_OPEN_REF_SEED',
  t.RECORD_VERSION=NVL(t.RECORD_VERSION,0)+1
WHERE NVL(t.TITLE_FA,CHR(0))<>s.fa OR NVL(t.TITLE_EN,CHR(0))<>s.en
   OR NVL(t.DESCRIPTION,CHR(0))<>'Phase 5 controlled Opening audit event'
   OR NVL(t.DISPLAY_ORDER,-999999999)<>s.ord OR NVL(t.IS_ACTIVE,-1)<>1
WHEN NOT MATCHED THEN INSERT
  (EVENT_TYPE_CODE,TITLE_FA,TITLE_EN,DESCRIPTION,DISPLAY_ORDER,IS_ACTIVE,CREATED_AT,CREATED_BY,RECORD_VERSION)
VALUES
  (s.code,s.fa,s.en,'Phase 5 controlled Opening audit event',s.ord,1,SYSTIMESTAMP,'DEP_OPEN_REF_SEED',1);

-- --------------------------------------------------------------------------
-- REF_DEP_OPEN_PROFIT_CALC_METHOD
-- --------------------------------------------------------------------------
MERGE INTO DPS2.REF_DEP_OPEN_PROFIT_CALC_METHOD t
USING (SELECT 'PERCENTAGE' AS CALCULATION_METHOD_CODE FROM dual) s
ON (t.CALCULATION_METHOD_CODE = s.CALCULATION_METHOD_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'درصدی',
    t.TITLE_EN = 'Percentage',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 10,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'درصدی' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Percentage' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 10 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (CALCULATION_METHOD_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('PERCENTAGE', 'درصدی', 'Percentage', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 10, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

-- --------------------------------------------------------------------------
-- REF_DEP_OPEN_BATCH_ERROR_STAGE
-- --------------------------------------------------------------------------
MERGE INTO DPS2.REF_DEP_OPEN_BATCH_ERROR_STAGE t
USING (SELECT 'VALIDATION' AS ERROR_STAGE_CODE FROM dual) s
ON (t.ERROR_STAGE_CODE = s.ERROR_STAGE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'اعتبارسنجی',
    t.TITLE_EN = 'Validation',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 10,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'اعتبارسنجی' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Validation' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 10 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (ERROR_STAGE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('VALIDATION', 'اعتبارسنجی', 'Validation', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 10, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_BATCH_ERROR_STAGE t
USING (SELECT 'PROCESSING' AS ERROR_STAGE_CODE FROM dual) s
ON (t.ERROR_STAGE_CODE = s.ERROR_STAGE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'در حال پردازش',
    t.TITLE_EN = 'Processing',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 20,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'در حال پردازش' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Processing' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 20 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (ERROR_STAGE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('PROCESSING', 'در حال پردازش', 'Processing', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 20, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_BATCH_ERROR_STAGE t
USING (SELECT 'POSTING' AS ERROR_STAGE_CODE FROM dual) s
ON (t.ERROR_STAGE_CODE = s.ERROR_STAGE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'ثبت مالی',
    t.TITLE_EN = 'Posting',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 30,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'ثبت مالی' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Posting' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 30 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (ERROR_STAGE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('POSTING', 'ثبت مالی', 'Posting', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 30, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

-- --------------------------------------------------------------------------
-- REF_DEP_OPEN_CHANNEL
-- --------------------------------------------------------------------------
MERGE INTO DPS2.REF_DEP_OPEN_CHANNEL t
USING (SELECT 'BRANCH' AS CHANNEL_CODE FROM dual) s
ON (t.CHANNEL_CODE = s.CHANNEL_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'شعبه',
    t.TITLE_EN = 'Branch',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 10,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'شعبه' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Branch' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 10 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (CHANNEL_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('BRANCH', 'شعبه', 'Branch', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 10, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_CHANNEL t
USING (SELECT 'INTERNET' AS CHANNEL_CODE FROM dual) s
ON (t.CHANNEL_CODE = s.CHANNEL_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'اینترنت‌بانک',
    t.TITLE_EN = 'Internet',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 20,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'اینترنت‌بانک' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Internet' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 20 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (CHANNEL_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('INTERNET', 'اینترنت‌بانک', 'Internet', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 20, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_CHANNEL t
USING (SELECT 'MOBILE' AS CHANNEL_CODE FROM dual) s
ON (t.CHANNEL_CODE = s.CHANNEL_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'موبایل‌بانک',
    t.TITLE_EN = 'Mobile',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 30,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'موبایل‌بانک' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Mobile' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 30 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (CHANNEL_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('MOBILE', 'موبایل‌بانک', 'Mobile', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 30, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_CHANNEL t
USING (SELECT 'API' AS CHANNEL_CODE FROM dual) s
ON (t.CHANNEL_CODE = s.CHANNEL_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'رابط برنامه‌نویسی API',
    t.TITLE_EN = 'API',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 40,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'رابط برنامه‌نویسی API' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'API' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 40 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (CHANNEL_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('API', 'رابط برنامه‌نویسی API', 'API', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 40, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_CHANNEL t
USING (SELECT 'SMS' AS CHANNEL_CODE FROM dual) s
ON (t.CHANNEL_CODE = s.CHANNEL_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'پیامک',
    t.TITLE_EN = 'SMS',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 50,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'پیامک' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'SMS' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 50 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (CHANNEL_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('SMS', 'پیامک', 'SMS', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 50, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_CHANNEL t
USING (SELECT 'EMAIL' AS CHANNEL_CODE FROM dual) s
ON (t.CHANNEL_CODE = s.CHANNEL_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'پست الکترونیکی',
    t.TITLE_EN = 'Email',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 60,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'پست الکترونیکی' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Email' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 60 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (CHANNEL_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('EMAIL', 'پست الکترونیکی', 'Email', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 60, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

-- --------------------------------------------------------------------------
-- REF_DEP_OPEN_ACCEPTANCE_STATUS
-- --------------------------------------------------------------------------
MERGE INTO DPS2.REF_DEP_OPEN_ACCEPTANCE_STATUS t
USING (SELECT 'ACCEPTED' AS ACCEPTANCE_STATUS_CODE FROM dual) s
ON (t.ACCEPTANCE_STATUS_CODE = s.ACCEPTANCE_STATUS_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'پذیرفته‌شده',
    t.TITLE_EN = 'Accepted',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 10,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'پذیرفته‌شده' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Accepted' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 10 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (ACCEPTANCE_STATUS_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('ACCEPTED', 'پذیرفته‌شده', 'Accepted', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 10, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_ACCEPTANCE_STATUS t
USING (SELECT 'DECLINED' AS ACCEPTANCE_STATUS_CODE FROM dual) s
ON (t.ACCEPTANCE_STATUS_CODE = s.ACCEPTANCE_STATUS_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'نپذیرفته / رد پذیرش',
    t.TITLE_EN = 'Declined',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 20,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'نپذیرفته / رد پذیرش' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Declined' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 20 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (ACCEPTANCE_STATUS_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('DECLINED', 'نپذیرفته / رد پذیرش', 'Declined', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 20, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_ACCEPTANCE_STATUS t
USING (SELECT 'REVOKED' AS ACCEPTANCE_STATUS_CODE FROM dual) s
ON (t.ACCEPTANCE_STATUS_CODE = s.ACCEPTANCE_STATUS_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'لغو پذیرش',
    t.TITLE_EN = 'Revoked',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 30,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'لغو پذیرش' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Revoked' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 30 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (ACCEPTANCE_STATUS_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('REVOKED', 'لغو پذیرش', 'Revoked', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 30, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

-- --------------------------------------------------------------------------
-- REF_DEP_OPEN_DOCUMENT_TYPE
-- --------------------------------------------------------------------------
MERGE INTO DPS2.REF_DEP_OPEN_DOCUMENT_TYPE t
USING (SELECT 'IDENTITY' AS DOCUMENT_TYPE_CODE FROM dual) s
ON (t.DOCUMENT_TYPE_CODE = s.DOCUMENT_TYPE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'مدرک هویتی',
    t.TITLE_EN = 'Identity',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 10,
    t.IS_ACTIVE = 1,
    t.DEFAULT_REQUIRED_FLAG = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'مدرک هویتی' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Identity' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 10 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1 OR
       NVL(t.DEFAULT_REQUIRED_FLAG, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (DOCUMENT_TYPE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, DEFAULT_REQUIRED_FLAG, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('IDENTITY', 'مدرک هویتی', 'Identity', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 10, 1, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_DOCUMENT_TYPE t
USING (SELECT 'ADDRESS' AS DOCUMENT_TYPE_CODE FROM dual) s
ON (t.DOCUMENT_TYPE_CODE = s.DOCUMENT_TYPE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'مدرک نشانی',
    t.TITLE_EN = 'Address',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 20,
    t.IS_ACTIVE = 1,
    t.DEFAULT_REQUIRED_FLAG = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'مدرک نشانی' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Address' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 20 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1 OR
       NVL(t.DEFAULT_REQUIRED_FLAG, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (DOCUMENT_TYPE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, DEFAULT_REQUIRED_FLAG, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('ADDRESS', 'مدرک نشانی', 'Address', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 20, 1, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_DOCUMENT_TYPE t
USING (SELECT 'SIGNATURE' AS DOCUMENT_TYPE_CODE FROM dual) s
ON (t.DOCUMENT_TYPE_CODE = s.DOCUMENT_TYPE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'نمونه / مدرک امضا',
    t.TITLE_EN = 'Signature',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 30,
    t.IS_ACTIVE = 1,
    t.DEFAULT_REQUIRED_FLAG = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'نمونه / مدرک امضا' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Signature' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 30 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1 OR
       NVL(t.DEFAULT_REQUIRED_FLAG, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (DOCUMENT_TYPE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, DEFAULT_REQUIRED_FLAG, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('SIGNATURE', 'نمونه / مدرک امضا', 'Signature', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 30, 1, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_DOCUMENT_TYPE t
USING (SELECT 'BUSINESS_DOC' AS DOCUMENT_TYPE_CODE FROM dual) s
ON (t.DOCUMENT_TYPE_CODE = s.DOCUMENT_TYPE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'مدرک کسب‌وکار',
    t.TITLE_EN = 'Business Doc',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 40,
    t.IS_ACTIVE = 1,
    t.DEFAULT_REQUIRED_FLAG = 0,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'مدرک کسب‌وکار' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Business Doc' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 40 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1 OR
       NVL(t.DEFAULT_REQUIRED_FLAG, -999999999) <> 0)
WHEN NOT MATCHED THEN
  INSERT (DOCUMENT_TYPE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, DEFAULT_REQUIRED_FLAG, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('BUSINESS_DOC', 'مدرک کسب‌وکار', 'Business Doc', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 40, 1, 0, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

-- --------------------------------------------------------------------------
-- REF_DEP_OPEN_HOLIDAY_ADJUSTMENT
-- --------------------------------------------------------------------------
MERGE INTO DPS2.REF_DEP_OPEN_HOLIDAY_ADJUSTMENT t
USING (SELECT 'NEXT_BUSINESS_DAY' AS HOLIDAY_ADJUSTMENT_CODE FROM dual) s
ON (t.HOLIDAY_ADJUSTMENT_CODE = s.HOLIDAY_ADJUSTMENT_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'روز کاری بعد',
    t.TITLE_EN = 'Next Business Day',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 10,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'روز کاری بعد' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Next Business Day' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 10 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (HOLIDAY_ADJUSTMENT_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('NEXT_BUSINESS_DAY', 'روز کاری بعد', 'Next Business Day', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 10, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

-- --------------------------------------------------------------------------
-- REF_DEP_OPEN_BENEFICIARY_TYPE
-- --------------------------------------------------------------------------
MERGE INTO DPS2.REF_DEP_OPEN_BENEFICIARY_TYPE t
USING (SELECT 'NOMINEE' AS BENEFICIARY_TYPE_CODE FROM dual) s
ON (t.BENEFICIARY_TYPE_CODE = s.BENEFICIARY_TYPE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'ذی‌نفع معرفی‌شده',
    t.TITLE_EN = 'Nominee',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 10,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'ذی‌نفع معرفی‌شده' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Nominee' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 10 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (BENEFICIARY_TYPE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('NOMINEE', 'ذی‌نفع معرفی‌شده', 'Nominee', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 10, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_BENEFICIARY_TYPE t
USING (SELECT 'BENEFICIAL_OWNER' AS BENEFICIARY_TYPE_CODE FROM dual) s
ON (t.BENEFICIARY_TYPE_CODE = s.BENEFICIARY_TYPE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'ذی‌نفع واقعی',
    t.TITLE_EN = 'Beneficial Owner',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 20,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'ذی‌نفع واقعی' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Beneficial Owner' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 20 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (BENEFICIARY_TYPE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('BENEFICIAL_OWNER', 'ذی‌نفع واقعی', 'Beneficial Owner', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 20, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_BENEFICIARY_TYPE t
USING (SELECT 'PAYMENT_BENEFICIARY' AS BENEFICIARY_TYPE_CODE FROM dual) s
ON (t.BENEFICIARY_TYPE_CODE = s.BENEFICIARY_TYPE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'ذی‌نفع پرداخت',
    t.TITLE_EN = 'Payment Beneficiary',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 30,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'ذی‌نفع پرداخت' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Payment Beneficiary' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 30 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (BENEFICIARY_TYPE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('PAYMENT_BENEFICIARY', 'ذی‌نفع پرداخت', 'Payment Beneficiary', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 30, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

-- --------------------------------------------------------------------------
-- REF_DEP_OPEN_PURPOSE
-- --------------------------------------------------------------------------
MERGE INTO DPS2.REF_DEP_OPEN_PURPOSE t
USING (SELECT 'SAVING' AS PURPOSE_CODE FROM dual) s
ON (t.PURPOSE_CODE = s.PURPOSE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'پس‌انداز',
    t.TITLE_EN = 'Saving',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 10,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'پس‌انداز' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Saving' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 10 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (PURPOSE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('SAVING', 'پس‌انداز', 'Saving', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 10, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_PURPOSE t
USING (SELECT 'DAILY_BANKING' AS PURPOSE_CODE FROM dual) s
ON (t.PURPOSE_CODE = s.PURPOSE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'عملیات روزمره بانکی',
    t.TITLE_EN = 'Daily Banking',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 20,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'عملیات روزمره بانکی' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Daily Banking' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 20 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (PURPOSE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('DAILY_BANKING', 'عملیات روزمره بانکی', 'Daily Banking', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 20, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_PURPOSE t
USING (SELECT 'BUSINESS' AS PURPOSE_CODE FROM dual) s
ON (t.PURPOSE_CODE = s.PURPOSE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'فعالیت تجاری',
    t.TITLE_EN = 'Business',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 30,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'فعالیت تجاری' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Business' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 30 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (PURPOSE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('BUSINESS', 'فعالیت تجاری', 'Business', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 30, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_PURPOSE t
USING (SELECT 'INVESTMENT' AS PURPOSE_CODE FROM dual) s
ON (t.PURPOSE_CODE = s.PURPOSE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'سرمایه‌گذاری',
    t.TITLE_EN = 'Investment',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 40,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'سرمایه‌گذاری' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Investment' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 40 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (PURPOSE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('INVESTMENT', 'سرمایه‌گذاری', 'Investment', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 40, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

-- --------------------------------------------------------------------------
-- REF_DEP_OPEN_INSTRUCTION_SOURCE
-- --------------------------------------------------------------------------
MERGE INTO DPS2.REF_DEP_OPEN_INSTRUCTION_SOURCE t
USING (SELECT 'CUSTOMER' AS INSTRUCTION_SOURCE_CODE FROM dual) s
ON (t.INSTRUCTION_SOURCE_CODE = s.INSTRUCTION_SOURCE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'مشتری',
    t.TITLE_EN = 'Customer',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 10,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'مشتری' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Customer' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 10 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (INSTRUCTION_SOURCE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('CUSTOMER', 'مشتری', 'Customer', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 10, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_INSTRUCTION_SOURCE t
USING (SELECT 'PRODUCT_DEFAULT' AS INSTRUCTION_SOURCE_CODE FROM dual) s
ON (t.INSTRUCTION_SOURCE_CODE = s.INSTRUCTION_SOURCE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'پیش‌فرض محصول',
    t.TITLE_EN = 'Product Default',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 20,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'پیش‌فرض محصول' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Product Default' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 20 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (INSTRUCTION_SOURCE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('PRODUCT_DEFAULT', 'پیش‌فرض محصول', 'Product Default', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 20, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

-- --------------------------------------------------------------------------
-- REF_DEP_OPEN_CHANGE_TYPE
-- Includes one or more values required/mentioned elsewhere in the same XMI.
-- --------------------------------------------------------------------------
MERGE INTO DPS2.REF_DEP_OPEN_CHANGE_TYPE t
USING (SELECT 'CUSTOMER_CORRECTION' AS CHANGE_TYPE_CODE FROM dual) s
ON (t.CHANGE_TYPE_CODE = s.CHANGE_TYPE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'اصلاح به درخواست مشتری',
    t.TITLE_EN = 'Customer Correction',
    t.DESCRIPTION = 'نمونه صریح ذکرشده در XMI برای نوع اصلاح؛ کاتالوگ نهایی باید توسط Data Governance تکمیل شود.',
    t.DISPLAY_ORDER = 10,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'اصلاح به درخواست مشتری' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Customer Correction' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'نمونه صریح ذکرشده در XMI برای نوع اصلاح؛ کاتالوگ نهایی باید توسط Data Governance تکمیل شود.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 10 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (CHANGE_TYPE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('CUSTOMER_CORRECTION', 'اصلاح به درخواست مشتری', 'Customer Correction', 'نمونه صریح ذکرشده در XMI برای نوع اصلاح؛ کاتالوگ نهایی باید توسط Data Governance تکمیل شود.', 10, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_CHANGE_TYPE t
USING (SELECT 'COMPLIANCE_CORRECTION' AS CHANGE_TYPE_CODE FROM dual) s
ON (t.CHANGE_TYPE_CODE = s.CHANGE_TYPE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'اصلاح ناشی از کنترل تطبیق',
    t.TITLE_EN = 'Compliance Correction',
    t.DESCRIPTION = 'نمونه صریح ذکرشده در XMI برای نوع اصلاح؛ کاتالوگ نهایی باید توسط Data Governance تکمیل شود.',
    t.DISPLAY_ORDER = 20,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'اصلاح ناشی از کنترل تطبیق' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Compliance Correction' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'نمونه صریح ذکرشده در XMI برای نوع اصلاح؛ کاتالوگ نهایی باید توسط Data Governance تکمیل شود.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 20 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (CHANGE_TYPE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('COMPLIANCE_CORRECTION', 'اصلاح ناشی از کنترل تطبیق', 'Compliance Correction', 'نمونه صریح ذکرشده در XMI برای نوع اصلاح؛ کاتالوگ نهایی باید توسط Data Governance تکمیل شود.', 20, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

-- --------------------------------------------------------------------------
-- REF_DEP_OPEN_TAX_RESIDENCY
-- --------------------------------------------------------------------------
MERGE INTO DPS2.REF_DEP_OPEN_TAX_RESIDENCY t
USING (SELECT 'IR' AS TAX_RESIDENCY_CODE FROM dual) s
ON (t.TAX_RESIDENCY_CODE = s.TAX_RESIDENCY_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'مقیم ایران',
    t.TITLE_EN = 'IR',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 10,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'مقیم ایران' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'IR' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 10 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (TAX_RESIDENCY_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('IR', 'مقیم ایران', 'IR', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 10, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_TAX_RESIDENCY t
USING (SELECT 'NON_RESIDENT' AS TAX_RESIDENCY_CODE FROM dual) s
ON (t.TAX_RESIDENCY_CODE = s.TAX_RESIDENCY_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'غیرمقیم',
    t.TITLE_EN = 'Non Resident',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 20,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'غیرمقیم' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Non Resident' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 20 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (TAX_RESIDENCY_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('NON_RESIDENT', 'غیرمقیم', 'Non Resident', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 20, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

-- --------------------------------------------------------------------------
-- REF_DEP_OPEN_CHANGE_STATUS
-- Includes one or more values required/mentioned elsewhere in the same XMI.
-- --------------------------------------------------------------------------
MERGE INTO DPS2.REF_DEP_OPEN_CHANGE_STATUS t
USING (SELECT 'DRAFT' AS CHANGE_STATUS_CODE FROM dual) s
ON (t.CHANGE_STATUS_CODE = s.CHANGE_STATUS_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'پیش‌نویس',
    t.TITLE_EN = 'Draft',
    t.DESCRIPTION = 'مقدار موردنیاز/ذکرشده در Default، Constraint یا شرح عملیاتی همین XMI برای حفظ سازگاری مدل.',
    t.DISPLAY_ORDER = 10,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'پیش‌نویس' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Draft' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'مقدار موردنیاز/ذکرشده در Default، Constraint یا شرح عملیاتی همین XMI برای حفظ سازگاری مدل.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 10 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (CHANGE_STATUS_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('DRAFT', 'پیش‌نویس', 'Draft', 'مقدار موردنیاز/ذکرشده در Default، Constraint یا شرح عملیاتی همین XMI برای حفظ سازگاری مدل.', 10, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

-- --------------------------------------------------------------------------
-- REF_DEP_OPEN_CHANGE_REASON
-- Source authority: XMI + application validation explicitly define OTHER and
-- require CHANGE_REASON_NOTE when OTHER is selected. No broader bank taxonomy
-- is invented here.
-- --------------------------------------------------------------------------
MERGE INTO DPS2.REF_DEP_OPEN_CHANGE_REASON t
USING (SELECT 'OTHER' code, 'سایر' fa, 'Other' en, 10 ord FROM DUAL) s
ON (t.CHANGE_REASON_CODE=s.code)
WHEN MATCHED THEN UPDATE SET
  t.TITLE_FA=s.fa, t.TITLE_EN=s.en,
  t.DESCRIPTION='علت عمومی برای موارد خارج از کاتالوگ؛ تکمیل CHANGE_REASON_NOTE الزامی است.',
  t.DISPLAY_ORDER=s.ord, t.IS_ACTIVE=1,
  t.UPDATED_AT=SYSTIMESTAMP, t.UPDATED_BY='DEP_OPEN_REF_SEED',
  t.RECORD_VERSION=NVL(t.RECORD_VERSION,0)+1
WHERE NVL(t.TITLE_FA,CHR(0))<>s.fa OR NVL(t.TITLE_EN,CHR(0))<>s.en
   OR NVL(t.DESCRIPTION,CHR(0))<>'علت عمومی برای موارد خارج از کاتالوگ؛ تکمیل CHANGE_REASON_NOTE الزامی است.'
   OR NVL(t.DISPLAY_ORDER,-999999999)<>s.ord OR NVL(t.IS_ACTIVE,-1)<>1
WHEN NOT MATCHED THEN INSERT
  (CHANGE_REASON_CODE,TITLE_FA,TITLE_EN,DESCRIPTION,DISPLAY_ORDER,IS_ACTIVE,CREATED_AT,CREATED_BY,RECORD_VERSION)
VALUES
  (s.code,s.fa,s.en,'علت عمومی برای موارد خارج از کاتالوگ؛ تکمیل CHANGE_REASON_NOTE الزامی است.',s.ord,1,SYSTIMESTAMP,'DEP_OPEN_REF_SEED',1);

-- --------------------------------------------------------------------------
-- REF_DEP_OPEN_FUNDING_METHOD
-- --------------------------------------------------------------------------
MERGE INTO DPS2.REF_DEP_OPEN_FUNDING_METHOD t
USING (SELECT 'TRANSFER' AS FUNDING_METHOD_CODE FROM dual) s
ON (t.FUNDING_METHOD_CODE = s.FUNDING_METHOD_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'انتقال وجه',
    t.TITLE_EN = 'Transfer',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 10,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'انتقال وجه' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Transfer' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 10 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (FUNDING_METHOD_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('TRANSFER', 'انتقال وجه', 'Transfer', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 10, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_FUNDING_METHOD t
USING (SELECT 'CASH' AS FUNDING_METHOD_CODE FROM dual) s
ON (t.FUNDING_METHOD_CODE = s.FUNDING_METHOD_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'نقدی',
    t.TITLE_EN = 'Cash',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 20,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'نقدی' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Cash' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 20 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (FUNDING_METHOD_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('CASH', 'نقدی', 'Cash', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 20, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_FUNDING_METHOD t
USING (SELECT 'CARD' AS FUNDING_METHOD_CODE FROM dual) s
ON (t.FUNDING_METHOD_CODE = s.FUNDING_METHOD_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'کارت',
    t.TITLE_EN = 'Card',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 30,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'کارت' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Card' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 30 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (FUNDING_METHOD_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('CARD', 'کارت', 'Card', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 30, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_FUNDING_METHOD t
USING (SELECT 'INTERNAL' AS FUNDING_METHOD_CODE FROM dual) s
ON (t.FUNDING_METHOD_CODE = s.FUNDING_METHOD_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'انتقال داخلی',
    t.TITLE_EN = 'Internal',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 40,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'انتقال داخلی' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Internal' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 40 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (FUNDING_METHOD_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('INTERNAL', 'انتقال داخلی', 'Internal', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 40, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

-- --------------------------------------------------------------------------
-- REF_DEP_OPEN_WITHDRAWAL_MEDIA
-- --------------------------------------------------------------------------
MERGE INTO DPS2.REF_DEP_OPEN_WITHDRAWAL_MEDIA t
USING (SELECT 'CASH' AS WITHDRAWAL_MEDIA_CODE FROM dual) s
ON (t.WITHDRAWAL_MEDIA_CODE = s.WITHDRAWAL_MEDIA_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'نقدی',
    t.TITLE_EN = 'Cash',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 10,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'نقدی' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Cash' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 10 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (WITHDRAWAL_MEDIA_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('CASH', 'نقدی', 'Cash', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 10, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_WITHDRAWAL_MEDIA t
USING (SELECT 'CARD' AS WITHDRAWAL_MEDIA_CODE FROM dual) s
ON (t.WITHDRAWAL_MEDIA_CODE = s.WITHDRAWAL_MEDIA_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'کارت',
    t.TITLE_EN = 'Card',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 20,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'کارت' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Card' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 20 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (WITHDRAWAL_MEDIA_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('CARD', 'کارت', 'Card', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 20, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_WITHDRAWAL_MEDIA t
USING (SELECT 'CHEQUE' AS WITHDRAWAL_MEDIA_CODE FROM dual) s
ON (t.WITHDRAWAL_MEDIA_CODE = s.WITHDRAWAL_MEDIA_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'عملیات چک',
    t.TITLE_EN = 'Cheque',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 30,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'عملیات چک' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Cheque' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 30 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (WITHDRAWAL_MEDIA_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('CHEQUE', 'عملیات چک', 'Cheque', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 30, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_WITHDRAWAL_MEDIA t
USING (SELECT 'TRANSFER' AS WITHDRAWAL_MEDIA_CODE FROM dual) s
ON (t.WITHDRAWAL_MEDIA_CODE = s.WITHDRAWAL_MEDIA_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'انتقال وجه',
    t.TITLE_EN = 'Transfer',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 40,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'انتقال وجه' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Transfer' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 40 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (WITHDRAWAL_MEDIA_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('TRANSFER', 'انتقال وجه', 'Transfer', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 40, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

-- --------------------------------------------------------------------------
-- REF_DEP_OPEN_DAY_COUNT_BASIS
-- --------------------------------------------------------------------------
MERGE INTO DPS2.REF_DEP_OPEN_DAY_COUNT_BASIS t
USING (SELECT 'ACT_365' AS DAY_COUNT_BASIS_CODE FROM dual) s
ON (t.DAY_COUNT_BASIS_CODE = s.DAY_COUNT_BASIS_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'روز واقعی / ۳۶۵',
    t.TITLE_EN = 'ACT 365',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 10,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'روز واقعی / ۳۶۵' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'ACT 365' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 10 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (DAY_COUNT_BASIS_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('ACT_365', 'روز واقعی / ۳۶۵', 'ACT 365', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 10, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

-- --------------------------------------------------------------------------
-- REF_DEP_OPEN_TERM_UNIT
-- --------------------------------------------------------------------------
MERGE INTO DPS2.REF_DEP_OPEN_TERM_UNIT t
USING (SELECT 'DAY' AS TERM_UNIT_CODE FROM dual) s
ON (t.TERM_UNIT_CODE = s.TERM_UNIT_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'روز',
    t.TITLE_EN = 'Day',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 10,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'روز' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Day' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 10 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (TERM_UNIT_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('DAY', 'روز', 'Day', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 10, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_TERM_UNIT t
USING (SELECT 'MONTH' AS TERM_UNIT_CODE FROM dual) s
ON (t.TERM_UNIT_CODE = s.TERM_UNIT_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'ماه',
    t.TITLE_EN = 'Month',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 20,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'ماه' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Month' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 20 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (TERM_UNIT_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('MONTH', 'ماه', 'Month', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 20, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_TERM_UNIT t
USING (SELECT 'YEAR' AS TERM_UNIT_CODE FROM dual) s
ON (t.TERM_UNIT_CODE = s.TERM_UNIT_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'سال',
    t.TITLE_EN = 'Year',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 30,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'سال' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Year' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 30 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (TERM_UNIT_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('YEAR', 'سال', 'Year', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 30, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

-- --------------------------------------------------------------------------
-- REF_DEP_OPEN_CHECK_PHASE
-- --------------------------------------------------------------------------
MERGE INTO DPS2.REF_DEP_OPEN_CHECK_PHASE t
USING (SELECT 'PRE_SUBMISSION' AS CHECK_PHASE_CODE FROM dual) s
ON (t.CHECK_PHASE_CODE = s.CHECK_PHASE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'پیش از ارسال درخواست',
    t.TITLE_EN = 'Pre Submission',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 10,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'پیش از ارسال درخواست' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Pre Submission' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 10 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (CHECK_PHASE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('PRE_SUBMISSION', 'پیش از ارسال درخواست', 'Pre Submission', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 10, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_CHECK_PHASE t
USING (SELECT 'PRE_APPROVAL' AS CHECK_PHASE_CODE FROM dual) s
ON (t.CHECK_PHASE_CODE = s.CHECK_PHASE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'پیش از تأیید',
    t.TITLE_EN = 'Pre Approval',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 20,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'پیش از تأیید' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Pre Approval' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 20 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (CHECK_PHASE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('PRE_APPROVAL', 'پیش از تأیید', 'Pre Approval', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 20, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_CHECK_PHASE t
USING (SELECT 'PRE_ACCOUNT_CREATION' AS CHECK_PHASE_CODE FROM dual) s
ON (t.CHECK_PHASE_CODE = s.CHECK_PHASE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'پیش از ایجاد حساب',
    t.TITLE_EN = 'Pre Account Creation',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 30,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'پیش از ایجاد حساب' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Pre Account Creation' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 30 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (CHECK_PHASE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('PRE_ACCOUNT_CREATION', 'پیش از ایجاد حساب', 'Pre Account Creation', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 30, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_CHECK_PHASE t
USING (SELECT 'PRE_FUNDING' AS CHECK_PHASE_CODE FROM dual) s
ON (t.CHECK_PHASE_CODE = s.CHECK_PHASE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'پیش از تأمین وجه',
    t.TITLE_EN = 'Pre Funding',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 40,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'پیش از تأمین وجه' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Pre Funding' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 40 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (CHECK_PHASE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('PRE_FUNDING', 'پیش از تأمین وجه', 'Pre Funding', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 40, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_CHECK_PHASE t
USING (SELECT 'PRE_ACTIVATION' AS CHECK_PHASE_CODE FROM dual) s
ON (t.CHECK_PHASE_CODE = s.CHECK_PHASE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'پیش از فعال‌سازی',
    t.TITLE_EN = 'Pre Activation',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 50,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'پیش از فعال‌سازی' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Pre Activation' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 50 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (CHECK_PHASE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('PRE_ACTIVATION', 'پیش از فعال‌سازی', 'Pre Activation', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 50, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_CHECK_PHASE t
USING (SELECT 'POST_ACTIVATION' AS CHECK_PHASE_CODE FROM dual) s
ON (t.CHECK_PHASE_CODE = s.CHECK_PHASE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'پس از فعال‌سازی',
    t.TITLE_EN = 'Post Activation',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 60,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'پس از فعال‌سازی' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Post Activation' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 60 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (CHECK_PHASE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('POST_ACTIVATION', 'پس از فعال‌سازی', 'Post Activation', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 60, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

-- --------------------------------------------------------------------------
-- REF_DEP_OPEN_BLOCKING_SCOPE
-- --------------------------------------------------------------------------
MERGE INTO DPS2.REF_DEP_OPEN_BLOCKING_SCOPE t
USING (SELECT 'REQUEST' AS BLOCKING_SCOPE_CODE FROM dual) s
ON (t.BLOCKING_SCOPE_CODE = s.BLOCKING_SCOPE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'درخواست',
    t.TITLE_EN = 'Request',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 10,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'درخواست' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Request' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 10 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (BLOCKING_SCOPE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('REQUEST', 'درخواست', 'Request', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 10, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_BLOCKING_SCOPE t
USING (SELECT 'ACCOUNT_CREATION' AS BLOCKING_SCOPE_CODE FROM dual) s
ON (t.BLOCKING_SCOPE_CODE = s.BLOCKING_SCOPE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'ایجاد حساب',
    t.TITLE_EN = 'Account Creation',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 20,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'ایجاد حساب' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Account Creation' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 20 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (BLOCKING_SCOPE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('ACCOUNT_CREATION', 'ایجاد حساب', 'Account Creation', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 20, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_BLOCKING_SCOPE t
USING (SELECT 'ACCOUNT_ACTIVATION' AS BLOCKING_SCOPE_CODE FROM dual) s
ON (t.BLOCKING_SCOPE_CODE = s.BLOCKING_SCOPE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'فعال‌سازی حساب',
    t.TITLE_EN = 'Account Activation',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 30,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'فعال‌سازی حساب' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Account Activation' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 30 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (BLOCKING_SCOPE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('ACCOUNT_ACTIVATION', 'فعال‌سازی حساب', 'Account Activation', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 30, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_BLOCKING_SCOPE t
USING (SELECT 'DEBIT_CAPABILITY' AS BLOCKING_SCOPE_CODE FROM dual) s
ON (t.BLOCKING_SCOPE_CODE = s.BLOCKING_SCOPE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'قابلیت برداشت / بدهکارکردن',
    t.TITLE_EN = 'Debit Capability',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 40,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'قابلیت برداشت / بدهکارکردن' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Debit Capability' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 40 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (BLOCKING_SCOPE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('DEBIT_CAPABILITY', 'قابلیت برداشت / بدهکارکردن', 'Debit Capability', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 40, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_BLOCKING_SCOPE t
USING (SELECT 'SERVICE_ONLY' AS BLOCKING_SCOPE_CODE FROM dual) s
ON (t.BLOCKING_SCOPE_CODE = s.BLOCKING_SCOPE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'فقط خدمت مربوطه',
    t.TITLE_EN = 'Service Only',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 50,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'فقط خدمت مربوطه' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Service Only' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 50 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (BLOCKING_SCOPE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('SERVICE_ONLY', 'فقط خدمت مربوطه', 'Service Only', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 50, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_BLOCKING_SCOPE t
USING (SELECT 'NON_BLOCKING' AS BLOCKING_SCOPE_CODE FROM dual) s
ON (t.BLOCKING_SCOPE_CODE = s.BLOCKING_SCOPE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'غیرمسدودکننده',
    t.TITLE_EN = 'Non Blocking',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 60,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'غیرمسدودکننده' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Non Blocking' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 60 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (BLOCKING_SCOPE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('NON_BLOCKING', 'غیرمسدودکننده', 'Non Blocking', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 60, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

-- --------------------------------------------------------------------------
-- REF_DEP_OPEN_OBLIGATION_TYPE
-- --------------------------------------------------------------------------
MERGE INTO DPS2.REF_DEP_OPEN_OBLIGATION_TYPE t
USING (SELECT 'INITIAL_BALANCE' AS OBLIGATION_TYPE_CODE FROM dual) s
ON (t.OBLIGATION_TYPE_CODE = s.OBLIGATION_TYPE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'مانده اولیه',
    t.TITLE_EN = 'Initial Balance',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 10,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'مانده اولیه' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Initial Balance' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 10 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (OBLIGATION_TYPE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('INITIAL_BALANCE', 'مانده اولیه', 'Initial Balance', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 10, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_OBLIGATION_TYPE t
USING (SELECT 'OPENING_FEE' AS OBLIGATION_TYPE_CODE FROM dual) s
ON (t.OBLIGATION_TYPE_CODE = s.OBLIGATION_TYPE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'کارمزد افتتاح',
    t.TITLE_EN = 'Opening Fee',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 20,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'کارمزد افتتاح' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Opening Fee' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 20 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (OBLIGATION_TYPE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('OPENING_FEE', 'کارمزد افتتاح', 'Opening Fee', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 20, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_OBLIGATION_TYPE t
USING (SELECT 'SERVICE_FEE' AS OBLIGATION_TYPE_CODE FROM dual) s
ON (t.OBLIGATION_TYPE_CODE = s.OBLIGATION_TYPE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'کارمزد خدمت',
    t.TITLE_EN = 'Service Fee',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 30,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'کارمزد خدمت' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Service Fee' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 30 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (OBLIGATION_TYPE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('SERVICE_FEE', 'کارمزد خدمت', 'Service Fee', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 30, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_OBLIGATION_TYPE t
USING (SELECT 'TAX' AS OBLIGATION_TYPE_CODE FROM dual) s
ON (t.OBLIGATION_TYPE_CODE = s.OBLIGATION_TYPE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'مالیات',
    t.TITLE_EN = 'Tax',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 40,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'مالیات' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Tax' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 40 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (OBLIGATION_TYPE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('TAX', 'مالیات', 'Tax', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 40, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_OBLIGATION_TYPE t
USING (SELECT 'CARD_FEE' AS OBLIGATION_TYPE_CODE FROM dual) s
ON (t.OBLIGATION_TYPE_CODE = s.OBLIGATION_TYPE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'کارمزد کارت',
    t.TITLE_EN = 'Card Fee',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 50,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'کارمزد کارت' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Card Fee' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 50 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (OBLIGATION_TYPE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('CARD_FEE', 'کارمزد کارت', 'Card Fee', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 50, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_OBLIGATION_TYPE t
USING (SELECT 'CHEQUEBOOK_FEE' AS OBLIGATION_TYPE_CODE FROM dual) s
ON (t.OBLIGATION_TYPE_CODE = s.OBLIGATION_TYPE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'کارمزد دسته‌چک',
    t.TITLE_EN = 'Chequebook Fee',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 60,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'کارمزد دسته‌چک' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Chequebook Fee' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 60 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (OBLIGATION_TYPE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('CHEQUEBOOK_FEE', 'کارمزد دسته‌چک', 'Chequebook Fee', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 60, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_OBLIGATION_TYPE t
USING (SELECT 'OTHER_CHARGE' AS OBLIGATION_TYPE_CODE FROM dual) s
ON (t.OBLIGATION_TYPE_CODE = s.OBLIGATION_TYPE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'سایر هزینه‌ها',
    t.TITLE_EN = 'Other Charge',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 70,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'سایر هزینه‌ها' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Other Charge' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 70 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (OBLIGATION_TYPE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('OTHER_CHARGE', 'سایر هزینه‌ها', 'Other Charge', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 70, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

-- --------------------------------------------------------------------------
-- REF_DEP_OPEN_SETTLEMENT_STATUS
-- --------------------------------------------------------------------------
MERGE INTO DPS2.REF_DEP_OPEN_SETTLEMENT_STATUS t
USING (SELECT 'PENDING' AS SETTLEMENT_STATUS_CODE FROM dual) s
ON (t.SETTLEMENT_STATUS_CODE = s.SETTLEMENT_STATUS_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'در انتظار',
    t.TITLE_EN = 'Pending',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 10,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'در انتظار' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Pending' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 10 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (SETTLEMENT_STATUS_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('PENDING', 'در انتظار', 'Pending', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 10, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_SETTLEMENT_STATUS t
USING (SELECT 'PARTIAL' AS SETTLEMENT_STATUS_CODE FROM dual) s
ON (t.SETTLEMENT_STATUS_CODE = s.SETTLEMENT_STATUS_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'جزئی',
    t.TITLE_EN = 'Partial',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 20,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'جزئی' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Partial' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 20 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (SETTLEMENT_STATUS_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('PARTIAL', 'جزئی', 'Partial', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 20, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_SETTLEMENT_STATUS t
USING (SELECT 'SETTLED' AS SETTLEMENT_STATUS_CODE FROM dual) s
ON (t.SETTLEMENT_STATUS_CODE = s.SETTLEMENT_STATUS_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'تسویه‌شده',
    t.TITLE_EN = 'Settled',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 30,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'تسویه‌شده' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Settled' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 30 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (SETTLEMENT_STATUS_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('SETTLED', 'تسویه‌شده', 'Settled', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 30, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_SETTLEMENT_STATUS t
USING (SELECT 'WAIVED' AS SETTLEMENT_STATUS_CODE FROM dual) s
ON (t.SETTLEMENT_STATUS_CODE = s.SETTLEMENT_STATUS_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'معاف‌شده',
    t.TITLE_EN = 'Waived',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 40,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'معاف‌شده' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Waived' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 40 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (SETTLEMENT_STATUS_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('WAIVED', 'معاف‌شده', 'Waived', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 40, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_SETTLEMENT_STATUS t
USING (SELECT 'FAILED' AS SETTLEMENT_STATUS_CODE FROM dual) s
ON (t.SETTLEMENT_STATUS_CODE = s.SETTLEMENT_STATUS_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'ناموفق',
    t.TITLE_EN = 'Failed',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 50,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'ناموفق' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Failed' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 50 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (SETTLEMENT_STATUS_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('FAILED', 'ناموفق', 'Failed', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 50, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_SETTLEMENT_STATUS t
USING (SELECT 'REFUNDED' AS SETTLEMENT_STATUS_CODE FROM dual) s
ON (t.SETTLEMENT_STATUS_CODE = s.SETTLEMENT_STATUS_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'مستردشده',
    t.TITLE_EN = 'Refunded',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 60,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'مستردشده' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Refunded' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 60 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (SETTLEMENT_STATUS_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('REFUNDED', 'مستردشده', 'Refunded', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 60, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_SETTLEMENT_STATUS t
USING (SELECT 'CANCELLED' AS SETTLEMENT_STATUS_CODE FROM dual) s
ON (t.SETTLEMENT_STATUS_CODE = s.SETTLEMENT_STATUS_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'لغوشده',
    t.TITLE_EN = 'Cancelled',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 70,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'لغوشده' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Cancelled' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 70 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (SETTLEMENT_STATUS_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('CANCELLED', 'لغوشده', 'Cancelled', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 70, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

-- --------------------------------------------------------------------------
-- REF_DEP_OPEN_ACTIVATION_STATUS
-- --------------------------------------------------------------------------
MERGE INTO DPS2.REF_DEP_OPEN_ACTIVATION_STATUS t
USING (SELECT 'NOT_CREATED' AS ACTIVATION_STATUS_CODE FROM dual) s
ON (t.ACTIVATION_STATUS_CODE = s.ACTIVATION_STATUS_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'ایجادنشده',
    t.TITLE_EN = 'Not Created',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 10,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'ایجادنشده' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Not Created' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 10 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (ACTIVATION_STATUS_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('NOT_CREATED', 'ایجادنشده', 'Not Created', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 10, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_ACTIVATION_STATUS t
USING (SELECT 'PENDING_READINESS' AS ACTIVATION_STATUS_CODE FROM dual) s
ON (t.ACTIVATION_STATUS_CODE = s.ACTIVATION_STATUS_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'در انتظار احراز آمادگی',
    t.TITLE_EN = 'Pending Readiness',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 20,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'در انتظار احراز آمادگی' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Pending Readiness' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 20 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (ACTIVATION_STATUS_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('PENDING_READINESS', 'در انتظار احراز آمادگی', 'Pending Readiness', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 20, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_ACTIVATION_STATUS t
USING (SELECT 'READY' AS ACTIVATION_STATUS_CODE FROM dual) s
ON (t.ACTIVATION_STATUS_CODE = s.ACTIVATION_STATUS_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'آماده',
    t.TITLE_EN = 'Ready',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 30,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'آماده' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Ready' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 30 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (ACTIVATION_STATUS_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('READY', 'آماده', 'Ready', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 30, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_ACTIVATION_STATUS t
USING (SELECT 'BLOCKED' AS ACTIVATION_STATUS_CODE FROM dual) s
ON (t.ACTIVATION_STATUS_CODE = s.ACTIVATION_STATUS_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'مسدود',
    t.TITLE_EN = 'Blocked',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 40,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'مسدود' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Blocked' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 40 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (ACTIVATION_STATUS_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('BLOCKED', 'مسدود', 'Blocked', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 40, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_ACTIVATION_STATUS t
USING (SELECT 'ACTIVATED' AS ACTIVATION_STATUS_CODE FROM dual) s
ON (t.ACTIVATION_STATUS_CODE = s.ACTIVATION_STATUS_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'فعال‌شده',
    t.TITLE_EN = 'Activated',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 50,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'فعال‌شده' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Activated' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 50 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (ACTIVATION_STATUS_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('ACTIVATED', 'فعال‌شده', 'Activated', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 50, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_ACTIVATION_STATUS t
USING (SELECT 'CANCELLED' AS ACTIVATION_STATUS_CODE FROM dual) s
ON (t.ACTIVATION_STATUS_CODE = s.ACTIVATION_STATUS_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'لغوشده',
    t.TITLE_EN = 'Cancelled',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 60,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'لغوشده' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Cancelled' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 60 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (ACTIVATION_STATUS_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('CANCELLED', 'لغوشده', 'Cancelled', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 60, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

-- --------------------------------------------------------------------------
-- REF_DEP_OPEN_JOINT_BASIS
-- --------------------------------------------------------------------------
MERGE INTO DPS2.REF_DEP_OPEN_JOINT_BASIS t
USING (SELECT 'FAMILY_RELATION' AS JOINT_ACCOUNT_BASIS_CODE FROM dual) s
ON (t.JOINT_ACCOUNT_BASIS_CODE = s.JOINT_ACCOUNT_BASIS_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'رابطه خانوادگی مجاز',
    t.TITLE_EN = 'Family Relation',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 10,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'رابطه خانوادگی مجاز' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Family Relation' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 10 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (JOINT_ACCOUNT_BASIS_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('FAMILY_RELATION', 'رابطه خانوادگی مجاز', 'Family Relation', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 10, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_JOINT_BASIS t
USING (SELECT 'COMMERCIAL_ACCOUNT' AS JOINT_ACCOUNT_BASIS_CODE FROM dual) s
ON (t.JOINT_ACCOUNT_BASIS_CODE = s.JOINT_ACCOUNT_BASIS_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'حساب تجاری مشترک',
    t.TITLE_EN = 'Commercial Account',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 20,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'حساب تجاری مشترک' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Commercial Account' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 20 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (JOINT_ACCOUNT_BASIS_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('COMMERCIAL_ACCOUNT', 'حساب تجاری مشترک', 'Commercial Account', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 20, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

-- --------------------------------------------------------------------------
-- REF_DEP_OPEN_FUND_PURPOSE
-- --------------------------------------------------------------------------
MERGE INTO DPS2.REF_DEP_OPEN_FUND_PURPOSE t
USING (SELECT 'OPENING_BALANCE' AS FUNDING_PURPOSE_CODE FROM dual) s
ON (t.FUNDING_PURPOSE_CODE = s.FUNDING_PURPOSE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'مانده افتتاح',
    t.TITLE_EN = 'Opening Balance',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 10,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'مانده افتتاح' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Opening Balance' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 10 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (FUNDING_PURPOSE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('OPENING_BALANCE', 'مانده افتتاح', 'Opening Balance', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 10, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_FUND_PURPOSE t
USING (SELECT 'OBLIGATION_SETTLEMENT' AS FUNDING_PURPOSE_CODE FROM dual) s
ON (t.FUNDING_PURPOSE_CODE = s.FUNDING_PURPOSE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'تسویه تعهدات افتتاح',
    t.TITLE_EN = 'Obligation Settlement',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 20,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'تسویه تعهدات افتتاح' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Obligation Settlement' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 20 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (FUNDING_PURPOSE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('OBLIGATION_SETTLEMENT', 'تسویه تعهدات افتتاح', 'Obligation Settlement', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 20, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_FUND_PURPOSE t
USING (SELECT 'COMBINED' AS FUNDING_PURPOSE_CODE FROM dual) s
ON (t.FUNDING_PURPOSE_CODE = s.FUNDING_PURPOSE_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'ترکیبی',
    t.TITLE_EN = 'Combined',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 30,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'ترکیبی' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Combined' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 30 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (FUNDING_PURPOSE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('COMBINED', 'ترکیبی', 'Combined', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 30, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

-- --------------------------------------------------------------------------
-- REF_DEP_OPEN_CHECK
-- --------------------------------------------------------------------------
MERGE INTO DPS2.REF_DEP_OPEN_CHECK t
USING (SELECT 'PRODUCT_ELIGIBILITY' AS CHECK_CODE FROM dual) s
ON (t.CHECK_CODE = s.CHECK_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'اهلیت محصول',
    t.TITLE_EN = 'Product Eligibility',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 10,
    t.IS_ACTIVE = 1,
    t.CHECK_TYPE_CODE = 'PRODUCT_RULE',
    t.DEFAULT_REQUIRED_FLAG = 1,
    t.EXECUTION_ORDER = 10,
    t.DEFAULT_PHASE_CODE = 'PRE_APPROVAL',
    t.DEFAULT_BLOCKING_SCOPE_CODE = 'ACCOUNT_CREATION',
    t.DEFAULT_RECHECK_FLAG = 0,
    t.RESULT_VALIDITY_MINUTES = NULL,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'اهلیت محصول' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Product Eligibility' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 10 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1 OR
       NVL(t.CHECK_TYPE_CODE, CHR(0)) <> 'PRODUCT_RULE' OR
       NVL(t.DEFAULT_REQUIRED_FLAG, -999999999) <> 1 OR
       NVL(t.EXECUTION_ORDER, -999999999) <> 10 OR
       NVL(t.DEFAULT_PHASE_CODE, CHR(0)) <> 'PRE_APPROVAL' OR
       NVL(t.DEFAULT_BLOCKING_SCOPE_CODE, CHR(0)) <> 'ACCOUNT_CREATION' OR
       NVL(t.DEFAULT_RECHECK_FLAG, -999999999) <> 0 OR
       t.RESULT_VALIDITY_MINUTES IS NOT NULL)
WHEN NOT MATCHED THEN
  INSERT (CHECK_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CHECK_TYPE_CODE, DEFAULT_REQUIRED_FLAG, EXECUTION_ORDER, DEFAULT_PHASE_CODE, DEFAULT_BLOCKING_SCOPE_CODE, DEFAULT_RECHECK_FLAG, RESULT_VALIDITY_MINUTES, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('PRODUCT_ELIGIBILITY', 'اهلیت محصول', 'Product Eligibility', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 10, 1, 'PRODUCT_RULE', 1, 10, 'PRE_APPROVAL', 'ACCOUNT_CREATION', 0, NULL, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_CHECK t
USING (SELECT 'KYC_CDD' AS CHECK_CODE FROM dual) s
ON (t.CHECK_CODE = s.CHECK_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'شناخت مشتری و بررسی CDD',
    t.TITLE_EN = 'KYC CDD',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 20,
    t.IS_ACTIVE = 1,
    t.CHECK_TYPE_CODE = 'COMPLIANCE',
    t.DEFAULT_REQUIRED_FLAG = 1,
    t.EXECUTION_ORDER = 20,
    t.DEFAULT_PHASE_CODE = 'PRE_APPROVAL',
    t.DEFAULT_BLOCKING_SCOPE_CODE = 'ACCOUNT_CREATION',
    t.DEFAULT_RECHECK_FLAG = 0,
    t.RESULT_VALIDITY_MINUTES = NULL,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'شناخت مشتری و بررسی CDD' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'KYC CDD' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 20 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1 OR
       NVL(t.CHECK_TYPE_CODE, CHR(0)) <> 'COMPLIANCE' OR
       NVL(t.DEFAULT_REQUIRED_FLAG, -999999999) <> 1 OR
       NVL(t.EXECUTION_ORDER, -999999999) <> 20 OR
       NVL(t.DEFAULT_PHASE_CODE, CHR(0)) <> 'PRE_APPROVAL' OR
       NVL(t.DEFAULT_BLOCKING_SCOPE_CODE, CHR(0)) <> 'ACCOUNT_CREATION' OR
       NVL(t.DEFAULT_RECHECK_FLAG, -999999999) <> 0 OR
       t.RESULT_VALIDITY_MINUTES IS NOT NULL)
WHEN NOT MATCHED THEN
  INSERT (CHECK_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CHECK_TYPE_CODE, DEFAULT_REQUIRED_FLAG, EXECUTION_ORDER, DEFAULT_PHASE_CODE, DEFAULT_BLOCKING_SCOPE_CODE, DEFAULT_RECHECK_FLAG, RESULT_VALIDITY_MINUTES, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('KYC_CDD', 'شناخت مشتری و بررسی CDD', 'KYC CDD', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 20, 1, 'COMPLIANCE', 1, 20, 'PRE_APPROVAL', 'ACCOUNT_CREATION', 0, NULL, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_CHECK t
USING (SELECT 'SANCTIONS' AS CHECK_CODE FROM dual) s
ON (t.CHECK_CODE = s.CHECK_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'کنترل تحریم‌ها',
    t.TITLE_EN = 'Sanctions',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 30,
    t.IS_ACTIVE = 1,
    t.CHECK_TYPE_CODE = 'COMPLIANCE',
    t.DEFAULT_REQUIRED_FLAG = 1,
    t.EXECUTION_ORDER = 30,
    t.DEFAULT_PHASE_CODE = 'PRE_APPROVAL',
    t.DEFAULT_BLOCKING_SCOPE_CODE = 'ACCOUNT_CREATION',
    t.DEFAULT_RECHECK_FLAG = 0,
    t.RESULT_VALIDITY_MINUTES = NULL,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'کنترل تحریم‌ها' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Sanctions' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 30 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1 OR
       NVL(t.CHECK_TYPE_CODE, CHR(0)) <> 'COMPLIANCE' OR
       NVL(t.DEFAULT_REQUIRED_FLAG, -999999999) <> 1 OR
       NVL(t.EXECUTION_ORDER, -999999999) <> 30 OR
       NVL(t.DEFAULT_PHASE_CODE, CHR(0)) <> 'PRE_APPROVAL' OR
       NVL(t.DEFAULT_BLOCKING_SCOPE_CODE, CHR(0)) <> 'ACCOUNT_CREATION' OR
       NVL(t.DEFAULT_RECHECK_FLAG, -999999999) <> 0 OR
       t.RESULT_VALIDITY_MINUTES IS NOT NULL)
WHEN NOT MATCHED THEN
  INSERT (CHECK_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CHECK_TYPE_CODE, DEFAULT_REQUIRED_FLAG, EXECUTION_ORDER, DEFAULT_PHASE_CODE, DEFAULT_BLOCKING_SCOPE_CODE, DEFAULT_RECHECK_FLAG, RESULT_VALIDITY_MINUTES, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('SANCTIONS', 'کنترل تحریم‌ها', 'Sanctions', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 30, 1, 'COMPLIANCE', 1, 30, 'PRE_APPROVAL', 'ACCOUNT_CREATION', 0, NULL, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_CHECK t
USING (SELECT 'DOCUMENTS' AS CHECK_CODE FROM dual) s
ON (t.CHECK_CODE = s.CHECK_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'کنترل مدارک',
    t.TITLE_EN = 'Documents',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 40,
    t.IS_ACTIVE = 1,
    t.CHECK_TYPE_CODE = 'DOCUMENT',
    t.DEFAULT_REQUIRED_FLAG = 1,
    t.EXECUTION_ORDER = 40,
    t.DEFAULT_PHASE_CODE = 'PRE_APPROVAL',
    t.DEFAULT_BLOCKING_SCOPE_CODE = 'ACCOUNT_CREATION',
    t.DEFAULT_RECHECK_FLAG = 0,
    t.RESULT_VALIDITY_MINUTES = NULL,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'کنترل مدارک' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Documents' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 40 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1 OR
       NVL(t.CHECK_TYPE_CODE, CHR(0)) <> 'DOCUMENT' OR
       NVL(t.DEFAULT_REQUIRED_FLAG, -999999999) <> 1 OR
       NVL(t.EXECUTION_ORDER, -999999999) <> 40 OR
       NVL(t.DEFAULT_PHASE_CODE, CHR(0)) <> 'PRE_APPROVAL' OR
       NVL(t.DEFAULT_BLOCKING_SCOPE_CODE, CHR(0)) <> 'ACCOUNT_CREATION' OR
       NVL(t.DEFAULT_RECHECK_FLAG, -999999999) <> 0 OR
       t.RESULT_VALIDITY_MINUTES IS NOT NULL)
WHEN NOT MATCHED THEN
  INSERT (CHECK_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CHECK_TYPE_CODE, DEFAULT_REQUIRED_FLAG, EXECUTION_ORDER, DEFAULT_PHASE_CODE, DEFAULT_BLOCKING_SCOPE_CODE, DEFAULT_RECHECK_FLAG, RESULT_VALIDITY_MINUTES, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('DOCUMENTS', 'کنترل مدارک', 'Documents', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 40, 1, 'DOCUMENT', 1, 40, 'PRE_APPROVAL', 'ACCOUNT_CREATION', 0, NULL, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_CHECK t
USING (SELECT 'INQUIRIES' AS CHECK_CODE FROM dual) s
ON (t.CHECK_CODE = s.CHECK_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'استعلام‌های افتتاح',
    t.TITLE_EN = 'Inquiries',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 50,
    t.IS_ACTIVE = 1,
    t.CHECK_TYPE_CODE = 'INQUIRY',
    t.DEFAULT_REQUIRED_FLAG = 1,
    t.EXECUTION_ORDER = 50,
    t.DEFAULT_PHASE_CODE = 'PRE_APPROVAL',
    t.DEFAULT_BLOCKING_SCOPE_CODE = 'ACCOUNT_CREATION',
    t.DEFAULT_RECHECK_FLAG = 0,
    t.RESULT_VALIDITY_MINUTES = NULL,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'استعلام‌های افتتاح' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Inquiries' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 50 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1 OR
       NVL(t.CHECK_TYPE_CODE, CHR(0)) <> 'INQUIRY' OR
       NVL(t.DEFAULT_REQUIRED_FLAG, -999999999) <> 1 OR
       NVL(t.EXECUTION_ORDER, -999999999) <> 50 OR
       NVL(t.DEFAULT_PHASE_CODE, CHR(0)) <> 'PRE_APPROVAL' OR
       NVL(t.DEFAULT_BLOCKING_SCOPE_CODE, CHR(0)) <> 'ACCOUNT_CREATION' OR
       NVL(t.DEFAULT_RECHECK_FLAG, -999999999) <> 0 OR
       t.RESULT_VALIDITY_MINUTES IS NOT NULL)
WHEN NOT MATCHED THEN
  INSERT (CHECK_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CHECK_TYPE_CODE, DEFAULT_REQUIRED_FLAG, EXECUTION_ORDER, DEFAULT_PHASE_CODE, DEFAULT_BLOCKING_SCOPE_CODE, DEFAULT_RECHECK_FLAG, RESULT_VALIDITY_MINUTES, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('INQUIRIES', 'استعلام‌های افتتاح', 'Inquiries', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 50, 1, 'INQUIRY', 1, 50, 'PRE_APPROVAL', 'ACCOUNT_CREATION', 0, NULL, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_CHECK t
USING (SELECT 'OPENING_RULES' AS CHECK_CODE FROM dual) s
ON (t.CHECK_CODE = s.CHECK_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'قواعد افتتاح',
    t.TITLE_EN = 'Opening Rules',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 60,
    t.IS_ACTIVE = 1,
    t.CHECK_TYPE_CODE = 'PRODUCT_RULE',
    t.DEFAULT_REQUIRED_FLAG = 1,
    t.EXECUTION_ORDER = 60,
    t.DEFAULT_PHASE_CODE = 'PRE_APPROVAL',
    t.DEFAULT_BLOCKING_SCOPE_CODE = 'ACCOUNT_CREATION',
    t.DEFAULT_RECHECK_FLAG = 0,
    t.RESULT_VALIDITY_MINUTES = NULL,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'قواعد افتتاح' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Opening Rules' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 60 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1 OR
       NVL(t.CHECK_TYPE_CODE, CHR(0)) <> 'PRODUCT_RULE' OR
       NVL(t.DEFAULT_REQUIRED_FLAG, -999999999) <> 1 OR
       NVL(t.EXECUTION_ORDER, -999999999) <> 60 OR
       NVL(t.DEFAULT_PHASE_CODE, CHR(0)) <> 'PRE_APPROVAL' OR
       NVL(t.DEFAULT_BLOCKING_SCOPE_CODE, CHR(0)) <> 'ACCOUNT_CREATION' OR
       NVL(t.DEFAULT_RECHECK_FLAG, -999999999) <> 0 OR
       t.RESULT_VALIDITY_MINUTES IS NOT NULL)
WHEN NOT MATCHED THEN
  INSERT (CHECK_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CHECK_TYPE_CODE, DEFAULT_REQUIRED_FLAG, EXECUTION_ORDER, DEFAULT_PHASE_CODE, DEFAULT_BLOCKING_SCOPE_CODE, DEFAULT_RECHECK_FLAG, RESULT_VALIDITY_MINUTES, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('OPENING_RULES', 'قواعد افتتاح', 'Opening Rules', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 60, 1, 'PRODUCT_RULE', 1, 60, 'PRE_APPROVAL', 'ACCOUNT_CREATION', 0, NULL, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_CHECK t
USING (SELECT 'SIGNATORY_AUTHORITY' AS CHECK_CODE FROM dual) s
ON (t.CHECK_CODE = s.CHECK_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'اختیار صاحبان امضا',
    t.TITLE_EN = 'Signatory Authority',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 70,
    t.IS_ACTIVE = 1,
    t.CHECK_TYPE_CODE = 'AUTHORITY',
    t.DEFAULT_REQUIRED_FLAG = 1,
    t.EXECUTION_ORDER = 70,
    t.DEFAULT_PHASE_CODE = 'PRE_APPROVAL',
    t.DEFAULT_BLOCKING_SCOPE_CODE = 'ACCOUNT_CREATION',
    t.DEFAULT_RECHECK_FLAG = 0,
    t.RESULT_VALIDITY_MINUTES = NULL,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'اختیار صاحبان امضا' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Signatory Authority' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 70 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1 OR
       NVL(t.CHECK_TYPE_CODE, CHR(0)) <> 'AUTHORITY' OR
       NVL(t.DEFAULT_REQUIRED_FLAG, -999999999) <> 1 OR
       NVL(t.EXECUTION_ORDER, -999999999) <> 70 OR
       NVL(t.DEFAULT_PHASE_CODE, CHR(0)) <> 'PRE_APPROVAL' OR
       NVL(t.DEFAULT_BLOCKING_SCOPE_CODE, CHR(0)) <> 'ACCOUNT_CREATION' OR
       NVL(t.DEFAULT_RECHECK_FLAG, -999999999) <> 0 OR
       t.RESULT_VALIDITY_MINUTES IS NOT NULL)
WHEN NOT MATCHED THEN
  INSERT (CHECK_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CHECK_TYPE_CODE, DEFAULT_REQUIRED_FLAG, EXECUTION_ORDER, DEFAULT_PHASE_CODE, DEFAULT_BLOCKING_SCOPE_CODE, DEFAULT_RECHECK_FLAG, RESULT_VALIDITY_MINUTES, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('SIGNATORY_AUTHORITY', 'اختیار صاحبان امضا', 'Signatory Authority', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 70, 1, 'AUTHORITY', 1, 70, 'PRE_APPROVAL', 'ACCOUNT_CREATION', 0, NULL, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_CHECK t
USING (SELECT 'TERMS_ACCEPTANCE' AS CHECK_CODE FROM dual) s
ON (t.CHECK_CODE = s.CHECK_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'پذیرش شروط',
    t.TITLE_EN = 'Terms Acceptance',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 80,
    t.IS_ACTIVE = 1,
    t.CHECK_TYPE_CODE = 'CONSENT',
    t.DEFAULT_REQUIRED_FLAG = 1,
    t.EXECUTION_ORDER = 80,
    t.DEFAULT_PHASE_CODE = 'PRE_APPROVAL',
    t.DEFAULT_BLOCKING_SCOPE_CODE = 'ACCOUNT_CREATION',
    t.DEFAULT_RECHECK_FLAG = 0,
    t.RESULT_VALIDITY_MINUTES = NULL,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'پذیرش شروط' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Terms Acceptance' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 80 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1 OR
       NVL(t.CHECK_TYPE_CODE, CHR(0)) <> 'CONSENT' OR
       NVL(t.DEFAULT_REQUIRED_FLAG, -999999999) <> 1 OR
       NVL(t.EXECUTION_ORDER, -999999999) <> 80 OR
       NVL(t.DEFAULT_PHASE_CODE, CHR(0)) <> 'PRE_APPROVAL' OR
       NVL(t.DEFAULT_BLOCKING_SCOPE_CODE, CHR(0)) <> 'ACCOUNT_CREATION' OR
       NVL(t.DEFAULT_RECHECK_FLAG, -999999999) <> 0 OR
       t.RESULT_VALIDITY_MINUTES IS NOT NULL)
WHEN NOT MATCHED THEN
  INSERT (CHECK_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CHECK_TYPE_CODE, DEFAULT_REQUIRED_FLAG, EXECUTION_ORDER, DEFAULT_PHASE_CODE, DEFAULT_BLOCKING_SCOPE_CODE, DEFAULT_RECHECK_FLAG, RESULT_VALIDITY_MINUTES, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('TERMS_ACCEPTANCE', 'پذیرش شروط', 'Terms Acceptance', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 80, 1, 'CONSENT', 1, 80, 'PRE_APPROVAL', 'ACCOUNT_CREATION', 0, NULL, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_CHECK t
USING (SELECT 'DUPLICATE_REQUEST' AS CHECK_CODE FROM dual) s
ON (t.CHECK_CODE = s.CHECK_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'کنترل درخواست تکراری',
    t.TITLE_EN = 'Duplicate Request',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 90,
    t.IS_ACTIVE = 1,
    t.CHECK_TYPE_CODE = 'OPERATIONAL',
    t.DEFAULT_REQUIRED_FLAG = 1,
    t.EXECUTION_ORDER = 90,
    t.DEFAULT_PHASE_CODE = 'PRE_APPROVAL',
    t.DEFAULT_BLOCKING_SCOPE_CODE = 'ACCOUNT_CREATION',
    t.DEFAULT_RECHECK_FLAG = 0,
    t.RESULT_VALIDITY_MINUTES = NULL,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'کنترل درخواست تکراری' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Duplicate Request' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 90 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1 OR
       NVL(t.CHECK_TYPE_CODE, CHR(0)) <> 'OPERATIONAL' OR
       NVL(t.DEFAULT_REQUIRED_FLAG, -999999999) <> 1 OR
       NVL(t.EXECUTION_ORDER, -999999999) <> 90 OR
       NVL(t.DEFAULT_PHASE_CODE, CHR(0)) <> 'PRE_APPROVAL' OR
       NVL(t.DEFAULT_BLOCKING_SCOPE_CODE, CHR(0)) <> 'ACCOUNT_CREATION' OR
       NVL(t.DEFAULT_RECHECK_FLAG, -999999999) <> 0 OR
       t.RESULT_VALIDITY_MINUTES IS NOT NULL)
WHEN NOT MATCHED THEN
  INSERT (CHECK_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CHECK_TYPE_CODE, DEFAULT_REQUIRED_FLAG, EXECUTION_ORDER, DEFAULT_PHASE_CODE, DEFAULT_BLOCKING_SCOPE_CODE, DEFAULT_RECHECK_FLAG, RESULT_VALIDITY_MINUTES, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('DUPLICATE_REQUEST', 'کنترل درخواست تکراری', 'Duplicate Request', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 90, 1, 'OPERATIONAL', 1, 90, 'PRE_APPROVAL', 'ACCOUNT_CREATION', 0, NULL, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

-- --------------------------------------------------------------------------
-- REF_DEP_OPEN_DECISION_REASON
-- --------------------------------------------------------------------------
MERGE INTO DPS2.REF_DEP_OPEN_DECISION_REASON t
USING (SELECT 'ALL_CHECKS_PASSED' AS DECISION_REASON_CODE FROM dual) s
ON (t.DECISION_REASON_CODE = s.DECISION_REASON_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'عبور از همه کنترل‌ها',
    t.TITLE_EN = 'All Checks Passed',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 10,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'عبور از همه کنترل‌ها' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'All Checks Passed' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 10 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (DECISION_REASON_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('ALL_CHECKS_PASSED', 'عبور از همه کنترل‌ها', 'All Checks Passed', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 10, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_DECISION_REASON t
USING (SELECT 'MANUAL_REVIEW' AS DECISION_REASON_CODE FROM dual) s
ON (t.DECISION_REASON_CODE = s.DECISION_REASON_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'بررسی دستی',
    t.TITLE_EN = 'Manual Review',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 20,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'بررسی دستی' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Manual Review' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 20 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (DECISION_REASON_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('MANUAL_REVIEW', 'بررسی دستی', 'Manual Review', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 20, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

MERGE INTO DPS2.REF_DEP_OPEN_DECISION_REASON t
USING (SELECT 'RULE_FAILED' AS DECISION_REASON_CODE FROM dual) s
ON (t.DECISION_REASON_CODE = s.DECISION_REASON_CODE)
WHEN MATCHED THEN
  UPDATE SET
    t.TITLE_FA = 'عدم احراز قاعده',
    t.TITLE_EN = 'Rule Failed',
    t.DESCRIPTION = 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.',
    t.DISPLAY_ORDER = 30,
    t.IS_ACTIVE = 1,
    t.UPDATED_AT = SYSTIMESTAMP,
    t.UPDATED_BY = 'DEP_OPEN_REF_SEED',
    t.RECORD_VERSION = NVL(t.RECORD_VERSION, 0) + 1
  WHERE (NVL(t.TITLE_FA, CHR(0)) <> 'عدم احراز قاعده' OR
       NVL(t.TITLE_EN, CHR(0)) <> 'Rule Failed' OR
       NVL(t.DESCRIPTION, CHR(0)) <> 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.' OR
       NVL(t.DISPLAY_ORDER, -999999999) <> 30 OR
       NVL(t.IS_ACTIVE, -999999999) <> 1)
WHEN NOT MATCHED THEN
  INSERT (DECISION_REASON_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_AT, CREATED_BY, RECORD_VERSION)
  VALUES ('RULE_FAILED', 'عدم احراز قاعده', 'Rule Failed', 'Seed مرجع مطابق Package «Deposit Account Opening - Reference Data» در XMI افتتاح حساب سپرده.', 30, 1, SYSTIMESTAMP, 'DEP_OPEN_REF_SEED', 1);

-- SKIPPED: REF_DEP_OPEN_CHANNEL_ORG_MAP
-- Reason: کد واحد سازمانی پیش‌فرض وابسته به Master واحدهای سازمانی بانک و تنظیمات محیط است.

-- --------------------------------------------------------------------------
-- REF_DEP_OPEN_BATCH_ERROR_CODE
-- Source authority: Phase 6 Batch Opening migration. These are prototype
-- provisional technical codes and may later be governed by the bank taxonomy.
-- --------------------------------------------------------------------------
MERGE INTO DPS2.REF_DEP_OPEN_BATCH_ERROR_CODE t
USING (
  SELECT 'ROW_INVALID' code, 'ردیف نامعتبر' fa, 'Invalid row' en, 'Prototype provisional code aligned with supplied Deposit Account Opening HTML.' descr, 'VALIDATION' stage, 10 ord FROM DUAL UNION ALL
  SELECT 'PROCESSING_FAILED', 'خطای پردازش ردیف', 'Item processing failed', 'Prototype provisional technical code; final bank taxonomy remains Data-Governance owned.', 'PROCESSING', 20 FROM DUAL UNION ALL
  SELECT 'ACTIVATION_FAILED', 'خطای فعال‌سازی حساب', 'Account activation failed', 'Prototype provisional technical code; final bank taxonomy remains Data-Governance owned.', 'POSTING', 30 FROM DUAL
) s
ON (t.ERROR_CODE=s.code)
WHEN MATCHED THEN UPDATE SET
  t.TITLE_FA=s.fa, t.TITLE_EN=s.en, t.DESCRIPTION=s.descr,
  t.ERROR_STAGE_CODE=s.stage, t.DISPLAY_ORDER=s.ord, t.IS_ACTIVE=1,
  t.UPDATED_AT=SYSTIMESTAMP, t.UPDATED_BY='DEP_OPEN_REF_SEED',
  t.RECORD_VERSION=NVL(t.RECORD_VERSION,0)+1
WHERE NVL(t.TITLE_FA,CHR(0))<>s.fa OR NVL(t.TITLE_EN,CHR(0))<>s.en
   OR NVL(t.DESCRIPTION,CHR(0))<>s.descr OR NVL(t.ERROR_STAGE_CODE,CHR(0))<>s.stage
   OR NVL(t.DISPLAY_ORDER,-999999999)<>s.ord OR NVL(t.IS_ACTIVE,-1)<>1
WHEN NOT MATCHED THEN INSERT
  (ERROR_CODE,TITLE_FA,TITLE_EN,DESCRIPTION,ERROR_STAGE_CODE,DISPLAY_ORDER,IS_ACTIVE,CREATED_AT,CREATED_BY,RECORD_VERSION)
VALUES
  (s.code,s.fa,s.en,s.descr,s.stage,s.ord,1,SYSTIMESTAMP,'DEP_OPEN_REF_SEED',1);

-- ============================================================================
-- Governance / Environment-dependent catalog
-- REF_DEP_OPEN_CHANNEL_ORG_MAP intentionally remains unpopulated because
-- DEFAULT_ORG_UNIT_CODE must reference an approved bank organization-unit code.
-- Do not invent a virtual branch or organization code in a portable seed.
-- ============================================================================
-- Environment-specific template ONLY:
-- MERGE INTO DPS2.REF_DEP_OPEN_CHANNEL_ORG_MAP t
-- USING (SELECT 'INTERNET' MAP_CODE FROM dual) s
-- ON (t.MAP_CODE=s.MAP_CODE)
-- WHEN NOT MATCHED THEN INSERT
--   (MAP_CODE,TITLE_FA,TITLE_EN,DESCRIPTION,CHANNEL_CODE,DEFAULT_ORG_UNIT_CODE,OVERRIDE_ALLOWED,DISPLAY_ORDER,IS_ACTIVE,CREATED_AT,CREATED_BY,RECORD_VERSION)
-- VALUES
--   ('INTERNET','اینترنت‌بانک','Internet Banking','Map to approved virtual branch','INTERNET','<BANK_ORG_UNIT_CODE>',0,10,1,SYSTIMESTAMP,'DEP_OPEN_REF_SEED',1);

COMMIT;

PROMPT === Seed verification counts ===
SELECT 'REF_DEP_OPEN_OPERATION' TABLE_NAME, COUNT(*) ROW_COUNT FROM DPS2.REF_DEP_OPEN_OPERATION
UNION ALL
SELECT 'REF_DEP_OPEN_SNAPSHOT_TYPE' TABLE_NAME, COUNT(*) ROW_COUNT FROM DPS2.REF_DEP_OPEN_SNAPSHOT_TYPE
UNION ALL
SELECT 'REF_DEP_OPEN_BATCH_SOURCE_TYPE' TABLE_NAME, COUNT(*) ROW_COUNT FROM DPS2.REF_DEP_OPEN_BATCH_SOURCE_TYPE
UNION ALL
SELECT 'REF_DEP_OPEN_FIELD_CHANGE_TYPE' TABLE_NAME, COUNT(*) ROW_COUNT FROM DPS2.REF_DEP_OPEN_FIELD_CHANGE_TYPE
UNION ALL
SELECT 'REF_DEP_OPEN_CHECK' TABLE_NAME, COUNT(*) ROW_COUNT FROM DPS2.REF_DEP_OPEN_CHECK
UNION ALL
SELECT 'REF_DEP_OPEN_BATCH_STATUS' TABLE_NAME, COUNT(*) ROW_COUNT FROM DPS2.REF_DEP_OPEN_BATCH_STATUS
UNION ALL
SELECT 'REF_DEP_OPEN_ACCEPTANCE_SOURCE' TABLE_NAME, COUNT(*) ROW_COUNT FROM DPS2.REF_DEP_OPEN_ACCEPTANCE_SOURCE
UNION ALL
SELECT 'REF_DEP_OPEN_CHANNEL_SCOPE' TABLE_NAME, COUNT(*) ROW_COUNT FROM DPS2.REF_DEP_OPEN_CHANNEL_SCOPE
UNION ALL
SELECT 'REF_DEP_OPEN_FIRST_PAYMENT_RULE' TABLE_NAME, COUNT(*) ROW_COUNT FROM DPS2.REF_DEP_OPEN_FIRST_PAYMENT_RULE
UNION ALL
SELECT 'REF_DEP_OPEN_VERIFICATION_STATUS' TABLE_NAME, COUNT(*) ROW_COUNT FROM DPS2.REF_DEP_OPEN_VERIFICATION_STATUS
UNION ALL
SELECT 'REF_DEP_OPEN_ENROLLMENT_STATUS' TABLE_NAME, COUNT(*) ROW_COUNT FROM DPS2.REF_DEP_OPEN_ENROLLMENT_STATUS
UNION ALL
SELECT 'REF_DEP_OPEN_REQUEST_STATUS' TABLE_NAME, COUNT(*) ROW_COUNT FROM DPS2.REF_DEP_OPEN_REQUEST_STATUS
UNION ALL
SELECT 'REF_DEP_OPEN_TAX_STATUS_SOURCE' TABLE_NAME, COUNT(*) ROW_COUNT FROM DPS2.REF_DEP_OPEN_TAX_STATUS_SOURCE
UNION ALL
SELECT 'REF_DEP_OPEN_FREQUENCY' TABLE_NAME, COUNT(*) ROW_COUNT FROM DPS2.REF_DEP_OPEN_FREQUENCY
UNION ALL
SELECT 'REF_DEP_OPEN_ACTION_STATUS' TABLE_NAME, COUNT(*) ROW_COUNT FROM DPS2.REF_DEP_OPEN_ACTION_STATUS
UNION ALL
SELECT 'REF_DEP_OPEN_SIGNATURE_RULE' TABLE_NAME, COUNT(*) ROW_COUNT FROM DPS2.REF_DEP_OPEN_SIGNATURE_RULE
UNION ALL
SELECT 'REF_DEP_OPEN_PAYMENT_INSTRUMENT' TABLE_NAME, COUNT(*) ROW_COUNT FROM DPS2.REF_DEP_OPEN_PAYMENT_INSTRUMENT
UNION ALL
SELECT 'REF_DEP_OPEN_SERVICE' TABLE_NAME, COUNT(*) ROW_COUNT FROM DPS2.REF_DEP_OPEN_SERVICE
UNION ALL
SELECT 'REF_DEP_OPEN_DOCUMENT_STATUS' TABLE_NAME, COUNT(*) ROW_COUNT FROM DPS2.REF_DEP_OPEN_DOCUMENT_STATUS
UNION ALL
SELECT 'REF_DEP_OPEN_FUNDING_STATUS' TABLE_NAME, COUNT(*) ROW_COUNT FROM DPS2.REF_DEP_OPEN_FUNDING_STATUS
UNION ALL
SELECT 'REF_DEP_OPEN_PAYMENT_DESTINATION' TABLE_NAME, COUNT(*) ROW_COUNT FROM DPS2.REF_DEP_OPEN_PAYMENT_DESTINATION
UNION ALL
SELECT 'REF_DEP_OPEN_CHECK_TYPE' TABLE_NAME, COUNT(*) ROW_COUNT FROM DPS2.REF_DEP_OPEN_CHECK_TYPE
UNION ALL
SELECT 'REF_DEP_OPEN_PRICING_OVERRIDE_TYPE' TABLE_NAME, COUNT(*) ROW_COUNT FROM DPS2.REF_DEP_OPEN_PRICING_OVERRIDE_TYPE
UNION ALL
SELECT 'REF_DEP_OPEN_CHECK_RESULT' TABLE_NAME, COUNT(*) ROW_COUNT FROM DPS2.REF_DEP_OPEN_CHECK_RESULT
UNION ALL
SELECT 'REF_DEP_OPEN_ACCESS_ROLE' TABLE_NAME, COUNT(*) ROW_COUNT FROM DPS2.REF_DEP_OPEN_ACCESS_ROLE
UNION ALL
SELECT 'REF_DEP_OPEN_AUTHORITY_SCOPE' TABLE_NAME, COUNT(*) ROW_COUNT FROM DPS2.REF_DEP_OPEN_AUTHORITY_SCOPE
UNION ALL
SELECT 'REF_DEP_OPEN_AUDIT_ACTOR_TYPE' TABLE_NAME, COUNT(*) ROW_COUNT FROM DPS2.REF_DEP_OPEN_AUDIT_ACTOR_TYPE
UNION ALL
SELECT 'REF_DEP_OPEN_SIGNATORY_ROLE' TABLE_NAME, COUNT(*) ROW_COUNT FROM DPS2.REF_DEP_OPEN_SIGNATORY_ROLE
UNION ALL
SELECT 'REF_DEP_OPEN_SOURCE_OF_FUNDS' TABLE_NAME, COUNT(*) ROW_COUNT FROM DPS2.REF_DEP_OPEN_SOURCE_OF_FUNDS
UNION ALL
SELECT 'REF_DEP_OPEN_DECISION_REASON' TABLE_NAME, COUNT(*) ROW_COUNT FROM DPS2.REF_DEP_OPEN_DECISION_REASON
UNION ALL
SELECT 'REF_DEP_OPEN_BATCH_ITEM_STATUS' TABLE_NAME, COUNT(*) ROW_COUNT FROM DPS2.REF_DEP_OPEN_BATCH_ITEM_STATUS
UNION ALL
SELECT 'REF_DEP_OPEN_PAYMENT_DAY_RULE' TABLE_NAME, COUNT(*) ROW_COUNT FROM DPS2.REF_DEP_OPEN_PAYMENT_DAY_RULE
UNION ALL
SELECT 'REF_DEP_OPEN_PARTY_ROLE' TABLE_NAME, COUNT(*) ROW_COUNT FROM DPS2.REF_DEP_OPEN_PARTY_ROLE
UNION ALL
SELECT 'REF_DEP_OPEN_MATURITY_ACTION' TABLE_NAME, COUNT(*) ROW_COUNT FROM DPS2.REF_DEP_OPEN_MATURITY_ACTION
UNION ALL
SELECT 'REF_DEP_OPEN_OWNERSHIP_TYPE' TABLE_NAME, COUNT(*) ROW_COUNT FROM DPS2.REF_DEP_OPEN_OWNERSHIP_TYPE
UNION ALL
SELECT 'REF_DEP_OPEN_REQUEST_TYPE' TABLE_NAME, COUNT(*) ROW_COUNT FROM DPS2.REF_DEP_OPEN_REQUEST_TYPE
UNION ALL
SELECT 'REF_DEP_OPEN_AUTHORITY_LEVEL' TABLE_NAME, COUNT(*) ROW_COUNT FROM DPS2.REF_DEP_OPEN_AUTHORITY_LEVEL
UNION ALL
SELECT 'REF_DEP_OPEN_DELEGATION_TYPE' TABLE_NAME, COUNT(*) ROW_COUNT FROM DPS2.REF_DEP_OPEN_DELEGATION_TYPE
UNION ALL
SELECT 'REF_DEP_OPEN_DECISION' TABLE_NAME, COUNT(*) ROW_COUNT FROM DPS2.REF_DEP_OPEN_DECISION
UNION ALL
SELECT 'REF_DEP_OPEN_AUDIT_EVENT_TYPE' TABLE_NAME, COUNT(*) ROW_COUNT FROM DPS2.REF_DEP_OPEN_AUDIT_EVENT_TYPE
UNION ALL
SELECT 'REF_DEP_OPEN_PROFIT_CALC_METHOD' TABLE_NAME, COUNT(*) ROW_COUNT FROM DPS2.REF_DEP_OPEN_PROFIT_CALC_METHOD
UNION ALL
SELECT 'REF_DEP_OPEN_BATCH_ERROR_STAGE' TABLE_NAME, COUNT(*) ROW_COUNT FROM DPS2.REF_DEP_OPEN_BATCH_ERROR_STAGE
UNION ALL
SELECT 'REF_DEP_OPEN_CHANNEL' TABLE_NAME, COUNT(*) ROW_COUNT FROM DPS2.REF_DEP_OPEN_CHANNEL
UNION ALL
SELECT 'REF_DEP_OPEN_ACCEPTANCE_STATUS' TABLE_NAME, COUNT(*) ROW_COUNT FROM DPS2.REF_DEP_OPEN_ACCEPTANCE_STATUS
UNION ALL
SELECT 'REF_DEP_OPEN_DOCUMENT_TYPE' TABLE_NAME, COUNT(*) ROW_COUNT FROM DPS2.REF_DEP_OPEN_DOCUMENT_TYPE
UNION ALL
SELECT 'REF_DEP_OPEN_HOLIDAY_ADJUSTMENT' TABLE_NAME, COUNT(*) ROW_COUNT FROM DPS2.REF_DEP_OPEN_HOLIDAY_ADJUSTMENT
UNION ALL
SELECT 'REF_DEP_OPEN_BENEFICIARY_TYPE' TABLE_NAME, COUNT(*) ROW_COUNT FROM DPS2.REF_DEP_OPEN_BENEFICIARY_TYPE
UNION ALL
SELECT 'REF_DEP_OPEN_PURPOSE' TABLE_NAME, COUNT(*) ROW_COUNT FROM DPS2.REF_DEP_OPEN_PURPOSE
UNION ALL
SELECT 'REF_DEP_OPEN_CHANNEL_ORG_MAP' TABLE_NAME, COUNT(*) ROW_COUNT FROM DPS2.REF_DEP_OPEN_CHANNEL_ORG_MAP
UNION ALL
SELECT 'REF_DEP_OPEN_INSTRUCTION_SOURCE' TABLE_NAME, COUNT(*) ROW_COUNT FROM DPS2.REF_DEP_OPEN_INSTRUCTION_SOURCE
UNION ALL
SELECT 'REF_DEP_OPEN_CHANGE_TYPE' TABLE_NAME, COUNT(*) ROW_COUNT FROM DPS2.REF_DEP_OPEN_CHANGE_TYPE
UNION ALL
SELECT 'REF_DEP_OPEN_TAX_RESIDENCY' TABLE_NAME, COUNT(*) ROW_COUNT FROM DPS2.REF_DEP_OPEN_TAX_RESIDENCY
UNION ALL
SELECT 'REF_DEP_OPEN_CHANGE_STATUS' TABLE_NAME, COUNT(*) ROW_COUNT FROM DPS2.REF_DEP_OPEN_CHANGE_STATUS
UNION ALL
SELECT 'REF_DEP_OPEN_CHANGE_REASON' TABLE_NAME, COUNT(*) ROW_COUNT FROM DPS2.REF_DEP_OPEN_CHANGE_REASON
UNION ALL
SELECT 'REF_DEP_OPEN_FUNDING_METHOD' TABLE_NAME, COUNT(*) ROW_COUNT FROM DPS2.REF_DEP_OPEN_FUNDING_METHOD
UNION ALL
SELECT 'REF_DEP_OPEN_WITHDRAWAL_MEDIA' TABLE_NAME, COUNT(*) ROW_COUNT FROM DPS2.REF_DEP_OPEN_WITHDRAWAL_MEDIA
UNION ALL
SELECT 'REF_DEP_OPEN_DAY_COUNT_BASIS' TABLE_NAME, COUNT(*) ROW_COUNT FROM DPS2.REF_DEP_OPEN_DAY_COUNT_BASIS
UNION ALL
SELECT 'REF_DEP_OPEN_BATCH_ERROR_CODE' TABLE_NAME, COUNT(*) ROW_COUNT FROM DPS2.REF_DEP_OPEN_BATCH_ERROR_CODE
UNION ALL
SELECT 'REF_DEP_OPEN_TERM_UNIT' TABLE_NAME, COUNT(*) ROW_COUNT FROM DPS2.REF_DEP_OPEN_TERM_UNIT
UNION ALL
SELECT 'REF_DEP_OPEN_CHECK_PHASE' TABLE_NAME, COUNT(*) ROW_COUNT FROM DPS2.REF_DEP_OPEN_CHECK_PHASE
UNION ALL
SELECT 'REF_DEP_OPEN_BLOCKING_SCOPE' TABLE_NAME, COUNT(*) ROW_COUNT FROM DPS2.REF_DEP_OPEN_BLOCKING_SCOPE
UNION ALL
SELECT 'REF_DEP_OPEN_OBLIGATION_TYPE' TABLE_NAME, COUNT(*) ROW_COUNT FROM DPS2.REF_DEP_OPEN_OBLIGATION_TYPE
UNION ALL
SELECT 'REF_DEP_OPEN_SETTLEMENT_STATUS' TABLE_NAME, COUNT(*) ROW_COUNT FROM DPS2.REF_DEP_OPEN_SETTLEMENT_STATUS
UNION ALL
SELECT 'REF_DEP_OPEN_ACTIVATION_STATUS' TABLE_NAME, COUNT(*) ROW_COUNT FROM DPS2.REF_DEP_OPEN_ACTIVATION_STATUS
UNION ALL
SELECT 'REF_DEP_OPEN_JOINT_BASIS' TABLE_NAME, COUNT(*) ROW_COUNT FROM DPS2.REF_DEP_OPEN_JOINT_BASIS
UNION ALL
SELECT 'REF_DEP_OPEN_FUND_PURPOSE' TABLE_NAME, COUNT(*) ROW_COUNT FROM DPS2.REF_DEP_OPEN_FUND_PURPOSE
ORDER BY TABLE_NAME;

PROMPT === Seed completed successfully ===
PROMPT REF_DEP_OPEN_CHANNEL_ORG_MAP may remain empty until bank org-unit mapping is approved.
