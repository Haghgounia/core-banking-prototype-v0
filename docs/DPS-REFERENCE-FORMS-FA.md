# فرم‌های اطلاعات پایه محصول سپرده

Schema: `DPS`

| Resource | جدول Oracle | عنوان فرم |
|---|---|---|
| `dps-accrual-frequencies` | `REF_ACCRUAL_FREQUENCY_CODE` | تناوب‌های محاسبه سود |
| `dps-aml-risk-max-levels` | `REF_AML_RISK_MAX_CODE` | حداکثر سطح ریسک پول‌شویی |
| `dps-approval-levels` | `REF_APPROVAL_LEVEL_CODE` | سطوح تأیید |
| `dps-balance-destinations` | `REF_BALANCE_DESTINATION_CODE` | مقاصد مانده |
| `dps-channels` | `REF_CHANNEL_CODE` | کانال‌ها |
| `dps-check-types` | `REF_CHECK_TYPE_CODE` | انواع کنترل |
| `dps-closure-types` | `REF_CLOSURE_TYPE_CODE` | انواع بستن سپرده |
| `dps-customer-segments` | `REF_CUSTOMER_SEGMENT_CODE` | بخش‌بندی مشتریان |
| `dps-day-count-bases` | `REF_DAY_COUNT_BASIS_CODE` | مبانی شمارش روز |
| `dps-default-currencies` | `REF_DEFAULT_CURRENCY_CODE` | ارزهای پیش‌فرض |
| `dps-deposit-groups` | `REF_DEPOSIT_GROUP_CODE` | گروه‌های سپرده |
| `dps-deposit-product-codes` | `REF_DEPOSIT_PRODUCT_CODE` | کدهای محصول سپرده |
| `dps-deposit-types` | `REF_DEPOSIT_TYPE_CODE` | انواع سپرده |
| `dps-destinations` | `REF_DESTINATION_CODE` | مقاصد |
| `dps-document-types` | `REF_DOCUMENT_TYPE_CODE` | انواع مدارک |
| `dps-failure-actions` | `REF_FAILURE_ACTION_CODE` | اقدامات در صورت شکست |
| `dps-genders` | `REF_GENDER_CODE` | جنسیت |
| `dps-hold-types` | `REF_HOLD_TYPE_CODE` | انواع مسدودی |
| `dps-holiday-adjustments` | `REF_HOLIDAY_ADJUSTMENT_CODE` | روش‌های تعدیل تعطیلات |
| `dps-inactivity-period-units` | `REF_INACTIVITY_PERIOD_UNIT_CODE` | واحدهای دوره عدم فعالیت |
| `dps-inquiry-types` | `REF_INQUIRY_TYPE_CODE` | انواع استعلام |
| `dps-kyc-levels` | `REF_KYC_LEVEL_CODE` | سطوح شناخت مشتری |
| `dps-nationality-scopes` | `REF_NATIONALITY_SCOPE_CODE` | دامنه‌های تابعیت |
| `dps-opening-statuses` | `REF_OPENING_STATUS_CODE` | وضعیت‌های افتتاح |
| `dps-org-units` | `REF_ORG_UNIT_CODE` | واحدهای سازمانی |
| `dps-org-unit-types` | `REF_ORG_UNIT_TYPE_CODE` | انواع واحد سازمانی |
| `dps-ownership-types` | `REF_OWNERSHIP_TYPE_CODE` | انواع مالکیت |
| `dps-party-types` | `REF_PARTY_TYPE_CODE` | انواع طرف حساب |
| `dps-payment-frequencies` | `REF_PAYMENT_FREQUENCY_CODE` | تناوب‌های پرداخت |
| `dps-product-families` | `REF_PRODUCT_FAMILY_CODE` | خانواده‌های محصول |
| `dps-product-statuses` | `REF_PRODUCT_STATUS_CODE` | وضعیت‌های محصول |
| `dps-profit-destination-rules` | `REF_PROFIT_DESTINATION_RULE_CODE` | قواعد مقصد سود |
| `dps-profit-distributions` | `REF_PROFIT_DISTRIBUTION_CODE` | روش‌های توزیع سود |
| `dps-profit-methods` | `REF_PROFIT_METHOD_CODE` | روش‌های محاسبه سود |
| `dps-reactivation-methods` | `REF_REACTIVATION_METHOD_CODE` | روش‌های فعال‌سازی مجدد |
| `dps-relationship-types` | `REF_RELATIONSHIP_TYPE_CODE` | انواع ارتباط |
| `dps-renewal-instructions` | `REF_RENEWAL_INSTRUCTION_CODE` | دستورهای تمدید |
| `dps-requirement-stages` | `REF_REQUIREMENT_STAGE_CODE` | مراحل الزام |
| `dps-residency-statuses` | `REF_RESIDENCY_STATUS_CODE` | وضعیت‌های اقامت |
| `dps-rule-statuses` | `REF_RULE_STATUS_CODE` | وضعیت‌های قاعده |
| `dps-servicing-statuses` | `REF_SERVICING_STATUS_CODE` | وضعیت‌های سرویس‌دهی |
| `dps-settlement-components` | `REF_SETTLEMENT_COMPONENT_CODE` | اجزای تسویه |
| `dps-settlement-methods` | `REF_SETTLEMENT_METHOD_CODE` | روش‌های تسویه |
| `dps-signing-rules` | `REF_SIGNING_RULE_CODE` | قواعد امضا |
| `dps-statuses` | `REF_STATUS_CODE` | وضعیت‌ها |
| `dps-term-units` | `REF_TERM_UNIT_CODE` | واحدهای مدت |
| `dps-transaction-types` | `REF_TRANSACTION_TYPE_CODE` | انواع تراکنش |
| `dps-version-statuses` | `REF_VERSION_STATUS_CODE` | وضعیت‌های نسخه |
| `dps-warning-period-units` | `REF_WARNING_PERIOD_UNIT_CODE` | واحدهای دوره هشدار |
| `dps-withdrawal-media` | `REF_WITHDRAWAL_MEDIA_CODE` | ابزارهای برداشت |

تمام این فرم‌ها در منوی «اطلاعات پایه محصول سپرده» نمایش داده می‌شوند. ستون `CREATED_BY` در فرم ورود نمایش داده نمی‌شود و هنگام Insert از شناسه کاربر جاری مقدار می‌گیرد.
