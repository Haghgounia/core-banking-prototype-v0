# گزارش QA بسته CLT-R1B — 2026-11-03

**مرجع مسیر:** `CLT-ROADMAP-R1.0` | **Baseline:** Core Banking Prototype 0.11.0 + CLT-R1A  
**محدوده تغییر:** فقط منابع `collateral` و اسناد/SQL اختصاصی CLT؛ ۳ فایل مشترک Frontend و IAM دست‌نخورده.

## نتایج تأییدشده (ایستا و نصب روی کپی سورس)

| آزمون | شاهد | نتیجه |
|---|---|---|
| نصب روی Baseline واقعی 0.11.0 پس از نصب R1A | `CLT_R1A_INSTALLED=PASS` و `CLT_R1B_INSTALLED=PASS; CHANGED=37` | PASS |
| نصب مجدد بدون اثر | `CLT_R1B_INSTALLED=PASS; CHANGED=0` | PASS |
| تطبیق فایل‌های قدیمی R1A قبل از حذف | `CLT_R1B_LEGACY_CLASSES=11` و SHA بر اساس Manifest R1A | PASS |
| عدم تغییر فایل‌های مشترک/ IAM | `SHARED_MERGES=0; IAM_TOUCH=NONE` | PASS در مقایسه نصب آزمایشی |
| Catalog مرجع | ۱۷ جدول/۲۰۴ ستون/۵۳ پیش‌نمایش Seed فنی | PASS |
| نام‌گذاری | ۹ کلاس Java و ۱ تست با نام پایدار، حذف `CltR1a*.java` | PASS |
| Parsing نحوی Java | `CLT_R1A_JAVA_SYNTAX_PASS=16` با Java Compiler AST | PASS صرفاً syntax |
| آزمون مستقل Java Policy | `CLT_REFERENCE_POLICY_VALIDATION_PASS=8` | PASS |
| Parsing نحوی TypeScript | `CLT_TS_PARSE_PASS` برای دو فایل TS تغییرکرده | PASS صرفاً syntax |
| حفاظت از تغییرات کاربر | تعارض عمدی SHA روی `CollateralReferenceController`، `EXISTING_MODULE_FILE_DIFFERS` | PASS (متوقف شد) |
| محدودیت SQL نصب | Installer هیچ Oracle/SQLcl/DDL/DML اجرا نمی‌کند | PASS ایستا |
| Gate امنیت پیش‌فرض | هر دو Read/Write غیرفعال، JWT و Scope اختصاصی الزامی | PASS بازبینی کد/تنظیمات، نه آزمون JWT واقعی |

## محدودیت‌های صریح

- Maven executable / مخزن Dependency کامل Spring Boot 4.1.0 در این محیط در دسترس نبود؛ **Build واقعی Java، Spring context loading و JUnit اجرا نشده‌اند**. Java Parser به‌جای Compile علامت‌گذاری نمی‌شود.
- وابستگی‌های Angular 21 محلی در اختیار نبود؛ **Angular template compiler، تست TypeScript semantic و Build Frontend اجرا نشده‌اند**. فقط TS transpile syntax انجام شده است.
- هیچ Oracle/DBA/IdP واقعی متصل نبود؛ **Migration، Privilege، تراکنش Maker/Checker، تأیید JWT، Audit و UAT بانکی اجرا یا تأیید نشده‌اند**.
- DDL ارائه‌شده فقط پیشنهاد رفع شکاف G-15 و نیازمند بازبینی/تصویب DBA/بانک است؛ G-10 همچنان OPEN است.
- Oracle Write پیش‌فرض غیرفعال است. هیچ SQL واقعی روی CLT اجرا نشده است.

## اثر نصب

Installer خروجی R1A را ارتقا می‌دهد، نه کل پروژه را جایگزین کند: ۱۱ فایل Legacy با تطبیق SHA حذف، ۲۶ فایل جدید/به‌روزشده اعمال و در مجموع ۳۷ تغییر فایل انجام شد. ۳ فایل مشترک Frontend تغییری نداشتند. شماره نسخه اصلی `VERSION=0.11.0` حفظ شد. هر فایل قبلی پیش از حذف/تعویض در `patch-backups/clt-r1b-*` پشتیبان‌گیری شد.

## Gate بعدی R1

۱. Build واقعی Maven/Angular روی Windows Master؛ ۲. تأیید Data Dictionary/DDL G-15 توسط DBA؛ ۳. صدور مجوز دو حساب حداقل دسترسی، JWT و Scopeها از IAM؛ ۴. آزمون دو شخص مستقل Maker/Checker، Undo/Redo/Concurrency/Audit؛ ۵. تصویب سیاست‌های G-10 و UAT ستاد. تا آن زمان، **R1 خروجی آماده Production نیست**.
