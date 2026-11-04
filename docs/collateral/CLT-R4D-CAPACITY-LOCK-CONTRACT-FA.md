# CLT-R4D — قرارداد قفل سطر وثیقه و نمونه‌برداری تراکنشی (فقط تشخیصی)

**نسخه پایه:** Core Banking Prototype `0.11.0` به‌همراه `R4C`  
**مرجع ثابت:** `CLT-ROADMAP-R1.0` — R4 «ظرفیت و تخصیص بین‌دامنه‌ای»  
**وضعیت:** Source Implementation Candidate؛ **فعال‌سازی مالی ممنوع**، بررسی Oracle و استعلام JWT عملیاتی انجام‌نشده.

## ۱. مسئله و مرزبندی

در R4C، کلید یکتای `(CONSUMER_DOMAIN, CLIENT_ID, IDEMPOTENCY_KEY)` فقط یک درخواست واحد را از تکرار مصون می‌کند. دو **درخواست متفاوت** برای یک وثیقه ممکن است از دو Snapshot غیراتمیک ظرفیت عبور کنند. R4D مسیر کنترل‌شده‌ای اضافه می‌کند که **سطر والد `CLT.COLLATERAL` را قفل و داده‌های مالی را روی همان Connection/Transaction دوباره می‌خواند**؛ اما **هیچ Allocation، Hold یا Reservation صادر نمی‌کند**.

قفل یک Probe صرفاً هنگام تراکنش وجود دارد و هنگام بازگشت پاسخ آزاد شده است. `parentRowLockedWithinTransaction=true` به معنی «پروتکل اجرا شد» است، نه اینکه قفل در دسترس مصرف‌کننده باقی مانده یا ظرفیت رزرو شده باشد.

## ۲. توالی یک Unit of Work

1. قبل از هر تماس با Oracle: JWT امضاشده، `SCOPE_clt.allocation.request`، `client_credentials`، `client_id/azp`، Client Binding همان دامین، شعبه توکن، `collateralId` و `requestId` کنترل می‌شوند.
2. Feature Gate دوگانه `capacity-lock.probe-enabled=false` و `allocation.intake.request-writes-enabled=false`، هر دو پیش‌فرض خاموش‌اند. بدون هر دو، `503` یا `403` باز می‌گردد.
3. شرط تکمیلی R4C برای سه جدول درخواست و Unique/FK/Check باید معتبر باشد. حساب Writer محدود، `SELECT` روی جداول مربوط را نیاز دارد؛ Privilege لازم برای `SELECT ... FOR UPDATE` نیز باید صریحاً توسط DBA آزموده و تصویب شود. هیچ grant در بسته اجرا نمی‌شود.
4. **شروع TransactionTemplate روی همان DataSource**؛ اولین Query مالی: `SELECT COLLATERAL_ID,RECORD_VERSION FROM CLT.COLLATERAL WHERE COLLATERAL_ID=? AND BRANCH_CODE=? FOR UPDATE WAIT 1`؛ شناسه و شعبه از مقادیر کنترل‌شده استخراج می‌شوند.
5. خواندن درخواست R4C صرفاً برای همان وثیقه، Client، دامین و شعبه. تنها `RECORDED_NOT_ALLOCATED` پذیرفته می‌شود؛ شناسه پرونده، مبلغ و ارز از رکورد ثبت‌شده، نه بدنه HTTP، گرفته می‌شوند.
6. با **همان JdbcTemplate و Connection**: وضعیت و نسخه وثیقه، نوع و وضعیت مرجع، اشاره‌گر `CURRENT_VALUATION_HISTORY_ID`، ارزش ثبت‌شده، اعتبار تاریخ ارزیابی، مجموع تخصیص‌های فعال `USED='YES'` به تفکیک دامین/ارز، بدهکاران ثبت‌شده و تعارض فعلی با ضامنین بازخوانی می‌شوند. سقف گروه‌بندی ۲۰۰ ردیف است.
7. محاسبه اختلاف حسابی و کسری فرضی با `BigDecimal` و قواعد R4A انجام می‌شود؛ ارز متفاوت بدون FX رسمی **قابل جمع نیست**. وضعیت `FINAL` از `APPROVED` در UI متمایز است و انقضای ارزیابی مجوز جدید نمی‌دهد.
8. نتیجه صرفاً شواهد `EVIDENCE_LEVEL=SINGLE_ORACLE_TRANSACTION_PARENT_ROW_LOCK_NO_FINANCIAL_WRITE` است؛ `allocationAuthorized=false`، `capacityReservationPersisted=false` و `oracleWritePerformed=false` **ثابت‌اند**. پایان تراکنش، قفل آزاد. خطای Lock Timeout، JDBC، مغایرت یا قطع اتصال موجب توقف کنترل‌شده می‌شود.

