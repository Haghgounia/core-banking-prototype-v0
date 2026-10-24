# قرارداد اجراي قابل اتکاي تقويم و يادآورها

## مثال: کار ماهانه در 15 هر ماه شمسي، ساعت 10:00

درخواست کاربر: اعلام از 3 روز قبل هر روز ساعت 09:00 و يک اعلان در روز موعد ساعت 10:00؛ تکرار خود Task ماهانه است.

1. `ENTRIES`: يک Entry نوع TASK با Owner معتبر، مبناي روز شروع و Asia/Tehran.
2. `RECURRENCE_RULES`: يک Rule فعال MONTHLY در Calendar Variant شمسي و `DAY_OF_MONTH=15`.
3. `OCCURRENCES`: با Generator براي افق محدود، براي هر ماه يک وقوع با Original Slot Immutable؛ Day از `CALENDAR_DATE` حل مي شود.
4. `REMINDER_RULES`: Rule A=`DAILY_WINDOW(START=3, END=1, STEP=1, LOCAL_MINUTE=540)`؛ Rule B=`AT_DUE`.
5. `ALERTS`: براي هر وقوع چهار Alert مستقل ساخته مي شود: روزهاي -3/-2/-1 و روز 0؛ unique slot مانع Duplicate است.
6. اگر کاربر Alert روز -3 را ACK کند، سه اعلان بعدي برقرار مي مانند؛ اگر وقوع را DONE کند، اعلان‌هاي زمان آينده همان وقوع CANCELLED مي شوند.

## مثال چند نوبت و تعداد رکورد

سه وقوع فعال با الگوی بالا: `1 Entry`, `1 Recurrence Rule`, `3 Occurrences`, `2 Reminder Rules`, `12 Alerts`. اگر فقط Inbox فعال باشد `0 Delivery Attempts`، Audit بر مبنای اقدامات متغير است.

## الگوريتم منطقي Scheduler و Reconciler

```text
FOR each active recurrence entry needing horizon extension:
    compute bounded slots using CALENDAR_VARIANT + CALENDAR_DATE
    resolve every slot to CANONICAL_DAY; fail explicitly on missing coverage
    insert occurrence with stable key and uniqueness protection
    resolve active reminder versions into planned alert slots
    insert alert using unique (occ, rule, revision, slot)

FOR each alert where status SCHEDULED and nextEligible <= nowUtc:
    claim with lease (atomic/conditional update) and version
    transition to AVAILABLE in one Oracle transaction
    clear claim; record audit/metrics

FOR each expired lease or missed horizon:
    reconcile safely; never fabricate historical delivered/ACK state
```

## محدوديت‌ها

- زمان `NEXT_ELIGIBLE_AT_UTC` برای Snooze قابل تغيير است، `PLANNED_AT_UTC` نيست.
- Backfill نبايد اعلان پيش‌آگاهي گذشته را مانند اعلان تازهِ لحظه‌اي ارسال کند؛ Late Delivery Policy مستقل لازم است: `AVAILABLE_LATE` يا `EXPIRED` طبق سياست قابل قبول. پيشنهاد: اعلان مربوط به موعدِ هنوز معتبر در Inbox ظاهر شود؛ اعلان‌های پيش‌آگاهي منقضي‌شده نبايد سيل اعلان ايجاد کنند.
- Horizon پيش‌فرض پيشنهادي 90 روز است؛ سقف Batch و Max Alerts Per Entry بايد با تست بار تثبيت شود.
- با خاموش بودن مرورگر، منابع Oracle و Scheduler فعال بايد مستقل کار کنند. Polling Angular صرفاً Read View است.
- در انتخاب زمان UTC، تطبيق Java `ZoneId` با Oracle Time Zone File و تغيير DSTهاي بين‌المللي بايد تست شود.

## تغيير يک وقوع و Split Series

1. `THIS_ONLY`: برقراري Original Slot Key و تغيير `DAY_ID`/UTC Effectives همان وقوع با Optimistic Lock؛ Alertهاي آينده قديم لغو و براي زمان موثر جديد توليد مي شوند؛ تاريخچه حفظ مي شود.
2. `THIS_AND_FUTURE`: ايجاد Entry جديد با `PREDECESSOR_ENTRY_ID`، شروع قاعده جانشين از Slot مورد نظر، متوقف کردن توليد آينده Entry قبلي؛ گذشته بدون بازنويسي.
3. `ENTIRE_SERIES`: از تاريخ درخواست براي تغيير سياست همه Occurrenceهاي آينده اعمال شود؛ گذشته همچنان Immutable Audit دارد.

## سنجه هاي آماده‌سازي عمليات

- `AlertReadyLagSeconds`, `ScheduledBacklog`, `LeaseExpirationRecoveries`, `AlertDuplicatePreventions`, `OccurrenceGenerationErrors`, `CalendarDateMissingCoverage`, `OwnerScopeDeniedCount`.
- Threshold پيشنهادي براي ارزيابي UAT (نه SLA مصوب): Alertهاي due در سامانه فعال ظرف 60 ثانيه AVAILABLE شوند؛ در قطع سيستم، پس از Recovery ظرف 5 دقيقه Reconcile شوند. سقف نهايي بر اساس ظرفيت واقعي تعيين شود.
