@echo off
setlocal EnableExtensions
set "ROOT=%~dp0..\"
cd /d "%ROOT%" || exit /b 1

echo ===============================================================
echo ORG-CAL2 OWNERSHIP CLEANUP - SOURCE PREFLIGHT - READ ONLY
echo ===============================================================
node "tools\verify-release-layout.mjs" || exit /b 1
node "tools\verify-org-cal2-consolidation.mjs" || exit /b 1
node "tools\verify-org-forms.mjs" || exit /b 1
node "tools\verify-org-ux-r2.mjs" || exit /b 1
node "tools\verify-org-grade-p3-p4.mjs" || exit /b 1
node "tools\verify-org-location-form.mjs" || exit /b 1
node "tools\verify-calendar2-business-calendar-lookups.mjs" || exit /b 1
node "tools\verify-node-tool-path-portability.mjs" || exit /b 1
echo ORG_CAL2_SOURCE_PREFLIGHT_PASS. Oracle remains untouched.
exit /b 0
