# CLT R3 — Atomic Sponsor Posting Candidate / Checkpoint

**تاریخ:** 2026-11-09  | **تحویل:** `CLT-R3-SPONSOR-ATOMIC-COMMIT-2026-11-09`  | **Baseline:** `core-banking-prototype-v0-2026-11-09-R3-POSTING-PREFLIGHT-SOURCE.zip`  | **نسخه نقشه راه:** R1.0، بدون تغییر R0..R7

## نتیجه توسعه

- کد اجرایی **Stage REVIEWED → `CLT.COLLATERAL_SPONSOR`** اضافه شد و از `DataSource` مشترک استفاده می‌کند؛ این کد ذاتاً write-capable است، اما **فعال‌سازی آن به‌صورت پیش‌فرض `false`** است.
- `POST /api/v1/collaterals/r3-workflow/sponsor/{requestId}/commit` با بدنه `{expectedVersion:"3", idempotencyKey, reason}` و پاسخ Receipt دارای `sponsorId`, `policyRevision`, `replayed`؛ مجوزهای موجود `clt.collateral.read` و `clt.collateral.review` و بازبین مستقل از Maker و مستقل از تصویب‌کننده Release اجباری است.
- `GET /api/v1/collaterals/r3-workflow/sponsor/posting-activation`، وضعیت Feature Flag، قرارداد DB و مصوبه Release را به‌صورت جداگانه اعلام می‌کند. `GET /api/v1/collaterals/r3-workflow/sponsor/{requestId}/receipt` رسید COMMITTED را فقط برای شعبه Principal نمایش می‌دهد و پس از غیرفعال‌شدن Flag نیز قابل مشاهده است. کارتابل R3 فقط در صورت فعال بودن واقعی گیت‌ها امکان اقدام را نشان می‌دهد.
- داخل یک **تراکنش Oracle**، Stage و وثیقه `FOR UPDATE`، کلید Idempotency و Ledger یکتا، Approval Record بانکی زمان‌دار، Audit/Fingerprint/نسخه ۳، شواهد preflight و احراز مجدد CIF/Party بازخوانی می‌شوند. سپس Insert ضامن و ثبت Receipt اتمیک انجام می‌شود؛ خطا موجب Rollback کل تراکنش است. Oracle `RETURNING ... INTO` در همان Connection شناسه ثبت را می‌خواند؛ اتکا به Last Insert ID یا شناسه مرورگر وجود ندارد.
- Replay با کلید/Hash/دلیل/نسخه/Actor یکسان، Receipt قبلی را برمی‌گرداند؛ اختلاف با HTTP 409 رد می‌شود. Unique DB برای `(REQUEST_KIND,REQUEST_ID)` و `(BRANCH_CODE,IDEMPOTENCY_KEY)` و **Unique ضامن `(COLLATERAL_ID,PARTY_ID)`** جزء پیش‌شرط اجباری هستند.
- هیچ فایل فرم سفارشی بیمه/ضامن/ارزیابی، پیکربندی DataSource، ماژول IAM یا داده Oracle در Installer تغییر نمی‌کند.

## گیت‌های فعال‌سازی — «کد آماده» با «بانک تأیید کرده» متفاوت است

1. `core-banking.collateral.r3-posting.sponsor-enabled=false` (پیش‌فرض؛ این بسته هیچ پروفایل با `true` تحویل نمی‌دهد).
2. جداول و قیود `CLT.R3_POSTING_LEDGER`, `CLT.R3_POSTING_RELEASE`, Sequence و Unique Physical Sponsor فقط پس از DBA Review / Duplicate Reconciliation قابل استقرار هستند؛ SQL نمونه `database/oracle/clt/r3posting/01_SPONSOR_POSTING_LEDGER_RELEASE_CANDIDATE_NOT_APPROVED.sql` در ابتدا با `RAISE_APPLICATION_ERROR` عامدانه متوقف می‌شود.
3. یک Release **واقعی** از نوع `SPONSOR`, با `RELEASE_STATUS='APPROVED'`, `POLICY_REVISION`, `BUSINESS_DECISION_REF`, `DATA_DECISION_REF`, `INTEGRATION_DECISION_REF`, `APPROVED_BY`, پنجره اعتبار زمانی، و اسناد مرتبط وجود داشته باشد. Installer هیچ Release Row ایجاد نمی‌کند.
4. کنترل‌های فنی و شواهد مرحله‌ای لازم‌اند ولی کافی برای نتیجه عملیاتی/مجوز تخصیص نیستند؛ سوابق ابطال ضامن و حداقل تعداد معتبر هنوز G-05/G-11 `OPEN` می‌مانند. احراز امنیتی/عملیاتی و قرارداد Integration G-09 همچنان به تصویب و آزمون زنده نیاز دارد.
5. `REVIEWED` مرحله‌ای به معنی Approved نیست. ضامن ثبت‌شده به معنی ضامن **معتبر برای تخصیص** نیست؛ محاسبه آن به R4 و Policy R3 نیاز دارد.

## شکاف جدید فیزیکی (D-04، بدون تغییر شناسه‌ها یا بستن Gap)

در سند `Collateral_DB_Specification_FA(2).html`/مدل EA برای جدول `COLLATERAL_SPONSOR` فقط PK شناسه مستقل و FK وثیقه ثبت شده است، نه Unique مرکب `(COLLATERAL_ID,PARTY_ID)`. نبود آن می‌تواند تحت چند Writer، ثبت ضامن تکراری ایجاد کند. **D-04**: برقراری یکتایی فیزیکی همراه پاک‌سازی داده موجود و Impact Analysis به تأیید DBA نیاز دارد؛ کد در نبود قید فعال/VALIDATED متوقف می‌شود. این موضوع به G-05 و کنترل‌های DB/Enterprise Integration مرتبط است. فازهای مصوب عوض نمی‌شوند.

## QA و وضعیت

- Java21 pure policy Smoke، Java syntax parse، Synthetic Spring typecheck، TypeScript syntax parse، SQL contract checks، Regression قبلی R3 و Dry-run Installer/Idempotency/Rollback/Conflicts در `QA-REPORT.json` ثبت می‌شوند.
- **تأیید نشده:** Build واقعی Maven/Angular با وابستگی‌های کامل و Live Oracle/DBA migration و E2E/Concurrency Race با بانک. اینها پیش از فعال‌کردن Feature Flag لازم‌اند. نصب سورس موفق مساوی فعال شدن عملیات نیست.
- **گام بعد R3:** بعد از تکمیل و تأیید این زیرساخت، چرخه بیمه (پوشش/تمدید/لغو) و تاریخچه ارزیابی (`FINAL`, Appraisability, Business Date) با همان رویکرد تراکنشی و مصوبات مستقل؛ سپس UAT جامع R6.
