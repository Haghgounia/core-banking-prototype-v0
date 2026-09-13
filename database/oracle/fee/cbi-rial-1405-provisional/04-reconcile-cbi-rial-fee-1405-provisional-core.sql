-- ============================================================================
-- CBI Rial Banking Fees 1405 - PROVISIONAL source-to-Oracle reconciliation
-- Workbook SHA256: bbe79ea223fabbfcddd83a402a70bc87f9c3fb3ad3d0de01369155240e4bd15e
-- PDF SHA256     : ea2e6a22e3b92b7ea6ef5da8cd6bfa20f918d0fc069c2e1bd46b89167a993e14
-- Generated from the same normalized source contract as the FIX98 importer.
-- Core reconciliation contract. Wrapper controls transaction/error behavior.
-- ============================================================================

DECLARE
  v_errors NUMBER := 0;
  v_tariffs_checked NUMBER := 0;
  v_components_checked NUMBER := 0;
  v_inputs_checked NUMBER := 0;
  v_tiers_checked NUMBER := 0;

  FUNCTION same_text(a VARCHAR2, b VARCHAR2) RETURN BOOLEAN IS
  BEGIN
    IF a IS NULL AND b IS NULL THEN RETURN TRUE; END IF;
    IF a IS NULL OR b IS NULL THEN RETURN FALSE; END IF;
    RETURN a = b;
  END;

  FUNCTION same_num(a NUMBER, b NUMBER) RETURN BOOLEAN IS
  BEGIN
    IF a IS NULL AND b IS NULL THEN RETURN TRUE; END IF;
    IF a IS NULL OR b IS NULL THEN RETURN FALSE; END IF;
    RETURN a = b;
  END;

  FUNCTION same_date(a DATE, b DATE) RETURN BOOLEAN IS
  BEGIN
    IF a IS NULL AND b IS NULL THEN RETURN TRUE; END IF;
    IF a IS NULL OR b IS NULL THEN RETURN FALSE; END IF;
    RETURN a = b;
  END;

  FUNCTION show_text(v VARCHAR2) RETURN VARCHAR2 IS
  BEGIN RETURN CASE WHEN v IS NULL THEN '<NULL>' ELSE SUBSTR(v,1,900) END; END;

  FUNCTION show_num(v NUMBER) RETURN VARCHAR2 IS
  BEGIN RETURN CASE WHEN v IS NULL THEN '<NULL>' ELSE TO_CHAR(v) END; END;

  PROCEDURE fail(p_scope VARCHAR2, p_key VARCHAR2, p_field VARCHAR2, p_expected VARCHAR2, p_actual VARCHAR2) IS
  BEGIN
    v_errors := v_errors + 1;
    DBMS_OUTPUT.PUT_LINE('[MISMATCH] '||p_scope||' '||p_key||' '||p_field||' expected='||show_text(p_expected)||' actual='||show_text(p_actual));
  END;

  PROCEDURE check_text(p_scope VARCHAR2, p_key VARCHAR2, p_field VARCHAR2, p_expected VARCHAR2, p_actual VARCHAR2) IS
  BEGIN IF NOT same_text(p_expected,p_actual) THEN fail(p_scope,p_key,p_field,p_expected,p_actual); END IF; END;

  PROCEDURE check_num(p_scope VARCHAR2, p_key VARCHAR2, p_field VARCHAR2, p_expected NUMBER, p_actual NUMBER) IS
  BEGIN IF NOT same_num(p_expected,p_actual) THEN fail(p_scope,p_key,p_field,show_num(p_expected),show_num(p_actual)); END IF; END;

  PROCEDURE check_date(p_scope VARCHAR2, p_key VARCHAR2, p_field VARCHAR2, p_expected DATE, p_actual DATE) IS
  BEGIN IF NOT same_date(p_expected,p_actual) THEN fail(p_scope,p_key,p_field,CASE WHEN p_expected IS NULL THEN '<NULL>' ELSE TO_CHAR(p_expected,'YYYY-MM-DD') END,CASE WHEN p_actual IS NULL THEN '<NULL>' ELSE TO_CHAR(p_actual,'YYYY-MM-DD') END); END IF; END;

  PROCEDURE check_tariff(
    p_fee_code VARCHAR2, p_tariff_code VARCHAR2, p_rule_code VARCHAR2, p_name_fa VARCHAR2,
    p_feature_code VARCHAR2, p_category_code VARCHAR2, p_config_hash VARCHAR2,
    p_strategy VARCHAR2, p_basis VARCHAR2, p_fixed NUMBER, p_rate NUMBER,
    p_min NUMBER, p_max NUMBER, p_period VARCHAR2, p_component_count NUMBER
  ) IS
    a_name FEE.FEE_DEFINITION.NAME_FA%TYPE; a_feature FEE.FEE_FEATURE.FEATURE_CODE%TYPE; a_category FEE.FEE_DEFINITION.CATEGORY_CODE%TYPE; a_class FEE.FEE_DEFINITION.CLASSIFICATION_CODE%TYPE;
    a_tariff FEE.FEE_DEFINITION_VERSION.REGULATORY_TARIFF_CODE%TYPE; a_version FEE.FEE_DEFINITION_VERSION.VERSION_NO%TYPE; a_status FEE.FEE_DEFINITION_VERSION.STATUS_CODE%TYPE; a_hash FEE.FEE_DEFINITION_VERSION.CONFIG_HASH%TYPE;
    a_source FEE.FEE_REGULATORY_SOURCE.SOURCE_CODE%TYPE; a_policy FEE.FEE_POLICY_SET.POLICY_CODE%TYPE; a_rule FEE.FEE_CALCULATION_RULE.RULE_CODE%TYPE; a_strategy FEE.FEE_CALCULATION_RULE.CALCULATION_STRATEGY_CODE%TYPE; a_basis FEE.FEE_CALCULATION_RULE.BASIS_TYPE_CODE%TYPE;
    a_fixed NUMBER; a_rate NUMBER; a_min NUMBER; a_max NUMBER; a_period FEE.FEE_CALCULATION_RULE.RATE_PERIOD_CODE%TYPE;
    a_currency FEE.FEE_CALCULATION_RULE.CURRENCY_CODE%TYPE; a_active FEE.FEE_CALCULATION_RULE.IS_ACTIVE%TYPE; a_from DATE; a_to DATE; a_components NUMBER;
  BEGIN
    v_tariffs_checked := v_tariffs_checked + 1;
    BEGIN
      SELECT d.NAME_FA,f.FEATURE_CODE,d.CATEGORY_CODE,d.CLASSIFICATION_CODE,
             dv.REGULATORY_TARIFF_CODE,dv.VERSION_NO,dv.STATUS_CODE,dv.CONFIG_HASH,
             rs.SOURCE_CODE,ps.POLICY_CODE,r.RULE_CODE,r.CALCULATION_STRATEGY_CODE,r.BASIS_TYPE_CODE,
             r.FIXED_AMOUNT,r.RATE_VALUE,r.MIN_FEE_AMOUNT,r.MAX_FEE_AMOUNT,r.RATE_PERIOD_CODE,
             r.CURRENCY_CODE,r.IS_ACTIVE,r.EFFECTIVE_FROM,r.EFFECTIVE_TO,
             (SELECT COUNT(*) FROM FEE_RULE_COMPONENT c WHERE c.CALCULATION_RULE_ID=r.CALCULATION_RULE_ID AND c.REFERENCE_CODE LIKE 'SRC:%')
        INTO a_name,a_feature,a_category,a_class,a_tariff,a_version,a_status,a_hash,a_source,a_policy,a_rule,a_strategy,a_basis,
             a_fixed,a_rate,a_min,a_max,a_period,a_currency,a_active,a_from,a_to,a_components
        FROM FEE_DEFINITION d
        JOIN FEE_FEATURE f ON f.FEE_FEATURE_ID=d.FEE_FEATURE_ID
        JOIN FEE_DEFINITION_VERSION dv ON dv.FEE_DEFINITION_ID=d.FEE_DEFINITION_ID
        JOIN FEE_REGULATORY_SOURCE rs ON rs.REGULATORY_SOURCE_ID=dv.REGULATORY_SOURCE_ID
        JOIN FEE_POLICY_VERSION pv ON pv.POLICY_VERSION_ID=dv.POLICY_VERSION_ID
        JOIN FEE_POLICY_SET ps ON ps.POLICY_SET_ID=pv.POLICY_SET_ID
        JOIN FEE_CALCULATION_RULE r ON r.FEE_DEFINITION_VERSION_ID=dv.FEE_DEFINITION_VERSION_ID
       WHERE d.FEE_CODE=p_fee_code AND dv.VERSION_NO='1405.P1' AND r.RULE_CODE=p_rule_code;
    EXCEPTION
      WHEN NO_DATA_FOUND THEN fail('TARIFF',p_fee_code,'ROW','present','missing'); RETURN;
      WHEN TOO_MANY_ROWS THEN fail('TARIFF',p_fee_code,'ROW','one row','multiple rows'); RETURN;
    END;
    check_text('TARIFF',p_fee_code,'NAME_FA',p_name_fa,a_name);
    check_text('TARIFF',p_fee_code,'FEATURE_CODE',p_feature_code,a_feature);
    check_text('TARIFF',p_fee_code,'CATEGORY_CODE',p_category_code,a_category);
    check_text('TARIFF',p_fee_code,'CLASSIFICATION_CODE','CBI_1405_RIAL_PROVISIONAL',a_class);
    check_text('TARIFF',p_fee_code,'REGULATORY_TARIFF_CODE',p_tariff_code,a_tariff);
    check_text('TARIFF',p_fee_code,'VERSION_NO','1405.P1',a_version);
    check_text('TARIFF',p_fee_code,'STATUS_CODE','ACTIVE',a_status);
    check_text('TARIFF',p_fee_code,'CONFIG_HASH',p_config_hash,a_hash);
    check_text('TARIFF',p_fee_code,'SOURCE_CODE','CBI_RIAL_FEE_1405_PROVISIONAL',a_source);
    check_text('TARIFF',p_fee_code,'POLICY_CODE','CBI_RIAL_BANKING_1405_PROVISIONAL',a_policy);
    check_text('TARIFF',p_fee_code,'RULE_CODE',p_rule_code,a_rule);
    check_text('TARIFF',p_fee_code,'STRATEGY',p_strategy,a_strategy);
    check_text('TARIFF',p_fee_code,'BASIS_TYPE',p_basis,a_basis);
    check_num('TARIFF',p_fee_code,'FIXED_AMOUNT',p_fixed,a_fixed);
    check_num('TARIFF',p_fee_code,'RATE_VALUE',p_rate,a_rate);
    check_num('TARIFF',p_fee_code,'MIN_FEE_AMOUNT',p_min,a_min);
    check_num('TARIFF',p_fee_code,'MAX_FEE_AMOUNT',p_max,a_max);
    check_text('TARIFF',p_fee_code,'RATE_PERIOD_CODE',p_period,a_period);
    check_text('TARIFF',p_fee_code,'CURRENCY_CODE','IRR',a_currency);
    check_text('TARIFF',p_fee_code,'IS_ACTIVE','Y',a_active);
    check_date('TARIFF',p_fee_code,'EFFECTIVE_FROM',DATE '2026-09-10',a_from);
    check_date('TARIFF',p_fee_code,'EFFECTIVE_TO',NULL,a_to);
    check_num('TARIFF',p_fee_code,'SOURCE_COMPONENT_COUNT',p_component_count,a_components);
  END;

  PROCEDURE check_component(p_fee_code VARCHAR2, p_sequence NUMBER, p_node VARCHAR2, p_number NUMBER, p_text VARCHAR2, p_ref VARCHAR2, p_desc VARCHAR2) IS
    a_node FEE.FEE_RULE_COMPONENT.NODE_TYPE_CODE%TYPE; a_number NUMBER; a_text FEE.FEE_RULE_COMPONENT.CONSTANT_TEXT%TYPE; a_ref FEE.FEE_RULE_COMPONENT.REFERENCE_CODE%TYPE; a_desc FEE.FEE_RULE_COMPONENT.DESCRIPTION%TYPE;
    a_parent NUMBER; a_operator FEE.FEE_RULE_COMPONENT.OPERATOR_CODE%TYPE; a_input FEE.FEE_RULE_COMPONENT.INPUT_CODE%TYPE;
    k VARCHAR2(200) := p_fee_code||'#'||p_sequence;
  BEGIN
    v_components_checked := v_components_checked + 1;
    BEGIN
      SELECT c.NODE_TYPE_CODE,c.CONSTANT_NUMBER,c.CONSTANT_TEXT,c.REFERENCE_CODE,c.DESCRIPTION,c.PARENT_RULE_COMPONENT_ID,c.OPERATOR_CODE,c.INPUT_CODE
        INTO a_node,a_number,a_text,a_ref,a_desc,a_parent,a_operator,a_input
        FROM FEE_RULE_COMPONENT c
        JOIN FEE_CALCULATION_RULE r ON r.CALCULATION_RULE_ID=c.CALCULATION_RULE_ID
        JOIN FEE_DEFINITION_VERSION dv ON dv.FEE_DEFINITION_VERSION_ID=r.FEE_DEFINITION_VERSION_ID
        JOIN FEE_DEFINITION d ON d.FEE_DEFINITION_ID=dv.FEE_DEFINITION_ID
       WHERE d.FEE_CODE=p_fee_code AND dv.VERSION_NO='1405.P1' AND c.SEQUENCE_NO=p_sequence;
    EXCEPTION
      WHEN NO_DATA_FOUND THEN fail('COMPONENT',k,'ROW','present','missing'); RETURN;
      WHEN TOO_MANY_ROWS THEN fail('COMPONENT',k,'ROW','one row','multiple rows'); RETURN;
    END;
    check_text('COMPONENT',k,'NODE_TYPE_CODE',p_node,a_node);
    check_num('COMPONENT',k,'CONSTANT_NUMBER',p_number,a_number);
    check_text('COMPONENT',k,'CONSTANT_TEXT',p_text,a_text);
    check_text('COMPONENT',k,'REFERENCE_CODE',p_ref,a_ref);
    check_text('COMPONENT',k,'DESCRIPTION',p_desc,a_desc);
    IF a_parent IS NOT NULL THEN fail('COMPONENT',k,'PARENT_RULE_COMPONENT_ID','<NULL>',show_num(a_parent)); END IF;
    check_text('COMPONENT',k,'OPERATOR_CODE',NULL,a_operator);
    check_text('COMPONENT',k,'INPUT_CODE',NULL,a_input);
  END;

  PROCEDURE check_input(p_fee_code VARCHAR2, p_input_code VARCHAR2, p_name VARCHAR2, p_unit VARCHAR2, p_order NUMBER) IS
    a_name FEE.FEE_INPUT_DEFINITION.NAME_FA%TYPE; a_type FEE.FEE_INPUT_DEFINITION.DATA_TYPE_CODE%TYPE; a_unit FEE.FEE_INPUT_DEFINITION.UNIT_CODE%TYPE; a_mand FEE.FEE_INPUT_DEFINITION.MANDATORY_FLAG%TYPE; a_order NUMBER;
    k VARCHAR2(200) := p_fee_code||'#'||p_input_code;
  BEGIN
    v_inputs_checked := v_inputs_checked + 1;
    BEGIN
      SELECT i.NAME_FA,i.DATA_TYPE_CODE,i.UNIT_CODE,i.MANDATORY_FLAG,i.DISPLAY_ORDER
        INTO a_name,a_type,a_unit,a_mand,a_order
        FROM FEE_INPUT_DEFINITION i
        JOIN FEE_CALCULATION_RULE r ON r.CALCULATION_RULE_ID=i.CALCULATION_RULE_ID
        JOIN FEE_DEFINITION_VERSION dv ON dv.FEE_DEFINITION_VERSION_ID=r.FEE_DEFINITION_VERSION_ID
        JOIN FEE_DEFINITION d ON d.FEE_DEFINITION_ID=dv.FEE_DEFINITION_ID
       WHERE d.FEE_CODE=p_fee_code AND dv.VERSION_NO='1405.P1' AND i.INPUT_CODE=p_input_code;
    EXCEPTION
      WHEN NO_DATA_FOUND THEN fail('INPUT',k,'ROW','present','missing'); RETURN;
      WHEN TOO_MANY_ROWS THEN fail('INPUT',k,'ROW','one row','multiple rows'); RETURN;
    END;
    check_text('INPUT',k,'NAME_FA',p_name,a_name);
    check_text('INPUT',k,'DATA_TYPE_CODE','NUMBER',a_type);
    check_text('INPUT',k,'UNIT_CODE',p_unit,a_unit);
    check_text('INPUT',k,'MANDATORY_FLAG','Y',a_mand);
    check_num('INPUT',k,'DISPLAY_ORDER',p_order,a_order);
  END;

  PROCEDURE check_tier(p_fee_code VARCHAR2, p_tier_no NUMBER, p_name VARCHAR2, p_lower NUMBER, p_upper NUMBER, p_basis VARCHAR2, p_strategy VARCHAR2, p_fixed NUMBER, p_rate NUMBER, p_min NUMBER, p_max NUMBER) IS
    a_name FEE.FEE_CALCULATION_TIER.TIER_NAME_FA%TYPE; a_lower NUMBER; a_upper NUMBER; a_unit FEE.FEE_CALCULATION_TIER.BOUND_UNIT_CODE%TYPE; a_basis FEE.FEE_CALCULATION_TIER.TIER_BASIS_CODE%TYPE; a_strategy FEE.FEE_CALCULATION_TIER.TIER_STRATEGY_CODE%TYPE;
    a_fixed NUMBER; a_rate NUMBER; a_min NUMBER; a_max NUMBER; a_from DATE; a_to DATE;
    k VARCHAR2(200) := p_fee_code||'#TIER'||p_tier_no;
  BEGIN
    v_tiers_checked := v_tiers_checked + 1;
    BEGIN
      SELECT tr.TIER_NAME_FA,tr.LOWER_BOUND,tr.UPPER_BOUND,tr.BOUND_UNIT_CODE,tr.TIER_BASIS_CODE,tr.TIER_STRATEGY_CODE,
             tr.FIXED_AMOUNT,tr.RATE_VALUE,tr.MIN_FEE_AMOUNT,tr.MAX_FEE_AMOUNT,tr.EFFECTIVE_FROM,tr.EFFECTIVE_TO
        INTO a_name,a_lower,a_upper,a_unit,a_basis,a_strategy,a_fixed,a_rate,a_min,a_max,a_from,a_to
        FROM FEE_CALCULATION_TIER tr
        JOIN FEE_CALCULATION_RULE r ON r.CALCULATION_RULE_ID=tr.CALCULATION_RULE_ID
        JOIN FEE_DEFINITION_VERSION dv ON dv.FEE_DEFINITION_VERSION_ID=r.FEE_DEFINITION_VERSION_ID
        JOIN FEE_DEFINITION d ON d.FEE_DEFINITION_ID=dv.FEE_DEFINITION_ID
       WHERE d.FEE_CODE=p_fee_code AND dv.VERSION_NO='1405.P1' AND tr.TIER_NO=p_tier_no;
    EXCEPTION
      WHEN NO_DATA_FOUND THEN fail('TIER',k,'ROW','present','missing'); RETURN;
      WHEN TOO_MANY_ROWS THEN fail('TIER',k,'ROW','one row','multiple rows'); RETURN;
    END;
    check_text('TIER',k,'TIER_NAME_FA',p_name,a_name);
    check_num('TIER',k,'LOWER_BOUND',p_lower,a_lower);
    check_num('TIER',k,'UPPER_BOUND',p_upper,a_upper);
    check_text('TIER',k,'BOUND_UNIT_CODE','IRR',a_unit);
    check_text('TIER',k,'TIER_BASIS_CODE',p_basis,a_basis);
    check_text('TIER',k,'TIER_STRATEGY_CODE',p_strategy,a_strategy);
    check_num('TIER',k,'FIXED_AMOUNT',p_fixed,a_fixed);
    check_num('TIER',k,'RATE_VALUE',p_rate,a_rate);
    check_num('TIER',k,'MIN_FEE_AMOUNT',p_min,a_min);
    check_num('TIER',k,'MAX_FEE_AMOUNT',p_max,a_max);
    check_date('TIER',k,'EFFECTIVE_FROM',DATE '2026-09-10',a_from);
    check_date('TIER',k,'EFFECTIVE_TO',NULL,a_to);
  END;

  PROCEDURE check_global_counts IS
    n NUMBER;
  BEGIN
    SELECT COUNT(*) INTO n FROM FEE_DEFINITION WHERE CLASSIFICATION_CODE='CBI_1405_RIAL_PROVISIONAL' AND IS_ACTIVE='Y';
    check_num('GLOBAL','CBI1405','DEFINITIONS',152,n);
    SELECT COUNT(*) INTO n FROM FEE_CALCULATION_RULE r JOIN FEE_DEFINITION_VERSION dv ON dv.FEE_DEFINITION_VERSION_ID=r.FEE_DEFINITION_VERSION_ID JOIN FEE_DEFINITION d ON d.FEE_DEFINITION_ID=dv.FEE_DEFINITION_ID WHERE d.CLASSIFICATION_CODE='CBI_1405_RIAL_PROVISIONAL' AND r.IS_ACTIVE='Y';
    check_num('GLOBAL','CBI1405','RULES',152,n);
    SELECT COUNT(*) INTO n FROM FEE_RULE_COMPONENT c JOIN FEE_CALCULATION_RULE r ON r.CALCULATION_RULE_ID=c.CALCULATION_RULE_ID JOIN FEE_DEFINITION_VERSION dv ON dv.FEE_DEFINITION_VERSION_ID=r.FEE_DEFINITION_VERSION_ID JOIN FEE_DEFINITION d ON d.FEE_DEFINITION_ID=dv.FEE_DEFINITION_ID WHERE d.CLASSIFICATION_CODE='CBI_1405_RIAL_PROVISIONAL' AND c.REFERENCE_CODE LIKE 'SRC:%';
    check_num('GLOBAL','CBI1405','SOURCE_COMPONENTS',180,n);
    SELECT COUNT(*) INTO n FROM FEE_INPUT_DEFINITION i JOIN FEE_CALCULATION_RULE r ON r.CALCULATION_RULE_ID=i.CALCULATION_RULE_ID JOIN FEE_DEFINITION_VERSION dv ON dv.FEE_DEFINITION_VERSION_ID=r.FEE_DEFINITION_VERSION_ID JOIN FEE_DEFINITION d ON d.FEE_DEFINITION_ID=dv.FEE_DEFINITION_ID WHERE d.CLASSIFICATION_CODE='CBI_1405_RIAL_PROVISIONAL';
    check_num('GLOBAL','CBI1405','INPUT_DEFINITIONS',66,n);
    SELECT COUNT(*) INTO n FROM FEE_CALCULATION_TIER tr JOIN FEE_CALCULATION_RULE r ON r.CALCULATION_RULE_ID=tr.CALCULATION_RULE_ID JOIN FEE_DEFINITION_VERSION dv ON dv.FEE_DEFINITION_VERSION_ID=r.FEE_DEFINITION_VERSION_ID JOIN FEE_DEFINITION d ON d.FEE_DEFINITION_ID=dv.FEE_DEFINITION_ID WHERE d.CLASSIFICATION_CODE='CBI_1405_RIAL_PROVISIONAL';
    check_num('GLOBAL','CBI1405','TIERS',15,n);
  END;

  PROCEDURE check_source_hashes IS
    a_ref VARCHAR2(500); a_hash VARCHAR2(200); a_status VARCHAR2(30);
  BEGIN
    SELECT DOCUMENT_REF,DOCUMENT_HASH,STATUS_CODE INTO a_ref,a_hash,a_status FROM FEE_REGULATORY_SOURCE WHERE SOURCE_CODE='CBI_RIAL_FEE_1405_PROVISIONAL';
    IF INSTR(a_ref,'XLSX_SHA256=bbe79ea223fabbfcddd83a402a70bc87f9c3fb3ad3d0de01369155240e4bd15e')=0 THEN fail('SOURCE','CBI_RIAL_FEE_1405_PROVISIONAL','XLSX_SHA256','bbe79ea223fabbfcddd83a402a70bc87f9c3fb3ad3d0de01369155240e4bd15e',a_ref); END IF;
    check_text('SOURCE','CBI_RIAL_FEE_1405_PROVISIONAL','PDF_SHA256','SHA256:ea2e6a22e3b92b7ea6ef5da8cd6bfa20f918d0fc069c2e1bd46b89167a993e14',a_hash);
    check_text('SOURCE','CBI_RIAL_FEE_1405_PROVISIONAL','STATUS_CODE','PROVISIONAL',a_status);
  EXCEPTION WHEN NO_DATA_FOUND THEN fail('SOURCE','CBI_RIAL_FEE_1405_PROVISIONAL','ROW','present','missing');
  END;

