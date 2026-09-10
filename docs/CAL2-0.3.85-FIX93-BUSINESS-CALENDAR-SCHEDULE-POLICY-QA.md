# CAL2 0.3.85 / FIX93 — Business Calendar Schedule Policy QA

## هدف

تعریف گروهی ساعات کاری شعب و کارکنان برای بازه‌های ماهانه، فصلی، سالانه یا هر بازه معتبر، بدون درج/ویرایش روزبه‌روز؛ و امکان Override یک تاریخ برای فورس‌ماژور، تعطیلی موردی یا ساعات خاص.

## مدل داده

| Object | نقش |
|---|---|
| `CAL2.BUSINESS_CALENDAR_SCHEDULE` | Policy بازه‌ای و اطلاعات بخشنامه/اطلاعیه |
| `CAL2.BUSINESS_CALENDAR_SCHEDULE_DAY` | الگوی هفتگی؛ ساعت کارکنان و مشتری |
| `CAL2.BUSINESS_CALENDAR_EXCEPTION` | Override تک‌روز |
| `CAL2.BUSINESS_CALENDAR_DAY` | خروجی Resolve‌شده و فقط‌خواندنی |

`OPEN_TIME/CLOSE_TIME` در `BUSINESS_CALENDAR_DAY` زمان بازشدن/پایان خدمت‌رسانی به مشتری را نگه می‌دارد؛ `STAFF_START_TIME/STAFF_END_TIME` زمان حضور کارکنان را نگه می‌دارد.

Schedule جدید با `DRAFT` شروع می‌شود. فعال‌سازی (`ACTIVE`) فقط وقتی مجاز است که هر هفت Weekday موجود باشد و برای تمام روزهای `OPEN/PARTIAL` چهار ساعت کارکنان/مشتری کامل شده باشد. همین کنترل پیش از Rebuild نیز روی داده‌های فعال اجرا می‌شود تا تغییرات مستقیم DBA باعث Materialize ناقص نشود.

## قرارداد Resolver

ترتیب تصمیم برای هر روز:

1. `BUSINESS_CALENDAR_EXCEPTION` فعال برای همان تاریخ
2. `EVENT_OCCURRENCE.HOLIDAY_FLAG='Y'`
3. Schedule فعال که تاریخ را پوشش می‌دهد
4. `DEFAULT / UNCLASSIFIED` برای Gapهای فاقد Rule

در همپوشانی Scheduleها، بالاترین `PRIORITY_NO`، سپس جدیدترین `EFFECTIVE_FROM` و سپس بزرگ‌ترین Schedule ID انتخاب می‌شود.

اگر وضعیت نهایی `CLOSED` باشد، تمام ساعات و Flagهای Business/Settlement/Clearing/Processing به `NULL/N` Normalize می‌شوند.

## UI

سه فرم جدید زیر گروه «تقویم کاری و بانکی» اضافه شده است:

- برنامه‌های ساعات کاری
- الگوی هفتگی ساعات کاری
- استثناهای تک‌روز

فرم «روزهای تقویم کاری» فقط‌خواندنی است و پنل «بازسازی خروجی روزهای تقویم کاری» دارد. بازه تاریخ اختیاری است؛ در صورت خالی بودن، Backend بازه Policyهای فعال را محاسبه می‌کند.

## API

`POST /api/v1/calendar2/business-calendar/rebuild`

Query Parameters:
- `businessCalendarId` — الزامی
- `fromDate` — اختیاری، ISO date
- `toDate` — اختیاری، ISO date

حداکثر بازه هر اجرا پنج سال است. نتیجه تعداد روزهای Resolve‌شده به تفکیک Schedule/Holiday/Exception/Default را برمی‌گرداند.

## Migration

برای CAL2 موجود:

`database/oracle/cal2/migrations/0.3.85-fix93-business-calendar-schedule-policy.sql`

Migration idempotent است و وجود Table/Column/Constraint/Index را قبل از ایجاد کنترل می‌کند.

## سناریوی پذیرش

1. یک Schedule با بازه نمونه `1405/04/01` تا `1405/06/31` ایجاد شود؛ وضعیت اولیه باید `DRAFT` باشد.
2. سیستم هفت ردیف Weekday را بسازد؛ جمعه `CLOSED` باشد.
3. شنبه تا چهارشنبه ساعات کارکنان `07:00-14:30` و مشتری `07:30-13:30` ثبت و سپس Schedule به `ACTIVE` تغییر داده شود.
4. برای یک تاریخ داخل بازه، Exception از نوع `SPECIAL_HOURS` با ساعات متفاوت ثبت شود.
5. Rebuild همان بازه اجرا شود.
6. روز عادی `RESOLUTION_SOURCE=SCHEDULE` و روز Exception برابر `EXCEPTION` باشد.
7. یک روز دارای Holiday Flag بدون Exception باید `CLOSED/HOLIDAY` شود.
8. اگر برای روز تعطیل Exception با `DAY_STATUS=OPEN` ثبت شود، Exception بر Holiday غالب شود.
9. Grid روزهای تقویم کاری امکان Edit/Delete مستقیم نداشته باشد.

## Guard

`tools/verify-calendar2-business-calendar-schedule.mjs`

Guard 13 Contract اصلی DDL/Migration/Metadata/API/Resolver/UI را کنترل می‌کند و در `build-production.cmd` و `build-production.sh` اجرا می‌شود.

## وضعیت Verification این بسته

- Static project verifiers: اجرا می‌شوند و باید قبل از Packaging همگی Pass باشند.
- Maven/Angular full build در محیط تحویل باید با `build-production.cmd` انجام شود.
- در محیط تولید Artifact این گفتگو، Maven Wrapper امکان دریافت Maven از `repo.maven.apache.org` نداشت؛ بنابراین Full Java/Angular compile در این محیط قابل تکمیل نبود.
