# معماری Core Banking Prototype

## سبک معماری

پروژه در این فاز یک **Modular Monolith سبک** است:

- یک Backend مبتنی بر Spring Boot
- یک Frontend مبتنی بر Angular
- یک Executable JAR
- یک Oracle Database با Schemaهای مجزا براساس دامنه

هدف، مرزبندی دامنه‌ها بدون ایجاد پیچیدگی Microservice است.

## ماژول‌های فعلی

### Reference Data

ماژول اجرایی موجود:

```text
com.behsazan.corebanking.referencedata
```

جداول این ماژول فعلاً در Schema `GEO` قرار دارند. موتور Descriptor-driven فقط برای جداول اطلاعات پایه استفاده می‌شود.

### Deposit Product Factory

Schema فیزیکی:

```text
DPS
```

در این مرحله فقط زیرقابلیت «اطلاعات پایه محصول سپرده» فعال شده است. یک Provider واقعی برای ۵۰ جدول `DPS.REF_*` ایجاد شده و همان Runtime عمومی اطلاعات پایه فرم‌ها را ارائه می‌کند. جدول‌های عملیاتی `DEPOSIT_PRODUCT*` همچنان خارج از محدوده‌اند.

## ساختار Backend

```text
com.behsazan.corebanking
├── CoreBankingApplication
├── shared
│   ├── config
│   ├── error
│   └── model
├── referencedata
│   ├── catalog
│   ├── descriptor
│   ├── education
│   ├── employment
│   ├── general
│   ├── geography
│   └── management
└── deposit
    └── productfactory
        └── reference
```

## اصل محدودسازی توسعه

در این فاز Package یا Class برای موارد زیر ایجاد نمی‌شود:

```text
deposit account
balance
interest
hold
posting
accounting
```

هر Package جدید باید همراه با حداقل یک Use Case واقعی و طراحی تأییدشده ایجاد شود.

## دسترسی داده

- `JdbcClient`
- Query پارامتری
- کنترل نام جدول و ستون با Descriptor Registry
- Transaction Boundary در Service
- Dynamic SQL فقط در ماژول اطلاعات پایه

Runtime عمومی و `Map<String,Object>` فقط برای جداول مرجع `DPS.REF_*` استفاده می‌شود. مدل عملیاتی Product Factory نباید به‌صورت پیش‌فرض از این الگو استفاده کند و پس از تحلیل جدول‌های `DEPOSIT_PRODUCT*` طراحی خواهد شد.
