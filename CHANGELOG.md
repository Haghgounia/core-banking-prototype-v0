## 0.3.75 — FIX83: فعال‌سازی قطعی Renderer روزهای تقویم کاری

- علت عدم مشاهده تغییرات 0.3.74 اصلاح شد: تشخیص صفحه `BUSINESS_CALENDAR_DAY` دیگر فقط بر `descriptor.resource` متکی نیست و `descriptor.tableName` نیز معیار قطعی است.
- بنابراین صفحه `CAL2.BUSINESS_CALENDAR_DAY` به Grid اختصاصی منتقل می‌شود و Grid عمومی شناسه‌محور نمایش داده نمی‌شود.
- Grid اختصاصی تاریخ شمسی، روز هفته، نام فارسی تقویم، عنوان فارسی وضعیت/دلیل و نام فارسی منبع را نمایش می‌دهد.
- Codeهای Oracle (`PUBLIC_HOLIDAY`, `UNCLASSIFIED`, ...) بدون تغییر در دیتابیس باقی می‌مانند و فقط در UI ترجمه می‌شوند.

## 0.3.73 — GEO: نمایش Candidate رومن‌نویسی و وضعیت Governance

- ابزار تبدیل نام فارسی به انگلیسی اکنون مستقیماً `GEO.PKG_NAME_ROMANIZATION.RESOLVE_NAME` را فراخوانی می‌کند.
- معادل‌های Review-only مانند `آقا مصطفی → Agha Mostafa` نیز نمایش داده می‌شوند و دیگر با «پیدا نشد» اشتباه نمی‌شوند.
- خروجی فرم شامل روش Resolve، Confidence، وضعیت Governance، Auto-fill و نیاز به بازبینی است.
- `GEO.FN_ROMANIZE_NAME` برای Auto-fill امن بدون تغییر باقی مانده است.

## 0.3.72 — GEO: ابزار تبدیل نام فارسی به انگلیسی

- فرم مستقل «تبدیل نام فارسی به انگلیسی» در اطلاعات پایه عمومی اضافه شد.
- API جدید `POST /api/v1/name-romanization/resolve` مستقیماً `GEO.FN_NORMALIZE_NAME` و `GEO.FN_ROMANIZE_NAME` را در Oracle فراخوانی می‌کند.
- خروجی فرم بین نتیجه مجاز برای Auto-fill، نتیجه نیازمند Governance Review و خطای سرویس تفکیک می‌شود.
- فرم برای نام‌های ساده و مرکب مانند `محمد` و `سید محمد` قابل استفاده است.
- Guard استاتیک GEO برای Route، API و فراخوانی Function گسترش یافت.

# Changelog

## 0.3.71 — GEO: واژه‌نامه رومن‌نویسی نام و پیشوند/پسوند

- دو جدول `GEO.NAME_ROMANIZATION_DICTIONARY` و `GEO.NAME_AFFIX_DICTIONARY` به موتور Generic Reference CRUD متصل شدند و فرم‌های آن‌ها در «اطلاعات پایه عمومی → اطلاعات عمومی» نمایش داده می‌شود.
- فرم واژه‌نامه اصلی از جستجو، صفحه‌بندی، مرتب‌سازی، ایجاد/ویرایش/حذف، وضعیت فعال، Governance Status، Romanization Method، Confidence Score و کنترل `AUTO_FILL_ALLOWED` پشتیبانی می‌کند.
- فرم Affix برای مدیریت `سید/سیده/سادات/آقا/خانم/خانوم/میرزا/...` با Position، Context Sensitivity، Priority و `AUTO_APPLY_ALLOWED` اضافه شد.
- نوع جدید `STRING_SELECT` به موتور عمومی اطلاعات پایه اضافه شد تا Codeهای رشته‌ای در فرم به‌صورت Combo کنترل‌شده نمایش داده شوند.
- Normalization نام فارسی در UI و Backend اعمال می‌شود (`ي/ى→ی`، `ك→ک`، نیم‌فاصله و فاصله‌های زائد).
- قواعد حاکمیتی Backend مانع فعال‌شدن Auto-fill بدون Canonical English و Governance معتبر، و مانع Auto-apply برای Affixهای Context-sensitive می‌شوند.
- DDL کامل Name Romanization به Installer استاندارد GEO اضافه شد و Verifier استاتیک `verify-geo-name-romanization.mjs` قرارداد UI/Backend/DDL را کنترل می‌کند.

## 0.3.70 — FIX81: Import تمیز و کامل تعرفه‌های بانک مرکزی ۱۴۰۴

- فایل `cbi-fee-1404(1).xlsx` با SHA256 ثابت به‌صورت کامل تحلیل و 239 ردیف فیزیکی آن به 229 تعرفه منطقی قابل نگهداری تبدیل شد؛ 10 ردیف ادامه پله‌های ارزیابی در 5 تعرفه Tiered تجمیع شده‌اند.
- Import جدید در `database/oracle/fee/cbi-1404` به‌صورت Upsert و بدون `DELETE/TRUNCATE/DROP` است؛ 229 `FEE_DEFINITION` و 229 `FEE_DEFINITION_VERSION` رسمی با `CLASSIFICATION_CODE=CBI_1404` ایجاد/به‌روزرسانی می‌شوند. Wrapper رسمی از الگوی تراکنشی Import → Verify → COMMIT استفاده می‌کند و در صورت خطا Rollback می‌کند.
- 17 سرفصل واقعی فایل بانک مرکزی به 17 `FEE_FEATURE` گروهی تبدیل شده‌اند و کد ردیف بانک مرکزی بدون ساختن شماره جعلی در `REGULATORY_TARIFF_CODE` حفظ می‌شود.
- درصدهای منبع از واحد درصد به ضریب موتور تبدیل می‌شوند؛ مثال `0.5% -> RATE_VALUE=0.005`.
- 5 تعرفه ارزیابی پلکانی با مجموع 15 Tier و وابستگی بیرونی «تعرفه کارشناس رسمی» مدل شده‌اند؛ مقدار بیرونی جعل نمی‌شود.
- 20 ردیف با فرمول غیرساختاری به‌صورت `EXTERNAL_VALUE` وارد می‌شوند تا متن مقرراتی حفظ شود ولی موتور فرمول حدسی اجرا نکند.
- سه تناقض عددی منبع (Excel rows 45، 60 و 154) به‌صورت `REVIEW_CONFLICT` قرنطینه شده‌اند و Rule اجرایی عددی برای آنها ساخته نمی‌شود.
- شش تعریف CBI موجود Baseline دوباره‌سازی نمی‌شوند و همان `FEE_CODE` قبلی به داده رسمی متصل می‌شود.
- خطای داده‌ای مهم `CREDIT_STAGE2` اصلاح شد: نرخ مرحله دوم از `0.0015` به `0.001` تغییر می‌کند تا همراه `0.0005` مرحله اول، مجموع صحیح 1.5 در هزار شود.
- فایل‌های Audit شامل `cbi_fee_1404_clean.csv`، `cbi_fee_1404_review.csv` و Manifest با SHA256 منبع اضافه شدند.
- Verifier جدید `verify-cbi-fee-1404-import.mjs` شمارش‌ها، عدم وجود SQL مخرب، نرخ‌های نرمال‌شده، Tierها و اصلاح Stage2 را کنترل می‌کند.

## 0.3.69 — FIX80: فرم صریح نوع محاسبه کارمزد (مبلغ ثابت/درصد/پلکانی)

- فرم `FEE_CALCULATION_RULE` در UI با عنوان روشن «نوع و قاعده محاسبه کارمزد» نمایش داده می‌شود؛ انتخاب نوع محاسبه دیگر در میان فیلدهای عمومی پنهان نیست.
- در ابتدای صفحه این فرم، 11 کارت انتخاب سریع برای `FIXED`، `PERCENTAGE`، درصد با کف/سقف، `FIXED_PLUS_PERCENTAGE`، `PER_UNIT`، `TIERED`، `MARGINAL_TIERED`، `ANNUALIZED_PERCENTAGE` و `COMPOSITE` اضافه شد.
- کلیک روی هر کارت، فرم ثبت Rule را باز و `CALCULATION_STRATEGY_CODE` را از پیش مقداردهی می‌کند.
- فیلدهای مبلغ ثابت، نرخ، حداقل/حداکثر و پارامترهای سالانه بر اساس Strategy انتخاب‌شده به‌صورت شرطی نمایش داده می‌شوند تا فرم گنگ نباشد.
- توضیح نرخ درصدی به UI اضافه شد: مقدار `RATE_VALUE` به‌صورت اعشاری ذخیره می‌شود؛ مثلاً `0.002 = 0.2%`.
- `BASIS_TYPE_CODE` نیز به دامنه `CALCULATION_BASIS` وصل شد تا مبنای محاسبه از مقادیر مرجع فارسی انتخاب شود.
- لینک سریع «نوع محاسبه کارمزد» به صفحه اصلی `/fee` اضافه شد و Node فلوچارت نیز با همین عنوان روشن‌تر شد.
- هیچ تغییر DDL یا Seed در این Release وجود ندارد.

## 0.3.68 — FIX79: تبدیل نقشه کارمزد به فلوچارت بصری واقعی و قابل کلیک

- نمایش مرحله‌ای قبلی صفحه `/fee` به یک Flowchart واقعی‌تر با Canvas، Shapeهای فرم، خط اتصال افقی داخل هر مرحله و فلش عمودی بین مراحل بازطراحی شد.
- هر Shape همچنان به فرم واقعی متناظر یا شبیه‌ساز لینک مستقیم دارد و عنوان جدول در خود Node نمایش داده می‌شود.
- گام‌های Configuration اصلی، داده‌های پشتیبان/تست و Runtime فقط‌خواندنی با رنگ و Style مستقل از هم تفکیک شده‌اند.
- اگر جدول Oracle موجود نباشد، Shape مربوط به صورت Disabled و بدون Navigation نمایش داده می‌شود.
- طراحی Responsive است؛ در نمایشگرهای کوچک Backbone افقی حذف و Nodeها به Grid/ستون تبدیل می‌شوند، ولی ترتیب مراحل و فلش‌های اصلی حفظ می‌شود.
- هیچ تغییر DDL، Seed یا Backend در این Release وجود ندارد.

## 0.3.67 — FIX78: دیاگرام تعاملی روال تعریف فرم‌های کارمزد در صفحه اصلی

- در صفحه اصلی ماژول کارمزد (`/fee`) یک دیاگرام تعاملی جدید اضافه شد که ترتیب پیشنهادی تعریف اطلاعات کارمزد را در 9 گام نمایش می‌دهد.
- هر Shape در دیاگرام به‌صورت مستقیم به فرم مرتبط (`/fee/tables/:table`) یا شبیه‌ساز (`/fee/simulator`) لینک می‌شود تا ناوبری کاربر ساده‌تر شود.
- گام‌های پشتیبان Prototype مانند Party/Product/Account/FX Demo از گام‌های اصلی Configuration به‌صورت بصری تفکیک شدند.
- گره‌های Runtime/Audit با برچسب «فقط مشاهده» نمایش داده می‌شوند تا تمایز Configuration و Execution روشن باشد.
- در صورت نبودن جدول در Oracle، Node مربوط در دیاگرام به‌صورت Disabled نمایش داده می‌شود و امکان کلیک ندارد.
- Quick Access Cardهای موجود و Grid کامل 47 فرم بدون حذف قابلیت‌های قبلی حفظ شدند.
- Verifier استاتیک FEE برای کنترل وجود دیاگرام تعاملی در صفحه Home به‌روزرسانی شد.

## 0.3.66 — FIX77: فرم‌های جامع کارمزد بر مبنای FEE Baseline 1.0 (47 جدول / 574 Seed)

- فایل‌های Oracle و Seed Data پیوست به‌عنوان مبنای فیزیکی ماژول FEE پذیرفته شدند: 47 جدول و 574 رکورد Seed.
- صفحه جدید «مدیریت جامع کارمزد» در مسیر `/fee` اضافه شد و 47 فرم را در شش گروه کسب‌وکاری نمایش می‌دهد: اطلاعات پایه، داده آزمایشی، سیاست/مقررات، تعریف و پیکربندی، Arrangement و Runtime/Audit.
- Backend جدید `/api/v1/fees/admin` بر اساس Metadata واقعی Oracle، PK/FK/Check Constraint/Comment/Datatype را خوانده و فرم‌های CRUD را بدون تکرار مدل فیلدها در کد ارائه می‌کند.
- `FEE_REF_DOMAIN/FEE_REF_VALUE` برای فیلدهای کدی به‌عنوان Lookup فارسی استفاده می‌شوند و FKها نیز به Lookup رکورد مرجع تبدیل می‌شوند.
- فیلدهای DATE/TIMESTAMP از Date Picker شمسی مشترک، CLOB از Textarea و مقادیر محدودشده با Check Constraint از Select استفاده می‌کنند.
- 35 جدول Reference/Configuration/Arrangement قابل ثبت/ویرایش/حذف‌اند؛ 12 جدول Runtime/Audit در فرم عمومی فقط خواندنی هستند تا شواهد تراکنش و ممیزی دستی تغییر نکند.
- Simulator قدیمی حفظ شد و به مسیر `/fee/simulator` منتقل شد.
- 47 فایل DDL پیوست بدون تغییر محتوایی در `database/oracle/fee/baseline-1.0/ddl` نگهداری و Installer وابستگی‌محور `install-baseline-1.0-ddl.sql` اضافه شد.
- بسته Seed پیوست در `baseline-1.0/seed` نگهداری و Installer `install-baseline-1.0-seed.sql` اضافه شد؛ Seed IDها منفی‌اند و رکوردهای جدید UI از Sequenceهای مثبت Oracle استفاده می‌کنند.
- مدل قدیمی 21 جدولی FEE به `legacy-phase1-21-tables` منتقل شد و `install-ddl.sql` اکنون به Baseline 1.0 جدید هدایت می‌شود.

## 0.3.65 — FIX76: پاک‌سازی خودکار INSTALLهای قدیمی در Upgrade

- در سناریوی Extract/Copy نسخه جدید روی پوشه نصب قدیمی، فایل‌های `INSTALL-*.txt` باقی‌مانده در Root دیگر باعث توقف `build-production.cmd` نمی‌شوند.
- ابزار جدید `tools/migrate-release-layout.mjs` قبل از `verify-release-layout.mjs` اجرا می‌شود؛ فایل Root را اگر مقصد متناظر در `docs/install/` وجود داشته باشد حذف می‌کند و در غیر این صورت به `docs/install/` منتقل می‌کند.
- همین Migration در `build-production.sh` نیز اعمال شد تا قرارداد Release در Windows/Linux یکسان باشد.
- Guard `verify-release-layout.mjs` اکنون علاوه بر محل فایل‌ها، وجود Migration و ترتیب اجرای آن پیش از Verification را کنترل می‌کند.
- `verify-cif-isic2.mjs` از وابستگی سخت به نسخه `0.3.64` خارج شد و فقط قرارداد نسخه‌گذاری `x.y.z` را کنترل می‌کند؛ بنابراین Releaseهای بعدی بدون تغییر مصنوعی Verifier ساخته می‌شوند.
- هیچ تغییر DDL، Seed یا UI در ISIC نسبت به FIX75 ندارد؛ دیتابیس ISIC نسخه 0.3.64/0.3.65 از نظر Schema و Import یکسان است.

## 0.3.64 — FIX75: همگام‌سازی واقعی Schema/Import ISIC و نرمال‌سازی Explanatory Notes

