# DPS2 0.3.97 — Four Deposits Phase 1 QA

## Scope

این Release فاز اول منوی جدید **چهار سپرده** را بر اساس مدل Deposit Account Opening در Schema `DPS2` پیاده‌سازی می‌کند. دامنه شامل چهار خانواده حساب است: قرض‌الحسنه پس‌انداز، حساب جاری، سپرده کوتاه‌مدت و سپرده بلندمدت. گواهی سپرده از این bounded context خارج است.

## Source traceability

- `DPS2.txt` SHA-256: `22f9a41a1123124321b644209c3bc4af1a04144585b2fe45d72395c8da80eb3e`
- `Dps_Opn_14050622.xml` SHA-256: `ce95098e3257f4e8472b35cd6eba7cc52ef7f01bcfa5437f45458a6b4ed358e9`
- `Deposit_Account_Opening.html` SHA-256: `1c7469ff78f0acfaa276a7976337129086357d8bfe216d3031a4d9d4da953c44`
- baseline ZIP `core-banking-prototype-v0-2026-09-14-1248.zip` SHA-256: `a31801ac1b210db5470eb2ed28cf57fb7d080535301003d8cf57a15ba89ff46c`

## Delivered

- منوی سطح اول `چهار سپرده` در Sidebar.
- صفحه Landing برای چهار خانواده حساب سپرده.
- 59 فرم Reference Data از جداول `REF_DEP_OPEN_*`.
- 25 فرم Operational CRUD از جداول ایمن برای نگهداری عمومی `DEPOSIT_OPENING_*`.
- Wizard هفت‌مرحله‌ای افتتاح موردی که قرارداد Payload اصلی مدل Opening را تولید می‌کند.
- مسیرهای جداگانه برای Reference Data، Operational Forms و Single Opening.
- Schema configuration جدید `core-banking.schemas.deposit-opening=DPS2`.
- پشتیبانی generic CRUD از `UPDATED_BY`, `UPDATED_AT` و ورودی `TIMESTAMP`.

## Deliberate exclusions

این پنج جدول عمداً در Generic CRUD عملیاتی قرار نگرفته‌اند، چون ماهیت آن‌ها append-only یا change-controlled است و باید از سرویس دامنه مدیریت شوند:

- `DEPOSIT_OPENING_AUDIT_EVENT`
- `DEPOSIT_OPENING_AUDIT_FIELD_CHANGE`
- `DEPOSIT_OPENING_CHANGE_SET`
- `DEPOSIT_OPENING_SNAPSHOT`
- `DEPOSIT_OPENING_STATUS_HISTORY`

## Phase boundary

Wizard این فاز Payload عملیاتی و Draft محلی ایجاد می‌کند؛ API اتمیک Aggregate برای ایجاد/تغییر پرونده Opening و Materialization به Account-Level در فاز بعدی پیاده‌سازی می‌شود. فرم‌های فیزیکی DPS2 برای تست و مدیریت داده‌های جدول‌محور در این فاز در دسترس هستند.

## Validation

- `tools/verify-dps2-four-deposits.mjs` ساختار منو، routeها، 59 descriptor مرجع، 25 descriptor عملیاتی، چهار خانواده و 7 مرحله Wizard را کنترل می‌کند.
- Java/Angular build status باید از اجرای build همین Release ثبت شود و نباید از Release قبلی فرض شود.
