@echo off
setlocal EnableExtensions DisableDelayedExpansion
rem R5-B explicit pilot startup. No Oracle writes; database is not modified.
rem The operator must stop old runtime first (bin\stop.cmd).
set "ORG_CONTACT_STORAGE_MODE=canonical"
set "ORG_CONTACT_CANONICAL_WRITES_ENABLED=false"
echo [ORG R5-B] Canonical V2 READ ONLY. All writes DISABLED.
call "%~dp0start.cmd"
set "START_EXIT=%ERRORLEVEL%"
endlocal & exit /b %START_EXIT%
