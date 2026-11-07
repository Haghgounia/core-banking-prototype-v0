# گام ۰۱ — ماتریس پوشش قابلیت‌ها و گیت‌های داده وثایق (CLT)

**شناسه سند:** `CLT-STEP01-COVERAGE-DATA-GATES-R6K-2026-11-07`  
**وضعیت:** مبنای مدیریت توسعه و آزمون، نه مجوز Oracle Write و نه گواهی UAT  
**نسخه:** ۱.۰ — ۷ نوامبر ۲۰۲۶  
**نقشه راه ثابت:** `CLT-ROADMAP-R1.0` (R0 تا R7 بدون تغییر)  
**سورس اصلی:** `core-banking-prototype-v0-2026-11-07-0936.zip` — SHA-256: `b4ed3a4726c5d52c3d070ced56de3c51d7c4a22db9f93111298942bbf745a6f4`  
**اصلاح نصب‌شده R6K:** `CLT_R6K_LOCAL_UAT_START_FIX_2026-11-07.zip` — SHA-256: `4782680a20478624360351c23d63257c6443c7ab8ff0c2fe78bd16c1052224d7`  
**مرجع ردیابی:** `docs/collateral/CLT-R6A-TRACEABILITY-MATRIX-FA.md` و `frontend/src/app/features/collateral/collateral-r6a-catalog.ts`  
**منابع نتیجه Runtime:** لاگ ارسالی کاربر R6K، تصاویر Chrome قبل/بعد R6K و خروجی تشخیصی read-only Oracle.

> قواعد گزارش‌دهی: وجود Controller، Form، Route یا successful Build فقط «شاهد وجود کد» است. PASS نهایی فقط با اجرای UAT مربوطه قابل اعلام است. `FAIL` = رفتار نادرست با پیش‌شرط موجود؛ `BLOCKED_DATA` = داده معتبر هنوز موجود نیست؛ `GATED` = قابلیت عمداً خاموش یا نیازمند مجوز/سیاست؛ `PARTIAL` = فقط بخشی از رفتار پیاده شده؛ `NOT_RUN` = شواهد پذیرش موجود نیست.

## ۱) نتیجه گام T0 — خط مبنا

| کنترل | نتیجه و شاهد |
|---|---|
| نسخه محصول | `0.11.0` |
| تثبیت خط مبنا | سورس ۲۰۲۶-۱۱-۰۷-۰۹۳۶ و Overlay R6K؛ Fingerprint هر دو در سرصفحه |
| تعداد BUC/SUC/View | **۱۶ / ۲۰ / ۱۵**؛ تطبیق متن مرجع و کاتالوگ سورس |
| Routeهای حیاتی | `collateral/reference-data/:table`، `collateral/lookup`، `collateral/registry` موجود |
| قرارداد گروه‌های وثیقه | Controller، Angular service و GET allowlist موجود |
| تست R6K از Node | `HEALTH=200`، `API_GROUPS=200`، `GROUPS=28`، `TYPES=0` طبق لاگ کاربر |
| UAT واقعی در Chrome | **FAIL**؛ جدول گروه‌ها صفر، پیام دسترسی پس از «بازیابی» |
| داده عملیاتی Oracle | `COLLATERAL_GROUP=28`، `COLLATERAL_TYPE=0`، `COLLATERAL=0` طبق خروجی Oracle |
| امنیت | حالت محلی فقط‌خواندنی؛ گیت‌های نوشتن نباید برای آزمون رفع مشکل UI باز شوند |

## ۲) ماتریس ۱۶ BUC — کسب‌وکار

