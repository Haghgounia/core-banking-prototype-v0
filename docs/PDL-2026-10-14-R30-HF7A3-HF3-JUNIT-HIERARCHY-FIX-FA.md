# گزارش اصلاح R30-HF7-A3/HF3 — شکست JUnit مدت مجاز و پله نرخ

**مبنای سورس:** `R30-HF7A3-HF2`، تاریخ ۱۴ اکتبر ۲۰۲۶؛ **دامنه:** اعتبارسنجی دو قاعده فرزند در Backend، بدون DDL/DML و بدون تغییر داده‌ها یا محافظ‌های عملیاتی.

## ۱) شواهد خطای دریافتی

در `build-production.cmd` ارسالی کاربر، Maven بعد از تکمیل آزمون‌های ایستای Product Builder و گذر از کامپایل به آزمون Surefire رسید و با این دو مورد متوقف شد:

- `ProductRuleHierarchyHF7A3Test.allowedTermNeedsValidUnitAndIntegralPositiveDuration` در خط ۴۵
- `ProductRuleHierarchyHF7A3Test.rateTierRequiresRateAndValidFractionAndTermUnit` در خط ۵۶

نتیجه اجرا: **۱۶۳ تست / ۲ failure / ۰ error / ۱ skipped**. هشدارهای Mockito و DOCTYPE علت توقف نبودند.

**تفاوت مهم Baseline:** کلاس `ProductRuleHierarchyHF7A3Test.java` در سورس تجمیعی رسمی HF2 که در اختیار بررسی‌کننده بوده **وجود نداشته**، با این حال روی ویندوز کاربر اجرا شده است. بنابراین بدنه دقیق دو assertion ناموفق معلوم نیست. اصلاح فعلی بر مبنای نام تست‌ها و قرارداد شناخته‌شده جداول، با آزمون‌های مستقل اعمال شده است و عبور دقیق آن کلاس باید روی سورس کاربر تأیید شود.

## ۲) تشخیص علّی

`ProductBuilderBusinessValidator.validate` اعتبارسنجی مرکزی `ProductRuleSemanticContract.validate` را اجرا می‌کند؛ اما در Baseline HF2 دو جدول `DEPOSIT_PRODUCT_ALLOWED_TERM` و `PRODUCT_RATE_TIER` از شاخه `default` بدون کنترل صریح عبور می‌کردند. در نتیجه «مدت صفر/کسری»، «نرخ خالی/نامعتبر»، «شماره پله غیردقیق» و «واحد نامشخص» می‌توانستند پیش از بررسی Oracle رد نشوند.

## ۳) قرارداد پیاده‌شده در HF3

### `DEPOSIT_PRODUCT_ALLOWED_TERM`

- `TERM_UNIT_CODE`: اجباری؛ واحدهای معنایی روز/ماه/سال (`DAY/MONTH/YEAR` یا `D/M/Y`). اگر CODE مرجع بانک عددی باشد، تبدیل آن به واحد از این قرارداد **حدس زده نمی‌شود** و باید در سرویس مرجع DPS معتبر باشد.
- `TERM_VALUE`: عدد صحیح مثبت، نه صفر، منفی یا کسری.
- `IS_ACTIVE` و `IS_DEFAULT`: اگر در payload وجود داشته باشند، باید بله/خیر صریح `0/1` باشند؛ مقدار SQL default هنگام CREATE همچنان مسئول پرکردن فیلد غایب است.
- `DISPLAY_ORDER`: در صورت درج، عدد صحیح مثبت.
- تاریخ‌های `EFFECTIVE_FROM_DATE/EFFECTIVE_TO_DATE` طبق اعتبارسنجی موجود نباید معکوس شوند.
- `TERM_CODE`: سیستمی است و در ورود اولیه فرم به‌زور درخواست نمی‌شود.

### `PRODUCT_RATE_TIER`

- `TIER_NO`: عدد صحیح مثبت و اجباری.
- `MIN_AMOUNT/MAX_AMOUNT`: غیرمنفی و در صورت تکمیل هر دو، حداکثر ≥ حداقل.
- `MIN_TERM_VALUE/MAX_TERM_VALUE`: غیرمنفی و صحیح، و در صورت تکمیل هر دو، حداکثر ≥ حداقل.
- اگر دامنه مدت تعیین شود، `TERM_UNIT_CODE` از واحدهای معتبر روز/ماه/سال الزامی است؛ واحدِ بدون دامنه مدت نیز رد می‌شود.
- `RATE_VALUE`: **اجباری** و نسبت اعشاری صفر تا یک؛ به‌عنوان مثال `0.18` یعنی ۱۸ درصد. مقدار `NULL` یا `1.2` رد می‌شود؛ صفر برای نرخ صفر درصد مجاز است.
- تاریخ‌های `VALID_FROM/VALID_TO` با کنترل عمومی موجود ارزیابی می‌شوند.

