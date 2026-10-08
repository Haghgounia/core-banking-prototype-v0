# PDL 0.11.0 R10U — بازطراحی قاعده مشترک اهلیت

## تصمیم معماری

- **تغییر دیتابیس لازم است.** مدل تک‌مقداری قبلی برای انتخاب چند مقدار از نوع مشتری، بخش مشتری، وضعیت مشتری، جنسیت و دامنه‌های مشابه کافی نبود.
- **تغییر XML برای Runtime لازم نیست.** Product Builder از Metadata واقعی Oracle استفاده می‌کند. اگر EA/XML مدل رسمی مستندسازی است، پس از تثبیت DDL باید با مدل جدید همگام شود.

## مدل داده

جدول والد `PDL.PRODUCT_ELIGIBILITY_RULE` حفظ شده و ستون `CRITERIA_MODEL_VERSION` به آن افزوده می‌شود. جدول جدید `PDL.PRODUCT_ELIGIBILITY_CRITERION` منبع اصلی معیارهای چندمقداری/آستانه‌ای است.

قرارداد ارزیابی:
- چند مقدار از یک نوع معیار = OR / IN
- انواع مختلف معیار = AND
- نبودن رکورد برای یک معیار دامنه‌ای = «همه موارد / بدون محدودیت»
- سابقه مشتری و حساب = مقدار عددی + واحد صریح `DAY | MONTH | YEAR`

ستون‌های scalar قدیمی در والد فعلاً برای سازگاری با مصرف‌کنندگان قبلی نگه داشته می‌شوند و هنگام ذخیره، Projection سازگار تولید می‌شود.

## UX قاعده مشترک اهلیت

1. `PRODUCT_VERSION_ID` از Field Grid حذف شده و نسخه جاری فقط در Context فرم نمایش داده می‌شود.
2. نوع مشتری، بخش مشتری، وضعیت مشتری، جنسیت، وضعیت اقامت و دامنه تابعیت Multi-select چک‌باکسی هستند.
3. اولین گزینه در Dimensionها «همه موارد» است؛ انتخاب آن با انتخاب‌های مشخص متقابلاً انحصاری است.
4. جنسیت از Reference Data `dps-genders` بارگذاری می‌شود و فقط زمانی فعال است که دامنه نوع مشتری صرفاً «شخص حقیقی» باشد.
5. فیلدهای مبلغی با جداکننده هزارگان نمایش داده می‌شوند ولی مقدار Form/Backend عددی باقی می‌ماند.
6. اولویت فنی قاعده از فرم کاربر حذف شده و در صورت وجود ستون Priority، مقدار پیش‌فرض سیستمی `100` اعمال می‌شود.
7. «حداقل سابقه مشتری» و «حداقل سابقه حساب» دو معیار مستقل‌اند و واحد روز/ماه/سال صریحاً انتخاب می‌شود.
8. مقادیر Legacy سابقه که واحد مشخص ندارند، بدون حدس به واحد تبدیل نمی‌شوند و برای اصلاح کاربر علامت‌گذاری می‌شوند.

## سازگاری Runtime چهار سپرده

Opening Runtime اکنون `PRODUCT_ELIGIBILITY_CRITERION` را نیز می‌خواند. برای Customer Type، معیارهای چندمقداری مدل نسخه 2 منبع اصلی هستند؛ رکوردهای Legacy بدون مدل جدید همچنان از ستون scalar قبلی خوانده می‌شوند.

## Migration

پس از اعمال Patch و قبل از اجرای Runtime Product Builder، Migration زیر روی Oracle اجرا شود:

`database\oracle\pdl\migrations\0.11.0-r10u-eligibility-rule-business-ux.sql`

با Runner استاندارد پروژه:

```bat
set "CORE_BANKING_ORACLE_CONNECT=SYSTEM/Oracle123@//localhost:1521/FREEPDB1"
tools\run-oracle-sql.cmd database\oracle\pdl\migrations\0.11.0-r10u-eligibility-rule-business-ux.sql
```

در محیط دیگری Connection String متناسب با همان محیط تنظیم شود.

## Qualification ایستا

- R10U Eligibility: 31/31 PASS
- R10T UI Alignment: 26/26 PASS
- R10S Common Rules: 54/54 PASS
- R10R Product Version: 23/23 PASS
- R10T-HF1 Java syntax guard: 5/5 PASS
- Unified Product Builder: PASS — 51 business tables / 54 physical tables
- Release Layout: PASS

اجرای واقعی Migration Oracle و Build کامل Maven/Angular باید در محیط Windows/Oracle پروژه انجام شود. در محیط بسته‌بندی، Maven Wrapper نتوانست Maven 3.9.16 را از Maven Central دریافت کند؛ بنابراین PASS کامپایل کامل ادعا نشده است.