| BUC | هدف | فاز | شاهد کد/قرارداد | وضعیت قابل دفاع | نیاز داده/سیاست |
|---|---|---|---|---|---|
| BUC-01 | تعریف و حاکمیت داده‌های مرجع | R1 | REF/GOV + UI مرجع | نیمه‌پیاده؛ GET موجود، تغییرات و تأیید غیرفعال؛ نمایش گروه‌ها FAIL | گروه‌ها: ثبت شده؛ انواع: داده مصوب لازم |
| BUC-02 | استعلام مالک از Party | R2 | CollateralPartyEligibility → CifService | کنترل Eligibility وجود دارد؛ UAT انتخاب/ارتباط Party انجام نشده | Party مجاز تستی در گیت داده عملیاتی |
| BUC-03 | معرفی وثیقه جدید | R2 | INTRO/REVIEW + UI معرفی | پیش‌نویس/اعتبارسنجی؛ ثبت قطعی در CLT.COLLATERAL غیرفعال | نوع مصوب، Party، ارز و شعبه |
| BUC-04 | ثبت اقلام شمارشی | R2 | CollateralSerialRangePolicy + INTRO | کنترل بازه موجود؛ ثبت نهایی/سابقه کامل نشده | نوع شمارشی و تعریف ویژگی، پس از تصویب |
| BUC-05 | اصلاح پیش از وابستگی عملیاتی | R2 | REVIEW و گزارش G-01 | گردش اصلاحیه اصلی End-to-End پیاده‌سازی کامل ندارد | وثیقه تستی با وابستگی مناسب |
| BUC-06 | ثبت ضامن مستقل | R3 | SPONSOR + پنل ضامن | GET/Validate/Request؛ ثبت قطعی غیرفعال | Party ضامن مجاز و مورد آزمایشی |
| BUC-07 | ثبت/تمدید بیمه‌نامه | R3 | INSURANCE + پنل بیمه | GET/Validate/Request؛ ثبت قطعی غیرفعال | شرکت بیمه، نوع بیمه و بیمه‌نامه تستی |
| BUC-08 | ارزیابی/تجدید ارزیابی | R3 | VALUATION + پنل ارزیابی | GET/Validate/Request؛ نهایی‌سازی غیرفعال | وثیقه تستی، ارزیابی معتبر/منقضی |
| BUC-09 | درخواست تخصیص | R4 | CAPACITY/ALLOC_PRE/ALLOCATION | محاسبه و Preflight؛ Intake پشت گیت و تراکنش مالی غیرفعال | وثیقه/تعهد تستی + سیاست مصوب |
| BUC-10 | رفع تخصیص یک تعهد | R5 | UNALLOC + پنل رفع تخصیص | GET/Preview و مسیر درخواستِ گیت‌شده؛ رفع واقعی غیرفعال | تخصیص معتبر تستی، بعد از تصویب |
| BUC-11 | آزادسازی نهایی | R5 | RELEASE + شواهد وضعیت | GET/Preview؛ عملیات Finalize واقعی وجود ندارد/غیرفعال | وثیقه/تعهد آزادسازیِ مجاز |
| BUC-12 | معرفی مجدد دارایی آزادشده | R5 | G-14 / R5C | گردش ثبت جدید با شناسه دارایی پایدار تکمیل نشده | دارایی آزادشده با تاریخچه تاییدشده |
| BUC-13 | استعلام مستقیم شماره انتظامی | R2 | REG + /collateral/lookup | کد GET و UI موجود؛ بدون داده عملیاتی UAT مثبت مسدود | شماره انتظامی متعلق به وثیقه تستی |
| BUC-14 | پایش کسری پوشش | R4/R6 | CAPACITY + REPORT | خواندن/محاسبه جزئی؛ پایش تغییر سیاست و کسری سراسری کامل نیست | سیاست/تخصیص/ارزیابی معتبر |
| BUC-15 | مدیریت بین‌واحدی وثیقه | R4/R7 | REG branch-scoped + G-09 | کنترل محدوده شعبه GET موجود؛ عملیات بین‌واحدی End-to-End کامل نیست | تعهد و شعب مجاز آزمایشی |
| BUC-16 | مشاهده تاریخچه و ردگیری | R5/R6 | UNALLOC/RELEASE/REPORT | سوابق موضعی GET و رویداد اخیر؛ Audit سراسری کامل نیست | رکوردهای رویداد واقعی/تستی مجاز |

## ۳) ماتریس ۲۰ SUC — سیستم

