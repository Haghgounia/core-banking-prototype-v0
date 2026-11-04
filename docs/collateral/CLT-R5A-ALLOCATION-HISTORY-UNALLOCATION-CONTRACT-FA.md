# CLT-R5A — قرارداد سوابق تخصیص و پیش‌بررسی رفع تخصیص

**نسخه:** 1.0 / 2026-11-04  | **مرجع نقشه راه:** `CLT-ROADMAP-R1.0`  | **سطح تحویل:** Source Candidate / Read-only / Fail-Closed

## ۱. محدوده و استناد

این تحویل مرحله R5A از فاز ثابت **R5: رفع تخصیص، آزادسازی و سوابق** است و بر مبنای `Collateral_System_Use_Cases_FA.html` (SUC-13)، `Collateral_Business_Use_Cases_FA.html` (BUC-10)، `Collateral_Service_Specification_FA.html` (SVC-11)، `Collateral_API_Contract_FA.html` و متن خروجی DDL `CLT-2026-11-1.txt` توسعه یافته است.

**تمایز دامنه‌ای:** Unallocate آزادسازی مصرف ظرفیتِ یک *تعهد مشخص* به درخواست دامین صاحب آن است؛ **Final Release** تغییر وضعیت کل وثیقه پس از رفع همه تعهدات و موانع حقوقی است و در R5A پیاده‌سازی نمی‌شود. نتیجه `USED='NO'` در `COLLATERAL_ALLOCATION` به تنهایی اثبات رویداد Posted Unallocation نیست.

## ۲. تطبیق مدل داده واقعی

| شیء فیزیکی CLT | نقش در R5A | محدودیت |
|---|---|---|
| `COLLATERAL` | احراز شناسه وثیقه و شعبه از JWT | بدون Update |
| `COLLATERAL_ALLOCATION` | فهرست تخصیص‌ها، MODULE_ID، FILE_NUMBER، مبلغ، ارز، USED، RECORD_VERSION | فقط SELECT؛ `USED=NO` بدون علت تاییدشده |
| `COLLATERAL_TRANSACTION` | شواهد تراکنش‌های مرتبط با Allocation ID، نوع/وضعیت و تراکنش مبنا | تراکنش‌های با Allocation ID خالی در لیست این مرحله نیستند |
| `REF_COLL_TXN_TYPE` / `REF_COLL_TXN_STATUS` | مرجع نیازمند انطباق در فاز عملیاتی | Seed کد `REVERSAL` دارد؛ وضعیت `PENDING` در DDL پیش‌فرض است اما Seed فعلی آن را ندارد (G-16) |

محدودیت API: حداکثر ۵۰ رکورد در هر درخواست، مقدار پیش‌فرض ۲۰؛ مرزبندی بر اساس `CLT.COLLATERAL.BRANCH_CODE` و انطباق `COLLATERAL_ALLOCATION.BRANCH_CODE`، نه کد شعبه ارسالی در HTTP. `NUMBER(19)` و `NUMBER(20,4)` در JSON به صورت رشته برای حفظ دقت.

## ۳. Endpointها

| روش | Endpoint | Scope | اثر پایگاه داده |
|---|---|---|---|
| GET | `/api/v1/collaterals/{collateralId}/allocations/readiness` | `clt.collateral.read` | بدون SQL |
| GET | `/api/v1/collaterals/{collateralId}/allocations?limit=20` | `clt.collateral.read` | SELECT فقط‌خواندنی |
| GET | `/api/v1/collaterals/{collateralId}/allocations/{allocationId}/transactions?limit=20` | `clt.collateral.read` | SELECT فقط‌خواندنی |
| POST | `/api/v1/collaterals/{collateralId}/allocations/{allocationId}/unallocation/preview` | `clt.collateral.read` | SELECT؛ پیش‌نمایش تحلیلگر |
| POST | `/api/v1/collaterals/{collateralId}/allocations/{allocationId}/unallocation/service-preflight` | `clt.unallocation.preflight` + `client_credentials` + Bank Client Allowlist | SELECT؛ بدون تأیید رفع تخصیص |

**هیچ Endpoint `POST .../unallocate` عملیاتی یا `POST .../final-release` در R5A ساخته نشده است.** مسیرهای جدید در همان SecurityFilterChain وثایق ثبت شده‌اند؛ Route و امنیت IAM مشترک تغییر نمی‌کند.

### درخواست پیش‌بررسی

Headers: `Authorization: Bearer ...`, `Idempotency-Key: <UUID canonical>`, `If-Match: "4"` (ETag قوی مطابق `RECORD_VERSION` تخصیص).

```json
{
  "consumerDomain": "LOAN",
  "fileNumber": "123456789",
  "requestedAmount": "10000000.0000",
  "currencyCode": "IRR",
  "releaseMode": "FULL",
  "reasonReference": "CASE-RELEASE-2026-001"
}
```

دامین‌ها: `LOAN`, `GUARANTEE`, `LETTER_OF_CREDIT`, `GREEN_LICENSE`، همسو با R4B. «FULL» فقط یک کاندیدای بررسی است؛ رفع جزئی به‌دلیل نبود قواعد مصوب و ناسازگاری بالقوه با مدل `USED` مسدود است. `reasonReference` مرجع ردیابی درخواست است، نه «کد علت» تأییدشده از کاتالوگ.

