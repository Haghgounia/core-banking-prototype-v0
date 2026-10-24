# CAL2 Personal Calendar & Reminder — R2-A2 Design Closure Candidate

**وضعيت:** Design Review / Physical Model Candidate. **اين بسته DDL اجرایي، Migration نوشتني يا Patch برنامه ندارد.**

## خط مبنا

- سورس ارسالي: `core-banking-prototype-v0-2026-10-22-1644.zip` (نسخه `0.11.0`)
- SHA-256: `50649910d65cdd2ba92bcc73aa085c3bd43d05e915198906ad965f5ba2d86589`
- منبع مفهومي: بسته پيشين `CAL2-PERSONAL-R2A-DESIGN-REVIEW-2026-10-22.zip`
- Oracle: طبق شواهد گفتگو R1B `CAL2.USER_CALENDAR_ITEMS` وجود دارد؛ R1C Rename و Target R2 در Oracle تأييد نشده اند.

## مسئله حل‌شده در R2-A2

مدل R2-A تفکيک صحيح Entry/Occurrence/Rule/Alert داشت، اما چهار مورد نياز به تکميل داشت: ذخيره پايدار Idempotency، مالکيت چند-Issuer، سياست بازتوليد Daily Window و حفظ Original Planned Instant هنگام Snooze. اکنون کنترل FK مرکب براي جلوگيري از اتصال Rule و Occurrence دو Entry مختلف نيز اضافه شده است.

## فهرست محتويات

| فايل | هدف |
|---|---|
| `01-ARCHITECTURE-DECISIONS-FA.md` | 18 تصميم/فرض معماري و موارد باز |
| `02-ENTITY-BOUNDARIES-AND-INVARIANTS-FA.md` | محدوده مسئوليت 5 جدول هسته‌اي و 3 قابليت جانبي |
| `03-SCHEDULER-SCENARIOS-FA.md` | اجرا، چند اعلان، Retry، Split Series و مثال هاي عددي |
| `04-ORACLE-DATA-DICTIONARY-CANDIDATE.csv` | واژه‌نامه فيزيکي پيشنهادي 8 جدول |
| `04-ORACLE-DATA-DICTIONARY-CANDIDATE.md` | نماي جدولي قابل بررسي براي DBA |
| `05-MIGRATION-CUTOVER-FA.md` | انتقال غيرتخريبي R1B، semantic mapping و Rollback |
| `06-API-EXAMPLES-FA.md` | API نسخه دوم با Owner Scope، `If-Match`, `Idempotency-Key` |
| `07-ERD-MERMAID.md` | ERD منطقي و وابستگي به جداول مرجع CAL2 |
| `08-ORACLE-R2A2-BASELINE-READONLY.sql` | بررسي فقط خواندني SQL Developer (F5)؛ بدون NEXTVAL |
| `09-R2A2-UAT-MATRIX.csv` | 58 سناريوي UAT (شامل 30 مورد R2-A) |
| `10-PHYSICAL-CONSTRAINTS-REVIEW.md` | FK مرکب، Unique، CHECK و Index Candidate |
| `11-QA-REPORT-FA.md` | نتيجه کنترل هاي درون اين بسته |
| `R2A-UAT-BASELINE.csv` | کپی خط مبناي 30 سناريو جهت Traceability |

## مرز ايجاد جدول

5 جدول اصلي: `USER_CALENDAR_ENTRIES`, `USER_CALENDAR_RECURRENCE_RULES`, `USER_CALENDAR_OCCURRENCES`, `USER_CALENDAR_REMINDER_RULES`, `USER_CALENDAR_ALERTS`.

3 جدول/قابليت جانبي: `USER_CALENDAR_AUDIT_LOG`, `USER_CALENDAR_REQUEST_KEYS`, `USER_CALENDAR_ALERT_DELIVERIES`.

در صورت وجود سرويس مشترک Audit يا Idempotency با تضمين Transactional و قرارداد امن، از جدول محلي معادل آن صرف‌نظر مي شود. جدول Deliveries تنها براي کانال خارج از برنامه لازم است. تعداد فيزيکي نهايي هنوز مصوب نيست.

## اقدام پيش از R2-B

1. اجراي فقط خواندني Preflight و بررسي واقعي R1B/R1C در Oracle؛ ذخيره خروجي.
2. تثبيت IAM issuer/sub و سياست حريم خصوصي، انواع Legacy Reminder و قواعد تکرار.
3. تأييد مدل فيزيکي با DBA و يکپارچگي FK مرکب و index performance.
4. بعد از گيت تصويب، توليد Patch R2-B (اول PREVIEW، سپس APPLY Opt-in) همراه با Backend/Frontend و Integration/UAT.

**هشدار:** اجراي R1C Rename يا R1C Fresh و هر نوع Drop/Truncate/Rename جدول R1B در اين مرحله توصيه/مجاز نيست.
