# مرز Aggregate، ارتباطات و Invariantهاي R2-A2

## هسته پنج‌جدولي

### 1. `USER_CALENDAR_ENTRIES`
- **مالکيت:** Entry Aggregate Root با شناسه `USER_CALENDAR_ENTRY_ID NUMBER(12)`، نوع `NOTE/TASK/APPOINTMENT`، عنوان، محتواي خصوصي، روز شروع و برنامه زماني، منطقه زماني، منبع ايجاد و نسخه.
- `OWNER_ISSUER` + `OWNER_SUBJECT_ID` بايد از Principal سروري استخراج شود. هيچ REST Body حاوي Owner پذيرفته نشود. `PREDECESSOR_ENTRY_ID` براي رديابي Split-Series خودارجاع است.
- `ENTRY_STATUS=ACTIVE/ARCHIVED/CANCELLED`، وضعيت انجام شدن هر TASK در Entry نگهداري نمي شود.
- `VERSION_NO` براي `If-Match` افزايش مي يابد. حذف واقعي در API انجام نمي شود. محتوای حساس در Audit/Notification/Idempotency کپي نشود.

### 2. `USER_CALENDAR_RECURRENCE_RULES`
- نسخه‌بندي Rule با `RULE_VERSION_NO` و `RULE_STATUS=ACTIVE/SUPERSEDED/CANCELLED`؛ حداکثر يک رکورد ACTIVE براي هر Entry. براي اعمال تغيير کلي، نسخه جديد ايجاد و قديمي SUPERSEDED مي شود؛ تغيير «اين و آينده» از Split-Series استفاده مي کند.
- `CALENDAR_VARIANT_ID` + `FREQUENCY` + `INTERVAL_NO`، `DAY_OF_MONTH`، `MONTH_OF_YEAR`، ماسک روزهاي هفته و سیاست روز نامعتبر.
- تعريف ماسک هفتگي: رشته ۷کاراکتري `0/1` به ترتيب ISO weekday دوشنبه تا يکشنبه، مستقل از تقويم نمايشي. `1000000`=هر دوشنبه. اعتبار `NTH_WEEKDAY` فقط همراه `NTH_ORDINAL`.
- پردازش: محاسبه تاريخ در Variant -> lookup در `CALENDAR_DATE` -> استخراج `CANONICAL_DAY.DAY_ID`؛ اگر Coverage موجود نيست، خطاي مشخص ثبت شود، نه سکوت.

### 3. `USER_CALENDAR_OCCURRENCES`
- هر وقوع داراي هويت مستقل، `ORIGINAL_SLOT_KEY` تغييرناپذير، `ORIGINAL_DAY_ID` و `DAY_ID` مؤثر است؛ تغيير يک نوبت باعث بازتوليد همان نوبت با شناسه ديگر نمي شود.
- `DUE_AT_UTC` لحظه موعد است؛ براي مدخل all-day بدون ساعت ميتواند NULL باشد، اما اين به‌معناي موعد ساعت 00:00 نيست.
- `OCCURRENCE_STATUS=OPEN/DONE/SKIPPED/CANCELLED`؛ `DONE` صرفاً براي TASK و در صورت تصويب سياست براي APPOINTMENT کاربرد دارد، براي NOTE اقدام «انجام شد» نشان داده نشود.
- قيد يکتا `(USER_CALENDAR_ENTRY_ID, ORIGINAL_SLOT_KEY)`، FK به Entry و Canonical Day و نسخه Optimistic Lock لازم است.

### 4. `USER_CALENDAR_REMINDER_RULES`
- تعريف اعلان مستقل از تکرار Entry. سه الگو:
  - `SINGLE_OFFSET`: يک اعلان با فاصله مثبت قبل از موعد؛ براي Offsetهاي مبتني بر روز تقويمي، زمان اعلان محلي اجباري است.
  - `AT_DUE`: يک اعلان در لحظه موعد؛ براي Entry تمام‌روز، ساعت اعلان محلي بايد صريحاً مشخص شود.
  - `DAILY_WINDOW`: اعلان در N، N-interval، ... روز تقويمي قبل از موعد، مثلاً 3,2,1؛ شامل روز موعد **نيست** و براي روز موعد Rule مستقل AT_DUE لازم است.
