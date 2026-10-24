# R2-A2 QA Evidence

مبنای آزمون: سورس 16:44، SHA-256 `50649910d65cdd2ba92bcc73aa085c3bd43d05e915198906ad965f5ba2d86589`.

| کنترل | نتیجه | محدوده اثبات |
|---|---|---|
| SHA-256 آخرين سورس | PASS | تطابق با فایل 2026-10-22-1644.zip |
| واژه‌نامه داده | PASS | 129 ستون پيشنهادي در 8 جدول؛ نام جداول و ستون ها حداکثر 30 کاراکتر |
| تکرار نام ستون در يک جدول | PASS | صفر |
| UAT | PASS | 58 سناريو با ID يکتا؛ 30 سناريوي R2-A بدون تغيير حفظ شده، 53 مورد P0 و 5 مورد P1 |
| سناريوي Daily Window | PASS (model simulation) | 3 وقوع و 4 Alert براي هر وقوع = 12 کلید يکتا |
| کنترل FK مرکب در مستند | PASS | Occurrence/ReminderRule/Entry و RecurrenceRule/Entry مستند شده است |
| SQL فقط خواندني | PASS (static) | در کد SQL هيچ DDL/DML/COMMIT/NEXTVAL شناسايي نشد؛ متکي به Oracle runtime نيست |
| موجوديت هاي مرجع CAL2 | Documented | `CANONICAL_DAY`, `CALENDAR_DATE`, `CALENDAR_VARIANT` از سورس مبنا شناسايي شده اند |
| مهاجرت واقعي و SQL اجرايي R2 | NOT DONE | اين بسته DDL يا DML اجرايي ندارد |
| اجراي Read-only SQL بر Oracle | NOT DONE | نياز به اتصال SQL Developer کاربر |
| Build Java/Angular و Integration Test | NOT DONE | هيچ تغيير کد برنامه در R2-A2 ايجاد نشده است |
| Semantic Migration Approval | PENDING | نگاشت R1B `REMINDER`, `DAILY/ONCE`, `DONE`, Actor و Overdue نياز به تصويب دارد |

## محدوديت ها

- 8 جدول در اين بسته **Candidate** هستند. Audit/Idempotency ممکن است توسط Capability مشترک بانک تأمين شوند؛ Delivery براي کانال هاي خارجي است.
- عملکرد Oracle `TIMESTAMP WITH TIME ZONE`، ايجاد Index/Constraints، بهينه‌سازي SQL و نگاشت Driver Java بايد با Database واقعی آزمون شوند.
- هيچ داده بانکي يا Oracle تغيير نکرده، هيچ Schema حذف/تغييرنام داده نشده، و هيچ برنامه‌اي Patch نشده است.
- اجراي SQL فقط خواندني به مجوزهاي لازم براي مشاهده Metadata و شمارش ردیف CAL2 نياز دارد.

## مراجع فني عمومي

- Oracle Database 19c SQL Reference — CREATE TABLE, DEFAULT/NEXTVAL: https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/CREATE-TABLE.html
- Oracle Database 19c SQL Reference — Data Types / TIMESTAMP WITH TIME ZONE: https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/Data-Types.html
- RFC 7529 — Non-Gregorian Recurrence Rules, SKIP handling: https://www.rfc-editor.org/info/rfc7529/

**Result:** Suitable for architecture and DBA design review, NOT approved for production or any DDL APPLY.
