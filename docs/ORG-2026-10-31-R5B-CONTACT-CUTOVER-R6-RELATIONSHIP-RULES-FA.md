# ORG R5-B + R6 — Canonical Contact Read-Only Cutover and Relationship Rules Deprecation

تاریخ: ۱۴۰۵/۰۸/۰۹ (2026-10-31) | دامنه: ORG | وضعیت: Source Ready / Oracle Rename Opt-In

## خلاصه تصمیم‌ها

- **R5-B:** مهاجرت ۶٬۹۳۰ انتساب تماس به `ORG.CONTACT_POINTS_V2` بر اساس خروجی Oracle و API کاربر تأیید شده است. در این بسته فقط راه‌اندازی و بررسی یکپارچه **Canonical Read-Only** تسهیل می‌شود؛ هیچ فعال‌سازی خودکار نوشتن صورت نمی‌گیرد. سرویس R5-A موجود کماکان مبنای خواندن از V2 است.
- **R6:** جدول `ORG.ORGANIZATION_UNIT_RELATIONSHIP_RULES` از قرارداد عمومی Reference Data حذف می‌شود و در صورت عبور از کنترل‌های Oracle و دریافت مجوزها به `ORG.DEPRECATED_ORGANIZATION_UNIT_RELATIONSHIP_RULES` تغییر نام می‌یابد. این عملیات **Rename** است، نه Drop، و هیچ رکوردی انتقال یا حذف نمی‌شود.
- `ORG.ORGANIZATION_UNIT_RELATIONSHIPS` و `ORG.ORGANIZATION_UNIT_RELATIONSHIP_TYPES` فعال می‌مانند. `SEQ_ORGANIZATION_UNIT_RELATIONSHIP_RULES` و Constraints/Indexes تاریخی نیز حفظ می‌شوند.

## ۱. وضعیت مبنا و محدودیت شواهد

داده‌های مشاهده‌شده در گفت‌وگو: `LEGACY_UNIT=6930`, `CANONICAL_V2=6930`, `MISSING_LINKS=0`, `ORPHANS=0`, `SOURCE_BUSINESS_DRIFT=0`, `sourceCoverageComplete=true`.

بررسی Source موجود نشان داد که Reference Descriptor عمومی جدول قواعد ارتباط وجود داشت و فرم آن در منو مخفی بود. وجود مصرف مستقیم این جدول در سرویس‌های عملیاتی در Source مشاهده نشد؛ این **اثبات نبود وابستگی بیرونی نیست**. تعداد واقعی ردیف‌ها، FKهای ورودی، Jobها و Stored Objectها از Oracle زنده هنوز مشخص نیست. اسکریپت `01` همه را از دیتابیس جاری بررسی می‌کند.

## ۲. نصب Source — بدون Oracle DDL/DML

1. برنامه را متوقف کنید (`bin\stop.cmd`). از فولدر پروژه و پایگاه داده Backup بگیرید.
2. ZIP را خارج از مسیر پروژه استخراج کنید و `INSTALL.cmd` را از پوشه Patch اجرا کنید.
3. انتظار: `ORG_R5B_R6_SOURCE_VERIFIED=...`, `ORG_R5B_R6_BUILD_PASS=1`, `ORG_R5B_R6_ORACLE_NOT_APPLIED=1`, `ORG_R5B_R6_INSTALL_EXIT=0`.
4. نصب سورس **هیچ Oracle DDL یا DML اجرا نمی‌کند** و پیش‌فرض `ORG_CONTACT_STORAGE_MODE=legacy` و `ORG_CONTACT_CANONICAL_WRITES_ENABLED=false` را تغییر نمی‌دهد.
5. Descriptor جدول قواعد قدیمی از API عمومی حذف می‌شود؛ Route پیشین به صفحه سازمان هدایت می‌شود و تلاش برای نوشتن در Resource قدیمی با Validation Error متوقف می‌شود.

## ۳. R5-B — راه‌اندازی Canonical در حالت فقط خواندنی

این مرحله باید تحت نظارت مسئول داده و پس از توقف Writerهای Legacy انجام شود تا میان اجرای Precheck و UAT هیچ داده جدید در جدول‌های قدیمی ثبت نشود.

1. به‌عنوان بررسی نهایی Oracle، اسکریپت موجود `database\oracle\org\contact-cutover-r5\02-ORG-R5-V2-CONTACT-COVERAGE-VERIFY-READONLY.sql` را از Connection مستقل اجرا کنید. قبل از شروع هرگونه نوشتن V2، مقادیر `MISSING_LINKS`, `ORPHANS`, `SOURCE_BUSINESS_DRIFT` باید صفر باشد.
2. نسخه Source جدید را نصب و Build کنید. سپس در پروژه `bin\start-org-contacts-canonical-readonly.cmd` را اجرا کنید (به جای `bin\start.cmd`). این ابزار با متغیرهای محیطی فقط همان فرآیند را در `canonical` راه‌اندازی و نوشتن را به‌صورت اجباری غیرفعال می‌کند؛ `config\application.yml` را دست‌کاری نمی‌کند.
3. در پنجره فرمان جداگانه، `bin\verify-org-contacts-canonical-readonly.cmd` را اجرا کنید؛ نتیجه مورد انتظار `ORG_R5B_CANONICAL_READONLY_RUNTIME_PASS` است. کنترل HTTP به هیچ وجه رکورد/مقدار تماس را نمایش نمی‌دهد.
4. در UI چند واحد سازمانی دارای داده و به‌ویژه یکی از ۸۶ انتساب با شناسه V2 متفاوت را از نظر نمایش، تعداد، نقش، وضعیت اصلی و دوره اعتبار بررسی کنید؛ دکمه‌های افزودن و ویرایش باید غیرفعال باشند.
5. برای بازگشت *صرفاً از حالت آزمایشی خواندنی*، برنامه را متوقف و با `bin\start.cmd` عادی راه‌اندازی کنید. از **بازگشت به Legacy بعد از ایجاد هر رکورد Native یا تغییر V2** اجتناب کنید؛ چنین بازگشتی بدون Reverse Migration ایمن نیست.

