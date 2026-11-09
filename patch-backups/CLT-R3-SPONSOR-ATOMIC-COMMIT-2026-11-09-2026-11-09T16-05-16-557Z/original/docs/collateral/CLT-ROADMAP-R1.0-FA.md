# نقشه راه مرجع توسعه ماژول وثایق (Collateral Management)

**شناسه:** CLT-ROADMAP-R1.0  
**وضعیت:** APPROVED FOR DEVELOPMENT PLANNING / تصویب‌شده به‌عنوان مبنای ثابت برنامه توسعه  
**تاریخ:** 2026-11-03  
**پروژه:** Core Banking Prototype  
**Baseline سورس:** `core-banking-prototype-v0-2026-11-03-0910.zip` — `VERSION=0.11.0`  
**Baseline تجربه کاربری:** `Collateral_UI_Final_Offline_V1.42_FA.html`  
**مرجع مدل داده:** `Collateral_EA_Model_Candidate.xml` (مدل پیشنهادی) و `CLT-2026-11-1.txt` (استخراج DDL اسکیمای CLT)  
**سطح تصویب:** تثبیت دامنه، مرزبندی ماژول، توالی فازها، کنترل تغییرات و معیارهای پذیرش. به معنای تصویب سیاست‌های بانکی، تغییر Schema، گواهی اجرای Oracle، آمادگی امنیتی یا مجوز استقرار نیست.

## 1. مصوبات مبنایی (Fixed Decisions)

1. ساختار جاری Modular Monolith و یک Backend JAR / Frontend Angular مشترک **حفظ** می‌شود. استخراج Microservice، تغییر به Maven Multi-Module، ایجاد مخزن مستقل یا Git Branch شرط شروع نیست.
2. توسعه Collateral موازی IAM در Workspace مستقل و از Baseline ثابت `0.11.0` آغاز می‌شود. توسعه Collateral نباید کد IAM یا دیگر ماژول‌ها را بازنویسی کند.
3. محدوده Backend تحت `com.behsazan.corebanking.collateral/**`، Frontend تحت `frontend/src/app/features/collateral/**`، SQL و ابزارهای جدید تحت `database/oracle/clt/**` و `tools/clt/**` تعریف می‌شود. این‌ها مسیرهای پیشنهادی توسعه‌اند و فعلاً در سورس ایجاد نشده‌اند.
4. فایل‌های مشترک مانند `frontend/src/app/app.routes.ts`، `frontend/src/app/layout/app-shell.component.html`، `frontend/src/app/app.config.ts`، `backend/pom.xml`، تنظیمات Spring، امنیت سراسری، `VERSION` و اسکریپت‌های انتشار فقط در Integration Workspace تغییر می‌کنند؛ هیچ نسخه قدیمی از آن‌ها کپی/جایگزین نمی‌شود.
5. هنگام ادغام، **آخرین نسخه کامل و معتبر پروژه** مبنا است، نه ZIP اختصاصی Collateral. تمام ماژول‌های موجود (CIF، DPS2، Reference Data، ORG، CAL2، FEE3، PDL، Posting، IAM و...) حفظ می‌شوند.
6. معیار صحت کسب‌وکار: 14 قاعده `BR-01..BR-14` و 16 مورد `BUC`، کنترل‌های سناریویی `SUC-01..SUC-20`، و تعاملات UI V1.42. تغییر رفتار فقط از مسیر Change Record مستند و تأیید مرتبط انجام می‌شود.
7. 17 Endpoint سند API و 14 سرویس سند Service **Target Contracts** هستند، نه سرویس‌های مستقر یا OpenAPI نهایی؛ پس از رفع تصمیمات مربوطه نسخه `CLT-API-v1` تثبیت می‌شود.
8. مدل EA شامل 26 جدول/373 ستون/41 FK است، ولی وضعیت خود همه 26 کلاس `Proposed` است؛ فقط Package `Collateral Domain` در فایل EA وضعیت `Approved` دارد. بنابراین برچسب نام فایل SQL «Approved XMI» مجوز استقرار نیست.
9. فایل `CLT-2026-11-1.txt` یک **DDL export** از اسکیمای `CLT` است، نه گزارش اجرای query زنده، شمارش رکوردها یا مجوز تغییر. هر DDL/DML روی Oracle ممنوع تا زمان تطبیق read-only و تصویب Migration.
10. مقادیر مشتری، دارایی، بیمه‌گر، ضریب پذیرش و سیاست در UI آفلاین نمونه‌اند. فقط Seed فنی مجاز است *پس از بررسی* برای محیط آزمایشی استفاده شود؛ ورود Policy عملیاتی بدون تصویب بانک ممنوع است.
11. Idempotency، کنترل نسخه `RECORD_VERSION`، محافظت رقابت روی ظرفیت، Audit، تفکیک وظایف و مجوز شعبه باید در Backend اجرا شوند؛ اتکا به اعتبارسنجی UI یا Header هویت دلخواه ممنوع است.
12. تست فنی قبل از تحویل توسط مسیر توسعه انجام می‌شود؛ کاربر عمدتاً UAT عملکردی را در UI بررسی می‌کند. Oracle Write و فعال‌سازی یکپارچه‌سازی واقعی در گیت‌های پایانی هستند.

## 2. خط مبنای بررسی 12 پیوست + سورس پروژه

