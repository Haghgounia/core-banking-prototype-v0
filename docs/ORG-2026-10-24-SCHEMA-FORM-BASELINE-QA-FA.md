> **یادداشت نسخه:** این سند یک Snapshot از وضعیت ORG پیش از تصمیم حذف ساعات کاری است (۳۷ جدول/۳۵ Descriptor). برای برنامه اجرایی پس از تصمیم، به `docs/ORG-2026-10-24-CAL2-HOURS-CONSOLIDATION-QA-FA.md` مراجعه کنید. هنوز حذف فیزیکی Oracle تأیید نشده است.

# ORG — گزارش تطبیق اولیه Oracle و فرم‌ها (۲۴ اکتبر ۲۰۲۶)

**Oracle evidence:** `ORG-2026-10-24.txt`؛ **Source:** `core-banking-prototype-v0-2026-10-24-1043.zip`، نسخه `0.11.0`.
**روش:** خواندن فایل DDL/Metadata ارسالی و بررسی ایستای Source؛ بدون اتصال به Oracle، DML یا DDL.

## ۱. شمارش و انطباق

| موضوع | نتیجه |
|---|---|
| جداول `CREATE TABLE ORG.*` در فایل متادیتا | ۳۷ جدول یکتا |
| `CREATE SEQUENCE ORG.*` در فایل متادیتا | ۳۷ Sequence یکتا |
| جدول‌های فرم عمومی `OrganizationDescriptorProvider` | ۳۵ جدول؛ **همگی با نام واقعی جدول متناظرند** |
| جداول دارای UI اختصاصی خارج از Descriptor | `ORGANIZATION_UNIT_GRADE_LEVELS`, `ORGANIZATION_UNIT_GRADE_PERIODS` |
| گروه‌های منوی ORG | ۷ گروه |
| گزینه‌های عمومی قابل مشاهده در منو | ۳۱ فرم |
| فرم‌های اختصاصی درجه‌بندی | ۲ فرم (`/organization/grade-levels`, `/organization/grade-periods`) |
| چهار Descriptor محفوظ اما مخفی در منو | `ORGANIZATION_UNIT_CLASSIFICATIONS`, `ORGANIZATION_UNIT_RELATIONSHIP_RULES`, `FOREIGN_EXCHANGE_SERVICE_PROFILES`, `ORGANIZATION_UNIT_GEO_COVERAGES` |

**تطبیق نقطه خدمت:** هفت جدول با نام فیزیکی `SERVICE_POINT_*` و `ORGANIZATION_UNIT_SERVICE_POINT_ASSIGNMENTS` در Oracle موجودند. Descriptorهای برنامه همین نام‌های فیزیکی را استفاده می‌کنند؛ ولی نام `POINT_OF_SERVICE_ID`، نام Sequenceها و مسیرهای Resource مانند `points-of-service` برای سازگاری حفظ شده‌اند. تغییر نام مجدد جدول‌ها مجاز نیست.

## ۲. کنترل‌های ایستای اجراشده روی نسخه فوق

| دستور | نتیجه |
|---|---|
| `node tools/verify-org-forms.mjs` | `ORG_FORMS_VERIFY_PASS` |
| `node tools/verify-org-grade-p3-p4.mjs` | `ORG_GRADE_PROTOTYPE_VERIFY_PASS` |
| `node tools/verify-org-location-form.mjs` | `ORG_LOCATION_FORM_VERIFY_PASS` |
| `node tools/verify-org-ux-r2.mjs` | `ORG_UX_R2_VERIFY_PASS` |
| `node tools/verify-release-layout.mjs` | `Release layout verification OK` |

این بررسی‌ها **نه Build کامل Maven/Angular** و **نه اثبات CRUD واقعی همه جداول** هستند.

## ۳. نکات باز / ریسک‌های قابل بررسی

