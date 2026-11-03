# IAM-R1.9 / S1B-RO3-REPAIR2 — رفع خطای Bean Override آزمون

تاریخ: 2026-11-03  |  حوزه: IAM-01 / S1B-RO3  |  نوع: اصلاح آزمون، بدون تغییر کد Production

## شواهد خطا

نصب REPAIR1 با پیام `IAM_S1B_RO3_REPAIR1_SOURCE_PASS=37/37; changed=3` و `testCompile` موفق شد؛
اما آزمون `IamS1bSignedHttpIntegrationTest` هنگام ساخت `ApplicationContext` با خطای
`Unable to replace bean ... iamS1bJwtDecoder` متوقف شد. در این آزمون Context محدود
به‌طور عمدی از IdP و Oracle واقعی استفاده نمی‌کند و Bean تولیدی از نوع JWT ممکن است ثبت نشده باشد.

## اصلاح

فقط در کلاس آزمون، `@MockitoBean(name="iamS1bJwtDecoder", enforceOverride=true)`
به `@MockitoBean(name="iamS1bJwtDecoder")` تغییر کرد. این رفتار مطابق سازوکار
`REPLACE_OR_CREATE` Spring Test است. تست همچنان از `NimbusJwtDecoder.withPublicKey` و
جفت‌کلید موقت RSA برای اعتبارسنجی واقعی امضا، Issuer، Audience و زمان اعتبار استفاده می‌کند.
کنترل ایستا برای پیشگیری از بازگشت `enforceOverride=true` نیز افزوده شده است.

## محدوده پذیرش

- آزمون مستقل Java/Node و نصب Hash-based روی سورس مرجع: قابل انجام در بسته‌بندی.
- Maven/JUnit Spring روی ویندوز، سپس Build و JAR و HTTP: الزام نصب؛ تا لاگ موفق نباشد پذیرفته نیست.
- IdP/JWKS بانکی و Oracle DEV `SELECT-only`: هنوز تأیید نشده، Gate باز.
- هیچ DDL/DML/COMMIT، API نوشتنی، Token/کلید خصوصی پایدار یا تغییر Frontend.
- فازهای ثابت IAM-00 تا IAM-08 و نقشه راه R1.9 بدون بازتعریف حفظ شده‌اند.
