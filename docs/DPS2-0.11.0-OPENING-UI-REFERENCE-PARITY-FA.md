# DPS2 0.11.0 — Opening UI Reference Parity

تاریخ: 2026-09-29

## مبنا

مرجع این تغییر فایل نهایی کاربر `11(1).html` است. SHA-256 فایل مرجع دریافت‌شده:

`130425719de98ce8fc27c57c6ab28a9bdd1f41b1ac257189cf7322f21e26ca46`

این فایل از نظر بایت با `11.html` قبلی یکسان است؛ بنابراین اختلاف مشاهده‌شده ناشی از پیاده‌سازی Angular بود، نه تغییر مرجع.

## تغییر رویکرد

نسخه قبلی بیشتر Business/Field Contract را منتقل می‌کرد و ظاهر Material/Card موجود را حفظ می‌کرد. در این نسخه خود فایل مرجع به‌عنوان Visual/DOM Contract در نظر گرفته شده است.

- Sidebar شناور/داک‌شونده و Launcher
- Topbar و Page Head مطابق مرجع
- Wizard Shell و Stepper با ابعاد/چیدمان مرجع
- Domain/Product cards با ساختار مرجع
- فرم 12 ستونه و Controlهای native-like به‌جای ظاهر Material در Opening
- Signatory card و signing mode layout
- Withdrawal Media card grid + impact panel
- Product-derived profit card
- Service auto card با جدول 7 ستونه مرجع و editor استثناء داخلی
- Payment Instrument grid و جدول مرجع
- Pricing / Tax / Reward layouts
- Funding summary + table
- Step 5 / 6 / 7 با structure و visual hierarchy مرجع

## Runtime integration

ظاهر مرجع جایگزین Backend واقعی نشده است. Angular Reactive Form، Party search واقعی، Account lookup، runtime validation، aggregate persistence، settlement و activation همان مسیرهای واقعی پروژه را استفاده می‌کنند.

برای Service Recommendation، تا زمان وجود Adapter واقعی Product/Party/Channel، جدول فقط داده‌های ثبت‌شده واقعی Opening را نمایش می‌دهد و UI از ساخت داده جعلی برای پرکردن جدول خودداری می‌کند.

## Static gates

- `DPS2_OPENING_UI_REFERENCE_PARITY_PASS=70`
- `DPS2_OPENING_UI_REFERENCE_PARITY_FAIL=0`
- `DPS2_OPENING_FINAL_UI_11_FAIL=0`
- `DPS2_OPENING_REVIEWED_ALIGNMENT_FAIL=0`
- Four Deposits 59/25/4/7 PASS
- Node verifier path portability: 72 scripts PASS

## Build

Build کامل Angular/Maven در محیط بسته‌بندی اجرا نشده است زیرا `frontend/node_modules` در Source Package وجود ندارد و npm cache برای نصب offline کامل نیست. اجرای `npm ci --offline` با `ENOTCACHED` متوقف شد. Build نهایی باید روی محیط پروژه کاربر با dependencies موجود اجرا شود.
