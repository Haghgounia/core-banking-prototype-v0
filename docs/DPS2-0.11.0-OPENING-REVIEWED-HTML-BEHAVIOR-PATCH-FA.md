# DPS2 0.11.0 - Deposit Opening Reviewed HTML Behavior Alignment Patch

## مبنا

- Base ZIP: `core-banking-prototype-v0-2026-09-30-1331.zip`
- Reference UI: `Deposit_Account_Opening_Operational_Final_Reviewed_2026-09-20_ReadOnly_Color_Aligned(2).html`
- Scope: فقط Opening Wizard؛ بدون DDL و بدون Migration.

## هدف

این Patch مغایرت‌های رفتاری مهم بین فرم Angular افتتاح سپرده و HTML مرجع Reviewed را رفع می‌کند. تمرکز فقط روی ظاهر نیست؛ ReadOnly/Editability، منبع داده، Auto-fill، دامنه انتخاب‌ها، کنترل داده‌های سیستمی و عدم تولید داده ساختگی هم بررسی شده است.

## اصلاحات اصلی

| حوزه | اصلاح |
|---|---|
| Product | `PRODUCT_VERSION_ID` از حالت ReadOnly خارج و به انتخاب واقعی نسخه فعال/معتبر/قابل Origination از Product Builder تبدیل شد. |
| Product Details | مشاهده مشخصات محصول و خلاصه Policy شامل حداقل مبلغ، کانال، دامنه مشتری، Media، KYC و شرایط اختصاصی اضافه/تکمیل شد. |
| Product Policy | حداقل مبلغ، کانال افتتاح، نوع مشتری، Withdrawal Media، Payment Instrument و Reward Program با محصول انتخابی کنترل می‌شوند. |
| Request Type | Bulk از فرم افتتاح تکی حذف شد؛ فقط Hand-off واقعی از پردازش گروهی می‌تواند `BULK` را وارد کند. |
| Party | نوع Party در جدول مالکان نمایش داده می‌شود و سازگاری نوع مالک با Policy محصول کنترل می‌شود. |
| Risk/Evidence | ارزیابی ریسک و Evidenceهای قابل استخراج از Party/CIF به‌صورت سیستمی/ReadOnly نگهداری می‌شوند. |
| Service | پیام Placeholder مربوط به Adapter حذف شد. پیشنهاد خدمت از Party اصلی، Contact Point معتبر، Preference و واحد افتتاح ساخته می‌شود. |
| Service Target | مقصد فنی Service از ورود دستی خارج و Hidden/System-derived شد؛ مقدار قابل نمایش ReadOnly است. |
| Service Override | پیشنهادهای سیستمی در حالت عادی قابل حذف مستقیم نیستند؛ تغییر فقط از مسیر Override انجام می‌شود. |
| Tax | Tax Profile از داده موجود Party/CIF خوانده می‌شود؛ Exemption Code/Document در صورت نبود Source واقعی ساخته نمی‌شود. |
| Pricing | Authority و Approval توسط Workflow کنترل می‌شوند و ورود دستی ندارند. |
| Reward | Program به Policy محصول محدود شده و Consent فقط در Presence یک Party اصلی واقعی تولید می‌شود. |
| Withdrawal Media | گزینه‌ها از Policy محصول انتخاب‌شده محدود می‌شوند، نه صرفاً Family عمومی. |
| Product Eligibility | کنترل Runtime/UI اکنون Channel، Minimum Amount، Party Type و تنظیمات وابسته به Product را هم بررسی می‌کند. |

## اصل عدم جعل Integration

HTML مرجع در بعضی نقاط Mock/Reference Data دارد. در این Patch هرجا Adapter واقعی پروژه وجود دارد از آن استفاده شده است (Product Builder و Party/CIF). هرجا Source واقعی در مدل فعلی وجود ندارد، مقدار ساختگی تولید نمی‌شود و وضعیت صریحاً به‌صورت unavailable/empty باقی می‌ماند.

به‌طور خاص، Tax Exemption Code/Document در `Party360.financialProfiles` منبع مستقل ندارد؛ بنابراین Patch آن را جعل نمی‌کند.

## Regression

Static guards اجراشده:

- `verify-dps2-opening-reviewed-alignment.mjs`: 128/128 PASS
- `verify-dps2-opening-operational-v5-phase10d.mjs`: 24/24 PASS
- `verify-dps2-opening-ui-reference-parity.mjs`: 70/70 PASS
- `verify-dps2-opening-final-ui-11.mjs`: 14/14 PASS
- `verify-dps2-opening-operational-v5-phase10.mjs`: 34/34 PASS
- `verify-dps2-opening-operational-v5-phase10b.mjs`: 30/30 PASS
- `verify-dps2-opening-v5-phase10e10f.mjs`: 35/35 PASS
- TypeScript transpile diagnostics: 0

مجموع Guardهای ثبت‌شده در این مجموعه: 335 PASS / 0 FAIL.

## Build

در محیط تولید Patch، `frontend/node_modules` در ZIP پایه وجود ندارد و Cache آفلاین همه وابستگی‌ها را ندارد؛ بنابراین Build کامل Angular در این محیط اجرا نشد. روی محیط پروژه پس از اعمال Patch اجرا شود:

```bat
node tools\verify-dps2-opening-reviewed-alignment.mjs
node tools\verify-dps2-opening-operational-v5-phase10d.mjs
build-production.cmd
```

## Database

این Patch هیچ DDL/SQL/Migration ندارد.
