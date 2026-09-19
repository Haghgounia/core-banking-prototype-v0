-- Core Banking Prototype 0.9.3
-- DPS2 Deposit Opening Reference Data + FK Reconciliation
--
-- Purpose
--   1) Restore source-defined Phase 5 / Phase 6 reference values that are empty
--      in environments where the historical migrations were not applied.
--   2) Ensure the six semantic code relationships around Request / Decision /
--      Terms Acceptance point to the correct lookup tables.
--   3) Remove only the three specifically-known wrong XMI FK mappings if they
--      exist; no unrelated constraint is touched.
--
-- Safety
--   * All objects are explicitly qualified with DPS2.
--   * Safe to rerun.
--   * Existing business rows are not modified.
--   * FK creation is blocked if orphan code values exist.
--   * REF_DEP_OPEN_CHANNEL_ORG_MAP remains environment-governed and is not seeded.
--
-- Recommended execution: SQL Developer -> Run Script (F5)

SET DEFINE OFF;
SET SERVEROUTPUT ON;
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK;

PROMPT ============================================================
PROMPT DPS2 Reference Data + FK Reconciliation 0.9.3
PROMPT ============================================================

SELECT SYS_CONTEXT('USERENV','SESSION_USER') AS SESSION_USER,
       SYS_CONTEXT('USERENV','CURRENT_SCHEMA') AS CURRENT_SCHEMA
  FROM DUAL;

PROMPT === Preflight required tables ===
DECLARE
  l_missing NUMBER;
BEGIN
  SELECT COUNT(*)
    INTO l_missing
    FROM (
      SELECT 'DEPOSIT_OPENING_REQUEST' table_name FROM dual
      UNION ALL SELECT 'DEPOSIT_OPENING_DECISION' FROM dual
      UNION ALL SELECT 'DEPOSIT_OPENING_TERMS_ACCEPTANCE' FROM dual
      UNION ALL SELECT 'REF_DEP_OPEN_REQUEST_TYPE' FROM dual
      UNION ALL SELECT 'REF_DEP_OPEN_REQUEST_STATUS' FROM dual
      UNION ALL SELECT 'REF_DEP_OPEN_DECISION' FROM dual
      UNION ALL SELECT 'REF_DEP_OPEN_DECISION_REASON' FROM dual
      UNION ALL SELECT 'REF_DEP_OPEN_ACCEPTANCE_SOURCE' FROM dual
      UNION ALL SELECT 'REF_DEP_OPEN_ACCEPTANCE_STATUS' FROM dual
      UNION ALL SELECT 'REF_DEP_OPEN_SNAPSHOT_TYPE' FROM dual
      UNION ALL SELECT 'REF_DEP_OPEN_AUDIT_ACTOR_TYPE' FROM dual
      UNION ALL SELECT 'REF_DEP_OPEN_AUDIT_EVENT_TYPE' FROM dual
      UNION ALL SELECT 'REF_DEP_OPEN_CHANGE_REASON' FROM dual
      UNION ALL SELECT 'REF_DEP_OPEN_BATCH_ERROR_STAGE' FROM dual
      UNION ALL SELECT 'REF_DEP_OPEN_BATCH_ERROR_CODE' FROM dual
    ) e
   WHERE NOT EXISTS (
     SELECT 1
       FROM all_tables t
      WHERE t.owner='DPS2'
        AND t.table_name=e.table_name
   );

  IF l_missing > 0 THEN
    RAISE_APPLICATION_ERROR(-20931, 'One or more required DPS2 Deposit Opening tables are missing.');
  END IF;
  DBMS_OUTPUT.PUT_LINE('OK  : required DPS2 tables exist.');
END;
/

PROMPT === Reconcile lookup values used by correct FKs ===