| SUC | هدف | شاهد کد/قرارداد | پوشش و محدودیت فعلی |
|---|---|---|---|
| SUC-01 | بازیابی شناسنامه با شماره انتظامی | REG /by-registration/{number} | GET موجود؛ UAT مثبت BLOCKED_BY_DATA |
| SUC-02 | استعلام مالک از Party | CollateralPartyEligibility | وابستگی CIF موجود؛ انتخاب/رفتار E2E تأیید نشده |
| SUC-03 | ثبت شناسنامه و معرفی | INTRO /introduction-drafts | DRAFT/Preflight؛ ثبت قطعی غیرفعال |
| SUC-04 | ثبت بازه اقلام شمارشی | CollateralSerialRangePolicy | قواعد ورودی موجود؛ Persistence نهایی ناقص |
| SUC-05 | ثبت درخواست اصلاحیه | R2/G-01 | گردش عملیاتی تکمیل نشده |
| SUC-06 | تأیید/رد اصلاحیه | R2/G-01 | گردش عملیاتی تکمیل نشده |
| SUC-07 | ثبت ضامن | SPONSOR /sponsors/requests | درخواست گیت‌شده؛ ثبت نهایی غیرفعال |
| SUC-08 | کنترل ضامن در تخصیص | SPONSOR + ALLOC_PRE | قرارداد/پیش‌بررسی؛ سناریوی تعارض E2E تأیید نشده |
| SUC-09 | ثبت و تمدید بیمه | INSURANCE /insurance-policies/requests | درخواست گیت‌شده؛ ثبت/تمدید نهایی غیرفعال |
| SUC-10 | ارزیابی و جانشینی ارزیابی قبلی | VALUATION /valuations/requests | درخواست گیت‌شده؛ نهایی‌سازی و انقضا UAT نشده |
| SUC-11 | آمادگی و ظرفیت تخصیص | CAPACITY /capacity | GET/Preview موجود؛ نیازمند داده واقعی |
| SUC-12 | ثبت امن درخواست تخصیص | ALLOCATION /intake | مسیر Intake گیت‌شده؛ تخصیص مالی غیرفعال |
| SUC-13 | رفع تخصیص | UNALLOC /unallocation/requests | درخواست گیت‌شده؛ رفع واقعی غیرفعال |
| SUC-14 | آزادسازی نهایی | RELEASE /final-release | Readiness/Evidence/Preview؛ Finalize غیرفعال |
| SUC-15 | معرفی مجدد دارایی آزادشده | G-14 / R5 | گردش عملیاتی نهایی تکمیل نشده |
| SUC-16 | انتشار فوری تغییر قاعده مرجع | GOV /changes + decisions | قرارداد وجود دارد؛ Write، تصویب و Audit عملیاتی غیرفعال |
| SUC-17 | پایش کسری و موعد ارزیابی | CAPACITY + REPORT | نمای محدود؛ Event-driven/بانک‌سراسری ناقص |
| SUC-18 | تاریخچه وضعیت و ردگیری | RELEASE/UNALLOC/REPORT | GET جزئی؛ Audit جامع تأیید نشده |
| SUC-19 | کنترل مجوز بین‌واحدی | REG branch guard | محدودسازی شعبه در GET؛ مجوز عملیاتی بین‌واحدی ناقص |
| SUC-20 | بهداشت تراکنش و کنترل نسخه | Reference record version + gated services | کنترل‌های جزئی موجود؛ کنترل جامع تراکنش/Idempotency نهایی نیست |

## ۴) ماتریس ۱۵ نمای UI — مطابق کاتالوگ R6A

