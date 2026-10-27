# FEE3-R3-QA3 - Oracle catalog Page<T> read-only qualification hotfix

## Reason / demonstrated evidence

User runtime on 2026-10-27: `FEE3_R3_QUALIFICATION_PASS=27 FAIL=1`.
`GET /items?page=0&size=5` responded HTTP 200, but the QA2 assertion expected `rows`.
The actual Java contract is `Fee3Models.Page<T>(items, total, page, size)`.
The QA2 mock had incorrectly returned `rows`, so its selftest did not catch the mismatch.
No Oracle/Backend fault has been established by this single false failure.

## Changes and safety

- An additive standalone QA3 package; **NO overwrite** of QA1, QA2, application source, configuration, or data.
- Actual `Page<T>` verification checks `items`, `total`, `page`, `size`, first-page length,
  catalog identity and parity with the live Oracle `calcItemCount`.
- The test refuses to POST after an invalid catalog response (fail-closed).
- Direct loopback connection, no proxies, no redirects, no credentials (QA2 policy retained).
- POST allowlist is exactly `/validation` and `/preview`; these API operations are non-persisting.
- No draft, change request, approval, posting, DDL, DML, seed, or migration changes.
- The mock returns the actual Page JSON shape and includes negative regressions.

## Windows CMD installation

Extract this ZIP to project root (`D:\Projects\core-banking-prototype-v0`), preserving folders.
Before copying files, inspect the ZIP: it should only add `r3-qa3`, two `docs/install` files and a manifest.

```bat
cd /d D:\Projects\core-banking-prototype-v0
python tools\fee3-backend\qualification\r3-qa3\verify-kit.py
python tools\fee3-backend\qualification\r3-qa3\selftest.py
curl.exe --noproxy "*" http://127.0.0.1:8091/api/v1/fee3/readiness
call tools\fee3-backend\qualification\r3-qa3\run-runtime-readonly.cmd
```

No Maven/Angular rebuild and no service restart required (only test tools added).

## Expected result and evidence

Expected if the same backend responses remain stable:
`FEE3_R3_QUALIFICATION_PASS=28 FAIL=0`.
Actual runtime acceptance requires **your Windows run**, not the mock tests here.
The generated report is `docs\install\FEE3-R3-QA3-RUNTIME-RESULT.json`.

Afterwards, rerun the previously supplied `FEE3-R3-Oracle-Check.sql` and confirm
FCE_CALC_ITEM=297, FCE_TARIFF_VERSION=300, FCE_CALC_RULE=313,
FCE_CHANGE_REQUEST=0, FCE_AUDIT_EVENT=297, active versions=0.
A different result must be investigated; do not auto-correct counts or data.

Do not enable Maker/Checker, draft-write, tariff publication or financial posting.