| ورودی | دامنه/نتیجه تأییدشده با بررسی ایستا |
|---|---|
| `Collateral_UI_Final_Offline_V1.42_FA.html` | مرجع رفتار و نمایش؛ شبیه‌ساز دارای state مرورگر و داده‌های نمونه؛ 15 مسیر؛ هیچ REST/Oracle واقعی |
| `Collateral_Business_Use_Cases_FA.html` | 16 مورد کاربرد `BUC-01..16`؛ 14 قاعده `BR-01..14` |
| `Collateral_System_Use_Cases_FA.html` | 20 مورد `SUC-01..20` و 15 شکاف `G-01..15` |
| `Collateral_Service_Specification_FA.html` | 14 سرویس `SVC-01..14`، چرخه وضعیت، ظرفیت، کنترل تراکنش |
| `Collateral_API_Contract_FA.html` | 17 Endpoint هدف، Bearer/ETag/Idempotency/Correlation و شکاف‌ها |
| `Collateral_UI_Specification_FA.html` | 15 صفحه/مسیر، 10 آزمون `UAT-01..10`، مجوز و RTL |
| `Collateral_User_Guide_FA.html` | 10 گام کاربری، تفکیک محیط عملیاتی و ستاد، هشدار آفلاین |
| `Collateral_DB_Specification_FA.html` | 26 جدول، 373 ستون، 26 PK، 46 UNIQUE، 161 CHECK، 41 FK، 49 تعریف Index در XMI |
| `Collateral_EA_Model_Candidate.xml` | همان 26 کلاس جدول، همگی Proposed؛ 3 Package (7 دامنه، 2 Trace، 17 Reference) |
| `Collateral_Oracle_Schema_From_Approved_XMI.sql` | DDL پیشنهادی برای اسکیمای خالی؛ 26 ستون `IDENTITY`، 47 `CREATE INDEX` مستقل |
| `Collateral_Oracle_Reference_Data_Seed.sql` | 53 رکورد فنی در 11 جدول؛ برای سیاست‌های بانکی و داده نمایشی Seed ندارد |
| `CLT-2026-11-1.txt` | 26 جدول با همان 373 نام ستون، 26 Sequence با `NEXTVAL`؛ اختلاف ساخت/نام‌گذاری و 3 نوع ستون با DDL مشتق‌شده |
| `core-banking-prototype-v0-2026-11-03-0910.zip` | نسخه کامل `0.11.0`؛ فاقد ماژول اختصاصی `collateral` در Backend/Frontend و فاقد `database/oracle/clt` |

### 2.1. تمایزهای فیزیکی مهم

- مدل پیشنهادی: 26 ستون Identity. استخراج `CLT`: شناسه‌ها با 26 Sequence مستقل تأمین می‌شوند. **هیچ `CREATE TABLE` جدید روی CLT موجود اجرا نشود.**
- اختلاف‌های تایپی شناسایی‌شده: `COLLATERAL_ATTRIBUTE_DEFINITION_ITEM.TEXT_VALUE`: `VARCHAR2(4000 BYTE)` در SQL مدل در برابر `VARCHAR2(4000 CHAR)` در CLT؛ `COLLATERAL_GROUP.CREATED_BY`: `VARCHAR2(100 CHAR)` در برابر `VARCHAR2(100)`؛ `COLLATERAL_INSURANCE_TYPE.COLLATERAL_INSURANCE_TYPE_CODE`: `VARCHAR2(50 CHAR)` در برابر `VARCHAR2(50)`.
- شمار PK/FK/UK/CHECK در دو متن برابر به دست آمد (26/41/46/161)، اما نام قیود و Indexها متفاوت است؛ همسانی *منطقی* تمام expressions به‌صورت خودکار تأیید نشده است. Validation روی Data Dictionary واقعی ضروری است.
- `COLLATERAL_TRANSACTION.TRANSACTION_STATUS_CODE` مقدار پیش‌فرض `PENDING` دارد، درحالی‌که Seed جدول `REF_COLL_TXN_STATUS` کد `PENDING_APPROVAL` دارد و `PENDING` ندارد. این **G-16 مستقل** است و قبل از اولین نوشتن باید برطرف شود.
- الگوریتم شماره انتظامی در UI به شکل «کد نوع + سال شمسی + شماره مشتری + توالی 3رقمی + رقم کنترل» است؛ طول نهایی با `COLLATERAL.COLLATERAL_NUMBER NUMBER(19)` و اندازه شناسه واقعی CIF باید قبل از اجرا بررسی شود؛ کد نوع می‌تواند تا پنج رقم باشد. این **G-17 مستقل** است. شماره انتظامی باید در API به‌صورت رشته برای حفظ دقت و صفرهای احتمالی حمل شود و Backend در محدوده فیزیکی تأیید کند.

## 3. مرز دامنه و وابستگی‌ها

**مالکیت Collateral:** معرفی/شناسنامه وثیقه، ویژگی‌های اختصاصی، ضامن، بیمه‌نامه، تاریخچه ارزیابی مرتبط با وثیقه، تخصیص و ظرفیت، آزادسازی، Audit و گزارش.

**مالکیت خارج از Collateral:** مشتری/Party در CIF؛ سپرده/حساب و Hold در DPS2؛ ساختار شعبه در ORG؛ هویت، Authorization و SoD در IAM؛ اطلاعات تسهیلات/ضمانت‌نامه/LC در دامین‌های مصرف‌کننده؛ DMS و FX در سرویس‌های مستقل. Collateral فقط از Contract/Port/Adapter استفاده می‌کند.

**BIAN:** تفکیک `Collateral Allocation Management` از `Party Asset Directory` باید در مدل مفهومی ثبت شود؛ شرح BIAN v14 مدیریت ارزش دارایی را در Party Asset Directory قرار می‌دهد. در Prototype فعلی، ذخیره تاریخچه ارزیابی در CLT طبق مستندات ارسالی پذیرفته‌شده برای طراحی موقت است؛ در ADR طراحی باید محل مرجع نهایی ارزیابی و نحوه همگام‌سازی با Party Asset Directory تصمیم‌گیری شود. منبع: https://bian.org/servicelandscape-14-0-0/object_15.html?object=40635

