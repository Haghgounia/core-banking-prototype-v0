# PDL 0.11.0 — R10U-HF5 Currency Reference & Business Error UX

## مسئله
در محیط UAT، `DPS.REF_DEFAULT_CURRENCY_CODE` چهار رکورد فعال داشت اما مقدار `CODE` آنها به‌ترتیب `1/2/3/4` بود، در حالی که قرارداد Product Builder و Deposit Runtime از کد کسب‌وکاری ارز (`IRR/USD/EUR/AED`) استفاده می‌کند. نتیجه این بود که UI مقدار `IRR` را ارسال می‌کرد ولی Backend، با رجوع به Reference Data، آن را نامعتبر تشخیص می‌داد.

هم‌زمان Product Workspace متن `ProblemDetail.detail` را استخراج نمی‌کرد و پیام دقیق Backend به «خطای پیش‌بینی‌نشده» تبدیل می‌شد.

## تصمیم
- ستون `CODE` در `DPS.REF_DEFAULT_CURRENCY_CODE` کد کسب‌وکاری است و برای این چهار ارز به `IRR/USD/EUR/AED` reconcile می‌شود؛ شناسه‌های فنی (`DEFAULT_CURRENCY_ID`) تغییر نمی‌کنند.
- Migration صرفاً DML و idempotent است؛ DDL و XML ندارد.
- اگر `PDL.PRODUCT.DEFAULT_CURRENCY_CODE` مقدار legacy عددی داشته باشد، همان Migration آن را به ISO تبدیل می‌کند.
- ComboBox ارز در Product Workspace دیگر hard-coded نیست و از Descriptor/Reference Backend پر می‌شود.
- اگر Reference ارز خراب یا فاقد کد سه‌حرفی باشد، ذخیره Product مسدود و پیام روشن نمایش داده می‌شود.
- `ProblemDetail.detail/message/title` و خطاهای فیلدی در Product Workspace به کاربر نمایش داده می‌شوند.

## اجرای Migration
```bat
tools\run-oracle-sql.cmd database\oracle\dps\migrations\0.11.0-r10u-hf5-default-currency-code-reconciliation.sql
```

انتظار بعد از اجرا:
```text
IRR  ریال ایران
USD  دلار آمریکا
EUR  یورو
AED  درهم امارات متحده عربی
PDL_R10U_HF5_CURRENCY_REFERENCE_PASS
```

## نکته
Migration اهلیت R10U را دوباره اجرا نکنید. HF5 فقط Reference ارز و UX پیام خطا را reconcile می‌کند.
