# گزارش R30-HF7-A2 — اصلاح جامع محافظ‌های معنایی و زمان اعتبار

**Source baseline:** `core-banking-prototype-v0-2026-10-13-1713.zip` به‌اضافه اصلاحات تجمیعی A1. **طبقه‌بندی:** Development/UAT only؛ این نسخه مجوز Production نیست.

## تغییرات کد واقعی

- `ProductRuleEvidenceEvaluator`: بررسی پوشش پیوسته تاریخ در کل بازه اعتبار نسخه، اعتبارسنجی قاعده آینده، کنترل تداخل تک‌رکوردی، پیشگیری از سرریز در `LocalDate.MAX`.
- `ProductRuleGovernanceService`: گپ زمانی کنترل اجباری و ردیف فعال با تاریخ معکوس، مانع تصویب می‌شوند؛ `canApprove` از `canOpen` مستقل است؛ دامنه غیرمجاز حتی مجوز تأیید طراحی نمی‌گیرد.
- `ProductRuleSemanticContract`: ارتقای جدول‌های Common دارای قرارداد مستقل از WRITE به Approval Audit (کانال، اهلیت، سازمان، استعلام و مدرک)؛ شمارش و روزهای صحیح و وابستگی افتتاح/مانده و تسهیلات؛ شناسه‌های NUMBER(19) بدون محدودسازی اشتباه به ۳۲ بیت.
- `ProductRuleGovernanceFixture`: نمونه‌های آزمایشی معتبر با همان قرارداد جدید؛ داده واقعی Oracle تغییر نکرده است.
- `tools/verify-pdl-r30-hf7a2-temporal.mjs`: آزمون اجرایی Java و اسکن Static برای کد **واقعی** governance و contract با Stub مخزن و Spring، بدون Oracle.
- `tools/verify-pdl-r30-hf7a2-runtime-links.mjs`: اسکن غیرنوشتنی ۲۳ کنترل در ۲۰۵ فایل Java خارج از Product Builder؛ فقط موجودبودن ارجاع مستقیم نام جدول را بررسی می‌کند، نه اتصال اجرایی.
- verifier R19 با کنترل دقیق‌تر تداخل آینده سازگار شد. هر دو اسکریپت `build-production.cmd/.sh` اکنون آزمون A2 را اجرا می‌کنند.

## آزمون و شواهد

| Suite | نتیجه مشاهده‌شده | ماهیت |
|---|---:|---|
| HF7-A1 | ۱۰ Static و ۲۷ Java | واقعی با Stub فریم‌ورک/Repository |
| HF7-A2 | ۱۱ Static و ۲۳ Java | واقعی با Stub فریم‌ورک/Repository |
| R30 | ۵۰ Static | Regression |
| HF1 / HF2 / HF3 | ۱۳ / ۳۶ / ۲۱ Static | Regression |
| HF4 / HF5 / HF6 | ۴۱ Java / ۴۴ Static / ۳۳ Static | Regression |
| HF7-SG | ۱۰ Static | محافظ انتشار |
| R28 / R29 | ۳۲ / ۴۸ Static | رگرسیون کدگذاری |
| R26 Validator | ۱۲ Java | رگرسیون اعتبارسنجی |
| R19 / R20 | ۵۱ / ۵۳ Static | Governance |
| Angular Production / Maven full build | **اجرا نشده** | وابستگی Build حاضر نیست |
| Oracle UAT / E2E چندکاناله | **اجرا نشده** | اتصال و محیط عملیاتی بانکی احراز نشده |

## موارد تکمیل‌نشده

- ماتریس از مرجع HTML دارای ۴۷ جدول و ۶۶۱ فیلد است؛ در ۱۶ جدول تنها قرارداد معنایی حداقلی تأیید نسخه داریم. برای بقیه و قواعد والد/فرزند، تصمیم و بازطراحی هنوز باز است.
- مدل ۴حالته NULL و سیاست «نامحدود مصوب» هنوز ستون و تصویب کسب‌وکاری ندارد؛ `NULL` را به معنی اجازه نامحدود تبدیل نکردیم.
- حتی وجود کد در `DepositProfitRepository` یا `DepositAccountWaveDRepository` تضمین E2E نیست؛ اتصال Rule به pre-commit، approval، disbursement و settlement اثبات نشده است.
- در Production، تا احراز AuthN/AuthZ، داده مرجع واقعی، version snapshot، trace و rollback اجازه آزاد کردن `canOpen` وجود ندارد.
- FEE/FEE2، الگوریتم کد محصول R30/HF5 و ساخت خودکار پروفایل HF6 تغییر نکرده‌اند.

## نصب

**Overlay cumulative patch** را روی ریشه همان Baseline منتشرشده ۲۰۲۶-۱۰-۱۳ اعمال کنید؛ پوشه‌های موجود را حذف نکنید. نسخه کامل نیز به‌صورت مستقل موجود است. پس از Backup، تست‌های غیرنوشتنی `node tools\verify-pdl-r30-hf7a1-semantic.mjs` و `node tools\verify-pdl-r30-hf7a2-temporal.mjs` را اجرا کنید. برای Build واقعی از `build-production.cmd` در Development/UAT استفاده کنید. هیچ DDL/DML توسط Patch لازم نیست.
