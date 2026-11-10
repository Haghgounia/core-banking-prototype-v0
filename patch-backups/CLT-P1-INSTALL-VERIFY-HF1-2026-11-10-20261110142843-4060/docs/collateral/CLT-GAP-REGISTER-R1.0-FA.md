# دفتر شکاف‌ها و تصمیمات Collateral — CLT-GAPS-R1.0

**تاریخ:** 2026-11-03  
**وضعیت:** Approved as issue register؛ تصمیم‌های ساختاری با برچسب OPEN هنوز مصوب نشده‌اند.  
**منابع:** شکاف‌های G-01..G-15 عیناً از DB/API/UI/System/Service Specification پیوست استخراج شده‌اند. G-16 و G-17 حاصل تطبیق مستقل مدارک و کد شبیه‌ساز هستند. G-18 از قرارداد ارزیابی R3C و تطبیق UI V1.42 با DDL استخراج شده است.

| شناسه | شکاف/تعارض | تصمیم اولیه/گزینه طراحی (نه تغییر خودکار) | گیت رفع | وضعیت |
|---|---|---|---|---|
| G-01 | فقدان ذخیره درخواست اصلاح شناسنامه و رأی | مدل Amendment + Pending Decision + Audit؛ فقط پیش از هر وابستگی | R2 | OPEN |
| G-02 | درخواست مستقل تخصیص و Replay/Idempotency ذخیره نمی‌شود | Allocation Request/Decision با شناسه مصرف‌کننده، Snapshot و کنترل تکرار | R4 | OPEN |
| G-03 | بازه سریال و اقلام شمارشی مدل مستقل ندارد | تعریف Item Range با کنترل هم‌پوشانی، سقف هر ردیف 20 و تعداد صحیح | R2 | OPEN |
| G-04 | تعریف الزامات نوع بیمه برای نوع وثیقه/معرفی ناقص | Rule Mapping تأییدشده و تاریخ اعتبار، کنترل قبل تخصیص | R3 | OPEN |
| G-05 | وضعیت و تاریخچه تأیید/لغو ضامن در جدول نیست | Sponsor Lifecycle + Audit؛ حداقل معتبر قابل محاسبه | R3 | OPEN |
| G-06 | چرخه لغو/جایگزینی بیمه ناقص | Insurance Status + Renewal/Cancel Audit؛ حفظ بیمه قبلی | R3 | OPEN |
| G-07 | UI از `APPROVED` و CHECK فیزیکی از `FINAL` برای ارزیابی استفاده می‌کند | کد canonical فیزیکی `FINAL`، نمایش ترجمه‌شده UI؛ نیازمند قرارداد تأییدشده | R3 | OPEN |
| G-08 | `REVERSAL` در نوع تراکنش فیزیکی پیش‌بینی و در کاتالوگ UI مغفول است | پذیرش `REVERSAL` در Domain Contract در صورت تأیید چرخه برگشت | R5 | OPEN |
| G-09 | سرویس واقعی Party/DMS/حساب/مصرف‌کننده/IAM متصل نیست | Ports/Adapters با قرارداد قابل آزمون؛ Dev Stubs جدا از Runtime واقعی | R4/R7 | OPEN |
| G-10 | داده و ضرایب نمایش آفلاین، Policy بانک نیست | عدم انتقال Demo Seed؛ دریافت Policy مصوب و مالک کسب‌وکار | R1 | OPEN |
| G-11 | حداقل تعداد ضامن معتبر مدل نشده | `MIN_VALID_SPONSOR_COUNT` یا Rule Table با Audit و Scope | R3 | OPEN |
| G-12 | تفاوت `HAS_MULTIPLICITY` و `IS_COUNTABLE_FLAG` و اقلام | مدل دو مفهوم مستقل و روابط Item Range | R2 | OPEN |
| G-13 | مجوز استفاده بین‌واحدی در سطح معرفی ذخیره نمی‌شود | ذخیره Permission در Introduction و AND آن با Type + IAM/ORG | R4 | OPEN |
| G-14 | شناسه پایدار دارایی و پیوند معرفی مجدد وجود ندارد | Stable Asset Identifier + Previous Collateral Link بدون بازاستفاده شماره | R5 | OPEN |
| G-15 | Audit before/after تغییر داده مرجع موجود نیست | Reference Change Log با Actor, Reason, Before/After, Correlation | R1 | OPEN |
| **G-16 (جدید)** | مقدار پیش‌فرض `COLLATERAL_TRANSACTION.TRANSACTION_STATUS_CODE='PENDING'` در DDL، ولی Seed فقط `PENDING_APPROVAL` (بدون PENDING) دارد | تعیین canonical code: ترجیح طراحی `PENDING_APPROVAL`، اصلاح کنترل‌شده Default یا Reference بعد از توافق؛ تست FK قبل از تراکنش | R0 decision / R4 gate | OPEN |
| **G-17 (جدید)** | شماره انتظامی UI به شکل type+Jalali year+customer+3digits+check ساخته می‌شود و ممکن است از `NUMBER(19)` فراتر رود؛ JS Number و Long هم محدودیت دارند | تأیید Policy شماره‌گذاری و max length CIF؛ حفظ رشته در API؛ اعتبارسنجی طول/تکرار/Checksum و ظرفیت PK پیش از انتشار؛ اصلاح مدل فقط با Migration | R0 decision / R2 gate | OPEN |
| **G-18 (R3C)** | UI V1.42 از `IS_APPRAISABLE_FLAG` و `REVALUATION_INTERVAL_UNIT` استفاده می‌کند، اما مدل فیزیکی فقط `REVALUATION_INTERVAL_VALUE` دارد | تعیین شمول ارزیابی و واحد روز/ماه/سال، قاعده محاسبه موعد و مدل ذخیره با Migration مصوب؛ `EFFECTIVE_TO` به‌جای سیاست دوره‌ای فرض نشود | R3 | OPEN |

