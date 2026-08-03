import {GENERATED_SYSTEM_VERSIONS} from './system-version.generated';

export interface TechnologyItem {
  readonly name: string;
  readonly version: string;
  readonly purpose: string;
}

export interface TechnologyGroup {
  readonly title: string;
  readonly icon: string;
  readonly items: readonly TechnologyItem[];
}

export const SYSTEM_RELEASE = {
  version: GENERATED_SYSTEM_VERSIONS.release,
  lastUpdated: '2026-08-02',
  activeForms: 70,
  databaseSchema: 'GEO و DPS فعال'
} as const;

export const SYSTEM_ARCHITECTURE = [
  {
    title: 'رابط کاربری',
    icon: 'web',
    description: 'Angular به‌صورت Single Page Application، با Runtime عمومی برای Grid، فرم، Lookup و روابط سلسله‌مراتبی.'
  },
  {
    title: 'لایه سرویس',
    icon: 'dns',
    description: 'REST API مبتنی بر Spring MVC، سرویس عمومی Descriptor-driven و پاسخ خطای استاندارد ProblemDetail.'
  },
  {
    title: 'دسترسی داده',
    icon: 'database',
    description: 'Spring JDBC و JdbcClient با Queryهای پارامتری، صفحه‌بندی سمت سرور و Registry کنترل‌شده نام جداول و ستون‌ها.'
  },
  {
    title: 'پایگاه داده',
    icon: 'storage',
    description: 'Oracle Database؛ جداول فعال اطلاعات پایه فعلاً در Schema GEO قرار دارند و پنجاه جدول مرجع محصول‌ساز سپرده نیز در Schema DPS فعال شده‌اند.'
  }
] as const;

export const SYSTEM_TECHNOLOGY_GROUPS: readonly TechnologyGroup[] = [
  {
    title: 'Backend',
    icon: 'terminal',
    items: [
      {name: 'Java', version: GENERATED_SYSTEM_VERSIONS.java, purpose: 'زبان اصلی Backend و Runtime اجرایی'},
      {name: 'Spring Boot', version: GENERATED_SYSTEM_VERSIONS.springBoot, purpose: 'راه‌اندازی، پیکربندی و بسته‌بندی برنامه'},
      {name: 'Spring Web MVC', version: 'Managed by Boot', purpose: 'REST API و مدیریت درخواست‌های HTTP'},
      {name: 'Spring JDBC / JdbcClient', version: 'Managed by Boot', purpose: 'دسترسی مستقیم و کنترل‌شده به Oracle'},
      {name: 'Bean Validation', version: 'Jakarta / managed', purpose: 'اعتبارسنجی ورودی API'},
      {name: 'Spring Boot Actuator', version: GENERATED_SYSTEM_VERSIONS.springBoot, purpose: 'Health و اطلاعات عملیاتی سرویس'},
      {name: 'Oracle JDBC', version: GENERATED_SYSTEM_VERSIONS.oracleJdbc, purpose: 'Driver اتصال به Oracle Database'}
    ]
  },
  {
    title: 'Frontend',
    icon: 'devices',
    items: [
      {name: 'Angular', version: GENERATED_SYSTEM_VERSIONS.angular, purpose: 'چارچوب اصلی رابط کاربری'},
      {name: 'TypeScript', version: GENERATED_SYSTEM_VERSIONS.typescript, purpose: 'زبان توسعه Frontend با کنترل نوع'},
      {name: 'Angular Material', version: GENERATED_SYSTEM_VERSIONS.angularMaterial, purpose: 'کامپوننت‌های UI، فرم، منو و جدول'},
      {name: 'RxJS', version: GENERATED_SYSTEM_VERSIONS.rxjs, purpose: 'مدیریت جریان‌های HTTP و تعاملات غیرهم‌زمان'},
      {name: 'Sass / SCSS', version: 'Angular build managed', purpose: 'استایل، متغیرهای Theme و طراحی RTL'}
    ]
  },
  {
    title: 'Database و داده',
    icon: 'storage',
    items: [
      {name: 'Oracle Database', version: 'Service: FREEPDB1', purpose: 'ذخیره جداول اطلاعات پایه'},
      {name: 'Reference Data Schema', version: 'GEO', purpose: 'مالک فعلی جداول فعال اطلاعات پایه'},
      {name: 'Deposit Product Factory Schema', version: 'DPS', purpose: 'مالک ۵۰ جدول مرجع فعال محصول‌ساز سپرده'},
      {name: 'Descriptor Registry', version: 'داخلی', purpose: 'تعریف Metadata جدول، فیلد، Lookup و روابط والد–فرزند'},
      {name: 'Server-side Pagination', version: 'Oracle OFFSET/FETCH', purpose: 'نمایش کارآمد جداول حجیم مانند روستاها'}
    ]
  },
  {
    title: 'Build و استقرار',
    icon: 'deployed_code',
    items: [
      {name: 'Maven Wrapper', version: 'پروژه', purpose: 'Build و Test Backend بدون نیاز به Maven سراسری'},
      {name: 'npm / Angular CLI', version: '21', purpose: 'Build Frontend و تولید فایل‌های Static'},
      {name: 'Executable JAR', version: 'Spring Boot', purpose: 'بسته استقرار واحد شامل Backend و Frontend'},
      {name: 'Windows/Linux Scripts', version: 'پروژه', purpose: 'Build و اجرای یکسان در محیط‌های توسعه'}
    ]
  }
];

export const SYSTEM_CAPABILITIES = [
  '۷۰ فرم فعال اطلاعات پایه شامل ۲۰ فرم GEO و ۵۰ فرم مرجع محصول سپرده در DPS',
  'CRUD عمومی بدون Controller و Component اختصاصی برای هر جدول',
  'جست‌وجو، مرتب‌سازی و صفحه‌بندی سمت سرور',
  'Combo والد و فیلتر سلسله‌مراتبی',
  'درخت جغرافیایی با بارگذاری مرحله‌ای',
  'ثبت CREATED_BY و کنترل هم‌زمانی RECORD_VERSION در جداول مرجع DPS',
  'پاسخ خطای استاندارد ProblemDetail',
  'Theme روشن، تیره و هماهنگ با سیستم با نگهداری انتخاب کاربر'
] as const;
