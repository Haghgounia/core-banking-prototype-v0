# R30-HF7 — دفتر جامع اجرا، شکاف‌ها و معیار پذیرش

**نسخه ارزیابی:** `R30-HF7-A2` بر مبنای سورس ۲۰۲۶-۱۰-۱۳، همراه با HF7-SG و HF7-A1. **رده:** Development/UAT Only؛ عدم تأیید بهره‌برداری بانکی.

## ۱. دامنه، منبع و حدود شواهد

- ماتریس مرجع HF7-A1: ۴۷ جدول / ۶۶۱ ستون استخراج‌شده از HTML مرجع مخزن؛ تطبیق با Oracle جاری در این نوبت انجام نشده است.
- فهرست حاکمیت `ProductRuleGovernanceService`: ۲۳ کنترل موجود در پروژه؛ شامل کنترل‌های مستقل محصول‌ساز، نه همه جداول ۴۷گانه.
- قرارداد معنایی هنگام تصویب: ۱۶ جدول دارای کنترل **حداقلی**؛ این عبارت به معنای بررسی کامل قواعد حقوقی/مقرراتی یا آزمون مصرف عملیاتی نیست.
- آزمون ردیابی ایستا: ۲۰۵ فایل Java خارج از پکیج Product Builder و ۲۳ کنترل بررسی شده‌اند؛ نبود ارجاع مستقیم، نبود integration دینامیک را اثبات نمی‌کند.
- هیچ اتصال Oracle، DML، Migration، تغییر داده‌های تاریخ‌دار، Maven/Angular Production Build یا آزمون E2E انجام نشده است.

## ۲. خروجی‌های واقعی A/B/C

| مرحله | انجام‌شده و قابل‌اثبات | شکاف باقیمانده | وضعیت |
|---|---|---|---|
| HF7-A | فهرست فیلد-به-فیلد، قراردادهای حداقلی و تحلیل زمان اعتبار | تصویب معنای NULL، تمام ۴۷ جدول، اولویت و وراثت، روابط والد/فرزند، مستندات بانک/نهاد ناظر | **ممیزی اولیه؛ باز** |
| HF7-B | Backend validate هنگام create/update؛ ممیزی read-only ۱۶ جدول در Approval؛ شناسایی گپ/تداخل زمانی؛ کنترل واحدهای عدد صحیح | UX وابستگی همه فرم‌ها، تعارض بین جداول، ماشین حالت صریح NULL/Unlimited و migration پس از مصوبه | **پیاده‌سازی جزئی؛ باز** |
| HF7-C | جست‌وجوی ایستای مصرف‌کننده Runtime و حفظ fail-closed برای `canOpen` | اتصال واقعی pre-commit در سپرده و چرخه اعطا/پرداخت/وصول تسهیلات، E2E چندکاناله، rollback و همزمانی | **عدم احراز** |

## ۳. ماتریس ۴۷ جدول (شناسنامه وضعیت واقعی این نسخه)

راهنما: `A2-PARTIAL` یعنی صرفاً حداقل کنترل مستقل هنگام ثبت و تصویب؛ `GOV-CATALOG` یعنی موجود در گزارش حاکمیت اما ممیزی معنایی جامع ندارد؛ `WRITE/REF` یعنی شناسایی شده ولی گواهی Approval در HF7-A2 ندارد. C برای هیچ جدول «تکمیل‌شده» نیست.

