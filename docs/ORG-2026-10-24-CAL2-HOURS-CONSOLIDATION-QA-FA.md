# ORG / CAL2 — حذف مدل موازی ساعات کاری (کاندید UAT)

**مبنای تغییر:** `core-banking-prototype-v0-2026-10-24-1043.zip` + نقشه راه ORG مورخ ۲۴ اکتبر ۲۰۲۶ و خروجی واقعی ساختار `ORG-2026-10-24.txt`.

**تصمیم معماری:** قواعد «کدام روز کاری است، برنامه ساعات حضور کارکنان، ساعات خدمت به مشتری و استثنای تک‌روز» متعلق به CAL2 است. ORG فقط مرجع سازمان، واحدها، نقاط خدمت، روابط و قابلیت‌های خدمت باقی می‌ماند. وجود جدول در ORG به معنای اثبات تعلق به دامنه ORG نیست.

## ۱. مواردی که از برنامه ORG کنار گذاشته شدند

| جدول فیزیکی ORG | فرم عمومی قبلی | نتیجه در Source |
|---|---|---|
| `OPERATING_SCHEDULES` | `operating-schedules` | حذف Descriptor و لینک |
| `OPERATING_SCHEDULE_INTERVALS` | `operating-schedule-intervals` | حذف Descriptor و لینک |
| `ORGANIZATION_UNIT_OPERATING_SCHEDULES` | `organization-unit-operating-schedules` | حذف Descriptor و لینک |
| `ORGANIZATION_UNIT_OPERATING_EXCEPTIONS` | `organization-unit-operating-exceptions` | حذف Descriptor و لینک |
| `ORGANIZATION_UNIT_OPERATING_EXCEPTION_INTERVALS` | `organization-unit-operating-exception-intervals` | حذف Descriptor و لینک |
| `SERVICE_POINT_OPERATING_SCHEDULES` | `point-of-service-operating-schedules` | حذف Descriptor و لینک |
| `SERVICE_POINT_OPERATING_EXCEPTIONS` | `point-of-service-operating-exceptions` | حذف Descriptor و لینک |
| `SERVICE_POINT_OPERATING_EXCEPTION_INTERVALS` | `point-of-service-operating-exception-intervals` | حذف Descriptor و لینک |

همچنین فیلترهای ویژه این فرم‌ها از Reference Page برداشته شدند؛ URLهای قدیمی `/organization/<resource>` به `/calendar2/reference-data` هدایت می‌شوند. گروه «ساعات کاری» در منوی ORG حذف و به مرجع CAL2 لینک داده شده است. **API جدید CAL2 ساخته نشده و انتقال داده خودکار صورت نگرفته است.**

**نتیجه قابل انتظار پس از حذف فیزیکی:** ۳۷ → ۲۹ جدول ORG، ۳۷ → ۲۹ Sequence ORG؛ ۳۵ → ۲۷ Descriptor عمومی، ۳۱ → ۲۳ فرم عمومی قابل مشاهده در منوی ORG، به‌علاوه دو فرم اختصاصی درجه‌بندی.

## ۲. موارد مهمی که عمداً حذف نشدند

`ORGANIZATIONS`، `ORGANIZATION_UNITS`، `ORGANIZATION_UNIT_TYPES`، `ORGANIZATION_UNIT_RELATIONSHIPS`، `SERVICE_POINTS`، `SERVICE_CAPABILITIES`، `ORGANIZATION_UNIT_SERVICE_CAPABILITIES`، `SELF_SERVICE_TERMINALS`، جدول‌های مکان، نشانی، تماس، تخصیص و درجه‌بندی، اطلاعات ماهوی ORG هستند. چهار Descriptor از قبل مخفی نیز صرفاً به دلیل مخفی‌بودن از منو حذف نشدند.

اسکریپت تاریخی `database/oracle/org/service-point/ORG-SERVICE-POINT-RENAME-OPT-IN.sql` برای ممیزی تغییرنام قبلی حفظ شده است؛ **بعد از پاک‌سازی این مرحله نباید دوباره اجرا شود**. وجود نام‌های قبلی ساعات کاری در این اسکریپت تاریخی به معنای ثبت فعال آن‌ها در Source نیست.

## ۳. مالک فعلی ساعات کاری در CAL2

- `CAL2.BUSINESS_CALENDAR`: تعریف تقویم کاری، کشور/منطقه زمانی/سازمان.
- `CAL2.BUSINESS_CALENDAR_SCHEDULE`: برنامه معتبر در یک بازه زمانی با وضعیت و اولویت.
- `CAL2.BUSINESS_CALENDAR_SCHEDULE_DAY`: الگوی هفتگی شامل ساعات کارکنان، ساعات خدمت به مشتری و پرچم‌های بانکی.
- `CAL2.BUSINESS_CALENDAR_EXCEPTION`: استثنای یک روز مشخص.
- `CAL2.BUSINESS_CALENDAR_DAY`: روزهای نهایی Resolveشده؛ فقط‌خواندنی.

