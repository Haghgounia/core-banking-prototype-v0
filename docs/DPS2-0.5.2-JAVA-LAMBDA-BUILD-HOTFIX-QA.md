# DPS2 0.5.2 — Java Lambda Build Hotfix QA

Scope این نسخه فقط اصلاح Build است و Functional Scope فاز 5 را تغییر نمی‌دهد.

## Defect
`DepositOpeningAuditService.prepareTargetPlans` متغیر `entity` را دوباره assign می‌کرد و همان متغیر در lambda مربوط به `orElseThrow` capture می‌شد. Java compiler خطای effectively-final صادر می‌کرد.

## Fix
```java
String requestedEntity = upper(mutation.entityName());
final String entity = blank(requestedEntity) ? ROOT_ENTITY : requestedEntity;
```

## Acceptance
- VERSION / Maven / Angular = 0.5.2
- legacy reassignment pattern absent
- immutable lambda-capture pattern present
- Phase 4/5/0.5.1 regressions preserved
- no DDL change
