# R10K — Angular duplicate method hotfix

## علت
Angular production build پس از عبور همه verifierهای R10J/R10D/R10I با خطای TS2393 متوقف شد، چون در `DepositAccountOperationsComponent` دو پیاده‌سازی با نام `saveAttribute` وجود داشت:

- alias قدیمی: `saveAttribute(){return this.saveMaintenance()}`
- پیاده‌سازی واقعی Wave A: `async saveAttribute(){...}`

## اصلاح
Alias قدیمی حذف شد و پیاده‌سازی واقعی `async saveAttribute()` حفظ شد. دکمه compatibility مخفی در Template همچنان به همین متد واقعی متصل است، بنابراین verifierهای تاریخی و قرارداد Wave A حفظ می‌شوند.

## دامنه
- بدون تغییر Backend/API/DDL/Migration
- بدون تغییر رفتار قابل مشاهده UI
- فقط رفع خطای Angular/TypeScript duplicate function implementation

## آزمون
`node tools\\verify-dps2-account-operations-r10k-angular-duplicate-method-hotfix.mjs`