-- Request Type
MERGE INTO DPS2.REF_DEP_OPEN_REQUEST_TYPE t
USING (
  SELECT 'CUSTOMER_REQUEST' code, 'درخواست مشتری' fa, 'Customer Request' en, 10 ord FROM dual UNION ALL
  SELECT 'STAFF_ASSISTED', 'ثبت با کمک کاربر شعبه', 'Staff Assisted', 20 FROM dual UNION ALL
  SELECT 'BULK', 'گروهی', 'Bulk', 30 FROM dual
) s
ON (t.REQUEST_TYPE_CODE=s.code)
WHEN MATCHED THEN UPDATE SET t.TITLE_FA=s.fa,t.TITLE_EN=s.en,t.DISPLAY_ORDER=s.ord,t.IS_ACTIVE=1,t.UPDATED_AT=SYSTIMESTAMP,t.UPDATED_BY='MIGRATION_0.9.3'
WHEN NOT MATCHED THEN INSERT (REQUEST_TYPE_CODE,TITLE_FA,TITLE_EN,DESCRIPTION,DISPLAY_ORDER,IS_ACTIVE,CREATED_AT,CREATED_BY,RECORD_VERSION)
VALUES (s.code,s.fa,s.en,'Source-defined Deposit Opening request type.',s.ord,1,SYSTIMESTAMP,'MIGRATION_0.9.3',1);

-- Request Status
MERGE INTO DPS2.REF_DEP_OPEN_REQUEST_STATUS t
USING (
  SELECT 'DRAFT' code, 'پیش‌نویس' fa, 'Draft' en, 10 ord FROM dual UNION ALL
  SELECT 'SUBMITTED', 'ارسال‌شده', 'Submitted', 20 FROM dual UNION ALL
  SELECT 'APPROVED', 'تأییدشده', 'Approved', 30 FROM dual UNION ALL
  SELECT 'REJECTED', 'ردشده', 'Rejected', 40 FROM dual UNION ALL
  SELECT 'COMPLETED', 'تکمیل‌شده', 'Completed', 50 FROM dual UNION ALL
  SELECT 'IN_REVIEW', 'در حال بررسی', 'In Review', 60 FROM dual UNION ALL
  SELECT 'RETURNED_FOR_CORRECTION', 'برگشت برای اصلاح', 'Returned For Correction', 70 FROM dual UNION ALL
  SELECT 'FAILED', 'ناموفق', 'Failed', 80 FROM dual UNION ALL
  SELECT 'CANCELLED', 'لغوشده', 'Cancelled', 90 FROM dual
) s
ON (t.REQUEST_STATUS_CODE=s.code)
WHEN MATCHED THEN UPDATE SET t.TITLE_FA=s.fa,t.TITLE_EN=s.en,t.DISPLAY_ORDER=s.ord,t.IS_ACTIVE=1,t.UPDATED_AT=SYSTIMESTAMP,t.UPDATED_BY='MIGRATION_0.9.3'
WHEN NOT MATCHED THEN INSERT (REQUEST_STATUS_CODE,TITLE_FA,TITLE_EN,DESCRIPTION,DISPLAY_ORDER,IS_ACTIVE,CREATED_AT,CREATED_BY,RECORD_VERSION)
VALUES (s.code,s.fa,s.en,'Source/model-consistency Deposit Opening request status.',s.ord,1,SYSTIMESTAMP,'MIGRATION_0.9.3',1);

-- Decision
MERGE INTO DPS2.REF_DEP_OPEN_DECISION t
USING (
  SELECT 'APPROVE' code, 'تأیید' fa, 'Approve' en, 10 ord FROM dual UNION ALL
  SELECT 'REJECT', 'رد' fa, 'Reject' en, 20 FROM dual UNION ALL
  SELECT 'REFER', 'ارجاع' fa, 'Refer' en, 30 FROM dual UNION ALL
  SELECT 'RETURN', 'بازگشت برای اصلاح' fa, 'Return' en, 40 FROM dual
) s
ON (t.DECISION_CODE=s.code)
WHEN MATCHED THEN UPDATE SET t.TITLE_FA=s.fa,t.TITLE_EN=s.en,t.DISPLAY_ORDER=s.ord,t.IS_ACTIVE=1,t.UPDATED_AT=SYSTIMESTAMP,t.UPDATED_BY='MIGRATION_0.9.3'
WHEN NOT MATCHED THEN INSERT (DECISION_CODE,TITLE_FA,TITLE_EN,DESCRIPTION,DISPLAY_ORDER,IS_ACTIVE,CREATED_AT,CREATED_BY,RECORD_VERSION)
VALUES (s.code,s.fa,s.en,'Deposit Opening decision catalog.',s.ord,1,SYSTIMESTAMP,'MIGRATION_0.9.3',1);

