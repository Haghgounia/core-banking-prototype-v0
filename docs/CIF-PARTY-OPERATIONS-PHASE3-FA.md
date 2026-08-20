# فرم‌های عملیاتی Party — فاز ۳: اطلاعات مالی، شغلی و فعالیت اقتصادی

## هدف
این فاز ادامه مستقیم فاز ۲ فرم عملیاتی Party است و نمایه مالی، منابع درآمد/وجوه، دارایی‌ها و تعهدات و اطلاعات فعالیت اقتصادی را به جریان واقعی CIF اضافه می‌کند.

## مرزبندی شخص حقیقی و حقوقی
مطابق مدل `CIF-tables3.xlsx`، جدول `CIF.PARTY_EMPLOYMENT` برای Party از نوع `PERSON` استفاده می‌شود و برای `ORGANIZATION` سابقه اشتغال ایجاد نمی‌شود.

برای شخص حقوقی، اطلاعات فعالیت اصلی روی خود `CIF.ORGANIZATION` نگهداری و مجوزهای فعالیت در `CIF.PARTY_LICENSE` ثبت می‌شوند.

## جداول عملیاتی جدید
- `CIF.FINANCIAL_PROFILE`
- `CIF.PARTY_EMPLOYMENT` — فقط شخص حقیقی
- `CIF.PARTY_INCOME_SOURCE`
- `CIF.PARTY_ASSET_LIABILITY`
- `CIF.PARTY_LICENSE`

علاوه بر این، فیلدهای فعالیت اقتصادی `CIF.ORGANIZATION` در همین فرم قابل تکمیل/ویرایش هستند.

## پروفایل مالی
`FINANCIAL_PROFILE` به‌صورت Snapshot تاریخ‌دار نگهداری می‌شود و علاوه بر فیلدهای پایه، ستون‌های آخرین مدل شامل درآمد خالص و سایر درآمد ماهانه، تعداد تراکنش مورد انتظار، کشورهای مبدأ/مقصد وجوه، هدف رابطه مالی، ارزش املاک و سرمایه‌گذاری، اقساط ماهانه، خالص ثروت و سطح توان مالی پوشش داده شده‌اند.

قید یکتایی `(PARTY_ID, AS_OF_DATE)` در سطح سرویس نیز کنترل می‌شود تا کاربر پیش از رسیدن به خطای Oracle پیام معنادار دریافت کند.

## اشتغال شخص حقیقی
سوابق اشتغال شامل شغل، عنوان شغلی، بخش اقتصادی، ISIC، درآمد، بازه اعتبار و ستون‌های تکمیلی آخرین مدل است.

مطابق Constraint مدل، برای هر رکورد اشتغال دقیقاً یکی از دو منبع کارفرما باید ثبت شود:
- `EMPLOYER_PARTY_ID` برای کارفرمایی که خودش Party موجود است؛ یا
- `EMPLOYER_NAME` برای کارفرمای خارج از CIF.

ثبت هم‌زمان هر دو یا خالی‌بودن هر دو پذیرفته نمی‌شود.

## شخص حقوقی و مجوز فعالیت
فرم شخص حقوقی اطلاعات `MAIN_ACTIVITY_DESCRIPTION`، `ECONOMIC_SECTOR_CODE`، `ISIC_CODE`، تعداد کارکنان، اندازه بنگاه و نوع مالکیت را روی `ORGANIZATION` نگهداری می‌کند.

مجوزها به‌صورت رکورد مستقل `PARTY_LICENSE` ثبت می‌شوند. یکتایی نوع و شماره مجوز و ترتیب تاریخ صدور/انقضا قبل از ارسال SQL کنترل می‌شود.

## منابع درآمد و دارایی/تعهد
هر منبع درآمد در `PARTY_INCOME_SOURCE` و هر قلم دارایی/تعهد در `PARTY_ASSET_LIABILITY` به‌صورت رکورد مستقل نگهداری می‌شود. مبالغ منفی پذیرفته نمی‌شوند و تمام ویرایش‌ها از `RECORD_VERSION` برای Optimistic Locking استفاده می‌کنند.

## Reference Data
Lookup فقط برای روابطی استفاده شده است که Source of Truth صریح دارند، از جمله:
- `CIF.REF_OCCUPATION`
- `CIF.REF_ECONOMIC_SECTOR`
- `CIF.REF_ISIC_ACTIVITY`
- `CIF.REF_SOURCE_OF_FUNDS`
- `CIF.REF_SOURCE_OF_WEALTH`
- `CIF.REF_TAX_STATUS`
- `CIF.REF_VERIFICATION_STATUS`
- `CIF.REF_LICENSE_TYPE`
- ارز از اطلاعات پایه عمومی GEO

برای ستون‌هایی مانند `JOB_STATUS`، `EMPLOYMENT_STATUS_CODE`، `OCCUPATION_GROUP_CODE`، `CONTRACT_TYPE_CODE`، `FINANCIAL_RELATION_PURPOSE_CODE`، `FINANCIAL_CAPACITY_CODE`، `ITEM_TYPE_CODE` و برخی `STATUS_CODE`ها که در مدل تحویلی رابطه REF مستقل و صریح ندارند، جدول یا نگاشت ساختگی ایجاد نشده است.

## مسیر UI
`/cif/parties/{partyId}/onboarding/financial-employment`

مسیر از انتهای فاز ۲ و همچنین از Customer 360 در دسترس است.

## Customer 360
تب «مالی و شغلی» به Customer 360 اضافه شده و داده‌های مالی، منابع درآمد، سوابق اشتغال یا مجوزها و دارایی/تعهد را مستقیماً از API پرونده Party نمایش می‌دهد.

## مرحله بعد
فاز ۴ فرم عملیاتی روی شناسه‌های تکمیلی و مدارک Party متمرکز خواهد بود.

## تکمیل Fix15 — الگوی UI و Reference Data

از `0.3.22-prototype-fix15` قواعد زیر در فرم فاز ۳ اعمال شده است:

- کارفرما به دو حالت صریح «ثبت‌شده در CIF» و «خارج از سامانه / محل فعالیت» تفکیک شده است؛ در حالت خارج سامانه، `EMPLOYER_NAME` قابل ورود و الزامی است و `EMPLOYER_PARTY_ID` غیرفعال می‌شود.
- `ISIC_CODE` در UI به‌صورت کد عددی نمایش داده نمی‌شود؛ `NAME_FA` فعالیت نمایش و Code فقط Persist می‌شود.
- `PARTY_INCOME_SOURCE.SOURCE_TYPE_CODE` از `REF_SOURCE_OF_FUNDS` و `STATUS_CODE` منابع درآمد و دارایی/تعهد از `REF_WORKFLOW_STATUS` تغذیه می‌شوند. برای تاب‌آوری Prototype، Fallback کنترل‌شده هم‌ارز Seed رسمی وجود دارد، ولی Database Migration همچنان Source of Truth محیط موجود را اصلاح می‌کند.
- `PARTY_ASSET_LIABILITY.ITEM_TYPE_CODE` چون در مدل فیزیکی جاری Reference Table مستقل ندارد، از Vocabulary عملیاتی کنترل‌شده استفاده می‌کند؛ عنوان فارسی نمایش داده می‌شود و Code ذخیره و در Backend نیز Validate می‌شود.
- قاعده عمومی فرم‌های بعدی: **Title در UI / Code یا ID در Persistence**؛ از ورود مستقیم کد فنی توسط کاربر فقط در مواردی استفاده شود که خود کد یک شناسه کسب‌وکاری قابل فهم باشد (مانند کد مالیاتی).
