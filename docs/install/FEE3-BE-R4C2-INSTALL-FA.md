# نصب ایمن FEE3-R4-C2 — مدل‌های مستقل تسهیلات (فقط Preview)

**نسخه:** 2026-10-27 · **پیش‌نیاز:** Core Banking Prototype 0.11.0 + R3 + R4-A + R4-B1 + R4-B2 + R4-C1 پذیرفته‌شده در Runtime.

## ۱) محدوده و تضمین‌های محدود

R4-C2 به‌صورت افزایشی ۵ مدل سود مشارکتی، خرید دین و سلف را در مسیر جدید `POST /api/v1/fee3/r4/loans/models/preview` پیاده می‌کند. این مسیر **هیچ INSERT/UPDATE/DELETE/MERGE، DDL، انتشار نسخه، شناسایی سود یا ثبت حسابداری ندارد**. فقط `@Transactional(readOnly=true)`، Oracle GET/SELECT مرجع و محاسبه BigDecimal در حافظه اجرا می‌شود. نصب Patch هیچ ساختار/داده Oracle، Angular، FEE، FEE2، Product Builder یا APIهای قبلی را تغییر نمی‌دهد.

نتیجه حتی در حالت `SIMULATED` دارای `approved=false`, `published=false`, `persisted=false`, `financialPosting=false`, `simulationMode=UNVERIFIED_UAT_DRAFT` است. `contractReference`، `rateEvidenceReference` و سایر شناسه‌های شواهد از **کلاینت و بدون احراز اصالت قرارداد** دریافت می‌شوند، پس `CLIENT_CONTRACT_EVIDENCE_UNVERIFIED` روی خروجی می‌ماند. این مبلغ قابل وصول، ثبت سود قطعی یا تعیین مالیات نیست.

## ۲) جدول قرارداد محاسبات

| `loanModelProfile` | `loanCalculationEvent` | `loanPolicy.dayCountBasis` | کد فرمول whitelist | متغیرهای عددی | ماهیت خروجی |
|---|---|---|---|---|---|
| `PARTICIPATION_REALIZED` | `REALIZED_PROFIT` | `CONTRACT_MONTHS` | `PARTICIPATION_REALIZED` | `realizedProfit`, `sharePercent` | سهم از سود *گزارش‌شده*؛ شناسایی حسابداری نیست |
| `PARTICIPATION_ESTIMATE` | `ESTIMATED_PROFIT` | `JALALI_ACT_ACT` | `TPL_E624746F262F6E` | `amount`, `contractRate`, دوره ISO | صرفاً برآورد مشارکتی |
| `DEBT_PURCHASE_SPREAD` | `PURCHASE_SPREAD` | `CONTRACT_MONTHS` | `DEBT_PURCHASE_SPREAD` | `faceAmount`, `purchasePrice` | اختلاف ناخالص قیمت؛ سود قطعی نیست |
| `SALAM_SPREAD` | `SALAM_SETTLEMENT` | `CONTRACT_MONTHS` | `SALAM_PRICE_SPREAD` | `saleAmount`, `purchaseAmount` | اختلاف ناخالص تحقق‌یافته؛ سود قطعی نیست |
| `SALAM_ESTIMATE` | `ESTIMATED_PROFIT` | `JALALI_ACT_ACT` | `TPL_E624746F262F6E` | `amount`, `contractRate`, دوره ISO | صرفاً برآورد سلف |

`CONTRACT_MONTHS` در سه مدل غیرروزشمار صرفاً مشخصه قرارداد Seed است و موتور **آن را به ضریب زمانی یا ماه ۳۰ روزه تبدیل نمی‌کند**. در دو مدل برآورد، `yearFraction` از ورودی مشتری پذیرفته نمی‌شود؛ سال جلالی و مخرج ۳۶۵/۳۶۶ از مخزن فقط‌خواندنی `CAL2` متعلق به R4-C1 به دست می‌آیند.

سه گروه رویدادی متفاوت‌اند؛ ارسال کد ناهماهنگ مدل/عقد/فرمول، نرخ ثابت تعرفه، taxMode غیر از `OUT_OF_SCOPE`، فقدان شناسه شواهد، سهم بیش از ۱۰۰٪، نرخ با دقت بیش از ۸ رقم یا مبلغ با بیش از ۲ رقم اعشار => `BLOCKED`. اختلاف منفی خرید دین/سلف به‌جای تبدیل به صفر با `NEGATIVE_SPREAD_NEEDS_LOSS_POLICY` مسدود می‌شود. تاریخ رویداد سود تحقق‌یافته یا تسویه برای Preview باید دقیقاً تاریخ کاری فعلی Oracle باشد؛ بازپردازش تاریخی در محدوده نیست.

