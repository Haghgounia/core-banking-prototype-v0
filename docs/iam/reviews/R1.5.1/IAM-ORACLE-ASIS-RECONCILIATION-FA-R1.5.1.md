# IAM — Oracle Physical DDL Snapshot & R1.3 Reconciliation

**Document ID:** IAM-PHY-01  
**Version:** R1.5.1 / 2026-11-02  
**Status:** Evidence-based design reconciliation; NOT a migration or Oracle execution approval  
**Input:** `IAM-2026-11-02.txt`  
**Input SHA-256:** `932efb3e7a52d16c46a0737badcedfbb33f091df417bf654d2dafe5832f89d94`  
**Comparison:** `IAM-IDENTITY-FEDERATION-DATA-DICTIONARY-R1.3.xlsx`  
**Roadmap baseline:** IAM-R1.5, IAM-00 to IAM-08; no renaming or redefinition of stages.

## 1. حدود شواهد و وضعیت واقعی

فایل ارائه‌شده خروجی استخراج ساختار آبجکت‌ها به سبک Oracle DDL است، نه گزارش وضعیت Runtime با اتصال مستقل. وجود متن `CREATE TABLE` شاهد تعریف ساختار در این استخراج است؛ اثبات وجود کنونی جدول، VALID بودن تمام قیود، تعداد داده‌ها، سطح دسترسی و وضعیت عملکردی نیازمند Metadata Read-only در اتصال واقعی است. گزارش روز قبل `IAM_TABLES_VISIBLE=0` بود و این Snapshot تغییر ساختار نسبت به آن را نشان می‌دهد، اما زمان/علت اجرا از فایل قابل اثبات نیست.

**هشدار اجرا:** فایل ورودی شامل نقل‌قول‌های صادرشده از خروجی ابزار و تکرار دستورات `CREATE INDEX` و `ALTER TABLE` است؛ نباید مستقیماً به‌عنوان Migration اجرایی اعمال شود.

## 2. موجودی دقیق

| معیار | از فایل ورودی |
|---|---:|
| Tables | 37 |
| Columns | 300 |
| Reference tables `REF_*` | 15 |
| Primary keys | 37 |
| Foreign keys | 54 |
| Unique constraints | 26 |
| Check constraints | 90 |
| Total named constraints | 207 |
| Index definitions (distinct) | 63 |
| Sequences | 22 |
| Sequences with `NOCACHE` | 22 |
| Generic four-column Audit fields | 0 |
| Data/Seed INSERT statements | 0 |

تمام ۳۷ نام جدول جمع هستند. کلید Principal در `PRINCIPALS.PRINCIPAL_ID` عددی `NUMBER(19,0)` و UID در `PRINCIPAL_UID RAW(16)` با `UNIQUE NOT NULL` است؛ UID فاقد Default در Oracle است و سیاست تولید UUIDv4 در سرویس باید کنترل شود.

## 3. تطبیق ساختار با مدل هدف R1.3

| معیار | Snapshot Oracle | R1.3 پیشنهادی | اختلاف |
|---|---:|---:|---:|
| Tables | 37 | 38 | 1 candidate table |
| Columns | 300 | 319 | 19 target-only |
| FK | 54 | 58 | 4 proposed |

**هشت ستون پیشنهادی در جداول فعلی هنوز وجود ندارند:**

- `IDENTITY_PROVIDERS`: `NAME_FA`, `NAME_EN` — الزام دوزبانه مصوب کاربر.
- `IDENTITY_LINKS`: `VERIFIED_AT`, `VERIFIED_BY_PRINCIPAL_ID`, `EVIDENCE_REFERENCE` — منوط به تصمیم ADR-IAM-02.
- `FEDERATED_IDENTITIES`: `VERIFIED_AT`, `LINKED_BY_PRINCIPAL_ID`, `LINK_EVIDENCE_REFERENCE` — منوط به تصمیم ADR-IAM-04.

**یازده ستون باقیمانده** متعلق به جدول جدید پیشنهادی `TARGET_SYSTEM_ACCOUNT_LINKS` هستند. ایجاد این جدول پیش از تصویب مالک IAM در برابر IGA مطابق ADR-IAM-09 توصیه نمی‌شود.

