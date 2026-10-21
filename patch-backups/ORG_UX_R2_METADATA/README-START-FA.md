# نصب ORG UX R2 روی آخرین سورس ارسالی

مبنای Patch: core-banking-prototype-v0-2026-10-21-1415.zip.

**ترتیب الزامی** (برای جلوگیری از ازکارافتادن فرم‌های نقاط خدمت):

1. Backend را متوقف کنید؛ از پروژه و Oracle بکاپ بگیرید.
2. SQL زیر را در SQL Developer با F5 اجرا کنید: `files\database\oracle\org\service-point\ORG-SERVICE-POINT-RENAME-OPT-IN.sql`. ابتدا با `DEFINE APPLY_CHANGES = NO` پیش‌نمایش بگیرید. پس از بررسی وابستگی‌ها، آن را `YES` کنید و F5 بزنید. منتظر خروجی `SERVICE_POINT_RENAME_APPLY_COMPLETE` و فهرست هفت جدول باشید. **بدون اجرای این مرحله، فرم‌های نقاط خدمت با سورس جدید کار نمی‌کنند.**
3. این ZIP را **خارج از ریشه پروژه** استخراج کنید. در همان پوشه Patch:

```bat
tools\apply-org-ux-r2.cmd "D:\Projects\core-banking-prototype-v0" CHECK
```

4. فقط در صورت `ORG_UX_R2_PRECHECK_PASS`:

```bat
tools\apply-org-ux-r2.cmd "D:\Projects\core-banking-prototype-v0" APPLY
```

5. در ریشه پروژه اجرا کنید:

```bat
node tools\verify-org-forms.mjs
node tools\verify-org-location-form.mjs
node tools\verify-org-grade-p3-p4.mjs
node tools\verify-org-ux-r1.mjs
node tools\verify-org-ux-r2.mjs
build-production.cmd
```

فایل‌های راهنما و manifest بسته را در ریشه پروژه کپی نکنید. نصب‌کننده فقط محتویات `files` را کپی می‌کند و از فایل‌های جایگزین‌شده در پوشه `patch-backups` پشتیبان می‌گیرد.

**محدودیت QA:** آزمون‌های ساختاری و Java به‌صورت محلی موفق شدند، اما Build واقعی Angular/Maven و اجرای زنده Oracle در این محیط تأیید نشده است. جزئیات در `files/docs/ORG-UX-R2-QA-2026-10-21-FA.md`.