- `AFTER` و Business Day Offset در فاز بعد؛ نبايد بي‌صدا به روز تقويمي تفسير شوند.
- در حالت پيش فرض ACK هر اعلان فقط همان اعلان را متاثر مي کند. سياست استثنايي `ACK_STOP_POLICY=STOP_FUTURE_ADVANCES` براي سازگاري اختياري با R1B.ONCE مي تواند پس از ACK اولين پيش‌آگاهي، پيش‌آگاهي‌هاي آتي همان وقوع را متوقف کند و `AT_DUE` را حفظ کند.
- تغيير Rule، Alertهاي SCHEDULED آينده نسخه قبلي را لغو مي کند؛ Alertهاي AVAILABLE/ACKNOWLEDGED گذشته حفظ مي شوند.

### 5. `USER_CALENDAR_ALERTS`
- از ترکيب **Occurrence + ReminderRule + RuleVersion + Slot** توليد مي شود؛ `PLANNED_AT_UTC` immutable است. براي تماميت مالکيت بايد `ENTRY_ID` در Alert و FK مرکب به Occurrence و ReminderRule تعبيه شود تا Alert نتواند وقوع يک کاربر را به Rule کاربر ديگر متصل کند.
- `SCHEDULED -> AVAILABLE -> ACKNOWLEDGED/DISMISSED/SNOOZED`; از `SNOOZED` پس از موعد تعويق به AVAILABLE باز مي گردد؛ زمان تعويق فقط `NEXT_ELIGIBLE_AT_UTC` را تغيير مي دهد.
- ACK يک اعلان، Occurrence را DONE نمي کند؛ Complete وقوع تمام Alertهاي آينده همان وقوع را CANCELLED مي کند؛ گذشته را حذف نمي کند.
- Scheduler با Lease/Conditional Update/Unique Constraint و Reconcile از Duplicate و جاافتادن اعلان پس از قطع سرويس جلوگيري مي کند؛ تضمين Exactly-Once خارجي ادعا نمي شود.

## دو قابليت بنيادين مشروط به سرويس مشترک

### 6. `USER_CALENDAR_AUDIT_LOG`
- Actor (Issuer + Subject)، action، aggregate key، version، UTC time، correlation و fingerprint غيرحساس.
- Append-only در سطح سرويس؛ حذف/Retention تابع Data Governance. نبايد متن آزاد عنوان/يادداشت بدون تصويب در لاگ کپي شود.

### 7. `USER_CALENDAR_REQUEST_KEYS` (افزوده R2-A2)
- Unique روي `(OWNER_ISSUER, OWNER_SUBJECT_ID, OPERATION_CODE, IDEMPOTENCY_KEY)`؛ fingerprint درخواست + نتيجه کد/مرجع.
- Claim درخواست، تغيير مدخل و ثبت نتيجه بايد در يک Transaction محلي Oracle انجام شوند. Retry با همان کلید و fingerprint همان نتيجه را مي گيرد؛ با fingerprint متفاوت HTTP 409.
- اگر سامانه مشترک Idempotency قرارداد معادل Transactional داشته باشد اين جدول محلي حذف و قرارداد Adapter جايگزين مي شود.

## قابليت مرحله بعد

### 8. `USER_CALENDAR_ALERT_DELIVERIES` (اختياري)
- تنها براي Push/Email/SMS يا Provider خارجي، شامل Attempt Number، Channel، Status و Provider Reference بدون Payload حساس.
- براي Inbox داخلي وجود اين جدول ضروري نيست؛ `USER_CALENDAR_ALERTS` منبع حقيقت است.

## کليدها و محدوديت‌هاي مشترک

- همه PKها `NUMBER(12/19)` با Sequence مستقل، بدون UUID براي شناسه عملياتي.
- `ON DELETE CASCADE` براي اطلاعات حسابرسي/اعلان پيشنهاد نمي شود. ارجاع بين دامنه هاي بانکي از طريق API يا Correlation Reference کنترل‌شده باشد، نه FK مستقيم.
- ZoneId معتبر از IANA، وجود Day در `CANONICAL_DAY` و قواعد تقويم از `CALENDAR_DATE` اعتبارسنجي مي شوند.
- جداول R2 با جداول رسمي رويدادها و تعطيلات CAL2 غيرمتداخل بمانند.
