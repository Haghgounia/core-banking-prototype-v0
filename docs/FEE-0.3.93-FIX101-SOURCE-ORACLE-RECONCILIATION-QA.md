# FEE 0.3.93 / FIX101 — Source-to-Oracle Reconciliation QA

## هدف
این Fix هیچ تعرفه‌ای را تغییر نمی‌دهد. هدف آن اثبات تطبیق Business Content مصوبه موقت کارمزد خدمات بانکی ریالی ۱۴۰۵ با داده ذخیره‌شده در Oracle است.

## منبع کنترل
- Workbook SHA256: `bbe79ea223fabbfcddd83a402a70bc87f9c3fb3ad3d0de01369155240e4bd15e`
- PDF SHA256: `ea2e6a22e3b92b7ea6ef5da8cd6bfa20f918d0fc069c2e1bd46b89167a993e14`
- Classification: `CBI_1405_RIAL_PROVISIONAL`
- Regulatory Source: `CBI_RIAL_FEE_1405_PROVISIONAL`
- Version: `1405.P1`

## Artifact اصلی
`database/oracle/fee/cbi-rial-1405-provisional/04-reconcile-cbi-rial-fee-1405-provisional.sql`

اسکریپت Read-only است و هیچ INSERT/UPDATE/DELETE/MERGE/DDL اجرا نمی‌کند.

## دامنه Reconciliation
| کنترل | تعداد مورد انتظار |
|---|---:|
| Tariff Contract | 152 |
| Source Component | 180 |
| Executable Input Definition | 66 |
| Appraisal Tier | 15 |
| Mismatch | 0 |

برای هر تعرفه موارد زیر کنترل می‌شود:
- Fee Code و Regulatory Tariff Code
- Feature و Category
- نام فارسی تعرفه
- Policy / Regulatory Source
- Version / Status / Effective Date
- `CONFIG_HASH` استخراج‌شده از Source
- Calculation Strategy و Basis
- Fixed Amount / Rate / Min / Max / Period
- Currency / Active Flag
- تعداد Source Componentها

برای هر یک از 180 جزء منبع نیز `SEQUENCE_NO`، `NODE_TYPE_CODE`، `CONSTANT_NUMBER`، `CONSTANT_TEXT`، `REFERENCE_CODE` و Description مقایسه می‌شوند.

برای 66 Input اجرایی، `INPUT_CODE`، نام، نوع داده، Unit، Mandatory و Display Order کنترل می‌شود. برای 15 Tier ارزیابی نیز بازه، Strategy، Fixed/Rate، Min/Max و تاریخ اعتبار تطبیق داده می‌شود.

## رفتار در اختلاف
هر اختلاف به شکل زیر چاپ می‌شود:

`[MISMATCH] <scope> <key> <field> expected=<...> actual=<...>`

در صورت وجود حتی یک اختلاف، اسکریپت با `ORA-20260` Fail می‌شود.

## خروجی موفق مورد انتظار
```text
tariffs_checked=152 expected=152
components_checked=180 expected=180
inputs_checked=66 expected=66
tiers_checked=15 expected=15
mismatches=0 expected=0
CBI Rial Fee 1405 PROVISIONAL source-to-Oracle reconciliation OK.
```

## Integration با Installer
از FIX101، `00-install-cbi-rial-fee-1405-provisional.sql` قبل از COMMIT این ترتیب را اجرا می‌کند:

`Import -> Structural Verify -> Source-to-Oracle Reconciliation -> COMMIT`

در نتیجه نصب جدید در صورت اختلاف Business Value قبل از Commit متوقف می‌شود.

## Database Change
- DDL جدید: ندارد
- DML جدید: ندارد
- اجرای مجدد Import برای دیتابیس موجود: لازم نیست
- برای دیتابیس موجود فقط اسکریپت `04-reconcile...sql` اجرا شود.
