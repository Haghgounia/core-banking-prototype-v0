# CLT-R2D | گردش بازبینی مستقل پیش‌نویس معرفی وثیقه

**خط مبنا:** `CLT-ROADMAP-R1.0`؛ فاز `R2` («شناسنامه، معرفی و جست‌وجو»)، برش داخلی R2D. **نسخه محصول:** `0.11.0`.

## هدف و مرز

پس از تحویل R2A/R2B/R2C، یک کارتابل محدود، قابل ردیابی و **غیرقطعی** برای بازبینی پیش‌نویس معرفی وثیقه افزوده شده است. هیچ وضعیت `REVIEWED` یا `RETURNED` به معنی ایجاد وثیقه، تأیید ارزش، تخصیص، صدور شماره انتظامی، یا رفع G-17 نیست. ۲۶ جدول اصلی EA و `CLT.COLLATERAL` بدون تغییر باقی می‌مانند.

## جریان کاربردی

1. کاربر با scope `clt.collateral.introduce` و `branch_code` معتبر، پیش‌نویس R2B را (در صورت فعال‌بودن اختیاری DBA) ثبت یا بازیابی می‌کند.
2. `POST /api/v1/collaterals/introduction-drafts/{draftId}/submit-review` با `Idempotency-Key`، تنها برای سازنده همان پیش‌نویس و شعبه خودش قابل اجرا است. هر پیش‌نویس دقیقاً یک پرونده بازبینی دارد. درخواست‌های تکراری با همان کلید، همان پاسخ را می‌دهند.
3. بازبین دارای scope **مستقل** `clt.collateral.review`، کارتابل شعبه را می‌بیند؛ محتوا را از `GET /api/v1/collaterals/introduction-reviews/{reviewId}/draft` دریافت و اثرانگشت snapshot را بررسی می‌کند.
4. با `POST /api/v1/collaterals/introduction-reviews/{reviewId}/decision` یک نتیجه `REVIEWED` یا `RETURNED`، دلیل ۱۰ تا ۵۰۰ نویسه، `expectedVersion='1'` و `draftHash` ثبت می‌شود. ثبت‌کننده نمی‌تواند بازبین همان درخواست باشد؛ تصمیم دوم، تغییر شعبه یا نسخه کهنه رد می‌شود. تکرار دقیق همان تصمیم با کلید یکسان پاسخ قبلی را برمی‌گرداند.
5. وضعیت درخواست در دو فهرست «درخواست‌های من» و «کارتابل بازبین شعبه» قابل مشاهده است. تعداد هر پاسخ حداکثر ۱۰۰ رکورد است؛ صفحه‌بندی سروری در برش تکمیلی لازم خواهد بود.
6. اگر نتیجه `RETURNED` شد، پیش‌نویس اصلی **ویرایش نمی‌شود**. ایجاد پیش‌نویس جدید/سابقه اصلاحیه معتبر برای فرایند قطعی، ذیل G-01 هنوز باز است.

## قراردادهای API

| متد | مسیر | scope | عملیات |
|---|---|---|---|
| GET | `/api/v1/collaterals/introduction-reviews/readiness` | `clt.collateral.read` | وضعیت Feature Gate و Schema، بدون ادعای فعال‌بودن ثبت قطعی |
| POST | `/api/v1/collaterals/introduction-drafts/{draftId}/submit-review` | `clt.collateral.introduce` | ایجاد درخواست بررسی مستقل با Idempotency |
| GET | `/api/v1/collaterals/introduction-reviews/mine` | `clt.collateral.introduce` | ۱۰۰ درخواست آخر سازنده همان شعبه |
| GET | `/api/v1/collaterals/introduction-reviews/pending` | `clt.collateral.review` | ۱۰۰ درخواست در انتظار شعبه JWT |
| GET | `/api/v1/collaterals/introduction-reviews/{reviewId}/draft` | `clt.collateral.review` | محتوای واقعی پیش‌نویس با تطبیق شاخه و hash |
| POST | `/api/v1/collaterals/introduction-reviews/{reviewId}/decision` | `clt.collateral.review` | تصمیم اتمیک و Audit با محافظت CAS/Idempotency |