- نسخه‌گذاری Release از این نسخه فقط به شکل `x.y.z` است؛ پسوند `prototype-fee-p1` از نسخه جاری و فایل ZIP حذف شد.
- فایل‌های `INSTALL-*.txt` از Root پروژه به `docs/install/` منتقل شدند و Guard جدید `verify-release-layout.mjs` این قرارداد را کنترل می‌کند.
- مشخص شد DDL مشاهده‌شده در Oracle مربوط به مدل قدیمی ISIC2 بوده است؛ Installer جدید آبجکت‌های جدید ISIC را Drop/Recreate و Seed را دوباره Import می‌کند، بدون هیچ تغییر در `CIF.REF_ISIC_ACTIVITY` قدیمی.
- `CIF.REF_ISIC_ACTIVITY2` فقط ساختار طبقه‌بندی را نگه می‌دارد: `PARENT_ACTIVITY_ID`، `LEVEL_CODE/LEVEL_NO`، کد و نام‌های دو‌زبانه و وضعیت‌های عملیاتی. ستون‌های تکراری `BASE_ISIC_CODE/PARENT_ISIC_CODE/SECTION_CODE` و شش CLOB توضیحی از این جدول حذف شدند.
- جدول جدید `CIF.REF_ISIC_ACTIVITY_NOTE` برای متن‌های طولانی توضیحی اضافه شد؛ نوع‌های `EXPLANATORY`، `INCLUDES`، `ALSO_INCLUDES` و `EXCLUDES` و زبان `fa/en` را مستقل نگه می‌دارد.
- Backend فرم فعالیت ISIC متن‌های توضیحی را از جدول Note خوانده/در آن ذخیره می‌کند و فیلد «همچنین شامل» فارسی/انگلیسی به UI اضافه شد.
- Import UNSD Rev.4 همچنان 766 رکورد کامل ساختاری (21/88/238/419) با `NAME_FA` و `NAME_EN` کامل و 419 Class قابل انتخاب وارد می‌کند. هیچ Explanatory Note ساختگی Seed نمی‌شود، چون فایل Structure-only مبنا آن محتوا را ندارد.
- `04-verify-isic2.sql` علاوه بر شمارش Dataset، نبود ستون‌های Legacy در `REF_ISIC_ACTIVITY2` و وجود `REF_ISIC_ACTIVITY_NOTE` را نیز کنترل می‌کند.
- Build برنامه عمداً DDL را روی Oracle اجرا نمی‌کند؛ اجرای `database/oracle/cif/isic2/CIF_ISIC2_FULL_INSTALL.sql` برای اعمال تغییرات بانک اطلاعاتی الزامی است.

## 0.3.63-prototype-fee-p1 — FIX74: اصلاح مسیر Verifierهای Node در Windows و Build Guard

- خطای Windows در `tools/verify-cif-isic2.mjs` که مسیر `file:///D:/...` را با `URL.pathname` به `/D:/...` تبدیل و در نهایت مسیر اشتباه `D:\D:\...` تولید می‌کرد، اصلاح شد.
- ریشه فایل‌سیستم Verifierهای ISIC2 و PDL اکنون با `fileURLToPath(import.meta.url)` و `path.resolve(...)` محاسبه می‌شود و به Current Working Directory وابسته نیست.
- `build-production.cmd` و `build-production.sh` در ابتدای اجرا به Root خود پروژه منتقل می‌شوند تا اجرای Build از هر Working Directory قابل اتکا باشد.
- Guard جدید `verify-node-tool-path-portability.mjs` اضافه شد تا استفاده مجدد از `URL.pathname` یا `process.cwd()` برای ریشه Verifierها را Fail کند.
- `bin/stop.cmd` از Kill کردن تکراری یک PID جلوگیری می‌کند؛ در صورت تکرار همان PID در خروجی `netstat` دیگر پیام خطای «There is no running instance» تولید نمی‌شود.
- هیچ تغییر DDL، Seed، API یا UI در ISIC2 انجام نشده و طراحی/داده‌های FIX73 بدون تغییر باقی مانده‌اند.

## 0.3.62-prototype-fee-p1 — FIX73: بازطراحی مستقل ISIC با سلسله‌مراتب Parent-ID و Seed کامل فارسی

- مدل ISIC2 به‌صورت مستقل بازطراحی شد و محور آن فقط `CIF.REF_ISIC_RELEASE` و `CIF.REF_ISIC_ACTIVITY2` است.
- `REF_ISIC_ACTIVITY2` از فیلدهای تکراری ساختار (`BASE_ISIC_CODE`، `SECTION_CODE` و Parent Code فیزیکی) پاک شد؛ رابطه درختی با `PARENT_ACTIVITY_ID` و Self FK برقرار می‌شود.
- ستون `LEVEL_NO` اضافه شد و سازگاری `SECTION=1`، `DIVISION=2`، `GROUP=3`، `CLASS=4` و `NATIONAL_SUBCLASS=5` در Oracle و Backend کنترل می‌شود.
- FK مرکب `(ISIC_RELEASE_ID, PARENT_ACTIVITY_ID)` تضمین می‌کند والد و فرزند متعلق به یک Release باشند.
- `NAME_FA` و `NAME_EN` در Release و Activity اجباری شدند. Seed UNSD Rev.4 اکنون برای هر 766 رکورد عنوان فارسی و انگلیسی دارد.
- تمام 766 عنوان فارسی Seed با `TRANSLATION_STATUS_CODE=BANK_TRANSLATED` ثبت می‌شوند؛ این عناوین ترجمه پروژه هستند و به‌عنوان ترجمه رسمی مرجع معرفی نمی‌شوند.
- تعداد Seed بدون تغییر است: 21 Section، 88 Division، 238 Group و 419 Class؛ فقط 419 Class قابل انتخاب است.
- فرم «نسخه‌های ISIC» نام فارسی را اجباری می‌کند و فرم «فعالیت‌های ISIC» به Tree/Search/Detail بر مبنای Parent ID، Level No و عنوان فارسی اجباری بازطراحی شد.
- اسکریپت Prototype پاک‌سازی/ایجاد، Import کامل دو‌زبانه، ثبت Release غیرفعال `IR-SCI` و Validation دیتاست به بسته `database/oracle/cif/isic2` افزوده/بازنویسی شد.
- `CIF_ISIC2_FULL_INSTALL.sql` به‌صورت Standalone ساخته شد و برای اجرای آن نیاز به include فایل‌های دیگر نیست.

## 0.3.61-prototype-fee-p1 — FIX72: ISIC نسخه‌محور با REF_ISIC_ACTIVITY2 و فرم‌های مستقل

- جدول قدیمی `CIF.REF_ISIC_ACTIVITY` بدون تغییر باقی می‌ماند و مسیرهای عملیاتی فعلی Party همچنان از همان جدول استفاده می‌کنند.
- بسته Oracle مستقل `database/oracle/cif/isic2` اضافه شد که `CIF.REF_ISIC_RELEASE`، `CIF.REF_ISIC_ACTIVITY2` و `CIF.V_REF_ISIC_ACTIVITY_LOOKUP2` را ایجاد می‌کند.
- Seed رسمی UNSD ISIC Rev.4 شامل 766 رکورد (21 Section، 88 Division، 238 Group، 419 Class) به `REF_ISIC_ACTIVITY2` Import می‌شود؛ چهار عنوان فارسی بازبینی‌شده با وضعیت `BANK_VERIFIED` حفظ شده‌اند.
- Release مربوط به `IR-SCI Rev.4` فقط به صورت `DRAFT` و غیرفعال ثبت می‌شود و هیچ داده ساختگی ملی برای ایران تولید نمی‌شود.
- دو فرم مستقل در «اطلاعات پایه مشتری / Party» اضافه شد: «نسخه‌های ISIC» و «فعالیت‌های ISIC نسخه‌محور».
- فرم فعالیت‌ها دارای انتخاب Release، جستجوی کد/فارسی/انگلیسی، فیلتر سطح/فعال/قابل انتخاب، نمای سلسله‌مراتبی Lazy، ویرایش CLOBهای شرح/مشمول/مستثنی، وضعیت ترجمه و تاریخ اعتبار شمسی است.
- Backend اختصاصی `/api/v1/cif/isic` برای CRUD نسخه‌ها و فعالیت‌ها و Lookupهای مربوط ایجاد شد؛ Optimistic Lock با `RECORD_VERSION` حفظ می‌شود.
- هیچ Migrationای از `ORGANIZATION.ISIC_CODE` یا `PARTY_EMPLOYMENT.ISIC_CODE` به مدل جدید در این نسخه انجام نمی‌شود.

## 0.3.60-prototype-fee-p1 — FIX71: پیش‌فرض سال/ماه جاری در Date Picker شمسی

- انتخاب‌گر مشترک تاریخ شمسی هنگام خالی بودن مقدار، روی تاریخ جاری شمسی Seed می‌شود بدون اینکه مقدار FormControl به‌صورت خودکار تغییر کند.
- سال و ماه جاری (و روز جاری) هنگام باز شدن Picker به‌صورت صریح در Optionها Selected می‌شوند تا Browser به اولین گزینه فهرست (`1300 / فروردین`) برنگردد.
- همین رفتار برای همه فرم‌هایی که از `app-persian-date-input` استفاده می‌کنند اعمال می‌شود، از جمله `CAL2.BUSINESS_CALENDAR`.
- اگر فیلد قبلاً مقدار داشته باشد، Date Picker همچنان روی همان تاریخ ذخیره‌شده باز می‌شود.
- محدودیت‌های `minDate`/`maxDate` و دکمه «امروز» بدون تغییر حفظ شده‌اند.
- هیچ DDL یا Migration دیتابیسی ندارد.

## 0.3.58-prototype-fee-p1 — FIX69: Lookup کشور و منطقه زمانی در تقویم کاری

- در فرم `CAL2.BUSINESS_CALENDAR` فیلد کشور از حالت Text آزاد به Lookup جستجوپذیر تبدیل شد و داده مستقیماً از `GEO.COUNTRIES` خوانده می‌شود.
- با توجه به ساختار واقعی `CAL2.BUSINESS_CALENDAR.COUNTRY_CODE VARCHAR2(3)`، مقدار ذخیره‌شده `COUNTRY_ISO_CODE` سه‌حرفی است؛ نام فارسی کشور برای کاربر نمایش داده می‌شود و `COUNTRY_ISO_CODE2` نیز در کد گزینه دیده می‌شود.
- فقط کشورهای فعال (`IS_ACTIVE=1`) در Lookup قابل انتخاب‌اند و کشور پیش‌فرض مرجع در ابتدای فهرست قرار می‌گیرد.
- فیلد منطقه زمانی از حالت Text آزاد به Lookup استاندارد IANA بر پایه `ZoneId.getAvailableZoneIds()` تبدیل شد؛ `Asia/Tehran` در ابتدای فهرست قرار می‌گیرد و جستجو پشتیبانی می‌شود.
- Backend علاوه بر UI، معتبر بودن `COUNTRY_CODE` در `GEO.COUNTRIES` و معتبر بودن شناسه IANA منطقه زمانی را پیش از Insert/Update کنترل می‌کند.
- طبق تصمیم پروژه، `ORGANIZATION_ID` در این نسخه همچنان Text آزاد باقی مانده و هیچ وابستگی به CIF/Party ایجاد نشده است.
- هیچ DDL یا Migration دیتابیسی ندارد.

## 0.3.57-prototype-fee-p1 — FIX68: اصلاح Test Compile در مقایسه EA/Oracle

- خطای `testCompile` در `EaOracleXmiWriterTest` ناشی از تداخل نام متغیر محلی `fk` با پارامتر Lambda هم‌نام اصلاح شد.
- نام پارامتر Lambda به `parsedFk` تغییر کرد؛ رفتار Runtime و منطق مقایسه PK/FK/Check/TIMESTAMP نسخه 0.3.56 بدون تغییر حفظ شده است.
- هیچ DDL یا Migration دیتابیسی ندارد.

## 0.3.56-prototype-fee-p1 — FIX67: تکمیل مقایسه Constraintهای EA/Oracle و سازگاری TIMESTAMP

- در فرم «مقایسه مدل Enterprise Architect با Oracle»، اختلاف `TIMESTAMP(0)` در EA در برابر `TIMESTAMP(6)` در Oracle به‌تنهایی اختلاف محسوب نمی‌شود؛ سایر Precisionهای متفاوت همچنان گزارش می‌شوند.
- مقایسه Primary Key اصلاح شد تا نبود PK در EA در حالی که Oracle دارای PK است واقعاً جدول را «دارای اختلاف» کند.
- مقایسه Foreign Key اضافه شد: نام Constraint، ستون‌های فرزند، جدول مرجع و ستون‌های مرجع از EA XMI و Oracle Data Dictionary مقایسه می‌شوند.
- مقایسه Check Constraint اضافه شد: Checkهای کاربری بر اساس نام و عبارت نرمال‌شده مقایسه می‌شوند؛ Checkهای سیستمی NOT NULL از این بخش حذف می‌شوند چون Nullable جداگانه کنترل می‌شود.
- Parser EA اکنون FK را از `UML:Operation`/`UML:Association`/`UML:Constraint` و Check را از `UML:Operation`/`UML:Constraint` استخراج می‌کند.
- Oracle Inspector برای مقایسه از `ALL_CONSTRAINTS`، `ALL_CONS_COLUMNS` و `SEARCH_CONDITION_VC` استفاده می‌کند.
- Grid اصلی ستون‌های Foreign Key و Check Constraint را با تعداد EA/Oracle نمایش می‌دهد و در جزئیات هر جدول، اختلاف Constraintها ردیف‌به‌ردیف قابل مشاهده است؛ خروجی CSV نیز شمارنده و وضعیت آنها را دارد.
- اصلاح متادیتای فارسی جدول: EA `Alias` با Oracle `TABLE COMMENT` مقایسه می‌شود و EA `Documentation` صرفاً اطلاعات توصیفی است و دیگر باعث False Positive نمی‌شود.
- طبق درخواست ثبت‌شده برای نسخه بعدی، Schemaهای `ORACLE_MAINTAINED` سیستمی از فهرست Schemaهای قابل انتخاب حذف شدند؛ فهرست همچنان Metadata-driven است.
- هیچ DDL یا Migration دیتابیسی ندارد.

## 0.3.55-prototype-fee-p1 — FIX66: Oracle Metadata-driven Schema Discovery

- فهرست Schemaهای فرم مقایسه EA/Oracle و Oracle→EA XMI از Oracle Data Dictionary و `ALL_TABLES` دریافت می‌شود و دیگر به فهرست Hard-code محدود نیست.
- Configuration فقط برای Friendly Label و Default ترجیحی استفاده می‌شود؛ تعداد Tableهای قابل مشاهده در Label هر Schema نمایش داده می‌شود.
- Validation انتخاب Schema نیز بر همان فهرست Metadata-driven اعمال می‌شود.
- هیچ DDL یا Migration دیتابیسی ندارد.

## 0.3.54-prototype-fee-p1 — FIX65: نمای ماهانه تقویم CAL2

