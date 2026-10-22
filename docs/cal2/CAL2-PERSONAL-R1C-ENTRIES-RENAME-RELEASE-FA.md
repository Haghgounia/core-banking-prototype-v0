# CAL2 — نام‌گذاری دقیق مدخل‌های تقویم شخصی (R1C)

**مبنای سورس:** `core-banking-prototype-v0-2026-10-22-1237-CAL2-PERSONAL-R1B-SOURCE.zip`  
**تاریخ:** ۱۴۰۵/۰۷/۳۰ (۲۰۲۶/۱۰/۲۲)  
**دامنه:** فقط Personal Notes & Reminders در Schema `CAL2`. رویدادها، مناسبت‌ها، تقویم کاری و سایر Domainها تغییر نکرده‌اند.

## ۱. تصمیم نام‌گذاری

| قلم | R1B | R1C |
|---|---|---|
| Table | `CAL2.USER_CALENDAR_ITEMS` | `CAL2.USER_CALENDAR_ENTRIES` |
| PK column | `USER_CALENDAR_ITEM_ID NUMBER(12,0)` | `USER_CALENDAR_ENTRY_ID NUMBER(12,0)` |
| Sequence | `SEQ_CAL2_USER_CALENDAR_ITEMS` | `SEQ_CAL2_USER_CALENDAR_ENTRIES` (۳۰ کاراکتر) |
| Type column | `ITEM_TYPE` | `ENTRY_TYPE` |
| PK / FK | `PK_CAL2_UCI`, `FK_CAL2_UCI_DAY` | `PK_CAL2_UCE`, `FK_CAL2_UCE_DAY` |
| Check constraints | `CK_CAL2_UCI_*` | `CK_CAL2_UCE_*` (۶ قید اختصاصی) |
| Indexes | `PK_CAL2_UCI`, `IX_CAL2_UCI_ACTOR_DAY`, `IX_CAL2_UCI_ALERT` | `PK_CAL2_UCE`, `IX_CAL2_UCE_ACTOR_DAY`, `IX_CAL2_UCE_ALERT` |
| API list/create | `/api/v1/calendar2/personal/items` | `/api/v1/calendar2/personal/entries` |
| API JSON | `userCalendarItemId`, `itemType` | `userCalendarEntryId`, `entryType` |
| API Alerts | `/api/v1/calendar2/personal/alerts` | ثابت؛ در Alert شناسه `userCalendarEntryId` است |

مدل دارای دو نوع Entry، یعنی `NOTE` و `REMINDER` است؛ منطق اعلان، محدودیت ۰ تا ۳۰ روز، `ONCE/DAILY` و منطقه زمانی `Asia/Tehran` تغییر نکرده است. شناسه‌های عددی موجود و داده‌های ثبت‌شده حفظ می‌شوند؛ هیچ `DROP`/`CREATE` دوباره‌ای روی جدول موجود انجام نمی‌شود. ستون اختیاری `LEGACY_ITEM_ID`، اگر از مهاجرت UUID به جا مانده باشد، حذف یا بازنویسی نمی‌شود.

### قرارداد جدید API

```json
{
  "userCalendarEntryId": 47,
  "dayId": 123,
  "canonicalIsoDate": "2026-10-22",
  "entryType": "REMINDER",
  "title": "پیگیری گزارش شعب",
  "description": null,
  "reminderTime": "09:00",
  "advanceDays": 3,
  "repeatPolicy": "DAILY",
  "status": "OPEN",
  "lastAlertAckDate": null,
  "firstAdvanceAcknowledged": false
}
```

**توجه سازگاری API:** نام جدید عمداً در endpoint و JSON نیز اعمال شده است. اگر مصرف‌کننده‌ای خارج از UI فعلی از `/items` یا `userCalendarItemId` استفاده می‌کند، باید همزمان ارتقا یابد؛ Alias خودکار در R1C پیش‌بینی نشده است.

## ۲. اجرا روی Oracle موجود (همان ساختاری که R1B ایجاد کرده است)

**این تنها مسیر مناسب محیط فعلی شماست. فایل `FRESH` برای نصب‌های جدید و بدون جدول است و نباید روی Oracle فعلی اجرا شود.**

پیش‌نیاز: ارتباط مستقیم SQL Developer با **کاربر `CAL2`** (دستور Oracle `RENAME` باید در Schema مالک اجرا شود)، توقف برنامه، Backup معتبر Schema و Backup سورس، اطمینان از نبود تراکنش یا سرویس وابسته، پنجره تغییر کنترل‌شده.

1. `database/oracle/cal2/migrations/CAL2-PERSONAL-ENTRIES-R1C-RENAME-OPT-IN.sql` را با `DEFINE APPLY_CHANGES = NO` و **F5** اجرا کنید.
2. پیش‌نمایش باید `CAL2_PERSONAL_R1C_PREVIEW_PASS` و تعداد `R1B_ROWS_BEFORE` را نمایش دهد. شرط‌های ایمنی از جمله نام‌های تکراری، اشیای وابسته و FK ورودی باید برقرار باشند. در صورت اعلام Dependency/Conflict یا اجرای نیمه‌کاره، **متوقف شوید**؛ DDL را بدون تحلیل مجدد اجرا نکنید.
3. تنها پس از تأیید Backup و پیش‌نمایش، مقدار `DEFINE APPLY_CHANGES = YES` را تغییر دهید و با F5 اجرا کنید. پیام مورد انتظار: `CAL2_PERSONAL_R1C_RENAME_COMPLETE` همراه تعداد ردیف‌های قبل و بعد برابر.
4. سپس `CAL2-PERSONAL-ENTRIES-R1C-VERIFY-READONLY.sql` را با F5 اجرا کنید. عبارت `CAL2_PERSONAL_R1C_READONLY_VERIFY_PASS` باید چاپ شود. خروجی `DATA_DEFAULT` ستون شناسه را نیز **چشمی** بررسی کنید؛ باید `CAL2.SEQ_CAL2_USER_CALENDAR_ENTRIES.NEXTVAL` باشد. این فایل فقط‌خواندنی است و NEXTVAL را مصرف نمی‌کند.
5. نام جدول و Sequence قدیمی باید دیگر وجود نداشته باشد. `CANONICAL_DAY` و سایر جداول CAL2 بدون تغییر باقی می‌مانند.

