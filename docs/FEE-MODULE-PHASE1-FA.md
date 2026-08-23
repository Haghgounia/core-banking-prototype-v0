# ماژول مستقل کارمزد — Phase 1

نسخه: `0.3.23-prototype-fee-p1`

## هدف
این Increment اولین نسخه قابل اجرای Prototype مستقل Fee است. هیچ وابستگی Runtime یا FK بین Schema `FEE` و `CIF`/`DPS`/Account/Accounting ایجاد نشده است.

## UI
صفحه `/fee` تمام گروه‌های اصلی ورود اطلاعات را در ۱۲ Tab پوشش می‌دهد: اطلاعات پایه، نسخه، Applicability، Calculation و Tier، Currency، Timing/Collection، Discount/Waiver، Allocation، Posting، Arrangement، Simulator، Transaction/Reversal.

## Backend
- `GET /api/v1/fees/prototype-metadata`
- `POST /api/v1/fees/calculate`

## Database
Baseline فیزیکی در `database/oracle/fee` قرار دارد. این ساختار BIAN-aligned است ولی «BIAN physical schema» ادعا نمی‌شود.

## محدودیت این Increment
Persistence فرم Definition هنوز به Backend متصل نشده است؛ هدف P1 تثبیت Coverage فرم‌ها، مرزبندی Domain و Calculator قابل اجراست. Increment بعدی CRUD/Version persistence را فعال می‌کند.
