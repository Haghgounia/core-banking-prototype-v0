# CAL2 0.11.0 — Event Media Upload Size Hotfix 2

## Problem
A valid occasion image was rejected with the validation message that the image must be at most 5 MB. The 5 MB cap was introduced by the prototype implementation and was not a business requirement.

The image uploaded to the support conversation was about 1.1 MB, but that copy may have been recompressed by the conversation platform and therefore does not prove the original browser-selected file size.

## Decision
- Raise the CAL2 event-media application limit from 5 MB to 20 MB.
- Keep the global Spring multipart limit unchanged (`64MB`), so the CAL2 service remains the narrower policy boundary.
- Show the selected file size immediately in the admin UI.
- Reject files above 20 MB on the client before upload and return the same 20 MB policy from the backend.
- Keep JPEG/PNG/WebP binary-signature validation from Hotfix 1.
- Recommend images below 2 MB for faster login/dashboard delivery, but do not enforce 2 MB as a business limit.

## Database impact
None. `CAL2.EVENT_MEDIA` and its existing migration remain unchanged. Do not rerun or add DDL for this hotfix.

## Verification
Run:

```bat
node tools\verify-calendar2-event-media.mjs
```

Then run the normal production build:

```bat
build-production.cmd
```