**جداسازی IAM:** Actor موجود برای نوشتن در Canonical مقدار نمایشی `PROTOTYPE` دارد. فعال‌سازی `ORG_CONTACT_CANONICAL_WRITES_ENABLED=true` در این مرحله مجاز نیست؛ نیازمند جایگزینی با Principal معتبر، Audit و UAT نوشتن است.

## ۴. R6 — Preview بازنشستگی قواعد روابط

در SQL Developer، فایل زیر را با `F5` اجرا کنید:

`database\oracle\org\relationship-rules-retirement-r6\01-ORG-R6-RELATIONSHIP-RULES-RENAME-OPT-IN.sql`

هر پنج Flag به‌طور پیش‌فرض `NO` هستند:

```sql
DEFINE APPLY_CHANGES = NO
DEFINE CONFIRM_APP_UAT = NO
DEFINE CONFIRM_APP_STOPPED = NO
DEFINE CONFIRM_DBA_BACKUP = NO
DEFINE CONFIRM_EXTERNAL_CONSUMERS = NO
```

تعداد رکوردهای جدول (هرچه باشد) در Preview محاسبه می‌شود؛ هیچ فرضی درباره صفر بودن رکوردها نداریم. Precheck، قرارداد شش‌ستونی، PK و Unique، سه FK خروجی، نبود FK ورودی، Stored Dependency، Synonym، Trigger، ارجاعات کد متنی و Scheduler Job را بررسی می‌کند. سازگاری واقعی با SQLهای پویا، گزارش‌های خارجی و کاربران دارای دسترسی مستقیم باید **دستی** تأیید شود؛ `BLOCKERS=0` به‌تنهایی مجوز DDL نیست.

در صورت وجود وابستگی، وضعیت `BLOCKERS` غیرصفر خواهد بود و Rename در حالت Apply نیز متوقف می‌شود. هیچ Script قدیمی Rename/Drop نباید اجرا شود.

## ۵. اعمال R6 پس از تأیید

فقط اگر Preview برابر `BLOCKERS=0`، UAT و تأیید DBA و مصرف‌کنندگان بیرونی ثبت شده باشد، در پنجره Maintenance و در حالی‌که Application متوقف است، پنج Flag را `YES` کنید و F5 اجرا کنید. خروجی موفق:

`ORG_R6_RULES_DEPRECATION_PASS | RENAMED_NOW=1 | ROWS=<current> | OBJECT_ID_PRESERVED=1 | NO DML`

اسکریپت خودتان را `COMMIT` نکنید؛ **Oracle DDL ذاتاً Auto-Commit دارد**. پس از اجرا، `02-ORG-R6-RELATIONSHIP-RULES-VERIFY-READONLY.sql` را اجرا کنید. انتظار `ORG_R6_RULES_VERIFY_PASS`، فعال‌بودن دو جدول روابط و نوع روابط، و وجود سه FK خروجی معتبر است. Indexها و Sequence تاریخی تحت نام قبلی باقی می‌مانند و به‌صورت خودکار Rename نمی‌شوند.

فایل `03-ORG-R6-RELATIONSHIP-RULES-RESTORE-OPT-IN.sql` برای بازگرداندن نام، پیش‌فرض Preview و نیازمند تأیید DBA است؛ بازگشت Source از Backup نصب به‌صورت مستقل ضروری است.

## ۶. معیار پذیرش

| مورد | معیار | وضعیت پیش از اجرای کاربر |
|---|---|---|
| R5-A Data / Runtime | 6930/6930، بدون مغایرت | PASS بر اساس خروجی کاربر |
| R5-B Installer | Source verified و Windows Build PASS | در انتظار اجرا |
| R5-B Read-Only | `storageMode=CANONICAL`, `writesEnabled=false`, `ready=true` | در انتظار Runtime |
| R5-B UI | مشاهده کامل و غیرفعال بودن عملیات ویرایش | در انتظار UAT |
| R6 Preview | Original=1, Archived=0, Blockers=0 | در انتظار Oracle |
| R6 Apply | Rename به `DEPRECATED_...`, حفظ شمارش و Object ID | **اجرا نشده** |
| R6 Verify | Archived=1، Original=0، سه FK خروجی، دو جدول فعال | در انتظار |
| Legacy Contact Retirement | بازنشستگی 3 جدول تماس | خارج از دامنه R5-B/R6 |

**محدودیت QA:** Verifierهای Node این بسته بدون اتصال Oracle اجرا می‌شوند. Build کامل Maven/Angular و آزمون Runtime روی ویندوز مشتری بعد از نصب انجام می‌شود. هیچ ادعایی درباره اجرای زنده Oracle در این محیط وجود ندارد.
