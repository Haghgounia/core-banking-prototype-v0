DPS2 Account Operations R10O - Owner / Product Change Flow
==========================================================
Baseline: core-banking-prototype-v0-2026-10-04-1409
Cumulative: R10N + R10O

اصلاحات R10O:
1) تغییر محصول: فقط یک درخواست باز برای هر حساب مجاز است. چندکلیک همان مقصد Business Replay است و رکورد جدید ایجاد نمی‌کند؛ درخواست مقصد دیگر تا تعیین تکلیف درخواست باز مسدود است.
2) مالک حساب در Context حساب انتخاب‌شده و نمای 360 با نام/شناسه/نقش/درصد نمایش داده می‌شود.
3) نتایج جستجوی مشتری گزینه صریح «افزودن به‌عنوان شریک» دارد.
4) اگر مجموع مالکیت قبلی 100٪ باشد، کاربر مالک واگذارکننده سهم را انتخاب می‌کند؛ کاهش سهم مالک موجود و افزودن شریک در یک Transaction Backend انجام می‌شود.
5) Backend از رابطه مالک فعال تکراری و عبور مجموع مالکیت از 100٪ جلوگیری می‌کند.
6) تمام اصلاحات R10N نیز داخل Patch وجود دارد.

DDL یا Migration ندارد.

اجرا:
  patch-r10o\APPLY-DPS2-ACCOUNT-OPERATIONS-R10O.cmd D:\Projects\core-banking-prototype-v0

پس از PASS:
  cd /d D:\Projects\core-banking-prototype-v0
  build-production.cmd