-- Decision Reason
MERGE INTO DPS2.REF_DEP_OPEN_DECISION_REASON t
USING (
  SELECT 'ALL_CHECKS_PASSED' code, 'عبور از همه کنترل‌ها' fa, 'All Checks Passed' en, 10 ord FROM dual UNION ALL
  SELECT 'MANUAL_REVIEW', 'بررسی دستی', 'Manual Review', 20 FROM dual UNION ALL
  SELECT 'RULE_FAILED', 'عدم احراز قاعده', 'Rule Failed', 30 FROM dual
) s
ON (t.DECISION_REASON_CODE=s.code)
WHEN MATCHED THEN UPDATE SET t.TITLE_FA=s.fa,t.TITLE_EN=s.en,t.DISPLAY_ORDER=s.ord,t.IS_ACTIVE=1,t.UPDATED_AT=SYSTIMESTAMP,t.UPDATED_BY='MIGRATION_0.9.3'
WHEN NOT MATCHED THEN INSERT (DECISION_REASON_CODE,TITLE_FA,TITLE_EN,DESCRIPTION,DISPLAY_ORDER,IS_ACTIVE,CREATED_AT,CREATED_BY,RECORD_VERSION)
VALUES (s.code,s.fa,s.en,'Deposit Opening decision reason catalog.',s.ord,1,SYSTIMESTAMP,'MIGRATION_0.9.3',1);

-- Acceptance Source
MERGE INTO DPS2.REF_DEP_OPEN_ACCEPTANCE_SOURCE t
USING (
  SELECT 'UI' code, 'رابط کاربری' fa, 'UI' en, 10 ord FROM dual UNION ALL
  SELECT 'API', 'رابط برنامه‌نویسی API', 'API', 20 FROM dual UNION ALL
  SELECT 'BRANCH', 'شعبه', 'Branch', 30 FROM dual UNION ALL
  SELECT 'BATCH', 'پردازش گروهی', 'Batch', 40 FROM dual
) s
ON (t.ACCEPTANCE_SOURCE_CODE=s.code)
WHEN MATCHED THEN UPDATE SET t.TITLE_FA=s.fa,t.TITLE_EN=s.en,t.DISPLAY_ORDER=s.ord,t.IS_ACTIVE=1,t.UPDATED_AT=SYSTIMESTAMP,t.UPDATED_BY='MIGRATION_0.9.3'
WHEN NOT MATCHED THEN INSERT (ACCEPTANCE_SOURCE_CODE,TITLE_FA,TITLE_EN,DESCRIPTION,DISPLAY_ORDER,IS_ACTIVE,CREATED_AT,CREATED_BY,RECORD_VERSION)
VALUES (s.code,s.fa,s.en,'Deposit Opening terms acceptance source.',s.ord,1,SYSTIMESTAMP,'MIGRATION_0.9.3',1);

