# ADR-IAM-001 — دامنه فیزیکی و اصول User Management

**تاریخ:** ۲۰۲۶-۱۰-۳۰ | **نسخه:** R0.1 | **وضعیت:** PROPOSED; SECURITY/ARCHITECTURE APPROVAL PENDING

## تصمیم
اسکیمای کسب‌وکاری مدیریت کاربران و حاکمیت دسترسی: **`IAM`**. این نام انتخاب معماری بانک است نه نام تحمیلی BIAN. استفاده از `USR` قدیمی به عنوان مرجع مدل رد شد.

سه مرز مستقل:
1. **IdP** (Keycloak یا محصول مصوب): احراز هویت، Credential، MFA، نشست، OAuth/OIDC/SAML، Client Registration.
2. **IAM** (این DDL): Principal، حساب دسترسی، role، permission، scope، گروه، درخواست/تأیید، SoD و لاگ حاکمیتی.
3. **Authoritative Banking Sources**: CIF برای Party/KYC/Mandate، ORG/HR برای ساختار واحد و وضعیت اشتغال؛ داده‌ها تکثیر نمی‌شوند.

## چرایی
پوشش شعب، مدیریت شعب، ستاد، کانال غیرحضوری، بانکداری باز و هویت سرویس‌ها، بدون ترکیب Role کسب‌وکاری و Role امنیتی. جلوگیری از Vendor Lock-in و فرار از نقص‌های مدل USER قدیمی.

## پیامدها
- مصوب شدن IAM به معنی نصب یا راه‌اندازی آن نیست. Schema در محیط Oracle هنوز ایجاد نشده است.
- Reference Codes و ظرفیت RBAC در SQL آماده شده‌اند، اما enforcement در Spring Security/OPA با آزمون عملیاتی اجرا خواهد شد.
- هر گسترش نیازمند Migration شماره‌دار و ثبت در Canonical Roadmap است.

## مستند کنترل تغییر
`docs/iam/IAM-CANONICAL-ROADMAP-FA.md` تنها مرجع فاز و وضعیت است.