چهار FK پیشنهادی جدید شامل دو ارجاع به Principal تأییدکننده/پیونددهنده و دو ارجاع جدول کاندید به `USER_ACCOUNTS` و `APPLICATIONS` هستند.

## 4. ریسک‌های مهم مهندسی

| شناسه | مشاهده از DDL | پیامد | پیشنهاد و وضعیت |
|---|---|---|---|
| IAM-PHY-01 | `USER_ACCOUNTS.USERNAME_NORMALIZED` nullable و بدون Default/Generated Expression است؛ Unique روی `(SECURITY_DOMAIN_ID, USERNAME_NORMALIZED)` وجود دارد. | در Oracle، برای Unique مرکب، تکرار کلیدهای دارای مؤلفه NULL با سایر مؤلفه‌های یکسان نیز ممنوع است؛ بنابراین چند حساب با Normalized=NULL در یک دامنه احتمالاً با ORA-00001 رد می‌شوند. در عین حال، دو Username یکسان با یکی Normalized=NULL و دیگری مقدار واقعی، الزاماً با این قید تشخیص داده نمی‌شوند. به‌هرحال نبود مقدار Normalized مانع عملکرد صحیح ثبت و جست‌وجوی نام کاربری است. | **P0**: تعیین یک قرارداد نرمال‌سازی Unicode و تضمین تولید مقدار برای تمام حساب‌ها در DB/Service؛ آزمون یکتایی و Rename. قبل از داده واقعی اصلاح شود. |
| IAM-PHY-02 | `IDENTITY_LINKS` دارای Unique همه‌تاریخی بر سه‌گانه Source/Type/Entity است. | پس از End-date و اصلاح پیوند اشتباه، نگهداری تاریخچه و پیوند معتبر جدید می‌تواند مسدود شود. | **P0 برای Federation/Identity-Link**: طراحی active-only uniqueness، Proof و اجرای Link/Unlink/Re-link امن؛ ADR-IAM-02. |
| IAM-PHY-03 | `FEDERATED_IDENTITIES` دارای دو Unique دائمی با Provider+Subject و Account+Provider است. | امکان ثبت تاریخچه و اتصال مجدد کنترل‌شده به IdP محدود می‌شود. | **P0 برای Federation**: قرارداد Issuer+Subject، Proof، یکتایی پیوند فعال؛ ADR-IAM-04. |
| IAM-PHY-04 | `IDENTITY_PROVIDERS` دارای `PROVIDER_CODE` و `ISSUER_URI` است اما `NAME_FA` و `NAME_EN` ندارد. | عدم انطباق با قاعده نمایش دوزبانه. | **P0 Metadata**: افزودن عناوین دوزبانه پس از ارزیابی داده‌های موجود و Migration امن. |
| IAM-PHY-05 | `PRINCIPAL_CORRELATIONS` فقط دو FK اصلی و بدون Check جلوگیری از پیوند خود/معکوس/تکراری است. | ممکن است ثبت همانی متناقض یا ایجاد ارتباط نامعتبر بین Principalها رخ دهد. | **P1**: قواعد canonical ordering, cross-context type, verification, approvals و Unique فعال قبل از فعال‌سازی قابلیت. |
| IAM-PHY-06 | هر ۲۲ Sequence با `NOCACHE` تولید شده است. | سربار تولید شناسه در ثبت پرحجم، به‌ویژه در سناریوی ده‌ها میلیون کاربر. | **P1 performance**: ارزیابی Oracle Sequence CACHE و مانیتور در آزمون بار؛ مقادیر Cache نیازمند آزمایش‌اند. |
| IAM-PHY-07 | فایل فاقد Seed/DML و آمار داده است. | وجود ساختار `REF_*` تضمین وجود مقادیر `WORKFORCE`, `CUSTOMER`, وضعیت‌ها و انواع نیست. | **P0 برای اجرای API**: بررسی Seed قابل‌تکرار و مرجع مقادیر بدون تکثیر غیرمجاز. |
| IAM-PHY-08 | فیلدهای نوع/وضعیت Principal از نوع `VARCHAR2(40)` و FK به کاتالوگ‌های Reference هستند. | خطای ساختاری نیست؛ نوع عددی در بحث‌های پیشین نهایی نشده. | **Open**: `IAM-REF-01`؛ بدون تغییر خودکار به کلید عددی. |

