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
  lastUpdated: '2026-08-29',
  referenceForms: 205,
  generalReferenceForms: 20,
  partyReferenceForms: 101,
  depositReferenceForms: 50,
  calendarReferenceForms: 16,
  calendar2ReferenceForms: 16,
  partyOperationalScreens: 12,
  cifTableCoverage: 48,
  cifOperationalTables: 30,
  cifReadOnly360Tables: 18,
  databaseSchema: 'CIF / GEO / DPS / CAL / CAL2 / FEE'
} as const;

export const SYSTEM_ARCHITECTURE = [
  {
    title: 'رابط کاربری',
    icon: 'web',
    description: 'Angular SPA با فرم‌های عملیاتی Party/Customer، نمای ۳۶۰، تقویم شمسی و Runtimeهای مستقل اطلاعات پایه عمومی، Party، سپرده، تقویم یک در CAL و تقویم دو در CAL2.'
  },
  {
    title: 'لایه سرویس',
    icon: 'dns',
    description: 'REST API مبتنی بر Spring MVC برای Workflowهای CIF، Reference Data و دو ماژول مستقل تقویم CAL/CAL2، همراه با Lookup سمت سرور، Import تراکنشی JDBC Batch و ProblemDetail.'
  },
  {
    title: 'دسترسی داده',
    icon: 'database',
    description: 'Spring JDBC / JdbcClient با Queryهای پارامتری، کنترل Optimistic Lock، Lookup سمت سرور و تجمیع Read-only برای Party / Customer 360.'
  },
  {
    title: 'پایگاه داده',
    icon: 'storage',
    description: 'Oracle Database با Schemaهای CIF، GEO، DPS، CAL، CAL2 و FEE؛ پوشش ۴۸ جدول عملیاتی CIF و ۲۰۵ فرم اطلاعات پایه/تقویم در پنج دامنه مرجع.'
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
      {name: 'CIF Schema', version: 'CIF', purpose: 'Party/Customer، KYC/Risk، Consent، Lifecycle و ۱۰۱ جدول/فرم مرجع Party'},
      {name: 'General Reference Schema', version: 'GEO', purpose: '۲۰ فرم اطلاعات پایه عمومی، جغرافیا، اشتغال و تحصیلات'},
      {name: 'Deposit Reference Schema', version: 'DPS', purpose: '۵۰ جدول مرجع فعال محصول‌ساز سپرده'},
      {name: 'تقویم یک', version: 'CAL', purpose: '۱۶ جدول تقویم سه‌گانه، روز کاری، مناسبت، اصلاح رسمی قمری و Dataset تقویم'},
      {name: 'تقویم دو', version: 'CAL2', purpose: '۱۶ جدول مستقل شامل Calendar Variant، Source Authority، Dataset Version، Canonical Day، Event Rule/Occurrence، Business Calendar و Validation Evidence'},
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
  'دو فرم مستقل ISIC نسخه‌محور در CIF: مدیریت Release/Variant و درخت فعالیت‌های اقتصادی بر پایه Parent ID، با ۷۶۶ عنوان فارسی/انگلیسی Rev.4 و وضعیت ترجمه BANK_TRANSLATED',
  '۲۰۵ فرم فعال اطلاعات پایه/تقویم: ۲۲ فرم عمومی/GEO، ۱۰۱ فرم Party/Customer در CIF، ۵۰ فرم سپرده/DPS، ۱۶ فرم تقویم یک/CAL و ۱۶ فرم تقویم دو/CAL2',
  '۱۲ صفحه عملیاتی CIF شامل جست‌وجو، ایجاد Party، مراحل Onboarding، عملیات Lifecycle/Merge و Party / Customer 360',
  'Workflow انتهابه‌انتها برای Person و Organization از ایجاد Party تا Role/Customer، KYC، Consent و کنترل نهایی آمادگی',
  'تفکیک Party از Customer و ایجاد شماره مشتری فقط در نقش بانکی Customer',
  'نشانی و تماس، اطلاعات مالی/شغلی، شناسه و مدرک، طبقه‌بندی، روابط/UBO/اختیار و نقش‌ها',
  'KYC، ارزیابی ریسک، Screening، استعلام بیرونی، Consent و ترجیحات ارتباطی/عمومی',
  'Lifecycle زمان‌مند، تاریخچه وضعیت و Merge کنترل‌شده Partyهای تکراری',
  'Party / Customer 360 با پوشش همه ۴۸ جدول عملیاتی CIF؛ ۳۰ جدول Workflow و ۱۸ منبع تکمیلی Read-only',
  'Searchable Combo سمت سرور، Lookupهای مرجع، جست‌وجو، مرتب‌سازی و صفحه‌بندی سمت سرور',
  'تقویم شمسی پیش‌فرض در فرم‌های عملیاتی با تبدیل استاندارد به تاریخ ISO برای Java/Oracle',
  'ماژول تقویم یک/CAL با ۱۶ فرم مستقل: سیستم‌های تقویم، الگوریتم، ماه/هفته، Dataset سه‌تقویمی، تقویم کاری، استثناها، مناسبت‌ها و اصلاح رسمی قمری',
  'ماژول مستقل تقویم دو/CAL2 با ۱۶ فرم: Calendar System/Variant، Source Authority، Dataset Version، Canonical Day/Calendar Date، Event، قاعده تکرار مناسبت و Materialized Occurrence، Business Calendar و Validation Evidence',
  'تعریف یک‌باره مناسبت‌های سالانه/یک‌باره در CAL2 بر مبنای Calendar Variant و تولید/بازسازی خودکار EVENT_OCCURRENCE برای سال‌های Dataset، بدون بازنویسی رخدادهای دستی/رسمی',
  'Import مستقیم بسته ZIP تقویم دو شامل ۱۵ CSV به CAL2 با JDBC Batch ترتیبی و یک تراکنش، بدون نیاز به Oracle Client/SQL*Loader',
  'Import مستقیم Dataset تقویم CAL از calendar_day.csv و calendar_date.csv با JDBC Batch خام و Commit پس از پایان هر دو stream، بدون وابستگی به SQL*Loader',
  'Validation قواعد کسب‌وکار، Optimistic Lock با RECORD_VERSION و پاسخ خطای استاندارد ProblemDetail',
  'لاگ Runtime در فایل logs/core-banking-prototype.log با Rolling Policy و Health endpointهای Actuator',
  'Theme روشن، تیره و هماهنگ با سیستم با نگهداری انتخاب کاربر',
  'مقایسه فایل XML/XMI خروجی Enterprise Architect با Schemaهای Oracle تنظیم‌شده در برنامه، شامل ساختار جدول/ستون، کلیدها، Metadata و Comment',
  'استخراج مستقیم Schemaهای Oracle از جمله CAL و CAL2 به XML/XMI استاندارد Enterprise Architect همراه با Table، Column، PK/FK، Index و Constraint'
] as const;
