-- CAL2 0.11.0 — International observance provenance baseline
-- Date: 2026-10-11
-- Idempotent migration for the currently present 33 UN-system exact/alias observances.
-- This migration DOES NOT insert the 235 missing observance definitions; that is a separate coverage seed.
-- Run as CAL2 or a privileged account.
SET SERVEROUTPUT ON
SET DEFINE OFF
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

DECLARE
  v_count NUMBER;
BEGIN
  SELECT COUNT(*) INTO v_count
    FROM ALL_TAB_COLUMNS
   WHERE OWNER='CAL2' AND TABLE_NAME='EVENT' AND COLUMN_NAME='SOURCE_ID';
  IF v_count = 0 THEN
    EXECUTE IMMEDIATE 'ALTER TABLE CAL2.EVENT ADD (SOURCE_ID NUMBER(19))';
    DBMS_OUTPUT.PUT_LINE('Added CAL2.EVENT.SOURCE_ID');
  ELSE
    DBMS_OUTPUT.PUT_LINE('CAL2.EVENT.SOURCE_ID already exists');
  END IF;

  SELECT COUNT(*) INTO v_count
    FROM ALL_CONSTRAINTS
   WHERE OWNER='CAL2' AND CONSTRAINT_NAME='FK_CAL2_EVENT_SOURCE';
  IF v_count = 0 THEN
    EXECUTE IMMEDIATE 'ALTER TABLE CAL2.EVENT ADD CONSTRAINT FK_CAL2_EVENT_SOURCE FOREIGN KEY (SOURCE_ID) REFERENCES CAL2.SOURCE_AUTHORITY (SOURCE_ID)';
    DBMS_OUTPUT.PUT_LINE('Added FK_CAL2_EVENT_SOURCE');
  END IF;

  SELECT COUNT(*) INTO v_count
    FROM ALL_INDEXES
   WHERE OWNER='CAL2' AND INDEX_NAME='IX_CAL2_EVENT_SOURCE';
  IF v_count = 0 THEN
    EXECUTE IMMEDIATE 'CREATE INDEX CAL2.IX_CAL2_EVENT_SOURCE ON CAL2.EVENT (SOURCE_ID)';
    DBMS_OUTPUT.PUT_LINE('Created IX_CAL2_EVENT_SOURCE');
  END IF;
END;
/

COMMENT ON COLUMN CAL2.EVENT.SOURCE_ID IS 'مرجع رسمی یا داده‌ای تعریف/اعلام مناسبت';

DECLARE
  PROCEDURE ensure_source(
      p_code VARCHAR2,
      p_name_fa VARCHAR2,
      p_name_en VARCHAR2,
      p_uri VARCHAR2
  ) IS
    v_id NUMBER;
  BEGIN
    BEGIN
      SELECT SOURCE_ID INTO v_id
        FROM CAL2.SOURCE_AUTHORITY
       WHERE SOURCE_CODE = p_code;
      UPDATE CAL2.SOURCE_AUTHORITY
         SET NAME_FA = p_name_fa,
             NAME_EN = p_name_en,
             SOURCE_TYPE = 'INTERNATIONAL_ORGANIZATION',
             SOURCE_URI = p_uri,
             COUNTRY_CODE = NULL,
             AUTHORITY_LEVEL = 'OFFICIAL',
             ACTIVE_FLAG = 'Y'
       WHERE SOURCE_ID = v_id;
      DBMS_OUTPUT.PUT_LINE('Updated source authority: ' || p_code);
    EXCEPTION
      WHEN NO_DATA_FOUND THEN
        SELECT NVL(MAX(SOURCE_ID),0)+1 INTO v_id FROM CAL2.SOURCE_AUTHORITY;
        INSERT INTO CAL2.SOURCE_AUTHORITY(
          SOURCE_ID,SOURCE_CODE,NAME_FA,NAME_EN,SOURCE_TYPE,SOURCE_URI,COUNTRY_CODE,AUTHORITY_LEVEL,ACTIVE_FLAG
        ) VALUES (
          v_id,p_code,p_name_fa,p_name_en,'INTERNATIONAL_ORGANIZATION',p_uri,NULL,'OFFICIAL','Y'
        );
        DBMS_OUTPUT.PUT_LINE('Inserted source authority: ' || p_code);
    END;
  END;
BEGIN
  LOCK TABLE CAL2.SOURCE_AUTHORITY IN SHARE ROW EXCLUSIVE MODE;
  ensure_source('UNITED_NATIONS','سازمان ملل متحد','United Nations',
                'https://www.un.org/en/observances/list-days-weeks');
  ensure_source('UNESCO','سازمان آموزشی، علمی و فرهنگی ملل متحد (یونسکو)','UNESCO',
                'https://www.unesco.org/en/days');
  ensure_source('WHO','سازمان جهانی بهداشت','World Health Organization',
                'https://www.who.int/campaigns');
END;
/

