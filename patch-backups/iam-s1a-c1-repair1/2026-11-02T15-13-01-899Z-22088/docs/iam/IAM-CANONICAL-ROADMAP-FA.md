# نقشه راه مرجع IAM — Core Banking Prototype

**سند مرجع:** `docs/iam/IAM-CANONICAL-ROADMAP-FA.md`  
**نسخه سند:** IAM-R0.1 — ۲۰۲۶-۱۰-۳۰  
**مرجع سورس:** تصویر ZIP پروژه `core-banking-prototype-v0-2026-10-30-1801.zip` / نسخه `0.11.0`  
**وضعیت:** پیشنهاد معماری و سورس SQL آماده بازبینی؛ **هیچ اسکریپتی روی Oracle اجرا نشده و هیچ یکپارچه‌سازی عملیاتی ایجاد نشده است**.

## ۱. خط‌مشی ثابت و تصمیم معماری

- نام اسکیمای مستقل حاکمیت دسترسی **`IAM`** است؛ اسکیمای قدیمی `USR` صرفاً یک نمونه تاریخی برای قالب نام‌گذاری بوده و **مبنای مدل داده نیست**.
- BIAN 14 مرزبندی Service Domainها را هدایت می‌کند؛ نام فیزیکی Oracle را تعیین نمی‌کند. انطباق کامل با BIAN/APIهای رسمی هنوز تأیید نشده است.
- Password، Credential، Refresh Token، MFA Secret، Private Key و داده خام بیومتریک در `IAM` ذخیره نمی‌شوند؛ ارائه‌دهنده هویت مسئول آن‌هاست.
- `CIF` مالک Party/Customer، `ORG` مالک ساختار سازمانی و `IAM` مالک کاربران دیجیتال، نقش، انتساب، دامنه و شواهد حاکمیتی است.
- هویت ارائه‌دهنده از طریق زوج پایدار `(IDENTITY_PROVIDER_ID, SUBJECT_ID)` به حساب متصل می‌شود؛ نام کاربری و شماره ملی کلید هویت نیست.
- نقش کسب‌وکاری `CIF.PARTY_ROLE` معادل `IAM.ACCESS_ROLE` نیست.
- داده واقعی Reference/Policy و بازنویسی ساختار یا اتصال به Keycloak فقط پس از تصویب DBA/امنیت انجام شود.
- وضعیت فازها فقط در این سند به‌روز شود؛ تغییر نام یا بازتعریف فازها بدون تأیید صریح کاربر ممنوع است.

## ۲. پوشش برنامه

| Domain | مصادیق | وضعیت در R0 |
|---|---|---|
| Workforce IAM | کارمندان شعب، مدیریت مناطق و ستاد | اسکیمای عمومی طراحی شده؛ اتصال HR/ORG باز |
| Customer IAM | کاربران همراه‌بانک و اینترنت‌بانک، نماینده حقیقی/حقوقی | پیوند CIF طراحی شده؛ Entitlement/Mandate باز |
| Partner IAM | فین‌تک‌ها، TPPها، بانکداری باز | دامنه هویت طراحی شده؛ ثبت OAuth Client و Consent باز |
| Workload IAM | سرویس‌های Backend، Batchها و APIها | Principal نوع SERVICE طراحی شده؛ mTLS/JWT باز |
| Privileged IAM | مدیران و حساب‌های اضطراری | Role Type طراحی شده؛ PAM/JIT باز |
| Legacy Federation | سامانه‌های بانکی و سازمانی موجود | IDP و Account Link طراحی شده؛ Adapter باز |

## ۳. فازهای ثابت اجرا و معیار خروج

| کد | فاز | خروجی | شرط عبور | وضعیت |
|---|---|---|---|---|
| IAM-00 | Architecture & Naming Baseline | ADR، قرارداد نامگذاری، مدل مفهومی، مرزبندی CIF/ORG/IDP | بازبینی معمار و امنیت | **DESIGNED / APPROVAL_PENDING** |
| IAM-01 | User Management Data Foundation | DDL نسخه‌دار، جداول هویت، حساب، نقش، مجوز، گروه و Scope | DBA SQL Review + اجرای ایزوله + پذیرش داده | **DDL_PREPARED / ORACLE_NOT_APPLIED** |
| IAM-02 | Identity Federation & Authentication | Keycloak/IdP، SSO، OIDC، MFA، Provisioning، SCIM | آزمون نشست/توکن و صدور/ابطال | NOT_STARTED |
| IAM-03 | Workforce & Organizational Scope | HR/ORG linking، JML، شعب، مناطق، ستاد، تفویض | سناریوهای جابه‌جایی و لغو دسترسی | NOT_STARTED |
| IAM-04 | Authorization Engine | RBAC+ABAC+ReBAC، PEP/PDP، FEE3/CIF/DPS2 integration | Deny-by-default، Cross-scope و SoD آزموده شود | NOT_STARTED |
| IAM-05 | Identity Governance & SoD | درخواست/تصویب، بازبینی، Audit، JIT governance | Maker/Checker و Recertification UAT | NOT_STARTED |
| IAM-06 | Customer & Corporate Identity | CIAM، Mandate، نقش مشتری حقوقی و Delegation | Cross-account access denial + Mandate expiry | NOT_STARTED |
| IAM-07 | Partner & Workload Identity | Open Banking/FAPI 2، OAuth Client، mTLS، Federation | انطباق امنیتی و آزمون کلاینت‌ها | NOT_STARTED |
| IAM-08 | PAM, Resilience & Production Readiness | PAM/JIT، HA/DR، Security Test و UAT | شواهد تست، Recovery و مصوبه عملیاتی | NOT_STARTED |

