PROMPT ============================================================
PROMPT DPS2 0.7.0 - Phase 7 Deposit Opening E2E Runtime Hardening
PROMPT Scope: database-level idempotency/concurrency guards only
PROMPT Existing business tables are not recreated.
PROMPT ============================================================

SET DEFINE OFF;
WHENEVER SQLERROR EXIT SQL.SQLCODE;

DECLARE
  v_count NUMBER;
BEGIN
  SELECT COUNT(*) INTO v_count
    FROM (
      SELECT IDEMPOTENCY_KEY
        FROM DPS2.DEPOSIT_OPENING_REQUEST
       WHERE IDEMPOTENCY_KEY IS NOT NULL
       GROUP BY IDEMPOTENCY_KEY
      HAVING COUNT(*) > 1
    );
  IF v_count > 0 THEN
    RAISE_APPLICATION_ERROR(-20701,
      'DPS2.DEPOSIT_OPENING_REQUEST contains duplicate IDEMPOTENCY_KEY values. Resolve duplicates before 0.7.0 migration.');
  END IF;

  SELECT COUNT(*) INTO v_count
    FROM (
      SELECT IDEMPOTENCY_KEY
        FROM DPS2.DEPOSIT_OPENING_BATCH
       WHERE IDEMPOTENCY_KEY IS NOT NULL
       GROUP BY IDEMPOTENCY_KEY
      HAVING COUNT(*) > 1
    );
  IF v_count > 0 THEN
    RAISE_APPLICATION_ERROR(-20702,
      'DPS2.DEPOSIT_OPENING_BATCH contains duplicate IDEMPOTENCY_KEY values. Resolve duplicates before 0.7.0 migration.');
  END IF;

  SELECT COUNT(*) INTO v_count
    FROM (
      SELECT OPENING_BATCH_ID, EXTERNAL_ROW_KEY
        FROM DPS2.DEPOSIT_OPENING_BATCH_ITEM
       WHERE EXTERNAL_ROW_KEY IS NOT NULL
       GROUP BY OPENING_BATCH_ID, EXTERNAL_ROW_KEY
      HAVING COUNT(*) > 1
    );
  IF v_count > 0 THEN
    RAISE_APPLICATION_ERROR(-20703,
      'DPS2.DEPOSIT_OPENING_BATCH_ITEM contains duplicate EXTERNAL_ROW_KEY inside a Batch. Resolve duplicates before 0.7.0 migration.');
  END IF;
END;
/

DECLARE
  FUNCTION unique_guard_exists(
    p_table_name IN VARCHAR2,
    p_column_list IN VARCHAR2
  ) RETURN BOOLEAN IS
    v_count NUMBER;
  BEGIN
    SELECT COUNT(*)
      INTO v_count
      FROM (
        SELECT i.index_name
          FROM all_indexes i
          JOIN all_ind_columns ic
            ON ic.index_owner = i.owner
           AND ic.index_name = i.index_name
         WHERE i.owner = 'DPS2'
           AND i.table_name = UPPER(p_table_name)
           AND i.uniqueness = 'UNIQUE'
           AND i.status = 'VALID'
         GROUP BY i.index_name
        HAVING LISTAGG(ic.column_name, ',') WITHIN GROUP (ORDER BY ic.column_position) = UPPER(p_column_list)
      );
    RETURN v_count > 0;
  END;
BEGIN
  IF NOT unique_guard_exists('DEPOSIT_OPENING_REQUEST', 'IDEMPOTENCY_KEY') THEN
    EXECUTE IMMEDIATE 'CREATE UNIQUE INDEX DPS2.UX_DEP_OPEN_REQ_IDEMPOTENCY ON DPS2.DEPOSIT_OPENING_REQUEST (IDEMPOTENCY_KEY)';
  END IF;

  IF NOT unique_guard_exists('DEPOSIT_OPENING_BATCH', 'IDEMPOTENCY_KEY') THEN
    EXECUTE IMMEDIATE 'CREATE UNIQUE INDEX DPS2.UX_DEP_OPEN_BATCH_IDEMPOTENCY ON DPS2.DEPOSIT_OPENING_BATCH (IDEMPOTENCY_KEY)';
  END IF;

  IF NOT unique_guard_exists('DEPOSIT_OPENING_BATCH_ITEM', 'OPENING_BATCH_ID,EXTERNAL_ROW_KEY') THEN
    EXECUTE IMMEDIATE 'CREATE UNIQUE INDEX DPS2.UX_DEP_OPEN_BATCH_EXT_ROW_KEY ON DPS2.DEPOSIT_OPENING_BATCH_ITEM (OPENING_BATCH_ID, EXTERNAL_ROW_KEY)';
  END IF;
END;
/
PROMPT 0.7.0 Phase 7 hardening migration completed.
