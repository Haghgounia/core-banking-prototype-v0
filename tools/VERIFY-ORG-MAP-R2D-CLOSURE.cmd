@echo off
setlocal
set "ROOT=%~dp0..\"
node "%ROOT%\tools\verify-org-map-r2d-closure.mjs" || exit /b 1
node "%ROOT%\tools\verify-org-r2c-map-ux.mjs" || exit /b 1
node "%ROOT%\tools\verify-org-r2c-map-filters.mjs" || exit /b 1
node "%ROOT%\tools\verify-org-r2b-location-contacts.mjs" || exit /b 1
echo ORG_MAP_R2D_PRESMOKE_PASS
