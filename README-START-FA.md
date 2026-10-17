# R1-D5 | Posting / Cash Deposit & Withdrawal / GL Journal

## هدف
از نسخه مبنا `core-banking-prototype-v0-2026-10-17-1443.zip`، اجرای اختیاری و **فقط آزمایشی** Posting در همان تراکنش Oracle ثبت مالی DPS2 برای دو نوع `CASH_DEPOSIT` و `CASH_WITHDRAWAL` اضافه می‌شود. پس از موفقیت عملیات مالی، به ازای هر تراکنش:

- ۱ دستور، ۱ طرح، ۱ Execution و ۱ Receipt در `PST`
- ۱ Ledger Transaction، ۱ Movement دوطرفه و به‌روزرسانی Position Balance در `LGR`
- ۱ Fact، ۱ Accounting Transaction و دقیقاً ۲ Entry متوازن در `ACC`

ثبت می‌شود. هر خطا در این مسیر موجب Rollback **تمام تغییرات همان تراکنش** شامل DPS2 خواهد شد. Kafka برای ثبت سند لازم نیست.

**مهم**: این بسته موتور کامل Formance نیست؛ نخستین vertical slice مبتنی بر الگوی Formance است. هیچ داده/کدینگ بانکی حدس زده نشده؛ تنظیمات منتشرشده و مصوب پیش‌نیاز هستند. تعمیم به Transfer، Fee، Profit و Reversal در این نسخه مجاز نیست. اجرای DML یا migration در Oracle از این محیط انجام نشده است.

## نصب (بدون فعال‌سازی Posting)

1. از پروژه محلی backup تهیه کنید. اعتبار بسته فقط برای ZIP مبنای **1443** بررسی شده است. اگر سورس جدیدتری دارید، فایل‌ها را کورکورانه جایگزین نکنید.
2. ZIP را استخراج و پوشه‌های `backend`، `tools`، `database`، `docs` و `config` را با ریشه پروژه ادغام کنید. تنها فایل‌های موجود جایگزین‌شده عبارت‌اند از:
   - `DepositTransactionService.java`
   - `DepositTransactionRepository.java`
3. از ریشه پروژه، دستور `node tools\posting\verify-posting-r1d5.mjs` را اجرا کنید.
4. Build و Regression معمول پروژه را در محیط توسعه اجرا کنید (`build-production.cmd` و تست‌های Maven). اسکریپت مذکور تنها قرارداد سورس و Java Policy را بررسی می‌کند و اثبات DB Integration نیست.
5. **تنظیمات اصلی `application.yml` تغییر نکرده است.** سرویس جدید با مقدار پیش‌فرض **غیرفعال** است.

## پیش‌نیاز فعال‌سازی آزمایشی (UAT Database Only)

- محیط Oracle ایزوله و مورد تأیید تیم پروژه، با دسترسی نوشتنی مجاز به جداول موردنیاز `DPS2`, `PST`, `LGR`, `ACC`؛ نه Oracle فعلی بدون مجوز.
- اجرای اسکریپت صرفاً خواندنی `database/oracle/posting/R1D5-POSTING-READINESS-READONLY.sql`.
- تنظیم واقعیِ فعال و منحصربه‌فرد Book، Ledger، Chart، Fiscal Period باز، Currency Asset، Rule/Template/Mapping منتشرشده، Maker-Checker Approval و Account/GL Bindings؛ **هیچ Seed خودکاری ارائه یا اجرا نمی‌شود.**
- محیط تست باید از هر محیط عملیاتی/حسابداری واقعی جدا باشد. موجودی افتتاحیه LGR باید پیش از برداشت با DPS2 وفق داده شود؛ راحت‌ترین آزمون، واریز و سپس برداشت از همان حساب آزمایشی تازه است.
- فایل `config/posting-cash-UAT-EXAMPLE-DO-NOT-ACTIVATE.yml` فقط الگوی پیکربندی است؛ آن را با مقادیر واقعی محیط آزمایشی تکمیل کنید. فقط بعد از اخذ مجوز `enabled: true` را تنظیم و آن را به Spring Boot از راه یک External Config ایزوله معرفی کنید.
- به داده‌های Master بانک (GL/Charts/Rules) در این بسته تغییری اعمال نمی‌شود. نقشه GL نقش‌ها باید قبل از تست توسط خبره حسابداری تأیید شود.

## API آزمون

از API موجود تراکنش‌های سپرده استفاده کنید:

