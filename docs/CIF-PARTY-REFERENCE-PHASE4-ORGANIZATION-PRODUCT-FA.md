# CIF Party Reference - Phase 4: Organization & Product

## نتیجه Mapping

مدل منبع برای حوزه «سازمان و محصول» ۱۳ Reference Table تعریف می‌کند. در این نسخه ۱۱ جدول واقعاً جدید در `CIF` فعال شده‌اند. `REF_CURRENCY` به `GEO.CURRENCIES` و `REF_ORGANIZATIONAL_UNIT` به `DPS.REF_ORG_UNIT_CODE` نگاشت شده‌اند تا Source of Truth تکراری ایجاد نشود.

## فرم‌های فعال جدید

- بخش اقتصادی (`REF_ECONOMIC_SECTOR`)
- فعالیت اقتصادی ISIC (`REF_ISIC_ACTIVITY`)
- نوع شخصیت حقوقی (`REF_LEGAL_FORM`)
- نوع مجوز (`REF_LICENSE_TYPE`)
- شغل (`REF_OCCUPATION`)
- پیشنهاد محصول/خدمت (`REF_OFFER`)
- سمت/نقش سازمانی (`REF_OFFICER_ROLE`)
- دوره زمانی (`REF_PERIOD`)
- وضعیت رابطه محصولی (`REF_PRODUCT_HOLDING_STATUS`)
- نقش مشتری نسبت به محصول (`REF_PRODUCT_RELATIONSHIP_ROLE`)
- نوع محصول/خدمت بانکی (`REF_PRODUCT_TYPE`)

تعداد Seed این فاز: **44**.

## اتصال به Customer 360

در فرم `ORGANIZATION` سه فیلد `LEGAL_FORM_CODE`, `ECONOMIC_SECTOR_CODE` و `ISIC_CODE` از TextBox آزاد به Lookup واقعی CIF تبدیل شده‌اند. فرم ایجاد مشتری حقوقی نیز `LEGAL_FORM_CODE` را از `CIF.REF_LEGAL_FORM` انتخاب می‌کند و دیگر مقدار فرضی `OTHER` تولید نمی‌کند.