-- Acceptance Status
MERGE INTO DPS2.REF_DEP_OPEN_ACCEPTANCE_STATUS t
USING (
  SELECT 'ACCEPTED' code, 'پذیرفته‌شده' fa, 'Accepted' en, 10 ord FROM dual UNION ALL
  SELECT 'DECLINED', 'نپذیرفته / رد پذیرش', 'Declined', 20 FROM dual UNION ALL
  SELECT 'REVOKED', 'لغو پذیرش', 'Revoked', 30 FROM dual
) s
ON (t.ACCEPTANCE_STATUS_CODE=s.code)
WHEN MATCHED THEN UPDATE SET t.TITLE_FA=s.fa,t.TITLE_EN=s.en,t.DISPLAY_ORDER=s.ord,t.IS_ACTIVE=1,t.UPDATED_AT=SYSTIMESTAMP,t.UPDATED_BY='MIGRATION_0.9.3'
WHEN NOT MATCHED THEN INSERT (ACCEPTANCE_STATUS_CODE,TITLE_FA,TITLE_EN,DESCRIPTION,DISPLAY_ORDER,IS_ACTIVE,CREATED_AT,CREATED_BY,RECORD_VERSION)
VALUES (s.code,s.fa,s.en,'Deposit Opening terms acceptance status.',s.ord,1,SYSTIMESTAMP,'MIGRATION_0.9.3',1);

PROMPT === Restore Phase 5 audit catalogs found empty in target database ===

MERGE INTO DPS2.REF_DEP_OPEN_AUDIT_ACTOR_TYPE t
USING (
  SELECT 'USER' code, 'کاربر' fa, 'User' en, 'عامل انسانی ثبت‌کننده عملیات Opening' descr, 10 ord FROM dual UNION ALL
  SELECT 'SYSTEM', 'سامانه', 'System', 'عامل سیستمی برای عملیات خودکار و یکپارچه‌سازی', 20 FROM dual
) s
ON (t.ACTOR_TYPE_CODE=s.code)
WHEN MATCHED THEN UPDATE SET t.TITLE_FA=s.fa,t.TITLE_EN=s.en,t.DESCRIPTION=s.descr,t.DISPLAY_ORDER=s.ord,t.IS_ACTIVE=1,t.UPDATED_AT=SYSTIMESTAMP,t.UPDATED_BY='MIGRATION_0.9.3'
WHEN NOT MATCHED THEN INSERT (ACTOR_TYPE_CODE,TITLE_FA,TITLE_EN,DESCRIPTION,DISPLAY_ORDER,IS_ACTIVE,CREATED_AT,CREATED_BY,RECORD_VERSION)
VALUES (s.code,s.fa,s.en,s.descr,s.ord,1,SYSTIMESTAMP,'MIGRATION_0.9.3',1);

MERGE INTO DPS2.REF_DEP_OPEN_AUDIT_EVENT_TYPE t
USING (
  SELECT 'CREATE' code, 'ایجاد پرونده' fa, 'Create opening' en, 10 ord FROM dual UNION ALL
  SELECT 'CHANGE_REQUESTED', 'درخواست تغییر', 'Change requested', 20 FROM dual UNION ALL
  SELECT 'CHANGE_APPROVED', 'تأیید تغییر', 'Change approved', 30 FROM dual UNION ALL
  SELECT 'CHANGE_REJECTED', 'رد تغییر', 'Change rejected', 40 FROM dual UNION ALL
  SELECT 'CHANGE_APPLIED', 'اعمال تغییر', 'Change applied', 50 FROM dual UNION ALL
  SELECT 'ACCOUNT_LINKED', 'اتصال حساب ایجادشده', 'Created account linked', 60 FROM dual UNION ALL
  SELECT 'STATUS_CHANGE', 'تغییر وضعیت', 'Status changed', 70 FROM dual
) s
ON (t.EVENT_TYPE_CODE=s.code)
WHEN MATCHED THEN UPDATE SET t.TITLE_FA=s.fa,t.TITLE_EN=s.en,t.DESCRIPTION='Phase 5 controlled Opening audit event',t.DISPLAY_ORDER=s.ord,t.IS_ACTIVE=1,t.UPDATED_AT=SYSTIMESTAMP,t.UPDATED_BY='MIGRATION_0.9.3'
WHEN NOT MATCHED THEN INSERT (EVENT_TYPE_CODE,TITLE_FA,TITLE_EN,DESCRIPTION,DISPLAY_ORDER,IS_ACTIVE,CREATED_AT,CREATED_BY,RECORD_VERSION)
VALUES (s.code,s.fa,s.en,'Phase 5 controlled Opening audit event',s.ord,1,SYSTIMESTAMP,'MIGRATION_0.9.3',1);

