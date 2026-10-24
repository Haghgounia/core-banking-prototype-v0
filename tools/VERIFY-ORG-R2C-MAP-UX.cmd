@echo off
setlocal
cd /d "%~dp0.." || exit /b 1
where node >nul 2>nul
if errorlevel 1 (echo Node.js not found. & exit /b 1)
node tools\verify-org-r2c-map-ux.mjs
if errorlevel 1 exit /b 1
node tools\verify-org-r2b-location-contacts.mjs
exit /b %errorlevel%
