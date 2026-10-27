# راهنمای نصب FEE3-BE-R4B1 — کارمزد خدمات بازه‌ای و پلکانی

**نسخه:** 2026-10-27 | **وضعیت:** نامزد پذیرش Runtime روی Oracle واقعی | **پیش‌نیاز:** Core 0.11.0 + FEE3 BE-R3 + FEE3 BE-R4A (R4A Runtime=14/14).

## محدوده قطعی

- افزوده‌شدن مسیر مستقل `POST /api/v1/fee3/r4/services/preview` با `@Transactional(readOnly=true)`؛ این مسیر برای **شبیه‌سازی پیش‌نویس UAT** است و هیچ دستور ثبت حسابداری، برداشت کارمزد، فعال‌سازی نسخه یا انتشار تعرفه اجرا نمی‌کند.
- `BRACKET`: کل مبلغ ریالی در **یک بازه** انتخاب می‌شود و همان بازه فرمول `fixedAmount + amount × ratePercent / 100` را تعیین می‌کند؛ حد پایین شامل و حد بالا غیرشامل است. آخرین بازه بدون سقف است.
- `PROGRESSIVE`: مبلغ از صفر به پله‌های متوالی تقسیم می‌شود؛ برای **هر پله دارای سهم مثبت**، `fixedAmount + chargedBand × ratePercent / 100` محاسبه و سپس جمع می‌شود.
- `FIXED`، `PERCENT` و `MIXED` همچنان از API تاییدشده‌ی قبلی R4-A یعنی `POST /api/v1/fee3/r4/preview` استفاده می‌کنند؛ R4-B1 آن API را تغییر نمی‌دهد.
- صرفاً یک قاعده با `RULE_ORDER=1`، مبنای صریح `REQUEST_AMOUNT`، نرخ سطری ثابت و ارز `IRR` پذیرفته می‌شود. از انتخاب خودکار تعرفه، ارز خارجی، شاخص مصرف، نرخ درخواستی و چندقاعده‌ای پشتیبانی نمی‌شود.
- مقادیر مبلغ و مرزها حداکثر دو رقم اعشار (`NUMBER(22,2)`)، نرخ پله حداکثر هشت رقم اعشار (`NUMBER(18,8)`)، و مبلغ سقف محاسبه‌شده حداکثر `10^18` ریال است.
- نرخ هر پله و جزء ثابت آن باید **صریحاً درج شوند**؛ خالی‌بودن فیلد به معنی صفر نیست. مرزهای بازه‌ای/پلکانی باید صعودی و سراسری از صفر تا بازه/پله باز انتهایی باشند. هم‌پوشانی و خلأ با `BLOCKED` پاسخ داده می‌شوند.
- کف و سقف کل خروجی **پس از محاسبه خام و پیش از گردکردن** اعمال می‌شوند؛ سپس گردکردن صحیح ریال، و در صورت `TAXABLE`، محاسبه مالیات فقط با `taxPercent` صریح انجام می‌شود. `EXEMPT` در `taxMode` یعنی **غیرمشمول مالیات**، نه معافیت از اصل کارمزد.
- تخفیف، بخشودگی اصل کارمزد، اصلاحات، توافق گروهی، ماتریس، تعداد/مصرف، `UNIT_RATE`، `DAILY`، فرمول و نوع `GUARANTEE_FEE` عمداً `BLOCKED` می‌مانند تا قواعد مصوب آنها در فازهای مربوط مشخص شود.
- مرجع داده‌های فعلی Oracle همچنان **۳۰۰ نسخه DRAFT، بدون نسخه ACTIVE** است. هیچ نتیجه‌ای مجوز وصول یا نرخ قطعی بانکی نیست.

## مرجع طراحی و محدودیت انطباق

ساختار فیزیکی از DDL واقعی `FEE3-2026-10-25.txt` برداشت شده است. تفاوت ریاضی `BRACKET` و `PROGRESSIVE` از فایل مرجع `FCE_v13_8_84_Program_REDOWNLOAD.html` و نمونه‌های محاسباتی همان نسخه بررسی شد. این پیاده‌سازی **زیرمجموعه محدود و Fail-closed** از آن برنامه است؛ تنظیمات `calcType` مستقل، نرخ واحد و `inclusiveUpper=true` در R4-B1 اجرا نمی‌شوند.

سناریوهای عددی: `docs/install/FEE3-BE-R4B1-GOLDEN-CASES.json`. نمونه‌ها برای کنترل عددی هستند و نرخ بانکی معتبر روز را تعیین نمی‌کنند.

## الزامات نصب — Windows CMD

فایل ZIP را در مسیر `D:\Projects\core-banking-prototype-v0` **استخراج** کنید. ابتدا دو فرمان زیر را اجرا کنید؛ در صورت `CONFLICT` هیچ تغییری اعمال نکنید:

