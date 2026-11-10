# خط مبنای اجرایی نقشه راه وثایق — ۱۰ نوامبر ۲۰۲۶

**شناسه سند:** `CLT-EXECUTION-BASELINE-2026-11-10`  
**مرجع الزام‌آور برنامه:** `CLT-ROADMAP-R1.0-FA.md` — ساختار و معیارهای خروج **R0 تا R7 بدون تغییر**  
**محصول:** Core Banking Prototype، نسخه اصلی `0.11.0` (بدون تغییر)  
**دامنه:** فقط Collateral/CLT، بدون توسعه IAM یا تغییر ماژول‌های دیگر  
**ماهیت:** اولویت‌بندی و ردیابی اجرایی؛ نه تصویب کسب‌وکاری، نه مجوز DDL/Financial Posting، نه گواهی UAT  
**مالک تأیید تغییرات:** طبق Governance پروژه؛ اشخاص حقیقی، بانک و DBA باید جداگانه تعیین/تأیید شوند.

## ۱. شواهد مبنا و تفکیک سطوح پذیرش

| موضوع | واقعیت مستند در تاریخ تهیه | وضعیت / قید |
|---|---|---|
| آخرین نصب Patch تأییدشده توسط کاربر | `CLT-BUC-SUC-UAT-EVIDENCE-GAPS-2026-11-10` | **INSTALL_CONFIRMED**؛ صرفاً نصب، نه UAT یا Oracle Acceptance |
| آخرین Patch توسعه تحویل‌شده | `CLT-BUC-SUC-UAT-ORACLE-EVIDENCE-2026-11-10` | **DELIVERED / INSTALL_NOT_CONFIRMED** در تاریخ این سند |
| ثبت شواهد UAT در Oracle | Backend/UI و DDL Candidate در آخرین بسته تحویل‌شده | جداول Candidate هنوز تصویب/استقرارشان اثبات نشده؛ Persistence عملیاتی **NOT_VERIFIED** |
| ماتریس سناریو | ۱۶ BUC، ۲۰ SUC، ۳۲ سناریوی مثبت/بازدارنده در سورس | تعریف و پوشش UI، نه اجرای/تأیید ۳۲ آزمون |
| UAT نهایی | هیچ صورت‌جلسه یا شواهد تأیید نهایی ۱۶ BUC در گفتگو ارائه نشده | **۰ مورد END-TO-END ACCEPTED قابل استناد**؛ این عدد شمار آزمون شکست‌خورده نیست |
| ساختار Oracle / داده عملیاتی | گزارش‌های تاریخی Read-only و SQLهای Candidate؛ دسترسی زنده در محیط تهیه سند موجود نیست | وضعیت جاری جداول/داده‌ها **UNKNOWN** تا Query و Readback واقعی |
| Build و Runtime آخرین بسته | کنترل‌های ایستا و JDBC شبیه‌سازی‌شده در تحویل گزارش شده | Build نهایی Windows، Oracle Write و UAT Analyst **NOT_VERIFIED** برای آخرین بسته |

**سطوح وضعیت الزامی:** `SOURCE_IMPLEMENTED`، `BUILD_VERIFIED`، `ORACLE_VERIFIED`، `ANALYST_UAT_PASSED`، `BANK_ACCEPTED` جدا ثبت شوند. `OPEN` بودن Gap با وجود کدِ Candidate تناقض ندارد. هیچ گذار خودکاری از نصب Patch به پذیرش بانک مجاز نیست.

## ۲. تصمیم اولویت اجرایی بعدی

**P1 / فوری: Read-only Oracle and Reference-Data Reconciliation**؛ هدف، حذف ابهام درباره قابلیت اجرای اولین چرخه معرفی/ثبت وثیقه است، نه تولید Dashboard یا ابزار UAT جدید.

