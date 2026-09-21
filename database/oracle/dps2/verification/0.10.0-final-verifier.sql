SET SERVEROUTPUT ON SIZE UNLIMITED
SET DEFINE OFF
SET VERIFY OFF
SET FEEDBACK ON
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

PROMPT ============================================================
PROMPT Core Banking Prototype 0.10.0 - FINAL DB VERIFIER
PROMPT Scope: DPS2 four-deposit baseline + Phase 10F runtime contract
PROMPT Read-only verifier; no business data is changed.
PROMPT ============================================================

DECLARE
  v_pass NUMBER := 0;
  v_fail NUMBER := 0;
  v_n    NUMBER;

  PROCEDURE check_count(p_label VARCHAR2, p_sql VARCHAR2, p_expected NUMBER) IS
    l_n NUMBER;
  BEGIN
    EXECUTE IMMEDIATE p_sql INTO l_n;
    IF l_n = p_expected THEN
      v_pass := v_pass + 1;
      DBMS_OUTPUT.PUT_LINE('PASS | ' || RPAD(p_label, 58) || ' | ' || l_n);
    ELSE
      v_fail := v_fail + 1;
      DBMS_OUTPUT.PUT_LINE('FAIL | ' || RPAD(p_label, 58) || ' | actual=' || l_n || ', expected=' || p_expected);
    END IF;
  END;

  PROCEDURE check_min(p_label VARCHAR2, p_sql VARCHAR2, p_min NUMBER) IS
    l_n NUMBER;
  BEGIN
    EXECUTE IMMEDIATE p_sql INTO l_n;
    IF l_n >= p_min THEN
      v_pass := v_pass + 1;
      DBMS_OUTPUT.PUT_LINE('PASS | ' || RPAD(p_label, 58) || ' | ' || l_n);
    ELSE
      v_fail := v_fail + 1;
      DBMS_OUTPUT.PUT_LINE('FAIL | ' || RPAD(p_label, 58) || ' | actual=' || l_n || ', minimum=' || p_min);
    END IF;
  END;