**مرجع کدهای واحد:** فهرست واقعی `DPS.REF_TERM_UNIT_CODE` و کنترل‌های Reference Option در Backend کماکان مرجع اصلی هستند. HF3 هیچ رکورد/کد تازه در جدول مرجع تولید نمی‌کند؛ سازگاری کدهای واقعی فعلی بانک باید در UAT تأیید شود.

## ۴) فایل‌های نسخه

- `backend/src/main/java/com/behsazan/corebanking/productbuilder/application/ProductRuleSemanticContract.java` — افزوده‌شدن قواعد فرزند در قرارداد مرکزی.
- `backend/src/test/java/com/behsazan/corebanking/productbuilder/application/ProductRuleChildValidationHF7A3HF3Test.java` — چهار سناریوی JUnit جدید با حالت‌های مرزی و خطا.
- `tools/verify-pdl-r30-hf7a3-hf3-child-validation.mjs` — کامپایل مستقل کد واقعی Validator و ۲۲ سناریوی بدون Oracle.
- `build-production.cmd` و `build-production.sh` — افزودن کنترل قبل از Maven.
- این گزارش در `docs/`.

**هرگونه فایل جدید محلی کاربر** مانند `ProductRuleHierarchyHF7A3Test.java` در Patch حذف یا بازنویسی نمی‌شود.

## ۵) آزمون‌ها و محدودیت‌ها

| کنترل | نتیجه آزمایش این نسخه |
|---|---|
| HF3 — Java smoke واقعی (۲۲ حالت مثبت/منفی) | PASS |
| HF3 — ۴ روش JUnit تازه با stub سبک AssertJ/JUnit | PASS؛ غیرمعادل Maven Surefire |
| HF7-A3/HF2 — ۲۷ کنترل اختصاصی | PASS |
| HF7-A2 — ۱۱ ایستا / ۲۳ Java | PASS |
| HF7-A1 — ۱۰ ایستا / ۲۷ Java | PASS |
| PB-R19 — ۵۱ ایستا | PASS |
| HF4 Closure — کنترل‌های ایستا و Java | PASS |
| Release Layout Verification | PASS |
| Maven Surefire با کلاس محلی خاص کاربر | **قابل تأیید فقط در ویندوز کاربر** |
| Angular Production / Oracle UAT | در این محیط اجرا نشده |

## ۶) نحوه نصب و معیار پذیرش

۱. از پوشه پروژه ویندوز نسخه پشتیبان بگیرید. ۲. **Patch کوچک HF3 را روی سورس HF2 فعلی Overlay کنید** و فایل‌های هم‌نام را جایگزین کنید؛ پوشه را پاک نکنید. ۳. دستورات را اجرا کنید:

```bat
cd /d D:\Projects\core-banking-prototype-v0
node tools\verify-pdl-r30-hf7a3-hf3-child-validation.mjs
node tools\verify-pdl-r30-hf7a3-hf2-product-rules.mjs
node tools\verify-release-layout.mjs
build-production.cmd
```

پذیرش زمانی است که آزمون‌های `ProductRuleHierarchyHF7A3Test` (هر ۱۱ مورد)، مجموعه آزمون‌ها و Build نهایی بدون Failure عبور کنند. در صورت تکرار خطا، فایل `backend/src/test/java/com/behsazan/corebanking/productbuilder/application/ProductRuleHierarchyHF7A3Test.java` و خروجی `backend/target/surefire-reports/*ProductRuleHierarchyHF7A3Test*` باید دریافت شود؛ **بدون دسترسی به این تست، هیچ ادعای قطعی درباره دو assertion دقیق مجاز نیست**.

## ۷) محدوده غیرمتأثر و بدهی فنی

بدون تغییر UI، Oracle، داده‌های PDL، FEE/FEE2، سیاست `canApprove/canOpen` و اتصال عملیاتی. اعتبارسنجی مستقل دو جدول افزوده شده اما HF7-A3 جامع، ارزیابی همه سلسله‌مراتب parent/child در Approval و گواهی Runtime تکمیل نشده‌اند.