## سایر موارد قابل ردیابی (با حفظ شناسه‌های G-01..G-18)

- **D-01:** تطبیق physical `SEQUENCE` در CLT با `IDENTITY` در DDL مشتق‌شده؛ استفاده از اسکیمای واقعی و Migration، نه نصب مجدد.
- **D-02:** سه اختلاف `VARCHAR2` از نظر BYTE/CHAR؛ بررسی Data Dictionary و `MAX_STRING_SIZE` قبل از Migration.
- **D-03:** یکسانی شمار قیود به معنای برابری کامل عبارت‌ها یا Index Strategy نیست؛ مقایسه read-only از دیکشنری Oracle لازم است.
- **A-01:** جایگاه داده مرجع دارایی/ارزیابی با BIAN v14 `Party Asset Directory` مشخص شود؛ از تکثیر Master Data جلوگیری گردد.
- **S-01:** SecurityFilterChain فعلی IAM فقط `/api/v1/iam/**` را پوشش می‌دهد؛ API Collateral نیازمند حفاظت مستقل یا Security Policy یکپارچه در Integration است.
- **S-02:** هدر `X-User-Id` در بعضی سرویس‌های موجود برای Prototype استفاده می‌شود؛ منبع احراز هویت واقعی Collateral باید Principal معتبر باشد.
- **I-01:** قرارداد مسدودی سپرده DPS2 موجود است، اما اتصال وثیقه، ضمانت Idempotency سراسری و آزادسازی جبرانی باید مستقل تأیید شوند.
- **V-01:** موارد XMI با وضعیت `Proposed` نباید از روی اسم فایل «Approved XMI» تصویب‌شده تلقی شوند.

## تقدم تصمیم‌گیری