BEGIN
  -- Core account schema.
  check_count('DPS2.DEPOSIT_ACCOUNT exists',
    q'[select count(*) from all_tables where owner='DPS2' and table_name='DEPOSIT_ACCOUNT']', 1);
  check_count('DPS2.DEPOSIT_ACCOUNT_LIFECYCLE_EVENT exists',
    q'[select count(*) from all_tables where owner='DPS2' and table_name='DEPOSIT_ACCOUNT_LIFECYCLE_EVENT']', 1);
  check_count('SEQ_DEPOSIT_ACCOUNT exists',
    q'[select count(*) from all_sequences where sequence_owner='DPS2' and sequence_name='SEQ_DEPOSIT_ACCOUNT']', 1);
  check_count('Account status constraint enabled',
    q'[select count(*) from all_constraints where owner='DPS2' and table_name='DEPOSIT_ACCOUNT' and constraint_name='CK_DEP_ACCOUNT_STATUS' and status='ENABLED']', 1);

  -- Append-only audit guards.
  check_count('Four append-only Phase 5 triggers enabled',
    q'[select count(*) from all_triggers where owner='DPS2' and trigger_name in ('TRG_DEP_OPEN_AUD_EVT_APPEND_ONLY','TRG_DEP_OPEN_AUD_FLD_APPEND_ONLY','TRG_DEP_OPEN_SNAPSHOT_APPEND_ONLY','TRG_DEP_OPEN_STATUS_APPEND_ONLY') and status='ENABLED']', 4);

  -- Phase 7 idempotency semantic guards: names are intentionally not assumed.
  check_min('Unique request IDEMPOTENCY_KEY guard', q'[
    select count(*) from (
      select i.index_name
        from all_indexes i join all_ind_columns c
          on c.index_owner=i.owner and c.index_name=i.index_name
       where i.owner='DPS2' and i.table_name='DEPOSIT_OPENING_REQUEST'
         and i.uniqueness='UNIQUE' and i.status='VALID'
       group by i.index_name
      having listagg(c.column_name, ',') within group(order by c.column_position)='IDEMPOTENCY_KEY'
    )]', 1);
  check_min('Unique batch IDEMPOTENCY_KEY guard', q'[
    select count(*) from (
      select i.index_name
        from all_indexes i join all_ind_columns c
          on c.index_owner=i.owner and c.index_name=i.index_name
       where i.owner='DPS2' and i.table_name='DEPOSIT_OPENING_BATCH'
         and i.uniqueness='UNIQUE' and i.status='VALID'
       group by i.index_name
      having listagg(c.column_name, ',') within group(order by c.column_position)='IDEMPOTENCY_KEY'
    )]', 1);
  check_min('Unique batch item external row guard', q'[
    select count(*) from (
      select i.index_name
        from all_indexes i join all_ind_columns c
          on c.index_owner=i.owner and c.index_name=i.index_name
       where i.owner='DPS2' and i.table_name='DEPOSIT_OPENING_BATCH_ITEM'
         and i.uniqueness='UNIQUE' and i.status='VALID'
       group by i.index_name
      having listagg(c.column_name, ',') within group(order by c.column_position)='OPENING_BATCH_ID,EXTERNAL_ROW_KEY'
    )]', 1);

  -- Phase 10F FUND_ALLOC canonical contract.
  check_count('FUND_ALLOC canonical ALLOCATED_AMOUNT column',
    q'[select count(*) from all_tab_columns where owner='DPS2' and table_name='DEPOSIT_OPENING_FUND_ALLOC' and column_name='ALLOCATED_AMOUNT' and data_type='NUMBER' and data_precision=19 and data_scale=4 and nullable='N']', 1);
  check_count('FUND_ALLOC legacy ALLOCATION_AMOUNT absent',
    q'[select count(*) from all_tab_columns where owner='DPS2' and table_name='DEPOSIT_OPENING_FUND_ALLOC' and column_name='ALLOCATION_AMOUNT']', 0);

  -- Created-account final contract.
  check_count('Final created-account constraint enabled',
    q'[select count(*) from all_constraints where owner='DPS2' and table_name='DEPOSIT_OPENING_REQUEST' and constraint_name='CHK_DEPOSIT_OPENING_REQUEST_REQUEST_STATUS_CODE_CREATED_ACCOUNT_ID' and status='ENABLED']', 1);
  check_count('Obsolete account ID/number pair constraint absent',
    q'[select count(*) from all_constraints where owner='DPS2' and table_name='DEPOSIT_OPENING_REQUEST' and constraint_name in ('CHK_DEPOSIT_OPENING_REQUEST_CREATED_ACCOUNT_ID_CREATED_ACCOUNT_NO','CHK_DEPOSIT_OPENING_REQUEST_REQUEST_STATUS_CODE_CREATED_ACCOUNT_ID_CREATED_ACCOUNT_NO')]', 0);
  check_count('No COMPLETED request without CREATED_ACCOUNT_ID',
    q'[select count(*) from DPS2.DEPOSIT_OPENING_REQUEST where REQUEST_STATUS_CODE='COMPLETED' and CREATED_ACCOUNT_ID is null]', 0);

  -- Opening check result contract.
  check_count('Canonical result-status constraint enabled',
    q'[select count(*) from all_constraints where owner='DPS2' and table_name='DEPOSIT_OPENING_CHECK' and constraint_name='CHK_DEPOSIT_OPENING_CHECK_RESULT_STATUS_CODE' and status='ENABLED']', 1);
  check_count('Five canonical result statuses active',
    q'[select count(*) from DPS2.REF_DEP_OPEN_CHECK_RESULT where RESULT_STATUS_CODE in ('PASS','FAIL','PENDING','WAIVED','NOT_APPLICABLE') and IS_ACTIVE=1]', 5);
  check_count('No non-canonical opening-check status rows',
    q'[select count(*) from DPS2.DEPOSIT_OPENING_CHECK where RESULT_STATUS_CODE is not null and RESULT_STATUS_CODE not in ('PASS','FAIL','PENDING','WAIVED','NOT_APPLICABLE')]', 0);

  -- Runtime reference catalogs that previously caused Phase 10F failures.
  check_min('Activation status catalog available',
    q'[select count(*) from DPS2.REF_DEP_OPEN_ACTIVATION_STATUS where IS_ACTIVE=1]', 1);
  check_min('Joint basis catalog available',
    q'[select count(*) from DPS2.REF_DEP_OPEN_JOINT_BASIS where IS_ACTIVE=1]', 1);
  check_min('Funding purpose catalog available',
    q'[select count(*) from DPS2.REF_DEP_OPEN_FUND_PURPOSE where IS_ACTIVE=1]', 1);
  check_min('Obligation type catalog available',
    q'[select count(*) from DPS2.REF_DEP_OPEN_OBLIGATION_TYPE where IS_ACTIVE=1]', 1);
  check_min('Settlement status catalog available',
    q'[select count(*) from DPS2.REF_DEP_OPEN_SETTLEMENT_STATUS where IS_ACTIVE=1]', 1);
  check_min('Check phase catalog available',
    q'[select count(*) from DPS2.REF_DEP_OPEN_CHECK_PHASE where IS_ACTIVE=1]', 1);
  check_min('Blocking scope catalog available',
    q'[select count(*) from DPS2.REF_DEP_OPEN_BLOCKING_SCOPE where IS_ACTIVE=1]', 1);

  -- CREATED_AT default reconciliation: every Deposit Opening table with CREATED_AT must have a default.
  check_count('Opening tables with CREATED_AT but no default', q'[
    select count(*)
      from all_tab_columns
     where owner='DPS2'
       and table_name like 'DEPOSIT_OPENING\_%' escape '\'
       and column_name='CREATED_AT'
       and nvl(default_length,0)=0
  ]', 0);

  -- Product family qualification prerequisites.
  check_count('Four Phase10F PDL product codes exist', q'[
    select count(*) from PDL.PRODUCT
     where PRODUCT_CODE in ('P10F_QARD_SAVINGS','P10F_CURRENT_ACCOUNT','P10F_SHORT_TERM','P10F_LONG_TERM')
  ]', 4);
  check_count('Four current Phase10F product versions exist', q'[
    select count(distinct p.PRODUCT_CODE)
      from PDL.PRODUCT p join PDL.PRODUCT_VERSION v on v.PRODUCT_ID=p.PRODUCT_ID
     where p.PRODUCT_CODE in ('P10F_QARD_SAVINGS','P10F_CURRENT_ACCOUNT','P10F_SHORT_TERM','P10F_LONG_TERM')
       and v.IS_CURRENT=1
  ]', 4);

  -- Qualification evidence: at least four ACTIVE accounts exist for the four P10F products.
  check_count('All four Phase10F product families reached ACTIVE', q'[
    select count(distinct p.PRODUCT_CODE)
      from DPS2.DEPOSIT_ACCOUNT a
      join PDL.PRODUCT_VERSION v on v.PRODUCT_VERSION_ID=a.PRODUCT_VERSION_ID
      join PDL.PRODUCT p on p.PRODUCT_ID=v.PRODUCT_ID
     where a.ACCOUNT_STATUS_CODE='ACTIVE'
       and p.PRODUCT_CODE in ('P10F_QARD_SAVINGS','P10F_CURRENT_ACCOUNT','P10F_SHORT_TERM','P10F_LONG_TERM')
  ]', 4);

  DBMS_OUTPUT.PUT_LINE('------------------------------------------------------------');
  DBMS_OUTPUT.PUT_LINE('FINAL_DB_VERIFIER_PASS=' || v_pass);
  DBMS_OUTPUT.PUT_LINE('FINAL_DB_VERIFIER_FAIL=' || v_fail);
  IF v_fail > 0 THEN
    RAISE_APPLICATION_ERROR(-21090, '0.10.0 final DB verifier failed; failed checks=' || v_fail);
  END IF;
  DBMS_OUTPUT.PUT_LINE('FINAL_DB_BASELINE_PASS');
END;
/

PROMPT ============================================================
PROMPT FINAL_DB_BASELINE_PASS
PROMPT ============================================================
EXIT SUCCESS