## ۳) نصب (Windows CMD)

فایل `FEE3-R4C2-PATCH.zip` را در ریشه `D:\Projects\core-banking-prototype-v0` قرار دهید. نام کوتاه برای پرهیز از مشکل دانلود قبلی انتخاب شده است.

```bat
cd /d D:\Projects\core-banking-prototype-v0
tar -xf FEE3-R4C2-PATCH.zip
dir tools\fee3-backend\releases\r4c2\install.py
python tools\fee3-backend\releases\r4c2\install.py --preview
```

فقط اگر همه `ANCHOR` برابر `MATCH` و فایل‌های افزوده `READY` یا `ALREADY_APPLIED` بودند و `FEE3_R4C2_PREVIEW_PASS_NO_CHANGES` ظاهر شد:

```bat
python tools\fee3-backend\releases\r4c2\install.py --apply
python tools\fee3-backend\r4c2\verify-r4c2.py
python tools\fee3-backend\r4c2\verify-java.py
python tools\fee3-backend\r4c2\run-pure-java.py
python tools\fee3-backend\r4c2\selftest.py
```

اگر `FEE3_R4C2_CONFLICT_ABORTED_NO_CHANGES` یا خطای دیگر دیدید، **متوقف شوید** و لاگ را ارسال کنید؛ فایل‌های پایه را دستی جایگزین نکنید. نصب افزایشی فقط فایل‌های جدید R4C2 را اضافه می‌کند و اجرای مجدد باید بدون تغییر باشد.

## ۴) Build و پذیرش اجرایی Oracle

پس از موفقیت کنترل‌ها:

```bat
bin\stop.cmd
build-production.cmd
bin\start.cmd
```

پس از راه‌اندازی سرویس، در CMD دیگری:

```bat
cd /d D:\Projects\core-banking-prototype-v0
curl.exe --noproxy "*" http://127.0.0.1:8091/api/v1/fee3/readiness
```

تنها در صورت `readsEnabled:true`، `draftWritesEnabled:false`، `missingTables:[]`، وجود ۲۹۷ قلم/۳۰۰ نسخه مطابق مبنای ۲۷ اکتبر، و غیرفعال‌بودن تمام کلیدهای Workflow:

```bat
call tools\fee3-backend\r4c2\run-runtime-readonly.cmd
```

مورد انتظار **صرفاً در صورت موفقیت اجرای واقعی روی محیط شما**:

```
FEE3_R4C2_RUNTIME_PASS=38 FAIL=0
REPORT=D:\Projects\core-banking-prototype-v0\docs\install\FEE3-R4C2-RUNTIME-RESULT.json
```

آزمون Runtime تنها ۴ مسیر `GET` و `POST` غیرماندگار `/r4/loans/models/preview` را فراخوانی می‌کند؛ proxy/redirect و مقصد غیرمحلی مسدودند. دو سناریوی برآورد نیز به پوشش واقعی و پیوسته CAL2 وابسته‌اند. فقدان پوشش CAL2 به‌درستی باعث **عدم پذیرش** می‌شود.

## ۵) محدودیت‌های عمدی

* `FORMULA_CODE` فعال در مرجع Oracle، به‌خودی‌خود مجوز اجرای فرمول یا محاسبه سود بانکی نیست.
* نسخه‌های قدیمی Seed هنوز `DRAFT`، غیرفعال و با `taxMode=UNREVIEWED` هستند. مسیر نمونه مثبت، **پیش‌نویس غیرماندگار با مالیات خارج از محدوده** است. ما Seed را به‌صورت خودکار به تعرفه معتبر تبدیل نمی‌کنیم.
* R4-C1 و R4-C2 برای نرخ‌های قراردادی هیچ اتصال اثبات‌شده به دامین تسهیلات ندارند؛ نرخ و سود تحقق‌یافته و شواهد از درخواست مشتری/آزمون می‌آیند.
* سهم واقعی مشارکتی، نحوه تخصیص زیان، تسویه چندمرحله‌ای، اصلاح قرارداد، مالیات و ثبت حسابداری خارج از دامنه است.
* این Patch هیچ اسکریپت SQL اجرایی/تغییردهنده ندارد. مغایرت سیاست تعطیلات R4-C0 حل‌نشده باقی می‌ماند؛ خسارت تأخیر اینجا اجرا نمی‌شود.

برای شواهد تفصیلی به `FEE3-BE-R4C2-QA-FA.md` و `FEE3-R4C2-GOLDEN-CASES.json` رجوع کنید.