| View Key | عنوان | فاز | پوشش فعلی | نقص یا شرط پذیرش |
|---|---|---|---|---|
| list | فهرست وثایق | R2 | کامپوننت متصل | Oracle عملیاتی ۰؛ آزمون empty و خطا لازم |
| new | معرفی وثیقه | R2 | جزئی | DRAFT/Preflight، ثبت قطعی بسته |
| direct | دسترسی با شماره انتظامی | R2 | کامپوننت متصل | UAT مثبت وابسته به وثیقه تستی |
| valuation | ارزیابی و تجدید ارزیابی | R3 | جزئی | پنل وابسته؛ نهایی‌سازی بسته |
| insurance | بیمه و ضامنین | R3 | جزئی | پنل‌ها؛ نهایی‌سازی بسته |
| release | آزادسازی / خروج | R5 | جزئی | نمای شواهد و Preflight؛ Finalize بسته |
| history | سوابق و رویدادها | R5/R6 | جزئی | تاریخچه تک‌وثیقه، نه تمام Audit |
| allocation | آزمون سرویس تخصیص | R4 | جزئی | Preflight، بدون تغییر مالی |
| qa | کنترل کیفیت عملیاتی | R6 | کامپوننت متصل | ثبت دستی شواهد، ۱۰ UAT هنوز پذیرفته نشده |
| dashboard | شاخص‌های کلان وثایق | R6 | جزئی | API گزارش محدود؛ تجمیع کل بانک/مالی نهایی نیست |
| recentEvents | رویدادهای اخیر | R6 | جزئی | ۲۰ رویداد اخیر scoped؛ جریان Audit جامع نیست |
| capacityOverview | نمای سریع ظرفیت وثایق | R4/R6 | جزئی | ظرفیت تک‌وثیقه، نه داشبورد تجمیعی |
| hqRefs | تعاریف و اطلاعات پایه | R1 | کامپوننت متصل / T1 PASS, T2 pending | گروه‌ها در UI: 28؛ CLT-DEF-001 بسته شده |
| hqAudit | تاریخچه تغییرات مرجع | R1/R6 | جزئی | گردش درخواست و تصمیم؛ گزارش Audit مستقل ناقص |
| hqDashboard | آمار اطلاعات پایه | R1/R6 | جزئی | خلاصه واقعی محدود؛ نمایش ستادی/گیت UAT باز |

## ۵) گیت‌های داده — «چه زمانی باید در Oracle ثبت شود؟»

| گیت | زمان اقدام | جدول/دامنه | دقیقاً چه چیزی لازم است | آیا اکنون درج شود؟ | مجوز/شرط |
|---|---|---|---|---|---|
| `DATA-00` | انجام‌شده | `CLT.COLLATERAL_GROUP` | ۲۸ کد مصوب از شیت «5وثایق» | **خیر؛ تکمیل شده** | Commit و تطبیق ۲۸/۲۸ قبلاً دریافت شده |
| `DATA-01` | **پس از رفع T1 (خطای نمایش گروه‌ها)**، پیش از آزمون مثبت معرفی | `CLT.COLLATERAL_TYPE` | فهرست انواع مصوب، کد حداکثر ۳ کاراکتر، نام، `COLLATERAL_GROUP_ID` و تمام ضرایب/نشانگرهای سیاستی الزامی | **هنوز خیر؛ منبع سیاست لازم است** | تأیید تحلیلگر/مالک قواعد و کنترل DDL/قیود واقعی DBA؛ سپس SQL کنترل‌شده، قابل اجرای مجدد، در DEV |
| `DATA-02` | هم‌زمان DATA-01، فقط درصورت نوعِ دارای ویژگی اختصاصی | `CLT.COLLATERAL_ATTRIBUTE_DEFINITION`, `CLT.COLLATERAL_TYPE_ATTRIBUTE` | کد ویژگی، نوع داده، الزامی/اختیاری، چندمقداری، مقادیر مجاز، ارتباط با نوع | **مشروط؛ اکنون خیر** | تعریف مصوب و کنترل یکپارچگی FK |
| `DATA-03` | قبل از تکمیل Draft/Insurance/Valuation | مرجع‌های `REF_*` و کاتالوگ‌های مرتبط بیمه/ارز | تنها ردیف‌های مرجع واقعاً غایب که با Read-only Precheck شناسایی شوند | **هنوز خیر** | Seed فنی و Policy کسب‌وکار باید تفکیک شوند؛ هر Seed فقط پس از تطبیق |
| `DATA-04` | بعد از تکمیل DATA-01 و تصمیم‌های G-17/G-03/G-14 | `CLT.COLLATERAL` و داده‌های مالک/شعبه مرتبط | یک/چند شناسنامه **آزمایشی مجاز**، شماره انتظامی معتبر، Party و نوع و شعبه معتبر | **خیر؛ عملیات نهایی همچنان قفل است** | Fixture رسمی DEV/DBA، نه تولید رکورد خودسرانه یا فعال‌کردن تراکنش مالی |
| `DATA-05` | بعد از داده شناسنامه و سیاست‌های حوزه R3 | داده ضامن، بیمه‌نامه و ارزیابی | موارد مثبت/منفی برای UAT-04/05/09 و پنل‌ها | **خیر** | سناریو و داده غیرمحرمانه مصوب؛ بدون تغییر وضعیت مالی |
| `DATA-06` | پس از تصویب گیت R4/R5 | داده تخصیص، رفع تخصیص و سابقه | تعهد، تخصیص، حالت آزادسازی و سابقه کافی برای UAT | **خیر** | صرفاً داده آزمون مورد تأیید DBA/مالک دامین؛ نوشتن مالی خارج از محدوده فعلی |

