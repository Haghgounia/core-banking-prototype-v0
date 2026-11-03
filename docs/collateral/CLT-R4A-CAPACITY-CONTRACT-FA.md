# CLT-R4A — قرارداد اجرایی شواهد ظرفیت وثیقه

**نسخه محصول:** 0.11.0  
**نقشه راه ثابت:** CLT-ROADMAP-R1.0، فاز R4 «ظرفیت و تخصیص بین‌دامنه‌ای»، تحویل R4A  
**مبنای استخراج:** Collateral_Service_Specification_FA.html / SVC-09، Collateral_Business_Use_Cases_FA.html / BUC-09 و BUC-14 و BR-07/08/09/10/12/13، Collateral_UI_Final_Offline_V1.42_FA.html، DDL ارسالی CLT-2026-11-1.txt و سورس یکپارچه R3C.

## محدوده پیاده‌سازی

- `GET /api/v1/collaterals/{collateralId}/capacity`: شواهد خواندنی برای وثیقه موجود در شعبه مجاز JWT، وضعیت وثیقه، ارزیابی جاری، مقادیر ثبت‌شده مرجع نوع وثیقه، تخصیص‌های موجود و محاسبه خام کسری.
- `POST /api/v1/collaterals/{collateralId}/capacity/preview`: مقایسه فرضی مبلغ پیشنهادی با اختلاف حسابی؛ **بدون هیچ ثبت، تخصیص یا تأیید بانکی**. این POST برخلاف POSTهای عملیاتی فقط خواندنی است و به `SCOPE_clt.collateral.read` نیاز دارد.
- محیط Angular در شناسنامه وثیقه، بدون ایجاد منوی مشترک یا مسیر جدید، وضعیت شواهد و درخواست فرضی را نمایش می‌دهد. نتیجه همواره `approvedForNewAllocation=false` و `verifiedAvailableAmount=null` است.

## منبع فیزیکی داده

| مفهوم | منبع Oracle | قاعده |
|---|---|---|
| شناسنامه، شعبه و وضعیت | `CLT.COLLATERAL` | شماره انتظامی با `COLLATERAL_ID` تفاوت دارد؛ محدودیت `c.BRANCH_CODE = JWT.branch_code` در SQL |
| کد و درصدهای نوع | `CLT.COLLATERAL_TYPE` | مقادیر واقعی خوانده می‌شوند؛ تفسیر فرمول رسمی و تصویب ضرایب **انجام نشده** |
| وضعیت چرخه عمر | `CLT.REF_COLLATERAL_STATUS` | صرفاً فعال بودن مرجع کافی نیست؛ وضعیت‌های مجاز جداگانه کنترل می‌شوند |
| ارزیابی جاری | `CLT.COLLATERAL_VALUATION_HISTORY` | فقط ارزیابی دارای اشاره‌گر از شناسنامه، همان وثیقه، وضعیت فیزیکی `FINAL` و بازه اعتبار قابل اثبات |
| تخصیص‌های موجود | `CLT.COLLATERAL_ALLOCATION` | فقط `USED='YES'`، گروه‌بندی بر اساس `MODULE_ID` و ارز؛ رکوردهای غیرفعال به صورت سابقه حفظ می‌شوند |
| ساعت مشاهده | Oracle `SYSTIMESTAMP AT TIME ZONE 'Asia/Tehran'` | زمان تقویمی مشاهده، **نه** تاریخ کاری مصوب بانک |

## محاسبه شواهد و محدودیت آن

وقتی `ELIGIBLE_VALUE` جاری در دیتابیس موجود باشد و **تمام تخصیص‌های فعال به همان ارز** باشند، دو مقدار زیر صرفاً برای نمایش شواهد محاسبه می‌شوند:

```
rawResidual = recordedCurrentEligibleValue - SUM(activeAllocationAmountSameCurrency)
rawShortfall = max(0, -rawResidual)
whatIfShortfall = max(0, proposedAmount - rawResidual)
```