- افزودن فرم عملیاتی Angular «تقویم ماهانه» با مسیر `/calendar2/month-view` و ورودی مستقیم از صفحه اصلی تقویم دو.
- پیش‌فرض نمایش روی تقویم هجری شمسی و ماه جاری است؛ امکان جابه‌جایی ماه قبل/بعد، بازگشت به امروز، انتخاب سال/ماه و تغییر تقویم مبنا بین شمسی، میلادی و قمری فراهم شد.
- Grid هفت‌ستونه با شروع هفته از شنبه، نمایش جمعه، امروز، مناسبت‌ها، تعداد رخدادهای هر روز و علامت تعطیلی پیاده‌سازی شد.
- پنل جزئیات روز تاریخ متناظر شمسی/میلادی/قمری، نوع مناسبت، رسمی/تعطیل بودن، منشأ رخداد و توضیحات را نمایش می‌دهد.
- Read Model مستقل Backend روی `CAL2.CALENDAR_DATE + CANONICAL_DAY + EVENT_OCCURRENCE + EVENT + EVENT_TYPE` ایجاد شد؛ رویدادها از occurrence واقعی خوانده می‌شوند، نه مستقیماً از Rule.
- API جدید `GET /api/v1/calendar2/month-view` اضافه شد و با Default Calendar Variantهای سه تقویم، تاریخ‌های متناظر را در یک Query Model ارائه می‌کند.
- هیچ DDL یا Migration جدیدی ندارد؛ ساختار ۱۶ جدول CAL2 بدون تغییر است.
- Guard جدید `verify-calendar2-month-view.mjs` مسیر API، Route Angular، نمایش سه تقویم، تعطیلی/مناسبت و ساختار Grid هفت‌روزه را کنترل می‌کند.

## 0.3.53-prototype-fee-p1 — FIX64: نام ثابت Runtime JAR

- نام Runtime Artifact در Windows، Linux/macOS، Build، Start، Database Export و مستندات عملیاتی یکسان شد: `app\core-banking-prototype.jar`.
- نام JAR دیگر شامل شماره نسخه Release نیست.
- برای حفظ حفاظت در برابر اجرای Artifact قدیمی، Build فایل `app\BUILD-VERSION` را تولید می‌کند و Start آن را با `VERSION` تطبیق می‌دهد.
- `build-production.cmd` و `build-production.sh` قبل از Build همه JARهای قدیمی پوشه `app` را حذف و فقط `core-banking-prototype.jar` را تولید می‌کنند.
- Guard `verify-runtime-artifact-contract.mjs` برای قرارداد Canonical JAR + BUILD-VERSION بازنویسی شد.
- نسخه Release، Frontend و Backend روی 0.3.53 همگام شد؛ هیچ DDL یا Migration جدیدی ندارد.

## 0.3.52-prototype-fee-p1 — FIX63: تقویت Guard اجرای Runtime

- Guard اجرای Artifact قدیمی اضافه شد.
- این قرارداد در FIX64 ساده‌سازی شد تا نام Runtime Artifact در تمام ابزارها ثابت بماند.
- هیچ DDL یا Migration جدیدی ندارد.

## 0.3.51-prototype-fee-p1 — FIX62: اصلاح خطای Generic در CAL2 و Backend Compile Preflight

- رفع خطای Java compilation در `Calendar2ReferenceRepository` برای دو RowMapper جدید `CALENDAR_DATE` و `EVENT`؛ متغیر RowMapper به `Map<String,Object>` تایپ شد تا خروجی `List<Map<String,Object>>` با Generic invariance سازگار باشد.
- افزودن Backend compile preflight به `build-production.cmd` پیش از Angular build تا خطاهای Java قبل از صرف زمان برای build فرانت‌اند شناسایی شوند.
- همگام‌سازی نسخه Release، Frontend و Backend روی 0.3.51.
- تمام تغییرات UI تقویم دو از FIX59/FIX60 بدون تغییر حفظ شده‌اند.

## 0.3.50-prototype-fee-p1 — FIX61: جلوگیری از اجرای UI قدیمی پس از ارتقا

- تثبیت تغییرات UI تقویم دو از FIX60 در نسخه 0.3.50 و همگام‌سازی VERSION، نسخه Angular و نسخه Backend.
- افزودن `bin/rebuild-and-start.cmd` برای Stop، Build کامل Frontend/Backend و Start فقط پس از Build موفق.
- `build-production.cmd` قبل از Build هر JAR و `frontend/dist` قدیمی را حذف می‌کند تا Artifact نسخه قبلی قابل اجرا باقی نماند.
- Build در صورت نبود Sourceهای کلیدی، شکست Verifierها، شکست Angular Build یا شکست Maven متوقف می‌شود و برنامه Start نخواهد شد.
- Guard ایستا CAL2 وجود فیلترهای نگاشت تاریخ، فیلترهای رویداد، نمایش عنوان فارسی نوع رویداد و دیاگرام روابط ۱۶ جدول را کنترل می‌کند.
- این Fix تغییری در DDL یا داده‌های CAL2 ایجاد نمی‌کند؛ Seed مناسبت‌های ثابت قمری FIX60 حفظ شده است.

## 0.3.49-prototype-fee-p1 — CAL2 FIX60

- بازنگری نگاشت تاریخ‌های تقویمی با فیلتر نوع تقویم، قرن و سال و پیش‌فرض سال جاری شمسی.
- نمایش عنوان فارسی نوع رویداد و فیلتر نوع رویداد/تقویم در فرم رویدادها.
- افزودن دیاگرام روابط ۱۶ جدول CAL2 به صفحه اصلی تقویم دو.
- افزودن Seed idempotent مناسبت‌های ثابت هجری قمری مورد استفاده در تقویم ایران.


## 0.3.48-prototype-fee-p1 — FIX59: فیلتر شمسی و تکمیل نمایش روزهای مرجع تقویم دو

- عنوان فرم `CAL2.CANONICAL_DAY` از «روزهای Canonical» به عنوان کاملاً فارسی «روزهای مرجع تقویم» تغییر کرد؛ نام فیزیکی جدول و قرارداد Dataset بدون تغییر باقی مانده است.
- فیلتر «قرن شمسی» و «سال شمسی» به بالای Grid اضافه شد و صفحه به‌صورت پیش‌فرض سال شمسی جاری Dataset را نمایش می‌دهد.
- انتخاب قرن، فهرست سال‌ها را به همان قرن محدود می‌کند؛ انتخاب سال نیز قرن متناظر را همگام می‌کند و امکان «همه قرن‌ها/همه سال‌ها» حفظ شده است.
- Grid تخصصی روزهای مرجع اکنون «نام روز هفته» و «نام ماه شمسی» را از `CAL2.WEEKDAY` و `CAL2.CALENDAR_MONTH` نمایش می‌دهد و تاریخ شمسی متناظر به‌صورت اطلاعات فرعی ماه قابل مشاهده است.
- APIهای `GET /api/v1/calendar2/reference/canonical-days/explorer` و `GET /api/v1/calendar2/reference/canonical-days/filter-meta` اضافه شدند؛ Join روی Default Persian Variant انجام می‌شود و فیلتر قرن با مرز استاندارد ۱–۱۰۰، ۱۰۱–۲۰۰، ... اعمال می‌شود.
- هیچ DDL، Migration یا تغییر در ۱۶ جدول CAL2 لازم نیست.

## 0.3.47-prototype-fee-p1 — FIX58: بازطراحی رخدادهای مناسبت‌های تقویم دو

- فرم `CAL2.EVENT_OCCURRENCE` از CRUD فنی شناسه‌ها به نمای کسب‌وکاری «رخدادهای مناسبت‌ها» تبدیل شد.
- Grid جدید عنوان مناسبت و نوع آن، تاریخ شمسی/میلادی/قمری، نوع وقوع، منشأ رخداد، تعطیل، وضعیت داده و نام منبع را نمایش می‌دهد.
- فیلتر پیش‌فرض سال شمسی جاری اضافه شد؛ فیلترهای مناسبت، منشأ، تعطیل و گزینه «نمایش همه سال‌ها» نیز در همان صفحه قرار گرفتند.
- Drawer جزئیات رخداد، قاعده مولد، سه تاریخ، روز هفته، منبع، Dataset و شناسه‌های فنی را بدون شلوغ کردن Grid نمایش می‌دهد.
- رخدادهای `GENERATED` در UI و Backend فقط‌خواندنی هستند؛ رخداد `MANUAL` قابل ویرایش/حذف و رخداد `OFFICIAL` قابل ویرایش ولی غیرقابل حذف/تنزل به دستی است.
- APIهای خلاصه `GET /api/v1/calendar2/event-recurrence/occurrences` و `GET /api/v1/calendar2/event-recurrence/occurrence-meta` اضافه شدند.
- هیچ DDL یا Migration جدیدی لازم نیست؛ ساختار ۱۶ جدول CAL2 بدون تغییر است.

## 0.3.46-prototype-fee-p1 — FIX57: رابط کسب‌وکاری مناسبت‌های تقویم دو

- فرم `CAL2.EVENT_RECURRENCE_RULE` از نمایش فنی شناسه‌ها به فرم «مناسبت‌های تقویم» تبدیل شد.
- Grid جدید عنوان و کد مناسبت، تقویم مبنا، تاریخ خوانا با نام ماه، نحوه وقوع، بازه سال، تعداد رخدادهای تولیدشده و وضعیت را نمایش می‌دهد.
- شناسه قاعده و شناسه‌های خام Event/Variant از Grid اصلی حذف شدند.
- API خلاصه قواعد با Join روی `EVENT`, `CALENDAR_VARIANT`, `CALENDAR_SYSTEM`, `CALENDAR_MONTH` و شمارش رخدادهای `GENERATED` اضافه شد.
- انتخاب `Calendar Variant` در Lookup اکنون نام فارسی سیستم تقویم را به‌عنوان عنوان و Variant Code را به‌عنوان کد فرعی نشان می‌دهد.
- ماه در فرم Rule بر اساس تقویم مبنا از `CAL2.CALENDAR_MONTH` به‌صورت عنوان فارسی انتخاب می‌شود.
- عناوین عملیاتی به «تعریف مناسبت»، «بازسازی رخدادهای مناسبت‌ها» و «ذخیره و تولید رخدادها» ساده شدند.
- هیچ DDL یا Migration جدیدی لازم نیست.

# 0.3.45-prototype-fee-p1 - FIX56

- افزودن جدول `CAL2.EVENT_RECURRENCE_RULE` به‌عنوان فرم شانزدهم CAL2 برای تعریف یک‌باره قواعد مناسبت سالانه/یک‌باره بر مبنای Calendar Variant.
- توسعه `CAL2.EVENT_OCCURRENCE` با `EVENT_RULE_ID` و `OCCURRENCE_SOURCE` برای تفکیک رخدادهای `GENERATED / MANUAL / OFFICIAL`.
- Materialize خودکار رخدادهای Rule در همان Transaction ذخیره/ویرایش؛ Generatedهای قبلی همان Rule بازسازی می‌شوند و رخدادهای دستی/رسمی بازنویسی نمی‌شوند.
- پشتیبانی از `ANNUAL_FIXED_DATE` با بازه سال اختیاری و `ONE_TIME_DATE` با سال مشخص.
- افزودن عملیات «تولید/بازسازی رخدادها» برای هر Rule و «بازسازی همه قواعد فعال» در UI تقویم دو.
- افزودن Migration قابل اجرای مجدد `database/oracle/cal2/migrations/0.3.45-fix56-event-recurrence-rule.sql` برای Schemaهای CAL2 موجود.
- به‌روزرسانی System Specification به ۲۰۱ فرم: ۲۰ GEO + ۹۹ CIF + ۵۰ DPS + ۱۶ CAL + ۱۶ CAL2.

# 0.3.44-prototype-fee-p1 - FIX55

- ساده‌سازی نام‌گذاری دو دامنه تقویم در تمام فرم‌های فعال: `CAL` با عنوان «تقویم یک» و `CAL2` با عنوان «تقویم دو».
- حذف عبارت «چهارصدساله»، «تقویم سازمانی» و عنوان نمایشی «تقویم BIAN» از منوها، Breadcrumbها، فرم‌های Dataset/Import، پیام‌های UI و System Specification.
- تغییر گروه Dataset هر دو تقویم به عنوان خنثی «Dataset تقویم»؛ Schemaها، Routeها، APIها، DDL و ساختار فیزیکی Oracle بدون تغییر باقی مانده‌اند.
- افزودن Guard ساخت `verify-calendar-display-labels.mjs` برای جلوگیری از بازگشت عنوان‌های قبلی در فرم‌های زنده.
- بدون Migration یا DDL جدید.

# 0.3.43-prototype-fee-p1 - FIX54

- افزودن Schema مستقل `CAL2` برای مدل BIAN-aligned چهارصدساله؛ بدون FK یا اشتراک فیزیکی با `CAL`.
- افزودن DDL کامل ۱۵ جدول `CAL2` شامل PK/UQ/FK/Check/Index، Comment جدول و ۱۴۴ Comment فارسی ستون.
- افزودن ۱۵ فرم مستقل CAL2 در شش گروه: تعاریف، منبع/نسخه Dataset، Dataset، مناسبت، تقویم کاری و Validation Evidence.
- `DATASET_VERSION`، `CANONICAL_DAY`، `CALENDAR_DATE`، `VALIDATION_RUN` و `VALIDATION_RESULT` از UI فقط‌خواندنی هستند؛ Event/Business Calendar برای نگهداری داده رسمی/عملیاتی قابل ویرایش‌اند.
- افزودن Import مستقیم ZIP اصلی شامل ۱۵ CSV به `/api/v1/calendar2/dataset/import` با JDBC Batch 1000، ترتیب وابستگی FK و Transaction واحد؛ بدون SQL*Loader/Oracle Client.
- رفع compile-time IOException در loop خواندن CSV که در compile harness پیش از بسته‌بندی شناسایی شد.
- افزودن مسیرهای `/calendar2/reference-data` و `/calendar2/reference-data/import` و کارت مستقل «تقویم BIAN / CAL2» زیر اطلاعات پایه.
- افزودن `CAL2` به Schemaهای قابل انتخاب در ابزارهای EA/Oracle comparison و Oracle → EA XMI export.
- به‌روزرسانی System Specification: مجموع ۲۰۰ فرم اطلاعات پایه/تقویم = ۲۰ GEO + ۹۹ CIF + ۵۰ DPS + ۱۶ CAL + ۱۵ CAL2.

# 0.3.42-prototype-fee-p1 - FIX53

- رفع خطای کامپایل Java در `CalendarDatasetImportRepository`: متد `toIso(Date value)` از `java.sql.Date` استفاده می‌کرد اما import آن در FIX52 حذف شده بود.
- `import java.sql.Date;` بازگردانده شد؛ منطق Raw Transactional Import و عدم اجرای کنترل Dataset بدون تغییر باقی مانده است.
- هیچ Migration یا DDL جدیدی ندارد.

# 0.3.41-prototype-fee-p1 - FIX52

- رفع خطای Angular TS2339 در فرم Import تقویم: حذف ارجاع Template به `minimumCanonicalDate` و `maximumCanonicalDate` که در قرارداد `CalendarDatasetImportResult` وجود نداشتند.
- نتیجه Import اکنون فقط از فیلدهای واقعی قرارداد API استفاده می‌کند: تعداد رکوردهای Insert شده، نام دو فایل و زمان اجرا.
- هیچ Dataset/count validation به مسیر Import اضافه نشده است؛ رفتار Raw Transactional Import نسخه FIX50 حفظ شده است.

# 0.3.40-prototype-fee-p1 - FIX51

- Fixed production build ordering: `tools/sync-system-specification.mjs` now runs before static verifiers.
- Prevents false build failure when `system-version.generated.ts` still contains the prior release.
- Package generated system version synchronized to `0.3.40-prototype-fee-p1`.
- No database migration.

# 0.3.39 - FIX50

- Calendar CSV import switched to raw transactional JDBC mode.
- Removed seed/empty-table checks, row-count checks, 3x ratio check, DB count verification, SHA comparison and final dataset verification.
- CSV headers are skipped without validation.
- CALENDAR_DAY is inserted first, then CALENDAR_DATE; Spring commits only after both insert streams finish successfully.
- Existing Oracle constraints are not altered by the application.