| ترتیب | دامنه / شناسه | اقدام اصلی | تحویل و Evidence | معیار خروج / شرط انتقال |
|---|---|---|---|---|
| **P1 — فوری** | R1/R2، BUC-01/02/03/04/13 | استعلام زنده فقط‌خواندنی دیکشنری Oracle، ۱۷ مرجع، نوع وثیقه فعال، شناسنامه‌های آزمون، Sequence/Constraint، وضعیت مرجع و شماره انتظامی | گزارش نسخه‌دار Object/Column/Constraint/Sequence و Count، با `OBSERVED_AT`، محیط، `PASS/BLOCKED/UNKNOWN` و تفکیک «داده ندارد» از «اتصال برقرار نیست» | پیش‌نیازهای اولین BUC-03 معلوم و شواهد قابل تکرار؛ هیچ DDL/DML نصب خودکار نباشد |
| **P2** | R2، BUC-02/03/04/13 | تکمیل قواعد داده، G-03/G-12/G-14/G-17 و صدور شماره مصوب؛ ثبت و بازیابی شناسنامه واقعی در UI | آزمون مثبت و منفی UI→API→Oracle→Readback با داده آزمایشی مجاز، تکرار/رقابت شماره و Integrity اقلام | تأیید شواهد هر BUC؛ مجوز مستقل DBA/مالک سیاست برای هر تغییر لازم |
| **P3** | R3/R4، BUC-06/07/08/14 | اتصال ضامن/بیمه/ارزیابی مصوب و محاسبه ظرفیت به شناسنامه واقعی | ثبت/تمدید/ابطال/ارزیابی و بازحساب کفایت همراه با History و Readback | قواعد و داده بانک مصوب؛ انواع و تاریخچه صحیح؛ منع پیش‌فرض‌های نمایشی |
| **P4** | R4/R5، BUC-09/10/11/12/15/16 | تأیید قرارداد Consumer، Permit مستقل، ثبت تراکنش با Capacity Lock، رفع تخصیص، آزادسازی و معرفی مجدد | Evidence اتمی Oracle، Idempotency، جلوگیری از Double Allocation، Rollback و تاریخچه بدون حذف | بانک/DBA/مالک Consumer مجوز داده‌اند؛ Flag نهایی پس از آزمون امن؛ تا آن زمان **OFF** |
| **P5** | R6/R7، همه ۱۶ BUC و ۲۰ SUC | اجرای ۳۲ سناریو، ۱۰ UAT اصلی، ۱۷ مرجع، Integration/Concurrency/Security/Performance و Gate انتشار | ماتریس Evidence با لینک Issue/Commit/Build/Oracle/UAT و امضا/تصمیم مستقل | Gate خروج هر فاز مطابق `CLT-ROADMAP-R1.0`، بدون تغییر دامنه یا اعلام قبولی ساختگی |

**قانون:** P1..P5 صرفاً «اولویت اجرا» هستند؛ **فاز جدید یا نام کلاس/فایل/API نیستند**. عنوان‌ها و ترتیب رسمی R0..R7 ثابت می‌مانند. بسته‌های آتی نام قابلیت‌محور (`CLT-ORACLE-REFERENCE-RECONCILIATION`, `CLT-BUC03-INTRODUCTION-...`) خواهند داشت، نه پیشوند فاز.

## ۳. ماتریس به‌روز ۱۶ Business Use Case

`SRC` یعنی کد/کاندید در سورس گزارش شده است، نه E2E/Acceptance. ستون «مانع» بر اساس Gap Register و اسناد موجود است و پذیرش/تصویب تازه‌ای را القا نمی‌کند.