- **مانع نوشتن عملیاتی:** G-01/02/03/04/05/06/07/11/12/13/14/15/16/17 حسب مسیر؛ امنیت و Schema تأییدشده برای هر فرمان.
- **مانع راه‌اندازی سیاست‌های واقعی:** G-09/10 و D-01..03 و S-01..02.
- **غیرمانع شروع طراحی/Read-only UI:** می‌توان UX، read models، Contracts و Test Fixtures را با روشن‌بودن برچسب Dev/Mock توسعه داد.


## Checkpoint اجرایی 2026-11-09 — الحاق وضعیت G-01/G-03/G-12/G-14/G-17

این جدول وضعیت تاریخی `OPEN` در ردیف‌های اصلی Gap Register را پاک یا مخدوش نمی‌کند؛ وضعیت زیر «پیشرفت کدنویسی در برابر تصمیم و نصب Oracle» است.

| Gap | وضعیت کد | وضعیت مصوبه/Oracle | وضعیت نهایی |
|---|---|---|---|
| G-01 | گردش درخواست/رأی و Audit کدنویسی شد | Migration و بررسی واقعی باز | OPEN |
| G-03 | ذخیره مستقل و اعتبارسنجی اقلام پیش‌نویس کدنویسی شد | Migration/Backfill باز | OPEN |
| G-12 | `IS_COUNTABLE_FLAG` مستقل از `HAS_MULTIPLICITY` کدنویسی شد | تعیین مقادیر واقعی نوع‌ها باز | OPEN |
| G-14 | مقایسه Stage و preflight نامزد دارایی پیاده‌سازی شد | Source of Truth پایدار و Re-introduction باز | OPEN |
| G-17 | کنترل طول طرح V1.42 با CIF.customerNo و Oracle NUMBER(19) و zero loss پیاده شد | طرح شماره‌گذاری، یکتایی، توالی و مدل فیزیکی باز | OPEN |
| G-16 | تحلیل تعارض Status مستند است | مقدار canonical و Migration باز | OPEN |

هیچ Gap فوق با صرف «نصب موفق سورس» CLOSED اعلام نمی‌شود. **تصمیم‌های کسب‌وکاری و Schema فقط از مسیر Change Control مصوب ثبت می‌شوند.**


## پیشرفت تکمیلی R2 — کنترل تصمیم بازبین (2026-11-09)

| شناسه | توسعه جدید | مانع و وضعیت رسمی |
|---|---|---|
| G-01 | قبل از تصمیم `REVIEWED` تصویر پیش‌نویس/Audit بازخوانی و بررسی می‌شود | Migration اصلاحیه و سیاست مصوب مستقل؛ OPEN |
| G-03/G-12 | انطباق JSON پیش‌نویس با جدول مستقل بازه سریال در تصمیم بازبین و گزارش ثبت قطعی اجباری شد | اجرای Migration و تعیین نوع شمارشی؛ OPEN |
| G-14 | مقایسه جدید Stage با سایر مالکان در لحظه تصمیم و گزارش انجام می‌شود؛ خروجی بدون افشای هویت | مرجع هویت پایدار دارایی و معرفی مجدد؛ OPEN |
| G-17 | گزارش آمادگی با کنترل‌های جدید ایمن‌تر شد؛ شماره‌گذاری هنوز غیرفعال است | تصویب طرح، دقت فیزیکی و یکتایی؛ OPEN |

**نصب موفق سورس یا بازبینی `REVIEWED` به معنی تکمیل هیچ‌یک از Gaps یا مجوز ثبت قطعی نیست.**


## وضعیت R3 / یکپارچه‌سازی DataSource و کارتابل درخواست‌ها (2026-11-09)

