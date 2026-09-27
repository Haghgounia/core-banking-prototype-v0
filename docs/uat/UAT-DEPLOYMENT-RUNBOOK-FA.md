# DPS2 0.11.0 — Runbook نصب و اجرای UAT

## 1. پیش‌نیاز محیط

- Windows 10/11 یا محیط سازگار با اسکریپت‌های `.cmd`
- Java 21
- دسترسی شبکه به Oracle هدف
- Schemaهای پروژه و Migrationهای نسخه 0.11.0 از قبل اعمال شده باشند
- پورت پیش‌فرض برنامه: `8091`

> اطلاعات Credential را داخل مستند یا Repository ثبت نکنید. Connection String را در Session محیط UAT تنظیم کنید.

## 2. مسیر پروژه

نمونه مسیر مرجع:

```bat
cd /d D:\Projects\core-banking-prototype-v0
```

## 3. تنظیم Oracle Connection

در همان Command Prompt:

```bat
set "CORE_BANKING_ORACLE_CONNECT=<USER>/<PASSWORD>@//<HOST>:<PORT>/<SERVICE>"
```

در صورت استفاده از Config محیطی، مقدار واقعی Credential را فقط در محیط امن UAT نگه دارید.

## 4. Preflight غیرمخرب

```bat
tools\uat-preflight.cmd
```

این Preflight فقط موارد زیر را بررسی می‌کند:

- `VERSION=0.11.0`
- وجود `app\core-banking-prototype.jar`
- وجود و تطابق `app\BUILD-VERSION`
- وجود `config\application.yml`
- آزاد بودن پورت 8091 یا اعلام PID فعال
- وجود Java

این ابزار هیچ Business Write و هیچ Qualification اجرا نمی‌کند.

## 5. Build اولیه در محیط UAT

این Handoff به‌صورت **Source RC** بسته‌بندی شده و JAR اجرایی داخل ZIP قرار نگرفته است. بنابراین قبل از اولین Start در محیط UAT یک Build لازم است:

```bat
build-production.cmd
```

پس از Build دوباره اجرا کنید:

```bat
tools\uat-preflight.cmd
```

و وجود این فایل را کنترل کنید:

```text
app\core-banking-prototype.jar
```

> خود بسته Handoff هیچ Build/Regressionی اجرا نکرده است. اجرای Build در محیط UAT یک اقدام استقرار است و Debt تست‌های Delta ذکرشده را جایگزین نمی‌کند.

## 6. Start

```bat
bin\start.cmd
```

پس از Start، برنامه روی آدرس زیر در دسترس است:

```text
http://127.0.0.1:8091/
```

Log اصلی:

```text
logs\core-banking-prototype.log
```

## 7. Stop

```bat
bin\stop.cmd
```

## 8. در صورت خطای Start

ابتدا این موارد را بررسی کنید:

```bat
netstat -ano | findstr ":8091"
java -version
type VERSION
type app\BUILD-VERSION
powershell -NoProfile -Command "Get-Content .\logs\core-banking-prototype.log -Tail 200"
```

اگر Port قبلاً اشغال است، قبل از Start نسخه جدید، Process قبلی را با `bin\stop.cmd` متوقف کنید.

## 9. مرزبندی UAT

در این مرحله موارد زیر اجرا نشوند مگر برای چرخه Regression نهایی:

```text
tools\qualify-dps2-phase11nk-final.cmd
tools\qualify-dps2-designer-step16.cmd
runtime-* e2e scripts
```

UAT باید از UI و APIهای Business واقعی انجام شود، نه از Qualification Fixtureها.
