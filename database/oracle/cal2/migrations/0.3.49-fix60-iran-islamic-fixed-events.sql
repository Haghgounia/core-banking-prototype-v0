-- FIX60 / 0.3.49
-- Seed fixed Hijri-lunar occasions commonly referenced in Iran's public calendar.
-- Idempotent: existing EVENT_CODE / annual fixed rule pairs are not duplicated.
-- Prerequisite: CAL2 base dataset including CALENDAR_SYSTEM='ISLAMIC' and at least one active Islamic CALENDAR_VARIANT.
SET SERVEROUTPUT ON
SET DEFINE OFF
WHENEVER SQLERROR EXIT SQL.SQLCODE

DECLARE
  v_event_type_id NUMBER;
  v_source_id NUMBER;
  v_variant_id NUMBER;
  v_event_id NUMBER;
  v_rule_id NUMBER;
  v_count NUMBER;
BEGIN
  BEGIN
    SELECT EVENT_TYPE_ID INTO v_event_type_id FROM CAL2.EVENT_TYPE WHERE EVENT_TYPE_CODE='RELIGIOUS' FETCH FIRST 1 ROW ONLY;
  EXCEPTION WHEN NO_DATA_FOUND THEN
    SELECT NVL(MAX(EVENT_TYPE_ID),0)+1 INTO v_event_type_id FROM CAL2.EVENT_TYPE;
    INSERT INTO CAL2.EVENT_TYPE(EVENT_TYPE_ID,EVENT_TYPE_CODE,NAME_FA,NAME_EN,ACTIVE_FLAG)
    VALUES(v_event_type_id,'RELIGIOUS','مذهبی','Religious','Y');
  END;

  BEGIN
    SELECT SOURCE_ID INTO v_source_id FROM CAL2.SOURCE_AUTHORITY WHERE SOURCE_CODE='IRAN_PUBLIC_CALENDAR' FETCH FIRST 1 ROW ONLY;
  EXCEPTION WHEN NO_DATA_FOUND THEN
    SELECT NVL(MAX(SOURCE_ID),0)+1 INTO v_source_id FROM CAL2.SOURCE_AUTHORITY;
    INSERT INTO CAL2.SOURCE_AUTHORITY(SOURCE_ID,SOURCE_CODE,NAME_FA,NAME_EN,SOURCE_TYPE,COUNTRY_CODE,AUTHORITY_LEVEL,ACTIVE_FLAG)
    VALUES(v_source_id,'IRAN_PUBLIC_CALENDAR','تقویم عمومی رسمی ایران','Iran Public Calendar','GOVERNMENT','IR','OFFICIAL','Y');
  END;

  BEGIN
    SELECT CALENDAR_VARIANT_ID INTO v_variant_id
      FROM CAL2.CALENDAR_VARIANT V
      JOIN CAL2.CALENDAR_SYSTEM S ON S.CALENDAR_SYSTEM_ID=V.CALENDAR_SYSTEM_ID
     WHERE S.CALENDAR_CODE='ISLAMIC' AND V.ACTIVE_FLAG='Y'
     ORDER BY CASE WHEN V.IS_DEFAULT='Y' THEN 0 ELSE 1 END, V.CALENDAR_VARIANT_ID
     FETCH FIRST 1 ROW ONLY;
  EXCEPTION WHEN NO_DATA_FOUND THEN
    RAISE_APPLICATION_ERROR(-20060,'No active CAL2 Islamic calendar variant exists. Import CAL2 dataset first.');
  END;

  FOR r IN (
    SELECT 'IR_HIJRI_NEW_YEAR' code, 'آغاز سال هجری قمری' fa, 'Islamic New Year' en, 1 m, 1 d, 'N' hol FROM dual UNION ALL
    SELECT 'IR_TASUA', 'تاسوعای حسینی', 'Tasu''a', 1, 9, 'Y' FROM dual UNION ALL
    SELECT 'IR_ASHURA', 'عاشورای حسینی', 'Ashura', 1, 10, 'Y' FROM dual UNION ALL
    SELECT 'IR_ARBAEEN', 'اربعین حسینی', 'Arbaeen', 2, 20, 'Y' FROM dual UNION ALL
    SELECT 'IR_PROPHET_DEMISE_IMAM_HASAN', 'رحلت پیامبر اکرم (ص) و شهادت امام حسن مجتبی (ع)', 'Demise of Prophet Muhammad and Martyrdom of Imam Hasan', 2, 28, 'Y' FROM dual UNION ALL
    SELECT 'IR_IMAM_REZA_MARTYRDOM', 'شهادت امام رضا (ع)', 'Martyrdom of Imam Reza', 2, 30, 'Y' FROM dual UNION ALL
    SELECT 'IR_IMAM_ASKARI_MARTYRDOM', 'شهادت امام حسن عسکری (ع)', 'Martyrdom of Imam Hasan al-Askari', 3, 8, 'Y' FROM dual UNION ALL
    SELECT 'IR_IMAM_MAHDI_IMAMATE', 'آغاز امامت حضرت ولی‌عصر (عج)', 'Beginning of Imam Mahdi''s Imamate', 3, 9, 'N' FROM dual UNION ALL
    SELECT 'IR_PROPHET_IMAM_SADIQ_BIRTH', 'میلاد پیامبر اکرم (ص) و امام جعفر صادق (ع)', 'Birth of Prophet Muhammad and Imam Jafar al-Sadiq', 3, 17, 'Y' FROM dual UNION ALL
    SELECT 'IR_FATIMA_MASUMEH_DEMISE', 'وفات حضرت فاطمه معصومه (س)', 'Demise of Fatima Masumeh', 4, 10, 'N' FROM dual UNION ALL
    SELECT 'IR_ZEINAB_BIRTH', 'میلاد حضرت زینب (س) و روز پرستار', 'Birth of Zaynab and Nurses Day', 5, 5, 'N' FROM dual UNION ALL
    SELECT 'IR_FATIMA_MARTYRDOM', 'شهادت حضرت فاطمه زهرا (س)', 'Martyrdom of Fatima al-Zahra', 6, 3, 'Y' FROM dual UNION ALL
    SELECT 'IR_FATIMA_BIRTH', 'میلاد حضرت فاطمه زهرا (س) و روز زن', 'Birth of Fatima al-Zahra and Women''s Day', 6, 20, 'N' FROM dual UNION ALL
    SELECT 'IR_IMAM_BAQIR_BIRTH', 'میلاد امام محمد باقر (ع)', 'Birth of Imam Muhammad al-Baqir', 7, 1, 'N' FROM dual UNION ALL
    SELECT 'IR_IMAM_HADI_MARTYRDOM', 'شهادت امام علی النقی الهادی (ع)', 'Martyrdom of Imam al-Hadi', 7, 3, 'N' FROM dual UNION ALL
    SELECT 'IR_IMAM_JAWAD_BIRTH', 'میلاد امام محمد تقی (ع)', 'Birth of Imam Muhammad al-Jawad', 7, 10, 'N' FROM dual UNION ALL
    SELECT 'IR_IMAM_ALI_BIRTH', 'میلاد امام علی (ع) و روز پدر', 'Birth of Imam Ali and Fathers Day', 7, 13, 'Y' FROM dual UNION ALL
    SELECT 'IR_IMAM_KAZIM_MARTYRDOM', 'شهادت امام موسی کاظم (ع)', 'Martyrdom of Imam Musa al-Kazim', 7, 25, 'N' FROM dual UNION ALL
    SELECT 'IR_MABATH', 'مبعث رسول اکرم (ص)', 'Mabath', 7, 27, 'Y' FROM dual UNION ALL
    SELECT 'IR_IMAM_MAHDI_BIRTH', 'میلاد حضرت قائم (عج)', 'Birth of Imam Mahdi', 8, 15, 'Y' FROM dual UNION ALL
    SELECT 'IR_IMAM_ALI_MARTYRDOM', 'شهادت امام علی (ع)', 'Martyrdom of Imam Ali', 9, 21, 'Y' FROM dual UNION ALL
    SELECT 'IR_EID_FITR', 'عید سعید فطر', 'Eid al-Fitr', 10, 1, 'Y' FROM dual UNION ALL
    SELECT 'IR_EID_FITR_SECOND_DAY', 'تعطیل عید سعید فطر', 'Second day of Eid al-Fitr holiday', 10, 2, 'Y' FROM dual UNION ALL
    SELECT 'IR_IMAM_SADIQ_MARTYRDOM', 'شهادت امام جعفر صادق (ع)', 'Martyrdom of Imam Jafar al-Sadiq', 10, 25, 'Y' FROM dual UNION ALL
    SELECT 'IR_IMAM_REZA_BIRTH', 'میلاد امام رضا (ع)', 'Birth of Imam Reza', 11, 11, 'N' FROM dual UNION ALL
    SELECT 'IR_ARAFAT', 'روز عرفه', 'Day of Arafah', 12, 9, 'N' FROM dual UNION ALL
    SELECT 'IR_EID_ADHA', 'عید سعید قربان', 'Eid al-Adha', 12, 10, 'Y' FROM dual UNION ALL
    SELECT 'IR_EID_GHADIR', 'عید سعید غدیر خم', 'Eid al-Ghadir', 12, 18, 'Y' FROM dual UNION ALL
    SELECT 'IR_MUBAHALA', 'روز مباهله پیامبر اسلام (ص)', 'Day of Mubahala', 12, 24, 'N' FROM dual
  ) LOOP
    BEGIN
      SELECT EVENT_ID INTO v_event_id FROM CAL2.EVENT WHERE EVENT_CODE=r.code;
    EXCEPTION WHEN NO_DATA_FOUND THEN
      SELECT NVL(MAX(EVENT_ID),0)+1 INTO v_event_id FROM CAL2.EVENT;
      INSERT INTO CAL2.EVENT(EVENT_ID,EVENT_CODE,EVENT_TYPE_ID,NAME_FA,NAME_EN,DESCRIPTION,RECURRENCE_TYPE,BASE_CALENDAR_SYSTEM_ID,OFFICIAL_FLAG,DEFAULT_HOLIDAY_FLAG,ACTIVE_FLAG)
      SELECT v_event_id,r.code,v_event_type_id,r.fa,r.en,'مناسبت ثابت هجری قمری مورد استفاده در تقویم عمومی ایران','ANNUAL_FIXED_DATE',S.CALENDAR_SYSTEM_ID,'Y',r.hol,'Y'
        FROM CAL2.CALENDAR_SYSTEM S WHERE S.CALENDAR_CODE='ISLAMIC';
    END;

    SELECT COUNT(*) INTO v_count FROM CAL2.EVENT_RECURRENCE_RULE WHERE EVENT_ID=v_event_id AND CALENDAR_VARIANT_ID=v_variant_id AND RULE_TYPE='ANNUAL_FIXED_DATE' AND MONTH_NO=r.m AND DAY_NO=r.d;
    IF v_count = 0 THEN
      SELECT NVL(MAX(EVENT_RULE_ID),0)+1 INTO v_rule_id FROM CAL2.EVENT_RECURRENCE_RULE;
      INSERT INTO CAL2.EVENT_RECURRENCE_RULE(EVENT_RULE_ID,EVENT_ID,RULE_TYPE,CALENDAR_VARIANT_ID,YEAR_NO,MONTH_NO,DAY_NO,START_YEAR_NO,END_YEAR_NO,SOURCE_ID,DESCRIPTION,ACTIVE_FLAG)
      VALUES(v_rule_id,v_event_id,'ANNUAL_FIXED_DATE',v_variant_id,NULL,r.m,r.d,NULL,NULL,v_source_id,'قاعده سالانه ثابت هجری قمری برای تقویم ایران','Y');
    END IF;
  END LOOP;
  COMMIT;
  DBMS_OUTPUT.PUT_LINE('FIX60: Iranian fixed Islamic events seeded successfully.');
END;
/

PROMPT Verification
SELECT E.EVENT_CODE,E.NAME_FA,R.MONTH_NO,R.DAY_NO,E.DEFAULT_HOLIDAY_FLAG,S.NAME_FA AS CALENDAR_NAME
  FROM CAL2.EVENT E
  JOIN CAL2.EVENT_RECURRENCE_RULE R ON R.EVENT_ID=E.EVENT_ID
  JOIN CAL2.CALENDAR_VARIANT V ON V.CALENDAR_VARIANT_ID=R.CALENDAR_VARIANT_ID
  JOIN CAL2.CALENDAR_SYSTEM S ON S.CALENDAR_SYSTEM_ID=V.CALENDAR_SYSTEM_ID
 WHERE E.EVENT_CODE LIKE 'IR_%' AND S.CALENDAR_CODE='ISLAMIC'
 ORDER BY R.MONTH_NO,R.DAY_NO,E.EVENT_CODE;
