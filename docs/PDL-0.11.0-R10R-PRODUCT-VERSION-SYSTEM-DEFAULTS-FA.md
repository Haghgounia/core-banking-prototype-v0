# PDL 0.11.0 / R10R — Product Version System Defaults

## هدف
اصلاح فرم «ثبت نسخه جدید» در محصول‌ساز یکپارچه با تمرکز بر مقادیر سیستمی و نمایش فارسی.

## تغییرات
- «نسخه مبدأ» دارای Label و Caption صریح شد.
- `VERSION_NO` دیگر ورودی کاربر نیست؛ UI فقط شماره پیشنهادی را نمایش می‌دهد و Backend هنگام Insert زیر Lock دیتابیس `MAX(VERSION_NO)+1` را مجدداً محاسبه می‌کند.
- `VALID_FROM` برای نسخه جدید از تاریخ جاری دیتابیس (`SYSDATE`) مقدار اولیه می‌گیرد و همچنان قابل ویرایش است.
- گزینه‌های `ORIGINATION_STATUS_CODE` و `SERVICING_STATUS_CODE` فقط با عنوان فارسی نمایش داده می‌شوند؛ Code فنی در Model باقی می‌ماند.
- «زمان تصویب» در فرم جدید، زمان جاری سامانه را به‌صورت فقط‌خواندنی نمایش می‌دهد. مقدار نهایی `APPROVED_AT` فقط هنگام ورود Version به وضعیت `APPROVED` توسط Backend از ساعت دیتابیس ثبت می‌شود.
- `APPROVED_BY` فقط‌خواندنی و System-managed است. مقدار نهایی از `X-User-Name` درخواست تصویب گرفته می‌شود. اگر هویت ارسال نشود، Prototype از `prototype-ui` استفاده می‌کند که در UI با عنوان «کاربر پیش‌فرض محصول‌ساز» نمایش داده می‌شود.
- Client نمی‌تواند `VERSION_NO`, `APPROVED_AT`, `APPROVED_BY` را با Payload دلخواه Override کند.

## Database
DDL/Migration جدید ندارد.

## QA
`node tools/verify-pdl-product-version-r10r.mjs`
