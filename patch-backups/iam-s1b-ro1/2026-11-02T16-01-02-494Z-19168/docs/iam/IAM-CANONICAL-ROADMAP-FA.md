# نقشه راه مرجع IAM — Core Banking Prototype

**نسخه مرجع:** `IAM-R1.6 / S1A-C1` — ۲۰۲۶-۱۱-۰۲  
**مبنای واقعی سورس:** `core-banking-prototype-v0-2026-11-02-1751.zip`، نسخه نرم‌افزار `0.11.0`  
**سطح اعتبار:** تصمیم‌های معماری هنوز در انتظار تصویب؛ کد S1A-C1 یک شبیه‌ساز UI و کنترل Release است، **نه سامانه IAM عملیاتی**.  
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
| کد UI-S1A-C1 | Route/Component/Domain test | در سورس حاضر؛ Build ویندوز و اجرای Browser هنوز وابسته به نصب/آزمون است |

**عدم قطعیت مهم:** الحاقیه `IAM-ROADMAP-ORACLE-SNAPSHOT-ADDENDUM-R1.5.1.md` به والد R1.5 ارجاع می‌دهد که در ZIP مبنا نیست. این نسخه R1.6 آن خلأ را با ارائه وضعیت و مرجع صریح جبران می‌کند، نه با ادعای وجود سند مفقود.

## ۳. فازهای ثابت، وضعیت واقعی و Gate خروج

| کد | نام ثابت | خروجی/شرط عبور بدون تغییر ماهوی | وضعیت واقعی در S1A-C1 |
|---|---|---|---|
| IAM-00 | Architecture & Naming Baseline | ADR، مالکیت داده، Name Contract، Threat Model و تأیید معمار/امنیت | **DESIGN_PRESENT / APPROVAL_PENDING**؛ Snapshotها آشتی شده ولی ADRهای باز وجود دارد |
| IAM-01 | User Management Data Foundation | Data Foundation، API/Service امن، Migration ایزوله و پذیرش UI/Oracle DEV | **IN_PROGRESS**؛ S1A-C1 فقط Route/UI شبیه‌ساز و Release Gate؛ Oracle/API واقعی تأیید نشده |
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
| S1A-C1 — جاری | Route `#/iam/user-management`، گروه ناوبری، شبیه‌ساز حساب/Principal/Link، Negative Tests و کنترل سه‌لایه Build/Static/JAR | نصب واقعی و Build تازه، HTTP Proof و Browser UAT؛ تا تأیید عملیاتی **IN_PROGRESS** |
| S1B — بعدی | Identity Trust Boundary، Backend Service و API فقط با Actor واقعی؛ وضعیت‌ها/Idempotency/Optimistic Lock/Audit | Security Architecture Approved، 401/403 درست، Cross-Context Deny و E2E/API Tests؛ پیش از آن Write API بسته |
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
- **G2 (IAM-01 Data/Application):** **NOT_PASSED**؛ DDL Export، Mock UI و Static Tests کافی نیستند. Build، Backend امن، Oracle DEV و پذیرش داده/UAT باقی است.
- **G3 (Federation):** شروع نشده، متعلق به IAM-02.

## ۸. منابع معماری و اسناد

- `docs/iam/IAM-NAMING-DATA-MODEL-FA.md`، `docs/iam/ADR-IAM-001-FA.md` و `docs/iam/reviews/R1.5.1/`؛ اسناد RFP/BNCP برای تطبیق رسمی در این تصویر ZIP در دسترس نیستند.
- BIAN Service Landscape: https://bian.org/deliverables/service-landscape/
- NIST SP 800-63-4: https://csrc.nist.gov/pubs/sp/800/63/4/final
- RFC 9700 OAuth 2.0 Security BCP: https://datatracker.ietf.org/doc/html/rfc9700
- FAPI 2.0 Final: https://openid.net/specs/fapi-security-profile-2_0-final.html

**قاعده نگهداری:** در هر Release باید Baseline ZIP، اجرا/عدم اجرای Oracle، Backend/Frontend، آزمون‌های قابل تکرار، تصمیم‌های باز و گام بعد در همین سند ثبت شود؛ نام فازها بدون تأیید تغییر نکند.
