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