| # | جدول | شرح مدل مرجع | ممیزی کنونی | ردیابی اجرای عملیاتی |
|---:|---|---|---|---|
| 1 | `PRODUCT` | محصول بانکی | `WRITE/REF` | گواهی Runtime ندارد |
| 2 | `PRODUCT_LEGACY_MAPPING` | نگاشت میراثی محصول | `WRITE/REF` | گواهی Runtime ندارد |
| 3 | `PRODUCT_RELATIONSHIP` | رابطه بین محصولات | `GOV-CATALOG` | گواهی Runtime ندارد |
| 4 | `PRODUCT_VERSION` | نسخه محصول | `WRITE/REF` | گواهی Runtime ندارد |
| 5 | `PRODUCT_VERSION_MODULE` | ماژول‌های نسخه محصول | `WRITE/REF` | گواهی Runtime ندارد |
| 6 | `PRODUCT_CHANNEL_OPERATION` | عملیات مجاز کانال | `WRITE/REF` | گواهی Runtime ندارد |
| 7 | `PRODUCT_CHANNEL_RULE` | قاعده مشترک کانال | `A2-PARTIAL` | گواهی Runtime ندارد |
| 8 | `PRODUCT_ELIGIBILITY_RULE` | قاعده مشترک اهلیت | `A2-PARTIAL` | گواهی Runtime ندارد |
| 9 | `PRODUCT_ORG_SCOPE` | محدوده سازمانی محصول | `A2-PARTIAL` | گواهی Runtime ندارد |
| 10 | `PRODUCT_PRICING_COMPONENT` | اجزای قیمت‌گذاری | `WRITE/REF` | گواهی Runtime ندارد |
| 11 | `PRODUCT_PRICING_RULE` | قاعده قیمت‌گذاری مشترک | `GOV-CATALOG` | ارجاع مستقیم در سورس (نه گواهی E2E) |
| 12 | `PRODUCT_RATE_TIER` | پله‌های نرخ | `WRITE/REF` | گواهی Runtime ندارد |
| 13 | `PRODUCT_REQUIRED_DOCUMENT` | مدارک الزامی محصول | `A2-PARTIAL` | گواهی Runtime ندارد |
| 14 | `PRODUCT_REQUIRED_INQUIRY` | استعلام‌های الزامی محصول | `A2-PARTIAL` | گواهی Runtime ندارد |
| 15 | `DEPOSIT_PRODUCT_ALLOWED_TERM` | مدت‌های مجاز سپرده | `WRITE/REF` | گواهی Runtime ندارد |
| 16 | `DEPOSIT_PRODUCT_CLOSURE_APPROVAL_RULE` | قواعد تأیید بستن سپرده | `WRITE/REF` | گواهی Runtime ندارد |
| 17 | `DEPOSIT_PRODUCT_CLOSURE_PRECHECK` | کنترل‌های پیش از بستن سپرده | `WRITE/REF` | گواهی Runtime ندارد |
| 18 | `DEPOSIT_PRODUCT_CLOSURE_RULE` | قواعد بستن و فسخ سپرده | `A2-PARTIAL` | گواهی Runtime ندارد |
| 19 | `DEPOSIT_PRODUCT_CLOSURE_SETTLEMENT_RULE` | قواعد تسویه در زمان بستن سپرده | `WRITE/REF` | گواهی Runtime ندارد |
| 20 | `DEPOSIT_PRODUCT_DORMANCY_RULE` | قواعد راکدی سپرده | `A2-PARTIAL` | گواهی Runtime ندارد |
| 21 | `DEPOSIT_PRODUCT_HOLD_RULE` | قواعد مسدودی سپرده | `A2-PARTIAL` | گواهی Runtime ندارد |
| 22 | `DEPOSIT_PRODUCT_JOINT_RULE` | قواعد مالکیت مشترک سپرده | `A2-PARTIAL` | گواهی Runtime ندارد |
| 23 | `DEPOSIT_PRODUCT_OPENING_RULE` | قواعد افتتاح سپرده | `A2-PARTIAL` | گواهی Runtime ندارد |
| 24 | `DEPOSIT_PRODUCT_PROFILE` | پروفایل سپرده | `GOV-CATALOG` | گواهی Runtime ندارد |
| 25 | `DEPOSIT_PRODUCT_TERM_RULE` | قواعد مدت، سررسید و تمدید سپرده | `GOV-CATALOG` | گواهی Runtime ندارد |
| 26 | `DEPOSIT_PRODUCT_TRANSACTION_RULE` | قواعد تراکنش سپرده | `A2-PARTIAL` | گواهی Runtime ندارد |
| 27 | `DEPOSIT_PRODUCT_WITHDRAWAL_MEDIA` | ابزارهای مجاز برداشت سپرده | `A2-PARTIAL` | گواهی Runtime ندارد |
| 28 | `LOAN_ELIGIBILITY_EXTENSION` | افزونه اهلیت تسهیلات | `WRITE/REF` | گواهی Runtime ندارد |
| 29 | `LOAN_FINANCIAL_EXTENSION` | افزونه مالی تسهیلات | `A2-PARTIAL` | گواهی Runtime ندارد |
| 30 | `LOAN_PRODUCT_COLLATERAL_RULE` | قواعد وثیقه محصول تسهیلات | `A2-PARTIAL` | گواهی Runtime ندارد |
| 31 | `LOAN_PRODUCT_PROCESS_RULE` | قواعد فرایندی محصول تسهیلات | `A2-PARTIAL` | گواهی Runtime ندارد |
| 32 | `LOAN_PRODUCT_PROFILE` | پروفایل تسهیلات | `GOV-CATALOG` | گواهی Runtime ندارد |
| 33 | `LOAN_PRODUCT_REPAYMENT_RULE` | قواعد بازپرداخت محصول تسهیلات | `A2-PARTIAL` | گواهی Runtime ندارد |
| 34 | `DOCUMENT_TYPE` | نوع مدرک | `WRITE/REF` | گواهی Runtime ندارد |
| 35 | `ECONOMIC_SECTION` | بخش اقتصادی | `WRITE/REF` | گواهی Runtime ندارد |
| 36 | `ECONOMIC_SUB_SECTION` | زیربخش اقتصادی | `WRITE/REF` | گواهی Runtime ندارد |
| 37 | `FACILITY_OPERATION_PARAMETER` | پارامترهای عملیات تسهیلات | `WRITE/REF` | گواهی Runtime ندارد |
| 38 | `LOAN_PRODUCT_COLLATERAL` | انواع وثیقه محصول تسهیلاتی | `WRITE/REF` | گواهی Runtime ندارد |
| 39 | `LOAN_TYPE` | انواع عقد تسهیلات | `WRITE/REF` | گواهی Runtime ندارد |
| 40 | `LOAN_TYPE_PLAN_TYPE` | ارتباط نوع عقد و نوع طرح | `WRITE/REF` | گواهی Runtime ندارد |
| 41 | `LOAN_USAGE` | کاربرد تسهیلات | `WRITE/REF` | گواهی Runtime ندارد |
| 42 | `OPERATION` | انواع عملیات | `WRITE/REF` | گواهی Runtime ندارد |
| 43 | `PATTERN_OPERATION` | الگوی عملیات | `WRITE/REF` | گواهی Runtime ندارد |
| 44 | `PATTERN_OPERATION_DETAIL` | جزئیات الگوی عملیات | `WRITE/REF` | گواهی Runtime ندارد |
| 45 | `PATTERN_OPERATION_LOAN_TYPE` | الگوی عملیات نوع عقد | `WRITE/REF` | گواهی Runtime ندارد |
| 46 | `PLAN_TYPE` | انواع طرح‌های تسهیلات | `WRITE/REF` | گواهی Runtime ندارد |
| 47 | `SUB_OPERATION` | ریزعملیات | `WRITE/REF` | گواهی Runtime ندارد |

