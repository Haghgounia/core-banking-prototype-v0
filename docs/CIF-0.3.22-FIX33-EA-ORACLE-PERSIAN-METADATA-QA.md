# CIF 0.3.22 Fix33 — EA / Oracle Persian Metadata Comparison QA

## Baseline
- Source baseline: `0.3.22-prototype-fix32`
- Output version: `0.3.22-prototype-fix33`
- Database migration: **not required**

## Requested gap
Fix32 compared physical EA/Oracle metadata (table/column existence, type/length/precision/scale, nullable, PK, row counts) but did not include Persian names/comments in the match decision.

## Implemented mapping
### Enterprise Architect XMI
- Table Persian title: `UML:TaggedValue tag="alias"`
- Table description/comment: `UML:TaggedValue tag="documentation"`
- Column Persian description/comment: `UML:TaggedValue tag="description"`

EA rich-text wrappers such as `<span dir="rtl">...</span>` are removed before comparison.

### Oracle
- Table comment: `ALL_TAB_COMMENTS.COMMENTS`
- Column comment: `ALL_COL_COMMENTS.COMMENTS`

Oracle does not expose a separate native table alias. Therefore:
- EA `alias` is checked against Oracle TABLE COMMENT using normalized containment, so a short Persian title can match a longer Oracle comment that contains it.
- EA `documentation` is compared with Oracle TABLE COMMENT using normalized text equality.
- EA column `description` is compared directly with Oracle COLUMN COMMENT using normalized text equality.

## Normalization
Before Persian metadata comparison:
- Unicode NFKC normalization is applied.
- Arabic/Persian character variants such as `ي/ی` and `ك/ک` are unified.
- whitespace, ZWNJ/directional formatting characters and punctuation are ignored for equality purposes.
- original human-readable text is still returned to the UI and shown in the difference report.

## Status behavior
- Column COMMENT mismatch -> `ColumnStatus.DIFFERENT`.
- Table alias/documentation mismatch -> `TableStatus.DIFFERENT` even if the physical table structure matches.
- The existing per-row «جزئیات اختلاف» link therefore also opens Persian metadata differences.

## UI
The comparison detail now shows:
- EA Persian table title (Alias)
- EA table Documentation
- Oracle TABLE COMMENT
- a dedicated Persian metadata status in the table grid
- EA column Persian description
- Oracle COLUMN COMMENT
- exact Persian metadata difference messages

CSV table-level export also includes the table Persian metadata fields/status.

## Sample XMI smoke test
File: `Party-Operation_Froms-1.xml`

Standalone Java parser smoke result:
- raw EA table definitions: **53**
- unique merged tables: **48**
- tables with EA Alias: **48**
- tables with EA Documentation: **48**
- parsed columns with EA Description: **816**

`PARTY_CLASSIFICATION` was verified to expose its Persian Alias, Documentation and column descriptions from the supplied XMI.

## Static / compile QA
- Modified backend comparison classes compiled with `javac 21` using minimal Spring/JDBC API stubs: **PASS**.
- Standalone comparison smoke:
  - normalized Persian/Arabic variants and spacing -> `MATCH`: **PASS**
  - different table/column comments -> `DIFFERENT`: **PASS**
- TypeScript transpile syntax check for modified comparison component/models: **0 errors**.
- HTML tag-balance check: **PASS**.
- `tools/verify-ea-oracle-comparison.mjs`: **PASS**.
- `tools/verify-cif-persisted-grids.mjs`: **PASS**.

## Full Maven limitation
The Maven Wrapper attempted to download Maven 3.9.16 from Maven Central and the runtime could not fetch it, so the normal Maven/JUnit execution was unavailable in this environment. The targeted production classes were nevertheless compiled with JDK 21 and the parser/comparator were exercised through standalone smoke harnesses.
