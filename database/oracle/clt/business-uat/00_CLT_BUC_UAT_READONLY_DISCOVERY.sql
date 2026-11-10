-- READ-ONLY: bank-owned CLT schema metadata, no DDL or DML.
SELECT table_name,column_name,data_type,data_length,nullable
FROM all_tab_columns
WHERE owner='CLT' AND table_name IN
('CLT_BUC_UAT_RUN','CLT_BUC_UAT_RESULT','CLT_BUC_UAT_EVENT')
ORDER BY table_name,column_id;
SELECT sequence_name FROM all_sequences
WHERE sequence_owner='CLT' AND sequence_name='CLT_BUC_UAT_EVENT_SEQ';
