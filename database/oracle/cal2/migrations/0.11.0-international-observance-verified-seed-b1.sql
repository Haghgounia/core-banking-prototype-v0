-- CAL2 0.11.0 / Phase B1 - authoritative, bounded International Observance seed.
-- 72 official-directory listings (63 UNESCO-listed, 9 WHO); 24 UNESCO-listed
-- UNGA observances use UNITED_NATIONS as declaring authority; plus variable rule for
-- existing INTL_WORLD_MARITIME_DAY (UNITED_NATIONS provenance from Phase A).
-- EXPLICITLY NOT THE 235-definition full Working Master. Do not report 235 as seeded.
-- WHO source is WHO mandates/campaigns. Source decisions for overlapping UN/UNESCO
-- observances are best-effort and need full Working Master crosswalk review.
-- Precondition: 0.11.0-international-observance-provenance.sql has completed,
--               0.11.0-international-variable-rule-engine.sql has completed.
-- Calendar month views read EVENT_OCCURRENCE: this seed materializes occurrences.
-- Re-running is idempotent for all 72 definitions and their Gregorian rules.
SET SERVEROUTPUT ON
SET DEFINE OFF
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK
PROMPT === CAL2 Phase B1 / SOURCE-VERIFIED INTERNATIONAL SEED ===

DECLARE
  v_type NUMBER;
  v_system NUMBER;
  v_variant NUMBER;
  v_event_id NUMBER;
  v_source_id NUMBER;
  v_rule_id NUMBER;
  v_matches NUMBER;
  v_added NUMBER := 0;
  v_rules_added NUMBER := 0;
  v_validated NUMBER := 0;

  PROCEDURE ensure_observance(
      p_source VARCHAR2, p_code VARCHAR2,
      p_en VARCHAR2, p_fa VARCHAR2,
      p_month NUMBER, p_day NUMBER,
      p_weekday NUMBER DEFAULT NULL, p_ordinal NUMBER DEFAULT NULL
  ) IS
    v_expected_type VARCHAR2(40);
    v_current_name VARCHAR2(300);
    v_current_source NUMBER;
    v_existing_rule_type VARCHAR2(40);
    v_existing_month NUMBER;
    v_existing_day NUMBER;
    v_existing_weekday NUMBER;
    v_existing_ordinal NUMBER;
  BEGIN
    v_expected_type := CASE WHEN p_weekday IS NULL THEN 'ANNUAL_FIXED_DATE' ELSE 'ANNUAL_NTH_WEEKDAY' END;
    IF p_month NOT BETWEEN 1 AND 12 OR
       (p_weekday IS NULL AND (p_day IS NULL OR p_day NOT BETWEEN 1 AND 31)) OR
       (p_weekday IS NOT NULL AND (p_day IS NOT NULL OR p_weekday NOT BETWEEN 1 AND 7 OR p_ordinal NOT IN (-1,1,2,3,4,5))) THEN
      RAISE_APPLICATION_ERROR(-20101,'Invalid observance recurrence definition '||p_code);
    END IF;

    SELECT SOURCE_ID INTO v_source_id
      FROM CAL2.SOURCE_AUTHORITY
     WHERE SOURCE_CODE=p_source AND ACTIVE_FLAG='Y' AND AUTHORITY_LEVEL='OFFICIAL';

    -- Fail closed on exact bilingual duplicates under other codes; alias-level
    -- matching will require the full user Working Master, which is not in this ZIP.
    SELECT COUNT(*) INTO v_matches FROM CAL2.EVENT
     WHERE EVENT_CODE<>p_code
       AND (UPPER(TRIM(NAME_EN))=UPPER(TRIM(p_en)) OR TRIM(NAME_FA)=TRIM(p_fa));
    IF v_matches>0 THEN
      RAISE_APPLICATION_ERROR(-20102,'Review existing title alias before seeding: '||p_code||' / '||p_en);
    END IF;

    BEGIN
      SELECT EVENT_ID,NAME_EN,SOURCE_ID
        INTO v_event_id,v_current_name,v_current_source
        FROM CAL2.EVENT WHERE EVENT_CODE=p_code;
      IF NVL(UPPER(TRIM(v_current_name)),'~')<>UPPER(TRIM(p_en)) OR
         NVL(v_current_source,-1)<>v_source_id THEN
        RAISE_APPLICATION_ERROR(-20103,'Existing code/source/title conflicts with official seed: '||p_code);
      END IF;
    EXCEPTION WHEN NO_DATA_FOUND THEN
      SELECT NVL(MAX(EVENT_ID),0)+1 INTO v_event_id FROM CAL2.EVENT;
      INSERT INTO CAL2.EVENT (
        EVENT_ID,EVENT_CODE,EVENT_TYPE_ID,NAME_FA,NAME_EN,DESCRIPTION,
        RECURRENCE_TYPE,BASE_CALENDAR_SYSTEM_ID,SOURCE_ID,
        OFFICIAL_FLAG,DEFAULT_HOLIDAY_FLAG,ACTIVE_FLAG
      ) VALUES (
        v_event_id,p_code,v_type,p_fa,p_en,
        'CAL2 B1 | UNESCO/WHO listing; attributed authority: '||p_source||' | 2026-10-11',
        v_expected_type,v_system,v_source_id,'Y','N','Y'
      );
      v_added:=v_added+1;
    END;

    SELECT COUNT(*) INTO v_matches FROM CAL2.EVENT_RECURRENCE_RULE
     WHERE EVENT_ID=v_event_id AND ACTIVE_FLAG='Y';
    IF v_matches>1 THEN
      RAISE_APPLICATION_ERROR(-20104,'Multiple active rules; review required: '||p_code);
    ELSIF v_matches=1 THEN
      SELECT RULE_TYPE,MONTH_NO,DAY_NO,WEEKDAY_NO,WEEKDAY_ORDINAL
        INTO v_existing_rule_type,v_existing_month,v_existing_day,v_existing_weekday,v_existing_ordinal
        FROM CAL2.EVENT_RECURRENCE_RULE
       WHERE EVENT_ID=v_event_id AND ACTIVE_FLAG='Y';
      IF v_existing_rule_type<>v_expected_type OR v_existing_month<>p_month OR
         NVL(v_existing_day,-1)<>NVL(p_day,-1) OR
         NVL(v_existing_weekday,-1)<>NVL(p_weekday,-1) OR
         NVL(v_existing_ordinal,-2)<>NVL(p_ordinal,-2) THEN
         RAISE_APPLICATION_ERROR(-20105,'Existing recurrence differs from verified date: '||p_code);
      END IF;
    ELSE
      SELECT NVL(MAX(EVENT_RULE_ID),0)+1 INTO v_rule_id FROM CAL2.EVENT_RECURRENCE_RULE;
      INSERT INTO CAL2.EVENT_RECURRENCE_RULE (
        EVENT_RULE_ID,EVENT_ID,RULE_TYPE,CALENDAR_VARIANT_ID,YEAR_NO,
        MONTH_NO,DAY_NO,WEEKDAY_NO,WEEKDAY_ORDINAL,
        START_YEAR_NO,END_YEAR_NO,DAY_RESOLUTION_POLICY,SOURCE_ID,DESCRIPTION,ACTIVE_FLAG
      ) VALUES (
        v_rule_id,v_event_id,v_expected_type,v_variant,NULL,
        p_month,p_day,p_weekday,p_ordinal,NULL,NULL,'EXACT',v_source_id,
        'CAL2 B1 | UNESCO/WHO listing; authority '||p_source,'Y'
      );
      v_rules_added:=v_rules_added+1;
    END IF;
    v_validated:=v_validated+1;
  END;