1. **مرجع‌های خارجی:** سه مقدار `EMPLOYEE_ID`، `CONTACT_POINT_ID` و `GEO_ENTITY_ID` در ORG به Master داخل همین Schema وصل نیستند؛ فیلدهای کارمند و راه تماس در UI عمدتاً شناسه عددی می‌گیرند. مالک مرجع و Lookup معتبر باید مشخص شود. هر FK بین Schemaها فقط در صورت تصمیم معماری ایجاد می‌شود.
2. **ساعت‌های ORG:** در `OPERATING_SCHEDULE_INTERVALS` و دو جدول بازه استثنا، `START_TIME` و `END_TIME` از نوع `VARCHAR2(5 CHAR)` هستند و در Descriptor با `text(...)` آمده‌اند؛ در DDL ارسالی برای این دو ستون CHECK قالب/ترتیب زمان دیده نشد. قبل از گشودن UAT باید صحت `HH:mm`، بازه‌ها و تداخل شیفت‌ها در API/فرم را ثابت کنیم. نباید از موفقیت فرم ساعت‌گزین CAL2، سلامت خودکار فرم ORG را نتیجه گرفت.
3. **مرزبندی CAL2 و ORG:** ORG الگوی `OPERATING_SCHEDULES` و استثنای سطح واحد/نقطه خدمت دارد؛ CAL2 نیز الگوی بازه‌ای تقویم کاری و روز Resolveشده دارد. مرجع روز تعطیل، ساعت کارکنان، ساعت خدمت‌رسانی، و اولویت استثناها باید صریح تعیین شود؛ فعلاً تغییر یا ادغام جدول انجام نمی‌شود.
4. **اعتبار زمانی و رابطه:** DDL برای بسیاری از پیوندها فقط شرط `EFFECTIVE_TO >= EFFECTIVE_FROM` دارد. جلوگیری از دوره‌های هم‌پوشان، روابط سلسله‌مراتبی دوری و چند تخصیص اصلی هم‌زمان باید در سرویس یا قواعد داده اثبات شود؛ وجود CHECK ساده کافی نیست.
5. **درجه‌بندی:** دو جدول درجه، شناسه `NUMBER(19)` دارند؛ وضعیت `ACTIVE` برای ثبت مستقیم Prototype در DDL مجاز است. این امر به معنی آماده‌بودن Maker/Checker و Audit هویت برای تولید بانکی نیست.
6. **منو:** چهار جدول مخفی حذف نشده‌اند؛ پیش از حذف یا فعال‌کردن مجدد باید کاربرد کسب‌وکاری آنها مشخص شود. `ORG.FOREIGN_EXCHANGE_SERVICE_PROFILES` در گروه قابل نمایش فعلی قرار ندارد.
7. **اطلاعات شعب:** مطابق تصمیم قبلی پروژه، برای صحت‌سنجی مشخصات رسمی شعب بانک ملت، منبع رسمی بانک مقدم است؛ GEO صرفاً مرجع شناسه فنی و کدپستی اختیاری است. این تصمیم با آزمون داده واقعی و تبدیل فیلدها به Lookup تداخل پیدا نکند.
8. **ثبت و ویرایش در محیط واقعی:** شمار رکوردها، رفتار قیود Oracle، Map tile شبکه و خطاهای اجرایی هنوز با فایل متادیتای DDL قابل اثبات نیستند. بدون شواهد نباید PASS عملیاتی ثبت شود.

## ۴. پیشنهاد شروع کار روی فرم‌ها

**گام نخست: `ORGANIZATION_UNITS`** و جداول مرجع `ORGANIZATIONS` و `ORGANIZATION_UNIT_TYPES` با مرور کامل قرارداد فیلدها، Label/Lookup و وضعیت‌های مجاز. سپس `ORGANIZATION_UNIT_RELATIONSHIPS` و دو فرم درجه‌بندی بررسی شوند. در بررسی ساعات کاری، ابتدا تعارض ORG/CAL2 به‌عنوان تصمیم معماری مشخص گردد و از ایجاد دو منبع موازی خودداری شود.

## ۵. معیار پذیرش هر فرم

- تطبیق ستون، نوع داده، Nullable، Default، Sequence، FK، Unique و CHECK با Oracle.
- برچسب‌های فارسی دقیق؛ فیلدهای فنی فقط خواندنی؛ انتخاب‌گرهای وابسته به مرجع واقعی و کنترل داده نامعتبر در Backend.
- رفتار درست افزودن، ویرایش، مشاهده، حذف/غیرفعال‌سازی متناسب با دامنه، فیلتر و صفحه‌بندی.
- تاریخ شمسی برای نمایش و ISO برای قرارداد API، زمان `HH:mm` در کنترل صحیح، خطاهای قابل فهم.
- جلوگیری از داده ناسازگار تاریخی، دوره‌های هم‌پوشان، ارجاع بی‌اعتبار و عملیات تکراری.
- ثبت نتیجه آزمون Oracle / مرورگر / Build پیش از تغییر وضعیت به `DONE`.

**تغییر انجام‌شده در این بسته:** صرفاً سند برنامه/QA و ارجاع آن در README؛ هیچ schema، code path، feature toggle یا داده‌ای تغییر نکرده است.
