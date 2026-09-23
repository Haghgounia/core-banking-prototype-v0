-- Core Banking Prototype 0.11.0
-- DPS2 Phase 11F DB verifier
SET DEFINE OFF;
SET SERVEROUTPUT ON;
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK;
DECLARE
  v_pass NUMBER:=0; v_fail NUMBER:=0; v NUMBER;
  PROCEDURE ok(p VARCHAR2) IS BEGIN v_pass:=v_pass+1; DBMS_OUTPUT.PUT_LINE('PASS | '||p); END;
  PROCEDURE bad(p VARCHAR2) IS BEGIN v_fail:=v_fail+1; DBMS_OUTPUT.PUT_LINE('FAIL | '||p); END;
  PROCEDURE assert_table(p VARCHAR2) IS BEGIN SELECT COUNT(*) INTO v FROM ALL_TABLES WHERE OWNER='DPS2' AND TABLE_NAME=p; IF v=1 THEN ok(p||' exists'); ELSE bad(p||' missing'); END IF; END;
  PROCEDURE assert_index_cols(p_table VARCHAR2,p_cols VARCHAR2,p_label VARCHAR2) IS BEGIN SELECT COUNT(*) INTO v FROM (SELECT INDEX_NAME FROM ALL_IND_COLUMNS WHERE INDEX_OWNER='DPS2' AND TABLE_OWNER='DPS2' AND TABLE_NAME=p_table GROUP BY INDEX_NAME HAVING LISTAGG(COLUMN_NAME,',') WITHIN GROUP (ORDER BY COLUMN_POSITION)=p_cols); IF v>0 THEN ok(p_label); ELSE bad(p_label); END IF; END;
