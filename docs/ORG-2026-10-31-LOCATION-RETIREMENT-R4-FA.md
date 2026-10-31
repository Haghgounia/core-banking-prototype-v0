# ORG R4 — قطع وابستگی اجرایی به LOCATIONS و POSTAL_ADDRESSES

**مبنای توسعه:** سورس کامل `core-banking-prototype-v0-2026-10-31-1151.zip` به‌علاوه R3 نصب‌شده و تأییدشده در خروجی کاربر (Oracle R3: `ORG_CANONICAL_LOCATION_DETACH_PASS`).

## ۱. تصمیم مرجع دامنه

- `ORG.ORGANIZATION_UNIT_LOCATIONS` تنها مرجع عملیاتی نشانی و موقعیت **واحد سازمانی** است؛ رکورد مستقل استقرار/دوره و شناسه `ORGANIZATION_UNIT_LOCATION_ID` حفظ می‌شود.
- `ORG.LOCATIONS` و `ORG.POSTAL_ADDRESSES` منابع تاریخی هستند و **فقط پس از تکمیل R4-A، موفقیت UAT و تأیید DBA و مصرف‌کنندگان بیرونی** به `DEPRECATED_LOCATIONS` و `DEPRECATED_POSTAL_ADDRESSES` تغییر نام خواهند یافت. اسکریپت‌ها این جداول و داده‌ها را حذف نمی‌کنند.
- `ORG.SERVICE_POINTS` و نوع آن فعال می‌مانند. برای نقطه خدمتی که واقعاً در یک استقرار مشخص واحد سازمانی قرار دارد، محل از طریق `ORG.SERVICE_POINTS.ORGANIZATION_UNIT_LOCATION_ID` به مرجع Canonical وصل می‌شود؛ شناسه قدیمی `LOCATION_ID` از عملیات جدید حذف و به ستون تاریخی NULLABLE تبدیل می‌شود (و CHECK مانع نوشتن شناسه Legacy می‌شود).
- **نقاط خدمت مستقل** (مانند ATM خارج از مکان یک واحد) نباید به‌صورت خودکار یا صوری به یک استقرار واحد متصل شوند. این انتشار در R4-A صرفاً «نقاط خدمت دارای استقرار واحد میزبان تأییدشده» را پوشش می‌دهد. برای مدل مستقل، تصمیم دامنه و طراحی Business Capability جداگانه لازم است؛ تا آن زمان ثبت نقطه خدمت بدون محل Canonical مجاز نیست.
- استقرارهای مشترک `LOCATION_ID=1327` همچنان چهار رکورد معتبر مستقل‌اند. هیچ انتقال، تکرارسازی یا طبقه‌بندی خودکار رخ نمی‌دهد. نشانی رسمی بانک معتبرتر از GEO است و کدپستی اختیاری می‌ماند.
- این بسته **حوزه تماس** (`CONTACT_POINTS`, `CONTACT_POINTS_V2` و انتساب‌ها) را تغییر نمی‌دهد؛ ۸۶ انتساب منتقل‌نشده مربوط به R5 است.

## ۲. شواهد واقعی مبنای تصمیم (و نه نتیجه اجرای بسته)

آخرین گزارش کاربر:

| شاهد | مقدار |
|---|---:|
| ORGANIZATION_UNIT_LOCATIONS | 1340 |
| LOCATIONS | 1337 |
| POSTAL_ADDRESSES | 1337 |
| SERVICE_POINTS | 0 |
| POSTAL_NOT_LINKED_BY_CANONICAL_ID | 0 |
| LOCATION_WITHOUT_ORG_REFERENCES | 0 |
| UNKNOWN SOURCE ID | 0 |
| FK فعال SERVICE_POINTS به LOCATIONS | 1 |
| FK فعال POSTAL_ADDRESSES به LOCATIONS | 1 |
| FK فعال ORGANIZATION_UNIT_LOCATIONS به LOCATIONS | **0 پس از R3** |

این شمارش‌ها از گزارش کاربر هستند؛ اسکریپت R4-A در پایگاه داده **دوباره** می‌سنجد. کنترل برابری محتوایی ۲۰ فیلد در R4-B جداگانه انجام می‌شود.

