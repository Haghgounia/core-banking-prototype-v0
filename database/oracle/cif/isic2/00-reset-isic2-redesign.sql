-- ============================================================================
-- ISIC2 redesign reset (prototype only)
-- Drops and recreates the two ISIC2 reference tables and their lookup view.
-- ============================================================================
BEGIN
  EXECUTE IMMEDIATE 'DROP VIEW CIF.V_REF_ISIC_ACTIVITY_LOOKUP2';
EXCEPTION WHEN OTHERS THEN IF SQLCODE != -942 THEN RAISE; END IF;
END;
/
BEGIN
  EXECUTE IMMEDIATE 'DROP TABLE CIF.REF_ISIC_ACTIVITY2 CASCADE CONSTRAINTS PURGE';
EXCEPTION WHEN OTHERS THEN IF SQLCODE != -942 THEN RAISE; END IF;
END;
/
BEGIN
  EXECUTE IMMEDIATE 'DROP TABLE CIF.REF_ISIC_RELEASE CASCADE CONSTRAINTS PURGE';
EXCEPTION WHEN OTHERS THEN IF SQLCODE != -942 THEN RAISE; END IF;
END;
/
PROMPT ISIC2 redesign objects reset.