| BUC | قابلیت | توسعه در سورس | مانع اصلی و موضوع تصمیم | اقدام/شاهد بعدی |
|---|---|---|---|---|
| 01 | داده‌های مرجع | SRC: CRUD و ۱۷ Workspace | مصوبه سیاست‌ها/فعال‌سازی نوع و آزمون ۱۷ مرجع؛ G-10/15 | Oracle catalog + 17/17 CRUD UAT |
| 02 | انتخاب مالک | SRC: Lookup شخص/مالک | قرارداد CIF و صلاحیت/مالکیت واقعی؛ G-09 | Lookup real Party + branch enforcement |
| 03 | معرفی جدید | SRC: Stage/Review و preflight | شناسنامه قطعی، هویت دارایی، شماره و وضعیت مرجع؛ G-14/16/17 | ثبت مثبت/منفی با Oracle Readback |
| 04 | اقلام شمارشی | SRC: کنترل‌ها و Candidate | جدول و قاعده سریال/شمارشی؛ G-03/12 | تست یکتایی/هم‌پوشانی/Writeback |
| 05 | اصلاح قبل از تعهد | SRC: Stage و تصمیم بازبین | Atomic Amendment و سیاست وضعیت؛ G-01/03/14 | ثبت اصلاحیه + تاریخچه Before/After |
| 06 | ضامن | SRC: Stage و Candidate ثبت اتمیک | حداقل ضامن، اعتبار و Unique فیزیکی؛ G-05/11، D-04 | سناریوی ضامن معتبر/نامعتبر Oracle |
| 07 | بیمه | SRC: Create/Renew/Cancel Candidates | پوشش لازم، شرکت معتبر، سیاست لغو، Unique؛ G-04/06، D-05/07 | ثبت و تمدید/لغو با اثر پوشش |
| 08 | ارزیابی | SRC: Finalization Candidate | FINAL canonical، دوره، ارزیاب و Currency؛ G-07/18، D-06 | تاریخچه ارزش و کنترل اعتبار زمانی |
| 09 | تخصیص | SRC: Stage + Candidate Atomic Posting | Permit/Consumer، Idempotency، قواعد ظرفیت و G-02/09/10/13/16 | Atomic write+capacity lock+replay/rollback |
| 10 | رفع تخصیص | SRC: Stage + Candidate Atomic Unposting | دستور دامین صاحب تعهد، وضعیت/مانده/Branch؛ G-09 و قرارداد BUC-10 | Partial/Full unallocation و Readback |
| 11 | آزادسازی نهایی | SRC: Candidate با Permit و چرخه interlock | تأیید خروج همه تعهدها، Permit/Schema/Status و تراکنش باز | منع با تخصیص فعال و Release موفق کنترل‌شده |
| 12 | معرفی مجدد | SRC: Candidate با پیوند هویت قبلی | BUC-11 پذیرفته‌شده، G-14/17 و شماره جدید | Chain جدید بدون بازاستفاده شماره/سوابق |
| 13 | استعلام با شماره | SRC: Direct lookup UI/API | طرح شماره/یکتایی و داده واقعی؛ G-17 | Lookup مثبت و کنترل عدم افشا |
| 14 | کسری پوشش | SRC: Snapshot/Report | سیاست حداقل، ارزش جاری و ارتباط تخصیص؛ G-10/09 | سناریوی undercoverage با داده واقعی |
| 15 | استفاده بین‌واحدی | SRC: Grants و Revalidation Candidate | اختیار مالک/مصرف‌کننده، تصویب G-13 و قرارداد شعب | مجوز دوگانه، revoke، مصرف ظرفیت اتمی |
| 16 | تاریخچه | SRC: Oracle transactions/status read models | شواهد واقعی DML و یکپارچگی Audit | Readback رویداد هر BUC و تطبیق مانده/تراکنش |

**پذیرش:** برای همه ۱۶ BUC وضعیت نهایی `BANK_ACCEPTED` هنوز شواهد ندارد. ثبت `PASS` دستی تحلیلگر بدون Readback/مصوبات لازم، Gate بانک را بسته نمی‌کند. ۲۰ SUC، ۱۴ BR و قراردادهای API باید به شواهد همسان هر BUC لینک شوند، نه صرفاً به صفحه UI.

## ۴. تصمیمات وابسته و اولویت‌بندی Gapها

| دسته | Gap/موضوع | اقدام تصمیم‌گیر و مالک نقش | اثر بر اجرا |
|---|---|---|---|
| **BLOCKER برای اولین شناسنامه** | G-03، G-12، G-14، G-17، G-16 در صورت ثبت تراکنش | مالک داده CLT + معماری/DBA + مالک سیاست شماره | BUC-03/04/13 و تست تراکنش را مسدود می‌کند |
| **BLOCKER برای پوشش و ظرفیت** | G-04/05/06/07/10/11/18، D-04..07 | کسب‌وکار وثایق + DBA + ارزیابی/بیمه | BUC-06/07/08/14 |
| **BLOCKER برای Posting بانکی** | G-02/09/13/16، Mapping `MODULE_ID` و قراردادهای R5 | Domain Consumer + بانک/DBA + مالک Integration | BUC-09/10/11/12/15؛ Flagهای مالی باید خاموش بمانند |
| **BLOCKER برای شواهد پایدار UAT** | ۳ جدول و ۱ Sequence کمکی Candidate مربوط به `business-uat` | DBA/مالک حاکمیت داده | در صورت عدم تصویب: JSON جایگزین؛ اعلام ذخیره Oracle ممنوع |
| **BLOCKER برای خروج فازها** | ۳۲ سناریو، ۱۰ UI-UAT، ۱۷ مرجع، ۲۰ SUC و BRها | تحلیلگر + نماینده بانک + QA | R6/R7 بسته نمی‌شود |

موضوعات `G-19..G-22` در چند Checkpoint به‌شکل قراردادی مطرح شده‌اند؛ **شماره و عنوان دقیق باید پیش از درج به فهرست رسمی شکاف‌ها با مرجع تطبیق شود**. هیچ Gap صرفاً با ایجاد SQL Candidate، نصب موفق یا تست شبیه‌سازی‌شده CLOSED نمی‌شود.

## ۵. معیار Done / Definition of Evidence برای نسخه بعدی