| Gap | پیشرفت کدنویسی | مانع باقیمانده | وضعیت |
|---|---|---|---|
| G-04 | اعتبارسنجی نامزد بیمه و فهرست شرکت فعال حفظ شد؛ درخواست Stage روی DS مشترک | سیاست پوشش و پذیرش بیمه‌گر نیازمند تصویب | OPEN |
| G-05/G-11 | ثبت نامزد ضامن و کارتابل Stage، بررسی CIF و منع تکرار حفظ شد | چرخه تأیید/لغو و حداقل ضامنین معتبر مصوب نیست | OPEN |
| G-06 | درخواست مرحله‌ای بیمه و کارتابل؛ تمدید نهایی فعال نیست | قرارداد لغو/جایگزینی و Audit قطعی | OPEN |
| G-07 | درخواست مرحله‌ای ارزیابی و کارتابل؛ سوابق فعلی فقط خواندنی | انتخاب کد وضعیت canonical `FINAL` و قواعد تأیید | OPEN |
| G-09 | جریان‌های R3 از DataSource مشترک پروژه استفاده می‌کنند | صحت قرارداد اتصال واقعی Party/DMS/Consumer در تراکنش نهایی | OPEN |
| G-14/G-17 | هیچ تغییر قطعی در صدور شماره انجام نشد | هویت پایدار دارایی و سیاست شماره انتظامی مصوب نشده‌اند | OPEN |

**مرز تغییر:** سه جدول درخواست Stage (`..._SPONSOR_REQUEST`, `..._INSURANCE_REQUEST`, `..._VALUATION_REQUEST`) در XMI مرجع نیستند. SQLهای موجود صرفاً Candidate/Fail-Closed هستند و تا اجرای Migration مصوب، ثبت آنها فعال نیست. نصب موفق سورس، تصویب DBA یا تأیید زنده Oracle نیست.

## R3-HF1 — اصلاح سازگاری فایل فرم بیمه در نصب (2026-11-09)

- خطای `SOURCE_CHANGED_OR_UNKNOWN` در HTML فرم بیمه یک **تعارض نصب سورس** بود، نه تأیید/رد بانک درباره G-04 یا G-06.
- HF1 فقط قطعه کارتابل Stage را با الحاق محافظت‌شده به فایل موجود اضافه می‌کند؛ بدون پاک‌کردن تغییرات تأییدنشده یا افزوده‌های سایت/لوگو.
- هیچ Gap کسب‌وکاری بسته نشده است؛ **G-04/G-05/G-06/G-07/G-09/G-11 و G-14/G-17 OPEN** می‌مانند. Stage همچنان نیازمند جداول و مجوزهای داده است؛ UAT به پایان توسعه موکول است.

## R3 HF2 installer compatibility checkpoint (2026-11-09)

- HF1 failed precheck because the installed sponsor and valuation HTML differed from the package's hash allowlist. The program files were not changed.
- HF2 adds protected append-only Stage-worklist insertion to the sponsor and valuation forms, alongside the pre-existing safe merge for the insurer form. No unrelated form content may be overwritten.
- This packaging correction closes **no business gap**. G-04/G-05/G-06/G-07/G-09/G-11/G-14/G-17 stay OPEN; R3 staging Oracle prerequisites and final approval workflows remain pending.

## R3 Stage Workflow — پیشرفت سورس 2026-11-09

- G-05/G-11: Maker Submit/Withdraw و Reviewer Screening/Reject در Stage ضامن پیاده‌سازی شد؛ سیاست رسمی حداقل ضامن، تأیید/لغو قطعی و انتقال به جدول عملیاتی همچنان **OPEN**.
- G-04/G-06: همین گردش Stage برای بیمه‌نامه، بدون ایجاد/تمدید/لغو بیمه مؤثر؛ شرایط اجباری، دامنه بیمه و بیمه‌گر مصوب **OPEN**.
- G-07/G-18: همین گردش Stage برای ارزیابی؛ کد قطعی `FINAL`، تأیید ارزش و تناوب انقضا **OPEN**.
- G-09: مسیرهای درخواست محلی در Security Chain مشترک مجاز و Backend محدود به Stage؛ اتصال/احراز Enterprise و قراردادهای مصرف‌کننده همچنان **OPEN**.
- DATA: SQL تغییر Status Check و Event Reason فقط کاندید Fail-Closed و خارج از مدل EA مصوب است. هیچ DDL خودکار یا داده قطعی ایجاد نمی‌شود.
- آخرین خط مبنای واقعی سورس: `core-banking-prototype-v0-2026-11-09-1618.zip`؛ تأیید نصب HF2 توسط کاربر. جزئیات [Checkpoint](CLT-R3-STAGE-WORKFLOW-CHECKPOINT-2026-11-09.md).


