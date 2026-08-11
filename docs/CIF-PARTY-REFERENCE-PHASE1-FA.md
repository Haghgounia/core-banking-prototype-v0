# CIF Party / Customer Reference Data — Phase 1

## منبع

این فاز از مدل تعبیه‌شده در فایل `Party_Customer_Reference_Data_Interactive_Forms_FA.html` تولید شده است. مدل اصلی 104 جدول مرجع، 1041 فیلد، 416 Seed Row و 6 رابطه صریح دارد.

## محدوده این نسخه

برای جلوگیری از ایجاد Reference Data موازی و متناقض با داده‌های موجود پروژه، در نسخه 0.3.2 همه 104 جدول فعال نشده‌اند. محدوده فعال شامل این موارد است:

- تمام 31 جدول Package `Identity and Party`
- جدول `REF_LEGAL_CAPACITY` به‌عنوان وابستگی مستقیم فرم `CIF.PERSON`

در مجموع 32 فرم CRUD و 123 Seed Row فعال شده‌اند. وضعیت تصمیم‌گیری برای تمام 104 جدول منبع در `docs/PARTY-REFERENCE-MAPPING-ALL-104.csv` ثبت شده است؛ 32 جدول Active و 72 جدول Deferred هستند.

## دلیل عدم فعال‌سازی یکباره 104 جدول

پروژه از قبل Reference Data واقعی در Schemaهای دیگر دارد؛ از جمله `GEO.COUNTRIES`، `GEO.LANGUAGES`، `GEO.CURRENCIES`، `GEO.PROVINCES` و `GEO.CITIES`. بنابراین ساخت هم‌زمان `CIF.REF_COUNTRY`، `CIF.REF_LANGUAGE` و موارد مشابه می‌تواند دو Source of Truth ایجاد کند. این جداول در فاز Mapping بعدی یا به منابع موجود متصل می‌شوند یا فقط در صورت نیاز واقعی ساخته خواهند شد.

همچنین در مدل ارسالی، `REF_VERIFICATION_STATUS` مقدار `NOT_VERIFIED` دارد، در حالی که DDL عملیاتی CIF در چند جدول از `UNVERIFIED` استفاده می‌کند. تا قبل از تعیین Mapping/Normalization رسمی، `VERIFICATION_STATUS_CODE` به این جدول متصل نشده است.

## اتصال‌های انجام‌شده به Customer 360

در فرم `PERSON` این فیلدها از Reference Data جدید تغذیه می‌شوند:

- `GENDER_CODE` ← `CIF.REF_GENDER`
- `MARITAL_STATUS_CODE` ← `CIF.REF_MARITAL_STATUS`
- `LEGAL_CAPACITY_CODE` ← `CIF.REF_LEGAL_CAPACITY`
- `RESIDENCE_STATUS_CODE` ← `CIF.REF_RESIDENCE_STATUS`
- `LIFE_STATUS_CODE` ← `CIF.REF_LIFE_STATUS`
- `DATA_QUALITY_STATUS_CODE` ← `CIF.REF_DATA_QUALITY_STATUS`

کشور و زبان همچنان از منابع موجود GEO خوانده می‌شوند.

در سایر فرم‌های Customer 360 نیز `PARTY_LIFECYCLE_STATUS`، `PARTY_STATUS_REASON`، `NAME_TYPE`، `IDENTIFIER_TYPE` و `DOCUMENT_TYPE` به Reference Data این فاز متصل شده‌اند.

در این فاز برای ستون‌های عملیاتی CIF، Foreign Key جدید به جداول `REF_*` اضافه نشده است؛ اتصال فعلاً در سطح Lookup و Validation فرم انجام می‌شود تا قبل از بررسی داده‌های موجود، Migration محدودکننده‌ای به مدل عملیاتی تحمیل نشود.

## API جدید

```text
GET    /api/v1/cif/reference/catalog
GET    /api/v1/cif/reference/{resource}/descriptor
GET    /api/v1/cif/reference/{resource}
GET    /api/v1/cif/reference/{resource}/lookup
GET    /api/v1/cif/reference/{resource}/{key}
POST   /api/v1/cif/reference/{resource}
PUT    /api/v1/cif/reference/{resource}/{key}
DELETE /api/v1/cif/reference/{resource}/{key}
```

این API برخلاف Reference Engine قدیمی، به `NUMBER ID + SEQUENCE` وابسته نیست و PKهای متنی و کلید مرکب `REF_CLASSIFICATION_VALUE` را پشتیبانی می‌کند.

## نصب Oracle

فایل‌های زیر اجرا شوند:

```text
database/oracle/cif/reference-data/identity-party/01-ddl.sql
database/oracle/cif/reference-data/identity-party/02-seed.sql
database/oracle/cif/reference-data/identity-party/03-verify.sql
```

کاربر `CIF` باید روی `TS_CIF` و `ITS_CIF` Quota کافی داشته باشد.
