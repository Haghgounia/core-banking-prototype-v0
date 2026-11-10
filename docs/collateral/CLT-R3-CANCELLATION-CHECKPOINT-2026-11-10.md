# CLT R3 — Checkpoint 2026-11-10 — Insurance Cancellation G-06

**مبنای سورس:** `core-banking-prototype-v0-2026-11-09-R3-VALUATION-ATOMIC-COMMIT-SOURCE.zip` (نصب موفق توسط کاربر).  
**آخرین تغییر:** قابلیت جدید ارزیابی اثر لغو و درخواست مرحله‌ای/رویداد اتمیک نامزد لغو بیمه.

| حوزه | انجام‌شده در سورس | وضعیت اجرایی |
|---|---|---|
| R3 ضامن | Insert عملیاتی بانک‌گیت‌شده، Ledger/Idempotency و UI | Flag خاموش، بانک/Oracle Live تأیید نشده |
| R3 ثبت/تمدید بیمه | Atomic CREATE/RENEW با کنترل پوشش و زنجیره | Flag خاموش، بانک/Oracle Live تأیید نشده |
| R3 ارزیابی | Append history و supersession با CAS اشاره‌گر | Flag خاموش، بانک/Oracle Live تأیید نشده |
| R3 لغو بیمه — Preflight | GET از Oracle واقعی، شمارش همه تخصیص‌ها/تمدیدها، کنترل Event و شعبه | پیاده‌سازی شده؛ صحت با Oracle Live هنوز آزموده نشده |
| R3 لغو بیمه — Stage | Maker/Reviewer، Hash، Idempotency، CAS و Audit | آماده در کد؛ Flag و Schema مصوب لازم |
| R3 لغو بیمه — Final | Event append-only و رسید؛ بدون تغییر بیمه تاریخی یا تخصیص | آماده در کد؛ Feature Flag خاموش + مسیر HTTP بسته + Bank Release و DBA لازم |
| UI | کارتابل مستقل کنار فرم بیمه بدون تغییر HTML سفارشی | توسعه انجام شده؛ Angular Build/تحلیلگر نهایی اجرا نشده |
| SQL | SELECT dictionary و DDL Candidate غیرقابل‌اجرای خودکار | مهاجرت واقعی **انجام نشده** |

**شکاف‌ها:** G-04/G-05/G-06/G-07/G-09/G-11/G-18 و D-04/D-05/D-06/D-07 همچنان OPEN؛ هیچ مصوبه یا Closure ساختگی اعلام نمی‌شود. 

**گام بعدی مصوب مسیر:** R3 cross-domain gap closure و Traceability BUC/SUC/API/UI، تکمیل قراردادهای تصمیم/Oracle و سپس تصمیم عبور **مشروط** به R4 (Capacity/Allocation). فازهای R0–R7 و نام/ترتیب آن‌ها بدون تغییر هستند.