**محدودیت مهم:** در نسخه فعلی CAL2، نگاشت مستقیم هر `ORG.ORGANIZATION_UNIT_ID` یا `ORG.SERVICE_POINTS.POINT_OF_SERVICE_ID` به یک تقویم CAL2 پیاده‌سازی نشده است. همچنین ساعت اختصاصی خدمات نقدی (`CASH_SERVICE`) و چند شیفت در یک روز، با مدل فعلی CAL2 معادل تضمین‌شده‌ای ندارند. تا تکمیل و آزمون این نگاشت‌ها، **نباید ادعا کرد حذف مدل قدیمی به معنی جایگزینی کامل تمام سناریوهای ساعات کاری واحد و نقطه خدمت است**. اگر داده‌ای در یکی از هشت جدول وجود داشته باشد، اسکریپت به‌جای حذف، متوقف می‌شود تا انتقال/تصمیم کسب‌وکار تعریف گردد.

این اقلام به عنوان `ORG-06A` و `ORG-06B` در نقشه راه باز می‌مانند. ORG دیگر مالک *تعریف ساعت* نیست؛ اگر در آینده اتصال واحد/نقطه خدمت به تقویم لازم شد، مدل ارتباطی مستقل با CAL2 باید طراحی و تصویب شود، بدون ایجاد دوباره قواعد ساعات کاری.

## ۴. اجرای Oracle (بدون دسترسی این محیط به بانک)

ترتیب برای DBA در محیط آزمایش و SQL Developer **F5**:

1. `database/oracle/org/calendar2-consolidation/01-ORG-CAL2-PREVIEW-READONLY.sql` — شمار دقیق رکورد هر ۸ جدول و نمایش وابستگی‌ها؛ هیچ نوشتنی ندارد.
2. `database/oracle/org/calendar2-consolidation/02-ORG-CAL2-DROP-EMPTY-OPT-IN.sql` — مقدار پیش‌فرض `DEFINE APPLY_CHANGES = NO` است؛ فقط Preflight انجام می‌دهد. وقتی تمام رکوردها **صفر** بود و همه وابستگی‌ها رفع شد و بک‌آپ/توقف مصرف‌کنندگان تأیید گردید، DBA همان فایل را با `YES` اجرا کند. از `CASCADE CONSTRAINTS` و `PURGE` استفاده نشده است.
3. `database/oracle/org/calendar2-consolidation/03-ORG-CAL2-VERIFY-READONLY.sql` — انتظار `ORG_CAL2_POST_VERIFY_PASS`.

**قواعد ایمنی:** Oracle DDL خودکار Commit می‌شود و ROLLBACK نتیجه DROP را برنمی‌گرداند. هیچ‌یک از اسکریپت‌ها داده‌های موجود در هشت جدول را پاک/انتقال نمی‌دهند؛ اگر جدول غیرخالی، Trigger، وابستگی View/PLSQL، Synonym یا FK بیرونی وجود داشته باشد، APPLY قبل از اجرای نخستین DROP متوقف می‌شود. وابستگی‌های SQL پویا، سایر برنامه‌ها و گردش‌کارهای خارج از این Repository باید دستی نیز بررسی شوند. اگر جدول خالی نیست، ابتدا قواعد انتقال واقعی بررسی شود؛ **از TRUNCATE یا حذف اجباری برای عبور از گیت استفاده نکنید**.

## ۵. آزمون و تحویل

اجرای ایستا (بدون اتصال Oracle):

```cmd
cd /d D:\Projects\core-banking-prototype-v0
node tools\verify-org-cal2-consolidation.mjs
node tools\verify-org-forms.mjs
node tools\verify-org-ux-r2.mjs
node tools\verify-org-grade-p3-p4.mjs
node tools\verify-release-layout.mjs
```

سپس `build-production.cmd` و راه‌اندازی عادی برنامه. در مرورگر باید گروه ساعات کاری از منوی ORG حذف شده باشد و لینک CAL2 کار کند؛ فرم‌های سازمان، واحد، نقطه خدمت، مکان، درجه‌بندی و سایر ORG باقی باشند. اجرای Oracle در این محیط انجام نشده است؛ از آزمون ایستا نمی‌توان صحت نهایی UAT و تأیید حذف فیزیکی نتیجه گرفت.
