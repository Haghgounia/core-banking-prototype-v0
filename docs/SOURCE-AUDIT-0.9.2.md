# Source Audit — 0.9.2

## مبنای بررسی
Source ارسالی `core-banking-prototype-v0-2026-09-15-1536.zip` با Contract فازهای چهار سپرده و نتیجه واقعی Reconciliation دیتابیس Phase 4 + Phase 9 بررسی شد.

## وضعیت قبل از اصلاح
- `VERSION`, Backend POM, Angular package و System Specification همگی روی `0.9.0` هماهنگ بودند.
- Verifierهای Phase 4، Phase 7، Phase 8 و Phase 9 Pass بودند؛ بنابراین Business Code مربوط به Account Creation/Activation/Operations/Closure هم‌راستا بود.
- با این حال Migration موفق `0.9.2-phase4-phase9-account-schema-reconciliation.sql` که در Oracle اجرا و تأیید شده بود داخل Source وجود نداشت؛ بنابراین Source و وضعیت واقعی DB کاملاً همگام نبودند.
- `README-FA.txt` در Root متعلق به Build Fix بسیار قدیمی 0.3.2 بود.
- `config/application.yml_` یک Backup اضافی بود و `backend/src/main/resources/application.yml_` نیز Backup قدیمی‌تری بود که حتی `deposit-account: DPS2` و تنظیم Audit جدید را نداشت.
- `build-production.sh` نسبت به Windows Build سه Guard کم داشت: `migrate-source-layout`, `verify-cif-religion-reference`, `verify-calendar-current-year-default`.
- ZIP ارسالی یک Working Tree خام بود و Artifactهای Runtime/Generated را نیز حمل می‌کرد: `.upgrade-backup`, `logs`, `data/document-storage`, `frontend/.angular`, `backend/src/main/resources/static`, `app/BUILD-VERSION`, `database/oracle/exports`.

## نکته امنیت/بسته‌بندی
`data/document-storage` شامل فایل‌های واقعی ذخیره‌شده توسط Runtime بود. این مسیر نباید در Source/Release ZIP توزیع شود. نسخه Clean Source آن را حذف می‌کند؛ در محیط Runtime موجود، این داده‌ها نباید توسط Patch حذف شوند.

## اصلاحات 0.9.2
- Migration Reconciliation به مسیر استاندارد `database/oracle/dps2/migrations` اضافه شد.
- Version markerها روی `0.9.2` همگام شدند.
- `README-FA.txt` به `docs/patches/PATCH-0.3.2-BUILD-FIX1-README-FA.txt` منتقل شد.
- Backupهای `application.yml_` از Source فعال خارج شدند.
- `tools/migrate-root-layout.mjs` اضافه شد تا Overlay روی Sourceهای قدیمی نیز Layout را خودکار تمیز کند.
- Buildهای Windows/Unix از نظر Node Guards همسان شدند.
- `package-release.cmd` قبل از Packaging Layout را Normalize می‌کند و Runtime/Generated/DB-export/document-storage را از بسته Source حذف می‌کند.

## Root استاندارد پس از پاک‌سازی
- `.gitignore`
- `CHANGELOG.md`
- `README-FA.md`
- `VERSION`
- `build-production.cmd`
- `build-production.sh`
- `package-release.cmd`

`package-release.cmd` عمداً در Root باقی می‌ماند؛ Entry Point رسمی Release Packaging است و توسط Runtime Artifact Verifier کنترل می‌شود.

## نتیجه Contract دیتابیس و کد
Schema موفق Oracle شامل ستون‌ها/Sequenceهای مورد استفاده Repositoryها است. Statusهای Account برابر `PENDING_ACTIVATION / ACTIVE / CLOSED` و Lifecycle Eventها برابر `CREATE / ACTIVATE / CLOSE` هستند. Phase 9 در کد نیز فقط `ACTIVE -> CLOSED` را با `RECORD_VERSION`, row lock و Event append-only پیاده می‌کند.

## QA
تمام 46 Node Build Guard نسخه نهایی Pass شدند. Verifier اختصاصی Reconciliation نیز `20/20 PASS` است. Maven/Angular Compile نهایی همچنان باید در محیط Build ویندوز پروژه اجرا شود.