# 0.3.38-prototype-fee-p1

- FIX49: Calendar CSV JDBC import now binds CANONICAL_DATE as ISO text and converts it inside Oracle with `TO_DATE(..., 'YYYY-MM-DD')`, preventing timezone-sensitive JDBC `setDate` values from violating `CAL.CK_CAL_DAY_MIDNIGHT`.
- No database migration is required.

# 0.3.38-prototype-fee-p1

- FIX48: resolve Spring JdbcTemplate execute() overload ambiguity in calendar JDBC dataset importer by explicitly selecting ConnectionCallback<Long>.
- Restores Java 21 / Spring Boot 4.1 compilation of CalendarDatasetImportRepository.

# 0.3.36-prototype-fee-p1

- FIX47: added browser-based import of `calendar_day.csv` and `calendar_date.csv` directly into Oracle through the configured JDBC DataSource; SQL*Loader and Oracle Client are no longer required for the enterprise-calendar dataset.
- Added `/calendar/reference-data/import` under the separate calendar Reference Data menu and `/api/v1/calendar/dataset-import/*` backend endpoints.
- Import is transactional, locks both target tables against concurrent loads, requires the CAL seed contract, and refuses append when `CALENDAR_DAY` or `CALENDAR_DATE` already contains data.
- CSV headers and values are validated while streaming; Oracle inserts run in 1,000-row batches without materializing the 585k-row dataset in memory.
- Post-load validation checks the 3:1 representation contract, contiguous canonical dates/DAY_ID, JDN, ISO weekday, three calendar-system counts, and unknown system codes before commit.
- SHA-256 is calculated for both files; the supplied enterprise-calendar v1.0.0 dataset is identified by its published hashes.
- Multipart limits were raised to 64MB per file / 96MB per request in both packaged and external runtime configuration.
- Added a friendly HTTP 413 response for oversized uploads, CSV parser regression tests, and a FIX47 static build guard.
- No Oracle DDL migration is required.

# 0.3.35-prototype-fee-p1

- FIX46: Added a separate «اطلاعات پایه تقویم سازمانی» domain under Reference Data for the physical Oracle `CAL` schema supplied in the enterprise-calendar v1.0 package.
- Added 16 calendar forms grouped into core chronology, business calendar, occasions, and official Hijri override sections.
- `CALENDAR_DAY` and `CALENDAR_DATE` are intentionally read-only to protect the verified 400-year canonical dataset; other maintenance tables use parameterized Oracle CRUD.
- Added three-calendar `DAY_ID` lookup so operational calendar/occasion forms search by canonical Gregorian, Solar Hijri or Hijri Civil date instead of requiring technical IDs.
- Added CAL to configured Oracle schemas, so EA/Oracle comparison and Oracle-to-EA XMI export can target the calendar schema.
- Updated system-specification counts and architecture to include 16 CAL forms and the CAL schema.
- No Oracle migration is required; CAL physical tables are assumed to have already been installed.

# 0.3.34-prototype-fee-p1

- FIX45: corrected the regression test introduced with FIX44. The runtime SQL was already correct (`TC.VIRTUAL_COLUMN` from `ALL_TAB_COLS`), but the JUnit assertion incorrectly searched for the substring `C.VIRTUAL_COLUMN`, which is also contained inside `TC.VIRTUAL_COLUMN`.
- Replaced the ambiguous substring assertion with an alias-aware regular expression that rejects only a standalone `C.VIRTUAL_COLUMN` reference while allowing `TC.VIRTUAL_COLUMN`.
- Added positive/negative regression cases so the test proves both conditions explicitly.
- No database migration or runtime Oracle query change is required in this fix.

# 0.3.33-prototype-fee-p1

- FIX44: Oracle → EA XMI exporter no longer queries `ALL_TAB_COLUMNS.VIRTUAL_COLUMN`; Oracle exposes this flag through `ALL_TAB_COLS`.
- Column metadata keeps `ALL_TAB_COLUMNS` as the visible-column source and joins `ALL_TAB_COLS` only for `VIRTUAL_COLUMN`, including external-reference stub loading.
- Added a regression test that rejects reintroduction of `C.VIRTUAL_COLUMN` in both exporter metadata queries.
- Removed automatic full-metadata preview when the export page opens; selecting a Schema is now lightweight and metadata is read only after the user presses «پیش‌نمایش Metadata» or Export.
- No database migration is required.

# 0.3.32-prototype-fee-p1

- FIX43: added an Oracle -> Enterprise Architect physical-model exporter form under Management/System.
- Exporter reads the configured Oracle DataSource directly; no database credentials are accepted or exposed in the browser.
- Supports schema selection and table-name patterns (`%`, `_`, `*`, `?`) with a metadata preview before export.
- Generated file is EA-compatible XMI 1.1 / UML 1.3 and includes tables, columns, Oracle datatypes/length/precision/scale, nullability, defaults/identity, PK, UK, FK operations, FK associations, non-constraint indexes, check constraints, table/column comments, owner and tablespace metadata.
- Optional referenced-table stubs preserve relationships when an FK targets another schema or a table outside the selected pattern.
- Stable deterministic EA/XMI IDs are derived from owner/table/constraint names so repeated exports do not produce random object identities.
- Export output is parsed again by the existing EA XMI parser before download as a structural safety check.
- Added a dedicated backend writer unit test and `tools/verify-oracle-ea-xmi-export.mjs`; exporter static QA and the existing EA/Oracle and persisted-grid verifiers pass.

# 0.3.31-prototype-fee-p1

- FIX42: corrected the FIX41 cross-schema FK migration after Oracle returned ORA-00942 while adding `REFERENCES GEO.JOBS`.
- Root cause: parent GEO tables exist and their PK/UK metadata is visible, but the CIF schema needs a **direct REFERENCES object privilege** for cross-schema referential constraints; SELECT/catalog visibility is not sufficient.
- FIX42 no longer drops the old CIF occupation FK before validating parent objects. It supports the user's current partial state where FIX41 already dropped the old FK.
- Added separate diagnostic, DBA grant, and repair scripts. The old FIX41 migration is retained as a safe no-op deprecation marker so it cannot repeat the partial migration.
- Runtime application behavior from FIX41 is unchanged: job/group validation still uses the configured reference-data schema.

# 0.3.30-prototype-fee-p1

- FIX41: corrected the employment reference-data design after FIX40 SQL failure. The invalid `ACTIVE_FLAG` / `DISPLAY_ORDER` assumptions are removed.
- Detailed job/group UI remains sourced from `GEO.JOBS` and `GEO.JOB_GROUPS`, matching the operator-facing job catalog.
- Physical FK alignment migration replaces `PARTY_EMPLOYMENT.OCCUPATION_CODE -> CIF.REF_OCCUPATION` with `PARTY_EMPLOYMENT.OCCUPATION_CODE -> GEO.JOBS.JOB_CODE` and adds `OCCUPATION_GROUP_CODE -> GEO.JOB_GROUPS.JOB_GROUP_CODE` using `ENABLE NOVALIDATE` for existing legacy rows.
- Backend validates that the selected job and group are active in GEO and that the selected job belongs to the selected group before persistence.
- Keeps FIX40 Persian lifecycle status and unified nested onboarding stepper.

# 0.3.29-prototype-fee-p1

- FIX40: Party search grid now also resolves `LIFECYCLE_STATUS_CODE` to Persian business labels, alongside the earlier Persian rendering for verification and data-quality status.
- Unified Party onboarding navigation into one consistent nested stepper with 6 phases and 10 substeps; the operator now sees both macro phase and exact substep (`گام X از ۱۰`) in a single place instead of competing 9/12-item trackers.
- Replaced the inconsistent per-page hard-coded trackers on create/contact/financial/identifier/classification/relationship pages and added the same shared stepper to roles, KYC, consent and lifecycle pages.
- Corrected employment occupation selection so persistence aligns with `CIF.REF_OCCUPATION`; this removes the FK/runtime error that occurred when GEO job catalog values were being saved into `PARTY_EMPLOYMENT.OCCUPATION_CODE`.
- Extended `REF_OCCUPATION` seed with `HOMEMAKER` and `UNEMPLOYED` to cover the active operational vocabulary used by the employment form.

# 0.3.28-prototype-fee-p1

- FIX39: Party search grid now resolves `VERIFICATION_STATUS_CODE` and `DATA_QUALITY_STATUS_CODE` to governed Persian Reference Data labels; technical codes remain available as hover titles and Persian fallbacks cover lookup outages.
- Party creation progress header redesigned as a live four-step tracker: base Party data, person/legal-entity identity, primary identifier, then submit/continue to address and contact.
- Tracker distinguishes completed/current/upcoming steps, shows `گام X از ۴`, completed count, and a live percentage so the operator can understand progress at a glance.
- No database or backend persistence contract change is introduced.

# 0.3.27-prototype-fee-p1

- FIX38: `SCREENING_RESULT.CREATED_BY` is now populated from `X-User-Id` during screening creation.
- `CifService.createScreening` now passes the actor to `CifRepository.insertScreening`.
- Oracle insert for `SCREENING_RESULT` now includes `CREATED_BY`.
- Added QA note for the runtime `ORA-01400` found after successful Party onboarding.

# 0.3.26-prototype-fee-p1

- FIX37: هم‌راستاسازی `PARTY_MERGE_HISTORY` با Oracle 16:24؛ حذف ستون منسوخ `CREATED_DATE` از SELECT/INSERT و مدل API.
- رفع `ORA-00904: CREATED_DATE invalid identifier` در انتهای `loadParty360()` که باعث Rollback شدن تراکنش ایجاد Party می‌شد.
- حفظ `CREATED_AT` به‌عنوان زمان ایجاد canonical مطابق مدل فیزیکی Oracle.

# 0.3.25-prototype-fee-p1

- FIX36: aligned runtime/build version metadata across VERSION, backend Maven POM, and frontend package metadata.
- Added explicit Party onboarding diagnostics for organization registration country and bootstrap insert values.
- Keeps FIX35 required-column contract: ORGANIZATION.REGISTRATION_COUNTRY_CODE is validated and passed to the initial INSERT.

# 0.3.24-prototype-fee-p1

- Fixed remaining legal-entity Party onboarding rollback after reference-FK hardening: the intermediate `ORGANIZATION` insert now receives the required `REGISTRATION_COUNTRY_CODE` instead of sending NULL against the current Oracle NOT NULL contract.
- Added pre-persistence validation for Party base reference codes and the remaining primary-identifier FK-backed codes, so invalid reference values are reported as field validation errors before Oracle FK enforcement.
- Legal-entity onboarding selectors no longer invent missing ISIC/activity-status/enterprise-size/ownership codes that are absent from the active database Reference Data.
- `ORA-01400` now maps to `REQUIRED_DATABASE_VALUE_MISSING` and surfaces the missing Oracle column when available.
- Visible release version bumped so a stale 0.3.23 JAR can be distinguished immediately after restart.

# 0.3.23-prototype-fee-p1

- Added standalone FEE module entry point and Angular management workspace.
- Added comprehensive fee input UI covering catalog, versioning, applicability, calculation/tiering, currency, timing/collection, discount/waiver, allocation, posting, arrangement, simulator and reversal shell.
- Added `/api/v1/fees/prototype-metadata` and `/api/v1/fees/calculate` prototype endpoints.
- Added Oracle FEE schema physical-model baseline with configuration/runtime separation.
- Added FEE as a selectable schema in EA/Oracle model comparison.

## 0.3.22-prototype-fix33

- Extended EA/Oracle model comparison to include Persian metadata, not only physical structure.
- EA table `alias` (Persian title) and `documentation` are extracted from XMI; EA column `description` is extracted as the column Persian comment.
- Oracle table/column comments are read from `ALL_TAB_COMMENTS` and `ALL_COL_COMMENTS`.
- Column COMMENT differences now contribute to column/table `DIFFERENT` status; table title/documentation differences also contribute to table status and appear in the per-table difference detail.
- Persian/Arabic character variants, whitespace, ZWNJ/directional marks and punctuation are normalized before comment comparison to avoid cosmetic false positives.
- Because Oracle has no separate native table alias, EA `alias` is checked against the Oracle table COMMENT using normalized containment, while EA `documentation` is compared with the Oracle table COMMENT using normalized text equality.
- The supplied EA sample parses with Persian metadata on all 48 unique tables and all 816 parsed columns.
- No database migration is required.

## 0.3.22-prototype-fix32

- Corrected EA/Oracle character-length comparison so an EA `VARCHAR2(30)` with unspecified length semantics is not visually presented as different from Oracle `VARCHAR2(30 CHAR)` when semantics are not modeled in EA.
- Oracle `CHAR/BYTE` suffix remains visible and is compared when EA explicitly declares `LengthType=CHAR` or `LengthType=BYTE`.
- Duplicate EA table definitions now enrich missing column metadata from their sibling definitions, including `LengthType`, instead of discarding richer metadata when the selected definition omits it.
- Added regression coverage for implicit semantics normalization and explicit `BYTE` vs `CHAR` mismatch detection.
- No database migration is required.

## 0.3.22-prototype-fix31

- Added an explicit «جزئیات اختلاف» action in each EA/Oracle comparison row whose table status is `DIFFERENT`.
- The action selects that table and scrolls directly to the existing per-column difference grid; matching rows do not show the action.
- No backend, comparison algorithm, Oracle query or database migration change is introduced.

## 0.3.22-prototype-fix30

- Added a new Management/System screen **EA / Oracle Model Comparison** at `/system/database-model-comparison`.
- Users can upload Enterprise Architect XML/XMI exports and compare table definitions with the Oracle schemas already configured under `core-banking.schemas` in the running application.
- The EA parser reads UML table classes, columns, Oracle data types/length/precision/scale/nullability and declared primary keys; duplicate EA table definitions are merged by table name with warnings when duplicates exist.
- Oracle comparison uses `ALL_TABLES`, `ALL_TAB_COLUMNS`, `ALL_CONSTRAINTS` and `ALL_CONS_COLUMNS` for the selected configured schema; arbitrary unconfigured schema names are rejected.
- The report shows table status, EA/Oracle column counts, exact `COUNT(*)` row count for each compared table, PK status, missing/extra/changed columns, detailed per-column differences and database-only tables.
- Added CSV export of the table-level report and connection metadata display without exposing credentials.
- XML parsing is hardened against DTD/external-entity expansion (XXE).
- Added backend parser/comparison unit tests and `tools/verify-ea-oracle-comparison.mjs`, executed by `build-production.cmd`.
- The supplied EA sample was parser-smoke-tested as 53 raw table definitions -> 48 unique tables -> 816 columns, with the repeated PARTY definitions merged.
- No database migration is required for Fix30.

## 0.3.22-prototype-fix29

- Fixed Windows path resolution in `tools/verify-cif-persisted-grids.mjs`.
- The verifier now uses Node `fileURLToPath(import.meta.url)` instead of URL pathname + `path.resolve`, preventing duplicated drive paths such as `D:\\D:\\Projects\...`.
- Fix28 persisted-grid and dockable-sidebar behavior is unchanged.
- No database migration is required.

## 0.3.22-prototype-fix28

