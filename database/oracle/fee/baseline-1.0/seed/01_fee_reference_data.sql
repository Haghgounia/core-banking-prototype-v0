-- ============================================================================
-- 01_fee_reference_data.sql
-- Fee Prototype Target Model Baseline 1.0 / Oracle
-- UTF-8
-- ============================================================================
SET DEFINE OFF;
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK;
ALTER SESSION SET CURRENT_SCHEMA = FEE;

PROMPT Loading FEE_REF_DOMAIN ...
INSERT INTO FEE_REF_DOMAIN (
    DOMAIN_ID,
    DOMAIN_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    VALUE_TYPE_CODE,
    DISPLAY_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -1001,
    'FEE_TYPE',
    'انواع کارمزد BIAN',
    'BIAN FeeTypeValues',
    'BIAN FeeTypeValues',
    'CODE',
    1,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_DOMAIN (
    DOMAIN_ID,
    DOMAIN_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    VALUE_TYPE_CODE,
    DISPLAY_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -1002,
    'CHARGE_BEARER_TYPE',
    'نوع تحمل‌کننده کارمزد',
    'BIAN ChargeBearerTypeValues',
    'BIAN ChargeBearerTypeValues',
    'CODE',
    2,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_DOMAIN (
    DOMAIN_ID,
    DOMAIN_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    VALUE_TYPE_CODE,
    DISPLAY_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -1003,
    'DEBIT_CREDIT_INDICATOR',
    'جهت بدهکار/بستانکار',
    'BIAN DebitCreditIndicatorValues',
    'BIAN DebitCreditIndicatorValues',
    'CODE',
    3,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_DOMAIN (
    DOMAIN_ID,
    DOMAIN_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    VALUE_TYPE_CODE,
    DISPLAY_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -1004,
    'ADJUSTMENT_DIRECTION',
    'جهت تعدیل',
    'BIAN AdjustmentDirectionTypeValues',
    'BIAN AdjustmentDirectionTypeValues',
    'CODE',
    4,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_DOMAIN (
    DOMAIN_ID,
    DOMAIN_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    VALUE_TYPE_CODE,
    DISPLAY_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -1005,
    'FEE_PAYMENT_METHOD',
    'روش پرداخت کارمزد',
    'BIAN FeePaymentMethodTypeValues',
    'BIAN FeePaymentMethodTypeValues',
    'CODE',
    5,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_DOMAIN (
    DOMAIN_ID,
    DOMAIN_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    VALUE_TYPE_CODE,
    DISPLAY_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -1006,
    'APPLICATION_TIMING_MODALITY',
    'مدالیتی زمان اعمال',
    'BIAN FeeApplicationTimingModalityTypeValues',
    'BIAN FeeApplicationTimingModalityTypeValues',
    'CODE',
    6,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_DOMAIN (
    DOMAIN_ID,
    DOMAIN_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    VALUE_TYPE_CODE,
    DISPLAY_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -1007,
    'CALCULATION_MODALITY',
    'مدالیتی محاسبه',
    'BIAN FeeCalculationModalityTypeValues',
    'BIAN FeeCalculationModalityTypeValues',
    'CODE',
    7,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_DOMAIN (
    DOMAIN_ID,
    DOMAIN_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    VALUE_TYPE_CODE,
    DISPLAY_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -1008,
    'CALCULATION_BASIS',
    'مبنای محاسبه',
    'BIAN FeeCalculationBasisTypeValues',
    'BIAN FeeCalculationBasisTypeValues',
    'CODE',
    8,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_DOMAIN (
    DOMAIN_ID,
    DOMAIN_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    VALUE_TYPE_CODE,
    DISPLAY_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -1009,
    'CURRENCY_MODALITY',
    'مدالیتی ارز',
    'BIAN FeeCurrencyModalityTypeValues',
    'BIAN FeeCurrencyModalityTypeValues',
    'CODE',
    9,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_DOMAIN (
    DOMAIN_ID,
    DOMAIN_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    VALUE_TYPE_CODE,
    DISPLAY_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -1010,
    'POSTING_MODALITY',
    'مدالیتی ثبت',
    'BIAN FeePostingModalityTypeValues',
    'BIAN FeePostingModalityTypeValues',
    'CODE',
    10,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_DOMAIN (
    DOMAIN_ID,
    DOMAIN_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    VALUE_TYPE_CODE,
    DISPLAY_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -1011,
    'INVOLVEMENT_ROLE',
    'نقش طرف در کارمزد',
    'BIAN FeeInvolvementTypeValues',
    'BIAN FeeInvolvementTypeValues',
    'CODE',
    11,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_DOMAIN (
    DOMAIN_ID,
    DOMAIN_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    VALUE_TYPE_CODE,
    DISPLAY_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -1012,
    'MODALITY_TYPE',
    'نوع مدالیتی',
    'BIAN FeeModalityTypeValues',
    'BIAN FeeModalityTypeValues',
    'CODE',
    12,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_DOMAIN (
    DOMAIN_ID,
    DOMAIN_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    VALUE_TYPE_CODE,
    DISPLAY_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -1013,
    'APPLICATION_FREQUENCY',
    'تناوب اعمال کارمزد',
    'BIAN FeeApplicationFrequencyValues',
    'BIAN FeeApplicationFrequencyValues',
    'CODE',
    13,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_DOMAIN (
    DOMAIN_ID,
    DOMAIN_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    VALUE_TYPE_CODE,
    DISPLAY_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -1014,
    'CALCULATION_FREQUENCY',
    'تناوب محاسبه کارمزد',
    'BIAN FeeCalculationFrequencyValues',
    'BIAN FeeCalculationFrequencyValues',
    'CODE',
    14,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_DOMAIN (
    DOMAIN_ID,
    DOMAIN_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    VALUE_TYPE_CODE,
    DISPLAY_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -1015,
    'FEE_ACCOUNT_TYPE',
    'نوع حساب کارمزد',
    'BIAN FeeAccountTypeValues',
    'BIAN FeeAccountTypeValues',
    'CODE',
    15,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_DOMAIN (
    DOMAIN_ID,
    DOMAIN_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    VALUE_TYPE_CODE,
    DISPLAY_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -1016,
    'FEE_PLAN_TYPE',
    'نوع طرح کارمزد',
    'BIAN FeePlanTypeValues',
    'BIAN FeePlanTypeValues',
    'CODE',
    16,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_DOMAIN (
    DOMAIN_ID,
    DOMAIN_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    VALUE_TYPE_CODE,
    DISPLAY_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -1017,
    'POLICY_TYPE',
    'نوع سیاست',
    'Platform',
    'Platform',
    'CODE',
    17,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_DOMAIN (
    DOMAIN_ID,
    DOMAIN_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    VALUE_TYPE_CODE,
    DISPLAY_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -1018,
    'VERSION_TYPE',
    'نوع نسخه',
    'Platform',
    'Platform',
    'CODE',
    18,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_DOMAIN (
    DOMAIN_ID,
    DOMAIN_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    VALUE_TYPE_CODE,
    DISPLAY_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -1019,
    'LIFECYCLE_STATUS',
    'وضعیت چرخه عمر',
    'Platform',
    'Platform',
    'CODE',
    19,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_DOMAIN (
    DOMAIN_ID,
    DOMAIN_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    VALUE_TYPE_CODE,
    DISPLAY_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -1020,
    'CALCULATION_STRATEGY',
    'استراتژی محاسبه',
    'Platform',
    'Platform',
    'CODE',
    20,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_DOMAIN (
    DOMAIN_ID,
    DOMAIN_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    VALUE_TYPE_CODE,
    DISPLAY_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -1021,
    'TIER_BASIS',
    'مبنای پله',
    'Platform',
    'Platform',
    'CODE',
    21,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_DOMAIN (
    DOMAIN_ID,
    DOMAIN_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    VALUE_TYPE_CODE,
    DISPLAY_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -1022,
    'ROUNDING_MODE',
    'روش گردکردن',
    'Platform',
    'Platform',
    'CODE',
    22,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_DOMAIN (
    DOMAIN_ID,
    DOMAIN_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    VALUE_TYPE_CODE,
    DISPLAY_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -1023,
    'PARTY_TYPE',
    'نوع طرف',
    'Prototype',
    'Prototype',
    'CODE',
    23,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_DOMAIN (
    DOMAIN_ID,
    DOMAIN_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    VALUE_TYPE_CODE,
    DISPLAY_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -1024,
    'CUSTOMER_SEGMENT',
    'بخش مشتری',
    'Prototype',
    'Prototype',
    'CODE',
    24,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_DOMAIN (
    DOMAIN_ID,
    DOMAIN_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    VALUE_TYPE_CODE,
    DISPLAY_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -1025,
    'CUSTOMER_GROUP',
    'گروه مشتری',
    'Prototype',
    'Prototype',
    'CODE',
    25,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_DOMAIN (
    DOMAIN_ID,
    DOMAIN_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    VALUE_TYPE_CODE,
    DISPLAY_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -1026,
    'PRODUCT_TYPE',
    'نوع محصول',
    'Prototype',
    'Prototype',
    'CODE',
    26,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_DOMAIN (
    DOMAIN_ID,
    DOMAIN_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    VALUE_TYPE_CODE,
    DISPLAY_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -1027,
    'PRODUCT_FEATURE_TYPE',
    'نوع ویژگی محصول',
    'Prototype',
    'Prototype',
    'CODE',
    27,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_DOMAIN (
    DOMAIN_ID,
    DOMAIN_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    VALUE_TYPE_CODE,
    DISPLAY_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -1028,
    'REQUIREMENT_TYPE',
    'نوع الزام ویژگی',
    'Platform',
    'Platform',
    'CODE',
    28,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_DOMAIN (
    DOMAIN_ID,
    DOMAIN_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    VALUE_TYPE_CODE,
    DISPLAY_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -1029,
    'ARRANGEMENT_ORIGIN',
    'منشأ ترتیب کارمزد',
    'Platform',
    'Platform',
    'CODE',
    29,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_DOMAIN (
    DOMAIN_ID,
    DOMAIN_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    VALUE_TYPE_CODE,
    DISPLAY_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -1030,
    'COMPONENT_TYPE',
    'نوع مؤلفه کارمزد',
    'IFW/Platform',
    'IFW/Platform',
    'CODE',
    30,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_DOMAIN (
    DOMAIN_ID,
    DOMAIN_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    VALUE_TYPE_CODE,
    DISPLAY_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -1031,
    'REFUNDABILITY',
    'قابلیت برگشت',
    'Platform',
    'Platform',
    'CODE',
    31,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_DOMAIN (
    DOMAIN_ID,
    DOMAIN_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    VALUE_TYPE_CODE,
    DISPLAY_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -1032,
    'ALLOCATION_METHOD',
    'روش تسهیم',
    'IFW/Platform',
    'IFW/Platform',
    'CODE',
    32,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_DOMAIN (
    DOMAIN_ID,
    DOMAIN_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    VALUE_TYPE_CODE,
    DISPLAY_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -1033,
    'ALLOCATION_BASIS',
    'مبنای تسهیم',
    'IFW/Platform',
    'IFW/Platform',
    'CODE',
    33,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_DOMAIN (
    DOMAIN_ID,
    DOMAIN_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    VALUE_TYPE_CODE,
    DISPLAY_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -1034,
    'SETTLEMENT_METHOD',
    'روش تسویه',
    'IFW/Platform',
    'IFW/Platform',
    'CODE',
    34,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_DOMAIN (
    DOMAIN_ID,
    DOMAIN_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    VALUE_TYPE_CODE,
    DISPLAY_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -1035,
    'OVERRIDE_TYPE',
    'نوع Override',
    'IFW/Platform',
    'IFW/Platform',
    'CODE',
    35,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_DOMAIN (
    DOMAIN_ID,
    DOMAIN_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    VALUE_TYPE_CODE,
    DISPLAY_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -1036,
    'REVERSAL_TYPE',
    'نوع برگشت',
    'Platform',
    'Platform',
    'CODE',
    36,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_DOMAIN (
    DOMAIN_ID,
    DOMAIN_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    VALUE_TYPE_CODE,
    DISPLAY_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -1037,
    'EVIDENCE_TYPE',
    'نوع شاهد ممیزی',
    'IFW/Platform',
    'IFW/Platform',
    'CODE',
    37,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_DOMAIN (
    DOMAIN_ID,
    DOMAIN_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    VALUE_TYPE_CODE,
    DISPLAY_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -1038,
    'RULE_NODE_TYPE',
    'نوع گره قاعده',
    'Platform',
    'Platform',
    'CODE',
    38,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_DOMAIN (
    DOMAIN_ID,
    DOMAIN_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    VALUE_TYPE_CODE,
    DISPLAY_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -1039,
    'RULE_OPERATOR',
    'عملگر قاعده',
    'Platform',
    'Platform',
    'CODE',
    39,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_DOMAIN (
    DOMAIN_ID,
    DOMAIN_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    VALUE_TYPE_CODE,
    DISPLAY_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -1040,
    'INPUT_DATA_TYPE',
    'نوع داده ورودی',
    'Platform',
    'Platform',
    'CODE',
    40,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_DOMAIN (
    DOMAIN_ID,
    DOMAIN_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    VALUE_TYPE_CODE,
    DISPLAY_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -1041,
    'UNIT',
    'واحد',
    'Platform',
    'Platform',
    'CODE',
    41,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_DOMAIN (
    DOMAIN_ID,
    DOMAIN_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    VALUE_TYPE_CODE,
    DISPLAY_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -1042,
    'CHANNEL',
    'کانال',
    'Prototype',
    'Prototype',
    'CODE',
    42,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_DOMAIN (
    DOMAIN_ID,
    DOMAIN_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    VALUE_TYPE_CODE,
    DISPLAY_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -1043,
    'ACTIVITY',
    'فعالیت',
    'Prototype',
    'Prototype',
    'CODE',
    43,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_DOMAIN (
    DOMAIN_ID,
    DOMAIN_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    VALUE_TYPE_CODE,
    DISPLAY_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -1044,
    'TRANSACTION_TYPE',
    'نوع تراکنش',
    'Prototype',
    'Prototype',
    'CODE',
    44,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_DOMAIN (
    DOMAIN_ID,
    DOMAIN_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    VALUE_TYPE_CODE,
    DISPLAY_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -1045,
    'CURRENCY',
    'ارز',
    'Prototype',
    'Prototype',
    'CODE',
    45,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_DOMAIN (
    DOMAIN_ID,
    DOMAIN_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    VALUE_TYPE_CODE,
    DISPLAY_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -1046,
    'ACCOUNT_TYPE',
    'نوع حساب',
    'Prototype',
    'Prototype',
    'CODE',
    46,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_DOMAIN (
    DOMAIN_ID,
    DOMAIN_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    VALUE_TYPE_CODE,
    DISPLAY_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -1047,
    'COLLECTION_MODE',
    'حالت وصول',
    'Platform',
    'Platform',
    'CODE',
    47,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_DOMAIN (
    DOMAIN_ID,
    DOMAIN_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    VALUE_TYPE_CODE,
    DISPLAY_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -1048,
    'FAILURE_ACTION',
    'اقدام در شکست وصول',
    'Platform',
    'Platform',
    'CODE',
    48,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_DOMAIN (
    DOMAIN_ID,
    DOMAIN_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    VALUE_TYPE_CODE,
    DISPLAY_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -1049,
    'STACKING_MODE',
    'روش همپوشانی تخفیف',
    'Platform',
    'Platform',
    'CODE',
    49,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_DOMAIN (
    DOMAIN_ID,
    DOMAIN_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    VALUE_TYPE_CODE,
    DISPLAY_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -1050,
    'PRICING_FEATURE_TYPE',
    'نوع ویژگی قیمت‌گذاری',
    'BIAN/Platform',
    'BIAN/Platform',
    'CODE',
    50,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_DOMAIN (
    DOMAIN_ID,
    DOMAIN_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    VALUE_TYPE_CODE,
    DISPLAY_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -1051,
    'ACCOUNT_ROLE',
    'نقش حساب',
    'BIAN/Platform',
    'BIAN/Platform',
    'CODE',
    51,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_DOMAIN (
    DOMAIN_ID,
    DOMAIN_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    VALUE_TYPE_CODE,
    DISPLAY_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -1052,
    'BENEFICIARY_ROLE',
    'نقش ذی‌نفع',
    'IFW/Platform',
    'IFW/Platform',
    'CODE',
    52,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

PROMPT Loading FEE_REF_VALUE ...
INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100001,
    -1001,
    'AccountingFee',
    'کارمزد حسابداری',
    'AccountingFee',
    NULL,
    1,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100002,
    -1001,
    'ADRFee',
    'کارمزد گواهی سپرده آمریکایی',
    'ADRFee',
    NULL,
    2,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100003,
    -1001,
    'AdvisoryFee',
    'کارمزد مشاوره',
    'AdvisoryFee',
    NULL,
    3,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100004,
    -1001,
    'AirWayBillFee',
    'کارمزد بارنامه هوایی',
    'AirWayBillFee',
    NULL,
    4,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100005,
    -1001,
    'BackEndLoad',
    'شارژ انتهایی',
    'BackEndLoad',
    NULL,
    5,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100006,
    -1001,
    'BrokerageFee',
    'کارمزد کارگزاری',
    'BrokerageFee',
    NULL,
    6,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100007,
    -1001,
    'ClearanceAndHandlingAtDestination',
    'کارمزد تسویه و انجام در مقصد',
    'ClearanceAndHandlingAtDestination',
    NULL,
    7,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100008,
    -1001,
    'ClearanceAndHandlingAtOrigin',
    'کارمزد تسویه و انجام در مبدأ',
    'ClearanceAndHandlingAtOrigin',
    NULL,
    8,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100009,
    -1001,
    'CollectFreight',
    'جمع‌آوری کرایه حمل',
    'CollectFreight',
    NULL,
    9,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100010,
    -1001,
    'Commission',
    'کمیسیون',
    'Commission',
    NULL,
    10,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100011,
    -1001,
    'ContingencyDeferredSalesCharge',
    'کارمزد فروش موکول‌شده اضطراری',
    'ContingencyDeferredSalesCharge',
    NULL,
    11,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100012,
    -1001,
    'CorrespondentBankCharge',
    'کارمزد بانک کارسپاری',
    'CorrespondentBankCharge',
    NULL,
    12,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100013,
    -1001,
    'CustodyFee',
    'کارمزد نگهداری',
    'CustodyFee',
    NULL,
    13,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100014,
    -1001,
    'DangerousGoodsFee',
    'کارمزد کالاهای خطرناک',
    'DangerousGoodsFee',
    NULL,
    14,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100015,
    -1001,
    'DilutionLevy',
    'مالیات رقیق‌سازی',
    'DilutionLevy',
    NULL,
    15,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100016,
    -1001,
    'Discount',
    'تخفیف',
    'Discount',
    NULL,
    16,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100017,
    -1001,
    'Equalisation',
    'متوازن‌سازی',
    'Equalisation',
    NULL,
    17,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100018,
    -1001,
    'FrontEndLoad',
    'شارژ اولیه',
    'FrontEndLoad',
    NULL,
    18,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100019,
    -1001,
    'Initial',
    'اولیه',
    'Initial',
    NULL,
    19,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100020,
    -1001,
    'InsurancePremium',
    'حق بیمه',
    'InsurancePremium',
    NULL,
    20,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100021,
    -1001,
    'IssuanceCancellationFee',
    'کارمزد صدور/ابطال',
    'IssuanceCancellationFee',
    NULL,
    21,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100022,
    -1001,
    'IssuanceFee',
    'کارمزد صدور',
    'IssuanceFee',
    NULL,
    22,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100023,
    -1001,
    'ManagementFee',
    'کارمزد مدیریت',
    'ManagementFee',
    NULL,
    23,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100024,
    -1001,
    'MatchingFees',
    'کارمزد تطبیق',
    'MatchingFees',
    NULL,
    24,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100025,
    -1001,
    'MiscellaneousFee',
    'کارمزد متفرقه',
    'MiscellaneousFee',
    NULL,
    25,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100026,
    -1001,
    'Other',
    'سایر',
    'Other',
    NULL,
    26,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100027,
    -1001,
    'Packaging',
    'بسته‌بندی',
    'Packaging',
    NULL,
    27,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100028,
    -1001,
    'PartAcquis',
    'مشارکت بخشی',
    'PartAcquis',
    NULL,
    28,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100029,
    -1001,
    'Penalty',
    'جریمه',
    'Penalty',
    NULL,
    29,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100030,
    -1001,
    'PickUp',
    'جمع‌آوری',
    'PickUp',
    NULL,
    30,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100031,
    -1001,
    'PostageCharge',
    'هزینه پستی',
    'PostageCharge',
    NULL,
    31,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100032,
    -1001,
    'Premium',
    'حق بیمه/علاوه',
    'Premium',
    NULL,
    32,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100033,
    -1001,
    'PublicationFee',
    'کارمزد انتشار',
    'PublicationFee',
    NULL,
    33,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100034,
    -1001,
    'RegulatoryFee',
    'کارمزد نظارتی',
    'RegulatoryFee',
    NULL,
    34,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100035,
    -1001,
    'SecurityCharge',
    'هزینه‌های امنیتی',
    'SecurityCharge',
    NULL,
    35,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100036,
    -1001,
    'ServiceProvisionFee',
    'کارمزد ارائه خدمات',
    'ServiceProvisionFee',
    NULL,
    36,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100037,
    -1001,
    'ShippingCharge',
    'هزینه حمل',
    'ShippingCharge',
    NULL,
    37,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100038,
    -1001,
    'SignatureService',
    'خدمات امضا',
    'SignatureService',
    NULL,
    38,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100039,
    -1001,
    'SpecialConcessions',
    'امتیازات ویژه',
    'SpecialConcessions',
    NULL,
    39,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100040,
    -1001,
    'SpeciallyAgreedFrontEndLoad',
    'شارژ اولیه توافقی ویژه',
    'SpeciallyAgreedFrontEndLoad',
    NULL,
    40,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100041,
    -1001,
    'StorageAtDestination',
    'انبارداری در مقصد',
    'StorageAtDestination',
    NULL,
    41,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100042,
    -1001,
    'StorageAtOrigin',
    'انبارداری در مبدأ',
    'StorageAtOrigin',
    NULL,
    42,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100043,
    -1001,
    'Switch',
    'سوییچ/تعویض',
    'Switch',
    NULL,
    43,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100044,
    -1001,
    'TransferFee',
    'کارمزد انتقال',
    'TransferFee',
    NULL,
    44,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100045,
    -1001,
    'TransportCharges',
    'هزینه حمل‌ونقل',
    'TransportCharges',
    NULL,
    45,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100046,
    -1001,
    'UCITSCommission',
    'کمیسیون UCITS',
    'UCITSCommission',
    NULL,
    46,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100047,
    -1002,
    'BorneByDebtor',
    'بر عهده بدهکار',
    'BorneByDebtor',
    NULL,
    1,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100048,
    -1002,
    'BorneByCreditor',
    'بر عهده بستانکار',
    'BorneByCreditor',
    NULL,
    2,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100049,
    -1002,
    'Shared',
    'مشترک',
    'Shared',
    NULL,
    3,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100050,
    -1002,
    'FollowingServiceLevel',
    'براساس سطح خدمت',
    'FollowingServiceLevel',
    NULL,
    4,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100051,
    -1003,
    'Debit',
    'بدهکار',
    'Debit',
    NULL,
    1,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100052,
    -1003,
    'Credit',
    'بستانکار',
    'Credit',
    NULL,
    2,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100053,
    -1004,
    'Added',
    'افزوده‌شده',
    'Added',
    NULL,
    1,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100054,
    -1004,
    'Subtracted',
    'کسرشده',
    'Subtracted',
    NULL,
    2,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100055,
    -1005,
    'Cash',
    'نقدی',
    'Cash',
    NULL,
    1,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100056,
    -1005,
    'Unit',
    'واحدی/غیرنقدی',
    'Unit',
    NULL,
    2,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100057,
    -1006,
    'One-time',
    'یکبار',
    'One-time',
    NULL,
    1,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100058,
    -1006,
    'Recurring (Periodic)',
    'دوره‌ای',
    'Recurring (Periodic)',
    NULL,
    2,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100059,
    -1006,
    'Per Transaction',
    'به‌ازای هر تراکنش',
    'Per Transaction',
    NULL,
    3,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100060,
    -1006,
    'Event-triggered',
    'مبتنی بر رخداد',
    'Event-triggered',
    NULL,
    4,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100061,
    -1007,
    'Fixed Amount',
    'مبلغ ثابت',
    'Fixed Amount',
    NULL,
    1,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100062,
    -1007,
    'Percentage-Based',
    'مبتنی بر درصد',
    'Percentage-Based',
    NULL,
    2,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100063,
    -1007,
    'Tiered/Slab-Based',
    'پلکانی/بازه‌ای',
    'Tiered/Slab-Based',
    NULL,
    3,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100064,
    -1007,
    'Capped Fee',
    'کارمزد با سقف',
    'Capped Fee',
    NULL,
    4,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100065,
    -1007,
    'Minimum Fee',
    'کارمزد با حداقل',
    'Minimum Fee',
    NULL,
    5,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100066,
    -1008,
    'Flat',
    'مبلغ ثابت',
    'Flat',
    NULL,
    1,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100067,
    -1008,
    'PerUnit',
    'به‌ازای هر واحد',
    'PerUnit',
    NULL,
    2,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100068,
    -1008,
    'Percentage',
    'درصدی',
    'Percentage',
    NULL,
    3,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100069,
    -1008,
    'RatePerBrackets',
    'نرخ پلکانی',
    'RatePerBrackets',
    NULL,
    4,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100070,
    -1008,
    'RateWithMinimumAmount',
    'نرخ با حداقل مبلغ',
    'RateWithMinimumAmount',
    NULL,
    5,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100071,
    -1008,
    'RateWithMaximumAmount',
    'نرخ با حداکثر مبلغ',
    'RateWithMaximumAmount',
    NULL,
    6,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100072,
    -1008,
    'Other',
    'سایر',
    'Other',
    NULL,
    7,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100073,
    -1008,
    'GrossAmount',
    'مبلغ ناخالص',
    'GrossAmount',
    NULL,
    8,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100074,
    -1008,
    'NetAmount',
    'مبلغ خالص',
    'NetAmount',
    NULL,
    9,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100075,
    -1008,
    'NetAssetValuePrice',
    'ارزش خالص دارایی',
    'NetAssetValuePrice',
    NULL,
    10,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100076,
    -1009,
    'Fixed Currency',
    'ارز ثابت',
    'Fixed Currency',
    NULL,
    1,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100077,
    -1009,
    'Transaction Currency',
    'ارز تراکنش',
    'Transaction Currency',
    NULL,
    2,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100078,
    -1009,
    'Account Currency',
    'ارز حساب',
    'Account Currency',
    NULL,
    3,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100079,
    -1009,
    'Converted Currency',
    'ارز تبدیل‌شده',
    'Converted Currency',
    NULL,
    4,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100080,
    -1010,
    'Immediate Posting',
    'ثبت فوری',
    'Immediate Posting',
    NULL,
    1,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100081,
    -1010,
    'End-of-Day Posting',
    'ثبت پایان روز',
    'End-of-Day Posting',
    NULL,
    2,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100082,
    -1010,
    'Aggregated Posting',
    'ثبت تجمیعی',
    'Aggregated Posting',
    NULL,
    3,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100083,
    -1011,
    'ChargeBearer',
    'تحمل‌کننده هزینه',
    'ChargeBearer',
    NULL,
    1,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100084,
    -1011,
    'ChargeAgent',
    'عامل کارمزد',
    'ChargeAgent',
    NULL,
    2,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100085,
    -1011,
    'ChargeRecipient',
    'دریافت‌کننده کارمزد',
    'ChargeRecipient',
    NULL,
    3,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100086,
    -1011,
    'ChargeAccountAgent',
    'عامل حساب کارمزد',
    'ChargeAccountAgent',
    NULL,
    4,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100087,
    -1012,
    'Fee Application Timing Modality',
    'مدالیتی زمان‌بندی اعمال کارمزد',
    'Fee Application Timing Modality',
    NULL,
    1,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100088,
    -1012,
    'Fee Calculation Modality',
    'مدالیتی محاسبه کارمزد',
    'Fee Calculation Modality',
    NULL,
    2,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100089,
    -1012,
    'Fee Currency Modality',
    'مدالیتی ارزی کارمزد',
    'Fee Currency Modality',
    NULL,
    3,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100090,
    -1012,
    'Fee Posting Modality',
    'مدالیتی ثبت کارمزد',
    'Fee Posting Modality',
    NULL,
    4,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100091,
    -1013,
    'OnClosing',
    'هنگام بستن',
    'OnClosing',
    NULL,
    1,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100092,
    -1013,
    'OnOpening',
    'هنگام افتتاح',
    'OnOpening',
    NULL,
    2,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100093,
    -1013,
    'ChargingPeriod',
    'دوره شارژ',
    'ChargingPeriod',
    NULL,
    3,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100094,
    -1013,
    'Daily',
    'روزانه',
    'Daily',
    NULL,
    4,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100095,
    -1013,
    'PerItem',
    'به‌ازای هر قلم',
    'PerItem',
    NULL,
    5,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100096,
    -1013,
    'Monthly',
    'ماهانه',
    'Monthly',
    NULL,
    6,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100097,
    -1013,
    'OnAnniversary',
    'در سالگرد',
    'OnAnniversary',
    NULL,
    7,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100098,
    -1013,
    'Other',
    'سایر',
    'Other',
    NULL,
    8,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100099,
    -1013,
    'PerHundredPounds',
    'به‌ازای هر صد پوند',
    'PerHundredPounds',
    NULL,
    9,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100100,
    -1013,
    'PerHour',
    'به‌ازای هر ساعت',
    'PerHour',
    NULL,
    10,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100101,
    -1013,
    'PerOccurrence',
    'به‌ازای هر وقوع',
    'PerOccurrence',
    NULL,
    11,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100102,
    -1013,
    'PerSheet',
    'به‌ازای هر برگ/صفحه',
    'PerSheet',
    NULL,
    12,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100103,
    -1013,
    'PerTransaction',
    'به‌ازای هر تراکنش',
    'PerTransaction',
    NULL,
    13,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100104,
    -1013,
    'PerTransactionAmount',
    'براساس مبلغ تراکنش',
    'PerTransactionAmount',
    NULL,
    14,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100105,
    -1013,
    'PerTransactionPercentage',
    'درصدی از مبلغ تراکنش',
    'PerTransactionPercentage',
    NULL,
    15,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100106,
    -1013,
    'Quarterly',
    'فصلی',
    'Quarterly',
    NULL,
    16,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100107,
    -1013,
    'SixMonthly',
    'شش‌ماهه',
    'SixMonthly',
    NULL,
    17,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100108,
    -1013,
    'StatementMonthly',
    'ماهانه همراه صورتحساب',
    'StatementMonthly',
    NULL,
    18,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100109,
    -1013,
    'Weekly',
    'هفتگی',
    'Weekly',
    NULL,
    19,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100110,
    -1013,
    'Yearly',
    'سالانه',
    'Yearly',
    NULL,
    20,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100111,
    -1014,
    'OnClosing',
    'هنگام بستن',
    'OnClosing',
    NULL,
    1,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100112,
    -1014,
    'OnOpening',
    'هنگام افتتاح',
    'OnOpening',
    NULL,
    2,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100113,
    -1014,
    'ChargingPeriod',
    'دوره شارژ',
    'ChargingPeriod',
    NULL,
    3,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100114,
    -1014,
    'Daily',
    'روزانه',
    'Daily',
    NULL,
    4,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100115,
    -1014,
    'PerItem',
    'به‌ازای هر قلم',
    'PerItem',
    NULL,
    5,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100116,
    -1014,
    'Monthly',
    'ماهانه',
    'Monthly',
    NULL,
    6,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100117,
    -1014,
    'OnAnniversary',
    'در سالگرد',
    'OnAnniversary',
    NULL,
    7,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100118,
    -1014,
    'Other',
    'سایر',
    'Other',
    NULL,
    8,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100119,
    -1014,
    'PerHundredPounds',
    'به‌ازای هر صد پوند',
    'PerHundredPounds',
    NULL,
    9,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100120,
    -1014,
    'PerHour',
    'به‌ازای هر ساعت',
    'PerHour',
    NULL,
    10,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100121,
    -1014,
    'PerOccurrence',
    'به‌ازای هر وقوع',
    'PerOccurrence',
    NULL,
    11,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100122,
    -1014,
    'PerSheet',
    'به‌ازای هر برگ/صفحه',
    'PerSheet',
    NULL,
    12,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100123,
    -1014,
    'PerTransaction',
    'به‌ازای هر تراکنش',
    'PerTransaction',
    NULL,
    13,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100124,
    -1014,
    'PerTransactionAmount',
    'براساس مبلغ تراکنش',
    'PerTransactionAmount',
    NULL,
    14,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100125,
    -1014,
    'PerTransactionPercentage',
    'درصدی از مبلغ تراکنش',
    'PerTransactionPercentage',
    NULL,
    15,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100126,
    -1014,
    'Quarterly',
    'فصلی',
    'Quarterly',
    NULL,
    16,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100127,
    -1014,
    'SixMonthly',
    'شش‌ماهه',
    'SixMonthly',
    NULL,
    17,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100128,
    -1014,
    'StatementMonthly',
    'ماهانه همراه صورتحساب',
    'StatementMonthly',
    NULL,
    18,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100129,
    -1014,
    'Weekly',
    'هفتگی',
    'Weekly',
    NULL,
    19,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100130,
    -1014,
    'Yearly',
    'سالانه',
    'Yearly',
    NULL,
    20,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100131,
    -1015,
    'Fee Debit Account',
    'حساب برداشت کارمزد',
    'Fee Debit Account',
    NULL,
    1,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100132,
    -1016,
    'Standard Fee Plan',
    'طرح کارمزد استاندارد',
    'Standard Fee Plan',
    NULL,
    1,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100133,
    -1016,
    'Final Fee Plan',
    'طرح کارمزد نهایی',
    'Final Fee Plan',
    NULL,
    2,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100134,
    -1017,
    'REGULATORY',
    'مقرراتی',
    'REGULATORY',
    NULL,
    1,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100135,
    -1017,
    'BANK_STANDARD',
    'استاندارد بانک',
    'BANK_STANDARD',
    NULL,
    2,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100136,
    -1017,
    'BANK_SPECIAL',
    'ویژه بانک',
    'BANK_SPECIAL',
    NULL,
    3,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100137,
    -1017,
    'CAMPAIGN',
    'کمپین',
    'CAMPAIGN',
    NULL,
    4,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100138,
    -1018,
    'MAJOR',
    'اصلی',
    'MAJOR',
    NULL,
    1,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100139,
    -1018,
    'MINOR',
    'فرعی',
    'MINOR',
    NULL,
    2,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100140,
    -1018,
    'HOTFIX',
    'اصلاح فوری',
    'HOTFIX',
    NULL,
    3,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100141,
    -1019,
    'DRAFT',
    'پیش‌نویس',
    'DRAFT',
    NULL,
    1,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100142,
    -1019,
    'UNDER_REVIEW',
    'در حال بررسی',
    'UNDER_REVIEW',
    NULL,
    2,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100143,
    -1019,
    'APPROVED',
    'تأییدشده',
    'APPROVED',
    NULL,
    3,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100144,
    -1019,
    'ACTIVE',
    'فعال',
    'ACTIVE',
    NULL,
    4,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100145,
    -1019,
    'SUSPENDED',
    'تعلیق',
    'SUSPENDED',
    NULL,
    5,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100146,
    -1019,
    'RETIRED',
    'بازنشسته',
    'RETIRED',
    NULL,
    6,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100147,
    -1019,
    'APPLIED',
    'اعمال‌شده',
    'APPLIED',
    NULL,
    7,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100148,
    -1019,
    'CALCULATED',
    'محاسبه‌شده',
    'CALCULATED',
    NULL,
    8,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100149,
    -1019,
    'COLLECTED',
    'وصول‌شده',
    'COLLECTED',
    NULL,
    9,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100150,
    -1019,
    'PARTIALLY_REVERSED',
    'بخشی برگشت‌شده',
    'PARTIALLY_REVERSED',
    NULL,
    10,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100151,
    -1019,
    'REVERSED',
    'برگشت‌شده',
    'REVERSED',
    NULL,
    11,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100152,
    -1019,
    'REJECTED',
    'ردشده',
    'REJECTED',
    NULL,
    12,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100153,
    -1019,
    'COMPLETED',
    'تکمیل‌شده',
    'COMPLETED',
    NULL,
    13,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100154,
    -1020,
    'FIXED',
    'مبلغ ثابت',
    'FIXED',
    NULL,
    1,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100155,
    -1020,
    'PER_UNIT',
    'به‌ازای واحد',
    'PER_UNIT',
    NULL,
    2,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100156,
    -1020,
    'PERCENTAGE',
    'درصدی',
    'PERCENTAGE',
    NULL,
    3,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100157,
    -1020,
    'PERCENTAGE_WITH_FLOOR',
    'درصدی با کف',
    'PERCENTAGE_WITH_FLOOR',
    NULL,
    4,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100158,
    -1020,
    'PERCENTAGE_WITH_CAP',
    'درصدی با سقف',
    'PERCENTAGE_WITH_CAP',
    NULL,
    5,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100159,
    -1020,
    'PERCENTAGE_FLOOR_CAP',
    'درصدی با کف و سقف',
    'PERCENTAGE_FLOOR_CAP',
    NULL,
    6,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100160,
    -1020,
    'FIXED_PLUS_PERCENTAGE',
    'مبلغ ثابت به‌علاوه درصد',
    'FIXED_PLUS_PERCENTAGE',
    NULL,
    7,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100161,
    -1020,
    'TIERED',
    'پلکانی',
    'TIERED',
    NULL,
    8,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100162,
    -1020,
    'MARGINAL_TIERED',
    'پلکانی بر مبنای مازاد',
    'MARGINAL_TIERED',
    NULL,
    9,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100163,
    -1020,
    'ANNUALIZED_PERCENTAGE',
    'درصد سالانه',
    'ANNUALIZED_PERCENTAGE',
    NULL,
    10,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100164,
    -1020,
    'COMPOSITE',
    'ترکیبی',
    'COMPOSITE',
    NULL,
    11,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100165,
    -1020,
    'EXTERNAL_VALUE',
    'مقدار بیرونی',
    'EXTERNAL_VALUE',
    NULL,
    12,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100166,
    -1021,
    'WHOLE_AMOUNT',
    'کل مبلغ',
    'WHOLE_AMOUNT',
    NULL,
    1,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100167,
    -1021,
    'EXCESS_OVER_LOWER_BOUND',
    'مازاد بر کف بازه',
    'EXCESS_OVER_LOWER_BOUND',
    NULL,
    2,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100168,
    -1022,
    'HALF_UP',
    'گرد کردن نیم به بالا',
    'HALF_UP',
    NULL,
    1,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100169,
    -1022,
    'HALF_EVEN',
    'گرد کردن بانکی',
    'HALF_EVEN',
    NULL,
    2,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100170,
    -1022,
    'UP',
    'رو به بالا',
    'UP',
    NULL,
    3,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100171,
    -1022,
    'DOWN',
    'رو به پایین',
    'DOWN',
    NULL,
    4,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100172,
    -1023,
    'PERSON',
    'شخص حقیقی',
    'PERSON',
    NULL,
    1,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100173,
    -1023,
    'LEGAL_ENTITY',
    'شخص حقوقی',
    'LEGAL_ENTITY',
    NULL,
    2,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100174,
    -1023,
    'BANK',
    'بانک',
    'BANK',
    NULL,
    3,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100175,
    -1023,
    'BRANCH',
    'شعبه',
    'BRANCH',
    NULL,
    4,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100176,
    -1023,
    'EXTERNAL_ORG',
    'سازمان بیرونی',
    'EXTERNAL_ORG',
    NULL,
    5,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100177,
    -1024,
    'STANDARD',
    'عادی',
    'STANDARD',
    NULL,
    1,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100178,
    -1024,
    'VIP',
    'ارزنده/VIP',
    'VIP',
    NULL,
    2,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100179,
    -1024,
    'WELFARE_SUPPORT',
    'گروه حمایتی',
    'WELFARE_SUPPORT',
    NULL,
    3,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100180,
    -1024,
    'KNOWLEDGE_BASED',
    'دانش‌بنیان',
    'KNOWLEDGE_BASED',
    NULL,
    4,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100181,
    -1025,
    'RETAIL',
    'خرد',
    'RETAIL',
    NULL,
    1,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100182,
    -1025,
    'CORPORATE',
    'شرکتی',
    'CORPORATE',
    NULL,
    2,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100183,
    -1025,
    'STAFF',
    'کارکنان',
    'STAFF',
    NULL,
    3,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100184,
    -1025,
    'PARTNER',
    'همکار',
    'PARTNER',
    NULL,
    4,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100185,
    -1026,
    'CURRENT_ACCOUNT',
    'حساب جاری',
    'CURRENT_ACCOUNT',
    NULL,
    1,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100186,
    -1026,
    'GUARANTEE',
    'ضمانت‌نامه',
    'GUARANTEE',
    NULL,
    2,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100187,
    -1026,
    'FACILITY',
    'تسهیلات',
    'FACILITY',
    NULL,
    3,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100188,
    -1026,
    'SERVICE',
    'خدمت عمومی',
    'SERVICE',
    NULL,
    4,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100189,
    -1027,
    'TRANSACTION',
    'تراکنش',
    'TRANSACTION',
    NULL,
    1,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100190,
    -1027,
    'ISSUANCE',
    'صدور',
    'ISSUANCE',
    NULL,
    2,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100191,
    -1027,
    'APPRAISAL',
    'ارزیابی',
    'APPRAISAL',
    NULL,
    3,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100192,
    -1027,
    'LIMIT',
    'حد/تعهد',
    'LIMIT',
    NULL,
    4,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100193,
    -1027,
    'SERVICE',
    'خدمت',
    'SERVICE',
    NULL,
    5,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100194,
    -1028,
    'MANDATORY',
    'اجباری',
    'MANDATORY',
    NULL,
    1,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100195,
    -1028,
    'OPTIONAL',
    'اختیاری',
    'OPTIONAL',
    NULL,
    2,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100196,
    -1029,
    'STANDARD',
    'استاندارد',
    'STANDARD',
    NULL,
    1,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100197,
    -1029,
    'OFFER',
    'پیشنهاد',
    'OFFER',
    NULL,
    2,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100198,
    -1029,
    'NEGOTIATED',
    'توافقی',
    'NEGOTIATED',
    NULL,
    3,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100199,
    -1029,
    'EXCEPTIONAL',
    'استثنایی',
    'EXCEPTIONAL',
    NULL,
    4,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100200,
    -1030,
    'BANK_REVENUE',
    'درآمد بانک',
    'BANK_REVENUE',
    NULL,
    1,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100201,
    -1030,
    'PASS_THROUGH_COST',
    'هزینه عبوری',
    'PASS_THROUGH_COST',
    NULL,
    2,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100202,
    -1030,
    'TAX',
    'مالیات',
    'TAX',
    NULL,
    3,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100203,
    -1030,
    'STAMP',
    'تمبر',
    'STAMP',
    NULL,
    4,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100204,
    -1030,
    'POSTAGE',
    'پست',
    'POSTAGE',
    NULL,
    5,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100205,
    -1030,
    'EXTERNAL_SERVICE',
    'خدمت بیرونی',
    'EXTERNAL_SERVICE',
    NULL,
    6,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100206,
    -1030,
    'OTHER',
    'سایر',
    'OTHER',
    NULL,
    7,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100207,
    -1031,
    'REFUNDABLE',
    'قابل برگشت',
    'REFUNDABLE',
    NULL,
    1,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100208,
    -1031,
    'NON_REFUNDABLE',
    'غیرقابل برگشت',
    'NON_REFUNDABLE',
    NULL,
    2,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100209,
    -1031,
    'RULE_BASED',
    'قاعده‌محور',
    'RULE_BASED',
    NULL,
    3,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100210,
    -1032,
    'PERCENT',
    'درصدی',
    'PERCENT',
    NULL,
    1,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100211,
    -1032,
    'FIXED',
    'مبلغ ثابت',
    'FIXED',
    NULL,
    2,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100212,
    -1032,
    'HYBRID',
    'ترکیبی',
    'HYBRID',
    NULL,
    3,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100213,
    -1033,
    'PERCENT',
    'درصد',
    'PERCENT',
    NULL,
    1,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100214,
    -1033,
    'FIXED',
    'مبلغ ثابت',
    'FIXED',
    NULL,
    2,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100215,
    -1033,
    'RESIDUAL',
    'باقیمانده',
    'RESIDUAL',
    NULL,
    3,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100216,
    -1034,
    'TRANSFER',
    'انتقال',
    'TRANSFER',
    NULL,
    1,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100217,
    -1034,
    'NETTING',
    'خالص‌سازی',
    'NETTING',
    NULL,
    2,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100218,
    -1034,
    'INTERNAL',
    'داخلی',
    'INTERNAL',
    NULL,
    3,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100219,
    -1035,
    'AMOUNT_OVERRIDE',
    'جایگزینی مبلغ',
    'AMOUNT_OVERRIDE',
    NULL,
    1,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100220,
    -1035,
    'RATE_OVERRIDE',
    'جایگزینی نرخ',
    'RATE_OVERRIDE',
    NULL,
    2,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100221,
    -1035,
    'WAIVER',
    'معافیت',
    'WAIVER',
    NULL,
    3,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100222,
    -1035,
    'RULE_OVERRIDE',
    'جایگزینی قاعده',
    'RULE_OVERRIDE',
    NULL,
    4,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100223,
    -1036,
    'FULL',
    'کامل',
    'FULL',
    NULL,
    1,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100224,
    -1036,
    'PARTIAL',
    'جزئی',
    'PARTIAL',
    NULL,
    2,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100225,
    -1036,
    'RULE_BASED',
    'قاعده‌محور',
    'RULE_BASED',
    NULL,
    3,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100226,
    -1037,
    'INPUT',
    'ورودی',
    'INPUT',
    NULL,
    1,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100227,
    -1037,
    'RULE',
    'قاعده',
    'RULE',
    NULL,
    2,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100228,
    -1037,
    'APPROVAL',
    'مجوز',
    'APPROVAL',
    NULL,
    3,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100229,
    -1037,
    'POSTING',
    'ثبت',
    'POSTING',
    NULL,
    4,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100230,
    -1037,
    'REVERSAL',
    'برگشت',
    'REVERSAL',
    NULL,
    5,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100231,
    -1037,
    'OVERRIDE',
    'استثناء',
    'OVERRIDE',
    NULL,
    6,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100232,
    -1038,
    'OPERATOR',
    'عملگر',
    'OPERATOR',
    NULL,
    1,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100233,
    -1038,
    'INPUT',
    'ورودی',
    'INPUT',
    NULL,
    2,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100234,
    -1038,
    'CONSTANT',
    'ثابت',
    'CONSTANT',
    NULL,
    3,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100235,
    -1038,
    'EXTERNAL_VALUE',
    'مقدار بیرونی',
    'EXTERNAL_VALUE',
    NULL,
    4,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100236,
    -1038,
    'PREVIOUS_CHARGE',
    'مبلغ قبلی',
    'PREVIOUS_CHARGE',
    NULL,
    5,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100237,
    -1039,
    'ADD',
    'جمع',
    'ADD',
    NULL,
    1,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100238,
    -1039,
    'SUBTRACT',
    'تفریق',
    'SUBTRACT',
    NULL,
    2,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100239,
    -1039,
    'MULTIPLY',
    'ضرب',
    'MULTIPLY',
    NULL,
    3,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100240,
    -1039,
    'DIVIDE',
    'تقسیم',
    'DIVIDE',
    NULL,
    4,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100241,
    -1039,
    'MIN_OF',
    'کمینه',
    'MIN_OF',
    NULL,
    5,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100242,
    -1039,
    'MAX_OF',
    'بیشینه',
    'MAX_OF',
    NULL,
    6,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100243,
    -1039,
    'SUM_CHILDREN',
    'جمع فرزندان',
    'SUM_CHILDREN',
    NULL,
    7,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100244,
    -1039,
    'FLOOR',
    'کف',
    'FLOOR',
    NULL,
    8,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100245,
    -1039,
    'CAP',
    'سقف',
    'CAP',
    NULL,
    9,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100246,
    -1039,
    'PERIOD_FACTOR',
    'ضریب زمان',
    'PERIOD_FACTOR',
    NULL,
    10,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100247,
    -1040,
    'NUMBER',
    'عدد',
    'NUMBER',
    NULL,
    1,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100248,
    -1040,
    'TEXT',
    'متن',
    'TEXT',
    NULL,
    2,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100249,
    -1040,
    'DATE',
    'تاریخ',
    'DATE',
    NULL,
    3,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100250,
    -1040,
    'BOOLEAN',
    'منطقی',
    'BOOLEAN',
    NULL,
    4,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100251,
    -1040,
    'COLLECTION',
    'مجموعه',
    'COLLECTION',
    NULL,
    5,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100252,
    -1041,
    'IRR',
    'ریال',
    'IRR',
    NULL,
    1,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100253,
    -1041,
    'PERCENT',
    'درصد',
    'PERCENT',
    NULL,
    2,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100254,
    -1041,
    'DAY',
    'روز',
    'DAY',
    NULL,
    3,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100255,
    -1041,
    'ITEM',
    'قلم',
    'ITEM',
    NULL,
    4,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100256,
    -1041,
    'PAGE',
    'صفحه',
    'PAGE',
    NULL,
    5,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100257,
    -1041,
    'COUNT',
    'تعداد',
    'COUNT',
    NULL,
    6,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100258,
    -1041,
    'CUBIC_CM',
    'سانتی‌متر مکعب',
    'CUBIC_CM',
    NULL,
    7,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100259,
    -1042,
    'BRANCH',
    'شعبه',
    'BRANCH',
    NULL,
    1,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100260,
    -1042,
    'MOBILE',
    'همراه بانک',
    'MOBILE',
    NULL,
    2,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100261,
    -1042,
    'INTERNET',
    'اینترنت بانک',
    'INTERNET',
    NULL,
    3,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100262,
    -1042,
    'API',
    'API',
    'API',
    NULL,
    4,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100263,
    -1042,
    'BATCH',
    'پردازش گروهی',
    'BATCH',
    NULL,
    5,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100264,
    -1043,
    'MONEY_TRANSFER',
    'انتقال وجه',
    'MONEY_TRANSFER',
    NULL,
    1,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100265,
    -1043,
    'GUARANTEE_ISSUANCE',
    'صدور ضمانت‌نامه',
    'GUARANTEE_ISSUANCE',
    NULL,
    2,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100266,
    -1043,
    'APPRAISAL',
    'ارزیابی',
    'APPRAISAL',
    NULL,
    3,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100267,
    -1043,
    'BILL_COLLECTION',
    'وصول بروات',
    'BILL_COLLECTION',
    NULL,
    4,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100268,
    -1043,
    'FACILITY_UNUSED_LIMIT',
    'عدم استفاده از حد اعتباری',
    'FACILITY_UNUSED_LIMIT',
    NULL,
    5,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100269,
    -1043,
    'CREDIT_APPLICATION',
    'پذیرش درخواست تسهیلات',
    'CREDIT_APPLICATION',
    NULL,
    6,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100270,
    -1043,
    'CONTRACT_SIGNING',
    'انعقاد قرارداد',
    'CONTRACT_SIGNING',
    NULL,
    7,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100271,
    -1043,
    'CHEQUEBOOK_ISSUE',
    'صدور دسته‌چک',
    'CHEQUEBOOK_ISSUE',
    NULL,
    8,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100272,
    -1044,
    'TRANSFER',
    'انتقال',
    'TRANSFER',
    NULL,
    1,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100273,
    -1044,
    'ISSUANCE',
    'صدور',
    'ISSUANCE',
    NULL,
    2,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100274,
    -1044,
    'APPRAISAL',
    'ارزیابی',
    'APPRAISAL',
    NULL,
    3,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100275,
    -1044,
    'COLLECTION',
    'وصول',
    'COLLECTION',
    NULL,
    4,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100276,
    -1044,
    'COMMITMENT',
    'تعهد',
    'COMMITMENT',
    NULL,
    5,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100277,
    -1044,
    'APPLICATION',
    'درخواست',
    'APPLICATION',
    NULL,
    6,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100278,
    -1045,
    'IRR',
    'ریال ایران',
    'IRR',
    NULL,
    1,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100279,
    -1045,
    'USD',
    'دلار آمریکا',
    'USD',
    NULL,
    2,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100280,
    -1045,
    'EUR',
    'یورو',
    'EUR',
    NULL,
    3,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100281,
    -1046,
    'CURRENT',
    'جاری',
    'CURRENT',
    NULL,
    1,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100282,
    -1046,
    'FEE_INCOME',
    'درآمد کارمزد',
    'FEE_INCOME',
    NULL,
    2,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100283,
    -1046,
    'SETTLEMENT',
    'تسویه',
    'SETTLEMENT',
    NULL,
    3,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100284,
    -1046,
    'REFUND',
    'برگشت',
    'REFUND',
    NULL,
    4,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100285,
    -1047,
    'ACCOUNT_DEBIT',
    'برداشت از حساب',
    'ACCOUNT_DEBIT',
    NULL,
    1,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100286,
    -1047,
    'CASH',
    'نقدی',
    'CASH',
    NULL,
    2,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100287,
    -1047,
    'FROM_TRANSACTION',
    'کسر از مبلغ تراکنش',
    'FROM_TRANSACTION',
    NULL,
    3,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100288,
    -1047,
    'ACCRUAL',
    'تعهدی',
    'ACCRUAL',
    NULL,
    4,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100289,
    -1048,
    'REJECT_TRANSACTION',
    'رد تراکنش',
    'REJECT_TRANSACTION',
    NULL,
    1,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100290,
    -1048,
    'ACCRUE_FEE',
    'ثبت تعهد',
    'ACCRUE_FEE',
    NULL,
    2,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100291,
    -1048,
    'RETRY',
    'تلاش مجدد',
    'RETRY',
    NULL,
    3,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100292,
    -1049,
    'FIRST_MATCH',
    'اولین تطابق',
    'FIRST_MATCH',
    NULL,
    1,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100293,
    -1049,
    'BEST_BENEFIT',
    'بیشترین منفعت مشتری',
    'BEST_BENEFIT',
    NULL,
    2,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100294,
    -1049,
    'CUMULATIVE',
    'تجمیعی',
    'CUMULATIVE',
    NULL,
    3,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100295,
    -1049,
    'EXCLUSIVE',
    'انحصاری',
    'EXCLUSIVE',
    NULL,
    4,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100296,
    -1050,
    'STANDARD',
    'استاندارد',
    'STANDARD',
    NULL,
    1,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100297,
    -1050,
    'NEGOTIATED',
    'توافقی',
    'NEGOTIATED',
    NULL,
    2,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100298,
    -1050,
    'EXCEPTIONAL_FEE',
    'کارمزد استثنایی',
    'EXCEPTIONAL_FEE',
    NULL,
    3,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100299,
    -1050,
    'EXCEPTIONAL_FEE_DISCOUNT',
    'تخفیف استثنایی',
    'EXCEPTIONAL_FEE_DISCOUNT',
    NULL,
    4,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100300,
    -1050,
    'CAMPAIGN',
    'کمپین',
    'CAMPAIGN',
    NULL,
    5,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100301,
    -1051,
    'CHARGE_ACCOUNT',
    'حساب برداشت',
    'CHARGE_ACCOUNT',
    NULL,
    1,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100302,
    -1051,
    'CREDIT_ACCOUNT',
    'حساب بستانکار',
    'CREDIT_ACCOUNT',
    NULL,
    2,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100303,
    -1051,
    'REFUND_ACCOUNT',
    'حساب برگشت',
    'REFUND_ACCOUNT',
    NULL,
    3,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100304,
    -1051,
    'SETTLEMENT_ACCOUNT',
    'حساب تسویه',
    'SETTLEMENT_ACCOUNT',
    NULL,
    4,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100305,
    -1052,
    'BANK',
    'بانک',
    'BANK',
    NULL,
    1,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100306,
    -1052,
    'AGENT_BRANCH',
    'شعبه عامل',
    'AGENT_BRANCH',
    NULL,
    2,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REF_VALUE (
    VALUE_ID,
    DOMAIN_ID,
    VALUE_CODE,
    NAME_FA,
    NAME_EN,
    DESCRIPTION,
    SORT_ORDER,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -100307,
    -1052,
    'EXTERNAL_ORG',
    'سازمان بیرونی',
    'EXTERNAL_ORG',
    NULL,
    3,
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

COMMIT;
