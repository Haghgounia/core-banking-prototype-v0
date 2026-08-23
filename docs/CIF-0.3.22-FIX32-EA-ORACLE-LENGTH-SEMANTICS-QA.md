# CIF 0.3.22 Fix32 - EA / Oracle Length Semantics QA

## Scope

Correction on top of Fix31 for character-length semantics in the EA/Oracle model comparison. No database schema or runtime persistence behavior changes.

## Reported case

The comparison detail could visually show:

- EA: `VARCHAR2(30)`
- Oracle: `VARCHAR2(30 CHAR)`

This is ambiguous when the EA XML does not declare `LengthType`. The comparator already avoided a semantics mismatch when EA semantics were absent, but the two displayed definitions still looked different. In addition, duplicate EA table definitions could contain richer `LengthType` metadata that was discarded when another duplicate was selected as the base definition.

## Fix

1. Duplicate EA definitions now enrich missing column metadata from sibling definitions by column name, including `LengthType`, length, precision/scale, nullable metadata and default value when the selected definition omits them.
2. When EA does not specify `LengthType`, Oracle `CHAR/BYTE` suffix is omitted only in the comparison display, matching the comparison rule that unspecified EA semantics are not asserted.
3. When EA explicitly specifies `CHAR` or `BYTE`, Oracle semantics remain visible and are compared exactly. An explicit `BYTE` vs Oracle `CHAR` remains a real difference.

## Verification

- `javac` compiled the actual Fix32 parser/comparison source with minimal framework stubs under Java 21.
- A Java harness parsed the supplied EA sample and confirmed `53` raw table definitions and `48` unique tables after merge.
- Harness result for unspecified EA semantics: `VARCHAR2(30)` vs Oracle `VARCHAR2(30 CHAR)` => displayed as `VARCHAR2(30)` / `VARCHAR2(30)`, status `MATCH`.
- Harness result for explicit EA `BYTE`: `VARCHAR2(30 BYTE)` vs Oracle `VARCHAR2(30 CHAR)` => status `DIFFERENT`.
- Added JUnit regression tests for duplicate `LengthType` enrichment, implicit-semantics normalization, and explicit `BYTE/CHAR` mismatch.
- Maven wrapper could not execute in this environment because its Maven distribution is not cached and external download is unavailable; the independent Java 21 compile/harness therefore covers the changed Java source directly.
- Fix32 static EA/Oracle verifier and the persisted-grid verifier pass.
- No database migration is required.
