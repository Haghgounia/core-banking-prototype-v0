# CAL2 0.3.49 / FIX60 — بازنگری نگاشت تاریخ، رویدادها، دیاگرام و مناسبت‌های قمری

## دامنه تغییر

این Fix چهار بخش CAL2 را تکمیل می‌کند:

1. فرم `CAL2.CALENDAR_DATE` با فیلترهای **نوع تقویم / قرن / سال** و پیش‌فرض **تقویم هجری شمسی + سال جاری**.
2. فرم `CAL2.EVENT` با نمایش **عنوان فارسی نوع رویداد** به جای شناسه عددی و فیلترهای **نوع رویداد / نوع تقویم**.
3. صفحه اصلی «تقویم دو» با دیاگرام مفهومی روابط ۱۶ جدول CAL2.
4. اسکریپت Seed مستقل و idempotent برای مناسبت‌های ثابت هجری قمری مورد استفاده در تقویم عمومی ایران.

## رفتار فرم نگاشت تاریخ‌های تقویمی

- API جدید: `GET /api/v1/calendar2/reference/calendar-dates/explorer`
- Metadata جدید: `GET /api/v1/calendar2/reference/calendar-dates/filter-meta`
- نوع تقویم: `PERSIAN | GREGORIAN | ISLAMIC`
- مقدار پیش‌فرض: `PERSIAN`
- سال پیش‌فرض از رکورد `CALENDAR_DATE` متناظر با `TRUNC(SYSDATE)` استخراج می‌شود و Hard-code نیست.
- با تغییر نوع تقویم، سال جاری و قرن متناظر همان سیستم تقویمی انتخاب می‌شود.
- شناسه Variant در Grid به صورت «نام فارسی تقویم / Variant Code» نمایش داده می‌شود.
- روز هفته در Grid با عنوان فارسی نمایش داده می‌شود.

## رفتار فرم رویدادها و مناسبت‌ها

- API جدید: `GET /api/v1/calendar2/reference/events/explorer`
- ستون `EVENT_TYPE_ID` در Grid به جای مقدار عددی، `EVENT_TYPE.NAME_FA` را نمایش می‌دهد.
- فیلتر نوع رویداد از Lookup جدول `CAL2.EVENT_TYPE` تغذیه می‌شود.
- فیلتر نوع تقویم با `EXISTS` روی `EVENT_RECURRENCE_RULE -> CALENDAR_VARIANT -> CALENDAR_SYSTEM` اعمال می‌شود؛ بنابراین معنای فیلتر، «تقویمی که قاعده وقوع مناسبت بر آن تعریف شده» است.
- عملیات ویرایش/حذف همچنان با `EVENT_ID` واقعی انجام می‌شود و مدل فیزیکی تغییر نکرده است.

## دیاگرام صفحه اصلی CAL2

دیاگرام، مسیرهای اصلی زیر را نشان می‌دهد:

- `CALENDAR_SYSTEM -> CALENDAR_VARIANT -> CALENDAR_DATE <- CANONICAL_DAY`
- `CALENDAR_SYSTEM -> CALENDAR_MONTH`
- `WEEKDAY -> CANONICAL_DAY / CALENDAR_DATE`
- `DATASET_VERSION -> CALENDAR_DATE`
- `EVENT_TYPE -> EVENT -> EVENT_RECURRENCE_RULE -> EVENT_OCCURRENCE -> CANONICAL_DAY`
- `SOURCE_AUTHORITY -> EVENT_RECURRENCE_RULE / EVENT_OCCURRENCE`
- `BUSINESS_CALENDAR -> BUSINESS_CALENDAR_DAY -> CANONICAL_DAY`
- `VALIDATION_RUN -> VALIDATION_RESULT`

## Seed مناسبت‌های قمری ایران

فایل:

`database/oracle/cal2/migrations/0.3.49-fix60-iran-islamic-fixed-events.sql`

خصوصیات:

- قابل اجرای مجدد بدون Duplicate شدن `EVENT_CODE` یا Rule متناظر.
- در صورت نبود نوع `RELIGIOUS` آن را ایجاد می‌کند.
- در صورت نبود منبع `IRAN_PUBLIC_CALENDAR` آن را ایجاد می‌کند.
- Variant قمری فعال و ترجیحاً Default را به صورت Dynamic پیدا می‌کند.
- رویدادها در `CAL2.EVENT` و قاعده سالانه ثابت در `CAL2.EVENT_RECURRENCE_RULE` درج می‌شوند.
- تاریخ‌ها بر مبنای **ماه/روز هجری قمری** هستند و تبدیل به روز Canonical توسط موتور Recurrence موجود انجام می‌شود.
- Script شامل ۲۹ مناسبت شاخص ثابت قمری است؛ از جمله تاسوعا، عاشورا، اربعین، رحلت پیامبر، شهادت امام رضا، میلاد پیامبر، شهادت حضرت زهرا، مبعث، نیمه شعبان، شهادت امام علی، عید فطر، عید قربان و عید غدیر.

## Verification

- `node tools/verify-calendar2-reference.mjs` : PASS
- Angular build در محیط حاضر اجرا نشد چون `ng`/node_modules در Runtime موجود نبود.
- Maven wrapper نیز به علت عدم دسترسی Runtime به Maven Central قادر به دریافت Maven 3.9.16 نبود؛ بنابراین Build کامل Backend در این محیط انجام نشد.
- تغییرات Schema ندارند و Migration DDL جدید لازم نیست؛ فقط Seed قمری اختیاری است.
