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
  lastUpdated: '2026-08-26',
  referenceForms: 185,
  generalReferenceForms: 20,
  partyReferenceForms: 99,
  depositReferenceForms: 50,
  calendarReferenceForms: 16,
  partyOperationalScreens: 12,
  cifTableCoverage: 48,
  cifOperationalTables: 30,
  cifReadOnly360Tables: 18,
  databaseSchema: 'CIF / GEO / DPS / CAL / FEE'
} as const;

export const SYSTEM_ARCHITECTURE = [
  {
    title: 'رابط کاربری',
    icon: 'web',
    description: 'Angular SPA با فرم‌های عملیاتی Party/Customer، نمای ۳۶۰، تقویم شمسی و Runtimeهای مستقل اطلاعات پایه عمومی، Party، سپرده و تقویم سازمانی.'
  },
  {
    title: 'لایه سرویس',
    icon: 'dns',
    description: 'REST API مبتنی بر Spring MVC برای Workflowهای CIF، Reference Data و ماژول مدیریت تقویم CAL، همراه با Validation، Lookup سمت سرور و ProblemDetail.'
  },
  {
    title: 'دسترسی داده',
    icon: 'database',
    description: 'Spring JDBC / JdbcClient با Queryهای پارامتری، کنترل Optimistic Lock، Lookup سمت سرور و تجمیع Read-only برای Party / Customer 360.'
  },
  {
    title: 'پایگاه داده',
    icon: 'storage',
    description: 'Oracle Database با Schemaهای CIF، GEO، DPS، CAL و FEE؛ پوشش ۴۸ جدول عملیاتی CIF و ۱۸۵ فرم اطلاعات پایه/تقویم در چهار دامنه مرجع.'
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
      {name: 'Bean Validation', version: 'Jakarta / managed', purpose: 'اعتبارسنجی ورودی API و قواعد Workflow'},
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
      {name: 'Sass / SCSS', version: 'Angular build managed', purpose: 'استایل، متغیرهای Theme و طراحی RTL'},
      {name: 'Persian Date Input', version: 'داخلی', purpose: 'نمایش و ورود تاریخ شمسی با ارسال ISO استاندارد به Backend'}
    ]
  },
  {
    title: 'Database و داده',
    icon: 'storage',
    items: [
      {name: 'Oracle Database', version: 'Service: FREEPDB1', purpose: 'ذخیره داده‌های عملیاتی و Reference Data'},
      {name: 'CIF Schema', version: 'CIF', purpose: 'Party/Customer، KYC/Risk، Consent، Lifecycle و ۹۹ جدول مرجع Party'},
      {name: 'General Reference Schema', version: 'GEO', purpose: '۲۰ فرم اطلاعات پایه عمومی، جغرافیا، اشتغال و تحصیلات'},
      {name: 'Deposit Reference Schema', version: 'DPS', purpose: '۵۰ جدول مرجع فعال محصول‌ساز سپرده'},
      {name: 'Enterprise Calendar Schema', version: 'CAL', purpose: '۱۶ جدول تقویم سه‌گانه، روز کاری، مناسبت و اصلاح رسمی قمری؛ شامل Dataset چهارصدساله'},
      {name: 'CIF Operational Coverage', version: '48 tables', purpose: '۳۰ جدول Workflow و ۱۸ منبع Read-only در Party / Customer 360'},
      {name: 'Server-side Pagination', version: 'Oracle OFFSET/FETCH', purpose: 'نمایش کارآمد جداول حجیم و Lookupهای جست‌وجویی'}
    ]
  },
  {
    title: 'Build و استقرار',
    icon: 'deployed_code',
    items: [
      {name: 'Maven Wrapper / Maven', version: 'پروژه', purpose: 'Build و Test Backend'},
      {name: 'npm / Angular CLI', version: '21', purpose: 'Build Frontend و تولید فایل‌های Static'},
      {name: 'Executable JAR', version: 'Spring Boot', purpose: 'بسته استقرار واحد شامل Backend و Frontend'},
      {name: 'Application Log', version: 'Rolling file', purpose: 'ثبت Runtime در logs/core-banking-prototype.log'},
      {name: 'Windows/Linux Scripts', version: 'پروژه', purpose: 'Build و اجرای یکسان در محیط‌های توسعه'}
    ]
  }
];

export const SYSTEM_CAPABILITIES = [
  '۱۸۵ فرم فعال اطلاعات پایه/تقویم: ۲۰ فرم عمومی/GEO، ۹۹ فرم Party/Customer در CIF، ۵۰ فرم سپرده/DPS و ۱۶ فرم تقویم/CAL',
  '۱۲ صفحه عملیاتی CIF شامل جست‌وجو، ایجاد Party، مراحل Onboarding، عملیات Lifecycle/Merge و Party / Customer 360',
  'Workflow انتهابه‌انتها برای Person و Organization از ایجاد Party تا Role/Customer، KYC، Consent و کنترل نهایی آمادگی',
  'تفکیک Party از Customer و ایجاد شماره مشتری فقط در نقش بانکی Customer',
  'نشانی و تماس، اطلاعات مالی/شغلی، شناسه و مدرک، طبقه‌بندی، روابط/UBO/اختیار و نقش‌ها',
  'KYC، ارزیابی ریسک، Screening، استعلام بیرونی، Consent و ترجیحات ارتباطی/عمومی',
  'Lifecycle زمان‌مند، تاریخچه وضعیت و Merge کنترل‌شده Partyهای تکراری',
  'Party / Customer 360 با پوشش همه ۴۸ جدول عملیاتی CIF؛ ۳۰ جدول Workflow و ۱۸ منبع تکمیلی Read-only',
  'Searchable Combo سمت سرور، Lookupهای مرجع، جست‌وجو، مرتب‌سازی و صفحه‌بندی سمت سرور',
  'تقویم شمسی پیش‌فرض در فرم‌های عملیاتی با تبدیل استاندارد به تاریخ ISO برای Java/Oracle',
  'ماژول تقویم سازمانی CAL با ۱۶ فرم مستقل: سیستم‌های تقویم، الگوریتم، ماه/هفته، Dataset چهارصدساله سه‌تقویمی، تقویم کاری، استثناها، مناسبت‌ها و اصلاح رسمی قمری',
  'Validation قواعد کسب‌وکار، Optimistic Lock با RECORD_VERSION و پاسخ خطای استاندارد ProblemDetail',
  'لاگ Runtime در فایل logs/core-banking-prototype.log با Rolling Policy و Health endpointهای Actuator',
  'Theme روشن، تیره و هماهنگ با سیستم با نگهداری انتخاب کاربر',
  'مقایسه فایل XML/XMI خروجی Enterprise Architect با Schemaهای Oracle تنظیم‌شده در برنامه، شامل ساختار جدول/ستون، کلیدها، Metadata و Comment',
  'استخراج مستقیم Schemaهای Oracle از جمله CAL به XML/XMI استاندارد Enterprise Architect همراه با Table، Column، PK/FK، Index و Constraint'
] as const;