### نکات ایمنی ضروری

- Oracle DDL به‌صورت ضمنی Commit می‌شود. هیچ `ROLLBACK` ساده‌ای تغییر نام‌ها را برنمی‌گرداند. در صورت خطا بعد از شروع، ممکن است وضعیت **نیمه‌مهاجرت‌شده** باشد؛ قبل از هر تلاش دیگر از DBA و Backup/Runbook استفاده کنید.
- خود اسکریپت R1C هیچ `DROP TABLE`, `DROP SEQUENCE`, `DELETE`, `TRUNCATE` یا بازشماره‌گذاری انجام نمی‌دهد.
- Rename معمولاً امتیازهای موجود بر جدول و Sequence را حفظ می‌کند، اما نام‌های ارجاع‌دهنده به آن‌ها در کد SQL دیگر یا Jobهای بیرونی باید بررسی شوند؛ اسکریپت وجود Dependencyهای قابل مشاهده در Data Dictionary را مانع اجرا قرار می‌دهد.
- اگر **Session User** شما `SYSTEM` یا کاربر دیگری است، بدون انتقال حساب/مجوز و برنامه اجرایی مورد تأیید DBA، به `YES` تغییر ندهید.
- فایل‌های R1B `*-ITEMS-*` برای سابقه مهاجرت نگه داشته شده‌اند؛ روی ساختار R1C اجرا نمی‌شوند.

## ۳. نصب Patch کد؛ کاملاً مستقل از Oracle

Patch فقط روی **سورسی که R1B قبلاً نصب شده** یا روی نسخه کامل R1B مورد اشاره قابل اعمال است. `APPLY` ابتدا SHA-256 فایل‌های مبنا را چک می‌کند و در صورت ناسازگاری هیچ فایلی را دستکاری نمی‌کند.

از پوشه ZIP بازشده Patch در Windows CMD:

```cmd
apply-cal2-personal-r1c.cmd "D:\Projects\core-banking-prototype-v0" CHECK
apply-cal2-personal-r1c.cmd "D:\Projects\core-banking-prototype-v0" APPLY
```

اگر هنوز R1B را نصب نکرده‌اید، ابتدا سورس کامل R1B را به‌عنوان مبنا در نظر بگیرید، یا از ZIP **سورس کامل R1C** استفاده کنید. R1C را مستقیماً روی نسخه قدیمی R1 (UUID) اعمال نکنید.

در ریشه پروژه:

```cmd
node tools\verify-cal2-personal-r1c.mjs
build-production.cmd
```

نصب‌کننده کد، **هیچ SQLای اجرا نمی‌کند**. پیش از نخستین راه‌اندازی، حتماً مهاجرت Oracle و بررسی فقط‌خواندنی را انجام دهید؛ کد جدید به نام‌های R1C متکی است. نصب‌کننده از فایل‌های سورس تغییر‌یافته نسخه پشتیبان می‌گیرد و فایل `app/BUILD-DIRTY` را برای جلوگیری از اجرای JAR قدیمی ثبت می‌کند. فایل‌های Build قبلی تا زمان اجرای دوباره `build-production.cmd` معتبر محسوب نمی‌شوند.

## ۴. معیار پذیرش و UAT

| سناریو | انتظار |
|---|---|
| Preflight Oracle | نام‌های مبنا معتبر و نام‌های مقصد آزاد؛ Dependency خارجی و FK ورودی صفر |
| Migration داده | شمار رکورد قبل/بعد برابر، IDهای قدیمی همان اعداد، `LEGACY_ITEM_ID` احتمالی حفظ شود |
| Constraints | PK، FK و شش Check همگی `ENABLED/VALIDATED` |
| Indexes | سه Index جدید `VALID` |
| API | `GET/POST/PUT/PATCH/DELETE /entries` با `userCalendarEntryId` عددی و `entryType` |
| ثبت NOTE | بدون اعلان، نمایش در تقویم و حفظ اطلاعات پس از refresh |
| REMINDER | اعلان پیش‌موعد، روز موعد و overdue مطابق تنظیمات قبلی |
| دسترسی | همه CRUDها با `ACTOR_KEY` سمت سرور محدود باقی بمانند |
| رگرسیون | رویدادها/تعطیلات/تقویم کاری CAL2 بدون تغییر |

**سطح شواهد ساخت:** قرارداد R1C، بررسی نحوی ۴ فایل TS، بررسی نحوی ۵ کلاس Backend با Stubهای Spring، آزمون مستقل Java منطق اعلان و نصب آزمایشی Patch روی کپی مبنا قابل انجام است. این نتایج **جایگزین Build کامل Spring/Angular و تست Oracle روی محیط شما نیستند**. برنامه هنوز از Demo Actor ثابت `prototype-admin` استفاده می‌کند و برای بهره‌برداری چندکاربره، Authentication/RBAC واقعی لازم است.
