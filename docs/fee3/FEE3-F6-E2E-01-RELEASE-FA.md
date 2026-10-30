# تحویل یکپارچه FEE3 — F6-E2E-01 (۲۹ اکتبر ۲۰۲۶)

**خط مبنا:** `Core Banking Prototype 0.11.0`، آرشیو `core-banking-prototype-v0-2026-10-29-1651.zip`  
**مرجع عملکردی:** `FCE_v13_8_84_Program_REDOWNLOAD(4).html`  
**نقشه راه یکتا:** `docs/fee3/FEE3-FCE-V13.8.84-CANONICAL-ROADMAP-FA.md`، بخش ۹  
**وضعیت تحویل:** `SOURCE_CANDIDATE / ENGINEERING_PARTIAL` تا زمان Build کامل در محیط اصلی و UAT تحلیلگر؛ **نه پذیرش بانکی**.

## قابلیت واقعی افزوده‌شده

1. مسیر مستقل محاسبات Backend: `POST /api/v1/fee3/simulations/preview`، محاسبه Java/BigDecimal بدون وابستگی به Oracle، Workflow یا Posting. از موتور موجود هشت روش عمومی F6 استفاده می‌کند؛ موتور موازی فرمولی نمی‌سازد.
2. داده ورودی: `{ "draft": { ... }, "inputs": {"amount":"1000000", "customer":"PERSON", "channel":"MOBILE"} }`. `draft` مطابق قرارداد موجود `Fee3Models.DraftRequest` است. در حالت `REQUEST` نرخ به‌صورت `inputs.contractRate` ارسال می‌شود.
3. خروجی: `status`, `method`, `rawIrr`, `feeIrr`, `taxIrr`, `totalIrr`, `breakdown`, `diagnostics`, `persisted=false`, `financialPosting=false`. تمام مقادیر مالی رشته Decimal و قابل نمایش بدون تبدیل شناور JavaScript هستند.
4. `UNREVIEWED`: وضعیت `PRETAX_ONLY`؛ فقط مبلغ خام/پیش از مالیات؛ `taxIrr=null` و `totalIrr=null`. مدل/فرمول غیرمجاز، بازه مبهم و `ACT_ACT` بدون CAL2 به‌صورت `BLOCKED` برمی‌گردند.
5. گام «آزمون و جمع‌بندی» Angular: برای هشت روش عمومیِ غیرتخصصی درخواست Backend می‌فرستد، مبلغ و Trace را نمایش می‌دهد و تنها **پس از ورود مبلغ مورد انتظار و برابری عددی** اجازه ثبت تأیید عددی تحلیلگر در نشست را می‌دهد. بدون اقدام تحلیلگر PASS جدید ساخته نمی‌شود.
6. FORMULA، تسهیلات، ضمانت‌نامه، LC و اقلام تخصصی همچنان مسیر تخصصی موجود دارند و **تا تکمیل اتصال Backend در F7** در UI با عنوان «محلی / در انتظار F7» مشخص می‌شوند. آن‌ها در این بسته Backend-certified نام‌گذاری نشده‌اند.
7. Oracle Read-only باقی می‌ماند؛ نصب هیچ DDL/DML/COMMIT، انتشار، وصول، سند حسابداری یا تغییر وضعیت رسمی ندارد.

## QA فنی در محیط تولید بسته

| کنترل | نتیجه | دامنه اثبات |
|---|---|---|
| `javac --release 21` برای مدل/موتور/سرویس F6 | PASS | کامپایل سه کلاس pure-Java |
| اجرای مستقیم Java: Fixed، Percent، Mixed، Usage، Daily، محدودیت UNREVIEWED، FORMULA و ACT_ACT | PASS | شواهد عددی و محدودسازی، بدون HTTP |
| تحلیل نحوی TypeScript با compiler محلی | PASS | خطای نحوی وجود ندارد؛ **Type Check و Angular Build نیست** |
| Angular 21 / `npm ci --offline` | NOT_VERIFIED | وابستگی `zod-to-json-schema` در npm cache موجود نبود |
| Maven Wrapper / Full Spring Boot Build | NOT_VERIFIED | دانلود توزیع Maven از repo.maven.apache.org در این محیط ممکن نشد |
| اجرای مرورگر روی Windows، آزمون UI توسط تحلیلگر | OPEN | باید پس از Build موفق نسخه انجام شود |
| Oracle Runtime / تأیید مقررات | NOT_RUN | عمداً خارج از محدوده این تحویل |

