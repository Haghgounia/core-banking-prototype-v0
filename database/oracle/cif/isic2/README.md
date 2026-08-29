# CIF ISIC2 — مدل مستقل نسخه‌محور ISIC

این بسته مدل ISIC را بر پایه دو جدول مستقل نسخه/فعالیت و یک View جستجو نصب می‌کند.

## آبجکت‌ها

- `CIF.REF_ISIC_RELEASE` — نسخه‌ها، Revisionها و Variantهای ISIC
- `CIF.REF_ISIC_ACTIVITY2` — درخت فعالیت‌های اقتصادی با Parent ID، سطح و عنوان دو‌زبانه اجباری
- `CIF.V_REF_ISIC_ACTIVITY_LOOKUP2` — View مناسب Lookup و نمایش عنوان فارسی

## طراحی فعالیت

هر فعالیت به یک Release تعلق دارد. رابطه سلسله‌مراتبی با `PARENT_ACTIVITY_ID` برقرار می‌شود و FK مرکب تضمین می‌کند والد و فرزند در یک Release باشند. سطح‌ها به‌ترتیب `SECTION=1`، `DIVISION=2`، `GROUP=3`، `CLASS=4` و `NATIONAL_SUBCLASS=5` هستند.

`NAME_FA` و `NAME_EN` اجباری‌اند. Seed اولیه UNSD Rev.4 برای هر 766 رکورد عنوان فارسی و انگلیسی دارد. عنوان انگلیسی بر پایه ساختار مرجع ورودی و عنوان فارسی ترجمه پروژه بانکی است، بنابراین `TRANSLATION_STATUS_CODE='BANK_TRANSLATED'` ثبت می‌شود.

فیلدهای Description/Inclusions/Exclusions فقط در صورت وجود منبع معتبر پر می‌شوند و در Seed ساختاری خالی می‌مانند.

## نصب

از SQLcl/SQL*Plus و از همین پوشه:

```sql
@00-install-isic2.sql
```

نصب Prototype، دو جدول ISIC2 را Drop/Recreate می‌کند. برای اجرای مرحله‌ای:

```sql
@00-reset-isic2-redesign.sql
@01-create-isic2-tables.sql
@02-import-isic-rev4-unsd.sql
@03-register-ir-sci-release.sql
@04-verify-isic2.sql
```

Seed شماره 02 شامل 766 رکورد Rev.4 است: 21 Section، 88 Division، 238 Group و 419 Class. اسکریپت شماره 03 فقط Release مربوط به `IR-SCI` را به صورت `DRAFT/Inactive` ثبت می‌کند و هیچ داده ملی ساختگی ایجاد نمی‌کند.
