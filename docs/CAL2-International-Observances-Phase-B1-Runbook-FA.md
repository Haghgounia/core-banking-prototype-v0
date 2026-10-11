# CAL2 — مرحله B1: تکمیل تدریجی مناسبت‌های بین‌المللی و قواعد تاریخ متغیر

تاریخ: 2026-10-11 — هدف: توسعه کد 0.11.0

## دامنه و محدودیت

این بسته فقط **۷۲ تعریف جدید** از فهرست رسمی UNESCO (۶۳ مورد فهرست‌شده، بخشی اعلام‌شده توسط UN) و کمپین‌های رسمی WHO (۹ مورد) را به‌عنوان یک Seed محدود و قابل بازبینی تهیه می‌کند، به‌علاوه ایجاد قاعده تاریخ متغیر برای `INTL_WORLD_MARITIME_DAY` موجود. مرجع اعلان‌دهنده پیشنهادی برای ۷۲ مورد: ۲۴ UN، ۳۹ UNESCO، ۹ WHO. انتساب‌های مرجع در موارد مشترک UN/UNESCO نیازمند مقایسه با **Working Master 268تایی** و در صورت لزوم اصلاح دقیق‌تر است. **فایل Master 268تایی در ZIP اخیر موجود نبود**؛ بنابراین این بسته پوشش قطعی ۲۳۵ Missing را ادعا نمی‌کند و جایگزین Crosswalk کامل نیست.

**این بسته در Runtime شما اجرا نشده است**. موفقیت آزمون‌های استاتیک به معنی موفقیت Build، Migration یا نمایش تقویم در Oracle نیست.

## مسیر فایل‌ها

- `database/oracle/cal2/migrations/0.11.0-international-variable-rule-engine.sql` — DDL تکمیلی به‌صورت افزایشی و قابل اجرای مجدد.
- `database/oracle/cal2/migrations/0.11.0-international-observance-verified-seed-b1.sql` — درج ۷۲ تعریف + ۷۳ قاعده جدید در اجرای اولیه و ساخت وقوع‌ها در `EVENT_OCCURRENCE`.
- `database/oracle/cal2/migrations/0.11.0-international-observance-phaseb1-readonly-verify.sql` — کنترل فقط‌خواندنی Oracle.
- `database/oracle/cal2/data/international-observances-b1-verified.tsv` — Manifest با عنوان دو‌زبانه، مرجع اعلان‌دهنده و URL منبع.
- `tools/verify-cal2-international-b1.mjs` — آزمون استاتیک Node.js (بدون دسترسی Oracle).

## پیش‌نیازها

1. از Schema `CAL2` یا حداقل جداول `EVENT`, `EVENT_RECURRENCE_RULE`, `EVENT_OCCURRENCE`, `SOURCE_AUTHORITY` پشتیبان معتبر تهیه کنید.
2. Migration مرحله A باید قبلاً با پیام `CAL2_INTL_OBSERVANCE_PROVENANCE_PASS sources=3 mappedEvents=33` تکمیل شده باشد (طبق خروجی شما انجام شده است). **آن فایل را برای B1 اجرا نکنید**.
3. دقیقاً بررسی کنید Runtime و SQL Developer به همان PDB، به‌طور مشخص `FREEPDB1`، وصل می‌شوند.
4. Backend را قبل از اجرای DDL مربوط به نوع قاعده جدید متوقف کنید؛ نسخه قبلی موتور وقوع این نوع قاعده را نمی‌شناسد.
5. از ۷۲ عنوان Manifest در برابر نسخه واقعی Oracle و Working Master مستقل، تطبیق نام/هم‌معنا را پیش از Seed انجام دهید. SQL، تصادم کد و تکرار **عنوان دقیق** فارسی/انگلیسی را متوقف می‌کند؛ هم‌معنایی زبانی را تضمین نمی‌کند.

## ترتیب اجرا

1. کد این Patch را به پروژه اعمال کنید و در CMD در مسیر پروژه اجرا کنید:

   `node tools\\verify-cal2-international-b1.mjs`

