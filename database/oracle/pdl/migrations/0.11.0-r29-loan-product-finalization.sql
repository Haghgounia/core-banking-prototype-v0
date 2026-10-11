-- R29: Loan PRODUCT_CODE is issued ONLY after the product builder's final approval gate.
-- Requires R28 registry and working-year migration. Run as PDL owner or authorized DBA.
-- Oracle DDL auto-commits. Take a schema backup and preflight constraints/triggers first.
-- Idempotent: safe to re-run after successful completion.
SET DEFINE OFF;
DECLARE
  v_nullable VARCHAR2(1);
  v_constraint NUMBER;
  v_registry NUMBER;
BEGIN
  SELECT COUNT(*) INTO v_registry FROM ALL_TABLES
    WHERE OWNER='PDL' AND TABLE_NAME='PRODUCT_LOAN_CODE_REGISTRY';
  IF v_registry <> 1 THEN
    RAISE_APPLICATION_ERROR(-20029, 'R29 requires R28 PRODUCT_LOAN_CODE_REGISTRY migration first.');
  END IF;

  -- Add the restrictive CHECK before relaxing NOT NULL: a partial DDL failure
  -- must never leave DEPOSIT products without a database-enforced code rule.
  SELECT COUNT(*) INTO v_constraint FROM ALL_CONSTRAINTS
    WHERE OWNER='PDL' AND TABLE_NAME='PRODUCT'
      AND CONSTRAINT_NAME='CK_PDL_PRODUCT_CODE_ISSUED';
  IF v_constraint=0 THEN
    EXECUTE IMMEDIATE 'ALTER TABLE PDL.PRODUCT ADD CONSTRAINT CK_PDL_PRODUCT_CODE_ISSUED ' ||
      'CHECK (PRODUCT_CODE IS NOT NULL OR ' ||
      '(PRODUCT_CLASS_CODE = ''LOAN'' AND PRODUCT_STATUS_CODE = ''DRAFT'')) ENABLE VALIDATE';
  END IF;

  SELECT NULLABLE INTO v_nullable FROM ALL_TAB_COLUMNS
    WHERE OWNER='PDL' AND TABLE_NAME='PRODUCT' AND COLUMN_NAME='PRODUCT_CODE';
  IF v_nullable='N' THEN
    EXECUTE IMMEDIATE 'ALTER TABLE PDL.PRODUCT MODIFY (PRODUCT_CODE NULL)';
  END IF;
END;
/
-- KEEP PDL.UQ_PRODUCT_CODE unique; Oracle permits multiple NULL values in UNIQUE columns.
-- Deposits and non-DRAFT loans must always have an issued product code.
-- Legacy loans with an existing code are retained; never regenerate their codes.
