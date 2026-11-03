# IAM-01 / S1B-RO2 — امنیت توکن، مرز دسترسی و پذیرش Oracle DEV

**Release:** `IAM-R1.8-S1B-RO2` — ۲۰۲۶-۱۱-۰۲  
**Baseline:** سورس تجمیعی S1B-RO1 مبتنی بر ZIP ساعت ۱۷:۵۱ و استقرار موفق گزارش‌شده `IAM_S1B_RO1_DEPLOYED_DEFAULT_DISABLED`  
**طبقه‌بندی:** پیاده‌سازی و ابزار پذیرش؛ **نه تأیید یکپارچه‌سازی واقعی IdP یا Oracle DEV**.  
**تغییر Oracle:** صفر DDL/DML/COMMIT. **تغییر Frontend:** صفر. **IAM Write API:** صفر.

## ۱. تغییر فنی واقعی

| مؤلفه | رفتار تحویلی |
|---|---|
| JWT `RS256` | الگوریتم صریحاً pin شده، JWKS و Issuer معتبر HTTPS از پیکربندی بیرونی |
| Token Contract | Audience، Subject، iat، exp، حداکثر طول عمر ۳۶۰۰ ثانیه، آرایه‌های `iam_domains`/`iam_contexts` با ساختار دقیق |
| Unauthorized vs Forbidden | در حالت فعال، بدون/نامعتبر Bearer باید `401` و اعتبار معتبر با Scope/Domain ناکافی باید `403` بدهد؛ در حالت پیش‌فرض غیرفعال `403` |
| Oracle Enumeration | کنترل Scope و Domain **قبل از هر مراجعه به Oracle**؛ کنترل Context بر مبنای Domain واقعی بعد از AuthN/AuthZ اولیه |
| Oracle Least Privilege | Reader باید صرفاً `CREATE SESSION`، بدون Role فعال، و فقط ۴ مجوز مستقیم `READ/SELECT` روی جداول مشخص IAM داشته باشد؛ مجوز جانبی باعث رد Readiness می‌شود |
| Oracle Contract | ستون‌های ۴ جدول مطابق Snapshot R1.5.1، سپس آزمون `SELECT` بدون خواندن ردیف برای هر جدول؛ خطاها به `503` تبدیل می‌شوند |
| Release Gate | کنترل فایل‌های Java جدید، قرارداد امنیت و Proof از سورس در JAR؛ JAR هم‌نسخه قدیمی پذیرفته نیست |

**هشدار امنیتی:** `Connection.setReadOnly(true)` فقط یک hint به Driver است و تضمین Oracle نیست. کنترل اصلی، مجوزهای واقعی حساب اختصاصی و آزمون‌های تحت همان حساب است. حساب دارای Role، SELECT ANY TABLE، DML، DDL یا دسترسی به جدول اضافی عمداً پذیرفته نمی‌شود. این قرارداد نیازمند تأیید DBA و Security است.

## ۲. وضعیت پیش‌فرض (بدون IdP یا Oracle)

`IAM_S1B_READS_ENABLED=false` باقی است. تنها کنترل‌های محلی انجام می‌شوند و دسترسی Oracle انجام نمی‌شود.

پس از استقرار، ابزار زیر آزمون HTTP منفی انجام می‌دهد:

```cmd
node tools\iam\s1b\verify-s1b-ro2-live.cjs http://127.0.0.1:8091 disabled
```

پنج مورد شامل Readiness/Accounts بدون Token، Header جعلی، Bearer نامعتبر و POST ردشده بررسی می‌شود. پیام موفقیت `IAM_S1B_RO2_DEFAULT_DISABLED_PASS=5/5` است. این پیام اثبات AuthN فعال نیست.

## ۳. راه‌اندازی IdP/Oracle فقط پس از مصوبه

برچسب‌ها باید توسط Security و DBA با Secret Manager/Environment معتبر تأمین شوند؛ **مقدار/Token را در Git، Ticket، لاگ، ZIP یا چت منتشر نکنید**.

```text
IAM_S1B_READS_ENABLED=true
IAM_S1B_ISSUER=<exact approved HTTPS issuer>
IAM_S1B_JWKS_URI=<approved HTTPS JWKS URL>
IAM_S1B_AUDIENCE=<API resource audience>
IAM_S1B_MAX_TOKEN_LIFETIME_SECONDS=3600
IAM_S1B_JDBC_URL=<approved Oracle DEV JDBC URL>
IAM_S1B_JDBC_USERNAME=<dedicated direct-grant SELECT-only reader>
IAM_S1B_JDBC_PASSWORD=<injected secret>
```

