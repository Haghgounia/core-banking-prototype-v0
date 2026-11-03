# CLT-R3C — قرارداد ارزیابی و تجدید ارزیابی وثیقه

**مرجع نسخه‌دار:** CLT-ROADMAP-R1.0؛ فاز ثابت R3 («ضامن، بیمه و ارزیابی»)، برش R3C. تاریخ: ۱۴۰۵/۰۸/۱۲ (2026-11-03). وضعیت: سورسِ آماده یکپارچه‌سازی، نه مجوز عملیات بانکی نهایی.

## مبنای واقعی
- `Collateral_UI_Final_Offline_V1.42_FA.html` (نمای valuation، `appraisalDueDate`، آزمون‌های دوره‌ای).
- `Collateral_Business_Use_Cases_FA.html`: BUC-08، BR-07، BR-12.
- `Collateral_System_Use_Cases_FA.html`: SUC-10 و SUC-17.
- `Collateral_Service_Specification_FA.html`: SVC-08، SVC-09، SVC-14.
- `CLT-2026-11-1.txt`: ساختار واقعیِ صادرشده (نه تأیید اتصال زنده Oracle): `CLT.COLLATERAL_VALUATION_HISTORY`، `CLT.COLLATERAL.CURRENT_VALUATION_HISTORY_ID`، `REF_VALUATION_TYPE/METHOD/STATUS`، `REF_CURRENCY`.
- `Collateral_Oracle_Reference_Data_Seed.sql`: کدهای فنی DRAFT/SUBMITTED/FINAL/REJECTED/SUPERSEDED؛ این Seed به معنای تصویب سیاست بانکی نیست.

## نکات مهم مدل

۱. در XMI و DDL جاری، ارزیابی‌های قطعی در `COLLATERAL_VALUATION_HISTORY` ذخیره می‌شوند. ترتیب (`VALUATION_SEQUENCE_NO`)، پیوند جایگزین (`SUPERSEDES_VALUATION_HISTORY_ID`) و اشاره‌گر جاری (`CURRENT_VALUATION_HISTORY_ID`) وجود دارند. هیچ جدول موازی برای *تاریخچه اصلی* در R3C ایجاد نمی‌شود.

۲. `FINAL` کد وضعیت نهایی در Oracle است؛ `APPROVED` رابط آفلاین نیازمند مصوبه نگاشت است (G-07). فقط `FINAL` می‌تواند شواهد «بازه تاریخ ثبت‌شده» را فراهم کند و `SUPERSEDED` یا `APPROVED` به‌طور حدسی معادل آن نیستند.

۳. **شکاف جدید G-18 (کارشناسی، نیازمند تصمیم):** UI V1.42 به `COLLATERAL_TYPE.IS_APPRAISABLE_FLAG` و `REVALUATION_INTERVAL_UNIT` با DAY/MONTH/YEAR متکی است؛ DDL صادرشده CLT تنها `REVALUATION_INTERVAL_VALUE` دارد. واحد زمان یا شمول ارزیابی را نمی‌توان حدس زد. `EFFECTIVE_TO` در تاریخچه با «موعد دوره‌ای برآمده از سیاست» یکسان نیست.

۴. حتی وقتی اشاره‌گر جاری به رکورد `FINAL` دارای تاریخ شروع/پایان معتبر و ارز یکسان اشاره کند، **`eligibleForNewAllocation=false`** است؛ زیرا تطبیق سیاست نوع وثیقه، ارز/FX، صلاحیت ارزیاب، وضعیت بیمه/ضامن و محاسبه ظرفیت تخصیص هنوز برقرار نشده‌اند. تخصیص‌های قبلی با انقضای ارزیابی به‌طور خودکار تغییر یا حذف نمی‌شوند.

۵. تا تثبیت تاریخ کسب‌وکاری بانک، مبنای خواندنی «تاریخ تقویمی Oracle در منطقه Asia/Tehran» گزارش می‌شود؛ این تاریخ به‌عنوان *روزکاری مصوب* معرفی نمی‌شود. «داخل بازه ثبت‌شده» تنها شاهد محدود، نه تصویب کفایت است.

## قرارداد API محدود به JWT و شعبه