- POST `.../deposit-accounts/{accountId}/transactions` (Initiate)
- POST `.../deposit-accounts/{accountId}/transactions/{transactionId}/validate`
- POST `.../deposit-accounts/{accountId}/transactions/{transactionId}/authorize` (با Maker/Checker متفاوت)
- POST `.../deposit-accounts/{accountId}/transactions/{transactionId}/post` (بدون تغییر در Contract قبلی)

برای cash، مقدار `cashManagementTxnRef` و `cashDeskCode` باید معتبر و قابل نگاشت باشند. هر مرحله X-Idempotency-Key منحصربه‌فرد نیاز دارد. لطفاً از شناسه‌های واریز/برداشت واقعی یا اطلاعات شخصی مشتری استفاده نکنید.

پس از موفقیت، سند با endpoint جدید فقط‌خواندنی قابل مشاهده است:

`GET http://localhost:8091/api/v1/deal-posting/cash/deposit-transactions/{transactionId}/journal`

Header: `X-Posting-Review-Token: <private-review-token>` (حداقل ۱۶ کاراکتر؛ مقدار مثال قابل استفاده نیست).

اجرای SQL فقط‌خواندنی `R1D5-CASH-JOURNAL-EVIDENCE-READONLY.sql` با شناسه تراکنش DPS2، شواهد PST/LGR/ACC و تراز ۲ قلم را نشان می‌دهد.

## معیارهای توقف / عدم پذیرش

- Book/GL/Period/Rule/Mapping/Template/Binding ناموجود، مبهم، تاریخ‌گذشته یا نامعتبر => خطا و Rollback کامل.
- برداشت با مانده موقعیتی ناکافی، مبلغ منفی/اعشاری غیرقابل تبدیل، یا نامتوازن => خطا و Rollback.
- هنگام فعال بودن حالت Cash-only، عملیات مالی Transfer و Derived و Reversal به‌صراحت مسدود می‌شوند؛ از ایجاد سند ناقص جلوگیری می‌شود.
- در این Slice، Endpoint عمومی `balance/postings` در DPS2 و دیگر ماژول‌های مستقل به Accounting متصل نشده‌اند. محیط UAT باید تنها سناریوهای Step-05 Cash را برای پذیرش این Slice اجرا کند.
- از این بسته برای Production، تراکنش‌های واقعی، FX، Multi-Currency، Reverse Accounting، EOD، Fee/Tax یا GL خارجی استفاده نشود.

## یادداشت سازگاری

الگوی `source`/`destination` در `LGR`، یک clearing position خارجی را به deposit customer position متصل می‌کند؛ افزایش Cash Asset واقعی بانک در `ACC` با نقش `TELLER_CASH` ثبت می‌شود. نباید حساب Clearing بیرونی را با صندوق فیزیکی اشتباه گرفت. برای اولین نسخه حسابداری دو قلم فقط «اصل مبلغ» ثبت می‌شود و کارمزد/مالیات خارج از دامنه است.

## کنترل‌ها

- `node tools\posting\verify-posting-r1d5.mjs`: آزمون ایستای Patch و ۲۱ آزمون pure-Java.
- `python3 tools/posting/stub-compile-check.py`: آزمون syntax ماژول جدید با Spring stubها؛ **جایگزین Maven نیست**.
- `docs/posting/R1D5-ACCEPTANCE-FA.md`: آزمون پذیرش و نقشه حسابداری.
- `PATCH-MANIFEST-SHA256.txt`: هش فایل‌های بسته برای اعتبارسنجی.

## نصب خودکار ایمن (اختیاری، Windows CMD)

در پوشه استخراج‌شده Patch، دستور زیر را اجرا کنید. نصب‌کننده SHA256 دو فایل مبنای نسخه 1443 را بررسی می‌کند؛ اگر متفاوت باشند، بدون جایگزینی متوقف می‌شود، و قبل از نصب از دو فایل موجود نسخه پشتیبان می‌گیرد. این دستور **هیچ SQL اجرا نمی‌کند**.

```bat
Install-R1D5-Patch.cmd "D:\Projects\core-banking-prototype-v0" -Approve
```

خروجی موفق: `R1D5_INSTALL_STATUS=PASS` و `R1D5_INSTALL_EXIT_CODE=0`. اگر وضعیت FAIL بود، هیچ عملیات پایگاه‌داده‌ای انجام نشده است؛ پوشه Backup برای بررسی فایل‌های احتمالی تغییرکرده در اختیار شماست.
