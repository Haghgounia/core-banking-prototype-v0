# EDU - نمایش عنوان رشته و مقطع در Grid مقاطع معتبر هر رشته

در فرم `مقاطع معتبر هر رشته` (`edu-education-field-levels`) ستون‌های Lookup زیر در Grid دیگر شناسه عددی خام نمایش نمی‌دهند:

- `EDUCATION_FIELDS_ID` -> عنوان فارسی رشته/گرایش از `EDU.EDUCATION_FIELDS`
- `EDUCATION_LEVELS_ID` -> عنوان فارسی مقطع از `EDU.EDUCATION_LEVELS`

این اصلاح در Renderer عمومی Reference Data انجام شده است تا Lookupهای Grid در صورت داشتن `lookupResource` با Label نمایش داده شوند. مقدار ذخیره‌شده و Payload همچنان شناسه عددی FK است و فقط Presentation تغییر کرده است.

## QA

1. مسیر `#/reference-data/edu-education-field-levels` را باز کنید.
2. ستون «رشته تحصیلی» باید عنوان رشته را نشان دهد، نه عدد.
3. ستون «مقطع تحصیلی» باید عنوان مقطع را نشان دهد، نه عدد.
4. فرم افزودن/ویرایش همچنان شناسه FK را در Backend ذخیره می‌کند.
