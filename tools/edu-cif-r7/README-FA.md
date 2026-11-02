# EDU to CIF Cutover R7

Scope: four lookup SQL queries in EduLookupController and (if needed) the canonical Descriptor SCHEMA constant. No GEO change. No Oracle DDL/DML.

Extract under repository root, then run `tools\edu-cif-r7\INSTALL.cmd`. Installer backs up two files; it supports exact old R3 queries or already-converted R6 queries, but stops for unknown variants. Build separately after source verification.

Verify with: `findstr /n /i /c:"EDU.V_EDUCATION" /c:"CIF.EDUCATION" backend\src\main\java\com\behsazan\corebanking\referencedata\education\web\EduLookupController.java`

Do not retire EDU until all dependencies and parity have been checked.
