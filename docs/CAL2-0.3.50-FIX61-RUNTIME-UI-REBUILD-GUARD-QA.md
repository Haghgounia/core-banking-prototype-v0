# CAL2 0.3.50 — FIX61: Runtime UI Rebuild Guard

## مسئله

در نصب آزمایشی، داده‌ها و Seedهای جدید CAL2 در Oracle قابل مشاهده بودند، اما رابط کاربری همچنان نسخه `0.3.48-prototype-fee-p1` را نشان می‌داد. علت، اجرای JAR قدیمی در کنار Source جدید بود؛ بسته Source قبلی عمداً JAR را حمل نمی‌کرد و Extract روی پوشه موجود می‌توانست Artifact قبلی را باقی بگذارد.

## اصلاح

1. نسخه Release به `0.3.50-prototype-fee-p1` ارتقا یافت و نسخه Frontend/Backend همگام شد.
2. `bin/rebuild-and-start.cmd` اضافه شد تا اجرای نسخه جدید فقط از مسیر Stop → Build → Start انجام شود.
3. `build-production.cmd` پیش از Build، JAR و `frontend/dist` قدیمی را حذف می‌کند؛ اگر Build شکست بخورد هیچ JAR قدیمی به عنوان نسخه جاری باقی نمی‌ماند.
4. Verifier تقویم دو قبل از Build بررسی می‌کند که موارد زیر در Source وجود داشته باشند:
   - فیلتر نوع تقویم/قرن/سال در نگاشت تاریخ‌ها؛
   - پیش‌فرض سال جاری برای نگاشت تاریخ؛
   - نمایش عنوان فارسی نوع رویداد؛
   - فیلتر نوع رویداد و نوع تقویم در رویدادها؛
   - دیاگرام روابط ۱۶ جدول CAL2 در صفحه اصلی تقویم دو.

## روش نصب پیشنهادی

- ZIP را در یک پوشه جدید Extract کنید.
- از پوشه اصلی، `bin\rebuild-and-start.cmd` را اجرا کنید.
- پس از Start، Badge نسخه در Header باید `0.3.50-prototype-fee-p1` باشد.
- اگر Badge نسخه دیگری بود، Runtime صحیح اجرا نشده است.

## شواهد Static QA

اجرای موفق:

- `node tools/verify-calendar2-reference.mjs`
- `node tools/verify-calendar-display-labels.mjs`
- `node tools/verify-calendar-reference.mjs`
- `node tools/verify-calendar-dataset-import.mjs`
- `node tools/verify-cif-persisted-grids.mjs`
- `node tools/verify-ea-oracle-comparison.mjs`

## محدودیت محیط Build

Runtime فعلی به Registryهای بیرونی npm/Maven دسترسی پایدار ندارد؛ بنابراین JAR نهایی در این محیط تولید نشده است. بسته تحویلی Source Package است، اما مسیر Build/Start طوری اصلاح شده که روی محیط توسعه/نصب دارای Dependencyها، Artifact قدیمی قابل استفاده نباشد.
