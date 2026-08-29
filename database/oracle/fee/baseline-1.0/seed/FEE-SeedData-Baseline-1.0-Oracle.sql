-- ============================================================================
-- FEE-SeedData-Baseline-1.0-Oracle.sql
-- Consolidated Oracle seed data for all 47 Fee Baseline 1.0 tables
-- UTF-8; IDs are negative to avoid collision with operational sequences.
-- ============================================================================

PROMPT ===== 01_fee_reference_data.sql =====
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

PROMPT ===== 02_fee_demo_master_data.sql =====
-- ============================================================================
-- 02_fee_demo_master_data.sql
-- Fee Prototype Target Model Baseline 1.0 / Oracle
-- UTF-8
-- ============================================================================
SET DEFINE OFF;
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK;
ALTER SESSION SET CURRENT_SCHEMA = FEE;

PROMPT Loading FEE_DEMO_PARTY ...
INSERT INTO FEE_DEMO_PARTY (
    DEMO_PARTY_ID,
    PARTY_NO,
    PARTY_TYPE_CODE,
    NAME_FA,
    NAME_EN,
    CUSTOMER_SEGMENT_CODE,
    CUSTOMER_GROUP_CODE,
    KNOWLEDGE_BASED_FLAG,
    WELFARE_SUPPORT_FLAG,
    COUNTRY_CODE,
    STATUS_CODE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -2001,
    'BANK-MELLAT-DEMO',
    'BANK',
    'بانک ملت - نمونه',
    'Bank Mellat Demo',
    NULL,
    NULL,
    'N',
    'N',
    'IR',
    'ACTIVE',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_DEMO_PARTY (
    DEMO_PARTY_ID,
    PARTY_NO,
    PARTY_TYPE_CODE,
    NAME_FA,
    NAME_EN,
    CUSTOMER_SEGMENT_CODE,
    CUSTOMER_GROUP_CODE,
    KNOWLEDGE_BASED_FLAG,
    WELFARE_SUPPORT_FLAG,
    COUNTRY_CODE,
    STATUS_CODE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -2002,
    'BR-1001',
    'BRANCH',
    'شعبه مرکزی نمونه',
    'Demo Central Branch',
    NULL,
    NULL,
    'N',
    'N',
    'IR',
    'ACTIVE',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_DEMO_PARTY (
    DEMO_PARTY_ID,
    PARTY_NO,
    PARTY_TYPE_CODE,
    NAME_FA,
    NAME_EN,
    CUSTOMER_SEGMENT_CODE,
    CUSTOMER_GROUP_CODE,
    KNOWLEDGE_BASED_FLAG,
    WELFARE_SUPPORT_FLAG,
    COUNTRY_CODE,
    STATUS_CODE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -2003,
    'CUST-1001',
    'PERSON',
    'مشتری حقیقی عادی',
    'Standard Individual',
    'STANDARD',
    'RETAIL',
    'N',
    'N',
    'IR',
    'ACTIVE',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_DEMO_PARTY (
    DEMO_PARTY_ID,
    PARTY_NO,
    PARTY_TYPE_CODE,
    NAME_FA,
    NAME_EN,
    CUSTOMER_SEGMENT_CODE,
    CUSTOMER_GROUP_CODE,
    KNOWLEDGE_BASED_FLAG,
    WELFARE_SUPPORT_FLAG,
    COUNTRY_CODE,
    STATUS_CODE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -2004,
    'CUST-1002',
    'PERSON',
    'مشتری حقیقی VIP',
    'VIP Individual',
    'VIP',
    'RETAIL',
    'N',
    'N',
    'IR',
    'ACTIVE',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_DEMO_PARTY (
    DEMO_PARTY_ID,
    PARTY_NO,
    PARTY_TYPE_CODE,
    NAME_FA,
    NAME_EN,
    CUSTOMER_SEGMENT_CODE,
    CUSTOMER_GROUP_CODE,
    KNOWLEDGE_BASED_FLAG,
    WELFARE_SUPPORT_FLAG,
    COUNTRY_CODE,
    STATUS_CODE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -2005,
    'CUST-2001',
    'LEGAL_ENTITY',
    'شرکت دانش‌بنیان نمونه',
    'Demo Knowledge-based Co.',
    'KNOWLEDGE_BASED',
    'CORPORATE',
    'Y',
    'N',
    'IR',
    'ACTIVE',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_DEMO_PARTY (
    DEMO_PARTY_ID,
    PARTY_NO,
    PARTY_TYPE_CODE,
    NAME_FA,
    NAME_EN,
    CUSTOMER_SEGMENT_CODE,
    CUSTOMER_GROUP_CODE,
    KNOWLEDGE_BASED_FLAG,
    WELFARE_SUPPORT_FLAG,
    COUNTRY_CODE,
    STATUS_CODE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -2006,
    'CUST-1003',
    'PERSON',
    'مشتری مشمول حمایت',
    'Welfare Customer',
    'WELFARE_SUPPORT',
    'RETAIL',
    'N',
    'Y',
    'IR',
    'ACTIVE',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_DEMO_PARTY (
    DEMO_PARTY_ID,
    PARTY_NO,
    PARTY_TYPE_CODE,
    NAME_FA,
    NAME_EN,
    CUSTOMER_SEGMENT_CODE,
    CUSTOMER_GROUP_CODE,
    KNOWLEDGE_BASED_FLAG,
    WELFARE_SUPPORT_FLAG,
    COUNTRY_CODE,
    STATUS_CODE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -2007,
    'ORG-POST',
    'EXTERNAL_ORG',
    'شرکت پست نمونه',
    'Demo Postal Partner',
    NULL,
    'PARTNER',
    'N',
    'N',
    'IR',
    'ACTIVE',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

PROMPT Loading FEE_DEMO_PRODUCT ...
INSERT INTO FEE_DEMO_PRODUCT (
    DEMO_PRODUCT_ID,
    PRODUCT_CODE,
    NAME_FA,
    NAME_EN,
    PRODUCT_TYPE_CODE,
    DEFAULT_CURRENCY_CODE,
    STATUS_CODE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -3001,
    'CURRENT-STD',
    'حساب جاری استاندارد نمونه',
    'Demo Standard Current Account',
    'CURRENT_ACCOUNT',
    'IRR',
    'ACTIVE',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_DEMO_PRODUCT (
    DEMO_PRODUCT_ID,
    PRODUCT_CODE,
    NAME_FA,
    NAME_EN,
    PRODUCT_TYPE_CODE,
    DEFAULT_CURRENCY_CODE,
    STATUS_CODE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -3002,
    'GUARANTEE-STD',
    'ضمانت‌نامه استاندارد نمونه',
    'Demo Guarantee',
    'GUARANTEE',
    'IRR',
    'ACTIVE',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_DEMO_PRODUCT (
    DEMO_PRODUCT_ID,
    PRODUCT_CODE,
    NAME_FA,
    NAME_EN,
    PRODUCT_TYPE_CODE,
    DEFAULT_CURRENCY_CODE,
    STATUS_CODE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -3003,
    'FACILITY-STD',
    'تسهیلات استاندارد نمونه',
    'Demo Facility',
    'FACILITY',
    'IRR',
    'ACTIVE',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_DEMO_PRODUCT (
    DEMO_PRODUCT_ID,
    PRODUCT_CODE,
    NAME_FA,
    NAME_EN,
    PRODUCT_TYPE_CODE,
    DEFAULT_CURRENCY_CODE,
    STATUS_CODE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -3004,
    'SERVICE-GENERIC',
    'خدمت عمومی نمونه',
    'Demo Generic Service',
    'SERVICE',
    'IRR',
    'ACTIVE',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

PROMPT Loading FEE_DEMO_PRODUCT_FEATURE ...
INSERT INTO FEE_DEMO_PRODUCT_FEATURE (
    DEMO_PRODUCT_FEATURE_ID,
    DEMO_PRODUCT_ID,
    FEATURE_CODE,
    NAME_FA,
    NAME_EN,
    FEATURE_TYPE_CODE,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -3101,
    -3001,
    'TRANSFER',
    'انتقال وجه',
    'Money Transfer',
    'TRANSACTION',
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_DEMO_PRODUCT_FEATURE (
    DEMO_PRODUCT_FEATURE_ID,
    DEMO_PRODUCT_ID,
    FEATURE_CODE,
    NAME_FA,
    NAME_EN,
    FEATURE_TYPE_CODE,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -3102,
    -3001,
    'CHEQUEBOOK',
    'صدور دسته‌چک',
    'Cheque Book Issuance',
    'ISSUANCE',
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_DEMO_PRODUCT_FEATURE (
    DEMO_PRODUCT_FEATURE_ID,
    DEMO_PRODUCT_ID,
    FEATURE_CODE,
    NAME_FA,
    NAME_EN,
    FEATURE_TYPE_CODE,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -3103,
    -3002,
    'GUARANTEE_ISSUE',
    'صدور ضمانت‌نامه',
    'Guarantee Issuance',
    'ISSUANCE',
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_DEMO_PRODUCT_FEATURE (
    DEMO_PRODUCT_FEATURE_ID,
    DEMO_PRODUCT_ID,
    FEATURE_CODE,
    NAME_FA,
    NAME_EN,
    FEATURE_TYPE_CODE,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -3104,
    -3003,
    'UNUSED_LIMIT',
    'حد اعتباری استفاده‌نشده',
    'Unused Credit Limit',
    'LIMIT',
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_DEMO_PRODUCT_FEATURE (
    DEMO_PRODUCT_FEATURE_ID,
    DEMO_PRODUCT_ID,
    FEATURE_CODE,
    NAME_FA,
    NAME_EN,
    FEATURE_TYPE_CODE,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -3105,
    -3003,
    'CREDIT_APPLICATION',
    'کارشناسی درخواست تسهیلات',
    'Credit Application Appraisal',
    'APPRAISAL',
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_DEMO_PRODUCT_FEATURE (
    DEMO_PRODUCT_FEATURE_ID,
    DEMO_PRODUCT_ID,
    FEATURE_CODE,
    NAME_FA,
    NAME_EN,
    FEATURE_TYPE_CODE,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -3106,
    -3004,
    'APPRAISAL',
    'ارزیابی اموال',
    'Property Appraisal',
    'APPRAISAL',
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_DEMO_PRODUCT_FEATURE (
    DEMO_PRODUCT_FEATURE_ID,
    DEMO_PRODUCT_ID,
    FEATURE_CODE,
    NAME_FA,
    NAME_EN,
    FEATURE_TYPE_CODE,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -3107,
    -3004,
    'BILL_COLLECTION',
    'وصول بروات',
    'Bill Collection',
    'SERVICE',
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

PROMPT Loading FEE_DEMO_ACCOUNT ...
INSERT INTO FEE_DEMO_ACCOUNT (
    DEMO_ACCOUNT_ID,
    ACCOUNT_NO,
    DEMO_PARTY_ID,
    DEMO_PRODUCT_ID,
    ACCOUNT_TYPE_CODE,
    CURRENCY_CODE,
    STATUS_CODE,
    AVAILABLE_BALANCE,
    OPEN_DATE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -3201,
    '1001-001-IRR',
    -2003,
    -3001,
    'CURRENT',
    'IRR',
    'ACTIVE',
    1500000000,
    DATE '2025-01-01',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_DEMO_ACCOUNT (
    DEMO_ACCOUNT_ID,
    ACCOUNT_NO,
    DEMO_PARTY_ID,
    DEMO_PRODUCT_ID,
    ACCOUNT_TYPE_CODE,
    CURRENCY_CODE,
    STATUS_CODE,
    AVAILABLE_BALANCE,
    OPEN_DATE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -3202,
    '1002-001-IRR',
    -2004,
    -3001,
    'CURRENT',
    'IRR',
    'ACTIVE',
    3500000000,
    DATE '2025-01-01',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_DEMO_ACCOUNT (
    DEMO_ACCOUNT_ID,
    ACCOUNT_NO,
    DEMO_PARTY_ID,
    DEMO_PRODUCT_ID,
    ACCOUNT_TYPE_CODE,
    CURRENCY_CODE,
    STATUS_CODE,
    AVAILABLE_BALANCE,
    OPEN_DATE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -3203,
    '2001-001-IRR',
    -2005,
    -3001,
    'CURRENT',
    'IRR',
    'ACTIVE',
    9000000000,
    DATE '2025-01-01',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_DEMO_ACCOUNT (
    DEMO_ACCOUNT_ID,
    ACCOUNT_NO,
    DEMO_PARTY_ID,
    DEMO_PRODUCT_ID,
    ACCOUNT_TYPE_CODE,
    CURRENCY_CODE,
    STATUS_CODE,
    AVAILABLE_BALANCE,
    OPEN_DATE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -3204,
    '9999-FEE-INCOME',
    -2001,
    NULL,
    'FEE_INCOME',
    'IRR',
    'ACTIVE',
    0,
    DATE '2025-01-01',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_DEMO_ACCOUNT (
    DEMO_ACCOUNT_ID,
    ACCOUNT_NO,
    DEMO_PARTY_ID,
    DEMO_PRODUCT_ID,
    ACCOUNT_TYPE_CODE,
    CURRENCY_CODE,
    STATUS_CODE,
    AVAILABLE_BALANCE,
    OPEN_DATE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -3205,
    '9999-SETTLEMENT',
    -2001,
    NULL,
    'SETTLEMENT',
    'IRR',
    'ACTIVE',
    0,
    DATE '2025-01-01',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_DEMO_ACCOUNT (
    DEMO_ACCOUNT_ID,
    ACCOUNT_NO,
    DEMO_PARTY_ID,
    DEMO_PRODUCT_ID,
    ACCOUNT_TYPE_CODE,
    CURRENCY_CODE,
    STATUS_CODE,
    AVAILABLE_BALANCE,
    OPEN_DATE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -3206,
    'POST-SETTLEMENT',
    -2007,
    NULL,
    'SETTLEMENT',
    'IRR',
    'ACTIVE',
    0,
    DATE '2025-01-01',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

PROMPT Loading FEE_DEMO_FX_RATE ...
INSERT INTO FEE_DEMO_FX_RATE (
    DEMO_FX_RATE_ID,
    FROM_CURRENCY_CODE,
    TO_CURRENCY_CODE,
    RATE_TYPE_CODE,
    RATE_VALUE,
    QUOTED_AT,
    EFFECTIVE_FROM,
    SOURCE_CODE,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY
) VALUES (
    -3301,
    'USD',
    'IRR',
    'DEMO_STATIC',
    750000,
    TIMESTAMP '2026-08-29 09:00:00',
    TIMESTAMP '2026-08-29 00:00:00',
    'DEMO',
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0'
);

INSERT INTO FEE_DEMO_FX_RATE (
    DEMO_FX_RATE_ID,
    FROM_CURRENCY_CODE,
    TO_CURRENCY_CODE,
    RATE_TYPE_CODE,
    RATE_VALUE,
    QUOTED_AT,
    EFFECTIVE_FROM,
    SOURCE_CODE,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY
) VALUES (
    -3302,
    'EUR',
    'IRR',
    'DEMO_STATIC',
    850000,
    TIMESTAMP '2026-08-29 09:00:00',
    TIMESTAMP '2026-08-29 00:00:00',
    'DEMO',
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0'
);

INSERT INTO FEE_DEMO_FX_RATE (
    DEMO_FX_RATE_ID,
    FROM_CURRENCY_CODE,
    TO_CURRENCY_CODE,
    RATE_TYPE_CODE,
    RATE_VALUE,
    QUOTED_AT,
    EFFECTIVE_FROM,
    SOURCE_CODE,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY
) VALUES (
    -3303,
    'IRR',
    'USD',
    'DEMO_STATIC',
    1.33333333333e-06,
    TIMESTAMP '2026-08-29 09:00:00',
    TIMESTAMP '2026-08-29 00:00:00',
    'DEMO',
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0'
);

COMMIT;

PROMPT ===== 03_fee_policy_regulatory_data.sql =====
-- ============================================================================
-- 03_fee_policy_regulatory_data.sql
-- Fee Prototype Target Model Baseline 1.0 / Oracle
-- UTF-8
-- ============================================================================
SET DEFINE OFF;
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK;
ALTER SESSION SET CURRENT_SCHEMA = FEE;

PROMPT Loading FEE_FEATURE ...
INSERT INTO FEE_FEATURE (
    FEE_FEATURE_ID,
    FEATURE_CODE,
    NAME_FA,
    NAME_EN,
    FEE_TYPE_CODE,
    DEFAULT_CATEGORY_CODE,
    DESCRIPTION,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -4001,
    'TRANSFER_FEE',
    'کارمزد انتقال',
    'Transfer Fee',
    'TransferFee',
    'TRANSFER',
    'ویژگی کارمزدی برای پروتوتایپ مستقل',
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_FEATURE (
    FEE_FEATURE_ID,
    FEATURE_CODE,
    NAME_FA,
    NAME_EN,
    FEE_TYPE_CODE,
    DEFAULT_CATEGORY_CODE,
    DESCRIPTION,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -4002,
    'GUARANTEE_FEE',
    'کارمزد ضمانت‌نامه',
    'Guarantee Fee',
    'ServiceProvisionFee',
    'GUARANTEE',
    'ویژگی کارمزدی برای پروتوتایپ مستقل',
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_FEATURE (
    FEE_FEATURE_ID,
    FEATURE_CODE,
    NAME_FA,
    NAME_EN,
    FEE_TYPE_CODE,
    DEFAULT_CATEGORY_CODE,
    DESCRIPTION,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -4003,
    'APPRAISAL_FEE',
    'کارمزد ارزیابی',
    'Appraisal Fee',
    'AdvisoryFee',
    'APPRAISAL',
    'ویژگی کارمزدی برای پروتوتایپ مستقل',
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_FEATURE (
    FEE_FEATURE_ID,
    FEATURE_CODE,
    NAME_FA,
    NAME_EN,
    FEE_TYPE_CODE,
    DEFAULT_CATEGORY_CODE,
    DESCRIPTION,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -4004,
    'COLLECTION_FEE',
    'کارمزد وصول بروات',
    'Collection Fee',
    'ServiceProvisionFee',
    'COLLECTION',
    'ویژگی کارمزدی برای پروتوتایپ مستقل',
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_FEATURE (
    FEE_FEATURE_ID,
    FEATURE_CODE,
    NAME_FA,
    NAME_EN,
    FEE_TYPE_CODE,
    DEFAULT_CATEGORY_CODE,
    DESCRIPTION,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -4005,
    'COMMITMENT_FEE',
    'کارمزد تعهد',
    'Commitment Fee',
    'Other',
    'FACILITY',
    'ویژگی کارمزدی برای پروتوتایپ مستقل',
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_FEATURE (
    FEE_FEATURE_ID,
    FEATURE_CODE,
    NAME_FA,
    NAME_EN,
    FEE_TYPE_CODE,
    DEFAULT_CATEGORY_CODE,
    DESCRIPTION,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -4006,
    'CREDIT_EXPERTISE_FEE',
    'هزینه کارشناسی تسهیلات',
    'Credit Expertise Fee',
    'AdvisoryFee',
    'FACILITY',
    'ویژگی کارمزدی برای پروتوتایپ مستقل',
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_FEATURE (
    FEE_FEATURE_ID,
    FEATURE_CODE,
    NAME_FA,
    NAME_EN,
    FEE_TYPE_CODE,
    DEFAULT_CATEGORY_CODE,
    DESCRIPTION,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -4007,
    'CHEQUEBOOK_ISSUANCE_FEE',
    'کارمزد صدور دسته‌چک',
    'Cheque Book Issuance Fee',
    'IssuanceFee',
    'ACCOUNT',
    'ویژگی کارمزدی برای پروتوتایپ مستقل',
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_FEATURE (
    FEE_FEATURE_ID,
    FEATURE_CODE,
    NAME_FA,
    NAME_EN,
    FEE_TYPE_CODE,
    DEFAULT_CATEGORY_CODE,
    DESCRIPTION,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -4008,
    'TIERED_SERVICE_FEE',
    'کارمزد پلکانی نمونه',
    'Demo Tiered Service Fee',
    'ServiceProvisionFee',
    'SERVICE',
    'ویژگی کارمزدی برای پروتوتایپ مستقل',
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

PROMPT Loading FEE_REGULATORY_SOURCE ...
INSERT INTO FEE_REGULATORY_SOURCE (
    REGULATORY_SOURCE_ID,
    SOURCE_CODE,
    SOURCE_TYPE_CODE,
    ISSUER_CODE,
    CIRCULAR_NO,
    TITLE_FA,
    TITLE_EN,
    ISSUE_DATE,
    EFFECTIVE_FROM,
    DOCUMENT_REF,
    STATUS_CODE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -6001,
    'CBI_FEE_1404_04_35500',
    'CIRCULAR',
    'CBI',
    '04/35500',
    'کارمزد خدمات بانکی ریالی و الکترونیکی - سال ۱۴۰۴',
    'CBI Banking Fee Circular 1404',
    DATE '2025-05-04',
    DATE '2025-05-04',
    'cbi-fee-1404.pdf',
    'ACTIVE',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

PROMPT Loading FEE_POLICY_SET ...
INSERT INTO FEE_POLICY_SET (
    POLICY_SET_ID,
    POLICY_CODE,
    NAME_FA,
    NAME_EN,
    POLICY_TYPE_CODE,
    DESCRIPTION,
    OWNER_ORG_CODE,
    STATUS_CODE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -5001,
    'CBI_RIAL_ELECTRONIC_1404',
    'سیاست مقرراتی کارمزد بانک مرکزی ۱۴۰۴',
    'CBI Fee Policy 1404',
    'REGULATORY',
    'مبنای مقرراتی بخشنامه 04/35500',
    'CBI',
    'ACTIVE',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_POLICY_SET (
    POLICY_SET_ID,
    POLICY_CODE,
    NAME_FA,
    NAME_EN,
    POLICY_TYPE_CODE,
    DESCRIPTION,
    OWNER_ORG_CODE,
    STATUS_CODE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -5002,
    'MELLAT_PROTO_STANDARD_2026',
    'سیاست استاندارد کارمزد پروتوتایپ',
    'Prototype Standard Fee Policy',
    'BANK_STANDARD',
    'سیاست آزمایشی مستقل از سایر ماژول‌ها',
    'FEE_PROTO',
    'ACTIVE',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

PROMPT Loading FEE_POLICY_VERSION ...
INSERT INTO FEE_POLICY_VERSION (
    POLICY_VERSION_ID,
    POLICY_SET_ID,
    VERSION_NO,
    VERSION_TYPE_CODE,
    STATUS_CODE,
    EFFECTIVE_FROM,
    APPROVED_AT,
    APPROVED_BY,
    ACTIVATED_AT,
    ACTIVATED_BY,
    POLICY_HASH,
    CHANGE_SUMMARY,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -5101,
    -5001,
    '1404.1',
    'MAJOR',
    'ACTIVE',
    DATE '2025-05-04',
    TIMESTAMP '2025-05-04 09:00:00',
    'CBI',
    TIMESTAMP '2025-05-04 09:00:00',
    'CBI',
    'CBI-04-35500-1404',
    'Seed policy based on attached CBI circular',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_POLICY_VERSION (
    POLICY_VERSION_ID,
    POLICY_SET_ID,
    VERSION_NO,
    VERSION_TYPE_CODE,
    STATUS_CODE,
    EFFECTIVE_FROM,
    APPROVED_AT,
    APPROVED_BY,
    ACTIVATED_AT,
    ACTIVATED_BY,
    POLICY_HASH,
    CHANGE_SUMMARY,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -5102,
    -5002,
    '1.0',
    'MAJOR',
    'ACTIVE',
    DATE '2026-01-01',
    TIMESTAMP '2026-01-01 08:00:00',
    'PROTO_ADMIN',
    TIMESTAMP '2026-01-01 08:30:00',
    'PROTO_ADMIN',
    'PROTO-STANDARD-2026-V1',
    'Initial standalone fee prototype policy',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

PROMPT Loading FEE_REGULATORY_DISCOUNT_LIMIT ...
INSERT INTO FEE_REGULATORY_DISCOUNT_LIMIT (
    REG_DISCOUNT_LIMIT_ID,
    REGULATORY_SOURCE_ID,
    SEGMENT_CODE,
    PARTY_TYPE_CODE,
    MAX_DISCOUNT_RATE,
    EFFECTIVE_FROM,
    DESCRIPTION,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -6101,
    -6001,
    'ALL_CUSTOMERS',
    NULL,
    0.3,
    DATE '2025-05-04',
    'حداکثر تخفیف عمومی ۳۰ درصد',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REGULATORY_DISCOUNT_LIMIT (
    REG_DISCOUNT_LIMIT_ID,
    REGULATORY_SOURCE_ID,
    SEGMENT_CODE,
    PARTY_TYPE_CODE,
    MAX_DISCOUNT_RATE,
    EFFECTIVE_FROM,
    DESCRIPTION,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -6102,
    -6001,
    'WELFARE_SUPPORT',
    'PERSON',
    1,
    DATE '2025-05-04',
    'حداکثر تخفیف گروه‌های حمایتی ۱۰۰ درصد',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_REGULATORY_DISCOUNT_LIMIT (
    REG_DISCOUNT_LIMIT_ID,
    REGULATORY_SOURCE_ID,
    SEGMENT_CODE,
    PARTY_TYPE_CODE,
    MAX_DISCOUNT_RATE,
    EFFECTIVE_FROM,
    DESCRIPTION,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -6103,
    -6001,
    'KNOWLEDGE_BASED',
    'LEGAL_ENTITY',
    0.4,
    DATE '2025-05-04',
    'حداکثر تخفیف شرکت‌های دانش‌بنیان ۴۰ درصد',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

COMMIT;

PROMPT ===== 04_fee_configuration_data.sql =====
-- ============================================================================
-- 04_fee_configuration_data.sql
-- Fee Prototype Target Model Baseline 1.0 / Oracle
-- UTF-8
-- ============================================================================
SET DEFINE OFF;
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK;
ALTER SESSION SET CURRENT_SCHEMA = FEE;

PROMPT Loading FEE_DEFINITION ...
INSERT INTO FEE_DEFINITION (
    FEE_DEFINITION_ID,
    FEE_FEATURE_ID,
    FEE_CODE,
    NAME_FA,
    NAME_EN,
    CATEGORY_CODE,
    CLASSIFICATION_CODE,
    DESCRIPTION,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -7001,
    -4001,
    'DEMO_TRANSFER_FEE',
    'کارمزد انتقال وجه نمونه',
    'Demo Transfer Fee',
    'TRANSFER',
    'FEE',
    'تعریف داده اولیه برای پروتوتایپ',
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_DEFINITION (
    FEE_DEFINITION_ID,
    FEE_FEATURE_ID,
    FEE_CODE,
    NAME_FA,
    NAME_EN,
    CATEGORY_CODE,
    CLASSIFICATION_CODE,
    DESCRIPTION,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -7002,
    -4002,
    'CBI_GUARANTEE_DEPOSIT_FEE',
    'کارمزد صدور ضمانت‌نامه در مقابل سپرده',
    'CBI Guarantee Deposit Fee',
    'GUARANTEE',
    'FEE',
    'تعریف داده اولیه برای پروتوتایپ',
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_DEFINITION (
    FEE_DEFINITION_ID,
    FEE_FEATURE_ID,
    FEE_CODE,
    NAME_FA,
    NAME_EN,
    CATEGORY_CODE,
    CLASSIFICATION_CODE,
    DESCRIPTION,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -7003,
    -4003,
    'CBI_PROPERTY_APPRAISAL_FEE',
    'کارمزد ارزیابی املاک و ساختمان',
    'CBI Property Appraisal Fee',
    'APPRAISAL',
    'FEE',
    'تعریف داده اولیه برای پروتوتایپ',
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_DEFINITION (
    FEE_DEFINITION_ID,
    FEE_FEATURE_ID,
    FEE_CODE,
    NAME_FA,
    NAME_EN,
    CATEGORY_CODE,
    CLASSIFICATION_CODE,
    DESCRIPTION,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -7004,
    -4004,
    'CBI_BILL_COLLECTION_FEE',
    'کارمزد وصول سفته و برات',
    'CBI Bill Collection Fee',
    'COLLECTION',
    'FEE',
    'تعریف داده اولیه برای پروتوتایپ',
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_DEFINITION (
    FEE_DEFINITION_ID,
    FEE_FEATURE_ID,
    FEE_CODE,
    NAME_FA,
    NAME_EN,
    CATEGORY_CODE,
    CLASSIFICATION_CODE,
    DESCRIPTION,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -7005,
    -4005,
    'CBI_UNUSED_LIMIT_COMMITMENT_FEE',
    'کارمزد عدم استفاده از حد اعتباری',
    'CBI Commitment Fee',
    'FACILITY',
    'FEE',
    'تعریف داده اولیه برای پروتوتایپ',
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_DEFINITION (
    FEE_DEFINITION_ID,
    FEE_FEATURE_ID,
    FEE_CODE,
    NAME_FA,
    NAME_EN,
    CATEGORY_CODE,
    CLASSIFICATION_CODE,
    DESCRIPTION,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -7006,
    -4006,
    'CBI_CREDIT_EXPERTISE_FEE',
    'هزینه کارشناسی طرح تسهیلات',
    'CBI Credit Expertise Fee',
    'FACILITY',
    'FEE',
    'تعریف داده اولیه برای پروتوتایپ',
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_DEFINITION (
    FEE_DEFINITION_ID,
    FEE_FEATURE_ID,
    FEE_CODE,
    NAME_FA,
    NAME_EN,
    CATEGORY_CODE,
    CLASSIFICATION_CODE,
    DESCRIPTION,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -7007,
    -4007,
    'CBI_CHEQUEBOOK_ISSUE_FEE',
    'کارمزد صدور دسته‌چک',
    'CBI Cheque Book Issuance Fee',
    'ACCOUNT',
    'FEE',
    'تعریف داده اولیه برای پروتوتایپ',
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_DEFINITION (
    FEE_DEFINITION_ID,
    FEE_FEATURE_ID,
    FEE_CODE,
    NAME_FA,
    NAME_EN,
    CATEGORY_CODE,
    CLASSIFICATION_CODE,
    DESCRIPTION,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -7008,
    -4008,
    'DEMO_TIERED_SERVICE_FEE',
    'کارمزد پلکانی نمونه',
    'Demo Tiered Service Fee',
    'SERVICE',
    'FEE',
    'تعریف داده اولیه برای پروتوتایپ',
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

PROMPT Loading FEE_DEFINITION_VERSION ...
INSERT INTO FEE_DEFINITION_VERSION (
    FEE_DEFINITION_VERSION_ID,
    FEE_DEFINITION_ID,
    POLICY_VERSION_ID,
    REGULATORY_SOURCE_ID,
    REGULATORY_TARIFF_CODE,
    VERSION_NO,
    VERSION_TYPE_CODE,
    STATUS_CODE,
    FEE_PLAN_NAME,
    FEE_PLAN_TYPE_CODE,
    DEFAULT_BEARER_TYPE_CODE,
    DEFAULT_DEBIT_CREDIT_CODE,
    DEFAULT_DIRECTION_CODE,
    FEE_REASON,
    EFFECTIVE_FROM,
    APPROVED_AT,
    APPROVED_BY,
    ACTIVATED_AT,
    ACTIVATED_BY,
    CONFIG_HASH,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -7101,
    -7001,
    -5102,
    NULL,
    NULL,
    '1.0',
    'MAJOR',
    'ACTIVE',
    'کارمزد انتقال وجه نمونه',
    'Standard Fee Plan',
    'BorneByDebtor',
    'Debit',
    'Added',
    'Seed baseline 1.0',
    DATE '2026-01-01',
    TIMESTAMP '2026-01-01 08:00:00',
    'SEED_ADMIN',
    TIMESTAMP '2026-01-01 08:30:00',
    'SEED_ADMIN',
    'SEED-DEMO_TRANSFER_FEE-V1',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_DEFINITION_VERSION (
    FEE_DEFINITION_VERSION_ID,
    FEE_DEFINITION_ID,
    POLICY_VERSION_ID,
    REGULATORY_SOURCE_ID,
    REGULATORY_TARIFF_CODE,
    VERSION_NO,
    VERSION_TYPE_CODE,
    STATUS_CODE,
    FEE_PLAN_NAME,
    FEE_PLAN_TYPE_CODE,
    DEFAULT_BEARER_TYPE_CODE,
    DEFAULT_DEBIT_CREDIT_CODE,
    DEFAULT_DIRECTION_CODE,
    FEE_REASON,
    EFFECTIVE_FROM,
    APPROVED_AT,
    APPROVED_BY,
    ACTIVATED_AT,
    ACTIVATED_BY,
    CONFIG_HASH,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -7102,
    -7002,
    -5101,
    -6001,
    '1-2',
    '1.0',
    'MAJOR',
    'ACTIVE',
    'کارمزد صدور ضمانت‌نامه در مقابل سپرده',
    'Standard Fee Plan',
    'BorneByDebtor',
    'Debit',
    'Added',
    'Seed baseline 1.0',
    DATE '2025-05-04',
    TIMESTAMP '2026-01-01 08:00:00',
    'SEED_ADMIN',
    TIMESTAMP '2026-01-01 08:30:00',
    'SEED_ADMIN',
    'SEED-CBI_GUARANTEE_DEPOSIT_FEE-V1',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_DEFINITION_VERSION (
    FEE_DEFINITION_VERSION_ID,
    FEE_DEFINITION_ID,
    POLICY_VERSION_ID,
    REGULATORY_SOURCE_ID,
    REGULATORY_TARIFF_CODE,
    VERSION_NO,
    VERSION_TYPE_CODE,
    STATUS_CODE,
    FEE_PLAN_NAME,
    FEE_PLAN_TYPE_CODE,
    DEFAULT_BEARER_TYPE_CODE,
    DEFAULT_DEBIT_CREDIT_CODE,
    DEFAULT_DIRECTION_CODE,
    FEE_REASON,
    EFFECTIVE_FROM,
    APPROVED_AT,
    APPROVED_BY,
    ACTIVATED_AT,
    ACTIVATED_BY,
    CONFIG_HASH,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -7103,
    -7003,
    -5101,
    -6001,
    '7-2',
    '1.0',
    'MAJOR',
    'ACTIVE',
    'کارمزد ارزیابی املاک و ساختمان',
    'Standard Fee Plan',
    'BorneByDebtor',
    'Debit',
    'Added',
    'Seed baseline 1.0',
    DATE '2025-05-04',
    TIMESTAMP '2026-01-01 08:00:00',
    'SEED_ADMIN',
    TIMESTAMP '2026-01-01 08:30:00',
    'SEED_ADMIN',
    'SEED-CBI_PROPERTY_APPRAISAL_FEE-V1',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_DEFINITION_VERSION (
    FEE_DEFINITION_VERSION_ID,
    FEE_DEFINITION_ID,
    POLICY_VERSION_ID,
    REGULATORY_SOURCE_ID,
    REGULATORY_TARIFF_CODE,
    VERSION_NO,
    VERSION_TYPE_CODE,
    STATUS_CODE,
    FEE_PLAN_NAME,
    FEE_PLAN_TYPE_CODE,
    DEFAULT_BEARER_TYPE_CODE,
    DEFAULT_DEBIT_CREDIT_CODE,
    DEFAULT_DIRECTION_CODE,
    FEE_REASON,
    EFFECTIVE_FROM,
    APPROVED_AT,
    APPROVED_BY,
    ACTIVATED_AT,
    ACTIVATED_BY,
    CONFIG_HASH,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -7104,
    -7004,
    -5101,
    -6001,
    '5-1',
    '1.0',
    'MAJOR',
    'ACTIVE',
    'کارمزد وصول سفته و برات',
    'Standard Fee Plan',
    'BorneByDebtor',
    'Debit',
    'Added',
    'Seed baseline 1.0',
    DATE '2025-05-04',
    TIMESTAMP '2026-01-01 08:00:00',
    'SEED_ADMIN',
    TIMESTAMP '2026-01-01 08:30:00',
    'SEED_ADMIN',
    'SEED-CBI_BILL_COLLECTION_FEE-V1',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_DEFINITION_VERSION (
    FEE_DEFINITION_VERSION_ID,
    FEE_DEFINITION_ID,
    POLICY_VERSION_ID,
    REGULATORY_SOURCE_ID,
    REGULATORY_TARIFF_CODE,
    VERSION_NO,
    VERSION_TYPE_CODE,
    STATUS_CODE,
    FEE_PLAN_NAME,
    FEE_PLAN_TYPE_CODE,
    DEFAULT_BEARER_TYPE_CODE,
    DEFAULT_DEBIT_CREDIT_CODE,
    DEFAULT_DIRECTION_CODE,
    FEE_REASON,
    EFFECTIVE_FROM,
    APPROVED_AT,
    APPROVED_BY,
    ACTIVATED_AT,
    ACTIVATED_BY,
    CONFIG_HASH,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -7105,
    -7005,
    -5101,
    -6001,
    '8-15',
    '1.0',
    'MAJOR',
    'ACTIVE',
    'کارمزد عدم استفاده از حد اعتباری',
    'Standard Fee Plan',
    'BorneByDebtor',
    'Debit',
    'Added',
    'Seed baseline 1.0',
    DATE '2025-05-04',
    TIMESTAMP '2026-01-01 08:00:00',
    'SEED_ADMIN',
    TIMESTAMP '2026-01-01 08:30:00',
    'SEED_ADMIN',
    'SEED-CBI_UNUSED_LIMIT_COMMITMENT_FEE-V1',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_DEFINITION_VERSION (
    FEE_DEFINITION_VERSION_ID,
    FEE_DEFINITION_ID,
    POLICY_VERSION_ID,
    REGULATORY_SOURCE_ID,
    REGULATORY_TARIFF_CODE,
    VERSION_NO,
    VERSION_TYPE_CODE,
    STATUS_CODE,
    FEE_PLAN_NAME,
    FEE_PLAN_TYPE_CODE,
    DEFAULT_BEARER_TYPE_CODE,
    DEFAULT_DEBIT_CREDIT_CODE,
    DEFAULT_DIRECTION_CODE,
    FEE_REASON,
    EFFECTIVE_FROM,
    APPROVED_AT,
    APPROVED_BY,
    ACTIVATED_AT,
    ACTIVATED_BY,
    CONFIG_HASH,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -7106,
    -7006,
    -5101,
    -6001,
    '8-16',
    '1.0',
    'MAJOR',
    'ACTIVE',
    'هزینه کارشناسی طرح تسهیلات',
    'Standard Fee Plan',
    'BorneByDebtor',
    'Debit',
    'Added',
    'Seed baseline 1.0',
    DATE '2025-05-04',
    TIMESTAMP '2026-01-01 08:00:00',
    'SEED_ADMIN',
    TIMESTAMP '2026-01-01 08:30:00',
    'SEED_ADMIN',
    'SEED-CBI_CREDIT_EXPERTISE_FEE-V1',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_DEFINITION_VERSION (
    FEE_DEFINITION_VERSION_ID,
    FEE_DEFINITION_ID,
    POLICY_VERSION_ID,
    REGULATORY_SOURCE_ID,
    REGULATORY_TARIFF_CODE,
    VERSION_NO,
    VERSION_TYPE_CODE,
    STATUS_CODE,
    FEE_PLAN_NAME,
    FEE_PLAN_TYPE_CODE,
    DEFAULT_BEARER_TYPE_CODE,
    DEFAULT_DEBIT_CREDIT_CODE,
    DEFAULT_DIRECTION_CODE,
    FEE_REASON,
    EFFECTIVE_FROM,
    APPROVED_AT,
    APPROVED_BY,
    ACTIVATED_AT,
    ACTIVATED_BY,
    CONFIG_HASH,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -7107,
    -7007,
    -5101,
    -6001,
    '6-1-9',
    '1.0',
    'MAJOR',
    'ACTIVE',
    'کارمزد صدور دسته‌چک',
    'Standard Fee Plan',
    'BorneByDebtor',
    'Debit',
    'Added',
    'Seed baseline 1.0',
    DATE '2025-05-04',
    TIMESTAMP '2026-01-01 08:00:00',
    'SEED_ADMIN',
    TIMESTAMP '2026-01-01 08:30:00',
    'SEED_ADMIN',
    'SEED-CBI_CHEQUEBOOK_ISSUE_FEE-V1',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_DEFINITION_VERSION (
    FEE_DEFINITION_VERSION_ID,
    FEE_DEFINITION_ID,
    POLICY_VERSION_ID,
    REGULATORY_SOURCE_ID,
    REGULATORY_TARIFF_CODE,
    VERSION_NO,
    VERSION_TYPE_CODE,
    STATUS_CODE,
    FEE_PLAN_NAME,
    FEE_PLAN_TYPE_CODE,
    DEFAULT_BEARER_TYPE_CODE,
    DEFAULT_DEBIT_CREDIT_CODE,
    DEFAULT_DIRECTION_CODE,
    FEE_REASON,
    EFFECTIVE_FROM,
    APPROVED_AT,
    APPROVED_BY,
    ACTIVATED_AT,
    ACTIVATED_BY,
    CONFIG_HASH,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -7108,
    -7008,
    -5102,
    NULL,
    NULL,
    '1.0',
    'MAJOR',
    'ACTIVE',
    'کارمزد پلکانی نمونه',
    'Standard Fee Plan',
    'BorneByDebtor',
    'Debit',
    'Added',
    'Seed baseline 1.0',
    DATE '2026-01-01',
    TIMESTAMP '2026-01-01 08:00:00',
    'SEED_ADMIN',
    TIMESTAMP '2026-01-01 08:30:00',
    'SEED_ADMIN',
    'SEED-DEMO_TIERED_SERVICE_FEE-V1',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

PROMPT Loading FEE_PRODUCT_FEATURE ...
INSERT INTO FEE_PRODUCT_FEATURE (
    FEE_PRODUCT_FEATURE_ID,
    DEMO_PRODUCT_FEATURE_ID,
    FEE_DEFINITION_VERSION_ID,
    NAME_FA,
    REQUIREMENT_TYPE_CODE,
    IS_MANDATORY_FLAG,
    DEFAULT_FLAG,
    EFFECTIVE_FROM,
    STATUS_CODE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -7201,
    -3101,
    -7101,
    'کارمزد انتقال در حساب جاری',
    'MANDATORY',
    'Y',
    'Y',
    DATE '2026-01-01',
    'ACTIVE',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_PRODUCT_FEATURE (
    FEE_PRODUCT_FEATURE_ID,
    DEMO_PRODUCT_FEATURE_ID,
    FEE_DEFINITION_VERSION_ID,
    NAME_FA,
    REQUIREMENT_TYPE_CODE,
    IS_MANDATORY_FLAG,
    DEFAULT_FLAG,
    EFFECTIVE_FROM,
    STATUS_CODE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -7202,
    -3103,
    -7102,
    'کارمزد صدور ضمانت‌نامه',
    'MANDATORY',
    'Y',
    'Y',
    DATE '2026-01-01',
    'ACTIVE',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_PRODUCT_FEATURE (
    FEE_PRODUCT_FEATURE_ID,
    DEMO_PRODUCT_FEATURE_ID,
    FEE_DEFINITION_VERSION_ID,
    NAME_FA,
    REQUIREMENT_TYPE_CODE,
    IS_MANDATORY_FLAG,
    DEFAULT_FLAG,
    EFFECTIVE_FROM,
    STATUS_CODE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -7203,
    -3104,
    -7105,
    'کارمزد تعهد حد اعتباری',
    'MANDATORY',
    'Y',
    'Y',
    DATE '2026-01-01',
    'ACTIVE',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_PRODUCT_FEATURE (
    FEE_PRODUCT_FEATURE_ID,
    DEMO_PRODUCT_FEATURE_ID,
    FEE_DEFINITION_VERSION_ID,
    NAME_FA,
    REQUIREMENT_TYPE_CODE,
    IS_MANDATORY_FLAG,
    DEFAULT_FLAG,
    EFFECTIVE_FROM,
    STATUS_CODE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -7204,
    -3105,
    -7106,
    'کارمزد کارشناسی تسهیلات',
    'MANDATORY',
    'Y',
    'Y',
    DATE '2026-01-01',
    'ACTIVE',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_PRODUCT_FEATURE (
    FEE_PRODUCT_FEATURE_ID,
    DEMO_PRODUCT_FEATURE_ID,
    FEE_DEFINITION_VERSION_ID,
    NAME_FA,
    REQUIREMENT_TYPE_CODE,
    IS_MANDATORY_FLAG,
    DEFAULT_FLAG,
    EFFECTIVE_FROM,
    STATUS_CODE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -7205,
    -3102,
    -7107,
    'کارمزد صدور دسته‌چک',
    'MANDATORY',
    'Y',
    'Y',
    DATE '2026-01-01',
    'ACTIVE',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_PRODUCT_FEATURE (
    FEE_PRODUCT_FEATURE_ID,
    DEMO_PRODUCT_FEATURE_ID,
    FEE_DEFINITION_VERSION_ID,
    NAME_FA,
    REQUIREMENT_TYPE_CODE,
    IS_MANDATORY_FLAG,
    DEFAULT_FLAG,
    EFFECTIVE_FROM,
    STATUS_CODE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -7206,
    -3106,
    -7103,
    'کارمزد ارزیابی',
    'MANDATORY',
    'Y',
    'Y',
    DATE '2026-01-01',
    'ACTIVE',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_PRODUCT_FEATURE (
    FEE_PRODUCT_FEATURE_ID,
    DEMO_PRODUCT_FEATURE_ID,
    FEE_DEFINITION_VERSION_ID,
    NAME_FA,
    REQUIREMENT_TYPE_CODE,
    IS_MANDATORY_FLAG,
    DEFAULT_FLAG,
    EFFECTIVE_FROM,
    STATUS_CODE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -7207,
    -3107,
    -7104,
    'کارمزد وصول بروات',
    'MANDATORY',
    'Y',
    'Y',
    DATE '2026-01-01',
    'ACTIVE',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

PROMPT Loading FEE_APPLICABILITY_RULE ...
INSERT INTO FEE_APPLICABILITY_RULE (
    APPLICABILITY_RULE_ID,
    FEE_DEFINITION_VERSION_ID,
    RULE_CODE,
    NAME_FA,
    PRIORITY_NO,
    MATCH_MODE_CODE,
    ACTION_CODE,
    EFFECTIVE_FROM,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -8001,
    -7101,
    'TRANSFER_BASE',
    'شرط پایه انتقال',
    10,
    'ALL',
    'APPLY',
    DATE '2025-05-04',
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_APPLICABILITY_RULE (
    APPLICABILITY_RULE_ID,
    FEE_DEFINITION_VERSION_ID,
    RULE_CODE,
    NAME_FA,
    PRIORITY_NO,
    MATCH_MODE_CODE,
    ACTION_CODE,
    EFFECTIVE_FROM,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -8002,
    -7101,
    'TRANSFER_VIP',
    'شرط تخفیف VIP',
    5,
    'ALL',
    'APPLY',
    DATE '2025-05-04',
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_APPLICABILITY_RULE (
    APPLICABILITY_RULE_ID,
    FEE_DEFINITION_VERSION_ID,
    RULE_CODE,
    NAME_FA,
    PRIORITY_NO,
    MATCH_MODE_CODE,
    ACTION_CODE,
    EFFECTIVE_FROM,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -8003,
    -7102,
    'GUARANTEE_BASE',
    'شرط ضمانت‌نامه',
    10,
    'ALL',
    'APPLY',
    DATE '2025-05-04',
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_APPLICABILITY_RULE (
    APPLICABILITY_RULE_ID,
    FEE_DEFINITION_VERSION_ID,
    RULE_CODE,
    NAME_FA,
    PRIORITY_NO,
    MATCH_MODE_CODE,
    ACTION_CODE,
    EFFECTIVE_FROM,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -8004,
    -7103,
    'APPRAISAL_BASE',
    'شرط ارزیابی',
    10,
    'ALL',
    'APPLY',
    DATE '2025-05-04',
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_APPLICABILITY_RULE (
    APPLICABILITY_RULE_ID,
    FEE_DEFINITION_VERSION_ID,
    RULE_CODE,
    NAME_FA,
    PRIORITY_NO,
    MATCH_MODE_CODE,
    ACTION_CODE,
    EFFECTIVE_FROM,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -8005,
    -7104,
    'BILL_COLLECTION_BASE',
    'شرط وصول بروات',
    10,
    'ALL',
    'APPLY',
    DATE '2025-05-04',
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_APPLICABILITY_RULE (
    APPLICABILITY_RULE_ID,
    FEE_DEFINITION_VERSION_ID,
    RULE_CODE,
    NAME_FA,
    PRIORITY_NO,
    MATCH_MODE_CODE,
    ACTION_CODE,
    EFFECTIVE_FROM,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -8006,
    -7105,
    'COMMITMENT_BASE',
    'شرط تعهد حد اعتباری',
    10,
    'ALL',
    'APPLY',
    DATE '2025-05-04',
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_APPLICABILITY_RULE (
    APPLICABILITY_RULE_ID,
    FEE_DEFINITION_VERSION_ID,
    RULE_CODE,
    NAME_FA,
    PRIORITY_NO,
    MATCH_MODE_CODE,
    ACTION_CODE,
    EFFECTIVE_FROM,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -8007,
    -7106,
    'CREDIT_EXPERTISE_BASE',
    'شرط کارشناسی تسهیلات',
    10,
    'ALL',
    'APPLY',
    DATE '2025-05-04',
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_APPLICABILITY_RULE (
    APPLICABILITY_RULE_ID,
    FEE_DEFINITION_VERSION_ID,
    RULE_CODE,
    NAME_FA,
    PRIORITY_NO,
    MATCH_MODE_CODE,
    ACTION_CODE,
    EFFECTIVE_FROM,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -8008,
    -7107,
    'CHEQUEBOOK_BASE',
    'شرط صدور دسته‌چک',
    10,
    'ALL',
    'APPLY',
    DATE '2025-05-04',
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_APPLICABILITY_RULE (
    APPLICABILITY_RULE_ID,
    FEE_DEFINITION_VERSION_ID,
    RULE_CODE,
    NAME_FA,
    PRIORITY_NO,
    MATCH_MODE_CODE,
    ACTION_CODE,
    EFFECTIVE_FROM,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -8009,
    -7108,
    'TIERED_BASE',
    'شرط کارمزد پلکانی نمونه',
    10,
    'ALL',
    'APPLY',
    DATE '2025-05-04',
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

PROMPT Loading FEE_APPLICABILITY_CONDITION ...
INSERT INTO FEE_APPLICABILITY_CONDITION (
    CONDITION_ID,
    APPLICABILITY_RULE_ID,
    SEQUENCE_NO,
    GROUP_NO,
    DIMENSION_CODE,
    OPERATOR_CODE,
    LOGICAL_OPERATOR_CODE,
    VALUE_TYPE_CODE,
    TEXT_VALUE,
    DESCRIPTION,
    CREATED_AT,
    CREATED_BY
) VALUES (
    -8101,
    -8001,
    1,
    1,
    'ACTIVITY',
    'EQ',
    'AND',
    'TEXT',
    'MONEY_TRANSFER',
    'ACTIVITY EQ MONEY_TRANSFER',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0'
);

INSERT INTO FEE_APPLICABILITY_CONDITION (
    CONDITION_ID,
    APPLICABILITY_RULE_ID,
    SEQUENCE_NO,
    GROUP_NO,
    DIMENSION_CODE,
    OPERATOR_CODE,
    LOGICAL_OPERATOR_CODE,
    VALUE_TYPE_CODE,
    TEXT_VALUE,
    DESCRIPTION,
    CREATED_AT,
    CREATED_BY
) VALUES (
    -8102,
    -8001,
    2,
    1,
    'CURRENCY',
    'EQ',
    'AND',
    'TEXT',
    'IRR',
    'CURRENCY EQ IRR',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0'
);

INSERT INTO FEE_APPLICABILITY_CONDITION (
    CONDITION_ID,
    APPLICABILITY_RULE_ID,
    SEQUENCE_NO,
    GROUP_NO,
    DIMENSION_CODE,
    OPERATOR_CODE,
    LOGICAL_OPERATOR_CODE,
    VALUE_TYPE_CODE,
    TEXT_VALUE,
    DESCRIPTION,
    CREATED_AT,
    CREATED_BY
) VALUES (
    -8103,
    -8002,
    1,
    1,
    'CUSTOMER_SEGMENT',
    'EQ',
    'AND',
    'TEXT',
    'VIP',
    'CUSTOMER_SEGMENT EQ VIP',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0'
);

INSERT INTO FEE_APPLICABILITY_CONDITION (
    CONDITION_ID,
    APPLICABILITY_RULE_ID,
    SEQUENCE_NO,
    GROUP_NO,
    DIMENSION_CODE,
    OPERATOR_CODE,
    LOGICAL_OPERATOR_CODE,
    VALUE_TYPE_CODE,
    TEXT_VALUE,
    DESCRIPTION,
    CREATED_AT,
    CREATED_BY
) VALUES (
    -8104,
    -8003,
    1,
    1,
    'PRODUCT_TYPE',
    'EQ',
    'AND',
    'TEXT',
    'GUARANTEE',
    'PRODUCT_TYPE EQ GUARANTEE',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0'
);

INSERT INTO FEE_APPLICABILITY_CONDITION (
    CONDITION_ID,
    APPLICABILITY_RULE_ID,
    SEQUENCE_NO,
    GROUP_NO,
    DIMENSION_CODE,
    OPERATOR_CODE,
    LOGICAL_OPERATOR_CODE,
    VALUE_TYPE_CODE,
    TEXT_VALUE,
    DESCRIPTION,
    CREATED_AT,
    CREATED_BY
) VALUES (
    -8105,
    -8004,
    1,
    1,
    'ACTIVITY',
    'EQ',
    'AND',
    'TEXT',
    'APPRAISAL',
    'ACTIVITY EQ APPRAISAL',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0'
);

INSERT INTO FEE_APPLICABILITY_CONDITION (
    CONDITION_ID,
    APPLICABILITY_RULE_ID,
    SEQUENCE_NO,
    GROUP_NO,
    DIMENSION_CODE,
    OPERATOR_CODE,
    LOGICAL_OPERATOR_CODE,
    VALUE_TYPE_CODE,
    TEXT_VALUE,
    DESCRIPTION,
    CREATED_AT,
    CREATED_BY
) VALUES (
    -8106,
    -8005,
    1,
    1,
    'ACTIVITY',
    'EQ',
    'AND',
    'TEXT',
    'BILL_COLLECTION',
    'ACTIVITY EQ BILL_COLLECTION',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0'
);

INSERT INTO FEE_APPLICABILITY_CONDITION (
    CONDITION_ID,
    APPLICABILITY_RULE_ID,
    SEQUENCE_NO,
    GROUP_NO,
    DIMENSION_CODE,
    OPERATOR_CODE,
    LOGICAL_OPERATOR_CODE,
    VALUE_TYPE_CODE,
    TEXT_VALUE,
    DESCRIPTION,
    CREATED_AT,
    CREATED_BY
) VALUES (
    -8107,
    -8006,
    1,
    1,
    'ACTIVITY',
    'EQ',
    'AND',
    'TEXT',
    'FACILITY_UNUSED_LIMIT',
    'ACTIVITY EQ FACILITY_UNUSED_LIMIT',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0'
);

INSERT INTO FEE_APPLICABILITY_CONDITION (
    CONDITION_ID,
    APPLICABILITY_RULE_ID,
    SEQUENCE_NO,
    GROUP_NO,
    DIMENSION_CODE,
    OPERATOR_CODE,
    LOGICAL_OPERATOR_CODE,
    VALUE_TYPE_CODE,
    NUMBER_VALUE_FROM,
    DESCRIPTION,
    CREATED_AT,
    CREATED_BY
) VALUES (
    -8108,
    -8006,
    2,
    1,
    'ELAPSED_PERIOD_DAYS',
    'GE',
    'AND',
    'NUMBER',
    45,
    'ELAPSED_PERIOD_DAYS GE 45',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0'
);

INSERT INTO FEE_APPLICABILITY_CONDITION (
    CONDITION_ID,
    APPLICABILITY_RULE_ID,
    SEQUENCE_NO,
    GROUP_NO,
    DIMENSION_CODE,
    OPERATOR_CODE,
    LOGICAL_OPERATOR_CODE,
    VALUE_TYPE_CODE,
    TEXT_VALUE,
    DESCRIPTION,
    CREATED_AT,
    CREATED_BY
) VALUES (
    -8109,
    -8007,
    1,
    1,
    'PRODUCT_TYPE',
    'EQ',
    'AND',
    'TEXT',
    'FACILITY',
    'PRODUCT_TYPE EQ FACILITY',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0'
);

INSERT INTO FEE_APPLICABILITY_CONDITION (
    CONDITION_ID,
    APPLICABILITY_RULE_ID,
    SEQUENCE_NO,
    GROUP_NO,
    DIMENSION_CODE,
    OPERATOR_CODE,
    LOGICAL_OPERATOR_CODE,
    VALUE_TYPE_CODE,
    TEXT_VALUE,
    DESCRIPTION,
    CREATED_AT,
    CREATED_BY
) VALUES (
    -8110,
    -8008,
    1,
    1,
    'ACTIVITY',
    'EQ',
    'AND',
    'TEXT',
    'CHEQUEBOOK_ISSUE',
    'ACTIVITY EQ CHEQUEBOOK_ISSUE',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0'
);

INSERT INTO FEE_APPLICABILITY_CONDITION (
    CONDITION_ID,
    APPLICABILITY_RULE_ID,
    SEQUENCE_NO,
    GROUP_NO,
    DIMENSION_CODE,
    OPERATOR_CODE,
    LOGICAL_OPERATOR_CODE,
    VALUE_TYPE_CODE,
    TEXT_VALUE,
    DESCRIPTION,
    CREATED_AT,
    CREATED_BY
) VALUES (
    -8111,
    -8009,
    1,
    1,
    'CHANNEL',
    'EQ',
    'AND',
    'TEXT',
    'MOBILE',
    'CHANNEL EQ MOBILE',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0'
);

PROMPT Loading FEE_CALCULATION_RULE ...
INSERT INTO FEE_CALCULATION_RULE (
    CALCULATION_RULE_ID,
    FEE_DEFINITION_VERSION_ID,
    APPLICABILITY_RULE_ID,
    RULE_CODE,
    NAME_FA,
    PRIORITY_NO,
    CALCULATION_STRATEGY_CODE,
    BASIS_TYPE_CODE,
    FIXED_AMOUNT,
    RATE_VALUE,
    MIN_FEE_AMOUNT,
    MAX_FEE_AMOUNT,
    RATE_PERIOD_CODE,
    DAY_COUNT_BASIS_CODE,
    PRORATION_MODE_CODE,
    MIN_CHARGE_PERIOD_DAYS,
    CURRENCY_CODE,
    ROUNDING_MODE_CODE,
    ROUNDING_SCALE,
    EFFECTIVE_FROM,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -9001,
    -7101,
    -8001,
    'TRANSFER_STD',
    'محاسبه درصدی انتقال',
    10,
    'PERCENTAGE_FLOOR_CAP',
    'AMOUNT',
    NULL,
    0.002,
    50000,
    2000000,
    NULL,
    NULL,
    NULL,
    NULL,
    'IRR',
    'HALF_UP',
    0,
    DATE '2025-05-04',
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_CALCULATION_RULE (
    CALCULATION_RULE_ID,
    FEE_DEFINITION_VERSION_ID,
    APPLICABILITY_RULE_ID,
    RULE_CODE,
    NAME_FA,
    PRIORITY_NO,
    CALCULATION_STRATEGY_CODE,
    BASIS_TYPE_CODE,
    FIXED_AMOUNT,
    RATE_VALUE,
    MIN_FEE_AMOUNT,
    MAX_FEE_AMOUNT,
    RATE_PERIOD_CODE,
    DAY_COUNT_BASIS_CODE,
    PRORATION_MODE_CODE,
    MIN_CHARGE_PERIOD_DAYS,
    CURRENCY_CODE,
    ROUNDING_MODE_CODE,
    ROUNDING_SCALE,
    EFFECTIVE_FROM,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -9002,
    -7102,
    -8003,
    'GUARANTEE_ANNUAL',
    'محاسبه سالانه ضمانت‌نامه',
    10,
    'ANNUALIZED_PERCENTAGE',
    'AMOUNT',
    NULL,
    0.005,
    812500,
    NULL,
    'YEAR',
    'ACTUAL_ACTUAL',
    'ACTUAL',
    NULL,
    'IRR',
    'HALF_UP',
    0,
    DATE '2025-05-04',
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_CALCULATION_RULE (
    CALCULATION_RULE_ID,
    FEE_DEFINITION_VERSION_ID,
    APPLICABILITY_RULE_ID,
    RULE_CODE,
    NAME_FA,
    PRIORITY_NO,
    CALCULATION_STRATEGY_CODE,
    BASIS_TYPE_CODE,
    FIXED_AMOUNT,
    RATE_VALUE,
    MIN_FEE_AMOUNT,
    MAX_FEE_AMOUNT,
    RATE_PERIOD_CODE,
    DAY_COUNT_BASIS_CODE,
    PRORATION_MODE_CODE,
    MIN_CHARGE_PERIOD_DAYS,
    CURRENCY_CODE,
    ROUNDING_MODE_CODE,
    ROUNDING_SCALE,
    EFFECTIVE_FROM,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -9003,
    -7103,
    -8004,
    'APPRAISAL_COMPOSITE',
    'محاسبه ترکیبی ارزیابی',
    10,
    'COMPOSITE',
    'AMOUNT',
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    'IRR',
    'HALF_UP',
    0,
    DATE '2025-05-04',
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_CALCULATION_RULE (
    CALCULATION_RULE_ID,
    FEE_DEFINITION_VERSION_ID,
    APPLICABILITY_RULE_ID,
    RULE_CODE,
    NAME_FA,
    PRIORITY_NO,
    CALCULATION_STRATEGY_CODE,
    BASIS_TYPE_CODE,
    FIXED_AMOUNT,
    RATE_VALUE,
    MIN_FEE_AMOUNT,
    MAX_FEE_AMOUNT,
    RATE_PERIOD_CODE,
    DAY_COUNT_BASIS_CODE,
    PRORATION_MODE_CODE,
    MIN_CHARGE_PERIOD_DAYS,
    CURRENCY_CODE,
    ROUNDING_MODE_CODE,
    ROUNDING_SCALE,
    EFFECTIVE_FROM,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -9004,
    -7104,
    -8005,
    'BILL_FIXED',
    'کارمزد ثابت وصول بروات',
    10,
    'FIXED',
    'COUNT',
    90000,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    'IRR',
    'HALF_UP',
    0,
    DATE '2025-05-04',
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_CALCULATION_RULE (
    CALCULATION_RULE_ID,
    FEE_DEFINITION_VERSION_ID,
    APPLICABILITY_RULE_ID,
    RULE_CODE,
    NAME_FA,
    PRIORITY_NO,
    CALCULATION_STRATEGY_CODE,
    BASIS_TYPE_CODE,
    FIXED_AMOUNT,
    RATE_VALUE,
    MIN_FEE_AMOUNT,
    MAX_FEE_AMOUNT,
    RATE_PERIOD_CODE,
    DAY_COUNT_BASIS_CODE,
    PRORATION_MODE_CODE,
    MIN_CHARGE_PERIOD_DAYS,
    CURRENCY_CODE,
    ROUNDING_MODE_CODE,
    ROUNDING_SCALE,
    EFFECTIVE_FROM,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -9005,
    -7105,
    -8006,
    'COMMITMENT_ANNUAL',
    'کارمزد سالانه تعهد',
    10,
    'ANNUALIZED_PERCENTAGE',
    'AMOUNT',
    NULL,
    0.01,
    NULL,
    NULL,
    'YEAR',
    'ACTUAL_ACTUAL',
    'ACTUAL',
    45,
    'IRR',
    'HALF_UP',
    0,
    DATE '2025-05-04',
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_CALCULATION_RULE (
    CALCULATION_RULE_ID,
    FEE_DEFINITION_VERSION_ID,
    APPLICABILITY_RULE_ID,
    RULE_CODE,
    NAME_FA,
    PRIORITY_NO,
    CALCULATION_STRATEGY_CODE,
    BASIS_TYPE_CODE,
    FIXED_AMOUNT,
    RATE_VALUE,
    MIN_FEE_AMOUNT,
    MAX_FEE_AMOUNT,
    RATE_PERIOD_CODE,
    DAY_COUNT_BASIS_CODE,
    PRORATION_MODE_CODE,
    MIN_CHARGE_PERIOD_DAYS,
    CURRENCY_CODE,
    ROUNDING_MODE_CODE,
    ROUNDING_SCALE,
    EFFECTIVE_FROM,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -9006,
    -7106,
    -8007,
    'CREDIT_STAGE1',
    'مرحله پذیرش درخواست',
    10,
    'PERCENTAGE',
    'AMOUNT',
    NULL,
    0.0005,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    'IRR',
    'HALF_UP',
    0,
    DATE '2025-05-04',
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_CALCULATION_RULE (
    CALCULATION_RULE_ID,
    FEE_DEFINITION_VERSION_ID,
    APPLICABILITY_RULE_ID,
    RULE_CODE,
    NAME_FA,
    PRIORITY_NO,
    CALCULATION_STRATEGY_CODE,
    BASIS_TYPE_CODE,
    FIXED_AMOUNT,
    RATE_VALUE,
    MIN_FEE_AMOUNT,
    MAX_FEE_AMOUNT,
    RATE_PERIOD_CODE,
    DAY_COUNT_BASIS_CODE,
    PRORATION_MODE_CODE,
    MIN_CHARGE_PERIOD_DAYS,
    CURRENCY_CODE,
    ROUNDING_MODE_CODE,
    ROUNDING_SCALE,
    EFFECTIVE_FROM,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -9007,
    -7106,
    -8007,
    'CREDIT_STAGE2',
    'مرحله تصویب و انعقاد',
    20,
    'PERCENTAGE',
    'AMOUNT',
    NULL,
    0.0015,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    'IRR',
    'HALF_UP',
    0,
    DATE '2025-05-04',
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_CALCULATION_RULE (
    CALCULATION_RULE_ID,
    FEE_DEFINITION_VERSION_ID,
    APPLICABILITY_RULE_ID,
    RULE_CODE,
    NAME_FA,
    PRIORITY_NO,
    CALCULATION_STRATEGY_CODE,
    BASIS_TYPE_CODE,
    FIXED_AMOUNT,
    RATE_VALUE,
    MIN_FEE_AMOUNT,
    MAX_FEE_AMOUNT,
    RATE_PERIOD_CODE,
    DAY_COUNT_BASIS_CODE,
    PRORATION_MODE_CODE,
    MIN_CHARGE_PERIOD_DAYS,
    CURRENCY_CODE,
    ROUNDING_MODE_CODE,
    ROUNDING_SCALE,
    EFFECTIVE_FROM,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -9008,
    -7107,
    -8008,
    'CHEQUEBOOK_FIXED',
    'مبلغ پایه صدور دسته‌چک',
    10,
    'FIXED',
    'COUNT',
    75000,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    'IRR',
    'HALF_UP',
    0,
    DATE '2025-05-04',
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_CALCULATION_RULE (
    CALCULATION_RULE_ID,
    FEE_DEFINITION_VERSION_ID,
    APPLICABILITY_RULE_ID,
    RULE_CODE,
    NAME_FA,
    PRIORITY_NO,
    CALCULATION_STRATEGY_CODE,
    BASIS_TYPE_CODE,
    FIXED_AMOUNT,
    RATE_VALUE,
    MIN_FEE_AMOUNT,
    MAX_FEE_AMOUNT,
    RATE_PERIOD_CODE,
    DAY_COUNT_BASIS_CODE,
    PRORATION_MODE_CODE,
    MIN_CHARGE_PERIOD_DAYS,
    CURRENCY_CODE,
    ROUNDING_MODE_CODE,
    ROUNDING_SCALE,
    EFFECTIVE_FROM,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -9009,
    -7108,
    -8009,
    'DEMO_TIERED',
    'محاسبه پلکانی نمونه',
    10,
    'TIERED',
    'AMOUNT',
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    'IRR',
    'HALF_UP',
    0,
    DATE '2025-05-04',
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_CALCULATION_RULE (
    CALCULATION_RULE_ID,
    FEE_DEFINITION_VERSION_ID,
    APPLICABILITY_RULE_ID,
    RULE_CODE,
    NAME_FA,
    PRIORITY_NO,
    CALCULATION_STRATEGY_CODE,
    BASIS_TYPE_CODE,
    FIXED_AMOUNT,
    RATE_VALUE,
    MIN_FEE_AMOUNT,
    MAX_FEE_AMOUNT,
    RATE_PERIOD_CODE,
    DAY_COUNT_BASIS_CODE,
    PRORATION_MODE_CODE,
    MIN_CHARGE_PERIOD_DAYS,
    CURRENCY_CODE,
    ROUNDING_MODE_CODE,
    ROUNDING_SCALE,
    EFFECTIVE_FROM,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -9010,
    -7104,
    -8005,
    'POSTAGE_EXTERNAL',
    'هزینه پست بیرونی',
    20,
    'EXTERNAL_VALUE',
    'AMOUNT',
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    'IRR',
    'HALF_UP',
    0,
    DATE '2025-05-04',
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_CALCULATION_RULE (
    CALCULATION_RULE_ID,
    FEE_DEFINITION_VERSION_ID,
    APPLICABILITY_RULE_ID,
    RULE_CODE,
    NAME_FA,
    PRIORITY_NO,
    CALCULATION_STRATEGY_CODE,
    BASIS_TYPE_CODE,
    FIXED_AMOUNT,
    RATE_VALUE,
    MIN_FEE_AMOUNT,
    MAX_FEE_AMOUNT,
    RATE_PERIOD_CODE,
    DAY_COUNT_BASIS_CODE,
    PRORATION_MODE_CODE,
    MIN_CHARGE_PERIOD_DAYS,
    CURRENCY_CODE,
    ROUNDING_MODE_CODE,
    ROUNDING_SCALE,
    EFFECTIVE_FROM,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -9011,
    -7107,
    -8008,
    'PRINTING_EXTERNAL',
    'هزینه چاپ بیرونی',
    20,
    'EXTERNAL_VALUE',
    'AMOUNT',
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    'IRR',
    'HALF_UP',
    0,
    DATE '2025-05-04',
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_CALCULATION_RULE (
    CALCULATION_RULE_ID,
    FEE_DEFINITION_VERSION_ID,
    APPLICABILITY_RULE_ID,
    RULE_CODE,
    NAME_FA,
    PRIORITY_NO,
    CALCULATION_STRATEGY_CODE,
    BASIS_TYPE_CODE,
    FIXED_AMOUNT,
    RATE_VALUE,
    MIN_FEE_AMOUNT,
    MAX_FEE_AMOUNT,
    RATE_PERIOD_CODE,
    DAY_COUNT_BASIS_CODE,
    PRORATION_MODE_CODE,
    MIN_CHARGE_PERIOD_DAYS,
    CURRENCY_CODE,
    ROUNDING_MODE_CODE,
    ROUNDING_SCALE,
    EFFECTIVE_FROM,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -9012,
    -7107,
    -8008,
    'STAMP_EXTERNAL',
    'تمبر مالیاتی بیرونی',
    30,
    'EXTERNAL_VALUE',
    'AMOUNT',
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    'IRR',
    'HALF_UP',
    0,
    DATE '2025-05-04',
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

PROMPT Loading FEE_INPUT_DEFINITION ...
INSERT INTO FEE_INPUT_DEFINITION (
    INPUT_DEF_ID,
    CALCULATION_RULE_ID,
    INPUT_CODE,
    NAME_FA,
    DATA_TYPE_CODE,
    UNIT_CODE,
    MANDATORY_FLAG,
    DEFAULT_NUMBER_VALUE,
    DISPLAY_ORDER,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -9101,
    -9001,
    'BASIS_AMOUNT',
    'مبلغ تراکنش',
    'NUMBER',
    'IRR',
    'Y',
    NULL,
    1,
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_INPUT_DEFINITION (
    INPUT_DEF_ID,
    CALCULATION_RULE_ID,
    INPUT_CODE,
    NAME_FA,
    DATA_TYPE_CODE,
    UNIT_CODE,
    MANDATORY_FLAG,
    DEFAULT_NUMBER_VALUE,
    DISPLAY_ORDER,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -9102,
    -9002,
    'GUARANTEE_AMOUNT',
    'مبلغ ضمانت‌نامه',
    'NUMBER',
    'IRR',
    'Y',
    NULL,
    1,
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_INPUT_DEFINITION (
    INPUT_DEF_ID,
    CALCULATION_RULE_ID,
    INPUT_CODE,
    NAME_FA,
    DATA_TYPE_CODE,
    UNIT_CODE,
    MANDATORY_FLAG,
    DEFAULT_NUMBER_VALUE,
    DISPLAY_ORDER,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -9103,
    -9002,
    'START_DATE',
    'تاریخ شروع',
    'DATE',
    NULL,
    'Y',
    NULL,
    2,
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_INPUT_DEFINITION (
    INPUT_DEF_ID,
    CALCULATION_RULE_ID,
    INPUT_CODE,
    NAME_FA,
    DATA_TYPE_CODE,
    UNIT_CODE,
    MANDATORY_FLAG,
    DEFAULT_NUMBER_VALUE,
    DISPLAY_ORDER,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -9104,
    -9002,
    'END_DATE',
    'تاریخ پایان',
    'DATE',
    NULL,
    'Y',
    NULL,
    3,
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_INPUT_DEFINITION (
    INPUT_DEF_ID,
    CALCULATION_RULE_ID,
    INPUT_CODE,
    NAME_FA,
    DATA_TYPE_CODE,
    UNIT_CODE,
    MANDATORY_FLAG,
    DEFAULT_NUMBER_VALUE,
    DISPLAY_ORDER,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -9105,
    -9003,
    'APPRAISAL_AMOUNT',
    'مبلغ ارزیابی',
    'NUMBER',
    'IRR',
    'Y',
    NULL,
    1,
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_INPUT_DEFINITION (
    INPUT_DEF_ID,
    CALCULATION_RULE_ID,
    INPUT_CODE,
    NAME_FA,
    DATA_TYPE_CODE,
    UNIT_CODE,
    MANDATORY_FLAG,
    DEFAULT_NUMBER_VALUE,
    DISPLAY_ORDER,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -9106,
    -9003,
    'OFFICIAL_EXPERT_TARIFF',
    'تعرفه کارشناس رسمی',
    'NUMBER',
    'IRR',
    'Y',
    NULL,
    2,
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_INPUT_DEFINITION (
    INPUT_DEF_ID,
    CALCULATION_RULE_ID,
    INPUT_CODE,
    NAME_FA,
    DATA_TYPE_CODE,
    UNIT_CODE,
    MANDATORY_FLAG,
    DEFAULT_NUMBER_VALUE,
    DISPLAY_ORDER,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -9107,
    -9004,
    'ITEM_COUNT',
    'تعداد اسناد',
    'NUMBER',
    'COUNT',
    'Y',
    1,
    1,
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_INPUT_DEFINITION (
    INPUT_DEF_ID,
    CALCULATION_RULE_ID,
    INPUT_CODE,
    NAME_FA,
    DATA_TYPE_CODE,
    UNIT_CODE,
    MANDATORY_FLAG,
    DEFAULT_NUMBER_VALUE,
    DISPLAY_ORDER,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -9108,
    -9010,
    'POSTAGE_AMOUNT',
    'هزینه پست',
    'NUMBER',
    'IRR',
    'Y',
    NULL,
    2,
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_INPUT_DEFINITION (
    INPUT_DEF_ID,
    CALCULATION_RULE_ID,
    INPUT_CODE,
    NAME_FA,
    DATA_TYPE_CODE,
    UNIT_CODE,
    MANDATORY_FLAG,
    DEFAULT_NUMBER_VALUE,
    DISPLAY_ORDER,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -9109,
    -9005,
    'UNUSED_LIMIT_AMOUNT',
    'مانده استفاده‌نشده',
    'NUMBER',
    'IRR',
    'Y',
    NULL,
    1,
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_INPUT_DEFINITION (
    INPUT_DEF_ID,
    CALCULATION_RULE_ID,
    INPUT_CODE,
    NAME_FA,
    DATA_TYPE_CODE,
    UNIT_CODE,
    MANDATORY_FLAG,
    DEFAULT_NUMBER_VALUE,
    DISPLAY_ORDER,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -9110,
    -9005,
    'START_DATE',
    'تاریخ شروع تعهد',
    'DATE',
    NULL,
    'Y',
    NULL,
    2,
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_INPUT_DEFINITION (
    INPUT_DEF_ID,
    CALCULATION_RULE_ID,
    INPUT_CODE,
    NAME_FA,
    DATA_TYPE_CODE,
    UNIT_CODE,
    MANDATORY_FLAG,
    DEFAULT_NUMBER_VALUE,
    DISPLAY_ORDER,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -9111,
    -9005,
    'END_DATE',
    'تاریخ پایان محاسبه',
    'DATE',
    NULL,
    'Y',
    NULL,
    3,
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_INPUT_DEFINITION (
    INPUT_DEF_ID,
    CALCULATION_RULE_ID,
    INPUT_CODE,
    NAME_FA,
    DATA_TYPE_CODE,
    UNIT_CODE,
    MANDATORY_FLAG,
    DEFAULT_NUMBER_VALUE,
    DISPLAY_ORDER,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -9112,
    -9006,
    'APPLICATION_AMOUNT',
    'مبلغ درخواستی',
    'NUMBER',
    'IRR',
    'Y',
    NULL,
    1,
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_INPUT_DEFINITION (
    INPUT_DEF_ID,
    CALCULATION_RULE_ID,
    INPUT_CODE,
    NAME_FA,
    DATA_TYPE_CODE,
    UNIT_CODE,
    MANDATORY_FLAG,
    DEFAULT_NUMBER_VALUE,
    DISPLAY_ORDER,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -9113,
    -9007,
    'APPROVED_BANK_SHARE',
    'سهم بانک از مبلغ مصوب',
    'NUMBER',
    'IRR',
    'Y',
    NULL,
    1,
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_INPUT_DEFINITION (
    INPUT_DEF_ID,
    CALCULATION_RULE_ID,
    INPUT_CODE,
    NAME_FA,
    DATA_TYPE_CODE,
    UNIT_CODE,
    MANDATORY_FLAG,
    DEFAULT_NUMBER_VALUE,
    DISPLAY_ORDER,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -9114,
    -9007,
    'PREVIOUS_CHARGED_AMOUNT',
    'مبلغ قبلاً دریافت‌شده',
    'NUMBER',
    'IRR',
    'Y',
    NULL,
    2,
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_INPUT_DEFINITION (
    INPUT_DEF_ID,
    CALCULATION_RULE_ID,
    INPUT_CODE,
    NAME_FA,
    DATA_TYPE_CODE,
    UNIT_CODE,
    MANDATORY_FLAG,
    DEFAULT_NUMBER_VALUE,
    DISPLAY_ORDER,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -9115,
    -9011,
    'PRINTING_COST',
    'هزینه چاپ',
    'NUMBER',
    'IRR',
    'Y',
    NULL,
    1,
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_INPUT_DEFINITION (
    INPUT_DEF_ID,
    CALCULATION_RULE_ID,
    INPUT_CODE,
    NAME_FA,
    DATA_TYPE_CODE,
    UNIT_CODE,
    MANDATORY_FLAG,
    DEFAULT_NUMBER_VALUE,
    DISPLAY_ORDER,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -9116,
    -9012,
    'STAMP_TAX',
    'تمبر مالیاتی',
    'NUMBER',
    'IRR',
    'Y',
    NULL,
    1,
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_INPUT_DEFINITION (
    INPUT_DEF_ID,
    CALCULATION_RULE_ID,
    INPUT_CODE,
    NAME_FA,
    DATA_TYPE_CODE,
    UNIT_CODE,
    MANDATORY_FLAG,
    DEFAULT_NUMBER_VALUE,
    DISPLAY_ORDER,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -9117,
    -9009,
    'BASIS_AMOUNT',
    'مبلغ مبنا',
    'NUMBER',
    'IRR',
    'Y',
    NULL,
    1,
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

PROMPT Loading FEE_CALCULATION_TIER ...
INSERT INTO FEE_CALCULATION_TIER (
    TIER_ID,
    CALCULATION_RULE_ID,
    TIER_NO,
    TIER_NAME_FA,
    LOWER_BOUND,
    UPPER_BOUND,
    BOUND_UNIT_CODE,
    TIER_BASIS_CODE,
    TIER_STRATEGY_CODE,
    FIXED_AMOUNT,
    RATE_VALUE,
    MIN_FEE_AMOUNT,
    MAX_FEE_AMOUNT,
    EFFECTIVE_FROM,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -9201,
    -9009,
    1,
    'پله اول',
    0,
    10000000,
    'IRR',
    'WHOLE_AMOUNT',
    'FIXED',
    50000,
    NULL,
    NULL,
    NULL,
    DATE '2025-05-04',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_CALCULATION_TIER (
    TIER_ID,
    CALCULATION_RULE_ID,
    TIER_NO,
    TIER_NAME_FA,
    LOWER_BOUND,
    UPPER_BOUND,
    BOUND_UNIT_CODE,
    TIER_BASIS_CODE,
    TIER_STRATEGY_CODE,
    FIXED_AMOUNT,
    RATE_VALUE,
    MIN_FEE_AMOUNT,
    MAX_FEE_AMOUNT,
    EFFECTIVE_FROM,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -9202,
    -9009,
    2,
    'پله دوم',
    10000000,
    100000000,
    'IRR',
    'WHOLE_AMOUNT',
    'PERCENTAGE',
    NULL,
    0.002,
    NULL,
    NULL,
    DATE '2025-05-04',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_CALCULATION_TIER (
    TIER_ID,
    CALCULATION_RULE_ID,
    TIER_NO,
    TIER_NAME_FA,
    LOWER_BOUND,
    UPPER_BOUND,
    BOUND_UNIT_CODE,
    TIER_BASIS_CODE,
    TIER_STRATEGY_CODE,
    FIXED_AMOUNT,
    RATE_VALUE,
    MIN_FEE_AMOUNT,
    MAX_FEE_AMOUNT,
    EFFECTIVE_FROM,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -9203,
    -9009,
    3,
    'پله سوم',
    100000000,
    NULL,
    'IRR',
    'WHOLE_AMOUNT',
    'PERCENTAGE',
    NULL,
    0.0015,
    NULL,
    2000000,
    DATE '2025-05-04',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_CALCULATION_TIER (
    TIER_ID,
    CALCULATION_RULE_ID,
    TIER_NO,
    TIER_NAME_FA,
    LOWER_BOUND,
    UPPER_BOUND,
    BOUND_UNIT_CODE,
    TIER_BASIS_CODE,
    TIER_STRATEGY_CODE,
    FIXED_AMOUNT,
    RATE_VALUE,
    MIN_FEE_AMOUNT,
    MAX_FEE_AMOUNT,
    EFFECTIVE_FROM,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -9204,
    -9003,
    1,
    'تا ۲۰ میلیارد ریال',
    0,
    20000000000,
    'IRR',
    'WHOLE_AMOUNT',
    'FIXED',
    2587500,
    NULL,
    NULL,
    NULL,
    DATE '2025-05-04',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_CALCULATION_TIER (
    TIER_ID,
    CALCULATION_RULE_ID,
    TIER_NO,
    TIER_NAME_FA,
    LOWER_BOUND,
    UPPER_BOUND,
    BOUND_UNIT_CODE,
    TIER_BASIS_CODE,
    TIER_STRATEGY_CODE,
    FIXED_AMOUNT,
    RATE_VALUE,
    MIN_FEE_AMOUNT,
    MAX_FEE_AMOUNT,
    EFFECTIVE_FROM,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -9205,
    -9003,
    2,
    'مازاد ۲۰ تا ۴۰۰ میلیارد ریال',
    20000000000,
    400000000000,
    'IRR',
    'EXCESS_OVER_LOWER_BOUND',
    'PERCENTAGE',
    NULL,
    0.002,
    NULL,
    NULL,
    DATE '2025-05-04',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

PROMPT Loading FEE_RULE_COMPONENT ...
INSERT INTO FEE_RULE_COMPONENT (
    RULE_COMPONENT_ID,
    CALCULATION_RULE_ID,
    PARENT_RULE_COMPONENT_ID,
    SEQUENCE_NO,
    NODE_TYPE_CODE,
    OPERATOR_CODE,
    INPUT_CODE,
    CONSTANT_NUMBER,
    CONSTANT_TEXT,
    REFERENCE_CODE,
    DESCRIPTION,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -9301,
    -9003,
    NULL,
    1,
    'OPERATOR',
    'MIN_OF',
    NULL,
    NULL,
    NULL,
    NULL,
    'کمینه محاسبه بانکی و تعرفه کارشناس',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_RULE_COMPONENT (
    RULE_COMPONENT_ID,
    CALCULATION_RULE_ID,
    PARENT_RULE_COMPONENT_ID,
    SEQUENCE_NO,
    NODE_TYPE_CODE,
    OPERATOR_CODE,
    INPUT_CODE,
    CONSTANT_NUMBER,
    CONSTANT_TEXT,
    REFERENCE_CODE,
    DESCRIPTION,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -9302,
    -9003,
    -9301,
    2,
    'OPERATOR',
    'MULTIPLY',
    NULL,
    NULL,
    NULL,
    NULL,
    'محاسبه ۲ در هزار مازاد',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_RULE_COMPONENT (
    RULE_COMPONENT_ID,
    CALCULATION_RULE_ID,
    PARENT_RULE_COMPONENT_ID,
    SEQUENCE_NO,
    NODE_TYPE_CODE,
    OPERATOR_CODE,
    INPUT_CODE,
    CONSTANT_NUMBER,
    CONSTANT_TEXT,
    REFERENCE_CODE,
    DESCRIPTION,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -9303,
    -9003,
    -9302,
    3,
    'INPUT',
    NULL,
    'APPRAISAL_AMOUNT',
    NULL,
    NULL,
    NULL,
    'مبلغ ارزیابی',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_RULE_COMPONENT (
    RULE_COMPONENT_ID,
    CALCULATION_RULE_ID,
    PARENT_RULE_COMPONENT_ID,
    SEQUENCE_NO,
    NODE_TYPE_CODE,
    OPERATOR_CODE,
    INPUT_CODE,
    CONSTANT_NUMBER,
    CONSTANT_TEXT,
    REFERENCE_CODE,
    DESCRIPTION,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -9304,
    -9003,
    -9302,
    4,
    'CONSTANT',
    NULL,
    NULL,
    0.002,
    NULL,
    NULL,
    'نرخ دو در هزار',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_RULE_COMPONENT (
    RULE_COMPONENT_ID,
    CALCULATION_RULE_ID,
    PARENT_RULE_COMPONENT_ID,
    SEQUENCE_NO,
    NODE_TYPE_CODE,
    OPERATOR_CODE,
    INPUT_CODE,
    CONSTANT_NUMBER,
    CONSTANT_TEXT,
    REFERENCE_CODE,
    DESCRIPTION,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -9305,
    -9003,
    -9301,
    5,
    'EXTERNAL_VALUE',
    NULL,
    'OFFICIAL_EXPERT_TARIFF',
    NULL,
    NULL,
    'OFFICIAL_EXPERT_TARIFF',
    'تعرفه کارشناس رسمی',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

PROMPT Loading FEE_CHARGE_COMPONENT ...
INSERT INTO FEE_CHARGE_COMPONENT (
    CHARGE_COMPONENT_ID,
    FEE_DEFINITION_VERSION_ID,
    CALCULATION_RULE_ID,
    COMPONENT_CODE,
    NAME_FA,
    COMPONENT_TYPE_CODE,
    SEQUENCE_NO,
    DISCOUNTABLE_FLAG,
    REFUNDABLE_FLAG,
    TAXABLE_FLAG,
    REVENUE_FLAG,
    DEFAULT_BEARER_ROLE_CODE,
    POSTING_PROFILE_CODE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -9401,
    -7101,
    -9001,
    'BANK_FEE',
    'کارمزد بانک',
    'BANK_REVENUE',
    1,
    'Y',
    'Y',
    'N',
    'Y',
    'ChargeBearer',
    'FEE_DEFAULT',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_CHARGE_COMPONENT (
    CHARGE_COMPONENT_ID,
    FEE_DEFINITION_VERSION_ID,
    CALCULATION_RULE_ID,
    COMPONENT_CODE,
    NAME_FA,
    COMPONENT_TYPE_CODE,
    SEQUENCE_NO,
    DISCOUNTABLE_FLAG,
    REFUNDABLE_FLAG,
    TAXABLE_FLAG,
    REVENUE_FLAG,
    DEFAULT_BEARER_ROLE_CODE,
    POSTING_PROFILE_CODE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -9402,
    -7102,
    -9002,
    'BANK_FEE',
    'کارمزد ضمانت‌نامه',
    'BANK_REVENUE',
    1,
    'Y',
    'Y',
    'N',
    'Y',
    'ChargeBearer',
    'FEE_DEFAULT',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_CHARGE_COMPONENT (
    CHARGE_COMPONENT_ID,
    FEE_DEFINITION_VERSION_ID,
    CALCULATION_RULE_ID,
    COMPONENT_CODE,
    NAME_FA,
    COMPONENT_TYPE_CODE,
    SEQUENCE_NO,
    DISCOUNTABLE_FLAG,
    REFUNDABLE_FLAG,
    TAXABLE_FLAG,
    REVENUE_FLAG,
    DEFAULT_BEARER_ROLE_CODE,
    POSTING_PROFILE_CODE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -9403,
    -7103,
    -9003,
    'BANK_FEE',
    'کارمزد ارزیابی',
    'BANK_REVENUE',
    1,
    'Y',
    'Y',
    'N',
    'Y',
    'ChargeBearer',
    'FEE_DEFAULT',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_CHARGE_COMPONENT (
    CHARGE_COMPONENT_ID,
    FEE_DEFINITION_VERSION_ID,
    CALCULATION_RULE_ID,
    COMPONENT_CODE,
    NAME_FA,
    COMPONENT_TYPE_CODE,
    SEQUENCE_NO,
    DISCOUNTABLE_FLAG,
    REFUNDABLE_FLAG,
    TAXABLE_FLAG,
    REVENUE_FLAG,
    DEFAULT_BEARER_ROLE_CODE,
    POSTING_PROFILE_CODE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -9404,
    -7104,
    -9004,
    'BANK_FEE',
    'کارمزد وصول',
    'BANK_REVENUE',
    1,
    'Y',
    'Y',
    'N',
    'Y',
    'ChargeBearer',
    'FEE_DEFAULT',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_CHARGE_COMPONENT (
    CHARGE_COMPONENT_ID,
    FEE_DEFINITION_VERSION_ID,
    CALCULATION_RULE_ID,
    COMPONENT_CODE,
    NAME_FA,
    COMPONENT_TYPE_CODE,
    SEQUENCE_NO,
    DISCOUNTABLE_FLAG,
    REFUNDABLE_FLAG,
    TAXABLE_FLAG,
    REVENUE_FLAG,
    DEFAULT_BEARER_ROLE_CODE,
    POSTING_PROFILE_CODE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -9405,
    -7104,
    -9010,
    'POSTAGE',
    'هزینه پست',
    'POSTAGE',
    2,
    'N',
    'Y',
    'N',
    'N',
    'ChargeBearer',
    'FEE_DEFAULT',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_CHARGE_COMPONENT (
    CHARGE_COMPONENT_ID,
    FEE_DEFINITION_VERSION_ID,
    CALCULATION_RULE_ID,
    COMPONENT_CODE,
    NAME_FA,
    COMPONENT_TYPE_CODE,
    SEQUENCE_NO,
    DISCOUNTABLE_FLAG,
    REFUNDABLE_FLAG,
    TAXABLE_FLAG,
    REVENUE_FLAG,
    DEFAULT_BEARER_ROLE_CODE,
    POSTING_PROFILE_CODE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -9406,
    -7105,
    -9005,
    'BANK_FEE',
    'کارمزد تعهد',
    'BANK_REVENUE',
    1,
    'Y',
    'Y',
    'N',
    'Y',
    'ChargeBearer',
    'FEE_DEFAULT',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_CHARGE_COMPONENT (
    CHARGE_COMPONENT_ID,
    FEE_DEFINITION_VERSION_ID,
    CALCULATION_RULE_ID,
    COMPONENT_CODE,
    NAME_FA,
    COMPONENT_TYPE_CODE,
    SEQUENCE_NO,
    DISCOUNTABLE_FLAG,
    REFUNDABLE_FLAG,
    TAXABLE_FLAG,
    REVENUE_FLAG,
    DEFAULT_BEARER_ROLE_CODE,
    POSTING_PROFILE_CODE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -9407,
    -7106,
    -9006,
    'BANK_FEE',
    'کارمزد کارشناسی',
    'BANK_REVENUE',
    1,
    'Y',
    'Y',
    'N',
    'Y',
    'ChargeBearer',
    'FEE_DEFAULT',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_CHARGE_COMPONENT (
    CHARGE_COMPONENT_ID,
    FEE_DEFINITION_VERSION_ID,
    CALCULATION_RULE_ID,
    COMPONENT_CODE,
    NAME_FA,
    COMPONENT_TYPE_CODE,
    SEQUENCE_NO,
    DISCOUNTABLE_FLAG,
    REFUNDABLE_FLAG,
    TAXABLE_FLAG,
    REVENUE_FLAG,
    DEFAULT_BEARER_ROLE_CODE,
    POSTING_PROFILE_CODE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -9408,
    -7107,
    -9008,
    'BANK_FEE',
    'کارمزد پایه صدور دسته‌چک',
    'BANK_REVENUE',
    1,
    'Y',
    'Y',
    'N',
    'Y',
    'ChargeBearer',
    'FEE_DEFAULT',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_CHARGE_COMPONENT (
    CHARGE_COMPONENT_ID,
    FEE_DEFINITION_VERSION_ID,
    CALCULATION_RULE_ID,
    COMPONENT_CODE,
    NAME_FA,
    COMPONENT_TYPE_CODE,
    SEQUENCE_NO,
    DISCOUNTABLE_FLAG,
    REFUNDABLE_FLAG,
    TAXABLE_FLAG,
    REVENUE_FLAG,
    DEFAULT_BEARER_ROLE_CODE,
    POSTING_PROFILE_CODE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -9409,
    -7107,
    -9011,
    'PRINTING_COST',
    'هزینه چاپ',
    'PASS_THROUGH_COST',
    2,
    'N',
    'N',
    'N',
    'N',
    'ChargeBearer',
    'FEE_DEFAULT',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_CHARGE_COMPONENT (
    CHARGE_COMPONENT_ID,
    FEE_DEFINITION_VERSION_ID,
    CALCULATION_RULE_ID,
    COMPONENT_CODE,
    NAME_FA,
    COMPONENT_TYPE_CODE,
    SEQUENCE_NO,
    DISCOUNTABLE_FLAG,
    REFUNDABLE_FLAG,
    TAXABLE_FLAG,
    REVENUE_FLAG,
    DEFAULT_BEARER_ROLE_CODE,
    POSTING_PROFILE_CODE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -9410,
    -7107,
    -9012,
    'STAMP_TAX',
    'تمبر مالیاتی',
    'STAMP',
    3,
    'N',
    'N',
    'N',
    'N',
    'ChargeBearer',
    'FEE_DEFAULT',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_CHARGE_COMPONENT (
    CHARGE_COMPONENT_ID,
    FEE_DEFINITION_VERSION_ID,
    CALCULATION_RULE_ID,
    COMPONENT_CODE,
    NAME_FA,
    COMPONENT_TYPE_CODE,
    SEQUENCE_NO,
    DISCOUNTABLE_FLAG,
    REFUNDABLE_FLAG,
    TAXABLE_FLAG,
    REVENUE_FLAG,
    DEFAULT_BEARER_ROLE_CODE,
    POSTING_PROFILE_CODE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -9411,
    -7108,
    -9009,
    'BANK_FEE',
    'کارمزد پلکانی',
    'BANK_REVENUE',
    1,
    'Y',
    'Y',
    'N',
    'Y',
    'ChargeBearer',
    'FEE_DEFAULT',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

PROMPT Loading FEE_CHARGE_STAGE ...
INSERT INTO FEE_CHARGE_STAGE (
    CHARGE_STAGE_ID,
    FEE_DEFINITION_VERSION_ID,
    CHARGE_COMPONENT_ID,
    CALCULATION_RULE_ID,
    STAGE_NO,
    STAGE_CODE,
    NAME_FA,
    TRIGGER_CODE,
    DEDUCT_PREVIOUS_CHARGES_FLAG,
    REFUNDABILITY_CODE,
    EFFECTIVE_FROM,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -9501,
    -7106,
    -9407,
    -9006,
    1,
    'APPLICATION',
    'مرحله پذیرش درخواست',
    'APPLICATION_ACCEPTED',
    'N',
    'NON_REFUNDABLE',
    DATE '2025-05-04',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_CHARGE_STAGE (
    CHARGE_STAGE_ID,
    FEE_DEFINITION_VERSION_ID,
    CHARGE_COMPONENT_ID,
    CALCULATION_RULE_ID,
    STAGE_NO,
    STAGE_CODE,
    NAME_FA,
    TRIGGER_CODE,
    DEDUCT_PREVIOUS_CHARGES_FLAG,
    REFUNDABILITY_CODE,
    EFFECTIVE_FROM,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -9502,
    -7106,
    -9407,
    -9007,
    2,
    'CONTRACT',
    'مرحله تصویب و انعقاد',
    'CONTRACT_SIGNED',
    'Y',
    'RULE_BASED',
    DATE '2025-05-04',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

PROMPT Loading FEE_TIMING_RULE ...
INSERT INTO FEE_TIMING_RULE (
    TIMING_RULE_ID,
    FEE_DEFINITION_VERSION_ID,
    RULE_CODE,
    APPLICATION_TIMING_TYPE_CODE,
    APPLICATION_FREQUENCY_CODE,
    CALCULATION_FREQUENCY_CODE,
    CHARGING_FREQUENCY_CODE,
    EVENT_TRIGGER_CODE,
    DUE_OFFSET_DAYS,
    COLLECTION_TIMING_CODE,
    EFFECTIVE_FROM,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -9601,
    -7101,
    'TIMING_7001',
    'Event-triggered',
    'PerTransaction',
    'PerTransaction',
    'PerTransaction',
    'MONEY_TRANSFER',
    0,
    'IMMEDIATE',
    DATE '2025-05-04',
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_TIMING_RULE (
    TIMING_RULE_ID,
    FEE_DEFINITION_VERSION_ID,
    RULE_CODE,
    APPLICATION_TIMING_TYPE_CODE,
    APPLICATION_FREQUENCY_CODE,
    CALCULATION_FREQUENCY_CODE,
    CHARGING_FREQUENCY_CODE,
    EVENT_TRIGGER_CODE,
    DUE_OFFSET_DAYS,
    COLLECTION_TIMING_CODE,
    EFFECTIVE_FROM,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -9602,
    -7102,
    'TIMING_7002',
    'Event-triggered',
    'PerOccurrence',
    'PerOccurrence',
    'PerOccurrence',
    NULL,
    0,
    'IMMEDIATE',
    DATE '2025-05-04',
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_TIMING_RULE (
    TIMING_RULE_ID,
    FEE_DEFINITION_VERSION_ID,
    RULE_CODE,
    APPLICATION_TIMING_TYPE_CODE,
    APPLICATION_FREQUENCY_CODE,
    CALCULATION_FREQUENCY_CODE,
    CHARGING_FREQUENCY_CODE,
    EVENT_TRIGGER_CODE,
    DUE_OFFSET_DAYS,
    COLLECTION_TIMING_CODE,
    EFFECTIVE_FROM,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -9603,
    -7103,
    'TIMING_7003',
    'Event-triggered',
    'PerOccurrence',
    'PerOccurrence',
    'PerOccurrence',
    NULL,
    0,
    'IMMEDIATE',
    DATE '2025-05-04',
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_TIMING_RULE (
    TIMING_RULE_ID,
    FEE_DEFINITION_VERSION_ID,
    RULE_CODE,
    APPLICATION_TIMING_TYPE_CODE,
    APPLICATION_FREQUENCY_CODE,
    CALCULATION_FREQUENCY_CODE,
    CHARGING_FREQUENCY_CODE,
    EVENT_TRIGGER_CODE,
    DUE_OFFSET_DAYS,
    COLLECTION_TIMING_CODE,
    EFFECTIVE_FROM,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -9604,
    -7104,
    'TIMING_7004',
    'Event-triggered',
    'PerOccurrence',
    'PerOccurrence',
    'PerOccurrence',
    NULL,
    0,
    'IMMEDIATE',
    DATE '2025-05-04',
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_TIMING_RULE (
    TIMING_RULE_ID,
    FEE_DEFINITION_VERSION_ID,
    RULE_CODE,
    APPLICATION_TIMING_TYPE_CODE,
    APPLICATION_FREQUENCY_CODE,
    CALCULATION_FREQUENCY_CODE,
    CHARGING_FREQUENCY_CODE,
    EVENT_TRIGGER_CODE,
    DUE_OFFSET_DAYS,
    COLLECTION_TIMING_CODE,
    EFFECTIVE_FROM,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -9605,
    -7105,
    'TIMING_7005',
    'Recurring (Periodic)',
    'Daily',
    'Daily',
    'Daily',
    'FACILITY_UNUSED_LIMIT',
    0,
    'IMMEDIATE',
    DATE '2025-05-04',
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_TIMING_RULE (
    TIMING_RULE_ID,
    FEE_DEFINITION_VERSION_ID,
    RULE_CODE,
    APPLICATION_TIMING_TYPE_CODE,
    APPLICATION_FREQUENCY_CODE,
    CALCULATION_FREQUENCY_CODE,
    CHARGING_FREQUENCY_CODE,
    EVENT_TRIGGER_CODE,
    DUE_OFFSET_DAYS,
    COLLECTION_TIMING_CODE,
    EFFECTIVE_FROM,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -9606,
    -7106,
    'TIMING_7006',
    'Event-triggered',
    'PerOccurrence',
    'PerOccurrence',
    'PerOccurrence',
    'CREDIT_APPLICATION',
    0,
    'IMMEDIATE',
    DATE '2025-05-04',
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_TIMING_RULE (
    TIMING_RULE_ID,
    FEE_DEFINITION_VERSION_ID,
    RULE_CODE,
    APPLICATION_TIMING_TYPE_CODE,
    APPLICATION_FREQUENCY_CODE,
    CALCULATION_FREQUENCY_CODE,
    CHARGING_FREQUENCY_CODE,
    EVENT_TRIGGER_CODE,
    DUE_OFFSET_DAYS,
    COLLECTION_TIMING_CODE,
    EFFECTIVE_FROM,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -9607,
    -7107,
    'TIMING_7007',
    'Event-triggered',
    'PerOccurrence',
    'PerOccurrence',
    'PerOccurrence',
    NULL,
    0,
    'IMMEDIATE',
    DATE '2025-05-04',
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_TIMING_RULE (
    TIMING_RULE_ID,
    FEE_DEFINITION_VERSION_ID,
    RULE_CODE,
    APPLICATION_TIMING_TYPE_CODE,
    APPLICATION_FREQUENCY_CODE,
    CALCULATION_FREQUENCY_CODE,
    CHARGING_FREQUENCY_CODE,
    EVENT_TRIGGER_CODE,
    DUE_OFFSET_DAYS,
    COLLECTION_TIMING_CODE,
    EFFECTIVE_FROM,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -9608,
    -7108,
    'TIMING_7008',
    'Event-triggered',
    'PerOccurrence',
    'PerOccurrence',
    'PerOccurrence',
    NULL,
    0,
    'IMMEDIATE',
    DATE '2025-05-04',
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

PROMPT Loading FEE_CURRENCY_RULE ...
INSERT INTO FEE_CURRENCY_RULE (
    CURRENCY_RULE_ID,
    FEE_DEFINITION_VERSION_ID,
    RULE_CODE,
    CURRENCY_MODALITY_TYPE_CODE,
    DEFINITION_CURRENCY_CODE,
    BASIS_CURRENCY_MODE_CODE,
    POSTING_CURRENCY_MODE_CODE,
    POSTING_CURRENCY_CODE,
    CONVERSION_REQUIRED_FLAG,
    ROUNDING_MODE_CODE,
    ROUNDING_SCALE,
    EFFECTIVE_FROM,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -9701,
    -7101,
    'CURRENCY_7001',
    'Transaction Currency',
    'IRR',
    'TRANSACTION_CURRENCY',
    'FIXED_CURRENCY',
    'IRR',
    'N',
    'HALF_UP',
    0,
    DATE '2025-05-04',
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_CURRENCY_RULE (
    CURRENCY_RULE_ID,
    FEE_DEFINITION_VERSION_ID,
    RULE_CODE,
    CURRENCY_MODALITY_TYPE_CODE,
    DEFINITION_CURRENCY_CODE,
    BASIS_CURRENCY_MODE_CODE,
    POSTING_CURRENCY_MODE_CODE,
    POSTING_CURRENCY_CODE,
    CONVERSION_REQUIRED_FLAG,
    ROUNDING_MODE_CODE,
    ROUNDING_SCALE,
    EFFECTIVE_FROM,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -9702,
    -7102,
    'CURRENCY_7002',
    'Transaction Currency',
    'IRR',
    'TRANSACTION_CURRENCY',
    'FIXED_CURRENCY',
    'IRR',
    'N',
    'HALF_UP',
    0,
    DATE '2025-05-04',
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_CURRENCY_RULE (
    CURRENCY_RULE_ID,
    FEE_DEFINITION_VERSION_ID,
    RULE_CODE,
    CURRENCY_MODALITY_TYPE_CODE,
    DEFINITION_CURRENCY_CODE,
    BASIS_CURRENCY_MODE_CODE,
    POSTING_CURRENCY_MODE_CODE,
    POSTING_CURRENCY_CODE,
    CONVERSION_REQUIRED_FLAG,
    ROUNDING_MODE_CODE,
    ROUNDING_SCALE,
    EFFECTIVE_FROM,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -9703,
    -7103,
    'CURRENCY_7003',
    'Transaction Currency',
    'IRR',
    'TRANSACTION_CURRENCY',
    'FIXED_CURRENCY',
    'IRR',
    'N',
    'HALF_UP',
    0,
    DATE '2025-05-04',
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_CURRENCY_RULE (
    CURRENCY_RULE_ID,
    FEE_DEFINITION_VERSION_ID,
    RULE_CODE,
    CURRENCY_MODALITY_TYPE_CODE,
    DEFINITION_CURRENCY_CODE,
    BASIS_CURRENCY_MODE_CODE,
    POSTING_CURRENCY_MODE_CODE,
    POSTING_CURRENCY_CODE,
    CONVERSION_REQUIRED_FLAG,
    ROUNDING_MODE_CODE,
    ROUNDING_SCALE,
    EFFECTIVE_FROM,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -9704,
    -7104,
    'CURRENCY_7004',
    'Transaction Currency',
    'IRR',
    'TRANSACTION_CURRENCY',
    'FIXED_CURRENCY',
    'IRR',
    'N',
    'HALF_UP',
    0,
    DATE '2025-05-04',
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_CURRENCY_RULE (
    CURRENCY_RULE_ID,
    FEE_DEFINITION_VERSION_ID,
    RULE_CODE,
    CURRENCY_MODALITY_TYPE_CODE,
    DEFINITION_CURRENCY_CODE,
    BASIS_CURRENCY_MODE_CODE,
    POSTING_CURRENCY_MODE_CODE,
    POSTING_CURRENCY_CODE,
    CONVERSION_REQUIRED_FLAG,
    ROUNDING_MODE_CODE,
    ROUNDING_SCALE,
    EFFECTIVE_FROM,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -9705,
    -7105,
    'CURRENCY_7005',
    'Transaction Currency',
    'IRR',
    'TRANSACTION_CURRENCY',
    'FIXED_CURRENCY',
    'IRR',
    'N',
    'HALF_UP',
    0,
    DATE '2025-05-04',
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_CURRENCY_RULE (
    CURRENCY_RULE_ID,
    FEE_DEFINITION_VERSION_ID,
    RULE_CODE,
    CURRENCY_MODALITY_TYPE_CODE,
    DEFINITION_CURRENCY_CODE,
    BASIS_CURRENCY_MODE_CODE,
    POSTING_CURRENCY_MODE_CODE,
    POSTING_CURRENCY_CODE,
    CONVERSION_REQUIRED_FLAG,
    ROUNDING_MODE_CODE,
    ROUNDING_SCALE,
    EFFECTIVE_FROM,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -9706,
    -7106,
    'CURRENCY_7006',
    'Transaction Currency',
    'IRR',
    'TRANSACTION_CURRENCY',
    'FIXED_CURRENCY',
    'IRR',
    'N',
    'HALF_UP',
    0,
    DATE '2025-05-04',
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_CURRENCY_RULE (
    CURRENCY_RULE_ID,
    FEE_DEFINITION_VERSION_ID,
    RULE_CODE,
    CURRENCY_MODALITY_TYPE_CODE,
    DEFINITION_CURRENCY_CODE,
    BASIS_CURRENCY_MODE_CODE,
    POSTING_CURRENCY_MODE_CODE,
    POSTING_CURRENCY_CODE,
    CONVERSION_REQUIRED_FLAG,
    ROUNDING_MODE_CODE,
    ROUNDING_SCALE,
    EFFECTIVE_FROM,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -9707,
    -7107,
    'CURRENCY_7007',
    'Transaction Currency',
    'IRR',
    'TRANSACTION_CURRENCY',
    'FIXED_CURRENCY',
    'IRR',
    'N',
    'HALF_UP',
    0,
    DATE '2025-05-04',
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_CURRENCY_RULE (
    CURRENCY_RULE_ID,
    FEE_DEFINITION_VERSION_ID,
    RULE_CODE,
    CURRENCY_MODALITY_TYPE_CODE,
    DEFINITION_CURRENCY_CODE,
    BASIS_CURRENCY_MODE_CODE,
    POSTING_CURRENCY_MODE_CODE,
    POSTING_CURRENCY_CODE,
    CONVERSION_REQUIRED_FLAG,
    ROUNDING_MODE_CODE,
    ROUNDING_SCALE,
    EFFECTIVE_FROM,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -9708,
    -7108,
    'CURRENCY_7008',
    'Transaction Currency',
    'IRR',
    'TRANSACTION_CURRENCY',
    'FIXED_CURRENCY',
    'IRR',
    'N',
    'HALF_UP',
    0,
    DATE '2025-05-04',
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

PROMPT Loading FEE_COLLECTION_RULE ...
INSERT INTO FEE_COLLECTION_RULE (
    COLLECTION_RULE_ID,
    FEE_DEFINITION_VERSION_ID,
    RULE_CODE,
    PAYMENT_METHOD_CODE,
    COLLECTION_MODE_CODE,
    CHARGE_ACCOUNT_ROLE_CODE,
    REFERENCE_REQUIRED_FLAG,
    PARTIAL_COLLECTION_ALLOWED_FLAG,
    FAILURE_ACTION_CODE,
    EFFECTIVE_FROM,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -9801,
    -7101,
    'COLLECT_7001',
    'Cash',
    'ACCOUNT_DEBIT',
    'CHARGE_ACCOUNT',
    'N',
    'N',
    'REJECT_TRANSACTION',
    DATE '2025-05-04',
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_COLLECTION_RULE (
    COLLECTION_RULE_ID,
    FEE_DEFINITION_VERSION_ID,
    RULE_CODE,
    PAYMENT_METHOD_CODE,
    COLLECTION_MODE_CODE,
    CHARGE_ACCOUNT_ROLE_CODE,
    REFERENCE_REQUIRED_FLAG,
    PARTIAL_COLLECTION_ALLOWED_FLAG,
    FAILURE_ACTION_CODE,
    EFFECTIVE_FROM,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -9802,
    -7102,
    'COLLECT_7002',
    'Cash',
    'ACCOUNT_DEBIT',
    'CHARGE_ACCOUNT',
    'N',
    'N',
    'REJECT_TRANSACTION',
    DATE '2025-05-04',
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_COLLECTION_RULE (
    COLLECTION_RULE_ID,
    FEE_DEFINITION_VERSION_ID,
    RULE_CODE,
    PAYMENT_METHOD_CODE,
    COLLECTION_MODE_CODE,
    CHARGE_ACCOUNT_ROLE_CODE,
    REFERENCE_REQUIRED_FLAG,
    PARTIAL_COLLECTION_ALLOWED_FLAG,
    FAILURE_ACTION_CODE,
    EFFECTIVE_FROM,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -9803,
    -7103,
    'COLLECT_7003',
    'Cash',
    'ACCOUNT_DEBIT',
    'CHARGE_ACCOUNT',
    'N',
    'N',
    'REJECT_TRANSACTION',
    DATE '2025-05-04',
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_COLLECTION_RULE (
    COLLECTION_RULE_ID,
    FEE_DEFINITION_VERSION_ID,
    RULE_CODE,
    PAYMENT_METHOD_CODE,
    COLLECTION_MODE_CODE,
    CHARGE_ACCOUNT_ROLE_CODE,
    REFERENCE_REQUIRED_FLAG,
    PARTIAL_COLLECTION_ALLOWED_FLAG,
    FAILURE_ACTION_CODE,
    EFFECTIVE_FROM,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -9804,
    -7104,
    'COLLECT_7004',
    'Cash',
    'ACCOUNT_DEBIT',
    'CHARGE_ACCOUNT',
    'N',
    'N',
    'REJECT_TRANSACTION',
    DATE '2025-05-04',
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_COLLECTION_RULE (
    COLLECTION_RULE_ID,
    FEE_DEFINITION_VERSION_ID,
    RULE_CODE,
    PAYMENT_METHOD_CODE,
    COLLECTION_MODE_CODE,
    CHARGE_ACCOUNT_ROLE_CODE,
    REFERENCE_REQUIRED_FLAG,
    PARTIAL_COLLECTION_ALLOWED_FLAG,
    FAILURE_ACTION_CODE,
    EFFECTIVE_FROM,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -9805,
    -7105,
    'COLLECT_7005',
    'Cash',
    'ACCOUNT_DEBIT',
    'CHARGE_ACCOUNT',
    'N',
    'N',
    'REJECT_TRANSACTION',
    DATE '2025-05-04',
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_COLLECTION_RULE (
    COLLECTION_RULE_ID,
    FEE_DEFINITION_VERSION_ID,
    RULE_CODE,
    PAYMENT_METHOD_CODE,
    COLLECTION_MODE_CODE,
    CHARGE_ACCOUNT_ROLE_CODE,
    REFERENCE_REQUIRED_FLAG,
    PARTIAL_COLLECTION_ALLOWED_FLAG,
    FAILURE_ACTION_CODE,
    EFFECTIVE_FROM,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -9806,
    -7106,
    'COLLECT_7006',
    'Cash',
    'ACCOUNT_DEBIT',
    'CHARGE_ACCOUNT',
    'N',
    'N',
    'REJECT_TRANSACTION',
    DATE '2025-05-04',
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_COLLECTION_RULE (
    COLLECTION_RULE_ID,
    FEE_DEFINITION_VERSION_ID,
    RULE_CODE,
    PAYMENT_METHOD_CODE,
    COLLECTION_MODE_CODE,
    CHARGE_ACCOUNT_ROLE_CODE,
    REFERENCE_REQUIRED_FLAG,
    PARTIAL_COLLECTION_ALLOWED_FLAG,
    FAILURE_ACTION_CODE,
    EFFECTIVE_FROM,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -9807,
    -7107,
    'COLLECT_7007',
    'Cash',
    'ACCOUNT_DEBIT',
    'CHARGE_ACCOUNT',
    'N',
    'N',
    'REJECT_TRANSACTION',
    DATE '2025-05-04',
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_COLLECTION_RULE (
    COLLECTION_RULE_ID,
    FEE_DEFINITION_VERSION_ID,
    RULE_CODE,
    PAYMENT_METHOD_CODE,
    COLLECTION_MODE_CODE,
    CHARGE_ACCOUNT_ROLE_CODE,
    REFERENCE_REQUIRED_FLAG,
    PARTIAL_COLLECTION_ALLOWED_FLAG,
    FAILURE_ACTION_CODE,
    EFFECTIVE_FROM,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -9808,
    -7108,
    'COLLECT_7008',
    'Cash',
    'ACCOUNT_DEBIT',
    'CHARGE_ACCOUNT',
    'N',
    'N',
    'REJECT_TRANSACTION',
    DATE '2025-05-04',
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

PROMPT Loading FEE_POSTING_RULE ...
INSERT INTO FEE_POSTING_RULE (
    POSTING_RULE_ID,
    FEE_DEFINITION_VERSION_ID,
    RULE_CODE,
    POSTING_MODALITY_TYPE_CODE,
    POSTING_PURPOSE_CODE,
    ACCOUNTING_PROFILE_CODE,
    DEBIT_ACCOUNT_ROLE_CODE,
    CREDIT_ACCOUNT_ROLE_CODE,
    NARRATIVE_TEMPLATE,
    EFFECTIVE_FROM,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -9901,
    -7101,
    'POST_7001',
    'Immediate Posting',
    'CHARGE',
    'FEE_DEFAULT',
    'CHARGE_ACCOUNT',
    'CREDIT_ACCOUNT',
    'ثبت کارمزد {FEE_CODE}',
    DATE '2025-05-04',
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_POSTING_RULE (
    POSTING_RULE_ID,
    FEE_DEFINITION_VERSION_ID,
    RULE_CODE,
    POSTING_MODALITY_TYPE_CODE,
    POSTING_PURPOSE_CODE,
    ACCOUNTING_PROFILE_CODE,
    DEBIT_ACCOUNT_ROLE_CODE,
    CREDIT_ACCOUNT_ROLE_CODE,
    NARRATIVE_TEMPLATE,
    EFFECTIVE_FROM,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -9902,
    -7102,
    'POST_7002',
    'Immediate Posting',
    'CHARGE',
    'FEE_DEFAULT',
    'CHARGE_ACCOUNT',
    'CREDIT_ACCOUNT',
    'ثبت کارمزد {FEE_CODE}',
    DATE '2025-05-04',
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_POSTING_RULE (
    POSTING_RULE_ID,
    FEE_DEFINITION_VERSION_ID,
    RULE_CODE,
    POSTING_MODALITY_TYPE_CODE,
    POSTING_PURPOSE_CODE,
    ACCOUNTING_PROFILE_CODE,
    DEBIT_ACCOUNT_ROLE_CODE,
    CREDIT_ACCOUNT_ROLE_CODE,
    NARRATIVE_TEMPLATE,
    EFFECTIVE_FROM,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -9903,
    -7103,
    'POST_7003',
    'Immediate Posting',
    'CHARGE',
    'FEE_DEFAULT',
    'CHARGE_ACCOUNT',
    'CREDIT_ACCOUNT',
    'ثبت کارمزد {FEE_CODE}',
    DATE '2025-05-04',
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_POSTING_RULE (
    POSTING_RULE_ID,
    FEE_DEFINITION_VERSION_ID,
    RULE_CODE,
    POSTING_MODALITY_TYPE_CODE,
    POSTING_PURPOSE_CODE,
    ACCOUNTING_PROFILE_CODE,
    DEBIT_ACCOUNT_ROLE_CODE,
    CREDIT_ACCOUNT_ROLE_CODE,
    NARRATIVE_TEMPLATE,
    EFFECTIVE_FROM,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -9904,
    -7104,
    'POST_7004',
    'Immediate Posting',
    'CHARGE',
    'FEE_DEFAULT',
    'CHARGE_ACCOUNT',
    'CREDIT_ACCOUNT',
    'ثبت کارمزد {FEE_CODE}',
    DATE '2025-05-04',
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_POSTING_RULE (
    POSTING_RULE_ID,
    FEE_DEFINITION_VERSION_ID,
    RULE_CODE,
    POSTING_MODALITY_TYPE_CODE,
    POSTING_PURPOSE_CODE,
    ACCOUNTING_PROFILE_CODE,
    DEBIT_ACCOUNT_ROLE_CODE,
    CREDIT_ACCOUNT_ROLE_CODE,
    NARRATIVE_TEMPLATE,
    EFFECTIVE_FROM,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -9905,
    -7105,
    'POST_7005',
    'Immediate Posting',
    'CHARGE',
    'FEE_DEFAULT',
    'CHARGE_ACCOUNT',
    'CREDIT_ACCOUNT',
    'ثبت کارمزد {FEE_CODE}',
    DATE '2025-05-04',
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_POSTING_RULE (
    POSTING_RULE_ID,
    FEE_DEFINITION_VERSION_ID,
    RULE_CODE,
    POSTING_MODALITY_TYPE_CODE,
    POSTING_PURPOSE_CODE,
    ACCOUNTING_PROFILE_CODE,
    DEBIT_ACCOUNT_ROLE_CODE,
    CREDIT_ACCOUNT_ROLE_CODE,
    NARRATIVE_TEMPLATE,
    EFFECTIVE_FROM,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -9906,
    -7106,
    'POST_7006',
    'Immediate Posting',
    'CHARGE',
    'FEE_DEFAULT',
    'CHARGE_ACCOUNT',
    'CREDIT_ACCOUNT',
    'ثبت کارمزد {FEE_CODE}',
    DATE '2025-05-04',
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_POSTING_RULE (
    POSTING_RULE_ID,
    FEE_DEFINITION_VERSION_ID,
    RULE_CODE,
    POSTING_MODALITY_TYPE_CODE,
    POSTING_PURPOSE_CODE,
    ACCOUNTING_PROFILE_CODE,
    DEBIT_ACCOUNT_ROLE_CODE,
    CREDIT_ACCOUNT_ROLE_CODE,
    NARRATIVE_TEMPLATE,
    EFFECTIVE_FROM,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -9907,
    -7107,
    'POST_7007',
    'Immediate Posting',
    'CHARGE',
    'FEE_DEFAULT',
    'CHARGE_ACCOUNT',
    'CREDIT_ACCOUNT',
    'ثبت کارمزد {FEE_CODE}',
    DATE '2025-05-04',
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_POSTING_RULE (
    POSTING_RULE_ID,
    FEE_DEFINITION_VERSION_ID,
    RULE_CODE,
    POSTING_MODALITY_TYPE_CODE,
    POSTING_PURPOSE_CODE,
    ACCOUNTING_PROFILE_CODE,
    DEBIT_ACCOUNT_ROLE_CODE,
    CREDIT_ACCOUNT_ROLE_CODE,
    NARRATIVE_TEMPLATE,
    EFFECTIVE_FROM,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -9908,
    -7108,
    'POST_7008',
    'Immediate Posting',
    'CHARGE',
    'FEE_DEFAULT',
    'CHARGE_ACCOUNT',
    'CREDIT_ACCOUNT',
    'ثبت کارمزد {FEE_CODE}',
    DATE '2025-05-04',
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

PROMPT Loading FEE_ADJUSTMENT_POLICY ...
INSERT INTO FEE_ADJUSTMENT_POLICY (
    ADJUSTMENT_POLICY_ID,
    FEE_DEFINITION_VERSION_ID,
    APPLICABILITY_RULE_ID,
    REG_DISCOUNT_LIMIT_ID,
    CHARGE_COMPONENT_ID,
    POLICY_CODE,
    NAME_FA,
    ADJUSTMENT_TYPE_CODE,
    PRICING_FEATURE_TYPE_CODE,
    PRIORITY_NO,
    PERCENT_VALUE,
    STACKING_MODE_CODE,
    APPROVAL_REQUIRED_FLAG,
    EFFECTIVE_FROM,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -10001,
    -7101,
    -8002,
    -6101,
    -9401,
    'VIP_20',
    'تخفیف ۲۰ درصدی VIP',
    'PERCENT_REDUCTION',
    'STANDARD',
    10,
    0.2,
    'BEST_BENEFIT',
    'N',
    DATE '2026-01-01',
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_ADJUSTMENT_POLICY (
    ADJUSTMENT_POLICY_ID,
    FEE_DEFINITION_VERSION_ID,
    REG_DISCOUNT_LIMIT_ID,
    CHARGE_COMPONENT_ID,
    POLICY_CODE,
    NAME_FA,
    ADJUSTMENT_TYPE_CODE,
    PRICING_FEATURE_TYPE_CODE,
    PRIORITY_NO,
    PERCENT_VALUE,
    STACKING_MODE_CODE,
    APPROVAL_REQUIRED_FLAG,
    EFFECTIVE_FROM,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -10002,
    -7102,
    -6103,
    -9402,
    'KB_40',
    'تخفیف ۴۰ درصدی شرکت دانش‌بنیان',
    'PERCENT_REDUCTION',
    'STANDARD',
    10,
    0.4,
    'EXCLUSIVE',
    'N',
    DATE '2025-05-04',
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

PROMPT Loading FEE_ALLOCATION_POLICY ...
INSERT INTO FEE_ALLOCATION_POLICY (
    ALLOCATION_POLICY_ID,
    FEE_DEFINITION_VERSION_ID,
    POLICY_CODE,
    NAME_FA,
    ALLOCATION_METHOD_CODE,
    RESIDUAL_HANDLING_CODE,
    CURRENCY_MODE_CODE,
    EFFECTIVE_FROM,
    IS_ACTIVE,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -10101,
    -7101,
    'TRANSFER_SHARE',
    'تسهیم کارمزد انتقال نمونه',
    'PERCENT',
    'BANK',
    'FEE_CURRENCY',
    DATE '2026-01-01',
    'Y',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

PROMPT Loading FEE_ALLOCATION_RULE ...
INSERT INTO FEE_ALLOCATION_RULE (
    ALLOCATION_RULE_ID,
    ALLOCATION_POLICY_ID,
    SEQUENCE_NO,
    BENEFICIARY_ROLE_CODE,
    DEMO_BENEFICIARY_PARTY_ID,
    ALLOCATION_BASIS_CODE,
    PERCENT_VALUE,
    PRIORITY_NO,
    SETTLEMENT_METHOD_CODE,
    EFFECTIVE_FROM,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -10201,
    -10101,
    1,
    'BANK',
    -2001,
    'PERCENT',
    0.7,
    1,
    'INTERNAL',
    DATE '2026-01-01',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_ALLOCATION_RULE (
    ALLOCATION_RULE_ID,
    ALLOCATION_POLICY_ID,
    SEQUENCE_NO,
    BENEFICIARY_ROLE_CODE,
    DEMO_BENEFICIARY_PARTY_ID,
    ALLOCATION_BASIS_CODE,
    PERCENT_VALUE,
    PRIORITY_NO,
    SETTLEMENT_METHOD_CODE,
    EFFECTIVE_FROM,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -10202,
    -10101,
    2,
    'AGENT_BRANCH',
    -2002,
    'PERCENT',
    0.2,
    2,
    'INTERNAL',
    DATE '2026-01-01',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_ALLOCATION_RULE (
    ALLOCATION_RULE_ID,
    ALLOCATION_POLICY_ID,
    SEQUENCE_NO,
    BENEFICIARY_ROLE_CODE,
    DEMO_BENEFICIARY_PARTY_ID,
    ALLOCATION_BASIS_CODE,
    PERCENT_VALUE,
    PRIORITY_NO,
    SETTLEMENT_METHOD_CODE,
    EFFECTIVE_FROM,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -10203,
    -10101,
    3,
    'EXTERNAL_ORG',
    -2007,
    'PERCENT',
    0.1,
    3,
    'TRANSFER',
    DATE '2026-01-01',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

COMMIT;

PROMPT ===== 05_fee_arrangement_data.sql =====
-- ============================================================================
-- 05_fee_arrangement_data.sql
-- Fee Prototype Target Model Baseline 1.0 / Oracle
-- UTF-8
-- ============================================================================
SET DEFINE OFF;
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK;
ALTER SESSION SET CURRENT_SCHEMA = FEE;

PROMPT Loading FEE_ARRANGEMENT ...
INSERT INTO FEE_ARRANGEMENT (
    FEE_ARRANGEMENT_ID,
    ARRANGEMENT_NO,
    FEE_PRODUCT_FEATURE_ID,
    FEE_DEFINITION_VERSION_ID,
    ORIGIN_CODE,
    STATUS_CODE,
    EFFECTIVE_FROM,
    DESCRIPTION,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -11001,
    'ARR-TRANSFER-VIP-001',
    -7201,
    -7101,
    'NEGOTIATED',
    'ACTIVE',
    DATE '2026-01-01',
    'ترتیب آزمایشی انتقال برای مشتری VIP',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_ARRANGEMENT (
    FEE_ARRANGEMENT_ID,
    ARRANGEMENT_NO,
    FEE_PRODUCT_FEATURE_ID,
    FEE_DEFINITION_VERSION_ID,
    ORIGIN_CODE,
    STATUS_CODE,
    EFFECTIVE_FROM,
    DESCRIPTION,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -11002,
    'ARR-GUARANTEE-001',
    -7202,
    -7102,
    'STANDARD',
    'ACTIVE',
    DATE '2025-05-04',
    'ترتیب استاندارد ضمانت‌نامه',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

PROMPT Loading FEE_ARRANGEMENT_INVOLVEMENT ...
INSERT INTO FEE_ARRANGEMENT_INVOLVEMENT (
    INVOLVEMENT_ID,
    FEE_ARRANGEMENT_ID,
    DEMO_PARTY_ID,
    INVOLVEMENT_ROLE_CODE,
    ROLE_DETAIL_CODE,
    IS_PRIMARY_FLAG,
    EFFECTIVE_FROM,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -11101,
    -11001,
    -2004,
    'ChargeBearer',
    'CUSTOMER',
    'Y',
    DATE '2026-01-01',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_ARRANGEMENT_INVOLVEMENT (
    INVOLVEMENT_ID,
    FEE_ARRANGEMENT_ID,
    DEMO_PARTY_ID,
    INVOLVEMENT_ROLE_CODE,
    ROLE_DETAIL_CODE,
    IS_PRIMARY_FLAG,
    EFFECTIVE_FROM,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -11102,
    -11001,
    -2001,
    'ChargeRecipient',
    'BANK',
    'Y',
    DATE '2026-01-01',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_ARRANGEMENT_INVOLVEMENT (
    INVOLVEMENT_ID,
    FEE_ARRANGEMENT_ID,
    DEMO_PARTY_ID,
    INVOLVEMENT_ROLE_CODE,
    ROLE_DETAIL_CODE,
    IS_PRIMARY_FLAG,
    EFFECTIVE_FROM,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -11103,
    -11001,
    -2002,
    'ChargeAgent',
    'BRANCH',
    'Y',
    DATE '2026-01-01',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_ARRANGEMENT_INVOLVEMENT (
    INVOLVEMENT_ID,
    FEE_ARRANGEMENT_ID,
    DEMO_PARTY_ID,
    INVOLVEMENT_ROLE_CODE,
    ROLE_DETAIL_CODE,
    IS_PRIMARY_FLAG,
    EFFECTIVE_FROM,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -11104,
    -11002,
    -2003,
    'ChargeBearer',
    'CUSTOMER',
    'Y',
    DATE '2026-01-01',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_ARRANGEMENT_INVOLVEMENT (
    INVOLVEMENT_ID,
    FEE_ARRANGEMENT_ID,
    DEMO_PARTY_ID,
    INVOLVEMENT_ROLE_CODE,
    ROLE_DETAIL_CODE,
    IS_PRIMARY_FLAG,
    EFFECTIVE_FROM,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -11105,
    -11002,
    -2001,
    'ChargeRecipient',
    'BANK',
    'Y',
    DATE '2026-01-01',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

PROMPT Loading FEE_ARRANGEMENT_ACCOUNT ...
INSERT INTO FEE_ARRANGEMENT_ACCOUNT (
    ARRANGEMENT_ACCOUNT_ID,
    FEE_ARRANGEMENT_ID,
    DEMO_ACCOUNT_ID,
    ACCOUNT_ROLE_CODE,
    IS_PRIMARY_FLAG,
    EFFECTIVE_FROM,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -11201,
    -11001,
    -3202,
    'CHARGE_ACCOUNT',
    'Y',
    DATE '2026-01-01',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_ARRANGEMENT_ACCOUNT (
    ARRANGEMENT_ACCOUNT_ID,
    FEE_ARRANGEMENT_ID,
    DEMO_ACCOUNT_ID,
    ACCOUNT_ROLE_CODE,
    IS_PRIMARY_FLAG,
    EFFECTIVE_FROM,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -11202,
    -11001,
    -3204,
    'CREDIT_ACCOUNT',
    'Y',
    DATE '2026-01-01',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_ARRANGEMENT_ACCOUNT (
    ARRANGEMENT_ACCOUNT_ID,
    FEE_ARRANGEMENT_ID,
    DEMO_ACCOUNT_ID,
    ACCOUNT_ROLE_CODE,
    IS_PRIMARY_FLAG,
    EFFECTIVE_FROM,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -11203,
    -11002,
    -3201,
    'CHARGE_ACCOUNT',
    'Y',
    DATE '2026-01-01',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_ARRANGEMENT_ACCOUNT (
    ARRANGEMENT_ACCOUNT_ID,
    FEE_ARRANGEMENT_ID,
    DEMO_ACCOUNT_ID,
    ACCOUNT_ROLE_CODE,
    IS_PRIMARY_FLAG,
    EFFECTIVE_FROM,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -11204,
    -11002,
    -3204,
    'CREDIT_ACCOUNT',
    'Y',
    DATE '2026-01-01',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

PROMPT Loading FEE_ARRANGEMENT_MODALITY ...
INSERT INTO FEE_ARRANGEMENT_MODALITY (
    ARRANGEMENT_MODALITY_ID,
    FEE_ARRANGEMENT_ID,
    MODALITY_TYPE_CODE,
    TIMING_RULE_ID,
    CURRENCY_RULE_ID,
    COLLECTION_RULE_ID,
    POSTING_RULE_ID,
    EFFECTIVE_FROM,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -11301,
    -11001,
    'Fee Application Timing Modality',
    -9601,
    NULL,
    NULL,
    NULL,
    DATE '2026-01-01',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_ARRANGEMENT_MODALITY (
    ARRANGEMENT_MODALITY_ID,
    FEE_ARRANGEMENT_ID,
    MODALITY_TYPE_CODE,
    TIMING_RULE_ID,
    CURRENCY_RULE_ID,
    COLLECTION_RULE_ID,
    POSTING_RULE_ID,
    EFFECTIVE_FROM,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -11302,
    -11001,
    'Fee Currency Modality',
    NULL,
    -9701,
    NULL,
    NULL,
    DATE '2026-01-01',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_ARRANGEMENT_MODALITY (
    ARRANGEMENT_MODALITY_ID,
    FEE_ARRANGEMENT_ID,
    MODALITY_TYPE_CODE,
    TIMING_RULE_ID,
    CURRENCY_RULE_ID,
    COLLECTION_RULE_ID,
    POSTING_RULE_ID,
    EFFECTIVE_FROM,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -11303,
    -11001,
    'Fee Calculation Modality',
    NULL,
    NULL,
    NULL,
    NULL,
    DATE '2026-01-01',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_ARRANGEMENT_MODALITY (
    ARRANGEMENT_MODALITY_ID,
    FEE_ARRANGEMENT_ID,
    MODALITY_TYPE_CODE,
    TIMING_RULE_ID,
    CURRENCY_RULE_ID,
    COLLECTION_RULE_ID,
    POSTING_RULE_ID,
    EFFECTIVE_FROM,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -11304,
    -11001,
    'Fee Posting Modality',
    NULL,
    NULL,
    NULL,
    -9901,
    DATE '2026-01-01',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

INSERT INTO FEE_ARRANGEMENT_MODALITY (
    ARRANGEMENT_MODALITY_ID,
    FEE_ARRANGEMENT_ID,
    MODALITY_TYPE_CODE,
    TIMING_RULE_ID,
    CURRENCY_RULE_ID,
    COLLECTION_RULE_ID,
    POSTING_RULE_ID,
    EFFECTIVE_FROM,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -11305,
    -11001,
    'Payment Modality',
    NULL,
    NULL,
    -9801,
    NULL,
    DATE '2026-01-01',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

PROMPT Loading FEE_ARRANGEMENT_CALC_TERM ...
INSERT INTO FEE_ARRANGEMENT_CALC_TERM (
    ARRANGEMENT_CALC_TERM_ID,
    FEE_ARRANGEMENT_ID,
    BASE_CALCULATION_RULE_ID,
    CALCULATION_STRATEGY_CODE,
    RATE_VALUE,
    MIN_FEE_AMOUNT,
    MAX_FEE_AMOUNT,
    CURRENCY_CODE,
    ROUNDING_MODE_CODE,
    ROUNDING_SCALE,
    EFFECTIVE_FROM,
    APPROVAL_REF,
    DESCRIPTION,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -11401,
    -11001,
    -9001,
    'PERCENTAGE_FLOOR_CAP',
    0.0015,
    50000,
    2000000,
    'IRR',
    'HALF_UP',
    0,
    DATE '2026-01-01',
    'NEG-APP-001',
    'نرخ توافقی ۰.۱۵ درصد برای مشتری VIP',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

COMMIT;

PROMPT ===== 06_fee_runtime_sample_data.sql =====
-- ============================================================================
-- 06_fee_runtime_sample_data.sql
-- Fee Prototype Target Model Baseline 1.0 / Oracle
-- UTF-8
-- ============================================================================
SET DEFINE OFF;
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK;
ALTER SESSION SET CURRENT_SCHEMA = FEE;

PROMPT Loading FEE_INSTRUCTION ...
INSERT INTO FEE_INSTRUCTION (
    FEE_INSTRUCTION_ID,
    INSTRUCTION_NO,
    IDEMPOTENCY_KEY,
    REQUEST_ID,
    CORRELATION_ID,
    FEE_ARRANGEMENT_ID,
    FEE_DEFINITION_VERSION_ID,
    DEMO_PARTY_ID,
    DEMO_ACCOUNT_ID,
    DEMO_PRODUCT_ID,
    BUSINESS_TRANSACTION_REF,
    EVENT_TYPE_CODE,
    ACTIVITY_CODE,
    TRANSACTION_TYPE_CODE,
    CHANNEL_CODE,
    BASIS_AMOUNT,
    BASIS_CURRENCY_CODE,
    QUANTITY_VALUE,
    EVENT_DATE_TIME,
    STATUS_CODE,
    REQUESTED_AT,
    CREATED_AT,
    CREATED_BY,
    RECORD_VERSION
) VALUES (
    -12001,
    'FINSTR-0001',
    'FEE-SEED-TRANSFER-0001',
    'REQ-0001',
    'CORR-0001',
    -11001,
    -7101,
    -2004,
    -3202,
    -3001,
    'BUS-TX-0001',
    'TRANSFER',
    'MONEY_TRANSFER',
    'TRANSFER',
    'MOBILE',
    500000000,
    'IRR',
    1,
    TIMESTAMP '2026-08-29 10:00:00',
    'COMPLETED',
    TIMESTAMP '2026-08-29 10:00:00',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0',
    1
);

PROMPT Loading FEE_TRANSACTION ...
INSERT INTO FEE_TRANSACTION (
    FEE_TRANSACTION_ID,
    TRANSACTION_NO,
    FEE_INSTRUCTION_ID,
    FEE_DEFINITION_VERSION_ID,
    FEE_ARRANGEMENT_ID,
    CALCULATION_RULE_ID,
    TRANSACTION_DATE,
    CALCULATED_AMOUNT,
    GROSS_FEE_AMOUNT,
    ACCRUED_FEE_AMOUNT,
    APPLIED_FEE_AMOUNT,
    ADJUSTMENT_AMOUNT,
    NET_FEE_AMOUNT,
    COLLECTED_AMOUNT,
    FEE_CURRENCY_CODE,
    STATUS_CODE,
    CALCULATED_AT,
    APPLIED_AT,
    COLLECTED_AT,
    CREATED_AT,
    CREATED_BY
) VALUES (
    -12101,
    'FTX-0001',
    -12001,
    -7101,
    -11001,
    -9001,
    DATE '2026-08-29',
    750000,
    750000,
    750000,
    600000,
    150000,
    600000,
    600000,
    'IRR',
    'PARTIALLY_REVERSED',
    TIMESTAMP '2026-08-29 10:00:01',
    TIMESTAMP '2026-08-29 10:00:02',
    TIMESTAMP '2026-08-29 10:00:03',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0'
);

PROMPT Loading FEE_TRANSACTION_COMPONENT ...
INSERT INTO FEE_TRANSACTION_COMPONENT (
    TRANSACTION_COMPONENT_ID,
    FEE_TRANSACTION_ID,
    CHARGE_COMPONENT_ID,
    SEQUENCE_NO,
    COMPONENT_CODE,
    COMPONENT_TYPE_CODE,
    CALCULATED_AMOUNT,
    APPLIED_AMOUNT,
    DISCOUNT_AMOUNT,
    REFUNDABLE_AMOUNT,
    REVENUE_AMOUNT,
    PASS_THROUGH_AMOUNT,
    CURRENCY_CODE,
    DESCRIPTION
) VALUES (
    -12201,
    -12101,
    -9401,
    1,
    'BANK_FEE',
    'BANK_REVENUE',
    750000,
    600000,
    150000,
    600000,
    600000,
    0,
    'IRR',
    'جزء درآمدی کارمزد انتقال'
);

PROMPT Loading FEE_CALCULATION_SNAPSHOT ...
INSERT INTO FEE_CALCULATION_SNAPSHOT (
    CALCULATION_SNAPSHOT_ID,
    FEE_TRANSACTION_ID,
    POLICY_VERSION_ID,
    FEE_DEFINITION_VERSION_ID,
    CALCULATION_RULE_ID,
    APPLICABILITY_RULE_ID,
    CONFIG_HASH,
    INPUT_CONTEXT_HASH,
    INPUT_CONTEXT_JSON,
    SNAPSHOT_JSON,
    EXPLANATION_TEXT,
    BASIS_AMOUNT,
    GROSS_FEE_AMOUNT,
    TOTAL_ADJUSTMENT_AMOUNT,
    NET_FEE_AMOUNT,
    CALCULATED_AT
) VALUES (
    -12301,
    -12101,
    -5102,
    -7101,
    -9001,
    -8001,
    'SEED-DEMO_TRANSFER_FEE-V1',
    'SEED-CTX-TRANSFER-0001',
    TO_CLOB('{"basisAmount":500000000,"currency":"IRR","segment":"VIP","channel":"MOBILE"}'),
    TO_CLOB('{"arrangementRate":0.0015,"gross":750000,"discountRate":0.20,"discount":150000,"net":600000}'),
    TO_CLOB('مبلغ مبنا ۵۰۰,۰۰۰,۰۰۰ ریال × نرخ توافقی ۰.۱۵٪ = ۷۵۰,۰۰۰ ریال؛ سپس تخفیف VIP به میزان ۲۰٪ اعمال و مبلغ نهایی ۶۰۰,۰۰۰ ریال محاسبه شد.'),
    500000000,
    750000,
    150000,
    600000,
    TIMESTAMP '2026-08-29 10:00:01'
);

PROMPT Loading FEE_APPLIED_ADJUSTMENT ...
INSERT INTO FEE_APPLIED_ADJUSTMENT (
    APPLIED_ADJUSTMENT_ID,
    FEE_TRANSACTION_ID,
    ADJUSTMENT_POLICY_ID,
    SEQUENCE_NO,
    ADJUSTMENT_TYPE_CODE,
    BASE_AMOUNT,
    PERCENT_VALUE,
    ADJUSTMENT_AMOUNT,
    RESULT_AMOUNT,
    CURRENCY_CODE,
    APPLIED_AT
) VALUES (
    -12401,
    -12101,
    -10001,
    1,
    'PERCENT_REDUCTION',
    750000,
    0.2,
    150000,
    600000,
    'IRR',
    TIMESTAMP '2026-08-29 10:00:02'
);

PROMPT Loading FEE_ALLOCATION_RESULT ...
INSERT INTO FEE_ALLOCATION_RESULT (
    ALLOCATION_RESULT_ID,
    FEE_TRANSACTION_ID,
    ALLOCATION_POLICY_ID,
    ALLOCATION_RULE_ID,
    BENEFICIARY_PARTY_ID,
    BENEFICIARY_ROLE_CODE,
    ALLOCATION_BASIS_CODE,
    ALLOCATION_PERCENT,
    ALLOCATION_AMOUNT,
    CURRENCY_CODE,
    CALCULATED_AT,
    STATUS_CODE
) VALUES (
    -12501,
    -12101,
    -10101,
    -10201,
    -2001,
    'BANK',
    'PERCENT',
    0.7,
    420000,
    'IRR',
    TIMESTAMP '2026-08-29 10:00:02',
    'CALCULATED'
);

INSERT INTO FEE_ALLOCATION_RESULT (
    ALLOCATION_RESULT_ID,
    FEE_TRANSACTION_ID,
    ALLOCATION_POLICY_ID,
    ALLOCATION_RULE_ID,
    BENEFICIARY_PARTY_ID,
    BENEFICIARY_ROLE_CODE,
    ALLOCATION_BASIS_CODE,
    ALLOCATION_PERCENT,
    ALLOCATION_AMOUNT,
    CURRENCY_CODE,
    CALCULATED_AT,
    STATUS_CODE
) VALUES (
    -12502,
    -12101,
    -10101,
    -10202,
    -2002,
    'AGENT_BRANCH',
    'PERCENT',
    0.2,
    120000,
    'IRR',
    TIMESTAMP '2026-08-29 10:00:02',
    'CALCULATED'
);

INSERT INTO FEE_ALLOCATION_RESULT (
    ALLOCATION_RESULT_ID,
    FEE_TRANSACTION_ID,
    ALLOCATION_POLICY_ID,
    ALLOCATION_RULE_ID,
    BENEFICIARY_PARTY_ID,
    BENEFICIARY_ROLE_CODE,
    ALLOCATION_BASIS_CODE,
    ALLOCATION_PERCENT,
    ALLOCATION_AMOUNT,
    CURRENCY_CODE,
    CALCULATED_AT,
    STATUS_CODE
) VALUES (
    -12503,
    -12101,
    -10101,
    -10203,
    -2007,
    'EXTERNAL_ORG',
    'PERCENT',
    0.1,
    60000,
    'IRR',
    TIMESTAMP '2026-08-29 10:00:02',
    'CALCULATED'
);

PROMPT Loading FEE_OVERRIDE_REQUEST ...
INSERT INTO FEE_OVERRIDE_REQUEST (
    OVERRIDE_REQUEST_ID,
    FEE_ARRANGEMENT_ID,
    FEE_TRANSACTION_ID,
    FEE_DEFINITION_VERSION_ID,
    OVERRIDE_TYPE_CODE,
    OVERRIDE_AMOUNT,
    REASON_CODE,
    REASON_TEXT,
    REQUESTED_BY,
    REQUESTED_AT,
    STATUS_CODE,
    APPROVED_BY,
    APPROVED_AT,
    RECORD_VERSION
) VALUES (
    -12601,
    -11001,
    -12101,
    -7101,
    'AMOUNT_OVERRIDE',
    500000,
    'MANUAL_EXCEPTION',
    'درخواست نمونه برای آزمون گردش کار Override',
    'DEMO_USER',
    TIMESTAMP '2026-08-29 10:05:00',
    'REJECTED',
    'DEMO_SUPERVISOR',
    TIMESTAMP '2026-08-29 10:06:00',
    1
);

PROMPT Loading FEE_REVERSAL ...
INSERT INTO FEE_REVERSAL (
    FEE_REVERSAL_ID,
    REVERSAL_NO,
    FEE_TRANSACTION_ID,
    REVERSAL_TYPE_CODE,
    REVERSAL_CALC_RULE_ID,
    REQUESTED_AMOUNT,
    CALCULATED_REVERSAL_AMOUNT,
    NON_REFUNDABLE_AMOUNT,
    REVERSAL_CURRENCY_CODE,
    REASON_CODE,
    REASON_TEXT,
    AUTHORIZATION_REF,
    REFUND_ACCOUNT_ID,
    STATUS_CODE,
    REQUESTED_AT,
    COMPLETED_AT,
    CREATED_AT,
    CREATED_BY
) VALUES (
    -12701,
    'FREV-0001',
    -12101,
    'PARTIAL',
    NULL,
    100000,
    100000,
    0,
    'IRR',
    'CUSTOMER_REQUEST',
    'برگشت جزئی نمونه',
    'AUTH-REV-0001',
    -3202,
    'COMPLETED',
    TIMESTAMP '2026-08-29 10:10:00',
    TIMESTAMP '2026-08-29 10:11:00',
    TIMESTAMP '2026-01-01 00:00:00',
    'SEED_BASELINE_1_0'
);

PROMPT Loading FEE_POSTING_REFERENCE ...
INSERT INTO FEE_POSTING_REFERENCE (
    POSTING_REFERENCE_ID,
    FEE_TRANSACTION_ID,
    POSTING_PURPOSE_CODE,
    POSTING_MODALITY_CODE,
    ACCOUNTING_PROFILE_CODE,
    DEBIT_DEMO_ACCOUNT_ID,
    CREDIT_DEMO_ACCOUNT_ID,
    AMOUNT,
    CURRENCY_CODE,
    ACCOUNTING_INSTRUCTION_REF,
    POSTING_REF,
    JOURNAL_REF,
    LEDGER_TRANSACTION_REF,
    POSTING_STATUS_CODE,
    REQUESTED_AT,
    COMPLETED_AT,
    CREATED_AT
) VALUES (
    -12801,
    -12101,
    'CHARGE',
    'Immediate Posting',
    'FEE_DEFAULT',
    -3202,
    -3204,
    600000,
    'IRR',
    'ACC-I-0001',
    'POST-0001',
    'JRN-0001',
    'LEDGER-DEMO-0001',
    'COMPLETED',
    TIMESTAMP '2026-08-29 10:00:03',
    TIMESTAMP '2026-08-29 10:00:04',
    TIMESTAMP '2026-01-01 00:00:00'
);

INSERT INTO FEE_POSTING_REFERENCE (
    POSTING_REFERENCE_ID,
    FEE_TRANSACTION_ID,
    FEE_REVERSAL_ID,
    POSTING_PURPOSE_CODE,
    POSTING_MODALITY_CODE,
    ACCOUNTING_PROFILE_CODE,
    DEBIT_DEMO_ACCOUNT_ID,
    CREDIT_DEMO_ACCOUNT_ID,
    AMOUNT,
    CURRENCY_CODE,
    ACCOUNTING_INSTRUCTION_REF,
    POSTING_REF,
    JOURNAL_REF,
    LEDGER_TRANSACTION_REF,
    POSTING_STATUS_CODE,
    REQUESTED_AT,
    COMPLETED_AT,
    CREATED_AT
) VALUES (
    -12802,
    -12101,
    -12701,
    'REVERSAL',
    'Immediate Posting',
    'FEE_REVERSAL',
    -3204,
    -3202,
    100000,
    'IRR',
    'ACC-I-0002',
    'POST-0002',
    'JRN-0002',
    'LEDGER-DEMO-0002',
    'COMPLETED',
    TIMESTAMP '2026-08-29 10:10:30',
    TIMESTAMP '2026-08-29 10:11:00',
    TIMESTAMP '2026-01-01 00:00:00'
);

PROMPT Loading FEE_SETTLEMENT_REFERENCE ...
INSERT INTO FEE_SETTLEMENT_REFERENCE (
    SETTLEMENT_REFERENCE_ID,
    FEE_TRANSACTION_ID,
    ALLOCATION_RESULT_ID,
    SETTLEMENT_METHOD_CODE,
    SETTLEMENT_STATUS_CODE,
    SETTLEMENT_DATE,
    SETTLEMENT_ACCOUNT_ID,
    SETTLEMENT_AMOUNT,
    CURRENCY_CODE,
    SETTLEMENT_REFERENCE,
    CREATED_AT
) VALUES (
    -12901,
    -12101,
    -12501,
    'INTERNAL',
    'COMPLETED',
    DATE '2026-08-29',
    -3205,
    420000,
    'IRR',
    'SET-BANK-0001',
    TIMESTAMP '2026-01-01 00:00:00'
);

INSERT INTO FEE_SETTLEMENT_REFERENCE (
    SETTLEMENT_REFERENCE_ID,
    FEE_TRANSACTION_ID,
    ALLOCATION_RESULT_ID,
    SETTLEMENT_METHOD_CODE,
    SETTLEMENT_STATUS_CODE,
    SETTLEMENT_DATE,
    SETTLEMENT_ACCOUNT_ID,
    SETTLEMENT_AMOUNT,
    CURRENCY_CODE,
    SETTLEMENT_REFERENCE,
    CREATED_AT
) VALUES (
    -12902,
    -12101,
    -12502,
    'INTERNAL',
    'COMPLETED',
    DATE '2026-08-29',
    -3205,
    120000,
    'IRR',
    'SET-BR-0001',
    TIMESTAMP '2026-01-01 00:00:00'
);

INSERT INTO FEE_SETTLEMENT_REFERENCE (
    SETTLEMENT_REFERENCE_ID,
    FEE_TRANSACTION_ID,
    ALLOCATION_RESULT_ID,
    SETTLEMENT_METHOD_CODE,
    SETTLEMENT_STATUS_CODE,
    SETTLEMENT_DATE,
    SETTLEMENT_ACCOUNT_ID,
    SETTLEMENT_AMOUNT,
    CURRENCY_CODE,
    SETTLEMENT_REFERENCE,
    CREATED_AT
) VALUES (
    -12903,
    -12101,
    -12503,
    'TRANSFER',
    'COMPLETED',
    DATE '2026-08-29',
    -3206,
    60000,
    'IRR',
    'SET-EXT-0001',
    TIMESTAMP '2026-01-01 00:00:00'
);

PROMPT Loading FEE_DECISION_TRACE ...
INSERT INTO FEE_DECISION_TRACE (
    TRACE_ID,
    FEE_TRANSACTION_ID,
    STEP_NO,
    STEP_NAME,
    STEP_TYPE_CODE,
    CALCULATION_RULE_ID,
    APPLICABILITY_RULE_ID,
    RULE_COMPONENT_ID,
    INPUT_PAYLOAD,
    OUTPUT_PAYLOAD,
    EVALUATION_RESULT_CODE,
    CREATED_AT
) VALUES (
    -13001,
    -12101,
    1,
    'Resolve applicability',
    'APPLICABILITY',
    NULL,
    -8001,
    NULL,
    TO_CLOB('{"activity":"MONEY_TRANSFER","currency":"IRR"}'),
    TO_CLOB('{"matched":true}'),
    'MATCHED',
    TIMESTAMP '2026-08-29 10:00:01'
);

INSERT INTO FEE_DECISION_TRACE (
    TRACE_ID,
    FEE_TRANSACTION_ID,
    STEP_NO,
    STEP_NAME,
    STEP_TYPE_CODE,
    CALCULATION_RULE_ID,
    APPLICABILITY_RULE_ID,
    RULE_COMPONENT_ID,
    INPUT_PAYLOAD,
    OUTPUT_PAYLOAD,
    EVALUATION_RESULT_CODE,
    CREATED_AT
) VALUES (
    -13002,
    -12101,
    2,
    'Resolve arrangement rate',
    'ARRANGEMENT',
    -9001,
    NULL,
    NULL,
    TO_CLOB('{"standardRate":0.002,"arrangementRate":0.0015}'),
    TO_CLOB('{"selectedRate":0.0015}'),
    'OVERRIDDEN_BY_ARRANGEMENT',
    TIMESTAMP '2026-08-29 10:00:01'
);

INSERT INTO FEE_DECISION_TRACE (
    TRACE_ID,
    FEE_TRANSACTION_ID,
    STEP_NO,
    STEP_NAME,
    STEP_TYPE_CODE,
    CALCULATION_RULE_ID,
    APPLICABILITY_RULE_ID,
    RULE_COMPONENT_ID,
    INPUT_PAYLOAD,
    OUTPUT_PAYLOAD,
    EVALUATION_RESULT_CODE,
    CREATED_AT
) VALUES (
    -13003,
    -12101,
    3,
    'Calculate gross fee',
    'CALCULATION',
    -9001,
    -8001,
    NULL,
    TO_CLOB('{"basis":500000000,"rate":0.0015}'),
    TO_CLOB('{"gross":750000}'),
    'CALCULATED',
    TIMESTAMP '2026-08-29 10:00:01'
);

INSERT INTO FEE_DECISION_TRACE (
    TRACE_ID,
    FEE_TRANSACTION_ID,
    STEP_NO,
    STEP_NAME,
    STEP_TYPE_CODE,
    CALCULATION_RULE_ID,
    APPLICABILITY_RULE_ID,
    RULE_COMPONENT_ID,
    INPUT_PAYLOAD,
    OUTPUT_PAYLOAD,
    EVALUATION_RESULT_CODE,
    CREATED_AT
) VALUES (
    -13004,
    -12101,
    4,
    'Apply VIP discount',
    'ADJUSTMENT',
    NULL,
    -8002,
    NULL,
    TO_CLOB('{"gross":750000,"discountRate":0.20}'),
    TO_CLOB('{"net":600000}'),
    'APPLIED',
    TIMESTAMP '2026-08-29 10:00:01'
);

PROMPT Loading FEE_AUDIT_EVIDENCE ...
INSERT INTO FEE_AUDIT_EVIDENCE (
    AUDIT_EVIDENCE_ID,
    FEE_TRANSACTION_ID,
    EVIDENCE_TYPE_CODE,
    ENTITY_TYPE_CODE,
    ENTITY_ID,
    EVIDENCE_REF,
    EVIDENCE_HASH,
    EVIDENCE_PAYLOAD,
    CAPTURED_AT,
    CAPTURED_BY
) VALUES (
    -13101,
    -12101,
    'RULE',
    'FEE_TRANSACTION',
    'FTX-0001',
    'seed://calculation/FTX-0001',
    'HASH-SEED-FTX-0001',
    TO_CLOB('{"policyVersion":"1.0","feeDefinition":"DEMO_TRANSFER_FEE"}'),
    TIMESTAMP '2026-08-29 10:00:04',
    'FEE_ENGINE'
);

INSERT INTO FEE_AUDIT_EVIDENCE (
    AUDIT_EVIDENCE_ID,
    OVERRIDE_REQUEST_ID,
    EVIDENCE_TYPE_CODE,
    ENTITY_TYPE_CODE,
    ENTITY_ID,
    EVIDENCE_REF,
    CAPTURED_AT,
    CAPTURED_BY
) VALUES (
    -13102,
    -12601,
    'OVERRIDE',
    'FEE_OVERRIDE_REQUEST',
    '-12601',
    'seed://override/-12601',
    TIMESTAMP '2026-08-29 10:06:00',
    'DEMO_SUPERVISOR'
);

INSERT INTO FEE_AUDIT_EVIDENCE (
    AUDIT_EVIDENCE_ID,
    FEE_REVERSAL_ID,
    EVIDENCE_TYPE_CODE,
    ENTITY_TYPE_CODE,
    ENTITY_ID,
    EVIDENCE_REF,
    CAPTURED_AT,
    CAPTURED_BY
) VALUES (
    -13103,
    -12701,
    'REVERSAL',
    'FEE_REVERSAL',
    'FREV-0001',
    'seed://reversal/FREV-0001',
    TIMESTAMP '2026-08-29 10:11:00',
    'FEE_ENGINE'
);

COMMIT;

