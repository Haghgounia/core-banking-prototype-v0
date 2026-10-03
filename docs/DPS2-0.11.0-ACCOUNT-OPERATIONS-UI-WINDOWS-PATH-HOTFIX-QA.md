# DPS2 0.11.0 — Account Operations UI Windows Path Hotfix

## Problem
`verify-dps2-account-operations-ui-alignment.mjs` derived its own path from `new URL(import.meta.url).pathname`.
On Windows a `file:///D:/...` URL exposes a pathname beginning with `/D:/...`; passing that directly to `path.resolve()` can produce a duplicated drive path such as `D:\\D:\\Projects\\...`.

## Fix
Use Node's cross-platform URL conversion API:

```js
import {fileURLToPath} from 'node:url';
const scriptFile=fileURLToPath(import.meta.url);
const root=path.resolve(path.dirname(scriptFile),'..');
```

No Account Operations runtime/UI/API behavior is changed by this hotfix.

## Verification
Run:

```bat
node tools\verify-dps2-account-operations-ui-alignment.mjs
```

The verifier must resolve files under the current project root without duplicating the Windows drive prefix.