## ۴. قراردادهای قطعی کد و تصمیم‌های نیازمند مصوبه

| موضوع | تصمیم محافظه‌کارانه پیاده‌شده | تصمیم کسب‌وکاری/مقرراتی باز |
|---|---|---|
| `IS_ALLOWED` تراکنش و ابزار برداشت | الزام ۰/۱ و سقف مثبت برای حالت مجاز؛ حدود حالت غیرمجاز باید خالی باشند | مدل `UNSPECIFIED / BOUNDED / NO_PRODUCT_LIMIT_APPROVED / NOT_APPLICABLE`، حدود کانال/تعداد/مبلغ و تعارض بین کنترل‌ها |
| اعتبار نسخه | یک کنترل REQUIRED باید در کل بازه نسخه دست‌کم پوشش زمانیِ فعال داشته باشد؛ گپ/ردیف آینده نامعتبر بلوکه می‌شود | پوشش جداگانه دامنه مشتری، کانال، ارز، محصول/استثنا و قواعد override |
| مشترک اهلیت | مقدارهای سن، سابقه، مانده و PEP در سطح فیلد کنترل می‌شوند | مدل رسمی شرایط چندبعدی، قواعد AML/KYC، source of truth و precedence |
| مشترک کانال و سازمان | تصمیم مجازبودن صریح؛ سازمان با شناسه مثبت صحیح؛ استعلام/مدرک با stage و الزام | استثناهای کانالی، تأیید هویت، parent/child و پروتکل خطای سرویس |
| افتتاح و بستن سپرده | bounds سازگار و شمارش/مدت صحیح؛ عدم امکان افتتاح متناقض | حدود بدون سقف، تمبر و هزینه، قوانین خاتمه، ترتیب مالی/حقوقی تسویه |
| تسهیلات | بازپرداخت و مدت صحیح؛ وابستگی تنفس/پیش‌پرداخت و نسبت آورده کنترل می‌شوند | هویت عقد، روش تصویب، فرمول نرخ و سهم آورده، ضامن/وثیقه و طبقه‌بندی مطالبات |
| انتشار/اجرا | `canApprove` مستقل از `canOpen`؛ محافظ LOAN و سپرده برقرار است | تأیید UAT اتصال Runtime به نسخه دقیق Rule و ردپای تصمیم‌ها |

