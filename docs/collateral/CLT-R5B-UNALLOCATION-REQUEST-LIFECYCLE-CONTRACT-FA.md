# CLT R5B — قرارداد چرخه پایدار درخواست رفع تخصیص وثیقه

**نسخه:** R5B.1.0 | **تاریخ:** ۱۴۰۵/۰۸/۱۳ (2026-11-04) | **نقشه راه:** `CLT-ROADMAP-R1.0` | **پیش‌نیاز:** R5A روی Prototype 0.11.0

## ۱. تصمیم معماری و مرز دامنه

R5B فقط **درخواست رفع تخصیص** را در دفتر مرحله‌ای مستقل و دارای Audit ثبت می‌کند. هیچ سطر `CLT.COLLATERAL_ALLOCATION`، `CLT.COLLATERAL_TRANSACTION`، `CLT.COLLATERAL` یا DPS2 اصلاح نمی‌شود. دو مفهوم باید تفکیک شوند:

- **Unallocation Request Lifecycle:** دریافت، Replay، مشاهده و لغو خودِ درخواست؛ در این مرحله پیاده‌سازی شده اما به‌صورت **Default OFF**.
- **Financial Unallocation / Final Collateral Release:** رفع تعهد، تغییر `USED`، کاهش تعهد، ثبت Ledger و آزادسازی نهایی وثیقه؛ **پیاده‌سازی یا فعال نشده**.

در این بسته هیچ ادعای «تخصیص آزاد شد» یا «ظرفیت افزایش یافت» وجود ندارد. عملیات مالی بدون تأیید نهاد ذی‌صلاح بانک و آزمون Oracle ممنوع است. G-19 و G-20 باز باقی می‌مانند.

## ۲. وضعیت‌های چرخه درخواست

| وضعیت | رویداد | معنا | اثر مالی |
|---|---|---|---|
| `RECORDED_NOT_UNALLOCATED` | `REQUEST_RECORDED` | فقط درخواست سرویس معتبر ثبت شده است | ندارد |
| `CANCELLED_NOT_UNALLOCATED` | `REQUEST_CANCELLED` | درخواست مرحله‌ای با CAS و Audit لغو شده است | ندارد |

حالت دوم نهایی است و بازگشت به حالت اول ندارد. پیاده‌سازی `APPROVED`، `POSTED`، `PARTIAL_UNALLOCATED` یا `RELEASED` عمداً در R5B انجام نشده است.

## ۳. APIها

| روش و مسیر | مجوز | اثر |
|---|---|---|
| `GET /api/v1/collaterals/{id}/unallocation-requests/readiness` | `SCOPE_clt.collateral.read` | فقط وضعیت قابلیت |
| `GET /api/v1/collaterals/{id}/unallocation-requests/journal?limit=20` | READ + فعال بودن دفتر | فقط سوابق شعبه مجاز |
| `POST /api/v1/collaterals/{id}/allocations/{allocationId}/unallocation/requests` | `SCOPE_clt.unallocation.request`، JWT سرویس، نگاشت Client/Domain و MODULE_ID، قابلیت مرحله‌ای فعال | ثبت مرحله‌ای در دو جدول |
| `GET /api/v1/collaterals/{id}/allocations/{allocationId}/unallocation/requests/{requestId}?consumerDomain=LOAN` | همان Scope سرویس، Client و مالکیت | Receipt همان درخواست |
| `POST /api/v1/collaterals/{id}/allocations/{allocationId}/unallocation/requests/{requestId}/cancel` | همان Scope سرویس، Client و مالکیت | فقط لغو درخواست مرحله‌ای |

ثبت جدید نیازمند `Idempotency-Key` با UUID استاندارد و `If-Match: "<RECORD_VERSION>"` (نسخه تخصیص) است. بدنه همان `PreviewRequest` در R5A: `consumerDomain`, `fileNumber`, `requestedAmount`, `currencyCode`, `releaseMode` (`FULL` یا `PARTIAL`), `reasonReference`.

