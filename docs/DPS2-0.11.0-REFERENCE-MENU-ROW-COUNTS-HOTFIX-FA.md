# DPS2 0.11.0 — نمایش تعداد رکورد جداول پایه چهار سپرده

## دامنه
صفحه:

`/#/four-deposits/reference-data`

## تغییر
- تعداد واقعی رکورد جدول متناظر هر فرم کنار عنوان همان فرم نمایش داده می‌شود.
- شمارش‌ها با یک درخواست تجمیعی به endpoint موجود `GET /api/v1/dashboard/counts` دریافت می‌شوند.
- مقدار صفر به‌صورت `۰` نمایش داده می‌شود.
- هنگام بارگذاری، badge مقدار `…` دارد.
- اگر سرویس شمارش در دسترس نباشد یا count یک resource موجود نباشد، badge مقدار `—` نشان می‌دهد و صفحه همچنان قابل استفاده است.
- هیچ DDL، Business Write یا API جدیدی اضافه نشده است.

## فایل‌های تغییرکرده
- `frontend/src/app/core/catalog/catalog.service.ts`
- `frontend/src/app/features/reference-menu/reference-menu.component.ts`
- `frontend/src/app/features/reference-menu/reference-menu.component.html`
- `frontend/src/app/features/reference-menu/reference-menu.component.scss`

## نکته Build
در محیط بسته‌سازی، نصب npm به علت timeout محیط ابزار کامل نشد و Angular CLI محلی در دسترس نبود؛ بنابراین Angular production build این Patch در همان محیط اجرا نشد. تغییرات به‌صورت ساختاری کنترل شده‌اند. Build اصلی با `build-production.cmd` در محیط پروژه انجام شود.
