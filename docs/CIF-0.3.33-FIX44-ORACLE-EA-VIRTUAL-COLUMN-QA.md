# Core Banking Prototype 0.3.33 — FIX44 Oracle → EA XMI Data Dictionary Compatibility

## گزارش خطای Runtime
در نسخه 0.3.32، Preview/Export فرم Oracle → Enterprise Architect هنگام خواندن ستون‌های Schema با خطای زیر متوقف می‌شد:

`ORA-00904: "C"."VIRTUAL_COLUMN": invalid identifier`

Query مشکل‌دار `VIRTUAL_COLUMN` را از `ALL_TAB_COLUMNS` می‌خواند.

## علت
در Oracle، `ALL_TAB_COLUMNS` نمای فیلترشده ستون‌های قابل مشاهده است و ستون `VIRTUAL_COLUMN` را ارائه نمی‌کند. فلگ `VIRTUAL_COLUMN` در `ALL_TAB_COLS` قرار دارد.

بنابراین ترکیب صحیح برای Exporter این است:
- مشخصات اصلی ستون و ترتیب/Nullability/Datatype از `ALL_TAB_COLUMNS`؛
- فلگ Virtual از `ALL_TAB_COLS` با Join روی `OWNER + TABLE_NAME + COLUMN_NAME`.

## اصلاح Backend
هر دو مسیر زیر اصلاح شدند:
1. خواندن ستون‌های اصلی Schema (`loadColumns`)
2. خواندن ستون‌های Reference Stub خارج از Scope (`loadReferencedTableStub`)

الگوی جدید:

```sql
SELECT C.TABLE_NAME,
       C.COLUMN_NAME,
       ...,
       C.IDENTITY_COLUMN,
       COALESCE(TC.VIRTUAL_COLUMN, 'NO') AS VIRTUAL_COLUMN
FROM ALL_TAB_COLUMNS C
LEFT JOIN ALL_TAB_COLS TC
  ON TC.OWNER = C.OWNER
 AND TC.TABLE_NAME = C.TABLE_NAME
 AND TC.COLUMN_NAME = C.COLUMN_NAME
...
```

`ALL_TAB_COLUMNS` همچنان Source اصلی است تا ستون‌های Hidden/System-generated ناخواسته وارد XMI نشوند.

## اصلاح UX
در 0.3.32 بلافاصله بعد از دریافت Configuration، `preview()` خودکار اجرا می‌شد. این رفتار باعث می‌شد کاربر خطای Metadata را به انتخاب Schema نسبت دهد و همچنین برای Schema پیش‌فرض بدون درخواست کاربر Scan کامل انجام شود.

در 0.3.33:
- انتخاب Schema فقط مقدار فرم را تغییر می‌دهد؛
- Scan دیتابیس فقط با «پیش‌نمایش Metadata» یا «تولید و دانلود XML استاندارد EA» اجرا می‌شود.

## Regression Controls
- Unit test جدید: `OracleEaMetadataInspectorDictionarySqlTest`
  - هر دو Query باید `ALL_TAB_COLS TC` داشته باشند؛
  - هر دو Query باید `TC.VIRTUAL_COLUMN` استفاده کنند؛
  - استفاده مجدد از `C.VIRTUAL_COLUMN` مجاز نیست.
- Static exporter verifier از 20 به 22 کنترل افزایش یافت:
  - Virtual-column metadata uses ALL_TAB_COLS
  - No automatic metadata preview on page initialization

## Static QA Result
- CIF persisted-grid verification: PASS
- EA/Oracle comparison verification: PASS
- Oracle → EA XMI exporter verification: 22/22 PASS

## Database Migration
نیاز ندارد. این Fix فقط Queryهای Data Dictionary و رفتار UI را اصلاح می‌کند.

## نسخه مورد انتظار
- UI/Release: `0.3.33-prototype-fee-p1`
- Backend JAR: `0.3.33-SNAPSHOT`
