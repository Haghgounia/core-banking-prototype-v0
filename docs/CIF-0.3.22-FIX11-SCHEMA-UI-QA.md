# CIF 0.3.22 Fix 11 - Schema / UI QA

مبنای Schema: `CIF-tables5.xlsx`

- جدول‌ها: 146
- ستون‌ها: 1795
- Reference Tableها: 98
- Operational Tableها: 48
- FKها: 44
- Indexها: 328 (همه VALID)
- Check Constraintها: 504 (همه ENABLED / VALIDATED)

## Driftهای اصلاح‌شده

- `PARTY_ROLE`: 21 ستون جاری؛ سه ستون قدیمی `CREATED_DATE`, `LAST_MODIFIED_BY`, `LAST_MODIFIED_DATE` در برنامه استفاده نمی‌شوند.
- `PARTY_RISK_ASSESSMENT`: 17 ستون جاری؛ سه ستون قدیمی بالا از INSERT/UPDATE حذف شدند.
- `SCREENING_RESULT`: 20 ستون جاری؛ `CREATED_DATE` از INSERT حذف شد.

## UI فارسی

در 12 فرم عملیاتی CIF، عنوان و نمایش پیش‌فرض فیلدها بازبینی شد. کدهای مرجع در مقدار فرم/API باقی می‌مانند اما در Comboها نمایش داده نمی‌شوند.

- Material labels: 242
- Native field labels: 126
- Material options: 191
- Searchable combo labels: 43
- Latin field-facing labels/options/placeholders/hints found after cleanup: 0
