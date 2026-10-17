# گزارش QA قابل استناد / PDL Business UAT RC1

**Baseline فایل:** `core-banking-prototype-v0-2026-10-17-0914.zip`  
**محدوده:** Source-only & isolated `javac` Java harness در محیط Linux حاضر، نه اتصال Oracle یا Maven/Surefire کامل.

## نتایج واقعی

| ابزار | نتیجه | لاگ |
|---|---|---|
| `verify-release-layout` | PASS | `verify-release-layout.log` |
| `verify-pdl-r30-hf7a3-hf9-single-rate` | PASS | `verify-pdl-r30-hf7a3-hf9-single-rate.log` |
| `verify-pdl-r30-hf7a3-hf8-final` | PASS | `verify-pdl-r30-hf7a3-hf8-final.log` |
| `verify-pdl-r30-hf7a3-hf7-closure` | PASS | `verify-pdl-r30-hf7a3-hf7-closure.log` |
| `verify-pdl-r30-hf7a1-semantic` | PASS | `verify-pdl-r30-hf7a1-semantic.log` |
| `verify-pdl-r30-hf7a2-temporal` | PASS | `verify-pdl-r30-hf7a2-temporal.log` |
| `verify-pdl-r30-hf7a3-hf3-child-validation` | PASS | `verify-pdl-r30-hf7a3-hf3-child-validation.log` |
| `verify-pdl-r30-hf7a3-hf4-hierarchy` | PASS | `verify-pdl-r30-hf7a3-hf4-hierarchy.log` |
| `verify-pdl-r30-hf7a3-hf6-publication` | PASS | `verify-pdl-r30-hf7a3-hf6-publication.log` |
| `verify-pdl-r30-hf7-safety-gate` | PASS | `verify-pdl-r30-hf7-safety-gate.log` |
| `verify-pdl-r30-hf7a3-hf5-compat` | PASS | `verify-pdl-r30-hf7a3-hf5-compat.log` |
| `verify-pdl-r30-hf7a3-hf2-product-rules` | PASS | `verify-pdl-r30-hf7a3-hf2-product-rules.log` |
| `verify-pdl-ui-alignment-r10t` | PASS | `verify-pdl-ui-alignment-r10t.log` |
| `verify-pdl-ux-oct11` | PASS | `verify-pdl-ux-oct11.log` |
| `verify-pb-r19` | PASS | `verify-pb-r19.log` |
| `verify-pdl-r30-hf7a3-integrity` | FAIL / HISTORICAL VERIFIER | `verify-pdl-r30-hf7a3-integrity.log` |

جمع: **۱۵ آزمون/Verifier موفق، ۱ ابزار تاریخی شکست‌خورده که به‌عنوان بدهی ثبت شده است.**

## اثبات محدوده

- HF9: `JAVA_PASS=29`, `STATIC_PASS=19`, FAIL=0.
- HF8: کنترل‌های Static و Java و سناریوهای Governance در Harness واقعی اجرا شدند؛ گزارش‌های جداگانه در پوشه `evidence` هستند.
- HF7 safety: کنترل source level برای `canOpen` موفق؛ هیچ UAT تراکنشی اجرا نشده است.
- Verifier قدیمی `verify-pdl-r30-hf7a3-integrity.mjs` شکست در انتظار متن محلی منسوخ، بدون استنتاج خرابی Business Runtime.

## آزمون‌های اجرا نشده

1. Build کامل Frontend با Dependency نصب‌شده، Maven/Surefire روی ویندوز و اجرای واقعی تمام ۲۹۸ یا تعداد جدید آزمون‌ها؛ **نتیجه `PASS` ادعا نمی‌شود**.
2. تطبیق ۵۴ جدول Oracle زنده، بررسی داده‌های قدیمی و اجرای سناریوهای حقیقی بانک.
3. تست UI/E2E، دسترسی نقش، Maker/Checker و تطبیق مبالغ واقعی و تراز مالی.
4. امنیت وابستگی‌ها / npm audit روی همین نسخه.

## دستور تکرار

HF9-HF1 در نسخه 0914 موجود است؛ آن Patch را تکرار نکنید. از ریشه پروژه:

```bat
node tools\verify-pdl-r30-hf7a3-hf9-single-rate.mjs
node tools\verify-pdl-r30-hf7a3-hf8-final.mjs
node tools\verify-release-layout.mjs
build-production.cmd > "%TEMP%\PDL-BUAT-RC1-build.log" 2>&1
```

**حتی اگر Build موفق شد، این نتیجه مجوز `canOpen` یا اعمال نرخ بانکی در Runtime نیست.**

## تکرار آزمون روی Baseline 0914 (شواهد جدید)

تمام ۱۶ Verifier جدول بالا در سورس استخراج‌شده از ZIP 0914 **دوباره اجرا شدند**. نتیجه: ۱۵ PASS و همان یک FAIL تاریخی `verify-pdl-r30-hf7a3-integrity` (در انتظار متن و ساختار قدیمی audit).

HF9: `JAVA_PASS=29 FAIL=0`، `STATIC_PASS=19 FAIL=0`؛ HF8: `STATIC_PASS=28 FAIL=0`، به‌همراه `HF9_GOVERNANCE_PASS=7 FAIL=0` و کامپایل مستقل سورس JUnit نسخه HF9.

تغییر جدید 0914 در `ProductRuleHierarchyAudit`، رد صریح جزء قیمت‌گذاری بدون پله خودش (`HF9-COVERAGE-GUARD`) است؛ سناریوی JUnit نیز بررسی FK دو جزء 814 و 815 را اضافه می‌کند. **اجرای Maven/JUnit با Surefire روی این ZIP در این محیط انجام نشده است.** اثبات Oracle parity و runtime UAT نیز همچنان باز است.

تمام لاگ‌های پوشه `evidence/` در بسته RC1.1 از اجرای روی نسخه 0914 تولید شده‌اند، نه نسخه قدیمی 0649.
