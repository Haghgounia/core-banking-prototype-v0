SET DEFINE OFF
-- Register the Iranian national variant placeholder only.
-- No national activity rows are fabricated by this script.
MERGE INTO CIF.REF_ISIC_RELEASE T
USING (SELECT 'ISIC' CLASSIFICATION_CODE, '4' REVISION_CODE, 'IR-SCI' VARIANT_CODE, 'IR' COUNTRY_CODE FROM DUAL) S
ON (T.CLASSIFICATION_CODE=S.CLASSIFICATION_CODE AND T.REVISION_CODE=S.REVISION_CODE AND T.VARIANT_CODE=S.VARIANT_CODE AND NVL(T.COUNTRY_CODE,'~')=NVL(S.COUNTRY_CODE,'~'))
WHEN MATCHED THEN UPDATE SET
    T.NAME_FA='طبقه‌بندی فعالیت‌های اقتصادی ایران بر پایه ISIC Rev.4',
    T.NAME_EN='Iranian economic activity classification based on ISIC Rev.4',
    T.SOURCE_AUTHORITY='Statistical Centre of Iran',
    T.DATASET_STATUS_CODE='DRAFT', T.IS_CURRENT=0, T.IS_ACTIVE=0,
    T.LAST_MODIFIED_BY='SEED', T.LAST_MODIFIED_DATE=SYSTIMESTAMP
WHEN NOT MATCHED THEN INSERT
    (CLASSIFICATION_CODE,REVISION_CODE,VARIANT_CODE,COUNTRY_CODE,NAME_FA,NAME_EN,SOURCE_AUTHORITY,
     DATASET_STATUS_CODE,IS_CURRENT,IS_ACTIVE,RECORD_VERSION,CREATED_BY)
VALUES
    ('ISIC','4','IR-SCI','IR','طبقه‌بندی فعالیت‌های اقتصادی ایران بر پایه ISIC Rev.4',
     'Iranian economic activity classification based on ISIC Rev.4','Statistical Centre of Iran',
     'DRAFT',0,0,1,'SEED');
COMMIT;
PROMPT IR-SCI release placeholder registered as DRAFT/Inactive. No IR-SCI activity data inserted.
