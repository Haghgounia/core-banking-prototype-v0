# گزارش QA — FEE3-BE-R2 — ۲۶ اکتبر ۲۰۲۶

## نوع آزمون و نتیجه
| کنترل | نتیجه | حدود اعتبار |
|---|---|---|
| R1 Backend static verifier | 52/52 PASS | وجود و مرز APIهای R1 |
| R2 Backend / Frontend static contract | 48/48 PASS | نگاشت، مرزهای UAT، مسیرهای اصلی |
| R8 UI reference verifier | 59/59 PASS | تطبیق استاتیک فرم و 33 کنترل مبلغی |
| Java compile (isolated project signatures) | PASS | فقط بسته fee3 با Stubهای Spring/Jackson، **نه Maven واقعی** |
| Java pure business policy | 10/10 PASS | تاریخ، ارز، UAT context، سبد وثیقه |
| TypeScript strict source check | PASS | با Shims جایگزین Angular؛ **نه Angular AOT** |
| TypeScript mapper runtime tests | 17/17 PASS | هویت، ارز، source، فرمول سفارشی، آزمون و round-trip |
| Safe patch installer | PASS | ۲۱ هدف فایل شامل ۱۹ فایل FEE3 و دو Build Wrapper؛ نصب تکراری بی‌اثر و تغییر مستقل فایل سبب CONFLICT بدون نوشتن شد |
| Read-after-write verification | PASS (source) | UI شناسه، وضعیت Draft، شناسنامه، ارزها، اعتبار، روش و تعداد قواعد/آزمون‌ها را مجدداً می‌خواند؛ Oracle runtime هنوز تست نشده است |
| Angular Browser UAT | NOT RUN | باید روی ویندوز پروژه انجام شود |
| Oracle SQL API/transaction UAT | NOT RUN | نیازمند اجرای کاربر در محیط توسعه |

## سیاست‌های منفی
- ارز غیرریالی خروجی با ستون‌های `*_IRR` رد می‌شود.
- تاریخ‌های قبل از روز معتبر بانک، دامنه محصول نامرتبط، و منبع نرخ نامعتبر رد می‌شوند.
- ویرایش `FCE-*`، نسخه غیرفعال/غیر Draft، تعارض RECORD_VERSION و تغییر بیرون از UAT رد می‌شود.
- عدم دسترسی به `FCE_REF_*` از Oracle، به‌صورت آفلاینِ صریح نمایش داده می‌شود، نه به‌عنوان نرخ زنده.
- داده‌های شبیه‌سازی آزمون هرگز به وضعیت اجرایی Bank Test تبدیل نمی‌شوند.

## موارد باز برای Gate بعدی
- UAT با پایگاه داده واقعی و Build واقعی Spring Boot + Angular.
- تأیید نگاشت کالا/محصول به PDL و Currency Conversion غیرریالی.
- Authentication/RBAC/Maker-Checker، Workflow و Approval.
- سنجش E2E ضمانت‌نامه و قرض‌الحسنه، برآورد سود مشارکتی و Formula Sandbox.
