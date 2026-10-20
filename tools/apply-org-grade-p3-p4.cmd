@echo off
setlocal
if "%~1"=="" (
  echo Usage: apply-org-grade-p3-p4.cmd "D:\Projects\core-banking-prototype-v0" CHECK^|APPLY
  exit /b 2
)
if /I "%~2"=="APPLY" (
  powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0apply-org-grade-p3-p4.ps1" -ProjectRoot "%~1" -Apply
) else (
  powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0apply-org-grade-p3-p4.ps1" -ProjectRoot "%~1"
)
exit /b %ERRORLEVEL%
