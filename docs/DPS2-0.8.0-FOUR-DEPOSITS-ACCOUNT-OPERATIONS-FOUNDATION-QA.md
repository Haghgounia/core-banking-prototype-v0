# DPS2 0.8.0 — Four Deposits Account Operations Foundation QA

## Scope

این فاز نخستین گام پس از تکمیل Deposit Account Opening است و Bounded Context مستقل **Deposit Account Operations** را فعال می‌کند. چون فایل HTML/EA ارسالی کاربر برای این بخش مدل کامل Servicing ارائه نمی‌کند، این Release عمداً فقط از Contract قطعی موجود در Phase 4 استفاده می‌کند و دیتامدل جدید برای Hold/Closure/Dormancy/Posting اختراع نمی‌کند.

## Source of Truth فعلی

- `DPS2.DEPOSIT_ACCOUNT`
- `DPS2.DEPOSIT_ACCOUNT_LIFECYCLE_EVENT`
- Integration Reference به `DPS2.DEPOSIT_OPENING_REQUEST`
- مالکان تاریخی از `DPS2.DEPOSIT_OPENING_PARTY`
- Product Family از `PDL.PRODUCT_VERSION -> PDL.PRODUCT`

## Backend

Endpoint مستقل Account Operations:

- `GET /api/v1/deposit-accounts`
- `GET /api/v1/deposit-accounts/{accountId}`

فیلترهای جست‌وجو:

- `accountNo`
- `status`
- `openingRequestId`
- `partyId`
- `productFamilyCode`
- `offset / limit`

API این فاز **Read-Only** است. Repository هیچ `INSERT/UPDATE/DELETE/MERGE` ندارد.

## Angular

Route جدید:

`/four-deposits/account-operations`

Workspace شامل:

1. جست‌وجوی حساب سپرده؛
2. فیلتر چهار خانواده؛
3. Account 360؛
4. Opening Request linkage؛
5. Party/مالک‌های Opening؛
6. Lifecycle Timeline از CREATE/ACTIVATE؛
7. وضعیت و Record Version.

## Architectural Boundary

عملیات زیر عمداً هنوز ایجاد نشده‌اند:

- Hold / Block
- Reactivation
- Closure
- Dormancy
- Profit Accrual / Profit Posting
- Transaction Posting
- Balance Ledger

این موارد فقط پس از دریافت/استخراج مدل قطعی Servicing به فازهای بعدی اضافه خواهند شد.

## Database

Migration جدید ندارد؛ این Release فقط از جداول قطعی موجود می‌خواند.
