# CLT-R3B — قرارداد مدیریت بیمه‌نامه وثیقه

مرجع: `CLT-ROADMAP-R1.0` / فاز R3؛ Business `BUC-07`، System `SUC-09`، UI V1.42.

## دامنه تحویل

- نمایش صفحه‌بندی‌شده بیمه‌نامه‌های موجود از `CLT.COLLATERAL_INSURANCE` در محدوده شعبه JWT؛ با نام نوع بیمه و شرکت از دو جدول مرجع واقعی.
- درخواست گزینه‌های فعال از Oracle؛ هیچ شرکت بیمه نمونه، ضریب پوشش یا نوع الزامی ساختگی استفاده نشده است.
- اعتبارسنجی سمت سرور برای **بیمه تهیه‌شده توسط مشتری** (INSURER_CODE=2)، مجوز شعبه، اعتبار جاری مالک از CIF، INSURABLE نوع وثیقه، فعال‌بودن شرکت/نوع بیمه/ارز، شماره و مبلغ، دوره تاریخ و عدم تکرار.
- زنجیره تمدید: `PREVIOUS_INSURANCE_ID` باید متعلق به همان وثیقه و نوع باشد، قبلاً برای تمدید استفاده نشده باشد و تاریخ شروع جدید دقیقاً روز بعد از پایان قبلی باشد؛ تاریخ‌های دو سر شامل می‌شوند. گرید UI فقط صفحه فعلی را نشان می‌دهد، ولی کنترل اصلی در Backend تمام رکوردهای Oracle را بررسی می‌کند.
- انتقال عددهای پولی به‌صورت رشته اعشاری و در Backend به شکل BigDecimal؛ شناسه‌ها به‌صورت string/Oracle NUMBER(19).
- ثبت **درخواست مرحله‌ای DRAFT** در دو جدول پیشنهادی با Idempotency، کنترل ثبت موازی تمدید و Audit تراکنشی، فقط با مجوز نوشتن محدود و فعال‌سازی صریح. هیچ DML روی `CLT.COLLATERAL_INSURANCE` وجود ندارد.

## قرارداد API

| HTTP | مسیر | مجوز | عملکرد |
|---|---|---|---|
| GET | `/api/v1/collaterals/{id}/insurance-policies` | `clt.collateral.read` | فهرست بیمه‌نامه‌های ثبت‌شده؛ شعبه JWT |
| GET | `/api/v1/collaterals/{id}/insurance-policies/options` | `clt.collateral.read` | شرکت/نوع بیمه/ارز فعال |
| GET | `/api/v1/collaterals/insurance-policies/readiness` | `clt.collateral.read` | وضعیت قابلیت |
| POST | `/api/v1/collaterals/{id}/insurance-policies/validate` | `clt.collateral.introduce` | کنترل و گزارش موانع |
| POST | `/api/v1/collaterals/{id}/insurance-policies/requests` | `clt.collateral.introduce` + opt-in | درخواست مرحله‌ای؛ Idempotency-Key الزامی |
| GET | `/api/v1/collaterals/insurance-policies/requests/mine` | `clt.collateral.introduce` + opt-in | درخواست‌های ثبت‌کننده همان شعبه |

پاسخ Validation شامل `eligibleForRequest` (فقط پذیرش پیش‌نویس)، `eligibleForFinalRegistration=false` و `eligibleForAllocation=false` است.

## شکاف‌های غیرقابل حل خودکار

**G-04:** فهرست انواع بیمه *لازم برای خودِ این وثیقه* و حداقل پوشش به‌صورت مصوب در مدل CLT ذخیره نمی‌شود؛ بنابراین انتخاب از همه انواع فعال فقط برای طرح اولیه درخواست قابل‌استفاده است و کفایت بیمه/آمادگی تخصیص اثبات نمی‌شود.

**G-06:** `COLLATERAL_INSURANCE` وضعیت تأیید، لغو و جایگزینی مستقل ندارد. تمامی رکوردهای ثبت‌شده برای جلوگیری از هم‌پوشانی/تکرار بررسی می‌شوند؛ وضعیت بیمه‌ای آن‌ها «نامشخص» است. `PREVIOUS_INSURANCE_ID` به‌تنهایی چرخه لغو را حل نمی‌کند.

**G-09:** API شرکت بیمه طرف قرارداد با بانک موجود نیست؛ انتخاب `INSURER_CODE=1` برای درخواست جدید مسدود است؛ سوابق تاریخی قابل مشاهده باقی می‌مانند.

**G-10:** هیچ داده نمونه رابط آفلاین به‌عنوان فهرست بیمه‌گران مورد تأیید بانک به Oracle منتقل نمی‌شود.

## گیت‌ها و مرز ایمنی

به صورت پیش‌فرض `CLT_REFERENCE_READS_ENABLED=false`، `CLT_REGISTRY_READS_ENABLED=false`، `CLT_INSURANCE_REQUEST_WRITES_ENABLED=false`. اعتبارسنجی نیازمند Reader مجاز است؛ Writer مرحله‌ای مستقل است، حساب CLT/SYS/SYSTEM ندارد و باید صرفاً روی جداول مرحله‌ای مجوز DML دریافت کند. هیچ SQL در Installer اجرا نمی‌شود. فایل مهاجرت کاندیدا عمداً با خطای Oracle متوقف می‌شود و به‌هیچ‌وجه مصوبه استقرار نیست.

## هشدار طراحی

- پاسخ خواندنی، زمان اعتبار، لغو، یا تایید تخصیص را از نبود ستون وضعیت استنتاج نمی‌کند.
- برای Currency برابر ارز غیر IRR، مقدار وجه بدون تبدیل خودکار به ریال نگه داشته می‌شود؛ تطبیق با حداقل پوشش ریالی نیازمند قواعد FX مصوب است.
- `INSURANCE_NUMBER` در DDL حداکثر ۲۰ کاراکتر است؛ قرارداد API حداکثر ۲۰ را می‌پذیرد. مدل نمونه UI برخی مسیرها را محدودتر کرده است. سیاست بانکی نهایی نیازمند تطبیق و تصویب است.
- وضعیت R2 ثبت قطعی هنوز `DISABLED` است و این تحویل آن را فعال نمی‌کند.

## اصلاح Warningهای Angular 21

در `clt-reference-workspace.component.ts` و `collateral-introduction.component.ts` نوع مقدار Index Signature پویا از `string` به `string | undefined` اصلاح شده است؛ Template با `?? ''` برای کلید غایب به رفتار قبلی پایبند است. چهار NG8102 قبلی باید در Build بعدی حذف شوند.