BEGIN
  DBMS_OUTPUT.PUT_LINE('============================================================');
  DBMS_OUTPUT.PUT_LINE('DPS2 Phase 11F - Term Deposit Operations DB Verifier');
  DBMS_OUTPUT.PUT_LINE('============================================================');
  assert_table('DEPOSIT_TERM_CONTRACT'); assert_table('DEPOSIT_MATURITY_EVENT'); assert_table('DEPOSIT_TERM_RENEWAL');
  assert_table('DEPOSIT_TERM_SETTLEMENT'); assert_table('DEPOSIT_TERM_CONVERSION'); assert_table('DEPOSIT_TERM_PARTIAL_WITHDRAWAL');
  assert_table('DEPOSIT_TERM_EARLY_TERMINATION'); assert_table('DEPOSIT_ACCOUNT_PRODUCT_HISTORY'); assert_table('DEPOSIT_OPERATION_APPROVAL_REQUEST');
  assert_table('DEPOSIT_OPERATION_IDEMPOTENCY'); assert_table('DEPOSIT_ACCOUNT_BALANCE');

  SELECT COUNT(*) INTO v FROM DPS2.DEPOSIT_OPENING_TERM OT JOIN DPS2.DEPOSIT_ACCOUNT A ON A.OPENING_REQUEST_ID=OT.OPENING_REQUEST_ID WHERE NOT EXISTS (SELECT 1 FROM DPS2.DEPOSIT_TERM_CONTRACT T WHERE T.ACCOUNT_ID=A.ACCOUNT_ID AND T.SOURCE_OPENING_TERM_ID=OT.OPENING_TERM_ID); IF v=0 THEN ok('every opening term is represented by operational term contract'); ELSE bad('every opening term is represented by operational term contract'); END IF;
  SELECT COUNT(*) INTO v FROM DPS2.DEPOSIT_TERM_CONTRACT WHERE MATURITY_DATE<=START_DATE OR PRINCIPAL_AMOUNT<0 OR TERM_VALUE<=0; IF v=0 THEN ok('term contract chronology and principal are valid'); ELSE bad('term contract chronology and principal are valid'); END IF;
  SELECT COUNT(*) INTO v FROM (SELECT ACCOUNT_ID FROM DPS2.DEPOSIT_TERM_CONTRACT WHERE STATUS_CODE='ACTIVE' GROUP BY ACCOUNT_ID HAVING COUNT(*)>1); IF v=0 THEN ok('at most one ACTIVE term contract per account'); ELSE bad('at most one ACTIVE term contract per account'); END IF;
  SELECT COUNT(*) INTO v FROM DPS2.DEPOSIT_TERM_PARTIAL_WITHDRAWAL W LEFT JOIN DPS2.DEPOSIT_OPERATION_APPROVAL_REQUEST A ON A.APPROVAL_REQUEST_ID=W.APPROVAL_REQUEST_ID WHERE W.STATUS_CODE IN ('APPROVED','EXECUTED') AND NVL(A.APPROVAL_STATUS_CODE,'?')<>'APPROVED'; IF v=0 THEN ok('approved/executed partial withdrawals have approved maker-checker request'); ELSE bad('approved/executed partial withdrawals have approved maker-checker request'); END IF;
  SELECT COUNT(*) INTO v FROM DPS2.DEPOSIT_TERM_RENEWAL R LEFT JOIN DPS2.DEPOSIT_OPERATION_APPROVAL_REQUEST A ON A.APPROVAL_REQUEST_ID=R.APPROVAL_REQUEST_ID WHERE R.STATUS_CODE IN ('APPROVED','EXECUTED') AND NVL(A.APPROVAL_STATUS_CODE,'?')<>'APPROVED'; IF v=0 THEN ok('approved/executed renewals have approved maker-checker request'); ELSE bad('approved/executed renewals have approved maker-checker request'); END IF;
  SELECT COUNT(*) INTO v FROM DPS2.DEPOSIT_TERM_CONVERSION C LEFT JOIN DPS2.DEPOSIT_OPERATION_APPROVAL_REQUEST A ON A.APPROVAL_REQUEST_ID=C.APPROVAL_REQUEST_ID WHERE C.STATUS_CODE IN ('APPROVED','EXECUTED') AND NVL(A.APPROVAL_STATUS_CODE,'?')<>'APPROVED'; IF v=0 THEN ok('approved/executed conversions have approved maker-checker request'); ELSE bad('approved/executed conversions have approved maker-checker request'); END IF;
  SELECT COUNT(*) INTO v FROM DPS2.DEPOSIT_TERM_EARLY_TERMINATION E WHERE E.STATUS_CODE='HANDED_OFF_TO_CLOSURE' AND NOT EXISTS (SELECT 1 FROM DPS2.DEPOSIT_ACCOUNT_CLOSURE C WHERE C.ACCOUNT_ID=E.ACCOUNT_ID AND C.CLOSURE_TYPE_CODE='EARLY_TERMINATION' AND C.REASON_CODE='TERM_EARLY_TERMINATION' AND C.REQUESTED_AT>=E.CREATED_AT); IF v=0 THEN ok('early termination handoff resolves to controlled closure'); ELSE bad('early termination handoff resolves to controlled closure'); END IF;
  assert_index_cols('DEPOSIT_TERM_CONTRACT','ACCOUNT_ID,STATUS_CODE','term contract account/status index coverage exists');
  assert_index_cols('DEPOSIT_TERM_PARTIAL_WITHDRAWAL','ACCOUNT_ID,STATUS_CODE','partial withdrawal account/status index coverage exists');
  assert_index_cols('DEPOSIT_TERM_CONVERSION','ACCOUNT_ID,STATUS_CODE','term conversion account/status index coverage exists');
  assert_index_cols('DEPOSIT_TERM_EARLY_TERMINATION','ACCOUNT_ID,STATUS_CODE','early termination account/status index coverage exists');
  assert_index_cols('DEPOSIT_MATURITY_EVENT','ACCOUNT_ID,MATURITY_DATE','maturity event account/date index coverage exists');

  DBMS_OUTPUT.PUT_LINE('------------------------------------------------------------');
  DBMS_OUTPUT.PUT_LINE('PHASE11F_DB_VERIFIER_PASS='||v_pass);
  DBMS_OUTPUT.PUT_LINE('PHASE11F_DB_VERIFIER_FAIL='||v_fail);
  IF v_fail>0 THEN RAISE_APPLICATION_ERROR(-21169,'Phase 11F DB verifier failed: '||v_fail); END IF;
  DBMS_OUTPUT.PUT_LINE('PHASE11F_DB_BASELINE_PASS');
END;
/
