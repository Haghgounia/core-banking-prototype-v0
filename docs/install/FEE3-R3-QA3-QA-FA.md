# FEE3-R3-QA3 - quality and scope

## Proven root cause

Source of truth: `backend/src/main/java/com/behsazan/corebanking/fee3/domain/Fee3Models.java`:

```java
public record Page<T>(List<T> items, long total, int page, int size) {}
```

The R1 controller returns this record from `GET /api/v1/fee3/items`.
QA2 checked `items.get("rows")` while its stub returned `rows`. This was a self-consistent
but incorrect test fixture and caused one false negative with a real HTTP 200 response.

## QA3 corrections

- Correct JSON property `items`; verify actual page metadata, row identity and catalog total.
- Include safe metadata in report (`keys`, `total`, `page`, `size`, `itemCount`, `expectedTotal`),
  not catalog content, tokens or customer data.
- Stop immediately without POST upon catalog schema or parity failure.
- Use actual Java record shape in local HTTP mock.
- Add negative tests for old `rows` JSON, wrong total, missing page, bad row identity,
  and empty first page despite positive total.

## Verification matrix

1. Python syntax compile: `py_compile` for all QA3 scripts.
2. Deterministic mock behavioral tests: 16 expected successes, no Oracle access.
3. Package checksum/manifest verification and ZIP CRC.
4. Exact-byte, additive install on a clean copy with QA2: no existing-file modification.
5. Baseline R3 source qualification, where tooling is available.
6. Real Windows runtime QA3 + Oracle before/after: **not executed in this environment**.

## Safety boundaries

Application files, database schema and seed: unchanged. No financial operations.
Runtime POST allowlist remains non-persisting validation/preview only.
Flags for workflow, approval and posting are required to be false.
