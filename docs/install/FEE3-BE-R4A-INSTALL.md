# FEE3-BE-R4A | Calculation Contract, Deterministic Evidence, Read-Only Preview

**Release candidate:** 2026-10-27. Requires installed and qualified BE-R3 + QA3 on Core Banking Prototype 0.11.0.

## Scope and safety

This is the *first* R4 milestone (R4-A), **not** certification of all tariff calculation models.
The patch adds five isolated Java source files under `fee3/r4`, an offline numeric probe,
JDK/contract verification helpers, and a loopback-only runtime read-only acceptance test.
No existing production class, build wrapper, Angular file, SQL DDL, seed row or Oracle data is modified by the installer.
Spring Boot component scanning will discover the new Java Controller/Service/Repository on a rebuilt JAR.

- Endpoint: `POST /api/v1/fee3/r4/preview`, JSON `{"draft": ..., "inputs": ..., "effectiveOn": "YYYY-MM-DD"}`.
  `effectiveOn` is optional and defaults to the **Oracle bank date** (Asia/Tehran).
- Supported here: **general SERVICE_FEE** with a **single** `FIXED`, `PERCENT`, or `MIXED` rule,
  using the already-tested bounded R3 arithmetic; not advanced formulas or specialized financial models.
- This endpoint is **simulation only**, not charge authorization, approval, publication, tax determination,
  posting, collection, or a pricing decision for live customer operations.
- Server reads current Oracle date and R3 authoritative operation references; it does not trust client local date.
- Effective date must be today or later, fall within the supplied version's inclusive validity interval.
  Future-date testing is identified as UAT simulation, never live tariff applicability.
- The current data baseline has 300 DRAFT versions, 0 ACTIVE; there is no basis for a production tariff selection.
- IRR amount inputs, fixed/min/max obey **max 2 fractional digits** (Oracle NUMBER(22,2)); rate percentages
  obey **max 8 digits** (NUMBER(18,8)); coverage percent obeys **max 4 digits** (NUMBER(12,4)).
  The legacy R3 engine additionally caps amounts at 10^18 IRR and caps percentages at 1000,
  both deliberately more restrictive than some physical column maxima. Unsupported precision is BLOCKED, never rounded silently.
- Applies one explicitly selected integer-rial rounding policy (`HALF_UP`, `HALF_EVEN`, `FLOOR`/`DOWN`, `CEIL`/`UP`).
  Caps precede rounding; tax is computed only when `TAXABLE` with explicit `taxPercent`. No default tax rate is invented.
- The response reports `simulationMode`, source provenance, original R3 calculation breakdown,
  effective business date, rounding policy, SHA-256 of canonical simulation snapshot, and explicit false flags
  for `published`, `approved`, `persisted`, `financialPosting`.
- Do not infer bank approval merely because a deterministic hash or amount is returned.

## Oracle reference integrity

For **unsaved** requests, omit *all* four IDs/version fields (`calcItemId`, `tariffVersionId`,
`expectedItemRecordVersion`, `expectedVersionRecordVersion`); the response is labelled
`UNPERSISTED_CLIENT_DRAFT`.

If IDs are supplied, both item/version IDs and both expected record-version values are mandatory.
The R4-A repository reads the single pair using **parameterized SELECT**. It must match:
- DRAFT inactive state, exact Oracle record versions, item identity and metadata,
  effective dates, tax/rounding/financial rules;
- the server-owned `_fee3UiV1` payload (including contextual metadata), and full client financial policy;
- exactly one canonical version scope with matching JSON dimensions and currency;
- exactly one numeric calculation rule with matching method, rate source, caps and params.

If any required value differs, R4-A returns BLOCKED with no amount.
This is **proof of the calculation-driving fields only**: stored test cases, audit history and
organizational authorization are not certified by this read-only snapshot comparison.
Seed versions without the R2 server-owned UI envelope are intentionally rejected for this flow.

## Install (Windows CMD)

1. Stop any financial-transaction UAT tasks; keep all workflow/posting feature flags OFF.
2. Extract `FEE3-BE-R4A-SAFE-PATCH-20261027.zip` in `D:\Projects\core-banking-prototype-v0`.
3. The package contains an installer, manifest and payload in `tools\fee3-backend\releases\r4a`.
4. Execute:

```bat
cd /d D:\Projects\core-banking-prototype-v0
python tools\fee3-backend\releases\r4a\install.py --preview
python tools\fee3-backend\releases\r4a\install.py --apply
python tools\fee3-backend\r4a\verify-r4a.py
python tools\fee3-backend\r4a\verify-java-signatures.py
bin\stop.cmd
build-production.cmd
bin\start.cmd
curl.exe --noproxy "*" http://127.0.0.1:8091/api/v1/fee3/readiness
call tools\fee3-backend\r4a\run-runtime-readonly.cmd
```

The signature-only checker compiles against stubs and **is not equivalent to an actual Spring/Maven compile**.
A successful `build-production.cmd` and loopback runtime test are required for final acceptance.
If port 8091 has no listener or `readiness` does not return `readsEnabled=true`, do NOT run the runtime test.
No database script is required to install R4-A.

Runtime report: `docs\install\FEE3-R4A-RUNTIME-RESULT.json`.
Compare the prior Oracle no-write baselines (297 items, 300 versions, 300 scopes,
313 rules, 0 change requests, 297 audit events), using your existing read-only SQL.

## Next gates

- R4-B: specialized services, guarantee fee and collateral portfolio audit reference cases;
- R4-C: contract-confirmed Jalali year fractions, partnership profit, Qard and late-damage policies;
- R4-D: comparison with the original standalone HTML and expert-reviewed numeric golden cases;
- Maker/Checker authentication, publication, transaction collection and accounting posting remain blocked.
