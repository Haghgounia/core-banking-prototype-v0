# CIF 0.3.22 Fix21 - Reference Column Persian Comments QA

## Scope

- User-provided missing-comment list: **991 columns / 99 tables**.
- Current governed Party reference metadata: **994 columns / 99 tables**.
- Extra Fix20 attributes included proactively: `REF_RISK_MODEL.MODEL_VERSION`, `MIN_SCORE`, `MAX_SCORE`.

## Resolution

1. Added idempotent migration `database/oracle/cif/migrations/0.3.22-fix21-reference-column-comments-fa.sql`.
2. Added Persian `COMMENT ON COLUMN` statements to all six reference-data phase DDL files so fresh installs receive the same comments.
3. Persian comments are generated from governed metadata labels and table titles.
4. Added review catalog `docs/CIF-0.3.22-FIX21-REFERENCE-COLUMN-COMMENTS-FA.csv`.

## Verification

- Attached missing list fully covered: **991 / 991**.
- Current governed reference columns covered: **994 / 994**.
- Reference tables covered: **99 / 99**.
- Migration checks column existence before applying comments, so it is safe on environments where a reference phase has not yet been installed.
- `COMMENT ON COLUMN` is idempotent in effect: rerunning replaces the same governed comment.

## Deployment

Run after prior CIF migrations, especially Fix20:

```sql
@database/oracle/cif/migrations/0.3.22-fix21-reference-column-comments-fa.sql
```

Expected verification result after all 99 reference tables are installed: `MISSING_FA_COMMENT_COUNT = 0`.
