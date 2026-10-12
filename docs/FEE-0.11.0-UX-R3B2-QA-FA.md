# FEE R3-B.2 — گزارش دامنه آزمون

- مبنا: سورس R3-B.1 با Backend یکپارچگی R3-B.
- تولیدی: صفر فایل تغییریافته.
- جدید: دو فایل Java Test در زیرشاخه FEE، ابزار Read-only HTTP، Verifier، Installer و Uninstaller، مستندات.
- آزمون ایستا: روی نسخه مبنا اجرا شود.
- آزمون Java با Mockito: Maven/JUnit در Windows UAT اجرا شود.
- Oracle: به‌صورت پیش‌فرض غیرفعال، فقط پس از احراز DB_NAME + CON_NAME و تأیید آزمایشی بودن پایگاه.
- هزینه Sequence در آزمون Oracle: افزایش Sequence امکان دارد، با Rollback برنمی‌گردد.
- اصل محدودسازی: هیچ رکوردی در تعرفه واقعی ۸۳۷ آزمایش ثبت نمی‌شود.
- عدم تداخل: FEE2 / Product Builder / app.routes.ts / AppShell / Backend Production / Oracle DDL بدون تغییر.
