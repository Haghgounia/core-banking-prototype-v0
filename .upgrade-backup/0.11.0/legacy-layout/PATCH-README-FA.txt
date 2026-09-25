DPS2 0.11.0 - Phase 11N-C R9
Early Termination Status-History Reason Reconciliation Hotfix

علت:
DEPOSIT_ACCOUNT_CLOSURE برای فسخ پیش از موعد REASON_CODE=TERM_EARLY_TERMINATION را الزام می‌کند،
اما CHECK جدول immutable DEPOSIT_ACCOUNT_STATUS_HISTORY این reason مستند را نداشت و Execute با 409 DATA_CONFLICT متوقف می‌شد.

اصلاح:
- reconcile کردن CHK_DEPOSIT_ACCOUNT_STATUS_HISTORY_REASON_CODE و افزودن TERM_EARLY_TERMINATION
- بدون INSERT/UPDATE/DELETE/MERGE روی business data
- DB verifier مستقل برای کنترل constraint جدید
- Static verifier جدید: 72/72 PASS

نصب:
ZIP را روی root پروژه Extract/Replace کنید و tools\qualify-dps2-phase11nc.cmd را اجرا کنید.
