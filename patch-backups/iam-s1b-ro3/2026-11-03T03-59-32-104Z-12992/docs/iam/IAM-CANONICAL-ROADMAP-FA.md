# نقشه راه مرجع IAM — Core Banking Prototype

**نسخه مرجع:** `IAM-R1.8 / S1B-RO2` — ۲۰۲۶-۱۱-۰۲  
**مبنای واقعی سورس:** `core-banking-prototype-v0-2026-11-02-1751.zip`، نسخه نرم‌افزار `0.11.0`  
**سطح اعتبار:** معماری هنوز در انتظار تصویب؛ S1A-C1 در مرورگر تأیید شده و S1B-RO2 یک Backend امن و غیرفعال پیش‌فرض با کنترل‌های جدید توکن، Oracle Reader و Harness پذیرش است، **نه سامانه IAM عملیاتی**.  
**مالک ثبت وضعیت:** این فایل؛ `docs/roadmap/IAM-IMPLEMENTATION-ROADMAP.md` فقط نشانی پایدار آن است.

## ۱. تصمیم‌های ثابت معماری

- طرح جدید مستقل از مدل قدیمی هفت‌ساله `USR` است؛ نام اسکیمای پیشنهادی **`IAM`** و قرارداد نامگذاری Oracle مستقل و بازبینی‌پذیر است.
- تفکیک داده و مسئولیت: `CIF` مالک Party/Customer، `ORG` مالک Organization/Staff Context، `IAM` مالک Principal، Account، Entitlement و Governance Evidence، و IdP مالک Credential/MFA/Session است. IAM نباید Password، Refresh Token، Private Key یا داده خام بیومتریک را نگهداری کند.
- `Principal`، `Account` و `Identity Link` مفاهیم جدا هستند. `(Identity Provider, Subject ID)` شناسه فدره پایدار است؛ Username، شماره ملی یا شناسه شخص CIF شناسه هویت دیجیتال نیست.
- BIAN برای مرز Service Domain و API Contract راهنماست، نه قاعده نام فیزیکی Table. انطباق کامل BIAN هنوز آزموده/تصویب نشده است.
- `CIF.PARTY_ROLE` معادل `IAM.ACCESS_ROLE` نیست. Entitlement باید در چارچوب Scope، Policy و زمان اعتبار بررسی شود.
- هیچ API نوشتنی IAM بدون **Authenticated Actor، Deny-by-default، مجوز عملیات، Audit، Rate Limit و کنترل Cross-Context** فعال نمی‌شود؛ Header خوداظهاری (`X-User-Id` و مشابه) اثبات هویت نیست.
- Oracle Write، DDL تکراری و Seed واقعی نیازمند مصوبه DBA و امنیت و Migration نسخه‌دار با Rollback است. فایل‌های R0.1 یا DDL Export خام R1.5.1 **اسکریپت اعمال تغییر نیستند**.
- **شناسه، ترتیب و تعریف IAM-00 تا IAM-08 ثابت می‌مانند**؛ تغییر بدون موافقت صریح کاربر ممنوع است.

## ۲. وضعیت مدل داده — تفکیک شواهد از اجرا

| مرجع | شمارش | وضعیت و حدود اعتبار |
|---|---|---|
| SQL تاریخی R0.1 در `database/oracle/iam/` | ۳۵ جدول، ۴۱۵ ستون | صرفاً ردیابی مدل اولیه؛ به‌عنوان نسخه جاری **اجرا نشود** |
| `docs/iam/reviews/R1.5.1/IAM-2026-11-02-ORACLE-DDL-EXPORT.txt` | ۳۷ جدول، ۳۰۰ ستون، ۵۴ FK، ۲۲ Sequence | Snapshot متنی Export؛ به‌تنهایی اثبات Runtime زنده نیست |
| R1.3 Target مطابق سند Reconciliation | ۳۸ جدول، ۳۱۹ ستون، ۵۸ FK | هدف پیشنهادی؛ فایل مدل کامل R1.3 در مبنای ۱۷:۵۱ موجود نیست |
| کد UI-S1A-C1 | Route/Component/Domain test | در سورس حاضر؛ نصب REPAIR1 و نمایش Browser در تاریخ ۲۰۲۶-۱۱-۰۲ از روی لاگ/تصویر کاربر تأیید شد |