MERGE INTO DPS2.REF_DEP_OPEN_SNAPSHOT_TYPE t
USING (
  SELECT 'SUBMITTED' code, 'تصویر زمان ارسال' fa, 'Submitted snapshot' en, 10 ord FROM dual UNION ALL
  SELECT 'APPROVED', 'تصویر زمان تأیید', 'Approved snapshot', 20 FROM dual UNION ALL
  SELECT 'COMPLETED', 'تصویر زمان تکمیل', 'Completed snapshot', 30 FROM dual UNION ALL
  SELECT 'PRE_CHANGE', 'تصویر قبل از تغییر', 'Pre-change snapshot', 40 FROM dual UNION ALL
  SELECT 'POST_CHANGE', 'تصویر بعد از تغییر', 'Post-change snapshot', 50 FROM dual
) s
ON (t.SNAPSHOT_TYPE_CODE=s.code)
WHEN MATCHED THEN UPDATE SET t.TITLE_FA=s.fa,t.TITLE_EN=s.en,t.DESCRIPTION='Phase 5 immutable canonical Opening snapshot type',t.DISPLAY_ORDER=s.ord,t.IS_ACTIVE=1,t.UPDATED_AT=SYSTIMESTAMP,t.UPDATED_BY='MIGRATION_0.9.3'
WHEN NOT MATCHED THEN INSERT (SNAPSHOT_TYPE_CODE,TITLE_FA,TITLE_EN,DESCRIPTION,DISPLAY_ORDER,IS_ACTIVE,CREATED_AT,CREATED_BY,RECORD_VERSION)
VALUES (s.code,s.fa,s.en,'Phase 5 immutable canonical Opening snapshot type',s.ord,1,SYSTIMESTAMP,'MIGRATION_0.9.3',1);

-- XMI and application logic explicitly use OTHER and require a note with it.
MERGE INTO DPS2.REF_DEP_OPEN_CHANGE_REASON t
USING (SELECT 'OTHER' code, 'سایر' fa, 'Other' en, 10 ord FROM dual) s
ON (t.CHANGE_REASON_CODE=s.code)
WHEN MATCHED THEN UPDATE SET t.TITLE_FA=s.fa,t.TITLE_EN=s.en,t.DESCRIPTION='علت عمومی برای موارد خارج از کاتالوگ؛ تکمیل CHANGE_REASON_NOTE الزامی است.',t.DISPLAY_ORDER=s.ord,t.IS_ACTIVE=1,t.UPDATED_AT=SYSTIMESTAMP,t.UPDATED_BY='MIGRATION_0.9.3'
WHEN NOT MATCHED THEN INSERT (CHANGE_REASON_CODE,TITLE_FA,TITLE_EN,DESCRIPTION,DISPLAY_ORDER,IS_ACTIVE,CREATED_AT,CREATED_BY,RECORD_VERSION)
VALUES (s.code,s.fa,s.en,'علت عمومی برای موارد خارج از کاتالوگ؛ تکمیل CHANGE_REASON_NOTE الزامی است.',s.ord,1,SYSTIMESTAMP,'MIGRATION_0.9.3',1);

PROMPT === Reconcile Phase 6 provisional batch error taxonomy ===
MERGE INTO DPS2.REF_DEP_OPEN_BATCH_ERROR_STAGE t
USING (
  SELECT 'VALIDATION' code, 'اعتبارسنجی' fa, 'Validation' en, 10 ord FROM dual UNION ALL
  SELECT 'PROCESSING', 'پردازش', 'Processing', 20 FROM dual UNION ALL
  SELECT 'POSTING', 'ثبت/فعال‌سازی نتیجه', 'Posting', 30 FROM dual
) s
ON (t.ERROR_STAGE_CODE=s.code)
WHEN MATCHED THEN UPDATE SET t.TITLE_FA=s.fa,t.TITLE_EN=s.en,t.DISPLAY_ORDER=s.ord,t.IS_ACTIVE=1,t.UPDATED_AT=SYSTIMESTAMP,t.UPDATED_BY='MIGRATION_0.9.3'
WHEN NOT MATCHED THEN INSERT (ERROR_STAGE_CODE,TITLE_FA,TITLE_EN,DISPLAY_ORDER,IS_ACTIVE,CREATED_AT,CREATED_BY,RECORD_VERSION)
VALUES (s.code,s.fa,s.en,s.ord,1,SYSTIMESTAMP,'MIGRATION_0.9.3',1);