**هشدار داده:** کدهای `10..70` از اکسل «کد وثیقه» **کد گروه** هستند؛ یک‌به‌یک ساختن ۲۸ نوع وثیقه از آن‌ها، تا زمان تصویب طبقه‌بندی و سیاست، مجاز نیست. تعداد انواع مصوب لزوماً ۲۸ نیست.

### DATA-01 — فیلدهای سیاستی که باید از مرجع معتبر دریافت شود

| دسته | فیلدهای نمونه در مدل فعلی | منبع لازم |
|---|---|---|
| هویت و طبقه‌بندی | `COLLATERAL_TYPE_CODE`, `COLLATERAL_TYPE_NAME`, `COLLATERAL_GROUP_ID` | کاتالوگ انواع مصوب و نگاشت گروه |
| درصدهای پذیرش و استفاده | `MORTGAGE_PERCENT_LOAN`, `USAGE_PERCENT_LOAN`, `MORTGAGE_PERCENT_GUARANTEE`, `USAGE_PERCENT_GUARANTEE`, `MORTGAGE_PERCENT_LETTERCREDIT`, `USAGE_PERCENT_LETTERCREDIT`, `USAGE_PERCENT_GREENLICENSE` | سیاست مصوب اعتباری بانک |
| ضریب ذخیره و دوره بازنگری | `RESERVE_RATIO`, `REVALUATION_INTERVAL_VALUE` | مالک ریسک/اعتبارات |
| الزام‌ها و نحوه استفاده | `INSURABLE`, `SPONSOR_REQUIRED`, `HAS_MULTIPLICITY`, `UPON_REQUEST`, `EXCLUSIVE_USAGE`, `REQUIRED_RELEASE_INQUIRY`, `CENTRALIZED_DEPOSIT_TYPES`, `COLLATERAL_USAGE_TERM`, `USE_IN_OTHER_BRANCHES` | قواعد کسب‌وکار مصوب |
| مشخصه بیمه | `INSURANCE_CALCULATION_BASE` (مشروط) | مالک بیمه/اعتبارات |
| وضعیت و Audit | `STATUS`, `CREATED_BY`, `RECORD_VERSION` و ستون‌های سیستمی | کنترل DBA و Defaultهای اسکیمای موجود |

فیلدها بر اساس کاتالوگ Angular سورس فعلی فهرست شده‌اند؛ **پیش از هر INSERT باید با قیود Data Dictionary واقعی Oracle تطبیق شوند**. هیچ درصد یا پرچمی به‌طور حدسی پر نشود.

## ۶) ترتیب دقیق آزمون از این گام به بعد

