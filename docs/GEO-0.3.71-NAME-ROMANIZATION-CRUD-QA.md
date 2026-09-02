# GEO 0.3.71 — Name Romanization CRUD

## دامنه تغییر

دو جدول واژه‌نامه نام در Schema `GEO` به موتور عمومی Reference CRUD سامانه متصل شدند:

- `GEO.NAME_ROMANIZATION_DICTIONARY`
- `GEO.NAME_AFFIX_DICTIONARY`

## مسیر UI

`اطلاعات پایه` → `اطلاعات پایه عمومی` → گروه `اطلاعات عمومی`

فرم‌های جدید:

1. **واژه‌نامه رومن‌نویسی نام‌ها** (`name-romanization-dictionary`)
2. **واژه‌نامه پیشوند و پسوند نام** (`name-affix-dictionary`)

## قابلیت‌ها

- جستجو، صفحه‌بندی و مرتب‌سازی
- ایجاد، ویرایش و حذف
- فیلتر فعال/غیرفعال برای واژه‌نامه اصلی
- Combo برای کدهای Governance، Method، Entry Type، Affix Type و Position
- Switch برای Auto-fill / Auto-apply / Context-sensitive
- Normalization خودکار فارسی در UI و Backend
- Optimistic Lock با `RECORD_VERSION`
- نمایش اطلاعات Audit و Evidence در بخش اطلاعات سیستمی

## قواعد Normalization

- `ي/ى → ی`
- `ك → ک`
- `ة/ۀ → ه`
- حذف کشیده `ـ`
- تبدیل نیم‌فاصله به فاصله
- یکسان‌سازی فاصله‌های متوالی

## کنترل‌های پیشنهادی QA

- ایجاد `محمد` و کنترل تولید `NORMALIZED_PERSIAN_NAME=محمد`
- ایجاد مقدار دارای حروف عربی مثل `سيد` و کنترل نرمال‌شدن به `سید`
- انتخاب Governance Status از Combo
- تغییر `AUTO_FILL_ALLOWED`
- ویرایش هم‌زمان و کنترل `RECORD_VERSION`
- ایجاد Affix مثل `خانوم → Khanom` و کنترل Context-sensitive
