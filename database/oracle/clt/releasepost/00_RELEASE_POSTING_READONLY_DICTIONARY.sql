-- BUC-11 / safe read-only DBA reconnaissance (NO DDL/DML/COMMIT)
SELECT owner, table_name, COUNT(*) column_count
  FROM all_tab_columns WHERE owner='CLT' AND table_name IN
 ('COLLATERAL','COLLATERAL_STATUS_HISTORY','COLLATERAL_ALLOCATION',
  'COLLATERAL_TRANSACTION','REF_COLLATERAL_STATUS','CLT_FINAL_RELEASE_PERMIT')
 GROUP BY owner,table_name ORDER BY table_name;
SELECT constraint_name,constraint_type,status,validated
  FROM all_constraints WHERE owner='CLT'
   AND table_name IN ('COLLATERAL_STATUS_HISTORY','CLT_FINAL_RELEASE_PERMIT')
 ORDER BY table_name,constraint_name;
SELECT collateral_status_code,is_active_status_flag,is_terminal_status_flag,status
  FROM clt.ref_collateral_status ORDER BY collateral_status_code;
SELECT COUNT(*) active_allocations FROM clt.collateral_allocation WHERE used='YES';
SELECT COUNT(*) unknown_allocations FROM clt.collateral_allocation
 WHERE used IS NULL OR used NOT IN ('YES','NO');