### Integration Ports (قرارداد هدف)

| Port | مبنا/مالک | وضعیت تصمیم |
|---|---|---|
| `PartyLookupPort` | `GET /api/v1/cif/parties/{partyId}` در سورس موجود | Adapter با کنترل KYC و شناسه Party؛ نیازمند اعتبارسنجی قرارداد |
| `DepositHoldPort` | DPS2 `POST /api/v1/deposit-accounts/{accountId}/holds` و `.../{holdId}/release` | فقط برای وثیقه سپرده‌ای؛ `COLLATERAL_PLEDGE`, `COLLATERAL`, `ORIGIN_ONLY`؛ قرارداد امنیتی و ترتیب تراکنش باید تعیین تکلیف شود |
| `ConsumerObligationPort` | تسهیلات/ضمانت‌نامه/LC | قرارداد شماره پرونده، بدهکاران، مبلغ و وضعیت؛ Adapter آزمایشی فقط در Dev |
| `OrganizationAuthorizationPort` | ORG + IAM | مرز شعبه، مجوز نوع و معرفی؛ توکن معتبر و تفکیک وظایف |
| `ReferencePolicyPort` | جداول تخصصی `CLT` با مالکیت سیاست و داده مرجع | تعاریف فنی از سیاست مصوب بانک جدا است |
| `AssetDirectoryPort` | BIAN Party Asset Directory / مرجع دارایی آینده | طراحی و تصمیم مالکیت داده پیش از اتصال رسمی |
| `FxRatePort`، `DocumentPort` | سرویس‌های ارزی/DMS | بدون اتصال واقعی هیچ عملیات مالی ارزی یا سندی واقعی ادعا نمی‌شود |

## 4. نقشه راه نسخه‌دار و ثابت (R0..R7)

> **قانون:** شناسه، نام و ترتیب فازهای R0 تا R7 در تحویل‌های بعدی تغییر نمی‌کند؛ تغییر دامنه فازها فقط از طریق Change Request نسخه‌دار امکان دارد. کار در هر فاز می‌تواند به Feature Slices تجمیع‌شده تقسیم شود. هر تحویل باید وضعیت تکمیل، باقی‌مانده، مانع، گام بعد و شواهد Diff/Build/UAT را ثبت کند.

| فاز | عنوان قطعی | دامنه و تحویل قابل مشاهده | معیار خروج (Gate) |
|---|---|---|---|
| **R0** | تحلیل و تثبیت خط مبنا | این نقشه راه، Gap Register، مشخصات Baseline، ADRهای معماری، تعیین مالکیت فایل‌ها و تطبیق ایستای XMI/CLT | ثبت 12 منبع + سورس؛ تمام شکاف‌ها شماره‌دار؛ هیچ DDL/DML یا تغییر کد |
| **R1** | اطلاعات پایه و حاکمیت قواعد | محیط ستاد، کاتالوگ گروه/نوع/ویژگی/بیمه/ارز/وضعیت، Read API و نسخه‌بندی رکورد، کنترل تغییر و Audit | کنترل یکتایی، نقش ستاد، Seed صرفاً فنی، G-10 و G-15 تعیین تکلیف؛ UAT ستاد |
| **R2** | شناسنامه، معرفی و جست‌وجو | فهرست/جزئیات، دسترسی مستقیم با شماره انتظامی، استعلام Party، معرفی و ویژگی‌ها، اقلام شمارشی، درخواست اصلاحیه | BR-02/03/11، G-01/03/12/17 حل؛ شماره انتظامی پایدار؛ UAT-01/02/07/08 |
| **R3** | ضامن، بیمه و ارزیابی | ثبت/تأیید/لغو ضامن، بیمه و تمدید، ارزیابی و تجدید ارزیابی، انقضای دوره‌ای | BR-05/06/07/12، G-04/05/06/07/11/18 حل؛ تاریخچه غیرقابل حذف؛ UAT-04/05/09 |
| **R4** | ظرفیت و تخصیص بین‌دامنه‌ای | الگوریتم کفایت و کسری، سیاست مجاز، Consumer API، Idempotency، تخصیص اتمیک و کنترل مجوز بین‌واحدی | BR-08/09/10/13، G-02/09/13/16 حل؛ رقابت و تکرار تست‌شده؛ UAT-03/04/05 |
| **R5** | رفع تخصیص، آزادسازی و سوابق | رفع تخصیص توسط دامین صاحب تعهد، آزادسازی نهایی، معرفی مجدد با شماره جدید، تراکنش‌ها/Status History/Reference Audit | BR-04/14، G-08/14 حل؛ عدم حذف سوابق و بازاستفاده شماره؛ UAT-06 |
| **R6** | تکمیل رابط کاربری و UAT جامع | تکمیل همه 15 صفحه، گزارش ظرفیت و رویداد، داشبورد، دسترس‌پذیری و آزمون طراح/تحلیلگر | 10/10 سناریوی UAT و همه 16 BUC و 20 SUC ردیابی‌شده؛ RTL و نمایش 1366px |
| **R7** | یکپارچه‌سازی، امنیت و آمادگی عملیاتی | اتصال نسخه جاری Core Banking، قرارداد IAM/ORG/CIF/DPS2، Migration کنترل‌شده Oracle، تست Integration/Regression/Performance/Security، Feature Flag و انتشار | تمام G-01..G-18 بسته یا با Waiver رسمی؛ Oracle evidence و مجوز عملیاتی مستقل؛ صفر بازنویسی ناخواسته ماژول‌های دیگر |

