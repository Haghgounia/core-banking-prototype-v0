-- Core Banking Prototype 0.3.19
-- Party Operations Phase 8: align KYC_CASE with CIF-tables4.xlsx / supplied EA model.
-- The latest physical metadata has these nine fields but does not define Y/N checks
-- for HIGH_RISK_COUNTRY_FLAG and EDD_REQUIRED_FLAG; those flags are validated in the application.
DECLARE
  v_count NUMBER;
  PROCEDURE add_column_if_missing(p_column VARCHAR2, p_definition VARCHAR2) IS
  BEGIN
    SELECT COUNT(*) INTO v_count
      FROM ALL_TAB_COLUMNS
     WHERE OWNER='CIF' AND TABLE_NAME='KYC_CASE' AND COLUMN_NAME=p_column;
    IF v_count = 0 THEN
      EXECUTE IMMEDIATE 'ALTER TABLE CIF.KYC_CASE ADD ('||p_column||' '||p_definition||')';
    END IF;
  END;
BEGIN
  add_column_if_missing('RELATION_PURPOSE_CODE','VARCHAR2(30 CHAR)');
  add_column_if_missing('EXPECTED_ACTIVITY_LEVEL_CODE','VARCHAR2(30 CHAR)');
  add_column_if_missing('GEOGRAPHIC_SCOPE_CODE','VARCHAR2(30 CHAR)');
  add_column_if_missing('ACTIVITY_COUNTRIES_TEXT','VARCHAR2(500 CHAR)');
  add_column_if_missing('REQUESTED_PRODUCTS_TEXT','VARCHAR2(500 CHAR)');
  add_column_if_missing('PREFERRED_SERVICE_CHANNEL_CODE','VARCHAR2(30 CHAR)');
  add_column_if_missing('PEP_STATUS_CODE','VARCHAR2(30 CHAR)');
  add_column_if_missing('HIGH_RISK_COUNTRY_FLAG','CHAR(1 CHAR)');
  add_column_if_missing('EDD_REQUIRED_FLAG','CHAR(1 CHAR)');
END;
/

COMMENT ON COLUMN CIF.KYC_CASE.RELATION_PURPOSE_CODE IS 'هدف افتتاح رابطه';
COMMENT ON COLUMN CIF.KYC_CASE.EXPECTED_ACTIVITY_LEVEL_CODE IS 'حجم عملیات مورد انتظار';
COMMENT ON COLUMN CIF.KYC_CASE.GEOGRAPHIC_SCOPE_CODE IS 'محدوده جغرافیایی فعالیت';
COMMENT ON COLUMN CIF.KYC_CASE.ACTIVITY_COUNTRIES_TEXT IS 'کشورهای محل فعالیت';
COMMENT ON COLUMN CIF.KYC_CASE.REQUESTED_PRODUCTS_TEXT IS 'محصولات مورد تقاضا';
COMMENT ON COLUMN CIF.KYC_CASE.PREFERRED_SERVICE_CHANNEL_CODE IS 'کانال ترجیحی خدمت';
COMMENT ON COLUMN CIF.KYC_CASE.PEP_STATUS_CODE IS 'وضعیت PEP';
COMMENT ON COLUMN CIF.KYC_CASE.HIGH_RISK_COUNTRY_FLAG IS 'کشور پرریسک مرتبط';
COMMENT ON COLUMN CIF.KYC_CASE.EDD_REQUIRED_FLAG IS 'نیازمند بررسی مضاعف';
