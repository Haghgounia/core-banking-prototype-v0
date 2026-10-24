@echo off
setlocal
set "ROOT=%~dp0..\"
cd /d "%ROOT%" || exit /b 1
node "tools\verify-org-unit-location-r1.mjs" || exit /b 1
node "tools\verify-org-unit-location-r2a.mjs" || exit /b 1
node "tools\verify-org-forms.mjs" || exit /b 1
node "tools\verify-org-location-form.mjs" || exit /b 1
node "tools\verify-org-ux-r2.mjs" || exit /b 1
node "tools\verify-org-cal2-consolidation.mjs" || exit /b 1
node "tools\verify-release-layout.mjs" || exit /b 1
echo ORG_UNIT_LOCATIONS_R2A_STATIC_SUITE_PASS
