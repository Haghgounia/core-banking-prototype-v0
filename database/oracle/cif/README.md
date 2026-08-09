# CIF Oracle Schema

این پوشه DDL واقعی Schema `CIF` دریافت‌شده از Oracle را نگهداری می‌کند.

- `ddl/CIF-050517.sql`: Snapshot مبنای توسعه فرم‌های CIF.
- جداول در محیط Oracle از قبل ایجاد شده‌اند؛ این فایل در Installer عمومی پروژه به‌صورت خودکار اجرا نمی‌شود.
- فاز ۱ UI/Backend روی `PARTY`, `PERSON`, `ORGANIZATION`, `PARTY_NAME`, `PARTY_IDENTIFIER`, `ADDRESS`, `PARTY_ADDRESS`, `CONTACT_POINT`, `KYC_CASE`, `PARTY_DOCUMENT`, `PARTY_RISK_ASSESSMENT`, `SCREENING_RESULT` فعال شده است.
