# EDU / General Reference 0.3.83 — FIX91

## هدف
رفع دو مشاهده Runtime: تفکیک‌نشدن گزینه‌های واژه‌ها در منوی اطلاعات پایه عمومی و دیده‌نشدن فیلترهای فرم‌های جدید EDU.

## علت Runtime نسخه قبل
Source نسخه 0.3.82 شامل تغییرات UI بود، اما Release Metadata همگام نبود: Maven/Frontend هنوز روی 0.3.81 قرار داشتند و Install Guide مورد انتظار Guard برای 0.3.82 موجود نبود. این وضعیت می‌توانست `build-production` را قبل از ساخت JAR جدید متوقف کند. همچنین Source Package قبلی `app/BUILD-VERSION` را همراه خود داشت که در Overlay روی نصب قبلی می‌توانست Marker را بدون جایگزینی JAR تغییر دهد. در 0.3.83 هر دو مورد رفع شده‌اند.

## اصلاح منو
Category دو Descriptor زیر از `GENERAL` به `VOCABULARY` تغییر کرده است:
- `name-romanization-dictionary`
- `name-affix-dictionary`

ابزار `name-romanization-tool` نیز در همان گروه UI نمایش داده می‌شود. Frontend برای سازگاری با Backend قدیمی، Category قبلی `GENERAL` را نیز تشخیص می‌دهد.

## فیلترهای EDU
- `edu-education-levels`: نظام آموزشی، قابل انتخاب، وضعیت فعال
- `edu-education-fields`: رشته/گروه والد، نظام آموزشی، نوع گره، قابل انتخاب، وضعیت فعال
- `edu-education-field-levels`: رشته تحصیلی، مقطع تحصیلی، منبع، وضعیت فعال
- `edu-education-institutions`: نوع مؤسسه، نظام آموزشی، وضعیت مؤسسه، اعطاکننده مدرک، وضعیت فعال
- `edu-education-sources`: نوع منبع، سال منبع، وضعیت فعال
- `edu-education-source-mappings`: منبع، نوع موجودیت، نوع نگاشت، وضعیت تطبیق، وضعیت فعال

فیلترها از UI با پارامتر `filter.<apiField>` ارسال می‌شوند. Backend فقط فیلد موجود در Descriptor و قابل‌نوشتن را قبول می‌کند و شرط Oracle به‌صورت Parameterized Query ساخته می‌شود.

## QA Runtime
1. `build-production.cmd` را اجرا کنید و `Built version: 0.3.83` را ببینید.
2. برنامه را با `bin\start.cmd` اجرا کنید و نسخه 0.3.83 را در Header کنترل کنید.
3. `#/reference-data/general` را باز کنید؛ گروه مستقل «واژگان و اصطلاحات» باید دقیقاً سه گزینه مرتبط را نشان دهد.
4. هر شش فرم `edu-*` را باز کنید؛ زیر جست‌وجوی عمومی باید بخش «فیلترهای تخصصی» وجود داشته باشد.
5. در `edu-education-field-levels` ستون‌های رشته و مقطع باید عنوان Lookup را نمایش دهند.
6. یک فیلتر را تغییر دهید؛ Pagination باید به صفحه اول برگردد و Request شامل `filter.*` باشد.
7. «پاک کردن فیلترها» باید Query را بدون فیلترهای تخصصی دوباره اجرا کند.

## Guard Build
`tools/verify-edu-reference-ui.mjs` هجده کنترل استاتیک روی Category منو، پوشش شش فرم EDU، UI فیلترها، Gateway، Controller و Oracle Repository اجرا می‌کند.

## Database
هیچ DDL، Migration یا Seed جدیدی لازم نیست.
