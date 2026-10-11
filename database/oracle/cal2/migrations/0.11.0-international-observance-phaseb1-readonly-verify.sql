-- CAL2 0.11.0 B1 Verification: pure SELECT/metadata; no write, no DDL.
-- Expected on pristine 2026-10-11 baseline: EVENT 244 -> 316, INTERNATIONAL 60 -> 132.
-- All 72 definitions and their rules must be active, correctly attributed and
-- map to the exact reviewed dates. World Maritime Day completes 3 variable rules.
-- Execute after schema + seed B1 migrations in SQL Developer (F5).
SET SERVEROUTPUT ON SIZE UNLIMITED
SET DEFINE OFF
PROMPT === CAL2 B1 International observance READ ONLY verifier ===
DECLARE
  v_expected NUMBER := 0;
  v_correct_events NUMBER := 0;
  v_correct_rules NUMBER := 0;
  v_missing NUMBER := 0;
  v_mismatch NUMBER := 0;
  v_seed_2026_verified NUMBER := 0;
  v_seed_2026_skipped NUMBER := 0;
  v_seed_2026_fail NUMBER := 0;
  v_total NUMBER;
  v_intl NUMBER;
  v_column1 NUMBER;
  v_column2 NUMBER;
  v_rule_count NUMBER;
  v_sources NUMBER;
  v_dynamic NUMBER;
  v_maritime NUMBER;
  v_2026_checks NUMBER := 0;
  v_2026_fail NUMBER := 0;

  PROCEDURE check_observance(p_source VARCHAR2,p_code VARCHAR2,p_name VARCHAR2,
      p_month NUMBER,p_day NUMBER,p_weekday NUMBER,p_ordinal NUMBER) IS
    e_count NUMBER;
    r_count NUMBER;
    o_count NUMBER;
    expected_2026 NUMBER;
  BEGIN
    v_expected := v_expected+1;
    SELECT COUNT(*) INTO e_count
      FROM CAL2.EVENT E JOIN CAL2.EVENT_TYPE ET ON E.EVENT_TYPE_ID=ET.EVENT_TYPE_ID
       JOIN CAL2.SOURCE_AUTHORITY S ON E.SOURCE_ID=S.SOURCE_ID
     WHERE E.EVENT_CODE=p_code AND E.NAME_EN=p_name
       AND E.ACTIVE_FLAG='Y' AND E.OFFICIAL_FLAG='Y'
       AND ET.EVENT_TYPE_CODE='INTERNATIONAL' AND S.SOURCE_CODE=p_source;
    IF e_count=1 THEN v_correct_events:=v_correct_events+1;
    ELSE v_missing:=v_missing+1; DBMS_OUTPUT.PUT_LINE('EVENT_MISSING_OR_WRONG_SOURCE='||p_code); END IF;

    SELECT COUNT(*) INTO r_count
      FROM CAL2.EVENT E
      JOIN CAL2.EVENT_RECURRENCE_RULE R ON R.EVENT_ID=E.EVENT_ID
      JOIN CAL2.SOURCE_AUTHORITY S ON S.SOURCE_ID=R.SOURCE_ID
      JOIN CAL2.CALENDAR_VARIANT V ON V.CALENDAR_VARIANT_ID=R.CALENDAR_VARIANT_ID
      JOIN CAL2.CALENDAR_SYSTEM CS ON CS.CALENDAR_SYSTEM_ID=V.CALENDAR_SYSTEM_ID
     WHERE E.EVENT_CODE=p_code AND R.ACTIVE_FLAG='Y'
       AND R.RULE_TYPE=CASE WHEN p_weekday IS NULL THEN 'ANNUAL_FIXED_DATE' ELSE 'ANNUAL_NTH_WEEKDAY' END
       AND R.MONTH_NO=p_month AND NVL(R.DAY_NO,-1)=NVL(p_day,-1)
       AND NVL(R.WEEKDAY_NO,-1)=NVL(p_weekday,-1)
       AND NVL(R.WEEKDAY_ORDINAL,-2)=NVL(p_ordinal,-2)
       AND R.YEAR_NO IS NULL AND R.START_YEAR_NO IS NULL AND R.END_YEAR_NO IS NULL
       AND R.DAY_RESOLUTION_POLICY='EXACT' AND CS.CALENDAR_CODE='GREGORIAN'
       AND S.SOURCE_CODE=p_source;
    IF r_count=1 THEN v_correct_rules:=v_correct_rules+1;
    ELSE v_mismatch:=v_mismatch+1; DBMS_OUTPUT.PUT_LINE('RULE_MISSING_OR_WRONG='||p_code); END IF;

    -- Only verify materialized dates where the Gregorian 2026 canonical day exists.
    SELECT COUNT(*),COUNT(EO.EVENT_OCCURRENCE_ID)
      INTO expected_2026,o_count
      FROM CAL2.CALENDAR_DATE CD
      JOIN CAL2.CALENDAR_VARIANT V ON V.CALENDAR_VARIANT_ID=CD.CALENDAR_VARIANT_ID
      JOIN CAL2.CALENDAR_SYSTEM CS ON CS.CALENDAR_SYSTEM_ID=V.CALENDAR_SYSTEM_ID
      JOIN CAL2.CANONICAL_DAY D ON D.DAY_ID=CD.DAY_ID
      JOIN CAL2.WEEKDAY W ON W.WEEKDAY_ID=D.WEEKDAY_ID
      LEFT JOIN CAL2.EVENT E ON E.EVENT_CODE=p_code
      LEFT JOIN CAL2.EVENT_OCCURRENCE EO ON EO.EVENT_ID=E.EVENT_ID AND EO.DAY_ID=CD.DAY_ID
     WHERE CS.CALENDAR_CODE='GREGORIAN' AND V.IS_DEFAULT='Y'
       AND CD.YEAR_NO=2026 AND CD.MONTH_NO=p_month
       AND ((p_weekday IS NULL AND CD.DAY_NO=p_day)
        OR (p_weekday IS NOT NULL AND W.ISO_WEEKDAY_NO=p_weekday
            AND ((p_ordinal>0 AND FLOOR((CD.DAY_NO-1)/7)+1=p_ordinal)
              OR (p_ordinal=-1 AND CD.DAY_NO+7>(
                SELECT MAX(MD.DAY_NO) FROM CAL2.CALENDAR_DATE MD
                WHERE MD.CALENDAR_VARIANT_ID=CD.CALENDAR_VARIANT_ID
                  AND MD.YEAR_NO=CD.YEAR_NO AND MD.MONTH_NO=CD.MONTH_NO)))));
    IF expected_2026=0 THEN v_seed_2026_skipped:=v_seed_2026_skipped+1;
    ELSIF expected_2026=1 AND o_count=1 THEN v_seed_2026_verified:=v_seed_2026_verified+1;
    ELSE
      v_seed_2026_fail:=v_seed_2026_fail+1;
      DBMS_OUTPUT.PUT_LINE('FAIL_2026_OCCURRENCE '||p_code||' DAY_MATCHES='||expected_2026||' OCCURRENCES='||o_count);
    END IF;
  END;

  PROCEDURE check_2026(p_code VARCHAR2,p_iso VARCHAR2) IS
    v_day NUMBER;
    v_has_gregorian NUMBER;
    v_occurrences NUMBER;
  BEGIN
    SELECT COUNT(*) INTO v_has_gregorian
     FROM CAL2.CANONICAL_DAY CD
     JOIN CAL2.CALENDAR_DATE C ON C.DAY_ID=CD.DAY_ID
     JOIN CAL2.CALENDAR_VARIANT V ON V.CALENDAR_VARIANT_ID=C.CALENDAR_VARIANT_ID
     JOIN CAL2.CALENDAR_SYSTEM CS ON CS.CALENDAR_SYSTEM_ID=V.CALENDAR_SYSTEM_ID
     WHERE CD.CANONICAL_DATE=TO_DATE(p_iso,'YYYY-MM-DD')
       AND CS.CALENDAR_CODE='GREGORIAN' AND V.IS_DEFAULT='Y';
    IF v_has_gregorian=0 THEN
      DBMS_OUTPUT.PUT_LINE('SKIP_2026_DATE_NOT_IN_CANONICAL_DATASET '||p_code||' '||p_iso);
      RETURN;
    END IF;
    v_2026_checks:=v_2026_checks+1;
    SELECT COUNT(*) INTO v_occurrences FROM CAL2.EVENT_OCCURRENCE EO
      JOIN CAL2.EVENT E ON E.EVENT_ID=EO.EVENT_ID
      JOIN CAL2.CANONICAL_DAY D ON D.DAY_ID=EO.DAY_ID
     WHERE E.EVENT_CODE=p_code AND D.CANONICAL_DATE=TO_DATE(p_iso,'YYYY-MM-DD');
    IF v_occurrences<>1 THEN
      v_2026_fail:=v_2026_fail+1;
      DBMS_OUTPUT.PUT_LINE('FAIL_2026_DATE '||p_code||' '||p_iso||' COUNT='||v_occurrences);
    ELSE DBMS_OUTPUT.PUT_LINE('PASS_2026_DATE '||p_code||' '||p_iso); END IF;
  END;