**عدم قطعیت مهم:** الحاقیه `IAM-ROADMAP-ORACLE-SNAPSHOT-ADDENDUM-R1.5.1.md` به والد R1.5 ارجاع می‌دهد که در ZIP مبنا نیست. این نسخه R1.6 آن خلأ را با ارائه وضعیت و مرجع صریح جبران می‌کند، نه با ادعای وجود سند مفقود.

## ۳. فازهای ثابت، وضعیت واقعی و Gate خروج

| کد | نام ثابت | خروجی/شرط عبور بدون تغییر ماهوی | وضعیت واقعی در S1A-C1 |
|---|---|---|---|
| IAM-00 | Architecture & Naming Baseline | ADR، مالکیت داده، Name Contract، Threat Model و تأیید معمار/امنیت | **DESIGN_PRESENT / APPROVAL_PENDING**؛ Snapshotها آشتی شده ولی ADRهای باز وجود دارد |
| IAM-01 | User Management Data Foundation | Data Foundation، API/Service امن، Migration ایزوله و پذیرش UI/Oracle DEV | **IN_PROGRESS**؛ S1A-C1 Browser OK؛ Backend S1B-RO2 کنترل امنیت و Reader اضافه شده ولی پیش‌فرض خاموش، فعال‌سازی IdP/Oracle و UAT باز |
| IAM-02 | Identity Federation & Authentication | OIDC/SSO/MFA، Federation، Provisioning/SCIM، مدیریت Token/Session | **NOT_STARTED** |
| IAM-03 | Workforce & Organizational Scope | HR/ORG Linking، Joiner-Mover-Leaver، Branch/Region/Head Office Scope | **NOT_STARTED** |
| IAM-04 | Authorization Engine | RBAC/ABAC/ReBAC، PDP/PEP، Deny-by-default و دامنه‌های CIF/FEE3/DPS2 | **NOT_STARTED** |
| IAM-05 | Identity Governance & SoD | Access Request/Approval/Review، SoD، Maker/Checker، Recertification، Audit | **NOT_STARTED** |
| IAM-06 | Customer & Corporate Identity | CIAM، Mandate/Delegation، کنترل Cross-account و تاریخ انقضا | **NOT_STARTED** |
| IAM-07 | Partner & Workload Identity | OAuth/FAPI 2، mTLS/DPoP، Partner/Client/Workload Identity | **NOT_STARTED** |
| IAM-08 | PAM, Resilience & Production Readiness | PAM/JIT/Break-glass، HA/DR، Security/UAT و Production Sign-off | **NOT_STARTED** |

## ۴. تصمیم‌های باز معماری و داده

| شناسه | تصمیم باز | اثر / نیازمندی |
|---|---|---|
| ADR-IAM-02 | قرارداد نهایی `USERNAME_NORMALIZED` و یکتایی per Security Domain | Normalize NFKC، عدم برخورد نام‌ها، NOT NULL/Constraint پس از Data Profiling |
| ADR-IAM-04 | مدل Link/Unlink/Re-link بین Principal، Account و External Identity | Evidence، تاریخچه غیرقابل‌تحریف، جلوگیری از پیوند تکراری و Hijacking |
| ADR-IAM-09 | مالک `TARGET_SYSTEM_ACCOUNT_LINKS`: IAM یا IGA | پیش از افزودن جدول و FK تصمیم‌گیری شود |
| IAM-REF-01 | Vocabulary، Seed مرجع و عنوان دوزبانه IdP | Reference Integrity، نسخه‌بندی و مالک تغییرات |
| SEC-IAM-01 | AuthN/Actor Trust Boundary و Bootstrap مدیریت IAM | OIDC/JWT Verification، Policy، Error Contract، Audit، عدم اتکا به Header خوداظهاری |
| DATA-IAM-01 | Oracle DEV As-Is Metadata، Read-only Verification | تایید زنده جدول/ستون/قیود و مقایسه قبل از Delta Migration |

## ۵. گام‌های برنامه و شرایط پذیرش