تمام مسیرهای R2D علاوه بر scope به Gateهای `draftWritesEnabled` و `reviewWorkflowEnabled` وابسته‌اند؛ مسیر readiness فقط scope خواندن دارد. خطای ساختار/مجوز Oracle Fail Closed است. اعتبار JWT صرفاً در زنجیره جداگانه Collateral صورت می‌گیرد؛ IAM دست‌نخورده است.

## مدل پیشنهادی داده و مرز مهاجرت

`database/oracle/clt/r2d/00_REVIEW_READONLY_PRECHECK.sql`: قابل اجرای خواندنی توسط DBA.

`database/oracle/clt/r2d/01_REVIEW_WORKFLOW_DBA_OPT_IN.sql`: دو جدول **پیشنهادی و تصویب‌نشده**، `CLT_INTRODUCTION_REVIEW` و `CLT_INTRODUCTION_REVIEW_EVENT`، همراه دو Sequence. هیچ DDL از Installer اجرا نمی‌شود. مجوزهای `SELECT/INSERT/UPDATE` فقط روی جدول درخواست بازبینی، `SELECT/INSERT` روی جدول رویداد و `SELECT` Sequence، مشروط به تصویب DBA است. هیچ Grant بر جدول اصلی وثیقه پیشنهاد نمی‌شود. Oracle DDL در صورت شکست میان راه خودکار Rollback کامل ندارد و نیازمند طرح بازگشت DBA است.

**Gateهای مستقل و خاموش به‌صورت پیش‌فرض**:

```
CLT_INTRODUCTION_DRAFT_WRITES_ENABLED=false
CLT_INTRODUCTION_REVIEW_WORKFLOW_ENABLED=false
```

فقط بعد از تصویب قرارداد API، امنیت، Migration، مجوز حساب و تست Integration می‌توان هر دو را در محیط مجاز فعال کرد؛ اولی بدون دومی نیز مسیر قدیمی پیش‌نویس را حفظ می‌کند.

## نیازمندی باز باقی‌مانده

- G-01: ذخیره اصلاحیه شناسنامه قطعی و گردش آن مستقل از کارتابل موقت.
- G-03/G-12: مدل نهایی اقلام شمارشی و تفکیک تکثیرپذیر از شمارشی.
- G-14: شناسه پایدار دارایی و ارتباط معرفی مجدد پس از آزادسازی.
- G-17: شماره انتظامی، محدودیت ۱۹رقمی Oracle و تولید اتمیک شماره در DB.
- G-16: سازگاری `TRANSACTION_STATUS_CODE` پیش‌فرض و Seed.
- R1: تکمیل Audit اطلاعات مرجع و Maker/Checker عملیاتی طبق برنامه.
- تست‌های ساخت عملیاتی Spring/Angular، JWT، DBA و UAT مستقل از تست سورس هستند.

## ملاحظات فنی

- تغییرات فقط در `collateral` و `clt/introduction.properties` است؛ هیچ Merge در Sidebar/Routes/POM/Beanهای مشترک ندارد.
- به سبب خطای CGLIB مشاهده‌شده قبلی، فایل‌های اصلاح‌شده سه Repository و تست regression از HF1 همراه همین بسته هستند؛ چه HF1 نصب شده باشد چه نصب نشده باشد، به‌شرط مطابقت دقیق SHA شناخته‌شده، وضعیت یکسان خواهد شد.
- ساختار اطلاعات اصلی شناسنامه و شماره انتظامی از کارتابل تغییر نمی‌کند؛ تصمیم بازبین صرفاً یک رویداد مرحله‌ای است.
- معیار خروج R2D: نصب بدون تعارض، Build/JAR، آزمون بستن دسترسی و تغییر نقش، تأیید DBA، و UAT عملیاتی؛ فقط دو مورد اول در محیط ویندوز کاربر قابل اثبات‌اند.