- Converted every persisted multi-record collection in CIF operational forms to explicit column tables; no stream/card-style saved-record renderer remains in `frontend/src/app/features/cif`.
- Extended the same grid rule to Party / Customer 360, including operational histories and read-only source-system collections (products, limits, interactions, complaints, alerts, analytics, groups, signatures, registration and audit).
- Added shared responsive `db-grid`/`record-table` styling: fixed-layout, wrapped cells and no horizontal scrolling on desktop; small-screen horizontal scrolling is retained only as a responsive fallback.
- Added a dockable/collapsible left sidebar (RTL `position=end`) with controls in both the rail and top bar. The compact 76px rail preserves icon navigation/tooltips and its state is persisted in localStorage; first use defaults to compact mode for maximum form width.
- Kept all Fix19-Fix27 data rules, validations, reference lookups and database mappings unchanged; Fix28 is a presentation/layout-only release and requires no new database migration.
- Added static QA checks for stream renderers, table tag balance, table coverage, sidebar bindings and generated version synchronization.

## 0.3.22-prototype-fix27

- Fixed false red validation state in Contact Point-to-Address association after successful save by resetting the native FormGroupDirective submitted state as well as the reactive form state.
- Replaced stream/card-style saved records on the Address & Contact page with explicit column grids for addresses, contact points, and contact-address associations.
- Association grid now separates contact, address, association type, primary status, validity dates, and actions into dedicated columns.
- Contact/address labels in association history resolve Persian business titles instead of displaying raw reference codes when lookup data is available.
- Added application-level duplicate validation for CONTACT_POINT_ADDRESS before Oracle UQ_CPA_CONTACT_ADDRESS is reached; users now receive a clear Persian validation message.
- Clarified Party Role context semantics in the UI: context is an optional scope for a role and is normally left blank for a bank-wide CUSTOMER role.
- No database schema migration is required for Fix27. Existing Fix24 migration remains required wherever REF_AUTHORITY_DOCUMENT_TYPE has not yet been created.

## 0.3.22-prototype-fix26

- Legal entity registration now follows Country -> Registration City; city is selected from GEO.CITIES for Iran or GEO.FOREIGN_CITIES filtered by the selected country.
- Backend rejects a registration city that does not belong to the selected registration country.
- Employee-count hint states the onboarding provenance rule: customer declaration first, verified documentary value when available.
- End-to-End readiness now shows the detail/reason of every requirement instead of only the section title.
- KYC readiness explains exactly which finalization fields are missing and points to section 8.1 when a linked risk assessment already exists.
- Final KYC risk level is now required consistently in the full KYC form and the Party 360 quick form.

## 0.3.22-prototype-fix25

- UX guidance: low-contrast placeholder styling plus contextual examples in touched operational edit boxes.
- Risk Assessment 8.2 reordered as model -> version/min/max -> score -> rating/decision; score remains dynamically validated by model range.
- External Inquiry 8.4 provider changed from free text to REF_EXTERNAL_PROVIDER searchable combo; backend validates inquiry type/provider/result references.
- General Preference 9.3 now uses type-appropriate controls: language selection, HH:mm contact time, and REF_CHANNEL for statement delivery; backend validates type/value compatibility.
- Additional identifier types already registered for a Party are removed from the add list (current edit type remains available).
- Classification types already registered for a Party are removed from the add list (current edit type remains available).
- Fix24 authority document combo, formatted max amount, and IRR default behavior preserved unchanged.

## 0.3.22-prototype-fix24

- Reworked Party Authority section C: mandatory authority-document reference is now selected by Persian title from governed `CIF.REF_AUTHORITY_DOCUMENT_TYPE`; its code is persisted in `PARTY_AUTHORITY.DOCUMENT_REF` and validated server-side.
- Added eight operational authority-document reference values (official power of attorney, board resolution, court order, guardianship, executorship, articles of association, official delegation, other official document) with Persian Oracle comments and reference-catalog metadata.
- `MAX_AMOUNT` now uses the shared thousands-separated amount input while preserving numeric API/database persistence.
- Authority-limit currency now defaults to `IRR` / «ریال ایران» in the UI; when no amount is entered the display default is not persisted, preserving the existing `CK_AUTH_AMOUNT_CURRENCY` invariant.
- Backend defaults a missing currency to IRR only when a maximum amount exists, validates the currency against active `GEO.CURRENCIES`, and validates authority type/scope/document codes against active Reference Data.
- Authority history rows now display Persian authority type, scope, document reference and currency titles instead of raw codes where lookup data is available.
- Added idempotent migration `0.3.22-fix24-party-authority-reference-currency.sql`; no Person/Organization identity model changes are introduced.

## 0.3.22-prototype-fix23

- Fixed Angular compile error in `Party360Component`: added the missing `workflowStatuses` signal used by the KYC status selector.
- Added loading of `CIF.REF_WORKFLOW_STATUS` to Party 360 lookup initialization.
- No database schema or migration change is required for Fix23.
- Fix22 legal-entity behavior is otherwise unchanged.

## 0.3.22-prototype-fix22

- Legal-entity-only UX/data alignment: Organization economic-profile fields now use business titles in UI while persisting governed codes in `CIF.ORGANIZATION`; Person identity forms and Person model mappings remain unchanged.
- `ISIC_CODE` is selected through a searchable Persian-title selector backed by `CIF.REF_ISIC_ACTIVITY`; known opaque prototype seeds are re-aligned to Persian titles.
- Added dedicated Reference Data tables `REF_ORGANIZATION_ACTIVITY_STATUS`, `REF_ENTERPRISE_SIZE`, and `REF_OWNERSHIP_TYPE` instead of overloading workflow/classification vocabularies.
- `ACTIVITY_STATUS_CODE`, `ENTERPRISE_SIZE_CODE`, and `OWNERSHIP_TYPE_CODE` are validated against their dedicated active Reference Data domains before persistence.
- `EMPLOYEE_COUNT` is explicitly numeric/non-coded and is captured once in Legal Entity Identity; the duplicate employee-count field was removed from section 3.1 while the existing value is preserved on activity-profile updates.
- Reworked Organization form layout: main-activity description spans the full form width; enterprise size and ownership are Persian ComboBoxes; section 3.1 explains ownership of employee count.
- Existing Fix19 multi-record business-context grids (address, contact, identifiers, classifications, financial profile, income sources, assets/liabilities and other Party histories) are retained without regression.
- Added idempotent migration `0.3.22-fix22-organization-economic-profile.sql` and expanded the governed Party Reference catalog from 99 to 102 active tables.

## 0.3.22-prototype-fix21

- Added governed Persian Oracle `COMMENT ON COLUMN` metadata for all 99 CIF Party reference tables.
- Covered all 991 columns reported by the database missing-comment inventory and all 994 columns in the current governed metadata, including Fix20 `REF_RISK_MODEL.MODEL_VERSION`, `MIN_SCORE`, and `MAX_SCORE`.
- Added safe/idempotent migration `0.3.22-fix21-reference-column-comments-fa.sql`; missing tables/columns are skipped with DBMS_OUTPUT instead of failing the migration.
- Added the same Persian comments to all six reference-data phase DDLs for clean/fresh installations.
- Added `CIF-0.3.22-FIX21-REFERENCE-COLUMN-COMMENTS-FA.csv` and QA report for traceability.

## 0.3.22-prototype-fix20

- Risk Assessment UX and reference-governance alignment: risk model version and permitted score range are now system-owned Reference Data rather than manual user inputs.
- Extended `CIF.REF_RISK_MODEL` with `MODEL_VERSION`, `MIN_SCORE`, and `MAX_SCORE`; added idempotent migration `0.3.22-fix20-risk-model-profile.sql`.
- Selecting a risk model now automatically loads its version and score domain; model version is read-only and the score input shows/enforces the configured minimum/maximum.
- Risk submit is no longer silently disabled by generic form invalidity; invalid submissions produce a field-specific Persian message. The button remains disabled only during model-profile loading or save operations.
- Backend validates KYC ownership, risk type, risk level, decision, model/version consistency and score bounds before persistence.
- Added `GET /api/v1/cif/risk-models/{modelCode}/profile` for governed model metadata.

## 0.3.22-prototype-fix19

- مبنای Schema این Fix، Metadata تحویلی `CIF-tables-2026-08-22-1200.xlsx` است.
- بازطراحی نمایش رکوردهای ذخیره‌شده در فرم‌های عملیاتی Party با حداقل Business Context؛ Gridها دیگر صرفاً ID/Code/شماره تماس نمایش نمی‌دهند.
- تکمیل Grid راه‌های تماس با نوع/کاربرد تماس، اصلی/تأییدشده، وضعیت تأیید و دوره اعتبار.
- تکمیل تاریخچه پروفایل مالی با درآمد ماهانه/سالانه، گردش مورد انتظار، دارایی، بدهی، خالص ثروت، توان مالی و وضعیت احراز.
- تکمیل Grid منابع درآمد/وجوه، دارایی‌ها و تعهدات، شناسه‌های تکمیلی و طبقه‌بندی‌ها.
- تکمیل Party Role Context: نوع زمینه از REF_CONTEXT_TYPE؛ شناسه زمینه برای PRODUCT و CASE از داده موجود قابل انتخاب است و مقدار در PARTY_ROLE.CONTEXT_ID ذخیره می‌شود؛ برای Contextهای Domainهای آتی شناسه مرجع سامانه مبدا قابل ثبت است.
- تکمیل Gridهای KYC/Risk/Screening، روابط، رضایت‌ها/ترجیحات و چرخه عمر برای نمایش حداقل داده گویا.
- اصلاح نام فنی نمایش داده‌شده CIF.PARTY_IDENTIFIER در نمای 360 و حذف *ngIf باقی‌مانده در KYC برای جلوگیری از Angular standalone warning.
- Migration جدید `0.3.22-fix19-party-grid-role-context.sql` عناوین فارسی و مقادیر GLOBAL/ACCOUNT/PRODUCT/BRANCH/CONTRACT/CASE را در REF_CONTEXT_TYPE همگام می‌کند.
- ابزار `package-release.cmd` برای تولید ZIP تمیز بدون node_modules/dist/target/cache/log/runtime artifacts اضافه شد.

## 0.3.22-prototype-fix17

- Fixed Party-document persistence UX: the file attachment is now explicitly marked mandatory, save/upload success and failure messages are shown inside the document section, and invalid save attempts list the exact missing fields instead of failing silently/out of view.
- Removed the invalid `CK_DOC_VERIFY_DATE` rule (`VERIFIED_AT >= CREATED_AT`), because a business verification timestamp may legitimately precede the database-row creation timestamp; an idempotent migration drops it on existing schemas.
- Fixed the shared Persian date picker so clicking `امروز` immediately writes the selected Gregorian ISO value back to the Angular FormControl and displays the Persian date, without requiring a second click on `انتخاب`.
- Reworked Party classification UX: type, value and assignment reason are explicitly reference-data-backed searchable ComboBoxes; the value Combo is filtered by classification type; submit is no longer silently disabled by invalid state and instead reports missing fields.
- Expanded and corrected classification Reference Data: 4 classification types, 20 type-specific values and 4 Persian assignment reasons are seeded; fresh-install metadata and existing-database migration are aligned.
- Resets the Angular submitted state after successful classification persistence to prevent false validation highlighting.

## 0.3.22-prototype-fix16

- Fixed Angular strict-template/TypeScript build failure in `party-financial-employment.component.ts` by allowing the shared lookup `label()` helper to accept `undefined`, matching optional lookup codes used by workflow status and asset/liability rows.
- No business behavior, database schema, or API contract changed from Fix15.

## 0.3.22-prototype-fix15

- Fixed external-employer entry with an explicit internal/external employer mode; the business user can now either select an existing CIF Party employer or enter an external employer name/identifier, while the existing backend XOR invariant remains enforced.
- Hardened economic-activity display so Persian ISIC titles are shown instead of numeric codes; aligned runtime metadata, fresh-install seeds and an idempotent migration.
- Populated income/source-of-funds and operational status ComboBoxes from Reference Data with controlled fallbacks; income rows now show type, amount/currency, status and documentation state instead of an ambiguous single title.
- Replaced raw asset/liability `ITEM_TYPE_CODE` with a 13-value Persian business selector, populated asset/liability status from shared workflow statuses, and added matching backend validation.
- Corrected post-save UX for additional identifiers and Party documents by resetting Angular submitted state and clearing residual focus after successful persistence.
- Reworked Party-document handling: `CONTENT_HASH` and `STORAGE_REF` are system-owned and no longer user inputs. Added PDF/JPEG/PNG/TIFF upload (20 MB), server-side SHA-256, private file storage with opaque `cif-doc:` references, and document-file retrieval. Scanner output files are supported; direct hardware scanning is explicitly reserved for an approved local scanner Agent/middleware.
- Added migration `0.3.22-fix15-operational-lookup-alignment.sql` and QA report `CIF-0.3.22-FIX15-OPERATIONAL-FORMS-QA.md`.

## 0.3.22-prototype-fix14

- Financial/employment operational UI now shows meaningful Persian economic-activity titles instead of raw ISIC codes; the existing REF_ISIC_ACTIVITY seed values and runtime metadata are corrected and an idempotent migration updates existing databases.
- Household size is captured as an actual positive integer (`FAMILY_RANGE` remains schema-compatible text), while employer employee range is a controlled ComboBox with predefined operational ranges.
- Added one reusable formatted-amount ControlValueAccessor and applied thousands separators to employment monthly income, financial monthly/other/annual income, expected turnover, assets, liabilities, real estate, investments, installments, estimated net worth, income-source amount, and asset/liability amount. Persian/Arabic digits and separators are normalized before persistence.
- Financial capacity and financial relationship purpose are now title-based selectors; controlled codes remain persisted in `FINANCIAL_PROFILE`.
- Funds origin/destination countries are populated from the existing GEO countries Reference Data and selected through a multi-select; country codes are serialized into the existing `FUNDS_COUNTRIES_TEXT` column without a schema change.
- Added migration `0.3.22-fix14-financial-employment-ui.sql`; no new synthetic Reference Data tables were introduced for fields that have no physical REF table in the supplied CIF model.
- Reset submitted state after successful saves across financial profile, income source, asset/liability, and license forms to prevent false Required/error highlighting after form reset.

## 0.3.22-prototype-fix13

- Address geography flow no longer requires district/region before city: Province -> County enables city selection; city options are aggregated from all districts of the selected county and the selected CITY_CODE is still persisted.
- Fixed Angular Material post-submit error state for address, contact, and employment forms by resetting the FormGroupDirective submitted state after successful persistence.
- Employer Party is selected by searchable Persian Party lookup; raw EMPLOYER_PARTY_ID is not shown to the user but is persisted. External employer name/identifier remain available only when no CIF employer Party is selected.
- Employment job group and job now use GEO.JOB_GROUPS and GEO.JOBS; users select Persian titles while JOB_GROUP_CODE/JOB_CODE are persisted.
- Economic activity and currency selectors display Persian titles only; employment income defaults to Iranian Rial and monthly income displays thousands separators.
- Added database-backed CIF.REF_CONTRACT_TYPE with Permanent/Temporary/Contractor/Self-employed operational values and server-side validation of CONTRACT_TYPE_CODE.
- Added migration `0.3.22-fix13-employment-reference-ui.sql`; CIF reference catalog is now 99 forms / 169 total reference forms.

## 0.3.22-prototype-fix12

- Party identifier UI now uses a single issuing-authority selector; the duplicate issuer-code selector was removed.
- `ISSUER_CODE` is derived from the selected issuing authority for compatibility with the current `UQ_IDENTIFIER` key.
- CIF validation responses preserve `HttpErrorResponse`, so field-level `ProblemDetail.fieldErrors` are shown instead of a generic log-only message.
- Duplicate identifier validation wording now refers to the issuing authority selected by the user.

## 0.3.22-prototype-fix11

