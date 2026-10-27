# FEE3-BE-R4B2 — راهنمای نصب ایمن شبیه‌سازی ضمانت‌نامه

**تاریخ:** 2026-10-27 · **پیش‌نیاز:** پروژه 0.11.0 با BE-R3، R4-A و R4-B1. آخرین Runtime کاربر: `FEE3_R4B1_RUNTIME_PASS=23 FAIL=0`.

## دامنه و مرزها

این Patch فقط **Backend** و ابزار Qualification را اضافه می‌کند و هیچ فایل قبلی را جایگزین نمی‌کند. هیچ `DDL`، `DML`، تغییر Seed، تغییر Angular، فعال‌سازی Maker/Checker، وصول کارمزد یا ثبت حسابداری ندارد. مسیر `POST /api/v1/fee3/r4/guarantees/preview` با `@Transactional(readOnly=true)` فقط شبیه‌سازی پیش‌نویسِ غیرمنتشرشده است. وجود شناسه نسخه Oracle نیز مجوز انتشار یا وصول ایجاد نمی‌کند.

فقط `GUARANTEE / GUARANTEE_ISSUE / GUARANTEE_FEE` پذیرفته می‌شود؛ عملیات تمدید، کاهش، ابطال، اصلاح، مطالبه، تسویه و سایر انواع هزینه عمداً مسدود هستند. ارز ورودی و خروجی `IRR` و مبنای مبلغ، صریح است.

## محاسبات تأییدشده در این زیرمجموعه

- **سبد وثایق:** درصد سهم هر وثیقه از `inputs.portfolio` می‌آید، باید مثبت و جمع دقیقاً **۱۰۰٪** با دقت حداکثر چهار رقم اعشار باشد. نرخ و درصد پوشش هر وثیقه در `version.uiContext.guaranteePolicy.portfolioRules` تعریف می‌شود؛ درخواست مجاز به تغییر نرخ و پوشش تعرفه نیست. قواعد `CONFIG`، `REQUEST` و `EXEMPT` باید صریح باشند. `EXEMPT` در سطح ردیف فقط زمانی خروجی صفر دارد که تعرفه همان ردیف آن را تعیین کرده باشد. `CASH_100` پوشش کمتر از ۱۰۰٪ را رد می‌کند.
- **دو مبنا:** `GUARANTEE_PORTFOLIO_ALLOCATED` = سهم تخصیص‌یافته از اصل مبلغ؛ `GUARANTEE_PORTFOLIO_COLLATERAL_VALUE` = سهم × ضریب پوشش. برای همه ردیف‌ها، کارمزد = مبنای ردیف × نرخ ردیف / ۱۰۰ × ضریب دوره. جمع ردیف‌ها قبل از **یک‌بار** کف/سقف، گردکردن و مالیات صریح محاسبه می‌شود.
- **تک‌وثیقه:** `GUARANTEE_AMOUNT` یا `GUARANTEE_COLLATERAL_COVERED_VALUE`؛ نرخ سطری `RULE` یا `REQUEST` باید صریح و معتبر باشد. مقدار `collateralCoveragePercent` برای مبنای پوشش/وثیقه نقدی اجباری است؛ نرخ خودکار استنتاج نمی‌شود.
- **روش `PERCENT`:** نرخ درصدی بدون روزشمار. `periodStart`, `periodEndExclusive` و `yearFraction` در این روش ممنوع‌اند.
- **روش `DAILY`:** نرخ سالانه با بازه `[periodStart, periodEndExclusive)`، تاریخ‌های ISO و سیاست صریح `ACT_ACT`، `ACT_365` یا `ACT_360`. `ACT_ACT` بر اساس **سال واقعی میلادی** (۳۶۵/۳۶۶ روز، تفکیک عبور از سال)، مطابق مرجع رابط ضمانت‌نامه است و به‌معنی سال جلالی نیست. شروع دوره نباید پیش از تاریخ مؤثر شبیه‌سازی باشد. کاربر مجاز به ارسال `yearFraction` محاسبه‌شده نیست.
- `BigDecimal`، دقت مبلغ حداکثر دو رقم، نرخ حداکثر هشت رقم، پوشش حداکثر چهار رقم؛ سقف امن مبلغ `10^18` ریال، تا ۵۰ ردیف. گردکردن بر اساس `version.rounding`. مالیات فقط با `taxMode=TAXABLE` و `financialPolicy.taxPercent` صریح؛ هیچ نرخ قانونی فرض نمی‌شود.
- فقط یک قاعده اصلی با `order=1` مجاز است. در **سبد درصدی** `rule.rate` باید `null` باشد و نرخ‌ها فقط در ردیف‌های وثیقه قرار گیرند. کنترل عمومی R3 که در `PERCENT` نرخ سراسری می‌خواهد، صرفاً برای همین سناریوی صریح و پس از اعتبارسنجی تخصصی استثنا شده؛ تمام خطاهای دیگر R3 و قواعد Oracle همچنان سد هستند. هیچ تغییر در R3 اعمال نشده است.
- منشأ نرخ، معافیت، ضرایب و مالیات در نمونه‌های تست، **صرفاً اعداد ساختگی برای کنترل ریاضی** هستند و ابلاغیه یا تعرفه جاری بانک نیستند.