## ۴. ترتیب فایل‌های این تحویل (فقط آماده‌سازی)

1. `database/oracle/iam/00-iam-precheck-readonly.sql`: بررسی فقط خواندنی؛ **بدون تغییر**.
2. `database/oracle/iam/01-iam-foundation-ddl-opt-in.sql`: 14 جدول مرجع و 16 جدول اصلی؛ **اجرای صرفاً اختیاری**.
3. `database/oracle/iam/02-iam-governance-ddl-opt-in.sql`: 5 جدول Governance/SoD/Audit؛ **اجرای صرفاً اختیاری**.
4. `database/oracle/iam/03-iam-reference-seed-opt-in.sql`: مقادیر پیشنهادی؛ **بدون COMMIT خودکار**.
5. `database/oracle/iam/99-iam-verify-readonly.sql`: ارزیابی فیزیکی بدون تغییر.
6. `docs/iam/IAM-R0-DATA-DICTIONARY.csv`: فرهنگ داده تک‌به‌تک ستون‌ها.

## ۵. وضعیت دقیق و شواهد تحویل

| محور | شاهد | نتیجه |
|---|---|---|
| اسناد مدل | `IAM-NAMING-DATA-MODEL-FA.md` و CSV | تهیه شده |
| Schema | `IAM` مستقل از `USR` | تصمیم پیشنهادی، DBA approval لازم |
| SQL | 35 جدول؛ 415 ستون | Generated، Static Check انجام‌شده در تولید |
| تست Oracle | **وجود ندارد** | `ORACLE_NOT_APPLIED` |
| تغییر Backend/Frontend | **وجود ندارد** | `NO_APPLICATION_CHANGE` |
| تغییر اسکیمای CIF/ORG/USR | **وجود ندارد** | `NO_LEGACY_DDL` |
| ردیابی Git | Commit/Hash در محیط کاربر در دسترس نیست | بعد از نصب با `git diff` ثبت شود |

## ۶. موانع و تصمیم‌های باز

- DBA باید موجودیت مستقل `IAM`، Tablespace، Grants و نحوه استقرار را تصویب کند؛ هیچ CREATE USER/GRANT به‌طور خودکار اجرا نمی‌شود.
- ساختار کلید یکتای ORG و مرجع رسمی HR هنوز برای Foreign Key بین‌اسکیمایی تأیید نشده‌اند؛ در این نسخه پیوندهای خارجی به‌صورت typed reference هستند، **نه FK صوری**.
- محصول نهایی Identity Provider (Keycloak یا جایگزین) پس از PoC و Security Review انتخاب می‌شود؛ جداول IAM به محصول خاص وابسته نیستند.
- قواعد گذار وضعیت‌ها، سازگاری بازه‌های زمانی و جلوگیری از هم‌پوشانی انتساب‌های فعال، در این DDL به‌تنهایی enforce نشده‌اند؛ فاز IAM-04/05 باید Policy و Transactional Service را تأمین کند.
- جدول `SOD_CONSTRAINT` به‌تنهایی برای اعمال SoD کافی نیست؛ جلوگیری از تصویب خودکار در Backend/Policy لازم است.
- اسناد مصوب RFP/BNCP در تصویر پروژه حاضر برای کنترل نهایی وجود ندارند؛ تطبیق رسمی در IAM-00 تکمیل شود.

## ۷. قدم بعدِ بدون پرش فاز

**IAM-01:** بازبینی DDL و مدل در UI/Repository، سپس با تصمیم صریح DBA اجرای isolated Oracle، ثبت خروجی `99-iam-verify-readonly.sql` و پذیرش Data Model. پس از آن IAM-02 شروع می‌شود. فعال‌سازی Identity Provider و تغییر Login فعلی در این تحویل وجود ندارد.

## ۸. منابع مبنا

- BIAN 14 Service Landscape: https://bian.org/deliverables/service-landscape/
- NIST SP 800-63-4: https://csrc.nist.gov/pubs/sp/800/63/4/final
- Microsoft Entra Identity Governance: https://learn.microsoft.com/en-us/entra/id-governance/identity-governance-overview
- OpenID FAPI 2.0 final: https://openid.net/specs/fapi-security-profile-2_0-final.html
- Keycloak official docs: https://www.keycloak.org/documentation
