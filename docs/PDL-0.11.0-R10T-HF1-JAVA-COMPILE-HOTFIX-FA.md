# PDL 0.11.0 — R10T-HF1 Java Compile Hotfix

## مسئله
پس از PASS شدن Static Verifierهای R10T، Maven در کامپایل `PdlReferenceOptionService.java` با خطای `illegal start of expression` در انتهای `PARTY_RESOURCE_BY_COLUMN` متوقف می‌شد.

## علت
آخرین آرگومان `Map.ofEntries(...)` دارای comma انتهایی بود. Java در invocation متد، trailing comma را نمی‌پذیرد.

## اصلاح
- comma انتهایی پس از `RESIDENCY_STATUS_CODE` حذف شد.
- Guard جدید `verify-pdl-r10t-hf1-java-compile.mjs` اضافه شد تا این الگوی نحوی قبل از Maven کنترل شود.
- Guard جدید داخل `build-production.cmd` اجرا می‌شود.

## دامنه
این Hotfix فقط خطای کامپایل را اصلاح می‌کند و منطق Reference Data، UI Alignment و Persistence نسخه R10T را تغییر نمی‌دهد.