**نکته مراحل:** توسعه UI به‌صورت قابل مشاهده از R1 تا R5 انجام می‌شود؛ R6 به معنای شروع دیرهنگام UI نیست، بلکه پوشش نهایی، گزارش و UAT کامل است. شروع R1..R6 وابسته به اتمام IAM نیست، اما فعال‌سازی عملیات واقعی دارای مجوز و R7 به کنترل امنیت معتبر نیاز دارد.

## 5. معیارهای فنی اجباری در تمام مراحل

- **Database:** Oracle در فازهای اولیه Read-Only یا Stub؛ هیچ اجرای خودکار SQL ارسالی؛ مسیر Migration بر اساس اسکیمای موجود CLT و Sequenceها.
- **API:** OpenAPI نسخه‌دار و Schema validation؛ `X-Correlation-Id` و `Idempotency-Key` برای فرمان‌های مالی؛ `If-Match` یا معادل `RECORD_VERSION`؛ قرارداد خطا و عدم افشای منبع غیرمجاز.
- **Concurrency:** `SELECT FOR UPDATE`/قفل مناسب یا Optimistic Lock در مرز تراکنش؛ آزمون دو تخصیص هم‌زمان که با هم از ظرفیت مجاز عبور نکنند.
- **Security:** Deny-by-default برای API Collateral، JWT معتبر، Authorityهای Collateral، کنترل دوگانه مجوز نوع و معرفی برای دسترسی بین‌واحدی، Maker/Checker و عدم اعتماد به `X-User-Id` کاربر.
- **Integration:** بدون خواندن/نوشتن مستقیم Schemaهای CIF/DPS2/ORG؛ فقط API/Adapter و قوانین Ownership.
- **Audit:** ثبت منبع، تغییر، عامل، زمان، شناسه همبستگی و نسخه؛ رکوردهای قطعی قابل پاک‌شدن پنهان نباشند.
- **UAT:** مقایسه با V1.42 بر اساس رفتار و سفر کاربر، نه انتقال خام HTML/JavaScript شبیه‌ساز به Angular.
- **Build:** آزمون Maven و Angular، Contract Tests، Regression سناریوهای موجود پروژه، بررسی Diff فایل‌های مشترک، ثبت SHA و نسخه Master.
- **Release:** هیچ تغییر به `app.routes.ts`, `app-shell.component.html`, IAM Security یا Build از کپی Collateral مستقیماً جایگزین نشود؛ ادغام کنترل‌شده لازم است.

## 6. وضعیت در تاریخ تثبیت (2026-11-03)

| بخش | وضعیت |
|---|---|
| شناسایی و بررسی ایستای 12 سند ارسالی | تکمیل |
| تطبیق اولیه 26 جدول/373 ستون DDL و XMI | تکمیل |
| ثبت G-01..G-17 و اولویت‌ها | تکمیل |
| تصویب توالی ثابت R0..R7 به‌عنوان Baseline توسعه | تکمیل با درخواست کاربر |
| تصویب رسمی تک‌تک تصمیمات Schema/Policy | باز؛ جزو Gateهای فازهای مربوطه |
| تأیید ساختار واقعی Oracle با اتصال و Query خواندنی | انجام‌نشده؛ فقط CLT DDL export بررسی شده |
| پیاده‌سازی Collateral در Java/Angular | شروع‌نشده؛ مطابق درخواست، این تحویل فقط تحلیل/نقشه راه است |
| ادغام با IAM و اجرای UAT | انجام‌نشده |

## 7. قواعد مدیریت تغییر و تحویل آینده

- در هر گزارش: `CLT-ROADMAP-R1.0` و `Baseline source SHA256` ذکر شود.
- هر فاز یک Acceptance Matrix شامل `BUC/SUC/API/UI/DB/BR`، تست، وضعیت و شواهد تولید کند.
- هر Gap باید وضعیت `OPEN -> DECIDED -> IMPLEMENTED -> VERIFIED -> CLOSED` داشته باشد؛ `WAIVED` فقط با علت، مالک و تاریخ مجاز است.
- نسخه‌های Collateral مستقل از محصول شماره می‌خورند (`CLT-R1`, `CLT-R2`, ...)، اما فقط Build یکپارچه نسخه اصلی محصول را تغییر می‌دهد.
- هر تحویل ZIP فقط شامل تغییرات دامنه Collateral + Manifest و شواهد QA باشد؛ هیچ پوشه یا فایل دیگر بدون Change Request مشترک بازنویسی نشود.
- توسعه بدون اجرای Oracle Write قابل شروع است؛ وصل‌کردن فرمان‌های واقعی مالی/سپرده‌ای قبل از احراز هویت و شواهد تراکنش ممنوع است.

**تصمیم اجرایی:** پس از R0، گام بعد **R1 — اطلاعات پایه و حاکمیت قواعد** است. پیش از هر نوشتن عملیاتی، G-16 و G-17 و تصمیم‌های وابسته به آن مسیر باید به‌صورت رسمی resolved شوند.


## پیوست کنترل پیشرفت — CLT-STEP01 / T1 (2026-11-07)