## R3 REVIEW REVALIDATION — checkpoint 2026-11-09 (Baseline 1720)

- G-04/G-06: کنترل نامزد بیمه‌گر، نوع پوشش، دوره و پیوند تمدید در لحظه `REVIEW` مجدداً اجرا می‌شود؛ مصوبه پوشش/لغو/تأیید بیمه همچنان **OPEN** است.
- G-05/G-11: وضعیت ضامن، مالک، تکرار و CIF برای Reviewer مستقل دوباره استعلام می‌شود؛ چرخه تأیید/لغو و حداقل تعداد ضامنین **OPEN** است.
- G-07/G-18: مرجع و اشاره‌گر ارزیابی جاری در لحظه بازبینی مجدداً بررسی می‌شود؛ وضعیت `FINAL` قطعی و قاعده تناوب/اعتبار **OPEN** است.
- G-09: وابستگی به CIF/Oracle هنگام `REVIEW` به‌صورت Fail-closed باقی است. امنیت Enterprise و اتصال عملیاتی کامل **OPEN** است.
- موفقیت نصب سورس به معنی اجرا/تأیید Migration، اوراکل زنده، UAT یا بسته‌شدن شکاف نیست. جزئیات: [Checkpoint](CLT-R3-REVIEW-REVALIDATION-CHECKPOINT-2026-11-09.md).


## R3 Lifecycle Evidence — وضعیت توسعه 2026-11-09

**نصب موفق پیش‌نیاز:** `CLT-R3-REVIEW-REVALIDATION-2026-11-09` (اعلام کاربر). کد خواندنی جدید `CLT-R3-LIFECYCLE-EVIDENCE-2026-11-09` داده‌های زیر را بدون استنتاج اعتبار قطعی منتشر می‌کند:

| Gap | شواهد کدنویسی‌شده | مانع و وضعیت رسمی |
|---|---|---|
| G-04/G-06 | پرچم INSURABLE و تعداد تمام بیمه‌نامه‌های ثبت‌شده از Oracle | پوشش الزام‌آور، وضعیت تأیید/لغو و Renewal Audit همچنان **OPEN** |
| G-05/G-11 | پرچم SPONSOR_REQUIRED و تعداد تمام ضامن‌های ثبت‌شده از Oracle | حداقل ضامنین معتبر و گردش تأیید/لغو همچنان **OPEN** |
| G-07 | وجود ارزیابی جاری، وضعیت فیزیکی FINAL و مقدارهای Effective | قواعد تأیید و Actor/Role مصوب **OPEN** |
| G-18 | نمایش تنها دوره Effective ثبت‌شده، بدون ساخت سررسید تجدید ارزیابی | Appraisability، تناوب/واحد و Business Date بانک **OPEN** |
| G-09 | API خواندنی مشترک با branch-scope، بدون اتصال خارجی جدید | Party/Insurer/FX و گیت عملیاتی **OPEN** |

**ممنوعیت استنتاج:** `recordedRows` برای ضامن یا بیمه معادل `validCount` نیست؛ وضعیت `FINAL` یا `WITHIN_RECORDED_EFFECTIVE_PERIOD` تأیید تخصیص نیست. هیچ Migration، DML روی جدول قطعی یا Feature Flag فعال نشده است. مسیر فازها بدون تغییر می‌ماند.


