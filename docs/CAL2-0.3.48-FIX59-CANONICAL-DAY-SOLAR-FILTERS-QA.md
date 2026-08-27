# CAL2 0.3.48 — FIX59 Canonical Day Solar Filters QA

## هدف

تکمیل فرم فقط‌خواندنی `CAL2.CANONICAL_DAY` بدون تغییر مدل فیزیکی: نام فارسی روشن، فیلتر سال/قرن شمسی، پیش‌فرض سال جاری و نمایش نام روز هفته و نام ماه شمسی.

## رفتار مورد انتظار UI

1. مسیر «تقویم دو → Dataset تقویم → روزهای مرجع تقویم» با عنوان «روزهای مرجع تقویم» باز شود.
2. در اولین ورود، «سال شمسی» روی سال جاری Dataset قرار گیرد و «قرن شمسی» به‌صورت خودکار قرن متناظر را نشان دهد.
3. تغییر قرن به قرن دیگر، انتخاب سال قبلی را پاک کند و همه روزهای همان قرن را در صفحه‌بندی نمایش دهد.
4. انتخاب یک سال، قرن متناظر را همگام کند و فقط روزهای همان سال شمسی را نمایش دهد.
5. گزینه «همه قرن‌ها» همراه با «همه سال‌ها» کل Dataset را قابل مرور کند.
6. Grid شامل ستون‌های `شناسه روز`، `تاریخ مرجع`، `تاریخ ISO`، `نام روز هفته`، `نام ماه شمسی`، `Epoch Day`، `هفته ISO` و `سال هفته ISO` باشد.
7. زیر نام ماه شمسی، تاریخ شمسی `YYYY/MM/DD` همان `DAY_ID` نمایش داده شود.

## قرارداد API

- `GET /api/v1/calendar2/reference/canonical-days/filter-meta`
  - `currentSolarYear`
  - `minimumSolarYear`
  - `maximumSolarYear`
- `GET /api/v1/calendar2/reference/canonical-days/explorer`
  - فیلترهای اختیاری: `text`, `solarYear`, `solarCentury`
  - صفحه‌بندی/مرتب‌سازی: `page`, `size`, `sortBy`, `direction`

قرن شمسی با تعریف تقویمی استاندارد محاسبه می‌شود؛ برای مثال قرن ۱۵ شامل سال‌های ۱۴۰۱ تا ۱۵۰۰ است.

## Query Join

Explorer از `CAL2.CANONICAL_DAY` به Default Persian Variant در `CAL2.CALENDAR_DATE` Join می‌شود و نام‌ها را از این جداول می‌گیرد:

- `CAL2.WEEKDAY.NAME_FA` برای نام روز هفته
- `CAL2.CALENDAR_MONTH.NAME_FA` برای نام ماه شمسی
- `CAL2.CALENDAR_VARIANT` + `CAL2.CALENDAR_SYSTEM` برای شناسایی Default Persian Variant

## کنترل پایگاه داده

نیاز به Migration یا DDL جدید وجود ندارد. برای کنترل دستی سال جاری می‌توان Query زیر را در Oracle اجرا کرد:

```sql
WITH PERSIAN_CONTEXT AS (
    SELECT MAX(CASE WHEN S.CALENDAR_CODE = 'PERSIAN' AND V.IS_DEFAULT = 'Y'
                    THEN V.CALENDAR_VARIANT_ID END) AS PERSIAN_VARIANT_ID
      FROM CAL2.CALENDAR_VARIANT V
      JOIN CAL2.CALENDAR_SYSTEM S ON S.CALENDAR_SYSTEM_ID = V.CALENDAR_SYSTEM_ID
)
SELECT CD.YEAR_NO, COUNT(*) AS DAY_COUNT
  FROM CAL2.CANONICAL_DAY D
  CROSS JOIN PERSIAN_CONTEXT P
  JOIN CAL2.CALENDAR_DATE CD
    ON CD.DAY_ID = D.DAY_ID
   AND CD.CALENDAR_VARIANT_ID = P.PERSIAN_VARIANT_ID
 WHERE D.CANONICAL_DATE = TRUNC(SYSDATE)
 GROUP BY CD.YEAR_NO;
```

## Static Guard

`node tools/verify-calendar2-reference.mjs` کنترل می‌کند که عنوان فارسی، API Explorer/Meta، فیلترهای سال/قرن و ستون‌های نام روز/ماه در Source باقی مانده باشند.

## نسخه

- Product: `0.3.48-prototype-fee-p1`
- Angular package: `0.3.48`
- Maven: `0.3.48-SNAPSHOT`