BEGIN
  DBMS_OUTPUT.PUT_LINE('=== CBI 1405 PROVISIONAL SOURCE -> ORACLE RECONCILIATION ===');
  check_source_hashes;
  check_global_counts;

  DBMS_OUTPUT.PUT_LINE('--- Checking 152 tariff contracts ---');
  check_tariff('CBI1405R_GAR_1_1','1-1','TARIFF_1_1','ضمانتنامه در مقابل 100 درصد سپرده نقدی','CBI_RIAL_1405_GUARANTEE_GROUP','CBI1405R_GAR','c86c771375a21fae9b4b364a0171fbb6248dc271d448122341995a53a73af413','FIXED','Flat',1300000,NULL,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_GAR_1_2','1-2','TARIFF_1_2','ضمانتنامه در مقابل انواع حساب های سپرده اعم از قرض الحسنه پس انداز و سرمایه گذاری مدت دار (ریالی و ارزی)','CBI_RIAL_1405_GUARANTEE_GROUP','CBI1405R_GAR','5fb51f39f82cd2d44e83b46267a4209e35906b0ac5b2f84822b7c675f322e6f4','ANNUALIZED_PERCENTAGE','RateWithMinimumAmount',NULL,0.005,1300000,NULL,'YEAR',1);
  check_tariff('CBI1405R_GAR_1_3','1-3','TARIFF_1_3','ضمانتنامه در مقابل طلا (1)','CBI_RIAL_1405_GUARANTEE_GROUP','CBI1405R_GAR','618253e38b98daef0fec577dd71fe903d95f3edb8ea69dab7a48f3a971702563','ANNUALIZED_PERCENTAGE','RateWithMinimumAmount',NULL,0.0075,1300000,NULL,'YEAR',1);
  check_tariff('CBI1405R_GAR_1_4','1-4','TARIFF_1_4','ضمانتنامه در مقابل ضمانتنامه های صادره توسط مؤسسات اعتباری داخلی','CBI_RIAL_1405_GUARANTEE_GROUP','CBI1405R_GAR','59293f9c653b7b4cd641113b76b1a7ca92eda23032379d2642c8adf86779239e','ANNUALIZED_PERCENTAGE','RateWithMinimumAmount',NULL,0.0075,1300000,NULL,'YEAR',1);
  check_tariff('CBI1405R_GAR_1_5','1-5','TARIFF_1_5','ضمانتنامه در مقابل ضمانتنامه های اعتباری (ارزی-ریالی) صادره توسط صندوق ضمانت صادرات ایران و سایر صندوق هایی که به موجب قانون تأسیس می شوند','CBI_RIAL_1405_GUARANTEE_GROUP','CBI1405R_GAR','309f17ce03e17f87cc5765c969d84a836619a7f33cdd4cc5d019c00f5a19c250','ANNUALIZED_PERCENTAGE','RateWithMinimumAmount',NULL,0.0075,1300000,NULL,'YEAR',1);
  check_tariff('CBI1405R_GAR_1_6','1-6','TARIFF_1_6','ضمانتنامه در مقابل ضمانتنامه های صادره توسط بانک های خارجی معتبر','CBI_RIAL_1405_GUARANTEE_GROUP','CBI1405R_GAR','a4d23c0f25c7aa5e49e299c547d7fda2ebc98ca9b7bce5c2c09f865a18ae3b32','ANNUALIZED_PERCENTAGE','RateWithMinimumAmount',NULL,0.0175,1300000,NULL,'YEAR',1);
  check_tariff('CBI1405R_GAR_1_7','1-7','TARIFF_1_7','ضمانتنامه در مقابل اوراق بهادار بدون ریسک (2)','CBI_RIAL_1405_GUARANTEE_GROUP','CBI1405R_GAR','062ae729b655acff285d7dbde94bf886bec0ded3a5d00205d7db5beda31db8c0','ANNUALIZED_PERCENTAGE','RateWithMinimumAmount',NULL,0.005,1300000,NULL,'YEAR',1);
  check_tariff('CBI1405R_GAR_1_8','1-8','TARIFF_1_8','ضمانتنامه در مقابل سایر اوراق بهادار (3)','CBI_RIAL_1405_GUARANTEE_GROUP','CBI1405R_GAR','ed3724bd8d1ef307b628e278d2050cb5bef10c1f5694e9cef39c894ae9518192','ANNUALIZED_PERCENTAGE','RateWithMinimumAmount',NULL,0.01,1300000,NULL,'YEAR',1);
  check_tariff('CBI1405R_GAR_1_9','1-9','TARIFF_1_9','ضمانتنامه در مقابل سهام پذیرفته شده در بورس (4)','CBI_RIAL_1405_GUARANTEE_GROUP','CBI1405R_GAR','929fe778ef95f31ba16ba98c666ccd7dc842396971437a990d4fe21ec82f33bb','ANNUALIZED_PERCENTAGE','RateWithMinimumAmount',NULL,0.01,1300000,NULL,'YEAR',1);
  check_tariff('CBI1405R_GAR_1_10','1-10','TARIFF_1_10','ضمانتنامه در مقابل اموال غیرمنقول سهل البیع (با تشخیص مؤسسه اعتباری)','CBI_RIAL_1405_GUARANTEE_GROUP','CBI1405R_GAR','4e051c37d28fb735e61f97ef782e9a732fb362a3a5310cbd896053976531be2c','ANNUALIZED_PERCENTAGE','RateWithMinimumAmount',NULL,0.01,1300000,NULL,'YEAR',1);
  check_tariff('CBI1405R_GAR_1_11','1-11','TARIFF_1_11','ضمانتنامه در مقابل سایر اموال غیرمنقول','CBI_RIAL_1405_GUARANTEE_GROUP','CBI1405R_GAR','9d22d524600e25e51781bbaac6c476c44b9b4b51ac49818d6877f611c485da0b','ANNUALIZED_PERCENTAGE','RateWithMinimumAmount',NULL,0.015,1300000,NULL,'YEAR',1);
  check_tariff('CBI1405R_GAR_1_12','1-12','TARIFF_1_12','ضمانتنامه در مقابل توثیق سند کشتی و هواپیما دارای بیمه نامه معتبر','CBI_RIAL_1405_GUARANTEE_GROUP','CBI1405R_GAR','10139767847660285c38147ff2c59a2b926ac5605962685378a283966ae51749','ANNUALIZED_PERCENTAGE','RateWithMinimumAmount',NULL,0.015,1300000,NULL,'YEAR',1);
  check_tariff('CBI1405R_GAR_1_13','1-13','TARIFF_1_13','ضمانتنامه در مقابل اسناد تجاری نظیر سفته و چک','CBI_RIAL_1405_GUARANTEE_GROUP','CBI1405R_GAR','4c614b8302dca6aff5acd736d352d7b4f8e3aa87d660f9e2c1a3bf195f0636ca','ANNUALIZED_PERCENTAGE','RateWithMinimumAmount',NULL,0.0225,1300000,NULL,'YEAR',1);
  check_tariff('CBI1405R_GAR_1_14','1-14','TARIFF_1_14','ضمانتنامه در مقابل رسید انبارهای عمومی','CBI_RIAL_1405_GUARANTEE_GROUP','CBI1405R_GAR','06f4f71f51618610c8ecbdd42db6dd6f9a33cca3dfabd305a178d0a526f53bd8','ANNUALIZED_PERCENTAGE','RateWithMinimumAmount',NULL,0.02,1300000,NULL,'YEAR',1);
  check_tariff('CBI1405R_GAR_1_15','1-15','TARIFF_1_15','ضمانتنامه در مقابل سایر وثایق','CBI_RIAL_1405_GUARANTEE_GROUP','CBI1405R_GAR','6d29b4b0b2cb404c1d3e3a5c7914f43b62b75c6fc8d090821ccd6a247e160a5e','ANNUALIZED_PERCENTAGE','Percentage',NULL,0.0225,NULL,NULL,'YEAR',1);
  check_tariff('CBI1405R_GAR_1_16','1-16','TARIFF_1_16','تمدید ضمانتنامه','CBI_RIAL_1405_GUARANTEE_GROUP','CBI1405R_GAR','2dadc7d20a7063c08f3175b94e488c45f083de6bdc3245feb8ecfc412d2c3270','COMPOSITE','Other',NULL,NULL,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_GAR_1_17','1-17','TARIFF_1_17','تقلیل ضمانتنامه','CBI_RIAL_1405_GUARANTEE_GROUP','CBI1405R_GAR','254b4011237f3944cfb660ff2293dfd0f8c7ef9732ff04bc527415eb8eff1220','COMPOSITE','Other',NULL,NULL,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_GAR_1_18','1-18','TARIFF_1_18','ابطال ضمانتنامه قبل از سررسید (غیر از ضبط ضمانتنامه) (5)','CBI_RIAL_1405_GUARANTEE_GROUP','CBI1405R_GAR','4f30dcea8f4ff7de4c56ad3d14b4a559133bd3d726a94d3ff429c2baf417115e','COMPOSITE','Other',NULL,NULL,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_GAR_1_19','1-19','TARIFF_1_19','صدور المثنی ضمانتنامه های بانکی','CBI_RIAL_1405_GUARANTEE_GROUP','CBI1405R_GAR','36ff4467014cd57d487955b7f264b01e53952e0f5d0ba9f6a952094380642db4','FIXED','Flat',1300000,NULL,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_GAR_1_20','1-20','TARIFF_1_20','انتقال ذی نفع ضمانتنامه','CBI_RIAL_1405_GUARANTEE_GROUP','CBI1405R_GAR','95f29334658529f263295b5e8401c886403b6d1942144429c0ee9441c8bb3384','PERCENTAGE_WITH_FLOOR','RateWithMinimumAmount',NULL,0.0005,1300000,NULL,NULL,1);
  check_tariff('CBI1405R_REM_2_1','2-1','TARIFF_2_1','صدور چک تضمین شده یا رمزدار بین بانکی','CBI_RIAL_1405_REMITTANCE_GROUP','CBI1405R_REM','d24430d6b669eef06d337f8efaa26c64175a30e7ee1d3fc1cf460c029f4ad874','PER_UNIT','PerUnit',144000,NULL,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_REM_2_2','2-2','TARIFF_2_2','اعلام منع پرداخت چک های بانکی/بین بانکی مفقودی','CBI_RIAL_1405_REMITTANCE_GROUP','CBI1405R_REM','d8383d1aac3afed03367a70d0c68d7563f0444a4dc5e47a099519668bf2c5748','FIXED','Flat',1152000,NULL,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_REM_2_3','2-3','TARIFF_2_3','صدور المثنی چک بانکی','CBI_RIAL_1405_REMITTANCE_GROUP','CBI1405R_REM','f9e72875022796b4e97ef2bf6f92fde44a19b7e9e382e35889e9a07204efffcb','PER_UNIT','PerUnit',144000,NULL,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_REM_2_4','2-4','TARIFF_2_4','استرداد/ابطال چک بانکی/بین بانکی به درخواست مشتری','CBI_RIAL_1405_REMITTANCE_GROUP','CBI1405R_REM','e1d774cf8c0c5c6e27a0f59a980742203eaba49b769f0ca16bfe58123aa4088e','PER_UNIT','PerUnit',288000,NULL,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_SDB_3_1','3-1','TARIFF_3_1','صندوق اجاره ای (اجاره بها و ودیعه سالانه)','CBI_RIAL_1405_SAFE_DEPOSIT_GROUP','CBI1405R_SDB','d2be2e6bde60dad6f2c284a000c165cf999e1a71f86b710e1b51324d53529708','COMPOSITE','Other',NULL,NULL,NULL,NULL,NULL,2);
  check_tariff('CBI1405R_SDB_3_2','3-2','TARIFF_3_2','هر بار استفاده از صندوق','CBI_RIAL_1405_SAFE_DEPOSIT_GROUP','CBI1405R_SDB','af091aba3e46f28d96b3d5e28385a172accbcaf351e5cbb1fbcc4710d703d3c2','FIXED','Flat',120000,NULL,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_SDB_3_3','3-3','TARIFF_3_3','مفقودی کلیدها','CBI_RIAL_1405_SAFE_DEPOSIT_GROUP','CBI1405R_SDB','535310cfe1a66c1e74ddce14748a67c6be06d06d7d5776aa3f52506ecca49c2c','FIXED','Flat',288000,NULL,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_SDB_3_4','3-4','TARIFF_3_4','تخلیه و تحویل صندوق','CBI_RIAL_1405_SAFE_DEPOSIT_GROUP','CBI1405R_SDB','f9227c8064aab415673d659aef726314a20da8cef6fa2155673975e24987a959','FIXED','Flat',0,NULL,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_SDB_3_5','3-5','TARIFF_3_5','هماهنگی جهت بازدید از صندوق اجاره ای به درخواست مراجع ذی صلاح','CBI_RIAL_1405_SAFE_DEPOSIT_GROUP','CBI1405R_SDB','08072d74bc6747801e97a745e4cb29c06f8c2cfa2655d54639aa56baf28a7cad','FIXED','Flat',0,NULL,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_SEC_4_1','4-1','TARIFF_4_1','انتقال هر برگ اوراق گواهی حق تقدم اعطای تسهیلات','CBI_RIAL_1405_SECURITIES_GROUP','CBI1405R_SEC','a8b6b4d3b345562825bf5b7854e037a42240164e6bd42aff00a3bb9f18efec70','FIXED','Flat',66000,NULL,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_SEC_4_2','4-2','TARIFF_4_2','صدور هر برگ المثنی اوراق حق تقدم اعطای تسهیلات','CBI_RIAL_1405_SECURITIES_GROUP','CBI1405R_SEC','ace5e32719b1e0589681e83ae4fdc9aa22b47db60dd7f87a07f8d0d938e2f3d4','FIXED','Flat',110000,NULL,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_SEC_4_3','4-3','TARIFF_4_3','تضمین اصل و سود اوراق بدهی توسط مؤسسه اعتباری عامل','CBI_RIAL_1405_SECURITIES_GROUP','CBI1405R_SEC','2fafbf1a111ed3c18343e6e39b6b09ebfd7a2a217aa002746f3734586262d340','COMPOSITE','Other',NULL,NULL,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_SEC_4_4','4-4','TARIFF_4_4','انتقال اوراق بهادار/نقل و انتقال اوراق گواهی سپرده با نام به شخص ثالث','CBI_RIAL_1405_SECURITIES_GROUP','CBI1405R_SEC','1888de836789bf23bda5d428b2c612635818392c58da3bd8ec9193b12cff6977','PERCENTAGE_WITH_CAP','RateWithMaximumAmount',NULL,0.0005,NULL,480000,NULL,1);
  check_tariff('CBI1405R_SEC_4_5','4-5','TARIFF_4_5','عاملیت و ضمانت اوراق گواهی اعتبار مولد (گام)','CBI_RIAL_1405_SECURITIES_GROUP','CBI1405R_SEC','286fb05e79862164c27de33cd7967db15f3406b8530e93a2fc0c96931a35b698','COMPOSITE','Other',NULL,NULL,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_SEC_4_6','4-6','TARIFF_4_6','انتقال اوراق گواهی اعتبار مولد (گام)','CBI_RIAL_1405_SECURITIES_GROUP','CBI1405R_SEC','2998e24db2b3ea91424021bf25ea50f065d210a1e8e3c874412635598588be87','PERCENTAGE','Percentage',NULL,0.001,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_SEC_4_7','4-7','TARIFF_4_7','ابطال اوراق گواهی اعتبار مولد (گام) در لایه اول زنجیره تولید (از متقاضی) (1)','CBI_RIAL_1405_SECURITIES_GROUP','CBI1405R_SEC','d19a0a0c3c9bec30b333aaffb32f87ff485c0b258397bbc46bd172ccc05ee109','PERCENTAGE','Percentage',NULL,0.0005,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_SEC_4_8','4-8','TARIFF_4_8','انتقال اوراق گام به بازار سرمایه (با نماد بازار سرمایه)','CBI_RIAL_1405_SECURITIES_GROUP','CBI1405R_SEC','6395d804a347c0ac4de5bec624a663dad0a44e9d29405b62eb5360d4785b18a9','PERCENTAGE','Percentage',NULL,0.0005,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_SEC_4_9','4-9','TARIFF_4_9','تنزیل (خرید دین) اوراق گام (با نماد بازار پول)','CBI_RIAL_1405_SECURITIES_GROUP','CBI1405R_SEC','2b64241dc04b8c36cde8916b65c90420b5de0a76d194dbd35e8e1cdef1789bd6','PERCENTAGE','Percentage',NULL,0.0005,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_SEC_4_10','4-10','TARIFF_4_10','توثیق اوراق گواهی اعتبار مولد (گام)','CBI_RIAL_1405_SECURITIES_GROUP','CBI1405R_SEC','42092c3d4088ec5cb07a7a3c61ee8eb91958778545bff704fda84926d2d9ed54','PERCENTAGE','Percentage',NULL,0.0005,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_SEC_4_11','4-11','TARIFF_4_11','بررسی و پذیرش مطالبات قراردادی با موضوع تأمین کالا','CBI_RIAL_1405_SECURITIES_GROUP','CBI1405R_SEC','87d901fab59c2caac06a755e7acc7a73dda0e5e7acbce5f24478775fda192106','PERCENTAGE','Percentage',NULL,0.0005,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_SEC_4_12','4-12','TARIFF_4_12','بررسی و پذیرش مطالبات قراردادی با موضوع تأمین خدمات','CBI_RIAL_1405_SECURITIES_GROUP','CBI1405R_SEC','6b253b083fa973780e47c61f88c0d908cee9880a6c825e5d17315439e2cadb81','PERCENTAGE','Percentage',NULL,0.0015,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_SEC_4_13','4-13','TARIFF_4_13','کارت رفاهی متصل به اوراق گواهی اعتبار مولد (گام) (به صورت اقساط مساوی در دوره بازپرداخت)','CBI_RIAL_1405_SECURITIES_GROUP','CBI1405R_SEC','953d11940068b72d9cd0bfd1e27c3184c794164490edf9ef88bd1594ce5e0224','PERCENTAGE','Percentage',NULL,0.03,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_SEC_4_14','4-14','TARIFF_4_14','صدور اوراق گواهی اعتبار مولد (گام) یارانه دارو','CBI_RIAL_1405_SECURITIES_GROUP','CBI1405R_SEC','68cca70ce1382315e19182c03846d2d0f1171423310351fd535b442909846904','ANNUALIZED_PERCENTAGE','Percentage',NULL,0.005,NULL,NULL,'YEAR',1);
  check_tariff('CBI1405R_COL_5_1','5-1','TARIFF_5_1','سفته، برات و قبوض ثبتی وصولی','CBI_RIAL_1405_COLLECTION_GROUP','CBI1405R_COL','c8f095463291787c47e646e4f72c0a3ba9c5690e6b7697f7ef310342dc8e9bf5','PER_UNIT','PerUnit',144000,NULL,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_COL_5_2','5-2','TARIFF_5_2','ابلاغ سفته و برات','CBI_RIAL_1405_COLLECTION_GROUP','CBI1405R_COL','b397bc0ba71fc6fabc27d2b8a6310e2dbe0b10eae6995e6b69cce8c9cc755e0f','PER_UNIT','PerUnit',86400,NULL,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_COL_5_3','5-3','TARIFF_5_3','واخواست سفته','CBI_RIAL_1405_COLLECTION_GROUP','CBI1405R_COL','70834c73e70f2a0bcee7e5335ea6100c1ec7be7d3dfd0ec822cd56fffc9a6540','PER_UNIT','PerUnit',1152000,NULL,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_COL_5_4','5-4','TARIFF_5_4','فروش سفته/براتنامه (از متقاضی)','CBI_RIAL_1405_COLLECTION_GROUP','CBI1405R_COL','2d4daa76d3a28dcdb8dac1e44adcb7dc7fa9f9120dfe4301449dade965c1d8cb','PER_UNIT','PerUnit',120000,NULL,NULL,2400000,NULL,1);
  check_tariff('CBI1405R_COL_5_5','5-5','TARIFF_5_5','قبولی «برات الکترونیکی زنجیره تولید»','CBI_RIAL_1405_COLLECTION_GROUP','CBI1405R_COL','af4c999b5a808c9a9e13f2abe31efa3485165db7858aa4d5bf4145996a3ba6ef','COMPOSITE','Other',NULL,NULL,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_COL_5_6','5-6','TARIFF_5_6','انتقال «برات الکترونیکی زنجیره تولید»','CBI_RIAL_1405_COLLECTION_GROUP','CBI1405R_COL','8e04978519fba6988416a3ae4f2c9a4c2055993c47e5aeb486a4111adca2da48','PERCENTAGE','Percentage',NULL,0.001,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_ACC_6_1_1','6-1-1','TARIFF_6_1_1','تقاضای عدم پرداخت چک','CBI_RIAL_1405_ACCOUNT_GROUP','CBI1405R_ACC','9178b4d49b6ba8108a4af30ae08e877b6aaa37388937b73c590db48164db9102','PER_UNIT','PerUnit',288000,NULL,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_ACC_6_1_2','6-1-2','TARIFF_6_1_2','صدور گواهی نامه عدم پرداخت وجه چک','CBI_RIAL_1405_ACCOUNT_GROUP','CBI1405R_ACC','e2baf008ded01e348ffbf642555a752ea27899c899d5226908e0908bb60c514d','FIXED','Flat',288000,NULL,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_ACC_6_1_3','6-1-3','TARIFF_6_1_3','رفع سوء اثر برای هر برگ چک','CBI_RIAL_1405_ACCOUNT_GROUP','CBI1405R_ACC','c71608b91cfa9a7c5590bc736d19737841f41b9c98590d7740af285d877b748a','FIXED','Flat',288000,NULL,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_ACC_6_1_4','6-1-4','TARIFF_6_1_4','انتقال حساب جاری به شعبه دیگر در هر شهر (بنا به درخواست مشتری)','CBI_RIAL_1405_ACCOUNT_GROUP','CBI1405R_ACC','cabe007e9fd49dfe1f9ca37e422f08bd5bdff01921c62efe6b6ba689b8d194bf','FIXED','Flat',864000,NULL,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_ACC_6_1_5','6-1-5','TARIFF_6_1_5','اخذ هرگونه اطلاعات لازم جهت افتتاح حساب سپرده قرض الحسنه جاری با/بدون دسته چک','CBI_RIAL_1405_ACCOUNT_GROUP','CBI1405R_ACC','1aee1e421606d5bd8a00f1c080c15c250bd4fa5c50c0c9ad7024e74dac7fad6e','FIXED','Flat',NULL,NULL,120000,NULL,NULL,1);
  check_tariff('CBI1405R_ACC_6_1_6','6-1-6','TARIFF_6_1_6','افتتاح حساب سپرده قرض الحسنه جاری موقت (برای شخص حقوقی در شرف تأسیس)','CBI_RIAL_1405_ACCOUNT_GROUP','CBI1405R_ACC','ff038b316d4d4f746dfec96a0ef8b7bb22377cf3340dd1bc0c995fb203a1c8c5','FIXED','Flat',120000,NULL,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_ACC_6_1_7','6-1-7','TARIFF_6_1_7','چک های واگذاری عهده سایر مؤسسات اعتباری (از ذی نفع چک)','CBI_RIAL_1405_ACCOUNT_GROUP','CBI1405R_ACC','1a82c06fb509faebb4801c768e41814b2f16305519144db45fabe9cab0eb34ae','PER_UNIT','PerUnit',28800,NULL,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_ACC_6_1_8','6-1-8','TARIFF_6_1_8','انتقال وجه از سایر حساب های مشتری بابت تأمین وجه چک','CBI_RIAL_1405_ACCOUNT_GROUP','CBI1405R_ACC','ce8aa7d18b8e40eb11abc0a77b40da12d59e1cc67aa2d6921fa4add9c4c5a293','PER_UNIT','PerUnit',144000,NULL,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_ACC_6_1_9','6-1-9','TARIFF_6_1_9','اخذ گزارش اعتباری برای صدور دسته چک اشخاص حقیقی','CBI_RIAL_1405_ACCOUNT_GROUP','CBI1405R_ACC','c8aece82449f7a649715f29da06eb33cc1087be07050dbb435357fe7f65ec56b','COMPOSITE','Other',NULL,NULL,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_ACC_6_1_10','6-1-10','TARIFF_6_1_10','صدور دسته چک','CBI_RIAL_1405_ACCOUNT_GROUP','CBI1405R_ACC','8ed8819dea5829e4f2a8cd57382f6f245e4223a406c542c75a24a63ff028e3ce','FIXED','Flat',120000,NULL,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_ACC_6_1_11','6-1-11','TARIFF_6_1_11','صدور المثنی (چاپ مجدد) گواهی نامه عدم پرداخت صادرشده قبلی به درخواست مشتری','CBI_RIAL_1405_ACCOUNT_GROUP','CBI1405R_ACC','ecf64820e239918c84d0caa2f3e3450188766b613e1afe09f92caa11cb45de4f','FIXED','Flat',44000,NULL,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_ACC_6_1_12','6-1-12','TARIFF_6_1_12','ابطال/استرداد چک صادره توسط اشخاص به درخواست مشتری با ارائه لاشه چک باطل شده','CBI_RIAL_1405_ACCOUNT_GROUP','CBI1405R_ACC','473e6dd22b2d9242afc17c3ea90cb77e95ccd5a3f28cf85173f1e52f503feeb6','FIXED','Flat',22000,NULL,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_ACC_6_1_13','6-1-13','TARIFF_6_1_13','اخذ صورت اطلاعات چک های برگشتی به درخواست مشتری','CBI_RIAL_1405_ACCOUNT_GROUP','CBI1405R_ACC','9a1e833a9bb45895d1859caf1c5665b2935952f9aa2e096cf38a8dd2f03d4178','FIXED','Flat',52800,NULL,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_ACC_6_1_14','6-1-14','TARIFF_6_1_14','تحویل دسته چک به مشتری در شعبه ای غیر از شعبه افتتاح کننده حساب سپرده قرض الحسنه جاری','CBI_RIAL_1405_ACCOUNT_GROUP','CBI1405R_ACC','782fb126b7862199aabc514e06917751172b223f26b767977126a913a142a9db','FIXED','Flat',48000,NULL,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_ACC_6_1_15','6-1-15','TARIFF_6_1_15','نگهداری امانی چک های با سررسید بیش از 30 روز (به استثنای چک های مأخوذه بابت تضمین تسهیلات)','CBI_RIAL_1405_ACCOUNT_GROUP','CBI1405R_ACC','0fc4b9a4003f2b990354f1a5c6588278e7ec2163e189ab53e475e3c6cb6d99cd','PER_UNIT','PerUnit',12000,NULL,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_ACC_6_1_16','6-1-16','TARIFF_6_1_16','صدور چک موردی جاری اشخاص','CBI_RIAL_1405_ACCOUNT_GROUP','CBI1405R_ACC','42bee16d3066c0830fb287fe8e7928ef758021019797fe25448b76a26e5c1375','FIXED','Flat',140000,NULL,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_ACC_6_2_1','6-2-1','TARIFF_6_2_1','افتتاح حساب سپرده قرض الحسنه پس انداز','CBI_RIAL_1405_ACCOUNT_GROUP','CBI1405R_ACC','5c296826b4b6d79477ad04665de7418c046ff827c4ee5ef9dccb7ad29bfc7deb','FIXED','Flat',0,NULL,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_ACC_6_2_2','6-2-2','TARIFF_6_2_2','افتتاح حساب سپرده سرمایه گذاری کوتاه مدت','CBI_RIAL_1405_ACCOUNT_GROUP','CBI1405R_ACC','6e7b733e0416f2249118777d8016ab816fb3dfba635b8df09e387b68eca4b1c8','FIXED','Flat',120000,NULL,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_ACC_6_2_3','6-2-3','TARIFF_6_2_3','صدور هر جلد دفترچه صرفاً جهت حساب های سپرده سرمایه گذاری کوتاه مدت','CBI_RIAL_1405_ACCOUNT_GROUP','CBI1405R_ACC','91e38158c3fe5a0e9e1a3601e3fa177bedba9352c9bde44ab412b5c1294cd87a','FIXED','Flat',96000,NULL,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_ACC_6_3_1','6-3-1','TARIFF_6_3_1','صدور هرگونه گواهی حساب','CBI_RIAL_1405_ACCOUNT_GROUP','CBI1405R_ACC','69fefcc63cfaa229ab2b1474b36515a2950f01de3893d097343c4803bb038037','FIXED','Flat',201500,NULL,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_ACC_6_3_2','6-3-2','TARIFF_6_3_2','صدور گواهی حساب به لاتین','CBI_RIAL_1405_ACCOUNT_GROUP','CBI1405R_ACC','2a01716b35741686dfc27abdbedbab9b42531bceb194a531d55ff40d3bd34ec7','PER_UNIT','PerUnit',432000,NULL,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_ACC_6_3_3','6-3-3','TARIFF_6_3_3','صدور المثنی گواهی سپرده های سرمایه گذاری ویژه کوتاه مدت و بلندمدت','CBI_RIAL_1405_ACCOUNT_GROUP','CBI1405R_ACC','f8be683a202b2ef102c707c93b45f081c784aa372867eb179031930e4b2db957','FIXED','Flat',132000,NULL,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_ACC_6_4_1','6-4-1','TARIFF_6_4_1','ارائه نسخه فیزیکی صورت حساب به مشتری','CBI_RIAL_1405_ACCOUNT_GROUP','CBI1405R_ACC','0d058d556b5b17540b9e3dc41c5d333ae591e202520773a1f2bbad1aa774dca6','PER_UNIT','PerUnit',14500,NULL,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_ACC_6_4_2','6-4-2','TARIFF_6_4_2','ارائه نسخه الکترونیکی (فایل) صورت حساب به مشتری','CBI_RIAL_1405_ACCOUNT_GROUP','CBI1405R_ACC','864cc2e25017e991a3ea761584665f09afe4a1c7a19af5f544e981d18e2fa5a5','FIXED','Flat',96000,NULL,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_ACC_6_5_1','6-5-1','TARIFF_6_5_1','ارتباط هر حساب جدید با کارت (پس از صدور کارت) - از طریق مراجعه به شعبه','CBI_RIAL_1405_ACCOUNT_GROUP','CBI1405R_ACC','dff98b5cb3ac62981dcf72e54a9298692ad9458a1322860cd75e6707a441a5fa','FIXED','Flat',43000,NULL,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_ACC_6_5_2','6-5-2','TARIFF_6_5_2','تغییر حساب اصلی کارت - از طریق مراجعه به شعبه','CBI_RIAL_1405_ACCOUNT_GROUP','CBI1405R_ACC','7c51bf0d6843e19a7a7e32270f6f2aec51ecb18255f8d937e47b8c7a05f8a1ad','FIXED','Flat',72000,NULL,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_ACC_6_5_3','6-5-3','TARIFF_6_5_3','فعال سازی حساب های راکد - از طریق مراجعه به شعبه','CBI_RIAL_1405_ACCOUNT_GROUP','CBI1405R_ACC','e00c89a2088ff4bf36dd5231423716941b0286583c4a9805ec3976c7d1d6a3f5','PER_UNIT','PerUnit',52000,NULL,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_ACC_6_5_4','6-5-4','TARIFF_6_5_4','فعال سازی حساب های مطالبه نشده و تعیین تکلیف نشده - از طریق مراجعه به شعبه','CBI_RIAL_1405_ACCOUNT_GROUP','CBI1405R_ACC','e31bbfeb5b9b945af378d746429a3850f604d07615c079c162178f4c9c6a9e31','PER_UNIT','PerUnit',52000,NULL,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_ACC_6_5_5','6-5-5','TARIFF_6_5_5','انتقال و واگذاری سپرده سرمایه گذاری بلندمدت به اشخاص مجاز طبق ضوابط ابلاغی بانک مرکزی','CBI_RIAL_1405_ACCOUNT_GROUP','CBI1405R_ACC','6d61bde66c5e22f23237c4a0aed47309cc153cb2eeb701d5ed884278a5304ae9','PER_UNIT','PerUnit',145000,NULL,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_ACC_6_5_6','6-5-6','TARIFF_6_5_6','انسداد یا رفع انسداد انواع حساب/کارت به درخواست مشتری - از طریق مراجعه به شعبه','CBI_RIAL_1405_ACCOUNT_GROUP','CBI1405R_ACC','40ae601323af78c1c5f891d53e0c9aa7dc4a0cdd2200ebfd9c7348fe3172701c','FIXED','Flat',72000,NULL,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_ACC_6_5_7','6-5-7','TARIFF_6_5_7','تغییر در شرایط/اطلاعات انواع حساب (نظیر صاحبان امضاء و آدرس) به درخواست مشتری - از طریق مراجعه به شعبه','CBI_RIAL_1405_ACCOUNT_GROUP','CBI1405R_ACC','a88086b4e7713acc29cd57341d640d826c8ec2b6c256beda6d91eee301882c8b','COMPOSITE','Other',NULL,NULL,NULL,NULL,NULL,2);
  check_tariff('CBI1405R_ACC_6_5_8','6-5-8','TARIFF_6_5_8','برقراری ارتباط بین انواع حساب با لحاظ کف و سقف مبلغ - از طریق مراجعه به شعبه (به استثنای تأمین وجه چک)','CBI_RIAL_1405_ACCOUNT_GROUP','CBI1405R_ACC','641dbf81e6104661e73313c157d8a466ec2765a393fbfb8d931857ae9bfe3859','COMPOSITE','Other',NULL,NULL,NULL,NULL,NULL,2);
  check_tariff('CBI1405R_ACC_6_5_9','6-5-9','TARIFF_6_5_9','ثبت دستور پرداخت مستمر - از طریق مراجعه به شعبه','CBI_RIAL_1405_ACCOUNT_GROUP','CBI1405R_ACC','f35bb6419508446f4213a189b8c63fc0402d2b9fbdb029cf48a83dbb483fa6b5','COMPOSITE','Other',NULL,NULL,NULL,NULL,NULL,2);
  check_tariff('CBI1405R_ACC_6_5_10','6-5-10','TARIFF_6_5_10','انتقال سپرده قرض الحسنه پس انداز و سرمایه گذاری مدت دار به شعب دیگر در هر شهر','CBI_RIAL_1405_ACCOUNT_GROUP','CBI1405R_ACC','70748664c43df67cdf4e008ba0981bcb933dea7585e5cc59c7aefc82715759f4','FIXED','Flat',220000,NULL,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_ACC_6_5_11','6-5-11','TARIFF_6_5_11','انسداد یا رفع انسداد انواع حساب یا موجودی که از سوی مراجع ذی صلاح به هر طریق به مؤسسه اعتباری اعلام می گردد','CBI_RIAL_1405_ACCOUNT_GROUP','CBI1405R_ACC','6317440ff72aa0ca899ab8b2dd30c8f79aad30426798001da88349c220b03c9c','FIXED','Flat',0,NULL,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_ACC_6_5_12','6-5-12','TARIFF_6_5_12','صدور رمز جدید برای انواع کارت های بانکی (نقدی، اعتباری و هدیه) به درخواست مشتری - از طریق مراجعه به شعبه','CBI_RIAL_1405_ACCOUNT_GROUP','CBI1405R_ACC','edd2b6dd6aeef9e707ade89330bec098e0d22da301da3353d04c9fe3acd6af9a','FIXED','Flat',24000,NULL,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_ACC_6_5_13','6-5-13','TARIFF_6_5_13','هزینه آبونمان سالانه کارت اعتباری مراجعه','CBI_RIAL_1405_ACCOUNT_GROUP','CBI1405R_ACC','173d9961dfd5f8f849de71fe15128f8dfb0d8d4f98de528e91a56d4ff12a9ca3','PERCENTAGE','Percentage',NULL,0.01,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_ACC_6_5_14','6-5-14','TARIFF_6_5_14','بررسی تراکنش های کارت های مفقودی/سرقتی به درخواست مشتری از طریق مراجعه به شعبه','CBI_RIAL_1405_ACCOUNT_GROUP','CBI1405R_ACC','3e22b743eaff68cc5b1df69bc40f56015407b6956229451cb4bb47ea43abcce9','PER_UNIT','PerUnit',40000,NULL,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_ACC_6_5_15','6-5-15','TARIFF_6_5_15','تعیین سقف برداشت/واریز به حساب به درخواست مشتری از طریق مراجعه به شعبه','CBI_RIAL_1405_ACCOUNT_GROUP','CBI1405R_ACC','e7cdfdd02550cf27061c733c5104ac7b315fd4371bcb796ab6b702fbe2072de2','FIXED','Flat',40000,NULL,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_ACC_6_5_16','6-5-16','TARIFF_6_5_16','بستن انواع حساب در شعبه ای غیر از شعبه افتتاح کننده حساب به درخواست مشتری از طریق مراجعه به شعبه','CBI_RIAL_1405_ACCOUNT_GROUP','CBI1405R_ACC','67728b6cf84fccde3aa1b5cc7acf479c17984fead98faa5a6a38c850af4563ba','FIXED','Flat',100000,NULL,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_APP_7_1','7-1','TARIFF_7_1','ارزیابی ماشین آلات و کالا','CBI_RIAL_1405_APPRAISAL_GROUP','CBI1405R_APP','623979f41e4504e7651399854d6a28c6a4834a0fbab1006fe2fa0f127d16fc5f','COMPOSITE','Other',NULL,NULL,NULL,NULL,NULL,3);
  check_tariff('CBI1405R_APP_7_2','7-2','TARIFF_7_2','ارزیابی املاک و ساختمان','CBI_RIAL_1405_APPRAISAL_GROUP','CBI1405R_APP','2336a9473bf1898a5505dc22cf7b8e0b11b8ef45b9eb247fa6f0ef9be5b74adb','COMPOSITE','Other',NULL,NULL,NULL,NULL,NULL,3);
  check_tariff('CBI1405R_APP_7_3','7-3','TARIFF_7_3','ارزیابی املاک مزروعی و باغات','CBI_RIAL_1405_APPRAISAL_GROUP','CBI1405R_APP','180e93e0b262167e526d8b2b9e487addcb6572d03f5cbda322f58a189536d81c','COMPOSITE','Other',NULL,NULL,NULL,NULL,NULL,3);
  check_tariff('CBI1405R_APP_7_4','7-4','TARIFF_7_4','ارزیابی میزان پیشرفت فیزیکی طرح های موضوع تسهیلات مشارکت مدنی (برآورد هزینه و آورده غیرنقدی شریک)','CBI_RIAL_1405_APPRAISAL_GROUP','CBI1405R_APP','bfa3bbd7ff510908e885c4ad30fb8c34c6a128bf5351e38d2d55d35bb872005d','COMPOSITE','Other',NULL,NULL,NULL,NULL,NULL,3);
  check_tariff('CBI1405R_APP_7_5','7-5','TARIFF_7_5','بازدید تسهیلات تعمیر مسکن/بازدید نوبت های اضافی تسهیلات تعمیر مسکن در قالب کلیه عقود','CBI_RIAL_1405_APPRAISAL_GROUP','CBI1405R_APP','0643a4c337e1a78c3f0ed03347a0178e9403a96bc9602bd3f7ca626181417e20','FIXED','Flat',1000000,NULL,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_APP_7_6','7-6','TARIFF_7_6','ارزیابی مجدد به درخواست مشتری (به غیر از تسهیلات جعاله تعمیر مسکن)','CBI_RIAL_1405_APPRAISAL_GROUP','CBI1405R_APP','9035cc34bb9f3b37894fb2a5cdb5796f06c3abac11ae4d9c22c3625bd39e8e68','COMPOSITE','Other',NULL,NULL,NULL,NULL,NULL,3);
  check_tariff('CBI1405R_APP_7_7','7-7','TARIFF_7_7','ارزیابی وثایق غیرمنقول مازاد','CBI_RIAL_1405_APPRAISAL_GROUP','CBI1405R_APP','67272638e4ee7de9a5c94bccea5dcc838955efa20126a6e9415cf75104e89448','COMPOSITE','Other',NULL,NULL,NULL,NULL,NULL,3);
  check_tariff('CBI1405R_CRD_8_1','8-1','TARIFF_8_1','اخذ استعلام اعتبارسنجی از شرکت رتبه بندی اعتباری ایران','CBI_RIAL_1405_CREDIT_GROUP','CBI1405R_CRD','879a99d17e13d0404ef840c36535aadc2c79555c986dabe7738549b612dbed6b','COMPOSITE','Other',NULL,NULL,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_CRD_8_2','8-2','TARIFF_8_2','واگذاری سهم الشرکه بانک به غیر (1)','CBI_RIAL_1405_CREDIT_GROUP','CBI1405R_CRD','8bd4714ec36b08527c1171f071950107faf3bae02269c1a02ed251e94b3823ea','COMPOSITE','Other',NULL,NULL,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_CRD_8_3','8-3','TARIFF_8_3','تمدید تسهیلات عقود مشارکتی','CBI_RIAL_1405_CREDIT_GROUP','CBI1405R_CRD','6c6501f2e53b885272a4d2224277f055f93bebc14be825771f1f986f27bd92eb','PERCENTAGE_WITH_CAP','RateWithMaximumAmount',NULL,0.005,NULL,5760000,NULL,1);
  check_tariff('CBI1405R_CRD_8_4','8-4','TARIFF_8_4','بررسی اصالت پیش فاکتور','CBI_RIAL_1405_CREDIT_GROUP','CBI1405R_CRD','031252a94c2efee02572553dde30be0acb65b95c1616828237a27a1d4af64944','FIXED','Flat',0,NULL,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_CRD_8_5','8-5','TARIFF_8_5','صلح حقوق یا انتقال تعهد','CBI_RIAL_1405_CREDIT_GROUP','CBI1405R_CRD','12363d73685e89e2a7541e2e44ff2c17dc0e8ed068e44e90b50575ccd527462a','PERCENTAGE_FLOOR_CAP','Other',NULL,0.01,2880000,144000000,NULL,1);
  check_tariff('CBI1405R_CRD_8_6','8-6','TARIFF_8_6','تغییر در شرایط قرارداد','CBI_RIAL_1405_CREDIT_GROUP','CBI1405R_CRD','86bb98d2fae1f7838c8dc09063bd8497c6e1e8c73a51c6741da7f7877c2fadea','FIXED','Flat',220000,NULL,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_CRD_8_7','8-7','TARIFF_8_7','تفکیک، تغییر و تعویض رهینه با وثیقه (تسهیلات و تعهدات)','CBI_RIAL_1405_CREDIT_GROUP','CBI1405R_CRD','1f9cc73ed332591cb48c6f00d5a7a47ad76a29328ecd8946e80da9f79ba6d1b4','FIXED','Flat',2880000,NULL,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_CRD_8_8','8-8','TARIFF_8_8','اقاله ملک','CBI_RIAL_1405_CREDIT_GROUP','CBI1405R_CRD','98f54ce2eb08a5d163feed2ffc0e77f9d79f8b3a04bdfa665ce4054d23aebe74','FIXED','Flat',4320000,NULL,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_CRD_8_9','8-9','TARIFF_8_9','امور مربوط به نظارت بر مصرف تسهیلات (جهت انواع تسهیلاتی که به صورت دفعات واحده نیست و همچنین متناسب با پیشرفت کار پرداخت می گردد) (2)','CBI_RIAL_1405_CREDIT_GROUP','CBI1405R_CRD','b7ca1e05285be7f703276e8be4ddfd9a9ec3ce6cd5733aac43a749e7273af8db','PERCENTAGE','Percentage',NULL,0.0005,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_CRD_8_10','8-10','TARIFF_8_10','امور مربوط به بررسی رهن مازاد سایر سازمان ها و مؤسسات اعتباری','CBI_RIAL_1405_CREDIT_GROUP','CBI1405R_CRD','4cd97148119b14f3356060224751e5eb0258c9dfc2652948aff1018c84ec0ee7','FIXED','Flat',2880000,NULL,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_CRD_8_11','8-11','TARIFF_8_11','تکمیل فرم یارانه تسهیلات تکلیفی','CBI_RIAL_1405_CREDIT_GROUP','CBI1405R_CRD','c78230f37e8b07005dc63622bc31dbeb7d442dcecac008a19d1fc26a6ebc685d','FIXED','Flat',264000,NULL,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_CRD_8_12','8-12','TARIFF_8_12','صدور اخطاریه کتبی جهت اقساط یا مطالبات غیرجاری','CBI_RIAL_1405_CREDIT_GROUP','CBI1405R_CRD','7a9c5cd5028c838c0098602b0bb5e87fb30f3bbb692e3eee7c732e6301c86b8d','PER_UNIT','PerUnit',576000,NULL,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_CRD_8_13','8-13','TARIFF_8_13','انتقال ملک مرهونه به شخص ثالث','CBI_RIAL_1405_CREDIT_GROUP','CBI1405R_CRD','2413bcb393fee7999116dac2b69ce803a0f60b14a3165694ed0ae99e66ea6e54','FIXED','Flat',2880000,NULL,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_CRD_8_14','8-14','TARIFF_8_14','تغییر ضامن/گیرنده تسهیلات/تعهدات','CBI_RIAL_1405_CREDIT_GROUP','CBI1405R_CRD','602398dda2ec26b34ed99ed0f1da3c8b76b61b65d8d7d5e6a5c5dd7d89469213','FIXED','Flat',2880000,NULL,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_CRD_8_15','8-15','TARIFF_8_15','عدم استفاده از حد اعتباری تسهیلات مصوب پس از 45 روز از تاریخ ابلاغ (Commitment fee)','CBI_RIAL_1405_CREDIT_GROUP','CBI1405R_CRD','267cc361c46260f48fa37685164f4265fe026c33a0107165fa651662e4f6061d','ANNUALIZED_PERCENTAGE','Percentage',NULL,0.01,NULL,NULL,'YEAR',1);
  check_tariff('CBI1405R_CRD_8_16','8-16','TARIFF_8_16','هزینه کارشناسی (ارزیابی) طرح ها','CBI_RIAL_1405_CREDIT_GROUP','CBI1405R_CRD','e22bd343e28ec82d5eb0d4e77f89aa50ee12238c2e27d8b6ecfab02809358022','COMPOSITE','Other',NULL,NULL,NULL,NULL,NULL,2);
  check_tariff('CBI1405R_CRD_8_17','8-17','TARIFF_8_17','هزینه کارشناسی تسهیلات سرمایه در گردش (3)','CBI_RIAL_1405_CREDIT_GROUP','CBI1405R_CRD','8d8b0ef61ab153cf637823fd172a2f1dcf0840d78ac970e1fe1a8bbeff9607c3','PERCENTAGE','Percentage',NULL,0.0005,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_CRD_8_18','8-18','TARIFF_8_18','هزینه کارشناسی کالای سرمایه ای واحدهای تولیدی، خدماتی و بازرگانی خارج از طرح (4)','CBI_RIAL_1405_CREDIT_GROUP','CBI1405R_CRD','30052b0e79348461ce168704a0f93df6aa521cfa0e07ebffee07026761d0f0ed','COMPOSITE','Other',NULL,NULL,NULL,NULL,NULL,2);
  check_tariff('CBI1405R_CRD_8_19','8-19','TARIFF_8_19','هزینه کارشناسی در ایجاد تعهدات (ضمانتنامه، برات، فاکتورینگ، گشایش اعتبار اسنادی و غیره)','CBI_RIAL_1405_CREDIT_GROUP','CBI1405R_CRD','fb358f5bdec5807c18d123909fbffe27f0ae692369cf684b75de96a29b94eb4a','PERCENTAGE','Percentage',NULL,0.0005,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_CRD_8_20','8-20','TARIFF_8_20','تهیه گزارش اطلاعات اعتباری مشتمل بر استعلام از شاهکار، آدرس، استفاده از سامانه های داخلی، لیست سیاه و ... (تسهیلات/تعهدات)','CBI_RIAL_1405_CREDIT_GROUP','CBI1405R_CRD','4a4bbf98bb3c3b972720735982d985ea236792b3092a529f4b11c51f2f135231','COMPOSITE','Other',NULL,NULL,NULL,NULL,NULL,2);
  check_tariff('CBI1405R_CRD_8_21','8-21','TARIFF_8_21','تشکیل پرونده اعتباری اعم از تعهدات یا تسهیلات (به استثنای قرض الحسنه)','CBI_RIAL_1405_CREDIT_GROUP','CBI1405R_CRD','dd3c014982ea56ee0dade94153550186e0f3bfbae470225ebecfbad63edaf55c','FIXED','Flat',576000,NULL,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_CRD_8_22','8-22','TARIFF_8_22','پذیرش اوراق گواهی حق تقدم هنگام پرداخت تسهیلات','CBI_RIAL_1405_CREDIT_GROUP','CBI1405R_CRD','7879b154e086d5df1951032003e2f05bad3a69d3428922cb071d022ff2354443','PERCENTAGE','Percentage',NULL,0.001,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_CRD_8_23','8-23','TARIFF_8_23','ارائه صورت اطلاعات تسهیلات و تعهدات به درخواست مشتری','CBI_RIAL_1405_CREDIT_GROUP','CBI1405R_CRD','6b8e0c5eb21e8780e0fc49488b93589ffb74e28e8b02f2cb6897b1105af17446','PER_UNIT','PerUnit',120000,NULL,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_CRD_8_24','8-24','TARIFF_8_24','تغییر شرایط مصوبات ابلاغی صادره از سوی ارکان اعتباری بالاتر از رکن اعتباری شعبه به درخواست مشتری و منوط به صدور مصوبه جدید (تسهیلات/تعهدات)','CBI_RIAL_1405_CREDIT_GROUP','CBI1405R_CRD','0aed29bc95bbdb6476480e8eb30affd9f693b2af24dee48d6e860509d775f9d5','COMPOSITE','Other',NULL,NULL,NULL,NULL,NULL,2);
  check_tariff('CBI1405R_CRD_8_25','8-25','TARIFF_8_25','موافقت با تنظیم اجاره نامه بر روی هر واحد مورد رهن','CBI_RIAL_1405_CREDIT_GROUP','CBI1405R_CRD','53a7e57480c522d4777fadd61d06a8e04f9d1eacff8893d02ec72bfe260fe398','COMPOSITE','Other',NULL,NULL,NULL,NULL,NULL,2);
  check_tariff('CBI1405R_CRD_8_26','8-26','TARIFF_8_26','بررسی و تصویب درخواست امهال (7)','CBI_RIAL_1405_CREDIT_GROUP','CBI1405R_CRD','69624f4ea198d9073d13ccfc13069fd721748dd230070586e0f27e63a0f03a8c','PERCENTAGE_WITH_CAP','RateWithMaximumAmount',NULL,0.0002,NULL,120000000,NULL,1);
  check_tariff('CBI1405R_CRD_8_27','8-27','TARIFF_8_27','توقف عملیات اجرایی در هر نوبت بعد از صدور اجرائیه نسبت به قراردادهای رهنی، داخلی و چک به تقاضای بدهکار/راهن/سایر ذی نفعان (ضامنین، اشخاص ثالث و غیره)','CBI_RIAL_1405_CREDIT_GROUP','CBI1405R_CRD','4b8d412146891bb67df4dfae7eff41e5805eec65db1fb05e0edef96904047acf','PERCENTAGE_WITH_CAP','RateWithMaximumAmount',NULL,0.0002,NULL,120000000,NULL,1);
  check_tariff('CBI1405R_CRD_8_28','8-28','TARIFF_8_28','هزینه استعلام از سازمان ثبت احوال کشور','CBI_RIAL_1405_CREDIT_GROUP','CBI1405R_CRD','e5afc8ce7fbf253632fad8ed2290da4cae0b87c83087a39b8c0a2f41879cd9da','COMPOSITE','Other',NULL,NULL,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_CRD_8_29','8-29','TARIFF_8_29','ثبت و نگهداری سفته های تضمینی','CBI_RIAL_1405_CREDIT_GROUP','CBI1405R_CRD','a5866e26259700255b17557299e2bc70aa3be404e0b687854aa153cbcd3a52a2','COMPOSITE','Other',NULL,NULL,NULL,NULL,NULL,2);
  check_tariff('CBI1405R_CRD_8_30','8-30','TARIFF_8_30','انتقال قراردادهای تسهیلاتی','CBI_RIAL_1405_CREDIT_GROUP','CBI1405R_CRD','42491136c83022f093a491eccab0cb548776ce9ba64ec18f01295ab9ae7eab0d','FIXED','Flat',240000,NULL,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_CRD_8_31','8-31','TARIFF_8_31','موافقت با تجمیع دو یا چند پلاک به منظور پرداخت تسهیلات مشارکت','CBI_RIAL_1405_CREDIT_GROUP','CBI1405R_CRD','7e1c39c60f40747846a3b6f374bd3356a44b93a72d34f5e3694e1e94a5311ab9','FIXED','Flat',0,NULL,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_CRD_8_32','8-32','TARIFF_8_32','موافقت با تغییر و اصلاح حدود اربعه پلاک در رهن بانک','CBI_RIAL_1405_CREDIT_GROUP','CBI1405R_CRD','acdb07c883808e261a88e00ca971977c7c7cdbd7c7171bf8ce2d9e2d9904ae44','COMPOSITE','Other',NULL,NULL,NULL,NULL,NULL,2);
  check_tariff('CBI1405R_CRD_8_33','8-33','TARIFF_8_33','درخواست تمدید مدت تعویض وثیقه غیر همزمان','CBI_RIAL_1405_CREDIT_GROUP','CBI1405R_CRD','7268d25cf149ef55a4ad9245bc0aaab681215140d4eb20c13d1cf2dbe458e931','COMPOSITE','Other',NULL,NULL,NULL,NULL,NULL,2);
  check_tariff('CBI1405R_CRD_8_34','8-34','TARIFF_8_34','موافقت با تنظیم سند پیش فروش روی واحد مورد مشارکت بانک','CBI_RIAL_1405_CREDIT_GROUP','CBI1405R_CRD','0ada86deec81cb095330127ea5fd98ffe6783dd86be6d5b006b8ee5c72ddc7ad','COMPOSITE','Other',NULL,NULL,NULL,NULL,NULL,2);
  check_tariff('CBI1405R_CRD_8_35','8-35','TARIFF_8_35','تنظیم پیش نویس و ثبت سند رهنی در سیستم','CBI_RIAL_1405_CREDIT_GROUP','CBI1405R_CRD','6de3d76021bdb87f268bb67cedcd0fee51ea958b2fc2d758712ae2346fef5e79','COMPOSITE','Other',NULL,NULL,NULL,NULL,NULL,2);
  check_tariff('CBI1405R_CRD_8_36','8-36','TARIFF_8_36','تمدید پیش نویس قراردادهای رهنی پس از طی مهلت یک ماهه','CBI_RIAL_1405_CREDIT_GROUP','CBI1405R_CRD','e2e2d453d16fe28394deb3ecae555b5d54ba4233d4d5fa9ffab7886cb18f745c','FIXED','Flat',0,NULL,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_CRD_8_37','8-37','TARIFF_8_37','اصلاح یا برگشت مبلغ اضافه واریزی اقساط (به درخواست مشتری)','CBI_RIAL_1405_CREDIT_GROUP','CBI1405R_CRD','f04a6ac8dc5f05150874e73b98c32284b874ede1943a6643e041069752af9d41','COMPOSITE','Other',NULL,NULL,NULL,NULL,NULL,2);
  check_tariff('CBI1405R_CRD_8_38','8-38','TARIFF_8_38','تشکیل پرونده برای پرداخت بیمه غرامت فوت گیرندگان تسهیلات','CBI_RIAL_1405_CREDIT_GROUP','CBI1405R_CRD','de5e8e76d4ddff70773f7a7e029dd770bc87d13811a6d0bc26408d37062176cf','FIXED','Flat',0,NULL,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_CRD_8_39','8-39','TARIFF_8_39','وصول مطالبات اعم از برون سپاری به مؤسسات/شرکت های ذی ربط یا توسط خود مؤسسه اعتباری','CBI_RIAL_1405_CREDIT_GROUP','CBI1405R_CRD','4eee266c69d51d950ba7801453e530364b67020c5a7fa4063334f218f33392c0','FIXED','Flat',0,NULL,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_OTH_9_1','9-1','TARIFF_9_1','نگهداری مانده های مطالبه نشده و راکد (سالانه)','CBI_RIAL_1405_OTHER_GROUP','CBI1405R_OTH','570123c2d072f4e56c0d47b8a18be209c8dda19af028a3dd47a53420efc89fde','FIXED','Flat',30000,NULL,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_OTH_9_2','9-2','TARIFF_9_2','تهیه تصویر (صدور سند المثنی)/چاپ مجدد اسناد روزهای قبل به درخواست مشتری (1)','CBI_RIAL_1405_OTHER_GROUP','CBI1405R_OTH','c994a8bcd809726f1a72d7f9a9e609504f4bf88ac192b0b7b8dd80974616713b','FIXED','Flat',57600,NULL,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_OTH_9_3','9-3','TARIFF_9_3','بازیابی تصاویر دوربین های مدار بسته (اعم از داخل و بیرون) شعب به درخواست مشتری/دستور مرجع قضایی','CBI_RIAL_1405_OTHER_GROUP','CBI1405R_OTH','ca5436a37c3c199217d1e81e5fa0b1dde5d2459282034c5d2c7bc18d50de80bc','FIXED','Flat',0,NULL,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_OTH_9_4','9-4','TARIFF_9_4','اطلاعات درخواستی مشتریان جهت ارائه به بازرسان قانونی و حسابرسان مستقل آن ها','CBI_RIAL_1405_OTHER_GROUP','CBI1405R_OTH','4fa66ef56f6c62a102a2236c105d8d10e6ebceb916cdfbfb70afd9c1b415c968','PER_UNIT','PerUnit',1008000,NULL,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_OTH_9_5','9-5','TARIFF_9_5','گواهی اعلام بدهی و یا مانده هرگونه از حساب ها به مراجع ذی صلاح به درخواست مشتری (2)','CBI_RIAL_1405_OTHER_GROUP','CBI1405R_OTH','8641d7b2bc19b2f70e43c64ff43db3e1b5dc889c17e0f09dc20728cde9175be9','FIXED','Flat',288000,NULL,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_OTH_9_6','9-6','TARIFF_9_6','دریافت حضوری قبوض آب، برق، گاز، تلفن (ثابت-همراه)، شهرداری، راهنمایی و رانندگی و موارد مشابه (از سازمان/شرکت ذی ربط)','CBI_RIAL_1405_OTHER_GROUP','CBI1405R_OTH','1cfcb43f9348cef7b7f4e9880b9e4b677d57148e886c99d56a8e1b6835efced3','FIXED','Flat',57600,NULL,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_OTH_9_7','9-7','TARIFF_9_7','تأیید اصالت وکالتنامه','CBI_RIAL_1405_OTHER_GROUP','CBI1405R_OTH','2395a65ed4d4abfd41ec35bf5ac66193c73a15a820f880345563bab4c23b4190','FIXED','Flat',120000,NULL,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_OTH_9_8','9-8','TARIFF_9_8','ارائه تأییدیه شماره حساب و شماره شبا به سایر سازمان ها بنا به درخواست مشتری','CBI_RIAL_1405_OTHER_GROUP','CBI1405R_OTH','8204979b8ac1a6dc33706a82932d1aa6706ef1eaa9eeb9572b620331c6dd1ec5','COMPOSITE','Other',NULL,NULL,NULL,NULL,NULL,2);
  check_tariff('CBI1405R_OTH_9_9','9-9','TARIFF_9_9','محاسبه و پرداخت سهم الارث وراث از حساب متوفی (حسب مدارک مثبته)','CBI_RIAL_1405_OTHER_GROUP','CBI1405R_OTH','768aec6232bb2ae819938644eefe8babdb8d82ed30522e6fffd6fbb4f7dfa922','FIXED','Flat',0,NULL,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_OTH_9_10','9-10','TARIFF_9_10','اخذ کد بورسی به درخواست مشتری از طریق مراجعه به شعبه','CBI_RIAL_1405_OTHER_GROUP','CBI1405R_OTH','9d39ba5b15059e6552048331df16da43921ba6419166ee9a49940b97889265a5','FIXED','Flat',100000,NULL,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_OTH_9_11','9-11','TARIFF_9_11','ارائه خدمات مربوط به ETC برای بار اول','CBI_RIAL_1405_OTHER_GROUP','CBI1405R_OTH','452e8657af305582700b6c18a66b1516bfcf1da5daaa987c3c250dcceae9cce4','FIXED','Flat',40000,NULL,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_OTH_9_12','9-12','TARIFF_9_12','اخذ استعلام وضعیت اعتباری (تسهیلات و تعهدات) و ماده (21) حساب جاری از سامانه بانک مرکزی جهت ارائه به سایر سازمان ها و نهادها','CBI_RIAL_1405_OTHER_GROUP','CBI1405R_OTH','9de6a3119d673e1a4269f54db08bb23f25393b9b2e686f6de2b70d6f0e05c10b','FIXED','Flat',0,NULL,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_OTH_9_13','9-13','TARIFF_9_13','ارائه تصویر مدارک موجود در پرونده تسهیلاتی یا قراردادهای افتتاح حساب اشخاص حقیقی به تسهیلات گیرنده یا صاحب حساب','CBI_RIAL_1405_OTHER_GROUP','CBI1405R_OTH','ad9808b4c1e9616653d7cdc054d60d76abc7869854069dd93a1c6c8b8cea69fd','FIXED','Flat',0,NULL,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_OTH_9_14','9-14','TARIFF_9_14','استعلام مربوط به اخذ فرم ماده (186) قانون مالیات های مستقیم','CBI_RIAL_1405_OTHER_GROUP','CBI1405R_OTH','83205ec5d8e0f45ca2808f0a15fc6b9afbe6399ff8552cfcce1f9fd144b68a69','FIXED','Flat',140000,NULL,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_OTH_9_15','9-15','TARIFF_9_15','استعلام و به روزرسانی کدپستی به درخواست مشتری (دریافت نشانی، مختصات جغرافیایی، تولید گواهی کد پستی) از طریق مراجعه به شعبه','CBI_RIAL_1405_OTHER_GROUP','CBI1405R_OTH','2c29df92b90e197dce039e31fc43d52edd72b81759d4cd4dc430f5c2d7d1ce6d','FIXED','Flat',100000,NULL,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_OTH_9_16','9-16','TARIFF_9_16','انجام استعلام از مراجع ذی ربط (نظیر شهرداری، سازمان نظام وظیفه و شرکت مترو) جهت انجام/ارائه عملیات/خدمات بانکی','CBI_RIAL_1405_OTHER_GROUP','CBI1405R_OTH','644f503b26becb42358534ab25026ab8e7e4f7b2fcd94bba8502643fd3054470','FIXED','Flat',140000,NULL,NULL,NULL,NULL,1);
  check_tariff('CBI1405R_OTH_9_17','9-17','TARIFF_9_17','استعلام مالکیت شماره تلفن همراه از سامانه شاهکار','CBI_RIAL_1405_OTHER_GROUP','CBI1405R_OTH','f0aed4d29a2f1096be70bed9352df7cd49bce6927f69a0b901041f2f4744be4f','FIXED','Flat',100000,NULL,NULL,NULL,NULL,1);

  DBMS_OUTPUT.PUT_LINE('--- Checking 180 preserved source components ---');
  check_component('CBI1405R_GAR_1_1',1,'CONSTANT',1300000,'مقطوع 1,300,000 ریال','SRC:1-1-01:FIXED_AMOUNT','جزء منبع 1-1-01 | نوع=FIXED_AMOUNT | وضعیت=دارای کارمزد | مقطوع 1,300,000 ریال');
  check_component('CBI1405R_GAR_1_2',1,'CONSTANT',0.005,'0.5 درصد مبلغ در سال، حداقل 1,300,000 ریال','SRC:1-2-01:RATE','جزء منبع 1-2-01 | نوع=RATE | وضعیت=دارای کارمزد | محدودیت نرخ=دقیق | مبنا=مبلغ خدمت/معامله | تناوب=سالانه | 0.5 درصد مبلغ در سال، حداقل 1,300,000 ریال');
  check_component('CBI1405R_GAR_1_3',1,'CONSTANT',0.0075,'0.75 درصد مبلغ در سال، حداقل 1,300,000 ریال','SRC:1-3-01:RATE','جزء منبع 1-3-01 | نوع=RATE | وضعیت=دارای کارمزد | محدودیت نرخ=دقیق | مبنا=مبلغ خدمت/معامله | تناوب=سالانه | 0.75 درصد مبلغ در سال، حداقل 1,300,000 ریال');
  check_component('CBI1405R_GAR_1_4',1,'CONSTANT',0.0075,'0.75 درصد مبلغ در سال، حداقل 1,300,000 ریال','SRC:1-4-01:RATE','جزء منبع 1-4-01 | نوع=RATE | وضعیت=دارای کارمزد | محدودیت نرخ=دقیق | مبنا=مبلغ خدمت/معامله | تناوب=سالانه | 0.75 درصد مبلغ در سال، حداقل 1,300,000 ریال');
  check_component('CBI1405R_GAR_1_5',1,'CONSTANT',0.0075,'0.75 درصد مبلغ در سال، حداقل 1,300,000 ریال','SRC:1-5-01:RATE','جزء منبع 1-5-01 | نوع=RATE | وضعیت=دارای کارمزد | محدودیت نرخ=دقیق | مبنا=مبلغ خدمت/معامله | تناوب=سالانه | 0.75 درصد مبلغ در سال، حداقل 1,300,000 ریال');
  check_component('CBI1405R_GAR_1_6',1,'CONSTANT',0.0175,'1.75 درصد مبلغ در سال، حداقل 1,300,000 ریال','SRC:1-6-01:RATE','جزء منبع 1-6-01 | نوع=RATE | وضعیت=دارای کارمزد | محدودیت نرخ=دقیق | مبنا=مبلغ خدمت/معامله | تناوب=سالانه | 1.75 درصد مبلغ در سال، حداقل 1,300,000 ریال');
  check_component('CBI1405R_GAR_1_7',1,'CONSTANT',0.005,'0.5 درصد مبلغ در سال، حداقل 1,300,000 ریال','SRC:1-7-01:RATE','جزء منبع 1-7-01 | نوع=RATE | وضعیت=دارای کارمزد | محدودیت نرخ=دقیق | مبنا=مبلغ خدمت/معامله | تناوب=سالانه | 0.5 درصد مبلغ در سال، حداقل 1,300,000 ریال');
  check_component('CBI1405R_GAR_1_8',1,'CONSTANT',0.01,'1 درصد مبلغ در سال، حداقل 1,300,000 ریال','SRC:1-8-01:RATE','جزء منبع 1-8-01 | نوع=RATE | وضعیت=دارای کارمزد | محدودیت نرخ=دقیق | مبنا=مبلغ خدمت/معامله | تناوب=سالانه | 1 درصد مبلغ در سال، حداقل 1,300,000 ریال');
  check_component('CBI1405R_GAR_1_9',1,'CONSTANT',0.01,'1 درصد مبلغ در سال، حداقل 1,300,000 ریال','SRC:1-9-01:RATE','جزء منبع 1-9-01 | نوع=RATE | وضعیت=دارای کارمزد | محدودیت نرخ=دقیق | مبنا=مبلغ خدمت/معامله | تناوب=سالانه | 1 درصد مبلغ در سال، حداقل 1,300,000 ریال');
  check_component('CBI1405R_GAR_1_10',1,'CONSTANT',0.01,'1 درصد مبلغ در سال، حداقل 1,300,000 ریال','SRC:1-10-01:RATE','جزء منبع 1-10-01 | نوع=RATE | وضعیت=دارای کارمزد | محدودیت نرخ=دقیق | مبنا=مبلغ خدمت/معامله | تناوب=سالانه | 1 درصد مبلغ در سال، حداقل 1,300,000 ریال');
  check_component('CBI1405R_GAR_1_11',1,'CONSTANT',0.015,'1.5 درصد مبلغ در سال، حداقل 1,300,000 ریال','SRC:1-11-01:RATE','جزء منبع 1-11-01 | نوع=RATE | وضعیت=دارای کارمزد | محدودیت نرخ=دقیق | مبنا=مبلغ خدمت/معامله | تناوب=سالانه | 1.5 درصد مبلغ در سال، حداقل 1,300,000 ریال');
  check_component('CBI1405R_GAR_1_12',1,'CONSTANT',0.015,'1.5 درصد مبلغ در سال، حداقل 1,300,000 ریال','SRC:1-12-01:RATE','جزء منبع 1-12-01 | نوع=RATE | وضعیت=دارای کارمزد | محدودیت نرخ=دقیق | مبنا=مبلغ خدمت/معامله | تناوب=سالانه | 1.5 درصد مبلغ در سال، حداقل 1,300,000 ریال');
  check_component('CBI1405R_GAR_1_13',1,'CONSTANT',0.0225,'2.25 درصد مبلغ در سال، حداقل 1,300,000 ریال','SRC:1-13-01:RATE','جزء منبع 1-13-01 | نوع=RATE | وضعیت=دارای کارمزد | محدودیت نرخ=دقیق | مبنا=مبلغ خدمت/معامله | تناوب=سالانه | 2.25 درصد مبلغ در سال، حداقل 1,300,000 ریال');
  check_component('CBI1405R_GAR_1_14',1,'CONSTANT',0.02,'2 درصد مبلغ در سال، حداقل 1,300,000 ریال','SRC:1-14-01:RATE','جزء منبع 1-14-01 | نوع=RATE | وضعیت=دارای کارمزد | محدودیت نرخ=دقیق | مبنا=مبلغ خدمت/معامله | تناوب=سالانه | 2 درصد مبلغ در سال، حداقل 1,300,000 ریال');
  check_component('CBI1405R_GAR_1_15',1,'CONSTANT',0.0225,'2.25 درصد در سال','SRC:1-15-01:RATE','جزء منبع 1-15-01 | نوع=RATE | وضعیت=دارای کارمزد | محدودیت نرخ=دقیق | تناوب=سالانه | 2.25 درصد در سال');
  check_component('CBI1405R_GAR_1_16',1,'EXTERNAL_VALUE',NULL,'کارمزد تمدید برابر با نرخ صدور ضمانتنامه در زمان تمدید؛ حداقل 1,300,000 ریال','SRC:1-16-01:REFERENCE','جزء منبع 1-16-01 | نوع=REFERENCE | وضعیت=تعرفه مرجع | مرجع=نرخ صدور ضمانتنامه در زمان تمدید | شرط=تمدید ضمانتنامه | کارمزد تمدید برابر با نرخ صدور ضمانتنامه در زمان تمدید؛ حداقل 1,300,000 ریال');
  check_component('CBI1405R_GAR_1_17',1,'EXTERNAL_VALUE',NULL,'کارمزد اخذشده در هنگام صدور نسبت به مابه التفاوت مبلغ برای مدت باقی مانده پس از کسر یک ماه به نفع بانک محاسبه و مسترد می گردد؛ حداقل مبلغ کارمزد صدور ضمانتنامه (1,300,000 ریال) غیرقابل برگشت می باشد','SRC:1-17-01:REFUND_FORMULA','جزء منبع 1-17-01 | نوع=REFUND_FORMULA | وضعیت=فرمول استرداد | مبنا=مابه التفاوت مبلغ و مدت باقی مانده | شرط=پس از کسر یک ماه به نفع بانک؛ حداقل کارمزد صدور غیرقابل برگشت | کارمزد اخذشده در هنگام صدور نسبت به مابه التفاوت مبلغ برای مدت باقی مانده پس از کسر یک ماه به نفع بانک محاسبه و مسترد می گردد؛ حداقل مبلغ کارمزد صدور ضمانتنامه (1,300,000 ریال) غیرقابل برگشت می باشد');
  check_component('CBI1405R_GAR_1_18',1,'EXTERNAL_VALUE',NULL,'کارمزد اخذشده در هنگام صدور نسبت به مابه التفاوت مبلغ برای مدت باقی مانده پس از کسر یک ماه به نفع بانک محاسبه و مسترد می گردد؛ حداقل مبلغ کارمزد صدور ضمانتنامه (1,300,000 ریال) غیرقابل برگشت می باشد','SRC:1-18-01:REFUND_FORMULA','جزء منبع 1-18-01 | نوع=REFUND_FORMULA | وضعیت=فرمول استرداد | مبنا=مابه التفاوت مبلغ و مدت باقی مانده | شرط=پس از کسر یک ماه به نفع بانک؛ حداقل کارمزد صدور غیرقابل برگشت | کارمزد اخذشده در هنگام صدور نسبت به مابه التفاوت مبلغ برای مدت باقی مانده پس از کسر یک ماه به نفع بانک محاسبه و مسترد می گردد؛ حداقل مبلغ کارمزد صدور ضمانتنامه (1,300,000 ریال) غیرقابل برگشت می باشد');
  check_component('CBI1405R_GAR_1_19',1,'CONSTANT',1300000,'مقطوع 1,300,000 ریال','SRC:1-19-01:FIXED_AMOUNT','جزء منبع 1-19-01 | نوع=FIXED_AMOUNT | وضعیت=دارای کارمزد | مقطوع 1,300,000 ریال');
  check_component('CBI1405R_GAR_1_20',1,'CONSTANT',0.0005,'حداکثر معادل نیم در هزار مبلغ ضمانتنامه صرفاً بر عهده تأمین کننده، با لحاظ حداقل مبلغ کارمزد 1,300,000 ریال مشابه کارمزد صدور ضمانتنامه','SRC:1-20-01:RATE','جزء منبع 1-20-01 | نوع=RATE | وضعیت=دارای کارمزد | محدودیت نرخ=حداکثر | مبنا=مبلغ ضمانتنامه | شرط=صرفاً بر عهده تأمین کننده | حداکثر معادل نیم در هزار مبلغ ضمانتنامه صرفاً بر عهده تأمین کننده، با لحاظ حداقل مبلغ کارمزد 1,300,000 ریال مشابه کارمزد صدور ضمانتنامه');
  check_component('CBI1405R_REM_2_1',1,'CONSTANT',144000,'هر فقره 144,000 ریال','SRC:2-1-01:FIXED_AMOUNT','جزء منبع 2-1-01 | نوع=FIXED_AMOUNT | وضعیت=دارای کارمزد | واحد=فقره | هر فقره 144,000 ریال');
  check_component('CBI1405R_REM_2_2',1,'CONSTANT',1152000,'مقطوع 1,152,000 ریال','SRC:2-2-01:FIXED_AMOUNT','جزء منبع 2-2-01 | نوع=FIXED_AMOUNT | وضعیت=دارای کارمزد | مقطوع 1,152,000 ریال');
  check_component('CBI1405R_REM_2_3',1,'CONSTANT',144000,'هر فقره 144,000 ریال','SRC:2-3-01:FIXED_AMOUNT','جزء منبع 2-3-01 | نوع=FIXED_AMOUNT | وضعیت=دارای کارمزد | واحد=فقره | هر فقره 144,000 ریال');
  check_component('CBI1405R_REM_2_4',1,'CONSTANT',288000,'هر فقره 288,000 ریال','SRC:2-4-01:FIXED_AMOUNT','جزء منبع 2-4-01 | نوع=FIXED_AMOUNT | وضعیت=دارای کارمزد | واحد=فقره | هر فقره 288,000 ریال');
  check_component('CBI1405R_SDB_3_1',1,'CONSTANT',288,'هر سانتی متر مکعب حجم 288 ریال (حداقل 1,440,000 ریال)؛ همچنین ودیعه اجاره 8 برابر مبلغ اجاره بها، حداقل مبلغ 11,520,000 ریال','SRC:3-1-01:RATE_UNIT','جزء منبع 3-1-01 | نوع=RATE_UNIT | وضعیت=دارای کارمزد | مبنا=حجم صندوق | واحد=سانتی متر مکعب | تناوب=سالانه | شرط=اجاره بها | 288 ریال به ازای هر سانتی متر مکعب حجم؛ حداقل 1,440,000 ریال');
  check_component('CBI1405R_SDB_3_1',2,'CONSTANT',NULL,'هر سانتی متر مکعب حجم 288 ریال (حداقل 1,440,000 ریال)؛ همچنین ودیعه اجاره 8 برابر مبلغ اجاره بها، حداقل مبلغ 11,520,000 ریال','SRC:3-1-02:DEPOSIT_MULTIPLIER','جزء منبع 3-1-02 | نوع=DEPOSIT_MULTIPLIER | وضعیت=دارای ودیعه | مبنا=اجاره بها | واحد=سال | تناوب=سالانه | شرط=ودیعه اجاره | ودیعه اجاره برابر 8 برابر مبلغ اجاره بها؛ حداقل 11,520,000 ریال');
  check_component('CBI1405R_SDB_3_2',1,'CONSTANT',120000,'120,000 ریال','SRC:3-2-01:FIXED_AMOUNT','جزء منبع 3-2-01 | نوع=FIXED_AMOUNT | وضعیت=دارای کارمزد | 120,000 ریال');
  check_component('CBI1405R_SDB_3_3',1,'CONSTANT',288000,'تمامی هزینه های مربوط به قفل ها و کلیدها + 288,000 ریال','SRC:3-3-01:FIXED_AMOUNT','جزء منبع 3-3-01 | نوع=FIXED_AMOUNT | وضعیت=دارای کارمزد | هزینه اضافی=تمامی هزینه های مربوط به قفل ها و کلیدها | تمامی هزینه های مربوط به قفل ها و کلیدها + 288,000 ریال');
  check_component('CBI1405R_SDB_3_4',1,'CONSTANT',0,'بدون کارمزد','SRC:3-4-01:NO_FEE','جزء منبع 3-4-01 | نوع=NO_FEE | وضعیت=بدون کارمزد | بدون کارمزد');
  check_component('CBI1405R_SDB_3_5',1,'CONSTANT',0,'بدون کارمزد','SRC:3-5-01:NO_FEE','جزء منبع 3-5-01 | نوع=NO_FEE | وضعیت=بدون کارمزد | بدون کارمزد');
  check_component('CBI1405R_SEC_4_1',1,'CONSTANT',66000,'66,000 ریال','SRC:4-1-01:FIXED_AMOUNT','جزء منبع 4-1-01 | نوع=FIXED_AMOUNT | وضعیت=دارای کارمزد | 66,000 ریال');
  check_component('CBI1405R_SEC_4_2',1,'CONSTANT',110000,'110,000 ریال','SRC:4-2-01:FIXED_AMOUNT','جزء منبع 4-2-01 | نوع=FIXED_AMOUNT | وضعیت=دارای کارمزد | 110,000 ریال');
  check_component('CBI1405R_SEC_4_3',1,'EXTERNAL_VALUE',NULL,'حداقل به میزان کارمزد صدور ضمانتنامه ها متناسب با وثایق و حداکثر تا دو برابر آن به مأخذ مجموع مانده اصل و سود اوراق بدهی در پایان هر سال','SRC:4-3-01:FORMULA','جزء منبع 4-3-01 | نوع=FORMULA | وضعیت=فرمولی | مبنا=مجموع مانده اصل و سود اوراق بدهی در پایان هر سال | تناوب=سالانه | مرجع=کارمزد صدور ضمانتنامه متناسب با وثایق | شرط=حداقل برابر کارمزد صدور ضمانتنامه و حداکثر دو برابر آن | حداقل به میزان کارمزد صدور ضمانتنامه ها متناسب با وثایق و حداکثر تا دو برابر آن به مأخذ مجموع مانده اصل و سود اوراق بدهی در پایان هر سال');
  check_component('CBI1405R_SEC_4_4',1,'CONSTANT',0.0005,'نیم در هزار مبلغ مورد انتقال، حداکثر 480,000 ریال','SRC:4-4-01:RATE','جزء منبع 4-4-01 | نوع=RATE | وضعیت=دارای کارمزد | محدودیت نرخ=دقیق | مبنا=مبلغ مورد انتقال | نیم در هزار مبلغ مورد انتقال، حداکثر 480,000 ریال');
  check_component('CBI1405R_SEC_4_5',1,'EXTERNAL_VALUE',NULL,'حداکثر به میزان کارمزد صدور ضمانتنامه ها و متناسب با وثایق دریافتی','SRC:4-5-01:REFERENCE','جزء منبع 4-5-01 | نوع=REFERENCE | وضعیت=تعرفه مرجع | مرجع=کارمزد صدور ضمانتنامه متناسب با وثایق دریافتی | شرط=حداکثر | حداکثر به میزان کارمزد صدور ضمانتنامه ها و متناسب با وثایق دریافتی');
  check_component('CBI1405R_SEC_4_6',1,'CONSTANT',0.001,'حداکثر به میزان یک دهم درصد مبلغ اوراق انتقال یافته','SRC:4-6-01:RATE','جزء منبع 4-6-01 | نوع=RATE | وضعیت=دارای کارمزد | محدودیت نرخ=حداکثر | مبنا=مبلغ اوراق انتقال یافته | حداکثر به میزان یک دهم درصد مبلغ اوراق انتقال یافته');
  check_component('CBI1405R_SEC_4_7',1,'CONSTANT',0.0005,'حداکثر نیم در هزار مبلغ ابطال اوراق','SRC:4-7-01:RATE','جزء منبع 4-7-01 | نوع=RATE | وضعیت=دارای کارمزد | محدودیت نرخ=حداکثر | مبنا=مبلغ ابطال اوراق | حداکثر نیم در هزار مبلغ ابطال اوراق');
  check_component('CBI1405R_SEC_4_8',1,'CONSTANT',0.0005,'حداکثر نیم در هزار مبلغ مورد انتقال','SRC:4-8-01:RATE','جزء منبع 4-8-01 | نوع=RATE | وضعیت=دارای کارمزد | محدودیت نرخ=حداکثر | مبنا=مبلغ مورد انتقال | حداکثر نیم در هزار مبلغ مورد انتقال');
  check_component('CBI1405R_SEC_4_9',1,'CONSTANT',0.0005,'حداکثر نیم در هزار مبلغ تنزیل شده','SRC:4-9-01:RATE','جزء منبع 4-9-01 | نوع=RATE | وضعیت=دارای کارمزد | محدودیت نرخ=حداکثر | مبنا=مبلغ تنزیل شده | حداکثر نیم در هزار مبلغ تنزیل شده');
  check_component('CBI1405R_SEC_4_10',1,'CONSTANT',0.0005,'حداکثر نیم در هزار مبلغ مورد توثیق','SRC:4-10-01:RATE','جزء منبع 4-10-01 | نوع=RATE | وضعیت=دارای کارمزد | محدودیت نرخ=حداکثر | مبنا=مبلغ مورد توثیق | حداکثر نیم در هزار مبلغ مورد توثیق');
  check_component('CBI1405R_SEC_4_11',1,'CONSTANT',0.0005,'حداکثر معادل نیم در هزار مبلغ توافقی واریزی به حساب تأمین کننده','SRC:4-11-01:RATE','جزء منبع 4-11-01 | نوع=RATE | وضعیت=دارای کارمزد | محدودیت نرخ=حداکثر | مبنا=مبلغ توافقی واریزی به حساب تأمین کننده | حداکثر معادل نیم در هزار مبلغ توافقی واریزی به حساب تأمین کننده');
  check_component('CBI1405R_SEC_4_12',1,'CONSTANT',0.0015,'حداکثر معادل یک و نیم در هزار مبلغ توافقی واریزی به حساب تأمین کننده','SRC:4-12-01:RATE','جزء منبع 4-12-01 | نوع=RATE | وضعیت=دارای کارمزد | محدودیت نرخ=حداکثر | مبنا=مبلغ توافقی واریزی به حساب تأمین کننده | حداکثر معادل یک و نیم در هزار مبلغ توافقی واریزی به حساب تأمین کننده');
  check_component('CBI1405R_SEC_4_13',1,'CONSTANT',0.03,'حداکثر به میزان 3 درصد','SRC:4-13-01:RATE','جزء منبع 4-13-01 | نوع=RATE | وضعیت=دارای کارمزد | محدودیت نرخ=حداکثر | حداکثر به میزان 3 درصد');
  check_component('CBI1405R_SEC_4_14',1,'CONSTANT',0.005,'حداکثر به میزان 0.5 درصد سالانه','SRC:4-14-01:RATE','جزء منبع 4-14-01 | نوع=RATE | وضعیت=دارای کارمزد | محدودیت نرخ=حداکثر | تناوب=سالانه | حداکثر به میزان 0.5 درصد سالانه');
  check_component('CBI1405R_COL_5_1',1,'CONSTANT',144000,'هر فقره 144,000 ریال + هزینه پست','SRC:5-1-01:FIXED_AMOUNT','جزء منبع 5-1-01 | نوع=FIXED_AMOUNT | وضعیت=دارای کارمزد | واحد=فقره | هزینه اضافی=هزینه پست | هر فقره 144,000 ریال + هزینه پست');
  check_component('CBI1405R_COL_5_2',1,'CONSTANT',86400,'برای هر نوبت 86,400 ریال + هزینه پست','SRC:5-2-01:FIXED_AMOUNT','جزء منبع 5-2-01 | نوع=FIXED_AMOUNT | وضعیت=دارای کارمزد | واحد=نوبت | تناوب=هر نوبت | هزینه اضافی=هزینه پست | برای هر نوبت 86,400 ریال + هزینه پست');
  check_component('CBI1405R_COL_5_3',1,'CONSTANT',1152000,'برای هر برگ 1,152,000 ریال','SRC:5-3-01:FIXED_AMOUNT','جزء منبع 5-3-01 | نوع=FIXED_AMOUNT | وضعیت=دارای کارمزد | واحد=برگ | برای هر برگ 1,152,000 ریال');
  check_component('CBI1405R_COL_5_4',1,'CONSTANT',120000,'برای هر برگ 120,000 ریال، حداکثر 2,400,000 ریال','SRC:5-4-01:FIXED_AMOUNT','جزء منبع 5-4-01 | نوع=FIXED_AMOUNT | وضعیت=دارای کارمزد | واحد=برگ | برای هر برگ 120,000 ریال، حداکثر 2,400,000 ریال');
  check_component('CBI1405R_COL_5_5',1,'EXTERNAL_VALUE',NULL,'مشابه کارمزد صدور ضمانتنامه و متناسب با وثایق دریافتی','SRC:5-5-01:REFERENCE','جزء منبع 5-5-01 | نوع=REFERENCE | وضعیت=تعرفه مرجع | مرجع=کارمزد صدور ضمانتنامه متناسب با وثایق دریافتی | مشابه کارمزد صدور ضمانتنامه و متناسب با وثایق دریافتی');
  check_component('CBI1405R_COL_5_6',1,'CONSTANT',0.001,'حداکثر یک در هزار مبلغ برات انتقال یافته','SRC:5-6-01:RATE','جزء منبع 5-6-01 | نوع=RATE | وضعیت=دارای کارمزد | محدودیت نرخ=حداکثر | مبنا=مبلغ خدمت/معامله | حداکثر یک در هزار مبلغ برات انتقال یافته');
  check_component('CBI1405R_ACC_6_1_1',1,'CONSTANT',288000,'هر برگ چک 288,000 ریال','SRC:6-1-1-01:FIXED_AMOUNT','جزء منبع 6-1-1-01 | نوع=FIXED_AMOUNT | وضعیت=دارای کارمزد | واحد=برگ چک | هر برگ چک 288,000 ریال');
  check_component('CBI1405R_ACC_6_1_2',1,'CONSTANT',288000,'288,000 ریال','SRC:6-1-2-01:FIXED_AMOUNT','جزء منبع 6-1-2-01 | نوع=FIXED_AMOUNT | وضعیت=دارای کارمزد | 288,000 ریال');
  check_component('CBI1405R_ACC_6_1_3',1,'CONSTANT',288000,'288,000 ریال','SRC:6-1-3-01:FIXED_AMOUNT','جزء منبع 6-1-3-01 | نوع=FIXED_AMOUNT | وضعیت=دارای کارمزد | 288,000 ریال');
  check_component('CBI1405R_ACC_6_1_4',1,'CONSTANT',864000,'864,000 ریال + هزینه پست در صورت وجود','SRC:6-1-4-01:FIXED_AMOUNT','جزء منبع 6-1-4-01 | نوع=FIXED_AMOUNT | وضعیت=دارای کارمزد | هزینه اضافی=هزینه پست | 864,000 ریال + هزینه پست در صورت وجود');
  check_component('CBI1405R_ACC_6_1_5',1,'CONSTANT',NULL,'حداقل 120,000 ریال + هزینه استعلام (هزینه استعلام از سامانه های ذی ربط نظیر سامانه شاهکار می باشد)','SRC:6-1-5-01:FIXED_AMOUNT','جزء منبع 6-1-5-01 | نوع=FIXED_AMOUNT | وضعیت=دارای کارمزد | هزینه اضافی=هزینه استعلام | حداقل 120,000 ریال + هزینه استعلام (هزینه استعلام از سامانه های ذی ربط نظیر سامانه شاهکار می باشد)');
  check_component('CBI1405R_ACC_6_1_6',1,'CONSTANT',120000,'120,000 ریال','SRC:6-1-6-01:FIXED_AMOUNT','جزء منبع 6-1-6-01 | نوع=FIXED_AMOUNT | وضعیت=دارای کارمزد | 120,000 ریال');
  check_component('CBI1405R_ACC_6_1_7',1,'CONSTANT',28800,'هر فقره 28,800 ریال','SRC:6-1-7-01:FIXED_AMOUNT','جزء منبع 6-1-7-01 | نوع=FIXED_AMOUNT | وضعیت=دارای کارمزد | واحد=فقره | هر فقره 28,800 ریال');
  check_component('CBI1405R_ACC_6_1_8',1,'CONSTANT',144000,'از هر حساب 144,000 ریال','SRC:6-1-8-01:FIXED_AMOUNT','جزء منبع 6-1-8-01 | نوع=FIXED_AMOUNT | وضعیت=دارای کارمزد | واحد=حساب | از هر حساب 144,000 ریال');
  check_component('CBI1405R_ACC_6_1_9',1,'EXTERNAL_VALUE',NULL,'مطابق تعرفه ابلاغی بانک مرکزی به شرکت رتبه بندی اعتباری ایران','SRC:6-1-9-01:REFERENCE','جزء منبع 6-1-9-01 | نوع=REFERENCE | وضعیت=تعرفه مرجع | مرجع=تعرفه ابلاغی بانک مرکزی به شرکت رتبه بندی اعتباری ایران | مطابق تعرفه ابلاغی بانک مرکزی به شرکت رتبه بندی اعتباری ایران');
  check_component('CBI1405R_ACC_6_1_10',1,'CONSTANT',120000,'120,000 ریال + مبلغ پرداختی به چاپخانه دولتی (بهای تمام شده) + هزینه تمبر مالیاتی','SRC:6-1-10-01:FIXED_AMOUNT','جزء منبع 6-1-10-01 | نوع=FIXED_AMOUNT | وضعیت=دارای کارمزد | هزینه اضافی=هزینه تمبر مالیاتی + مبلغ پرداختی به چاپخانه دولتی | 120,000 ریال + مبلغ پرداختی به چاپخانه دولتی (بهای تمام شده) + هزینه تمبر مالیاتی');
  check_component('CBI1405R_ACC_6_1_11',1,'CONSTANT',44000,'44,000 ریال','SRC:6-1-11-01:FIXED_AMOUNT','جزء منبع 6-1-11-01 | نوع=FIXED_AMOUNT | وضعیت=دارای کارمزد | 44,000 ریال');
  check_component('CBI1405R_ACC_6_1_12',1,'CONSTANT',22000,'22,000 ریال','SRC:6-1-12-01:FIXED_AMOUNT','جزء منبع 6-1-12-01 | نوع=FIXED_AMOUNT | وضعیت=دارای کارمزد | 22,000 ریال');
  check_component('CBI1405R_ACC_6_1_13',1,'CONSTANT',52800,'52,800 ریال','SRC:6-1-13-01:FIXED_AMOUNT','جزء منبع 6-1-13-01 | نوع=FIXED_AMOUNT | وضعیت=دارای کارمزد | 52,800 ریال');
  check_component('CBI1405R_ACC_6_1_14',1,'CONSTANT',48000,'48,000 ریال','SRC:6-1-14-01:FIXED_AMOUNT','جزء منبع 6-1-14-01 | نوع=FIXED_AMOUNT | وضعیت=دارای کارمزد | 48,000 ریال');
  check_component('CBI1405R_ACC_6_1_15',1,'CONSTANT',12000,'بابت هر برگ چک 12,000 ریال','SRC:6-1-15-01:FIXED_AMOUNT','جزء منبع 6-1-15-01 | نوع=FIXED_AMOUNT | وضعیت=دارای کارمزد | واحد=برگ چک | بابت هر برگ چک 12,000 ریال');
  check_component('CBI1405R_ACC_6_1_16',1,'CONSTANT',140000,'140,000 ریال','SRC:6-1-16-01:FIXED_AMOUNT','جزء منبع 6-1-16-01 | نوع=FIXED_AMOUNT | وضعیت=دارای کارمزد | 140,000 ریال');
  check_component('CBI1405R_ACC_6_2_1',1,'CONSTANT',0,'بدون کارمزد','SRC:6-2-1-01:NO_FEE','جزء منبع 6-2-1-01 | نوع=NO_FEE | وضعیت=بدون کارمزد | بدون کارمزد');
  check_component('CBI1405R_ACC_6_2_2',1,'CONSTANT',120000,'120,000 ریال','SRC:6-2-2-01:FIXED_AMOUNT','جزء منبع 6-2-2-01 | نوع=FIXED_AMOUNT | وضعیت=دارای کارمزد | 120,000 ریال');
  check_component('CBI1405R_ACC_6_2_3',1,'CONSTANT',96000,'96,000 ریال','SRC:6-2-3-01:FIXED_AMOUNT','جزء منبع 6-2-3-01 | نوع=FIXED_AMOUNT | وضعیت=دارای کارمزد | 96,000 ریال');
  check_component('CBI1405R_ACC_6_3_1',1,'CONSTANT',201500,'201,500 ریال','SRC:6-3-1-01:FIXED_AMOUNT','جزء منبع 6-3-1-01 | نوع=FIXED_AMOUNT | وضعیت=دارای کارمزد | 201,500 ریال');
  check_component('CBI1405R_ACC_6_3_2',1,'CONSTANT',432000,'هر فقره 432,000 ریال','SRC:6-3-2-01:FIXED_AMOUNT','جزء منبع 6-3-2-01 | نوع=FIXED_AMOUNT | وضعیت=دارای کارمزد | واحد=فقره | هر فقره 432,000 ریال');
  check_component('CBI1405R_ACC_6_3_3',1,'CONSTANT',132000,'132,000 ریال','SRC:6-3-3-01:FIXED_AMOUNT','جزء منبع 6-3-3-01 | نوع=FIXED_AMOUNT | وضعیت=دارای کارمزد | 132,000 ریال');
  check_component('CBI1405R_ACC_6_4_1',1,'CONSTANT',14500,'هر صفحه 14,500 ریال','SRC:6-4-1-01:FIXED_AMOUNT','جزء منبع 6-4-1-01 | نوع=FIXED_AMOUNT | وضعیت=دارای کارمزد | واحد=صفحه | هر صفحه 14,500 ریال');
  check_component('CBI1405R_ACC_6_4_2',1,'CONSTANT',96000,'96,000 ریال','SRC:6-4-2-01:FIXED_AMOUNT','جزء منبع 6-4-2-01 | نوع=FIXED_AMOUNT | وضعیت=دارای کارمزد | 96,000 ریال');
  check_component('CBI1405R_ACC_6_5_1',1,'CONSTANT',43000,'43,000 ریال','SRC:6-5-1-01:FIXED_AMOUNT','جزء منبع 6-5-1-01 | نوع=FIXED_AMOUNT | وضعیت=دارای کارمزد | 43,000 ریال');
  check_component('CBI1405R_ACC_6_5_2',1,'CONSTANT',72000,'72,000 ریال','SRC:6-5-2-01:FIXED_AMOUNT','جزء منبع 6-5-2-01 | نوع=FIXED_AMOUNT | وضعیت=دارای کارمزد | 72,000 ریال');
  check_component('CBI1405R_ACC_6_5_3',1,'CONSTANT',52000,'هر حساب 52,000 ریال','SRC:6-5-3-01:FIXED_AMOUNT','جزء منبع 6-5-3-01 | نوع=FIXED_AMOUNT | وضعیت=دارای کارمزد | واحد=حساب | هر حساب 52,000 ریال');
  check_component('CBI1405R_ACC_6_5_4',1,'CONSTANT',52000,'هر حساب 52,000 ریال','SRC:6-5-4-01:FIXED_AMOUNT','جزء منبع 6-5-4-01 | نوع=FIXED_AMOUNT | وضعیت=دارای کارمزد | واحد=حساب | هر حساب 52,000 ریال');
  check_component('CBI1405R_ACC_6_5_5',1,'CONSTANT',145000,'145,000 ریال بابت هر بار انتقال','SRC:6-5-5-01:FIXED_AMOUNT','جزء منبع 6-5-5-01 | نوع=FIXED_AMOUNT | وضعیت=دارای کارمزد | واحد=انتقال | تناوب=هر بار | 145,000 ریال بابت هر بار انتقال');
  check_component('CBI1405R_ACC_6_5_6',1,'CONSTANT',72000,'72,000 ریال','SRC:6-5-6-01:FIXED_AMOUNT','جزء منبع 6-5-6-01 | نوع=FIXED_AMOUNT | وضعیت=دارای کارمزد | 72,000 ریال');
  check_component('CBI1405R_ACC_6_5_7',1,'CONSTANT',48000,'برای اشخاص حقیقی: 48,000 ریال؛ برای اشخاص حقوقی: 120,000 ریال','SRC:6-5-7-01:FIXED_AMOUNT','جزء منبع 6-5-7-01 | نوع=FIXED_AMOUNT | وضعیت=دارای کارمزد | گروه مشتری=حقیقی | برای اشخاص حقیقی: 48,000 ریال');
  check_component('CBI1405R_ACC_6_5_7',2,'CONSTANT',120000,'برای اشخاص حقیقی: 48,000 ریال؛ برای اشخاص حقوقی: 120,000 ریال','SRC:6-5-7-02:FIXED_AMOUNT','جزء منبع 6-5-7-02 | نوع=FIXED_AMOUNT | وضعیت=دارای کارمزد | گروه مشتری=حقوقی | برای اشخاص حقوقی: 120,000 ریال');
  check_component('CBI1405R_ACC_6_5_8',1,'CONSTANT',48000,'بابت برقراری خدمت 48,000 ریال برای هر نوبت و به ازای هر بار تراکنش 4,800 ریال','SRC:6-5-8-01:FIXED_AMOUNT','جزء منبع 6-5-8-01 | نوع=FIXED_AMOUNT | وضعیت=دارای کارمزد | واحد=نوبت | شرط=برقراری خدمت | 48,000 ریال بابت برقراری خدمت برای هر نوبت');
  check_component('CBI1405R_ACC_6_5_8',2,'CONSTANT',4800,'بابت برقراری خدمت 48,000 ریال برای هر نوبت و به ازای هر بار تراکنش 4,800 ریال','SRC:6-5-8-02:FIXED_AMOUNT','جزء منبع 6-5-8-02 | نوع=FIXED_AMOUNT | وضعیت=دارای کارمزد | واحد=تراکنش | شرط=هر بار تراکنش | 4,800 ریال به ازای هر بار تراکنش');
  check_component('CBI1405R_ACC_6_5_9',1,'CONSTANT',0,'برای اشخاص حقیقی: بدون کارمزد؛ برای اشخاص حقوقی: 100,000 ریال برای هر بار تعریف','SRC:6-5-9-01:NO_FEE','جزء منبع 6-5-9-01 | نوع=NO_FEE | وضعیت=بدون کارمزد | گروه مشتری=حقیقی | برای اشخاص حقیقی: بدون کارمزد');
  check_component('CBI1405R_ACC_6_5_9',2,'CONSTANT',100000,'برای اشخاص حقیقی: بدون کارمزد؛ برای اشخاص حقوقی: 100,000 ریال برای هر بار تعریف','SRC:6-5-9-02:FIXED_AMOUNT','جزء منبع 6-5-9-02 | نوع=FIXED_AMOUNT | وضعیت=دارای کارمزد | گروه مشتری=حقوقی | تناوب=هر بار | برای اشخاص حقوقی: 100,000 ریال برای هر بار تعریف');
  check_component('CBI1405R_ACC_6_5_10',1,'CONSTANT',220000,'220,000 ریال + هزینه پست در صورت وجود','SRC:6-5-10-01:FIXED_AMOUNT','جزء منبع 6-5-10-01 | نوع=FIXED_AMOUNT | وضعیت=دارای کارمزد | هزینه اضافی=هزینه پست | 220,000 ریال + هزینه پست در صورت وجود');
  check_component('CBI1405R_ACC_6_5_11',1,'CONSTANT',0,'بدون کارمزد','SRC:6-5-11-01:NO_FEE','جزء منبع 6-5-11-01 | نوع=NO_FEE | وضعیت=بدون کارمزد | بدون کارمزد');
  check_component('CBI1405R_ACC_6_5_12',1,'CONSTANT',24000,'24,000 ریال','SRC:6-5-12-01:FIXED_AMOUNT','جزء منبع 6-5-12-01 | نوع=FIXED_AMOUNT | وضعیت=دارای کارمزد | 24,000 ریال');
  check_component('CBI1405R_ACC_6_5_13',1,'CONSTANT',0.01,'یک درصد اعتبار تخصیص یافته به دارنده کارت','SRC:6-5-13-01:RATE','جزء منبع 6-5-13-01 | نوع=RATE | وضعیت=دارای کارمزد | محدودیت نرخ=دقیق | مبنا=اعتبار تخصیص یافته به دارنده کارت | یک درصد اعتبار تخصیص یافته به دارنده کارت');
  check_component('CBI1405R_ACC_6_5_14',1,'CONSTANT',40000,'40,000 ریال بابت هر شماره پیگیری','SRC:6-5-14-01:FIXED_AMOUNT','جزء منبع 6-5-14-01 | نوع=FIXED_AMOUNT | وضعیت=دارای کارمزد | واحد=شماره پیگیری | 40,000 ریال بابت هر شماره پیگیری');
  check_component('CBI1405R_ACC_6_5_15',1,'CONSTANT',40000,'40,000 ریال','SRC:6-5-15-01:FIXED_AMOUNT','جزء منبع 6-5-15-01 | نوع=FIXED_AMOUNT | وضعیت=دارای کارمزد | 40,000 ریال');
  check_component('CBI1405R_ACC_6_5_16',1,'CONSTANT',100000,'100,000 ریال','SRC:6-5-16-01:FIXED_AMOUNT','جزء منبع 6-5-16-01 | نوع=FIXED_AMOUNT | وضعیت=دارای کارمزد | 100,000 ریال');
  check_component('CBI1405R_APP_7_1',1,'CONSTANT',4140000,'تا چهار میلیارد ریال مبلغ ارزیابی: 4,140,000 ریال؛ بیش از چهار میلیارد ریال تا 800 میلیارد ریال نسبت به مازاد: 2 در هزار یا معادل تعرفه کارشناسان رسمی دادگستری، هر کدام که کمتر باشد؛ مازاد بر 800 میلیارد ریال: ارجاع به کارشناس رسمی دادگستری','SRC:7-1-01:FIXED_AMOUNT','جزء منبع 7-1-01 | نوع=FIXED_AMOUNT | وضعیت=دارای کارمزد | مبنا=مبلغ ارزیابی | بازه=..4000000000 | مبنای بازه=مبلغ ارزیابی | برای مبلغ ارزیابی تا 4 میلیارد ریال: 4,140,000 ریال');
  check_component('CBI1405R_APP_7_1',2,'CONSTANT',0.002,'تا چهار میلیارد ریال مبلغ ارزیابی: 4,140,000 ریال؛ بیش از چهار میلیارد ریال تا 800 میلیارد ریال نسبت به مازاد: 2 در هزار یا معادل تعرفه کارشناسان رسمی دادگستری، هر کدام که کمتر باشد؛ مازاد بر 800 میلیارد ریال: ارجاع به کارشناس رسمی دادگستری','SRC:7-1-02:RATE','جزء منبع 7-1-02 | نوع=RATE | وضعیت=دارای کارمزد | محدودیت نرخ=کمترِ دو مبنا | مبنا=مازاد مبلغ ارزیابی بیش از 4 میلیارد ریال | بازه=4000000000..800000000000 | مبنای بازه=مبلغ ارزیابی | مرجع=تعرفه کارشناسان رسمی دادگستری | شرط=هر کدام از 2 در هزار مازاد یا تعرفه کارشناسان رسمی دادگستری که کمتر باشد | بیش از 4 میلیارد تا 800 میلیارد ریال: 2 در هزار نسبت به مازاد یا تعرفه کارشناسان رسمی دادگستری، هر کدام کمتر باشد');
  check_component('CBI1405R_APP_7_1',3,'EXTERNAL_VALUE',NULL,'تا چهار میلیارد ریال مبلغ ارزیابی: 4,140,000 ریال؛ بیش از چهار میلیارد ریال تا 800 میلیارد ریال نسبت به مازاد: 2 در هزار یا معادل تعرفه کارشناسان رسمی دادگستری، هر کدام که کمتر باشد؛ مازاد بر 800 میلیارد ریال: ارجاع به کارشناس رسمی دادگستری','SRC:7-1-03:REFERENCE','جزء منبع 7-1-03 | نوع=REFERENCE | وضعیت=تعرفه مرجع | بازه=800000000000.. | مبنای بازه=مبلغ ارزیابی | مرجع=کارشناس رسمی دادگستری | شرط=مازاد بر 800 میلیارد ریال | ارجاع به کارشناس رسمی دادگستری');
  check_component('CBI1405R_APP_7_2',1,'CONSTANT',4140000,'تا چهار میلیارد ریال مبلغ ارزیابی: 4,140,000 ریال؛ بیش از چهار میلیارد ریال تا 800 میلیارد ریال نسبت به مازاد: 2 در هزار یا معادل تعرفه کارشناسان رسمی دادگستری، هر کدام که کمتر باشد؛ مازاد بر 800 میلیارد ریال: ارجاع به کارشناس رسمی دادگستری','SRC:7-2-01:FIXED_AMOUNT','جزء منبع 7-2-01 | نوع=FIXED_AMOUNT | وضعیت=دارای کارمزد | مبنا=مبلغ ارزیابی | بازه=..4000000000 | مبنای بازه=مبلغ ارزیابی | برای مبلغ ارزیابی تا 4 میلیارد ریال: 4,140,000 ریال');
  check_component('CBI1405R_APP_7_2',2,'CONSTANT',0.002,'تا چهار میلیارد ریال مبلغ ارزیابی: 4,140,000 ریال؛ بیش از چهار میلیارد ریال تا 800 میلیارد ریال نسبت به مازاد: 2 در هزار یا معادل تعرفه کارشناسان رسمی دادگستری، هر کدام که کمتر باشد؛ مازاد بر 800 میلیارد ریال: ارجاع به کارشناس رسمی دادگستری','SRC:7-2-02:RATE','جزء منبع 7-2-02 | نوع=RATE | وضعیت=دارای کارمزد | محدودیت نرخ=کمترِ دو مبنا | مبنا=مازاد مبلغ ارزیابی بیش از 4 میلیارد ریال | بازه=4000000000..800000000000 | مبنای بازه=مبلغ ارزیابی | مرجع=تعرفه کارشناسان رسمی دادگستری | شرط=هر کدام از 2 در هزار مازاد یا تعرفه کارشناسان رسمی دادگستری که کمتر باشد | بیش از 4 میلیارد تا 800 میلیارد ریال: 2 در هزار نسبت به مازاد یا تعرفه کارشناسان رسمی دادگستری، هر کدام کمتر باشد');
  check_component('CBI1405R_APP_7_2',3,'EXTERNAL_VALUE',NULL,'تا چهار میلیارد ریال مبلغ ارزیابی: 4,140,000 ریال؛ بیش از چهار میلیارد ریال تا 800 میلیارد ریال نسبت به مازاد: 2 در هزار یا معادل تعرفه کارشناسان رسمی دادگستری، هر کدام که کمتر باشد؛ مازاد بر 800 میلیارد ریال: ارجاع به کارشناس رسمی دادگستری','SRC:7-2-03:REFERENCE','جزء منبع 7-2-03 | نوع=REFERENCE | وضعیت=تعرفه مرجع | بازه=800000000000.. | مبنای بازه=مبلغ ارزیابی | مرجع=کارشناس رسمی دادگستری | شرط=مازاد بر 800 میلیارد ریال | ارجاع به کارشناس رسمی دادگستری');
  check_component('CBI1405R_APP_7_3',1,'CONSTANT',4140000,'تا چهار میلیارد ریال مبلغ ارزیابی: 4,140,000 ریال؛ بیش از چهار میلیارد ریال تا 800 میلیارد ریال نسبت به مازاد: 2 در هزار یا معادل تعرفه کارشناسان رسمی دادگستری، هر کدام که کمتر باشد؛ مازاد بر 800 میلیارد ریال: ارجاع به کارشناس رسمی دادگستری','SRC:7-3-01:FIXED_AMOUNT','جزء منبع 7-3-01 | نوع=FIXED_AMOUNT | وضعیت=دارای کارمزد | مبنا=مبلغ ارزیابی | بازه=..4000000000 | مبنای بازه=مبلغ ارزیابی | برای مبلغ ارزیابی تا 4 میلیارد ریال: 4,140,000 ریال');
  check_component('CBI1405R_APP_7_3',2,'CONSTANT',0.002,'تا چهار میلیارد ریال مبلغ ارزیابی: 4,140,000 ریال؛ بیش از چهار میلیارد ریال تا 800 میلیارد ریال نسبت به مازاد: 2 در هزار یا معادل تعرفه کارشناسان رسمی دادگستری، هر کدام که کمتر باشد؛ مازاد بر 800 میلیارد ریال: ارجاع به کارشناس رسمی دادگستری','SRC:7-3-02:RATE','جزء منبع 7-3-02 | نوع=RATE | وضعیت=دارای کارمزد | محدودیت نرخ=کمترِ دو مبنا | مبنا=مازاد مبلغ ارزیابی بیش از 4 میلیارد ریال | بازه=4000000000..800000000000 | مبنای بازه=مبلغ ارزیابی | مرجع=تعرفه کارشناسان رسمی دادگستری | شرط=هر کدام از 2 در هزار مازاد یا تعرفه کارشناسان رسمی دادگستری که کمتر باشد | بیش از 4 میلیارد تا 800 میلیارد ریال: 2 در هزار نسبت به مازاد یا تعرفه کارشناسان رسمی دادگستری، هر کدام کمتر باشد');
  check_component('CBI1405R_APP_7_3',3,'EXTERNAL_VALUE',NULL,'تا چهار میلیارد ریال مبلغ ارزیابی: 4,140,000 ریال؛ بیش از چهار میلیارد ریال تا 800 میلیارد ریال نسبت به مازاد: 2 در هزار یا معادل تعرفه کارشناسان رسمی دادگستری، هر کدام که کمتر باشد؛ مازاد بر 800 میلیارد ریال: ارجاع به کارشناس رسمی دادگستری','SRC:7-3-03:REFERENCE','جزء منبع 7-3-03 | نوع=REFERENCE | وضعیت=تعرفه مرجع | بازه=800000000000.. | مبنای بازه=مبلغ ارزیابی | مرجع=کارشناس رسمی دادگستری | شرط=مازاد بر 800 میلیارد ریال | ارجاع به کارشناس رسمی دادگستری');
  check_component('CBI1405R_APP_7_4',1,'CONSTANT',4140000,'تا چهار میلیارد ریال مبلغ ارزیابی: 4,140,000 ریال؛ بیش از چهار میلیارد ریال تا 800 میلیارد ریال نسبت به مازاد: 2 در هزار یا معادل تعرفه کارشناسان رسمی دادگستری، هر کدام که کمتر باشد؛ مازاد بر 800 میلیارد ریال: ارجاع به کارشناس رسمی دادگستری','SRC:7-4-01:FIXED_AMOUNT','جزء منبع 7-4-01 | نوع=FIXED_AMOUNT | وضعیت=دارای کارمزد | مبنا=مبلغ ارزیابی | بازه=..4000000000 | مبنای بازه=مبلغ ارزیابی | برای مبلغ ارزیابی تا 4 میلیارد ریال: 4,140,000 ریال');
  check_component('CBI1405R_APP_7_4',2,'CONSTANT',0.002,'تا چهار میلیارد ریال مبلغ ارزیابی: 4,140,000 ریال؛ بیش از چهار میلیارد ریال تا 800 میلیارد ریال نسبت به مازاد: 2 در هزار یا معادل تعرفه کارشناسان رسمی دادگستری، هر کدام که کمتر باشد؛ مازاد بر 800 میلیارد ریال: ارجاع به کارشناس رسمی دادگستری','SRC:7-4-02:RATE','جزء منبع 7-4-02 | نوع=RATE | وضعیت=دارای کارمزد | محدودیت نرخ=کمترِ دو مبنا | مبنا=مازاد مبلغ ارزیابی بیش از 4 میلیارد ریال | بازه=4000000000..800000000000 | مبنای بازه=مبلغ ارزیابی | مرجع=تعرفه کارشناسان رسمی دادگستری | شرط=هر کدام از 2 در هزار مازاد یا تعرفه کارشناسان رسمی دادگستری که کمتر باشد | بیش از 4 میلیارد تا 800 میلیارد ریال: 2 در هزار نسبت به مازاد یا تعرفه کارشناسان رسمی دادگستری، هر کدام کمتر باشد');
  check_component('CBI1405R_APP_7_4',3,'EXTERNAL_VALUE',NULL,'تا چهار میلیارد ریال مبلغ ارزیابی: 4,140,000 ریال؛ بیش از چهار میلیارد ریال تا 800 میلیارد ریال نسبت به مازاد: 2 در هزار یا معادل تعرفه کارشناسان رسمی دادگستری، هر کدام که کمتر باشد؛ مازاد بر 800 میلیارد ریال: ارجاع به کارشناس رسمی دادگستری','SRC:7-4-03:REFERENCE','جزء منبع 7-4-03 | نوع=REFERENCE | وضعیت=تعرفه مرجع | بازه=800000000000.. | مبنای بازه=مبلغ ارزیابی | مرجع=کارشناس رسمی دادگستری | شرط=مازاد بر 800 میلیارد ریال | ارجاع به کارشناس رسمی دادگستری');
  check_component('CBI1405R_APP_7_5',1,'CONSTANT',1000000,'مقطوع 1,000,000 ریال','SRC:7-5-01:FIXED_AMOUNT','جزء منبع 7-5-01 | نوع=FIXED_AMOUNT | وضعیت=دارای کارمزد | مقطوع 1,000,000 ریال');
  check_component('CBI1405R_APP_7_6',1,'CONSTANT',0,'الف) در صورت ارزیابی مجدد به درخواست مؤسسه اعتباری یا سایر مراجع ذی صلاح، کارمزدی از مشتری اخذ نخواهد شد. ب) در صورت ارزیابی مجدد به درخواست مشتری و تا 6 ماه پس از ارزیابی قبلی، حداکثر کارمزد قابل مطالبه 30 درصد رقم کل کارمزد ارزیابی است؛ پس از 6 ماه، کارمزد ارزیابی در چارچوب ردیف های 7-1 تا 7-4 دریافت می شود','SRC:7-6-01:NO_FEE','جزء منبع 7-6-01 | نوع=NO_FEE | وضعیت=بدون کارمزد | شرط=ارزیابی مجدد به درخواست مؤسسه اعتباری یا سایر مراجع ذی صلاح | کارمزدی از مشتری اخذ نمی شود');
  check_component('CBI1405R_APP_7_6',2,'CONSTANT',0.3,'الف) در صورت ارزیابی مجدد به درخواست مؤسسه اعتباری یا سایر مراجع ذی صلاح، کارمزدی از مشتری اخذ نخواهد شد. ب) در صورت ارزیابی مجدد به درخواست مشتری و تا 6 ماه پس از ارزیابی قبلی، حداکثر کارمزد قابل مطالبه 30 درصد رقم کل کارمزد ارزیابی است؛ پس از 6 ماه، کارمزد ارزیابی در چارچوب ردیف های 7-1 تا 7-4 دریافت می شود','SRC:7-6-02:RATE','جزء منبع 7-6-02 | نوع=RATE | وضعیت=دارای کارمزد | محدودیت نرخ=حداکثر | مبنا=رقم کل کارمزد ارزیابی قبلی | شرط=ارزیابی مجدد به درخواست مشتری تا 6 ماه پس از ارزیابی قبلی | حداکثر 30 درصد رقم کل کارمزد ارزیابی');
  check_component('CBI1405R_APP_7_6',3,'EXTERNAL_VALUE',NULL,'الف) در صورت ارزیابی مجدد به درخواست مؤسسه اعتباری یا سایر مراجع ذی صلاح، کارمزدی از مشتری اخذ نخواهد شد. ب) در صورت ارزیابی مجدد به درخواست مشتری و تا 6 ماه پس از ارزیابی قبلی، حداکثر کارمزد قابل مطالبه 30 درصد رقم کل کارمزد ارزیابی است؛ پس از 6 ماه، کارمزد ارزیابی در چارچوب ردیف های 7-1 تا 7-4 دریافت می شود','SRC:7-6-03:REFERENCE','جزء منبع 7-6-03 | نوع=REFERENCE | وضعیت=تعرفه مرجع | مرجع=ردیف های 7-1 تا 7-4 | شرط=درخواست مشتری پس از 6 ماه از ارزیابی قبلی | کارمزد در چارچوب ردیف های 7-1 تا 7-4');
  check_component('CBI1405R_APP_7_7',1,'CONSTANT',4140000,'تا چهار میلیارد ریال مبلغ ارزیابی: 4,140,000 ریال؛ بیش از چهار میلیارد ریال تا 800 میلیارد ریال نسبت به مازاد: 2 در هزار یا معادل تعرفه کارشناسان رسمی دادگستری، هر کدام که کمتر باشد؛ مازاد بر 800 میلیارد ریال: ارجاع به کارشناس رسمی دادگستری','SRC:7-7-01:FIXED_AMOUNT','جزء منبع 7-7-01 | نوع=FIXED_AMOUNT | وضعیت=دارای کارمزد | مبنا=مبلغ ارزیابی | بازه=..4000000000 | مبنای بازه=مبلغ ارزیابی | برای مبلغ ارزیابی تا 4 میلیارد ریال: 4,140,000 ریال');
  check_component('CBI1405R_APP_7_7',2,'CONSTANT',0.002,'تا چهار میلیارد ریال مبلغ ارزیابی: 4,140,000 ریال؛ بیش از چهار میلیارد ریال تا 800 میلیارد ریال نسبت به مازاد: 2 در هزار یا معادل تعرفه کارشناسان رسمی دادگستری، هر کدام که کمتر باشد؛ مازاد بر 800 میلیارد ریال: ارجاع به کارشناس رسمی دادگستری','SRC:7-7-02:RATE','جزء منبع 7-7-02 | نوع=RATE | وضعیت=دارای کارمزد | محدودیت نرخ=کمترِ دو مبنا | مبنا=مازاد مبلغ ارزیابی بیش از 4 میلیارد ریال | بازه=4000000000..800000000000 | مبنای بازه=مبلغ ارزیابی | مرجع=تعرفه کارشناسان رسمی دادگستری | شرط=هر کدام از 2 در هزار مازاد یا تعرفه کارشناسان رسمی دادگستری که کمتر باشد | بیش از 4 میلیارد تا 800 میلیارد ریال: 2 در هزار نسبت به مازاد یا تعرفه کارشناسان رسمی دادگستری، هر کدام کمتر باشد');
  check_component('CBI1405R_APP_7_7',3,'EXTERNAL_VALUE',NULL,'تا چهار میلیارد ریال مبلغ ارزیابی: 4,140,000 ریال؛ بیش از چهار میلیارد ریال تا 800 میلیارد ریال نسبت به مازاد: 2 در هزار یا معادل تعرفه کارشناسان رسمی دادگستری، هر کدام که کمتر باشد؛ مازاد بر 800 میلیارد ریال: ارجاع به کارشناس رسمی دادگستری','SRC:7-7-03:REFERENCE','جزء منبع 7-7-03 | نوع=REFERENCE | وضعیت=تعرفه مرجع | بازه=800000000000.. | مبنای بازه=مبلغ ارزیابی | مرجع=کارشناس رسمی دادگستری | شرط=مازاد بر 800 میلیارد ریال | ارجاع به کارشناس رسمی دادگستری');
  check_component('CBI1405R_CRD_8_1',1,'EXTERNAL_VALUE',NULL,'مطابق تعرفه ابلاغی بانک مرکزی به شرکت مذکور','SRC:8-1-01:REFERENCE','جزء منبع 8-1-01 | نوع=REFERENCE | وضعیت=تعرفه مرجع | مرجع=تعرفه ابلاغی بانک مرکزی | مطابق تعرفه ابلاغی بانک مرکزی به شرکت مذکور');
  check_component('CBI1405R_CRD_8_2',1,'EXTERNAL_VALUE',NULL,'مطابق تعرفه ارزیابی اموال منقول و غیرمنقول مندرج در سرفصل 7','SRC:8-2-01:REFERENCE','جزء منبع 8-2-01 | نوع=REFERENCE | وضعیت=تعرفه مرجع | مرجع=تعرفه ارزیابی اموال منقول و غیرمنقول مندرج در سرفصل 7 | مطابق تعرفه ارزیابی اموال منقول و غیرمنقول مندرج در سرفصل 7');
  check_component('CBI1405R_CRD_8_3',1,'CONSTANT',0.005,'0.5 درصد مبلغ تسهیلات، حداکثر 5,760,000 ریال','SRC:8-3-01:RATE','جزء منبع 8-3-01 | نوع=RATE | وضعیت=دارای کارمزد | محدودیت نرخ=دقیق | مبنا=مبلغ تسهیلات | 0.5 درصد مبلغ تسهیلات، حداکثر 5,760,000 ریال');
  check_component('CBI1405R_CRD_8_4',1,'CONSTANT',0,'بدون کارمزد','SRC:8-4-01:NO_FEE','جزء منبع 8-4-01 | نوع=NO_FEE | وضعیت=بدون کارمزد | بدون کارمزد');
  check_component('CBI1405R_CRD_8_5',1,'CONSTANT',0.01,'یک درصد مانده اصل تسهیلات قابل انتقال، حداقل 2,880,000 ریال و حداکثر 144,000,000 ریال','SRC:8-5-01:RATE','جزء منبع 8-5-01 | نوع=RATE | وضعیت=دارای کارمزد | محدودیت نرخ=دقیق | مبنا=مانده اصل تسهیلات قابل انتقال | یک درصد مانده اصل تسهیلات قابل انتقال، حداقل 2,880,000 ریال و حداکثر 144,000,000 ریال');
  check_component('CBI1405R_CRD_8_6',1,'CONSTANT',220000,'مقطوع 220,000 ریال','SRC:8-6-01:FIXED_AMOUNT','جزء منبع 8-6-01 | نوع=FIXED_AMOUNT | وضعیت=دارای کارمزد | مقطوع 220,000 ریال');
  check_component('CBI1405R_CRD_8_7',1,'CONSTANT',2880000,'مقطوع 2,880,000 ریال','SRC:8-7-01:FIXED_AMOUNT','جزء منبع 8-7-01 | نوع=FIXED_AMOUNT | وضعیت=دارای کارمزد | مقطوع 2,880,000 ریال');
  check_component('CBI1405R_CRD_8_8',1,'CONSTANT',4320000,'مقطوع 4,320,000 ریال','SRC:8-8-01:FIXED_AMOUNT','جزء منبع 8-8-01 | نوع=FIXED_AMOUNT | وضعیت=دارای کارمزد | مقطوع 4,320,000 ریال');
  check_component('CBI1405R_CRD_8_9',1,'CONSTANT',0.0005,'0.5 در هزار مبلغ پرداخت شده در هر مرحله قبل','SRC:8-9-01:RATE','جزء منبع 8-9-01 | نوع=RATE | وضعیت=دارای کارمزد | محدودیت نرخ=دقیق | مبنا=مبلغ پرداخت شده در هر مرحله قبل | تناوب=هر مرحله | 0.5 در هزار مبلغ پرداخت شده در هر مرحله قبل');
  check_component('CBI1405R_CRD_8_10',1,'CONSTANT',2880000,'مقطوع 2,880,000 ریال','SRC:8-10-01:FIXED_AMOUNT','جزء منبع 8-10-01 | نوع=FIXED_AMOUNT | وضعیت=دارای کارمزد | مقطوع 2,880,000 ریال');
  check_component('CBI1405R_CRD_8_11',1,'CONSTANT',264000,'مقطوع 264,000 ریال','SRC:8-11-01:FIXED_AMOUNT','جزء منبع 8-11-01 | نوع=FIXED_AMOUNT | وضعیت=دارای کارمزد | مقطوع 264,000 ریال');
  check_component('CBI1405R_CRD_8_12',1,'CONSTANT',576000,'هر برگ 576,000 ریال به علاوه هزینه پستی (در صورت وجود)','SRC:8-12-01:FIXED_AMOUNT','جزء منبع 8-12-01 | نوع=FIXED_AMOUNT | وضعیت=دارای کارمزد | واحد=برگ | هزینه اضافی=هزینه پست + هزینه پستی | هر برگ 576,000 ریال به علاوه هزینه پستی (در صورت وجود)');
  check_component('CBI1405R_CRD_8_13',1,'CONSTANT',2880000,'مقطوع 2,880,000 ریال','SRC:8-13-01:FIXED_AMOUNT','جزء منبع 8-13-01 | نوع=FIXED_AMOUNT | وضعیت=دارای کارمزد | مقطوع 2,880,000 ریال');
  check_component('CBI1405R_CRD_8_14',1,'CONSTANT',2880000,'مقطوع 2,880,000 ریال','SRC:8-14-01:FIXED_AMOUNT','جزء منبع 8-14-01 | نوع=FIXED_AMOUNT | وضعیت=دارای کارمزد | مقطوع 2,880,000 ریال');
  check_component('CBI1405R_CRD_8_15',1,'CONSTANT',0.01,'یک درصد در سال به ازای مانده استفاده نشده از تاریخ ابلاغ','SRC:8-15-01:RATE','جزء منبع 8-15-01 | نوع=RATE | وضعیت=دارای کارمزد | محدودیت نرخ=دقیق | مبنا=مانده استفاده نشده | تناوب=سالانه | یک درصد در سال به ازای مانده استفاده نشده از تاریخ ابلاغ');
  check_component('CBI1405R_CRD_8_16',1,'CONSTANT',0.0005,'1.5 در هزار تسهیلات مصوب (فقط سهم بانک)، شامل 0.5 در هزار مبلغ درخواستی در هنگام پذیرش طرح که از مشتری اخذ شده و غیرقابل برگشت است؛ مابقی در صورت تصویب تا میزان 1.5 در هزار پس از کسر 0.5 در هزار مبلغ مأخوذه قبلی، در زمان انعقاد قرارداد از مشتری اخذ می گردد','SRC:8-16-01:RATE','جزء منبع 8-16-01 | نوع=RATE | وضعیت=دارای کارمزد | محدودیت نرخ=جزء از سقف کل | مبنا=مبلغ درخواستی | شرط=هنگام پذیرش طرح/درخواست؛ غیرقابل برگشت | 0.5 در هزار مبلغ درخواستی در هنگام پذیرش، غیرقابل برگشت');
  check_component('CBI1405R_CRD_8_16',2,'CONSTANT',0.0015,'1.5 در هزار تسهیلات مصوب (فقط سهم بانک)، شامل 0.5 در هزار مبلغ درخواستی در هنگام پذیرش طرح که از مشتری اخذ شده و غیرقابل برگشت است؛ مابقی در صورت تصویب تا میزان 1.5 در هزار پس از کسر 0.5 در هزار مبلغ مأخوذه قبلی، در زمان انعقاد قرارداد از مشتری اخذ می گردد','SRC:8-16-02:RATE','جزء منبع 8-16-02 | نوع=RATE | وضعیت=دارای کارمزد | محدودیت نرخ=سقف کل | مبنا=تسهیلات مصوب (فقط سهم بانک) | شرط=در صورت تصویب و هنگام انعقاد قرارداد؛ پس از کسر مبلغ مأخوذه مرحله قبل | کل کارمزد تا 1.5 در هزار تسهیلات مصوب؛ مابقی پس از کسر 0.5 در هزار قبلی اخذ می شود');
  check_component('CBI1405R_CRD_8_17',1,'CONSTANT',0.0005,'0.5 در هزار تسهیلات مصوب (فقط سهم بانک)','SRC:8-17-01:RATE','جزء منبع 8-17-01 | نوع=RATE | وضعیت=دارای کارمزد | محدودیت نرخ=دقیق | مبنا=تسهیلات مصوب | 0.5 در هزار تسهیلات مصوب (فقط سهم بانک)');
  check_component('CBI1405R_CRD_8_18',1,'CONSTANT',0.0005,'1.5 در هزار تسهیلات مصوب (فقط سهم بانک) جهت تأمین کالاهای سرمایه ای، شامل 0.5 در هزار مبلغ درخواستی در هنگام پذیرش که از مشتری اخذ شده و غیرقابل برگشت است؛ مابقی در صورت تصویب تا میزان 1.5 در هزار پس از کسر 0.5 در هزار مبلغ مأخوذه قبلی، در زمان انعقاد قرارداد از مشتری اخذ می گردد','SRC:8-18-01:RATE','جزء منبع 8-18-01 | نوع=RATE | وضعیت=دارای کارمزد | محدودیت نرخ=جزء از سقف کل | مبنا=مبلغ درخواستی | شرط=هنگام پذیرش طرح/درخواست؛ غیرقابل برگشت | 0.5 در هزار مبلغ درخواستی در هنگام پذیرش، غیرقابل برگشت');
  check_component('CBI1405R_CRD_8_18',2,'CONSTANT',0.0015,'1.5 در هزار تسهیلات مصوب (فقط سهم بانک) جهت تأمین کالاهای سرمایه ای، شامل 0.5 در هزار مبلغ درخواستی در هنگام پذیرش که از مشتری اخذ شده و غیرقابل برگشت است؛ مابقی در صورت تصویب تا میزان 1.5 در هزار پس از کسر 0.5 در هزار مبلغ مأخوذه قبلی، در زمان انعقاد قرارداد از مشتری اخذ می گردد','SRC:8-18-02:RATE','جزء منبع 8-18-02 | نوع=RATE | وضعیت=دارای کارمزد | محدودیت نرخ=سقف کل | مبنا=تسهیلات مصوب (فقط سهم بانک) | شرط=در صورت تصویب و هنگام انعقاد قرارداد؛ پس از کسر مبلغ مأخوذه مرحله قبل | کل کارمزد تا 1.5 در هزار تسهیلات مصوب؛ مابقی پس از کسر 0.5 در هزار قبلی اخذ می شود');
  check_component('CBI1405R_CRD_8_19',1,'CONSTANT',0.0005,'0.5 در هزار مبلغ تعهد مورد درخواست','SRC:8-19-01:RATE','جزء منبع 8-19-01 | نوع=RATE | وضعیت=دارای کارمزد | محدودیت نرخ=دقیق | مبنا=مبلغ تعهد مورد درخواست | 0.5 در هزار مبلغ تعهد مورد درخواست');
  check_component('CBI1405R_CRD_8_20',1,'CONSTANT',720000,'اشخاص حقیقی: 720,000 ریال (5)؛ اشخاص حقوقی: 2,880,000 ریال (6)','SRC:8-20-01:FIXED_AMOUNT','جزء منبع 8-20-01 | نوع=FIXED_AMOUNT | وضعیت=دارای کارمزد | گروه مشتری=حقیقی | اشخاص حقیقی: 720,000 ریال (5)');
  check_component('CBI1405R_CRD_8_20',2,'CONSTANT',2880000,'اشخاص حقیقی: 720,000 ریال (5)؛ اشخاص حقوقی: 2,880,000 ریال (6)','SRC:8-20-02:FIXED_AMOUNT','جزء منبع 8-20-02 | نوع=FIXED_AMOUNT | وضعیت=دارای کارمزد | گروه مشتری=حقوقی | اشخاص حقوقی: 2,880,000 ریال (6)');
  check_component('CBI1405R_CRD_8_21',1,'CONSTANT',576000,'576,000 ریال','SRC:8-21-01:FIXED_AMOUNT','جزء منبع 8-21-01 | نوع=FIXED_AMOUNT | وضعیت=دارای کارمزد | 576,000 ریال');
  check_component('CBI1405R_CRD_8_22',1,'CONSTANT',0.001,'یک در هزار مبلغ تسهیلات','SRC:8-22-01:RATE','جزء منبع 8-22-01 | نوع=RATE | وضعیت=دارای کارمزد | محدودیت نرخ=دقیق | مبنا=مبلغ تسهیلات | یک در هزار مبلغ تسهیلات');
  check_component('CBI1405R_CRD_8_23',1,'CONSTANT',120000,'120,000 ریال برای هر برگ','SRC:8-23-01:FIXED_AMOUNT','جزء منبع 8-23-01 | نوع=FIXED_AMOUNT | وضعیت=دارای کارمزد | واحد=برگ | 120,000 ریال برای هر برگ');
  check_component('CBI1405R_CRD_8_24',1,'CONSTANT',1100000,'برای اشخاص حقیقی: مقطوع 1,100,000 ریال؛ برای اشخاص حقوقی: 0.5 درصد مبلغ تسهیلات مصوب، حداقل 1,100,000 ریال و حداکثر 6,000,000 ریال','SRC:8-24-01:FIXED_AMOUNT','جزء منبع 8-24-01 | نوع=FIXED_AMOUNT | وضعیت=دارای کارمزد | گروه مشتری=حقیقی | اشخاص حقیقی: مقطوع 1,100,000 ریال');
  check_component('CBI1405R_CRD_8_24',2,'CONSTANT',0.005,'برای اشخاص حقیقی: مقطوع 1,100,000 ریال؛ برای اشخاص حقوقی: 0.5 درصد مبلغ تسهیلات مصوب، حداقل 1,100,000 ریال و حداکثر 6,000,000 ریال','SRC:8-24-02:RATE','جزء منبع 8-24-02 | نوع=RATE | وضعیت=دارای کارمزد | گروه مشتری=حقوقی | مبنا=مبلغ تسهیلات مصوب | اشخاص حقوقی: 0.5 درصد مبلغ تسهیلات مصوب؛ حداقل 1,100,000 و حداکثر 6,000,000 ریال');
  check_component('CBI1405R_CRD_8_25',1,'CONSTANT',0,'برای اشخاص حقیقی: بدون کارمزد؛ برای اشخاص حقوقی: مقطوع 1,200,000 ریال','SRC:8-25-01:NO_FEE','جزء منبع 8-25-01 | نوع=NO_FEE | وضعیت=بدون کارمزد | گروه مشتری=حقیقی | برای اشخاص حقیقی: بدون کارمزد');
  check_component('CBI1405R_CRD_8_25',2,'CONSTANT',1200000,'برای اشخاص حقیقی: بدون کارمزد؛ برای اشخاص حقوقی: مقطوع 1,200,000 ریال','SRC:8-25-02:FIXED_AMOUNT','جزء منبع 8-25-02 | نوع=FIXED_AMOUNT | وضعیت=دارای کارمزد | گروه مشتری=حقوقی | برای اشخاص حقوقی: مقطوع 1,200,000 ریال');
  check_component('CBI1405R_CRD_8_26',1,'CONSTANT',0.0002,'0.2 در هزار مبلغ مورد امهال، حداکثر 120 میلیون ریال','SRC:8-26-01:RATE','جزء منبع 8-26-01 | نوع=RATE | وضعیت=دارای کارمزد | محدودیت نرخ=دقیق | مبنا=مبلغ مورد امهال | 0.2 در هزار مبلغ مورد امهال، حداکثر 120 میلیون ریال');
  check_component('CBI1405R_CRD_8_27',1,'CONSTANT',0.0002,'0.2 در هزار کل بدهی، حداکثر 120 میلیون ریال','SRC:8-27-01:RATE','جزء منبع 8-27-01 | نوع=RATE | وضعیت=دارای کارمزد | محدودیت نرخ=دقیق | مبنا=کل بدهی | 0.2 در هزار کل بدهی، حداکثر 120 میلیون ریال');
  check_component('CBI1405R_CRD_8_28',1,'EXTERNAL_VALUE',NULL,'معادل تعرفه اعلامی از سوی سازمان ثبت احوال کشور (مندرج در قانون بودجه سنواتی)','SRC:8-28-01:REFERENCE','جزء منبع 8-28-01 | نوع=REFERENCE | وضعیت=تعرفه مرجع | مرجع=تعرفه اعلامی سازمان ثبت احوال کشور | معادل تعرفه اعلامی از سوی سازمان ثبت احوال کشور (مندرج در قانون بودجه سنواتی)');
  check_component('CBI1405R_CRD_8_29',1,'CONSTANT',0,'اشخاص حقیقی: بدون کارمزد؛ اشخاص حقوقی: مقطوع 120,000 ریال','SRC:8-29-01:NO_FEE','جزء منبع 8-29-01 | نوع=NO_FEE | وضعیت=بدون کارمزد | گروه مشتری=حقیقی | اشخاص حقیقی: بدون کارمزد');
  check_component('CBI1405R_CRD_8_29',2,'CONSTANT',120000,'اشخاص حقیقی: بدون کارمزد؛ اشخاص حقوقی: مقطوع 120,000 ریال','SRC:8-29-02:FIXED_AMOUNT','جزء منبع 8-29-02 | نوع=FIXED_AMOUNT | وضعیت=دارای کارمزد | گروه مشتری=حقوقی | اشخاص حقوقی: مقطوع 120,000 ریال');
  check_component('CBI1405R_CRD_8_30',1,'CONSTANT',240000,'240,000 ریال + هزینه پست در صورت وجود','SRC:8-30-01:FIXED_AMOUNT','جزء منبع 8-30-01 | نوع=FIXED_AMOUNT | وضعیت=دارای کارمزد | هزینه اضافی=هزینه پست | 240,000 ریال + هزینه پست در صورت وجود');
  check_component('CBI1405R_CRD_8_31',1,'CONSTANT',0,'بدون کارمزد','SRC:8-31-01:NO_FEE','جزء منبع 8-31-01 | نوع=NO_FEE | وضعیت=بدون کارمزد | بدون کارمزد');
  check_component('CBI1405R_CRD_8_32',1,'CONSTANT',0,'برای اشخاص حقیقی: بدون کارمزد؛ برای اشخاص حقوقی: 1,000,000 ریال','SRC:8-32-01:NO_FEE','جزء منبع 8-32-01 | نوع=NO_FEE | وضعیت=بدون کارمزد | گروه مشتری=حقیقی | برای اشخاص حقیقی: بدون کارمزد');
  check_component('CBI1405R_CRD_8_32',2,'CONSTANT',1000000,'برای اشخاص حقیقی: بدون کارمزد؛ برای اشخاص حقوقی: 1,000,000 ریال','SRC:8-32-02:FIXED_AMOUNT','جزء منبع 8-32-02 | نوع=FIXED_AMOUNT | وضعیت=دارای کارمزد | گروه مشتری=حقوقی | برای اشخاص حقوقی: 1,000,000 ریال');
  check_component('CBI1405R_CRD_8_33',1,'CONSTANT',0,'برای اشخاص حقیقی: بدون کارمزد؛ برای اشخاص حقوقی: 1,000,000 ریال','SRC:8-33-01:NO_FEE','جزء منبع 8-33-01 | نوع=NO_FEE | وضعیت=بدون کارمزد | گروه مشتری=حقیقی | برای اشخاص حقیقی: بدون کارمزد');
  check_component('CBI1405R_CRD_8_33',2,'CONSTANT',1000000,'برای اشخاص حقیقی: بدون کارمزد؛ برای اشخاص حقوقی: 1,000,000 ریال','SRC:8-33-02:FIXED_AMOUNT','جزء منبع 8-33-02 | نوع=FIXED_AMOUNT | وضعیت=دارای کارمزد | گروه مشتری=حقوقی | برای اشخاص حقوقی: 1,000,000 ریال');
  check_component('CBI1405R_CRD_8_34',1,'CONSTANT',0,'برای اشخاص حقیقی: بدون کارمزد؛ برای اشخاص حقوقی: 1,000,000 ریال','SRC:8-34-01:NO_FEE','جزء منبع 8-34-01 | نوع=NO_FEE | وضعیت=بدون کارمزد | گروه مشتری=حقیقی | برای اشخاص حقیقی: بدون کارمزد');
  check_component('CBI1405R_CRD_8_34',2,'CONSTANT',1000000,'برای اشخاص حقیقی: بدون کارمزد؛ برای اشخاص حقوقی: 1,000,000 ریال','SRC:8-34-02:FIXED_AMOUNT','جزء منبع 8-34-02 | نوع=FIXED_AMOUNT | وضعیت=دارای کارمزد | گروه مشتری=حقوقی | برای اشخاص حقوقی: 1,000,000 ریال');
  check_component('CBI1405R_CRD_8_35',1,'CONSTANT',0,'برای اشخاص حقیقی: بدون کارمزد؛ برای اشخاص حقوقی: 1,000,000 ریال','SRC:8-35-01:NO_FEE','جزء منبع 8-35-01 | نوع=NO_FEE | وضعیت=بدون کارمزد | گروه مشتری=حقیقی | برای اشخاص حقیقی: بدون کارمزد');
  check_component('CBI1405R_CRD_8_35',2,'CONSTANT',1000000,'برای اشخاص حقیقی: بدون کارمزد؛ برای اشخاص حقوقی: 1,000,000 ریال','SRC:8-35-02:FIXED_AMOUNT','جزء منبع 8-35-02 | نوع=FIXED_AMOUNT | وضعیت=دارای کارمزد | گروه مشتری=حقوقی | برای اشخاص حقوقی: 1,000,000 ریال');
  check_component('CBI1405R_CRD_8_36',1,'CONSTANT',0,'بدون کارمزد','SRC:8-36-01:NO_FEE','جزء منبع 8-36-01 | نوع=NO_FEE | وضعیت=بدون کارمزد | بدون کارمزد');
  check_component('CBI1405R_CRD_8_37',1,'CONSTANT',0,'برای اشخاص حقیقی: بدون کارمزد؛ برای اشخاص حقوقی: 1,000,000 ریال','SRC:8-37-01:NO_FEE','جزء منبع 8-37-01 | نوع=NO_FEE | وضعیت=بدون کارمزد | گروه مشتری=حقیقی | برای اشخاص حقیقی: بدون کارمزد');
  check_component('CBI1405R_CRD_8_37',2,'CONSTANT',1000000,'برای اشخاص حقیقی: بدون کارمزد؛ برای اشخاص حقوقی: 1,000,000 ریال','SRC:8-37-02:FIXED_AMOUNT','جزء منبع 8-37-02 | نوع=FIXED_AMOUNT | وضعیت=دارای کارمزد | گروه مشتری=حقوقی | برای اشخاص حقوقی: 1,000,000 ریال');
  check_component('CBI1405R_CRD_8_38',1,'CONSTANT',0,'بدون کارمزد','SRC:8-38-01:NO_FEE','جزء منبع 8-38-01 | نوع=NO_FEE | وضعیت=بدون کارمزد | بدون کارمزد');
  check_component('CBI1405R_CRD_8_39',1,'CONSTANT',0,'بدون کارمزد','SRC:8-39-01:NO_FEE','جزء منبع 8-39-01 | نوع=NO_FEE | وضعیت=بدون کارمزد | بدون کارمزد');
  check_component('CBI1405R_OTH_9_1',1,'CONSTANT',30000,'30,000 ریال','SRC:9-1-01:FIXED_AMOUNT','جزء منبع 9-1-01 | نوع=FIXED_AMOUNT | وضعیت=دارای کارمزد | 30,000 ریال');
  check_component('CBI1405R_OTH_9_2',1,'CONSTANT',57600,'57,600 ریال','SRC:9-2-01:FIXED_AMOUNT','جزء منبع 9-2-01 | نوع=FIXED_AMOUNT | وضعیت=دارای کارمزد | 57,600 ریال');
  check_component('CBI1405R_OTH_9_3',1,'CONSTANT',0,'بدون کارمزد','SRC:9-3-01:NO_FEE','جزء منبع 9-3-01 | نوع=NO_FEE | وضعیت=بدون کارمزد | بدون کارمزد');
  check_component('CBI1405R_OTH_9_4',1,'CONSTANT',1008000,'به ازای هر حساب 1,008,000 ریال','SRC:9-4-01:FIXED_AMOUNT','جزء منبع 9-4-01 | نوع=FIXED_AMOUNT | وضعیت=دارای کارمزد | واحد=حساب | به ازای هر حساب 1,008,000 ریال');
  check_component('CBI1405R_OTH_9_5',1,'CONSTANT',288000,'288,000 ریال','SRC:9-5-01:FIXED_AMOUNT','جزء منبع 9-5-01 | نوع=FIXED_AMOUNT | وضعیت=دارای کارمزد | 288,000 ریال');
  check_component('CBI1405R_OTH_9_6',1,'CONSTANT',57600,'57,600 ریال','SRC:9-6-01:FIXED_AMOUNT','جزء منبع 9-6-01 | نوع=FIXED_AMOUNT | وضعیت=دارای کارمزد | 57,600 ریال');
  check_component('CBI1405R_OTH_9_7',1,'CONSTANT',120000,'120,000 ریال','SRC:9-7-01:FIXED_AMOUNT','جزء منبع 9-7-01 | نوع=FIXED_AMOUNT | وضعیت=دارای کارمزد | 120,000 ریال');
  check_component('CBI1405R_OTH_9_8',1,'CONSTANT',0,'برای اشخاص حقیقی: بدون کارمزد؛ برای اشخاص حقوقی: 20,000 ریال','SRC:9-8-01:NO_FEE','جزء منبع 9-8-01 | نوع=NO_FEE | وضعیت=بدون کارمزد | گروه مشتری=حقیقی | برای اشخاص حقیقی: بدون کارمزد');
  check_component('CBI1405R_OTH_9_8',2,'CONSTANT',20000,'برای اشخاص حقیقی: بدون کارمزد؛ برای اشخاص حقوقی: 20,000 ریال','SRC:9-8-02:FIXED_AMOUNT','جزء منبع 9-8-02 | نوع=FIXED_AMOUNT | وضعیت=دارای کارمزد | گروه مشتری=حقوقی | برای اشخاص حقوقی: 20,000 ریال');
  check_component('CBI1405R_OTH_9_9',1,'CONSTANT',0,'بدون کارمزد','SRC:9-9-01:NO_FEE','جزء منبع 9-9-01 | نوع=NO_FEE | وضعیت=بدون کارمزد | بدون کارمزد');
  check_component('CBI1405R_OTH_9_10',1,'CONSTANT',100000,'100,000 ریال','SRC:9-10-01:FIXED_AMOUNT','جزء منبع 9-10-01 | نوع=FIXED_AMOUNT | وضعیت=دارای کارمزد | 100,000 ریال');
  check_component('CBI1405R_OTH_9_11',1,'CONSTANT',40000,'40,000 ریال + هزینه برچسب','SRC:9-11-01:FIXED_AMOUNT','جزء منبع 9-11-01 | نوع=FIXED_AMOUNT | وضعیت=دارای کارمزد | هزینه اضافی=هزینه برچسب | 40,000 ریال + هزینه برچسب');
  check_component('CBI1405R_OTH_9_12',1,'CONSTANT',0,'بدون کارمزد','SRC:9-12-01:NO_FEE','جزء منبع 9-12-01 | نوع=NO_FEE | وضعیت=بدون کارمزد | بدون کارمزد');
  check_component('CBI1405R_OTH_9_13',1,'CONSTANT',0,'بدون کارمزد','SRC:9-13-01:NO_FEE','جزء منبع 9-13-01 | نوع=NO_FEE | وضعیت=بدون کارمزد | بدون کارمزد');
  check_component('CBI1405R_OTH_9_14',1,'CONSTANT',140000,'140,000 ریال','SRC:9-14-01:FIXED_AMOUNT','جزء منبع 9-14-01 | نوع=FIXED_AMOUNT | وضعیت=دارای کارمزد | 140,000 ریال');
  check_component('CBI1405R_OTH_9_15',1,'CONSTANT',100000,'100,000 ریال + هزینه استعلام در صورت وجود','SRC:9-15-01:FIXED_AMOUNT','جزء منبع 9-15-01 | نوع=FIXED_AMOUNT | وضعیت=دارای کارمزد | هزینه اضافی=هزینه استعلام | 100,000 ریال + هزینه استعلام در صورت وجود');
  check_component('CBI1405R_OTH_9_16',1,'CONSTANT',140000,'140,000 ریال + هزینه استعلام در صورت وجود','SRC:9-16-01:FIXED_AMOUNT','جزء منبع 9-16-01 | نوع=FIXED_AMOUNT | وضعیت=دارای کارمزد | هزینه اضافی=هزینه استعلام | 140,000 ریال + هزینه استعلام در صورت وجود');
  check_component('CBI1405R_OTH_9_17',1,'CONSTANT',100000,'100,000 ریال + هزینه استعلام','SRC:9-17-01:FIXED_AMOUNT','جزء منبع 9-17-01 | نوع=FIXED_AMOUNT | وضعیت=دارای کارمزد | هزینه اضافی=هزینه استعلام | 100,000 ریال + هزینه استعلام');

  DBMS_OUTPUT.PUT_LINE('--- Checking executable input contracts ---');
  check_input('CBI1405R_GAR_1_2','BASE_AMOUNT','مبلغ خدمت/معامله','IRR',1);
  check_input('CBI1405R_GAR_1_3','BASE_AMOUNT','مبلغ خدمت/معامله','IRR',1);
  check_input('CBI1405R_GAR_1_4','BASE_AMOUNT','مبلغ خدمت/معامله','IRR',1);
  check_input('CBI1405R_GAR_1_5','BASE_AMOUNT','مبلغ خدمت/معامله','IRR',1);
  check_input('CBI1405R_GAR_1_6','BASE_AMOUNT','مبلغ خدمت/معامله','IRR',1);
  check_input('CBI1405R_GAR_1_7','BASE_AMOUNT','مبلغ خدمت/معامله','IRR',1);
  check_input('CBI1405R_GAR_1_8','BASE_AMOUNT','مبلغ خدمت/معامله','IRR',1);
  check_input('CBI1405R_GAR_1_9','BASE_AMOUNT','مبلغ خدمت/معامله','IRR',1);
  check_input('CBI1405R_GAR_1_10','BASE_AMOUNT','مبلغ خدمت/معامله','IRR',1);
  check_input('CBI1405R_GAR_1_11','BASE_AMOUNT','مبلغ خدمت/معامله','IRR',1);
  check_input('CBI1405R_GAR_1_12','BASE_AMOUNT','مبلغ خدمت/معامله','IRR',1);
  check_input('CBI1405R_GAR_1_13','BASE_AMOUNT','مبلغ خدمت/معامله','IRR',1);
  check_input('CBI1405R_GAR_1_14','BASE_AMOUNT','مبلغ خدمت/معامله','IRR',1);
  check_input('CBI1405R_GAR_1_15','BASE_AMOUNT','مبلغ مبنای محاسبه','IRR',1);
  check_input('CBI1405R_GAR_1_20','BASE_AMOUNT','مبلغ ضمانتنامه','IRR',1);
  check_input('CBI1405R_REM_2_1','UNIT_COUNT','تعداد فقره','COUNT',1);
  check_input('CBI1405R_REM_2_3','UNIT_COUNT','تعداد فقره','COUNT',1);
  check_input('CBI1405R_REM_2_4','UNIT_COUNT','تعداد فقره','COUNT',1);
  check_input('CBI1405R_SEC_4_4','BASE_AMOUNT','مبلغ مورد انتقال','IRR',1);
  check_input('CBI1405R_SEC_4_6','BASE_AMOUNT','مبلغ اوراق انتقال یافته','IRR',1);
  check_input('CBI1405R_SEC_4_7','BASE_AMOUNT','مبلغ ابطال اوراق','IRR',1);
  check_input('CBI1405R_SEC_4_8','BASE_AMOUNT','مبلغ مورد انتقال','IRR',1);
  check_input('CBI1405R_SEC_4_9','BASE_AMOUNT','مبلغ تنزیل شده','IRR',1);
  check_input('CBI1405R_SEC_4_10','BASE_AMOUNT','مبلغ مورد توثیق','IRR',1);
  check_input('CBI1405R_SEC_4_11','BASE_AMOUNT','مبلغ توافقی واریزی به حساب تأمین کننده','IRR',1);
  check_input('CBI1405R_SEC_4_12','BASE_AMOUNT','مبلغ توافقی واریزی به حساب تأمین کننده','IRR',1);
  check_input('CBI1405R_SEC_4_13','BASE_AMOUNT','مبلغ مبنای محاسبه','IRR',1);
  check_input('CBI1405R_SEC_4_14','BASE_AMOUNT','مبلغ مبنای محاسبه','IRR',1);
  check_input('CBI1405R_COL_5_1','UNIT_COUNT','تعداد فقره','COUNT',1);
  check_input('CBI1405R_COL_5_2','UNIT_COUNT','تعداد نوبت','COUNT',1);
  check_input('CBI1405R_COL_5_3','UNIT_COUNT','تعداد برگ','COUNT',1);
  check_input('CBI1405R_COL_5_4','UNIT_COUNT','تعداد برگ','COUNT',1);
  check_input('CBI1405R_COL_5_6','BASE_AMOUNT','مبلغ خدمت/معامله','IRR',1);
  check_input('CBI1405R_ACC_6_1_1','UNIT_COUNT','تعداد برگ چک','COUNT',1);
  check_input('CBI1405R_ACC_6_1_7','UNIT_COUNT','تعداد فقره','COUNT',1);
  check_input('CBI1405R_ACC_6_1_8','UNIT_COUNT','تعداد حساب','COUNT',1);
  check_input('CBI1405R_ACC_6_1_15','UNIT_COUNT','تعداد برگ چک','COUNT',1);
  check_input('CBI1405R_ACC_6_3_2','UNIT_COUNT','تعداد فقره','COUNT',1);
  check_input('CBI1405R_ACC_6_4_1','UNIT_COUNT','تعداد صفحه','COUNT',1);
  check_input('CBI1405R_ACC_6_5_3','UNIT_COUNT','تعداد حساب','COUNT',1);
  check_input('CBI1405R_ACC_6_5_4','UNIT_COUNT','تعداد حساب','COUNT',1);
  check_input('CBI1405R_ACC_6_5_5','UNIT_COUNT','تعداد انتقال','COUNT',1);
  check_input('CBI1405R_ACC_6_5_13','BASE_AMOUNT','اعتبار تخصیص یافته به دارنده کارت','IRR',1);
  check_input('CBI1405R_ACC_6_5_14','UNIT_COUNT','تعداد شماره پیگیری','COUNT',1);
  check_input('CBI1405R_APP_7_1','APPRAISAL_AMOUNT','مبلغ ارزیابی','IRR',1);
  check_input('CBI1405R_APP_7_1','OFFICIAL_EXPERT_TARIFF','تعرفه کارشناس رسمی دادگستری','IRR',2);
  check_input('CBI1405R_APP_7_2','APPRAISAL_AMOUNT','مبلغ ارزیابی','IRR',1);
  check_input('CBI1405R_APP_7_2','OFFICIAL_EXPERT_TARIFF','تعرفه کارشناس رسمی دادگستری','IRR',2);
  check_input('CBI1405R_APP_7_3','APPRAISAL_AMOUNT','مبلغ ارزیابی','IRR',1);
  check_input('CBI1405R_APP_7_3','OFFICIAL_EXPERT_TARIFF','تعرفه کارشناس رسمی دادگستری','IRR',2);
  check_input('CBI1405R_APP_7_4','APPRAISAL_AMOUNT','مبلغ ارزیابی','IRR',1);
  check_input('CBI1405R_APP_7_4','OFFICIAL_EXPERT_TARIFF','تعرفه کارشناس رسمی دادگستری','IRR',2);
  check_input('CBI1405R_APP_7_7','APPRAISAL_AMOUNT','مبلغ ارزیابی','IRR',1);
  check_input('CBI1405R_APP_7_7','OFFICIAL_EXPERT_TARIFF','تعرفه کارشناس رسمی دادگستری','IRR',2);
  check_input('CBI1405R_CRD_8_3','BASE_AMOUNT','مبلغ تسهیلات','IRR',1);
  check_input('CBI1405R_CRD_8_5','BASE_AMOUNT','مانده اصل تسهیلات قابل انتقال','IRR',1);
  check_input('CBI1405R_CRD_8_9','BASE_AMOUNT','مبلغ پرداخت شده در هر مرحله قبل','IRR',1);
  check_input('CBI1405R_CRD_8_12','UNIT_COUNT','تعداد برگ','COUNT',1);
  check_input('CBI1405R_CRD_8_15','BASE_AMOUNT','مانده استفاده نشده','IRR',1);
  check_input('CBI1405R_CRD_8_17','BASE_AMOUNT','تسهیلات مصوب','IRR',1);
  check_input('CBI1405R_CRD_8_19','BASE_AMOUNT','مبلغ تعهد مورد درخواست','IRR',1);
  check_input('CBI1405R_CRD_8_22','BASE_AMOUNT','مبلغ تسهیلات','IRR',1);
  check_input('CBI1405R_CRD_8_23','UNIT_COUNT','تعداد برگ','COUNT',1);
  check_input('CBI1405R_CRD_8_26','BASE_AMOUNT','مبلغ مورد امهال','IRR',1);
  check_input('CBI1405R_CRD_8_27','BASE_AMOUNT','کل بدهی','IRR',1);
  check_input('CBI1405R_OTH_9_4','UNIT_COUNT','تعداد حساب','COUNT',1);

  DBMS_OUTPUT.PUT_LINE('--- Checking 15 appraisal tiers ---');
  check_tier('CBI1405R_APP_7_1',1,'برای مبلغ ارزیابی تا 4 میلیارد ریال: 4,140,000 ریال',NULL,4000000000,'WHOLE_AMOUNT','FIXED',4140000,NULL,NULL,NULL);
  check_tier('CBI1405R_APP_7_1',2,'بیش از 4 میلیارد تا 800 میلیارد ریال: 2 در هزار نسبت به مازاد یا تعرفه کارشناسان رسمی دادگستری، هر کدام کمتر باشد',4000000000,800000000000,'EXCESS_OVER_LOWER_BOUND','COMPOSITE',NULL,0.002,NULL,NULL);
  check_tier('CBI1405R_APP_7_1',3,'ارجاع به کارشناس رسمی دادگستری',800000000000,NULL,'EXCESS_OVER_LOWER_BOUND','EXTERNAL_VALUE',NULL,NULL,NULL,NULL);
  check_tier('CBI1405R_APP_7_2',1,'برای مبلغ ارزیابی تا 4 میلیارد ریال: 4,140,000 ریال',NULL,4000000000,'WHOLE_AMOUNT','FIXED',4140000,NULL,NULL,NULL);
  check_tier('CBI1405R_APP_7_2',2,'بیش از 4 میلیارد تا 800 میلیارد ریال: 2 در هزار نسبت به مازاد یا تعرفه کارشناسان رسمی دادگستری، هر کدام کمتر باشد',4000000000,800000000000,'EXCESS_OVER_LOWER_BOUND','COMPOSITE',NULL,0.002,NULL,NULL);
  check_tier('CBI1405R_APP_7_2',3,'ارجاع به کارشناس رسمی دادگستری',800000000000,NULL,'EXCESS_OVER_LOWER_BOUND','EXTERNAL_VALUE',NULL,NULL,NULL,NULL);
  check_tier('CBI1405R_APP_7_3',1,'برای مبلغ ارزیابی تا 4 میلیارد ریال: 4,140,000 ریال',NULL,4000000000,'WHOLE_AMOUNT','FIXED',4140000,NULL,NULL,NULL);
  check_tier('CBI1405R_APP_7_3',2,'بیش از 4 میلیارد تا 800 میلیارد ریال: 2 در هزار نسبت به مازاد یا تعرفه کارشناسان رسمی دادگستری، هر کدام کمتر باشد',4000000000,800000000000,'EXCESS_OVER_LOWER_BOUND','COMPOSITE',NULL,0.002,NULL,NULL);
  check_tier('CBI1405R_APP_7_3',3,'ارجاع به کارشناس رسمی دادگستری',800000000000,NULL,'EXCESS_OVER_LOWER_BOUND','EXTERNAL_VALUE',NULL,NULL,NULL,NULL);
  check_tier('CBI1405R_APP_7_4',1,'برای مبلغ ارزیابی تا 4 میلیارد ریال: 4,140,000 ریال',NULL,4000000000,'WHOLE_AMOUNT','FIXED',4140000,NULL,NULL,NULL);
  check_tier('CBI1405R_APP_7_4',2,'بیش از 4 میلیارد تا 800 میلیارد ریال: 2 در هزار نسبت به مازاد یا تعرفه کارشناسان رسمی دادگستری، هر کدام کمتر باشد',4000000000,800000000000,'EXCESS_OVER_LOWER_BOUND','COMPOSITE',NULL,0.002,NULL,NULL);
  check_tier('CBI1405R_APP_7_4',3,'ارجاع به کارشناس رسمی دادگستری',800000000000,NULL,'EXCESS_OVER_LOWER_BOUND','EXTERNAL_VALUE',NULL,NULL,NULL,NULL);
  check_tier('CBI1405R_APP_7_7',1,'برای مبلغ ارزیابی تا 4 میلیارد ریال: 4,140,000 ریال',NULL,4000000000,'WHOLE_AMOUNT','FIXED',4140000,NULL,NULL,NULL);
  check_tier('CBI1405R_APP_7_7',2,'بیش از 4 میلیارد تا 800 میلیارد ریال: 2 در هزار نسبت به مازاد یا تعرفه کارشناسان رسمی دادگستری، هر کدام کمتر باشد',4000000000,800000000000,'EXCESS_OVER_LOWER_BOUND','COMPOSITE',NULL,0.002,NULL,NULL);
  check_tier('CBI1405R_APP_7_7',3,'ارجاع به کارشناس رسمی دادگستری',800000000000,NULL,'EXCESS_OVER_LOWER_BOUND','EXTERNAL_VALUE',NULL,NULL,NULL,NULL);

  DBMS_OUTPUT.PUT_LINE('--- Reconciliation summary ---');
  DBMS_OUTPUT.PUT_LINE('tariffs_checked='||v_tariffs_checked||' expected=152');
  DBMS_OUTPUT.PUT_LINE('components_checked='||v_components_checked||' expected=180');
  DBMS_OUTPUT.PUT_LINE('inputs_checked='||v_inputs_checked||' expected=66');
  DBMS_OUTPUT.PUT_LINE('tiers_checked='||v_tiers_checked||' expected=15');
  DBMS_OUTPUT.PUT_LINE('mismatches='||v_errors||' expected=0');
  IF v_tariffs_checked<>152 THEN fail('GLOBAL','CBI1405','TARIFF_CALL_COUNT','152',TO_CHAR(v_tariffs_checked)); END IF;
  IF v_components_checked<>180 THEN fail('GLOBAL','CBI1405','COMPONENT_CALL_COUNT','180',TO_CHAR(v_components_checked)); END IF;
  IF v_inputs_checked<>66 THEN fail('GLOBAL','CBI1405','INPUT_CALL_COUNT','66',TO_CHAR(v_inputs_checked)); END IF;
  IF v_tiers_checked<>15 THEN fail('GLOBAL','CBI1405','TIER_CALL_COUNT','15',TO_CHAR(v_tiers_checked)); END IF;
  IF v_errors>0 THEN RAISE_APPLICATION_ERROR(-20260,'CBI 1405 source-to-Oracle reconciliation failed; mismatches='||v_errors); END IF;
  DBMS_OUTPUT.PUT_LINE('CBI Rial Fee 1405 PROVISIONAL source-to-Oracle reconciliation OK.');
END;
/