لغو درخواست نیازمند `Idempotency-Key` جدید، `If-Match: "<RECORD_VERSION_OF_REQUEST>"` و بدنه `{ "consumerDomain":"LOAN", "reasonReference":"SERVICE_CANCEL_REF" }` است؛ برخلاف ثبت، ETag به **نسخه دفتر مرحله‌ای** مربوط است.

## ۴. منطق امنیت و اعتبارسنجی

۱. مجوز JWT، grant `client_credentials`، `client_id` امضاشده و نگاشت سروری Client به دامین قبل از دسترسی به Oracle بررسی می‌شوند. بدون نگاشت `consumerDomain → MODULE_ID` مجوز داده نمی‌شود.

۲. وثیقه و تخصیص با شعبه استخراج‌شده از JWT و رابطه والد، نه اطلاعات قابل دست‌کاری کاربر، خوانده می‌شوند. برای خدمت متعلق به دامین دیگر، همان 404 داده می‌شود.

۳. فعال بودن تخصیص، `RECORD_VERSION`، شماره پرونده، ارز، مبلغ و سازگاری `FULL/PARTIAL` کنترل می‌شوند. `FULL` باید دقیقاً برابر مانده مبلغ ثبت‌شده تخصیص باشد. `PARTIAL` باید مثبت و کمتر از مبلغ تخصیص باشد؛ حتی در این صورت سیاست عملیات جزئی G-20 تصویب نشده و فقط درخواست مرحله‌ای ذخیره می‌شود.

۴. قواعد R5A به‌صورت بلوکه‌کننده ثبت و همراه Receipt منعکس می‌شوند. تأیید دامین تعهد (`G-09`)، مجوز شعبه (`G-13`) و سیاست Ledger (`G-20`) همچنان برقرار نیست. هیچ تأیید مثبت تخصیص مالی صادر نمی‌شود.

۵. مرورگر Angular فقط `readiness` و `journal` را فراخوانی می‌کند. **ثبت، دریافت رسید اختصاصی سرویس و لغو درخواست از UI قابل فراخوانی نیستند.**

## ۵. Idempotency، هم‌زمانی و Audit

- کلید یکتا: `(CONSUMER_DOMAIN, CLIENT_ID, IDEMPOTENCY_KEY)` در `CLT_UNALLOC_REQ`؛ این کلید از شعبه مستقل است تا کلید مشابه در شعبه دیگر نیز نتواند درخواست دیگری بسازد.
- Digest: SHA-256 با طول‌گذاری صریح هر فیلد؛ شامل وثیقه، تخصیص، شعبه، شناسه Client و تمام داده‌های نرمال‌شده درخواست و نسخه تخصیص است؛ خود Idempotency-Key در Digest نیست.
- درخواست اول: در یک تراکنش JDBC، سطر درخواست و رویداد `REQUEST_RECORDED` ایجاد و Receipt خوانده می‌شود. خطا باید هر دو را Rollback کند.
- Replay یک کلید با Digest یکسان: نتیجه قبلی بدون سطر یا رویداد جدید برمی‌گردد. کلید یکسان و محتوای متفاوت: `409 Conflict`. برخورد هم‌زمان کلید یکتا پس از Rollback با بازخوانی معامله دوم حل می‌شود.
- لغو: سطر درخواست با `SELECT ... FOR UPDATE WAIT 1` قفل می‌شود؛ تطبیق `If-Match` قوی، وضعیت قابل لغو و مالکیت خدمت الزامی است. `UPDATE ... WHERE RECORD_VERSION=?` و `REQUEST_CANCELLED` در یک تراکنش رخ می‌دهند. کلید اختصاصی لغو در `(REQUEST_ID, EVENT_CODE, ACTION_KEY)` یکتا است؛ Replay آن، تنها همان Cancel قبلی را بازمی‌گرداند.
- **محدودیت:** این قفل فقط دفتر درخواست را محافظت می‌کند؛ قفل و ظرفیت واقعی وثیقه/دامین مصرف‌کننده در این تراکنش نیست. اتصال خواندنی R5A از Pool مستقل انجام می‌شود و اثبات یک Snapshot مالی سراسری نیست.

