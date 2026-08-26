# CAL2 0.3.45 / FIX56 — Event Recurrence Rule & Occurrence Materialization

## هدف

افزودن تعریف یک‌باره مناسبت و تولید خودکار رخدادهای آن برای سال‌های موجود در `CAL2.CALENDAR_DATE`، بدون بازنویسی رخدادهای دستی یا رسمی.

## تغییر مدل فیزیکی

جدول جدید:

`CAL2.EVENT_RECURRENCE_RULE`

فیلدهای اصلی:

- `EVENT_RULE_ID`: کلید اصلی
- `EVENT_ID`: مناسبت تعریف‌شده در `CAL2.EVENT`
- `RULE_TYPE`: `ANNUAL_FIXED_DATE` یا `ONE_TIME_DATE`
- `CALENDAR_VARIANT_ID`: Variant مبنای تاریخ
- `YEAR_NO`: فقط برای رخداد یک‌باره
- `MONTH_NO`, `DAY_NO`: تاریخ در تقویم مبنا
- `START_YEAR_NO`, `END_YEAR_NO`: بازه اختیاری برای قاعده سالانه
- `SOURCE_ID`: مرجع تعریف قاعده
- `DESCRIPTION`
- `ACTIVE_FLAG`

جدول `CAL2.EVENT_OCCURRENCE` نیز با دو ستون زیر توسعه یافته است:

- `EVENT_RULE_ID`: ردیابی قاعده مولد
- `OCCURRENCE_SOURCE`: یکی از `GENERATED`, `MANUAL`, `OFFICIAL`

## رفتار Materialization

1. کاربر ابتدا مناسبت را در فرم `EVENT` تعریف می‌کند.
2. در فرم «تعریف مناسبت و قاعده تکرار» Calendar Variant، نوع قاعده، ماه/روز و بازه سال را ثبت می‌کند.
3. همان Transaction ذخیره قاعده، رخدادهای `GENERATED` قبلی همان Rule را حذف می‌کند.
4. Backend از `CAL2.CALENDAR_DATE` تمام `DAY_ID`های منطبق را انتخاب می‌کند.
5. برای هر روز منطبق، `EVENT_OCCURRENCE` جدید با `OCCURRENCE_SOURCE='GENERATED'` و `DATA_STATUS='CALCULATED'` درج می‌شود.
6. اگر برای همان `EVENT_ID + DAY_ID` از قبل رخداد دیگری وجود داشته باشد، رکورد موجود بازنویسی نمی‌شود.
7. رخدادهای `MANUAL` و `OFFICIAL` هیچ‌گاه در Rebuild حذف نمی‌شوند.
8. غیرفعال‌کردن Rule یا Event و ذخیره Rule، رخدادهای Generated همان Rule را پاک می‌کند و رخداد جدید نمی‌سازد.

## قواعد پشتیبانی‌شده

### ANNUAL_FIXED_DATE

برای مناسبت‌های سالانه ثابت در یک Calendar Variant:

- 1 اردیبهشت در Variant شمسی
- 12 May در Variant میلادی
- 5 June در Variant میلادی
- 19 August در Variant میلادی
- روز ثابت قمری در Variant قمری محاسباتی

`START_YEAR_NO` و `END_YEAR_NO` اختیاری هستند. اگر خالی باشند، کل سال‌های موجود در Dataset همان Variant استفاده می‌شود.

### ONE_TIME_DATE

برای رویداد تاریخی که فقط یک‌بار رخ داده است؛ `YEAR_NO`, `MONTH_NO`, `DAY_NO` لازم‌اند.

مثال: 1367/04/27 در Variant شمسی.

## کنترل روی Dataset تحویلی

با خواندن مستقیم `08_calendar_date.csv` بسته اصلی CAL2، منطق Fixed-Date روی نمونه‌های زیر کنترل شد:

| قاعده | Variant | نتیجه در Dataset |
|---|---|---:|
| 12 May | `GREGORIAN_ICU` | 400 روز |
| 5 June | `GREGORIAN_ICU` | 400 روز |
| 19 August | `GREGORIAN_ICU` | 400 روز |
| 1 اردیبهشت | `PERSIAN_ICU_CALCULATED` | 400 روز |
| 1367/04/27 | `PERSIAN_ICU_CALCULATED` / One-time | 1 روز |

این کنترل فقط برای صحت منطق انتخاب Rule انجام شده است؛ Backend در Runtime از `CAL2.CALENDAR_DATE` خود Oracle استفاده می‌کند.

## UI

تعداد فرم‌های CAL2 از 15 به 16 افزایش یافته است. فرم جدید در گروه «مناسبت‌ها و رویدادها» قرار دارد:

`تعریف مناسبت و قاعده تکرار`

رفتار UI:

- در Rule سالانه، `YEAR_NO` مخفی و `START_YEAR_NO/END_YEAR_NO` نمایش داده می‌شوند.
- در Rule یک‌باره، `YEAR_NO` نمایش داده و بازه سال مخفی می‌شود.
- ذخیره/ویرایش Rule به‌صورت خودکار Materialization را اجرا می‌کند.
- هر Rule دکمه «تولید/بازسازی رخدادها» دارد.
- دکمه «بازسازی همه قواعد فعال» برای اجرای جمعی وجود دارد.

## API

- `POST /api/v1/calendar2/event-recurrence/rebuild?ruleId=<id>`
- `POST /api/v1/calendar2/event-recurrence/rebuild-all`

## Migration برای CAL2 موجود

اگر FIX54/FIX55 قبلاً نصب شده است، Schema را دوباره نسازید. فقط اجرا شود:

`database/oracle/cal2/migrations/0.3.45-fix56-event-recurrence-rule.sql`

Migration قابل اجرای مجدد است و قبل از ایجاد Table/Column/Constraint/Index وجود آن را کنترل می‌کند.

اگر DataSource با User دیگری غیر از `SYSTEM` یا `CAL2` متصل است، پس از Migration اسکریپت `02-grant-cal2-to-application-user.sql` دوباره اجرا شود.

## کنترل‌های انجام‌شده در محیط بسته‌بندی

- `verify-calendar2-reference.mjs`: PASS
- `verify-calendar-display-labels.mjs`: PASS
- سایر Verifierهای CAL/CIF/EA-Oracle: PASS
- Java 21 compile harness برای package جدید recurrence: PASS
- Java 21 compile harness برای Registry/Service اصلاح‌شده: PASS
- TypeScript syntax transpile برای فایل‌های تغییرکرده: PASS
- Angular Build کامل اجرا نشد؛ `ng` در محیط بسته‌بندی نصب نبود و `npm install` به Dependency Download دسترسی نداشت.
- Maven Build کامل اجرا نشد؛ Maven/Wrapper distribution در محیط بسته‌بندی در دسترس نبود.