- سند تکمیلی نسخه‌دار: [ماتریس ۱۶ BUC، ۲۰ SUC، ۱۵ نما و گیت‌های داده](CLT-STEP01-FEATURE-COVERAGE-DATA-GATES-FA.md).
- شاهد آزمون جاری: [CLT-R6L-T1-BROWSER-GATE-FA.md](CLT-R6L-T1-BROWSER-GATE-FA.md).
- چارچوب R0 تا R7 تغییر نکرده است؛ گیت T1 منوط به نمایش ۲۸ گروه در مرورگر واقعی است. وضعیت از پیش‌ثبت‌شده R6K: UI=FAIL، نه PASS.
- گیت داده: `CLT.COLLATERAL_GROUP=28`؛ `CLT.COLLATERAL_TYPE=0`؛ `CLT.COLLATERAL=0`. هیچ مجوز Oracle Write صادر نشده است.


## CLT-R6M / T2-T3 (2026-11-07)

- T1: R6L-HF1 real Chrome DOM and analyst screenshot confirmed 28 active Oracle groups; CLT-DEF-001 CLOSED.
- T2: explicit code/title search, 20+8 pagination, native modal detail with Escape and focus, and zero-types state; independent browser acceptance gate added.
- T3: negative empty/error states for registry and introduction without type data; no positive collateral creation gate is opened.
- Detailed matrix: [CLT-STEP01-FEATURE-COVERAGE-DATA-GATES-FA.md](CLT-STEP01-FEATURE-COVERAGE-DATA-GATES-FA.md).
- Implementation/UAT: [CLT-R6M-T2-T3-UI-UAT-FA.md](CLT-R6M-T2-T3-UI-UAT-FA.md).
- Oracle group=28, active types=0, collateral=0; no DDL/DML, no IAM changes. R0-R7 unchanged.


## CLT-R3C / Insurance Catalog Analyst UAT (2026-11-07)

- R3: Insurance Company Reference Data catalog is available through the **existing** `GET /api/v1/collateral/reference/INSURANCE_COMPANY` read-only API; no duplicate API or database table was introduced.
- Read-only insurer list includes inactive Oracle rows. Policy registration selector continues to read only `STATUS='ACTIVE'` records and therefore excludes unverified candidates.
- 42 insurer names and temporary codes were inserted in a **PRECOMMIT** SQL Developer session; persistence in a separate service session is **not yet proven** until Oracle GET returns them.
- R3C makes the catalog accessible in collateral sidebar and insurance panel, documents candidate/approval boundaries, and disables generic reference mutation proposals for `INSURANCE_COMPANY`.
- Open items: current CII license, insurer type, approved business line/territory, bank acceptability, canonical code, G-04, G-06, G-09; NO automatic activation.
- R0–R7 phase names and scope unchanged. CLT-R6M T2 analyst acceptance remains independently open; no T2 or R3 business PASS is implied by this source update.
- Evidence: [CLT-R3C-INSURANCE-CATALOG-UAT-FA.md](CLT-R3C-INSURANCE-CATALOG-UAT-FA.md).

### ثبت وضعیت 2026-11-07 — CLT-RF1 (بدون تغییر فازهای R0 تا R7)

- **R1 / Reference Foundation:** قرارداد عمومی ۱۷ جدول در Backend موجود بود، ولی Smoke قبلی R6K فقط دو Endpoint گروه/نوع (Registry) را می‌سنجید. RF1 Probe فقط‌خواندنی `uat-matrix` و آزمون Endpoint واقعی همه ۱۷ مرجع را اضافه می‌کند؛ وضعیت Runtime تا اجرای واقعی در بانک **NOT PROVEN** باقی می‌ماند.
- **R3 / Insurance:** بسته R3C کاتالوگ نامزدها و لینک پنل بیمه را با آخرین سورس یکپارچه کرده است؛ ۴۲ شرکت تنها پس از COMMIT و HTTP 200 قابل اثبات‌اند. نامزدهای `INACTIVE` فعال یا واجد شرایط استفاده عملیاتی نیستند.
- **R6 / Analyst UAT:** شروع بررسی‌های کارشناس بدون انتظار برای تکمیل جریان عملیاتی همچنان مجاز است. گیت Technical Integration از تأیید عملکردی مستقل است.
- **R7 / Security:** خط مبنای احراز هویت، گیت مجوزهای مرجع و Loopback-only بدون گسترش دسترسی یا IAM حفظ می‌شوند.
- سند شواهد: `docs/collateral/CLT-RF1-REFERENCE-FOUNDATION-17-UAT-FA.md`؛ خروجی محیطی `logs/clt-rf1-reference-matrix.json`.

## پیوست وضعیت CLT-R3D — UAT بیمه‌نامه بدون شناسنامه (2026-11-07)

- فازهای **R0 تا R7 بدون تغییر** باقی می‌مانند. این تحویل در **R3 — ضامن، بیمه و ارزیابی** و آزمون هم‌زمان R6 قرار دارد.
- نتیجه RF1 کاربر: ۱۷/۱۷ جدول Reference قابل خواندن؛ `INSURANCE_COMPANY=42`, `ACTIVE=0`, `INACTIVE=42`, `TMP=42`؛ ۶۶۱ آزمون Java، بدون شکست. این شواهد گیت خواندن را می‌بندد، نه CRUD نوشتنی یا پذیرش بیمه.
- مانع UAT: در مبنای فعلی نوع وثیقه فعال و شناسنامه عملیاتی وجود ندارد و پنل بیمه فقط داخل شناسنامه باز می‌شد.
- قابلیت R3D: همان کامپوننت بیمه‌نامه در تب جدید میزکار تحلیلگر با `analystPreview=true` نمایش داده می‌شود؛ سه GET روی مراجع Oracle، کنترل شکلی مستقل مرورگر و هشت سناریوی قابل ثبت و صدور JSON.
- شرکت‌های `INACTIVE` و کدهای موقت وارد انتخابگر عملیاتی نمی‌شوند؛ تمامی POSTها در Preview مسدودند و ثبت نهایی همچنان خاموش است.
- گیت‌های **G-04/G-06/G-09، DATA-01، DATA-04** باز می‌مانند؛ نتیجه R3D تا آزمون محیط و تحلیلگر `ANALYST_UAT_NOT_RUN` است.
- شرح و معیار پذیرش: [CLT-R3D-INSURANCE-ANALYST-UAT-FA.md](CLT-R3D-INSURANCE-ANALYST-UAT-FA.md).


