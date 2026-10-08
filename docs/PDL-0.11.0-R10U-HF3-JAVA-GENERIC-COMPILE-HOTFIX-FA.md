# PDL 0.11.0 R10U-HF3 — Java Generic Compile Hotfix

## مسئله
کامپایل واقعی Maven در `ProductBuilderService.saveEligibilityRule` به علت invariant بودن genericهای Java متوقف می‌شد:

`Stream<LinkedHashMap<String,Object>>.toList()` قابل انتساب به `List<Map<String,Object>>` نیست.

## اصلاح
مرحله copy در Stream به صورت صریح به `Map<String,Object>` widen شد:

```java
.<Map<String, Object>>map(row -> new LinkedHashMap<>(row))
```

منطق کسب‌وکاری، ترتیب Criteria، تزریق `ELIGIBILITY_RULE_ID` و مدل دیتابیس R10U تغییری نکرده‌اند.

## Guard
`tools/verify-pdl-r10u-hf3-java-generic.mjs` به build ویندوز و Unix اضافه شد تا برگشت الگوی `map(LinkedHashMap::new)` قبل از Maven شناسایی شود.

## Migration
هیچ DDL/Migration جدیدی ندارد. Migration R10U را دوباره اجرا نکنید.
