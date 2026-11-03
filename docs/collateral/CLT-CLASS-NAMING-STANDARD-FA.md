# استاندارد نام‌گذاری کلاس‌های Collateral

- Package: `com.behsazan.corebanking.collateral.<subdomain>`؛ متعلق به دامین و مسئولیت.
- Class: `CollateralReferenceCatalog`, `CollateralReferenceRepository`, `CollateralReferenceController`, `CollateralSecurityConfiguration`, `CollateralReferenceChangePolicy`, `CollateralReferenceChangeService` و مانند آن؛ بدون Phase، Sprint یا نسخه.
- Reactivity/UI: `CltReferenceWorkspaceComponent` پیشوند دامنه (CLT)، نه شماره تحویل؛ بدون تغییر الزامی.
- Release identification: `CLT-R1A`, `CLT-R1B`, ... فقط در README، نسخه بسته، Manifest، QA، Roadmap و Migration name.
- Backward compatibility: مسیرهای API و متغیرهای قدیمی CLT_R1A برای دوره گذار حفظ می‌شوند؛ نام Bean و کلاس تغییر می‌کند.
- تغییرات آینده بدون ایجاد `CltR2*` و با توسعه کلاس‌های موجود انجام می‌شود.
