# CAL2 Personal Calendar R2-A2 — تصميم هاي معماري و تصميم تفصيلي پيشنهادي

**وضعيت: Design Candidate، نه DDL تأييدشده و نه مجوز اجرا.**

مبنا: `core-banking-prototype-v0-2026-10-22-1644.zip`، نسخه `0.11.0`، SHA-256:
`50649910d65cdd2ba92bcc73aa085c3bd43d05e915198906ad965f5ba2d86589`.
مبناي تکميلي: CAL2 Personal R2-A اسناد 01 تا 07. در سورس جاري Backend نسخه R1C هدف مي گيرد؛ مطابق آخرين شواهد ارائه شده توسط کاربر، Oracle روي R1B است.

## تصميم هاي پيشنهادي (Needs Review)

| ID | موضوع | تصميم R2-A2 | علت / قيد |
|---|---|---|---|
| ADR-01 | مرز دامنه | Personal Calendar + Recurrence + Reminder/Alert داخل Modular Monolith CAL2؛ Adapter کانال خارجي جدا | استقلال داده شخصي از تقويم رسمي و ORG |
| ADR-02 | نوع مدخل | `NOTE`, `TASK`, `APPOINTMENT`؛ `REMINDER` نوع مدخل نيست | يادآوري سياست مستقلي است |
| ADR-03 | هويت مالک | زوج `OWNER_ISSUER` و `OWNER_SUBJECT_ID` از Principal معتبر | `sub` در Issuerهاي مختلف الزاماً يکتا نيست؛ ورودي کاربر پذيرفته نشود |
| ADR-04 | شناسه | Entry=`NUMBER(12)` براي حفظ R1B، ساير Aggregateها=`NUMBER(19)`؛ String Decimal روي JSON | پيشگيري از خطاي precision جاوااسکريپت |
| ADR-05 | زمان | `LOCAL_START_MINUTE` و `LOCAL_ALERT_MINUTE` بين 0 و 1439؛ `TIME_ZONE_ID`؛ لحظه اجرا در UTC با `TIMESTAMP(6) WITH TIME ZONE` | تفکيک تقويم/ساعت ديواري از زمان اجرا؛ نگاشت JDBC بايد آزمون شود |
| ADR-06 | مبناي تاريخ | FK به `CAL2.CANONICAL_DAY`، Rule FK به `CAL2.CALENDAR_VARIANT`؛ رزولوشن از `CALENDAR_DATE` | عدم تکثير موتور تبديل |
| ADR-07 | تکرار | يک Rule فعال به ازاي يک Entry در هر لحظه؛ نسخه هاي قديمي Rule براي سابقه باقي مي مانند | اصلاح Rule گذشته را بازنويسي نکند |
| ADR-08 | تغيير آينده | `SPLIT_SERIES`: ايجاد Entry جانشين با `PREDECESSOR_ENTRY_ID` و خاتمه نوبت هاي آينده مدل قبلي | سابقه و Alertهاي قبلي معتبر بمانند |
| ADR-09 | يکتايي وقوع | `(ENTRY_ID, ORIGINAL_SLOT_KEY)` UNIQUE؛ کلید قطعي Immutable و غيرحساس | توليد مجدد بدون Duplicate |
| ADR-10 | سياست يادآوري | `SINGLE_OFFSET`, `AT_DUE`, `DAILY_WINDOW` در يک ReminderRule با Validation وابسته به نوع | پشتيباني «از سه روز قبل هر روز» بدون تکرار Entry |
| ADR-11 | يکتايي Alert | `(OCCURRENCE_ID, REMINDER_RULE_ID, RULE_VERSION_NO, SCHEDULED_SLOT_KEY)` UNIQUE | ايمني همزماني Schedulerها |
| ADR-12 | Snooze | `PLANNED_AT_UTC` تغيير نمي کند؛ `NEXT_ELIGIBLE_AT_UTC` و `SNOOZED_UNTIL_UTC` تغيير مي کنند | حفظ زمان‌بندي اوليه و اثربخشي تعويق |
| ADR-13 | قابليت اطمينان | Inbox از `ALERTS` خوانده مي شود؛ Browser مسئول زمان‌بندي نيست؛ Lease + Retry + Reconcile | پس از قطع برنامه نيز اعلان پايدار است |
| ADR-14 | API idempotency | جدول `USER_CALENDAR_REQUEST_KEYS` **مشروط** به نبود سرويس مشترک معادل؛ رکورد درخواست و تغيير دامنه در يک Transaction | بازپخش POST در قطع اتصال Duplicate نسازد |
| ADR-15 | حسابرسي | Audit Trail الزامي؛ جدول محلي فقط اگر سرويس مشترک کفايت نکند | صرفاً Metadata ايمن، بدون متن کامل يادداشت |
| ADR-16 | کانال خارجي | `USER_CALENDAR_ALERT_DELIVERIES` تنها با فعال‌سازي کانال خارجي؛ در فاز پايه خالي/ايجادنشده | پرهيز از بيش‌مهندسي |
| ADR-17 | محدوده مدل | فقط 5 موجوديت دامنه اصلي + Capabilityهاي Audit و Idempotency؛ Delivery اختياري | ساختار متناسب با نياز واقعي |
| ADR-18 | عمليات بانک | ثبت يادآوري هرگز مجوز انجام خودکار تراکنش سپرده/تسهيلات نيست | مرز فرمان بانکي مستقل |

