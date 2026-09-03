# CAL2 0.3.80 — FIX88: سیاست حل روز نامعتبر در قواعد مناسبت

## هدف

حفظ تاریخ کسب‌وکاری مناسبت (برای نمونه «۳۰ صفر») در قاعده، بدون تحریف آن به «۲۹ صفر»، و در عین حال امکان تولید رخداد محاسباتی روی Calendar Variantهایی که آن روز را ندارند.

## تغییر مدل

ستون `CAL2.EVENT_RECURRENCE_RULE.DAY_RESOLUTION_POLICY` اضافه شد:

- `EXACT`: فقط تاریخ دقیق؛ رفتار پیش‌فرض همه قواعد موجود.
- `LAST_DAY_IF_INVALID`: اگر `DAY_NO` در ماه/سال موردنظر وجود نداشت، آخرین روز معتبر همان ماه انتخاب می‌شود.

فقط `IR_IMAM_REZA_MARTYRDOM` با ماه ۲ و روز ۳۰ به سیاست دوم تنظیم می‌شود.

## رفتار Generator

برای هر سال:
1. ابتدا وجود روز درخواست‌شده بررسی می‌شود.
2. اگر وجود داشت، همان روز تولید می‌شود.
3. فقط در سیاست `LAST_DAY_IF_INVALID` و در صورت نبود روز، `MAX(DAY_NO)` همان ماه/سال انتخاب می‌شود.
4. `EVENT_RECURRENCE_RULE.DAY_NO` دست‌نخورده می‌ماند؛ بنابراین قاعده همچنان «۳۰ صفر» است.

## انتظار پس از Migration + rebuild-all

- `IR_IMAM_REZA_MARTYRDOM`: حدود 413 رخداد Generated در Dataset فعلی.
- Ruleهای با zero generated occurrence: صفر.
- مجموع رخدادهای Generated با داده فعلی: حدود `97,732`.
- Duplicate روی `EVENT_ID + DAY_ID`: صفر.

## نصب

`database/oracle/cal2/migrations/0.3.80-fix88-last-day-resolution-policy.sql` را اجرا، بررسی و Commit کنید؛ سپس Runtime نسخه 0.3.80 را Build/Start کرده و بعد endpoint `POST /api/v1/calendar2/event-recurrence/rebuild-all` را فراخوانی کنید.