## R3 Stage-to-Domain Posting Preflight — کنترل پیش از ثبت قطعی (2026-11-09)

- برای **G-05/G-11** ثبت ضامن واقعی در Stage `REVIEWED`، Audit و Fingerprint و نبود تکرار جدول اصلی قابل بررسی شد؛ اما مدل لغو/تأیید و حداقل معتبر **OPEN** است.
- برای **G-04/G-06** کنترل یکتایی بیمه‌گر/شماره و رویداد بازبین Stage اضافه شد؛ سیاست پوشش، تأیید/تمدید/لغو اصلی **OPEN** است.
- برای **G-07/G-18** تطبیق اشاره‌گر ارزیابی با Supersedes و احتمال تکرار Reference اضافه شد؛ سیاست FINAL/تجدید ارزیابی/Business Date **OPEN** است.
- برای **G-09** خواندن شواهد در یک SELECT از DataSource مشترک و Branch Principal انجام می‌شود؛ Integration و نقش‌های ثبت قطعی **OPEN** هستند.
- تأیید کاربر صرفاً **نصب موفق Lifecycle Evidence** است. Preflight جدید هنوز روی Oracle بانک/UAT تأیید نشده و `policyApproved=false` / `finalPostingEligible=false` باقی می‌ماند.
- تصمیم‌های لازم، مالکیت و کنترل اتمیک در [CLT-R3-POSTING-DECISION-MATRIX-FA.md](CLT-R3-POSTING-DECISION-MATRIX-FA.md) ثبت شدند؛ هیچ تصمیم به جای بانک تصویب یا هیچ Migration خودکار اجرا نشده است.


## D-04 و تکمیل R3 Sponsor Commit Candidate — 2026-11-09

- **D-04 (جدید، Technical/Data):** مطابق `Collateral_DB_Specification_FA(2).html`، مدل `CLT.COLLATERAL_SPONSOR` Unique مرکب روی `(COLLATERAL_ID,PARTY_ID)` ندارد. در R3 الگوریتم منع تکرار در Application وجود داشت، اما بدون Constraint فیزیکی چند Writer دیگر می‌توانند داده تکراری بسازند. تصمیم و پاک‌سازی داده با DBA/مالک داده باز است. `R3SponsorCommitService.schemaReady()` نبود `UQ_CLT_SPONSOR_COLL_PARTY` معتبر را رد می‌کند. **وضعیت OPEN**.
- **G-05/G-11/G-09:** مسیر کد ثبت رابطه ضامن با `REVIEWED`, Revalidation و Oracle Ledger آماده است اما Approval واقعی و Migration اجرا نشده؛ وضعیت رسمی هر سه **OPEN**. ثبت رابطه ضامن برابر اعتبار ضامن برای تخصیص، گردش ابطال یا حداقل ضامن معتبر نیست.
- **G-04/G-06/G-07/G-18:** هیچ Domain DML جدید برای بیمه یا ارزیابی فعال/ایجاد نشده؛ وضعیت **OPEN**.
- تغییر شناسه، Gate یا نام R0..R7 انجام نشده است. مستند گام جدید: `CLT-R3-SPONSOR-ATOMIC-COMMIT-CHECKPOINT-2026-11-09.md`.


## D-05 / R3 Insurance Atomic Create–Renew Candidate — 2026-11-09