DECLARE
  v_mapped_events NUMBER := 0;
  v_mapped_rules  NUMBER := 0;

  PROCEDURE map_official_event(p_event_code VARCHAR2, p_source_code VARCHAR2) IS
    v_event_id NUMBER;
    v_source_id NUMBER;
    v_rules NUMBER;
  BEGIN
    BEGIN
      SELECT EVENT_ID INTO v_event_id
        FROM CAL2.EVENT
       WHERE EVENT_CODE = p_event_code;
    EXCEPTION
      WHEN NO_DATA_FOUND THEN
        RAISE_APPLICATION_ERROR(-20041, 'Expected CAL2.EVENT missing: ' || p_event_code);
    END;

    SELECT SOURCE_ID INTO v_source_id
      FROM CAL2.SOURCE_AUTHORITY
     WHERE SOURCE_CODE = p_source_code
       AND ACTIVE_FLAG = 'Y';

    UPDATE CAL2.EVENT
       SET OFFICIAL_FLAG = 'Y',
           SOURCE_ID = v_source_id
     WHERE EVENT_ID = v_event_id;

    UPDATE CAL2.EVENT_RECURRENCE_RULE
       SET SOURCE_ID = v_source_id
     WHERE EVENT_ID = v_event_id
       AND ACTIVE_FLAG = 'Y';
    v_rules := SQL%ROWCOUNT;

    v_mapped_events := v_mapped_events + 1;
    v_mapped_rules := v_mapped_rules + v_rules;
    DBMS_OUTPUT.PUT_LINE('Mapped ' || p_event_code || ' -> ' || p_source_code || ' (active rules=' || v_rules || ')');
  END;
BEGIN
  -- UNESCO-specific definitions in the current runtime set.
  map_official_event('INTL_PI_DAY', 'UNESCO');
  map_official_event('INTL_SCIENCE_PEACE_DEVELOPMENT_DAY', 'UNESCO');
  map_official_event('INTL_TEACHERS_DAY', 'UNESCO');

  -- WHO Member-State mandated global public-health days already present in CAL2.
  map_official_event('INTL_WORLD_HEALTH_DAY', 'WHO');
  map_official_event('INTL_AIDS_DAY', 'WHO');
  map_official_event('INTL_BLOOD_DONOR_DAY', 'WHO');
  map_official_event('INTL_NO_TOBACCO_DAY', 'WHO');

  -- Remaining current definitions verified against the United Nations observance list.
  map_official_event('INTL_EARTH_DAY', 'UNITED_NATIONS');
  map_official_event('INTL_HUMAN_RIGHTS_DAY', 'UNITED_NATIONS');
  map_official_event('INTL_CIVIL_AVIATION_DAY', 'UNITED_NATIONS');
  map_official_event('INTL_DRUG_ABUSE_DAY', 'UNITED_NATIONS');
  map_official_event('INTL_VIOLENCE_AGAINST_WOMEN_DAY', 'UNITED_NATIONS');
  map_official_event('INTL_POVERTY_ERADICATION_DAY', 'UNITED_NATIONS');
  map_official_event('INTL_HAPPINESS_DAY', 'UNITED_NATIONS');
  map_official_event('INTL_NOWRUZ_DAY', 'UNITED_NATIONS');
  map_official_event('INTL_OLDER_PERSONS_DAY', 'UNITED_NATIONS');
  map_official_event('INTL_PEACE_DAY', 'UNITED_NATIONS');
  map_official_event('INTL_PERSONS_WITH_DISABILITIES_DAY', 'UNITED_NATIONS');
  map_official_event('INTL_GIRL_DAY', 'UNITED_NATIONS');
  map_official_event('INTL_MOUNTAIN_DAY', 'UNITED_NATIONS');
  map_official_event('INTL_TRANSLATION_DAY', 'UNITED_NATIONS');
  map_official_event('INTL_INTERNATIONAL_WOMENS_DAY', 'UNITED_NATIONS');
  map_official_event('INTL_CHILDRENS_DAY', 'UNITED_NATIONS');
  map_official_event('INTL_CHILD_LABOUR_DAY', 'UNITED_NATIONS');
  map_official_event('INTL_DESERTIFICATION_DAY', 'UNITED_NATIONS');
  map_official_event('INTL_DIABETES_DAY', 'UNITED_NATIONS');
  map_official_event('INTL_ENVIRONMENT_DAY', 'UNITED_NATIONS');
  map_official_event('INTL_FOOD_DAY', 'UNITED_NATIONS');
  map_official_event('INTL_WORLD_MARITIME_DAY', 'UNITED_NATIONS');
  map_official_event('INTL_WORLD_METEOROLOGICAL_DAY', 'UNITED_NATIONS');
  map_official_event('INTL_POST_DAY', 'UNITED_NATIONS');
  map_official_event('INTL_TOURISM_DAY', 'UNITED_NATIONS');
  map_official_event('INTL_WORLD_WATER_DAY', 'UNITED_NATIONS');

  IF v_mapped_events <> 33 THEN
    RAISE_APPLICATION_ERROR(-20042, 'Expected 33 current official/alias international event mappings, got ' || v_mapped_events);
  END IF;

  DBMS_OUTPUT.PUT_LINE('Mapped official international event definitions: ' || v_mapped_events);
  DBMS_OUTPUT.PUT_LINE('Updated active recurrence-rule provenance rows: ' || v_mapped_rules);