## ۳. اصلاحات Source

1. `OrganizationDescriptorProvider`: حذف دو Descriptor قدیمی از registry. مرجع `organization-unit-locations` فقط برای Lookup به جدول Canonical می‌رود؛ عنوان Lookup از `ADDRESS_FA` و شناسه اصلی از `ORGANIZATION_UNIT_LOCATION_ID` گرفته می‌شود. فرم «نقاط خدمت» به‌جای `locationId` مقدار `organizationUnitLocationId` را الزامی می‌کند.
2. `app.routes.ts`: مسیرهای عمومی قدیمی `locations`, `postal-addresses`, `organization-unit-locations` و مسیر ارزیابی قدیمی به صفحه مستقل Canonical هدایت می‌شوند؛ صفحه کاربر از داده‌های Legacy خواندن ندارد.
3. دو Controller قدیمی بررسی/مقایسهٔ تاریخی با `legacy-review-enabled=false` به‌طور پیش‌فرض از Spring حذف می‌شوند. فایل‌های سرویس تاریخی به‌عنوان **کد مرجع برای ممیزی با opt-in** باقی می‌مانند و پس از Rename فعال‌سازی مجدد آن‌ها بدون بازنگری SQL مجاز نیست.
4. `GET /api/v1/organization/service-point-location-readiness`: API جدید فقط‌خواندنی برای سنجش ستون و FK Canonical و بستن مسیر نوشتن قدیمی؛ پس از Rename نیز SQL آن به جدول‌های قدیمی وابسته نیست.
5. `verify-org-legacy-location-r4.mjs` و آزمون جدید Java برای تثبیت رجیستری، مسیر نقاط خدمت، حذف Descriptorهای قدیمی و گیت‌های Oracle. Verifierهای تاریخی که شمار ۲۷ Descriptor را الزام می‌کردند، **به‌صورت آگاهانه به ۲۵ Descriptor** تطبیق داده شده‌اند؛ پوشش CAL2 و R1/R2 حفظ شده است.

## ۴. اجرای مرحله‌ای و ایمن

**مرحله صفر — Backup و Stop:** ابتدا از سورس/دیتابیس نسخه پشتیبان تهیه کنید؛ سرویس را متوقف کنید. نصب‌کننده ZIP تنها سورس را اعمال و Build انجام می‌دهد و به Oracle وصل نمی‌شود.

**مرحله A — Oracle (فقط نقاط خدمت):** در SQL Developer فایل زیر را با F5 باز کنید:

`database/oracle/org/location-retirement-r4/01-ORG-R4-SERVICE-POINT-CANONICAL-CUTOVER-OPT-IN.sql`

پیش‌فرض: `DEFINE APPLY_CHANGES = NO`؛ خروجی `ORG_R4A_SERVICE_POINT_PREVIEW_COMPLETE`. روی دیتابیس واقعی `BLOCKERS=0` و `POINTS=0` را بررسی کنید. پس از تأیید DBA و پنجره نگهداری، فقط همین فایل را با `APPLY_CHANGES=YES` اجرا کنید. خروجی لازم: `ORG_R4A_SERVICE_POINT_CANONICAL_PASS`. این عملیات شامل DDL است، **Rollback تراکنشی ندارد** و داده‌ها را DML نمی‌کند.

بلافاصله SQL فقط‌خواندنی دوم را اجرا کنید:

`02-ORG-R4-SERVICE-POINT-VERIFY-READONLY.sql`

همه کنترل‌های قرارداد باید `ACTUAL=EXPECTED` باشند، FK نقطه خدمت به Canonical باید `ENABLED/VALIDATED` و FK آن به `LOCATIONS` باید صفر باشد. بعد از راه‌اندازی Backend، `GET /api/v1/organization/service-point-location-readiness` باید `ready=true` باشد.

