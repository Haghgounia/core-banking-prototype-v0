# DPS2 0.9.2 — Phase 4 + Phase 9 Account Schema Reconciliation QA

## هدف
این نسخه، Source پروژه را با وضعیت واقعی Oracle پس از اجرای موفق Reconciliation حساب سپرده همگام می‌کند. در محیط عملیاتی مشاهده شد که Opening موجود بود اما Objectهای Phase 4 شامل `DEPOSIT_ACCOUNT` و `DEPOSIT_ACCOUNT_LIFECYCLE_EVENT` نصب نشده بودند. Migration 0.9.2 این پیش‌نیازها را به‌صورت Idempotent بازسازی و Phase 9 Closure را روی همان Contract تثبیت می‌کند.

## Contract نهایی
- `DPS2.DEPOSIT_ACCOUNT`: وضعیت‌های `PENDING_ACTIVATION`, `ACTIVE`, `CLOSED`.
- `DPS2.DEPOSIT_ACCOUNT_LIFECYCLE_EVENT`: Eventهای `CREATE`, `ACTIVATE`, `CLOSE`.
- `RECORD_VERSION`: کنترل Optimistic Concurrency برای Servicing.
- `TRG_DEP_ACCT_EVT_APPEND_ONLY`: جلوگیری از Update/Delete تاریخچه Lifecycle.
- `CREATED_ACCOUNT_ID`: Integration Reference در Opening بدون Cross-Domain FK فیزیکی.

## نتیجه تطبیق با کد
Repositoryهای Phase 4، Phase 8 و Phase 9 دقیقاً ستون‌ها، Sequenceها و Status/Eventهای موجود در Reconciliation را مصرف می‌کنند. Schema پیش‌فرض `deposit-account` نیز `DPS2` است.

## Source Layout Cleanup
- `README-FA.txt` قدیمی از Root به `docs/patches/PATCH-0.3.2-BUILD-FIX1-README-FA.txt` منتقل شد.
- `config/application.yml_` و `backend/src/main/resources/application.yml_` از Source فعال حذف/Archive می‌شوند.
- `package-release.cmd` عمداً در Root باقی می‌ماند؛ این فایل Entry Point رسمی Packaging است و `verify-runtime-artifact-contract.mjs` آن را کنترل می‌کند.
- Artifactهای Runtime/Generated شامل `app`, `logs`, `data/document-storage`, `frontend/.angular`, `backend/src/main/resources/static`, `database/oracle/exports` در Clean Source Package قرار نمی‌گیرند.

## کنترل ثابت
`tools/verify-dps2-account-schema-reconciliation-092.mjs` قرارداد Database/Application/Layout را کنترل می‌کند و در Build ویندوز و Unix اجرا می‌شود.