2. SQL Developer: فایل `0.11.0-international-variable-rule-engine.sql` را با **F5** روی PDB هدف اجرا کنید. در انتها `CAL2_B1_VARIABLE_SCHEMA_PASS` را بررسی کنید. این اسکریپت DDL دارد؛ در Oracle Commit ضمنی ایجاد می‌شود.
3. SQL Developer: فایل `0.11.0-international-observance-verified-seed-b1.sql` را با **F5** اجرا کنید. در اجرای اولیه‌ی بدون برخورد انتظار داریم `CAL2_B1_EVENTS_ADDED=72`, `CAL2_B1_ALL_NEW_RULES_ADDED=73` و `CAL2_B1_SEED_AND_OCCURRENCE_COMMIT_COMPLETE`. ممکن است تعداد `EVENT_OCCURRENCE` تولیدشده بنا بر بازه سال‌های واردشده به CAL2 متفاوت باشد. اگر اسکریپت روی برخورد نام یا قاعده متوقف شد، **نباید به‌زور ادامه دهید**؛ داده را Reconcile کنید.
4. فایل `0.11.0-international-observance-phaseb1-readonly-verify.sql` را با **F5** اجرا کنید. صرف وجود رکوردها کافی نیست؛ `CAL2_B1_STATUS=FULLY_APPLIED` و `B1_2026_OCCURRENCES_VERIFIED=72/72` باید تأیید شوند. اگر تقویم مرجع ۲۰۲۶ کامل نباشد، پیام `DATA_APPLIED_OCCURRENCES_NOT_FULLY_TESTABLE` دریافت می‌شود.
5. کد جدید را با `build-production.cmd` در ویندوز Build و سرویس را طبق روش فعلی پروژه راه‌اندازی کنید. سپس فرم‌های «مناسبت‌های تقویم»، «رخدادهای مناسبت‌ها» و نمای ماهانه را کنترل کنید.

## نقاط پذیرش

| کنترل | انتظار در دیتابیس baseline | توضیح |
|---|---:|---|
| کل `CAL2.EVENT` | 316 | 244 + 72؛ اگر اطلاعات دیگری پس از مبنا اضافه شده باشد، مقدار متفاوت است |
| رویدادهای `INTERNATIONAL` | 132 | 60 + 72؛ اگر اطلاعات دیگری اضافه شده باشد، متفاوت است |
| قواعد جدید | 73 | 72 مناسبت + یک قاعده دریانوردی موجود |
| قواعد فعال | 315 | 242 + 73؛ مشروط به عدم تغییر همزمان |
| نمونه روز جهانی دریانوردی ۲۰۲۶ | 2026-09-24 | آخرین پنجشنبه سپتامبر، IMO |
| نمونه روز جهانی فلسفه ۲۰۲۶ | 2026-11-19 | سومین پنجشنبه نوامبر، UNESCO |
| نمونه مقابله با خشونت در مدرسه ۲۰۲۶ | 2026-11-05 | اولین پنجشنبه نوامبر، UNESCO |
| تعطیلی بانکی | بدون تغییر | Seed تمام رویدادهای جدید را با `DEFAULT_HOLIDAY_FLAG='N'` ثبت می‌کند؛ نباید روز بانکی را ببندد |

## پس از B1

- فهرست `CAL2.xlsx` و Working Master رسمی ۲۶۸تایی نیازمند Crosswalk مجدد روی نسخه کامل ۳۱۶+ رکورد است؛ ۲۳۵ Missing مبنای پیشین باید از نو محاسبه شود، نه اینکه با کم کردن کورکورانه ۷۲ عدد از آن نتیجه‌گیری شود.
- پشتیبانی مناسبت‌های «آخرین جمعه ماه قمری»، «اولین شنبه/هفته‌های خاص»، بازه چندروزه و مناسبت‌های شناور مبتنی بر اعلان‌های سالانه در این بسته پیاده‌سازی نشده است.
- منشأ `UNESCO` برای عناوین مشترک UN/UNESCO در Manifest صرفاً پیشنهاد اولیه بازبینی‌شده است؛ دفتر مرجع نهایی باید Resolution یا Proclamation صادرکننده را به‌صورت دقیق ثبت کند.

## منابع عمومی مورد استفاده

- UNESCO: https://www.unesco.org/en/days/list?hub=180536
- UN: https://www.un.org/en/observances/list-days-weeks
- WHO: https://www.who.int/campaigns
- IMO: https://www.imo.org/en/mediacentre/pressbriefings/pages/2026-2027-world-maritime-day-theme.aspx
- UNESCO World Philosophy: https://www.unesco.org/en/days/world-philosophy
