# CIF 0.3.22 Fix29 - Windows Verifier Path QA

## Scope
Fixes the Fix28 Windows build failure in `tools/verify-cif-persisted-grids.mjs`.

## Root cause
`new URL(import.meta.url).pathname` returns a URL-style path such as `/D:/Projects/...` on Windows. Passing that value to `path.resolve()` can prefix the current drive again and produce an invalid path like `D:\D:\Projects\...`.

## Fix
The verifier now resolves its own filesystem location with Node's cross-platform API:

```js
import { fileURLToPath } from 'node:url';
const scriptFile = fileURLToPath(import.meta.url);
const root = path.resolve(path.dirname(scriptFile), '..');
```

## Verification performed
- `node --check tools/verify-cif-persisted-grids.mjs` : PASS
- Verifier executed from project root : PASS
- Verifier executed from an unrelated working directory (`/tmp`) : PASS
- Persisted-grid regression result: 67 CIF grids; no stream/card renderer; dockable sidebar checks PASS
- Source diff against Fix28 confirms no functional change to persisted grids/sidebar beyond version metadata and verifier path fix
- No database migration required

## Expected Windows build output
Before `npm install`, `build-production.cmd` should print:

```text
Fix29 persisted-grid verification OK: 67 CIF grids, no stream/card record renderers, dockable sidebar verified.
```

The previous `ENOENT` path `D:\D:\Projects\...` must no longer occur.