### CLT-R3E (2026-11-07) — اصلاح تجربه آزمون تحلیلگر در R3/R6
- فازهای R0 تا R7 و مرز کنترل‌های G-04 و G-06 تغییر نکرده‌اند.
- میزکار تحلیلگر: ابزارهای R6D/R6E در بخش بازشونده اختیاری (پیش‌فرض بسته) قرار می‌گیرند.
- فرم بیمه‌نامه R3: برچسب اجباری‌بودن شماره سپرده به‌عنوان قرارداد فعلی Backend و شکاف تصمیم کسب‌وکار تفکیک می‌شود؛ معنای مبلغ دریافتی و مرجع ارز شفاف است.
- هیچ تغییری در Backend، Oracle، داده بیمه‌گر یا مسیر ثبت عملیاتی رخ نمی‌دهد.
- گیت UAT: هشت سناریوی R3-INS همچنان NOT_RUN تا ثبت شواهد توسط تحلیلگر.


## کنترل پیشرفت 2026-11-09 — R2/G-14/G-17 (الحاقی، بدون تغییر Baseline)

**تأیید کاربر:** بسته `CLT-R2-G03-G12-SERIAL-RANGES-2026-11-09` با موفقیت نصب شده است. این شاهد نصب سورس است، نه گواهی Oracle Migration/UAT.

**وضعیت R0..R7 بدون تغییر تعریف/ترتیب:** R0 تثبیت؛ R1 اطلاعات پایه کدنویسی‌شده و پذیرش نهایی باز؛ **R2 در حال پیاده‌سازی** (Worklist، Integrity، G-01، G-03/G-12، G-14 Stage و G-17 preflight)؛ R3 ناقص؛ R4/R5 عملیات قطعی باز؛ R6/R7 آزمون و یکپارچه‌سازی نهایی به انتهای توسعه موکول شده‌اند.

**تحویل این گام:** کنترل شماره انتظامی صرفاً بر مبنای کد نوع Oracle، شماره مشتری واقعی CIF و Data Dictionary ستون `COLLATERAL_NUMBER NUMBER(19,0)`، با گزارش طول، صفر پیشرو و ناسازگاری. کنترل نامزد دارایی Stage به Preflight اضافه شد. در API/DB هیچ شماره انتظامی قطعی صادر یا ذخیره نمی‌شود؛ **G-14/G-17 OPEN** باقی می‌مانند.

**پروتکل تحویل:** فایل مرجع نقشه راه در Repository اصلاح می‌شود؛ سند جزئیات [CLT-R2-G14-G17-IMPLEMENTATION-CHECKPOINT-2026-11-09.md](CLT-R2-G14-G17-IMPLEMENTATION-CHECKPOINT-2026-11-09.md) و Gap Register نیز همراه بسته نصب می‌شوند. هیچ DDL، نوشتن اصلی وثیقه، تغییر IAM یا آزمون UAT در این مرحله انجام نمی‌شود.


## کنترل پیشرفت 2026-11-09 — R2 / کنترل تصمیم بازبین و شواهد Oracle

**آخرین نسخه نصب‌شده تأییدشده توسط کاربر:** `CLT-R2-G14-G17-NUMBERING-PREFLIGHT-2026-11-09`.
**تحویل این گام:** `CLT-R2-REVIEW-DECISION-INTEGRITY-2026-11-09`.

- **ساختار R0 تا R7 بدون تغییر است.** R2 در حال توسعه؛ G-01/G-03/G-12/G-14/G-17 همچنان OPEN در سطح تصویب و بهره‌برداری.
- درخواست `REVIEWED` فقط بعد از بازخوانی واقعی پیش‌نویس از Oracle، کنترل Hash، رخداد Audit، بازخوانی ردیف‌های فرزند اقلام شمارشی، اعتبار جاری CIF و قواعد CLT و نبود تعارض نامزد دارایی با مالک دیگر مجاز است. `RETURNED` برای برگرداندن پیش‌نویس معیوب محفوظ است.
- گزارش آمادگی ثبت قطعی به‌صورت مجزا وضعیت ایجاد Audit، ردیف‌های شمارشی Oracle و تعارض نامزد دارایی را نشان می‌دهد؛ هیچ تأیید عملیاتی یا شماره انتظامی ایجاد نمی‌کند.
- مدارک تحویل: [CLT-R2-REVIEW-DECISION-CHECKPOINT-2026-11-09.md](CLT-R2-REVIEW-DECISION-CHECKPOINT-2026-11-09.md)، Gap Register به‌روزشده و QA-REPORT داخل بسته.
- G-14/G-17 و مرزهای Oracle Migration تغییری نکرده‌اند؛ UAT به درخواست کاربر به انتهای توسعه منتقل است.


## Checkpoint اجرایی 2026-11-09 — R3 / Shared Oracle Intake & Worklists

**آخرین نصب موفق تأییدشده توسط کاربر:** `CLT-R2-REVIEW-DECISION-INTEGRITY-2026-11-09`.
**نسخه تحویلی بعدی:** `CLT-R3-SHARED-DATASOURCE-INTAKE-2026-11-09`.