- `CIF-tables5.xlsx` به‌عنوان آخرین Schema اجرایی Oracle مبنا قرار گرفت: 146 جدول، 1795 ستون، 98 جدول مرجع و 48 جدول عملیاتی.
- SQLهای `PARTY_RISK_ASSESSMENT` با Schema جاری همگام شدند و ارجاع به `CREATED_DATE / LAST_MODIFIED_BY / LAST_MODIFIED_DATE` حذف شد.
- SQL ثبت `SCREENING_RESULT` با Schema جاری همگام شد و ارجاع به `CREATED_DATE` حذف شد.
- DDL snapshot از comment/ALTERهای مربوط به ستون‌های حذف‌شده پاک شد.
- نمایش کد لاتین از Comboها و Searchable Comboهای فرم‌های CIF حذف شد؛ کد همچنان فقط در API/Database نگهداری می‌شود.
- 242 عنوان Material، 126 عنوان فیلد Native، 191 گزینه Material و 43 Searchable Combo از نظر نمایش پیش‌فرض فارسی بازبینی شدند.
- پیام fallback ثبت Party فارسی و تفکیک خطای عدم ارتباط با سرویس اصلاح شد.

## 0.3.22-prototype-fix10

- Fixed backend compilation regression in CIF identifier date validation: `validateIdentifier` now uses the existing `CifModels.PersonProfile` type returned by Party 360/current Party data instead of the nonexistent `PersonRecord` type.
- No database migration is required for this patch.

## 0.3.22-prototype-fix9
- Hardened identity dates end-to-end: a person's birth date is required and must be strictly before the system date; identifier issue date cannot be in the future and cannot precede the person's birth date; identifier expiry date cannot precede issue date. Equal-to-today birth dates receive a dedicated Persian validation message instead of the generic required-fields message.
- Contact validity hardened: `VALID_TO` accepts the system date or later and must not precede `VALID_FROM`.
- Contact verification time is system-controlled in the UI; new/re-verified contact points receive the current system date/time and cannot be back-dated. Historical verification timestamps remain edit-safe when unchanged.
- `CONTACT_POINT.OWNER_TYPE_CODE` is now a Persian ComboBox using the operational-form values `CUSTOMER / REPRESENTATIVE / COMPANY` (خود مشتری / نماینده / شرکت), with server-side validation.
- Contact verification-status and verification-method choices now display Persian labels only; persisted reference codes remain unchanged.
- Employment status is now selectable from the operational-form vocabulary (شاغل، خویش‌فرما، بازنشسته، خانه‌دار، دانشجو، بیکار); `JOB_STATUS` and `EMPLOYMENT_STATUS_CODE` are normalized to the same selected code and validated server-side.
- Added dedicated database-backed `CIF.REF_ADDRESS_SOURCE` because the Party address operational form has its own source vocabulary distinct from generic `REF_DATA_SOURCE`: `CUSTOMER_DECLARATION` (اظهار مشتری), `POSTAL_SYSTEM` (سامانه پست), `RESIDENCE_DOCUMENT` (مدرک سکونت).
- Added idempotent migration `0.3.22-fix9-contact-date-address-source.sql` and synchronized the CIF reference catalog to 98 runtime forms / 168 total reference forms.

## 0.3.22-prototype-fix8
- Party creation: localized the system display values `UNVERIFIED` and `INCOMPLETE` to Persian while keeping the persisted/API codes unchanged.
- `PARTY_IDENTIFIER.ISSUER_CODE` is no longer free text; it is selected from `CIF.REF_ISSUING_AUTHORITY`, matching the current model/index metadata.
- Server-side identifier validation now checks both `ISSUING_AUTHORITY_CODE` and `ISSUER_CODE` against active `REF_ISSUING_AUTHORITY` rows and normalizes both codes to uppercase.
- No database migration is required.

## 0.3.22-prototype-fix7
- Party creation: replaced free/manual birth-place entry with cascading Province -> County -> City selection for Iranian birth locations.
- City choices are reduced to the selected county; the application resolves its districts internally and shows only their cities. The city ComboBox is searchable by city name/code and returns at most the first 100 unfiltered options.
- The selected GEO.CITIES.CITY_ID is persisted in CIF.PERSON.BIRTH_PLACE_ID; the previous onboarding payload bug that always sent birthPlaceId=null is removed.
- Persian is ordered first in language lookups and remains the default for both name language and preferred language.
- PERSON.PHYSICAL_ABILITY is now a ComboBox with the two operational-form values: NORMAL / ACCESS_NEEDED.
- Primary/Active labels in the initial identifier section are localized to Persian; issuer-code wording is clarified.
- No database migration is required.

## 0.3.22-prototype-fix6
- Refreshed the System Specification page to the actual final CIF/Party scope instead of the old GEO/DPS-only snapshot.
- Updated live scope metrics to 167 Reference Data forms (20 general/GEO + 97 CIF Party/Customer + 50 DPS), 12 Party operational screens, and 48 covered CIF operational tables (30 workflow + 18 read-only 360 sources).
- Updated Oracle scope to the three active schemas CIF / GEO / DPS and refreshed architecture, technology and capability descriptions for Party onboarding, Customer role, KYC/Risk, Consent, Lifecycle/Merge, Persian dates, runtime logging and Party/Customer 360.
- Updated the page review date to 2026-08-18. No database migration is required.

## 0.3.22-prototype-fix5
- PARTY_ROLE runtime schema correction: removed legacy `CREATED_DATE`, `LAST_MODIFIED_BY`, and `LAST_MODIFIED_DATE` from role INSERT/UPDATE SQL because these columns have been removed from the current Oracle schema.
- Updated the bundled CIF DDL snapshot and Phase 7 documentation so new environments do not recreate the removed PARTY_ROLE audit columns.
- No database migration is required for this fix; existing databases that still contain the legacy columns remain compatible because the application no longer references them.

## 0.3.22-prototype-fix4
- Synchronized `PartyReferenceMetadataRegistryTest` with the new `REF_TENURE_TYPE` reference table introduced by Address Fix 2/3.
- Updated the Party reference catalog runtime count from 96 to 97 while preserving the original 104-source-definition accounting (96 source CIF references + 8 GEO/DPS mappings, plus the local tenure extension).
- Added explicit test coverage for `ref-tenure-type`.

## 0.3.22-prototype-fix3

- Party Address UI realigned with the operational reference form: structured address fields are shown in the same two sections (address details / status and validity).
- Removed user-facing ADDRESS_LINE1/ADDRESS_LINE2 fields; ADDRESS_LINE1 is derived from structured street/plaque/floor/unit fields and ADDRESS_LINE2 remains compatibility-only.
- SOURCE_CODE remains database-backed via CIF.REF_DATA_SOURCE and TENURE_TYPE_CODE via CIF.REF_TENURE_TYPE.
- Postal code, main street and plaque are now required in the UI; Iranian addresses also require province/county/city selections.
- Persian valid-from/valid-to fields are kept and explicitly labeled as address validity, not verification timestamps.

## 0.3.22-prototype-fix2

- Party Address: `SOURCE_CODE` is now selected from `CIF.REF_DATA_SOURCE` instead of free text.
- Party Address: added database-backed `CIF.REF_TENURE_TYPE` for `TENURE_TYPE_CODE` with Owner/Tenant/Organizational/Other seed values per the operational form.
- Party Address: removed the independent `ADDRESS_LINE2` control from the operational UI; the physical optional column remains preserved for compatibility while `ADDRESS_DETAIL` is the single supplementary-address field shown to the user.
- Party Address: clarified `VALID_FROM/VALID_TO` labels as the address-validity interval; no unsupported verification timestamp range was invented.
- Reference lookups now honor `IS_ACTIVE`, `VALID_FROM` and `VALID_TO`; address reference codes are also validated server-side.
- Added idempotent Oracle migration `0.3.22-fix2-address-reference-alignment.sql`.

## 0.3.22-prototype

- Completed Party Operations Phase 11 as the final Party / Customer 360 and end-to-end hardening slice; no new source-domain CRUD was introduced.
- Added a calculated `Party360SummaryRecord` matching the conceptual EA 360 summary without creating a synthetic physical `PARTY_360_SUMMARY` table.
- Added read-only 360 aggregation for the 18 remaining read-only 360 operational tables: products/restrictions/limits, interactions/journey, complaints/alerts, segment/value/metrics/recommendations, organization officers/groups/signatures, registration request and audit metadata.
- Intentionally excludes `SIGNATURE_IMAGE` payload and `AUDIT_EVENT.BEFORE_DATA/AFTER_DATA` payloads from the 360 API; only safe summary/metadata is returned.
- Added `GET /api/v1/cif/parties/{partyId}/readiness` to report workflow completion without changing lifecycle status; customer-specific requirements are conditional on an active Customer Role.
- Hardened customer readiness so the current `PARTY_CUSTOMER` must reference one of the Party's active Customer Role records before its customer number satisfies the workflow.
- Added the final 360 overview UI with summary metrics, readiness blockers and read-only source-system cards, while preserving existing maintenance tabs and routes.
- Closed the remaining backend table-coverage gap: all 48 non-REF operational tables from `CIF-tables4.xlsx` are now referenced by CIF backend code; 18 are explicitly read-only 360 aggregates.
- Added `database/oracle/cif/migrations/0.3.22-registration-request-alignment.sql` because `PARTY_REGISTRATION_REQUEST` exists in the current metadata/EA model but not in the historical bundled CIF DDL snapshot.
- Added `docs/CIF-0.3.22-OPERATIONAL-TABLE-COVERAGE.csv` and Phase 11 documentation for final schema/workflow traceability.

## 0.3.21-prototype

- Added Party Operations Phase 10 at `/cif/parties/{partyId}/operations/lifecycle-merge`, following the supplied operational forms for time-bound lifecycle status changes and Party merge.
- Added append-only `PARTY_STATUS_HISTORY` to Party 360 and a dedicated transactional status-change API that closes the current open period, creates the next period and updates `PARTY` with optimistic locking.
- Reserved lifecycle status `MERGED` for the dedicated merge operation; ordinary status changes cannot move a Party into or out of MERGED.
- Added `PARTY_MERGE_HISTORY` to Party 360 and a transactional merge API that records source/target/reason/conflict handling, marks the source Party as `MERGED`, sets `MERGED_INTO_PARTY_ID` and writes the corresponding status history.
- Merge requires source and target to be distinct, non-merged and of the same Party type; the real `DUPLICATE_MERGED` status reason from `REF_PARTY_STATUS_REASON` is used for the lifecycle transition.
- Kept `MERGE_REASON_CODE` and `CONFLICT_RESOLUTION_CODE` free of synthetic Reference Data because `CIF-tables4.xlsx` defines no physical REF tables for those columns; UI choices are the exact operational options from the supplied HTML.
- Added idempotent migration `database/oracle/cif/migrations/0.3.21-party-lifecycle-merge.sql`, including safe sequence creation from `MAX(ID)+1`, table creation when missing, and physical `PARTY_MERGE_HISTORY.CREATED_DATE` alignment.
- New Party creation now records its initial lifecycle row using the real `NEW_REGISTRATION` status reason, making lifecycle history complete for newly created Parties.
- Added direct navigation from Phase 9 and Party 360 to lifecycle/merge operations.
- Aligned Merge with the supplied operational form: currently-valid `PARTY_NAME`, active/current `PARTY_IDENTIFIER`, and currently-valid `PARTY_CLASSIFICATION` rows follow the canonical target; expired/historical rows remain on the merged source for audit, target primary name/identifier wins on primary conflicts, and duplicate classification periods are not re-created.

## 0.3.20-prototype

- Phase 9 build hotfix: corrected `DatabaseTablesComponent` import path in `party-consents-preferences.component.ts` so Angular can resolve the shared standalone component.

- Added Party Operations Phase 9 at `/cif/parties/{partyId}/onboarding/consents-preferences`, following the supplied HTML workflow after KYC/Risk.
- Added end-to-end `PARTY_CONSENT` CRUD semantics: create/update and lifecycle-preserving revoke, customer decision, capture channel, declaration time, validity, consent-text version, multi-scope text, limitations and evidence.
- Aligned `PARTY_CONSENT` to all 21 columns in `CIF-tables4.xlsx` and added idempotent migration `database/oracle/cif/migrations/0.3.20-consent-preference-alignment.sql`.
- Added `COMMUNICATION_PREFERENCE` CRUD with server-searchable channel/purpose, allowed flag, preferred time window, language, allowed days, time zone and marketing opt-out; aligned all 16 physical columns.
- Added `PARTY_GENERAL_PREFERENCE` CRUD for service/general preferences using actual `REF_PREFERENCE_TYPE` and `REF_SOURCE_SYSTEM`; overlapping validity periods for the same type are blocked.
- Reused the existing Phase 4 `PARTY_DOCUMENT` workflow instead of creating duplicate document CRUD inside Phase 9.
- Added Consent/Preference data to Party 360 and linked Phase 8 directly to the new operational form.
- Preserved physical-model gaps explicitly: no synthetic `REF_LANGUAGE`, consent-text-version, time-zone or allowed-days tables were introduced, and no unsupported preference types were seeded.
- Kept customer rejection distinct from consent lifecycle: `CUSTOMER_DECISION_CODE=REJECT` is authoritative; because the supplied status catalog has no `REJECTED`, the lifecycle remains non-granted without inventing a new reference code.

## 0.3.19-prototype

- Added Party Operations Phase 8 at `/cif/parties/{partyId}/onboarding/kyc-risk` for the supplied operational KYC/Risk/Screening workflow.
- Aligned `KYC_CASE` with the current 27-column `CIF-tables4.xlsx` model, adding the nine operational customer-understanding/PEP/EDD fields end-to-end.
- Added idempotent migration `database/oracle/cif/migrations/0.3.19-kyc-case-alignment.sql` for existing databases; no synthetic FK/REF or new Y/N database checks were introduced where the supplied physical metadata has none.
- Upgraded existing `PARTY_RISK_ASSESSMENT` and `SCREENING_RESULT` CRUD into the dedicated onboarding workflow with optimistic locking and searchable Reference Data.
- Added `EXTERNAL_INQUIRY_RESULT` CRUD, validation and Party 360 visibility; payload reference/hash pairing and request/response/expiry rules mirror the supplied database constraints.
- Added an application guard that prevents physical KYC-case deletion while risk, screening or document records still depend on it.
- Fixed server-searchable Party Reference combos by normalizing `REF_*` UI resource names to the API's kebab-case resource contract centrally in `CifService`.
- Corrected the bundled base DDL so the nine Phase 8 columns belong to `KYC_CASE` only and `ADDRESS` remains unchanged.
- Corrected the root release marker from the inherited 0.3.17 value to `0.3.19-prototype`; backend and frontend versions are synchronized.

## 0.3.18-prototype

- Added Party Operations Phase 7 at `/cif/parties/{partyId}/onboarding/roles` for Party Role and banking-customer relationship management.
- Implemented `PARTY_ROLE` CRUD with optimistic locking, server-searchable `REF_ROLE_TYPE`, `REF_CONTEXT_TYPE` and `REF_WORKFLOW_STATUS`, optional context pairing and related/principal Party selection.
- Implemented the source-model boundary `Party -> Role -> Customer`: only role type `CUSTOMER` creates `PARTY_CUSTOMER` and a customer number; all other roles remain Party roles without customer numbers.
- Added Party 360 role/customer data and a dedicated «نقش‌ها و رابطه بانکی» tab.
- Added idempotent migration `database/oracle/cif/migrations/0.3.18-party-role-customer.sql`: aligns the historical 16-column `PARTY_ROLE` snapshot to the current 24-column model and creates the 13-column `PARTY_CUSTOMER` model when absent.
- Added operational role values from the supplied Party form to `REF_ROLE_TYPE` without changing the generic Role model.
- Added isolated prototype `SEQ_CUSTOMER_NO`; the supplied EA/XMI defines `CUSTOMER_NO` but does not define a bank numbering algorithm, so this sequence is explicitly replaceable by the production customer-number policy.
- Customer roles are not physically deleted; they are closed by status/end-date so `CUSTOMER_NO` history remains stable.
- Fixed a duplicated `SELECT` token in the existing `PARTY_DOCUMENT` read query discovered during Phase 7 QA.

