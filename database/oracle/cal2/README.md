# CAL2 — BIAN-aligned 400-Year Calendar

این شاخه Schema مستقل `CAL2` را برای بسته `BIAN_Calendar_400Y_Oracle_Import` ایجاد می‌کند. مدل شامل ۱۶ جدول است و هیچ FK یا جدول مشترکی با Schema `CAL` ندارد.

## ترتیب نصب

1. با `SYSTEM`/`SYS` یا حساب دارای مجوز `CREATE USER`:
   ```sql
   @00-create-cal2-schema.sql
   ```
2. با `CAL2` یا حساب Privileged:
   ```sql
   @01-create-cal2-tables.sql
   ```
3. اگر برنامه با User دیگری متصل می‌شود:
   ```sql
   @02-grant-cal2-to-application-user.sql
   ```
4. کنترل فقط‌خواندنی:
   ```sql
   @99-verify-cal2-schema.sql
   ```

اگر Tablespace `USERS` در محیط مقصد وجود ندارد، قبل از اجرای `00-create-cal2-schema.sql` نام Tablespace را مطابق استاندارد همان Oracle Environment تغییر دهید.


## ارتقا از FIX54/FIX55

اگر Schema `CAL2` از قبل ساخته شده است، برای افزودن قواعد مناسبت تکرارشونده فقط Migration زیر را اجرا کنید:

در SQL*Plus وقتی داخل همین شاخه هستید:

```sql
@migrations/0.3.45-fix56-event-recurrence-rule.sql
```

Migration جدول `EVENT_RECURRENCE_RULE` را اضافه می‌کند و `EVENT_OCCURRENCE` را با `EVENT_RULE_ID` و `OCCURRENCE_SOURCE` توسعه می‌دهد. اگر برنامه با User دیگری غیر از `SYSTEM` یا `CAL2` متصل است، پس از Migration اسکریپت `02-grant-cal2-to-application-user.sql` را دوباره اجرا کنید تا مجوز جدول جدید نیز اعطا شود.

## Import داده

پس از Build/Start برنامه، از مسیر زیر ZIP اصلی ۱۵ فایل CSV را انتخاب کنید:

`/calendar2/reference-data/import`

Backend فایل‌ها را با همان Oracle DataSource، به‌ترتیب وابستگی FK و با JDBC Batch در یک Transaction درج می‌کند. SQL*Loader و Oracle Client روی سیستم کاربر لازم نیست. Event/Business Calendar CSVهای Header-only با صفر رکورد پذیرفته می‌شوند.

Dataset تحویلی مبنا:
- Canonical range: `1826-01-01` تا `2225-12-31`
- Canonical days: `146097`
- Calendar mappings: `438291`
- ICU version: `76.1`

توجه: Variant قمری بسته `ISLAMIC_CIVIL_ICU_CALCULATED` محاسباتی است و نباید به‌عنوان تقویم رسمی رؤیت هلال ایران تلقی شود.