- این اعداد **قبل از قواعد نهایی و ضریب دامین مصرف‌کننده** و **بدون معادل‌سازی مبلغ هر تخصیص با ارزش پایه** هستند؛ پس حتی `rawResidual > 0` مجوز مصرف نیست.
- اگر یک تخصیص فعال با ارز دیگر وجود داشته باشد، `rawResidual`, `rawShortfall` و `whatIfShortfall` همگی `null` خواهند بود؛ مجموع ارزهای مختلف اشتباه تجمیع نمی‌شود.
- وجود کسری حتی پس از تجدید ارزیابی باعث حذف تخصیص‌های قبلی نمی‌شود؛ فقط گزارش و به‌صورت پیش‌فرض توقف تصمیم‌های جدید است.
- اگر `ELIGIBLE_VALUE` ذخیره نشده، از `ESTIMATE_AMOUNT`، `MORTGAGE_AMOUNT`، `RESERVE_RATIO` یا فرمول نمونه HTML، ارزش قابل پذیرش اختراع نمی‌شود.
- مانع انقضای ارزیابی بر اساس `EFFECTIVE_FROM/TO` ثبت‌شده بررسی می‌شود. دوره تجدید ارزیابی در G-18 بدون واحد رسمی است و از روی `REVALUATION_INTERVAL_VALUE` سررسید قطعی استخراج نمی‌شود.
- `MODULE_ID` یک شناسه فنی است؛ نگاشت دامنه/پرونده، درصد مصرف هر دامنه، مبلغ معادل پایه، نوع تعهد و نرخ ارز باید در قرارداد بین‌دامنه‌ای رسمی تأیید شوند.
- برای محاسبه عملیاتی آینده، تراکنش واحد، قفل رقابتی، Idempotency، مجوز سرویس درخواست‌کننده، تطبیق بدهکار/ضامن، کفایت بیمه و سیاست بین‌شعبه‌ای لازم است.

## امنیت، قرارداد پیام و کنترل خروج

- هر دو Endpoint به JWT واقعی با `SCOPE_clt.collateral.read` و Claim معتبر `branch_code` نیاز دارند. هیچ Branch ID از Query یا Body پذیرفته نمی‌شود. 404 مشترک برای وثیقه ناموجود یا خارج از شعبه.
- فعال شدن Reader CLT تابع Gateهای R1/R2 است؛ به‌صورت پیش‌فرض خاموش. هیچ حساب جدید DB، هیچ DDL/DML/COMMIT و هیچ Authorization برای POST تخصیص فعال نمی‌شود.
- `String` برای ID تا ۱۹ رقم و مبالغ تا چهار رقم اعشار، و `BigDecimal` در Backend؛ در Frontend هیچ تبدیل مالی با `Number()` انجام نمی‌شود.
- جمع تخصیص‌ها در دو Query مجزا از Parent گرفته می‌شود. چون Snapshot قفل‌کننده/serializable نداریم، نتیجه فقط «شواهد مشاهده» است؛ نه Transactional Admission Check.
- ورودی سناریو: `{ "consumerDomain": "LOAN", "requestedAmount": "2000000", "currencyCode": "IRR" }` و نتیجه شامل `hypotheticalShortfall`, `approvedForNewAllocation:false`, `verifiedAvailableAmount:null`, `blockers`.

## مثال آموزشی (نه سیاست بانک)

ارزش ثبت‌شده ۱۰۰٬۰۰۰٬۰۰۰ و تخصیص فعال ۶۰٬۰۰۰٬۰۰۰ ریال → اختلاف حسابی ۴۰٬۰۰۰٬۰۰۰ و کسری صفر. پس از کاهش ارزش ثبت‌شده به ۵۰٬۰۰۰٬۰۰۰ → اختلاف حسابی ۱۰٬۰۰۰٬۰۰۰- و کسری ۱۰٬۰۰۰٬۰۰۰؛ تخصیص قبلی باقی می‌ماند. در هر دو حالت نتیجه `eligibleForNewAllocation=false` است.

## گیت‌های باز

- G-02 قرارداد مستقل تخصیص/Idempotency و مالکیت دامین؛ G-04 الزامات بیمه به تفکیک نوع؛ G-05 و G-11 تأیید ضامن و حداقل تعداد؛ G-07 و G-18 ارزیابی و دوره؛ G-09 قرارداد پرونده، بدهکاران و FX؛ G-10 ضرایب مصوب؛ G-13 مجوز در سطح معرفی؛ و کنترل استفاده انحصاری/موردی.
- R4A **به معنای خاتمه R4 یا R2/R3 نیست.** گام بعدی R4B: طراحی قرارداد درخواست تخصیص و Authorization/Idempotency به‌صورت مستقل، بدون عملیات مالی تا مصوبه.
