# DPS2 Opening Fund Allocation Lifecycle Hotfix

این Hotfix ناسازگاری بین UI/Backend و Constraint واقعی `DPS2.DEPOSIT_OPENING_FUND_ALLOC.ALLOCATION_STATUS_CODE` را اصلاح می‌کند.

- برنامه تخصیص قبل از Settlement با `PENDING` ذخیره می‌شود.
- در Settlement، Allocationهای `PENDING` پس از کنترل پوشش تعهدات و عدم تجاوز از مبلغ منبع به `POSTED` تبدیل می‌شوند.
- Allocationهای غیر `PENDING` در Settlement ناقص همچنان به عنوان خطر Double Posting رد می‌شوند.
- مسیر سازگاری قبلی که در نبود Allocation برنامه‌ریزی‌شده، Backend Allocationهای `POSTED` را ایجاد می‌کند حفظ شده است.
- Aggregate Validator اکنون Status نامعتبر را قبل از رسیدن به Constraint Oracle رد می‌کند.

No database migration is required.