- توالی و عنوان ثابت **R0 تا R7 بدون تغییر** است. R2 همچنان با G-14/G-17 و Migrationهای تأییدنشده باز است؛ این تحویل شروع توسعه تکمیلی **R3 — ضامن، بیمه و ارزیابی** است. R4/R5 عملیات قطعی باز؛ R6/R7 پس از توسعه نهایی پذیرش می‌شوند.
- سرویس‌های سه‌گانه R3 از اتصال جدید مستقل در نسخه محلی صرف‌نظر کرده و همان `DataSource` برنامه و تراکنش Oracle مشترک را برای **درخواست‌های Stage** به کار می‌گیرند. در محیط غیرمحلی محدودیت‌های قبلی برقرار می‌ماند.
- کارتابل‌های درخواست‌های مرحله‌ای ضامن/بیمه/ارزیابی به فرم‌های اصلی متصل شده‌اند؛ هر درخواست فقط در محدوده کاربر و شعبه درخواست‌دهنده خوانده می‌شود و بعد از ذخیره دوباره دریافت می‌شود.
- `CLT.COLLATERAL_SPONSOR`، `CLT.COLLATERAL_INSURANCE`، `CLT.COLLATERAL_VALUATION_HISTORY` و شماره انتظامی قطعی **هیچ نوشتن جدیدی ندارند**. اسکریپت‌های کاندید R3 همچنان نیازمند تصویب و نصب DBA هستند و خودکار اجرا نمی‌شوند.
- وضعیت نهایی G-04/G-05/G-06/G-07/G-09/G-11/G-18 و G-14/G-17: **OPEN**. نتیجه `stagingSchemaVerified=false` در Oracle مانع فعال‌شدن ذخیره Stage است، نه شاهد شکست نصب سورس.
- مدارک نسخه‌دار: [CLT-R3-SHARED-DATASOURCE-INTAKE-CHECKPOINT-2026-11-09.md](CLT-R3-SHARED-DATASOURCE-INTAKE-CHECKPOINT-2026-11-09.md). UAT به درخواست کاربر به پایان توسعه موکول است.

## Checkpoint اصلاح نصب R3 / HF1 — 2026-11-09

**مبنای نصب موفق تأییدشده توسط کاربر:** `CLT-R2-REVIEW-DECISION-INTEGRITY-2026-11-09`.
**بسته R3 اولیه:** `CLT-R3-SHARED-DATASOURCE-INTAKE-2026-11-09` در مرحله Precheck و پیش از اعمال هر تغییر، با خطای `SOURCE_CHANGED_OR_UNKNOWN` در `collateral-insurance-panel.component.html` متوقف شد. بنابراین نصب R3 قبلی **ناموفق/تأییدنشده** است.
**نسخه اصلاحی:** `CLT-R3-SHARED-DATASOURCE-INTAKE-HF1-2026-11-09`.

- فازهای **R0 تا R7 و نام/ترتیب آنها بدون تغییر** می‌مانند؛ R3 مرحله توسعه است و پذیرش UAT انجام نشده است.
- برای فایل فرم بیمه، نصب دیگر جایگزینی کامل ندارد؛ فقط یک قطعه استاندارد کارتابل درخواست Stage با شرط `!analystPreview` و کنترل ساختاری دقیق پیش از `</section>` الحاق می‌شود. تغییرات موجود سایت/لوگو/قالب بیمه حفظ می‌شوند؛ هر ساختار نامعتبر بدون بازنویسی رد می‌شود.
- سایر 24 فایل کد و مستندات با سیاست SHA-256 محافظت می‌شوند؛ Backup، نصب مجدد بدون تغییر و Rollback قابل بررسی هستند.
- هیچ DDL یا DML در نصب انجام نمی‌شود. ثبت نهایی ضامن/بیمه‌نامه/ارزیابی، فعال‌سازی Stage بدون Migration، G-04..G-07/G-09/G-11 و G-14/G-17 همچنان OPEN هستند.
- گزارش دقیق: [CLT-R3-HF1-INSURANCE-SAFE-MERGE-CHECKPOINT-2026-11-09.md](CLT-R3-HF1-INSURANCE-SAFE-MERGE-CHECKPOINT-2026-11-09.md).

## R3 installation checkpoint - HF2 / 2026-11-09

- User-confirmed last successful baseline: `CLT-R2-REVIEW-DECISION-INTEGRITY-2026-11-09`.
- Both previous R3 intake packages failed **during precheck, before changes were applied**: original version conflicted with the insurer panel HTML; HF1 subsequently conflicted with the sponsor and valuation panel HTML. HF1 was **not installed successfully**.
- `CLT-R3-SHARED-DATASOURCE-INTAKE-HF2-2026-11-09` extends safe, append-only guarded merging to **all three panels** (insurance, sponsor, valuation), retaining customized HTML and failing closed if anchors are missing. All non-HTML files remain SHA-256 guarded.
- Canonical phase plan **R0 through R7 is unchanged**; this is an installer-compatibility correction, not a new R3 domain feature or proof of runtime readiness.
- R3 Stage write prerequisites, database migrations, final registration and G-04/G-05/G-06/G-07/G-09/G-11/G-14/G-17 remain **OPEN**. User UAT is deferred until the end of development.
- Delivery evidence: [CLT-R3-HF2-THREE-PANEL-MERGE-CHECKPOINT-2026-11-09.md](CLT-R3-HF2-THREE-PANEL-MERGE-CHECKPOINT-2026-11-09.md).

## Checkpoint اجرایی 2026-11-09 — R3 / Stage Workflow مشترک