| گام | آزمون توسعه/QA یا تحلیلگر | معیار خروج | نیاز به درج داده | نتیجه کنونی |
|---|---|---|---|---|
| **T0** | بررسی Baseline سورس و ردیابی ۱۶+۲۰+۱۵ | SHA و مسیرهای کد مشخص، ماتریس کامل | خیر | **DONE — STATIC** |
| **T1** | مرورگر واقعی Chrome + Network برای فرم گروه‌ها، علت‌گذاری 403، اصلاح یک‌جا و آزمون رگرسیون | `GET groups=200` در مرورگر + نمایش ۲۸ گروه و صفحه‌بندی ۲۰+۸ | خیر | **PASS (R6L-HF1 Chrome + analyst screenshot)** |
| **T2** | جست‌وجوی گروه، نمایش ۰ نوع با پیام درست، Route، ارقام فارسی | همه سناریوها با داده واقعی و خطای شفاف | خیر | `R6M_SOURCE_QA; BROWSER_UAT_PENDING` |
| **T3** | فهرست خالی Oracle، ورودی نامعتبر/صحیح، معرفی با نوعِ خالی | Empty state صحیح؛ ثبت نهایی خاموش | خیر | `R6M_NEGATIVE_UI_PARTIAL; POSITIVE_NOT_RUN` |
| **T4 / DATA-01** | تصویب و بارگذاری انواع وثیقه + آزمون فیلتر وابسته گروه/نوع | تعداد/کد/سیاست/رابطه بر اساس مصوبه | **بله، پس از تأیید** | `BLOCKED_BY_POLICY_DATA` |
| **T5 / DATA-04** | تأمین Fixture مجاز و آزمایش شناسنامه مثبت و UAT-01/02 | استعلام و جزئیات با کنترل شعبه | **بله، پس از تصمیم G-17** | `BLOCKED_BY_OPERATIONAL_DATA` |
| **T6** | R3 تا R5: بیمه، ضامن، ارزیابی، ظرفیت، تخصیص، آزادسازی (محدود به Read/Preflight) | قراردادها و قواعد منفی و UAT با شواهد | بسته به سناریو | `NOT_RUN` |
| **T7** | ۱۰ سناریوی UAT کسب‌وکار + ۱۰ سناریوی UX + پوشش ۱۵ نما | PASS/FAIL/BLOCKED مستند؛ بدون جایگزینی با JUnit | بدون درج جدید مگر گیت‌های قبل | `NOT_RUN` |
| **T8** | گیت R7: Contract/Integration/Concurrency/Audit/Performance/Security غیر IAM | شواهد Release و تصمیم انتشار | فعلاً خیر | `NOT_READY` |

### تصمیم عملیاتی برای ادامه

**مبنای اقدام بعدی:** `CLT-DEF-001` (مشکل خواندن گروه‌ها در Chrome) و `CLT-QA-002` (نبود E2E خودکار مرورگر). **اقدام از تحلیلگر در این لحظه: هیچ تست فنی یا درج داده‌ای لازم نیست.** پس از عبور T1، درخواست تأمین «انواع وثیقه و سیاست‌های مصوب» با قالب دقیق برای DATA-01 اعلام خواهد شد.

## ۷) محدودیت شواهد و جلوگیری از ادعای نادرست

- ماتریس از سورس ارسالی/مستندات و لاگ‌های کاربر استخراج شده است؛ **Test Browser/Oracle واقعی در محیط ویندوز کاربر از این محیط اجرا نشده**.
- شمار Backend methods، حضور Service و تعداد آزمون‌های JUnit را با تعداد قابلیت تکمیل‌شده برابر نگیرید.
- وضعیت ۱۵ نما از کاتالوگ رسمی `collateral-r6a-catalog.ts` استخراج شده؛ ۴ `CONNECTED_COMPONENT` و ۱۱ `PARTIAL` همچنان تا UAT معتبر نشده‌اند.
- گزارش `CLT_R6K_RUNTIME_PASS` فقط درخواست راه‌انداز را تأیید می‌کند و خطای UI را نفی نمی‌کند.
- نوشتن Oracle، عملیات مالی و فعال‌سازی مسیرهای نوشتنی در گام ۱ وجود ندارد.


## CLT-R6M / T2-T3 status update (2026-11-07)
T1 PASS is supported by the real Chrome run and screenshot, not the old Node-only R6K probe.
T2 code delivered for modal detail, explicit search/filter state, code/title/Persian-digit search and empty active types. Browser/analyst UAT remains an independent gate.
T3 negative paths have clearer error vs empty messages; positive registration and introduction remain blocked by DATA-01 / DATA-04 and G-17.
See [CLT-R6M-T2-T3-UI-UAT-FA.md](CLT-R6M-T2-T3-UI-UAT-FA.md).


## CLT-R3C / R3 Insurance Company catalog (2026-11-07)

