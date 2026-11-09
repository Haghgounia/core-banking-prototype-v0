# دفتر شکاف‌ها و تصمیمات Collateral — CLT-GAPS-R1.0

**تاریخ:** 2026-11-03  
**وضعیت:** Approved as issue register؛ تصمیم‌های ساختاری با برچسب OPEN هنوز مصوب نشده‌اند.  
**منابع:** شکاف‌های G-01..G-15 عیناً از DB/API/UI/System/Service Specification پیوست استخراج شده‌اند. G-16 و G-17 حاصل تطبیق مستقل مدارک و کد شبیه‌ساز هستند.

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

## سایر موارد قابل ردیابی (بدون تغییر شناسه G-01..G-17)

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
