# ORG — R2-D آخرین اصلاح دامنه نقشه شعب

**مبنای Source:** ORG R2-C Map Filters FIX (24-Oct-2026).

## تغییرات قابل بررسی
- تفکیک نوع جغرافیایی `CITY` و `RURAL_DISTRICT` حتی در صورت تکرار شناسه عددی.
- حفظ منشأ شناسه فنی شهر برای رکوردهای R1 با `LEGACY_BACKFILL_FLAG=1`؛ بدون DML روی ۱۳۴۰ رکورد.
- `GEO_ENTITY_TYPE_CODE` افزایشی/اختیاری با اسکریپت Oracle به‌صورت opt-in.
- ثبت و ویرایش جدید با انتخاب آبشاری جغرافیایی و اعتبارسنجی مرجع واقعی.
- تعداد استقرار فعال در کنار نام شهر و دهستان در فیلترهای نقشه.
- نمایشی‌ماندن واحدهای فاقد مختصات در فهرست (بدون نشانگر ساختگی).
- Backend فهرست/نقشه فقط‌خواندنی و Queryهای نوع‌دار؛ تماس‌های شعب و وضعیت `MERGED/CLOSED` دست‌نخورده.

## مدرک کنترل
- `tools/verify-org-map-r2d-closure.mjs`: ۱۱ کنترل مستقل ایستا (موفق در محیط آماده‌سازی).
- `tools/verify-org-r2c-map-ux.mjs`: ۱۰ کنترل (موفق).
- `tools/verify-org-r2c-map-filters.mjs`: ۱۲ کنترل (موفق).
- `tools/verify-org-r2b-location-contacts.mjs`: ۱۱ کنترل (موفق).
- `tools/verify-release-layout.mjs`: موفق.
- `tools/verify-node-tool-path-portability.mjs`: ۱۹۰ اسکریپت کنترل، موفق.
- Java 21: کلاس‌های واقعی Map Service، Location Models و Location Service با وابستگی‌های آزمایشی برای بررسی نحوی کامپایل شدند؛ نه Maven کامل.
- Java قرارداد MapService: ۹ کنترل اجرای SQL template و تفکیک type (موفق).
- TypeScript parser/transpiler: دو ماژول Angular تغییر‌یافته بدون خطای نحوی؛ نه Angular AOT.

## شرط پذیرش نهایی
نتیجه اسکریپت‌های postverify و map acceptance SQL فقط‌خواندنی باید در اتصال واقعی Oracle بررسی شود. Build کامل ویندوز و مرورگر UAT هنوز اجرا نشده است. هیچ مدرکی از اعمال DDL/DML روی Oracle کارفرما در این تحویل وجود ندارد. تا پیش از تأیید UAT، وضعیت Release برابر Source Candidate است.
