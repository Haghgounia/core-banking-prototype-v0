# IAM-S1A — برش آزمایشی مدیریت کاربران (UI + Domain Simulator)

**نسخه بسته:** IAM-S1A DEV-UI.1 — ۲۰۲۶-۱۱-۰۲  
**مبنای سورس:** `core-banking-prototype-v0-2026-11-02-1443.zip`؛ Core Banking Prototype 0.11.0  
**مدل مرجع:** `IAM-R1.5.1` / استخراج Oracle با SHA-256 `932efb3e7a52d16c46a0737badcedfbb33f091df417bf654d2dafe5832f89d94`  
**مرجع نقشه راه:** `docs/iam/IAM-CANONICAL-ROADMAP-FA.md` (فازهای ثابت IAM-00 تا IAM-08)  
**وضعیت:** `DEV_UI_MOCK_READY`; `BACKEND_AUTHN_NOT_IMPLEMENTED`; `ORACLE_WRITE_NONE`; `ORACLE_RUNTIME_UNVERIFIED`.

## ۱. هدف و مرز پیاده‌سازی

این برش، یک **محیط سناریویی داخل Angular** برای تحلیلگر و تیم طراحی است، نه User Management عملیاتی. منطق دامنه به‌صورت TypeScript مستقل نوشته شده و در Service حافظه‌ای استفاده می‌شود. هیچ HTTP Endpoint جدیدی، اتصال Oracle جدید، ذخیره‌سازی LocalStorage، کوکی، رمز عبور، توکن، نقش عملیاتی یا یکپارچه‌سازی واقعی IdP/CIF/ORG وجود ندارد. اطلاعات نمونه ساختگی‌اند و با رفرش مرورگر، از ابتدا بارگذاری می‌شوند.

هدف از محدود کردن مرز اجرا، جلوگیری از ارائه Write API بدون اعتبارسنجی Actor و Authorization معتبر در شرایطی است که سامانه هنوز Spring Security/JWT Access Control برای IAM ندارد. **حالت MOCK_VERIFIED جایگزین اثبات هویت نیست** و هیچ اعتبار امنیتی خارج از شبیه‌ساز ندارد.

## ۲. قابلیت‌های تحویلی

| قابلیت | سازوکار | محدوده |
|---|---|---|
| ایجاد Principal و User Account | UUIDv4 تولیدشده با Web Crypto؛ Account ID مستقل؛ وضعیت PROVISIONING | حافظه مرورگر |
| تفکیک WORKFORCE/CUSTOMER | قاعده ثابت سازگاری Identity Context با Security Domain و مرجع ORG/CIF | دو Context نمونه |
| Username | NFKC، trim، lowercase، اعتبارسنجی کاراکتر و یکتایی در Security Domain | قرارداد موقت DEV؛ نیازمند تصویب Security/IdP |
| Identity Link | ایجاد لینک نمونه؛ منع پیوند تکراری به Principal دیگر | بدون تأیید منبع معتبر |
| Link Verification | تأیید *شبیه‌سازی‌شده* با علامت MOCK_VERIFIED | هرگز در Oracle/IdP اعمال نمی‌شود |
| Rename | حفظ Account ID و Principal UID و پیوند کسب‌وکار؛ افزایش Record Version | فقط غیرتعلیق‌شده |
| Activate/Suspend | تست انتقال حالت و رد دسترسی حساب معلق | بدون Grant/Revoke واقعی |
| Cross-Context | Deny برای تقاضای دسترسی از دامنه/Context دیگر | صرفاً در تابع سناریویی |
| Audit | لاگ اعمال و شماره نسخه؛ قابل مشاهده در UI | غیردائمی، بدون Actor واقعی |
| Metadata verification | SQL فقط خواندنی `04-iam-physical-snapshot-verify-r1.5.1-readonly.sql` | اجرای DBA اختیاری؛ روی Oracle **اجرا نشده** |

## ۳. سناریوهای UAT تحلیلگر

