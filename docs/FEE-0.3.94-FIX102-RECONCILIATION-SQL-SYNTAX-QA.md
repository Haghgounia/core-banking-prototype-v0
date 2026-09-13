# FEE 0.3.94 / FIX102 — Reconciliation SQL Syntax QA

## مشکل مشاهده‌شده
اجرای `04-reconcile-cbi-rial-fee-1405-provisional.sql` در Oracle با `ORA-00936` و `PLS-00103` متوقف شد. چهار literal تولیدشده توسط Generator فاقد Quote معتبر Oracle بودند:

- `c.REFERENCE_CODE LIKE SRC:%`
- `p_fee_code||#||p_sequence`
- `p_fee_code||#||p_input_code`
- `p_fee_code||#TIER||p_tier_no`

## علت ریشه‌ای
در Generator پایتون، SQL fragmentها با single-quoted Python literals و الگوی `''...''` ساخته شده بودند. Python این بخش‌ها را به‌صورت adjacent string literal تفسیر کرد و Quoteهای موردنیاز SQL در خروجی نهایی حذف شدند.

## اصلاح
خروجی Oracle اکنون شامل موارد صحیح زیر است:

```sql
c.REFERENCE_CODE LIKE 'SRC:%'
p_fee_code||'#'||p_sequence
p_fee_code||'#'||p_input_code
p_fee_code||'#TIER'||p_tier_no
```

Generator نیز با Python double-quoted strings اصلاح شد تا Quoteهای SQL صریحاً حفظ شوند.

## Regression Guard
`tools/verify-cbi-rial-fee-1405-provisional.mjs` اکنون هم حضور literalهای صحیح و هم نبود چهار الگوی malformed قبلی را کنترل می‌کند. Generator نیز برای وجود fragmentهای صحیح بررسی می‌شود.

## بازتولید از Source
Generator اصلاح‌شده با همان Sourceها اجرا شد:

- XLSX SHA256: `bbe79ea223fabbfcddd83a402a70bc87f9c3fb3ad3d0de01369155240e4bd15e`
- PDF SHA256: `ea2e6a22e3b92b7ea6ef5da8cd6bfa20f918d0fc069c2e1bd46b89167a993e14`

نتیجه Generator همچنان قرارداد قبلی را حفظ کرد: 152 تعرفه، 180 Component، 15 Tier، 156 نسخه قبلی آرشیوشونده و 73 تعرفه الکترونیکی حفظ‌شده. فایل Generated `04` با فایل Release تطبیق کامل داشت.

## اثر دیتابیسی
این Fix هیچ DDL/DML جدیدی ندارد. خطای FIX101 در مرحله Parse/Compile PL/SQL رخ داده و Reconciliation نیز ذاتاً Read-only است. بنابراین `ROLLBACK` نمایش‌داده‌شده داده‌های Commit‌شده FIX98 را تغییر نداده است.

## Acceptance
روی دیتابیس موجود فقط فایل اصلاح‌شده `04-reconcile-cbi-rial-fee-1405-provisional.sql` اجرا شود. نتیجه صحیح باید `mismatches=0` و پیام `CBI Rial Fee 1405 PROVISIONAL source-to-Oracle reconciliation OK.` باشد.
