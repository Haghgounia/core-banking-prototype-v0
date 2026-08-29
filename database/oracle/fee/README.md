# FEE schema — Baseline 1.0

این مسیر مدل فیزیکی و Seed Data ماژول کارمزد را بر اساس فایل‌های Oracle پیوست نگهداری می‌کند.

## مدل جاری
- 47 جدول در Schema `FEE`
- 47 Sequence متناظر با PKها
- 574 رکورد Seed پایه
- 52 دامنه مرجع و 307 مقدار مرجع
- شناسه‌های Seed منفی؛ شناسه‌های جدید برنامه از Sequenceهای مثبت تولید می‌شوند.

## نصب
1. Schema و Tablespaceهای `FEE / TS_FEE / ITS_FEE` باید قبلاً توسط DBA ایجاد شده باشند.
2. اجرای DDL: `@install-baseline-1.0-ddl.sql`
3. اجرای Seed: `@install-baseline-1.0-seed.sql`
4. کنترل Seed: `@verify-baseline-1.0.sql`

فایل `install-ddl.sql` برای سازگاری با دستور قدیمی نگه داشته شده و به installer جدید 47 جدولی هدایت می‌شود.

## ساختار
- `baseline-1.0/ddl/`: نسخه عیناً کپی‌شده 47 فایل DDL پیوست
- `baseline-1.0/seed/`: بسته Seed Data پیوست
- `legacy-phase1-21-tables/`: مدل 21 جدولی قدیمی؛ فقط جهت تاریخچه و **نباید برای نصب جدید استفاده شود**.

## UI
نسخه 0.3.66 برای هر 47 جدول فرم Metadata-driven دارد. جداول Configuration/Reference قابل ثبت و ویرایش‌اند؛ 12 جدول Runtime/Audit در فرم عمومی فقط خواندنی هستند تا داده اجرایی به‌صورت دستی تغییر نکند.
