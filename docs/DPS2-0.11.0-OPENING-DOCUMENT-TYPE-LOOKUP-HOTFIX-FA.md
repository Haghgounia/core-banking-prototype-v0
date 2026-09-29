# DPS2 0.11.0 — Opening Document Type Lookup Hotfix

## مسئله
در Step 5 فرم افتتاح موردی، فیلد «نوع مدرک» به‌صورت input آزاد پیاده‌سازی شده بود و داده‌های جدول `DPS2.REF_DEP_OPEN_DOCUMENT_TYPE` را مصرف نمی‌کرد. بنابراین وجود داده در جدول پایه باعث نمایش لیست در Wizard نمی‌شد.

## اصلاح
- Resource رسمی `dps2-document-type` از Generic Reference API مصرف می‌شود.
- فیلد `documentType` از input آزاد به `mat-select` تبدیل شد.
- مقدار ذخیره‌شونده همان `DOCUMENT_TYPE_CODE` است و عنوان فارسی برای کاربر نمایش داده می‌شود.
- Generic Lookup فقط رکوردهای `IS_ACTIVE = 1` را برمی‌گرداند.
- حالت loading، خطای lookup و retry در UI قابل مشاهده است.
- هیچ DDL، Business DML یا تغییر API جدیدی اضافه نشده است.
- Hotfix جستجوی Party قبلی حفظ شده است.

## بررسی سریع API
```bat
curl "http://localhost:8091/api/v1/reference/dps2-document-type/lookup?limit=5000"
```
اگر خروجی خالی باشد، `IS_ACTIVE` رکوردهای جدول را بررسی کنید.

## اعمال
Patch را روی ریشه پروژه Extract/Replace کنید و سپس `build-production.cmd` را اجرا کنید.
