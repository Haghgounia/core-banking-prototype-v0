# گزارش بررسی Patchهای IAM و فایل‌های جدید Oracle

**شناسه:** IAM-SOURCE-AUDIT-2026-11-01  
**محدوده:** بررسی ایستای فایل ZIP ارسالی `core-banking-prototype-v0-2026-11-01-1320.zip` و مقایسه با `core-banking-prototype-v0-2026-10-30-1801.zip`  
**تاریخ بررسی:** 2026-11-01  
**نوع بررسی:** Repository snapshot inspection; **بدون دسترسی به Oracle زنده، Git metadata یا اجرای Patch روی سیستم کاربر**.

## نتیجه اجرایی

**بسته IAM-R0.1 در سورس نصب شده؛ مدل‌های بعدی R1.3/R1.4 در اسنپ‌شات دیده نمی‌شوند. هیچ‌کدام از فایل‌های SQL جدید `database/oracle/iam/` یا `database/oracle/org/` نباید اکنون حذف شوند.**

## شواهد دقیق IAM

| کنترل | نتیجه |
|---|---|
| فایل‌های محتوایی R0.1 و Manifest | ۱۲ فایل؛ SHA256 همه مطابق (۱۲/۱۲ PASS) |
| بررسی ابزار `node tools/iam/verify-iam-foundation-model.cjs` | `IAM_R0_STATIC_PASS: TABLES=35 SEQUENCES=21 CONSTRAINTS=202; NO_DB_WRITE` |
| مدل R0.1 | ۳۵ جدول، ۴۱۵ ستون طبق Manifest؛ جداول مفرد و ۴ ستون Audit عمومی در هر جدول |
| DDL R0.1 | فایل‌های `01-...`, `02-...`, `03-...` تنها **Opt-in** و نه Migration تصویب‌شده جدید |
| مدل R0.5 تا R0.9 | فایل بازنگری مرحله‌ای متناظر در Repository جاری یافت نشد |
| Baseline R1.3 | مسیر `docs/iam/reviews/R1.3` و XMI نسخه R1.3 در ZIP موجود نیست |
| Readiness/Roadmap R1.4 | مسیر `docs/iam/reviews/R1.4` و اسناد R1.4 در ZIP موجود نیست |
| Backend/Frontend IAM | پیاده‌سازی اختصاصی جدید IAM در Source این اسنپ‌شات دیده نشد |
| Oracle IAM | از روی ZIP، ایجاد/تعداد جدول‌های واقعی در Oracle **قابل تشخیص نیست** |
| Git | پوشه `.git` در ZIP وجود ندارد؛ Commit و وضعیت Stage بررسی‌پذیر نیست |

**هشدار:** فایل `IAM-CANONICAL-ROADMAP-FA.md` موجود هنوز نسخه **R0.1** است و عبارت `ORACLE_NOT_APPLIED / NO_APP_CHANGES` دارد. همچنین DDL فیزیکی موجود هنوز از نام‌های مفرد مانند `IAM.PRINCIPAL` استفاده می‌کند و ستون‌های `CREATED_AT`, `CREATED_BY`, `UPDATED_AT`, `UPDATED_BY` را دارد؛ برخلاف تصمیم‌های بعدی تیم. **قبل از تصویب مدل فیزیکی جدید، اسکریپت‌های R0.1 را روی Oracle اجرا نکنید.**

## مقایسه دو اسنپ‌شات

| نوع تغییر | تعداد فایل |
|---|---:|
| اضافه‌شده | 65 |
| حذف‌شده | 7 |
| تغییر محتوا (CRC/size) | 34 |
| افزوده‌شده در `database/oracle/iam` | 6 |
| افزوده‌شده در `database/oracle/org` | 20 |
| افزوده‌شده در Backend اختصاصی IAM | 0 |

**توجه:** تغییرات فایل‌های Java/Frontend در این دوره عمدتاً مربوط به ORG و FEE3 هستند، نه IAM.

## فایل‌های جدید Oracle — طبقه‌بندی حفظ / آرشیو

از ۲۰ فایل ORG جدید، **۱۷ فایل تاریخ ۳۱ اکتبر و ۳ فایل تاریخ ۱ نوامبر** در متادیتای ZIP دارند. این فایل‌ها همگی SQL هستند و سه رده `READONLY`، `OPT-IN` و `RESTORE` را شامل می‌شوند.

