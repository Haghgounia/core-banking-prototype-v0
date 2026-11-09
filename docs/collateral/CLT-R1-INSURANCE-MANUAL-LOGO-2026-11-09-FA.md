# CLT R1 / Manual Insurance Company BLOB Upload - 2026-11-09

Applies to the existing 0.11.0 project after CLT-INSURANCE-LOGO-REPORT-HF1.

## Scope
- The Insurance Company edit form allows manual local PNG, JPEG and GIF upload, max 1,500,000 bytes, dimensions max 2,000 x 2,000, decoded image required.
- The browser shows a local file preview, requests explicit user confirmation of brand ownership, then sends multipart/form-data to an application-local API.
- API POST /api/v1/collateral/reference/logo-upload reuses the shared application DataSource, validates image bytes with CltInsuranceImagePolicy, uses Oracle RECORD_VERSION and LOGO IS NULL as concurrent/no-overwrite guards.
- Database row updates are transactional. A fresh independent Oracle read of image bytes and RECORD_VERSION verifies committed persistence.
- Existing 13 BLOBs are never replaced. User may upload for the 5 source failures and other companies with no stored image.
- No Oracle DDL, SQL scripts, IAM changes, new service process, special JWT or additional startup command.

## Acceptance gates
- Candidate: source and policy tests; live Angular/Maven build must pass on Windows.
- Analyst UAT: open an insurer with no logo, edit, choose valid image, visually verify it belongs to the company, confirm, upload, reload page and see persistent logo.
- Reject HTML/SVG, oversized files, stale versions and pre-existing logos.
- Do not treat an upload result as legal verification of an insurer license or logo ownership.

## Roadmap
The existing CLT R0-R7 phases are unchanged. R1 completion is NOT claimed; G-10, G-15, G-16 and G-17 stay open. R2/R3 remain a separate functional workstream.
