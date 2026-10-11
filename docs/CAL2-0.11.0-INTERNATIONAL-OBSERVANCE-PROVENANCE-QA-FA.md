# CAL2 0.11.0 — International Observance Provenance Baseline

## هدف

ممیزی Runtime مورخ 2026-10-11 نشان داد جدول `CAL2.EVENT` دارای 244 رویداد فعال است که 60 مورد از نوع `INTERNATIONAL` هستند. این 60 رکورد دقیقاً با فهرست 60‌تایی مورد بررسی تطبیق دارند، اما provenance رسمی بین‌المللی در مدل رویداد ثبت نشده بود.

## یافته‌های Runtime

- `EVENT`: 244 رکورد، همگی فعال.
- `EVENT_RECURRENCE_RULE`: 242 قاعده فعال.
- `INTERNATIONAL`: 60 رویداد.
- 58 رویداد INTERNATIONAL قاعده فعال دارند.
- `INTL_WORLD_MARITIME_DAY` و `IR_WORLD_QUDS_DAY` بدون قاعده فعال‌اند و `VARIABLE_RULE_REQUIRED` دارند.
- تمام 242 قاعده فعال Runtime از نوع `ANNUAL_FIXED_DATE` هستند.
- 55 قاعده INTERNATIONAL به `IR_PUBLIC_CALENDAR_ALAMTO_1405` و 3 قاعده به `IR_CALENDAR_CENTER` متصل‌اند.
- هیچ Source Authority برای UN / UNESCO / WHO در Runtime وجود نداشت.
- 33 عنوان از 60 عنوان با Working Master رسمی UN-system به‌صورت Exact/Alias تطبیق دارند، ولی همه 33 مورد در Runtime دارای `OFFICIAL_FLAG='N'` بودند.

## تغییرات این Patch

1. ستون اختیاری `EVENT.SOURCE_ID` و FK به `SOURCE_AUTHORITY` اضافه می‌شود تا provenance خود تعریف رویداد مستقل از قاعده وقوع ثبت شود.
2. `UNITED_NATIONS`، `UNESCO` و `WHO` به‌صورت Source Authority رسمی و idempotent ایجاد/به‌روزرسانی می‌شوند.
3. 33 رویداد رسمی Exact/Alias موجود در Runtime با `OFFICIAL_FLAG='Y'` و Source رسمی Reconcile می‌شوند.
4. Source قاعده فعال همان رویدادها نیز از مرجع عمومی به مرجع رسمی منتقل می‌شود. `World Maritime Day` فعلاً قاعده ندارد و فقط provenance تعریف رویداد اصلاح می‌شود.
5. فرم `SOURCE_AUTHORITY` نوع منبع `INTERNATIONAL_ORGANIZATION` را پشتیبانی می‌کند.
6. فرم `EVENT` ستون lookup «مرجع تعریف مناسبت» را نشان می‌دهد و Event explorer جستجو/مرتب‌سازی روی آن را پشتیبانی می‌کند.

## نکته مهم Coverage

این Patch **235 تعریف Missing** را وارد نمی‌کند. این کار عمداً جدا شده است، زیرا بخشی از مناسبت‌های رسمی تاریخ متغیر دارند و مدل فعلی `EVENT_RECURRENCE_RULE` فقط `ANNUAL_FIXED_DATE` و `ONE_TIME_DATE` را پشتیبانی می‌کند. Seed کامل باید پس از تفکیک fixed-date / weekday-relative / lunar-complex ساخته شود.

## اجرا

```sql
@database/oracle/cal2/migrations/0.11.0-international-observance-provenance.sql
```

خروجی نهایی مورد انتظار:

```text
CAL2_INTL_OBSERVANCE_PROVENANCE_PASS sources=3 mappedEvents=33
CAL2 international observance provenance migration completed.
```

سپس:

```bat
node tools\verify-calendar2-reference.mjs
build-production.cmd
```
