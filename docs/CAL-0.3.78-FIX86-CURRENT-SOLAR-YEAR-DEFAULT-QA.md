# CAL 0.3.78 — FIX86 — پیش‌فرض سال هجری شمسی جاری

## هدف
فرم‌های CAL که رکوردهای روز/تاریخ را در کل Dataset چندصدساله نمایش می‌دهند، هنگام ورود روی سال هجری شمسی جاری محدود شوند؛ کاربر همچنان دسترسی به تمام سال‌ها داشته باشد.

## فرم‌های مشمول
1. `CAL.CALENDAR_DAY`
2. `CAL.CALENDAR_DATE`
3. `CAL.BUSINESS_CALENDAR_DAY`
4. `CAL.CALENDAR_EXCEPTION`
5. `CAL.OCCASION_OCCURRENCE`
6. `CAL.CALENDAR_DAY_OCCASION`
7. `CAL.HIJRI_DATE_OVERRIDE`

جداول Master مانند `CALENDAR_SYSTEM`, `CALENDAR_MONTH`, `WEEKDAY`, `OCCASION`, `OCCASION_RULE` و `BUSINESS_DAY_CONVENTION` مشمول نیستند.

## رفتار UI
- مقدار اولیه فیلتر = `currentYear` دریافتی از Backend.
- کاربر می‌تواند سال دیگری وارد کند.
- «سال جاری» مقدار پیش‌فرض را بازیابی می‌کند.
- «همه سال‌ها» پارامتر `solarYear` را حذف می‌کند.

## Backend
- Endpoint جدید: `GET /api/v1/calendar/reference/solar-year-context`.
- Searchهای CAL پارامتر اختیاری `solarYear` دارند.
- معیار سال، `CAL.CALENDAR_DATE.CALENDAR_SYSTEM_CODE='SOLAR_HIJRI_IR'` و `YEAR_NO` است.
- برای جداول دارای `DAY_ID` فیلتر با `EXISTS` روی تاریخ شمسی همان روز اعمال می‌شود.
- برای Gridهای اختصاصی، Join موجود به تاریخ شمسی برای Filter استفاده می‌شود.

## کنترل استاتیک
`node tools/verify-calendar-current-year-default.mjs`

انتظار: `CAL current Solar Hijri year default verification OK: 7 date-bound forms.`

## ملاحظات
- هیچ DDL و هیچ Update/Delete روی Dataset انجام نمی‌شود.
- شمارش `totalElements` و Pagination بعد از اعمال فیلتر سال محاسبه می‌شود.
