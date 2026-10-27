# FEE3-BE-R1 | گزارش کنترل اولیه Backend

**مبنای نسخه:** Core Banking Prototype 0.11.0 + FEE3 UI R8 + HF1 (26 اکتبر 2026)

## خروجی فنی
- Backend اختصاصی: `fee3/domain`, `fee3/application`, `fee3/oracle`, `fee3/web`
- APIهای خواندن: Readiness، کاتالوگ صفحه‌بندی‌شده، جزئیات Aggregate، عملیات، فرمول‌ها، منابع، واژگان
- APIهای نوشتن: ایجاد و ویرایش اولین پیش‌نویس UAT جدید؛ تمام نوشتن‌ها پیش‌فرض غیرفعال، با توکن UAT در سطح سرور
- نوشتن کاملاً تراکنشی روی ۵ جدول اصلی `FCE_CALC_ITEM`, `FCE_TARIFF_VERSION`, `FCE_VERSION_SCOPE`, `FCE_CALC_RULE`, `FCE_TEST_CASE` به همراه `FCE_AUDIT_EVENT`
- عدم تغییر Seed، FEE، FEE2، Product Builder، Accounting و Posting
- هیچ DDL/DML مستقل یا تغییر Connection/رمز موجود در Patch قرار ندارد

## آزمون‌های انجام‌شده
| آزمون | نتیجه | حدود اعتبار |
|---|---|---|
| کنترل ساختاری Backend | 52/52 PASS | مسیرها، Feature Gate، قفل‌گذاری، Audit، ممنوعیت انتشار |
| Policy UAT با Java 21 | 18/18 PASS | آزمون واقعی اجرای کد سیاست با استاب HttpStatus؛ نه اتصال Oracle |
| کامپایل Java 21 کد جدید با امضاهای شبیه‌سازی Spring/JDBC/Jackson | PASS | کنترل Syntax و نوع؛ نه Maven واقعی |
| ستون‌های Oracle در مدل رسمی پیوست | 51/51 MATCH | تطبیق DDL متن مرجع؛ نه Metadata زنده |
| R8 UI Static | 59/59 PASS | بدون رندر/مرورگر |
| R8-HF1 AOT Date Guard | 9/9 PASS | کنترل منبع |
| R8 Source Integrity | 12/12 PASS | بدون Build |
| SQL واقعی Oracle / REST اجراشده | اجرا نشده | نیازمند دیتابیس و Backend شما |
| Maven Spring Boot کامل و JUnit Maven | اجرا نشده | Maven و وابستگی‌های پروژه در این محیط حاضر نبود |
| E2E ذخیره در Oracle، تعارض همزمان، rollback | اجرا نشده | گام UAT ضروری |

## محدودیت آگاهانه
1. عنوان، نوع مبلغ و تاریخ اعتبار در سرور بررسی می‌شوند؛ اما قرارداد همه زمینه‌های اختصاصی UI هنوز برای ذخیره بدون اتلاف داده کامل نشده است.
2. فرمول‌ها فقط از واژگان مرجع موجود قابل ارجاع‌اند؛ هیچ موتور محاسبه یا نتیجه مالی رسمی در Backend R1 وجود ندارد.
3. مدل پیش‌نویس فقط UAT نسخه ۱ را می‌پذیرد؛ Seed قابل ویرایش نیست، و نسخه‌های دارای آزمون اجراشده از بازنویسی Child محافظت می‌شوند.
4. وجود واقعی محصول در PDL و قرارداد آن، کنترل کامل وثایق ترکیبی، Maker/Checker و انطباق نرخ با مرجع مقرراتی جزو دروازه R2/R3 هستند.
5. تمام JSON تخصصی باید قبل از اتصال Write از UI دارای نگاشت نسخه‌گذاری‌شده قطعی باشند.

## دروازه پذیرش R1
پس از نصب در سیستم بانک: `build-production.cmd`، Readiness، خواندن حداقل یک `FCE-` موجود، بررسی وضعیت Write=false، سپس با فعال‌سازی آگاهانه UAT، یک POST نمونه و کنترل Read-back/Audit/عدم تغییر Seed. پس از آن خاموش‌کردن Write.