BEGIN
  SELECT COUNT(*) INTO v_total FROM CAL2.EVENT;
  SELECT COUNT(*) INTO v_intl FROM CAL2.EVENT E
    JOIN CAL2.EVENT_TYPE ET ON ET.EVENT_TYPE_ID=E.EVENT_TYPE_ID WHERE ET.EVENT_TYPE_CODE='INTERNATIONAL';
  SELECT COUNT(*) INTO v_column1 FROM ALL_TAB_COLUMNS
   WHERE OWNER='CAL2' AND TABLE_NAME='EVENT_RECURRENCE_RULE' AND COLUMN_NAME='WEEKDAY_NO';
  SELECT COUNT(*) INTO v_column2 FROM ALL_TAB_COLUMNS
   WHERE OWNER='CAL2' AND TABLE_NAME='EVENT_RECURRENCE_RULE' AND COLUMN_NAME='WEEKDAY_ORDINAL';
  SELECT COUNT(*) INTO v_sources FROM CAL2.SOURCE_AUTHORITY
    WHERE SOURCE_CODE IN ('UNITED_NATIONS','UNESCO','WHO') AND ACTIVE_FLAG='Y';

  DBMS_OUTPUT.PUT_LINE('EVENTS_TOTAL='||v_total||' INTERNATIONAL='||v_intl);
  DBMS_OUTPUT.PUT_LINE('SCHEMA_WEEKDAY_COLUMNS='||v_column1||','||v_column2||' OFFICIAL_AUTHORITIES='||v_sources);
  check_observance('UNESCO','INTL_WORLD_LOGIC_DAY','World Logic Day',1,14,NULL,NULL);
  check_observance('UNITED_NATIONS','INTL_EDUCATION_DAY','International Day of Education',1,24,NULL,NULL);
  check_observance('UNESCO','INTL_AFRICAN_AFRODESCENDANT_CULTURE_DAY','World Day for African and Afrodescendant Culture',1,24,NULL,NULL);
  check_observance('UNESCO','INTL_WOMEN_MULTILATERALISM_DAY','International Day of Women in Multilateralism',1,25,NULL,NULL);
  check_observance('UNITED_NATIONS','INTL_HOLOCAUST_REMEMBRANCE_DAY','International Day of Commemoration in Memory of the Victims of the Holocaust',1,27,NULL,NULL);
  check_observance('UNITED_NATIONS','INTL_WOMEN_GIRLS_SCIENCE_DAY','International Day of Women and Girls in Science',2,11,NULL,NULL);
  check_observance('UNESCO','INTL_WORLD_RADIO_DAY','World Radio Day',2,13,NULL,NULL);
  check_observance('UNESCO','INTL_MOTHER_LANGUAGE_DAY','International Mother Language Day',2,21,NULL,NULL);
  check_observance('UNESCO','INTL_ENGINEERING_SUSTAINABLE_DAY','World Engineering Day for Sustainable Development',3,4,NULL,NULL);
  check_observance('UNESCO','INTL_DIGITAL_LEARNING_DAY','International Day for Digital Learning',3,19,NULL,NULL);
  check_observance('UNESCO','INTL_FRANCOPHONIE_DAY','International Francophonie Day',3,20,NULL,NULL);
  check_observance('UNITED_NATIONS','INTL_RACIAL_DISCRIMINATION_ELIMINATION_DAY','International Day for the Elimination of Racial Discrimination',3,21,NULL,NULL);
  check_observance('UNITED_NATIONS','INTL_GLACIERS_DAY','World Day for Glaciers',3,21,NULL,NULL);
  check_observance('UNESCO','INTL_WORLD_POETRY_DAY','World Poetry Day',3,21,NULL,NULL);
  check_observance('UNITED_NATIONS','INTL_CONSCIENCE_DAY','International Day of Conscience',4,5,NULL,NULL);
  check_observance('UNITED_NATIONS','INTL_SPORT_DEVELOPMENT_PEACE_DAY','International Day of Sport for Development and Peace',4,6,NULL,NULL);
  check_observance('UNITED_NATIONS','INTL_TUTSI_GENOCIDE_REFLECTION_DAY','International Day of Reflection on the 1994 Genocide against the Tutsi in Rwanda',4,7,NULL,NULL);
  check_observance('UNESCO','INTL_WORLD_ART_DAY','World Art Day',4,15,NULL,NULL);
  check_observance('UNESCO','INTL_BOOK_COPYRIGHT_DAY','World Book and Copyright Day',4,23,NULL,NULL);
  check_observance('UNITED_NATIONS','INTL_SMALL_ISLAND_STATES_DAY','International Day for Small Island Developing States',4,25,NULL,NULL);
  check_observance('UNESCO','INTL_INTERNATIONAL_JAZZ_DAY','International Jazz Day',4,30,NULL,NULL);
  check_observance('UNITED_NATIONS','INTL_PRESS_FREEDOM_DAY','World Press Freedom Day',5,3,NULL,NULL);
  check_observance('UNESCO','INTL_PORTUGUESE_LANGUAGE_DAY','World Portuguese Language Day',5,5,NULL,NULL);
  check_observance('UNESCO','INTL_AFRICAN_WORLD_HERITAGE_DAY','African World Heritage Day',5,5,NULL,NULL);
  check_observance('UNITED_NATIONS','INTL_LIVING_TOGETHER_PEACE_DAY','International Day of Living Together in Peace',5,16,NULL,NULL);
  check_observance('UNESCO','INTL_LIGHT_DAY','International Day of Light',5,16,NULL,NULL);
  check_observance('UNESCO','INTL_WORLD_METROLOGY_DAY','World Metrology Day',5,20,NULL,NULL);
  check_observance('UNITED_NATIONS','INTL_CULTURAL_DIVERSITY_DAY','World Day for Cultural Diversity for Dialogue and Development',5,21,NULL,NULL);
  check_observance('UNITED_NATIONS','INTL_BIODIVERSITY_DAY','International Day for Biological Diversity',5,22,NULL,NULL);
  check_observance('UNITED_NATIONS','INTL_WORLD_OCEANS_DAY','World Oceans Day',6,8,NULL,NULL);
  check_observance('UNESCO','INTL_KISWAHILI_LANGUAGE_DAY','Kiswahili Language Day',7,7,NULL,NULL);
  check_observance('UNITED_NATIONS','INTL_NELSON_MANDELA_DAY','Nelson Mandela International Day',7,18,NULL,NULL);
  check_observance('UNESCO','INTL_MANGROVE_ECOSYSTEM_DAY','International Day for the Conservation of the Mangrove Ecosystem',7,26,NULL,NULL);
  check_observance('UNITED_NATIONS','INTL_INDIGENOUS_PEOPLES_DAY','International Day of the World''s Indigenous Peoples',8,9,NULL,NULL);
  check_observance('UNITED_NATIONS','INTL_YOUTH_DAY','International Youth Day',8,12,NULL,NULL);
  check_observance('UNESCO','INTL_UNDERWATER_CULTURAL_HERITAGE_DAY','International Day for Underwater Cultural Heritage',8,21,NULL,NULL);
  check_observance('UNESCO','INTL_SLAVE_TRADE_REMEMBRANCE_DAY','International Day for the Remembrance of the Slave Trade and its Abolition',8,23,NULL,NULL);
  check_observance('UNESCO','INTL_LITERACY_DAY','International Literacy Day',9,8,NULL,NULL);
  check_observance('UNITED_NATIONS','INTL_PROTECT_EDUCATION_ATTACK_DAY','International Day to Protect Education from Attack',9,9,NULL,NULL);
  check_observance('UNESCO','INTL_CAVES_KARST_DAY','International Day for Caves and Karst',9,13,NULL,NULL);
  check_observance('UNITED_NATIONS','INTL_DEMOCRACY_DAY','International Day of Democracy',9,15,NULL,NULL);
  check_observance('UNESCO','INTL_UNIVERSITY_SPORT_DAY','International Day of University Sport',9,20,NULL,NULL);
  check_observance('UNESCO','INTL_ACCESS_INFORMATION_DAY','International Day for Universal Access to Information',9,28,NULL,NULL);
  check_observance('UNESCO','INTL_GEODIVERSITY_DAY','International Geodiversity Day',10,6,NULL,NULL);
  check_observance('UNITED_NATIONS','INTL_DISASTER_RISK_REDUCTION_DAY','International Day for Disaster Risk Reduction',10,13,NULL,NULL);
  check_observance('UNESCO','INTL_INTANGIBLE_CULTURAL_HERITAGE_DAY','International Day of the Intangible Cultural Heritage',10,17,NULL,NULL);
  check_observance('UNESCO','INTL_AUDIOVISUAL_HERITAGE_DAY','World Day for Audiovisual Heritage',10,27,NULL,NULL);
  check_observance('UNESCO','INTL_WORLD_CODING_DAY','World Coding Day',10,29,NULL,NULL);
  check_observance('UNITED_NATIONS','INTL_END_IMPUNITY_JOURNALISTS_DAY','International Day to End Impunity for Crimes against Journalists',11,2,NULL,NULL);
  check_observance('UNESCO','INTL_BIOSPHERE_RESERVES_DAY','International Day for Biosphere Reserves',11,3,NULL,NULL);
  check_observance('UNESCO','INTL_ROMANI_LANGUAGE_DAY','World Day of Romani Language',11,5,NULL,NULL);
  check_observance('UNITED_NATIONS','INTL_TSUNAMI_AWARENESS_DAY','World Tsunami Awareness Day',11,5,NULL,NULL);
  check_observance('UNESCO','INTL_SCHOOL_ANTIBULLYING_DAY','International Day against Violence and Bullying at School, including Cyberbullying',11,NULL,4,1);
  check_observance('UNESCO','INTL_ILLICIT_CULTURAL_PROPERTY_DAY','International Day against Illicit Trafficking in Cultural Property',11,14,NULL,NULL);
  check_observance('UNESCO','INTL_TOLERANCE_DAY','International Day for Tolerance',11,16,NULL,NULL);
  check_observance('UNESCO','INTL_ISLAMIC_ART_DAY','International Day of Islamic Art',11,18,NULL,NULL);
  check_observance('UNESCO','INTL_WORLD_PHILOSOPHY_DAY','World Philosophy Day',11,NULL,4,3);
  check_observance('UNESCO','INTL_OLIVE_TREE_DAY','World Olive Tree Day',11,26,NULL,NULL);
  check_observance('UNESCO','INTL_SCIENCE_ENGAGEMENT_SUSTAINABLE_DAY','International Day of Engagement in Science for Sustainable Development',11,27,NULL,NULL);
  check_observance('UNITED_NATIONS','INTL_PALESTINIAN_SOLIDARITY_DAY','International Day of Solidarity with the Palestinian People',11,29,NULL,NULL);
  check_observance('UNESCO','INTL_WORLD_FUTURES_DAY','World Futures Day',12,2,NULL,NULL);
  check_observance('UNITED_NATIONS','INTL_MIGRANTS_DAY','International Migrants Day',12,18,NULL,NULL);
  check_observance('UNESCO','INTL_ARABIC_LANGUAGE_DAY','World Arabic Language Day',12,18,NULL,NULL);
  check_observance('WHO','INTL_NEGLECTED_TROPICAL_DISEASES_DAY','World Neglected Tropical Diseases Day',1,30,NULL,NULL);
  check_observance('WHO','INTL_TUBERCULOSIS_DAY','World TB Day',3,24,NULL,NULL);
  check_observance('WHO','INTL_CHAGAS_DISEASE_DAY','World Chagas Disease Day',4,14,NULL,NULL);
  check_observance('WHO','INTL_MALARIA_DAY','World Malaria Day',4,25,NULL,NULL);
  check_observance('WHO','INTL_DROWNING_PREVENTION_DAY','World Drowning Prevention Day',7,25,NULL,NULL);
  check_observance('WHO','INTL_HEPATITIS_DAY','World Hepatitis Day',7,28,NULL,NULL);
  check_observance('WHO','INTL_PATIENT_SAFETY_DAY','World Patient Safety Day',9,17,NULL,NULL);
  check_observance('WHO','INTL_PREMATURITY_DAY','World Prematurity Day',11,15,NULL,NULL);
  check_observance('WHO','INTL_CERVICAL_CANCER_ELIMINATION_DAY','World Cervical Cancer Elimination Day',11,17,NULL,NULL);

  SELECT COUNT(*) INTO v_dynamic FROM CAL2.EVENT_RECURRENCE_RULE R
    JOIN CAL2.EVENT E ON E.EVENT_ID=R.EVENT_ID
   WHERE E.EVENT_CODE IN ('INTL_WORLD_MARITIME_DAY','INTL_WORLD_PHILOSOPHY_DAY','INTL_SCHOOL_ANTIBULLYING_DAY')
     AND R.RULE_TYPE='ANNUAL_NTH_WEEKDAY' AND R.ACTIVE_FLAG='Y';
  SELECT COUNT(*) INTO v_maritime FROM CAL2.EVENT E
    JOIN CAL2.EVENT_RECURRENCE_RULE R ON E.EVENT_ID=R.EVENT_ID
   WHERE E.EVENT_CODE='INTL_WORLD_MARITIME_DAY' AND R.ACTIVE_FLAG='Y'
     AND R.RULE_TYPE='ANNUAL_NTH_WEEKDAY' AND R.MONTH_NO=9
     AND R.WEEKDAY_NO=4 AND R.WEEKDAY_ORDINAL=-1;
  SELECT COUNT(*) INTO v_rule_count FROM CAL2.EVENT_RECURRENCE_RULE WHERE ACTIVE_FLAG='Y';
  check_2026('INTL_WORLD_MARITIME_DAY','2026-09-24');
  check_2026('INTL_WORLD_PHILOSOPHY_DAY','2026-11-19');
  check_2026('INTL_SCHOOL_ANTIBULLYING_DAY','2026-11-05');

  DBMS_OUTPUT.PUT_LINE('B1_EXPECTED='||v_expected||' CORRECT_EVENTS='||v_correct_events||' CORRECT_RULES='||v_correct_rules);
  DBMS_OUTPUT.PUT_LINE('B1_EVENT_FAILURES='||v_missing||' B1_RULE_FAILURES='||v_mismatch);
  DBMS_OUTPUT.PUT_LINE('ACTIVE_RULES_TOTAL='||v_rule_count||' VARIABLE_RULES='||v_dynamic||' MARITIME_RULES='||v_maritime);
  DBMS_OUTPUT.PUT_LINE('VARIABLE_2026_CHECKS='||v_2026_checks||' FAIL='||v_2026_fail);
  DBMS_OUTPUT.PUT_LINE('B1_2026_OCCURRENCES_VERIFIED='||v_seed_2026_verified||'/72 SKIPPED_NO_DATE='||v_seed_2026_skipped||' FAIL='||v_seed_2026_fail);
  IF v_correct_events=0 AND v_correct_rules=0 THEN
    DBMS_OUTPUT.PUT_LINE('CAL2_B1_STATUS=NOT_APPLIED');
  ELSIF v_column1=1 AND v_column2=1 AND v_sources=3 AND v_correct_events=72 AND v_correct_rules=72
       AND v_dynamic=3 AND v_maritime=1 AND v_2026_fail=0 AND v_seed_2026_fail=0 THEN
    IF v_2026_checks=3 AND v_seed_2026_verified=72 THEN
      DBMS_OUTPUT.PUT_LINE('CAL2_B1_STATUS=FULLY_APPLIED');
    ELSE
      DBMS_OUTPUT.PUT_LINE('CAL2_B1_STATUS=DATA_APPLIED_OCCURRENCES_NOT_FULLY_TESTABLE');
    END IF;
  ELSE
    DBMS_OUTPUT.PUT_LINE('CAL2_B1_STATUS=PARTIAL_OR_INCONSISTENT');
  END IF;
END;
/
PROMPT === END CAL2 B1 READ ONLY verifier ===
