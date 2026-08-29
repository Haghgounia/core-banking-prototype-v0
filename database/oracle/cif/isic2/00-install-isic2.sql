SET DEFINE OFF
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK
PROMPT === CIF ISIC 0.3.64 normalized hierarchy/notes install ===
PROMPT WARNING: REF_ISIC_RELEASE, REF_ISIC_ACTIVITY2 and REF_ISIC_ACTIVITY_NOTE are dropped/recreated.
PROMPT Legacy CIF.REF_ISIC_ACTIVITY is NOT changed.
@00-reset-isic2-redesign.sql
@01-create-isic2-tables.sql
@02-import-isic-rev4-unsd.sql
@03-register-ir-sci-release.sql
@04-verify-isic2.sql
PROMPT === CIF ISIC 0.3.64 install completed ===
