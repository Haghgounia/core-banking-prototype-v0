set pagesize 500
set linesize 240
set trimspool on
column table_name format a42
column column_name format a42
column data_type format a24
column data_default format a50

prompt ================================================================================
prompt DPS2 Deposit Opening - Reviewed UX metadata check
prompt ================================================================================

select table_name,
       column_id,
       column_name,
       data_type,
       data_length,
       data_precision,
       data_scale,
       nullable,
       data_default
  from all_tab_columns
 where owner = 'DPS2'
   and table_name in (
       'DEPOSIT_OPENING_PAYMENT_INSTRUMENT',
       'DEPOSIT_OPENING_MATURITY_INSTRUCTION',
       'DEPOSIT_OPENING_PROFIT_INSTRUCTION',
       'DEPOSIT_OPENING_DOCUMENT'
   )
 order by table_name, column_id;

prompt ================================================================================
prompt Target columns only
prompt ================================================================================

select table_name,
       column_id,
       column_name,
       data_type,
       data_length,
       data_precision,
       data_scale,
       nullable,
       data_default
  from all_tab_columns
 where owner = 'DPS2'
   and (
       (table_name = 'DEPOSIT_OPENING_PAYMENT_INSTRUMENT'
        and column_name in ('INSTRUMENT_HOLDER_ROLE_CODE','CHEQUEBOOK_LEAF_COUNT'))
    or (table_name = 'DEPOSIT_OPENING_MATURITY_INSTRUCTION'
        and column_name in ('SETTLEMENT_ACCOUNT_ID','SETTLEMENT_ACCOUNT_REFERENCE'))
    or (table_name = 'DEPOSIT_OPENING_PROFIT_INSTRUCTION'
        and column_name in ('DESTINATION_ACCOUNT_ID','DESTINATION_ACCOUNT_REFERENCE'))
    or (table_name = 'DEPOSIT_OPENING_DOCUMENT'
        and column_name = 'DOCUMENT_REFERENCE')
   )
 order by table_name, column_id;

prompt ================================================================================
prompt Constraints/FKs involving target columns
prompt ================================================================================

select c.table_name,
       cc.column_name,
       c.constraint_name,
       c.constraint_type,
       c.status,
       r.owner as referenced_owner,
       r.table_name as referenced_table,
       r.constraint_name as referenced_constraint
  from all_constraints c
  join all_cons_columns cc
    on cc.owner = c.owner
   and cc.constraint_name = c.constraint_name
  left join all_constraints r
    on r.owner = c.r_owner
   and r.constraint_name = c.r_constraint_name
 where c.owner = 'DPS2'
   and c.table_name in (
       'DEPOSIT_OPENING_PAYMENT_INSTRUMENT',
       'DEPOSIT_OPENING_MATURITY_INSTRUCTION',
       'DEPOSIT_OPENING_PROFIT_INSTRUCTION',
       'DEPOSIT_OPENING_DOCUMENT'
   )
   and cc.column_name in (
       'INSTRUMENT_HOLDER_ROLE_CODE',
       'CHEQUEBOOK_LEAF_COUNT',
       'SETTLEMENT_ACCOUNT_ID',
       'SETTLEMENT_ACCOUNT_REFERENCE',
       'DESTINATION_ACCOUNT_ID',
       'DESTINATION_ACCOUNT_REFERENCE',
       'DOCUMENT_REFERENCE'
   )
 order by c.table_name, cc.column_name, c.constraint_name;
