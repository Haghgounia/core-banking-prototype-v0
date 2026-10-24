# R2-B Migration / Cutover Plan — candidate only

## خط مبناي مستند

- طبق خروجي کاربر، R1B `CAL2.USER_CALENDAR_ITEMS` ساخته شده، PK/Indexes/Sequence صحيح اند.
- در ZIP ساعت 16:44، Java Repository از نام R1C `CAL2.USER_CALENDAR_ENTRIES` استفاده مي کند. بنابراين داده/کد اکنون از نظر طرح نام با هم يکسان نيستند.
- براي R2 هيچ اجراي Oracle تا اين مرحله توسط ما مشاهده نشده است. اسکریپت R1C-RENAME يا FRESH اجرا نشود.

## 1. Preflight Read Only

- اجراي `08-ORACLE-R2A2-BASELINE-READONLY.sql` و ثبت تعداد ردیف، حداقل/حداکثر ID، LAST_NUMBER، FKهاي ورودي و Actor هاي Demo بدون افشاي محتواي شخصي.
- آخرين ID فعلي *به تنهايي* براي انتخاب شروع Sequence جديد کافي نيست؛ بايد High-water، Sequence قديمي و مقادير اختصاص‌داده‌شده در Migration لحاظ شوند. `NEXTVAL` در Preflight مصرف نشود.
- بررسي Coverage `CANONICAL_DAY` و `CALENDAR_DATE` براي بازه تاريخي/آينده مورد استفاده.

## 2. Dual-Schema Shadow Model (نه Rename in Place)

- جدول R1B بدون `DROP`/`TRUNCATE`/`RENAME` باقي بماند.
- Target R2 در نام `USER_CALENDAR_ENTRIES` و پنج جدول ديگر و Capabilityهاي ضروري به‌صورت مستقل ايجاد شود. وجود جدول هم‌نام R1C قبل از CREATE بايد `STOP` ايجاد کند.
- اپليکيشن R1C پيش از Cutover نبايد به جدول هدف R2 با قرارداد ستون متفاوت Query بزند. V1 بايد Feature Flag/Fallback/Read-Only صريح داشته باشد؛ Ghost compatibility ممنوع.
- DDL Oracle پس از Review با `PREVIEW` و Apply صريح و با Backup Schema، توقف نوشتن و Oracle DBA Change Window عرضه شود.

## 3. Semantic Mapping (ممنوعيت تبديل خام)

| منبع R1B | پيشنهاد | شرط پذيرش |
|---|---|---|
| `USER_CALENDAR_ITEM_ID` | حفظ مقدار در `USER_CALENDAR_ENTRY_ID` | مطابقت Count + Set of IDs + Sequence High-water |
| `ITEM_TYPE=NOTE` | `ENTRY_TYPE=NOTE` | محتوا و تاريخ حفظ شود |
| `ITEM_TYPE=REMINDER` | `TASK` يا `NOTE+ReminderRule` | تصميم موردبه‌مورد کسب‌وکاري؛ تبديل خودکار ممنوع |
| `STATUS=OPEN/DONE` | Occurrence `OPEN/DONE` در Task | براي NOTE و غيرTask نيازمند تصميم؛ `ENTRY_STATUS` جايگزين مستقيم DONE نيست |
| `REPEAT_POLICY=DAILY` | Window پيش‌آگاهي روزانه تا روز قبل موعد | R1B DAILY، تکرار **اعلان** است نه تکرار Entry؛ در R1B alertKind policy بررسي شد |
| `REPEAT_POLICY=ONCE` | Window با `ACK_STOP_POLICY=STOP_FUTURE_ADVANCES` | فقط پس از تأييد معناي اولين ACK؛ اعلان روز موعد مستقل |
| `ADVANCE_DAYS` | Window از N روز قبل تا روز قبل | `0` يعني بدون پيش‌آگاهي، فقط روز موعد |
| `REMINDER_TIME` | minute-of-day؛ اعلان پيش‌موعد در همان ساعت | ساعت UTC با `Asia/Tehran` و تاريخ محلي resolve شود |
| `FIRST_ADVANCE_ACK`/`LAST_ALERT_ACK_DATE` | Snapshot تفسيري، نه تاريخچه ساختگي | تاريخچه واقعي ارسال/مشاهده از اين دو فيلد قابل بازسازي نيست |
| `ACTOR_KEY=prototype-admin` | انتقال به Quarantine يا مالک Demo در محيط جدا | هرگز بدون مدرک به `OWNER_SUBJECT_ID` حقيقي Map نشود |
| R1B `OVERDUE` | Badge موعدگذشته/Policy مستقل | R1B پس از موعد API نوع OVERDUE مي داد؛ در R2 نبايد سيل اعلان گذشته خودکار ساخته شود |

## 4. Reconciliation / Stop Conditions

- شمارش ردیف منبع و Target، برابري کامل IDهاي منتقل‌شده، Hash مجموعه فیلدهای مهاجرتی کنترل‌شده، تعداد Quarantine، استثناهاي Semantic Mapping و Last Migration Batch ثبت شود.
- هر ردیف Unmapped/Quarantined بايد گزارش شود؛ اگر رکورد جابه‌جا نشده، گزارش `100% migrated` ممنوع.
- FKها، Checkها، Rule Uniqueها، Indexها، Policy Typeها، Timezone و Query Plan در UAT/DBA سنجيده شوند.
- اگر هم‌پوشاني اسم `USER_CALENDAR_ENTRIES` مشاهده شود يا برنامه V1 هنوز آن را مي خواند، Cutover متوقف شود.

## 5. Cutover / Rollback

1. Snapshot و مانيفست SHA-256؛ تمام نوشتن‌هاي V1 متوقف.
2. Shadow Backfill تا Reconciliation موفق.
3. Build و آزمون REST V2 و Frontend V2 با IAM معتبر و Shadow DB.
4. Switching Feature Flag در Change Window؛ تست Canary با کاربر تست مجاز، سپس گسترده.
5. در Rollback، برنامه V2 Disable و R1B دست‌نخورده بماند. بازگرداني نوشتن روي V1 فقط با طرح بازپخش/عدم ازدست‌دادن تغييرات بعد از Cutover مجاز است.
6. Purge/Rename R1B تنها با مصوبه جداگانه پس از Retention و Evidence؛ نه در Release اول.

## 6. Gates before physical DDL

- [ ] Security/IAM Mapping + Fail-Closed با مديريت هويت مصوب.
- [ ] قاعده `REMINDER` legacy -> Entry نوع جديد مصوب.
- [ ] سياست هاي `DAILY`, `ONCE`, `OVERDUE`, `DONE` و ACK Legacy با UAT مصوب.
- [ ] Zone/DST Policy، Coverage `CALENDAR_DATE` و زمان all-day مصوب.
- [ ] Audit/Idempotency/Delivery بين ماژول CAL2 و سرويس هاي مشترک تعيين تکليف.
- [ ] DDL با Schema DBA، Constraintها و حقوق اجرا در محيط تست Oracle بازبيني شده است.
