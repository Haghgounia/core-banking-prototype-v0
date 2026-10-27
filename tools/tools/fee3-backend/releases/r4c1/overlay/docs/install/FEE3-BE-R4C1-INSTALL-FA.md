# FEE3-BE-R4C1 — فقط شبیه‌سازی سود دوره‌ای عقود مبادله‌ای

تاریخ: 2026-10-27 · مبنای تحلیل: خروجی واقعی R4C0 بانک در تاریخ 2026-10-27.

## اصل حاکمیتی

R4-C1 مجوز اخذ سود، محاسبه سود قطعی قرارداد، تأیید نرخ، تعیین مالیات، فعال‌سازی تعرفه یا ثبت حسابداری نیست. نتیجه همیشه `SIMULATED` یا `BLOCKED`، و `approved=false, published=false, persisted=false, financialPosting=false` است. حتی در حالت SIMULATED، `contractReference` و `rateEvidenceReference` از **درخواست کاربر** می‌آیند، نه از دامین تسهیلات؛ لذا خروجی صریحاً `CLIENT_CONTRACT_RATE_UNVERIFIED` دارد. از این API برای بدهکار/بستانکار کردن مشتری استفاده نکنید.

## شواهد R4C0 و محدودیت‌ها

Oracle: ۱۱ قلم سود، ۲۷ خسارت، ۴ قرض‌الحسنه، هر ۴۲ قلم غیرفعال. صفر نسخه فعال یا منتشرشده. برای سود یک Test Case تعریف و صفر Test Case موفق ثبت شده، برای خسارت یک/صفر، برای قرض‌الحسنه صفر/صفر.

مغایرت کد مرجع: ۹ نسخه خسارت با `EXCLUDE_ALL_HOLIDAYS` ولی Vocabulary فعال فقط `EXCLUDE_NON_BUSINESS_DAYS`؛ این دو خودکار یکی نیستند. ۱۸ نسخه تقویمی خسارت به `QA_CALENDAR_SIMULATION_NOT_APPROVED` وابسته‌اند. R4-C1 هیچ‌کدام از این سیاست‌های خسارت را اجرا نمی‌کند؛ اصلاح داده Oracle فقط پس از تصمیم کسب‌وکار.

## فقط دو شاخه محاسباتی، بدون نرخ پیش‌فرض

- `EXCHANGE_ACCRUAL / PERIOD_PROFIT / JALALI_ACT_ACT`: فرمول ثابت و *فقط* whitelist `TPL_E624746F262F6E`، `amount * contractRate / 100 * factor`. سال جلالی و ۳۶۵/۳۶۶ از روزهای واقعی موجود در `CAL2.CALENDAR_DATE` پیوسته با `CANONICAL_DAY` و Variant فعال Persian به‌دست می‌آید. ناتمام‌بودن پوشش، تکرار روز، شکاف روز، تقویم ناسازگار، دیتاست ناقص یا نداشتن SELECT لازم => `BLOCKED`. `yearFraction` هرگز از کلاینت پذیرفته نمی‌شود.
- `EXCHANGE_ACCRUAL / PERIOD_PROFIT / CONTRACT_MONTHS`: فرمول ثابت و *فقط* whitelist `TPL_7F2FE580EDB351`، `amount * contractRate / 100 * contractMonths/12`. ماه‌های قراردادی عدد صحیح از قرارداد ادعایی درخواست است؛ بدون اثبات معتبر منبع خارجی همچنان فقط **شبیه‌سازی** است. روش ACT/ACT میلادی ضمانت‌نامه با سال جلالی ادغام نمی‌شود.

در هر دو روش `BigDecimal`، مبلغ `NUMBER(22,2)`، نرخ `NUMBER(18,8)`، گردکردن تصریح‌شده، فقط IRR، بدون معافیت و با `taxMode=OUT_OF_SCOPE` (صرفاً تعیین‌نشدن مالیات در این مرحله) است. مدل‌های مشارکتی، خسارت، وجه التزام، قرض‌الحسنه، نرخ ذخیره‌شده در تعرفه، فرمول آزاد و فیلدهای محاسبه‌شده توسط کاربر مسدودند.

**نکته درباره Seed موجود:** نسخه‌های ذخیره‌شده سود هنوز `taxMode=UNREVIEWED` و به نسخه‌بندی اولیه وابسته‌اند. R4-C1 به‌صورت خودکار آن‌ها را به نرخ/تعرفه معتبر تبدیل نمی‌کند؛ مسیر آزمایش مثبت از Draft غیرماندگار و صریحاً **تأییدنشده** استفاده می‌کند. برای استفاده از نسخه‌های ذخیره‌شده باید حاکمیت قرارداد، تاریخ اعتبار، مالیات و شواهد تست بررسی شود.

## نصب ایمن

فایل ZIP را در ریشه `D:\Projects\core-banking-prototype-v0` استخراج کنید. هیچ Java، SQL، Angular یا تنظیمات موجود جایگزین نمی‌شود. فقط مسیرهای جدید R4-C1 اضافه می‌شوند. Installer قبل از هر نوشتن، SHA-256 فایل‌های موجود و همه فایل‌های Overlay را بررسی می‌کند؛ در تعارض توقف امن دارد.

```bat
cd /d D:\Projects\core-banking-prototype-v0
python tools\fee3-backend\releases\r4c1\install.py --preview
python tools\fee3-backend\releases\r4c1\install.py --apply
python tools\fee3-backend\r4c1\verify-r4c1.py
python tools\fee3-backend\r4c1\verify-java.py
python tools\fee3-backend\r4c1\run-pure-java.py
python tools\fee3-backend\r4c1\selftest.py
```

سپس برای اعمال Java جدید:

```bat
bin\stop.cmd
build-production.cmd
bin\start.cmd
curl.exe --noproxy "*" http://127.0.0.1:8091/api/v1/fee3/readiness
```

فقط وقتی `draftWritesEnabled:false` و Backend فعال است:

```bat
call tools\fee3-backend\r4c1\run-runtime-readonly.cmd
```

Runtime روی `GET /readiness`، `/workflow/readiness`، `/references/operations`، `/references/formulas` و فقط `POST /r4/loans/profit/preview` عمل می‌کند. هیچ POST دیگری ندارد؛ Proxy و Redirect به بیرون ممنوع. گزارش `docs\install\FEE3-R4C1-RUNTIME-RESULT.json` است.

## بررسی CAL2 و مغایرت کد

`docs\install\FEE3-R4C1-ORACLE-READONLY-CAL2-CHECK.sql` را در SQL Developer با F5 اجرا کنید (فقط SELECT). اگر حساب شما دسترسی CAL2 نداشت یا ۱۰ روز آینده پوشش پیوسته نداشت، آن را بدون تغییر Oracle گزارش کنید؛ R4-C1 مسیر Jalali را طبق طراحی `BLOCKED` می‌کند. اجرای SQL به معنی تأیید رسمی تقویم نیست.

## محدوده پذیرش

آزمون Java و Mock به تنهایی معادل Build Maven یا پذیرش Oracle نیست. دروازه بعدی: Build+JUnit واقعی، Runtime ماه قراردادی، Runtime JALALI با CAL2 واقعی، بازشماری ۱۱ جدول، سپس بررسی کارشناس کسب‌وکار. هیچ نرخ ۱۸٪ یا قاعده روزشمار نمونه، «نرخ مصوب» نیست.
