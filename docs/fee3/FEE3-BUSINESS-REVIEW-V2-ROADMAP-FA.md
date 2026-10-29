# FEE3 — ثبت توسعه تجمیعی میز ارزیابی کارشناسی (۱۴۰۵/۰۸)

## تحویل جدید
میز ارزیابی کارشناسی در همان Angular موجود افزوده شد؛ انتخاب قلم/نسخه، شش کنترل، شرح اختلاف، خروجی CSV/JSON و بازیابی پرونده بدون ارتباط نوشتنی با Oracle. UI خواندنی است و نتیجه آن صرفاً نظر تحلیلگر است.

## وضعیت دروازه‌های قبلی
- F5-P1: انطباق فیلدهای ۳۰۰/۳۰۰ نسخه، PASS؛ ۷ اختلاف عنوان/منشأ باز.
- F5-P2A: ساختار ۳۱۳/۳۱۳ قاعده PASS؛ ۹۸ هشدار داده/کسب‌وکار باز.
- F5-P2B: محاسبه مهندسی ۲۰/۲۰ Java و مرجع PASS؛ نه UAT مالی.
- F5-P2C: آزمون HTTP ایزوله هنوز HOLD؛ UI جدید به این مسیر وابسته نیست.

## بعدی
ارزیابی طراحی و کاربردپذیری به دست تحلیلگر؛ سپس تکمیل روش‌های F6 و دامنه‌های F7، مدیریت تعرفه F8، UAT رسمی F9 و امنیت و انتشار F10. آزمون‌های unit و Build باید در مسیر خودکار توسعه انجام شوند، نه با Patchهای آزمایشی جداگانه برای کاربر.

**محدودیت:** Oracle DRAFT/INACTIVE، نرخ‌های تأییدنشده و کنترل‌های Posting/Workflow بدون تغییر باقی می‌مانند.


## V2 business review evidence (2026-10-29)
- Oracle catalogue summary contains no BASE_METHOD; fetch complete item aggregate on selection. Never infer method.
- Engineer-only pre-tax arithmetic: FIXED, PERCENT, single-rule USAGE; guarded by amount/rate/bounds and fail-closed; FORMULA specialized preview remains restricted.
- Defect fields per review dimension: severity, expected, actual, owner, tracking reference, status. Issue CSV plus six-dimension CSV, session JSON.
- V1 JSON import retains notes and reviewer, changes in Oracle snapshot require explicit re-review; rebase resets all decisions, preserves observations.
- F5-P1 PASS 300/300, F5-P2A PASS 313/313, F5-P2B offline 20/20; F5-P2C runtime HOLD. No bank financial UAT.
- F6/F7 remain specialized methods/domains, F8 governance, F9 business UAT, F10 hardening.
- Next: analyst usability assessment, validated defect register, specialized engine test against authorized tariff contracts.
- Oracle DML/DDL, workflow, posting, publication: unchanged and disabled.