1. منوی «مدیریت هویت و دسترسی → مدیریت کاربران (DEV)» را باز کنید (`#/iam/user-management`). هشدار محیط آزمایشی باید مشخص باشد.
2. با انتخاب WORKFORCE یک Account و مرجع ساختگی ORG ایجاد کنید. وضعیت PROVISIONING و LinkStatus=UNVERIFIED خواهد بود. فعال‌سازی اولیه غیرفعال است.
3. روی «تأیید آزمایشی پیوند»، سپس «فعال‌سازی آزمایشی» کلیک کنید. Active فقط در حافظه مرورگر ثبت می‌شود.
4. Username را تغییر دهید؛ `Account ID`, `Principal UID`, `External Entity ID` باید ثابت بمانند و Version افزایش یابد.
5. حساب را Suspend کنید؛ در نتایج سناریوی دسترسی قابل قبول نباشد و Rename غیرفعال شود.
6. با UserName برابر در همان Security Domain دوباره ثبت کنید؛ باید Duplicate رد شود. با همان Username در Context متفاوت، برای دامنه دیگر مجاز است.
7. روی «آزمون Cross-Context» کلیک کنید؛ نتیجه **DENIED (EXPECTED)** است. این تست، احراز هویت واقعی یا PEP/PDP را اثبات نمی‌کند.
8. «بازنشانی داده نمونه» باید داده‌های آزمایشی اولیه را برگرداند.

## ۴. معیارهای خروج IAM-S1A و وضعیت صریح

| کنترل | وضعیت | شاهد / شرط عبور |
|---|---|---|
| مدل داده R1.5.1 As-Is در Repository | مستند | `reviews/R1.5.1/` و تطبیق SHA |
| UI با سناریوهای Principal/Account/Link | پیاده‌سازی شده | `features/iam/`، route و منو |
| منطق رد Duplicate, Cross-Context, Invalid UUID | تست خودکار | `node tools/iam/s1a/verify-s1a-ui-mock.cjs` |
| Static Developer QA حوزه S1A | ۱۵ تست موفق در محیط تولید بسته | لاگ `IAM_S1A_DEV_QA_PASS=15/15` |
| انتشار UI و تست Navigation | verifier منوی R22 بر اساس route موجود FEE3 اصلاح و موفق شد | `tools/verify-r22-sidebar.mjs`؛ تغییری در رفتار FEE3 داده نشد |
| Angular Production Build | **در محیط تولید بسته قابل اجرا نبود** | `frontend/node_modules` و toolchain نصب‌شده در ZIP موجود نیست؛ پس از نصب `build-production.cmd` اجرا شود |
| Oracle Metadata Live Verification | **انجام نشده** | DBA باید metadata read-only اجرا و خروجی را ضمیمه کند |
| Authenticated Backend API / Actor / Audit Durable | **انجام نشده (مسدود)** | JWT/OIDC trust, permissions, PEP/PDP, audit design, threat model |
| API Write/Oracle DML/Migration | **انجام نشده** | تصویب ADRها، G2، مهاجرت ایمن + DBA approval |
| Git Diff/Commit در Repository محلی | **نامعلوم** | ZIP فاقد `.git`؛ Installer می‌تواند فایل‌های نصب‌شده را نشان دهد |

## ۵. مسائل باز برای برش بعدی (IAM-S1B)

**نباید Backend Write API قبل از حل این Gateها فعال شود:**
- تصویب `ADR-IAM-02/04/09`, `IAM-REF-01`، Tenant/Context ownership، Username Policy و Link Proof Contract.
- تعیین IdP/Trust Boundary و Middleware بررسی JWT signature/issuer/audience/expiry/scopes؛ `X-User-Id`/Header ادعایی مجاز نیست؛ deny-by-default و least-privilege.
- طراحی Repository و نسخه‌بندی تراکنش‌های Oracle و Seed در DEV پس از تطبیق metadata واقعی، نه اجرای مجدد R0.1 یا DDL Export.
- پیاده‌سازی Authentication + Authorization برای هر عملیات و UI بر اساس مجوز Actor واقعی؛ Audit پایدار و آزمون‌های منفی Endpoint.
- تست cross-context و duplicate در API + DB، UUIDv4 RAW(16) round trip، Revocation واقعی و اثر تعلیق در PEP.

## ۶. حدود شواهد

از فایل DDL استخراج‌شده ۳۷ جدول، ۳۰۰ ستون، ۵۴ FK و ۲۲ Sequence گزارش شده است. این شواهد **وجود Runtime امروز یا اعتبار اطلاعات** را اثبات نمی‌کند. DDLهای IAM-R0.1 متعلق به نسخه متفاوتی هستند و با این ساختار **نباید دوباره اجرا شوند**. SQL جدید تنها ALL_* Metadata را می‌خواند، اما نتایج آن نیز به مجوزهای کاربر متصل بستگی دارد.
