DPS2 Account Operations R10N
============================
Baseline: core-banking-prototype-v0-2026-10-04-1409

اصلاحات:
1) فارسی‌سازی Labelهای حساب‌ها، نمای 360، مانده/قابل برداشت و نمایش دفتر معین.
2) تثبیت شناسه و نام مشتری پس از انتخاب مالک/صاحب حساب.
3) جلوگیری از عبور مجموع درصد مالکیت فعال از 100 درصد در UI و Backend.
4) تبدیل «مرجع الزام / تأیید» محدودیت تراکنش از Edit Box به Selector کنترل‌شده.
5) اصلاح فرآیند بستن/بازگشایی: علت‌های معتبر وابسته به نوع بستن، الزام مقصد تسویه برای مانده مثبت، بارگذاری مستقل حساب‌های مقصد و پیام محلی عملیات/خطا.

DDL یا Migration ندارد.

روش اجرا از هر مسیر:
  APPLY-DPS2-ACCOUNT-OPERATIONS-R10N.cmd D:\Projects\core-banking-prototype-v0

اگر Patch را داخل ریشه پروژه Extract کرده‌اید نیز مشکلی نیست؛ Installer خودش به Root کپی نمی‌شود.
پس از PASS شدن Patch:
  cd /d D:\Projects\core-banking-prototype-v0
  build-production.cmd