## 5. بخش‌های منطبق با تصمیم‌های مصوب

- `PRINCIPALS`, `USER_ACCOUNTS`, `SECURITY_DOMAINS` از `IDENTITY_CONTEXT_CODE` استفاده می‌کنند و دو FK ترکیبی برای تفکیک Context دارند. مبنای تصمیم `ADR-IAM-01` فراهم است؛ انتقال مجوز بین Contextها باید در Application نیز ممنوع شود.
- `USERNAME` در `USER_ACCOUNTS` نگهداری می‌شود و شناسه پایدار `USER_ACCOUNT_ID` مستقل از آن است. تولید اولیه Username و اجازه تغییر طبق سیاست، تکلیف Application/IdP است و از DDL به تنهایی اثبات نمی‌شود.
- داده‌های هویتی نظیر شماره ملی/تاریخ تولد/جنسیت در IAM کپی نشده‌اند؛ مرجع آن‌ها CIF/HR باقی می‌ماند.
- چهار ستون عمومی Audit حذف شده‌اند. جدول مستقل `ACCESS_AUDIT_EVENTS` برای رویداد امنیتی حفظ شده است.

## 6. تصمیم برای آغاز توسعه IAM-S1A

**توصیه:** این DDL را به‌عنوان **As-Is Physical Baseline** ثبت کنید؛ مجموعه DDLهای `IAM-R0.1` یا Export فعلی را دوباره اجرا نکنید. طبق Gate G2، ابتدا منبع واقعی Oracle و اتصال Backend مشخص شود، سپس Migration نسخه‌دار فقط برای دلتاهای تصویب‌شده تهیه شود.

اولین برش توسعه در DEV با داده ساختگی: `PRINCIPAL` + `USER_ACCOUNT` + `IDENTITY_LINK`، فهرست/ثبت/تغییر Username/تعلیق با آزمون‌های امنیتی و منفی؛ اتصال واقعی SSO/Federation بعد از تکمیل ADR-IAM-04. از استفاده از `X-User-Id` یا سایر Headerهای خوداظهاری به‌عنوان هویت معتبر جلوگیری شود. بررسی Source/Production با این فایل جایگزین آزمون کارکرد API نیست.

## 7. معیارهای پذیرش S1A

1. WORKFORCE/ CUSTOMER cross-context Account Creation از طریق FK و Service رد شود.
2. UUIDv4 معتبر بدون Default پایگاه داده تولید و Round-trip شود؛ هر UID یک بار ثبت شود.
3. Username به‌صورت سیستماتیک تولید، نرمال و Unique شود؛ تغییر Username شناسه حساب یا Federation را عوض نکند.
4. Principal انسانی فقط با Link معتبر به مرجع کسب‌وکاری مجاز فعال شود؛ Onboarding موقت از دسترسی عملیاتی جدا بماند.
5. عملیات نوشتن فقط از Actor احرازشده؛ Headerهای اختیاری کاربر برای Permission پذیرفته نشوند.
6. تعلیق/لغو حساب در تست دسترسی واقعی مؤثر باشد.
7. Seed و FKهای لازم با تست‌های خودکار Developer QA احراز شوند؛ UAT تحلیلگر فقط روی UI و سناریوهای کسب‌وکار باشد.
8. Git Diff/Commit و شواهد اجرای DEV در بسته تحویلی ثبت شوند؛ Oracle Production Write مجاز نیست.

## 8. تغییرات این بسته و وضعیت انتشار

**Documentation-only**: این فایل به همراه Inventory و Delta و الحاقیه Roadmap تولید شده است. فاقد Oracle DDL/DML، تغییر Runtime یا Installer است. ورود به پروژه محلی، Git Commit و تأیید Oracle هنوز انجام نشده است. در تحویل تجمیعی کد IAM-S1A، این اسناد باید به `docs/iam/reviews/R1.5.1/` افزوده شوند.
