# ORG UX R2 - گزارش کنترل کیفیت

| آزمون | نتیجه |
|---|---|
| 35 تعریف ORG و سازگاری mapping | PASS |
| 31 فرم مرجع در منو + 2 فرم درجه‌بندی | PASS (Static) |
| چهار منوی کنارگذاشته‌شده و حفظ Descriptor | PASS |
| روابط و فیلترهای جستجوی واحدهای سازمانی | PASS (Static) |
| Enum علت رویداد چرخه عمر | PASS (Static) |
| طرح درجه مستقیم، کنترل هم‌پوشانی و سیاست Java | PASS (Local Java smoke) |
| نظام BRANCH_STANDARD و کامپایل مستقل GradeService/Descriptor | PASS (Java source with test stubs) |
| نقشه انتخاب مختصات و انتقال به فرم | PASS (Static only) |
| نمایش تاریخ‌های ORG با کنترل شمسی | PASS (Static only) |
| انتقال سرویس کاشی نقشه به CARTO | PASS (Static only; live network not tested) |
| هفت نام جدید جداول نقاط خدمت | PASS (Static mapping) |
| Release root layout | PASS |
| کامپایل واقعی Angular / Maven | NOT RUN (npm unavailable, Maven unavailable) |
| تست زنده Oracle و نقشه | NOT RUN |

نکته: اجرای SQL مربوط به تغییر نام جدول‌ها، عملیاتی مستقل و مستلزم تأیید کاربر است؛ بسته Patch به تنهایی تغییر Oracle ایجاد نمی‌کند.
