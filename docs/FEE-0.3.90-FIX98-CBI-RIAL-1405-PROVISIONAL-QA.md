# FEE 0.3.90 / FIX98 — CBI Rial Fee 1405 Provisional Versioned Import

## هدف
به‌روزرسانی داده‌های FEE بر اساس پیوست جدید «کارمزد خدمات بانکی ریالی» و فایل Excel ساختاری، بدون حذف یا overwrite تاریخچه تعرفه قبلی.

## منبع
- PDF: `پیوست (کارمزدهای ریالی).pdf` — 8 صفحه
- Excel: `کارمزدهای_ریالی_تفکیک_ساختاری_برای_درج_دیتابیس_v3_2026-09-10.xlsx`
- SHA256 Excel: `bbe79ea223fabbfcddd83a402a70bc87f9c3fb3ad3d0de01369155240e4bd15e`
- SHA256 PDF: `ea2e6a22e3b92b7ea6ef5da8cd6bfa20f918d0fc069c2e1bd46b89167a993e14`

> نامه/صفحه ابلاغ اصلی در اختیار پروژه نیست؛ بنابراین metadata مصوبه عمداً `PROVISIONAL` است.

## نتیجه استخراج
| مورد | تعداد |
|---|---:|
| تعرفه جدید | 152 |
| جزء محاسباتی منبع | 180 |
| سرفصل | 9 |
| Rule مرکب | 32 |
| Tier ارزیابی | 15 |
| نسخه قبلی غیرالکترونیکی برای آرشیو زمانی | 156 |
| نسخه قبلی الکترونیکی که حفظ می‌شود | 73 |

## سیاست آرشیو
Physical Archive یا DELETE انجام نمی‌شود. برای 156 نسخه غیرالکترونیکی CBI-1404:
- `FEE_DEFINITION_VERSION.STATUS_CODE = SUPERSEDED`
- `FEE_DEFINITION_VERSION.EFFECTIVE_TO = 2026-09-09`
- `FEE_CALCULATION_RULE.EFFECTIVE_TO = 2026-09-09`

خود `FEE_DEFINITION` و Ruleهای تاریخی حذف یا غیرفعال فیزیکی نمی‌شوند تا بازیابی تاریخی، Audit و Reversal ممکن باشد.

چهار گروه تعرفه الکترونیکی قدیم از این Scope خارج هستند و نباید بسته شوند:
- `CBI_ELECTRONIC_SERVICE_FEE_GROUP`
- `CBI_ELECTRONIC_LC_RIAL_FEE_GROUP`
- `CBI_ELECTRONIC_GUARANTEE_RIAL_FEE_GROUP`
- `CBI_ELECTRONIC_BILL_FEE_GROUP`

## Metadata موقت مصوبه
| فیلد | مقدار |
|---|---|
| SOURCE_CODE | `CBI_RIAL_FEE_1405_PROVISIONAL` |
| CIRCULAR_NO | `PROVISIONAL-RIAL-FEE-1405` |
| STATUS_CODE | `PROVISIONAL` |
| POLICY_CODE | `CBI_RIAL_BANKING_1405_PROVISIONAL` |
| POLICY_VERSION_NO | `1405.P1` |
| EFFECTIVE_FROM | `2026-09-10` |

تاریخ فوق رسمی نیست؛ صرفاً برای اجرای Prototype از تاریخ فایل ساختاری استفاده شده است.

## نگاشت محاسبات
- `FIXED_AMOUNT` ساده → `FIXED` یا `PER_UNIT`
- `RATE` ساده → `PERCENTAGE*` یا `ANNUALIZED_PERCENTAGE`
- `NO_FEE` ساده → `FIXED` با مبلغ صفر
- قواعد چندجزئی → `COMPOSITE`
- `REFERENCE`, `FORMULA`, `REFUND_FORMULA` و موارد غیرقابل محاسبه قطعی → `EXTERNAL_VALUE` یا جزء External در Rule
- درصدها Normalize می‌شوند: مثال `0.5% → 0.005`
- تمام اجزای منبع در `FEE_RULE_COMPONENT` با شناسه `SRC:<component-id>:<rule-type>` نگهداری می‌شوند.
- پنج تعرفه ارزیابی `7-1`, `7-2`, `7-3`, `7-4`, `7-7` هرکدام سه Tier ساختاری دارند.

## فایل‌های اجرایی
- `database/oracle/fee/install-cbi-rial-fee-1405-provisional.sql`
- `database/oracle/fee/cbi-rial-1405-provisional/00-install-cbi-rial-fee-1405-provisional.sql`
- `01-import-cbi-rial-fee-1405-provisional.sql`
- `02-verify-cbi-rial-fee-1405-provisional.sql`
- `03-finalize-official-reference-template.sql`
- CSVهای definitions/components/delta و manifest JSON

## کنترل‌های Acceptance
1. Source موقت دقیقاً یک رکورد و `PROVISIONAL` باشد.
2. 9 Feature جدید فعال باشد.
3. 152 `FEE_DEFINITION` جدید ایجاد شود.
4. 152 Version و 152 Calculation Rule فعال وجود داشته باشد.
5. 180 Source Component و 15 Tier ارزیابی ثبت شود.
6. 156 Version قبلی غیرالکترونیکی `SUPERSEDED` شود.
7. 73 Version الکترونیکی قبلی همچنان پوشش زمانی بعد از 2026-09-10 داشته باشند.
8. Duplicate `FEE_CODE` و نرخ Normalize نامعتبر وجود نداشته باشد.

## محدودیت
تا زمان دریافت نامه رسمی بانک مرکزی، شماره بخشنامه و تاریخ رسمی را نباید از این داده Prototype به‌عنوان مستند مقرراتی استفاده کرد.