## ۵. یافته‌های واقعی HF7-C (صرفاً اسکن ایستا)

در ۲۳ کنترل موجود، تنها دو نام جدول ارجاع مستقیم در Java خارج از Product Builder داشتند:

1. `PRODUCT_PRICING_RULE` در `deposit/account/profit/oracle/DepositProfitRepository.java`.
2. `CORRESPONDENT_ACCOUNT_PRODUCT_PROFILE` در `deposit/account/waved/oracle/DepositAccountWaveDRepository.java`.

برای ۲۱ کنترل دیگر نام مستقیم جدول در ۲۰۵ فایل Java اسکن‌شده مشاهده نشد. این **اثبات نبود مصرف غیرمستقیم** نیست و دو مورد دارای ارجاع هم بدون E2E و trace مجوز عملیاتی نمی‌گیرند.

## ۶. معیار خروج قطعی از HF7 و پذیرش

1. **تصمیم مالک محصول/Compliance:** تأیید نسخه‌دار معنا و نوع هر Rule، حالت NULL و همه وابستگی‌های مشترک/سپرده/تسهیلات و ارجاعات ابلاغیه‌ای معتبر.
2. **Backend و Oracle UAT:** کنترل همسان هنگام insert/update، ارزیابی نسخه مؤثر، شواهد Oracle واقعی برای FK/NOT NULL/CHECK، همزمانی و ارتقای نسخه بدون ازبین‌بردن حقوق قراردادهای موجود.
3. **فرانت‌اند:** UX شرطی فیلدها و تصمیم‌های صریح، پیام خطای فارسی قابل اقدام؛ عدم امکان عبور از Backend با درخواست مستقیم API.
4. **Runtime:** pre-commit policy evaluation با نسخه Rule ثابت و provenance، deny بر اختلال/عدم تطابق، عدم دورزدن از شعبه/موبایل/اینترنت/سرویس‌های batch و تسویه واقعی.
5. **QA:** مثبت، منفی، مرزی، بازه زمانی، تداخل/همزمانی، idempotency، rollback، service outage، reconciliation مانده و GL، و تأیید UAT در محیط مجاز.
6. **Release governance:** Migration/Cutover و اثر بر قرارداد فعال تصویب شود؛ تنها پس از Build کامل و مدارک C، مجوز Production جداگانه صادر گردد.

## ۷. دستورهای غیرنوشتنی برای اجرای محلی

```bat
cd /d D:\Projects\core-banking-prototype-v0
node tools\verify-pdl-r30-hf7a1-semantic.mjs
node tools\verify-pdl-r30-hf7a2-temporal.mjs
node tools\verify-pdl-r30-hf7a2-runtime-links.mjs
build-production.cmd
```

**نکته:** `build-production.cmd` یک فرآیند Build است و با verifier غیرنوشتنی متفاوت است؛ اجرای آن باید روی محیط توسعه/UAT با کنترل‌های پروژه انجام شود. عدم موفقیت یا نبود وابستگی‌ها به‌معنای مجوز انتشار نیست.
