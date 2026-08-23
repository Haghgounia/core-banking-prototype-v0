# نگهداری صفحه مشخصات فنی سیستم

مسیر صفحه در Angular:

```text
/#/system-specification
```

فایل اصلی داده و نمایش:

```text
frontend/src/app/features/system-specification/system-specification.data.ts
frontend/src/app/features/system-specification/system-specification.component.ts
frontend/src/app/features/system-specification/system-specification.component.html
frontend/src/app/features/system-specification/system-specification.component.scss
```

فایل `system-specification.data.ts` منبع متمرکز اطلاعات نسخه است.

در هر نسخه این موارد کنترل و به‌روزرسانی شوند:

1. `release.version`
2. `release.lastUpdated`
3. آمار `referenceForms` و تفکیک عمومی / Party / سپرده
4. آمار `partyOperationalScreens` و پوشش `cifTableCoverage`
5. `release.databaseSchema`
6. نسخه Dependencyهای `backend/pom.xml`
7. نسخه Dependencyهای `frontend/package.json`
8. فهرست قابلیت‌های نسخه جاری
9. `VERSION` و `CHANGELOG.md`

این صفحه باید فقط قابلیت‌های واقعاً موجود در Build جاری را نمایش دهد.

## همگام‌سازی خودکار نسخه‌ها

اسکریپت زیر شماره Release و نسخه فناوری‌های اصلی را از `VERSION`، `backend/pom.xml` و `frontend/package.json` استخراج می‌کند:

```text
tools/sync-system-specification.mjs
```

این اسکریپت پیش از `npm start` و `npm run build` به‌صورت خودکار اجرا و فایل زیر را بازتولید می‌کند:

```text
frontend/src/app/features/system-specification/system-version.generated.ts
```

تاریخ بازنگری، تعداد فرم‌های Reference، تعداد صفحات عملیاتی Party، پوشش جدول‌های CIF و فهرست قابلیت‌ها همچنان باید در هر Release به‌صورت آگاهانه بررسی شوند. در نسخه 0.3.22-prototype-fix30 مقادیر جاری 169 فرم Reference (20 عمومی/GEO + 99 CIF Party/Customer + 50 DPS)، 12 صفحه عملیاتی Party و 48 جدول CIF است. علاوه بر آن یک صفحه مدیریتی مستقل برای مقایسه XML/XMI مدل Enterprise Architect با Schemaهای Oracle تنظیم‌شده در برنامه فعال است.
