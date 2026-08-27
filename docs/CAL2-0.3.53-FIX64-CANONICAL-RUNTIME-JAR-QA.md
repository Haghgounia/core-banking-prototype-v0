# CAL2 0.3.53 — FIX64: Canonical Runtime JAR Name

## درخواست

نام Runtime JAR در تمام مسیرهای پروژه ثابت باشد و فقط `core-banking-prototype.jar` استفاده شود.

## قرارداد نهایی

| جزء | قرارداد |
|---|---|
| Runtime JAR | `app/core-banking-prototype.jar` |
| Maven output | `backend/target/core-banking-prototype.jar` |
| Build version marker | `app/BUILD-VERSION` |
| Source release version | `VERSION` |

## تغییرات

1. `build-production.cmd` و `build-production.sh` فقط Runtime JAR با نام ثابت تولید می‌کنند.
2. `bin/start.cmd` و `bin/start.sh` فقط همان نام ثابت را اجرا می‌کنند.
3. `bin/export-database.cmd` از همان نام ثابت استفاده می‌کند.
4. Build پیش از Packaging، JARهای قبلی پوشه `app` را پاک می‌کند.
5. Build مقدار `VERSION` را در `app/BUILD-VERSION` ثبت می‌کند.
6. Start قبل از `java -jar` مقدار `BUILD-VERSION` را با `VERSION` مقایسه می‌کند؛ در صورت اختلاف، اجرا متوقف و Rebuild مطالبه می‌شود.
7. Guard `verify-runtime-artifact-contract.mjs` قرارداد بالا را در هر دو سیستم عامل کنترل می‌کند.

## نتیجه معماری

نام Artifact از Release Identity جدا شده است. Release Identity همچنان در `VERSION` و Metadata سامانه مدیریت می‌شود، در حالی که Deployment Contract یک نام ثابت و ساده دارد. Marker مستقل Build مانع Stale Artifact می‌شود.

## DDL / Migration

ندارد.
