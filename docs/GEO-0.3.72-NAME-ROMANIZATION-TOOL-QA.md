# GEO 0.3.72 — Name Romanization Tool QA

## هدف
افزودن فرم مستقل برای ورود نام فارسی و نمایش معادل انگلیسی با استفاده مستقیم از Functionهای Oracle موجود در Schema GEO.

## Backend
- Endpoint: `POST /api/v1/name-romanization/resolve`
- ورودی: `{ "persianName": "محمد" }`
- Oracle calls: `GEO.FN_NORMALIZE_NAME(?)` و `GEO.FN_ROMANIZE_NAME(?)`
- `FN_ROMANIZE_NAME` فقط نتیجه مجاز برای Auto-fill را برمی‌گرداند؛ NULL به‌معنای نبود خروجی Auto-fill تأییدشده است.

## Frontend
- Route: `/reference-data/general/name-romanization-tool`
- Menu: اطلاعات پایه > اطلاعات پایه عمومی > اطلاعات عمومی > تبدیل نام فارسی به انگلیسی
- نمایش سه حالت: resolved، governance review، service/database error.

## Regression
- CRUDهای `NAME_ROMANIZATION_DICTIONARY` و `NAME_AFFIX_DICTIONARY` بدون تغییر باقی می‌مانند.
- ابزار جدید هیچ داده‌ای را Insert/Update نمی‌کند و Read-only است.
