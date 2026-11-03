# CLT-R2B — معرفی وثیقه: اعتبارسنجی و پیش‌نویس کنترل‌شده

تاریخ: 2026-11-03 | Baseline: Core Banking Prototype 0.11.0 + CLT-R1B + CLT-R2A  
مرجع نقشه راه: **CLT-ROADMAP-R1.0**، فاز R2 (بدون تغییر نام/ترتیب R0 تا R7)  
مرجع رفتاری: `Collateral_UI_Final_Offline_V1.42_FA.html` و BUC-02/03/04، SUC-02/03/04، SVC-02، API Contract و فایل EA/XMI پیوست.

## دامنه واقعی این تحویل

- توسعه فرم «معرفی وثیقه» داخل همان workspace وثایق؛ بدون Route/Menu جدید یا تغییر Backend/Frontend سایر دامنه‌ها.
- جست‌وجو و انتخاب Party از API موجود CIF؛ انتخاب شناسه دستی یا ساختگی جایگزین جست‌وجوی CIF نیست. در سرور، `CifService.find` و `CifService.readiness` دوباره بررسی می‌شوند.
- استخراج نوع و ویژگی فعال از جداول رسمی CLT؛ پردازش داده عددی با BigDecimal و تاریخ ISO با date picker شمسی مشترک Angular.
- Endpoint اعتبارسنجی سرور، حتی وقتی DML خاموش است (مشروط به اجازه JWT و CLT reader).
- **اختیاری پس از تأیید DBA و IAM:** ایجاد یک رکورد `DRAFT` در `CLT.CLT_INTRODUCTION_DRAFT`، همراه با Audit در همان تراکنش و Idempotency سطح (Maker, Branch, Key).
- پاسخ ثبت و بازیابی، فقط برای همان Maker و شعبه JWT؛ شناسه Draft فنی با شماره انتظامی وثیقه کاملاً متفاوت است.
- **هیچ API ثبت قطعی**، INSERT روی `CLT.COLLATERAL`، تولید شماره انتظامی، تخصیص یا تأیید نهایی در این تحویل وجود ندارد.

## APIهای جدید

| Method | مسیر | Scope | نتیجه |
|---|---|---|---|
| GET | `/api/v1/collaterals/introduction-drafts/readiness` | `clt.collateral.read` | وضعیت Gateها و اعلام نبود ثبت نهایی |
| POST | `/api/v1/collaterals/introduction-drafts/validate` | `clt.collateral.introduce` | ارزیابی Party/نوع/ارز/ویژگی‌های فعال، **بدون DML** |
| POST | `/api/v1/collaterals/introduction-drafts` | `clt.collateral.introduce` + Gate DML | ثبت واقعی فقط در staging پیش‌نویس، Idempotency-Key الزامی |
| GET | `/api/v1/collaterals/introduction-drafts/{draftId}` | `clt.collateral.introduce` + Gate DML | فقط Draft متعلق به JWT Maker/Branch |

### قرارداد شناسه و داده

- `partyId` و `collateralTypeId` رشته عدد مثبت با ۱ تا ۱۹ رقم؛ Backend `BigDecimal` به کار می‌گیرد. برای دریافت Party، مدل قدیمی CIF هنوز شناسه `long` را در JSON به صورت Number می‌فرستد؛ رابط جدید مقدار خارج از بازه `Number.isSafeInteger` را برای جلوگیری از افت دقت رد می‌کند.
- `estimateAmount` رشته عدد صحیح یا اعشاری با حداکثر ۱۶ رقم صحیح و ۴ رقم اعشاری؛ عدد نمایی/جداکننده هزارگان پذیرفته نمی‌شود.
- `validFrom` و `validTo` با ISO `yyyy-MM-dd`؛ UI از انتخابگر شمسی مشترک استفاده می‌کند.
- `attributes`: آرایه `[{"attributeId":"...","value":"..."}]`؛ نوع داده توسط تعریف CLT تعیین می‌شود. TEXT, NUMBER, DATE, BOOLEAN کنترل می‌شوند. LIST چندگزینه‌ای به دلیل نبود قرارداد صریح جداکننده گزینه‌های `LIST_VALUE` تا تعیین تکلیف رد می‌شود؛ فقط مقدار تک‌گزینه‌ای دقیق قابل بررسی است.
- در صورت پیکربندی `HAS_MULTIPLICITY=Y`، **پیش‌نویس سروری رد می‌شود** تا G-03/G-12 (بازه‌ها/اقلام شمارشی) تعیین تکلیف شود.
- `Idempotency-Key`: رشته ۱۲ تا ۱۰۰ کاراکتر امن؛ پخش مجدد همان درخواست و کلید نتیجه اصلی را برمی‌گرداند؛ کلید با محتوای متفاوت HTTP 409 می‌دهد. SHA-256 از فرم نرمال‌شده و مرتب‌شده گرفته می‌شود.
- Branch از Claim معتبر `branch_code` استخراج می‌شود، نه Request؛ Subject/JWT باید توسط SecurityFilterChain موجود اعتبارسنجی شود.