| Gap | پیشرفت در کد | شرط باقیمانده | وضعیت |
|---|---|---|---|
| G-04 | کنترل Rule مصوب `COLLATERAL_TYPE_ID × INSURANCE_TYPE_ID × CURRENCY_CODE × POLICY_REVISION` و حداقل مبلغ، در لحظه Commit | تعریف مجموعه پوشش‌های الزامی و کفایت برای Allocation در سطح بانک و تأیید Ruleهای واقعی | OPEN |
| G-06 | مسیر CREATE/RENEW و حفظ ارتباط `PREVIOUS_INSURANCE_ID` با Replay و Audit تراکنش | چرخه Cancel/Revoke و اثر آن بر تخصیص، سوابق Append-only، قواعد جایگزینی | OPEN |
| G-09 | اتصال Commit به Shared DataSource و الزام Release Integration و کنترل CIF/مالک در Revalidation | قرارداد و آزمون اعتبارسنجی بیمه‌گر خارجی | OPEN |
| D-05 (جدید، فیزیکی/DBA) | قفل وثیقه و بررسی جانشین قبلی در سرویس وجود دارد | `COLLATERAL_INSURANCE.PREVIOUS_INSURANCE_ID` در مدل EA فقط Index غیریکتا دارد؛ برای جلوگیری از Race چند Writer، پس از تطبیق داده‌ها و تایید DBA قید Unique ضروری است | OPEN |

**محدودیت:** در نبود Release مصوب، Schema Ledger/Rule و دو Unique معتبر Oracle، `insurance-enabled=false` و عملیات ثبت قطعی متوقف است. هیچ Gap با نصب سورس بسته اعلام نمی‌شود. **R3 ادامه دارد؛ R4 آغاز نشده است.**


## D-06 / R3 Valuation Atomic Finalization Candidate — 2026-11-09

| شناسه | پیشرفت در کد | کنترل بانک/DBA باقی‌مانده | وضعیت |
|---|---|---|---|
| G-07 | کد گذار Stage REVIEWED به History FINAL و تغییر قبلی به SUPERSEDED با Audit/Receipt | نگاشت مصوب APPROVED/UI به FINAL/Oracle و سیاست نقش تأییدکننده | OPEN |
| G-18 | Rule مصوب نوع وثیقه برای Appraisable و `DAY/MONTH/YEAR`، Anchor و EffectiveTo؛ بدون مقدار پیش‌فرض | اصلاح یا تصویب محل ذخیره، مرز تاریخ و Business Date بانک | OPEN |
| G-09 | کنترل وجود Rule صلاحیت ارزیاب و Currency برابر وثیقه؛ حالت نامعتبر Fail-Closed | اعتبارسنجی حرفه‌ای/سرویس بیرونی ارزیاب و FX | OPEN |
| G-10 | منع Value منفی/صفر و ارزش فرعی بزرگتر از Amount؛ بدون تولید خودکار مقدار | سیاست Haircut/EligibleValue رسمی برای تخصیص | OPEN |
| D-06 (فنی/DBA) | در Service موجودیت Identity، Unique دوگانه sequence/supersedes، FK مرکب و CAS بررسی می‌شوند؛ SQL تشخیص فقط خواندنی است | تطبیق DB فیزیکی واقعی و داده‌های گذشته با مشخصات `Collateral_DB_Specification_FA(2).html` و امضای Migration/Recovery پیش از فعال‌سازی | OPEN |

**محدودیت:** این نسخه اثبات اتصال زنده Oracle نیست و Feature Flag ارزیابی خاموش می‌ماند. جداول Release/Rule/Ledger فقط کاندید هستند؛ هیچ بانک/DBA Approval جعل یا هیچ Gap بسته نشده است.

## 2026-11-10 — G-06 Cancellation implementation checkpoint (CLT R3)

