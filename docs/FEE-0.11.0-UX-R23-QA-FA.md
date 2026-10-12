# گزارش R2.3

مبنای آزمون: سورس ۲۱:۵۳ با FEE UX R2.2؛ تنها چهار فایل FEE و آزمون R2.2 به‌روزرسانی شده‌اند.

- شناسایی DDL: `FEE_FEATURE_ID` یک FK واقعی به `FEE_FEATURE` دارد؛ `CATEGORY_CODE` و `CLASSIFICATION_CODE` ستون آزادند و FK/CHECK ندارند. پیشنهاد کدها فقط از رکوردهای موجود FEE گرفته می‌شود.
- آزمون‌های نرم‌افزاری: نصب/عدم‌تداخل و Rollback با SHA؛ TypeScript transpile؛ آزمون R2.3، R2.2، R2.1 و سایر Regressionهای FEE؛ آزمون FEE2 و Product Builder R29؛ کنترل Release Layout.
- آزمایش عملی مرورگر و Build production/Oracle در محیط بررسی محلی در دسترس نبوده و باید پس از نصب انجام شود.