BEGIN
  SELECT EVENT_TYPE_ID INTO v_type FROM CAL2.EVENT_TYPE
   WHERE EVENT_TYPE_CODE='INTERNATIONAL' AND ACTIVE_FLAG='Y';
  SELECT CALENDAR_SYSTEM_ID INTO v_system FROM CAL2.CALENDAR_SYSTEM
   WHERE CALENDAR_CODE='GREGORIAN' AND ACTIVE_FLAG='Y';
  SELECT CALENDAR_VARIANT_ID INTO v_variant FROM CAL2.CALENDAR_VARIANT
   WHERE CALENDAR_SYSTEM_ID=v_system AND IS_DEFAULT='Y' AND ACTIVE_FLAG='Y';
  LOCK TABLE CAL2.EVENT IN SHARE ROW EXCLUSIVE MODE;
  LOCK TABLE CAL2.EVENT_RECURRENCE_RULE IN SHARE ROW EXCLUSIVE MODE;

  -- Source (listing): https://www.unesco.org/en/days/list?hub=180536
  -- Source (UN proclaimed days): https://www.un.org/en/observances/list-days-weeks
  -- Source: https://www.who.int/campaigns
  -- Dynamic rules use the actual UNESCO definition, not a date from an older annual web page.
  ensure_observance('UNESCO','INTL_WORLD_LOGIC_DAY','World Logic Day','روز جهانی منطق',1,14,NULL,NULL);
  ensure_observance('UNITED_NATIONS','INTL_EDUCATION_DAY','International Day of Education','روز جهانی آموزش',1,24,NULL,NULL);
  ensure_observance('UNESCO','INTL_AFRICAN_AFRODESCENDANT_CULTURE_DAY','World Day for African and Afrodescendant Culture','روز جهانی فرهنگ آفریقایی و آفریقایی‌تباران',1,24,NULL,NULL);
  ensure_observance('UNESCO','INTL_WOMEN_MULTILATERALISM_DAY','International Day of Women in Multilateralism','روز جهانی زنان در چندجانبه‌گرایی',1,25,NULL,NULL);
  ensure_observance('UNITED_NATIONS','INTL_HOLOCAUST_REMEMBRANCE_DAY','International Day of Commemoration in Memory of the Victims of the Holocaust','روز جهانی یادبود قربانیان هولوکاست',1,27,NULL,NULL);
  ensure_observance('UNITED_NATIONS','INTL_WOMEN_GIRLS_SCIENCE_DAY','International Day of Women and Girls in Science','روز جهانی زنان و دختران در علم',2,11,NULL,NULL);
  ensure_observance('UNESCO','INTL_WORLD_RADIO_DAY','World Radio Day','روز جهانی رادیو',2,13,NULL,NULL);
  ensure_observance('UNESCO','INTL_MOTHER_LANGUAGE_DAY','International Mother Language Day','روز جهانی زبان مادری',2,21,NULL,NULL);
  ensure_observance('UNESCO','INTL_ENGINEERING_SUSTAINABLE_DAY','World Engineering Day for Sustainable Development','روز جهانی مهندسی برای توسعه پایدار',3,4,NULL,NULL);
  ensure_observance('UNESCO','INTL_DIGITAL_LEARNING_DAY','International Day for Digital Learning','روز جهانی یادگیری دیجیتال',3,19,NULL,NULL);
  ensure_observance('UNESCO','INTL_FRANCOPHONIE_DAY','International Francophonie Day','روز جهانی فرانکوفونی',3,20,NULL,NULL);
  ensure_observance('UNITED_NATIONS','INTL_RACIAL_DISCRIMINATION_ELIMINATION_DAY','International Day for the Elimination of Racial Discrimination','روز جهانی رفع تبعیض نژادی',3,21,NULL,NULL);
  ensure_observance('UNITED_NATIONS','INTL_GLACIERS_DAY','World Day for Glaciers','روز جهانی یخچال‌های طبیعی',3,21,NULL,NULL);
  ensure_observance('UNESCO','INTL_WORLD_POETRY_DAY','World Poetry Day','روز جهانی شعر',3,21,NULL,NULL);
  ensure_observance('UNITED_NATIONS','INTL_CONSCIENCE_DAY','International Day of Conscience','روز جهانی وجدان',4,5,NULL,NULL);
  ensure_observance('UNITED_NATIONS','INTL_SPORT_DEVELOPMENT_PEACE_DAY','International Day of Sport for Development and Peace','روز جهانی ورزش برای توسعه و صلح',4,6,NULL,NULL);
  ensure_observance('UNITED_NATIONS','INTL_TUTSI_GENOCIDE_REFLECTION_DAY','International Day of Reflection on the 1994 Genocide against the Tutsi in Rwanda','روز جهانی تأمل درباره نسل‌کشی سال ۱۹۹۴ علیه توتسی‌ها در رواندا',4,7,NULL,NULL);
  ensure_observance('UNESCO','INTL_WORLD_ART_DAY','World Art Day','روز جهانی هنر',4,15,NULL,NULL);
  ensure_observance('UNESCO','INTL_BOOK_COPYRIGHT_DAY','World Book and Copyright Day','روز جهانی کتاب و حق مؤلف',4,23,NULL,NULL);
  ensure_observance('UNITED_NATIONS','INTL_SMALL_ISLAND_STATES_DAY','International Day for Small Island Developing States','روز جهانی کشورهای جزیره‌ای کوچک در حال توسعه',4,25,NULL,NULL);
  ensure_observance('UNESCO','INTL_INTERNATIONAL_JAZZ_DAY','International Jazz Day','روز جهانی جاز',4,30,NULL,NULL);
  ensure_observance('UNITED_NATIONS','INTL_PRESS_FREEDOM_DAY','World Press Freedom Day','روز جهانی آزادی مطبوعات',5,3,NULL,NULL);
  ensure_observance('UNESCO','INTL_PORTUGUESE_LANGUAGE_DAY','World Portuguese Language Day','روز جهانی زبان پرتغالی',5,5,NULL,NULL);
  ensure_observance('UNESCO','INTL_AFRICAN_WORLD_HERITAGE_DAY','African World Heritage Day','روز میراث جهانی آفریقا',5,5,NULL,NULL);
  ensure_observance('UNITED_NATIONS','INTL_LIVING_TOGETHER_PEACE_DAY','International Day of Living Together in Peace','روز جهانی همزیستی مسالمت‌آمیز',5,16,NULL,NULL);
  ensure_observance('UNESCO','INTL_LIGHT_DAY','International Day of Light','روز جهانی نور',5,16,NULL,NULL);
  ensure_observance('UNESCO','INTL_WORLD_METROLOGY_DAY','World Metrology Day','روز جهانی اندازه‌شناسی',5,20,NULL,NULL);
  ensure_observance('UNITED_NATIONS','INTL_CULTURAL_DIVERSITY_DAY','World Day for Cultural Diversity for Dialogue and Development','روز جهانی تنوع فرهنگی برای گفت‌وگو و توسعه',5,21,NULL,NULL);
  ensure_observance('UNITED_NATIONS','INTL_BIODIVERSITY_DAY','International Day for Biological Diversity','روز جهانی تنوع زیستی',5,22,NULL,NULL);
  ensure_observance('UNITED_NATIONS','INTL_WORLD_OCEANS_DAY','World Oceans Day','روز جهانی اقیانوس‌ها',6,8,NULL,NULL);
  ensure_observance('UNESCO','INTL_KISWAHILI_LANGUAGE_DAY','Kiswahili Language Day','روز جهانی زبان سواحیلی',7,7,NULL,NULL);
  ensure_observance('UNITED_NATIONS','INTL_NELSON_MANDELA_DAY','Nelson Mandela International Day','روز جهانی نلسون ماندلا',7,18,NULL,NULL);
  ensure_observance('UNESCO','INTL_MANGROVE_ECOSYSTEM_DAY','International Day for the Conservation of the Mangrove Ecosystem','روز جهانی حفاظت از زیست‌بوم جنگل‌های حرا',7,26,NULL,NULL);
  ensure_observance('UNITED_NATIONS','INTL_INDIGENOUS_PEOPLES_DAY','International Day of the World''s Indigenous Peoples','روز جهانی مردمان بومی',8,9,NULL,NULL);
  ensure_observance('UNITED_NATIONS','INTL_YOUTH_DAY','International Youth Day','روز جهانی جوانان',8,12,NULL,NULL);
  ensure_observance('UNESCO','INTL_UNDERWATER_CULTURAL_HERITAGE_DAY','International Day for Underwater Cultural Heritage','روز جهانی میراث فرهنگی زیر آب',8,21,NULL,NULL);
  ensure_observance('UNESCO','INTL_SLAVE_TRADE_REMEMBRANCE_DAY','International Day for the Remembrance of the Slave Trade and its Abolition','روز جهانی یادبود تجارت برده و لغو آن',8,23,NULL,NULL);
  ensure_observance('UNESCO','INTL_LITERACY_DAY','International Literacy Day','روز جهانی سوادآموزی',9,8,NULL,NULL);
  ensure_observance('UNITED_NATIONS','INTL_PROTECT_EDUCATION_ATTACK_DAY','International Day to Protect Education from Attack','روز جهانی حفاظت از آموزش در برابر حمله',9,9,NULL,NULL);
  ensure_observance('UNESCO','INTL_CAVES_KARST_DAY','International Day for Caves and Karst','روز جهانی غارها و کارست',9,13,NULL,NULL);
  ensure_observance('UNITED_NATIONS','INTL_DEMOCRACY_DAY','International Day of Democracy','روز جهانی مردم‌سالاری',9,15,NULL,NULL);
  ensure_observance('UNESCO','INTL_UNIVERSITY_SPORT_DAY','International Day of University Sport','روز جهانی ورزش دانشگاهی',9,20,NULL,NULL);
  ensure_observance('UNESCO','INTL_ACCESS_INFORMATION_DAY','International Day for Universal Access to Information','روز جهانی دسترسی همگانی به اطلاعات',9,28,NULL,NULL);
  ensure_observance('UNESCO','INTL_GEODIVERSITY_DAY','International Geodiversity Day','روز جهانی زمین‌گوناگونی',10,6,NULL,NULL);
  ensure_observance('UNITED_NATIONS','INTL_DISASTER_RISK_REDUCTION_DAY','International Day for Disaster Risk Reduction','روز جهانی کاهش خطر بلایا',10,13,NULL,NULL);
  ensure_observance('UNESCO','INTL_INTANGIBLE_CULTURAL_HERITAGE_DAY','International Day of the Intangible Cultural Heritage','روز جهانی میراث فرهنگی ناملموس',10,17,NULL,NULL);
  ensure_observance('UNESCO','INTL_AUDIOVISUAL_HERITAGE_DAY','World Day for Audiovisual Heritage','روز جهانی میراث دیداری و شنیداری',10,27,NULL,NULL);
  ensure_observance('UNESCO','INTL_WORLD_CODING_DAY','World Coding Day','روز جهانی برنامه‌نویسی',10,29,NULL,NULL);
  ensure_observance('UNITED_NATIONS','INTL_END_IMPUNITY_JOURNALISTS_DAY','International Day to End Impunity for Crimes against Journalists','روز جهانی پایان دادن به مصونیت مرتکبان جرائم علیه روزنامه‌نگاران',11,2,NULL,NULL);
  ensure_observance('UNESCO','INTL_BIOSPHERE_RESERVES_DAY','International Day for Biosphere Reserves','روز جهانی ذخیره‌گاه‌های زیست‌کره',11,3,NULL,NULL);
  ensure_observance('UNESCO','INTL_ROMANI_LANGUAGE_DAY','World Day of Romani Language','روز جهانی زبان رومانی',11,5,NULL,NULL);
  ensure_observance('UNITED_NATIONS','INTL_TSUNAMI_AWARENESS_DAY','World Tsunami Awareness Day','روز جهانی آگاهی از سونامی',11,5,NULL,NULL);
  ensure_observance('UNESCO','INTL_SCHOOL_ANTIBULLYING_DAY','International Day against Violence and Bullying at School, including Cyberbullying','روز جهانی مقابله با خشونت و قلدری در مدرسه از جمله قلدری سایبری',11,NULL,4,1);
  ensure_observance('UNESCO','INTL_ILLICIT_CULTURAL_PROPERTY_DAY','International Day against Illicit Trafficking in Cultural Property','روز جهانی مبارزه با قاچاق غیرقانونی اموال فرهنگی',11,14,NULL,NULL);
  ensure_observance('UNESCO','INTL_TOLERANCE_DAY','International Day for Tolerance','روز جهانی مدارا',11,16,NULL,NULL);
  ensure_observance('UNESCO','INTL_ISLAMIC_ART_DAY','International Day of Islamic Art','روز جهانی هنر اسلامی',11,18,NULL,NULL);
  ensure_observance('UNESCO','INTL_WORLD_PHILOSOPHY_DAY','World Philosophy Day','روز جهانی فلسفه',11,NULL,4,3);
  ensure_observance('UNESCO','INTL_OLIVE_TREE_DAY','World Olive Tree Day','روز جهانی درخت زیتون',11,26,NULL,NULL);
  ensure_observance('UNESCO','INTL_SCIENCE_ENGAGEMENT_SUSTAINABLE_DAY','International Day of Engagement in Science for Sustainable Development','روز جهانی مشارکت در علم برای توسعه پایدار',11,27,NULL,NULL);
  ensure_observance('UNITED_NATIONS','INTL_PALESTINIAN_SOLIDARITY_DAY','International Day of Solidarity with the Palestinian People','روز جهانی همبستگی با مردم فلسطین',11,29,NULL,NULL);
  ensure_observance('UNESCO','INTL_WORLD_FUTURES_DAY','World Futures Day','روز جهانی آینده‌ها',12,2,NULL,NULL);
  ensure_observance('UNITED_NATIONS','INTL_MIGRANTS_DAY','International Migrants Day','روز جهانی مهاجران',12,18,NULL,NULL);
  ensure_observance('UNESCO','INTL_ARABIC_LANGUAGE_DAY','World Arabic Language Day','روز جهانی زبان عربی',12,18,NULL,NULL);
  ensure_observance('WHO','INTL_NEGLECTED_TROPICAL_DISEASES_DAY','World Neglected Tropical Diseases Day','روز جهانی بیماری‌های گرمسیری فراموش‌شده',1,30,NULL,NULL);
  ensure_observance('WHO','INTL_TUBERCULOSIS_DAY','World TB Day','روز جهانی سل',3,24,NULL,NULL);
  ensure_observance('WHO','INTL_CHAGAS_DISEASE_DAY','World Chagas Disease Day','روز جهانی بیماری شاگاس',4,14,NULL,NULL);
  ensure_observance('WHO','INTL_MALARIA_DAY','World Malaria Day','روز جهانی مالاریا',4,25,NULL,NULL);
  ensure_observance('WHO','INTL_DROWNING_PREVENTION_DAY','World Drowning Prevention Day','روز جهانی پیشگیری از غرق‌شدگی',7,25,NULL,NULL);
  ensure_observance('WHO','INTL_HEPATITIS_DAY','World Hepatitis Day','روز جهانی هپاتیت',7,28,NULL,NULL);
  ensure_observance('WHO','INTL_PATIENT_SAFETY_DAY','World Patient Safety Day','روز جهانی ایمنی بیمار',9,17,NULL,NULL);
  ensure_observance('WHO','INTL_PREMATURITY_DAY','World Prematurity Day','روز جهانی نوزادان نارس',11,15,NULL,NULL);
  ensure_observance('WHO','INTL_CERVICAL_CANCER_ELIMINATION_DAY','World Cervical Cancer Elimination Day','روز جهانی ریشه‌کنی سرطان دهانه رحم',11,17,NULL,NULL);

  IF v_validated<>72 THEN RAISE_APPLICATION_ERROR(-20106,'Expected 72 reviewed definitions, got '||v_validated); END IF;
  DBMS_OUTPUT.PUT_LINE('CAL2_B1_EVENTS_ADDED='||v_added||' VERIFIED=72');
  DBMS_OUTPUT.PUT_LINE('CAL2_B1_EVENT_RULES_ADDED='||v_rules_added);

  -- Reconcile the previously present official Maritime definition: IMO = last Thursday of September.
  -- Preserve its Phase-A defining source (UNITED_NATIONS), do not duplicate the event.
  SELECT EVENT_ID,SOURCE_ID INTO v_event_id,v_source_id FROM CAL2.EVENT
   WHERE EVENT_CODE='INTL_WORLD_MARITIME_DAY' AND ACTIVE_FLAG='Y' AND OFFICIAL_FLAG='Y';
  SELECT COUNT(*) INTO v_matches FROM CAL2.EVENT_RECURRENCE_RULE WHERE EVENT_ID=v_event_id AND ACTIVE_FLAG='Y';
  IF v_matches=0 THEN
    SELECT NVL(MAX(EVENT_RULE_ID),0)+1 INTO v_rule_id FROM CAL2.EVENT_RECURRENCE_RULE;
    INSERT INTO CAL2.EVENT_RECURRENCE_RULE (
      EVENT_RULE_ID,EVENT_ID,RULE_TYPE,CALENDAR_VARIANT_ID,YEAR_NO,MONTH_NO,DAY_NO,
      WEEKDAY_NO,WEEKDAY_ORDINAL,START_YEAR_NO,END_YEAR_NO,DAY_RESOLUTION_POLICY,
      SOURCE_ID,DESCRIPTION,ACTIVE_FLAG
    ) VALUES (
      v_rule_id,v_event_id,'ANNUAL_NTH_WEEKDAY',v_variant,NULL,9,NULL,
      4,-1,NULL,NULL,'EXACT',v_source_id,
      'IMO: last Thursday of September | https://www.imo.org/en/about/events/worldmaritimeday','Y'
    );
    v_rules_added:=v_rules_added+1;
    DBMS_OUTPUT.PUT_LINE('MARITIME_RULE_CREATED=1');
  ELSIF v_matches=1 THEN
    SELECT COUNT(*) INTO v_matches FROM CAL2.EVENT_RECURRENCE_RULE
     WHERE EVENT_ID=v_event_id AND ACTIVE_FLAG='Y' AND RULE_TYPE='ANNUAL_NTH_WEEKDAY'
       AND MONTH_NO=9 AND WEEKDAY_NO=4 AND WEEKDAY_ORDINAL=-1;
    IF v_matches<>1 THEN RAISE_APPLICATION_ERROR(-20107,'Maritime existing rule conflict'); END IF;
    DBMS_OUTPUT.PUT_LINE('MARITIME_RULE_ALREADY_PRESENT=1');
  ELSE
    RAISE_APPLICATION_ERROR(-20108,'Multiple active Maritime rules');
  END IF;
  UPDATE CAL2.EVENT SET RECURRENCE_TYPE='ANNUAL_NTH_WEEKDAY'
   WHERE EVENT_ID=v_event_id AND NVL(RECURRENCE_TYPE,'~')<>'ANNUAL_NTH_WEEKDAY';
  DBMS_OUTPUT.PUT_LINE('CAL2_B1_ALL_NEW_RULES_ADDED='||v_rules_added);
