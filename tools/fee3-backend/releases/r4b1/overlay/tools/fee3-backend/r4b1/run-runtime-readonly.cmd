@echo off
setlocal
cd /d "%~dp0\..\..\.."
python tools\fee3-backend\r4b1\runtime-readonly.py %*
exit /b %ERRORLEVEL%