| برش | محتوای تجمیعی | معیار پذیرش |
|---|---|---|
| S1A-C1 — تأیید UI | Route `#/iam/user-management`، گروه ناوبری، شبیه‌ساز حساب/Principal/Link، Negative Tests و کنترل سه‌لایه Build/Static/JAR | نصب و Browser UAT فرم در ۲۰۲۶-۱۱-۰۲ تأیید؛ Mock بودن و عدم یکپارچگی Oracle محفوظ |
| S1B — جاری (S1B-RO2) | JWT RS256 با Claims سخت‌گیرانه، API GET، بررسی واقعی حداقل‌مجوز Reader و Policy Domain/Context پیش از Oracle؛ No Write | Source/Pure Java QA؛ Maven و IdP/Oracle DEV Integration + 401/403/200/503 واقعی و تصویب امنیت هنوز باز |
| S1C — پس از مجوز DBA | Read-only Runtime Recon، Delta Migration نسخه‌دار، Seed کنترل‌شده و Oracle DEV Integration | DBA/SEC Sign-off، Migration Integrity و Recovery/Negative Tests |
| S1D — بستن IAM-01 | UAT یکپارچه User Management، trace و Release Evidence | UAT تحلیلگر، امنیت، DBA و Developer QA؛ ثبت تاریخ و صاحب تأیید |

**سناریوهای S1A:** ایجاد حساب از نوع Workforce یا Customer، Username normalization/unique per Domain، UUIDv4، Identity Link ساختگی، Activating only after mock proof، Rename preserving identity، Suspension، Optimistic Version و Deny Cross-Context. **این سناریوها صرفاً در حافظه مرورگر هستند.** ادعای احراز هویت، مجوز واقعی یا Audit ماندگار ندارند.

## ۶. قرارداد Release و مانع تکرار Redirect

1. `app.routes.ts` باید Lazy Route و منوی IAM را داشته باشد؛ منوی R22 برای URL مستقیم گروه IAM را فعال کند.
2. پس از Build Angular، `tools/iam/s1a/verify-s1a-visibility.cjs dist` وجود Route و Form را در JS کامپایل‌شده کنترل کند.
3. فایل `iam-s1a-deploy.json` هش سورس و فایل‌های JS را ثبت کند؛ Static و سپس JAR واقعی باید با آن برابر باشند.
4. `bin/start.cmd` / `bin/start.sh` اگر JAR قدیمی باشد Fail شوند، حتی با همان Version `0.11.0`.
5. پس از شروع برنامه، `tools/iam/s1a/verify-s1a-live.cjs` فقط با HTTP Proof متناسب با سورس و JAR قبول کند. این آزمون جایگزین Browser UAT نیست.
6. **شواهد Git:** ZIP فاقد `.git` است؛ هیچ Commit یا Git Diff واقعی ساخته نشده. نصب در Repository کاربر نباید Commit خودکار ایجاد کند؛ `git status/diff` به‌عنوان Evidence بعد از نصب ثبت شود.

## ۷. وضعیت Gateهای فعلی

- **G1 (Architecture):** شواهد برای تصمیم فراهم؛ تصویب ADRها، Threat Model و مسئولیت‌ها باز است.
- **G2 (IAM-01 Data/Application):** **NOT_PASSED**؛ S1A Browser OK و S1B-RO.1 Source QA انجام‌شده، اما Build/IdP/Oracle DEV، Security Acceptance و UAT واقعی باقی است.
- **G3 (Federation):** شروع نشده، متعلق به IAM-02.

## ۸. منابع معماری و اسناد

- `docs/iam/IAM-NAMING-DATA-MODEL-FA.md`، `docs/iam/ADR-IAM-001-FA.md` و `docs/iam/reviews/R1.5.1/`؛ اسناد RFP/BNCP برای تطبیق رسمی در این تصویر ZIP در دسترس نیستند.
- BIAN Service Landscape: https://bian.org/deliverables/service-landscape/
- NIST SP 800-63-4: https://csrc.nist.gov/pubs/sp/800/63/4/final
- RFC 9700 OAuth 2.0 Security BCP: https://datatracker.ietf.org/doc/html/rfc9700
- FAPI 2.0 Final: https://openid.net/specs/fapi-security-profile-2_0-final.html

**قاعده نگهداری:** در هر Release باید Baseline ZIP، اجرا/عدم اجرای Oracle، Backend/Frontend، آزمون‌های قابل تکرار، تصمیم‌های باز و گام بعد در همین سند ثبت شود؛ نام فازها بدون تأیید تغییر نکند.

## ۹. ثبت پیشرفت IAM-R1.7 / S1B-RO.1 (۲۰۲۶-۱۱-۰۲)