برای جلوگیری از تکرار مشکل F7-A، بسته تحویلی `INSTALL.cmd` پس از بک‌آپ فایل‌ها، Build اصلی پروژه را اجرا می‌کند و در صورت شکست، فایل‌های سورس تغییرکرده را برمی‌گرداند. **موفقیت چنین Build در رایانه کارفرما از قبل ادعا نمی‌شود.**

## قدم بعد مطابق نقشه راه ثابت

- **F6:** تکمیل مسیر Backend فرمول‌های کنترل‌شده با قرارداد F2 و گسترش Trace روش‌ها، بدون اجرای عبارت دلخواه؛ سپس رفع نقص‌های UAT گام آزمون.
- **F7:** اتصال موتورهای تخصصی موجود (Loan/Qard/Guarantee/LC و موارد دسته‌چک و ارزش‌گذاری) به فرم‌های تخصصی؛ ریشه‌یابی و رفع شکست F7-A با `build-and-qa.log`، بدون تکرار نصب نسخه شکسته.
- **F8/F9/F10:** بدون تغییر عنوان یا ترتیب؛ صرفاً پس از تکمیل فعالیت قبلی و با ثبت وضعیت واقعی ادامه می‌یابند.

## شیوه Git

ورود فایل‌ها به Git پس از Build موفق و بازبینی Diff انجام می‌شود، نه با اعلام PASS ساختگی. بسته ZIP به‌تنهایی شامل پوشه `.git` نیست و شناسه Commit پروژه در این محیط قابل مشاهده نیست. فایل `DIFF-REPORT.json` بسته، تغییرات واقعی نسبت به آرشیو مبنا را نمایش می‌دهد. نصب هیچ Commit خودکاری ایجاد نمی‌کند.

**دستور Commit پس از Build و بازبینی Diff** (از ریشه پروژه):

```cmd
git status --short
git diff --stat
git add CHANGELOG.md backend/src/main/java/com/behsazan/corebanking/fee3/r3/Fee3F6SimulationController.java backend/src/main/java/com/behsazan/corebanking/fee3/r3/Fee3F6SimulationService.java backend/src/main/java/com/behsazan/corebanking/fee3/r3/Fee3PreviewEngine.java backend/src/test/java/com/behsazan/corebanking/fee3/r3/Fee3F6SimulationServiceTest.java docs/CORE-BANKING-PROJECT-ROADMAP-FA.md docs/fee3/FEE3-FCE-V13.8.84-CANONICAL-ROADMAP-FA.md docs/fee3/FEE3-F6-E2E-01-RELEASE-FA.md docs/roadmap/FEE3-IMPLEMENTATION-ROADMAP.md frontend/src/app/features/fee3/fee3-oracle-api.service.ts frontend/src/app/features/fee3/fee3-oracle-mapping.ts frontend/src/app/features/fee3/fee3-workspace.component.ts frontend/src/app/features/fee3/fee3-workspace.component.html
git diff --cached --stat
git commit -m "feat(fee3): connect F6 backend simulation to analyst wizard"
```

**احتیاط:** اگر فایل‌های غیرمرتبط در مسیر Backend مشخص‌شده تغییر کرده‌اند، پیش از Stage کردن آن‌ها انتخاب را به فایل‌های فهرست Manifest محدود کنید. هیچ Commit نباید بدون بازبینی کدهای Stage‌شده انجام شود.