MERGE INTO DPS2.REF_DEP_OPEN_BATCH_ERROR_CODE t
USING (
  SELECT 'ROW_INVALID' code, 'ردیف نامعتبر' fa, 'Invalid row' en, 'Prototype provisional code aligned with supplied Deposit Account Opening HTML.' descr, 'VALIDATION' stage, 10 ord FROM dual UNION ALL
  SELECT 'PROCESSING_FAILED', 'خطای پردازش ردیف', 'Item processing failed', 'Prototype provisional technical code; final bank taxonomy remains Data-Governance owned.', 'PROCESSING', 20 FROM dual UNION ALL
  SELECT 'ACTIVATION_FAILED', 'خطای فعال‌سازی حساب', 'Account activation failed', 'Prototype provisional technical code; final bank taxonomy remains Data-Governance owned.', 'POSTING', 30 FROM dual
) s
ON (t.ERROR_CODE=s.code)
WHEN MATCHED THEN UPDATE SET t.TITLE_FA=s.fa,t.TITLE_EN=s.en,t.DESCRIPTION=s.descr,t.ERROR_STAGE_CODE=s.stage,t.DISPLAY_ORDER=s.ord,t.IS_ACTIVE=1,t.UPDATED_AT=SYSTIMESTAMP,t.UPDATED_BY='MIGRATION_0.9.3'
WHEN NOT MATCHED THEN INSERT (ERROR_CODE,TITLE_FA,TITLE_EN,DESCRIPTION,ERROR_STAGE_CODE,DISPLAY_ORDER,IS_ACTIVE,CREATED_AT,CREATED_BY,RECORD_VERSION)
VALUES (s.code,s.fa,s.en,s.descr,s.stage,s.ord,1,SYSTIMESTAMP,'MIGRATION_0.9.3',1);

COMMIT;