END;
/

-- Materialize all occurrences for the 72 bounded definitions plus existing World Maritime Day.
-- Use the same ISO weekday/ordinal semantics as the Java recurrence engine.
-- Existing event/day occurrences (GENERATED/MANUAL/OFFICIAL) are never overwritten.
-- Lock EO before choosing MAX(EVENT_OCCURRENCE_ID) to prevent duplicate PKs.
DECLARE
  v_next NUMBER;
  v_added NUMBER;
BEGIN
  LOCK TABLE CAL2.EVENT_OCCURRENCE IN SHARE ROW EXCLUSIVE MODE;
  SELECT NVL(MAX(EVENT_OCCURRENCE_ID),0)+1 INTO v_next FROM CAL2.EVENT_OCCURRENCE;
  INSERT INTO CAL2.EVENT_OCCURRENCE (
    EVENT_OCCURRENCE_ID,EVENT_ID,EVENT_RULE_ID,DAY_ID,SOURCE_ID,
    OCCURRENCE_SOURCE,DATA_STATUS,HOLIDAY_FLAG,START_TIME,END_TIME,DESCRIPTION,DATASET_VERSION_ID
  )
  SELECT v_next+ROW_NUMBER() OVER (ORDER BY CD.DAY_ID,R.EVENT_RULE_ID)-1,
         E.EVENT_ID,R.EVENT_RULE_ID,CD.DAY_ID,R.SOURCE_ID,
         'GENERATED','CALCULATED',E.DEFAULT_HOLIDAY_FLAG,NULL,NULL,
         R.DESCRIPTION,CD.DATASET_VERSION_ID
    FROM CAL2.EVENT_RECURRENCE_RULE R
    JOIN CAL2.EVENT E ON E.EVENT_ID=R.EVENT_ID
    JOIN CAL2.CALENDAR_DATE CD ON CD.CALENDAR_VARIANT_ID=R.CALENDAR_VARIANT_ID
    JOIN CAL2.CANONICAL_DAY D ON D.DAY_ID=CD.DAY_ID
    JOIN CAL2.WEEKDAY W ON W.WEEKDAY_ID=D.WEEKDAY_ID
   WHERE E.EVENT_CODE IN (
     'INTL_WORLD_LOGIC_DAY', 'INTL_EDUCATION_DAY', 'INTL_AFRICAN_AFRODESCENDANT_CULTURE_DAY', 'INTL_WOMEN_MULTILATERALISM_DAY',
     'INTL_HOLOCAUST_REMEMBRANCE_DAY', 'INTL_WOMEN_GIRLS_SCIENCE_DAY', 'INTL_WORLD_RADIO_DAY', 'INTL_MOTHER_LANGUAGE_DAY',
     'INTL_ENGINEERING_SUSTAINABLE_DAY', 'INTL_DIGITAL_LEARNING_DAY', 'INTL_FRANCOPHONIE_DAY', 'INTL_RACIAL_DISCRIMINATION_ELIMINATION_DAY',
     'INTL_GLACIERS_DAY', 'INTL_WORLD_POETRY_DAY', 'INTL_CONSCIENCE_DAY', 'INTL_SPORT_DEVELOPMENT_PEACE_DAY',
     'INTL_TUTSI_GENOCIDE_REFLECTION_DAY', 'INTL_WORLD_ART_DAY', 'INTL_BOOK_COPYRIGHT_DAY', 'INTL_SMALL_ISLAND_STATES_DAY',
     'INTL_INTERNATIONAL_JAZZ_DAY', 'INTL_PRESS_FREEDOM_DAY', 'INTL_PORTUGUESE_LANGUAGE_DAY', 'INTL_AFRICAN_WORLD_HERITAGE_DAY',
     'INTL_LIVING_TOGETHER_PEACE_DAY', 'INTL_LIGHT_DAY', 'INTL_WORLD_METROLOGY_DAY', 'INTL_CULTURAL_DIVERSITY_DAY',
     'INTL_BIODIVERSITY_DAY', 'INTL_WORLD_OCEANS_DAY', 'INTL_KISWAHILI_LANGUAGE_DAY', 'INTL_NELSON_MANDELA_DAY',
     'INTL_MANGROVE_ECOSYSTEM_DAY', 'INTL_INDIGENOUS_PEOPLES_DAY', 'INTL_YOUTH_DAY', 'INTL_UNDERWATER_CULTURAL_HERITAGE_DAY',
     'INTL_SLAVE_TRADE_REMEMBRANCE_DAY', 'INTL_LITERACY_DAY', 'INTL_PROTECT_EDUCATION_ATTACK_DAY', 'INTL_CAVES_KARST_DAY',
     'INTL_DEMOCRACY_DAY', 'INTL_UNIVERSITY_SPORT_DAY', 'INTL_ACCESS_INFORMATION_DAY', 'INTL_GEODIVERSITY_DAY',
     'INTL_DISASTER_RISK_REDUCTION_DAY', 'INTL_INTANGIBLE_CULTURAL_HERITAGE_DAY', 'INTL_AUDIOVISUAL_HERITAGE_DAY', 'INTL_WORLD_CODING_DAY',
     'INTL_END_IMPUNITY_JOURNALISTS_DAY', 'INTL_BIOSPHERE_RESERVES_DAY', 'INTL_ROMANI_LANGUAGE_DAY', 'INTL_TSUNAMI_AWARENESS_DAY',
     'INTL_SCHOOL_ANTIBULLYING_DAY', 'INTL_ILLICIT_CULTURAL_PROPERTY_DAY', 'INTL_TOLERANCE_DAY', 'INTL_ISLAMIC_ART_DAY',
     'INTL_WORLD_PHILOSOPHY_DAY', 'INTL_OLIVE_TREE_DAY', 'INTL_SCIENCE_ENGAGEMENT_SUSTAINABLE_DAY', 'INTL_PALESTINIAN_SOLIDARITY_DAY',
     'INTL_WORLD_FUTURES_DAY', 'INTL_MIGRANTS_DAY', 'INTL_ARABIC_LANGUAGE_DAY', 'INTL_NEGLECTED_TROPICAL_DISEASES_DAY',
     'INTL_TUBERCULOSIS_DAY', 'INTL_CHAGAS_DISEASE_DAY', 'INTL_MALARIA_DAY', 'INTL_DROWNING_PREVENTION_DAY',
     'INTL_HEPATITIS_DAY', 'INTL_PATIENT_SAFETY_DAY', 'INTL_PREMATURITY_DAY', 'INTL_CERVICAL_CANCER_ELIMINATION_DAY',
     'INTL_WORLD_MARITIME_DAY'
   )
     AND E.ACTIVE_FLAG='Y' AND R.ACTIVE_FLAG='Y'
     AND CD.MONTH_NO=R.MONTH_NO
     AND (R.START_YEAR_NO IS NULL OR CD.YEAR_NO>=R.START_YEAR_NO)
     AND (R.END_YEAR_NO IS NULL OR CD.YEAR_NO<=R.END_YEAR_NO)
     AND (
       (R.RULE_TYPE='ANNUAL_FIXED_DATE' AND CD.DAY_NO=R.DAY_NO)
       OR (R.RULE_TYPE='ANNUAL_NTH_WEEKDAY' AND W.ISO_WEEKDAY_NO=R.WEEKDAY_NO
           AND (
              (R.WEEKDAY_ORDINAL>0 AND FLOOR((CD.DAY_NO-1)/7)+1=R.WEEKDAY_ORDINAL)
              OR (R.WEEKDAY_ORDINAL=-1 AND CD.DAY_NO+7 > (
                SELECT MAX(MD.DAY_NO) FROM CAL2.CALENDAR_DATE MD
                 WHERE MD.CALENDAR_VARIANT_ID=CD.CALENDAR_VARIANT_ID
                   AND MD.YEAR_NO=CD.YEAR_NO AND MD.MONTH_NO=CD.MONTH_NO
              ))
           ))
     )
     AND NOT EXISTS (SELECT 1 FROM CAL2.EVENT_OCCURRENCE X
        WHERE X.EVENT_ID=E.EVENT_ID AND X.DAY_ID=CD.DAY_ID);
  v_added:=SQL%ROWCOUNT;
  DBMS_OUTPUT.PUT_LINE('CAL2_B1_OCCURRENCES_MATERIALIZED='||v_added);