END;
/

DECLARE
  v_event_count NUMBER;
  v_source_count NUMBER;
  v_unmapped_official NUMBER;
BEGIN
  SELECT COUNT(*) INTO v_source_count
    FROM CAL2.SOURCE_AUTHORITY
   WHERE SOURCE_CODE IN ('UNITED_NATIONS','UNESCO','WHO')
     AND AUTHORITY_LEVEL='OFFICIAL'
     AND ACTIVE_FLAG='Y';

  SELECT COUNT(*) INTO v_event_count
    FROM CAL2.EVENT E
    JOIN CAL2.SOURCE_AUTHORITY S ON S.SOURCE_ID=E.SOURCE_ID
   WHERE E.EVENT_CODE IN (
     'INTL_PI_DAY','INTL_SCIENCE_PEACE_DEVELOPMENT_DAY','INTL_TEACHERS_DAY',
     'INTL_WORLD_HEALTH_DAY','INTL_AIDS_DAY','INTL_BLOOD_DONOR_DAY','INTL_NO_TOBACCO_DAY',
     'INTL_EARTH_DAY','INTL_HUMAN_RIGHTS_DAY','INTL_CIVIL_AVIATION_DAY','INTL_DRUG_ABUSE_DAY',
     'INTL_VIOLENCE_AGAINST_WOMEN_DAY','INTL_POVERTY_ERADICATION_DAY','INTL_HAPPINESS_DAY','INTL_NOWRUZ_DAY',
     'INTL_OLDER_PERSONS_DAY','INTL_PEACE_DAY','INTL_PERSONS_WITH_DISABILITIES_DAY','INTL_GIRL_DAY',
     'INTL_MOUNTAIN_DAY','INTL_TRANSLATION_DAY','INTL_INTERNATIONAL_WOMENS_DAY','INTL_CHILDRENS_DAY',
     'INTL_CHILD_LABOUR_DAY','INTL_DESERTIFICATION_DAY','INTL_DIABETES_DAY','INTL_ENVIRONMENT_DAY','INTL_FOOD_DAY',
     'INTL_WORLD_MARITIME_DAY','INTL_WORLD_METEOROLOGICAL_DAY','INTL_POST_DAY','INTL_TOURISM_DAY','INTL_WORLD_WATER_DAY'
   )
     AND E.OFFICIAL_FLAG='Y'
     AND S.SOURCE_CODE IN ('UNITED_NATIONS','UNESCO','WHO');

  SELECT COUNT(*) INTO v_unmapped_official
    FROM CAL2.EVENT E
   WHERE E.EVENT_CODE IN (
     'INTL_PI_DAY','INTL_SCIENCE_PEACE_DEVELOPMENT_DAY','INTL_TEACHERS_DAY',
     'INTL_WORLD_HEALTH_DAY','INTL_AIDS_DAY','INTL_BLOOD_DONOR_DAY','INTL_NO_TOBACCO_DAY',
     'INTL_EARTH_DAY','INTL_HUMAN_RIGHTS_DAY','INTL_CIVIL_AVIATION_DAY','INTL_DRUG_ABUSE_DAY',
     'INTL_VIOLENCE_AGAINST_WOMEN_DAY','INTL_POVERTY_ERADICATION_DAY','INTL_HAPPINESS_DAY','INTL_NOWRUZ_DAY',
     'INTL_OLDER_PERSONS_DAY','INTL_PEACE_DAY','INTL_PERSONS_WITH_DISABILITIES_DAY','INTL_GIRL_DAY',
     'INTL_MOUNTAIN_DAY','INTL_TRANSLATION_DAY','INTL_INTERNATIONAL_WOMENS_DAY','INTL_CHILDRENS_DAY',
     'INTL_CHILD_LABOUR_DAY','INTL_DESERTIFICATION_DAY','INTL_DIABETES_DAY','INTL_ENVIRONMENT_DAY','INTL_FOOD_DAY',
     'INTL_WORLD_MARITIME_DAY','INTL_WORLD_METEOROLOGICAL_DAY','INTL_POST_DAY','INTL_TOURISM_DAY','INTL_WORLD_WATER_DAY'
   )
     AND (E.OFFICIAL_FLAG <> 'Y' OR E.SOURCE_ID IS NULL);

  IF v_source_count <> 3 OR v_event_count <> 33 OR v_unmapped_official <> 0 THEN
    RAISE_APPLICATION_ERROR(-20043,
      'International observance provenance verification failed. sources='||v_source_count||
      ', mappedEvents='||v_event_count||', unmapped='||v_unmapped_official);
  END IF;

  DBMS_OUTPUT.PUT_LINE('CAL2_INTL_OBSERVANCE_PROVENANCE_PASS sources=3 mappedEvents=33');
END;
/

COMMIT;
PROMPT CAL2 international observance provenance migration completed.