**قرارداد توکن Access (نه ID Token):** JWT امضاشده `RS256` با `iss`، `aud`، `sub`، `iat`، `exp`؛ `scope` شامل `iam.users.read` و دو آرایه استاندارد سفارشی `iam_domains` و `iam_contexts`. مقادیر Domain/Context حروف بزرگ ASCII و exact-match هستند؛ wildcard/duplicate پذیرفته نیست. طول عمر توکن حداکثر ۳۶۰۰ ثانیه است. Issuer/Audience و Semantic هر Claim باید در `SEC-IAM-01` به تأیید Security/IdP برسند. احراز هویت برنامه فعلی برای این API از JWT خارج از برنامه می‌آید و به معنی تکمیل IAM-02 نیست.

### سنجش زنده — بدون نوشتن توکن در فرمان یا خروجی

در Session محلی امن، متغیرهای محیطی `IAM_RO2_TEST_VALID_TOKEN` و `IAM_RO2_TEST_DOMAIN` را از محیط آزمون تأییدشده فراهم کنید. سپس:

```cmd
node tools\iam\s1b\verify-s1b-ro2-live.cjs http://127.0.0.1:8091 enabled
```

در حالت واقعی، نبود Token، توکن خراب، تغییر امضای JWT، Header جعلی باید `401`، Domain غیرمجاز و POST باید `403` و Readiness و Accounts با توکن/دامنه مجاز و Oracle آماده باید `200` باشد. برای تأیید Fail-closed پایگاه ناموجود، حالت `enabled-db-unavailable` انتظار `503` برای Readiness دارد. برای موارد دلخواهِ توکن Issuer غلط، Audience غلط، منقضی و فاقد Scope، نام‌های `IAM_RO2_TEST_WRONG_ISSUER_TOKEN`، `IAM_RO2_TEST_WRONG_AUD_TOKEN`، `IAM_RO2_TEST_EXPIRED_TOKEN`، `IAM_RO2_TEST_NO_SCOPE_TOKEN` تعریف شده‌اند. اگر IdP این نمونه‌ها را فراهم نکند، هر مورد **PENDING** می‌ماند؛ از ساخت توکن آزمون با کلید جعلی به‌عنوان اثبات یکپارچه‌سازی پرهیز شود.

### اثبات مجوز Oracle

`IAM-S1B-RO2-ORACLE-DEV-READONLY-PREFLIGHT.sql` را **پس از مصوبه** و با همان Reader روی Oracle DEV اجرا کنید. فقط `SELECT` دارد و داده شخصی خروجی نمی‌دهد؛ لازم است خروجی Privilege/Role، ستون‌های چهار جدول، صفر بودن تست SELECT بدون ردیف، تعداد Usernameهای NULL و Principalهای چندحسابی بررسی شود. مقدارهای واقعی Oracle در این Release قابل ادعا نیستند.

## ۴. شواهد و Gateها

| مورد | وضعیت این تحویل |
|---|---|
| بررسی سورس/قرارداد S1B قبلی | ۳۷ کنترل (پس از سازگاری با Validator جدید) |
| کنترل‌های ساختاری RO2 | ۲۶ کنترل |
| آزمون خالص Java (Claim و Grant) | ۲۶ مورد، بدون Spring/Oracle |
| آزمون ابزار HTTP با سرور ساختگی | سه حالت، صرفاً برای خود ابزار |
| آزمون‌های JUnit Token Validation | ثبت در Repository؛ اجرای واقعی وابسته به Maven |
| Build کامل Spring/Maven در Windows | **باز، باید از Installer تأیید شود** |
| تست امضای JWT و IdP واقعی | **باز؛ نیازمند مصوبه و Token واقعی** |
| تست پایگاه Oracle DEV و حساب Reader | **باز؛ نیازمند DBA** |
| Runtime AuthN 401/403/200/503 | **باز تا اجرای ابزار با IdP و Oracle واقعی** |
| IAM-01 کامل / Approval | **خیر** |

**خروج از RO2:** پیام `IAM_S1B_RO2_DEPLOYED_DEFAULT_DISABLED` صرفاً استقرار امن پیش‌فرض را اثبات می‌کند. بستن RO2 و عبور به S1C نیازمند Evidence مستقل 401/403/200/503، بررسی واقعی grant و تأیید SEC/DBA است. هیچ عملیات Oracle Write، Audit ماندگار و چرخه مدیریت کاربران هنوز مجاز نشده است.
