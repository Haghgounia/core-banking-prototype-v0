Core Banking Prototype 0.3.2 - Build Fix 1

علت خطا:
Spring Boot 4.1 از Jackson 3 به صورت پیش فرض استفاده می کند. فضای نام databind از com.fasterxml.jackson.databind به tools.jackson.databind تغییر کرده است.

فایل های اصلاح شده:
1) PartyReferenceMetadataRegistry.java
2) PartyReferenceMetadataRegistryTest.java
3) party-reference-page.component.ts
4) party-reference-page.component.html

بعد از جایگزینی فایل ها:
bin\stop.cmd
rmdir /s /q frontend\dist
rmdir /s /q backend\src\main\resources\static
build-production.cmd
bin\start.cmd

برای این Build Fix هیچ تغییر Oracle/DDL لازم نیست.
