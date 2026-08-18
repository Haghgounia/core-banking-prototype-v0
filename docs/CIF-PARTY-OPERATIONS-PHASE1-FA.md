# فاز ۱ فرم‌های عملیاتی Party — ایجاد هویت پایه

## هدف

این فاز نخستین گام انتقال فرم‌های عملیاتی Party به پروتوتایپ Core Banking است. مبنای دیتامدل، فایل `CIF-tables3.xlsx` و مبنای جریان کاربری، فرم‌های عملیاتی Party ارائه‌شده در `Party-Operation_Froms-1.html` / مدل EA متناظر است.

## دامنه این نسخه

ثبت اولیه Party در یک تراکنش واحد انجام می‌شود و چهار بخش داده‌ای را پوشش می‌دهد:

1. `CIF.PARTY` — نوع Party، چرخه عمر، دلیل وضعیت، منبع ایجاد و بازه اعتبار.
2. `CIF.PARTY_NAME` — نام قانونی/ثبتی اصلی و اجزای نام برای شخص حقیقی.
3. یکی از `CIF.PERSON` یا `CIF.ORGANIZATION` متناسب با نوع Party.
4. `CIF.PARTY_IDENTIFIER` — شناسه هویتی اصلی Party.

API جدید:

`POST /api/v1/cif/parties/onboarding`

این API با `@Transactional` اجرا می‌شود؛ در نتیجه اگر ثبت هریک از اجزای فوق شکست بخورد، کل ایجاد Party Rollback می‌شود.

## همگام‌سازی آخرین دیتامدل

### PERSON

فیلد `NATIONALITY_COUNTRY_CODE` به مدل Domain، Repository، API و UI اضافه شد.

### ORGANIZATION

فیلدهای زیر به‌صورت End-to-End اضافه شدند:

- `REGISTRATION_COUNTRY_CODE`
- `ACTIVITY_STATUS_CODE`
- `MAIN_ACTIVITY_DESCRIPTION`
- `EMPLOYEE_COUNT`
- `ENTERPRISE_SIZE_CODE`
- `OWNERSHIP_TYPE_CODE`

### PARTY

فرایند ایجاد اکنون `STATUS_REASON_CODE`، `VALID_FROM` و `VALID_TO` را نیز دریافت و ذخیره می‌کند. `PARTY_ID` و `PARTY_UID` همچنان توسط Oracle تولید می‌شوند و فیلدهای سیستمی/ممیزی از منطق دیتابیس پیروی می‌کنند.

## تصمیم درباره وضعیت DRAFT

در نمونه HTML عملیاتی، جریان کار با مفهوم «پیش‌نویس Party» نمایش داده شده است؛ اما Reference Data فعلی پروژه در `CIF.REF_PARTY_LIFECYCLE_STATUS` فقط کدهای موجود دیتابیس را ارائه می‌کند و `DRAFT` در آن تعریف نشده است. بنابراین این نسخه هیچ کد جدیدی را به‌صورت ضمنی اختراع نمی‌کند و فرم از Lookup واقعی دیتابیس استفاده می‌کند. اگر در مدل مرجع بعدی `DRAFT` تصویب شود، UI بدون Hard-code قابل استفاده از آن خواهد بود.

## ادامه پیشنهادی فرم‌های عملیاتی

ترتیب توسعه بعدی بر اساس جریان فرم عملیاتی پیوست:

1. نشانی‌ها و نقاط تماس + ارتباط Contact/Address
2. اطلاعات مالی و شغلی / فعالیت اقتصادی
3. شناسه‌های تکمیلی و مدارک
4. طبقه‌بندی Party
5. روابط و ذی‌نفعان
6. نقش‌ها و رابطه بانکی و تخصیص شماره مشتری
7. KYC، Screening و Risk
8. Consent و Preferences
9. تغییر وضعیت و Merge Party
10. تکمیل نمای ۳۶۰ درجه بر مبنای داده‌های عملیاتی فوق

## نکات معماری

- فرم «ایجاد Party» از «ایجاد Customer» تفکیک شده است؛ Customer یک Role/Relationship بانکی است و موجودیت Party مستقل باقی می‌ماند.
- Lookupهای دارای Reference Data از APIهای مرجع موجود خوانده می‌شوند و در مواردی که Reference Table مصوب در پروژه موجود نیست، مقدار کد به‌صورت آزاد ولی بدون ساخت Vocabulary جدید دریافت می‌شود.
- این نسخه هیچ DDL جدیدی ایجاد نمی‌کند و صرفاً کد برنامه را با ساختار فعلی Schema همگام می‌کند.
