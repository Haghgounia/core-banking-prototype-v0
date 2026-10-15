# R30-HF7A2-HF1 — اصلاح ناسازگاری داده آزمون‌های تاریخی JUnit با قرارداد معنایی HF7

تاریخ: ۲۰۲۶-۱۰-۱۴  |  طبقه‌بندی: Development / UAT  |  Oracle: **بدون DDL/DML/Migration**

## علت واقعی شکست Build

خروجی `build-production.cmd` در محیط ویندوز نشان داد Angular Production Build و کامپایل Java/Main و Test موفق هستند، اما Surefire چهار `ProductBuilderValidationException` در دو تست قدیمی ثبت می‌کند:

1. `LoanProductFinalUxR25Test.contributionStoredAsFractionAndAcceptsNoneOrBoundaries` به دلیل فقدان دو پرچم صریح `INTEREST_DISCOUNT_ALLOWED` و `PENALTY_HOLIDAY_CALCULATION`.
2. `LoanProductFinalUxR25Test.interestDiscountDaysFollowTheExplicitPermissionSwitch` به دلیل فقدان پرچم `PENALTY_HOLIDAY_CALCULATION`.
3. `LoanProductFinalUxR25Test.collateralRulePriorityZeroMeansUnspecifiedAndOneMeansFirst` به دلیل نبود شناسه واقعی نوع وثیقه `LOAN_PRODUCT_COLLATERAL_ID` و پرچم‌های چهارگانه وثیقه.
4. `ProductBuilderBusinessValidatorTest.openingAmountsAllowNullButNotNegativeOrReversedBounds` چون داده مثبت آزمون حداقل مبلغ افتتاح `1000` را بیش از سقف مانده مجاز `500` می‌گذاشت، که تحت HF7-A2 پیکربندی ناممکن محسوب می‌شود.

**اینها خطای داده و Fixtures آزمونِ پیش از HF7 بودند، نه دلیلی برای برداشتن کنترل ایمنی Backend.** بعضی آزمون‌های منفی نیز پیش از اصلاح، صرفاً به سبب خالی بودن پرچم‌ها پاس می‌شدند و موضوع اصلی آزمون را واقعاً آزمایش نمی‌کردند.

## دامنه دقیق Patch

فقط این دو کلاس JUnit اصلاح شده‌اند:

- `backend/src/test/java/com/behsazan/corebanking/productbuilder/application/LoanProductFinalUxR25Test.java`
- `backend/src/test/java/com/behsazan/corebanking/productbuilder/application/ProductBuilderBusinessValidatorTest.java`

Fixtureهای مثبت اکنون پرچم‌ها و شناسه مرجع لازم را صریحاً تنظیم می‌کنند و مقدار `MAX_ALLOWED_BALANCE` در آزمون معتبر به `2500` تغییر یافته است. آزمون‌های منفی اکنون پیام خطاهای مرتبط با کسر آورده، غیرفعال بودن تخفیف، نبود مرجع وثیقه، و ناممکن بودن سقف مانده را نیز کنترل می‌کنند.

**هیچ تغییری در `ProductRuleSemanticContract`, `ProductBuilderBusinessValidator`, Business Service, Frontend, R30/HF5/HF6, `canApprove`, `canOpen`, Oracle, FEE و FEE2 داده نشده است.**

## راهنمای نصب Patch تجمیعی HF1 روی HF7-A2 فعلی

از پروژه فعلی Backup بگیرید؛ ZIP کوچک HF1 را در **ریشه** همان پروژه باز کنید و برای جایگزینی دو تست اجازه Overwrite بدهید. فایل‌های دیگر را پاک نکنید و نسخه کامل قبلی را مجدداً روی پروژه نصب نکنید.

دستورات بررسی در CMD:

```bat
cd /d D:\Projects\core-banking-prototype-v0
node tools\verify-release-layout.mjs
node tools\verify-pdl-r30-hf7a1-semantic.mjs
node tools\verify-pdl-r30-hf7a2-temporal.mjs
build-production.cmd
```

## آزمون اجراشده و محدودیت شواهد

- با JDK 21، چهار کلاس اصلی Java (شامل Validator و Semantic Contract واقعی) و دو کلاس JUnit قدیمی با جایگزین سبک `@Test`/`Assertions` فقط برای آزمون مستقل کامپایل و اجرا شدند. پیش از Patch: **۷ موفق، ۴ خطا**؛ بعد از Patch: **۱۱ موفق، صفر خطا**. این تست مستقل جایگزین Maven/Surefire واقعی نیست.
- آزمون‌های HF7-A1: `27 Java + 10 Static` موفق؛ HF7-A2: `23 Java + 11 Static` موفق.
- اجرای کامل Maven در محیط بسته‌بندی این Patch به دلیل نبود توزیع/مخزن وابستگی Maven ممکن نبوده و **موفقیت Build نهایی ادعا نمی‌شود**.
- اجرای Oracle UAT یا E2E عملیاتی انجام نشده؛ محافظ‌های توقف `canOpen` پابرجا هستند.

## نکات لاگ اصلی

- `Mockito is currently self-attaching` هشدار غیرمسدودکننده است؛ دلیل شکست نبود.
- `[Fatal Error] DOCTYPE is disallowed` در خروجی تست پارسر XMI ظاهر شد اما خود آن تست با `Errors:0` خاتمه یافته؛ جزو چهار خطای مسدودکننده نیست.
- تست Oracle UAT مربوط به کارمزد در گزارش اولیه Skipped بوده است. موفقیت Maven به‌تنهایی به معنی تکمیل UAT نیست.

## مبنای استفاده

این Patch مخصوص سورسی است که HF7-A1 و HF7-A2 روی آن نصب و دو دستور `verify-pdl-r30-hf7a*.mjs` با موفقیت اجرا شده است. Patch را **روی Baseline بدون HF7-A2** نصب نکنید.
