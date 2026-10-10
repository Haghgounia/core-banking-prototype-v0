# FEE2 Studio Layout — QA / Implementation Note

تاریخ: 2026-10-10

## هدف

افزودن یک لایه‌ی UX جدید برای FEE2 با چیدمان و الگوی تعامل نزدیک به نمونه‌ی مرجع `Simple_Fee_Engine_R5_2_FA_UI_Full`، بدون حذف یا تغییر مسیر فرم‌های فعلی FEE2 و بدون انتقال کد منبع پروژه‌ی مرجع.

## اصل پیاده‌سازی

- پیاده‌سازی جدید با Angular standalone components، Angular Material، Spring Boot و JdbcClient موجود پروژه انجام شده است.
- فرم‌های فعلی `/fee2`، `/fee2/versions` و `/fee2/tables/:table` حفظ شده‌اند.
- Studio جدید یک مسیر افزوده است و جایگزین فرم‌های فنی/عمومی FEE2 نیست.
- هیچ JavaScript/CSS/Java/Package از پروژه مرجع در سورس پروژه اصلی کپی نشده است؛ Layout و رفتار فقط به عنوان specification بصری/کسب‌وکاری استفاده شده‌اند.

## مسیرهای جدید

- `/fee2/studio/dashboard` — پیشخوان
- `/fee2/studio/catalog` — کاتالوگ و نسخه‌ها
- `/fee2/studio/editor` — طراحی نسخه و تب‌های کسب‌وکاری
- `/fee2/studio/simulation` — شبیه‌سازی و سناریوها
- `/fee2/studio/runtime` — مشاهده محاسبات ثبت‌شده
- `/fee2/studio/compare` — مقایسه نسخه‌ها
- `/fee2/studio/regulations` — مقررات و بخشنامه‌ها
- `/fee2/studio/reference` — داده مرجع
- `/fee2/studio/history` — تاریخچه محاسبات
- `/fee2/studio/audit` — ممیزی تغییرات

## Wizard ایجاد کارمزد

Wizard چهار مرحله دارد:

1. الگو
2. مشخصات
3. اعمال و مالیات
4. بازبینی

Wizard هر 9 روش محاسبه FEE2 را پوشش می‌دهد و در یک Transaction موارد زیر را ایجاد می‌کند:

- `FEE_DEFINITION`
- `FEE_VERSION` با وضعیت `DRAFT`
- `FEE_BINDING`
- `FEE_TAX` در صورت انتخاب کاربر

فعال‌سازی خودکار انجام نمی‌شود و lifecycle موجود FEE2 حفظ شده است.

## APIهای افزوده

- `GET /api/v1/fee2/studio/summary`
- `GET /api/v1/fee2/studio/catalog`
- `POST /api/v1/fee2/studio/fees`

## کنترل عدم کپی

`tools/verify-fee2-studio.mjs` وجود markerهای پروژه‌ی مرجع و فایل JavaScript در Studio جدید را رد می‌کند و CWD-independent است.

## نتایج کنترل استاتیک

- `verify-fee2-forms.mjs`: PASS
- `verify-fee2-studio.mjs`: PASS
- `verify-node-tool-path-portability.mjs`: PASS
- TypeScript transpile/syntax check برای فایل‌های جدید و تغییر یافته: PASS
- Java syntax/type-shape compile با stubهای Spring/Jackson/JdbcClient برای package کامل FEE2: PASS

Full Angular/Maven build در محیط تولید Patch اجرا نشده است، زیرا `frontend/node_modules` در محیط حاضر وجود ندارد و Maven dependency cache نیز موجود نیست. Build نهایی باید با `build-production.cmd` در محیط پروژه اجرا شود.
