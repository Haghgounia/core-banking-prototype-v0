# فرم‌های عملیاتی Party — فاز ۲: نشانی و اطلاعات تماس

## هدف
این فاز ادامه مستقیم فاز ۱ ایجاد Party است و پس از ایجاد اتمیک `PARTY + PARTY_NAME + PERSON/ORGANIZATION + PARTY_IDENTIFIER`، اطلاعات نشانی و تماس Party را تکمیل می‌کند.

## جداول عملیاتی
- `CIF.ADDRESS`
- `CIF.PARTY_ADDRESS`
- `CIF.CONTACT_POINT`
- `CIF.CONTACT_POINT_ADDRESS`

## همگام‌سازی با CIF-tables3.xlsx
مدل Application برای ستون‌های جدید `ADDRESS` شامل `COUNTY_CODE`, `NEIGHBORHOOD_TEXT`, `MAIN_STREET_TEXT`, `SIDE_STREET_TEXT`, `PLAQUE_NO`, `FLOOR_NO`, `UNIT_NO`, `ADDRESS_DETAIL` تکمیل شده است.

برای `PARTY_ADDRESS` نیز `TENURE_TYPE_CODE`, `VERIFICATION_STATUS_CODE`, `SOURCE_CODE` و برای `CONTACT_POINT` فیلدهای `COUNTRY_DIAL_CODE`, `AREA_CODE`, `EXTENSION_NO`, `OWNER_TYPE_CODE`, `VERIFICATION_STATUS_CODE`, `VERIFICATION_METHOD_CODE` پوشش داده شده‌اند.

## جغرافیا
در UI از Source of Truth موجود GEO استفاده می‌شود و زنجیره استان -> شهرستان -> بخش -> شهر از API اطلاعات پایه بارگذاری می‌شود. جدول مرجع جغرافیایی موازی در CIF ساخته نشده است.

## ارتباط تماس با نشانی
API و UI جدید برای `CONTACT_POINT_ADDRESS` امکان اتصال تلفن ثابت، نمابر یا تماس سازمانی به نشانی مشخص Party را فراهم می‌کند.

فرم فاز ۲ برای نشانی، راه تماس و ارتباط تماس/نشانی عملیات ایجاد، ویرایش و حذف را ارائه می‌کند و ویرایش‌ها با `RECORD_VERSION` کنترل هم‌زمانی می‌شوند.

## نکته Reference Data
در فایل مدل تحویلی برای `TENURE_TYPE_CODE` و `OWNER_TYPE_CODE` جدول مرجع مستقل و برای `PARTY_ADDRESS.SOURCE_CODE` رابطه FK/REF صریح تعریف نشده است. بنابراین در این نسخه جدول یا نگاشت مرجع ساختگی ایجاد نشده و این فیلدها به‌صورت کد اختیاری نگهداری می‌شوند.

## مسیر UI
`/cif/parties/{partyId}/onboarding/contact-address`

پس از تکمیل فاز ۲، کاربر به نمای ۳۶۰ Party بازمی‌گردد. فاز بعدی فرم عملیاتی، اطلاعات مالی و شغلی است.

## منطق رکورد اصلی
نشانی و راه تماس اصلی در محدوده نوع مربوطه مدیریت می‌شوند؛ بنابراین Party می‌تواند هم‌زمان یک نشانی اصلی منزل و یک نشانی اصلی محل کار، یا یک موبایل اصلی و یک تلفن ثابت اصلی داشته باشد.
