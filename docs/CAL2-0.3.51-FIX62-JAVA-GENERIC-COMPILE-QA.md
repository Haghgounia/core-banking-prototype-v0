# CAL2 0.3.51 — FIX62: Java Generic Compile Fix

## مسئله
Build نسخه 0.3.50 در Java compilation با دو خطای Generic invariance در `Calendar2ReferenceRepository` متوقف می‌شد. `JdbcClient.query(...).list()` به دلیل نوع متغیر داخل lambda، `List<LinkedHashMap<String,Object>>` استنتاج می‌کرد، در حالی که مقصد `List<Map<String,Object>>` بود.

## اصلاح
در هر دو RowMapper مربوط به `searchCalendarDates` و `searchEvents` نوع متغیر `row` از `LinkedHashMap<String,Object>` به `Map<String,Object>` تغییر داده شد. پیاده‌سازی همچنان `new LinkedHashMap<>()` است، بنابراین ترتیب ستون‌های پاسخ حفظ می‌شود اما Generic return type با قرارداد متد سازگار است.

## Build guard
`build-production.cmd` اکنون پیش از Angular build، `mvnw.cmd -DskipTests compile` را اجرا می‌کند. در نتیجه خطاهای Java پیش از build فرانت‌اند شناسایی می‌شوند. Build نهایی همچنان پس از کپی Angular assets با `clean package` انجام می‌شود.

## انتظار آزمون روی Windows
1. `bin\stop.cmd`
2. `build-production.cmd`
3. مرحله Backend compile preflight باید بدون خطا تمام شود.
4. Angular build اجرا شود.
5. Maven `clean package` موفق شود و `app\core-banking-prototype.jar` ساخته شود.
6. پس از `bin\start.cmd`، Badge نسخه باید `0.3.51-prototype-fee-p1` باشد.
7. در CAL2، فیلترهای نوع تقویم/قرن/سال و Event Type، عنوان فارسی نوع رویداد و دیاگرام روابط باید قابل مشاهده باشند.

## محدودیت QA این محیط
Maven Wrapper در محیط تولید بسته به دلیل عدم دسترسی شبکه به Maven Central قادر به دانلود Maven distribution نبود؛ بنابراین compile نهایی در این محیط اجرا نشد. اصلاح انجام‌شده مستقیماً همان دو خطای گزارش‌شده توسط `javac` را برطرف می‌کند و Build کاربر مرجع نهایی Compile خواهد بود.
