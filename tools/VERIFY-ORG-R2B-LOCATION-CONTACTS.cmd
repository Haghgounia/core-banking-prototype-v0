@echo off
setlocal
cd /d "%~dp0.."
where node >nul 2>nul
if errorlevel 1 (echo Node.js not found on PATH. & exit /b 1)
node tools\verify-org-r2b-location-contacts.mjs
exit /b %errorlevel%
