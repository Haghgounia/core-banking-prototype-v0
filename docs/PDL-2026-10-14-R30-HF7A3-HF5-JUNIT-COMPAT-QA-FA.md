# R30-HF7-A3/HF5 — سازگاری آزمون تاریخی و کنترل شواهد قواعد

**مبنای دقیق:** R30-HF7-A3/HF4  (ZIP منتشرشده در ۱۴ اکتبر ۲۰۲۶)

## علت خطا

در ماشین ویندوز، فایل آزمون قدیمی `ProductRuleHierarchyHF7A3Test.java` وجود داشت ولی در ZIP رسمی HF4 نبود. پنج خطای `testCompile` به فراخوانی متد حذف‌شده `ProductRuleHierarchyAudit.audit(...)` مربوط بود. بررسی سورس ارسالی نشان داد حتی با اضافه‌کردن این متد، دو سناریوی Reflection دیگر هم نیازمند اصلاح هستند:

- `hasActiveAllowedTerm(...)` و `hasValidPricingTiers(...)` در HF4 از نوع `Finding` هستند، نه `boolean`.
- `validateLoanCivilContribution(...)` دیگر عضو `ProductRuleGovernanceService` نیست؛ کنترل عقد مشارکت مدنی اکنون از مسیر `ProductBuilderService` و `LoanCivilPartnershipPolicy` اجرا می‌شود.
- سناریوی قدیمی والد/فرزند در Mock خود، کلید خارجی برخی رکوردهای فرزند را نداشت، در حالی که قرارداد HF4 کلید والد را کنترل می‌کند.

## تغییرات

1. فایل JUnit تاریخی با API جدید سازگار شد؛ هیچ‌کدام از سناریوهای هدف حذف نشد، ولی آزمون مربوط به عقد مشارکت مدنی اکنون Policy واقعی را به‌صورت مستقل کنترل می‌کند. آزمون‌های اجرای Persistence همچنان با تست‌های دیگر پوشش داده می‌شوند.
2. قرارداد read-only `ProductRuleHierarchyAudit.audit(...)` برای سه رابطه `PRODUCT_CHANNEL_RULE → PRODUCT_CHANNEL_OPERATION`، `PRODUCT_ELIGIBILITY_RULE → LOAN_ELIGIBILITY_EXTENSION` (صرفاً تسهیلات) و `DEPOSIT_PRODUCT_CLOSURE_RULE → PRECHECK/SETTLEMENT/APPROVAL` اضافه شد. رکورد فرزند اختیاری است ولی رکورد موجود باید معتبر، دارای FK صحیح و دارای تاریخ معتبر باشد. عدم دسترسی به شواهد فرزند موجب رد نتیجه ممیزی می‌شود.
3. `IS_ACTIVE` با ۰/۱ عددی، اعشار متناظر (`0.0` / `1.0`) و Boolean به شکل صحیح تفسیر می‌شود. مقدار نامعتبر مانند ۹ دیگر تاریخچه غیرفعال فرض نمی‌شود و مانع تصویب می‌گردد.
4. تکرار مدارک و استعلام‌ها با کلید کسب‌وکاری نوع+مرحله و تکرار شماره پله در بازه‌های زمانی هم‌پوشان در `ProductRuleEvidenceEvaluator` آشکار می‌شود.
5. کنترل جدید Java با ۲۸ سناریوی واقعی و ۱۴ کنترل ایستا به Build ویندوز و لینوکس متصل شد. کامپایل کلاس JUnit قدیمی با جایگزین‌های **صرفاً کامپایل** JUnit/Mockito در همین Verifier کنترل می‌شود. این جایگزین‌ها اثبات اجرای واقعی JUnit/Mockito نیستند.

## مرز مهم

متد سازگار `audit(...)` در HF5 به عنوان قرارداد قابل آزمایش اضافه شده، **اما هنوز برای این سه سلسله‌مراتب جدید به مسیر محاسبه `canApprove` در `ProductRuleGovernanceService.report(...)` متصل نشده است**. این اتصال باید در ادامه ممیزی جامع HF7-A3 با آزمون‌های گزارش حاکمیت انجام شود. برای مدت مجاز سپرده و قیمت‌گذاری پله‌ای، اتصال HF4 قبلی برقرار است.

هیچ Migration، داده‌سازی، عملیات Oracle، اصلاح Angular یا تغییر محافظ‌های `canOpen` در این بسته انجام نشده است. نتیجه **Build Maven کامل** و آزمون با Oracle واقعی همچنان باید روی محیط کاربر تأیید شود.

## نتیجه کنترل محلی

- HF5: ۲۸ سناریوی Java واقعی PASS، ۱۴ کنترل ایستا PASS.
- کامپایل واقعی سورس JUnit تاریخی با stubهای سبک JUnit/Mockito: PASS (صرفاً compile-only).
- HF7-A1: ۳۷ PASS؛ HF7-A2: ۳۴ PASS؛ HF4: ۳۷ PASS؛ HF2/HF3 و رگرسیون‌های منتخب R19/R20/R25/R30: PASS.
- Release Layout: PASS؛ ZIP Integrity و استخراج/Overlay: کنترل در بسته‌بندی انجام می‌شود.
- Angular Production + Maven Surefire در این محیط اجرا نشده‌اند.

## نحوه نصب و معیار پذیرش

1. از سورس فعلی و فایل JUnit مورد بحث پشتیبان بگیرید.
2. **فقط روی نسخه HF4**، Patch کوچک HF5 را در ریشه پروژه Extract/Overwrite کنید. فایل Manifest در `release/` قرار دارد، نه ریشه پروژه.
3. در CMD مسیر پروژه را باز کنید و اجرا کنید:

```bat
cd /d D:\Projects\core-banking-prototype-v0
node tools\verify-pdl-r30-hf7a3-hf5-compat.mjs
node tools\verify-pdl-r30-hf7a3-hf4-hierarchy.mjs
node tools\verify-release-layout.mjs
build-production.cmd
```

4. پذیرش نهایی: هیچ خطای کامپایل در `ProductRuleHierarchyHF7A3Test`، هیچ Failure در Surefire، و `BUILD SUCCESS`.
5. پس از موفقیت Build، مراحل بعدی HF7-A3 عبارت‌اند از اتصال ممیزی‌های فرزند جدید به گزارش تصویب، سپس بررسی پوشش کانال/عملیات و تسویه سپرده و اعتبارسنجی دامنه تسهیلات.