## 0.3.17-prototype

- Added Party Operations Phase 6 at `/cif/parties/{partyId}/onboarding/relationships` for Party-to-Party relationships, beneficial ownership/UBO and authority/representation.
- Added end-to-end CRUD and optimistic locking for `PARTY_RELATIONSHIP`, `BENEFICIAL_OWNERSHIP` and `PARTY_AUTHORITY`.
- Added server-searchable Party selection to the reusable ComboBox flow; related parties are searched by Party ID, name or primary identifier and self-relationship is rejected.
- Added relationship semantic validation: family relations are PERSON-to-PERSON, parent-company/affiliate are ORGANIZATION-to-ORGANIZATION, and BENEFICIAL_OWNER requires a positive ownership/control percentage.
- Added UBO rules: ORGANIZATION-only ownership records, at least one direct/indirect/control percentage, 0..100 validation and `REF_CONTROL_BASIS` lookup.
- Added authority rules using `REF_AUTHORITY_TYPE` and `REF_AUTHORITY_SCOPE`, amount/currency pairing and source-model semantics where `PRINCIPAL_PARTY_ID` is the grantor while `PARTY_ID` mirrors the authorized holder.
- Added Phase 6 data to Party 360 and linked Phase 5 directly to the new operational step.
- Added idempotent migration `database/oracle/cif/migrations/0.3.17-party-relationship.sql` because the supplied current `CIF-tables3.xlsx` contains `PARTY_RELATIONSHIP` while the historical repository DDL snapshot does not; missing sequences continue from `MAX(ID)+1` when data already exists.
- Kept `RELATIONSHIP_TYPE_CODE` as an application-controlled code list because the supplied model does not define an explicit relationship-type REF table; no synthetic REF catalog was introduced.

## 0.3.16-prototype

- Added Party Operations Phase 5 for `CIF.PARTY_CLASSIFICATION`.
- Added create/update/delete APIs with optimistic locking and reference validation.
- Added dependent lookup for `REF_CLASSIFICATION_VALUE` filtered by classification type.
- Added reusable server-searchable ComboBox UI component with debounce.
- Added classifications to Party 360 response and UI.
- Aligned `PARTY_CLASSIFICATION.DESCRIPTION_TEXT` with `CIF-tables3.xlsx` and added an idempotent Oracle migration.
- Added onboarding route `/cif/parties/:partyId/onboarding/classifications`.

## 0.3.15-prototype

- Added Party operational-form Phase 4 at `/cif/parties/{partyId}/onboarding/identifiers-documents`, linked from Phase 3 and Customer 360.
- Added a protected read-only Primary Identifier section and operational CRUD for secondary `PARTY_IDENTIFIER` records; Phase 4 never promotes, demotes or deletes the primary identity created during Phase 1.
- Added reference-backed identifier fields for identifier type, country, issuing authority, verification status, data source and verification method, with validity/issue/expiry controls and optimistic `RECORD_VERSION` editing.
- Added application validation for the Oracle `UQ_IDENTIFIER` business key so duplicate type/value/issuer/valid-from combinations are rejected before a raw Oracle constraint error reaches the UI.
- Added a dedicated `PARTY_DOCUMENT` operational editor including optional KYC association, document type/number, issuer, dates, verification metadata, content hash, secure storage reference and MIME type.
- Synchronized `PARTY_DOCUMENT` with the latest supplied `CIF-tables3.xlsx` by adding `ISSUING_AUTHORITY_TEXT`, `CONTROL_STATUS_CODE` and `DESCRIPTION_TEXT` end-to-end in domain/API, Oracle repository and UI.
- Added an idempotent Oracle migration `database/oracle/cif/migrations/0.3.15-party-document-alignment.sql` for existing environments and aligned the bundled CIF DDL snapshot.
- When a document type matches the Party primary identifier type, the operational form reads the document number from that primary identifier instead of requesting a second manually-entered identity value.
- No synthetic reference catalog was created for `PARTY_DOCUMENT.CONTROL_STATUS_CODE` because the supplied model does not define an explicit REF source for that column.

## 0.3.14-prototype

- Added Party operational-form Phase 3 at `/cif/parties/{partyId}/onboarding/financial-employment` and linked Phase 2 plus Customer 360 directly to this step.
- Added operational CRUD and Party 360 coverage for `FINANCIAL_PROFILE`, `PARTY_EMPLOYMENT`, `PARTY_INCOME_SOURCE`, `PARTY_ASSET_LIABILITY` and `PARTY_LICENSE`, aligned to the supplied `CIF-tables3.xlsx` column set.
- Preserved the model boundary that `PARTY_EMPLOYMENT` is PERSON-only; ORGANIZATION economic activity is maintained on `ORGANIZATION`, with activity licenses in `PARTY_LICENSE`.
- Added financial snapshot fields from the latest schema including net/other monthly income, expected transaction count, funds countries, relationship-purpose code, real-estate/investment values, monthly installments, estimated net worth and financial-capacity code.
- Added employment fields from the latest schema including employment status, occupation group, employer identifier, contract type, insurance number and tax code.
- Added source-of-funds/wealth, tax, occupation, economic-sector, ISIC, verification and license-type lookups only where explicit reference sources exist; no synthetic REF mapping was introduced for unmapped code columns.
- Added database-constraint-aware validation: exactly one employer source for employment, unique financial snapshot per Party/as-of date, license type/number uniqueness, date ordering, non-negative financial amounts and optimistic `RECORD_VERSION` updates.
- Extended Customer 360 with a dedicated «مالی و شغلی» tab for financial profiles, income sources, employment/licenses and asset/liability records.
- No database migration is included; this release targets the supplied current CIF schema definition in `CIF-tables3.xlsx`.

## 0.3.13-prototype

- Added Party operational-form Phase 2 at `/cif/parties/{partyId}/onboarding/contact-address` and redirected successful Phase 1 onboarding directly into this step.
- Synchronized `ADDRESS`, `PARTY_ADDRESS` and `CONTACT_POINT` application models with the supplied `CIF-tables3.xlsx`, including county/structured address details, address verification/source fields, telephone dialing fields, contact owner and verification metadata.
- Added operational support for `CONTACT_POINT_ADDRESS` so a contact point can be explicitly associated with a Party address.
- Added create/edit/delete operations in the Phase 2 UI for addresses, contact points and contact-address associations with optimistic `RECORD_VERSION` updates.
- Added cascading GEO lookups for province -> county -> district -> city using the existing reference-data API; no duplicate geography reference catalog was introduced.
- Added reference lookups for address/contact type, contact purpose, contact-address association type and verification status/method.
- Preserved free optional code entry for `PARTY_ADDRESS.TENURE_TYPE_CODE`, `PARTY_ADDRESS.SOURCE_CODE` and `CONTACT_POINT.OWNER_TYPE_CODE` because the supplied model does not define explicit independent REF/FK mappings for those columns.
- Made address/contact deletion association-safe by removing dependent `CONTACT_POINT_ADDRESS` records inside the same transaction.
- Corrected primary-record semantics so `IS_PRIMARY` is exclusive within each address/contact type, not globally across the whole Party.
- Added a direct «ادامه فرم عملیاتی» action from Customer 360 back to Phase 2.
- No database migration is included; this release targets the supplied current CIF schema definition in `CIF-tables3.xlsx`.

## 0.3.12-prototype

- Synchronized the operational CIF base models with `CIF-tables3.xlsx` for `PARTY`, `PERSON`, `ORGANIZATION`, `PARTY_NAME` and `PARTY_IDENTIFIER`.
- Added `PERSON.NATIONALITY_COUNTRY_CODE` end-to-end in Oracle repository, domain/API models and Customer 360 UI.
- Added the latest `ORGANIZATION` fields end-to-end: `REGISTRATION_COUNTRY_CODE`, `ACTIVITY_STATUS_CODE`, `MAIN_ACTIVITY_DESCRIPTION`, `EMPLOYEE_COUNT`, `ENTERPRISE_SIZE_CODE` and `OWNERSHIP_TYPE_CODE`.
- Extended Party creation with `STATUS_REASON_CODE`, `VALID_FROM` and `VALID_TO` and replaced free-text creation source with `CIF.REF_SOURCE_SYSTEM` where applicable.
- Added the first dedicated operational Party onboarding form at `/cif/parties/new`, separated from the legacy list modal and aligned with the supplied Party operational-form flow.
- Added atomic onboarding API `POST /api/v1/cif/parties/onboarding` covering `PARTY + PARTY_NAME + PERSON/ORGANIZATION + PARTY_IDENTIFIER` in one transaction.
- Added a dedicated «ایجاد Party جدید» navigation item and clarified Party search semantics independently from the banking-customer role.
- Preserved database-supported lifecycle reference codes; no invented `DRAFT` code is persisted because the current `CIF.REF_PARTY_LIFECYCLE_STATUS` catalog does not define it.
- No database/DDL changes; application code is aligned to the supplied current schema definition.

## 0.3.11-prototype

- Reworked the main navigation around business domains instead of listing reference-data domains directly in the sidebar.
- Replaced the three Public / CIF Party / Deposit reference-data sidebar entries with a single «اطلاعات پایه» entry and a dedicated domain-selection hub.
- Moved «درخت جغرافیایی» under «اطلاعات پایه عمومی → اطلاعات جغرافیایی» while preserving its existing route and geography-level management links.
- Removed the «دامنه‌های برنامه‌ریزی‌شده / ادیان» area from the main dashboard.
- Replaced the three reference-domain cards on the dashboard with one compact «اطلاعات پایه» quick-access card.
- Kept Public, CIF/Party and Deposit reference forms physically and functionally separated behind the new reference-data hub.
- No database/DDL changes.

## 0.3.10-prototype

- Removed the detailed public/general reference-data section from the main dashboard.
- Removed geography and other public reference statistic cards from the dashboard; these forms remain available from the dedicated «اطلاعات پایه عمومی» menu.
- Removed the no-longer-needed `/api/v1/dashboard/counts` request from the dashboard component.
- Kept the three independent reference-domain entry cards for Public, CIF/Party and Deposit reference data.

## 0.3.9-prototype

- Separated public reference data from CIF/Party reference data in the main navigation.
- Moved all DPS deposit-product reference forms out of the global sidebar/dashboard list into a dedicated Deposit Reference Data menu.
- Moved all CIF/Party reference forms out of the global sidebar into a dedicated Party Reference Data menu grouped by package.
- Added dedicated reference menu pages with search, form counts and domain-specific routing for Public, Party and Deposit reference data.
- Dashboard now lists only public reference forms in detail; Party and Deposit reference forms are represented by separate domain entry cards.
- Preserved the 0.3.8 database-table labels on all forms.

## 0.3.8-prototype

- Added a visible database-table context to every active data-entry/search form.
- Generic GEO/DPS reference forms now show the exact descriptor-backed `SCHEMA.TABLE` name.
- CIF Party reference forms now show their exact `CIF.REF_*` table name.
- CIF Party list/create and Customer 360 forms now show the operational table or tables actually read/written by each form, including the two-table address form (`CIF.ADDRESS` + `CIF.PARTY_ADDRESS`).
- Added a reusable `DatabaseTablesComponent` so future forms can expose their physical table mapping consistently.

## 0.3.7-prototype

- Completed the Party/Customer reference catalog with Phase 6 Analytics and Recommendation.
- Added 7 CIF reference tables and 24 reviewed seed rows for metrics, metric units, analytics models, recommendations and score metadata.
- Reused `DPS.REF_CUSTOMER_SEGMENT_CODE` instead of creating duplicate `CIF.REF_CUSTOMER_SEGMENT`.
- Original Party Reference source catalog exposes 96 CIF-owned forms; all 104 source definitions are resolved with 8 GEO/DPS mappings and no deferred items. Runtime catalog is 97 after the local `REF_TENURE_TYPE` extension.
- Added completion mapping/documentation for the entire Party/Customer reference catalog.

## 0.3.6-prototype

- Activated all 17 `Workflow and Interaction` CIF reference tables (71 seed rows).
- Added reviewed Persian form titles, primary-key labels and normalized Persian seed captions for the new forms.
- Preserved the explicit `Journey -> Stage -> Event Type` reference hierarchy with Oracle foreign keys and UI lookups.
- Customer 360 KYC status now uses `CIF.REF_WORKFLOW_STATUS` instead of free text.
- Party Reference catalog now exposes 89 active CIF forms; 7 source references remain mapped to GEO/DPS, leaving 8 Analytics/Recommendation source tables for the next phase.

## 0.3.5-prototype

- Activated 11 new `Organization and Product` CIF reference tables (44 seed rows).
- Reused `GEO.CURRENCIES` and `DPS.REF_ORG_UNIT_CODE` instead of creating duplicate CIF sources.
- Added reviewed Persian titles/labels and normalized broken Persian seed captions in the new forms.
- Customer 360 ORGANIZATION now uses lookups for legal form, economic sector and ISIC activity.
- Organization creation now selects legal form from `CIF.REF_LEGAL_FORM` and no longer falls back to the invented code `OTHER`.
- Party Reference catalog now exposes 72 active CIF forms; 7 source references are mapped to existing GEO/DPS data, leaving 25 deferred source tables.

## 0.3.4-prototype

- Activated 8 Contact reference tables (33 seed rows).
- Kept existing GEO country/province/city/district/language as the single geography source of truth.
- Customer 360 now uses lookups for address type, contact type, contact purpose and country.
- Party Reference catalog now exposes 61 active forms.

# Changelog

## 0.3.71 — GEO: واژه‌نامه رومن‌نویسی نام و پیشوند/پسوند

- دو جدول `GEO.NAME_ROMANIZATION_DICTIONARY` و `GEO.NAME_AFFIX_DICTIONARY` به موتور Generic Reference CRUD متصل شدند و فرم‌های آن‌ها در «اطلاعات پایه عمومی → اطلاعات عمومی» نمایش داده می‌شود.
- فرم واژه‌نامه اصلی از جستجو، صفحه‌بندی، مرتب‌سازی، ایجاد/ویرایش/حذف، وضعیت فعال، Governance Status، Romanization Method، Confidence Score و کنترل `AUTO_FILL_ALLOWED` پشتیبانی می‌کند.
- فرم Affix برای مدیریت `سید/سیده/سادات/آقا/خانم/خانوم/میرزا/...` با Position، Context Sensitivity، Priority و `AUTO_APPLY_ALLOWED` اضافه شد.
- نوع جدید `STRING_SELECT` به موتور عمومی اطلاعات پایه اضافه شد تا Codeهای رشته‌ای در فرم به‌صورت Combo کنترل‌شده نمایش داده شوند.
- Normalization نام فارسی در UI و Backend اعمال می‌شود (`ي/ى→ی`، `ك→ک`، نیم‌فاصله و فاصله‌های زائد).
- قواعد حاکمیتی Backend مانع فعال‌شدن Auto-fill بدون Canonical English و Governance معتبر، و مانع Auto-apply برای Affixهای Context-sensitive می‌شوند.
- DDL کامل Name Romanization به Installer استاندارد GEO اضافه شد و Verifier استاتیک `verify-geo-name-romanization.mjs` قرارداد UI/Backend/DDL را کنترل می‌کند.