## نصب در Windows CMD

ZIP را در ریشه `D:\Projects\core-banking-prototype-v0` استخراج کنید و سپس:

```bat
cd /d D:\Projects\core-banking-prototype-v0
python tools\fee3-backend\releases\r4b2\install.py --preview
python tools\fee3-backend\releases\r4b2\install.py --apply
python tools\fee3-backend\r4b2\verify-r4b2.py
python tools\fee3-backend\r4b2\verify-java.py
python tools\fee3-backend\r4b2\run-pure-java.py
python tools\fee3-backend\r4b2\selftest.py
```

در صورت `CONFLICT`، **هیچ فایل موجودی را بازنویسی نکنید**؛ گزارش را ارسال کنید. نصب مجدد صحیح باید `APPLIED=0` بدهد.

سپس:

```bat
bin\stop.cmd
build-production.cmd
bin\start.cmd
curl.exe --noproxy "*" http://127.0.0.1:8091/api/v1/fee3/readiness
```

**فقط پس از مشاهده پاسخ معتبر readiness با `draftWritesEnabled:false` و روشن‌ماندن Backend:**

```bat
call tools\fee3-backend\r4b2\run-runtime-readonly.cmd
```

گزارش: `docs\install\FEE3-R4B2-RUNTIME-RESULT.json`؛ نتیجه هدف در وضعیت داده‌های فعلی: `FEE3_R4B2_RUNTIME_PASS=26 FAIL=0`. این عدد قبل از اجرای واقعی روی Oracle شما صرفاً نتیجه مورد انتظار است.

در صورت خطای `WinError 10061`، ابتدا `netstat -ano | findstr ":8091"` و `curl.exe --noproxy "*" .../readiness` را بررسی کنید؛ این خطا در گذشته به‌دلیل در دسترس نبودن سرویس رخ داده و لزوماً نقص موتور نیست.

## نمونه JSON

برای درخواست کاملِ محاسبه فقط‌خواندنی و سناریوهای عددی به `docs/install/FEE3-BE-R4B2-GOLDEN-CASES.json` و سازنده درخواست واقعی در `tools/fee3-backend/r4b2/runtime-readonly.py` مراجعه کنید. قرارداد دقیق `DraftRequest` با R4-A/R4-B1 یکسان است و نرخ‌ها داخل `uiContext.guaranteePolicy` تعریف می‌شوند.

## دروازه پذیرش

- کنترل منبع، کامپایل Java، آزمون عددی و Selftest همگی بدون خطا باشند.
- Build واقعی Maven و JUnit نیز روی سیستم کاربر موفق شوند.
- Runtime به Oracle واقعی متصل شود و ۲۶ کنترل HTTP/مالی/داده را بدون خطا بگذراند.
- SQL خواندنی خط مبنا، عدم تغییر تمامی جداول کسب‌وکاری را تأیید کند؛ صرف ثابت‌ماندن ۲ شمارش API کافی نیست.
- بدون این موارد R4-B2 فقط **نامزد پذیرش** است؛ انتشار، وصول و Posting مجاز نیست.