- R3 / BUC-07 / SUC-09: **Catalog UI/code READY FOR ANALYST UAT; NOT BUSINESS ACCEPTED**.
- Existing generic reference GET reads all insurer statuses from `CLT.INSURANCE_COMPANY`; existing insurance selector filters `STATUS='ACTIVE'`. R3C links both views and makes restrictions explicit in the UI.
- Source-data gate: 42 candidate insurer names reported inserted by user with `COMMIT=MANUAL`; zero persisted candidates are not assumed, and independent Oracle GET is required for live runtime evidence.
- Candidate data has temporary codes and all were inserted `INACTIVE`; no official insurer/line license verification or bank approval is available. No policy can be accepted for allocation based on this catalog.
- UAT scenarios R3C-UAT-01..07 are versioned in [CLT-R3C-INSURANCE-CATALOG-UAT-FA.md](CLT-R3C-INSURANCE-CATALOG-UAT-FA.md).
- Existing 16 BUC / 20 SUC / 15 UI coverage set is unchanged. G-04/G-06/G-09 and insurer selection eligibility remain **OPEN**. No DDL/DML, no IAM changes.

## CLT-RF1 — گیت ۱۷ جدول Reference (2026-11-07)

- در ۱۷ فرم اطلاعات پایه، **قابلیت پیاده‌سازی‌شده** خواندن از Oracle، نمایش رکورد، جست‌وجو و صفحه‌بندی است؛ نه عملیات نهایی CRUD نوشتنی.
- معیار گیت جدید: خواندن و واکشی صفحه اول از **تمام ۱۷ جدول**؛ شواهد Runtime در `logs/clt-rf1-reference-matrix.json`.
- گیت `INSURANCE_COMPANY`: ۴۲ شرکت نامزد (`TMP`، `INACTIVE`) تنها در صورت تأیید COMMIT و ۲۰۰ بودن Endpoint قابل اثبات‌اند؛ تا زمان تصویب رسمی بانک و پروانه هیچ انتخابگر عملیاتی نباید آن‌ها را واجد شرایط تلقی کند.
- `READY_FOR_ANALYST_EXPLORATORY_UAT` به معنی `BUSINESS_ACCEPTED` یا مجوز Oracle Write نیست.

## CLT-R3D — درگاه آزمون تحلیلی بیمه‌نامه بدون داده (2026-11-07)

| مؤلفه | پیش از R3D | پس از R3D | وضعیت گیت |
|---|---|---|---|
| فرم بیمه‌نامه وثیقه | فقط در شناسنامه واقعی، هنگام `insurable=true` | همان کامپوننت در `#/collateral/uat`، تب بیمه‌نامه | Source QA؛ UAT تحلیلگر باز |
| کاتالوگ بیمه‌گر | R3C/RF1 خواندن ۴۲ شرکت نامزد | GET واقعی شمارش نامزدها/فعال‌ها بدون مجوزسازی | RF1 Runtime PASS، پیش‌نمایش نیازمند Browser UAT |
| نوع پوشش بیمه | `COLLATERAL_INSURANCE_TYPE`، بدون داده مثبت احرازشده | گزارش تعداد و وضعیت از Oracle | سناریوی مثبت مسدود |
| فیلد و کنترل شکلی | وابسته به دریافت وثیقه | قابل بررسی در مرورگر بدون API نوشتنی | QA محلی، تأیید تحلیلگر باز |
| بیمه‌نامه صادره بانک | غیرفعال | همچنان غیرفعال | G-09 باز |
| کفایت پوشش/چرخه تأیید | غیرفعال | هیچ اعتبار عملیاتی ادعا نشده | G-04/G-06 باز |
| ثبت شواهد UAT | ده سناریوی رسمی R6 | هشت سناریوی مکمل بیمه + JSON جداگانه؛ ده سناریوی اصلی ثابت | NOT_RUN تا تصمیم تحلیلگر |

**توالی تصمیم:** (۱) تحلیلگر پیش‌نمایش و فیلدها را بررسی کند، (۲) شکاف‌های UI/Business جمع‌آوری شوند، (۳) انواع و سیاست‌های مصوب بیمه و وثیقه از مالک داده اخذ شوند، (۴) سناریوی مثبت واقعی پس از گیت DATA-01/DATA-04 و کنترل‌های مالی اجرا شود. هیچ Seed یا فعال‌سازی خودکار از این نسخه ناشی نمی‌شود.
