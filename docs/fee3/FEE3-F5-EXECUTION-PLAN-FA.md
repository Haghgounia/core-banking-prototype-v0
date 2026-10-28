# FEE3 — طرح اجرای F5: انطباق و شواهد محاسبه

**تاریخ:** ۲۰۲۶-۱۰-۲۸ · **وضعیت:** `PLAN_READY / LIVE_POSITIVE_NOT_YET_QUALIFIED`  
**قاعده:** بدون تغییر داده و بدون فعال‌سازی مالی. بستهٔ نصب این سند صرفاً Markdown/CSV است.

## ۱. نقطه شروع قابل استناد

- Root: Source Apply/Verify موفق ۲۲/۲۲؛ تست Backend ۱۳۳/۱۳۳ و Build Frontend موفق.
- Runtime جداگانه: `http://127.0.0.1:8092`, `sessionUser=SYSTEM`, `jdbcSqlGate=SELECT_ONLY`; GET هویت، readiness، catalog موفق؛ HTTP Smoke کامل با Exit=0.
- عدم قطعیت: Runtime `8092` با Root نصب‌شده یکی نیست؛ صحت محاسبه معتبر F1/F2 و Browser E2E هنوز اثبات نشده.
- Baseline Oracle Read-only: ۱۱ جدول؛ Calc Item=297، Tariff Version=300، Calc Rule=313، Test Case=6. تمامی نسخه‌های مرجع HTML در DRAFT هستند، وضعیت فعال‌سازی مالی باید بسته بماند.

## ۲. ابزارهای تحلیلی

- `FEE3-F5-V13884-300-VERSION-TRACEABILITY.csv`: همه ۳۰۰ نسخه اولیه HTML، همراه با کد، نسخه، دامنه، عملیات، روش، منشأ و طبقه اولیه شواهد. این CSV **داده Oracle نیست**.
- `FEE3-F5-SCENARIO-GATES-20261028.csv`: ۳۸ سناریو برای شروع. فقط سناریوهای GET/منفی که قبلاً شواهد Windows دارند `RECORDED` ثبت شده‌اند.
- انطباق سطر به سطر باید با Oracle مجدداً فقط‌خواندنی اجرا شود، نه صرفاً تطبیق ۲۹۷/۳۰۰.

## ۳. ترتیب اجرای F5

1. **Snapshot/Parity (فقط SELECT):** شناسه، شماره نسخه، نوع مبلغ، روش محاسبه، عملیات، تاریخ، فعال‌سازی و fingerprint Rule را از Oracle بخوانید؛ برای هر `fee_code + tariff_version` با مرجع HTML مقایسه کنید. تفاوت، حذف/اضافه یا بازه معتبر نامعلوم را در CSV ثبت کنید. هیچ تبدیل خودکار Seed به نرخ بانکی انجام ندهید.
2. **Negative Gate:** خواندن GET موفق، رد مسیرهای غیرمجاز، رد ورودی نامعتبر و ثابت‌ماندن Baseline را روی Runtime ایزوله با همان اسکریپت Smoke حفظ کنید؛ این Gate طبق خروجی ۲۸ اکتبر موفق است.
3. **Positive F1/F2:** فقط با ورودی کامل و مستند (نسخه، Rule، currency/rounding، تاریخ مؤثر، قرارداد/هزینه/مبنای قانونی)، `POST` Preview فقط‌خواندنی را اجرا کنید. مسیرهای مرتبط: `/api/v1/fee3/r4/services/usage/preview` و `/api/v1/fee3/r4/services/formula-allowlist/preview`. موفقیت واقعی باید همراه `expectedAmount`, `actualAmount` و `trace` باشد؛ `BLOCKED` ناشی از داده مفقود یک نتیجه محافظتی است، نه PASS عددی.
4. **رگرسیون روش‌ها:** مسیر عمومی `/api/v1/fee3/preview` و سرویس‌های R4 را برای FIXED، PERCENT، MIXED، DAILY و محاسبات تخصصی ارزیابی کنید. وجود Contract در کد به معنای پوشش همه ۳۰۰ نسخه نیست.
5. **بازپخش تاریخی/آتی:** تاریخ اعتبار و نسخه صحیح را کنترل کنید. F2 فعلی، پیش‌نمایش تاریخی را به‌صورت عمومی اجازه نمی‌دهد؛ نباید با جابه‌جایی ساعت/تاریخ یا تغییر دیتابیس این مانع دور زده شود. اختلاف نیازمند تصمیم طراحی و تست مستقل است.
6. **Browser E2E:** Angular به API صحیح در محیط ایزوله متصل شود؛ وضعیت DRAFT/UNREVIEWED، درخواست ردشده و Trace قابل فهم برای کاربر نمایش داده شود. ترجیحاً از Proxy development با allowlist مسیر استفاده شود.
7. **گزارش کارشناس:** برای هر سناریو سه خروجی: نتیجه فنی، وضعیت داده/قرارداد، و نیاز به تصمیم بانکی. بدون این تفکیک UAT قابل امضا نیست.

## ۴. حداقل قالب Evidence

| ستون | الزام |
|---|---|
| `scenario_id`, `fee_code`, `version`, `operation`, `method` | هویت دقیق محاسبه |
| `oracle_item_id`, `oracle_tariff_version_id`, `rule_id` | شناسه‌های واقعی و قابل بازتولید در بانک |
| `effective_at`, `bank_business_date`, `timezone` | نسخه مؤثر با منطقه زمانی بانک |
| `request_sha256`, `rule_snapshot_sha256` | جلوگیری از Drift و اختلاط پرونده‌ها |
| `amount_expected`, `amount_actual`, `currency`, `rounding` | مقایسه عددی دقیق، بدون Float نامطمئن |
| `http_status`, `diagnostics`, `block_reason` | جداکردن خطای فنی از Gate محافظتی |
| `baseline_before`, `baseline_after` | تأیید نبود اثر DB در Preview |
| `source_reference`, `policy_owner`, `review_outcome` | منشأ مصوبه و تصمیم کارشناسی |

وضعیت‌های مجاز: `PASS_NUMERIC`, `PASS_NEGATIVE_GUARD`, `FAIL_IMPLEMENTATION`, `BLOCKED_MISSING_CONTRACT`, `BLOCKED_EXPIRED_OR_DRAFT`, `PENDING_POLICY`, `NOT_RUN`. نتیجه `PASS_NUMERIC` تنها با داده عددی مستند ثبت شود.

## ۵. محدودیت و مسیر بعد

این Patch فقط مستندسازی است، نه ابزار اجرای HTTP و نه تغییردهنده Oracle. بعد از نصب مستندات، فاز بعد ساخت/اجرای **بسته Oracle/HTTP فقط‌خواندنی F5** خواهد بود که بتواند `Expected vs Actual` را با شواهد واقعی در Runtime مجاز ثبت کند. برای اقلام فاقد نرخ/قرارداد، نتیجه مسدود قابل قبول است؛ ساخت داده فرضی برای عبور آزمون ممنوع است.
