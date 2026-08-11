# CIF Party Reference - Phase 2: Compliance, Risk and KYC

نسخه 0.3.3-prototype حوزه `Compliance and Risk` مدل مرجع Party/Customer را فعال می‌کند.

- جدول‌های جدید: 21
- Seed Row جدید: 87
- کل فرم‌های Party Reference فعال پس از این فاز: 53
- UI فرم‌ها از همان الگوی استاندارد فرم‌های اطلاعات پایه پروژه استفاده می‌کند؛ Metadata فنی در صفحه عملیاتی نمایش داده نمی‌شود.
- عناوین فارسی فرم‌ها و Labelهای کلید اصلی به‌صورت معنایی بازبینی شده‌اند.
- Customer 360 برای KYC، ریسک، غربالگری و وضعیت اعتبارسنجی از Lookupهای همین Reference Data استفاده می‌کند.

## Normalization

`REF_VERIFICATION_STATUS.NOT_VERIFIED` در مستند منبع به `UNVERIFIED` تبدیل شده است، چون DDL عملیاتی CIF برای PARTY و PARTY_IDENTIFIER از `UNVERIFIED` استفاده می‌کند.
