# CLT R3 — شواهد تجمیعی چرخه عمر ضامن، بیمه و ارزیابی (Read-only)

**تاریخ:** 2026-11-09  
**شناسه تحویل:** `CLT-R3-LIFECYCLE-EVIDENCE-2026-11-09`  
**مبنای سورس:** `core-banking-prototype-v0-2026-11-09-R3-REVIEW-REVALIDATION-SOURCE.zip`؛ نصب نسخه قبل توسط تحلیلگر موفق اعلام شد.  
**فاز ثابت:** R3 — ضامن، بیمه و ارزیابی  
**وضعیت:** SOURCE IMPLEMENTED / JAVA PURE POLICY & STATIC CONTRACT VERIFIED / WINDOWS BUILD AND LIVE ORACLE NOT VERIFIED

## هدف و انطباق با مستندات

منطبق بر BUC-06/07/08، SUC-07/09/10 و SVC-05/07/08، تحلیلگر برای هر وثیقه بتواند در **یک درخواست خواندنی** تصویر واقعی ثبت‌های ضامن/بیمه/ارزیابی را دریافت کند؛ بدون اینکه رکورد موجود را به‌جای ضامن تأییدشده، بیمه معتبر یا ارزیابی مجاز برای تخصیص بگیرد. مراجع هدف: HTML V1.42، مشخصات سرویس و مدل فیزیکی ۲۶جدولی CLT. این مرحله **هیچ‌یک از ۱۷ endpoint هدف مصوب را جایگزین نمی‌کند**؛ یک endpoint داخلی تشخیصی افزوده شده است.

## قرارداد API جدید

`GET /api/v1/collaterals/{collateralId}/r3/lifecycle-evidence`

- فقط `clt.collateral.read` و شعبه معتبر از principal داخلی/معتبر؛ شناسه وثیقه از `NUMBER(19)` با رشته ده‌دهی و بدون تبدیل شناور دریافت می‌شود.
- خواندن `COLLATERAL`, `COLLATERAL_TYPE`, `COLLATERAL_SPONSOR`, `COLLATERAL_INSURANCE`, `COLLATERAL_VALUATION_HISTORY` در یک **SELECT** از DataSource موجود پروژه؛ هیچ connection pool یا تنظیم جدیدی ایجاد نمی‌شود.
- سه شمارش شامل تمام رکوردهای ثبت‌شده هستند و **هیچ ادعایی درباره تعداد ضامنین معتبر/پوشش قابل پذیرش نمی‌کنند**. `sponsor.validity` و `insurance.validity` همواره `NOT_VERIFIED` باقی می‌مانند.
- ارزیابی جاری از `CURRENT_VALUATION_HISTORY_ID` به تاریخچه متصل می‌شود؛ فقط کد فیزیکی `FINAL`، ارز یکسان، پیوند درست و دوره `EFFECTIVE_FROM/EFFECTIVE_TO` بررسی می‌شود.
- `WITHIN_RECORDED_EFFECTIVE_PERIOD` فقط یک شواهد تاریخی است؛ **به معنای معتبر بودن دوره تجدید ارزیابی نیست**. `EFFECTIVE_TO=NULL` به‌صورت `NOT_PROVEN` برمی‌گردد و **G-18** باز می‌ماند.
- `finalRegistrationEnabled=false` و `eligibleForNewAllocation=false` به‌صورت صریح برگردانده می‌شوند. هیچ تأیید یا محاسبه جدید ظرفیت یا تعهد ایجاد نمی‌شود.
- زمان از ساعت Oracle با timezone `Asia/Tehran` گرفته می‌شود، ولی به‌جای «روز کاری رسمی بانک» معرفی نشده است. وضعیت مراجع/سیاست‌ها در پاسخ با G-04/05/06/07/09/11/18 نمایش داده می‌شود.
- عدم دسترسی بین‌شعبه‌ای بدون افشای وجود وثیقه خطای `404` خواهد داشت؛ خواندن غیرفعال `503` و Scope نامعتبر `403` برمی‌گرداند.

## خروجی نمونه ساختاری (نه نتیجه زنده Oracle)

```json
{
  "collateralId": "712",
  "asOfDate": "2026-11-09",
  "sponsor": {"applicability": "REQUIRED_BY_TYPE", "recordedRows": 10, "validity": "NOT_VERIFIED"},
  "insurance": {"applicability": "INSURABLE_TYPE", "recordedRows": 2, "validity": "NOT_VERIFIED"},
  "valuation": {"recordedRows": 3, "recordedPeriodState": "WITHIN_RECORDED_EFFECTIVE_PERIOD", "eligibleForNewAllocation": false},
  "finalRegistrationEnabled": false,
  "eligibleForNewAllocation": false,
  "unresolvedApprovalGates": ["G-04", "G-05", "G-06", "G-07", "G-09", "G-11", "G-18"]
}
```

نمونه بالا عمداً بخشی از فیلدهای پاسخ را برای اختصار حذف کرده است. هیچ داده‌ای از Oracle بانک بازیابی یا ادعا نشده است.

## گیت‌های باقی‌مانده و Definition of Done

| Gap | شواهد قابل ارائه در این مرحله | شرط بستن Gap |
|---|---|---|
| G-05/G-11 | شمار ثبت‌های ضامن؛ فلگ SPONSOR_REQUIRED | تصویب Lifecycle/حداقل ضامن و انطباق schema و قواعد تعهد |
| G-04/G-06 | شمار بیمه‌نامه‌های ثبت‌شده؛ INSURABLE | سیاست پوشش، وضعیت بیمه، ابطال/تمدید و Audit مصوب |
| G-07 | اشاره‌گر ارزیابی جاری، `FINAL` فیزیکی | قرارداد تصویب تأیید نهایی/Actor و Audit |
| G-18 | بررسی بازه Effective ثبت‌شده | تعیین Appraisability، واحد تناوب و Business Date |
| G-09 | هیچ فراخوانی جدید به Party/Insurance/FX | قرارداد Enterprise و ادغام واقعی |

**آزمون‌های این تحویل:** Smoke Java مستقل از Spring، کنترل ساختاری سورس و parsing Java؛ نصب، rollback و تکرار نصب محافظت‌شده (شواهد داخل `QA-REPORT.json`). **NOT RUN:** Build رسمی Windows، استارت Runtime، JDBC زنده Oracle و UAT تحلیلگر. این مرحله نه Migration Stage را اجرا می‌کند و نه ثبت قطعی جداول Domain را فعال می‌کند.

**گام بعدِ R3:** تصویب سیاست‌های G-04/G-05/G-06/G-07/G-11/G-18 و قرارداد ذخیره‌سازی و سپس پیاده‌سازی تراکنش‌های قطعی (در همین فاز). R4–R7 تغییر نام/مرز ندارند؛ UAT جامع به انتهای توسعه موکول است.
