# PB-R25 HF1 — رفع خطای Maven testCompile در تست تسهیلات

## خطا

```
LoanProductFinalUxR25Test.java:[73,84] local variables referenced from a lambda expression must be final or effectively final
```

در تست `durationsNeedUnitAndPartialNeedsPrepayment` متغیر `invalid` پس از استفاده در lambda دوباره مقداردهی می‌شد و از نظر Java effectively final نبود.

## تغییر

فقط یک فایل تست تغییر می‌کند:

`backend/src/test/java/com/behsazan/corebanking/productbuilder/application/LoanProductFinalUxR25Test.java`

هر سناریوی تست اکنون یک Map مستقل و `final` دارد؛ منطق بررسی مدت، پیش‌پرداخت جزئی و رفتار مورد انتظار تست حفظ شده است. هیچ کد production، UI، DDL، داده یا قرارداد API تغییر نمی‌کند.

## نصب روی R25 فعلی

```bat
cd /d D:\Projects\core-banking-prototype-v0
node tools\repair-pb-r25-hf1-testcompile.mjs --check
node tools\repair-pb-r25-hf1-testcompile.mjs --apply
node tools\repair-pb-r25-hf1-testcompile.mjs --check
node tools\verify-pb-r25-hf1-testcompile.mjs
cd backend
mvnw.cmd -Dtest=LoanProductFinalUxR25Test test
cd ..
build-production.cmd
```

ابزار نصب از فایل اصلی پشتیبان تهیه می‌کند؛ اجرای مجدد تغییری ندارد؛ روی سورس غیرمنتظره متوقف می‌شود.

**تأیید نهایی Build/Oracle همچنان به اجرای ویندوز وابسته است.**
