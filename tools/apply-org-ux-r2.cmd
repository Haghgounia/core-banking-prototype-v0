@echo off
setlocal
if "%~2"=="" (
  echo Usage: apply-org-ux-r2.cmd PROJECT_ROOT CHECK^|APPLY
  exit /b 2
)
node "%~dp0apply-org-ux-r2.mjs" "%~1" "%~2"
exit /b %errorlevel%