END;
/

-- Validate seeded definitions/rules, and example variable dates for Gregorian 2026.
DECLARE
  v_count NUMBER;
  v_d NUMBER;
  PROCEDURE assert_day(p_code VARCHAR2,p_iso VARCHAR2) IS
    n NUMBER;
    valid_day NUMBER;
  BEGIN
    SELECT COUNT(*) INTO valid_day FROM CAL2.CANONICAL_DAY
      WHERE CANONICAL_DATE=TO_DATE(p_iso,'YYYY-MM-DD');
    IF valid_day=0 THEN
      DBMS_OUTPUT.PUT_LINE('WARNING: canonical day is outside dataset: '||p_iso);
      RETURN;
    END IF;
    SELECT COUNT(*) INTO n FROM CAL2.EVENT_OCCURRENCE EO
      JOIN CAL2.EVENT E ON E.EVENT_ID=EO.EVENT_ID
      JOIN CAL2.CANONICAL_DAY CD ON CD.DAY_ID=EO.DAY_ID
     WHERE E.EVENT_CODE=p_code AND CD.CANONICAL_DATE=TO_DATE(p_iso,'YYYY-MM-DD');
    IF n<>1 THEN
      RAISE_APPLICATION_ERROR(-20109,'B1 variable date occurrence failed: '||p_code||' '||p_iso||' got='||n);
    END IF;
    DBMS_OUTPUT.PUT_LINE('VARIABLE_DATE_PASS '||p_code||' '||p_iso);
  END;
