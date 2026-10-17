# PDL — بسته پذیرش طراحی کسب‌وکار (Business UAT RC1)

**تاریخ:** ۲۰۲۶-۱۰-۱۷  
**Baseline:** `core-banking-prototype-v0-2026-10-17-0914.zip` (HF9-HF1 already included)  
**شناسه بسته:** `PDL-R30-HF7A3-HF9-HF1-BUSINESS-UAT-RC1.1-0914`  
**وضعیت:** `SOURCE_REGRESSION_PASS / EXPERT_UAT_PENDING / WINDOWS_MAVEN_PENDING / ORACLE_PARITY_PENDING / OPERATIONAL_NOT_QUALIFIED`

## هدف و مرز آزمون

برای بازبینی مشترک کسب‌وکار سپرده، تسهیلات، کارگزاری، عملیات، ریسک، انطباق و معماری. این **نسخه کاندید ارزیابی طراحی** است؛ نه انتشار عملیاتی و نه گواهی قطعی صحت همه قواعد و فیلدها. نرم‌افزار فعلی به‌دلیل محافظ‌های عمدی `canOpen=false` برای سپرده عادی، تسهیلات و نوسترو/وسترو، **نباید برای افتتاح/اعطای واقعی یا Posting** استفاده شود.

## خروجی‌ها

- `PDL-BUSINESS-UAT-RC1-TESTBOOK-FA.xlsx`: **139** سناریوی قابل ثبت نتیجه (از جمله **۵۴** مورد پوشش فیزیکی)؛ دفتر **۶۶۱** فیلد از **۴۷** جدول مدل مرجع؛ **18** تصمیم باز؛ شواهد QA و صورتجلسه امضا.
- `PDL-BUAT-RC1-SCENARIOS.csv`: سناریوها برای ورود در ابزار مدیریت آزمون.
- `PDL-BUAT-RC1-54-TABLES.csv` و `PDL-BUAT-RC1-661-FIELD-RISKS.csv`: مبنای پوشش Source-only، نه تأیید Oracle.
- `PDL-BUAT-RC1-DECISIONS.csv`: دفتر مصوبات لازم؛ **بدون مصوبه ساختگی**.
- `PDL-BUSINESS-UAT-RC1-CHECKLIST-FA.md`: چک‌لیست الزام‌های خروج و تفکیک سطوح تأیید.
- `PDL-BUSINESS-UAT-RC1-GAPS-FA.md`: خطرها/کارهای باز.
- `PDL-BUSINESS-UAT-RC1-QA-FA.md`: آزمون‌های اجراشده و محدودیت شواهد.
- `PDL-BUSINESS-UAT-RC1-ORACLE-METADATA-READONLY.sql`: فقط استخراج فراداده بدون DML/DDL.

## آغاز کار — فقط در محیط آزمایشی ایزوله

۱. **مسئول فنی** ZIP سورس را در دایرکتوری جدید استخراج و کنترل Hash/نسخه کند؛ روی سورس در حال توسعه مستقیم Overlay نشود.
۲. Build کامل Windows را اجرا کرده، `BUILD SUCCESS` و Maven `Failures=0, Errors=0` را همراه Angular Production بررسی کند. هیچ‌کس صرف موفقیت Node verifier را جایگزین Maven نکند.
۳. DBA با مجوز لازم، اسکریپت Oracle فقط خواندنی را روی Schema صحیح اجرا و گزارش اختلاف را مستند کند؛ **اجرای این SQL به معنای اصلاح دیتابیس نیست**.
۴. کارشناس هر حوزه شیت «سناریوها» را برای حوزه خود و شیت «ریسک ۶۶۱ فیلد» را برای بخش مربوطه تکمیل کند. از داده مشتری واقعی استفاده نشود.
۵. نتیجه هر ردیف یکی از «قبول / رد / مسدود محیطی / نیازمند تصمیم» باشد؛ Failure باید شرح، گام بازتولید و شواهد داشته باشد.
۶. مصوبات D01 تا D18 را فقط مالک مجاز امضا کند. اگر تصمیم باز است، آزمون وابسته نباید PASS نهایی بخورد.
۷. صورتجلسه پذیرش امضا شود؛ تا آن زمان وضعیت انتشار RC باقی بماند.

## دستورهای لازم در ویندوز

```bat
cd /d D:\Projects\core-banking-prototype-v0
node tools\verify-pdl-r30-hf7a3-hf9-single-rate.mjs
node tools\verify-pdl-r30-hf7a3-hf8-final.mjs
node tools\verify-release-layout.mjs
build-production.cmd > "%TEMP%\PDL-BUAT-RC1-build.log" 2>&1
```

اگر مسیر استخراج نسخه مستقل است، دستور `cd` را با همان مسیر جایگزین کنید. برای دیدن خلاصه نتایج، `findstr /C:"BUILD SUCCESS" /C:"BUILD FAILURE" /C:"Tests run:" "%TEMP%\PDL-BUAT-RC1-build.log"` را **فقط یک بار** اجرا کنید.

## سیاست کنترل تغییر

- بدون Feature جدید؛ فقط Defect قابل بازتولید، مرتبط با قرارداد فعلی، همراه آزمون مثبت/منفی.
- قواعد مصوب نرخ: `SINGLE_APPLICABLE_RATE` روی کل مبلغ، `[Min,Max)`، و `NULL` مبلغ یعنی نامحدود همان سمت؛ مدت مبهم Fail-Closed.
- FEE/FEE2 و Posting مستقل، بدون مداخله در این بسته.
- نه تغییری در Oracle و نه تأیید Runtime در این انتشار انجام نشده است.

مبنای فنی: PdlCatalog.java؛ ProductRuleGovernanceService.java؛ ProductRuleHierarchyAudit.java؛ ProductPricingSingleRatePolicy.java؛ product-workspace.component.ts؛ دفتر ۵۴ جدول و دفتر ریسک ۶۶۱ فیلد.

## یادداشت سازگاری 0914

- **Patch قدیمی HF9-HF1 را اعمال نکنید.** این ZIP اصلاح Fixture را دارد و کنترل جدید `HF9-COVERAGE-GUARD` و آزمون ثبت FK lookup را نیز اضافه کرده است؛ Patch قدیمی فایل آزمون قوی‌تر را بازنویسی می‌کند.
- فقط مستندات `docs/PDL-BUSINESS-UAT-RC1/` به این مبنا افزوده شده‌اند. فایل‌های Java، Angular، Oracle، FEE/FEE2 و Posting با نسخه `0914` از نظر بایت یکسان باقی می‌مانند.
- ۱۵ Verifier مستقل در همان نسخه 0914 موفق شدند و یک Verifier تاریخی `verify-pdl-r30-hf7a3-integrity` با انتظار منسوخ شکست خورد؛ این ابزار در مسیر Build جاری فراخوانی نمی‌شود.
- Maven/Surefire کامل و Oracle زنده هنوز تأیید نشده‌اند؛ قبل از تست کاربر نهایی در محیط بانک Build کامل ویندوز را اجرا کنید.

فایل `PDL-BUSINESS-UAT-RC1-0914-COMPATIBILITY-FA.md` شامل شواهد تطبیق و راهنمای نصب ایمن است.
