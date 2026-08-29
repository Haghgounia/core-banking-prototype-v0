SET DEFINE OFF
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK
PROMPT === CIF ISIC2 FIX73 clean bilingual redesign install ===
PROMPT WARNING: REF_ISIC_RELEASE and REF_ISIC_ACTIVITY2 are dropped/recreated.
@00-reset-isic2-redesign.sql
@01-create-isic2-tables.sql
@02-import-isic-rev4-unsd.sql
@03-register-ir-sci-release.sql
@04-verify-isic2.sql
PROMPT === CIF ISIC2 FIX73 install completed ===