BEGIN
  SELECT COUNT(*) INTO v_count FROM CAL2.EVENT_RECURRENCE_RULE R
   JOIN CAL2.EVENT E ON E.EVENT_ID=R.EVENT_ID
   WHERE E.EVENT_CODE IN ('INTL_WORLD_MARITIME_DAY','INTL_WORLD_PHILOSOPHY_DAY','INTL_SCHOOL_ANTIBULLYING_DAY')
     AND R.ACTIVE_FLAG='Y' AND R.RULE_TYPE='ANNUAL_NTH_WEEKDAY';
  IF v_count<>3 THEN RAISE_APPLICATION_ERROR(-20110,'Expected 3 B1 dynamic rules; got '||v_count); END IF;
  assert_day('INTL_WORLD_MARITIME_DAY','2026-09-24');
  assert_day('INTL_WORLD_PHILOSOPHY_DAY','2026-11-19');
  assert_day('INTL_SCHOOL_ANTIBULLYING_DAY','2026-11-05');
  DBMS_OUTPUT.PUT_LINE('CAL2_B1_VERIFIED_DEFINITIONS=72 DYNAMIC_RULES=3');
END;
/
COMMIT;
PROMPT CAL2_B1_SEED_AND_OCCURRENCE_COMMIT_COMPLETE
