# Hotfix جستجوی Party در افتتاح موردی — 0.11.0

مسیر UI: `/#/four-deposits/opening` — Step 2

## مسئله
Wizard فقط Partyهای `ACTIVE` را جستجو می‌کرد. وقتی رکورد مطابق عبارت جستجو در `CIF.PARTY` وجود داشت ولی وضعیت آن ACTIVE نبود، نتیجه صفر بدون توضیح نمایش داده می‌شد و کاربر تصور می‌کرد جستجو کار نمی‌کند. ورودی Party ID با ارقام فارسی/عربی نیز normalize نمی‌شد.

## اصلاح
- جستجوی اصلی همچنان فقط `ACTIVE` است و قرارداد Opening حفظ شده است.
- اگر جستجوی ACTIVE نتیجه نداشت و متن وارد شده باشد، یک lookup تشخیصی بدون فیلتر وضعیت انجام می‌شود.
- Party غیر ACTIVE نمایش داده می‌شود ولی قابل انتخاب نیست.
- برای نتیجه صفر پیام صریح نمایش داده می‌شود.
- ارقام فارسی و عربی به ارقام ASCII تبدیل می‌شوند.
- هیچ DDL، API، Business Write یا قرارداد persistence تغییر نکرده است.

## کنترل انجام‌شده
`node tools/verify-dps2-opening-operational-v5-phase10d.mjs`

نتیجه: `24/24` PASS.

## بررسی سریع داده
در صورت نیاز:

```sql
SELECT PARTY_ID, PARTY_TYPE_CODE, LIFECYCLE_STATUS_CODE, IS_CURRENT, VALID_FROM, VALID_TO
FROM CIF.PARTY
WHERE PARTY_ID = :PARTY_ID;
```

فقط Party با `LIFECYCLE_STATUS_CODE = 'ACTIVE'` برای Opening قابل انتخاب است.
