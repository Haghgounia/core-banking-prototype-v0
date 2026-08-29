# FEE 0.3.66 / FIX77 — Baseline 1.0 Forms QA

## مبنا
این تغییر مستقیماً از دو بسته پیوست کاربر ساخته شده است:
- 47 فایل Oracle DDL برای Schema `FEE`
- `FEE-SeedData-Baseline-1.0-Oracle` با 574 رکورد Seed

هیچ جدول یا ستون کسب‌وکاری خارج از این دو منبع به مدل FEE اضافه نشده است.

## پوشش فرم
- 47/47 جدول در کاتالوگ Backend ثبت شده‌اند.
- شش گروه UI دقیقاً با تقسیم‌بندی Seed ساخته شده‌اند.
- 35 جدول Reference/Configuration/Arrangement قابل CRUD هستند.
- 12 جدول Runtime/Audit فقط خواندنی هستند.
- Simulator قبلی در `/fee/simulator` حفظ شده است.

## قرارداد Metadata-driven
Backend برای هر جدول از Oracle موارد زیر را می‌خواند:
- PK تک‌ستونی
- FK و Parent Table/Column
- Check Constraintهای ساده `IN (...)`
- Comment فارسی ستون
- Data Type / Length / Precision / Scale / Nullability / Default

UI بر همین اساس نوع کنترل را انتخاب می‌کند: FK/Domain/Check => Select، DATE/TIMESTAMP => Date Picker شمسی، CLOB => Textarea، NUMBER => Numeric Input.

## Seed
Manifest پیوست اعلام می‌کند:
- table_count = 47
- seeded_table_count = 47
- total_seed_rows = 574

Seed IDها منفی هستند. درج جدید UI از `FEE.SEQ_<TABLE>.NEXTVAL` استفاده می‌کند و بنابراین با Seedها برخورد نمی‌کند.

## نصب Oracle
1. `database/oracle/fee/install-baseline-1.0-ddl.sql`
2. `database/oracle/fee/install-baseline-1.0-seed.sql`
3. `database/oracle/fee/verify-baseline-1.0.sql`

DDLهای منفرد عین بسته پیوست در `baseline-1.0/ddl` نگهداری شده‌اند. Installer فقط ترتیب اجرای آنها را بر اساس FKها تعیین می‌کند.

## کنترل استاتیک
`tools/verify-fee-admin-baseline.mjs` این موارد را کنترل می‌کند:
- 47 DDL
- 47 جدول Seed شده
- 574 رکورد Seed
- 47 Entry یکتای Catalog
- 12 فرم Runtime فقط خواندنی
- Route/API/UI و Persian Date Picker
- وجود Installerهای DDL/Seed

## محدودیت Build در محیط تولید Release
Maven Wrapper در محیط تولید این Release به دانلود Maven 3.9.16 نیاز داشت و دسترسی شبکه موجود نبود؛ بنابراین Maven Compile کامل در این محیط اجرا نشد. صحت ساخت نهایی باید روی Windows پروژه با `build-production.cmd` تأیید شود. Verifierهای استاتیک مستقل از Maven اجرا شده‌اند.
