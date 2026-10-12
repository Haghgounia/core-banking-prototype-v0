# گزارش QA مستقل — FEE UX R3-B.1

- مبنا: سورس R3-B روی نسخه 21:53 پروژه به‌همراه R2.3 و NAV-R22.1
- محدوده: سه فایل frontend FEE و چهار Verifier برای پذیرش overlay امضاشده
- هیچ تغییری در backend/Oracle/Product Builder/FEE2/routes انجام نمی‌شود
- موارد بررسی: عنوان مخصوص «مرحله اخذ»، نام فارسی قاعده/مؤلفه، Null مجاز، تفکیک موفق‌بودن فهرست خالی از خطای lookup، لینک با تعریف و نسخه درست، کنترل وابستگی سرور R3-B
- آزمون‌های هدف: `verify-fee-r3b1`, `verify-fee-r3b`، `verify-fee-r3a`، `verify-fee-ux-r23`، `verify-fee-ux-r22`، `verify-fee2-studio`، `verify-pdl-r29-finalization`، `verify-release-layout`
- پس از اجرای آزمون باید وضعیت‌های اجراشده و موارد باقی‌مانده در خروجی همین Patch گزارش شوند؛ **در نبود Build واقعی و Oracle UAT، آنها PASS اعلام نمی‌شوند.**
