# Changelog

## Unreleased

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