- **Readiness:** از اتصال واقعی فقط‌خواندنی، وضعیت Object/Column/Constraint/Sequence، تعداد رکورد مرجع/فعال و `PENDING` در برابر `PENDING_APPROVAL` همراه با Timestamp و Branch/Schema قابل بازتولید ثبت شود. اتصال ناموفق = `UNKNOWN`، نه `EMPTY`.
- **Build:** خروجی کامل Maven/Angular و JAR runtime همراه با SHA و تغییرات واقعی Git Diff؛ Smoke روشن شدن برنامه با همان `bin/start.cmd`. اسکریپت نصب یک‌مرحله‌ای با Precheck/Backup/Rollback؛ سه HTML سفارشی بیمه، ضامن، ارزیابی محفوظ.
- **Business:** حداقل یک سناریوی BUC-03 و BUC-13 با داده معتبر، Readback و نتیجه تحلیلگر؛ در نبود تصویب شماره/Schema، وضعیت `BLOCKED` و شاهد دقیق، نه ثبت جعلی.
- **Financial:** هیچ Permit یا JWT ساختگی و هیچ Post مالی به‌نام تحلیلگر تولید نشود. Candidateها تا تصمیم رسمی بانک خاموش می‌مانند. UAT محلی غیرمالی بدون نیاز به JWT مرورگر ادامه دارد.
- **Release:** برای هر BUC ستون‌های `Implemented`, `Build`, `Oracle`, `UAT`, `Bank`, `Gap`, `Next Action`, `Owner Role` و لینک به Diff/Commit/QA موجود باشد؛ بدون اثبات، مقدار `NOT_VERIFIED` ثبت شود.

## ۶. گام مهندسی بعدی و مرز تحویل

**نام قابلیت پیشنهادی:** `CLT-ORACLE-REFERENCE-RECONCILIATION` (صرفاً نام قابلیت، نه نام فاز جدید).  
**محدوده:** Read-only inventory و reconciliation بر روی **DataSource مشترک** برنامه، تطبیق Reference و شناسنامه، شفاف‌سازی موانع BUC-03/04/13 در UI فعلی؛ بدون UI مستقل جدید، Connection Pool/Starter مجزا، DDL یا IAM.  
**گام بعد از آن:** اجرای مسیر مجاز معرفی وثیقه و جست‌وجوی شماره در Oracle با داده و قواعد مورد تأیید بانک؛ سایر Use Caseها بر همان شناسنامه واقعی متصل می‌شوند.

### پیوندهای مرجع

- [نقشه راه ثابت](CLT-ROADMAP-R1.0-FA.md)
- [Gap Register](CLT-GAP-REGISTER-R1.0-FA.md)
- [Checkpoint 16 BUC](CLT-USECASE-DELIVERY-CHECKPOINT-2026-11-10.md)
- [ماتریس UAT یکپارچه](CLT-R6-BUC-SUC-INTEGRATED-UAT-2026-11-10.md)
- [آخرین قرارداد شواهد UAT](CLT-BUC-SUC-UAT-ORACLE-EVIDENCE-2026-11-10.md)
- [قواعد نام‌گذاری پایدار](CLT-STABLE-DOMAIN-NAMING-2026-11-10-FA.md)

## Execution checkpoint: P1 source implementation (2026-11-10)

| Item | Status | Evidence |
|---|---|---|
| P1 read-only API on shared DataSource | SOURCE_IMPLEMENTED | `CollateralOracleReconciliationController/Service/Policy` |
| 17 reference catalogs, Oracle NUMBER(19), unique, status/default and branch count | SOURCE_IMPLEMENTED | `CLT-ORACLE-REFERENCE-RECONCILIATION-CHECKPOINT-2026-11-10.md` |
| P1 analyst evidence UI and JSON export | SOURCE_IMPLEMENTED | Existing BUC UAT workbench, no new route |
| Developer policy/static/type checks | LOCALLY_TESTED | QA report and source diff in install package |
| Production Maven/Angular build and Windows runtime | NOT_VERIFIED | Requires destination development environment |
| Live Oracle reference data, BUC-03/BUC-13 Oracle readback | NOT_VERIFIED | Export reconciliation evidence first |
| Policy/DBA approval, business UAT, bank sign-off | OPEN / NOT_ATTESTED | Never infer from technical metadata |

**No phase promotion:** R0..R7 remain canonical; P1..P5 are execution priorities only. No IAM development, CLT-specific launcher, financial DML, schema migration or permission bypass was introduced. The next action is observe actual Oracle and resolve data/policy gates before claiming BUC-03 and BUC-13 readiness.