## موارد باز براي تصويب

1. **IAM**: قرارداد قطعي `issuer/sub`، نوع Subject و سياست لغو دسترسي يا انتقال حساب کاربران هنوز ارائه نشده است؛ تا آن زمان اجراي چندکاربره `Fail-Closed` باشد.
2. **مدت نگهداري**: Audit، درخواست Idempotency، Alert و داده حذف منطقي بايد طبق مصوبه بانک تعيين شود؛ زمان پيش‌فرض عملياتي قطعي نيست.
3. **سرويس هاي مشترک**: در صورت وجود سرويس Audit/Notification/Idempotency مورد تأييد، معادل محلي ايجاد نشود.
4. **قانون Date Resolve**: `SKIP` و `LAST_DAY_IF_INVALID` براي تاريخ هاي نامعتبر؛ رفتار پيش‌فرض `SKIP` پيشنهادي است، نيازمند UAT تقويم شمسي.
5. **ساعت DST**: `GAP_SHIFT_FORWARD` و `OVERLAP_EARLIER_OFFSET` پيشنهادي؛ به‌روزرسانی TZDB در محيط بانک آزمون شود.
6. **روال مهاجرت**: ميزان داده R1B، وجود مالک واقعي، معناي `REMINDER`, `DAILY/ONCE`, `DONE` در هر رکورد بايد پيش از Mapping نهايي مشخص شود.

## اصلاح هاي R2-A2 نسبت به R2-A

- افزوده شدن Idempotency Store مشروط؛ R2-A وعده `Idempotency-Key` مي داد ولي مدل Data يکي براي نگهداري درخواست نداشت.
- Owner از يک `SUBJECT_ID` به زوج Issuer/Subject تغيير مي کند.
- `VARCHAR2(5)` براي ساعت در طراحي فيزيکي پيشنهادي به `NUMBER(4)` (دقيقه از 00:00) تغيير مي کند؛ نمايش UI همچنان `HH:mm` است.
- `DAILY_WINDOW` به‌عنوان الگوی مستقل اعلان قبل از موعد اضافه مي شود.
- تفکيک `PLANNED_AT_UTC` تغييرناپذير از `NEXT_ELIGIBLE_AT_UTC` براي Snooze.
- افزودن `PREDECESSOR_ENTRY_ID` براي Split-Series و قواعد تاريخچه.

## گيت اجرا

**اين سند مجوز اجراي DDL، حذف، Rename يا Backfill نيست.** فقط پس از تأييد مالکيت، نام‌گذاري، مدل SQL، قرارداد Recurrence، Cutover و برنامه تست، بسته R2-B با `PREVIEW -> APPLY` مستقل ايجاد شود.