## 0.3.3-prototype

- Activated all 21 `Compliance and Risk` Party/Customer reference tables (87 seed rows).
- Added reviewed Persian form titles and primary-key labels for the new reference forms.
- Normalized verification status `NOT_VERIFIED` to operational CIF code `UNVERIFIED`.
- Replaced free-text KYC/risk/screening/verification fields in Customer 360 with reference-data lookups where a source table exists.
- Party Reference catalog now exposes 53 active forms.

## Unreleased
- Fixed CIF Party onboarding after enabling reference foreign keys: normalized `PARTY_NAME.SCRIPT_CODE` to `ARAB`/`LATN`, normalized reference-backed name/source codes before persistence, validated primary-name reference codes before insert, and changed Party creation source lookup from `REF_SOURCE_SYSTEM` to the FK-backed `REF_DATA_SOURCE`.
- Added a specific `ORA-02291` response (`REFERENCE_VALUE_NOT_FOUND`) instead of the generic database-constraint message.
- Build fix for Spring Boot 4.1 / Jackson 3: migrated Party reference metadata loading from `com.fasterxml.jackson.databind.ObjectMapper` to `tools.jackson.databind.json.JsonMapper`; retained Jackson annotations and corrected Angular lookup typing to remove NG8102.

## 0.3.2-prototype
- Added CIF Party/Customer Reference Data Phase 1 generated from the supplied interactive reference model.
- Added 32 code-keyed reference forms: all 31 `Identity and Party` tables plus `REF_LEGAL_CAPACITY`.
- Added a new generic CIF reference engine supporting textual primary keys and the composite key of `REF_CLASSIFICATION_VALUE` without introducing surrogate IDs.
- Added Oracle DDL and 123 seed rows for the enabled phase.
- Connected Customer 360 PERSON and selected PARTY/name/identifier/document fields to the new CIF reference lookups while keeping country/language on existing GEO sources.
- Deferred geography/currency/language duplicates and verification-status normalization pending explicit mapping.


## 0.3.1-prototype

- تکمیل فرم PERSON در Customer 360 با فیلدهای تاریخ فوت و توانایی جسمانی.
- تبدیل کشور محل تولد و زبان اصلی به Lookup واقعی از `GEO.COUNTRIES` و `GEO.LANGUAGES`.
- تبدیل جنسیت و وضعیت اقامت به Lookup از جداول مرجع موجود `DPS.REF_GENDER_CODE` و `DPS.REF_RESIDENCY_STATUS_CODE`.
- عدم اختراع کدهای مرجع برای وضعیت تأهل، اهلیت قانونی و وضعیت حیات؛ این فیلدها تا دریافت DDL/Data مرجع مستقل، کد فعلی را حفظ می‌کنند.
- نمایش پیام واقعی ProblemDetail سمت Backend در عملیات CIF به‌جای پیام عمومی ثابت.
- تشخیص اختصاصی `ORA-01950` و بازگرداندن خطای قابل فهم برای کمبود Quota در Oracle.
- بدون تغییر در DDL جداول CIF و بدون نیاز به Migration پایگاه داده.

## 0.3.0-prototype

- تغییر عنوان رابط کاربری به «سامانه دموی بانکداری متمرکز».
- افزودن Schema `CIF` به تنظیمات Runtime با همان مشخصات Oracle محیط تست.
- افزودن ماژول «مدیریت مشتری / CIF» با فهرست Party و صفحه Customer 360.
- فعال‌سازی فاز اول CIF برای ۱۲ جدول: PARTY، PERSON، ORGANIZATION، PARTY_NAME، PARTY_IDENTIFIER، ADDRESS، PARTY_ADDRESS، CONTACT_POINT، KYC_CASE، PARTY_DOCUMENT، PARTY_RISK_ASSESSMENT و SCREENING_RESULT.
- افزودن CRUD تایپ‌شده Spring/JdbcClient برای مشخصات پایه، نام و شناسه، نشانی و تماس، KYC و مدرک، ریسک و غربالگری.
- افزودن Optimistic Lock بر پایه `RECORD_VERSION` در عملیات ویرایش CIF.
- افزودن آمار CIF به Dashboard و لینک مستقیم به فهرست مشتریان.
- جلوگیری از شکست کامل آمار اطلاعات پایه در صورت خطای یک جدول؛ شمارش جداول سالم ادامه پیدا می‌کند و خطای جدول ناموفق در Log ثبت می‌شود.
- نگهداری Snapshot واقعی `CIF-050517.sql` در `database/oracle/cif/ddl`.
- اصلاح بسته سورس 0.3.0 و بازگرداندن کامل `frontend/src/app` شامل Route، منوی CIF و صفحات Customer 360.

- انتقال قاره‌ها، کشورها و شهرهای خارجی به گروه «اطلاعات جغرافیایی» در منو و Dashboard.
- نمایش ستون «نام انگلیسی» به‌جای «نسخه جاری» در Gridهای اطلاعات پایه محصول سپرده.
- حذف خروجی‌های زمان‌دار `database/oracle/exports` از Source و افزودن آن به `.gitignore`.
- ثبت مستقیم مشخصات اتصال Oracle محیط تست در هر دو فایل `application.yml` برای اجرای بدون تنظیم CMD.
- جداسازی Indexهای آینده `DEPOSIT_PRODUCT*` از DDL فعال جدول‌های `REF_*` و انتقال آن‌ها به `database/oracle/dps/pending`.
- افزودن Comment ستون `CREATED_BY` برای همه ۵۰ جدول مرجع DPS.
- افزودن `frontend/public/.gitkeep` برای تطابق ساختار Repository با تنظیمات Angular.

- افزودن ابزار استخراج DDL و داده Oracle از طریق `bin\export-database.cmd`.
- دریافت تأیید کاربر پیش از اتصال و شروع عملیات.
- استخراج Sequence، Table، Index، PK/UK/CHECK، FK، Comment، Trigger و Object Grant برای هر جدول.
- تولید داده هر جدول در قالب `INSERT INTO ... VALUES ...` با خروجی UTF-8 و Manifest اجرا.

## در حال توسعه

- فعال‌سازی ۵۰ فرم `DPS.REF_*` زیر منوی «اطلاعات پایه محصول سپرده».
- افزودن Descriptor مشترک برای ساختار کد، عنوان فارسی و انگلیسی، وضعیت، بازه اعتبار و نسخه‌بندی جداول مرجع DPS.
- پشتیبانی فرم عمومی از فیلدهای `DATE` و توضیحات چندخطی.
- ثبت `CREATED_BY` متناسب با نوع `VARCHAR2(100)` و جلوگیری از تغییر آن در Update.
- افزودن کنترل هم‌زمانی Optimistic بر اساس `RECORD_VERSION` برای جداول دارای این ستون.
- نگهداری اسکریپت‌های Oracle دریافت‌شده در `database/oracle/dps/ddl`.
- اصلاح `bin/start.cmd` برای بازماندن پنجره پس از خاتمه یا خطای Java.

## 0.2.0-prototype

- تغییر هویت پروژه از Reference Data Prototype به `core-banking-prototype`.
- تغییر Maven Artifact، نام JAR، نام پروژه Angular و عنوان رابط کاربری.
- تغییر Root Package به `com.behsazan.corebanking`.
- انتقال کد موجود اطلاعات پایه به ماژول منطقی `referencedata` بدون تغییر رفتار اجرایی.
- انتقال اجزای مشترک فعلی به `com.behsazan.corebanking.shared`.
- یکپارچه‌سازی Property مالک جداول اطلاعات پایه در `core-banking.schemas.reference-data`.
- تعریف Schema محصول‌ساز سپرده با نام `DPS` در تنظیمات، بدون ایجاد کلاس یا قابلیت فرضی.
- بازآرایی اسکریپت‌های Oracle در `database/oracle/geo/{ddl,data}`.
- افزودن ساختار `database/oracle/dps/{ddl,data}` برای دریافت اسکریپت‌های واقعی پایگاه داده.
- افزودن Data Scriptهای موجود GEO و اصلاح مستند Owner دو Export مشاغل از CIF به GEO.
- تغییر مسیر UI فرم‌های اطلاعات پایه به `/#/reference-data/{resource}` با حفظ Contract فعلی REST.
- به‌روزرسانی صفحه مشخصات فنی، مستند معماری و راهنمای فاز Deposit Product Factory.

## 0.1.6.2-prototype

- یکپارچه‌سازی فونت تمام صفحات با پشته محلی و قابل اتکای `Tahoma`, `Segoe UI`, `Arial` و حذف وابستگی متن به Vazirmatn آنلاین.
- حفظ فونت اختصاصی Material Symbols برای آیکون‌ها و فونت Monospace برای کدها و شماره نسخه‌ها.
- تغییر عنوان «کاربر نمونه» به «کاربر مدیر».

## 0.1.6-prototype

- غیرفعال‌سازی Font Inlining در Build تولید Angular برای جلوگیری از خطای Build در محیط‌های بدون دسترسی به `fonts.googleapis.com`
- یکسان‌سازی Schema تمام جداول فعال روی `GEO`
- اصلاح Schema پیش‌فرض دامنه اشتغال از مقدار قبلی به `GEO`
- حذف راهنمای تنظیم Schemaهای جایگزین از مستندات Runtime
- اصلاح و تجمیع اسکریپت‌های DDL عمومی، آموزشی، مشاغل و شهرهای خارجی با مالکیت `GEO`
- افزودن صفحه «مشخصات فنی سیستم» شامل معماری، فناوری‌ها، ابزارهای Build و قابلیت‌های نسخه جاری
- افزودن لینک صفحه مشخصات فنی در داشبورد و منوی اصلی
- افزودن Theme روشن، تیره و هماهنگ با سیستم
- نگهداری انتخاب Theme کاربر در Local Storage مرورگر
- بازطراحی رنگ‌های عمومی صفحات و فرم‌ها بر پایه CSS Variable برای پشتیبانی کامل از Theme
- افزودن راهنمای نگهداری و به‌روزرسانی صفحه مشخصات فنی در هر Release
- افزودن همگام‌سازی خودکار شماره Release و نسخه فناوری‌ها از `VERSION`، `pom.xml` و `package.json`

## 0.1.5-prototype

- فعال‌سازی ۱۴ فرم جدید و افزایش فرم‌های فعال از ۶ به ۲۰ مورد
- افزودن فرم‌های قاره، زبان، ارز، کشور، گروه خونی، بانک و شهر خارجی
- افزودن فرم‌های گروه شغلی و شغل
- افزودن فرم‌های گروه، زیرگروه، مقطع، رشته و دانشگاه
- افزودن Combo والد و فیلتر والد برای همه روابط سلسله‌مراتبی جدید
- افزودن Lookupهای مستقل برای روابط چندگانه کشور و دانشگاه
- پشتیبانی Repository از نام متفاوت ستون‌های Audit در جداول مشاغل
- قابل تنظیم شدن Schemaهای عمومی، آموزشی و اشتغال از طریق `application.yml`
- حذف وضعیت Planned از جداولی که اکنون فعال شده‌اند

## 0.1.4-prototype

- جایگزینی عنوان عمومی «والد» در Grid با عنوان فارسی والد تعریف‌شده در Descriptor؛ مانند «استان» در Grid شهرستان‌ها
- افزودن Combo فیلتر والد به همه فرم‌های سلسله‌مراتبی؛ مانند فیلتر استان در فهرست شهرستان‌ها
- اعمال فیلتر والد به Query سمت سرور و بازگشت خودکار به صفحه اول
- افزایش سقف Lookup از ۵۰۰ به ۵۰۰۰ گزینه برای پوشش کامل سطوح جغرافیایی فعلی

## 0.1.3-prototype

- تثبیت مستقیم تنظیمات Oracle در هر دو فایل `application.yml`
- استفاده از Service Name برابر `FREEPDB1`
- تنظیم کاربر Oracle روی `SYSTEM` و رمز محلی تعیین‌شده برای نمونه
- حذف وابستگی اتصال Oracle به متغیرهای محیطی `ORACLE_URL`، `ORACLE_USERNAME` و `ORACLE_PASSWORD`

## 0.1.2-prototype

- اصلاح نمایش آیکون‌های Angular Material با Material Symbols Rounded در محیط RTL
- جلوگیری از نمایش متن شکسته آیکون‌ها مانند `age`، `city` و `ree`
- نمایش پیام روشن در داشبورد هنگام خطای Backend یا اتصال Oracle
- اصلاح آدرس پیش‌فرض Oracle به قالب Service Name: `@//localhost:1521/FREEPDB1`
- حذف رمز آزمایشی `change-me` و استفاده از مقدار خالی/متغیر محیطی `ORACLE_PASSWORD`
- اصلاح `start.cmd` و `start.sh` برای یافتن JAR در پوشه `app` یا `backend/target`
- هم‌راستاسازی Build با Startup از طریق کپی JAR نهایی در پوشه `app`

## 0.1.1-prototype

- اصلاح فرم پویا در Angular با جایگزینی `FormGroup<Record<...>>` با `FormRecord<FormControl<unknown>>`
- رفع خطای TypeScript `TS2769` در `removeControl` برای کلیدهای Runtime

## 0.1.0-prototype

- پروژه مستقل Java/Angular بدون وابستگی به SchemaForge
- Runtime عمومی Descriptor-driven برای اطلاعات پایه
- CRUD کامل شش سطح جغرافیایی Oracle
- Grid با جست‌وجو، مرتب‌سازی و صفحه‌بندی سمت سرور
- فرم سلسله‌مراتبی با Lookupهای وابسته
- درخت جغرافیایی Lazy-load
- Audit صحیح: عدم مقداردهی `LAST_MODIFIED_*` در Insert
- Catalog توسعه آینده برای کشور، ارز، زبان، مشاغل و اطلاعات تحصیلی
- پاسخ خطای استاندارد ProblemDetail
- Build نهایی Angular داخل Executable JAR

## 0.1.6.1-prototype

- نمایش صریح دکمه «تم» در نوار بالا، به‌جای اتکا به آیکون تنها.
- اضافه‌شدن لینک دوم «مشخصات فنی» در نوار بالا، علاوه بر منوی اصلی و داشبورد.
- نمایش شماره نسخه در نوار بالا برای تشخیص سریع Build در حال اجرا.
- حذف JAR قدیمی در ابتدای Build تا Build ناموفق با نسخه قبلی اشتباه نشود.
- کنترل اشغال‌بودن پورت 8091 پیش از اجرا و نمایش PID نسخه قبلی.
- اضافه‌شدن `bin/stop.cmd` برای توقف کنترل‌شده سرویس روی پورت 8091.


## 0.3.55-prototype-fee-p1 — FIX66 Oracle metadata-driven schema discovery

- فرم «مقایسه مدل Enterprise Architect با Oracle» دیگر لیست Schema را از مجموعه Hard-code/Configuration دریافت نمی‌کند.
- Source of Truth فهرست Schemaها اکنون Oracle Data Dictionary و `ALL_TABLES` است؛ فقط Ownerهایی که حداقل یک جدول برای کاربر اتصال قابل مشاهده دارند در UI نمایش داده می‌شوند.
- تعداد جدول‌های قابل مشاهده هر Schema در عنوان گزینه نمایش داده می‌شود.
- Configurationهای `core-banking.schemas.*` فقط برای Label دوستانه و ترجیح Schema پیش‌فرض باقی مانده‌اند و دیگر تعیین‌کننده فهرست انتخابی نیستند.
- همین قرارداد برای فرم «استخراج Oracle به EA XMI» نیز یکسان شد تا دو ابزار مدیریت مدل رفتار متفاوت نداشته باشند.
