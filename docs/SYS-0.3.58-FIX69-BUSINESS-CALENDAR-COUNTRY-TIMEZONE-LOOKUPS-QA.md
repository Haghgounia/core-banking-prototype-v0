# Core Banking Prototype 0.3.58 — FIX69

## هدف
بازنگری ورودی‌های فرم `CAL2.BUSINESS_CALENDAR` بدون ایجاد Coupling با دامنه Party/CIF.

## تصمیم‌های دامنه و UI

| فیلد | رفتار نسخه 0.3.58 | منبع / مقدار Persist شده |
|---|---|---|
| کشور | Lookup جستجوپذیر | `GEO.COUNTRIES` / `COUNTRY_ISO_CODE` |
| منطقه زمانی | Lookup جستجوپذیر | IANA Zone IDs / خود Zone ID مانند `Asia/Tehran` |
| شناسه سازمان | Text | `CAL2.BUSINESS_CALENDAR.ORGANIZATION_ID` |

### کشور
- Query از Schema تنظیم‌شده در `core-banking.schemas.reference-data` و جدول `COUNTRIES` انجام می‌شود؛ مقدار پیش‌فرض Schema همان `GEO` است.
- تنها رکوردهای `IS_ACTIVE=1` ارائه می‌شوند.
- جستجو روی `COUNTRY_ISO_CODE`، `COUNTRY_ISO_CODE2`، `COUNTRY_NAME` و `COUNTRY_ENGLISH_NAME` انجام می‌شود.
- چون ستون مقصد CAL2 برابر `VARCHAR2(3)` است، `COUNTRY_ISO_CODE` سه‌حرفی Persist می‌شود.
- Label کاربر `COUNTRY_NAME` است و کد کمکی `ISO3 / ISO2` نمایش داده می‌شود.

### منطقه زمانی
- فهرست از `java.time.ZoneId.getAvailableZoneIds()` تولید می‌شود و Hard-code نیست.
- جستجوی متنی روی Zone ID انجام می‌شود.
- `Asia/Tehran` برای دسترسی سریع در ابتدای فهرست مرتب می‌شود، ولی انتخاب اجباری یا Default ذخیره‌شده نیست.
- Backend با `ZoneId.of(...)` مقدار ارسالی را Validation می‌کند.

### شناسه سازمان
- طبق تصمیم فعلی پروژه، `ORGANIZATION_ID` Lookup به CIF/Party نیست و همچنان Text باقی می‌ماند.
- در این نسخه هیچ FK یا وابستگی Domain جدیدی ایجاد نشده است.

## کنترل Backend
- `countryCode` فقط در صورتی پذیرفته می‌شود که `COUNTRY_ISO_CODE` متناظر در `GEO.COUNTRIES` فعال باشد.
- `timeZone` فقط در صورتی پذیرفته می‌شود که شناسه معتبر IANA/Java ZoneId باشد.
- هر دو فیلد اختیاری مطابق DDL فعلی CAL2 باقی مانده‌اند.

## Regression Guard
Guard جدید `tools/verify-calendar2-business-calendar-lookups.mjs` کنترل می‌کند:
1. Country و Time Zone از نوع Lookup باشند.
2. Organization ID همچنان Text باشد.
3. Country Lookup واقعاً از `COUNTRIES` و ISO code استفاده کند.
4. IANA Zone IDs در Backend استفاده شوند.
5. Validation سمت Backend فعال باشد.
6. Hintهای Angular برای هر دو Lookup وجود داشته باشد.

## DDL / Migration
هیچ DDL یا Migration جدیدی لازم نیست.

## Verification
- Static verifier اختصاصی FIX69: PASS.
- Maven compile در Runtime تولید نسخه به علت عدم امکان دریافت Maven distribution از Maven Central اجرا نشد؛ کنترل نهایی compile/test باید توسط `build-production.cmd` در محیط پروژه انجام شود.