```bat
cd /d D:\Projects\core-banking-prototype-v0
python tools\fee3-backend\releases\r4b1\install.py --preview
python tools\fee3-backend\releases\r4b1\install.py --apply
```

سپس کنترل‌های بدون Oracle:

```bat
python tools\fee3-backend\r4b1\verify-r4b1.py
python tools\fee3-backend\r4b1\verify-java.py
python tools\fee3-backend\r4b1\run-pure-java.py
python tools\fee3-backend\r4b1\selftest.py
```

پس از همه PASSها، Build واقعی و راه‌اندازی مجدد:

```bat
bin\stop.cmd
build-production.cmd
bin\start.cmd
curl.exe --noproxy "*" http://127.0.0.1:8091/api/v1/fee3/readiness
```

**حتماً** بعد از پاسخ `readiness` و تأیید `draftWritesEnabled:false`، در حالی‌که سرویس روی پورت 8091 روشن است، اجرا کنید:

```bat
call tools\fee3-backend\r4b1\run-runtime-readonly.cmd
```

گزارش: `docs\install\FEE3-R4B1-RUNTIME-RESULT.json`.

> اگر `WinError 10061` مشاهده شد، علت رد اتصال محلی است: ابتدا listener پورت 8091، لاگ `bin\start.cmd` و سپس `curl --noproxy` را کنترل کنید. **نیازی به Patch جدید یا تغییر Oracle نیست.**

## قرارداد API

```json
{
  "draft": {
    "calcItemId": null, "tariffVersionId": null,
    "expectedItemRecordVersion": null, "expectedVersionRecordVersion": null,
    "item": {"title":"آزمون کارمزد", "module":"PAYMENT", "operation":"TRANSFER", "amountKind":"SERVICE_FEE", "variant":null, "ownerDomain":"TARIFF", "description":null},
    "version": {"validFrom":"2026-10-27", "validTo":null, "origin":"BANK_INTERNAL", "sourceId":null, "method":"PROGRESSIVE", "taxMode":"EXEMPT", "rounding":"HALF_UP", "changeReason":"آزمون غیرماندگار", "financialPolicy":{}, "serviceCurrency":"IRR", "feeCurrency":"IRR", "uiContext":{}},
    "scope": {"channels":["ALL"],"customers":["ALL"],"groups":["ALL"],"products":["ALL"],"contracts":[],"currency":"IRR"},
    "rules": [{"order":1,"method":"PROGRESSIVE","base":"REQUEST_AMOUNT","rateSource":"RULE","rate":null,"fixed":null,"collateral":null,"coverage":null,"min":null,"max":null,"formulaCode":null,"parameters":{"originalConfig":{"basisExplicit":true,"basisCode":"REQUEST_AMOUNT","rateSource":"CONFIG","tiers":[{"upTo":10000000,"ratePercent":0.1,"fixedAmount":0},{"upTo":null,"ratePercent":0.05,"fixedAmount":0}],"brackets":[]}}}],
    "tests": [{"title":"آزمون نمونه","request":{"amount":30000000},"expected":20000}]
  },
  "inputs":{"amount":30000000},
  "effectiveOn":null
}
```

`module` و `operation` در نمونه باید با مرجع فعال Oracle مطابقت داشته باشند؛ اسکریپت Runtime به‌جای اتکا به مقادیر نمونه، یک عملیات فعال را از Oracle می‌خواند. تاریخ `validFrom` باید در اجرای واقعی روز کاری Oracle یا بعد از آن تنظیم شود (اسکریپت این کار را خودکار انجام می‌دهد).

اگر درخواست دارای شناسه Oracle است، **هر چهار فیلد شناسه/نسخه رکورد** باید کامل باشند؛ Repository مرحله R4-A تمام مشخصات محاسبه‌ساز را از Oracle بازخوانی و مقایسه می‌کند. وجود شناسه Oracle به‌تنهایی مجوز استفاده از تعرفه منتشرشده نیست.

## معیار پذیرش

۱. Build واقعی Maven (شامل JUnitهای جدید) موفق شود.
۲. آزمون pure Java، کنترل کامپایل API و selftest محلی موفق شوند.
۳. Runtime HTTP واقعی در حالت read-only با `FAIL=0` تکمیل شود.
۴. SQL فقط‌خواندنی قبلی، تغییرنکردن هر شش شمارش Oracle و صفر ماندن Change Request و نسخه فعال را تأیید کند.
۵. هیچ جابجایی در وضعیت Maker/Checker، وصول یا Posting ایجاد نشود.

**بدون اثبات چهار دروازه نخست، R4-B1 صرفاً نامزد پذیرش است.**