## مراحل فعال‌سازی (خارج از Installer)

۱. `INSTALL-AND-BUILD.cmd` را اجرا کنید؛ این دستور **هیچ Oracle DDL/DML اجرا نمی‌کند**.  
۲. با DBA خروجی SQL read-only `00_INTRODUCTION_DRAFT_READONLY_PRECHECK.sql` و دو جدول جدید پیشنهادی را بازبینی کنید. جدول‌ها هنوز عضو مدل XMI مصوب ۲۶جدولی نیستند.  
۳. فقط پس از تصویب تغییر مدل و DBA، Migration `01_INTRODUCTION_DRAFT_DDL_OPT_IN.sql` را یک‌بار و با حساب مجاز اجرا کنید؛ حساب نوشتنی اختصاصی صرفاً SELECT/INSERT روی ۲ جدول پیش‌نویس، SELECT روی Sequence و هیچ مجوز نوشتن روی `CLT.COLLATERAL` نداشته باشد.  
۴. JWT، Issuer، Audience، JWKS و scopeها را طبق تنظیمات موجود R1B/R2A فراهم کنید؛ **در محیط عملیاتی HTTP بدون TLS یا هویت ساختگی پذیرفته نیست**.  
۵. در صورت تأیید محیط، `CLT_INTRODUCTION_DRAFT_WRITES_ENABLED=true` و سه متغیر Writer اختصاصی را تنظیم کنید؛ بدون آن کل مسیر DML DenyAll است.

```properties
# Existing prerequisites
CLT_REFERENCE_READS_ENABLED=true
CLT_REGISTRY_READS_ENABLED=true
# New, opt-in, stage-only
CLT_INTRODUCTION_DRAFT_WRITES_ENABLED=false
CLT_INTRODUCTION_WRITER_JDBC_URL=
CLT_INTRODUCTION_WRITER_JDBC_USERNAME=
CLT_INTRODUCTION_WRITER_JDBC_PASSWORD=
```

## شکاف‌های باز و شرط ورود به ثبت قطعی R2

- **G-17**: بانک هنوز الگوریتم قطعی شماره انتظامی `NUMBER(19)` را تصویب نکرده است. Draft ID هیچ‌گاه به شماره انتظامی تبدیل نمی‌شود.
- **G-14**: هویت پایدار دارایی و پیوند معرفی مجدد، و کنترل یکتایی دارایی در مدل XMI کامل نیست. صرفاً یک Party و نوع نمی‌تواند نشان‌دهنده دارایی یکتا باشد.
- **G-03/G-12**: تعریف بازه شمارشی، سریال و پرچم مستقل `IS_COUNTABLE_FLAG` کامل نیست. پیکربندی HAS_MULTIPLICITY نه معادل سیستم شمارش سریال است، نه اجازه ثبت بدون جدول اقلام.
- **G-01**: اصلاحیه شناسنامه با Approval/History مستقل هنوز مدل نشده است؛ برای ثبت قطعی ویرایش باید تعیین تکلیف شود.
- **G-04/G-10/G-13**: الزامات بیمه و سیاست بانکی و مجوز سطح معرفی هنوز نیازمند تأیید مالک فرآیند است.
- **CIF ↔ CLT**: این تحویل استعلام CIF را از لایه application موجود انجام می‌دهد اما آزمون واقعی KYC و مالکیت دارایی نزد بانک اجرا نشده است؛ readiness فنی CIF جایگزین کنترل حقوقی مالکیت دارایی نیست.
- **Oracle**: دو جدول Candidate staging عمداً به XMI اصلی اضافه نشده‌اند؛ هرگونه پذیرش مدل فیزیکی جدید نیازمند تصمیم رسمی است.

## اثر ادغام

- فایل‌های مشترک Frontend (`app.routes.ts`، Sidebar، `app.config.ts`)، فایل‌های IAM/CIF/DPS2، `pom.xml` و `application.yml` بدون تغییر می‌مانند.
- فقط **سه فایل متعلق به خود Collateral R2A** ارتقا داده می‌شوند: Registry Security و دو فایل Registry Component. دیگر فایل‌ها زیر پکیج/دایرکتوری `collateral/introduction` هستند.
- نصب با SHA-256 Fail-Closed است؛ در صورت تغییر قبلی یکی از سه فایل، از بازنویسی آن جلوگیری می‌شود.

## معیار خروج R2B

- Build کامل Maven/Angular و حضور کلاس‌ها در JAR، آزمون JWT معتبر/نامعتبر و scope، HTTP validation، Oracle transactions & rollback، race/idempotency، read own-branch/maker، آزمون CIF و UAT فرم در runtime باید به‌صورت واقعی گذرانده شوند.
- تا آن زمان وضعیت **Source Delivered / Runtime Pending** است، نه R2 بسته‌شده یا قابلیت بانکی عملیاتی.