- **ورودی:** تصویر تأیید فرم `#/iam/user-management` و لاگ موفق `IAM_REPAIR1_DEPLOYED`.
- **توسعه واقعی:** ۱۰ فایل Java کلاس جدید Backend برای JWT Resource Server محدود به `/api/v1/iam/**`، مخزن Oracle فقط‌خواندنی، دامنه، حساب، Principal و Identity Link؛ تست‌های جدید و Build Gate.
- **پیش‌فرض:** IAM API خاموش (`IAM_S1B_READS_ENABLED=false`)، بدون ارسال Token واقعی و بدون Oracle IO. Backend پس از ایجاد حساب حداقلی و Trust Issuer مجاز، فقط READ ارائه می‌کند.
- **ممانعت از Cross-Domain:** `iam_domains` و `iam_contexts` امضاشده، تطبیق Domain/Context واقعی Oracle و پیشگیری از نمایش پیوند Principal مشترک.
- **آزمون:** `tools/iam/s1b/verify-s1b-security.mjs` و `IamS1bPolicyTest.java`؛ نتیجه تست واقعی Java/Maven و JWT E2E باید جدا ثبت شود.
- **تصمیم‌های باز:** SEC-IAM-01، DATA-IAM-01، ADR-IAM-02، ADR-IAM-04 و تایید Claim Contract توسط امنیت/IdP.
- **گام بعد:** پس از Build موفق، راه‌اندازی IAM DEV با حساب SELECT-only و IdP معتبر در محیط کنترل‌شده، آزمون HTTP 401/403/200/503، سپس تکمیل IAM-01 با Persisted Audit و عملیات نوشتنی صرفاً پس از Approval.
- **ارجاع:** `docs/iam/implementation/S1B/IAM-S1B-RO1-SECURE-READ-FOUNDATION-FA.md`.
- **شواهد Git:** ZIP بدون `.git` است. Git Diff و Commit را نمی‌توان در این بسته ادعا کرد.


## ۱۰. ثبت پیشرفت IAM-R1.8 / S1B-RO2 (۲۰۲۶-۱۱-۰۲)

- **مبنا:** استقرار موفق R1.7 روی سیستم کاربر (`IAM_S1B_RO1_DEPLOYED_DEFAULT_DISABLED`)؛ این Release جایگزین S1B-RO1 است، اما S1A-C1 و فرم آزمایشی را حفظ می‌کند.
- **تغییر کد:** توکن‌های RS256 با کنترل Audience/Subject/iat/exp/حداکثر طول عمر/Claimهای typed، پاسخ‌های 401/403، کنترل Scope و Domain قبل از Oracle، بررسی مجوزهای واقعی Reader (`SESSION_PRIVS`، `SESSION_ROLES`، `USER_TAB_PRIVS_RECD`) و آزمون بدون ردیف SELECT.
- **گستره:** همان ۴ GET قبلی، بدون اضافه‌شدن API نوشتنی، بدون تغییر UI و بدون DDL/DML/COMMIT.
- **کیفیت قابل تکرار:** ۲۶ آزمون Java بدون وابستگی، ۲۶ کنترل ساختاری RO2، ۳ سناریوی ابزار HTTP؛ آزمون JUnit ثبت شده ولی تا Maven و IdP/Oracle واقعی اجرا نشود، Gate پذیرش نهایی باز است.
- **قرارداد Reader:** فقط CREATE SESSION، بدون Role فعال، ۴ مجوز مستقیم READ/SELECT روی `SECURITY_DOMAINS`، `PRINCIPALS`، `USER_ACCOUNTS`، `IDENTITY_LINKS` در Schema IAM. اگر DBA معماری Grant دیگری پیشنهاد کند باید پیش از تغییر این قرارداد تصویب شود.
- **گام بعد:** دریافت مصوبه SEC-IAM-01 / DATA-IAM-01، اتصال IdP و Oracle DEV در محیط کنترل‌شده، اجرای HTTP 401/403/200/503 و بررسی DBA، سپس تصمیم S1C (Migration و Audit ماندگار). **IAM-01 همچنان IN_PROGRESS است**.
- **شواهد Git:** ZIP سورس فاقد `.git` است، Commit واقعی ادعا نمی‌شود؛ Patch دارای Diff و Manifest نسخه‌دار است.
- **جزئیات:** `docs/iam/implementation/S1B/IAM-S1B-RO2-SECURITY-AND-ORACLE-ACCEPTANCE-FA.md`.