PROMPT === Reconcile the six correct Foreign Keys ===
DECLARE
  FUNCTION relation_fk_name(
      p_src_table VARCHAR2,
      p_src_col   VARCHAR2,
      p_tgt_table VARCHAR2,
      p_tgt_col   VARCHAR2
  ) RETURN VARCHAR2 IS
      l_name VARCHAR2(128);
  BEGIN
      SELECT constraint_name
        INTO l_name
        FROM (
          SELECT c.constraint_name
            FROM all_constraints c
            JOIN all_cons_columns sc
              ON sc.owner=c.owner AND sc.constraint_name=c.constraint_name
            JOIN all_constraints r
              ON r.owner=c.r_owner AND r.constraint_name=c.r_constraint_name
            JOIN all_cons_columns rc
              ON rc.owner=r.owner AND rc.constraint_name=r.constraint_name AND rc.position=sc.position
           WHERE c.owner='DPS2'
             AND c.constraint_type='R'
             AND c.table_name=UPPER(p_src_table)
             AND sc.column_name=UPPER(p_src_col)
             AND r.owner='DPS2'
             AND r.table_name=UPPER(p_tgt_table)
             AND rc.column_name=UPPER(p_tgt_col)
           ORDER BY c.constraint_name
        )
       WHERE ROWNUM=1;
      RETURN l_name;
  EXCEPTION
      WHEN NO_DATA_FOUND THEN RETURN NULL;
  END;

  PROCEDURE drop_wrong_fk(
      p_src_table VARCHAR2,
      p_src_col   VARCHAR2,
      p_tgt_table VARCHAR2,
      p_tgt_col   VARCHAR2
  ) IS
      l_name VARCHAR2(128);
  BEGIN
      l_name := relation_fk_name(p_src_table,p_src_col,p_tgt_table,p_tgt_col);
      IF l_name IS NOT NULL THEN
          EXECUTE IMMEDIATE 'ALTER TABLE DPS2.'||p_src_table||' DROP CONSTRAINT '||l_name;
          DBMS_OUTPUT.PUT_LINE('DROP: known wrong FK '||l_name||' on '||p_src_table||'.'||p_src_col);
      END IF;
  END;

  PROCEDURE ensure_fk(
      p_name      VARCHAR2,
      p_src_table VARCHAR2,
      p_src_col   VARCHAR2,
      p_tgt_table VARCHAR2,
      p_tgt_col   VARCHAR2
  ) IS
      l_existing VARCHAR2(128);
      l_orphans  NUMBER;
      l_sql      VARCHAR2(4000);
  BEGIN
      l_existing := relation_fk_name(p_src_table,p_src_col,p_tgt_table,p_tgt_col);
      IF l_existing IS NOT NULL THEN
          DBMS_OUTPUT.PUT_LINE('OK  : '||p_src_table||'.'||p_src_col||' -> '||p_tgt_table||'.'||p_tgt_col||' ('||l_existing||')');
          RETURN;
      END IF;

      l_sql := 'SELECT COUNT(*) FROM DPS2.'||p_src_table||' s WHERE s.'||p_src_col||' IS NOT NULL AND NOT EXISTS ('||
               'SELECT 1 FROM DPS2.'||p_tgt_table||' t WHERE t.'||p_tgt_col||'=s.'||p_src_col||')';
      EXECUTE IMMEDIATE l_sql INTO l_orphans;
      IF l_orphans > 0 THEN
          RAISE_APPLICATION_ERROR(-20932, 'Cannot add '||p_name||': orphan values='||l_orphans||' in '||p_src_table||'.'||p_src_col);
      END IF;

      EXECUTE IMMEDIATE 'ALTER TABLE DPS2.'||p_src_table||' ADD CONSTRAINT '||p_name||
                        ' FOREIGN KEY ('||p_src_col||') REFERENCES DPS2.'||p_tgt_table||' ('||p_tgt_col||')';
      DBMS_OUTPUT.PUT_LINE('ADD : '||p_name||'  '||p_src_table||'.'||p_src_col||' -> '||p_tgt_table||'.'||p_tgt_col);
  END;
BEGIN
  -- Remove only the three explicitly-identified bad XMI mappings, if present.
  drop_wrong_fk('DEPOSIT_OPENING_REQUEST','REQUEST_TYPE_CODE','REF_DEP_OPEN_REQUEST_STATUS','REQUEST_STATUS_CODE');
  drop_wrong_fk('DEPOSIT_OPENING_DECISION','DECISION_CODE','REF_DEP_OPEN_DECISION_REASON','DECISION_REASON_CODE');
  drop_wrong_fk('DEPOSIT_OPENING_TERMS_ACCEPTANCE','ACCEPTANCE_SOURCE_CODE','REF_DEP_OPEN_ACCEPTANCE_STATUS','ACCEPTANCE_STATUS_CODE');

  -- Install complete semantic relationships for the affected code pairs.
  ensure_fk('FK_DEP_OPEN_REQ_REQUEST_TYPE',
            'DEPOSIT_OPENING_REQUEST','REQUEST_TYPE_CODE','REF_DEP_OPEN_REQUEST_TYPE','REQUEST_TYPE_CODE');
  ensure_fk('FK_DEP_OPEN_REQ_REQUEST_STATUS',
            'DEPOSIT_OPENING_REQUEST','REQUEST_STATUS_CODE','REF_DEP_OPEN_REQUEST_STATUS','REQUEST_STATUS_CODE');
  ensure_fk('FK_DEP_OPEN_DEC_DECISION',
            'DEPOSIT_OPENING_DECISION','DECISION_CODE','REF_DEP_OPEN_DECISION','DECISION_CODE');
  ensure_fk('FK_DEP_OPEN_DEC_REASON',
            'DEPOSIT_OPENING_DECISION','DECISION_REASON_CODE','REF_DEP_OPEN_DECISION_REASON','DECISION_REASON_CODE');
  ensure_fk('FK_DEP_OPEN_TERMS_ACC_SOURCE',
            'DEPOSIT_OPENING_TERMS_ACCEPTANCE','ACCEPTANCE_SOURCE_CODE','REF_DEP_OPEN_ACCEPTANCE_SOURCE','ACCEPTANCE_SOURCE_CODE');
  ensure_fk('FK_DEP_OPEN_TERMS_ACC_STATUS',
            'DEPOSIT_OPENING_TERMS_ACCEPTANCE','ACCEPTANCE_STATUS_CODE','REF_DEP_OPEN_ACCEPTANCE_STATUS','ACCEPTANCE_STATUS_CODE');
