# CIF Oracle Schema

این پوشه DDL واقعی Schema `CIF` دریافت‌شده از Oracle را نگهداری می‌کند.

- `ddl/CIF-050517.sql`: Snapshot مبنای توسعه فرم‌های CIF.
- جداول در محیط Oracle از قبل ایجاد شده‌اند؛ این فایل در Installer عمومی پروژه به‌صورت خودکار اجرا نمی‌شود.
- فاز ۱ UI/Backend روی `PARTY`, `PERSON`, `ORGANIZATION`, `PARTY_NAME`, `PARTY_IDENTIFIER`, `ADDRESS`, `PARTY_ADDRESS`, `CONTACT_POINT`, `KYC_CASE`, `PARTY_DOCUMENT`, `PARTY_RISK_ASSESSMENT`, `SCREENING_RESULT` فعال شده است.
- فاز ۲ فرم `PERSON` را تکمیل می‌کند و Lookupهای موجود کشور، زبان، جنسیت و وضعیت اقامت را به UI متصل می‌کند؛ برای کدهایی که DDL مرجع مستقل ارائه نکرده، جدول یا مقدار فرضی ساخته نمی‌شود.
- در محیط تست، کاربر `CIF` باید روی Tablespaceهای مورد استفاده Schema سهمیه داشته باشد. نمونه رفع `ORA-01950`: `ALTER USER CIF QUOTA UNLIMITED ON TS_CIF;` و `ALTER USER CIF QUOTA UNLIMITED ON ITS_CIF;`.

- فاز عملیاتی ۸ (`0.3.19`) فیلدهای جدید `KYC_CASE` را با Metadata جاری همگام می‌کند و `EXTERNAL_INQUIRY_RESULT` را وارد Workflow می‌کند. برای دیتابیس موجود اسکریپت `migrations/0.3.19-kyc-case-alignment.sql` اجرا شود.
- فاز Reference Data شماره ۲ در `reference-data/compliance-risk` شامل ۲۱ جدول مرجع تطبیق، ریسک و KYC است.
- برای ارتقا از 0.3.2، فایل `reference-data/compliance-risk/install.sql` اجرا شود.
- وضعیت اعتبارسنجی `NOT_VERIFIED` منبع به `UNVERIFIED` نرمال شده است تا با Default جداول عملیاتی CIF همسان باشد.

- FIX73 (`0.3.62`): مدل مستقل ISIC در `isic2/` بر پایه `REF_ISIC_RELEASE` و `REF_ISIC_ACTIVITY2` بازطراحی شده است؛ سلسله‌مراتب با `PARENT_ACTIVITY_ID`، سطح عددی با `LEVEL_NO` و عنوان فارسی/انگلیسی اجباری نگهداری می‌شود. Seed Rev.4 شامل 766 عنوان دو‌زبانه است. برای نصب `@isic2/00-install-isic2.sql` یا فایل Standalone `@isic2/CIF_ISIC2_FULL_INSTALL.sql` اجرا شود.
