# HF9-HF1 — اصلاح محدود Fixture آزمون تصویب قیمت‌گذاری

**Baseline:** `core-banking-prototype-v0-2026-10-16-2014-HF9-INTEGRATED.zip` (HF9 + تغییرات جدید Posting کاربر)

**وضعیت:** Patch آزمون آماده شده و تست‌های مستقل موفق‌اند؛ Build کامل Maven/Angular روی Windows پس از نصب Patch هنوز اجرا/تأیید نشده است.

## شواهد دریافت‌شده

خروجی `findstr` ارسالی کاربر از Build ویندوز:

- Maven `BUILD FAILURE`
- Tests run: 298, Failures: 1, Errors: 0, Skipped: 1
- تنها کلاس دارای شکست: `ProductPricingPublicationHF7A3HF9Test` (۷ آزمون)
- `ProductRateTierHF7A3HF9Test`: تعداد ۲۹، بدون شکست
- PostingConfigurationPreviewEngineTest: تعداد ۳۸، بدون شکست؛ PostingReadinessCatalogTest: تعداد ۲، بدون شکست
- یک آزمون FEE Oracle UAT همچنان Skipped است.

تذکر: در خروجی ارسالی اسم متد ناموفق و stack trace وجود ندارد. بازتولید مستقیم نشان داد که آزمون `secondComponentCannotBorrowCoverageFromFirst` با Fixture قبلی دقیقاً به جای `CONFLICT`، `INVALID_SEMANTICS` می‌گیرد؛ این خطا با شمار شکست گزارش‌شده منطبق است. برای تأیید قطعی این‌که همان متد در Maven ناموفق بوده، گزارش کامل Surefire لازم است.

## علت فنی و اصلاح

Mock مربوط به `PdlProductBuilderRepository.search(...)` در این کلاس برای داده‌های فرزند، پارامتر `PRICING_COMPONENT_ID` را فیلتر نمی‌کرد. در آزمون مؤلفه دوم (`815`) که واقعاً هیچ پله‌ای ندارد، دو پله متعلق به مؤلفه اول (`814`) برگردانده می‌شد. ممیزی واقعی به درستی مشکل FK را `INVALID_SEMANTICS` تشخیص می‌داد، درحالی‌که سناریوی آزمون، فقدان پوشش نرخ مؤلفه دوم (`CONFLICT`) را می‌خواست.

فقط در Fixture همین آزمون، یک سوییچ `honorTierForeignKey` اضافه شد. این سوییچ **فقط برای همان سناریو** فعال می‌شود تا `search` مطابق فیلتر واقعی Oracle نتایج مؤلفه را برگرداند. آزمون مستقل «بازگشت فرزند با FK غلط» عمداً بدون تغییر باقی ماند تا کنترل Fail-Closed آن نیز حفظ شود.

## محدوده تغییرات

- **اصلاح‌شده:** `backend/src/test/java/com/behsazan/corebanking/productbuilder/application/ProductPricingPublicationHF7A3HF9Test.java`
- **اضافه‌شده:** همین فایل QA در `docs/`
- **بدون تغییر:** تمام فایل‌های Java عملیاتی، frontend، `build-production.cmd`, Oracle DDL/DML، FEE، FEE2، Posting و منطق `canOpen`.

## شواهد اجرای محلی مستقل (بدون Oracle)

- `verify-pdl-r30-hf7a3-hf9-single-rate.mjs`: **29 Java + 19 static, FAIL=0**.
- `verify-pdl-r30-hf7a3-hf8-final.mjs`: **24 Java + 28 static, FAIL=0**؛ شامل کامپایل سورس واقعی `ProductPricingPublicationHF7A3HF9Test.java` با Signature های آزمون.
- اجرای Java مستقل با **کلاس واقعی** `ProductRuleGovernanceService` در ۷ سناریوی متناظر: **۷ موفق**.
- بازتولید اختلاف وضعیت برای مؤلفه ۸۱۵: بدون فیلتر `INVALID_SEMANTICS`، با فیلتر صحیح `CONFLICT`.
- `verify-release-layout.mjs`: موفق.

**این اجراها معادل Maven/JUnit و UAT واقعی Oracle نیستند.** ادعای `BUILD SUCCESS` صرفاً پس از بازاجرای کامل Build ویندوز پذیرفته خواهد شد.

## شیوه نصب

1. از پروژه فعلی نسخه پشتیبان تهیه کنید.
2. فایل `PDL-R30-HF7A3-HF9-HF1-JUNIT-FIX-PATCH.zip` را در ریشه `D:\Projects\core-banking-prototype-v0` استخراج کنید. مسیرهای داخل ZIP نسبی‌اند.
3. برای اجرای سریع کلاس ناموفق:

```bat
cd /d D:\Projects\core-banking-prototype-v0\backend
mvnw.cmd "-Dtest=ProductPricingPublicationHF7A3HF9Test" test
```

4. سپس Build کامل:

```bat
cd /d D:\Projects\core-banking-prototype-v0
build-production.cmd > "%TEMP%\HF9-HF1-build.log" 2>&1
findstr /C:"BUILD SUCCESS" /C:"BUILD FAILURE" /C:"Tests run:" "%TEMP%\HF9-HF1-build.log"
```

5. لاگ کامل یا لاگ Surefire کلاس مذکور را جهت پذیرش ارسال کنید.

## نکات انضباط انتشار

- Patch فقط روی **HF9 سازگارشده با نسخه 2014** یا سورسی که همان فایل آزمون Baseline را دارد اعمال شود؛ نسخه HF8 به‌تنهایی پایه نصب این Fix نیست.
- جایگزینی کل پروژه با ZIP قدیمی HF9 توصیه نمی‌شود؛ ZIP یکپارچه HF9-HF1 تغییرات Posting را حفظ کرده است.
- نتیجه پیش‌بینی‌شده پس از رفع همین شکست، ۲۹۸ آزمون با صفر Failures/Errors و یک Skipped است؛ این نتیجه هنوز در Maven ویندوز مشاهده نشده است.