END;
/

COMMIT;

PROMPT ============================================================
PROMPT Verification - restored catalogs
PROMPT ============================================================
SELECT 'REF_DEP_OPEN_SNAPSHOT_TYPE' table_name, COUNT(*) row_count FROM DPS2.REF_DEP_OPEN_SNAPSHOT_TYPE
UNION ALL SELECT 'REF_DEP_OPEN_AUDIT_ACTOR_TYPE', COUNT(*) FROM DPS2.REF_DEP_OPEN_AUDIT_ACTOR_TYPE
UNION ALL SELECT 'REF_DEP_OPEN_AUDIT_EVENT_TYPE', COUNT(*) FROM DPS2.REF_DEP_OPEN_AUDIT_EVENT_TYPE
UNION ALL SELECT 'REF_DEP_OPEN_CHANGE_REASON', COUNT(*) FROM DPS2.REF_DEP_OPEN_CHANGE_REASON
UNION ALL SELECT 'REF_DEP_OPEN_BATCH_ERROR_CODE', COUNT(*) FROM DPS2.REF_DEP_OPEN_BATCH_ERROR_CODE
UNION ALL SELECT 'REF_DEP_OPEN_CHANNEL_ORG_MAP', COUNT(*) FROM DPS2.REF_DEP_OPEN_CHANNEL_ORG_MAP
ORDER BY 1;

PROMPT ============================================================
PROMPT Verification - six corrected FK relationships
PROMPT ============================================================
SELECT c.table_name,
       c.constraint_name,
       sc.column_name,
       r.table_name referenced_table,
       rc.column_name referenced_column,
       c.status
  FROM all_constraints c
  JOIN all_cons_columns sc
    ON sc.owner=c.owner AND sc.constraint_name=c.constraint_name
  JOIN all_constraints r
    ON r.owner=c.r_owner AND r.constraint_name=c.r_constraint_name
  JOIN all_cons_columns rc
    ON rc.owner=r.owner AND rc.constraint_name=r.constraint_name AND rc.position=sc.position
 WHERE c.owner='DPS2'
   AND c.constraint_type='R'
   AND (
        (c.table_name='DEPOSIT_OPENING_REQUEST' AND sc.column_name IN ('REQUEST_TYPE_CODE','REQUEST_STATUS_CODE'))
     OR (c.table_name='DEPOSIT_OPENING_DECISION' AND sc.column_name IN ('DECISION_CODE','DECISION_REASON_CODE'))
     OR (c.table_name='DEPOSIT_OPENING_TERMS_ACCEPTANCE' AND sc.column_name IN ('ACCEPTANCE_SOURCE_CODE','ACCEPTANCE_STATUS_CODE'))
   )
 ORDER BY c.table_name, sc.column_name;

PROMPT ============================================================
PROMPT SUCCESS: DPS2 Reference Data + FK Reconciliation 0.9.3 completed.
PROMPT NOTE: REF_DEP_OPEN_CHANNEL_ORG_MAP may remain empty by design until bank org-unit mapping is approved.
PROMPT ============================================================
