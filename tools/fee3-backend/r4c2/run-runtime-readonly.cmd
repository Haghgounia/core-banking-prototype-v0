@echo off
setlocal
cd /d "%~dp0\..\..\.."
python tools\fee3-backend\r4c2\runtime-readonly.py
exit /b %ERRORLEVEL%
