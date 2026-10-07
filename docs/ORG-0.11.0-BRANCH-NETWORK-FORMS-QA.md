# ORG 0.11.0 — فرم‌های ساختار سازمانی و شبکه شعب

تاریخ: 2026-10-07

## دامنه پیاده‌سازی

برای تمام 35 جدول فعلی Schema `ORG` فرم مدیریتی ایجاد شده است. فرم‌ها از زیرساخت Generic Reference Data موجود پروژه استفاده می‌کنند و از مسیر اصلی زیر در دسترس هستند:

```text
/organization
```

فرم هر Resource نیز از مسیر زیر باز می‌شود:

```text
/organization/:resource
```

منوی «ساختار سازمانی و شبکه شعب» فرم‌ها را در شش گروه کسب‌وکاری نمایش می‌دهد: ساختار و روابط، مکان/پوشش/تماس/مسئولین، خدمات و قابلیت‌ها، ساعات کاری و استثناها، نقاط ارائه خدمت و پایانه‌های خودخدمت.

## قابلیت‌های پیاده‌سازی‌شده

- عنوان و برچسب فارسی برای تمام Resourceها و فیلدهای Schema ORG.
- جست‌وجو، مشاهده، ثبت، ویرایش و حذف از طریق API عمومی `/api/v1/reference/{resource}`.
- Lookup برای FKهای داخلی Schema ORG و Parent Context برای فرم‌های وابسته.
- Combo/Select برای وضعیت‌ها و کدهای کنترل‌شده مانند وضعیت واحد، نوع برنامه کاری، روز هفته، نقش مکان، نوع پوشش، نوع استثنا و نقش انتساب.
- کنترل تاریخ‌های متقاطع `effective_from/effective_to`، `opening_date/closing_date` و تاریخ‌های نصب/فعال‌سازی/جمع‌آوری پایانه.
- پشتیبانی `CREATED_AT = SYSTIMESTAMP` در Generic Insert برای `ORGANIZATION_UNITS` و استفاده از `CREATED_BY` موجود در زیرساخت عمومی.
- نمایش تعداد رکورد هر فرم در صفحه منوی ORG.

## Masterهای مشترک خارج از ORG

سه شناسه زیر در Schema ORG وجود دارند اما Master canonical آنها در این Schema تعریف نشده است:

- `EMPLOYEE_ID`
- `CONTACT_POINT_ID`
- `GEO_ENTITY_ID`

بنابراین در این نسخه این سه فیلد به‌صورت شناسه عددی نگهداری شده‌اند. پس از اتصال Descriptor/API به Master واقعی Employee، Contact Point و GEO، باید به Lookup تبدیل شوند؛ جدول تکراری برای آنها داخل ORG ایجاد نشده است.

## کنترل ایستا

Verifier اختصاصی:

```bat
node tools\verify-org-forms.mjs
```

خروجی مورد انتظار:

```text
ORG_FORMS_VERIFY_PASS
```

Verifier موارد زیر را کنترل می‌کند:

- وجود دقیق 35 Descriptor برای 35 جدول ORG؛
- تطابق نام Resource با نام Physical Table؛
- وجود تنظیم `organization: ORG`؛
- Routeهای Angular؛
- ورودی Sidebar؛
- حضور هر 35 فرم دقیقاً یک‌بار در منوی ORG؛
- Audit fields جدول `ORGANIZATION_UNITS`؛
- پشتیبانی `createdAt/SYSTIMESTAMP` در Repository عمومی؛
- Validation تاریخ‌های متقاطع.

## وضعیت Build در محیط تولید این Patch

در محیط ساخت این Patch، اجرای کامل Angular/Maven به علت نبود Dependency Cache کامل و عدم دسترسی شبکه ممکن نبود. `npm ci --offline` به علت نبود tarball یکی از Dependencyها متوقف شد و Maven Wrapper نیز برای دریافت Maven Distribution به شبکه نیاز داشت.

در مقابل، `OrganizationDescriptorProvider` با `javac` و Stubهای حداقلی Annotationهای Spring از نظر نحوی/نوعی کنترل شده و Verifier ایستای ORG نیز PASS شده است. Build کامل باید در محیط پروژه که Dependencyهای Maven و npm در دسترس‌اند اجرا شود.
