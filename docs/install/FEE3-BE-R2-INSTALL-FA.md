# FEE3-BE-R2 — اتصال کنترل‌شده Angular به Oracle

**نسخه مبنا:** Core Banking Prototype 0.11.0، FEE3-UI-R8+HF1، FEE3-BE-R1. این بسته صرفاً برای محیط توسعه/UAT است و نباید روی بانک عملیاتی نصب شود.

## مرز فاز
- خواندن کاتالوگ، عملیات، کدهای مرجع، فرمول‌ها و مستندات از **Oracle واقعی FEE3**؛ اطلاعات آفلاین به‌عنوان Seed آموزشی مشخص می‌شود.
- دریافت ریز شناسنامه، نسخه‌ها، دامنه، قواعد و آزمون‌ها از API به‌صورت **lazy** هنگام باز کردن فرم.
- ذخیره و ویرایش **فقط نسخه ۱، DRAFT و غیرفعال اقلام جدید `F3-UAT-*`**، با مجوز مستقل UAT، کنترل قفل رکورد، Audit و read-after-write.
- **هیچ ثبت یا تغییر** در اقلام Seed `FCE-*`، نسخه‌های ACTIVE/APPROVED/REVIEW یا وضعیت‌های رسمی صورت نمی‌گیرد.
- ایجاد نسخه دوم، ارجاع رسمی، تأیید، فعال‌سازی، انتشار، Fee Assessment، وصول و Posting همچنان غیرفعال‌اند.
- هیچ DDL و هیچ اجرای Seed جدیدی وجود ندارد. کد از ۱۱ جدول ازپیش‌موجود FEE3 استفاده می‌کند.

