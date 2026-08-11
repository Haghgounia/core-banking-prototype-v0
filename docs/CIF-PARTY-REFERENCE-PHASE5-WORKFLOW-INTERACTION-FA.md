# CIF Party Reference - Phase 5: Workflow & Interaction

## نتیجه

تمام 17 جدول حوزه «گردش‌کار و تعامل» از مدل مرجع ارسالی در `CIF` فعال شده‌اند. عنوان فرم‌ها و `NAME_FA`های Seed به‌صورت دستی بازبینی شده‌اند تا الگوی ترجمه مکانیکی مانند «تعامل نوع» یا «گردش‌کار وضعیت» وارد رابط کاربری نشود.

## فرم‌های جدید

- نقش عامل عملیات (`REF_ACTOR_ROLE`)
- نوع عملیات ممیزی (`REF_AUDIT_ACTION`)
- نوع موجودیت ممیزی (`REF_AUDIT_ENTITY_TYPE`)
- دلیل ممیزی (`REF_AUDIT_REASON`)
- نتیجه رسیدگی به شکایت (`REF_COMPLAINT_RESOLUTION`)
- وضعیت شکایت (`REF_COMPLAINT_STATUS`)
- دلیل تغییر وضعیت شکایت (`REF_COMPLAINT_STATUS_REASON`)
- نوع شکایت (`REF_COMPLAINT_TYPE`)
- الگوریتم هش (`REF_HASH_ALGORITHM`)
- نتیجه تعامل (`REF_INTERACTION_OUTCOME`)
- نوع تعامل (`REF_INTERACTION_TYPE`)
- سفر مشتری (`REF_JOURNEY`)
- مرحله سفر مشتری (`REF_JOURNEY_STAGE`)
- نوع رویداد سفر مشتری (`REF_JOURNEY_EVENT_TYPE`)
- نوع ترجیح مشتری (`REF_PREFERENCE_TYPE`)
- نوع مرجع مرتبط (`REF_REFERENCE_TYPE`)
- وضعیت گردش‌کار (`REF_WORKFLOW_STATUS`)

تعداد Seed این فاز: **71**.

## اتصال به Customer 360

فیلد `KYC_CASE.STATUS_CODE` در فرم Customer 360 از TextBox آزاد به Lookup `CIF.REF_WORKFLOW_STATUS` تبدیل شده است. بقیه Referenceهای این فاز برای مراحل عملیاتی بعدی Complaint / Interaction / Journey / Audit آماده شده‌اند.

## روابط

دو رابطه صریح منبع حفظ شده‌اند:

```text
REF_JOURNEY
   └── REF_JOURNEY_STAGE
          └── REF_JOURNEY_EVENT_TYPE
```
