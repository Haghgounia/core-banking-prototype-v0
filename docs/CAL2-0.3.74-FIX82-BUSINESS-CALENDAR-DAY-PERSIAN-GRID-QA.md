# CAL2 0.3.74 / FIX82 — Business Calendar Day Persian Grid QA

## هدف
بازنگری فرم `CAL2.BUSINESS_CALENDAR_DAY` برای نمایش کسب‌وکاری و فارسی.

## قرارداد
- تاریخ شمسی از Persian default `CALENDAR_VARIANT` و `CALENDAR_DATE` استخراج می‌شود.
- روز هفته از `CAL2.WEEKDAY.NAME_FA` نمایش داده می‌شود.
- تقویم کاری با `BUSINESS_CALENDAR.NAME_FA` و کد آن نمایش داده می‌شود.
- منبع با `SOURCE_AUTHORITY.NAME_FA` نمایش داده می‌شود.
- Codeهای دیتابیس بدون تغییر باقی می‌مانند.
- `UNCLASSIFIED` = طبقه‌بندی‌نشده، `PUBLIC_HOLIDAY` = تعطیل رسمی، `PENDING_RULE_EVALUATION` = در انتظار اعمال قواعد.

## تست پذیرش
1. صفحه `business-calendar-days` را باز کنید.
2. رکوردهای سال 1405 باید تاریخ `1405/MM/DD` داشته باشند.
3. عبارت `PUBLIC_HOLIDAY` نباید به صورت خام در Grid دیده شود.
4. جستجوی `1405/01/01` باید روز متناظر را برگرداند.
5. Edit/Delete همچنان با کلید اصلی `BUSINESS_CALENDAR_DAY_ID` کار کند.
