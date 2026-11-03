# CLT-R2C-HF1 | اصلاح خطای راه‌اندازی Runtime

تاریخ: 2026-11-03  
مرجع: CLT-ROADMAP-R1.0، فاز R2C  
برگرفته از: لاگ اجرای کاربر در 2026-11-03، نسخه 0.11.0.

## علت

Spring Boot پس از Build و گذر 556 آزمون (0 failure، 0 error، 1 skipped) هنگام ساخت Bean `CollateralIntroductionRepository` با خطای زیر متوقف شد:

`java.lang.IllegalArgumentException: Cannot subclass final class com.behsazan.corebanking.collateral.introduction.CollateralIntroductionRepository`

تمام Repositoryهای Collateral با `@Repository` و `public final class` تعریف شده بودند. فرآیند ترجمه استثناهای Repository می‌تواند CGLIB Proxy بسازد؛ کلاس final قابل subclass نیست. این خطا مستقل از اتصال Oracle و تنظیمات IAM است.

## اصلاح دقیق

- حذف صرفاً کلیدواژه `final` از `CollateralIntroductionRepository`.
- حذف صرفاً کلیدواژه `final` از `CollateralRegistryRepository`.
- حذف صرفاً کلیدواژه `final` از `CollateralReferenceRepository`.
- افزودن `CollateralRepositoryProxyCompatibilityTest` برای جلوگیری از بازگشت خطا در Maven Test.
- افزودن بررسی Bytecode کلاس‌های Repository در JAR نهایی؛ وجود کلاس به‌تنهایی اثبات Proxyability نیست.

## مرز امنیت و داده

- تغییر API، SQL، Oracle Schema، Configuration یا IAM انجام نشده.
- R2C `FINAL_REGISTRATION=DISABLED` و `DRAFT_WRITES_DEFAULT=OFF` باقی مانده‌اند.
- پیشرفت فاز R2 تغییر نمی‌کند؛ این یک Runtime hotfix در همان R2C است.

## شواهد آزمون

- تجزیه لاگ: Build موفق، شکست راه‌اندازی در CGLIB/Repository.
- بررسی ایستای تمام `@Repository`های Collateral: سه مورد، هر سه نیازمند حذف final.
- نصب روی کپی سورس R2C، اجرای مجدد، رد تعارض و کنترل عدم تغییر فایل‌های مشترک.
- کد و تست JUnit برای Build واقعی همراه بسته است.

**محدودیت:** در محیط تولید بسته، Build کامل و راه‌اندازی JAR روی Windows/Oracle اجرا نشده؛ `CLT_R2C_HF1_FULL_BUILD_AND_JAR=PASS` تنها در خروجی دستور Build خود کاربر قابل اثبات است. نتیجه `bin/start.cmd` نیز باید جداگانه موفق باشد. این Hotfix هیچ وعده‌ای درباره اتصال Oracle یا فعال‌شدن ثبت قطعی نمی‌دهد.