**مرحله B — UAT در محیط پایدار:** فرم `#/organization/unit-locations-canonical` را بررسی کنید؛ رکوردهای ۱۳۴۰ و اشتراک `1327` را بدون تغییر حفظ کنید. در `#/organization/points-of-service` نمایش، جست‌وجو و انتخاب **شناسه یک استقرار واقعی و تأییدشده** را بیازمایید؛ ایجاد/ویرایش/خواندن نقطه خدمت باید فقط از طریق `ORGANIZATION_UNIT_LOCATION_ID` انجام شود و بدون آن در Backend رد شود. در صورت نیاز برای آزمایش از رکورد آزمایشی کنترل‌شده و قابل حذف استفاده کنید؛ این بسته هیچ رکورد تستی تولید نمی‌کند. ورود به مسیرهای قدیمی باید به صفحه اصلی Canonical هدایت شود. دو API قدیمی `unit-location-review` و `unit-location-canonical-review` باید به‌طور پیش‌فرض وجود نداشته باشند.

**مرحله C — Preview مستقل برای Rename:** پس از UAT، فایل زیر را با F5 اجرا کنید:

`03-ORG-R4-LEGACY-RENAME-PREVIEW-OPT-IN.sql`

پیش‌فرض هر سه پرچم `NO` است. کنترل‌ها شامل پوشش کامل ردیف‌ها، ۲۰ فیلد تطبیق محتوایی در ردیف‌های قدیمی و ویرایش‌نشده، نبود FK ورودی ناشناخته، Stored Object، Synonym، Trigger و وضعیت R4-A هستند. **`BLOCKERS=0` در این مرحله مجوز خودکار Rename نیست**؛ بررسی Dynamic SQL، سامانه‌های بیرونی، Jobها و مالکیت داده همچنان الزامی است.

**مرحله D — Rename با تأیید DBA:** فقط پس از UAT و تایید مستقل مصرف‌کنندگان بیرونی، در همان فایل سوم:

```
DEFINE APPLY_CHANGES = YES
DEFINE CONFIRM_RUNTIME_CUTOVER = YES
DEFINE CONFIRM_EXTERNAL_CONSUMERS = YES
```

با F5 اجرا کنید. ترتیب Rename: ابتدا `POSTAL_ADDRESSES` سپس `LOCATIONS`؛ ارتباط FK تاریخی بین دو جدول به‌طور خودکار توسط Oracle حفظ می‌شود. هیچ `DROP`, `DML`, `CASCADE`, `PURGE` یا تغییر Sequence وجود ندارد. پس از انجام، `04-ORG-R4-LEGACY-RENAME-VERIFY-READONLY.sql` را اجرا کنید. برای بازگرداندن نام‌ها فایل `05-...-RESTORE-OPT-IN.sql` با حالت پیش‌فرض Preview وجود دارد؛ بازگردانی نام‌ها نیاز به هماهنگی deployment دارد.

## ۵. مرزهای پذیرش و ریسک

- **R4-A کامل**: Build، Verify، Readiness و آزمون UI نقاط خدمت روی Oracle واقعی موفق؛ FK/نام‌های قدیمی برای نقاط خدمت قطع شده‌اند.
- **R4-B کامل**: علاوه بر همه پیش‌نیازهای A، برابری ۲۰ فیلد (برای snapshots بدون ویرایش Canonical)، همخوانی داده، نبود وابستگی‌ها، موافقت DBA و مصرف‌کنندگان بیرونی و خروجی Rename/Verify موفق.
- **محدودیت آگاهانه**: مدل «نقطه خدمت مستقل از مکان سازمانی» در این نسخه **طراحی نشده است**؛ اتصال ساختگی ممنوع است. تا انجام طراحی مستقل، باید ثبت آن دسته از نقاط خدمت مسدود بماند.
- APIهای ممیزی تاریخی منبع با پیشوند `DEPRECATED_` خودکار بازنویسی نمی‌شوند؛ به‌طور پیش‌فرض غیرفعال و از فرایند عملیاتی جدا هستند.
- اجرای Oracle از محیط توسعه انجام نشده؛ خروجی موفق Compile/Static Check به معنی تست زنده DB یا UAT نیست.
- اسکریپت قدیمی `database/oracle/org/service-point/ORG-SERVICE-POINT-RENAME-OPT-IN.sql` مربوط به تاریخچه نام‌گذاری است؛ **نباید دوباره روی ساختار فعلی اجرا شود**.
