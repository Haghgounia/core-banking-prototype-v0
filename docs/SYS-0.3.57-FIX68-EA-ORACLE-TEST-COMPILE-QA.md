# Core Banking Prototype 0.3.57 — FIX68

## هدف
اصلاح خطای Java test compilation در تست round-trip مربوط به EA/Oracle XMI که در نسخه 0.3.56 مانع تکمیل `build-production.cmd` می‌شد.

## خطای گزارش‌شده

```text
EaOracleXmiWriterTest.java:[47,64] variable fk is already defined in method generatedXmiCanBeReadBackByEaParserAndContainsPhysicalArtifacts()
```

## علت
در متد تست، یک متغیر محلی با نام `fk` از نوع `OracleEaForeignKey` وجود داشت و در همان Scope، پارامتر Lambda در `anyMatch` نیز با نام `fk` تعریف شده بود. Java اجازه Shadow کردن Local Variable محصورکننده توسط پارامتر Lambda را نمی‌دهد.

## اصلاح
پارامتر Lambda از `fk` به `parsedFk` تغییر کرد. هیچ تغییر رفتاری در Parser، Comparator، XMI Writer یا Runtime ایجاد نشده است.

## دامنه Regression
قابلیت‌های FIX67 بدون تغییر باقی مانده‌اند:
- نادیده گرفتن اختلاف `TIMESTAMP(0)` در EA در برابر `TIMESTAMP(6)` در Oracle؛
- مقایسه PK؛
- مقایسه FK؛
- مقایسه Check Constraint؛
- حذف Schemaهای سیستمی Oracle از لیست؛
- مقایسه EA Alias با Oracle TABLE COMMENT برای عنوان فارسی جدول.

## Verification
Static verifierهای پروژه اجرا می‌شوند. Maven Wrapper در محیط تولید این بسته به دلیل عدم دسترسی شبکه به Maven Central قابل اجرا نبود؛ خطای مشخص Java در Source برطرف شده است.

## Database
هیچ DDL یا Migration دیتابیسی ندارد.
