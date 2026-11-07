# CLT R6K - Runtime Startup/Browser GET Corrective Delivery

## Unchanged program roadmap
R0 baseline; R1 reference and governance; R2 registry/search; R3 guarantor/insurance/valuation; R4 capacity/allocation; R5 release/history; R6 analyst UI/UAT; R7 integration, security and release.

## Evidence and scope
- R6I runtime evidence confirms 28 active Oracle groups, 0 active types and 0 operational collateral rows.
- R6J build succeeded but browser users saw HTTP 403 after default-mode restart.
- User's R6H PowerShell launcher returned exit code 0 with no output and no listener on port 8091; this is not a successful startup.
- R6K replaces the callable CMD launcher with a Node.js launcher (same child-process technique as previously successful R6J installer). It reuses the already built 0.11.0 JAR.
- R6K requires genuine local-mode boot marker, HTTP 200 health, 28 real Oracle groups and HTTP 200 for an Angular/Chrome-header-equivalent call before claiming PASS. Inaccessible API => nonzero exit and log output.
- Existing explicit opt-in, loopback-only, GET-only security filter and strict allow-list are unchanged. All nine CLT financial/reference write flags are disabled in the Java child process. No IAM changes.
- Standard `bin/start.cmd` deliberately stays normal/default-deny, not an implicit local-UAT grant. Separate `bin/start-clt-local.cmd` offers an explicit mode.

## Delivery evidence categories
- Static/node unit verification: script and 403/200 contracts (not database runtime)
- Windows user acceptance: remains pending; do not mark browser UAT passed unless R6K runtime receipt shows actual HTTP 200 and analyst confirms UI.
- No changes to Oracle data or reference/type policy; never invent `COLLATERAL_TYPE` records.

## Next gate
Run the one-step installer/launcher (no re-build), then open the collateral group form. If HTTP 200/28 groups verified, proceed to review `COLLATERAL_TYPE` policy data, not IAM.
