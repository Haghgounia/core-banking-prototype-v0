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
3. `release.activeForms`
4. `release.databaseSchema`
5. نسخه Dependencyهای `backend/pom.xml`
6. نسخه Dependencyهای `frontend/package.json`
7. فهرست قابلیت‌های نسخه جاری
8. `VERSION` و `CHANGELOG.md`

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

تاریخ بازنگری، تعداد فرم‌ها و فهرست قابلیت‌ها همچنان باید در هر Release به‌صورت آگاهانه بررسی شوند.
