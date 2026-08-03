# استخراج DDL و داده Oracle

این ابزار برای استخراج ساختار و داده جدول‌های Oracle از داخل همان برنامه ساخته شده است.

## اجرای پیش‌فرض

پس از Build پروژه:

```cmd
bin\export-database.cmd
```

مقادیر پیش‌فرض:

```text
Schema       : DPS
Table prefix : REF_
Output       : database\oracle\exports
```

Batch پیش از اتصال به Database و شروع استخراج، مشخصات عملیات را نمایش می‌دهد و تأیید `Y/N` می‌گیرد.

## پارامترهای اختیاری

```cmd
bin\export-database.cmd [SCHEMA] [TABLE_PREFIX] [OUTPUT_DIRECTORY]
```

نمونه:

```cmd
bin\export-database.cmd GEO REF_ D:\db-export
```

برای استخراج همه جدول‌های یک Schema، به‌جای Prefix مقدار `*` ارسال شود:

```cmd
bin\export-database.cmd DPS * D:\db-export
```

## ساختار خروجی

برای هر اجرا یک پوشه Timestampدار ایجاد می‌شود. این مسیر Generated است و توسط `.gitignore` وارد Git نمی‌شود:

```text
database/oracle/exports/
└── yyyyMMdd-HHmmss/
    └── dps/
        ├── manifest.txt
        ├── ddl/
        │   └── REF_TABLE_ddl.sql
        └── data/
            └── REF_TABLE_data.sql
```

فایل DDL هر جدول می‌تواند شامل موارد زیر باشد:

- Sequenceهای متصل به Default ستون‌ها و Sequence هم‌نام `SEQ_<TABLE>`
- `CREATE TABLE`
- Indexها
- Primary Key، Unique و Check Constraintها
- Foreign Keyها
- Comment جدول و ستون‌ها
- Triggerها
- Object Grantها

فایل داده، هر رکورد را به شکل زیر تولید می‌کند:

```sql
INSERT INTO DPS.REF_TABLE (COL1, COL2, COL3) VALUES (...);
```

در انتهای فایل دارای داده، دستور `COMMIT` قرار می‌گیرد. رشته‌ها با UTF-8 ذخیره می‌شوند، علامت نقل‌قول Escape می‌شود و `SET DEFINE OFF` از تفسیر `&` توسط SQL*Plus جلوگیری می‌کند.

## انواع داده پشتیبانی‌شده

- `CHAR`, `VARCHAR2`, `NCHAR`, `NVARCHAR2`, `LONG`
- `NUMBER`, `DECIMAL`, `INTEGER`, `FLOAT`, `BINARY_FLOAT`, `BINARY_DOUBLE`
- `DATE`
- `TIMESTAMP`, `TIMESTAMP WITH TIME ZONE`, `TIMESTAMP WITH LOCAL TIME ZONE`
- `RAW`, `ROWID`, `UROWID`
- `CLOB`, `NCLOB`

در صورت وجود نوع داده پشتیبانی‌نشده مانند `BLOB` یا Object Type سفارشی، عملیات با پیام مشخص متوقف می‌شود تا فایل ناقص به‌عنوان خروجی موفق شناخته نشود.

## دسترسی Oracle

اطلاعات اتصال از متغیرهای محیطی `ORACLE_URL`، `ORACLE_USERNAME` و `ORACLE_PASSWORD` خوانده می‌شود. کاربر معرفی‌شده باید دسترسی خواندن جدول‌ها و Viewهای Data Dictionary مورد استفاده را داشته باشد. برای استخراج DDL نیز دسترسی اجرای `DBMS_METADATA` و مشاهده Objectهای Schema هدف لازم است.
