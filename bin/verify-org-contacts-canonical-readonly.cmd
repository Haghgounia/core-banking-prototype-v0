@echo off
setlocal EnableExtensions DisableDelayedExpansion
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0..\tools\verify-org-contact-r5b-runtime.ps1"
set "RC=%ERRORLEVEL%"
endlocal & exit /b %RC%