| زیرشاخه ORG | تعداد | تصمیم | علت |
|---|---:|---|---|
| `canonical-location-independence` | 2 | حفظ فعال | استقلال مکان و نشانی واحدها؛ پیش‌نمایش DDL و اعتبارسنجی |
| `location-retirement-r4` | 5 | حفظ فعال | تغییر مرجع نقاط خدمت و Rename/Restore جداول تاریخی مکان؛ وابسته به UAT/DBA |
| `contact-cutover-r5` | 2 | حفظ تاریخی؛ کاندید آرشیو پس از اختتام | مسیر اولیه پایلوت تماس V2؛ با تصمیم OUCP-only نباید دوباره به‌عنوان مسیر عملیاتی اجرا شود |
| `contact-points-retirement-r5c2` | 5 | حفظ تاریخی و بازیابی | مرحله‌های R5C2/R5C3 و Restore؛ مستند 31 اکتبر مدعی تکمیل ساختاری است ولی وضعیت DB زنده در این ارزیابی تأیید نشده |
| `contact-points-retirement-r5c4` | 3 | حفظ حیاتی؛ اجرای Apply ممنوع تا Gate | ممیزی R5C4 و Rename مشروط R5C5 برای CONTACT_POINTS_V2؛ بنا بر سند 1 نوامبر هنوز مجوز اجرای R5C5 حاصل نشده |
| `relationship-rules-retirement-r6` | 3 | حفظ فعال | Rename و Restore قواعد قدیمی روابط سازمانی؛ نتیجه واقعی Oracle باید جدا تأیید شود |

فایل‌های **R5C2/R5C3** شامل تغییر ساختار و مسیر بازگشت هستند؛ سند پروژه از تکمیل ساختاری این مراحل خبر می‌دهد، اما این گزارش وضعیت دیتابیس زنده را تأیید نمی‌کند. فایل‌های **R5C4/R5C5** هنوز به‌عنوان گام بعدی Rename وابسته به تأیید DBA، UAT و توقف نویسندگان هستند؛ حذف یا اجرای عجولانه آن‌ها مجاز نیست.

### فهرست کامل فایل‌های ORG افزوده‌شده

| تاریخ (ZIP) | نقش | فایل |
|---|---|---|
| 2026-10-31 | OPT-IN | `database/oracle/org/canonical-location-independence/01-ORG-UNIT-LOCATION-DETACH-OPT-IN.sql` |
| 2026-10-31 | READONLY | `database/oracle/org/canonical-location-independence/02-ORG-UNIT-LOCATION-INDEPENDENCE-VERIFY-READONLY.sql` |
| 2026-10-31 | OPT-IN | `database/oracle/org/contact-cutover-r5/01-ORG-R5-V2-UNIT-CONTACT-BACKFILL-OPT-IN.sql` |
| 2026-10-31 | READONLY | `database/oracle/org/contact-cutover-r5/02-ORG-R5-V2-CONTACT-COVERAGE-VERIFY-READONLY.sql` |
| 2026-10-31 | OPT-IN | `database/oracle/org/contact-points-retirement-r5c2/01-ORG-R5C2-OUCP-DETACH-OPT-IN.sql` |
| 2026-10-31 | READONLY | `database/oracle/org/contact-points-retirement-r5c2/02-ORG-R5C2-OUCP-DETACH-VERIFY-READONLY.sql` |
| 2026-10-31 | OPT-IN | `database/oracle/org/contact-points-retirement-r5c2/03-ORG-R5C3-CONTACT_POINTS-RENAME-OPT-IN.sql` |
| 2026-10-31 | READONLY | `database/oracle/org/contact-points-retirement-r5c2/04-ORG-R5C3-RENAME-VERIFY-READONLY.sql` |
| 2026-10-31 | RESTORE | `database/oracle/org/contact-points-retirement-r5c2/05-ORG-R5C3-MASTER-RESTORE-OPT-IN.sql` |
| 2026-11-01 | READONLY | `database/oracle/org/contact-points-retirement-r5c4/01-ORG-R5C4-V2-RETIREMENT-AUDIT-READONLY.sql` |
| 2026-11-01 | OPT-IN | `database/oracle/org/contact-points-retirement-r5c4/02-ORG-R5C5-CONTACT_POINTS_V2-RENAME-OPT-IN.sql` |
| 2026-11-01 | READONLY | `database/oracle/org/contact-points-retirement-r5c4/03-ORG-R5C5-V2-RENAME-VERIFY-READONLY.sql` |
| 2026-10-31 | OPT-IN | `database/oracle/org/location-retirement-r4/01-ORG-R4-SERVICE-POINT-CANONICAL-CUTOVER-OPT-IN.sql` |
| 2026-10-31 | READONLY | `database/oracle/org/location-retirement-r4/02-ORG-R4-SERVICE-POINT-VERIFY-READONLY.sql` |
| 2026-10-31 | OPT-IN | `database/oracle/org/location-retirement-r4/03-ORG-R4-LEGACY-RENAME-PREVIEW-OPT-IN.sql` |
| 2026-10-31 | READONLY | `database/oracle/org/location-retirement-r4/04-ORG-R4-LEGACY-RENAME-VERIFY-READONLY.sql` |
| 2026-10-31 | RESTORE | `database/oracle/org/location-retirement-r4/05-ORG-R4-LEGACY-RENAME-RESTORE-OPT-IN.sql` |
| 2026-10-31 | OPT-IN | `database/oracle/org/relationship-rules-retirement-r6/01-ORG-R6-RELATIONSHIP-RULES-RENAME-OPT-IN.sql` |
| 2026-10-31 | READONLY | `database/oracle/org/relationship-rules-retirement-r6/02-ORG-R6-RELATIONSHIP-RULES-VERIFY-READONLY.sql` |
| 2026-10-31 | RESTORE | `database/oracle/org/relationship-rules-retirement-r6/03-ORG-R6-RELATIONSHIP-RULES-RESTORE-OPT-IN.sql` |

