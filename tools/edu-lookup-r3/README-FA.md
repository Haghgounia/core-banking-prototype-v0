# EDU Lookup R3 - additive read-only cutover pilot

Baseline: core-banking-prototype-v0-2026-11-01-1800.zip

Adds only one Java Spring MVC controller. Does not modify any old descriptor, existing API, database tables, or GEO. Existing POST/PUT/DELETE remain as-is, and application cutover is NOT declared complete.

Endpoints: GET /api/v1/education/lookups/{fields|levels|institutions|field-levels}?text=...&limit=100

Response: [{"value":123,"code":"...","label":"..."}]

Prerequisites: the four EDU lookup views are VALID and the application Oracle user has SELECT privileges on them. Where no grants are in place, the endpoint will return an Oracle permission error; this installer does not grant privileges.

Run tools\edu-lookup-r3\INSTALL.cmd from project root after extracting zip. No files are placed directly in root.

After installation execute bin\stop.cmd, build-production.cmd, then bin\start.cmd only after a successful build. Verify GET requests and compare response semantics with the documented view WHERE clauses. No Oracle write occurs through this controller.

Open issues: Data ownership between CIF/EDU, full content parity, real runtime API test, Java build and analyst UAT. Historical/renamed/needs-review institutions are included in the current EDU view, so user-facing selection rules require business approval.