**هشدار هم‌زمانی:** حتی اجرای درست `FOR UPDATE` برای Probe به‌تنهایی مانع Over-Allocation از Writer دیگری نمی‌شود، اگر آن Writer همان قفل را در همان Transaction قبل از محاسبه/ثبت تخصیص نگیرد. اثبات همکاری تمام دامین‌های نویسنده، تصویب فرمول پذیرش/FX و ایجاد/آزمون Actual Financial Commit هنوز انجام نشده‌اند.

## ۳. APIها

| Method | مسیر | مجوز | معنا |
|---|---|---|---|
| `GET` | `/api/v1/collaterals/{collateralId}/allocation-requests/lock/readiness` | JWT + `SCOPE_clt.collateral.read` | فقط وضعیت Feature Gate، **نه** تأیید واقعی Oracle |
| `POST` | `/api/v1/collaterals/{collateralId}/allocation-requests/lock/probes/{requestId}?consumerDomain=LOAN` | JWT سرویس + `SCOPE_clt.allocation.request` + Client Binding + فعال‌سازی دوگانه | نمونه‌برداری محدود در Oracle، بدون درخواست جدید، بدون Write |

هیچ UI بانکی نمی‌تواند POST سرویس‌به‌سرویس را فراخوانی کند؛ فقط وضعیت عمومی گیت در صفحه ظرفیت/درخواست نمایش داده می‌شود. شناسه اشخاص و JWT در پاسخ Probe نمی‌آیند. خطاهای SQL نیز به پیام عمومی تبدیل می‌شوند.

## ۴. پیکربندی و DBA

`backend/src/main/resources/clt/allocation-capacity-lock.properties`:

```properties
core-banking.collateral.allocation.capacity-lock.probe-enabled=${CLT_CAPACITY_LOCK_PROBE_ENABLED:false}
```

به‌تنهایی **هرگز کافی نیست**: علاوه بر آن، R4C Writer و Schema با مجوز مستقل و Client Map مصوب لازم‌اند. هیچ SQL خودکار در Installer اجرا نمی‌شود؛ فایل `database/oracle/clt/r4d/00_CAPACITY_LOCK_READONLY_PRECHECK.sql` فقط اطلاعات Dictionary را می‌خواند. برای اجرای Oracle Probe حتی در آزمایشگاه، DBA باید اثر قفل روی SLA و Row Contention را بررسی کند.

## ۵. موارد باز و معیار خروج واقعی R4

- G-02: درخواست R4C در سورس وجود دارد اما Migration/Writer/Idempotency واقعی اثبات نشده است.
- G-09: تأیید اصالت تعهد و کامل بودن فهرست بدهکاران توسط دامین مصرف‌کننده لازم است.
- G-13: مجوز بین‌واحدی در سطح معرفی وثیقه هنوز ذخیره نشده است.
- G-10: ضرایب پذیرش و نرخ مبنا از نمونه UI قابل اخذ نیستند.
- G-04/05/06/07/11/18: بیمه، ضامن، ارزیابی و سیاست دوره‌ای دارای موانع بازند.
- نگاشت رسمی MODULE_ID، G-16 وضعیت تراکنش، نقش‌های واقعی IAM و قفل‌پذیری تمام Writerهای تخصیص هنوز مصوب نیستند.
- **برای خروج R4** باید عملیات واقعی در Transaction واحد: Parent Lock → Re-read → Attestation → Capacity Calculation → INSERT/UPDATE Allocation + Audit + Idempotency → Commit/Rollback، تحت آزمون Oracle موازی و Maker/Checker واقعی تأیید شود. چنین Writerی در R4D تحویل داده نشده است.

## ۶. ماتریس QA

- نصب از R4C و نصب مجدد بدون تغییر، کنترل تعارض Hash و حفظ دیگر ماژول‌ها.
- آزمون Pure Java و شبیه‌ساز هم‌زمانی **صرفاً در حافظه** (اثبات Oracle نیست).
- آزمون Parser Java/TypeScript، Static Security Rules، JAR bytecode بعد از Build روی Windows.
- Oracle حقیقی: تست `FOR UPDATE WAIT 1`، نقش Writer، Timeouts، Rollback، Race در دو Session و انطباق تمام Writers؛ در محیط کاربر و پس از تصویب، نه در بسته.
