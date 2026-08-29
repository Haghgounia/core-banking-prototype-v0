WHENEVER SQLERROR EXIT SQL.SQLCODE
PROMPT [ISIC2] 1/4 Create release/activity2 tables and lookup view
@@01-create-isic2-tables.sql
PROMPT [ISIC2] 2/4 Import complete official UNSD ISIC Rev.4 hierarchy (766 rows)
@@02-import-isic-rev4-unsd.sql
PROMPT [ISIC2] 3/4 Register IR-SCI Rev.4 as inactive DRAFT release
@@03-register-ir-sci-release.sql
PROMPT [ISIC2] 4/4 Validate counts/hierarchy
@@04-verify-isic2.sql
PROMPT [ISIC2] Install complete.