## سیاست پیشنهادی پاکسازی

1. **اکنون هیچ‌کدام از ۲۶ فایل SQL جدید IAM/ORG را حذف یا جابه‌جا نکنید.** بعضی از اسکریپت‌ها در فایل‌های Verify، Runbook و ابزارهای پروژه به مسیر فعلی ارجاع دارند. جابه‌جایی شتاب‌زده می‌تواند آزمون‌های ثابت و بازیابی را خراب کند.
2. بعد از اتمام Oracle Verify، UAT و ثبت وضعیت Commit، می‌توان اسکریپت‌های *منسوخ ولی ضروری برای تاریخچه* را با یک Git Move به `database/oracle/org/archive/...` منتقل کرد؛ ارجاع‌ها و Checksums نیز باید هم‌زمان اصلاح شوند. آرشیو بهتر از حذف است.
3. برای IAM، DDL نسخه R0.1 تا تولید و تصویب DDL جدید نگهداری شود، اما **به‌عنوان اسکریپت اجرایی فعلی مورد استفاده قرار نگیرد**. در مرحله Cutover، نسخه قدیمی به `archive/r0.1` انتقال یابد.
4. بسته‌های R1.3 و R1.4 فقط مستندات/مدل طراحی را نصب می‌کنند و **به‌خودی‌خود نه Oracle را تغییر می‌دهند و نه R0.1 DDL را اصلاح می‌کنند**. مسیر مقصد آن‌ها به‌ترتیب `docs/iam/reviews/R1.3` و `docs/iam/reviews/R1.4` است. نبود این مسیرها در ZIP نشان می‌دهد وارد این اسنپ‌شات نشده‌اند؛ نمی‌توان درباره پوشه‌های خارج از اسنپ‌شات قضاوت کرد.
5. وضعیت دیتابیس را در صورت نیاز DBA با فایل اختیاری `IAM-ORG-DB-STATUS-READONLY.sql` بررسی کند؛ این گزارش بدون آن هیچ ادعایی درباره اجرای SQL قبلی نمی‌کند.

## معیار نهایی مجاز برای حذف یا آرشیو

فقط پس از ثبت هم‌زمان: **(الف)** آخرین حالت Oracle و تأیید Readonly Verify، **(ب)** تکمیل UAT، **(ج)** نبود وابستگی اپلیکیشن و مصرف‌کنندگان خارجی، **(د)** وجود Migration و Restore معتبر در Git، **(ه)** تأیید DBA و معمار.

## حدود نتیجه‌گیری

- تاریخ فایل در ZIP به‌تنهایی تاریخ اجرای DDL یا نتیجه موفق Patch نیست.
- خروجی `STATIC_PASS` صرفاً صحت اسکریپت و اسناد R0.1 را بررسی می‌کند؛ تأیید Schema یا API عملیاتی نیست.
- برخی اسناد ORG دارای شواهد قدیمی DBA هستند، اما همه مراحل جدیدتر الزاماً تکمیل نشده‌اند.
- این گزارش سورس اصلی ویندوز کاربر، پایگاه داده، Git commit یا محیط Runtime را تغییر نداده است.
