# CLT-R1 Closure Candidate — گزارش QA محیط تولید بسته

**مبنای آزمایش:** Full source 2026-11-08 12:19 + System UI V1 + HF1 (قبل از تغییرات R1-Closure). **نسخه سامانه:** 0.11.0.

## آزمون‌های انجام‌شده در محیط ساخت Patch

| کنترل | نتیجه | سطح مدرک |
|---|---|---|
| تطبیق کاتالوگ دقیق UI/Backend | PASS — 17 جدول/204 ستون | Static contract |
| صحت ۱۷ فرم در Workspace مشترک | PASS — ساختار | Static؛ UAT اجرایی نیست |
| وضعیت و فیلدهای حسابرسی فقط در بخش فنی | PASS | Static |
| تاریخ شمسی بدون شیفت TIMESTAMP فاقد timezone، با ثانیه | PASS — ۸ سناریو | Pure TypeScript VM |
| بررسی پس از POST/PUT با GET مستقل، Version و فیلدهای ذخیره‌شده | PASS — قرارداد | Static + pure test |
| حذف دومرحله‌ای و انتظار 404 از GET بعد از DELETE | PASS — قرارداد | Static |
| کامپایل Java 21 `CltUnifiedReferenceValidation` | PASS | `javac` standalone |
| آزمون Java اعتبارسنجی نمونه (status/flag/managed/required/overflow) | PASS — ۶/۶ | اجرای مستقل، بدون DB |
| کنترل‌های قبلی CLT UI R3 | PASS — ۲۰/۲۰ | Static |
| کنترل‌های Unified R2 | PASS — ۱۸/۱۸ | Static |
| کنترل‌های UI سراسری System V1 | PASS — ۲۱/۲۱ | Static |
| Angular Template Type Check و Full Maven/JUnit این Patch | NOT_RUN | وابستگی‌های Maven/Angular در محیط ساخت موجود نیست |
| نصب آزمایشی روی وضعیت V1+HF1 و بازخوانی SHA-256 | PASS — ۱۳/۱۳ فایل | نمونه پروژه جداگانه |
| نصب مجدد بدون تغییر فایل | PASS — `FILES_TO_APPLY=0` | نصب آزمایشی |
| جلوگیری از overwrite فایل تغییر‌یافته خارج از Patch | PASS — خروج با خطای روشن | Negative installer test |
| وجود Installer و Manifest در ریشه ZIP | PASS — ۲۰ Entry / ۱۳ Payload | ZIP CRC + SHA-256 |
| Real Oracle CREATE/UPDATE/DELETE و مرورگر | NOT_VERIFIED | نیازمند محیط بانک؛ هیچ داده به صورت خودکار نوشته نشده |

## کنترل خط مبنا و ریسک‌ها

- تغییرات فقط در Backend Reference Validation و تست، Frontend Collateral Reference Workspace/Presentation/Service، اسکریپت‌های CLT، و اسناد Collateral است.
- همان DataSource مشترک و Starter استاندارد؛ نه Starter وثایق، نه اتصال دوم، نه تغییر IAM.
- `RECORD_VERSION` در Backend برای UPDATE/DELETE همراه شروط WHERE حفظ شده است؛ Readback دلیل حذف این گیت نیست.
- `G-16`, `G-17`، وضعیت صلاحیت بیمه‌گر و گیت‌های R2..R7 تغییر نکرده‌اند.
- نصب کدها با SHA-256 و Backup/Rollback کنترل می‌شود و DDL/DML اجرا نمی‌شود.
- مدرک UAT گام به گام در `CLT-R1-CLOSURE-17-TABLE-UAT.csv` قابل تکمیل است؛ این فایل به‌صورت پیش‌فرض همه سطرها را `NOT_VERIFIED` ثبت کرده است.

**معیار تأیید نهایی R1:** اجرای موفق Build و Spring Boot نسخه جدید + شواهد CRUD واقعیِ ۱۷/۱۷ فرم (بدون دست‌کاری رکوردهای عملیاتی)، شامل رفع مغایرت و پیام خطا؛ تا آن زمان `R1_CLOSURE_PENDING_UAT`.
