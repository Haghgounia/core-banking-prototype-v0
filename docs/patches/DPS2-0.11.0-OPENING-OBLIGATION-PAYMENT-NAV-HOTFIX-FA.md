# DPS2 Opening — Obligation Grid + Payment Instrument Navigation Hotfix

Base: 0.11.0 / 2026-09-30-1331 with prior Opening patches applied.

## اصلاحات
- بازیابی قرارداد HTML مرجع برای Step 4: INITIAL_BALANCE + OPENING_FEE + TAX و در صورت درخواست، CARD_FEE / CHEQUEBOOK_FEE.
- مقادیر Prototype مطابق HTML مرجع و با برچسب شفاف شبیه‌سازی سرویس هستند: Opening Fee=100,000؛ Tax=10,000؛ Card Fee=150,000 per card؛ Chequebook Fee=80,000.
- `FEE_SERVICE` و `TAX_SERVICE` به‌عنوان Source ثبت می‌شوند؛ متن UI صراحتاً اعلام می‌کند این ارقام تعرفه واقعی بانک نیستند.
- دکمه «تکمیل درخواست کارت/دسته‌چک» Editor ابزار پرداخت را مقداردهی کرده، Section را Flash و با `scrollIntoView` به `DEPOSIT_OPENING_PAYMENT_INSTRUMENT` هدایت می‌کند.
- Contract tests برای جلوگیری از بازگشت این دو مغایرت به‌روزرسانی شدند.

## Database
بدون DDL/Migration.
