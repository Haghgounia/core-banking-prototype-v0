-- Core Banking Prototype
-- DPS2 / Four Deposits / Phase 5
-- Opening Audit & Change Management
-- Version: 0.5.0
--
-- Source contract:
--   DEPOSIT_OPENING_CHANGE_SET is change-controlled.
--   DEPOSIT_OPENING_AUDIT_EVENT, DEPOSIT_OPENING_AUDIT_FIELD_CHANGE,
--   DEPOSIT_OPENING_SNAPSHOT and DEPOSIT_OPENING_STATUS_HISTORY are append-only.
--
-- This migration does not create the supplied DPS2 tables; they are part of the
-- Deposit Account Opening schema. It supplies the prototype lifecycle codes used
-- by Phase 5 and database immutability guards for the append-only audit tables.

SET DEFINE OFF;

-------------------------------------------------------------------------------
-- Prototype reference contract used by Phase 5
-------------------------------------------------------------------------------

MERGE INTO DPS2.REF_DEP_OPEN_AUDIT_ACTOR_TYPE t
USING (SELECT 'USER' code, 'کاربر' fa, 'User' en, 10 ord FROM DUAL) s
ON (t.ACTOR_TYPE_CODE = s.code)
WHEN NOT MATCHED THEN INSERT
  (ACTOR_TYPE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_BY)
VALUES
  (s.code, s.fa, s.en, 'عامل انسانی ثبت‌کننده عملیات Opening', s.ord, 1, 'PHASE5_MIGRATION');

MERGE INTO DPS2.REF_DEP_OPEN_AUDIT_ACTOR_TYPE t
USING (SELECT 'SYSTEM' code, 'سامانه' fa, 'System' en, 20 ord FROM DUAL) s
ON (t.ACTOR_TYPE_CODE = s.code)
WHEN NOT MATCHED THEN INSERT
  (ACTOR_TYPE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_BY)
VALUES
  (s.code, s.fa, s.en, 'عامل سیستمی برای عملیات خودکار و یکپارچه‌سازی', s.ord, 1, 'PHASE5_MIGRATION');

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
ON (t.EVENT_TYPE_CODE = s.code)
WHEN NOT MATCHED THEN INSERT
  (EVENT_TYPE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_BY)
VALUES
  (s.code, s.fa, s.en, 'Phase 5 controlled Opening audit event', s.ord, 1, 'PHASE5_MIGRATION');

MERGE INTO DPS2.REF_DEP_OPEN_CHANGE_STATUS t
USING (
  SELECT 'DRAFT' code, 'پیش‌نویس' fa, 'Draft' en, 10 ord FROM DUAL UNION ALL
  SELECT 'APPROVED', 'تأییدشده', 'Approved', 20 FROM DUAL UNION ALL
  SELECT 'APPLIED', 'اعمال‌شده', 'Applied', 30 FROM DUAL UNION ALL
  SELECT 'REJECTED', 'ردشده', 'Rejected', 40 FROM DUAL
) s
ON (t.CHANGE_STATUS_CODE = s.code)
WHEN NOT MATCHED THEN INSERT
  (CHANGE_STATUS_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_BY)
VALUES
  (s.code, s.fa, s.en, 'Phase 5 Change Set lifecycle status', s.ord, 1, 'PHASE5_MIGRATION');

MERGE INTO DPS2.REF_DEP_OPEN_CHANGE_TYPE t
USING (
  SELECT 'CUSTOMER_CORRECTION' code, 'اصلاح به درخواست مشتری' fa, 'Customer correction' en, 10 ord FROM DUAL UNION ALL
  SELECT 'OPERATOR_CORRECTION', 'اصلاح کاربر عملیاتی', 'Operator correction', 20 FROM DUAL UNION ALL
  SELECT 'SYSTEM_CORRECTION', 'اصلاح سیستمی', 'System correction', 30 FROM DUAL UNION ALL
  SELECT 'COMPLIANCE_CORRECTION', 'اصلاح تطبیق', 'Compliance correction', 40 FROM DUAL UNION ALL
  SELECT 'DATA_QUALITY_CORRECTION', 'اصلاح کیفیت داده', 'Data quality correction', 50 FROM DUAL UNION ALL
  SELECT 'CONTROLLED_REOPEN', 'بازگشایی کنترل‌شده', 'Controlled reopen', 60 FROM DUAL
) s
ON (t.CHANGE_TYPE_CODE = s.code)
WHEN NOT MATCHED THEN INSERT
  (CHANGE_TYPE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_BY)
