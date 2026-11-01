# ORG — نقشه راه قطعی بازنشستگی CONTACT_POINTS_V2

**نسخه سند:** 2.0 — 2026-11-01 (R5-C4B / R5-C5)  
**مبنای سورس:** بسته واقعی `core-banking-prototype-v0-2026-11-01-1215.zip` ارسالی کارفرما.  
**اصل تغییرناپذیر:** تنها مخزن عملیاتی راه‌های ارتباطی شعب و واحدها `ORG.ORGANIZATION_UNIT_CONTACT_POINTS` است. سایر Masterهای تماس صرفاً شواهد تاریخی هستند.

## وضعیت مبتنی بر شواهد

| مرحله | وضعیت | شاهد |
|---|---|---|
| R5-C1 | تکمیل | تطبیق ۶٬۴۲۷ Master تاریخی و ۶٬۹۳۰ انتساب، بدون Drift |
| R5-C2 | تکمیل | Detach دو FK و nullable شدن شناسه تاریخی؛ Build و آزمون فرم موفق |
| R5-C3 | تکمیل ساختاری | `CONTACT_POINTS` به `DEPRECATED_CONTACT_POINTS` تبدیل شد؛ ۶٬۴۲۷ ردیف محفوظ، OBJECT_ID=121134؛ Runtime روی UNIT و write enabled تأیید شد |
| R5-C3 — ویرایش پس از Rename | تأیید جداگانه ارائه نشده | موفقیت Readiness دلیل اجرای واقعی UPDATE پس از Rename نیست |
| R5-C4A | تکمیل | Oracle: V2=۶٬۹۳۰، OUCP=۶٬۹۴۷، ۱۷ رکورد جدید، `DB_BLOCKER_CATEGORIES=0`، `GRANTS=0` |
| R5-C4B — سورس | **تهیه‌شده، نصب/Build ویندوز در انتظار** | وابستگی‌های Java به V2 حذف؛ alias `canonical` فقط OUCP و READ-ONLY؛ API تاریخی 410 |
| R5-C4C — پذیرش | **در انتظار** | Build/JUnit، Smoke و تأیید UI و مصرف‌کنندگان بیرونی |
| R5-C5 — Rename | **مجاز نیست تا پیش‌شرط‌ها احراز شوند** | Preview/Opt-In + Oracle Verify و Smoke پس از Rename |

## اصلاح منطبق با سورس واقعی R5-C4B

1. `CanonicalOrganizationContactService.java` دیگر هیچ SQL روی `CONTACT_POINTS_V2` ندارد؛ `storage-mode=canonical` فقط نام سازگاری تاریخی است، از پیاده‌سازی واحد `OrganizationContactService` (OUCP) به‌صورت Delegate استفاده می‌کند و **همیشه فقط‌خواندنی** می‌ماند. هر دو قابلیت نوشتن تاریخی در این alias بی‌اثرند؛ نوشتن مجاز فقط در سرویس پیش‌فرض `legacy` با پرچم `ORG_CONTACT_UNIT_WRITES_ENABLED=true` و کنترل‌های Oracle انجام می‌شود.
2. `GET /api/v1/organization/unit-contacts/migration/readiness` دیگر V2 را Query نمی‌کند و `HTTP 410 Gone` با کد `ORG_V2_MIGRATION_ENDPOINT_RETIRED` برمی‌گرداند. مصرف‌کنندگان API قدیمی باید این تغییر قرارداد را بپذیرند.
3. API عملیاتی `/api/v1/organization/unit-contacts` و فرم Angular بدون تغییر مانده‌اند؛ کنترل نسخه و قفل‌های تراکنشی OUCP محفوظ‌اند.
4. سه کنترل ایستای قدیمی R3B2، R5، R5B/R6 و دو کلاس JUnit با معماری OUCP سازگار شده‌اند؛ آزمون Source R5-C4B به Build ویندوز و لینوکس اضافه شده است.

## نقشه راه اجرایی و معیار خروج

**R5-C4B — نصب فقط سورس:** نصب‌کننده یکپارچه با Backup، کنترل فایل‌های واقعیِ داده‌شده و اجرای Build ویندوز. بدون اجرای Oracle SQL. انتظار: `ORG_R5C4B_STATIC_PASS`, `ORG_R5C4B_BUILD_PASS=1` و `INSTALL_EXIT=0`. در صورت شکست، Rollback سورس انجام می‌شود و Log باقی می‌ماند.

**R5-C4C — کنترل Runtime:** پس از راه‌اندازی، `bin/verify-org-contacts-r5c4b.cmd` را یک بار اجرا کنید. باید `storageMode=UNIT`, `ready=true`, `masterPresent=false` و HTTP 410 برای API تاریخی دیده شود. سپس مشاهده/ویرایش کنترل‌شده OUCP در UI و تصمیم مصرف‌کنندگان API قدیمی ثبت شود. `writesEnabled` در حالت پیش‌فرض وابسته به پرچم مجاز محیط است.

**R5-C5 — Oracle Rename جداگانه:** فایل `02-ORG-R5C5-CONTACT_POINTS_V2-RENAME-OPT-IN.sql` در حالت پیش‌فرض `APPLY_CHANGES=NO` فقط Preview است. فقط بعد از Backup معتبر، توقف Backend و تمام نویسندگان، UAT، اثبات Cutover سورس و تأیید مصرف‌کنندگان بیرونی، شش پرچم را `YES` کنید و با F5 اجرا کنید. سپس فایل `03-ORG-R5C5-V2-RENAME-VERIFY-READONLY.sql` را اجرا کرده و Runtime Smoke را مجدداً انجام دهید.

## ثابت‌های داده و مخاطرات

- `ORG.DEPRECATED_CONTACT_POINTS`: تاریخچه ۶٬۴۲۷ رکورد با `OBJECT_ID=121134` حفظ شود.
- `ORG.CONTACT_POINTS_V2` (تا R5-C5): ۶٬۹۳۰ ردیف تاریخی با `OBJECT_ID=121823`، بدون رکورد Native جدید. در Rename باید ردیف‌ها و شناسه شیء محفوظ بمانند.
- `ORG.ORGANIZATION_UNIT_CONTACT_POINTS`: ۶٬۹۴۷ رکورد در آخرین ممیزی؛ ۶٬۹۳۰ ارتباط تاریخی و ۱۷ رکورد فاقد ارتباط V2؛ حذف/Backfill خودکار یا تغییر این ردیف‌ها ممنوع.
- صفر بودن وابستگی در نمای `ALL_*` اثبات نبودن SQL دینامیک یا مصرف‌کننده خارجی نیست؛ تأیید مستقل سازمانی الزامی است.
- `ALTER TABLE ... RENAME TO ...` در Oracle DDL با Commit خودکار است؛ بدون امضای واقعی تأییدها نباید اجرا شود.
- هیچ جدول حذف یا Truncate نمی‌شود و هیچ تغییری در CIF/Party، ORG Locations، CAL2، FEE3 و سایر دامنه‌ها مجاز نیست.

**مانع فعلی:** شواهد اجرای Build/Smoke نسخه واقعی R5-C4B و مجوزهای عملیاتی R5-C5 هنوز ارائه نشده‌اند. در نتیجه وضعیت Rename نهایی `NOT_APPLIED` است.
