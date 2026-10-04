# DPS2 Account Operations R10M — Maintenance Delta & No-op Protection

## Scope
این Patch منطق «نگهداری حساب سپرده / اطلاعات پایه و ویژگی‌ها» را اصلاح می‌کند و به‌صورت Cumulative شامل R10L نیز هست.

## UX changes
- عنوان حساب (`ACCOUNT_NAME`) در فرم نگهداری قابل ویرایش است.
- واحد نگهدارنده فقط‌خواندنی است و به‌صورت `کد شعبه — عنوان شعبه` از مرجع `dps-org-units / REF_ORG_UNIT_CODE` نمایش داده می‌شود.
- دکمه «ثبت تغییرات حساب» فقط زمانی فعال است که حداقل یک Delta واقعی در عنوان حساب، ویژگی انتخاب‌شده یا اطلاعات تماس وجود داشته باشد.
- اگر هیچ تغییری وجود نداشته باشد، هیچ Mutation API فراخوانی نمی‌شود.

## Backend safety
- `updateBasicInfo` پیش از UPDATE، مقدار Canonical جاری `ACCOUNT_NAME` و `ORG_UNIT_CODE` را با درخواست مقایسه می‌کند؛ در صورت برابری، No-op است و `RECORD_VERSION`/`DEPOSIT_ACCOUNT_SERVICING_HISTORY` تغییر نمی‌کند.
- Attribute Upsert قبل از Idempotency claim مقدار و نوع Attribute فعال را بررسی می‌کند؛ درخواست همسان No-op است و هیچ UPDATE یا Idempotency write جدید ایجاد نمی‌کند.
- Contact maintenance در Orchestrator فقط در صورت اختلاف واقعی پایان/ایجاد می‌شود.

## Database
هیچ DDL/Migration جدیدی لازم نیست.