## ۱. پیش‌بررسی و نصب ایمن
ZIP را در ریشه پروژه استخراج کنید؛ فایل‌های اصلی پروژه هنوز جایگزین نمی‌شوند و منابع جدید فقط زیر `tools\fee3-backend\releases\r2\` قرار می‌گیرند. سپس از مسیر ریشه پروژه:

```bat
cd /d D:\Projects\core-banking-prototype-v0
python tools\fee3-backend\releases\r2\install.py --preview
```

اگر تمام فایل‌ها `READY` یا `ALREADY_APPLIED` باشند:

```bat
python tools\fee3-backend\releases\r2\install.py --apply
node tools\fee3-backend\verify-r1.mjs
node tools\fee3-backend\verify-r2.mjs
node tools\fee3-backend\verify-r2-mapping.cjs
python tools\fee3-ui\verify-reference-r8.py
bin\stop.cmd
build-production.cmd
bin\start.cmd
```

- وجود `CONFLICT` به معنای تغییر فایل‌های مبناست. **Patch نباید به‌زور اعمال شود**؛ برای آن فایل نسخه جدید را با مبنا مقایسه و Patch تازه تهیه کنید.
- Installer، `build-production.cmd` و `build-production.sh` را به‌صورت **افزایشی** و با حفظ سایر خطوط، به اجرای `verify-r2.mjs` وصل می‌کند. هیچ فایل کامل Build Wrapper جایگزین نمی‌شود.
- پیش از هر جایگزینی، نسخه قبلی فایل‌های موجود در `docs/install/fee3-r2-backup-<timestamp>/` ذخیره می‌شود.
- `--preview` هیچ تغییری نمی‌دهد. `--apply` پس از بررسی تمام فایل‌ها تغییرات را انجام می‌دهد.
- وجود فایل ZIP در ریشه پروژه مجاز نیست؛ پس از استخراج آن را بیرون از Root نگه دارید.

## ۲. کنترل واقعی Oracle قبل از هر نوشتن
Backend R1 دارای مسیرهای خواندنی است:

```bat
curl.exe http://127.0.0.1:8091/api/v1/fee3/readiness
curl.exe "http://127.0.0.1:8091/api/v1/fee3/items?page=0&size=10"
curl.exe http://127.0.0.1:8091/api/v1/fee3/references/sources
```

باید `missingTables=[]`، `readsEnabled=true`، تعداد واقعی ردیف‌ها و اطلاعات معتبر برگردد. شمارش‌های ۲۹۷/۳۰۰ مبنای Seed گذشته‌اند، نه تضمین مقدار زنده فعلی.

## ۳. فعال‌سازی محدود نوشتن در UAT
**فقط در پایگاه داده توسعه اشتراکی، بعد از تأیید Readiness و گرفتن نسخه پشتیبان:**

```bat
set "FEE3_READS_ENABLED=true"
set "FEE3_DRAFT_WRITES_ENABLED=true"
set "FEE3_UAT_ACTOR=FEE3_UAT"
set "FEE3_UAT_WRITE_TOKEN=REPLACE_WITH_A_NEW_RANDOM_SECRET_MINIMUM_20_CHARACTERS"
bin\stop.cmd
bin\start.cmd
```

مقدار نمونه `REPLACE_WITH_...` **نباید به‌عنوان رمز واقعی** استفاده شود؛ آن را با یک رشته تصادفی محرمانه و قوی جایگزین کنید.

در UI با انتخاب «ذخیره پیش‌نویس»، پنجره اخذ مجوز موقت ظاهر می‌شود. توکن را **فقط در همان پنجره** وارد کنید؛ نباید در کد، `localStorage`، فایل‌های پروژه یا Log قرار گیرد. اتصال مرورگر باید HTTPS یا Loopback باشد. برای بازگشت به حالت فقط‌خواندنی:

```bat
set "FEE3_DRAFT_WRITES_ENABLED=false"
bin\stop.cmd
bin\start.cmd
```

**این توکن UAT جایگزین Authentication/RBAC سازمانی نیست.** پیش از عملیاتی شدن، احراز هویت معتبر، تفکیک Maker/Checker، مجوزهای ردیفی، CSRF/CORS و ممیزی امنیتی لازم‌اند.

## ۴. نگاشت مدل فرم به Oracle
| مدل Angular | موجودیت/ستون Oracle | قواعد |
|---|---|---|
| `item.title/module/operation/amountKind/variant/ownerDomain/description` | `FCE_CALC_ITEM` | `IS_ENABLED=0`، شناسه و کد با Sequence واقعی |
| `version.from/to/origin/sourceCode/method/taxMode/rounding/reason` | `FCE_TARIFF_VERSION` | مرجع از `FCE_REF_SOURCE.DOCUMENT_NO` به `SOURCE_ID` نگاشت می‌شود |
| `version.financialPolicy` (اجزای مالی/تخفیف/مالیات/زمان‌بندی) | `FCE_TARIFF_VERSION.FINANCIAL_POLICY_JSON` | JSON محفوظ و کنترل تاریخ شروع |
| `item.serviceCurrency/feeCurrency/context` | `FINANCIAL_POLICY_JSON._fee3UiV1` | قرارداد نسخه‌دار با `contractVersion=1`؛ بدون ستون جدید |
| `version.scopes[0]` | `FCE_VERSION_SCOPE` | یک دامنه فیزیکی، کانال/مشتری/محصول/عقد JSON، ارز در `CURRENCY_CODE`، اولویت فنی 100 |
| `version.rules[*]` | `FCE_CALC_RULE` | تنظیمات اختصاصی در `PARAMETERS_JSON` |
| `rule.formula` از نوع کد مرجع | `FORMULA_CODE` | فقط کد معتبر و فعال Seed/Oracle |
| `rule.formula` از نوع عبارت ریاضی سفارشی | `PARAMETERS_JSON._fceEditableExpressionV1` | تعریف ذخیره می‌شود؛ **سرور آن را اجرا نمی‌کند** |
| `version.tests[*].request/expected` | `FCE_TEST_CASE` | `LAST_STATUS_CODE`, `LAST_RESULT_IRR`, `TESTED_AT` دست‌نخورده؛ آزمون محلی، تأیید بانکی نیست |
| `itemRecordVersion/versionRecordVersion` | `RECORD_VERSION` | کنترل همزمانی و خطای 409 |
| ممیزی نوشتن | `FCE_AUDIT_EVENT` | درج در همان تراکنش |

**محدودیت عمدی ارز:** ستون‌های مبالغ `FIXED_AMOUNT_IRR`, `MIN_FEE_IRR`, `MAX_FEE_IRR`, `EXPECTED_IRR` ریالی‌اند. بنابراین تا تعیین مدل ارزی و نرخ تبدیل، **feeCurrency غیر IRR در Backend رد می‌شود**؛ ارز حساب/عملیات می‌تواند مطابق کد ISO معتبر باشد. هیچ تبدیل نرخ فرضی انجام نمی‌شود.

**محدودیت محصول:** قابلیت انتخاب دامنه محصول فقط برای عملیات مجاز است. اصالت محصول در PDL هنوز به‌صورت سرویس تعاملی بررسی نمی‌شود؛ این داده‌ها Draft می‌مانند و مجوز انتشار نمی‌گیرند.

**محدودیت تاریخ:** مبنای قطعی تصمیم سرور، تاریخ روز Oracle با منطقه زمانی Asia/Tehran است؛ frontend تلاش می‌کند روز محلی/بانکی را پیش‌فرض قرار دهد اما سرور تاریخ گذشته را رد می‌کند.

## ۵. سناریوهای UAT
1. کاتالوگ واقعی از Oracle بارگذاری شود؛ حالت آفلاین کاملاً مشخص و بدون ادعای اتصال نمایش داده شود.
2. شناسنامه Seed باز شود و هیچ امکان ویرایش واقعی آن ارائه نشود.
3. قلم جدید خدمت افتتاح حساب ایجاد، روش FIXED و تاریخ جاری انتخاب و با مجوز UAT ذخیره شود؛ پاسخ `F3-UAT-*`, `DRAFT`, `published=false` باشد؛ UI رکورد را مجدداً از Oracle بخواند.
4. فرم را دوباره باز و مبلغ/قاعده را تغییر دهید؛ PUT باید با شماره نسخه رکورد کار کند و Audit تولید کند.
5. همان درخواست PUT با نسخه رکورد قبلی باید `409` شود.
6. فهرست محصول برای صدور دسته‌چک و ارزیابی وثیقه نباید به‌صورت نامرتبط ظاهر شود؛ Backend نیز فهرست غیرمجاز را رد کند.
7. ارز کارمزد USD، تاریخ دیروز، کد منبع نامعتبر، توکن غلط یا Feature Flag خاموش، همگی باید خطا بدهند و هیچ داده‌ای تغییر نکند.
8. آزمون عددی و ارسال برای بررسی محلی همچنان نباید وضعیت Oracle را از DRAFT خارج کنند.

## ۶. محدودیت‌های پذیرش
- Build کامل Angular AOT، Maven واقعی، و آزمون Runtime/Oracle در محیط تولید Patch قابل اجرا نبودند. ابزارهای QA استاتیک/نوع‌سنجی با شبیه‌ساز و آزمون عددی مستقل اجرا شدند؛ این ادعای Test End-to-End نیست.
- قبل از اتصال موتور محاسبه و Workflow، بررسی تعارض تعرفه‌ها، کنترل Scope واقعی PDL، اجرای امن فرمول، نسخه ۲+، و انتشار/فعال‌سازی **در نقشه راه باز** هستند.
- هیچ تغییری در FEE، FEE2، Product Builder، ORG، Kafka، Accounting یا Oracle Seed در این Patch وجود ندارد.