VALUES
  (s.code, s.fa, s.en, 'Phase 5 controlled Opening change type', s.ord, 1, 'PHASE5_MIGRATION');

MERGE INTO DPS2.REF_DEP_OPEN_FIELD_CHANGE_TYPE t
USING (
  SELECT 'SET' code, 'مقداردهی' fa, 'Set' en, 10 ord FROM DUAL UNION ALL
  SELECT 'UPDATE', 'تغییر مقدار', 'Update' en, 20 FROM DUAL UNION ALL
  SELECT 'CLEAR', 'پاک‌کردن مقدار', 'Clear' en, 30 FROM DUAL UNION ALL
  SELECT 'ADD', 'افزودن عضو', 'Add' en, 40 FROM DUAL UNION ALL
  SELECT 'REMOVE', 'حذف عضو', 'Remove' en, 50 FROM DUAL
) s
ON (t.FIELD_CHANGE_TYPE_CODE = s.code)
WHEN NOT MATCHED THEN INSERT
  (FIELD_CHANGE_TYPE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_BY)
VALUES
  (s.code, s.fa, s.en, 'Phase 5 field-level audit change type', s.ord, 1, 'PHASE5_MIGRATION');

MERGE INTO DPS2.REF_DEP_OPEN_SNAPSHOT_TYPE t
USING (
  SELECT 'SUBMITTED' code, 'تصویر زمان ارسال' fa, 'Submitted snapshot' en, 10 ord FROM DUAL UNION ALL
  SELECT 'APPROVED', 'تصویر زمان تأیید' fa, 'Approved snapshot' en, 20 FROM DUAL UNION ALL
  SELECT 'COMPLETED', 'تصویر زمان تکمیل' fa, 'Completed snapshot' en, 30 FROM DUAL UNION ALL
  SELECT 'PRE_CHANGE', 'تصویر قبل از تغییر' fa, 'Pre-change snapshot' en, 40 FROM DUAL UNION ALL
  SELECT 'POST_CHANGE', 'تصویر بعد از تغییر' fa, 'Post-change snapshot' en, 50 FROM DUAL
) s
ON (t.SNAPSHOT_TYPE_CODE = s.code)
WHEN NOT MATCHED THEN INSERT
  (SNAPSHOT_TYPE_CODE, TITLE_FA, TITLE_EN, DESCRIPTION, DISPLAY_ORDER, IS_ACTIVE, CREATED_BY)
VALUES
  (s.code, s.fa, s.en, 'Phase 5 immutable canonical Opening snapshot type', s.ord, 1, 'PHASE5_MIGRATION');

COMMIT;

-------------------------------------------------------------------------------
-- Append-only enforcement
-------------------------------------------------------------------------------

CREATE OR REPLACE TRIGGER DPS2.TRG_DEP_OPEN_AUD_EVT_APPEND_ONLY
BEFORE UPDATE OR DELETE ON DPS2.DEPOSIT_OPENING_AUDIT_EVENT
BEGIN
  RAISE_APPLICATION_ERROR(-20051, 'DEPOSIT_OPENING_AUDIT_EVENT is append-only');
END;
/

CREATE OR REPLACE TRIGGER DPS2.TRG_DEP_OPEN_AUD_FLD_APPEND_ONLY
BEFORE UPDATE OR DELETE ON DPS2.DEPOSIT_OPENING_AUDIT_FIELD_CHANGE
BEGIN
  RAISE_APPLICATION_ERROR(-20052, 'DEPOSIT_OPENING_AUDIT_FIELD_CHANGE is append-only');
END;
/

CREATE OR REPLACE TRIGGER DPS2.TRG_DEP_OPEN_SNAPSHOT_APPEND_ONLY
BEFORE UPDATE OR DELETE ON DPS2.DEPOSIT_OPENING_SNAPSHOT
BEGIN
  RAISE_APPLICATION_ERROR(-20053, 'DEPOSIT_OPENING_SNAPSHOT is append-only');
END;
/

CREATE OR REPLACE TRIGGER DPS2.TRG_DEP_OPEN_STATUS_APPEND_ONLY
BEFORE UPDATE OR DELETE ON DPS2.DEPOSIT_OPENING_STATUS_HISTORY
BEGIN
  RAISE_APPLICATION_ERROR(-20054, 'DEPOSIT_OPENING_STATUS_HISTORY is append-only');
END;
/

PROMPT Phase 5 Opening Audit & Change Management migration completed.
