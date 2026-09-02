WHENEVER SQLERROR EXIT SQL.SQLCODE
SET DEFINE OFF
SET SERVEROUTPUT ON

PROMPT Installing GEO DDL...
@@ddl/01_geo_hierarchy_tables.sql
@@ddl/02_reference_data_tables.sql
@@ddl/03_foreign_cities.sql
@@ddl/04_name_romanization_dictionary.sql
PROMPT GEO DDL installation completed.