**آخرین نصب موفق تأییدشده توسط کاربر:** `CLT-R3-SHARED-DATASOURCE-INTAKE-HF2-2026-11-09`؛ **مبنای نسخه کامل سورس:** `core-banking-prototype-v0-2026-11-09-1618.zip`.

- **R0 تا R7 بدون تغییر در عنوان، ترتیب یا معیار خروج:** R2 همچنان دارای G-14/G-17 و Migrationهای باز؛ R3 در حال پیاده‌سازی؛ UAT جامع R6 و گیت عملیاتی R7 در انتهای توسعه.
- سه درخواست Stage R3 اکنون دارای کد گردش `DRAFT/SUBMITTED/REVIEWED/REJECTED/WITHDRAWN` و رویداد Audit، کنترل هم‌زمانی و بازاعتبارسنجی Maker هستند؛ Reviewer مستقل با ثبت‌کننده یکی نیست.
- در محیط محلی فقط مسیر POST **Stage** در همان Security Chain مجاز شده، نه ثبت قطعی. سه فایل HTML سفارشی ضامن/بیمه/ارزیابی هیچ تغییر ندارند.
- SQL نمونه وضعیت/رویداد و فیلد دلیل، فقط Candidate با Guard اجرا-نشدنی است؛ اجرای DBA، Oracle Live و UAT تأیید نشده‌اند. هیچ Gap عملیاتی G-04/G-05/G-06/G-07/G-09/G-11/G-14/G-17/G-18 بسته نشده است.
- سند اجرایی: [CLT-R3-STAGE-WORKFLOW-CHECKPOINT-2026-11-09.md](CLT-R3-STAGE-WORKFLOW-CHECKPOINT-2026-11-09.md).


## R3 توسعه تکمیلی — کنترل دوباره قواعد در لحظه Stage Review (2026-11-09)

- **Baseline جدید:** `core-banking-prototype-v0-2026-11-09-1720.zip`؛ مرحله R3 همچنان فعال است، عنوان و ترتیب R0..R7 تغییر نکرده است.
- برای هر سه دامنه ضامن/بیمه/ارزیابی، پیش از انتقال مثبت `REVIEW` قواعد زمان‌حال Oracle/CIF با Scope مستقل Reviewer دوباره اعتبارسنجی می‌شوند؛ Submit همچنان با Scope Maker بررسی می‌شود. REJECT/WITHDRAW بدون نیاز به معتبر بودن مجدد نامزد امکان‌پذیر می‌مانند.
- **گیت بسته نشده:** G-04/05/06/07/09/11/18 و Migration Stage، ثبت نهایی و سیاست عملیاتی همچنان OPEN هستند؛ R2/G-14/G-17 نیز باز باقی مانده‌اند.
- وضعیت تحویل در [CLT-R3-REVIEW-REVALIDATION-CHECKPOINT-2026-11-09.md](CLT-R3-REVIEW-REVALIDATION-CHECKPOINT-2026-11-09.md) ثبت است. UAT جامع R6 در پایان توسعه انجام می‌شود.


## R3 — Checkpoint شواهد تجمیعی بدون ثبت قطعی (2026-11-09)

- **نصب بسته قبلی `CLT-R3-REVIEW-REVALIDATION-2026-11-09` توسط کاربر موفق تأیید شد.** آخرین سورس مرجع توسعه در این تحویل، همان سورس حاصل از نصب آن بسته است، نه ZIP قدیمی `1720`.
- بسته `CLT-R3-LIFECYCLE-EVIDENCE-2026-11-09` یک API خواندنی تجمیعی برای شواهد ضامن/بیمه/ارزیابی از **DataSource مشترک** اضافه می‌کند؛ داده‌های ثبت‌شده را معادل تأیید بانکی نمی‌داند و `eligibleForNewAllocation=false` را ثابت نگه می‌دارد.
- **R3 فعال و باز است**؛ G-04/05/06/07/09/11/18، مدل چرخه عمر، Migration مصوب Oracle و آزمون زنده هنوز بازند. R0..R7 ثابت و R6/UAT جامع همچنان موکول به پایان توسعه است.
- جزئیات: [Checkpoint R3 Lifecycle Evidence](CLT-R3-LIFECYCLE-EVIDENCE-CHECKPOINT-2026-11-09.md).


## R3 — کنترل پیش‌بررسی ثبت قطعی پس از شواهد Lifecycle (2026-11-09)

- **نصب موفق `CLT-R3-LIFECYCLE-EVIDENCE-2026-11-09` توسط کاربر تأیید شد.** مبنای بسته بعدی همین سورس نصب‌شده است.
- تحویل `CLT-R3-POSTING-PREFLIGHT-2026-11-09` روی هر درخواست Stage API و نمایش «آمادگی ثبت قطعی فقط شواهد» اضافه می‌کند؛ Fingerprint و Audit و عدم تکرار فیزیکی و تغییر ارزیابی مرجع بررسی می‌شود.
- نتیجه تکنیکی هرگز اجازه DML نیست؛ `finalPostingEnabled=false` و `policyApproved=false` حتی با کنترل‌های موفق. G-04/G-05/G-06/G-07/G-09/G-11/G-18 و Migration و UAT زنده **OPEN** می‌مانند.
- مستند جدید: [Checkpoint پیش‌بررسی R3](CLT-R3-POSTING-PREFLIGHT-CHECKPOINT-2026-11-09.md) و [ماتریس تصمیم ثبت قطعی](CLT-R3-POSTING-DECISION-MATRIX-FA.md). R0..R7 بدون تغییر است؛ R4/R5 و UAT جامع R6 وارد فاز اجرا نشده‌اند.
