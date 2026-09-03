# CAL2 0.3.81 — FIX89: SQL-driven Day Resolution

## مسئله
در 0.3.80، Rule 193 در API مقدار `LAST_DAY_IF_INVALID` را نشان می‌داد ولی Rebuild هنوز `matchedCalendarDates=0` برمی‌گرداند.

## اصلاح
انتخاب روز موثر به Oracle SQL منتقل شد. عبارت واحد برای هر سال و ماه:

- `EXACT`: همان `DAY_NO` درخواستی.
- `LAST_DAY_IF_INVALID`: اگر روز درخواستی وجود دارد همان روز، وگرنه `MAX(DAY_NO)` همان ماه.

هم `countCalendarMatches` و هم `insertGenerated` از همان `RuleFilter` استفاده می‌کنند. پاسخ Rebuild نیز `dayResolutionPolicy` مصرف‌شده را نشان می‌دهد.

## تست پذیرش
1. Runtime باید `0.3.81-SNAPSHOT` باشد.
2. `GET /api/v1/calendar2/event-recurrence/rules?text=IR_IMAM_REZA_MARTYRDOM&page=0&size=5` باید `LAST_DAY_IF_INVALID` نشان دهد.
3. `POST /api/v1/calendar2/event-recurrence/rebuild?ruleId=193` باید `dayResolutionPolicy=LAST_DAY_IF_INVALID`, `matchedCalendarDates=413`, `insertedGeneratedOccurrences=413` نشان دهد.
4. شمارش Rule 193 باید 413 و شمارش کل رخدادها حدود 97732 شود.
5. سپس `rebuild-all` و `CAL2_FIX88_POST_REBUILD_VALIDATION.sql` اجرا شود و Zero Rule برابر صفر باشد.