### پاسخ همواره Fail-Closed

```json
{
  "allocationId": "10", "collateralId": "99", "consumerDomain": "LOAN",
  "activeAllocation": true, "expectedVersionMatched": true,
  "domainOwnerMapped": false, "serviceClientVerified": false,
  "consumerObligationVerified": false, "idempotencyPersisted": false,
  "capacityReleased": false, "unallocationAuthorized": false,
  "decision": "DENIED_NO_FINANCIAL_UNALLOCATION",
  "evidenceLevel": "R5A_NON_ATOMIC_READ_ONLY_NO_CONSUMER_ATTESTATION_NO_DURABLE_IDEMPOTENCY",
  "blockers": ["G-09_CONSUMER_OBLIGATION_STATUS_NOT_ATTESTED", "CLT_R5A_FINANCIAL_UNALLOCATION_AND_AUDIT_WRITES_DISABLED"]
}
```

این JSON نمونه توضیحیِ فیلدهاست؛ فهرست واقعی `blockers` بزرگ‌تر و داده‌محور است. هیچ Digest یا Idempotency-Key صرفاً به دلیل تولید در Preflight، «Persisted» تلقی نمی‌شود.

## ۴. قرارداد مالکیت، امنیت و هم‌زمانی

۱. شناسه شعبه از `CollateralIntroductionService.actor(jwt).branch()` استخراج می‌شود. شماره پرونده صرفاً با `COLLATERAL_ALLOCATION.FILE_NUMBER` تطبیق می‌شود؛ اثبات تعهد فعالِ دامین مصرف‌کننده نیست.
۲. در Service Preflight، `CollateralAllocationConsumerBinding` باید Client ID، Grant و دامنه مصرف‌کننده را احراز کند؛ نبود نگاشت مصوب قبل از دسترسی به دیتابیس رد می‌شود و تخصیص متعلق به ماژول دیگر بدون افشای جزئیات با 404 پاسخ می‌گیرد. نگاشت Server-Owned `consumerDomain -> CLT.MODULE_ID` باید توسط بانک تعیین شده باشد. تمامی نگاشت‌ها پیش‌فرض خالی هستند. `MODULE_ID` نمایشی یا مقدار نمونه UI مبنا نیست.
۳. `If-Match` یک ETag قوی و بدون صفر ابتدایی است. اختلاف با `RECORD_VERSION` به‌عنوان مانع گزارش می‌شود. `Idempotency-Key` UUID معتبر است ولی در R5A **ثبت پایدار یا Replay Protection ندارد**.
۴. خواندن سوابق به‌خودی‌خود نیاز به قفل والد ندارد؛ اما اجرای واقعی رفع تخصیص در R5B+ باید زیر همان قفل والد R4D و تراکنش واحد با قید یکتا، ثبت وضعیت تعهد و Audit باشد. هیچ Worker یا Writer واقعی در R5A اضافه نشده است.
۵. نمایش `[USED=NO]` ممکن است ناشی از رخدادی خارج از قرارداد عملیاتی CLT باشد؛ برای تأیید قطعی باید تراکنش Posted و تصمیم دامین مصرف‌کننده و Audit تطبیق شوند.

## ۵. شکاف‌های باز و تصمیم‌های مورد نیاز

- **G-02:** چرخه مستقل درخواست‌های تخصیص و رفع تخصیص + Durable Idempotency و بازپخش پاسخ؛ R4C فقط Intake تخصیص را در حالت پیش‌فرض خاموش دارد.
- **G-08 / G-16:** مدل تایید وضعیت تراکنش، قواعد `REVERSAL` و `PENDING`؛ تفکیک رفع تخصیص از برگشت تراکنش و آزادسازی نهایی.
- **G-09:** تأیید تعهد اصلی، وضعیت بدهی و دامنه صاحب پرونده با API مصرف‌کننده؛ از `FILE_NUMBER` نمی‌توان آن را استنتاج کرد.
- **G-13:** مجوز دقیق شعبه معرفی‌کننده/نوع وثیقه/شعبه اجراکننده و تفکیک وظایف.
- **G-19 (جدید):** نگاشت مصوب `consumerDomain` به `CLT.MODULE_ID` و مرجع رسمی Module Catalog؛ نباید از UI نتیجه‌گیری شود.
- **G-20 (جدید):** قرارداد کاهش/رفع جزئی و کامل تخصیص، Ledger/Audit ثبت‌شده، ترتیب قفل و درستی بازگشت ظرفیت؛ الگوی `USED` موجود برای تصمیم قطعی کفایت ندارد.

## ۶. معیارهای خروج و محدودیت گواهی

تحویل R5A زمانی از نظر Build تأیید است که Windows `INSTALL-AND-BUILD.cmd` موفق شود. UAT نیز باید با توکن واقعی، Oracle واقعی و آزمون داده ساختگی کنترل‌شده انجام گیرد. **هیچ طرح SQL در این بسته اجرا نمی‌شود** و فایل Precheck فقط خواندنی است. موفقیت Smoke/JAR به معنی تأیید Runtime و مجوز بانک نیست.