| شناسه | تغییر و شاهد در سورس | مانع باز | وضعیت |
|---|---|---|---|
| G-06 | پیش‌بررسی شعبه‌محور، درخواست Stage با Maker/Reviewer/Audit، Event نامزد Append-only، Receipt و UI مستقل؛ حذف یا ویرایش بیمه‌نامه تاریخی وجود ندارد | تصویب قرارداد لغو/زمان اثر، سوابق و اثر بر تعهد و پوشش، ترمینال موفق Oracle | **OPEN** |
| G-04 | کنترل محافظه‌کارانه شرط پوشش جایگزین هم‌نوع/هم‌ارز در نامزد Posting | سیاست مجموعه پوشش‌های الزامی، حدود و پذیرش بیمه‌گر مصوب نیست | **OPEN** |
| G-09 | شرط Decision Ref برای R4 و Integration و قرارداد Consumer در Release نامزد | پیاده‌سازی/پذیرش عملیاتی R4 و شرکت بیمه و روال ارزیابی اثر خارج از این Patch | **OPEN** |
| D-07 | جداول Stage/Impact/Release/Event و Triggerهای Append-only به‌صورت SQL Candidate؛ گیت سخت‌گیرانه Dictionary در Backend | مهاجرت امضاشده DBA، تطبیق اوراکل زنده، آزمون هم‌زمانی و تفکیک دسترسی | **OPEN** |

**گیت مهم:** هر ردیف در `CLT.COLLATERAL_ALLOCATION` به‌صورت Fail Closed مانع Posting لغو است؛ استفاده از `USED='NO'` به‌عنوان مجوز حذف مسئولیت صحیح نیست. حتی در نبود ردیف تخصیص، نیاز به مصوبه رسمی کفایت جایگزین و تأیید مستقل باقی می‌ماند.


## Checkpoint 2026-11-10 — R4/R5 staged analyst requests + stable domain naming

| Gap / boundary | Evidence in this delivery | Bank / Oracle dependency | State |
|---|---|---|---|
| G-02 | Persistent analyst request idempotency in the already documented R4C **candidate staging** model; no financial allocation | Consumer-authenticated obligations, durable final register and Oracle capacity lock | **OPEN** |
| G-09/G-13 | Local Stage branch binding checked by existing application principal; no external obligation attestations | Approved consumer contract and cross-branch introduction scope | **OPEN** |
| G-04/G-05/G-07/G-10/G-18 | Existing sponsor/insurance/valuation policies and evidence preserved | Bank rules, valuation, insurance coverage, Oracle final activation | **OPEN** |
| G-16 | No new write to `COLLATERAL_TRANSACTION` | Reconcile default `PENDING` vs status master | **OPEN** |
| R5 G-19..G-22 (as referenced in phase-status records) | Local R5B Stage request and withdrawal with If-Match; no financial impact | Approved module mapping, domain-owner instruction, final unallocation and release semantics | **OPEN — verify exact official gap indexing** |

**No gap closure:** `CLT_LOCAL_ANALYST` is an internally created local Stage actor, not external consumer attestation. Phase exit remains governed independently of source progress; [checkpoint](CLT-USECASE-DELIVERY-CHECKPOINT-2026-11-10.md).


## BUC-12 — معرفی مجدد دارایی آزادشده، 2026-11-10

| شکاف | پیشرفت کد | شرط باقی‌مانده | وضعیت |
|---|---|---|---|
| G-14 | هویت پایدار، تعارض شناسنامه جاری و زنجیره نسخه‌های تاریخی در ثبت اتمیک لحاظ شده است | تأیید Canonical Asset Identity، Backfill و Unique Oracle | **OPEN** |
| G-17 | شماره جدید از Inventory/Permit مستقل بانک مصرف می‌شود؛ عدم استفاده مجدد بررسی می‌شود | تصویب مرجع صدور، طرح شماره‌گذاری و Migration | **OPEN** |
| G-12 | اعتبارسنجی سریال‌ها، مبلغ، نوع شمارشی و ذخیره اقلام روی شناسنامه جدید | مصوبه IS_COUNTABLE_FLAG و جدول فیزیکی اقلام | **OPEN** |
| BUC-11/R5 | ثبت جدید فقط با شاهد Permit POSTED آزادسازی قبلی و تاریخچه نهایی امکان‌پذیر است | فعال‌سازی و آزمون موفق BUC-11 بر Oracle زنده | **OPEN** |

هیچ Gap با صرف نصب سورس بسته و بدون Approval و آزمون واقعی بسته اعلام نمی‌شود.
