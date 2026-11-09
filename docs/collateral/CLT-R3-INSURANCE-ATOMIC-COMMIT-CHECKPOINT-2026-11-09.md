# CLT R3 — Insurance Atomic Create/Renew Candidate / Checkpoint

**Date:** 2026-11-09 | **Delivery:** `CLT-R3-INSURANCE-ATOMIC-COMMIT-2026-11-09` | **Baseline:** `core-banking-prototype-v0-2026-11-09-R3-SPONSOR-ATOMIC-COMMIT-SOURCE.zip` | **Roadmap:** `CLT-ROADMAP-R1.0`, R0..R7 unchanged.

## دامنه تحویل

این تحویل مسیر کدنویسی‌شدهٔ **ثبت بیمه‌نامه جدید یا تمدید** (`BUC-07`, `SUC-09`, `SVC-07`) را بر اساس درخواست Stage با وضعیت `REVIEWED` ارائه می‌کند؛ **به‌صورت پیش‌فرض غیرفعال** و بدون Oracle DDL خودکار.

- `GET /api/v1/collaterals/r3-workflow/insurance/posting-activation` — تفکیک Feature Flag، آمادگی Schema و Release بانکی.
- `GET /api/v1/collaterals/r3-workflow/insurance/{requestId}/receipt` — Receipt ثبت‌شده فقط در محدوده شعبه Principal؛ حتی پس از غیرفعال‌شدن Flag قابل مشاهده است.
- `POST /api/v1/collaterals/r3-workflow/insurance/{requestId}/commit` — مشروط به `expectedVersion="3"`, Idempotency Key و Reason معتبر، Reviewer مستقل و Approvalهای واقعی.
- UI کارتابل R3 وضعیت ثبت را از `REVIEWED` جدا می‌کند، رسید عملیاتی را نشان می‌دهد و در نبود هر گیت دکمه ثبت را غیرفعال نگه می‌دارد. فرم‌های سفارشی بیمه، ضامن و ارزیابی بازنویسی نمی‌شوند.

## گیت‌های بانکی و فنی

1. `core-banking.collateral.r3-posting.insurance-enabled=false` به‌صورت پیش‌فرض. هیچ Profile یا YAML تغییر نمی‌کند.
2. Oracle `R3_INS_POSTING_RELEASE`, `R3_INS_POSTING_RULE`, `R3_INS_POSTING_LEDGER` و Sequence با Migration **مصوب و جداگانه**؛ همه با Dictionary Check تاییدشده. SQL پیوست `01_INSURANCE_POSTING_SCHEMA_CANDIDATE_NOT_APPROVED.sql` با `RAISE_APPLICATION_ERROR` قطع می‌شود.
3. قید فیزیکی یکتای `(INSURANCE_COMPANY_ID,INSURANCE_NUMBER)` طبق مدل EA (`UQ_CI_POLICY`) باید ENABLED/VALIDATED باشد. **D-05:** برای جلوگیری از دو تمدید هم‌زمان از یک بیمه‌نامه، قید فیزیکی یکتای `PREVIOUS_INSURANCE_ID` لازم است؛ پیش از اعمال آن داده‌های فعلی باید بررسی شوند.
4. Release زمان‌دار واقعی بانک با ارجاع Business/Data/Integration/Coverage/Renewal/Cancellation Impact، شناسه Approver و Revision؛ هیچ رکورد Approval یا مبلغ پوشش نمونه در بسته به DB افزوده نمی‌شود.
5. Rule مصوب به ازای `(COLLATERAL_TYPE_ID, INSURANCE_TYPE_ID, CURRENCY_CODE, POLICY_REVISION)` با مقدار `MIN_COVERAGE_AMOUNT` و بازه زمانی و Approver واقعی؛ در نبود Rule یکتا و معتبر، Commit مسدود است. این Rule صرفاً حداقل پوشش **همان بیمه‌نامه** را اثبات می‌کند و مجموعه کامل بیمه‌های اجباری وثیقه برای Allocation را تایید نمی‌کند.
6. Stage `REVIEWED`، Version=3، سه رویداد Audit، Hash و استقلال Maker/Poster، عدم تکرار Company/Number و عدم هم‌پوشانی نوع و دوره باید در همان تراکنش مجدداً محاسبه شوند.
7. در تمدید، `PREVIOUS_INSURANCE_ID` باید همان وثیقه و نوع بیمه باشد؛ تاریخ شروع دقیقاً یک روز پس از پایان قبلی و تعداد جانشینان قبلی صفر باشد. قید Unique فیزیکی احتمال هم‌زمانی مستقل را کنترل می‌کند.
8. TransactionTemplate با DataSource مشترک: Stage و وثیقه و Rule و predecessor قفل می‌شوند؛ `PREPARED` Ledger، `INSERT ... RETURNING` Domain و `COMMITTED` Receipt اتمیک هستند؛ خطا تمام عملیات را Rollback می‌کند. Replay فقط با key/hash/reason/version/poster یکسان مجاز است.

## محدوده‌های عمداً باقی‌مانده

- **G-04 OPEN:** Policy مجموعه کامل بیمه‌های ضروری، سطوح پوشش، ارز و کفایت برای تخصیص هنوز باید از بانک تصویب شود؛ Rule حداقل مبلغ برای یک بیمه‌نامه جایگزین آن نیست.
- **G-06 OPEN:** Cancel/Revoke و اثر آن بر تعهدات/تخصیص و تاریخچه Append-only وجود ندارد. این نسخه هیچ Endpoint یا DML لغو صادر نمی‌کند و Renewal به معنی لغو خودکار بیمه‌نامه قبلی نیست.
- **G-09 OPEN:** استعلام اعتبار واقعی از بیمه‌گر طرف قرارداد و اتصال Enterprise هنوز نیازمند قرارداد/تست عملیاتی است؛ `INSURER_CODE=2` فقط بیمه‌نامه ارائه‌شده توسط مشتری را در Stage می‌پذیرد.
- **D-05 OPEN:** دیتابیس فعلی برای `PREVIOUS_INSURANCE_ID` فقط Index غیر یکتا دارد؛ قید Unique و سازگاری داده باید جداگانه به تایید DBA برسد.
- هیچ تغییری در IAM، DataSource، کد ضامن یا ارزیابی انجام نشده و R4 آغاز نشده است.

## QA

- Java21 policy smoke، JUnit policy، Java synthetic typecheck و TypeScript parser، source contract verifier و آزمون installer / rollback انجام می‌شوند؛ نتایج دقیق در `QA-REPORT.json` بسته هستند.
- **هنوز انجام نشده:** Maven/Angular production build با وابستگی‌های کامل، اجرای Oracle واقعی/مهاجرت DBA، Concurrency E2E، تأیید داده Bank Release، UAT تحلیلگر.
- تأیید کاربر فقط نصب بسته قبلی `CLT-R3-SPONSOR-ATOMIC-COMMIT` است، نه تأیید این Release یا اجرای DML واقعی.