| متد | آدرس | خروجی / اثر |
|---|---|---|
| GET | `/api/v1/collaterals/valuations/readiness` | پرچم‌های واقعی، پیش‌فرض عدم نوشتن |
| GET | `/api/v1/collaterals/{collateralId}/valuations?page=0&size=20` | تاریخچه واقعی و صفحه‌بندی‌شده، شامل FINAL و SUPERSEDED |
| GET | `/api/v1/collaterals/{collateralId}/valuations/current-evidence` | رکورد اشاره‌شده در شناسنامه، کنترل تاریخ/ارز/وضعیت، با `eligibleForNewAllocation=false` |
| GET | `/api/v1/collaterals/{collateralId}/valuations/options` | انواع، روش‌ها و ارزهای فعال از Oracle CLT |
| POST | `/api/v1/collaterals/{collateralId}/valuations/validate` | کنترل اعتبار فرم و منابع (بدون DML) |
| POST | `/api/v1/collaterals/{collateralId}/valuations/requests` | فقط درخواست مرحله‌ای DRAFT و Audit اتمیک، اختیاری و پیش‌فرض خاموش |
| GET | `/api/v1/collaterals/valuations/requests/mine` | درخواست‌های مرحله‌ای همان Maker و شعبه؛ اختیاری |

GET: `SCOPE_clt.collateral.read`؛ validate/درخواست: `SCOPE_clt.collateral.introduce`؛ Writer با پرچم `CLT_VALUATION_REQUEST_WRITES_ENABLED=false` پیش‌فرض خاموش است. `Authentication` باید JWT دارای `branch_code` باشد؛ هیچ Branch یا Actor از Payload یا Headerهای `X-User-*` پذیرفته نمی‌شود. Missing و forbidden collateral در API نتیجه یکسان 404 دارند.

## قرارداد اعتبارسنجی درخواست

- شناسه‌های Oracle به شکل رشته مثبت با حداکثر ۱۹ رقم؛ پول `NUMBER(20,4)` با `BigDecimal`، نه Double یا تبدیل اعشاری JS.
- نوع و روش ارزیابی از مرجع فعال CLT؛ کدهای ناشناخته/غیرفعال رد می‌شوند.
- ارزیاب دارای Party ID یا نام؛ صلاحیت حرفه‌ای او **هنوز اعتبارسنجی نشده** و جزء G-09 است.
- `EFFECTIVE_FROM >= VALUATION_DATE` و در صورت وجود `EFFECTIVE_TO >= EFFECTIVE_FROM`.
- مبلغ و ارزش‌های اختیاری غیرمنفی، درصد کسر ارزش بین ۰ تا ۱۰۰؛ هیچ مقدار یا نرخ Haircut پیش‌فرض بانکی تعریف نشده است.
- تجدید ارزیابی باید به `CURRENT_VALUATION_HISTORY_ID` واقعیِ دارای وضعیت FINAL اشاره کند. این صرفاً کنترل درخواست مرحله‌ای است، نه جایگزینی عملیاتی سوابق.
- هر درخواست با `Idempotency-Key`، `PAYLOAD_HASH`، و رویداد Audit `REQUEST_CREATED` در یک تراکنش ثبت می‌شود؛ تکرار با Payload متفاوت 409 می‌دهد. اجرای آن فقط پس از تصویب و نصب دو جدول پیشنهادی/حساب Writer مجزاست.
- هیچ endpoint نهایی‌سازی، حذف/ویرایش `COLLATERAL_VALUATION_HISTORY` یا تغییر `COLLATERAL.CURRENT_VALUATION_HISTORY_ID` در R3C وجود ندارد.

## موانع رسمی و ورودی‌های تصمیم

- G-07: نگاشت `APPROVED` در UI به `FINAL` در Oracle و قرارداد تأیید ارزیاب.
- G-18: `IS_APPRAISABLE_FLAG` و `REVALUATION_INTERVAL_UNIT`، معنای صفر و تقویم/روزکاری، حدود زمانی.
- G-09: صلاحیت ارزیاب، مرجع نرخ ارز FX، تأیید مالک CIF، DMS و سرویس‌های بانکی.
- G-10: ضرایب Haircut/Eligible Value، سیاست‌های ستادی و عدم استفاده از نرخ نمونه HTML.
- R2/G-14/G-17: ثبت قطعی شناسنامه و هویت دارایی هنوز بسته نشده است.
- R4: قاعده عدم تخصیص جدید در صورت انقضا/کسری باید در Consumer Allocation Gate برقرار شود؛ این R3C *هیچ تخصیص فعالی را تغییر نمی‌دهد*.

## DBA و عملیات

`database/oracle/clt/r3c/00_VALUATION_READONLY_PRECHECK.sql` فقط‌خواندنی است. فایل `01_VALUATION_REQUEST_MODEL_CANDIDATE_NOT_APPROVED.sql` در ابتدای اجرا خطای `-20953` می‌دهد و SQL پیشنهادی آن داخل Comment است؛ **اجرا و فعال‌سازی ممنوع تا تصویب رسمی**. Installer هیچ DDL/DML اجرا نمی‌کند.

اکوسیستم IAM مستقل توسعه می‌یابد. سه فایل مشترک پروژه (Routing/Sidebar/Build) و همه فایل‌های موجود IAM، CIF، DPS2 دست‌نخورده‌اند.
