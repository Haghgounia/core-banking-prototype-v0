# CAL2 0.3.52 — FIX63: Runtime Artifact Contract (Superseded)

## وضعیت

این FIX یک Guard برای جلوگیری از اجرای Artifact قدیمی معرفی کرد. قرارداد نام‌گذاری آن در FIX64 بازنگری شده است.

از FIX64 به بعد تنها نام مجاز Runtime Artifact در Build، Start، Export و مستندات عملیاتی این است:

```text
app/core-banking-prototype.jar
```

کنترل تعلق Artifact به Release دیگر از طریق نام فایل انجام نمی‌شود؛ `app/BUILD-VERSION` نسخه Build را نگه می‌دارد و `start` قبل از اجرا آن را با فایل `VERSION` تطبیق می‌دهد. این طراحی هم نام فایل را ثابت نگه می‌دارد و هم مانع اجرای JAR قدیمی می‌شود.