## ۶. مدل داده Oracle / DBA

دو جدول **کاندید و هنوز تصویب‌نشده**: `CLT.CLT_UNALLOC_REQ` (درخواست‌ها) و `CLT.CLT_UNALLOC_REQ_EVENT` (رویدادهای ثبت و لغو). دو Sequence کوتاه‌نام و قیود Unique/FK/Check در `database/oracle/clt/r5b/01_UNALLOCATION_STAGING_CANDIDATE_NOT_APPROVED.sql` آمده‌اند؛ فایل در ابتدا با `RAISE_APPLICATION_ERROR(-20955)` متوقف می‌شود و تمام DDLها صرفاً کامنت هستند. Installer هیچ فایل SQL را اجرا نمی‌کند.

شرط فعال‌سازی مرحله‌ای با مصوبه جداگانه معماری، امنیت و DBA: ایجاد جداول و قیود *در محیط مناسب*، تأیید حداقل مجوزها برای حساب Writer غیر از `CLT/SYS/SYSTEM`، و تنظیم صریح:

```properties
CLT_UNALLOCATION_INTAKE_WRITES_ENABLED=true
CLT_UNALLOCATION_INTAKE_WRITER_JDBC_URL=jdbc:oracle:thin:@...
CLT_UNALLOCATION_INTAKE_WRITER_JDBC_USERNAME=<staging-only-user>
CLT_UNALLOCATION_INTAKE_WRITER_JDBC_PASSWORD=<secret-in-approved-vault>
```

**این متغیرها در بسته و سیستم نصب‌شده فعال نشده‌اند.** برقراری این شرایط نیز مجوز رفع تخصیص مالی نیست. هیچ رمز یا تنظیم تولیدی در Repository نگهداری نمی‌شود.

`requireSchema` وجود ۲۱ ستون درخواست، ۹ ستون رویداد، قیود فعال/معتبر و ترتیب دقیق ستون‌های کلیدهای یکتا را کنترل می‌کند. این کنترل جای آزمون واقعی دو Session Oracle یا بازبینی کامل قیود DBA نیست.

## ۷. مدل رابط کاربری

پنل موجود R5A بدون تغییر Route اصلی گسترش یافته است: نمایش روشن یا خاموش بودن دفتر، وضعیت ایجاد جداول، نمایش درخواست‌ها با شناسه تخصیص/دامین/مبلغ/وضعیت و اخطار «هیچ اثر مالی ندارد». هیچ Button برای ثبت یا لغو درخواست افزوده نشده است.

## ۸. گیت‌های باز پیش از توسعه عملیات مالی

- `G-09`: دامین صادرکننده تعهد باید بدهی، مبلغ قابل رفع، همه بدهکاران و صلاحیت ابطال را **اتست کند**.
- `G-13`: مجوز شعبه و عملیات بین‌شعبه‌ای و مسئولیت دامین صادرکننده باید مصوب شود.
- `G-19`: نگاشت رسمی `consumerDomain` به `CLT.MODULE_ID` و شناسه سرویس مرتبط لازم است.
- `G-20`: تعریف مدل کامل/جزئی، Ledger، Reversal، Audit و رفتار تراکنشی مالی لازم است.
- R4: تخصیص مالی اتمیک و آزمون رقابت واقعی Oracle هنوز اجرا نشده‌اند.
- R5C: آزادسازی نهایی وثیقه تنها پس از احراز نبود همه تعهدات/تخصیص‌های فعال ممکن است؛ در R5B پیاده نشده است.

## ۹. شواهد QA و محدودیت صریح

آزمون قواعد مستقل JDK، نصب/نصب مجدد، کنترل هش و محدوده فایل، تست تعارض و بررسی نحوی انجام می‌شود. **Build واقعی Spring Boot 4.1/Angular 21 و اجرای Oracle/JWT واقعی تا دریافت خروجی محیط Windows کاربر تأییدشده نیست.** نقشه راه R0 تا R7 و سایر ماژول‌ها دست‌نخورده باقی می‌مانند.
