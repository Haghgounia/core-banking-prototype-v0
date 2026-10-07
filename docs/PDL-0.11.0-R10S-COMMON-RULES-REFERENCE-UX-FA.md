# PDL 0.11.0 / R10S — Common Rules Reference UX

## دامنه
این اصلاح روی مرحله «قواعد مشترک» در محصول‌ساز یکپارچه اعمال می‌شود و R10R را نیز در خود نگه می‌دارد.

فرم‌های تحت پوشش:
- PRODUCT_ELIGIBILITY_RULE — قاعده مشترک اهلیت
- PRODUCT_CHANNEL_RULE / PRODUCT_CHANNEL_OPERATION — قاعده کانال و عملیات مجاز
- PRODUCT_ORG_SCOPE — محدوده سازمانی محصول
- PRODUCT_REQUIRED_DOCUMENT — مدارک الزامی
- PRODUCT_REQUIRED_INQUIRY — استعلام‌های الزامی
- PRODUCT_PRICING_RULE / PRODUCT_PRICING_COMPONENT / PRODUCT_RATE_TIER — قیمت‌گذاری مشترک
- PRODUCT_RELATIONSHIP — ارتباط محصولات
- LOAN_ELIGIBILITY_EXTENSION — ادامه اهلیت وام در مسیر Child

## اصلاح اصلی
فیلدهای Business Code که قبلاً به علت نبود FK مستقیم در PDL به‌صورت Text Box نمایش داده می‌شدند، اکنون از Reference Data حاکمیتی تغذیه می‌شوند. مقدار Persist شده همچنان Business Code است و شناسه Surrogate جدول مرجع وارد PDL نمی‌شود.

منابع اصلی:
- نوع مشتری و وضعیت مشتری: CIF Party Reference
- بخش مشتری: DPS.REF_CUSTOMER_SEGMENT_CODE
- کانال / عملیات: مرجع عملیاتی افتتاح سپرده
- نوع/کد واحد سازمانی: DPS Reference Data
- نوع مدرک و نوع استعلام: CIF Party Reference
- مرحله الزام، وضعیت قاعده، ارز، تناوب، مبنای شمارش روز و سایر Codeهای شناخته‌شده: DPS Reference Data

در صورت وجود Check Constraint روی یک Code، گزینه‌های مرجع با همان دامنه مجاز دیتابیس محدود می‌شوند؛ بنابراین UI نمی‌تواند گزینه‌ای خارج از قرارداد Oracle ارائه کند.

## کنترل Backend
برای فیلدهای reference-controlled، Create و تغییر مقدار در Update فقط زمانی پذیرفته می‌شود که مقدار در Option Set حاکمیتی وجود داشته باشد. مقدار قدیمی نامعتبر در Update غیرمرتبط تحمل می‌شود، ولی تغییر آن باید به یک مقدار مرجع معتبر انجام شود.

## Grid
- گزینه‌های مرجع با عنوان فارسی نمایش داده می‌شوند و Code لاتین کنار عنوان چاپ نمی‌شود.
- FK labels قبل از اولین Render Grid بارگذاری می‌شوند.
- مقدار Legacy که دیگر عنوان مرجع معتبر ندارد با پیام فارسی مشخص می‌شود و Raw Code به کاربر نمایش داده نمی‌شود.
- در Gridهای Common Rules، ستون Code فنی که Reference/User label ندارد از نمایش حذف می‌شود؛ مقدار فنی در Persistence باقی می‌ماند.

## Database
DDL/Migration ندارد. R10S از Reference Data موجود پروژه استفاده می‌کند و Schema جدیدی ایجاد نمی‌کند.

## Qualification
Verifier اختصاصی:
`node tools/verify-pdl-common-rules-r10s.mjs`

Regressionهای الزامی:
- `node tools/verify-pdl-product-version-r10r.mjs`
- `node tools/verify-pdl-product-builder.mjs`
- `node tools/verify-release-layout.mjs`

Build کامل Angular/Java باید روی محیط Windows پروژه با `build-production.cmd` انجام شود.
